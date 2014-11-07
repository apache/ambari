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

from mock.mock import MagicMock, patch
from HttpClientInvoker import HttpClientInvoker

from ambari_client.ambari_api import  AmbariClient
from ambari_client.model.host import HostModel
from ambari_client.core.errors import BadRequest

import unittest

class TestClusterModel(unittest.TestCase):

  def setUp(self):
    http_client_logger = logging.getLogger()
    http_client_logger.info('Running test:' + self.id())

  def create_cluster(self, http_client_mock = MagicMock()):    
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    return client.get_cluster('test1')
   
  def test_get_service(self):
    """
    Get the service of a cluster
    This testcase checks if get_service returns a list of ServiceModel.
    """   
    expected_dict_output = {'cluster_name': 'test1', 'version': 'HDP-1.2.1'}
    
    cluster = self.create_cluster()
    serviceList = cluster.get_all_services()
    ganglia = cluster.get_service("GANGLIA")  

    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")
    self.assertEqual(len(serviceList), 3, "There should be a 3 services from the response")
    self.assertEqual(str(ganglia.state), "STARTED", "The ganglia service state should be fetched as STARTED")
    self.assertEqual(ganglia.clusterRef.cluster_name, cluster.cluster_name, "The clusterRef value for  service  should be fetched ")
      
  def test_get_all_services(self):
    """
    Get all services of a cluster.
    This testcase checks if get_all_services returns a list of ModelList.
    """
    expected_dict_output = {'cluster_name': 'test1', 'version': 'HDP-1.2.1'}
    
    cluster = self.create_cluster()
    serviceList = cluster.get_all_services()
    
    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")
    self.assertEqual(len(serviceList), 3, "There should be a 3 services from the response")

  def test_get_all_hosts(self):
    """
    Get all cluster hosts
    This testcase checks if get_all_services returns a list of ModelList.
    """
    expected_dict_output = {'items': [{'ip': '10.0.2.15', 'host_name': 'dev05.hortonworks.com', 'rack_info': '/default-rack'}, {'ip': '10.0.2.15', 'host_name': 'dev06.hortonworks.com', 'rack_info': '/default-rack'}]}

    cluster = self.create_cluster()
    hostlist = cluster.get_all_hosts()
    
    self.assertEqual(cluster.cluster_name, "test1")
    self.assertEqual(hostlist.to_json_dict(), expected_dict_output)
    self.assertEqual(hostlist[1].host_name, 'dev06.hortonworks.com')
    self.assertEqual(len(hostlist), 2)  

  def test_get_host(self):
    """
    Get cluster host
    This testcase checks if get_host returns correct HostModel
    """
    expected_dict_output = {'ip': '10.104.44.95', 'host_name': 'myhost', 'rack_info': '/default-rack'}
    
    cluster = self.create_cluster()
    host = cluster.get_host('myhost')
    
    self.assertEqual(host.clusterRef.cluster_name, "test1")
    self.assertEqual(host.to_json_dict(), expected_dict_output)
    self.assertEqual(host.host_state, "HEALTHY")
    self.assertEqual(host.public_host_name, "myhost")
     
  def test_get_global_config(self):
    """
    Get global config
    This testcase checks if get_host returns correct HostModel
    """
    expected_dict_output = {'tag': 'version1', 'type': 'global'}
    expected_properties = {'dfs_namenode_name_dir': '/hadoop/hdfs/namenode', 'security_enabled': 'false', 'proxyuser_group': 'users', 'hdfs_log_dir_prefix': '/var/log/hadoop', 'dfs_datanode_data_dir': '/hadoop/hdfs/data', 'namenode_formatted_mark_dir': '/var/run/hadoop/hdfs/namenode/formatted/', 'rrdcached_base_dir': '/var/lib/ganglia/rrds', 'user_group': 'hadoop', 'dfs_namenode_checkpoint_dir': '/hadoop/hdfs/namesecondary', 'dfs_namenode_checkpoint_period': '21600', 'hive_user': 'hive', 'fs_checkpoint_size': '0.5', 'hbase_conf_dir': '/etc/hbase', 'datanode_du_reserved': '1', 'dfs_datanode_http_address': '50075', 'namenode_heapsize': '1024m', 'dfs_webhdfs_enabled': 'true', 'oozie_user': 'oozie', 'hcat_conf_dir': '', 'hadoop_conf_dir': '/etc/hadoop/conf', 'dfs_replication': '3', 'namenode_opt_maxnewsize': '640m', 'apache_artifacts_download_url': '', 'dfs_datanode_address': '50010', 'dfs_exclude': 'dfs.exclude', 'yarn_user': 'yarn', 'zk_user': 'zookeeper', 'smokeuser': 'ambari-qa', 'dtnode_heapsize': '1024m', 'gmond_user': 'nobody', 'dfs_datanode_failed_volume_tolerated': '0', 'java64_home': '/usr/jdk/jdk1.6.0_31', 'run_dir': '/var/run/hadoop', 'ganglia_runtime_dir': '/var/run/ganglia/hdp', 'dfs_datanode_data_dir_perm': '750', 'hdfs_enable_shortcircuit_read': 'true', 'hdfs_user': 'hdfs', 'hbase_user': 'hbase', 'webhcat_user': 'hcat', 'gmetad_user': 'nobody', 'dfs_block_local_path_access_user': 'hbase', 'namenode_opt_newsize': '200m', 'namenode_opt_maxpermsize': '256m', 'namenode_opt_permsize': '128m', 'mapred_user': 'mapred', 'nagios_group': 'nagios', 'hcat_user': 'hcat', 'hadoop_heapsize': '1024', 'hadoop_pid_dir_prefix': '/var/run/hadoop', 'nagios_user': 'nagios'}
    
    cluster = self.create_cluster()
    global_config = cluster.get_global_config()
    
    self.assertEqual(cluster.cluster_name, "test1")
    self.assertEqual(global_config.properties, expected_properties)
    self.assertEqual(global_config.to_json_dict(), expected_dict_output)
    
  def test_get_core_site_config(self):
    """
    Get core-site config
    """
    expected_dict_output = {'tag': 'version1', 'type': 'core-site'}
    expected_properties = {'io.serializations': 'org.apache.hadoop.io.serializer.WritableSerialization', 'fs.checkpoint.size': '0.5', 'fs.trash.interval': '360', 'hadoop.security.authentication': 'simple', 'io.compression.codecs': 'org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec', 'mapreduce.jobtracker.webinterface.trusted': 'false', 'hadoop.security.authorization': 'false', 'fs.checkpoint.edits.dir': '/hadoop/hdfs/namesecondary', 'ipc.client.connection.maxidletime': '30000', 'ipc.client.connect.max.retries': '50', 'hadoop.security.auth_to_local': '\n        RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n        RULE:[2:$1@$0](jhs@.*)s/.*/mapred/\n        RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n        RULE:[2:$1@$0](hm@.*)s/.*/hbase/\n        RULE:[2:$1@$0](rs@.*)s/.*/hbase/\n        DEFAULT\n    ', 'io.file.buffer.size': '131072', 'dfs.namenode.checkpoint.dir': '/hadoop/hdfs/namesecondary', 'ipc.client.idlethreshold': '8000', 'dfs.namenode.checkpoint.edits.dir': '${dfs.namenode.checkpoint.dir}', 'fs.defaultFS': 'hdfs://dev05.hortonworks.com:8020', 'dfs.namenode.checkpoint.period': '21600'}
        
    cluster = self.create_cluster()
    global_config = cluster.get_core_site_config()
    
    self.assertEqual(cluster.cluster_name, "test1")
    self.assertEqual(global_config.properties, expected_properties)
    self.assertEqual(global_config.to_json_dict(), expected_dict_output)
    
  def test_get_hdfs_site_config(self):
    """
    Get hdfs config
    """
    expected_dict_output = {'tag': 'version1', 'type': 'hdfs-site'}
    expected_properties = {'dfs.namenode.avoid.write.stale.datanode': 'true', 'dfs.webhdfs.enabled': 'true', 'dfs.block.access.token.enable': 'true', 'dfs.datanode.address': '0.0.0.0:50010', 'dfs.cluster.administrators': ' hdfs', 'dfs.datanode.balance.bandwidthPerSec': '6250000', 'dfs.namenode.safemode.threshold-pct': '1.0f', 'dfs.permissions.enabled': 'true', 'dfs.client.read.shortcircuit': 'true', 'dfs.journalnode.edits.dir': '/grid/0/hdfs/journal', 'dfs.blocksize': '134217728', 'dfs.datanode.max.transfer.threads': '1024', 'dfs.datanode.du.reserved': '1', 'dfs.replication': '3', 'dfs.namenode.handler.count': '100', 'fs.permissions.umask-mode': '022', 'dfs.datanode.http.address': '0.0.0.0:50075', 'dfs.datanode.ipc.address': '0.0.0.0:8010', 'dfs.datanode.data.dir': '/hadoop/hdfs/data', 'dfs.namenode.http-address': 'dev05.hortonworks.com:50070', 'dfs.blockreport.initialDelay': '120', 'dfs.datanode.failed.volumes.tolerated': '0', 'dfs.namenode.accesstime.precision': '0', 'dfs.block.local-path-access.user': 'hbase', 'dfs.https.namenode.https-address': 'dev05.hortonworks.com:50470', 'dfs.namenode.secondary.http-address': 'dev05.hortonworks.com:50090', 'dfs.namenode.stale.datanode.interval': '30000', 'dfs.heartbeat.interval': '3', 'dfs.client.read.shortcircuit.streams.cache.size': '4096', 'dfs.permissions.superusergroup': 'hdfs', 'dfs.journalnode.http-address': '0.0.0.0:8480', 'dfs.domain.socket.path': '/var/lib/hadoop-hdfs/dn_socket', 'dfs.namenode.avoid.read.stale.datanode': 'true', 'dfs.hosts.exclude': '/etc/hadoop/conf/dfs.exclude', 'dfs.datanode.data.dir.perm': '750', 'dfs.namenode.write.stale.datanode.ratio': '1.0f', 'dfs.replication.max': '50', 'dfs.namenode.name.dir': '/hadoop/hdfs/namenode'}
        
    cluster = self.create_cluster()
    global_config = cluster.get_hdfs_site_config()
    
    self.assertEqual(cluster.cluster_name, "test1")
    self.assertEqual(global_config.properties, expected_properties)
    self.assertEqual(global_config.to_json_dict(), expected_dict_output)
    
  def test_get_mapred_site_config(self):
    """
    Get mapred config
    """
    expected_dict_output = {'tag': 'version1', 'type': 'mapred-site'}
    expected_properties = {'mapreduce.jobhistory.address': 'dev05.hortonworks.com:10020', 'mapreduce.reduce.input.buffer.percent': '0.0', 'mapred.jobtracker.maxtasks.per.job': '-1', 'mapreduce.framework.name': 'yarn', 'mapreduce.map.speculative': 'false', 'mapreduce.tasktracker.healthchecker.script.path': 'file:////mapred/jobstatus', 'mapreduce.reduce.shuffle.merge.percent': '0.66', 'mapred.userlog.retain.hours': '24', 'yarn.app.mapreduce.am.resource.mb': '1024', 'mapreduce.reduce.shuffle.parallelcopies': '30', 'mapreduce.map.java.opts': '-Xmx320m', 'mapreduce.task.io.sort.factor': '100', 'mapreduce.application.classpath': '$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*,$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*', 'yarn.app.mapreduce.am.command-opts': '-Xmx756m', 'mapreduce.job.reduce.slowstart.completedmaps': '0.05', 'mapreduce.output.fileoutputformat.compress.type': 'BLOCK', 'mapreduce.reduce.speculative': 'false', 'mapreduce.reduce.java.opts': '-Xmx756m', 'mapreduce.am.max-attempts': '2', 'yarn.app.mapreduce.am.admin-command-opts': '-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN', 'mapreduce.jobtracker.system.dir': '/mapred/system', 'mapreduce.map.sort.spill.percent': '0.1', 'mapreduce.task.timeout': '600000', 'mapreduce.map.memory.mb': '1536', 'mapreduce.reduce.log.level': 'INFO', 'mapreduce.jobhistory.intermediate-done-dir': '/mr-history/tmp', 'mapreduce.reduce.memory.mb': '2048', 'mapreduce.tasktracker.map.tasks.maximum': '4', 'yarn.app.mapreduce.am.log.level': 'INFO', 'mapreduce.map.log.level': 'INFO', 'mapreduce.shuffle.port': '13562', 'mapred.jobtracker.taskScheduler': 'org.apache.hadoop.mapred.CapacityTaskScheduler', 'mapreduce.admin.user.env': 'LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`', 'mapreduce.jobhistory.webapp.address': 'dev05.hortonworks.com:19888', 'mapred.hosts.exclude': '/etc/hadoop/conf/mapred.exclude', 'mapreduce.reduce.shuffle.input.buffer.percent': '0.7', 'yarn.app.mapreduce.am.staging-dir': '/user', 'mapred.hosts': '/etc/hadoop/conf/mapred.include', 'mapreduce.jobhistory.done-dir': '/mr-history/done', 'mapreduce.admin.reduce.child.java.opts': '-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN', 'mapreduce.task.io.sort.mb': '200', 'mapred.task.tracker.task-controller': 'org.apache.hadoop.mapred.DefaultTaskController', 'mapreduce.admin.map.child.java.opts': '-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN'}
        
    cluster = self.create_cluster()
    global_config = cluster.get_mapred_site_config()
    
    self.assertEqual(cluster.cluster_name, "test1")
    self.assertEqual(global_config.properties, expected_properties)
    self.assertEqual(global_config.to_json_dict(), expected_dict_output)
    
  def test_update_global_config(self):
    """
    Update global config
    """
    http_client_mock = MagicMock()
    
    expected_properties = {'dfs_namenode_name_dir': 'abc', 'security_enabled': 'false', 'proxyuser_group': 'users', 'hdfs_log_dir_prefix': '/var/log/hadoop', 'dfs_datanode_data_dir': '/hadoop/hdfs/data', 'namenode_formatted_mark_dir': '/var/run/hadoop/hdfs/namenode/formatted/', 'rrdcached_base_dir': '/var/lib/ganglia/rrds', 'user_group': 'hadoop', 'dfs_namenode_checkpoint_dir': '/hadoop/hdfs/namesecondary', 'dfs_namenode_checkpoint_period': '21600', 'hive_user': 'hive', 'fs_checkpoint_size': '0.5', 'hbase_conf_dir': '/etc/hbase', 'datanode_du_reserved': '1', 'dfs_datanode_http_address': '50075', 'namenode_heapsize': '1024m', 'dfs_webhdfs_enabled': 'true', 'oozie_user': 'oozie', 'hcat_conf_dir': '', 'hadoop_conf_dir': '/etc/hadoop/conf', 'dfs_replication': '3', 'namenode_opt_maxnewsize': '640m', 'apache_artifacts_download_url': '', 'dfs_datanode_address': '50010', 'dfs_exclude': 'dfs.exclude', 'yarn_user': 'yarn', 'zk_user': 'zookeeper', 'smokeuser': 'ambari-qa', 'dtnode_heapsize': '1024m', 'gmond_user': 'nobody', 'dfs_datanode_failed_volume_tolerated': '0', 'java64_home': '/usr/jdk/jdk1.6.0_31', 'run_dir': '/var/run/hadoop', 'ganglia_runtime_dir': '/var/run/ganglia/hdp', 'dfs_datanode_data_dir_perm': '750', 'hdfs_enable_shortcircuit_read': 'true', 'hdfs_user': 'hdfs', 'hbase_user': 'hbase', 'webhcat_user': 'hcat', 'gmetad_user': 'nobody', 'dfs_block_local_path_access_user': 'hbase', 'namenode_opt_newsize': '200m', 'namenode_opt_maxpermsize': '256m', 'namenode_opt_permsize': '128m', 'mapred_user': 'mapred', 'nagios_group': 'nagios', 'hcat_user': 'hcat', 'hadoop_heapsize': '1024', 'hadoop_pid_dir_prefix': '/var/run/hadoop', 'nagios_user': 'nagios'}
    expected_put_path = '//clusters/test1'
    expected_post_request = {'Clusters': {'desired_configs': {'tag': 'version1', 'type': 'global', 'properties':expected_properties}}}   
    expected_get_path = '//clusters/test1/configurations?type=global&tag=version1'
    expected_get_request = None
        
    cluster = self.create_cluster(http_client_mock)
    existant_global_config = cluster.get_global_config()
    existant_global_config.properties['dfs_namenode_name_dir'] = 'abc'
    cluster.update_global_config(existant_global_config)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_any_call('PUT', expected_put_path, headers=None, payload=expected_post_request)
    http_client_mock.invoke.assert_any_call('GET', expected_get_path, headers=None, payload=expected_get_request)
 
  def test_update_core_site_config(self):
    """
    Update core-site config
    """
    http_client_mock = MagicMock()
    
    expected_properties = {'io.serializations': 'abc', 'fs.checkpoint.size': '0.5', 'fs.trash.interval': '360', 'hadoop.security.authentication': 'simple', 'io.compression.codecs': 'org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec', 'mapreduce.jobtracker.webinterface.trusted': 'false', 'hadoop.security.authorization': 'false', 'fs.checkpoint.edits.dir': '/hadoop/hdfs/namesecondary', 'ipc.client.connection.maxidletime': '30000', 'ipc.client.connect.max.retries': '50', 'hadoop.security.auth_to_local': '\n        RULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\n        RULE:[2:$1@$0](jhs@.*)s/.*/mapred/\n        RULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\n        RULE:[2:$1@$0](hm@.*)s/.*/hbase/\n        RULE:[2:$1@$0](rs@.*)s/.*/hbase/\n        DEFAULT\n    ', 'io.file.buffer.size': '131072', 'dfs.namenode.checkpoint.dir': '/hadoop/hdfs/namesecondary', 'ipc.client.idlethreshold': '8000', 'dfs.namenode.checkpoint.edits.dir': '${dfs.namenode.checkpoint.dir}', 'fs.defaultFS': 'hdfs://dev05.hortonworks.com:8020', 'dfs.namenode.checkpoint.period': '21600'}
    expected_put_path = '//clusters/test1'
    expected_post_request = {'Clusters': {'desired_configs': {'tag': 'version1', 'type': 'core-site', 'properties':expected_properties}}}   
    expected_get_path = '//clusters/test1/configurations?type=core-site&tag=version1'
    expected_get_request = None
        
    cluster = self.create_cluster(http_client_mock)
    existant_global_config = cluster.get_core_site_config()
    existant_global_config.properties['io.serializations'] = 'abc'
    cluster.update_core_site_config(existant_global_config)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_any_call('PUT', expected_put_path, headers=None, payload=expected_post_request)
    http_client_mock.invoke.assert_any_call('GET', expected_get_path, headers=None, payload=expected_get_request)
       
  def test_update_hdfs_site_config(self):
    """
    Update hdfs-site config
    """
    http_client_mock = MagicMock()
    
    expected_properties = {'dfs.namenode.avoid.write.stale.datanode': 'abc', 'dfs.webhdfs.enabled': 'true', 'dfs.block.access.token.enable': 'true', 'dfs.datanode.address': '0.0.0.0:50010', 'dfs.cluster.administrators': ' hdfs', 'dfs.datanode.balance.bandwidthPerSec': '6250000', 'dfs.namenode.safemode.threshold-pct': '1.0f', 'dfs.permissions.enabled': 'true', 'dfs.client.read.shortcircuit': 'true', 'dfs.journalnode.edits.dir': '/grid/0/hdfs/journal', 'dfs.blocksize': '134217728', 'dfs.datanode.max.transfer.threads': '1024', 'dfs.datanode.du.reserved': '1', 'dfs.replication': '3', 'dfs.namenode.handler.count': '100', 'fs.permissions.umask-mode': '022', 'dfs.datanode.http.address': '0.0.0.0:50075', 'dfs.datanode.ipc.address': '0.0.0.0:8010', 'dfs.datanode.data.dir': '/hadoop/hdfs/data', 'dfs.namenode.http-address': 'dev05.hortonworks.com:50070', 'dfs.blockreport.initialDelay': '120', 'dfs.datanode.failed.volumes.tolerated': '0', 'dfs.namenode.accesstime.precision': '0', 'dfs.block.local-path-access.user': 'hbase', 'dfs.https.namenode.https-address': 'dev05.hortonworks.com:50470', 'dfs.namenode.secondary.http-address': 'dev05.hortonworks.com:50090', 'dfs.namenode.stale.datanode.interval': '30000', 'dfs.heartbeat.interval': '3', 'dfs.client.read.shortcircuit.streams.cache.size': '4096', 'dfs.permissions.superusergroup': 'hdfs', 'dfs.journalnode.http-address': '0.0.0.0:8480', 'dfs.domain.socket.path': '/var/lib/hadoop-hdfs/dn_socket', 'dfs.namenode.avoid.read.stale.datanode': 'true', 'dfs.hosts.exclude': '/etc/hadoop/conf/dfs.exclude', 'dfs.datanode.data.dir.perm': '750', 'dfs.namenode.write.stale.datanode.ratio': '1.0f', 'dfs.replication.max': '50', 'dfs.namenode.name.dir': '/hadoop/hdfs/namenode'}
    expected_put_path = '//clusters/test1'
    expected_post_request = {'Clusters': {'desired_configs': {'tag': 'version1', 'type': 'hdfs-site', 'properties':expected_properties}}}   
    expected_get_path = '//clusters/test1/configurations?type=hdfs-site&tag=version1'
    expected_get_request = None
        
    cluster = self.create_cluster(http_client_mock)
    existant_global_config = cluster.get_hdfs_site_config()
    existant_global_config.properties['dfs.namenode.avoid.write.stale.datanode'] = 'abc'
    cluster.update_hdfs_site_config(existant_global_config)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_any_call('PUT', expected_put_path, headers=None, payload=expected_post_request)
    http_client_mock.invoke.assert_any_call('GET', expected_get_path, headers=None, payload=expected_get_request)\
    
  def test_update_mapred_site_config(self):
    """
    Update mapred-site config
    """
    http_client_mock = MagicMock()
    
    expected_properties = {'mapreduce.jobhistory.address': 'abc', 'mapreduce.reduce.input.buffer.percent': '0.0', 'mapred.jobtracker.maxtasks.per.job': '-1', 'mapreduce.framework.name': 'yarn', 'mapreduce.map.speculative': 'false', 'mapreduce.tasktracker.healthchecker.script.path': 'file:////mapred/jobstatus', 'mapreduce.reduce.shuffle.merge.percent': '0.66', 'mapred.userlog.retain.hours': '24', 'yarn.app.mapreduce.am.resource.mb': '1024', 'mapreduce.reduce.shuffle.parallelcopies': '30', 'mapreduce.map.java.opts': '-Xmx320m', 'mapreduce.task.io.sort.factor': '100', 'mapreduce.application.classpath': '$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*,$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*', 'yarn.app.mapreduce.am.command-opts': '-Xmx756m', 'mapreduce.job.reduce.slowstart.completedmaps': '0.05', 'mapreduce.output.fileoutputformat.compress.type': 'BLOCK', 'mapreduce.reduce.speculative': 'false', 'mapreduce.reduce.java.opts': '-Xmx756m', 'mapreduce.am.max-attempts': '2', 'yarn.app.mapreduce.am.admin-command-opts': '-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN', 'mapreduce.jobtracker.system.dir': '/mapred/system', 'mapreduce.map.sort.spill.percent': '0.1', 'mapreduce.task.timeout': '600000', 'mapreduce.map.memory.mb': '1536', 'mapreduce.reduce.log.level': 'INFO', 'mapreduce.jobhistory.intermediate-done-dir': '/mr-history/tmp', 'mapreduce.reduce.memory.mb': '2048', 'mapreduce.tasktracker.map.tasks.maximum': '4', 'yarn.app.mapreduce.am.log.level': 'INFO', 'mapreduce.map.log.level': 'INFO', 'mapreduce.shuffle.port': '13562', 'mapred.jobtracker.taskScheduler': 'org.apache.hadoop.mapred.CapacityTaskScheduler', 'mapreduce.admin.user.env': 'LD_LIBRARY_PATH=/usr/lib/hadoop/lib/native:/usr/lib/hadoop/lib/native/`$JAVA_HOME/bin/java -d32 -version &amp;&gt; /dev/null;if [ $? -eq 0 ]; then echo Linux-i386-32; else echo Linux-amd64-64;fi`', 'mapreduce.jobhistory.webapp.address': 'dev05.hortonworks.com:19888', 'mapred.hosts.exclude': '/etc/hadoop/conf/mapred.exclude', 'mapreduce.reduce.shuffle.input.buffer.percent': '0.7', 'yarn.app.mapreduce.am.staging-dir': '/user', 'mapred.hosts': '/etc/hadoop/conf/mapred.include', 'mapreduce.jobhistory.done-dir': '/mr-history/done', 'mapreduce.admin.reduce.child.java.opts': '-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN', 'mapreduce.task.io.sort.mb': '200', 'mapred.task.tracker.task-controller': 'org.apache.hadoop.mapred.DefaultTaskController', 'mapreduce.admin.map.child.java.opts': '-Djava.net.preferIPv4Stack=true -Dhadoop.metrics.log.level=WARN'}
    expected_put_path = '//clusters/test1'
    expected_post_request = {'Clusters': {'desired_configs': {'tag': 'version1', 'type': 'mapred-site', 'properties':expected_properties}}}   
    expected_get_path = '//clusters/test1/configurations?type=mapred-site&tag=version1'
    expected_get_request = None
        
    cluster = self.create_cluster(http_client_mock)
    existant_global_config = cluster.get_mapred_site_config()
    existant_global_config.properties['mapreduce.jobhistory.address'] = 'abc'
    cluster.update_mapred_site_config(existant_global_config)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_any_call('PUT', expected_put_path, headers=None, payload=expected_post_request)
    http_client_mock.invoke.assert_any_call('GET', expected_get_path, headers=None, payload=expected_get_request)
    
  def test_create_services(self):
    """
    Create services
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/services'
    expected_request = [{'ServiceInfo': {'service_name': 'HDFS'}}, {'ServiceInfo': {'service_name': 'YARN'}}, {'ServiceInfo': {'service_name': 'MAPREDUCEv2'}}, {'ServiceInfo': {'service_name': 'TEZ'}}]
    
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.create_services(['HDFS','YARN','MAPREDUCEv2','TEZ'])
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=expected_request)
    
  def test_create_service_components(self):
    """
    Create service components
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/services/?ServiceInfo/service_name=HDFS'
    expected_request = {'components': [{'ServiceComponentInfo': {'component_name': u'NODEMANAGER'}}, {'ServiceComponentInfo': {'component_name': u'RESOURCEMANAGER'}}, {'ServiceComponentInfo': {'component_name': u'YARN_CLIENT'}}]}
    
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.create_service_components("2.0.5", "HDFS")
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=expected_request)
    
  def test_create_service_component(self):
    """
    Create service component
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/services/HDFS/components/NAMENODE'
    
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.create_service_component("2.0.5", "HDFS","NAMENODE")
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=None)
    
  def test_create_hosts(self):
    """
    Create cluster hosts
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/hosts'
    expected_request = [{'Hosts': {'ip': '1.2.3.4', 'host_name': 'hostname01', 'rack_info': '/default-rack'}}, {'Hosts': {'ip': '2.3.1.22', 'host_name': 'hostname02', 'rack_info': 'rack'}}]
        
    cluster = self.create_cluster(http_client_mock)
    host_list = [HostModel(None, 'hostname01','1.2.3.4'), HostModel(None, 'hostname02','2.3.1.22','rack')]
    resp = cluster.create_hosts(host_list)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=expected_request)
    
  def test_create_host(self):
    """
    Create cluster host
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/hosts'
    expected_request = [{'Hosts': {'ip': '1.2.3.4', 'host_name': 'hostname01', 'rack_info': '/default-rack'}}]
            
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.create_host('hostname01','1.2.3.4')
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=expected_request)
    
    
  def test_delete_host(self):
    """
    Delete cluster host
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/hosts/hostname01'
            
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.delete_host('hostname01')
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('DELETE', expected_path, headers=None, payload=None)
    
    
  def test_exceptions(self):
    """
    Test exceptions from ambari.client.core.errors
    """
    cluster = self.create_cluster()
    
    try:
      cluster.delete_host('deleted_nonexistant_cluster')
      print http_client_mock.invoke.call_args_list
      self.fail('Exception should have been thrown!')
    except BadRequest, ex:
      self.assertEquals(str(ex), 'exception: 400. Attempted to add unknown hosts to a cluster.  These hosts have not been registered with the server: dev05')
    except Exception, ex:
      self.fail('Wrong exception thrown!')
    
  def test_start_all_services(self):
    """
    Start all services
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/services?ServiceInfo/state=INSTALLED&params/run_smoke_test=true&params/reconfigure_client=false'
    expected_request = {'RequestInfo': {'context': 'Start All Services'}, 'Body': {'ServiceInfo': {'state': 'STARTED'}}}
            
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.start_all_services(True)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_request)
    
  def test_stop_all_services(self):
    """
    Stop all services
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/services?ServiceInfo'
    expected_request = {'RequestInfo': {'context': 'Stop All Services'}, 'Body': {'ServiceInfo': {'state': 'INSTALLED'}}}
            
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.stop_all_services()
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_request)
    
    
  def test_install_all_services(self):
    """
    Install all services
    """   
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/test1/services?ServiceInfo/state=INSTALLED'
    expected_request = {'RequestInfo': {'context': 'Install Services'}, 'Body': {'ServiceInfo': {'state': 'INSTALLED'}}}
            
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.install_all_services()
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_request)
    
  def test_add_config(self):
    """
    Set desired configurations
    """   
    http_client_mock = MagicMock()
    
    propr_dict = {"dfs_name_dir":"/data/1/hadoop/hdfs/namenode,/data/2/hadoop/hdfs/namenode,/data/3/hadoop/hdfs/namenode,/data/4/hadoop/hdfs/namenode,/data/5/hadoop/hdfs/namenode,/data/6/hadoop/hdfs/namenode,/data/7/hadoop/hdfs/namenode,/data/8/hadoop/hdfs/namenode", "namenode_heapsize":"1024m", "namenode_opt_newsize":"200m", 'namenode_opt_maxpermsize': '256m', 'namenode_opt_permsize': '128m', "fs_checkpoint_dir":"/data/1/hadoop/hdfs/namesecondary", "dfs_data_dir":"/data/1/hadoop/hdfs/data,/data/2/hadoop/hdfs/data,/data/3/hadoop/hdfs/data,/data/4/hadoop/hdfs/data,/data/5/hadoop/hdfs/data,/data/6/hadoop/hdfs/data,/data/7/hadoop/hdfs/data,/data/8/hadoop/hdfs/data,/data/9/hadoop/hdfs/data,/data/10/hadoop/hdfs/data", "dtnode_heapsize":"1024m", "dfs_datanode_failed_volume_tolerated":"0", "dfs_webhdfs_enabled":"true", "hadoop_heapsize":"1024", "datanode_du_reserved":"0", "fs_checkpoint_period":"21600", "fs_checkpoint_size":"67108864", "hdfs_log_dir_prefix":"/var/log/hadoop", "hadoop_pid_dir_prefix":"/var/run/hadoop", "namenode_opt_maxnewsize":"200m", "dfs_exclude":"dfs.exclude", "dfs_include":"dfs.include", "dfs_replication":"3", "dfs_block_local_path_access_user":"hbase", "dfs_datanode_data_dir_perm":"750", "security_enabled":"false", "namenode_formatted_mark_dir":"/var/run/hadoop/hdfs/namenode/formatted/", "hcat_conf_dir":"", "jtnode_opt_newsize":"200m", "jtnode_opt_maxnewsize":"200m", "jtnode_heapsize":"1024m", "mapred_local_dir":"/data/1/hadoop/mapred,/data/2/hadoop/mapred,/data/3/hadoop/mapred,/data/4/hadoop/mapred,/data/5/hadoop/mapred,/data/6/hadoop/mapred,/data/7/hadoop/mapred,/data/8/hadoop/mapred,/data/9/hadoop/mapred,/data/10/hadoop/mapred", "mapred_map_tasks_max":"4", "mapred_red_tasks_max":"2", "mapred_child_java_opts_sz":"768", "scheduler_name":"org.apache.hadoop.mapred.CapacityTaskScheduler", "mapred_cluster_map_mem_mb":"1536", "mapred_cluster_red_mem_mb":"2048", "mapred_cluster_max_map_mem_mb":"6144", "mapred_cluster_max_red_mem_mb":"4096", "mapred_job_map_mem_mb":"1536", "mapred_job_red_mem_mb":"2048", "io_sort_mb":"200", "io_sort_spill_percent":"0.9", "mapreduce_userlog_retainhours":"24", "maxtasks_per_job":"-1", "lzo_enabled":"true", "snappy_enabled":"true", "rca_enabled":"true", "mapred_system_dir":"/mapred/system", "mapred_hosts_exclude":"mapred.exclude", "mapred_hosts_include":"mapred.include", "mapred_jobstatus_dir":"file:////mapred/jobstatus", "nagios_web_login":"nagiosadmin", "nagios_web_password":"admin", "nagios_contact":"admin@admin.com", "nagios_group":"nagios", "hbase_conf_dir":"/etc/hbase", "proxyuser_group":"users", "dfs_datanode_address":"50010", "dfs_datanode_http_address":"50075", "apache_artifacts_download_url":"", "ganglia_runtime_dir":"/var/run/ganglia/hdp", "java64_home":"/usr/jdk/jdk1.6.0_31", "run_dir":"/var/run/hadoop", "hadoop_conf_dir":"/etc/hadoop", "hdfs_user":"hdfs", "mapred_user":"mapred", "hbase_user":"hbase", "hive_user":"hive", "hcat_user":"hcat", "webhcat_user":"hcat", "oozie_user":"oozie", "zk_user":"zookeeper", "gmetad_user":"nobody", "gmond_user":"nobody", "nagios_user":"nagios", "smokeuser":"ambari-qa", "user_group":"hadoop", "rrdcached_base_dir":"/var/lib/ganglia/rrds"}
    expected_path = '//clusters/test1'
    expected_request = {'Clusters': {'desired_configs': {'tag':'version1', 'type':'global', 'properties':propr_dict}}}
                
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.add_config("global","version1",propr_dict)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_request)
    
  def test_create_config(self):
    """
    Add a configuration
    """   
    http_client_mock = MagicMock()
    
    propr_dict = {"dfs_name_dir":"/data/1/hadoop/hdfs/namenode,/data/2/hadoop/hdfs/namenode,/data/3/hadoop/hdfs/namenode,/data/4/hadoop/hdfs/namenode,/data/5/hadoop/hdfs/namenode,/data/6/hadoop/hdfs/namenode,/data/7/hadoop/hdfs/namenode,/data/8/hadoop/hdfs/namenode", "namenode_heapsize":"1024m", "namenode_opt_newsize":"200m", 'namenode_opt_maxpermsize': '256m', 'namenode_opt_permsize': '128m', "fs_checkpoint_dir":"/data/1/hadoop/hdfs/namesecondary", "dfs_data_dir":"/data/1/hadoop/hdfs/data,/data/2/hadoop/hdfs/data,/data/3/hadoop/hdfs/data,/data/4/hadoop/hdfs/data,/data/5/hadoop/hdfs/data,/data/6/hadoop/hdfs/data,/data/7/hadoop/hdfs/data,/data/8/hadoop/hdfs/data,/data/9/hadoop/hdfs/data,/data/10/hadoop/hdfs/data", "dtnode_heapsize":"1024m", "dfs_datanode_failed_volume_tolerated":"0", "dfs_webhdfs_enabled":"true", "hadoop_heapsize":"1024", "datanode_du_reserved":"0", "fs_checkpoint_period":"21600", "fs_checkpoint_size":"67108864", "hdfs_log_dir_prefix":"/var/log/hadoop", "hadoop_pid_dir_prefix":"/var/run/hadoop", "namenode_opt_maxnewsize":"200m", "dfs_exclude":"dfs.exclude", "dfs_include":"dfs.include", "dfs_replication":"3", "dfs_block_local_path_access_user":"hbase", "dfs_datanode_data_dir_perm":"750", "security_enabled":"false", "namenode_formatted_mark_dir":"/var/run/hadoop/hdfs/namenode/formatted/", "hcat_conf_dir":"", "jtnode_opt_newsize":"200m", "jtnode_opt_maxnewsize":"200m", "jtnode_heapsize":"1024m", "mapred_local_dir":"/data/1/hadoop/mapred,/data/2/hadoop/mapred,/data/3/hadoop/mapred,/data/4/hadoop/mapred,/data/5/hadoop/mapred,/data/6/hadoop/mapred,/data/7/hadoop/mapred,/data/8/hadoop/mapred,/data/9/hadoop/mapred,/data/10/hadoop/mapred", "mapred_map_tasks_max":"4", "mapred_red_tasks_max":"2", "mapred_child_java_opts_sz":"768", "scheduler_name":"org.apache.hadoop.mapred.CapacityTaskScheduler", "mapred_cluster_map_mem_mb":"1536", "mapred_cluster_red_mem_mb":"2048", "mapred_cluster_max_map_mem_mb":"6144", "mapred_cluster_max_red_mem_mb":"4096", "mapred_job_map_mem_mb":"1536", "mapred_job_red_mem_mb":"2048", "io_sort_mb":"200", "io_sort_spill_percent":"0.9", "mapreduce_userlog_retainhours":"24", "maxtasks_per_job":"-1", "lzo_enabled":"true", "snappy_enabled":"true", "rca_enabled":"true", "mapred_system_dir":"/mapred/system", "mapred_hosts_exclude":"mapred.exclude", "mapred_hosts_include":"mapred.include", "mapred_jobstatus_dir":"file:////mapred/jobstatus", "nagios_web_login":"nagiosadmin", "nagios_web_password":"admin", "nagios_contact":"admin@admin.com", "nagios_group":"nagios", "hbase_conf_dir":"/etc/hbase", "proxyuser_group":"users", "dfs_datanode_address":"50010", "dfs_datanode_http_address":"50075", "apache_artifacts_download_url":"", "ganglia_runtime_dir":"/var/run/ganglia/hdp", "java64_home":"/usr/jdk/jdk1.6.0_31", "run_dir":"/var/run/hadoop", "hadoop_conf_dir":"/etc/hadoop", "hdfs_user":"hdfs", "mapred_user":"mapred", "hbase_user":"hbase", "hive_user":"hive", "hcat_user":"hcat", "webhcat_user":"hcat", "oozie_user":"oozie", "zk_user":"zookeeper", "gmetad_user":"nobody", "gmond_user":"nobody", "nagios_user":"nagios", "smokeuser":"ambari-qa", "user_group":"hadoop", "rrdcached_base_dir":"/var/lib/ganglia/rrds"}
    expected_path = '//clusters/test1'
    expected_request = {'tag':'version1', 'type':'global', 'properties':propr_dict}
                
    cluster = self.create_cluster(http_client_mock)
    resp = cluster.create_config("global","version1",propr_dict)
    
    self.assertEqual(cluster.cluster_name, "test1")
    http_client_mock.invoke.assert_called_with('PUT', expected_path, headers=None, payload=expected_request)
    
    
