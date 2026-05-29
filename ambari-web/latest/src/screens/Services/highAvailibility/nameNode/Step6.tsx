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

import { useContext, useEffect, useRef, useState } from "react";
import { EnableHighAvailibilityContext } from "./store/context";
import { getStepData } from "../../../../Utils/Utility";
import { filter, find, map } from "lodash";
import { AppContext } from "../../../../store/context";
import adminApi from "../../../../api/adminApi";
import { enableNamenodeSteps } from "./wizardSteps";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { Card, ListGroup } from "react-bootstrap";
import usePolling from "../../../../hooks/usePolling";

function Step6() {
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName } = useContext(AppContext);
  const status = useRef("waiting");
  const initJnCounter = useRef(0);
  const requestsCounter = useRef(0);
  const hasStoppedJNs = useRef(false);
  const [isNextEnabled, setIsNextEnabled] = useState(true);
  const MINIMAL_JOURNALNODE_COUNT = 3;
  const nameServiceId = getStepData(
    state,
    enableNamenodeSteps.GET_STARTED,
    "nameserviceId",
    "enableHighAvailibilitySteps"
  );
  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilitySteps"
  );

  const {stopPolling,pausePolling,resumePolling}=usePolling(pullCheckPointStatus)

  function resolveJnCheckPointStatus(jNCounter: number) {
    if (jNCounter === MINIMAL_JOURNALNODE_COUNT) {
      status.current = "done";
      pausePolling();
      setIsNextEnabled(true);
    } else {
      if (hasStoppedJNs.current) {
        status.current = "journalnode_stopped";
      } else {
        status.current = "waiting";
      }
      resumePolling();
    }
  }

  async function pullEachJnStatus(hostName: string) {
    try {
      const data = await adminApi.getJnCheckPointStatus(clusterName, hostName);
      let journalStatusInfo;
      let jNCounter = 0;
      if (data?.metrics?.dfs) {
        journalStatusInfo = JSON.parse(
          data.metrics.dfs.journalnode.journalsStatus
        );
        if (
          journalStatusInfo[nameServiceId] &&
          journalStatusInfo[nameServiceId].Formatted === "true"
        ) {
          jNCounter += 1;
        } else {
          hasStoppedJNs.current = true;
        }
        requestsCounter.current = requestsCounter.current + 1;
        if (requestsCounter.current === MINIMAL_JOURNALNODE_COUNT) {
          resolveJnCheckPointStatus(jNCounter);
        }
      }
    } catch (err) {
      console.error("Could not fetch jn status", err);
    }
  }
  function pullCheckPointStatus() {
    initJnCounter.current = 0;
    requestsCounter.current = 0;
    hasStoppedJNs.current = false;
    const hostNames = map(
      filter(masterComponentHosts, ["component", "JOURNALNODE"]),
      "hostName"
    );
    hostNames.forEach(async (hostName: string) => {
      await pullEachJnStatus(hostName);
    });
  }
  useEffect(() => {
    pullCheckPointStatus();
    return ()=>{
      stopPolling()
    }
  }, []);

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
                    sudo su hdfs -l -c 'hdfs namenode -initializeSharedEdits'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  You will be able to proceed once Ambari detects that the
                  JournalNodes have been initialized successfully.
                </li>
              </ol>
            </ListGroup>
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
        onNext={() => {
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step6;
