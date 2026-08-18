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

import { useContext } from "react";
import { ServiceContext } from "../../../../store/ServiceContext";
import Spinner from "../../../../components/Spinner";
import { Alert, Button, Card } from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ManageJournalNodesContext } from "./store/context";
import { filter, find } from "lodash";
import { getStepData } from "../../../../Utils/Utility";
import useHDFSConfigsTags from "../../../../hooks/useConfigsTags";
import { getJournalNodeDirectories } from "../haWorkflowUtils";

function Step5() {
  const { allServiceModels } = useContext(ServiceContext);
  const hdfsModel = allServiceModels["hdfs"];
  const namespacesLoaded = hdfsModel?.isNamespaceLoaded;
  const {
    configsData,
    configsError,
    isConfigsLoading,
    reloadConfigs,
  } = useHDFSConfigsTags();
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(ManageJournalNodesContext);

  const step1Data = getStepData(
    state,
    "ASSIGN_JOURNALNODES",
    "masterComponentHosts",
    "manageJournalNodesSteps",
  );
  const currentJournalNodes = find(
    filter(step1Data, ["component", "JOURNALNODE"]),
    ["isInstalled", true],
  )?.hostName;
  const hdfsSiteConfigs = find(configsData?.items, ["type", "hdfs-site"]);
  const directoryEvaluation = getJournalNodeDirectories(
    hdfsModel,
    hdfsSiteConfigs?.properties,
  );
  const directoryError = directoryEvaluation.missingProperties.length
    ? `Ambari could not load the JournalNode directory configuration: ${directoryEvaluation.missingProperties.join(
        ", ",
      )}.`
    : "";

  function getBodyText() {
    const directoriesString = directoryEvaluation.directories.join(", ");
    return (
      <ul>
        <li>
          Login to the JournalNode host{" "}
          <span className="fw-bold">{currentJournalNodes}</span>.
        </li>
        <li>
          Create a tarball of the Journal directiories:{" "}
          <b>{directoriesString}</b>.
        </li>
        <li>
          Copy the tarball on the new JournalNodes and untar at the respective
          locations as in Step 2.
        </li>
      </ul>
    );
  }

  if (configsError || (!isConfigsLoading && directoryError)) {
    return (
      <Alert variant="danger">
        {configsError || directoryError}
        <Button className="ms-3" size="sm" onClick={() => void reloadConfigs()}>
          Retry
        </Button>
      </Alert>
    );
  }

  if (!namespacesLoaded || isConfigsLoading) {
    return <Spinner />;
  }

  return (
    <div>
      <h3 className="step-title">Manual Steps Required</h3>
      <p className="step-description light-text">
        {" "}
        Copy JournalNode directories
      </p>
      <Card className="mt-4">
        <Card.Body>{getBodyText()}</Card.Body>
      </Card>
      <WizardFooter
        onBack={() => {
          flushStateToDb("back");
          handleBackImperitive();
        }}
        step={currentStep}
        isNextEnabled={hdfsModel.isNamespaceLoaded && !directoryError}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
      />
    </div>
  );
}
export default Step5;
