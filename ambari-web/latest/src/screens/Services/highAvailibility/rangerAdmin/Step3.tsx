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

import { filter, find, get, has, isEmpty, map } from "lodash";
import { useContext, useEffect, useRef, useState } from "react";
import { AppContext } from "../../../../store/context";
import { wizardConfigs } from "./wizardConstants";
import ConfigsApi from "../../../../api/configsApi";
import { allServiceProperties, getStepData } from "../../../../Utils/Utility";
import { enableRangerAdminSteps } from "./wizardSteps";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { Accordion, Alert, Card, Col, Form, Row, Stack } from "react-bootstrap";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import Center from "../../../../components/Center";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";

function Step3() {
  const configs = useRef<any>([]);
  const configCategories = useRef<any>([]);
  const [serverConfigs, setServerConfigs] = useState([]);
  const { services, cluster } = useContext(AppContext);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const stack = get(cluster, "version", "").split("-")[0];
  const version = get(cluster, "version", "").split("-")[1];
  const [configsState, setConfigsState] = useState([]);
  const [configCategoriesState, setConfigCategoriesState] = useState([]);

  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityRangerAdminContext);

  const [selectedService, setSelectedService] = useState({});

  function getServiceForProperty(propertyName: string, siteName: string) {
    let serviceObj: any = {};
    for (const config of serverConfigs) {
      const serviceConfigs = get(config, "configurations");
      const matchingConfig = find(serviceConfigs, function (conf) {
        return (
          get(conf, "StackConfigurations.property_name") === propertyName &&
          get(conf, "StackConfigurations.type", "")?.split(".")?.[0] ===
            siteName
        );
      });
      if (matchingConfig) {
        serviceObj.service = get(config, "StackServices", {});
        serviceObj.config = get(matchingConfig, "StackConfigurations", {});
        break;
      }
    }
    return serviceObj;
  }
  console.log("State is", state);
  useEffect(() => {
    if (configs.current.length) {
      setConfigsState(configs.current);
    }
    if (configCategories.current.length) {
      setConfigCategoriesState(configCategories.current);
      setSelectedService(configCategories.current[0].name);
    }
  }, [configCategories.current.length, configs.current.length]);
  useEffect(() => {
    function getConfigsAndCategories() {
      wizardConfigs.forEach((config: any) => {
        const { service, config: serviceConfig } = getServiceForProperty(
          config.propertyName,
          config.siteName
        );
        if (!isEmpty(service)) {
          const serviceName = get(service, "service_name", "");
          const serviceDisplayName = get(service, "display_name", "");
          if (selectedServices.includes(serviceName)) {
            const property = {
              id: `${config.propertyName}__${
                serviceConfig.type?.split(".")?.[0]
              }`,
              name: serviceConfig.property_name,
              displayName: serviceConfig.property_display_name,
              fileName: serviceConfig.type,
              filename: serviceConfig.type,
              description: serviceConfig.property_description,
              value: serviceConfig.property_value,
              recommendedValue: "",
              serviceName: serviceName,
              stackName: stack,
              stackVersion: version,
              isOverridable: get(
                serviceConfig,
                "property_value_attributes.overridable",
                false
              ),
              displayType: get(
                serviceConfig,
                "property_value_attributes.type",
                "string"
              ),
              isRequired: true,
              isReconfigurable: true,
              isEditable: true,
              isFinal: serviceConfig.final,
              propertyDependsOn: serviceConfig.property_depends_on,
              valueAttributes: serviceConfig.property_value_attributes,
              category: find(allServiceProperties, [
                "name",
                config.propertyName,
              ])?.category,
            };
            if (!find(configCategories.current, ["name", serviceName])) {
              configCategories.current.push({
                name: serviceName,
                displayName: serviceDisplayName,
              });
            }
            if (!find(configs.current, ["name", property.name])) {
              configs.current.push({
                ...property,
                category: serviceName,
                value: getStepData(
                  state,
                  enableRangerAdminSteps.GET_STARTED,
                  "loadBalancerUrl",
                  "enableHighAvailibilityRangerAdminSteps"
                ),
                isEditable: false,
              });
            }
          }
        }
      });
    }
    if (serverConfigs.length) {
      getConfigsAndCategories();
    }
  }, [serverConfigs]);
  useEffect(() => {
    async function getServerConfigs() {
      const allServicesConfigs = await ConfigsApi.getConfigProperties(
        stack,
        version,
        selectedServices.join(",")
      );
      setServerConfigs(allServicesConfigs?.items);
    }
    getServerConfigs();
  }, []);
  function getMastersInfo() {
    const step2Data = getStepData(
      state,
      "SELECT_HOSTS",
      "masterComponentHosts",
      "enableHighAvailibilityRangerAdminSteps"
    );
    for (let masterComponent of step2Data) {
      if (!has(masterComponent, "serviceId") || !masterComponent.serviceId) {
        //@ts-ignore
        masterComponent.serviceId = "RANGER";
      }
      if (!has(masterComponent, "hostName") || !masterComponent.hostName) {
        masterComponent.hostName = masterComponent.selectedHost;
        masterComponent.component = masterComponent.component_name;
      }
    }
    const currentRangerAdmin = find(
      filter(step2Data, ["component", "RANGER_ADMIN"]),
      ["isInstalled", true]
    ).hostName;
    const addRangerAdmin = find(
      filter(step2Data, ["component", "RANGER_ADMIN"]),
      ["isInstalled", false]
    ).hostName;

    return {
      currentRangerAdmin,
      addRangerAdmin,
    };
  }

  return (
    <>
      <div className="step-title">Review</div>
      <div className="step-description my-2">Confirm your host selections.</div>
      <Card>
        <Card.Body>
          <div className="bg-light-subtle border p-3">
            <Row>
              <Col md={4}>
                <b className="fw-bolder">Current Ranger Admin:</b>
              </Col>
              <Col>{getMastersInfo().currentRangerAdmin}</Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">Additional Ranger Admin:</b>
              </Col>
              <Col>
                <Stack direction="horizontal">
                  {getMastersInfo().addRangerAdmin}
                  <Stack direction="horizontal" className="ms-2">
                    <FontAwesomeIcon icon={faPlus} className="text-success" />
                    <div className="text-success">TO BE INSTALLED</div>
                  </Stack>
                </Stack>
              </Col>
            </Row>
          </div>
          {!configsState.length ? (
            <Center>
              <Spinner />
            </Center>
          ) : (
            <>
              <Alert variant="info" className="mt-4">
                <Stack>
                  <div className="fw-bolder fs-12">
                    Review Configuration Changes.
                  </div>
                  <div className="fs-12 mt-2">
                    The following lists the configuration changes that will be
                    made by the Wizard to enable NameNode HA. This information
                    is for <span className="fw-bolder fs-12">review only</span>{" "}
                    and is not editable
                  </div>
                </Stack>
              </Alert>
              <Accordion
                defaultActiveKey={selectedService as any}
                className="mt-4"
                alwaysOpen
              >
                {configCategoriesState?.map((category: any, index: number) => {
                  return (
                    <Accordion.Item eventKey={category.name} key={index}>
                      <Accordion.Header>
                        {category.displayName}
                      </Accordion.Header>
                      <Accordion.Body>
                        {filter(configsState, ["category", category.name]).map(
                          (config: any) => {
                            return (
                              <Row
                                key={config.name}
                                className="mt-3 align-items-center"
                              >
                                <Col md={5}>
                                  <small>
                                    {config.displayName || config.name}
                                  </small>
                                </Col>
                                <Col>
                                  <Form.Control
                                    type="text"
                                    size="sm"
                                    value={config.value}
                                    disabled={!config.isEditable}
                                  ></Form.Control>
                                </Col>
                              </Row>
                            );
                          }
                        )}
                      </Accordion.Body>
                    </Accordion.Item>
                  );
                })}
              </Accordion>
            </>
          )}
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!!configsState.length}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(2);
        }}
        onNext={() => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                configsState,
              },
            },
          });
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step3;
