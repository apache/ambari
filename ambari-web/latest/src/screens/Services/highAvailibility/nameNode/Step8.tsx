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
import { EnableHighAvailibilityContext } from "./store/context";
import { Alert, Card, ListGroup } from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { filter, find } from "lodash";
import { getStepData } from "../../../../Utils/Utility";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import ConfirmationModal from "../../../../components/ConfirmationModal";

function Step8() {
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const [isNextEnabled] = useState(true);
  const [namenodeHost, setNamenodeHost] = useState("");
  const [additionalNamenodeHost, setAdditionalNamenodeHost] = useState("");
  const [showCompletionConfirmation, setShowCompletionConfirmation] =
    useState(false);
  const [validationError, setValidationError] = useState("");
  const [isFinalizing, setIsFinalizing] = useState(false);
  const { getKDCSessionState } = useKDCSessionState(() => {});

  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilitySteps"
  );
  const hdfsUser =
    getStepData(
      state,
      "GET_STARTED",
      "hdfsUser",
      "enableHighAvailibilitySteps",
    ) || "hdfs";
  useEffect(() => {
    const hostName = find(
      filter(masterComponentHosts, ["component", "NAMENODE"]),
      ["isInstalled", true]
    )?.hostName;
    setNamenodeHost(hostName);
    const additionalHostname = find(
      filter(masterComponentHosts, ["component", "NAMENODE"]),
      ["isInstalled", false]
    )?.hostName;
    setAdditionalNamenodeHost(additionalHostname);
  }, []);

  return (
    <>
      <ConfirmationModal
        isOpen={showCompletionConfirmation}
        onClose={() => setShowCompletionConfirmation(false)}
        modalTitle="Confirm Manual Steps"
        modalBody="Confirm that formatZK and bootstrapStandby completed successfully before finalizing NameNode HA."
        isOkDisabled={isFinalizing}
        successCallback={async () => {
          setIsFinalizing(true);
          setValidationError("");
          try {
            await flushStateToDb("next");
            await handleNextImperitive();
            setShowCompletionConfirmation(false);
          } catch (error: any) {
            setShowCompletionConfirmation(false);
            setValidationError(
              error?.response?.data?.message ||
                error?.message ||
                "Ambari could not persist the NameNode HA checkpoint.",
            );
          } finally {
            setIsFinalizing(false);
          }
        }}
      />
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
                  Initialize the metadata for NameNode automatic failover by
                  running:
                  <div className="code-snippet fs-12 mt-2">
                    sudo su {hdfsUser} -l -c 'hdfs zkfc -formatZK'
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  Login to the Additional NameNode host {additionalNamenodeHost}
                  <div className="mt-2">
                    <Alert variant="warning" className="fs-12">
                      <strong>Important!</strong>
                      <span className="ms-2">
                        Be sure to login to the Additional NameNode host.
                      </span>
                      <br />
                      <div>
                        This is a different host from the Steps 1 and 2 above.
                      </div>
                    </Alert>
                  </div>
                </li>
                <li className="mt-3 fs-12">
                  Initialize the metadata for the Additional NameNode by
                  running:
                  <div className="code-snippet fs-12 mt-2">
                    sudo su {hdfsUser} -l -c 'hdfs namenode -bootstrapStandby'
                  </div>
                </li>
              </ol>
            </ListGroup>
          </Card.Body>
        </Card>
        <div className="fs-12">
          Please proceed once you have completed the steps above.
        </div>
        {validationError ? (
          <Alert variant="danger" className="mt-3">
            {validationError}
          </Alert>
        ) : null}
      </div>
      <WizardFooter
        onBack={() => {
          jumpToStep(7);
          flushStateToDb("back");
        }}
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onNext={() => {
          setValidationError("");
          void getKDCSessionState(
            () => setShowCompletionConfirmation(true),
            (error: any) => {
              setValidationError(
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
        cancelConfirmationBody="NameNode HA changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Complete the documented manual recovery before making further HDFS topology changes."
      />
    </>
  );
}

export default Step8;
