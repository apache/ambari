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


from mock.mock import MagicMock, patch
from ambari_client.ambari_api import  AmbariClient 
from HttpClientInvoker import HttpClientInvoker
from ambari_client.model.stack import StackConfigModel, StackComponentModel
import unittest
import logging

class TestAmbariClient(unittest.TestCase):

  def setUp(self):
    http_client_logger = logging.getLogger()
    http_client_logger.info('Running test:' + self.id())

  def create_client(self, http_client_mock = MagicMock()):
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    return client

  def test_init(self):
    """
    AmbariClient is the top-level root resources.
    This testcase checks if when the  init method was called &
    the httpclient was initialized
    """
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1)
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "http://localhost:8080/api/v1",
                       "host_url should be http://localhost:8080/api/v1")
      
    client = AmbariClient(host_name="localhost", user_name="admin", password="admin")
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "http://localhost:8080/api/v1",
                       "host_url should be http://localhost:8080/api/v1")
      
    client = AmbariClient(host_name="localhost")
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "http://localhost:8080/api/v1",
                       "host_url should be http://localhost:8080/api/v1")
    
    
    client = AmbariClient("localhost", 8443, "admin", "admin", use_https=True)
    self.assertEqual(client.version, 1, "version should be 1")
    self.assertEqual(client.host_url, "https://localhost:8443/api/v1",
                       "host_url should be https://localhost:8443/api/v1")
      
  def test_get_all_clusters(self):
    """
    Get all clusters.
    This testcase checks if get_all_clusters returns a list of ModelList.
    """
    expected_output = {'items': [{'cluster_name': 'test1', 'version': 'HDP-1.2.1'}]}
      
    client = self.create_client()
    all_clusters = client.get_all_clusters()
      
    self.assertEqual(len(all_clusters), 1)
    self.assertEqual(all_clusters.to_json_dict(), expected_output)
    
  def test_get_cluster(self):
    """
    Get all clusters.
    This testcase checks if get_all_clusters returns a list of ModelList.
    """
    expected_dict_output = {'cluster_name': 'test1', 'version': 'HDP-1.2.1'}
    
    client = self.create_client()
    cluster = client.get_cluster('test1')
    
    self.assertEqual(cluster.cluster_name, "test1", "cluster_name should be test1 ")
    self.assertEqual(cluster.to_json_dict(), expected_dict_output, "to_json_dict should convert ClusterModel")
    
  def test_get_host(self):
    """
    Get host
    This testcase checks if client.get_host returns a correct host
    """
    expected_dict_output = {'ip': '10.0.2.15', 'host_name': 'dev06.hortonworks.com', 'rack_info': '/default-rack'}
    
    client = self.create_client()
    host = client.get_host('dev06.hortonworks.com')
    
    self.assertEqual(host.to_json_dict(), expected_dict_output)
    self.assertEqual(host.host_state, "HEARTBEAT_LOST")
     
  def test_get_all_hosts(self):
    """
    Get all hosts.
    This testcase checks if get_all_hosts returns a list of ModelList.
    """
    expected_hosts_dict = {'items': [{'ip': None, 'host_name': 'apspal44-83', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'apspal44-84', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'apspal44-85', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'apspal44-86', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'apspal44-87', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'apspal44-88', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'apspal44-89', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'r01hn01', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'r01mgt', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'r01wn01', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'r01wn02', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': 'r01wn03', 'rack_info': '/default-rack'}]}
      
    client = self.create_client()
    all_hosts = client.get_all_hosts()
    
    self.assertEqual(len(all_hosts), 12, "There should be 12 hosts from the response")
    self.assertEqual(all_hosts.to_json_dict(), expected_hosts_dict)
    
  def test_bootstrap_hosts(self):
    """
    Test Bootstrap
    """
    http_client_mock = MagicMock()
    
    ssh_key = 'abc!@#$%^&*()_:"|<>?[];\'\\./'
    host_list = ['dev05.hortonworks.com','dev06.hortonworks.com']
    
    expected_path = '//bootstrap'
    expected_headers = {'Content-Type': 'application/json'}
    expected_request = {'hosts': host_list, 'sshKey': 'abc!@#$%^&*()_:"|<>?[];\\\'\\\\./'}
    expected_response = {'status': 201, 'message': 'Running Bootstrap now.', 'requestId': 5}
                               
    client = self.create_client(http_client_mock)
    resp = client.bootstrap_hosts(host_list, ssh_key)

    self.assertEqual(resp.to_json_dict(), expected_response)
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=expected_headers, payload=expected_request)
  
  def test_create_cluster(self):
    """
    Test create cluster
    """
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/c1'
    expected_request = {'Clusters': {'version': 'HDP-2.0.5'}}
          
    client = self.create_client(http_client_mock)
    resp = client.create_cluster('c1', 'HDP-2.0.5')
    
    http_client_mock.invoke.assert_called_with('POST', expected_path, headers=None, payload=expected_request)
    
  def test_delete_cluster(self):
    """
    Test create cluster
    """
    http_client_mock = MagicMock()
    
    expected_path = '//clusters/c1'
    expected_request = None
          
    client = self.create_client(http_client_mock)
    resp = client.delete_cluster('c1')
    
    http_client_mock.invoke.assert_called_with('DELETE', expected_path, headers=None, payload=expected_request)
    
  def test_delete_host(self):
    """
    Test delete host
    """
    http_client_mock = MagicMock()
    
    expected_path = '//hosts/abc.abc.abc'
    expected_request = None
          
    client = self.create_client(http_client_mock)
    resp = client.delete_host('abc.abc.abc')
    
    http_client_mock.invoke.assert_called_with('DELETE', expected_path, headers=None, payload=expected_request)
    
  def test_get_config(self):
    """
    Test get config
    """
    expected_dict = {'items': [{'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'datanode_du_reserved', 'property_value': '1'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.access.time.precision', 'property_value': '0'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.balance.bandwidthPerSec', 'property_value': '6250000'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.block.access.token.enable', 'property_value': 'true'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.block.size', 'property_value': '134217728'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.blockreport.initialDelay', 'property_value': '120'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.cluster.administrators', 'property_value': ' hdfs'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.datanode.du.pct', 'property_value': '0.85f'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.datanode.failed.volumes.tolerated', 'property_value': '0'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.datanode.ipc.address', 'property_value': '0.0.0.0:8010'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.datanode.max.xcievers', 'property_value': '4096'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.datanode.socket.write.timeout', 'property_value': '0'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.heartbeat.interval', 'property_value': '3'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.https.port', 'property_value': '50470'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.namenode.avoid.read.stale.datanode', 'property_value': 'true'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.namenode.avoid.write.stale.datanode', 'property_value': 'true'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.namenode.handler.count', 'property_value': '100'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.namenode.handler.count', 'property_value': '40'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.namenode.stale.datanode.interval', 'property_value': '30000'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.namenode.write.stale.datanode.ratio', 'property_value': '1.0f'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.permissions', 'property_value': 'true'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.permissions.supergroup', 'property_value': 'hdfs'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.replication.max', 'property_value': '50'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.safemode.threshold.pct', 'property_value': '1.0f'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.secondary.https.port', 'property_value': '50490'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.umaskmode', 'property_value': '077'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs.web.ugi', 'property_value': 'gopher,gopher'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_block_local_path_access_user', 'property_value': 'hbase'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_data_dir', 'property_value': '/hadoop/hdfs/data'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_datanode_address', 'property_value': '50010'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_datanode_data_dir_perm', 'property_value': '750'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_datanode_failed_volume_tolerated', 'property_value': '0'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_datanode_http_address', 'property_value': '50075'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_name_dir', 'property_value': '/hadoop/hdfs/namenode'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_replication', 'property_value': '3'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dfs_webhdfs_enabled', 'property_value': 'true'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'dtnode_heapsize', 'property_value': '1024'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs.checkpoint.edits.dir', 'property_value': '${fs.checkpoint.dir}'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs.checkpoint.period', 'property_value': '21600'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs.checkpoint.size', 'property_value': '536870912'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs.trash.interval', 'property_value': '360'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs_checkpoint_dir', 'property_value': '/hadoop/hdfs/namesecondary'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs_checkpoint_period', 'property_value': '21600'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'fs_checkpoint_size', 'property_value': '0.5'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'hadoop.security.authentication', 'property_value': 'simple'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'hadoop_heapsize', 'property_value': '1024'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'hadoop_pid_dir_prefix', 'property_value': '/var/run/hadoop'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'hdfs_log_dir_prefix', 'property_value': '/var/log/hadoop'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'hdfs_user', 'property_value': 'hdfs'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'io.compression.codec.lzo.class', 'property_value': 'com.hadoop.compression.lzo.LzoCodec'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'io.compression.codecs', 'property_value': 'org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec,org.apache.hadoop.io.compress.SnappyCodec'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'io.file.buffer.size', 'property_value': '131072'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'io.serializations', 'property_value': 'org.apache.hadoop.io.serializer.WritableSerialization'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'ipc.client.connect.max.retries', 'property_value': '50'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'ipc.client.connection.maxidletime', 'property_value': '30000'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'ipc.client.idlethreshold', 'property_value': '8000'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'ipc.server.max.response.size', 'property_value': '5242880'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'ipc.server.read.threadpool.size', 'property_value': '5'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'kerberos_domain', 'property_value': 'EXAMPLE.COM'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'keytab_path', 'property_value': '/etc/security/keytabs'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'namenode_formatted_mark_dir', 'property_value': '/var/run/hadoop/hdfs/namenode/formatted/'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'namenode_heapsize', 'property_value': '1024'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'namenode_opt_maxnewsize', 'property_value': '640'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'namenode_opt_newsize', 'property_value': '200'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'proxyuser_group', 'property_value': 'users'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.client.datanode.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.client.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.datanode.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.inter.datanode.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.inter.tracker.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.job.submission.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.namenode.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security.task.umbilical.protocol.acl', 'property_value': '*'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'security_enabled', 'property_value': 'false'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'property_name': 'webinterface.private.actions', 'property_value': 'false'}]}
    expected_first_item = StackConfigModel(None, property_name='datanode_du_reserved' , property_value='1' , service_name='HDFS' , stack_version='1.3.0')    
    expected_request = None
              
    client = self.create_client()
    configs = client.get_config('1.3.0','HDFS')
    
        
    self.assertEqual(len(configs), 75)
    self.assertEqual(str(configs[0]),str(expected_first_item))
    self.assertEqual(configs.to_json_dict(), expected_dict)
    
  def test_get_components(self):
    """
    Test get components
    """
    expected_dict = {'items': [{'stack_version': '1.3.0', 'service_name': 'HDFS', 'component_name': 'DATANODE'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'component_name': 'HDFS_CLIENT'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'component_name': 'NAMENODE'}, {'stack_version': '1.3.0', 'service_name': 'HDFS', 'component_name': 'SECONDARY_NAMENODE'}]}
    expected_first_item = StackComponentModel(None, component_name='DATANODE', service_name='HDFS' , stack_version='1.3.0')    
    expected_request = None
              
    client = self.create_client()
    components = client.get_components('1.3.0','HDFS')
        
    self.assertEqual(len(components), 4)
    self.assertEqual(str(components[0]),str(expected_first_item))
    self.assertEqual(components.to_json_dict(), expected_dict)
  