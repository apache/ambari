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
import { Alert, Button, Nav, Table } from "react-bootstrap";
import { Link, useHistory, useLocation } from "react-router-dom";
import AppContent from "../../context/AppContext";
import { clusterEndpoint, ClusterSummary, HostSummary, items } from "../../api/clusterManagement";
import useAdminResource from "../../hooks/useAdminResource";
import { useManagement } from "../../context/ManagementContext";
import { clusterOperationUrl, latestClusterDashboardUrl } from "../../utils/navigation";
import ClusterInformation from "./ClusterInformation";
import { PermissionEditor } from "./ClusterPermissions";
import { ClusterSelect, LoadError, PageHeading } from "./ManagementShared";

type Service = { ServiceInfo: { service_name: string; state: string } };
type Operation = { Requests: { id: number; request_context: string; request_status: string; progress_percent?: number; start_time?: number } };
const tabs = [ ["general", "Basic information"], ["resources", "Services & hosts"], ["permissions", "Access permissions"], ["operations", "Operation history"], ["export", "Configuration & export"] ];

function ServiceHosts({ name }: { name: string }) {
  const services = useAdminResource<{ items: Service[] }>(`${clusterEndpoint(name)}/services?fields=ServiceInfo/service_name,ServiceInfo/state`, true);
  const hosts = useAdminResource<{ items: HostSummary[] }>(`${clusterEndpoint(name)}/hosts?fields=Hosts/host_name,Hosts/host_status,Hosts/ip`, true);
  const { can } = useManagement();
  return <>
    <div className="d-flex gap-2 mb-3"><Button variant="outline-secondary" onClick={() => { services.reload(); hosts.reload(); }}>Refresh</Button>
      {can("SERVICE.ADD_DELETE_SERVICES", name) && <Button href={clusterOperationUrl(name, "/main/service/add/step1")}>Add service</Button>}
      {can("HOST.ADD_DELETE_HOSTS", name) && <Button href={clusterOperationUrl(name, "/main/host/add/step1")}>Add hosts</Button>}
    </div>
    <LoadError error={services.error || hosts.error} retry={() => { services.reload(); hosts.reload(); }} />
    <h5>Services</h5>{services.loading ? <p>Loading services…</p> : <Table responsive><thead><tr><th>Service</th><th>State</th><th>Configuration</th></tr></thead><tbody>{items(services.data).map(({ ServiceInfo: s }) => <tr key={s.service_name}><td><a href={clusterOperationUrl(name, `/main/services/${encodeURIComponent(s.service_name)}/summary`)}>{s.service_name}</a></td><td>{s.state || "Unknown"}</td><td><a href={clusterOperationUrl(name, `/main/services/${encodeURIComponent(s.service_name)}/configs`)}>Open configuration</a></td></tr>)}</tbody></Table>}
    <h5>Hosts</h5>{hosts.loading ? <p>Loading hosts…</p> : <Table responsive><thead><tr><th>Host</th><th>IP</th><th>Health</th></tr></thead><tbody>{items(hosts.data).map(({ Hosts: host }) => <tr key={host.host_name}><td><a href={clusterOperationUrl(name, `/main/hosts/${encodeURIComponent(host.host_name)}/summary`)}>{host.host_name}</a></td><td>{host.ip || "Unavailable"}</td><td>{host.host_status || "Unknown"}</td></tr>)}</tbody></Table>}
  </>;
}
function Operations({ name }: { name: string }) {
  const [page, setPage] = useState(0);
  const { can } = useManagement();
  const permitted = ["CLUSTER.VIEW_STATUS_INFO", "HOST.VIEW_STATUS_INFO", "SERVICE.VIEW_STATUS_INFO"].some((permission) => can(permission, name));
  const result = useAdminResource<{ items: Operation[] }>(permitted ? `${clusterEndpoint(name)}/requests?fields=Requests/id,Requests/request_context,Requests/request_status,Requests/progress_percent,Requests/start_time&sortBy=Requests/id.desc&page_size=20&from=${page * 20}` : null, true);
  if (!permitted) return <Alert variant="info">You do not have permission to view this cluster's operation history.</Alert>;
  return <>
    <p>Server request records for {name}. Open a request to inspect its tasks and failure details. Continue installation or service workflows through their original wizard.</p>
    <Button variant="outline-secondary" className="mb-3" onClick={result.reload}>Refresh</Button><LoadError error={result.error} retry={result.reload} />
    {result.loading ? <p>Loading requests…</p> : <Table responsive><thead><tr><th>Request</th><th>Operation</th><th>Status</th><th>Progress</th><th>Started</th></tr></thead><tbody>{items(result.data).map(({ Requests: request }) => <tr key={request.id}><td><a href={clusterOperationUrl(name, `/main/requests?requestId=${request.id}`)}>#{request.id}</a></td><td>{request.request_context}</td><td>{request.request_status}</td><td>{typeof request.progress_percent === "number" ? `${Math.round(request.progress_percent)}%` : "Unavailable"}</td><td>{typeof request.start_time === "number" && request.start_time > 0 ? new Date(request.start_time).toLocaleString() : "Not started"}</td></tr>)}</tbody></Table>}
    <div className="d-flex gap-2"><Button variant="outline-secondary" disabled={page === 0 || result.loading} onClick={() => setPage((v) => v - 1)}>Newer</Button><span className="align-self-center">Page {page + 1}</span><Button variant="outline-secondary" disabled={result.loading || items(result.data).length < 20} onClick={() => setPage((v) => v + 1)}>Older</Button></div>
  </>;
}
export default function ClusterDetails() {
  const { cluster } = useContext(AppContent);
  const name: string = cluster?.cluster_name || "";
  const location = useLocation(); const history = useHistory();
  const requestedTab = new URLSearchParams(location.search).get("tab") || "general";
  const tab = tabs.some(([key]) => key === requestedTab) ? requestedTab : "general";
  const result = useAdminResource<ClusterSummary>(name ? `${clusterEndpoint(name)}?fields=Clusters/cluster_id,Clusters/cluster_name,Clusters/provisioning_state,Clusters/version,Clusters/security_type,hosts/Hosts/host_name,services/ServiceInfo/service_name` : null);
  const c = result.data?.Clusters;
  return <>
    <PageHeading title="Cluster Details" description="Manage one explicitly selected cluster. Dashboard and service actions keep this cluster's identity." />
    <div className="d-flex gap-2 mb-3"><Link to="/clusters">All clusters</Link>{name && <a href={latestClusterDashboardUrl(name)}>Dashboard: {name}</a>}</div>
    <ClusterSelect route="/clusterInformation" />
    {name && <><h4 className="mb-3">{name}</h4><Nav variant="tabs" activeKey={tab} onSelect={(key) => history.replace({ pathname: location.pathname, search: `?tab=${key}` })}>{tabs.map(([key, label]) => <Nav.Item key={key}><Nav.Link eventKey={key}>{label}</Nav.Link></Nav.Item>)}</Nav>
      <div className="pt-4" key={`${cluster.cluster_id}:${tab}`}>
        {tab === "general" && <><LoadError error={result.error} retry={result.reload} />{result.loading ? <p>Loading cluster details…</p> : c && <Table><tbody>{[["Cluster name", c.cluster_name], ["Stable cluster ID", c.cluster_id], ["Installation state", c.provisioning_state], ["Stack version", c.version || "Not selected"], ["Security", c.security_type || "Unavailable"], ["Hosts", result.data?.hosts?.length ?? "Unavailable"], ["Services", result.data?.services?.map((s) => s.ServiceInfo.service_name).join(", ") || "None"]].map(([label, value]) => <tr key={String(label)}><th>{label}</th><td>{value}</td></tr>)}</tbody></Table>}{c?.provisioning_state !== "INSTALLED" && <Link to="/clusters/create">Continue a saved installation</Link>}<div className="mt-3"><Link to="/stackVersions">Manage Stack repositories and versions</Link></div></>}
        {tab === "resources" && <ServiceHosts name={name} />}
        {tab === "permissions" && <PermissionEditor clusterName={name} />}
        {tab === "operations" && <Operations name={name} />}
        {tab === "export" && <ClusterInformation />}
      </div>
    </>}
  </>;
}
