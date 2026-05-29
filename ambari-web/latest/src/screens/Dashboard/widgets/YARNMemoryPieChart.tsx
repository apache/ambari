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
import ChartContainer from "../ChartContainer";
import { Doughnut } from "react-chartjs-2";
import {
  Chart as ChartJs,
  ArcElement,
  Tooltip,
  Legend,
} from "chart.js";
import { isEmpty } from "lodash";
import { ServiceContext } from "../../../store/ServiceContext";

ChartJs.register(ArcElement, Tooltip, Legend);

interface YARNMemoryData {
  allocatedMB: number;
  availableMB: number;
  totalMB: number;
  usagePercent: number;
}

export default function YARNMemoryPieChart() {
  const { allServiceModels } = useContext(ServiceContext);
  const [chartData, setChartData] = useState<any>(null);
  const [memoryData, setMemoryData] = useState<YARNMemoryData>({
    allocatedMB: 0,
    availableMB: 0,
    totalMB: 0,
    usagePercent: 0,
  });



  useEffect(() => {
     if (allServiceModels?.yarn) {
        const yarnService = allServiceModels.yarn;
        const allocatedMB = yarnService.allocatedMemory || 0;
        const availableMB = yarnService.availableMemory || 0;
        const totalMB = allocatedMB + availableMB;
        const usagePercent = totalMB > 0 ? (allocatedMB / totalMB) * 100 : 0;

          const newMemoryData: YARNMemoryData = {
            allocatedMB,
            availableMB,
            totalMB,
            usagePercent,
          };

          setMemoryData(newMemoryData);

          const chartConfig = {
            labels: ['Allocated Memory', 'Available Memory'],
            datasets: [
              {
                data: [usagePercent, 100 - usagePercent],
                backgroundColor: [
                  usagePercent > 90 ? '#dc3545' : usagePercent > 75 ? '#ffc107' : '#28a745',
                  '#e9ecef',
                ],
                borderColor: [
                  usagePercent > 90 ? '#dc3545' : usagePercent > 75 ? '#ffc107' : '#28a745',
                  '#dee2e6',
                ],
                borderWidth: 1,
              },
            ],
          };

          setChartData(chartConfig);
        }
  }, [JSON.stringify(allServiceModels?.yarn)]);

  const isLoading = !allServiceModels?.yarn;



  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        enabled: false,
      }, 
    },
    cutout: "75%",
  };

  const displayText = `${memoryData.usagePercent.toFixed(1)}%`;
  const hoverContent = `${memoryData.usagePercent}% ${(memoryData.allocatedMB)} allocated of ${(memoryData.totalMB)} total`;

  if (isLoading && isEmpty(chartData)) {
    return (
      <ChartContainer text="Loading..." onHoverContent="Fetching YARN memory metrics...">
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
    <div className="d-flex justify-content-center mh-100">
        {!isEmpty(chartData) && (
          <Doughnut data={chartData} options={options} />
        )}
      </div>
    </ChartContainer>
  );
}
