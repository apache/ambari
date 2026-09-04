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

import type { Layout } from "react-grid-layout";
import {
  DASHBOARD_PANEL_TYPES,
  type DashboardPanel,
  type DashboardPanelType,
  type DashboardPayload,
} from "../types";

export const PANEL_TYPE_LABELS: Record<DashboardPanelType, string> = {
  row: "Section row",
  timeseries: "Time series",
  stat: "Stat",
  gauge: "Gauge",
  barGauge: "Bar gauge",
  table: "Table",
  tableNG: "Advanced table",
  pie: "Pie chart",
  barchart: "Bar chart",
  heatmap: "Heatmap",
  hexbin: "Hex tiles",
  text: "Text",
  iframe: "Embedded page",
};

export const PANEL_TYPE_OPTIONS = DASHBOARD_PANEL_TYPES.map((value) => ({
  value,
  label: PANEL_TYPE_LABELS[value],
}));

export const STATIC_PANEL_TYPES = new Set<DashboardPanelType>(["row", "text", "iframe"]);

const newPanelId = () => {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return `panel-${crypto.randomUUID()}`;
  }
  return `panel-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
};

const nextRow = (panels: DashboardPanel[]) => panels.reduce(
  (maximum, panel) => Math.max(maximum, panel.layout.y + panel.layout.h),
  0,
);

export const cloneDashboardPayload = (payload: DashboardPayload): DashboardPayload => (
  JSON.parse(JSON.stringify(payload)) as DashboardPayload
);

export function createDashboardPanel(
  type: DashboardPanelType,
  panels: DashboardPanel[],
  datasourceId?: number,
): DashboardPanel {
  const id = newPanelId();
  const isRow = type === "row";
  const isStatic = STATIC_PANEL_TYPES.has(type);
  return {
    id,
    name: PANEL_TYPE_LABELS[type],
    type,
    datasourceCate: isStatic ? undefined : "prometheus",
    datasourceValue: isStatic ? undefined : datasourceId,
    targets: isStatic ? [] : [{
      refId: "A",
      expr: 'up{cluster="$cluster"}',
      legend: "{{instance}}",
      instant: type !== "timeseries" && type !== "heatmap",
    }],
    layout: {
      i: id,
      x: 0,
      y: nextRow(panels),
      w: isRow ? 24 : 12,
      h: isRow ? 1 : type === "stat" ? 3 : 5,
      isResizable: !isRow,
    },
    custom: type === "text" ? {
      content: "", textColor: "#25313d", bgColor: "#ffffff", textSize: 14,
      justifyContent: "center", alignItems: "center",
    } : type === "iframe" ? { url: "" } : type === "timeseries" ? {
      drawStyle: "lines", lineInterpolation: "smooth", lineWidth: 2, fillOpacity: 0.08,
      stack: "off", scaleDistribution: { type: "linear" }, showPoints: "none", pointSize: 4,
    } : type === "stat" ? {
      textMode: "valueAndName", colorMode: "value", calc: "lastNotNull", orientation: "auto", graphMode: "none",
    } : type === "barGauge" ? {
      displayMode: "basic", calc: "lastNotNull", sortOrder: "desc", valueMode: "color",
    } : type === "pie" ? {
      calc: "lastNotNull", displayMode: "pie", legendPosition: "right",
    } : type === "tableNG" ? {
      showHeader: true, filterable: true, cellOptions: { type: "gauge", mode: "basic", valueDisplayMode: "text", wrapText: false },
    } : type === "hexbin" ? {
      textMode: "valueAndName", calc: "lastNotNull", colorRange: ["#dbeafe", "#38bdf8", "#075985"], reverseColorOrder: false,
    } : type === "gauge" ? {
      textMode: "valueAndName", calc: "lastNotNull",
    } : type === "heatmap" ? {
      scheme: "blueGreen",
    } : type === "barchart" ? {
      calc: "lastNotNull", orientation: "vertical", sortOrder: "none",
    } : undefined,
    options: isStatic ? undefined : {
      standardOptions: { util: "none", decimals: 2 },
      legend: { displayMode: type === "timeseries" ? "list" : "hidden", placement: "bottom" },
      tooltip: { mode: "all", sort: "none" },
      thresholds: {
        mode: "absolute",
        steps: [
          { value: null, color: "green" },
          { value: 80, color: "red" },
        ],
      },
    },
  };
}

export function duplicateDashboardPanel(panel: DashboardPanel, panels: DashboardPanel[]): DashboardPanel {
  const copy = JSON.parse(JSON.stringify(panel)) as DashboardPanel;
  const id = newPanelId();
  copy.id = id;
  copy.name = `${panel.name} copy`;
  copy.layout = {
    ...copy.layout,
    i: id,
    x: 0,
    y: nextRow(panels),
  };
  return copy;
}

export function applyDashboardLayout(panels: DashboardPanel[], layout: Layout[]): DashboardPanel[] {
  const positions = new Map(layout.map((item) => [item.i, item]));
  return panels.map((panel) => {
    const next = positions.get(panel.id);
    if (!next) return panel;
    return {
      ...panel,
      layout: {
        i: panel.id,
        x: panel.type === "row" ? 0 : next.x,
        y: next.y,
        w: panel.type === "row" ? 24 : next.w,
        h: panel.type === "row" ? 1 : next.h,
        isResizable: panel.type !== "row",
      },
    };
  });
}
