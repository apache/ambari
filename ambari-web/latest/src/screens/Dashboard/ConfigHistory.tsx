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

import { useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AppContext } from "../../store/context";
import ConfigHistoryApi from "../../api/configHistoryApi";
import Spinner from "../../components/Spinner";
import Table from "../../components/Table";
import { ceil, get } from "lodash";
import Paginator from "../../components/Paginator";
import { Card, Button } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faFilter,
  faSort,
  faSortAsc,
  faSortDesc,
} from "@fortawesome/free-solid-svg-icons";
import ConfigHistoryComboSearch from "./ConfigHistoryFilterBar";
import DefaultButton from "../../components/DefaultButton";

export default function DashboardConfigHistory() {
  const { clusterName } = useContext(AppContext);
  const navigate = useNavigate();
  const [configHistorydata, setConfigHistoryData] = useState<any[]>([]);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [pageSize, setPageSize] = useState(10);
  const [currentPage, setCurrentPage] = useState(1);
  const [filters, setFilters] = useState<any[]>([]);
  const [showFilters, setShowFilters] = useState(false);

  const serviceOptions = [
    { label: "HDFS", value: "HDFS" },
    { label: "YARN", value: "YARN" },
    { label: "HBASE", value: "HBASE" },
    { label: "ZOOKEEPER", value: "ZOOKEEPER" },
    { label: "HIVE", value: "HIVE" },
    { label: "SPARK3", value: "SPARK3" },
    { label: "RANGER", value: "RANGER" },
    { label: "TEZ", value: "TEZ" },
    { label: "MAPREDUCE2", value: "MAPREDUCE2" },
    { label: "Ranger KMS", value: "RANGER_KMS" },
    { label: "SSM", value: "SSM" },
    { label: "Ambari Metrics", value: "AMBARI_METRICS" },
    { label: "Kerberos", value: "KERBEROS" },
  ];

  // Dynamic group, user, and notes options
  const [groupOptions, setGroupOptions] = useState<
    { label: string; value: string }[]
  >([]);
  const [userOptions, setUserOptions] = useState<
    { label: string; value: string }[]
  >([]);
  const [notesOptions, setNotesOptions] = useState<
    { label: string; value: string }[]
  >([]);

  const [sortState, setSortState] = useState({
    columnName: "createtime",
    order: "desc",
  });

  const maxPage = ceil(totalItems / pageSize);

  const changePage = (newPage: number) => {
    const safePage = Math.max(1, Math.min(newPage, maxPage));
    setCurrentPage(safePage);
  };

  function getGreaterThanTimestamp(value: string) {
    const now = Date.now();
    let msAgo = 0;
    if (value.endsWith("h")) {
      msAgo = parseInt(value) * 60 * 60 * 1000;
    } else if (value.endsWith("d")) {
      msAgo = parseInt(value) * 24 * 60 * 60 * 1000;
    } else {
      return "";
    }
    const timestamp = now - msAgo;
    return encodeURIComponent(">" + timestamp);
  }

  function buildFilterParams() {
    // Only the most recent filter for each field
    const lastFilterMap = new Map();
    filters.forEach((f) => {
      lastFilterMap.set(f.field.value, f);
    });
    return Array.from(lastFilterMap.values())
      .map((f) => {
        if (
          f.field.value === "user" ||
          f.field.value === "service_config_version_note"
        ) {
          return `&${f.field.value}.matches(.*${encodeURIComponent(
            f.value.value
          )}*.)`;
        }
        if (f.field.value === "createtime") {
          // Convert "1h", "7d", etc. to timestamp
          const encoded = getGreaterThanTimestamp(f.value.value);
          if (encoded) {
            return `&createtime${encoded}`;
          }
          return "";
        }
        return `&${f.field.value}=${encodeURIComponent(f.value.value)}`;
      })
      .join("");
  }
    

  function transformConfigHistoryData(data: any[]) {
    return data?.map((item) => ({
      serviceConfigVersion: item.service_config_version,
      user: item.user,
      groupId: item.group_id,
      groupName: item.group_name,
      isCurrent: item.is_current,
      createTime: item.createtime,
      serviceName: item.service_name,
      hosts: item.hosts,
      serviceConfigVersionNote: item.service_config_version_note,
      isClusterCompatible: item.is_cluster_compatible,
      stackId: item.stack_id,
    }));
  }

  async function fetchConfigHistory(currPage: number) {
    setLoading(true);
    try {
      const from = (currPage - 1) * pageSize;
      const filterParams = buildFilterParams();
      const parameters = `page_size=${pageSize}&from=${from}&sortBy=${sortState.columnName}.${sortState.order}${filterParams}&`;
      const response = await ConfigHistoryApi.fetchConfigHistory(
        clusterName,
        parameters
      );
      const transformed = transformConfigHistoryData(response.items);
      setConfigHistoryData(transformed);
      setTotalItems(response.itemTotal);
    } catch (error) {
      console.log("Error fetching configuration history:", error);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (currentPage !== 1) {
      setCurrentPage(1);
    } else {
      fetchConfigHistory(1);
    }
  }, [pageSize]);

  useEffect(() => {
    fetchConfigHistory(currentPage);
  }, [
    currentPage,
    clusterName,
    sortState.columnName,
    sortState.order,
    filters,
  ]);


  useEffect(() => {
    async function fetchAllOptions() {
      const response = await ConfigHistoryApi.fetchConfigHistory(
        clusterName,
        ""
      );
      const transformed = transformConfigHistoryData(response.items);

      const uniqueGroups = Array.from(
        new Set(transformed.map((item) => item.groupName).filter(Boolean))
      ).map((group) => ({ label: group, value: group }));

      const uniqueUsers = Array.from(
        new Set(transformed.map((item) => item.user).filter(Boolean))
      ).map((user) => ({ label: user, value: user }));

      const uniqueNotes = Array.from(
        new Set(transformed.map((item) => item.serviceConfigVersionNote).filter(Boolean))
      ).map((note) => ({ label: note, value: note }));

      setGroupOptions(uniqueGroups);
      setUserOptions(uniqueUsers);
      setNotesOptions(uniqueNotes);
    }
    fetchAllOptions();
  }, [clusterName]);

  // Sorting icon logic
  const getSortIcon = (colName: string) => {
    if (sortState.columnName !== colName) {
      return <FontAwesomeIcon className="text-muted" icon={faSort} />;
    }
    if (sortState.order === "asc") {
      return <FontAwesomeIcon className="text-info" icon={faSortAsc} />;
    }
    return <FontAwesomeIcon className="text-info" icon={faSortDesc} />;
  };

  const handleSortClick = (colName: string) => {
    setSortState((prev) => {
      if (prev.columnName === colName) {
        return {
          columnName: colName,
          order: prev.order === "asc" ? "desc" : "asc",
        };
      } else {
        return {
          columnName: colName,
          order: "asc",
        };
      }
    });
    setCurrentPage(1);
  };

  const getHeader = (headerString: string, columnId: string) => (
    <Button
      variant="transparent"
      className="d-flex m-0 p-0 border-0 align-items-center"
      onClick={() => handleSortClick(columnId)}
      style={{ userSelect: "none" }}
    >
      <div className="me-1 text-muted">{headerString}</div>
      <div>{getSortIcon(columnId)}</div>
    </Button>
  );

  const columns = [
    {
      id: "service_name",
      header: getHeader("Service", "service_name"),
      width: "15%",
      cell: (info: any) => {
        const serviceName = get(info, "row.original.serviceName");
        const versionNumber = get(info, "row.original.serviceConfigVersion");
        
        const handleServiceClick = () => {
          navigate(`/main/services/${serviceName}/configs`);
        };
        
        return (
          <div className="status-container" style={{ cursor: "pointer" }} onClick={handleServiceClick}>
            <span className="bg-info text-white px-2 rounded">
              {versionNumber}
            </span>
            <span className="mx-2 text-info">{serviceName}</span>
          </div>
        );
      },
    },
    {
      id: "group_name",
      header: getHeader("Config Group", "group_name"),
      width: "15%",
      cell: (info: any) => {
        const groupName = get(info, "row.original.groupName");
        const versionName = get(info, "row.original.isCurrent");
        return (
          <div className="status-container">
            <span>{groupName} </span>
            {versionName && (
              <span className="bg-primary text-white px-2 rounded ms-2">
                Current
              </span>
            )}
          </div>
        );
      },
    },
    {
      id: "createtime",
      header: getHeader("Created", "createtime"),
      width: "25%",
      cell: (info: any) => {
        const createTime = get(info, "row.original.createTime");
        if (!createTime) return null;
        const date = new Date(createTime);
        const formatted = date.toLocaleString("en-US", {
          weekday: "short",
          year: "numeric",
          month: "short",
          day: "2-digit",
          hour: "2-digit",
          minute: "2-digit",
          hour12: false,
          timeZone: "Europe/Moscow",
        });
        return <span>{formatted.replace(",", "")}</span>;
      },
    },
    {
      id: "user",
      header: getHeader("Author", "user"),
      width: "20%",
      cell: (info: any) => get(info, "row.original.user"),
    },
    {
      id: "service_config_version_note",
      header: getHeader("Notes", "service_config_version_note"),
      cell: (info: any) => get(info, "row.original.serviceConfigVersionNote"),
    },
  ];

  if (loading) {
    return <Spinner />;
  }

  return (
    <Card>
      <div className="d-flex justify-content-between p-3">
        <h2>Config History</h2>
        <div className="d-flex">
          <DefaultButton
            className="me-2"
            onClick={() => setShowFilters(!showFilters)}
            data-testid="filter-users-btn"
          >
            <FontAwesomeIcon icon={faFilter} />
          </DefaultButton>
        </div>
      </div>
      <div className="p-2 m-3">
        {showFilters && (
          <ConfigHistoryComboSearch
            filters={filters}
            setFilters={setFilters}
            serviceOptions={serviceOptions}
            groupOptions={groupOptions}
            userOptions={userOptions}
            notesOptions={notesOptions}
            addFilterCallback={()=>{
              setCurrentPage(1)
            }}
          />
        )}
        <Table
          columns={columns}
          data={configHistorydata}
          hover
          entityName="config history"
        />
        <Paginator
          currentPage={currentPage}
          maxPage={maxPage}
          changePage={changePage}
          itemsPerPage={pageSize}
          setItemsPerPage={setPageSize}
          totalItems={totalItems}
        />
      </div>
    </Card>
  );
}
