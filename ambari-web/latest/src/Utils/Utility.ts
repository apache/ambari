
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

import _,{  startCase, get, has } from "lodash";
import { detectUserTimezone, parseTimezones } from "./timezone";

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

const serviceMap: any = {
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
};

const componentsMap: any = {
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
      hosts: hosts,
      recommend: recommend,
      recommendations: {
        blueprint: {
          host_groups: blueprint,
        },
        blueprint_cluster_binding: {
          host_groups: blueprintClusterBinding,
        },
      },
      services: services,
    };
    return payload;
  },
};
 
export const isRequestRunning = (status: string) => {
  return ["IN_PROGRESS", "QUEUED", "PENDING"].includes(status);
};

export const normalizeName = (name: string) => {
  if (!name || typeof name !== "string") return "";
  if (get(components, name, "")) return get(components, name, "");
  name = name.toLowerCase();
  const suffixNoSpaces = ["node", "tracker", "manager"];
  const suffixRegExp = new RegExp(`(\\w+)(${suffixNoSpaces.join("|")})`, "gi");
  if (/_/g.test(name)) {
    name = name
      .split("_")
      .map((singleName) => normalizeName(singleName.toUpperCase()))
      .join(" ");
  } else if (suffixRegExp.test(name)) {
    suffixRegExp.lastIndex = 0;
    const matches = suffixRegExp.exec(name);
    if (matches) {
      name =
        startCase(matches[1].toLowerCase()) +
        startCase(matches[2].toLowerCase());
    }
  }
  return startCase(name.toLowerCase());
};
 
export const isFinished = (status: string) => {
  return ["FAILED", "ABORTED", "COMPLETED"].includes(status);
};
 
export const isFailed = (status: string) => {
  return ["FAILED"].includes(status);
};
export const role = (role: string, isServiceRole: boolean) => {
  if (isServiceRole) {
    if (has(serviceMap,role)) {
      return serviceMap[role];
    }
  } else {
    if (has(componentsMap,role)) {
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
      if (item.contains("/") && !isIncludeExcludeFiles) {
        item = item.split("/")[1];
      }
      if (item === "DECOMMISSION,") {
        // ignore text 'DECOMMISSION,'( command came from 'excluded/included'), here get the component name from request_inputs
        var parsedInputs = JSON.parse(request_inputs);
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
}

export const pluralize = (count: number, noun: string, suffix = "s") =>
  `${count} ${noun}${count !== 1 ? suffix : ""}`;

export const getStepData = (state: any, stepName: string, dataKey: string) => {
  const stepData = get(state, `clusterCreationSteps.${stepName}.data`, {});
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
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    return date.toLocaleString();

};
const getHighestPriorityStatus = (statuses: any[]) => {
  const statusOrder = ["critical", "warning", "ok", "unknown", "none"];
  return statuses.reduce((highestPriorityStatus, statusItem) => {
    const status = get(statusItem, 'status', '').toLowerCase();
    return statusOrder.indexOf(status) < statusOrder.indexOf(highestPriorityStatus)
        ? status
        : highestPriorityStatus;
  }, statusOrder[statusOrder.length - 1]);
};

export function sortPropertyLight(arr: any[], path: string, desc: boolean = false) {
  const realPath = (typeof path === "string") ? path.split('.') : [];
  const statusOrder = ["critical", "warning", "ok", "unknown", "none"];
  const sortedArr = arr.sort((a, b) => {
    let aProperty = a;
    let bProperty = b;
    realPath.forEach((key) => {
      aProperty = aProperty[key];
      bProperty = bProperty[key];
    });

    if (path === "statuses") {
      const aStatus = getHighestPriorityStatus(aProperty);
      const bStatus = getHighestPriorityStatus(bProperty);
      return desc
          ? statusOrder.indexOf(bStatus) - statusOrder.indexOf(aStatus)
          : statusOrder.indexOf(aStatus) - statusOrder.indexOf(bStatus);
    } else {
      aProperty = aProperty.toString().toLowerCase();
      bProperty = bProperty.toString().toLowerCase();
      if (aProperty > bProperty) return desc ? -1 : 1;
      if (aProperty < bProperty) return desc ? 1 : -1;
      return 0;
    }
  });
  return sortedArr;
};

export const isInstallable=(stackName:string,componentName:string)=>{
  const notInstallable = stackName === 'HDF' ? ['ACTIVITY_ANALYZER', 'ACTIVITY_EXPLORER'] : [];
  return !notInstallable.includes(componentName);
}

export const isShownOnInstallerAssignMasterPage=(component:string,isMaster:boolean,stackName:string)=> {
  var mastersNotShown = ['MYSQL_SERVER', 'POSTGRESQL_SERVER', 'HIVE_SERVER_INTERACTIVE'];
  return isMaster && isInstallable(stackName,component) && !mastersNotShown.includes(component);
}

export function isShownOnAddServiceAssignMasterPage(component:string,isMaster:boolean,stackName:string,isHaEnabled:boolean) {
  let isVisible = isShownOnInstallerAssignMasterPage(component,isMaster,stackName);
  if (isHaEnabled) {
    isVisible =  isVisible && component !== 'SECONDARY_NAMENODE';
  }
  return isVisible;
}
