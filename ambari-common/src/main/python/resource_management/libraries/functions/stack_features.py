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

"""

# simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import ambari_simplejson as json
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

def check_stack_feature(stack_feature, stack_version):
  """
  Given a stack_feature and a specific stack_version, it validates that the feature is supported by the stack_version.
  IMPORTANT, notice that the mapping of feature to version comes from cluster-env if it exists there.
  :param stack_feature: Feature name to check if it is supported by the stack. For example: "rolling_upgrade"
  :param stack_version: Version of the stack
  :return: Will return True if successful, otherwise, False. 
  """

  from resource_management.libraries.functions.default import default
  from resource_management.libraries.functions.version import compare_versions
  stack_features_config = default("/configurations/cluster-env/stack_features", None)

  if not stack_version:
    Logger.debug("Cannot determine if feature %s is supported since did not provide a stack version." % stack_feature)
    return False

  if stack_features_config:
    data = json.loads(stack_features_config)
    for feature in data["stack_features"]:
      if feature["name"] == stack_feature:
        if "min_version" in feature:
          min_version = feature["min_version"]
          if compare_versions(stack_version, min_version, format = True) < 0:
            return False
        if "max_version" in feature:
          max_version = feature["max_version"]
          if compare_versions(stack_version, max_version, format = True) >= 0:
            return False
        return True
  else:
    raise Fail("Stack features not defined by stack")
        
  return False
