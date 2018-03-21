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

import copy
import json
import os

from mock.mock import patch
from stacks.utils.RMFTestCase import Template, RMFTestCase


class TestPxf(RMFTestCase):
  PXF_PACKAGE_DIR = "PXF/3.0.0/package"
  STACK_VERSION = "2.3"
  PXF_USER = 'pxf'
  PXF_GROUP = 'pxf'
  TOMCAT_GROUP = 'tomcat'
  BASH_SHELL = '/bin/bash'
  DEFAULT_TIMEOUT = 600
  CONFIG_FILE = os.path.join(os.path.dirname(__file__), '../configs/pxf_default.json')
  with open(CONFIG_FILE, "r") as f:
     pxf_config = json.load(f)

  def setUp(self):
    self.config_dict = copy.deepcopy(self.pxf_config)

  def assert_configure_default(self):
    self.assertResourceCalled('User', self.PXF_USER,
                              groups=[self.getConfig()['configurations']['hdfs-site']['dfs.permissions.superusergroup'],
                              self.getConfig()['configurations']['cluster-env']['user_group'],
                              self.TOMCAT_GROUP],
        shell=self.BASH_SHELL)

    self.assertResourceCalled('File', '/etc/pxf/conf/pxf-env.sh',
                content=Template('pxf-env.j2'))

    self.assertResourceCalled('File', '/etc/pxf/conf/pxf-public.classpath',
                content = self.getConfig()['configurations']['pxf-public-classpath']['content'].lstrip())

    self.assertResourceCalled('File', '/etc/pxf/conf/pxf-profiles.xml',
                content = self.getConfig()['configurations']['pxf-profiles']['content'].lstrip())

    self.assertResourceCalled('XmlConfig', 'pxf-site.xml',
                              conf_dir='/etc/pxf/conf',
                              configurations=self.getConfig()['configurations']['pxf-site'],
                              configuration_attributes=self.getConfig()['configurationAttributes']['pxf-site'])

    self.assertResourceCalled('Execute', 'service pxf-service init',
                              timeout=self.DEFAULT_TIMEOUT,
                              logoutput=True)

  @patch('shutil.copy2')
  def test_install_default(self, shutil_copy2_mock):
    self.executeScript(self.PXF_PACKAGE_DIR + "/scripts/pxf.py",
                       classname="Pxf",
                       command="install",
                       config_dict=self.config_dict,
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES,
                       try_install=True)

    self.assertResourceCalled('Package', 'pxf-service',
                              retry_count=5,
                              retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'apache-tomcat',
                              retry_count=5,
                              retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'pxf-hive',
                              retry_count=5,
                              retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'pxf-hdfs',
                              retry_count=5,
                              retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'pxf-hbase',
                              retry_count=5,
                              retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'pxf-json',
                              retry_count=5,
                              retry_on_repo_unavailability=False)

    self.assert_configure_default()

  @patch('shutil.copy2')
  def test_configure_default(self, shutil_copy2_mock):
      self.executeScript(self.PXF_PACKAGE_DIR + "/scripts/pxf.py",
                   classname="Pxf",
                   command="configure",
                   config_dict=self.config_dict,
                   stack_version=self.STACK_VERSION,
                   target=RMFTestCase.TARGET_COMMON_SERVICES,
                   try_install=True)

      self.assert_configure_default()

  @patch('shutil.copy2')
  def test_start_default(self, shutil_copy2_mock):
      self.executeScript(self.PXF_PACKAGE_DIR + "/scripts/pxf.py",
                   classname="Pxf",
                   command="start",
                   config_dict=self.config_dict,
                   stack_version=self.STACK_VERSION,
                   target=RMFTestCase.TARGET_COMMON_SERVICES,
                   try_install=True)

      self.assert_configure_default()

      self.assertResourceCalled('Directory', '/var/pxf',
              owner=self.PXF_USER,
              group=self.PXF_GROUP,
              create_parents = True)

      self.assertResourceCalled('Execute', 'service pxf-service restart',
                          timeout=self.DEFAULT_TIMEOUT,
                          logoutput=True)

  def test_stop_default(self):
      self.executeScript(self.PXF_PACKAGE_DIR + "/scripts/pxf.py",
                   classname="Pxf",
                   command="stop",
                   config_dict=self.config_dict,
                   stack_version=self.STACK_VERSION,
                   target=RMFTestCase.TARGET_COMMON_SERVICES,
                   try_install=True)

      self.assertResourceCalled('Execute', 'service pxf-service stop',
                          timeout=self.DEFAULT_TIMEOUT,
                          logoutput=True)

  def test_status_default(self):
      self.executeScript(self.PXF_PACKAGE_DIR + "/scripts/pxf.py",
                   classname="Pxf",
                   command="status",
                   config_dict=self.config_dict,
                   stack_version=self.STACK_VERSION,
                   target=RMFTestCase.TARGET_COMMON_SERVICES,
                   try_install=True)

      self.assertResourceCalled('Execute', 'service pxf-service status',
                          timeout=self.DEFAULT_TIMEOUT,
                          logoutput=True)

  def tearDown(self):
      self.assertNoMoreResources()
