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

interface ContainerData {
  allocated: number;
  pending: number;
  reserved: number;
  total: number;
}

export default function YarnContainers() {
  const { allServiceModels } = useContext(ServiceContext);
  const [containerData, setContainerData] = useState<ContainerData>({
    allocated: 0,
    pending: 0,
    reserved: 0,
    total: 0,
  });


  useEffect(() => {
    if (allServiceModels?.yarn) {
      const yarnService = allServiceModels.yarn;
      const allocated = yarnService.containersAllocated || 0;
      const pending = yarnService.containersPending || 0;
      const reserved = yarnService.containersReserved || 0;
      const total = allocated + pending + reserved;

      setContainerData({
        allocated,
        pending,
        reserved,
        total,
      });
    }
  }, [allServiceModels?.yarn]);

  const isLoading = !allServiceModels?.yarn;

  const displayText = `${containerData.allocated}/${containerData.pending}/${containerData.reserved}`;
  const hoverContent = `${containerData.allocated} allocated, ${containerData.pending} pending, ${containerData.reserved} reserved`;

  if (isLoading) {
    return (
      <ChartContainer text="Loading..." onHoverContent="Fetching YARN container metrics...">
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
