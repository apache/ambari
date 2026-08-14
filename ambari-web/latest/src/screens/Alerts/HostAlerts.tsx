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

import { useContext, useState } from "react";
import { Alert, Button, Container } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMedkit } from "@fortawesome/free-solid-svg-icons";
import { ColumnDef, SortingState } from "@tanstack/react-table";
import { Link } from "react-router-dom";
import { AlertsApi } from "../../api/alertsApi";
import Paginator from "../../components/Paginator";
import Spinner from "../../components/Spinner";
import Table from "../../components/Table";
import usePagination from "../../hooks/usePagination";
import usePolling from "../../hooks/usePolling";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";
import AlertFilters from "./AlertFilters";
import { AlertStatus, AlertStatusDisplay } from "./alertStatus";
import { sortAlerts } from "./alertUtils";
import { MergedAlert } from "./types";

interface HostAlertsProps {
  hostname?: string;
}

const statusClassMap: { [key in AlertStatus]: string } = {
  [AlertStatus.CRITICAL]: "status-critical",
  [AlertStatus.WARNING]: "status-warning",
  [AlertStatus.OK]: "status-ok",
  [AlertStatus.UNKNOWN]: "status-unknown",
  [AlertStatus.NONE]: "status-none",
};

const HostAlerts = ({ hostname }: HostAlertsProps) => {
  const { clusterName } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const [alerts, setAlerts] = useState<MergedAlert[]>([]);
  const [filteredAlerts, setFilteredAlerts] = useState<MergedAlert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [sorting, setSorting] = useState<SortingState>([
    { id: "statuses", desc: true },
  ]);

  const serviceDisplayName = (serviceName: string) => {
    const service = Object.values(allServiceModels || {}).find(
      (model: any) => String(model?.serviceName).toUpperCase() === serviceName,
    ) as any;
    return service?.displayName || serviceName.replaceAll("_", " ");
  };

  const fetchData = async () => {
    if (!clusterName || !hostname) {
      setIsLoading(false);
      return;
    }

    try {
      setLoadError("");
      const response = await AlertsApi.getHostAlertInstances(clusterName, hostname);
      const mappedAlerts = (response.items || [])
        .filter((item: any) => item.Alert)
        .map((item: any) => {
          const serviceName = item.Alert.service_name || "";
          return {
            serviceName,
            serviceDisplayName: serviceDisplayName(serviceName),
            latest_text: item.Alert.text || "",
            label: item.Alert.label,
            statuses: [{
              status: item.Alert.state,
              count: 1,
              last_status_changed: item.Alert.latest_timestamp,
              latest_text: item.Alert.text || "",
            }],
            last_status_changed: item.Alert.latest_timestamp,
            alert_definition_id: item.Alert.definition_id,
            maintenance_state: item.Alert.maintenance_state,
          } as MergedAlert;
        });
      setAlerts(mappedAlerts);
    } catch (error: any) {
      setLoadError(
        error?.response?.data?.message || "Ambari could not load alerts for this host.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  usePolling(fetchData, 10000);

  let sortedAlerts = filteredAlerts;
  if (sorting.length) {
    const { id, desc } = sorting[0];
    const fieldMap: Record<string, string> = {
      statuses: "status",
      serviceDisplayName: "service_name",
      label: "name",
    };
    sortedAlerts = sortAlerts(filteredAlerts, fieldMap[id] || id, id === "statuses" ? desc : !desc);
  }

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(sortedAlerts);

  const columns: ColumnDef<MergedAlert, any>[] = [
    {
      header: "Service",
      accessorKey: "serviceDisplayName",
      cell: ({ row }) => {
        const serviceName = row.original.serviceName || "";
        if (serviceName === "AMBARI") {
          return <span>{row.original.serviceDisplayName}</span>;
        }
        return (
          <Link
            to={`/main/services/${encodeURIComponent(serviceName)}/summary`}
            className="custom-link"
          >
            {row.original.serviceDisplayName}
          </Link>
        );
      },
    },
    {
      header: "Alert Definition Name",
      accessorKey: "label",
      cell: ({ row }) => (
        <Link
          to={`/main/alerts/${row.original.alert_definition_id}`}
          className="custom-link"
        >
          {row.original.label || ""}
        </Link>
      ),
    },
    {
      header: "Status",
      accessorKey: "statuses",
      cell: ({ row }) => {
        const isInMaintenance = row.original.maintenance_state === "ON";
        const status = row.original.statuses[0]?.status || AlertStatus.NONE;
        const normalizedStatus = status.toLowerCase() as AlertStatus;
        return (
          <span className={`alert-item alert-status-box ${
            isInMaintenance ? "bg-light text-dark" : statusClassMap[normalizedStatus] || "status-none"
          }`}>
            {isInMaintenance ? (
              <FontAwesomeIcon className="me-1" icon={faMedkit} title="Maintenance mode" />
            ) : null}
            {AlertStatusDisplay[normalizedStatus] || status.toUpperCase()}
          </span>
        );
      },
    },
    {
      header: "Response",
      accessorKey: "latest_text",
      cell: ({ row }) => (
        <span className="text-truncate" title={row.original.latest_text}>
          {row.original.latest_text || ""}
        </span>
      ),
    },
  ];

  return (
    <div className="mx-5">
      <Container className="p-4 bg-white">
        <h2 className="table-title">Alerts</h2>
        {loadError ? (
          <Alert variant="danger">
            {loadError}{" "}
            <Button
              size="sm"
              variant="outline-danger"
              onClick={() => {
                setIsLoading(alerts.length === 0);
                void fetchData();
              }}
            >
              Retry
            </Button>
          </Alert>
        ) : null}
        {isLoading ? (
          <Spinner />
        ) : (
          <div>
            <AlertFilters data={alerts} onFilter={setFilteredAlerts} />
            <Table
              columns={columns as ColumnDef<unknown, unknown>[]}
              data={currentItems}
              hover
              entityName="alerts"
              sorting={sorting}
              onSortingChange={setSorting}
            />
            {sortedAlerts.length ? (
              <Paginator
                currentPage={currentPage}
                maxPage={maxPage}
                changePage={changePage}
                itemsPerPage={itemsPerPage}
                setItemsPerPage={setItemsPerPage}
                totalItems={sortedAlerts.length}
              />
            ) : null}
          </div>
        )}
      </Container>
    </div>
  );
};

export default HostAlerts;
