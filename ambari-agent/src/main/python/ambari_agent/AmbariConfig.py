#!/usr/bin/env python2.6

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

import ConfigParser
import StringIO

config = ConfigParser.RawConfigParser()
content = """

[server]
hostname=localhost
url_port=8440
secured_url_port=8441

[agent]
prefix=/tmp/ambari-agent
data_cleanup_interval=86400
data_cleanup_max_age=2592000
ping_port=8670
cache_dir=/var/lib/ambari-agent/cache

[services]

[puppet]
puppetmodules=/var/lib/ambari-agent/puppet/
puppet_home=/root/workspace/puppet-install/puppet-2.7.9
facter_home=/root/workspace/puppet-install/facter-1.6.10
timeout_seconds = 600

[command]
maxretries=2
sleepBetweenRetries=1

[security]
keysdir=/tmp/ambari-agent
server_crt=ca.crt
passphrase_env_var_name=AMBARI_PASSPHRASE

[heartbeat]
state_interval = 6
dirs=/etc/hadoop,/etc/hadoop/conf,/var/run/hadoop,/var/log/hadoop
rpms=glusterfs,openssl,wget,net-snmp,ntpd,ruby,ganglia,nagios,glusterfs
"""
s = StringIO.StringIO(content)
config.readfp(s)

imports = [
  "hdp/manifests/*.pp",
  "hdp-hadoop/manifests/*.pp",
  "hdp-hbase/manifests/*.pp",
  "hdp-zookeeper/manifests/*.pp",
  "hdp-oozie/manifests/*.pp",
  "hdp-pig/manifests/*.pp",
  "hdp-sqoop/manifests/*.pp",
  "hdp-templeton/manifests/*.pp",
  "hdp-hive/manifests/*.pp",
  "hdp-hcat/manifests/*.pp",
  "hdp-mysql/manifests/*.pp",
  "hdp-monitor-webserver/manifests/*.pp",
  "hdp-repos/manifests/*.pp"
]

rolesToClass = {
  'HCFS': 'hdp-hadoop::hcfs',
  'HCFS_CLIENT': 'hdp-hadoop::hcfs_client',
  'HCFS_SERVICE_CHECK': 'hdp-hadoop::hcfs_service_check',
  'NAMENODE': 'hdp-hadoop::namenode',
  'DATANODE': 'hdp-hadoop::datanode',
  'SECONDARY_NAMENODE': 'hdp-hadoop::snamenode',
  'JOBTRACKER': 'hdp-hadoop::jobtracker',
  'TASKTRACKER': 'hdp-hadoop::tasktracker',
  'RESOURCEMANAGER': 'hdp-yarn::resourcemanager',
  'NODEMANAGER': 'hdp-yarn::nodemanager',
  'HISTORYSERVER': 'hdp-yarn::historyserver',
  'YARN_CLIENT': 'hdp-yarn::yarn_client',
  'HDFS_CLIENT': 'hdp-hadoop::client',
  'MAPREDUCE_CLIENT': 'hdp-hadoop::client',
  'MAPREDUCE2_CLIENT': 'hdp-yarn::mapreducev2_client',
  'ZOOKEEPER_SERVER': 'hdp-zookeeper',
  'ZOOKEEPER_CLIENT': 'hdp-zookeeper::client',
  'HBASE_MASTER': 'hdp-hbase::master',
  'HBASE_REGIONSERVER': 'hdp-hbase::regionserver',
  'HBASE_CLIENT': 'hdp-hbase::client',
  'PIG': 'hdp-pig',
  'SQOOP': 'hdp-sqoop',
  'OOZIE_SERVER': 'hdp-oozie::server',
  'OOZIE_CLIENT': 'hdp-oozie::client',
  'HIVE_CLIENT': 'hdp-hive::client',
  'HCAT': 'hdp-hcat',
  'HIVE_SERVER': 'hdp-hive::server',
  'HIVE_METASTORE': 'hdp-hive::metastore',
  'MYSQL_SERVER': 'hdp-mysql::server',
  'WEBHCAT_SERVER': 'hdp-templeton::server',
  'DASHBOARD': 'hdp-dashboard',
  'NAGIOS_SERVER': 'hdp-nagios::server',
  'GANGLIA_SERVER': 'hdp-ganglia::server',
  'GANGLIA_MONITOR': 'hdp-ganglia::monitor',
  'HTTPD': 'hdp-monitor-webserver',
  'HUE_SERVER': 'hdp-hue::server',
  'HDFS_SERVICE_CHECK': 'hdp-hadoop::hdfs::service_check',
  'MAPREDUCE_SERVICE_CHECK': 'hdp-hadoop::mapred::service_check',
  'MAPREDUCE2_SERVICE_CHECK': 'hdp-yarn::mapred2::service_check',
  'ZOOKEEPER_SERVICE_CHECK': 'hdp-zookeeper::zookeeper::service_check',
  'ZOOKEEPER_QUORUM_SERVICE_CHECK': 'hdp-zookeeper::quorum::service_check',
  'HBASE_SERVICE_CHECK': 'hdp-hbase::hbase::service_check',
  'HIVE_SERVICE_CHECK': 'hdp-hive::hive::service_check',
  'HCAT_SERVICE_CHECK': 'hdp-hcat::hcat::service_check',
  'OOZIE_SERVICE_CHECK': 'hdp-oozie::oozie::service_check',
  'PIG_SERVICE_CHECK': 'hdp-pig::pig::service_check',
  'SQOOP_SERVICE_CHECK': 'hdp-sqoop::sqoop::service_check',
  'WEBHCAT_SERVICE_CHECK': 'hdp-templeton::templeton::service_check',
  'DASHBOARD_SERVICE_CHECK': 'hdp-dashboard::dashboard::service_check',
  'DECOMMISSION_DATANODE': 'hdp-hadoop::hdfs::decommission',
  'HUE_SERVICE_CHECK': 'hdp-hue::service_check',
  'RESOURCEMANAGER_SERVICE_CHECK': 'hdp-yarn::resourcemanager::service_check',
  'HISTORYSERVER_SERVICE_CHECK': 'hdp-yarn::historyserver::service_check',
  'TEZ_CLIENT': 'hdp-tez::tez_client',
  'YARN_SERVICE_CHECK': 'hdp-yarn::yarn::service_check',
  'FLUME_SERVER': 'hdp-flume',
  'JOURNALNODE': 'hdp-hadoop::journalnode',
  'ZKFC': 'hdp-hadoop::zkfc'
}

serviceStates = {
  'START': 'running',
  'INSTALL': 'installed_and_configured',
  'STOP': 'stopped'
}

servicesToPidNames = {
  'HCFS' : 'glusterd.pid$',    
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
  'NAGIOS_SERVER': 'nagios.pid',
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

pidPathesVars = [
  {'var' : 'hcfs_pid_dir_prefix',
   'defaultValue' : '/var/run'},      
  {'var' : 'hadoop_pid_dir_prefix',
   'defaultValue' : '/var/run/hadoop'},
  {'var' : 'hadoop_pid_dir_prefix',
   'defaultValue' : '/var/run/hadoop'},                 
  {'var' : 'ganglia_runtime_dir',
   'defaultValue' : '/var/run/ganglia/hdp'},                 
  {'var' : 'hbase_pid_dir',
   'defaultValue' : '/var/run/hbase'},                
  {'var' : '',
   'defaultValue' : '/var/run/nagios'},                    
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
  def getConfig(self):
    global config
    return config

  def getImports(self):
    global imports
    return imports

  def getRolesToClass(self):
    global rolesToClass
    return rolesToClass

  def getServiceStates(self):
    global serviceStates
    return serviceStates

  def getServicesToPidNames(self):
    global servicesToPidNames
    return servicesToPidNames

  def getPidPathesVars(self):
    global pidPathesVars
    return pidPathesVars


def setConfig(customConfig):
  global config
  config = customConfig


def main():
  print config

if __name__ == "__main__":
  main()
