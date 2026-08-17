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
import OperationsProgress from "../../../../components/OperationsProgress";
import { ManageJournalNodesContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { startAllServices } from "../../../../Utils/taskUtils";
import { AppContext } from "../../../../store/context";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import modalManager from "../../../../store/ModalManager";
import { getStepData } from "../../../../Utils/Utility";
import { manageJournalNodesSteps } from "./wizardSteps";
import { mergeSavedOperations } from "../haWorkflowUtils";
import { Alert } from "react-bootstrap";

function Step7() {
  const { clusterName } = useContext(AppContext);
  const [completionStatus, setCompletionStatus] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [completionError, setCompletionError] = useState("");
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleBackImperitive },
  } = useContext(ManageJournalNodesContext);
  const operations = [
    {
      id: 1,
      label: "Start all services",
      skippable: false,
      callback: async () => {
        return await startAllServices(clusterName);
      },
    },
  ];
  const savedOperationsState = getStepData(
    state,
    manageJournalNodesSteps.START_ALL_SERVICES,
    "operationsState",
    "manageJournalNodesSteps",
  );
  const restoredOperations = mergeSavedOperations(
    operations,
    savedOperationsState,
  );
  const completeWizard = async () => {
    setIsCompleting(true);
    setCompletionError("");
    try {
      await flushStateToDb("complete");
      modalManager.hide();
      window.location.href = "/#/main/services/HDFS/summary";
    } catch (error: unknown) {
      const persistenceError = error as {
        message?: string;
        response?: { data?: { message?: string } };
      };
      setCompletionError(
        persistenceError.response?.data?.message ||
          persistenceError.message ||
          "Ambari could not clear the Manage JournalNodes workflow checkpoint.",
      );
      setIsCompleting(false);
    }
  };
  return (
    <>
      {completionError ? <Alert variant="danger">{completionError}</Alert> : null}
      <h3 className="step-title">Start services</h3>
      <div className="mt-3">
        <OperationsProgress
          title=""
          description=""
          setCompletionStatus={setCompletionStatus}
          operations={restoredOperations as any}
          dispatch={async (operationsState: any) => {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: {
                step: currentStep.name,
                data: {
                  operationsState,
                },
              },
            });
            await flushStateToDb();
          }}
        />
      </div>
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus && !isCompleting}
        onNext={() => void completeWizard()}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        cancelConfirmationBody="JournalNode changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Any incomplete recovery must be performed manually before changing HDFS topology again."
      />
    </>
  );
}
export default Step7;
