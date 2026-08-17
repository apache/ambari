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

import { useContext, useRef, useState } from "react";
import { Alert, Badge, Button, Card, ListGroup, Spinner, Stack } from "react-bootstrap";
import { get } from "lodash";
import { AppContext } from "../../../store/context";
import { ContextWrapper } from "../../ClusterWizard";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import { HostsApi } from "../../../api/hostsApi";
import ConfigGroupApi from "../../../api/configGroupApi";
import useKDCSessionState from "../../../hooks/useKDCSessionState";
import {
  buildAddHostComponentAssignments,
  buildAddHostConfigGroupUpdates,
} from "../../../Utils/hostWizard";
import { ActionTypes } from "./wizardDataStore/types";
import { wizardCheckpoint } from "../../ClusterWizard/installationProgress";

function requestIdFrom(response: any): string | number | undefined {
  return response?.Requests?.id ?? response?.data?.Requests?.id ?? response?.id;
}

function errorMessage(error: any) {
  return error?.response?.data?.message
    || error?.response?.data
    || error?.message
    || "Ambari could not prepare the selected hosts for installation.";
}

export default function AddHostReview() {
  const { clusterName: contextClusterName } = useContext(AppContext);
  const { Context } = useContext(ContextWrapper);
  const {
    dispatch,
    flushStateToDb,
    state,
    stepWizardUtilities: {
      currentStep,
      handleBackImperitive,
      handleNextImperitive,
    },
  }: any = useContext(Context);
  const { getKDCSessionState } = useKDCSessionState(() => {});

  const clusterName = get(
    state,
    "addHostSteps.NAME.data.clusterName",
    contextClusterName,
  );
  const registeredHosts = get(
    state,
    "addHostSteps.HOST_STATUS.data.hosts",
    [],
  ).filter((host: any) => host.bootStatus === "REGISTERED");
  const assignments = get(
    state,
    "addHostSteps.SLAVES_AND_CLIENTS.data.serviceComponents",
    [],
  );
  const componentMetadata = get(
    state,
    "addHostSteps.SLAVES_AND_CLIENTS.data.allServiceComponentsList",
    [],
  );
  const configurations = get(
    state,
    "addHostSteps.CONFIGURATIONS.data.configurations",
    [],
  );
  const restoredReview = get(state, "addHostSteps.REVIEW.data", {});
  const componentAssignments = buildAddHostComponentAssignments(
    assignments,
    componentMetadata,
  );
  const configGroupUpdates = buildAddHostConfigGroupUpdates(
    configurations,
    assignments,
    componentMetadata,
  );

  const hostsRegistered = useRef(Boolean(restoredReview.hostsRegistered));
  const completedComponents = useRef(new Set<string>(
    restoredReview.completedComponents || [],
  ));
  const completedConfigGroups = useRef(new Set<string>(
    restoredReview.completedConfigGroups || [],
  ));
  const deployLocked = useRef(false);
  const [deploying, setDeploying] = useState(false);
  const [deployError, setDeployError] = useState("");
  const [deploymentStage, setDeploymentStage] = useState(
    restoredReview.deploymentStage || "Ready to deploy",
  );
  const deploymentStageRef = useRef(
    restoredReview.deploymentStage || "Ready to deploy",
  );

  const updateDeploymentStage = (stage: string) => {
    deploymentStageRef.current = stage;
    setDeploymentStage(stage);
  };

  const saveReviewState = (data: Record<string, any>) => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "REVIEW",
        data: {
          ...restoredReview,
          hostsRegistered: hostsRegistered.current,
          completedComponents: [...completedComponents.current],
          completedConfigGroups: [...completedConfigGroups.current],
          deploymentStage: deploymentStageRef.current,
          ...data,
        },
      },
    });
  };

  const waitForKDCSession = () => new Promise<void>((resolve, reject) => {
    void getKDCSessionState(resolve, reject).catch(reject);
  });

  const deploy = async () => {
    if (deployLocked.current || deploying) return;
    deployLocked.current = true;
    setDeploying(true);
    setDeployError("");

    try {
      await Promise.resolve(flushStateToDb(
        "checkpoint",
        -1,
        wizardCheckpoint("addHost", "PREP"),
      ));
      await waitForKDCSession();

      if (!hostsRegistered.current) {
        updateDeploymentStage("Adding hosts to the cluster");
        await HostsApi.registerHostToComponent(
          clusterName,
          registeredHosts.map((host: any) => ({
            Hosts: { host_name: host.name },
          })),
        );
        hostsRegistered.current = true;
        saveReviewState({
          hostsRegistered: true,
          deploymentStage: "Hosts added to the cluster",
        });
      }

      for (const [componentName, hostNames] of Object.entries(componentAssignments)) {
        if (completedComponents.current.has(componentName)) continue;
        updateDeploymentStage(`Assigning ${componentName}`);
        await HostsApi.registerHostToComponent(clusterName, {
          RequestInfo: {
            query: hostNames.map((hostName) => `Hosts/host_name=${hostName}`).join("|"),
          },
          Body: {
            host_components: [{
              HostRoles: { component_name: componentName },
            }],
          },
        });
        completedComponents.current.add(componentName);
        saveReviewState({
          completedComponents: [...completedComponents.current],
          deploymentStage: `${componentName} assigned`,
        });
      }

      for (const update of configGroupUpdates) {
        if (completedConfigGroups.current.has(update.groupId)) continue;
        updateDeploymentStage(`Applying the ${update.serviceName} configuration group`);
        await ConfigGroupApi.updateConfigGroup(
          clusterName,
          update.groupId,
          update.payload,
        );
        completedConfigGroups.current.add(update.groupId);
        saveReviewState({
          completedConfigGroups: [...completedConfigGroups.current],
          deploymentStage: `${update.serviceName} configuration group applied`,
        });
      }

      let clusterStatus: Record<string, any>;
      if (Object.keys(componentAssignments).length === 0) {
        clusterStatus = {
          status: "STARTED",
          isCompleted: true,
          oldRequestsId: [],
          phase: "COMPLETE",
        };
      } else {
        updateDeploymentStage("Launching component installation");
        const installResponse = await HostsApi.updateHostComponents(
          clusterName,
          "",
          {
            context: "Install Components",
            HostRoles: { state: "INSTALLED" },
            level: "HOST_COMPONENT",
            query: `HostRoles/host_name.in(${registeredHosts
              .map((host: any) => host.name)
              .join(",")})`,
          },
        );
        const requestId = requestIdFrom(installResponse);
        if (requestId == null) {
          throw new Error("Ambari did not return an installation request ID.");
        }
        clusterStatus = {
          status: "PENDING",
          requestId,
          oldRequestsId: [requestId],
          isInstallError: false,
          isCompleted: false,
          installStartTime: Date.now(),
          phase: "INSTALL",
        };
      }

      saveReviewState({
        clusterStatus,
        deploymentStage: "Installation request launched",
      });
      await Promise.resolve(flushStateToDb(
        "next",
        -1,
        wizardCheckpoint("addHost", "INSTALLING"),
      ));
      handleNextImperitive();
    } catch (error: any) {
      setDeployError(String(errorMessage(error)));
      saveReviewState({
        deploymentStage: deploymentStageRef.current,
        deploymentError: String(errorMessage(error)),
      });
      deployLocked.current = false;
    } finally {
      setDeploying(false);
    }
  };

  return (
    <>
      <div className="step-title">Review</div>
      <p className="step-description mt-2">
        Review the new hosts, component assignments, and configuration groups before deployment.
      </p>

      {deployError ? (
        <Alert variant="danger" className="mt-3">
          {deployError}{" "}
          <Button size="sm" variant="outline-danger" onClick={() => void deploy()}>
            Retry
          </Button>
        </Alert>
      ) : null}
      {deploying ? (
        <Alert variant="info" className="mt-3 d-flex align-items-center gap-2">
          <Spinner animation="border" size="sm" />
          {deploymentStage}
        </Alert>
      ) : null}

      <Card className="mt-3">
        <Card.Header>Hosts</Card.Header>
        <ListGroup variant="flush">
          {registeredHosts.map((host: any) => (
            <ListGroup.Item key={host.name}>{host.name}</ListGroup.Item>
          ))}
        </ListGroup>
      </Card>

      <Card className="mt-3">
        <Card.Header>Component Assignments</Card.Header>
        <ListGroup variant="flush">
          {Object.entries(componentAssignments).map(([componentName, hostNames]) => (
            <ListGroup.Item key={componentName}>
              <Stack direction="horizontal" className="justify-content-between">
                <span>{componentName}</span>
                <Badge bg="secondary">{hostNames.join(", ")}</Badge>
              </Stack>
            </ListGroup.Item>
          ))}
        </ListGroup>
      </Card>

      <Card className="mt-3 mb-5">
        <Card.Header>Configuration Groups</Card.Header>
        <ListGroup variant="flush">
          {configurations.length ? configurations.map((configuration: any) => {
            const selected = (configuration.configGroups || []).find(
              (group: any) => group.isSelected,
            );
            return (
              <ListGroup.Item key={configuration.serviceName}>
                <b>{configuration.serviceName}</b>: {selected?.group_name || "Default"}
              </ListGroup.Item>
            );
          }) : (
            <ListGroup.Item>No configuration-group changes are required.</ListGroup.Item>
          )}
        </ListGroup>
      </Card>

      <WizardFooter
        isNextEnabled={!deploying}
        lifted
        step={{ ...currentStep, nextLabel: "DEPLOY" }}
        onNext={() => void deploy()}
        onCancel={() => void flushStateToDb("cancel")}
        onBack={() => {
          void flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}
