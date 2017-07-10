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
import os
import resource_management

from ambari_commons.yaml_utils import escape_yaml_property
from resource_management.core.source import InlineTemplate
from resource_management.core.resources.system import File

def replace_jaas_placeholder(name, security_enabled, conf_dir):
  if name.find('_JAAS_PLACEHOLDER') > -1:
    if security_enabled:
      if name.find('Nimbus_JVM') > -1:
        return name.replace('_JAAS_PLACEHOLDER', '-Djava.security.auth.login.config=' + conf_dir + '/storm_jaas.conf -Djavax.security.auth.useSubjectCredsOnly=false')
      else:
        return name.replace('_JAAS_PLACEHOLDER', '-Djava.security.auth.login.config=' + conf_dir + '/storm_jaas.conf')
    else:
      return name.replace('_JAAS_PLACEHOLDER', '')
  else:
    return name

storm_yaml_template = """{% for key, value in configurations|dictsort if not key.startswith('_') %}{{key}} : {{ escape_yaml_property(replace_jaas_placeholder(resource_management.core.source.InlineTemplate(value).get_content().strip(), security_enabled, conf_dir)) }}
{% endfor %}"""

def yaml_config_template(configurations):
  return InlineTemplate(storm_yaml_template, configurations=configurations,
                        extra_imports=[escape_yaml_property, replace_jaas_placeholder, resource_management,
                                       resource_management.core, resource_management.core.source])

def yaml_config(filename, configurations = None, conf_dir = None, owner = None, group = None):
  import params
  config_content = InlineTemplate('''{% for key, value in configurations_dict|dictsort %}{{ key }}: {{ escape_yaml_property(resource_management.core.source.InlineTemplate(value).get_content()) }}
{% endfor %}''', configurations_dict=configurations, extra_imports=[escape_yaml_property, resource_management, resource_management.core, resource_management.core.source])

  File (os.path.join(params.conf_dir, filename),
        content = config_content,
        owner = owner,
        mode = "f"
  )
