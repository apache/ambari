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
/* eslint-disable @typescript-eslint/ban-types */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useState } from "react";
import Select, { SingleValue } from "react-select";
import { get } from "lodash";
import { Badge, Button } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faClose } from "@fortawesome/free-solid-svg-icons";
type FilterField = { label: string; value: string };
type PropTypes = {
  fields: FilterField[];
  data: any;
  valueMappings?: { [key: string]: string };
  searchCallback: Function;
};
function ComboSearch({ fields, data, searchCallback }: PropTypes) {
  const [selectedFilters, setSelectedFilters] = useState<
    { field: FilterField; value: FilterField }[]
  >([]);
  const [selectedField, setSelectedField] = useState<FilterField>(
    {} as FilterField
  );
  const [selectedValue, setSelectedValue] = useState<FilterField>(
    {} as FilterField
  );
  const [valueOptions, setValueOptions] = useState<FilterField[]>([]);
  function getCorrespondingValues(){
    const mappedKey = selectedField.value;
      let allValues: any[] = [];
      data.forEach((item: any) => {
        const value = get(item, mappedKey, "");
        if (!value) {
          // Ignore empty value
          return;
        }
        if (Array.isArray(value)) {
          allValues = [...allValues, ...value];
        } else if (typeof value !== "object") {
          allValues.push(value);
        }
      });
      const uniqueValues = [...new Set(allValues)];
      console.log("Unique values", uniqueValues);
      const correspondingValues = uniqueValues.filter(item=>{
        return selectedValue?.value!==item&&!!!selectedFilters?.find(fil=>fil.value.value===item)
      }).map((item: any) => {
        return {
          label: item,
          value: item,
        };
      });
      return correspondingValues;
  }
  useEffect(() => {
    if (selectedField) {
      const correspondingValues = getCorrespondingValues();
      setSelectedValue({} as FilterField);
      setValueOptions(correspondingValues);
    }
  }, [selectedField]);

  useEffect(()=>{
    if(selectedValue){
      const correspondingValues=getCorrespondingValues();
      setValueOptions(correspondingValues);
    }
  },[selectedValue,selectedFilters])

  const filterData = (data: any) => {
    const categoryFilters: { [key: string]: any } = {};

    selectedFilters.forEach((filter) => {
      const category = filter.field.value;
      if (!categoryFilters[category]) {
        categoryFilters[category] = [];
      }
      categoryFilters[category].push(filter.value.value);
    });

    const filteredData = data.filter((item: any) => {
      return Object.keys(categoryFilters).every((category) => {
        // Apply OR logic within the same category
        const itemValue = get(item, category);
        return Array.isArray(itemValue)
          ? itemValue.some((v) => categoryFilters[category].includes(v))
          : categoryFilters[category].includes(itemValue);
      });
    });
    return filteredData;
  };
  useEffect(() => {
    if (selectedFilters.length) {
      searchCallback(filterData(data));
    } else {
      searchCallback(data);
    }
    console.log("Selected Filters", selectedFilters);
  }, [selectedFilters.length]);
  function addFilter(e: any) {
    e.preventDefault();
    const newFilter = { field: selectedField, value: selectedValue };
    if (
      !selectedFilters.some(
        (filter) =>
          filter.field.value === newFilter.field.value &&
          filter.value.value === newFilter.value.value
      )
    ) {
      setSelectedFilters([...selectedFilters, newFilter]);
      setSelectedField(null as any);
      setSelectedValue(null as any)
    }
  }
  function deleteFilter(filterToDelete: {
    field: { label: string; value: any };
    value: { label: string; value: any };
  }) {
    setSelectedFilters((prevFilters) => {
      return prevFilters.filter((filter) => {
        return !(
          filter.field.value === filterToDelete.field.value &&
          filter.value.value === filterToDelete.value.value
        );
      });
    });
  }
  function resetFilters() {
    setSelectedField(null as any);
    setSelectedValue(null as any);
    setSelectedFilters([]);
  }
  return (
    <div className="d-flex w-100 flex-column ease show" data-testid="search-filters">
      <div className="text-muted">
        Select filter(s) to tailor your search. Records update immediately to
        reflect your preferences.
      </div>
      <div className="d-flex mt-2">
        <form onSubmit={addFilter} className="d-flex w-100 align-items-center">
          <Select
            value={selectedField}
            menuPortalTarget={document.body} 
            options={fields}
            placeholder="Select field"
            className="w-25"
            onChange={(value:SingleValue<FilterField>) => {
              setSelectedField(value as FilterField);
            }}
          ></Select>
          <Select
            className="ms-2 w-25"
            menuPortalTarget={document.body} 
            value={selectedValue}
            options={valueOptions}
            placeholder="Value"
            onChange={(value) => {
              setSelectedValue(value as FilterField);
            }}
          ></Select>
          <Button
            disabled={!selectedField?.label || !selectedValue?.value}
            size="sm"
            variant="outline-secondary"
            onClick={addFilter}
            type="submit"
            className="ms-2"
          >
            Add Filter
          </Button>
          <Button
            size="sm"
            variant="outline-danger"
            onClick={resetFilters}
            className="ms-2"
          >
            Reset Filters
          </Button>
        </form>
      </div>
      <div className="mt-2 d-flex flex-wrap">
        {selectedFilters.map((fil, index) => {
          return (
            <Badge
              bg={`secondary d-flex mt-2  align-items-center text-white ${
                index > 0 ? "ms-2" : ""
              }`}
            >
              <div className="text-white">{fil.field.label}:</div>
              <div className="ms-2 text-white">{fil.value.label}</div>
              <FontAwesomeIcon
                icon={faClose}
                onClick={() => {
                  deleteFilter(fil);
                }}
                className="delete-filter cursot-pointer ms-2"
              />
            </Badge>
          );
        })}
      </div>
    </div>
  );
}

export default ComboSearch;
