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

type ConfigProperty = {
  name: string;
  value: number | string;
  recommendedValue?: number | string;
  initialValue?: number | string;
};

type Initializer = {
  type: string;
  isChecker?: boolean;
  port?: number;
};

type LocalDB = {
  masterComponentHosts: { component: string; hostName: string }[];
};

type Dependencies = Record<string, any>;

type InitializerType = {
  name: string;
  method: (
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies,
    initializer: Initializer
  ) => ConfigProperty | number;
};

function RmHaConfigInitializer() {
  const initializerFlowCode = {
    next: 0,
    skipNext: 1,
    skipAll: 2,
  };

  let initializers:any = generateInitializers();
  let uniqueInitializers: Record<
    string,
    (
      configProperty: ConfigProperty,
      localDB: LocalDB,
      dependencies: Dependencies
    ) => ConfigProperty
  > = {};

  let copyInitializers: typeof initializers | null = null;
  let copyUniqueInitializers: typeof uniqueInitializers | null = null;

  const initializerTypes: InitializerType[] = [
    { name: "rm_hosts_with_port", method: initRmHaHostsWithPort },
    { name: "host_with_port", method: initAsHostWithPort },
    { name: "hosts_with_port", method: initAsHostsWithPort },
  ];

  function initAsHostWithPort(
    config: ConfigProperty,
    localDB: any,
    dependencies: any,
    initializer: any
  ): ConfigProperty {
    const hostName = localDB.masterComponentHosts.find(
      (host:any) =>
        host.component === initializer.component &&
        host.isInstalled === initializer.componentExists
    )?.hostName;
    const port = initializer.portKey
      ? dependencies[initializer.portKey]
      : initializer.port;
    config.value =
      initializer.modifier.prefix +
      (hostName || "") +
      (port ? ":" + port : "") +
      initializer.modifier.suffix;
    config.recommendedValue = config.value;
    return config;
  }

  function initAsHostsWithPort(
    config: ConfigProperty,
    localDB: any,
    dependencies: any,
    initializer: any
  ): ConfigProperty {
    const hostNames = localDB.masterComponentHosts
      .filter(
        (host:any) =>
          initializer.component.includes(host.component) &&
          (initializer.componentExists === undefined ||
            host.isInstalled === initializer.componentExists)
      )
      .map((host:any) => host.hostName);
    const port = initializer.portKey
      ? dependencies[initializer.portKey]
      : initializer.port;
    config.value =
      initializer.modifier.prefix +
      hostNames
        .map((host:any) => host + (port ? ":" + port : ""))
        .join(initializer.modifier.delimiter) +
      initializer.modifier.suffix;
    config.recommendedValue = config.value;
    return config;
  }

  function getHostWithPortConfig(
    component: string,
    componentExists: boolean,
    prefix: string = "",
    suffix: string = "",
    port: string,
    portFromDependencies: boolean = false
  ): any {
    return {
      type: "host_with_port",
      component: component,
      componentExists: componentExists,
      modifier: {
        prefix: prefix,
        suffix: suffix,
      },
      portKey: portFromDependencies ? port : undefined,
      port: portFromDependencies ? undefined : port,
    };
  }

  function getHostsWithPortConfig(
    component: string | string[],
    prefix: string = "",
    suffix: string = "",
    delimiter: string = ",",
    port: string,
    portFromDependencies: boolean = false
  ): any {
    return {
      type: "hosts_with_port",
      component: component,
      modifier: {
        prefix: prefix,
        suffix: suffix,
        delimiter: delimiter,
      },
      portKey: portFromDependencies ? port : undefined,
      port: portFromDependencies ? undefined : port,
    };
  }

  function generateInitializers() {
    return {
      "yarn.resourcemanager.hostname.rm1": getHostWithPortConfig(
        "RESOURCEMANAGER",
        true,
        "",
        "",
        ""
      ),
      "yarn.resourcemanager.hostname.rm2": getHostWithPortConfig(
        "RESOURCEMANAGER",
        false,
        "",
        "",
        ""
      ),
      "yarn.resourcemanager.zk-address": getHostsWithPortConfig(
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "yarn.resourcemanager.resource-tracker.address.rm1":
        getHostWithPortConfig(
          "RESOURCEMANAGER",
          true,
          "",
          "",
          "trackerAddressPort",
          true
        ),
      "yarn.resourcemanager.resource-tracker.address.rm2":
        getHostWithPortConfig(
          "RESOURCEMANAGER",
          false,
          "",
          "",
          "trackerAddressPort",
          true
        ),
      "yarn.resourcemanager.webapp.address.rm1": getHostWithPortConfig(
        "RESOURCEMANAGER",
        true,
        "",
        "",
        "webAddressPort",
        true
      ),
      "yarn.resourcemanager.webapp.address.rm2": getHostWithPortConfig(
        "RESOURCEMANAGER",
        false,
        "",
        "",
        "webAddressPort",
        true
      ),
      "yarn.resourcemanager.webapp.https.address.rm1": getHostWithPortConfig(
        "RESOURCEMANAGER",
        true,
        "",
        "",
        "httpsWebAddressPort",
        true
      ),
      "yarn.resourcemanager.webapp.https.address.rm2": getHostWithPortConfig(
        "RESOURCEMANAGER",
        false,
        "",
        "",
        "httpsWebAddressPort",
        true
      ),
      "yarn.resourcemanager.ha": getRmHaHostsWithPort(8032),
      "yarn.resourcemanager.scheduler.ha": getRmHaHostsWithPort(8030),
      "hadoop.proxyuser.{{yarnUser}}.hosts": getComponentsHostsConfig([
        "RESOURCEMANAGER",
      ]),
    };
  }

  function getRmHaHostsWithPort(port: number): Initializer {
    return { type: "rm_hosts_with_port", port };
  }

  function getComponentsHostsConfig(_components: string[]): any {
    return {}; // Placeholder for actual logic
  }

  function setup(settings: Record<string, string | number>) {
    copyInitializers = structuredClone(initializers);
    copyUniqueInitializers = structuredClone(uniqueInitializers);

    initializers = updateNames(initializers, settings) as any;
    uniqueInitializers = updateNames(uniqueInitializers, settings);
  }

  function cleanup() {
    if (copyInitializers) initializers = structuredClone(copyInitializers);
    if (copyUniqueInitializers)
      uniqueInitializers = structuredClone(copyUniqueInitializers);
  }

  function initialValue(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const configName = configProperty.name;
    const initializer = initializers[configName];

    if (initializer) {
      return defaultInitializer(
        configProperty,
        localDB,
        dependencies,
        initializer
      );
    }

    const uniqueInitFn = uniqueInitializers[configName];
    if (uniqueInitFn) {
      return uniqueInitFn(configProperty, localDB, dependencies);
    }

    configProperty.initialValue = configProperty.value;
    return configProperty;
  }

  function defaultInitializer(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies,
    initializer: Initializer | Initializer[]
  ) {
    const initArray = Array.isArray(initializer) ? initializer : [initializer];

    for (let i = 0; i < initArray.length; i++) {
      const init = initArray[i];
      const typeDef = initializerTypes.find((t) => t.name === init.type);
      if (!typeDef) continue;

      if (init.isChecker) {
        const result = typeDef.method(
          configProperty,
          localDB,
          dependencies,
          init
        );
        if (result === initializerFlowCode.skipNext) i++;
        if (result === initializerFlowCode.skipAll) break;
      } else {
        configProperty = typeDef.method(
          configProperty,
          localDB,
          dependencies,
          init
        ) as ConfigProperty;
      }
    }
    return configProperty;
  }

  function initRmHaHostsWithPort(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    _dependencies: Dependencies,
    initializer: Initializer
  ): ConfigProperty {
    const rmHosts = localDB.masterComponentHosts
      .filter((h) => h.component === "RESOURCEMANAGER")
      .map((h) => `${h.hostName}:${initializer.port}`);

    const value = rmHosts.join(",");
    configProperty.value = value;
    configProperty.recommendedValue = value;
    return configProperty;
  }

  function updateNames(
    source: Record<string, any>,
    settings: Record<string, string | number>
  ): Record<string, any> {
    const updated: Record<string, any> = {};
    Object.keys(source).forEach((key) => {
      let newKey = key;
      Object.keys(settings).forEach((sKey) => {
        newKey = newKey.replace(`{{${sKey}}}`, String(settings[sKey]));
      });
      updated[newKey] = source[key];
    });
    return updated;
  }

  return {
    setup,
    cleanup,
    initialValue,
    flowNext: () => initializerFlowCode.next,
    flowSkipNext: () => initializerFlowCode.skipNext,
    flowSkipAll: () => initializerFlowCode.skipAll,
  };
}

export default RmHaConfigInitializer;