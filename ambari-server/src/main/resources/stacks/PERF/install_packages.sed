# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
/actionexecute/{i\
  def actionexecute(self, env):\
    from resource_management.core.resources.system import Execute\
    # Parse parameters\
    config = Script.get_config()\
    try:\
      command_repository = CommandRepository(config['repositoryFile'])\
    except KeyError:\
      raise Fail("The command repository indicated by 'repositoryFile' was not found")\
    self.repository_version = command_repository.version_string\
    if self.repository_version is None:\
      raise Fail("Cannot determine the repository version to install")\
    self.repository_version = self.repository_version.strip()\
    (stack_selector_name, stack_selector_path, stack_selector_package) = stack_tools.get_stack_tool(stack_tools.STACK_SELECTOR_NAME)\
    command = 'ambari-python-wrap {0} install {1}'.format(stack_selector_path, self.repository_version)\
    Execute(command)\
    self.structured_output = {\
      'package_installation_result': 'SUCCESS',\
      'repository_version_id': command_repository.version_id\
    }\
    self.put_structured_out(self.structured_output)\
  def actionexecute_old(self, env):
d
}