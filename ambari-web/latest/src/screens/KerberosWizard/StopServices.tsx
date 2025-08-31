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
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { AppContext } from "../../store/context";
import { Alert } from "react-bootstrap";
import { translate } from "../../Utils/Utility";
import { get } from "lodash";
import { EnableKerberosContext } from "./KerberosStore/context";
import { RequestApi } from "../../api/requestApi";
import OperationsProgress from "../../components/OperationProgress";
import { ActionTypes } from "./KerberosStore/types";

export default function StopServices() {
  const {
    state,
    dispatch,
    flushStateToDb,
    onExitPopUp,
    stepWizardUtilities: { wizardSteps, currentStep, handleNextImperitive, handleBackImperitive},
  } = useContext(EnableKerberosContext);

  const [completionStatus, setCompletionStatus] = useState(false);
  const [nextEnabled, setNextEnabled] = useState(false);
  const { clusterName } = useContext(AppContext);

  useEffect(() => {
    if (completionStatus) {
      setNextEnabled(true);
    }
  }, [completionStatus]);

  const operations = [
    {
      id: "1",
      label: "Stop services",
      skippable: false,
      context: "Stop services",
      callback: async () => {
        const stopServicesPayload = {
          RequestInfo: {
            context: "Stop services",
            operation_level: {
              level: "CLUSTER",
              cluster_name: `${clusterName}`,
            },
          },
          Body: {
            ServiceInfo: {
              state: "INSTALLED",
            },
          },
        };

        const requestData = await RequestApi.stopServices(
          clusterName,
          stopServicesPayload
        );
        return requestData;
      },
    },
  ];

  return (
    <>
      { completionStatus &&
        <Alert variant="success">{translate("admin.kerberos.wizard.step6.notice.completed")}</Alert>
      }
      <OperationsProgress
        operations={
          (get(
            state,
            `kerberosWizardSteps.${wizardSteps[6].name}.data.operationsState`,
            null
          ) as any) ||
          (operations as any)}
        title="Stop services"
        description="Stop services"
        setCompletionStatus={setCompletionStatus}
        dispatch={(operationsState: any) => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                operationsState,
              },
            },
          });
        }}
      />
      <WizardFooter
        isNextEnabled={nextEnabled}
        step={currentStep}
        onNext={() => {
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={() => {
          onExitPopUp(false, false);
        }}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}
