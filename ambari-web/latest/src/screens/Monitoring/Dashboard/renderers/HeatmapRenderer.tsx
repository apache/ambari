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

import type { DashboardPanel } from "../../types";
import { formatMetricValue, getPanelUnit } from "../../valueFormatter";
import { type DashboardPanelResult } from "../data/panelData";

interface HeatmapRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

const heatColor = (value: number, minimum: number, maximum: number) => {
  const ratio = maximum > minimum ? (value - minimum) / (maximum - minimum) : 0.5;
  return `rgba(39, 133, 65, ${0.12 + Math.min(1, Math.max(0, ratio)) * 0.78})`;
};

export default function HeatmapRenderer({ panel, results }: HeatmapRendererProps) {
  const unit = getPanelUnit(panel.options);
  const timestamps = Array.from(new Set(results.flatMap((result) => (
    result.values || (result.value ? [result.value] : [])
  ).map(([timestamp]) => timestamp)))).sort((left, right) => left - right);
  const values = results.flatMap((result) => (
    result.values || (result.value ? [result.value] : [])
  ).map(([, value]) => Number(value)).filter(Number.isFinite));
  const minimum = values.length ? Math.min(...values) : 0;
  const maximum = values.length ? Math.max(...values) : 1;

  return (
    <div className="dashboard-heatmap-wrap">
      <div className="dashboard-heatmap" style={{ gridTemplateColumns: `minmax(140px, 1.2fr) repeat(${Math.max(1, timestamps.length)}, minmax(42px, 1fr))` }}>
        <div className="dashboard-heatmap-corner" />
        {timestamps.map((timestamp) => <div className="dashboard-heatmap-axis" key={timestamp}>{new Date(timestamp * 1000).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}</div>)}
        {results.map((result) => {
          const points = new Map((result.values || (result.value ? [result.value] : [])).map(([timestamp, value]) => [timestamp, Number(value)]));
          return (
            <div className="dashboard-heatmap-row" key={result.seriesKey}>
              <span className="dashboard-heatmap-label" title={result.displayName}>{result.displayName}</span>
              {timestamps.map((timestamp) => {
                const value = points.get(timestamp);
                return <span className="dashboard-heatmap-cell" key={`${result.seriesKey}-${timestamp}`} style={{ backgroundColor: value === undefined ? undefined : heatColor(value, minimum, maximum) }} title={value === undefined ? "No data" : formatMetricValue(value, unit)} />;
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
