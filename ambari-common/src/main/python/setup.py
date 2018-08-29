'''
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
'''

from setuptools import setup

setup(
  name = "ambari-commons",
  version = "dev-3.0.0",
  author = "Apache Software Foundation",
  author_email = "dev@ambari.apache.org",
  description = ("Framework for provison/manage/monitor Hadoop clusters"),
  license = "AP2",
  keywords = "hadoop, ambari",
  url = "https://ambari.apache.org",
  packages=['ambari_commons', 'ambari_jinja2', 'ambari_simplejson', 'ambari_stomp', 'ambari_ws4py', 'pluggable_stack_definition','resource_management', 'urlinfo_processor'],
  long_description="The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. "
                   "Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs."
)