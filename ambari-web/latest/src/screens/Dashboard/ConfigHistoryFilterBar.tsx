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

import { useState, useEffect } from "react";
import Select from "react-select";
import CreatableSelect from "react-select/creatable";
import { Badge, Button } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faClose } from "@fortawesome/free-solid-svg-icons";

// Define filter fields
const filterFieldOptions = [
  { label: "Service", value: "service_name" },
  { label: "Config Group", value: "group_name" },
  { label: "Author", value: "user" },
  { label: "Notes", value: "service_config_version_note" },
  { label: "Created", value: "createtime" },
];

// Predefined options for createtime
const createtimeOptions = [
  { label: "Last 1 hour", value: "1h" },
  { label: "Last 1 day", value: "1d" },
  { label: "Last 2 days", value: "2d" },
  { label: "Last 7 days", value: "7d" },
  { label: "Last 14 days", value: "14d" },
  { label: "Last 30 days", value: "30d" },
];

type FilterField = { label: string; value: string };
type Filter = { field: FilterField; value: { label: string; value: string } };

type Props = {
  filters: Filter[];
  setFilters: (filters: Filter[]) => void;
  serviceOptions?: { label: string; value: string }[];
  groupOptions?: { label: string; value: string }[];
  userOptions?: { label: string; value: string }[];
  notesOptions?: { label: string; value: string }[];
  addFilterCallback?: () => void;
};

export default function ConfigHistoryComboSearch({
  filters,
  setFilters,
  serviceOptions = [],
  groupOptions = [],
  userOptions = [],
  notesOptions = [],
  addFilterCallback,
}: Props) {
  const [selectedField, setSelectedField] = useState<FilterField | null>(null);
  const [selectedValue, setSelectedValue] = useState<{
    label: string;
    value: string;
  } | null>(null);
  const [valueOptions, setValueOptions] = useState<
    { label: string; value: string }[]
  >([]);

  // Filter out value options already used for the selected field
  useEffect(() => {
    if (!selectedField) {
      setValueOptions([]);
      setSelectedValue(null);
      return;
    }

    const usedValues = filters
      .filter((f) => f.field.value === selectedField.value)
      .map((f) => f.value.value);

    let options: { label: string; value: string }[] = [];
    switch (selectedField.value) {
      case "service_name":
        options = serviceOptions;
        break;
      case "group_name":
        options = groupOptions;
        break;
      case "user":
        options = userOptions;
        break;
      case "service_config_version_note":
        options = notesOptions;
        break;
      case "createtime":
        options = createtimeOptions;
        break;
      default:
        options = [];
    }

    setValueOptions(options.filter((opt) => !usedValues.includes(opt.value)));
    setSelectedValue(null);
  }, [selectedField, filters, serviceOptions, groupOptions, userOptions, notesOptions]);

  function addFilter(e: any) {
    e.preventDefault();
    if (!selectedField || !selectedValue) return;
    const valueObj = selectedValue as { label: string; value: string };
    if (
      !filters.some(
        (f) =>
          f.field.value === selectedField.value &&
          f.value.value === valueObj.value
      )
    ) {
      setFilters([...filters, { field: selectedField, value: valueObj }]);
      setSelectedField(null);
      setSelectedValue(null);
      if (addFilterCallback) {
        addFilterCallback();
      }
    }
  }

  function deleteFilter(filterToDelete: Filter) {
    setFilters(
      filters.filter(
        (f) =>
          !(
            f.field.value === filterToDelete.field.value &&
            f.value.value === filterToDelete.value.value
          )
      )
    );
    addFilterCallback?.();
  }

  // Reset all filters
  function resetFilters() {
    setFilters([]);
    setSelectedField(null);
    setSelectedValue(null);
    addFilterCallback?.();
  }

  return (
    <div className="d-flex flex-column mb-3">
      <form className="d-flex align-items-center" onSubmit={addFilter}>
        <Select
          value={selectedField}
          onChange={(value) => setSelectedField(value as FilterField)}
          options={filterFieldOptions}
          placeholder="Select field"
          className="w-15 me-2"
          isClearable
        />
        {selectedField?.value === "user" || selectedField?.value === "service_config_version_note" ? (
          <CreatableSelect
            value={selectedValue}
            onChange={(value) => setSelectedValue(value as { label: string; value: string })}
            options={valueOptions}
            placeholder="Value"
            className="w-15 me-2"
            isClearable
            formatCreateLabel={(value) => `Use "${value}"`}
          />
        ) : (
          <Select
            value={selectedValue}
            onChange={(value) => setSelectedValue(value as { label: string; value: string })}
            options={valueOptions}
            placeholder="Value"
            className="w-15 me-2"
            isClearable
            isDisabled={!selectedField}
          />
        )}
        <Button
          disabled={!selectedField || !selectedValue}
          size="sm"
          variant="outline-secondary"
          type="submit"
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
      <div className="mt-2 d-flex flex-wrap">
        {filters.map((fil, idx) => (
          <Badge
            key={fil.field.value + fil.value.value}
            bg="secondary"
            className={`d-flex align-items-center text-white mt-2 ${
              idx > 0 ? "ms-2" : ""
            }`}
          >
            <div>{fil.field.label}:</div>
            <div className="ms-2">{fil.value.label}</div>
            <FontAwesomeIcon
              icon={faClose}
              onClick={() => deleteFilter(fil)}
              className="ms-2 cursor-pointer"
              style={{ cursor: "pointer" }}
            />
          </Badge>
        ))}
      </div>
    </div>
  );
}
