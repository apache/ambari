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

import os

from resource_management.core.exceptions import Fail
from stacks.utils.RMFTestCase import RMFTestCase

import unittest

class TestJdbcDriverConfig(RMFTestCase):
  STACK_VERSION = "2.6"
  CONFIG_DIR = os.path.join(os.path.dirname(__file__), '../configs')


  def test_jdbc_driver_1_4_4_2_0(self):
    self.executeScript("SQOOP/1.4.4.2.0/package/scripts/sqoop_client.py",
                       classname="SqoopClient",
                       command="configure",
                       target=RMFTestCase.TARGET_COMMON_SERVICES,
                       stack_version=self.STACK_VERSION,
                       config_file=os.path.join(self.CONFIG_DIR, "sqoop_default.json"))

  def test_unsupported_jdbc_driver_1_4_4_2_0(self):
    try:
      self.executeScript("SQOOP/1.4.4.2.0/package/scripts/sqoop_client.py",
                       classname="SqoopClient",
                       command="configure",
                       target=RMFTestCase.TARGET_COMMON_SERVICES,
                       stack_version=self.STACK_VERSION,
                       config_file=os.path.join(self.CONFIG_DIR, "sqoop_unsupported_jdbc_driver.json"))
      self.fail("Expected 'Fail', but call completed without throwing")
    except Fail as e:
      pass
    except Exception as e:
      self.fail("Expected 'Fail', got {}".format(e))

  def test_jdbc_driver_1_4_4_3_0(self):
    self.executeScript("SQOOP/1.4.4.3.0/package/scripts/sqoop_client.py",
                       classname="SqoopClient",
                       command="configure",
                       target=RMFTestCase.TARGET_COMMON_SERVICES,
                       stack_version=self.STACK_VERSION,
                       config_file=os.path.join(self.CONFIG_DIR, "sqoop_default.json"))

  def test_unsupported_jdbc_driver_1_4_4_3_0(self):
    try:
      self.executeScript("SQOOP/1.4.4.3.0/package/scripts/sqoop_client.py",
                         classname="SqoopClient",
                         command="configure",
                         target=RMFTestCase.TARGET_COMMON_SERVICES,
                         stack_version=self.STACK_VERSION,
                         config_file=os.path.join(self.CONFIG_DIR, "sqoop_unsupported_jdbc_driver.json"))
      self.fail("Expected 'Fail', but call completed without throwing")
    except Fail as e:
      pass
    except Exception as e:
      self.fail("Expected 'Fail', got {}".format(e))
