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

import { useContext, useState, useCallback, useRef } from "react";
import { QuicklinksApi } from "../api/quicklinksApi";
import ConfigsApi from "../api/configsApi";
import { ServiceApi } from "../api/serviceApi";
import { AppContext } from "../store/context";
import { ServiceContext } from "../store/ServiceContext";
import { find, get } from "lodash";
import {
  setProtocol,
  getServiceProtocol,
} from "../Utils/sslProtocolUtils";
import { useAuth } from "./useAuth";
import {
  createPublicHostNameMap,
  QuicklinkConfiguration,
  resolveQuicklinkConfigPlaceholders,
  substituteQuicklinkTemplate,
} from "../Utils/quicklinks";

type QuicklinkHostComponent = {
  HostRoles?: { host_name?: string; state?: string };
};

type QuicklinkComponentInfo = {
  ServiceComponentInfo?: { component_name?: string };
  host_components?: QuicklinkHostComponent[];
};

/**
 * Lazy-loading quicklinks hook following Ember.js pattern
 * Only loads quicklinks when component is mounted and service is selected
 * Completely decoupled from service updaters
 *
 * Flow:
 * 1. Call Quicklinks API for the service
 * 2. Call services_config_versions API to fetch config
 * 3. Perform the overrides and return the quicklinks
 */
export const useLazyQuicklinks = (serviceName: string) => {
  const { clusterName, cluster, isClusterInstalled } = useContext(AppContext);
  const { polledHostComponentsData, allServiceModels } = useContext(ServiceContext);
  const { user } = useAuth();

  const [quicklinks, setQuicklinks] = useState<any>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const currentServiceRef = useRef<string>(serviceName);
  const publicHostNamesRef = useRef(new Map<string, string>());

  // Memoized hostname extraction
  const memoizedGetHostName = useCallback(
    (() => {
      const cache = new Map();
      return (url: string) => {
        if (cache.has(url)) {
          return cache.get(url);
        }
        try {
          const hostname = new URL(url).hostname;
          cache.set(url, hostname);
          return hostname;
        } catch (error) {
          return "";
        }
      };
    })(),
    []
  );

  // Extract port using regex (following Ember.js setPort pattern)
  const extractPortWithRegex = useCallback(
    (portConfigs: any, protocol: string, configProperties: any) => {
      if (!portConfigs) {
        return "";
      }

      const defaultPort = portConfigs[`${protocol}_default_port`];
      const portProperty = portConfigs[`${protocol}_property`];
      const site = configProperties.find(
        (conf: any) => conf.type === portConfigs.site
      );
      const propertyValue = site?.properties?.[portProperty];


      if (!propertyValue) {
        // Special handling for Pinot - try alternative property names
        if (serviceName.toUpperCase() === "PINOT" && protocol === "https") {
          // Try alternative HTTPS property name
          const alternativeHttpsProperty = "controller.access.protocols.https.port";
          const alternativePropertyValue = site?.properties?.[alternativeHttpsProperty];
                  
          if (alternativePropertyValue) {
            return alternativePropertyValue;
          }
        }
        
        return defaultPort;
      }

      let regexValue = portConfigs.regex;
      if (protocol === "https") {
        const httpsRegex = portConfigs.https_regex;
        if (httpsRegex) {
          regexValue = httpsRegex;
        }
      }

      if (regexValue) {
        regexValue = regexValue.trim();
        try {
          const re = new RegExp(regexValue);
          const portValue = propertyValue.match(re);
          const extractedPort = portValue?.[1] || defaultPort;
          
          
          return extractedPort;
        } catch (err) {
          return defaultPort;
        }
      } else {
        return propertyValue;
      }
    },
    [serviceName]
  );

  // Extract host from URI (following Ember.js parseHostFromUri pattern)
  const parseHostFromUri = useCallback((uri: string) => {
    if (uri) {
      const match = uri.match(/:\/\/([^/:]+)/i);
      return match != null && match.length === 2 ? match[1] : uri;
    }
    return null;
  }, []);

  // URL reconstruction with sophisticated SSL support following Ember.js patterns
  const reconstructURL = useCallback(
    (
      link: any,
      configProperties: any,
      hostName?: string,
      protocolConfig?: any
    ) => {
      try {
        let finalUrl = link.url;
        let host = hostName || "";
        let port = "";

        // Debug logging for Pinot

        // Determine protocol using Ember.js setProtocol logic
        let protocol = "http";
        if (link.protocol) {
          // Link-specific protocol configuration
          protocol = setProtocol(configProperties, link.protocol);
        } else if (protocolConfig) {
          // Service-level protocol configuration
          protocol = setProtocol(configProperties, protocolConfig);
        } else {
          // Use service-specific protocol logic
          protocol = getServiceProtocol(serviceName, configProperties);
        }

        // Handle host configuration from link.host if no hostName provided
        if (!host && link.host) {
          const hostProperty = link.host[`${protocol}_property`];
          const site = configProperties.find(
            (conf: any) => conf.type === link.host.site
          );
          const hostUri = site?.properties?.[hostProperty];
          if (hostUri) {
            host = parseHostFromUri(hostUri) || "";
          }
        }

        if (serviceName.toUpperCase() === "MAPREDUCE2" && link.port) {
          const configuredHostProperty = link.port[`${protocol}_config`];
          const configuredHostSite = configProperties.find(
            (conf: QuicklinkConfiguration) => conf.type === link.port.site
          );
          const configuredHostAndPort =
            configuredHostSite?.properties?.[configuredHostProperty];
          const configuredHost = configuredHostAndPort?.match(
            /([\w\d.-]+):(\d+)/
          )?.[1];
          if (configuredHost) {
            host = configuredHost;
          }
        }

        host = publicHostNamesRef.current.get(host) || host;

        // Handle port configuration with regex
        if (link.port) {
          port = extractPortWithRegex(link.port, protocol, configProperties);
        }

        finalUrl = substituteQuicklinkTemplate(
          finalUrl,
          protocol,
          host,
          port,
          user?.user_name || "",
          link.requires_user_name === "true"
        );
        return resolveQuicklinkConfigPlaceholders(finalUrl, configProperties);
      } catch (error) {
        return link.url;
      }
    },
    [extractPortWithRegex, parseHostFromUri, serviceName, user?.user_name]
  );

  // Check if any quicklinks have overridden host configuration (following Ember.js hasOverriddenHost pattern)
  const hasOverriddenHost = useCallback(
    (quicklinksData: any) => {
      if (!quicklinksData?.items || quicklinksData.items.length === 0) {
        return false;
      }

      // Check if any link has a host configuration
      const hasOverride = quicklinksData.items.some((quicklinkItem: any) => {
        const quicklinkConfig =
          quicklinkItem.QuickLinkInfo?.quicklink_data?.QuickLinksConfiguration;
        if (!quicklinkConfig || !quicklinkConfig.configuration?.links) {
          return false;
        }

        const links = quicklinkConfig.configuration.links;

        return links.some((link: any) => link.host);
      });

      return hasOverride;
    },
    [serviceName]
  );

  // Transform flat links to host structure (following Ember.js logic)
  const transformQuicklinksToHostStructure = useCallback(
    (flatLinks: any[], quicklinksData: any) => {
      if (!flatLinks || flatLinks.length === 0) {
        return [];
      }

      // Get unique component names and host names from links
      const componentNames = [
        ...new Set(flatLinks.map((link) => link.componentName)),
      ];
      const isMultipleComponentsInLinks = componentNames.length > 1;
      const hostNames = [...new Set(flatLinks.map((link) => link.hostName))];
      const hasOverriddenHostConfig = hasOverriddenHost(quicklinksData);

      // Following Ember.js logic: if (hosts.length === 1 || isMultipleComponentsInLinks || this.hasOverriddenHost())
      // Use service-level quicklinks (setSingleHostLinks pattern) when:
      // 1. Only one host has all the components
      // 2. Multiple different components are in the links (like Hive with HIVE_SERVER and HIVE_METASTORE)
      // 3. Host is overridden in config (hasOverriddenHost)
      // 4. Special case: HIVE service always uses service-level quicklinks (based on Ember.js behavior)
      if (
        hostNames.length === 1 ||
        isMultipleComponentsInLinks ||
        hasOverriddenHostConfig ||
        serviceName.toUpperCase() === "HIVE"
      ) {
        // Use service-level quicklinks (setSingleHostLinks pattern)
        // Remove duplicates by label to avoid showing same link multiple times
        const serviceLinks: any[] = [];
        const processedLabels = new Set();

        flatLinks.forEach((link) => {
          if (!processedLabels.has(link.label)) {
            processedLabels.add(link.label);
            serviceLinks.push({
              label: link.label,
              url: link.url,
              componentName: link.componentName,
              haState: link.haState, // Preserve HA state for service-level quicklinks
            });
          }
        });

        // For service-level quicklinks, group by HA state if available
        const linksByHAState = new Map<string, any[]>();
        
        serviceLinks.forEach(link => {
          const haStateKey = link.haState || "default";
          if (!linksByHAState.has(haStateKey)) {
            linksByHAState.set(haStateKey, []);
          }
          linksByHAState.get(haStateKey)!.push(link);
        });
        
        // If we have HA states, create separate groups for Active/Standby
        if (linksByHAState.size > 1 && linksByHAState.has("Active")) {
          const result: any[] = [];
          
          // Add Active group first
          if (linksByHAState.has("Active")) {
            result.push({
              hostName: "", // Service-level grouping without header text
              links: linksByHAState.get("Active"),
              haState: "Active",
            });
          }
          
          // Add Standby group
          if (linksByHAState.has("Standby")) {
            result.push({
              hostName: "", // Service-level grouping without header text
              links: linksByHAState.get("Standby"),
              haState: "Standby",
            });
          }
          
          // Add any other groups
          linksByHAState.forEach((links, haState) => {
            if (haState !== "Active" && haState !== "Standby" && haState !== "default") {
              result.push({
                hostName: "", // Service-level grouping without header text
                links: links,
                haState: haState,
              });
            }
          });
          
          // Add default group (no HA state) last
          if (linksByHAState.has("default")) {
            result.push({
              hostName: "", // Service-level grouping without header text
              links: linksByHAState.get("default"),
            });
          }
          
          return result;
        } else {
          // No HA states or only one group, return as single group
          return [
            {
              hostName: "", // Service-level grouping without header text (matching Ember.js)
              links: serviceLinks,
              haState: serviceLinks.length > 0 ? serviceLinks[0].haState : undefined,
            },
          ];
        }
      }

      // Default behavior for multiple hosts with single component type - group by host
      const linksByHost = new Map<string, any[]>();

      for (const link of flatLinks) {
        const hostName = link.hostName;
        if (!hostName) continue;

        if (!linksByHost.has(hostName)) {
          linksByHost.set(hostName, []);
        }
        linksByHost.get(hostName)!.push(link);
      }

      return Array.from(linksByHost.entries()).map(([hostName, links]) => {
        // Extract haState from the first link since all links for a host should have the same haState
        const haState = links.length > 0 ? links[0].haState : undefined;
        
        return {
          hostName,
          links,
          haState, // Add haState at the host level
        };
      });
    },
    [serviceName, hasOverriddenHost]
  );

  // Check if NameNode HA is enabled (following useHDFSConfigUpdater pattern)
  const checkHAEnabledForNameNode = useCallback(async () => {
    if (serviceName.toUpperCase() !== "HDFS") {
      return false;
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const secondaryNn = find(response.data.items, (item) => {
        return (
          get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
          get(item, "ServiceComponentInfo.component_name") ===
            "SECONDARY_NAMENODE"
        );
      });

      const nameNode = find(response.data.items, (item) => {
        return (
          get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
          get(item, "ServiceComponentInfo.component_name") === "NAMENODE"
        );
      });

      // HA is enabled if no Secondary NameNode and multiple NameNodes
      const isHAEnabled =
        (!secondaryNn || secondaryNn.host_components.length === 0) &&
        nameNode &&
        nameNode.host_components.length > 1;

      return isHAEnabled;
    } catch (error) {
      return false;
    }
  }, [serviceName, clusterName]);

  // Check if YARN ResourceManager HA is enabled (following useYarnConfigUpdater pattern)
  const checkHAEnabledForResourceManager = useCallback(async () => {
    if (serviceName.toUpperCase() !== "YARN") {
      return false;
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const resourceManager = find(response.data.items, (item) => {
        return (
          get(item, "ServiceComponentInfo.service_name") === "YARN" &&
          get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
        );
      });

      // HA is enabled if multiple ResourceManagers
      const isHAEnabled =
        resourceManager &&
        resourceManager.host_components.length > 1;

      return isHAEnabled;
    } catch (error) {
      return false;
    }
  }, [serviceName, clusterName]);

  // Get NameNode HA status information
  const getNameNodeHAStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "HDFS") {
      return {
        activeNameNodes: [] as any[],
        standbyNameNodes: [] as any[],
        nonActiveStandbyNameNodes: [] as any[],
      };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state,host_components/metrics/dfs/FSNamesystem/HAState&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const nameNode = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
          get(item, "ServiceComponentInfo.component_name") === "NAMENODE"
      );

      let activeNameNodes: any[] = [];
      let standbyNameNodes: any[] = [];
      let nonActiveStandbyNameNodes: any[] = [];

      if (nameNode && nameNode.host_components) {
        nameNode.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "NAMENODE",
            hostName: get(hostComponent, "HostRoles.host_name"),
            haStatus: get(hostComponent, "metrics.dfs.FSNamesystem.HAState"),
            state: get(hostComponent, "HostRoles.state"),
          };

          if (
            hostComponentData.state === "STARTED" &&
            hostComponentData.haStatus === "active"
          ) {
            activeNameNodes.push(hostComponentData);
          } else if (
            hostComponentData.state === "STARTED" &&
            hostComponentData.haStatus === "standby"
          ) {
            standbyNameNodes.push(hostComponentData);
          } else {
            nonActiveStandbyNameNodes.push(hostComponentData);
          }
        });
      }

      return { activeNameNodes, standbyNameNodes, nonActiveStandbyNameNodes };
    } catch (error) {
      return {
        activeNameNodes: [] as any[],
        standbyNameNodes: [] as any[],
        nonActiveStandbyNameNodes: [] as any[],
      };
    }
  }, [serviceName, clusterName]);

  // Get HBase Master status information (following useHbaseConfigUpdater pattern)
  const getHBaseMasterStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "HBASE") {
      return {
        activeHbaseMasters: [] as any[],
        standbyHbaseMasters: [] as any[],
        nonActiveStandbyHbaseMasters: [] as any[],
      };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state,host_components/metrics/hbase/master/IsActiveMaster&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const hbaseMaster = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "HBASE" &&
          get(item, "ServiceComponentInfo.component_name") === "HBASE_MASTER"
      );

      let activeHbaseMasters: any[] = [];
      let standbyHbaseMasters: any[] = [];
      let nonActiveStandbyHbaseMasters: any[] = [];

      if (hbaseMaster && hbaseMaster.host_components) {
        hbaseMaster.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "HBASE_MASTER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            isActiveMaster: get(
              hostComponent,
              "metrics.hbase.master.IsActiveMaster"
            ),
            state: get(hostComponent, "HostRoles.state"),
          };

          if (
            hostComponentData.state === "STARTED" &&
            hostComponentData.isActiveMaster === "true"
          ) {
            activeHbaseMasters.push(hostComponentData);
          } else if (
            hostComponentData.state === "STARTED" &&
            hostComponentData.isActiveMaster === "false"
          ) {
            standbyHbaseMasters.push(hostComponentData);
          } else {
            nonActiveStandbyHbaseMasters.push(hostComponentData);
          }
        });
      }

      return {
        activeHbaseMasters,
        standbyHbaseMasters,
        nonActiveStandbyHbaseMasters,
      };
    } catch (error) {
      return {
        activeHbaseMasters: [] as any[],
        standbyHbaseMasters: [] as any[],
        nonActiveStandbyHbaseMasters: [] as any[],
      };
    }
  }, [serviceName, clusterName]);

  // Get YARN ResourceManager status information (following useYarnConfigUpdater pattern)
  const getYarnResourceManagerStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "YARN") {
      return {
        activeResourceManagers: [] as any[],
        standbyResourceManagers: [] as any[],
        nonActiveStandbyResourceManagers: [] as any[],
      };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state,host_components/HostRoles/ha_state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const resourceManager = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "YARN" &&
          get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
      );

      let activeResourceManagers: any[] = [];
      let standbyResourceManagers: any[] = [];
      let nonActiveStandbyResourceManagers: any[] = [];

      if (resourceManager && resourceManager.host_components) {
        resourceManager.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "RESOURCEMANAGER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            haStatus: get(hostComponent, "HostRoles.ha_state"),
            state: get(hostComponent, "HostRoles.state"),
          };
          
          if (
            hostComponentData.state === "STARTED" &&
            hostComponentData.haStatus && 
            hostComponentData.haStatus.toUpperCase() === "ACTIVE"
          ) {
            activeResourceManagers.push(hostComponentData);
          } else if (
            hostComponentData.state === "STARTED" &&
            hostComponentData.haStatus && 
            hostComponentData.haStatus.toUpperCase() === "STANDBY"
          ) {
            standbyResourceManagers.push(hostComponentData);
          } else {
            nonActiveStandbyResourceManagers.push(hostComponentData);
          }
        });
      }

      return {
        activeResourceManagers,
        standbyResourceManagers,
        nonActiveStandbyResourceManagers,
      };
    } catch (error) {
      return {
        activeResourceManagers: [] as any[],
        standbyResourceManagers: [] as any[],
        nonActiveStandbyResourceManagers: [] as any[],
      };
    }
  }, [serviceName, clusterName]);

  // Get MapReduce2 JobHistoryServer status information (following useMapReduce2ConfigUpdater pattern)
  const getMapReduce2JobHistoryServerStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "MAPREDUCE2") {
      return { jobHistoryServers: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const historyServer = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "MAPREDUCE2" &&
          get(item, "ServiceComponentInfo.component_name") === "HISTORYSERVER"
      );

      let jobHistoryServers: any[] = [];

      if (historyServer && historyServer.host_components) {
        historyServer.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "HISTORYSERVER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          jobHistoryServers.push(hostComponentData);
        });
      }

      return { jobHistoryServers };
    } catch (error) {
      return { jobHistoryServers: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Get Hive HiveServer2 status information (following useHiveConfigUpdater pattern)
  const getHiveServer2Status = useCallback(async () => {
    if (serviceName.toUpperCase() !== "HIVE") {
      return { hiveServers: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const hiveServer2 = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "HIVE" &&
          get(item, "ServiceComponentInfo.component_name") === "HIVE_SERVER"
      );

      let hiveServers: any[] = [];

      if (hiveServer2 && hiveServer2.host_components) {
        hiveServer2.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "HIVE_SERVER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          hiveServers.push(hostComponentData);
        });
      }

      return { hiveServers };
    } catch (error) {
      return { hiveServers: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Get Ranger Admin status information (following useRangerConfigUpdater pattern)
  const getRangerAdminStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "RANGER") {
      return { rangerAdmins: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const rangerAdmin = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "RANGER" &&
          get(item, "ServiceComponentInfo.component_name") === "RANGER_ADMIN"
      );

      let rangerAdmins: any[] = [];

      if (rangerAdmin && rangerAdmin.host_components) {
        rangerAdmin.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "RANGER_ADMIN",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          rangerAdmins.push(hostComponentData);
        });
      }

      return { rangerAdmins };
    } catch (error) {
      return { rangerAdmins: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Special Ranger URL handling (following Ember.js pattern with policymgr_external_url)
  const getRangerExternalUrl = useCallback((configProperties: any) => {
    const adminPropertiesConfig = configProperties.find(
      (conf: any) => conf.type === "admin-properties"
    );
    if (
      adminPropertiesConfig &&
      adminPropertiesConfig.properties &&
      adminPropertiesConfig.properties["policymgr_external_url"]
    ) {
      return adminPropertiesConfig.properties["policymgr_external_url"];
    }
    return null;
  }, []);

  // Get Spark3 JobHistoryServer status information (following useSpark3ConfigUpdater pattern)
  const getSpark3JobHistoryServerStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "SPARK3") {
      return { spark3JobHistoryServers: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const spark3HistoryServer = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "SPARK3" &&
          get(item, "ServiceComponentInfo.component_name") ===
            "SPARK3_JOBHISTORYSERVER"
      );

      let spark3JobHistoryServers: any[] = [];

      if (spark3HistoryServer && spark3HistoryServer.host_components) {
        spark3HistoryServer.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "SPARK3_JOBHISTORYSERVER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          spark3JobHistoryServers.push(hostComponentData);
        });
      }

      return { spark3JobHistoryServers };
    } catch (error) {
      return { spark3JobHistoryServers: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Get Trino Coordinator status information (following useTrinoConfigUpdater pattern)
  const getTrinoCoordinatorStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "TRINO") {
      return { trinoCoordinators: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const trinoCoordinator = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "TRINO" &&
          get(item, "ServiceComponentInfo.component_name") ===
            "TRINO_COORDINATOR"
      );

      let trinoCoordinators: any[] = [];

      if (trinoCoordinator && trinoCoordinator.host_components) {
        trinoCoordinator.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "TRINO_COORDINATOR",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          trinoCoordinators.push(hostComponentData);
        });
      }

      return { trinoCoordinators };
    } catch (error) {
      return { trinoCoordinators: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Get SSM Smart Server status information (following Ember.js processSSMServerHosts pattern)
  const getSSMSmartServerStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "SSM") {
      return { smartServers: [] as any[] };
    }

    try {
      // Get basic component info first
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const ssmServer = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "SSM" &&
          get(item, "ServiceComponentInfo.component_name") === "SSM_SERVER"
      );

      let smartServers: any[] = [];

      if (ssmServer && ssmServer.host_components) {
        ssmServer.host_components.forEach((hostComponent: any) => {
          const hostName = get(hostComponent, "HostRoles.host_name");
          const state = get(hostComponent, "HostRoles.state");
          
          // Following Ember.js processSSMServerHosts logic:
          // 1. Check if this host is in activeSSMServers (STARTED components)
          // 2. Check if it has active_ssm_ip from polledHostComponentsData
          let haState = undefined;
          
          if (state === "STARTED") {
            // Look for this host in polledHostComponentsData to get active_ssm_ip
            let isActiveSmartServer = false;
            
            if ((polledHostComponentsData as any)?.items) {
              // Find the SSM component data in polledHostComponentsData
              const ssmPolledData = find((polledHostComponentsData as any).items, (item: any) =>
                get(item, "ServiceComponentInfo.service_name") === "SSM" &&
                get(item, "ServiceComponentInfo.component_name") === "SSM_SERVER"
              );
              
              if (ssmPolledData?.host_components) {
                const hostComponentInPolledData = find(ssmPolledData.host_components, (hc: any) =>
                  get(hc, "HostRoles.host_name") === hostName
                );
                
                if (hostComponentInPolledData) {
                  // Check for active_ssm_ip following Ember.js pattern
                  isActiveSmartServer = !!(
                    hostComponentInPolledData.processes &&
                    hostComponentInPolledData.processes[0] &&
                    hostComponentInPolledData.processes[0].HostComponentProcess &&
                    hostComponentInPolledData.processes[0].HostComponentProcess.active_ssm_ip
                  );
                }
              }
            }
            
            // Following Ember.js logic: if STARTED and has active_ssm_ip = Active, else Standby
            haState = isActiveSmartServer ? "Active" : "Standby";
          }
          
          const hostComponentData = {
            componentName: "SSM_SERVER",
            hostName: hostName,
            state: state,
            haState: haState, // Properly determined based on Ember.js logic
          };

          smartServers.push(hostComponentData);
        });
      }

      return { smartServers };
    } catch (error) {
      return { smartServers: [] as any[] };
    }
  }, [serviceName, clusterName, polledHostComponentsData]);

  // Get Trino Gateway status information (following useTrinoGatewayConfigUpdater pattern)
  const getTrinoGatewayStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "TRINO_GATEWAY") {
      return { trinoGateways: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const trinoGateway = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "TRINO_GATEWAY" &&
          get(item, "ServiceComponentInfo.component_name") === "TRINO_GATEWAY"
      );

      let trinoGateways: any[] = [];

      if (trinoGateway && trinoGateway.host_components) {
        trinoGateway.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "TRINO_GATEWAY",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          trinoGateways.push(hostComponentData);
        });
      }

      return { trinoGateways };
    } catch (error) {
      return { trinoGateways: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Get Kyuubi Server status information (following useKyuubiConfigUpdater pattern)
  const getKyuubiServerStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "KYUUBI") {
      return { kyuubiServers: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );


      const kyuubiServer = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "KYUUBI" &&
          get(item, "ServiceComponentInfo.component_name") === "KYUUBI_SERVER"
      );


      let kyuubiServers: any[] = [];

      if (kyuubiServer && kyuubiServer.host_components) {
        kyuubiServer.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "KYUUBI_SERVER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          kyuubiServers.push(hostComponentData);
        });
      }

      return { kyuubiServers };
    } catch (error) {
      console.error(`[KYUUBI] Error getting Kyuubi status:`, error);
      return { kyuubiServers: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Get Pinot Controller status information (following usePinotConfigUpdater pattern)
  const getPinotControllerStatus = useCallback(async () => {
    if (serviceName.toUpperCase() !== "PINOT") {
      return { pinotControllers: [] as any[] };
    }

    try {
      const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
      const response =
        await ServiceApi.getAllServiceComponents(
          clusterName,
          fields
        );

      const pinotController = find(
        response.data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "PINOT" &&
          get(item, "ServiceComponentInfo.component_name") === "PINOT_CONTROLLER"
      );

      let pinotControllers: any[] = [];

      if (pinotController && pinotController.host_components) {
        pinotController.host_components.forEach((hostComponent: any) => {
          const hostComponentData = {
            componentName: "PINOT_CONTROLLER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };

          pinotControllers.push(hostComponentData);
        });
      }

      return { pinotControllers };
    } catch (error) {
      console.error(`[PINOT] Error getting Pinot Controller status:`, error);
      return { pinotControllers: [] as any[] };
    }
  }, [serviceName, clusterName]);

  // Process quicklinks with config overrides and HA support
  const processQuicklinksWithConfig = useCallback(
    async (quicklinksData: any, configData: any) => {
      if (!quicklinksData?.items || quicklinksData.items.length === 0) {
        return [];
      }

      const processedLinks: any[] = [];
      publicHostNamesRef.current = new Map();

      // Get component information for all services (following Ember.js isRelatedComponentInstalled pattern)
      let allComponentInfo: QuicklinkComponentInfo[] | null = null;
      try {
        const componentFields = `ServiceComponentInfo/service_name,ServiceComponentInfo/component_name,ServiceComponentInfo/total_count,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
        const componentResponse =
          await ServiceApi.getAllServiceComponents(
            clusterName,
            componentFields
          );
        allComponentInfo = componentResponse.data.items;
      } catch (error) {
        // Will fallback to original logic if this fails
      }

      if (allComponentInfo) {
        const hostNames = [
          ...new Set<string>(
            allComponentInfo.flatMap((component) =>
              (component.host_components || [])
                .map((hostComponent) =>
                  get(hostComponent, "HostRoles.host_name")
                )
                .filter((hostName): hostName is string => Boolean(hostName))
            )
          ),
        ];
        if (hostNames.length > 0) {
          const publicHosts = await QuicklinksApi.getPublicHostNames(
            clusterName,
            hostNames
          );
          publicHostNamesRef.current = createPublicHostNameMap(
            publicHosts.items || []
          );
        }
      }

      // Check service-specific HA status
      let isHDFSHAEnabled = false;
      let isYARNHAEnabled = false;
      let hdfsHAStatus = {
        activeNameNodes: [] as any[],
        standbyNameNodes: [] as any[],
        nonActiveStandbyNameNodes: [] as any[],
      };
      let hbaseStatus = {
        activeHbaseMasters: [] as any[],
        standbyHbaseMasters: [] as any[],
        nonActiveStandbyHbaseMasters: [] as any[],
      };
      let yarnStatus = {
        activeResourceManagers: [] as any[],
        standbyResourceManagers: [] as any[],
        nonActiveStandbyResourceManagers: [] as any[],
      };
      let mapReduce2Status = { jobHistoryServers: [] as any[] };
      let hiveStatus = { hiveServers: [] as any[] };
      let rangerStatus = { rangerAdmins: [] as any[] };
      let spark3Status = { spark3JobHistoryServers: [] as any[] };
      let trinoStatus = { trinoCoordinators: [] as any[] };
      let ssmStatus = { smartServers: [] as any[] };
      let trinoGatewayStatus = { trinoGateways: [] as any[] };
      let kyuubiStatus = { kyuubiServers: [] as any[] };
      let pinotStatus = { pinotControllers: [] as any[] };

      if (serviceName.toUpperCase() === "HDFS") {
        isHDFSHAEnabled = await checkHAEnabledForNameNode();
        if (isHDFSHAEnabled) {
          hdfsHAStatus = await getNameNodeHAStatus();
        }
      } else if (serviceName.toUpperCase() === "HBASE") {
        hbaseStatus = await getHBaseMasterStatus();
      } else if (serviceName.toUpperCase() === "YARN") {
        isYARNHAEnabled = await checkHAEnabledForResourceManager();
        yarnStatus = await getYarnResourceManagerStatus();
      } else if (serviceName.toUpperCase() === "MAPREDUCE2") {
        mapReduce2Status = await getMapReduce2JobHistoryServerStatus();
      } else if (serviceName.toUpperCase() === "HIVE") {
        hiveStatus = await getHiveServer2Status();
      } else if (serviceName.toUpperCase() === "RANGER") {
        rangerStatus = await getRangerAdminStatus();
      } else if (serviceName.toUpperCase() === "SPARK3") {
        spark3Status = await getSpark3JobHistoryServerStatus();
      } else if (serviceName.toUpperCase() === "TRINO") {
        trinoStatus = await getTrinoCoordinatorStatus();
      } else if (serviceName.toUpperCase() === "SSM") {
        ssmStatus = await getSSMSmartServerStatus();
      } else if (serviceName.toUpperCase() === "TRINO_GATEWAY") {
        trinoGatewayStatus = await getTrinoGatewayStatus();
      } else if (serviceName.toUpperCase() === "KYUUBI") {
        kyuubiStatus = await getKyuubiServerStatus();
      } else if (serviceName.toUpperCase() === "PINOT") {
        pinotStatus = await getPinotControllerStatus();
      }

      quicklinksData.items.forEach((quicklinkItem: any) => {
        const quicklinkConfig =
          quicklinkItem.QuickLinkInfo?.quicklink_data?.QuickLinksConfiguration;

        if (!quicklinkConfig || !quicklinkConfig.configuration?.links) {
          return;
        }

        const links = quicklinkConfig.configuration.links;

        const configurations =
          configData?.items?.[configData.items.length - 1]?.configurations ||
          [];
        const protocolConfig = quicklinkConfig.configuration?.protocol;

        // Debug logging for Pinot configurations
        if (serviceName.toUpperCase() === "PINOT") {
          const pinotCommonConf = configurations.find((c: any) => c.type === "pinot-common-conf");
          if (pinotCommonConf) {
          }
        }

        // For HDFS HA, process links for each NameNode (with federation support)
        if (serviceName.toUpperCase() === "HDFS" && isHDFSHAEnabled) {
          // Process Active NameNodes
          hdfsHAStatus.activeNameNodes.forEach((nameNode: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              // Get namespace information for federation support
              let namespace = undefined;
              const serviceModel = allServiceModels?.["hdfs"];
              if (serviceModel?.federationNamespaces && serviceModel.federationNamespaces.length > 1) {
                // Find which namespace this nameNode belongs to
                const namespaceForHost = serviceModel.federationNamespaces.find((ns: any) => 
                  ns.hosts && ns.hosts.includes(nameNode.hostName)
                );
                namespace = namespaceForHost?.name;
              }

              const finalUrl = reconstructURL(
                link,
                configurations,
                nameNode.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: nameNode.hostName,
                componentName: link.component_name,
                haState: "Active",
                namespace: namespace, // Add namespace for federation grouping
              });
            });
          });

          // Process Standby NameNodes
          hdfsHAStatus.standbyNameNodes.forEach((nameNode: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              // Get namespace information for federation support
              let namespace = undefined;
              const serviceModel = allServiceModels?.["hdfs"];
              if (serviceModel?.federationNamespaces && serviceModel.federationNamespaces.length > 1) {
                // Find which namespace this nameNode belongs to
                const namespaceForHost = serviceModel.federationNamespaces.find((ns: any) => 
                  ns.hosts && ns.hosts.includes(nameNode.hostName)
                );
                namespace = namespaceForHost?.name;
              }

              const finalUrl = reconstructURL(
                link,
                configurations,
                nameNode.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: nameNode.hostName,
                componentName: link.component_name,
                haState: "Standby",
                namespace: namespace, // Add namespace for federation grouping
              });
            });
          });

          // Process other NameNodes
          hdfsHAStatus.nonActiveStandbyNameNodes.forEach((nameNode: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              // Get namespace information for federation support
              let namespace = undefined;
              const serviceModel = allServiceModels?.["hdfs"];
              if (serviceModel?.federationNamespaces && serviceModel.federationNamespaces.length > 1) {
                // Find which namespace this nameNode belongs to
                const namespaceForHost = serviceModel.federationNamespaces.find((ns: any) => 
                  ns.hosts && ns.hosts.includes(nameNode.hostName)
                );
                namespace = namespaceForHost?.name;
              }

              const finalUrl = reconstructURL(
                link,
                configurations,
                nameNode.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: nameNode.hostName,
                componentName: link.component_name,
                haState:
                  nameNode.haStatus === "observer" ? "Observer" : undefined,
                namespace: namespace, // Add namespace for federation grouping
              });
            });
          });
        } else if (serviceName.toUpperCase() === "HBASE") {
          // Process HBase Masters (following useHbaseConfigUpdater pattern)

          // Process Active HBase Masters
          hbaseStatus.activeHbaseMasters.forEach((hbaseMaster: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                hbaseMaster.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: hbaseMaster.hostName,
                componentName: link.component_name,
                haState: "Active",
              });
            });
          });

          // Process Standby HBase Masters
          hbaseStatus.standbyHbaseMasters.forEach((hbaseMaster: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                hbaseMaster.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: hbaseMaster.hostName,
                componentName: link.component_name,
                haState: "Standby",
              });
            });
          });

          // Process other HBase Masters
          hbaseStatus.nonActiveStandbyHbaseMasters.forEach(
            (hbaseMaster: any) => {
              links.forEach((link: any) => {
                if (link.removed || !link.visible) return;

                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  hbaseMaster.hostName,
                  protocolConfig
                );
                processedLinks.push({
                  label: link.label,
                  url: finalUrl,
                  hostName: hbaseMaster.hostName,
                  componentName: link.component_name,
                });
              });
            }
          );
        } else if (serviceName.toUpperCase() === "YARN" && isYARNHAEnabled) {
          // Process YARN ResourceManagers HA (following useYarnConfigUpdater pattern)

          // Process Active ResourceManagers
          yarnStatus.activeResourceManagers.forEach((resourceManager: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                resourceManager.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: resourceManager.hostName,
                componentName: link.component_name,
                haState: "Active",
              });
            });
          });

          // Process Standby ResourceManagers
          yarnStatus.standbyResourceManagers.forEach((resourceManager: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                resourceManager.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: resourceManager.hostName,
                componentName: link.component_name,
                haState: "Standby",
              });
            });
          });

          // Process other ResourceManagers
          yarnStatus.nonActiveStandbyResourceManagers.forEach(
            (resourceManager: any) => {
              links.forEach((link: any) => {
                if (link.removed || !link.visible) return;

                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  resourceManager.hostName,
                  protocolConfig
                );
                processedLinks.push({
                  label: link.label,
                  url: finalUrl,
                  hostName: resourceManager.hostName,
                  componentName: link.component_name,
                });
              });
            }
          );
        } else if (serviceName.toUpperCase() === "YARN" && !isYARNHAEnabled) {
          // Process YARN non-HA ResourceManagers (following standard pattern)
          
          // Get ResourceManager hosts for non-HA YARN
          const resourceManagerHosts: any[] = [];
          if (allComponentInfo) {
            const resourceManagerComponent = find(allComponentInfo, (item) => {
              return (
                get(item, "ServiceComponentInfo.service_name") === "YARN" &&
                get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
              );
            });
            
            if (resourceManagerComponent && resourceManagerComponent.host_components) {
              resourceManagerComponent.host_components.forEach((hostComponent: any) => {
                resourceManagerHosts.push({
                  componentName: "RESOURCEMANAGER",
                  hostName: get(hostComponent, "HostRoles.host_name"),
                  state: get(hostComponent, "HostRoles.state"),
                });
              });
            }
          }

          resourceManagerHosts.forEach((resourceManagerHost: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                resourceManagerHost.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: resourceManagerHost.hostName,
                componentName: link.component_name,
              });
            });
          });
        } else if (serviceName.toUpperCase() === "MAPREDUCE2") {
          // Process MapReduce2 JobHistoryServers (following useMapReduce2ConfigUpdater pattern)

          mapReduce2Status.jobHistoryServers.forEach(
            (jobHistoryServer: any) => {
              links.forEach((link: any) => {
                if (link.removed || !link.visible) return;

                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  jobHistoryServer.hostName,
                  protocolConfig
                );
                processedLinks.push({
                  label: link.label,
                  url: finalUrl,
                  hostName: jobHistoryServer.hostName,
                  componentName: link.component_name,
                });
              });
            }
          );
        } else if (serviceName.toUpperCase() === "HIVE") {
          links.forEach((link: any, _linkIndex: number) => {
            if (link.removed || !link.visible) {
              return;
            }

            // Following Ember.js isRelatedComponentInstalled check - check ALL services, not just HIVE
            let isRelatedComponentInstalled = false;
            if (allComponentInfo) {
              const relatedComponent = find(allComponentInfo, (item) => {
                return (
                  get(item, "ServiceComponentInfo.component_name") ===
                  link.component_name
                );
              });
              isRelatedComponentInstalled = !!relatedComponent;
            }

            if (isRelatedComponentInstalled) {
              // Following Ember.js publicHostName logic
              let publicHostName = null;

              // Determine protocol first
              let protocol = "http";
              if (link.protocol) {
                protocol = setProtocol(configurations, link.protocol);
              } else if (protocolConfig) {
                protocol = setProtocol(configurations, protocolConfig);
              } else {
                protocol = getServiceProtocol(serviceName, configurations);
              }

              if (link.host) {
                // If quicklink overrides hostcomponent host name, get host from config
                const hostProperty = link.host[`${protocol}_property`];
                const site = configurations.find(
                  (conf: any) => conf.type === link.host.site
                );
                publicHostName =
                  site && site.properties
                    ? parseHostFromUri(site.properties[hostProperty])
                    : null;
              } else {
                // Find host for this component from the appropriate service status
                let hostForComponent = null;

                // Check different service statuses based on component name
                if (link.component_name === "HIVE_SERVER") {
                  hostForComponent = hiveStatus.hiveServers.find(
                    (server: any) =>
                      server.componentName === link.component_name
                  );
                } else if (link.component_name === "HIVE_SERVER_INTERACTIVE") {
                  // This component might not exist, but check anyway
                  hostForComponent = hiveStatus.hiveServers.find(
                    (server: any) =>
                      server.componentName === link.component_name
                  );
                }

                publicHostName = hostForComponent
                  ? hostForComponent.hostName
                  : null;
              }

              if (publicHostName) {
                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  publicHostName,
                  protocolConfig
                );
                
                // Get HA state from the host component
                let haState = undefined;
                if (link.component_name === "HIVE_SERVER") {
                  const hiveServer = hiveStatus.hiveServers.find(
                    (server: any) => server.componentName === link.component_name
                  );
                  haState = hiveServer?.haState;
                }
                
                processedLinks.push({
                  label: link.label,
                  url: finalUrl,
                  hostName: publicHostName,
                  componentName: link.component_name,
                  haState: haState,
                });
              }
            }
          });
        } else if (serviceName.toUpperCase() === "RANGER") {
          // Process Ranger with special policymgr_external_url handling (following Ember.js pattern)

          rangerStatus.rangerAdmins.forEach((rangerAdmin: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              // Check for special Ranger external URL first (Ember.js pattern)
              const externalUrl = getRangerExternalUrl(configurations);

              if (externalUrl) {
                // Use external URL directly
                processedLinks.push({
                  label: link.label,
                  url: externalUrl,
                  hostName: rangerAdmin.hostName,
                  componentName: link.component_name,
                });
              } else {
                // Use standard URL construction with sophisticated SSL detection
                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  rangerAdmin.hostName,
                  protocolConfig
                );
                processedLinks.push({
                  label: link.label,
                  url: finalUrl,
                  hostName: rangerAdmin.hostName,
                  componentName: link.component_name,
                });
              }
            });
          });
        } else if (serviceName.toUpperCase() === "SPARK3") {
          // Process Spark3 JobHistoryServers (following useSpark3ConfigUpdater pattern)

          spark3Status.spark3JobHistoryServers.forEach(
            (spark3JobHistoryServer: any) => {
              links.forEach((link: any) => {
                if (link.removed || !link.visible) return;

                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  spark3JobHistoryServer.hostName,
                  protocolConfig
                );
                processedLinks.push({
                  label: link.label,
                  url: finalUrl,
                  hostName: spark3JobHistoryServer.hostName,
                  componentName: link.component_name,
                });
              });
            }
          );
        } else if (serviceName.toUpperCase() === "TRINO") {
          // Process Trino Coordinators (following useTrinoConfigUpdater pattern)

          trinoStatus.trinoCoordinators.forEach((trinoCoordinator: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                trinoCoordinator.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: trinoCoordinator.hostName,
                componentName: link.component_name,
              });
            });
          });
        } else if (serviceName.toUpperCase() === "SSM") {
          // Process SSM Smart Servers with special active_ssm_ip handling (following useSSMConfigUpdater pattern)

          ssmStatus.smartServers.forEach((smartServer: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                smartServer.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: smartServer.hostName,
                componentName: link.component_name,
                haState: smartServer.haState, // Active or Standby based on active_ssm_ip
              });
            });
          });
        } else if (serviceName.toUpperCase() === "HDFS" && !isHDFSHAEnabled) {
          // Process HDFS non-HA NameNodes (following standard pattern)
          
          // Get NameNode hosts for non-HA HDFS
          const nameNodeHosts: any[] = [];
          if (allComponentInfo) {
            const nameNodeComponent = find(allComponentInfo, (item) => {
              return (
                get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
                get(item, "ServiceComponentInfo.component_name") === "NAMENODE"
              );
            });
            
            
            if (nameNodeComponent && nameNodeComponent.host_components) {
              nameNodeComponent.host_components.forEach((hostComponent: any) => {
                nameNodeHosts.push({
                  componentName: "NAMENODE",
                  hostName: get(hostComponent, "HostRoles.host_name"),
                  state: get(hostComponent, "HostRoles.state"),
                });
              });
            }
          }


          nameNodeHosts.forEach((nameNodeHost: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                nameNodeHost.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: nameNodeHost.hostName,
                componentName: link.component_name,
              });
            });
          });
        } else if (serviceName.toUpperCase() === "TRINO_GATEWAY") {
          // Process Trino Gateway (following useTrinoGatewayConfigUpdater pattern)

          trinoGatewayStatus.trinoGateways.forEach((trinoGateway: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                trinoGateway.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: trinoGateway.hostName,
                componentName: link.component_name,
              });
            });
          });
        } else if (serviceName.toUpperCase() === "KYUUBI") {
          // Process Kyuubi Servers (following useKyuubiConfigUpdater pattern)

          kyuubiStatus.kyuubiServers.forEach((kyuubiServer: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;

              const finalUrl = reconstructURL(
                link,
                configurations,
                kyuubiServer.hostName,
                protocolConfig
              );
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: kyuubiServer.hostName,
                componentName: link.component_name,
              });
            });
          });
        } else if (serviceName.toUpperCase() === "PINOT") {
          // Process Pinot Controllers (following usePinotConfigUpdater pattern)

          pinotStatus.pinotControllers.forEach((pinotController: any) => {
            links.forEach((link: any) => {
              if (link.removed || !link.visible) return;


              const finalUrl = reconstructURL(
                link,
                configurations,
                pinotController.hostName,
                protocolConfig
              );
              
              
              processedLinks.push({
                label: link.label,
                url: finalUrl,
                hostName: pinotController.hostName,
                componentName: link.component_name,
              });
            });
          });
        } else {
          links.forEach((link: any) => {
            if (link.removed || !link.visible) {
              return;
            }

            try {

              const relatedComponent = allComponentInfo
                ? find(
                    allComponentInfo,
                    (item) =>
                      get(item, "ServiceComponentInfo.component_name") ===
                      link.component_name
                  )
                : null;
              const componentHosts = (relatedComponent?.host_components || [])
                .filter(
                  (hostComponent) =>
                    serviceName.toUpperCase() !== "OOZIE" ||
                    get(hostComponent, "HostRoles.state") === "STARTED"
                )
                .map((hostComponent) =>
                  get(hostComponent, "HostRoles.host_name")
                );
              const hostsToRender =
                link.host || !allComponentInfo ? [undefined] : componentHosts;

              hostsToRender.forEach((componentHost: string | undefined) => {
                const finalUrl = reconstructURL(
                  link,
                  configurations,
                  componentHost,
                  protocolConfig
                );
                const hostName = memoizedGetHostName(finalUrl);

                if (finalUrl && hostName) {
                  processedLinks.push({
                    label: link.label,
                    url: finalUrl,
                    hostName,
                    componentName: link.component_name,
                  });
                }
              });
            } catch (error) {
              if (serviceName.toUpperCase() === "PINOT") {
                console.error("[PINOT DEBUG] Error processing link:", error);
              }
            }
          });
        }
      });

      return processedLinks.map((link) => ({
        ...link,
        hostName: publicHostNamesRef.current.get(link.hostName) || link.hostName,
      }));
    },
    [
      serviceName,
      reconstructURL,
      memoizedGetHostName,
      checkHAEnabledForNameNode,
      checkHAEnabledForResourceManager,
      getNameNodeHAStatus,
      getHBaseMasterStatus,
      getYarnResourceManagerStatus,
      getMapReduce2JobHistoryServerStatus,
      getHiveServer2Status,
      getRangerAdminStatus,
      getRangerExternalUrl,
      getSpark3JobHistoryServerStatus,
      getTrinoCoordinatorStatus,
      getSSMSmartServerStatus,
      getTrinoGatewayStatus,
      getKyuubiServerStatus,
      getPinotControllerStatus,
    ]
  );

  // Main quicklinks loading function
  const loadQuicklinks = useCallback(async () => {
    if(isClusterInstalled){
    // Update the ref to track current service
    currentServiceRef.current = serviceName;
    const currentServiceName = serviceName; // Capture current service name
    
    setIsLoading(true);
    setError(null);
    setQuicklinks([]); // Clear previous quicklinks immediately when service changes

    try {
      // Get stack version from cluster
      const stackName = get(cluster, "stack");
      const stackVersion = get(cluster, "versionNum");

      // Step 1: Call Quicklinks API for the service
      const quicklinksData = await QuicklinksApi.getQuicklinks(
        stackVersion,
        stackName,
        currentServiceName
      );

      // Check if service has changed during async operation using ref
      if (currentServiceRef.current !== currentServiceName) {
        return; // Abort if service has changed
      }

      // Step 2: Call services_config_versions API to fetch config
      const configData = await ConfigsApi.getConfigValues(
        clusterName,
        currentServiceName
      );

      // Check if service has changed during async operation using ref
      if (currentServiceRef.current !== currentServiceName) {
        return; // Abort if service has changed
      }

      // Step 3: Perform the overrides and return the quicklinks
      const processedLinks = await processQuicklinksWithConfig(
        quicklinksData.data,
        configData
      );

      // Final check: only update state if service hasn't changed using ref
      if (currentServiceRef.current !== currentServiceName) {
        return; // Abort if service has changed
      }

      if (processedLinks.length === 0) {
        setQuicklinks([]);
        return;
      }

      // Transform to host structure
      const transformedQuicklinks = transformQuicklinksToHostStructure(
        processedLinks,
        quicklinksData.data
      );
      
      // Final check before setting state using ref
      if (currentServiceRef.current === currentServiceName) {
        setQuicklinks(transformedQuicklinks);
      }
    } catch (error) {
      // Only set error if service hasn't changed using ref
      if (currentServiceRef.current === currentServiceName) {
        setError(`Failed to load quicklinks for ${currentServiceName}`);
      }
    } finally {
      // Only set loading to false if service hasn't changed using ref
      if (currentServiceRef.current === currentServiceName) {
        setIsLoading(false);
      }
    }
  }
  }, [serviceName, clusterName,isClusterInstalled]);

  // Refresh quicklinks (same as load)
  const refreshQuicklinks = useCallback(async () => {
    if(isClusterInstalled)
    await loadQuicklinks();
  }, [loadQuicklinks,isClusterInstalled]);

  return {
    quicklinks,
    isLoading,
    error,
    loadQuicklinks,
    refreshQuicklinks,
  };
};
