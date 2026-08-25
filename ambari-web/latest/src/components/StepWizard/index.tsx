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

import {
  createContext,
  FunctionComponent,
  memo,
  useContext,
  useState,
} from "react";
import "./styles.scss";
import classNames from "classnames";
import { CheckLg } from "react-bootstrap-icons";
import { Alert } from "react-bootstrap";
import ConfirmationModal from "../ConfirmationModal";
import { getVisibleStepNumbers } from "../../hooks/useStepWizard";
import { Step } from "../../types/StepWizard";

type StepWizardUtilities = {
  activeStep: number;
  wizardSteps: { [key: number]: Step };
  jumpToStep: (stepNumber: number) => void;
  canJumpFromCurrentStep: (stepNumber: number) => boolean;
};

type WizardContextValue = {
  flushStateToDb?: (
    operation: "jump",
    jumpStep: number,
  ) => void | Promise<void>;
};

const EmptyWizardContext = createContext<unknown>({});

interface StepWizardProps {
  wizardUtilities: StepWizardUtilities;
  Context?: unknown;
}

const StepWizard: FunctionComponent<StepWizardProps> = ({
  wizardUtilities,
  Context,
}) => {
  const { activeStep, wizardSteps, jumpToStep, canJumpFromCurrentStep } =
    wizardUtilities;
  const contextValue = useContext(
    (Context || EmptyWizardContext) as React.Context<unknown>,
  ) as WizardContextValue;
  const flushStateToDb = contextValue?.flushStateToDb;
  const [jumpStep, setjumpStep] = useState(0);
  const [showNavigationModal, setShowNavigationModal] = useState(false);
  const [navigationError, setNavigationError] = useState("");
  const [isNavigating, setIsNavigating] = useState(false);
  const visibleStepNumbers = getVisibleStepNumbers(wizardSteps);
  const jumpStepDisplayNumber = visibleStepNumbers.indexOf(jumpStep) + 1;

  const persistAndJump = async () => {
    if (isNavigating) return;
    setIsNavigating(true);
    setNavigationError("");
    try {
      if (typeof flushStateToDb === "function") {
        await Promise.resolve(flushStateToDb("jump", jumpStep));
      }
      jumpToStep(jumpStep);
      setShowNavigationModal(false);
    } catch (error) {
      const requestError = error as {
        message?: string;
        response?: { data?: { message?: string } };
      };
      setNavigationError(
        requestError.response?.data?.message ||
          requestError.message ||
          "Ambari could not save the wizard checkpoint.",
      );
    } finally {
      setIsNavigating(false);
    }
  };

  return (
    <div className="step-wizard h-95" style={{ position: "relative" }}>
      <ConfirmationModal
        isOpen={showNavigationModal}
        onClose={() => {
          setShowNavigationModal(false);
          setNavigationError("");
        }}
        modalTitle="Navigation Warning"
        modalBody={
          <>
            <div>{`If you proceed to go back to Step ${jumpStepDisplayNumber}, you will lose any changes you made.`}</div>
            {navigationError && (
              <Alert variant="danger" className="mt-3" role="alert">
                {navigationError}
              </Alert>
            )}
          </>
        }
        successCallback={() => void persistAndJump()}
        isOkDisabled={isNavigating}
        cancellable={!isNavigating}
      />
      <div className="d-flex h-100">
        <div className="wizard-nav p-2">
          {visibleStepNumbers.map((currentStep, visibleIndex) => {
            const step = wizardSteps[currentStep];
            return (
              <div
                key={currentStep}
                onClick={() => {
                  if (
                    wizardSteps[activeStep].canGoBack &&
                    canJumpFromCurrentStep(currentStep)
                  ) {
                    setNavigationError("");
                    setShowNavigationModal(true);
                    setjumpStep(Number(currentStep));
                  }
                }}
                className={classNames(
                  "d-flex align-items-center step-wizard-step cursor-pointer",
                  {
                    "step-active": activeStep === Number(currentStep),
                    "cursor-not-allowed": !canJumpFromCurrentStep(currentStep),
                  }
                )}
              >
                <div
                  className={classNames("step-wizard-step-count", {
                    "step-wizard-step-count-completed": step.completed,
                    "step-wizard-step-count-active":
                      activeStep === Number(currentStep),
                  })}
                >
                  <div>
                    {step.completed ? (
                      <CheckLg className="text-white fw-5" />
                    ) : (
                      visibleIndex + 1
                    )}
                  </div>
                </div>
                <div
                  className="ms-2 step-label"
                  style={{ whiteSpace: "nowrap" }}
                >
                  {step.label}
                </div>
              </div>
            );
          })}
        </div>
        <div className="px-5 py-4 w-100 mh-100 y-scroll wizard-content">
          {wizardSteps[activeStep]?.Component}
        </div>
      </div>
      {/* <div className="step-wizard-footer d-flex justify-content-between bg-white p-2">
        <Button
          size="sm"
          variant="outline-secondary"
          className="d-flex align-items-center ms-3"
          onClick={handleBack}
          disabled={!wizardSteps[activeStep].canGoBack}
        >
          <ArrowLeft />
          <span className="ms-1">BACK</span>
        </Button>
        <Button
          variant="success"
          className="me-3"
          onClick={handleNext}
          disabled={!wizardSteps[activeStep].isNextEnabled}
        >
          <span className="me-1">{wizardSteps[activeStep].nextLabel||"NEXT"}</span>
          <ArrowRight />
        </Button>
      </div> */}
    </div>
  );
};

export default memo(StepWizard);
