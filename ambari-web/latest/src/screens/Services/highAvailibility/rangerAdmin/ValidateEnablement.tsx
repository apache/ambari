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

import Modal from "../../../../components/Modal";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import { EnableHighAvailibilityProvider, EnableHighAvailibilityRangerAdminContext } from "./store/context";
import StepWizard from "../../../../components/StepWizard";
import { useState } from "react";

function ValidateEnablement() {
  const stepWizardUtilities = useStepWizard(wizardSteps, 1);
  const [showModal, setShowModal] = useState(true);
  const getModalBodyContent = () => {
    return (
      <EnableHighAvailibilityProvider stepWizardUtilities={stepWizardUtilities}>
        <StepWizard Context={EnableHighAvailibilityRangerAdminContext} wizardUtilities={stepWizardUtilities} />
      </EnableHighAvailibilityProvider>
    );
  };

  return (
    <>
      {showModal ? (
        <Modal
          isOpen={showModal}
          onClose={() => {
            setShowModal(false);
          }}
          modalTitle="Enable Ranger Admin HA Wizard"
          modalBody={getModalBodyContent()}
          successCallback={() => {}}
          options={{
            shouldShowFooter: false,
            modalSize:"modal-wizard"
          }}
        />
      ) : null}
    </>
  );
}

export default ValidateEnablement;
