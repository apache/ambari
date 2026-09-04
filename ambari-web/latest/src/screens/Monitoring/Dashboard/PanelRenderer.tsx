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

import type { ComponentType } from "react";
import type { DashboardPanel } from "../types";
import DashboardRow from "./DashboardRow";
import type { DashboardPanelResult } from "./data/panelData";
import BarChartRenderer from "./renderers/BarChartRenderer";
import BarGaugeRenderer from "./renderers/BarGaugeRenderer";
import GaugeRenderer from "./renderers/GaugeRenderer";
import HeatmapRenderer from "./renderers/HeatmapRenderer";
import HexbinRenderer from "./renderers/HexbinRenderer";
import IframeRenderer from "./renderers/IframeRenderer";
import PieRenderer from "./renderers/PieRenderer";
import StatRenderer from "./renderers/StatRenderer";
import TableRenderer from "./renderers/TableRenderer";
import TextRenderer from "./renderers/TextRenderer";
import TimeSeriesRenderer from "./renderers/TimeSeriesRenderer";

interface RendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
  height?: number;
  graphTooltip?: string;
}

type Renderer = ComponentType<RendererProps>;

const rendererRegistry: Record<string, Renderer> = {
  timeseries: TimeSeriesRenderer,
  stat: StatRenderer,
  gauge: GaugeRenderer,
  barGauge: BarGaugeRenderer,
  table: TableRenderer,
  tableNG: TableRenderer,
  pie: PieRenderer,
  barchart: BarChartRenderer,
  heatmap: HeatmapRenderer,
  hexbin: HexbinRenderer,
  text: TextRenderer,
  iframe: IframeRenderer,
};

export default function PanelRenderer({ panel, results, height, graphTooltip }: RendererProps) {
  if (panel.type === "row") return <DashboardRow panel={panel} />;
  const Renderer = rendererRegistry[panel.type];
  if (!Renderer) {
    return <div className="monitoring-empty">Unsupported panel type: {panel.type}</div>;
  }
  return <Renderer panel={panel} results={results} height={height} graphTooltip={graphTooltip} />;
}

export { rendererRegistry };
