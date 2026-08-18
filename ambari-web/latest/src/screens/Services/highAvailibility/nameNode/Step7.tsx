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
import { getStepData } from "../../../../Utils/Utility";
import { EnableHighAvailibilityContext } from "./store/context";
import { filter, find, get, map } from "lodash";
import { startServices, updateComponent } from "../../../../Utils/taskUtils";
import { AppContext } from "../../../../store/context";
import OperationsProgress from "../../../../components/OperationsProgress";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import { enableNamenodeSteps } from "./wizardSteps";
import { mergeSavedOperations } from "../haWorkflowUtils";
import { HostsApi } from "../../../../api/hostsApi";
import { Alert, Button } from "react-bootstrap";

enum COMMANDS {
  startZooKeeperServers = "startZooKeeperServers",
  startAmbariInfra = "startAmbariInfra",
  startMysqlServer = "startMysqlServer",
  startRanger = "startRanger",
  startNameNode = "startNameNode",
}

function Step7() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services } = useContext(AppContext);
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const [topologyError, setTopologyError] = useState("");
  const [topologyRetry, setTopologyRetry] = useState(0);
  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilitySteps"
  );

  const selectedServices = map(services, "ServiceInfo.service_name");

  function initializeTasks(componentItems: any[]) {
    const allOps = [];
    const component = (componentName: string) =>
      find(componentItems, [
        "ServiceComponentInfo.component_name",
        componentName,
      ]);
    const componentHosts = (componentName: string) =>
      map(
        get(component(componentName), "host_components", []),
        (hostComponent: any) => get(hostComponent, "HostRoles.host_name"),
      ).filter(Boolean);
    const isInstalled = (componentName: string) =>
      Number(
        get(component(componentName), "ServiceComponentInfo.installed_count", 0),
      ) > 0;
    const hasInfraModel = componentItems.some(
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") ===
        "AMBARI_INFRA_SOLR",
    );

    allOps.push({
      id: COMMANDS.startZooKeeperServers,
      label: "Start Zookeeper Servers",
      skippable: false,
      callback: async () =>
        await updateComponent(
          clusterName,
          "ZOOKEEPER_SERVER",
          componentHosts("ZOOKEEPER_SERVER"),
          "ZOOKEEPER",
          "Start",
          1,
        ),
    });
    if (
      selectedServices.includes("AMBARI_INFRA_SOLR") &&
      hasInfraModel
    ) {
      allOps.push({
        id: COMMANDS.startAmbariInfra,
        label: "Start Ambari Infra",
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
    if (isInstalled("MYSQL_SERVER")) {
      allOps.push({
        id: COMMANDS.startMysqlServer,
        label: "Start MySQL Server",
        skippable: false,
        callback: async () =>
          await updateComponent(
            clusterName,
            "MYSQL_SERVER",
            componentHosts("MYSQL_SERVER"),
            "HIVE",
            "Start",
            1,
          ),
      });
    }
    if (selectedServices.includes("RANGER") && isInstalled("RANGER_ADMIN")) {
      allOps.push({
        id: COMMANDS.startRanger,
        label: "Start Ranger",
        skippable: false,
        callback: async () => {
          return await updateComponent(
            clusterName,
            "RANGER_ADMIN",
            componentHosts("RANGER_ADMIN"),
            "RANGER",
            "Start",
            1
          );
        },
      });
    }
    const currentNameNodeHost = map(
      filter(
        masterComponentHosts,
        (masterComponentHost: any) =>
          masterComponentHost.component === "NAMENODE" &&
          masterComponentHost.isInstalled,
      ),
      "hostName",
    );
    allOps.push({
      id: COMMANDS.startNameNode,
      label: "Start NameNode",
      skippable: false,
      callback: async () =>
        await updateComponent(
          clusterName,
          "NAMENODE",
          currentNameNodeHost,
          "HDFS",
          "Start",
          1,
        ),
    });
    return allOps;
  }
  const savedOperationsState = getStepData(
    state,
    enableNamenodeSteps.START_COMPONENTS,
    "operationsState",
    "enableHighAvailibilitySteps"
  );
  useEffect(() => {
    let cancelled = false;
    const loadTopology = async () => {
      setTopologyError("");
      setStepOperations([]);
      try {
        const fields =
          "ServiceComponentInfo/service_name,ServiceComponentInfo/component_name," +
          "ServiceComponentInfo/installed_count,host_components/HostRoles/host_name";
        const response = await HostsApi.getClusterComponents(clusterName, fields);
        const componentItems = response?.items || [];
        const zooKeeper = find(componentItems, [
          "ServiceComponentInfo.component_name",
          "ZOOKEEPER_SERVER",
        ]);
        const currentNameNode = find(
          masterComponentHosts || [],
          (item: any) => item.component === "NAMENODE" && item.isInstalled,
        );
        if (
          !componentItems.length ||
          !get(zooKeeper, "host_components", []).length ||
          !currentNameNode?.hostName
        ) {
          throw new Error(
            "Ambari could not resolve the ZooKeeper and current NameNode topology.",
          );
        }
        if (!cancelled) {
          setStepOperations(
            mergeSavedOperations(
              initializeTasks(componentItems),
              savedOperationsState,
            ),
          );
        }
      } catch (error: any) {
        if (!cancelled) {
          setTopologyError(
            error?.response?.data?.message ||
              error?.message ||
              "Ambari could not load the component topology.",
          );
        }
      }
    };
    void loadTopology();
    return () => {
      cancelled = true;
    };
  }, [
    clusterName,
    topologyRetry,
    JSON.stringify(savedOperationsState),
  ]);
  if (topologyError) {
    return (
      <Alert variant="danger">
        {topologyError}
        <Button
          size="sm"
          className="ms-3"
          onClick={() => setTopologyRetry((value) => value + 1)}
        >
          Retry
        </Button>
      </Alert>
    );
  }
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
        dispatch={async (operationsState: any) => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                operationsState,
              },
            },
          });
          await flushStateToDb();
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus}
        onNext={async () => {
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onBack={() => {
          jumpToStep(6);
          flushStateToDb("back");
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        cancelConfirmationBody="NameNode HA changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Complete the documented manual recovery before making further HDFS topology changes."
      />
    </>
  );
}

export default Step7;
