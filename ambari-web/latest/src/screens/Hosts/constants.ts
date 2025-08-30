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

import { translate } from "../../Utils/Utility";

export const sortByColIdToKeyMapping = {
  hostname: "Hosts/host_name",
  ip: "Hosts/ip",
  rack: "Hosts/rack_info",
  cores: "Hosts/cpu_count",
  ram: "Hosts/total_mem",
  disk: "metrics/disk/disk_free",
  load: "metrics/load/load_one",
};

export const hostMetricsOption = [
  "Last 1 hour",
  "Last 2 hours",
  "Last 4 hours",
  "Last 12 hours",
  "Last 24 hours",
  "Last 1 week",
  "Last 1 month",
  "Last 1 year",
];

export const actionToStateMapping = {
  start: {
    PENDING: "INSTALLED",
    IN_PROGRESS: "STARTING",
    COMPLETED: "STARTED",
    FAILED: "INSTALLED",
  },
  restart: {
    PENDING: "INSTALLED",
    IN_PROGRESS: "STARTING",
    COMPLETED: "STARTED",
    FAILED: "INSTALLED",
  },
  stop: {
    PENDING: "STARTED",
    IN_PROGRESS: "STOPPING",
    COMPLETED: "INSTALLED",
    FAILED: "STARTED",
  },
};

export const finishStates = ["FAILED", "COMPLETED", "TIMEDOUT", "ABORTED"];
export const componentFinishStates = ["INSTALLED", "STARTED"];
export const maintenanceStates = ["ON", "OFF"];
export const minDiskSpace = 2.0;
export const minDiskSpaceUsrLib = 1.0;

export const restWarningCategories = [
  "repoCategoryWarnings",
  "diskCategoryWarnings",
  "jdkCategoryWarnings",
  "hostCheckWarnings",
  "thpCategoryWarnings",
];

export const restWarningCategoriesToIssuesKeyMap = {
  repoCategoryWarnings: "repositories",
  diskCategoryWarnings: "disk",
  jdkCategoryWarnings: "jdk",
  hostCheckWarnings: "hostNameResolution",
  thpCategoryWarnings: "thp",
};

export const checkHostIssues = {
  packages: {
    displayName: "Package Issues",
    count: 0,
    data: [],
    dataMessage: "The following packages should be uninstalled",
    noDataMessage: "There were no unwanted packages",
  },
  processes: {
    displayName: "Process Issues",
    count: 0,
    data: [],
    dataMessage: "The following processes should not be running",
    noDataMessage: "There were no unwanted processes",
  },
  thp: {
    displayName: "Transparent Huge Pages Issues",
    count: 0,
    data: [],
    dataMessage: "The following THP issues should be fixed",
    noDataMessage: "There were no THP issues",
  },
  jdk: {
    displayName: "JDK Issues",
    count: 0,
    data: [],
    dataMessage: "The following JDK issues should be fixed",
    noDataMessage: "There were no JDK issues",
  },
  disk: {
    displayName: "Disk Issues",
    count: 0,
    data: [],
    dataMessage: "The following disks should be cleaned",
    noDataMessage: "There were no disk space issues",
  },
  repositories: {
    displayName: "Repository Issues ",
    count: 0,
    data: [],
    dataMessage:
      "The following repositories has OS type mis-match with registered hosts",
    noDataMessage:
      "There were no repositories OS type mis-match with registered hosts",
  },
  firewall: {
    displayName: "Firewall Issues",
    count: 0,
    data: [],
    dataMessage: "The following firewalls should be disabled",
    noDataMessage: "There were no firewalls running",
  },
  fileFolders: {
    displayName: "File and Folder Issues",
    count: 0,
    data: [],
    dataMessage: "The following files/folders should be removed",
    noDataMessage: "There were no unwanted files and folders",
  },
  services: {
    displayName: "Service Issues",
    count: 0,
    data: [],
    dataMessage: "The following services should be stopped",
    noDataMessage: "There were no unwanted services",
  },
  users: {
    displayName: "User Issues",
    count: 0,
    data: [],
    dataMessage: "The following users should be removed",
    noDataMessage: "There were no unwanted users",
  },
  misc: {
    displayName: "Misc Issues",
    count: 0,
    data: [],
    dataMessage: "The following issues should be resolved",
    noDataMessage: "There were no issues",
  },
  alternatives: {
    displayName: "Alternatives Issues",
    count: 0,
    data: [],
    dataMessage: "The following alternatives should be removed",
    noDataMessage: "There were no alternative issues",
  },
  reverseLookup: {
    displayName: "Reverse Lookup Issues",
    count: 0,
    data: [],
    dataMessage: "The following hosts have reverse lookup issues",
    noDataMessage: "There were no reverse DNS lookup issues",
  },
  hostNameResolution: {
    displayName: "Hostname Resolution Issues",
    count: 0,
    data: [],
    dataMessage: "The following hosts have hostname resolution issues",
    noDataMessage: "There were no hostname resolution issues",
  },
};

export const categoriesToReportMap = [
  {
    key: "thp",
    label: "installer.step3.hostWarningsPopup.report.thp",
    separator: " ",
  },
  {
    key: "jdk",
    label: "installer.step3.hostWarningsPopup.report.jdk",
    separator: "<br>",
  },
  {
    key: "disk",
    label: "installer.step3.hostWarningsPopup.report.disk",
    separator: "<br>",
  },
  {
    key: "repositories",
    label: "installer.step3.hostWarningsPopup.report.repositories",
    separator: "<br>",
  },
  {
    key: "hostNameResolution",
    label: "installer.step3.hostWarningsPopup.report.hostNameResolution",
    separator: "<br>",
  },
  {
    key: "firewall",
    label: "installer.step3.hostWarningsPopup.report.firewall",
    separator: "<br>",
    mapProperty: "name",
  },
  {
    key: "fileFolders",
    label: "installer.step3.hostWarningsPopup.report.fileFolders",
    separator: " ",
    mapProperty: "name",
  },
  {
    key: "reverseLookup",
    label: "installer.step3.hostWarningsPopup.report.reverseLookup",
    separator: " ",
    mapProperty: "hostsLong",
  },
  {
    key: "packages",
    label: "installer.step3.hostWarningsPopup.report.package",
    separator: " ",
    mapProperty: "name",
  },
  {
    key: "services",
    label: "installer.step3.hostWarningsPopup.report.service",
    separator: " ",
    mapProperty: "name",
  },
  {
    key: "users",
    label: "installer.step3.hostWarningsPopup.report.user",
    separator: " ",
    mapProperty: "name",
  },
];

// To be moved to internationalization
export const hostCheckReportConstants: any = {
  "installer.step3.hostWarningsPopup.report.header":
    '<p style="font-family: monospace">######################################<br># Host Checks Report<br>#<br># Generated: ',
  "installer.step3.hostWarningsPopup.report.hosts":
    "<br>######################################<br><br>######################################<br># Hosts<br>#<br># A space delimited list of hosts which have issues.<br># Provided so that administrators can easily copy hostnames into scripts, email etc.<br>######################################<br>HOSTS<br>",
  "installer.step3.hostWarningsPopup.report.jdk":
    "<br><br>######################################<br># JDK Check <br>#<br># A newline delimited list of JDK issues.<br>######################################<br>JDK ISSUES<br>",
  "installer.step3.hostWarningsPopup.report.disk":
    "<br><br>######################################<br># Disk <br>#<br># A newline delimited list of disk issues.<br>######################################<br>DISK ISSUES<br>",
  "installer.step3.hostWarningsPopup.report.repositories":
    "<br><br>######################################<br># Repositories <br>#<br># A newline delimited list of repositories issues.<br>######################################<br>REPOSITORIES ISSUES<br>",
  "installer.step3.hostWarningsPopup.report.hostNameResolution":
    "<br><br>######################################<br># Hostname Resolution<br>#<br># A newline delimited list of hostname resolution issues.<br>######################################<br>HOSTNAME RESOLUTION ISSUES<br>",
  "installer.step3.hostWarningsPopup.report.thp":
    "<br><br>######################################<br># Transparent Huge Pages(THP) <br>#<br># A space delimited list of hostnames on which Transparent Huge Pages are enabled.<br>######################################<br>THP ISSUES HOSTS<br>",
  "installer.step3.hostWarningsPopup.report.firewall":
    "<br><br>######################################<br># Firewall<br>#<br># A newline delimited list of firewall issues.<br>######################################<br>FIREWALL<br>",
  "installer.step3.hostWarningsPopup.report.fileFolders":
    "<br><br>######################################<br># Files and Folders<br>#<br># A space delimited list of files and folders which should not exist.<br># Provided so that administrators can easily copy paths into scripts, email etc.<br># Example: rm -r /etc/hadoop /etc/hbase<br>######################################<br>FILES AND FOLDERS<br>",
  "installer.step3.hostWarningsPopup.report.reverseLookup":
    "<br><br>######################################<br># Reverse Lookup<br># <br># The hostname was not found in the reverse DNS lookup. This may result in incorrect behavior. <br># Please check the DNS setup and fix the issue.<br>######################################<br>REVERSE LOOKUP<br>",
  "installer.step3.hostWarningsPopup.report.process":
    "<br><br>######################################<br># Processes<br>#<br># A comma separated list of process tuples which should not be running.<br># Provided so that administrators can easily copy paths into scripts, email etc.<br>######################################<br>PROCESSES<br>",
  "installer.step3.hostWarningsPopup.report.package":
    "<br><br>######################################<br># Packages<br>#<br># A space delimited list of software packages which should be uninstalled.<br># Provided so that administrators can easily copy paths into scripts, email etc.<br># Example: yum remove hadoop-hdfs yarn<br>######################################<br>PACKAGES<br>",
  "installer.step3.hostWarningsPopup.report.service":
    "<br><br>######################################<br># Services<br>#<br># A space delimited list of services which should be up and running.<br># Provided so that administrators can easily copy paths into scripts, email etc.<br># Example: services start ntpd httpd<br>######################################<br>SERVICES<br>",
  "installer.step3.hostWarningsPopup.report.user":
    "<br><br>######################################<br># Users<br>#<br># A space delimited list of users who should not exist.<br># Provided so that administrators can easily copy paths into scripts, email etc.<br># Example: userdel hdfs<br>######################################<br>USERS<br>",
};

// These fields cannot be selected again if already selected in the filter
export const nonRepeatableHostFieldOptions = ["cpu", "memoryFormatted"];

export const healthClassesForHostFilter = [
  {
    label: translate("hosts.host.healthStatusCategory.green"),
    value: "HEALTHY",
    name: "healthClass",
  },
  {
    label: translate("hosts.host.healthStatusCategory.red"),
    value: "UNHEALTHY",
    name: "healthClass",
  },
  {
    label: translate("hosts.host.healthStatusCategory.orange"),
    value: "ALERT",
    name: "healthClass",
  },
  {
    label: translate("hosts.host.healthStatusCategory.yellow"),
    value: "UNKNOWN",
    name: "healthClass",
  },
  {
    label: translate("hosts.host.alerts.label"),
    value: [">0", ">0"],
    name: "criticalWarningAlertsCount",
  },
  {
    label: translate("common.restart"),
    value: "true",
    name: "componentsWithStaleConfigsCount",
  },
  {
    label: translate("common.passive_state"),
    value: [
      "ON",
      "IMPLIED_FROM_HOST",
      "IMPLIED_FROM_SERVICE",
      "IMPLIED_FROM_SERVICE_AND_HOST",
    ],
    name: "componentsInPassiveStateCount",
  },
];

export const hostFilterProperties = [
  {
    name: "hostName",
    key: "Hosts/host_name",
    type: "MATCH",
  },
  {
    name: "ip",
    key: "Hosts/ip",
    type: "MATCH",
  },
  {
    name: "cpu",
    key: "Hosts/cpu_count",
    type: "EQUAL",
    valueType: "number",
  },
  {
    name: "memoryFormatted",
    key: "Hosts/total_mem",
    type: "EQUAL",
    valueType: "ambari-bandwidth",
  },
  {
    name: "loadAvg",
    key: "metrics/load/load_one",
    type: "EQUAL",
  },
  {
    name: "rack",
    key: "Hosts/rack_info",
    type: "MATCH",
  },
  {
    name: "hostComponents",
    key: "host_components/HostRoles/component_name",
    type: "EQUAL",
  },
  {
    name: "services",
    key: "host_components/HostRoles/service_name",
    type: "MATCH",
  },
  {
    name: "state",
    key: "host_components/HostRoles/state",
    type: "MATCH",
  },
  {
    name: "healthClass",
    key: "Hosts/host_status",
    type: "EQUAL",
  },
  {
    name: "criticalWarningAlertsCount",
    key: "(alerts_summary/CRITICAL{0}|alerts_summary/WARNING{1})",
    type: "CUSTOM",
  },
  {
    name: "componentsWithStaleConfigsCount",
    key: "host_components/HostRoles/stale_configs",
    type: "EQUAL",
  },
  {
    name: "componentsInPassiveStateCount",
    key: "host_components/HostRoles/maintenance_state",
    type: "MULTIPLE",
  },
  {
    name: "selected",
    key: "Hosts/host_name",
    type: "MULTIPLE",
  },
  {
    name: "version",
    key: "stack_versions/repository_versions/RepositoryVersions/display_name",
    type: "EQUAL",
  },
  {
    name: "versionState",
    key: "stack_versions/HostStackVersions/state",
    type: "EQUAL",
  },
  {
    name: "hostStackVersion",
    key: "stack_versions",
    type: "EQUAL",
  },
  {
    name: "componentState",
    key: [
      "(host_components/HostRoles/component_name={0})",
      "(host_components/HostRoles/component_name={0}&host_components/HostRoles/state={1})",
      "(host_components/HostRoles/component_name={0}&host_components/HostRoles/desired_admin_state={1})",
      "(host_components/HostRoles/component_name={0}&host_components/HostRoles/maintenance_state={1})",
    ],
    type: "COMBO",
  },
];

export const serviceNameToModelKeyMap: any = {
  HDFS: "hdfs",
  YARN: "yarn",
  MAPREDUCE2: "mapreduce2",
  TEZ: "tez",
  HIVE: "hive",
  HBASE: "hbase",
  ZOOKEEPER: "zk",
  AMBARI_METRICS: "ambari_metrics",
  RANGER: "ranger",
  RANGER_KMS: "ranger_kms",
  KERBEROS: "kerberos",
  SPARK3: "spark3",
  SSM: "ssm",
  TRINO: "trino",
};
