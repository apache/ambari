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
    expected_json_output = {'HostRoles': {'cluster_name': 'cl1', 'host_name': 'myhost', 'component_name': 'DATANODE'}, 'metrics': {'load': {'load_one': 0.0125555555556, 'load_five': 0.059277777777800002, 'load_fifteen': 0.069222222222199994}, 'ugi': {'loginSuccess_avg_time': 0.0, 'loginFailure_avg_time': 0.0, 'loginSuccess_num_ops': 0.0, 'loginFailure_num_ops': 0.0}, 'network': {'bytes_in': 30989.887416699999, 'pkts_in': 44.982222222200001, 'pkts_out': 214.35891666699999, 'bytes_out': 98799.674277800004}, 'process': {'proc_total': 682.39722222199998, 'proc_run': 2.0}, 'dfs': {'datanode': {'replaceBlockOp_num_ops': 0.0, 'replaceBlockOp_avg_time': 0.0, 'blockChecksumOp_avg_time': 0.0, 'copyBlockOp_avg_time': 0.0, 'copyBlockOp_num_ops': 0.0, 'heartBeats_avg_time': 1.69166666667, 'writes_from_local_client': 0.0, 'blockReports_avg_time': 6.0, 'blocks_written': 0.0, 'writeBlockOp_num_ops': 0.0, 'bytes_read': 0.0, 'writeBlockOp_avg_time': 13.896907216500001, 'writes_from_remote_client': 0.0, 'blocks_read': 0.0, 'readBlockOp_avg_time': 0.0, 'reads_from_remote_client': 0.0, 'block_verification_failures': 0.0, 'reads_from_local_client': 0.0, 'blocks_removed': 0.0, 'blocks_get_local_pathinfo': 0.0, 'blockReports_num_ops': 0.0, 'heartBeats_num_ops': 0.33648148148099999, 'blocks_verified': 0.0, 'bytes_written': 0.0, 'readBlockOp_num_ops': 0.0, 'blocks_replicated': 0.0, 'blockChecksumOp_num_ops': 0.0}, 'FSNamesystem': {'VolumeInfo': '{"/hadoop/hdfs/data/current":{"freeSpace":495195869184,"usedSpace":345120768,"reservedSpace":1073741824}}', 'HttpPort': None, 'RpcPort': '8010', 'NamenodeAddress': '{"myhost":"BP-442795920-192.168.64.101-1383132565020"}', 'Version': '2.2.0.2.0.6.0-76'}}, 'rpc': {'NumOpenConnections': 0.0, 'RpcProcessingTime_avg_time': 0.0, 'rpcAuthorizationFailures': 0.0, 'callQueueLen': 0.0, 'RpcProcessingTime_num_ops': 0.0, 'RpcQueueTime_avg_time': 0.0, 'rpcAuthorizationSuccesses': 0.0, 'rpcAuthenticationSuccesses': 0.0, 'rpcAuthenticationFailures': 0.0, 'ReceivedBytes': 0.0, 'RpcQueueTime_num_ops': 0.0, 'SentBytes': 0.0}, 'boottime': 1383131209.0, 'jvm': {'NonHeapMemoryMax': 136314880, 'logWarn': 0.0, 'gcCount': 0.011111111111100001, 'threadsRunnable': 8.0416666666700003, 'memHeapCommittedM': 28.5625, 'threadsWaiting': 18.0, 'NonHeapMemoryUsed': 30798600, 'threadsTimedWaiting': 8.9166666666700003, 'threadsNew': 0.0, 'HeapMemoryUsed': 11395264, 'memHeapUsedM': 11.7175731111, 'memNonHeapUsedM': 29.360076750000001, 'threadsTerminated': 0.0, 'logInfo': 0.0, 'logError': 0.0, 'HeapMemoryMax': 1037959168, 'threadsBlocked': 0.0, 'logFatal': 0.0, 'memNonHeapCommittedM': 29.625, 'gcTimeMillis': 388}, 'memory': {'mem_cached': 160191.85555599999, 'swap_free': 2593920.0, 'mem_free': 183983.85555599999, 'mem_buffers': 23914.266666700001, 'mem_shared': 0.0, 'swap_total': 2621432.0, 'mem_total': 1922680.0}, 'disk': {'disk_total': 525.78999999999996, 'disk_free': 495.64499999999998, 'part_max_used': 11.6}, 'cpu': {'cpu_idle': 85.730000000000004, 'cpu_num': 1.0, 'cpu_wio': 0.0041666666666699999, 'cpu_user': 7.4288888888900004, 'cpu_aidle': 0.0, 'cpu_system': 6.8458333333299999, 'cpu_speed': 2967.0, 'cpu_nice': 0.0}}, 'host': {'href': 'http://192.168.64.101:8080/api/v1/clusters/cl1/hosts/myhost'}, 'href': 'http://192.168.64.101:8080/api/v1/clusters/cl1/hosts/myhost/host_components/DATANODE?fields=metrics'}

    component = self.create_component(http_client_mock)
    metrics_json = component.get_metrics()

    self.assertEqual(expected_json_output,metrics_json)
    http_client_mock.invoke.assert_called_with('GET', expected_path, headers=None, payload=None)
    pass
