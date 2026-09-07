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
import EmbeddedDashboards from "../Monitoring/EmbeddedDashboards";
import DashboardConfigHistory from "./ConfigHistory";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { useAuth } from "../../hooks/useAuth";

const dashboardTabs = ["metrics", "confighistory"];

function Dashboard({ tabName: tabNameProp }: { tabName?: string }) {
  const navigate = useNavigate();
  const { tabName: tabNameParam } = useParams<{ tabName: string }>();
  const { hasAuthorization } = useAuth();
  const canViewMetrics = hasAuthorization("CLUSTER.VIEW_METRICS");
  const defaultTab = canViewMetrics ? "metrics" : "confighistory";
  const [activeTab, setActiveTab] = useState(defaultTab);
  const tabName = tabNameProp || tabNameParam;
  
  useEffect(() => {
    if (
      tabName &&
      dashboardTabs.includes(tabName) &&
      (tabName !== "metrics" || canViewMetrics)
    ) {
      setActiveTab(tabName);
      return;
    }

    setActiveTab(defaultTab);
    navigate(`/main/dashboard/${defaultTab}`, { replace: true });
  }, [canViewMetrics, defaultTab, navigate, tabName]);
  
  // Handle tab selection and update URL
  const handleTabSelect = (key: string | null) => {
    if (key) {
      setActiveTab(key);
      navigate(`/main/dashboard/${key}`);
    }
  };

  return (
    <div className="p-4">
      <Row>
        <Col md={12} style={{ position: "relative" }}>
          <Tabs 
            id="service-tabs" 
            className="ambari-tabs mb-3"
            activeKey={activeTab}
            onSelect={handleTabSelect}
          >
            {canViewMetrics && (
              <Tab eventKey="metrics" title="METRICS">
                {activeTab === "metrics" ? <EmbeddedDashboards location="Dashboard" includeAllFallback /> : null}
              </Tab>
            )}
            <Tab eventKey="confighistory" title="CONFIG HISTORY">
              <DashboardConfigHistory />
            </Tab>
          </Tabs>
        </Col>
      </Row>
    </div>
  );
}

export default Dashboard;
