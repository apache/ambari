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

import { useContext, useState } from "react";
import type { ComponentProps } from "react";
import { Alert } from "react-bootstrap";
import { AppContext } from "../../../../store/context";
import OperationsProgress from "../../../../components/OperationsProgress";
import Spinner from "../../../../components/Spinner";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import modalManager from "../../../../store/ModalManager";
import { EnableHighAvailibilityContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableResourceManagerSteps } from "./wizardSteps";
import {
  createRmHaOperations,
  mergePersistedRmHaOperations,
} from "./rmHaWorkflow";
import {
  canCompleteRmHa,
  getRmHaAssignment,
  responseErrorMessage,
  stripOperationCallbacks,
} from "./rmHaUtils";
import {
  PersistedRmHaOperation,
  RmHaOperation,
  RmHaReviewConfig,
} from "./rmHaTypes";

type ProgressDispatch = NonNullable<
  ComponentProps<typeof OperationsProgress>["dispatch"]
>;

function Step4() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services, ambariProperties } = useContext(AppContext);
  const { isLoaded: isKdcLoaded, getKDCSessionState } =
    useKDCSessionState(() => {});
  const assignment = getRmHaAssignment(
    state.enableHighAvailibilitySteps?.[
      enableResourceManagerSteps.SELECT_HOSTS
    ],
  );
  const reviewStepData =
    state.enableHighAvailibilitySteps?.[enableResourceManagerSteps.REVIEW]?.data;
  const reviewConfig = (reviewStepData?.reviewConfig ||
    reviewStepData?.stepConfigs) as RmHaReviewConfig | undefined;
  const savedOperations = state.enableHighAvailibilitySteps?.[
    enableResourceManagerSteps.CONFIGURE_COMPONENTS
  ]?.data?.operationsState as PersistedRmHaOperation[] | undefined;
  const selectedServices = services
    .map((service) => service?.ServiceInfo?.service_name)
    .filter(Boolean) as string[];
  const setupError = !assignment
    ? "The ResourceManager host assignment is missing."
    : !reviewConfig?.configs
      ? "The ResourceManager HA configuration review is missing."
      : "";

  const [stepOperations] = useState<RmHaOperation[]>(() => {
    if (!assignment || !reviewConfig?.configs) return [];
    const operations = createRmHaOperations({
      clusterName,
      services: selectedServices,
      additionalRM: assignment.additionalRM,
      reviewConfig,
      runSmokeTest: ambariProperties?.["skip.service.checks"] !== "true",
      getKdcSessionState: getKDCSessionState,
    });
    return mergePersistedRmHaOperations(operations, savedOperations);
  });
  const [completionStatus, setCompletionStatus] = useState(() =>
    canCompleteRmHa(stripOperationCallbacks(stepOperations)),
  );
  const [workflowError, setWorkflowError] = useState("");
  const [isCompleting, setIsCompleting] = useState(false);

  const persistOperations: ProgressDispatch = async (operationsState) => {
    const persistedOperations = operationsState.map(
      ({ callback: _callback, ...operation }) => {
        if (typeof operation.id !== "string" || operation.skippable) {
          throw new Error(
            "ResourceManager HA received an invalid operation checkpoint.",
          );
        }
        return {
          ...operation,
          id: operation.id,
          skippable: false as const,
        } as PersistedRmHaOperation;
      },
    );
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: { operationsState: persistedOperations },
      },
    });
    await flushStateToDb();
    setCompletionStatus(canCompleteRmHa(persistedOperations));
  };

  async function completeWorkflow() {
    if (!completionStatus || isCompleting) return;
    setIsCompleting(true);
    setWorkflowError("");
    try {
      await flushStateToDb("complete");
      modalManager.hide();
      window.location.href = "/#/main/services/YARN/summary";
      window.location.reload();
    } catch (error) {
      setWorkflowError(
        responseErrorMessage(
          error,
          "Ambari could not clear the completed ResourceManager HA workflow.",
        ),
      );
      setIsCompleting(false);
    }
  }

  async function cancelWorkflow() {
    setWorkflowError("");
    try {
      await flushStateToDb("cancel");
    } catch (error) {
      const message = responseErrorMessage(
        error,
        "Ambari could not save the ResourceManager HA recovery checkpoint.",
      );
      setWorkflowError(message);
      throw new Error(message);
    }
  }

  if (setupError) {
    return (
      <>
        <h3 className="step-title">Configure Components</h3>
        <Alert variant="danger" className="mt-3">
          {setupError} Close the wizard and restart ResourceManager HA.
        </Alert>
        <WizardFooter
          step={currentStep}
          isNextEnabled={false}
          onBack={() => undefined}
          onNext={() => undefined}
          onCancel={cancelWorkflow}
          cancelConfirmationBody="Exit the wizard and keep the deployment checkpoint for recovery?"
        />
      </>
    );
  }

  return (
    <>
      <h3 className="step-title">Configure Components</h3>
      <div className="step-description">
        Ambari is stopping services, installing the additional ResourceManager,
        saving HA configurations, and starting the cluster.
      </div>

      {!isKdcLoaded ? (
        <div className="d-flex align-items-center gap-2 mt-4">
          <Spinner />
          <span>Preparing Kerberos session validation...</span>
        </div>
      ) : (
        <OperationsProgress
          title=""
          description=""
          setCompletionStatus={setCompletionStatus}
          operations={stepOperations}
          dispatch={persistOperations}
          errorCallback={setWorkflowError}
          allowCompleteOnFinalFailure
        />
      )}

      {workflowError && (
        <Alert variant="danger" className="mt-3">
          {workflowError}
        </Alert>
      )}

      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus && !isCompleting}
        onBack={() => undefined}
        onNext={() => void completeWorkflow()}
        onCancel={cancelWorkflow}
        cancelConfirmationBody="ResourceManager HA deployment may continue on the server. Exit the wizard and keep this checkpoint so progress can be resumed?"
      />
    </>
  );
}

export default Step4;
