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

import { Card, CardBody, Col, Row } from "react-bootstrap";
import ServiceAlerts from "./ServiceAlerts";
import ServiceComponents from "./ServiceComponents";
import ServiceMetrics from "./ServiceMetrics";
import OptimizedServiceQuicklinks from "./OptimizedServiceQuicklinks";
import HDFSFederationSummary from "./HDFSFederationSummary";
import { filter, find, get, isNumber, isObject, map } from "lodash";
import { AlertsApi } from "../../api/alertsApi";
import { useContext, useEffect, useState } from "react";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";
import usePolling from "../../hooks/usePolling";
import { centralizedServiceStateApi } from "../../api/centralizedServiceStateApi";

type SummaryProps = {
  serviceName: string;
  selectedTab: string;
};
function ServiceSummary({ serviceName, selectedTab }: SummaryProps) {
  const [alerts, setAlerts] = useState<any>([]);
  const [alertsCount, setAlertsCount] = useState<number>(0);
  const { clusterName, isClusterInstalled } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);

  const STATUS_PRIORITY_ORDER = [
    "CRITICAL",
    "OK",
    "WARNING",
    "UNKNOWN",
    "NONE",
  ];

  async function getAlerts() {
    const alertDefinitionsFields = `AlertDefinition/component_name,AlertDefinition/description,AlertDefinition/enabled,AlertDefinition/repeat_tolerance,AlertDefinition/repeat_tolerance_enabled,AlertDefinition/id,AlertDefinition/ignore_host,AlertDefinition/interval,AlertDefinition/label,AlertDefinition/name,AlertDefinition/scope,AlertDefinition/service_name,AlertDefinition/source,AlertDefinition/help_url`;
    const alertDefinitions = await AlertsApi.getAlertDefinition(
      clusterName,
      alertDefinitionsFields,
      Date.now()
    );
    const { items } = alertDefinitions;

    // FIXED: Get ALL alerts (including maintenance mode) to properly display them with maintenance styling
    const { alerts_summary_grouped: allGroupedAlerts } =
      await AlertsApi.getGroupFormattedAlertsNotifications(clusterName);
    
    const alertsForSelectedService = filter(items, [
      "AlertDefinition.service_name",
      serviceName,
    ]);

    const inferredAlerts = map(alertsForSelectedService, (alert: any) => {
      const matchingAlert = find(allGroupedAlerts, [
        "definition_id",
        get(alert, "AlertDefinition.id", ""),
      ]);

      return {
        label: get(alert, "AlertDefinition.label", ""),
        name: get(alert, "AlertDefinition.name", ""),
        description: get(alert, "AlertDefinition.description", ""),
        id: get(alert, "AlertDefinition.id", ""),
        component_name: get(alert, "AlertDefinition.component_name", ""),
        summary: matchingAlert
          ? matchingAlert?.summary
          : {
              NONE: { count: 1, maintenance_count: 0 },
              CRITICAL: { count: 0, maintenance_count: 0 },
              WARNING: { count: 0, maintenance_count: 0 },
              UNKNOWN: { count: 0, maintenance_count: 0 },
              OK: { count: 0, maintenance_count: 0 },
            },
        highestStatus: "",
      };
    });
    for (const alert of inferredAlerts) {
      const statuses =
        alert && isObject(alert) && alert.summary
          ? Object.keys(alert?.summary).map((status: any) => ({
              status,
              count: alert.summary[status].count,
              maintenance_count: alert.summary[status].maintenance_count,
            }))
          : [];
      
      // FIXED: Determine highest status based on both regular and maintenance alerts
      // This preserves the actual alert status even when in maintenance mode
      let highestStatus = "none";
      const statusOrder = ["critical", "warning", "ok", "unknown"];
      
      for (const priorityStatus of statusOrder) {
        const statusItem = statuses.find(s => s.status.toLowerCase() === priorityStatus);
        if (statusItem && (statusItem.count > 0 || statusItem.maintenance_count > 0)) {
          highestStatus = priorityStatus;
          break;
        }
      }
      
      alert.highestStatus = highestStatus.toUpperCase();
    }
    const sortedInferredAlerts = STATUS_PRIORITY_ORDER.map((status) => {
      return inferredAlerts.filter(
        (alert: any) => alert.highestStatus === status
      );
    }).flat();
    setAlerts(sortedInferredAlerts);
  }
  usePolling(getAlerts, 30000);
  
  useEffect(() => {
    if (isClusterInstalled) {
      setAlertsCount(0);
      getAlerts();
    }
  }, [serviceName, isClusterInstalled]);

  useEffect(() => {
    if (!clusterName || !serviceName) return;

    const unsubscribe = centralizedServiceStateApi.subscribe((serviceStatesData) => {
      const serviceStateData = serviceStatesData.get(serviceName);
      if (serviceStateData) {
        getAlerts();
      }
    });

    return unsubscribe;
  }, [clusterName, serviceName]);
  useEffect(() => {
    if (alerts.length) {
      let inferredAlertsCount = 0;
      for (const alert of alerts) {
        // FIXED: Only count non-maintenance alerts like Ember (exclude maintenance_count from visible counts)
        const criticalAlerts = isNumber(alert?.summary?.["CRITICAL"]?.count)
          ? alert?.summary?.["CRITICAL"]?.count
          : 0;
        const warningAlerts = isNumber(alert?.summary?.["WARNING"]?.count)
          ? alert?.summary?.["WARNING"]?.count
          : 0;
        inferredAlertsCount =
          inferredAlertsCount + criticalAlerts + warningAlerts;
      }
      setAlertsCount(inferredAlertsCount);
    } else {
      setAlertsCount(0);
    }
  }, [alerts]);
  return (
    <div className="mt-2">
      {/* Restart Warning - Requires SERVICE.START_STOP authorization like Ember.js ui/app/templates/main/service/info/summary.hbs */}
      <Row>
        <Col md={10}>
          <Card>
            <CardBody>
              <div className="d-flex justify-content-between align-items-center mt-2">
                <h2>Summary</h2>
                <ServiceAlerts
                  serviceName={serviceName}
                  alerts={alerts}
                  alertsCount={alertsCount}
                />
              </div>
              {serviceName?.toLowerCase() === "hdfs" &&
              allServiceModels?.["hdfs"]?.federationNamespaces &&
              allServiceModels["hdfs"].federationNamespaces.length > 1 ? (
                <Row className="mt-4">
                  <Col>
                    <HDFSFederationSummary
                      hdfsModel={allServiceModels["hdfs"]}
                      masterComponents={
                        allServiceModels["hdfs"]?.masterComponents || []
                      }
                      alerts={alerts}
                    />
                  </Col>
                </Row>
              ) : (
                <>
                  <ServiceComponents
                    serviceName={serviceName}
                    alerts={alerts}
                  />
                  <ServiceMetrics serviceName={serviceName} />
                </>
              )}
            </CardBody>
          </Card>
        </Col>
        <Col md={2}>
          <Card className="h-100">
            <CardBody>
              <OptimizedServiceQuicklinks
                serviceName={serviceName}
                selectedTab={selectedTab}
              />
            </CardBody>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default ServiceSummary;
