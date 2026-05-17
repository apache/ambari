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

const useStepWizard = (steps: any, initialActiveStep = 0, onCancel?: any) => {
  const [activeStep, setActiveStep] = useState(initialActiveStep || 0);
  const navigate = useNavigate();
  const [wizardSteps, setWizardSteps] = useState<{ [key: number]: Step }>(
    steps
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
          if (activeStep !== Object.keys(steps).length - 1) {
            setActiveStep(activeStep + 1);
          }
        }
      } catch (error) {
        console.log("Cannot move to next step:", error);
      }
    }
  };
  const handleNextImperitive = async () => {
    const wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].completed = true;
    setActiveStep(Number(activeStep) + 1);
    setWizardSteps(wizardStepsCopy);
  };
  const handleBackImperitive = async () => {
    const wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].completed = false;
    wizardStepsCopy[activeStep - 1].completed = false;
    setActiveStep(Number(activeStep) - 1);
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
    let wizardStepsCopy = { ...wizardSteps };
    wizardStepsCopy[activeStep].completed = false;
    wizardStepsCopy[activeStep - 1].completed = false;
    setWizardSteps(wizardStepsCopy);
    if (activeStep !== 0) setActiveStep(activeStep - 1);
  };

  const canJumpFromCurrentStep = (currentStep: number) => {
    return !(
      currentStep > activeStep ||
      (currentStep !== activeStep && !wizardSteps[activeStep]?.canGoBack)
    );
  };

  useEffect(() => {
    // navigate(`/installer/step${activeStep}`);
  }, [activeStep]);

  const jumpToStep = (stepNumber: number, isImperitiveJump = false) => {
    if (canJumpFromCurrentStep(stepNumber) || isImperitiveJump) {
      const wizardStepsCopy = cloneDeep(wizardSteps);
      for (const step in wizardStepsCopy) {
        if (Number(step) >= stepNumber) {
          wizardStepsCopy[step].completed = false;
        } else {
          wizardStepsCopy[step].completed = true;
        }
      }
      setWizardSteps(wizardStepsCopy);
      setActiveStep(stepNumber);
    }
  };

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
    currentStep: wizardSteps[activeStep],
    prevStepNumber: activeStep - 1,
    onCancel
  };
};

export default useStepWizard;
