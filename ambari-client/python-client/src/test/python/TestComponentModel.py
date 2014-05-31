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

import unittest

class TestClusterModel(unittest.TestCase):

  def setUp(self):
    http_client_logger = logging.getLogger()
    http_client_logger.info('Running test:' + self.id())

  def create_component(self, http_client_mock = MagicMock()):
    http_client_mock.invoke.side_effect = HttpClientInvoker.http_client_invoke_side_effects
    client = AmbariClient("localhost", 8080, "admin", "admin", version=1, client=http_client_mock)
    cluster = client.get_cluster('test1')
    host = cluster.get_host('myhost')
    component = host.get_host_component("DATANODE")
    return component

  def test_component_get_metrics(self):
    http_client_mock = MagicMock()

    expected_path = '//clusters/cl1/hosts/myhost/host_components/DATANODE?fields=metrics'
    expected_json_output = {u'HostRoles': {u'cluster_name': u'cl1', u'host_name': u'myhost', u'component_name': u'DATANODE'}, u'metrics': {u'load': {u'load_one': 0.0125555555556, u'load_five': 0.059277777777800002, u'load_fifteen': 0.069222222222199994}, u'ugi': {u'loginSuccess_avg_time': 0.0, u'loginFailure_avg_time': 0.0, u'loginSuccess_num_ops': 0.0, u'loginFailure_num_ops': 0.0}, u'network': {u'bytes_in': 30989.887416699999, u'pkts_in': 44.982222222200001, u'pkts_out': 214.35891666699999, u'bytes_out': 98799.674277800004}, u'process': {u'proc_total': 682.39722222199998, u'proc_run': 2.0}, u'dfs': {u'datanode': {u'replaceBlockOp_num_ops': 0.0, u'replaceBlockOp_avg_time': 0.0, u'blockChecksumOp_avg_time': 0.0, u'copyBlockOp_avg_time': 0.0, u'copyBlockOp_num_ops': 0.0, u'heartBeats_avg_time': 1.69166666667, u'writes_from_local_client': 0.0, u'blockReports_avg_time': 6.0, u'blocks_written': 0.0, u'writeBlockOp_num_ops': 0.0, u'bytes_read': 0.0, u'writeBlockOp_avg_time': 13.896907216500001, u'writes_from_remote_client': 0.0, u'blocks_read': 0.0, u'readBlockOp_avg_time': 0.0, u'reads_from_remote_client': 0.0, u'block_verification_failures': 0.0, u'reads_from_local_client': 0.0, u'blocks_removed': 0.0, u'blocks_get_local_pathinfo': 0.0, u'blockReports_num_ops': 0.0, u'heartBeats_num_ops': 0.33648148148099999, u'blocks_verified': 0.0, u'bytes_written': 0.0, u'readBlockOp_num_ops': 0.0, u'blocks_replicated': 0.0, u'blockChecksumOp_num_ops': 0.0}, u'FSNamesystem': {u'VolumeInfo': u'{"/hadoop/hdfs/data/current":{"freeSpace":495195869184,"usedSpace":345120768,"reservedSpace":1073741824}}', u'HttpPort': None, u'RpcPort': u'8010', u'NamenodeAddress': u'{"myhost":"BP-442795920-192.168.64.101-1383132565020"}', u'Version': u'2.2.0.2.0.6.0-76'}}, u'rpc': {u'NumOpenConnections': 0.0, u'RpcProcessingTime_avg_time': 0.0, u'rpcAuthorizationFailures': 0.0, u'callQueueLen': 0.0, u'RpcProcessingTime_num_ops': 0.0, u'RpcQueueTime_avg_time': 0.0, u'rpcAuthorizationSuccesses': 0.0, u'rpcAuthenticationSuccesses': 0.0, u'rpcAuthenticationFailures': 0.0, u'ReceivedBytes': 0.0, u'RpcQueueTime_num_ops': 0.0, u'SentBytes': 0.0}, u'boottime': 1383131209.0, u'jvm': {u'NonHeapMemoryMax': 136314880, u'logWarn': 0.0, u'gcCount': 0.011111111111100001, u'threadsRunnable': 8.0416666666700003, u'memHeapCommittedM': 28.5625, u'threadsWaiting': 18.0, u'NonHeapMemoryUsed': 30798600, u'threadsTimedWaiting': 8.9166666666700003, u'threadsNew': 0.0, u'HeapMemoryUsed': 11395264, u'memHeapUsedM': 11.7175731111, u'memNonHeapUsedM': 29.360076750000001, u'threadsTerminated': 0.0, u'logInfo': 0.0, u'logError': 0.0, u'HeapMemoryMax': 1037959168, u'threadsBlocked': 0.0, u'logFatal': 0.0, u'memNonHeapCommittedM': 29.625, u'gcTimeMillis': 388}, u'memory': {u'mem_cached': 160191.85555599999, u'swap_free': 2593920.0, u'mem_free': 183983.85555599999, u'mem_buffers': 23914.266666700001, u'mem_shared': 0.0, u'swap_total': 2621432.0, u'mem_total': 1922680.0}, u'disk': {u'disk_total': 525.78999999999996, u'disk_free': 495.64499999999998, u'part_max_used': 11.6}, u'cpu': {u'cpu_idle': 85.730000000000004, u'cpu_num': 1.0, u'cpu_wio': 0.0041666666666699999, u'cpu_user': 7.4288888888900004, u'cpu_aidle': 0.0, u'cpu_system': 6.8458333333299999, u'cpu_speed': 2967.0, u'cpu_nice': 0.0}}, u'host': {u'href': u'http://192.168.64.101:8080/api/v1/clusters/cl1/hosts/myhost'}, u'href': u'http://192.168.64.101:8080/api/v1/clusters/cl1/hosts/myhost/host_components/DATANODE?fields=metrics'}

    component = self.create_component(http_client_mock)
    metrics_json = component.get_metrics()

    self.assertEqual(expected_json_output,metrics_json)
    http_client_mock.invoke.assert_called_with('GET', expected_path, headers=None, payload=None)
    pass
