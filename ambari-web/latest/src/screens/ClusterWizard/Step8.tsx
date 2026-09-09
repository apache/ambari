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
import { HostsApi } from "../../api/hostsApi";
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
import {
  buildAtomicVersionDefinitionPayload,
  buildInitialOperatingSystems,
  resolveRepositoryVersion,
} from "./repositoryVersionResolution";
import { clusterCreationReentrySteps } from "../../Utils/scopedWorkflow";
import WorkflowStateApi from "../../api/workflowStateApi";
import { useTranslation } from "react-i18next";
import ServiceDependenciesApi, {
  type ManagedDependencyBinding,
  type ManagedDependencyPlanPreviewResponse,
  type ManagedDependencyPreview,
  type ManagedDependencyType,
} from "../../api/serviceDependenciesApi";
import ManagedDependencySettings from "./ManagedDependencySettings";
import {
  buildManagedDependencyClientConfig,
  reviewedManagedDependencies,
} from "./managedDependencyConfig";
import {
  appendManagedDependencyAttempt,
  assertManagedDependencyRecord,
  buildManagedDependencyPlanSelections,
  buildManagedDependencyCreateRequest,
  isBindingIdUnavailable,
  isMissingManagedDependency,
  managedDependencyAttemptMatchesPreview,
  managedDependencyAttempts,
  ManagedDependencyAttemptLimitError,
  ManagedDependencyReviewRequiredError,
  managedDependencyReviewSignature,
  recordManagedDependencyResponse,
  type ManagedDependencyMaterializationAttempt,
  type ManagedDependencyMaterializationRecord,
  type ManagedDependencyMaterializations,
} from "./managedDependencyMaterialization";
import { DependencyCard } from "../Services/ServiceDependencies";
import {
  clearDeploymentSignatureScope,
  createDeploymentSignatureScope,
  DeploymentTopologyRecoveryRequiredError,
  deploymentInputSignature,
  reconcileDeploymentInputSignatures,
} from "./deploymentInputRecovery";
import { createSecureUuid } from "../../Utils/uuid";
import type {
  ManagedDependencyInstallIntent,
  ManagedDependencyInstallTarget,
} from "./installationProgress";

type Step8Props = {
  wizardName?: string;
};

type HostComponentAssignmentAttempt = {
  component: string;
  hostNames: string[];
  request: Record<string, any>;
  targetClusterName: string;
  topologyInput: "masters" | "slavesAndClients";
};

const normalizeHostComponentAssignmentAttempts = (
  value: Record<string, HostComponentAssignmentAttempt | HostComponentAssignmentAttempt[]> = {},
) => Object.fromEntries(Object.entries(value).map(([key, attempts]) => [
  key,
  (Array.isArray(attempts) ? attempts : [attempts]).filter(Boolean),
]));

function Step8({ wizardName = "clusterCreation" }: Step8Props) {
  const { t } = useTranslation();
  const { Context } = useContext(ContextWrapper);
  const { state, dispatch, installedServices = [] }: any = useContext(Context);
  const {
    flushStateToDb,
    getDraftRevision,
    getWorkflowRevision,
    storeStepDataAndFlush,
    draftId,
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
  const [topologyRecoveryInputs, setTopologyRecoveryInputs] = useState<string[]>([]);
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
    cluster = {},
    isKerberosEnabled,
  } = useContext(AppContext);
  const { stack, versionNum } = cluster;
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
  const managedDependencies = get(
    state,
    `${wizardName}Steps.SERVICES.data.managedDependencies`,
    {},
  );
  const managedDependencyClientConfig = buildManagedDependencyClientConfig(
    managedDependencies,
  );
  const reviewDataRef = useRef<Record<string, any>>(restoredReview);
  const completedOperationIds = useRef<Set<string>>(
    new Set(restoredReview.completedOperationIds || []),
  );
  const deploymentArtifacts = useRef<Record<string, any>>({
    clusterCreationAttempted: restoredReview.clusterCreationAttempted,
    clusterId: restoredReview.clusterId,
    repositoryVersionId: restoredReview.repositoryVersionId,
    repositoryVersionReused: restoredReview.repositoryVersionReused,
    stackName: restoredReview.stackName,
    stackVersion: restoredReview.stackVersion,
  });
  const managedDependencyMaterializations = useRef<ManagedDependencyMaterializations>(
    restoredReview.managedDependencyMaterializations || {},
  );
  const hostComponentAssignmentAttempts = useRef<Record<
    string,
    HostComponentAssignmentAttempt[]
  >>(normalizeHostComponentAssignmentAttempts(
    restoredReview.hostComponentAssignmentAttempts,
  ));
  const [managedDependencyBindings, setManagedDependencyBindings] = useState<
    Partial<Record<ManagedDependencyType, ManagedDependencyBinding>>
  >(() => Object.fromEntries(
    Object.entries(restoredReview.managedDependencyMaterializations || {})
      .flatMap(([type, record]: [string, any]) =>
        record?.response ? [[type, record.response]] : []),
  ));
  const [managedDependencyReviewPending, setManagedDependencyReviewPending] =
    useState(Boolean(restoredReview.managedDependencyPendingReviewSignature));
  const authoritativeClusterId = useRef<number | null>(null);
  const deploymentGenerationRef = useRef(0);
  const deploymentSignatureScopeRef = useRef(createDeploymentSignatureScope());
  const currentHostAssignmentsRef = useRef<Map<string, Set<string>> | null>(null);
  const managedDependencyPlanPreviewsRef = useRef<Partial<
    Record<ManagedDependencyType, ManagedDependencyPreview>
  >>({});

  useEffect(() => {
    const signatureScope = createDeploymentSignatureScope();
    deploymentSignatureScopeRef.current = signatureScope;
    deploymentGenerationRef.current += 1;
    return () => {
      clearDeploymentSignatureScope(signatureScope);
      deploymentGenerationRef.current += 1;
    };
  }, [cluster?.cluster_id, clusterName, draftId, wizardName]);

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
    // Use selectedVersion.id to match the key stored by Step1 (e.g. "3.4.1.0-13"), not selectedStack.id (e.g. "VDP-3.4")
    const selectedVersionId = getStepData("VERSION", "selectedVersion.id");
    const addedOs = operatingSystems[selectedVersionId].filter(
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

  function getInitialOperatingSystems() {
    const usesRedhat = getStepData("VERSION", "redhatSatellite");
    // Use selectedVersion.id to match the key stored by Step1 (e.g. "3.4.1.0-13"), not selectedStack.id (e.g. "VDP-3.4")
    const selectedVersionId = getStepData("VERSION", "selectedVersion.id");
    const operatingSystemsFromState = getStepData(
      "VERSION",
      `operatingSystems`
    );
    const operatingSystems = operatingSystemsFromState[selectedVersionId];
    return buildInitialOperatingSystems(operatingSystems || [], !usesRedhat);
  }

  async function createCluster() {
    const selectedStackVersion = getStepData("VERSION", "selectedStack.id");
    const targetClusterName = getStepData("NAME", "clusterName");
    if (!draftId) {
      throw new Error(t("installer.step8.draftMissing"));
    }

    if (authoritativeClusterId.current != null) return;
    if (deploymentArtifacts.current.clusterId != null) {
      throw new Error(
        t("installer.step8.clusterNoLongerAvailable", { cluster: targetClusterName }),
      );
    }

    deploymentArtifacts.current.clusterCreationAttempted = true;
    await saveReviewData({
      ...deploymentArtifacts.current,
      clusterCreationAttempted: true,
    });
    let created: any;
    try {
      created = await ClusterDeploymentApi.createCluster(targetClusterName, {
        Clusters: {
          creation_draft_id: draftId,
          version: selectedStackVersion,
        },
      });
    } catch (error) {
      if (await reconcileCreatedClusterDraft()) return;
      throw error;
    }
    const clusterId = get(created, "Clusters.cluster_id")
      || get(created, "resources.0.Clusters.cluster_id");
    if (clusterId == null) {
      if (await reconcileCreatedClusterDraft()) return;
      throw new Error(t("installer.step8.clusterResultMissing"));
    }
    authoritativeClusterId.current = Number(clusterId);
    deploymentArtifacts.current.clusterId = clusterId;
    await saveReviewData({ ...deploymentArtifacts.current });
  }

  async function createSelectedServices(versionId?: string, generation?: number) {
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
    const { targetClusterId, targetClusterName } = targetClusterIdentity();
    const serviceNames = selectedServicesBody.map(({ ServiceInfo }) =>
      ServiceInfo.service_name).sort();
    let intent = reviewDataRef.current.serviceCreationIntent;
    const existing = await ServiceApi.getAllServices(targetClusterName);
    if (generation != null) assertCurrentDeployment(generation);
    const existingByName = new Map(
      get(existing, "items", []).map((item: any) => [
        get(item, "ServiceInfo.service_name"),
        get(item, "ServiceInfo.state"),
      ]),
    );
    if (!intent) {
      const collision = serviceNames.find((serviceName) => existingByName.has(serviceName));
      if (collision) {
        throw new Error(t("installer.step8.serviceAlreadyExists", { service: collision }));
      }
      intent = {
        clusterId: targetClusterId,
        clusterName: targetClusterName,
        request: selectedServicesBody,
        serviceNames,
      };
      await saveReviewData({ serviceCreationIntent: intent });
      if (generation != null) assertCurrentDeployment(generation);
    } else if (intent.clusterId !== targetClusterId
      || intent.clusterName !== targetClusterName
      || JSON.stringify(intent.serviceNames) !== JSON.stringify(serviceNames)
      || JSON.stringify(intent.request) !== JSON.stringify(selectedServicesBody)) {
      throw new Error(t("installer.step8.serviceSelectionChangedAfterAttempt"));
    }
    const unsafeExisting = serviceNames.find((serviceName) =>
      existingByName.has(serviceName) && existingByName.get(serviceName) !== "INIT");
    if (unsafeExisting) {
      throw new Error(t("installer.step8.serviceStateChanged", { service: unsafeExisting }));
    }
    const missingServicesBody = selectedServicesBody.filter(({ ServiceInfo }) =>
      !existingByName.has(ServiceInfo.service_name));
    if (!missingServicesBody.length) return;
    await ClusterDeploymentApi.createSelectedServices(
      targetClusterName,
      missingServicesBody
    );
    if (generation != null) assertCurrentDeployment(generation);
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

  async function createComponents(generation: number) {
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      function (service: any) {
        return service.selected && !service.installed;
      }
    );
    if (!selectedServices.length) return;
    const targetClusterName = getStepData("NAME", "clusterName") || clusterName;
    const existingServices = await ServiceApi.getAllServices(targetClusterName);
    assertCurrentDeployment(generation);
    for (const selectedService of selectedServices) {
      const matchedServiceComponents = getServiceComponentsForService(
        selectedService.serviceName
      );
      if (!matchedServiceComponents?.length) continue;
      const existingService = get(existingServices, "items", []).find((item: any) =>
        get(item, "ServiceInfo.service_name") === selectedService.serviceName);
      const existingComponentNames = new Set(
        get(existingService, "components", []).map((component: any) =>
          get(component, "ServiceComponentInfo.component_name")),
      );
      const missingComponents = matchedServiceComponents.filter((serviceComponent: any) =>
        !existingComponentNames.has(
          get(serviceComponent, "StackServiceComponents.component_name"),
        ));
      if (!missingComponents.length) continue;
      const requestBody = {
        components: missingComponents.map((serviceComponent: any) => {
          return {
            ServiceComponentInfo: {
              component_name:
                serviceComponent.StackServiceComponents.component_name,
            },
          };
        }),
      };
      assertCurrentDeployment(generation);
      await ClusterDeploymentApi.addRequestToCreateComponent(
        targetClusterName,
        selectedService.serviceName,
        requestBody
      );
      assertCurrentDeployment(generation);
    }
  }

  async function registerHostsToComponent(
    hostNames: string[],
    component: string,
    topologyInput: "masters" | "slavesAndClients",
    generation: number,
  ) {
    if (!hostNames.length) return;
    const targetClusterName = getStepData("NAME", "clusterName") || clusterName;

    if (!currentHostAssignmentsRef.current) {
      const existing = await HostsApi.getHostComponentsDetails(
        targetClusterName,
        "fields=Hosts/host_name,host_components/HostRoles/component_name,host_components/HostRoles/host_name,host_components/HostRoles/state",
      );
      assertCurrentDeployment(generation);
      const assignments = new Map<string, Set<string>>();
      get(existing, "items", []).forEach((host: any) => {
        const fallbackHostName = get(host, "Hosts.host_name");
        get(host, "host_components", []).forEach((hostComponent: any) => {
          const currentComponent = get(hostComponent, "HostRoles.component_name");
          const currentHostName = get(hostComponent, "HostRoles.host_name", fallbackHostName);
          if (!currentComponent || !currentHostName) return;
          if (!assignments.has(currentComponent)) assignments.set(currentComponent, new Set());
          assignments.get(currentComponent)?.add(currentHostName);
        });
      });
      currentHostAssignmentsRef.current = assignments;
    }
    const desiredHostNames = uniq(hostNames).sort();
    const existingHostNames = currentHostAssignmentsRef.current.get(component) || new Set();
    const unexpectedHostNames = [...existingHostNames].filter(
      (hostName) => !desiredHostNames.includes(hostName),
    );
    if (topologyInput === "masters" && unexpectedHostNames.length) {
      throw new DeploymentTopologyRecoveryRequiredError([topologyInput]);
    }
    const missingHostNames = desiredHostNames.filter(
      (hostName) => !existingHostNames.has(hostName),
    );
    if (!missingHostNames.length) return;

    let queryStr = "";
    missingHostNames.forEach(function (hostName) {
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
    const attemptKey = `${topologyInput}:${component}`;
    const attempt = {
      component,
      hostNames: desiredHostNames,
      request: data,
      targetClusterName,
      topologyInput,
    } as const;
    const savedAttempts = hostComponentAssignmentAttempts.current[attemptKey] || [];
    const savedTargetChanged = savedAttempts.some((savedAttempt) =>
      savedAttempt.component !== component
      || savedAttempt.targetClusterName !== targetClusterName
      || savedAttempt.topologyInput !== topologyInput
      || JSON.stringify(savedAttempt.hostNames) !== JSON.stringify(desiredHostNames));
    if (savedTargetChanged) {
      throw new DeploymentTopologyRecoveryRequiredError([topologyInput]);
    }
    const hasExactAttempt = savedAttempts.some((savedAttempt) =>
      JSON.stringify(savedAttempt.request) === JSON.stringify(data));
    if (!hasExactAttempt) {
      const nextAttempts = {
        ...hostComponentAssignmentAttempts.current,
        [attemptKey]: [...savedAttempts, attempt],
      };
      await saveReviewData({
        hostComponentAssignmentAttempts: nextAttempts,
      });
      assertCurrentDeployment(generation);
      hostComponentAssignmentAttempts.current = nextAttempts;
    }
    assertCurrentDeployment(generation);
    await ClusterDeploymentApi.registerHostToCluster(
      targetClusterName,
      data
    );
    assertCurrentDeployment(generation);
    const current = currentHostAssignmentsRef.current.get(component) || new Set<string>();
    missingHostNames.forEach((hostName) => current.add(hostName));
    currentHostAssignmentsRef.current.set(component, current);
  }

  async function createMasterHostComponents(generation: number) {
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
        "masters",
        generation,
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

  async function createAdditionalHostComponents(generation: number) {
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
        "slavesAndClients",
        generation,
      );
    }
  }

  async function createSlaveAndClientsHostComponents(generation: number) {
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
        const newHostObj = {
          group: "Default",
          isInstalled: Boolean(currentComponent.isInstalled),
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
        "slavesAndClients",
        generation,
      );
    }
  }

  async function applyConfigurationsToCluster() {
    const configurations = getStepData("CONFIGURATION", "configProperties");
    const applyConfigurationsPayload = buildClusterConfigurationPayload({
      configProperties: configurations,
      includeInstalledChanges: isAddServiceWizard(),
      installedServices,
      requiredConfigurations: managedDependencyClientConfig,
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

  const assertCurrentDeployment = (generation: number) => {
    if (generation !== deploymentGenerationRef.current) {
      throw new Error(t("installer.step8.targetChanged"));
    }
  };

  const targetClusterIdentity = () => {
    const targetClusterName = getStepData("NAME", "clusterName") || clusterName;
    const targetClusterId = isAddServiceWizard()
      ? Number(cluster?.cluster_id)
      : Number(authoritativeClusterId.current || deploymentArtifacts.current.clusterId);
    if (!targetClusterName || !Number.isInteger(targetClusterId) || targetClusterId <= 0) {
      throw new Error(t("installer.step8.clusterIdentityMissing"));
    }
    return { targetClusterId, targetClusterName };
  };

  const managedSelectionSignature = (selections: Record<string, any>) =>
    JSON.stringify(reviewedManagedDependencies(selections).map(({ dependencyType, choice }) => [
      dependencyType,
      choice.preview ? managedDependencyReviewSignature(choice.preview) : null,
    ]));

  const currentDeploymentInputSignatures = async () => {
    const selectedServices = filter(
      getStepData("SERVICES", "services"),
      (service: any) => service.selected && !service.installed,
    ).map((service: any) => ({
      installed: Boolean(service.installed),
      serviceName: service.serviceName,
    })).sort((left: any, right: any) =>
      String(left.serviceName).localeCompare(String(right.serviceName)));
    const configuration = buildClusterConfigurationPayload({
      configProperties: getStepData("CONFIGURATION", "configProperties") || {},
      includeInstalledChanges: isAddServiceWizard(),
      installedServices,
      requiredConfigurations: managedDependencyClientConfig,
    });
    const inputs = {
      configuration,
      configGroups: getStepData("CONFIGURATION", "configGroupData") || [],
      hosts: {
        installed: getStepData("HOSTS", "installedHosts") || [],
        registered: getStepData("HOST_STATUS", "hosts") || [],
      },
      managedDependencies: managedSelectionSignature(managedDependencies),
      masters: getStepData("MASTERS", "mastersData") || [],
      serviceComponents: serviceComponents.items,
      services: selectedServices,
      slavesAndClients: getStepData("SLAVES_AND_CLIENTS", "serviceComponents") || [],
    };
    return Object.fromEntries(await Promise.all(
      Object.entries(inputs).map(async ([key, value]) => [
        key,
        await deploymentInputSignature(
          value,
          globalThis.crypto,
          deploymentSignatureScopeRef.current,
        ),
      ] as const),
    ));
  };

  const currentDeploymentTopologyIntent = () => ({
    masters: cloneDeep(getStepData("MASTERS", "mastersData") || []),
    slavesAndClients: cloneDeep(
      getStepData("SLAVES_AND_CLIENTS", "serviceComponents") || [],
    ),
  });

  const currentManagedInstallIntent = (): ManagedDependencyInstallIntent => {
    const { targetClusterId, targetClusterName } = targetClusterIdentity();
    const selectedServiceNames = Object.values(
      getStepData("SERVICES", "services") || {},
    )
      .filter((service: any) => service.selected && !service.installed)
      .map((service: any) => String(service.serviceName))
      .sort();
    const selectedServices = new Set(selectedServiceNames);
    const componentServiceNames = new Map<string, string>();
    serviceComponents.items.forEach((service: any) => {
      const serviceName = get(service, "StackServices.service_name");
      get(service, "components", []).forEach((component: any) => {
        const componentName = get(component, "StackServiceComponents.component_name");
        if (serviceName && componentName) componentServiceNames.set(componentName, serviceName);
      });
    });
    const targets: ManagedDependencyInstallTarget[] = [];
    const masters = getStepData("MASTERS", "mastersData") || [];
    masters.forEach((host: any) => {
      (host.masterServices || []).forEach((component: any) => {
        const serviceName = String(component.serviceId || componentServiceNames.get(component.component) || "");
        if (component.isInstalled || !serviceName || !selectedServices.has(serviceName)) return;
        targets.push({
          componentName: String(component.component),
          hostName: String(component.hostName || host.host_name),
          serviceName,
        });
      });
    });
    const slavesAndClients = getStepData("SLAVES_AND_CLIENTS", "serviceComponents") || [];
    slavesAndClients.forEach((host: any) => {
      (host.checkboxes || []).filter((component: any) =>
        component.checked && component.isInstalled !== true,
      ).forEach((component: any) => {
        const serviceName = String(component.serviceName || componentServiceNames.get(component.label) || "");
        if (!serviceName || !selectedServices.has(serviceName)) return;
        targets.push({
          componentName: String(component.label),
          hostName: String(host.hostname),
          serviceName,
        });
      });
    });
    const uniqueTargets = Array.from(new Map(
      targets.map((target) => [
        `${target.serviceName}:${target.componentName}:${target.hostName}`,
        target,
      ]),
    ).values()).sort((left, right) =>
      `${left.serviceName}:${left.componentName}:${left.hostName}`
        .localeCompare(`${right.serviceName}:${right.componentName}:${right.hostName}`));
    const existingIntent = reviewDataRef.current.managedDependencyInstallIntent;
    return {
      clusterId: targetClusterId,
      clusterName: targetClusterName,
      wizardName: wizardName as ManagedDependencyInstallIntent["wizardName"],
      serviceNames: selectedServiceNames,
      targets: uniqueTargets,
      intentId: existingIntent?.intentId || createSecureUuid(),
      state: "READY",
      ...(existingIntent?.requestId != null ? { requestId: existingIntent.requestId } : {}),
    };
  };

  const reconcileDeploymentInputs = async (generation: number) => {
    const current = await currentDeploymentInputSignatures();
    assertCurrentDeployment(generation);
    const reconciled = reconcileDeploymentInputSignatures({
      attemptedTopologyInputs: Object.values(hostComponentAssignmentAttempts.current)
        .flatMap((attempts) => attempts.map((attempt) => attempt.topologyInput)),
      completedOperationIds: completedOperationIds.current,
      current,
      previous: reviewDataRef.current.deploymentInputSignatures,
    });
    if (reconciled.topologyChanges.length) {
      throw new DeploymentTopologyRecoveryRequiredError(reconciled.topologyChanges);
    }
    const currentTopology = currentDeploymentTopologyIntent();
    const savedTopology = reviewDataRef.current.deploymentTopologyIntent || {};
    const nextTopology = {
      masters: !reviewDataRef.current.deploymentInputSignatures
        || reconciled.changedInputs.includes("masters")
        ? currentTopology.masters
        : savedTopology.masters,
      slavesAndClients: !reviewDataRef.current.deploymentInputSignatures
        || reconciled.changedInputs.includes("slavesAndClients")
        ? currentTopology.slavesAndClients
        : savedTopology.slavesAndClients,
    };
    if (!reconciled.changed
      && savedTopology.masters
      && savedTopology.slavesAndClients) return;
    completedOperationIds.current = new Set(reconciled.completedOperationIds);
    await saveReviewData({
      completedOperationIds: reconciled.completedOperationIds,
      deploymentInputSignatures: current,
      deploymentTopologyIntent: nextTopology,
    });
    assertCurrentDeployment(generation);
  };

  const restoreSavedTopologyIntent = async () => {
    const savedTopology = reviewDataRef.current.deploymentTopologyIntent;
    if (!savedTopology) return;
    if (topologyRecoveryInputs.includes("masters") && savedTopology.masters) {
      const mastersData = get(state, `${wizardName}Steps.MASTERS.data`, {});
      await storeStepDataAndFlush("MASTERS", {
        ...mastersData,
        mastersData: cloneDeep(savedTopology.masters),
      });
    }
    if (topologyRecoveryInputs.includes("slavesAndClients")
      && savedTopology.slavesAndClients) {
      const slavesData = get(
        state,
        `${wizardName}Steps.SLAVES_AND_CLIENTS.data`,
        {},
      );
      await storeStepDataAndFlush("SLAVES_AND_CLIENTS", {
        ...slavesData,
        serviceComponents: cloneDeep(savedTopology.slavesAndClients),
      });
    }
    const mastersChanged = topologyRecoveryInputs.includes("masters");
    const recoveryStep = isAddServiceWizard()
      ? (mastersChanged ? 2 : 3)
      : (mastersChanged ? 5 : 6);
    setDeploymentError("");
    setTopologyRecoveryInputs([]);
    jumpToStep(recoveryStep);
  };

  const saveManagedDependencySelections = async (selections: Record<string, any>) => {
    const servicesData = get(state, `${wizardName}Steps.SERVICES.data`, {});
    const nextData = { ...servicesData, managedDependencies: selections };
    if (storeStepDataAndFlush) {
      await storeStepDataAndFlush("SERVICES", nextData);
      return;
    }
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: { step: "SERVICES", data: nextData },
    });
    await Promise.resolve(flushStateToDb("checkpoint", -1, `${deploymentStatePrefix}_DEPLOY_PREP_2`));
  };

  const saveMaterializationRecord = async (
    dependencyType: ManagedDependencyType,
    record: ManagedDependencyMaterializationRecord,
  ) => {
    managedDependencyMaterializations.current = {
      ...managedDependencyMaterializations.current,
      [dependencyType]: record,
    };
    await saveReviewData({
      managedDependencyMaterializations: managedDependencyMaterializations.current,
    });
  };

  const reconcileMaterializationRecord = async (
    record: ManagedDependencyMaterializationRecord,
    generation: number,
  ) => {
    try {
      const binding = await ServiceDependenciesApi.get(
        record.clusterName,
        record.bindingId,
      );
      assertCurrentDeployment(generation);
      const recovered = assertManagedDependencyRecord(binding, record);
      setManagedDependencyBindings((current) => ({
        ...current,
        [record.dependencyType]: binding,
      }));
      return recovered;
    } catch (error: any) {
      assertCurrentDeployment(generation);
      if (isMissingManagedDependency(error)) return null;
      throw error;
    }
  };

  const reconcileManagedDependencyMaterializations = async (generation: number) => {
    let changed = false;
    for (const record of Object.values(managedDependencyMaterializations.current)) {
      if (!record || !managedDependencyAttempts(record).length) continue;
      const { targetClusterId, targetClusterName } = targetClusterIdentity();
      if (record.clusterId !== targetClusterId || record.clusterName !== targetClusterName) {
        throw new Error(t("installer.step8.dependencyTargetChanged"));
      }
      const recovered = await reconcileMaterializationRecord(record, generation);
      const operationKey = `create-managed-dependency-${record.dependencyType}-${record.bindingId}`;
      if (recovered) {
        const choice = managedDependencies[record.dependencyType];
        if (!choice?.preview
          || !managedDependencyAttemptMatchesPreview(recovered.attempt, choice.preview)) {
          throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
        }
        completedOperationIds.current.add(operationKey);
        if (record.response !== recovered.binding) {
          managedDependencyMaterializations.current = {
            ...managedDependencyMaterializations.current,
            [record.dependencyType]: recordManagedDependencyResponse(
              record,
              recovered.attempt,
              recovered.binding,
            ),
          };
          changed = true;
        }
      } else {
        completedOperationIds.current.delete(operationKey);
      }
    }
    if (changed) {
      await saveReviewData({
        completedOperationIds: [...completedOperationIds.current],
        managedDependencyMaterializations: managedDependencyMaterializations.current,
      });
      assertCurrentDeployment(generation);
    }
  };

  const pauseForManagedDependencyReview = async (
    selections: Record<string, any>,
    generation: number,
  ) => {
    await saveManagedDependencySelections(selections);
    assertCurrentDeployment(generation);
    await saveReviewData({
      managedDependencyPendingReviewSignature: managedSelectionSignature(selections),
    });
    assertCurrentDeployment(generation);
    setManagedDependencyReviewPending(true);
    throw new ManagedDependencyReviewRequiredError();
  };

  const completeProspectiveManagedDependencyPreviews = async (
    generation: number,
  ) => {
    const reviewed = reviewedManagedDependencies(managedDependencies);
    if (!reviewed.length) return;
    const currentRevision = Number(
      (isAddServiceWizard() ? getWorkflowRevision?.() : getDraftRevision?.()) || 0,
    );
    const { targetClusterId } = targetClusterIdentity();
    const consumer = isAddServiceWizard()
      ? {
          scope: "SERVICE_PLAN" as const,
          cluster_id: targetClusterId,
          expected_revision: currentRevision,
        }
      : {
          scope: "DRAFT" as const,
          draft_id: draftId,
          expected_revision: currentRevision,
        };
    const selections = buildManagedDependencyPlanSelections(
      reviewed.map(({ choice, dependencyType }) => {
        if (!choice.preview?.compatible) {
          throw new Error(t("installer.step8.dependencyPreviewFailed"));
        }
        return { dependencyType, preview: choice.preview };
      }),
    );
    let response: ManagedDependencyPlanPreviewResponse;
    try {
      response = await ServiceDependenciesApi.previewPlan({ consumer, selections });
    } catch (error: any) {
      assertCurrentDeployment(generation);
      throw error;
    }
    assertCurrentDeployment(generation);
    if (!Array.isArray(response?.items)
      || response.items.length !== reviewed.length) {
      throw new Error(t("installer.step8.dependencyPreviewFailed"));
    }
    const returnedByType = new Map(
      response.items.map((preview) => [preview.dependency_type, preview]),
    );
    const nextSelections = cloneDeep(managedDependencies);
    let reviewChanged = false;
    reviewed.forEach(({ choice, dependencyType }) => {
      const preview = returnedByType.get(dependencyType);
      if (!preview?.compatible) {
        throw new Error(
          preview?.errors?.[0]?.message || t("installer.step8.dependencyPreviewFailed"),
        );
      }
      managedDependencyPlanPreviewsRef.current[dependencyType] = preview;
      if (!choice.preview
        || managedDependencyReviewSignature(preview)
          !== managedDependencyReviewSignature(choice.preview)) {
        nextSelections[dependencyType] = { ...choice, preview, planningIssue: undefined };
        reviewChanged = true;
      }
    });
    if (reviewChanged) {
      await pauseForManagedDependencyReview(nextSelections, generation);
    }
  };

  const materializeManagedDependency = async (
    dependencyType: ManagedDependencyType,
    generation: number,
  ) => {
    const choice = managedDependencies[dependencyType];
    if (choice?.mode !== "managed" || !choice.provider || !choice.preview?.compatible) {
      return;
    }
    const { targetClusterId, targetClusterName } = targetClusterIdentity();
    let record = managedDependencyMaterializations.current[dependencyType];
    if (record) {
      if (record.clusterId !== targetClusterId
        || record.clusterName !== targetClusterName
        || record.bindingId !== choice.preview.binding_id) {
        throw new Error(t("installer.step8.dependencySelectionChangedAfterAttempt"));
      }
      const reconciled = await reconcileMaterializationRecord(record, generation);
      if (reconciled) {
        if (!managedDependencyAttemptMatchesPreview(reconciled.attempt, choice.preview)) {
          throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
        }
        record = recordManagedDependencyResponse(
          record,
          reconciled.attempt,
          reconciled.binding,
        );
        await saveMaterializationRecord(dependencyType, record);
        return;
      }

      const latestAttempt = managedDependencyAttempts(record).at(-1);
      const currentRevision = Number(
        (isAddServiceWizard() ? getWorkflowRevision?.() : getDraftRevision?.()) || 0,
      );
      const canReplayExactRequest = latestAttempt
        && managedDependencyAttemptMatchesPreview(latestAttempt, choice.preview)
        && (!latestAttempt.request.draft
          || latestAttempt.request.draft.revision === currentRevision);
      if (latestAttempt && canReplayExactRequest) {
        try {
          const binding = await ServiceDependenciesApi.create(
            targetClusterName,
            latestAttempt.request,
          );
          assertCurrentDeployment(generation);
          const recovered = assertManagedDependencyRecord(binding, record);
          if (!managedDependencyAttemptMatchesPreview(recovered.attempt, choice.preview)) {
            throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
          }
          record = recordManagedDependencyResponse(record, recovered.attempt, binding);
          await saveMaterializationRecord(dependencyType, record);
          assertCurrentDeployment(generation);
          setManagedDependencyBindings((current) => ({
            ...current,
            [dependencyType]: binding,
          }));
          return;
        } catch (error: any) {
          assertCurrentDeployment(generation);
          const reconciledAfterFailure = await reconcileMaterializationRecord(record, generation);
          if (reconciledAfterFailure) {
            if (!managedDependencyAttemptMatchesPreview(
              reconciledAfterFailure.attempt,
              choice.preview,
            )) {
              throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
            }
            record = recordManagedDependencyResponse(
              record,
              reconciledAfterFailure.attempt,
              reconciledAfterFailure.binding,
            );
            await saveMaterializationRecord(dependencyType, record);
            return;
          }
          throw error;
        }
      }
    }

    let finalPreview: ManagedDependencyPreview;
    try {
      finalPreview = await ServiceDependenciesApi.previewService(
        targetClusterName,
        dependencyType,
        {
          cluster_id: choice.provider.cluster_id,
          service_name: choice.provider.service_name,
        },
        undefined,
        choice.preview.binding_id,
      );
    } catch (error: any) {
      assertCurrentDeployment(generation);
      if (record && isBindingIdUnavailable(error)) {
        const reconciled = await reconcileMaterializationRecord(record, generation);
        if (reconciled) {
          if (!managedDependencyAttemptMatchesPreview(reconciled.attempt, choice.preview)) {
            throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
          }
          record = recordManagedDependencyResponse(
            record,
            reconciled.attempt,
            reconciled.binding,
          );
          await saveMaterializationRecord(dependencyType, record);
          return;
        }
      }
      throw error;
    }
    assertCurrentDeployment(generation);
    if (!finalPreview.compatible) {
      throw new Error(
        finalPreview.errors?.[0]?.message || t("installer.step8.dependencyPreviewFailed"),
      );
    }
    if (managedDependencyReviewSignature(finalPreview)
      !== managedDependencyReviewSignature(choice.preview)) {
      await pauseForManagedDependencyReview({
        ...managedDependencies,
        [dependencyType]: { ...choice, preview: finalPreview },
      }, generation);
    }

    record ||= {
      bindingId: finalPreview.binding_id,
      clusterId: targetClusterId,
      clusterName: targetClusterName,
      dependencyType,
      operationId: "",
    };
    const operationId = createSecureUuid();
    const draft = isAddServiceWizard()
      ? undefined
      : { id: draftId, revision: Number(getDraftRevision?.() || 0) + 1 };
    const request = buildManagedDependencyCreateRequest({
      draft,
      operationId,
      preview: finalPreview,
    });
    const attempt: ManagedDependencyMaterializationAttempt = { operationId, request };
    record = appendManagedDependencyAttempt(record, attempt);
    await saveMaterializationRecord(dependencyType, record);
    assertCurrentDeployment(generation);
    if (draft && Number(getDraftRevision?.() || 0) !== draft.revision) {
      throw new Error(t("installer.step8.dependencyDraftRevisionChanged"));
    }
    try {
      const binding = await ServiceDependenciesApi.create(targetClusterName, request);
      assertCurrentDeployment(generation);
      const recovered = assertManagedDependencyRecord(binding, record);
      record = recordManagedDependencyResponse(record, recovered.attempt, binding);
      await saveMaterializationRecord(dependencyType, record);
      assertCurrentDeployment(generation);
      setManagedDependencyBindings((current) => ({ ...current, [dependencyType]: binding }));
    } catch (error: any) {
      assertCurrentDeployment(generation);
      const reconciled = await reconcileMaterializationRecord(record, generation);
      if (reconciled) {
        if (!managedDependencyAttemptMatchesPreview(reconciled.attempt, finalPreview)) {
          throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
        }
        record = recordManagedDependencyResponse(
          record,
          reconciled.attempt,
          reconciled.binding,
        );
        await saveMaterializationRecord(dependencyType, record);
        return;
      }
      throw error;
    }
  };

  const materializeManagedDependencyPlan = async (generation: number) => {
    const reviewed = reviewedManagedDependencies(managedDependencies);
    if (!reviewed.length) return;
    const { targetClusterId, targetClusterName } = targetClusterIdentity();
    const currentRevision = Number(
      (isAddServiceWizard() ? getWorkflowRevision?.() : getDraftRevision?.()) || 0,
    );

    // Reconcile every saved identity before deciding whether a batch POST is needed.
    // A lost response for one item must not cause the other item to be recreated alone.
    for (const { dependencyType, choice } of reviewed) {
      if (!choice.preview?.compatible) {
        throw new Error(t("installer.step8.dependencyPreviewFailed"));
      }
      const record = managedDependencyMaterializations.current[dependencyType];
      if (!record) continue;
      if (record.clusterId !== targetClusterId
        || record.clusterName !== targetClusterName
        || record.bindingId !== choice.preview.binding_id) {
        throw new Error(t("installer.step8.dependencySelectionChangedAfterAttempt"));
      }
      const recovered = await reconcileMaterializationRecord(record, generation);
      if (recovered) {
        if (!managedDependencyAttemptMatchesPreview(recovered.attempt, choice.preview)) {
          throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
        }
        managedDependencyMaterializations.current = {
          ...managedDependencyMaterializations.current,
          [dependencyType]: recordManagedDependencyResponse(
            record,
            recovered.attempt,
            recovered.binding,
          ),
        };
      }
    }

    const nextMaterials = { ...managedDependencyMaterializations.current };
    const requests = reviewed.map(({ dependencyType, choice }) => {
      const preview = managedDependencyPlanPreviewsRef.current[dependencyType]
        || choice.preview!;
      const existing = nextMaterials[dependencyType];
      const latestAttempt = existing && managedDependencyAttempts(existing).at(-1);
      const draft = isAddServiceWizard()
        ? undefined
        : { id: draftId, revision: currentRevision + 1 };
      const reusable = latestAttempt
        && managedDependencyAttemptMatchesPreview(latestAttempt, preview)
        && (Boolean(latestAttempt.response)
          || !latestAttempt.request.draft
          || latestAttempt.request.draft.revision === currentRevision);
      const request = reusable
        ? latestAttempt.request
        : buildManagedDependencyCreateRequest({
            draft,
            operationId: createSecureUuid(),
            preview,
          });
      const record = existing || {
        bindingId: request.binding_id,
        clusterId: targetClusterId,
        clusterName: targetClusterName,
        dependencyType,
        operationId: request.operation_id,
      };
      nextMaterials[dependencyType] = reusable
        ? record
        : appendManagedDependencyAttempt(record, {
            operationId: request.operation_id,
            request,
          });
      return request;
    });

    const managedDependencyInstallIntent = currentManagedInstallIntent();
    const managedDependencyHandoff = {
      phase: "WAIT_FOR_PROVIDER_PREPARATION" as const,
      clusterId: targetClusterId,
      clusterName: targetClusterName,
      consumerServiceName: "HBASE" as const,
      installIntent: managedDependencyInstallIntent,
      items: requests.map((request) => ({
        bindingId: request.binding_id,
        dependencyType: request.dependency_type,
        operationId: request.operation_id,
      })),
    };

    await saveReviewData({
      managedDependencyMaterializations: nextMaterials,
      managedDependencyInstallIntent,
      managedDependencyHandoff,
    });
    assertCurrentDeployment(generation);
    managedDependencyMaterializations.current = nextMaterials;

    try {
      const bindings = await ServiceDependenciesApi.createMany(
        targetClusterName,
        requests,
      );
      assertCurrentDeployment(generation);
      if (!Array.isArray(bindings) || bindings.length !== requests.length) {
        throw new Error(t("installer.step8.dependencyMaterializationIncomplete"));
      }
      const bindingsByIdentity = new Map(
        bindings.map((binding) => [
          `${binding.dependency_type}:${binding.binding_id}`,
          binding,
        ]),
      );
      const resolvedMaterials = { ...nextMaterials };
      const resolvedBindings: Partial<Record<ManagedDependencyType, ManagedDependencyBinding>> = {};
      reviewed.forEach(({ dependencyType }) => {
        const record = resolvedMaterials[dependencyType]!;
        const request = managedDependencyAttempts(record).at(-1)!.request;
        const binding = bindingsByIdentity.get(`${dependencyType}:${request.binding_id}`);
        if (!binding) {
          throw new Error(t("installer.step8.dependencyMaterializationIncomplete"));
        }
        const recovered = assertManagedDependencyRecord(binding, record);
        resolvedMaterials[dependencyType] = recordManagedDependencyResponse(
          record,
          recovered.attempt,
          binding,
        );
        resolvedBindings[dependencyType] = binding;
      });
      managedDependencyMaterializations.current = resolvedMaterials;
      setManagedDependencyBindings((current) => ({ ...current, ...resolvedBindings }));
      await saveReviewData({
        managedDependencyMaterializations: resolvedMaterials,
        managedDependencyInstallIntent,
        managedDependencyHandoff,
      });
    } catch (error: any) {
      assertCurrentDeployment(generation);
      let unresolved = false;
      for (const { dependencyType } of reviewed) {
        const record = managedDependencyMaterializations.current[dependencyType];
        if (!record) {
          unresolved = true;
          continue;
        }
        const recovered = await reconcileMaterializationRecord(record, generation);
        if (!recovered) {
          unresolved = true;
          continue;
        }
        const choice = managedDependencies[dependencyType];
        if (!choice?.preview
          || !managedDependencyAttemptMatchesPreview(recovered.attempt, choice.preview)) {
          throw new Error(t("installer.step8.dependencyExistingBindingRequiresReview"));
        }
        managedDependencyMaterializations.current = {
          ...managedDependencyMaterializations.current,
          [dependencyType]: recordManagedDependencyResponse(
            record,
            recovered.attempt,
            recovered.binding,
          ),
        };
        setManagedDependencyBindings((current) => ({
          ...current,
          [dependencyType]: recovered.binding,
        }));
      }
      if (unresolved) throw error;
      await saveReviewData({
        managedDependencyMaterializations: managedDependencyMaterializations.current,
      });
    }
  };

  const approveManagedDependencyReview = async () => {
    const signature = managedSelectionSignature(managedDependencies);
    await saveReviewData({
      managedDependencyApprovedSignature: signature,
      managedDependencyPendingReviewSignature: null,
    });
    setManagedDependencyReviewPending(false);
    setDeploymentError("");
    setDeploymentStage(t("installer.step8.readyToDeploy"));
    setIsNextEnabled(true);
  };

  const returnToManagedDependencySelection = async () => {
    const servicesStep = isAddServiceWizard() ? 1 : 4;
    await Promise.resolve(flushStateToDb("jump", servicesStep));
    jumpToStep(servicesStep);
  };

  const reconcileCreatedClusterDraft = async () => {
    const targetClusterName = getStepData("NAME", "clusterName");
    if (!draftId) {
      throw new Error(t("installer.step8.draftMissing"));
    }
    try {
      const target = await WorkflowStateApi.getCreationDraftCluster(draftId);
      if (target.cluster_name !== targetClusterName || !target.cluster_id) {
        throw new Error(t("installer.step8.clusterIdentityMismatch"));
      }
      if (deploymentArtifacts.current.clusterId != null
        && String(deploymentArtifacts.current.clusterId) !== String(target.cluster_id)) {
        throw new Error(t("installer.step8.clusterIdentityChanged"));
      }
      authoritativeClusterId.current = Number(target.cluster_id);
      if (String(deploymentArtifacts.current.clusterId || "") !== String(target.cluster_id)) {
        deploymentArtifacts.current.clusterId = target.cluster_id;
        await saveReviewData({ ...deploymentArtifacts.current });
      }
      return true;
    } catch (error: any) {
      if (error?.response?.status !== 404) throw error;
      authoritativeClusterId.current = null;
      if (deploymentArtifacts.current.clusterId != null
        || completedOperationIds.current.has("create-cluster")) {
        throw new Error(
          t("installer.step8.clusterNoLongerAvailable", { cluster: targetClusterName }),
        );
      }
      return false;
    }
  };

  const validateClusterTarget = async () => {
    const targetClusterName = getStepData("NAME", "clusterName");
    const response = await ClusterApi.getAllClusters();
    const collision = get(response, "items", []).find(
      (item: any) => item?.Clusters?.cluster_name === targetClusterName,
    );
    if (collision) {
      if (authoritativeClusterId.current != null
        && String(collision?.Clusters?.cluster_id) === String(authoritativeClusterId.current)) {
        return;
      }
      throw new Error(
        t("installer.step8.clusterNameCollision", { cluster: targetClusterName }),
      );
    }
  };

  const resolveOrCreateRepositoryVersion = async () => {
    const selectedStack = getStepData("VERSION", "selectedStack");
    const selectedVersion = getStepData("VERSION", "selectedVersion");
    const operatingSystems = getStepData(
      "VERSION",
      `operatingSystems.${selectedVersion.id}`,
    ) || [];
    const definitions = await VersionsApi.getVersionDefinitions(
      selectedStack.stack_name,
    );
    const resolution = resolveRepositoryVersion({
      items: get(definitions, "items", []),
      ambariManagedRepositories: !getStepData("VERSION", "redhatSatellite"),
      operatingSystems,
      repositoryVersion: selectedVersion.repository_version || selectedVersion.id,
      stackName: selectedStack.stack_name,
      stackVersion: selectedStack.stack_version,
    });
    if (resolution.kind === "conflict") {
      throw new Error(t("installer.step8.repositoryVersionConflict", {
        version: resolution.versionLabel,
      }));
    }
    if (resolution.kind === "reuse") {
      deploymentArtifacts.current.repositoryVersionId = resolution.repositoryVersionId;
      deploymentArtifacts.current.repositoryVersionReused = true;
      deploymentArtifacts.current.stackName = resolution.stackName;
      deploymentArtifacts.current.stackVersion = resolution.stackVersion;
      return;
    }

    const source = getStepData("VERSION", "versionDefinitionSource");
    const sourcePayload = source?.payload || {
      VersionDefinition: { available: selectedStack.id },
    };
    const payload = buildAtomicVersionDefinitionPayload(
      sourcePayload,
      getInitialOperatingSystems(),
    );
    const response = await postVersionDefinition(
      payload,
      typeof sourcePayload === "string" ? {} : source?.headers,
    );
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
      throw new Error(t("installer.step8.repositoryResultMissing"));
    }
    deploymentArtifacts.current.repositoryVersionId = versionDefinition.id;
    deploymentArtifacts.current.repositoryVersionReused = false;
    deploymentArtifacts.current.stackName = versionDefinition.stack_name;
    deploymentArtifacts.current.stackVersion = versionDefinition.stack_version;
  };

  const buildDeploymentStages = (generation: number) => {
    const scoped = (operation: { id: string; label: string; run: () => Promise<unknown> }) => ({
      ...operation,
      run: async () => {
        assertCurrentDeployment(generation);
        const result = await operation.run();
        assertCurrentDeployment(generation);
        return result;
      },
    });
    const managedPlanOperationKey = reviewedManagedDependencies(managedDependencies)
      .map(({ choice, dependencyType }) => [
        dependencyType,
        choice.preview?.binding_id || "pending",
        choice.preview?.provider_fingerprint || choice.provider?.cluster_id || "provider",
        choice.preview?.consumer_descriptor_fingerprint || "consumer",
      ].join(":"))
      .join("|") || "local";
    const commonOperations = [
      { id: "create-services", label: "Creating services", run: () =>
        createSelectedServices(deploymentArtifacts.current.repositoryVersionId, generation) },
      {
        id: `complete-managed-dependency-previews-${managedPlanOperationKey}`,
        label: t("installer.step8.reviewingManagedDependencies"),
        run: () => completeProspectiveManagedDependencyPreviews(generation),
      },
      { id: `apply-configurations-${managedPlanOperationKey}`, label: "Applying configurations", run: applyConfigurationsToCluster },
      { id: "create-components", label: "Creating service components", run: () => createComponents(generation) },
      { id: "create-configuration-groups", label: "Saving configuration groups", run: createConfigurationGroups },
      { id: "register-masters", label: "Assigning master components", run: () => createMasterHostComponents(generation) },
      { id: "register-slaves-clients", label: "Assigning slave and client components", run: () => createSlaveAndClientsHostComponents(generation) },
      { id: "register-required-components", label: "Assigning required components", run: () => createAdditionalHostComponents(generation) },
    ];
    const managedDependencyOperations = reviewedManagedDependencies(managedDependencies).length
      ? [{
          id: `create-managed-dependency-plan-${managedPlanOperationKey}`,
          label: t("installer.step8.preparingManagedDependencies"),
          run: () => materializeManagedDependencyPlan(generation),
        }]
      : [];

    if (isAddServiceWizard()) {
      const kerberosOperations = isKerberosEnabled && !isManualKerberos
        ? [{
            id: "update-kerberos-descriptor",
            label: "Updating the Kerberos descriptor",
            run: updateKerberosDescriptorMethod,
          }]
        : [];
      return [
        ...commonOperations.slice(0, 1),
        ...kerberosOperations,
        ...commonOperations.slice(1),
        ...managedDependencyOperations,
      ].map((operation) => ({ operations: [scoped(operation)] }));
    }

    return [
      { operations: [scoped({ id: "validate-cluster-target", label: t("installer.step8.checkingClusterName"), run: validateClusterTarget })] },
      { operations: [scoped({ id: "resolve-repository-version", label: t("installer.step8.resolvingRepositoryVersion"), run: resolveOrCreateRepositoryVersion })] },
      { operations: [scoped({ id: "create-cluster", label: "Creating the cluster", run: createCluster })] },
      ...commonOperations.slice(0, 4).map((operation) => ({ operations: [scoped(operation)] })),
      { operations: [scoped({ id: "register-hosts", label: "Adding hosts to the cluster", run: registerHostsToCluster })] },
      ...commonOperations.slice(4).map((operation) => ({ operations: [scoped(operation)] })),
      ...managedDependencyOperations.map((operation) => ({ operations: [scoped(operation)] })),
    ];
  };

  const waitForKdcSession = () => new Promise<void>((resolve, reject) => {
    void getKDCSessionState(resolve, reject).catch(reject);
  });

  const deploy = async () => {
    if (deploymentTriggered) return;
    const generation = deploymentGenerationRef.current;
    if (!isAddServiceWizard()) {
      const requiredReentry = clusterCreationReentrySteps({ state });
      if (requiredReentry.length) {
        setDeploymentError(
          t("installer.step8.reentryBeforeDeploy", {
            steps: requiredReentry.map(({ labelKey }) => t(labelKey)).join(", "),
          }),
        );
        return;
      }
    }
    if (isAddServiceWizard() && isKerberosEnabled && !isKerberosDescriptorReady) {
      setKerberosPreparationError(
        "The Kerberos descriptor must be prepared before deployment.",
      );
      return;
    }
    setDeploymentTriggered(true);
    setIsNextEnabled(false);
    setDeploymentError("");
    setTopologyRecoveryInputs([]);
    currentHostAssignmentsRef.current = null;

    try {
      if (!isAddServiceWizard()) {
        await reconcileCreatedClusterDraft();
      }
      assertCurrentDeployment(generation);
      await reconcileManagedDependencyMaterializations(generation);
      await reconcileDeploymentInputs(generation);
      await saveReviewData({
        completedOperationIds: [...completedOperationIds.current],
        deploymentStage: "Preparing deployment",
      });
      assertCurrentDeployment(generation);
      const stages = buildDeploymentStages(generation);
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

      if (reviewedManagedDependencies(managedDependencies).length) {
        setDeploymentStage(t("installer.step8.providerPreparationStarted"));
        setDeploymentTriggered(false);
        setIsNextEnabled(false);
        const managedDependencyHandoff = reviewDataRef.current.managedDependencyHandoff;
        const managedDependencyInstallIntent =
          reviewDataRef.current.managedDependencyInstallIntent
          || managedDependencyHandoff?.installIntent;
        await saveReviewData({
          completedOperationIds: [...completedOperationIds.current],
          deploymentStage: t("installer.step8.providerPreparationStarted"),
          managedDependencyMaterializations: managedDependencyMaterializations.current,
          managedDependencyHandoff,
          managedDependencyInstallIntent,
        });
        const clusterStatus = {
          ...(getStepData("REVIEW", "clusterStatus") || {}),
          status: "PENDING",
          phase: "WAIT_FOR_PROVIDER_PREPARATION",
          managedDependencyHandoff,
          managedDependencyInstallIntent,
        };
        await storeStepDataAndFlush("INSTALL_START_TEST", {
          clusterStatus,
          phase: "WAIT_FOR_PROVIDER_PREPARATION",
          managedDependencyHandoff,
          managedDependencyInstallIntent,
          hostInfo: getStepData("INSTALL_START_TEST", "hostInfo") || [],
        });
        handleNextImperitive();
        return;
      }

      if (isAddServiceWizard()) {
        await waitForKdcSession();
      }
      setDeploymentStage("Starting component installation");
      await installServices();
    } catch (error: any) {
      if (error instanceof ManagedDependencyReviewRequiredError) {
        setDeploymentStage(t("installer.step8.managedDependencyReviewRequired"));
        setDeploymentTriggered(false);
        setIsNextEnabled(false);
        return;
      }
      const message = error instanceof DeploymentTopologyRecoveryRequiredError
        ? t("installer.step8.topologyRecoveryRequired")
        : error instanceof ManagedDependencyAttemptLimitError
        ? t("installer.step8.dependencyAttemptLimit")
        : error?.response?.data?.message
        || error?.message
        || t("installer.step8.prepareFailed");
      setDeploymentError(String(message));
      if (error instanceof DeploymentTopologyRecoveryRequiredError) {
        setTopologyRecoveryInputs(error.changedInputs);
      }
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
        <div className="mt-4">
          <ManagedDependencySettings
            onReturn={() => void returnToManagedDependencySelection()}
            selections={managedDependencies}
            view="review"
          />
          {managedDependencyReviewPending ? (
            <Alert variant="warning">
              <div>{t("installer.step8.managedDependencyReviewRequired")}</div>
              <Button
                className="mt-2"
                size="sm"
                variant="primary"
                onClick={() => void approveManagedDependencyReview().catch((error: any) =>
                  setDeploymentError(String(
                    error?.response?.data?.message
                      || error?.message
                      || t("installer.step8.prepareFailed"),
                  )))}
              >
                {t("installer.step8.approveManagedDependencies")}
              </Button>
            </Alert>
          ) : null}
          {Object.values(managedDependencyBindings).length ? (
            <section aria-labelledby="managed-dependency-progress-heading">
              <h3 className="h5" id="managed-dependency-progress-heading">
                {t("installer.step8.managedDependencyProgress")}
              </h3>
              <Alert variant="info">
                {t("installer.step8.providerPreparationStarted")}
              </Alert>
              <div className="row g-3">
                {Object.values(managedDependencyBindings).map((binding) => (
                  <div className="col-12 col-xl-6" key={binding.binding_id}>
                    <DependencyCard binding={binding} />
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </div>
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
            <div>{deploymentError}</div>
            {topologyRecoveryInputs.length
              && reviewDataRef.current.deploymentTopologyIntent ? (
              <Button
                className="mt-2"
                size="sm"
                variant="outline-danger"
                onClick={() => void restoreSavedTopologyIntent()}
              >
                {t("installer.step8.restoreSavedAssignments")}
              </Button>
            ) : null}
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
