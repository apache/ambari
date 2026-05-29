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

import { kerberosDescriptorProperties } from "./kerberos_descriptor_properties";
import { normalizeName } from "../Hosts/helpers";

type ConfigObject = {
  recommendedValue: any;
  initialValue: any;
  defaultValue: any;
  value: any;
  filename: any;
  name: any;
  referenceProperty?: string;
  isEditable?: boolean;
  displayName?: string;
  index?: number;
};

type KerberosIdentity = {
  name: string;
  displayName: string;
  category: string;
  filename: string;
  index: number;
};

/**
 *
 * @param {object[]} items - stack descriptor json response
 * @returns {configs[]}
 */
export function ServicesStackDescriptorConfigs(items: any, kerberosIdentitiesMap: Record<string, KerberosIdentity>, configId: (name: string, filename: string) => string) {
  let configs: any = [];
  let clusterConfigs: any = [];
  const descriptor = items.KerberosDescriptor.kerberos_descriptor;

  if (!descriptor) {
    return;
  }

  clusterConfigs = clusterConfigs.concat(
    expandKerberosStackDescriptorProps(
      descriptor.properties,
      "Cluster",
      "stackConfigs"
    )
  );
  clusterConfigs = clusterConfigs.concat(
    createConfigsByIdentities(descriptor.identities, "Cluster", kerberosIdentitiesMap, configId)
  );

  descriptor.services.forEach((service: { name: any; components: any }) => {
    const serviceName = service.name;
    configs = configs.concat(createResourceConfigs(service, serviceName, kerberosIdentitiesMap, configId));
    (service.components || []).forEach((component: any) => {
      configs = configs.concat(createResourceConfigs(component, serviceName, kerberosIdentitiesMap, configId));
    });
  });

  configs = configs.concat(clusterConfigs);
  processConfigReferences(descriptor, configs);
  return configs;
}

/**
 * Wrap kerberos properties to App.ServiceConfigProperty model class instances.
 *
 * @param {object} kerberosProperties
 * @param {string} serviceName
 * @param {string} filename
 * @returns {object[]}
 */
export function expandKerberosStackDescriptorProps(
  kerberosProperties: any,
  serviceName: any,
  filename: any
): object[] {
  const configs = [];

  for (const propertyName in kerberosProperties) {
    if (kerberosProperties.hasOwnProperty(propertyName)) {
      const predefinedProperty = kerberosDescriptorProperties.find(
        (prop) => prop.name === propertyName
      );
      const value = kerberosProperties[propertyName];
      const isRequired = ["additional_realms", "principal_suffix"].includes(
        propertyName
      )
        ? false
        : value !== "";
      const propertyObject = {
        name: propertyName,
        value: value,
        defaultValue: value,
        recommendedValue: value,
        initialValue: value,
        serviceName: serviceName,
        filename: filename,
        displayName:
          serviceName === "Cluster"
            ? normalizeName(propertyName)
            : propertyName,
        isOverridable: false,
        isEditable: propertyName !== "realm",
        isRequired: isRequired,
        isVisible:true,
        isSecureConfig: true,
        placeholderText:
          predefinedProperty && predefinedProperty.index !== undefined
            ? predefinedProperty.placeholderText
            : "",
        index:
          predefinedProperty && predefinedProperty.index !== undefined
            ? predefinedProperty.index
            : Infinity,
      };
      configs.push(propertyObject);
    }
  }

  return configs;
}

/**
 * Create service properties based on component identity
 *
 * @param {object[]} identities
 * @param {string} serviceName
 * @returns {object[]}
 */
export function createConfigsByIdentities(
  identities: any,
  serviceName: string,
  kerberosIdentitiesMap: Record<string, KerberosIdentity>,
  configId: (name: string, filename: string) => string
): object[] {
  const configs: any[] = [];

  identities.forEach(
    (identity: { reference: any; name: string; principal: { type: any } }) => {
      const defaultObject = {
        isConfigIdentity: true,
        isOverridable: false,
        isVisible: !Boolean(
          identity.reference || identity.name.startsWith("/")
        ),
        isSecureConfig: true,
        serviceName: serviceName,
        name: identity.name,
        identityType: identity.principal && identity.principal.type,
      };

      parseIdentityObject(identity, kerberosIdentitiesMap, configId).forEach((item) => {
        configs.push({ ...defaultObject, ...item });
      });
    }
  );

  return configs;
}

/**
 * Bootstrap base object according to identity info. Generate objects will be converted to
 * configuration properties.
 *
 * @param {object} identity
 * @returns {object[]}
 */
export function parseIdentityObject(identity: any, kerberosIdentitiesMap: Record<string, KerberosIdentity>, 
    configId: (name: string, fileName: string) => string
): object[] {
  const result: {
    recommendedValue: any;
    initialValue: any;
    defaultValue: any;
    value: any;
    filename: any;
    name: any;
  }[] = [];
  const name = identity.name;

  Object.keys(identity)
    .filter((key) => key !== "name")
    .forEach((item) => {
      const prop = identity[item];

      const key = { keytab: 'file', principal: 'value' }[item];
      const itemValue = key ? prop[key] : undefined;
      
      if (!prop.configuration && !itemValue) return;

      const configObject: ConfigObject = {
        recommendedValue: itemValue,
        initialValue: itemValue,
        defaultValue: itemValue,
        value: itemValue,
        filename: prop.configuration
          ? prop.configuration.split("/")[0]
          : "cluster-env",
        name: prop.configuration
          ? prop.configuration.split("/")[1]
          : `${name}_${item}`,
      };

      if (name.startsWith("/") && !itemValue) {
        configObject.referenceProperty = `${name.substring(1)}:${item}`;
        configObject.isEditable = false;
      }

      configObject.displayName = getDisplayNameForConfig(
        configObject.name,
        configObject.filename,
        kerberosIdentitiesMap,
        configId
      );
      const predefinedProperty = kerberosDescriptorProperties.find(
        (prop) => prop.name === configObject.name
      );
      configObject.index =
        predefinedProperty && predefinedProperty.index !== undefined
          ? predefinedProperty.index
          : Infinity;

      result.push(configObject);
    });

  return result;
}

/**
 *
 * @param {Object} resource
 * @param {String} serviceName
 * @return {Array}
 */
export function createResourceConfigs(
  resource: any,
  serviceName: string,
  kerberosIdentitiesMap: Record<string, KerberosIdentity>,
  configId: (name: string, filename: string) => string
): Array<any> {
  let identityConfigs: any[] = [];
  let resourceConfigs: any[] = [];


  if (resource.identities) {
    identityConfigs = createConfigsByIdentities(
      resource.identities,
      serviceName,
      kerberosIdentitiesMap,
      configId
    );
  }

  if (resource.configurations) {
    resource.configurations.forEach((_configuration: any) => {
      for (const key in _configuration) {
        if (_configuration.hasOwnProperty(key)) {
          resourceConfigs = resourceConfigs.concat(
            expandKerberosStackDescriptorProps(
              _configuration[key],
              serviceName,
              key
            )
          );
        }
      }
    });
  }

  return identityConfigs.concat(resourceConfigs);
}

/**
 * Take care about configs that should observe value from referenced configs.
 * Reference is set with `referenceProperty` key.
 *
 * @param {object[]} kerberosDescriptor
 * @param {object[]} configs
 */
export function processConfigReferences(
  kerberosDescriptor: { identities: any; services: any[] },
  configs: any[]
) {
  let identities = kerberosDescriptor.identities;

  /**
   * Returns identity object with additional attribute `referencePath`.
   * Reference path depends on how deep identity is. Each level separated by `/` sign.
   *
   * @param {object} identity
   * @param {string} [prefix=false] prefix to append e.g., 'SERVICE_NAME'
   * @returns {object} identity object
   */
  const setReferencePath = (
    identity: { name: string; referencePath: any },
    prefix: string | undefined
  ) => {
    let name = identity?.name || false;
    if (name) {
      if (prefix) {
        name = `${prefix}/${name}`;
      }
      identity.referencePath = name;
    }
    return identity;
  };

  // Map all identities and add the `referencePath` attribute
  identities = identities
    .map((i: { name: string; referencePath: any }) =>
      setReferencePath(i, undefined)
    )
    .concat(
      kerberosDescriptor.services
        .map((service) => {
          const serviceName = service?.name || false;
          const serviceIdentities = (service?.identities || []).map(
            (i: { name: string; referencePath: any }) =>
              setReferencePath(i, serviceName)
          );
          const componentIdentities = (service?.components || [])
            .map((component: { name: boolean; identities: any }) => {
              const componentName = component?.name || false;
              return (component?.identities || []).map(
                (identity: { name: string; referencePath: any }) =>
                  setReferencePath(identity, `${serviceName}/${componentName}`)
              );
            })
            .reduce((p: string | any[], c: any) => p.concat(c), []);
          return serviceIdentities.concat(componentIdentities);
        })
        .reduce((p, c) => p.concat(c), [])
    )
    .filter((identity: undefined) => identity !== undefined);

  configs.forEach((item) => {
    const reference = item.referenceProperty;
    if (reference) {
      const [referenceName, referenceKey] = reference.split(":");
      const identity =
        identities.find(
          (identity: { name: any }) => identity.name === referenceName
        )?.[referenceKey] ||
        identities.find(
          (identity: { referencePath: any }) =>
            identity.referencePath === referenceName
        )?.[referenceKey];

      if (identity?.configuration) {
        item.observesValueFrom = identity.configuration.split("/")[1];
      } else {
        item.observesValueFrom = reference.replace(":", "_");
      }
    }
  });
}

export function getDisplayNameForConfig(
  name: string,
  fileName: string,
  kerberosIdentitiesMap: Record<string, KerberosIdentity>,
  configId: (name: string, filename: string) => string
): string {
  const predefinedConfig = kerberosIdentitiesMap[configId(name, fileName)];
  return predefinedConfig && predefinedConfig.displayName
    ? predefinedConfig.displayName
    : fileName === "cluster-env.xml"
    ? normalizeName(name)
    : name;
}
