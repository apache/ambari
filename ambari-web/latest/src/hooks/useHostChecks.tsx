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
import {
  finishStates,
  minDiskSpace,
  minDiskSpaceUsrLib,
} from "../screens/Hosts/constants";
import { HostsApi } from "../api/hostsApi";
import usePolling from "./usePolling";
import { cloneDeep, get, isEmpty, map, set } from "lodash";
import { AppContext } from "../store/context";
import { BootStatus } from "../screens/ClusterWizard/Step3";
import { sortPropertyLight } from "../screens/Hosts/helpers";

type BootHostType = {
  name: string;
};

type HostCheckRecoveryOptions = {
  initialHosts?: BootHostType[];
  initialRequestID?: number;
  initialWarningData?: any;
  onStateChange?: (state: {
    isRunning: boolean;
    requestID: number;
    warningData: any;
  }) => void;
};

export const initialWarningData: any = {
  allWarnings: [],
  repoCategoryWarnings: [],
  diskCategoryWarnings: [],
  thpCategoryWarnings: [],
  hostCheckWarnings: [],
  warningsByHost: [],
  jdkCategoryWarnings: [],
};

export const useHostChecks = (
  isAddHostWizard = false,
  isClusterInstallationWizard = false,
  recovery: HostCheckRecoveryOptions = {},
) => {
  const { clusterName, ambariProperties } = useContext(AppContext);

  const [requestID, setRequestID] = useState<number>(
    recovery.initialRequestID ?? -1,
  );
  const [warningData, setWarningData] = useState<any>(
    recovery.initialWarningData || initialWarningData,
  );
  const [componentInfo, setComponentInfo] = useState({});
  const [isHostCheckRunning, setIsHostCheckRunning] = useState(
    recovery.initialRequestID != null && recovery.initialRequestID !== -1,
  );

  const hostsPackagesData = useRef([]);
  const hostCheckResult = useRef(null);
  const skipBootstrap = useRef(false);
  const hostPackagesData = useRef([]);
  const bootHosts = useRef<BootHostType[]>(recovery.initialHosts || []);
  const dataForHostCheck = useRef({});

  const getHostCheckTasks = async () => {
    // Don't attempt API call with invalid requestID
    if (requestID === -1) {
      return;
    }

    const response = await HostsApi.getRequestStatus(
      requestID,
      "Requests/inputs,Requests/request_status,Requests/end_time,tasks/Tasks/host_name,tasks/Tasks/structured_out/host_resolution_check/hosts_with_failures,tasks/Tasks/structured_out/host_resolution_check/failed_count,tasks/Tasks/structured_out/installed_packages,tasks/Tasks/structured_out/last_agent_env_check,tasks/Tasks/structured_out/transparentHugePage,tasks/Tasks/structured_out/java_home_check/exit_code,tasks/Tasks/stdout,tasks/Tasks/stderr,tasks/Tasks/error_log,tasks/Tasks/command_detail,tasks/Tasks/status&minimal_response=true"
    );
    await getHostCheckTasksSuccess(response);
  };

  const { pausePolling, resumePolling } = usePolling(getHostCheckTasks, 1000);

  useEffect(() => {
    pausePolling();
    if (!isClusterInstallationWizard) {
      getClusterComponents();
    }
  }, []);

  useEffect(() => {
    if (requestID !== -1) {
      resumePolling();
    }
  }, [requestID]);

  useEffect(() => {
    recovery.onStateChange?.({
      isRunning: isHostCheckRunning,
      requestID,
      warningData,
    });
  }, [isHostCheckRunning, requestID, warningData]);

  const getClusterComponents = async () => {
    const response = await HostsApi.getClusterComponents(
      clusterName,
      "ServiceComponentInfo/component_name,host_components/HostRoles/host_name"
    );
    setComponentInfo(response);
  };

  const getMasterComponentHosts = () => {
    const components = get(componentInfo, "items", []);
    return components
      .map((component: any) => {
        return get(component, "host_components", []).map(
          (hostComponent: any) => {
            return get(hostComponent, "HostRoles.host_name", "");
          }
        );
      })
      .flat();
  };

  const cleanup = () => {
    setWarningData(initialWarningData);
  };

  const stopHostCheck = () => {
    setIsHostCheckRunning(false);
    pausePolling();
    cleanup();
  };

  const requestToPerformHostCheck = async () => {
    if (!isEmpty(dataForHostCheck.current)) {
      const data = {
        RequestInfo: get(dataForHostCheck.current, "RequestInfo", {}),
        "Requests/resource_filters": [
          get(dataForHostCheck.current, "resource_filters", {}),
        ],
      };
      const response = await HostsApi.makeRequest(data);
      setRequestID(get(response, "Requests.id", -1));
    }
  };

  const getDataForCheckRequest = (
    checkExecuteList: string,
    addHostsParameter: boolean
  ) => {
    const newHosts = bootHosts.current
      .filter((host) => get(host, "bootStatus") === BootStatus.REGISTERED)
      .map((host) => get(host, "name"));
    const hosts = isAddHostWizard
      ? [...new Set([...getMasterComponentHosts(), ...newHosts])]
      : newHosts;
    const hostsString = hosts.join(",");
    if (hostsString.length === 0) return null;
    const jdkLocation = get(
      ambariProperties,
      "RootServiceComponents.properties.jdk_location",
      ""
    );
    const RequestInfo = {
      action: "check_host",
      context: "Check host",
      parameters: {
        check_execute_list: checkExecuteList,
        jdk_location: jdkLocation,
        threshold: "20",
      },
    };
    if (addHostsParameter) {
      set(RequestInfo, "parameters.hosts", hostsString);
    }
    const resourceFilters = {
      hosts: hostsString,
    };
    return {
      RequestInfo,
      resource_filters: resourceFilters,
    };
  };

  const getGeneralHostCheck = () => {
    const data = getDataForCheckRequest(
      "last_agent_env_check,installed_packages,existing_repos,transparentHugePage",
      false
    );
    if (data) {
      requestToPerformHostCheck();
    } else {
      stopHostCheck();
    }
  };

  const filterHostsData = (data: any) => {
    const bootHostNames = new Set(
      bootHosts.current.map((bootHost: any) => bootHost.name)
    );
    const filteredData = {
      href: data.href,
      tasks: data.tasks.filter((task: any) =>
        bootHostNames.has(task.Tasks.host_name)
      ),
    };
    return filteredData;
  };

  const filterBootHosts = (data: any) => {
    const bootHostNames = new Set(
      bootHosts.current.map((bootHost: any) => bootHost.name)
    );
    const filteredData = {
      href: data.href,
      items: data.items.filter((host: any) =>
        bootHostNames.has(host.Hosts.host_name)
      ),
    };
    return filteredData;
  };

  const parseHostCheckWarnings = (warningDataCopy: any, data: any) => {
    data = filterHostsData(data);
    let warnings: any[] = [];
    let warning;
    let hosts: any[] = [];
    let warningCategories = {
      fileFoldersWarnings: {},
      packagesWarnings: {},
      processesWarnings: {},
      servicesWarnings: {},
      usersWarnings: {},
      alternativeWarnings: {},
    };

    sortPropertyLight(data.tasks, "Tasks.host_name").forEach((_task: any) => {
      let hostName = get(_task, "Tasks.host_name", "");
      let host: any = {
        name: hostName,
        warnings: [],
      };

      if (
        !get(_task, "Tasks.structured_out", "") ||
        !get(_task, "Tasks.structured_out.last_agent_env_check", "")
      ) {
        return;
      }

      let lastAgentEnvCheck = get(
        _task,
        "Tasks.structured_out.last_agent_env_check",
        ""
      );

      let stackFoldersAndFiles = get(
        lastAgentEnvCheck,
        "stackFoldersAndFiles",
        []
      );
      stackFoldersAndFiles.forEach((path: any) => {
        warning = get(warningCategories, "fileFoldersWarnings." + path.name);
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: path.name,
            hosts: [hostName],
            hostsLong: [hostName],
            category: "fileFolders",
          };
          set(warningCategories, "fileFoldersWarnings." + path.name, warning);
        }
        host.warnings.push(warning);
      });

      // parse all package warnings for host
      let _hostPackagesData = hostsPackagesData.current.find(
        (hpd: any) => get(hpd, "hostName") === hostName
      );

      if (!isEmpty(_hostPackagesData)) {
        get(_hostPackagesData, "installedPackages", []).forEach(
          (_package: any) => {
            warning = get(
              warningCategories,
              "packagesWarnings." + _package.name
            );
            if (warning) {
              warning.hosts.push(hostName);
              warning.hostsLong.push(hostName);
              warning.version = _package.version;
            } else {
              warning = {
                name: _package.name,
                version: _package.version,
                hosts: [hostName],
                hostsLong: [hostName],
                category: "packages",
              };
              set(
                warningCategories,
                "packagesWarnings." + _package.name,
                warning
              );
            }
            host.warnings.push(warning);
          }
        );
      }

      // parse all process warnings for host
      let hostHealth = lastAgentEnvCheck.hostHealth;

      let liveServices = get(hostHealth, "liveServices", null);
      let javaProcs = get(hostHealth, "activeJavaProcs", null);

      if (javaProcs) {
        javaProcs.forEach((process: any) => {
          warning = get(warningCategories, "processesWarnings." + process.pid);
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
          } else {
            let command = `pid=${process.pid}, user=${process.user}`;
            warning = {
              name:
                command.length > 36
                  ? command.substring(0, 35) + "..."
                  : command,
              hosts: [hostName],
              hostsLong: [hostName],
              category: "processes",
              user: process.user,
              pid: process.pid,
              command: command,
            };
            set(warningCategories, "processesWarnings." + process.pid, warning);
          }
          host.warnings.push(warning);
        });
      }

      // parse all service warnings for host
      if (liveServices) {
        liveServices.forEach((service: any) => {
          if (service.status === "Unhealthy") {
            warning = get(
              warningCategories,
              "servicesWarnings." + service.name
            );
            if (warning) {
              warning.hosts.push(hostName);
              warning.hostsLong.push(hostName);
            } else {
              warning = {
                name: service.name,
                hosts: [hostName],
                hostsLong: [hostName],
                category: "services",
              };
              set(
                warningCategories,
                "servicesWarnings" + service.name,
                warning
              );
            }
            host.warnings.push(warning);
          }
        });
      }

      // parse all user warnings for host
      let existingUsers = lastAgentEnvCheck.existingUsers;
      if (existingUsers) {
        existingUsers.forEach((user: any) => {
          warning = get(warningCategories, "usersWarnings." + user.name);
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
          } else {
            warning = {
              name: user.name,
              hosts: [hostName],
              hostsLong: [hostName],
              category: "users",
            };
            set(warningCategories, "usersWarnings." + user.name, warning);
          }
          host.warnings.push(warning);
        });
      }

      // parse misc warnings for host
      let umask = lastAgentEnvCheck.umask;
      if (umask && umask > 23) {
        warning = warnings.find(
          (w) => w.category === "misc" && w.name === umask
        );
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: umask,
            hosts: [hostName],
            hostsLong: [hostName],
            category: "misc",
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      let firewallRunning = lastAgentEnvCheck.firewallRunning;
      if (firewallRunning !== null && firewallRunning) {
        let name = `${lastAgentEnvCheck.firewallName} Running`;
        warning = warnings.find(
          (w) => w.category === "firewall" && w.name === name
        );
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: name,
            hosts: [hostName],
            hostsLong: [hostName],
            category: "firewall",
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      if (lastAgentEnvCheck.alternatives) {
        lastAgentEnvCheck.alternatives.forEach((alternative: any) => {
          warning = get(
            warningCategories,
            "alternativeWarnings" + alternative.name
          );
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
          } else {
            warning = {
              name: alternative.name,
              target: alternative.target,
              hosts: [hostName],
              hostsLong: [hostName],
              category: "alternatives",
            };
            set(
              warningCategories,
              "alternativeWarnings" + alternative.name,
              warning
            );
          }
          host.warnings.push(warning);
        });
      }

      if (lastAgentEnvCheck.reverseLookup === false) {
        let name = "Reverse Lookup validation failed on";
        warning = warnings.find(
          (w) => w.category === "reverseLookup" && w.name === name
        );
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: name,
            hosts: [hostName],
            hostsLong: [hostName],
            category: "reverseLookup",
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }
      hosts.push(host);
    });

    for (let categoryId in warningCategories) {
      let category = get(warningCategories, categoryId);
      for (let warningId in category) {
        warnings.push(category[warningId]);
      }
    }

    hosts.unshift({
      name: "All Hosts",
      warnings: warnings,
    });

    set(warningDataCopy, "allWarnings", warnings);
    set(warningDataCopy, "warningsByHost", hosts);
  };

  function parseWarnings(warningDataCopy: any, data: any) {
    data = filterBootHosts(data);
    let warnings: any[] = [];
    let warning;
    let hosts: any[] = [];
    let warningCategories = {
      fileFoldersWarnings: {},
      packagesWarnings: {},
      processesWarnings: {},
      servicesWarnings: {},
      usersWarnings: {},
      alternativeWarnings: {},
    };

    sortPropertyLight(data.items, "Hosts.host_name").forEach((_host: any) => {
      let host: any = {
        name: _host.Hosts.host_name,
        warnings: [],
      };
      if (!_host.Hosts.last_agent_env) {
        return;
      }

      let stackFoldersAndFiles = get(
        _host,
        "Hosts.last_agent_env.stackFoldersAndFiles",
        []
      );
      stackFoldersAndFiles.forEach((path: any) => {
        warning = get(warningCategories, "fileFoldersWarnings." + path.name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: path.name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: "fileFolders",
          };
          set(warningCategories, "fileFoldersWarnings." + path.name, warning);
        }
        host.warnings.push(warning);
      });

      let _hostPackagesData: any = hostsPackagesData.current.find(
        (hpd: any) => get(hpd, "hostName") === get(_host, "Hosts.host_name")
      );
      if (_hostPackagesData) {
        _hostPackagesData.installedPackages.forEach((_package: any) => {
          warning = get(warningCategories, "packagesWarnings." + _package.name);
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
            warning.version = _package.version;
          } else {
            warning = {
              name: _package.name,
              version: _package.version,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: "packages",
            };
            set(
              warningCategories,
              "packagesWarnings." + _package.name,
              warning
            );
          }
          host.warnings.push(warning);
        });
      }

      let javaProcs = _host.Hosts.last_agent_env.hostHealth
        ? _host.Hosts.last_agent_env.hostHealth.activeJavaProcs
        : _host.Hosts.last_agent_env.javaProcs;
      if (javaProcs) {
        javaProcs.forEach((process: any) => {
          warning = get(warningCategories, "processesWarnings." + process.pid);
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
          } else {
            let command = "pid=" + process.pid + ", user=" + process.user;
            warning = {
              name:
                command.length > 36
                  ? command.substring(0, 35) + "..."
                  : command,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: "processes",
              user: process.user,
              pid: process.pid,
              command: command,
            };
            set(warningCategories, "processesWarnings." + process.pid, warning);
          }
          host.warnings.push(warning);
        });
      }

      if (
        _host.Hosts.last_agent_env.hostHealth &&
        _host.Hosts.last_agent_env.hostHealth.liveServices
      ) {
        _host.Hosts.last_agent_env.hostHealth.liveServices.forEach(
          (service: any) => {
            if (service.status === "Unhealthy") {
              warning = get(
                warningCategories,
                "servicesWarnings" + service.name
              );
              if (warning) {
                warning.hosts.push(_host.Hosts.host_name);
                warning.hostsLong.push(_host.Hosts.host_name);
              } else {
                warning = {
                  name: service.name,
                  hosts: [_host.Hosts.host_name],
                  hostsLong: [_host.Hosts.host_name],
                  category: "services",
                };
                set(
                  warningCategories,
                  "servicesWarnings" + service.name,
                  warning
                );
              }
              host.warnings.push(warning);
            }
          }
        );
      }

      if (_host.Hosts.last_agent_env.existingUsers) {
        _host.Hosts.last_agent_env.existingUsers.forEach((user: any) => {
          warning = get(warningCategories, "usersWarnings." + user.name);
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
          } else {
            warning = {
              name: user.name,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: "users",
            };
            set(warningCategories, "usersWarnings." + user.name, warning);
          }
          host.warnings.push(warning);
        });
      }

      let umask = _host.Hosts.last_agent_env.umask;
      if (umask && umask > 23) {
        warning = warnings.find(
          (w) => w.category === "misc" && w.name === umask
        );
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: umask,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: "misc",
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      let firewallRunning = _host.Hosts.last_agent_env.firewallRunning;
      if (firewallRunning !== null && firewallRunning) {
        let name = _host.Hosts.last_agent_env.firewallName + " Running";
        warning = warnings.find(
          (w) => w.category === "firewall" && w.name === name
        );
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: "firewall",
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      if (_host.Hosts.last_agent_env.alternatives) {
        _host.Hosts.last_agent_env.alternatives.forEach((alternative: any) => {
          warning = get(
            warningCategories,
            "alternativeWarnings" + alternative.name
          );
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
          } else {
            warning = {
              name: alternative.name,
              target: alternative.target,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: "alternatives",
            };
            set(
              warningCategories,
              "alternativeWarnings" + alternative.name,
              warning
            );
          }
          host.warnings.push(warning);
        });
      }

      if (_host.Hosts.last_agent_env.reverseLookup === false) {
        let name = "Reverse Lookup validation failed on";
        warning = warnings.find(
          (w) => w.category === "reverseLookup" && w.name === name
        );
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: "reverseLookup",
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }
      hosts.push(host);
    });

    for (let categoryId in warningCategories) {
      let category = get(warningCategories, categoryId);
      for (let warningId in category) {
        warnings.push(category[warningId]);
      }
    }

    hosts.unshift({
      name: "All Hosts",
      warnings: warnings,
    });

    set(warningDataCopy, "allWarnings", warnings);
    set(warningDataCopy, "warningsByHost", hosts);
  }

  const setHostDataWithSkipBootstrap = (host: any) => {
    set(host, "cpu", 2);
    set(host, "memory", parseInt("2000000").toFixed(2));
    set(host, "disk_info", [
      { mountpoint: "/", type: "ext4" },
      { mountpoint: "/grid/0", type: "ext4" },
      { mountpoint: "/grid/1", type: "ext4" },
      { mountpoint: "/grid/2", type: "ext4" },
    ]);
    return host;
  };

  const setHostDataFromLoadedHostInfo = (host: any, hostInfo: any) => {
    set(host, "cpu", get(hostInfo, "Hosts.cpu_count", 2));
    set(
      host,
      "memory",
      parseInt(get(hostInfo, "Hosts.total_mem", "2000000")).toFixed(2)
    );
    set(
      host,
      "disk_info",
      get(hostInfo, "Hosts.disk_info", []).filter(
        (h: any) => h.mountpoint != "/boot"
      )
    );
    set(host, "os_type", get(hostInfo, "Hosts.os_type", ""));
    set(host, "os_family", get(hostInfo, "Hosts.os_family", ""));
    set(host, "os_arch", get(hostInfo, "Hosts.os_arch", ""));
    set(host, "ip", get(hostInfo, "Hosts.ip", ""));
    return host;
  };

  const checkHostOSType = (osFamily: any, hostName: any) => {
    const stacks: any[] = []; //TODO: figure out and get the stack from content or App
    if (!isEmpty(stacks)) {
      const selectedStack = stacks.find((stack: any) => stack.isSelected);
      const selectedOS: any[] = [];
      let isValid = false;
      if (selectedStack && selectedStack.operatingSystems) {
        selectedStack.operatingSystems
          .filter((os: any) => os.isSelected)
          .forEach((os: any) => {
            selectedOS.push(os.osType);
            if (os.osType === osFamily) {
              isValid = true;
            }
          });
      }
      if (isValid) {
        return "";
      } else {
        return `Host (${hostName}) is ${osFamily} OS type, but the repositories chosen in "Select Stack" step was ${[
          ...new Set(selectedOS),
        ].join(", ")}. Selected repositories do not support this host OS type.`;
      }
    } else {
      return "";
    }
  };

  const checkHostDiskSpace = (hostName: any, diskInfo: any) => {
    const minFreeRootSpace = minDiskSpace * 1024 * 1024;
    const minFreeUsrLibSpace = minDiskSpaceUsrLib * 1024 * 1024;
    let warningString = "";

    diskInfo.forEach((info: any) => {
      switch (info.mountpoint) {
        case "/":
          warningString =
            info.available < minFreeRootSpace
              ? `A minimum of ${minDiskSpace}GB is required for "${info.mountpoint}" mount. ${warningString}`
              : warningString;
          break;
        case "/usr":
        case "/usr/lib":
          warningString =
            info.available < minFreeUsrLibSpace
              ? `A minimum of ${minDiskSpaceUsrLib}GB is required for "${info.mountpoint}" mount. ${warningString}`
              : warningString;
          break;
        default:
          break;
      }
    });

    return warningString
      ? `Not enough disk space on host (${hostName}). ${warningString}`
      : "";
  };

  const checkTHP = (hostName: any, transparentHugePage: any) => {
    if (transparentHugePage === "always") {
      return hostName;
    } else {
      return "";
    }
  };

  const getHostInfoSuccessCallback = (data: any) => {
    const warningDataCopy = cloneDeep(warningData);
    const hosts = bootHosts.current;
    let repoWarnings: any[] = [],
      diskWarnings: any[] = [],
      thpWarnings: any[] = [],
      hostsRepoNames: any[] = [],
      hostsDiskContext: any[] = [],
      thpContext: any[] = [],
      hostsContext: any[] = [],
      hostsDiskNames: any[] = [],
      thpHostsNames: any[] = [];

    const hostCheckRes = hostCheckResult.current;
    if (hostCheckRes) {
      parseHostCheckWarnings(warningDataCopy, hostCheckRes);
      hostCheckResult.current = null;
    } else {
      parseWarnings(warningDataCopy, data);
    }
    hosts.forEach((_host) => {
      const host = get(data, "items", []).filter((item: any) => {
        return get(item, "Hosts.host_name") === get(_host, "name");
      })[0];
      if (skipBootstrap.current) {
        _host = setHostDataWithSkipBootstrap(_host);
      } else if (!isEmpty(host)) {
        _host = setHostDataFromLoadedHostInfo(_host, host);
        const hostName = get(host, "Hosts.host_name", "");

        let context = checkHostOSType(
          get(host, "Hosts.os_family", ""),
          hostName
        );
        if (!isEmpty(context)) {
          hostsContext.push(context);
          hostsRepoNames.push(hostName);
        }

        const diskContext = checkHostDiskSpace(
          hostName,
          get(host, "Hosts.disk_info", [])
        );
        if (!isEmpty(diskContext)) {
          hostsDiskContext.push(diskContext);
          hostsDiskNames.push(hostName);
        }
        const _hostPackagesData = hostPackagesData.current.filter(
          (hostData: any) => {
            return get(hostData, "hostName") === hostName;
          }
        );
        if (!isEmpty(_hostPackagesData)) {
          const transparentHugePage = get(
            _hostPackagesData,
            "transparentHugePage",
            []
          );
          context = checkTHP(hostName, transparentHugePage);
        } else {
          context = checkTHP(
            hostName,
            get(host, "Hosts.last_agent_env.transparentHugePage")
          );
        }
        if (!isEmpty(context)) {
          thpContext.push(context);
          thpHostsNames.push(hostName);
        }
      }
    });

    if (hostsContext.length > 0) {
      repoWarnings.push({
        name: "Repository for OS not available",
        hosts: hostsContext,
        hostsLong: hostsContext,
        hostsNames: hostsRepoNames,
        category: "repositories",
      });
    }

    if (hostsDiskContext.length > 0) {
      diskWarnings.push({
        name: "Not enough disk space",
        hosts: hostsDiskContext,
        hostsLong: hostsDiskContext,
        hostsNames: hostsDiskNames,
        category: "disk",
      });
    }

    if (thpContext.length > 0) {
      thpWarnings.push({
        name: "Transparent Huge Pages",
        hosts: thpContext,
        hostsLong: thpContext,
        hostsNames: thpHostsNames,
        category: "thp",
      });
    }

    set(warningDataCopy, "repoCategoryWarnings", repoWarnings);
    set(warningDataCopy, "diskCategoryWarnings", diskWarnings);
    set(warningDataCopy, "thpCategoryWarnings", thpWarnings);
    setWarningData(warningDataCopy);
  };

  const getHostInfo = async () => {
    const response = await HostsApi.getHostsData(
      "Hosts/total_mem,Hosts/cpu_count,Hosts/disk_info,Hosts/last_agent_env,Hosts/host_name,Hosts/os_type,Hosts/os_arch,Hosts/os_family,Hosts/ip"
    );
    getHostInfoSuccessCallback(response);
  };

  const parseHostNameResolution = (data: any) => {
    if (!data) {
      return;
    }
    const warningDataCopy = cloneDeep(warningData);
    data.tasks.forEach((task: any) => {
      const name = "Hostname resolution";
      let hostInfo = get(warningDataCopy, "allWarnings", []).find(
        (warning: any) => warning.name === name
      );
      if (finishStates.includes(task.Tasks.status)) {
        if (
          task.Tasks.status === "COMPLETED" &&
          !!get(task, "Tasks.structured_out.host_resolution_check.failed_count")
        ) {
          const targetHostName = get(task, "Tasks.host_name");
          const relatedHostNames = get(
            task,
            "Tasks.structured_out.host_resolution_check.hosts_with_failures",
            []
          );
          const contextMessage = `${targetHostName} could not resolve: ${
            relatedHostNames.length
          } host${relatedHostNames.length === 1 ? "" : "s"}.`;
          const contextMessageLong = `${targetHostName} could not resolve: ${relatedHostNames.join(
            ", "
          )}.`;
          if (!hostInfo) {
            hostInfo = {
              name: name,
              hosts: [contextMessage],
              hostsLong: [contextMessageLong],
              hostsNames: [targetHostName],
              category: "hostNameResolution",
            };
            warningDataCopy.hostCheckWarnings = [
              ...warningDataCopy.hostCheckWarnings,
              hostInfo,
            ];
          } else {
            if (!hostInfo.hostsNames.includes(targetHostName)) {
              hostInfo.hosts.push(contextMessage);
              hostInfo.hostsLong.push(contextMessageLong);
              hostInfo.hostsNames.push(targetHostName);
              warningDataCopy.hostCheckWarnings =
                warningDataCopy.hostCheckWarnings.map((warning: any) =>
                  warning.name === name ? hostInfo : warning
                );
            }
          }
        }
      }
    });
    setWarningData(warningDataCopy);
  };

  const finishHostCheck = () => {
    pausePolling();
    setIsHostCheckRunning(false);
    setRequestID(-1);
  };

  const launchJdkCheck = async () => {
    const hostNames = map(bootHosts.current, "name").join(",");
    const properties = get(ambariProperties, "RootServiceComponents.properties", {});
    if (!hostNames || properties["jdk.name"]) {
      finishHostCheck();
      return;
    }
    const response = await HostsApi.makeRequest({
      RequestInfo: {
        action: "check_host",
        context: "Check hosts",
        parameters: {
          check_execute_list: "java_home_check",
          java_home: properties["java.home"],
          jdk_location: properties.jdk_location,
          threshold: "60",
        },
      },
      "Requests/resource_filters": [{ hosts: hostNames }],
    });
    const nextRequestID = get(response, "Requests.id", -1);
    if (nextRequestID === -1) {
      finishHostCheck();
      return;
    }
    setRequestID(nextRequestID);
  };

  const parseJdkCheckResults = (data: any) => {
    const invalidHosts = get(data, "tasks", []).filter(
      (task: any) => Number(get(
        task,
        "Tasks.structured_out.java_home_check.exit_code",
        0,
      )) === 1,
    ).map((task: any) => get(task, "Tasks.host_name", ""));
    const javaHome = get(
      ambariProperties,
      "RootServiceComponents.properties.java.home",
      "",
    );
    setWarningData((current: any) => ({
      ...current,
      jdkCategoryWarnings: invalidHosts.length ? [{
        category: "jdk",
        hosts: invalidHosts.map((hostName: string) =>
          `${hostName} does not have a valid JDK at ${javaHome}.`,
        ),
        hostsLong: invalidHosts,
        hostsNames: invalidHosts,
        name: `Invalid JDK location ${javaHome}`,
      }] : [],
    }));
  };

  const getHostCheckTasksSuccess = async (data: any) => {
    if (isEmpty(data)) {
      getGeneralHostCheck();
      return;
    }
    if (finishStates.includes(get(data, "Requests.request_status"))) {
      if (get(data, "Requests.inputs", "").includes("last_agent_env_check")) {
        pausePolling();
        hostsPackagesData.current = get(data, "tasks", []).map((task: any) => {
          const installedPackages = get(
            task,
            "Tasks.structured_out.installed_packages",
            []
          );
          return {
            hostName: get(task, "Tasks.host_name", ""),
            transparentHugePage: get(
              task,
              "Tasks.structured_out.transparentHugePage.message",
              ""
            ),
            installedPackages: installedPackages,
          };
        });
        hostCheckResult.current = data;
        await getHostInfo();
        try {
          await launchJdkCheck();
        } catch {
          finishHostCheck();
        }
      } else if (get(data, "Requests.inputs", "").includes("java_home_check")) {
        parseJdkCheckResults(data);
        finishHostCheck();
      } else if (
        get(data, "Requests.inputs", "").includes("host_resolution_check")
      ) {
        parseHostNameResolution(data);
        getGeneralHostCheck();
      }
    }
  };

  const startHostCheck = (bootHostsList: BootHostType[]) => {
    cleanup();
    bootHosts.current = bootHostsList;
    if (!isEmpty(ambariProperties)) {
      const hostName = map(bootHosts.current, "name").join(",");
      const jdkLocation = get(
        ambariProperties,
        "RootServiceComponents.properties.jdk_location",
        ""
      );
      const requestInfo = {
        action: "check_host",
        context: "Check host",
        parameters: {
          hosts: hostName,
          check_execute_list:
            "last_agent_env_check,installed_packages,existing_repos,transparentHugePage",
          jdk_location: jdkLocation,
          threshold: "20",
        },
      };
      const data = {
        RequestInfo: requestInfo,
        resource_filters: { hosts: hostName },
      };
      dataForHostCheck.current = data;
    }
    setIsHostCheckRunning(true);
    requestToPerformHostCheck();
  };

  return {
    startHostCheck,
    stopHostCheck,
    isHostCheckRunning,
    hostCheckResult: warningData,
  };
};
