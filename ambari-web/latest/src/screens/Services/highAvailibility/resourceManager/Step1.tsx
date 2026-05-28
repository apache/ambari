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

import { useContext } from "react";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { EnableHighAvailibilityContext } from "./store/context";

function Step1() {
  const {
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive },
  } = useContext(EnableHighAvailibilityContext);
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
      <WizardFooter
        step={currentStep}
        isNextEnabled={true}
        onBack={() => {
          flushStateToDb("back");
        }}
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

export default Step1;
