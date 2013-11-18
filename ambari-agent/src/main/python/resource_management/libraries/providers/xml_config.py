#!/usr/bin/env python2.6
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
    conf_dir = self.resource.conf_dir
    
    # |e - for html-like escaping of <,>,',"
    config_content = InlineTemplate('''<!--{{time.asctime(time.localtime())}}-->
    <configuration>
    {% for key, value in configurations_dict.items() %}
    <property>
      <name>{{ key|e }}</name>
      <value>{{ value|e }}</value>
    </property>
    {% endfor %}
  </configuration>''', extra_imports=[time], configurations_dict=self.resource.configurations)
   
  
    self.log.debug(format("Generating config: {conf_dir}/{filename}"))
    
    with Environment.get_instance_copy() as env:
      File (format("{conf_dir}/{filename}"),
        content = config_content,
        owner = self.resource.owner,
        group = self.resource.group,
        mode = self.resource.mode
      )
    env.run()
