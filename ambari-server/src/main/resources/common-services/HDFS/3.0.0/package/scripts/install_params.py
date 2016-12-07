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
from ambari_commons import OSCheck

# These parameters are supposed to be referenced at installation time, before the Hadoop environment variables have been set
if OSCheck.is_windows_family():
  exclude_packages = []
else:
  from resource_management.libraries.functions.default import default
  from resource_management.libraries.functions.get_lzo_packages import get_lzo_packages
  from resource_management.libraries.script.script import Script

  _config = Script.get_config()
  stack_version_unformatted = str(_config['hostLevelParams']['stack_version'])

  # The logic for LZO also exists in OOZIE's params.py
  io_compression_codecs = default("/configurations/core-site/io.compression.codecs", None)
  lzo_enabled = io_compression_codecs is not None and "com.hadoop.compression.lzo" in io_compression_codecs.lower()
  lzo_packages = get_lzo_packages(stack_version_unformatted)

  exclude_packages = []
  if not lzo_enabled:
    exclude_packages += lzo_packages
