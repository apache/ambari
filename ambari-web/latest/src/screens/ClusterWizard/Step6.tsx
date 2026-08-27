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

import { Alert, Card, Form, Table } from "react-bootstrap";
import classNames from "classnames";
import { cloneDeep, get, map } from "lodash";
import { useContext, useEffect, useState } from "react";
import ValidationsApi from "../../api/validations";
import Paginator from "../../components/Paginator";
import usePagination from "../../hooks/usePagination";
import {
  blueprintUtils,
  isShownOnInstallerSlaveClientPage,
  minToInstall,
} from "./utils";
import useServiceComponents from "./hooks/useServiceComponents";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./clusterStore/types";
import { ContextWrapper } from ".";
import {
  nextAddServiceStep,
  previousAddServiceStep,
} from "../Services/AddServiceWizard/addServiceNavigation";
import { getStepData } from "../../Utils/Utility";
import modalManager from "../../store/ModalManager";
import Modal from "../../components/Modal";
import { AppContext } from "../../store/context";
import { HostsApi } from "../../api/hostsApi";

enum SelectOperations {
  SELECT = "SELECT",
  UNSELECT = "UNSELECT",
}

type Step6Props = {
  wizardName?: string;
};

function Step6({ wizardName = "clusterCreation" }: Step6Props) {
  const { Context } = useContext(ContextWrapper);
  const { clusterName = "" } = useContext(AppContext);
  const [validationErrors, setValidationErrors] = useState<any[]>([]);
  const {
    dispatch,
    state,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
      jumpToStep,
    },
  }: any = useContext(Context);
  const initialServiceComponents = get(
    state,
    `${wizardName}Steps.SLAVES_AND_CLIENTS.data.serviceComponents`,
    {}
  );

  const {
    serviceComponents,
    setServiceComponents,
    STACK,
    VERSION,
    services,
    hosts,
    getClientComponents,
    allServiceComponentsList,
    ComponentCategory,
  } = useServiceComponents(wizardName, initialServiceComponents);

  const [nextEnabled, setNextEnabled] = useState(false);
  const addServiceFlow = get(
    state,
    "addServiceSteps.SERVICES.data.addServiceFlow",
    {},
  );

  const enableNext = () => {
    setNextEnabled(true);
  };

  const {
    currentItems,
    changePage,
    currentPage,
    maxPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(serviceComponents);

  const allHostsSelectedForComponent = (component: string) => {
    let allHostsSelected = true;
    for (const serviceComponent of serviceComponents) {
      const hostComponent = serviceComponent.checkboxes.find((com: any) => {
        return com.label === component;
      });
      if (!hostComponent || !hostComponent.checked) {
        allHostsSelected = false;
      }
    }
    return allHostsSelected;
  };

  const isNoHostSelectedForComponent = (component: string) => {
    let noHostSelected = true;
    for (const serviceComponent of serviceComponents) {
      const hostComponent = serviceComponent.checkboxes.find((com: any) => {
        return com.label === component;
      });
      if (hostComponent && hostComponent.checked) {
        noHostSelected = false;
      }
    }
    return noHostSelected;
  };

  const toggleComponent = (hostname: string, component: string) => {
    const serviceComponentsCopy = cloneDeep(serviceComponents);
    const host = serviceComponentsCopy.find(
      (serviceComponent: any) => serviceComponent.hostname === hostname
    );
    if (host) {
      const hostComponent = host.checkboxes.find((comp: any) => {
        return comp.label === component;
      });
      if (hostComponent) {
        hostComponent.checked = !hostComponent.checked;
      }
    }
    setServiceComponents(serviceComponentsCopy);
  };

  const handleHostSelectionForComponent = (
    operation: SelectOperations,
    componentName: string
  ) => {
    const serviceComponentsCopy = cloneDeep(serviceComponents);
    for (const serviceHostComponent of serviceComponentsCopy) {
      const serviceComponent = serviceHostComponent.checkboxes.find(
        (hostComponent: any) => {
          return hostComponent.label === componentName;
        }
      );
      if (serviceComponent) {
        serviceComponent.checked =
          operation === SelectOperations.SELECT ? true : false;
      }
    }
    setServiceComponents(serviceComponentsCopy);
  };

  const getSlaveBlueprint = () => {
    const clientComponents = getClientComponents();
    let currentHostComponentMapping: any = [];
    for (let host of Object.keys(hosts)) {
      const correspondingHostMapping = serviceComponents.find((sc: any) => {
        return sc.hostname === host;
      });
      if (correspondingHostMapping) {
        const selectedComponents = correspondingHostMapping.checkboxes
          .filter((cb: any) => {
            return cb.checked && cb.label !== "CLIENT";
          })
          ?.map((comp: any) => {
            return { name: comp.label };
          });
        const isClientChecked = !!correspondingHostMapping.checkboxes.find(
          (selectedComponent: any) =>
            selectedComponent.label === "CLIENT" && selectedComponent.checked
        );
        if (isClientChecked) {
          currentHostComponentMapping.push({
            hostname: host,
            components: [
              ...selectedComponents,
              ...clientComponents.map((component: any) => {
                return {
                  name: component,
                };
              }),
            ],
          } as never);
        } else {
          currentHostComponentMapping.push({
            hostname: host,
            components: [...selectedComponents],
          } as never);
        }
      }
    }
    const slaveBlueprint = blueprintUtils.getBlueprint(
      Object.keys(hosts),
      currentHostComponentMapping
    );
    return slaveBlueprint;
  };

  const getInvisibleSlaveAndClients = () => {
    return allServiceComponentsList.filter((serviceComponent: any) => {
      if (
        (serviceComponent["component_category"] === ComponentCategory.SLAVE &&
          !isShownOnInstallerSlaveClientPage(serviceComponent)) ||
        (serviceComponent["component_category"] === ComponentCategory.CLIENT &&
          minToInstall(serviceComponent.cardinality) === Infinity)
      ) {
        return serviceComponent;
      }
    });
  };

  const getSelectedMastersGroupedMapping = () => {
    const mastersData = getStepData(
      state,
      "MASTERS",
      "mastersData",
      `${wizardName}Steps`
    );
    const hostComponentMapping: any = [];
    mastersData.forEach((selectedMaster: any) => {
      hostComponentMapping.push({
        hostname: selectedMaster.host_name,
        components: selectedMaster.masterServices.map(
          (selectedComponent: any) => {
            return {
              name: selectedComponent.component,
            };
          }
        ),
      });
    });
    return hostComponentMapping;
  };

  const getValidationRequestBody = async () => {
    const slaveBlueprint = getSlaveBlueprint();
    let hostnames = Object.keys(hosts);
    //@ts-ignore
    const invisibleSlavesAndClients = map(
      getInvisibleSlaveAndClients(),
      "component_name"
    );
    
    let masterBlueprint;
    
    // For Add Host wizard: include ALL existing hosts in validation
    // This matches Ember logic where hostNames are concatenated with existing hosts
    if (wizardName === "addHost") {
      // Fetch all existing hosts and their components from the cluster
      const existingHostsData = await HostsApi.getHostComponentsDetails(
        clusterName,
        "fields=Hosts/host_name,host_components/HostRoles/component_name"
      );
      
      // Build blueprint for existing hosts with their installed components
      const existingHostsMapping = existingHostsData.items.map((item: any) => {
        return {
          hostname: item.Hosts.host_name,
          components: item.host_components.map((component: any) => {
            return {
              name: component.HostRoles.component_name,
            };
          }),
        };
      });
      
      // Get all existing host names
      const existingHostNames = existingHostsData.items.map(
        (item: any) => item.Hosts.host_name
      );
      
      // Combine new hosts with existing hosts for validation
      const allHostNames = [...new Set([...hostnames, ...existingHostNames])];
      
      // Create blueprint with existing components
      masterBlueprint = blueprintUtils.getBlueprint(
        allHostNames,
        existingHostsMapping
      );
      
      const mergedBlueprints = blueprintUtils.mergeBlueprints(
        cloneDeep(slaveBlueprint),
        cloneDeep(masterBlueprint)
      );
      return mergedBlueprints;
    } else {
      // For other wizards: use the master component mapping from step 5
      masterBlueprint = blueprintUtils.getBlueprint(
        hostnames,
        getSelectedMastersGroupedMapping()
      );
      const mergedBlueprints = blueprintUtils.mergeBlueprints(
        cloneDeep(slaveBlueprint),
        cloneDeep(masterBlueprint)
      );
      return mergedBlueprints;
    }
  };

  const getServicesBeingAdded = () => {
    const servicesData = getStepData(state, "SERVICES", "services", `${wizardName}Steps`) || {};
    
    return Object.keys(servicesData).filter((serviceName) => {
      const service = servicesData[serviceName];
      return service.selected === true && service.installed === false && service.isIgnored === false;
    });
  };

  const validateChange = async () => {
    try {
      const validationRequestBody = await getValidationRequestBody();
      
      // For Add Host wizard, include all cluster hosts (existing + new)
      let allHosts = Object.keys(hosts);
      if (wizardName === "addHost") {
        const existingHostsData = await HostsApi.getHostComponentsDetails(
          clusterName,
          "fields=Hosts/host_name"
        );
        const existingHostNames = existingHostsData.items.map(
          (item: any) => item.Hosts.host_name
        );
        allHosts = [...new Set([...allHosts, ...existingHostNames])];
      }
      
      const validationResponse = await ValidationsApi.validateMapping(
        STACK,
        VERSION,
        {
          hosts: allHosts,
          services,
          validate: "host_groups",
          recommendations: validationRequestBody,
        }
      );
      //Based on validation response set this
      const { resources } = validationResponse;
      const [validationItems] = resources;
      const allErrors = validationItems?.items || [];
      
      // Filter errors to only show those related to services being added in this wizard
      // Get list of service names being added (not already installed)
      const servicesBeingAdded = wizardName === "addService" 
        ? getServicesBeingAdded() || []
        : services;
      
      // Get all component names for services being added
      const componentsOfServicesBeingAdded = allServiceComponentsList
        .filter((comp: any) => servicesBeingAdded.includes(comp.service_name))
        .map((comp: any) => comp.component_name);
      
      // Components that are automatically added (not shown as checkboxes)
      // These should be filtered out from validation errors
      const autoAddedComponents: string[] = [];
      if (services.includes("AMBARI_METRICS")) {
        autoAddedComponents.push("METRICS_MONITOR");
      }
      if (services.includes("KERBEROS")) {
        autoAddedComponents.push("KERBEROS_CLIENT");
      }
      
      // Filter validation errors to only include those for components of services being added
      // Also filter out master components (Step6 is only for slaves and clients)
      const filteredErrors = allErrors.filter((error: any) => {
        const componentName = error['component-name'];
        const errorType = error.type;
        
        // Only process host-component type errors
        if (errorType !== 'host-component') {
          return false;
        }
        
        // Filter out auto-added components (METRICS_MONITOR, KERBEROS_CLIENT, etc.)
        if (componentName && autoAddedComponents.includes(componentName)) {
          return false;
        }
        
        // Filter out master components - Step6 only deals with slaves and clients
        if (componentName) {
          const component = allServiceComponentsList.find(
            (comp: any) => comp.component_name === componentName
          );
          // Skip if it's a master component
          if (component && component.component_category === 'MASTER') {
            return false;
          }
        }
        
        // Only show errors for components of services being added
        return !componentName || componentsOfServicesBeingAdded.includes(componentName);
      });
      
      setValidationErrors(filteredErrors);

      // Enable next but show warning if there are validation errors
      if (filteredErrors && filteredErrors.length > 0) {
        setNextEnabled(true); // Still allow proceeding but with warning
      } else {
        enableNext();
      }
    } finally {
      enableNext();
    }
  };

  const getAdditionalServiceComponents = () => {
    const changedComponents = cloneDeep(serviceComponents);
    for (const serviceComponent of changedComponents) {
      if (services.includes("AMBARI_METRICS")) {
        serviceComponent.checkboxes.push({
          label: "METRICS_MONITOR",
          checked: true,
          isDisabled: false,
        });
      }
      if (services.includes("KERBEROS")) {
        serviceComponent.checkboxes.push({
          label: "KERBEROS_CLIENT",
          checked: true,
          isDisabled: false,
        });
      }
    }
    return changedComponents;
  };

  const saveAssignmentsAndContinue = async () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: {
          serviceComponents:
            wizardName === "addHost"
              ? getAdditionalServiceComponents()
              : serviceComponents,
          allServiceComponentsList,
        },
      },
    });
    if (wizardName === "addService") {
      const nextStep = nextAddServiceStep(3, addServiceFlow);
      await Promise.resolve(flushStateToDb("jump", nextStep));
      jumpToStep(nextStep);
    } else {
      await Promise.resolve(flushStateToDb("next"));
      handleNextImperitive();
    }
  };

  const showValidationIssuesModal = () => {
    if (validationErrors.length > 0) {
      modalManager.show(
        <Modal
          isOpen={true}
          modalTitle="Validation Issues"
          modalBody={
            <div>
              Slave and Client component assignments have issues that need attention.
            </div>
          }
          options={{
            okButtonText: "CONTINUE ANYWAY",
            okButtonVariant: "danger",
            cancelButtonText: "Cancel",
            cancelableViaBtn: true,
            cancelableViaIcon: true,
          }}
          successCallback={() => {
            modalManager.hide();
            void saveAssignmentsAndContinue();
          }}
          onClose={() => {
            modalManager.hide();
          }}
        />
      );
    } else {
      void saveAssignmentsAndContinue();
    }
  };

  useEffect(() => {
    validateChange();
  }, [serviceComponents]);

  return (
    <>
      <div>
        <div className="step-title">Assign Slaves and Clients</div>
        <div className="d-flex flex-column step-description">
          <small className="light-text step-description">
            Assign slave and client components to hosts you want to run them on.
          </small>
          <small className="light-text step-description">
            Hosts that are assigned master components are shown with{" "}
            <span className="text-info">✵</span>.
          </small>
        </div>
        {validationErrors.length > 0 && (
          <Alert variant="warning" className="mt-3">
            <p>Assignment of slave and client components has the following issues:</p>
            <ul className="mb-0">
              {validationErrors.map((error: any, index: number) => {
                const message = error.message || error['display-text'] || 'Validation issue detected';
                return (
                  <li key={index} className="fs-12">
                    {message}
                  </li>
                );
              })}
            </ul>
          </Alert>
        )}
        <Card className="mt-4">
          <Card.Body>
            <Table
              responsive
              hover
              id="component_assign_table"
              className="mw-100 narrow-table"
              style={{ overflowX: "scroll" }}
            >
              <thead>
                <tr>
                  <th>Hostname</th>

                  {serviceComponents?.[0]?.checkboxes?.map(
                    (serviceComponent: any) => {
                      return (
                        <th>
                          <div className="d-flex">
                            <div
                              className={classNames(
                                "cursor-pointer fw-bolder",
                                {
                                  "text-info": !allHostsSelectedForComponent(
                                    serviceComponent.label
                                  ),
                                  "text-secondary":
                                    allHostsSelectedForComponent(
                                      serviceComponent.label
                                    ),
                                }
                              )}
                              onClick={() => {
                                if (!serviceComponent.isDisabled) {
                                  handleHostSelectionForComponent(
                                    SelectOperations.SELECT,
                                    serviceComponent.label
                                  );
                                }
                              }}
                            >
                              all
                            </div>
                            <div className="mx-1">|</div>
                            <div
                              className={classNames(
                                "cursor-pointer fw-bolder",
                                {
                                  cu: !isNoHostSelectedForComponent(
                                    serviceComponent.label
                                  ),
                                  "text-secondary":
                                    isNoHostSelectedForComponent(
                                      serviceComponent.label
                                    ),
                                }
                              )}
                              onClick={() => {
                                if (!serviceComponent.isDisabled) {
                                  handleHostSelectionForComponent(
                                    SelectOperations.UNSELECT,
                                    serviceComponent.label
                                  );
                                }
                              }}
                            >
                              none
                            </div>
                          </div>
                        </th>
                      );
                    }
                  )}
                </tr>
              </thead>
              <tbody>
                {currentItems?.map((serviceComponent: any) => {
                  return (
                    <tr>
                      <td>{serviceComponent.hostname}</td>
                      {serviceComponent.checkboxes.map((checkbox: any) => {
                        return (
                          <td>
                            <Form.Check
                              id={`${serviceComponent.hostname}-${checkbox.label}`}
                              className="d-flex align-items-center"
                              // disabled={checkbox.isDisabled}
                            >
                              <Form.Check.Input
                                id={`${serviceComponent.hostname}-${checkbox.label}-input`}
                                disabled={checkbox.isDisabled}
                                onChange={() => {
                                  toggleComponent(
                                    serviceComponent.hostname,
                                    checkbox.label
                                  );
                                }}
                                checked={checkbox.checked}
                              />
                              <Form.Check.Label 
                                className="mt-1 ms-1"
                                htmlFor={`${serviceComponent.hostname}-${checkbox.label}-input`}
                              >
                                {checkbox.label}
                              </Form.Check.Label>
                            </Form.Check>
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </Table>
            <Paginator
              currentPage={currentPage}
              maxPage={maxPage}
              changePage={changePage}
              itemsPerPage={itemsPerPage}
              setItemsPerPage={setItemsPerPage}
              totalItems={serviceComponents?.length}
            />
          </Card.Body>
        </Card>
      </div>
      <WizardFooter
        isNextEnabled={nextEnabled}
        onNext={showValidationIssuesModal}
        onCancel={() => flushStateToDb("cancel")}
        onBack={async () => {
          if (wizardName === "addService") {
            const previousStep = previousAddServiceStep(3, addServiceFlow);
            await Promise.resolve(flushStateToDb("jump", previousStep));
            jumpToStep(previousStep);
          } else {
            await Promise.resolve(flushStateToDb("back"));
            handleBackImperitive();
          }
        }}
        step={currentStep}
      />
    </>
  );
}

export default Step6;
