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
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "./KerberosStore/context"
import { AppContext } from "../../store/context";
import { Alert } from "react-bootstrap";
import { translate } from "../../Utils/Utility";
import { ActionTypes } from "./KerberosStore/types";
import { get } from "lodash";
import { useNavigate } from "react-router";
import OperationsProgress from "../../components/OperationProgress";

function StartAndTestServices() {

  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { wizardSteps, currentStep, handleBackImperitive},
  } = useContext(EnableKerberosContext);

  const [completionStatus, setCompletionStatus] = useState(false);
  const [ nextEnabled, setNextEnabled ] = useState(true)
  const [stepOperations, setStepOperations] = useState<any>([]);
  const { clusterName } = useContext(AppContext);
  const navigate = useNavigate();
  
  useEffect(()=>{
    if(completionStatus){
      setNextEnabled(true);
    }
  },[completionStatus])


  const initialOperations = [
    {
      id: "1",
      label: "Start And Test Services",
      skippable: false,
      context: "Start services",
      callback: async () => {
        const startAndTestServicesPayload = {
            "RequestInfo": {
                "context": "Start services",
                "operation_level": {
                    "level": "CLUSTER",
                    "cluster_name": `${clusterName}`
                }
            },
            "Body": {
                "ServiceInfo": {
                    "state": "STARTED"
                }
            }
        };
        const requestData = await RequestApi.startServices(
          clusterName,
          startAndTestServicesPayload,
          "run_smoke_test=true"
        );
        return requestData;
      },
    },
  ];

  const savedOperationsState = get(
    state,
    `kerberosWizardSteps.${wizardSteps[8].name}.data.operationsState`,
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
        <Alert variant="success">{translate("admin.kerberos.wizard.step8.notice.completed")}</Alert>
      }
      <OperationsProgress
        operations={stepOperations as any}
        title="Start And Test Services"
        description="Start and Test Services"
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
          navigate(`/main/admin/kerberos/`);
          window.location.reload();
        }}
        onCancel={() => {
          navigate(`/main/admin/kerberos/`);
          window.location.reload();
        }}
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
      />
    </>
  );
}

export default StartAndTestServices;
