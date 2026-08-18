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
import { filter, find, map } from "lodash";
import { AppContext } from "../../../../store/context";
import adminApi from "../../../../api/adminApi";
import { enableNamenodeSteps } from "./wizardSteps";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { Alert, Card, ListGroup } from "react-bootstrap";
import usePolling from "../../../../hooks/usePolling";
import { evaluateJournalNodeFormatSet } from "../haWorkflowUtils";

function Step6() {
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName } = useContext(AppContext);
  const [isNextEnabled, setIsNextEnabled] = useState(false);
  const [statusMessage, setStatusMessage] = useState(
    "Waiting for all JournalNodes to be formatted.",
  );
  const [pollError, setPollError] = useState("");
  const nameServiceId = getStepData(
    state,
    enableNamenodeSteps.GET_STARTED,
    "nameserviceId",
    "enableHighAvailibilitySteps"
  );
  const hdfsUser =
    getStepData(
      state,
      enableNamenodeSteps.GET_STARTED,
      "hdfsUser",
      "enableHighAvailibilitySteps",
    ) || "hdfs";
  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilitySteps"
  );

  const { pausePolling } = usePolling(pullCheckPointStatus);

  async function pullCheckPointStatus() {
    const hostNames = map(
      filter(masterComponentHosts, ["component", "JOURNALNODE"]),
      "hostName"
    );
    const responses: Record<string, unknown> = {};
    const requestErrors: string[] = [];
    await Promise.all(
      hostNames.map(async (hostName: string) => {
        try {
          responses[hostName] = await adminApi.getJnCheckPointStatus(
            clusterName,
            hostName,
          );
        } catch (error) {
          console.error(`Could not fetch JournalNode status for ${hostName}`, error);
          requestErrors.push(hostName);
        }
      }),
    );

    const evaluation = evaluateJournalNodeFormatSet(
      hostNames,
      responses,
      nameServiceId,
    );
    if (requestErrors.length) {
      setPollError(
        `Ambari could not read JournalNode status for ${requestErrors.join(", ")}. Polling will retry.`,
      );
    } else {
      setPollError(evaluation.error || "");
    }
    setIsNextEnabled(evaluation.ready);
    if (evaluation.ready) {
      setStatusMessage("All selected JournalNodes are formatted.");
      pausePolling();
    } else if (evaluation.missingHosts.length) {
      setStatusMessage(
        `Waiting for JournalNodes: ${evaluation.missingHosts.join(", ")}`,
      );
    } else if (evaluation.invalidHosts.length) {
      setStatusMessage(
        `JournalNodes not formatted: ${evaluation.invalidHosts.join(", ")}`,
      );
    }
  }

  const namenodeHost = find(
    filter(masterComponentHosts, ["component", "NAMENODE"]),
    ["isInstalled", true]
  )?.hostName;
  return (
    <>
      <div>
        <div className="step-title">
          Manual Steps Required: Initialize JournalNodes
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
                  Initialize the JournalNodes by running:
                  <div className="code-snippet fs-12 mt-2">
                    sudo su {hdfsUser} -l -c 'hdfs namenode -initializeSharedEdits'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  You will be able to proceed once Ambari detects that the
                  JournalNodes have been initialized successfully.
                </li>
              </ol>
            </ListGroup>
            {pollError ? (
              <Alert variant="danger" className="mt-3">
                {pollError}
              </Alert>
            ) : null}
          </Card.Body>
        </Card>
      </div>
      <WizardFooter
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(5);
        }}
        step={currentStep}
        isNextEnabled={isNextEnabled}
        sideItems={statusMessage}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        cancelConfirmationBody="NameNode HA changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Complete the documented manual recovery before making further HDFS topology changes."
      />
    </>
  );
}

export default Step6;
