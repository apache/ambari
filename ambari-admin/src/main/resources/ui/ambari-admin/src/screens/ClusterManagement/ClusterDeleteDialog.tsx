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
import { Alert, Button, Form, Modal } from "react-bootstrap";
import { adminApi } from "../../api/configs/axiosConfig";
import { clusterEndpoint, ClusterSummary, errorMessage, readResource } from "../../api/clusterManagement";
import useAdminResource from "../../hooks/useAdminResource";
import { LoadError } from "./ManagementShared";

type Preview = Omit<ClusterSummary, "hosts"> & { hosts?: { Hosts: { host_name: string }; host_components?: { HostRoles: { state: string; desired_state?: string } }[] }[] };
const removableStates = new Set(["INIT", "INSTALLED", "INSTALL_FAILED", "UNINSTALLED", "DISABLED"]);
export default function ClusterDeleteDialog({ cluster, close, completed }: { cluster: ClusterSummary; close: () => void; completed: () => void }) {
  const name = cluster.Clusters.cluster_name;
  const path = clusterEndpoint(name);
  const previewPath = `${path}?fields=Clusters/cluster_id,hosts/Hosts/host_name,services/ServiceInfo/service_name,hosts/host_components/HostRoles/state,hosts/host_components/HostRoles/desired_state`;
  const preview = useAdminResource<Preview>(previewPath);
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  function blocked(data?: Preview) {
    return !data || data.Clusters.cluster_id !== cluster.Clusters.cluster_id || !Array.isArray(data.hosts)
      || data.hosts.some((host) => !Array.isArray(host.host_components) || host.host_components.some((item) => !removableStates.has(item.HostRoles.state) || !removableStates.has(item.HostRoles.desired_state || "")));
  }
  async function remove() {
    setBusy(true); setError("");
    try {
      const current = await readResource<Preview>(previewPath);
      if (blocked(current)) { setError("The cluster identity or component states changed. Refresh the deletion review."); return; }
      let mutationError: unknown;
      try { await adminApi.delete(path); } catch (e) { mutationError = e; }
      try { await readResource(path); } catch (e) {
        if ((e as { response?: { status?: number } }).response?.status === 404) { completed(); return; }
        throw e;
      }
      throw mutationError || new Error("Deletion is not confirmed; the cluster still exists");
    } catch (e) { setError(errorMessage(e)); }
    finally { setBusy(false); }
  }
  return <Modal show onHide={() => { if (!busy) close(); }} size="lg"><Modal.Header closeButton={!busy}><Modal.Title>Delete {name}</Modal.Title></Modal.Header><Modal.Body>
    <Alert variant="warning">This removes the cluster's Ambari configuration, service definitions and permissions. Its hosts are released from cluster membership. This does not erase Hadoop data from disks.</Alert>
    <LoadError error={preview.error} retry={preview.reload} />
    {preview.loading ? <p>Loading deletion review…</p> : preview.data && <><p>Cluster ID: {preview.data.Clusters.cluster_id} · Hosts: {preview.data.hosts?.length ?? "Unavailable"} · Services: {preview.data.services?.map((s) => s.ServiceInfo.service_name).join(", ") || "None"}</p><Alert variant={blocked(preview.data) ? "danger" : "info"}>{blocked(preview.data) ? "Deletion is blocked while components are running, changing state, or their state cannot be confirmed." : "Component states permit deletion review. The server performs the final lifecycle and dependency checks when you confirm."}</Alert></>}
    {error && <Alert variant="danger">{error}</Alert>}
    <Form.Label htmlFor="confirm-cluster-deletion">Type {name} to confirm</Form.Label><Form.Control id="confirm-cluster-deletion" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} disabled={busy} />
  </Modal.Body><Modal.Footer><Button variant="secondary" disabled={busy} onClick={close}>Cancel</Button><Button variant="danger" disabled={busy || preview.loading || Boolean(preview.error) || blocked(preview.data) || confirmation !== name} onClick={() => void remove()}>Delete cluster</Button></Modal.Footer></Modal>;
}
