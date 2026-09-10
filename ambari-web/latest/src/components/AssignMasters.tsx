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

import { useEffect, useReducer, useRef, useState } from "react";
import { Row, Col, Form, Card, Button, CardBody, Alert } from "react-bootstrap";
import { ChooseServicesApi } from "../api/chooseServicesApi";
import AssignMastersApi from "../api/assignMastersApi";
import { Utility } from "../Utils/Utility";
import { misc } from "../Utils/misc";
import Spinner from "./Spinner";
import { filter, get, map, uniq } from "lodash";
import Select from "react-select";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMinus, faPlus } from "@fortawesome/free-solid-svg-icons";
import { blueprintUtils } from "../screens/ClusterWizard/utils";
import { maxToInstall, isMultipleAllowed } from "../screens/Hosts/utils";
import { AssignMastersProps, Host, Masters, State, Action, ServicesResponse } from "../screens/ClusterWizard/types/AssignMastersTypes";
import {
  getMasterValidationMessages,
  masterValidationKey,
  MasterValidationMessages,
} from "../screens/ClusterWizard/masterValidation";
import {
  hasManagedAdvisorDependency,
  ManagedDependencyAdvisorReviewRequiredError,
  stackAdvisorNeedsProviderReview,
  type PreparedStackAdvisorRequest,
} from "../screens/ClusterWizard/managedDependencyAdvisor";
import { responseErrorMessage } from "../Utils/httpError";
import { useTranslation } from "react-i18next";

const initialState: State = {
  hosts: {},
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "SET_HOSTS_DATA":
      return {
        ...state,
        hosts: action.payload,
      };
    case "UPDATE_COMPONENT_HOST": {
      const { component, oldHost, newHost } = action.payload;
      const updatedHosts = { ...state.hosts };

      if (oldHost) {
        updatedHosts[oldHost].components = updatedHosts[
          oldHost
        ].components.filter((c) => c !== component);
      }

      // Add component to new host if newHost is not null and it doesn't already exist
      if (newHost && !updatedHosts[newHost].components.includes(component)) {
        updatedHosts[newHost].components.push(component);
        // Note: We don't sort here as the sorting will be handled in the data preparation phase
        // The components array in state.hosts is just for tracking which components are on which hosts
      }

      return {
        ...state,
        hosts: updatedHosts,
      };
    }
    default:
      return state;
  }
}

export default function AssignMasters({
  STACK,
  VERSION,
  hostsList,
  services,
  dispatch: dispatchParent,
  installedServices,
  parentState,
  advisorInputKey,
  setCanProceed,
  setHasValidationIssues = () => {},
  onReviewManagedDependencies,
  runWithAdvisorRequest,
}: AssignMastersProps) {
  const { t } = useTranslation();
  const restoredData = get(
    parentState,
    `clusterCreationSteps.MASTERS.data`,
    {},
  );
  const canRestoreAdvice = Boolean(
    advisorInputKey
      && restoredData.advisorInputKey === advisorInputKey
      && Object.keys(restoredData.state?.hosts || {}).length > 0,
  );
  const restoredState = canRestoreAdvice ? restoredData.state : initialState;
  const [state, dispatch] = useReducer(reducer, restoredState);
  const [loading, setLoading] = useState(false);
  const [mastersData, setMastersData] = useState<Masters[]>([]);
  const [servicesAndComponents, setServicesAndComponents] = useState<ServicesResponse | null>(null);
  const [validationMessages, setValidationMessages] =
    useState<MasterValidationMessages>({});
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [loadFailure, setLoadFailure] = useState<{
    kind: "recommendation" | "validation";
    message: string;
    reviewRequired: boolean;
  } | null>(null);
  const validationSequence = useRef(0);
  const requestSequence = useRef(0);
  const advisorRequestRef = useRef<PreparedStackAdvisorRequest | null>(null);
  const notMasters = ["MYSQL_SERVER", "HIVE_SERVER_INTERACTIVE"];

  useEffect(() => {
    const sequence = ++requestSequence.current;
    const isCurrent = (prepared?: PreparedStackAdvisorRequest | null) =>
      sequence === requestSequence.current && (!prepared || prepared.isCurrent());

    async function getMastersData() {
      setLoading(true);
      setLoadFailure(null);
      setCanProceed(false);
      let prepared: PreparedStackAdvisorRequest | null = null;
      try {
        if (canRestoreAdvice) {
          if (!runWithAdvisorRequest) {
            advisorRequestRef.current = { isCurrent: () => true, properties: {} };
          }
          const servicesAndComponentsData = await ChooseServicesApi.getServices(
            STACK,
            VERSION,
          );
          if (!isCurrent()) return;
          setServicesAndComponents(servicesAndComponentsData);
          return;
        }
        const cpuResponse = await AssignMastersApi.getCpuInfo(hostsList);
        if (!isCurrent()) return;
        const hostnames = cpuResponse.data.items.map(
          (item: any) => item.Hosts.host_name
        );
        const requestRecommendations = async (
          currentPrepared: PreparedStackAdvisorRequest,
        ) => {
          prepared = currentPrepared;
          advisorRequestRef.current = currentPrepared;
          if (!isCurrent(currentPrepared)) return null;
          const recommendationsResponse1 =
            await AssignMastersApi.postRecommendations(
              {
                ...Utility.recommendationPayload(
                  hostnames,
                  "host_groups",
                  [],
                  [],
                  services,
                ),
                ...currentPrepared.properties,
              },
              STACK,
              VERSION,
            );
          if (!isCurrent(currentPrepared)) return null;
          const firstBlueprintClusterBinding =
            recommendationsResponse1.resources[0].recommendations
              .blueprint_cluster_binding.host_groups;
          const firstBlueprint =
            recommendationsResponse1.resources[0].recommendations.blueprint
              .host_groups;
          const response = await AssignMastersApi.postRecommendations(
            {
              ...Utility.recommendationPayload(
                hostnames,
                "host_groups",
                firstBlueprint,
                firstBlueprintClusterBinding,
                services,
              ),
              ...currentPrepared.properties,
            },
            STACK,
            VERSION,
          );
          return isCurrent(currentPrepared) ? response : null;
        };
        const recommendationsResponse2 = runWithAdvisorRequest
          ? await runWithAdvisorRequest(requestRecommendations)
          : await requestRecommendations({ isCurrent: () => true, properties: {} });
        if (!recommendationsResponse2 || !isCurrent(prepared)) return;

        const blueprintClusterBinding =
          recommendationsResponse2.resources[0].recommendations
            .blueprint_cluster_binding.host_groups;
        const blueprint =
          recommendationsResponse2.resources[0].recommendations.blueprint
            .host_groups;
        const hostsData = blueprintClusterBinding.reduce(
          (acc: { [key: string]: Host }, hostGroup: any) => {
            const hostname = hostGroup.hosts[0].fqdn;
            const components = blueprint
              .find((group: any) => group.name === hostGroup.name)
              .components.map((component: any) => component.name);
            acc[hostname] = { hostname, cores: 0, memory: 0, components };
            return acc;
          },
          {},
        );

        if (!hasManagedAdvisorDependency(prepared, "ZOOKEEPER")) {
          Object.keys(hostsData).forEach((hostname) => {
            if (!hostsData[hostname].components.includes("ZOOKEEPER_SERVER")) {
              hostsData[hostname].components.push("ZOOKEEPER_SERVER");
            }
          });
        }

        cpuResponse.data.items.forEach((item: any) => {
          const hostname = item.Hosts.host_name;
          if (hostsData[hostname]) {
            hostsData[hostname].cores = item.Hosts.cpu_count;
            hostsData[hostname].memory = item.Hosts.total_mem;
          }
        });

        const sortedHostsData: { [key: string]: Host } = {};
        Object.keys(hostsData).sort().forEach((hostname) => {
          sortedHostsData[hostname] = hostsData[hostname];
        });

        const servicesAndComponentsData: ServicesResponse =
          await ChooseServicesApi.getServices(STACK, VERSION);
        if (!isCurrent(prepared)) return;
        setServicesAndComponents(servicesAndComponentsData);

        const assignedComponents = new Set<string>();
        Object.keys(hostsData).forEach((hostname) => {
          hostsData[hostname].components = hostsData[hostname].components.filter(
            (component: any) => {
              if (notMasters.includes(component)) return false;
              const serviceComponent = servicesAndComponentsData.items
                .flatMap((service: any) => service.components)
                .find(
                  (comp: any) => get(
                    comp,
                    "StackServiceComponents.component_name",
                  ) === component,
                );
              if (!serviceComponent
                || !get(serviceComponent, "StackServiceComponents.is_master")) {
                return false;
              }
              const stackComponent = get(serviceComponent, "StackServiceComponents");
              if (isMasterAddableInstallerWizard(stackComponent)) return true;
              if (!assignedComponents.has(component)) {
                assignedComponents.add(component);
                return true;
              }
              return false;
            },
          );
        });

        if (!isCurrent(prepared)) return;
        dispatch({ type: "SET_HOSTS_DATA", payload: sortedHostsData });
        dispatchParent({
          hostsData: sortedHostsData,
          mastersData: getTransformedMastersData(mastersData),
          state,
          advisorInputKey,
        });
      } catch (error: any) {
        if (!isCurrent(prepared)) return;
        advisorRequestRef.current = null;
        setLoadFailure({
          kind: "recommendation",
          message: error instanceof ManagedDependencyAdvisorReviewRequiredError
            ? t("managedDependencies.advisorReviewRequired")
            : responseErrorMessage(error, t("managedDependencies.advisorFailed")),
          reviewRequired: error instanceof ManagedDependencyAdvisorReviewRequiredError
            || stackAdvisorNeedsProviderReview(error)
            || hasManagedAdvisorDependency(prepared, "HDFS")
            || hasManagedAdvisorDependency(prepared, "ZOOKEEPER"),
        });
      } finally {
        if (isCurrent(prepared)) setLoading(false);
      }
    }
    void getMastersData();
    return () => {
      requestSequence.current += 1;
      validationSequence.current += 1;
      advisorRequestRef.current = null;
    };
  }, [loadAttempt]);
  
  useEffect(() => {
    const transformedMastersData = getTransformedMastersData(mastersData);
    dispatchParent({
      mastersData: transformedMastersData,
      state,
      advisorInputKey,
    });
    validateChange(transformedMastersData)
  }, [mastersData]);

  const isMasterAddableOnlyOnHA = (component: any) => {
    return ["NAMENODE", "RESOURCEMANAGER", "RANGER_ADMIN"].includes(
      get(component, "component_name", "")
    );
  }

  const isNotAddableOnlyInInstall = (component: any) => {
    return [
      "HIVE_METASTORE",
      "HIVE_SERVER",
      "RANGER_KMS_SERVER",
      "OOZIE_SERVER",
      "TIMELINE_READER",
      "YARN_REGISTRY_DNS",
    ].includes(get(component, "component_name", ""));
  };

  const isMasterAddableInstallerWizard = (component: any) => {
    return (
      get(component, "is_master", false) &&
      isMultipleAllowed(component) &&
      !isMasterAddableOnlyOnHA(component) &&
      !isNotAddableOnlyInInstall(component)
    );
  };

  const validateChange = async (transformedMastersData: any) => {
    const sequence = ++validationSequence.current;
    if (!transformedMastersData.length) {
      setCanProceed(false);
      return;
    }
    setCanProceed(false);
    let prepared = advisorRequestRef.current;
    try {
      const allHostnames = map(transformedMastersData, "host_name");
      const hostnames = uniq(allHostnames);
      const requestValidation = async (
        currentPrepared: PreparedStackAdvisorRequest,
      ) => {
        prepared = currentPrepared;
        advisorRequestRef.current = currentPrepared;
        if (sequence !== validationSequence.current || !currentPrepared.isCurrent()) {
          return null;
        }
        return AssignMastersApi.postValidations(
          {
            hosts: hostnames,
            services,
            validate: "host_groups",
            recommendations: getValidationRequestBody(transformedMastersData),
            ...currentPrepared.properties,
          },
          STACK,
          VERSION,
        );
      };
      const validationResponse = runWithAdvisorRequest
        ? await runWithAdvisorRequest(requestValidation)
        : prepared
          ? await requestValidation(prepared)
          : null;
      if (!validationResponse || !prepared) return;
      if (sequence !== validationSequence.current || !prepared.isCurrent()) return;
      updateValidationsSuccessCallback(validationResponse);
    } catch (error) {
      if (sequence !== validationSequence.current
        || (prepared && !prepared.isCurrent())) return;
      setValidationMessages({});
      setHasValidationIssues(false);
      setCanProceed(false);
      setLoadFailure({
        kind: "validation",
        message: responseErrorMessage(error, t("managedDependencies.advisorFailed")),
        reviewRequired: stackAdvisorNeedsProviderReview(error)
          || hasManagedAdvisorDependency(prepared, "HDFS")
            || hasManagedAdvisorDependency(prepared, "ZOOKEEPER"),
      });
    }
  };

  const getValidationRequestBody = (transformedMastersData: any) => {
    const allHostnames = map(transformedMastersData, "host_name");
    const hostnames = uniq(allHostnames);
    const masterBlueprint = blueprintUtils.getBlueprint(
      hostnames,
      getSelectedMastersGroupedMapping(transformedMastersData)
    );

    return masterBlueprint;
  };

  const getSelectedMastersGroupedMapping = (transformedMastersData: any) => {
    const hostComponentMapping: any = [];
    transformedMastersData.forEach((selectedMaster: any) => {
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

  const getTransformedMastersData = (mastersDataToBeTransformed: any) => {
    const allHostnames = map(mastersDataToBeTransformed, "hostName");
    const uniqueHostnames = uniq(allHostnames);
    const transformedMasterMapping = [];
    for (let hostname of uniqueHostnames) {
      const hostObj = {
        host_name: hostname,
        masterServices: [],
      };
      const matchingServicesForHost = filter(mastersDataToBeTransformed, [
        "hostName",
        hostname,
      ]);
      hostObj.masterServices = matchingServicesForHost as any;
      transformedMasterMapping.push(hostObj);
    }
    return transformedMasterMapping;
  };

  useEffect(() => {
    function setMasterComponentsData() {
      // Only proceed if we have cached services data
      if (!servicesAndComponents) {
        return;
      }

      const mastersData: Masters[] = Object.keys(state.hosts).flatMap(
        (hostname, index) =>
          state.hosts[hostname].components
          .filter(component => !notMasters.includes(component))
          .map((component) => {
            const serviceComponent = servicesAndComponents.items
              .flatMap((service: any) => service.components)
              .find(
                (comp: any) =>
                  get(comp, "StackServiceComponents.component_name") ===
                  component
              );
            return {
              display_name: get(
                serviceComponent,
                "StackServiceComponents.display_name"
              ),
              component: component,
              serviceId: get(
                serviceComponent,
                "StackServiceComponents.service_name"
              ),
              isInstalled: installedServices?.includes(
                get(serviceComponent, "StackServiceComponents.service_name")
              ),
              host_id: index + 1,
              hostName: hostname,
            };
          })
      );
      
      // Sort all mastersData by display_name to ensure consistent ordering
      const sortedMastersData = mastersData.sort((a, b) => 
        (a.display_name || a.component).localeCompare(b.display_name || b.component)
      );
      
      dispatchParent({
        mastersData: getTransformedMastersData(sortedMastersData),
        state,
        advisorInputKey,
      });
      setMastersData(sortedMastersData);
    }
    setMasterComponentsData();
  }, [state.hosts, servicesAndComponents]);

  const handleComponentChange = (
    component: string,
    oldHost: string,
    newHost: string
  ) => {
    if (oldHost !== newHost) {
      dispatch({
        type: "UPDATE_COMPONENT_HOST",
        payload: { component, oldHost, newHost, state },
      });
    }
  };
  const getMaxNumberOfMasters = (componentName: string) => {
    if (!servicesAndComponents) return 1;
    
    const serviceComponent = servicesAndComponents.items
      .flatMap((service: any) => service.components)
      .find(
        (comp: any) =>
          get(comp, "StackServiceComponents.component_name") === componentName
      );

    if (!serviceComponent) return 1;

    const stackComponent = get(serviceComponent, "StackServiceComponents");
    const maxToInstallValue = maxToInstall(stackComponent);
    const hostsNumber = Object.keys(state.hosts).length;
    
    return Math.min(maxToInstallValue, hostsNumber);
  };

  const handleAddComponent = (component: string) => {
    const maxNumMasters = getMaxNumberOfMasters(component);
    const currentMasters = mastersData.filter(master => master.component === component);
    
    if (currentMasters.length >= maxNumMasters) {
      return false;
    }

    // Find the first host that doesn't already have this component
    const availableHost = Object.keys(state.hosts).find((hostname) => {
      return !get(state.hosts[hostname], "components", []).includes(
        component as never
      );
    });

    if (availableHost) {
      dispatch({
        type: "UPDATE_COMPONENT_HOST",
        payload: { component, oldHost: null, newHost: availableHost, state },
      });
      return true;
    }
    return false;
  };

  const handleRemoveComponent = (component: string, hostname: string) => {
    const currentMasters = mastersData.filter(master => master.component === component);
    
    // Don't allow removal if only one instance exists
    if (currentMasters.length <= 1) {
      return false;
    }

    dispatch({
      type: "UPDATE_COMPONENT_HOST",
      payload: { component, oldHost: hostname, newHost: null, state },
    });
    return true;
  };

  /**
   * Process validation response
   */
  const updateValidationsSuccessCallback = (data: any) => {
    const messages = getMasterValidationMessages(data, mastersData);
    setLoadFailure((failure) => failure?.kind === "validation" ? null : failure);
    setValidationMessages(messages);
    setHasValidationIssues(Object.keys(messages).length > 0);
    setCanProceed(true);
  };

  const retryFailure = () => {
    if (loadFailure?.kind === "validation") {
      setLoadFailure(null);
      void validateChange(getTransformedMastersData(mastersData));
      return;
    }
    setLoadAttempt((attempt) => attempt + 1);
  };

  return (
    <>
      <div className="step-title">Assign Masters</div>
      <p className="step-description">
        Assign master components to hosts you want to run them on.
      </p>
      {loadFailure ? (
        <Alert variant="danger">
          <div>{loadFailure.message}</div>
          <div className="d-flex flex-wrap gap-2 mt-2">
            <Button
              size="sm"
              variant="outline-danger"
              onClick={retryFailure}
            >
              {t("common.retry")}
            </Button>
            {loadFailure.reviewRequired && onReviewManagedDependencies ? (
              <Button
                size="sm"
                variant="outline-danger"
                onClick={onReviewManagedDependencies}
              >
                {t("managedDependencies.reviewProviderSettings")}
              </Button>
            ) : null}
          </div>
        </Alert>
      ) : null}
      
      {loading ? (
        <Spinner />
      ) : loadFailure?.kind !== "recommendation" ? (
        <Card>
          <CardBody>
            <Row>
              <Col md={8}>
                {/* Render components using pre-sorted mastersData */}
                {mastersData
                  // Filter out installed components
                  .filter((master) => !master.isInstalled)
                  // Render each component
                  .map((master) => {
                    const { component, hostName: hostname, display_name } = master;
                    const { errorMessage, warnMessage } =
                      validationMessages[
                        masterValidationKey(hostname, component)
                      ] || {};
                    const displayName = display_name || component;
                    
                    return (
                      <div key={`${hostname}-${component}`}>
                        <Row className="mb-3">
                          <Col xs={4} className="text-end mt-3">
                            <Form.Label className="fw-100">
                              {displayName}:
                            </Form.Label>
                          </Col>
                          <Col xs={4}>
                            <Select
                              id={`select-${component}`}
                              value={{ label: hostname, value: hostname }}
                              onChange={(selectedOption) => {
                                if (selectedOption) {
                                  handleComponentChange(
                                    component,
                                    hostname,
                                    selectedOption.value
                                  );
                                }
                              }}
                              options={Object.keys(state.hosts)
                                .filter(
                                  (host) =>
                                    host === hostname ||
                                    !get(
                                      state.hosts[host],
                                      "components",
                                      []
                                    ).includes(component as never)
                                )
                                .map((host) => ({
                                  label: host,
                                  value: host,
                                }))}
                              className="w-100"
                            />
                          </Col>
                          <Col xs={4} className="d-flex align-items-center">
                            {(() => {
                              // Get the service component to check if it's addable in installer wizard
                              const serviceComponent = servicesAndComponents?.items
                                .flatMap((service: any) => service.components)
                                .find(
                                  (comp: any) =>
                                    get(comp, "StackServiceComponents.component_name") === component
                                );

                              if (!serviceComponent) return null;
                              const stackComponent = get(serviceComponent, "StackServiceComponents");
                              const isAddableInInstallerWizard = isMasterAddableInstallerWizard(stackComponent);
                              if (!isAddableInInstallerWizard) {
                                return null;
                              }

                              const componentCount = Object.keys(state.hosts).filter(hostname => 
                                state.hosts[hostname].components.includes(component)
                              ).length;
                              
                              const totalHosts = Object.keys(state.hosts).length;
                              
                              // Logic based on Ember.js implementation:
                              // 1. Show add control if current count < max allowed masters
                              // 2. Show remove control if current count > 1 (for non-installed components)
                              // 3. Respect the maxToInstall cardinality from stack definition
                              const maxNumMasters = getMaxNumberOfMasters(component);
                              let showPlusButton = false;
                              let showMinusButton = false;
                              
                              // Show add button if we haven't reached the maximum
                              if (componentCount < maxNumMasters) {
                                showPlusButton = true;
                              }
                              
                              // Show remove button if we have more than 1 instance
                              if (componentCount > 1) {
                                showMinusButton = true;
                              }
                              
                              return (
                                <div className="d-flex gap-1">
                                  {showPlusButton && (
                                    <Button
                                      variant="success"
                                      size="sm"
                                      className="h15"
                                      onClick={() => handleAddComponent(component)}
                                      title={`Add ${component} (${componentCount}/${totalHosts})`}
                                    >
                                      <FontAwesomeIcon icon={faPlus} />
                                    </Button>
                                  )}
                                  {showMinusButton && (
                                    <Button
                                      variant="secondary"
                                      size="sm"
                                      onClick={() => handleRemoveComponent(component, hostname)}
                                      title={`Remove ${component} (${componentCount}/${totalHosts})`}
                                    >
                                      <FontAwesomeIcon icon={faMinus} />
                                    </Button>
                                  )}
                                </div>
                              );
                            })()}
                          </Col>
                        </Row>
                        {/* Display validation messages */}
                        {errorMessage && (
                          <Row className="mb-2">
                            <Col xs={12}>
                              <Alert variant="danger" className="py-2 small">
                                <strong>Error:</strong> {errorMessage}
                              </Alert>
                            </Col>
                          </Row>
                        )}
                        {warnMessage && (
                          <Row className="mb-2">
                            <Col xs={12}>
                              <Alert variant="warning" className="py-2 small">
                                <strong>Warning:</strong> {warnMessage}
                              </Alert>
                            </Col>
                          </Row>
                        )}
                      </div>
                    );
                  })}
              </Col>

              <Col md={4}>
                {Object.keys(state.hosts).map((hostname) => (
                  <Card key={hostname} className="mb-3">
                    <Card.Body>
                      <Card.Title className="text-nowrap text-truncate small">
                        {hostname}({" "}
                        {misc.formatBandwidth(state.hosts[hostname].memory)},{" "}
                        {state.hosts[hostname].cores} cores)
                      </Card.Title>
                      <Card.Text>
                        {/* Display components sorted alphabetically by display name */}
                        {(() => {
                          // Create display name mapping
                          const displayNameMap: { [key: string]: string } = {};
                          
                          // Get display names for each component
                          state.hosts[hostname].components.forEach(comp => {
                            const master = mastersData.find(m => 
                              m.component === comp && m.hostName === hostname
                            );
                            displayNameMap[comp] = master?.display_name || comp;
                          });
                          
                          // Sort components alphabetically by display name
                          return [...state.hosts[hostname].components]
                            .sort((a, b) => 
                              (displayNameMap[a] || a).localeCompare(displayNameMap[b] || b)
                            )
                            .map(comp => (
                              <Button
                                key={comp}
                                size="sm"
                                variant="success"
                                className="me-1 mb-1 small"
                              >
                                {displayNameMap[comp] || comp}
                              </Button>
                            ));
                        })()}
                      </Card.Text>
                    </Card.Body>
                  </Card>
                ))}
              </Col>
            </Row>
          </CardBody>
        </Card>
      ) : null}
    </>
  );
}
