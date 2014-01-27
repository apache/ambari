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

from stacks.utils.RMFTestCase import *


class TestFalconClient(RMFTestCase):
  def test_configure_default(self):
    self.executeScript("2.1.1/services/FALCON/package/scripts/falcon_client.py",
                       classname="FalconClient",
                       command="configure",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute',
                              'cd /tmp; rm -f falcon-0.4.0.2.0.6.0-76.el6.noarch.rpm; wget http://public-repo-1.hortonworks.com/HDP-LABS/Projects/Falcon/2.0.6.0-76/rpm/falcon-0.4.0.2.0.6.0-76.el6.noarch.rpm; rpm -Uvh --nodeps falcon-0.4.0.2.0.6.0-76.el6.noarch.rpm',
                              not_if='yum list installed | grep falcon', )

    self.assertResourceCalled('File', '/etc/falcon/conf/client.properties',
                              content=Template('client.properties.j2'),
                              mode=0644, )
    self.assertNoMoreResources()
