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
import {
  latestPanelValue,
  panelNumericBounds,
  panelValueColor,
  type DashboardPanelResult,
} from "../data/panelData";

interface BarGaugeRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

const clamp = (value: number) => Math.min(1, Math.max(0, value));

export default function BarGaugeRenderer({ panel, results }: BarGaugeRendererProps) {
  const unit = getPanelUnit(panel.options);
  const { min = 0, max = 1 } = panelNumericBounds(panel);
  const range = max > min ? max - min : 1;

  return (
    <div className="dashboard-bar-gauge-list">
      {results.map((result) => {
        const value = latestPanelValue(result);
        const ratio = value === null ? 0 : clamp((value - min) / range);
        const color = panelValueColor(panel, value) || "#1769aa";
        return (
          <div className="dashboard-bar-gauge-item" key={result.seriesKey}>
            <div className="dashboard-bar-gauge-label">
              <span title={result.displayName}>{result.displayName}</span>
              <strong style={{ color }}>{formatMetricValue(value, unit)}</strong>
            </div>
            <div className="dashboard-bar-gauge-track" role="meter" aria-label={result.displayName}
              aria-valuemin={min} aria-valuemax={max} aria-valuenow={value ?? undefined}>
              <span style={{ width: `${ratio * 100}%`, backgroundColor: color }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}
