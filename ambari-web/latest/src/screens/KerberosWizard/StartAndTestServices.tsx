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
import { AppContext } from "../../store/context";
import { Alert } from "react-bootstrap";
import { translate } from "../../Utils/Utility";
import { get } from "lodash";
import { useNavigate } from "react-router";
import { RequestApi } from "../../api/requestApi";
import OperationsProgress from "../../components/OperationProgress";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "./KerberosStore/context";
import { ActionTypes } from "./KerberosStore/types";

function StartAndTestServices() {

  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { wizardSteps, currentStep, handleBackImperitive},
  } = useContext(EnableKerberosContext);

  const [completionStatus, setCompletionStatus] = useState(false);
  const [ nextEnabled, setNextEnabled ] = useState(false)
  const { clusterName } = useContext(AppContext);
  const navigate = useNavigate();
  
  useEffect(()=>{
    if(completionStatus){
      setNextEnabled(true);
    }
  },[completionStatus])

  const operations = [
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

  return (
    <>
      { completionStatus &&
        <Alert variant="success">{translate("admin.kerberos.wizard.step8.notice.completed")}</Alert>
      }
      <OperationsProgress
        operations={
          (get(
            state,
            `kerberosWizardSteps.${wizardSteps[8].name}.data.operationsState`,
            null
          ) as any) ||
          (operations as any)}
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
        }}
        onCancel={() => {
          navigate(`/main/admin/kerberos/`);
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