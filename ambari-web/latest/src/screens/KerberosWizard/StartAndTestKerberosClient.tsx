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

import { RequestApi } from "../../api/requestApi";
import { useContext, useEffect, useState } from "react";
import WizardFooter from "../../components/StepWizard/WizardFooter";
import { EnableKerberosContext } from "./KerberosStore/context"
import { Alert, Form } from "react-bootstrap";
import { AppContext } from "../../store/context";
import { translate } from "../../Utils/Utility";
import { ActionTypes } from "./KerberosStore/types";
import { get } from "lodash";
import OperationsProgress from "../../components/OperationsProgress";
import KerberosApi from "../../api/kerberosApi";
import { ProgressStatus } from "../../constants";
import { runKerberosClientInstall } from "../../Utils/kerberosWizard";
import useKDCSessionState from "../../hooks/useKDCSessionState";

export default function StartAndTestKerberosClient() {

  const {
    state,
    dispatch,
    flushStateToDb,
    onExitPopUp,
    stepWizardUtilities: { currentStep, wizardSteps, handleNextImperitive, handleBackImperitive},
  } = useContext(EnableKerberosContext);

  const [ completionStatus, setCompletionStatus ] = useState(false);
  const [ignoreErrors, setIgnoreErrors] = useState(false);
  const [hasFailed, setHasFailed] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const { allHostNames, clusterName } = useContext(AppContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});

  const initialOperations = [
    {
      id: 1,
      label: "Install Kerberos Client",
      skippable: false,
      context: "Install Kerberos Service",
      callback: () => runKerberosClientInstall({
        getKerberosClientState: () =>
          KerberosApi.getKerberosClientState(clusterName),
        installKerberosService: () =>
          KerberosApi.installKerberosService(clusterName),
        installKerberosClients: () =>
          KerberosApi.installKerberosClients(clusterName, allHostNames),
      }),
    },
    {
      id: 2,
      label: "Test Kerberos Client",
      skippable: false,
      context: "Kerberos Service Check",
      callback: () => new Promise((resolve, reject) => {
        const TestKerberosClientPayload = {
          "RequestInfo": {
            "context": "Kerberos Service Check",
            "command": "KERBEROS_SERVICE_CHECK",
            "operation_level": {
              "level": "CLUSTER",
              "cluster_name": `${clusterName}`
            }
          },
          "Requests/resource_filters": [
            {
                "service_name": "KERBEROS"
            }
          ]
        }
        void getKDCSessionState(
          async () => {
            resolve(await RequestApi.postRequest(
              clusterName,
              TestKerberosClientPayload,
            ));
          },
          reject,
          { forceCheck: true },
        );
      }),
    },
    {
      id: 3,
      label: "Check Host Heartbeats",
      skippable: false,
      context: "Check Host Heartbeats",
      retryFromOperationId: 1,
      callback: async () => {
        const response = await KerberosApi.getHeartbeatLostHosts(clusterName);
        const hosts = (response?.items ?? []).map(
          (item: any) => item?.Hosts?.host_name,
        ).filter(Boolean);
        if (hosts.length > 0) {
          throw new Error(`Heartbeat is lost for: ${hosts.join(", ")}`);
        }
      },
    },
  ];

  const savedOperationsState = get(
    state,
    `kerberosWizardSteps.${wizardSteps[3].name}.data.operationsState`,
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
        <Alert variant="success">{translate("admin.kerberos.wizard.step3.notice.completed")}</Alert>
      }
      <OperationsProgress
        operations={stepOperations as any}
        title="Install and Test Kerberos Client"
        description="Install and Test Kerberos Client"
        setCompletionStatus={setCompletionStatus}
        dispatch={(operationsState: any) => {
          setHasFailed(operationsState.some(
            (operation: any) => operation.status === ProgressStatus.FAILED,
          ));
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
        <Form.Check
          className="px-3"
          id="kerberos-ignore-client-errors"
          checked={ignoreErrors}
          onChange={(event) => setIgnoreErrors(event.target.checked)}
          label="Ignore errors and continue to the next step"
        />
      )}

      <WizardFooter
        isNextEnabled={completionStatus || (hasFailed && ignoreErrors)}
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
