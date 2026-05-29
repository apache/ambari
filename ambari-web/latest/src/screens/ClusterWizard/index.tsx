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

import { createContext } from "react";
import StepWizard from "../../components/StepWizard";
import useStepWizard from "../../hooks/useStepWizard";
import { redirectToAdminView } from "../../Utils/adminViewRedirect";

type PropTypes = {
  Context: any;
  Provider: any;
  wizardSteps: Partial<{
    label: string;
    completed: boolean;
    Component: any;
    canGoBack: boolean;
    isNextEnabled: boolean;
    name: string;
  }>[];
  initialActiveStep?: number;
};

export const ContextWrapper = createContext<{ Context: any }>({ Context: {} });

const ClusterCreationWizard = ({
  Context,
  Provider,
  wizardSteps,
  initialActiveStep = 0,
}: PropTypes) => {
  const stepWizardUtilities = useStepWizard(
    wizardSteps,
    initialActiveStep,
    redirectToAdminView
  );

  return (
    <ContextWrapper.Provider value={{ Context }}>
      <Provider stepWizardUtilities={stepWizardUtilities}>
        <StepWizard wizardUtilities={stepWizardUtilities} Context={Context} />
      </Provider>
    </ContextWrapper.Provider>
  );
};

export default ClusterCreationWizard;
