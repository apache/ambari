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

import { useState, useContext, useRef } from "react";
import { serviceNameDisplayMapping, serviceNames } from "../constants";
import modalManager from "../store/ModalManager";
import ConfirmationModal from "../components/ConfirmationModal";
import { ServiceApi } from "../api/serviceApi";
import { AppContext } from "../store/context";
import { ambariApi } from "../api/config/axiosConfig";
import { get, isEmpty } from "lodash";
import { buildRecommendationsPayload } from "../Utils/Utility";
import Spinner from "../components/Spinner";
import ConfigsApi from "../api/configsApi";
import RecommendationModal from "../components/RecommendationModal";
import { ServiceContext } from "../store/ServiceContext";
import { Button, Modal } from "react-bootstrap";
import { ServicesApi } from "../api/servicesApi";
import OverlayBackdrop from "../components/OverlayBackdrop";
import toast from "react-hot-toast";

interface DeleteServiceResponse {
  Requests?: {
    id: string;
  };
}

interface ConfigTag {
  siteName: string;
  tagName: string;
}

interface SiteConfig {
  type: string;
  tag: string;
  properties: Record<string, any>;
  properties_attributes?: Record<string, Record<string, string>>;
}

//Make sure to only pass the services which are actually installed!!
//in the hook data
export function useServiceDeletion(
  serviceName: string,
  stackServicesFromHook: any,
  stackDataWithDependencies: any
) {
  const { clusterName, cluster, services, allHostNames } =
    useContext(AppContext);
  const { serviceModels } = useContext(ServiceContext);
  const vdpStackVersion = get(cluster, "version", "").split("-")[1];
  const [isRecommendationInProgress, setIsRecommendationInProgress] =
    useState(false);
  const changedPropertiesRef = useRef([] as ConfigProperty[]);
  const isShowWarningWithRecommendationsLoaded = useRef(false);

  isShowWarningWithRecommendationsLoaded.current = false;
  const configsForRecommendationsCompareRef = useRef({});
  const configDependentServiceNamesRef = useRef([] as string[]);
  const allConfigsWithCurrentValueForServicesRef = useRef([] as any);
  //@ts-ignore
  const installedServicesInCluster =
    stackServicesFromHook?.map((s: any) => s?.StackServices?.service_name) ||
    [];
  // const [enhancedStackServices, setEnhancedStackServices] = useState<any[]>([]);
  let rangerPluginEnabled = false;
  const enhancedStackServicesRef = useRef<any[]>([]);

  const getInterDependentServicesToBeDeleted = () => {
    if (
      stackServicesFromHook &&
      Array.isArray(stackServicesFromHook) &&
      stackServicesFromHook.length > 0
    ) {
      const interDependentServices: string[] = [];

      const currentService = stackServicesFromHook.find(
        (service: any) => service?.StackServices?.service_name === serviceName
      );

      if (currentService?.StackServices?.required_services) {
        const requiredServices = currentService.StackServices.required_services;

        // For each service that the current service requires
        requiredServices.forEach((requiredServiceName: string) => {
          const requiredService = stackServicesFromHook.find(
            (service: any) =>
              service?.StackServices?.service_name === requiredServiceName
          );

          if (
            requiredService?.StackServices?.required_services?.includes(
              serviceName
            )
          ) {
            interDependentServices.push(requiredServiceName);
          }
        });
      }

      return interDependentServices;
    } else {
      return [];
    }
  };

  const getServiceNamesToBeDeleted = (interDependentServices: any) => {
    return [serviceName].concat(interDependentServices);
  };

  const findDependentServices = (serviceNamesToDelete: string[]): string[] => {
    const dependentServices: string[] = [];

    if (!stackServicesFromHook || !Array.isArray(stackServicesFromHook)) {
      return dependentServices;
    }

    stackServicesFromHook.forEach((service: any) => {
      const currentServiceName = service?.StackServices?.service_name;

      // If current service is not in the list of services to be deleted
      if (
        currentServiceName &&
        !serviceNamesToDelete.includes(currentServiceName)
      ) {
        const requiredServices =
          service?.StackServices?.required_services || [];

        // Check if this service depends on any of the services to be deleted
        serviceNamesToDelete.forEach((dependsOnService: string) => {
          if (requiredServices.includes(dependsOnService)) {
            dependentServices.push(currentServiceName);
          }
        });
      }
    });

    return dependentServices;
  };

  const servicesDisplayNames = (serviceNames: string[]): string => {
    return serviceNames
      .map((serviceName: string) => {
        //@ts-ignore
        return serviceNameDisplayMapping[serviceName] || serviceName;
      })
      .join(",");
  };

  const isRangerPluginEnabled = (): boolean => {
    const rangerServiceModel = serviceModels["ranger"];
    if (
      rangerServiceModel.rangerHDFSPluginProperties === "Enabled" ||
      rangerServiceModel.rangerHbasePluginProperties === "Enabled" ||
      rangerServiceModel.rangerHivePluginProperties === "Enabled" ||
      rangerServiceModel.rangerYarnPluginProperties === "Enabled"
    ) {
      return true;
    }
    return false;
  };

  const kerberosDeleteWarning = () => {
    modalManager.show(
      <Modal
        show={true}
        onHide={() => modalManager.hide()}
        size="lg"
        className="custom-modal-container modal-width"
      >
        <Modal.Header>
          <Modal.Title>
            <h3>Delete Service</h3>
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div>
            <p>
              The Kerberos Service <i>cannot be deleted</i> because Kerberos is
              currently enabled, and in use by the cluster. The Kerberos Service
              can only be removed by disabling Kerberos, which can be found by
              browsing to <b>Admin &gt; Kerberos</b> and selecting{" "}
              <b>Disable Kerberos</b>.
            </p>
          </div>
        </Modal.Body>
        <Modal.Footer className="d-flex justify-content-end">
          <Button
            variant="outline-primary"
            onClick={() => {
              modalManager.hide();
              // Navigate to Kerberos admin page
              window.location.href = "/#/main/admin/kerberos";
            }}
          >
            GO TO KERBEROS
          </Button>
          <Button variant="success" onClick={() => modalManager.hide()}>
            OK
          </Button>
        </Modal.Footer>
      </Modal>
    );
  };
  const deleteService = async () => {
    modalManager.show(<OverlayBackdrop isOpen={true} />);
    //transform stackServices data to have necessary properties
    //for deletion
    enhanceStackServices();

    const interDependentServices = getInterDependentServicesToBeDeleted();
    const serviceNamesToDelete = getServiceNamesToBeDeleted(
      interDependentServices
    );

    const dependentServices = findDependentServices(serviceNamesToDelete);
    //@ts-ignore
    const displayName = serviceNameDisplayMapping[serviceName];
    const popupHeader = "Delete Service";
    const dependentServicesToDeleteFmt = servicesDisplayNames(
      interDependentServices
    );
    rangerPluginEnabled = isRangerPluginEnabled();

    if (serviceName === "KERBEROS") {
      kerberosDeleteWarning();
      return;
    }

    if (serviceName === "RANGER" && rangerPluginEnabled) {
      modalManager.hide();
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => modalManager.hide()}
          modalTitle={popupHeader}
          modalBody={"Prior to deleting Ranger, you must disable plugins."}
          cancellable={false}
          successCallback={() => modalManager.hide()}
        />
      );
    }

    if (stackServicesFromHook?.length === 1) {
      // At least one service should be installed
      modalManager.hide();
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => modalManager.hide()}
          modalTitle={popupHeader}
          modalBody={`Cannot delete ${displayName}. At least one service must remain in the cluster.`}
          cancellable={false}
          successCallback={() => modalManager.hide()}
        />
      );
    } else if (dependentServices.length > 0) {
      dependentServicesWarning(serviceName, dependentServices);
    } else {
      const isServiceInRemovableState =
        allowUninstallServices(serviceNamesToDelete);
      if (isServiceInRemovableState) {
        if (serviceName === "RANGER_KMS") {
          modalManager.show(
            <ConfirmationModal
              isOpen={true}
              onClose={() => modalManager.hide()}
              modalTitle={popupHeader}
              modalBody="Warning: Deleting Ranger KMS will remove all encryption keys and make encrypted data inaccessible. Are you sure you want to continue?"
              cancellable={true}
              successCallback={async () => {
                modalManager.hide();
                await showLastWarning(
                  serviceName,
                  interDependentServices,
                  dependentServicesToDeleteFmt
                );
              }}
            />
          );
        } else {
          await loadConfigs();
          await showLastWarning(
            serviceName,
            interDependentServices,
            dependentServicesToDeleteFmt
          );
        }
      } else {
        let body = `${displayName} must be stopped before it can be deleted.`;
        if (interDependentServices.length) {
          body += ` The following dependent services must also be stopped: ${dependentServicesToDeleteFmt}`;
        }

        modalManager.show(
          <ConfirmationModal
            isOpen={true}
            onClose={() => modalManager.hide()}
            modalTitle={popupHeader}
            modalBody={body}
            cancellable={false}
            successCallback={() => modalManager.hide()}
          />
        );
      }
    }
  };

  const dependentServicesWarning = (
    serviceName: string,
    dependentServices: string[]
  ) => {
    modalManager.hide();
    const dependentServicesDisplay = dependentServices
      //@ts-ignore
      .map((service) => serviceNameDisplayMapping[service] || service)
      .join(", ");

    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle="Cannot Delete Service"
        modalBody={`Cannot delete ${
          //@ts-ignore
          serviceNameDisplayMapping[serviceName] || serviceName
        }. The following services depend on it: ${dependentServicesDisplay}`}
        cancellable={false}
        successCallback={() => modalManager.hide()}
      />
    );
  };

  // TODO: this and other functions rely on work status
  const allowUninstallServices = (serviceNames: string[]): boolean => {
    // Check if all services in the list are in a state that allows uninstallation
    // This would typically check if services are stopped/installed state
    if (!stackServicesFromHook) return false;

    return serviceNames.every((serviceName) => {
      const service = stackServicesFromHook.find(
        (s: any) => s?.StackServices?.service_name === serviceName
      );
      // Assuming services can be deleted if they exist and are not in a running state
      return service && service.ServiceInfo?.state !== "STARTED";
    });
  };

  const getDependencyChain = (
    serviceName: string,
    stackServices: any[],
    processedServices: Set<string>
  ): string[] => {
    if (processedServices.has(serviceName)) {
      return [];
    }

    processedServices.add(serviceName);

    // Always include the current service and HDFS in the dependency chain
    const dependencyChain: string[] = [serviceName];

    // Directly add HDFS if it's not the current service
    if (serviceName !== "HDFS") {
      dependencyChain.push("HDFS");
    }

    const directDependencies = configDependentServiceNamesRef.current;

    // Recursively add dependencies of dependencies
    directDependencies.forEach((depService) => {
      if (depService !== "HDFS") {
        const subDependencies = getDependencyChain(
          depService,
          stackServices,
          processedServices
        );
        dependencyChain.push(...subDependencies);
      }
    });

    return dependencyChain;
  };

  const populateConfigsForRecommendationsCompare = async (
    serviceNamesToDelete: string[]
  ): Promise<void> => {
    const processedServices = new Set<string>();
    try {
      const dependencyChain: string[] = [];
      serviceNamesToDelete.forEach((serviceName) => {
        const chain = getDependencyChain(
          serviceName,
          stackServicesFromHook || [],
          processedServices
        );
        dependencyChain.push(...chain);
      });

      const uniqueDependencyChain = [...new Set(dependencyChain)];

      if (uniqueDependencyChain.includes("RANGER")) {
        uniqueDependencyChain.push("YARN");
      }

      // Get sites to load for each service in the dependency chain
      let sitesToLoad: string[] = [];
      uniqueDependencyChain.forEach((serviceName) => {
        if (serviceName === "HDFS") {
          sitesToLoad.push("core-site");
          return;
        } else if (serviceName == "YARN") {
          sitesToLoad.push("yarn-site");
          return;
        }
        const configTypes = getConfigTypeList(serviceName);
        sitesToLoad.push(...configTypes);
        // }
      });

      sitesToLoad = [...new Set(sitesToLoad)];

      const siteConfigs = await getCurrentConfigsBySites(sitesToLoad);

      let allConfigs: ConfigProperty[] = [];
      const newConfigs: Record<string, any> = {};

      siteConfigs.forEach((site: SiteConfig) => {
        newConfigs[site.type] = site.properties;

        const configs = getConfigsFromJSON(site, true);

        if (site.type === "core-site") {
          configs.forEach((config) => {
            config.serviceName = "HDFS";
          });
        }

        allConfigs = allConfigs.concat(configs);
      });

      // newConfigs["hbase-site"]["hbase.bucketcache.ioengine"] = "";
      configsForRecommendationsCompareRef.current = newConfigs;

      //@ts-ignore
      const newStepConfigs: StepConfig[] = [];

      uniqueDependencyChain.forEach((serviceName) => {
        const configTypes = getConfigTypeList(serviceName);

        if (serviceName === "HDFS") {
          configTypes.push("core-site");
        }

        const configsByService = allConfigs.filter((config: ConfigProperty) => {
          //@ts-ignore
          if (serviceName === "HDFS" && config.filename === "core-site.xml") {
            return true;
          }
          //@ts-ignore
          const configTag = getConfigTagFromFileName(config.filename);
          return configTypes.includes(configTag);
        });

        if (
          hasPreDefinedServiceConfigs(serviceName) ||
          serviceName === "HDFS"
        ) {
          const serviceConfig = createServiceConfig(
            serviceName,
            [],
            configsByService
          );
          newStepConfigs.push(serviceConfig);
        }
      });
    } catch (error) {
      console.error("Error loading configs:", error);
    }
  };

  //contains all hdfs site files needed for payload generation
  const loadConfigs = async (): Promise<void> => {
    try {
      // Get dependent service names and config dependent service names
      const dependentServiceNames = getDependentServiceNames(
        serviceName,
        true // Assume configs properties are loaded
      );

      const configDependentServiceNames = getConfigDependentServiceNames(
        serviceName,
        dependentServiceNames
      );
      configDependentServiceNamesRef.current = configDependentServiceNames;

      const installedServices = services || [];
      const installedServicesNames = installedServices.map(
        (instServ) => instServ.ServiceInfo.service_name
      );
      const isHdfsInstalled = installedServicesNames.includes("HDFS");
      const isHdfsInConfigDependentServices =
        configDependentServiceNames.includes("HDFS");

      const servicesToInclude = [...configDependentServiceNames];
      if (isHdfsInstalled && !isHdfsInConfigDependentServices) {
        servicesToInclude.push("HDFS");
      }

      let sitesToLoad = getSitesToLoad(
        serviceName,
        configDependentServiceNames,
        stackServicesFromHook || []
      );

      const hdfsConfigProperties = await ConfigsApi.getConfigValues(
        clusterName,
        "HDFS"
      );
      const hdfsSiteFileNames =
        hdfsConfigProperties?.items[0].configurations.map(
          (hdfsConfProps: any) => hdfsConfProps.type
        );

      if (isHdfsInstalled) {
        sitesToLoad.push(...hdfsSiteFileNames);
      }

      // Fetch current configurations by sites
      const siteConfigs = await getCurrentConfigsBySites(sitesToLoad);

      let allConfigs: ConfigProperty[] = [];
      const newConfigs: Record<string, any> = {};

      // Process each site configuration
      siteConfigs.forEach((site: SiteConfig) => {
        newConfigs[site.type] = site.properties;

        // For core-site, explicitly associate it with HDFS
        const configs = getConfigsFromJSON(site, true);
        if (site.type === "core-site" && isHdfsInstalled) {
          configs.forEach((config) => {
            config.serviceName = "HDFS";
          });
        }

        allConfigs = allConfigs.concat(configs);
      });

      allConfigsWithCurrentValueForServicesRef.current = newConfigs;

      // Create step configs for each service to include
      //@ts-ignore
      const newStepConfigs: StepConfig[] = [];

      servicesToInclude.forEach((serviceName: string) => {
        const configTypes = getConfigTypeList(serviceName);

        // Filter configs by service config types
        const configsByService = allConfigs.filter((config: ConfigProperty) => {
          //@ts-ignore
          if (serviceName === "HDFS" && config.filename === "core-site.xml") {
            return true;
          }
          //@ts-ignore
          const configTag = getConfigTagFromFileName(config.filename);
          return configTypes.includes(configTag);
        });

        // Create service config and add to stepConfigs
        if (
          hasPreDefinedServiceConfigs(serviceName) ||
          serviceName === "HDFS"
        ) {
          const serviceConfig = createServiceConfig(
            serviceName,
            [],
            configsByService
          );
          newStepConfigs.push(serviceConfig);
        }
      });
    } catch (error) {
      console.error("Error loading configs:", error);
    }
  };

  const loadConfigRecommendations = async (serviceNamesToDelete: string[]) => {
    setIsRecommendationInProgress(true);
    try {
      const payload = await buildRecommendationsPayload(
        clusterName,
        serviceNamesToDelete,
        stackServicesFromHook,
        allHostNames || [],
        allConfigsWithCurrentValueForServicesRef.current
      );

      const response = await ambariApi.request({
        url: `/stacks/VDP/versions/${vdpStackVersion}/recommendations`,
        method: "POST",
        data: payload,
      });

      if (
        response.data &&
        response.data.resources &&
        response.data.resources[0]
      ) {
        const recommendationsData = response.data.resources[0].recommendations;

        // Process the recommendations to get changed properties
        const changedProps = processRecommendations(
          recommendationsData,
          serviceNamesToDelete
        );
        if (changedProps.length > 0) {
          isShowWarningWithRecommendationsLoaded.current = true;
        }

        //setChangedProperties(changedProps);
        changedPropertiesRef.current = changedProps;
      }
    } catch (error) {
      console.error("Error loading recommendations:", error);
    } finally {
      setIsRecommendationInProgress(false);
    }
  };

  // Build the enhanced stack services when stackServicesFromHook is available
  const enhanceStackServices = () => {
    if (
      stackDataWithDependencies &&
      Array.isArray(stackDataWithDependencies) &&
      stackDataWithDependencies.length > 0
    ) {
      // Build service by config type map
      const serviceByConfigTypeMap: Record<string, string> = {};
      stackDataWithDependencies.forEach((service) => {
        const serviceName = service.StackServices?.service_name;
        const configTypes = service.StackServices?.config_types || {};

        Object.keys(configTypes).forEach((configType) => {
          serviceByConfigTypeMap[configType] = serviceName;
        });
      });

      // Initialize dependent service names map
      const dependentServiceNames: Record<string, string[]> = {};
      stackDataWithDependencies.forEach((service) => {
        const serviceName = service.StackServices?.service_name;
        if (serviceName) {
          dependentServiceNames[serviceName] = [];
        }
      });

      // Process configurations for dependencies
      stackDataWithDependencies.forEach((service) => {
        if (!service.configurations || !Array.isArray(service.configurations)) {
          return;
        }

        const serviceName = service.StackServices?.service_name;
        if (!serviceName) return;

        service.configurations.forEach((config: any) => {
          // Process dependencies (property_depended_by)
          if (config.dependencies && Array.isArray(config.dependencies)) {
            config.dependencies.forEach((dep: any) => {
              const dependencyType =
                dep.StackConfigurationDependency?.dependency_type;
              const dependentServiceName =
                serviceByConfigTypeMap[dependencyType];

              if (
                dependentServiceName &&
                dependentServiceName !== serviceName &&
                !dependentServiceNames[serviceName].includes(
                  dependentServiceName
                )
              ) {
                dependentServiceNames[serviceName].push(dependentServiceName);
              }
            });
          }

          // Process property_depends_on
          if (
            config.StackConfigurations?.property_depends_on &&
            Array.isArray(config.StackConfigurations.property_depends_on)
          ) {
            config.StackConfigurations.property_depends_on.forEach(
              (dep: any) => {
                const dependentServiceName = serviceByConfigTypeMap[dep.type];

                if (
                  dependentServiceName &&
                  dependentServiceName !== serviceName &&
                  !dependentServiceNames[serviceName].includes(
                    dependentServiceName
                  )
                ) {
                  dependentServiceNames[serviceName].push(dependentServiceName);
                }
              }
            );
          }
        });
      });

      // Enhance stack services with dependentServiceNames
      const enhanced = stackDataWithDependencies.map((service) => {
        const serviceName = service.StackServices?.service_name;
        return {
          ...service,
          dependentServiceNames: dependentServiceNames[serviceName] || [],
        };
      });

      // setEnhancedStackServices(enhanced);
      enhancedStackServicesRef.current = enhanced;
    }
  };

  // Use the enhanced stack services in your functions
  const getDependentServiceNames = (
    serviceName: string,
    isConfigsPropertiesLoaded: boolean
  ): string[] => {
    if (!isConfigsPropertiesLoaded || !serviceName) {
      return [];
    }

    const service = enhancedStackServicesRef.current.find(
      (s) => s.StackServices?.service_name === serviceName
    );
    return service?.dependentServiceNames || [];
  };

  const getConfigDependentServiceNames = (
    serviceName: string,
    dependentServiceNames: string[]
  ): string[] => {
    const service = enhancedStackServicesRef.current.find(
      (s) => s.StackServices?.service_name === serviceName
    );
    const requiredServices = service?.StackServices?.required_services || [];

    return [...new Set([...dependentServiceNames, ...requiredServices])];
  };

  const allowSelectPropertiesUpdation = (
    changedPropertiesForUpdation: any,
    currentServiceToBeDeleted: string
  ) => {
    // Create a new array to store properties that should be kept
    const filteredProperties = [];
    const skippedProperties = [];

    for (const cp of changedPropertiesForUpdation) {
      let propName = cp.propertyName;

      let propServiceName = cp.serviceName;

      const stackServicePropObj = enhancedStackServicesRef.current.find(
        (eSS) => eSS.StackServices.service_name === propServiceName
      );

      //if service object itself does not exist in stack , push it to be added based on changed property values
      if (cp.initialValue !== cp.recommendedValue && cp.saveRecommended) {
        if (
          cp.recommendedValue === "Property removed" &&
          cp.initialValue === "Property undefined"
        ) {
          skippedProperties.push(cp);
          continue;
        }

        //filteredProperties.push(cp);
        //continue;
      }

      //if service object
      if (stackServicePropObj) {
        const stackServiceConfObj = stackServicePropObj.configurations.filter(
          (conf: any) => conf.StackConfigurations.property_name === propName
        );
        if (!stackServiceConfObj || stackServiceConfObj.length === 0) {
          if (cp.initialValue !== cp.recommendedValue && cp.saveRecommended) {
            filteredProperties.push(cp); // Keep the property if no matching configuration is found
          }
          continue;
        }

        if (
          stackServiceConfObj.length === 0 &&
          stackServiceConfObj[0].StackConfigurations.service_name !==
            currentServiceToBeDeleted
        ) {
          skippedProperties.push(cp);
          continue; // Skip if no matching configuration is found
        }
      }

      if (
        !stackServicePropObj ||
        stackServicePropObj.StackServices.service_name ===
          currentServiceToBeDeleted
      ) {
        filteredProperties.push(cp); // Keep the property if no matching service is found
        continue;
      }

      const propertyInStackObjPropDependsOn =
        stackServicePropObj.configurations.filter(
          (sspo: any) => sspo.StackConfigurations.property_name === propName
        );

      if (propertyInStackObjPropDependsOn.length === 0) {
        filteredProperties.push(cp); // Keep the property if no matching configuration is found
        continue;
      }

      const propDependsOnArr =
        propertyInStackObjPropDependsOn[0].StackConfigurations
          .property_depends_on;

      if (propDependsOnArr.length < 1) {
        filteredProperties.push(cp); // Keep the property if it doesn't depend on anything
        continue;
      }

      // Check if the property depends on a service that's not being deleted
      let shouldKeep = true;
      for (const propInStObPrDe of propDependsOnArr) {
        if (
          currentServiceToBeDeleted !==
          getServiceNameFromConfigType(propInStObPrDe.type)
        ) {
          shouldKeep = false;
          break;
        }
      }

      if (shouldKeep) {
        filteredProperties.push(cp);
      } else {
        skippedProperties.push(cp);
      }
    }

    // Replace the original array with the filtered one
    changedPropertiesForUpdation.length = 0;
    changedPropertiesForUpdation.push(...filteredProperties);
    return changedPropertiesForUpdation;
  };

  const allowUpdateProperty = (
    propertyName: string,
    configType: string,
    configGroup: string = "Default"
  ): boolean => {
    const serviceName = getServiceNameFromConfigType(configType);

    const serviceNamesToDelete = getServiceNamesToBeDeleted(
      getInterDependentServicesToBeDeleted()
    );
    const processedServices = new Set<string>();

    const dependencyChain: string[] = [];
    serviceNamesToDelete.forEach((svcName) => {
      const chain = getDependencyChain(
        svcName,
        stackServicesFromHook || [],
        processedServices
      );
      dependencyChain.push(...chain);
    });

    const uniqueDependencyChain = [...new Set(dependencyChain)];

    // If the service is not in our dependency chain, don't allow the update
    if (!uniqueDependencyChain.includes(serviceName)) {
      return false;
    }

    // Special case for HDFS: only include core-site properties
    if (serviceName === "HDFS" && configType !== "core-site") {
      return false;
    }

    // Special case for YARN: only include yarn-site properties
    if (serviceName === "YARN" && configType !== "yarn-site") {
      return false;
    }

    // If we've already processed this property, check its saveRecommended value
    const existingRecommendation = changedPropertiesRef.current.find(
      (prop) =>
        prop.propertyName === propertyName &&
        //@ts-ignore
        prop.filename === `${configType}.xml` &&
        //@ts-ignore
        (configGroup ? prop.configGroup === configGroup : true)
    );

    if (existingRecommendation) {
      return existingRecommendation.saveRecommended;
    }

    // By default, allow the update for properties in our dependency chain
    return true;
  };

  const processRecommendations = (
    recommendationsData: any,
    serviceNamesToDelete: string[]
  ) => {
    const changedProps: ConfigProperty[] = [];
    const configurations = recommendationsData.blueprint.configurations || {};
    const processedServices = new Set<string>();
    //@ts-ignore
    const undefinedProperty = "Property undefined";
    let propertiesToBeSkipped = new Set<string>([]);

    // Get the dependency chain for all services being deleted
    const dependencyChain: string[] = [];
    serviceNamesToDelete.forEach((serviceName) => {
      const chain = getDependencyChain(
        serviceName,
        stackServicesFromHook || [],
        processedServices
      );
      dependencyChain.push(...chain);
    });

    // Remove duplicates
    const uniqueDependencyChain = [...new Set(dependencyChain)];

    if (
      uniqueDependencyChain[0] === "SSM" ||
      uniqueDependencyChain[0] === "TEZ"
    ) {
      //return empty changed props
      return [];
    }

    Object.keys(configurations).forEach((configType) => {
      const properties = configurations[configType].properties || {};

      // Process each property
      Object.keys(properties).forEach((propertyName) => {
        const recommendedValue = properties[propertyName];
        if (isEmpty(recommendedValue) || recommendedValue === undefined) {
          propertiesToBeSkipped.add(propertyName);
          return;
        }

        // Get the service name for this config type
        const serviceName = getServiceNameFromConfigType(configType);

        // Only include properties for services in the dependency chain
        if (uniqueDependencyChain.includes(serviceName)) {
          // Skip HDFS properties that are not in core-site
          // Skip YARN properties that are in yarn-site
          // This is to avoid unnecessary changes for HDFS and YARN
          if (
            (serviceName === "HDFS" && configType !== "core-site") ||
            (serviceName === "YARN" && configType !== "yarn-site")
          ) {
            return; // Skip this property
          }

          // Get current value based on service and config type
          let currentValue = getCurrentPropertyValue(configType, propertyName);

          // If the recommended value is different from the current value
          if (recommendedValue !== currentValue) {
            //@ts-ignore
            if (
              changedProps.some(
                (prop) =>
                  prop.propertyName === propertyName &&
                  //@ts-ignore
                  prop.filename === configType
              )
            ) {
              return;
            }
            changedProps.push({
              serviceName,
              propertyName,
              saveRecommended: true,
              recommendedValue,
              initialValue: currentValue,
              //@ts-ignore
              filename: `${configType}`,
            });
          }
        }
      });
      // Process property_attributes section for deletions
      Object.keys(configurations[configType]).forEach((key) => {
        if (propertiesToBeSkipped.has(key)) {
          return; // Skip properties that were already skipped
        }
        const propertyAttributes =
          configurations[configType].property_attributes || {};
        Object.keys(propertyAttributes).forEach((propertyName) => {
          const attributes = propertyAttributes[propertyName];

          // Check if this property should be deleted
          if (
            attributes &&
            attributes.delete === "true" &&
            allowUpdateProperty(propertyName, configType)
          ) {
            const serviceName = getServiceNameFromConfigType(configType);

            // Only include properties for services in the dependency chain
            if (uniqueDependencyChain.includes(serviceName)) {
              // Get current value based on service and config type
              let currentValue = getCurrentPropertyValue(
                configType,
                propertyName
              );

              //@ts-ignore
              if (
                changedProps.some(
                  (prop) =>
                    prop.propertyName === propertyName &&
                    //@ts-ignore
                    prop.filename === configType
                )
              ) {
                return;
              }

              // Add to changed properties with recommendedValue set to null or "Property removed"
              changedProps.push({
                serviceName,
                propertyName,
                saveRecommended: true,
                recommendedValue: "Property removed",
                initialValue: currentValue,
                //@ts-ignore
                filename: `${configType}`,
              });
            }
          }
        });
      });
    });
    allowSelectPropertiesUpdation(changedProps, serviceName);
    return changedProps;
  };

  const getCurrentPropertyValue = (
    configType: string,
    propertyName: string
  ) => {
    const undefinedProperty = "Property undefined";

    const configsForType =
      //@ts-ignore
      configsForRecommendationsCompareRef.current[configType] || {};
    return configsForType[propertyName] !== undefined
      ? configsForType[propertyName]
      : undefinedProperty;
  };

  // Get service name from config type
  const getServiceNameFromConfigType = (configType: string): string => {
    // Find the service that has this config type
    const service = stackServicesFromHook?.find((s: any) => {
      const configTypes = s?.StackServices?.config_types || {};
      return Object.keys(configTypes).includes(configType);
    });

    return service ? service.StackServices.service_name : "MISC";
  };
  // Add these helper functions to your hook
  const clearRecommendations = () => {
    //setChangedProperties([]);
    changedPropertiesRef.current = [];
    // setRecommendationsConfigs(null);
  };

  const fetchConfigGroups = async (serviceNames: string[]) => {
    const configGroupsMap = {};

    for (const serviceName of serviceNames) {
      try {
        const response = await ConfigsApi.getConfigGroups(
          clusterName,
          serviceName
        );
        const groups = response.items || [];

        // Process each config group
        groups.forEach((group: any) => {
          const groupName = group.ConfigGroup.group_name;
          const configs = group.ConfigGroup.desired_configs || {};

          // For each config type in this group
          Object.keys(configs).forEach((configType) => {
            const properties = configs[configType].properties || {};

            // For each property in this config type
            Object.keys(properties).forEach((propName) => {
              // Create a unique key for this property
              const key = `${serviceName}:${configType}:${propName}`;
              //@ts-ignore
              configGroupsMap[key] = groupName;
            });
          });
        });
      } catch (error) {
        console.error(
          `Error fetching config groups for ${serviceName}:`,
          error
        );
      }
    }
    return configGroupsMap;
  };
  const showLastWarning = async (
    serviceName: string,
    interDependentServices: string[],
    dependentServicesToDeleteFmt: string
  ) => {
    if (serviceName === serviceNames.RANGER && rangerPluginEnabled) {
      return;
    }
    //@ts-ignore
    const displayName = serviceNameDisplayMapping[serviceName] || serviceName;
    const popupHeader = "Delete Service";
    const popupPrimary = "Delete";

    // Base warning message
    const warningMessage =
      `The ${displayName} service will be removed from Ambari and all configurations and configuration history will be lost`.concat(
        interDependentServices.length
          ? `Note: The dependent ${dependentServicesToDeleteFmt} service will be removed too.`
          : ""
      );

    // Clear any existing recommendations
    clearRecommendations();

    // Start loading recommendations
    const serviceNamesToDelete = [serviceName, ...interDependentServices];
    const configGroupsMap = await fetchConfigGroups(serviceNamesToDelete);

    // Load the configurations and recommendations
    await loadConfigs();
    await populateConfigsForRecommendationsCompare(serviceNamesToDelete);
    //lets add a set timeout of 2 seconds
    await new Promise((resolve) => setTimeout(resolve, 2000));
    await loadConfigRecommendations(serviceNamesToDelete);

    modalManager.hide();
    // Show a loading spinner while we fetch recommendations
    if (isRecommendationInProgress) {
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => {
            clearRecommendations();
            modalManager.hide();
          }}
          modalTitle={popupHeader}
          //@ts-ignore
          modalBody={
            <div>
              <div dangerouslySetInnerHTML={{ __html: warningMessage }} />
              <div className="text-center mt-4">
                <Spinner />
                <p>Loading configuration recommendations...</p>
              </div>
            </div>
          }
          cancellable={true}
          primaryButtonText={popupPrimary}
          //@ts-ignore
          primaryButtonClass="btn-danger"
          disablePrimary={true}
        />
      );
      await new Promise((resolve) => setTimeout(resolve, 500));
      modalManager.hide();
    }

    // Now show the final modal with recommendations
    if (changedPropertiesRef.current.length > 0) {
      const formattedProperties = changedPropertiesRef.current.map(
        (property: any) => {
          const key = `${property.serviceName}:${property.filename}:${property.propertyName}`;
          //@ts-ignore
          const configGroup = configGroupsMap[key] || "Default";
          return {
            ...property,
            serviceDisplayName:
              //@ts-ignore
              serviceNameDisplayMapping[property.serviceName] ||
              property.serviceName,
            // propertyFileName: property.filename || property.propertyName.split('/').pop() || '',
            propertyFileName: property.filename,
            configGroup: configGroup,
            // If recommendedValue is "Property removed", set a flag to indicate this is a removal
            isRemoved: property.recommendedValue === "Property removed",
          };
        }
      );

      modalManager.show(
        <RecommendationModal
          isOpen={true}
          onClose={() => {
            clearRecommendations();
            modalManager.hide();
          }}
          add={false}
          recommendedPropertiesToChange={formattedProperties}
          callback={(updatedProperties) => {
            // Update changedPropertiesRef with the updated properties
            changedPropertiesRef.current = updatedProperties;
            modalManager.hide();
            confirmDeleteService(
              serviceName,
              interDependentServices,
              dependentServicesToDeleteFmt
            );
          }}
          commonMessage={warningMessage}
        />
      );
    } else {
      // If there are no recommendations, show a simple confirmation modal
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => {
            clearRecommendations();
            modalManager.hide();
          }}
          modalTitle={popupHeader}
          //@ts-ignore
          modalBody={
            <div dangerouslySetInnerHTML={{ __html: warningMessage }} />
          }
          cancellable={true}
          primaryButtonText={popupPrimary}
          //@ts-ignore
          primaryButtonClass="btn-danger"
          successCallback={() => {
            modalManager.hide();
            confirmDeleteService(
              serviceName,
              interDependentServices,
              dependentServicesToDeleteFmt
            );
          }}
        />
      );
    }
  };

  //@ts-ignore
  const getFormattedStringFromArray = (array: string[]): string => {
    if (array.length === 0) return "";
    if (array.length === 1) return array[0];
    if (array.length === 2) return `${array[0]} and ${array[1]}`;

    // For arrays with more than 2 items: "item1, item2, and item3"
    const lastItem = array[array.length - 1];
    const otherItems = array.slice(0, array.length - 1);
    return `${otherItems.join(", ")}, and ${lastItem}`;
  };

  const confirmDeleteService = (
    serviceName: string,
    dependentServiceNames: string[],
    //@ts-ignore
    servicesToDeleteFmt: string
  ) => {
    const confirmKey = "delete";
    
    // Create a reactive modal that manages its own state
    const DeleteServiceModal = () => {
      const [confirmInput, setConfirmInput] = useState("");
      const isConfirmValid = confirmInput === confirmKey;

      return (
        <ConfirmationModal
          isOpen={true}
          onClose={() => modalManager.hide()}
          modalTitle="Confirm Delete"
          modalBody={
            <div>
              <p>
                You must confirm delete of{" "}
                <strong>
                  {
                    //@ts-ignore
                    serviceNameDisplayMapping[serviceName] || serviceName
                  }
                </strong>{" "}
                by typing "{confirmKey}" in the confirmation box.
              </p>
              <p className="text-danger">
                <strong>
                  This operation is not reversible and all configuration history
                  will be lost.
                </strong>
              </p>
              <div className="mt-3">
                <label className="form-label">
                  Type "{confirmKey}" to confirm:
                </label>
                <input
                  type="text"
                  className="form-control"
                  placeholder={`Type "${confirmKey}" to confirm`}
                  value={confirmInput}
                  onChange={(e) => setConfirmInput(e.target.value)}
                  autoFocus
                />
              </div>
            </div>
          }
          cancellable={true}
          isOkDisabled={!isConfirmValid}
          primaryButtonText="DELETE"
          successCallback={() => {
            const serviceNames = [serviceName].concat(dependentServiceNames);
            if (isConfirmValid) {
              modalManager.hide();
              deleteServiceCall(serviceNames);
            }
          }}
        />
      );
    };

    modalManager.show(<DeleteServiceModal />);
  };

  const deleteServiceCall = async (serviceNames: string[]): Promise<void> => {
    const serviceToDeleteNow = serviceNames[0];
    const servicesToDeleteNext =
      serviceNames.length > 1 ? serviceNames.slice(1) : undefined;

    try {
      const response = await ServiceApi.removeService(
        clusterName,
        serviceToDeleteNow,
        serviceToDeleteNow,
        servicesToDeleteNext
      );

      // Handle success
      deleteServiceCallSuccessCallback(
        response.data,
        serviceToDeleteNow,
        servicesToDeleteNext
      );
    } catch (error) {
      console.error("Error deleting service:", error);
      deleteServiceCallErrorCallback(error);
    }
  };

  const deleteServiceCallSuccessCallback = (
    //@ts-ignore
    data: DeleteServiceResponse,
    //@ts-ignore
    currentService: string,
    servicesToDeleteNext?: string[]
  ) => {
    if (servicesToDeleteNext && servicesToDeleteNext.length > 0) {
      // Continue with next service deletion
      deleteServiceCall(servicesToDeleteNext);
    } else {
      saveConfigs();
    }
  };

  const deleteServiceCallErrorCallback = (error: any) => {
    console.error("Service deletion failed:", error);

    // Hide progress popup
    modalManager.hide();

    // Show error message
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle="Service Deletion Failed"
        modalBody={`Failed to delete service. Error: ${error.message || error}`}
        cancellable={false}
        successCallback={() => modalManager.hide()}
      />
    );
  };

  // Add this interface at the top with other interfaces
  interface ServiceConfig {
    serviceName: string;
    configs: any[];
  }

  interface ConfigProperty {
    serviceName: string;
    propertyName: string;
    saveRecommended: boolean;
    recommendedValue: any;
    initialValue: any;
  }

  const getSitesToLoad = (
    serviceName: string,
    configDependentServiceNames: string[],
    stackServices: any[]
  ): string[] => {
    let configTypeList: string[] = [];

    // Get config types for all config dependent services
    if (configDependentServiceNames.length > 0) {
      // Filter stack services to only include those in the config dependent services list
      const filteredServices = stackServices.filter((service: any) =>
        configDependentServiceNames.includes(
          service?.StackServices?.service_name
        )
      );

      // Extract config types from each service
      filteredServices.forEach((service: any) => {
        if (service?.StackServices?.config_types) {
          // In the original Ember implementation, configTypeList is an array of config type names
          // In the stack service data, config_types is an object where keys are the config type names
          const serviceConfigTypes = Object.keys(
            service.StackServices.config_types
          );
          configTypeList = configTypeList.concat(serviceConfigTypes);
        }
      });
    }

    // Add special cases from serviceConfigsMap
    const serviceConfigsMap: Record<string, string[]> = {
      OOZIE: ["oozie-env"],
    };

    if (serviceConfigsMap[serviceName]) {
      configTypeList = configTypeList.concat(serviceConfigsMap[serviceName]);
    }

    // Always include cluster-env
    configTypeList.push("cluster-env");

    // Remove duplicates and return
    return [...new Set(configTypeList)];
  };

  // Helper function to get config tags for specified sites
  const getConfigTags = async (sites: string[] = []): Promise<ConfigTag[]> => {
    try {
      // First, check if we have tags in local storage or context
      // If not, we need to fetch them from the server
      const response = await ambariApi.request({
        url: `/clusters/${clusterName}/configurations/service_config_versions?is_current=true`,
        method: "GET",
      });

      // Extract the tags from the response
      const configVersions = response.data.items || [];
      const tags: ConfigTag[] = [];

      // Process the response to extract tags for the requested sites
      configVersions.forEach((version: any) => {
        const configurations = version.configurations || [];
        configurations.forEach((config: any) => {
          const type = config.type;
          // If sites is empty, get all tags, otherwise filter by the requested sites
          if (sites.length === 0 || sites.includes(type)) {
            tags.push({
              siteName: type,
              tagName: config.tag,
            });
          }
        });
      });

      return tags;
    } catch (error) {
      console.error("Error fetching config tags:", error);
      throw error;
    }
  };

  // Helper function to get configs by tags
  const getConfigsByTags = async (tags: ConfigTag[]): Promise<SiteConfig[]> => {
    try {
      // Build the URL parameters for the request
      const urlParams = tags
        .map((tag) => `(type=${tag.siteName}&tag=${tag.tagName})`)
        .join("|");

      if (!urlParams) {
        return [];
      }

      // Make the request to get the configurations
      const response = await ambariApi.request({
        url: `/clusters/${clusterName}/configurations?${urlParams}`,
        method: "GET",
      });

      // Process the response to extract the configurations
      const items = response.data.items || [];
      const configs: SiteConfig[] = items.map((item: any) => ({
        type: item.type,
        tag: item.tag,
        properties: item.properties || {},
        properties_attributes: item.properties_attributes || {},
      }));

      return configs;
    } catch (error) {
      console.error("Error fetching configs by tags:", error);
      throw error;
    }
  };

  // Main function to get current configs by sites
  const getCurrentConfigsBySites = async (
    sites: string[] = []
  ): Promise<SiteConfig[]> => {
    try {
      // First get the config tags for the specified sites
      const tags = await getConfigTags(sites);

      // Then get the configs using those tags
      const configs = await getConfigsByTags(tags);

      return configs;
    } catch (error) {
      console.error("Error fetching configurations:", error);
      throw error;
    }
  };
  /**
   * Get the list of config types for a specific service
   * @param serviceName - The name of the service
   * @returns {string[]} - List of config types for the service
   */
  const getConfigTypeList = (serviceName: string): string[] => {
    // Find the service in the stack services
    const service = stackServicesFromHook?.find(
      (s: any) => s?.StackServices?.service_name === serviceName
    );

    if (
      service &&
      service.StackServices &&
      service.StackServices.config_types
    ) {
      // Return the keys of the config_types object) {
      return Object.keys(service.StackServices.config_types);
    }

    return [];
  };
  /**
   * Check if a service has predefined configurations
   * @param serviceName - The name of the service
   * @returns {boolean} - Whether the service has predefined configs
   */
  const hasPreDefinedServiceConfigs = (serviceName: string): boolean => {
    // In the original code, this checks if the service exists in a predefined list
    // For now, we'll assume all services have predefined configs
    // You may want to refine this based on your application's needs
    const preDefinedServices = [
      "HDFS",
      "YARN",
      "MAPREDUCE2",
      "HIVE",
      "ZOOKEEPER",
      "OOZIE",
      "HBASE",
      "PIG",
      "SQOOP",
      "STORM",
      "FALCON",
      "FLUME",
      "ACCUMULO",
      "AMBARI_METRICS",
      "RANGER",
      "RANGER_KMS",
      "KAFKA",
      "KNOX",
      "LOGSEARCH",
      "SPARK",
      "SPARK2",
      "ZEPPELIN",
      "ATLAS",
      "AMBARI_INFRA",
    ];

    return preDefinedServices.includes(serviceName);
  };

  /**
   * Get the config tag from a file name
   * @param fileName - The file name
   * @returns {string} - The config tag
   */
  const getConfigTagFromFileName = (fileName: string): string => {
    return fileName.endsWith(".xml") ? fileName.slice(0, -4) : fileName;
  };

  // Helper function to convert site config to ConfigProperty array
  const getConfigsFromJSON = (
    site: SiteConfig,
    addMetadata: boolean = false
  ): ConfigProperty[] => {
    const configs: ConfigProperty[] = [];

    Object.entries(site.properties).forEach(([key, value]) => {
      const config: ConfigProperty = {
        //@ts-ignore
        name: key,
        value: value,
        filename: `${site.type}.xml`,
        serviceName: getServiceNameFromConfigType(site.type),
        displayName: key,
        description: "",
        isRequired: false,
        category: "General",
      };

      if (addMetadata) {
        // Add additional metadata if needed
        //@ts-ignore
        config.isNotDefaultValue = false;
        //@ts-ignore
        config.isNotSaved = false;
        //@ts-ignore
        config.savedValue = value;
        //@ts-ignore
        config.isFinal = false;
        //@ts-ignore
        config.savedIsFinal = false;
      }

      configs.push(config);
    });

    return configs;
  };

  // Enhanced createServiceConfig function
  const createServiceConfig = (
    serviceName: string,
    //@ts-ignore
    serviceConfigTags: any[],
    configs: ConfigProperty[]
    //@ts-ignore
  ): StepConfig => {
    return {
      serviceName,
      //@ts-ignore
      displayName: serviceNameDisplayMapping[serviceName],
      configs: configs.map((config) => ({
        ...config,
        // Add service-specific properties
        isOverridable: true,
        isVisible: true,
        showLabel: true,
        isUserProperty: false,
      })),
      showConfig: true,
      configGroups: [],
      // Additional properties that might be needed
      isConfigGroupSelected: false,
      dependentServiceNames: [],
      configVersionNote: `Configuration for ${serviceName} service deletion`,
    };
  };

  const saveConfigs = () => {
    const data: ServiceConfig[] = [];
    const stepConfigs = configsForRecommendationsCompareRef.current;

    // Apply recommended values first
    const stepConfigsWithRecommendedValues =
      applyRecommendedValues(stepConfigs);

    // Prepare service configs to save
    // Instead of forEach, we need to iterate through the object keys
    Object.keys(stepConfigsWithRecommendedValues).forEach((configType) => {
      const serviceName = getServiceNameFromConfigType(configType);
      const configProperties = stepConfigsWithRecommendedValues[configType];

      // Convert the properties to the format expected by getServiceConfigToSave
      const configs = Object.keys(configProperties).map((propName) => ({
        name: propName,
        value: configProperties[propName],
        filename: `${configType}.xml`,
      }));

      // Create the service config
      const serviceConfig = {
        serviceName,
        configs,
      };

      // Add it to the data array if it has configs
      if (configs.length > 0) {
        data.push(serviceConfig);
      }
    });

    // Rest of the function remains the same
    if (data.length > 0) {
      // Save configurations with changes
      putChangedConfigurations(data, "confirmServiceDeletion", () => {
        // Hide progress modal after configs are saved
        modalManager.hide();
        confirmServiceDeletion();
      });
    } else {
      // No configuration changes, proceed directly to confirmation
      confirmServiceDeletion();
    }
  };

  const applyRecommendedValues = (stepConfigs: any) => {
    const changedProperties = changedPropertiesRef.current;

    // Make sure changedProperties is an array before proceeding
    if (!Array.isArray(changedProperties)) {
      console.error("changedProperties is not an array:", changedProperties);
      return stepConfigs;
    }

    // Process each changed property
    changedProperties.forEach((property: ConfigProperty) => {
      if (!property.saveRecommended) {
        return; // Skip properties that shouldn't be updated
      }

      //@ts-ignore
      const configType = property.filename; // This should be the config type like "ams-env"

      // If this is a property to be removed
      if (property.recommendedValue === "Property removed") {
        // If the config type exists in stepConfigs and the property exists in that config
        if (
          stepConfigs[configType] &&
          stepConfigs[configType][property.propertyName] !== undefined
        ) {
          // Delete the property
          delete stepConfigs[configType][property.propertyName];
        }
      } else {
        // Otherwise, update the property value
        // Make sure the config type object exists
        if (!stepConfigs[configType]) {
          stepConfigs[configType] = {};
        }

        // Update the property value
        stepConfigs[configType][property.propertyName] =
          property.recommendedValue;
      }
    });

    return stepConfigs;
  };

  const putChangedConfigurations = async (
    //@ts-ignore
    data: ServiceConfig[],
    //@ts-ignore
    context: string,
    callback: () => void
  ) => {
    try {
      // Get the changed properties
      const changedProperties = changedPropertiesRef.current;

      // If no changed properties, just call the callback
      if (!changedProperties || changedProperties.length === 0) {
        callback();
        return;
      }

      // Group changed properties by site file
      const propertiesBySite: Record<string, Record<string, any>> = {};

      changedProperties.forEach((property) => {
        // Skip properties that shouldn't be updated
        if (!property.saveRecommended) {
          return;
        }

        //@ts-ignore
        const configType = property.filename;

        // Initialize the site object if it doesn't exist
        if (!propertiesBySite[configType]) {
          propertiesBySite[configType] = {};
        }

        // Add the property with its recommended value
        propertiesBySite[configType][property.propertyName] =
          property.recommendedValue;
      });

      // Create a timestamp for the config tag
      const timestamp = Math.floor(Date.now() / 1000);
      const configTag = `service-delete-${timestamp}`;

      // Create the desired_config array
      const desiredConfigsArray = Object.keys(propertiesBySite).map(
        (configType) => ({
          type: configType,
          tag: configTag,
          properties: propertiesBySite[configType],
          service_config_version_note: `Update configs after ${serviceName} has been removed`,
        })
      );

      // If no configs to save, just call the callback
      if (desiredConfigsArray.length === 0) {
        callback();
        return;
      }

      // Create the final payload
      const formattedData = [
        {
          Clusters: {
            desired_config: desiredConfigsArray,
          },
        },
      ];

      try {
        await ServicesApi.deleteServiceWithUpdatedConfigs(
          clusterName,
          JSON.stringify(formattedData)
        );
      } catch (error) {
        console.error("Error saving configurations before callback:", error);
        throw error;
      }

      callback();
    } catch (error) {
      console.error("Error saving configurations:", error);
      modalManager.hide();
      modalManager.show(
        <ConfirmationModal
          isOpen={true}
          onClose={() => modalManager.hide()}
          modalTitle="Configuration Save Failed"
          modalBody={`Failed to save configuration changes. Error: ${
            //@ts-ignore
            error.message || error
          }`}
          cancellable={false}
          successCallback={() => modalManager.hide()}
        />
      );
    }
  };

  //@ts-ignore
  const getConfigTypeFromFilename = (filename: string): string => {
    // Extract config type from filename (e.g., "hdfs-site.xml" -> "hdfs-site")
    return filename.replace(".xml", "").replace(".properties", "");
  };

  //@ts-ignore
  const getServiceConfigVersionNote = (): string => {
    const interDependentServices = getInterDependentServicesToBeDeleted();
    const serviceNamesToDelete = getServiceNamesToBeDeleted(
      interDependentServices
    );
    const services = serviceNamesToDelete.join(",");

    if (serviceNamesToDelete.length === 1) {
      return `Configuration changes for service deletion: ${services}`;
    }
    return `Configuration changes for services deletion: ${services}`;
  };

  // Update your existing confirmServiceDeletion function
  const confirmServiceDeletion = () => {
    const interDependentServices = getInterDependentServicesToBeDeleted();
    let serviceNames: string;
    let msg: string;

    if (interDependentServices.length > 0) {
      const serviceNamesToDelete = getServiceNamesToBeDeleted(
        interDependentServices
      );
      serviceNames = serviceNamesToDelete
        //@ts-ignore
        .map((name) => serviceNameDisplayMapping[name] || name)
        .join(", ");
      msg = `Successfully deleted services: ${serviceNames}`;
    } else {
      //@ts-ignore
      serviceNames = serviceNameDisplayMapping[serviceName] || serviceName;
      msg = `Successfully deleted service: ${serviceNames}`;
    }

    toast.success(msg);
    setTimeout(()=>{
          window.location.reload();
    },1000)

  };

  return {
    deleteService,
  };
}
