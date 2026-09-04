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

import { useMemo, useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faDownload } from "@fortawesome/free-solid-svg-icons";
import { saveAs } from "file-saver";
import type { DashboardPanel } from "../../types";
import { formatMetricValue, getPanelDecimals, getPanelUnit } from "../../valueFormatter";
import {
  calculatePanelValue,
  panelCustomOptions,
  panelNumericBounds,
  panelValueColor,
  type DashboardPanelResult,
} from "../data/panelData";

interface TableRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

export default function TableRenderer({ panel, results }: TableRendererProps) {
  const [filter, setFilter] = useState("");
  const [sortKey, setSortKey] = useState("value");
  const [descending, setDescending] = useState(true);
  const labelKeys = Array.from(new Set(results.flatMap((result) => Object.keys(result.metric))))
    .filter((key) => key !== "__name__")
    .sort();
  const unit = getPanelUnit(panel.options);
  const decimals = getPanelDecimals(panel.options);
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const showHeader = custom.showHeader !== false;
  const filterable = custom.filterable !== false;
  const cellOptions = custom.cellOptions && typeof custom.cellOptions === "object"
    ? custom.cellOptions as Record<string, unknown>
    : {};
  const cellType = String(cellOptions.type || (panel.type === "tableNG" ? "gauge" : "none"));
  const wrapText = cellOptions.wrapText === true;
  const bounds = panelNumericBounds(panel);
  const rows = useMemo(() => results.map((result) => ({
    result,
    value: calculatePanelValue(result, calculation),
  })).filter(({ result, value }) => {
    const searchText = [result.displayName, ...Object.values(result.metric), value == null ? "" : String(value)]
      .join(" ").toLowerCase();
    return searchText.includes(filter.trim().toLowerCase());
  }).sort((left, right) => {
    const leftValue = sortKey === "value" ? left.value : left.result.metric[sortKey] || "";
    const rightValue = sortKey === "value" ? right.value : right.result.metric[sortKey] || "";
    const leftNumber = typeof leftValue === "number" ? leftValue : Number(leftValue);
    const rightNumber = typeof rightValue === "number" ? rightValue : Number(rightValue);
    const comparison = Number.isFinite(leftNumber) && Number.isFinite(rightNumber)
      ? leftNumber - rightNumber
      : String(leftValue).localeCompare(String(rightValue));
    return descending ? -comparison : comparison;
  }), [calculation, filter, results, sortKey, descending]);

  const numericValues = rows.map(({ value }) => value).filter((value): value is number => value !== null);
  const min = bounds.min ?? Math.min(0, ...numericValues);
  const max = bounds.max ?? Math.max(1, ...numericValues);
  const gaugeRange = Math.max(max - min, Number.EPSILON);

  const exportCsv = () => {
    const header = ["Series", ...labelKeys, "Value"];
    const lines = rows.map(({ result, value }) => [
      result.displayName,
      ...labelKeys.map((key) => result.metric[key] || ""),
      value == null ? "" : String(value),
    ]);
    const csv = [header, ...lines].map((line) => line.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\n");
    saveAs(new Blob([csv], { type: "text/csv;charset=utf-8" }), `${panel.id}.csv`);
  };

  return (
    <div className="dashboard-table-wrap">
      <div className="dashboard-table-toolbar">
        {filterable && <Form.Control size="sm" aria-label="Filter table" placeholder="Filter rows" value={filter} onChange={(event) => setFilter(event.target.value)} />}
        <Form.Select size="sm" aria-label="Sort table" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
          <option value="value">Value</option>
          {labelKeys.map((key) => <option key={key} value={key}>{key}</option>)}
        </Form.Select>
        <Button variant="outline-secondary" size="sm" title={descending ? "Sort ascending" : "Sort descending"} onClick={() => setDescending((value) => !value)}>{descending ? "Desc" : "Asc"}</Button>
        <Button variant="outline-secondary" size="sm" title="Export table" onClick={exportCsv}><FontAwesomeIcon icon={faDownload} /></Button>
      </div>
      <Table responsive hover size="sm" className="mb-0 align-middle dashboard-data-table">
        {showHeader && <thead>
          <tr>
            <th><button type="button" className="dashboard-table-sort" onClick={() => { setSortKey("value"); setDescending((value) => sortKey === "value" ? !value : true); }}>Series</button></th>
            {labelKeys.map((key) => <th key={key}><button type="button" className="dashboard-table-sort" onClick={() => { setSortKey(key); setDescending((value) => sortKey === key ? !value : false); }}>{key}</button></th>)}
            <th className="text-end"><button type="button" className="dashboard-table-sort" onClick={() => { setSortKey("value"); setDescending((value) => sortKey === "value" ? !value : true); }}>Value</button></th>
          </tr>
        </thead>}
        <tbody>
          {rows.map(({ result, value }) => (
            <tr key={result.seriesKey}>
              <td className={`monitoring-code ${wrapText ? "dashboard-table-wrap-text" : ""}`}>{result.displayName}</td>
              {labelKeys.map((key) => <td className={wrapText ? "dashboard-table-wrap-text" : ""} key={key}>{result.metric[key] || "-"}</td>)}
              <td className={`text-end dashboard-table-value dashboard-table-value-${cellType}`} style={cellType === "color-background" ? { backgroundColor: panelValueColor(panel, value) } : cellType === "color-text" ? { color: panelValueColor(panel, value) } : undefined}>
                {cellType === "gauge" && <span className="dashboard-table-gauge"><i style={{ width: `${value === null ? 0 : Math.min(100, Math.max(0, (value - min) * 100 / gaugeRange))}%`, backgroundColor: panelValueColor(panel, value) || "#278541" }} /></span>}
                <span>{formatMetricValue(value, unit, decimals)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}
