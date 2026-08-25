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
import { AppContext } from "../store/context";
import { HostsApi } from "../api/hostsApi";
import useHostComponents from "../screens/ClusterWizard/hooks/useHostComponents";
import Spinner from "./Spinner";
import {
  cloneDeep,
  filter,
  find,
  flatten,
  forEach,
  get,
  isEmpty,
  map,
  merge,
  reject,
  set,
  some,
  sortBy,
  uniq,
} from "lodash";
import { groupPropertyValues } from "../Utils/dataUtils";
import Select from "react-select";
import AssignMastersApi from "../api/assignMastersApi";
import { isShownOnAddServiceAssignMasterPage, role } from "../Utils/Utility";
import { displayOrder } from "../screens/ClusterWizard/constants";
import {
  isMasterAddableInstallerWizard,
  maxToInstall,
} from "../screens/Hosts/utils";
import { Badge, Button, Col, Row, Stack, Alert } from "react-bootstrap";
import { misc } from "../Utils/misc";
import classNames from "classnames";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMinus, faPlus } from "@fortawesome/free-solid-svg-icons";
import { responseErrorMessage } from "../Utils/httpError";

export type AssignMastersLoadState = {
  status: "loading" | "ready" | "error";
  error?: string;
};

type AssignMastersAddableProps = {
  services: string[];
  mastersToCreate?: string[];
  mastersToShow?: string[];
  mastersToMove?: string[];
  mastersToAdd?: string[];
  showCurrentPrefix?: string[];
  showAdditionalPrefix?: string[];
  mastersAddableInHA?: string[];
  showInstalledMastersFirst?: boolean;
  dispatch: any;
  isInstallFlow?: boolean;
  showJournalNode?: boolean;
  wizardName?: string;
  servicesData?: any;
  maximumMasterCount?: number;
  minimumAdditionalMasterCount?: Record<string, number>;
  validateAssignments?: boolean;
  onAssignmentValidationChange?: (
    isValid: boolean,
    errors: string[],
  ) => void;
  onLoadStateChange?: (state: AssignMastersLoadState) => void;
};

type MasterAssignment = {
  component_name?: string;
  component?: string;
  display_name?: string;
  selectedHost?: string;
  movedHost?: string;
  isInstalled?: boolean;
};

type AssignmentHost = {
  Hosts?: {
    cpu_count?: number;
    host_name?: string;
    maintenance_state?: string;
    total_mem?: number;
  };
};

type RecommendationDocument = {
  blueprint: { host_groups: any[] };
  blueprint_cluster_binding: { host_groups: any[] };
};

// eslint-disable-next-line react-refresh/only-export-components
export function sortAssignmentHosts<T extends AssignmentHost>(hosts: T[]): T[] {
  const resourceValue = (host: T, path: string) => {
    const value = Number(get(host, path));
    return Number.isFinite(value) ? value : Number.NEGATIVE_INFINITY;
  };

  return [...hosts].sort((left, right) => {
    const memoryDifference =
      resourceValue(right, "Hosts.total_mem") -
      resourceValue(left, "Hosts.total_mem");
    if (memoryDifference) return memoryDifference;

    const cpuDifference =
      resourceValue(right, "Hosts.cpu_count") -
      resourceValue(left, "Hosts.cpu_count");
    if (cpuDifference) return cpuDifference;

    return String(get(left, "Hosts.host_name", "")).localeCompare(
      String(get(right, "Hosts.host_name", "")),
    );
  });
}

// eslint-disable-next-line react-refresh/only-export-components
export function recommendationDocumentFromResponse(
  response: unknown,
): RecommendationDocument {
  const recommendations: any = get(
    response as any,
    "resources[0].recommendations",
  );
  const blueprintGroups = recommendations?.blueprint?.host_groups;
  const bindingGroups =
    recommendations?.blueprint_cluster_binding?.host_groups;
  const bindingGroupNames = new Set(
    Array.isArray(bindingGroups)
      ? bindingGroups.map((group: any) => group?.name)
      : [],
  );
  if (
    !recommendations ||
    !Array.isArray(blueprintGroups) ||
    !blueprintGroups.length ||
    !Array.isArray(bindingGroups) ||
    !bindingGroups.length ||
    blueprintGroups.some(
      (group: any) =>
        !group?.name ||
        !bindingGroupNames.has(group.name) ||
        !Array.isArray(group.components) ||
        group.components.some((component: any) => !component?.name),
    ) ||
    bindingGroups.some(
      (group: any) =>
        !group?.name ||
        !Array.isArray(group.hosts) ||
        group.hosts.some((host: any) => !host?.fqdn),
    ) ||
    !blueprintGroups.some((group: any) => group.components.length) ||
    !bindingGroups.some((group: any) => group.hosts.length)
  ) {
    throw new Error(
      "Stack Advisor returned an incomplete host assignment recommendation.",
    );
  }
  return recommendations;
}

// eslint-disable-next-line react-refresh/only-export-components
export function buildAssignmentRecommendationRequest({
  hosts,
  services,
  recommendations,
}: {
  hosts: string[];
  services: string[];
  recommendations: RecommendationDocument;
}) {
  return {
    recommend: "host_groups",
    hosts,
    services,
    recommendations,
  };
}

function assignmentLoadErrorMessage(error: unknown, fallback: string): string {
  const responseMessage = responseErrorMessage(error, fallback);
  if (
    responseMessage === fallback &&
    error instanceof Error &&
    error.message
  ) {
    return error.message;
  }
  return responseMessage;
}

// eslint-disable-next-line react-refresh/only-export-components
export function validateMasterAssignments(
  assignments: MasterAssignment[] = [],
  hosts: AssignmentHost[] = [],
  minimumAdditionalMasterCount: Record<string, number> = {},
) {
  if (!assignments.length) {
    return ["At least one master component must be assigned to a host."];
  }

  const errors = new Set<string>();
  const assignmentsByComponentAndHost = new Map<string, number>();
  const hostsByName = new Map(
    hosts.map((host) => [get(host, "Hosts.host_name", ""), host]),
  );

  Object.entries(minimumAdditionalMasterCount).forEach(
    ([componentName, minimumCount]) => {
      const additionalCount = assignments.filter(
        (assignment) =>
          (assignment.component_name || assignment.component) === componentName &&
          !assignment.isInstalled,
      ).length;
      if (additionalCount < minimumCount) {
        const displayName = role(componentName, false) || componentName;
        errors.add(
          `Assign at least ${minimumCount} additional ${displayName} instance${minimumCount === 1 ? "" : "s"}.`,
        );
      }
    },
  );

  assignments.forEach((assignment) => {
    const componentName =
      assignment.component_name || assignment.component || "Master component";
    const displayName =
      assignment.display_name || role(componentName, false) || componentName;
    const hostName = (assignment.movedHost || assignment.selectedHost || "").trim();

    if (!hostName) {
      errors.add(`${displayName} must be assigned to a host.`);
      return;
    }

    const assignmentKey = `${componentName}\u0000${hostName}`;
    const assignmentCount = assignmentsByComponentAndHost.get(assignmentKey) || 0;
    assignmentsByComponentAndHost.set(assignmentKey, assignmentCount + 1);
    if (assignmentCount > 0) {
      errors.add(`${displayName} cannot be assigned to ${hostName} more than once.`);
    }

    const selectedHost = hostsByName.get(hostName);
    if (!selectedHost) {
      errors.add(`${displayName} host ${hostName} is no longer available.`);
      return;
    }
    const maintenanceState = get(selectedHost, "Hosts.maintenance_state", "");
    if (maintenanceState && maintenanceState !== "OFF") {
      errors.add(`${displayName} host ${hostName} is in maintenance mode.`);
    }
  });

  return [...errors];
}

export function MasterAssignmentValidationAlert({
  errors,
}: {
  errors: string[];
}) {
  if (!errors.length) return null;
  return (
    <Alert variant="danger" role="alert">
      <div className="fw-bold mb-1">Resolve the host assignment errors:</div>
      {errors.map((error) => (
        <div key={error}>{error}</div>
      ))}
    </Alert>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function canRemoveAdditionalMaster(
  assignments: MasterAssignment[],
  master: MasterAssignment,
  minimumAdditionalMasterCount: Record<string, number> = {},
) {
  if (master.isInstalled) return false;
  const componentName = master.component_name || master.component || "";
  const minimumCount = minimumAdditionalMasterCount[componentName] || 0;
  if (!minimumCount) return true;
  const additionalCount = assignments.filter(
    (assignment) =>
      (assignment.component_name || assignment.component) === componentName &&
      !assignment.isInstalled,
  ).length;
  return additionalCount > minimumCount;
}

function AssignMastersAddable({
  services,
  mastersToCreate = [],
  mastersToShow = [],
  mastersToMove = [],
  mastersToAdd = [],
  showCurrentPrefix = [],
  showAdditionalPrefix = [],
  mastersAddableInHA = [],
  showInstalledMastersFirst = false,
  isInstallFlow = false,
  dispatch,
  showJournalNode = false,
  wizardName = "",
  servicesData = {},
  maximumMasterCount,
  minimumAdditionalMasterCount = {},
  validateAssignments = false,
  onAssignmentValidationChange,
  onLoadStateChange,
}: AssignMastersAddableProps) {
  const [hosts, setHosts] = useState([]);
  const {
    clusterName,
    cluster: { versionNum, stack },
  } = useContext(AppContext);
  const [recommendations, setRecommendations] = useState<any>([]);
  const [recommededHostsForComponents, setRecommendedHostsForComponents] =
    useState<any>({});
  const { hostComponents: serviceHostComponents, serviceComponents } =
    useHostComponents(services, showJournalNode);
  const [selectedServicesMasters, setSelectedServicesMasters] = useState<any>(
    []
  );
  const [inferredMasterHosts, setInferredMasterHosts] = useState([]);
  const [inferredSlaveHosts, setInferredSlaveHosts] = useState([]);
  const [addableMasters, setAddableMasters] = useState<any>([]);
  const [servicesMasters, setServicesMasters] = useState<any>([]);
  const [masterHostsMapping, setMasterHostsMapping] = useState<any>([]);
  const [inferredMasterComponents, setInferredMasterComponents] = useState<any>(
    []
  );
  const [loadError, setLoadError] = useState("");
  const assignmentValidationErrors = validateAssignments
    ? validateMasterAssignments(
        addableMasters,
        hosts,
        minimumAdditionalMasterCount,
      )
    : [];
  const assignmentValidationKey = assignmentValidationErrors.join("\n");
  const assignmentValidationCallbackRef = useRef(
    onAssignmentValidationChange,
  );
  assignmentValidationCallbackRef.current = onAssignmentValidationChange;
  const loadStateCallbackRef = useRef(onLoadStateChange);
  loadStateCallbackRef.current = onLoadStateChange;

  useEffect(() => {
    if (!validateAssignments) return;
    assignmentValidationCallbackRef.current?.(
      assignmentValidationKey.length === 0,
      assignmentValidationKey ? assignmentValidationKey.split("\n") : [],
    );
  }, [validateAssignments, assignmentValidationKey]);

  const loadStatus = loadError
    ? "error"
    : masterHostsMapping.length
      ? "ready"
      : "loading";

  useEffect(() => {
    loadStateCallbackRef.current?.({
      status: loadStatus,
      ...(loadError ? { error: loadError } : {}),
    });
  }, [loadError, loadStatus]);

  // Function to get warning message for hosts not running master services
  function getHostWarningMessage() {
    if (!hosts.length || !inferredMasterHosts.length) return null;
    
    // Get all hosts in the cluster
    const allHostNames = hosts.map((host: any) => host.Hosts.host_name);
    
    // Get hosts that are currently running master services from inferredMasterHosts
    // These are the hosts that actually have installed master components
    const currentHostsRunningMasters = uniq(
      inferredMasterHosts
        .filter((masterHost: any) => masterHost.isInstalled)
        .map((masterHost: any) => masterHost.hostName)
    );
    
    // Get hosts that will be running master services after the move operation
    // This includes both current hosts and new target hosts from moves
    const futureHostsRunningMasters = uniq([
      ...currentHostsRunningMasters,
      // Add target hosts from move operations
      ...addableMasters
        .filter((master: any) => master.movedHost) // Only moving components
        .map((master: any) => master.movedHost),
      // Add any other selected hosts that will have masters
      ...addableMasters
        .filter((master: any) => !master.isInstalled) // New components
        .map((master: any) => master.selectedHost)
    ]);
    
    // Find hosts not running master services (considering future state)
    const hostsNotRunningMasters = allHostNames.filter(
      (hostName: string) => !futureHostsRunningMasters.includes(hostName)
    );

    if (hostsNotRunningMasters.length > 0) {
      return `${hostsNotRunningMasters.length} hosts not running master services`;
    }
    
    return null;
  }


  // Function to enhance inferred master hosts with isMoving information
  function getEnhancedInferredMasterHosts() {
    return inferredMasterHosts.map((masterHost: any) => {
      // Find matching component in selectedServicesMasters that has movedHost and isMoving
      const matchingServiceMaster = selectedServicesMasters.find((serviceMaster: any) => 
        serviceMaster.component_name === masterHost.component && 
        serviceMaster.hostName === masterHost.hostName &&
        serviceMaster.movedHost &&
        serviceMaster.isMoving === true
      );
      
      // Find matching component in addableMasters that has movedHost and isMoving
      const matchingAddableMaster = addableMasters.find((addableMaster: any) => 
        addableMaster.component_name === masterHost.component && 
        addableMaster.hostName === masterHost.hostName &&
        addableMaster.movedHost &&
        addableMaster.isMoving === true
      );

      const matchingComponent = matchingServiceMaster || matchingAddableMaster;

      return {
        ...masterHost,
        isMoving: !!matchingComponent,
        movedHost: matchingComponent ? matchingComponent.movedHost : undefined
      };
    });
  }
  const initiallyLoaded = useRef(false);
  const selectedServicesMastersRef = useRef<any>([]);
  const servicesMastersRef = useRef<any>([]);
  const [, setComponentToRebalance] = useState("");
  const [, setLastChangedComponent] = useState("");
  const [, setRebalanceHostCounter] = useState(0);
  
  useEffect(() => {
    const flattenedHostComponents = flatten(
      map(serviceHostComponents, "host_components")
    );
    const flattenedHostRoles = flatten(
      map(flattenedHostComponents, "HostRoles")
    );
    const groupedHosts = groupPropertyValues(flattenedHostRoles, "host_name");
    const hostComponentsRoles = flatten(
      map(serviceHostComponents, "host_components")
    );
    const hostComponents = flatten(map(hostComponentsRoles, "HostRoles"));
    const masterComponentHostsToDispatch = flatten([
      ...getEnhancedInferredMasterHosts(),
      filter(inferredMasterComponents, ["isInstalled", false]),
    ]);
    
    dispatch({
      masterHostsMapping,
      hosts: groupedHosts,
      masterHosts: inferredMasterHosts,
      enhancedMasterHosts: getEnhancedInferredMasterHosts(),
      slaveHosts: inferredSlaveHosts,
      mastersData: masterHostsMapping,
      masterComponentHosts: masterComponentHostsToDispatch,
      slaveComponentHosts: null,
      hostComponents,
    });
  }, [masterHostsMapping]);

  function createComponentInstallationObject(
    fullComponent: any,
    hostName: string,
    savedComponent?: any
  ) {
    fullComponent.component_name =
      fullComponent.componentName || fullComponent.component_name;
    fullComponent.component =
      fullComponent.componentName || fullComponent.component_name;
    const componentName: any = fullComponent.component_name,
      resultingHostName = savedComponent ? savedComponent.hostName : hostName;
    let componentObj: any = {};
    componentObj.component_name = componentName;
    componentObj.component = componentName;
    componentObj.display_name = role(fullComponent.component_name, false);
    componentObj.serviceId = fullComponent.service_name;
    componentObj.isServiceCoHost = !mastersToMove.includes(
      componentName as never
    );
    componentObj.selectedHost = resultingHostName;
    componentObj.hostName = resultingHostName;
    componentObj.isInstalled = savedComponent
      ? savedComponent.isInstalled ||
        !mastersToCreate.includes(fullComponent.component_name as never)
      : false;
    //Check for namenode federation
    // if (this.get('content.controllerName') === 'reassignMasterController' && componentName === 'NAMENODE' && App.get('hasNameNodeFederation')) {
    //   componentObj.nameSpace = App.HostComponent.find(`${componentName}_${resultingHostName}`).get('haNameSpace');
    // }
    return componentObj;
  }

  function getHostForMaster(master: any, allMasters: any[]) {
    const masterHostList: any = [];
    allMasters.forEach((component: any) => {
      if (component.component_name === master) {
        masterHostList.push(component.selectedHost);
      }
    });
    const recommendedHostsForMaster =
      recommededHostsForComponents[master] || [];
    for (let k = 0; k < recommendedHostsForMaster.length; k++) {
      if (!masterHostList.includes(recommendedHostsForMaster[k])) {
        return recommendedHostsForMaster[k];
      }
    }
    let usedHosts = map(
      filter(allMasters, ["component_name", master]),
      "selectedHost"
    );
    var allHosts = hosts;
    for (var i = 0; i < allHosts.length; i++) {
      if (!usedHosts.includes(get(allHosts[i], "Hosts.host_name"))) {
        return get(allHosts[i], "Hosts.host_name");
      }
    }

    return false;
  }

  function masterHostMapping() {
    let mapping: any = [],
      mappingObject,
      mappedHosts,
      hostObj: any;
    const allHosts = [
      ...map(selectedServicesMasters, "selectedHost"),
      ...map(selectedServicesMasters, "movedHost"),
    ];
    mappedHosts = uniq(allHosts);
    mappedHosts.forEach(function (item) {
      hostObj = find(hosts, ["Hosts.host_name", item]);
      // User may input invalid host name (this is handled in hostname checker). Here we just skip it
      if (!hostObj) return;
      let masterServices = filter(
        selectedServicesMasters,
        (masterService: any) => {
          return masterService.movedHost
            ? masterService.movedHost === item
            : masterService.selectedHost === item;
        }
      );
      let masterServicesToDisplay: any = [];
      uniq(map(masterServices, "display_name")).forEach(function (n) {
        masterServicesToDisplay.push(find(masterServices, ["display_name", n]));
      });
      mappingObject = {
        host_name: item,
        hostInfo: hostObj.host_info,
        masterServices: masterServices,
        masterServicesToDisplay: masterServicesToDisplay,
      };

      mapping.push(mappingObject);
    });
    setMasterHostsMapping(mapping);
  }

  function sortMasterComponents(masters: any[]) {
    return [].concat(
      filter(masters, ["isInstalled", true]) as any,
      filter(masters, ["isInstalled", false]) as any
    );
  }

  function servicesMastersToShow() {
    let result: any = [];
    if (!mastersToShow.length) {
      result = servicesMasters;
    } else {
      mastersToShow.forEach(function (master) {
        result = result.concat(
          filter(servicesMasters, ["component_name", master])
        );
      });
    }
    if (showInstalledMastersFirst) {
      result = sortMasterComponents(result);
    }

    const installedServices = map(
      filter(
        getFlattenedcomponents(),
        (component: any) => servicesData[component.service_name]?.installed
      ),
      "service_name"
    );

    for (const item of result) {
      const stackComponent =
        find(getFlattenedcomponents(), [
          "component_name",
          item.component_name,
        ]) || {};
      const isMasterWithMultipleInstances = get(
        stackComponent,
        "isMasterWithMultipleInstances",
        false
      );
      const showControl =
        !installedServices.includes(get(stackComponent, "service_name", "")) ||
        mastersAddableInHA.includes(item.component_name);

      // PRIMARY RULE: In move/reassign scenarios, NEVER show add/remove controls for ANY component
      // This is the most important rule and overrides all other logic
      if (mastersToMove.length > 0) {
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      }
      // In reassign/move scenarios, installed components should NEVER show add/remove controls
      // This is a secondary rule for additional safety
      else if (item.isInstalled && mastersToMove.includes(item.component_name)) {
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      }
      // Special handling for NameNode Federation - never show controls for additional NameNodes
      // This matches the ui version behavior where NAMENODE controls are not shown in federation wizard
      else if (
        item.component_name === "NAMENODE" &&
        showAdditionalPrefix.includes("NAMENODE") &&
        !item.isInstalled
      ) {
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      }
      // Special handling for NameNode in HA mode - never show controls
      else if (
        item.component_name === "NAMENODE" &&
        mastersToShow.includes("NAMENODE") &&
        mastersToShow.includes("JOURNALNODE")
      ) {
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      }
      // Special handling for ResourceManager in HA mode - never show controls (matching Ember)
      else if (
        item.component_name === "RESOURCEMANAGER" &&
        mastersToShow.includes("RESOURCEMANAGER") &&
        mastersToShow.length === 1
      ) {
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      } else if (item.isInstalled) {
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      } else if (isMasterWithMultipleInstances && showControl) {
        // For non-installed multiple instance masters, show add/remove based on count
        const mastersCount = filter(result, [
          "component_name",
          item.component_name,
        ]).length;
        set(item, "showRemoveControl", mastersCount > 1);
        set(
          item,
          "showAddControl",
          mastersCount < getMaxNumberOfMasters(item.component_name)
        );
      } else {
        // Default to no controls for components not in mastersAddableInHA
        set(item, "showRemoveControl", false);
        set(item, "showAddControl", false);
      }
      set(item, "isMoving", mastersToMove.includes(item.component));
    }

    // Special handling for JournalNodes - apply the Ember logic exactly
    // This applies to both NameNode HA wizard and Manage JournalNodes wizard
    if (mastersToShow.includes("JOURNALNODE") && mastersToMove.length === 0) {
      const JOURNALNODES_COUNT_MINIMUM = 3;
      const journalNodes = filter(result, ["component_name", "JOURNALNODE"]);
      const maxNumMasters = getMaxNumberOfMasters("JOURNALNODE");
      
      // TLHASD-1164: Implement exact Ember logic from showHideJournalNodesAddRemoveControl
      // Remove control is only shown when there are MORE than minimum required JournalNodes
      const showRemoveControl = journalNodes.length > JOURNALNODES_COUNT_MINIMUM;
      const showAddControl = journalNodes.length < maxNumMasters;

      // Set all JournalNodes to not show add control initially
      journalNodes.forEach(function (item: any) {
        set(item, "showAddControl", false);
        set(item, "showRemoveControl", showRemoveControl);
      });

      // Only the last JournalNode should show add control (matching Ember: jns.set('lastObject.showAddControl', showAddControl))
      if (journalNodes.length > 0) {
        set(
          journalNodes[journalNodes.length - 1],
          "showAddControl",
          showAddControl
        );
      }
    }

    setAddableMasters(result);
  }

  function getServiceByMaster(master: any) {
    const allComponents = flatten(
      map(
        flatten(map(serviceComponents, "components")),
        "StackServiceComponents"
      )
    );
    const mappedService = find(allComponents, ["component_name", master]);
    return get(mappedService, "service_name", "");
  }

  function addNewMasters(masterComponents: any) {
    if (!mastersToAdd.length && isInstallFlow) {
      const notMasters = ["MYSQL_SERVER", "HIVE_SERVER_INTERACTIVE"];
      const allMasterComponents = map(
        filter(flatten(map(serviceComponents, "components")), [
          "StackServiceComponents.is_master",
          true,
        ]),
        "StackServiceComponents.component_name"
      );
      const installedMasters: string[] = map(
        serviceHostComponents,
        "ServiceComponentInfo.component_name"
      );
      let missingMasters = filter(
        allMasterComponents,
        function (masterName: string) {
          return (
            !installedMasters.includes(masterName) &&
            !notMasters.includes(masterName)
          );
        }
      );
      let updatedMissingMasters = [];
      if (wizardName === "addService") {
        for (const master of missingMasters) {
          const serviceOfMaster = getServiceByMaster(master);
          if (servicesData[serviceOfMaster]?.installed === false) {
            updatedMissingMasters.push(master);
          }
        }
      }
      mastersToAdd =
        wizardName === "addService" ? updatedMissingMasters : missingMasters;
    }
    mastersToAdd.forEach(function (masterName: string) {
      var toBeAddedNumber = mastersToAdd.filter(function (name) {
          return name === masterName;
        }).length,
        alreadyAddedNumber = reject(
          filter(masterComponents, ["component_name", masterName]),
          "isInstalled"
        ).length;
      if (toBeAddedNumber > alreadyAddedNumber) {
        var hostName = getHostForMaster(masterName, masterComponents),
          serviceName = getServiceByMaster(masterName);
        masterComponents.push(
          createComponentInstallationObject(
            {
              componentName: masterName,
              serviceName: serviceName,
            },
            hostName
          )
        );
      }
    });
    return masterComponents;
  }

  function sortComponentsByServiceName(components: any) {
    const componentsOrderForService: any = {
      HAWQ: ["HAWQMASTER", "HAWQSTANDBY"],
    };
    let indexForUnordered = Math.max(displayOrder.length, components.length);
    return components.sort(function (a: any, b: any) {
      if (
        a.serviceId === b.serviceId &&
        a.serviceId in componentsOrderForService
      )
        return (
          componentsOrderForService[a.serviceId].indexOf(a.component_name) -
          componentsOrderForService[b.serviceId].indexOf(b.component_name)
        );
      var aValue =
        displayOrder.indexOf(a.serviceId) != -1
          ? displayOrder.indexOf(a.serviceId)
          : indexForUnordered;
      var bValue =
        displayOrder.indexOf(b.serviceId) != -1
          ? displayOrder.indexOf(b.serviceId)
          : indexForUnordered;
      return aValue - bValue;
    });
  }

  async function renderComponents(masterComponents: any) {
    let result: any = [];
    //@ts-ignore
    let serviceComponentId, previousComponentName;
    addNewMasters(masterComponents);
    //@ts-ignore
    const allComponents = flatten(
      map(
        flatten(map(serviceComponents, "components")),
        "StackServiceComponents"
      )
    );
    masterComponents.forEach(function (item: any) {
      const allComponents = flatten(
        map(
          flatten(map(serviceComponents, "components")),
          "StackServiceComponents"
        )
      );
      const masterComponent: any = find(allComponents, [
        "component_name",
        item.component_name,
      ]);
      setInferredMasterComponents(masterComponents);
      if (masterComponent) {
        let componentObj = { ...item };
        if (item.nameSpace) {
          componentObj = {
            allMasters: result,
            namespace: (function () {
              // Todo: Logic to get namespace
            })(),
          };
        }
        let showRemoveControl;
        if (masterComponent.isMasterWithMultipleInstances) {
          showRemoveControl =
            filter(masterComponents, function (masterComponent: any) {
              masterComponent.component_name === item.component_name &&
                !masterComponent.isInstalled;
            }).length > 1;
          previousComponentName = item.component_name;
          set(
            componentObj,
            "serviceComponentId",
            filter(result, ["component_name", item.component_name]).length + 1
          );
          if (componentObj.isInstalled) {
            set(componentObj, "showRemoveControl", false);
          } else {
            set(componentObj, "showRemoveControl", showRemoveControl);
          }
        }
        set(componentObj, "isHostNameValid", true);
        // Handle HA state display for ResourceManager (matching Ember logic)
        if (
          item.component_name === "RESOURCEMANAGER" &&
          mastersToShow.includes("RESOURCEMANAGER")
        ) {
          const flattenedHostComponents = flatten(
            map(serviceHostComponents, "host_components")
          );
          const flattenedHostRoles = flatten(
            map(flattenedHostComponents, "HostRoles")
          );
          const hostComponent = find(
            flattenedHostRoles,
            (hc: any) =>
              hc.component_name === "RESOURCEMANAGER" &&
              hc.host_name === item.selectedHost
          );
          const haState = get(hostComponent, "ha_state", "");

          if (haState === "ACTIVE") {
            set(componentObj, "showCurrentPrefix", false);
            set(componentObj, "showAdditionalPrefix", false);
            set(componentObj, "display_name", "Active ResourceManager");
          } else if (
            haState === "STANDBY" ||
            (!item.isInstalled && haState !== "ACTIVE")
          ) {
            set(componentObj, "showCurrentPrefix", false);
            set(componentObj, "showAdditionalPrefix", false);
            set(
              componentObj,
              "display_name",
              item.isInstalled
                ? "Standby ResourceManager"
                : "Additional ResourceManager"
            );
          } else {
            // Fallback to original logic
            set(
              componentObj,
              "showCurrentPrefix",
              showCurrentPrefix.includes(item.component_name as never) &&
                item.isInstalled
            );
            set(
              componentObj,
              "showAdditionalPrefix",
              showAdditionalPrefix.includes(item.component_name as never) &&
                !item.isInstalled
            );
          }
        } else {
          set(
            componentObj,
            "showCurrentPrefix",
            showCurrentPrefix.includes(item.component_name as never) &&
              item.isInstalled
          );
          set(
            componentObj,
            "showAdditionalPrefix",
            showAdditionalPrefix.includes(item.component_name as never) &&
              !item.isInstalled
          );
        }
        if (mastersToMove.includes(item.component_name as never)) {
          set(componentObj, "isInstalled", false);
        }
        result.push(componentObj);
      }
    });
    result = sortComponentsByServiceName(result);
    setSelectedServicesMasters(result);
    setServicesMasters(result);
    setInferredMasterComponents(result);
    selectedServicesMastersRef.current = result;
    servicesMastersRef.current = result;

    // JournalNode controls will be handled in servicesMastersToShow()
  }

  function createComponentInstallationObjects() {
    let stackMasterComponentsMap: any = {},
      resultComponents: any = [],
      multipleComponentHasBeenAdded: any = {},
      hostGroupsMap: any = {};

    const allComponents = flatten(
      map(
        flatten(map(serviceComponents, "components")),
        "StackServiceComponents"
      )
    );
    const journalNode: any = find(allComponents, [
      "component_name",
      "JOURNALNODE",
    ]);
    if (journalNode && showJournalNode) {
      journalNode.is_master = true;
    }
    const masterComponents = map(
      filter(allComponents, ["is_master", true]),
      "component_name"
    );
    const slaveComponents = map(
      filter(allComponents, ["component_category", "SLAVE"]),
      "component_name"
    );
    serviceHostComponents.forEach(function (serviceComponent: any) {
      serviceComponent.host_components.map((hostComponent: any) => {
        set(
          hostComponent,
          "HostRoles.service_name",
          get(serviceComponent, "ServiceComponentInfo.service_name")
        );
      });
    });
    const flattenedHostComponents = flatten(
      map(serviceHostComponents, "host_components")
    );
    const flattenedHostRoles = flatten(
      map(flattenedHostComponents, "HostRoles")
    );
    let masterHosts: any = [];
    let slaveHosts: any = [];
    for (const masterComponent of masterComponents) {
      const hostsWithMasterComponent = filter(flattenedHostRoles, [
        "component_name",
        masterComponent,
      ]);

      const transformedMasterHosts = map(
        hostsWithMasterComponent,
        function (masterComponentHost: any) {
          return {
            component: masterComponent,
            hostName: masterComponentHost.host_name,
            isInstalled: true,
            serviceId: get(masterComponentHost, "service_name", ""),
            display_name: get(masterComponentHost, "display_name", ""),
          };
        }
      );
      masterHosts = [...masterHosts, ...transformedMasterHosts];
      setInferredMasterHosts(masterHosts);
    }
    for (const slaveComponent of slaveComponents) {
      const hostsWithSlaveComponent = filter(flattenedHostRoles, [
        "component_name",
        slaveComponent,
      ]);

      const transformedSlaveHosts = map(
        hostsWithSlaveComponent,
        function (slaveComponentHost: any) {
          return {
            component: slaveComponent,
            hostName: slaveComponentHost.host_name,
            isInstalled: true,
            serviceId: get(slaveComponentHost, "service_name", ""),
            display_name: get(slaveComponentHost, "display_name", ""),
          };
        }
      );
      slaveHosts = [...slaveHosts, ...transformedSlaveHosts];
      setInferredSlaveHosts(masterHosts);
    }
    // const allStackServices = flatten(
    //   map(serviceHostComponents, "host_components.[0].HostRoles")
    // );
    const allStackServices = getFlattenedcomponents();
    const isHFDSInstalled = services.includes("HDFS");
    const servicesToAdd: any = [];
    const isHAEnabled =
      isHFDSInstalled &&
      !some(allStackServices, ["component_name", "SECONDARY_NAMENODE"]);
    allStackServices.forEach((component: any) => {
      // let component=componentHere;
      // const matchingComponent=find(getFlattenedcomponents(), ["component_name", componentHere?.component_name]);
      // if(matchingComponent){
      //   component=matchingComponent
      // }
      if (component) {
        const isMasterCreateOnConfig = mastersToCreate.includes(
          get(component, "component_name", "") as never
        );
        //@ts-ignore
        // const isMasterComponent=getFlattenedcomponents().find((c:any)=>c.component_name===component.component_name)?.is_master ;
        if (
          isShownOnAddServiceAssignMasterPage(
            component.component_name,
            component.is_master,
            stack,
            isHAEnabled
          ) ||
          mastersToShow.includes(component.component_name as never) ||
          isMasterCreateOnConfig
        ) {
          stackMasterComponentsMap[get(component, "component_name", "")] =
            component;
        }
      }
    });
    recommendations.blueprint_cluster_binding.host_groups.forEach(function (
      group: any
    ) {
      hostGroupsMap[group.name] = group;
    });
    recommendations.blueprint.host_groups.forEach(function (host_group: any) {
      let currentHosts = hostGroupsMap[host_group.name]
        ? hostGroupsMap[host_group.name].hosts
        : [];
      currentHosts.forEach(function (host: any) {
        host_group.components.forEach(function (component: any) {
          component.component_name = component.name;
          let willBeDisplayed = true;
          let stackMasterComponent =
            stackMasterComponentsMap[component.component_name];
          if (stackMasterComponent) {
            let isMasterCreateOnConfig = mastersToCreate.includes(
              component.component_name as never
            );
            // If service is already installed and not being added as a new service then render on UI only those master components
            // that have already installed hostComponents.
            // NOTE: On upgrade there might be a prior installed service with non-installed newly introduced serviceComponent
            if (
              !servicesToAdd.includes(
                get(stackMasterComponent, "serviceName")
              ) &&
              !isMasterCreateOnConfig
            ) {
              willBeDisplayed = some(masterHosts, [
                "component",
                component.component_name,
              ]);
            }

            if (willBeDisplayed) {
              let savedComponents = filter(masterHosts, [
                "component",
                component.component_name,
              ]);
              const multipleComponents = map(
                filter(allStackServices, [
                  "isMasterWithMultipleInstances",
                  true,
                ]),
                "component_name"
              );
              if (
                multipleComponents.includes(
                  component.component_name as never
                ) &&
                savedComponents.length > 0
              ) {
                if (!multipleComponentHasBeenAdded[component.component_name]) {
                  multipleComponentHasBeenAdded[component.component_name] =
                    true;

                  savedComponents.forEach(function (saved) {
                    resultComponents.push(
                      createComponentInstallationObject(
                        stackMasterComponent,
                        host.fqdn.toLowerCase(),
                        saved
                      )
                    );
                  });
                }
              } else {
                var savedComponent = find(masterHosts, [
                  "component",
                  component.name,
                ]);
                resultComponents.push(
                  createComponentInstallationObject(
                    stackMasterComponent,
                    host.fqdn.toLowerCase(),
                    savedComponent
                  )
                );
              }
            }
          }
        });
      });
    });

    return resultComponents;
  }
  function formatRecommendComponents(components: any) {
    const res: any = [];
    if (!components) return [];
    components.forEach(function (component: any) {
      var componentName = get(component, "component_name", "");
      if (get(component, "hosts.length")) {
        get(component, "hosts").forEach(function (hostName: string) {
          res.push({
            componentName: componentName,
            hostName: hostName,
          });
        });
      }
    });
    return res;
  }
  function getComponentsBlueprint(components: any) {
    const uniqueHosts = uniq([
      ...map(components, "hostName"),
      ...map(hosts, "Hosts.host_name"),
    ]);
    const mappedComponents = groupPropertyValues(components, "hostName");
    const res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] },
    };
    uniqueHosts.forEach(function (host, i) {
      var group_name = "host-group-" + (i + 1);

      res.blueprint.host_groups.push({
        name: group_name,
        components: mappedComponents[host]
          ? mappedComponents[host].map(function (c: any) {
              return { name: get(c, "componentName") };
            })
          : [],
      } as never);

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: [{ fqdn: host }],
      } as never);
    });
    return res;
  }
  function getRecommendationRequestData(opts: any) {
    return buildAssignmentRecommendationRequest({
      hosts: opts.hosts,
      services,
      recommendations:
        opts.recommendations || getComponentsBlueprint(opts.components),
    });
  }
  async function getRecommendedHosts() {
    const payloadObject = {
      services: [],
      hosts: map(hosts, "Hosts.host_name"),
      components: [],
      recommendations: null,
    };
    payloadObject.components = formatRecommendComponents(serviceHostComponents);
    const payload = getRecommendationRequestData(payloadObject);
    setLoadError("");
    try {
      const data = await AssignMastersApi.postRecommendations(
        payload,
        stack,
        versionNum
      );
      const firstRecommendations = recommendationDocumentFromResponse(data);
      const payloadWithComponentsObject = {
        services: [],
        hosts: map(hosts, "Hosts.host_name"),
        components: [],
        recommendations: firstRecommendations,
      };
      const payloadWithComponents = getRecommendationRequestData(
        payloadWithComponentsObject
      );
      const dataWithComponents = await AssignMastersApi.postRecommendations(
        payloadWithComponents,
        stack,
        versionNum
      );
      const recommendationsServerWithComponents =
        recommendationDocumentFromResponse(
          dataWithComponents,
        );
      setRecommendations(recommendationsServerWithComponents);
      const recommendedHostsForComponent: any = {};
      const hostsForHostGroup: any = {};
      recommendationsServerWithComponents.blueprint_cluster_binding.host_groups.forEach(
        function (hostGroup: any) {
          hostsForHostGroup[hostGroup.name] = map(hostGroup.hosts, "fqdn");
        }
      );

      recommendationsServerWithComponents.blueprint.host_groups.forEach(
        function (hostGroup: any) {
          var components = map(hostGroup.components, "name");
          components.forEach(function (componentName: string) {
            var hostList = recommendedHostsForComponent[componentName] || [];
            var hostNames = hostsForHostGroup[hostGroup.name] || [];
            hostList = [...hostList, ...hostNames];
            recommendedHostsForComponent[componentName] = hostList;
          });
        }
      );
      setRecommendedHostsForComponents(recommendedHostsForComponent);
    } catch (error) {
      setLoadError(
        assignmentLoadErrorMessage(
          error,
          "Ambari could not load host assignment recommendations.",
        ),
      );
    }
  }
  async function fetchHosts() {
    setLoadError("");
    try {
      const allHosts = await HostsApi.getHostComponentsDetails(
        clusterName,
        "fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true"
      );
      if (!Array.isArray(allHosts?.items) || !allHosts.items.length) {
        throw new Error(
          "Ambari returned no hosts for master component assignment.",
        );
      }
      if (
        allHosts.items.some(
          (host: any) => !String(get(host, "Hosts.host_name", "")).trim(),
        )
      ) {
        throw new Error("Ambari returned malformed host assignment data.");
      }
      allHosts.items.forEach((host: any) => {
        host.host_info = {
          cpu_count: host.Hosts.cpu_count,
          disk_info: host.Hosts.disk_info,
          total_mem: host.Hosts.total_mem,
        };
      });
      setHosts(sortAssignmentHosts(allHosts.items) as never[]);
    } catch (error) {
      setLoadError(
        assignmentLoadErrorMessage(
          error,
          "Ambari could not load hosts for master component assignment.",
        ),
      );
    }
  }

  function retryLoad() {
    setLoadError("");
    setRecommendations([]);
    setRecommendedHostsForComponents({});
    if (!hosts.length) {
      void fetchHosts();
      return;
    }
    void getRecommendedHosts();
  }

  function getFlattenedcomponents() {
    return flatten(
      map(
        flatten(map(serviceComponents, "components")),
        "StackServiceComponents"
      )
    );
  }

  function getMaxNumberOfMasters(componentName: string) {
    const maxByCardinality = maxToInstall(
      find(getFlattenedcomponents(), ["component_name", componentName])!
    );
    const hostsNumber = hosts.length;
    return Math.min(
      maxByCardinality,
      hostsNumber,
      maximumMasterCount ?? Number.POSITIVE_INFINITY,
    );
  }

  function updateComponent(componentName: string) {
    const selectedServicesMastersCopy = cloneDeep(
      selectedServicesMastersRef.current
    );
    const component: any = filter(selectedServicesMastersCopy, [
      "component_name",
      componentName,
    ])?.at(-1);
    if (!component) {
      return;
    }

    // PRIMARY RULE: In move/reassign scenarios, NEVER show add/remove controls
    if (mastersToMove.length > 0) {
      set(component, "showAddControl", false);
      set(component, "showRemoveControl", false);
      setSelectedServicesMasters(selectedServicesMastersCopy);
      setServicesMasters(selectedServicesMastersCopy);
      return;
    }

    // Get component stack service installation status
    const stackComponent =
      find(getFlattenedcomponents(), ["component_name", componentName]) || {};
    const isServiceInstalled =
      servicesData[get(stackComponent, "service_name", "")]?.installed;

    // Show control if service is not installed or component is addable in HA
    let showControl =
      !isServiceInstalled ||
      mastersAddableInHA.includes(componentName as never);
    if (showControl) {
      let mastersLength = filter(selectedServicesMastersRef.current, [
        "component_name",
        componentName,
      ]).length;
      if (mastersLength < getMaxNumberOfMasters(componentName)) {
        set(component, "showAddControl", true);
      } else {
        if (component.isInstalled) {
          set(component, "showRemoveControl", false);
        } else {
          set(component, "showRemoveControl", mastersLength != 1);
        }
      }
    }
    setSelectedServicesMasters(selectedServicesMastersCopy);
    setServicesMasters(selectedServicesMastersCopy);
    const addableMastersCopy = cloneDeep(addableMasters);
    const currentComponentInAddableMasters = filter(
      addableMastersCopy,
      function (addableMaster: any) {
        return addableMaster.component_name === componentName;
      }
    );
    forEach(currentComponentInAddableMasters, function (addableMaster: any) {
      const matchingComponentFromServiceCopy = find(
        selectedServicesMastersCopy,
        function (serviceMaster: any) {
          return (
            serviceMaster.component_name === addableMaster.component_name &&
            serviceMaster.selectedHost === addableMaster.selectedHost
          );
        }
      );
      if (matchingComponentFromServiceCopy) {
        merge(addableMaster, matchingComponentFromServiceCopy);
      }
    });
    setAddableMasters(addableMastersCopy);
    selectedServicesMastersRef.current = selectedServicesMastersCopy;
    servicesMastersRef.current = selectedServicesMastersCopy;
  }

  function thereIsNoMasters() {
    return !filter(selectedServicesMasters, ["isInstalled", false]).length;
  }

  async function loadStepCallback(components: any) {
    const allComponents = flatten(
      map(
        flatten(map(serviceComponents, "components")),
        "StackServiceComponents"
      )
    );
    renderComponents(components);
    const addableMasterInstallerWizard = (function () {
      return map(
        filter(allComponents, (component: any) => {
          return isMasterAddableInstallerWizard(component);
        }),
        "component_name"
      );
    })();
    const addableComponents = uniq([
      ...addableMasterInstallerWizard,
      ...mastersAddableInHA,
    ]);
    addableComponents.forEach((componentName: any) => {
      updateComponent(componentName);
    });

    // Special handling for JOURNALNODE in HA mode (matching Ember implementation)
    // In Ember, JOURNALNODE is handled by the showHideJournalNodesAddRemoveControl function
    // even when it's not in mastersAddableInHA
    if (mastersToShow.includes("JOURNALNODE")) {
      updateComponent("JOURNALNODE");
    }

    if (thereIsNoMasters() && !mastersToCreate.length) {
      //Enable Next
    }
  }

  useEffect(() => {
    fetchHosts();
  }, []);

  useEffect(() => {
    if (serviceHostComponents.length && hosts.length) {
      getRecommendedHosts();
    }
  }, [serviceHostComponents.length, hosts.length]);

  useEffect(() => {
    if (servicesMasters.length && !initiallyLoaded.current) {
      initiallyLoaded.current = true;
      servicesMastersToShow();
    }
  }, [servicesMasters.length]);

  useEffect(() => {
    if (addableMasters.length) {
      masterHostMapping();
    }
  }, [addableMasters, servicesMasters]);

  useEffect(() => {
    if (!isEmpty(recommededHostsForComponents) && !isEmpty(recommendations)) {
      loadStepCallback(createComponentInstallationObjects());
    }
  }, [!isEmpty(recommededHostsForComponents), !isEmpty(recommendations)]);

  if (loadError) {
    return (
      <Alert
        variant="danger"
        role="alert"
        className="d-flex justify-content-between align-items-center"
      >
        <span>{loadError}</span>
        <Button size="sm" variant="outline-danger" onClick={retryLoad}>
          Retry
        </Button>
      </Alert>
    );
  }

  if (
    !hosts.length ||
    !serviceHostComponents.length ||
    !masterHostsMapping.length
  ) {
    return <Spinner />;
  }

  const hostsOptions = hosts.map((host: any) => ({
    label: host.Hosts.host_name,
    value: host.Hosts.host_name,
  }));

  function changeHostNameForMaster(
    componentIndex: number,
    selectedHost: { label: string; value: string },
    isMoving = false,
    serviceComponentId?: string
  ) {
    const addableMastersCopy = cloneDeep(addableMasters);
    if (isMoving) {
      addableMastersCopy[componentIndex].movedHost = selectedHost.value;
      addableMastersCopy[componentIndex].isMoving=true;
    } else {
      addableMastersCopy[componentIndex].selectedHost = selectedHost.value;
      addableMastersCopy[componentIndex].hostName = selectedHost.value;
    }
    //change inferred masters as well
    const inferrredCopy = cloneDeep(inferredMasterComponents);
    const inferredComponent = find(inferrredCopy, function (comp) {
      return (
        comp.component_name ===
          addableMastersCopy[componentIndex].component_name &&
        comp.serviceComponentId === serviceComponentId &&
        comp.isInstalled === false
      );
    });
    if (isMoving) {
      inferredComponent.movedHost = selectedHost.value;
      inferredComponent.isMoving=true;
    } else {
      inferredComponent.selectedHost = selectedHost.value;
      inferredComponent.hostName = selectedHost.value;
    }
    setInferredMasterComponents(inferrredCopy);
    setSelectedServicesMasters(inferrredCopy);
    setAddableMasters(addableMastersCopy);
  }

  function getHostOptionsFor(componentIndex: number) {
    const selectedMaster = addableMasters[componentIndex];
    const allMasters = cloneDeep(addableMasters);
    const matchingMasters = allMasters.filter(
      (master: any) => master.component_name === selectedMaster.component_name
    );

    // For reassign scenarios, we need to consider both selectedHost and movedHost
    // to prevent duplicate host assignments
    const usedHosts: string[] = [];
    matchingMasters.forEach((master: any, index: number) => {
      // Skip the current component being edited to allow it to select any host
      if (index === componentIndex) {
        return;
      }
      
      // For other components of the same type, add their hosts to exclusion list
      // Add the current selected host
      if (master.selectedHost) {
        usedHosts.push(master.selectedHost);
      }
      // For moving components, also add the moved host if it exists
      if (master.movedHost) {
        usedHosts.push(master.movedHost);
      }
    });

    // Remove duplicates
    const uniqueUsedHosts = uniq(usedHosts);

    // For move scenarios, always allow the original host (selectedHost) to be available
    // so users can revert back to the current host
    const availableHosts = reject(hostsOptions, (hostOption: any) =>
      uniqueUsedHosts.includes(hostOption.value)
    );
    
    return availableHosts;
  }
  function addComponent(componentName: string) {
    const maxNumMasters = getMaxNumberOfMasters(componentName);
    const currentMasters = sortBy(
      filter(addableMasters, ["component_name", componentName])
    );
    let newMaster = null;
    let masterHosts = null;
    let suggestedHost = null;
    let i = 0;
    let lastMaster = null;
    if (!currentMasters.length) {
      return false;
    }
    const servicesMastersCopy = cloneDeep(servicesMastersRef.current);
    const selectedServicesMastersCopy = cloneDeep(
      selectedServicesMastersRef.current
    );
    if (currentMasters.length < maxNumMasters) {
      set(currentMasters.at(-1), "showAddControl", false);
      // set(currentMasters.at(-1), "showRemoveControl", true);
      newMaster = {};
      lastMaster = currentMasters.at(-1);
      set(newMaster, "display_name", get(lastMaster, "display_name", ""));
      set(newMaster, "component_name", get(lastMaster, "component_name", ""));
      set(newMaster, "component", get(lastMaster, "component_name", ""));
      set(newMaster, "selectedHost", get(lastMaster, "selectedHost", ""));
      set(newMaster, "hostName", get(lastMaster, "selectedHost", ""));
      set(newMaster, "serviceId", get(lastMaster, "serviceId", ""));
      set(newMaster, "isInstalled", false);
      set(
        newMaster,
        "showAdditionalPrefix",
        showAdditionalPrefix.includes(componentName)
      );
      if (currentMasters.length === maxNumMasters - 1) {
        set(newMaster, "showAddControl", false);
      } else {
        set(newMaster, "showAddControl", true);
      }
      set(newMaster, "showRemoveControl", true);
      masterHosts = uniq(map(currentMasters, "selectedHost"));
      for (i = 0; i < hosts.length; i++) {
        if (!masterHosts.includes(get(hosts[i], "Hosts.host_name"))) {
          suggestedHost = get(hosts[i], "Hosts.host_name");
          break;
        }
      }
      set(newMaster, "selectedHost", suggestedHost);
      set(newMaster, "hostName", suggestedHost);
      if (!get(currentMasters?.at(-1), "serviceComponentId", undefined)) {
        set(currentMasters?.at(-1), "serviceComponentId", 0);
      }
      set(
        newMaster,
        "serviceComponentId",
        get(currentMasters?.at(-1), "serviceComponentId") + 1
      );
      // Add to all the necessary arrays
      servicesMastersCopy.push(newMaster);
      selectedServicesMastersCopy.push(newMaster);
      currentMasters.push(newMaster);

      // Update all state arrays with the new component
      const addableMastersCopy = cloneDeep(addableMasters);
      const otherMasters = addableMastersCopy.filter(
        (m: any) => m.component_name !== componentName
      );
      const updatedAddableMasters = [...otherMasters, ...currentMasters];

      // Update all refs and state
      servicesMastersRef.current = servicesMastersCopy;
      selectedServicesMastersRef.current = selectedServicesMastersCopy;

      setSelectedServicesMasters(selectedServicesMastersCopy);
      setServicesMasters(servicesMastersCopy);
      setAddableMasters(updatedAddableMasters);
      setInferredMasterComponents([...inferredMasterComponents, newMaster]);

      // TLHASD-1164: Recompute JournalNode controls after adding (like Ember showHideJournalNodesAddRemoveControl)
      if (componentName === "JOURNALNODE" && mastersToShow.includes("JOURNALNODE") && mastersToMove.length === 0) {
        const JOURNALNODES_COUNT_MINIMUM = 3;
        const allJournalNodes = [...otherMasters, ...currentMasters].filter(
          (m: any) => m.component_name === "JOURNALNODE"
        );
        const maxNumMasters = getMaxNumberOfMasters("JOURNALNODE");
        const showRemoveControl = allJournalNodes.length > JOURNALNODES_COUNT_MINIMUM;
        const showAddControl = allJournalNodes.length < maxNumMasters;

        // Recompute controls for all JournalNodes
        allJournalNodes.forEach(function (item: any) {
          set(item, "showAddControl", false);
          set(item, "showRemoveControl", showRemoveControl);
        });

        // Only the last JournalNode should show add control
        if (allJournalNodes.length > 0) {
          set(allJournalNodes[allJournalNodes.length - 1], "showAddControl", showAddControl);
        }

        setAddableMasters([...otherMasters, ...allJournalNodes]);
      }

      setComponentToRebalance(componentName);
      setLastChangedComponent(componentName);
      setRebalanceHostCounter((hC) => hC + 1);

      //Toggle Property
      return true;
    }
    return false;
  }
  function removeComponent(componentName: string, serviceComponentId: number) {
    let currentMasters = sortBy(
      filter(addableMasters, ["component_name", componentName]),
      "serviceComponentId"
    );
    if (currentMasters.length <= 1) {
      return false;
    }

    // const removeIndex = currentMasters.findIndex(
    //   (master: any) =>
    //     master.serviceComponentId === serviceComponentId &&
    //     master.component_name === componentName
    // );
    //Remove item at this index
    const updatedMasters = currentMasters.filter((master: any) => {
      return !(
        master.serviceComponentId === serviceComponentId &&
        master.component_name === componentName
      );
    });
    selectedServicesMastersRef.current =
      selectedServicesMastersRef.current.filter((master: any) => {
        return !(
          master.serviceComponentId === serviceComponentId &&
          master.component_name === componentName
        );
      });
    servicesMastersRef.current = servicesMastersRef.current.filter(
      (master: any) => {
        return !(
          master.serviceComponentId === serviceComponentId &&
          master.component_name === componentName
        );
      }
    );
    currentMasters = updatedMasters;
    if (currentMasters.length < getMaxNumberOfMasters(componentName)) {
      set(currentMasters.at(-1), "showAddControl", true);
    }
    if (filter(currentMasters, ["isInstalled", false]).length === 1) {
      set(currentMasters.at(-1), "showRemoveControl", false);
    }

    // Update addableMasters by replacing only the components of current type
    const otherMasters = addableMasters.filter(
      (m: any) => m.component_name !== componentName
    );

    // TLHASD-1164: Recompute JournalNode controls after removal (like Ember showHideJournalNodesAddRemoveControl)
    if (componentName === "JOURNALNODE" && mastersToShow.includes("JOURNALNODE") && mastersToMove.length === 0) {
      const JOURNALNODES_COUNT_MINIMUM = 3;
      const allJournalNodes = [...otherMasters, ...currentMasters].filter(
        (m: any) => m.component_name === "JOURNALNODE"
      );
      const maxNumMasters = getMaxNumberOfMasters("JOURNALNODE");
      const showRemoveControl = allJournalNodes.length > JOURNALNODES_COUNT_MINIMUM;
      const showAddControl = allJournalNodes.length < maxNumMasters;

      // Recompute controls for all JournalNodes
      allJournalNodes.forEach(function (item: any) {
        set(item, "showAddControl", false);
        set(item, "showRemoveControl", showRemoveControl);
      });

      // Only the last JournalNode should show add control
      if (allJournalNodes.length > 0) {
        set(allJournalNodes[allJournalNodes.length - 1], "showAddControl", showAddControl);
      }

      setAddableMasters([...otherMasters, ...allJournalNodes]);
    } else {
      setAddableMasters([...otherMasters, ...currentMasters]);
    }

    setComponentToRebalance(componentName);
    setLastChangedComponent(componentName);
    setRebalanceHostCounter((hC) => hC + 1);
    setSelectedServicesMasters(selectedServicesMastersRef.current);
    setServicesMasters(servicesMastersRef.current);
    // Also remove from inferredMasterComponents and inferredMasterHosts
    setInferredMasterComponents(
      inferredMasterComponents.filter((master: any) => {
        return !(
          master.serviceComponentId === serviceComponentId &&
          master.component_name === componentName
        );
      })
    );
    
    if (componentName === "JOURNALNODE") {
      const componentToRemove = find(addableMasters, (master: any) => {
        return master.serviceComponentId === serviceComponentId &&
               master.component_name === componentName;
      });
      
      if (componentToRemove && componentToRemove.isInstalled) {
        setInferredMasterHosts(
          inferredMasterHosts.filter((master: any) => {
            return !(
              master.hostName === componentToRemove.hostName &&
              master.component === componentName
            );
          })
        );
      }
    }

    //Toggle Property
    return true;
  }

  // Get warning messages
  const hostWarningMessage = getHostWarningMessage();

  return (
    <>
      {validateAssignments && (
        <MasterAssignmentValidationAlert errors={assignmentValidationErrors} />
      )}
      <Row>
        <Col md={2}></Col>
        <Col md={6}>
          {addableMasters?.map((addableMaster: any, index: any) => {
            return (
              <>
                <Row
                  direction="horizontal"
                  className="justify-content-start align-items-center mt-3"
                >
                  <Col md={4}>
                    <strong className="bolder">
                      {addableMaster.showCurrentPrefix
                        ? "Current "
                        : addableMaster.showAdditionalPrefix
                        ? "Additional "
                        : null}
                      {addableMaster.display_name}
                    </strong>
                  </Col>
                  <Col>
                    <Row className="align-items-center">
                      <Col md={8}>
                        {addableMaster.isMoving ? (
                          addableMaster.selectedHost
                        ) : (
                          <Select
                            className="w-100"
                            isDisabled={addableMaster.isInstalled}
                            options={getHostOptionsFor(index)}
                            onChange={(value: any) => {
                              changeHostNameForMaster(
                                index,
                                value,
                                false,
                                addableMaster?.serviceComponentId
                              );
                            }}
                            value={{
                              label: addableMaster.selectedHost,
                              value: addableMaster.selectedHost,
                            }}
                          />
                        )}
                      </Col>
                      <Col md={1}>
                        {addableMaster.showAddControl ? (
                          <Button
                            variant="success"
                            size="sm"
                            onClick={() => {
                              addComponent(addableMaster.component_name);
                            }}
                          >
                            <FontAwesomeIcon icon={faPlus} />
                          </Button>
                        ) : null}
                      </Col>
                      <Col md={1}>
                        {addableMaster.showRemoveControl &&
                        canRemoveAdditionalMaster(
                          addableMasters,
                          addableMaster,
                          minimumAdditionalMasterCount,
                        ) ? (
                          <Button
                            variant="success"
                            size="sm"
                            onClick={() => {
                              removeComponent(
                                addableMaster.component_name,
                                addableMaster.serviceComponentId
                              );
                            }}
                          >
                            <FontAwesomeIcon icon={faMinus} />
                          </Button>
                        ) : null}
                      </Col>
                    </Row>
                  </Col>
                </Row>
                {addableMaster.isMoving ? (
                  <Row
                    direction="horizontal"
                    className="justify-content-start align-items-center mt-3"
                  >
                    <Col md={4}>
                      <strong className="bolder">
                        {addableMaster.display_name}
                      </strong>
                    </Col>
                    <Col>
                      <Row className="align-items-center">
                        <Col md={8}>
                          <Select
                            className="w-100"
                            isDisabled={addableMaster.isInstalled}
                            options={getHostOptionsFor(index)}
                            onChange={(value: any) => {
                              changeHostNameForMaster(
                                index,
                                value,
                                true,
                                addableMaster?.serviceComponentId
                              );
                            }}
                            value={{
                              label:
                                addableMaster.movedHost ||
                                addableMaster.selectedHost,
                              value:
                                addableMaster.movedHost ||
                                addableMaster.selectedHost,
                            }}
                          />
                        </Col>
                      </Row>
                    </Col>
                  </Row>
                ) : null}
              </>
            );
          })}
        </Col>
        <Col md={1}></Col>
        <Col md={3}>
          <Stack>
            {masterHostsMapping.map((mapping: any) => {
              return (
                <div className="p-3 border mb-3 bg-very-light">
                  <Stack>
                    <div className="text-muted">
                      {mapping.host_name} (
                      {misc.formatBandwidth(mapping.hostInfo.total_mem, "GB")},{" "}
                      {mapping.hostInfo.cpu_count} cores)
                    </div>
                  </Stack>
                  <Stack direction="horizontal" className="mt-2 flex-wrap">
                    {mapping.masterServicesToDisplay.map((service: any) => {
                      return (
                        <Badge
                          className={classNames("mb-2 me-2 rounded-1", {
                            "bg-success": !service.isInstalled,
                            "bg-light": service.isInstalled,
                          })}
                        >
                          {service.display_name}
                        </Badge>
                      );
                    })}
                  </Stack>
                </div>
              );
            })}
            
            {/* Display warning message below host mapping */}
            {hostWarningMessage && (
              <Alert variant="warning" className="mb-3">
                {hostWarningMessage}
              </Alert>
            )}
          </Stack>
        </Col>
      </Row>
    </>
  );
}

export default AssignMastersAddable;
