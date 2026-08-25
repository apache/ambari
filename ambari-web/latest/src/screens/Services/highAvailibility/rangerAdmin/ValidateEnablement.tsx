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
import { AppContext } from "../../../../store/context";
import { ClusterProgressStatus } from "../../../../constants";
import ConfirmationModal from "../../../../components/ConfirmationModal";
import Modal from "../../../../components/Modal";
import Spinner from "../../../../components/Spinner";
import StepWizard from "../../../../components/StepWizard";
import useStepWizard from "../../../../hooks/useStepWizard";
import useAuth from "../../../../hooks/useAuth";
import { parsePersistedValue } from "../../../../Utils/persistedSettings";
import wizardSteps from "./wizardSteps";
import {
  clearRangerAdminHaPersistedState,
  EnableHighAvailibilityProvider,
  EnableHighAvailibilityRangerAdminContext,
} from "./store/context";
import {
  evaluateRangerAdminEnablement,
  rangerAdminEnablementApi,
} from "./rangerAdminHaApi";

const ENABLE_PERMISSION_ERROR =
  "You are not authorized to enable service high availability.";
const PERSIST_PERMISSION_ERROR =
  "You are not authorized to manage persisted cluster workflow data.";
function responseErrorMessage(error: unknown) {
  const requestError = error as {
    message?: string;
    response?: { data?: { message?: string } };
  };
  return (
    requestError.response?.data?.message ||
    requestError.message ||
    "Ambari could not validate Ranger Admin HA prerequisites."
  );
}

function ValidateEnablement() {
  const { clusterName, clusterState, allHostNames } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const stepWizardUtilities = useStepWizard(wizardSteps, 1);
  const [canStartEnablement, setCanStartEnablement] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [checkingForEnablement, setCheckingForEnablement] = useState(true);
  const [showModal, setShowModal] = useState(true);
  const [showExitWarning, setShowExitWarning] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const canEnableHa = hasAuthorization("SERVICE.ENABLE_HA");
  const canPersist = hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const restoredClusterState = parsePersistedValue<Record<string, unknown>>(
    clusterState,
    {},
  );
  const isRecovering =
    restoredClusterState.progressStatus ===
    ClusterProgressStatus.ENABLING_RANGER_ADMIN_HA;
  const hostCount = allHostNames?.length ?? 0;

  useEffect(() => {
    let cancelled = false;

    async function validateCanEnable() {
      setCheckingForEnablement(true);
      setCanStartEnablement(false);
      setValidationErrors([]);
      const permissionErrors = [
        !canEnableHa ? ENABLE_PERMISSION_ERROR : "",
        !canPersist ? PERSIST_PERMISSION_ERROR : "",
      ].filter(Boolean);
      try {
        if (!clusterName) {
          throw new Error("The current cluster name is unavailable.");
        }
        if (isRecovering || permissionErrors.length) {
          if (!cancelled) {
            setValidationErrors(permissionErrors);
            setCanStartEnablement(permissionErrors.length === 0);
          }
          return;
        }

        const component =
          await rangerAdminEnablementApi.loadRangerAdminComponent(clusterName);
        const errors = evaluateRangerAdminEnablement(
          component,
          hostCount,
        ).errors;
        if (!cancelled) {
          setValidationErrors(errors);
          setCanStartEnablement(errors.length === 0);
        }
      } catch (error) {
        if (!cancelled) {
          setValidationErrors([responseErrorMessage(error)]);
        }
      } finally {
        if (!cancelled) setCheckingForEnablement(false);
      }
    }

    void validateCanEnable();
    return () => {
      cancelled = true;
    };
  }, [
    clusterName,
    canEnableHa,
    canPersist,
    hostCount,
    isRecovering,
    retryCount,
  ]);

  async function closeWizard(preserveWorkflow: boolean) {
    if (!preserveWorkflow) {
      await clearRangerAdminHaPersistedState();
    }
    setShowModal(false);
    window.location.href = "/#/main/services/RANGER/summary";
  }

  const requestClose = () => {
    if (stepWizardUtilities.activeStep >= 4 || isRecovering) {
      setShowExitWarning(true);
      return;
    }
    void closeWizard(false);
  };

  const getModalBodyContent = () => {
    if (checkingForEnablement) {
      return (
        <div className="d-flex justify-content-center align-items-center flex-column p-4">
          <Spinner />
          <small className="text-muted mt-2">
            Validating Ranger Admin HA prerequisites...
          </small>
        </div>
      );
    }
    if (validationErrors.length) {
      return (
        <div role="alert">
          <strong className="text-danger">Ranger Admin HA cannot start:</strong>
          {validationErrors.map((error) => (
            <div key={error} className="mt-2">
              {error}
            </div>
          ))}
        </div>
      );
    }
    return (
      <EnableHighAvailibilityProvider stepWizardUtilities={stepWizardUtilities}>
        <StepWizard
          Context={EnableHighAvailibilityRangerAdminContext}
          wizardUtilities={stepWizardUtilities}
        />
      </EnableHighAvailibilityProvider>
    );
  };

  return (
    <>
      {showModal && (
        <Modal
          isOpen={showModal}
          onClose={requestClose}
          modalTitle="Enable Ranger Admin HA Wizard"
          modalBody={getModalBodyContent()}
          successCallback={() => setRetryCount((value) => value + 1)}
          options={{
            shouldShowFooter: !checkingForEnablement && !canStartEnablement,
            modalSize: "modal-wizard",
            okButtonText: "Retry",
          }}
        />
      )}
      <ConfirmationModal
        isOpen={showExitWarning}
        onClose={() => setShowExitWarning(false)}
        modalTitle="Ranger Admin HA is still running"
        modalBody="Ambari will keep the deployment checkpoint and continue any server-side request. You can reopen the wizard to resume progress."
        okButtonText="Exit Wizard"
        buttonVariant="danger"
        successCallback={() => {
          setShowExitWarning(false);
          void closeWizard(true);
        }}
      />
    </>
  );
}

export default ValidateEnablement;
