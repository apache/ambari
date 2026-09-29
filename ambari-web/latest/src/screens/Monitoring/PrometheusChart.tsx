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

import {
  CategoryScale,
  BarElement,
  Chart as ChartJs,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  LogarithmicScale,
  PointElement,
  TimeScale,
  Title,
  Tooltip,
} from "chart.js";
import { Bar, Line } from "react-chartjs-2";
import { useState } from "react";
import { Eye, EyeSlash } from "react-bootstrap-icons";
import { useTranslation } from "react-i18next";
import { PrometheusResult } from "./types";
import { formatMetricValue } from "./valueFormatter";

ChartJs.register(CategoryScale, LinearScale, LogarithmicScale, PointElement, LineElement, BarElement, Filler, TimeScale, Title, Tooltip, Legend);

const LIGHT_COLORS = ["#278541", "#1769aa", "#bd6418", "#8a4f9d", "#b33a3a", "#477178"];

const colorWithOpacity = (color: string, opacity: number) => {
  const match = color.match(/^#([0-9a-f]{6})$/i);
  if (!match) return color;
  const channels = [0, 2, 4].map((offset) => Number.parseInt(match[1].slice(offset, offset + 2), 16));
  return `rgba(${channels.join(", ")}, ${Math.min(1, Math.max(0, opacity))})`;
};

type DisplayResult = PrometheusResult & { displayName?: string; targetRefId?: string };

const seriesIdentity = (result: DisplayResult) => JSON.stringify([
  result.targetRefId || result.displayName || "",
  Object.entries(result.metric).sort(([left], [right]) => left.localeCompare(right)),
]);

const seriesColorIndex = (identity: string, count: number) => {
  let hash = 0;
  for (let index = 0; index < identity.length; index++) hash = (hash * 31 + identity.charCodeAt(index)) | 0;
  return (hash >>> 0) % count;
};

const seriesName = (result: DisplayResult, index: number) => {
  if (result.displayName) return result.displayName;
  const metricName = result.metric.__name__ || `Series ${index + 1}`;
  const labels = Object.entries(result.metric)
    .filter(([name]) => name !== "__name__")
    .map(([name, value]) => `${name}=${value}`)
    .join(", ");
  return labels ? `${metricName} {${labels}}` : metricName;
};

import { useTheme } from "../../store/ThemeContext";

export default function PrometheusChart({
  results,
  unit = "",
  decimals,
  minimum,
  maximum,
  tooltipMode = "shared",
  tooltipSort = "none",
  drawStyle = "lines",
  lineInterpolation = "smooth",
  lineWidth = 2,
  fillOpacity = 0,
  stack = false,
  scaleType = "linear",
  showPoints = false,
  pointSize = 4,
  spanNulls = true,
  legendDisplay = true,
  legendMode = "list",
  legendColumns = [],
  legendPlacement = "bottom",
  barWidthFactor = 0.6,
  thresholds = [],
  height,
  start,
  end,
}: {
  results: DisplayResult[];
  unit?: string;
  decimals?: number;
  minimum?: number;
  maximum?: number;
  tooltipMode?: string;
  tooltipSort?: string;
  drawStyle?: string;
  lineInterpolation?: string;
  lineWidth?: number;
  fillOpacity?: number;
  stack?: boolean;
  scaleType?: string;
  showPoints?: boolean;
  pointSize?: number;
  spanNulls?: boolean;
  legendDisplay?: boolean;
  legendMode?: string;
  legendColumns?: string[];
  legendPlacement?: "top" | "left" | "right" | "bottom";
  barWidthFactor?: number;
  thresholds?: Array<{ value: number | null; color: string }>;
  height?: number;
  start?: number;
  end?: number;
}) {
  const { resolved } = useTheme();
  const { t } = useTranslation();
  const [visibility, setVisibility] = useState<{ hidden: Set<string>; only: string | null }>(() => ({ hidden: new Set(), only: null }));
  const identities = results.map(seriesIdentity);
  const isHidden = (id: string) => visibility.only !== null ? visibility.only !== id : visibility.hidden.has(id);
  const toggleSeries = (id: string) => setVisibility(current => {
    const hidden = current.only === null ? new Set(current.hidden) : new Set(identities.filter(key => key !== current.only));
    if (hidden.has(id)) hidden.delete(id); else hidden.add(id);
    return { hidden, only: null };
  });
  const showAll = () => setVisibility({ hidden: new Set(), only: null });
  const visibleCount = identities.filter(id => !isHidden(id)).length;
  const COLORS = resolved === "dark" ? ["#7ee2b8", "#79c0ff", "#e3b341", "#bc8cff", "#ff7b72", "#76e3ea"] : LIGHT_COLORS;
  const colors = identities.map(id => COLORS[seriesColorIndex(id, COLORS.length)]);
  const axisColor = resolved === "dark" ? "#afbdcb" : "#637380";
  const gridColor = resolved === "dark" ? "#303d4a" : "#e8edf2";
  const timestampSet = new Set<number>();
  results.forEach((result) => {
    (result.values || (result.value ? [result.value] : [])).forEach(([timestamp]) => timestampSet.add(timestamp));
  });
  const timestamps = Array.from(timestampSet).sort((left, right) => left - right);
  const datasets = results.map((result, index) => {
    const points = new Map((result.values || (result.value ? [result.value] : [])).map(
      ([timestamp, value]) => [timestamp, Number(value)],
    ));
    return {
      seriesId: identities[index],
      hidden: isHidden(identities[index]),
      label: seriesName(result, index),
      data: timestamps.map((timestamp) => ({
        x: timestamp * 1000,
        y: points.get(timestamp) ?? null,
      })),
      borderColor: colors[index],
      backgroundColor: colorWithOpacity(colors[index], fillOpacity),
      pointRadius: showPoints || timestamps.length <= 1 ? pointSize : 0,
      pointHoverRadius: Math.max(pointSize, 4),
      tension: lineInterpolation === "smooth" ? 0.28 : 0,
      borderWidth: lineWidth,
      fill: fillOpacity > 0,
      spanGaps: spanNulls,
      stack: stack ? "dashboard" : undefined,
    };
  });

  const range = timestamps.length > 1 ? timestamps[timestamps.length - 1] - timestamps[0] : 0;
  const formatTimestamp = (timestamp: number) => new Date(timestamp).toLocaleString([], {
    month: range > 2 * 24 * 60 * 60 ? "short" : undefined,
    day: range > 2 * 24 * 60 * 60 ? "2-digit" : undefined,
    hour: "2-digit",
    minute: "2-digit",
  });

  const legendPosition = ["top", "left", "right", "bottom"].includes(legendPlacement)
    ? legendPlacement
    : "bottom";
  const tooltipValue = (item: unknown) => Number((item as { parsed?: { y?: number } }).parsed?.y || 0);
  const itemSort = tooltipSort === "asc"
    ? (left: unknown, right: unknown) => tooltipValue(left) - tooltipValue(right)
    : tooltipSort === "desc"
      ? (left: unknown, right: unknown) => tooltipValue(right) - tooltipValue(left)
      : undefined;
  const tableLegend = legendDisplay && legendMode === "table";
  const visibleLegendColumns = legendColumns.length ? legendColumns : ["last"];
  const legendIndices = results.map((_, index) => index).sort((left, right) =>
    seriesName(results[left], left).localeCompare(seriesName(results[right], right), undefined, { numeric: true })
    || identities[left].localeCompare(identities[right]));
  const legendValue = (result: DisplayResult, calculation: string) => {
    const values = (result.values || (result.value ? [result.value] : []))
      .map(([, value]) => Number(value))
      .filter(Number.isFinite);
    if (!values.length) return null;
    switch (calculation) {
      case "min": return Math.min(...values);
      case "max": return Math.max(...values);
      case "avg": return values.reduce((sum, value) => sum + value, 0) / values.length;
      case "sum": return values.reduce((sum, value) => sum + value, 0);
      case "first": return values[0];
      default: return values.at(-1) ?? null;
    }
  };
  const legendName = (result: DisplayResult, index: number) => {
    const id = identities[index], name = seriesName(result, index), hidden = isHidden(id);
    return <div className="chart-legend-entry">
      <button type="button" className={`chart-legend-toggle ${hidden ? "is-hidden" : ""}`}
        aria-label={t(hidden ? "chartLegend.showSeries" : "chartLegend.hideSeries", { name })}
        aria-pressed={!hidden} title={t(hidden ? "chartLegend.showSeries" : "chartLegend.hideSeries", { name })}
        onClick={() => toggleSeries(id)}>
        <span className="chart-legend-swatch" style={{ backgroundColor: colors[index] }} />
        <span className="chart-legend-name">{name}</span>
        {hidden ? <EyeSlash aria-hidden="true" /> : <Eye aria-hidden="true" />}
      </button>
      <button type="button" className="chart-legend-only" aria-label={t("chartLegend.onlySeries", { name })}
        title={t("chartLegend.onlySeries", { name })} onClick={() => setVisibility({ hidden: new Set(), only: id })}>{t("chartLegend.only")}</button>
    </div>;
  };
  const legend = legendDisplay && results.length > 0 ? <div className="dashboard-series-legend" aria-label={t("chartLegend.label")}>
    <div className="chart-legend-toolbar"><span role="status" title={t("chartLegend.help")}>{visibleCount === 0 ? t("chartLegend.allHidden") : t("chartLegend.visible", { shown: visibleCount, total: results.length })}</span>
      <button type="button" onClick={showAll} disabled={visibleCount === results.length && visibility.only === null}>{t("chartLegend.showAll")}</button></div>
    <div className="chart-legend-scroll">{tableLegend ? <table>
      <thead><tr><th>{t("chartLegend.label")}</th>{visibleLegendColumns.map(column => <th key={column}>{column}</th>)}</tr></thead>
      <tbody>{legendIndices.map(index => <tr key={identities[index]}><td>{legendName(results[index], index)}</td>
        {visibleLegendColumns.map(column => <td key={column}>{formatMetricValue(legendValue(results[index], column), unit, decimals)}</td>)}
      </tr>)}</tbody></table> : legendIndices.map(index => <div key={identities[index]}>{legendName(results[index], index)}</div>)}</div>
  </div> : null;
  const wrapperClass = `dashboard-chart-wrap ${legendDisplay ? `dashboard-chart-with-legend chart-legend-${legendPosition}` : ""}`;
  const thresholdPlugin = {
    id: "ambariThresholdLines",
    afterDatasetsDraw: (chart: ChartJs) => {
      const yScale = chart.scales.y;
      const { ctx, chartArea } = chart;
      if (!yScale || !chartArea) return;
      thresholds.forEach((threshold) => {
        if (threshold.value === null) return;
        const y = yScale.getPixelForValue(threshold.value);
        if (y < chartArea.top || y > chartArea.bottom) return;
        ctx.save();
        ctx.beginPath();
        ctx.setLineDash([5, 4]);
        ctx.strokeStyle = threshold.color;
        ctx.lineWidth = 1;
        ctx.moveTo(chartArea.left, y);
        ctx.lineTo(chartArea.right, y);
        ctx.stroke();
        ctx.restore();
      });
    },
  };

  if (drawStyle === "bars") {
    return (
      <div className={wrapperClass} data-visible-series={visibleCount} style={{ height: Math.max(180, height || 300) }}>
        <div className="dashboard-chart-canvas"><Bar
          datasetIdKey="seriesId"
          plugins={thresholds.length ? [thresholdPlugin] : undefined}
          data={{
            labels: timestamps.map((timestamp) => formatTimestamp(timestamp * 1000)),
            datasets: results.map((result, index) => {
              const points = new Map((result.values || (result.value ? [result.value] : [])).map(
                ([timestamp, value]) => [timestamp, Number(value)],
              ));
              return {
                seriesId: identities[index],
                hidden: isHidden(identities[index]),
                label: seriesName(result, index),
                data: timestamps.map((timestamp) => points.get(timestamp) ?? null),
                backgroundColor: colors[index],
                borderWidth: 0,
                barPercentage: barWidthFactor,
                stack: stack ? "dashboard" : undefined,
              };
            }),
          }}
          options={{
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: tooltipMode === "single" ? "nearest" : "index", intersect: false },
            plugins: {
              legend: { display: false },
              tooltip: {
                itemSort,
                callbacks: { label: (context) => `${context.dataset.label || "Series"}: ${formatMetricValue(context.parsed.y, unit, decimals)}` },
              },
            },
            scales: {
              x: { stacked: stack, grid: { color: gridColor }, ticks: { color: axisColor, maxTicksLimit: 8, maxRotation: 0 } },
              y: { type: scaleType === "log" ? "logarithmic" : "linear", stacked: stack, min: minimum, max: maximum, grid: { color: gridColor }, ticks: { color: axisColor, callback: (value) => formatMetricValue(value, unit, decimals) } },
            },
          }}
        /></div>
        {legend}
      </div>
    );
  }

  return (
    <div className={wrapperClass} data-visible-series={visibleCount} style={{ height: Math.max(180, height || 300) }}>
      <div className="dashboard-chart-canvas"><Line
        datasetIdKey="seriesId"
        plugins={thresholds.length ? [thresholdPlugin] : undefined}
        data={{
          datasets,
        }}
        options={{
          responsive: true,
          maintainAspectRatio: false,
          interaction: { mode: tooltipMode === "single" ? "nearest" : "index", intersect: false },
          plugins: {
            legend: { display: false },
            tooltip: {
              itemSort,
              callbacks: {
                title: (items) => items.length ? formatTimestamp(Number(items[0].parsed.x)) : "",
                label: (context) => {
                  const label = context.dataset.label ? `${context.dataset.label}: ` : "";
                  return `${label}${formatMetricValue(context.parsed.y, unit, decimals)}`;
                },
              },
            },
          },
          scales: {
            x: {
              type: "linear",
              grid: { color: gridColor },
              min: Number.isFinite(start) ? start! * 1000 : undefined,
              max: Number.isFinite(end) ? end! * 1000 : undefined,
              ticks: {
                color: axisColor,
                maxTicksLimit: 8,
                callback: (value) => formatTimestamp(Number(value)),
              },
            },
            y: {
              grid: { color: gridColor },
              type: scaleType === "log" ? "logarithmic" : "linear",
              stacked: stack,
              min: minimum,
              max: maximum,
              ticks: {
                color: axisColor,
                callback: (value) => formatMetricValue(value, unit, decimals),
              },
            },
          },
        }}
      /></div>
      {legend}
    </div>
  );
}
