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

import { useEffect, useState, useContext } from "react";
import { AlertsApi } from "../../api/alertsApi";
import { MergedAlert } from "./types";
import { Container, Button } from "react-bootstrap";
import { Link } from "react-router-dom";
import { AppContext } from "../../store/context";
import Table from "../../components/Table";
import Paginator from "../../components/Paginator";
import usePagination from "../../hooks/usePagination";
import Spinner from "../../components/Spinner";
import AlertFilters from "./AlertFilters";
import { ColumnDef } from "@tanstack/react-table";
import { AlertStatus, AlertStatusDisplay } from "./alertStatus";
import { sortAlerts } from "./alertUtils";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMedkit } from "@fortawesome/free-solid-svg-icons";
import usePolling from "../../hooks/usePolling";

interface HostAlertsProps {
  hostname?: string;
}

const HostAlerts = ({ hostname }: HostAlertsProps) => {
  const { clusterName } = useContext(AppContext);
  const [alerts, setAlerts] = useState<MergedAlert[]>([]);
  const [filteredAlerts, setFilteredAlerts] = useState<MergedAlert[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [sorting, setSorting] = useState<{ id: string; desc: boolean }[]>([
    { id: "statuses", desc: true },
  ]);

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(filteredAlerts);

  const fetchData = async () => {
    if (clusterName && hostname) {
      try {
        const currTime = Date.now();
        const alertsResponse = await AlertsApi.getAlertsList(
          clusterName,
          currTime,
          hostname
        );
        const mappedAlerts = alertsResponse.items
          .filter((item: { Alert: any }) => item.Alert)
          .filter((item: any) => item.Alert.maintenance_state !== "ON") // Filter out alerts from components in maintenance mode
          .map((item: any) => ({
            serviceDisplayName: item.Alert.service_name,
            latest_text: item.Alert.text || item.Alert.latest_text || "",
            label: item.Alert.label,
            statuses: [
              {
                status: item.Alert.state,
                count: item.Alert.occurrences,
                last_status_changed: item.Alert.latest_timestamp,
              },
            ],
            last_status_changed: item.Alert.latest_timestamp,
            alert_definition_id: item.Alert.definition_id,
            maintenance_state: item.Alert.maintenance_state,
          }));
        setAlerts(mappedAlerts);
        setFilteredAlerts(mappedAlerts);
      } catch (error) {
        console.error("Error fetching data:", error);
      } finally {
        setIsLoading(false);
      }
    }
  };

  usePolling(fetchData, 10000);

  useEffect(()=>{
   fetchData(); 
  },[])

  const truncateText = (text: string, maxLength: number = 60) => {
    if (!text) return "";
    return text.length > maxLength
      ? text.substring(0, maxLength) + "..."
      : text;
  };

  useEffect(() => {
    if (sorting.length > 0) {
      const { id, desc } = sorting[0];
      const fieldMap: { [key: string]: string } = {
        statuses: "status",
        serviceDisplayName: "service_name",
      };
      const sortField = fieldMap[id] || id;
      const sortedData = sortAlerts(alerts, sortField, desc);
      setFilteredAlerts(sortedData);
    } else {
      setFilteredAlerts(alerts);
    }
  }, [sorting, alerts]);

  const columns: ColumnDef<MergedAlert, any>[] = [
    {
      header: "Service",
      accessorKey: "serviceDisplayName",
      cell: (info) => {
        const serviceName = info.row.original.serviceDisplayName || "";
        if (serviceName.toLowerCase() === "ambari") {
          return <span>{serviceName}</span>;
        }
        return (
          <Link
            to={`/main/services/${serviceName}/summary`}
            className="custom-link"
          >
            {serviceName}
          </Link>
        );
      },
    },
    {
      header: "Alert Definition Name",
      accessorKey: "label",
      cell: (info) => (
        <Link
          to={`/main/alerts/${info.row.original.alert_definition_id}`}
          className="custom-link"
        >
          {info.row.original.label || ""}
        </Link>
      ),
    },
    {
      header: "Status",
      accessorKey: "statuses",
      cell: (info) => {
        const isInMaintenance = info.row.original.maintenance_state === "ON";
        const statuses = info.row.original.statuses || [];
        const statusClassMap: { [key in AlertStatus]: string } = {
          [AlertStatus.CRITICAL]: "status-critical",
          [AlertStatus.WARNING]: "status-warning",
          [AlertStatus.OK]: "status-ok",
          [AlertStatus.UNKNOWN]: "status-unknown",
          [AlertStatus.NONE]: "status-none",
        };
        const getStatusClass = (status: string | undefined) =>
          status
            ? statusClassMap[status.toLowerCase() as AlertStatus] ||
              "status-none"
            : "status-none";

        return (
          <div className="status-container">
            {statuses.length > 0 ? (
              statuses.map(
                (
                  statusItem: { status: string; count: number },
                  index: number
                ) => (
                  <div key={statusItem.status} className="status-row">
                    <Button
                      key={statusItem.status}
                      className={`alert-item alert-status-box ${getStatusClass(
                        statusItem.status
                      )} ${index > 0 ? "mt-1" : ""} ${
                        isInMaintenance ? "bg-light" : ""
                      }`}
                    >
                      {isInMaintenance && (
                        <FontAwesomeIcon
                          className="text-dark fs-12 me-1"
                          icon={faMedkit}
                        />
                      )}
                      {AlertStatusDisplay[statusItem.status.toLowerCase()] ||
                        statusItem.status.toUpperCase()}
                    </Button>
                  </div>
                )
              )
            ) : (
              <Button className="alert-item alert-status-box status-none">
                {AlertStatusDisplay[AlertStatus.NONE]}
              </Button>
            )}
          </div>
        );
      },
    },
    {
      header: "Response",
      accessorKey: "latest_text",
      cell: (info) => {
        const { latest_text } = info.row.original;
        return (
          <span className="text-truncate">
            {truncateText(latest_text || "")}
          </span>
        );
      },
    },
  ];

  return (
    <div className="mx-5">
      <Container className="p-4 bg-white">
        <h2 className="table-title col-sm-1">Alerts</h2>
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
            <Paginator
              currentPage={currentPage}
              maxPage={maxPage}
              changePage={changePage}
              itemsPerPage={itemsPerPage}
              setItemsPerPage={setItemsPerPage}
              totalItems={filteredAlerts.length}
            />
          </div>
        )}
      </Container>
    </div>
  );
};

export default HostAlerts;
