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
from resource_management import *
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.script.script import Script
import os

# temp directory
exec_tmp_dir = Script.get_tmp_dir()

# server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()

r4ml_home = format("{stack_root}/current/r4ml-client")

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
stack_version = format_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
stack_name = default("/hostLevelParams/stack_name", None)

java_home = config['hostLevelParams']['java_home']
r4ml_conf_dir = "/etc/r4ml/conf"
if stack_version and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version):
  r4ml_conf_dir = format("{stack_root}/current/r4ml-client/conf")

# environment variables
spark_home = os.path.join(stack_root, "current", 'spark2-client')
spark_driver_memory = "4G"
spark_submit_args = "--num-executors 4 sparkr-shell"
r4ml_auto_start = 0
Renviron_template = config['configurations']['r4ml-env']['Renviron']

# rpm links
epel = ""
centos = ""
if System.get_instance().os_family == "redhat" :
  if System.get_instance().os_major_version == "7" :
    epel = "https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm"
    if System.get_instance().machine == "x86_64" :
      centos = "http://mirror.centos.org/centos/7/os/x86_64/Packages/"
  else :
    epel = "https://dl.fedoraproject.org/pub/epel/epel-release-latest-6.noarch.rpm"
    if System.get_instance().machine == "x86_64" :
      centos = "http://mirror.centos.org/centos/6/os/x86_64/Packages/"

# local R and R packages baseurl
baseurl = config['configurations']['r4ml-env']['Baseurl for local install of R and R packages dependencies']
rrepo = "/etc/yum.repos.d/localr.repo"

# systemml jar path
systemml_jar = os.path.join(stack_root, "current", "systemml-client", "lib", "systemml.jar")
if not os.path.isfile(systemml_jar) or not os.access(systemml_jar, os.R_OK) :
  systemml_jar = ""

smokeuser = config['configurations']['cluster-env']['smokeuser']