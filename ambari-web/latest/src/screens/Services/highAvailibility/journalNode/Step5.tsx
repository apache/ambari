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
import { Card } from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ManageJournalNodesContext } from "./store/context";
import { cloneDeep, filter, find, get, map, sortBy, uniq } from "lodash";
import { getStepData } from "../../../../Utils/Utility";
import useHDFSConfigsTags from "../../../../hooks/useConfigsTags";

function Step5() {
  const { allServiceModels } = useContext(ServiceContext);
  const hdfsModel = allServiceModels["hdfs"];
  const namespacesLoaded = hdfsModel?.isNamespaceLoaded;
  const { configsData } = useHDFSConfigsTags();
  const {
    state,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(ManageJournalNodesContext);

  function getBodyText() {
    const step1Data = getStepData(
      state,
      "ASSIGN_JOURNALNODES",
      "masterComponentHosts",
      "manageJournalNodesSteps"
    );
    const masterComponentGroups = (function () {
      let result: any = [];
      const componentsCopy = cloneDeep(get(hdfsModel, "masterComponents"));
      const allNameNodes = map(
        componentsCopy.find(
          (component: any) => component.componentName === "NAMENODE"
        )?.hostComponents,
        "HostRoles"
      );
      allNameNodes.forEach((nameNode: any) => {
        const nameSpace = nameNode.haNameSpace || "default";
        const hostName = nameNode.hostName;
        const clusterId = nameNode.clusterIdValue || "default";
        const existingNameSpace = find(result, ["name", nameSpace]);
        const currentNameSpace = existingNameSpace || {
          name: nameSpace,
          title: nameSpace,
          hosts: [],
          components: ["NAMENODE", "ZKFC"],
          clusterId,
        };
        if (!existingNameSpace) {
          result.push(currentNameSpace);
        }
        if (!currentNameSpace.hosts.includes(hostName)) {
          currentNameSpace.hosts.push(hostName);
        }
      });
      return sortBy(result, "name");
    })();
    const currentJournalNodes = find(
      filter(step1Data, ["component", "JOURNALNODE"]),
      ["isInstalled", true]
    )?.hostName;
    const nameSpaces = map(masterComponentGroups, "name");
    const hdfsSiteConfigs = find(configsData?.items, ["type", "hdfs-site"]);
    const configProperties = hdfsSiteConfigs ? hdfsSiteConfigs.properties : {};
    const directories =
      nameSpaces.length > 1
        ? uniq(
            nameSpaces.map(
              (ns) => configProperties[`dfs.journalnode.edits.dir.${ns}`]
            )
          )
        : [configProperties["dfs.journalnode.edits.dir"]];
    const directoriesString = directories.map((dir) => `${dir}`).join(", ");
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

  if (!namespacesLoaded) {
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
        isNextEnabled={hdfsModel.isNamespaceLoaded}
        onNext={() => {
          flushStateToDb("next");
          handleNextImperitive();
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </div>
  );
}
export default Step5;
