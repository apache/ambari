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

import { useState, useEffect } from "react";
import { cloneDeep } from "lodash";
import { Step } from "../types/StepWizard";
import { useLocation, useNavigate, useParams } from "react-router-dom";

export const getVisibleStepNumbers = (steps: { [key: number]: Step }) =>
  Object.keys(steps)
    .map(Number)
    .sort((a, b) => a - b)
    .filter((stepNumber) => !steps[stepNumber]?.hidden);

export const getAdjacentVisibleStep = (
  steps: { [key: number]: Step },
  activeStep: number,
  direction: 1 | -1,
) => {
  const visibleSteps = getVisibleStepNumbers(steps);
  const currentIndex = visibleSteps.indexOf(activeStep);
  if (currentIndex === -1) {
    if (direction === 1) {
      return visibleSteps.find((stepNumber) => stepNumber > activeStep);
    }
    return [...visibleSteps]
      .reverse()
      .find((stepNumber) => stepNumber < activeStep);
  }
  return visibleSteps[currentIndex + direction];
};

const useStepWizard = (steps: any, initialActiveStep = 0, onCancel?: any) => {
  const [activeStep, setActiveStep] = useState(initialActiveStep || 0);
  const navigate = useNavigate();
  const [wizardSteps, setWizardSteps] = useState<{ [key: number]: Step }>(
    () =>
      Object.fromEntries(
        Object.entries(steps).map(([stepNumber, step]) => [
          stepNumber,
          { ...(step as Step) },
        ]),
      ),
  );
  const { stepNumber } = useParams();
  const location = useLocation();
  useEffect(() => {
    if (stepNumber) {
      navigate(location.pathname.replace(/step\d+/g, `step${activeStep}`));
    }
  }, [activeStep]);

  const initialiseNextCallback = async (next: any) => {
    const wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].onNext = next;
    setWizardSteps(wizardStepsCopy);
  };

  const handleNext = async () => {
    const wizardStepsCopy = { ...wizardSteps };

    if (wizardSteps[activeStep].onNext) {
      try {
        const canProceed = await wizardSteps[activeStep].onNext();
        if (canProceed) {
          wizardStepsCopy[activeStep].completed = true;
          setWizardSteps(wizardStepsCopy);
          const nextStep = getAdjacentVisibleStep(
            wizardStepsCopy,
            activeStep,
            1,
          );
          if (nextStep !== undefined) {
            setActiveStep(nextStep);
          }
        }
      } catch (error) {
        console.log("Cannot move to next step:", error);
      }
    }
  };
  const handleNextImperitive = async (targetStep?: number) => {
    const wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].completed = true;
    const nextStep =
      targetStep ?? getAdjacentVisibleStep(wizardStepsCopy, activeStep, 1);
    if (nextStep !== undefined) {
      setActiveStep(nextStep);
    }
    setWizardSteps(wizardStepsCopy);
  };
  const handleBackImperitive = async () => {
    const wizardStepsCopy = { ...wizardSteps };
    const previousStep = getAdjacentVisibleStep(
      wizardStepsCopy,
      activeStep,
      -1,
    );
    if (previousStep === undefined) {
      return;
    }
    wizardStepsCopy[activeStep].completed = false;
    wizardStepsCopy[previousStep].completed = false;
    setActiveStep(previousStep);
    setWizardSteps(wizardStepsCopy);
  };

  const enableNext = async (nextCallback: any) => {
    const wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].isNextEnabled = true;
    wizardStepsCopy[activeStep].onNext = nextCallback;
    setWizardSteps(wizardStepsCopy);
  };
  const disableNext = () => {
    const wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].isNextEnabled = false;
    setWizardSteps(wizardStepsCopy);
  };

  const handleBack = () => {
    const wizardStepsCopy = { ...wizardSteps };
    const previousStep = getAdjacentVisibleStep(
      wizardStepsCopy,
      activeStep,
      -1,
    );
    if (previousStep === undefined) {
      return;
    }
    wizardStepsCopy[activeStep].completed = false;
    wizardStepsCopy[previousStep].completed = false;
    setWizardSteps(wizardStepsCopy);
    setActiveStep(previousStep);
  };

  const canJumpFromCurrentStep = (currentStep: number) => {
    const targetStep = Number(currentStep);
    return !(
      wizardSteps[targetStep]?.hidden ||
      targetStep > activeStep ||
      (targetStep !== activeStep && !wizardSteps[activeStep]?.canGoBack)
    );
  };

  useEffect(() => {
    // navigate(`/installer/step${activeStep}`);
  }, [activeStep]);

  const jumpToStep = (stepNumber: number, isImperitiveJump = false) => {
    let targetStep = stepNumber;
    if (wizardSteps[targetStep]?.hidden) {
      targetStep =
        getAdjacentVisibleStep(wizardSteps, targetStep, 1) ??
        getAdjacentVisibleStep(wizardSteps, targetStep, -1) ??
        targetStep;
    }
    if (canJumpFromCurrentStep(targetStep) || isImperitiveJump) {
      const wizardStepsCopy = cloneDeep(wizardSteps);
      for (const step in wizardStepsCopy) {
        if (Number(step) >= targetStep) {
          wizardStepsCopy[step].completed = false;
        } else {
          wizardStepsCopy[step].completed = true;
        }
      }
      setWizardSteps(wizardStepsCopy);
      setActiveStep(targetStep);
    }
  };

  const setStepsHidden = (stepNumbers: number[], hidden: boolean) => {
    setWizardSteps((currentSteps) => {
      const nextSteps = { ...currentSteps };
      stepNumbers.forEach((stepNumber) => {
        if (nextSteps[stepNumber]) {
          nextSteps[stepNumber] = { ...nextSteps[stepNumber], hidden };
        }
      });
      return nextSteps;
    });
  };

  const previousStepNumber = getAdjacentVisibleStep(
    wizardSteps,
    activeStep,
    -1,
  );
  const nextStepNumber = getAdjacentVisibleStep(wizardSteps, activeStep, 1);

  return {
    activeStep,
    wizardSteps,
    handleNext,
    handleBack,
    jumpToStep,
    canJumpFromCurrentStep,
    enableNext,
    disableNext,
    initialiseNextCallback,
    handleNextImperitive,
    handleBackImperitive,
    setStepsHidden,
    currentStep: wizardSteps[activeStep],
    prevStepNumber: previousStepNumber,
    nextStepNumber,
    onCancel,
  };
};

export default useStepWizard;
