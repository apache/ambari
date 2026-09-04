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
import PrometheusChart from "../../PrometheusChart";
import { getPanelDecimals, getPanelUnit } from "../../valueFormatter";
import { panelCustomOptions, panelNumericBounds, panelThresholds, type DashboardPanelResult } from "../data/panelData";

interface TimeSeriesRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
  height?: number;
  graphTooltip?: string;
}

export default function TimeSeriesRenderer({ panel, results, height, graphTooltip }: TimeSeriesRendererProps) {
  const { min, max } = panelNumericBounds(panel);
  const custom = panelCustomOptions(panel);
  const scaleDistribution = custom.scaleDistribution && typeof custom.scaleDistribution === "object"
    ? custom.scaleDistribution as Record<string, unknown>
    : {};
  const options = panel.options || {};
  const legend = options.legend && typeof options.legend === "object" ? options.legend as Record<string, unknown> : {};
  const tooltip = options.tooltip && typeof options.tooltip === "object" ? options.tooltip as Record<string, unknown> : {};
  return (
    <PrometheusChart
      results={results}
      unit={getPanelUnit(panel.options)}
      decimals={getPanelDecimals(panel.options)}
      minimum={min}
      maximum={max}
      tooltipMode={String(tooltip.mode || graphTooltip || "all") === "single" ? "single" : "shared"}
      tooltipSort={String(tooltip.sort || "none")}
      drawStyle={String(custom.drawStyle || "lines")}
      lineInterpolation={String(custom.lineInterpolation || "smooth")}
      lineWidth={typeof custom.lineWidth === "number" ? custom.lineWidth : 2}
      fillOpacity={typeof custom.fillOpacity === "number" ? custom.fillOpacity : 0}
      stack={custom.stack === "normal"}
      scaleType={String(scaleDistribution.type || "linear")}
      showPoints={custom.showPoints === "always"}
      pointSize={typeof custom.pointSize === "number" ? custom.pointSize : 4}
      spanNulls={custom.spanNulls !== false}
      legendDisplay={legend.displayMode !== "hidden"}
      legendMode={String(legend.displayMode || "list")}
      legendColumns={Array.isArray(legend.columns) ? legend.columns.filter((column): column is string => typeof column === "string") : []}
      legendPlacement={String(legend.placement || "bottom") as "top" | "left" | "right" | "bottom"}
      barWidthFactor={typeof custom.barWidthFactor === "number" ? custom.barWidthFactor : 0.6}
      thresholds={panelThresholds(panel).filter((threshold) => threshold.value !== null)}
      height={height}
    />
  );
}
