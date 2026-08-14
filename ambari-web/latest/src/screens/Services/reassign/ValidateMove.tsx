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


import { useContext, useEffect, useMemo, useState } from "react";
import useStepWizard from "../../../hooks/useStepWizard";
import Spinner from "../../../components/Spinner";
import { ReassignProvider, ReassignContext } from "./store/context";
import StepWizard from "../../../components/StepWizard";
import Modal from "../../../components/Modal";
import ClusterApi from "../../../api/clusterApi";
import { useParams } from "react-router-dom";
import { reassignSteps } from "./constants";
import { ServiceContext } from "../../../store/ServiceContext";
import { ActionTypes } from "./store/types";
import Step1 from "./Step1";
import Step2 from "./Step2";
import Step3 from "./Step3";
import Step4 from "./Step4";
import Step5 from "./Step5";
import Step6 from "./Step6";
import { AppContext } from "../../../store/context";
import { serviceNameModelMapping } from "../../../constants";
import { getReassignValidationErrors } from "../../../Utils/reassignValidation";
import ConfigsApi from "../../../api/configsApi";
import { Alert, Button } from "react-bootstrap";
import {
  getOozieJdbcDriver,
  hasReassignManualCommands,
} from "../../../Utils/reassignManualCommands";
import { Step } from "../../../types/StepWizard";

// Component to initialize the component name in the store
const ComponentNameInitializer: React.FC<{ componentName: string | undefined }> = ({ componentName }) => {
  const { dispatch } = useContext(ReassignContext);
  
  useEffect(() => {
    if (componentName) {
      dispatch({
        type: ActionTypes.SET_COMPONENT_NAME,
        payload: componentName,
      });
    }
  }, [componentName, dispatch]);
  
  return null;
};

function getSteps(componentName: string | undefined, hasManualCommands: boolean) {
  if (!componentName) {
    return {};
  }

  const inferredSteps: Record<number, Step & { name: reassignSteps }> = {
    1: {
      label: "Get Started",
      completed: false,
      Component: <Step1 />,
      canGoBack: false,
      isNextEnabled: false,
      name: reassignSteps.GET_STARTED,
    },
    2: {
      label: "Assign Masters",
      completed: false,
      Component: <Step2 />,
      canGoBack: true,
      isNextEnabled: false,
      name: reassignSteps.ASSIGN_MASTER,
    },
    3: {
      label: "Review",
      completed: false,
      Component: <Step3 />,
      canGoBack: true,
      isNextEnabled: false,
      name: reassignSteps.REVIEW,
      nextLabel: "DEPLOY",
    },
    4: {
      label: "Configure Component",
      completed: false,
      Component: <Step4 />,
      canGoBack: false,
      isNextEnabled: true,
      name: reassignSteps.CONFIGURE_COMPONENT,
      nextLabel: hasManualCommands ? "Next" : "Complete",
    },
  };

  if (hasManualCommands) {
    inferredSteps[5] = {
      label: "Manual Commands",
      completed: false,
      Component: <Step5 />,
      canGoBack: true,
      isNextEnabled: false,
      name: reassignSteps.MANUAL_COMMANDS,
      nextLabel: "Next",
    };
    inferredSteps[6] = {
      label: "Start and Test Services",
      completed: false,
      Component: <Step6 />,
      canGoBack: true,
      isNextEnabled: false,
      name: reassignSteps.START_AND_TEST_SERVICES,
      nextLabel: "Complete",
    };
  }

  return inferredSteps;
}

function MoveWizard({
  componentName,
  hasManualCommands,
}: {
  componentName: string | undefined;
  hasManualCommands: boolean;
}) {
  const stepWizardUtilities = useStepWizard(
    getSteps(componentName, hasManualCommands),
    1
  );

  return (
    <ReassignProvider
      stepWizardUtilities={stepWizardUtilities}
      hasManualCommands={hasManualCommands}
    >
      <ComponentNameInitializer componentName={componentName} />
      <StepWizard wizardUtilities={stepWizardUtilities} />
    </ReassignProvider>
  );
}

function ValidateMove({
  serviceName: serviceNameProp,
}: {
  serviceName: string;
}) {
  const { componentName } = useParams();
  const { allModelsLoaded, allServiceModels } = useContext(ServiceContext);
  const { allHostNames, clusterName, isAppLoaded, serviceComponentInfo } =
    useContext(AppContext);
  const [oozieEligibility, setOozieEligibility] = useState<{
    componentName?: string;
    hasManualCommands?: boolean;
    error?: string;
  }>({});
  const [oozieLoadAttempt, setOozieLoadAttempt] = useState(0);
  const isOozie = componentName === "OOZIE_SERVER";
  const hasCurrentOozieEligibility =
    oozieEligibility.componentName === componentName;
  const manualCommandsLoading =
    isOozie &&
    (!hasCurrentOozieEligibility ||
      (oozieEligibility.hasManualCommands === undefined &&
        !oozieEligibility.error));
  const manualCommandsError = hasCurrentOozieEligibility
    ? oozieEligibility.error
    : undefined;
  const hasManualCommands = isOozie
    ? oozieEligibility.hasManualCommands === true
    : hasReassignManualCommands(componentName);
  const checkingForMovement =
    !isAppLoaded || !allModelsLoaded || manualCommandsLoading;

  useEffect(() => {
    if (!isOozie || !clusterName) {
      return;
    }

    let cancelled = false;
    setOozieEligibility({ componentName });
    ConfigsApi.getConfigValues(clusterName, "OOZIE")
      .then((response) => {
        if (!cancelled) {
          setOozieEligibility({
            componentName,
            hasManualCommands: hasReassignManualCommands(
              componentName,
              getOozieJdbcDriver(response)
            ),
          });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setOozieEligibility({
            componentName,
            error:
              error instanceof Error
                ? error.message
                : "Could not load the current Oozie database configuration.",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [clusterName, componentName, isOozie, oozieLoadAttempt]);
  const validationErrors = useMemo(
    () =>
      checkingForMovement
        ? []
        : getReassignValidationErrors({
            componentName,
            serviceName: serviceNameProp,
            allHostNames,
            serviceComponentInfo,
            serviceModel:
              allServiceModels[serviceNameModelMapping[serviceNameProp]],
          }),
    [
      allHostNames,
      allServiceModels,
      checkingForMovement,
      componentName,
      serviceComponentInfo,
      serviceNameProp,
    ]
  );
  const canStartMove =
    !checkingForMovement && !manualCommandsError && validationErrors.length === 0;
  const [showModal, setShowModal] = useState(true);
  const getModalBodyContent = () => {
    if (checkingForMovement) {
      return (
        <div className="d-flex justify-content-center align-items-center flex-column p-4">
          <Spinner />
        </div>
      );
    }
    if (manualCommandsError) {
      return (
        <Alert variant="danger">
          <div>{manualCommandsError}</div>
          <Button
            className="mt-3"
            variant="outline-danger"
            onClick={() => setOozieLoadAttempt((attempt) => attempt + 1)}
          >
            Retry
          </Button>
        </Alert>
      );
    }
    if (validationErrors.length) {
      return (
        <>
          <strong className="text-danger">Errors:</strong>
          {validationErrors.map((error) => (
            <div key={error} className="mt-2">
              {error}
            </div>
          ))}
        </>
      );
    }
    return (
      <MoveWizard
        componentName={componentName}
        hasManualCommands={hasManualCommands}
      />
    );
  };
  return (
    <>
      {showModal ? (
        <Modal
          isOpen={showModal}
          onClose={async () => {
            // Clear persisted data on cancel/close
            await ClusterApi.postPersistData(
              JSON.stringify({
                USER_REDIRECTION_URL: "",
                REASSIGN_COMPONENT: JSON.stringify({}),
                CLUSTER_STATE: JSON.stringify({}),
              })
            );
            setShowModal(false);
            window.location.href = `/#/main/services/${serviceNameProp}/summary`;
          }}
          modalTitle={`Move ${componentName}`}
          modalBody={getModalBodyContent()}
          successCallback={() => {
            setShowModal(false);
            window.location.href = `/#/main/services/${serviceNameProp}/summary`;
          }}
          options={{
            shouldShowFooter: canStartMove ? false : true,
            modalSize: "modal-wizard",
            cancelableViaIcon: true,
            cancelableViaBtn: false,
          }}
        />
      ) : null}
    </>
  );
}

export default ValidateMove;
