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

import React from "react";
import { Line } from "react-chartjs-2";
import {
  LineElement,
  PointElement,
  LinearScale,
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  Chart as ChartJs,
  Filler,
} from "chart.js";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faDownload } from "@fortawesome/free-solid-svg-icons";
import { Alert, Dropdown } from "react-bootstrap";
import { translate } from "../../Utils/Utility";
import { isEmpty } from "lodash";

ChartJs.register(
  LineElement,
  PointElement,
  LinearScale,
  Title,
  Tooltip,
  Legend,
  CategoryScale,
  Filler
);

interface MetricData {
  [key: string]: {
    [metricName: string]: Array<[number, number]>; // [value, timestamp] pairs
  };
}

interface HostMetricsGraphProps {
  metricsData: MetricData;
  selectedMetricsOption?: string;
}

// Utility functions for data export
const downloadFile = (content: string, filename: string, mimeType: string) => {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const formatSeriesNameWithUnit = (name: string, unit: string) => {
  return unit ? `${name} (${unit})` : name;
};

const formatFilename = (metricName: string) => {
  return metricName.toLowerCase().replace(/\s+/g, "_");
};

const generateCSVContent = (seriesData: any[], unit: string) => {
  if (!seriesData || seriesData.length === 0) return "";

  // Get all unique timestamps from all series
  const timestampSet = new Set<number>();
  seriesData.forEach((series) => {
    series.data.forEach(([_, timestamp]: [number, number]) => {
      timestampSet.add(timestamp);
    });
  });

  const timestamps = Array.from(timestampSet).sort((a, b) => a - b);

  // Create headers with Bytes unit for applicable metrics
  const headers = ["Timestamp"];
  seriesData.forEach((series) => {
    const exportUnit = unit === "GB" || unit === "KB/s" ? "Bytes" : unit;
    headers.push(formatSeriesNameWithUnit(series.name, exportUnit));
  });

  // Create data rows
  const rows = [headers.join(",")];

  timestamps.forEach((timestamp) => {
    const row = [timestamp.toString()];

    seriesData.forEach((series) => {
      // Find the value for this timestamp in this series
      const dataPoint = series.data.find(
        ([_, ts]: [number, number]) => ts === timestamp
      );

      if (dataPoint) {
        let value = dataPoint[0];

        // Convert back to bytes for export
        if (unit === "GB") {
          value = value * Math.pow(2, 20);
        } else if (unit === "KB/s") {
          value = value * 1024;
        }

        row.push(value.toString());
      } else {
        row.push("");
      }
    });

    rows.push(row.join(","));
  });

  return rows.join("\n");
};

const generateJSONContent = (seriesData: any[], unit: string) => {
  return JSON.stringify(
    seriesData.map((series) => {
      const exportUnit = unit === "GB" || unit === "KB/s" ? "Bytes" : unit;
      const exportData = series.data.map(
        ([value, timestamp]: [number, number]) => {
          let exportValue = value;

          // Convert back to bytes for export
          if (unit === "GB") {
            exportValue = value * Math.pow(2, 20);
          } else if (unit === "KB/s") {
            exportValue = value * 1024;
          }

          return [exportValue, timestamp];
        }
      );

      return {
        name: formatSeriesNameWithUnit(series.name, exportUnit),
        data: exportData,
      };
    }),
    null,
    2
  );
};

const HostMetricsGraph: React.FC<HostMetricsGraphProps> = ({
  metricsData,
  selectedMetricsOption,
}) => {
  const renderChartJsChart = (metricName: string) => {
    // Configuration for all metrics using Chart.js
    const metricConfigs: Record<string, any> = {
      "CPU Usage": {
        calculation: (data: MetricData) => {
          const cpuIdle = data.cpu?.cpu_idle || [];
          const cpuUser = data.cpu?.cpu_user || [];
          const cpuSystem = data.cpu?.cpu_system || [];
          const cpuWio = data.cpu?.cpu_wio || [];
          const cpuNice = data.cpu?.cpu_nice || [];

          const series = [];
          if (cpuUser.length > 0) {
            series.push({
              name: "CPU User",
              data: cpuUser,
              color: "#FF8000",
            });
          }
          if (cpuSystem.length > 0) {
            series.push({
              name: "CPU System",
              data: cpuSystem,
              color: "#0066B3",
            });
          }
          if (cpuWio.length > 0) {
            series.push({
              name: "CPU I/O Idle",
              data: cpuWio,
              color: "#FFCC00",
            });
          }
          if (cpuNice.length > 0) {
            series.push({
              name: "CPU Nice",
              data: cpuNice,
              color: "#00CC00",
            });
          }
          if (cpuIdle.length > 0) {
            series.push({
              name: "CPU Idle",
              data: cpuIdle,
              color: "#CFECEC",
            });
          }
          return series;
        },
        unit: "%",
      },
      "Disk Usage": {
        calculation: (data: MetricData) => {
          const diskTotal = data.disk?.disk_total || [];
          const diskFree = data.disk?.disk_free || [];

          const series = [];
          if (diskTotal.length > 0) {
            series.push({
              name: "Total",
              data: diskTotal.map(([value, timestamp]) => [value, timestamp]),
              color: "#0066B3",
            });
          }
          if (diskFree.length > 0) {
            series.push({
              name: "Available",
              data: diskFree.map(([value, timestamp]) => [value, timestamp]),
              color: "#00CC00",
            });
          }
          return series;
        },
        unit: "GB",
      },
      Load: {
        calculation: (data: MetricData) => {
          const loadOne = data.load?.load_one || [];
          const loadFive = data.load?.load_five || [];
          const loadFifteen = data.load?.load_fifteen || [];

          const series = [];
          if (loadOne.length > 0) {
            series.push({
              name: "1 Minute Load",
              data: loadOne,
              color: "#FF8000",
            });
          }
          if (loadFive.length > 0) {
            series.push({
              name: "5 Minute Load",
              data: loadFive,
              color: "#0066B3",
            });
          }
          if (loadFifteen.length > 0) {
            series.push({
              name: "15 Minute Load",
              data: loadFifteen,
              color: "#00CC00",
            });
          }
          return series;
        },
        unit: "",
      },
      "Memory Usage": {
        calculation: (data: MetricData) => {
          const memFree = data.memory?.mem_free || [];
          const memCached = data.memory?.mem_cached || [];
          const memShared = data.memory?.mem_shared || [];
          const swapFree = data.memory?.swap_free || [];

          const series = [];
          if (memFree.length > 0) {
            series.push({
              name: "Free",
              data: memFree.map(([value, timestamp]) => [
                value / Math.pow(2, 20),
                timestamp,
              ]),
              color: "#0066B3",
            });
          }
          if (memCached.length > 0) {
            series.push({
              name: "Cached",
              data: memCached.map(([value, timestamp]) => [
                value / Math.pow(2, 20),
                timestamp,
              ]),
              color: "#00CC00",
            });
          }
          if (memShared.length > 0) {
            series.push({
              name: "Shared",
              data: memShared.map(([value, timestamp]) => [
                value / Math.pow(2, 20),
                timestamp,
              ]),
              color: "#FF8000",
            });
          }
          if (swapFree.length > 0) {
            series.push({
              name: "Swap",
              data: swapFree.map(([value, timestamp]) => [
                value / Math.pow(2, 20),
                timestamp,
              ]),
              color: "#FFCC00",
            });
          }
          return series;
        },
        unit: "GB",
      },
      "Network Usage": {
        calculation: (data: MetricData) => {
          const bytesIn = data.network?.bytes_in || [];
          const bytesOut = data.network?.bytes_out || [];
          const pktsIn = data.network?.pkts_in || [];
          const pktsOut = data.network?.pkts_out || [];

          const series = [];
          if (bytesIn.length > 0) {
            series.push({
              name: "Bytes In",
              data: bytesIn.map(([value, timestamp]) => [
                value / 1024,
                timestamp,
              ]),
              color: "#00CC00",
            });
          }
          if (bytesOut.length > 0) {
            series.push({
              name: "Bytes Out",
              data: bytesOut.map(([value, timestamp]) => [
                value / 1024,
                timestamp,
              ]),
              color: "#0066B3",
            });
          }
          if (pktsIn.length > 0) {
            series.push({
              name: "Packets In",
              data: pktsIn,
              color: "#FF8000",
            });
          }
          if (pktsOut.length > 0) {
            series.push({
              name: "Packets Out",
              data: pktsOut,
              color: "#FFCC00",
            });
          }
          return series;
        },
        unit: "KB/s",
      },
      Processes: {
        calculation: (data: MetricData) => {
          const procTotal = data.process?.proc_total || [];
          const procRun = data.process?.proc_run || [];

          const series = [];
          if (procTotal.length > 0) {
            series.push({
              name: "Total Processes",
              data: procTotal,
              color: "#0066B3",
            });
          }
          if (procRun.length > 0) {
            series.push({
              name: "Processes Run",
              data: procRun,
              color: "#00CC00",
            });
          }
          return series;
        },
        unit: "",
      },
    };

    const config = metricConfigs[metricName];
    if (!config) return null;

    if (isEmpty(metricsData)) {
      if (selectedMetricsOption?.includes("CUSTOM")) {
        return (
          <div className="mx-4">
            <Alert variant="info w-100">
              {translate("graphs.noData.title")}
              {": "}
              {translate("graphs.noDataAtTime.message")}
            </Alert>
            <div className="d-flex justify-content-center">{metricName}</div>
          </div>
        );
      }
      return (
        <div className="mx-4">
          <Alert variant="info px-4 w-100">
            {translate("graphs.noData.message")}
          </Alert>
          <div className="d-flex justify-content-center">{metricName}</div>
        </div>
      );
    }

    const seriesData = config.calculation(metricsData);

    // Prepare Chart.js data
    let labels: string[] = [];
    const datasets: any[] = [];

    seriesData.forEach((series: any) => {
      if (!series.data || series.data.length === 0) return;

      // Create labels from timestamps if not already created
      if (labels.length === 0) {
        labels = series.data.map(([_, timestamp]: [number, number]) =>
          new Date(timestamp * 1000).toLocaleTimeString()
        );
      }

      // Extract values for this series
      const data = series.data.map(([value]: [number, number]) => value);

      datasets.push({
        label: series.name,
        data,
        fill: false,
        backgroundColor: series.color + "40",
        borderColor: series.color,
        borderWidth: 1.5,
        pointRadius: 0.5,
        pointHoverRadius: 2,
        tension: 0.1,
      });
    });

    const chartData = {
      labels,
      datasets,
    };

    const options = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          display: false, // Hide x-axis labels for compact view
          grid: {
            display: false,
          },
        },
        y: {
          beginAtZero: true,
          grid: {
            color: "rgba(0,0,0,0.1)",
          },
          ticks: {
            font: {
              size: 10,
            },
            callback: function (value: any) {
              return value.toFixed(1) + (config.unit ? ` ${config.unit}` : "");
            },
          },
        },
      },
      plugins: {
        legend: {
          display: false,
        },
        tooltip: {
          mode: "index" as const,
          intersect: false,
          callbacks: {
            label: function (context: any) {
              return `${context.dataset.label}: ${context.parsed.y.toFixed(
                2
              )} ${config.unit}`;
            },
          },
        },
      },
      interaction: {
        mode: "nearest" as const,
        axis: "x" as const,
        intersect: false,
      },
    };

    // Export handlers
    const handleCSVExport = () => {
      const csvContent = generateCSVContent(seriesData, config.unit);
      if (csvContent) {
        downloadFile(
          csvContent,
          `${formatFilename(metricName)}.csv`,
          "text/csv"
        );
      }
    };

    const handleJSONExport = () => {
      const jsonContent = generateJSONContent(seriesData, config.unit);
      if (jsonContent) {
        downloadFile(
          jsonContent,
          `${formatFilename(metricName)}.json`,
          "application/json"
        );
      }
    };

    return (
      <div className="p-1">
        <div>
          <Line data={chartData} options={options} />
        </div>

        <div className="d-flex justify-content-between">
          <div className="pt-2 ps-4">{metricName}</div>
          <Dropdown drop="down">
            <Dropdown.Toggle
              bsPrefix="custom"
              variant="transparent border-0"
              className="m-0 p-0 text-dark"
              title="Export"
            >
              <FontAwesomeIcon icon={faDownload} />
            </Dropdown.Toggle>
            <Dropdown.Menu>
              <Dropdown.Item onClick={handleCSVExport}>
                Save as CSV
              </Dropdown.Item>
              <Dropdown.Item onClick={handleJSONExport}>
                Save as JSON
              </Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </div>
      </div>
    );
  };

  const metricNames = [
    "CPU Usage",
    "Disk Usage",
    "Load",
    "Memory Usage",
    "Network Usage",
    "Processes",
  ];

  const metricRows: string[][] = [];
  for (let i = 0; i < metricNames.length; i += 2) {
    metricRows.push(metricNames.slice(i, i + 2));
  }

  return (
    <div className="px-4">
      {metricRows.map((row, rowIndex) => (
        <div
          key={rowIndex}
          className="d-flex justify-content-center mb-4 gap-2"
        >
          {row.map((metricName) => (
            <div key={metricName} className="min-w-0">
              {renderChartJsChart(metricName)}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
};

export default HostMetricsGraph;
