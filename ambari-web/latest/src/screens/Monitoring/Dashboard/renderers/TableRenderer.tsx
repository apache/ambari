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
import { formatMetricValue, getPanelUnit } from "../../valueFormatter";
import { latestPanelValue, type DashboardPanelResult } from "../data/panelData";

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
  const rows = useMemo(() => results.map((result) => ({
    result,
    value: latestPanelValue(result),
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
  }), [filter, results, sortKey, descending]);

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
        <Form.Control size="sm" aria-label="Filter table" placeholder="Filter rows" value={filter} onChange={(event) => setFilter(event.target.value)} />
        <Form.Select size="sm" aria-label="Sort table" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
          <option value="value">Value</option>
          {labelKeys.map((key) => <option key={key} value={key}>{key}</option>)}
        </Form.Select>
        <Button variant="outline-secondary" size="sm" title={descending ? "Sort ascending" : "Sort descending"} onClick={() => setDescending((value) => !value)}>{descending ? "Desc" : "Asc"}</Button>
        <Button variant="outline-secondary" size="sm" title="Export table" onClick={exportCsv}><FontAwesomeIcon icon={faDownload} /></Button>
      </div>
      <Table responsive hover size="sm" className="mb-0 align-middle dashboard-data-table">
        <thead>
          <tr>
            <th><button type="button" className="dashboard-table-sort" onClick={() => { setSortKey("value"); setDescending((value) => sortKey === "value" ? !value : true); }}>Series</button></th>
            {labelKeys.map((key) => <th key={key}><button type="button" className="dashboard-table-sort" onClick={() => { setSortKey(key); setDescending((value) => sortKey === key ? !value : false); }}>{key}</button></th>)}
            <th className="text-end"><button type="button" className="dashboard-table-sort" onClick={() => { setSortKey("value"); setDescending((value) => sortKey === "value" ? !value : true); }}>Value</button></th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ result, value }) => (
            <tr key={result.seriesKey}>
              <td className="monitoring-code">{result.displayName}</td>
              {labelKeys.map((key) => <td key={key}>{result.metric[key] || "-"}</td>)}
              <td className="text-end">{formatMetricValue(value, unit)}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}
