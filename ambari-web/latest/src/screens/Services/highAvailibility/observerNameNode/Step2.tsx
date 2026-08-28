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
import { AppContext } from "../../../../store/context";
import { map } from "lodash";
import { AddObserverNamenodeContext } from "./store/context";
import { ActionTypes } from "./store/types";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import AssignMastersAddable from "../../../../components/AssignMastersAddable";
import { Card } from "react-bootstrap";
import { getStepData } from "../../../../Utils/Utility";
import { addObserverNamenodeSteps } from "./wizardSteps";

function Step2() {
  const { services } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { handleNextImperitive, currentStep },
  } = useContext(AddObserverNamenodeContext);
  const [isNextEnabled] = useState(true);
  const nameServiceId = getStepData(
    state,
    addObserverNamenodeSteps.GET_STARTED,
    "nameServiceId",
    "addObserverNamenodeSteps"
  );
  return (
    <>
      <div className="step-title">Select Hosts</div>
      <div className="step-description">
        {`Select hosts running the NameNodes for ${nameServiceId || ""}`.trim()}
      </div>
      <Card className="mt-2">
        <Card.Body>
          <AssignMastersAddable
            mastersToShow={["NAMENODE"]}
            mastersToAdd={["NAMENODE"]}
            mastersToCreate={[]}
            showCurrentPrefix={["NAMENODE"]}
            showAdditionalPrefix={["NAMENODE"]}
            services={map(services, "ServiceInfo.service_name")}
            dispatch={(payload: any) => {
              dispatch({
                type: ActionTypes.STORE_INFORMATION,
                payload: {
                  step: currentStep.name,
                  data: payload,
                },
              });
              flushStateToDb();
            }}
          />
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
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
export default Step2;
