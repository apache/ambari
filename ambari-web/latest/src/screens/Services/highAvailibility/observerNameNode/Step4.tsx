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
import { Alert } from "react-bootstrap";
import { AppContext } from "../../../../store/context";
import { filter, find, map } from "lodash";
import {
  createInstallComponentTask,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { AddObserverNamenodeContext } from "./store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getStepData } from "../../../../Utils/Utility";
import { addObserverNamenodeSteps } from "./wizardSteps";
import observerNameNodeApi from "../../../../api/observerNameNodeApi";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import OperationsProgress from "../../../../components/OperationsProgress";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { ActionTypes } from "./store/types";

export function Step4() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, jumpToStep },
  } = useContext(AddObserverNamenodeContext);
  const { clusterName } = useContext(AppContext);
  const { serviceModels: allServiceModels }: any = useContext(ServiceContext);
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const { getKDCSessionState } = useKDCSessionState(() => {});

  const masterComponentHosts = getStepData(
    state,
    addObserverNamenodeSteps.SELECT_HOSTS,
    "masterComponentHosts",
    "addObserverNamenodeSteps"
  );

  const newNameNodeHosts = () => {
    return map(
      filter(filter(masterComponentHosts, ["component", "NAMENODE"]), [
        "isInstalled",
        false,
      ]),
      "hostName"
    );
  };

  const oldNameNodeHosts = () => {
    return map(
      filter(filter(masterComponentHosts, ["component", "NAMENODE"]), [
        "isInstalled",
        true,
      ]),
      "hostName"
    );
  };

  const allDatanodeHosts = () => {
    const dnComponent = find(allServiceModels["hdfs"]?.slaveComponents, [
      "componentName",
      "DATANODE",
    ]);
    if (dnComponent) {
      return map(dnComponent?.hostComponents, "HostRoles.host_name");
    }
    return [];
  };

  function initializeTasks() {
    let id = 0;
    const allOps: any[] = [];

    // NOTE: The hdfs-site config changes are applied in Step3 (Review) when it loads,
    // matching the Ember Observer NameNode wizard which has no "Reconfigure Services"
    // task in step4. The step4 task list here mirrors the Ember commands exactly:
    // installNameNode, installZKFC, enterSafeMode, saveNamespace, leaveSafeMode,
    // formatZKFC, bootstrapNameNode, startZKFC, startNameNode, refreshConfigs,
    // refreshNamenodes, transitionToObserver.

    // Install Additional NameNode
    allOps.push({
      id: ++id,
      label: "Install Additional Namenode",
      skippable: false,
      callback: async () => {
        return await createInstallComponentTask(
          "NAMENODE",
          newNameNodeHosts(),
          "HDFS",
          clusterName,
          ["HDFS"],
          allServiceModels["hdfs"],
          getKDCSessionState
        );
      },
    });

    // Install ZKFC
    allOps.push({
      id: ++id,
      label: "Install ZKFC",
      skippable: false,
      callback: async () => {
        return await createInstallComponentTask(
          "ZKFC",
          newNameNodeHosts(),
          "HDFS",
          clusterName,
          ["HDFS"],
          allServiceModels["hdfs"],
          getKDCSessionState
        );
      },
    });

    // Enter Safe Mode (on the standby / second existing NN)
    allOps.push({
      id: ++id,
      label: "Enter Safe Mode",
      skippable: false,
      callback: async () => {
        const host = oldNameNodeHosts()[1] || oldNameNodeHosts()[0];
        const data = {
          RequestInfo: { command: "ENTER_SAFEMODE", context: "Enter Safemode" },
          "Requests/resource_filters": [
            { service_name: "HDFS", component_name: "NAMENODE", hosts: host },
          ],
        };
        return await observerNameNodeApi.enterSafeMode(clusterName, data);
      },
    });

    // Save Namespace
    allOps.push({
      id: ++id,
      label: "Save Namespace",
      skippable: false,
      callback: async () => {
        const host = oldNameNodeHosts()[1] || oldNameNodeHosts()[0];
        const data = {
          RequestInfo: { command: "SAVE_NAMESPACE", context: "Save Namespace" },
          "Requests/resource_filters": [
            { service_name: "HDFS", component_name: "NAMENODE", hosts: host },
          ],
        };
        return await observerNameNodeApi.saveNamespace(clusterName, data);
      },
    });

    // Leave Safe Mode
    allOps.push({
      id: ++id,
      label: "Leave Safe Mode",
      skippable: false,
      callback: async () => {
        const host = oldNameNodeHosts()[1] || oldNameNodeHosts()[0];
        const data = {
          RequestInfo: { command: "LEAVE_SAFEMODE", context: "Leave Safemode" },
          "Requests/resource_filters": [
            { service_name: "HDFS", component_name: "NAMENODE", hosts: host },
          ],
        };
        return await observerNameNodeApi.leaveSafeMode(clusterName, data);
      },
    });

    // Format ZKFC (on the new host)
    allOps.push({
      id: ++id,
      label: "Format ZKFC",
      skippable: false,
      callback: async () => {
        const host = newNameNodeHosts()[0];
        const data = {
          RequestInfo: { command: "FORMAT", context: "Format ZKFC" },
          "Requests/resource_filters": [
            { service_name: "HDFS", component_name: "ZKFC", hosts: host },
          ],
        };
        return await observerNameNodeApi.enterSafeMode(clusterName, data);
      },
    });

    // Bootstrap Additional NameNode
    allOps.push({
      id: ++id,
      label: "Bootstrap Additional Namenode",
      skippable: false,
      callback: async () => {
        const host = newNameNodeHosts()[0];
        const data = {
          RequestInfo: {
            command: "BOOTSTRAP_STANDBY",
            context: "Bootstrap NameNode",
          },
          "Requests/resource_filters": [
            { service_name: "HDFS", component_name: "NAMENODE", hosts: host },
          ],
        };
        return await observerNameNodeApi.saveNamespace(clusterName, data);
      },
    });

    // Start ZKFC (new host)
    allOps.push({
      id: ++id,
      label: "Start ZKFC",
      skippable: false,
      callback: async () => {
        const host = newNameNodeHosts()[0];
        return await updateComponent(
          clusterName,
          "ZKFC",
          host,
          "HDFS",
          "Start",
          id
        );
      },
    });

    // Start New NameNode
    allOps.push({
      id: ++id,
      label: "Start New Namenode",
      skippable: false,
      callback: async () => {
        const host = newNameNodeHosts()[0];
        return await updateComponent(
          clusterName,
          "NAMENODE",
          host,
          "HDFS",
          "Start",
          id
        );
      },
    });

    // Refresh configs on DataNodes
    allOps.push({
      id: ++id,
      label: "Refresh configs",
      skippable: false,
      callback: async () => {
        const data = {
          RequestInfo: { command: "CONFIGURE", context: "refresh configs" },
          "Requests/resource_filters": [
            {
              service_name: "HDFS",
              component_name: "DATANODE",
              hosts: allDatanodeHosts().join(","),
            },
          ],
        };
        return await observerNameNodeApi.refreshNamenodes(clusterName, data);
      },
    });

    // Refresh Namenodes on DataNodes
    allOps.push({
      id: ++id,
      label: "Refresh Namenodes",
      skippable: false,
      callback: async () => {
        const data = {
          RequestInfo: {
            command: "REFRESH_NAMENODE",
            context: "Refresh Namenode",
          },
          "Requests/resource_filters": [
            {
              service_name: "HDFS",
              component_name: "DATANODE",
              hosts: allDatanodeHosts().join(","),
            },
          ],
        };
        return await observerNameNodeApi.refreshNamenodes(clusterName, data);
      },
    });

    // Transition to Observer (the key final step)
    allOps.push({
      id: ++id,
      label: "Transition to Observer",
      skippable: false,
      callback: async () => {
        const host = newNameNodeHosts()[0];
        const data = {
          RequestInfo: {
            command: "TRANSITION_NAMENODE",
            context: "Transition Namenode",
          },
          "Requests/resource_filters": [
            { service_name: "HDFS", component_name: "NAMENODE", hosts: host },
          ],
        };
        return await observerNameNodeApi.transitionToObserver(
          clusterName,
          data
        );
      },
    });

    return allOps;
  }

  const savedOperationsState = getStepData(
    state,
    addObserverNamenodeSteps.CONFIGURE_COMPONENTS,
    "operationsState",
    "addObserverNamenodeSteps"
  );

  useEffect(() => {
    const operations = initializeTasks();
    const finalOperations = (() => {
      if (savedOperationsState && Array.isArray(savedOperationsState)) {
        return operations.map((originalOp) => {
          const savedOp = savedOperationsState.find(
            (saved: any) => saved.id === originalOp.id
          );
          return savedOp
            ? { ...originalOp, ...savedOp, callback: originalOp.callback }
            : originalOp;
        });
      }
      return operations;
    })();
    setStepOperations(finalOperations);
  }, [JSON.stringify(savedOperationsState)]);

  if (!stepOperations || stepOperations.length === 0) {
    return <div>Loading...</div>;
  }

  return (
    <>
      {completionStatus && (
        <Alert variant="success" className="mb-3">
          Observer Namenode has been enabled successfully.
        </Alert>
      )}
      <OperationsProgress
        title=""
        description=""
        setCompletionStatus={setCompletionStatus}
        operations={stepOperations as any}
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
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus}
        onNext={() => {
          flushStateToDb("cancel"); // Clear the wizard state on completion
          window.location.href = "/#/main/services/HDFS/summary";
          window.location.reload();
        }}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(2);
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}
