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
import { useParams } from "react-router-dom";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import { ChartShare, DashboardPanel as Panel, Datasource } from "./types";
import DashboardLayout from "./Dashboard/DashboardLayout";
import DashboardPanel from "./Dashboard/DashboardPanel";
import { dashboardPanelHeight } from "./Dashboard/layout/dashboardLayout";
import { panelFromShare } from "./utils";

const toLocalInput = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

export default function SharedCharts() {
  const { shareIds = "" } = useParams<{ shareIds: string }>();
  const { clusterName } = useContext(AppContext);
  const [shares, setShares] = useState<ChartShare[]>([]);
  const [datasources, setDatasources] = useState<Datasource[]>([]);
  const [start, setStart] = useState(toLocalInput(new Date(Date.now() - 60 * 60 * 1000)));
  const [end, setEnd] = useState(toLocalInput(new Date()));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!clusterName || !shareIds) return;
    setLoading(true);
    setError("");
    void Promise.all([
      MetricsApi.getChartShares(clusterName, shareIds),
      MetricsApi.listDatasources(clusterName),
    ]).then(([items, sourceItems]) => {
      setShares(items);
      setDatasources(sourceItems);
    }).catch((caught: unknown) => {
      setError(caught instanceof Error ? caught.message : "Unable to load shared charts");
    }).finally(() => setLoading(false));
  }, [clusterName, shareIds]);

  if (loading) return <div className="monitoring-empty"><Spinner size="sm" className="me-2" />Loading shared charts</div>;
  if (error) return <Alert variant="danger">{error}</Alert>;

  const panels = shares.map(panelFromShare).filter((panel): panel is Panel => panel !== null);
  const startSeconds = Math.floor(new Date(start).getTime() / 1000);
  const endSeconds = Math.floor(new Date(end).getTime() / 1000);

  return (
    <section>
      <div className="monitoring-toolbar">
        <div><h2 className="h4 mb-1">Shared charts</h2><div className="text-muted small">{panels.length} chart{panels.length === 1 ? "" : "s"}</div></div>
        <div className="d-flex gap-2">
          <Form.Control size="sm" type="datetime-local" aria-label="Start" value={start} onChange={(event) => setStart(event.target.value)} />
          <Form.Control size="sm" type="datetime-local" aria-label="End" value={end} onChange={(event) => setEnd(event.target.value)} />
        </div>
      </div>
      {panels.length === 0 ? <div className="monitoring-panel monitoring-empty">This link contains no readable chart data.</div> : <DashboardLayout panels={panels} renderPanel={(panel, layout) => <DashboardPanel panel={panel} panelHeight={dashboardPanelHeight(layout)} start={startSeconds} end={endSeconds} refreshKey={0} variables={{}} datasources={datasources} allowShare={false} />} />}
    </section>
  );
}
