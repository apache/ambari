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
import { Alert, Button, Table } from "react-bootstrap";
import { useManagement } from "../../context/ManagementContext";
import useAdminResource from "../../hooks/useAdminResource";
import { items } from "../../api/clusterManagement";
import { latestAmbariUrl } from "../../utils/navigation";
import { LoadError, PageHeading } from "./ManagementShared";

type Draft = { draft_id: string; phase: string; revision: number; cluster_name?: string };
export default function ClusterCreate() {
  const { can, loading } = useManagement();
  const allowed = can("AMBARI.ADD_DELETE_CLUSTERS");
  const drafts = useAdminResource<{ items: Draft[] }>(allowed ? "/persist/scopes/drafts" : null, true);
  const [launchError, setLaunchError] = useState("");
  function launch() {
    try {
      const bytes = crypto.getRandomValues(new Uint8Array(16));
      bytes[6] = (bytes[6] & 15) | 64; bytes[8] = (bytes[8] & 63) | 128;
      const hex = Array.from(bytes, (v) => v.toString(16).padStart(2, "0")).join("");
      const id = `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
      window.location.assign(latestAmbariUrl(`/installer/step0?draft=${id}`));
    } catch { setLaunchError("A secure installation identifier could not be generated. Reload the page before trying again."); }
  }
  return <>
    <PageHeading title="Create Cluster" description="Create an independent cluster using the existing installation wizard, or continue one of your saved installations." />
    <Alert variant="info">Choose the Stack and repository, select unassigned hosts, then configure services and component placement. For an independent HBase deployment, select this cluster's HDFS and ZooKeeper.</Alert>
    {launchError && <Alert variant="danger">{launchError}</Alert>}
    {!loading && !allowed ? <Alert variant="warning">You do not have permission to create clusters.</Alert> : <Button disabled={!allowed} onClick={launch}>Start installation wizard</Button>}
    <h4 className="mt-4">Your saved installations</h4>
    <LoadError error={drafts.error} retry={drafts.reload} />
    {drafts.loading ? <p>Loading installations…</p> : <Table responsive><thead><tr><th>Cluster</th><th>Phase</th><th>Installation ID</th><th /></tr></thead><tbody>
      {items(drafts.data).map((draft) => <tr key={draft.draft_id}><td>{draft.cluster_name || "Not named yet"}</td><td>{draft.phase}</td><td><code>{draft.draft_id}</code></td><td><Button size="sm" href={latestAmbariUrl(`/installer/step0?draft=${encodeURIComponent(draft.draft_id)}`)}>Continue installation</Button></td></tr>)}
    </tbody></Table>}
    {allowed && !drafts.loading && !drafts.error && !items(drafts.data).length && <p>No saved installations for your account.</p>}
  </>;
}
