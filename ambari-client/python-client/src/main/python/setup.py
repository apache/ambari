#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


from setuptools import setup, find_packages

from sys import version_info, platform

if version_info[:2] > (2, 5):
    install_requires = []
else:
    install_requires = ['simplejson >= 2.0.0']

# Python 2.6 and below requires argparse
if version_info[:2] < (2, 7):
    install_requires += ['argparse']

setup(
  name = 'ambari_client',
  author_email = "ambari-dev@incubator.apache.org",
  version = "1.0.3-SNAPSHOT",
  packages = ['ambari_client'],
  install_requires = install_requires,
  description = 'Ambari python REST API client',
  license = 'Apache License 2.0'
)
