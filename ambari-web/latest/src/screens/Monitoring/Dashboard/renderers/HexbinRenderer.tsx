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
  type DashboardPanelResult,
} from "../data/panelData";

interface HexbinRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

const asColors = (value: unknown) => Array.isArray(value)
  ? value.filter((item): item is string => typeof item === "string" && item.length > 0)
  : [];

const hexToRgb = (color: string) => {
  const match = color.match(/^#([0-9a-f]{6})$/i);
  if (!match) return [39, 133, 65];
  return [0, 2, 4].map((offset) => Number.parseInt(match[1].slice(offset, offset + 2), 16));
};

const interpolateColor = (colors: string[], ratio: number) => {
  if (colors.length === 0) return "#278541";
  if (colors.length === 1) return colors[0];
  const position = Math.min(1, Math.max(0, ratio)) * (colors.length - 1);
  const index = Math.min(colors.length - 2, Math.floor(position));
  const offset = position - index;
  const left = hexToRgb(colors[index]);
  const right = hexToRgb(colors[index + 1]);
  return `rgb(${left.map((channel, channelIndex) => Math.round(channel + (right[channelIndex] - channel) * offset)).join(", ")})`;
};

export default function HexbinRenderer({ panel, results }: HexbinRendererProps) {
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const values = results.map((result) => ({ result, value: calculatePanelValue(result, calculation) }));
  const numeric = values.map((item) => item.value).filter((value): value is number => value !== null);
  const minimum = numeric.length ? Math.min(...numeric) : 0;
  const maximum = numeric.length ? Math.max(...numeric) : 1;
  const palette = asColors(custom.colorRange).length
    ? asColors(custom.colorRange)
    : ["#dbeafe", "#38bdf8", "#075985"];
  const colors = custom.reverseColorOrder ? [...palette].reverse() : palette;
  const textMode = String(custom.textMode || "valueAndName");
  const unit = getPanelUnit(panel.options);
  const decimals = getPanelDecimals(panel.options);

  return (
    <div className="dashboard-hexbin-grid">
      {values.map(({ result, value }) => {
        const ratio = value === null || maximum === minimum ? 0.5 : (value - minimum) / (maximum - minimum);
        const valueText = formatMetricValue(value, unit, decimals);
        return <div className="dashboard-hexbin-item" key={result.seriesKey} style={{ backgroundColor: interpolateColor(colors, ratio) }} title={`${result.displayName}: ${valueText}`}>
          {textMode !== "name" && <strong>{valueText}</strong>}
          {textMode !== "value" && <span>{result.displayName}</span>}
        </div>;
      })}
    </div>
  );
}
