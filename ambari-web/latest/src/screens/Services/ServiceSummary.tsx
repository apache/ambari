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
import OptimizedServiceQuicklinks from "./OptimizedServiceQuicklinks";
import { find, isNumber, isObject } from "lodash";
import { useEffect, useState } from "react";
import { useAlerts } from "../../store/AlertsContext";

type SummaryProps = {
  serviceName: string;
  selectedTab: string;
};
function ServiceSummary({ serviceName, selectedTab }: SummaryProps) {
  const [alerts, setAlerts] = useState<any>([]);
  const [alertsCount, setAlertsCount] = useState<number>(0);

  // FOLLOWING EMBERJS PATTERN: Get alert data from useAlerts hook (WebSocket updates)
  // EmberJS: App.AlertDefinition.find() from Ember Data store
  // - Loaded once via updateAlertDefinitions() and updateAlertDefinitionSummary()
  // - Updated via WebSocket /events/alerts
  // - NO POLLING on service summary page
  const { alertDefinitions, alertSummary } = useAlerts();

  const STATUS_PRIORITY_ORDER = [
    "CRITICAL",
    "OK",
    "WARNING",
    "UNKNOWN",
    "NONE",
  ];

  // REMOVED getAlerts() and polling - using WebSocket data from useAlerts hook
  // EmberJS pattern: service summary uses App.AlertDefinition.find() from store
  // which is populated once and updated via WebSocket /events/alerts

  // Process alerts when alertDefinitions or alertSummary changes (from WebSocket)
  useEffect(() => {
    if (!alertDefinitions || !alertSummary || !serviceName) {
      setAlerts([]);
      return;
    }

    const allGroupedAlerts = alertSummary?.alerts_summary_grouped || [];

    // Filter alert definitions for this service
    const alertsForSelectedService = alertDefinitions.filter(
      (def: any) => def.service_name === serviceName
    );

    // Map definitions to alerts with summary data
    const inferredAlerts = alertsForSelectedService.map((alert: any) => {
      const matchingAlert = find(allGroupedAlerts, [
        "definition_id",
        alert.id,
      ]);

      return {
        label: alert.label || "",
        name: alert.name || "",
        description: alert.description || "",
        id: alert.id || "",
        component_name: alert.component_name || "",
        summary: matchingAlert
          ? matchingAlert.summary
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

    // Determine highest status for each alert
    for (const alert of inferredAlerts) {
      const statuses =
        alert && isObject(alert) && alert.summary
          ? Object.keys(alert.summary).map((status: any) => ({
              status,
              count: alert.summary[status].count,
              maintenance_count: alert.summary[status].maintenance_count,
            }))
          : [];

      let highestStatus = "none";
      const statusOrder = ["critical", "warning", "ok", "unknown"];

      for (const priorityStatus of statusOrder) {
        const statusItem = statuses.find(
          (s) => s.status.toLowerCase() === priorityStatus
        );
        if (
          statusItem &&
          (statusItem.count > 0 || statusItem.maintenance_count > 0)
        ) {
          highestStatus = priorityStatus;
          break;
        }
      }

      alert.highestStatus = highestStatus.toUpperCase();
    }

    // Sort by status priority
    const sortedInferredAlerts = STATUS_PRIORITY_ORDER.map((status) => {
      return inferredAlerts.filter(
        (alert: any) => alert.highestStatus === status
      );
    }).flat();

    setAlerts(sortedInferredAlerts);
  }, [alertDefinitions, alertSummary, serviceName]);
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
              <ServiceComponents serviceName={serviceName} alerts={alerts} />
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
