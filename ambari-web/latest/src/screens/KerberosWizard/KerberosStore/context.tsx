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

import React, {
  createContext,
  Dispatch,
  // useContext,
  useEffect,
  useReducer,
  useRef,
  useState,
} from "react";
import { State, Action, ActionTypes } from "./types";
import { reducer, initialState } from "./reducer";
import ClusterApi from "../../../api/clusterApi";
import { get, isEmpty } from "lodash";
import { ClusterProgressStatus } from "../../../constants";
// import ConfirmationModal from "../../../components/ConfirmationModal";
// import { AppContext } from "../../../store/context";
// import { translate } from "../../../Utils/Utility";
import { useNavigate } from "react-router";
import kerberosApi from "../../../api/kerberosApi";
// import modalManager from "../../../store/ModalManager";
import { RequestApi } from "../../../api/requestApi";

interface KerberosWizardContextProps {
  state: State;
  dispatch: Dispatch<Action>;
  stepWizardUtilities?: any;
  flushStateToDb?: any;
  onExitPopUp?: any;
}

export async function discardChanges(clusterName: string) {
  const payload = {
    Clusters: {
      security_type: "NONE",
    },
  };
  try {
    await RequestApi.preparingOperations(clusterName, payload);
    await kerberosApi.deleteKerberosService(clusterName, "KERBEROS");
  } catch (error) {
    console.log("Unable to remove kerberos", error);
  }
}

export const EnableKerberosContext = createContext<KerberosWizardContextProps>({
  state: initialState,
  dispatch: () => undefined,
  flushStateToDb: () => undefined,
  onExitPopUp: () => undefined,
});

export const KerberosWizardProvider: React.FC<{
  stepWizardUtilities: any;
  children: React.ReactNode;
}> = ({ stepWizardUtilities, children }) => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const isDataPersisted = useRef(false);
  const [currStepData, setCurrStepData] = useState({});
  // const { clusterName } = useContext(AppContext);
  const navigate = useNavigate();

  useEffect(() => {
    syncUserPersistedData();
  }, []);

  useEffect(() => {
    if (isDataPersisted.current) {
      flushCurrentData();
    }
  }, [state.kerberosWizardSteps, currStepData]);

  async function syncUserPersistedData() {
    try {
      const persistedData = await ClusterApi.getPersistData(
        "ENABLING_KERBEROS"
      );
      if (!isEmpty(get(persistedData, "kerberosWizardSteps", {}))) {
        dispatch({
          type: ActionTypes.SYNC_STATE,
          payload: persistedData,
        });
      }
      if (get(persistedData, "activeStep", "")) {
        try {
          const activeStepName = get(persistedData, "activeStep");
          setCurrStepData({
            progressStatus: ClusterProgressStatus.ENABLING_KERBEROS,
            stepName: activeStepName,
          });
          let activeStepNumber = Object.keys(
            stepWizardUtilities.wizardSteps
          ).find((stepName) => {
            return (
              stepWizardUtilities.wizardSteps?.[stepName]?.name ===
              activeStepName
            );
          });
          stepWizardUtilities.jumpToStep(Number(activeStepNumber), true);
        } catch (err) {
          console.error("Error while jumping to step", err);
        }
      } else {
        stepWizardUtilities.jumpToStep(1, true);
      }
    } finally {
      isDataPersisted.current = true;
    }
  }

  async function flushCurrentData() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        ENABLING_KERBEROS: JSON.stringify({
          ...state,
          activeStep: get(currStepData, "stepName", ""),
        }),
        CLUSTER_STATE: JSON.stringify(currStepData),
      })
    );
  }

  async function flushOnCancel() {
    await ClusterApi.postPersistData(
      JSON.stringify({
        ENABLING_KERBEROS: JSON.stringify(initialState),
        CLUSTER_STATE: JSON.stringify({}),
      })
    );
    navigate(`/main/admin/kerberos/`);
  }

  async function flushOnStepChange(nextStep: number) {
    if (nextStep >= 1) {
      let nextStepDetails = stepWizardUtilities.wizardSteps?.[nextStep];
      if (nextStepDetails?.keysToRemove) {
        nextStepDetails.keysToRemove.forEach((key: string) => {
          if (state?.kerberosWizardSteps?.[key]) {
            dispatch({
              type: ActionTypes.REMOVE_KEY,
              payload: { key },
            });
          }
        });
      }
      setCurrStepData({
        progressStatus: ClusterProgressStatus.ENABLING_KERBEROS,
        stepName: stepWizardUtilities?.wizardSteps?.[nextStep]?.name,
      });
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
        flushOnCancel();
        break;
      case "back":
        flushOnStepChange(Number(activeStep) - 1);
        break;
      case "next":
        flushOnStepChange(Number(activeStep) + 1);
        break;
      case "jump":
        flushOnStepChange(jumpStep);
        break;
      default:
        flushCurrentData();
    }
  }

  function onExitPopUp(isCritical: boolean, skipDiscardChanges: boolean) {
    console.log("Exit popup called", isCritical, skipDiscardChanges);
    // TODO: will be added once modalManager PR is merged
    // modalManager.show(
    //   <ConfirmationModal
    //     isOpen={true}
    //     onClose={() => modalManager.hide()}
    //     modalTitle={translate("popup.confirmation.commonHeader")}
    //     modalBody={
    //       isCritical
    //         ? translate("admin.kerberos.wizard.exit.critical.msg")
    //         : translate("admin.kerberos.wizard.exit.warning.msg")
    //     }
    //     successCallback={() => {
    //       if (!skipDiscardChanges) discardChanges(clusterName);
    //       flushStateToDb("cancel");
    //       modalManager.hide();
    //     }}
    //   />
    // );
  }

  return (
    <EnableKerberosContext.Provider
      value={{
        state,
        dispatch,
        stepWizardUtilities,
        flushStateToDb,
        onExitPopUp,
      }}
    >
      {children}
    </EnableKerberosContext.Provider>
  );
};
