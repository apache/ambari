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

import { useContext, useEffect, useState } from "react";
import { Alert, Form, Spinner } from "react-bootstrap";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { Dashboard } from "./types";
import DashboardDetail from "./DashboardDetail";
import { dashboardAppearsAt } from "./utils";

interface EmbeddedDashboardsProps {
  location: string;
  includeAllFallback?: boolean;
}

export default function EmbeddedDashboards({
  location,
  includeAllFallback = false,
}: EmbeddedDashboardsProps) {
  const { clusterName } = useContext(AppContext);
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [selectedId, setSelectedId] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!clusterName) return;
    setLoading(true);
    setError("");
    void MetricsApi.listDashboards(clusterName).then((items) => {
      const matching = items.filter((dashboard) => dashboardAppearsAt(dashboard, location));
      const available = matching.length || !includeAllFallback ? matching : items;
      setDashboards(available);
      setSelectedId((current) => available.some((dashboard) => dashboard.id === current)
        ? current
        : available[0]?.id || 0);
    }).catch((caught: unknown) => {
      setError(caught instanceof Error ? caught.message : "Unable to load monitoring dashboards");
    }).finally(() => setLoading(false));
  }, [clusterName, includeAllFallback, location]);

  if (loading) return <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading monitoring dashboards</div>;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!selectedId) {
    return <div className="monitoring-panel monitoring-empty">No monitoring dashboard is assigned to {location}.</div>;
  }

  return (
    <div className="monitoring-content p-0">
      {dashboards.length > 1 && <div className="d-flex justify-content-end mb-3"><Form.Select className="w-auto" size="sm" aria-label="Monitoring dashboard" value={selectedId} onChange={(event) => setSelectedId(Number(event.target.value))}>{dashboards.map((dashboard) => <option key={dashboard.id} value={dashboard.id}>{dashboard.name}</option>)}</Form.Select></div>}
      <DashboardDetail key={selectedId} dashboardId={String(selectedId)} embedded />
    </div>
  );
}
