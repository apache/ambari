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
import { faArrowUpRightFromSquare, faDownload } from "@fortawesome/free-solid-svg-icons";
import { saveAs } from "file-saver";
import type { DashboardPanel, DashboardTarget, JsonObject } from "../../types";
import { formatMetricValue } from "../../valueFormatter";
import {
  calculatePanelValue,
  panelCustomOptions,
  panelFieldColor,
  panelFieldStandardOptions,
  panelFieldValueText,
  panelNumericBounds,
  panelValueColor,
  type DashboardPanelResult,
} from "../data/panelData";

interface TableRendererProps {
  panel: DashboardPanel;
  results: DashboardPanelResult[];
}

interface CalculatedResult {
  result: DashboardPanelResult;
  value: number | null;
}

interface WideRow {
  key: string;
  labels: Record<string, string>;
  values: Map<string, CalculatedResult>;
}

const asObject = (value: unknown): JsonObject => value && typeof value === "object" && !Array.isArray(value)
  ? value as JsonObject
  : {};

const compareValues = (left: unknown, right: unknown) => {
  const leftNumber = typeof left === "number" ? left : Number(left);
  const rightNumber = typeof right === "number" ? right : Number(right);
  return Number.isFinite(leftNumber) && Number.isFinite(rightNumber)
    ? leftNumber - rightNumber
    : String(left ?? "").localeCompare(String(right ?? ""));
};

const targetLabel = (target: DashboardTarget) => target.legend || target.refId;

const safePanelLink = (template: unknown, labels: Record<string, string>) => {
  if (typeof template !== "string" || !template.startsWith("/main/")) return undefined;
  const route = template
    .replace(/\$\{__field\.labels\.([^}]+)}/g, (_match, label: string) => encodeURIComponent(labels[label] || ""))
    .replace(/\$\{([^}]+)}/g, (_match, label: string) => encodeURIComponent(labels[label] || ""));
  return `${window.location.origin}${window.location.pathname}#${route}`;
};

export default function TableRenderer({ panel, results }: TableRendererProps) {
  const custom = panelCustomOptions(panel);
  const calculation = String(custom.calc || "lastNotNull");
  const aggregationDimension = typeof custom.aggrDimension === "string" ? custom.aggrDimension : "";
  const initialSort = String(custom.sortColumn || (aggregationDimension || "value"));
  const [filter, setFilter] = useState("");
  const [sortKey, setSortKey] = useState(initialSort);
  const [descending, setDescending] = useState(String(custom.sortOrder || "descend") !== "ascend");
  const showHeader = custom.showHeader !== false;
  const filterable = custom.filterable !== false;
  const cellOptions = asObject(custom.cellOptions);
  const cellType = String(cellOptions.type || (custom.colorMode === "background" ? "color-background" : panel.type === "tableNG" ? "gauge" : "none"));
  const wrapText = cellOptions.wrapText === true || custom.nowrap === false;
  const calculated = useMemo(() => results.map((result) => ({
    result,
    value: calculatePanelValue(result, calculation),
  })), [calculation, results]);
  const labelKeys = Array.from(new Set(results.flatMap((result) => Object.keys(result.metric))))
    .filter((key) => key !== "__name__")
    .sort();
  const targetColumns = panel.targets.filter((target) => !target.hide);

  const wideRows = useMemo(() => {
    if (!aggregationDimension) return [];
    const byDimension = new Map<string, WideRow>();
    calculated.forEach((item) => {
      const key = item.result.metric[aggregationDimension];
      if (!key) return;
      const row = byDimension.get(key) || { key, labels: item.result.metric, values: new Map() };
      row.values.set(item.result.targetRefId, item);
      byDimension.set(key, row);
    });
    const search = filter.trim().toLowerCase();
    return Array.from(byDimension.values()).filter((row) => {
      const text = [row.key, ...Object.values(row.labels), ...Array.from(row.values.values()).map((item) => item.value)]
        .join(" ").toLowerCase();
      return text.includes(search);
    }).sort((left, right) => {
      const leftValue = sortKey === aggregationDimension ? left.key : left.values.get(sortKey)?.value;
      const rightValue = sortKey === aggregationDimension ? right.key : right.values.get(sortKey)?.value;
      const comparison = compareValues(leftValue, rightValue);
      return descending ? -comparison : comparison;
    });
  }, [aggregationDimension, calculated, descending, filter, sortKey]);

  const flatRows = useMemo(() => {
    if (aggregationDimension) return [];
    const search = filter.trim().toLowerCase();
    return calculated.filter(({ result, value }) => {
      const text = [result.displayName, ...Object.values(result.metric), value].join(" ").toLowerCase();
      return text.includes(search);
    }).sort((left, right) => {
      const leftValue = sortKey === "value" ? left.value : left.result.metric[sortKey];
      const rightValue = sortKey === "value" ? right.value : right.result.metric[sortKey];
      const comparison = compareValues(leftValue, rightValue);
      return descending ? -comparison : comparison;
    });
  }, [aggregationDimension, calculated, descending, filter, sortKey]);

  const numericValues = calculated.map(({ value }) => value).filter((value): value is number => value !== null);
  const bounds = panelNumericBounds(panel);
  const min = bounds.min ?? Math.min(0, ...numericValues);
  const max = bounds.max ?? Math.max(1, ...numericValues);
  const gaugeRange = Math.max(max - min, Number.EPSILON);
  const links = Array.isArray(custom.links) ? custom.links.map(asObject) : [];
  const primaryLink = links[0];

  const formatFieldValue = (refId: string, value: number | null) => {
    const options = panelFieldStandardOptions(panel, refId);
    return panelFieldValueText(panel, refId, value) || formatMetricValue(
      value,
      typeof options.util === "string" ? options.util : "none",
      typeof options.decimals === "number" ? options.decimals : undefined,
    );
  };

  const toggleSort = (key: string, defaultDescending = true) => {
    setSortKey(key);
    setDescending((value) => sortKey === key ? !value : defaultDescending);
  };

  const exportCsv = () => {
    const document = aggregationDimension
      ? [
        [aggregationDimension, ...targetColumns.map(targetLabel)],
        ...wideRows.map((row) => [row.key, ...targetColumns.map((target) => row.values.get(target.refId)?.value ?? "")]),
      ]
      : [
        ["Series", ...labelKeys, "Value"],
        ...flatRows.map(({ result, value }) => [result.displayName, ...labelKeys.map((key) => result.metric[key] || ""), value ?? ""]),
      ];
    const csv = document.map((line) => line.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")).join("\n");
    saveAs(new Blob([csv], { type: "text/csv;charset=utf-8" }), `${panel.id}.csv`);
  };

  return (
    <div className="dashboard-table-wrap">
      <div className="dashboard-table-toolbar">
        {filterable && <Form.Control size="sm" aria-label="Filter table" placeholder="Filter rows" value={filter} onChange={(event) => setFilter(event.target.value)} />}
        <Form.Select size="sm" aria-label="Sort table" value={sortKey} onChange={(event) => setSortKey(event.target.value)}>
          {aggregationDimension
            ? <><option value={aggregationDimension}>{aggregationDimension}</option>{targetColumns.map((target) => <option key={target.refId} value={target.refId}>{targetLabel(target)}</option>)}</>
            : <><option value="value">Value</option>{labelKeys.map((key) => <option key={key} value={key}>{key}</option>)}</>}
        </Form.Select>
        <Button variant="outline-secondary" size="sm" title={descending ? "Sort ascending" : "Sort descending"} onClick={() => setDescending((value) => !value)}>{descending ? "Desc" : "Asc"}</Button>
        <Button variant="outline-secondary" size="sm" title="Export table" onClick={exportCsv}><FontAwesomeIcon icon={faDownload} /></Button>
      </div>
      <Table responsive hover size="sm" className="mb-0 align-middle dashboard-data-table">
        {aggregationDimension ? <>
          {showHeader && <thead><tr>
            <th><button type="button" className="dashboard-table-sort" onClick={() => toggleSort(aggregationDimension, false)}>{aggregationDimension}</button></th>
            {targetColumns.map((target) => <th className="text-end" key={target.refId}><button type="button" className="dashboard-table-sort" onClick={() => toggleSort(target.refId)}>{targetLabel(target)}</button></th>)}
            {primaryLink && <th aria-label="Actions" />}
          </tr></thead>}
          <tbody>{wideRows.map((row) => {
            const href = safePanelLink(primaryLink?.url, row.labels);
            return <tr key={row.key}>
              <td className={wrapText ? "dashboard-table-wrap-text" : ""}>{row.key}</td>
              {targetColumns.map((target) => {
                const item = row.values.get(target.refId);
                const color = panelFieldColor(panel, target.refId, item?.value ?? null);
                return <td className={`text-end dashboard-table-value dashboard-table-value-${cellType}`} key={target.refId} style={cellType === "color-background" ? { backgroundColor: color } : cellType === "color-text" ? { color } : undefined}>
                  {cellType === "gauge" && <span className="dashboard-table-gauge"><i style={{ width: `${item?.value == null ? 0 : Math.min(100, Math.max(0, (item.value - min) * 100 / gaugeRange))}%`, backgroundColor: color || "#278541" }} /></span>}
                  <span>{formatFieldValue(target.refId, item?.value ?? null)}</span>
                </td>;
              })}
              {primaryLink && <td className="text-end">{href && <a className="btn btn-sm btn-link" href={href} target={primaryLink.targetBlank ? "_blank" : undefined} rel={primaryLink.targetBlank ? "noreferrer" : undefined} title={String(primaryLink.title || "Open details")}><FontAwesomeIcon icon={faArrowUpRightFromSquare} /></a>}</td>}
            </tr>;
          })}</tbody>
        </> : <>
          {showHeader && <thead><tr>
            <th><button type="button" className="dashboard-table-sort" onClick={() => toggleSort("value")}>Series</button></th>
            {labelKeys.map((key) => <th key={key}><button type="button" className="dashboard-table-sort" onClick={() => toggleSort(key, false)}>{key}</button></th>)}
            <th className="text-end"><button type="button" className="dashboard-table-sort" onClick={() => toggleSort("value")}>Value</button></th>
          </tr></thead>}
          <tbody>{flatRows.map(({ result, value }) => (
            <tr key={result.seriesKey}>
              <td className={`monitoring-code ${wrapText ? "dashboard-table-wrap-text" : ""}`}>{result.displayName}</td>
              {labelKeys.map((key) => <td className={wrapText ? "dashboard-table-wrap-text" : ""} key={key}>{result.metric[key] || "-"}</td>)}
              <td className={`text-end dashboard-table-value dashboard-table-value-${cellType}`} style={cellType === "color-background" ? { backgroundColor: panelValueColor(panel, value) } : cellType === "color-text" ? { color: panelValueColor(panel, value) } : undefined}>
                {cellType === "gauge" && <span className="dashboard-table-gauge"><i style={{ width: `${value === null ? 0 : Math.min(100, Math.max(0, (value - min) * 100 / gaugeRange))}%`, backgroundColor: panelValueColor(panel, value) || "#278541" }} /></span>}
                <span>{formatFieldValue(result.targetRefId, value)}</span>
              </td>
            </tr>
          ))}</tbody>
        </>}
      </Table>
    </div>
  );
}
