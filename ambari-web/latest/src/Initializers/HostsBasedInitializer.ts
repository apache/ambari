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

import {ExtendedTopologyLocalDB, NnHaConfigDependencies} from "../Utils/configs.ts";

type ConfigProperty = {
  value: string;
  recommendedValue: string;
};

type TopologyLocalDB = {
  masterComponentHosts: {
    component: string;
    hostName: string;
    isInstalled?: boolean;
  }[];
};

type Initializer = {
  component: string | string[];
  componentExists?: boolean;
  modifier?: {
    prefix?: string;
    suffix?: string;
    delimiter?: string;
    regex?: string;
  };
  asArray?: boolean;
  components?: string[];
  portKey?: string;
  port?: string;
};

type Dependencies = Record<string, string | number>;

export class HostsBasedInitializer {
  static _initAsHostWithPort(
    configProperty: ConfigProperty,
    localDB: TopologyLocalDB,
    dependencies: Dependencies,
    initializer: Initializer
  ): ConfigProperty {
    const host = localDB.masterComponentHosts
      .filter((host) => host.component === initializer.component)
      .find((host) => host.isInstalled === initializer.componentExists);

    const hostName = host?.hostName || "";
    const port = HostsBasedInitializer.__getPort(dependencies, initializer);
    const value =
      (initializer.modifier?.prefix || "") +
      hostName +
      (port ? `:${port}` : "") +
      (initializer.modifier?.suffix || "");

    configProperty.value = value;
    configProperty.recommendedValue = value;

    return configProperty;
  }

  static getComponentsHostsConfig(
    components: string | string[],
    asArray: boolean = false,
    isInstalled?: boolean
  ) {
    return {
      type: "hosts_with_components",
      components: Array.isArray(components) ? components : [components],
      asArray,
      isInstalled,
    };
  }

  static getSimpleComponentConfig(
    component: string,
    withModifier: boolean = true
  ) {
    const config: any = {
      type: "host_with_component",
      component,
    };
    if (withModifier) {
      config.modifier = {
        type: "regexp",
        regex: "([\\w|\\.]*)(?=:)",
      };
    }
    return config;
  }

  static getComponentConfigWithAffixes(
    component: string,
    prefix: string = "",
    suffix: string = ""
  ) {
    return {
      type: "host_with_component",
      component,
      modifier: {
        type: "regexp",
        regex: ":\\/\\/([\\w|\\.]*)(?=:)",
        prefix,
        suffix,
      },
    };
  }

  static getHostWithPortConfig(
    component: string,
    componentExists: boolean,
    prefix: string = "",
    suffix: string = "",
    port: string,
    portFromDependencies: boolean = false
  ) {
    const config: any = {
      type: "host_with_port",
      component,
      componentExists,
      modifier: {
        prefix,
        suffix,
      },
    };
    if (portFromDependencies) {
      config.portKey = port;
    } else {
      config.port = port;
    }
    return config;
  }

  static getHostsWithPortConfig(
    component: string,
    prefix: string = "",
    suffix: string = "",
    delimiter: string = ",",
    port: string,
    portFromDependencies: boolean = false
  ): {
    type: string;
    component: string;
    modifier: {
      prefix: string;
      suffix: string;
      delimiter: string;
    };
    portKey?: string;
    port?: string;
    componentExists?: boolean | undefined;
  } {
    const ret: {
      type: string;
      component: string;
      modifier: {
        prefix: string;
        suffix: string;
        delimiter: string;
      };
      portKey?: string;
      port?: string;
      componentExists?: boolean | undefined;
    } = {
      type: "hosts_with_port",
      component,
      modifier: {
        prefix: prefix || "",
        suffix: suffix || "",
        delimiter: delimiter || ",",
      },
    };

    if (portFromDependencies) {
      ret.portKey = port;
    } else {
      ret.port = port;
    }

    return ret;
  }

  static _initAsHostWithComponent(
    configProperty: ConfigProperty,
    localDB: TopologyLocalDB,
    //@ts-ignore
    dependencies: Dependencies,
    initializer: Initializer
  ): ConfigProperty {
    const component = localDB.masterComponentHosts.find(
      (host) => host.component === initializer.component
    );
    if (!component) {
      return configProperty;
    }
    if (initializer.modifier) {
      const replaceWith =
        (initializer.modifier.prefix || "") +
        component.hostName +
        (initializer.modifier.suffix || "");
      configProperty.value = replaceWith;
      configProperty.recommendedValue = replaceWith;
    } else {
      configProperty.value = component.hostName;
      configProperty.recommendedValue = component.hostName;
    }
    return configProperty;
  }

  static _initAsHostsWithComponents(
    configProperty: ConfigProperty,
    localDB: TopologyLocalDB,
    //@ts-ignore
    dependencies: Dependencies,
    initializer: Initializer
  ): ConfigProperty {
    // const hostNames = localDB.masterComponentHosts
    //     .filter((host) => initializer.components?.includes(host.component))
    //     //@ts-ignore
    //     .filter((host) => initializer.isInstalled === undefined || host.isInstalled === initializer.isInstalled)
    //     .map((host) => host.hostName)
    //     .sort();

    const hostNames = Array.from(
      new Set(
        localDB.masterComponentHosts
          .filter((host) => initializer.components?.includes(host.component))
          //@ts-ignore
          .filter(
            (host) =>
              //@ts-ignore
              initializer.isInstalled === undefined ||
              //@ts-ignore
              host.isInstalled === initializer.isInstalled
          )
          .map((host) => host.hostName)
      )
    ).sort();

    const value = initializer.asArray ? hostNames : hostNames.join(",");
    //@ts-ignore
    configProperty.value = value;
    //@ts-ignore
    configProperty.recommendedValue = value;
    return configProperty;
  }

  static _initAsHostsWithPort(
    configProperty: ConfigProperty,
    localDB: TopologyLocalDB,
    dependencies: Dependencies,
    initializer: Initializer
  ): ConfigProperty {
    const hostNames = localDB.masterComponentHosts
      .filter((host) => host.component === initializer.component)
      .filter(
        (host) =>
          initializer.componentExists === undefined ||
          host.isInstalled === initializer.componentExists
      )
      .map((host) => host.hostName);

    const port = HostsBasedInitializer.__getPort(dependencies, initializer);
    const value =
      (initializer.modifier?.prefix || "") +
      hostNames
        .map((hostName) => `${hostName}${port ? `:${port}` : ""}`)
        .join(initializer.modifier?.delimiter || ",") +
      (initializer.modifier?.suffix || "");

    configProperty.value = value;
    configProperty.recommendedValue = value;
    return configProperty;
  }

  //@ts-ignore
  static _initAsHostsListWithComponent(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: any): ConfigProperty {
    const hostNames = localDB.masterComponentHosts
        .filter(host => host.component === initializer.component && host.isInstalled === initializer.componentExists)
        .map(host => host.hostName)
        .join(initializer.modifier.delimiter);
    config.value = hostNames;
    config.recommendedValue = hostNames;
    return config;
  }

  static getHostsListComponentConfig(
    component: string,
    componentExists: boolean,
    delimiter?: string
  ): {
    type: string;
    component: string;
    componentExists: boolean;
    modifier: {
      delimiter: string;
    };
  } {
    return {
      type: "hosts_list_with_component",
      component: component,
      componentExists: componentExists,
      modifier: {
        delimiter: delimiter == null ? "," : delimiter, // Check for null or undefined
      },
    };
  }

  static __getPort(
    dependencies: Dependencies,
    initializer: Initializer
  ): string | number | undefined {
    return initializer.portKey
      ? dependencies[initializer.portKey]
      : initializer.port;
  }
}
