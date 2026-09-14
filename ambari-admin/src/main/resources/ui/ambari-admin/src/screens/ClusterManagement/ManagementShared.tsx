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

import { Alert, Button, Form } from "react-bootstrap";
import { useContext, useEffect } from "react";
import AppContent from "../../context/AppContext";
import { adminClusterUrl } from "../../utils/navigation";
import { useManagement } from "../../context/ManagementContext";

export function PageHeading({ title, description }: { title: string; description: string }) {
  const { setSelectedOption } = useContext(AppContent);
  const { error } = useManagement();
  useEffect(() => { setSelectedOption(title); }, [title, setSelectedOption]);
  return <><h3>{title}</h3><p className="text-muted">{description}</p>{error && <Alert variant="warning">{error}</Alert>}</>;
}
export function LoadError({ error, retry }: { error?: string; retry: () => void }) {
  return error ? <Alert variant="danger">{error} <Button variant="outline-danger" size="sm" onClick={retry}>Refresh</Button></Alert> : null;
}
export function ClusterSelect({ route }: { route: string }) {
  const { cluster, availableClusters } = useContext(AppContent);
  return <Form.Group className="mb-3" controlId="management-cluster-select">
    <Form.Label>Cluster</Form.Label>
    <Form.Select value={cluster?.cluster_name || ""} onChange={(event) => { if (event.target.value) window.location.assign(adminClusterUrl(event.target.value, route)); }}>
      <option value="" disabled>Select a cluster</option>
      {availableClusters.map((item: { cluster_id: number; cluster_name: string }) => <option key={item.cluster_id} value={item.cluster_name}>{item.cluster_name}</option>)}
    </Form.Select>
  </Form.Group>;
}
