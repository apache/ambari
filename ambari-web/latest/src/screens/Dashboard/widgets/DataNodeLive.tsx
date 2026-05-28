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

import { useContext } from "react";
import { ServiceContext } from "../../../store/ServiceContext";
import ChartContainer from "../ChartContainer";
import { messages } from "../../messages";

export default function DataNodeLive() {
  const { allServiceModels } = useContext(ServiceContext);

  const liveDataNodes = allServiceModels["hdfs"]?.["liveNodesDataNodes"] || 0;
  const deadDataNodes = allServiceModels["hdfs"]?.["deadNodesDataNodes"] || 0;
  const decomDataNodes =
    allServiceModels["hdfs"]?.["decommissionedNodesDataNodes"] || 0;
  const totalDataNodes = allServiceModels["hdfs"]?.["slaveComponents"].find((slaveComponent:any)=>slaveComponent.componentName === "DATANODE")?.totalCount || 0;

  const someMetricsNA =
    liveDataNodes === null ||
    totalDataNodes === null ||
    allServiceModels["hdfs"]?.["metricsNotAvailable"];

  const content = someMetricsNA
    ? messages["services.service.summary.notAvailable"]
    : `${liveDataNodes}/${totalDataNodes}`;

  const hiddenInfo = [
    `${liveDataNodes} ${messages["dashboard.services.hdfs.nodes.live"]}`,
    `${deadDataNodes} ${messages["dashboard.services.hdfs.nodes.dead"]}`,
    `${decomDataNodes} ${messages["dashboard.services.hdfs.nodes.decom"]}`,
  ];

  return (
    <ChartContainer
      text={content}
      onHoverContent={hiddenInfo.join(", ")}
    ></ChartContainer>
  );
}
