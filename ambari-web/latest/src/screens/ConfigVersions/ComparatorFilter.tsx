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

import Select from "react-select";
import { FilterLevels } from "./constants";



type ComparatorFilterProps = {
  selectedFilters: any;
  setSelectedFilters: (filters: any) => void;
};

function ComparatorFilter({
  selectedFilters,
  setSelectedFilters,
}: ComparatorFilterProps) {
  const options = [
    { label: "Overridden Properties", value: FilterLevels.OVERRIDDEN },
    { label: "Changed Properties", value: FilterLevels.CHANGED },
    { label: "Final Properties", value: FilterLevels.FINAL },
    { label: "Show property Issues", value: FilterLevels.ISSUES },
    { label: "Clear Filters", value: FilterLevels.CLEAR },
  ];

  const handleFilterChange = (newSelectedFilters: any) => {
    // Check if "Clear Filters" was selected
    const clearFilterSelected = newSelectedFilters?.some(
      (filter: any) => filter.value === FilterLevels.CLEAR
    );

    if (clearFilterSelected) {
      // If "Clear Filters" is selected, clear all filters
      setSelectedFilters([]);
    } else {
      // Otherwise, set the selected filters normally
      setSelectedFilters(newSelectedFilters);
    }
  };

  return (
    <>
      <Select
        options={options}
        isMulti
        className="w-50"
        placeholder="Filters"
        value={selectedFilters}
        onChange={handleFilterChange}
      />
    </>
  );
}

export default ComparatorFilter;
