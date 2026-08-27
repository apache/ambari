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

import { useContext,  useState } from "react";
import AssignMasters from "../../components/AssignMasters";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./clusterStore/types";
import { get } from "lodash";
import wizardSteps from "./wizardSteps";
import { BootStatus } from "./Step3";
import { ContextWrapper } from ".";
import AssignMastersAddable from "../../components/AssignMastersAddable";
import { Card, CardBody } from "react-bootstrap";
import Modal from "../../components/Modal";
import {
  nextAddServiceStep,
  previousAddServiceStep,
} from "../Services/AddServiceWizard/addServiceNavigation";

function Step5({ wizardName = "clusterCreation" }) {
  const { Context } = useContext(ContextWrapper);
  const {
    state,
    dispatch,
    flushStateToDb,
    installedHosts,
    installedServices,
    stepWizardUtilities: {
      handleNextImperitive,
      currentStep,
      handleBackImperitive,
      jumpToStep,
    },
  } = useContext(Context) as any;
  const [canProcced, setCanProceed] = useState(wizardName === "addService");
  const [hasValidationIssues, setHasValidationIssues] = useState(false);
  const [showValidationIssuesModal, setShowValidationIssuesModal] =
    useState(false);
  const servicesData: any = get(
    state,
    `${wizardName}Steps.SERVICES.data.services`,
    {}
  );
  
  const step1Data = get(state, `${wizardName}Steps.VERSION.data`, {});
  const services = Object.keys(servicesData).filter((service) => {
    return servicesData[service].selected;
  });
  const addServiceFlow = get(
    state,
    "addServiceSteps.SERVICES.data.addServiceFlow",
    {},
  );
  const hostsData = get(
    state,
    `${wizardName}Steps.${wizardSteps[3].name}.data.hosts`,
    []
  );
  const hostsList =
    wizardName === "addService"
      ? installedHosts
      : hostsData
          .filter((host: any) => {
            return host.bootStatus === BootStatus.REGISTERED;
          })
          .map((host: any) => host.name);

  return (
    <>
      {wizardName === "addService" ? (
        <Card>
          <CardBody>
            <AssignMastersAddable
            wizardName={wizardName}
             isInstallFlow={true}
              services={services}
              servicesData={servicesData}
              dispatch={(data: any) => {
                dispatch({
                  type: ActionTypes.STORE_INFORMATION,
                  payload: {
                    step: currentStep.name,
                    data,
                  },
                });
              }}
            />
          </CardBody>
        </Card>
      ) : (
        <AssignMasters
          STACK={step1Data?.selectedVersion?.stack_name}
          VERSION={step1Data?.selectedVersion?.stack_version}
          hostsList={hostsList}
          services={services}
          installedServices={installedServices}
          setCanProceed={setCanProceed}
          parentState={state}
          setHasValidationIssues={setHasValidationIssues}
          dispatch={(data: any) => {
            dispatch({
              type: ActionTypes.STORE_INFORMATION,
              payload: {
                step: currentStep.name,
                data,
              },
            });
          }}
        />
      )}
      <Modal
        isOpen={showValidationIssuesModal}
        onClose={() => setShowValidationIssuesModal(false)}
        modalTitle="Validation Issues"
        modalBody="The master assignments contain validation issues. Continue anyway?"
        options={{ okButtonText: "Continue Anyway" }}
        successCallback={async () => {
          setShowValidationIssuesModal(false);
          await Promise.resolve(flushStateToDb("next"));
          handleNextImperitive();
        }}
      />
      <WizardFooter
        step={currentStep}
        lifted
        isNextEnabled={canProcced}
        onNext={async () => {
          if (wizardName === "addService") {
            const nextStep = nextAddServiceStep(2, addServiceFlow);
            await Promise.resolve(flushStateToDb("jump", nextStep));
            jumpToStep(nextStep);
          } else if (hasValidationIssues) {
            setShowValidationIssuesModal(true);
          } else {
            await Promise.resolve(flushStateToDb("next"));
            handleNextImperitive();
          }
        }}
        onCancel={() => flushStateToDb("cancel")}
        onBack={async () => {
          if (wizardName === "addService") {
            const previousStep = previousAddServiceStep(2, addServiceFlow);
            await Promise.resolve(flushStateToDb("jump", previousStep));
            jumpToStep(previousStep);
          } else {
            await Promise.resolve(flushStateToDb("back"));
            handleBackImperitive();
          }
        }}
      />
    </>
  );
}

export default Step5;
