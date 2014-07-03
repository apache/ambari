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
 *  format - function for processing ajax params after default formatRequest. May be called with one or two parameters (data, opt). Return ajax-params object
 *  testInProduction - can this request be executed on production tests (used only in tests)
 *
 * @type {Object}
 */
var urls = {

  'common.services.update' : {
    'real': '/clusters/{clusterName}/services?{urlParams}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              "level": "CLUSTER",
              "cluster_name" : data.clusterName
            }
          },
          Body: {
            ServiceInfo: data.ServiceInfo
          }
        })
      };
    }
  },

  'common.service.update' : {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              "level": "SERVICE",
              "cluster_name" : data.clusterName,
              "service_name" : data.serviceName
            }
          },
          Body: {
            ServiceInfo: data.ServiceInfo
          }
        })
      };
    }
  },

  'common.host_components.update': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components?{urlParams}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              level: "HOST",
              cluster_name: data.clusterName,
              host_names: data.hostName
            },
            query: data.query
          },
          Body: {
            "HostRoles": data.HostRoles
          }
        })
      }
    }
  },

  'common.host_component.update': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.context,
            "operation_level": {
              level: "HOST_COMPONENT",
              cluster_name: data.clusterName,
              host_name: data.hostName,
              service_name: data.serviceName || null
            }
          },
          Body: {
            "HostRoles": data.HostRoles
          }
        })
      }
    }
  },

  'alerts.get_by_service': {
    'real': '/clusters/{clusterName}/services/{serviceName}?fields=alerts',
    'mock': '/data/alerts/HDP2/service_alerts.json'
  },
  'alerts.get_by_host': {
    'real': '/clusters/{clusterName}/hosts/{hostName}?fields=alerts',
    'mock': '/data/alerts/HDP2/host_alerts.json'
  },
  'background_operations.get_most_recent': {
    'real': '/clusters/{clusterName}/requests?to=end&page_size=10&fields=Requests',
    'mock': '/data/background_operations/list_on_start.json',
    'testInProduction': true
  },
  'background_operations.get_by_request': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=*,tasks/Tasks/command,tasks/Tasks/command_detail,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/exit_code,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true',
    'mock': '/data/background_operations/task_by_request{requestId}.json',
    'testInProduction': true
  },
  'background_operations.get_by_task': {
    'real': '/clusters/{clusterName}/requests/{requestId}/tasks/{taskId}',
    'mock': '/data/background_operations/list_on_start.json',
    'testInProduction': true
  },
  'service.item.smoke': {
    'real': '/clusters/{clusterName}/requests',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function (data) {
      return {
        'type': 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "context": data.displayName + " Service Check",
            "command" : data.actionName
          },
          "Requests/resource_filters": [{"service_name" : data.serviceName}]
        })
      };
    }
  },
  'service.item.passive': {
    'real': '/clusters/{clusterName}/services/{serviceName}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            "context": data.requestInfo
          },
          Body: {
            ServiceInfo: {
              maintenance_state: data.passive_state
            }
          }
        })
      };
    }
  },
  'service.load_config_groups': {
    'real': '/clusters/{clusterName}/config_groups?ConfigGroup/tag={serviceName}&fields=*',
    'mock': '/data/configurations/config_group.json'
  },
  'service.flume.agent.command': {
    'real': '/clusters/{clusterName}/hosts/{host}/host_components/FLUME_HANDLER',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          "RequestInfo": {
            "context": data.context,
            "flume_handler": data.agentName
          },
          "Body": {
            "HostRoles": {
              "state": data.state
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
              "maintenance_state": "ON"
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

  'host_component.passive': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function(data) {
      return {
        data: JSON.stringify({
          RequestInfo: {
            "context": data.requestInfo
          },
          Body: {
            HostRoles: {
              maintenance_state: data.passive_state
            }
          }
        })
      };
    }
  },

  'config.cluster_configuration.put': {
    'real': '/clusters/{cluster}',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'config.advanced': {
    'real': '{stackVersionUrl}/services/{serviceName}/configurations?fields=*',
    'mock': '/data/wizard/stack/hdp/version{stackVersion}/{serviceName}.json'
  },
  'config.tags': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs',
    'mock': '/data/clusters/cluster.json'
  },
  'config.tags_and_groups': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs,config_groups/*{urlParams}',
    'mock': '/data/clusters/tags_and_groups.json'
  },
  'config.ambari.database.info': {
    'real': '/services/AMBARI/components/AMBARI_SERVER?fields=hostComponents/RootServiceHostComponents/properties/server.jdbc.database,hostComponents/RootServiceHostComponents/properties/server.jdbc.url',
    'mock': '',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'config_groups.all_fields': {
    'real': '/clusters/{clusterName}/config_groups?fields=*',
    'mock': ''
  },
  'config_groups.get_config_group_by_id': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': ''
  },
  'config_groups.update_config_group': {
    'real': '/clusters/{clusterName}/config_groups/{id}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
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
    'format': function() {
      return {
        async: false
      };
    }
  },
  'config.host_overrides': {
    'real': '/clusters/{clusterName}/configurations?{params}',
    'mock': '/data/configurations/host_level_overrides_configs.json?{params}'
  },

  'host.host_component.delete': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'format': function() {
      return {
        type: 'DELETE'
      }
    }
  },

  'host.host_components.delete': {
    'real': '/clusters/{clusterName}/hosts/{hostName}',
    'mock': '',
    'format': function() {
      return {
        type: 'DELETE'
      }
    }
  },

  'host.host_component.add_new_component': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_name={hostName}',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function(data) {
      return {
        type: 'POST',
        data: data.data
      }
    }
  },

  'host.host_component.install_new_component': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/host_name={hostName}\&HostRoles/component_name={componentName}\&HostRoles/state=INIT',
    'mock': '/data/wizard/deploy/poll_1.json',
    'format': function(data) {
      return {
        type: 'PUT',
        data: data.data
      }
    }
  },

  'host.host_component.slave_desired_admin_state': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}/?fields=HostRoles/desired_admin_state',
    'mock': ''
  },
  'host.host_component.decommission_status': {
    'real': '/clusters/{clusterName}/services/{serviceName}/components/{componentName}/?fields=ServiceComponentInfo,host_components/HostRoles/state',
    'mock': ''
  },
  'host.host_component.decommission_status_datanode': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}?fields=metrics/dfs/namenode',
    'mock': ''
  },
  'host.host_component.decommission_slave': {
    'real' : '/clusters/{clusterName}/requests',
    'mock' : '',
    'format' : function(data) {
      return {
        type : 'POST',
        data : JSON.stringify({
          RequestInfo: {
            'context': data.context,
            'command': data.command,
            'parameters': {
              'slave_type': data.slaveType,
              'excluded_hosts': data.hostName
            }
          },
          "Requests/resource_filters": [{"service_name" : data.serviceName, "component_name" : data.componentName}]
        })
      }
    }
  },
  'host.host_component.recommission_and_restart': {
    'real': '/clusters/{clusterName}/request_schedules',
    'mock': '',
    'format' : function(data) {
      return {
        type : 'POST',
        data : JSON.stringify([ {
          "RequestSchedule" : {
            "batch" : [ {
              "requests" : data.batches
            }, {
              "batch_settings" : {
                "batch_separation_in_seconds" : data.intervalTimeSeconds,
                "task_failure_tolerance" : data.tolerateSize
              }
            } ]
          }
        } ])
      }
    }
  },
  'host.host_component.recommission_slave' : {
    'real' : '/clusters/{clusterName}/requests',
    'mock' : '',
    'format' : function(data) {
      return {
        type : 'POST',
        data : JSON.stringify({
          RequestInfo: {
            context: data.context,
            command: data.command,
            parameters: {
              slave_type: data.slaveType,
              included_hosts: data.hostName
            }
          },
          "Requests/resource_filters": [{"service_name" : data.serviceName, "component_name" : data.componentName}]
        })
      }
    }
  },

  'host.host_component.refresh_configs': {
    'real':'/clusters/{clusterName}/requests',
    'mock':'',
    'format': function(data) {
      return {
        type : 'POST',
        data : JSON.stringify({
          "RequestInfo": {
            "command": "CONFIGURE",
            "context": data.context
          },
          "Requests/resource_filters": data.resource_filters
        })
      }
    }
  },

  'host.delete': {
    'real': '/clusters/{clusterName}/hosts/{hostName}',
    'mock': '',
    'type': 'DELETE'
  },
  'hosts.metrics': {
    'real': '/clusters/{clusterName}/hosts?fields={metricName}',
    'mock': '/data/cluster_metrics/cpu_1hr.json'
  },
  'hosts.metrics.host_component': {
    'real': '/clusters/{clusterName}/services/{serviceName}/components/{componentName}?fields=host_components/{metricName}',
    'mock': '/data/cluster_metrics/cpu_1hr.json'
  },
  'service.service_component': {
    'real': '/clusters/{clusterName}/services/{serviceName}/components/{componentName}',
    'mock': '',
    'format': function(data) {
      return {
        async: data.async
      };
    }
  },
  'service.metrics.flume.channel_fill_percent': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/CHANNEL/*/ChannelFillPercentage[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/channelFillPct.json',
    'testInProduction': true
  },
  'service.metrics.flume.channel_size': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/CHANNEL/*/ChannelSize[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/channelSize.json',
    'testInProduction': true
  },
  'service.metrics.flume.sink_drain_success': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/SINK/*/EventDrainSuccessCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sinkDrainSuccessCount.json',
    'testInProduction': true
  },
  'service.metrics.flume.sink_connection_failed': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/SINK/*/ConnectionFailedCount[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/sinkConnectionFailedCount.json',
    'testInProduction': true
  },
  'service.metrics.flume.gc': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/jvm/gcTimeMillis[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmGcTime.json',
    'testInProduction': true
  },
  'service.metrics.flume.jvm_heap_used': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/jvm/memHeapUsedM[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmMemHeapUsedM.json',
    'testInProduction': true
  },
  'service.metrics.flume.jvm_threads_runnable': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/jvm/threadsRunnable[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '/data/services/metrics/flume/jvmThreadsRunnable.json',
    'testInProduction': true
  },
  'service.metrics.flume.cpu_user': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/cpu/cpu_user[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': '',
    'testInProduction': true
  },
  'service.metrics.flume.source_accepted': {
    'real': '/clusters/{clusterName}/services/FLUME/components/FLUME_HANDLER?fields=host_components/metrics/flume/flume/SOURCE/*/EventAcceptedCount[{fromSeconds},{toSeconds},{stepSeconds}]',
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
      if (opt.url != null && data.queueNames != null && data.queueNames.length > 0) {
        data.queueNames.forEach(function (q) {
          data.queueName = q;
          opt.url += (formatUrl(field1, data) + ",");
          opt.url += (formatUrl(field2, data) + ",");
        });
      } else {
        opt.url += (formatUrl(field1, data) + ",");
        opt.url += (formatUrl(field2, data) + ",");
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
  'service.metrics.storm.nimbus': {
    'real': '/clusters/{clusterName}/services/STORM/components/NIMBUS?fields={metricsTemplate}',
    'mock': ''
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
  'admin.security_status': {
    'real': '/clusters/{clusterName}?fields=Clusters/desired_configs',
    'mock': '',
    'format': function() {
      return {
        timeout: 10000
      };
    }
  },
  'settings.get.user_pref': {
    'real': '/persist/{key}',
    'mock': '/data/user_settings/{key}.json',
    'format': function(data) {
      return {
        async: data.async
      };
    }
  },
  'settings.post.user_pref': {
    'real': '/persist',
    'mock': '',
    'type': 'POST',
    'format': function (data) {
      return {
        async: data.async,
        data: JSON.stringify(data.keyValuePair)
      }
    }
  },
  'cluster.load_cluster_name': {
    'real': '/clusters',
    'mock': '/data/clusters/info.json',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'cluster.update_upgrade_version': {
    'real': '/stacks/HDP/versions?fields=stackServices/StackServices,Versions',
    'mock': '/data/wizard/stack/stacks.json',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'cluster.load_repositories': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/operating_systems?fields=repositories/*',
    'mock': '',
    'type': 'GET',
    'format': function (data) {
      return {
        data: data.data
      };
    }
  },
  'admin.high_availability.polling': {
    'real': '/clusters/{clusterName}/requests/{requestId}?fields=tasks/*,Requests/*',
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
  'admin.high_availability.maintenance_mode': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'PUT',
    'format': function () {
      return {
        data: JSON.stringify({
          "HostRoles": {
            "state": "DISABLED"
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
  'admin.delete_component': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}',
    'mock': '',
    'type': 'DELETE'
  },
  'admin.security.cluster_configs': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function() {
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
  'admin.get.all_configurations': {
    'real': '/clusters/{clusterName}/configurations?{urlParams}',
    'mock': '',
    'format': function() {
      return {
        timeout: 10000
      };
    }
  },
  'admin.security.apply_configurations': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data) {
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
    'format': function(data) {
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
    'format': function() {
      return {
        timeout: 10000
      };
    }
  },
  'admin.stack_upgrade.run_upgrade': {
    'real': '/clusters/{clusterName}',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: data.data
      };
    }
  },
  'admin.user.create': {
    'real': '/users/{user}',
    'mock': '/data/users/users.json',
    'format': function(data) {
      return {
        type: 'POST',
        data: JSON.stringify(data.data)
      }
    }
  },

  'admin.user.delete': {
    'real': '/users/{user}',
    'mock': '/data/users/users.json',
    'format': function() {
      return {
        type: 'DELETE'
      }
    }
  },

  'admin.user.edit': {
    'real': '/users/{user}',
    'mock':'/data/users/users.json',
    'format': function(data) {
      return {
        type: 'PUT',
        data: data.data
      }
    }
  },

  'admin.stack_upgrade.do_poll': {
    'real': '/clusters/{cluster}/requests/{requestId}?fields=tasks/*',
    'mock': '/data/wizard/{mock}'
  },
  'wizard.advanced_repositories.valid_url': {
    'real': '/stacks/{stackName}/versions/{stackVersion}/operating_systems/{osType}/repositories/{repoId}',
    'mock': '',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: JSON.stringify(data.data)
      }
    }
  },
  'wizard.install_services.add_host_controller.is_retry': {
    'real': '/clusters/{cluster}/host_components',
    'mock': '',
    'format': function (data) {
      return {
        type: 'PUT',
        data: data.data
      };
    }
  },
  'wizard.install_services.add_host_controller.not_is_retry': {
    'real': '/clusters/{cluster}/host_components',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        data: data.data
      };
    }
  },
  'wizard.install_services.installer_controller.is_retry': {
    'real': '/clusters/{cluster}/host_components?HostRoles/state=INSTALLED',
    'mock': '/data/wizard/deploy/2_hosts/poll_1.json',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: data.data
      };
    }
  },
  'wizard.install_services.installer_controller.not_is_retry': {
    'real': '/clusters/{cluster}/services?ServiceInfo/state=INIT',
    'mock': '/data/wizard/deploy/2_hosts/poll_1.json',
    'type': 'PUT',
    'format': function (data) {
      return {
        data: data.data
      };
    }
  },
  'wizard.install_services.add_service_controller.get_failed_host_components': {
    'real': '/clusters/{clusterName}/host_components?fields=HostRoles/state,component/ServiceComponentInfo/service_name',
    'mock': '',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'wizard.service_components': {
    'real': '{stackUrl}/services?fields=StackServices/comments,StackServices/service_version,serviceComponents/*',
    'mock': '/data/stacks/HDP-2.1/service_components.json',
    'format': function(data) {
      return {
        timeout: 10000,
        async: !!data.async
      };
    }
  },
  'wizard.step9.installer.get_host_status': {
    'real': '/clusters/{cluster}/hosts?fields=Hosts/host_state,host_components/HostRoles/state',
    'mock': '/data/wizard/deploy/5_hosts/get_host_state.json',
    'format': function () {
      return {
        async: false
      };
    }
  },
  'wizard.step9.installer.launch_start_services': {
    'real': '/clusters/{cluster}/services?ServiceInfo/state=INSTALLED&params/run_smoke_test=true&params/reconfigure_client=false',
    'mock': '/data/wizard/deploy/5_hosts/poll_6.json',
    'format': function (data) {
      var d = {
        type: 'PUT',
        data: data.data
      };
      if (App.testMode) {
        d.type = 'GET';
      }
      return d;
    }
  },
  'wizard.step9.add_service.launch_start_services': {
    'real': '/clusters/{cluster}/services?ServiceInfo/state=INSTALLED&ServiceInfo/service_name.in({servicesList})&params/reconfigure_client=false',
    'mock': '/data/wizard/deploy/5_hosts/poll_6.json',
    'format': function (data) {
      var d = {
        type: 'PUT',
        data: data.data
      };
      if (App.testMode) {
        d.type = 'GET';
      }
      return d;
    }
  },
  'wizard.step9.add_host.launch_start_services': {
    'real': '/clusters/{cluster}/host_components',
    'mock': '/data/wizard/deploy/5_hosts/poll_6.json',
    'format': function (data) {
      return {
        type: 'PUT',
        data: data.data
      };
    }
  },

  'wizard.step9.load_log': {
    'real': '/clusters/{cluster}/requests/{requestId}?fields=tasks/Tasks/command,tasks/Tasks/exit_code,tasks/Tasks/start_time,tasks/Tasks/end_time,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true',
    'mock': '/data/wizard/deploy/5_hosts/poll_{numPolls}.json',
    'format': function () {
      return {
        type: 'GET',
        dataType: 'text'
      };
    }
  },

  'wizard.step8.delete_cluster': {
    'real': '/clusters/{name}',
    'mock': '',
    'format': function() {
      return {
        type: 'DELETE'
      };
    }
  },
  'wizard.step8.existing_cluster_names': {
    'real': '/clusters',
    'mock': '',
    'format': function() {
      return {
        async: false
      };
    }
  },

  'wizard.step8.create_cluster': {
    'real':'/clusters/{cluster}',
    'mock':'',
    'format': function(data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.create_selected_services': {
    'real':'/clusters/{cluster}/services',
    'mock':'',
    'format': function(data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.create_components': {
    'real':'/clusters/{cluster}/services?ServiceInfo/service_name={serviceName}',
    'mock':'',
    'format': function(data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.register_host_to_cluster': {
    'real':'/clusters/{cluster}/hosts',
    'mock':'',
    'format': function(data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.register_host_to_component': {
    'real':'/clusters/{cluster}/hosts',
    'mock':'',
    'format': function(data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.apply_configuration_to_cluster': {
    'real':'/clusters/{cluster}',
    'mock':'',
    'format': function(data) {
      return {
        type: 'PUT',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.apply_configuration_groups': {
    'real':'/clusters/{cluster}/config_groups',
    'mock':'',
    'format': function(data) {
      return {
        type: 'POST',
        dataType: 'text',
        data: data.data
      }
    }
  },

  'wizard.step8.set_local_repos': {
    'real':'{stackVersionURL}/operating_systems/{osType}/repositories/{repoId}',
    'mock':'',
    'format': function(data) {
      return {
        type: 'PUT',
        dataType: 'text',
        data: data.data
      }
    }
  },
  'wizard.step3.jdk_check': {
    'real': '/requests',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          "RequestInfo": {
            "context": "Check hosts",
            "action": "check_host",
            "parameters" : {
              "threshold" : "60",
              "java_home" : data.java_home,
              "jdk_location": data.jdk_location,
              "check_execute_list" : "java_home_check"
            }
          },
          "Requests/resource_filters": [{
            "hosts": data.host_names
          }]
        })
      }
    }
  },
  'wizard.step3.jdk_check.get_results': {
    'real': '/requests/{requestIndex}?fields=*,tasks/Tasks/host_name,tasks/Tasks/status,tasks/Tasks/structured_out',
    'mock': '/data/requests/host_check/jdk_check_results.json'
  },
  'wizard.step3.host_info': {
    'real': '/hosts?fields=Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info,Hosts/last_agent_env,Hosts/host_name,Hosts/os_type,Hosts/os_arch,Hosts/ip',
    'mock': '/data/wizard/bootstrap/two_hosts_information.json',
    'format': function() {
      return {
        contentType: 'application/json'
      };
    }
  },

  'preinstalled.checks': {
    'real':'/requests',
    'mock':'',
    'format': function(data) {
      return {
        type : 'POST',
        data : JSON.stringify({
          "RequestInfo": data.RequestInfo,
          "Requests/resource_filters": [data.resource_filters]
        })
      }
    }
  },

  'preinstalled.checks.tasks': {
    'real':'/requests/{requestId}?fields=tasks/Tasks',
    'mock':'/data/requests/host_check/1.json'
  },

  'wizard.step3.rerun_checks': {
    'real': '/hosts?fields=Hosts/last_agent_env',
    'mock': '/data/wizard/bootstrap/two_hosts_information.json',
    'format': function() {
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
    'real': '/stacks',
    'mock': '/data/wizard/stack/stacks2.json',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'wizard.stacks_versions': {
    'real': '/stacks/{stackName}/versions?fields=Versions,operatingSystems/repositories/Repositories',
    'mock': '/data/wizard/stack/{stackName}_versions.json',
    'format': function() {
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
        contentType: 'application/json',
        data: data.bootStrapData,
        popup: data.popup
      }
    }
  },
  'router.login': {
    'real': '/users/{loginName}',
    'mock': '/data/users/user_{usr}.json',
    'format': function (data) {
      var statusCode = jQuery.extend({}, require('data/statusCodes'));
      statusCode['403'] = function () {
        console.log("Error code 403: Forbidden.");
      };
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
    'real': '/logout',
    'mock': ''
  },
  'router.set_ambari_stacks': {
    'real': '/stacks',
    'mock': '/data/wizard/stack/stacks.json',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'router.authentication': {
    'real': '/clusters',
    'mock': '/data/clusters/info.json',
    'format': function() {
      return {
        async: false
      };
    }
  },
  'ambari.service.load_jdk_name': {
    'real': '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/properties/jdk.name,RootServiceComponents/properties/java.home,RootServiceComponents/properties/jdk_location',
    'mock': '/data/requests/host_check/jdk_name.json'
  },
  'ambari.service.load_server_version': {
    'real': '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/component_version',
    'mock': ''
  },
  'ambari.service': {
    'real': '/services/AMBARI/components/AMBARI_SERVER',
    'mock': '/data/services/ambari_server.json'
  },
  'ambari.service.load_server_clock': {
    'real': '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/server_clock',
    'mock': ''
  },

  'config_groups.create': {
    'real': '/clusters/{clusterName}/config_groups',
    'mock': '',
    'format': function (data) {
      return {
        type: 'POST',
        data: JSON.stringify([{
          "ConfigGroup": {
            "group_name": data.group_name,
            "tag": data.service_id,
            "description": data.description,
            "desired_configs": data.desired_configs,
            "hosts": data.hosts
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
        type: 'PUT',
        data: JSON.stringify(data.data)
      }
    }
  },
  'rolling_restart.post': {
    'real': '/clusters/{clusterName}/request_schedules',
    'mock': '',
    'format' : function(data) {
      return {
        type : 'POST',
        data : JSON.stringify([ {
          "RequestSchedule" : {
            "batch" : [ {
              "requests" : data.batches
            }, {
              "batch_settings" : {
                "batch_separation_in_seconds" : data.intervalTimeSeconds,
                "task_failure_tolerance" : data.tolerateSize
              }
            } ]
          }
        } ])
      }
    }
  },
  'request_schedule.delete': {
    'real': '/clusters/{clusterName}/request_schedules/{request_schedule_id}',
    'mock': '',
    'format' : function() {
      return {
        type : 'DELETE'
      }
    }
  },
  'request_schedule.get': {
    'real': '/clusters/{clusterName}/request_schedules/{request_schedule_id}',
    'mock': ''
  },
  'restart.hostComponents': {
    'real':'/clusters/{clusterName}/requests',
    'mock':'',
    'format': function(data) {
      return {
        type : 'POST',
        data : JSON.stringify({
          "RequestInfo": {
            "command": "RESTART",
            "context": data.context,
            "operation_level": data.operation_level
          },
          "Requests/resource_filters": data.resource_filters
        })
      }
    }
  },

  'mirroring.get_all_entities': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/list/{type}?fields=status&user.name=ambari-qa',
    'mock': '/data/mirroring/{type}s.xml',
    'apiPrefix': '',
    'format': function () {
      return {
        dataType: 'xml'
      }
    }
  },

  'mirroring.get_definition': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/definition/{type}/{name}?user.name=ambari-qa',
    'mock': '/data/mirroring/{name}_definition.xml',
    'apiPrefix': '',
    'format': function () {
      return {
        cache: true,
        dataType: 'xml'
      }
    }
  },

  'mirroring.dataset.get_all_instances': {
    'real': '/proxy?url=http://{falconServer}:15000/api/instance/status/feed/{dataset}?start={start}&end={end}&user.name=ambari-qa',
    'mock': '/data/mirroring/{dataset}_instances.json',
    'apiPrefix': ''
  },


  'mirroring.create_new_dataset': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/submitAndSchedule/feed?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST',
    'format': function (data) {
      return {
        contentType: 'text/xml',
        dataType: 'xml',
        data: data.entity,
        headers: {
          'AmbariProxy-Content-Type': 'text/xml'
        }
      }
    }
  },

  'mirroring.submit_entity': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/submit/{type}?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST',
    'format': function (data) {
      return {
        contentType: 'text/xml',
        dataType: 'xml',
        data: data.entity,
        headers: {
          'AmbariProxy-Content-Type': 'text/xml'
        }
      }
    }
  },

  'mirroring.update_entity': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/update/{type}/{name}?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST',
    'format': function (data) {
      return {
        contentType: 'text/xml',
        dataType: 'xml',
        data: data.entity,
        headers: {
          'AmbariProxy-Content-Type': 'text/xml'
        }
      }
    }
  },

  'mirroring.delete_entity': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/delete/{type}/{name}?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'DELETE',
    'format': function () {
      return {
        dataType: 'xml'
      }
    }
  },

  'mirroring.suspend_entity': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/suspend/{type}/{name}?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST',
    'format': function (data) {
      return {
        dataType: 'xml',
        data: data.entity
      }
    }
  },

  'mirroring.resume_entity': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/resume/{type}/{name}?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST',
    'format': function () {
      return {
        dataType: 'xml'
      }
    }
  },

  'mirroring.schedule_entity': {
    'real': '/proxy?url=http://{falconServer}:15000/api/entities/schedule/{type}/{name}?user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST',
    'format': function () {
      return {
        dataType: 'xml'
      }
    }
  },

  'mirroring.suspend_instance': {
    'real': '/proxy?url=http://{falconServer}:15000/api/instance/suspend/feed/{feed}?start={name}&user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST'
  },

  'mirroring.resume_instance': {
    'real': '/proxy?url=http://{falconServer}:15000/api/instance/resume/feed/{feed}?start={name}&user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST'
  },

  'mirroring.kill_instance': {
    'real': '/proxy?url=http://{falconServer}:15000/api/instance/kill/feed/{feed}?start={name}&user.name=ambari-qa',
    'mock': '/data/mirroring/succeeded.json',
    'apiPrefix': '',
    'type': 'POST'
  },

  'bulk_request.host_components': {
    'real': '/clusters/{clusterName}/host_components',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: 'HostRoles/component_name=' + data.componentName + '&HostRoles/host_name.in(' + data.hostNames + ')&HostRoles/maintenance_state=OFF'
          },
          Body: {
            HostRoles: {
              state: data.state
            }
          }
        })
      }
    }
  },

  'bulk_request.hosts.all_components': {
    'real': '/clusters/{clusterName}/host_components',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: data.query,
            operation_level: {
              level: "HOST",
              cluster_name: data.clusterName,
              host_name: data.hostName
            }
          },
          Body: {
            HostRoles: {
              state: data.state
            }
          }
        })
      }
    }
  },

  'bulk_request.decommission': {
    'real' : '/clusters/{clusterName}/requests',
    'mock' : '',
    'format': function(data) {
      return {
        type: 'POST',
        data: JSON.stringify({
          'RequestInfo': {
            'context': data.context,
            'command': 'DECOMMISSION',
            'parameters': data.parameters
          },
          "Requests/resource_filters": [{"service_name" : data.serviceName, "component_name" : data.componentName}]
        })
      }
    }
  },

  'bulk_request.hosts.passive_state': {
    'real': '/clusters/{clusterName}/hosts',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: 'Hosts/host_name.in(' + data.hostNames + ')'
          },
          Body: {
            Hosts: {
              maintenance_state: data.passive_state
            }
          }
        })
      }
    }
  },

  'bulk_request.hosts.all_components.passive_state': {
    'real': '/clusters/{clusterName}/host_components',
    'mock': '',
    'format': function(data) {
      return {
        type: 'PUT',
        data: JSON.stringify({
          RequestInfo: {
            context: data.requestInfo,
            query: data.query
          },
          Body: {
            HostRoles: {
              maintenance_state: data.passive_state
            }
          }
        })
      }
    }
  },

  'jobs.lastID': {
    'real': '/proxy?url=http://{historyServerHostName}:{ahsWebPort}/ws/v1/timeline/HIVE_QUERY_ID?limit=1&secondaryFilter=tez:true',
    'mock': 'data/jobs/hive-queries.json',
    'apiPrefix': ''
  },

  'jobs.tezDag.NametoID': {
    'real': '/proxy?url=http://{historyServerHostName}:{ahsWebPort}/ws/v1/timeline/TEZ_DAG_ID?primaryFilter=dagName:{tezDagName}',
    'mock': '/data/jobs/tezDag-name-to-id.json',
    'apiPrefix': ''
  },
  'jobs.tezDag.tezDagId': {
    'real': '/proxy?url=http://{historyServerHostName}:{ahsWebPort}/ws/v1/timeline/TEZ_DAG_ID/{tezDagId}?fields=relatedentities,otherinfo',
    'mock': '/data/jobs/tezDag.json',
    'apiPrefix': ''
  },
  'jobs.tezDag.tezDagVertexId': {
    'real': '/proxy?url=http://{historyServerHostName}:{ahsWebPort}/ws/v1/timeline/TEZ_VERTEX_ID/{tezDagVertexId}?fields=otherinfo',
    'mock': '/data/jobs/tezDagVertex.json',
    'apiPrefix': ''
  },
  'views.info': {
    'real': '/views',
    'mock':''
  },
  /**
   * Get all instances of all views across versions
   */
  'views.instances': {
    'real': '/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label',
    'mock':''
  },
  'host.host_component.flume.metrics': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/FLUME_HANDLER?fields=metrics/flume/flume/{flumeComponent}/*',
    'mock': '',
    'format': function(data) {
      return {
        async: data.async
      }
    }
  },
  'host.host_component.flume.metrics.timeseries': {
    'real': '/clusters/{clusterName}/hosts/{hostName}/host_components/FLUME_HANDLER?fields=metrics/flume/flume/{flumeComponent}/*/{flumeComponentMetric}[{fromSeconds},{toSeconds},{stepSeconds}]',
    'mock': ''
  },
  'host.host_components.filtered': {
    'real': '/clusters/{clusterName}/hosts',
    'mock': '',
    format: function(data, opt) {
      return {
        url: opt.url + data.urlParams
      }
    }
  },
  'host.status.counters': {
    'real': '/clusters/{clusterName}?fields=alerts,Clusters/health_report,Clusters/total_hosts&minimal_response=true',
    'mock': '/data/hosts/HDP2/host_status_counters.json'
  },
  'components.filter_by_status': {
    'real': '/clusters/{clusterName}/components?fields=host_components/HostRoles/host_name,ServiceComponentInfo/component_name,ServiceComponentInfo/started_count{urlParams}&minimal_response=true',
    'mock': ''
  },
  'hosts.all.install': {
    'real': '/hosts?minimal_response=true',
    'mock': ''
  },
  'hosts.all': {
    'real': '/clusters/{clusterName}/hosts?minimal_response=true',
    'mock': ''
  },
  'hosts.with_public_host_names': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/public_host_name&minimal_response=true',
    'mock': ''
  },
  'hosts.for_quick_links': {
    'real': '/clusters/{clusterName}/hosts?Hosts/host_name.in({masterHosts})&fields=Hosts/public_host_name,host_components/HostRoles/component_name{urlParams}&minimal_response=true',
    'mock': ''
  },
  'hosts.confirmed.install': {
    'real': '/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem&minimal_response=true',
    'mock': ''
  },
  'hosts.confirmed': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/os_type,Hosts/os_arch,Hosts/ip,host_components/HostRoles/state&minimal_response=true',
    'mock': ''
  },
  'host_components.all': {
    'real': '/clusters/{clusterName}/host_components?fields=HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'host_components.with_services_names': {
    'real': '/clusters/{clusterName}/host_components?fields=component/ServiceComponentInfo/service_name,HostRoles/host_name&minimal_response=true',
    'mock': ''
  },
  'components.get_installed': {
    'real': '/clusters/{clusterName}/components',
    'mock': ''
  },
  'hosts.heatmaps': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/host_name,Hosts/public_host_name,Hosts/os_type,Hosts/ip,host_components,metrics/disk,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free&minimal_response=true',
    'mock': ''
  },

  'custom_action.create': {
    'real': '/requests',
    'mock': '',
    'format': function(data) {
      var requestInfo = {
        context: 'Check host',
        action: 'check_host',
        parameters: { }
      };
      $.extend(true, requestInfo, data.requestInfo)
      return {
        type: 'POST',
        data: JSON.stringify({
          'RequestInfo': requestInfo,
          'Requests/resource_filters': [{
            hosts: data.filteredHosts.join(',')
          }]
        })
      }
    }
  },
  'custom_action.request': {
    'real': '/requests/{requestId}/tasks/{taskId}',
    'mock': '',
    'format': function(data) {
      return {
        requestId: data.requestId,
        taskId: data.taskId || ''
      }
    }
  },
  'hosts.total_count': {
    'real': '/clusters/{clusterName}?fields=Clusters/total_hosts&minimal_response=true',
    'mock': '',
    'format': function() {
      return {
        async: false
      }
    }
  },
  'hosts.high_availability.wizard': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem&minimal_response=true',
    'mock': ''
  },
  'hosts.security.wizard': {
    'real': '/clusters/{clusterName}/hosts?fields=host_components/HostRoles/service_name&minimal_response=true',
    'mock': ''
  },
  'host_component.installed.on_hosts': {
    'real': '/clusters/{clusterName}/host_components?HostRoles/component_name={componentName}&HostRoles/host_name.in({hostNames})&fields=HostRoles/host_name&minimal_response=true',
    'mock': '',
    'format': function() {
      return {
        async: false
      }
    }
  },
  'hosts.basic_info': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name&minimal_response=true',
    'mock': ''
  },
  'hosts.one_component_of_type': {
    'real': '/clusters/{clusterName}/hosts?host_components/HostRoles/component_name.in({componentNames})&fields=host_components&page_size=1&minimal_response=true',
    'mock': ''
  },
  'hosts.all_components_of_type': {
    'real': '/clusters/{clusterName}/hosts?host_components/HostRoles/component_name.in({componentNames})&fields=host_components&minimal_response=true',
    'mock': ''
  },
  'hosts.config_groups': {
    'real': '/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name,host_components&minimal_response=true',
    'mock': ''
  },
  'cluster.fields': {
    real: '/clusters/{clusterName}?fields={fields}',
    mock: '',
    format: function(data) {
      return {
        async: true,
        fields: data.fields.join(',')
      }
    }
  },
  'hosts.host_components.pre_load': {
    real: '',
    mock: '',
    format: function(data) {
      return {
        url: data.url
      }
    }
  },
  'hosts.bulk.operations': {
    real: '',
    mock: '',
    format: function(data) {
      return {
        url: data.url
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
  if (!url) return null;
  var keys = url.match(/\{\w+\}/g);
  keys = (keys === null) ? [] : keys;
  if (keys) {
    keys.forEach(function (key) {
      var raw_key = key.substr(1, key.length - 2);
      var replace;
      if (!data || !data[raw_key]) {
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
    var prefix = this.apiPrefix != null ? this.apiPrefix : App.apiPrefix;
    opt.url = prefix + formatUrl(this.real, data);
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
var ajax = Em.Object.extend({
  /**
   * Send ajax request
   *
   * @param {Object} config
   * @return {$.ajax} jquery ajax object
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
    if (!urls[config.name]) {
      console.warn('Invalid name provided!');
      return null;
    }
    opt = formatRequest.call(urls[config.name], params);

    opt.context = this;

    // object sender should be provided for processing beforeSend, success and error responses
    opt.beforeSend = function (xhr) {
      if (config.beforeSend) {
        config.sender[config.beforeSend](opt, xhr, params);
      }
    };
    opt.success = function (data) {
      console.log("TRACE: The url is: " + opt.url);
      if (config.success) {
        config.sender[config.success](data, opt, params);
      }
    };
    opt.error = function (request, ajaxOptions, error) {
      if (config.error) {
        config.sender[config.error](request, ajaxOptions, error, opt, params);
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
    try {
      var json = $.parseJSON(jqXHR.responseText);
      var message = json.message;
    } catch (err) {
    }
    if (!showStatus) {
      showStatus = 500;
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
          showMessage: !!message
        })
      });
    }
  }

});

/**
 * Add few access-methods for test purposes
 */
if ($.mocho) {
  ajax.reopen({
    /**
     * Don't use it anywhere except tests!
     * @returns {Array}
     */
    fakeGetUrlNames: function() {
      return Em.keys(urls);
    },

    /**
     * Don't use it anywhere except tests!
     * @param name
     * @returns {*}
     */
    fakeGetUrl: function(name) {
      return urls[name];
    },

    /**
     * Don't use it anywhere except tests!
     * @param url
     * @param data
     * @returns {String}
     */
    fakeFormatUrl: function(url, data) {
      return formatUrl(url, data);
    },

    /**
     * Don't use it anywhere except tests!
     * @param urlObj
     * @param data
     * @returns {Object}
     */
    fakeFormatRequest: function(urlObj, data) {
      return formatRequest.call(urlObj, data);
    }
  });
}

App.ajax = ajax.create({});
