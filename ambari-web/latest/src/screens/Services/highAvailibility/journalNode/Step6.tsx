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
import OperationsProgress from "../../../../components/OperationsProgress";
import { find, get, map } from "lodash";
import { ManageJournalNodesContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { getStepData } from "../../../../Utils/Utility";
import { updateComponent } from "../../../../Utils/taskUtils";
import { AppContext } from "../../../../store/context";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { HostsApi } from "../../../../api/hostsApi";
import { manageJournalNodesSteps } from "./wizardSteps";
import { mergeSavedOperations } from "../haWorkflowUtils";
import { Alert, Button } from "react-bootstrap";

function Step6() {
  const { clusterName } = useContext(AppContext);
  const [completionStatus, setCompletionStatus] = useState(false);
  const [journalNodeHosts, setJournalNodeHosts] = useState<string[]>([]);
  const [topologyError, setTopologyError] = useState("");
  const [isLoadingTopology, setIsLoadingTopology] = useState(true);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive,handleBackImperitive },
  } = useContext(ManageJournalNodesContext);
  const loadFinalTopology = async () => {
    setIsLoadingTopology(true);
    setTopologyError("");
    try {
      const response = await HostsApi.getClusterComponents(
        clusterName,
        "ServiceComponentInfo/component_name,host_components/HostRoles/host_name&minimal_response=true",
      );
      const journalNodeComponent = find(response.items, [
        "ServiceComponentInfo.component_name",
        "JOURNALNODE",
      ]);
      const hosts = map(
        journalNodeComponent?.host_components || [],
        "HostRoles.host_name",
      ).filter(Boolean);
      if (hosts.length < 3) {
        throw new Error(
          "Ambari returned fewer than three JournalNodes after the topology change.",
        );
      }
      setJournalNodeHosts(hosts);
    } catch (error: any) {
      setTopologyError(
        get(error, "response.data.message", error?.message) ||
          "Ambari could not reload the final JournalNode topology.",
      );
    } finally {
      setIsLoadingTopology(false);
    }
  };

  useEffect(() => {
    void loadFinalTopology();
  }, [clusterName]);

  const operations = [
    {
      id: 1,
      label: "Start JournalNodes",
      skippable: false,
      callback: async () => {
        return await updateComponent(
          clusterName,
          "JOURNALNODE",
          journalNodeHosts,
          "HDFS",
          "Start",
          1
        );
      },
    },
  ];
  const savedOperationsState = getStepData(
    state,
    manageJournalNodesSteps.START_JOURNALNODES,
    "operationsState",
    "manageJournalNodesSteps",
  );
  const restoredOperations = mergeSavedOperations(
    operations,
    savedOperationsState,
  );

  if (isLoadingTopology) {
    return <div>Loading final JournalNode topology...</div>;
  }

  if (topologyError) {
    return (
      <Alert variant="danger">
        {topologyError}
        <Button className="ms-3" size="sm" onClick={loadFinalTopology}>
          Retry
        </Button>
      </Alert>
    );
  }
  return (
    <>
      <h3 className="step-title">Perform Operations</h3>
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
        isNextEnabled={completionStatus}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        cancelConfirmationBody="JournalNode changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Any incomplete recovery must be performed manually before changing HDFS topology again."
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}
export default Step6;
