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
import { cloneDeep, filter, find, get, isEmpty } from "lodash";
import { Stack } from "react-bootstrap";
import Table from "../../components/Table";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Spinner from "../../components/Spinner";
import Center from "../../components/Center";
import { ViewLevel } from "../../constants";
import { AppContext } from "../../store/context";
import Filters from "./Filters";
import { commandDetail } from "../../Utils/Utility";

type TasksListProps = {
  requestId: number | string;
  latestMessage: any;
  setSelectedHost: (host: string) => void;
  setSelectedLevel: (value: ViewLevel) => void;
  setSelectedRequestId: (id: number) => void;
  selectedHost: string;
  getStatus: any;
  setSelectedTask: (value: any) => void;
  clusterName?: string;
};
function TasksList({
  requestId,
  latestMessage,
  setSelectedHost,
  setSelectedLevel,
  setSelectedRequestId,
  selectedHost,
  getStatus,
  setSelectedTask,
  clusterName: clusterNameProps,
}: TasksListProps) {
  const [hostTasks, setHostTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const { clusterName: cName } = useContext(AppContext);
  const [selectedFilter, setSelectedFilter] = useState<any>(null);
  const [filteredTasks, setFilteredTasks] = useState([]);
  const [requestInputs, setRequestInputs] = useState<any>(null);
  const clusterName = clusterNameProps || cName;

  async function getHostTasksDetails() {
    setLoading(true);
    const requestTasks = await ClusterApi.getRequestById(
      clusterName,
      requestId
    );
    const allTasks = get(requestTasks, "tasks", []);
    // Get request inputs from the Requests object
    const inputs = get(requestTasks, "Requests.inputs", null);
    setRequestInputs(inputs);
    setHostTasks(filter(allTasks, ["Tasks.host_name", selectedHost]) as any);
    setLoading(false);
  }

  useEffect(() => {
    if (!isEmpty(latestMessage)) {
      const hostTasksCopy = cloneDeep(hostTasks);
      for (const task of latestMessage.Tasks) {
        const matchingTask: any = find(hostTasksCopy, ["Tasks.id", task.id]);
        if (matchingTask) {
          matchingTask.Tasks.status = task.status;
        }
      }
      setHostTasks(hostTasksCopy as any);
    }
  }, [latestMessage]);
  useEffect(() => {
    if (selectedFilter?.value) {
      setFilteredTasks(
        filter(hostTasks, [
          "Tasks.status",
          selectedFilter.value.toUpperCase(),
        ]) as any
      );
    } else {
      setFilteredTasks(hostTasks);
    }
  }, [selectedFilter, hostTasks]);
  useEffect(() => {
    getHostTasksDetails();
  }, []);
  const columns = [
    {
      id: "name",
      header: "",
      cell: (info: any) => {
        const {
          row: { original: rowData },
        } = info;
        const status = getStatus(rowData.Tasks.status);

        return (
          <Stack
            direction="horizontal"
            className="p-1 cursor-pointer"
            onClick={() => {
              setSelectedLevel(ViewLevel.TASK_LOGS);
              setSelectedHost(selectedHost);
              setSelectedRequestId(requestId as number);
              setSelectedTask(rowData.Tasks);
            }}
          >
            <FontAwesomeIcon
              icon={status[1] as any}
              className={`text-${status[2]}`}
            />
            <div className="text-info ms-2">
              {commandDetail(
                rowData.Tasks.command_detail,
                requestInputs,
                rowData.Tasks.ops_display_name
              )}
            </div>
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
        <h2>Tasks</h2>
        <Filters
          items={filteredTasks}
          allItems={hostTasks}
          statusKey={"Tasks.status"}
          selectedFilter={selectedFilter}
          setSelectedFilter={setSelectedFilter}
        />
      </Stack>
      <Table columns={columns} data={filteredTasks} />
    </>
  );
}
export default TasksList;
