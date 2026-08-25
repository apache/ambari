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
import { RequestApi } from "../../../../api/requestApi";
import { AppContext } from "../../../../store/context";
import { EnableHighAvailibilityContext } from "./store/context";
import { getStepData, role } from "../../../../Utils/Utility";
import { filter, find, has, map, uniq } from "lodash";
import {
  createInstallComponentTask,
  reconfigureSites,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { ServiceContext } from "../../../../store/ServiceContext";
import OperationsProgress from "../../../../components/OperationsProgress";
import { HostsApi } from "../../../../api/hostsApi";
import { enableNamenodeSteps } from "./wizardSteps";
import ConfigsApi from "../../../../api/configsApi";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import {
  buildDesiredConfigQuery,
  mergeReviewedConfigs,
  mergeSavedOperations,
} from "../haWorkflowUtils";

function Step5() {
  const { clusterName, services, isKerberosEnabled } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    showCancel,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const { serviceModels } = useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  //@ts-ignore
  const [completionStatus, setCompletionStatus] = useState(false);
  const [stepOperations, setStepOperations] = useState<any>([]);
  const masterComponentHosts = getStepData(
    state,
    "SELECT_HOSTS",
    "masterComponentHosts",
    "enableHighAvailibilitySteps"
  );
  const selectedServices = map(services, "ServiceInfo.service_name");

  async function updateConfigProperties(configData: any) {
    const siteNames = ["hdfs-site", "core-site"].concat(
      getRangerSiteNames(configData)
    );
    const missingSites = siteNames.filter(
      (siteName) => !find(configData?.items, ["type", siteName]),
    );
    if (missingSites.length) {
      throw new Error(
        `The NameNode HA configuration is missing: ${missingSites.join(", ")}.`,
      );
    }
    const note = `This configuration is created by Enable ${role(
      "NAMENODE",
      false
    )} HA wizard`;
    const desiredConfig = reconfigureSites(siteNames, configData, note);
    try {
      await ConfigsApi.updateServiceConfigurations(clusterName, {
        desired_config: desiredConfig,
      });
      const nnHostNames = map(
        filter(masterComponentHosts, ["component", "NAMENODE"]),
        "hostName"
      );
      const jnHostNames = map(
        filter(masterComponentHosts, ["component", "JOURNALNODE"]),
        "hostName"
      );
      const hostNames = uniq(nnHostNames.concat(jnHostNames));
      return await createInstallComponentTask(
        "HDFS_CLIENT",
        hostNames,
        "HDFS",
        clusterName,
        ["HDFS"],
        serviceModels["hdfs"],
        getKDCSessionState
      );
    } catch (err) {
      console.error("Could not update configs", err);
      throw err;
    }
  }

  async function loadCurrentSecureConfigs(reviewedConfigs: any) {
    const siteNames = ["hdfs-site", "core-site"].concat(
      getRangerSiteNames(reviewedConfigs),
    );
    const tagsResponse = await ConfigsApi.loadConfigTags(clusterName);
    const query = buildDesiredConfigQuery(
      tagsResponse?.Clusters?.desired_configs,
      siteNames,
    );
    const currentConfigs = await ConfigsApi.getConfigsByTags(clusterName, query);
    const loadedTypes = new Set(
      (currentConfigs?.items || []).map((item: any) => item.type),
    );
    const missingSites = siteNames.filter((siteName) => !loadedTypes.has(siteName));
    if (missingSites.length) {
      throw new Error(
        `Ambari did not return the current configuration for: ${missingSites.join(", ")}.`,
      );
    }
    const configsToRemove = getStepData(
      state,
      enableNamenodeSteps.REVIEW,
      "configsToRemove",
      "enableHighAvailibilitySteps",
    );
    return mergeReviewedConfigs(
      currentConfigs,
      reviewedConfigs,
      configsToRemove,
    );
  }

  function getRangerSiteNames(data: any) {
    var siteNames = [];
    if (selectedServices.includes("RANGER")) {
      const hdfsPluginConfig = find(data.items, [
        "type",
        "ranger-hdfs-plugin-properties",
      ]);
      if (hdfsPluginConfig) {
        if (
          has(
            hdfsPluginConfig.properties,
            "xasecure.audit.destination.hdfs.dir"
          )
        ) {
          siteNames.push("ranger-hdfs-plugin-properties");
        }
      }
      const hdfsAuditConfig = find(data.items, ["type", "ranger-hdfs-audit"]);
      if (hdfsAuditConfig) {
        if (
          has(hdfsAuditConfig.properties, "xasecure.audit.destination.hdfs.dir")
        ) {
          siteNames.push("ranger-hdfs-audit");
        }
      }
    }
    return siteNames;
  }


  let operations = [
    {
      id: 1,
      label: "Stop all services",
      skippable: false,
      callback: async () => {
        const data: any = {
          ServiceInfo: {
            state: "INSTALLED",
          },
        };
        data.context = "Stop all services";
        const stopServicesPayload = {
          RequestInfo: {
            context: "Stop all services",
            operation_level: {
              level: "CLUSTER",
              cluster_name: clusterName,
            },
          },
          Body: {
            ServiceInfo: {
              state: "INSTALLED",
            },
          },
        };
        const requestData = await RequestApi.stopServices(
          clusterName,
          stopServicesPayload
        );
        return requestData;
      },
    },
    {
      id: 2,
      label: "Install NameNode",
      skippable: false,
      callback: async () => {
        const hostName = find(
          filter(masterComponentHosts, ["component", "NAMENODE"]),
          ["isInstalled", false]
        ).hostName;
        return await createInstallComponentTask(
          "NAMENODE",
          hostName,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels["hdfs"],
          getKDCSessionState
        );
      },
    },
    {
      id: 3,
      label: "Install JournalNodes",
      skippable: false,
      callback: async () => {
        const hostName = map(
          filter(masterComponentHosts, ["component", "JOURNALNODE"]),
          "hostName"
        );
        return await createInstallComponentTask(
          "JOURNALNODE",
          hostName,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels["hdfs"],
          getKDCSessionState
        );
      },
    },
    {
      id: 4,
      label: "Reconfigure HDFS",
      skippable: false,
      callback: async () => {
        const reviewedConfigs = getStepData(
          state,
          enableNamenodeSteps.REVIEW,
          "overridenProperties",
          "enableHighAvailibilitySteps"
        );
        if (isKerberosEnabled) {
          const currentConfigs = await loadCurrentSecureConfigs(reviewedConfigs);
          return await updateConfigProperties(currentConfigs);
        }
        return await updateConfigProperties(reviewedConfigs);
      },
    },
    ...(isKerberosEnabled
      ? [
          {
            id: 5,
            label: "Prepare Operations",
            skippable: false,
            callback: async () => {
              const payload = {
                Clusters: {
                  security_type: "KERBEROS",
                },
              };
              try {
                const params = "regenerate_keytabs=all";
                return await RequestApi.regenerateKeytabs(
                  clusterName,
                  payload,
                  params
                );
              } catch (error) {
                console.log("Error regenerating keytabs: ", error);
                throw error;
              }
            },
          },
        ]
      : []),
    {
      id: isKerberosEnabled ? 6 : 5,
      label: "Start JournalNodes",
      skippable: false,
      callback: async () => {
        const hostNames: string[] = map(
          filter(masterComponentHosts, ["component", "JOURNALNODE"]),
          "hostName"
        );
        return await updateComponent(
          clusterName,
          "JOURNALNODE",
          hostNames as any,
          "HDFS",
          "Start",
          2
        );
      },
    },
    {
      id: isKerberosEnabled ? 7 : 6,
      label: "Disable SNameNode",
      skippable: false,
      callback: async () => {
        const hostName: string[] = find(masterComponentHosts, [
          "component",
          "SECONDARY_NAMENODE",
        ]).hostName;
        return await HostsApi.updateHostComponentPassiveState(
          clusterName,
          hostName as any,
          "SECONDARY_NAMENODE",
          {
            passive_state: "ON",
          }
        );
      },
    },
  ];

  const savedOperationsState = getStepData(
    state,
    enableNamenodeSteps.CONFIGURE_COMPONENTS,
    "operationsState",
    "enableHighAvailibilitySteps"
  );

  useEffect(() => {
    setStepOperations(mergeSavedOperations(operations, savedOperationsState));
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
          flushStateToDb("back");
          jumpToStep(4);
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        cancelConfirmationBody="NameNode HA changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Complete the documented manual recovery before making further HDFS topology changes."
        showCancel={showCancel}
      />
    </>
  );
}

export default Step5;
