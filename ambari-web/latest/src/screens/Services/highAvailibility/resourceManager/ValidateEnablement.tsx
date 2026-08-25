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
import { Alert } from "react-bootstrap";
import Modal from "../../../../components/Modal";
import ConfirmationModal from "../../../../components/ConfirmationModal";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import {
  EnableHighAvailibilityContext,
  EnableHighAvailibilityProvider,
} from "./store/context";
import StepWizard from "../../../../components/StepWizard";
import { AppContext } from "../../../../store/context";
import useAuth from "../../../../hooks/useAuth";
import { ClusterProgressStatus } from "../../../../constants";
import ClusterApi from "../../../../api/clusterApi";
import {
  parsePersistedValue,
  persistedPayload,
} from "../../../../Utils/persistedSettings";
import rmHaApi from "./rmHaApi";
import {
  flattenClusterTopology,
  getRmHaEnablementErrors,
  parseRmHaHosts,
  responseErrorMessage,
  RM_HA_ENABLEMENT_MESSAGES,
} from "./rmHaUtils";
import { initialState } from "./store/reducer";

function ValidateEnablement() {
  const { services, clusterName, clusterState } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const stepWizardUtilities = useStepWizard(wizardSteps, 1);
  const [canStartEnablement, setCanStartEnablement] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [checkingForEnablement, setCheckingForEnablement] = useState(true);
  const [showModal, setShowModal] = useState(true);
  const [showExitWarning, setShowExitWarning] = useState(false);
  const [closeError, setCloseError] = useState("");
  const [retryCount, setRetryCount] = useState(0);

  const canEnableHa = hasAuthorization("SERVICE.ENABLE_HA");
  const canPersist = hasAuthorization(
    "CLUSTER.MANAGE_USER_PERSISTED_DATA",
  );
  const restoredClusterState = parsePersistedValue<Record<string, unknown>>(
    clusterState,
    {},
  );
  const isRecovering =
    restoredClusterState.progressStatus ===
    ClusterProgressStatus.ENABLING_RM_HA;

  useEffect(() => {
    if (!clusterName) return;
    let cancelled = false;

    async function validateCanEnable() {
      setCheckingForEnablement(true);
      setCanStartEnablement(false);
      setValidationErrors([]);
      try {
        if (isRecovering) {
          const permissionErrors = [
            !canEnableHa ? RM_HA_ENABLEMENT_MESSAGES.enablePermission : "",
            !canPersist ? RM_HA_ENABLEMENT_MESSAGES.persistPermission : "",
          ].filter(Boolean);
          if (!cancelled) {
            setValidationErrors(permissionErrors);
            setCanStartEnablement(permissionErrors.length === 0);
          }
          return;
        }

        const [hostsData, componentsData] = await Promise.all([
          rmHaApi.getHosts(clusterName),
          rmHaApi.getClusterComponents(clusterName),
        ]);
        const hosts = parseRmHaHosts(hostsData);
        const topology = flattenClusterTopology(componentsData);
        const installedServices = services
          .map((service) => service?.ServiceInfo?.service_name)
          .filter(Boolean);
        const errors = getRmHaEnablementErrors({
          topology,
          hostNames: hosts.map(({ hostName }) => hostName),
          yarnInstalled: installedServices.includes("YARN"),
          alreadyEnabled:
            topology.filter(
              ({ component }) => component === "RESOURCEMANAGER",
            ).length > 1,
          canEnableHa,
          canPersist,
        });
        if (!cancelled) {
          setValidationErrors(errors);
          setCanStartEnablement(errors.length === 0);
        }
      } catch (error) {
        if (!cancelled) {
          setValidationErrors([
            responseErrorMessage(
              error,
              "Ambari could not validate ResourceManager HA prerequisites.",
            ),
          ]);
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
    isRecovering,
    services,
    retryCount,
  ]);

  async function closeWizard(preserveWorkflow: boolean) {
    setCloseError("");
    try {
      if (!preserveWorkflow) {
        await ClusterApi.postPersistData(
          persistedPayload({
            HIGH_AVAILIBILITY_RM_HA: initialState,
            CLUSTER_STATE: {},
            "wizard-data": {},
          }),
        );
      }
      setShowModal(false);
      window.location.href = "/#/main/services/YARN/summary";
    } catch (error) {
      setCloseError(
        responseErrorMessage(
          error,
          "Ambari could not clear the ResourceManager HA workflow.",
        ),
      );
    }
  }

  const requestClose = () => {
    if (stepWizardUtilities.activeStep >= 4) {
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
            Validating ResourceManager HA prerequisites...
          </small>
        </div>
      );
    }
    if (validationErrors.length) {
      return (
        <div role="alert">
          <strong className="text-danger">ResourceManager HA cannot start:</strong>
          {validationErrors.map((error) => (
            <div key={error} className="mt-2">
              {error}
            </div>
          ))}
        </div>
      );
    }
    return (
      <EnableHighAvailibilityProvider
        stepWizardUtilities={stepWizardUtilities}
      >
        <StepWizard
          Context={EnableHighAvailibilityContext}
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
          modalTitle="Enable ResourceManager HA Wizard"
          modalBody={
            <>
              {closeError && (
                <Alert variant="danger" className="mb-3" role="alert">
                  {closeError}
                </Alert>
              )}
              {getModalBodyContent()}
            </>
          }
          successCallback={() => setRetryCount((value) => value + 1)}
          options={{
            shouldShowFooter:
              !checkingForEnablement && !canStartEnablement,
            modalSize: "modal-wizard",
            okButtonText: "Retry",
          }}
        />
      )}
      <ConfirmationModal
        isOpen={showExitWarning}
        onClose={() => setShowExitWarning(false)}
        modalTitle="ResourceManager HA is still running"
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
