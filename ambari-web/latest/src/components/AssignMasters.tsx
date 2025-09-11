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

import { useEffect, useReducer, useState } from "react";
import { Row, Col, Form, Card, Button, CardBody } from "react-bootstrap";
import { Utility } from "../Utils/Utility.ts";
import { misc } from "../Utils/misc.ts";
import Spinner from "./Spinner.tsx";
import _, { filter, get, map, uniq } from "lodash";
import Select from "react-select";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMinus, faPlus } from "@fortawesome/free-solid-svg-icons";
import AssignMastersApi from "../api/AssignMastersApi.ts";
import { ServicesResponse } from "../screens/ClusterWizard/types/StackServiceComponent.ts";
import { ChooseServicesApi } from "../api/chooseServicesApi.ts";

interface Host {
  hostname: string;
  cores: number;
  memory: number;
  components: string[];
}

interface Masters {
  display_name: string;
  component: string;
  serviceId: string;
  host_id: number;
  hostName: string;
  isInstalled?: boolean;
}

interface State {
  hosts: { [key: string]: Host };
}

interface Action {
  type: string;
  payload: any;
}

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
        const index = updatedHosts[newHost].components.indexOf(component);
        if (index !== -1) {
          updatedHosts[newHost].components.splice(index + 1, 0, component);
        } else {
          updatedHosts[newHost].components.push(component);
        }
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

interface AssignMastersProps {
  STACK: string;
  VERSION: string;
  superMasters: any;
  hostsList: any;
  services: string[];
  setCanProceed: (canProcced: boolean) => void;
  dispatch: any;
  installedServices?: string[];
  parentState?:any;
}

export default function AssignMasters({
  STACK,
  VERSION,
  superMasters,
  hostsList,
  services,
  dispatch: dispatchParent,
  installedServices,
  parentState,
  setCanProceed,
}: AssignMastersProps) {
  const [state, dispatch] = useReducer(reducer, get(
      parentState,
      `clusterCreationSteps.MASTERS.data.state`,
      undefined
    ) || initialState);
  const [loading, setLoading] = useState(false);
  const [mastersData, setMastersData] = useState<Masters[]>([]);
  const notMasters = ["MYSQL_SERVER", "HIVE_SERVER_INTERACTIVE"];

  useEffect(() => {
    async function getMastersData() {
      setLoading(true);

      const cpuResponse = await AssignMastersApi.getCpuInfo(hostsList);
      const hostnames = cpuResponse.data.items.map(
        (item: any) => item.Hosts.host_name
      );

      const recommendationPayload1 = Utility.recommendationPayload(
        hostnames,
        "host_groups",
        [],
        [],
        services
      );

      const recommendationsResponse1 =
        await AssignMastersApi.postRecommendations(
          recommendationPayload1,
          STACK,
          VERSION
        );

      const firstBlueprintClusterBinding =
        recommendationsResponse1.resources[0].recommendations
          .blueprint_cluster_binding.host_groups;
      const firstBlueprint =
        recommendationsResponse1.resources[0].recommendations.blueprint
          .host_groups;

      const recommendationPayload2 = Utility.recommendationPayload(
        hostnames,
        "host_groups",
        firstBlueprint,
        firstBlueprintClusterBinding,
        services
      );

      const recommendationsResponse2 =
        await AssignMastersApi.postRecommendations(
          recommendationPayload2,
          STACK,
          VERSION
        );
      const processRecommendations = (response: any) => {
        const blueprintClusterBinding =
          response.resources[0].recommendations.blueprint_cluster_binding
            .host_groups;
        const blueprint =
          response.resources[0].recommendations.blueprint.host_groups;

        return blueprintClusterBinding.reduce(
          (acc: { [key: string]: Host }, hostGroup: any) => {
            const hostname = hostGroup.hosts[0].fqdn;
            const components = blueprint
              .find((group: any) => group.name === hostGroup.name)
              .components.map((component: any) => component.name);
            acc[hostname] = {
              hostname,
              cores: 0,
              memory: 0,
              components,
            };
            return acc;
          },
          {}
        );
      };

      const hostsData = processRecommendations(recommendationsResponse2);
      // Add ZOOKEEPER_SERVER to all hosts by default
      Object.keys(hostsData).forEach((hostname) => {
        if (!hostsData[hostname].components.includes("ZOOKEEPER_SERVER")) {
          hostsData[hostname].components.push("ZOOKEEPER_SERVER");
        }
      });
      
      cpuResponse.data.items.forEach((item: any) => {
        const hostname = item.Hosts.host_name;
        if (hostsData[hostname]) {
          hostsData[hostname].cores = item.Hosts.cpu_count;
          hostsData[hostname].memory = item.Hosts.total_mem;
        }
      });

      // validations.
      const validationPayload = {
        hosts: hostnames,
        validate: "host_groups",
        recommendations: {
          blueprint: {
            configurations: null,
            host_groups: firstBlueprint,
          },
          blueprint_cluster_binding: {
            host_groups: firstBlueprintClusterBinding,
          },
        },
        services: services,
      };
      AssignMastersApi.postValidations(validationPayload, STACK, VERSION);
      //TODO: Check for validations response.

      setCanProceed(true);
      const servicesAndComponents: ServicesResponse =
        await ChooseServicesApi.getServices(STACK, VERSION);

      // Filter components based on is_master and ensure no duplicates on a single host
      // except for superMaster components like ZOOKEEPER_SERVER which can be on multiple hosts
      const assignedComponents = new Set<string>();
      Object.keys(hostsData).forEach((hostname) => {
        hostsData[hostname].components = hostsData[hostname].components.filter(
          (component: any) => {
            if(notMasters.includes(component)) {
              return false;
            }
            const serviceComponent = servicesAndComponents.items
              .flatMap((service: any) => service.components)
              .find(
                (comp: any) =>
                  _.get(comp, "StackServiceComponents.component_name") ===
                  component
              );

            // Allow superMaster components on multiple hosts
            if (superMasters.includes(component)) {
              return serviceComponent && 
                _.get(serviceComponent, "StackServiceComponents.is_master");
            }

            if (
              serviceComponent &&
              _.get(serviceComponent, "StackServiceComponents.is_master") &&
              !assignedComponents.has(component)
            ) {
              assignedComponents.add(component);
              return true;
            }
            return false;
          }
        );
      });

      dispatch({ type: "SET_HOSTS_DATA", payload: hostsData });
      dispatchParent({
        hostsData,
        mastersData: getTransformedMastersData(mastersData),
        state,
      });
      setLoading(false);
    }
    getMastersData();
  }, []);
  
  useEffect(() => {
    dispatchParent({
      mastersData: getTransformedMastersData(mastersData),
      state,
    });
  }, [mastersData]);

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
    async function setMasterComponentsData() {
      const servicesAndComponents: ServicesResponse =
        await ChooseServicesApi.getServices(STACK, VERSION);

      
      const mastersData: Masters[] = Object.keys(state.hosts).flatMap(
        (hostname, index) =>
          state.hosts[hostname].components
          .filter(component => !notMasters.includes(component))
          .map((component) => {
            const serviceComponent = servicesAndComponents.items
              .flatMap((service: any) => service.components)
              .find(
                (comp: any) =>
                  _.get(comp, "StackServiceComponents.component_name") ===
                  component
              );
            return {
              display_name: _.get(
                serviceComponent,
                "StackServiceComponents.display_name"
              ),
              component: component,
              serviceId: _.get(
                serviceComponent,
                "StackServiceComponents.service_name"
              ),
              isInstalled: installedServices?.includes(
                _.get(serviceComponent, "StackServiceComponents.service_name")
              ),
              host_id: index + 1,
              hostName: hostname,
            };
          })
          // Sort components alphabetically by display_name
          .sort((a, b) => (a.display_name || '').localeCompare(b.display_name || ''))
      );
      dispatchParent({
        mastersData: getTransformedMastersData(mastersData),
        state,
      });
      setMastersData(mastersData);
    }
    setMasterComponentsData();
  }, [state.hosts]);

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

  const handleAddComponent = (component: string) => {
    // Find the first host that doesn't already have this component
    const availableHost = Object.keys(state.hosts).find((hostname) => {
      return !_.get(state.hosts[hostname], "components", []).includes(
        component as never
      );
    });

    if (availableHost) {
      dispatch({
        type: "UPDATE_COMPONENT_HOST",
        payload: { component, oldHost: null, newHost: availableHost, state },
      });
    }
  };

  const handleRemoveComponent = (component: string, hostname: string) => {
    dispatch({
      type: "UPDATE_COMPONENT_HOST",
      payload: { component, oldHost: hostname, newHost: null, state },
    });
  };


  return (
    <>
      <div className="step-title">Assign Masters</div>
      <p className="step-description">
        Assign master components to hosts you want to run them on.
      </p>
      {loading ? (
        <Spinner />
      ) : (
        <Card>
          <CardBody>
            <Row>
              <Col md={8}>
                {(() => {
                  // Get all components from all hosts
                  const allComponentsWithHosts: Array<{component: string, hostname: string}> = [];
                  
                  // Collect all components with their assigned hosts
                  Object.keys(state.hosts).forEach(hostname => {
                    state.hosts[hostname].components.forEach(component => {
                      allComponentsWithHosts.push({
                        component,
                        hostname
                      });
                    });
                  });
                  
                  // Get display names for all components
                  const componentDisplayNames: { [key: string]: string } = {};
                  allComponentsWithHosts.forEach(({component, hostname}) => {
                    const serviceComponent = mastersData.find(m => 
                      m.component === component && m.hostName === hostname
                    );
                    if (serviceComponent) {
                      // Use component as key to ensure uniqueness
                      const key = `${component}-${hostname}`;
                      componentDisplayNames[key] = serviceComponent.display_name || component;
                    }
                  });
                  
                  // Sort all components by their display names
                  return allComponentsWithHosts
                    // Filter out installed components
                    .filter(({component, hostname}) => {
                      const matchingMasterForInstall = mastersData.find(m => 
                        m.component === component && m.hostName === hostname && m.isInstalled
                      );
                      return !(matchingMasterForInstall && matchingMasterForInstall.isInstalled);
                    })
                    // Sort by display name
                    .sort((a, b) => {
                      const keyA = `${a.component}-${a.hostname}`;
                      const keyB = `${b.component}-${b.hostname}`;
                      const displayNameA = componentDisplayNames[keyA] || a.component;
                      const displayNameB = componentDisplayNames[keyB] || b.component;
                      return displayNameA.localeCompare(displayNameB);
                    })
                    // Render each component
                    .map(({component, hostname}) => {
                      const key = `${component}-${hostname}`;
                      const displayName = componentDisplayNames[key] || component;
                      
                      return (
                        <Row key={`${hostname}-${component}`} className="mb-3">
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
                                    !_.get(
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
                            {superMasters.includes(component) && (
                              <>
                                {Object.keys(state.hosts).some(
                                  (host) =>
                                    !state.hosts[host].components.includes(
                                      component
                                    )
                                ) ? (
                                  <Button
                                    variant="success"
                                    size="sm"
                                    onClick={() => handleAddComponent(component)}
                                  >
                                    <FontAwesomeIcon icon={faPlus} />
                                  </Button>
                                ) : (
                                  <Button
                                    variant="success"
                                    size="sm"
                                    onClick={() =>
                                      handleRemoveComponent(component, hostname)
                                    }
                                  >
                                    <FontAwesomeIcon icon={faMinus} />
                                  </Button>
                                )}
                              </>
                            )}
                          </Col>
                        </Row>
                      );
                    });
                })()}
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
      )}
    </>
  );
}
