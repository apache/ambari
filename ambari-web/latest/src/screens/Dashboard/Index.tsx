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

// @ts-nocheck
import { Col, Row, Tab, Tabs } from "react-bootstrap";
import DashboardMetrics from "./Metrics";
import DashboardConfigHistory from "./ConfigHistory";
import DashboardHeatmaps from "./DashboardHeatmaps";
import { useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";

function Dashboard() {
  const navigate = useNavigate();
  const { tabName } = useParams<{ tabName: string }>();
  const [activeTab, setActiveTab] = useState("metrics");
  
  // Set the active tab based on the tabName parameter from the URL
  useEffect(() => {
    if (tabName) {
      setActiveTab(tabName);
    } else {
      setActiveTab("metrics");
    }
  }, [tabName]);
  
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
            <Tab eventKey="metrics" title="METRICS">
              <DashboardMetrics />
            </Tab>
            <Tab eventKey="heatmaps" title="HEATMAPS">
              <DashboardHeatmaps />
            </Tab>
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
