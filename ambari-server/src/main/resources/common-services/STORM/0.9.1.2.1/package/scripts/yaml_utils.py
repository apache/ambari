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
import resource_management
from resource_management.core.source import InlineTemplate

def escape_yaml_propetry(value):
  unquouted = False
  unquouted_values = ["null","Null","NULL","true","True","TRUE","false","False","FALSE","YES","Yes","yes","NO","No","no","ON","On","on","OFF","Off","off"]
  if value in unquouted_values:
    unquouted = True

  # if is list [a,b,c] or dictionary {a: v, b: v2, c: v3}
  if re.match('^\w*\[.+\]\w*$', value) or re.match('^\w*\{.+\}\w*$', value):
    unquouted = True

  try:
    int(value)
    unquouted = True
  except ValueError:
    pass
  
  try:
    float(value)
    unquouted = True
  except ValueError:
    pass
  
  if not unquouted:
    value = value.replace("'","''")
    value = "'"+value+"'"
    
  return value

def replace_jaas_placeholder(name, security_enabled, conf_dir):
  if name.find('_JAAS_PLACEHOLDER') > -1:
    if security_enabled:
      return name.replace('_JAAS_PLACEHOLDER', '-Djava.security.auth.login.config=' + conf_dir + '/storm_jaas.conf')
    else:
      return name.replace('_JAAS_PLACEHOLDER', '')
  else:
    return name

storm_yaml_template = """{% for key, value in configurations|dictsort if not key.startswith('_') %}{{key}} : {{ escape_yaml_propetry(replace_jaas_placeholder(resource_management.core.source.InlineTemplate(value).get_content().strip(), security_enabled, conf_dir)) }}
{% endfor %}"""

def yaml_config_template(configurations):
  return InlineTemplate(storm_yaml_template, configurations=configurations,
                        extra_imports=[escape_yaml_propetry, replace_jaas_placeholder, resource_management,
                                       resource_management.core, resource_management.core.source])