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
import { filter, find, flatten, get, map, some } from "lodash";
import { t } from "i18next";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import { EnableHighAvailibilityProvider } from "./store/context";
import StepWizard from "../../../../components/StepWizard";
import ClusterApi from "../../../../api/clusterApi";
import { LocalStorageOps } from "../../../../Utils/LocalStorageOps";
import { AppContext } from "../../../../store/context";
import { ClusterProgressStatus } from "../../../../constants";
import { ServiceContext } from "../../../../store/ServiceContext";

function ValidateEnablement() {
  const { allServiceModels } = useContext(ServiceContext);
  const { services, clusterState, allHostNames } = useContext(AppContext);
  const { hostComponents: serviceHostComponents, serviceComponents } =
    useHostComponents(map(services, "ServiceInfo.service_name"));
  const stepWizardUtilities = useStepWizard(wizardSteps, 0);
  const [canStartEnablement, setCanStartEnablement] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [checkingForEnablement, setCheckingForEnablement] = useState(true);
  const [showModal, setShowModal] = useState(true);
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
        <StepWizard wizardUtilities={stepWizardUtilities} />
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
      get(find(hostComponents, ["component_name", "NAMENODE"]), "state") !==
      "STARTED"
    ) {
      messages.push(t("admin.highAvailability.error.namenodeStarted"));
    }
    if (
      filter(hostComponents, ["component_name", "ZOOKEEPER_SERVER"]).length < 3
    ) {
      messages.push(t("admin.highAvailability.error.zooKeeperNum"));
    }
    if (allHostNames.length < 3) {
      messages.push("NameNode HA requires at least three registered hosts.");
    }
    if (allServiceModels?.hdfs?.isNameNodeHaEnabled) {
      messages.push("NameNode HA is already enabled for HDFS.");
    }
    if (
      some(filter(hostComponents, ["isMaster", true]), [
        "maintenance_state",
        "ON",
      ]) ||
      some(filter(hostComponents, ["isMaster", true]), ["isImpliedState", true])
    ) {
      messages.push(t("admin.highAvailability.error.maintenanceMode"));
    }
    if (!messages.length) {
      prepareToProceed();
    } else {
      setValidationErrors(messages);
      setCheckingForEnablement(false);
    }
  };

  const prepareToProceed = () => {
    setCanStartEnablement(true);
    setValidationErrors([]);
    setCheckingForEnablement(false);
  };

  useEffect(() => {
    if (serviceHostComponents.length && serviceComponents.length) {
      if (
        get(clusterState, "progressStatus", "") ===
        ClusterProgressStatus.ENABLING_NAMENODE_HA
      ) {
        prepareToProceed();
      } else {
        validateCanEnable();
      }
    }
  }, [serviceHostComponents.length, serviceComponents.length]);

  return (
    <>
      {showModal ? (
        <Modal
          isOpen={showModal}
          onClose={async () => {
            await ClusterApi.postPersistData(
              JSON.stringify({
                USER_REDIRECTION_URL: "",
              })
            );
            setShowModal(false);
            LocalStorageOps.setItem(
              "lastVisitedURL",
              "/#/main/services/HDFS/summary"
            );
            window.location.href = "/#/main/services/HDFS/summary";
          }}
          modalTitle="Enable Namenode HA Wizard"
          modalBody={getModalBodyContent()}
          successCallback={() => {
            setShowModal(false);
            window.location.href = "/#/main/services/HDFS/summary";
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
