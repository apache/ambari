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

from resource_management import *
import os

# server configurations
config = Script.get_config()

hdp_root = None
slider_home = None
slider_bin_dir = None
slider_conf_dir = None
storm_slider_conf_dir = None
try:
  hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
  slider_home = os.environ['SLIDER_HOME']
  slider_bin_dir = os.path.join(slider_home, 'bin')
  slider_conf_dir = os.path.join(slider_home, 'conf')
  storm_slider_conf_dir = os.path.join(os.environ['STORM_HOME'], 'conf')
except:
  pass

slider_home_dir = slider_home

hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
slider_user = hadoop_user
hdfs_user = hadoop_user
