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

import type { JSX } from "react";
import { normalizeName } from "../screens/Hosts/helpers";
import _, { startCase, get, has, isEmpty } from "lodash";
import { messages } from "../screens/messages";
import modalManager from "../store/ModalManager";
import {
  faCheck,
  faClock,
  faCogs,
  faExclamationCircle,
  faGears,
  faMinus,
  faPause,
  faShare
} from "@fortawesome/free-solid-svg-icons";
import {
  kerberos_ui_properties,
  UIProperty,
} from "../data/configs/kerberos_ui_properties";
import { alert_notifications } from "../data/configs/alert_notifications";
import {ranger_properties as rangerProperties} from "../data/configs/services/ranger_properties";
import {yarn_properties as yarnProperties} from "../data/configs/services/yarn_properties";
import {hbase_properties as hbaseProperties} from "../data/configs/services/hbase_properties";
import {hdfs_properties as hdfsProperties} from "../data/configs/services/hdfs_properties";
import {hive_properties as hiveProperties} from "../data/configs/services/hive_properties";
import {kerberos_properties as kerberosProperties} from "../data/configs/services/kerberos_properties";
import {mapreduce2_properties as mapReduce2Properties} from "../data/configs/services/mapreduce2_properties";
import {zookeeper_properties as zookeeperProperties} from "../data/configs/services/zookeeper_properties";
import {tez_properties as tezProperties} from "../data/configs/services/tez_properties";
import { detectUserTimezone, parseTimezones } from "./timezone";
import DOMPurify from "isomorphic-dompurify";
import parse from 'html-react-parser';
import { t } from "i18next";
import { HostsApi } from "../api/hostsApi";

export const padNumber = (num: number) => (num < 10 ? `0${num}` : num);
const components: any = {
  API: "API",
  DECOMMISSION_DATANODE: "Update Exclude File",
  DRPC: "DRPC",
  FLUME_HANDLER: "Flume",
  GLUSTERFS: "GLUSTERFS",
  HBASE: "HBase",
  HBASE_REGIONSERVER: "RegionServer",
  HCAT: "HCat Client",
  HDFS: "HDFS",
  HISTORYSERVER: "History Server",
  HIVE_SERVER: "HiveServer2",
  JCE: "JCE",
  MAPREDUCE2: "MapReduce2",
  MYSQL: "MySQL",
  REST: "REST",
  SECONDARY_NAMENODE: "SNameNode",
  STORM_REST_API: "Storm REST API Server",
  WEBHCAT: "WebHCat",
  YARN: "YARN",
  UI: "UI",
  ZKFC: "ZKFailoverController",
  ZOOKEEPER: "ZooKeeper",
  ZOOKEEPER_QUORUM_SERVICE_CHECK: "ZK Quorum Service Check",
  HAWQ: "HAWQ",
  PXF: "PXF",
};
const command: any = {
  INSTALL: "Install",
  UNINSTALL: "Uninstall",
  START: "Start",
  STOP: "Stop",
  EXECUTE: "Execute",
  ABORT: "Abort",
  UPGRADE: "Upgrade",
  RESTART: "Restart",
  SERVICE_CHECK: "Check",
  SET_KEYTAB: "Set Keytab:",
  "Excluded:": "Decommission:",
  "Included:": "Recommission:",
};

export const serviceMap: any = {
  HDFS: "HDFS",
  YARN: "YARN",
  MAPREDUCE2: "MapReduce2",
  TEZ: "Tez",
  HIVE: "Hive",
  HBASE: "HBase",
  ZOOKEEPER: "ZooKeeper",
  AMBARI_METRICS: "Ambari Metrics",
  RANGER: "Ranger",
  RANGER_KMS: "Ranger KMS",
  KERBEROS: "Kerberos",
  SPARK3: "Spark3",
  SSM: "SSM",
  TRINO: "Trino",
  TRINO_GATEWAY: "Trino Gateway",
};

export const componentsMap: any = {
  DATANODE: "DataNode",
  HDFS_CLIENT: "HDFS Client",
  JOURNALNODE: "JournalNode",
  NAMENODE: "NameNode",
  NFS_GATEWAY: "NFSGateway",
  ROUTER: "Router",
  SECONDARY_NAMENODE: "SNameNode",
  ZKFC: "ZKFailoverController",
  APP_TIMELINE_SERVER: "Timeline Service V1.5",
  NODEMANAGER: "NodeManager",
  RESOURCEMANAGER: "ResourceManager",
  YARN_CLIENT: "YARN Client",
  YARN_REGISTRY_DNS: "YARN Registry DNS",
  HISTORYSERVER: "History Server",
  MAPREDUCE2_CLIENT: "MapReduce2 Client",
  TEZ_CLIENT: "Tez Client",
  HIVE_CLIENT: "Hive Client",
  HIVE_METASTORE: "Hive Metastore",
  HIVE_SERVER: "HiveServer2",
  HIVE_SERVER_INTERACTIVE: "HiveServer2 Interactive",
  MYSQL_SERVER: "MySQL Server",
  HBASE_CLIENT: "HBase Client",
  HBASE_MASTER: "HBase Master",
  HBASE_REGIONSERVER: "RegionServer",
  OMID_TSO_SERVER: "Omid TSO Server",
  PHOENIX_QUERY_SERVER: "Phoenix Query Server",
  ZOOKEEPER_CLIENT: "ZooKeeper Client",
  ZOOKEEPER_SERVER: "ZooKeeper Server",
  METRICS_COLLECTOR: "Metrics Collector",
  METRICS_GRAFANA: "Grafana",
  METRICS_MONITOR: "Metrics Monitor",
  RANGER_ADMIN: "Ranger Admin",
  RANGER_TAGSYNC: "Ranger Tagsync",
  RANGER_USERSYNC: "Ranger Usersync",
  RANGER_KMS_SERVER: "Ranger KMS Server",
  KERBEROS_CLIENT: "Kerberos Client",
  LIVY3_SERVER: "Livy for Spark3 Server",
  SPARK3_CLIENT: "Spark3 Client",
  SPARK3_JOBHISTORYSERVER: "Spark3 History Server",
  SPARK3_THRIFTSERVER: "Spark3 Thrift Server",
  SSM_AGENT: "Smart Agent",
  SSM_SERVER: "Smart Server",
  TRINO_CLI: "Trino CLI",
  TRINO_COORDINATOR: "Trino Coordinator",
  TRINO_WORKER: "TRINO Worker",
};

export const getUserTimezone = () => {
  const timeZones = parseTimezones();
  const userTimezone = detectUserTimezone();
  return timeZones.find((tz) => tz.value === userTimezone)?.label || "";
};

export const formatDate = (date: Date) => {
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(
    date.getDate()
  )}T${padNumber(date.getHours())}:${padNumber(date.getMinutes())}`;
};

export const getCurrTimeInSec = () => {
  return Math.floor(Date.now() / 1000);
};

export const getTimeInNumber = (time: string) => {
  return new Date(time).getTime() / 1000;
};

export const Utility = {
  components: {
    API: "API",
    DECOMMISSION_DATANODE: "Update Exclude File",
    DRPC: "DRPC",
    FLUME_HANDLER: "Flume",
    GLUSTERFS: "GLUSTERFS",
    HBASE: "HBase",
    HBASE_REGIONSERVER: "RegionServer",
    HCAT: "HCat Client",
    HDFS: "HDFS",
    HISTORYSERVER: "History Server",
    HIVE_SERVER: "HiveServer2",
    JCE: "JCE",
    MAPREDUCE2: "MapReduce2",
    MYSQL: "MySQL",
    REST: "REST",
    SECONDARY_NAMENODE: "SNameNode",
    STORM_REST_API: "Storm REST API Server",
    WEBHCAT: "WebHCat",
    YARN: "YARN",
    UI: "UI",
    ZKFC: "ZKFailoverController",
    ZOOKEEPER: "ZooKeeper",
    ZOOKEEPER_QUORUM_SERVICE_CHECK: "ZK Quorum Service Check",
    HAWQ: "HAWQ",
    PXF: "PXF",
  } as Record<string, string>,

  encryptData: function (data: string) {
    let encryptedValue = data;
    if (typeof data !== "string") {
      console.error("Encryption needs string to be encrypted");
    } else {
      encryptedValue = data
        .split("")
        .map(function (char) {
          var asciiCode = char.charCodeAt(0);
          if (asciiCode >= 32 && asciiCode <= 126) {
            return String.fromCharCode(((asciiCode - 32 + 13) % 95) + 32);
          } else {
            return char;
          }
        })
        .join("");
    }
    return encryptedValue;
  },

  decryptData: function (data: string) {
    let encryptedValue = data;
    let decryptedValue = data;
    if (typeof data !== "string") {
      console.error("Decryption needs string to be encrypted");
    } else {
      decryptedValue = encryptedValue
        .split("")
        .map(function (char) {
          var asciiCode = char.charCodeAt(0);
          if (asciiCode >= 32 && asciiCode <= 126) {
            return String.fromCharCode(((asciiCode - 32 - 13 + 95) % 95) + 32);
          } else {
            return char;
          }
        })
        .join("");
    }
    return decryptedValue;
  },
  recommendationPayload: function (
    hosts: any,
    recommend: string,
    blueprint: any,
    blueprintClusterBinding: any,
    services: string[]
  ) {
    const payload = {
      recommend: recommend,
      hosts: hosts,
      services: services,
      recommendations: {
        blueprint: {
          host_groups: blueprint,
        },
        blueprint_cluster_binding: {
          host_groups: blueprintClusterBinding,
        },
      },
    };
    return payload;
  },
};

export const isRequestRunning = (status: string) => {
  return ["IN_PROGRESS", "QUEUED", "PENDING"].includes(status);
};

export const isFinished = (status: string) => {
  return ["FAILED", "TIMEDOUT", "ABORTED", "COMPLETED"].includes(status);
};

export const isFailed = (status: string) => {
  return ["FAILED", "TIMEDOUT", "ABORTED"].includes(status);
};
export const role = (role: string, isServiceRole: boolean) => {
  if (isServiceRole) {
    if (has(serviceMap, role)) {
      return serviceMap[role];
    }
  } else {
    if (has(componentsMap, role)) {
      return componentsMap[role];
    }
  }
  return normalizeName(role);
};

export function getFormattedStringFromArray(array: any, endSeparator: any) {
  var label = "";
  (endSeparator = endSeparator || "and"),
    array.forEach(function (_arrElement: any) {
      if (array.length === 1) {
        label = _arrElement;
      } else {
        if (_arrElement !== array[array.length - 1]) {
          // [clients.length - 1]
          label = label + " " + _arrElement;
          if (_arrElement !== array[array.length - 2]) {
            label = label + ",";
          }
        } else {
          label = label + " " + endSeparator + " " + _arrElement;
        }
      }
    });
  return label.trim();
}

export function commandDetail(
  command_detail: any,
  request_inputs: any,
  ops_display_name: any
) {
  let detailArr = command_detail.split(" ");
  let result = "";
  let isIncludeExcludeFiles = false;
  //if an optional operation display name has been specified in the service metainfo.xml
  if (ops_display_name != null && ops_display_name.length > 0) {
    result = result + " " + ops_display_name;
  } else {
    detailArr.forEach(function (item: any) {
      // if the item has the pattern SERVICE/COMPONENT, drop the SERVICE part
      if (item.includes("/") && !isIncludeExcludeFiles) {
        item = item.split("/")[1];
      }
      if (item === "DECOMMISSION,") {
        // ignore text 'DECOMMISSION,'( command came from 'excluded/included'), here get the component name from request_inputs
        var parsedInputs = null;
        try {
          if (request_inputs && typeof request_inputs === "string") {
            parsedInputs = JSON.parse(request_inputs);
          } else if (request_inputs && typeof request_inputs === "object") {
            parsedInputs = request_inputs;
          }
        } catch (e) {
          // If parsing fails, parsedInputs remains null
          console.warn("Failed to parse request_inputs:", request_inputs);
        }
        item = parsedInputs ? parsedInputs.slave_type || "" : "";
        isIncludeExcludeFiles = parsedInputs
          ? parsedInputs.is_add_or_delete_slave_request === "true"
          : false;
      }
      if (components[item]) {
        result = result + " " + components[item];
      } else if (command[item]) {
        result = result + " " + command[item];
      } else if (isIncludeExcludeFiles) {
        result = result + " " + item;
      } else {
        result = result + " " + role(item, false);
      }
    });
  }

  // Special handling for Decommission/Recommission - make hostname lowercase
  if (result.indexOf('Decommission:') > -1 || result.indexOf('Recommission:') > -1) {
    result = result.split(':')[0] + ': ' + result.split(':')[1].toLowerCase();
  }

  // Special case handling for various operations
  if (result === ' Nagios Update Ignore Actionexecute') {
    result = translate('common.maintenance.task') as string;
  }
  if (result.indexOf('Install Packages Actionexecute') !== -1) {
    result = translate('common.installRepo.task') as string;
  }
  if (result === ' Rebalancehdfs NameNode') {
    result = translate('services.service.actions.run.rebalanceHdfsNodes.title') as string;
  }
  if (result === " Startdemoldap Knox Gateway") {
    result = translate('services.service.actions.run.startLdapKnox.title') as string;
  }
  if (result === " Stopdemoldap Knox Gateway") {
    result = translate('services.service.actions.run.stopLdapKnox.title') as string;
  }
  if (result === ' Refreshqueues ResourceManager') {
    result = translate('services.service.actions.run.yarnRefreshQueues.title') as string;
  }
  // HAWQ custom commands on back Ops page
  if (result === ' Resync Hawq Standby HAWQ Standby Master') {
    result = translate('services.service.actions.run.resyncHawqStandby.label') as string;
  }
  if (result === ' Immediate Stop Hawq Service HAWQ Master') {
    result = translate('services.service.actions.run.immediateStopHawqService.label') as string;
  }
  if (result === ' Immediate Stop Hawq Segment HAWQ Segment') {
    result = translate('services.service.actions.run.immediateStopHawqSegment.label') as string;
  }
  if (result === ' Activate Hawq Standby HAWQ Standby Master') {
    result = translate('admin.activateHawqStandby.button.enable') as string;
  }
  if (result === ' Hawq Clear Cache HAWQ Master') {
    result = translate('services.service.actions.run.clearHawqCache.label') as string;
  }
  if (result === ' Run Hawq Check HAWQ Master') {
    result = translate('services.service.actions.run.runHawqCheck.label') as string;
  }

  return result;
}

export const pluralize = (
  count: number,
  noun: string,
  suffix = "s",
  showCount = true
) => `${showCount ? count : ""} ${noun}${count !== 1 ? suffix : ""}`;

export const getStepData = (
  state: any,
  stepName: string,
  dataKey: string,
  wizardName = "clusterCreationSteps"
) => {
  const stepData = get(state, `${wizardName}.${stepName}.data`, {});
  if (dataKey) {
    return get(stepData, dataKey, "");
  }
  return stepData;
};
const getRequestContextWithPrefix = (requestContext: string) => {
  let contextSplits = requestContext.split("."),
    parsedRequestContext,
    contextCommand = contextSplits[1],
    service = contextSplits[2];

  switch (contextCommand) {
    case "STOP":
    case "START":
      const command = startCase(contextCommand.toLowerCase());
      parsedRequestContext =
        service === "ALL_SERVICES"
          ? `${command} All Services`
          : `${command} ${service}`;
      break;
    case "ROLLING-RESTART":
      parsedRequestContext = `Rolling Restart of ${service} - batch ${contextSplits[3]} of ${contextSplits[4]}`;
      break;
  }
  return {
    requestContext: parsedRequestContext,
    dependentService: service,
    contextCommand: contextCommand,
  };
};
export const parseRequestContext = (requestContext: string) => {
  let context: any = {};
  if (requestContext) {
    if (requestContext.indexOf("_PARSE_") !== -1) {
      context = getRequestContextWithPrefix(requestContext);
    } else {
      context.requestContext = requestContext;
    }
  } else {
    context.requestContext = "Unspecified";
  }
  return context;
};

export const formatDateString = (timestamp: string | null) => {
  if (!timestamp) return "Unknown";
  const date = new Date(timestamp);
  return date.toLocaleString();
};
export const getHighestPriorityStatus = (statuses: any[]) => {
  const statusOrder = ["critical", "warning", "ok", "unknown", "none"];
  return statuses.reduce((highestPriorityStatus, statusItem) => {
    const status = get(statusItem, "status", "").toLowerCase();
    return statusOrder.indexOf(status) <
      statusOrder.indexOf(highestPriorityStatus)
      ? status
      : highestPriorityStatus;
  }, statusOrder.at(-1));
};

export function sortPropertyLight(
  arr: any[],
  path: string,
  desc: boolean = false
) {
  const realPath = typeof path === "string" ? path.split(".") : [];
  const statusOrder = ["NONE", "UNKNOWN", "OK", "WARNING", "CRITICAL"];
  const sortedArr = arr.sort((a, b) => {
    let aProperty = a;
    let bProperty = b;
    realPath.forEach((key) => {
      aProperty = aProperty[key];
      bProperty = bProperty[key];
    });

    if (path === "statuses") {
      // For status sorting, use the highest priority status
      const aStatuses = aProperty || [];
      const bStatuses = bProperty || [];

      // If one has no statuses and the other does, empty statuses go to the end
      if (!aStatuses.length && bStatuses.length) return desc ? -1 : 1;
      if (aStatuses.length && !bStatuses.length) return desc ? 1 : -1;
      if (!aStatuses.length && !bStatuses.length) return 0;

      // Sort by the highest priority status
      const aHighest = aStatuses[0].status.toUpperCase();
      const bHighest = bStatuses[0].status.toUpperCase();
      const aIndex = statusOrder.indexOf(aHighest);
      const bIndex = statusOrder.indexOf(bHighest);

      // If priorities are equal, sort by count
      if (aIndex === bIndex) {
        return desc
          ? aStatuses[0].count - bStatuses[0].count
          : bStatuses[0].count - aStatuses[0].count;
      }

      // For ascending: CRITICAL (4) -> WARNING (3) -> OK (2) -> UNKNOWN (1) -> NONE (0)
      // For descending: NONE (0) -> UNKNOWN (1) -> OK (2) -> WARNING (3) -> CRITICAL (4)
      return desc ? aIndex - bIndex : bIndex - aIndex;
    } else if (path === "last_status_changed") {
      // For last_status_changed, handle 'Unknown' values
      const aStr = aProperty?.toString() || "";
      const bStr = bProperty?.toString() || "";

      // Put 'Unknown' values at the end
      if (aStr === "Unknown" && bStr !== "Unknown") return 1;
      if (aStr !== "Unknown" && bStr === "Unknown") return -1;
      if (aStr === "Unknown" && bStr === "Unknown") return 0;

      // Parse dates and compare timestamps
      const aDate = new Date(aStr).getTime();
      const bDate = new Date(bStr).getTime();
      return desc ? bDate - aDate : aDate - bDate;
    } else {
      // Default string comparison
      const aStr = (aProperty || "").toString().toLowerCase();
      const bStr = (bProperty || "").toString().toLowerCase();
      if (aStr > bStr) return desc ? -1 : 1;
      if (aStr < bStr) return desc ? 1 : -1;
      return 0;
    }
  });
  return sortedArr;
}

export const getStatusClass = (status: string | null | undefined) => {
  const statusClassMap: Record<string, string> = {
    critical: "status-critical",
    warning: "status-warning",
    ok: "status-ok",
    unknown: "status-unknown",
    none: "status-none",
  };
  const statusKey = status?.toLowerCase() || "";
  return statusClassMap[statusKey] || "status-default";
};

export const formatStatus = (
  status: string | null | undefined,
  count: number
) => {
  if (status === "none") return "None";
  
  // FIXED: Use short form like Ember.js (CRIT instead of CRITICAL, WARN instead of WARNING)
  const shortStatusMap: Record<string, string> = {
    'CRITICAL': 'CRIT',
    'WARNING': 'WARN',
    'OK': 'OK',
    'UNKNOWN': 'UNKWN',
    'NONE': 'NONE'
  };
  
  const shortStatus = shortStatusMap[status?.toUpperCase() || ''] || status?.toUpperCase();
  
  if (count <= 1) return shortStatus;
  return `${shortStatus} (${count})`;
};

export const isWithin24Hours = (timestamp: number): number => {
  const now = new Date();
  const lastUpdated = new Date(timestamp);
  const diffInHours =
    (now.getTime() - lastUpdated.getTime()) / (1000 * 60 * 60);
  return diffInHours <= 24 ? 1 : 0;
};

export const isInstallable = (stackName: string, componentName: string) => {
  const notInstallable =
    stackName === "HDF" ? ["ACTIVITY_ANALYZER", "ACTIVITY_EXPLORER"] : [];
  return !notInstallable.includes(componentName);
};

export const isShownOnInstallerAssignMasterPage = (
  component: string,
  isMaster: boolean,
  stackName: string
) => {
  var mastersNotShown = [
    "MYSQL_SERVER",
    "POSTGRESQL_SERVER",
    "HIVE_SERVER_INTERACTIVE",
  ];
  return (
    isMaster &&
    isInstallable(stackName, component) &&
    !mastersNotShown.includes(component)
  );
};

export function isShownOnAddServiceAssignMasterPage(
  component: string,
  isMaster: boolean,
  stackName: string,
  isHaEnabled: boolean
) {
  let isVisible = isShownOnInstallerAssignMasterPage(
    component,
    isMaster,
    stackName
  );
  if (isHaEnabled) {
    isVisible = isVisible && component !== "SECONDARY_NAMENODE";
  }
  return isVisible;
}

export function timeAgo(timestamp: any) {
  const now: any = new Date();
  const past: any = new Date(timestamp);

  const secondsAgo = Math.floor((now - past) / 1000);
  const minutesAgo = Math.floor(secondsAgo / 60);
  const hoursAgo = Math.floor(minutesAgo / 60);
  const daysAgo = Math.floor(hoursAgo / 24);
  const monthsAgo = Math.floor(daysAgo / 30); // Approximate
  const yearsAgo = Math.floor(daysAgo / 365); // Approximate

  if (yearsAgo > 0) {
    return yearsAgo === 1 ? "1 year" : `${yearsAgo} years`;
  } else if (monthsAgo > 0) {
    return monthsAgo === 1 ? "1 month" : `${monthsAgo} months`;
  } else if (daysAgo > 0) {
    return daysAgo === 1 ? "1 day" : `${daysAgo} days`;
  } else if (hoursAgo > 0) {
    return hoursAgo === 1 ? "1 hour" : `${hoursAgo} hours`;
  } else if (minutesAgo > 0) {
    return minutesAgo === 1 ? "1 minute" : `${minutesAgo} minutes`;
  } else {
    return secondsAgo === 1 ? "1 second" : `${secondsAgo} seconds`;
  }
}

export const showErrorModal = (error: any) => {
  const modalProps = {
    modalTitle: get(messages, "common.errorPopup.header", ""),
    modalBody: error,
    onClose: () => {},
    successCallback: () => {
      modalManager.hide();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: false,
      okButtonVariant: "primary",
    },
  };
  modalManager.show(modalProps);
};

export const showAlertModal = (header: any, body: any) => {
  const modalProps = {
    modalTitle: header,
    modalBody: body,
    onClose: () => {},
    successCallback: () => {
      modalManager.hide();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: false,
      okButtonVariant: "primary",
    },
  };
  modalManager.show(modalProps);
};

export const showRollingNothingToDoModal = (body: any) => {
  const modalProps = {
    modalTitle: get(messages, "rolling.nothingToDo.header", ""),
    modalBody: body,
    onClose: () => {},
    successCallback: () => {
      modalManager.hide();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: false,
      okButtonVariant: "primary",
    },
  };
  modalManager.show(modalProps);
};
export function getIconObject(status: string, onlyView: boolean) {
  if(status === "ABORTED" && onlyView) {
    return { icon: faMinus, color: "text-warning" };
  }
  switch (status.toUpperCase()) {
    case "PENDING":
    case "QUEUED":
      return { icon: faCogs, color: "text-secondary" };
    case "IN_PROGRESS":
      return { icon: faGears, color: "text-info" };
    case "COMPLETED":
      return { icon: faCheck, color: "text-success" };
    case "FAILED":
      case "HOLDING_FAILED":
      return { icon: faExclamationCircle, color: "text-danger" };
    case "ABORTED":
      return { icon: faPause, color: "text-ligth" };
    case "HOLDING":
    case "HOLDING_TIMEDOUT":
    case "TIMEDOUT":
      return { icon: faClock, color: "text-warning" };
    case "SKIPPED_FAILED":
      return { icon: faShare, color: "text-danger" };  
    default:
      return { icon: faPause, color: "text-light" };
  }
}

export const failedStatuses = [
  "HOLDING_FAILED",
  "HOLDING_TIMEDOUT",
  "FAILED",
  "TIMED_OUT",
  "ABORTED",
];
export const activeStatuses = [
  "HOLDING_FAILED",
  "HOLDING_TIMEDOUT",
  "FAILED",
  "TIMED_OUT",
  "HOLDING",
  "IN_PROGRESS",
  "ABORTED",
];

export function mapStackConfigProperties(json: any) {
  const configToPlain = {
    id: "id",
    name: "StackConfigurations.property_name",
    displayName: "StackConfigurations.property_display_name",
    fileName: "StackConfigurations.type",
    filename: "StackConfigurations.type",
    description: "StackConfigurations.property_description",
    value: "StackConfigurations.property_value",
    recommendedValue: "StackConfigurations.property_value",
    serviceName: "StackConfigurations.service_name",
    stackName: "StackConfigurations.stack_name",
    stackVersion: "StackConfigurations.stack_version",
    isOverridable: "StackConfigurations.property_value_attributes.overridable",
    isVisible: "StackConfigurations.property_value_attributes.visible",
    showLabel:
      "StackConfigurations.property_value_attributes.show_property_name",
    displayType: "StackConfigurations.property_value_attributes.type",
    unit: "StackConfigurations.property_value_attributes.unit",
    isRequired: "is_required",
    isReconfigurable: "is_reconfigurable",
    isEditable: "is_editable",
    isRequiredByAgent: "is_required_by_agent",
    isFinal: "recommended_is_final",
    recommendedIsFinal: "recommended_is_final",
    supportsFinal: "supports_final",
    propertyDependedBy: "StackConfigurations.property_depended_by",
    propertyDependsOn: "StackConfigurations.property_depends_on",
    valueAttributes: "StackConfigurations.property_value_attributes",
    category: "category",
    index: "index",
    radioName: "radioName",
    options: "options",
    dependentConfigPattern: "dependentConfigPattern",
  };

  let filteredConfigs = [];
  var clusterConfigs: boolean = false;
  if (json && json.Versions) {
    json = { items: [json] };
    clusterConfigs = true;
  }

  if (json && json.items) {
    const configs: any = [];

    json.items.forEach((stackItem: any) => {
      var configTypeInfo = clusterConfigs
        ? get(stackItem, "Versions.config_types")
        : get(stackItem, "StackServices.config_types");
      stackItem.configurations.forEach((config: any) => {
        if (clusterConfigs) {
          config.StackConfigurations = config.StackLevelConfigurations;
        }
        const configType = getConfigTagFromFileName(
          get(config, "StackConfigurations.type", "")
        );
        config.id = configId(
          config.StackConfigurations?.property_name,
          configType
        );
        config.recommended_is_final =
          config.StackConfigurations?.final === "true";
        config.supports_final =
          !!configTypeInfo[configType] &&
          configTypeInfo[configType].supports.final === "true";

        const attributes =
          config.StackConfigurations?.property_value_attributes;
        if (attributes) {
          config.is_required =
            !attributes?.empty_value_valid &&
            config.StackConfigurations?.property_value !== null;
          config.is_reconfigurable = !(
            attributes?.editable_only_at_install ||
            config.StackConfigurations?.type === "cluster-env.xml"
          );
          config.is_editable = !attributes?.read_only;
          config.is_required_by_agent = !attributes?.ui_only_property;
        }

        config.StackConfigurations = config.StackConfigurations || {};
        if (!config.StackConfigurations?.property_display_name) {
          config.StackConfigurations.property_display_name =
            config.StackConfigurations?.property_name;
        }

        if (!config.StackConfigurations?.service_name) {
          config.StackConfigurations.service_name = "MISC";
        }

        if (!attributes || !attributes.type) {
          if (!attributes) {
            config.StackConfigurations.property_value_attributes = {};
          }
          config.StackConfigurations.property_value_attributes.type =
            getDefaultDisplayType(config.StackConfigurations?.property_value);
        }

        config.StackConfigurations.property_depended_by = [];
        if (config.dependencies && config.dependencies.length > 0) {
          config.dependencies.forEach((dep: any) => {
            config.StackConfigurations?.property_depended_by.push({
              type: dep.StackConfigurationDependency.dependency_type,
              name: dep.StackConfigurationDependency.dependency_name,
            });
          });
        }

        const staticConfigInfo = parseConfig(config, configToPlain);
        const value =
          staticConfigInfo.recommendedValue || staticConfigInfo.value;
        staticConfigInfo.value = staticConfigInfo.recommendedValue =
          formatPropertyValue(staticConfigInfo, value);
        staticConfigInfo.isSecureConfig = false;
        staticConfigInfo.description = getDescription(
          staticConfigInfo.description,
          staticConfigInfo.displayType
        );
        staticConfigInfo.name = JSON.parse(`"${staticConfigInfo.name}"`);
        staticConfigInfo.isUserProperty = false;
        staticConfigInfo.index = staticConfigInfo.index ?? null;

        configs.push(staticConfigInfo);
      });
    });
    addUIOnlyProperties(configs);
    filteredConfigs = configs;
  }
  return filteredConfigs;
}

const transformAlertNotifications = (notifications: any[]): UIProperty[] => {
  return notifications.map((notification) => ({
    name: notification.name,
    displayName: notification.displayName,
    description: notification.description,
    displayType: notification.displayType,
    isRequiredByAgent: false,
    isOverridable: notification.isOverridable,
    isVisible: notification.isVisible,
    isRequired: notification.isRequired,
    isReconfigurable: notification.isReconfigurable,
    serviceName: notification.serviceName,
    category: notification.category,
    recommendedValue: notification.recommendedValue,
    rowStyleClass: notification.rowStyleClass,
    filename: notification.filename,
    index: undefined,
  }));
};

const addUIOnlyProperties = (configs: any) => {
  const transformedAlertNotifications =
    transformAlertNotifications(alert_notifications);
  const combinedProperties = kerberos_ui_properties.concat(
    transformedAlertNotifications
  );
  combinedProperties.forEach((p: any) => {
    if (p.name === "dfs.ha.fencing.methods") return;

    configs.push({
      id: configId(p.name, p.filename),
      name: p.name,
      display_name: p.displayName,
      file_name: p.filename,
      description: p.description || "",
      is_required_by_agent: p.isRequiredByAgent !== false,
      service_name: p.serviceName,
      supports_final: false,
      category: p.category,
      index: p.index,
    });
  });
};

const parseConfig = (config: any, configToPlain: any) => {
  const parsedConfig: Record<string, any> = {};
  for (const [key, value] of Object.entries(configToPlain)) {
    parsedConfig[key] = getValue(config, value);
  }
  return parsedConfig;
};

const getDescription = (description: any, displayType: any) => {
  const additionalDescription = get(
    messages,
    "services.service.config.password.additionalDescription"
  );

  if (displayType === "password") {
    if (description && !description.includes(additionalDescription)) {
      return `${description}\n${additionalDescription}`;
    } else {
      return additionalDescription;
    }
  }
  return description;
};

const formatPropertyValue = (
  serviceConfigProperty: any,
  originalValue: any
) => {
  const value =
    originalValue == null ? serviceConfigProperty.value : originalValue;
  const displayType =
    serviceConfigProperty.displayType ||
    serviceConfigProperty.valueAttributes?.type;

  if (serviceConfigProperty.name === "kdc_type") {
    return "";
  }

  if (/^\s+$/.test("" + value)) {
    return " ";
  }

  switch (displayType) {
    case "int":
      if (/\d+m$/.test(value)) {
        return value.slice(0, value.length - 1);
      } else {
        const intValue = parseInt(value, 10);
        return isNaN(intValue) ? "" : intValue.toString();
      }
    case "float":
      const floatValue = parseFloat(value);
      return isNaN(floatValue) ? "" : floatValue.toString();
    case "componentHosts":
      if (typeof value === "string") {
        return value.replace(/\[|]|'|&apos;/g, "").split(",");
      }
      return value;
    case "content":
    case "string":
    case "multiLine":
    case "directories":
    case "directory":
      return trimProperty({ displayType, value });
    default:
      return value;
  }
};

const trimProperty = (property: any) => {
  const displayType = get(property, "displayType", "");
  const value = get(property, "value", "");
  const name = get(property, "name", "");
  let rez;

  switch (displayType) {
    case "directories":
    case "directory":
      rez = value.replace(/,/g, " ").trim().split(/\s+/g).join(",");
      break;
    case "host":
      rez = value.trim();
      break;
    case "password":
      break;
    default:
      if (
        name === "javax.jdo.option.ConnectionURL" ||
        name === "oozie.service.JPAService.jdbc.url"
      ) {
        rez = value.trim();
      } else {
        rez = typeof value === "string" ? value.replace(/(\s+$)/g, "") : value;
      }
  }

  return rez === "" || rez === undefined ? value : rez;
};

const getValue = (obj: any, path: any) => {
  return path.split(".").reduce((acc: any, part: any) => acc && acc[part], obj);
};

const configId = (name: any, fileName: any) => {
  return name + "__" + getConfigTagFromFileName(fileName);
};

const getConfigTagFromFileName = (fileName: any) => {
  if (fileName === "") return "";
  return fileName.endsWith(".xml") ? fileName.slice(0, -4) : fileName;
};

export const redirectToLogin = () => {
  window.location.href = "#/login";
  window.location.reload();
};

const getDefaultDisplayType = (value: any) => {
  return value && typeof value === "string" && value.includes("\n")
    ? "multiLine"
    : "string";
};

export const allServiceProperties = [
  ...hbaseProperties,
  ...rangerProperties,
  ...yarnProperties,
  ...hdfsProperties,
  ...hiveProperties,
  ...kerberosProperties,
  ...mapReduce2Properties,
  ...zookeeperProperties,
  ...tezProperties,
];

export const getUpgradeRequestStatus = (
  upgradeState: string,
  isDowngrade: boolean = false
) => {
  let key = "";
  switch (upgradeState) {
    case "QUEUED":
    case "PENDING":
    case "IN_PROGRESS":
      key = "admin.stackUpgrade.state.inProgress";
      break;
    case "COMPLETED":
      key = "admin.stackUpgrade.state.completed";
      break;
    case "ABORTED":
      key = "admin.stackUpgrade.state.paused";
      break;
    case "TIMEDOUT":
    case "FAILED":
    case "HOLDING_FAILED":
    case "HOLDING_TIMEDOUT":
      key = "admin.stackUpgrade.state.paused";
      break;
    default:
      key = "admin.stackUpgrade.state.init";
      break;
  }
  if (key) {
    key += isDowngrade ? ".downgrade" : "";
  }

  if(upgradeState.includes("HOLDING")) {
    if(isDowngrade) {
      key = "admin.stackVersions.version.downgrade.pause";
    } else {
      key = "admin.stackVersions.version.upgrade.pause";
    }
  }
  return key;
};

export const addDeleteComponentsMap = {
  ZOOKEEPER_SERVER: {
    addPropertyName: "addZooKeeperServer",
    deletePropertyName: "fromDeleteZkServer",
    configTagsCallbackName: "loadZookeeperConfigs",
    configsCallbackName: "saveZkConfigs",
  },
  HIVE_METASTORE: {
    deletePropertyName: "deleteHiveMetaStore",
    hostPropertyName: "hiveMetastoreHost",
    configTagsCallbackName: "loadHiveConfigs",
    configsCallbackName: "onLoadHiveConfigs",
  },
  WEBHCAT_SERVER: {
    deletePropertyName: "deleteWebHCatServer",
    hostPropertyName: "webhcatServerHost",
    configTagsCallbackName: "loadWebHCatConfigs",
    configsCallbackName: "onLoadHiveConfigs",
  },
  HIVE_SERVER: {
    addPropertyName: "addHiveServer",
    deletePropertyName: "deleteHiveServer",
    configTagsCallbackName: "loadHiveConfigs",
    configsCallbackName: "onLoadHiveConfigs",
  },
  NIMBUS: {
    deletePropertyName: "deleteNimbusHost",
    hostPropertyName: "nimbusHost",
    configTagsCallbackName: "loadStormConfigs",
    configsCallbackName: "onLoadStormConfigs",
  },
  ATLAS_SERVER: {
    deletePropertyName: "deleteAtlasServer",
    hostPropertyName: "atlasServer",
    configTagsCallbackName: "loadAtlasConfigs",
    configsCallbackName: "onLoadAtlasConfigs",
  },
  RANGER_KMS_SERVER: {
    deletePropertyName: "deleteRangerKMSServer",
    hostPropertyName: "rangerKMSServerHost",
    configTagsCallbackName: "loadRangerConfigs",
    configsCallbackName: "onLoadRangerConfigs",
  },
};

export const zooKeeperRelatedServices = [
  {
    serviceName: "HIVE",
    typesToLoad: ["hive-site", "webhcat-site"],
    typesToSave: ["hive-site", "webhcat-site"],
  },
  {
    serviceName: "YARN",
    typesToLoad: ["yarn-site", "zoo.cfg"],
    typesToSave: ["yarn-site"],
  },
  {
    serviceName: "HBASE",
    typesToLoad: ["hbase-site"],
    typesToSave: ["hbase-site"],
  },
  {
    serviceName: "ACCUMULO",
    typesToLoad: ["accumulo-site"],
    typesToSave: ["accumulo-site"],
  },
  {
    serviceName: "KAFKA",
    typesToLoad: ["kafka-broker"],
    typesToSave: ["kafka-broker"],
  },
  {
    serviceName: "ATLAS",
    typesToLoad: ["application-properties", "infra-solr-env"],
    typesToSave: ["application-properties"],
  },
  {
    serviceName: "STORM",
    typesToLoad: ["storm-site"],
    typesToSave: ["storm-site"],
  },
];

export const translate = (messageKey: string) => {
  let translatedMessage: any = parse(
    DOMPurify.sanitize(t(messageKey), { USE_PROFILES: { html: true } })
  );
  if (!translatedMessage || isEmpty(translatedMessage)) {
    translatedMessage = get(messages, messageKey, "");
  }
  return translatedMessage;
};

// {translateWithVariables("admin.stackUpgrade.pauseUpgrade.warning", { "0": variable0, "1": variable1 })}
export const translateWithVariables = (messageKey: string, replacements: Record<string, string> = {}) => {
    let message = t(messageKey);

    Object.entries(replacements).forEach(([key, value]) => {
        message = message.replace(new RegExp(`\\{${key}\\}`, 'g'), value);
    });

    return parse(DOMPurify.sanitize(message, { USE_PROFILES: { html: true } }));
};

/**
 * Helper function to generate host map
 * @param hostMapObject - Object mapping hosts to components
 * @param hostNames - Array of host names
 * @param componentName - Component name to add
 * @returns Updated host map object
 */
export const generateHostMap = (
  hostMapObject: Record<string, string[]>,
  hostNames: string[],
  componentName: string
): Record<string, string[]> => {
  if (!hostNames.length) return hostMapObject;

  hostNames.forEach((hostName) => {
    if (!hostMapObject[hostName]) {
      hostMapObject[hostName] = [];
    }

    if (!hostMapObject[hostName].includes(componentName)) {
      hostMapObject[hostName].push(componentName);
    }
  });
  return hostMapObject;
};

/**
 * Get component information for hosts
 * @param clusterName - Cluster name
 * @returns Promise resolving to object mapping hosts to their components
 */
export const getComponentForHosts = async (
  clusterName: string
): Promise<Record<string, string[]>> => {
  let hostsMap: Record<string, string[]> = {};

  try {
    // Get host component details from API
    const fields =
      "fields=Hosts/host_name,host_components/HostRoles/component_name";
    const hostDetailsResponse = await HostsApi.getHostComponentsDetails(
      clusterName,
      fields
    );

    // Process the response to build the hosts map
    if (hostDetailsResponse && hostDetailsResponse.items) {
      hostDetailsResponse.items.forEach((host: any) => {
        const hostName = host.Hosts.host_name;

        if (host.host_components) {
          host.host_components.forEach((component: any) => {
            const componentName = component.HostRoles.component_name;

            if (hostName && componentName) {
              hostsMap = generateHostMap(hostsMap, [hostName], componentName);
            }
          });
        }
      });
    }
  } catch (error) {
    console.error("Error getting component information for hosts:", error);
  }

  return hostsMap;
};

/**
 * Generate host groups for recommendations
 * @param clusterName - Cluster name
 * @param hostNames - Array of host names
 * @returns Promise resolving to recommendations object with blueprint and blueprint_cluster_binding
 */
export const generateHostGroups = async (
  clusterName: string,
  hostNames: string[]
) => {
  const recommendations = {
    blueprint: {
      host_groups: [] as any,
    },
    blueprint_cluster_binding: {
      host_groups: [] as any,
    },
  };

  // Get component mapping for hosts
  const hostsMap = await getComponentForHosts(clusterName);

  // Create host groups
  for (let i = 0; i < hostNames.length; i++) {
    const hostName = hostNames[i];
    const hostGroup = {
      name: `host-group-${i + 1}`,
      components: [] as any,
    };

    const hostComponents = hostsMap[hostName];
    if (!hostComponents) continue;

    // Add components to the host group
    for (let j = 0; j < hostComponents.length; j++) {
      hostGroup.components.push({
        name: hostComponents[j] as any,
      });
    }

    // Add host group to blueprint and blueprint_cluster_binding
    recommendations.blueprint.host_groups.push(hostGroup);
    recommendations.blueprint_cluster_binding.host_groups.push({
      name: `host-group-${i + 1}`,
      hosts: [
        {
          fqdn: hostName,
        },
      ],
    });
  }

  return recommendations;
};

/**
 * Extract configurations from the loaded configs state
 * @returns Configuration object in the format expected by the recommendations API
 */
const getConfigurationsFromLoadedConfigs = (configs: any) => {
  // This function would access the configs state that was populated by loadConfigs
  // and transform it into the format expected by the recommendations API

  const configurations = {};

  // Iterate through the configs state
  Object.entries(configs).forEach(([configType, properties]) => {
    //@ts-ignore
    configurations[configType] = {
      //@ts-ignore
      properties: { ...properties },
    };
  });

  return configurations;
};

/**
 * Build the complete payload for the recommendations API
 * @param clusterName - Cluster name
 * @param serviceNamesToDelete - Services being deleted
 * @param stackServicesFromHook - Stack services from the hook
 * @param allHostNames - All host names
 * @returns Promise resolving to complete payload for the recommendations API
 */
export const buildRecommendationsPayload = async (
  clusterName: string,
  serviceNamesToDelete: string[],
  stackServicesFromHook: any,
  allHostNames: string[],
  configs: any
) => {
  const remainingServices =
    stackServicesFromHook
      ?.filter(
        (s: any) =>
          !serviceNamesToDelete.includes(s?.StackServices?.service_name)
      )
      ?.map((s: any) => s?.StackServices?.service_name) || [];

  const hosts = allHostNames || [];
  const hostGroupsRecommendations = await generateHostGroups(
    clusterName,
    hosts
  );
  const configurations = getConfigurationsFromLoadedConfigs(configs);

  return {
    recommend: "configurations",
    hosts: hosts,
    services: remainingServices,
    user_context: {
      operation: "RecommendAttribute",
    },
    recommendations: {
      blueprint: {
        host_groups: hostGroupsRecommendations.blueprint.host_groups,
        configurations: configurations,
      },
      blueprint_cluster_binding:
        hostGroupsRecommendations.blueprint_cluster_binding,
    },
    serviceName: serviceNamesToDelete[0], // Add the service being deleted
    clusterId: 2, // This might need to be dynamic
    autoComplete: "false",
    configsResponse: "false",
  };
};

/**
 * Clean up host groups from components that should be removed
 *
 * @param hostGroups - The host groups object containing blueprint and blueprint_cluster_binding
 * @param serviceNames - Array of service names to be deleted
 * @param stackServices - Stack services data from the context or hook
 * @returns The modified host groups with deleted components removed
 */
export const removeDeletedComponents = (
  hostGroups: any,
  serviceNames: string[],
  stackServices: any[]
): any => {
  // Get all component names for the services that are being deleted
  const componentsToRemove: string[] = [];

  // For each service being deleted, get all its components
  stackServices.forEach((service: any) => {
    if (serviceNames.includes(service?.StackServices?.service_name)) {
      // Get components for this service
      const serviceComponents = service.components || [];

      // Extract component names and add to the list
      serviceComponents.forEach((component: any) => {
        const componentName = component?.StackServiceComponents?.component_name;
        if (componentName) {
          componentsToRemove.push(componentName);
        }
      });
    }
  });

  // Create a deep copy of the host groups to avoid mutating the original
  const updatedHostGroups = JSON.parse(JSON.stringify(hostGroups));

  // Filter out components that should be removed from each host group
  if (updatedHostGroups.blueprint && updatedHostGroups.blueprint.host_groups) {
    updatedHostGroups.blueprint.host_groups.forEach((hostGroup: any) => {
      if (hostGroup.components) {
        hostGroup.components = hostGroup.components.filter((component: any) => {
          return !componentsToRemove.includes(component.name);
        });
      }
    });
  }

  return updatedHostGroups;
};

export const getUpgradeDisplayName = (upgradeType: string) => {
  switch (upgradeType) {
    case "ROLLING":
      return "Rolling";
    case "NON_ROLLING":
      return "Express";
    case "HOST_ORDERED":
      return "Host-Ordered";
    default:
      return upgradeType;
  }
}

export const initialOptions = [
  { key: "ALL", values: ["ALL"], count: 0 },
  {
    key: "NOT INSTALLED",
    values: ["INSTALL_FAILED", "INSTALLING", "NOT_REQUIRED"],
    count: 0,
  },
  { key: "UPGRADE READY", values: ["UPGRADE_READY"], count: 0 },
  { key: "CURRENT", values: ["CURRENT"], count: 0 },
  { key: "INSTALLED", values: ["INSTALLED"], count: 0 },
  {
    key: "UPGRADE/DOWNGRADE IN PROGRESS",
    values: ["Upgrade/Downgrade in Progress"],
    count: 0,
  },
  { key: "READY TO FINALIZE", values: ["Ready to Finalize"], count: 0 },
];

export const initialUpgradeMethods = [
  {
    displayName: get(
      messages,
      "admin.stackVersions.version.upgrade.upgradeOptions.RU.title"
    ),
    type: "ROLLING",
    icon: "faDashboard",
    description: get(
      messages,
      "admin.stackVersions.version.upgrade.upgradeOptions.RU.description"
    ),
    selected: false,
    allowed: true,
    isCheckComplete: false,
    isCheckRequestInProgress: false,
    precheckResultsMessage: "",
    preCheckResultsModalContent: null as JSX.Element | null,
    precheckResultsTitle: "",
    action: "",
    isWizardRestricted: false,
  },
  {
    displayName: get(
      messages,
      "admin.stackVersions.version.upgrade.upgradeOptions.EU.title"
    ),
    type: "NON_ROLLING",
    icon: "faBolt",
    description: get(
      messages,
      "admin.stackVersions.version.upgrade.upgradeOptions.EU.description"
    ),
    selected: false,
    allowed: true,
    isCheckComplete: false,
    isCheckRequestInProgress: false,
    precheckResultsMessage: "",
    preCheckResultsModalContent: null as JSX.Element | null,
    precheckResultsTitle: "",
    action: "",
    isWizardRestricted: false,
  },
  {
    displayName: get(
      messages,
      "admin.stackVersions.version.upgrade.upgradeOptions.HOU.title"
    ),
    type: "HOST_ORDERED",
    icon: "faBolt",
    description: "",
    selected: false,
    allowed: false,
    isCheckComplete: false,
    isCheckRequestInProgress: false,
    precheckResultsMessage: "",
    preCheckResultsModalContent: null as JSX.Element | null,
    precheckResultsTitle: "",
    action: "",
    cantBeStarted: true,
  },
];

export const createFallbackWidgetsForService = (
  serviceName: string,
  userName: string,
) => {
  const baseWidgets = [];

  // Service-specific widget configurations
  switch (serviceName.toUpperCase()) {
    case "AMBARI_METRICS":
      baseWidgets.push(
        // Master Average Load
        {
          WidgetInfo: {
            id: "service-metrics-ambari-metrics-master-average-load",
            widget_name: "Average load",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "AverageLoad",
                service_name: "AMBARI_METRICS",
                component_name: "METRICS_COLLECTOR",
                metric_path: "metrics/hbase/master/AverageLoad",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Average Load",
                value: "${AverageLoad}",
              },
            ]),
            properties: JSON.stringify({}),
            scope: "USER",
            author: userName,
            description: "HBase master average load",
          },
        },
        // RegionServer Regions
        {
          WidgetInfo: {
            id: "service-metrics-ambari-metrics-region-server-regions",
            widget_name: "Number of Regions",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "regions",
                service_name: "AMBARI_METRICS",
                component_name: "METRICS_COLLECTOR",
                metric_path: "metrics/hbase/regionserver/regions",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Regions",
                value: "${regions}",
              },
            ]),
            properties: JSON.stringify({}),
            scope: "USER",
            author: userName,
            description: "HBase RegionServer regions",
          },
        },
        // RegionServer Store Files
        {
          WidgetInfo: {
            id: "service-metrics-ambari-metrics-region-server-store-files",
            widget_name: "Number of StoreFiles",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "storefiles",
                service_name: "AMBARI_METRICS",
                component_name: "METRICS_COLLECTOR",
                metric_path: "metrics/hbase/regionserver/storefiles",
              },
            ]),
            values: JSON.stringify([
              {
                name: "StoreFiles",
                value: "${storefiles}",
              },
            ]),
            properties: JSON.stringify({}),
            scope: "USER",
            author: userName,
            description: "HBase RegionServer store files",
          },
        },
        // RegionServer Requests
        {
          WidgetInfo: {
            id: "service-metrics-ambari-metrics-region-server-requests",
            widget_name: "Total Request Count",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "requests._rate",
                service_name: "AMBARI_METRICS",
                component_name: "METRICS_COLLECTOR",
                metric_path: "metrics/hbase/regionserver/requests._rate",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Request Rate",
                value: "${requests._rate}",
              },
            ]),
            properties: JSON.stringify({}),
            scope: "USER",
            author: userName,
            description: "HBase RegionServer requests",
          },
        },
        // RegionServer Block Cache Hit Percent
        {
          WidgetInfo: {
            id: "service-metrics-ambari-metrics-region-server-block-cache-hit-percent",
            widget_name: "Block Cache Hit Percent",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "blockCacheHitPercent",
                service_name: "AMBARI_METRICS",
                component_name: "METRICS_COLLECTOR",
                metric_path: "metrics/hbase/regionserver/blockCacheHitPercent",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Block Cache Hit %",
                value: "${blockCacheHitPercent}",
              },
            ]),
            properties: JSON.stringify({}),
            scope: "USER",
            author: userName,
            description: "HBase RegionServer block cache hit percent",
          },
        },
        // RegionServer Compaction Queue Size
        {
          WidgetInfo: {
            id: "service-metrics-ambari-metrics-region-server-compaction-queue-size",
            widget_name: "Compaction Queue Size",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "compactionQueueSize",
                service_name: "AMBARI_METRICS",
                component_name: "METRICS_COLLECTOR",
                metric_path: "metrics/hbase/regionserver/compactionQueueSize",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Compaction Queue",
                value: "${compactionQueueSize}",
              },
            ]),
            properties: JSON.stringify({}),
            scope: "USER",
            author: userName,
            description: "HBase RegionServer compaction queue size",
          },
        },
      );
      break;

    case "HDFS":
      baseWidgets.push(
        // NameNode GC count
        {
          WidgetInfo: {
            id: "hdfs-namenode-gc-count",
            widget_name: "NameNode GC count",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "jvm.JvmMetrics.GcCount._rate",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/jvm/gcCount._rate",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
              {
                name: "jvm.JvmMetrics.GcCountG1 Old Generation._rate",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/jvm/GcCountG1 Old Generation._rate",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
            ]),
            values: JSON.stringify([
              {
                name: "GC total count",
                value: "${jvm.JvmMetrics.GcCount._rate}",
              },
              {
                name: "GC count of type major collection",
                value: "${jvm.JvmMetrics.GcCountG1 Old Generation._rate}",
              },
            ]),
            properties: JSON.stringify({
              graph_type: "LINE",
              time_range: "1",
            }),
            scope: "USER",
            author: userName,
            description:
              "Count of total garbage collections and count of major type garbage collections of the JVM.",
          },
        },
        // NameNode GC time
        {
          WidgetInfo: {
            id: "hdfs-namenode-gc-time",
            widget_name: "NameNode GC time",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "jvm.JvmMetrics.GcTimeMillisG1 Old Generation._rate",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/jvm/GcTimeMillisG1 Old Generation._rate",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
            ]),
            values: JSON.stringify([
              {
                name: "GC time in major collection",
                value: "${jvm.JvmMetrics.GcTimeMillisG1 Old Generation._rate}",
              },
            ]),
            properties: JSON.stringify({
              display_unit: "ms",
              graph_type: "LINE",
              time_range: "1",
            }),
            scope: "USER",
            author: userName,
            description:
              "Total time taken by major type garbage collections in milliseconds.",
          },
        },
        // NN Connection Load
        {
          WidgetInfo: {
            id: "hdfs-nn-connection-load",
            widget_name: "NN Connection Load",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "rpc.rpc.client.NumOpenConnections",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/rpc/client/NumOpenConnections",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
              {
                name: "rpc.rpc.datanode.NumOpenConnections",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/rpc/datanode/NumOpenConnections",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Open Client Connections",
                value: "${rpc.rpc.client.NumOpenConnections}",
              },
              {
                name: "Open Datanode Connections",
                value: "${rpc.rpc.datanode.NumOpenConnections}",
              },
            ]),
            properties: JSON.stringify({
              graph_type: "LINE",
              time_range: "1",
            }),
            scope: "USER",
            author: userName,
            description:
              "Number of open RPC connections being managed by NameNode.",
          },
        },
        // NameNode Heap
        {
          WidgetInfo: {
            id: "hdfs-namenode-heap",
            widget_name: "NameNode Heap",
            widget_type: "GRAPH",
            metrics: JSON.stringify([
              {
                name: "jvm.JvmMetrics.MemHeapCommittedM",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/jvm/memHeapCommittedM",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
              {
                name: "jvm.JvmMetrics.MemHeapUsedM",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/jvm/memHeapUsedM",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
            ]),
            values: JSON.stringify([
              {
                name: "JVM heap committed",
                value: "${jvm.JvmMetrics.MemHeapCommittedM}",
              },
              {
                name: "JVM heap used",
                value: "${jvm.JvmMetrics.MemHeapUsedM}",
              },
            ]),
            properties: JSON.stringify({
              display_unit: "MB",
              graph_type: "LINE",
              time_range: "1",
            }),
            scope: "USER",
            author: userName,
            description:
              "Heap memory committed and Heap memory used with respect to time.",
          },
        },
        // Failed disk volumes
        {
          WidgetInfo: {
            id: "hdfs-failed-disk-volumes",
            widget_name: "Failed disk volumes",
            widget_type: "NUMBER",
            metrics: JSON.stringify([
              {
                name: "FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.NumFailedVolumes._sum",
                service_name: "HDFS",
                component_name: "DATANODE",
                metric_path: "metrics/dfs/datanode/NumFailedVolumes",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Failed disk volumes",
                value:
                  "${FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.NumFailedVolumes._sum}",
              },
            ]),
            properties: JSON.stringify({
              display_unit: "",
            }),
            scope: "USER",
            author: userName,
            description:
              "Number of Failed disk volumes across all DataNodes. Its indicative of HDFS bad health.",
          },
        },
        // Blocks With Corrupted Replicas
        {
          WidgetInfo: {
            id: "hdfs-blocks-corrupted-replicas",
            widget_name: "Blocks With Corrupted Replicas",
            widget_type: "NUMBER",
            metrics: JSON.stringify([
              {
                name: "Hadoop:service=NameNode,name=FSNamesystem.CorruptBlocks",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/dfs/FSNamesystem/CorruptBlocks",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Blocks With Corrupted Replicas",
                value:
                  "${Hadoop:service=NameNode,name=FSNamesystem.CorruptBlocks}",
              },
            ]),
            properties: JSON.stringify({
              warning_threshold: "0",
              error_threshold: "50",
            }),
            scope: "USER",
            author: userName,
            description:
              "Number represents data blocks with at least one corrupted replica (but not all of them). Its indicative of HDFS bad health.",
          },
        },
        // Under Replicated Blocks
        {
          WidgetInfo: {
            id: "hdfs-under-replicated-blocks",
            widget_name: "Under Replicated Blocks",
            widget_type: "NUMBER",
            metrics: JSON.stringify([
              {
                name: "Hadoop:service=NameNode,name=FSNamesystem.UnderReplicatedBlocks",
                service_name: "HDFS",
                component_name: "NAMENODE",
                metric_path: "metrics/dfs/FSNamesystem/UnderReplicatedBlocks",
                host_component_criteria:
                  "host_components/metrics/dfs/FSNamesystem/HAState=active",
              },
            ]),
            values: JSON.stringify([
              {
                name: "Under Replicated Blocks",
                value:
                  "${Hadoop:service=NameNode,name=FSNamesystem.UnderReplicatedBlocks}",
              },
            ]),
            properties: JSON.stringify({
              warning_threshold: "0",
              error_threshold: "50",
            }),
            scope: "USER",
            author: userName,
            description:
              "Number represents file blocks that does not meet the replication factor criteria. Its indicative of HDFS bad health.",
          },
        },
        // HDFS Space Utilization
        {
          WidgetInfo: {
            id: "hdfs-space-utilization",
            widget_name: "HDFS Space Utilization",
            widget_type: "GAUGE",
            metrics: JSON.stringify([
              {
                name: "FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.Remaining",
                service_name: "HDFS",
                component_name: "DATANODE",
                metric_path:
                  "metrics/FSDatasetState/org/apache/hadoop/hdfs/server/datanode/fsdataset/impl/FsDatasetImpl/Remaining",
              },
              {
                name: "FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.Capacity",
                service_name: "HDFS",
                component_name: "DATANODE",
                metric_path: "metrics/dfs/datanode/Capacity",
              },
            ]),
            values: JSON.stringify([
              {
                name: "HDFS Space Utilization",
                value:
                  "${(FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.Capacity - FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.Remaining)/FSDatasetState.org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImpl.Capacity}",
              },
            ]),
            properties: JSON.stringify({
              warning_threshold: "0.75",
              error_threshold: "0.9",
            }),
            scope: "USER",
            author: userName,
            description: "Percentage of available space used in the DFS.",
          },
        },
      );
      break;

    case "YARN":
      baseWidgets.push({
        WidgetInfo: {
          id: "yarn-resourcemanager-heap",
          widget_name: "ResourceManager Heap",
          widget_type: "GRAPH",
          metrics: JSON.stringify([
            {
              name: "resourcemanager_heap",
              service_name: "YARN",
              component_name: "RESOURCEMANAGER",
              metric_path: "metrics/jvm/HeapMemoryUsed",
            },
          ]),
          values: JSON.stringify([]),
          properties: JSON.stringify({}),
          scope: "USER",
          author: userName,
          description: "ResourceManager Heap Memory",
        },
      });
      break;

    case "HBASE":
      baseWidgets.push({
        WidgetInfo: {
          id: "hbase-master-heap",
          widget_name: "HBase Master Heap",
          widget_type: "GRAPH",
          metrics: JSON.stringify([
            {
              name: "hbase_master_heap",
              service_name: "HBASE",
              component_name: "HBASE_MASTER",
              metric_path: "metrics/jvm/HeapMemoryUsed",
            },
          ]),
          values: JSON.stringify([]),
          properties: JSON.stringify({}),
          scope: "USER",
          author: userName,
          description: "HBase Master Heap Memory",
        },
      });
      break;

    default:
      baseWidgets.push({
        WidgetInfo: {
          id: `${serviceName.toLowerCase()}-default-widget`,
          widget_name: `${serviceName} Default Widget`,
          widget_type: "GRAPH",
          metrics: JSON.stringify([
            {
              name: "default_metric",
              service_name: serviceName,
              component_name: "DEFAULT_COMPONENT",
              metric_path: "metrics/jvm/HeapMemoryUsed",
            },
          ]),
          values: JSON.stringify([]),
          properties: JSON.stringify({}),
          scope: "USER",
          author: userName,
          description: `Default ${serviceName} widget`,
        },
      });
  }

  return baseWidgets;
};
