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

import React, { useContext, useEffect, useState } from "react";
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
import { Alert } from "react-bootstrap";
import { AppContext } from "../../../store/context";
import ClusterApi from "../../../api/clusterApi";
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

interface NetworkClusterMetricsProps {
  timeRange?: string;
  customTimeRange?: {
    startTime: number;
    endTime: number;
  } | null;
}

// Utility function to convert time range to API parameters
const getTimeRangeParams = (timeRange: string, customTimeRange?: { startTime: number; endTime: number } | null) => {
  const now = Math.floor(Date.now() / 1000);
  let fromTime = now;
  let toTime = now;
  
  if (timeRange?.includes("CUSTOM:") && customTimeRange) {
    // Handle custom time range with actual values
    fromTime = customTimeRange.startTime;
    toTime = customTimeRange.endTime;
  } else {
    // Handle predefined time ranges
    switch (timeRange) {
      case "Last 1 hour":
        fromTime = now - (60 * 60);
        break;
      case "Last 2 hours":
        fromTime = now - (2 * 60 * 60);
        break;
      case "Last 4 hours":
        fromTime = now - (4 * 60 * 60);
        break;
      case "Last 12 hours":
        fromTime = now - (12 * 60 * 60);
        break;
      case "Last 24 hours":
        fromTime = now - (24 * 60 * 60);
        break;
      case "Last 1 week":
        fromTime = now - (7 * 24 * 60 * 60);
        break;
      case "Last 1 month":
        fromTime = now - (30 * 24 * 60 * 60);
        break;
      case "Last 1 year":
        fromTime = now - (365 * 24 * 60 * 60);
        break;
      default:
        fromTime = now - (60 * 60); // Default to 1 hour
    }
  }
  
  return { fromTime, toTime };
};

const NetworkClusterMetrics: React.FC<NetworkClusterMetricsProps> = ({ timeRange = "Last 1 hour", customTimeRange = null }) => {
  const [networkData, setNetworkData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { clusterName } = useContext(AppContext);

  const fetchNetworkMetrics = async () => {
    if (!clusterName) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const { fromTime, toTime } = getTimeRangeParams(timeRange, customTimeRange);
      const timestamp = Date.now();
      
      const fields = [
        "metrics/network/In._avg",
        "metrics/network/Out._avg"
      ].join(",");
      
      const fieldsParam = `${fields}[${fromTime},${toTime},15]`;
      const response = await ClusterApi.getClusterMetrics(clusterName, `${fieldsParam}&_=${timestamp}`);
      
      if (response.metrics && response.metrics.network) {
        setNetworkData(response.metrics.network);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Failed to fetch network metrics";
      setError(errorMessage);
      console.error("Error fetching network metrics:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNetworkMetrics();
  }, [timeRange, customTimeRange, clusterName]);

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: "200px" }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="danger" className="m-2">
        Error loading network metrics: {error}
      </Alert>
    );
  }

  if (isEmpty(networkData)) {
    return (
      <Alert variant="info" className="m-2">
        No network metrics data available
      </Alert>
    );
  }

  // Prepare series data
  const seriesData = [];
  
  if (networkData["In._avg"]) {
    seriesData.push({
      name: "Network In",
      data: networkData["In._avg"].map(([value, timestamp]: [number, number]) => [
        value / 1024, // Convert to KB/s
        timestamp,
      ]),
      color: "#00CC00",
    });
  }
  
  if (networkData["Out._avg"]) {
    seriesData.push({
      name: "Network Out",
      data: networkData["Out._avg"].map(([value, timestamp]: [number, number]) => [
        value / 1024, // Convert to KB/s
        timestamp,
      ]),
      color: "#0066B3",
    });
  }

  if (seriesData.length === 0) {
    return (
      <Alert variant="info" className="m-2">
        No network metrics data available
      </Alert>
    );
  }

  // Prepare Chart.js data
  let labels: string[] = [];
  const datasets: any[] = [];

  seriesData.forEach((series) => {
    if (!series.data || series.data.length === 0) return;

    // Create labels from timestamps if not already created
    if (labels.length === 0) {
      labels = series.data.map((dataPoint: [number, number]) =>
        new Date(dataPoint[1] * 1000).toLocaleTimeString()
      );
    }

    // Extract values for this series
    const data = series.data.map((dataPoint: [number, number]) => dataPoint[0]);

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
          callback: function (value: string | number) {
            return `${Number(value).toFixed(1)} KB/s`;
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
            return `${context.dataset.label}: ${context.parsed.y.toFixed(2)} KB/s`;
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

  return (
    <div style={{ height: "150px", width: "100%" }}>
      <Line data={chartData} options={options} />
    </div>
  );
};

export default NetworkClusterMetrics;
