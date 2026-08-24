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

import { useContext, useState } from "react";
import { Alert, Card } from "react-bootstrap";
import { map } from "lodash";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { getStepData } from "../../../../Utils/Utility";
import { getHdfsNamespaces } from "../haWorkflowUtils";
import HostAssignment from "./HostAssignment";
import { EnableNamenodeFederationContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableNamenodeFederationSteps } from "./wizardSteps";
import {
  ComponentAssignment,
  validateComponentAssignments,
} from "./workflowUtils";

function Step2() {
  const { services } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      handleNextImperitive,
      handleBackImperitive,
      currentStep,
    },
  } = useContext(EnableNamenodeFederationContext);
  const savedAssignments =
    getStepData(
      state,
      enableNamenodeFederationSteps.SELECT_HOSTS,
      "masterComponentHosts",
      "enableNamenodeFederationSteps",
    ) || [];
  const installedNameNodeHosts = getHdfsNamespaces(
    allServiceModels.hdfs,
  ).flatMap((namespace) => namespace.hosts);
  const [assignments, setAssignments] = useState<ComponentAssignment[]>(
    savedAssignments,
  );
  const [assignmentError, setAssignmentError] = useState(
    "Select exactly two additional NAMENODE hosts.",
  );
  const [persistenceError, setPersistenceError] = useState("");

  const storeAssignments = (
    nextAssignments: ComponentAssignment[],
    unavailableHosts: string[],
  ) => {
    setAssignments(nextAssignments);
    setAssignmentError(
      validateComponentAssignments(
        nextAssignments,
        "NAMENODE",
        2,
        unavailableHosts,
      ),
    );
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: currentStep.name,
        data: { masterComponentHosts: nextAssignments },
      },
    });
  };

  return (
    <>
      <h2 className="step-title">Select Hosts</h2>
      <p className="step-description">
        Select two hosts for the NameNodes in the new nameservice.
      </p>
      {assignmentError && assignments.length ? (
        <Alert variant="danger">{assignmentError}</Alert>
      ) : null}
      {persistenceError ? <Alert variant="danger">{persistenceError}</Alert> : null}
      <Card className="mt-2">
        <Card.Body>
          <HostAssignment
            componentName="NAMENODE"
            componentLabel="NameNode"
            installedHosts={installedNameNodeHosts}
            initialAssignments={savedAssignments}
            additionalCount={2}
            services={map(services, "ServiceInfo.service_name")}
            onChange={storeAssignments}
          />
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={!assignmentError && assignments.length > 0}
        onBack={async () => {
          setPersistenceError("");
          try {
            await flushStateToDb("back");
            await handleBackImperitive();
          } catch (error: any) {
            setPersistenceError(
              error?.response?.data?.message ||
                error?.message ||
                "Ambari could not persist the wizard state.",
            );
          }
        }}
        onNext={async () => {
          setPersistenceError("");
          try {
            await flushStateToDb("next");
            await handleNextImperitive();
          } catch (error: any) {
            setPersistenceError(
              error?.response?.data?.message ||
                error?.message ||
                "Ambari could not persist the wizard state.",
            );
          }
        }}
        onCancel={() => void flushStateToDb("cancel")}
      />
    </>
  );
}

export default Step2;
