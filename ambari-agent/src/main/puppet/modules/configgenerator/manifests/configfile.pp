#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

#
# Generates xml configs from the given key-value hash maps
#
# Config file format:
#
# <configuration>
#   <property>
#     <name>name1</name><value>value1</value>
#   </property>
#     ..
#   <property>
#     <name>nameN</name><value>valueN</value>
#   </property>
# </configuration>
#

define configgenerator::configfile ($configname=$title, $module, $properties) {
    $configcontent => inline_template( "
	  <configuration>
	  <% properties.each do |key,value| -%>
     <property>
	  <name><%=key %></name><value><%=value %></value>
     </property>
	  <% end -%>
	  </configuration>
	")
 
	file {'config':
       ensure  => present,
       content => $configcontent,
		path => "/etc/puppet/agent/modules/${module}/templates/${configname}",
     }
	) 
