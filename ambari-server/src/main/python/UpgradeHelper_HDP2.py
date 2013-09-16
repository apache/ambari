#!/usr/bin/env python

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

import optparse
from pprint import pprint
import sys
import datetime
import os.path
import logging
import shutil
import json
import subprocess
import urllib
import time


# action commands
GET_MR_MAPPING_ACTION = "save-mr-mapping"
DELETE_MR_ACTION = "delete-mr"
ADD_YARN_MR2_ACTION = "add-yarn-mr2"
MODIFY_CONFIG_ACTION = "update-configs"
BACKUP_CONFIG_ACTION = "backup-configs"
INSTALL_YARN_MR2_ACTION = "install-yarn-mr2"
VALID_ACTIONS = ', '.join([GET_MR_MAPPING_ACTION, DELETE_MR_ACTION, ADD_YARN_MR2_ACTION, MODIFY_CONFIG_ACTION,
                           INSTALL_YARN_MR2_ACTION, BACKUP_CONFIG_ACTION])

MR_MAPPING_FILE = "mr_mapping"
UPGRADE_LOG_FILE = "upgrade_log"
CAPACITY_SCHEDULER_TAG = "capacity-scheduler"
MAPRED_QUEUE_ACLS_TAG = "mapred-queue-acls"
MAPRED_SITE_TAG = "mapred-site"
GLOBAL_TAG = "global"
HDFS_SITE_TAG = "hdfs-site"
CORE_SITE_TAG = "core-site"
YARN_SITE_TAG = "yarn-site"
REPLACE_JH_HOST_NAME_TAG = "REPLACE_JH_HOST"
REPLACE_RM_HOST_NAME_TAG = "REPLACE_RM_HOST"
REPLACE_WITH_TAG = "REPLACE_WITH_"

AUTH_FORMAT = '{0}:{1}'
URL_FORMAT = 'http://{0}:8080/api/v1/clusters/{1}'

logger = logging.getLogger()

CAPACITY_SCHEDULER = {
  "yarn.scheduler.capacity.maximum-am-resource-percent": "0.2",
  "yarn.scheduler.capacity.maximum-applications": "10000",
  "yarn.scheduler.capacity.root.acl_administer_queues": "*",
  "yarn.scheduler.capacity.root.capacity": "100",
  "yarn.scheduler.capacity.root.default.acl_administer_jobs": "*",
  "yarn.scheduler.capacity.root.default.acl_submit_jobs": "*",
  "yarn.scheduler.capacity.root.default.capacity": "100",
  "yarn.scheduler.capacity.root.default.maximum-capacity": "100",
  "yarn.scheduler.capacity.root.default.state": "RUNNING",
  "yarn.scheduler.capacity.root.default.user-limit-factor": "1",
  "yarn.scheduler.capacity.root.queues": "default",
  "yarn.scheduler.capacity.root.unfunded.capacity": "50"}

MAPRED_QUEUE_ACLS = {
  "mapred.queue.default.acl-administer-jobs": "*", "mapred.queue.default.acl-submit-job": "*"}

MAPRED_SITE = {
  "mapred.hosts": "REPLACE_WITH_",
  "mapred.hosts.exclude": "REPLACE_WITH_",
  "mapred.jobtracker.maxtasks.per.job": "REPLACE_WITH_",
  "mapred.jobtracker.taskScheduler": "REPLACE_WITH_",
  "mapred.task.tracker.task-controller": "REPLACE_WITH_",
  "mapred.userlog.retain.hours": "REPLACE_WITH_",
  "mapreduce.admin.map.child.java.opts": "-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN",
  "mapreduce.admin.reduce.child.java.opts": "-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN",
  "mapreduce.admin.user.env": "LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`",
  "mapreduce.am.max-attempts": "2",
  "mapreduce.application.classpath": "$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*,$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*",
  "mapreduce.framework.name": "yarn",
  "mapreduce.job.reduce.slowstart.completedmaps": "0.05",
  "mapreduce.jobhistory.address": "REPLACE_JH_HOST:10020",
  "mapreduce.jobhistory.done-dir": "/mr-history/done",
  "mapreduce.jobhistory.intermediate-done-dir": "/mr-history/tmp",
  "mapreduce.jobhistory.webapp.address": "REPLACE_JH_HOST:19888",
  "mapreduce.jobtracker.system.dir": "/mapred/system",
  "mapreduce.map.java.opts": "-Xmx320m",
  "mapreduce.map.log.level": "INFO",
  "mapreduce.map.memory.mb": "1536",
  "mapreduce.map.sort.spill.percent": "0.1",
  "mapreduce.map.speculative": "false",
  "mapreduce.output.fileoutputformat.compress.type": "BLOCK",
  "mapreduce.reduce.input.buffer.percent": "0.0",
  "mapreduce.reduce.java.opts": "-Xmx756m",
  "mapreduce.reduce.log.level": "INFO",
  "mapreduce.reduce.memory.mb": "2048",
  "mapreduce.reduce.shuffle.input.buffer.percent": "0.7",
  "mapreduce.reduce.shuffle.merge.percent": "0.66",
  "mapreduce.reduce.shuffle.parallelcopies": "30",
  "mapreduce.reduce.speculative": "false",
  "mapreduce.shuffle.port": "13562",
  "mapreduce.task.io.sort.factor": "100",
  "mapreduce.task.io.sort.mb": "200",
  "mapreduce.task.timeout": "600000",
  "mapreduce.tasktracker.healthchecker.script.path": "file:////mapred/jobstatus",
  "mapreduce.tasktracker.map.tasks.maximum": "4",
  "yarn.app.mapreduce.am.admin-command-opts": "-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN",
  "yarn.app.mapreduce.am.command-opts": "-Xmx756m",
  "yarn.app.mapreduce.am.log.level": "INFO",
  "yarn.app.mapreduce.am.resource.mb": "1024",
  "yarn.app.mapreduce.am.staging-dir": "/user"
}

GLOBAL = {
  "apache_artifacts_download_url": "",
  "datanode_du_reserved": "1",
  "dfs_block_local_path_access_user": "hbase",
  "dfs_datanode_address": "REPLACE_WITH_dfs_datanode_address",
  "dfs_datanode_data_dir": "REPLACE_WITH_dfs_data_dir",
  "dfs_datanode_data_dir_perm": "750",
  "dfs_datanode_failed_volume_tolerated": "0",
  "dfs_datanode_http_address": "REPLACE_WITH_",
  "dfs_exclude": "dfs.exclude",
  "dfs_namenode_checkpoint_dir": "REPLACE_WITH_fs_checkpoint_dir",
  "dfs_namenode_checkpoint_period": "21600",
  "dfs_namenode_name_dir": "REPLACE_WITH_dfs_name_dir",
  "dfs_replication": "3",
  "dfs_webhdfs_enabled": "true",
  "dtnode_heapsize": "1024m",
  "fs_checkpoint_size": "0.5",
  "ganglia_runtime_dir": "REPLACE_WITH_",
  "gmetad_user": "REPLACE_WITH_",
  "gmond_user": "REPLACE_WITH_",
  "gpl_artifacts_download_url": "",
  "hadoop_conf_dir": "REPLACE_WITH_",
  "hadoop_heapsize": "1024",
  "hadoop_pid_dir_prefix": "REPLACE_WITH_",
  "hbase_conf_dir": "REPLACE_WITH_",
  "hbase_user": "REPLACE_WITH_",
  "hcat_conf_dir": "REPLACE_WITH_",
  "hcat_user": "REPLACE_WITH_",
  "hdfs_enable_shortcircuit_read": "true",
  "hdfs_log_dir_prefix": "REPLACE_WITH_",
  "hdfs_user": "REPLACE_WITH_",
  "hive_user": "REPLACE_WITH_",
  "java64_home": "REPLACE_WITH_",
  "mapred_hosts_exclude": "mapred.exclude",
  "mapred_hosts_include": "mapred.include",
  "mapred_jobstatus_dir": "REPLACE_WITH_",
  "mapred_log_dir_prefix": "/var/log/hadoop-mapreduce",
  "mapred_pid_dir_prefix": "/var/run/hadoop-mapreduce",
  "mapred_user": "REPLACE_WITH_",
  "mapreduce_jobtracker_system_dir": "REPLACE_WITH_mapred_system_dir",
  "mapreduce_map_memory_mb": "1536",
  "mapreduce_reduce_memory_mb": "2048",
  "mapreduce_task_io_sort_mb": "200",
  "mapreduce_tasktracker_map_tasks_maximum": "4",
  "mapreduce_userlog_retainhours": "24",
  "maxtasks_per_job": "-1",
  "nagios_contact": "REPLACE_WITH_",
  "nagios_group": "REPLACE_WITH_",
  "nagios_user": "REPLACE_WITH_",
  "nagios_web_login": "REPLACE_WITH_",
  "nagios_web_password": "REPLACE_WITH_",
  "namenode_formatted_mark_dir": "REPLACE_WITH_",
  "namenode_heapsize": "1024m",
  "namenode_opt_maxnewsize": "640m",
  "namenode_opt_newsize": "200m",
  "nodemanager_heapsize": "1024",
  "oozie_user": "REPLACE_WITH_",
  "proxyuser_group": "REPLACE_WITH_",
  "resourcemanager_heapsize": "1024",
  "rrdcached_base_dir": "REPLACE_WITH_",
  "run_dir": "REPLACE_WITH_",
  "scheduler_name": "org.apache.hadoop.mapred.CapacityTaskScheduler",
  "security_enabled": "false",
  "smokeuser": "REPLACE_WITH_",
  "task_controller": "org.apache.hadoop.mapred.DefaultTaskController",
  "user_group": "REPLACE_WITH_",
  "webhcat_user": "REPLACE_WITH_",
  "yarn_heapsize": "1024",
  "yarn_log_dir_prefix": "/var/log/hadoop-yarn",
  "yarn_nodemanager_local-dirs": "/var/log/hadoop/yarn",
  "yarn_nodemanager_log-dirs": "/var/log/hadoop/yarn",
  "yarn_pid_dir_prefix": "/var/run/hadoop-yarn",
  "yarn_user": "yarn",
  "zk_user": "REPLACE_WITH_"
}

HDFS_SITE = {
  "dfs.block.access.token.enable": "REPLACE_WITH_",
  "dfs.block.local-path-access.user": "REPLACE_WITH_",
  "dfs.blockreport.initialDelay": "REPLACE_WITH_",
  "dfs.blocksize": "REPLACE_WITH_dfs.block.size",
  "dfs.client.read.shortcircuit": "true",
  "dfs.client.read.shortcircuit.streams.cache.size": "4096",
  "dfs.cluster.administrators": "REPLACE_WITH_",
  "dfs.datanode.address": "REPLACE_WITH_",
  "dfs.datanode.balance.bandwidthPerSec": "REPLACE_WITH_dfs.balance.bandwidthPerSec",
  "dfs.datanode.data.dir": "REPLACE_WITH_dfs.data.dir",
  "dfs.datanode.data.dir.perm": "REPLACE_WITH_",
  "dfs.datanode.du.reserved": "REPLACE_WITH_",
  "dfs.datanode.failed.volumes.tolerated": "REPLACE_WITH_",
  "dfs.datanode.http.address": "REPLACE_WITH_",
  "dfs.datanode.ipc.address": "REPLACE_WITH_",
  "dfs.datanode.max.transfer.threads": "REPLACE_WITH_dfs.datanode.max.xcievers",
  "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
  "dfs.heartbeat.interval": "REPLACE_WITH_",
  "dfs.hosts.exclude": "REPLACE_WITH_",
  "dfs.https.namenode.https-address": "REPLACE_WITH_dfs.https.address",
  "dfs.namenode.accesstime.precision": "0",
  "dfs.namenode.avoid.read.stale.datanode": "REPLACE_WITH_",
  "dfs.namenode.avoid.write.stale.datanode": "REPLACE_WITH_",
  "dfs.namenode.handler.count": "REPLACE_WITH_",
  "dfs.namenode.http-address": "REPLACE_WITH_dfs.http.address",
  "dfs.namenode.name.dir": "REPLACE_WITH_dfs.name.dir",
  "dfs.namenode.safemode.threshold-pct": "REPLACE_WITH_dfs.safemode.threshold.pct",
  "dfs.namenode.secondary.http-address": "REPLACE_WITH_dfs.secondary.http.address",
  "dfs.namenode.stale.datanode.interval": "REPLACE_WITH_",
  "dfs.namenode.write.stale.datanode.ratio": "REPLACE_WITH_",
  "dfs.permissions.enabled": "REPLACE_WITH_dfs.permissions",
  "dfs.permissions.superusergroup": "REPLACE_WITH_dfs.permissions.supergroup",
  "dfs.replication": "REPLACE_WITH_",
  "dfs.replication.max": "REPLACE_WITH_",
  "dfs.webhdfs.enabled": "REPLACE_WITH_",
  "fs.permissions.umask-mode": "022"
}

CORE_SITE = {
  "dfs.namenode.checkpoint.dir": "REPLACE_WITH_fs.checkpoint.dir",
  "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
  "dfs.namenode.checkpoint.period": "REPLACE_WITH_fs.checkpoint.period",
  "fs.checkpoint.edits.dir": "REPLACE_WITH_",
  "fs.checkpoint.size": "0.5",
  "fs.defaultFS": "REPLACE_WITH_fs.default.name",
  "fs.trash.interval": "REPLACE_WITH_",
  "hadoop.security.auth_to_local": "\n        RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n        RULE:[2:$1@$0](jhs@.*)s/.*/mapred/\n        RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n        RULE:[2:$1@$0](hm@.*)s/.*/hbase/\n        RULE:[2:$1@$0](rs@.*)s/.*/hbase/\n        DEFAULT\n    ",
  "hadoop.security.authentication": "simple",
  "hadoop.security.authorization": "false",
  "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec",
  "io.file.buffer.size": "REPLACE_WITH_",
  "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
  "ipc.client.connect.max.retries": "REPLACE_WITH_",
  "ipc.client.connection.maxidletime": "REPLACE_WITH_",
  "ipc.client.idlethreshold": "REPLACE_WITH_",
  "mapreduce.jobtracker.webinterface.trusted": "REPLACE_WITH_webinterface.private.actions"
}

YARN_SITE = {
  "yarn.application.classpath": "/etc/hadoop/conf,/usr/lib/hadoop/*,/usr/lib/hadoop/lib/*,/usr/lib/hadoop-hdfs/*,/usr/lib/hadoop-hdfs/lib/*,/usr/lib/hadoop-yarn/*,/usr/lib/hadoop-yarn/lib/*,/usr/lib/hadoop-mapreduce/*,/usr/lib/hadoop-mapreduce/lib/*",
  "yarn.log-aggregation-enable": "true",
  "yarn.log-aggregation.retain-seconds": "2592000",
  "yarn.log.server.url": "http://REPLACE_JH_HOST:19888/jobhistory/logs",
  "yarn.nodemanager.address": "0.0.0.0:45454",
  "yarn.nodemanager.admin-env": "MALLOC_ARENA_MAX=$MALLOC_ARENA_MAX",
  "yarn.nodemanager.aux-services": "mapreduce.shuffle",
  "yarn.nodemanager.aux-services.mapreduce.shuffle.class": "org.apache.hadoop.mapred.ShuffleHandler",
  "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor",
  "yarn.nodemanager.container-monitor.interval-ms": "3000",
  "yarn.nodemanager.delete.debug-delay-sec": "0",
  "yarn.nodemanager.disk-health-checker.min-healthy-disks": "0.25",
  "yarn.nodemanager.health-checker.interval-ms": "135000",
  "yarn.nodemanager.health-checker.script.timeout-ms": "60000",
  "yarn.nodemanager.linux-container-executor.group": "hadoop",
  "yarn.nodemanager.local-dirs": "/var/log/hadoop/yarn",
  "yarn.nodemanager.log-aggregation.compression-type": "gz",
  "yarn.nodemanager.log-dirs": "/var/log/hadoop/yarn",
  "yarn.nodemanager.log.retain-second": "604800",
  "yarn.nodemanager.remote-app-log-dir": "/app-logs",
  "yarn.nodemanager.remote-app-log-dir-suffix": "logs",
  "yarn.nodemanager.resource.memory-mb": "10240",
  "yarn.nodemanager.vmem-check-enabled": "false",
  "yarn.nodemanager.vmem-pmem-ratio": "2.1",
  "yarn.resourcemanager.address": "REPLACE_RM_HOST:8050",
  "yarn.resourcemanager.admin.address": "REPLACE_RM_HOST:8141",
  "yarn.resourcemanager.am.max-attempts": "2",
  "yarn.resourcemanager.hostname": "REPLACE_RM_HOST",
  "yarn.resourcemanager.resource-tracker.address": "REPLACE_RM_HOST:8025",
  "yarn.resourcemanager.scheduler.address": "REPLACE_RM_HOST:8030",
  "yarn.resourcemanager.scheduler.class": "org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler",
  "yarn.resourcemanager.webapp.address": "REPLACE_RM_HOST:8088",
  "yarn.scheduler.maximum-allocation-mb": "6144",
  "yarn.scheduler.minimum-allocation-mb": "512"
}


class FatalException(Exception):
  def __init__(self, code, reason):
    self.code = code
    self.reason = reason

  def __str__(self):
    return repr("Fatal exception: %s, exit code %s" % (self.reason, self.code))

  def _get_message(self):
    return str(self)

# Copy file and save with file.# (timestamp)
def backup_file(filePath):
  if filePath is not None and os.path.exists(filePath):
    timestamp = datetime.datetime.now()
    format = '%Y%m%d%H%M%S'
    try:
      shutil.copyfile(filePath, filePath + "." + timestamp.strftime(format))
      os.remove(filePath)
    except (Exception), e:
      logger.warn('Could not backup file "%s": %s' % (filePath, str(e)))
  return 0


def write_mapping(hostmapping):
  if os.path.isfile(MR_MAPPING_FILE):
    os.remove(MR_MAPPING_FILE)
  json.dump(hostmapping, open(MR_MAPPING_FILE, 'w'))
  pass


def write_config(config, type, tag):
  file_name = type + "_" + tag
  if os.path.isfile(file_name):
    os.remove(file_name)
  json.dump(config, open(file_name, 'w'))
  pass


def read_mapping():
  if os.path.isfile(MR_MAPPING_FILE):
    return json.load(open(MR_MAPPING_FILE))
  else:
    raise FatalException(-1, "MAPREDUCE host mapping file, mr_mapping, is not available or badly formatted. Execute "
                             "action save-mr-mapping. Ensure the file is present in the directory where you are "
                             "executing this command.")
  pass


def get_mr1_mapping(options):
  components = ["MAPREDUCE_CLIENT", "JOBTRACKER", "TASKTRACKER"]
  GET_URL_FORMAT = URL_FORMAT + '/services/MAPREDUCE/components/{2}'
  hostmapping = {}
  for component in components:
    hostlist = []
    response = curl(False, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    GET_URL_FORMAT.format(options.hostname, options.clustername, component))
    retcode, errdata = validate_response(response, True)
    if not retcode == 0:
      raise FatalException(retcode, errdata)

    structured_resp = json.loads(response)
    if 'host_components' in structured_resp:
      for hostcomponent in structured_resp['host_components']:
        if 'HostRoles' in hostcomponent:
          if 'host_name' in hostcomponent['HostRoles']:
            hostlist.append(hostcomponent['HostRoles']['host_name'])
            pass
          pass
        pass
      pass
    pass
    hostmapping[component] = hostlist
  write_mapping(hostmapping)


def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "": # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)


def delete_mr(options):
  saved_mr_mapping = get_YN_input("Have you saved MR host mapping using action save-mr-mapping [y/n] (n)? ", False)
  if not saved_mr_mapping:
    raise FatalException(1, "Ensure MAPREDUCE host component mapping is saved before deleting it. Use action "
                            "save-mr-mapping.")

  SERVICE_URL_FORMAT = URL_FORMAT + '/services/MAPREDUCE'
  COMPONENT_URL_FORMAT = URL_FORMAT + '/hosts/{2}/host_components/{3}'
  NON_CLIENTS = ["JOBTRACKER", "TASKTRACKER"]
  PUT_IN_MAINTENANCE = """{"HostRoles": {"state": "MAINTENANCE"}}"""
  hostmapping = read_mapping()

  for key, value in hostmapping.items():
    if (key in NON_CLIENTS) and (len(value) > 0):
      for host in value:
        response = curl(options.printonly, '-u',
                        AUTH_FORMAT.format(options.user, options.password),
                        '-X', 'PUT', '-d',
                        PUT_IN_MAINTENANCE,
                        COMPONENT_URL_FORMAT.format(options.hostname, options.clustername, host, key))
        retcode, errdata = validate_response(response, False)
        if not retcode == 0:
          raise FatalException(retcode, errdata)
        pass
      pass
    pass
  pass

  response = curl(options.printonly, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  '-X', 'DELETE',
                  SERVICE_URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, False)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
  pass


def add_services(options):
  SERVICE_URL_FORMAT = URL_FORMAT + '/services/{2}'
  COMPONENT_URL_FORMAT = SERVICE_URL_FORMAT + '/components/{3}'
  HOST_COMPONENT_URL_FORMAT = URL_FORMAT + '/hosts/{2}/host_components/{3}'
  service_comp = {
    "YARN": ["NODEMANAGER", "RESOURCEMANAGER", "YARN_CLIENT"],
    "MAPREDUCE2": ["HISTORYSERVER", "MAPREDUCE2_CLIENT"]}
  new_old_host_map = {
    "NODEMANAGER": "TASKTRACKER",
    "HISTORYSERVER": "JOBTRACKER",
    "RESOURCEMANAGER": "JOBTRACKER",
    "YARN_CLIENT": "MAPREDUCE_CLIENT",
    "MAPREDUCE2_CLIENT": "MAPREDUCE_CLIENT"}
  hostmapping = read_mapping()

  for service in service_comp.keys():
    response = curl(options.printonly, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    '-X', 'POST',
                    SERVICE_URL_FORMAT.format(options.hostname, options.clustername, service))
    retcode, errdata = validate_response(response, False)
    if not retcode == 0:
      raise FatalException(retcode, errdata)
    for component in service_comp[service]:
      response = curl(options.printonly, '-u',
                      AUTH_FORMAT.format(options.user, options.password),
                      '-X', 'POST',
                      COMPONENT_URL_FORMAT.format(options.hostname, options.clustername, service, component))
      retcode, errdata = validate_response(response, False)
      if not retcode == 0:
        raise FatalException(retcode, errdata)
      for host in hostmapping[new_old_host_map[component]]:
        response = curl(options.printonly, '-u',
                        AUTH_FORMAT.format(options.user, options.password),
                        '-X', 'POST',
                        HOST_COMPONENT_URL_FORMAT.format(options.hostname, options.clustername, host, component))
        retcode, errdata = validate_response(response, False)
        if not retcode == 0:
          raise FatalException(retcode, errdata)
        pass
      pass
    pass
  pass


def update_config(options, properties, type):
  tag = "version" + str(int(time.time() * 1000))
  properties_payload = {"Clusters": {"desired_config": {"type": type, "tag": tag, "properties": properties}}}
  response = curl(options.printonly, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  '-X', 'PUT', '-d',
                  json.dumps(properties_payload),
                  URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, False)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
  pass


def get_config(options, type):
  tag, structured_resp = get_config_resp(options, type)
  properties = None
  if 'items' in structured_resp:
    for item in structured_resp['items']:
      if (tag == item['tag']) or (type == item['type']):
        properties = item['properties']
  if (properties is None):
    raise FatalException(-1, "Unable to read configuration for type " + type + " and tag " + tag)
  else:
    logger.info("Read configuration for type " + type + " and tag " + tag)
  return properties


def get_config_resp(options, type, error_if_na=True):
  CONFIG_URL_FORMAT = URL_FORMAT + '/configurations?type={2}&tag={3}'
  response = curl(False, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, True)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
    # Read the config version
  tag = None
  structured_resp = json.loads(response)
  if 'Clusters' in structured_resp:
    if 'desired_configs' in structured_resp['Clusters']:
      if type in structured_resp['Clusters']['desired_configs']:
        tag = structured_resp['Clusters']['desired_configs'][type]['tag']

  if tag != None:
    # Get the config with the tag and return properties
    response = curl(False, '-u',
                    AUTH_FORMAT.format(options.user, options.password),
                    CONFIG_URL_FORMAT.format(options.hostname, options.clustername, type, tag))
    retcode, errdata = validate_response(response, True)
    if not retcode == 0:
      raise FatalException(retcode, errdata)
    structured_resp = json.loads(response)
    return (tag, structured_resp)
  else:
    if error_if_na:
      raise FatalException(-1, "Unable to get the current version for config type " + type)
    else:
      return (tag, None)
  pass


def modify_configs(options, config_type):
  hostmapping = read_mapping()
  # Add capacity-scheduler, mapred-queue-acls, yarn-site
  if (config_type is None) or (config_type == CAPACITY_SCHEDULER_TAG):
    update_config(options, CAPACITY_SCHEDULER, CAPACITY_SCHEDULER_TAG)
    pass

  if (config_type is None) or (config_type == MAPRED_QUEUE_ACLS_TAG):
    update_config(options, MAPRED_QUEUE_ACLS, MAPRED_QUEUE_ACLS_TAG)
    pass
  jt_host = hostmapping["JOBTRACKER"][0]

  if (config_type is None) or (config_type == YARN_SITE_TAG):
    for key in YARN_SITE.keys():
      if REPLACE_JH_HOST_NAME_TAG in YARN_SITE[key]:
        YARN_SITE[key] = YARN_SITE[key].replace(REPLACE_JH_HOST_NAME_TAG, jt_host, 1)
      if REPLACE_RM_HOST_NAME_TAG in YARN_SITE[key]:
        YARN_SITE[key] = YARN_SITE[key].replace(REPLACE_RM_HOST_NAME_TAG, jt_host, 1)
        pass
      pass
    pass
    update_config(options, YARN_SITE, YARN_SITE_TAG)
    pass

  # Update mapred-site config
  if (config_type is None) or (config_type == MAPRED_SITE_TAG):
    for key in MAPRED_SITE.keys():
      if REPLACE_JH_HOST_NAME_TAG in MAPRED_SITE[key]:
        MAPRED_SITE[key] = MAPRED_SITE[key].replace(REPLACE_JH_HOST_NAME_TAG, jt_host, 1)
        pass
      pass
    pass
    update_config_using_existing(options, MAPRED_SITE_TAG, MAPRED_SITE)
    pass

  # Update global config, hdfs-site, core-site
  if (config_type is None) or (config_type == GLOBAL_TAG):
    update_config_using_existing(options, GLOBAL_TAG, GLOBAL, True)
    pass
  if (config_type is None) or (config_type == HDFS_SITE_TAG):
    update_config_using_existing(options, HDFS_SITE_TAG, HDFS_SITE)
    pass
  if (config_type is None) or (config_type == CORE_SITE_TAG):
    update_config_using_existing(options, CORE_SITE_TAG, CORE_SITE)
    pass
  pass


def update_config_using_existing(options, type, properties_template, append_unprocessed=False):
  site_properties = get_config(options, type)
  keys_processed = []
  for key in properties_template.keys():
    keys_processed.append(key)
    if properties_template[key].find(REPLACE_WITH_TAG) == 0:
      name_to_lookup = key
      if len(properties_template[key]) > len(REPLACE_WITH_TAG):
        name_to_lookup = properties_template[key][len(REPLACE_WITH_TAG):]
        keys_processed.append(name_to_lookup)
      value = ""
      if name_to_lookup in site_properties.keys():
        value = site_properties[name_to_lookup]
        pass
      else:
        logger.warn("Unable to find the equivalent for " + key + ". Looked for " + name_to_lookup)
      properties_template[key] = value
      pass
    pass
  pass
  if append_unprocessed:
    for key in site_properties.keys():
      if key not in keys_processed:
        properties_template[key] = site_properties[key]
        pass
      pass
    pass
  pass
  update_config(options, properties_template, type)


def backup_configs(options, type=None):
  types_to_save = {"global": True, "mapred-site": True, "hdfs-site": True, "core-site": True,
                   "webhcat-site": False, "hive-site": False, "hbase-site": False, "oozie-site": False}
  for type in types_to_save.keys():
    backup_single_config_type(options, type, types_to_save[type])
    pass
  pass


def backup_single_config_type(options, type, error_if_na=True):
  tag, response = get_config_resp(options, type, error_if_na)
  if response is not None:
    logger.info("Saving config for type: " + type + " and tag: " + tag)
    write_config(response, type, tag)
  else:
    logger.info("Unable to obtain config for type: " + type)
    pass
  pass


def install_services(options):
  SERVICE_URL_FORMAT = URL_FORMAT + '/services?ServiceInfo/state=INIT'
  PUT_IN_INSTALLED = """{"ServiceInfo": {"state": "INSTALLED"}}"""

  response = curl(options.printonly, '-u',
                  AUTH_FORMAT.format(options.user, options.password),
                  '-X', 'PUT', '-d',
                  PUT_IN_INSTALLED,
                  SERVICE_URL_FORMAT.format(options.hostname, options.clustername))
  retcode, errdata = validate_response(response, not options.printonly)
  if not retcode == 0:
    raise FatalException(retcode, errdata + "(Services may already be installed.)")
  pass


def validate_response(response, expect_body):
  if expect_body:
    if "\"href\" : \"" not in response:
      return (1, response)
    else:
      return (0, "")
  elif len(response) > 0:
    return (1, response)
  else:
    return (0, "")
  pass


def curl(print_only, *args):
  curl_path = '/usr/bin/curl'
  curl_list = [curl_path]
  for arg in args:
    curl_list.append(arg)
  if print_only:
    logger.info("Command to be executed: " + ' '.join(curl_list))
    return ""
  pass
  logger.info(' '.join(curl_list))
  osStat = subprocess.Popen(
    curl_list,
    stderr=subprocess.PIPE,
    stdout=subprocess.PIPE)
  out, err = osStat.communicate()
  if 0 != osStat.returncode:
    error = "curl call failed. out: " + out + " err: " + err
    logger.error(error)
    raise FatalException(osStat.returncode, error)
  return out

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action\n  Valid actions: " + VALID_ACTIONS
                                 + "\n  update-configs accepts type, e.g. hdfs-site to update specific configs",)

  parser.add_option("-n", "--printonly",
                    action="store_true", dest="printonly", default=False,
                    help="Prints all the curl commands to be executed (only for write/update actions)")
  parser.add_option("-o", "--log", dest="logfile", default=UPGRADE_LOG_FILE,
                    help="Log file")

  parser.add_option('--hostname', default=None, help="Hostname for Ambari server", dest="hostname")
  parser.add_option('--user', default=None, help="Ambari admin user", dest="user")
  parser.add_option('--password', default=None, help="Ambari admin password", dest="password")
  parser.add_option('--clustername', default=None, help="Cluster name", dest="clustername")

  (options, args) = parser.parse_args()

  options.warnings = []
  if options.user is None:
    options.warnings.append("User name must be provided (e.g. admin)")
  if options.hostname is None:
    options.warnings.append("Ambari server host name must be provided")
  if options.clustername is None:
    options.warnings.append("Cluster name must be provided")
  if options.password is None:
    options.warnings.append("Ambari admin user's password name must be provided (e.g. admin)")

  if len(options.warnings) != 0:
    print parser.print_help()
    for warning in options.warnings:
      print "  " + warning
    parser.error("Invalid or missing options")

  if len(args) == 0:
    print parser.print_help()
    parser.error("No action entered")

  action = args[0]

  options.exit_message = "Upgrade action '%s' completed successfully." % action

  backup_file(options.logfile)
  global logger
  logger = logging.getLogger('UpgradeHelper')
  handler = logging.FileHandler(options.logfile)
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  handler.setFormatter(formatter)
  logger.addHandler(handler)
  logging.basicConfig(level=logging.DEBUG)

  try:
    if action == GET_MR_MAPPING_ACTION:
      get_mr1_mapping(options)
      pprint("File mr_mapping contains the host mapping for mapreduce components. This file is critical for later "
             "steps.")
    elif action == DELETE_MR_ACTION:
      delete_mr(options)
    elif action == ADD_YARN_MR2_ACTION:
      add_services(options)
    elif action == MODIFY_CONFIG_ACTION:
      config_type = None
      if len(args) > 1:
        config_type = args[1]
      modify_configs(options, config_type)
    elif action == INSTALL_YARN_MR2_ACTION:
      install_services(options)
    elif action == BACKUP_CONFIG_ACTION:
      backup_configs(options)
    else:
      parser.error("Invalid action")

  except FatalException as e:
    if e.reason is not None:
      error = "ERROR: Exiting with exit code {0}. Reason: {1}".format(e.code, e.reason)
      pprint(error)
      logger.error(error)
    sys.exit(e.code)

  if options.exit_message is not None:
    print options.exit_message


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)