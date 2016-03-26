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

import logging
import ConfigParser
import StringIO
import hostname
import ambari_simplejson as json
from NetUtil import NetUtil
import os

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
logger = logging.getLogger(__name__)

content = """

[server]
hostname=localhost
url_port=8440
secured_url_port=8441

[agent]
prefix={ps}tmp{ps}ambari-agent
tmp_dir={ps}tmp{ps}ambari-agent{ps}tmp
data_cleanup_interval=86400
data_cleanup_max_age=2592000
data_cleanup_max_size_MB = 100
ping_port=8670
cache_dir={ps}var{ps}lib{ps}ambari-agent{ps}cache
parallel_execution=0
system_resource_overrides={ps}etc{ps}resource_overrides

[services]

[python]
custom_actions_dir = {ps}var{ps}lib{ps}ambari-agent{ps}resources{ps}custom_actions

[security]
keysdir={ps}tmp{ps}ambari-agent
server_crt=ca.crt
passphrase_env_var_name=AMBARI_PASSPHRASE

[heartbeat]
state_interval = 6
dirs={ps}etc{ps}hadoop,{ps}etc{ps}hadoop{ps}conf,{ps}var{ps}run{ps}hadoop,{ps}var{ps}log{ps}hadoop
log_lines_count=300

[logging]
log_command_executes = 0

""".format(ps=os.sep)


servicesToPidNames = {
  'GLUSTERFS' : 'glusterd.pid$',
  'NAMENODE': 'hadoop-{USER}-namenode.pid$',
  'SECONDARY_NAMENODE': 'hadoop-{USER}-secondarynamenode.pid$',
  'DATANODE': 'hadoop-{USER}-datanode.pid$',
  'JOBTRACKER': 'hadoop-{USER}-jobtracker.pid$',
  'TASKTRACKER': 'hadoop-{USER}-tasktracker.pid$',
  'RESOURCEMANAGER': 'yarn-{USER}-resourcemanager.pid$',
  'NODEMANAGER': 'yarn-{USER}-nodemanager.pid$',
  'HISTORYSERVER': 'mapred-{USER}-historyserver.pid$',
  'JOURNALNODE': 'hadoop-{USER}-journalnode.pid$',
  'ZKFC': 'hadoop-{USER}-zkfc.pid$',
  'OOZIE_SERVER': 'oozie.pid',
  'ZOOKEEPER_SERVER': 'zookeeper_server.pid',
  'FLUME_SERVER': 'flume-node.pid',
  'TEMPLETON_SERVER': 'templeton.pid',
  'GANGLIA_SERVER': 'gmetad.pid',
  'GANGLIA_MONITOR': 'gmond.pid',
  'HBASE_MASTER': 'hbase-{USER}-master.pid',
  'HBASE_REGIONSERVER': 'hbase-{USER}-regionserver.pid',
  'HCATALOG_SERVER': 'webhcat.pid',
  'KERBEROS_SERVER': 'kadmind.pid',
  'HIVE_SERVER': 'hive-server.pid',
  'HIVE_METASTORE': 'hive.pid',
  'MYSQL_SERVER': 'mysqld.pid',
  'HUE_SERVER': '/var/run/hue/supervisor.pid',
  'WEBHCAT_SERVER': 'webhcat.pid',
}

#Each service, which's pid depends on user should provide user mapping
servicesToLinuxUser = {
  'NAMENODE': 'hdfs_user',
  'SECONDARY_NAMENODE': 'hdfs_user',
  'DATANODE': 'hdfs_user',
  'JOURNALNODE': 'hdfs_user',
  'ZKFC': 'hdfs_user',
  'JOBTRACKER': 'mapred_user',
  'TASKTRACKER': 'mapred_user',
  'RESOURCEMANAGER': 'yarn_user',
  'NODEMANAGER': 'yarn_user',
  'HISTORYSERVER': 'mapred_user',
  'HBASE_MASTER': 'hbase_user',
  'HBASE_REGIONSERVER': 'hbase_user',
}

pidPathVars = [
  {'var' : 'glusterfs_pid_dir_prefix',
   'defaultValue' : '/var/run'},
  {'var' : 'hadoop_pid_dir_prefix',
   'defaultValue' : '/var/run/hadoop'},
  {'var' : 'hadoop_pid_dir_prefix',
   'defaultValue' : '/var/run/hadoop'},
  {'var' : 'ganglia_runtime_dir',
   'defaultValue' : '/var/run/ganglia/hdp'},
  {'var' : 'hbase_pid_dir',
   'defaultValue' : '/var/run/hbase'},
  {'var' : 'zk_pid_dir',
   'defaultValue' : '/var/run/zookeeper'},
  {'var' : 'oozie_pid_dir',
   'defaultValue' : '/var/run/oozie'},
  {'var' : 'hcat_pid_dir',
   'defaultValue' : '/var/run/webhcat'},
  {'var' : 'hive_pid_dir',
   'defaultValue' : '/var/run/hive'},
  {'var' : 'mysqld_pid_dir',
   'defaultValue' : '/var/run/mysqld'},
  {'var' : 'hcat_pid_dir',
   'defaultValue' : '/var/run/webhcat'},
  {'var' : 'yarn_pid_dir_prefix',
   'defaultValue' : '/var/run/hadoop-yarn'},
  {'var' : 'mapred_pid_dir_prefix',
   'defaultValue' : '/var/run/hadoop-mapreduce'},
]


class AmbariConfig:
  TWO_WAY_SSL_PROPERTY = "security.server.two_way_ssl"
  AMBARI_PROPERTIES_CATEGORY = 'agentConfig'
  SERVER_CONNECTION_INFO = "{0}/connection_info"
  CONNECTION_PROTOCOL = "https"

  config = None
  net = None

  def __init__(self):
    global content
    self.config = ConfigParser.RawConfigParser()
    self.net = NetUtil()
    self.config.readfp(StringIO.StringIO(content))

  def get(self, section, value, default=None):
    try:
      return str(self.config.get(section, value)).strip()
    except ConfigParser.Error, err:
      if default != None:
        return default
      raise err

  def set(self, section, option, value):
    self.config.set(section, option, value)

  def add_section(self, section):
    self.config.add_section(section)

  def has_section(self, section):
    return self.config.has_section(section)

  def setConfig(self, customConfig):
    self.config = customConfig

  def getConfig(self):
    return self.config

  @staticmethod
  @OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
  def getConfigFile():
    if 'AMBARI_AGENT_CONF_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_CONF_DIR'], "ambari-agent.ini")
    else:
      return "ambari-agent.ini"

  @staticmethod
  @OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
  def getConfigFile():
    if 'AMBARI_AGENT_CONF_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_CONF_DIR'], "ambari-agent.ini")
    else:
      return os.path.join(os.sep, "etc", "ambari-agent", "conf", "ambari-agent.ini")

  @staticmethod
  def getAlertsLogFile():
    if 'AMBARI_ALERTS_AGENT_LOG_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_ALERTS_AGENT_LOG_DIR'], "ambari-agent.log")
    else:
      return os.path.join(os.sep, "var", "log", "ambari-agent", "ambari-alerts.log")

  @staticmethod
  def getLogFile():
    if 'AMBARI_AGENT_LOG_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_LOG_DIR'], "ambari-agent.log")
    else:
      return os.path.join(os.sep, "var", "log", "ambari-agent", "ambari-agent.log")

  @staticmethod
  def getOutFile():
    if 'AMBARI_AGENT_OUT_DIR' in os.environ:
      return os.path.join(os.environ['AMBARI_AGENT_OUT_DIR'], "ambari-agent.out")
    else:
      return os.path.join(os.sep, "var", "log", "ambari-agent", "ambari-agent.out")

  def has_option(self, section, option):
    return self.config.has_option(section, option)

  def remove_option(self, section, option):
    return self.config.remove_option(section, option)

  def load(self, data):
    self.config = ConfigParser.RawConfigParser(data)

  def read(self, filename):
    self.config.read(filename)

  def getServerOption(self, url, name, default=None):
    status, response = self.net.checkURL(url)
    if status is True:
      try:
        data = json.loads(response)
        if name in data:
          return data[name]
      except:
        pass
    return default

  def get_api_url(self):
    return "%s://%s:%s" % (self.CONNECTION_PROTOCOL,
                           hostname.server_hostname(self),
                           self.get('server', 'url_port'))

  def isTwoWaySSLConnection(self):
    req_url = self.get_api_url()
    response = self.getServerOption(self.SERVER_CONNECTION_INFO.format(req_url), self.TWO_WAY_SSL_PROPERTY, 'false')
    if response is None:
      return False
    elif response.lower() == "true":
      return True
    else:
      return False

  def get_parallel_exec_option(self):
    return int(self.get('agent', 'parallel_execution', 0))

  def update_configuration_from_registration(self, reg_resp):
    if reg_resp and AmbariConfig.AMBARI_PROPERTIES_CATEGORY in reg_resp:
      if not self.has_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY):
        self.add_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY)
      for k,v in reg_resp[AmbariConfig.AMBARI_PROPERTIES_CATEGORY].items():
        self.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, k, v)
        logger.info("Updating config property (%s) with value (%s)", k, v)
    pass

def updateConfigServerHostname(configFile, new_host):
  # update agent config file
  agent_config = ConfigParser.ConfigParser()
  agent_config.read(configFile)
  server_host = agent_config.get('server', 'hostname')
  if new_host is not None and server_host != new_host:
    print "Updating server host from " + server_host + " to " + new_host
    agent_config.set('server', 'hostname', new_host)
    with (open(configFile, "wb")) as new_agent_config:
      agent_config.write(new_agent_config)


def main():
  print AmbariConfig().config

if __name__ == "__main__":
  main()
