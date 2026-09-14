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

import { useContext, useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import AppContent from "../../context/AppContext";
import { HostSummary, hostsPath, items } from "../../api/clusterManagement";
import useAdminResource from "../../hooks/useAdminResource";
import { adminClusterUrl, clusterOperationUrl } from "../../utils/navigation";
import { useManagement } from "../../context/ManagementContext";
import { LoadError, PageHeading } from "./ManagementShared";

export default function HostResources() {
  const result = useAdminResource<{ items: HostSummary[] }>(hostsPath, true);
  const { availableClusters } = useContext(AppContent);
  const { can } = useManagement();
  const [filter, setFilter] = useState("all");
  const [query, setQuery] = useState("");
  const [target, setTarget] = useState("");
  const hosts = items(result.data);
  const visible = hosts.filter(({ Hosts: host }) => (filter === "all" || (filter === "free" ? !host.cluster_name : host.cluster_name === filter)) && host.host_name.toLowerCase().includes(query.toLowerCase()));
  return <>
    <PageHeading title="Host Resources" description="Registered hosts across clusters. Ownership changes are performed through the target cluster's Add Host wizard." />
    <Alert variant="info">Unassigned hosts can join a new or existing cluster. Hosts that belong to a cluster cannot be reassigned directly to another cluster.</Alert>
    <div className="d-flex flex-wrap gap-2 mb-3">
      <Form.Control aria-label="Find host" placeholder="Find a host" value={query} onChange={(e) => setQuery(e.target.value)} style={{ maxWidth: 280 }} />
      <Form.Select aria-label="Filter host ownership" value={filter} onChange={(e) => setFilter(e.target.value)} style={{ maxWidth: 260 }}>
        <option value="all">All registered hosts</option><option value="free">Unassigned hosts</option>
        {availableClusters.map((c: { cluster_name: string; cluster_id: number }) => <option key={c.cluster_id} value={c.cluster_name}>{c.cluster_name}</option>)}
      </Form.Select><Button variant="outline-secondary" onClick={result.reload}>Refresh</Button>
    </div>
    <div className="d-flex flex-wrap gap-2 mb-3 align-items-center">
      <Form.Select aria-label="Target cluster for Add Host" value={target} onChange={(e) => setTarget(e.target.value)} style={{ maxWidth: 280 }}>
        <option value="">Select target cluster</option>
        {availableClusters.filter((c: { cluster_name: string; provisioning_state: string }) => c.provisioning_state === "INSTALLED" && can("HOST.ADD_DELETE_HOSTS", c.cluster_name)).map((c: { cluster_name: string; cluster_id: number }) => <option key={c.cluster_id} value={c.cluster_name}>{c.cluster_name}</option>)}
      </Form.Select>
      <Button disabled={!target} href={target ? clusterOperationUrl(target, "/main/host/add/step1") : undefined}>Open Add Host wizard</Button>
    </div>
    <LoadError error={result.error} retry={result.reload} />
    {result.loading ? <p>Loading registered hosts…</p> : !result.error && <>
      <p>{hosts.length} registered · {hosts.filter((h) => !h.Hosts.cluster_name).length} unassigned</p>
      <Table responsive hover><thead><tr><th>Host</th><th>IP</th><th>Agent health</th><th>Cluster ownership</th></tr></thead><tbody>{visible.map(({ Hosts: host }) => <tr key={host.host_name}>
        <td>{host.cluster_name ? <a href={clusterOperationUrl(host.cluster_name, `/main/hosts/${encodeURIComponent(host.host_name)}/summary`)}>{host.host_name}</a> : host.host_name}</td><td>{host.ip || "Unavailable"}</td><td>{host.host_status || "Unknown"}</td>
        <td>{host.cluster_name ? <a href={adminClusterUrl(host.cluster_name)}>{host.cluster_name}</a> : <strong>Unassigned</strong>}</td>
      </tr>)}</tbody></Table>{!visible.length && <p>No matching hosts.</p>}
    </>}
  </>;
}
