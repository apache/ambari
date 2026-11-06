#!/usr/bin/env python3
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

"""

import unittest
import tempfile
import os
from unittest.mock import patch
from namenode_role_switch import plan_switch, switch_roles, verify_disk_config, operation_lock

SITE = {'dfs.nameservices': 'ns', 'dfs.ha.namenodes.ns': 'nn1,nn2,nn3',
        'dfs.ha.automatic-failover.enabled': 'true',
        'dfs.namenode.rpc-address.ns.nn1': 'a:8020',
        'dfs.namenode.rpc-address.ns.nn2': 'b:8020',
        'dfs.namenode.rpc-address.ns.nn3': 'c:8020'}
REQUEST = {'source_host': 'a', 'target_host': 'c'}

class Runner:
  def __init__(self):
    self.roles = {'nn1': 'active', 'nn2': 'standby', 'nn3': 'standby'}
    self.calls = []
    self.health = 0
    self.failover = 0
    self.apply = True
  def __call__(self, args, timeout):
    self.calls.append(args)
    if args[0] == '-getServiceState': return 0, 'WARNING: wrapper warning\n' + self.roles[args[1]]
    if args[0] == '-checkHealth': return self.health, ''
    if args[0] == '-failover':
      if self.apply: self.roles.update({args[1]: 'standby', args[2]: 'active'})
      return self.failover, ''
    raise AssertionError(args)
  def mutations(self): return [c for c in self.calls if c[0] == '-failover']

class Tests(unittest.TestCase):
  def plan(self): return plan_switch(SITE, 'a', REQUEST)
  def test_three_nodes(self): self.assertEqual(self.plan(), ('ns', ['nn1', 'nn2', 'nn3'], 'nn1', 'nn3'))
  def test_two_nodes_custom_ids(self):
    s = {'dfs.nameservices': 'ns', 'dfs.ha.namenodes.ns': 'primary,backup',
         'dfs.ha.automatic-failover.enabled.ns': 'true',
         'dfs.namenode.rpc-address.ns.primary': 'a:1234', 'dfs.namenode.rpc-address.ns.backup': 'b:1234'}
    self.assertEqual(plan_switch(s, 'a', {'source_host':'a','target_host':'b'})[2:], ('primary','backup'))
  def test_unknown_target(self):
    with self.assertRaises(ValueError): plan_switch(SITE,'a',dict(REQUEST,target_host='prefix-c'))
  def test_same_target(self):
    with self.assertRaises(ValueError): plan_switch(SITE,'a',dict(REQUEST,target_host='a'))
  def test_wrong_execution_host(self):
    with self.assertRaises(ValueError): plan_switch(SITE,'b',REQUEST)
  def test_manual_ha(self):
    with self.assertRaises(ValueError): plan_switch(dict(SITE, **{'dfs.ha.automatic-failover.enabled':'false'}),'a',REQUEST)
  def test_extra_input_rejected(self):
    with self.assertRaises(ValueError): plan_switch(SITE,'a',dict(REQUEST,force=True))
  def test_missing_input(self):
    with self.assertRaises(ValueError): plan_switch(SITE,'a',{})
  def test_success_single_mutation(self):
    r=Runner(); result=switch_roles(self.plan(),r,sleep=lambda _:None)
    self.assertTrue(result['verified']); self.assertEqual(r.mutations(),[('-failover','nn1','nn3')])
  def test_wrong_active(self):
    r=Runner();r.roles.update(nn1='standby',nn2='active')
    with self.assertRaises(ValueError): switch_roles(self.plan(),r)
    self.assertEqual(r.mutations(),[])
  def test_multiple_active(self):
    r=Runner();r.roles['nn2']='active'
    with self.assertRaises(ValueError): switch_roles(self.plan(),r)
    self.assertEqual(r.mutations(),[])
  def test_unknown_peer(self):
    r=Runner();r.roles['nn2']='unknown'
    with self.assertRaises(ValueError): switch_roles(self.plan(),r)
    self.assertEqual(r.mutations(),[])
  def test_health_failure(self):
    r=Runner();r.health=1
    with self.assertRaises(ValueError): switch_roles(self.plan(),r)
    self.assertEqual(r.mutations(),[])
  def test_drift_during_health(self):
    r=Runner()
    def run(args,timeout):
      result=r(args,timeout)
      if args[0]=='-checkHealth':r.roles.update(nn1='standby',nn2='active')
      return result
    with self.assertRaises(ValueError):switch_roles(self.plan(),run)
    self.assertEqual(r.mutations(),[])
  def test_failover_error_not_retried_even_if_roles_changed(self):
    r=Runner();r.failover=1
    with self.assertRaisesRegex(ValueError,'inspect live roles'):switch_roles(self.plan(),r)
    self.assertEqual(len(r.mutations()),1)
  def test_success_exit_without_transition_is_failure(self):
    r=Runner();r.apply=False
    with self.assertRaisesRegex(ValueError,'not confirmed'):switch_roles(self.plan(),r,sleep=lambda _:None,attempts=2)
    self.assertEqual(len(r.mutations()),1)
  def test_timeout_not_retried(self):
    r=Runner()
    def run(args,timeout):
      result=r(args,timeout)
      if args[0]=='-failover':raise TimeoutError('command timed out')
      return result
    with self.assertRaises(TimeoutError):switch_roles(self.plan(),run)
    self.assertEqual(len(r.mutations()),1)
  def test_local_config_drift(self):
    with tempfile.TemporaryDirectory() as d:
      f=os.path.join(d,'hdfs-site.xml')
      with open(f,'w') as h:h.write('<configuration><property><name>dfs.nameservices</name><value>wrong</value></property></configuration>')
      with self.assertRaisesRegex(ValueError,'differs'):verify_disk_config(SITE,f)
  def test_same_config(self):
    with tempfile.TemporaryDirectory() as d:
      f=os.path.join(d,'hdfs-site.xml')
      with open(f,'w') as h:h.write('<configuration>'+''.join('<property><name>'+k+'</name><value>'+v+'</value></property>' for k,v in SITE.items())+'</configuration>')
      verify_disk_config(SITE,f)
  def test_double_submission_lock(self):
    with tempfile.TemporaryDirectory() as d:
      with operation_lock(d,'ns'):
        with self.assertRaisesRegex(ValueError,'already running'):
          with operation_lock(d,'ns'): pass
      with operation_lock(d,'ns'):pass

if __name__ == '__main__':unittest.main(verbosity=2)
