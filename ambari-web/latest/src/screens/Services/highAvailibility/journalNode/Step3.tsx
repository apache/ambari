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
import { getStepData } from "../../../../Utils/Utility";
import { filter, find, get, isEmpty, map } from "lodash";
import adminApi from "../../../../api/adminApi";
import { AppContext } from "../../../../store/context";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { Card, ListGroup } from "react-bootstrap";
import { ManageJournalNodesContext } from "./store/context";
import { ServiceContext } from "../../../../store/ServiceContext";

function Step3() {
  const { clusterName } = useContext(AppContext);
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive,handleBackImperitive },
  } = useContext(ManageJournalNodesContext);
  console.log("Step3 state", state);
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const { allServiceModels } = useContext(ServiceContext);
  const [, setIsActiveNameNodesStarted] =
    useState(false);
  const hdfsModel = allServiceModels["hdfs"];

  function getNnCheckPointStatus(data: any) {
    const isInSafeMode = !isEmpty(get(data, "metrics.dfs.namenode.Safemode"));
    let journalTransactionInfo = JSON.parse(
      get(data, "metrics.dfs.namenode.JournalTransactionInfo")
    );
    // in case when transaction info absent or invalid return 2 which will return false in next `if` statement
    journalTransactionInfo = !!journalTransactionInfo
      ? parseInt(journalTransactionInfo.LastAppliedOrWrittenTxId) -
        parseInt(journalTransactionInfo.MostRecentCheckpointTxId)
      : 2;
    return journalTransactionInfo <= 1 && isInSafeMode;
  }

  async function pullCheckPointStatus() {
    const masterComponentHosts = getStepData(
      state,
      "ASSIGN_JOURNALNODES",
      "masterComponentHosts",
      "manageJournalNodesSteps"
    );
    const hostName = find(
      filter(masterComponentHosts, ["component", "NAMENODE"]),
      ["isInstalled", true]
    )?.hostName;
    try {
      const data = await adminApi.getNnCheckPointStatus(clusterName, hostName);
      // const isNamenodeStarted = data.HostRoles.desired_state === "STARTED";
      const shouldEnableNext = getNnCheckPointStatus(data);
      if (shouldEnableNext) {
        setIsNextEnabled(true);
      }
    } catch (err) {
      console.error("Error in fetching checkpoint status", err);
    }
  }

  function checkNnCheckPointsStatuses(data: any) {
    const items = data.items,
      isNextEnabledLocal =
        items.length && items.every((item: any) => getNnCheckPointStatus(item));
    setIsActiveNameNodesStarted(
      items.length &&
        items.every(
          (item: any) => get(item, "HostRoles.desired_state") === "STARTED"
        )
    );
    setIsNextEnabled(isNextEnabledLocal);
    if (!isNextEnabledLocal) {
      window.setTimeout(() => {
        pullCheckPointsStatuses();
      }, 2000);
    }
  }

  async function pullCheckPointsStatuses() {
    const nameSpaces = hdfsModel?.["namespaces"] || [];
    const nameSpaceCount = nameSpaces.length;
    if (nameSpaceCount > 1) {
      let hostNames = map(hdfsModel?.activeNameNodes, "hostName");
      if (hostNames.length < nameSpaceCount) {
        nameSpaces.forEach((nameSpace: any) => {
          const { hosts } = nameSpace,
            hasActiveNameNode = hosts.some((hostName: any) =>
              hostNames.includes(hostName)
            );
          if (!hasActiveNameNode) {
            const hostForNameSpace =
              hosts.find((hostName: any) => {
                const hostComponents = map(
                  find(hdfsModel?.masterComponents, [
                    "componentName",
                    "NAMENODE",
                  ])?.hostComponents,
                  "HostRoles"
                );
                return find(hostComponents, (hostComponent: any) => {
                  return (
                    hostComponent.host_name === hostName &&
                    hostComponent.state === "STARTED"
                  );
                });
              }) || hosts[0];
            hostNames.push(hostForNameSpace);
          }
        });
      }
      try {
        const data = await adminApi.getNnCheckPointStatus(
          clusterName,
          hostNames.join(",")
        );
        // const isNamenodeStarted = data.HostRoles.desired_state === "STARTED";
        checkNnCheckPointsStatuses(data);
      } catch (err) {
        console.error("Error in fetching checkpoint status", err);
      }
    } else {
      pullCheckPointStatus();
    }
  }
  function getNamenodeHost() {
    return hdfsModel?.activeNameNodes?.[0]?.hostName;
  }
  useEffect(() => {
    if (hdfsModel?.isNamespaceLoaded) {
      pullCheckPointsStatuses();
    }
  }, [JSON.stringify(hdfsModel)]);

  return (
    <>
      <div>
        <div className="step-title">
          Save Namespace:
        </div>
        <Card className="mt-4">
          <Card.Body>
            <ListGroup>
              <ol>
                <li className="fs-12">
                  Login to NameNode host{" "}
                  <span className="fw-bolder fs-12">{getNamenodeHost()}</span>
                </li>
                <li className="mt-3 fs-12">
                  Put the NameNode in Safe Mode (read only mode):
                  <div className="code-snippet fs-12 mt-2">
                    sudo su hdfs -l -c 'hdfs dfsadmin -safemode enter'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  Once in Safe Mode, create a Checkpoint:
                  <div className="code-snippet mt-2">
                    sudo su hdfs -l -c 'hdfs dfsadmin -saveNamespace'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  You will be able to proceed once Ambari detects that the
                  NameNode is in Safe Mode and the Checkpoint has been created
                  successfully.
                </li>
              </ol>
            </ListGroup>
            {/* <Alert variant="warning" className="mt-4 fs-14">
              If the <span className="fw-bold">Next</span> button is enabled
              before you run the
              <span className="fw-bold">
                "Step 4: Create a Checkpoint"
              </span>{" "}
              command, it means there is a recent Checkpoint already and you may
              proceed without running the "Step 4: Create a Checkpoint" command.
            </Alert> */}
          </Card.Body>
        </Card>
      </div>
      <WizardFooter
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
        sideItems={isNextEnabled?"Checkpoint created":"Checkpoint not created yet"}
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onNext={() => {
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={()=>{
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step3;
