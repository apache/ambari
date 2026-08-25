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
import { ChooseServicesApi } from "../../api/chooseServicesApi";
import Table from "../../components/Table";
import { cloneDeep, forEach, get, isEmpty, map } from "lodash";
import { Form } from "react-bootstrap";
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
};

type ErrorType = {
  serviceName: string;
  modalType: string;
};

export default function Step4({ wizardName = "clusterCreation" }) {
  const [, setServicesFromApi] = useState<any>([]);
  const [services, setServices] = useState<{ [key: string]: Service }>({});
  const [errorStack, setErrorStack] = useState<ErrorType[]>([]);
  const [showModal, setShowModal] = useState<boolean>(false);
  const [nextDisabled, setNextDisabled] = useState<boolean>(false);
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    serviceContextLoading = false,
    handleBackImperitive,
    installedServices: installedServicesProps = [],
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  }: any = useContext(Context);
  const { services: servicesContext } = useContext(AppContext);
  let installedServices = installedServicesProps;
  if (wizardName !== "clusterCreation") {
    installedServices = map(servicesContext, "ServiceInfo.service_name");
  }
  const stepData = getStepData(state, currentStep.name, "");

  const versionStepData = get(state, `${wizardName}Steps.VERSION.data`, {});
  const version = get(versionStepData, "selectedVersion.stack_version", "");
  const stack = get(versionStepData, "selectedStack.stack_name", "");

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
    setServices(updatedServices);
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
    setServices(updatedServices);
  };

  const saveServicesAndContinue = async () => {
    const flow = deriveAddServiceFlow(services);
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: { services, addServiceFlow: flow },
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
      selectedService?.required?.forEach((requiredService) => {
        dependantServiceValidation(
          selectedServices,
          requiredService,
          newErrorStack
        );
      });
    }

    serviceValidation("RANGER", newErrorStack);
    serviceValidation("AMBARI_METRICS", newErrorStack);

    setErrorStack(newErrorStack);
    if (newErrorStack.length > 0) {
      setShowModal(true);
    } else {
      void saveServicesAndContinue();
    }
  };

  const dependantServiceValidation = (
    selectedServices: Service[],
    requiredService: string,
    errorStackCopy: ErrorType[]
  ) => {
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
    if (selectedFileSystems.length === 0) {
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
    const fetchServicesData = async () => {
      try {
        const chooseServices = await ChooseServicesApi.getServices(
          stack,
          version
        );
        const transformedData: { [key: string]: any } = {};
        const installableServices = filterInstallableStackServices(
          get(chooseServices, "items", []),
        );
        installableServices.forEach((service: any) => {
          const components = get(service, "components", []).map(
            (component: any) => component.StackServiceComponents || {},
          );
          const configTypes = get(service, "StackServices.config_types");
          transformedData[service.StackServices.service_name] = {
            displayName: service.StackServices.display_name,
            serviceName: service.StackServices.service_name,
            serviceType: service.StackServices.service_type,
            version: service.StackServices.service_version,
            comments: service.StackServices.comments,
            selected: isServiceSelected(service.StackServices.service_name),
            required: service.StackServices.required_services,
            isIgnored: false,
            installed: installedServices.includes(
              service.StackServices.service_name
            ),
            canToggle: canToggleServiceSelection(
              service.StackServices.service_name
            ),
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

        setServicesFromApi({ ...chooseServices, items: installableServices });
        setServices(sortedServices);
        
        // Handle pre-selection from localStorage (for Add Service from Stack and Versions page)
        if (wizardName === "addService") {
          const preselectedService = localStorage.getItem('preselectedService');
          
          if (preselectedService && sortedServices[preselectedService]) {
            const updatedServices = cloneDeep(sortedServices);
            updatedServices[preselectedService].selected = true;
            
            // Also select any co-selected services
            if (coSelectedServices[preselectedService]) {
              for (const coSelectedService of coSelectedServices[preselectedService]) {
                if (updatedServices[coSelectedService]) {
                  updatedServices[coSelectedService].selected = true;
                }
              }
            }
            
            setServices(updatedServices);
            
            // Clear the localStorage item after using it
            localStorage.removeItem('preselectedService');
          }
        }
      } catch (error) {
        console.error("Error fetching services data:", error);
      }
    };
    if (!stepData.services&&stack&&version) fetchServicesData();
  }, [serviceContextLoading,stack,version]);


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
      );
    };

    setNextDisabled(isNextDisabled());
  }, [services]);

  useEffect(() => {
    console.log("Step Data is", stepData);
    if (!isEmpty(stepData)) {
      setServices(stepData.services);
    }
  }, []);

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


  if (isEmpty(services)) {
    return <Spinner />;
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
        onCancel={() => void flushStateToDb("cancel")}
        onBack={async () => {
          await Promise.resolve(flushStateToDb("back"));
          handleBackImperitive();
        }}
        isNextEnabled={!nextDisabled}
      />
    </>
  );
}
