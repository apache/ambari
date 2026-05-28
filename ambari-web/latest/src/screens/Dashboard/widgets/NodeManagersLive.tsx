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

import { useContext, useEffect, useState } from "react";
import { ServiceContext } from "../../../store/ServiceContext";
import ChartContainer from "../ChartContainer";

interface NodeManagerStatus {
  total: number;
  live: number;
  dead: number;
  livePercent: number;
}

export default function NodeManagersLive() {
  const { allServiceModels } = useContext(ServiceContext);
  const [nodeManagerStatus, setNodeManagerStatus] = useState<NodeManagerStatus>({
    total: 0,
    live: 0,
    dead: 0,
    livePercent: 0,
  });
  

  useEffect(() => {
    if (allServiceModels?.yarn) {
      const yarnService = allServiceModels.yarn;
      const total = yarnService["slaveComponents"].find((slaveComponent:any)=>slaveComponent.componentName === "NODEMANAGER")?.totalCount || 0;
      const live = yarnService["slaveComponents"].find((slaveComponent:any)=>slaveComponent.componentName === "NODEMANAGER")?.startedCount || 0;
      const dead = total - live;
      const livePercent = total > 0 ? (live / total) * 100 : 0;

      setNodeManagerStatus({
        total,
        live,
        dead,
        livePercent,
      });
    }
  }, [JSON.stringify(allServiceModels?.yarn)]);

  const isLoading = !allServiceModels?.yarn;

  const displayText = `${nodeManagerStatus.live}/${nodeManagerStatus.total}`;
  const hoverContent = `NodeManagers: ${nodeManagerStatus.live} live, ${nodeManagerStatus.dead} dead (${nodeManagerStatus.livePercent.toFixed(1)}% live)`;

  if (isLoading) {
    return (
      <ChartContainer text="Loading..." onHoverContent="Fetching NodeManager status...">
        <div className="d-flex justify-content-center align-items-center" style={{ height: "200px" }}>
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      </ChartContainer>
    );
  }

  return (
    <ChartContainer text={displayText} onHoverContent={hoverContent}>
    </ChartContainer>
  );
}
