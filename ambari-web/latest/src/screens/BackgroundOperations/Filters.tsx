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

import {get} from "lodash";
import Select from "react-select";
import { statusMatchesFilter } from "../../Utils/backgroundOperations";
type FiltersProps = {
  items: any;
  allItems: any;
  statusKey: string;
  selectedFilter: any;
  setSelectedFilter: any;
  successLevel?:'completed'|'success'
};
function Filters({
  allItems,
  statusKey,
  selectedFilter,
  setSelectedFilter,
  successLevel='completed'
}: FiltersProps) {
  const filterMap = {
    pending: "pending",
    in_progress: "in_progress",
    failed: "failed",
    completed: "completed",
    success:"success",
    aborted: "aborted",
    timedout: "timedout",
    all: "",
  };
  const filterOptions = () => [
    {
      label: `All (${allItems.length})`,
      value: filterMap.all,
    },
    {
      label: `Pending (${
        allItems.filter((item: any) => statusMatchesFilter(get(item, statusKey), "PENDING"))
          .length
      })`,
      value: filterMap.pending,
    },
    {
      label: `In Progress (${
        allItems.filter((item: any) => get(item, statusKey) === "IN_PROGRESS")
          .length
      })`,
      value: filterMap.in_progress,
    },
    {
      label: `Failed (${
        allItems.filter((item: any) => statusMatchesFilter(get(item, statusKey), "FAILED")).length
      })`,
      value: filterMap.failed,
    },
    {
      label: `Success (${
        allItems.filter((item: any) => statusMatchesFilter(get(item, statusKey), "SUCCESS"))
          .length
      })`,
      value: successLevel === 'completed' ? filterMap.completed : filterMap.success,
    },
    {
      label: `Aborted (${
        allItems.filter((item: any) => get(item, statusKey) === "ABORTED")
          .length
      })`,
      value: filterMap.aborted,
    },
    {
      label: `Timed Out (${
        allItems.filter((item: any) => get(item, statusKey) === "TIMEDOUT")
          .length
      })`,
      value: filterMap.timedout,
    },
  ];
  const currentOptions = filterOptions();

  const currentSelection = currentOptions.find(
    (option) => option.value === selectedFilter?.value,
  ) || currentOptions[0];
  return (
    <>
      <Select
        options={currentOptions}
        styles={{
          // Fixes the overlapping problem of the component
          menu: (provided) => ({ ...provided, zIndex: 9999 }),
        }}
        value={currentSelection}
        className="w-25"
        isSearchable={false}
        onChange={(value) => {
          setSelectedFilter(value);
        }}
      />
    </>
  );
}
export default Filters;
