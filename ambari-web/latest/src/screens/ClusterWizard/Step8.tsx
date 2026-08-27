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
import { Alert, Button, Card, Spinner as BootstrapSpinner, Stack } from "react-bootstrap";
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
import { minToInstall } from "./utils";
import { isHAComponentOnly } from "../../Utils/numberUtils";
import VersionsApi from "../../api/versionsApi";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import ClusterDeploymentApi from "../../api/clusterDeployment";
import ClusterApi from "../../api/clusterApi";
import { ServiceApi } from "../../api/serviceApi";
import { ActionTypes } from "./clusterStore/types";
import { ContextWrapper } from ".";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import KerberosApi from "../../api/kerberosApi";
import { getConfigTagFromFileName } from "../CommonConfigs/ConfigUtils";
import { AppContext } from "../../store/context";
import { runDeploymentPlan } from "./deploymentQueue";
import ConfigGroupApi from "../../api/configGroupApi";
import { previousAddServiceStep } from "../Services/AddServiceWizard/addServiceNavigation";
import useKerberosMode from "../../hooks/useKerberosMode";
import { saveAs } from "file-saver";
import JSZip from "jszip";
import { buildBlueprintExport } from "./blueprintExport";
import { buildClusterConfigurationPayload } from "./clusterConfigPayload";

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
  const [totalOperationsCount, setTotalOperationsCount] = useState(0);
  const [isNextEnabled, setIsNextEnabled] = useState(true);
  const [deploymentTriggered, setDeploymentTriggered] = useState(false);
  const [deploymentError, setDeploymentError] = useState("");
  const [deploymentStage, setDeploymentStage] = useState("Ready to deploy");
  const [exportError, setExportError] = useState("");
  const [isExportingBlueprint, setIsExportingBlueprint] = useState(false);
  const getStepData = (stepName: string, dataKey: string) => {
    const stepData = get(state, `${wizardName}Steps.${stepName}.data`, {});
    return get(stepData, dataKey, "");
  };
  const isAddHostWizard = () => wizardName === "addHost";
  const isAddServiceWizard = () => wizardName === "addService";

  const {
    clusterName = "",
    cluster: { stack, versionNum },
    isKerberosEnabled,
  } = useContext(AppContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const {
    error: kerberosModeError,
    isLoaded: isKerberosModeLoaded,
    isManualKerberos,
    kdcType,
    reload: reloadKerberosMode,
  } = useKerberosMode();
  const versionStepData = get(state, `${wizardName}Steps.VERSION.data`, {});
  const VERSION = versionNum || get(versionStepData, "selectedVersion.stack_version", "");
  const STACK = stack || get(versionStepData, "selectedStack.stack_name", "");
  const restoredReview = get(state, `${wizardName}Steps.REVIEW.data`, {});
  const addServiceFlow = get(
    state,
    "addServiceSteps.SERVICES.data.addServiceFlow",
    {},
  );
  const reviewDataRef = useRef<Record<string, any>>(restoredReview);
  const completedOperationIds = useRef<Set<string>>(
    new Set(restoredReview.completedOperationIds || []),
  );
  const deploymentArtifacts = useRef<Record<string, any>>({
    repositoryVersionId: restoredReview.repositoryVersionId,
    stackName: restoredReview.stackName,
    stackVersion: restoredReview.stackVersion,
  });

  // Kerberos-related state variables
  const [kerberosDescriptor, setKerberosDescriptor] = useState<any>(null);
  const [isKerberosDescriptorReady, setIsKerberosDescriptorReady] =
    useState(false);
  const [isKerberosPreparationRunning, setIsKerberosPreparationRunning] =
    useState(false);
  const [kerberosPreparationError, setKerberosPreparationError] = useState("");
  const [kerberosCsv, setKerberosCsv] = useState<string | null>(null);
  const [kerberosCsvError, setKerberosCsvError] = useState("");
  const [isKerberosCsvLoading, setIsKerberosCsvLoading] = useState(false);
  const [kerberosPreparationAttempt, setKerberosPreparationAttempt] = useState(0);
  const descriptorExistsRef = useRef(false);

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
        (kerberosDescriptor.services || []).forEach((service: any) => {
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
   */
  const saveKerberosDescriptor = async (descriptor: any): Promise<void> => {
    if (!descriptor) {
      throw new Error("Ambari did not return a Kerberos descriptor.");
    }
    const payload = {
      artifact_data: removeIdentityReferences(cloneDeep(descriptor)),
    };
    const resolvedClusterName = getStepData("NAME", "clusterName") || clusterName;
    if (descriptorExistsRef.current) {
      await KerberosApi.saveAndEditKerberosData(resolvedClusterName, payload);
    } else {
      await KerberosApi.saveKerberosData(resolvedClusterName, payload);
      descriptorExistsRef.current = true;
    }
  };

  const updateKerberosDescriptorMethod = async (): Promise<void> => {
    await saveKerberosDescriptor(kerberosDescriptor);
  };

  /**
   * The UI should ignore Kerberos identity references
   * when setting the user-supplied Kerberos descriptor
   * @param {any} kerberosDescriptor
   * @returns {any}
   */
  const removeIdentityReferences = (kerberosDescriptor: any): any => {
    const notReference = (identity: any) =>
      !identity.reference && !identity.name?.startsWith("/");

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

  const kerberosErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || error?.message || fallback;

  const descriptorConfigs = () => {
    const configProperties = getStepData("CONFIGURATION", "configProperties");
    const secureConfigs: any[] = [];
    Object.values(configProperties || {}).forEach((service: any) => {
      Object.values(service || {}).forEach((configType: any) => {
        Object.values(configType?.properties || {}).forEach((property: any) => {
          if (!property?.isSecureConfig) return;
          secureConfigs.push({
            ...property,
            filename: property.filename || property.fileName || property.type,
            name: property.name || property.propertyName,
          });
        });
      });
    });
    return secureConfigs;
  };

  const loadKerberosCsv = async () => {
    const resolvedClusterName = getStepData("NAME", "clusterName") || clusterName;
    setIsKerberosCsvLoading(true);
    setKerberosCsvError("");
    try {
      const csv = await KerberosApi.downloadKerberosIdentitiesCsv(
        resolvedClusterName,
      );
      setKerberosCsv(String(csv));
      return String(csv);
    } catch (error: any) {
      setKerberosCsvError(String(kerberosErrorMessage(
        error,
        "Ambari could not load the Kerberos principals and keytabs CSV.",
      )));
      throw error;
    } finally {
      setIsKerberosCsvLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    const requiresDescriptor = isAddServiceWizard() && isKerberosEnabled;
    if (!requiresDescriptor) {
      setIsKerberosDescriptorReady(true);
      setIsKerberosPreparationRunning(false);
      setKerberosPreparationError("");
      return () => {
        active = false;
      };
    }
    if (!isKerberosModeLoaded) {
      setIsKerberosPreparationRunning(true);
      return () => {
        active = false;
      };
    }
    if (kerberosModeError || !kdcType) {
      setIsKerberosDescriptorReady(false);
      setIsKerberosPreparationRunning(false);
      setKerberosPreparationError(
        kerberosModeError || "Ambari did not return the Kerberos KDC type.",
      );
      return () => {
        active = false;
      };
    }

    const prepareKerberos = async () => {
      const resolvedClusterName = getStepData("NAME", "clusterName") || clusterName;
      setIsKerberosPreparationRunning(true);
      setIsKerberosDescriptorReady(false);
      setKerberosPreparationError("");
      try {
        const response = await KerberosApi.getKerberosDescriptorProperties(
          "true",
          resolvedClusterName,
        );
        const descriptor = response?.KerberosDescriptor?.kerberos_descriptor;
        if (!descriptor || typeof descriptor !== "object" || !Array.isArray(descriptor.services)) {
          throw new Error("Ambari returned an invalid Kerberos descriptor.");
        }
        const updatedDescriptor = cloneDeep(descriptor);
        updateKerberosDescriptor(updatedDescriptor, descriptorConfigs());

        try {
          await KerberosApi.getKerberosDescriptorArtifact(resolvedClusterName);
          descriptorExistsRef.current = true;
        } catch (error: any) {
          if (error?.response?.status === 404) {
            descriptorExistsRef.current = false;
          } else {
            throw error;
          }
        }

        if (isManualKerberos) {
          await saveKerberosDescriptor(updatedDescriptor);
        }
        if (!active) return;
        setKerberosDescriptor(updatedDescriptor);
        setIsKerberosDescriptorReady(true);
      } catch (error: any) {
        if (!active) return;
        setKerberosPreparationError(String(kerberosErrorMessage(
          error,
          "Ambari could not prepare the Kerberos descriptor.",
        )));
      } finally {
        if (active) setIsKerberosPreparationRunning(false);
      }

      if (!active) return;
      try {
        await loadKerberosCsv();
      } catch {
        // CSV download errors are visible but do not prevent deployment.
      }
    };

    void prepareKerberos();
    return () => {
      active = false;
    };
  }, [
    clusterName,
    isKerberosEnabled,
    isKerberosModeLoaded,
    isManualKerberos,
    kdcType,
    kerberosModeError,
    kerberosPreparationAttempt,
    wizardName,
  ]);

  const retryKerberosPreparation = () => {
    if (kerberosModeError) reloadKerberosMode();
    setKerberosPreparationAttempt((value) => value + 1);
  };

  const downloadKerberosCsv = async () => {
    try {
      const csv = kerberosCsv ?? await loadKerberosCsv();
      saveAs(new Blob([csv], { type: "text/csv" }), "kerberos.csv");
    } catch {
      // loadKerberosCsv renders the recoverable error.
    }
  };

  const downloadBlueprint = async () => {
    setIsExportingBlueprint(true);
    setExportError("");
    try {
      const resolvedClusterName = getStepData("NAME", "clusterName") || clusterName;
      const selectedServices = filter(
        getStepData("SERVICES", "services"),
        (service: any) => service.selected && !service.installed,
      ).map((service: any) => service.serviceName);
      const { blueprint, clusterTemplate } = buildBlueprintExport({
        clusterName: resolvedClusterName,
        configProperties: getStepData("CONFIGURATION", "configProperties"),
        hosts: getHosts(),
        masterAssignments: getStepData("MASTERS", "mastersData") || [],
        selectedServiceNames: selectedServices,
        serviceComponents: serviceComponents.items,
        slaveAssignments: getStepData("SLAVES_AND_CLIENTS", "serviceComponents") || [],
        stackName: STACK,
        stackVersion: VERSION,
      });
      const zip = new JSZip();
      zip.file("blueprint.json", JSON.stringify(blueprint, null, 2));
      zip.file("clustertemplate.json", JSON.stringify(clusterTemplate, null, 2));
      const archive = await zip.generateAsync({ type: "blob" });
      saveAs(archive, `${resolvedClusterName}-blueprint.zip`);
    } catch (error: any) {
      setExportError(String(kerberosErrorMessage(
        error,
        "The Blueprint archive could not be generated.",
      )));
    } finally {
      setIsExportingBlueprint(false);
    }
  };

  function renderRepos() {
    const operatingSystems = getStepData("VERSION", "operatingSystems");
    const selectedStack = getStepData("VERSION", "selectedStack.id");
    const addedOs = operatingSystems[selectedStack].filter(
      (os: any) => os.isAdded
    );
    const allRepos = addedOs.map((currentOs: any) => {
      return currentOs.repos.map((repo: any) => {
        return (
          <Stack
            key={`${currentOs.os}-${repo.id}`}
            direction="vertical"
            className="m-3"
          >
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
  async function getServiceComponents() {
    const servicesAndComponents: ServicesResponse =
      await ChooseServicesApi.getServices(STACK, VERSION);
    setServiceComponents(servicesAndComponents);
  }

  useEffect(() => {
    getServiceComponents();
  }, []);

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
        <Stack
          key={selectedService.service_name}
          direction="vertical"
          className="mt-2"
        >
          <div className="my-2">
            <b>
              <i>{selectedService.service_name}</i>
            </b>
          </div>
          {selectedService.service_components.map((serviceComponent: any) => {
            return (
              <Stack
                key={serviceComponent.componentName}
                direction="horizontal"
                className="mt-2"
              >
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
  async function postVersionDefinition(data: any, headers = {}) {
    const versionInfo = await VersionsApi.postVersionDefinitionFile(data, headers);
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

  function createCluster() {
    const selectedStackVersion = getStepData("VERSION", "selectedStack.id");
    const clusterName = getStepData("NAME", "clusterName");
    return ClusterDeploymentApi.createCluster(clusterName, {
      Clusters: {
        version: selectedStackVersion,
      },
    });
  }

  async function createSelectedServices(versionId?: string) {
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
        if (!isAddServiceWizard() && versionId) {
          serviceInfoObj["desired_repository_version_id"] = versionId;
        }
        return {
          ServiceInfo: serviceInfoObj,
        };
      }
    );
    if (!selectedServicesBody.length) return;
    await ClusterDeploymentApi.createSelectedServices(
      getStepData("NAME", "clusterName") || clusterName,
      selectedServicesBody
    );
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
      if (!matchedServiceComponents?.length) continue;
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
      await ClusterDeploymentApi.addRequestToCreateComponent(
        getStepData("NAME", "clusterName") || clusterName,
        selectedService.serviceName,
        requestBody
      );
    }
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
    await ClusterDeploymentApi.registerHostToCluster(
      getStepData("NAME", "clusterName") || clusterName,
      data
    );
  }

  async function createMasterHostComponents() {
    const masterOnAllHosts: any = [];
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      function (service: any) {
        return !service.installed && service.selected;
      }
    );
    const registrations: Array<{ hostNames: string[]; component: string }> = [];
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
          registrations.push({ hostNames, component });
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
        registrations.push({ hostNames, component });
      }
    });
    for (const registration of registrations) {
      await registerHostsToComponent(
        registration.hostNames,
        registration.component,
      );
    }
  }

  function saveClusterStatus(clusterStatusLocal) {
    reviewDataRef.current = {
      ...reviewDataRef.current,
      clusterStatus: clusterStatusLocal,
    };
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "REVIEW",
        data: reviewDataRef.current,
      },
    });
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
    if (servicesInit.Requests?.id != null) {
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
      await Promise.resolve(flushStateToDb(
        "next",
        -1,
        `${isAddServiceWizard() ? "ADD_SERVICES" : "CLUSTER"}_INSTALLING_3`,
      ));
      handleNextImperitive();
    } else {
      throw new Error("Ambari did not return an installation request ID.");
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
    if (!requestPayload.length) return;
    await ClusterDeploymentApi.registerHostToCluster(
      getStepData("NAME", "clusterName") || clusterName,
      requestPayload
    );
  }

  async function createConfigurationGroups() {
    const configurationData = getStepData("CONFIGURATION", "configGroupData");
    const storedGroups = get(configurationData, "items", configurationData);
    const configGroups = Array.isArray(storedGroups) ? storedGroups : [];
    for (const group of configGroups) {
      const value = group.ConfigGroup || group;
      if (value.is_default || (!value.is_for_update && !value.is_temporary)) {
        continue;
      }
      const payload = group.ConfigGroup ? group : { ConfigGroup: value };
      if (value.is_for_update && value.id != null) {
        await ConfigGroupApi.updateConfigGroup(
          getStepData("NAME", "clusterName") || clusterName,
          String(value.id),
          payload,
        );
      } else {
        await ConfigGroupApi.addConfigGroup(
          getStepData("NAME", "clusterName") || clusterName,
          [payload],
        );
      }
    }
  }

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
    const registrations: Array<{ hostNames: string[]; component: string }> = [];
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
          registrations.push({
            hostNames: map(registeredHosts, "name"),
            component: requiredComponent.component_name,
          });
        }
      });
    }
    //add Mysql server if HIVE is selected
    // const isHiveSelected=!!getStepData("SERVICES","services")?.["HIVE"]?.selected
    // if(isHiveSelected){
    //   const hiveDb=
    // }
    for (const registration of registrations) {
      await registerHostsToComponent(
        registration.hostNames,
        registration.component,
      );
    }
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
    const registrations: Array<{ hostNames: string[]; component: string }> = [];
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
              registrations.push({
                hostNames,
                component: _slave.componentName,
              });
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
              registrations.push({
                hostNames,
                component: _slave.componentName,
              });
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
          registrations.push({
            hostNames,
            component: _slave.componentName,
          });
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
              registrations.push({
                hostNames,
                component: _client.component_name,
              });
            }
          } else {
            hostNames = uniq(hostNames);
            registrations.push({
              hostNames,
              component: _client.component_name,
            });
          }
        });
      }
    });
    for (const registration of registrations) {
      await registerHostsToComponent(
        registration.hostNames,
        registration.component,
      );
    }
  }

  async function applyConfigurationsToCluster() {
    const configurations = getStepData("CONFIGURATION", "configProperties");
    const applyConfigurationsPayload = buildClusterConfigurationPayload({
      configProperties: configurations,
      includeInstalledChanges: isAddServiceWizard(),
      installedServices,
    });
    if (!applyConfigurationsPayload.length) return;
    await ClusterDeploymentApi.applyClusterConfigs(
      getStepData("NAME", "clusterName")||clusterName,
      applyConfigurationsPayload
    );
  }

  const deploymentStatePrefix = isAddServiceWizard()
    ? "ADD_SERVICES"
    : "CLUSTER";

  const saveReviewData = async (
    data: Record<string, any>,
    clusterState = `${deploymentStatePrefix}_DEPLOY_PREP_2`,
  ) => {
    reviewDataRef.current = { ...reviewDataRef.current, ...data };
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "REVIEW",
        data: reviewDataRef.current,
      },
    });
    await Promise.resolve(flushStateToDb("checkpoint", -1, clusterState));
  };

  const deleteExistingClusters = async () => {
    const response = await ClusterApi.getAllClusters();
    await Promise.all(get(response, "items", []).map((item: any) =>
      ClusterApi.deleteCluster(item.Clusters.cluster_name),
    ));
  };

  const deleteExistingVersions = async () => {
    const response = await VersionsApi.getAllVersionDefinitions();
    await Promise.all(get(response, "items", []).map((item: any) =>
      VersionsApi.deleteRepositoryVersion(
        item.VersionDefinition.stack_name,
        item.VersionDefinition.stack_version,
        item.VersionDefinition.id,
      ),
    ));
  };

  const createRepositoryVersion = async () => {
    const selectedStack = getStepData("VERSION", "selectedStack");
    const source = getStepData("VERSION", "versionDefinitionSource");
    const payload = source?.payload || {
      VersionDefinition: { available: selectedStack.id },
    };
    const response = await postVersionDefinition(payload, source?.headers);
    const versionDefinition = get(
      response,
      "resources.0.VersionDefinition",
      {},
    );
    if (
      !versionDefinition.id
      || !versionDefinition.stack_name
      || !versionDefinition.stack_version
    ) {
      throw new Error("Ambari did not return the created repository version.");
    }
    deploymentArtifacts.current.repositoryVersionId = versionDefinition.id;
    deploymentArtifacts.current.stackName = versionDefinition.stack_name;
    deploymentArtifacts.current.stackVersion = versionDefinition.stack_version;
  };

  const updateRepositoryOperatingSystems = async () => {
    const repositoryVersionId = deploymentArtifacts.current.repositoryVersionId;
    if (!repositoryVersionId) {
      throw new Error("The repository version was not created.");
    }
    await VersionsApi.updateRepoOSInfo(
      deploymentArtifacts.current.stackName || STACK,
      deploymentArtifacts.current.stackVersion || VERSION,
      repositoryVersionId,
      await getUpdateRepoOSInfoBody(),
    );
  };

  const buildDeploymentStages = () => {
    const commonOperations = [
      { id: "create-services", label: "Creating services", run: () =>
        createSelectedServices(deploymentArtifacts.current.repositoryVersionId) },
      { id: "apply-configurations", label: "Applying configurations", run: applyConfigurationsToCluster },
      { id: "create-components", label: "Creating service components", run: createComponents },
      { id: "create-configuration-groups", label: "Saving configuration groups", run: createConfigurationGroups },
      { id: "register-masters", label: "Assigning master components", run: createMasterHostComponents },
      { id: "register-slaves-clients", label: "Assigning slave and client components", run: createSlaveAndClientsHostComponents },
      { id: "register-required-components", label: "Assigning required components", run: createAdditionalHostComponents },
    ];

    if (isAddServiceWizard()) {
      const kerberosOperations = isKerberosEnabled && !isManualKerberos
        ? [{
            id: "update-kerberos-descriptor",
            label: "Updating the Kerberos descriptor",
            run: updateKerberosDescriptorMethod,
          }]
        : [];
      return [...commonOperations.slice(0, 1), ...kerberosOperations, ...commonOperations.slice(1)]
        .map((operation) => ({ operations: [operation] }));
    }

    return [
      { operations: [{ id: "delete-clusters", label: "Removing incomplete clusters", run: deleteExistingClusters }] },
      { operations: [{ id: "delete-repository-versions", label: "Removing incomplete repository versions", run: deleteExistingVersions }] },
      { operations: [{ id: "create-repository-version", label: "Creating the repository version", run: createRepositoryVersion }] },
      { operations: [{ id: "update-repositories", label: "Saving repositories", run: updateRepositoryOperatingSystems }] },
      { operations: [{ id: "create-cluster", label: "Creating the cluster", run: createCluster }] },
      ...commonOperations.slice(0, 3).map((operation) => ({ operations: [operation] })),
      { operations: [{ id: "register-hosts", label: "Adding hosts to the cluster", run: registerHostsToCluster }] },
      ...commonOperations.slice(3).map((operation) => ({ operations: [operation] })),
    ];
  };

  const waitForKdcSession = () => new Promise<void>((resolve, reject) => {
    void getKDCSessionState(resolve, reject).catch(reject);
  });

  const deploy = async () => {
    if (deploymentTriggered) return;
    if (isAddServiceWizard() && isKerberosEnabled && !isKerberosDescriptorReady) {
      setKerberosPreparationError(
        "The Kerberos descriptor must be prepared before deployment.",
      );
      return;
    }
    setDeploymentTriggered(true);
    setIsNextEnabled(false);
    setDeploymentError("");

    try {
      await saveReviewData({
        completedOperationIds: [...completedOperationIds.current],
        deploymentStage: "Preparing deployment",
      });
      const stages = buildDeploymentStages();
      const operations = stages.flatMap((stage) => stage.operations);
      setTotalOperationsCount(operations.length);
      setCompletedOperationsCount(
        operations.filter((operation) => completedOperationIds.current.has(operation.id)).length,
      );

      await runDeploymentPlan(
        stages,
        completedOperationIds.current,
        async ({ completed, operation }) => {
          setCompletedOperationsCount(completed);
          setDeploymentStage(operation.label);
          await saveReviewData({
            completedOperationIds: [...completedOperationIds.current],
            deploymentStage: operation.label,
            ...deploymentArtifacts.current,
          });
        },
      );

      if (isAddServiceWizard()) {
        await waitForKdcSession();
      }
      setDeploymentStage("Starting component installation");
      await installServices();
    } catch (error: any) {
      const message = error?.response?.data?.message
        || error?.message
        || "Ambari could not prepare the cluster deployment.";
      setDeploymentError(String(message));
      setDeploymentStage("Deployment preparation failed");
      setDeploymentTriggered(false);
      setIsNextEnabled(true);
      try {
        await saveReviewData({
          completedOperationIds: [...completedOperationIds.current],
          deploymentStage: "Deployment preparation failed",
          deploymentError: String(message),
          ...deploymentArtifacts.current,
        });
      } catch {
        // The visible deployment error remains retryable if checkpoint persistence is unavailable.
      }
    }
  };

  if (!!!serviceComponents.items.length) {
    return <Spinner />;
  }
  return (
    <>
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
        {isAddServiceWizard() && isKerberosEnabled && isKerberosPreparationRunning ? (
          <Alert variant="info" className="mt-3 d-flex align-items-center gap-2">
            <BootstrapSpinner animation="border" size="sm" />
            Preparing the Kerberos descriptor
          </Alert>
        ) : null}
        {kerberosPreparationError ? (
          <Alert variant="danger" className="mt-3">
            <div>{kerberosPreparationError}</div>
            <Button
              className="mt-2"
              size="sm"
              variant="outline-danger"
              disabled={isKerberosPreparationRunning}
              onClick={retryKerberosPreparation}
            >
              Retry Kerberos Preparation
            </Button>
          </Alert>
        ) : null}
        {isAddServiceWizard() && isKerberosEnabled && isKerberosDescriptorReady ? (
          <Alert variant={isManualKerberos ? "warning" : "secondary"} className="mt-3">
            {isManualKerberos ? (
              <div>
                Because Kerberos was manually installed on the cluster, you must
                create and distribute principals and keytabs after this operation.
              </div>
            ) : (
              <div>Kerberos KDC type: {kdcType}</div>
            )}
            <Button
              className="mt-2"
              size="sm"
              variant="outline-primary"
              disabled={isKerberosCsvLoading || deploymentTriggered}
              onClick={() => void downloadKerberosCsv()}
            >
              {isKerberosCsvLoading ? "Loading CSV" : "Download Kerberos CSV"}
            </Button>
          </Alert>
        ) : null}
        {kerberosCsvError ? (
          <Alert variant="warning" className="mt-3">
            <div>{kerberosCsvError}</div>
            <Button
              className="mt-2"
              size="sm"
              variant="outline-warning"
              disabled={isKerberosCsvLoading}
              onClick={() => void loadKerberosCsv().catch(() => undefined)}
            >
              Retry CSV
            </Button>
          </Alert>
        ) : null}
        {exportError ? (
          <Alert variant="danger" className="mt-3">
            {exportError}
          </Alert>
        ) : null}
        {deploymentError ? (
          <Alert variant="danger" className="mt-3">
            {deploymentError}
          </Alert>
        ) : null}
        {deploymentTriggered ? (
          <Alert variant="info" className="mt-3 d-flex align-items-center gap-2">
            <BootstrapSpinner animation="border" size="sm" />
            {deploymentStage}
            {totalOperationsCount
              ? ` (${completedOperationsCount}/${totalOperationsCount})`
              : ""}
          </Alert>
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
        isNextEnabled={
          isNextEnabled
          && (!isAddServiceWizard()
            || !isKerberosEnabled
            || isKerberosDescriptorReady)
        }
        lifted
        step={{ ...currentStep, nextLabel: "DEPLOY" }}
        isCancelEnabled={!deploymentTriggered}
        isBackEnabled={!deploymentTriggered}
        onNext={() => void deploy()}
        onCancel={() => flushStateToDb("cancel")}
        onBack={async () => {
          if (isAddServiceWizard()) {
            const previousStep = previousAddServiceStep(5, addServiceFlow);
            await Promise.resolve(flushStateToDb("jump", previousStep));
            jumpToStep(previousStep);
          } else {
            await Promise.resolve(flushStateToDb("back"));
            handleBackImperitive();
          }
        }}
        sideItems={(
          <>
            <Button
              className="me-2"
              size="sm"
              variant="outline-secondary"
              disabled={deploymentTriggered}
              onClick={() => window.print()}
            >
              Print Review
            </Button>
            <Button
              className="me-2"
              size="sm"
              variant="outline-primary"
              disabled={deploymentTriggered || isExportingBlueprint}
              onClick={() => void downloadBlueprint()}
            >
              {isExportingBlueprint ? "Generating Blueprint" : "Generate Blueprint"}
            </Button>
          </>
        )}
      />
    </>
  );
}

export default Step8;
