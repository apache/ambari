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
  cloneDeep,
  filter,
  find,
  get,
  isFunction,
  map,
  sortBy,
  toArray,
  uniq,
} from "lodash";
import stringUtils from "../Utils/StringUtilsObj";

function getZKBasedConfig() {
  return {
    type: "zookeeper_based",
  };
}
const hostWithPort = "([\\w|\\.]*)(?=:)";
const hostWithPrefix = "://" + hostWithPort;

function WizardConfigInitializer(
  configProperty: any,
  _localDB: any,
  _dependencies: any
) {
  const obj: any = {
    _setRangerAdminPassword: () => {
      var value = "P1!q" + stringUtils.getRandomString(12);
      configProperty = {
        ...configProperty,
        ...{ value: value, recommendedValue: value, retypedPassword: value, confirmPassword: value },
      };
      return configProperty;
    },

    initializerTypes: [
      {
        name: "zookeeper_based",
        method: "_initAsZookeeperServersList",
      },
      {
        name: "host_with_component",
        method: "_initAsHostWithComponent",
      },
      {
        name: "hosts_with_components",
        method: "_initAsHostsWithComponents",
      },

      {
        name: "hosts_list_with_component",
        method: "_initAsHostsListWithComponent",
      },
    ],
    _initAsHostsListWithComponent: (
      configProperty: any,
      localDB: any,
      _dependencies: any,
      initializer?: any
    ) => {
      var hostNames = map(
        filter(
          filter(localDB.masterComponentHosts, [
            "component",
            initializer?.component,
          ]),
          ["isInstalled", initializer?.componentExists]
        ),
        "hostName"
      ).join(initializer?.modifier?.delimiter);
      configProperty = {
        ...configProperty,
        ...{
          value: hostNames,
          recommendedValue: hostNames,
        },
      };

      return configProperty;
    },
    _initAsHostsWithComponents: (
      configProperty: any,
      localDB: any,
      _dependencies: any,
      initializer?: any
    ) => {
      let hostNames: any = map(
        sortBy(
          localDB.masterComponentHosts.filter(function (masterComponent: any) {
            var hasFound = initializer?.components?.includes(
              masterComponent.component
            );
            if (initializer?.isInstalled) {
              return hasFound;
            }
            return (
              hasFound &&
              masterComponent?.isInstalled === initializer?.isInstalled
            );
          }),
          "hostName"
        ),
        "hostName"
      );
      if (!initializer) {
        hostNames = uniq(hostNames).join(",");
      }
      configProperty = {
        ...configProperty,
        ...{
          value: hostNames,
          recommendedValue: hostNames,
        },
      };
      return configProperty;
    },
    _initAsHostWithComponent: (
      configProperty: any,
      localDB: any,
      _dependencies: any,
      initializer?: any
    ) => {
      var component = find(localDB.masterComponentHosts, [
        "component",
        initializer?.component,
      ]);
      if (!component) {
        return configProperty;
      }
      if (initializer.modifier) {
        let replaceWith =
          (initializer.modifier.prefix || "") +
          component.hostName +
          (initializer.modifier.suffix || "");
        setRecommendedValue(
          configProperty,
          initializer.modifier.regex,
          replaceWith
        );
      } else {
        const changedValue = configProperty.value.split(":");
        changedValue[0] = component.hostName;
        configProperty = {
          ...configProperty,
          ...{
            recommendedValue: component.hostName,
            value: changedValue.join(":"),
          },
        };
      }

      return configProperty;
    },
    _initHiveDatabaseValue: (
      configProperty: any,
      _localDB: any,
      dependencies: any,
    ) => {
      var newMySQLDBOption = find(get(configProperty, "options"), [
        "displayName",
        "New MySQL Database",
      ]);
      if (newMySQLDBOption) {
        const isNewMySQLDBOptionHidden =
          !dependencies.alwaysEnableManagedMySQLForHive
          && !dependencies.isServiceConfigRoute
          && !dependencies.isManagedMySQLForHiveEnabled;
        if (
          isNewMySQLDBOptionHidden
          && configProperty.value === "New MySQL Database"
        ) {
          configProperty.value = "Existing MySQL Database";
        }
        newMySQLDBOption.hidden = isNewMySQLDBOptionHidden;
      }

      return configProperty;
    },
    _initTempletonHiveProperties: (
      configProperty: any,
      localDB: any,
      dependencies: any
    ) => {
      var hiveMSUris = self
        .getHiveMetastoreUris(
          localDB.masterComponentHosts,
          dependencies["hive.metastore.uris"]
        )
        .replace(",", "\\,");
      if (/\/\/localhost:/g.test(get(configProperty, "value"))) {
        configProperty.recommendedValue =
          get(configProperty, "value") + ",hive.metastore.execute.setugi=true";
      }
      setRecommendedValue(
        configProperty,
        "(hive\\.metastore\\.uris=)([^\\,]+)",
        "$1" + hiveMSUris
      );
      return configProperty;
    },
    _initHBaseZookeeperQuorum: (configProperty: any, localDB: any) => {
      if ("hbase-site.xml" === get(configProperty, "fileName")) {
        const zkHosts = uniq(map(
          filter(localDB.masterComponentHosts, [
            "component",
            "ZOOKEEPER_SERVER",
          ]),
          "hostName"
        ));
        setRecommendedValue(configProperty, "(.*)", zkHosts.join(","));
      }
      return configProperty;
    },
    _initRangerHost: (configProperty: any, localDB: any) => {
      var rangerAdminHost = find(localDB.masterComponentHosts, [
        "component",
        "RANGER_ADMIN",
      ]);
      if (rangerAdminHost) {
        configProperty = {
          ...configProperty,
          ...{
            value: rangerAdminHost.hostName,
            recommendedValue: rangerAdminHost.hostName,
          },
        };
      } else {
        configProperty = {
          ...configProperty,
          ...{
            isVisible: "false",
            isRequired: "false",
          },
        };
      }
      return configProperty;
    },
    _initAsZookeeperServersList: (configProperty: any, localDB: any) => {
      var zkHosts = uniq(map(
        filter(localDB.masterComponentHosts, ["component", "ZOOKEEPER_SERVER"]),
        "hostName"
      ));
      var zkHostPort = zkHosts.slice();
      var regex = "\\w*:(\\d+)"; //regex to fetch the port
      var sourceValue = get(configProperty, "recommendedValue") || get(configProperty, "value") || "";
      var portValue = sourceValue && sourceValue.match(new RegExp(regex));
      if (!portValue) {
        return configProperty;
      }
      if (portValue[1]) {
        for (var i = 0; i < zkHosts.length; i++) {
          zkHostPort[i] = zkHosts[i] + ":" + portValue[1];
        }
      }
      setRecommendedValue(configProperty, "(.*)", zkHostPort);
      return configProperty;
    },
    _initYarnRMzkAddress: (
      configProperty: any,
      localDB: any,
      dependencies: any
    ) => {
      const value = filter(localDB.masterComponentHosts, [
        "component",
        "ZOOKEEPER_SERVER",
      ])
        .map(function (component) {
          return component.hostName + ":" + dependencies.clientPort;
        })
        .join(",");

      configProperty = {
        ...configProperty,
        ...{
          value: value,
          recommendedValue: value,
        },
      };

      return configProperty;
    },
    _initHiveMetastoreUris: (
      configProperty: any,
      localDB: any,
      dependencies: any
    ) => {
      const fileName = get(configProperty, "fileName");
      const isHiveSite = fileName.endsWith(".xml")
        ? fileName.slice(0, -4)
        : fileName;
      if (isHiveSite) {
        var hiveMSUris = self.getHiveMetastoreUris(
          localDB.masterComponentHosts,
          dependencies["hive.metastore.uris"]
        );
        if (hiveMSUris) {
          setRecommendedValue(configProperty, "(.*)", hiveMSUris);
        }
      }
      return configProperty;
    },

    initAtlasRestAddress: (
      configProperty: any,
      localDB: any,
      dependencies: any
    ) => {
      var atlasTls = dependencies["atlas.enableTLS"];
      var httpPort = dependencies["atlas.server.http.port"];
      var httpsPort = dependencies["atlas.server.https.port"];
      var protocol = atlasTls ? "https" : "http";
      var port = atlasTls ? httpsPort : httpPort;
      var value = filter(localDB.masterComponentHosts, [
        "component",
        "ZOOKEEPER_SERVER",
      ])
        .map(function (component) {
          return protocol + "://" + component.hostName + ":" + port;
        })
        .join(",");
      configProperty = {
        ...configProperty,
        ...{
          value,
          recommendedValue: value,
        },
      };
      return configProperty;
    },
    getHiveMetastoreUris: (hosts: any, recommendedValue: any) => {
      var hiveMSHosts = map(
          filter(hosts, ["component", "HIVE_METASTORE"]),
          "hostName"
        ),
        hiveMSUris = hiveMSHosts,
        regex = "\\w*:(\\d+)",
        portValue =
          recommendedValue && recommendedValue.match(new RegExp(regex));

      if (!portValue) {
        return "";
      }
      if (portValue[1]) {
        for (var i = 0; i < hiveMSHosts.length; i++) {
          hiveMSUris[i] = "thrift://" + hiveMSHosts[i] + ":" + portValue[1];
        }
      }
      return hiveMSUris.join(",");
    },

    getSimpleComponentConfig: (component: any, withModifier?: any) => {
      if (arguments.length === 1) {
        withModifier = true;
      }
      let config: any = {
        type: "host_with_component",
        component: component,
      };
      if (withModifier) {
        config.modifier = {
          type: "regexp",
          regex: hostWithPort,
        };
      }
      return config;
    },

    getComponentConfigWithAffixes: (
      component: any,
      prefix: any,
      suffix?: any
    ) => {
      prefix = prefix || "";
      suffix = suffix || "";
      return {
        type: "host_with_component",
        component: component,
        modifier: {
          type: "regexp",
          regex: hostWithPrefix,
          prefix: prefix,
          suffix: suffix,
        },
      };
    },

    getComponentsHostsConfig: (
      components: any,
      asArray: any = false,
      isInstalled?: any
    ) => {
      if (1 === arguments.length) {
        asArray = false;
      }
      return {
        type: "hosts_with_components",
        components: toArray(components),
        asArray: asArray,
        isInstalled: isInstalled,
      };
    },
  };
  let self: any = obj;

  const uniqueInitializers: any = {
    ranger_admin_password: "_setRangerAdminPassword",
    hive_database: "_initHiveDatabaseValue",
    "templeton.hive.properties": "_initTempletonHiveProperties",
    "hbase.zookeeper.quorum": "_initHBaseZookeeperQuorum",
    "yarn.resourcemanager.zk-address": "_initYarnRMzkAddress",
    RANGER_HOST: "_initRangerHost",
    "hive.metastore.uris": "_initHiveMetastoreUris",
  };
  const initializers = {
    "dfs.namenode.rpc-address": self.getSimpleComponentConfig("NAMENODE"),
    "dfs.http.address": self.getSimpleComponentConfig("NAMENODE"),
    "dfs.namenode.http-address": self.getSimpleComponentConfig("NAMENODE"),
    "dfs.https.address": self.getSimpleComponentConfig("NAMENODE"),
    "dfs.namenode.https-address": self.getSimpleComponentConfig("NAMENODE"),
    "dfs.secondary.http.address":
      self.getSimpleComponentConfig("SECONDARY_NAMENODE"),
    "dfs.namenode.secondary.http-address":
      self.getSimpleComponentConfig("SECONDARY_NAMENODE"),
    "yarn.resourcemanager.hostname": self.getSimpleComponentConfig(
      "RESOURCEMANAGER",
      false
    ),
    "yarn.resourcemanager.resource-tracker.address":
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "yarn.resourcemanager.webapp.https.address":
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "yarn.resourcemanager.webapp.address":
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "yarn.resourcemanager.scheduler.address":
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "yarn.resourcemanager.address":
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "yarn.resourcemanager.admin.address":
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "yarn.timeline-service.webapp.address": self.getSimpleComponentConfig(
      "APP_TIMELINE_SERVER"
    ),
    "yarn.timeline-service.webapp.https.address": self.getSimpleComponentConfig(
      "APP_TIMELINE_SERVER"
    ),
    "yarn.timeline-service.address": self.getSimpleComponentConfig(
      "APP_TIMELINE_SERVER"
    ),
    "mapred.job.tracker": self.getSimpleComponentConfig("JOBTRACKER"),
    "mapred.job.tracker.http.address":
      self.getSimpleComponentConfig("JOBTRACKER"),
    "mapreduce.history.server.http.address":
      self.getSimpleComponentConfig("HISTORYSERVER"),
    "oozie.base.url": self.getComponentConfigWithAffixes("OOZIE_SERVER", "://"),
    hawq_dfs_url: self.getSimpleComponentConfig("NAMENODE"),
    hawq_rm_yarn_address: self.getSimpleComponentConfig("RESOURCEMANAGER"),
    hawq_rm_yarn_scheduler_address:
      self.getSimpleComponentConfig("RESOURCEMANAGER"),
    "fs.default.name": self.getComponentConfigWithAffixes("NAMENODE", "://"),
    "fs.defaultFS": self.getComponentConfigWithAffixes("NAMENODE", "://"),
    "hbase.rootdir": self.getComponentConfigWithAffixes("NAMENODE", "://"),
    "instance.volumes": self.getComponentConfigWithAffixes("NAMENODE", "://"),
    "yarn.log.server.url": self.getComponentConfigWithAffixes(
      "HISTORYSERVER",
      "://"
    ),
    "mapreduce.jobhistory.webapp.address":
      self.getSimpleComponentConfig("HISTORYSERVER"),
    "mapreduce.jobhistory.address":
      self.getSimpleComponentConfig("HISTORYSERVER"),
    hive_master_hosts: self.getComponentsHostsConfig([
      "HIVE_METASTORE",
      "HIVE_SERVER",
    ]),
    hadoop_host: self.getSimpleComponentConfig("NAMENODE", false),
    "nimbus.host": self.getSimpleComponentConfig("NIMBUS", false),
    "nimbus.seeds": self.getComponentsHostsConfig("NIMBUS", true),
    "storm.zookeeper.servers": self.getComponentsHostsConfig(
      "ZOOKEEPER_SERVER",
      true
    ),
    hawq_master_address_host: self.getSimpleComponentConfig(
      "HAWQMASTER",
      false
    ),
    hawq_standby_address_host: self.getSimpleComponentConfig(
      "HAWQSTANDBY",
      false
    ),

    "*.broker.url": {
      type: "host_with_component",
      component: "FALCON_SERVER",
      modifier: {
        type: "regexp",
        regex: "localhost",
      },
    },

    "zookeeper.connect": getZKBasedConfig(),
    "hive.zookeeper.quorum": getZKBasedConfig(),
    "templeton.zookeeper.hosts": getZKBasedConfig(),
    "hadoop.registry.zk.quorum": getZKBasedConfig(),
    "hive.cluster.delegation.token.store.zookeeper.connectString":
      getZKBasedConfig(),
    "instance.zookeeper.host": getZKBasedConfig(),
  };
  function _defaultInitializer(
    configProperty: any,
    _localDB: any,
    _dependencies: any
  ) {
    let args = [].slice.call(arguments);
    var localInitializers: any = initializers;
    var initializer = localInitializers[get(configProperty, "propertyName")];

    if (initializer) {
      let init = initializer;
      let _args: any = [].slice.call(args);
      const inferredVariabled = init?.type ? init.type : init;
      let type = find(self.initializerTypes, ["name", inferredVariabled]);
      // add initializer-settings
      if (type) {
        _args.push(init);
        let methodName: any = type.method;
        if (!isFunction(obj[methodName])) {
          console.log(methodName, "is not a function");
          // throw new Error("Method " + method + " is not defined");
        }
        if (init.isChecker) {
          // method(_args);
          // if (result === flowSkipNext()) {
          //   i++; // skip next
          // }
          // else {
          //   if (result === self.flowSkipAll()) {
          //     break;
          //   }
          // }
        } else {
          configProperty = obj[methodName]?.(..._args); // call method with args
        }
      }
    }
    return configProperty;
  }
  function setRecommendedValue(
    configProperty: any,
    regex: any,
    replaceWith: any
  ) {
    
    var recommendedValue = get(configProperty, "recommendedValue");
    recommendedValue = !recommendedValue ? (configProperty.value || "") : recommendedValue;
    var re = new RegExp(regex);
    
   
      recommendedValue = recommendedValue.replace(re, replaceWith);
    
    
    
    configProperty.recommendedValue = recommendedValue;
    let value = !get(configProperty, "recommendedValue")
      ? ""
      : recommendedValue;
    
    // If final value is empty/undefined, fall back to current value
    if (!value && configProperty.value) {
      value = configProperty.value;
      configProperty.recommendedValue = configProperty.value;
      
    }
      
    
    configProperty.value = value;
    configProperty.initialValue = value;
    return configProperty;
  }
  const initialValue = (
    configPropertyCopy: any,
    localDB: any,
    dependencies: any
  ) => {
    let configProperty = cloneDeep(configPropertyCopy);
    var configName = get(configProperty, "propertyName");
    var definedInitializers: any = initializers;
    var initializer = definedInitializers[configName];
    if (initializer) {
      return _defaultInitializer(configProperty, localDB, dependencies);
    }

    var uniqueInitializerName: any = uniqueInitializers[configName];
    if (uniqueInitializerName && typeof obj[uniqueInitializerName] === 'function') {
      return obj[uniqueInitializerName](configProperty, localDB, dependencies);
    }

  
    configProperty = {
      ...configProperty,
      ...{
        initialValue: get(configProperty, "value"),
      },
    };
    return configProperty;
  };
  return {
    initialValue,
  };
}

export default WizardConfigInitializer;
