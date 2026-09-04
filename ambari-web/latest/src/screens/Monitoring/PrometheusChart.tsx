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
  BarElement,
  Chart as ChartJs,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  LogarithmicScale,
  PointElement,
  TimeScale,
  Title,
  Tooltip,
} from "chart.js";
import { Bar, Line } from "react-chartjs-2";
import { PrometheusResult } from "./types";
import { formatMetricValue } from "./valueFormatter";

ChartJs.register(CategoryScale, LinearScale, LogarithmicScale, PointElement, LineElement, BarElement, Filler, TimeScale, Title, Tooltip, Legend);

const COLORS = ["#278541", "#1769aa", "#bd6418", "#8a4f9d", "#b33a3a", "#477178"];

const colorWithOpacity = (color: string, opacity: number) => {
  const match = color.match(/^#([0-9a-f]{6})$/i);
  if (!match) return color;
  const channels = [0, 2, 4].map((offset) => Number.parseInt(match[1].slice(offset, offset + 2), 16));
  return `rgba(${channels.join(", ")}, ${Math.min(1, Math.max(0, opacity))})`;
};

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
  decimals,
  minimum,
  maximum,
  tooltipMode = "shared",
  tooltipSort = "none",
  drawStyle = "lines",
  lineInterpolation = "smooth",
  lineWidth = 2,
  fillOpacity = 0,
  stack = false,
  scaleType = "linear",
  showPoints = false,
  pointSize = 4,
  spanNulls = true,
  legendDisplay = true,
  legendPlacement = "bottom",
  barWidthFactor = 0.6,
  height,
}: {
  results: DisplayResult[];
  unit?: string;
  decimals?: number;
  minimum?: number;
  maximum?: number;
  tooltipMode?: string;
  tooltipSort?: string;
  drawStyle?: string;
  lineInterpolation?: string;
  lineWidth?: number;
  fillOpacity?: number;
  stack?: boolean;
  scaleType?: string;
  showPoints?: boolean;
  pointSize?: number;
  spanNulls?: boolean;
  legendDisplay?: boolean;
  legendPlacement?: "top" | "left" | "right" | "bottom";
  barWidthFactor?: number;
  height?: number;
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
      data: timestamps.map((timestamp) => ({
        x: timestamp * 1000,
        y: points.get(timestamp) ?? null,
      })),
      borderColor: COLORS[index % COLORS.length],
      backgroundColor: colorWithOpacity(COLORS[index % COLORS.length], fillOpacity),
      pointRadius: showPoints || timestamps.length <= 1 ? pointSize : 0,
      pointHoverRadius: Math.max(pointSize, 4),
      tension: lineInterpolation === "smooth" ? 0.28 : 0,
      borderWidth: lineWidth,
      fill: fillOpacity > 0,
      spanGaps: spanNulls,
      stack: stack ? "dashboard" : undefined,
    };
  });

  const range = timestamps.length > 1 ? timestamps[timestamps.length - 1] - timestamps[0] : 0;
  const formatTimestamp = (timestamp: number) => new Date(timestamp).toLocaleString([], {
    month: range > 2 * 24 * 60 * 60 ? "short" : undefined,
    day: range > 2 * 24 * 60 * 60 ? "2-digit" : undefined,
    hour: "2-digit",
    minute: "2-digit",
  });

  const legendPosition = ["top", "left", "right", "bottom"].includes(legendPlacement)
    ? legendPlacement
    : "bottom";
  const tooltipValue = (item: unknown) => Number((item as { parsed?: { y?: number } }).parsed?.y || 0);
  const itemSort = tooltipSort === "asc"
    ? (left: unknown, right: unknown) => tooltipValue(left) - tooltipValue(right)
    : tooltipSort === "desc"
      ? (left: unknown, right: unknown) => tooltipValue(right) - tooltipValue(left)
      : undefined;

  if (drawStyle === "bars") {
    return (
      <div className="dashboard-chart-wrap" style={{ height: Math.max(180, height || 300) }}>
        <Bar
          data={{
            labels: timestamps.map((timestamp) => formatTimestamp(timestamp * 1000)),
            datasets: results.map((result, index) => {
              const points = new Map((result.values || (result.value ? [result.value] : [])).map(
                ([timestamp, value]) => [timestamp, Number(value)],
              ));
              return {
                label: seriesName(result, index),
                data: timestamps.map((timestamp) => points.get(timestamp) ?? null),
                backgroundColor: COLORS[index % COLORS.length],
                borderWidth: 0,
                barPercentage: barWidthFactor,
                stack: stack ? "dashboard" : undefined,
              };
            }),
          }}
          options={{
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: tooltipMode === "single" ? "nearest" : "index", intersect: false },
            plugins: {
              legend: { display: legendDisplay, position: legendPosition, labels: { boxWidth: 12 } },
              tooltip: {
                itemSort,
                callbacks: { label: (context) => `${context.dataset.label || "Series"}: ${formatMetricValue(context.parsed.y, unit, decimals)}` },
              },
            },
            scales: {
              x: { stacked: stack, ticks: { maxTicksLimit: 8, maxRotation: 0 } },
              y: { type: scaleType === "log" ? "logarithmic" : "linear", stacked: stack, min: minimum, max: maximum, ticks: { callback: (value) => formatMetricValue(value, unit, decimals) } },
            },
          }}
        />
      </div>
    );
  }

  return (
    <div className="dashboard-chart-wrap" style={{ height: Math.max(180, height || 300) }}>
      <Line
        data={{
          datasets,
        }}
        options={{
          responsive: true,
          maintainAspectRatio: false,
          interaction: { mode: tooltipMode === "single" ? "nearest" : "index", intersect: false },
          plugins: {
            legend: { display: legendDisplay, position: legendPosition, labels: { boxWidth: 12 } },
            tooltip: {
              itemSort,
              callbacks: {
                title: (items) => items.length ? formatTimestamp(Number(items[0].parsed.x)) : "",
                label: (context) => {
                  const label = context.dataset.label ? `${context.dataset.label}: ` : "";
                  return `${label}${formatMetricValue(context.parsed.y, unit, decimals)}`;
                },
              },
            },
          },
          scales: {
            x: {
              type: "linear",
              ticks: {
                maxTicksLimit: 8,
                callback: (value) => formatTimestamp(Number(value)),
              },
            },
            y: {
              type: scaleType === "log" ? "logarithmic" : "linear",
              stacked: stack,
              min: minimum,
              max: maximum,
              ticks: {
                callback: (value) => formatMetricValue(value, unit, decimals),
              },
            },
          },
        }}
      />
    </div>
  );
}
