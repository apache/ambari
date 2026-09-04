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
  panelNumericBounds,
  panelValueColor,
  panelValueText,
  type DashboardPanelResult,
} from "../data/panelData";

interface GaugeRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

const clamp = (value: number) => Math.min(1, Math.max(0, value));

export default function GaugeRenderer({ panel, results }: GaugeRendererProps) {
  const unit = getPanelUnit(panel.options);
  const decimals = getPanelDecimals(panel.options);
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const textMode = String(custom.textMode || "valueAndName");
  const values = results.map((result) => calculatePanelValue(result, calculation));
  const bounds = panelNumericBounds(panel);
  const min = bounds.min ?? 0;
  const max = bounds.max ?? Math.max(1, ...values.map((value) => value ?? 0));
  const range = max > min ? max - min : 1;

  return (
    <div className="dashboard-gauge-grid">
      {results.map((result, index) => {
        const value = values[index];
        const ratio = value === null ? 0 : clamp((value - min) / range);
        const color = panelValueColor(panel, value) || "#278541";
        const dash = `${ratio * 301.6} 301.6`;
        return (
          <div className="dashboard-gauge-item" key={result.seriesKey}>
            <svg viewBox="0 0 120 120" role="meter" aria-label={result.displayName}
              aria-valuemin={min} aria-valuemax={max} aria-valuenow={value ?? undefined}>
              <circle className="dashboard-gauge-track" cx="60" cy="60" r="48" />
              <circle className="dashboard-gauge-value" cx="60" cy="60" r="48"
                stroke={color} strokeDasharray={dash} />
            </svg>
            {textMode !== "name" && <strong style={{ color }}>{panelValueText(panel, value) || formatMetricValue(value, unit, decimals)}</strong>}
            {textMode !== "value" && <span title={result.displayName}>{result.displayName}</span>}
          </div>
        );
      })}
    </div>
  );
}
