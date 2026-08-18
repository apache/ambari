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
import { find, map } from "lodash";
import adminApi from "../../../../api/adminApi";
import { AppContext } from "../../../../store/context";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { Alert, Card, ListGroup } from "react-bootstrap";
import { ManageJournalNodesContext } from "./store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import usePolling from "../../../../hooks/usePolling";
import {
  evaluateCheckpointSet,
  evaluateNameNodeCheckpoint,
  getHdfsNamespaces,
  getHdfsUser,
} from "../haWorkflowUtils";
import ConfigsApi from "../../../../api/configsApi";

function Step3() {
  const { clusterName } = useContext(AppContext);
  const {
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive,handleBackImperitive },
  } = useContext(ManageJournalNodesContext);
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const { allServiceModels } = useContext(ServiceContext);
  const [isActiveNameNodesStarted, setIsActiveNameNodesStarted] =
    useState(true);
  const [pollError, setPollError] = useState("");
  const [hdfsUser, setHdfsUser] = useState("hdfs");
  const hdfsModel = allServiceModels["hdfs"];
  const { pausePolling } = usePolling(pullCheckPointsStatuses);

  useEffect(() => {
    async function loadHdfsUser() {
      try {
        const configData = await ConfigsApi.getConfigValues(clusterName, "HDFS");
        setHdfsUser(getHdfsUser(configData));
      } catch {
        setHdfsUser("hdfs");
      }
    }
    if (clusterName) void loadHdfsUser();
  }, [clusterName]);

  function getNamespaceTargets() {
    const namespaces = getHdfsNamespaces(hdfsModel);
    const activeHosts = map(hdfsModel?.activeNameNodes, "hostName");
    const hostComponents = map(
      find(hdfsModel?.masterComponents, ["componentName", "NAMENODE"])
        ?.hostComponents,
      "HostRoles",
    );

    return namespaces.map((namespace: any) => {
      const hosts = namespace.hosts || [];
      const activeHost = hosts.find((host: string) => activeHosts.includes(host));
      const startedHost = hosts.find((host: string) =>
        hostComponents.some(
          (component: any) =>
            component.host_name === host && component.state === "STARTED",
        ),
      );
      return {
        nameserviceId: namespace.name,
        hostName: activeHost || startedHost || hosts[0],
      };
    });
  }

  async function pullCheckPointStatus() {
    const hostName = getNamespaceTargets()[0]?.hostName;
    if (!hostName) {
      setPollError("No NameNode is available for checkpoint validation.");
      return;
    }
    try {
      const data = await adminApi.getNnCheckPointStatus(clusterName, hostName);
      const evaluation = evaluateNameNodeCheckpoint(data);
      setIsActiveNameNodesStarted(evaluation.started);
      setPollError(evaluation.error || "");
      setIsNextEnabled(evaluation.ready);
      if (evaluation.ready) pausePolling();
    } catch (err) {
      console.error("Error in fetching checkpoint status", err);
      setIsNextEnabled(false);
      setPollError(
        "Ambari could not read the NameNode checkpoint status. Polling will retry.",
      );
    }
  }

  function checkNnCheckPointsStatuses(data: any) {
    const items = data?.items || [],
      expectedHosts = getNamespaceTargets().map((target: any) => target.hostName);
    const evaluation = evaluateCheckpointSet(expectedHosts, items || []);
    setIsActiveNameNodesStarted(evaluation.started);
    setPollError(evaluation.error || "");
    setIsNextEnabled(evaluation.ready);
    if (evaluation.ready) pausePolling();
  }

  async function pullCheckPointsStatuses() {
    if (!hdfsModel?.isNamespaceLoaded) return;
    const targets = getNamespaceTargets();
    const nameSpaceCount = targets.length;
    if (nameSpaceCount > 1) {
      const hostNames = targets.map((target: any) => target.hostName);
      try {
        const data = await adminApi.getNnCheckPointStatuses(
          clusterName,
          hostNames.join(",")
        );
        checkNnCheckPointsStatuses(data);
      } catch (err) {
        console.error("Error in fetching checkpoint status", err);
        setIsNextEnabled(false);
        setPollError(
          "Ambari could not read all NameNode checkpoint statuses. Polling will retry.",
        );
      }
    } else {
      pullCheckPointStatus();
    }
  }
  const namespaceTargets = getNamespaceTargets();
  const isFederated = namespaceTargets.length > 1;

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
                {namespaceTargets.map((target: any) => (
                  <li className="fs-12 mb-3" key={target.nameserviceId}>
                    Login to NameNode host{" "}
                    <span className="fw-bolder fs-12">{target.hostName}</span>
                    <div className="code-snippet fs-12 mt-2">
                      sudo su {hdfsUser} -l -c 'hdfs dfsadmin
                      {isFederated
                        ? ` -fs hdfs://${target.nameserviceId}`
                        : ""}{" "}
                      -safemode enter'
                    </div>
                    <div className="code-snippet mt-2">
                      sudo su {hdfsUser} -l -c 'hdfs dfsadmin
                      {isFederated
                        ? ` -fs hdfs://${target.nameserviceId}`
                        : ""}{" "}
                      -saveNamespace'
                    </div>
                  </li>
                ))}
                <li className="mt-3 fs-12">
                  You will be able to proceed once Ambari detects that the
                  NameNode is in Safe Mode and the Checkpoint has been created
                  successfully.
                </li>
              </ol>
            </ListGroup>
            {!isActiveNameNodesStarted ? (
              <Alert variant="danger" className="mt-3">
                Every NameNode selected for checkpoint validation must be started.
              </Alert>
            ) : null}
            {pollError ? (
              <Alert variant="danger" className="mt-3">
                {pollError}
              </Alert>
            ) : null}
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
        onBack={async () => {
          await flushStateToDb("back");
          await handleBackImperitive();
        }}
        sideItems={isNextEnabled?"Checkpoint created":"Checkpoint not created yet"}
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step3;
