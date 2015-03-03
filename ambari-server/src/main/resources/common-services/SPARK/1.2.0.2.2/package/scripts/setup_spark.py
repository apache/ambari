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

  env.set_params(params)

  Directory([params.spark_pid_dir, params.spark_log_dir],
            owner=params.spark_user,
            group=params.user_group,
            recursive=True
  )
  if type == 'server':
    if action == 'start' or action == 'config':
      params.HdfsDirectory(params.spark_hdfs_user_dir,
                         action="create",
                         owner=params.spark_user,
                         mode=0775
      )

  file_path = params.spark_conf + '/spark-defaults.conf'
  create_file(file_path)

  write_properties_to_file(file_path, spark_properties(params))

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
    hive_config = get_hive_config()
    XmlConfig("hive-site.xml",
              conf_dir=params.spark_conf,
              configurations=hive_config,
              owner=params.spark_user,
              group=params.spark_group,
              mode=0644)

def get_hive_config():
  import params
  hive_conf_dict = dict()
  hive_conf_dict['hive.metastore.uris'] = params.config['configurations']['hive-site']['hive.metastore.uris']
  if params.security_enabled:
    hive_conf_dict['hive.metastore.sasl.enabled'] =  str(params.config['configurations']['hive-site']['hive.metastore.sasl.enabled']).lower()
    hive_conf_dict['hive.metastore.kerberos.keytab.file'] = params.config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']
    hive_conf_dict['hive.server2.authentication.spnego.principal'] =  params.config['configurations']['hive-site']['hive.server2.authentication.spnego.principal']
    hive_conf_dict['hive.server2.authentication.spnego.keytab'] = params.config['configurations']['hive-site']['hive.server2.authentication.spnego.keytab']
    hive_conf_dict['hive.metastore.kerberos.principal'] = params.config['configurations']['hive-site']['hive.metastore.kerberos.principal']
    hive_conf_dict['hive.server2.authentication.kerberos.principal'] = params.config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
    hive_conf_dict['hive.server2.authentication.kerberos.keytab'] =  params.config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']
    hive_conf_dict['hive.security.authorization.enabled']=  str(params.config['configurations']['hive-site']['hive.security.authorization.enabled']).lower()
    hive_conf_dict['hive.server2.enable.doAs'] =  str(params.config['configurations']['hive-site']['hive.server2.enable.doAs']).lower()

  return hive_conf_dict


def spark_properties(params):
  spark_dict = dict()

  all_spark_config  = params.config['configurations']['spark-defaults']
  #Add all configs unfiltered first to handle Custom case.
  spark_dict = all_spark_config.copy()

  spark_dict['spark.yarn.executor.memoryOverhead'] = params.spark_yarn_executor_memoryOverhead
  spark_dict['spark.yarn.driver.memoryOverhead'] = params.spark_yarn_driver_memoryOverhead
  spark_dict['spark.yarn.applicationMaster.waitTries'] = params.spark_yarn_applicationMaster_waitTries
  spark_dict['spark.yarn.scheduler.heartbeat.interval-ms'] = params.spark_yarn_scheduler_heartbeat_interval
  spark_dict['spark.yarn.max_executor.failures'] = params.spark_yarn_max_executor_failures
  spark_dict['spark.yarn.queue'] = params.spark_yarn_queue
  spark_dict['spark.yarn.containerLauncherMaxThreads'] = params.spark_yarn_containerLauncherMaxThreads
  spark_dict['spark.yarn.submit.file.replication'] = params.spark_yarn_submit_file_replication
  spark_dict['spark.yarn.preserve.staging.files'] = params.spark_yarn_preserve_staging_files

  # Hardcoded paramaters to be added to spark-defaults.conf
  spark_dict['spark.yarn.historyServer.address'] = params.spark_history_server_host + ':' + str(
    params.spark_history_ui_port)
  spark_dict['spark.yarn.services'] = 'org.apache.spark.deploy.yarn.history.YarnHistoryService'
  spark_dict['spark.history.provider'] = 'org.apache.spark.deploy.yarn.history.YarnHistoryProvider'
  spark_dict['spark.history.ui.port'] = params.spark_history_ui_port

  spark_dict['spark.driver.extraJavaOptions'] = params.spark_driver_extraJavaOptions
  spark_dict['spark.yarn.am.extraJavaOptions'] = params.spark_yarn_am_extraJavaOptions


  return spark_dict


def write_properties_to_file(file_path, value):
  for key in value:
    modify_config(file_path, key, value[key])


def modify_config(filepath, variable, setting):
  var_found = False
  already_set = False
  V = str(variable)
  S = str(setting)

  if ' ' in S:
    S = '%s' % S

  for line in fileinput.input(filepath, inplace=1):
    if not line.lstrip(' ').startswith('#') and '=' in line:
      _infile_var = str(line.split('=')[0].rstrip(' '))
      _infile_set = str(line.split('=')[1].lstrip(' ').rstrip())
      if var_found == False and _infile_var.rstrip(' ') == V:
        var_found = True
        if _infile_set.lstrip(' ') == S:
          already_set = True
        else:
          line = "%s %s\n" % (V, S)

    sys.stdout.write(line)

  if not var_found:
    with open(filepath, "a") as f:
      f.write("%s \t %s\n" % (V, S))
  elif already_set == True:
    pass
  else:
    pass

  return


def create_file(file_path):
  try:
    file = open(file_path, 'w')
    file.close()
  except:
    print('Unable to create file: ' + file_path)
    sys.exit(0)


def get_hdp_version():
  try:
    command = 'hdp-select status hadoop-client'
    return_code, hdp_output = shell.call(command, timeout=20)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute hdp-select command to retrieve the version.')

  if return_code != 0:
    raise Fail(
      'Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

  hdp_version = re.sub('hadoop-client - ', '', hdp_output)
  hdp_version = hdp_version.rstrip()
  match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', hdp_version)

  if match is None:
    raise Fail('Failed to get extracted version')

  return hdp_version
