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
import { find, flatten, get, map } from "lodash";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import { AddObserverNamenodeProvider } from "./store/context";
import StepWizard from "../../../../components/StepWizard";
import ClusterApi from "../../../../api/clusterApi";
import { LocalStorageOps } from "../../../../Utils/LocalStorageOps";
import { AppContext } from "../../../../store/context";
import { messages } from "../../../messages";
import { ServiceContext } from "../../../../store/ServiceContext";

function ValidateEnablement() {
  const { services } = useContext(AppContext);
  const { masterSlaveClientsData } = useContext(ServiceContext);
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
            Validating the components for Observer Namenode enablement...
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
      <AddObserverNamenodeProvider stepWizardUtilities={stepWizardUtilities}>
        <StepWizard wizardUtilities={stepWizardUtilities} />
      </AddObserverNamenodeProvider>
    );
  };

  const validateCanEnable = () => {
    const errorMessages = [];
    const hostComponentsRoles = flatten(
      map(serviceHostComponents, "host_components")
    );
    const hostComponents = flatten(map(hostComponentsRoles, "HostRoles"));

    if (
      get(
        find(hostComponents, ["component_name", "ZOOKEEPER_SERVER"]),
        "state"
      ) !== "STARTED"
    ) {
      errorMessages.push(
        get(messages, "admin.observerNameNode.wizard.required.zookeepers")
      );
    }

    const journalNodes: any[] = Object.values(masterSlaveClientsData).filter(
      (item: any) => {
        return item.ServiceComponentInfo.component_name === "JOURNALNODE";
      }
    );

    if (
      journalNodes.length > 0 &&
      journalNodes[0]?.ServiceComponentInfo.total_count !==
        journalNodes[0].ServiceComponentInfo.started_count
    ) {
      errorMessages.push(
        get(messages, "admin.observerNameNode.wizard.required.journalnodes")
      );
    }

    if (!errorMessages.length) {
      setCanStartEnablement(true);
      setValidationErrors([]);
      setCheckingForEnablement(false);
    } else {
      setValidationErrors(errorMessages);
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
          modalTitle={get(messages, "admin.observerNameNode.button.enable")}
          modalBody={getModalBodyContent()}
          successCallback={() => {
            setShowModal(false);
            window.location.href = "/#/main/services/HDFS/summary";
          }}
          options={{
            shouldShowFooter:
              checkingForEnablement || canStartEnablement ? false : true,
            modalSize: "modal-wizard",
            cancelableViaIcon: true,
          }}
        />
      ) : null}
    </>
  );
}

export default ValidateEnablement;
