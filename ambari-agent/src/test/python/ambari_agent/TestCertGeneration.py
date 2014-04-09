#!/usr/bin/env python

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
import os
import tempfile
import shutil
from unittest import TestCase
import configparser
import security
from security import CertificateManager
from ambari_agent import AmbariConfig

class TestCertGeneration(TestCase):
  def setUp(self):
    self.tmpdir = tempfile.mkdtemp()
    config = configparser.RawConfigParser()
    config.add_section('server')
    config.set('server', 'hostname', 'example.com')
    config.set('server', 'url_port', '777')
    config.add_section('security')
    config.set('security', 'keysdir', self.tmpdir)
    config.set('security', 'server_crt', 'ca.crt')
    self.certMan = CertificateManager(config)
    
  def test_generation(self):
    self.certMan.genAgentCrtReq()
    self.assertTrue(os.path.exists(self.certMan.getAgentKeyName()))
    self.assertTrue(os.path.exists(self.certMan.getAgentCrtReqName()))
  def tearDown(self):
    shutil.rmtree(self.tmpdir)
    

