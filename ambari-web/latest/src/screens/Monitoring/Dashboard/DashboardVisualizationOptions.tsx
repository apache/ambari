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

import { Form } from "react-bootstrap";
import type { DashboardPanel, JsonObject } from "../types";

interface DashboardVisualizationOptionsProps {
  panel: DashboardPanel;
  onChange: (panel: DashboardPanel) => void;
}

const CALCULATIONS = [
  ["lastNotNull", "Last"], ["firstNotNull", "First"], ["min", "Minimum"], ["max", "Maximum"],
  ["avg", "Average"], ["sum", "Sum"], ["count", "Count"],
] as const;

const asObject = (value: unknown): JsonObject => value && typeof value === "object" && !Array.isArray(value)
  ? value as JsonObject
  : {};

const OptionalNumber = ({ label, value, onChange, min = 0, max }: {
  label: string;
  value: unknown;
  onChange: (value: number | undefined) => void;
  min?: number;
  max?: number;
}) => <Form.Group><Form.Label>{label}</Form.Label><Form.Control size="sm" type="number" min={min} max={max} value={typeof value === "number" ? value : ""} onChange={(event) => onChange(event.target.value === "" ? undefined : Number(event.target.value))} /></Form.Group>;

const Calculation = ({ value, onChange }: { value: unknown; onChange: (value: string) => void }) => <Form.Group><Form.Label>Calculation</Form.Label><Form.Select size="sm" value={String(value || "lastNotNull")} onChange={(event) => onChange(event.target.value)}>{CALCULATIONS.map(([key, label]) => <option key={key} value={key}>{label}</option>)}</Form.Select></Form.Group>;

export default function DashboardVisualizationOptions({ panel, onChange }: DashboardVisualizationOptionsProps) {
  const custom = asObject(panel.custom);
  const options = asObject(panel.options);
  const legend = asObject(options.legend);
  const tooltip = asObject(options.tooltip);
  const cellOptions = asObject(custom.cellOptions);

  const setCustom = (name: string, value: unknown) => onChange({ ...panel, custom: { ...custom, [name]: value } });
  const setOptions = (name: string, value: unknown) => onChange({ ...panel, options: { ...options, [name]: value } });

  if (panel.type === "timeseries") {
    const scale = asObject(custom.scaleDistribution);
    return <section className="dashboard-editor-section">
      <div className="dashboard-editor-section-title">Time series style</div>
      <div className="dashboard-option-grid">
        <Form.Group><Form.Label>Draw style</Form.Label><Form.Select size="sm" value={String(custom.drawStyle || "lines")} onChange={(event) => setCustom("drawStyle", event.target.value)}><option value="lines">Lines</option><option value="bars">Bars</option></Form.Select></Form.Group>
        {custom.drawStyle !== "bars" && <Form.Group><Form.Label>Interpolation</Form.Label><Form.Select size="sm" value={String(custom.lineInterpolation || "smooth")} onChange={(event) => setCustom("lineInterpolation", event.target.value)}><option value="smooth">Smooth</option><option value="linear">Linear</option></Form.Select></Form.Group>}
        <OptionalNumber label="Line width" min={0} max={10} value={custom.lineWidth} onChange={(value) => setCustom("lineWidth", value)} />
        <Form.Group><Form.Label>Fill opacity</Form.Label><Form.Control size="sm" type="number" min={0} max={1} step={0.05} value={typeof custom.fillOpacity === "number" ? custom.fillOpacity : 0} onChange={(event) => setCustom("fillOpacity", Number(event.target.value))} /></Form.Group>
        <Form.Group><Form.Label>Y-axis scale</Form.Label><Form.Select size="sm" value={String(scale.type || "linear")} onChange={(event) => setCustom("scaleDistribution", { ...scale, type: event.target.value })}><option value="linear">Linear</option><option value="log">Logarithmic</option></Form.Select></Form.Group>
        <Form.Group><Form.Label>Legend</Form.Label><Form.Select size="sm" value={String(legend.displayMode || "list")} onChange={(event) => setOptions("legend", { ...legend, displayMode: event.target.value })}><option value="list">Visible</option><option value="hidden">Hidden</option></Form.Select></Form.Group>
        {legend.displayMode !== "hidden" && <Form.Group><Form.Label>Legend position</Form.Label><Form.Select size="sm" value={String(legend.placement || "bottom")} onChange={(event) => setOptions("legend", { ...legend, placement: event.target.value })}><option value="bottom">Bottom</option><option value="right">Right</option><option value="top">Top</option><option value="left">Left</option></Form.Select></Form.Group>}
        <Form.Group><Form.Label>Tooltip</Form.Label><Form.Select size="sm" value={String(tooltip.mode || "all")} onChange={(event) => setOptions("tooltip", { ...tooltip, mode: event.target.value })}><option value="all">All series</option><option value="single">Nearest series</option></Form.Select></Form.Group>
        <Form.Group><Form.Label>Tooltip order</Form.Label><Form.Select size="sm" value={String(tooltip.sort || "none")} onChange={(event) => setOptions("tooltip", { ...tooltip, sort: event.target.value })}><option value="none">Natural</option><option value="desc">Descending</option><option value="asc">Ascending</option></Form.Select></Form.Group>
      </div>
      <div className="dashboard-switch-row">
        <Form.Check type="switch" label="Stack series" checked={custom.stack === "normal"} onChange={(event) => setCustom("stack", event.target.checked ? "normal" : "off")} />
        <Form.Check type="switch" label="Show points" checked={custom.showPoints === "always"} onChange={(event) => setCustom("showPoints", event.target.checked ? "always" : "none")} />
        <Form.Check type="switch" label="Connect null values" checked={custom.spanNulls !== false} onChange={(event) => setCustom("spanNulls", event.target.checked)} />
      </div>
      {(custom.showPoints === "always" || custom.drawStyle === "bars") && <div className="dashboard-option-grid mt-3">{custom.showPoints === "always" && <OptionalNumber label="Point size" min={1} max={20} value={custom.pointSize} onChange={(value) => setCustom("pointSize", value)} />}{custom.drawStyle === "bars" && <Form.Group><Form.Label>Bar width</Form.Label><Form.Control size="sm" type="number" min={0.1} max={1} step={0.1} value={typeof custom.barWidthFactor === "number" ? custom.barWidthFactor : 0.6} onChange={(event) => setCustom("barWidthFactor", Number(event.target.value))} /></Form.Group>}</div>}
    </section>;
  }

  if (panel.type === "stat") {
    const textSize = asObject(custom.textSize);
    return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Stat style</div><div className="dashboard-option-grid">
      <Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} />
      <Form.Group><Form.Label>Text</Form.Label><Form.Select size="sm" value={String(custom.textMode || "valueAndName")} onChange={(event) => setCustom("textMode", event.target.value)}><option value="valueAndName">Value and name</option><option value="value">Value</option><option value="name">Name</option></Form.Select></Form.Group>
      <Form.Group><Form.Label>Color</Form.Label><Form.Select size="sm" value={String(custom.colorMode || "value")} onChange={(event) => setCustom("colorMode", event.target.value)}><option value="value">Value text</option><option value="background">Background</option></Form.Select></Form.Group>
      <Form.Group><Form.Label>Orientation</Form.Label><Form.Select size="sm" value={String(custom.orientation || "auto")} onChange={(event) => setCustom("orientation", event.target.value)}><option value="auto">Auto</option><option value="horizontal">Horizontal</option><option value="vertical">Vertical</option></Form.Select></Form.Group>
      <OptionalNumber label="Value text size" min={8} max={96} value={textSize.value} onChange={(value) => setCustom("textSize", { ...textSize, value })} />
      <OptionalNumber label="Name text size" min={8} max={40} value={textSize.title} onChange={(value) => setCustom("textSize", { ...textSize, title: value })} />
    </div><div className="dashboard-switch-row"><Form.Check type="switch" label="Show sparkline" checked={custom.graphMode === "area"} onChange={(event) => setCustom("graphMode", event.target.checked ? "area" : "none")} /></div></section>;
  }

  if (panel.type === "gauge") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Gauge style</div><div className="dashboard-option-grid"><Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} /><Form.Group><Form.Label>Text</Form.Label><Form.Select size="sm" value={String(custom.textMode || "valueAndName")} onChange={(event) => setCustom("textMode", event.target.value)}><option value="valueAndName">Value and name</option><option value="value">Value</option><option value="name">Name</option></Form.Select></Form.Group></div></section>;

  if (panel.type === "barGauge") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Bar gauge style</div><div className="dashboard-option-grid">
    <Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} />
    <Form.Group><Form.Label>Display</Form.Label><Form.Select size="sm" value={String(custom.displayMode || "basic")} onChange={(event) => setCustom("displayMode", event.target.value)}><option value="basic">Continuous</option><option value="lcd">LCD segments</option></Form.Select></Form.Group>
    <Form.Group><Form.Label>Sort</Form.Label><Form.Select size="sm" value={String(custom.sortOrder || "desc")} onChange={(event) => setCustom("sortOrder", event.target.value)}><option value="none">Original</option><option value="desc">High to low</option><option value="asc">Low to high</option></Form.Select></Form.Group>
    <Form.Group><Form.Label>Value</Form.Label><Form.Select size="sm" value={String(custom.valueMode || "color")} onChange={(event) => setCustom("valueMode", event.target.value)}><option value="color">Threshold color</option><option value="text">Default text</option><option value="hidden">Hidden</option></Form.Select></Form.Group>
  </div></section>;

  if (panel.type === "pie") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Pie style</div><div className="dashboard-option-grid">
    <Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} />
    <Form.Group><Form.Label>Shape</Form.Label><Form.Select size="sm" value={String(custom.displayMode || "pie")} onChange={(event) => setCustom("displayMode", event.target.value)}><option value="pie">Pie</option><option value="donut">Donut</option></Form.Select></Form.Group>
    <Form.Group><Form.Label>Legend position</Form.Label><Form.Select size="sm" value={String(custom.legendPosition || "right")} onChange={(event) => setCustom("legendPosition", event.target.value)}><option value="right">Right</option><option value="bottom">Bottom</option><option value="top">Top</option><option value="left">Left</option></Form.Select></Form.Group>
  </div></section>;

  if (panel.type === "barchart") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Bar chart style</div><div className="dashboard-option-grid">
    <Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} />
    <Form.Group><Form.Label>Orientation</Form.Label><Form.Select size="sm" value={String(custom.orientation || "vertical")} onChange={(event) => setCustom("orientation", event.target.value)}><option value="vertical">Vertical</option><option value="horizontal">Horizontal</option></Form.Select></Form.Group>
    <Form.Group><Form.Label>Sort</Form.Label><Form.Select size="sm" value={String(custom.sortOrder || "none")} onChange={(event) => setCustom("sortOrder", event.target.value)}><option value="none">Original</option><option value="desc">High to low</option><option value="asc">Low to high</option></Form.Select></Form.Group>
  </div></section>;

  if (panel.type === "table" || panel.type === "tableNG") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Table style</div><div className="dashboard-option-grid">
    <Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} />
    <Form.Group><Form.Label>Value cell</Form.Label><Form.Select size="sm" value={String(cellOptions.type || (panel.type === "tableNG" ? "gauge" : "none"))} onChange={(event) => setCustom("cellOptions", { ...cellOptions, type: event.target.value })}><option value="none">Plain</option><option value="color-text">Threshold text</option><option value="color-background">Threshold background</option><option value="gauge">Gauge</option></Form.Select></Form.Group>
  </div><div className="dashboard-switch-row"><Form.Check type="switch" label="Show header" checked={custom.showHeader !== false} onChange={(event) => setCustom("showHeader", event.target.checked)} /><Form.Check type="switch" label="Enable filter" checked={custom.filterable !== false} onChange={(event) => setCustom("filterable", event.target.checked)} /><Form.Check type="switch" label="Wrap text" checked={cellOptions.wrapText === true} onChange={(event) => setCustom("cellOptions", { ...cellOptions, wrapText: event.target.checked })} /></div></section>;

  if (panel.type === "heatmap") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Heatmap style</div><Form.Group><Form.Label>Color scheme</Form.Label><Form.Select value={String(custom.scheme || "blueGreen")} onChange={(event) => setCustom("scheme", event.target.value)}><option value="blueGreen">Ocean</option><option value="green">Green</option><option value="orange">Orange</option><option value="red">Red</option><option value="violet">Violet</option></Form.Select></Form.Group></section>;

  if (panel.type === "hexbin") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Hex tile style</div><div className="dashboard-option-grid">
    <Calculation value={custom.calc} onChange={(value) => setCustom("calc", value)} />
    <Form.Group><Form.Label>Text</Form.Label><Form.Select size="sm" value={String(custom.textMode || "valueAndName")} onChange={(event) => setCustom("textMode", event.target.value)}><option value="valueAndName">Value and name</option><option value="value">Value</option><option value="name">Name</option></Form.Select></Form.Group>
    <Form.Group><Form.Label>Palette</Form.Label><Form.Select size="sm" value={JSON.stringify(custom.colorRange || ["#dbeafe", "#38bdf8", "#075985"])} onChange={(event) => setCustom("colorRange", JSON.parse(event.target.value) as string[])}><option value='["#dbeafe","#38bdf8","#075985"]'>Ocean</option><option value='["#dcfce7","#4ade80","#166534"]'>Forest</option><option value='["#ffedd5","#fb923c","#9a3412"]'>Ember</option><option value='["#f3e8ff","#c084fc","#6b21a8"]'>Violet</option></Form.Select></Form.Group>
  </div><div className="dashboard-switch-row"><Form.Check type="switch" label="Reverse palette" checked={custom.reverseColorOrder === true} onChange={(event) => setCustom("reverseColorOrder", event.target.checked)} /></div></section>;

  if (panel.type === "text") return <section className="dashboard-editor-section"><div className="dashboard-editor-section-title">Text style</div><div className="dashboard-option-grid">
    <OptionalNumber label="Text size" min={8} max={96} value={custom.textSize} onChange={(value) => setCustom("textSize", value)} />
    <Form.Group><Form.Label>Text color</Form.Label><Form.Control size="sm" type="color" value={String(custom.textColor || "#25313d")} onChange={(event) => setCustom("textColor", event.target.value)} /></Form.Group>
    <Form.Group><Form.Label>Background</Form.Label><Form.Control size="sm" type="color" value={String(custom.bgColor || "#ffffff")} onChange={(event) => setCustom("bgColor", event.target.value)} /></Form.Group>
    <Form.Group><Form.Label>Horizontal alignment</Form.Label><Form.Select size="sm" value={String(custom.justifyContent || "center")} onChange={(event) => setCustom("justifyContent", event.target.value)}><option value="flexStart">Left</option><option value="center">Center</option><option value="flexEnd">Right</option></Form.Select></Form.Group>
    <Form.Group><Form.Label>Vertical alignment</Form.Label><Form.Select size="sm" value={String(custom.alignItems || "center")} onChange={(event) => setCustom("alignItems", event.target.value)}><option value="flexStart">Top</option><option value="center">Center</option><option value="flexEnd">Bottom</option></Form.Select></Form.Group>
  </div></section>;

  return null;
}
