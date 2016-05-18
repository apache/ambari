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
os.environ["ROOT"] = ""

from mock.mock import patch, MagicMock
from unittest import TestCase
import platform

from ambari_commons import os_utils
os_utils.search_file = MagicMock(return_value="/tmp/ambari.properties")
import shutil
project_dir = os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
shutil.copyfile(project_dir+"/ambari-server/conf/unix/ambari.properties", "/tmp/ambari.properties")

with patch.object(platform, "linux_distribution", return_value = MagicMock(return_value=('Redhat', '6.4', 'Final'))):
  with patch("os.path.isdir", return_value = MagicMock(return_value=True)):
    with patch("os.access", return_value = MagicMock(return_value=True)):
      with patch.object(os_utils, "parse_log4j_file", return_value={'ambari.log.dir': '/var/log/ambari-server'}):
        from ambari_server.serverUtils import get_ambari_server_api_base
        from ambari_server.serverConfiguration import CLIENT_API_PORT, CLIENT_API_PORT_PROPERTY, SSL_API, DEFAULT_SSL_API_PORT, SSL_API_PORT

@patch.object(platform, "linux_distribution", new = MagicMock(return_value=('Redhat', '6.4', 'Final')))
class TestServerUtils(TestCase):

  def test_get_ambari_server_api_base(self):

    # Test case of using http protocol
    properties = FakeProperties({
      SSL_API: "false",
      CLIENT_API_PORT_PROPERTY: None
    })
    result = get_ambari_server_api_base(properties)
    self.assertEquals(result, 'http://127.0.0.1:8080/api/v1/')

    # Test case of using http protocol and custom port
    properties = FakeProperties({
      SSL_API: "false",
      CLIENT_API_PORT_PROPERTY: "8033"
      })
    result = get_ambari_server_api_base(properties)
    self.assertEquals(result, 'http://127.0.0.1:8033/api/v1/')

    # Test case of using https protocol (and ssl port)
    properties = FakeProperties({
      SSL_API: "true",
      SSL_API_PORT : "8443",
      CLIENT_API_PORT_PROPERTY: None
    })
    result = get_ambari_server_api_base(properties)
    self.assertEquals(result, 'https://127.0.0.1:8443/api/v1/')



class FakeProperties(object):
  def __init__(self, prop_map):
    self.prop_map = prop_map

  def get_property(self, prop_name):
    return self.prop_map[prop_name]
