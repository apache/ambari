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
import { useNavigate, useParams, useLocation } from "react-router-dom";
import "./styles/services.scss";
import { useContext, useEffect, useState } from "react";
import { AppContext } from "../../store/context";
import { map } from "lodash";
import Heatmaps from "../Heatmaps";
import { useAuth } from "../../hooks/useAuth";
// import AuthGuard from "../../components/AuthGuard";
// import RestartWarning from "./RestartWarning";

enum TabNames {
  SUMMARY = "summary",
  HEATMAPS = "heatmaps",
  CONFIGS = "configs",
  METRICS = "metrics",
}

function ServiceDashboard({
  serviceName: serviceNameProps,
}: {
  serviceName?: string;
}) {
  // Authorization hooks - implementing Ember.js hasConfigTab logic
  const { havePermissions } = useAuth();
  const { upgradeState } = useContext(AppContext);

  const configTabsUpgradeBlocked = [
    "IN_PROGRESS",
    "PENDING",
    "HOLDING_FAILED",
    "HOLDING_TIMEDOUT",
    "HOLDING",
  ].includes(upgradeState);

  // Check CLUSTER.VIEW_CONFIGS permission like in Ember.js ui/app/views/main/service/item.js
  //@ts-ignore
  const hasConfigTab =
    havePermissions("CLUSTER.VIEW_CONFIGS") &&
    !configTabsUpgradeBlocked&&
    !upgradeState?.toLowerCase()?.includes("holding");
  const serviceTabs: { [key: string]: string[] } = {
    HDFS: ["summary", "heatmaps", "configs", "metrics"],
    YARN: ["summary", "heatmaps", "configs", "metrics"],
    HIVE: ["summary", "configs"],
    KAFKA: ["summary", "configs"],
    ZOOKEEPER: ["summary", "configs"],
    HBASE: ["summary", "heatmaps", "configs", "metrics"],
    ATLAS: ["summary", "configs"],
    RANGER: ["summary", "configs"],
    RANGER_KMS: ["summary", "configs"],
    SOLR: ["summary", "configs"],
    FLUME: ["summary", "configs"],
    AMBARI_METRICS: ["summary", "configs", "metrics"],
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

  const { serviceName: serviceNameParams } = useParams();
  const serviceName = serviceNameParams || serviceNameProps;
  const { tabName } = useParams();
  const [selectedTab, setSelectedTab] = useState(tabName);
  const { services, clusterName } = useContext(AppContext);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    // Don't run URL replacement logic if services are still loading
    // This prevents serviceName from becoming undefined during the race condition
    if (!services || services.length === 0) {
      return;
    }
    
    if (
      serviceNameParams &&
      !selectedServices.includes(serviceNameParams) &&
      location.pathname.includes(serviceNameParams)
    ) {
      if (clusterName) {
        navigate(
          location.pathname.replace(serviceNameParams, selectedServices?.[0])
        );
      } else {
        navigate("/installer/step0");
      }
    } else {
      if (!selectedTab) {
        setSelectedTab(TabNames.SUMMARY);
      }
      if (
        serviceName &&
        selectedTab &&
        !serviceTabs[serviceName.toUpperCase()]?.includes(selectedTab)
      ) {
        setSelectedTab(TabNames.SUMMARY);
      }
    }
  }, [serviceName, serviceNameParams, location, services]); // Add services to dependencies

  return (
    <div className="p-4">
      <Row>
        <Col md={12} style={{ position: "relative" }}>
          <Tabs
            id="service-tabs"
            className="ambari-tabs"
            activeKey={selectedTab}
            onSelect={(tab: any) => {
              navigate(`/main/services/${serviceName}/${tab}`);
              setSelectedTab(tab);
            }}
          >
            <Tab eventKey="summary" title="Summary">
              <div className="mt-2" />
              {/* <AuthGuard requireAuthorization="SERVICE.START_STOP">
                <RestartWarning serviceName={serviceName!} />
              </AuthGuard>
              <ServiceSummary
                serviceName={serviceName as string}
                //@ts-ignore
                selectedTab={selectedTab}
              /> */}
            </Tab>
            {serviceName &&
              serviceTabs[serviceName.toUpperCase()]?.includes(
                TabNames.HEATMAPS
              ) && (
                <Tab eventKey="heatmaps" title="Heatmaps">
                  {selectedTab === TabNames.HEATMAPS ? (
                    <Heatmaps serviceName={serviceName as string} />
                  ) : null}
                </Tab>
              )}
            {/* Configs Tab - Requires CLUSTER.VIEW_CONFIGS permission like Ember.js ui/app/views/main/service/item.js */}
            {/* {hasConfigTab && (
              <Tab eventKey={`configs`} title="configs">
                {selectedTab === "configs" ? (
                  <div className="mt-2">
                  <RestartWarning serviceName={serviceName!} />
                    <ServiceConfigs serviceName={serviceName as string} />
                  </div>
                ) : null}
              </Tab>
            )} */}
            {/* {serviceName &&
              serviceTabs[serviceName.toUpperCase()]?.includes(
                TabNames.METRICS
              ) && (
                <Tab eventKey="metrics" title="Metrics">
                  <Metrics serviceName={serviceName as string} />
                </Tab>
              )} */}
          </Tabs>
          {/* <Actions serviceName={serviceName!} className="action-btn" /> */}
        </Col>
      </Row>
    </div>
  );
}

export default ServiceDashboard;
