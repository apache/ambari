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
import { EnableHighAvailibilityContext } from "./store/context";
import { getStepData } from "../../../../Utils/Utility";
import { filter, find } from "lodash";
import adminApi from "../../../../api/adminApi";
import { AppContext } from "../../../../store/context";
import usePolling from "../../../../hooks/usePolling";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { Alert, Card, ListGroup } from "react-bootstrap";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { evaluateNameNodeCheckpoint } from "../haWorkflowUtils";

function Step4() {
  const { clusterName } = useContext(AppContext);
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const [namenodeHost, setNamenodeHost] = useState("");
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const [isNameNodeStarted, setIsNameNodeStarted] = useState(true);
  const [pollError, setPollError] = useState("");
  const [kdcError, setKdcError] = useState("");
  const { pausePolling } = usePolling(pullCheckPointStatus);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const hdfsUser =
    getStepData(
      state,
      "GET_STARTED",
      "hdfsUser",
      "enableHighAvailibilitySteps",
    ) || "hdfs";

  async function pullCheckPointStatus() {
    const masterComponentHosts = getStepData(
      state,
      "SELECT_HOSTS",
      "masterComponentHosts",
      "enableHighAvailibilitySteps"
    );
    const hostName = find(
      filter(masterComponentHosts, ["component", "NAMENODE"]),
      ["isInstalled", true]
    )?.hostName;
    setNamenodeHost(hostName);

    try {
      const data = await adminApi.getNnCheckPointStatus(clusterName, hostName);
      const evaluation = evaluateNameNodeCheckpoint(data);
      setIsNameNodeStarted(evaluation.started);
      setPollError(evaluation.error || "");
      setIsNextEnabled(evaluation.ready);
      if (evaluation.ready) {
        setIsNextEnabled(true);
        pausePolling();
      }
    } catch (err) {
      console.error("Error in fetching checkpoint status", err);
      setIsNextEnabled(false);
      setPollError(
        "Ambari could not read the NameNode checkpoint status. Polling will retry.",
      );
    }
  }

  return (
    <>
      <div>
        <div className="step-title">
          Manual Steps Required: Create Checkpoint on NameNode
        </div>
        <Card className="mt-4">
          <Card.Body>
            <ListGroup>
              <ol>
                <li className="fs-12">
                  Login to NameNode host{" "}
                  <span className="fw-bolder fs-12">{namenodeHost}</span>
                </li>
                <li className="mt-3 fs-12">
                  Put the NameNode in Safe Mode (read only mode):
                  <div className="code-snippet fs-12 mt-2">
                    sudo su {hdfsUser} -l -c 'hdfs dfsadmin -safemode enter'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  Once in Safe Mode, create a Checkpoint:
                  <div className="code-snippet mt-2">
                    sudo su {hdfsUser} -l -c 'hdfs dfsadmin -saveNamespace'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  You will be able to proceed once Ambari detects that the
                  NameNode is in Safe Mode and the Checkpoint has been created
                  successfully.
                </li>
              </ol>
            </ListGroup>
            <Alert variant="warning" className="mt-4 fs-14">
              If the <span className="fw-bold">Next</span> button is enabled
              before you run the
              <span className="fw-bold">
                "Step 4: Create a Checkpoint"
              </span>{" "}
              command, it means there is a recent Checkpoint already and you may
              proceed without running the "Step 4: Create a Checkpoint" command.
            </Alert>
            {!isNameNodeStarted ? (
              <Alert variant="danger" className="mt-3">
                The current NameNode is not started.
              </Alert>
            ) : null}
            {pollError ? (
              <Alert variant="danger" className="mt-3">
                {pollError}
              </Alert>
            ) : null}
            {kdcError ? (
              <Alert variant="danger" className="mt-3">
                {kdcError}
              </Alert>
            ) : null}
          </Card.Body>
        </Card>
      </div>
      <WizardFooter
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(3);
        }}
        step={currentStep}
        isNextEnabled={isNextEnabled}
        sideItems={isNextEnabled?"Checkpoint created":null}
        onNext={() => {
          setKdcError("");
          void getKDCSessionState(
            async () => {
              await flushStateToDb("next");
              await handleNextImperitive();
            },
            (error: any) => {
              setKdcError(
                error?.response?.data?.message ||
                  error?.message ||
                  "Ambari could not validate the KDC session.",
              );
            },
          );
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step4;
