#!/usr/bin/env python
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

Ambari Agent

"""

from resource_management.core.logger import Logger
from resource_management.core import shell
from ambari_commons import subprocess32

class JcePolicyInfo:
  def __init__(self, java_home):
    self.java_home = java_home
    self.jar = "/var/lib/ambari-agent/tools/jcepolicyinfo.jar"

  def is_unlimited_key_jce_policy(self):
    Logger.info("Testing the JVM's JCE policy to see it if supports an unlimited key length.")
    return shell.call(
      self._command('-tu'),
      stdout = subprocess32.PIPE,
      stderr = subprocess32.PIPE,
      timeout = 5,
      quiet = True)[0] == 0

  def _command(self, options):
    return '{0}/bin/java -jar /var/lib/ambari-agent/tools/jcepolicyinfo.jar {1}'.format(self.java_home, options)
