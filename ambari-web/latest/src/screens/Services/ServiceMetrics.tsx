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

import { cloneDeep, isEmpty, isObject } from "lodash";
import {
  ServiceMetricMap,
  serviceMetricsGroups,
  serviceMetricsMap,
} from "./constants";
import {  Col, Row } from "react-bootstrap";
import { useContext, useEffect } from "react";
import { ServiceContext } from "../../store/ServiceContext";
import Spinner from "../../components/Spinner";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCopy } from "@fortawesome/free-solid-svg-icons";
import toast from "react-hot-toast";

function ServiceMetrics({ serviceName }: { serviceName: string }) {
  const { allServiceModels } = useContext(ServiceContext);

  const stringifiedModel = JSON.stringify(
    allServiceModels?.[serviceName.toLowerCase()] || {}
  );

  useEffect(() => {
    if (allServiceModels[serviceName.toLowerCase()]) {
      renderMetrics();
    }
  }, [stringifiedModel]);
  function renderMetrics() {
    const selectedServiceGroups = cloneDeep(
      serviceMetricsGroups[serviceName.toLowerCase()]
    );
    if (selectedServiceGroups && selectedServiceGroups.length > 0) {
      for (let group of selectedServiceGroups) {
        const groupMetrics = serviceMetricsMap[
          serviceName.toLowerCase()
        ].filter((metric: ServiceMetricMap) => metric.group_id === group.id);
        group.metrics = groupMetrics;
      }
    }
    return selectedServiceGroups?.map((group: any) => {
      return (
        <div key={group.id}>
          <h3 className="text-uppercase summary-metrics-group-name fs-12 text-light">
            {group.display_name}
          </h3>
          <Row>
            {group.metrics.map((metric: ServiceMetricMap) => {
              const metricValue =
                allServiceModels[serviceName.toLowerCase()][metric.modelKey];
              const metricDescriptionValue =
                allServiceModels[serviceName.toLowerCase()][
                  metric?.descriptionModelKey as string
                ];
              return (
                <Col md={2} key={metric.display_name}>
                  <div>
                    <div>
                      <h3 className="text-dark">
                        {isObject(metricValue)
                          ? isEmpty(metricValue)
                            ? "N/A"
                            : JSON.stringify(metricValue)
                          : metricValue}
                      </h3>
                      <h5 className="metric-description">
                        {metric.descriptionModelKey
                          ? metricDescriptionValue
                            ? metricDescriptionValue
                            : metric.description
                          : metric.description}
                      </h5>
                      <p className="text-uppercase fs-12 text-light">
                        {metric.display_name}
                      </p>
                    </div>
                  </div>
                </Col>
              );
            })}
          </Row>
        </div>
      );
    });
  }

  if (!serviceMetricsGroups[serviceName.toLowerCase()]) {
    return null;
  }

  if (!allServiceModels[serviceName.toLowerCase()]) {
    return <Spinner />;
  }

  const copyToClipboard = async (text: string) => {
    try {
      // Check if clipboard API is available (HTTPS required)
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        console.log("Copied to clipboard");
        return;
      }
      
      // Fallback for HTTP or older browsers
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      
      try {
        document.execCommand('copy');
        console.log("Copied to clipboard using fallback");
      } catch (fallbackErr) {
        console.error("Fallback copy failed: ", fallbackErr);
        // Show user a message to manually copy
        alert(`Please copy this manually: ${text}`);
      }
      
      document.body.removeChild(textArea);
    } catch (err) {
      console.error("Failed to copy: ", err);
      // Final fallback - show text for manual copy
      alert(`Please copy this manually: ${text}`);
    }
  };

  // Check if there are any actual metrics to display for this service
  const hasActualMetrics =
    serviceMetricsMap[serviceName.toLowerCase()] &&
    serviceMetricsMap[serviceName.toLowerCase()].length > 0;

  return (
    <Row className="mt-4">
      {serviceName.toUpperCase() === "HIVE" ? (
        // Special handling for Hive JDBC URL - full width with display flex
        <Col md={12}>
          <div className="d-flex align-items-center">
            <div className="d-flex align-items-center text-nowrap">
              HIVESERVER2 JDBC URL
              {/* <Button
                variant="light"
                size="sm"
                className="ms-2 p-1 border"
                onClick={() =>
                  copyToClipboard(
                    allServiceModels["hive"]?.hiveServer2JDBCURL || ""
                  )
                }
                title="Copy JDBC URL"
              >
              </Button> */}
            </div>
            <div className="text-break ms-3">
              {allServiceModels["hive"]?.hiveServer2JDBCURL ||
                "Loading JDBC URL..."}
              <FontAwesomeIcon
                onClick={() => {
                  copyToClipboard(
                    allServiceModels["hive"]?.hiveServer2JDBCURL || ""
                  );
                  toast.success("Copied to clipboard", { duration: 2000 });
                }}
                icon={faCopy}
                className="fs-6 ms-2 cursor-pointer"
              />
            </div>

            {/* <div className="d-flex align-items-center p-3 bg-light border rounded">
              <div className="flex-grow-1">
                <code className="text-break">
                  {allServiceModels["hive"]?.hiveServer2JDBCURL ||
                    "Loading JDBC URL..."}
                </code>
              </div>
            </div> */}
          </div>
          {/* Only render Service Metrics section if there are actual metrics */}
          {hasActualMetrics && (
            <div className="mt-4">
              <h3>Service Metrics</h3>
              {renderMetrics()}
            </div>
          )}
        </Col>
      ) : (
        // Regular service metrics layout for non-Hive services
        <>
          <Col md={2}>
            <h3>Service Metrics</h3>
          </Col>
          <Col>{renderMetrics()}</Col>
        </>
      )}
    </Row>
  );
}

export default ServiceMetrics;
