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
import { AppContext } from "../../../../store/context";
import { filter, find, map } from "lodash";
import {
  createInstallComponentTask,
  reconfigureSites,
  restartAllRequired,
  startServices,
  stopServices,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { EnableNamenodeFederationContext } from "./store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getStepData } from "../../../../Utils/Utility";
import { enableNamenodeFederationSteps } from "./wizardSteps";
import ConfigsApi from "../../../../api/configsApi";
import nameNodeFederationApi from "../../../../api/nameNodeFederationApi";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import OperationsProgress from "../../../../components/OperationsProgress";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import { ActionTypes } from "./store/types";

enum COMMANDS {
  stopRequiredServices = "stopRequiredServices",
  reconfigureServices = "reconfigureServices",
  installNameNode = "installNameNode",
  installZKFC = "installZKFC",
  startJournalNodes = "startJournalNodes",
  startInfraSolr = "startInfraSolr",
  startRangerAdmin = "startRangerAdmin",
  startRangerUsersync = "startRangerUsersync",
  startNameNodes = "startNameNodes",
  startZKFCs = "startZKFCs",
  formatNameNode = "formatNameNode",
  formatZKFC = "formatZKFC",
  startZKFC = "startZKFC",
  startNameNode = "startNameNode",
  bootstrapNameNode = "bootstrapNameNode",
  startZKFC2 = "startZKFC2",
  startNameNode2 = "startNameNode2",
  restartAllServices = "restartAllServices",
}

export function Step4() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, jumpToStep },
  } = useContext(EnableNamenodeFederationContext);
  const { clusterName, services } = useContext(AppContext);
  const { serviceModels: allServiceModels }: any = useContext(ServiceContext);
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const {getKDCSessionState}=useKDCSessionState(()=>{})

  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableNamenodeFederationSteps"
  );

  const installedServices = map(services, "ServiceInfo.service_name");

  const newNameNodeHosts = () => {
    return map(
      filter(filter(masterComponentHosts, ["component", "NAMENODE"]), [
        "isInstalled",
        false,
      ]),
      "hostName"
    );
  };

  function initializeTasks() {
    let id = 0;
    const allOps = [];
    const tasksToRemove = [];

    if (!installedServices.includes("RANGER")) {
      tasksToRemove.push(COMMANDS.startInfraSolr);
      tasksToRemove.push(COMMANDS.startRangerAdmin);
      tasksToRemove.push(COMMANDS.startRangerUsersync);
    }

    if (!installedServices.includes("AMBARI_INFRA_SOLR")) {
      tasksToRemove.push(COMMANDS.startInfraSolr);
    }

    if (!tasksToRemove.includes(COMMANDS.stopRequiredServices)) {
      allOps.push({
        id: ++id,
        label: "Stop Required Services",
        skippable: false,
        callback: async () => {
          return await stopServices(
            clusterName,
            ["ZOOKEEPER"],
            false,
            false,
            installedServices
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.reconfigureServices)) {
      allOps.push({
        id: ++id,
        label: "Reconfigure Services",
        skippable: false,
        callback: async () => {
          let configs = [];
          const data = getStepData(
            state,
            enableNamenodeFederationSteps.REVIEW,
            "overridenProperties",
            "enableNamenodeFederationSteps"
          );
          const note =
            "This configuration is created by Enable NameNode Federation wizard";

          configs.push({
            Clusters: {
              desired_config: reconfigureSites(["hdfs-site"], data, note),
            },
          });
          if (installedServices.includes("RANGER")) {
            configs.push({
              Clusters: {
                desired_config: reconfigureSites(
                  ["ranger-tagsync-site"],
                  data,
                  note
                ),
              },
            });
          }
          if (installedServices.includes("ACCUMULO")) {
            configs.push({
              Clusters: {
                desired_config: reconfigureSites(["accumulo-site"], data, note),
              },
            });
          }
          try {
            await ConfigsApi.serviceMultiConfigurations(clusterName, configs);
            return await installHDFSClients();
          } catch (error) {
            console.log(error);
          }
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.installNameNode)) {
      allOps.push({
        id: ++id,
        label: "Install NameNode",
        skippable: false,
        callback: async () => {
          return await createInstallComponentTask(
            "NAMENODE",
            newNameNodeHosts(),
            "HDFS",
            clusterName,
            ["HDFS"],
            allServiceModels["hdfs"],
            getKDCSessionState
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.installZKFC)) {
      allOps.push({
        id: ++id,
        label: "Install ZKFC",
        skippable: false,
        callback: async () => {
          return await createInstallComponentTask(
            "ZKFC",
            newNameNodeHosts(),
            "HDFS",
            clusterName,
            ["HDFS"],
            allServiceModels["hdfs"],
            getKDCSessionState
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startJournalNodes)) {
      allOps.push({
        id: ++id,
        label: "Start JournalNodes",
        skippable: false,
        callback: async () => {
          // Get JournalNode hostnames from masterComponentHosts (matching nameNode HA pattern)

          let hostNames: string[] =[]
          const jNComponents = find(allServiceModels["hdfs"]?.slaveComponents, ["componentName", "JOURNALNODE"]);
          if(jNComponents){
            hostNames=map(jNComponents?.hostComponents,"HostRoles.host_name")
          }

          return await updateComponent(
            clusterName,
            "JOURNALNODE",
            hostNames,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startNameNodes)) {
      allOps.push({
        id: ++id,
        label: "Start NameNodes",
        skippable: false,
        callback: async () => {
          const hostNames = map(
            filter(filter(masterComponentHosts, ["component", "NAMENODE"]), [
              "isInstalled",
              true,
            ]),
            "hostName"
          );
          return await updateComponent(
            clusterName,
            "NAMENODE",
            hostNames,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startZKFCs)) {
      allOps.push({
        id: ++id,
        label: "Start ZKFCs",
        skippable: false,
        callback: async () => {
          const hostNames = map(
            filter(filter(masterComponentHosts, ["component", "NAMENODE"]), [
              "isInstalled",
              true,
            ]),
            "hostName"
          );
          return await updateComponent(
            clusterName,
            "ZKFC",
            hostNames,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.formatNameNode)) {
      allOps.push({
        id: ++id,
        label: "Format NameNode",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[0];
          const data: any ={
            RequestInfo: {
              command: "FORMAT",
              context: "Format NameNode",
            },
            "Requests/resource_filters": [
              {
                service_name: "HDFS",
                component_name: "NAMENODE",
                hosts: host,
              },
            ],
          };

          return await nameNodeFederationApi.formatNameNode(clusterName, data);
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.formatZKFC)) {
      allOps.push({
        id: ++id,
        label: "Format ZKFC",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[0];
          const data: any = {
            RequestInfo: {
              command: "FORMAT",
              context: "Format ZKFC",
            },
            "Requests/resource_filters": [
              {
                service_name: "HDFS",
                component_name: "ZKFC",
                hosts: host,
              },
            ],
          };

          return await nameNodeFederationApi.formatZKFC(clusterName, data);
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startZKFC)) {
      allOps.push({
        id: ++id,
        label: "Start ZKFC",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[0];
          return updateComponent(
            clusterName,
            "ZKFC",
            host,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startInfraSolr)) {
      allOps.push({
        id: ++id,
        label: "Start Infra Solr",
        skippable: false,
        callback: async () => {
          return await startServices(
            clusterName,
            false,
            ["AMBARI_INFRA_SOLR"],
            true
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startRangerAdmin)) {
      const hostNames = map(
        filter(masterComponentHosts, ["component", "RANGER_ADMIN"]),
        "hostName"
      );
      allOps.push({
        id: ++id,
        label: "Start Ranger Admin",
        skippable: false,
        callback: async () => {
          return await updateComponent(
            clusterName,
            "RANGER_ADMIN",
            hostNames,
            "RANGER",
            "Start",
            1
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startRangerUsersync)) {
     const hostNames = map(
        filter(masterComponentHosts, ["component", "RANGER_USERSYNC"]),
        "hostName"
      );
      allOps.push({
        id: ++id,
        label: "Start Ranger Usersync",
        skippable: false,
        callback: async () => {
          return await updateComponent(
            clusterName,
            "RANGER_USERSYNC",
            hostNames,
            "RANGER",
            "Start",
            1
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startNameNode)) {
      allOps.push({
        id: ++id,
        label: "Start NameNode",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[0];
          return await updateComponent(
            clusterName,
            "NAMENODE",
            host,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.bootstrapNameNode)) {
      allOps.push({
        id: ++id,
        label: "Bootstrap NameNode",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[1];
          const data: any = {
            RequestInfo: {
              command: "BOOTSTRAP_STANDBY",
              context: "Bootstrap NameNode",
            },
            "Requests/resource_filters": [
              {
                service_name: "HDFS",
                component_name: "NAMENODE",
                hosts: host,
              },
            ],
          };

          return await nameNodeFederationApi.bootStrapNameNode(clusterName, data);
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startZKFC2)) {
      allOps.push({
        id: ++id,
        label: "Start ZKFC2",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[1];
          return await updateComponent(
            clusterName,
            "ZKFC",
            host,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.startNameNode2)) {
      allOps.push({
        id: ++id,
        label: "Start NameNode2",
        skippable: false,
        callback: async () => {
          const host = newNameNodeHosts()[1];
          return await updateComponent(
            clusterName,
            "NAMENODE",
            host,
            "HDFS",
            "Start",
            id
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.restartAllServices)) {
      allOps.push({
        id: ++id,
        label: "Restart All Services",
        skippable: false,
        callback: async () => {
          return await restartAllRequired(clusterName);
        },
      });
    }

    return allOps;
  }

  async function installHDFSClients() {
    const nnHostNames = map(
      filter(masterComponentHosts, ["component", "NAMENODE"]),
      "hostName"
    );

    // Get JournalNode hostnames from masterComponentHosts (matching nameNode HA pattern)
    const jnHostNames = map(
      filter(masterComponentHosts, ["component", "JOURNALNODE"]),
      "hostName"
    );

    const hostNames = [...new Set(nnHostNames.concat(jnHostNames))];

    return await createInstallComponentTask(
      "HDFS_CLIENT",
      hostNames,
      "HDFS",
      clusterName,
      ["HDFS"],
      allServiceModels["hdfs"],
      getKDCSessionState
    );
  }

  // Get saved operations state to maintain progress across navigation
  const savedOperationsState = getStepData(
    state,
    enableNamenodeFederationSteps.CONFIGURE_COMPONENTS,
    "operationsState",
    "enableNamenodeFederationSteps"
  );

  useEffect(() => {
    const operations = initializeTasks();
    const finalOperations = (() => {
      if (savedOperationsState && Array.isArray(savedOperationsState)) {
        return operations.map((originalOp) => {
          const savedOp = savedOperationsState.find(
            (saved: any) => saved.id === originalOp.id
          );
          return savedOp
            ? { ...originalOp, ...savedOp, callback: originalOp.callback }
            : originalOp;
        });
      }
      return operations;
    })();
    setStepOperations(finalOperations);
  }, [JSON.stringify(savedOperationsState)]);

  if (!stepOperations || stepOperations.length === 0) {
    return <div>Loading...</div>;
  }

  return (
    <>
      <OperationsProgress
        title=""
        description=""
        setCompletionStatus={setCompletionStatus}
        operations={stepOperations as any}
        dispatch={(operationsState: any) => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                operationsState,
              },
            },
          });
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus}
        onNext={() => {
          flushStateToDb("cancel"); // Clear the wizard state on completion
          window.location.href = "/#/main/services/HDFS/summary";
          window.location.reload();
        }}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(2);
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}
