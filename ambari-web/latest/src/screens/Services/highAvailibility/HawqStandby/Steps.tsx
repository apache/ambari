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
import { Alert, Badge, Button, Card, Col, Row } from "react-bootstrap";
import AssignMastersApi from "../../../../api/assignMastersApi";
import federationApi from "../../../../api/federationApi";
import { HostsApi } from "../../../../api/hostsApi";
import OperationsProgress from "../../../../components/OperationsProgress";
import Modal from "../../../../components/Modal";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import {
  createInstallComponentTask,
  startServices,
  stopServices,
} from "../../../../Utils/taskUtils";
import { mergeSavedOperations } from "../haWorkflowUtils";
import HostAssignment from "../Federation/HostAssignment";
import { PersistedWorkflowContext } from "../Federation/PersistedWorkflowContext";
import {
  buildHostValidationPayload,
  ComponentAssignment,
  hawqTaskKeys,
  isMissingComponentError,
  mutateHawqConfiguration,
  validateComponentAssignments,
} from "../Federation/workflowUtils";
import {
  HawqStandbyContext,
  HawqStandbyMode,
  hawqStandbySteps,
} from "./context";

const modeLabel: Record<HawqStandbyMode, string> = {
  add: "Add HAWQ Standby",
  remove: "Remove HAWQ Standby",
  activate: "Activate HAWQ Standby",
};

interface AdvisorHostGroupRecommendations {
  blueprint: {
    host_groups: Array<{
      name: string;
      components: Array<{ name: string }>;
    }>;
  };
  blueprint_cluster_binding: {
    host_groups: Array<{
      name: string;
      hosts: Array<{ fqdn: string }>;
    }>;
  };
  [key: string]: unknown;
}

interface AdvisorValidationIssue {
  type?: string;
  level: string;
  message: string;
  host?: string;
  "component-name"?: string;
}

interface HawqValidationTopology {
  hosts: string[];
  assignments: ComponentAssignment[];
  installedPairs: Set<string>;
}

function record(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

const errorMessage = (error: unknown, fallback: string) =>
  requiredStringOrEmpty(record(record(error)?.response)?.data, "message") ||
  (error instanceof Error ? error.message : "") ||
  fallback;

function requiredStringOrEmpty(value: unknown, key: string) {
  const candidate = record(value)?.[key];
  return typeof candidate === "string" ? candidate : "";
}

function requiredString(value: unknown, description: string): string {
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(`Stack Advisor returned an invalid ${description}.`);
  }
  return value;
}

function componentHostPair(component: string, host: string) {
  return `${component}\0${host}`;
}

function requireHostGroupRecommendations(
  response: unknown,
): AdvisorHostGroupRecommendations {
  const resources = record(response)?.resources;
  if (!Array.isArray(resources) || resources.length !== 1) {
    throw new Error("Stack Advisor returned no host-group recommendation resource.");
  }
  const recommendations = record(record(resources[0])?.recommendations);
  const blueprint = record(recommendations?.blueprint);
  const binding = record(recommendations?.blueprint_cluster_binding);
  const blueprintGroups = blueprint?.host_groups;
  const bindingGroups = binding?.host_groups;
  if (!Array.isArray(blueprintGroups) || !Array.isArray(bindingGroups)) {
    throw new Error("Stack Advisor returned an incomplete host-group recommendation.");
  }

  const blueprintNames = blueprintGroups.map((group, index) => {
    const groupRecord = record(group);
    const name = requiredString(groupRecord?.name, `blueprint host group ${index + 1}`);
    if (!Array.isArray(groupRecord?.components)) {
      throw new Error(`Stack Advisor returned no components for host group ${name}.`);
    }
    groupRecord.components.forEach((component, componentIndex) => {
      requiredString(
        record(component)?.name,
        `component ${componentIndex + 1} in host group ${name}`,
      );
    });
    return name;
  });
  const bindingNames = bindingGroups.map((group, index) => {
    const groupRecord = record(group);
    const name = requiredString(groupRecord?.name, `binding host group ${index + 1}`);
    if (!Array.isArray(groupRecord?.hosts) || !groupRecord.hosts.length) {
      throw new Error(`Stack Advisor returned no hosts for binding group ${name}.`);
    }
    groupRecord.hosts.forEach((host, hostIndex) => {
      requiredString(
        record(host)?.fqdn,
        `host ${hostIndex + 1} in binding group ${name}`,
      );
    });
    return name;
  });
  const uniqueBlueprintNames = new Set(blueprintNames);
  const uniqueBindingNames = new Set(bindingNames);
  if (
    uniqueBlueprintNames.size !== blueprintNames.length ||
    uniqueBindingNames.size !== bindingNames.length ||
    uniqueBlueprintNames.size !== uniqueBindingNames.size ||
    [...uniqueBlueprintNames].some((name) => !uniqueBindingNames.has(name))
  ) {
    throw new Error("Stack Advisor returned inconsistent host-group bindings.");
  }
  return recommendations as unknown as AdvisorHostGroupRecommendations;
}

function requireValidationIssues(response: unknown): AdvisorValidationIssue[] {
  const resources = record(response)?.resources;
  const items = Array.isArray(resources) && resources.length === 1
    ? record(resources[0])?.items
    : null;
  if (!Array.isArray(items)) {
    throw new Error("Stack Advisor returned an invalid host validation response.");
  }
  return items.map((item, index) => {
    const itemRecord = record(item);
    const type = typeof itemRecord?.type === "string" ? itemRecord.type : undefined;
    const level = requiredString(itemRecord?.level, `validation level ${index + 1}`);
    const message = requiredString(itemRecord?.message, `validation message ${index + 1}`);
    const host = typeof itemRecord?.host === "string" ? itemRecord.host : undefined;
    const component = typeof itemRecord?.["component-name"] === "string"
      ? itemRecord["component-name"] as string
      : undefined;
    if (type === "host-component" && (!host || !component)) {
      throw new Error("Stack Advisor returned an incomplete host-component validation issue.");
    }
    return {
      type,
      level,
      message,
      ...(host ? { host } : {}),
      ...(component ? { "component-name": component } : {}),
    };
  });
}

function buildHawqValidationTopology(
  response: unknown,
  selectedStandby: string,
): HawqValidationTopology {
  const items = record(response)?.items;
  if (!Array.isArray(items) || !items.length) {
    throw new Error("Ambari returned no current host-component topology.");
  }
  const hosts: string[] = [];
  const assignments: ComponentAssignment[] = [];
  const installedPairs = new Set<string>();
  for (const item of items) {
    const itemRecord = record(item);
    const hostName = requiredString(record(itemRecord?.Hosts)?.host_name, "cluster host name");
    if (hosts.includes(hostName)) {
      throw new Error(`Ambari returned duplicate topology for host ${hostName}.`);
    }
    hosts.push(hostName);
    const hostComponents = itemRecord?.host_components ?? [];
    if (!Array.isArray(hostComponents)) {
      throw new Error(`Ambari returned invalid host components for ${hostName}.`);
    }
    for (const hostComponent of hostComponents) {
      const roles = record(record(hostComponent)?.HostRoles);
      const component = requiredString(roles?.component_name, `component on ${hostName}`);
      const componentHost = typeof roles?.host_name === "string"
        ? roles.host_name
        : hostName;
      if (componentHost !== hostName) {
        throw new Error(`Ambari returned ${component} under the wrong host.`);
      }
      const pair = componentHostPair(component, componentHost);
      if (installedPairs.has(pair)) continue;
      installedPairs.add(pair);
      assignments.push({ component, hostName: componentHost, isInstalled: true });
    }
  }
  if (!hosts.includes(selectedStandby)) {
    throw new Error(`${selectedStandby} is no longer available in this cluster.`);
  }
  const filteredAssignments = assignments.filter(
    (assignment) => assignment.component !== "HAWQSTANDBY",
  );
  filteredAssignments.push({
    component: "HAWQSTANDBY",
    hostName: selectedStandby,
    isInstalled: false,
  });
  return { hosts, assignments: filteredAssignments, installedPairs };
}

function recommendationContains(
  recommendations: AdvisorHostGroupRecommendations,
  componentName: string,
  hostName: string,
) {
  const hostsByGroup = new Map(
    recommendations.blueprint_cluster_binding.host_groups.map((group) => [
      group.name,
      group.hosts.map((host) => host.fqdn),
    ]),
  );
  return recommendations.blueprint.host_groups.some(
    (group) =>
      group.components.some((component) => component.name === componentName) &&
      hostsByGroup.get(group.name)?.includes(hostName),
  );
}

function kdcSessionPromise(
  getKDCSessionState: (
    callback: () => Promise<void>,
    errorCallback?: () => void,
  ) => Promise<void>,
) {
  return new Promise<void>((resolve, reject) => {
    void getKDCSessionState(
      async () => resolve(),
      () => reject(new Error("KDC session validation failed or was cancelled.")),
    );
  });
}

export function HawqStep1() {
  const { mode, capabilities } = useContext(HawqStandbyContext);
  const {
    storeStep,
    persist,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(PersistedWorkflowContext);
  const [error, setError] = useState("");
  const descriptions: Record<HawqStandbyMode, string> = {
    add:
      "Adding a Standby stops and restarts HAWQ. Schedule a maintenance window before continuing.",
    remove:
      "Removing the current Standby changes HAWQ topology and cannot be rolled back by Ambari.",
    activate:
      "Activating the Standby promotes it to Master and removes the original Master topology.",
  };
  return (
    <>
      <h2 className="step-title">{modeLabel[mode]}</h2>
      <Alert variant={mode === "add" ? "warning" : "danger"}>
        {descriptions[mode]}
      </Alert>
      <Card>
        <Card.Body>
          <Row>
            <Col md={4} className="bolder">Current HAWQ Master</Col>
            <Col>{capabilities.masterHost}</Col>
          </Row>
          {capabilities.standbyHost ? (
            <Row className="mt-2">
              <Col md={4} className="bolder">Current HAWQ Standby</Col>
              <Col>{capabilities.standbyHost}</Col>
            </Row>
          ) : null}
        </Card.Body>
      </Card>
      {error ? <Alert variant="danger" className="mt-3">{error}</Alert> : null}
      <WizardFooter
        step={currentStep}
        isNextEnabled
        onBack={() => undefined}
        onNext={async () => {
          setError("");
          storeStep(hawqStandbySteps.GET_STARTED, {
            masterHost: capabilities.masterHost || "",
            standbyHost: capabilities.standbyHost || "",
          });
          try {
            await persist("next");
            await handleNextImperitive();
          } catch (caught) {
            setError(errorMessage(caught, "Ambari could not persist the wizard state."));
          }
        }}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}

export function HawqSelectHostStep() {
  const {
    clusterName,
    cluster: { stack, versionNum },
    services,
  } = useContext(AppContext);
  const { capabilities } = useContext(HawqStandbyContext);
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
    (state.steps[hawqStandbySteps.SELECT_HOST]?.assignments as
      | ComponentAssignment[]
      | undefined) || [];
  const [assignments, setAssignments] = useState(savedAssignments);
  const [assignmentError, setAssignmentError] = useState(
    "Select exactly one additional HAWQSTANDBY host.",
  );
  const [validationIssues, setValidationIssues] = useState<AdvisorValidationIssue[]>([]);
  const [showValidation, setShowValidation] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [error, setError] = useState("");
  const validationInProgress = useRef(false);

  const continueToReview = async () => {
    setShowValidation(false);
    try {
      await persist("next");
      await handleNextImperitive();
    } catch (caught) {
      setError(errorMessage(caught, "Ambari could not persist the wizard state."));
    }
  };

  const validateAndContinue = async () => {
    if (validationInProgress.current) return;
    validationInProgress.current = true;
    setIsValidating(true);
    setError("");
    setValidationIssues([]);
    setShowValidation(false);
    try {
      const selectedStandby = assignments.find(
        (assignment) => !assignment.isInstalled,
      )?.hostName || "";
      if (!selectedStandby) {
        throw new Error("Select exactly one additional HAWQSTANDBY host.");
      }
      const currentTopology = await HostsApi.getHostComponentsDetails(
        clusterName,
        "fields=Hosts/host_name,host_components/HostRoles/component_name," +
          "host_components/HostRoles/host_name",
      );
      const topology = buildHawqValidationTopology(
        currentTopology,
        selectedStandby,
      );
      if (!topology.assignments.some(
        (assignment) =>
          assignment.component === "HAWQMASTER" &&
          assignment.hostName === capabilities.masterHost,
      )) {
        throw new Error("The current HAWQ Master topology changed. Retry after refreshing the cluster model.");
      }
      const serviceNames = services.map(
        (service) => service.ServiceInfo.service_name,
      );
      const { validate: _validate, ...recommendationPayload } =
        buildHostValidationPayload(
          topology.hosts,
          serviceNames,
          topology.assignments,
        );
      const recommendationResponse = await AssignMastersApi.postRecommendations(
        { ...recommendationPayload, recommend: "host_groups" },
        stack,
        versionNum,
      );
      const recommendations = requireHostGroupRecommendations(
        recommendationResponse,
      );
      for (const assignment of topology.assignments) {
        if (!recommendationContains(
          recommendations,
          assignment.component || "",
          assignment.hostName || "",
        )) {
          throw new Error(
            "Stack Advisor did not preserve the complete current component mapping.",
          );
        }
      }
      const validationResponse = await AssignMastersApi.postValidations(
        {
          hosts: topology.hosts,
          services: serviceNames,
          validate: "host_groups",
          recommendations,
        },
        stack,
        versionNum,
      );
      const issues = requireValidationIssues(validationResponse).filter(
        (issue) =>
          issue.type === "host-component" &&
          !topology.installedPairs.has(componentHostPair(
            issue["component-name"] || "",
            issue.host || "",
          )),
      );
      if (issues.length) {
        setValidationIssues(issues);
        setShowValidation(true);
      } else {
        await continueToReview();
      }
    } catch (caught) {
      setError(
        errorMessage(
          caught,
          "Stack Advisor could not validate the HAWQ Standby assignment. Retry before continuing.",
        ),
      );
    } finally {
      validationInProgress.current = false;
      setIsValidating(false);
    }
  };

  return (
    <>
      <Modal
        isOpen={showValidation}
        onClose={() => setShowValidation(false)}
        modalTitle="Host Assignment Validation"
        modalBody={
          <div>
            {validationIssues.map((issue, index) => (
              <Alert
                key={`${issue.level}-${issue.host}-${index}`}
                variant={issue.level === "ERROR" ? "danger" : "warning"}
              >
                <Badge bg={issue.level === "ERROR" ? "danger" : "warning"} className="me-2">
                  {issue.level}
                </Badge>
                {issue.message}
              </Alert>
            ))}
          </div>
        }
        successCallback={() => void continueToReview()}
        options={{
          okButtonText: "Continue Anyway",
          cancelButtonText: "Review Assignment",
        }}
      />
      <h2 className="step-title">Select Host</h2>
      <p className="step-description">Select the host for the new HAWQ Standby.</p>
      <Row className="mb-3">
        <Col md={4} className="bolder">Current HAWQ Master</Col>
        <Col>{capabilities.masterHost}</Col>
      </Row>
      {assignmentError && assignments.length ? <Alert variant="danger">{assignmentError}</Alert> : null}
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <Card>
        <Card.Body>
          <HostAssignment
            componentName="HAWQSTANDBY"
            componentLabel="HAWQ Standby"
            installedHosts={[]}
            initialAssignments={savedAssignments}
            additionalCount={1}
            services={services.map((service) => service.ServiceInfo.service_name)}
            onChange={(nextAssignments, unavailableHosts) => {
              setAssignments(nextAssignments);
              setAssignmentError(
                validateComponentAssignments(
                  nextAssignments,
                  "HAWQSTANDBY",
                  1,
                  unavailableHosts,
                ),
              );
              storeStep(hawqStandbySteps.SELECT_HOST, {
                assignments: nextAssignments,
              });
            }}
          />
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!assignmentError && assignments.length > 0 && !isValidating}
        isBackEnabled={!isValidating}
        isCancelEnabled={!isValidating}
        onBack={async () => {
          try {
            await persist("back");
            await handleBackImperitive();
          } catch (caught) {
            setError(errorMessage(caught, "Ambari could not persist the wizard state."));
          }
        }}
        onNext={() => void validateAndContinue()}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}

export function HawqReviewStep() {
  const { clusterName } = useContext(AppContext);
  const { mode, capabilities } = useContext(HawqStandbyContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
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
  const assignments =
    (state.steps[hawqStandbySteps.SELECT_HOST]?.assignments as
      | ComponentAssignment[]
      | undefined) || [];
  const selectedStandby =
    assignments.find((assignment) => !assignment.isInstalled)?.hostName ||
    capabilities.standbyHost ||
    "";
  const [masterDirectory, setMasterDirectory] = useState("");
  const [isLoading, setIsLoading] = useState(mode === "add");
  const [error, setError] = useState("");
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    if (mode !== "add") return;
    let cancelled = false;
    setIsLoading(true);
    setError("");
    void federationApi
      .loadCurrentConfigurations(clusterName, ["hawq-site"])
      .then((snapshot) => {
        const directory =
          snapshot.items.find((item) => item.type === "hawq-site")?.properties
            ?.hawq_master_directory;
        if (!directory) throw new Error("hawq_master_directory is missing.");
        if (!cancelled) setMasterDirectory(directory);
      })
      .catch((caught) => {
        if (!cancelled) {
          setError(
            errorMessage(
              caught,
              "Ambari could not load the current HAWQ configuration.",
            ),
          );
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [clusterName, mode, retryCount]);

  const proceed = async () => {
    setShowConfirmation(false);
    setError("");
    try {
      if (mode === "add") {
        await kdcSessionPromise(getKDCSessionState);
      }
      storeStep(hawqStandbySteps.REVIEW, {
        masterHost: capabilities.masterHost || "",
        standbyHost: selectedStandby,
        ...(masterDirectory ? { masterDirectory } : {}),
      });
      await persist("next");
      await handleNextImperitive();
    } catch (caught) {
      setError(errorMessage(caught, "The HAWQ workflow precondition failed."));
    }
  };

  const beginConfirmation = async () => {
    setError("");
    try {
      if (mode !== "add") await kdcSessionPromise(getKDCSessionState);
      setShowConfirmation(true);
    } catch (caught) {
      setError(errorMessage(caught, "KDC session validation failed."));
    }
  };

  if (isLoading) return <div className="d-flex justify-content-center p-5"><Spinner /></div>;
  if (mode === "add" && error && !masterDirectory) {
    return (
      <Alert variant="danger">
        {error}
        <Button size="sm" className="ms-3" onClick={() => setRetryCount((v) => v + 1)}>
          Retry
        </Button>
      </Alert>
    );
  }
  const changeName =
    mode === "activate" ? "hawq_master_address_host" : "hawq_standby_address_host";
  const changeValue = mode === "remove" ? "REMOVE" : selectedStandby;
  return (
    <>
      <Modal
        isOpen={showConfirmation}
        onClose={() => setShowConfirmation(false)}
        modalTitle={mode === "add" ? "Confirm HAWQ Data Directory" : "Irreversible Operation"}
        modalBody={
          mode === "add"
            ? `Before continuing, rename ${masterDirectory} on ${selectedStandby}, or verify that it is empty. Ambari cannot validate the remote directory.`
            : "Ambari cannot roll back this HAWQ topology change after it starts."
        }
        successCallback={() => void proceed()}
        options={{}}
      />
      <h2 className="step-title">Review</h2>
      {error ? <Alert variant="danger">{error}</Alert> : null}
      <Card>
        <Card.Body>
          <Row className="mb-2">
            <Col md={5}>{changeName}</Col>
            <Col><code>{changeValue}</code></Col>
          </Row>
          {mode === "activate" ? (
            <Row>
              <Col md={5}>Original HAWQ Master to remove</Col>
              <Col>{capabilities.masterHost}</Col>
            </Row>
          ) : null}
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={Boolean(selectedStandby)}
        onBack={async () => {
          try {
            await persist("back");
            await handleBackImperitive();
          } catch (caught) {
            setError(errorMessage(caught, "Ambari could not persist the wizard state."));
          }
        }}
        onNext={() => void beginConfirmation()}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}

interface HawqOperation {
  id: string;
  label: string;
  skippable: false;
  callback: () => Promise<unknown>;
  status?: string;
  requestId?: string | number;
  [key: string]: unknown;
}

const taskLabels: Record<string, string> = {
  removeStandby: "Remove HAWQ Standby",
  activateStandby: "Activate HAWQ Standby",
  stopRequiredServices: "Stop HAWQ",
  installHawqStandbyMaster: "Install HAWQ Standby",
  reconfigureHAWQ: "Reconfigure HAWQ",
  installHawqMaster: "Install Promoted HAWQ Master",
  deleteOldHawqMaster: "Delete Original HAWQ Master",
  deleteHawqStandby: "Delete HAWQ Standby Component",
  deleteHawqStandbyComponent: "Delete HAWQ Standby Component",
  startRequiredServices: "Start HAWQ",
};

export function HawqProgressStep() {
  const { clusterName } = useContext(AppContext);
  const { serviceModels } = useContext(ServiceContext);
  const { mode, capabilities } = useContext(HawqStandbyContext);
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
  const review = state.steps[hawqStandbySteps.REVIEW] || {};
  const masterHost = String(review.masterHost || capabilities.masterHost || "");
  const standbyHost = String(review.standbyHost || capabilities.standbyHost || "");
  const savedOperations = state.steps[hawqStandbySteps.CONFIGURE]?.operations as
    | HawqOperation[]
    | undefined;

  const reconfigure = async () => {
    const current = await federationApi.loadCurrentConfigurations(
      clusterName,
      ["hawq-site"],
    );
    const updated = mutateHawqConfiguration(current, mode, {
      masterHost,
      standbyHost,
    });
    return await federationApi.saveConfigurationTypes(
      clusterName,
      updated,
      ["hawq-site"],
      `${modeLabel[mode]} wizard configuration update`,
      false,
    );
  };
  const deleteComponent = async (componentName: string, hostName: string) => {
    try {
      return await HostsApi.deleteHostComponent(
        clusterName,
        hostName,
        componentName,
      );
    } catch (caught) {
      if (isMissingComponentError(caught)) return { status: 200 };
      throw caught;
    }
  };
  const callbacks: Record<string, () => Promise<unknown>> = {
    removeStandby: () =>
      federationApi.executeComponentCommand(clusterName, {
        command: "REMOVE_HAWQ_STANDBY",
        context: "Remove HAWQ Standby",
        serviceName: "HAWQ",
        componentName: "HAWQMASTER",
        hosts: masterHost,
      }),
    activateStandby: () =>
      federationApi.executeComponentCommand(clusterName, {
        command: "ACTIVATE_HAWQ_STANDBY",
        context: "Activate HAWQ Standby",
        serviceName: "HAWQ",
        componentName: "HAWQSTANDBY",
        hosts: standbyHost,
      }),
    stopRequiredServices: () =>
      stopServices(clusterName, ["HAWQ"], true, false, ["HAWQ"]),
    installHawqStandbyMaster: () =>
      createInstallComponentTask(
        "HAWQSTANDBY",
        standbyHost,
        "HAWQ",
        clusterName,
        ["HAWQ"],
        serviceModels.hawq,
        getKDCSessionState,
      ),
    reconfigureHAWQ: reconfigure,
    installHawqMaster: () =>
      createInstallComponentTask(
        "HAWQMASTER",
        standbyHost,
        "HAWQ",
        clusterName,
        ["HAWQ"],
        serviceModels.hawq,
        getKDCSessionState,
      ),
    deleteOldHawqMaster: () => deleteComponent("HAWQMASTER", masterHost),
    deleteHawqStandby: () => deleteComponent("HAWQSTANDBY", standbyHost),
    deleteHawqStandbyComponent: () =>
      deleteComponent("HAWQSTANDBY", standbyHost),
    startRequiredServices: () =>
      startServices(clusterName, false, ["HAWQ"], true),
  };
  const liveOperations = hawqTaskKeys(mode).map((id) => ({
    id,
    label: taskLabels[id],
    skippable: false as const,
    callback: callbacks[id],
  }));
  const operations = mergeSavedOperations<HawqOperation>(
    liveOperations,
    savedOperations,
  );

  if (!masterHost || !standbyHost) {
    return <Alert variant="danger">The persisted HAWQ topology is incomplete.</Alert>;
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
          storeStep(hawqStandbySteps.CONFIGURE, { operations: operationsState });
          await persist();
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={complete && !isCompleting}
        isBackEnabled={false}
        cancelConfirmationBody="Exit this wizard? Completed HAWQ changes are not rolled back. The recovery checkpoint will be preserved."
        onBack={() => undefined}
        onNext={async () => {
          setIsCompleting(true);
          try {
            await persist("complete");
            window.location.href = "/#/main/services/HAWQ/summary";
          } catch (caught) {
            setError(errorMessage(caught, "Ambari could not clear the completed workflow."));
            setIsCompleting(false);
          }
        }}
        onCancel={() => void persist("cancel")}
      />
    </>
  );
}
