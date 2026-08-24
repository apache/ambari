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
import { Alert } from "react-bootstrap";
import OperationsProgress from "../../../../components/OperationsProgress";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import {
  createInstallComponentTask,
  startAllServices,
  stopAllServices,
} from "../../../../Utils/taskUtils";
import { AppContext } from "../../../../store/context";
import { getStepData } from "../../../../Utils/Utility";
import { configValidator } from "../../../../Utils/validators";
import { ServiceContext } from "../../../../store/ServiceContext";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { mergeSavedOperations } from "../haWorkflowUtils";
import { reconfigureRangerAdminServices } from "./rangerAdminHaApi";
import {
  createRangerAdminHaOperations,
  RangerAdminHaOperation,
} from "./rangerAdminHaWorkflow";
import {
  getRangerAdminHosts,
  validateRangerAdminAssignments,
} from "./rangerAdminHaUtils";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableRangerAdminSteps } from "./wizardSteps";
import { responseErrorMessage } from "../../../../Utils/httpError";

function Step4() {
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<
    RangerAdminHaOperation[]
  >([]);
  const [workflowError, setWorkflowError] = useState("");
  const [isCompleting, setIsCompleting] = useState(false);
  const { clusterName, services, ambariProperties } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  } = useContext(EnableHighAvailibilityRangerAdminContext);
  const { getKDCSessionState } = useKDCSessionState(() => undefined);
  const selectedServices = services
    .map((service) => service?.ServiceInfo?.service_name)
    .filter(Boolean);
  const assignments = getStepData(
    state,
    enableRangerAdminSteps.SELECT_HOSTS,
    "masterComponentHosts",
    "enableHighAvailibilityRangerAdminSteps",
  );
  const normalizedAssignments = Array.isArray(assignments) ? assignments : [];
  const assignmentErrors = validateRangerAdminAssignments(
    normalizedAssignments,
  );
  const { additionalHosts } = getRangerAdminHosts(normalizedAssignments);
  const loadBalancerUrl = getStepData(
    state,
    enableRangerAdminSteps.GET_STARTED,
    "loadBalancerUrl",
    "enableHighAvailibilityRangerAdminSteps",
  );
  const preflightErrors = [
    ...assignmentErrors,
    ...(configValidator.isValidURL(String(loadBalancerUrl || ""))
      ? []
      : ["The saved load balancer URL is invalid."]),
  ];
  const savedOperationsState = getStepData(
    state,
    enableRangerAdminSteps.INSTALL_START_TEST,
    "operationsState",
    "enableHighAvailibilityRangerAdminSteps",
  );
  const savedOperationsKey = JSON.stringify(savedOperationsState || []);
  const preflightErrorsKey = preflightErrors.join("\n");
  const runtimeRef = useRef({
    clusterName,
    selectedServices,
    additionalHosts,
    rangerModel: allServiceModels.ranger,
    getKDCSessionState,
    loadBalancerUrl: String(loadBalancerUrl || ""),
    skipServiceChecks:
      ambariProperties?.["skip.service.checks"] === "true",
  });
  runtimeRef.current = {
    clusterName,
    selectedServices,
    additionalHosts,
    rangerModel: allServiceModels.ranger,
    getKDCSessionState,
    loadBalancerUrl: String(loadBalancerUrl || ""),
    skipServiceChecks:
      ambariProperties?.["skip.service.checks"] === "true",
  };
  const savedOperationsRef = useRef(savedOperationsState);
  savedOperationsRef.current = savedOperationsState;

  useEffect(() => {
    if (preflightErrorsKey) {
      setStepOperations([]);
      return;
    }
    const initialOperations = createRangerAdminHaOperations({
      stopAllServices: () => stopAllServices(runtimeRef.current.clusterName),
      installAdditionalRangerAdmins: async () => {
        const runtime = runtimeRef.current;
        if (!runtime.additionalHosts.length) {
          throw new Error("At least one additional Ranger Admin is required.");
        }
        return createInstallComponentTask(
          "RANGER_ADMIN",
          runtime.additionalHosts,
          "RANGER",
          runtime.clusterName,
          runtime.selectedServices,
          runtime.rangerModel,
          runtime.getKDCSessionState,
          { reconcileHosts: true },
        );
      },
      reconfigureServices: () =>
        reconfigureRangerAdminServices(
          runtimeRef.current.clusterName,
          runtimeRef.current.loadBalancerUrl,
        ),
      startAllServices: () =>
        startAllServices(runtimeRef.current.clusterName, {
          runSmokeTest: true,
          skipServiceChecks: runtimeRef.current.skipServiceChecks,
        }),
    });
    const savedOperations = savedOperationsRef.current;
    setStepOperations(
      mergeSavedOperations(
        initialOperations,
        Array.isArray(savedOperations) ? savedOperations : undefined,
      ),
    );
  }, [preflightErrorsKey, savedOperationsKey]);

  if (preflightErrors.length) {
    return (
      <>
        <Alert variant="danger">
          <div className="fw-bold mb-2">
            Ranger Admin HA cannot continue from this saved workflow.
          </div>
          {preflightErrors.map((error) => (
            <div key={error}>{error}</div>
          ))}
        </Alert>
        <WizardFooter
          step={currentStep}
          isNextEnabled={false}
          onBack={() => undefined}
          onNext={() => undefined}
          onCancel={() => void flushStateToDb("cancel")}
        />
      </>
    );
  }

  if (!stepOperations.length) return <div>Loading...</div>;

  const completeWorkflow = async () => {
    if (!completionStatus || isCompleting) return;
    setIsCompleting(true);
    setWorkflowError("");
    try {
      await flushStateToDb("complete");
      window.location.href = "/#/main/services/RANGER/summary";
    } catch (error) {
      setWorkflowError(
        responseErrorMessage(
          error,
          "Ambari could not clear the completed Ranger Admin HA workflow.",
        ),
      );
      setIsCompleting(false);
    }
  };

  const cancelWorkflow = async () => {
    setWorkflowError("");
    const shouldExit = window.confirm(
      "Ranger Admin HA is in progress. Exiting does not roll back completed operations. Continue?",
    );
    if (!shouldExit) return;
    try {
      await flushStateToDb("cancel");
    } catch (error) {
      const message = responseErrorMessage(
        error,
        "Ambari could not save the Ranger Admin HA recovery checkpoint.",
      );
      setWorkflowError(message);
      throw new Error(message);
    }
  };

  return (
    <>
      {workflowError ? <Alert variant="danger">{workflowError}</Alert> : null}
      <OperationsProgress
        title=""
        description=""
        setCompletionStatus={setCompletionStatus}
        operations={stepOperations}
        dispatch={async (operationsState) => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: { operationsState },
            },
          });
          await flushStateToDb();
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus && !isCompleting}
        onNext={completeWorkflow}
        onBack={() => undefined}
        onCancel={cancelWorkflow}
      />
    </>
  );
}

export default Step4;
