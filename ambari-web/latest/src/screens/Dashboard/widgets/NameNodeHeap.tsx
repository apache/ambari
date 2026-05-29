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
import { ArcElement, Tooltip, Chart as ChartJs } from "chart.js";
import { isEmpty } from "lodash";

ChartJs.register(ArcElement, Tooltip);

export default function NameNodeHeap() {
  const { allServiceModels } = useContext(ServiceContext);

  const [chartData, setChartData] = useState({
    maxValue: 0,
    usedValue: 0,
    percentage: 0,
    data: {
      datasets: [
        {
          data: [0, 100],
          backgroundColor: ["#429929", "#D3D3D3"],
        },
      ],
      labels: ["Used", "Free"],
    },
  });

  useEffect(() => {
    setChartData(getData());
  }, [JSON.stringify(allServiceModels)]);

  const modelValueMax = allServiceModels["hdfs"]?.["jvmMemoryHeapMaxValues"];
  const modelValueUsed = allServiceModels["hdfs"]?.["jvmMemoryHeapUsedValues"];

  const getUsed = () => {
    return modelValueUsed / (1024 * 1024) || 0;
  };

  const getMax = () => {
    return modelValueMax / (1024 * 1024) || 0;
  };

  const getData = () => {
    const usedValue = getUsed();
    const maxValue = getMax();
    const percentage = maxValue ? (usedValue / maxValue) * 100 : 0;

    const data = {
      datasets: [
        {
          data: [percentage, 100 - percentage],
          backgroundColor: ["#429929", "#D3D3D3"],
        },
      ],
      labels: ["Used", "Free"],
    };

    return { data, percentage, usedValue, maxValue };
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

  const dataToDisplay = `${chartData.percentage.toFixed(
    1
  )}% used \n ${chartData.usedValue.toFixed(1)}MB of ${(
    chartData.maxValue / 1024
  ).toFixed(1)}GB`;

  return (
    <ChartContainer
      text={`${Math.round(chartData.percentage)}%`}
      onHoverContent={dataToDisplay}
    >
      <div className="d-flex justify-content-center mh-100">
        {!isEmpty(chartData.data) && (
          <Doughnut data={chartData.data} options={options} />
        )}
      </div>
    </ChartContainer>
  );
}
