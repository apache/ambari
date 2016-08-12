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

from mock.mock import patch
from hawq_base_test_case import HawqBaseTestCase


class TestHawqSegment(HawqBaseTestCase):

  COMPONENT_TYPE = 'segment'

  @patch ('common.__set_osparams')
  def test_configure_default(self, set_osparams_mock):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqsegment.py',
        classname = 'HawqSegment',
        command = 'configure',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('common.__set_osparams')
  def test_install_default(self, set_osparams_mock):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqsegment.py',
        classname = 'HawqSegment',
        command = 'install',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('common.__set_osparams')
  def test_start_default(self, set_osparams_mock):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqsegment.py',
        classname = 'HawqSegment',
        command = 'start',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.asserts_for_configure()

    self.assertResourceCalled('Execute', self.SOURCE_HAWQ_SCRIPT + 'hawq init segment -a -v',
        logoutput = True,
        not_if = None,
        only_if = None,
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()


  @patch ('common.get_local_hawq_site_property_value')
  def test_stop_default(self, get_local_hawq_site_property_value_mock):
    get_local_hawq_site_property_value_mock.return_value = 40000

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqsegment.py',
        classname = 'HawqSegment',
        command = 'stop',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', self.SOURCE_HAWQ_SCRIPT + 'hawq stop segment -M fast -a -v',
        logoutput = True,
        not_if = None,
        only_if = "netstat -tupln | egrep ':40000\\s' | egrep postgres",
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()
