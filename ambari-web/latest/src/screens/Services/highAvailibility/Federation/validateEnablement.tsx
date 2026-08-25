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
import { filter, flatten, get, map } from "lodash";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import {
  EnableNamenodeFederationContext,
  EnableNamenodeFederationProvider,
} from "./store/context";
import StepWizard from "../../../../components/StepWizard";
import ClusterApi from "../../../../api/clusterApi";
import { LocalStorageOps } from "../../../../Utils/LocalStorageOps";
import { AppContext } from "../../../../store/context";
import { messages } from "../../../messages";
import { ServiceContext } from "../../../../store/ServiceContext";
import { Alert, Button } from "react-bootstrap";

function ValidateEnablement() {
  const { services, allHostNames } = useContext(AppContext);
  const { masterSlaveClientsData, allModelsLoaded, allServiceModels } =
    useContext(ServiceContext);
  const {
    hostComponents: serviceHostComponents,
    serviceComponents,
    isLoading: isHostDataLoading,
    error: hostDataError,
    retry: retryHostData,
  } = useHostComponents(map(services, "ServiceInfo.service_name"));
  const stepWizardUtilities = useStepWizard(wizardSteps, 0);
  const [canStartEnablement, setCanStartEnablement] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [checkingForEnablement, setCheckingForEnablement] = useState(true);
  const [showModal, setShowModal] = useState(true);

  const getModalBodyContent = () => {
    if (hostDataError) {
      return (
        <Alert variant="danger">
          {hostDataError}
          <Button
            size="sm"
            className="ms-3"
            onClick={() => {
              setCanStartEnablement(false);
              setValidationErrors([]);
              setCheckingForEnablement(true);
              retryHostData();
            }}
          >
            Retry
          </Button>
        </Alert>
      );
    }
    if (isHostDataLoading || checkingForEnablement) {
      return (
        <div className="d-flex justify-content-center align-items-center flex-column p-4">
          <Spinner />
          <small className="text-muted mt-2">
            Validating the components for Namenode Federation enablement...
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
      <EnableNamenodeFederationProvider
        stepWizardUtilities={stepWizardUtilities}
      >
        <StepWizard
          wizardUtilities={stepWizardUtilities}
          Context={EnableNamenodeFederationContext}
        />
      </EnableNamenodeFederationProvider>
    );
  };

  const validateCanEnable = () => {
    const errorMessages = [];
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
    const zookeeperServers = hostComponents.filter(
      (component: any) => component.component_name === "ZOOKEEPER_SERVER",
    );
    if (
      !zookeeperServers.length ||
      zookeeperServers.some((component: any) => component.state !== "STARTED")
    ) {
      errorMessages.push(get(messages, "admin.nameNodeFederation.wizard.required.zookeepers"));
    }
    const journalNodes = Object.values(masterSlaveClientsData).filter((item: any) => {
      return (
        item.ServiceComponentInfo.component_name === "JOURNALNODE"
      );  
    });

    if (
      !journalNodes.length ||
      journalNodes.some(
        (journalNode: any) =>
          journalNode.ServiceComponentInfo.total_count !==
          journalNode.ServiceComponentInfo.started_count,
      )
    ) {
      errorMessages.push(get(messages, "admin.nameNodeFederation.wizard.required.journalnodes"));
    }
    if ((allHostNames || []).length < 4) {
      errorMessages.push(
        "NameNode Federation requires at least four cluster hosts.",
      );
    }
    const hdfsModel: any = allServiceModels.hdfs;
    if (
      !allModelsLoaded ||
      !hdfsModel?.isNamespaceLoaded ||
      !hdfsModel?.isNameNodeHaEnabled
    ) {
      errorMessages.push(
        "The HDFS HA namespace topology is not ready for Federation.",
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
    if (!isHostDataLoading && !hostDataError && allModelsLoaded) {
      validateCanEnable();
    }
  }, [
    isHostDataLoading,
    hostDataError,
    serviceHostComponents.length,
    serviceComponents.length,
    allModelsLoaded,
    masterSlaveClientsData,
  ]);
  
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
          modalTitle={get(messages, "admin.nameNodeFederation.button.enable")}
          modalBody={getModalBodyContent()}
          successCallback={() => {
            setShowModal(false);
            window.location.href = "/#/main/services/HDFS/summary";
          }}
          options={{
            shouldShowFooter:
              checkingForEnablement || canStartEnablement ? false : true,
            modalSize: "modal-wizard",
            cancelableViaIcon: !canStartEnablement,
          }}
        />
      ) : null}
    </>
  );
}

export default ValidateEnablement;
