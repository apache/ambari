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

import { useContext, useEffect, useRef, useState } from "react";
import { Alert, Badge, Button, Spinner } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import ServiceDependenciesApi, {
  type ManagedDependentsResponse,
} from "../../api/serviceDependenciesApi";
import { AppContext } from "../../store/context";
import { clusterPath } from "../../Utils/clusterRoute";
import { dependentStateTranslationKey } from "../../Utils/managedDependencyPresentation";

const emptyResponse: ManagedDependentsResponse = {
  hidden_dependent_count: 0,
  impact_revision: "",
  items: [],
};

export default function ServiceDependents({ serviceName }: { serviceName: string }) {
  const { t } = useTranslation();
  const { clusterName, runtimeKey } = useContext(AppContext);
  const [response, setResponse] = useState(emptyResponse);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [retryCount, setRetryCount] = useState(0);
  const generationRef = useRef(0);

  useEffect(() => {
    const generation = generationRef.current + 1;
    generationRef.current = generation;
    const controller = new AbortController();
    setResponse(emptyResponse);
    setError(null);
    setLoading(true);
    if (!clusterName || !serviceName) {
      setLoading(false);
      setError(t("serviceDependents.explicitProvider"));
      return () => controller.abort();
    }
    void ServiceDependenciesApi.getDependents(
      clusterName,
      serviceName,
      controller.signal,
    ).then(
      (nextResponse) => {
        if (controller.signal.aborted || generation !== generationRef.current) return;
        setResponse(nextResponse);
        setLoading(false);
      },
      (requestError: any) => {
        if (controller.signal.aborted || generation !== generationRef.current) return;
        setError(String(
          requestError?.response?.data?.message
            || requestError?.message
            || t("serviceDependents.loadFailed"),
        ));
        setLoading(false);
      },
    );
    return () => {
      controller.abort();
      if (generationRef.current === generation) generationRef.current += 1;
    };
  }, [clusterName, retryCount, runtimeKey, serviceName, t]);

  if (loading) {
    return (
      <div aria-live="polite" className="py-4 text-center">
        <Spinner animation="border" className="me-2" size="sm" />
        {t("serviceDependents.loading")}
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
    <section aria-labelledby="service-dependents-heading" className="mt-3">
      <h2 className="h5" id="service-dependents-heading">{t("serviceDependents.heading")}</h2>
      <p className="text-body-secondary">
        {t("serviceDependents.description", { cluster: clusterName, service: serviceName })}
      </p>
      {response.items.length ? (
        <div className="row g-3">
          {response.items.map((dependent) => (
            <div className="col-12 col-xl-6" key={dependent.binding_id}>
              <article className="border rounded p-3 h-100">
                <div className="d-flex flex-wrap align-items-start justify-content-between gap-2">
                  <h3 className="h6 mb-0 text-break">
                    <Link to={clusterPath(
                      dependent.consumer_cluster_name,
                      `/main/services/${dependent.consumer_service_name}/dependencies`,
                    )}>
                      {dependent.consumer_cluster_name} / {dependent.consumer_service_name}
                    </Link>
                  </h3>
                  <Badge bg={dependent.state === "READY" ? "success"
                    : dependent.state === "FAILED" ? "danger" : "secondary"}>
                    {t(dependentStateTranslationKey(dependent.state))}
                  </Badge>
                </div>
                <div className="small text-body-secondary mt-2">
                  {t("serviceDependents.dependencyType", {
                    type: t(`serviceDependencies.${dependent.dependency_type.toLocaleLowerCase()}.label`),
                  })}
                </div>
              </article>
            </div>
          ))}
        </div>
      ) : response.hidden_dependent_count === 0 ? (
        <Alert variant="info">{t("serviceDependents.empty")}</Alert>
      ) : null}
      {response.hidden_dependent_count > 0 ? (
        <Alert className="mt-3 mb-0" variant="info">
          {t("serviceDependents.hiddenCount", { count: response.hidden_dependent_count })}
        </Alert>
      ) : null}
    </section>
  );
}
