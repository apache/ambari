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

import re
from resource_management import *

def properties_inline_template(configurations):
  return source.InlineTemplate('''{% for key, value in configurations_dict.items() %}{{ key }}={{ value }}
{% endfor %}''', configurations_dict=configurations)

def properties_config(filename, configurations = None, conf_dir = None,
                      mode = None, owner = None, group = None, brokerid = None):
    config_content = properties_inline_template(configurations)
    File (format("{conf_dir}/{filename}"), content = config_content, owner = owner,
          group = group, mode = mode)
