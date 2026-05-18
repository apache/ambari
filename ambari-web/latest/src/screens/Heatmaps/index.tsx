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
import { useCallback, useContext, useEffect, useState } from "react";
import { AppContext } from "../../store/context";
import MetricsApi from "../../api/metricsApi";
import { map } from "lodash";
import Spinner from "../../components/Spinner";
import { Card, CardBody, Col, Dropdown, Row } from "react-bootstrap";
import usePolling from "../../hooks/usePolling";
import {
  extractExpressions,
  computeHeatmapExpression,
  createHeatmapMetric,
  createHostPercentageWidgets,
} from "../../Utils/metricProcessing";
import HeatmapLegend from "../../components/Heatmap/HeatmapLegend";
import HeatmapGrid from "../../components/Heatmap/HeatmapGrid";
import { useNavigate } from "react-router-dom";

type WidgetInfo = {
  author: string;
  cluster_name: string;
  id: number;
  metrics: any;
  scope: string;
  widget_name: string;
  widget_type: string;
};

type HostInfo = {
  hostName: string;
  publicHostName: string;
  osType: string;
  ip: string;
  rack: string;
  diskTotal: number;
  diskFree: number;
  cpuSystem: number;
  cpuUser: number;
  memTotal: number;
  memFree: number;
  hostComponents: string[];
};

type RackInfo = {
  name: string;
  rackId: string;
  hosts: HostInfo[];
  isLoaded: boolean;
  index: number;
};

function Heatmaps({ serviceName }: { serviceName: string }) {
  const { clusterName } = useContext(AppContext);
  const [metrics, setMetrics] = useState<WidgetInfo[]>([]);
  const [metricsLoading, setMetricsLoading] = useState<boolean>(false);
  const [selectedMetric, setSelectedMetric] = useState<any>(null);
  const [racks, setRacks] = useState<RackInfo[]>([]);
  const [, setRackMap] = useState<Record<string, RackInfo>>({});
  const [isLoaded, setIsLoaded] = useState<boolean>(false);
  const navigate = useNavigate();
  const [, setMetricsData] = useState<any[]>([]);
  const [heatmapMetric, setHeatmapMetric] = useState<any>(null);
  const [inputMaximum, setInputMaximum] = useState<string>("");

  const processHostsData = (data: any): { hosts: HostInfo[], rackMap: Record<string, RackInfo>, racks: RackInfo[] } => {
    const hosts: HostInfo[] = [];

    data.items.forEach((item: any) => {
      const host: HostInfo = {
        hostName: item.Hosts.host_name,
        publicHostName: item.Hosts.public_host_name,
        osType: item.Hosts.os_type,
        ip: item.Hosts.ip,
        rack: item.Hosts.rack_info,
        diskTotal: item.metrics?.disk?.disk_total || 0,
        diskFree: item.metrics?.disk?.disk_free || 0,
        cpuSystem: item.metrics?.cpu?.cpu_system || 0,
        cpuUser: item.metrics?.cpu?.cpu_user || 0,
        memTotal: item.metrics?.memory?.mem_total || 0,
        memFree: item.metrics?.memory?.mem_free || 0,
        hostComponents: item.host_components.map((hc: any) => hc.HostRoles.component_name),
      };
      hosts.push(host);
    });

    const rackMap = indexByRackId(hosts);
    const racks = toList(rackMap);

    return { hosts, rackMap, racks };
  };

  const indexByRackId = (hosts: HostInfo[]): Record<string, RackInfo> => {
    const rackMap: Record<string, RackInfo> = {};

    hosts.forEach((host) => {
      const rackId = host.rack;
      if (!rackMap[rackId]) {
        rackMap[rackId] = {
          name: rackId,
          rackId: rackId,
          hosts: [host],
          isLoaded: false,
          index: 0,
        };
      } else {
        rackMap[rackId].hosts.push(host);
      }
    });

    return rackMap;
  };

  const toList = (rackMap: Record<string, RackInfo>): RackInfo[] => {
    const racks: RackInfo[] = [];
    let index = 0;

    for (const rackKey in rackMap) {
      if (rackMap.hasOwnProperty(rackKey)) {
        const rack: RackInfo = {
          name: rackKey,
          rackId: rackKey,
          hosts: rackMap[rackKey].hosts,
          isLoaded: false,
          index: index++,
        };
        racks.push(rack);
      }
    }

    return racks;
  };

  const loadRacks = useCallback(async () => {
    try {
      const data = await MetricsApi.getHostsForHeatmaps(clusterName);
      const {  rackMap: newRackMap, racks: newRacks } = processHostsData(data);

      setRackMap(newRackMap);
      setRacks(newRacks);

      return true;
    } catch (error) {
      console.error("Error loading racks:", error);
      return false;
    }
  }, [clusterName]);

  const loadPageData = useCallback(async () => {
    setMetricsLoading(true);

    try {
      const racksLoaded = await loadRacks();

      if (racksLoaded) {
        const data = await MetricsApi.getHeatmapWidgets(clusterName, serviceName);
        const allMetrics = map(data?.items, (item: any) => {
          item.WidgetInfo.metrics = JSON.parse(item.WidgetInfo.metrics);
          item.WidgetInfo.properties = JSON.parse(item.WidgetInfo.properties);
          item.WidgetInfo.values = JSON.parse(item.WidgetInfo.values);
          return item;
        });

        createHostPercentageWidgets();
        const combinedMetrics = [...map(allMetrics, "WidgetInfo")];

        setMetrics(combinedMetrics);
        setSelectedMetric(allMetrics?.[0]?.WidgetInfo || null);
        setIsLoaded(true);
      }
    } catch (error) {
      console.error("Error loading page data:", error);
    } finally {
      setMetricsLoading(false);
    }
  }, [clusterName, serviceName, loadRacks]);

  const loadMetrics = useCallback(async () => {
    if (!selectedMetric || !selectedMetric.metrics) {
      return;
    }

    const metrics: any[] = [];
    let requestCounter = 0;
    const totalRequests = selectedMetric.metrics.length;

    for (const metric of selectedMetric.metrics) {
      requestCounter++;

      try {
        let data;
        if (metric.service_name === "STACK") {
          data = await MetricsApi.getHostsMetrics(clusterName, [metric.metric_path]);

          data.items.forEach((item: any) => {
            const metricCopy = { ...metric };
            metricCopy.hostName = item.Hosts.host_name;
            const metricPath = metric.metric_path.replace(/\//g, ".");
            const metricValue = getNestedProperty(item, metricPath);
            if (metricValue != null) {
              metricCopy.data = metricValue;
              metricCopy.name = metric.name;
              metricCopy.metric_path = metric.metric_path;
              metrics.push(metricCopy);
            }
          });
        } else {
          data = await MetricsApi.getHostComponentsMetrics(
            clusterName,
            metric.service_name,
            metric.component_name,
            [metric.metric_path]
          );

          if (data.host_components) {
            data.host_components.forEach((item: any) => {
              const metricCopy = { ...metric };
              metricCopy.hostName = item.HostRoles.host_name;
              const metricPath = metric.metric_path.replace(/\//g, ".");
              const metricValue = getNestedProperty(item, metricPath);
              if (metricValue != null) {
                metricCopy.data = metricValue;
                metricCopy.name = metric.name;
                metricCopy.metric_path = metric.metric_path;
                metrics.push(metricCopy);
              }
            });
          }
        }
      } catch (error) {
        console.error("Error loading metric:", metric, error);
      }

      if (requestCounter === totalRequests) {
        setMetricsData(metrics);
        onMetricsLoaded(metrics);
      }
    }
  }, [selectedMetric, clusterName]);

  const getNestedProperty = (obj: any, path: string): any => {
    return path.split(".").reduce((current, key) => current?.[key], obj);
  };

  const onMetricsLoaded = useCallback(
    (metrics: any[]) => {
      if (!selectedMetric || !selectedMetric.values || !selectedMetric.values[0]) {
        return;
      }

      try {
        const expressions = extractExpressions(selectedMetric.values[0]);
        const hostToValueMap = computeHeatmapExpression(expressions, metrics);

        const hostNames: string[] = [];
        racks.forEach((rack) => {
          hostNames.push(...rack.hosts.map((host) => host.hostName));
        });

        const heatmapMetricObj = createHeatmapMetric(
          selectedMetric.widget_name,
          selectedMetric.properties?.display_unit || "",
          parseFloat(selectedMetric.properties?.max_limit) || 100,
          hostNames,
          hostToValueMap
        );

        setHeatmapMetric(heatmapMetricObj);
      } catch (error) {
        console.error("Error processing metrics:", error);
      }
    },
    [selectedMetric, racks]
  );

  const handleInputMaximumChange = useCallback(
    (value: string) => {
      setInputMaximum(value);
      if (heatmapMetric && /^\d+$/.test(value)) {
        const newMaxValue = parseFloat(value);
        const updatedHeatmapMetric = createHeatmapMetric(
          heatmapMetric.name,
          heatmapMetric.units,
          newMaxValue,
          heatmapMetric.hostNames,
          heatmapMetric.hostToValueMap
        );
        setHeatmapMetric(updatedHeatmapMetric);
      }
    },
    [heatmapMetric]
  );

  const handleHostClick = useCallback((host: any) => {
    navigate("/main/hosts/" + host.hostName + "/summary");
  }, [navigate]);

  const getSelectedMetricValue = useCallback(async () => {
    if (!selectedMetric) {
      return null;
    }
    loadMetrics();
  }, [selectedMetric, loadMetrics]);

  usePolling(getSelectedMetricValue, 5000);

  useEffect(() => {
    loadPageData();
  }, [loadPageData]);

  useEffect(() => {
    if (selectedMetric && isLoaded) {
      loadMetrics();
    }
  }, [selectedMetric, isLoaded, loadMetrics]);

  useEffect(() => {
    if (heatmapMetric && racks.length > 0) {
      const updatedRacks = racks.map((rack) => ({ ...rack, isLoaded: true }));
      setRacks(updatedRacks);
    }
  }, [heatmapMetric]);

  if (metricsLoading) {
    return <Spinner />;
  }

  return (
    <div className="heatmap">
      {isLoaded ? (
        <Row className="mt-4">
          <Col md={3} className="legend-column">
            <div className="mb-3">
              <Card>
                <CardBody>
                  <Dropdown>
                    <Dropdown.Toggle
                      variant="transparent"
                      className="w-100 text-start border fs-12"
                    >
                      {selectedMetric ? selectedMetric.widget_name : "SELECT METRIC"}
                    </Dropdown.Toggle>

                    <Dropdown.Menu className="w-100">
                      {metrics.map((metric) => (
                        <Dropdown.Item
                          key={metric.id}
                          onClick={() => setSelectedMetric(metric)}
                        >
                          {metric.widget_name}
                        </Dropdown.Item>
                      ))}
                    </Dropdown.Menu>
                  </Dropdown>
                  {selectedMetric && heatmapMetric && (
                    <HeatmapLegend
                      selectedMetric={heatmapMetric}
                      slotDefinitions={heatmapMetric.slotDefinitions || []}
                      inputMaximum={
                        inputMaximum ||
                        heatmapMetric.maximumValue?.toString() ||
                        "100"
                      }
                      onInputMaximumChange={handleInputMaximumChange}
                    />
                  )}
                </CardBody>
              </Card>
            </div>
          </Col>

          <Col md={9}>
            {heatmapMetric && racks.length > 0 ? (
              <div>
                <div className="mb-3">
                  <h1 className="p-2 rounded">
                    {selectedMetric?.widget_name || "Heatmap"}
                  </h1>
                </div>

                <HeatmapGrid
                  racks={racks}
                  hostToSlotMap={heatmapMetric.hostToSlotMap || {}}
                  hostToValueMap={heatmapMetric.hostToValueMap || {}}
                  slotDefinitions={heatmapMetric.slotDefinitions || []}
                  units={heatmapMetric.units}
                  onHostClick={handleHostClick}
                  selectedMetric={selectedMetric}
                />
              </div>
            ) : (
              <div className="text-center py-5">
                <div className="text-muted">
                  {selectedMetric
                    ? "Loading heatmap data..."
                    : "Please select a metric to view the heatmap"}
                </div>
              </div>
            )}
          </Col>
        </Row>
      ) : (
        <div className="text-center py-5">
          <Spinner />
        </div>
      )}
    </div>
  );
}

export default Heatmaps;
