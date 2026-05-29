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
import { filter, map } from "lodash";
import { ManageJournalNodesContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { getStepData } from "../../../../Utils/Utility";
import { updateComponent } from "../../../../Utils/taskUtils";
import { AppContext } from "../../../../store/context";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";

function Step6() {
    const { clusterName } = useContext(AppContext);
  const [completionStatus, setCompletionStatus] = useState(false);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive,handleBackImperitive },
  } = useContext(ManageJournalNodesContext);
  const operations = [
    {
      id: 1,
      label: "Start JournalNodes",
      skippable: false,
      callback: async () => {
        const step1Data = getStepData(
          state,
          "ASSIGN_JOURNALNODES",
          "masterComponentHosts",
          "manageJournalNodesSteps"
        );
        const currentJournalNodes = map(filter(step1Data, [
          "component",
          "JOURNALNODE",
        ]),"hostName");

       
        return await updateComponent(
          clusterName,
          "JOURNALNODE",
          currentJournalNodes.join(","),
          "HDFS",
          "Start",
          1
        );
      },
    },
  ];
  return (
    <>
      <h3 className="step-title">Perform Operations</h3>
      <div className="mt-3">
        <OperationsProgress
          title=""
          description=""
          setCompletionStatus={setCompletionStatus}
          operations={operations as any}
          dispatch={(operationsState: any) => {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: {
                step: currentStep.name,
                data: {
                  operationsState,
                },
              },
            });
          }}
        />
      </div>
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus}
        onNext={() => {
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}
export default Step6;
