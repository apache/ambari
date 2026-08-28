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

import {
  CategoryScale,
  Chart as ChartJs,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  TimeScale,
  Title,
  Tooltip,
} from "chart.js";
import { Line } from "react-chartjs-2";
import { PrometheusResult } from "./types";
import { formatMetricValue } from "./valueFormatter";

ChartJs.register(CategoryScale, LinearScale, PointElement, LineElement, TimeScale, Title, Tooltip, Legend);

const COLORS = ["#278541", "#1769aa", "#bd6418", "#8a4f9d", "#b33a3a", "#477178"];

type DisplayResult = PrometheusResult & { displayName?: string };

const seriesName = (result: DisplayResult, index: number) => {
  if (result.displayName) return result.displayName;
  const metricName = result.metric.__name__ || `Series ${index + 1}`;
  const labels = Object.entries(result.metric)
    .filter(([name]) => name !== "__name__")
    .map(([name, value]) => `${name}=${value}`)
    .join(", ");
  return labels ? `${metricName} {${labels}}` : metricName;
};

export default function PrometheusChart({
  results,
  unit = "",
}: {
  results: DisplayResult[];
  unit?: string;
}) {
  const timestampSet = new Set<number>();
  results.forEach((result) => {
    (result.values || (result.value ? [result.value] : [])).forEach(([timestamp]) => timestampSet.add(timestamp));
  });
  const timestamps = Array.from(timestampSet).sort((left, right) => left - right);
  const datasets = results.map((result, index) => {
    const points = new Map((result.values || (result.value ? [result.value] : [])).map(
      ([timestamp, value]) => [timestamp, Number(value)],
    ));
    return {
      label: seriesName(result, index),
      data: timestamps.map((timestamp) => points.get(timestamp) ?? null),
      borderColor: COLORS[index % COLORS.length],
      backgroundColor: COLORS[index % COLORS.length],
      borderWidth: 1.5,
      pointRadius: timestamps.length > 1 ? 0 : 3,
      tension: 0.15,
      spanGaps: true,
    };
  });

  return (
    <div style={{ minHeight: 320, height: 420 }}>
      <Line
        data={{
          labels: timestamps.map((timestamp) => new Date(timestamp * 1000).toLocaleString()),
          datasets,
        }}
        options={{
          responsive: true,
          maintainAspectRatio: false,
          interaction: { mode: "index", intersect: false },
          plugins: {
            legend: { position: "bottom", labels: { boxWidth: 12 } },
            tooltip: {
              callbacks: {
                label: (context) => {
                  const label = context.dataset.label ? `${context.dataset.label}: ` : "";
                  return `${label}${formatMetricValue(context.parsed.y, unit)}`;
                },
              },
            },
          },
          scales: {
            x: { ticks: { maxTicksLimit: 8 } },
            y: {
              ticks: {
                callback: (value) => formatMetricValue(value, unit),
              },
            },
          },
        }}
      />
    </div>
  );
}
