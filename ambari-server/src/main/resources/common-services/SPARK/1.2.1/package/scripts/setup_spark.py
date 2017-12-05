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

import os

from resource_management.core.source import InlineTemplate
from resource_management.core.resources.system import Directory, File
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import lzo_utils
from resource_management.libraries.resources import PropertiesFile
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.resources import HdfsResource
from resource_management.libraries.resources import XmlConfig


def setup_spark(env, type, upgrade_type=None, action=None, config_dir=None):
  """
  :param env: Python environment
  :param type: Spark component type
  :param upgrade_type: If in a stack upgrade, either UPGRADE_TYPE_ROLLING or UPGRADE_TYPE_NON_ROLLING
  :param action: Action to perform, such as generate configs
  :param config_dir: Optional config directory to write configs to.
  """

  import params

  # ensure that matching LZO libraries are installed for Spark
  lzo_utils.install_lzo_if_needed()

  if config_dir is None:
    config_dir = params.spark_conf

  Directory([params.spark_pid_dir, params.spark_log_dir],
            owner=params.spark_user,
            group=params.user_group,
            mode=0775,
            create_parents = True,
            cd_access = 'a',
  )
  if type == 'server' and action == 'config':
    params.HdfsResource(params.spark_hdfs_user_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.spark_user,
                       mode=0775
    )
    params.HdfsResource(None, action="execute")

  PropertiesFile(os.path.join(config_dir, "spark-defaults.conf"),
    properties = params.config['configurations']['spark-defaults'],
    key_value_delimiter = " ",
    owner=params.spark_user,
    group=params.spark_group,
    mode=0644
  )

  # create spark-env.sh in etc/conf dir
  File(os.path.join(config_dir, 'spark-env.sh'),
       owner=params.spark_user,
       group=params.spark_group,
       content=InlineTemplate(params.spark_env_sh),
       mode=0644,
  )

  #create log4j.properties in etc/conf dir
  File(os.path.join(config_dir, 'log4j.properties'),
       owner=params.spark_user,
       group=params.spark_group,
       content=params.spark_log4j_properties,
       mode=0644,
  )

  #create metrics.properties in etc/conf dir
  File(os.path.join(config_dir, 'metrics.properties'),
       owner=params.spark_user,
       group=params.spark_group,
       content=InlineTemplate(params.spark_metrics_properties),
       mode=0644
  )

  Directory(params.spark_logs_dir,
       owner=params.spark_user,
       group=params.spark_group,
       mode=0755,
  )

  if params.is_hive_installed:
    XmlConfig("hive-site.xml",
          conf_dir=config_dir,
          configurations=params.spark_hive_properties,
          owner=params.spark_user,
          group=params.spark_group,
          mode=0644)

  if params.has_spark_thriftserver:
    PropertiesFile(params.spark_thrift_server_conf_file,
      properties = params.config['configurations']['spark-thrift-sparkconf'],
      owner = params.hive_user,
      group = params.user_group,
      key_value_delimiter = " ",
      mode=0644
    )

  effective_version = params.version if upgrade_type is not None else params.version_for_stack_feature_checks
  if effective_version:
    effective_version = format_stack_version(effective_version)

  if check_stack_feature(StackFeature.SPARK_JAVA_OPTS_SUPPORT, effective_version):
    File(os.path.join(params.spark_conf, 'java-opts'),
      owner=params.spark_user,
      group=params.spark_group,
      content=InlineTemplate(params.spark_javaopts_properties),
      mode=0644
    )
  else:
    File(os.path.join(params.spark_conf, 'java-opts'),
      action="delete"
    )

  if params.spark_thrift_fairscheduler_content and check_stack_feature(StackFeature.SPARK_16PLUS, effective_version):
    # create spark-thrift-fairscheduler.xml
    File(os.path.join(config_dir,"spark-thrift-fairscheduler.xml"),
      owner=params.spark_user,
      group=params.spark_group,
      mode=0755,
      content=InlineTemplate(params.spark_thrift_fairscheduler_content)
    )
