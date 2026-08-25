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

import { Alert, Button } from "react-bootstrap";
import Spinner from "../../../../components/Spinner";
import StepWizard from "../../../../components/StepWizard";
import useStepWizard from "../../../../hooks/useStepWizard";
import {
  PersistedWorkflowContext,
  PersistedWorkflowProvider,
} from "../Federation/PersistedWorkflowContext";
import { HawqStandbyContext, HawqStandbyMode } from "./context";
import { createHawqWizardSteps } from "./wizardSteps";
import useHawqStandbyCapabilities from "./useHawqStandbyCapabilities";

export default function HawqStandbyWizard({ mode }: { mode: HawqStandbyMode }) {
  const wizardUtilities = useStepWizard(createHawqWizardSteps(mode), 0);
  const { capabilities, error, hawqInstalled, isLoading, retry } =
    useHawqStandbyCapabilities();
  const allowed =
    capabilities.supported &&
    ((mode === "add" && capabilities.canAdd) ||
      (mode === "remove" && capabilities.canRemove) ||
      (mode === "activate" && capabilities.canActivate));
  const capabilityError =
    error ||
    capabilities.reason ||
    (!allowed
      ? `The installed HAWQ stack does not expose the ${mode} standby capability.`
      : "");

  if (isLoading) return <div className="d-flex justify-content-center p-5"><Spinner /></div>;
  if (!allowed) {
    return (
      <Alert variant="danger">
        {capabilityError || "HAWQ standby management is unavailable."}
        {hawqInstalled ? (
          <Button size="sm" className="ms-3" onClick={retry}>
            Retry
          </Button>
        ) : null}
      </Alert>
    );
  }
  const storageKey = `${mode.toUpperCase()}_HAWQ_STANDBY`;
  return (
    <HawqStandbyContext.Provider value={{ mode, capabilities }}>
      <PersistedWorkflowProvider
        storageKey={storageKey}
        controllerName={`${mode}HawqStandbyWizardController`}
        progressStatus={storageKey}
        progressStepIndex={mode === "add" ? 3 : 2}
        summaryUrl="/#/main/services/HAWQ/summary"
        stepWizardUtilities={wizardUtilities}
      >
        <StepWizard
          wizardUtilities={wizardUtilities}
          Context={PersistedWorkflowContext}
        />
      </PersistedWorkflowProvider>
    </HawqStandbyContext.Provider>
  );
}
