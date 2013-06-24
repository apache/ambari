#!/usr/bin/env python2.6

'''
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
'''

from unittest import TestCase
import ambari_agent.hostname as hostname
import ambari_agent.AmbariConfig as AmbariConfig
import socket
import tempfile
import shutil
import os, pprint, json,stat

class TestHostname(TestCase):

  def test_hostname(self):
    self.assertEquals(hostname.hostname(), socket.getfqdn(), 
                      "hostname should equal the socket-based hostname")
    pass

  def test_hostname_override(self):
    fd = tempfile.mkstemp(text=True)
    tmpname = fd[1]
    os.close(fd[0])
    os.chmod(tmpname, os.stat(tmpname).st_mode | stat.S_IXUSR)

    tmpfile = file(tmpname, "w+")
    config = AmbariConfig.config
    try:
      tmpfile.write("#!/bin/sh\n\necho 'test.example.com'")
      tmpfile.close()

      config.set('agent', 'hostname_script', tmpname)

      self.assertEquals(hostname.hostname(), 'test.example.com', "expected hostname 'test.example.com'")
    finally:
      os.remove(tmpname)
      config.remove_option('agent', 'hostname_script')

    pass

  def test_public_hostname_override(self):
    fd = tempfile.mkstemp(text=True)
    tmpname = fd[1]
    os.close(fd[0])
    os.chmod(tmpname, os.stat(tmpname).st_mode | stat.S_IXUSR)
   
    tmpfile = file(tmpname, "w+")

    config = AmbariConfig.config
    try:
      tmpfile.write("#!/bin/sh\n\necho 'test.example.com'")
      tmpfile.close()

      config.set('agent', 'public_hostname_script', tmpname)

      self.assertEquals(hostname.public_hostname(), 'test.example.com', 
                        "expected hostname 'test.example.com'")
    finally:
      os.remove(tmpname)
      config.remove_option('agent', 'public_hostname_script')

    pass




