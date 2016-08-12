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
import crypt
import json
import os

from stacks.utils.RMFTestCase import RMFTestCase, InlineTemplate


class HawqBaseTestCase(RMFTestCase):

  HAWQ_PACKAGE_DIR = 'HAWQ/2.0.0/package'
  TARGET_COMMON_SERVICES = RMFTestCase.TARGET_COMMON_SERVICES
  SOURCE_HAWQ_SCRIPT = 'source /usr/local/hawq/greenplum_path.sh && '
  STACK_VERSION = '2.3'
  GPADMIN = 'gpadmin'
  POSTGRES = 'postgres'
  CONF_DIR = '/usr/local/hawq/etc/'
  CONFIG_FILE = os.path.join(os.path.dirname(__file__), '../configs/hawq_default.json')
  with open(CONFIG_FILE, "r") as f:
    hawq_config = json.load(f)

  def setUp(self):
    self.config_dict = copy.deepcopy(self.hawq_config)

  def asserts_for_configure(self):

    self.assertResourceCalled('Group', self.GPADMIN,
        ignore_failures = True
        )

    self.assertResourceCalled('User', self.GPADMIN,
        gid = self.GPADMIN,
        groups = [self.GPADMIN, u'hadoop'],
        ignore_failures = True,
        password = crypt.crypt(self.config_dict['configurations']['hawq-env']['hawq_password'], "$1$salt$")
        )

    self.assertResourceCalled('Group', self.POSTGRES,
        ignore_failures = True
        )

    self.assertResourceCalled('User', self.POSTGRES,
        gid = self.POSTGRES,
        groups = [self.POSTGRES, u'hadoop'],
        ignore_failures = True
        )

    self.assertResourceCalled('Execute', 'chown -R gpadmin:gpadmin /usr/local/hawq/',
        timeout = 600
        )

    self.assertResourceCalled('XmlConfig', 'hdfs-client.xml',
        conf_dir = self.CONF_DIR,
        configurations = self.getConfig()['configurations']['hdfs-client'],
        configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-client'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('XmlConfig', 'yarn-client.xml',
        conf_dir = self.CONF_DIR,
        configurations = self.getConfig()['configurations']['yarn-client'],
        configuration_attributes = self.getConfig()['configuration_attributes']['yarn-client'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('XmlConfig', 'hawq-site.xml',
        conf_dir = self.CONF_DIR,
        configurations = self.getConfig()['configurations']['hawq-site'],
        configuration_attributes = self.getConfig()['configuration_attributes']['hawq-site'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    if self.COMPONENT_TYPE == 'master':
        self.assertResourceCalled('File', self.CONF_DIR + 'hawq_check.cnf',
            content = self.getConfig()['configurations']['hawq-check-env']['content'],
            owner = self.GPADMIN,
            group = self.GPADMIN,
            mode = 0644
            )

        self.assertResourceCalled('File', self.CONF_DIR + 'slaves',
            content = InlineTemplate('c6401.ambari.apache.org\nc6402.ambari.apache.org\nc6403.ambari.apache.org\n\n'),
            group = self.GPADMIN,
            owner = self.GPADMIN,
            mode = 0644
            )

    self.assertResourceCalled('Directory', '/data/hawq/' + self.COMPONENT_TYPE,
        group = self.GPADMIN,
        owner = self.GPADMIN,
        create_parents = True
        )

    self.assertResourceCalled('Execute', 'chmod 700 /data/hawq/' + self.COMPONENT_TYPE,
        user = 'root',
        timeout = 600
        )

    self.assertResourceCalled('Directory', '/data/hawq/tmp/' + self.COMPONENT_TYPE,
        group = self.GPADMIN,
        owner = self.GPADMIN,
        create_parents = True
        )
