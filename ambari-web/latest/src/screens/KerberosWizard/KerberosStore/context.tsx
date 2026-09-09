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

import React, { createContext, Dispatch, useContext, useEffect, useReducer, useRef, useState } from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import { get, isEmpty } from "lodash";
import { ClusterProgressStatus } from "../../../constants";
import modalManager from "../../../store/ModalManager";
import ConfirmationModal from "../../../components/ConfirmationModal";
import { AppContext } from "../../../store/context";
import {translate } from "../../../Utils/Utility";
import useClusterNavigate from "../../../hooks/useClusterNavigate";
import { RequestApi } from "../../../api/requestApi";
import KerberosApi from "../../../api/kerberosApi";
import { Alert, Button } from "react-bootstrap";
import Spinner from "../../../components/Spinner";
import { responseErrorMessage } from "../../../Utils/httpError";
import useAuth from "../../../hooks/useAuth";
import useClusterWorkflowPersistence from "../../../hooks/useClusterWorkflowPersistence";
import { containsReentryMarker, workflowErrorMessage } from "../../../Utils/scopedWorkflow";
import { consumeWorkflowReturnPath } from "../../../Utils/workflowReturnPath";

interface KerberosWizardContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?:any;
  flushStateToDb?: any;
  onExitPopUp?: any;
}

export async function discardChanges(clusterName: string) {
    const payload = {
      "Clusters": {
          "security_type": "NONE"
      }
    }
    await RequestApi.preparingOperations(clusterName, payload).catch(() => undefined);
    await KerberosApi.deleteKerberosService(clusterName, "KERBEROS").catch(
      () => undefined,
    );
  }

export const EnableKerberosContext =
  createContext<KerberosWizardContextProps>({
    state: initialState,
    dispatch: () => undefined,
    flushStateToDb: () => undefined,
    onExitPopUp: () => undefined
  });

export const KerberosWizardProvider: React.FC<{
  stepWizardUtilities:any;
  children: React.ReactNode;
  onWizardExitReady?: () => void;
}> = ({ stepWizardUtilities, children, onWizardExitReady }) => {
  const [state, reducerDispatch] = useReducer(reducer, initialState);
  const isDataPersisted = useRef(false);
  const stateRef = useRef<State>(initialState);
  const currStepDataRef = useRef<Record<string, unknown>>({});
  const [isRecoveryLoading, setIsRecoveryLoading] = useState(true);
  const [recoveryLoadError, setRecoveryLoadError] = useState("");
  const [persistenceError, setPersistenceError] = useState("");
  const [reentryRequired, setReentryRequired] = useState(false);
  const { cluster, clusterName, loginName } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const persistence = useClusterWorkflowPersistence("ENABLING_KERBEROS", {
    controllerNames: ["kerberosWizardController"],
    keys: ["ENABLING_KERBEROS", "CLUSTER_STATE"],
  });
  const navigate = useClusterNavigate();
  const hydrationGeneration = useRef(0);
  const stepWizardUtilitiesRef = useRef(stepWizardUtilities);
  stepWizardUtilitiesRef.current = stepWizardUtilities;

  const dispatch: Dispatch<Action> = (action) => {
    stateRef.current = reducer(stateRef.current, action);
    reducerDispatch(action);
  };

  useEffect(() => {
    const generation = ++hydrationGeneration.current;
    void syncUserPersistedData(generation);
    return () => {
      if (hydrationGeneration.current === generation) hydrationGeneration.current += 1;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [canPersist, persistence]);

  useEffect(() => {
    if (isDataPersisted.current && !reentryRequired) {
      void flushCurrentData().catch(() => undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.kerberosWizardSteps, reentryRequired]);

  async function syncUserPersistedData(generation = ++hydrationGeneration.current) {
    isDataPersisted.current = false;
    setIsRecoveryLoading(true);
    setRecoveryLoadError("");
    setPersistenceError("");
    if (!canPersist || !persistence) {
      setRecoveryLoadError(
        !canPersist
          ? translate("workflow.persistence.permissionRequired")
          : translate("workflow.persistence.explicitCluster"),
      );
      setIsRecoveryLoading(false);
      return;
    }
    try {
      const values = await persistence.reload();
      if (hydrationGeneration.current !== generation) return;
      const persistedData = (values?.ENABLING_KERBEROS || initialState) as State;
      const needsReentry = containsReentryMarker(persistedData);
      setReentryRequired(needsReentry);
      const currentWizardUtilities = stepWizardUtilitiesRef.current;
      if (
        !isEmpty(
          get(persistedData, "kerberosWizardSteps", {})
        )
      ) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (needsReentry || get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = needsReentry
            ? "CONFIGURE_KERBEROS"
            : get(persistedData, "activeStep");
          currStepDataRef.current = {
            progressStatus: ClusterProgressStatus.ENABLING_KERBEROS,
            stepName: activeStepName,
          };
          let activeStepNumber = Object.keys(
            currentWizardUtilities.wizardSteps
          ).find((stepName) => {
            return (
              currentWizardUtilities.wizardSteps?.[stepName]?.name ===
              activeStepName
            );
          });
          if (activeStepNumber !== undefined) {
            currentWizardUtilities.jumpToStep(Number(activeStepNumber), true);
          }
        } catch (err) {
          console.error("Error while jumping to step", err);
        }
      } else {
        currentWizardUtilities.jumpToStep(1, true);
      }
      isDataPersisted.current = true;
    } catch (error) {
      if (hydrationGeneration.current !== generation) return;
      setRecoveryLoadError(workflowErrorMessage(
        error,
          translate("workflow.persistence.kerberosLoadFailed"),
      ));
    } finally {
      if (hydrationGeneration.current === generation) setIsRecoveryLoading(false);
    }
  }

  async function flushCurrentData(
    stateSnapshot = stateRef.current,
    stepSnapshot = currStepDataRef.current,
  ) {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    const activeStep = get(stepSnapshot, "stepName", "");
    try {
      await persistence.savePersistData({
        ENABLING_KERBEROS: { ...stateSnapshot, activeStep },
        CLUSTER_STATE: stepSnapshot,
      }, activeStep || ClusterProgressStatus.ENABLING_KERBEROS);
      setReentryRequired(false);
      setPersistenceError("");
    } catch (error) {
      setPersistenceError(responseErrorMessage(
        error,
        translate("workflow.persistence.kerberosSaveFailed"),
      ));
      throw error;
    }
  }

  async function flushOnCancel() {
    if (!persistence) throw new Error(translate("workflow.persistence.explicitCluster"));
    await persistence.release();
    if (onWizardExitReady) {
      onWizardExitReady();
    } else {
      navigate(consumeWorkflowReturnPath({
        clusterId: cluster?.cluster_id,
        principal: loginName,
        workflow: "ENABLING_KERBEROS",
      }, "/main/admin/kerberos/"));
    }
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      let nextStepDetails = stepWizardUtilitiesRef.current.wizardSteps?.[nextStep];
      let nextState = stateRef.current;
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (nextState?.kerberosWizardSteps?.[key]) {
            nextState = reducer(nextState, {
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      if (nextState !== stateRef.current) {
        dispatch({ type: ActionTypes.SYNC_STATE, payload: nextState });
      }
      const nextStepData = {
        progressStatus: ClusterProgressStatus.ENABLING_KERBEROS,
        stepName: nextStepDetails?.name,
      };
      currStepDataRef.current = nextStepData;
      await flushCurrentData(nextState, nextStepData);
    }
  }

  function flushStateToDb(
    operation: string = "default",
    jumpStep: number = -1
  ) {
    let activeStep = Object.keys(stepWizardUtilities.wizardSteps).find(
      (stepName) => {
        return (
          stepWizardUtilities.wizardSteps?.[stepName]?.name ===
          stepWizardUtilities.currentStep.name
        );
      }
    );
    switch (operation) {
      case "cancel":
        return flushOnCancel();
      case "complete":
        return flushOnCancel();
      case "back":
        return flushOnStepChange(Number(activeStep) - 1);
      case "next":
        return flushOnStepChange(Number(activeStep) + 1);
      case "jump":
        return flushOnStepChange(jumpStep);
      default:
        return flushCurrentData();
    }
  }

  function onExitPopUp(isCritical: boolean, skipDiscardChanges: boolean) {
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle={translate("popup.confirmation.commonHeader")}
        modalBody={isCritical ? translate('admin.kerberos.wizard.exit.critical.msg'): translate('admin.kerberos.wizard.exit.warning.msg')}
        successCallback={async () => {
          if(!skipDiscardChanges) {
            await discardChanges(clusterName);
          }
          await flushStateToDb("cancel");
          modalManager.hide();
        }}
      />
    );
  }

  if (isRecoveryLoading) {
    return <Spinner />;
  }

  if (recoveryLoadError) {
    return (
      <Alert variant="danger" className="m-3">
        <div>{recoveryLoadError}</div>
        <Button className="mt-3" onClick={() => void syncUserPersistedData()}>
          {translate("common.retry")}
        </Button>
      </Alert>
    );
  }

  return (
    <EnableKerberosContext.Provider
      value={{ state, dispatch, stepWizardUtilities, flushStateToDb, onExitPopUp }}
    >
      {reentryRequired && (
        <Alert variant="warning" className="m-3">
          {translate("workflow.persistence.reentryRequired")}
        </Alert>
      )}
      {persistenceError && (
        <Alert variant="danger" className="m-3">
          <div>{persistenceError}</div>
          <Button
            className="mt-3"
            onClick={() => void syncUserPersistedData()}
          >
            {translate("common.retry")}
          </Button>
        </Alert>
      )}
      {children}
    </EnableKerberosContext.Provider>
  );
};
