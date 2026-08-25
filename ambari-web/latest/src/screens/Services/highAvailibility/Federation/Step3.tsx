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
import {
  Accordion,
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  Row,
} from "react-bootstrap";
import { map } from "lodash";
import federationApi from "../../../../api/federationApi";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getStepData } from "../../../../Utils/Utility";
import { getHdfsNamespaces } from "../haWorkflowUtils";
import { EnableNamenodeFederationContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableNamenodeFederationSteps } from "./wizardSteps";
import {
  applyReviewedProperty,
  buildNameNodeFederationConfiguration,
  GeneratedConfiguration,
  validateJournalNodeDirectory,
} from "./workflowUtils";

const errorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message || error?.message || fallback;

function journalNodeHosts(hdfsModel: any): string[] {
  const components = [
    ...(hdfsModel?.journalNodes || []),
    ...(hdfsModel?.slaveComponents || []).filter(
      (component: any) =>
        (component.componentName || component.component_name) === "JOURNALNODE",
    ),
  ];
  return [
    ...new Set(
      components.flatMap((component: any) => {
        const directHost = component.hostName || component.host_name;
        const hostComponents = component.hostComponents || [];
        return [
          directHost,
          ...hostComponents.map(
            (hostComponent: any) =>
              hostComponent.HostRoles?.host_name || hostComponent.hostName,
          ),
        ].filter(Boolean);
      }),
    ),
  ];
}

function Step3() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(EnableNamenodeFederationContext);
  const { clusterName, services } = useContext(AppContext);
  const { allModelsLoaded, allServiceModels } = useContext(ServiceContext);
  const [generated, setGenerated] =
    useState<GeneratedConfiguration | null>(null);
  const [loadError, setLoadError] = useState("");
  const [directoryError, setDirectoryError] = useState("");
  const [persistenceError, setPersistenceError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [retryCount, setRetryCount] = useState(0);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const hdfsModel: any = allServiceModels.hdfs;
  const namespaces = getHdfsNamespaces(hdfsModel);
  const namespaceReady = Boolean(
    allModelsLoaded && hdfsModel?.isNamespaceLoaded && namespaces.length,
  );
  const assignments =
    getStepData(
      state,
      enableNamenodeFederationSteps.SELECT_HOSTS,
      "masterComponentHosts",
      "enableNamenodeFederationSteps",
    ) || [];
  const newNameserviceId = getStepData(
    state,
    enableNamenodeFederationSteps.GET_STARTED,
    "nameserviceIds.newNameServiceId",
    "enableNamenodeFederationSteps",
  );
  const currentJournalNodeHosts = journalNodeHosts(hdfsModel);
  const namespaceKey = JSON.stringify(namespaces);
  const assignmentKey = JSON.stringify(assignments);
  const journalNodeKey = currentJournalNodeHosts.join("|");
  const serviceKey = selectedServices.join("|");

  useEffect(() => {
    if (!namespaceReady || !newNameserviceId || !assignments.length) return;
    let cancelled = false;
    const load = async () => {
      setIsLoading(true);
      setLoadError("");
      try {
        const requiredTypes = ["hdfs-site"];
        if (selectedServices.includes("RANGER")) {
          requiredTypes.push(
            "core-site",
            "ranger-tagsync-site",
            "ranger-hdfs-security",
          );
        }
        if (selectedServices.includes("ACCUMULO")) {
          requiredTypes.push("accumulo-site");
        }
        const snapshot = await federationApi.loadCurrentConfigurations(
          clusterName,
          requiredTypes,
        );
        const result = buildNameNodeFederationConfiguration({
          clusterName,
          newNameserviceId,
          namespaces,
          assignments,
          journalNodeHosts: currentJournalNodeHosts,
          installedServices: selectedServices,
          snapshot,
        });
        if (!cancelled) {
          setGenerated(result);
          const editable = result.reviewedProperties.find(
            (item) => item.isEditable,
          );
          setDirectoryError(
            editable ? validateJournalNodeDirectory(editable.value) : "",
          );
        }
      } catch (error: any) {
        if (!cancelled) {
          setGenerated(null);
          setLoadError(
            errorMessage(
              error,
              "Ambari could not build the NameNode Federation configuration.",
            ),
          );
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [
    assignmentKey,
    clusterName,
    journalNodeKey,
    namespaceKey,
    namespaceReady,
    newNameserviceId,
    retryCount,
    serviceKey,
  ]);

  const currentNameNodes = assignments.filter(
    (host: any) => host.component === "NAMENODE" && host.isInstalled,
  );
  const additionalNameNodes = assignments.filter(
    (host: any) => host.component === "NAMENODE" && !host.isInstalled,
  );
  const categories = ["HDFS", "RANGER", "ACCUMULO"].filter((category) =>
    generated?.reviewedProperties.some((item) => item.category === category),
  );

  if (!newNameserviceId || !assignments.length) {
    return (
      <Alert variant="danger">
        The persisted Federation nameservice or host assignment is missing.
        Return to the earlier steps and rebuild the workflow checkpoint.
      </Alert>
    );
  }
  if (!namespaceReady || isLoading) {
    return (
      <div className="d-flex align-items-center justify-content-center gap-2 p-5">
        <Spinner /> Loading the current HDFS topology and configurations...
      </div>
    );
  }
  if (loadError || !generated) {
    return (
      <Alert variant="danger">
        {loadError || "The Federation configuration is unavailable."}
        <Button
          size="sm"
          className="ms-3"
          onClick={() => setRetryCount((value) => value + 1)}
        >
          Retry
        </Button>
      </Alert>
    );
  }

  return (
    <>
      <h2 className="step-title">Review</h2>
      <p className="step-description">Confirm the NameNode hosts and configuration changes.</p>
      {persistenceError ? <Alert variant="danger">{persistenceError}</Alert> : null}
      <Card className="mt-3">
        <Card.Body>
          {currentNameNodes.map((node: any) => (
            <Row key={`current-${node.hostName}`} className="mb-2">
              <Col md={3} className="bolder">Current NameNode</Col>
              <Col md={9}>{node.hostName}</Col>
            </Row>
          ))}
          {additionalNameNodes.map((node: any) => (
            <Row key={`additional-${node.hostName}`} className="mb-2">
              <Col md={3} className="bolder">Additional NameNode</Col>
              <Col md={9}>
                {node.hostName} <Badge bg="success">TO BE INSTALLED</Badge>
              </Col>
            </Row>
          ))}
        </Card.Body>
      </Card>
      <Alert variant="info" className="mt-3">
        Review the generated configuration. Only the new nameservice's
        JournalNode directory can be edited.
      </Alert>
      <Accordion defaultActiveKey="0" className="mt-3">
        {categories.map((category, categoryIndex) => (
          <Accordion.Item eventKey={String(categoryIndex)} key={category}>
            <Accordion.Header>
              {category}
              {category === "HDFS" && directoryError ? (
                <Badge bg="danger" className="ms-2">1</Badge>
              ) : null}
            </Accordion.Header>
            <Accordion.Body>
              {generated.reviewedProperties
                .filter((item) => item.category === category)
                .map((item) => (
                  <Row key={`${item.filename}-${item.name}`} className="mb-3 align-items-center">
                    <Col md={5} className="text-break">{item.displayName}</Col>
                    <Col md={7}>
                      {item.isEditable ? (
                        <>
                          <Form.Control
                            value={item.value}
                            isInvalid={Boolean(directoryError)}
                            onChange={(event) => {
                              const value = event.target.value;
                              const error = validateJournalNodeDirectory(value);
                              setDirectoryError(error);
                              if (!error) {
                                setGenerated(
                                  applyReviewedProperty(generated, item.name, value),
                                );
                              } else {
                                setGenerated({
                                  ...generated,
                                  reviewedProperties: generated.reviewedProperties.map(
                                    (candidate) =>
                                      candidate.name === item.name
                                        ? { ...candidate, value }
                                        : candidate,
                                  ),
                                });
                              }
                            }}
                          />
                          <Form.Control.Feedback type="invalid">
                            {directoryError}
                          </Form.Control.Feedback>
                        </>
                      ) : (
                        <code className="text-break">{item.value}</code>
                      )}
                    </Col>
                  </Row>
                ))}
            </Accordion.Body>
          </Accordion.Item>
        ))}
      </Accordion>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!directoryError}
        onBack={async () => {
          setPersistenceError("");
          try {
            await flushStateToDb("back");
            await handleBackImperitive();
          } catch (error: any) {
            setPersistenceError(
              errorMessage(error, "Ambari could not persist the wizard state."),
            );
          }
        }}
        onNext={async () => {
          setPersistenceError("");
          if (directoryError) return;
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                overridenProperties: generated.snapshot,
                reviewedProperties: generated.reviewedProperties,
                journalNodeHosts: currentJournalNodeHosts,
              },
            },
          });
          try {
            await flushStateToDb("next");
            await handleNextImperitive();
          } catch (error: any) {
            setPersistenceError(
              errorMessage(error, "Ambari could not persist the wizard state."),
            );
          }
        }}
        onCancel={() => void flushStateToDb("cancel")}
      />
    </>
  );
}

export default Step3;
