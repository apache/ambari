/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { useState } from "react";
import { Badge, Button, Dropdown, Form, Table } from "react-bootstrap";
import { Link } from "react-router-dom";
import { ClusterSummary, clusterListPath, items } from "../../api/clusterManagement";
import useAdminResource from "../../hooks/useAdminResource";
import { adminClusterUrl, latestClusterDashboardUrl } from "../../utils/navigation";
import { useManagement } from "../../context/ManagementContext";
import { LoadError, PageHeading } from "./ManagementShared";
import ClusterDeleteDialog from "./ClusterDeleteDialog";

export default function ClusterOverview() {
  const result = useAdminResource<{ items: ClusterSummary[] }>(clusterListPath, true);
  const { can } = useManagement();
  const [query, setQuery] = useState("");
  const [deleting, setDeleting] = useState<ClusterSummary>();
  const clusters = items(result.data).filter((item) => item.Clusters.cluster_name.toLowerCase().includes(query.toLowerCase()));
  return <>
    <PageHeading title="Cluster Overview" description="All clusters you can access, with their own hosts, services and management actions." />
    <div className="d-flex gap-2 mb-3 flex-wrap">
      <Form.Control aria-label="Find cluster" placeholder="Find a cluster" value={query} onChange={(e) => setQuery(e.target.value)} style={{ maxWidth: 320 }} />
      <Button variant="outline-secondary" onClick={result.reload}>Refresh</Button>
      {can("AMBARI.ADD_DELETE_CLUSTERS") && <Link className="btn btn-primary" to="/clusters/create">Create cluster</Link>}
    </div>
    <LoadError error={result.error} retry={result.reload} />
    {result.loading ? <p role="status">Loading clusters…</p> : !result.error && <Table responsive hover>
      <thead><tr><th>Cluster</th><th>Installation</th><th>Host health</th><th>Hosts</th><th>Services</th><th>Stack</th><th>Actions</th></tr></thead>
      <tbody>{clusters.map((item) => {
        const c = item.Clusters;
        const health = c.health_report;
        return <tr key={c.cluster_id}>
          <td><a href={adminClusterUrl(c.cluster_name)}>{c.cluster_name}</a><div className="small text-muted">ID {c.cluster_id}</div></td>
          <td><Badge bg={c.provisioning_state === "INSTALLED" ? "success" : "secondary"}>{c.provisioning_state || "Unknown"}</Badge></td>
          <td>{typeof health?.["Host/host_status/HEALTHY"] === "number" ? `${health["Host/host_status/HEALTHY"]} healthy / ${item.hosts?.length ?? "?"}` : "Unavailable"}</td>
          <td>{item.hosts?.length ?? "Unavailable"}</td><td>{Array.isArray(item.services) ? item.services.map((s) => s.ServiceInfo.service_name).join(", ") || "No services" : "Unavailable"}</td><td>{c.version || "Not selected"}</td>
          <td><div className="d-flex flex-wrap gap-2">
            <Button size="sm" variant="outline-primary" href={latestClusterDashboardUrl(c.cluster_name)}>Dashboard</Button>
            <Button size="sm" href={adminClusterUrl(c.cluster_name)}>Manage</Button>
            <Dropdown><Dropdown.Toggle size="sm" variant="outline-secondary">More</Dropdown.Toggle><Dropdown.Menu>
              {can("AMBARI.RENAME_CLUSTER") && <Dropdown.Item href={adminClusterUrl(c.cluster_name, "/clusterInformation?tab=export")}>Rename / export Blueprint</Dropdown.Item>}
              {can("AMBARI.ADD_DELETE_CLUSTERS") && <Dropdown.Item onClick={() => setDeleting(item)}>Delete cluster…</Dropdown.Item>}
              <Dropdown.Item href={adminClusterUrl(c.cluster_name, "/clusterInformation?tab=operations")}>Operation history</Dropdown.Item>
            </Dropdown.Menu></Dropdown>
          </div></td>
        </tr>;
      })}</tbody>
    </Table>}
    {!result.loading && !result.error && !clusters.length && <p>No matching clusters.</p>}
    {deleting && <ClusterDeleteDialog cluster={deleting} close={() => setDeleting(undefined)} completed={() => { const target = new URL(window.location.href); target.searchParams.delete("cluster"); target.hash = "/clusters"; window.location.assign(target.href); }} />}
  </>;
}
