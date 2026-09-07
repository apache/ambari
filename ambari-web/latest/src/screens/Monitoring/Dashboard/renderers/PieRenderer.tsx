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

import { ArcElement, Chart as ChartJs, Legend, Tooltip } from "chart.js";
import { Doughnut, Pie } from "react-chartjs-2";
import type { DashboardPanel } from "../../types";
import { formatMetricValue, getPanelDecimals, getPanelUnit } from "../../valueFormatter";
import { calculatePanelValue, panelCustomOptions, type DashboardPanelResult } from "../data/panelData";

ChartJs.register(ArcElement, Legend, Tooltip);

const COLORS = ["#278541", "#1769aa", "#bd6418", "#8a4f9d", "#b33a3a", "#477178"];

interface PieRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
  height?: number;
}

export default function PieRenderer({ panel, results, height }: PieRendererProps) {
  const unit = getPanelUnit(panel.options);
  const decimals = getPanelDecimals(panel.options);
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const displayMode = String(custom.displayMode || "pie");
  const position = String(custom.legendPosition || "right");
  const legendPosition = ["top", "left", "right", "bottom"].includes(position)
    ? position as "top" | "left" | "right" | "bottom"
    : "right";
  const data = {
    labels: results.map((result) => result.displayName),
    datasets: [{
      data: results.map((result) => calculatePanelValue(result, calculation) ?? 0),
      backgroundColor: results.map((_result, index) => COLORS[index % COLORS.length]),
      borderWidth: 1,
    }],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: legendPosition },
      tooltip: {
        callbacks: {
          label: (context: { label?: string; parsed: number }) => `${context.label || "Series"}: ${formatMetricValue(context.parsed, unit, decimals)}`,
        },
      },
    },
  };
  return (
    <div className="dashboard-chart-wrap" style={{ height: Math.max(180, height || 280) }}>
      {displayMode === "donut" ? <Doughnut data={data} options={options} /> : <Pie data={data} options={options} />}
    </div>
  );
}
