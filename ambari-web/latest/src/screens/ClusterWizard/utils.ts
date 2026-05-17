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
import { cloneDeep,get, isArray } from "lodash";
import { getCardinalityValue } from "../../Utils/numberUtils";
import { ComponentBlueprint } from "./types/ComponentBlueprint";
import { trimProperty } from "../CommonConfigs/ConfigUtils";

export const isHostname = (hostname: string): boolean => {
  const regex = new RegExp(
    /(?=^.{3,254}$)(^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*(\.[a-zA-Z]{1,62})$)/
  );
  return hostname === "localhost" || regex.test(hostname);
};

export const isValidUserName = (username: string): boolean => {
  const regex = new RegExp(/^[a-z]([-a-z0-9]{0,30})$/);
  return regex.test(username);
}

export const getDependentConfigChanges = (
  changedConfig: any,
  selectedServices: string[],
  allConfigs: any
) => {
  const propertyName = get(changedConfig, "propertyName");
  const propertyValue = get(changedConfig, "value");
  let allAffectedProperties: any[] = [];
  if (propertyName === "hdfs_user") {
    const affectedPropertyNames = [
      // "dfs.permissions.superusergroup",
      "dfs.cluster.administrators",
    ];
    allConfigs.forEach((config: any) => {
      if (
        get(config, "serviceName") === "HDFS" &&
        affectedPropertyNames.includes(get(config, "propertyName")) &&
        propertyValue.trim() !== get(config, "value", "").trim()
      ) {
        allAffectedProperties.push({ ...config, new_value: propertyValue });
      }
    });
  } else if (propertyName === "yarn_user") {
    const affectedPropertyNames = ["yarn.admin.acl"];
    allConfigs.forEach((config: any) => {
      if (
        get(config, "serviceName") === "YARN" &&
        affectedPropertyNames.includes(get(config, "propertyName")) &&
        propertyValue.trim() !== get(config, "value").trim()
      ) {
        allAffectedProperties.push({ ...config, new_value: propertyValue });
      }
    });
  } else if (propertyName === "user_group") {
    if (!selectedServices.includes("YARN")) {
      return;
    }
    if (selectedServices.includes("MAPREDUCE2")) {
      const affectedPropertyNames = ["mapreduce.cluster.administrators"];
      allConfigs.forEach((config: any) => {
        if (
          get(config, "serviceName") === "MAPREDUCE2" &&
          affectedPropertyNames.includes(get(config, "propertyName")) &&
          propertyValue.trim() !== get(config, "value").trim()
        ) {
          allAffectedProperties.push({ ...config, new_value: propertyValue });
        }
      });
    }
    if (selectedServices.includes("YARN")) {
      const affectedPropertyNames = [
        "yarn.nodemanager.linux-container-executor.group",
      ];
      allConfigs.forEach((config: any) => {
        if (
          get(config, "serviceName") === "YARN" &&
          affectedPropertyNames.includes(get(config, "propertyName")) &&
          propertyValue.trim() !== get(config, "value").trim()
        ) {
          allAffectedProperties.push({ ...config, new_value: propertyValue });
        }
      });
    }
  }

  return allAffectedProperties;
};
export function minToInstall(cardinality: string) {
  return getCardinalityValue(cardinality, false);
}

export function isShownOnInstallerSlaveClientPage(component: any) {
  const componentName = component.component_name;
  const slavesNotShown = ["JOURNALNODE", "ZKFC", "APP_TIMELINE_SERVER"];
  return (
    component["component_category"] === "SLAVE" &&
    minToInstall(component.cardinality) !== Infinity &&
    !slavesNotShown.includes(componentName)
  );
}

const heapsizeException = [
  "hadoop_heapsize",
  "yarn_heapsize",
  "nodemanager_heapsize",
  "resourcemanager_heapsize",
  "apptimelineserver_heapsize",
  "jobhistory_heapsize",
  "nfsgateway_heapsize",
  "accumulo_master_heapsize",
  "accumulo_tserver_heapsize",
  "accumulo_monitor_heapsize",
  "accumulo_gc_heapsize",
  "accumulo_other_heapsize",
  "hbase_master_heapsize",
  "hbase_regionserver_heapsize",
  "metrics_collector_heapsize",
  "hive_heapsize",
];

const heapsizeRegExp =
  /_heapsize|_newsize|_maxnewsize|_permsize|_maxpermsize$/;

const addM = (name: string, value: string) => {
  return (
    heapsizeRegExp.test(name) &&
    !heapsizeException.includes(name) &&
    !value.endsWith("m")
  );
};

export function formatValuesBeforeSave(property:any){
  const name = get(property, "propertyName");
  let value = get(property, "value");
  // var kdcTypesMap = App.router.get('mainAdminKerberosController.kdcTypesValues');

  if (addM(name, value)) {
    return (value += "m");
  }
  if (typeof value === "boolean") {
    return value.toString();
  }
  switch (name) {
    // case 'kdc_type':
    //   return Em.keys(kdcTypesMap).filter(function(key) {
    //       return kdcTypesMap[key] === value;
    //   })[0];
    case "storm.zookeeper.servers":
    case "nimbus.seeds":
      if (isArray(value)) {
        return JSON.stringify(value).replace(/"/g, "'");
      } else {
        return value;
      }
      break;
    default:
      return trimProperty(property, value);
  }
}

export const blueprintUtils = {
  getBlueprint: (
    hosts: string[],
    hostComponentMapping: { hostname: string; components: { name: string }[] }[]
  ) => {
    const blueprint = { host_groups: [] };
    const blueprint_cluster_binding = { host_groups: [] };
    let hostGroupIndex = 1;
    for (let host of hosts) {
      const correspondingHost = hostComponentMapping.find((master: any) => {
        return master.hostname === host;
      });
      let components: { name: string }[] = [];
      if (correspondingHost) {
        components = correspondingHost.components;
      }
      //@ts-ignore
      blueprint.host_groups.push({
        name: `host-group-${hostGroupIndex}`,
        components,
      });
      //@ts-ignore
      blueprint_cluster_binding.host_groups.push({
        name: `host-group-${hostGroupIndex}`,
        hosts: [{ fqdn: host }],
      });
      hostGroupIndex++;
    }
    return {
      blueprint,
      blueprint_cluster_binding
    }
  },
  mergeBlueprints:(blueprint1:ComponentBlueprint,blueprint2:ComponentBlueprint)=>{
    const obj1=cloneDeep(blueprint1);
    const obj2=cloneDeep(blueprint2);
    const mergeComponents = (components1:any, components2:any) => {
      let componentNames:any=[];
      if(components1)
       componentNames = new Set(components1.map((c:any) => c.name));
      if(components2)
      components2.forEach((component:any) => {
          if (!componentNames.has(component.name)) {
              components1.push(component);
          }
      });
      return components1;
  };

  const mergeHostGroups = (groups1:any, groups2:any) => {
      const mergedGroups:any = {};
      
      groups1.forEach((group:any) => {
          mergedGroups[group.name] = { ...group };
      });
      
      groups2.forEach((group:any) => {
          if (mergedGroups[group.name]) {
              mergedGroups[group.name].components = mergeComponents(mergedGroups[group.name].components, group.components);
              mergedGroups[group.name].hosts = mergeComponents(mergedGroups[group.name].hosts || [], group.hosts || []);
          } else {
              mergedGroups[group.name] = { ...group };
          }
      });

      return Object.values(mergedGroups);
  };

  return {
      blueprint: {
          host_groups: mergeHostGroups(obj1.blueprint.host_groups, obj2.blueprint.host_groups)
      },
      blueprint_cluster_binding: {
          host_groups: mergeHostGroups(obj1.blueprint_cluster_binding.host_groups, obj2.blueprint_cluster_binding.host_groups)
      }
  };
  }, 
  getHostGroupByFqdn: (blueprint: any, fqdn: any) => {
    const hostGroups = blueprint?.blueprint_cluster_binding?.host_groups || [];
    const hostGroup = hostGroups.find((group: any) =>
      group.hosts?.some((host: any) => host.fqdn === fqdn)
    );
 
    return hostGroup?.name || null;
  },
 
  addComponentToHostGroup: (
    blueprint: any,
    componentName: string,
    hostGroupName: string
  ) => {
    const hostGroup = blueprint.blueprint.host_groups.find(
      (group: any) => group.name === hostGroupName
    );
 
    if (hostGroup) {
      if (!hostGroup.components) {
        hostGroup.components = [];
      }
      const componentExists = hostGroup.components.some(
        (component: any) => component.name === componentName
      );
      if (!componentExists) {
        hostGroup.components.push({ name: componentName });
      }
    }
 
    return blueprint;
  },
};