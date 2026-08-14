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

import { useContext, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AppContext } from "../../store/context";
import ConfigHistoryApi from "../../api/configHistoryApi";
import Spinner from "../../components/Spinner";
import Table from "../../components/Table";
import { ceil, get } from "lodash";
import Paginator from "../../components/Paginator";
import { Badge, Button, Card } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faFilter,
  faSort,
  faSortAsc,
  faSortDesc,
} from "@fortawesome/free-solid-svg-icons";
import ConfigHistoryComboSearch from "./ConfigHistoryFilterBar";
import DefaultButton from "../../components/DefaultButton";
import {
  buildConfigHistoryParameters,
  canOpenConfigHistoryItem,
  ConfigHistoryFilter,
  ConfigHistoryItem,
  configHistoryNavigationState,
  formatConfigHistoryDate,
  transformConfigHistoryItems,
} from "../../Utils/configHistory";

type SelectOption = { label: string; value: string };

function NotesCell({ notes = "" }: { notes?: string }) {
  const [expanded, setExpanded] = useState(false);
  const hasMore = notes.length > 80;
  const visibleNotes = hasMore && !expanded ? notes.slice(0, 80) : notes;

  return (
    <span>
      {visibleNotes}
      {hasMore ? (
        <Button
          variant="link"
          size="sm"
          className="border-0 p-0 ms-1 align-baseline"
          onClick={() => setExpanded((value) => !value)}
        >
          {expanded ? "<< Less" : ">> More"}
        </Button>
      ) : null}
    </span>
  );
}

export default function DashboardConfigHistory() {
  const {
    clusterName,
    parsedSocketMessages,
    services,
    userTimezone,
  } = useContext(AppContext);
  const navigate = useNavigate();
  const [configHistoryData, setConfigHistoryData] = useState<ConfigHistoryItem[]>([]);
  const [filteredTotal, setFilteredTotal] = useState(0);
  const [overallTotal, setOverallTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [filters, setFilters] = useState<ConfigHistoryFilter[]>([]);
  const [showFilters, setShowFilters] = useState(false);
  const [refreshSequence, setRefreshSequence] = useState(0);
  const requestSequence = useRef(0);

  const installedServices = services
    .map((service) => get(service, "ServiceInfo.service_name", ""))
    .filter(Boolean);
  const [serviceOptions, setServiceOptions] = useState<SelectOption[]>([]);
  const [groupOptions, setGroupOptions] = useState<SelectOption[]>([]);
  const [userOptions, setUserOptions] = useState<SelectOption[]>([]);
  const [notesOptions, setNotesOptions] = useState<SelectOption[]>([]);
  const [sortState, setSortState] = useState<{
    columnName: string;
    order: "asc" | "desc";
  }>({
    columnName: "createtime",
    order: "desc",
  });

  const maxPage = Math.max(1, ceil(filteredTotal / pageSize));
  const latestConfigEvent = parsedSocketMessages.find(
    (message) => message.destination === "/events/configs",
  );

  useEffect(() => {
    if (latestConfigEvent) {
      setRefreshSequence((value) => value + 1);
    }
  }, [latestConfigEvent]);

  useEffect(() => {
    if (!clusterName) return;
    let active = true;

    async function fetchSupportingData() {
      const [totalResult, servicesResult, groupsResult, usersResult, notesResult] = await Promise.allSettled([
        ConfigHistoryApi.fetchTotal(clusterName),
        ConfigHistoryApi.fetchSuggestions(clusterName, "service_name"),
        ConfigHistoryApi.fetchSuggestions(clusterName, "group_name"),
        ConfigHistoryApi.fetchSuggestions(clusterName, "user"),
        ConfigHistoryApi.fetchSuggestions(clusterName, "service_config_version_note"),
      ]);
      if (!active) return;

      if (totalResult.status === "fulfilled") {
        setOverallTotal(Number(totalResult.value.itemTotal) || 0);
      }
      const options = (result: PromiseSettledResult<string[]>) =>
        result.status === "fulfilled"
          ? result.value.map((value) => ({ label: value, value }))
          : [];
      setServiceOptions(options(servicesResult));
      setGroupOptions(options(groupsResult));
      setUserOptions(options(usersResult));
      setNotesOptions(options(notesResult));
    }

    void fetchSupportingData();
    return () => {
      active = false;
    };
  }, [clusterName, refreshSequence]);

  useEffect(() => {
    if (!clusterName) return;
    const sequence = ++requestSequence.current;

    async function fetchConfigHistory() {
      setLoading(true);
      setLoadError("");
      try {
        const parameters = buildConfigHistoryParameters({
          currentPage,
          pageSize,
          sortColumn: sortState.columnName,
          sortOrder: sortState.order,
          filters,
        });
        const response = await ConfigHistoryApi.fetchConfigHistory(clusterName, parameters);
        if (sequence !== requestSequence.current) return;
        setConfigHistoryData(transformConfigHistoryItems(response.items));
        setFilteredTotal(Number(response.itemTotal) || 0);
      } catch (error) {
        if (sequence !== requestSequence.current) return;
        console.error("Error fetching configuration history:", error);
        setLoadError("Ambari could not load configuration history.");
      } finally {
        if (sequence === requestSequence.current) {
          setLoading(false);
        }
      }
    }

    void fetchConfigHistory();
  }, [clusterName, currentPage, filters, pageSize, refreshSequence, sortState]);

  const changePage = (newPage: number) => {
    setCurrentPage(Math.max(1, Math.min(newPage, maxPage)));
  };

  const getSortIcon = (columnName: string) => {
    if (sortState.columnName !== columnName) {
      return <FontAwesomeIcon className="text-muted" icon={faSort} />;
    }
    return (
      <FontAwesomeIcon
        className="text-info"
        icon={sortState.order === "asc" ? faSortAsc : faSortDesc}
      />
    );
  };

  const handleSortClick = (columnName: string) => {
    setSortState((current) => ({
      columnName,
      order: current.columnName === columnName && current.order === "asc" ? "desc" : "asc",
    }));
    setCurrentPage(1);
  };

  const getHeader = (label: string, columnName: string) => (
    <Button
      variant="transparent"
      className="d-flex m-0 p-0 border-0 align-items-center"
      onClick={() => handleSortClick(columnName)}
    >
      <span className="me-1 text-muted">{label}</span>
      {getSortIcon(columnName)}
    </Button>
  );

  const columns = [
    {
      id: "service_name",
      header: getHeader("Service", "service_name"),
      width: "15%",
      cell: (info: any) => {
        const item = info.row.original as ConfigHistoryItem;
        const canOpen = canOpenConfigHistoryItem(item, installedServices);
        const label = (
          <>
            <Badge bg="info">V{item.serviceConfigVersion}</Badge>
            <span className="ms-2">{item.serviceName}</span>
          </>
        );

        return canOpen ? (
          <Button
            variant="link"
            className="border-0 p-0 text-decoration-none"
            onClick={() => navigate(`/main/services/${item.serviceName}/configs`, {
              state: configHistoryNavigationState(item),
            })}
          >
            {label}
          </Button>
        ) : (
          <span title={item.groupName === "Deleted" ? "This config group was deleted." : "This service is not installed."}>
            {label}
          </span>
        );
      },
    },
    {
      id: "group_name",
      header: getHeader("Config Group", "group_name"),
      width: "14%",
      cell: (info: any) => {
        const item = info.row.original as ConfigHistoryItem;
        return (
          <span>
            {item.groupName || "Default"}
            {item.isCurrent ? <Badge bg="success" className="ms-2">Current</Badge> : null}
          </span>
        );
      },
    },
    {
      id: "hosts",
      header: "Hosts",
      width: "16%",
      cell: (info: any) => {
        const item = info.row.original as ConfigHistoryItem;
        if ((item.groupName || "Default") === "Default") {
          return <span>Hosts not assigned to another group</span>;
        }
        return item.hosts?.length ? (
          <span title={item.hosts.join(", ")}>{item.hosts.join(", ")}</span>
        ) : (
          <span>None</span>
        );
      },
    },
    {
      id: "createtime",
      header: getHeader("Created", "createtime"),
      width: "18%",
      cell: (info: any) => {
        const createTime = (info.row.original as ConfigHistoryItem).createTime;
        return createTime ? <span>{formatConfigHistoryDate(createTime, userTimezone)}</span> : null;
      },
    },
    {
      id: "user",
      header: getHeader("Author", "user"),
      width: "12%",
      cell: (info: any) => (info.row.original as ConfigHistoryItem).user,
    },
    {
      id: "is_cluster_compatible",
      header: "Compatible",
      width: "10%",
      cell: (info: any) => {
        const compatible = (info.row.original as ConfigHistoryItem).isClusterCompatible;
        return compatible === undefined ? "Unknown" : (
          <Badge bg={compatible ? "success" : "warning"} text={compatible ? undefined : "dark"}>
            {compatible ? "Yes" : "No"}
          </Badge>
        );
      },
    },
    {
      id: "service_config_version_note",
      header: getHeader("Notes", "service_config_version_note"),
      cell: (info: any) => (
        <NotesCell notes={(info.row.original as ConfigHistoryItem).serviceConfigVersionNote} />
      ),
    },
  ];

  return (
    <Card>
      <div className="d-flex justify-content-between p-3">
        <div>
          <h2>Config History</h2>
          <span className="text-muted">
            {filteredTotal} of {overallTotal || filteredTotal} versions showing
          </span>
        </div>
        <DefaultButton
          className="align-self-start"
          onClick={() => setShowFilters((value) => !value)}
          data-testid="config-history-filter-button"
        >
          <FontAwesomeIcon icon={faFilter} />
        </DefaultButton>
      </div>
      <div className="p-2 m-3">
        {showFilters ? (
          <ConfigHistoryComboSearch
            filters={filters}
            setFilters={setFilters}
            serviceOptions={serviceOptions}
            groupOptions={groupOptions}
            userOptions={userOptions}
            notesOptions={notesOptions}
            addFilterCallback={() => setCurrentPage(1)}
          />
        ) : null}
        {loadError ? (
          <div className="text-center py-5">
            <p className="text-danger">{loadError}</p>
            <Button variant="outline-primary" onClick={() => setRefreshSequence((value) => value + 1)}>
              Retry
            </Button>
          </div>
        ) : loading ? (
          <Spinner />
        ) : (
          <>
            <Table
              columns={columns}
              data={configHistoryData}
              hover
              entityName="config history"
            />
            <Paginator
              currentPage={currentPage}
              maxPage={maxPage}
              changePage={changePage}
              itemsPerPage={pageSize}
              setItemsPerPage={(value) => {
                setPageSize(value);
                setCurrentPage(1);
              }}
              totalItems={filteredTotal}
            />
          </>
        )}
      </div>
    </Card>
  );
}
