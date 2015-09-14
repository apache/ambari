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
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format

def get_lzo_packages(stack_version_unformatted):
  lzo_packages = []
 
  if OSCheck.is_redhat_family() or OSCheck.is_suse_family():
    lzo_packages += ["lzo", "hadoop-lzo-native"]
  elif OSCheck.is_ubuntu_family():
    lzo_packages += ["liblzo2-2"]
    
  underscored_version = stack_version_unformatted.replace('.', '_')
  dashed_version = stack_version_unformatted.replace('.', '-')
  hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

  if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
    if OSCheck.is_redhat_family() or OSCheck.is_suse_family():
      lzo_packages += [format("hadooplzo_{underscored_version}_*")]
    elif OSCheck.is_ubuntu_family():
      lzo_packages += [format("hadooplzo_{dashed_version}_*")]
  else:
    lzo_packages += ["hadoop-lzo"]

  return lzo_packages
