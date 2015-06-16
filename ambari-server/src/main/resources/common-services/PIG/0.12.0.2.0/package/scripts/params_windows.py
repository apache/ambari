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
from resource_management import *

# server configurations
config = Script.get_config()

hdp_root = None
pig_home = None
pig_conf_dir = None
try:
  hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
  pig_home = os.environ['PIG_HOME']
  pig_conf_dir = os.path.join(pig_home,'conf')
except:
  pass

pig_properties = config['configurations']['pig-properties']['content']

if (('pig-log4j' in config['configurations']) and ('content' in config['configurations']['pig-log4j'])):
  log4j_props = config['configurations']['pig-log4j']['content']
else:
  log4j_props = None

hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
pig_user = hadoop_user
hdfs_user = hadoop_user
