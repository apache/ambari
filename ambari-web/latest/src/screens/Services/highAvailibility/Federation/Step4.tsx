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
import { Alert } from "react-bootstrap";
import { map } from "lodash";
import federationApi from "../../../../api/federationApi";
import OperationsProgress from "../../../../components/OperationsProgress";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import {
  createInstallComponentTask,
  startServices,
  stopServices,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { getStepData } from "../../../../Utils/Utility";
import { mergeSavedOperations } from "../haWorkflowUtils";
import { EnableNamenodeFederationContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { enableNamenodeFederationSteps } from "./wizardSteps";
import { federationTaskKeys } from "./workflowUtils";

interface ProgressOperation {
  id: string;
  label: string;
  skippable: false;
  callback: () => Promise<unknown>;
  status?: string;
  requestId?: string | number;
  [key: string]: unknown;
}

const labels: Record<string, string> = {
  stopRequiredServices: "Stop Required Services",
  reconfigureServices: "Reconfigure Services",
  installNameNode: "Install NameNodes",
  installZKFC: "Install ZKFCs",
  startJournalNodes: "Start JournalNodes",
  startInfraSolr: "Start Infra Solr",
  startRangerAdmin: "Start Ranger Admin",
  startRangerUsersync: "Start Ranger Usersync",
  startNameNodes: "Start Existing NameNodes",
  startZKFCs: "Start Existing ZKFCs",
  formatNameNode: "Format First NameNode",
  formatZKFC: "Format ZKFC",
  startZKFC: "Start First ZKFC",
  startNameNode: "Start First NameNode",
  bootstrapNameNode: "Bootstrap Standby NameNode",
  startZKFC2: "Start Second ZKFC",
  startNameNode2: "Start Second NameNode",
  restartAllServices: "Restart Non-Federation Components",
};

const errorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message || error?.message || fallback;

export function Step4() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  } = useContext(EnableNamenodeFederationContext);
  const { clusterName, services } = useContext(AppContext);
  const { serviceModels, masterSlaveClientsData }: any =
    useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<ProgressOperation[]>([]);
  const [planError, setPlanError] = useState("");
  const [workflowError, setWorkflowError] = useState("");
  const [isCompleting, setIsCompleting] = useState(false);
  const installedServices = map(services, "ServiceInfo.service_name");
  const masterComponentHosts =
    getStepData(
      state,
      enableNamenodeFederationSteps.SELECT_HOSTS,
      "masterComponentHosts",
      "enableNamenodeFederationSteps",
    ) || [];
  const configSnapshot = getStepData(
    state,
    enableNamenodeFederationSteps.REVIEW,
    "overridenProperties",
    "enableNamenodeFederationSteps",
  );
  const savedOperations = getStepData(
    state,
    enableNamenodeFederationSteps.CONFIGURE_COMPONENTS,
    "operationsState",
    "enableNamenodeFederationSteps",
  );
  const newNameNodeHosts = masterComponentHosts
    .filter(
      (host: any) => host.component === "NAMENODE" && !host.isInstalled,
    )
    .map((host: any) => host.hostName);
  const existingNameNodeHosts = masterComponentHosts
    .filter(
      (host: any) => host.component === "NAMENODE" && host.isInstalled,
    )
    .map((host: any) => host.hostName);

  const hostsForComponent = (name: string): string[] => {
    const component = Object.values(masterSlaveClientsData || {}).find(
      (item: any) => item?.ServiceComponentInfo?.component_name === name,
    ) as any;
    return [
      ...new Set(
        (component?.host_components || [])
          .map((item: any) => item.HostRoles?.host_name)
          .filter(Boolean),
      ),
    ] as string[];
  };
  const journalNodeHosts =
    getStepData(
      state,
      enableNamenodeFederationSteps.REVIEW,
      "journalNodeHosts",
      "enableNamenodeFederationSteps",
    ) || [];

  useEffect(() => {
    if (
      newNameNodeHosts.length !== 2 ||
      !existingNameNodeHosts.length ||
      !journalNodeHosts.length ||
      !configSnapshot?.items
    ) {
      setPlanError(
        "The persisted Federation host or configuration snapshot is incomplete. Return to Review and rebuild it.",
      );
      return;
    }
    setPlanError("");
    const callbacks = taskCallbacks();
    const operations = federationTaskKeys(installedServices).map((id) => ({
      id,
      label: labels[id],
      skippable: false as const,
      callback: callbacks[id],
    }));
    setStepOperations(
      mergeSavedOperations<ProgressOperation>(operations, savedOperations),
    );
  }, [JSON.stringify(savedOperations)]);

  function taskCallbacks(): Record<string, () => Promise<unknown>> {
    const componentCommand = (
      command: string,
      context: string,
      componentName: string,
      host: string,
    ) =>
      federationApi.executeComponentCommand(clusterName, {
        command,
        context,
        serviceName: "HDFS",
        componentName,
        hosts: host,
      });
    const startComponent = (
      componentName: string,
      hosts: string | string[],
      serviceName = "HDFS",
    ) =>
      updateComponent(
        clusterName,
        componentName,
        hosts,
        serviceName,
        "Start",
        1,
      );
    return {
      stopRequiredServices: () =>
        stopServices(
          clusterName,
          ["ZOOKEEPER"],
          false,
          false,
          installedServices,
        ),
      reconfigureServices: async () => {
        const types = ["hdfs-site"];
        if (installedServices.includes("RANGER")) {
          types.push("ranger-tagsync-site");
        }
        if (installedServices.includes("ACCUMULO")) {
          types.push("accumulo-site");
        }
        await federationApi.saveConfigurationTypes(
          clusterName,
          configSnapshot,
          types,
          "This configuration is created by Enable NameNode Federation wizard",
        );
        const clientHosts = [
          ...new Set([
            ...existingNameNodeHosts,
            ...newNameNodeHosts,
            ...journalNodeHosts,
          ]),
        ];
        return await createInstallComponentTask(
          "HDFS_CLIENT",
          clientHosts,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels.hdfs,
          getKDCSessionState,
        );
      },
      installNameNode: () =>
        createInstallComponentTask(
          "NAMENODE",
          newNameNodeHosts,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels.hdfs,
          getKDCSessionState,
        ),
      installZKFC: () =>
        createInstallComponentTask(
          "ZKFC",
          newNameNodeHosts,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels.hdfs,
          getKDCSessionState,
        ),
      startJournalNodes: () => startComponent("JOURNALNODE", journalNodeHosts),
      startInfraSolr: () =>
        startServices(
          clusterName,
          false,
          ["AMBARI_INFRA_SOLR"],
          true,
        ),
      startRangerAdmin: () =>
        startComponent("RANGER_ADMIN", hostsForComponent("RANGER_ADMIN"), "RANGER"),
      startRangerUsersync: () =>
        startComponent(
          "RANGER_USERSYNC",
          hostsForComponent("RANGER_USERSYNC"),
          "RANGER",
        ),
      startNameNodes: () => startComponent("NAMENODE", existingNameNodeHosts),
      startZKFCs: () => startComponent("ZKFC", existingNameNodeHosts),
      formatNameNode: () =>
        componentCommand("FORMAT", "Format NameNode", "NAMENODE", newNameNodeHosts[0]),
      formatZKFC: () =>
        componentCommand("FORMAT", "Format ZKFC", "ZKFC", newNameNodeHosts[0]),
      startZKFC: () => startComponent("ZKFC", newNameNodeHosts[0]),
      startNameNode: () => startComponent("NAMENODE", newNameNodeHosts[0]),
      bootstrapNameNode: () =>
        componentCommand(
          "BOOTSTRAP_STANDBY",
          "Bootstrap NameNode",
          "NAMENODE",
          newNameNodeHosts[1],
        ),
      startZKFC2: () => startComponent("ZKFC", newNameNodeHosts[1]),
      startNameNode2: () => startComponent("NAMENODE", newNameNodeHosts[1]),
      restartAllServices: () =>
        federationApi.restartNonFederationComponents(clusterName),
    };
  }

  if (planError) return <Alert variant="danger">{planError}</Alert>;
  if (!stepOperations.length) return <div>Loading the Federation task plan...</div>;

  return (
    <>
      {workflowError ? <Alert variant="danger">{workflowError}</Alert> : null}
      <OperationsProgress
        title=""
        description=""
        setCompletionStatus={setCompletionStatus}
        operations={stepOperations}
        errorCallback={setWorkflowError}
        dispatch={async (operationsState: any) => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: { operationsState },
            },
          });
          await flushStateToDb();
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus && !isCompleting}
        isBackEnabled={false}
        cancelConfirmationBody={
          "Exit this wizard? Completed server changes are not rolled back. Ambari will preserve the checkpoint so the workflow can be resumed."
        }
        onNext={async () => {
          setIsCompleting(true);
          setWorkflowError("");
          try {
            await flushStateToDb("complete");
            window.location.href = "/#/main/services/HDFS/summary";
          } catch (error: any) {
            setWorkflowError(
              errorMessage(error, "Ambari could not clear the completed workflow."),
            );
            setIsCompleting(false);
          }
        }}
        onBack={() => undefined}
        onCancel={() => void flushStateToDb("cancel")}
      />
    </>
  );
}
