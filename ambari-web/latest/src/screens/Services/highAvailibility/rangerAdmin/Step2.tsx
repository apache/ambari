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

import { useCallback, useContext, useState } from "react";
import { Alert, Card } from "react-bootstrap";
import { map } from "lodash";
import { AppContext } from "../../../../store/context";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import AssignMastersAddable, {
  AssignMastersLoadState,
} from "../../../../components/AssignMastersAddable";
import { ActionTypes } from "./store/types";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import {
  RangerAdminAssignment,
  validateRangerAdminAssignments,
} from "./rangerAdminHaUtils";

interface AssignmentPayload extends Record<string, unknown> {
  masterComponentHosts?: RangerAdminAssignment[];
}

function Step2() {
  const { services } = useContext(AppContext);
  const {
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      handleNextImperitive,
      handleBackImperitive,
      currentStep,
    },
  } = useContext(EnableHighAvailibilityRangerAdminContext);
  const [assignmentPayload, setAssignmentPayload] =
    useState<AssignmentPayload | null>(null);
  const [sharedAssignmentsValid, setSharedAssignmentsValid] = useState(false);
  const [sharedErrors, setSharedErrors] = useState<string[]>([]);
  const [assignmentLoadState, setAssignmentLoadState] =
    useState<AssignMastersLoadState>({ status: "loading" });
  const rangerErrors = assignmentPayload
    ? validateRangerAdminAssignments(
        assignmentPayload.masterComponentHosts || [],
      )
    : ["Ranger Admin host assignments are still loading."];
  const isNextEnabled =
    assignmentLoadState.status === "ready" &&
    sharedAssignmentsValid &&
    rangerErrors.length === 0;

  const onAssignmentValidationChange = useCallback(
    (valid: boolean, errors: string[]) => {
      setSharedAssignmentsValid(valid);
      setSharedErrors(errors);
    },
    [],
  );

  return (
    <>
      <div className="step-title">Select Hosts</div>
      <div className="step-description">
        Select one or more hosts for the additional Ranger Admin components.
      </div>
      <Card className="mt-2">
        <Card.Body>
          <Alert className="my-1 fs-12" variant="warning">
            Keep the load balancer separate from all Ranger Admin components.
          </Alert>
          <AssignMastersAddable
            mastersToShow={["RANGER_ADMIN"]}
            mastersToAdd={["RANGER_ADMIN"]}
            mastersToCreate={[]}
            showCurrentPrefix={["RANGER_ADMIN"]}
            showAdditionalPrefix={["RANGER_ADMIN"]}
            mastersAddableInHA={["RANGER_ADMIN"]}
            minimumAdditionalMasterCount={{ RANGER_ADMIN: 1 }}
            services={map(services, "ServiceInfo.service_name")}
            showInstalledMastersFirst
            validateAssignments
            onAssignmentValidationChange={onAssignmentValidationChange}
            onLoadStateChange={setAssignmentLoadState}
            dispatch={(payload: AssignmentPayload) => {
              setAssignmentPayload(payload);
              dispatch({
                type: ActionTypes.STORE_INFORMATION,
                payload: {
                  step: currentStep.name,
                  data: payload,
                },
              });
            }}
          />
          {rangerErrors
            .filter((error) => !sharedErrors.includes(error))
            .map((error) => (
              <Alert variant="danger" className="mt-3 mb-0" key={error}>
                {error}
              </Alert>
            ))}
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onBack={async () => {
          await flushStateToDb("back");
          await handleBackImperitive();
        }}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => void flushStateToDb("cancel")}
      />
    </>
  );
}

export default Step2;
