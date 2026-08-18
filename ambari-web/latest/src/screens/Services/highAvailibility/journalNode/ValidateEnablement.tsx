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
import Modal from "../../../../components/Modal";
import { filter, find, flatten } from "lodash";
import Spinner from "../../../../components/Spinner";
import useStepWizard from "../../../../hooks/useStepWizard";
import wizardSteps from "./wizardSteps";
import StepWizard from "../../../../components/StepWizard";
import ClusterApi from "../../../../api/clusterApi";
import { LocalStorageOps } from "../../../../Utils/LocalStorageOps";
import { ManageJournalNodesProvider } from "./store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { AppContext } from "../../../../store/context";

function ValidateEnablement() {
  const { allServiceModels } = useContext(ServiceContext);
  const { allHostNames, supports } = useContext(AppContext);
  const stepWizardUtilities = useStepWizard(wizardSteps, 0, () => {
    window.location.href = "/#/main/services/HDFS/summary";
  });
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
            Validating the components for managing JournalNodes...
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
      <ManageJournalNodesProvider stepWizardUtilities={stepWizardUtilities}>
        <StepWizard wizardUtilities={stepWizardUtilities} />
      </ManageJournalNodesProvider>
    );
  };
  const validateCanEnable = () => {
    const messages: string[] = [];
    const hdfsModel = allServiceModels?.["hdfs"];
    if (!supports.manageJournalNode) {
      messages.push("This stack does not support Manage JournalNodes.");
    }

    const namenodes: any = find(hdfsModel?.masterComponents, [
      "componentName",
      "NAMENODE",
    ]);
    if (namenodes) {
      const activeNamenodes = filter(namenodes?.hostComponents, [
        "haStatus",
        "active",
      ]);
      const standbyNamenodes = filter(namenodes?.hostComponents, [
        "haStatus",
        "standby",
      ]);
      if (!activeNamenodes.length || !standbyNamenodes.length) {
        messages.push(
          "Manage JournalNodes Wizard requires all NameNodes be started and have Active/Standby state defined"
        );
      }
    } else {
      messages.push("Namenodes not present in HDFS service");
    }

    const journalNodeComponent = find(
      flatten([
        hdfsModel?.masterComponents || [],
        hdfsModel?.slaveComponents || [],
      ]),
      ["componentName", "JOURNALNODE"],
    );
    const journalNodeCount = Math.max(
      journalNodeComponent?.totalCount || 0,
      hdfsModel?.journalNodes?.length || 0,
    );
    if (
      journalNodeCount < 3 ||
      !(allHostNames.length > journalNodeCount || journalNodeCount > 3)
    ) {
      messages.push(
        "The current host and JournalNode counts do not allow an add or delete operation.",
      );
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
    if (allServiceModels?.["hdfs"]?.masterComponents?.length) {
      validateCanEnable();
    }
  }, [JSON.stringify(allServiceModels?.["hdfs"]?.masterComponents?.length)]);
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
          modalTitle="Manage JournalNodes Wizard"
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
