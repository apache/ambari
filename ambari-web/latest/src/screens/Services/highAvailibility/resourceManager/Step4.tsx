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
import { EnableHighAvailibilityContext } from "./store/context";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getStepData } from "../../../../Utils/Utility";
import { filter, find, map, startCase } from "lodash";
import {
  createInstallComponentTask,
  reconfigureSites,
  startAllServices,
  stopServices,
} from "../../../../Utils/taskUtils";
import ConfigsApi from "../../../../api/configsApi";
import { enableResourceManagerSteps } from "./wizardSteps";
import { t } from "i18next";
import ClusterApi from "../../../../api/clusterApi";
import OperationsProgress from "../../../../components/OperationsProgress";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";

type ServiceHandlersType = {
  [key: string]: (data: any) => void;
};

function Step4() {
  enum COMMANDS {
    stopRequiredServices = "stopRequiredServices",
    installResourceManager = "installResourceManager",
    reconfigureYARN = "reconfigureYARN",
    reconfigureHAWQ = "reconfigureHAWQ",
    reconfigureHDFS = "reconfigureHDFS",
    startAllServices = "startAllServices",
  }

  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services } = useContext(AppContext);
  const { serviceModels: allServiceModels }: any = useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilitySteps"
  );

  const selectedServices = map(services, "ServiceInfo.service_name");

  function initializeTasks() {
    let id = 0;
    const allOps = [];
    const tasksToRemove = [];

    if (!selectedServices.includes("HAWQ")) {
      {
        tasksToRemove.push(COMMANDS.reconfigureHAWQ);
      }
    }

    if (!tasksToRemove.includes(COMMANDS.stopRequiredServices)) {
      allOps.push({
        id: id++,
        label: "Stop Required Services",
        skippable: false,
        callback: async () => {
          return await stopServices(
            clusterName,
            ["HDFS"],
            true,
            false,
            selectedServices
          );
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.installResourceManager)) {
      allOps.push({
        id: id++,
        label: "Install ResourceManager",
        skippable: false,
        callback: async () => {
          const hostName = find(
            filter(masterComponentHosts, ["component", "RESOURCEMANAGER"]),
            ["isInstalled", false]
          )?.hostName;
          console.log("Host name for ResourceManager:", hostName);

          return await createInstallComponentTask(
            "RESOURCEMANAGER",
            hostName,
            "YARN",
            clusterName,
            ["YARN"],
            allServiceModels["yarn"],
            getKDCSessionState
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.reconfigureYARN)) {
      allOps.push({
        id: id++,
        label: "Reconfigure Yarn",
        skippable: false,
        callback: async () => {
          const reconfigureYarnResponse = await loadConfigTags("Yarn");
          return reconfigureYarnResponse;
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.reconfigureHAWQ)) {
      allOps.push({
        id: id++,
        label: "Reconfigure HAWQ",
        skippable: false,
        callback: async () => {
          return await loadConfigTags("Hawq");
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.reconfigureHDFS)) {
      allOps.push({
        id: id++,
        label: "Reconfigure HDFS",
        skippable: false,
        callback: async () => {
          return await loadConfigTags("Hdfs");
        },
      });
    }

    allOps.push({
      id: id++,
      label: "Start All Services",
      skippable: false,
      callback: async () => {
        return await startAllServices(clusterName);
      },
    });

    return allOps;
  }

  async function loadConfigTags(service: string) {
    const onLoadServiceConfigTags =
      "onLoad" + startCase(service) + "ConfigTags";
    console.log("Loading config tags for service:", service);
    try {
      const response = await ConfigsApi.loadConfigTags(clusterName);
      const data = response.data || response;
      if (onLoadServiceConfigTags in serviceHandlers) {
        return await serviceHandlers[onLoadServiceConfigTags](data);
      }
    } catch (error) {
      console.log(error);
      throw error;
    }
  }

  const serviceHandlers: ServiceHandlersType = {
    onLoadYarnConfigTags: async (data: any) => {
      try {
        const urlParams =
          "(type=yarn-site&tag=" +
          data.Clusters.desired_configs["yarn-site"].tag +
          ")";
        const response = await ConfigsApi.getConfigsByTags(
          clusterName,
          urlParams
        );
        if (response) {
          return await onLoadConfigs(response, "yarn-site");
        }
      } catch (error) {
        console.log(error);
        throw error;
      }
    },
    onLoadHawqConfigTags: async (data: any) => {
      try {
        const urlParams =
          "(type=yarn-client&tag=" +
          data.Clusters.desired_configs["yarn-client"].tag +
          ")";
        const response = await ConfigsApi.getConfigsByTags(
          clusterName,
          urlParams
        );
        if (response) {
          return await onLoadConfigs(response, "yarn-client");
        }
      } catch (error) {
        console.log(error);
        throw error;
      }
    },
    onLoadHdfsConfigTags: async (data: any) => {
      try {
        const urlParams =
          "(type=core-site&tag=" +
          data.Clusters.desired_configs["core-site"].tag +
          ")";
        const response = await ConfigsApi.getConfigsByTags(
          clusterName,
          urlParams
        );
        if (response) {
          return await onLoadConfigs(response, "core-site");
        }
      } catch (error) {
        console.log(error);
        throw error;
      }
    },
  };

  async function onLoadConfigs(data: any, type: string) {
    const configs = getStepData(
      state,
      enableResourceManagerSteps.REVIEW,
      "stepConfigs.configs",
      "enableHighAvailibilitySteps"
    );
    const propertiesToAdd = configs.filter(
      (config: any) => config.filename === type
    );

    propertiesToAdd.forEach((property: any) => {
      if (data?.items?.[0])
        data.items[0].properties[property.name] = property.value;
      else {
        data.items.push({
          type: type,
          properties: {
            [property.name]: property.value,
          },
        });
      }
    });

    const configData = reconfigureSites(
      [type],
      data,
      t("admin.highAvailability.step4.save.configuration.note")
    );

    return await ClusterApi.updateCluster(clusterName, {
      Clusters: {
        desired_config: configData,
      },
    });
  }

  const savedOperationsState = getStepData(
    state,
    enableResourceManagerSteps.CREATE_CHECKPOINT,
    "operationsState",
    "enableHighAvailibilitySteps"
  );

  useEffect(() => {
    const operations = (() => {
      const initialOperations = initializeTasks();

      if (savedOperationsState && Array.isArray(savedOperationsState)) {
        return initialOperations.map((originalOp) => {
          const savedOp = savedOperationsState.find(
            (saved: any) => saved.id === originalOp.id
          );
          return savedOp
            ? { ...originalOp, ...savedOp, callback: originalOp.callback }
            : originalOp;
        });
      }

      return initialOperations;
    })();
    setStepOperations(operations);
  }, [JSON.stringify(savedOperationsState)]);


  if(!stepOperations || stepOperations.length===0){
    return <div>Loading...</div>
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
          flushStateToDb("cancel");
          window.location.href = "#/main/services/YARN/summary";
          window.location.reload();
        }}
        onBack={() => {
          flushStateToDb("back");
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step4;
