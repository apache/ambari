/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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

import { useContext, useEffect, useMemo, useState } from "react";
import { Alert, Button, Card } from "react-bootstrap";
import { Link } from "react-router-dom";
import HostLogsApi from "../../api/hostLogsApi";
import Spinner from "../../components/Spinner";
import { ServiceContext } from "../../store/ServiceContext";
import { AppContext } from "../../store/context";
import {
  HOST_LOG_LEVELS,
  HostServiceLogCounts,
  mapHostLogLevelCounts,
} from "../../Utils/hostLogs";

const levelClasses = {
  CRITICAL: "bg-danger",
  DEBUG: "bg-primary",
  ERROR: "bg-danger",
  FATAL: "bg-dark",
  INFO: "bg-success",
  WARNING: "bg-warning text-dark",
};

type ServiceDisplayModel = {
  serviceName?: string;
  displayName?: string;
  ServiceInfo?: {
    service_name?: string;
  };
};

export default function HostLogMetrics({ hostName }: { hostName: string }) {
  const { clusterName } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const [rows, setRows] = useState<HostServiceLogCounts[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const serviceDisplayNames = useMemo(() => {
    const services: ServiceDisplayModel[] = Array.isArray(allServiceModels)
      ? allServiceModels
      : Object.values(allServiceModels || {});
    return Object.fromEntries(services.flatMap((service) => {
      const name = service?.serviceName || service?.ServiceInfo?.service_name;
      return name
        ? [[name, service?.displayName || service?.ServiceInfo?.service_name || name]]
        : [];
    }));
  }, [allServiceModels]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    void HostLogsApi.fetchHostLogs(clusterName, hostName)
      .then((response) => {
        if (active) setRows(mapHostLogLevelCounts(response, serviceDisplayNames));
      })
      .catch((requestError) => {
        if (active) {
          setError(
            requestError?.response?.data?.message ||
              "Ambari could not load host log counts.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [clusterName, hostName, retryCount, serviceDisplayNames]);

  return (
    <Card className="w-100 rounded-0">
      <Card.Header as="h3">Host Log Metrics</Card.Header>
      <Card.Body>
        {loading ? <Spinner /> : null}
        {error ? (
          <Alert variant="danger">
            {error}{" "}
            <Button
              size="sm"
              variant="outline-danger"
              onClick={() => setRetryCount((value) => value + 1)}
            >
              Retry
            </Button>
          </Alert>
        ) : null}
        {!loading && !error && !rows.some((row) => row.available) ? (
          <p className="text-muted mb-0">Log level count data is unavailable.</p>
        ) : null}
        {!loading && !error ? (
          <div className="row g-3">
            {rows.filter((row) => row.available).map((row) => (
              <div className="col-md-6" key={row.serviceName}>
                <div className="border p-3 h-100">
                  <Link
                    to={`/main/hosts/${encodeURIComponent(hostName)}/logs?service_name=${encodeURIComponent(row.serviceName)}`}
                    className="fw-semibold"
                  >
                    {row.serviceDisplayName}
                  </Link>
                  <div className="d-flex flex-wrap gap-2 mt-3">
                    {HOST_LOG_LEVELS.map((level) => (
                      <span
                        className={`badge ${levelClasses[level]}`}
                        key={level}
                      >
                        {level}: {row.counts[level]}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </Card.Body>
    </Card>
  );
}
