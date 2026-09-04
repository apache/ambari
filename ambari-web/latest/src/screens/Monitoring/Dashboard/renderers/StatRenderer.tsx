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
  panelValueColor,
  type DashboardPanelResult,
} from "../data/panelData";

interface StatRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

export default function StatRenderer({ panel, results }: StatRendererProps) {
  const unit = getPanelUnit(panel.options);
  return (
    <div className="dashboard-stat-grid dashboard-stat-grid-ambari">
      {results.map((result) => {
        const value = latestPanelValue(result);
        const color = panelValueColor(panel, value);
        return (
          <div className="dashboard-stat-item" key={result.seriesKey}>
            <strong style={color ? { color } : undefined}>{formatMetricValue(value, unit)}</strong>
            <span title={result.displayName}>{result.displayName}</span>
          </div>
        );
      })}
    </div>
  );
}
