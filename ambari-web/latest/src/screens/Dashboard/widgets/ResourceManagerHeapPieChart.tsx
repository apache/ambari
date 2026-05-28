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
import { Doughnut } from "react-chartjs-2";
import {
  Chart as ChartJs,
  ArcElement,
  Tooltip,
  Legend,
} from "chart.js";
import { isEmpty } from "lodash";


ChartJs.register(ArcElement, Tooltip, Legend);

interface HeapData {
  heapUsed: number;
  heapMax: number;
  heapFree: number;
  usagePercent: number;
}

export default function ResourceManagerHeapPieChart() {
  const { allServiceModels } = useContext(ServiceContext);
  const [chartData, setChartData] = useState<any>(null);
  const [heapData, setHeapData] = useState<HeapData>({
    heapUsed: 0,
    heapMax: 0,
    heapFree: 0,
    usagePercent: 0,
  });


  useEffect(() => {
    if (allServiceModels?.yarn) {
      const yarnService = allServiceModels.yarn;
      const heapUsed = yarnService.jvmMemoryHeapUsed || 0;
      const heapMax = yarnService.jvmMemoryHeapMax || 0;
      const heapFree = heapMax - heapUsed;
      const usagePercent = heapMax > 0 ? (heapUsed / heapMax) * 100 : 0;

      const newHeapData: HeapData = {
        heapUsed,
        heapMax,
        heapFree,
        usagePercent,
      };

      setHeapData(newHeapData);

      // Update chart data
      const chartConfig = {
        labels: ['Heap Used', 'Heap Free'],
        datasets: [
          {
            data: [usagePercent, 100 - usagePercent],
            backgroundColor: [
              usagePercent > 90 ? '#dc3545' : usagePercent > 70 ? '#ffc107' : '#429929',
              '#D3D3D3',
            ],
            borderColor: [
              usagePercent > 90 ? '#dc3545' : usagePercent > 70 ? '#ffc107' : '#429929',
              '#D3D3D3',
            ],
            borderWidth: 1,
          },
        ],
      };

      setChartData(chartConfig);
    }
  }, [JSON.stringify(allServiceModels?.yarn)]);

  const isLoading = !allServiceModels?.yarn;


  const formatBytes = (bytes: number): string => {
    if (bytes === 0) return "0 B";
    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

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

  const displayText = `${heapData.usagePercent.toFixed(1)}%`;
  const hoverContent = `${Math.round(heapData.usagePercent)}% ${formatBytes(heapData.heapUsed)} used of ${formatBytes(heapData.heapMax)} total`;

  if (isLoading && isEmpty(chartData)) {
    return (
      <ChartContainer text="Loading..." onHoverContent="Fetching ResourceManager heap metrics...">
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
