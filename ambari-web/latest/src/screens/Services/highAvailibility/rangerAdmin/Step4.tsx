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
import OperationsProgress from "../../../../components/OperationsProgress";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import {
  createInstallComponentTask,
  reconfigureSites,
  startServices,
  stopServices,
} from "../../../../Utils/taskUtils";
import { AppContext } from "../../../../store/context";
import { filter, map } from "lodash";
import { getStepData, role } from "../../../../Utils/Utility";
import { ServiceContext } from "../../../../store/ServiceContext";
import ConfigsApi from "../../../../api/configsApi";
import { wizardConfigs } from "./wizardConstants";
import { enableRangerAdminSteps } from "./wizardSteps";
import { ActionTypes } from "./store/types";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";

function Step4() {
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const { clusterName, services } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  } = useContext(EnableHighAvailibilityRangerAdminContext);
  const { allServiceModels } = useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const selectedServices = map(services, "ServiceInfo.service_name");
  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilityRangerAdminSteps"
  );

  function getOperations() {
    const allOperations = [];
    allOperations.push({
      id: 1,
      label: "Stop All Services",
      skippable: false,
      callback: async () => {
        return await stopServices(
          clusterName,
          [],
          true,
          true,
          selectedServices
        );
      },
    });
    allOperations.push({
      id: 2,
      label: "Install Ranger Admin",
      skippable: false,
      callback: async () => {
        const hostNames: string[] = map(
          filter(filter(masterComponentHosts, ["component", "RANGER_ADMIN"]), [
            "isInstalled",
            false,
          ]),
          "hostName"
        );
        return await createInstallComponentTask(
          "RANGER_ADMIN",
          hostNames,
          "RANGER",
          clusterName,
          ["RANGER"],
          allServiceModels?.["ranger"],
          getKDCSessionState
        );
      },
    });
    allOperations.push({
      id: 3,
      label: "Reconfigure Ranger",
      skippable: false,
      callback: async () => {
        try {
          const data = await ConfigsApi.loadConfigTags(clusterName);
          let urlParams: string[] = [];
          const siteNamesToFetch = map(wizardConfigs, "siteName");
          siteNamesToFetch.map(function (siteName) {
            if (siteName in data.Clusters.desired_configs) {
              urlParams.push(
                "(type=" +
                  siteName +
                  "&tag=" +
                  data.Clusters.desired_configs[siteName].tag +
                  ")"
              );
            }
          });
          const configsData = await ConfigsApi.reassignLoadConfigs(
            clusterName,
            urlParams.join("|")
          );
          const configs: any[] = [];
          const note = `This configuration is created by Enable ${role(
            "RANGER_ADMIN",
            false
          )} HA wizard`;
          wizardConfigs.map((item: any) => {
            var config = configsData.items.findProperty("type", item.siteName);
            if (config) {
              config.properties[item.propertyName] = getStepData(
                state,
                enableRangerAdminSteps.GET_STARTED,
                "loadBalancerUrl",
                "enableHighAvailibilityRangerAdminSteps"
              );
              configs.push({
                Clusters: {
                  desired_config: reconfigureSites([item.siteName], data, note),
                },
              });
            }
          });
          return await ConfigsApi.updateServiceMultiConfigurations(
            clusterName,
            {
              configs,
            }
          );
        } catch (error) {
          console.error("Error Encountered while reconfiguring Ranger", error);
        }
      },
    });
    allOperations.push({
      id: 4,
      label: "Start All Services",
      skippable: false,
      callback: async () => {
        return await startServices(clusterName, false, [], false);
      },
    });
    return allOperations;
  }


  const savedOperationsState = getStepData(
    state,
    enableRangerAdminSteps.INSTALL_START_TEST,
    "operationsState",
    "enableHighAvailibilityRangerAdminSteps"
  );

  useEffect(() => {
      const initialOperations = getOperations();
      const operations = (() => {
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
          flushStateToDb("cancel");
          window.location.href = "#/main/services/RANGER/summary";
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
