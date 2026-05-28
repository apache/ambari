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

import { Doughnut } from "react-chartjs-2";
import ChartContainer from "../Dashboard/ChartContainer";
import { WidgetInfo } from "./type";
import { isEmpty } from "lodash";

type GaugeWidgetViewProps = {
  widgetInfo: WidgetInfo;
  metricsValues: any;
};

export default function GaugeWidgetView({
  widgetInfo,
  metricsValues,
}: GaugeWidgetViewProps) {
  const values = JSON.parse(widgetInfo.values)[0];
  const expression = values.value.replace(/^\$\{|\}$/g, "");
  const variables = extractVariables(expression);

  let evaluatedExpression = expression;

  for (let i = 0; i < variables.length; i++) {
    const variable = variables[i];
    const metricData = metricsValues.find(
      (item: any) => item.name === variable
    );
    const metricValue = metricData ? metricData.data : 0;
    evaluatedExpression = evaluatedExpression.replace(
      new RegExp(`\\b${variable}\\b`, "g"),
      metricValue
    );
  }

  let result;
  try {
    result = eval(evaluatedExpression);
  } catch (error) {
    console.error("Error evaluating expression:", error);
    result = "Error";
  }

  function extractVariables(expression: string) {
    const VARIABLE_REGEX = /[a-zA-Z_][\w\.]*/g;
    const matches = expression.match(VARIABLE_REGEX);
    return [...new Set(matches)];
  }

  const percentage = result * 100;

  const data = {
    datasets: [
      {
        data: [percentage, 100 - percentage],
        backgroundColor: ["#429929", "#D3D3D3"],
      },
    ],
    labels: ["Used", "Free"],
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
  };

  return (
    <>
      <ChartContainer text={`${Math.round(percentage)}%`}>
        <div className="d-flex justify-content-center mh-100">
          {!isEmpty(data) && <Doughnut data={data} options={options} />}
        </div>
      </ChartContainer>
    </>
  );
}
