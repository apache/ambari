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

/**
 * Never Delete any key without seraching it in all View and Template files
 */
(function (window, undefined) {
  var Globalize;
  require(['globalize'], function (globalize) {
    Globalize = globalize;
    Globalize.addCultureInfo("en", {
      messages: {
        // Form labels, Table headers etc
        lbl: {
          name: 'Name',
          id: 'ID',
          owner: 'Owner',
          topologySummary: 'Topology Summary',
          total: 'Total',
          active: 'Active',
          inactive: 'Inactive',
          status: 'Status',
          uptime: 'Uptime',
          workers: 'Workers',
          executors: 'Executors',
          tasks: 'Tasks',
          schedulerInfo: 'Scheduler Info',
          jar: 'Jar',
          nimbusHostname: 'Nimbus Hostname',
          topologyClass: 'Topology Class',
          arguments: 'Arguments',
          selectTimeFrame: 'Select time frame',
          showSystemBolt: 'Show System Bolt',
          topologySummary: 'Topology Summary',
          last10Min: 'Last 10 minutes',
          last3Hr: 'Last 3 hours',
          last1Day: 'Last 1 day',
          allTime: 'All time',
          emitted: 'Emitted',
          transferred: 'Transferred',
          Capacity: 'Capacity (last 10m)',
          executed: 'Executed',
          processLatency: 'Process Latency (ms)',
          completeLatencyMS: 'Complete Latency (ms)',
          acked: 'Acked',
          key: 'Key',
          value: 'Value',
          host: 'Host',
          slots: 'Slots',
          usedSlots: 'Used Slots',
          port: 'Port',
          freeSlots: 'Free Slots',
          totalSlots: 'Total Slots',
          isLeader: 'Is Leader',
          version: 'Version',
          uptimeSeconds: 'Uptime',
          supervisors: 'Supervisors',
          viewLogs: 'View Logs',
          rebalanceTopology: 'Rebalance Topology',
          waitTime : 'Wait Time',
          statistics: 'Statistics',
          spouts: 'Spouts',
          bolts: 'Bolts',
          topologyConfig: 'Topology Configuration',
          outputStats: 'Output Stats',
          errors: 'Errors',
          error: 'Error',
          errorPort: 'Error Port',
          errorHost: 'Error Host',
          time: 'Time',
          failed: 'Failed',
          stream: 'Stream',
          completeLatency: 'Complete Latency',
          executeLatency: 'Execute Latency',
          capacity: 'Capacity'
        },
        btn: {
          deployNewTopology: 'Deploy New Topology',
          activate: 'Activate',
          deactivate: 'Deactivate',
          rebalance: 'Rebalance',
          kill: 'Kill',
          cancel: 'Cancel',
          save: 'Save',
          yes: 'Yes',
          no: 'No',
          apply: 'Apply'
        },
        h: {
          topologies: 'Topologies',
          cluster: 'Cluster',
          clusterSummary: 'Cluster Summary',
          nimbusSummary: 'Nimbus Summary',
          supervisorSummary: 'Supervisor Summary',
          nimbusConfiguration: 'Nimbus Configuration'
        },
        msg: {
          noTopologyFound: 'No topology found',
          topologySummaryName: 'The name given to the topology by when it was submitted.',
          topologySummaryId: 'The unique ID given to a Topology each time it is launched.',
          topologySummaryOwner: 'The user that submitted the Topology, if authentication is enabled.',
          topologySummaryStatus: 'The status can be one of ACTIVE, INACTIVE, KILLED, or REBALANCING.',
          topologySummaryUptime: 'The time since the Topology was submitted.',
          topologySummaryWorkers: 'The number of Workers (processes).',
          topologySummaryExecutors: 'Executors are threads in a Worker process.',
          topologySummaryTasks: 'A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.',
          topologySummaryScheduler: 'This shows information from the scheduler about the latest attempt to schedule the Topology on the cluster.',
          noClusterFound: 'No cluster found',
          noNimbusFound: 'No nimbus found',
          noSupervisorFound: 'No supervisor found',
          noNimbusConfigFound: 'No nimbus configuration found',
          noSpoutFound: 'No spout found',
          noBoltFound: 'No bolt found',
          noOutputStatsFound: 'No output stats found',
          noExecutorsFound: 'No executor found',
          noErrorFound: 'No error found',
          noTopologyConfigFound: 'No topology configuration found',
          clusterSummarySupervisors: 'The number of nodes in the cluster currently.',
          clusterSummarySlots: 'Slots are Workers (processes).',
          clusterSummaryExecutors: 'Executors are threads in a Worker process.',
          clusterSummaryTasks: 'A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.',
          supervisorId: 'A unique identifier given to a Supervisor when it joins the cluster.',
          supervisorHost: 'The hostname reported by the remote host. (Note that this hostname is not the result of a reverse lookup at the Nimbus node.)',
          supervisorUptime: 'The length of time a Supervisor has been registered to the cluster.',
          stormNotRunning: 'Error detected while fetching storm hostname and port number',
          stormNotConfigured: 'Currently, no service is configured in ambari',
          spoutId: 'The ID assigned to a the Component by the Topology.',
          spoutExecutors: 'Executors are threads in a Worker process.',
          spoutTasks: 'A Task is an instance of a Bolt or Spout. The number of Tasks is almost always equal to the number of Executors.',
          emitted: 'The number of Tuples emitted.',
          transferred: 'The number of Tuples emitted that sent to one or more bolts.',
          completeLatency: 'The average time a Tuple "tree" takes to be completely processed by the Topology. A value of 0 is expected if no acking is done.',
          acked: 'The number of Tuple "trees" successfully processed. A value of 0 is expected if no acking is done.',
          failed: 'The number of Tuple "trees" that were explicitly failed or timed out before acking was completed. A value of 0 is expected if no acking is done.',
          stream: 'The name of the Tuple stream given in the Topolgy, or "default" if none was given.',
          uniqueExecutorId: 'The unique executor ID.',
          extensionUptime: 'The length of time an Executor (thread) has been alive.',
          extensionHost: 'The hostname reported by the remote host. (Note that this hostname is not the result of a reverse lookup at the Nimbus node.)',
          extensionPort: 'The port number used by the Worker to which an Executor is assigned. Click on the port number to open the logviewer page for this Worker.',
          boltCapacity: 'If this is around 1.0, the corresponding Bolt is running as fast as it can, so you may want to increase the Bolt\'s parallelism. This is (number executed * average execute latency) / measurement time.',
          boltExecuteLatency: 'The average time a Tuple spends in the execute method. The execute method may complete without sending an Ack for the tuple.',
          boltExected: 'The number of incoming Tuples processed.',
          boltProcessLatency: 'The average time it takes to Ack a Tuple after it is first received.  Bolts that join, aggregate or batch may not Ack a tuple until a number of other Tuples have been received.',
          boltAcked: 'The number of Tuples acknowledged by this Bolt.',
        },
        plcHldr: {
          search: 'Search'
        },
        dialogMsg: {
          topologyBeingDeployed: 'Please wait, topology is being deployed',
          invalidFile: 'Selected file to upload is not a .jar file',
          topologyDeployedSuccessfully: 'New topology deployed successfully',
          topologyDeployFailed: 'Deploying new topology failed.',
          activateTopologyMsg: 'Are you sure you want to activate this topology ?',
          topologyActivateSuccessfully: 'Topology activated successfully',
          deactivateTopologyMsg: 'Are you sure you want to deactivate this topology ?',
          killTopologyMsg: 'Are you sure you want to kill this topology ? If yes, please, specify wait time in seconds.',
          topologyDeactivateSuccessfully: 'Topology deactivated successfully',
          topologyRebalanceSuccessfully: 'Topology rebalanced successfully',
          topologyKilledSuccessfully: 'Topology killed successfully',
        },
        validationMessages: {}
      }
    });
  });
}(this));
