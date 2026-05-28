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

//@ts-nocheck
import { useContext, useEffect, useRef, useState } from "react";
import { Card, Stack } from "react-bootstrap";
import {
  cloneDeep,
  every,
  filter,
  find,
  flatten,
  forEach,
  get,
  isArray,
  isUndefined,
  map,
  uniq,
} from "lodash";
import { ServicesResponse } from "./types/StackServiceComponent";
import { ChooseServicesApi } from "../../api/chooseServicesApi";
import Spinner from "../../components/Spinner";
import { formatValuesBeforeSave, minToInstall } from "./utils";
import { isHAComponentOnly } from "../../Utils/numberUtils";
import VersionsApi from "../../api/versionsApi";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import ClusterDeploymentApi from "../../api/clusterDeployment";
import ClusterApi from "../../api/clusterApi";
import ExecuteTasksModal from "../../components/ExecuteTasksModal";
import { ServiceApi } from "../../api/serviceApi";
import { ActionTypes } from "./clusterStore/types";
import { ContextWrapper } from ".";
import { HostsApi } from "../../api/hostsApi";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import KerberosApi from "../../api/kerberosApi";
import { getConfigTagFromFileName } from "../CommonConfigs/ConfigUtils";
import { AppContext } from "../../store/context";

type Step8Props = {
  wizardName?: string;
};

function Step8({ wizardName = "clusterCreation" }: Step8Props) {
  const { Context } = useContext(ContextWrapper);
  const { state, dispatch, installedServices = [] }: any = useContext(Context);
  const {
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      jumpToStep,
      prevStepNumber,
      handleBackImperitive,
    },
  }: any = useContext(Context);

  const [serviceComponents, setServiceComponents] = useState<ServicesResponse>({
    items: [],
  });
  const [completedOperationsCount, setCompletedOperationsCount] = useState(0);
  const [failedOperationsCount, setFailedOperationsCount] = useState(0);
  const [showExecutionModal, setShowExecutionModal] = useState(false);
  const [isNextEnabled, setIsNextEnabled] = useState(true);
  const [deploymentTriggered, setDeploymentTriggered] = useState(false);
  const getStepData = (stepName: string, dataKey: string) => {
    const stepData = get(state, `${wizardName}Steps.${stepName}.data`, {});
    return get(stepData, dataKey, "");
  };

  const {
      clusterName = "",
      cluster: { stack, versionNum },
    } = useContext(AppContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const versionStepData = get(state, `${wizardName}Steps.VERSION.data`, {});
  const VERSION = versionNum || get(versionStepData, "selectedVersion.stack_version", "");
  const STACK = stack || get(versionStepData, "selectedStack.stack_name", "");
  const [isGoingToNextStep, setIsGoingToNextStep] = useState(false);

  // Kerberos-related state variables
  const [kerberosDescriptor, setKerberosDescriptor] = useState<any>(null);
  const [isKerberosEnabled, setIsKerberosEnabled] = useState<boolean>(false);
  const [isManualKerberos, setIsManualKerberos] = useState<boolean>(false);
  const [isClusterDescriptorExists, setIsClusterDescriptorExists] =
    useState<boolean>(false);

  /**
   * This function updates stack/service/component level kerberos descriptor identities (principal and keytab)
   * with the values entered by the user on the rendered UI.
   * @param {Array} identities
   * @param {Object} config
   * @return {boolean}
   */
  const updateDescriptorIdentityConfig = (
    identities: any[],
    config: any
  ): boolean => {
    let isConfigUpdated = false;

    identities.forEach((identity) => {
      const keys = Object.keys(identity).filter((key) => key !== "name");

      keys.forEach((item) => {
        const prop = identity[item];

        // Compare UI rendered config against identity with `configuration attribute` (Most of the identities have `configuration attribute`)
        const isIdentityWithConfig =
          prop.configuration &&
          prop.configuration.split("/")[0] ===
            getConfigTagFromFileName(config.filename) &&
          prop.configuration.split("/")[1] === config.name;

        // Compare UI rendered config against identity without `configuration attribute` (For example spnego principal and keytab)
        const isIdentityWithoutConfig =
          !prop.configuration &&
          identity.name === config.name.split("_")[0] &&
          item === config.name.split("_")[1];

        if (isIdentityWithConfig || isIdentityWithoutConfig) {
          prop[item === "keytab" ? "file" : "value"] = config.value;
          isConfigUpdated = true;
        }
      });
    });

    return isConfigUpdated;
  };

  /**
   * This function updates stack/service/component level configurations of the kerberos descriptor
   * with the values entered by the user on the rendered ui
   * @param {any} configurations
   * @param {any} config
   * @return {boolean}
   */
  const updateDescriptorConfigs = (
    configurations: any,
    config: any
  ): boolean => {
    let isConfigUpdated = false;

    if (configurations) {
      if (Array.isArray(configurations)) {
        configurations.forEach((configuration) => {
          for (const key in configuration) {
            if (
              configuration[key].hasOwnProperty(config.name) &&
              getConfigTagFromFileName(config.filename) === key
            ) {
              configuration[key][config.name] = config.value;
              isConfigUpdated = true;
            }
          }
        });
      } else if (
        configurations.hasOwnProperty(config.name) &&
        getConfigTagFromFileName(config.filename) === "stackConfigs"
      ) {
        configurations[config.name] = config.value;
        isConfigUpdated = true;
      }
    }

    return isConfigUpdated;
  };

  /**
   * Updates the identity configs or configurations at a resource. A resource could be
   * 1) Stack
   * 2) Service
   * 3) Component
   * @param {any} resource
   * @param {any} config
   * @param {boolean} isStackResource
   * @return {boolean}
   */
  const updateResourceIdentityConfigs = (
    resource: any,
    config: any,
    isStackResource = false
  ): boolean => {
    let isConfigUpdated;
    const identities = resource.identities;
    const properties = isStackResource
      ? resource.properties
      : resource.configurations;

    isConfigUpdated = updateDescriptorConfigs(properties, config);

    if (!isConfigUpdated && identities) {
      isConfigUpdated = updateDescriptorIdentityConfig(identities, config);
    }

    return isConfigUpdated;
  };

  /**
   * Update the kerberos descriptor to be put on cluster resource with user customizations
   * @param {any} kerberosDescriptor
   * @param {any[]} configs
   */
  const updateKerberosDescriptor = (
    kerberosDescriptor: any,
    configs: any[]
  ): void => {
    configs.forEach((config) => {
      let isConfigUpdated;
      const isStackResource = true;
      isConfigUpdated = updateResourceIdentityConfigs(
        kerberosDescriptor,
        config,
        isStackResource
      );

      if (!isConfigUpdated) {
        kerberosDescriptor.services.forEach((service: any) => {
          isConfigUpdated = updateResourceIdentityConfigs(service, config);
          if (!isConfigUpdated) {
            (service.components || []).forEach((component: any) => {
              isConfigUpdated = updateResourceIdentityConfigs(
                component,
                config
              );
            });
          }
        });
      }
    });
  };

  /**
   * Updates kerberosDescriptorConfigs
   * @param {boolean} instant - whether to execute immediately or add to ajax queue
   */
  const updateKerberosDescriptorMethod = async (
    instant = false
  ): Promise<void> => {
    try {
      // Use the loaded kerberos descriptor
      const kerberosDescriptorConfigs = kerberosDescriptor;
      const descriptorExists = isClusterDescriptorExists;

      if (!kerberosDescriptorConfigs) {
        console.warn("No kerberos descriptor configs found");
        return;
      }

      // Remove identity references before sending to server
      const cleanedDescriptor = removeIdentityReferences(
        cloneDeep(kerberosDescriptorConfigs)
      );

      const payload = {
        artifact_data: cleanedDescriptor,
      };

      if (instant) {
        // Execute immediately
        await KerberosApi.saveAndEditKerberosData(
          getStepData("NAME", "clusterName")||clusterName,
          payload
        );
      } else {
        // Add to deployment promises queue
        const kerberosPromise = KerberosApi.saveAndEditKerberosData(
          getStepData("NAME", "clusterName")||clusterName,
          payload
        );

        pushTask(
          kerberosPromise
            .then(() => {
              incrementSuccessCount();
            })
            .catch(() => {
              incrementSuccessCount();
            })
        );
      }
    } catch (error) {
      console.error("Error updating kerberos descriptor:", error);
      if (!instant) {
        incrementSuccessCount();
      }
    }
  };

  /**
   * The UI should ignore Kerberos identity references
   * when setting the user-supplied Kerberos descriptor
   * @param {any} kerberosDescriptor
   * @returns {any}
   */
  const removeIdentityReferences = (kerberosDescriptor: any): any => {
    const notReference = (identity: any) =>
      !identity.reference && !identity.name.startsWith("/");

    if (kerberosDescriptor.services) {
      kerberosDescriptor.services.forEach((service: any) => {
        if (service.identities) {
          service.identities = service.identities.filter(notReference);
        }
        if (service.components) {
          service.components.forEach((component: any) => {
            if (component.identities) {
              component.identities = component.identities.filter(notReference);
            }
          });
        }
      });
    }

    return kerberosDescriptor;
  };

  /**
   * Initialize Kerberos-related data and state
   */
  const initializeKerberosData = async (): Promise<void> => {
    try {
      const clusterName = getStepData("NAME", "clusterName")||clusterName;

      // For new cluster creation, assume Kerberos is not enabled initially
      // This will be determined during the actual deployment process
      if (!clusterName || wizardName === "clusterCreation") {
        setIsKerberosEnabled(false);
        setIsManualKerberos(false);
        setIsClusterDescriptorExists(false);
        return;
      }

      // For existing clusters (Add Service/Add Host), check if Kerberos is enabled
      try {
        const securityType = await KerberosApi.getSecurityType(clusterName);
        const isKerberosEnabledValue =
          securityType?.Clusters?.security_type === "KERBEROS";
        setIsKerberosEnabled(isKerberosEnabledValue);

        // For existing clusters, assume it's not manual Kerberos unless specified
        setIsManualKerberos(false);

        // Load kerberos descriptor if available
        if (isKerberosEnabledValue) {
          try {
            const descriptor =
              await KerberosApi.getKerberosDescriptorProperties(
                "true",
                clusterName
              );
            setKerberosDescriptor(
              descriptor?.KerberosDescriptor?.kerberos_descriptor
            );
            setIsClusterDescriptorExists(
              !!descriptor?.KerberosDescriptor?.kerberos_descriptor
            );
          } catch (error) {
            console.warn("Could not load kerberos descriptor:", error);
            setIsClusterDescriptorExists(false);
          }
        } else {
          setIsClusterDescriptorExists(false);
        }
      } catch (error) {
        console.warn("Could not check security type:", error);
        setIsKerberosEnabled(false);
        setIsManualKerberos(false);
        setIsClusterDescriptorExists(false);
      }
    } catch (error) {
      console.error("Error initializing Kerberos data:", error);
      setIsKerberosEnabled(false);
      setIsManualKerberos(false);
      setIsClusterDescriptorExists(false);
    }
  };

  // Initialize Kerberos data on component mount
  useEffect(() => {
    if (!isAddHostWizard()) {
      initializeKerberosData();
    }
  }, []);

  function renderRepos() {
    const operatingSystems = getStepData("VERSION", "operatingSystems");
    const selectedStack = getStepData("VERSION", "selectedStack.id");
    const addedOs = operatingSystems[selectedStack].filter(
      (os: any) => os.isAdded
    );
    const allRepos = addedOs.map((currentOs: any) => {
      return currentOs.repos.map((repo: any) => {
        return (
          <Stack direction="vertical" className="m-3">
            <div className="text-info">
              {currentOs.os}({repo.id})
            </div>
            <div className="mt-2">{repo.baseUrl}</div>
          </Stack>
        );
      });
    });
    return allRepos;
  }
  useEffect(() => {
    if (isGoingToNextStep) {
      setTimeout(() => {
        flushStateToDb("next");
      }, 2000);
    }
  }, [isGoingToNextStep]);
  const deploymentPromises = useRef<Promise<any>[]>([]);

  async function getServiceComponents() {
    const servicesAndComponents: ServicesResponse =
      await ChooseServicesApi.getServices(STACK, VERSION);
    setServiceComponents(servicesAndComponents);
  }

  useEffect(() => {
    getServiceComponents();
  }, []);

  useEffect(() => {
    if (
      completedOperationsCount &&
      deploymentPromises.current.length &&
      completedOperationsCount === deploymentPromises.current.length
    ) {
      setIsNextEnabled(false);
    }
  }, [completedOperationsCount, failedOperationsCount]);

  function getNewHosts() {
    return getStepData("HOST_STATUS", "hosts")?.filter(
      (host: any) => host.bootStatus === "REGISTERED"
    );
  }

  function getHosts() {
    const installedHosts = getStepData("HOSTS", "installedHosts");
    const registeredHosts = getNewHosts();
    const allHosts = [...installedHosts, ...registeredHosts];
    return allHosts;
  }

  function getMasterComponentValue(componentName: string) {
    const masterComponents = getStepData("MASTERS", "mastersData");
    const allMasterServices = flatten(map(masterComponents, "masterServices"));
    const hosts = allMasterServices.filter((masterComponent: any) => {
      return masterComponent.component === componentName;
    });
    const hostsCount = hosts.length;
    if (hostsCount === 1) {
      return hosts[0]?.hostName;
    } else {
      return `${hostsCount} hosts`;
    }
  }

  function assignComponentHosts(component: any) {
    let componentValue;
    if (component.is_master) {
      componentValue = getMasterComponentValue(component.component_name);
    } else {
      const clientSlaveData = getStepData(
        "SLAVES_AND_CLIENTS",
        "serviceComponents"
      );
      const selectedComponentHosts = [];
      for (let clientServiceComponent of clientSlaveData) {
        const isComponentSelected = clientServiceComponent.checkboxes.find(
          (checkbox: { label: string; checked: boolean }) =>
            checkbox["label"] === component.component_name && checkbox.checked
        );
        if (isComponentSelected) {
          selectedComponentHosts.push(clientServiceComponent.hostname);
        }
      }
      componentValue = `${selectedComponentHosts.length} host${
        selectedComponentHosts.length > 1 ? "s" : ""
      }`;
    }
    return componentValue;
  }

  function renderServices() {
    const servicesStepData = getStepData("SERVICES", "services");
    const selectedServices = [];
    const allServices = servicesStepData;
    for (let service in allServices) {
      const currentService = allServices[service];
      if (currentService.selected) {
        selectedServices.push({
          service_name: currentService.serviceName,
          display_name: currentService.display_name,
          service_components: [],
          shouldShow: !installedServices.includes(currentService.serviceName),
        });
      }
    }
    for (let selectedService of selectedServices) {
      const selectedServiceComponents: any = [];
      const currentServiceComponents = serviceComponents.items.find(
        (stackService) => {
          return (
            get(stackService, "StackServices.service_name") ===
            selectedService.service_name
          );
        }
      );
      const allComponents = get(currentServiceComponents, "components");
      const isClientOnlyService = every(
        allComponents,
        "StackServiceComponents.is_client"
      );
      if (isClientOnlyService) {
        const clientSlaveData = getStepData(
          "SLAVES_AND_CLIENTS",
          "serviceComponents"
        );
        const selectedComponentHosts = [];
        for (let clientServiceComponent of clientSlaveData) {
          const isComponentSelected = clientServiceComponent.checkboxes.find(
            (checkbox: { label: string; checked: boolean }) =>
              checkbox["label"] === "CLIENT" && checkbox.checked
          );
          if (isComponentSelected) {
            selectedComponentHosts.push(clientServiceComponent.hostname);
          }
        }
        let componentValue = `${selectedComponentHosts.length} host${
          selectedComponentHosts.length > 1 ? "s" : ""
        }`;
        selectedServiceComponents.push({
          displayName: "Clients",
          componentName: get(
            allComponents,
            "0.StackServiceComponents.component_name",
            ""
          ),
          componentValue,
          shouldShow: !installedServices.includes(selectedService.service_name),
        });
      } else {
        if (currentServiceComponents) {
          forEach(
            get(currentServiceComponents, "components", []),
            (componentInfo) => {
              const component: any = get(
                componentInfo,
                "StackServiceComponents",
                {}
              );
              const componentName = component.component_name;
              // show clients for services that have only clients components
              if (
                (component.is_client ||
                  minToInstall(component.cardinality) === Infinity) &&
                !isClientOnlyService
              ) {
                return;
              }
              // no HA component
              if (isHAComponentOnly(componentName)) {
                return;
              }
              // skip if component is not allowed on single node cluster
              if (componentName === "HAWQSTANDBY" && getHosts()?.length === 1) {
                return;
              }
              const displayName = componentName.replace(
                new RegExp("^" + component.service_name + "\\s", "i"),
                ""
              );
              const masterComponentData = getStepData("MASTERS", "mastersData");
              const masterComponents = flatten(
                map(masterComponentData, "masterServices")
              );
              const isMasterComponentSelected = !!masterComponents.find(
                (masterComponent) => masterComponent.component === componentName
              );
              const isMaster = component.is_master;
              if (!isMaster || isMasterComponentSelected) {
                selectedServiceComponents.push({
                  displayName,
                  componentName,
                  shouldShow: !installedServices.includes(
                    selectedService.service_name
                  ),
                  componentValue: assignComponentHosts(component),
                });
              }
            }
          );
        }
      }
      selectedService.service_components = cloneDeep(selectedServiceComponents);
    }
    return selectedServices.map((selectedService) => {
      return selectedService.shouldShow ? (
        <Stack direction="vertical" className="mt-2">
          <div className="my-2">
            <b>
              <i>{selectedService.service_name}</i>
            </b>
          </div>
          {selectedService.service_components.map((serviceComponent: any) => {
            return (
              <Stack direction="horizontal" className="mt-2">
                <div className="text-info">{serviceComponent.displayName}:</div>
                <div className="ms-2">{serviceComponent.componentValue}</div>
              </Stack>
            );
          })}
        </Stack>
      ) : null;
    });
  }

  // const createCluster=()=>{
  //   const stackVersion=
  // }
  async function postVersionDefinition(data: any) {
    const versionInfo = await VersionsApi.postVersionDefinitionFile(data);
    return versionInfo;
  }

  async function getUpdateRepoOSInfoBody() {
    const usesRedhat = getStepData("VERSION", "redhatSatellite");
    const selectedStack = getStepData("VERSION", "selectedStack.id");
    const operatingSystemsFromState = getStepData(
      "VERSION",
      `operatingSystems`
    );
    const operatingSystems = operatingSystemsFromState[selectedStack];
    if (isArray(operatingSystems) && operatingSystems.length) {
      const selectedOperatingSystems = operatingSystems?.filter(
        (os: any) => os.isAdded
      );
      const osPayload = selectedOperatingSystems.map((selectedOs) => {
        return {
          OperatingSystems: {
            ambari_managed_repositories: !usesRedhat,
            os_type: selectedOs.os,
          },
          repositories: selectedOs.repos.map((selectedRepo: any) => {
            return {
              Repositories: {
                base_url: selectedRepo.baseUrl,
                repo_id: selectedRepo.id,
                repo_name: selectedRepo.name,
                tags: [],
                applicable_services: [],
                components: null,
                distribution: null,
              },
            };
          }),
        };
      });

      return { operating_systems: osPayload };
    }
    return {};
  }

  const incrementSuccessCount = () => {
    setCompletedOperationsCount((comp) => comp + 1);
  };
  const incrementFailureCount = () => {
    setFailedOperationsCount((comp) => comp + 1);
  };

  function createCluster() {
    const selectedStackVersion = getStepData("VERSION", "selectedStack.id");
    const clusterName = getStepData("NAME", "clusterName");
    return ClusterDeploymentApi.createCluster(clusterName, {
      Clusters: {
        version: selectedStackVersion,
      },
    });
  }

  async function createSelectedServices(versionId: string) {
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      function (service: any) {
        return !service.installed && service.selected;
      }
    );
    const selectedServicesBody = selectedServices.map(
      (selectedService: any) => {
        const serviceInfoObj = {
          service_name: selectedService.serviceName,
        };
        if (!isAddServiceWizard()) {
          serviceInfoObj["desired_repository_version_id"] = versionId;
        }
        return {
          ServiceInfo: serviceInfoObj,
        };
      }
    );
    return ClusterDeploymentApi.createSelectedServices(
      getStepData("NAME", "clusterName"),
      selectedServicesBody
    )
      .then(() => {
        incrementSuccessCount();
      })
      .catch(() => {
        incrementFailureCount();
      });
  }

  function getServiceComponentsForService(serviceName: string) {
    const selectedServiceComponents = serviceComponents.items.filter(
      (stackService: any) => {
        return get(stackService, "StackServices.service_name") === serviceName;
      }
    );
    const matchedServiceComponents = map(
      flatten(selectedServiceComponents),
      "components"
    )?.[0];
    return matchedServiceComponents;
  }

  async function createComponents() {
    const componentsPromise = [];
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      function (service: any) {
        return service.selected && !service.installed;
      }
    );
    for (const selectedService of selectedServices) {
      const matchedServiceComponents = getServiceComponentsForService(
        selectedService.serviceName
      );
      const requestBody = {
        components: matchedServiceComponents.map((serviceComponent: any) => {
          return {
            ServiceComponentInfo: {
              component_name:
                serviceComponent.StackServiceComponents.component_name,
            },
          };
        }),
      };
      componentsPromise.push(
        new Promise((resolve, reject) => {
          setTimeout(() => {
            ClusterDeploymentApi.addRequestToCreateComponent(
              getStepData("NAME", "clusterName")||clusterName,
              selectedService.serviceName,
              requestBody
            )
              .then(() => {
                incrementSuccessCount();
              })
              .catch(() => {
                incrementFailureCount();
              });
          }, deploymentPromises.current.length * 0.7 * 1000);
        })
      );
    }
    return componentsPromise;
  }

  async function registerHostsToComponent(
    hostNames: string[],
    component: string
  ) {
    if (!hostNames.length) return;

    let queryStr = "";
    hostNames.forEach(function (hostName) {
      queryStr += "Hosts/host_name=" + hostName + "|";
    });
    //slice off last symbol '|'
    queryStr = queryStr.slice(0, -1);

    const data = {
      RequestInfo: {
        query: queryStr,
      },
      Body: {
        host_components: [
          {
            HostRoles: {
              component_name: component,
            },
          },
        ],
      },
    };
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        resolve(
          ClusterDeploymentApi.registerHostToCluster(
            getStepData("NAME", "clusterName")||clusterName,
            data
          )
            .then(() => {
              incrementSuccessCount();
            })
            .catch(() => {
              incrementFailureCount();
            })
        );
      }, deploymentPromises.current.length * 0.7 * 1000);
    });
  }

  async function createMasterHostComponents() {
    const masterOnAllHosts: any = [];
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      function (service: any) {
        return !service.installed && service.selected;
      }
    );
    const registerPromises = [];
    forEach(selectedServices, (service: any) => {
      const selectedServiceComponents = getServiceComponentsForService(
        service.serviceName
      );
      const requiredServiceComponents = selectedServiceComponents.filter(
        (serviceComponent: any) => {
          return (
            minToInstall(
              serviceComponent.StackServiceComponents.cardinality
            ) === Infinity
          );
        }
      );
      forEach(requiredServiceComponents, (requiredServiceComponent: any) => {
        if (requiredServiceComponent.StackServiceComponents.is_master) {
          masterOnAllHosts.push(
            requiredServiceComponent.StackServiceComponents.component_name
          );
        }
      });
    });
    const mastersData = getStepData("MASTERS", "mastersData");
    const masterServices = filter(
      flatten(map(mastersData, "masterServices")),
      function (component) {
        return !component.isInstalled;
      }
    );
    const selectedMasterComponents = uniq(map(masterServices, "component"));
    forEach(selectedMasterComponents, (component) => {
      let hostNames = [];
      if (masterOnAllHosts.length > 0) {
        let compOnAllHosts = false;
        for (let i = 0; i < masterOnAllHosts.length; i++) {
          if (component === masterOnAllHosts[i]) {
            compOnAllHosts = true;
            break;
          }
        }
        if (!compOnAllHosts) {
          hostNames = map(
            filter(
              filter(masterServices, ["component", component]),
              (slaveHost) => {
                return (
                  slaveHost.isInstalled === false ||
                  isUndefined(slaveHost.isInstalled)
                );
              }
            ),
            "hostName"
          );
          registerPromises.push(registerHostsToComponent(hostNames, component));
        }
      } else {
        hostNames = map(
          filter(
            filter(masterServices, ["component", component]),
            (slaveHost) => {
              return (
                slaveHost.isInstalled === false ||
                isUndefined(slaveHost.isInstalled)
              );
            }
          ),
          "hostName"
        );
        registerPromises.push(registerHostsToComponent(hostNames, component));
      }
    });
    return registerPromises;
  }

  function saveClusterStatus(clusterStatusLocal) {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: {
          clusterStatus: clusterStatusLocal,
        },
      },
    });
  }

  async function installComponents() {
    const data = {
      context: "Install Components",
      HostRoles: { state: "INSTALLED" },
      urlParams: "",
      level: "HOST_COMPONENT",
      query: `HostRoles/host_name.in(${getNewHosts()
        .map((host) => get(host, "name", ""))
        .join(",")})`,
    };

    const clusterName = getStepData("NAME", "clusterName")||clusterName;

    try {
      const response = await HostsApi.updateHostComponents(
        clusterName,
        data.urlParams,
        data
      );
      const requestId = response.Requests.id;
      const responseStatus = {
        status: "PENDING",
        requestId,
        isInstallError: false,
        isCompleted: false,
        oldRequestsId: getStepData("REVIEW", "clusterStatus").oldRequestsId
          ? [...previousRequests, requestId]
          : [requestId],
      };
      saveClusterStatus(responseStatus);
      setIsGoingToNextStep(true);
      setTimeout(() => {
        handleNextImperitive();
      }, 2000);
    } catch (err) {
      const responseStatus = {
        status: "PENDING",
        isInstallError: true,
        isCompleted: false,
      };
      saveClusterStatus(responseStatus);
    }
  }

  async function installServices() {
    let urlParams = "ServiceInfo/state=INIT";
    const data = {
      context: "Install Services",
      ServiceInfo: { state: "INSTALLED" },
    };
    const selectedServices = map(
      filter(getStepData("SERVICES", "services"), function (service: any) {
        return !service.installed && service.selected;
      }),
      "serviceName"
    )?.join(",");
    if (isAddServiceWizard()) {
      urlParams = `ServiceInfo/service_name.in(${selectedServices})`;
    }
    saveClusterStatus({
      status: "PENDING",
    });
    const servicesInit = await ServiceApi.updateService(
      getStepData("NAME", "clusterName")||clusterName,
      data,
      urlParams
    );
    const installStartTime = Date.now();
    if (servicesInit.Requests) {
      const previousRequests = getStepData(
        "REVIEW",
        "clusterStatus"
      ).oldRequestsId;
      const requestId = servicesInit.Requests.id;
      const clusterStatus = {
        status: "PENDING",
        requestId,
        isInstallError: false,
        isCompleted: false,
        installStartTime,
        oldRequestsId: getStepData("REVIEW", "clusterStatus").oldRequestsId
          ? [...previousRequests, requestId]
          : [requestId],
      };
      saveClusterStatus(clusterStatus);
      setIsGoingToNextStep(true);
      setTimeout(() => {
        handleNextImperitive();
      }, 2000);
    }
  }

  async function registerHostsToCluster() {
    const registeredHosts = filter(getStepData("HOST_STATUS", "hosts"), [
      "bootStatus",
      "REGISTERED",
    ]);
    const requestPayload = registeredHosts.map((registeredHost) => {
      return {
        Hosts: {
          host_name: get(registeredHost, "name", ""),
        },
      };
    });
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        resolve(
          ClusterDeploymentApi.registerHostToCluster(
            getStepData("NAME", "clusterName")||clusterName,
            requestPayload
          )
            .then(() => {
              incrementSuccessCount();
            })
            .catch(() => {
              incrementFailureCount();
            })
        );
      }, deploymentPromises.current.length * 0.7 * 1000);
    });
  }

  async function createConfigurationGroups() {}

  function getClientsMap(flag: string, value?: string) {
    const serviceComponentItems = serviceComponents.items;
    const allComponents = flatten(map(serviceComponentItems, "components"));
    const clients = filter(allComponents, [
      "StackServiceComponents.is_client",
      true,
    ]);
    const clientsMap: any = {};
    const dependentComponents = flag
      ? filter(allComponents, [`StackServiceComponents.${flag}`, value || true])
      : [];
    forEach(clients, (client: any) => {
      const clientName = get(
        client,
        "StackServiceComponents.component_name",
        ""
      );
      clientsMap[clientName] = [];
      forEach(dependentComponents, (component: any) => {
        const dependsOn = map(
          component.dependencies,
          "Dependencies.component_name"
        );
        if (dependsOn.includes(clientName)) {
          clientsMap[clientName].push(
            get(component, "StackServiceComponents.component_name", "")
          );
        }
      });
      if (!clientsMap[clientName].length) delete clientsMap[clientName];
    });
    return clientsMap;
  }

  async function createAdditionalHostComponents() {
    // const masterHosts = flatten(map(getStepData("MASTERS", "mastersData"),"masterServices"));
    const additionalComponentPromises = [];
    const registeredHosts = filter(getStepData("HOST_STATUS", "hosts"), [
      "bootStatus",
      "REGISTERED",
    ]);
    // const notInstalledHosts=filter(registeredHosts,["isInstalled",false]);
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      function (service: any) {
        return !service.installed && service.selected;
      }
    );
    for (const service of selectedServices) {
      const serviceComponentsForService = getServiceComponentsForService(
        service.serviceName
      );
      const servicesRequiredOnAllHosts = serviceComponentsForService.filter(
        (sC: any) => {
          return (
            minToInstall(sC.StackServiceComponents.cardinality) === Infinity
          );
        }
      );
      forEach(servicesRequiredOnAllHosts, (component: any) => {
        const requiredComponent = get(component, "StackServiceComponents", {});
        if (registeredHosts.length) {
          additionalComponentPromises.push(
            registerHostsToComponent(
              map(registeredHosts, "name"),
              requiredComponent.component_name
            )
          );
        }
      });
    }
    //add Mysql server if HIVE is selected
    // const isHiveSelected=!!getStepData("SERVICES","services")?.["HIVE"]?.selected
    // if(isHiveSelected){
    //   const hiveDb=
    // }
    return additionalComponentPromises;
  }

  async function createSlaveAndClientsHostComponents() {
    const installedHosts = getStepData("HOSTS", "installedHosts");
    const masterHosts = flatten(
      map(getStepData("MASTERS", "mastersData"), "masterServices")
    );
    const slaveHostsData = getStepData(
      "SLAVES_AND_CLIENTS",
      "serviceComponents"
    );
    const slaveHosts: any = [];
    for (let component of slaveHostsData as any) {
      const currentComponents = filter(
        component.checkboxes,
        function (component) {
          return component.checked;
        }
      );
      for (const currentComponent of currentComponents) {
        // if (currentComponent.checked) {
        const existingComponentInSlaveHosts = find(slaveHosts, [
          "componentName",
          currentComponent.label,
        ]);
        const isHostInstalled = installedHosts.includes(component.hostname);
        const newHostObj = {
          group: "Default",
          isInstalled: isHostInstalled,
          host_id: find(masterHosts, [
            "host_name" || "hostName",
            component.hostname,
          ])?.masterServices?.[0]?.host_id,
          hostName: component.hostname,
        };
        if (existingComponentInSlaveHosts) {
          existingComponentInSlaveHosts.hosts = [
            ...existingComponentInSlaveHosts.hosts,
            newHostObj,
          ];
          const matchingSlaveHost = find(slaveHosts, [
            "componentName",
            currentComponent.label,
          ]);
          const hostnameExists = find(matchingSlaveHost.hosts, [
            "hostName",
            component.hostname,
          ]);
          if (matchingSlaveHost && !hostnameExists) {
            matchingSlaveHost.hosts = [...matchingSlaveHost.hosts, newHostObj];
          }
        } else {
          if (!currentComponent.isDisabled) {
            if (isAddHostWizard()) {
              if (!newHostObj.isInstalled) {
                slaveHosts.push({
                  componentName: currentComponent.label,
                  displayName: currentComponent.label,
                  hosts: [newHostObj],
                });
              }
            } else {
              slaveHosts.push({
                componentName: currentComponent.label,
                displayName: currentComponent.label,
                hosts: [newHostObj],
              });
            }
          }
        }
        // }
      }
    }
    const serviceComponentItems = serviceComponents.items;
    const allComponents = flatten(map(serviceComponentItems, "components"));
    const allClientComponents = filter(allComponents, [
      "StackServiceComponents.is_client",
      true,
    ]);
    const selectedServices = map(
      filter(getStepData("SERVICES", "services"), function (service: any) {
        return !service.installed && service.selected;
      }),
      "serviceName"
    );
    const clients: any = [];
    const slaveOnAllHosts: any = [];
    const clientOnAllHosts: any = [];
    for (let clientComponent of allClientComponents as any) {
      if (
        selectedServices.includes(
          clientComponent.StackServiceComponents.service_name
        )
      ) {
        clients.push({
          component_name: clientComponent.StackServiceComponents.component_name,
          display_name: clientComponent.StackServiceComponents.component_name,
          isInstalled: false,
        });
      }
    }
    for (const service of selectedServices) {
      const serviceComponentsForService = filter(allComponents, [
        "StackServiceComponents.service_name",
        service,
      ]);
      const servicesRequiredOnAllHosts = serviceComponentsForService.filter(
        (sC: any) => {
          return (
            minToInstall(sC.StackServiceComponents.cardinality) === Infinity
          );
        }
      );
      for (let requiredComponent of servicesRequiredOnAllHosts as any) {
        requiredComponent = get(
          requiredComponent,
          "StackServiceComponents",
          {}
        );
        if (requiredComponent.is_client) {
          clientOnAllHosts.push(requiredComponent.component_name);
        } else if (requiredComponent.is_slave) {
          slaveOnAllHosts.push(requiredComponent.component_name);
        }
      }
    }
    /**
     * Determines on which hosts client should be installed (based on availability of master components on hosts)
     * @type {Object}
     * Format:
     * <code>
     *  {
     *    CLIENT1: Em.A([MASTER1, MASTER2, ...]),
     *    CLIENT2: Em.A([MASTER3, MASTER1, ...])
     *    ...
     *  }
     * </code>
     */
    const clientsToMasterMap = getClientsMap("is_master");
    const clientSlavePromises = [];
    const clientsToSlaveMap = getClientsMap("component_category", "SLAVE");
    forEach(slaveHosts, (_slave) => {
      let hostNames: any = [];
      let compOnAllHosts;
      if (_slave.componentName !== "CLIENT") {
        if (slaveOnAllHosts.length > 0) {
          compOnAllHosts = false;
          for (let i = 0; i < slaveOnAllHosts.length; i++) {
            if (_slave.componentName === slaveOnAllHosts[i]) {
              // component with ALL cardinality should not
              // registerHostsToComponent in createSlaveAndClientsHostComponents
              compOnAllHosts = true;
              break;
            }
            if (!compOnAllHosts) {
              hostNames = map(
                filter(_slave.hosts, (slaveHost) => {
                  return (
                    slaveHost.isInstalled === false ||
                    isUndefined(slaveHost.isInstalled)
                  );
                }),
                "hostName"
              );
              clientSlavePromises.push(
                registerHostsToComponent(hostNames, _slave.componentName)
              );
            } else {
              hostNames = map(
                filter(_slave.hosts, (slaveHost) => {
                  return (
                    slaveHost.isInstalled === false ||
                    isUndefined(slaveHost.isInstalled)
                  );
                }),
                "hostName"
              );
              clientSlavePromises.push(
                registerHostsToComponent(hostNames, _slave.componentName)
              );
            }
          }
        } else {
          hostNames = map(
            filter(_slave.hosts, (slaveHost) => {
              return (
                slaveHost.isInstalled === false ||
                isUndefined(slaveHost.isInstalled)
              );
            }),
            "hostName"
          );
          clientSlavePromises.push(
            registerHostsToComponent(hostNames, _slave.componentName)
          );
        }
      } else {
        clients.forEach(function (_client: any) {
          hostNames = map(_slave.hosts, "hostName");
          // The below logic to install clients to existing/New master hosts should not be applied to Add Host wizard.
          // This is with the presumption that Add Host controller does not add any new Master component to the cluster
          if (clientsToMasterMap[_client.component_name]) {
            clientsToMasterMap[_client.component_name].forEach(function (
              componentName: any
            ) {
              let inferredHosts = filter(masterHosts, (masterHost) => {
                return masterHost.component === componentName;
              });
              if (isAddHostWizard()) {
                inferredHosts = filter(inferredHosts, (masterHost) => {
                  return !installedHosts.includes(masterHost.hostName);
                });
              }
              inferredHosts.forEach(function (_masterHost: any) {
                hostNames.push(_masterHost.hostName);
              });
            });
          }
          if (clientsToSlaveMap[_client.component_name]) {
            clientsToSlaveMap[_client.component_name].forEach(function (
              componentName: string
            ) {
              filter(slaveHosts, ["componentName", componentName]).forEach(
                function (slaveHost) {
                  hostNames = uniq(
                    hostNames.concat(map(slaveHost.hosts, "hostName"))
                  );
                }
              );
            });
          }
          if (clientOnAllHosts.length > 0) {
            compOnAllHosts = false;
            for (let i = 0; i < clientOnAllHosts.length; i++) {
              if (_client.component_name === clientOnAllHosts[i]) {
                // component with ALL cardinality should not
                // registerHostsToComponent in createSlaveAndClientsHostComponents
                compOnAllHosts = true;
                break;
              }
            }
            if (!compOnAllHosts) {
              hostNames = uniq(hostNames);
              clientSlavePromises.push(
                registerHostsToComponent(hostNames, _client.component_name)
              );
            }
          } else {
            hostNames = uniq(hostNames);
            clientSlavePromises.push(
              registerHostsToComponent(hostNames, _client.component_name)
            );
          }
        });
      }
    });
    return clientSlavePromises;
  }

  function applyConfigurationsToCluster() {
    const applyConfigurationsPayload = [];
    const configurations = getStepData("CONFIGURATION", "configProperties");
    Object.keys(configurations).map((service: string) => {
      if (service === "MISC") return;
      if (installedServices.includes(service)) return;
      const serviceConfigurations = configurations[service];
      const serviceConfigurationsTypes = {};
      Object.keys(serviceConfigurations).map((configSection: string) => {
        Object.keys(serviceConfigurations[configSection].properties).map(
          (property: string) => {
            const propertyName =
              serviceConfigurations[configSection].properties[property]
                .propertyName;
            const propertyType =
              serviceConfigurations[configSection].properties[property].type;

            if (propertyType === "hosts") {
              return;
            }

            if (!serviceConfigurationsTypes[propertyType]) {
              serviceConfigurationsTypes[propertyType] = {};
            }
            if (
              serviceConfigurations[configSection].properties[property]
                .value !== null
            ) {
              serviceConfigurationsTypes[propertyType][propertyName] =
                formatValuesBeforeSave(
                  serviceConfigurations[configSection].properties[property]
                );
            }
          }
        );
      });
      Object.keys(configurations["MISC"]["Users and Groups"]?.properties)?.map(
        (property: string) => {
          const propertyName =
            configurations["MISC"]["Users and Groups"]?.properties[property]
              ?.propertyName;
          const propertyType =
            configurations["MISC"]["Users and Groups"]?.properties[property]
              ?.type;
          const serviceName =
            configurations["MISC"]["Users and Groups"]?.properties[property]
              ?.serviceName;
          const value =
            configurations["MISC"]["Users and Groups"]?.properties[property]
              ?.value;
          if (service === serviceName) {
            if (!serviceConfigurationsTypes[propertyType]) {
              serviceConfigurationsTypes[propertyType] = {};
            }
            serviceConfigurationsTypes[propertyType][propertyName] = value;
          }
        }
      );

      const desiredConfigs = [];

      Object.keys(serviceConfigurationsTypes).map((type: string) => {
        let inferredProperties = serviceConfigurationsTypes[type];
        desiredConfigs.push({
          type,
          properties: inferredProperties,
          service_config_version_note: `Initial version of ${service} configurations`,
        });
      });

      applyConfigurationsPayload.push({
        Clusters: {
          desired_config: desiredConfigs,
        },
      });
    });
    return ClusterDeploymentApi.applyClusterConfigs(
      getStepData("NAME", "clusterName")||clusterName,
      applyConfigurationsPayload
    )

      .then(() => {
        incrementSuccessCount();
      })
      .catch(() => {
        incrementFailureCount();
      });
  }

  const pushTask = (task) => {
    deploymentPromises.current.push(task);
  };

  const startDeployForAddHost = async () => {
    try {
      pushTask(registerHostsToCluster());
      for (const slaveComponentPromise of await createSlaveAndClientsHostComponents()) {
        pushTask(slaveComponentPromise);
      }
      setShowExecutionModal(true);
      for (const deploymentPromise of deploymentPromises.current) {
        await deploymentPromise;
      }
    } catch (err) {
      console.log("Could not deploy Cluster", err);
    }
  };

  const startDeploymentForAddService = async () => {
    try {
      pushTask(createSelectedServices());

      // For manually enabled Kerberos descriptor was updated on transition to this step
      if (isKerberosEnabled && !isManualKerberos) {
        await updateKerberosDescriptorMethod();
      }

      pushTask(applyConfigurationsToCluster());
      for (const componentPromise of await createComponents()) {
        pushTask(componentPromise);
      }
      for (const masterComponentPromise of await createMasterHostComponents()) {
        pushTask(masterComponentPromise);
      }
      for (const slaveComponentPromise of await createSlaveAndClientsHostComponents()) {
        pushTask(slaveComponentPromise);
      }
      for (const additionalComponentPromise of await createAdditionalHostComponents()) {
        pushTask(additionalComponentPromise);
      }
      setShowExecutionModal(true);
      await Promise.all(deploymentPromises.current);
    } catch (err) {
      console.log("Could not deploy Cluster", err);
    }
  };

  const startDeploy = async () => {
    try {
      const versionStepData = getStepData("VERSION", "selectedStack");
      const versionData = {
        isXMLdata: false,
        data: {
          VersionDefinition: {
            available: versionStepData.id,
          },
        },
      };
      const versionInfoResponse = await postVersionDefinition(versionData.data);
      if (versionInfoResponse) {
        const versionInfo = {
          stackName:
            versionInfoResponse.resources[0].VersionDefinition.stack_name,
          id: versionInfoResponse.resources[0].VersionDefinition.id,
          stackVersion:
            versionInfoResponse.resources[0].VersionDefinition.stack_version,
        };
        if (
          versionInfo.id &&
          versionInfo.stackName &&
          versionInfo.stackVersion
        ) {
          const selectedStack = STACK;
          const selectedVersion = VERSION;
          const repoId = versionInfo.id;
          const payload = await getUpdateRepoOSInfoBody();
          await VersionsApi.updateRepoOSInfo(
            selectedStack,
            selectedVersion,
            repoId,
            payload
          );
          pushTask(
            createCluster().then(async () => {
              incrementSuccessCount();
              pushTask(createSelectedServices(versionInfo.id));
              pushTask(applyConfigurationsToCluster());
              for (const componentPromise of await createComponents()) {
                pushTask(componentPromise);
              }
              pushTask(registerHostsToCluster());
              for (const masterComponentPromise of await createMasterHostComponents()) {
                pushTask(masterComponentPromise);
              }
              for (const slaveComponentPromise of await createSlaveAndClientsHostComponents()) {
                pushTask(slaveComponentPromise);
              }
              for (const additionalComponentPromise of await createAdditionalHostComponents()) {
                pushTask(additionalComponentPromise);
              }
              setShowExecutionModal(true);
              await Promise.all(deploymentPromises.current);
            })
          );
        }
      }
    } catch (err) {
      console.log("Could not deploy Cluster", err);
    }
  };
  const prepareDeployment = async () => {
    const existingCluster = await ClusterApi.getAllClusters();
    const deletionPromises: any = [];
    if (get(existingCluster, "items", []).length) {
      forEach(get(existingCluster, "items", []), (cluster) => {
        deletionPromises.push(
          ClusterApi.deleteCluster(cluster.Clusters.cluster_name)
        );
      });
    }
    await Promise.all(deletionPromises);
    handleExistingVersions();
  };
  const handleExistingVersions = async () => {
    const existingVersions = await VersionsApi.getAllVersionDefinitions();
    const deletionPromises: any = [];
    if (existingVersions.items.length) {
      forEach(existingVersions.items, (version) => {
        deletionPromises.push(
          VersionsApi.deleteRepositoryVersion(
            version.VersionDefinition.stack_name,
            version.VersionDefinition.stack_version,
            version.VersionDefinition.id
          )
        );
      });
    }
    await Promise.all(deletionPromises);
    setTimeout(() => {
      startDeploy();
    }, 2000);
  };

  const isAddHostWizard = () => {
    return wizardName === "addHost";
  };

  const isAddServiceWizard = () => {
    return wizardName === "addService";
  };

  if (!!!serviceComponents.items.length) {
    return <Spinner />;
  }
  return (
    <>
      <ExecuteTasksModal
        isOpen={showExecutionModal}
        onClose={() => {
          setShowExecutionModal(false);
        }}
        successCallback={() => {
          if (wizardName === "clusterCreation") {
            installServices();
          } else {
            getKDCSessionState(() => {
              if (isAddHostWizard()) {
                installComponents();
              } else {
                installServices();
              }
            });
          }
          flushStateToDb("next");
        }}
        totalCount={deploymentPromises.current.length}
        successCount={completedOperationsCount}
        failedCount={failedOperationsCount}
      />
      <div className="step-title">Review</div>
      <div className="d-flex flex-column">
        <small className="light-text step-description">
          Please review the configuration before installation.
        </small>
        {!isAddHostWizard() ? (
          <small className="light-text step-description">
            Hosts that are assigned master components are shown with{" "}
            <span className="text-info">✵</span>.
          </small>
        ) : null}
        <Card className="mt-4">
          <Card.Body>
            <div className="bg-lightest p-3">
              <Stack direction="horizontal">
                <b>Cluster Name:</b>
                <div className="ms-2">{getStepData("NAME", "clusterName") || clusterName}</div>
              </Stack>
              <Stack direction="horizontal" className="mt-3">
                <b className="fw-boldest">Total Hosts:</b>
                <div className="ms-2">
                  {getHosts()?.length}
                  {isAddHostWizard() ? ` (${getNewHosts()?.length} new)` : ""}
                </div>
              </Stack>
              <Stack direction="vertical" className="mt-3">
                <b className="fw-boldest">Repositories:</b>
                <div>{renderRepos()}</div>
              </Stack>
              {!isAddHostWizard() ? (
                <Stack direction="vertical" className="mt-3">
                  <b className="fw-boldest">Services:</b>
                  <div className="p-3">{renderServices()}</div>
                </Stack>
              ) : null}
            </div>
          </Card.Body>
        </Card>
      </div>
      <WizardFooter
        isNextEnabled={isNextEnabled}
        lifted
        step={{ ...currentStep, nextLabel: "DEPLOY" }}
        onNext={() => {
          if (!deploymentTriggered) {
            setDeploymentTriggered(false);
            deploymentPromises.current = [];
            if (isAddHostWizard()) {
              startDeployForAddHost();
            } else if (isAddServiceWizard()) {
              startDeploymentForAddService();
            } else {
              prepareDeployment();
            }
            setIsNextEnabled(false);
          }
          // startDeploy();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}

export default Step8;
