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
import { Alert, Button, Form, Modal, Table } from "react-bootstrap";
import AppContent from "../../context/AppContext";
import { adminApi } from "../../api/configs/axiosConfig";
import { clusterEndpoint, Grant, Role, items, errorMessage, readResource } from "../../api/clusterManagement";
import useAdminResource from "../../hooks/useAdminResource";
import { useManagement } from "../../context/ManagementContext";
import { ClusterSelect, LoadError, PageHeading } from "./ManagementShared";

export function PermissionEditor({ clusterName }: { clusterName: string }) {
  const { can } = useManagement();
  const allowed = can("AMBARI.ASSIGN_ROLES");
  const path = `${clusterEndpoint(clusterName)}/privileges`;
  const result = useAdminResource<{ items: Grant[] }>(allowed ? `${path}?fields=PrivilegeInfo/*` : null, true);
  const roles = useAdminResource<{ items: Role[] }>(allowed ? "/permissions?PermissionInfo/resource_name=CLUSTER&fields=PermissionInfo/*" : null, true);
  const [kind, setKind] = useState("USER");
  const [principal, setPrincipal] = useState("");
  const [role, setRole] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [remove, setRemove] = useState<Grant>();
  const principals = useAdminResource<{ items: { Users?: { user_name: string }; Groups?: { group_name: string } }[] }>(allowed ? (kind === "USER" ? "/users?fields=Users/user_name" : "/groups?fields=Groups/group_name") : null, true);
  async function mutate(grant?: Grant) {
    setBusy(true); setError(""); setMessage("");
    const name = principal.trim();
    let mutationError: unknown;
    try {
      if (grant) await adminApi.delete(`${path}/${grant.PrivilegeInfo.privilege_id}`);
      else await adminApi.post(path, [{ PrivilegeInfo: { principal_type: kind, principal_name: name, permission_name: role } }]);
    } catch (e) { mutationError = e; }
    try {
      const current = items(await readResource<{ items: Grant[] }>(`${path}?fields=PrivilegeInfo/*`));
      const applied = grant ? !current.some((g) => g.PrivilegeInfo.privilege_id === grant.PrivilegeInfo.privilege_id)
        : current.some(({ PrivilegeInfo: g }) => g.principal_type === kind && g.principal_name === name && g.permission_name === role);
      if (!applied) throw mutationError || new Error("The requested grant was not confirmed");
      setMessage(grant ? "Grant removed and confirmed by the server." : "Grant saved and confirmed by the server.");
      setRemove(undefined); if (!grant) setPrincipal("");
    } catch (e) { setError(errorMessage(e)); }
    finally { setBusy(false); result.reload(); }
  }
  if (!allowed) return <Alert variant="info">Cluster grants require Ambari role-assignment permission.</Alert>;
  return <>
    <p>Explicit user and group grants for <strong>{clusterName}</strong>. Group members inherit their group's grants. Ambari administrator access applies to all clusters and is managed under Users.</p>
    <LoadError error={result.error || roles.error || principals.error} retry={() => { result.reload(); roles.reload(); principals.reload(); }} />
    {error && <Alert variant="danger">{error}</Alert>}{message && <Alert variant="success">{message}</Alert>}
    <Form className="d-flex flex-wrap gap-2 mb-3" onSubmit={(e) => { e.preventDefault(); void mutate(); }}>
      <Form.Select aria-label="Principal type" value={kind} onChange={(e) => { setKind(e.target.value); setPrincipal(""); }} disabled={busy} style={{ width: 140 }}><option>USER</option><option>GROUP</option></Form.Select>
      <Form.Select aria-label="User or group" value={principal} onChange={(e) => setPrincipal(e.target.value)} disabled={busy || principals.loading || Boolean(principals.error)} style={{ maxWidth: 260 }} required>
        <option value="">Select {kind === "USER" ? "user" : "group"}</option>{items(principals.data).map((p) => { const name = kind === "USER" ? p.Users?.user_name : p.Groups?.group_name; return name ? <option key={name}>{name}</option> : null; })}
      </Form.Select>
      <Form.Select aria-label="Cluster role" value={role} onChange={(e) => setRole(e.target.value)} disabled={busy} style={{ maxWidth: 260 }} required><option value="">Select cluster role</option>{items(roles.data).map(({ PermissionInfo: p }) => <option key={p.permission_name} value={p.permission_name}>{p.permission_label}</option>)}</Form.Select>
      <Button type="submit" disabled={busy || !principal || !role || result.loading || Boolean(result.error)}>Grant role</Button>
    </Form>
    {result.loading ? <p>Loading grants…</p> : <Table responsive><thead><tr><th>Principal</th><th>Type</th><th>Role</th><th /></tr></thead><tbody>{items(result.data).map((grant) => <tr key={grant.PrivilegeInfo.privilege_id}><td>{grant.PrivilegeInfo.principal_name}</td><td>{grant.PrivilegeInfo.principal_type}</td><td>{grant.PrivilegeInfo.permission_label || grant.PrivilegeInfo.permission_name}</td><td><Button size="sm" variant="outline-danger" disabled={busy} onClick={() => setRemove(grant)}>Remove grant</Button></td></tr>)}</tbody></Table>}
    {!result.loading && !result.error && !items(result.data).length && <p>No explicit grants on this cluster.</p>}
    <Modal show={Boolean(remove)} onHide={() => { if (!busy) setRemove(undefined); }}><Modal.Header closeButton={!busy}><Modal.Title>Remove cluster grant</Modal.Title></Modal.Header><Modal.Body>{error && <Alert variant="danger">{error}</Alert>}Remove {remove?.PrivilegeInfo.permission_label || remove?.PrivilegeInfo.permission_name} from {remove?.PrivilegeInfo.principal_name} on {clusterName}? Other grants and group memberships will remain in effect.</Modal.Body><Modal.Footer><Button variant="secondary" disabled={busy} onClick={() => setRemove(undefined)}>Cancel</Button><Button variant="danger" disabled={busy} onClick={() => void mutate(remove)}>Remove grant</Button></Modal.Footer></Modal>
  </>;
}
export default function ClusterPermissions() {
  const { cluster } = useContext(AppContent);
  return <><PageHeading title="Cluster Permissions" description="Manage access independently for each cluster." /><ClusterSelect route="/clusterPermissions" />{cluster?.cluster_name && <PermissionEditor key={cluster.cluster_id} clusterName={cluster.cluster_name} />}</>;
}
