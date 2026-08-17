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
import { useNavigate } from "react-router-dom";
import OperationsProgress from "../../components/OperationsProgress";
import { ProgressStatus } from "../../constants";
import ClusterApi from "../../api/clusterApi";
import { kerberosWizardPersistenceResetPayload } from "../../Utils/kerberosWizard";
import { responseErrorMessage } from "../../Utils/httpError";

function StartAndTestServices() {

  const {
    state,
    dispatch,
    stepWizardUtilities: { wizardSteps, currentStep},
  } = useContext(EnableKerberosContext);

  const [completionStatus, setCompletionStatus] = useState(false);
  const [ nextEnabled, setNextEnabled ] = useState(false)
  const [hasFailed, setHasFailed] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const [isCompleting, setIsCompleting] = useState(false);
  const [completionError, setCompletionError] = useState("");
  const { clusterName, ambariProperties } = useContext(AppContext);
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
          `run_smoke_test=${ambariProperties?.["skip.service.checks"] !== "true"}`
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

  const completeWizard = async () => {
    if (isCompleting) {
      return;
    }
    setIsCompleting(true);
    setCompletionError("");
    try {
      await ClusterApi.postPersistData(
        kerberosWizardPersistenceResetPayload(),
      );
      navigate(`/main/admin/kerberos/`);
      window.location.reload();
    } catch (error) {
      setCompletionError(
        responseErrorMessage(
          error,
          "Ambari could not clear the Kerberos wizard recovery state.",
        ),
      );
      setIsCompleting(false);
    }
  };


  return (
    <>
      { completionStatus &&
        <Alert variant="success">{translate("admin.kerberos.wizard.step8.notice.completed")}</Alert>
      }
      {completionError && <Alert variant="danger">{completionError}</Alert>}
      <OperationsProgress
        operations={stepOperations as any}
        title="Start And Test Services"
        description="Start and Test Services"
        setCompletionStatus={setCompletionStatus}
        dispatch={(operationsState: any) => {
          const failed = operationsState.some(
            (operation: any) => operation.status === ProgressStatus.FAILED,
          );
          setHasFailed(failed);
          if (failed) {
            setNextEnabled(true);
          }
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
      {hasFailed && (
        <Alert variant="warning">
          Some services failed to start or pass their checks. Complete the wizard and repair them from the service pages, or retry the failed operation.
        </Alert>
      )}
      <WizardFooter
        isNextEnabled={nextEnabled && !isCompleting}
        step={currentStep}
        onNext={() => void completeWizard()}
        onCancel={() => void completeWizard()}
        onBack={() => {}}
      />
    </>
  );
}

export default StartAndTestServices;
