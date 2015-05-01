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

from resource_management.libraries.script.script import Script
import os
from status_params import *

config = Script.get_config()

hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"], ".."))
oozie_root = os.environ['OOZIE_ROOT']
oozie_home = os.environ['OOZIE_HOME']
oozie_conf_dir = os.path.join(oozie_home,'conf')
oozie_user = hadoop_user
oozie_tmp_dir = "c:\\hadoop\\temp\\oozie"

oozie_env_cmd_template = config['configurations']['oozie-env']['content']
