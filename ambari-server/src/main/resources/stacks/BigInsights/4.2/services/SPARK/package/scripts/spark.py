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


def spark(env):
  import params

  env.set_params(params)

  Directory(params.spark_conf,
            owner=params.spark_user,
            create_parents = True,
            group=params.user_group
  )

  Directory([params.spark_pid_dir, params.spark_log_dir],
            owner=params.spark_user,
            group=params.user_group,
            mode=0775,
            create_parents = True
  )
  if type == 'server':
    if action == 'start' or action == 'config':
      params.HdfsResource(params.spark_hdfs_user_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.spark_user,
                         mode=0775
      )
      params.HdfsResource(None, action="execute")

  #file_path = params.spark_conf + '/spark-defaults.conf'
  #create_file(file_path)

  #write_properties_to_file(file_path, params.config['configurations']['spark-defaults'])
  configFile("spark-defaults.conf", template_name="spark-defaults.conf.j2")  

  # create spark-env.sh in conf dir
  File(os.path.join(params.spark_conf, 'spark-env.sh'),
       owner=params.spark_user,
       group=params.user_group,
       content=InlineTemplate(params.spark_env_sh)
  )

  # create log4j.properties in conf dir
  File(os.path.join(params.spark_conf, 'log4j.properties'),
       owner=params.spark_user,
       group=params.user_group,
       content=InlineTemplate(params.spark_log4j)
  )

  #create metrics.properties in conf dir
#  File(os.path.join(params.spark_conf, 'metrics.properties'),
#       owner=params.spark_user,
#       group=params.spark_group,
#       content=InlineTemplate(params.spark_metrics_properties)
#  )

  # create java-opts in etc/spark/conf dir for iop.version
  File(os.path.join(params.spark_conf, 'java-opts'),
       owner=params.spark_user,
       group=params.user_group,
       content=params.spark_javaopts_properties,
       mode=0644
  )

  if params.is_hive_installed:
    hive_config = get_hive_config()
    XmlConfig("hive-site.xml",
              conf_dir=params.spark_conf,
              configurations=hive_config,
              owner=params.spark_user,
              group=params.user_group,
              mode=0644)
def get_hive_config():
  import params
  # MUST CONVERT BOOLEANS TO LOWERCASE STRINGS
  hive_conf_dict = dict()
  hive_conf_dict['hive.metastore.uris'] = params.config['configurations']['hive-site']['hive.metastore.uris']
  hive_conf_dict['ambari.hive.db.schema.name'] = params.config['configurations']['hive-site']['ambari.hive.db.schema.name']
  hive_conf_dict['datanucleus.cache.level2.type'] = params.config['configurations']['hive-site']['datanucleus.cache.level2.type']
  #hive_conf_dict['fs.file.impl.disable.cache'] = str(params.config['configurations']['hive-site']['fs.file.impl.disable.cache']).lower()
  #hive_conf_dict['fs.hdfs.impl.disable.cache'] = str(params.config['configurations']['hive-site']['fs.hdfs.impl.disable.cache']).lower()
  hive_conf_dict['hive.auto.convert.join'] = str(params.config['configurations']['hive-site']['hive.auto.convert.join']).lower()
  hive_conf_dict['hive.auto.convert.join.noconditionaltask'] = str(params.config['configurations']['hive-site']['hive.auto.convert.join.noconditionaltask']).lower()
  hive_conf_dict['hive.auto.convert.join.noconditionaltask.size'] = params.config['configurations']['hive-site']['hive.auto.convert.join.noconditionaltask.size']
  hive_conf_dict['hive.auto.convert.sortmerge.join'] = str(params.config['configurations']['hive-site']['hive.auto.convert.sortmerge.join']).lower()
  #hive_conf_dict['hive.auto.convert.sortmerge.join.noconditionaltask'] = str(params.config['configurations']['hive-site']['hive.auto.convert.sortmerge.join.noconditionaltask']).lower()
  hive_conf_dict['hive.auto.convert.sortmerge.join.to.mapjoin'] = str(params.config['configurations']['hive-site']['hive.auto.convert.sortmerge.join.to.mapjoin']).lower()
  hive_conf_dict['hive.cbo.enable'] = str(params.config['configurations']['hive-site']['hive.cbo.enable']).lower()
  hive_conf_dict['hive.cli.print.header'] = str(params.config['configurations']['hive-site']['hive.cli.print.header']).lower()
  hive_conf_dict['hive.cluster.delegation.token.store.class'] = params.config['configurations']['hive-site']['hive.cluster.delegation.token.store.class']
  hive_conf_dict['hive.cluster.delegation.token.store.zookeeper.connectString'] = params.config['configurations']['hive-site']['hive.cluster.delegation.token.store.zookeeper.connectString']
  hive_conf_dict['hive.cluster.delegation.token.store.zookeeper.znode'] = params.config['configurations']['hive-site']['hive.cluster.delegation.token.store.zookeeper.znode']
  hive_conf_dict['hive.compactor.abortedtxn.threshold'] = params.config['configurations']['hive-site']['hive.compactor.abortedtxn.threshold']
  hive_conf_dict['hive.compactor.check.interval'] = params.config['configurations']['hive-site']['hive.compactor.check.interval']
  hive_conf_dict['hive.compactor.delta.num.threshold'] = params.config['configurations']['hive-site']['hive.compactor.delta.num.threshold']
  hive_conf_dict['hive.compactor.delta.pct.threshold'] = params.config['configurations']['hive-site']['hive.compactor.delta.pct.threshold']
  hive_conf_dict['hive.compactor.initiator.on'] = str(params.config['configurations']['hive-site']['hive.compactor.initiator.on']).lower()
  hive_conf_dict['hive.compactor.worker.threads'] = params.config['configurations']['hive-site']['hive.compactor.worker.threads']
  hive_conf_dict['hive.compactor.worker.timeout'] = params.config['configurations']['hive-site']['hive.compactor.worker.timeout']
  hive_conf_dict['hive.compute.query.using.stats'] = str(params.config['configurations']['hive-site']['hive.compute.query.using.stats']).lower()
  hive_conf_dict['hive.conf.restricted.list'] = params.config['configurations']['hive-site']['hive.conf.restricted.list']
  hive_conf_dict['hive.enforce.bucketing'] = str(params.config['configurations']['hive-site']['hive.enforce.bucketing']).lower()
  hive_conf_dict['hive.enforce.sorting'] = str(params.config['configurations']['hive-site']['hive.enforce.sorting']).lower()
  hive_conf_dict['hive.enforce.sortmergebucketmapjoin'] = str(params.config['configurations']['hive-site']['hive.enforce.sortmergebucketmapjoin']).lower()
  hive_conf_dict['hive.exec.compress.intermediate'] = str(params.config['configurations']['hive-site']['hive.exec.compress.intermediate']).lower()
  hive_conf_dict['hive.exec.compress.output'] = str(params.config['configurations']['hive-site']['hive.exec.compress.output']).lower()
  hive_conf_dict['hive.exec.dynamic.partition'] = str(params.config['configurations']['hive-site']['hive.exec.dynamic.partition']).lower()
  hive_conf_dict['hive.exec.dynamic.partition.mode'] = params.config['configurations']['hive-site']['hive.exec.dynamic.partition.mode']
  hive_conf_dict['hive.exec.max.created.files'] = params.config['configurations']['hive-site']['hive.exec.max.created.files']
  hive_conf_dict['hive.exec.max.dynamic.partitions'] = params.config['configurations']['hive-site']['hive.exec.max.dynamic.partitions']
  hive_conf_dict['hive.exec.max.dynamic.partitions.pernode'] = params.config['configurations']['hive-site']['hive.exec.max.dynamic.partitions.pernode']
  hive_conf_dict['hive.exec.orc.compression.strategy'] = params.config['configurations']['hive-site']['hive.exec.orc.compression.strategy']
  hive_conf_dict['hive.exec.orc.default.compress'] = params.config['configurations']['hive-site']['hive.exec.orc.default.compress']
  hive_conf_dict['hive.exec.orc.default.stripe.size'] = params.config['configurations']['hive-site']['hive.exec.orc.default.stripe.size']
  hive_conf_dict['hive.exec.parallel'] = str(params.config['configurations']['hive-site']['hive.exec.parallel']).lower()
  hive_conf_dict['hive.exec.parallel.thread.number'] = params.config['configurations']['hive-site']['hive.exec.parallel.thread.number']
  hive_conf_dict['hive.exec.reducers.bytes.per.reducer'] = params.config['configurations']['hive-site']['hive.exec.reducers.bytes.per.reducer']
  hive_conf_dict['hive.exec.reducers.max'] = params.config['configurations']['hive-site']['hive.exec.reducers.max']
  hive_conf_dict['hive.exec.scratchdir'] = params.config['configurations']['hive-site']['hive.exec.scratchdir']
  hive_conf_dict['hive.exec.submit.local.task.via.child'] = str(params.config['configurations']['hive-site']['hive.exec.submit.local.task.via.child']).lower()
  hive_conf_dict['hive.exec.submitviachild'] = str(params.config['configurations']['hive-site']['hive.exec.submitviachild']).lower()
  hive_conf_dict['hive.execution.engine'] = params.config['configurations']['hive-site']['hive.execution.engine']
  hive_conf_dict['hive.fetch.task.aggr'] = str(params.config['configurations']['hive-site']['hive.fetch.task.aggr']).lower()
  hive_conf_dict['hive.fetch.task.conversion'] = params.config['configurations']['hive-site']['hive.fetch.task.conversion']
  hive_conf_dict['hive.fetch.task.conversion.threshold'] = params.config['configurations']['hive-site']['hive.fetch.task.conversion.threshold']
  #hive_conf_dict['hive.heapsize'] = params.config['configurations']['hive-env']['hive.heapsize']
  hive_conf_dict['hive.limit.optimize.enable'] = str(params.config['configurations']['hive-site']['hive.limit.optimize.enable']).lower()
  hive_conf_dict['hive.limit.pushdown.memory.usage'] = params.config['configurations']['hive-site']['hive.limit.pushdown.memory.usage']
  hive_conf_dict['hive.metastore.authorization.storage.checks'] = str(params.config['configurations']['hive-site']['hive.metastore.authorization.storage.checks']).lower()
  hive_conf_dict['hive.metastore.cache.pinobjtypes'] = params.config['configurations']['hive-site']['hive.metastore.cache.pinobjtypes']
  # The following two parameters are hardcoded to workaround an issue
  # interpreting the syntax for the property values (unit like s for seconds
  # result in an error)
  #hive_conf_dict['hive.metastore.client.connect.retry.delay'] = params.config['configurations']['hive-site']['hive.metastore.client.connect.retry.delay']
  #hive_conf_dict['hive.metastore.client.socket.timeout'] = params.config['configurations']['hive-site']['hive.metastore.client.socket.timeout']
  hive_conf_dict['hive.metastore.client.connect.retry.delay'] = '5'
  hive_conf_dict['hive.metastore.client.socket.timeout'] = '1800'
  hive_conf_dict['hive.metastore.connect.retries'] = params.config['configurations']['hive-site']['hive.metastore.connect.retries']
  hive_conf_dict['hive.metastore.execute.setugi'] = str(params.config['configurations']['hive-site']['hive.metastore.execute.setugi']).lower()
  hive_conf_dict['hive.metastore.failure.retries'] = params.config['configurations']['hive-site']['hive.metastore.failure.retries']
  hive_conf_dict['hive.metastore.pre.event.listeners'] = params.config['configurations']['hive-site']['hive.metastore.pre.event.listeners']
  hive_conf_dict['hive.metastore.sasl.enabled'] = str(params.config['configurations']['hive-site']['hive.metastore.sasl.enabled']).lower()
  hive_conf_dict['hive.metastore.server.max.threads'] = params.config['configurations']['hive-site']['hive.metastore.server.max.threads']
  hive_conf_dict['hive.metastore.warehouse.dir'] = params.config['configurations']['hive-site']['hive.metastore.warehouse.dir']
  hive_conf_dict['hive.orc.compute.splits.num.threads'] = params.config['configurations']['hive-site']['hive.orc.compute.splits.num.threads']
  hive_conf_dict['hive.orc.splits.include.file.footer'] = str(params.config['configurations']['hive-site']['hive.orc.splits.include.file.footer']).lower()
  hive_conf_dict['hive.security.authenticator.manager'] = params.config['configurations']['hive-site']['hive.security.authenticator.manager']
  hive_conf_dict['hive.security.metastore.authenticator.manager'] = params.config['configurations']['hive-site']['hive.security.metastore.authenticator.manager']
  hive_conf_dict['hive.security.metastore.authorization.auth.reads'] = str(params.config['configurations']['hive-site']['hive.security.metastore.authorization.auth.reads']).lower()
  hive_conf_dict['hive.security.metastore.authorization.manager'] = params.config['configurations']['hive-site']['hive.security.metastore.authorization.manager']
  hive_conf_dict['hive.server2.allow.user.substitution'] = str(params.config['configurations']['hive-site']['hive.server2.allow.user.substitution']).lower()
  hive_conf_dict['hive.server2.logging.operation.enabled'] = str(params.config['configurations']['hive-site']['hive.server2.logging.operation.enabled']).lower()
  hive_conf_dict['hive.server2.logging.operation.log.location'] = '/tmp/spark/operation_logs'
  hive_conf_dict['hive.server2.support.dynamic.service.discovery'] = 'false'
  hive_conf_dict['hive.server2.table.type.mapping'] = params.config['configurations']['hive-site']['hive.server2.table.type.mapping']
  hive_conf_dict['hive.server2.thrift.http.path'] = params.config['configurations']['hive-site']['hive.server2.thrift.http.path']
  hive_conf_dict['hive.server2.thrift.max.worker.threads'] = params.config['configurations']['hive-site']['hive.server2.thrift.max.worker.threads']
  hive_conf_dict['hive.server2.thrift.port'] = params.spark_thriftserver_port
  hive_conf_dict['hive.server2.thrift.sasl.qop'] = params.config['configurations']['hive-site']['hive.server2.thrift.sasl.qop']
  hive_conf_dict['hive.server2.transport.mode'] = params.config['configurations']['hive-site']['hive.server2.transport.mode']
  hive_conf_dict['hive.server2.use.SSL'] = str(params.config['configurations']['hive-site']['hive.server2.use.SSL']).lower()
  hive_conf_dict['hive.server2.zookeeper.namespace'] = params.config['configurations']['hive-site']['hive.server2.zookeeper.namespace']
  hive_conf_dict['hive.smbjoin.cache.rows'] = params.config['configurations']['hive-site']['hive.smbjoin.cache.rows']
  hive_conf_dict['hive.stats.autogather'] = str(params.config['configurations']['hive-site']['hive.stats.autogather']).lower()
  hive_conf_dict['hive.stats.dbclass'] = params.config['configurations']['hive-site']['hive.stats.dbclass']
  hive_conf_dict['hive.stats.fetch.column.stats'] = params.config['configurations']['hive-site']['hive.stats.fetch.column.stats']
  hive_conf_dict['hive.stats.fetch.partition.stats'] = str(params.config['configurations']['hive-site']['hive.stats.fetch.partition.stats']).lower()
  hive_conf_dict['hive.support.concurrency'] = str(params.config['configurations']['hive-site']['hive.support.concurrency']).lower()
  hive_conf_dict['hive.txn.manager'] = params.config['configurations']['hive-site']['hive.txn.manager']
  hive_conf_dict['hive.txn.max.open.batch'] = params.config['configurations']['hive-site']['hive.txn.max.open.batch']
  hive_conf_dict['hive.txn.timeout'] = params.config['configurations']['hive-site']['hive.txn.timeout']
  hive_conf_dict['hive.vectorized.execution.enabled'] = str(params.config['configurations']['hive-site']['hive.vectorized.execution.enabled']).lower()
  hive_conf_dict['hive.vectorized.execution.reduce.enabled'] = str(params.config['configurations']['hive-site']['hive.vectorized.execution.reduce.enabled']).lower()
  hive_conf_dict['hive.vectorized.groupby.checkinterval'] = params.config['configurations']['hive-site']['hive.vectorized.groupby.checkinterval']
  hive_conf_dict['hive.vectorized.groupby.flush.percent'] = params.config['configurations']['hive-site']['hive.vectorized.groupby.flush.percent']
  hive_conf_dict['hive.vectorized.groupby.maxentries'] = params.config['configurations']['hive-site']['hive.vectorized.groupby.maxentries']
  hive_conf_dict['hive.zookeeper.client.port'] = params.config['configurations']['hive-site']['hive.zookeeper.client.port']
  hive_conf_dict['hive.zookeeper.namespace'] = params.config['configurations']['hive-site']['hive.zookeeper.namespace']
  hive_conf_dict['hive.zookeeper.quorum'] = params.config['configurations']['hive-site']['hive.zookeeper.quorum']
  hive_conf_dict['javax.jdo.option.ConnectionDriverName'] = params.config['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName']
  hive_conf_dict['javax.jdo.option.ConnectionURL'] = params.config['configurations']['hive-site']['javax.jdo.option.ConnectionURL']
  hive_conf_dict['javax.jdo.option.ConnectionUserName'] = params.config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']

# Comment out parameters not found in configurations dictionary
# hive_conf_dict['hive.prewarm.enabled'] = params.config['configurations']['hive-site']['hive.prewarm.enabled']
# hive_conf_dict['hive.prewarm.numcontainers'] = params.config['configurations']['hive-site']['hive.prewarm.numcontainers']
#  hive_conf_dict['hive.user.install.directory'] = params.config['configurations']['hive-site']['hive.user.install.directory']

  if params.security_enabled:
    hive_conf_dict['hive.metastore.sasl.enabled'] =  str(params.config['configurations']['hive-site']['hive.metastore.sasl.enabled']).lower()
    hive_conf_dict['hive.server2.authentication'] = params.config['configurations']['hive-site']['hive.server2.authentication']
    hive_conf_dict['hive.metastore.kerberos.keytab.file'] = params.config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']
    hive_conf_dict['hive.metastore.kerberos.principal'] = params.config['configurations']['hive-site']['hive.metastore.kerberos.principal']
    hive_conf_dict['hive.server2.authentication.spnego.principal'] =  params.config['configurations']['hive-site']['hive.server2.authentication.spnego.principal']
    hive_conf_dict['hive.server2.authentication.spnego.keytab'] = params.config['configurations']['hive-site']['hive.server2.authentication.spnego.keytab']
    hive_conf_dict['hive.server2.authentication.kerberos.principal'] = params.config['configurations']['hive-site']['hive.server2.authentication.kerberos.principal']
    hive_conf_dict['hive.server2.authentication.kerberos.keytab'] =  params.config['configurations']['hive-site']['hive.server2.authentication.kerberos.keytab']
    hive_conf_dict['hive.security.authorization.enabled']=  str(params.config['configurations']['hive-site']['hive.security.authorization.enabled']).lower()
    hive_conf_dict['hive.security.authorization.manager'] = str(params.config['configurations']['hive-site']['hive.security.authorization.manager'])
    hive_conf_dict['hive.server2.enable.doAs'] =  str(params.config['configurations']['hive-site']['hive.server2.enable.doAs']).lower()

  if hive_conf_dict['hive.server2.use.SSL']:
    if params.config['configurations']['hive-site']['hive.server2.keystore.path'] is not None:
      hive_conf_dict['hive.server2.keystore.path']= str(params.config['configurations']['hive-site']['hive.server2.keystore.path'])
    if params.config['configurations']['hive-site']['hive.server2.keystore.password'] is not None:
      hive_conf_dict['hive.server2.keystore.password']= str(params.config['configurations']['hive-site']['hive.server2.keystore.password'])

  # convert remaining numbers to strings
  for key, value in hive_conf_dict.iteritems():
    hive_conf_dict[key] = str(value)

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
  spark_dict['spark.history.ui.port'] = params.spark_history_ui_port
  spark_dict['spark.eventLog.enabled'] = str(params.spark_eventlog_enabled).lower()
  spark_dict['spark.eventLog.dir'] = params.spark_eventlog_dir
  spark_dict['spark.history.fs.logDirectory'] = params.spark_eventlog_dir
  spark_dict['spark.yarn.jar'] = params.spark_yarn_jar

  spark_dict['spark.driver.extraJavaOptions'] = params.spark_driver_extraJavaOptions
  spark_dict['spark.yarn.am.extraJavaOptions'] = params.spark_yarn_am_extraJavaOptions

  # convert remaining numbers to strings
  for key, value in spark_dict.iteritems():
    spark_dict[key] = str(value)

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

def configFile(name, template_name=None):
  import params

  File(format("{spark_conf}/{name}"),
       content=Template(template_name),
       owner=params.spark_user,
       group=params.user_group,
       mode=0644
  )

def get_iop_version():
  try:
    command = 'iop-select status hadoop-client'
    return_code, iop_output = shell.call(command, timeout=20)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute iop-select command to retrieve the version.')

  if return_code != 0:
    raise Fail(
      'Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

  iop_version = re.sub('hadoop-client - ', '', iop_output)
  match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+', iop_version)

  if match is None:
    raise Fail('Failed to get extracted version')

  return iop_version
