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

import { messages } from "../messages";
import DataNodeUp from "./widgets/DataNodeLive";
import HBaseAverageLoad from "./widgets/HbaseAverageLoad";
import HBaseMasterHeap from "./widgets/HbaseMasterHeap";
import HBaseMasterUptime from "./widgets/HbaseMasterUptime";
import HBaseRegionsInTransition from "./widgets/HbaseRegionsInTransition";
import NameNodeCapacity from "./widgets/NameNodeCapacity";
import NameNodeCpuPieChartView from "./widgets/NameNodeCpuPieChartView";
import NameNodeHeap from "./widgets/NameNodeHeap";
import NameNodeRpc from "./widgets/NameNodeRpc";
import NameNodeUptime from "./widgets/NameNodeUptime";
import ResourceManagerHeapPieChart from "./widgets/ResourceManagerHeapPieChart";
import ResourceManagerUptime from "./widgets/ResourceManagerUptime";
import NodeManagersLive from "./widgets/NodeManagersLive";
import YARNMemoryPieChart from "./widgets/YARNMemoryPieChart";
import YarnContainers from "./widgets/YarnContainers";
import CpuMetrics from "./widgets/CpuClusterMetrics";
import MemoryClusterMetrics from "./widgets/MemoryClusterMetrics";
import NetworkClusterMetrics from "./widgets/NetworkClusterMetrics";
import LoadClusterMetrics from "./widgets/LoadClusterMetrics";

export const dashboardWidgets = [
  {
    id: 1,
    viewName: <NameNodeHeap />,
    sourceName: "HDFS",
    title: messages["dashboard.widgets.NameNodeHeap"],
    threshold: [80, 90],
    groupName: "nn",
    isVisible: true,
  },
  {
    id: 2,
    viewName: <NameNodeCapacity />,
    sourceName: "HDFS",
    title: messages["dashboard.widgets.HDFSDiskUsage"],
    threshold: [85, 95],
    isVisible: true,
  },
  {
    id: 3,
    viewName: <NameNodeCpuPieChartView />,
    sourceName: "HDFS",
    title: messages["dashboard.widgets.NameNodeCpu"],
    threshold: [90, 95],
    groupName: "nn",
    isVisible: true,
  },
  {
    id: 4,
    viewName: <DataNodeUp />,
    sourceName: "HDFS",
    title: messages["dashboard.widgets.DataNodeUp"],
    threshold: [80, 90],
    isVisible: true,
  },
  {
    id: 5,
    viewName: <NameNodeRpc />,
    sourceName: "HDFS",
    title: messages["dashboard.widgets.NameNodeRpc"],
    threshold: [1000, 3000],
    groupName: "nn",
    isVisible: true,
  },
  {
    id: 6,
    viewName: <MemoryClusterMetrics />,
    sourceName: "HOST_METRICS",
    title: messages["dashboard.clusterMetrics.memory"],
    threshold: [],
  },
  {
    id: 7,
    viewName: <NetworkClusterMetrics />,
    sourceName: "HOST_METRICS",
    title: messages["dashboard.clusterMetrics.network"],
    threshold: [],
  },
  {
    id: 8,
    viewName: <CpuMetrics />,
    sourceName: "HOST_METRICS",
    title: messages["dashboard.clusterMetrics.cpu"],
    threshold: [],
  },
  {
    id: 9,
    viewName: <LoadClusterMetrics />,
    sourceName: "HOST_METRICS",
    title: messages["dashboard.clusterMetrics.load"],
    threshold: [],
  },
  {
    id: 10,
    viewName: <NameNodeUptime />,
    sourceName: "HDFS",
    title: messages["dashboard.widgets.NameNodeUptime"],
    threshold: [],
    groupName: "nn",
    isVisible: true,
  },
  {
    id: 11,
    viewName: "HDFSLinksView",
    sourceName: "HDFS",
    title: messages["dashboard.widgets.HDFSLinks"],
    threshold: [],
    groupName: "nn",
    isHiddenByDefault: true,
  },
  {
    id: 12,
    viewName: "HBaseLinksView",
    sourceName: "HBASE",
    title: messages["dashboard.widgets.HBaseLinks"],
    threshold: [],
    isHiddenByDefault: true,
  },
  {
    id: 13,
    viewName: <HBaseMasterHeap />,
    sourceName: "HBASE",
    title: messages["dashboard.widgets.HBaseMasterHeap"],
    threshold: [70, 90],
    isVisible: true,
  },
  {
    id: 14,
    viewName: <HBaseAverageLoad />,
    sourceName: "HBASE",
    title: messages["dashboard.widgets.HBaseAverageLoad"],
    threshold: [150, 250],
    isVisible: true,
  },
  {
    id: 15,
    viewName: <HBaseRegionsInTransition />,
    sourceName: "HBASE",
    title: messages["dashboard.widgets.HBaseRegionsInTransition"],
    threshold: [3, 10],
    isVisible: true,
  },
  {
    id: 16,
    viewName: <HBaseMasterUptime />,
    sourceName: "HBASE",
    title: messages["dashboard.widgets.HBaseMasterUptime"],
    threshold: [],
    isVisible: true,
  },
  {
    id: 17,
    viewName: <ResourceManagerHeapPieChart />,
    sourceName: "YARN",
    title: messages["dashboard.widgets.ResourceManagerHeap"],
    threshold: [70, 90],
    isVisible: true,
  },
  {
    id: 18,
    viewName: <ResourceManagerUptime />,
    sourceName: "YARN",
    title: messages["dashboard.widgets.ResourceManagerUptime"],
    threshold: [],
    isHiddenByDefault: true,
    isVisible: true,
  },
  {
    id: 19,
    viewName: <NodeManagersLive />,
    sourceName: "YARN",
    title: messages["dashboard.widgets.NodeManagersLive"],
    threshold: [50, 75],
    isVisible: true,
  },
  {
    id: 20,
    viewName: <YARNMemoryPieChart />,
    sourceName: "YARN",
    title: messages["dashboard.widgets.YARNMemory"],
    threshold: [50, 75],
    isHiddenByDefault: true,
    isVisible: true,
  },
  {
    id: 21,
    viewName: "SuperVisorUpView",
    sourceName: "STORM",
    title: messages["dashboard.widgets.SuperVisorUp"],
    threshold: [85, 95],
  },
  {
    id: 22,
    viewName: "FlumeAgentUpView",
    sourceName: "FLUME",
    title: messages["dashboard.widgets.FlumeAgentUp"],
    threshold: [85, 95],
  },
  {
    id: 23,
    viewName: "YARNLinksView",
    sourceName: "YARN",
    title: messages["dashboard.widgets.YARNLinks"],
    threshold: [],
    isHiddenByDefault: true,
  },
  {
    id: 24,
    viewName: "HawqSegmentUpView",
    sourceName: "HAWQ",
    title: messages["dashboard.widgets.HawqSegmentUp"],
    threshold: [75, 90],
  },
  {
    id: 25,
    viewName: "PxfUpView",
    sourceName: "PXF",
    title: messages["dashboard.widgets.PxfUp"],
    threshold: [],
  },
  {
    id: 26,
    viewName: <YarnContainers />,
    sourceName: "YARN",
    title: messages["dashboard.widgets.YarnContainers"],
    threshold: [],
    isVisible: true,
  },
];
