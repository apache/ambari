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

import { Nav } from "react-bootstrap";
import { Navigate, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import "./monitoring.scss";

const links = [
  ["Dashboards", "/main/monitoring/dashboards", "CLUSTER.VIEW_METRICS"],
  ["Explore", "/main/monitoring/explorer", "CLUSTER.VIEW_METRICS"],
  ["Targets", "/main/monitoring/targets", "HOST.VIEW_METRICS"],
  ["Data sources", "/main/monitoring/data-sources", "CLUSTER.VIEW_METRICS"],
];

export function MonitoringIndexRedirect() {
  const { hasAuthorization } = useAuth();
  const destination = hasAuthorization("CLUSTER.VIEW_METRICS")
    ? "/main/monitoring/dashboards"
    : hasAuthorization("HOST.VIEW_METRICS")
      ? "/main/monitoring/targets"
      : "/main/dashboard/metrics";

  return <Navigate to={destination} replace />;
}

export default function MonitoringLayout() {
  const { hasAuthorization } = useAuth();

  return (
    <div className="monitoring-shell">
      <header className="monitoring-header">
        <div>
          <h1>Monitoring</h1>
          <p>Prometheus queries, dashboards, scrape targets, and datasource connections</p>
        </div>
        <Nav className="monitoring-nav" variant="underline">
          {links.filter(([, , permission]) => hasAuthorization(permission)).map(([label, to]) => (
            <NavLink key={to} className="nav-link" to={to}>
              {label}
            </NavLink>
          ))}
        </Nav>
      </header>
      <main className="monitoring-content">
        <Outlet />
      </main>
    </div>
  );
}
