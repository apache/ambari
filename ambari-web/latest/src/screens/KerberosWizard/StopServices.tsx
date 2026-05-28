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
import { RequestApi } from "../../api/requestApi";
import { EnableKerberosContext } from "./KerberosStore/context";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { AppContext } from "../../store/context";
import { Alert } from "react-bootstrap";
import { translate } from "../../Utils/Utility";
import { get } from "lodash";
import { ActionTypes } from "./KerberosStore/types";
import OperationsProgress from "../../components/OperationsProgress";

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
  const [stepOperations, setStepOperations] = useState<any>([]);
  const { clusterName } = useContext(AppContext);

  useEffect(() => {
    if (completionStatus) {
      setNextEnabled(true);
    }
  }, [completionStatus]);


  const initialOperations = [
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

  const savedOperationsState = get(
    state,
    `kerberosWizardSteps.${wizardSteps[6].name}.data.operationsState`,
    null
  );

  useEffect(() => {
    const operations = (() => {
      if (savedOperationsState && Array.isArray(savedOperationsState)) {
        return initialOperations.map((originalOp) => {
          const savedOp = savedOperationsState.find(
            (saved: any) => saved.id === originalOp.id
          );
          return savedOp
            ? { ...originalOp, ...savedOp, callback: originalOp.callback }
            : originalOp;
        });
      }

      return initialOperations;
    })();
    setStepOperations(operations);
  }, [JSON.stringify(savedOperationsState)]);


  if(!stepOperations || stepOperations.length===0){
    return <div>Loading...</div>
  }


  return (
    <>
      { completionStatus &&
        <Alert variant="success">{translate("admin.kerberos.wizard.step6.notice.completed")}</Alert>
      }
      <OperationsProgress
        operations={stepOperations as any}
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
