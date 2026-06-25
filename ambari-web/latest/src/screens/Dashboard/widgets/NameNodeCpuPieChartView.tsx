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

import { useContext, useEffect, useState, useCallback } from "react";
import { ServiceContext } from "../../../store/ServiceContext";
import ChartContainer from "../ChartContainer";
import { Doughnut } from "react-chartjs-2";
import { ArcElement, Tooltip, Chart as ChartJs } from "chart.js";
import { AppContext } from "../../../store/context";
import metricsApi from "../../../api/metricsApi";
import usePolling from "../../../hooks/usePolling";

ChartJs.register(ArcElement, Tooltip);

export default function NameNodeCpuPieChartView({ subGroupId = "" }) {
  const { allServiceModels } = useContext(ServiceContext);
  const { clusterName } = useContext(AppContext);
  const [cpuWio, setCpuWio] = useState<number | null>(null);
  const [nnHostName, setNnHostName] = useState("");

  useEffect(() => {
    const isHaEnabled = allServiceModels?.hdfs?.isHaEnabled;

    if (isHaEnabled) {
      const activeNameNodes = allServiceModels?.hdfs?.activeNameNodes || [];
      const nn = activeNameNodes.find(
        (nn: any) => nn.haNameSpace === subGroupId
      );
      if (nn) {
        setNnHostName(nn.hostName);
      }
    } else {
      const nameNode = allServiceModels?.hdfs?.nameNode;
      if (nameNode) {
        setNnHostName(nameNode.hostName);
      }
    }
  }, [allServiceModels, subGroupId]);

  const getValue = useCallback(async () => {
    if (!nnHostName || !clusterName) {
      return;
    }

    try {
      const response = await metricsApi.getNameNodeCpuWio(
        clusterName,
        nnHostName
      );
      setCpuWio(response?.metrics?.cpu?.cpu_wio ?? null);
    } catch (error) {
      console.error("Error fetching NameNode CPU data:", error);
    }
  }, [clusterName, nnHostName]);

  // Use usePolling hook for automatic polling
  usePolling(getValue, 60000);

  const value = cpuWio !== null ? (cpuWio >= 100 ? 100 : cpuWio) : 0;
  const percent = value.toFixed(1);
  const percentPrecise = value.toFixed(2);

  const data = {
    datasets: [
      {
        data: [value, 100 - value],
        backgroundColor: ["#FF8C00", "#D3D3D3"],
      },
    ],
    labels: ["CPU wait I/O", "Available"],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        enabled: false,
      },
    },
    cutout: "75%", // Makes it a donut chart
  };

  // If data is not available yet, show loading or placeholder
  if (cpuWio === 0.0 || null) {
    return (
      <ChartContainer text="N/A" onHoverContent="CPU wait I/O: Not Available">
        <div className="d-flex justify-content-center mh-100">
          <Doughnut
            data={{
              datasets: [{ data: [100], backgroundColor: ["#D3D3D3"] }],
              labels: ["No Data"],
            }}
            options={options}
          />
        </div>
      </ChartContainer>
    );
  }

  return (
    <ChartContainer
      text={`${percent}%`}
      onHoverContent={`CPU wait I/O: ${percentPrecise}%`}
    >
      <div className="d-flex justify-content-center mh-100">
        <Doughnut data={data} options={options} />
      </div>
    </ChartContainer>
  );
}
