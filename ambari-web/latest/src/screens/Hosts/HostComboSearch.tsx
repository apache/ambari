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
import Select from "react-select";
import { get, isEmpty } from "lodash";
import { Badge, Button } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faClose } from "@fortawesome/free-solid-svg-icons";
import { serviceNameDisplayMapping } from "../../constants";
import { ComponentStatus } from "./enums";
import HostComponent from "../../models/hostComponent";
import { IHost } from "../../models/host";
import HostStackVersion from "../../models/hostStackVersion";
import {
  healthClassesForHostFilter,
  nonRepeatableHostFieldOptions,
} from "./constants";
import { HostsApi } from "../../api/hostsApi";
import { AppContext } from "../../store/context";
import { translate } from "../../Utils/Utility";

type FilterField = { label: string; value: string; name?: string };
export type SelectedFilters = {
  field: FilterField;
  value: FilterField;
}[];

type HostComboSearchProps = {
  showFilters: boolean;
  allHostModels: IHost[];
  clusterComponents: any;
  searchCallback: (data: any) => void;
  selectedFilters: SelectedFilters;
  setSelectedFilters: (
    filters: SelectedFilters | ((prev: SelectedFilters) => SelectedFilters)
  ) => void;
  onResetFilters?: () => void;
};

function HostComboSearch({
  showFilters,
  allHostModels,
  clusterComponents,
  searchCallback,
  selectedFilters,
  setSelectedFilters,
  onResetFilters,
}: HostComboSearchProps) {
  const { clusterName } = useContext(AppContext);
  const [selectedField, setSelectedField] = useState<FilterField | null>(null);
  const [selectedValue, setSelectedValue] = useState<FilterField | null>(null);
  const [valueOptions, setValueOptions] = useState<FilterField[]>([]);
  const [groupedFieldOptions, setGroupedFieldOptions] = useState<any[]>([]);

  useEffect(() => {
    updateGroupedFieldOptions();
  }, [clusterComponents]);

  useEffect(() => {
    setValuesOnFieldChange();
  }, [selectedField]);

  useEffect(() => {
    setValuesOnValueChange();
  }, [selectedValue]);

  useEffect(() => {
    searchCallback(selectedFilters);
    updateGroupedFieldOptions();
  }, [selectedFilters.length]);

  const updateGroupedFieldOptions = () => {
    const filteredHostOptions = hostOptions.filter((option) => {
      return !(
        nonRepeatableHostFieldOptions.includes(option.value) &&
        selectedFilters.some((filter) => filter.field.value === option.value)
      );
    });
    let groupedFieldOptionsList = [
      {
        label: "Host",
        options: filteredHostOptions,
      },
      {
        label: "Service",
        options: serviceOptions,
      },
    ];
    if (
      selectedFilters.length === 0 ||
      selectedFilters.every(
        (filter) => !(filter.field.name === "componentState")
      )
    ) {
      groupedFieldOptionsList.push({
        label: "Component",
        options: getComponentFieldOptions(),
      });
    }
    setGroupedFieldOptions(groupedFieldOptionsList);
  };

  const setValuesOnValueChange = async () => {
    const fieldValue = selectedField?.value || "";
    if (["hostName", "ip"].includes(fieldValue)) {
      const correspondingValues = await getCorrespondingValues();
      setValueOptions(correspondingValues);
    }
  };

  const setValuesOnFieldChange = async () => {
    if (!isEmpty(selectedField)) {
      const correspondingValues = await getCorrespondingValues();
      setSelectedValue(null);
      setValueOptions(correspondingValues);
    } else {
      setSelectedValue(null);
      setValueOptions([]);
    }
  };

  const getComponentFieldOptions = () => {
    return get(clusterComponents, "items", [])
      .filter(
        (component: any) => get(component, "host_components", []).length > 0
      )
      .map((component: any) => ({
        label: get(component, "host_components.[0].HostRoles.display_name", ""),
        value: get(
          component,
          "host_components.[0].HostRoles.component_name",
          ""
        ),
        name: "componentState",
      }))
      .sort((a: FilterField, b: FilterField) => a.label.localeCompare(b.label));
  };

  const hostOptions = [
    { label: "Host Name", value: "hostName", name: "host" },
    { label: "IP", value: "ip", name: "host" },
    { label: "Host Status", value: "healthClass", name: "host" },
    { label: "Cores", value: "cpu", name: "host" },
    { label: "RAM", value: "memoryFormatted", name: "host" },
    { label: "Stack Version", value: "version", name: "host" },
    { label: "Version State", value: "versionState", name: "host" },
    { label: "Rack", value: "rack", name: "host" },
  ];

  const serviceOptions = [
    { label: "Service", value: "services", name: "service" },
  ];

  async function getCorrespondingValues() {
    if (!selectedField) return [];
    let valueOptionsList: FilterField[] = [];
    const fieldname = selectedField.name;
    if (fieldname === "service") {
      valueOptionsList = getServiceValueOptions();
    } else if (fieldname === "componentState") {
      valueOptionsList = getComponentValueOptions();
    } else {
      valueOptionsList = await getHostValueOptions();
    }
    return valueOptionsList;
  }

  const getHostValueOptions = async () => {
    if (!selectedField) return [];
    const fieldValue = selectedField.value;
    let hostValueOptions: FilterField[] = [];
    switch (fieldValue) {
      case "hostName":
      case "ip":
        hostValueOptions = await searchByHostname();
        break;
      case "rack":
        hostValueOptions = searchByRack();
        break;
      case "version":
        hostValueOptions = searchByVersion();
        break;
      case "versionState":
        hostValueOptions = searchByVersionState();
        break;
      case "healthClass":
        hostValueOptions = searchByHealthClass();
        break;
    }
    return hostValueOptions;
  };

  const getServiceValueOptions = () => {
    if (!selectedField) return [];
    const fieldValue = selectedField.value;
    let serviceValueOptions: FilterField[] = [];
    if (fieldValue === "services") {
      get(clusterComponents, "items", []).forEach((component: any) => {
        const serviceName = get(
          component,
          "ServiceComponentInfo.service_name",
          ""
        );
        if (
          serviceName &&
          get(component, "host_components", []).length > 0 &&
          !serviceValueOptions.some((option) => option.value === serviceName)
        ) {
          serviceValueOptions.push({
            label: get(serviceNameDisplayMapping, serviceName, serviceName),
            value: serviceName,
          });
        }
      });
    }
    serviceValueOptions.sort((a, b) => a.label.localeCompare(b.label));
    return serviceValueOptions;
  };

  const getComponentValueOptions = () => {
    if (!selectedField) return [];
    const fieldValue = selectedField.value;
    let componentValueOptions: FilterField[] = [
      {
        label: "All",
        value: "ALL",
      },
    ];

    if (!fieldValue.toLowerCase().includes("client")) {
      componentValueOptions = componentValueOptions.concat(
        Object.keys(ComponentStatus)
          .filter((status: string) => status !== "UPGRADE_FAILED")
          .map((status: string) => ({
            label: HostComponent.getTextStatus(
              ComponentStatus[status as keyof typeof ComponentStatus]
            ),
            value: ComponentStatus[status as keyof typeof ComponentStatus],
          }))
      );
      componentValueOptions = componentValueOptions.concat([
        { label: "Inservice", value: "INSERVICE" },
        {
          label: "Decommissioned",
          value: "DECOMMISSIONED",
        },
        {
          label: "Decommissioning",
          value: "DECOMMISSIONING",
        },
        {
          label: "RS Decommissioned",
          value: "RS_DECOMMISSIONED",
        },
        { label: "Maintenance Mode On", value: "ON" },
        { label: "Maintenance Mode Off", value: "OFF" },
      ]);
    }

    return componentValueOptions;
  };

  const getPropertySuggestions = async (fieldValue: string) => {
    try {
      const data = {
        filter: fieldValue,
        searchTerm: selectedValue?.value || "",
        pageSize: 10,
      };
      const response = await HostsApi.getHostListFilterSuggestions(
        clusterName,
        data
      );
      return response;
    } catch (error) {
      console.error("Error getting property suggestions:", error);
    }
    return "";
  };

  const searchByHostname = async () => {
    let hostNameValueOptions: FilterField[] = [];
    let fieldValue = selectedField?.value || "";
    if (fieldValue === "hostName") fieldValue = "host_name";
    const suggestions = await getPropertySuggestions(fieldValue);
    if (suggestions) {
      get(suggestions, "items", []).forEach((host: any) => {
        const hostName = get(host, "Hosts." + fieldValue, "");
        if (
          hostName &&
          !hostNameValueOptions.some((option) => option.value === hostName)
        ) {
          hostNameValueOptions.push({ label: hostName, value: hostName });
        }
      });
    }
    return hostNameValueOptions;
  };

  const searchByRack = () => {
    let rackValueOptions: FilterField[] = [];
    allHostModels.forEach((host: IHost) => {
      const rack = get(host, "rack", "");
      if (rack && !rackValueOptions.some((option) => option.value === rack)) {
        rackValueOptions.push({ label: rack, value: rack });
      }
    });
    rackValueOptions.sort((a, b) => a.label.localeCompare(b.label));
    return rackValueOptions;
  };

  const searchByVersion = () => {
    let versionValueOptions: FilterField[] = [];
    allHostModels.forEach((host: IHost) => {
      const stackVersion = get(host, "stackVersions", []).find(
        (version: any) => {
          return get(version, "status", "") === "CURRENT";
        }
      );
      const versionName = get(stackVersion, "displayName", "");
      if (
        versionName &&
        !versionValueOptions.some((option) => option.value === versionName)
      ) {
        versionValueOptions.push({
          label: versionName,
          value: versionName,
        });
      }
    });
    versionValueOptions.sort((a, b) => a.label.localeCompare(b.label));
    return versionValueOptions;
  };

  const searchByVersionState = () => {
    let versionStateValueOptions: FilterField[] = [];
    HostStackVersion.statusDefinition.forEach((status: string) => {
      versionStateValueOptions.push({
        label: HostStackVersion.formatStatus(status),
        value: status,
      });
    });
    versionStateValueOptions.sort((a, b) => a.label.localeCompare(b.label));
    return versionStateValueOptions;
  };

  const searchByHealthClass = () => {
    let healthClassValueOptions: FilterField[] = [];
    healthClassesForHostFilter.forEach((healthClass: any) => {
      healthClassValueOptions.push({
        label: healthClass.label,
        value: healthClass.value,
        name: healthClass.name,
      });
    });
    return healthClassValueOptions;
  };

  function addFilter(e: any) {
    e.preventDefault();
    if (!selectedField || !selectedValue) return;
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
      setSelectedValue(null as any);
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
    if (onResetFilters) {
      onResetFilters();
    } else {
      setSelectedFilters([]);
    }
  }

  return (
    <>
      {showFilters ? (
        <div
          className="d-flex w-100 flex-column ease show ms-2"
          data-testid="search-filters"
        >
          <div className="text-muted">
            {translate("hosts.combo.search.placebolder")}
          </div>
          <div className="d-flex mt-2">
            <form
              onSubmit={addFilter}
              className="d-flex w-100 align-items-center"
            >
              <Select
                value={selectedField}
                onChange={(value) => {
                  setSelectedField(value as FilterField);
                }}
                options={groupedFieldOptions}
                placeholder="Select field"
                className="w-15 me-2"
                isClearable
                menuPortalTarget={document.body}
              />
              <Select
                value={selectedValue}
                options={valueOptions}
                placeholder="Select Value"
                className="w-15"
                isClearable
                menuPortalTarget={document.body}
                onChange={(value) => {
                  if (selectedValue?.value !== value?.value) {
                    setSelectedValue(value as FilterField);
                  }
                }}
                onInputChange={(inputValue, actionMeta) => {
                  if (
                    actionMeta.action === "input-change" &&
                    !isEmpty(selectedField) &&
                    selectedValue?.value !== inputValue
                  ) {
                    setSelectedValue({
                      label: inputValue,
                      value: inputValue,
                    } as FilterField);
                  }
                }}
              />
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
      ) : null}
    </>
  );
}

export default HostComboSearch;
