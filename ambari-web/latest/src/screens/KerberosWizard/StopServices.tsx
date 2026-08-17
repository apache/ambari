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
import { Alert, Button } from "react-bootstrap";
import { translate } from "../../Utils/Utility";
import { get } from "lodash";
import { ActionTypes } from "./KerberosStore/types";
import OperationsProgress from "../../components/OperationsProgress";
import KerberosApi from "../../api/kerberosApi";
import {
  appTimelineServerHost,
  doesAppTimelineServerSupportKerberos,
  isMissingHostComponentError,
} from "../../Utils/kerberosWizard";
import { responseErrorMessage } from "../../Utils/httpError";

export default function StopServices() {
  const {
    state,
    dispatch,
    flushStateToDb,
    onExitPopUp,
    stepWizardUtilities: { wizardSteps, currentStep, handleNextImperitive, jumpToStep},
  } = useContext(EnableKerberosContext);

  const [completionStatus, setCompletionStatus] = useState(false);
  const [nextEnabled, setNextEnabled] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const [setupError, setSetupError] = useState("");
  const [setupRetry, setSetupRetry] = useState(0);
  const { clusterName, services, serviceComponentInfo } = useContext(AppContext);

  useEffect(() => {
    if (completionStatus) {
      setNextEnabled(true);
    }
  }, [completionStatus]);


  const savedOperationsState = get(
    state,
    `kerberosWizardSteps.${wizardSteps[6].name}.data.operationsState`,
    null
  );

  useEffect(() => {
    let active = true;
    const loadOperations = async () => {
      setSetupError("");
      setStepOperations([]);
      setCompletionStatus(false);
      setNextEnabled(false);
      try {
        let timelineServerHost = "";
        const yarnInstalled = services.some(
          (service: any) => service?.ServiceInfo?.service_name === "YARN",
        );
        if (
          yarnInstalled
          && !doesAppTimelineServerSupportKerberos(serviceComponentInfo)
        ) {
          timelineServerHost = appTimelineServerHost(
            await KerberosApi.getAppTimelineServerHosts(clusterName),
          );
        }

        const operations = [
          {
            id: "1",
            label: "Stop services",
            skippable: false,
            context: "Stop services",
            callback: async () => RequestApi.stopServices(clusterName, {
              RequestInfo: {
                context: "Stop services",
                operation_level: {
                  level: "CLUSTER",
                  cluster_name: clusterName,
                },
              },
              Body: {
                ServiceInfo: {
                  state: "INSTALLED",
                },
              },
            }),
          },
          ...(timelineServerHost ? [{
            id: "2",
            label: "Delete ATS",
            skippable: false,
            context: "Delete ATS",
            callback: async () => {
              try {
                return await KerberosApi.deleteAppTimelineServer(
                  clusterName,
                  timelineServerHost,
                );
              } catch (error) {
                if (isMissingHostComponentError(error)) {
                  return { status: 204 };
                }
                throw error;
              }
            },
          }] : []),
        ];

        const recoveredOperations = Array.isArray(savedOperationsState)
          ? operations.map((originalOperation) => {
              const savedOperation = savedOperationsState.find(
                (saved: any) => saved.id === originalOperation.id,
              );
              return savedOperation
                ? {
                    ...originalOperation,
                    ...savedOperation,
                    callback: originalOperation.callback,
                  }
                : originalOperation;
            })
          : operations;
        if (active) {
          setStepOperations(recoveredOperations);
        }
      } catch (error) {
        if (active) {
          setSetupError(responseErrorMessage(
            error,
            "Ambari could not determine whether the Application Timeline Server must be removed.",
          ));
        }
      }
    };

    void loadOperations();
    return () => {
      active = false;
    };
  }, [
    clusterName,
    savedOperationsState,
    serviceComponentInfo,
    services,
    setupRetry,
  ]);


  if (setupError) {
    return (
      <Alert variant="danger">
        <div>{setupError}</div>
        <Button className="mt-3" onClick={() => setSetupRetry((value) => value + 1)}>
          Retry
        </Button>
      </Alert>
    );
  }

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
          flushStateToDb("jump", 4);
          jumpToStep(4, true);
        }}
      />
    </>
  );
}
