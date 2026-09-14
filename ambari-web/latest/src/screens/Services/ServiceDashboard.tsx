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

import { Col, Row, Tab, Tabs } from "react-bootstrap";
import ServiceSummary from "./ServiceSummary";
import { useNavigate, useParams } from "react-router-dom";
import "./styles/services.scss";
import ServiceConfigs from "../ServiceConfigs";
import { Actions } from "./Actions";
import { useContext, useEffect, useState } from "react";
import EmbeddedDashboards from "../Monitoring/EmbeddedDashboards";
import { AppContext } from "../../store/context";
import { map } from "lodash";
import { useAuth } from "../../hooks/useAuth";
import AuthGuard from "../../components/AuthGuard";
import RestartWarning from "./RestartWarning";
import { resolveServiceNavigation } from "../../Utils/serviceNavigation";
import useClusterPath from "../../hooks/useClusterPath";
import { clusterDraftPath } from "../../Utils/scopedWorkflow";
import ServiceDependencies from "./ServiceDependencies";
import ServiceDependents from "./ServiceDependents";
import { useTranslation } from "react-i18next";

enum TabNames {
  SUMMARY = "summary",
  CONFIGS = "configs",
  METRICS = "metrics",
  DEPENDENCIES = "dependencies",
  DEPENDENTS = "dependents",
}

const serviceTabs: Record<string, string[]> = {
  HDFS: ["summary", "configs", "metrics", "dependents"],
  YARN: ["summary", "configs", "metrics"],
  HIVE: ["summary", "configs", "metrics"],
  KAFKA: ["summary", "configs"],
  ZOOKEEPER: ["summary", "configs", "dependents"],
  HBASE: ["summary", "configs", "metrics", "dependencies"],
  ATLAS: ["summary", "configs"],
  RANGER: ["summary", "configs"],
  RANGER_KMS: ["summary", "configs"],
  SOLR: ["summary", "configs"],
  FLUME: ["summary", "configs"],
  VICTORIAMETRICS: ["summary", "configs"],
  AMBARI_INFRA_SOLR: ["summary", "configs"],
  LIVY: ["summary", "configs"],
  TEZ: ["summary", "configs"],
  KERBEROS: ["summary", "configs"],
  MAPREDUCE2: ["summary", "configs"],
  SQOOP: ["summary", "configs"],
  SPARK3: ["summary", "configs"],
  KYUUBI: ["summary", "configs"],
  TRINO: ["summary", "configs"],
  TRINO_GATEWAY: ["summary", "configs"],
  SSM: ["summary", "configs"],
  PINOT: ["summary", "configs"],
};

export function ServiceIndexRedirect() {
  const { services, clusterName } = useContext(AppContext);
  const navigate = useNavigate();
  const scopedPath = useClusterPath();

  useEffect(() => {
    if (!clusterName || !services?.length) {
      return;
    }
    const firstService = map(services, "ServiceInfo.service_name").find(Boolean);
    if (firstService) {
      navigate(scopedPath(`/main/services/${firstService}/summary`), { replace: true });
    }
  }, [clusterName, navigate, scopedPath, services]);

  return null;
}

function ServiceDashboard({
  serviceName: serviceNameProps,
}: {
  serviceName?: string;
}) {
  const { t } = useTranslation();
  // Authorization hooks - implementing Ember.js hasConfigTab logic
  const { hasAuthorization } = useAuth();
  // Check CLUSTER.VIEW_CONFIGS permission like in Ember.js ui/app/views/main/service/item.js
  const hasConfigTab = hasAuthorization("CLUSTER.VIEW_CONFIGS");
  const canViewMetrics = hasAuthorization("SERVICE.VIEW_METRICS");
  const { serviceName: serviceNameParams } = useParams();
  const serviceName = serviceNameParams || serviceNameProps;
  const { tabName } = useParams();
  const [selectedTab, setSelectedTab] = useState(tabName || TabNames.SUMMARY);
  const { services, clusterName } = useContext(AppContext);
  const navigate = useNavigate();
  const scopedPath = useClusterPath();
  const [installerPath] = useState(() => clusterDraftPath());

  useEffect(() => {
    // Don't run URL replacement logic if services are still loading
    // This prevents serviceName from becoming undefined during the race condition
    if (!services || services.length === 0) {
      return;
    }
    const selectedServices = map(services, "ServiceInfo.service_name");
    
    const selection = resolveServiceNavigation({
      availableTabs: serviceTabs,
      canViewConfigs: hasConfigTab,
      canViewMetrics,
      installedServices: selectedServices,
      requestedService: serviceName,
      requestedTab: tabName,
    });
    setSelectedTab(selection.selectedTab);

    if (serviceNameParams && selection.redirectPath) {
      navigate(clusterName ? scopedPath(selection.redirectPath) : installerPath, {
        replace: true,
      });
    }
  }, [
    clusterName,
    canViewMetrics,
    hasConfigTab,
    installerPath,
    navigate,
    serviceName,
    serviceNameParams,
    services,
    scopedPath,
    tabName,
  ]);

  return (
    <div className="p-4">
      <Row>
        <Col md={12} style={{ position: "relative" }}>
          <Tabs
            id="service-tabs"
            className="ambari-tabs"
            activeKey={selectedTab}
            onSelect={(tab) => {
              if (!tab) return;
              navigate(scopedPath(`/main/services/${serviceName}/${tab}`));
              setSelectedTab(tab);
            }}
          >
            <Tab eventKey="summary" title="Summary">
              <div className="mt-2" />
              <AuthGuard requireAuthorization="SERVICE.START_STOP">
                <RestartWarning serviceName={serviceName!} />
              </AuthGuard>
              <ServiceSummary
                serviceName={serviceName as string}
                //@ts-ignore
                selectedTab={selectedTab}
              />
            </Tab>
            {/* Configs Tab - Requires CLUSTER.VIEW_CONFIGS permission like Ember.js ui/app/views/main/service/item.js */}
            {hasConfigTab && (
              <Tab eventKey={`configs`} title="configs">
                {selectedTab === "configs" ? (
                  <div className="mt-2">
                  <RestartWarning serviceName={serviceName!} />
                    <ServiceConfigs serviceName={serviceName as string} />
                  </div>
                ) : null}
              </Tab>
            )}
            {serviceName &&
              canViewMetrics &&
              serviceTabs[serviceName.toUpperCase()]?.includes(
                TabNames.METRICS
              ) && (
                <Tab eventKey="metrics" title="Metrics">
                  {selectedTab === TabNames.METRICS ? <EmbeddedDashboards location={serviceName as string} /> : null}
                </Tab>
              )}
            {serviceName?.toUpperCase() === "HBASE" ? (
              <Tab eventKey={TabNames.DEPENDENCIES} title={t("serviceDependencies.tab")}>
                {selectedTab === TabNames.DEPENDENCIES ? <ServiceDependencies /> : null}
              </Tab>
            ) : null}
            {serviceName && ["HDFS", "ZOOKEEPER"].includes(serviceName.toUpperCase()) ? (
              <Tab eventKey={TabNames.DEPENDENTS} title={t("serviceDependents.tab")}>
                {selectedTab === TabNames.DEPENDENTS ? (
                  <ServiceDependents serviceName={serviceName.toUpperCase()} />
                ) : null}
              </Tab>
            ) : null}
          </Tabs>
          <Actions serviceName={serviceName!} className="action-btn" />
        </Col>
      </Row>
    </div>
  );
}

export default ServiceDashboard;
