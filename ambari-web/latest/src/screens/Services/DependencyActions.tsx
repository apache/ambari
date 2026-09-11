/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useEffect, useRef, useState } from "react";
import { Alert, Button, Table } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import ConfirmationModal from "../../components/ConfirmationModal";
import ServiceDependenciesApi, { type ManagedDependencyBindingSummary,
  type ManagedDependencyUpdatePreview } from "../../api/serviceDependenciesApi";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import { createSecureUuid } from "../../Utils/uuid";

type Action = "UPDATE" | "RETRY" | "DETACH";

export default function DependencyActions({ clusterName, binding, onChanged }: {
  clusterName: string;
  binding: ManagedDependencyBindingSummary;
  onChanged: () => void;
}) {
  const { t } = useTranslation();
  const { isAuthorized } = useAuthorizationPolicy();
  const [action, setAction] = useState<Action | null>(null);
  const [preview, setPreview] = useState<ManagedDependencyUpdatePreview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const attempt = useRef<{ action: Action; id: string; version: number } | null>(null);
  const active = useRef(false);
  useEffect(() => {
    active.current = true;
    return () => { active.current = false; };
  }, []);
  const allowed = isAuthorized("SERVICE.MODIFY_CONFIGS") && isAuthorized("SERVICE.SET_SERVICE_USERS_GROUPS");
  const message = (key: string, defaultValue: string) => t(`serviceDependencies.actions.${key}`, { defaultValue });
  const prepare = async (next: Action) => {
    if (!binding.binding_id || busy || !allowed) return;
    setError("");
    setAction(next);
    setPreview(null);
    if (next === "UPDATE") {
      setBusy(true);
      try {
        const result = await ServiceDependenciesApi.previewUpdate(clusterName, binding.binding_id);
        if (active.current) setPreview(result);
      } catch (reason: any) {
        if (active.current) setError(reason?.response?.data?.message || reason?.message || message("failed", "Could not load the dependency operation."));
      } finally {
        if (active.current) setBusy(false);
      }
    }
  };
  const submit = async () => {
    if (!action || !binding.binding_id || binding.row_version == null || busy || !allowed) return;
    if (action === "UPDATE" && !preview) return;
    const version = preview?.row_version ?? binding.row_version;
    const id = attempt.current?.action === action && attempt.current.version === version
      ? attempt.current.id : createSecureUuid();
    attempt.current = { action, id, version };
    setBusy(true);
    setError("");
    try {
      if (action === "UPDATE") await ServiceDependenciesApi.update(clusterName, preview!, id);
      else if (action === "RETRY") await ServiceDependenciesApi.retry(clusterName, binding.binding_id, version, id);
      else await ServiceDependenciesApi.detach(clusterName, binding.binding_id, version, id);
      if (active.current) {
        attempt.current = null;
        setAction(null);
        onChanged();
      }
    } catch (reason: any) {
      if (active.current) {
        setError(reason?.response?.data?.message || reason?.message || message("unknown", "The response was not received. Reload the current binding before another operation."));
        // Keep the immutable attempt for explicit retry; refreshing reads server state.
      }
    } finally {
      if (active.current) setBusy(false);
    }
  };
  const changes = preview ? [...new Set([
    ...Object.keys(preview.old_client_config), ...Object.keys(preview.new_client_config),
  ])].flatMap(type => [...new Set([
    ...Object.keys(preview.old_client_config[type] || {}), ...Object.keys(preview.new_client_config[type] || {}),
  ])].map(name => ({ type, name, previous: preview.old_client_config[type]?.[name],
    value: preview.new_client_config[type]?.[name] }))
    .filter(change => change.previous !== change.value)) : [];
  if (!allowed || binding.ownership !== "managed") return null;
  return <div className="mt-3 d-flex flex-wrap gap-2">
    {!binding.readiness?.active_command ? <Button size="sm" variant="outline-primary" disabled={busy}
      onClick={() => void prepare("UPDATE")}>{message("review", "Review changes")}</Button> : null}
    {binding.capabilities?.retry_allowed ? <Button size="sm" variant="outline-primary" disabled={busy}
      onClick={() => void prepare("RETRY")}>{message("retry", "Retry preparation")}</Button> : null}
    {binding.capabilities?.detach_allowed ? <Button size="sm" variant="outline-danger" disabled={busy}
      onClick={() => void prepare("DETACH")}>{message("detach", "Detach provider")}</Button> : null}
    <Button size="sm" variant="outline-secondary" disabled={busy} onClick={onChanged}>
      {message("reload", "Reload status")}</Button>
    <ConfirmationModal isOpen={action != null} onClose={() => { if (!busy) setAction(null); }}
      modalTitle={`${clusterName} / HBASE / ${binding.dependency_type}`}
      modalBody={<>
        {error ? <Alert variant="danger">{error}</Alert> : null}
        {action === "DETACH" ? <Alert variant="warning">{message("detachExplanation",
          "Stop HBase before detaching. Provider services and stored data are preserved. HBase cannot start again with this detached dependency.")}</Alert> : null}
        {action === "RETRY" ? <p>{message("retryExplanation", "Revalidate the consumer hosts against the approved provider configuration. Existing provider data is preserved.")}</p> : null}
        {action === "UPDATE" ? <>
          <p>{message("updateExplanation", "Review the provider settings before applying a new approved configuration. Consumer identity and storage namespaces are preserved.")}</p>
          <Table responsive size="sm"><thead><tr><th>{message("setting", "Setting")}</th><th>{message("before", "Current")}</th><th>{message("after", "Proposed")}</th></tr></thead>
            <tbody>{changes.map(change => <tr key={`${change.type}/${change.name}`}>
              <td>{change.type}/{change.name}</td><td className="text-break">{change.previous}</td><td className="text-break">{change.value}</td>
            </tr>)}</tbody></Table>
          {!preview && !busy ? <Button size="sm" onClick={() => void prepare("UPDATE")}>{message("reloadPreview", "Reload preview")}</Button> : null}
        </> : null}
      </>}
      isOkDisabled={busy || (action === "UPDATE" && !preview)}
      okButtonText={action === "DETACH" ? message("confirmDetach", "Detach provider")
        : action === "RETRY" ? message("confirmRetry", "Retry preparation") : message("confirmUpdate", "Apply reviewed changes")}
      successCallback={() => void submit()} />
  </div>;
}
