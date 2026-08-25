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
import { Alert, Button, Card, Col, Row } from "react-bootstrap";
import { map } from "lodash";
import federationApi from "../../../../api/federationApi";
import OperationsProgress from "../../../../components/OperationsProgress";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import {
  createInstallComponentTask,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { mergeSavedOperations } from "../haWorkflowUtils";
import HostAssignment from "../Federation/HostAssignment";
import { PersistedWorkflowContext } from "../Federation/PersistedWorkflowContext";
import {
  buildRouterFederationConfiguration,
  ComponentAssignment,
  GeneratedConfiguration,
  validateComponentAssignments,
} from "../Federation/workflowUtils";
import { getHdfsNamespaces } from "../haWorkflowUtils";
import { routerFederationSteps } from "./wizardSteps";

const errorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message || error?.message || fallback;

function navigationError(error: any) {
  return errorMessage(error, "Ambari could not persist the wizard state.");
}

export function RouterStep1() {
  const {
    persist,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(PersistedWorkflowContext);
  const [error, setError] = useState("");
  return (
    <>
      <h2 className="step-title">Get Started</h2>
      <Alert variant="info">
        Router-based Federation installs one or more HDFS Routers and configures
        them to monitor every existing nameservice.
      </Alert>
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <WizardFooter
        step={currentStep}
        isNextEnabled
        onBack={() => undefined}
        onNext={async () => {
          setError("");
          try {
            await persist("next");
            await handleNextImperitive();
          } catch (caught) {
            setError(navigationError(caught));
          }
        }}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}

export function RouterStep2() {
  const { services } = useContext(AppContext);
  const { masterSlaveClientsData }: any = useContext(ServiceContext);
  const {
    state,
    storeStep,
    persist,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(PersistedWorkflowContext);
  const savedAssignments =
    (state.steps[routerFederationSteps.SELECT_HOSTS]?.assignments as
      | ComponentAssignment[]
      | undefined) || [];
  const routerComponent = Object.values(masterSlaveClientsData || {}).find(
    (item: any) => item?.ServiceComponentInfo?.component_name === "ROUTER",
  ) as any;
  const installedHosts = (routerComponent?.host_components || [])
    .map((item: any) => item.HostRoles?.host_name)
    .filter(Boolean);
  const [assignments, setAssignments] = useState(savedAssignments);
  const [assignmentError, setAssignmentError] = useState(
    "Select at least one additional ROUTER host.",
  );
  const [error, setError] = useState("");

  return (
    <>
      <h2 className="step-title">Select Hosts</h2>
      <p className="step-description">
        Select the hosts that will run additional HDFS Routers.
      </p>
      {assignmentError && assignments.length ? (
        <Alert variant="danger">{assignmentError}</Alert>
      ) : null}
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <Card>
        <Card.Body>
          <HostAssignment
            componentName="ROUTER"
            componentLabel="Router"
            installedHosts={installedHosts}
            initialAssignments={savedAssignments}
            additionalCount={1}
            allowCountChange
            services={map(services, "ServiceInfo.service_name")}
            onChange={(nextAssignments, unavailableHosts) => {
              setAssignments(nextAssignments);
              setAssignmentError(
                validateComponentAssignments(
                  nextAssignments,
                  "ROUTER",
                  "at-least-one",
                  unavailableHosts,
                ),
              );
              storeStep(routerFederationSteps.SELECT_HOSTS, {
                assignments: nextAssignments,
              });
            }}
          />
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!assignmentError && assignments.length > 0}
        onBack={async () => {
          try {
            await persist("back");
            await handleBackImperitive();
          } catch (caught) {
            setError(navigationError(caught));
          }
        }}
        onNext={async () => {
          try {
            await persist("next");
            await handleNextImperitive();
          } catch (caught) {
            setError(navigationError(caught));
          }
        }}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}

export function RouterStep3() {
  const { clusterName } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const {
    storeStep,
    persist,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(PersistedWorkflowContext);
  const [generated, setGenerated] = useState<GeneratedConfiguration | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const namespaces = getHdfsNamespaces(allServiceModels.hdfs);
  const namespaceKey = JSON.stringify(namespaces);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsLoading(true);
      setError("");
      try {
        const snapshot = await federationApi.loadCurrentConfigurations(
          clusterName,
          ["hdfs-site", "core-site"],
          ["hdfs-rbf-site"],
        );
        const result = buildRouterFederationConfiguration(snapshot, namespaces);
        if (!cancelled) setGenerated(result);
      } catch (caught) {
        if (!cancelled) {
          setGenerated(null);
          setError(
            errorMessage(
              caught,
              "Ambari could not load the Router Federation configuration.",
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
  }, [clusterName, namespaceKey, retryCount]);

  if (isLoading) {
    return <div className="d-flex justify-content-center p-5"><Spinner /></div>;
  }
  if (!generated) {
    return (
      <Alert variant="danger">
        {error}
        <Button size="sm" className="ms-3" onClick={() => setRetryCount((v) => v + 1)}>
          Retry
        </Button>
      </Alert>
    );
  }

  return (
    <>
      <h2 className="step-title">Review</h2>
      <p className="step-description">
        Review the Router configuration. It is saved only after Next succeeds.
      </p>
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <Card>
        <Card.Body>
          {generated.reviewedProperties.map((item) => (
            <Row key={item.name} className="mb-3">
              <Col md={5} className="text-break">{item.name}</Col>
              <Col md={7}><code className="text-break">{item.value}</code></Col>
            </Row>
          ))}
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!isSaving}
        isBackEnabled={!isSaving}
        isCancelEnabled={!isSaving}
        onBack={async () => {
          try {
            await persist("back");
            await handleBackImperitive();
          } catch (caught) {
            setError(navigationError(caught));
          }
        }}
        onNext={async () => {
          setIsSaving(true);
          setError("");
          try {
            await federationApi.saveConfigurationTypes(
              clusterName,
              generated.snapshot,
              ["hdfs-rbf-site"],
              "This configuration is created by Enable Router-based Federation wizard",
              false,
            );
            storeStep(routerFederationSteps.REVIEW, {
              configSnapshot: generated.snapshot,
              reviewedProperties: generated.reviewedProperties,
            });
            await persist("next");
            await handleNextImperitive();
          } catch (caught) {
            setError(
              errorMessage(caught, "Ambari could not save the Router configuration."),
            );
          } finally {
            setIsSaving(false);
          }
        }}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}

interface RouterOperation {
  id: string;
  label: string;
  skippable: false;
  callback: () => Promise<unknown>;
  status?: string;
  requestId?: string | number;
  [key: string]: unknown;
}

export function RouterStep4() {
  const { clusterName } = useContext(AppContext);
  const { serviceModels }: any = useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const {
    state,
    storeStep,
    persist,
    stepWizardUtilities: { currentStep },
  } = useContext(PersistedWorkflowContext);
  const [complete, setComplete] = useState(false);
  const [error, setError] = useState("");
  const [isCompleting, setIsCompleting] = useState(false);
  const assignments =
    (state.steps[routerFederationSteps.SELECT_HOSTS]?.assignments as
      | ComponentAssignment[]
      | undefined) || [];
  const routerHosts = assignments
    .filter(
      (assignment) =>
        (assignment.component || assignment.component_name) === "ROUTER",
    )
    .map((assignment) => assignment.hostName || assignment.selectedHost || "")
    .filter(Boolean);
  const savedOperations = state.steps[routerFederationSteps.CONFIGURE]?.operations as
    | RouterOperation[]
    | undefined;
  const liveOperations: RouterOperation[] = [
    {
      id: "installRouter",
      label: "Install Routers",
      skippable: false,
      callback: () =>
        createInstallComponentTask(
          "ROUTER",
          routerHosts,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels.hdfs,
          getKDCSessionState,
          { reconcileHosts: true },
        ),
    },
    {
      id: "startRouters",
      label: "Start Routers",
      skippable: false,
      callback: () =>
        updateComponent(clusterName, "ROUTER", routerHosts, "HDFS", "Start", 1),
    },
  ];
  const operations = mergeSavedOperations(liveOperations, savedOperations);

  if (!routerHosts.length) {
    return <Alert variant="danger">The persisted Router host assignment is missing.</Alert>;
  }
  return (
    <>
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <OperationsProgress
        title=""
        description=""
        operations={operations}
        setCompletionStatus={setComplete}
        errorCallback={setError}
        dispatch={async (operationsState) => {
          storeStep(routerFederationSteps.CONFIGURE, {
            operations: operationsState,
          });
          await persist();
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={complete && !isCompleting}
        isBackEnabled={false}
        cancelConfirmationBody="Exit this wizard? Completed server changes are not rolled back. The recovery checkpoint will be preserved."
        onBack={() => undefined}
        onNext={async () => {
          setIsCompleting(true);
          try {
            await persist("complete");
            window.location.href = "/#/main/services/HDFS/summary";
          } catch (caught) {
            setError(navigationError(caught));
            setIsCompleting(false);
          }
        }}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}
