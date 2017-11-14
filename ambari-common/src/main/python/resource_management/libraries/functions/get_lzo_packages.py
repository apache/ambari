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
__all__ = ["get_lzo_packages"]

from ambari_commons.os_check import OSCheck
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.script.script import Script

# TODO: Make list of lzo packages stack driven
def get_lzo_packages(stack_version_unformatted):
  lzo_packages = []
  script_instance = Script.get_instance()
  if OSCheck.is_suse_family() and int(OSCheck.get_os_major_version()) >= 12:
    lzo_packages += ["liblzo2-2", "hadoop-lzo-native"]
  elif OSCheck.is_redhat_family() or OSCheck.is_suse_family():
    lzo_packages += ["lzo", "hadoop-lzo-native"]
  elif OSCheck.is_ubuntu_family():
    lzo_packages += ["liblzo2-2"]

  if stack_version_unformatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_unformatted):
    if OSCheck.is_ubuntu_family():
      lzo_packages += [script_instance.format_package_name("hadooplzo-${stack_version}") ,
                       script_instance.format_package_name("hadooplzo-${stack_version}-native")]
    else:
      lzo_packages += [script_instance.format_package_name("hadooplzo_${stack_version}"),
                       script_instance.format_package_name("hadooplzo_${stack_version}-native")]
  else:
    lzo_packages += ["hadoop-lzo"]

  return lzo_packages
