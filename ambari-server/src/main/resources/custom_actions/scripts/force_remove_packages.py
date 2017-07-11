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
from resource_management import Script
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.packaging import Package

class ForceRemovePackages(Script):
  """
  This script is used during cross-stack upgrade to remove packages
  required by the old stack that are in conflict with packages from the
  new stack (eg. stack tools).  It can be called via REST API as a custom
  action.
  """

  def actionexecute(self, env):
    config = Script.get_config()
    packages_to_remove = config['roleParams']['package_list'].split(',')
    structured_output = {'success': [], 'failure': []}

    for package_name in packages_to_remove:
      try:
        Package(package_name, action='remove', ignore_dependencies = True)
        Logger.info('Removed {0}'.format(package_name))
        structured_output['success'].append(package_name)
      except Exception, e:
        Logger.exception('Failed to remove {0}: {1}'.format(package_name, str(e)))
        structured_output['failure'].append(package_name)

    self.put_structured_out(structured_output)

    if structured_output['failure']:
      raise Fail('Failed to remove packages: ' + ', '.join(structured_output['failure']))


if __name__ == '__main__':
  ForceRemovePackages().execute()
