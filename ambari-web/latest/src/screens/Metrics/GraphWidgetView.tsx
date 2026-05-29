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

import { Line } from "react-chartjs-2";
import { WidgetInfo } from "./type";
import {
  LineElement,
  PointElement,
  LinearScale,
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  Chart as ChartJs,
} from "chart.js";
import { isEmpty } from "lodash";
import { Alert } from "react-bootstrap";

ChartJs.register(
  LineElement,
  PointElement,
  LinearScale,
  Title,
  Tooltip,
  Legend,
  CategoryScale
);

type RenderWidgetProps = {
  widgetInfo: WidgetInfo;
  metricsValues: any;
};

export default function GraphWidgetView({
  widgetInfo,
  metricsValues,
}: RenderWidgetProps) {
  const widgetMetrics = JSON.parse(widgetInfo.metrics);
  const values = JSON.parse(widgetInfo.values);
  const datasets: any = [];

  const colors = [
    {
      backgroundColor: "rgba(19, 212, 90, 0.4)",
      borderColor: "rgb(78, 201, 78)",
    },
    {
      backgroundColor: "rgba(54, 162, 235, 0.4)",
      borderColor: "rgb(26, 46, 174)",
    },
    {
      backgroundColor: "rgba(255, 206, 86, 0.4)",
      borderColor: "rgba(255, 206, 86, 1)",
    },
    {
      backgroundColor: "rgba(219, 57, 28, 0.4)",
      borderColor: "rgb(215, 42, 19)",
    },
    {
      backgroundColor: "rgba(153, 102, 255, 0.4)",
      borderColor: "rgb(164, 23, 195)",
    },
    {
      backgroundColor: "rgba(46, 12, 39, 0.4)",
      borderColor: "rgb(126, 24, 55)",
    },
  ];

  let labels: string[] = [];

  values.forEach((valueObj: any, index: number) => {
    const expression = valueObj.value.replace(/^\$\{|\}$/g, "");
    const variables = extractVariables(expression);

    const metricName = widgetMetrics[index]?.name || `Metric ${index + 1}`;
    const metricData = metricsValues.find(
      (item: any) => item.name === metricName
    );

    if (
      !metricData ||
      !Array.isArray(metricData.data) ||
      metricData.data.length === 0
    ) {
      return;
    }

    if (labels.length === 0) {
      labels = metricData.data.map((entry: [number | null, number]) =>
        new Date(entry[1] * 1000).toLocaleTimeString()
      );
    }

    const cleanedData = metricData.data.map(
      ([value, timestamp]: [number | null, number]) => [value ?? 0, timestamp]
    );

    const data = cleanedData.map((entry: [number, number]) => {
      let evaluatedExpression = expression;

      for (let i = 0; i < variables.length; i++) {
        const variable = variables[i];
        const varMetricData = metricsValues.find(
          (item: any) => item.name === variable
        );
        const varMetricValue = varMetricData ? varMetricData?.data : [];
        const valueAtTime = varMetricValue?.find(
          (dataPoint: [number | null, number]) => dataPoint[1] === entry[1]
        );
        const valueToUse = valueAtTime ? valueAtTime[0] ?? 0 : 0;
        evaluatedExpression = evaluatedExpression.replace(
          new RegExp(`\\b${variable}\\b`, "g"),
          valueToUse
        );
      }

      try {
        return eval(evaluatedExpression);
      } catch (error) {
        console.log("Error evaluating expression");
        return 0;
      }
    });

    const colorIndex = index % colors.length;

    datasets.push({
      label: metricName,
      data,
      fill: false,
      backgroundColor: colors[colorIndex].backgroundColor,
      borderColor: colors[colorIndex].borderColor,
    });
  });

  const graphData = {
    labels,
    datasets,
  };

  const options = {
    scales: {
      x: {
        ticks: {
          display: false,
        },
        grid: {
          display: false,
        },
      },
      y: {
        beginAtZero: true,
      },
    },
    plugins: {
      legend: {
        display: false,
      },
    },
  };

  function extractVariables(expression: string) {
    const VARIABLE_REGEX = /[a-zA-Z_][\w\.]*/g;
    const matches = expression.match(VARIABLE_REGEX);
    return [...new Set(matches)];
  }

  if(isEmpty(graphData.datasets)){
    return <Alert className="mx-3 my-4" variant="info">No data available for the time period</Alert>;
  }

  return (
    <div>
      <Line data={graphData} options={options} />
    </div>
  );
}
