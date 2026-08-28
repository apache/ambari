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
import { AppContext } from "../../../../store/context";
import { cloneDeep, find, get, isEmpty } from "lodash";
import { AddObserverNamenodeContext } from "./store/context";
import { getStepData } from "../../../../Utils/Utility";
import { addObserverNamenodeSteps } from "./wizardSteps";
import Spinner from "../../../../components/Spinner";
import {
  Accordion,
  Alert,
  Badge,
  Card,
  CardBody,
  Col,
  Form,
  FormControl,
  Row,
} from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import useConfigsTags from "../../../../hooks/useConfigsTags";
import ConfigsApi from "../../../../api/configsApi";
import { reconfigureSites } from "../../../../Utils/taskUtils";
import { messages } from "../../../messages";
import { observerNnProperties } from "./observer_nn_properties";

function Step3() {
  const {
    state,
    dispatch,
    stepWizardUtilities: { currentStep, handleNextImperitive },
    flushStateToDb,
  } = useContext(AddObserverNamenodeContext);
  const { clusterName } = useContext(AppContext);
  const serverConfigDataRef = useRef<any>([]);
  const stepConfigs = useRef<any>(null);
  const { configsData } = useConfigsTags();
  const [overridenProperties, setOverridenProperties] = useState({});
  const [isNextEnabled] = useState(true);
  const [, forceRender] = useState(0);

  function prepareDependencies() {
    const ret: any = {};
    const configsFromServer = serverConfigDataRef.current.items;

    const nameNodes = getStepData(
      state,
      addObserverNamenodeSteps.SELECT_HOSTS,
      "masterComponentHosts",
      "addObserverNamenodeSteps"
    ).filter((host: any) => host.component === "NAMENODE");

    const hdfsSiteConfigs =
      find(configsFromServer, ["type", "hdfs-site"])?.properties || {};

    // Namespace id: prefer the nameservice chosen in Step1, else fall back to
    // dfs.nameservices from configs (first entry for federated clusters).
    const selectedNameServiceId = getStepData(
      state,
      addObserverNamenodeSteps.GET_STARTED,
      "nameServiceId",
      "addObserverNamenodeSteps"
    );
    ret.namespaceId =
      selectedNameServiceId ||
      (hdfsSiteConfigs["dfs.nameservices"] || "").split(",")[0];

    // Existing namenodes list for this nameservice
    const existingListRaw =
      hdfsSiteConfigs[`dfs.ha.namenodes.${ret.namespaceId}`] || "";
    const existingList = existingListRaw
      .split(",")
      .map((nn: string) => nn.trim())
      .filter((nn: string) => nn);

    // New namenode index continues from existing count (nn1,nn2 -> nn3)
    ret.newNamenodeIndex = `nn${existingList.length + 1}`;
    ret.listNameNodes = existingList.concat(ret.newNamenodeIndex).join(",");

    // The host of the newly-selected (not yet installed) NameNode
    ret.newNameNode = nameNodes.filter(
      (node: any) => node.isInstalled === false
    )[0]?.hostName;

    if (ret.newNameNode === undefined) {
      ret.newNameNode = "false";
    }

    // Ports (fall back to Ember defaults)
    const dfsRpcA = hdfsSiteConfigs["dfs.namenode.rpc-address"];
    ret.nnRpcPort = dfsRpcA ? dfsRpcA.split(":")[1] : "8020";

    const dfsHttpA = hdfsSiteConfigs["dfs.namenode.http-address"];
    ret.nnHttpPort = dfsHttpA ? dfsHttpA.split(":")[1] : "50070";

    const dfsHttpsA = hdfsSiteConfigs["dfs.namenode.https-address"];
    ret.nnHttpsPort = dfsHttpsA ? dfsHttpsA.split(":")[1] : "50470";

    return ret;
  }

  function tweakServiceConfigs(configs: any) {
    const dependencies = prepareDependencies();
    const result: any[] = [];

    configs.forEach((config: any) => {
      const clone = { ...config };
      clone.isOverridable = false;

      const replaceTokens = (input: string) =>
        input.replace(/\{\{(\w+)\}\}/g, (match: string, key: string) => {
          return dependencies[key] !== undefined && dependencies[key] !== null
            ? dependencies[key]
            : match;
        });

      clone.name = replaceTokens(clone.name);
      clone.displayName = replaceTokens(clone.displayName);
      clone.value = replaceTokens(clone.value);
      clone.recommendedValue = replaceTokens(clone.recommendedValue);
      clone.changedValue = clone.value;

      result.push(clone);
    });

    return result;
  }

  function renderServiceConfigs(_serviceConfig: any) {
    const serviceConfig: any = {
      serviceName: _serviceConfig.serviceName,
      displayName: _serviceConfig.displayName,
      configCategories: [],
      showConfig: true,
      configs: [],
    };

    _serviceConfig.configCategories.forEach((_configCategory: any) => {
      serviceConfig.configCategories.push(_configCategory);
    });

    _serviceConfig.configs.forEach((_serviceConfigProperty: any) => {
      serviceConfig.configs.push({
        ..._serviceConfigProperty,
        isEditable: _serviceConfigProperty.isReconfigurable,
      });
    });

    stepConfigs.current = serviceConfig;
  }

  const loadStep = async () => {
    await loadConfigsTags();
  };

  const loadConfigsTags = async () => {
    try {
      const configsTagApiResponsedata = await ConfigsApi.loadConfigTags(
        clusterName
      );
      if (JSON.stringify(configsTagApiResponsedata) !== "{}") {
        await onLoadConfigsTags(configsTagApiResponsedata);
      }
    } catch (error) {
      console.error("Error loading config tags", error);
    }
  };

  async function onLoadConfigsTags(data: any) {
    const urlParams = `(type=hdfs-site&tag=${data.Clusters.desired_configs["hdfs-site"].tag})`;
    try {
      const configsByTagApiResponseData = await ConfigsApi.getConfigsByTags(
        clusterName,
        urlParams
      );
      onLoadConfigs(configsByTagApiResponseData);
    } catch (error) {
      console.error("Error loading configurations", error);
    }
  }

  const onLoadConfigs = (configsResponse: any) => {
    serverConfigDataRef.current = configsResponse;

    let observerConfigProperties = observerNnProperties().observerNnConfig.configs;
    const tweakedConfigs = tweakServiceConfigs(observerConfigProperties);
    observerConfigProperties = tweakedConfigs;

    // Build overridden properties (server configs + observer overrides) for Step4 save
    const overridenPropertiesCopy = cloneDeep(configsResponse);
    for (const siteConfig of observerConfigProperties) {
      const site = get(siteConfig, "filename", "");
      const correspondingSite = find(
        overridenPropertiesCopy.items,
        (item: any) => item.type === site
      );
      if (correspondingSite) {
        correspondingSite.properties = {
          ...correspondingSite.properties,
          [(siteConfig as any).name]:
            (siteConfig as any).changedValue || (siteConfig as any).value,
        };
      }
    }
    setOverridenProperties(overridenPropertiesCopy);

    const modifiedConfig = {
      ...observerNnProperties().observerNnConfig,
      configs: observerConfigProperties,
    };
    renderServiceConfigs(modifiedConfig);
    forceRender((n) => n + 1);
  };

  useEffect(() => {
    if (configsData && !isEmpty(configsData)) {
      loadStep();
    }
  }, [configsData]);

  function getMastersInfo() {
    const step2Data = getStepData(
      state,
      addObserverNamenodeSteps.SELECT_HOSTS,
      "masterComponentHosts",
      "addObserverNamenodeSteps"
    );

    const currentNameNodes = step2Data.filter(
      (host: any) => host.component === "NAMENODE" && host.isInstalled
    );

    const additionalNameNodes = step2Data.filter(
      (host: any) => host.component === "NAMENODE" && !host.isInstalled
    );

    return { currentNameNodes, additionalNameNodes };
  }

  const { currentNameNodes, additionalNameNodes } = getMastersInfo();

  if (!stepConfigs.current) {
    return (
      <div className="d-flex justify-content-center align-items-center p-5">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <h2 className="step-title">Review</h2>
      <h3 className="step-description light-text">
        Confirm your host selections.
      </h3>

      <Card className="mt-3">
        <CardBody>
          {currentNameNodes.map((node: any, index: number) => (
            <Row key={`current-${index}`} className="mb-2">
              <Col md={3} className="bolder">
                Current NameNode:
              </Col>
              <Col md={9}>{node.hostName}</Col>
            </Row>
          ))}

          {additionalNameNodes.map((node: any, index: number) => (
            <Row key={`additional-${index}`} className="mb-2">
              <Col md={3} className="bolder">
                Observer NameNode:
              </Col>
              <Col md={9}>
                {node.hostName ? node.hostName : "false"}{" "}
                <Badge bg="success">TO BE INSTALLED</Badge>
              </Col>
            </Row>
          ))}
        </CardBody>
      </Card>

      <Alert variant="info" className="mt-3">
        <div className="bolder mb-2">Review Configuration Changes.</div>
        <div>
          The following lists the configuration changes that will be made by the
          Wizard to add the Observer NameNode. This information is for{" "}
          <strong>review only</strong> and is not editable.
        </div>
      </Alert>

      <Accordion defaultActiveKey="0" className="mt-3">
        {stepConfigs.current?.configCategories?.map(
          (category: any, categoryIndex: number) => {
            const categoryConfigs = stepConfigs.current?.configs.filter(
              (config: any) => config.category === category.name
            );
            if (!categoryConfigs?.length) return null;

            return (
              <Accordion.Item
                eventKey={categoryIndex.toString()}
                key={categoryIndex}
              >
                <Accordion.Header>{category.displayName}</Accordion.Header>
                <Accordion.Body>
                  <Form>
                    {categoryConfigs.map((config: any, index: number) => (
                      <Form.Group as={Row} key={index} className="mb-3">
                        <Form.Label
                          column
                          sm={4}
                          className="text-break pe-3"
                          style={{
                            wordWrap: "break-word",
                            overflowWrap: "break-word",
                          }}
                        >
                          {config.displayName}
                        </Form.Label>
                        <Col sm={8}>
                          <FormControl
                            type="text"
                            value={config.changedValue || ""}
                            disabled={!config.isEditable}
                            readOnly
                          />
                        </Col>
                      </Form.Group>
                    ))}
                  </Form>
                </Accordion.Body>
              </Accordion.Item>
            );
          }
        )}
      </Accordion>

      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onBack={() => {
          flushStateToDb("back");
        }}
        onNext={async () => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                overridenProperties,
              },
            },
          });
          // Apply the hdfs-site config changes here, mirroring the Ember Observer
          // NameNode wizard which saves configs in step3 (not as a step4 task).
          try {
            const note = get(
              messages,
              "admin.observerNameNode.wizard.step4.save.configuration.note"
            );
            const configs = [
              {
                Clusters: {
                  desired_config: reconfigureSites(
                    ["hdfs-site"],
                    overridenProperties,
                    note
                  ),
                },
              },
            ];
            await ConfigsApi.serviceMultiConfigurations(clusterName, configs);
          } catch (error) {
            console.error("Error saving observer namenode configs", error);
          }
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
