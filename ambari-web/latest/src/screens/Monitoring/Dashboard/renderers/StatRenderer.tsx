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
import { formatMetricValue, getPanelDecimals, getPanelUnit } from "../../valueFormatter";
import {
  calculatePanelValue,
  panelCustomOptions,
  panelValueColor,
  panelValueText,
  type DashboardPanelResult,
} from "../data/panelData";

interface StatRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

export default function StatRenderer({ panel, results }: StatRendererProps) {
  const unit = getPanelUnit(panel.options);
  const decimals = getPanelDecimals(panel.options);
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const textMode = String(custom.textMode || "valueAndName");
  const colorMode = String(custom.colorMode || "value");
  const orientation = String(custom.orientation || "auto");
  const graphMode = String(custom.graphMode || "none");
  const textSize = custom.textSize && typeof custom.textSize === "object" ? custom.textSize as Record<string, unknown> : {};
  return (
    <div className={`dashboard-stat-grid dashboard-stat-grid-ambari dashboard-stat-${orientation}`}>
      {results.map((result) => {
        const value = calculatePanelValue(result, calculation);
        const color = panelValueColor(panel, value);
        const points = (result.values || []).map(([, item]) => Number(item)).filter(Number.isFinite);
        const minimum = points.length ? Math.min(...points) : 0;
        const maximum = points.length ? Math.max(...points) : 1;
        const polyline = points.map((point, index) => {
          const x = points.length <= 1 ? 0 : index * 100 / (points.length - 1);
          const y = maximum === minimum ? 50 : 92 - (point - minimum) * 84 / (maximum - minimum);
          return `${x},${y}`;
        }).join(" ");
        return (
          <div className={`dashboard-stat-item ${colorMode === "background" ? "dashboard-stat-background" : ""}`} key={result.seriesKey} style={colorMode === "background" && color ? { backgroundColor: color } : undefined}>
            {graphMode === "area" && points.length > 1 && <svg className="dashboard-stat-sparkline" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><polyline points={polyline} /></svg>}
            {textMode !== "name" && <strong style={{ color: colorMode === "value" ? color : undefined, fontSize: typeof textSize.value === "number" ? textSize.value : undefined }}>{panelValueText(panel, value) || formatMetricValue(value, unit, decimals)}</strong>}
            {textMode !== "value" && <span title={result.displayName} style={{ fontSize: typeof textSize.title === "number" ? textSize.title : undefined }}>{result.displayName}</span>}
          </div>
        );
      })}
    </div>
  );
}
