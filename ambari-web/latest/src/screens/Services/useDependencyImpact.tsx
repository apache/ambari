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

import { useEffect, useState } from "react";
import { Alert, Button, Spinner } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import ServiceDependenciesApi, { type ManagedDependencyImpact } from "../../api/serviceDependenciesApi";

/** Adds one current server impact snapshot to an existing lifecycle dialog. */
export default function useDependencyImpact(clusterName: string, serviceName: string,
  action: "STOP" | "RESTART", open: boolean) {
  const { t } = useTranslation();
  const [impact, setImpact] = useState<ManagedDependencyImpact | null>(null);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [loadedScope, setLoadedScope] = useState("");
  const supported = ["HDFS", "ZOOKEEPER"].includes(serviceName);
  const scope = JSON.stringify([clusterName, serviceName, action, open, revision]);
  useEffect(() => {
    setImpact(null);
    setError("");
    setLoadedScope("");
    if (!open || !supported) return;
    const abort = new AbortController();
    void ServiceDependenciesApi.getImpact(clusterName, serviceName, abort.signal, action).then(result => {
      if (abort.signal.aborted) return;
      if (result.action !== action || result.service_name !== serviceName
        || !Number.isSafeInteger(result.provider_cluster_id) || !result.impact_revision) {
        throw new Error("Ambari returned a different provider impact identity.");
      }
      setImpact(result);
      setLoadedScope(scope);
    }).catch(reason => {
      if (!abort.signal.aborted) setError(reason?.response?.data?.message || reason?.message || "Could not load dependency impact.");
    });
    return () => abort.abort();
  }, [scope]);
  const blocked = supported && open && (loadedScope !== scope || !impact);
  const parameters: Record<string, string> = !blocked && impact?.requires_confirmation ? {
    "parameters/managed_dependency_impact_confirmations": JSON.stringify([{
      provider_cluster_id: impact.provider_cluster_id,
      service_name: impact.service_name,
      action: impact.action,
      revision: impact.impact_revision,
    }]),
  } : {};
  const refresh = () => setRevision(value => value + 1);
  const handleFailure = (reason: any) => {
    const code = reason?.response?.data?.code;
    if (String(code).startsWith("DEPENDENCY_IMPACT_")) refresh();
  };
  const content = !supported ? null : <div className="mt-3" aria-live="polite">
    {error ? <Alert variant="danger">{error}{" "}<Button size="sm" onClick={refresh}>
      {t("common.retry")}</Button></Alert> : blocked ? <Spinner size="sm" /> : impact ?
      <Alert variant={impact.requires_confirmation ? "warning" : "info"}>
        <div>{t("serviceDependencies.impactCount", { count: impact.dependent_count,
          defaultValue: `${impact.dependent_count} dependent deployment(s) may be interrupted.` })}</div>
        {impact.items?.length ? <ul className="mb-1">{impact.items.map(item =>
          <li key={`${item.consumer_cluster_name}:${item.consumer_service_name}`}>
            {item.consumer_cluster_name} / {item.consumer_service_name}</li>)}</ul> : null}
        {impact.hidden_dependent_count ? <div>{t("serviceDependencies.hiddenImpact", {
          count: impact.hidden_dependent_count,
          defaultValue: `${impact.hidden_dependent_count} additional dependent deployment(s) are not visible to this user.`,
        })}</div> : null}
      </Alert> : null}
  </div>;
  return { blocked, parameters, content, handleFailure };
}
