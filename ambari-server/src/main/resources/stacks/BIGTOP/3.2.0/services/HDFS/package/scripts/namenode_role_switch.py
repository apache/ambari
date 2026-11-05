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

"""Explicit planned role switch. No forced transition or automatic retry."""
import json
import os
import pwd
import re
import tempfile
import time
import hashlib
import fcntl
import xml.etree.ElementTree as ET
from contextlib import contextmanager
from ha_topology import get_local_namenode


def plan_switch(site, local_host, request):
  if not isinstance(request, dict) or set(request) != {'source_host', 'target_host'}:
    raise ValueError('Expected source_host and target_host only')
  if any(not isinstance(request[k], str) or not request[k].strip() for k in request):
    raise ValueError('Host names must be nonempty strings')
  normalize = lambda h: h.lower().rstrip('.') if isinstance(h, str) else ''
  if not normalize(local_host) or normalize(request['source_host']) != normalize(local_host):
    raise ValueError('Run the command on the reviewed Active host')
  ns, source, _ = get_local_namenode(site, local_host)
  target_ns, target, _ = get_local_namenode(site, request['target_host'])
  if not ns or target_ns != ns or not target or target == source:
    raise ValueError('Choose a distinct NameNode in the same nameservice')
  ids = [x.strip() for x in site.get('dfs.ha.namenodes.' + ns, '').split(',')]
  if len(ids) not in (2, 3) or len(set(ids)) != len(ids):
    raise ValueError('Expected two or three distinct NameNode IDs')
  if any(not re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.-]*', x) for x in [ns] + ids):
    raise ValueError('Invalid nameservice or NameNode ID')
  auto = site.get('dfs.ha.automatic-failover.enabled.' + ns,
                  site.get('dfs.ha.automatic-failover.enabled', 'false'))
  if str(auto).lower() != 'true':
    raise ValueError('Automatic failover is required')
  return ns, ids, source, target


def verify_disk_config(site, filename):
  disk = {p.findtext('name'): p.findtext('value', '') for p in ET.parse(filename).getroot().findall('property')}
  def relevant(k):
    return k in ('dfs.nameservices', 'dfs.internal.nameservices') or k.startswith((
      'dfs.ha.', 'dfs.namenode.rpc-address.', 'dfs.namenode.servicerpc-address.',
      'dfs.namenode.shared.edits.dir'))
  for key in set(site) | set(disk):
    if relevant(key) and str(site.get(key, '')).strip() != str(disk.get(key, '')).strip():
      raise ValueError('Local hdfs-site differs from Ambari command: ' + key)


def switch_roles(plan, run, sleep=time.sleep, attempts=12):
  ns, ids, source, target = plan
  def states():
    result = {}
    for nn in ids:
      code, out = run(('-getServiceState', nn), 15)
      # Hadoop wrappers may print deprecation warnings before the state.
      lines = [line.strip() for line in out.splitlines() if line.strip()]
      state = lines[-1] if lines else ''
      if code or state not in ('active', 'standby'):
        raise ValueError('Unknown HA state for ' + nn)
      result[nn] = state
    return result
  before = states()
  expected = {nn: ('active' if nn == source else 'standby') for nn in ids}
  if before != expected:
    raise ValueError('HA roles changed or are ambiguous; reopen the dialog')
  for nn in (source, target):
    code, _ = run(('-checkHealth', nn), 15)
    if code:
      raise ValueError('NameNode health check failed: ' + nn)
  if states() != expected:
    raise ValueError('HA roles changed during preflight; no switch performed')
  # Exactly one mutation. A failure/timeout may have changed roles: never retry.
  code, _ = run(('-failover', source, target), 120)
  if code:
    raise ValueError('Failover returned an error; inspect live roles before any retry')
  wanted = {nn: ('active' if nn == target else 'standby') for nn in ids}
  for attempt in range(attempts):
    try:
      after = states()
    except ValueError:
      after = None
    if after == wanted:
      return {'nameservice': ns, 'before': before, 'after': after, 'verified': True}
    if attempt + 1 < attempts:
      sleep(2)
  raise ValueError('Failover result not confirmed; inspect live roles. No rollback attempted')


@contextmanager
def operation_lock(directory, ns):
  path = os.path.join(directory, 'nn-role-switch-' + hashlib.sha256(ns.encode()).hexdigest() + '.lock')
  fd = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
  try:
    try:
      fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
      raise ValueError('A role switch is already running on this source host')
    yield
  finally:
    os.close(fd)


def execute(params, hdfs_binary):
  from resource_management.core import shell
  from resource_management.core.exceptions import Fail
  try:
    plan = plan_switch(params.hdfs_site, params.hostname, json.loads(params.name_node_params or '{}'))
    verify_disk_config(params.hdfs_site, os.path.join(params.hadoop_conf_dir, 'hdfs-site.xml'))
    with operation_lock('/var/run/ambari-agent', plan[0]):
      # Isolated credentials; do not replace or destroy another operation's ticket.
      with tempfile.TemporaryDirectory(prefix='ambari-nn-role-switch-') as tmp:
        user = pwd.getpwnam(params.hdfs_user)
        os.chown(tmp, user.pw_uid, user.pw_gid)
        env = {'KRB5CCNAME': 'FILE:' + os.path.join(tmp, 'ccache')}
        if params.security_enabled:
          shell.checked_call((params.kinit_path_local, '-kt', params.hdfs_user_keytab,
                              params.hdfs_principal_name), user=params.hdfs_user,
                             env=env, timeout=30)
        def run(args, timeout):
          return shell.call((hdfs_binary, '--config', params.hadoop_conf_dir, 'haadmin',
                             '-ns', plan[0]) + args, user=params.hdfs_user,
                            env=env, timeout=timeout, logoutput=True)
        return switch_roles(plan, run)
  except (ValueError, OSError, ET.ParseError) as error:
    raise Fail(str(error)) from error
