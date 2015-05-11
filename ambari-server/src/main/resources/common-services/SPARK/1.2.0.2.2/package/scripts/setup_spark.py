#!/usr/bin/python
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

import sys
import fileinput
import shutil
import os
from resource_management import *
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell


def setup_spark(env, type, action = None):
  import params

  Directory([params.spark_pid_dir, params.spark_log_dir],
            owner=params.spark_user,
            group=params.user_group,
            recursive=True
  )
  if type == 'server' and action == 'config':
    params.HdfsDirectory(params.spark_hdfs_user_dir,
                       action="create",
                       owner=params.spark_user,
                       mode=0775
    )
    
  PropertiesFile(format("{spark_conf}/spark-defaults.conf"),
    properties = params.config['configurations']['spark-defaults'],
    key_value_delimiter = " ",               
  )

  # create spark-env.sh in etc/conf dir
  File(os.path.join(params.spark_conf, 'spark-env.sh'),
       owner=params.spark_user,
       group=params.spark_group,
       content=InlineTemplate(params.spark_env_sh)
  )

  #create log4j.properties in etc/conf dir
  File(os.path.join(params.spark_conf, 'log4j.properties'),
       owner=params.spark_user,
       group=params.spark_group,
       content=params.spark_log4j_properties
  )

  #create metrics.properties in etc/conf dir
  File(os.path.join(params.spark_conf, 'metrics.properties'),
       owner=params.spark_user,
       group=params.spark_group,
       content=InlineTemplate(params.spark_metrics_properties)
  )

  File(os.path.join(params.spark_conf, 'java-opts'),
       owner=params.spark_user,
       group=params.spark_group,
       content=params.spark_javaopts_properties
  )

  if params.is_hive_installed:
    XmlConfig("hive-site.xml",
              conf_dir=params.spark_conf,
              configurations=params.spark_hive_properties,
              owner=params.spark_user,
              group=params.spark_group,
              mode=0644)
