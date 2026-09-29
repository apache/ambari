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
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import useClusterPath from "../../hooks/useClusterPath";
import ScopedNavigate from "../../components/ScopedNavigate";
import "./monitoring.scss";
import { useWorkspaceText } from "./workspace";

const links = [
  ["dashboards", "/main/monitoring/dashboards", "CLUSTER.VIEW_METRICS"],
  ["explore", "/main/monitoring/explorer", "CLUSTER.VIEW_METRICS"],
  ["targets", "/main/monitoring/targets", "HOST.VIEW_METRICS"],
  ["sources", "/main/monitoring/data-sources", "CLUSTER.VIEW_METRICS"],
] as const;

export function MonitoringIndexRedirect() {
  const { hasAuthorization } = useAuth();
  const destination = hasAuthorization("CLUSTER.VIEW_METRICS")
    ? "/main/monitoring/dashboards"
    : hasAuthorization("HOST.VIEW_METRICS")
      ? "/main/monitoring/targets"
      : "/main/dashboard/metrics";

  return <ScopedNavigate to={destination} replace />;
}

export default function MonitoringLayout() {
  const text = useWorkspaceText();
  const { hasAuthorization } = useAuth();
  const scopedPath = useClusterPath();

  return (
    <div className="monitoring-shell">
      <header className="monitoring-header">
        <div>
          <h1>{text("title")}</h1>
          <p>{text("subtitle")}</p>
        </div>
        <Nav className="monitoring-nav" variant="underline">
          {links.filter(([, , permission]) => hasAuthorization(permission)).map(([label, to]) => (
            <NavLink key={to} className="nav-link" to={scopedPath(to)}>
              {text(label)}
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
