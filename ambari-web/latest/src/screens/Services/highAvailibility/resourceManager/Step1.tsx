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
import { Alert } from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { EnableHighAvailibilityContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableResourceManagerSteps } from "./wizardSteps";
import { responseErrorMessage } from "./rmHaUtils";

function Step1() {
  const {
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(EnableHighAvailibilityContext);
  const [persistenceError, setPersistenceError] = useState("");

  async function continueToHostSelection() {
    setPersistenceError("");
    try {
      [
        enableResourceManagerSteps.SELECT_HOSTS,
        enableResourceManagerSteps.REVIEW,
        enableResourceManagerSteps.CONFIGURE_COMPONENTS,
      ].forEach((key) => {
        dispatch({ type: ActionTypes.REMOVE_KEY, payload: { key } });
      });
      await flushStateToDb("next");
      await handleNextImperitive();
    } catch (error) {
      setPersistenceError(
        responseErrorMessage(
          error,
          "Ambari could not save the ResourceManager HA checkpoint.",
        ),
      );
    }
  }

  async function cancelWorkflow() {
    setPersistenceError("");
    try {
      await flushStateToDb("cancel");
    } catch (error) {
      const message = responseErrorMessage(
        error,
        "Ambari could not clear the ResourceManager HA workflow.",
      );
      setPersistenceError(message);
      throw new Error(message);
    }
  }
  return (
    <>
      <h3 className="step-title">Get Started</h3>
      <h5 className="step-description light-text">
        This wizard will walk you through enabling ResourceManager HA on your
        cluster.
        <br />
        Once enabled, you will be running a Standby ResourceManager in addition
        to your Active ResourceManager.
        <br />
        This allows for an Active-Standby ResourceManager configuration that
        automatically performs failover.
      </h5>
      <div className="fw-bold fs-12">
        You should plan a cluster maintenance window and prepare for cluster
        downtime when enabling ResourceManager HA.
      </div>
      {persistenceError && (
        <Alert variant="danger" className="mt-3">
          {persistenceError}
        </Alert>
      )}
      <WizardFooter
        step={currentStep}
        isNextEnabled={true}
        onBack={() => undefined}
        onNext={() => void continueToHostSelection()}
        onCancel={cancelWorkflow}
      />
    </>
  );
}

export default Step1;
