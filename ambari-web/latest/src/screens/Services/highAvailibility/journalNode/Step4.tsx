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
import { RequestApi } from "../../../../api/requestApi";
import { AppContext } from "../../../../store/context";
import { getStepData } from "../../../../Utils/Utility";
import { filter, get, map, uniq } from "lodash";
import {
  createInstallComponentTask,
  deleteComponent,
  reconfigureSites,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { ServiceContext } from "../../../../store/ServiceContext";
import OperationsProgress from "../../../../components/OperationsProgress";
import ConfigsApi from "../../../../api/configsApi";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ManageJournalNodesContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { manageJournalNodesSteps } from "./wizardSteps";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { mergeSavedOperations } from "../haWorkflowUtils";

function Step4() {
  const { clusterName } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive,handleBackImperitive },
  } = useContext(ManageJournalNodesContext);
  const { serviceModels } = useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  //@ts-ignore
  const [completionStatus, setCompletionStatus] = useState(false);
  const { allServiceModels } = useContext(ServiceContext);
  const hdfsModel = allServiceModels["hdfs"];
  const masterComponentHosts = getStepData(
    state,
    "ASSIGN_JOURNALNODES",
    "masterComponentHosts",
    "manageJournalNodesSteps"
  );

  function getJournalNodesToAdd() {
    if (!masterComponentHosts) return [];
    return map(
      filter(
        filter(masterComponentHosts, (jn: any) => 
          jn.component === "JOURNALNODE" || jn.component_name === "JOURNALNODE"
        ),
        ["isInstalled", false]
      ),
      "hostName"
    );
  }

  async function updateConfigProperties(configItems: any) {
    console.log("Confog Items are", configItems);
    const data = getStepData(
      state,
      manageJournalNodesSteps.REVIEW,
      "overridenProperties",
      "manageJournalNodesSteps"
    );

    const siteNames = ["hdfs-site"];
    const note = `This configuration is created by Manage JournalNode Wizard`;
    const configData = reconfigureSites(siteNames, data, note);
    try {
      await ConfigsApi.updateServiceConfigurations(clusterName, {
        desired_config: configData,
      });
      const nnHostNames = map(
        filter(masterComponentHosts, ["component", "NAMENODE"]),
        "hostName"
      );
      const jnHostNames = map(
        filter(masterComponentHosts, ["component", "JOURNALNODE"]),
        "hostName"
      );
      const hostNames = uniq(nnHostNames.concat(jnHostNames));
      return await createInstallComponentTask(
        "HDFS_CLIENT",
        hostNames,
        "HDFS",
        clusterName,
        ["HDFS"],
        serviceModels["hdfs"],
        getKDCSessionState
      );
    } catch (err) {
      console.error("Could not update configs", err);
      throw err;
    }
  }

  const operations = [
    {
      id: 1,
      label: "Stop Standby NameNode",
      skippable: false,
      callback: async () => {
        const hostName = get(hdfsModel, "standbyNameNodes[0].hostName", "");
        if (!hostName) {
          return new Promise((_,reject)=>{
            reject({status:500, message:"Standby NameNode host name not found"});
          })
        }
        return await updateComponent(
          clusterName,
          "NAMENODE",
          hostName,
          "HDFS",
          "INSTALLED",
          1
        );
      },
    },
    {
      id: 2,
      label: "Stop all services",
      skippable: false,
      callback: async () => {
        const data: any = {
          ServiceInfo: {
            state: "INSTALLED",
          },
        };
        data.context = "Stop all services";
        const stopServicesPayload = {
          RequestInfo: {
            context: "Stop all services",
            operation_level: {
              level: "CLUSTER",
              cluster_name: clusterName,
            },
          },
          Body: {
            ServiceInfo: {
              state: "INSTALLED",
            },
          },
        };
        const requestData = await RequestApi.stopServices(
          clusterName,
          stopServicesPayload
        );
        return requestData;
      },
    },
    {
      id: 3,
      label: "Install JournalNodes",
      skippable: false,
      callback: async () => {
        const journalNodesToAdd = getJournalNodesToAdd();
        if (journalNodesToAdd && journalNodesToAdd.length > 0) {
          return await createInstallComponentTask(
            "JOURNALNODE",
            journalNodesToAdd,
            "HDFS",
            clusterName,
            ["HDFS"],
            serviceModels["hdfs"],
            getKDCSessionState
          );
        } else {
          return Promise.resolve({ status: 200 });
        }
      },
    },
    {
      id: 4,
      label: "Delete JournalNodes",
      skippable: false,
      callback: async () => {
        const journalNodesToDelete = getStepData(
      state,
      manageJournalNodesSteps.REVIEW,
      "deletedJournalNodes",
      "manageJournalNodesSteps"
    );;
        if (journalNodesToDelete && journalNodesToDelete.length > 0) {
          // Delete each JournalNode sequentially
          const deletePromises = journalNodesToDelete.map(async (hostName: string) => {
            return await deleteComponent(
              clusterName,
              "JOURNALNODE",
              hostName,
              "HDFS",
              true,
            );
          });
          
          // Wait for all deletions to complete
          await Promise.all(deletePromises);
          return { status: 200 };
        } else {
          // No JournalNodes to delete, skip this step
          return Promise.resolve({ status: 200 });
        }
      },
    },
    {
      id: 5,
      label: "Reconfigure HDFS",
      skippable: false,
      callback: async () => {
        const data = getStepData(
          state,
          manageJournalNodesSteps.REVIEW,
          "overridenProperties",
          "manageJournalNodesSteps"
        );
        return await updateConfigProperties(data);
      },
    },
  ];
  const savedOperationsState = getStepData(
    state,
    manageJournalNodesSteps.ADD_REMOVE_JOURNALNODES,
    "operationsState",
    "manageJournalNodesSteps",
  );
  const restoredOperations = mergeSavedOperations(
    operations,
    savedOperationsState,
  );
  return (
    <>
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
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
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

export default Step4;
