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

import { useContext, useEffect, useState } from "react";
import KerberosApi from "../../api/kerberosApi";
import { ServicesStackDescriptorConfigs } from "../Kerberos/addSecurityConfigs";
import useKerberosConfigs from "../Kerberos/useKerberosConfigs";
import { get, isEmpty, cloneDeep } from "lodash";
import { Button, Tab, Tabs } from "react-bootstrap";
import AdvancedConfigs from "../CommonConfigs/AdvancedConfigs";
import Spinner from "../../components/Spinner";
import { AppContext } from "../../store/context";
import { getConfigTagFromFileName, getTotalErros } from "../CommonConfigs/ConfigUtils";

interface ConfigProperties {
    propertyName: string;
    propertyDisplayname: string;
    propertyDescription: string;
    propertyValue: string;
    propertyAttributes: {
        type: string;
        overridable: boolean;
    };
    previousValue: string;
    value: string;
    final: string;
    propertyDisplayValue?: string;
    errorMessages?: string[];
    confirmPassword?: boolean;
}

interface CategoryProperties {
    errors: number;
    properties: { [key: string]: ConfigProperties };
}

interface KerberosConfigProperties {
    [key: string]: { [key: string]: CategoryProperties };
}

type ConfigCategory = {
  name: string;
  displayName: string;
};

type StepConfig = {
  displayName: string;
  name: string;
  serviceName: string;
  configCategories: ConfigCategory[];
  configs: any[];
  configGroups: never[];
  showConfig: boolean;
};


function KerberosIdentities() {

  const [ loading, setLoading ] = useState(false);
  const { configId, kerberosIdentitiesMap } = useKerberosConfigs();
  const [ result, setResult ] = useState<Record<string, any>>({})
  const [ tabErrors, setTabErrors ] = useState({});
  const [ stepConfigs, setStepConfigs] = useState<StepConfig[]>([]);
  const [isEditMode,setIsEditMode] = useState<boolean>(false);
  const { clusterName, stackConfigurations, services } = useContext(AppContext);
  const [ saveEnabled, setSaveEnabled ] = useState<boolean>(false);


  useEffect(() => {
   const noErros = getTotalErros(tabErrors);
   setSaveEnabled(noErros);
  
  }, [tabErrors]);

  useEffect(() => {
    async function getKerberosIdentities() {
      setLoading(true);
      const kerberosDescriptor =
        await KerberosApi.getKerberosDescriptorProperties("true", clusterName);

      const stepConfigs = ServicesStackDescriptorConfigs(
        kerberosDescriptor,
        kerberosIdentitiesMap,
        configId
      );
      
      const kerberosConfigs = getStepConfigs(stepConfigs);
      setStepConfigs(kerberosConfigs);

      const result = transformData(kerberosConfigs)
      setResult(result)
      setLoading(false);
    }
    getKerberosIdentities();
  }, [isEditMode]);

  const transformData = (data: any[]): KerberosConfigProperties => {
    const kerberosConfigProperties: KerberosConfigProperties = {};

    data.forEach(section => {
      const serviceName = get(section, 'serviceName', '');
      if (!kerberosConfigProperties[serviceName]) {
        kerberosConfigProperties[serviceName] = {};
      }
  
      const configCategories = get(section, 'configCategories', []);
      configCategories.forEach((category: any) => {
        const categoryName = get(category, 'name', '');
        if(categoryName === 'KERBEROS') return;

        if (!kerberosConfigProperties[serviceName][categoryName]) {
          kerberosConfigProperties[serviceName][categoryName] = {
            errors: 0,
            properties: {}
          };
        }
  
        const configs = get(section, 'configs', []);
        configs.forEach((config: { propertyDisplayValue: any; errorMessages: any; confirmPassword: any; }) => {
          if (get(config, 'category', '') === categoryName && get(config,"isVisible",false)) {
            let name = get(config, 'name', '');
            kerberosConfigProperties[serviceName][categoryName].properties[name] = {
              propertyName: name,
              propertyDisplayname: get(config, 'displayName', ''),
              propertyDescription: get(config, 'description', ''),
              propertyValue: get(config, 'value', ''),
              propertyAttributes: {
                type: "string",
                overridable: get(config, 'isOverridable', false),
              },
              previousValue: get(config, 'previousValue', ''),
              value: get(config, 'value', ''),
              final: get(config, 'final', 'false'),
              ...(config.propertyDisplayValue && { propertyDisplayValue: config.propertyDisplayValue }),
              ...(config.errorMessages && { errorMessages: config.errorMessages }),
              ...(config.confirmPassword && { confirmPassword: config.confirmPassword }),
              isEditable: isEditMode && name !== "realm"
            };
          }
        });
      });
    });

    return kerberosConfigProperties;
  };
  

  function getStepConfigs(configs: any) {
    var configProperties = prepareConfigProperties(configs);

    configProperties = sortConfigs(configProperties);
    const newStepConfigs = createServiceConfig(configProperties);
    return newStepConfigs;
  }

  function prepareConfigProperties(configs: any) {
    let installedServiceNames = ["Cluster", "AMBARI"].concat(
      services.map((service: any) => service.ServiceInfo?.service_name)
    );
    let configPropertiesCopy = cloneDeep(configs);
    let siteProperties = stackConfigurations;

    configPropertiesCopy = configPropertiesCopy.filter(
      (item: { serviceName: string }) =>
        installedServiceNames.includes(item.serviceName)
    );


    configPropertiesCopy.forEach(
      (property: {
        isSecureConfig: boolean;
        name: string;
        observesValueFrom: any;
        value: any;
        recommendedValue: any;
        isVisible: boolean;
        category: string;
        serviceName: string;
        identityType: string;
        displayName: string | undefined;
        index: number;
        displayType: string;
      }) => {
        property.isSecureConfig = false;
        if (["spnego_keytab", "spnego_principal"].includes(property.name)) {
          // Add observer logic if needed, alternative ways in React might be different
        }
        if (property.observesValueFrom) {
          const observedConfig = configPropertiesCopy.find(
            (config: { name: any }) =>
              config.name === property.observesValueFrom
          );
          if (observedConfig) {
            property.value = observedConfig.value;
            property.recommendedValue = observedConfig.value;
            property.isVisible = true;
          }
        }
        property.category =
          property.serviceName === "Cluster" ? "Global" : property.serviceName;

        if (property.identityType === "user") {
          property.category = "Ambari Principals";
        }

        const siteProperty = siteProperties.find(
          (prop: any) => prop.name === property.name
        );
        if (siteProperty) {
          if (siteProperty.category === property.category) {
            property.displayName = siteProperty.displayName;
            if (siteProperty.index) {
              property.index = siteProperty.index;
            }
          }
          if (siteProperty.displayType) {
            property.displayType = siteProperty.displayType;
          }
        }
      }
    );

    return configPropertiesCopy;
  }

  function createServiceConfig(configs: any): StepConfig[] {
    const clusterConfigs = configs.filter(
      (config: { serviceName: string }) => config.serviceName === "Cluster"
    );

    // storm user principal is not required for ambari operation
    const userConfigs = configs.filter(
      (config: { identityType: string }) => config.identityType === "user"
    );

    const combinedConfigs = [...clusterConfigs, ...userConfigs];
    const configMap = new Map(
      combinedConfigs.map((config) => [config.name, config])
    );
    const generalConfigs = Array.from(configMap.values());

    const advancedConfigs = configs.filter(
      (element: { name: any }) => !generalConfigs.includes(element.name)
    );

    const categoryForGeneralConfigs = [
      { name: "Global", displayName: "Global" },
      { name: "Ambari Principals", displayName: "Ambari Principals" },
    ];

    const categoryForAdvancedConfigs = createCategoryForServices();

    return [
      {
        displayName: "General",
        name: "GENERAL",
        serviceName: "KERBEROS_GENERAL",
        configCategories: categoryForGeneralConfigs,
        configs: generalConfigs,
        configGroups: [],
        showConfig: true,
      },
      {
        displayName: "Advanced",
        name: "ADVANCED",
        serviceName: "KERBEROS_ADVANCED",
        configCategories: categoryForAdvancedConfigs,
        configs: advancedConfigs,
        configGroups: [],
        showConfig: true,
      },
    ];
  }

  function createCategoryForServices() {
    var services1 = services;
    return services1.map((item) => ({
      name: item.ServiceInfo?.service_name,
      displayName: item.ServiceInfo?.service_name,
      collapsedByDefault: true,
    }));
  }

  function sortConfigs(configs: any[]) {
    return configs.sort(
      (
        a: { index: number; name: number },
        b: { index: number; name: number }
      ) => {
        if (a.index > b.index) return 1;
        if (a.index < b.index) return -1;
        if (a.name > b.name) return 1;
        if (a.name < b.name) return -1;
        return 0;
      }
    );
  }

  /**
   * This function updates stack/service/component level kerberos descriptor identities (principal and keytab)
   * with the values entered by the user on the rendered UI.
   * @param {Array} identities
   * @param {Object} config
   * @return {boolean}
   */
  const updateDescriptorIdentityConfig = (identities: any[], config: any) => {
    let isConfigUpdated = false;
  
    const updatedIdentities = identities.map((identity) => {
      const updatedIdentity = cloneDeep(identity);
      const keys = Object.keys(identity).filter((key) => key !== 'name');
  
      keys.forEach((item) => {
        const prop = updatedIdentity[item];
  
        // Compare UI rendered config against identity with `configuration attribute` (Most of the identities have `configuration attribute`)
        const isIdentityWithConfig = (
          prop.configuration &&
          prop.configuration.split('/')[0] === getConfigTagFromFileName(config.filename) &&
          prop.configuration.split('/')[1] === config.name
        );
  
        // Compare UI rendered config against identity without `configuration attribute` (For example spnego principal and keytab)
        const isIdentityWithoutConfig = (
          !prop.configuration &&
          identity.name === config.name.split('_')[0] &&
          item === config.name.split('_')[1]
        );
  
        if (isIdentityWithConfig || isIdentityWithoutConfig) {
          updatedIdentity[item] = { ...prop, [item === 'keytab' ? 'file' : 'value']: config.value };
          isConfigUpdated = true;
        }
      });
  
      return updatedIdentity;
    });
  
    return { isConfigUpdated, updatedIdentities };
  };
  
  const updateDescriptorConfigs = (configurations: any, config: any) => {
    let isConfigUpdated = false;
  
    if (configurations) {
      if (Array.isArray(configurations)) {
        configurations.forEach((configuration) => {
          for (const key in configuration) {
            if (configuration[key].hasOwnProperty(config.name) && getConfigTagFromFileName(config.filename) === key) {
              configuration[key][config.name] = config.value;
              isConfigUpdated = true;
            }
          }
        });
      } else if (configurations.hasOwnProperty(config.name) && getConfigTagFromFileName(config.filename) === 'stackConfigs') {
        configurations[config.name] = config.value;
        isConfigUpdated = true;
      }
    }
  
    return isConfigUpdated;
  };
  
  const updateResourceIdentityConfigs = (resource: any, config: any, isStackResource = false) => {
    let isConfigUpdated;
    const identities = resource.identities;
    const properties = isStackResource ? resource.properties : resource.configurations;
    isConfigUpdated = updateDescriptorConfigs(properties, config);
  
    let updatedResource = cloneDeep(resource);
  
    if (!isConfigUpdated && identities) {
      const { isConfigUpdated: identityUpdated, updatedIdentities } = updateDescriptorIdentityConfig(identities, config);
      isConfigUpdated = identityUpdated;
      if (identityUpdated) {
        updatedResource = {
          ...resource,
          identities: updatedIdentities
        };
      }
    }
  
    return { isConfigUpdated, updatedResource };
  };
  
  const updateKerberosDescriptor = (kerberosDescriptor: any, configs: any[]) => {
    let updatedKerberosDescriptor = cloneDeep(kerberosDescriptor);
  
    configs.forEach((config) => {
      let isConfigUpdated;
      const isStackResource = true;
      let result = updateResourceIdentityConfigs(updatedKerberosDescriptor, config, isStackResource);
      isConfigUpdated = result.isConfigUpdated;
      updatedKerberosDescriptor = result.updatedResource;
  
      if (!isConfigUpdated) {
        updatedKerberosDescriptor.services = updatedKerberosDescriptor.services.map((service: any) => {
          let result = updateResourceIdentityConfigs(service, config);
          isConfigUpdated = result.isConfigUpdated;
          let updatedService = result.updatedResource;
  
          if (!isConfigUpdated) {
            updatedService.components = (service.components || []).map((component: any) => {
              let result = updateResourceIdentityConfigs(component, config);
              isConfigUpdated = result.isConfigUpdated;
              return result.updatedResource;
            });
          }
  
          return updatedService;
        });
      }
    });
  
    return updatedKerberosDescriptor;
  };
  
  async function saveConfigurations() {
    const response = await KerberosApi.getKerberosDescriptorProperties("true", clusterName);
    const kerberosDescriptor = get(response, "KerberosDescriptor.kerberos_descriptor", []);
    const configs = stepConfigs.reduce((acc: any[], stepConfig: StepConfig) => acc.concat(stepConfig.configs), []);
    const updatedKerberosDescriptor = updateKerberosDescriptor(kerberosDescriptor, configs);
    
    const payload = {
      "artifact_data":
      updatedKerberosDescriptor
    }
    
    await KerberosApi.saveKerberosData(clusterName, payload);
  }     

  if(loading || isEmpty(result)) {
    return <Spinner />
  }

  return (
    <div className="mt-4">
        {!isEditMode && <div className="d-flex justify-content-end">
            <Button variant="secondary" disabled={isEditMode} onClick={()=>{
                setIsEditMode(true);
            }}>Edit</Button>
        </div>}
        <div className="mt-3">
            <Tabs defaultActiveKey="general">
                <Tab eventKey="general" title="General">
                    <AdvancedConfigs
                        configPropertiesData={result}
                        setConfigProperties={setResult}
                        chosenService={"KERBEROS_GENERAL"}
                        setTabErrors={setTabErrors}
                        displayUndoRedo={false}
                    />
                </Tab>
                <Tab eventKey="advanced" title="Advanced">
                    <AdvancedConfigs
                        configPropertiesData={result}
                        setConfigProperties={setResult}
                        chosenService={"KERBEROS_ADVANCED"}
                        setTabErrors  ={setTabErrors}
                        displayUndoRedo={false}
                    />
                </Tab>
            </Tabs>
        </div>
        {isEditMode && <div className="mt-3 d-flex justify-content-end">
            <Button className="mx-2" variant="light" onClick={()=>setIsEditMode(false)}>Discard</Button>
            <Button disabled={!saveEnabled} onClick={()=>saveConfigurations()}>Save</Button>
        </div>}
    </div>
  );
}

export default KerberosIdentities;
