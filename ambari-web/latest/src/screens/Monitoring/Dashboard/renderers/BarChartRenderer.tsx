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
  BarElement,
  CategoryScale,
  Chart as ChartJs,
  Legend,
  LinearScale,
  Tooltip,
} from "chart.js";
import { Bar } from "react-chartjs-2";
import type { DashboardPanel } from "../../types";
import { formatMetricValue, getPanelDecimals, getPanelUnit } from "../../valueFormatter";
import { calculatePanelValue, panelCustomOptions, panelNumericBounds, type DashboardPanelResult } from "../data/panelData";

ChartJs.register(BarElement, CategoryScale, LinearScale, Legend, Tooltip);

const COLORS = ["#278541", "#1769aa", "#bd6418", "#8a4f9d", "#b33a3a", "#477178"];

interface BarChartRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
  height?: number;
}

export default function BarChartRenderer({ panel, results, height }: BarChartRendererProps) {
  const unit = getPanelUnit(panel.options);
  const decimals = getPanelDecimals(panel.options);
  const { min, max } = panelNumericBounds(panel);
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const horizontal = custom.orientation === "horizontal";
  const sortOrder = String(custom.sortOrder || "none");
  const items = results.map((result) => ({ result, value: calculatePanelValue(result, calculation) ?? 0 })).sort((left, right) => {
    if (sortOrder === "none") return 0;
    return sortOrder === "asc" ? left.value - right.value : right.value - left.value;
  });
  return (
    <div className="dashboard-chart-wrap" style={{ height: Math.max(180, height || 280) }}>
      <Bar
        data={{
          labels: items.map(({ result }) => result.displayName),
          datasets: [{
            label: panel.name || "Value",
            data: items.map(({ value }) => value),
            backgroundColor: items.map((_result, index) => COLORS[index % COLORS.length]),
            borderWidth: 0,
          }],
        }}
        options={{
          responsive: true,
          maintainAspectRatio: false,
          indexAxis: horizontal ? "y" : "x",
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: (context) => formatMetricValue(horizontal ? context.parsed.x : context.parsed.y, unit, decimals),
              },
            },
          },
          scales: {
            x: horizontal ? { min, max, ticks: { callback: (value) => formatMetricValue(value, unit, decimals) } } : { ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 12 } },
            y: horizontal ? { ticks: { autoSkip: true, maxTicksLimit: 12 } } : { min, max, ticks: { callback: (value) => formatMetricValue(value, unit, decimals) } },
          },
        }}
      />
    </div>
  );
}
