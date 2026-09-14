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

import DependencyActions from "./DependencyActions";
import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Badge, Button, Spinner } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import ServiceDependenciesApi, {
  type ManagedDependencyBindingSummary,
} from "../../api/serviceDependenciesApi";
import { AppContext } from "../../store/context";
import { dependencyPhaseTranslationKey } from "../../Utils/managedDependencyPresentation";
import { safeDirectoryReturnPath } from "../Directories/directoryUtils";

export { dependencyPhaseTranslationKey } from "../../Utils/managedDependencyPresentation";

const dependencyPhaseVariant = (phase?: string) => {
  switch (String(phase || "").toUpperCase()) {
    case "READY":
      return "success";
    case "FAILED":
      return "danger";
    case "STALE":
    case "FENCING_UNCERTAIN":
      return "warning";
    default:
      return "secondary";
  }
};

export function DependencyCard({ binding, clusterName, onChanged }: {
  binding: ManagedDependencyBindingSummary;
  clusterName?: string;
  onChanged?: () => void;
}) {
  const { t } = useTranslation();
  const hasDrift = binding.desired_snapshot_version != null
    && binding.applied_snapshot_version != null
    && binding.desired_snapshot_version !== binding.applied_snapshot_version;
  const providerName = binding.provider?.cluster_name;
  const ownership = binding.ownership || "unknown";
  return (
    <article className="border rounded p-3 h-100" data-testid={`dependency-${binding.dependency_type}`}>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-2">
        <div>
          <h3 className="h6 mb-1">
            {t(`serviceDependencies.${binding.dependency_type.toLocaleLowerCase()}.label`)}
          </h3>
          <div className="small text-body-secondary">
            {t(`serviceDependencies.ownership.${ownership}`)}
          </div>
        </div>
        {binding.phase ? (
          <Badge bg={dependencyPhaseVariant(binding.phase)}>
            {t(dependencyPhaseTranslationKey(binding.phase))}
          </Badge>
        ) : null}
      </div>

      {ownership === "managed" && binding.provider ? (
        <dl className="row small mt-3 mb-0">
          <dt className="col-5">{t("serviceDependencies.provider")}</dt>
          <dd className="col-7">
            {providerName || t("serviceDependencies.unavailable")}
            {` / ${binding.provider.service_name}`}
          </dd>
          <dt className="col-5">{t("serviceDependencies.version")}</dt>
          <dd className="col-7">
            {binding.provider.version?.service_version || t("serviceDependencies.unavailable")}
          </dd>
          <dt className="col-5">{t("serviceDependencies.security")}</dt>
          <dd className="col-7">
            {binding.provider.security_mode || t("serviceDependencies.unavailable")}
          </dd>
          {binding.namespace?.root_uri ? (
            <>
              <dt className="col-5">{t("serviceDependencies.storageRoot")}</dt>
              <dd className="col-7 text-break">{binding.namespace.root_uri}</dd>
            </>
          ) : null}
          {binding.namespace?.znode ? (
            <>
              <dt className="col-5">{t("serviceDependencies.zookeeperPath")}</dt>
              <dd className="col-7 text-break">{binding.namespace.znode}</dd>
            </>
          ) : null}
          {binding.planned_hbase_user ? (
            <>
              <dt className="col-5">{t("serviceDependencies.hbaseUser")}</dt>
              <dd className="col-7 text-break">{binding.planned_hbase_user}</dd>
            </>
          ) : null}
        </dl>
      ) : (
        <p className="small mt-3 mb-0 text-body-secondary">
          {t(`serviceDependencies.ownershipDescription.${ownership}`)}
        </p>
      )}

      {hasDrift ? (
        <Alert className="mt-3 mb-0 py-2" variant="warning">
          {t("serviceDependencies.drift", {
            applied: binding.applied_snapshot_version,
            desired: binding.desired_snapshot_version,
          })}
        </Alert>
      ) : null}
      {binding.failure_message ? (
        <Alert className="mt-3 mb-0 py-2" variant="danger">
          {binding.failure_message}

        </Alert>
      ) : null}
      {clusterName && onChanged ? <DependencyActions key={`${clusterName}:${binding.binding_id}`}
        clusterName={clusterName} binding={binding} onChanged={onChanged} /> : null}
    </article>
  );
}

export default function ServiceDependencies() {
  const { t } = useTranslation();
  const { clusterName, runtimeKey } = useContext(AppContext);
  const location = useLocation();
  const returnTo = safeDirectoryReturnPath(
    (location.state as { returnTo?: unknown } | null)?.returnTo,
  );
  const [bindings, setBindings] = useState<ManagedDependencyBindingSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [retryCount, setRetryCount] = useState(0);
  const [loadedScope, setLoadedScope] = useState("");
  const scope = JSON.stringify([clusterName, runtimeKey]);
  const generationRef = useRef(0);

  useEffect(() => {
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    const controller = new AbortController();
    setBindings([]);
    setError(null);
    setLoading(true);
    if (!clusterName) {
      setLoading(false);
      setError(t("serviceDependencies.explicitCluster"));
      return () => controller.abort();
    }
    let timer: ReturnType<typeof setTimeout> | undefined;
    const load = () => ServiceDependenciesApi.list(clusterName, controller.signal).then(
      (items) => {
        if (controller.signal.aborted || generation !== generationRef.current) return;
        setLoadedScope(scope);
        setError(null);
        setBindings(items);
        timer = setTimeout(() => void load(), 5000);
        setLoading(false);
      },
      (requestError: any) => {
        if (controller.signal.aborted || generation !== generationRef.current) return;
        setLoadedScope(scope);
        setError(String(
          requestError?.response?.data?.message
            || requestError?.message
            || t("serviceDependencies.loadFailed"),
        ));
        setLoading(false);
      },
    );
    void load();
    return () => {
      clearTimeout(timer);
      controller.abort();
      if (generationRef.current === generation) generationRef.current += 1;
    };
  }, [clusterName, retryCount, runtimeKey, t]);

  if (loading || clusterName && loadedScope !== scope) {
    return (
      <div aria-live="polite" className="py-4 text-center">
        <Spinner animation="border" className="me-2" size="sm" />
        {t("serviceDependencies.loading")}
      </div>
    );
  }

  if (error) {
    return (
      <Alert className="mt-3" variant="danger">
        {error}{" "}
        <Button onClick={() => setRetryCount((value) => value + 1)} size="sm" variant="outline-danger">
          {t("common.retry")}
        </Button>
      </Alert>
    );
  }

  return (
    <section aria-labelledby="service-dependencies-heading" className="mt-3">
      {returnTo ? (
        <Link className="d-inline-block mb-3" to={returnTo}>
          {t("directory.backToServices")}
        </Link>
      ) : null}
      <h2 className="h5" id="service-dependencies-heading">
        {t("serviceDependencies.heading")}
      </h2>
      <p className="text-body-secondary">{t("serviceDependencies.description")}</p>
      <div className="row g-3">
        {bindings.map((binding) => (
          <div className="col-12 col-xl-6" key={binding.dependency_type}>
            <DependencyCard binding={binding} clusterName={clusterName} onChanged={() => setRetryCount(value => value + 1)} />
          </div>
        ))}
      </div>
      {!bindings.length ? <Alert variant="info">{t("serviceDependencies.empty")}</Alert> : null}
    </section>
  );
}
