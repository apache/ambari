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
import useHostComponents from "../../../ClusterWizard/hooks/useHostComponents";
import Modal from "../../../../components/Modal";
import { filter, find, flatten, get, map } from "lodash";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import { EnableHighAvailibilityContext, EnableHighAvailibilityProvider } from "./store/context";
import StepWizard from "../../../../components/StepWizard";
import { messages as Messages } from "../../../messages";
import { AppContext } from "../../../../store/context";

function ValidateEnablement() {
  const {services}=useContext(AppContext);
  const { hostComponents: serviceHostComponents, serviceComponents } =
    useHostComponents(map(services, "ServiceInfo.service_name"));
  const stepWizardUtilities = useStepWizard(wizardSteps, 1);
  const [canStartEnablement, setCanStartEnablement] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [checkingForEnablement, setCheckingForEnablement] = useState(true);
  const [showModal, setShowModal] = useState(true);

  const { allHostNames } = useContext(AppContext);

  const getModalBodyContent = () => {
    if (checkingForEnablement) {
      return (
        <div className="d-flex justify-content-center align-items-center flex-column p-4">
          <Spinner />
          <small className="text-muted mt-2">
            Validating the components for high Availibility enablement...
          </small>
        </div>
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
      <EnableHighAvailibilityProvider stepWizardUtilities={stepWizardUtilities}>
        <StepWizard Context={EnableHighAvailibilityContext} wizardUtilities={stepWizardUtilities} />
      </EnableHighAvailibilityProvider>
    );
  };

  const validateCanEnable = () => {
    const messages = [];
    const hostComponentsRoles = flatten(
      map(serviceHostComponents, "host_components")
    );
    const hostComponents = flatten(map(hostComponentsRoles, "HostRoles"));
    const allComponents = flatten(
      map(
        flatten(map(serviceComponents, "components")),
        "StackServiceComponents"
      )
    );
    const masterComponents = map(
      filter(allComponents, ["is_master", true]),
      "component_name"
    );
    const impliedStates = [
      "IMPLIED_FROM_SERVICE_AND_HOST",
      "IMPLIED_FROM_HOST",
      "IMPLIED_FROM_SERVICE",
    ];
    for (let hostComponent of hostComponents as any) {
      //@ts-ignore
      hostComponent.isImpliedState = impliedStates.includes(
        hostComponent.maintenance_state
      );
      if (masterComponents.includes(hostComponent.component_name as never)) {
        hostComponent.isMaster = true;
      } else {
        hostComponent.isMaster = false;
      }
    }
    if (
      get(
        find(hostComponents, ["component_name", "RESOURCEMANAGER"]),
        "state"
      ) !== "STARTED"
    ) {
      messages.push(
        Messages["admin.rm_highAvailability.error.resourceManagerStarted"]
      );
    }
    if (
      filter(hostComponents, ["component_name", "ZOOKEEPER_SERVER"]).length < 3
    ) {
      messages.push(Messages["admin.highAvailability.error.zooKeeperNum"]);
    }
    if (allHostNames.length < 3) {
      messages.push(Messages["admin.rm_highAvailability.error.hostsNum"]);
    }
    if (!messages.length) {
      setCanStartEnablement(true);
      setValidationErrors([]);
      setCheckingForEnablement(false);
    } else {
      setValidationErrors(messages);
      setCheckingForEnablement(false);
    }
  };

  useEffect(() => {
    if (serviceHostComponents.length && serviceComponents.length) {
      validateCanEnable();
    }
  }, [serviceHostComponents.length, serviceComponents.length]);
  return (
    <>
      {showModal ? (
        <Modal
          isOpen={showModal}
          onClose={() => {
            setShowModal(false);
          }}
          modalTitle="Enable ResourceManager HA Wizard"
          modalBody={getModalBodyContent()}
          successCallback={() => {
            setShowModal(false);
          }}
          options={{
            shouldShowFooter:
              checkingForEnablement || canStartEnablement ? false : true,
            modalSize: "modal-wizard",
          }}
        />
      ) : null}
    </>
  );
}

export default ValidateEnablement;
