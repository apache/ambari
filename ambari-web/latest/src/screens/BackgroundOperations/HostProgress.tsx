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
import ClusterApi from "../../api/clusterApi";
import { cloneDeep, filter, find, get, isEmpty, map, uniq } from "lodash";
import { groupPropertyValues } from "../../Utils/dataUtils";
import { ProgressBar, Stack } from "react-bootstrap";
import Table from "../../components/Table";
import {
  faCheck,
  faClock,
  faCogs,
  faExclamation,
  faMinus,
} from "@fortawesome/free-solid-svg-icons";
import { getStatusIcon } from "../../Utils/statusIcons";
import Spinner from "../../components/Spinner";
import Center from "../../components/Center";
import usePagination from "../../hooks/usePagination";
import Paginator from "../../components/Paginator";
import { ViewLevel } from "../../constants";
import { AppContext } from "../../store/context";
import Filters from "./Filters";

type HostProgressProps = {
  requestId: number | string;
  latestMessage: any;
  setSelectedHost: (host: string) => void;
  setSelectedLevel: (value: ViewLevel) => void;
  setSelectedRequestId: (id: number) => void;
  clusterName?: string;
};
type Task = {
  command: string;
  command_detail: string;
  host_name: string;
  id: number;
  ops_display_name: string;
  request_id: number;
  role: string;
  status: string;
};
function HostProgress({
  requestId,
  latestMessage,
  setSelectedHost,
  setSelectedLevel,
  setSelectedRequestId,
  clusterName: clusterNameProps,
}: HostProgressProps) {
  const [hostDetails, setHostDetails] = useState([]);
  const [loading, setLoading] = useState(false);
  const { clusterName: cName } = useContext(AppContext);
  const [filteredHosts, setFilteredHosts] = useState([]);
  const [selectedFilter, setSelectedFilter] = useState<any>(null);
  const clusterName = clusterNameProps || cName;

  const getProgress = (tasks: Task[]) => {
    if (!tasks || !tasks.length) {
      return 0;
    }

    const groupedByStatus = groupPropertyValues(tasks, "Tasks.status");

    const completedActions =
      get(groupedByStatus, "COMPLETED", []).length +
      get(groupedByStatus, "FAILED", []).length +
      get(groupedByStatus, "ABORTED", []).length +
      get(groupedByStatus, "TIMEDOUT", []).length;
    const queuedActions = get(groupedByStatus, "QUEUED", []).length;
    const inProgressActions = get(groupedByStatus, "IN_PROGRESS", []).length;

    return Math.ceil(
      ((queuedActions * 0.09 + inProgressActions * 0.35 + completedActions) /
        tasks.length) *
        100
    );
  };
  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(hostDetails);

  useEffect(() => {
    if (!isEmpty(latestMessage)) {
      const hostDetailsCopy = cloneDeep(hostDetails);
      map(hostDetails, function (_host: any) {
        for (const task of latestMessage.Tasks) {
          const matchingHost: any = find(hostDetailsCopy, [
            "host",
            task.hostName,
          ]);
          if (matchingHost) {
            const matchingTask = find(matchingHost.tasks, [
              "Tasks.id",
              task.id,
            ]);
            if (matchingTask) {
              matchingTask.Tasks.status = task.status;
            }
          }
        }
      });
      setHostDetails(hostDetailsCopy as any);
    }
  }, [latestMessage]);
  useEffect(() => {
    if (selectedFilter?.value) {
      setFilteredHosts(
        currentItems.filter(
          (host: any) =>
            getStatus(host.tasks)[0] === selectedFilter.value.toUpperCase()
        ) as any
      );
    } else {
      setFilteredHosts(currentItems as any);
    }
  }, [selectedFilter, currentItems]);
  const getStatus = (tasks: any[]) => {
    let isCompleted = true;
    let hasInProgressTasks = false;
    let hasCompletedTasks = false;
    let tasksLength = tasks.length;
    
    for (let i = 0; i < tasksLength; i++) {
      let taskStatus = tasks[i].Tasks.status;
      if (taskStatus !== "COMPLETED") {
        isCompleted = false;
      }
      if (taskStatus === "FAILED") {
        return ["FAILED", faExclamation, "danger", false];
      }
      if (taskStatus === "ABORTED") {
        return ["ABORTED", faMinus, "warning", false];
      }
      if (taskStatus === "TIMEDOUT") {
        return ["TIMEDOUT", faClock, "warning", false];
      }
      if (taskStatus === "IN_PROGRESS") {
        hasInProgressTasks = true;
      }
      if (taskStatus === "COMPLETED") {
        hasCompletedTasks = true;
      }
    }
    
    // If all tasks are completed, show the special green checkmark
    if (isCompleted && hasCompletedTasks) {
      return ["CURRENTLY_EXECUTING", faCheck, "success", false]; // Use CURRENTLY_EXECUTING to get green checkmark
    }
    
    if (hasInProgressTasks) {
      // Check if this host is currently executing (has the most recent start time among IN_PROGRESS tasks)
      const inProgressTasks = tasks.filter(task => task.Tasks.status === "IN_PROGRESS");
      const hasActivelyExecutingTasks = inProgressTasks.some(task => 
        task.Tasks.start_time && 
        !task.Tasks.end_time &&
        // Consider a task as actively executing if it started within the last few minutes
        // and doesn't have an end time
        Date.now() - new Date(task.Tasks.start_time).getTime() < 24 * 60 * 60 * 1000 // 24 hours threshold
      );
      
      if (hasActivelyExecutingTasks) {
        return ["CURRENTLY_EXECUTING", faCogs, "warning", true]; // Use warning variant for bright orange
      } else {
        return ["IN_PROGRESS", faCogs, "info", true];
      }
    }
    
    return ["PENDING", faCogs, "info", true];
  };
  async function getHostProgressDetails() {
    setLoading(true);
    const requestTasks = await ClusterApi.getRequestById(
      clusterName,
      requestId
    );
    const allTasks = get(requestTasks, "tasks", []);
    const allHosts = map(allTasks, "Tasks.host_name");
    const uniqueHosts = uniq(allHosts);
    const hosts = map(uniqueHosts, function (host) {
      const hostTasks = filter(allTasks, ["Tasks.host_name", host]);
      const progress = getProgress(hostTasks);
      return {
        host,
        progress,
        tasks: hostTasks,
      };
    });
    setHostDetails(hosts as any);
    setLoading(false);
  }
  useEffect(() => {
    getHostProgressDetails();
  }, []);
  const columns = [
    {
      id: "hostname",
      header: "",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const status = getStatus(rowData.tasks);

        return (
          <Stack
            direction="horizontal"
            onClick={() => {
              setSelectedLevel(ViewLevel.TASKS_LIST);
              setSelectedHost(rowData.host);
              setSelectedRequestId(requestId as number);
            }}
            className="p-1 cursor-pointer"
          >
            {getStatusIcon(status[0] as string)}
            <div className="text-info ms-2">{rowData.host}</div>
          </Stack>
        );
      },
    },
    {
      id: "progress",
      header: "",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const progress = getProgress(rowData.tasks);
        const status = getStatus(rowData.tasks);
        return (
          <Stack
            direction="horizontal"
            className="p-1 cursor-pointer"
            onClick={() => {
              setSelectedLevel(ViewLevel.TASKS_LIST);
              setSelectedHost(rowData.host);
              setSelectedRequestId(requestId as number);
            }}
          >
            <ProgressBar
              className="w-100 request-progress-bar rounded-1"
              now={progress}
              variant={status[2] as any}
            />
            <div className="ms-2 text-nowrap">{`${progress}%`}</div>
          </Stack>
        );
      },
    },
  ];

  if (loading) {
    return (
      <Center>
        <Spinner />
      </Center>
    );
  }

  return (
    <>
      <Stack
        direction="horizontal"
        className="justify-content-between mt-3 w-100"
      >
        <h1>Hosts</h1>
        <Filters
          successLevel="success"
          items={filteredHosts.map((host: any) => {
            return { ...host, status: getStatus(host.tasks)[0] };
          })}
          allItems={currentItems.map((host: any) => {
            return { ...host, status: getStatus(host.tasks)[0] };
          })}
          statusKey={"status"}
          selectedFilter={selectedFilter}
          setSelectedFilter={setSelectedFilter}
        />
      </Stack>
      <Table columns={columns} data={filteredHosts} />
      <Paginator
        currentPage={currentPage}
        maxPage={maxPage}
        changePage={changePage}
        itemsPerPage={itemsPerPage}
        setItemsPerPage={setItemsPerPage}
        totalItems={hostDetails.length}
      />
    </>
  );
}
export default HostProgress;
