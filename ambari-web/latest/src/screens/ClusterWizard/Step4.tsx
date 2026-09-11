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

import { useContext, useEffect, useRef, useState } from "react";
import { ChooseServicesApi } from "../../api/chooseServicesApi";
import Table from "../../components/Table";
import { cloneDeep, forEach, get, isEmpty } from "lodash";
import { Alert, Button, Form } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import MissingServiceModal from "../../components/MissingServiceModal";
import {
  dfsServices,
  displayOrder,
  coSelectedServices,
  excludeServicesOnDisplay,
  ModalType,
  warnningMessages,
} from "./constants";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./clusterStore/types";
import { getStepData } from "../../Utils/Utility";
import { ContextWrapper } from ".";
import { AppContext } from "../../store/context";
import Spinner from "../../components/Spinner";
import {
  deriveAddServiceFlow,
  nextAddServiceStep,
} from "../Services/AddServiceWizard/addServiceNavigation";
import { filterInstallableStackServices } from "../../Utils/stackMetadata";
import { consumeAddServiceSelectionIntent } from "../../Utils/workflowSelectionIntent";
import ManagedDependencySelector, {
  type DependencyConsumerScope,
} from "./ManagedDependencySelector";
import {
  choiceCanContinue,
  dependencyTypes,
  localDependencyDefaults,
  type ManagedDependencyChoice,
  type ManagedDependencySelections,
} from "./managedDependencySelection";
import type { ManagedDependencyType } from "../../api/serviceDependenciesApi";

type Service = {
  displayName: string;
  serviceName: string;
  serviceType: string;
  version: string;
  comments: string;
  selected: boolean;
  required: string[];
  isIgnored?: boolean;
  isHiddenOnDisplay: boolean;
  hasClient?: boolean;
  hasConfigs?: boolean;
  hasMaster?: boolean;
  hasNonMastersWithCustomAssignment?: boolean;
  hasSlave?: boolean;
  installed?: boolean;
  canToggle?: boolean;
};

type ErrorType = {
  serviceName: string;
  modalType: string;
};

const isRequiredByAnotherSelectedService = (
  dependencyType: string,
  candidateServices: { [key: string]: Service },
) => Object.values(candidateServices).some((service) =>
  service.selected
  && service.serviceName !== "HBASE"
  && service.required?.includes(dependencyType));

const hasFreshHBaseSelection = (candidateServices: { [key: string]: Service }) =>
  Boolean(candidateServices.HBASE?.selected && !candidateServices.HBASE.installed);

export default function Step4({ wizardName = "clusterCreation" }) {
  const { t } = useTranslation();
  const [services, setServices] = useState<{ [key: string]: Service }>({});
  const [catalogAttempt, setCatalogAttempt] = useState(0);
  const [catalogError, setCatalogError] = useState("");
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [errorStack, setErrorStack] = useState<ErrorType[]>([]);
  const [showModal, setShowModal] = useState<boolean>(false);
  const [nextDisabled, setNextDisabled] = useState<boolean>(false);
  const [managedDependencies, setManagedDependencies] =
    useState<ManagedDependencySelections>({});
  const [autoSelectedLocalServices, setAutoSelectedLocalServices] =
    useState<string[]>([]);
  const servicesRef = useRef(services);
  const managedDependenciesRef = useRef(managedDependencies);
  const autoSelectedLocalServicesRef = useRef(autoSelectedLocalServices);
  const dependencyCheckpointRef = useRef<Promise<number> | null>(null);
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    storeStepDataAndFlush,
    draftId,
    getDraftRevision,
    getWorkflowRevision,
    serviceContextLoading = false,
    handleBackImperitive,
    installedServices: installedServicesProps = [],
    workflowMaterializedServices = [],
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  }: any = useContext(Context);
  const {
    cluster,
    clusterName,
    loginName,
  } = useContext(AppContext);
  const installedServices = installedServicesProps;
  const stepData = getStepData(
    state,
    currentStep.name,
    "",
    `${wizardName}Steps`,
  );
  const restoredServicesStepRef = useRef(stepData);

  const versionStepData = get(state, `${wizardName}Steps.VERSION.data`, {});
  const version = get(versionStepData, "selectedVersion.stack_version", "");
  const stack = get(versionStepData, "selectedStack.stack_name", "");
  const numericClusterId = Number(cluster?.cluster_id);

  const updateServices = (nextServices: { [key: string]: Service }) => {
    servicesRef.current = nextServices;
    setServices(nextServices);
  };

  const updateManagedDependencies = (next: ManagedDependencySelections) => {
    managedDependenciesRef.current = next;
    setManagedDependencies(next);
  };

  const updateAutoSelectedLocalServices = (next: string[]) => {
    autoSelectedLocalServicesRef.current = next;
    setAutoSelectedLocalServices(next);
  };

  const isDFS = (serviceName: string) => {
    return dfsServices.includes(serviceName);
  };

  const selectAllServices = () => {
    const updatedServices = cloneDeep(services);
    const allSelected = isAllServicesSelected();
    Object.values(updatedServices).forEach((service: Service) => {
      if (!isDFS(service.serviceName)) {
        service.selected = isServiceSelected(service.serviceName)
          ? true
          : !allSelected;
      }
      if (service.serviceName === "KERBEROS") {
        service.selected = false;
      }
    });
    if (!updatedServices.HBASE?.selected) {
      autoSelectedLocalServicesRef.current.forEach((serviceName) => {
        if (updatedServices[serviceName]?.installed !== true
          && !isRequiredByAnotherSelectedService(
            serviceName as ManagedDependencyType,
            updatedServices,
          )) {
          updatedServices[serviceName].selected = false;
        }
      });
      updateAutoSelectedLocalServices([]);
      updateManagedDependencies({});
    } else if (hasFreshHBaseSelection(updatedServices)) {
      updateManagedDependencies(localDependencyDefaults(managedDependenciesRef.current));
    } else {
      updateAutoSelectedLocalServices([]);
      updateManagedDependencies({});
    }
    updateServices(updatedServices);
  };

  const isAllServicesSelected = () => {
    for (const service of Object.values(services)) {
      if (
        !isDFS(service.serviceName) &&
        service.serviceName !== "KERBEROS" &&
        !service.selected
      ) {
        return false;
      }
    }
    return true;
  };

  const handleCheckboxChange = (serviceName: string) => {
    const updatedServices = cloneDeep(services);
    updatedServices[serviceName].selected =
      !updatedServices[serviceName].selected;

    if (coSelectedServices[serviceName]) {
      for (const coSelectedService of coSelectedServices[serviceName]) {
        if (updatedServices[coSelectedService]) {
          updatedServices[coSelectedService].selected =
            updatedServices[serviceName].selected;
        }
      }
    }
    let nextManagedDependencies = managedDependenciesRef.current;
    let nextAutoSelected = [...autoSelectedLocalServicesRef.current];
    if (serviceName === "HBASE") {
      if (updatedServices.HBASE.selected) {
        nextManagedDependencies = localDependencyDefaults(nextManagedDependencies);
        dependencyTypes.forEach((dependencyType) => {
          if (nextManagedDependencies[dependencyType]?.mode !== "local") return;
          const localService = updatedServices[dependencyType];
          if (localService && !localService.selected) {
            localService.selected = true;
            if (!localService.installed && !nextAutoSelected.includes(dependencyType)) {
              nextAutoSelected.push(dependencyType);
            }
          }
        });
      } else {
        nextAutoSelected.forEach((dependencyType) => {
          if (updatedServices[dependencyType]?.installed !== true
            && !isRequiredByAnotherSelectedService(dependencyType, updatedServices)) {
            updatedServices[dependencyType].selected = false;
          }
        });
        nextAutoSelected = [];
        nextManagedDependencies = {};
      }
    } else if (dependencyTypes.includes(serviceName as ManagedDependencyType)) {
      nextAutoSelected = nextAutoSelected.filter((item) => item !== serviceName);
      if (!updatedServices[serviceName].selected && updatedServices.HBASE?.selected
        && nextManagedDependencies[serviceName as ManagedDependencyType]?.mode === "local") {
        nextManagedDependencies = {
          ...nextManagedDependencies,
          [serviceName]: { mode: "managed" },
        };
      }
    }
    updateAutoSelectedLocalServices(nextAutoSelected);
    updateManagedDependencies(nextManagedDependencies);
    updateServices(updatedServices);
  };

  const buildServicesStepData = (
    nextServices: { [key: string]: Service },
    nextManagedDependencies: ManagedDependencySelections,
    nextAutoSelected: string[],
  ) => ({
    services: nextServices,
    addServiceFlow: deriveAddServiceFlow(nextServices),
    managedDependencies: hasFreshHBaseSelection(nextServices)
      ? localDependencyDefaults(nextManagedDependencies)
      : {},
    autoSelectedLocalServices: hasFreshHBaseSelection(nextServices)
      ? nextAutoSelected
      : [],
  });

  const persistServicesStep = async (
    nextServices: { [key: string]: Service },
    nextManagedDependencies: ManagedDependencySelections,
    nextAutoSelected: string[],
  ) => {
    const data = buildServicesStepData(
      nextServices,
      nextManagedDependencies,
      nextAutoSelected,
    );
    if (storeStepDataAndFlush) {
      return await storeStepDataAndFlush(currentStep.name, data);
    }
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: { step: currentStep.name, data },
    });
    await Promise.resolve(flushStateToDb("default"));
    return Number(getDraftRevision?.() || getWorkflowRevision?.() || 0);
  };

  const checkpointDependencySource = () => {
    if (dependencyCheckpointRef.current) return dependencyCheckpointRef.current;
    const pending = persistServicesStep(
      servicesRef.current,
      managedDependenciesRef.current,
      autoSelectedLocalServicesRef.current,
    );
    dependencyCheckpointRef.current = pending;
    const clearPending = () => {
      if (dependencyCheckpointRef.current === pending) {
        dependencyCheckpointRef.current = null;
      }
    };
    void pending.then(clearPending, clearPending);
    return pending;
  };

  const dependencyConsumer: DependencyConsumerScope | null =
    wizardName === "clusterCreation"
      ? (draftId ? {
          checkpoint: checkpointDependencySource,
          draftId,
          kind: "draft",
        } : null)
      : (Number.isInteger(numericClusterId) && numericClusterId > 0
        ? workflowMaterializedServices.includes("HBASE")
          ? {
              checkpoint: checkpointDependencySource,
              clusterId: numericClusterId,
              clusterName,
              kind: "service",
            }
          : {
              checkpoint: checkpointDependencySource,
              clusterId: numericClusterId,
              kind: "servicePlan",
            }
        : null);

  const changeManagedDependency = async (
    dependencyType: ManagedDependencyType,
    choice: ManagedDependencyChoice,
  ) => {
    const nextServices = cloneDeep(servicesRef.current);
    const nextManagedDependencies = {
      ...managedDependenciesRef.current,
      [dependencyType]: choice,
    };
    let nextAutoSelected = [...autoSelectedLocalServicesRef.current];
    const localService = nextServices[dependencyType];
    if (choice.mode === "local") {
      if (localService && !localService.selected) {
        localService.selected = true;
        if (!localService.installed && !nextAutoSelected.includes(dependencyType)) {
          nextAutoSelected.push(dependencyType);
        }
      }
    } else {
      const wasAutoSelected = nextAutoSelected.includes(dependencyType);
      if (wasAutoSelected && localService?.installed !== true
        && !isRequiredByAnotherSelectedService(dependencyType, nextServices)) {
        localService.selected = false;
      }
      nextAutoSelected = nextAutoSelected.filter((item) => item !== dependencyType);
    }
    updateServices(nextServices);
    updateManagedDependencies(nextManagedDependencies);
    updateAutoSelectedLocalServices(nextAutoSelected);
    return await persistServicesStep(
      nextServices,
      nextManagedDependencies,
      nextAutoSelected,
    );
  };

  const changeManagedDependencyPlan = async (
    nextManagedDependencies: ManagedDependencySelections,
  ) => {
    const nextServices = cloneDeep(servicesRef.current);
    let nextAutoSelected = [...autoSelectedLocalServicesRef.current];
    dependencyTypes.forEach((dependencyType) => {
      const choice = nextManagedDependencies[dependencyType] || { mode: "local" };
      const localService = nextServices[dependencyType];
      if (choice.mode === "local") {
        if (localService && !localService.selected) {
          localService.selected = true;
          if (!localService.installed && !nextAutoSelected.includes(dependencyType)) {
            nextAutoSelected.push(dependencyType);
          }
        }
        return;
      }
      const wasAutoSelected = nextAutoSelected.includes(dependencyType);
      if (wasAutoSelected && localService?.installed !== true
        && !isRequiredByAnotherSelectedService(dependencyType, nextServices)) {
        localService.selected = false;
      }
      nextAutoSelected = nextAutoSelected.filter((item) => item !== dependencyType);
    });
    updateServices(nextServices);
    updateManagedDependencies(nextManagedDependencies);
    updateAutoSelectedLocalServices(nextAutoSelected);
    return persistServicesStep(
      nextServices,
      nextManagedDependencies,
      nextAutoSelected,
    );
  };

  const saveServicesAndContinue = async () => {
    const flow = deriveAddServiceFlow(services);
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: buildServicesStepData(
          services,
          managedDependencies,
          autoSelectedLocalServices,
        ),
      },
    });
    if (wizardName === "addService") {
      const nextStep = nextAddServiceStep(1, flow);
      await Promise.resolve(flushStateToDb("jump", nextStep));
      jumpToStep(nextStep);
    } else {
      await Promise.resolve(flushStateToDb("next"));
      handleNextImperitive();
    }
  };

  const validateSelectedServices = () => {
    const selectedServices = Object.values(services).filter(
      (service) => service.selected === true
    );

    const newErrorStack: ErrorType[] = [];

    fileSystemServiceValidation(selectedServices, newErrorStack);

    for (const selectedService of selectedServices) {
      if (selectedService.installed) continue;
      selectedService?.required?.forEach((requiredService) => {
        dependantServiceValidation(
          selectedServices,
          selectedService.serviceName,
          requiredService,
          newErrorStack
        );
      });
    }

    serviceValidation("RANGER", newErrorStack);

    setErrorStack(newErrorStack);
    if (newErrorStack.length > 0) {
      setShowModal(true);
    } else {
      void saveServicesAndContinue();
    }
  };

  const dependantServiceValidation = (
    selectedServices: Service[],
    selectedServiceName: string,
    requiredService: string,
    errorStackCopy: ErrorType[]
  ) => {
    if (selectedServiceName === "HBASE"
      && hasFreshHBaseSelection(services)
      && dependencyTypes.includes(requiredService as ManagedDependencyType)
      && choiceCanContinue(
        managedDependencies[requiredService as ManagedDependencyType],
        Boolean(services[requiredService]?.selected),
      )) {
      return;
    }
    if (
      !selectedServices.find(
        (service) => service.serviceName === requiredService
      ) &&
      !errorStackCopy.find((error) => error.serviceName === requiredService)
    ) {
      errorStackCopy.push({
        serviceName: requiredService,
        modalType: ModalType.MISSING_DEPENDANT_SERVICE,
      });
    }
  };

  const serviceValidation = (service: string, errorStackCopy: ErrorType[]) => {
    const candidate = services[service];
    if (!candidate) return;

    if (
      candidate.selected === false &&
      !errorStackCopy.find(
        (missingService) => missingService.serviceName === service
      ) &&
      !candidate.isIgnored
    ) {
      errorStackCopy.push({
        serviceName: service,
        modalType: ModalType.MISSING_SERVICE,
      });
    }
  };

  const fileSystemServiceValidation = (
    selectedServices: Service[],
    errorStackCopy: ErrorType[]
  ) => {
    const selectedFileSystems = selectedServices.filter((service) =>
      dfsServices.includes(service.serviceName)
    );
    const managedHdfsSatisfiesHbase = hasFreshHBaseSelection(services)
      && choiceCanContinue(managedDependencies.HDFS, Boolean(services.HDFS?.selected));
    const installedHbaseHasAuthoritativeDependency = Boolean(services.HBASE?.installed);
    if (selectedFileSystems.length === 0
      && !managedHdfsSatisfiesHbase
      && !installedHbaseHasAuthoritativeDependency) {
      errorStackCopy.push({
        serviceName: "HDFS",
        modalType: ModalType.MISSING_FILE_SYSTEM,
      });
    }
  };

  const handleCloseAddServiceModal = () => {
    handleCheckboxChange(errorStack[0].serviceName);
    const updatedErrorStack = errorStack.slice(1);
    setErrorStack(updatedErrorStack);
    setShowModal(updatedErrorStack.length > 0);
  };

  const handleCloseLimitiedFunctionalityModal = () => {
    services[errorStack[0].serviceName].isIgnored = true;
    const updatedErrorStack = errorStack.slice(1);
    setErrorStack(updatedErrorStack);
    setShowModal(updatedErrorStack.length > 0);
  };

  const combineCoSelectedServices = (services: { [key: string]: Service }) => {
    Object.keys(coSelectedServices).forEach((service) => {
      forEach(coSelectedServices[service], (coSelectedService) => {
        if (!services[service] || !services[coSelectedService]) return;
        services[service].displayName =
          services[service].displayName +
          " + " +
          services[coSelectedService].displayName;
        services[coSelectedService].isHiddenOnDisplay = true;
      });
    });
  };

  const isServiceSelected = (serviceName: string) => {
    let isSelected = true;
    if (wizardName === "addService") {
      if (
        installedServices?.length &&
        installedServices?.includes(serviceName)
      ) {
        isSelected = true;
      } else {
        isSelected = false;
      }
      return isSelected;
    }
  };

  const canToggleServiceSelection = (serviceName: string) => {
    let canToggle = true;
    if (wizardName === "addService") {
      if (
        installedServices?.length &&
        installedServices?.includes(serviceName)
      ) {
        canToggle = false;
      } else {
        canToggle = true;
      }
      return canToggle;
    }
    return canToggle;
  };

  useEffect(() => {
    let currentRequest = true;
    const fetchServicesData = async () => {
      setCatalogLoading(true);
      setCatalogError("");
      try {
        const chooseServices = await ChooseServicesApi.getServices(
          stack,
          version
        );
        const transformedData: { [key: string]: any } = {};
        const installableServices = filterInstallableStackServices(
          get(chooseServices, "items", []),
        );
        const restoredStep = restoredServicesStepRef.current || {};
        const restoredServices = restoredStep.services || {};
        installableServices.forEach((service: any) => {
          const serviceName = service.StackServices.service_name;
          const restoredService = restoredServices[serviceName];
          const installed = installedServices.includes(serviceName);
          const components = get(service, "components", []).map(
            (component: any) => component.StackServiceComponents || {},
          );
          const configTypes = get(service, "StackServices.config_types");
          transformedData[serviceName] = {
            displayName: service.StackServices.display_name,
            serviceName,
            serviceType: service.StackServices.service_type,
            version: service.StackServices.service_version,
            comments: service.StackServices.comments,
            selected: installed || Boolean(restoredService?.selected)
              || Boolean(isServiceSelected(serviceName)),
            required: service.StackServices.required_services,
            isIgnored: Boolean(restoredService?.isIgnored),
            installed,
            canToggle: canToggleServiceSelection(serviceName),
            isHiddenOnDisplay: excludeServicesOnDisplay.includes(
              service.StackServices.service_name
            ),
            hasClient: components.some((component: any) => component.is_client),
            hasConfigs: configTypes == null
              ? true
              : (Array.isArray(configTypes)
                  ? configTypes.length > 0
                  : Object.keys(configTypes).length > 0),
            hasMaster: components.some((component: any) => component.is_master),
            hasNonMastersWithCustomAssignment: components.some((component: any) =>
              !component.is_master
              && !component.is_client
              && component.cardinality !== "ALL",
            ),
            hasSlave: components.some((component: any) =>
              component.is_slave || component.component_category === "SLAVE",
            ),
          };
        });
        combineCoSelectedServices(transformedData);

        const sortedServices = Object.keys(transformedData)
          .sort((a, b) => displayOrder.indexOf(a) - displayOrder.indexOf(b))
          .reduce((acc, key) => {
            acc[key] = transformedData[key];
            return acc;
          }, {} as { [key: string]: Service });

        if (!currentRequest) return;
        let nextServices = sortedServices;
        let nextManagedDependencies = hasFreshHBaseSelection(nextServices)
          ? localDependencyDefaults(restoredStep.managedDependencies || {})
          : {};
        let nextAutoSelected = hasFreshHBaseSelection(nextServices)
          ? restoredStep.autoSelectedLocalServices || []
          : [];

        // Consume only this tab's verified Add Service launch intent.
        if (wizardName === "addService" && isEmpty(restoredServices)) {
          const preselectedService = consumeAddServiceSelectionIntent({
            clusterId: cluster?.cluster_id,
            principal: loginName,
          });
          
          if (preselectedService && nextServices[preselectedService]) {
            const updatedServices = cloneDeep(nextServices);
            updatedServices[preselectedService].selected = true;
            
            // Also select any co-selected services
            if (coSelectedServices[preselectedService]) {
              for (const coSelectedService of coSelectedServices[preselectedService]) {
                if (updatedServices[coSelectedService]) {
                  updatedServices[coSelectedService].selected = true;
                }
              }
            }
            if (preselectedService === "HBASE" && !updatedServices.HBASE?.installed) {
              nextManagedDependencies = localDependencyDefaults({});
              nextAutoSelected = [];
              dependencyTypes.forEach((dependencyType) => {
                if (updatedServices[dependencyType] && !updatedServices[dependencyType].selected) {
                  updatedServices[dependencyType].selected = true;
                  if (!updatedServices[dependencyType].installed) {
                    nextAutoSelected.push(dependencyType);
                  }
                }
              });
            }
            nextServices = updatedServices;
          }
        }
        updateServices(nextServices);
        updateManagedDependencies(nextManagedDependencies);
        updateAutoSelectedLocalServices(nextAutoSelected);
      } catch (error: any) {
        if (currentRequest) {
          setCatalogError(String(
            error?.response?.data?.message
            || error?.message
            || t("installer.step4.catalogLoadFailed"),
          ));
        }
      } finally {
        if (currentRequest) setCatalogLoading(false);
      }
    };
    if (!serviceContextLoading && stack && version) void fetchServicesData();
    return () => {
      currentRequest = false;
    };
  }, [
    catalogAttempt,
    serviceContextLoading,
    stack,
    version,
    installedServices.join("\u0000"),
  ]);


  useEffect(() => {
    const isNextDisabled = () => {
      if (wizardName === "addService") {
        if (
          Object.values(services).filter((service) => service.selected === true)
            .length === installedServices.length
        ) {
          return true;
        }
      }
      return (
        Object.values(services).filter((service) => service.selected === true)
          .length === 0
        || (hasFreshHBaseSelection(services) && dependencyTypes.some((dependencyType) =>
          !choiceCanContinue(
            managedDependencies[dependencyType],
            Boolean(services[dependencyType]?.selected),
          )))
      );
    };

    setNextDisabled(isNextDisabled());
  }, [managedDependencies, services]);

  const fileSystemColumns = [
    {
      header: " ",
      cell: ({ row }: any) => {
        const checkboxId = `filesystem-step4-checkbox-${row.original.serviceName}`;
        return (
          <Form.Check
            type="checkbox"
            id={checkboxId}
            checked={row.original.selected}
            onChange={() => handleCheckboxChange(row.original.serviceName)}
          />
        );
      },
      width: "5%",
    },
    {
      header: "Service",
      accessorKey: "displayName",
      width: "20%",
      cell: ({ row }: any) => {
        return (
          <span
            className="cursor-pointer"
            onClick={() => handleCheckboxChange(row.original.serviceName)}
          >
            {row.original.displayName}
          </span>
        );
      },
    },
    {
      header: "Version",
      accessorKey: "version",
      width: "10%",
    },
    {
      header: "Description",
      accessorKey: "comments",
      width: "65%",
    },
  ];

  const servicesColumns = [
    {
      header: () => (
        <Form.Check
          type="checkbox"
          id="select-all-services-step4"
          checked={isAllServicesSelected()}
          onChange={selectAllServices}
        />
      ),
      id: "selectAllCheckcbox",
      cell: ({ row }: any) => {
        const checkboxId = `service-step4-checkbox-${row.original.serviceName}`;
        return (
          <Form.Check
            type="checkbox"
            id={checkboxId}
            checked={row.original.selected}
            onChange={() => {
              if (row.original.canToggle) {
                handleCheckboxChange(row.original.serviceName);
              }
            }}
          />
        );
      },
      width: "5%",
    },
    {
      header: "Service",
      accessorKey: "displayName",
      width: "20%",
      cell: ({ row }: any) => {
        return (
          <span
            className="cursor-pointer"
            onClick={() => {
              if (row.original.canToggle) {
                handleCheckboxChange(row.original.serviceName);
              }
            }}
          >
            {row.original.displayName}
          </span>
        );
      },
    },
    {
      header: "Version",
      accessorKey: "version",
      width: "10%",
    },
    {
      header: "Description",
      accessorKey: "comments",
      width: "65%",
    },
  ];

  const renderModalTitle = (modalType: string, serviceName: string) => {
    switch (modalType) {
      case ModalType.MISSING_DEPENDANT_SERVICE:
        return serviceName + " Needed";
      case ModalType.MISSING_SERVICE:
        return "Limited Functionality Warning";
      case ModalType.MISSING_FILE_SYSTEM:
        return "A Hadoop Compatible File System Needed";
      default:
        return null;
    }
  };

  const renderModalContent = (
    modalType: string,
    displayName: string,
    serviceName: string
  ) => {
    switch (modalType) {
      case ModalType.MISSING_DEPENDANT_SERVICE:
        return (
          <p>
            You did not select {displayName}, but it is needed by other services
            you selected. We will automatically add {displayName}. Is this OK?
          </p>
        );
      case ModalType.MISSING_SERVICE:
        return (
          <div>
            <p>{displayName}</p>
            <p>
              {warnningMessages[serviceName as keyof typeof warnningMessages]}
            </p>
          </div>
        );

      case ModalType.MISSING_FILE_SYSTEM:
        return (
          <p>
            You did not select a Hadoop Compatible File System, but it is needed
            by other services you selected. We will automatically add HDFS. Is
            this OK?
          </p>
        );
      default:
        return null;
    }
  };


  if (catalogLoading || serviceContextLoading) {
    return <Spinner />;
  }
  if (catalogError) {
    return (
      <Alert variant="danger">
        <div>{catalogError}</div>
        <Button
          className="mt-2"
          onClick={() => setCatalogAttempt((attempt) => attempt + 1)}
          size="sm"
          variant="outline-danger"
        >
          {t("common.retry")}
        </Button>
      </Alert>
    );
  }
  if (isEmpty(services)) {
    return <Alert variant="info">{t("installer.step4.noServices")}</Alert>;
  }

  return (
    <>
      <div>
        <div>
          <div className="step-title">Choose File System</div>
          <p className="step-description mt-1">
            Choose which file system you want to install on your cluster.
          </p>
          <Table
            data={Object.values(services).filter(
              (service) =>
                dfsServices.includes(service.serviceName) === true &&
                service.isHiddenOnDisplay === false
            )}
            columns={fileSystemColumns}
          />
          <h4 className="step-title">Choose Services</h4>
          <p className="step-description">
            Choose which services you want to install on your cluster.
          </p>
          <Table
            data={Object.values(services).filter(
              (service) =>
                dfsServices.includes(service.serviceName) === false &&
                service.isHiddenOnDisplay === false
            )}
            columns={servicesColumns}
          />
          {hasFreshHBaseSelection(services) && dependencyConsumer ? (
            <ManagedDependencySelector
              consumer={dependencyConsumer}
              onSelectionChange={changeManagedDependency}
              onPlanSelectionChange={changeManagedDependencyPlan}
              selections={localDependencyDefaults(managedDependencies)}
            />
          ) : null}
        </div>
        <div></div>
        {errorStack.length > 0 && (
          <>
            <MissingServiceModal
              isOpen={showModal}
              onClose={() => {
                if (errorStack[0].modalType === ModalType.MISSING_SERVICE) {
                  handleCloseLimitiedFunctionalityModal();
                  void saveServicesAndContinue();
                } else {
                  handleCloseAddServiceModal();
                }
              }}
              onCancel={() => setShowModal(false)}
              title={renderModalTitle(
                errorStack[0].modalType,
                errorStack[0].serviceName
              )}
              body={renderModalContent(
                errorStack[0].modalType,
                services[errorStack[0]?.serviceName]?.displayName,
                errorStack[0]?.serviceName
              )}
              modalType={errorStack[0].modalType}
            />
          </>
        )}
      </div>
      <WizardFooter
        step={currentStep}
        lifted
        onNext={() => {
          validateSelectedServices();
        }}
        onCancel={() => flushStateToDb("cancel")}
        onBack={async () => {
          await Promise.resolve(flushStateToDb("back"));
          handleBackImperitive();
        }}
        isNextEnabled={!nextDisabled}
      />
    </>
  );
}
