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

import time
from resource_management import *

class XmlConfigProvider(Provider):
  def action_create(self):
    filename = self.resource.filename
    xml_config_provider_config_dir = self.resource.conf_dir
    
    # |e - for html-like escaping of <,>,',"
    config_content = InlineTemplate('''<!--{{time.asctime(time.localtime())}}-->
    <configuration>
    {% for key, value in configurations_dict|dictsort %}
    <property>
      <name>{{ key|e }}</name>
      <value>{{ value|e }}</value>
      {%- if not configuration_attrs is none -%}
      {%- for attrib_name, attrib_occurances in  configuration_attrs.items() -%}
      {%- for property_name, attrib_value in  attrib_occurances.items() -%}
      {% if property_name == key and attrib_name %}
      <{{attrib_name|e}}>{{attrib_value|e}}</{{attrib_name|e}}>
      {%- endif -%}
      {%- endfor -%}
      {%- endfor -%}
      {%- endif %}
    </property>
    {% endfor %}
  </configuration>''', extra_imports=[time], configurations_dict=self.resource.configurations,
                                    configuration_attrs=self.resource.configuration_attributes)
   
  
    Logger.info(format("Generating config: {xml_config_provider_config_dir}/{filename}"))
    
    with Environment.get_instance_copy() as env:
      File (format("{xml_config_provider_config_dir}/{filename}"),
        content = config_content,
        owner = self.resource.owner,
        group = self.resource.group,
        mode = self.resource.mode,
        encoding = self.resource.encoding
      )
