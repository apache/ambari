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
    expected_output = {'items': [{'cluster_name': u'test1', 'version': u'HDP-1.2.1'}]}

    client = self.create_client()
    all_clusters = client.get_all_clusters()

    self.assertEqual(len(all_clusters), 1)
    self.assertEqual(all_clusters.to_json_dict(), expected_output)

  def test_get_cluster(self):
    """
    Get all clusters.
    This testcase checks if get_all_clusters returns a list of ModelList.
    """
    expected_dict_output = {'cluster_name': u'test1', 'version': u'HDP-1.2.1'}

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
    expected_hosts_dict = {'items': [{'ip': None, 'host_name': u'apspal44-83', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'apspal44-84', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'apspal44-85', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'apspal44-86', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'apspal44-87', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'apspal44-88', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'apspal44-89', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'r01hn01', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'r01mgt', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'r01wn01', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'r01wn02', 'rack_info': '/default-rack'}, {'ip': None, 'host_name': u'r01wn03', 'rack_info': '/default-rack'}]}

    client = self.create_client()
    all_hosts = client.get_all_hosts()

    self.assertEqual(len(all_hosts), 12, "There should be 12 hosts from the response")
    self.assertEqual(all_hosts.to_json_dict(), expected_hosts_dict)

  def ADisabledtest_bootstrap_hosts(self):
    """
    Test Bootstrap
    """
    http_client_mock = MagicMock()

    ssh_key = 'abc!@#$%^&*()_:"|<>?[];\'\\./'
    host_list = ['dev05.hortonworks.com', 'dev06.hortonworks.com']
    ssh_user = 'root'

    expected_path = '//bootstrap'
    expected_headers = {'Content-Type': 'application/json'}
    expected_request = {'user': ssh_user, 'hosts': str(host_list), 'verbose': True, 'sshKey': ssh_key}
    expected_response = {'status': 201, 'message': u'Running Bootstrap now.', 'requestId': 5}

    client = self.create_client(http_client_mock)
    resp = client.bootstrap_hosts(host_list, ssh_key, ssh_user)

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
    expected_dict = {'items': [{'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'datanode_du_reserved', 'property_value': u'1'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.access.time.precision', 'property_value': u'0'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.balance.bandwidthPerSec', 'property_value': u'6250000'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.block.access.token.enable', 'property_value': u'true'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.block.size', 'property_value': u'134217728'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.blockreport.initialDelay', 'property_value': u'120'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.cluster.administrators', 'property_value': u' hdfs'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.datanode.du.pct', 'property_value': u'0.85f'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.datanode.failed.volumes.tolerated', 'property_value': u'0'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.datanode.ipc.address', 'property_value': u'0.0.0.0:8010'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.datanode.max.xcievers', 'property_value': u'4096'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.datanode.socket.write.timeout', 'property_value': u'0'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.heartbeat.interval', 'property_value': u'3'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.https.port', 'property_value': u'50470'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.namenode.avoid.read.stale.datanode', 'property_value': u'true'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.namenode.avoid.write.stale.datanode', 'property_value': u'true'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.namenode.handler.count', 'property_value': u'100'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.namenode.handler.count', 'property_value': u'40'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.namenode.stale.datanode.interval', 'property_value': u'30000'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.namenode.write.stale.datanode.ratio', 'property_value': u'1.0f'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.permissions', 'property_value': u'true'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.permissions.supergroup', 'property_value': u'hdfs'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.replication.max', 'property_value': u'50'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.safemode.threshold.pct', 'property_value': u'1.0f'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.secondary.https.port', 'property_value': u'50490'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.umaskmode', 'property_value': u'077'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs.web.ugi', 'property_value': u'gopher,gopher'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_block_local_path_access_user', 'property_value': u'hbase'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_data_dir', 'property_value': u'/hadoop/hdfs/data'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_datanode_address', 'property_value': u'50010'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_datanode_data_dir_perm', 'property_value': u'750'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_datanode_failed_volume_tolerated', 'property_value': u'0'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_datanode_http_address', 'property_value': u'50075'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_name_dir', 'property_value': u'/hadoop/hdfs/namenode'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_replication', 'property_value': u'3'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dfs_webhdfs_enabled', 'property_value': u'true'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'dtnode_heapsize', 'property_value': u'1024'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs.checkpoint.edits.dir', 'property_value': u'${fs.checkpoint.dir}'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs.checkpoint.period', 'property_value': u'21600'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs.checkpoint.size', 'property_value': u'536870912'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs.trash.interval', 'property_value': u'360'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs_checkpoint_dir', 'property_value': u'/hadoop/hdfs/namesecondary'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs_checkpoint_period', 'property_value': u'21600'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'fs_checkpoint_size', 'property_value': u'0.5'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'hadoop.security.authentication', 'property_value': u'simple'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'hadoop_heapsize', 'property_value': u'1024'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'hadoop_pid_dir_prefix', 'property_value': u'/var/run/hadoop'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'hdfs_log_dir_prefix', 'property_value': u'/var/log/hadoop'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'hdfs_user', 'property_value': u'hdfs'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'io.compression.codec.lzo.class', 'property_value': u'com.hadoop.compression.lzo.LzoCodec'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'io.compression.codecs', 'property_value': u'org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec,org.apache.hadoop.io.compress.SnappyCodec'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'io.file.buffer.size', 'property_value': u'131072'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'io.serializations', 'property_value': u'org.apache.hadoop.io.serializer.WritableSerialization'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'ipc.client.connect.max.retries', 'property_value': u'50'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'ipc.client.connection.maxidletime', 'property_value': u'30000'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'ipc.client.idlethreshold', 'property_value': u'8000'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'ipc.server.max.response.size', 'property_value': u'5242880'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'ipc.server.read.threadpool.size', 'property_value': u'5'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'kerberos_domain', 'property_value': u'EXAMPLE.COM'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'keytab_path', 'property_value': u'/etc/security/keytabs'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'namenode_formatted_mark_dir', 'property_value': u'/var/run/hadoop/hdfs/namenode/formatted/'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'namenode_heapsize', 'property_value': u'1024'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'namenode_opt_maxnewsize', 'property_value': u'640'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'namenode_opt_newsize', 'property_value': u'200'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'namenode_opt_maxpermsize', 'property_value': u'256'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'namenode_opt_permsize', 'property_value': u'128'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'proxyuser_group', 'property_value': u'users'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.client.datanode.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.client.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.datanode.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.inter.datanode.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.inter.tracker.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.job.submission.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.namenode.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security.task.umbilical.protocol.acl', 'property_value': u'*'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'security_enabled', 'property_value': u'false'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'property_name': u'webinterface.private.actions', 'property_value': u'false'}]}
    expected_first_item = StackConfigModel(None, property_name='datanode_du_reserved' , property_value='1' , service_name='HDFS' , stack_version='1.3.0')
    expected_request = None

    client = self.create_client()
    configs = client.get_config('1.3.0','HDFS')


    self.assertEquals(len(configs), 77)
    self.assertEquals(str(configs[0]),str(expected_first_item))
    self.assertEquals(configs.to_json_dict(), expected_dict)

  def test_get_components(self):
    """
    Test get components
    """
    expected_dict = {'items': [{'stack_version': u'1.3.0', 'service_name': u'HDFS', 'component_name': u'DATANODE'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'component_name': u'HDFS_CLIENT'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'component_name': u'NAMENODE'}, {'stack_version': u'1.3.0', 'service_name': u'HDFS', 'component_name': u'SECONDARY_NAMENODE'}]}
    expected_first_item = StackComponentModel(None, component_name='DATANODE', service_name='HDFS' , stack_version='1.3.0')
    expected_request = None

    client = self.create_client()
    components = client.get_components('1.3.0','HDFS')

    self.assertEquals(len(components), 4)
    self.assertEquals(str(components[0]),str(expected_first_item))
    self.assertEquals(components.to_json_dict(), expected_dict)

