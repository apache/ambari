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
import { Doughnut } from "react-chartjs-2";
import { ArcElement, Tooltip, Chart as ChartJs } from "chart.js";

ChartJs.register(ArcElement, Tooltip);

export default function NameNodeCapacity() {
  const { allServiceModels } = useContext(ServiceContext);

  const modelValueMax = allServiceModels["hdfs"]?.["capacityTotal"];
  const modelValueUsed = allServiceModels["hdfs"]?.["capacityRemaining"];
  const modelValueCapacityUsed = allServiceModels["hdfs"]?.["capacityUsed"];
  const modelValueNonDfsUsed = allServiceModels["hdfs"]?.["capacityNonDfsUsed"];

  const total = modelValueMax || 0;
  const remaining = modelValueUsed || 0;
  const dfsUsed = modelValueCapacityUsed || 0;
  const nonDfsUsed = modelValueNonDfsUsed || 0;

  const dfsPercent = total > 0 ? (dfsUsed * 100) / total : 0;
  const nonDfsPercent = total > 0 ? (nonDfsUsed * 100) / total : 0;
  const remainingPercent = total > 0 ? (remaining * 100) / total : 0;

  const data = {
    datasets: [
      {
        data: [dfsPercent + nonDfsPercent, remainingPercent],
        backgroundColor: ["#429929", "#D3D3D3"],
      },
    ],
    labels: ["Used", "Remaining"],
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
    cutout: "75%",
  };

  const dataToDisplay = `DFS Used: ${dfsPercent.toFixed(
    1
  )}%, Non-DFS Used: ${nonDfsPercent.toFixed(
    1
  )}%, Remaining: ${remainingPercent.toFixed(1)}%`;

  return (
    <ChartContainer
      text={`${Math.round(dfsPercent + nonDfsPercent)}%`}
      onHoverContent={dataToDisplay}
    >
      <div className="d-flex justify-content-center mh-100">
        <Doughnut data={data} options={options} />
      </div>
    </ChartContainer>
  );
}
