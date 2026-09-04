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


import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Alert, Button, Card, Form, ProgressBar } from "react-bootstrap";
import { cloneDeep, get, startCase } from "lodash";
import DefaultButton from "../../components/DefaultButton";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCheckCircle,
  faFilter,
  faMedkit,
  faMinusCircle,
  faPlus,
  faQuestionCircle,
  faRefresh,
  faSort,
  faSortAsc,
  faSortDesc,
  faWarning,
} from "@fortawesome/free-solid-svg-icons";
import Table from "../../components/Table";
import bytesToSize from "../../Utils/numberUtils";
import Modal from "../../components/Modal";
import Tooltip from "../../components/Tooltip";
import { Link } from "react-router-dom";
import { ComponentStatus, HostStatus } from "./enums";
import { serviceNameToModelKeyMap, sortByColIdToKeyMapping } from "./constants";
import NestedDropdown from "../../components/NestedDropdown";
import {
  bulkOperationConfirm,
  isBulkComponentDeleteVisible,
} from "./bulkOperations";
import { AppContext } from "../../store/context";
import useStackVersion from "../../hooks/useStackVersion";
import { ServiceContext } from "../../store/ServiceContext";
import { useHostsListState } from "../../store/HostsListStateContext";
import { IHostComponent } from "../../models/hostComponent";
import { IHostStackVersion } from "../../models/hostStackVersion";
import Host, { IHost } from "../../models/host";
import { useHostConfigUpdater } from "../../hooks/useHostConfigUpdater";
import {
  getAllComponents,
  getClusterComponentsCount,
  getComponentDisplayName,
  getComponentName,
  getPopulatedHostComponentObject,
  minToInstall,
} from "./utils";
import Spinner from "../../components/Spinner";
import { HostsApi } from "../../api/hostsApi";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import Paginator from "../../components/Paginator";
import HostComboSearch from "./HostComboSearch";
import { getQueryParameters } from "./host";
import { computeParameters } from "../../globals/updateControl";
import { translate, translateWithVariables } from "../../Utils/Utility";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";

export const getCmponentsToBeRestarted = (data: IHost) => {
  return get(data, "hostComponents", []).filter((item: any) =>
    get(item, "staleConfigs", false)
  );
};

export default function HostsList() {
  const { clusterName, serviceComponentInfo } = useContext(AppContext);
  const { allServiceModels: serviceModels, polledHostComponentsData } = useContext(ServiceContext);
  const [loading, setLoading] = useState(true);
  const [paginationLoading, setPaginationLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [currentHostModels, setCurrentHostModels] = useState<Host[]>([]);
  const [allHostCount, setAllHostCount] = useState(0);
  const {
    selectedFilters,
    setSelectedFilters,
    selectedHosts,
    setSelectedHosts,
  } = useHostsListState();
  // Keep restored filters visible instead of hidden behind a collapsed bar.
  const [showFilters, setShowFilters] = useState(
    () => selectedFilters.length > 0
  );
  const [filterString, setFilterString] = useState<string>(() =>
    selectedFilters.length > 0
      ? computeParameters(getQueryParameters(selectedFilters))
      : ""
  );
  const clearFilters = useCallback(() => {
    setSelectedFilters([]);
  }, [setSelectedFilters]);
  // Must carry the filter: the fetch keys on this object, so without it mount
  // sends an unfiltered request and every host shows for one cycle.
  const [hostApiQueryParams, setHostApiQueryParams] = useState<any>(() => {
    const initialFilter =
      selectedFilters.length > 0
        ? computeParameters(getQueryParameters(selectedFilters))
        : "";
    return {
      pageSize: 10,
      startFrom: 0,
      sortBy: "Hosts/host_name",
      sortOrder: "asc",
      RequestInfo: {
        query: initialFilter
          ? `page_size=10&from=0&${initialFilter}`
          : `page_size=10&from=0`,
      },
    };
  });
  const [clusterComponents, setClusterComponents] = useState<any>({});
  const [clusterLoadError, setClusterLoadError] = useState<string | null>(null);
  const [clusterRetryCount, setClusterRetryCount] = useState(0);
  const [maxPage, setMaxPage] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [totalItems, setTotalItems] = useState(0);
  const { stackVersionList } = useStackVersion();

  // Authorization hooks - implementing Ember.js host authorization patterns
  const { havePermissions, isAuthorized } = useAuthorizationPolicy();

  // Check specific authorizations for host operations
  const canStartStopServices = isAuthorized("SERVICE.START_STOP");
  const canToggleHostMaintenance = isAuthorized("HOST.TOGGLE_MAINTENANCE");
  const canAddDeleteHosts = isAuthorized("HOST.ADD_DELETE_HOSTS");
  const canAddDeleteComponents = isAuthorized("HOST.ADD_DELETE_COMPONENTS");
  const canDecommissionRecommission = isAuthorized(
    "SERVICE.DECOMMISSION_RECOMMISSION"
  );

  // Overall permission check for Host Actions menu - matches Ember template logic
  // {{#havePermissions "HOST.ADD_DELETE_COMPONENTS, HOST.TOGGLE_MAINTENANCE, HOST.ADD_DELETE_HOSTS"}}
  const canShowHostActions = havePermissions("HOST.ADD_DELETE_COMPONENTS, HOST.TOGGLE_MAINTENANCE, HOST.ADD_DELETE_HOSTS");

  // Clear the spinner when a host response lands - not on the cluster-wide
  // components poll, which can arrive first and show an empty table.
  const applyHostModels = useCallback(
    (models: Host[] | ((prev: Host[]) => Host[])) => {
      setCurrentHostModels(models);
      setLoading(false);
    },
    []
  );

  const hostData = useHostConfigUpdater(
    hostApiQueryParams,
    applyHostModels,
    setTotalItems,
    setPaginationLoading
  );

  const modalProps = useRef({
    title: "",
    body: <div></div>,
  });
  const sortState = useRef({
    columnName: "hostname",
    order: "asc",
  });

  // Filters can arrive after mount, set by another page before navigating here.
  useEffect(() => {
    if (selectedFilters.length > 0) {
      setShowFilters(true);
    }
  }, [selectedFilters.length]);

  // Reuse centralized polled data from CachedServiceApi instead of making a separate API call
  // This eliminates the duplicate /components/ call that was previously polled independently
  useEffect(() => {
    if (polledHostComponentsData?.items) {
      setClusterComponents(polledHostComponentsData);
      setClusterLoadError(null);
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    if (clusterName && clusterRetryCount > 0) {
      void getClusterComponents();
    }
  }, [clusterName, clusterRetryCount]);

  useEffect(() => {
    if (!filterString) {
      setAllHostCount(totalItems);
    }
  }, [filterString, totalItems]);

  useEffect(() => {
    const queryParams = getQueryParameters(selectedFilters);
    const computedQueryParams = computeParameters(queryParams);
    setFilterString(computedQueryParams);
  }, [selectedFilters]);

  useEffect(() => {
    setMaxPage(Math.ceil(totalItems / itemsPerPage));
  }, [itemsPerPage, totalItems]);

  useEffect(() => {
    const hostApiQueryParamsCopy = cloneDeep(hostApiQueryParams);
    if (filterString) {
      hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=${
        (currentPage - 1) * itemsPerPage
      }&${filterString}`;
    } else {
      hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=${
        (currentPage - 1) * itemsPerPage
      }`;
    }
    // Skip no-op rebuilds; the fetch effect keys on this object by reference.
    if (
      hostApiQueryParamsCopy.RequestInfo.query ===
      get(hostApiQueryParams, "RequestInfo.query", "")
    ) {
      return;
    }
    setPaginationLoading(true);
    setHostApiQueryParams(hostApiQueryParamsCopy);
    setCurrentPage(1);
  }, [filterString]);

  useEffect(() => {
    if (currentPage == 1) {
      const hostApiQueryParamsCopy = cloneDeep(hostApiQueryParams);
      hostApiQueryParamsCopy.pageSize = itemsPerPage;
      hostApiQueryParamsCopy.startFrom = 0;
      if (filterString) {
        hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=0&${filterString}`;
      } else {
        hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=0`;
      }
      if (
        hostApiQueryParamsCopy.RequestInfo.query ===
        get(hostApiQueryParams, "RequestInfo.query", "")
      ) {
        return;
      }
      setPaginationLoading(true);
      setHostApiQueryParams(hostApiQueryParamsCopy);
    } else {
      setCurrentPage(1);
    }
  }, [itemsPerPage]);

  useEffect(() => {
    const hostApiQueryParamsCopy = cloneDeep(hostApiQueryParams);
    hostApiQueryParamsCopy.pageSize = itemsPerPage;
    hostApiQueryParamsCopy.startFrom = (currentPage - 1) * itemsPerPage;
    if (filterString) {
      hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=${
        (currentPage - 1) * itemsPerPage
      }&${filterString}`;
    } else {
      hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=${
        (currentPage - 1) * itemsPerPage
      }`;
    }
    if (
      hostApiQueryParamsCopy.RequestInfo.query ===
      get(hostApiQueryParams, "RequestInfo.query", "")
    ) {
      return;
    }
    setPaginationLoading(true);
    setHostApiQueryParams(hostApiQueryParamsCopy);
  }, [currentPage]);

  const { getKDCSessionState } = useKDCSessionState(() => {});

  const changePage = (newPage: number) => {
    const safePage = Math.max(1, Math.min(newPage, maxPage));
    setCurrentPage(safePage);
  };

  const getClusterComponents = async () => {
    setLoading(true);
    setClusterLoadError(null);
    try {
      const response = await HostsApi.getClusterComponents(
        clusterName,
        "ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,&minimal_response=true"
      );
      setClusterComponents(response);
    } catch (error: any) {
      setClusterLoadError(
        error?.response?.data?.message || "Ambari could not load host component data.",
      );
    } finally {
      setLoading(false);
    }
  };

  const isAllSelected = () => {
    if (!currentHostModels.length) return false;
    return currentHostModels.every((host) =>
      selectedHosts.includes(get(host, "hostName"))
    );
  };

  const handleSelect = (host: IHost) => {
    const hostName = get(host, "hostName");
    if (selectedHosts.includes(hostName)) {
      setSelectedHosts(selectedHosts.filter((name) => name !== hostName));
    } else {
      setSelectedHosts([...selectedHosts, hostName]);
    }
  };

  const handleSelectAll = () => {
    const allHostNames = currentHostModels.map((host) => get(host, "hostName"));
    if (isAllSelected()) {
      setSelectedHosts(
        selectedHosts.filter((name) => !allHostNames.includes(name))
      );
    } else {
      setSelectedHosts([...new Set([...selectedHosts, ...allHostNames])]);
    }
  };

  const getCurrentVersion = (hostData: any) => {
    const currentVersions = getAllVersions(hostData).filter(
      (version: any) => version.state === "CURRENT"
    );
    return get(currentVersions, "[0].version", "");
  };

  const getAllVersions = (hostData: any) => {
    const stackVersions = get(hostData, "stackVersions", []);
    return stackVersions.map((stackVersion: IHostStackVersion) => ({
      version: get(stackVersion, "displayName", ""),
      state: get(stackVersion, "status"),
    }));
  };

  const showAllVersionsModal = (hostName: string, allVersions: any) => {
    modalProps.current = {
      title: "Versions",
      body: (
        <div>
          <div className="fs-12 mb-2">{hostName}</div>
          <div className="scrollable-h15 border border-bottom-0">
            {allVersions.map((version: any) => (
              <div key={get(version, "version", "")}>
                <div className="p-1 d-flex">
                  <div className="fs-12 w-50">
                    {get(version, "version", "")}
                  </div>
                  <div className="fs-12">{get(version, "state", "")}</div>
                </div>
                <hr className="m-0" />
              </div>
            ))}
          </div>
        </div>
      ),
    };
    setShowModal(true);
  };

  const showAllComponentsModal = (
    hostName: string,
    allComponents: string[]
  ) => {
    modalProps.current = {
      title: "Components",
      body: (
        <div>
          <div className="fs-12 mb-2">{hostName}</div>
          <div className="scrollable-h15 border">
            {allComponents.map((component) => (
              <div className="fs-12 p-1" key={component}>
                {component}
              </div>
            ))}
          </div>
        </div>
      ),
    };
    setShowModal(true);
  };

  const getVersionsColumn = (hostData: any) => {
    const currentVersion = getCurrentVersion(hostData);
    if (currentVersion) {
      return currentVersion;
    }
    const allVersions = getAllVersions(hostData);
    return (
      <div
        className="custom-link"
        onClick={() =>
          showAllVersionsModal(get(hostData, "hostName", ""), allVersions)
        }
      >
        {get(allVersions, "[0].version", "")}
      </div>
    );
  };

  const getComponentsColumn = (hostData: any) => {
    const allComponents = get(hostData, "hostComponents", []).map(
      (component: IHostComponent) => get(component, "displayName", "")
    );
    return (
      <div
        className="custom-link"
        onClick={() =>
          showAllComponentsModal(get(hostData, "hostName", ""), allComponents)
        }
      >
        {allComponents.length}
        {" Components"}
      </div>
    );
  };

  const getDownMasters = useCallback(
    (hostData: any) => {
      const downMasters = get(hostData, "hostComponents", []).filter(
        (component: IHostComponent) =>
          get(component, "isMaster", false) &&
          get(component, "workStatus", "" as ComponentStatus) !==
            ComponentStatus.STARTED
      );
      return downMasters.map((component: IHostComponent) =>
        getComponentDisplayName(component)
      );
    },
    [currentHostModels]
  );

  const getDownSlaves = useCallback(
    (hostData: any) => {
      const downSlaves = get(hostData, "hostComponents", []).filter(
        (component: IHostComponent) =>
          !get(component, "isMaster", false) &&
          !get(component, "isClient", false) &&
          get(component, "workStatus", "" as ComponentStatus) !==
            ComponentStatus.STARTED
      );
      return downSlaves.map((component: IHostComponent) =>
        getComponentDisplayName(component)
      );
    },
    [currentHostModels]
  );

  const getHostStatusIcon = (hostData: IHost) => {
    const currHostStatus = get(hostData, "healthStatus", "");
    let message = "";
    let icon = <div></div>;
    switch (currHostStatus) {
      case HostStatus.UNKNOWN:
        message =
          "The server has not received a heartbeat from this host for more than 3 minutes.";
        icon = (
          <FontAwesomeIcon icon={faQuestionCircle} className="text-warning" />
        );
        break;
      case HostStatus.UNHEALTHY:
        message = `The following master components are down: ${getDownMasters(
          hostData
        ).join(", ")}`;
        icon = <FontAwesomeIcon icon={faWarning} className="text-danger" />;
        break;
      case HostStatus.HEALTHY:
        message = "All components are up";
        icon = (
          <FontAwesomeIcon icon={faCheckCircle} className="text-success" />
        );
        break;
      case HostStatus.ALERT:
        message = `The following slave components are down: ${getDownSlaves(
          hostData
        ).join(", ")}`;
        icon = <FontAwesomeIcon icon={faMinusCircle} className="text-orange" />;
        break;
    }

    if (get(hostData, "passiveState", "OFF") !== "OFF") {
      message = "Host is in Maintenance Mode";
      icon = <FontAwesomeIcon icon={faMedkit} className="text-dark" />;
    }

    return <Tooltip message={message}>{icon}</Tooltip>;
  };

  const getHostNameColumn = (hostData: any) => {
    const hostName = get(hostData, "hostName", "");
    const alertsCount =
      get(hostData, "alertsSummary.CRITICAL", 0) +
      get(hostData, "alertsSummary.WARNING", 0);
    const alertsStyleClass =
      get(hostData, "alertsSummary.CRITICAL", 0) > 0
        ? "bg-danger"
        : "bg-orange";
    const componentsToBeRestarted = getCmponentsToBeRestarted(hostData);
    const componentsToBeRestartedCount = componentsToBeRestarted.length;
    let restartTooltipMessage = "";
    if (componentsToBeRestartedCount <= 5 && componentsToBeRestartedCount > 0) {
      const word =
        componentsToBeRestartedCount === 1
          ? (translate("common.component") as string)
          : (translate("common.components") as string);
      restartTooltipMessage = translateWithVariables(
        "hosts.table.restartComponents.withNames",
        {
          "0":
            componentsToBeRestarted.map(getComponentDisplayName).join(", ") +
            " " +
            word.toLowerCase(),
        }
      ) as string;
    } else if (componentsToBeRestartedCount > 5) {
      restartTooltipMessage = translateWithVariables(
        "hosts.table.restartComponents.withoutNames",
        {
          "0": componentsToBeRestartedCount.toString(),
        }
      ) as string;
    }
    const componentsInMaintenance = get(hostData, "hostComponents", []).filter(
      (component: IHostComponent) =>
        get(component, "passiveState", "OFF") !== "OFF"
    );
    return (
      <div className="d-flex">
        <div className="me-1">{getHostStatusIcon(hostData)}</div>
        <Link to={`/main/hosts/${hostName}/summary`} className="custom-link">
          <div className="me-1">{hostName}</div>
        </Link>
        <div className="me-1">
          {alertsCount > 0 ? (
            <Link to={`/main/hosts/${hostName}/alerts`}>
              <Button
                className={`me-1 ${alertsStyleClass} text-white fs-10 rounded-1 px-1 py-0`}
              >
                {alertsCount}
              </Button>
            </Link>
          ) : null}
        </div>
        <div className="me-1">
          {restartTooltipMessage ? (
            <Tooltip message={restartTooltipMessage}>
              <FontAwesomeIcon icon={faRefresh} className="text-muted" />
            </Tooltip>
          ) : null}
        </div>
        <div className="me-1">
          {componentsInMaintenance.length > 0 ? (
            <Tooltip
              message={`${componentsInMaintenance.length} components in Maintenance Mode`}
            >
              <FontAwesomeIcon icon={faMedkit} className="text-dark" />
            </Tooltip>
          ) : null}
        </div>
      </div>
    );
  };

  const getSortIcon = (colName: string) => {
    if (sortState.current.columnName !== colName) {
      return <FontAwesomeIcon className="text-muted" icon={faSort} />;
    }
    if (sortState.current.order === "asc") {
      return <FontAwesomeIcon className="text-info" icon={faSortAsc} />;
    }
    return <FontAwesomeIcon className="text-info" icon={faSortDesc} />;
  };

  const handleSortClick = (colName: string) => {
    if (sortState.current.columnName === colName) {
      sortState.current.order =
        sortState.current.order === "asc" ? "desc" : "asc";
    } else {
      sortState.current.columnName = colName;
      sortState.current.order = "asc";
    }
    const hostApiQueryParamsCopy = cloneDeep(hostApiQueryParams);
    hostApiQueryParamsCopy.sortBy = get(sortByColIdToKeyMapping, colName, "");
    hostApiQueryParamsCopy.sortOrder = sortState.current.order;
    const queryParams = getQueryParameters(selectedFilters);
    const computedQueryParams = computeParameters(queryParams);
    if (computedQueryParams) {
      hostApiQueryParamsCopy.RequestInfo.query = `page_size=${itemsPerPage}&from=${
        (currentPage - 1) * itemsPerPage
      }&${computedQueryParams}`;
    }
    setHostApiQueryParams(hostApiQueryParamsCopy);
  };

  const getHeader = (headerString: string, columnId: string) => {
    return (
      <Button
        variant="transparent"
        className="d-flex m-0 p-0 border-0"
        onClick={() => handleSortClick(columnId)}
      >
        <div className="me-1 text-muted">{headerString}</div>
        <div>{getSortIcon(columnId)}</div>
      </Button>
    );
  };

  const getAdvanceOperationDisabled = (component: any) => {
    const serviceKey = get(
      serviceNameToModelKeyMap,
      get(component, "serviceName", "")
    );
    return get(serviceModels, `${serviceKey}.serviceState`) !== "STARTED";
  };

  const getHostActionsMenuThirdLevel = (component: any, selection: string) => {
    let result: any[] = [];
    const advanceSlaveComponentOperationDisabled =
      getAdvanceOperationDisabled(component);
    const hostsNames = selectedHosts;

    //For Hosts Items
    if (component === "hosts") {
      // SERVICE.START_STOP authorization check for component operations
      if (canStartStopServices) {
        result = result.concat([
          {
            label: "Start All Components",
            onClick: () =>
              bulkOperationConfirm(
                {
                  action: "STARTED",
                  actionToCheck: "INSTALLED",
                  message: "Start All Components",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
          {
            label: "Stop All Components",
            onClick: () =>
              bulkOperationConfirm(
                {
                  action: "INSTALLED",
                  actionToCheck: "STARTED",
                  message: "Stop All Components",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
          {
            label: "Restart All Components",
            onClick: () =>
              bulkOperationConfirm(
                {
                  action: "RESTART",
                  message: "Restart All Components",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
          {
            label: "Reinstall Failed Components",
            onClick: () => {
              bulkOperationConfirm(
                {
                  action: "REINSTALL",
                  message: "Reinstall Failed Components",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              );
            },
          },
          {
            label: "Refresh All Configs",
            onClick: () =>
              bulkOperationConfirm(
                {
                  action: "CONFIGURE",
                  message: "Refresh All Configs",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
        ]);
      }

      // HOST.TOGGLE_MAINTENANCE authorization check for maintenance operations
      if (canToggleHostMaintenance) {
        result = result.concat([
          {
            label: "Turn On Maintenance Mode",
            onClick: () =>
              bulkOperationConfirm(
                {
                  state: "ON",
                  action: "PASSIVE_STATE",
                  message: "Turn On Maintenance Mode for host",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
          {
            label: "Turn Off Maintenance Mode",
            onClick: () =>
              bulkOperationConfirm(
                {
                  state: "OFF",
                  action: "PASSIVE_STATE",
                  message: "Turn Off Maintenance Mode for host",
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
        ]);
      }

      // Ember exposes Set Rack whenever the outer Host Actions permission gate is open.
      result.push({
        label: "Set Rack",
        onClick: () =>
          bulkOperationConfirm(
            {
              action: "SET_RACK_INFO",
              message: "Set Rack",
              callback: setCurrentHostModels,
            },
            hostsNames,
            selection,
            clusterName,
            stackVersionList,
            serviceComponentInfo,
            serviceModels,
            getKDCSessionState,
            selectedFilters
          ),
      });

      // HOST.ADD_DELETE_HOSTS authorization check for delete host operation
      if (canAddDeleteHosts) {
        result = result.concat([
          {
            label: "Delete Hosts",
            onClick: () =>
              bulkOperationConfirm(
                {
                  action: "DELETE",
                  message: "Delete Hosts",
                  clusterComponents: get(clusterComponents, "items", []),
                  hostModels: currentHostModels,
                },
                hostsNames,
                selection,
                clusterName,
                stackVersionList,
                serviceComponentInfo,
                serviceModels,
                getKDCSessionState,
                selectedFilters
              ),
          },
        ]);
      }

      return result;
    }

    //For Slave Items - SERVICE.START_STOP authorization check
    if (canStartStopServices) {
      result = result.concat([
        {
          label: "Start",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: ComponentStatus.STARTED,
                message: "Start",
                componentName: getComponentName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
        },
        {
          label: "Stop",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: ComponentStatus.STOPPED,
                message: "Stop",
                componentName: getComponentName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
        },
        {
          label: "Restart",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: "RESTART",
                message: "Restart",
                componentName: getComponentName(component),
                componentDisplayName: getComponentDisplayName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
        },
      ]);
    }

    // HOST.ADD_DELETE_COMPONENTS authorization check
    if (canAddDeleteComponents) {
      result = result.concat([
        {
          label: "Add",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: "ADD",
                message: "Add",
                componentName: getComponentName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
        },
        {
          label: "Delete",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: "DELETE",
                message: "Delete",
                minToInstall: minToInstall(component),
                installedCount: get(
                  getClusterComponentsCount(clusterComponents),
                  getComponentName(component),
                  0
                ),
                componentName: getComponentName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
          // Classic deliberately omits component deletion only for All Hosts.
          isVisible: isBulkComponentDeleteVisible(selection),
        },
      ]);
    }

    // SERVICE.DECOMMISSION_RECOMMISSION authorization check
    if (
      canDecommissionRecommission &&
      get(component, "decommissionAllowed", false)
    ) {
      result = result.concat([
        {
          label: "Decommission",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: "DECOMMISSION",
                message: "Decommission",
                componentName: get(
                  component,
                  "bulkCommandsMasterComponentName"
                ),
                realComponentName: getComponentName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
          isDisabled: advanceSlaveComponentOperationDisabled,
        },
        {
          label: "Recommission",
          onClick: () =>
            bulkOperationConfirm(
              {
                action: "DECOMMISSION_OFF",
                message: "Recommission",
                componentName: get(
                  component,
                  "bulkCommandsMasterComponentName"
                ),
                realComponentName: getComponentName(component),
                serviceName: get(component, "serviceName", ""),
                componentNameFormatted: get(
                  component,
                  "bulkCommandsDisplayName",
                  ""
                ),
              },
              hostsNames,
              selection,
              clusterName,
              stackVersionList,
              serviceComponentInfo,
              serviceModels,
              getKDCSessionState,
              selectedFilters
            ),
          isDisabled: advanceSlaveComponentOperationDisabled,
        },
      ]);
    }

    return result;
  };

  const getHostActionsMenuSecondLevel = (selection: string) => {
    let secondLevelMenu = [
      {
        label: "Hosts",
        submenu: getHostActionsMenuThirdLevel("hosts", selection),
      },
    ];

    let components: any = {};
    const installedServices = get(clusterComponents, "items", [])
      .filter((component: any) => get(component, "host_components", []).length > 0)
      .map((component: any) => get(component, "ServiceComponentInfo.service_name", ""));

    const allComponents = getAllComponents(serviceComponentInfo);
    allComponents.forEach((component: any) => {
      const componentName = get(component, "HostRoles.component_name");
      const serviceName = get(component, "HostRoles.service_name", "");
      if (
        !components[componentName] &&
        installedServices.includes(serviceName) &&
        get(component, "HostRoles.has_bulk_commands_definition", false)
      ) {
        components[componentName] = {
          label: get(component, "HostRoles.bulk_commands_display_name", ""),
          serviceName: serviceName,
          componentName: componentName,
          masterComponentName: get(
            component,
            "HostRoles.bulk_commands_master_component_name",
            ""
          ),
          componentNameFormatted: get(
            component,
            "HostRoles.bulk_commands_display_name",
            ""
          ),
          submenu: getHostActionsMenuThirdLevel(
            getPopulatedHostComponentObject(component),
            selection
          ),
        };
      }
    });

    Object.keys(components).forEach((componentName) => {
      secondLevelMenu.push(components[componentName]);
    });
    return secondLevelMenu;
  };

  const getSelectedAndFilteredHostsCount = (key: string) => {
    if (key === "selected") {
      return selectedHosts.length;
    } else if (key === "filtered") {
      return totalItems;
    }
    return allHostCount;
  };

  const hostActionsOtherThanAdd = ["selected", "filtered", "all"];

  const hostActionsMenu = useMemo(() => {
    const submenu = [];

    // Add Host - Requires HOST.ADD_DELETE_HOSTS authorization
    if (canAddDeleteHosts) {
      submenu.push({
        label: (
          <Link
            to={"/main/host/add/step1"}
            className="text-reset text-decoration-none"
          >
            <FontAwesomeIcon icon={faPlus} className="me-1" />
            {translate("hosts.host.add")}
          </Link>
        ),
      });
    }

    // Add other host actions only if user has any relevant permissions
    const hasAnyHostPermissions =
      canStartStopServices ||
      canToggleHostMaintenance ||
      canAddDeleteHosts ||
      canAddDeleteComponents ||
      canDecommissionRecommission;

    if (hasAnyHostPermissions) {
      submenu.push(
        ...hostActionsOtherThanAdd.map((key) => {
          return {
            label: (
              <span>
                {startCase(key)} Hosts ({getSelectedAndFilteredHostsCount(key)})
              </span>
            ),
            submenu: getHostActionsMenuSecondLevel(key),
            isDisabled: getSelectedAndFilteredHostsCount(key) === 0,
          };
        })
      );
    }

    return {
      label: "ACTIONS",
      submenu: submenu,
    };
  }, [
    JSON.stringify(currentHostModels),
    JSON.stringify(stackVersionList),
    JSON.stringify(selectedHosts),
    JSON.stringify(selectedFilters),
    JSON.stringify(clusterComponents),
    JSON.stringify(serviceComponentInfo),
    clusterName,
    allHostCount,
    totalItems,
    canStartStopServices,
    canToggleHostMaintenance,
    canAddDeleteHosts,
    canAddDeleteComponents,
    canDecommissionRecommission,
  ]);

  const columnsInHostsList: any[] = [
    {
      header: (
        <Form.Check
          type="checkbox"
          id="select-all-hosts-header"
          className="custom-checkbox"
          checked={isAllSelected()}
          onChange={handleSelectAll}
        />
      ),
      id: "selectAll",
      width: "1%",
      cell: (info: any) => {
        const hostName = get(info, "row.original.hostName");
        const checkboxId = `host-checkbox-${hostName}`;
        return (
          <Form.Check
            type="checkbox"
            id={checkboxId}
            className="custom-checkbox"
            checked={selectedHosts.includes(hostName)}
            onChange={() => handleSelect(get(info, "row.original"))}
          />
        );
      },
    },
    {
      header: getHeader(translate("common.name") as string, "hostname"),
      id: "hostname",
      cell: (info: any) => {
        return getHostNameColumn(get(info, "row.original"));
      },
    },
    {
      header: getHeader(translate("common.ipAddress") as string, "ip"),
      accessorKey: "ip",
      id: "ip",
    },
    {
      header: getHeader(translate("common.rack") as string, "rack"),
      accessorKey: "rack",
      id: "rack",
    },
    {
      header: getHeader(translate("common.cores") as string, "cores"),
      id: "cores",
      cell: (info: any) => {
        return get(info, "row.original").coresFormatted();
      },
    },
    {
      header: getHeader(translate("common.ram") as string, "ram"),
      id: "ram",
      cell: (info: any) => {
        return bytesToSize(
          get(info, "row.original.memory"),
          2,
          "parseFloat",
          1024
        );
      },
    },
    {
      header: getHeader(translate("common.diskUsage") as string, "disk"),
      id: "disk",
      cell: (info: any) => {
        const hostData = get(info, "row.original");
        const diskUsageValue = hostData?.diskUsage ? hostData.diskUsage() : 0;
        return <ProgressBar variant="info" now={diskUsageValue} />;
      },
    },
    {
      header: getHeader(translate("common.loadAvg") as string, "load"),
      accessorKey: "loadOne",
      id: "load",
    },
    {
      header: <div className="text-muted">{translate("common.versions")}</div>,
      id: "version",
      cell: (info: any) => {
        return getVersionsColumn(get(info, "row.original"));
      },
    },
    {
      header: (
        <div className="text-muted">{translate("common.components")}</div>
      ),
      id: "component",
      cell: (info: any) => {
        return getComponentsColumn(get(info, "row.original"));
      },
    },
  ];

  return (
    <div>
      {showModal ? (
        <Modal
          modalTitle={get(modalProps.current, "title", "")}
          modalBody={get(modalProps.current, "body", "")}
          isOpen={showModal}
          onClose={() => setShowModal(false)}
          successCallback={() => setShowModal(false)}
          options={{
            buttonSize: "sm",
            cancelableViaIcon: true,
            cancelableViaBtn: false,
            okButtonVariant: "primary",
          }}
        />
      ) : null}
      <div className="d-flex justify-content-center pt-4 mx-5">
        <Card className="w-100 rounded-0">
          <div className="d-flex justify-content-between p-2 mb-2">
            <h2>{translate("common.hosts")}</h2>
            <div className="d-flex">
              <DefaultButton
                className="me-2"
                onClick={() => setShowFilters(!showFilters)}
                data-testid="filter-users-btn"
              >
                <FontAwesomeIcon icon={faFilter} />
              </DefaultButton>
              {/* Only show Actions dropdown if user has required permissions and upgrade is not in progress*/}
              {canShowHostActions && (
                <NestedDropdown
                  menu={hostActionsMenu}
                  dropDirection="start"
                />
              )}
            </div>
          </div>
          {hostData.error || clusterLoadError ? (
            <Alert variant="danger" className="m-3">
              {hostData.error || clusterLoadError}{" "}
              <Button
                size="sm"
                variant="outline-danger"
                onClick={() => {
                  if (hostData.error) {
                    hostData.retry();
                  }
                  if (clusterLoadError) {
                    setClusterRetryCount((value) => value + 1);
                  }
                }}
              >
                Retry
              </Button>
            </Alert>
          ) : loading || hostData.isLoading ? (
            <Spinner />
          ) : (
            <div>
              <HostComboSearch
                showFilters={showFilters}
                allHostModels={currentHostModels}
                clusterComponents={clusterComponents}
                searchCallback={setSelectedFilters}
                selectedFilters={selectedFilters}
                setSelectedFilters={setSelectedFilters}
                onResetFilters={clearFilters}
              />
              {paginationLoading ? (
                <Spinner />
              ) : currentHostModels.length === 0 ? (
                <div className="text-muted p-3">
                  {translate("hosts.table.noHosts")}
                </div>
              ) : (
                <Table data={currentHostModels} columns={columnsInHostsList} />
              )}
              {selectedHosts.length > 0 && (
                <div className="ps-2">
                  {selectedHosts.length}{" "}
                  {selectedHosts.length === 1
                    ? translate("hosts.filters.selectedHostInfo")
                    : translate("hosts.filters.selectedHostsInfo")}{" "}
                  -{" "}
                  <span
                    className="custom-link"
                    onClick={() => {
                      setSelectedHosts([]);
                    }}
                  >
                    {translate("hosts.filters.clearSelection")}
                  </span>
                </div>
              )}
              <Paginator
                currentPage={currentPage}
                maxPage={maxPage}
                changePage={(newPage: number) => changePage(newPage)}
                itemsPerPage={itemsPerPage}
                setItemsPerPage={setItemsPerPage}
                totalItems={totalItems}
              />
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
