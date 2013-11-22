/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var App = require('app');

/**
 * Config for each ajax-request
 *
 * Fields example:
 *  mock - testMode url
 *  real - real url (without API prefix)
 *  type - request type (also may be defined in the format method)
 *  format - function for processing ajax params after default formatRequest. Return ajax-params object
 *  testInProduction - can this request be executed on production tests (used only in tests)
 *
 * @type {Object}
 */
var urls = {
  'background_operations.get_most_recent': {
    'real': '/clusters/{clusterName}/requests?to=end&page_size=10&fields=Requests',
    'mock': '/data/background_operations/list_on_start.json',
    'testInProduction': true
  },
  'background_operations.get_by_request': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=*,tasks/Tasks/command,tasks/Tasks/exit_code,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status',
    'mock': '/data/background_operations/task_by_request{requestId}.json',
    'testInProduction': true,
    'format': function (data) {
      return {
        async: !data.sync
      };
    }
  },
  'background_operations.get_by_task': {
    'real': '/clusters/{clusterName}/requests/{requestId}/tasks/{taskId}',
    'mock': '/data/background_operations/list_on_start.json',
    'testInProduction': true,
    'format': function (data) {
      return {
        async: !data.sync
      };
    }
  },
  'service.item.start_stop': {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.requestInfo
          },
          Body: {
            ServiceInfo: {
              state: data.state
            }
          }
        })
      };
    }
  },
  'service.item.smoke': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        'type': 'POST',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.displayName + " Smoke Test",
            "command" : data.actionName,
            "service_name" : data.serviceName
          }
        })
      };
    }
  },
  'service.load_config_groups': {
    'real': '/clusters/{clusterName}/config_groups?fields=*'
  },
  'reassign.stop_services': {
    'real': '/clusters/{clusterName}/services',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Stop all services"
          },
          "Body": {
            "ServiceInfo": {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'reassign.start_services': {
    'real': '/clusters/{clusterName}/services?params/run_smoke_test=true',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Start all services"
          },
          "Body": {
            "ServiceInfo": {
              "state": "STARTED"
            }
          }
        })
      }
    }
  },
  'reassign.maintenance_mode': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function () {
      return {
        data: JSON.stringify(
          {
            "HostRoles": {
              "state": "MAINTENANCE"
            }
          }
        )
      }
    }
  },
  'reassign.remove_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'DELETE'
  },

  'reassign.load_configs': {
    'real': '/clusters/{clusterName}/configurations?{urlParams}',
    'mock': ''
  },

  'reassign.save_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        async: false,
        data: JSON.stringify({
          Clusters: {
            desired_config: {
              "type": data.siteName,
              "tag": 'version' + (new Date).getTime(),
              "properties": data.properties
            }
          }
        })
      }
    }
  },

  'config.advanced': {
    'real': '{stack2VersionUrl}/stackServices/{serviceName}/configurations?fields=*',
    'mock': '/data/wizard/stack/hdp/version{stackVersion}/{serviceName}.json',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'config.advanced.global': {
    'real': '{stack2VersionUrl}/stackServices?fields=configurations/StackConfigurations/type',
    'mock': '/data/wizard/stack/hdp/version1.3.0/global.json',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'config.tags': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs',
    'mock': '/data/clusters/cluster.json'
  },
  'config.tags_and_groups': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs,config_groups/*',
    'mock': '/data/clusters/tags_and_groups.json'
  },
  'config.tags.sync': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs',
    'mock': '/data/clusters/cluster.json',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'config_groups.all_fields': {
    'real': '/clusters/{clusterName}/config_groups?fields=*'
  },
  'config_groups.get_config_group_by_id': {
    'real': '/clusters/{clusterName}/config_groups/{id}'
  },
  'config_groups.update_config_group': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        async: false,
        data: JSON.stringify(
          [
            data.configGroup
          ]
        )
      }
    }
  },
  'config_groups.delete_config_group': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': '',
    'type': 'DELETE'
  },
  'config.on_site': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/cluster_level_configs.json?{params}',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'config.host_overrides': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/host_level_overrides_configs.json?{params}',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'config.stale.stop_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": "Stop " + data.displayName
          },
          Body: {
            "HostRoles": {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'config.stale.start_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": "Start " + data.displayName
          },
          Body: {
            "HostRoles": {
              "state": "STARTED"
            }
          }
        })
      }
    }
  },
  'service.metrics.flume.channel_fill_percent': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/flume/flume/CHANNEL/*/ChannelFillPercentage[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/channelFillPct.json',
    'testInProduction': true
  },
  'service.metrics.flume.channel_size': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/flume/flume/CHANNEL/*/ChannelSize[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/channelSize.json',
    'testInProduction': true
  },
  'service.metrics.flume.sink_drain_success': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/flume/flume/SINK/*/EventDrainSuccessCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sinkDrainSuccessCount.json',
    'testInProduction': true
  },
  'service.metrics.flume.sink_connection_failed': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/flume/flume/SINK/*/ConnectionFailedCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sinkConnectionFailedCount.json',
    'testInProduction': true
  },
  'service.metrics.flume.gc': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmGcTime.json',
    'testInProduction': true
  },
  'service.metrics.flume.jvm_heap_used': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmMemHeapUsedM.json',
    'testInProduction': true
  },
  'service.metrics.flume.jvm_threads_runnable': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmThreadsRunnable.json',
    'testInProduction': true
  },
  'service.metrics.flume.cpu_user': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.flume.source_accepted': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_SERVER?fields=host_components/metrics/flume/flume/SOURCE/*/EventAcceptedCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sourceEventAccepted.json',
    'testInProduction': true
  },
  'service.metrics.hbase.cluster_requests': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_MASTER?fields=metrics/hbase/master/cluster_requests[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/cluster_requests.json',
    'testInProduction': true
  },
  'service.metrics.hbase.hlog_split_size': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_MASTER?fields=metrics/hbase/master/splitSize_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/hlog_split_size.json',
    'testInProduction': true
  },
  'service.metrics.hbase.hlog_split_time': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_MASTER?fields=metrics/hbase/master/splitTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/hlog_split_time.json',
    'testInProduction': true
  },
  'service.metrics.hbase.regionserver_queuesize': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/flushQueueSize[{fromSeconds},{toSeconds},{stepSeconds}],metrics/hbase/regionserver/compactionQueueSize[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/regionserver_queuesize.json',
    'testInProduction': true
  },
  'service.metrics.hbase.regionserver_regions': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/regions[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/regionserver_regions.json',
    'testInProduction': true
  },
  'service.metrics.hbase.regionserver_rw_requests': {
    'real': '/clusters/{clusterName}/services/HBASE/components/HBASE_REGIONSERVER?fields=metrics/hbase/regionserver/readRequestsCount[{fromSeconds},{toSeconds},{stepSeconds}],metrics/hbase/regionserver/writeRequestsCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hbase/regionserver_rw_requests.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.gc': {
    'real': '/clusters/{clusterName}/hosts/{jobTrackerNode}/host_components/JOBTRACKER?fields=metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/gc.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.jobs_status': {
    'real': '/clusters/{clusterName}/services/MAPREDUCE/components/JOBTRACKER?fields=metrics/mapred/jobtracker/jobs_completed[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/jobs_preparing[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/jobs_failed[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/jobs_submitted[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/jobs_failed[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/jobs_running[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/jobs_status.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.jobs_heap': {
    'real': '/clusters/{clusterName}/hosts/{jobTrackerNode}/host_components/JOBTRACKER?fields=metrics/jvm/memNonHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memNonHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/jvm_heap.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.jobs_threads': {
    'real': '/clusters/{clusterName}/hosts/{jobTrackerNode}/host_components/JOBTRACKER?fields=metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsBlocked[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsWaiting[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsTimedWaiting[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/jvm_threads.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.map_slots': {
    'real': '/clusters/{clusterName}/services/MAPREDUCE/components/JOBTRACKER?fields=metrics/mapred/jobtracker/occupied_map_slots[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/reserved_map_slots[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/map_slots.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.reduce_slots': {
    'real': '/clusters/{clusterName}/services/MAPREDUCE/components/JOBTRACKER?fields=metrics/mapred/jobtracker/occupied_reduce_slots[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/reserved_reduce_slots[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/reduce_slots.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.rpc': {
    'real': '/clusters/{clusterName}/hosts/{jobTrackerNode}/host_components/JOBTRACKER?fields=metrics/rpc/RpcQueueTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/rpc.json',
    'testInProduction': true
  },
  'service.metrics.mapreduce.tasks_running_waiting': {
    'real': '/clusters/{clusterName}/services/MAPREDUCE/components/JOBTRACKER?fields=metrics/mapred/jobtracker/running_maps[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/running_reduces[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/waiting_maps[{fromSeconds},{toSeconds},{stepSeconds}],metrics/mapred/jobtracker/waiting_reduces[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/mapreduce/tasks_running_waiting.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.block_status': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/PendingReplicationBlocks[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/UnderReplicatedBlocks[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/block_status.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.file_operations': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/dfs/namenode/FileInfoOps[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/namenode/CreateFileOps[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/file_operations.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.gc': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/gc.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.io': {
    'real': '/clusters/{clusterName}/services/HDFS/components/DATANODE?fields=metrics/dfs/datanode/bytes_written[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/datanode/bytes_read[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/io.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.jvm_heap': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/jvm/memNonHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memNonHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/jvm_heap.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.jvm_threads': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsBlocked[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsWaiting[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsTimedWaiting[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/jvm_threads.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.rpc': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/rpc/RpcQueueTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/rpc.json',
    'testInProduction': true
  },
  'service.metrics.hdfs.space_utilization': {
    'real': '/clusters/{clusterName}/hosts/{nameNodeName}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/CapacityRemainingGB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityUsedGB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/dfs/FSNamesystem/CapacityTotalGB[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/hdfs/space_utilization.json',
    'testInProduction': true
  },
  'service.start_stop': {
    'real': '/clusters/{clusterName}/services?params/run_smoke_test=true',
    'mock': '/data/mirroring/poll/poll_6.json',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        data: data.data
      };
    }
  },
  'service.metrics.yarn.gc': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/gc.json',
    'testInProduction': true
  },
  'service.metrics.yarn.jobs_threads': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsBlocked[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsWaiting[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/threadsTimedWaiting[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/jvm_threads.json',
    'testInProduction': true
  },
  'service.metrics.yarn.rpc': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/rpc/RpcQueueTime_avg_time[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/rpc.json',
    'testInProduction': true
  },
  'service.metrics.yarn.jobs_heap': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/jvm/memNonHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memNonHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}],metrics/jvm/memHeapCommittedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/yarn/jvm_heap.json',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.allocated': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AvailableMB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/PendingMB[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AllocatedMB[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.allocated.container': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AllocatedContainers[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/ReservedContainers[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/PendingContainers[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.node.manager.statuses': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/ClusterMetrics/NumActiveNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumDecommissionedNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumLostNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumRebootedNMs[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/ClusterMetrics/NumUnhealthyNMs[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.memory.resource': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=',
    'mock': '',
    'format': function (data, opt) {
      var field1 = 'metrics/yarn/Queue/{queueName}/AllocatedMB[{fromSeconds},{toSeconds},{stepSeconds}]';
      var field2 = 'metrics/yarn/Queue/{queueName}/AvailableMB[{fromSeconds},{toSeconds},{stepSeconds}]';
      if (opt.url != null && data.queueNames != null) {
        data.queueNames.forEach(function (q) {
          data.queueName = q;
          opt.url += (formatUrl(field1, data) + ",");
          opt.url += (formatUrl(field2, data) + ",");
        });
      }
    },
    'testInProduction': true
  },
  'service.metrics.yarn.queue.apps.states.current': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AppsPending[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsRunning[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.yarn.queue.apps.states.finished': {
    'real': '/clusters/{clusterName}/hosts/{resourceManager}/host_components/RESOURCEMANAGER?fields=metrics/yarn/Queue/root/AppsKilled[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsFailed[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsSubmitted[{fromSeconds},{toSeconds},{stepSeconds}],metrics/yarn/Queue/root/AppsCompleted[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.cpu': {
    'real': '/clusters/{clusterName}/?fields=metrics/cpu[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/cpu_1hr.json',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.load': {
    'real': '/clusters/{clusterName}/?fields=metrics/load[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/load_1hr.json',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.memory': {
    'real': '/clusters/{clusterName}/?fields=metrics/memory[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/memory_1hr.json',
    'testInProduction': true
  },
  'dashboard.cluster_metrics.network': {
    'real': '/clusters/{clusterName}/?fields=metrics/network[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/cluster_metrics/network_1hr.json',
    'testInProduction': true
  },
  'host.metrics.cpu': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_wio[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_nice[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_aidle[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_system[{fromSeconds},{toSeconds},{stepSeconds}],metrics/cpu/cpu_idle[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/cpu.json',
    'testInProduction': true
  },
  'host.metrics.disk': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/disk/disk_total[{fromSeconds},{toSeconds},{stepSeconds}],metrics/disk/disk_free[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/disk.json',
    'testInProduction': true
  },
  'host.metrics.load': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/load/load_fifteen[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/load_one[{fromSeconds},{toSeconds},{stepSeconds}],metrics/load/load_five[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/load.json',
    'testInProduction': true
  },
  'host.metrics.memory': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/memory/swap_free[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_shared[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_free[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_cached[{fromSeconds},{toSeconds},{stepSeconds}],metrics/memory/mem_buffers[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/memory.json',
    'testInProduction': true
  },
  'host.metrics.network': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/network/bytes_in[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/bytes_out[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/pkts_in[{fromSeconds},{toSeconds},{stepSeconds}],metrics/network/pkts_out[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/network.json',
    'testInProduction': true
  },
  'host.metrics.processes': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=metrics/process/proc_total[{fromSeconds},{toSeconds},{stepSeconds}],metrics/process/proc_run[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/hosts/metrics/processes.json',
    'testInProduction': true
  },
  'host.service_config_hosts_overrides': {
    'real': '/clusters/{clusterName}/configurations?{urlParams}',
    'mock': '',
    'format': function (data, opt) {
      return {
        async: false,
        timeout: 10000
      };
    }
  },
  'admin.security_status': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs',
    'mock': '',
    'format': function (data, opt) {
      return {
        timeout: 10000
      };
    }
  },
  'settings.get.user_pref': {
    'real': '/persist/{key}',
    'mock': '',
    'type': 'GET',
    'format': function (data, opt) {
      return {
      };
    }
  },
  'settings.post.user_pref': {
    'real': '/persist',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify(data.keyValuePair)
      }
    }
  },
  'cluster.load_cluster_name': {
    'real': '/clusters',
    'mock': '/data/clusters/info.json',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'cluster.state': {
    'type': 'POST',
    'real': '/persist/',
    'mock': '',
    'format': function (data, opt) {
      return {
        async: false,
        data: JSON.stringify(data.keyValuePair)
      };
    }
  },
  'cluster.update_upgrade_version': {
    'real': '/stacks2/HDP/versions?fields=stackServices/StackServices,Versions',
    'mock': '/data/wizard/stack/stacks.json',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'cluster.load_repositories': {
    'real': '/stacks2/{stackName}/versions/{stackVersion}/operatingSystems?fields=repositories/*',
    'mock': '',
    'type': 'GET',
    'format': function (data, opt) {
      return {
        data: data.data
      };
    }
  },
  'admin.high_availability.stop_all_services': {
    'real': '/clusters/{clusterName}/services',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Stop all services"
          },
          "Body": {
            "ServiceInfo": {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'admin.high_availability.start_all_services': {
    'real': '/clusters/{clusterName}/services',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Start all services"
          },
          "Body": {
            "ServiceInfo": {
              "state": "STARTED"
            }
          }
        })
      }
    }
  },
  'admin.high_availability.polling': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=tasks/*',
    'mock': '',
    'type': 'GET'
  },
  'admin.high_availability.getNnCheckPointStatus': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/NAMENODE',
    'mock': '',
    'type': 'GET'
  },
  'admin.high_availability.getJnCheckPointStatus': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/JOURNALNODE?fields=metrics',
    'mock': ''
  },
  'admin.high_availability.getHostComponent': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': ''
  },
  'admin.high_availability.create_component': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_name={hostName}',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "host_components": [
            {
              "HostRoles": {
                "component_name": data.componentName
              }
            }
          ]
        })
      }
    }
  },
  'admin.high_availability.create_journalnode': {
    'real': '/clusters/{clusterName}/services?ServiceInfo/service_name=HDFS',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "components": [
            {
              "ServiceComponentInfo": {
                "component_name": "JOURNALNODE"
              }
            }
          ]
        })
      }
    }
  },
  'admin.high_availability.create_zkfc': {
    'real': '/clusters/{clusterName}/services?ServiceInfo/service_name=HDFS',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        data: JSON.stringify({
          "components": [
            {
              "ServiceComponentInfo": {
                "component_name": "ZKFC"
              }
            }
          ]
        })
      }
    }
  },
  'admin.high_availability.install_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": "Install " + data.displayName
          },
          Body: {
            "HostRoles": {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'admin.high_availability.start_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": "Start " + data.displayName
          },
          Body: {
            "HostRoles": {
              "state": "STARTED"
            }
          }
        })
      }
    }
  },
  'admin.high_availability.maintenance_mode': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function () {
      return {
        data: JSON.stringify({
          "HostRoles": {
            "state": "MAINTENANCE"
          }
        })
      }
    }
  },
  'admin.high_availability.stop_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": "Stop " + data.displayName
          },
          Body: {
            "HostRoles": {
              "state": "INSTALLED"
            }
          }
        })
      }
    }
  },
  'admin.high_availability.load_configs': {
    'real': '/clusters/{clusterName}/configurations?(type=core-site&tag={coreSiteTag})|(type=hdfs-site&tag={hdfsSiteTag})',
    'mock': '',
    'type': 'GET'
  },
  'admin.high_availability.save_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        async: false,
        data: JSON.stringify({
          Clusters: {
            desired_config: {
              "type": data.siteName,
              "tag": 'version' + (new Date).getTime(),
              "properties": data.properties
            }
          }
        })
      }
    }
  },
  'admin.high_availability.load_hbase_configs': {
    'real': '/clusters/{clusterName}/configurations?type=hbase-site&tag={hbaseSiteTag}',
    'mock': '',
    'type': 'GET'
  },
  'admin.high_availability.delete_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'DELETE'
  },
  'admin.security.cluster_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data, opt) {
      return {
        timeout: 10000
      };
    }
  },
  'admin.delete_host': {
    'real': '/clusters/{clusterName}/hosts/{hostName}',
    'mock': '',
    'type': 'DELETE'
  },
  'admin.security.all_configurations': {
    'real': '/clusters/{clusterName}/configurations?{urlParams}',
    'mock': '',
    'format': function (data, opt) {
      return {
        timeout: 10000
      };
    }
  },
  'admin.security.apply_configurations': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        timeout: 10000,
        data: data.configData
      };
    }
  },
  'admin.security.apply_configuration': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        timeout: 5000,
        data: JSON.stringify(data.clusterData)
      };
    }
  },
  'admin.security.add.cluster_configs': {
    'real': '/clusters/{clusterName}' + '?fields=Clusters/desired_configs',
    'mock': '',
    'format': function (data, opt) {
      return {
        timeout: 10000
      };
    }
  },
  'admin.stack_upgrade.run_upgrade': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        data: data.data
      };
    }
  },
  'admin.stack_upgrade.stop_services': {
    'real': '/clusters/{clusterName}/services?ServiceInfo/state=STARTED',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        data: data.data
      };
    }
  },
  'admin.stack_upgrade.do_poll': {
    'real': '/clusters/{cluster}/requests/{requestId}?fields=tasks/*',
    'mock': '/data/wizard/{mock}'
  },
  'wizard.advanced_repositories.valid_url': {
    'real': '/stacks2/{stackName}/versions/{stackVersion}/operatingSystems/{osType}/repositories/{nameVersionCombo}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        async: true,
        data: JSON.stringify(data.data)
      }
    }
  },
  'wizard.install_services.add_host_controller.is_retry': {
    'real': '/clusters/{cluster}/host_components',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        data: data.data
      };
    }
  },
  'wizard.install_services.add_host_controller.not_is_retry': {
    'real': '/clusters/{cluster}/host_components',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        data: data.data
      };
    }
  },
  'wizard.install_services.installer_controller.is_retry': {
    'real': '/clusters/{cluster}/host_components?HostRoles/state=INSTALLED',
    'mock': '/data/wizard/deploy/2_hosts/poll_1.json',
    'type': 'PUT',
    'format': function (data, opt) {
      return {
        async: false,
        data: data.data
      };
    }
  },
  'wizard.install_services.installer_controller.not_is_retry': {
    'real': '/clusters/{cluster}/services?ServiceInfo/state=INIT',
    'mock': '/data/wizard/deploy/2_hosts/poll_1.json',
    'type': 'PUT',
    'format': function (data, opt) {
      return {
        async: false,
        data: data.data
      };
    }
  },
  'wizard.install_services.add_service_controller.get_failed_host_components': {
    'real': '/clusters/{clusterName}/host_components?fields=HostRoles/state,component/ServiceComponentInfo/service_name',
    'mock': '',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'wizard.service_components': {
    'real': '{stackUrl}/stackServices?fields=StackServices',
    'mock': '/data/wizard/stack/hdp/version/{stackVersion}.json',
    'format': function (data, opt) {
      return {
        timeout: 10000,
        async: false
      };
    }
  },
  'wizard.step9.installer.launch_start_services': {
    'real': '/clusters/{cluster}/services?ServiceInfo/state=INSTALLED&params/run_smoke_test=true&params/reconfigure_client=false',
    'mock': '/data/wizard/deploy/5_hosts/poll_6.json',
    'format': function (data, opt) {
      var data = {
        type: 'PUT',
        async: false,
        data: data.data
      };
      if (App.testMode) {
        data.type = 'GET';
      }
      return data;
    }
  },
  'wizard.step9.add_host.launch_start_services': {
    'real': '/clusters/{cluster}/host_components',
    'mock': '/data/wizard/deploy/5_hosts/poll_6.json',
    'format': function (data, opt) {
      return {
        type: 'PUT',
        async: false,
        data: data.data
      };
    }
  },
  'wizard.step8.delete_cluster': {
    'real': '/clusters/{name}',
    'mock': '',
    'format': function (data, opt) {
      return {
        type: 'DELETE',
        async: false
      };
    }
  },
  'wizard.step8.existing_cluster_names': {
    'real': '/clusters',
    'mock': '',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'wizard.step3.host_info': {
    'real': '/hosts?fields=Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info,Hosts/last_agent_env,Hosts/host_name,Hosts/os_type',
    'mock': '/data/wizard/bootstrap/two_hosts_information.json',
    'format': function (data, opt) {
      return {
        contentType: 'application/json'
      };
    }
  },
  'wizard.step3.rerun_checks': {
    'real': '/hosts?fields=Hosts/last_agent_env',
    'mock': '/data/wizard/bootstrap/two_hosts_information.json',
    'format': function (data, opt) {
      return {
        contentType: 'application/json'
      };
    }
  },
  'wizard.step3.bootstrap': {
    'real': '/bootstrap/{bootRequestId}',
    'mock': '/data/wizard/bootstrap/poll_{numPolls}.json'
  },
  'wizard.step3.is_hosts_registered': {
    'real': '/hosts',
    'mock': '/data/wizard/bootstrap/single_host_registration.json'
  },
  'wizard.stacks': {
    'real': '/stacks2',
    'mock': '/data/wizard/stack/stacks2.json',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'wizard.stacks_versions': {
    'real': '/stacks2/{stackName}/versions?fields=Versions,operatingSystems/repositories/Repositories',
    'mock': '/data/wizard/stack/{stackName}_versions.json',
    'format': function (data) {
      return {
        async: false
      };
    }
  },
  'wizard.launch_bootstrap': {
    'real': '/bootstrap',
    'mock': '/data/wizard/bootstrap/bootstrap.json',
    'type': 'POST',
    'format': function (data) {
      return {
        async: false,
        contentType: 'application/json',
        data: data.bootStrapData
      }
    }
  },
  'router.login': {
    'real': '/users/{loginName}',
    'mock': '/data/users/user_{usr}.json',
    'format': function (data, opt) {
      var statusCode = jQuery.extend({}, require('data/statusCodes'));
      statusCode['403'] = function () {
        console.log("Error code 403: Forbidden.");
      }
      return {
        statusCode: statusCode
      };
    }
  },
  'router.login2': {
    'real': '/clusters',
    'mock': '/data/clusters/info.json'
  },
  'router.logoff': {
    'real': '/logout'
  },
  'router.set_ambari_stacks': {
    'real': '/stacks',
    'mock': '/data/wizard/stack/stacks.json',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'router.authentication': {
    'real': '/clusters',
    'mock': '/data/clusters/info.json',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'ambari.service': {
    'real': '/services/AMBARI/components/AMBARI_SERVER',
    'mock': ''
  },
  'dashboard.get.user_pref': {
    'real': '/persist/{key}',
    'mock': '',
    'format': function (data, opt) {
      return {
        async: false
      };
    }
  },
  'dashboard.post.user_pref': {
    'real': '/persist',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        async: false,
        data: JSON.stringify(data.keyValuePair)
      }
    }
  },
  'config_groups.create': {
    'real': '/clusters/{clusterName}/config_groups',
    'mock': '',
    'format': function (data) {
      return {
        async: false,
        type: 'POST',
        data: JSON.stringify([{
          "ConfigGroup": {
            "group_name": data.group_name,
            "tag": data.service_id,
            "description": data.description
          }
        }])
      }
    }
  },
  'config_groups.update': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': '',
    'format': function (data) {
      return {
        async: true,
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  }
};
/**
 * Replace data-placeholders to its values
 *
 * @param {String} url
 * @param {Object} data
 * @return {String}
 */
var formatUrl = function (url, data) {
  var keys = url.match(/\{\w+\}/g);
  keys = (keys === null) ? [] : keys;
  if (keys) {
    keys.forEach(function (key) {
      var raw_key = key.substr(1, key.length - 2);
      var replace;
      if (!data[raw_key]) {
        replace = '';
      }
      else {
        replace = data[raw_key];
      }
      url = url.replace(new RegExp(key, 'g'), replace);
    });
  }
  return url;
};

/**
 * this = object from config
 * @return {Object}
 */
var formatRequest = function (data) {
  var opt = {
    type: this.type || 'GET',
    timeout: App.timeout,
    dataType: 'json',
    statusCode: require('data/statusCodes')
  };
  if (App.testMode) {
    opt.url = formatUrl(this.mock ? this.mock : '', data);
    opt.type = 'GET';
  }
  else {
    opt.url = App.apiPrefix + formatUrl(this.real, data);
  }

  if (this.format) {
    jQuery.extend(opt, this.format(data, opt));
  }
  return opt;
};

/**
 * Wrapper for all ajax requests
 *
 * @type {Object}
 */
App.ajax = {
  /**
   * Send ajax request
   *
   * @param {Object} config
   * @return Object jquery ajax object
   *
   * config fields:
   *  name - url-key in the urls-object *required*
   *  sender - object that send request (need for proper callback initialization) *required*
   *  data - object with data for url-format
   *  beforeSend - method-name for ajax beforeSend response callback
   *  success - method-name for ajax success response callback
   *  error - method-name for ajax error response callback
   *  callback - callback from <code>App.updater.run</code> library
   */
  send: function (config) {

    console.warn('============== ajax ==============', config.name, config.data);

    if (!config.sender) {
      console.warn('Ajax sender should be defined!');
      return null;
    }

    // default parameters
    var params = {
      clusterName: App.get('clusterName')
    };

    // extend default parameters with provided
    if (config.data) {
      jQuery.extend(params, config.data);
    }

    var opt = {};
    opt = formatRequest.call(urls[config.name], params);

    opt.context = this;

    // object sender should be provided for processing beforeSend, success and error responses
    opt.beforeSend = function (xhr) {
      if (config.beforeSend) {
        config.sender[config.beforeSend](opt, xhr, params);
      }
    };
    opt.success = function (data, textStatus, xhr) {
      console.log("TRACE: The url is: " + opt.url);
      if (config.success) {
        config.sender[config.success](data, opt, params);
      }
    };
    opt.error = function (request, ajaxOptions, error) {
      if (config.error) {
        config.sender[config.error](request, ajaxOptions, error, opt);
      } else {
        this.defaultErrorHandler(request, opt.url, opt.type);
      }
    };
    opt.complete = function () {
      if (config.callback) {
        config.callback();
      }
    };
    if ($.mocho) {
      opt.url = 'http://' + $.hostName + opt.url;
    }
    return $.ajax(opt);
  },

  // A single instance of App.ModalPopup view
  modalPopup: null,
  /**
   * defaultErrorHandler function is referred from App.ajax.send function and App.HttpClient.defaultErrorHandler function
   * @jqXHR {jqXHR Object}
   * @url {string}
   * @method {String} Http method
   * @showStatus {number} HTTP response code which should be shown. Default is 500.
   */
  defaultErrorHandler: function (jqXHR, url, method, showStatus) {
    method = method || 'GET';
    var self = this;
    var api = " received on " + method + " method for API: " + url;
    var showMessage = true;
    try {
      var json = $.parseJSON(jqXHR.responseText);
      var message = json.message;
    } catch (err) {
    }
    if (showStatus === null) {
      showStatus = 500;
    }
    if (message === undefined) {
      showMessage = false;
    }
    var statusCode = jqXHR.status + " status code";
    if (jqXHR.status === showStatus && !this.modalPopup) {
      this.modalPopup = App.ModalPopup.show({
        header: jqXHR.statusText,
        secondary: false,
        onPrimary: function () {
          this.hide();
          self.modalPopup = null;
        },
        bodyClass: Ember.View.extend({
          classNames: ['api-error'],
          templateName: require('templates/utils/ajax'),
          api: api,
          statusCode: statusCode,
          message: message,
          showMessage: showMessage
        })
      });
    }
  }
};
