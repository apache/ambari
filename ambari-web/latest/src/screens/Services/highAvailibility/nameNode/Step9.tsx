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
import { filter, find, map } from "lodash";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import OperationsProgress from "../../../../components/OperationsProgress";
import {
  createInstallComponentTask,
  reconfigureSites,
  startServices,
  stopServices,
  updateComponent,
} from "../../../../Utils/taskUtils";
import { enableNamenodeSteps } from "./wizardSteps";
import { t } from "i18next";
import ClusterApi from "../../../../api/clusterApi";
import { HostsApi } from "../../../../api/hostsApi";
import { ActionTypes } from "./store/types";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";

function Step9() {
  enum COMMANDS {
    startSecondNameNode = "startSecondNameNode",
    installZKFC = "installZKFC",
    startZKFC = "startZKFC",
    reconfigureRanger = "reconfigureRanger",
    reconfigureHBase = "reconfigureHBase",
    reconfigureAMS = "reconfigureAMS",
    reconfigureAccumulo = "reconfigureAccumulo",
    reconfigureHawq = "reconfigureHawq",
    deleteSNameNode = "deleteSNameNode",
    stopHDFS = "stopHDFS",
    startAllServices = "startAllServices",
  }
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, jumpToStep },
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
  async function saveReconfiguredConfigs(siteNames: any, data: any) {
    const note = t("admin.highAvailability.step9.save.configuration.note");
    const configData = reconfigureSites(siteNames, data, note);
    return await ClusterApi.updateCluster(clusterName, {
      Clusters: {
        desired_config: configData,
      },
    });
  }
  function initializeTasks() {
    let id = 0;
    const allOps = [];
    const tasksToRemove = [];
    if (!selectedServices.includes("RANGER")) {
      tasksToRemove.push(COMMANDS.reconfigureRanger);
    }
    if (!selectedServices.includes("HBASE")) {
      tasksToRemove.push(COMMANDS.reconfigureHBase);
    }
    if (!selectedServices.includes("AMBARI_METRICS")) {
      tasksToRemove.push(COMMANDS.reconfigureAMS);
    }
    if (!selectedServices.includes("ACCUMULO")) {
      tasksToRemove.push(COMMANDS.reconfigureAccumulo);
    }
    if (!selectedServices.includes("HAWQ")) {
      tasksToRemove.push(COMMANDS.reconfigureHawq);
    }

    if (!tasksToRemove.includes(COMMANDS.startSecondNameNode)) {
      allOps.push({
        id: id++,
        label: "Start NameNode",
        skippable: false,
        callback: async () => {
          const hostName = find(masterComponentHosts, (hostComponent: any) => {
            return (
              hostComponent.component === "NAMENODE" &&
              hostComponent.isInstalled === false
            );
          })?.hostName;
          return await updateComponent(
            clusterName,
            "NAMENODE",
            hostName,
            "HDFS",
            "Start",
            1
          );
        },
      });
    }

    if (!tasksToRemove.includes(COMMANDS.installZKFC)) {
      allOps.push({
        id: id++,
        label: "Install ZKFC",
        skippable: false,
        callback: async () => {
          const hostNames = map(
            filter(masterComponentHosts, ["component", "NAMENODE"]),
            "hostName"
          );

          return await createInstallComponentTask(
            "ZKFC",
            hostNames,
            "HDFS",
            clusterName,
            ["HDFS"],
            allServiceModels["hdfs"],
            getKDCSessionState
          );
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.startZKFC)) {
      allOps.push({
        id: id++,
        label: "Start ZKFC",
        skippable: false,
        callback: async () => {
          const hostNames = map(
            filter(masterComponentHosts, ["component", "NAMENODE"]),
            "hostName"
          );

          return await updateComponent(
            clusterName,
            "ZKFC",
            hostNames,
            "HDFS",
            "Start",
            1
          );
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.reconfigureRanger)) {
      const data = getStepData(
        state,
        enableNamenodeSteps.REVIEW,
        "overridenProperties",
        "enableHighAvailibilitySteps"
      );
      let siteNames = ["ranger-env"];
      const configs: any = [];
      configs.push({
        Clusters: {
          desired_config: reconfigureSites(
            siteNames,
            data,
            t("admin.highAvailability.step9.save.configuration.note")
          ),
        },
      });
      if (selectedServices.includes("YARN")) {
        siteNames = [];
        const yarnAuditConfig = find(data.items, ["type", "ranger-yarn-audit"]);
        if (yarnAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in yarnAuditConfig.properties
          ) {
            siteNames.push("ranger-yarn-audit");
            configs.push({
              Clusters: {
                desired_config: reconfigureSites(
                  siteNames,
                  data,
                  t("admin.highAvailability.step9.save.configuration.note")
                ),
              },
            });
          }
        }
      }
      if (selectedServices.includes("STORM")) {
        const stormPluginConfig = find(data.items, [
          "type",
          "ranger-storm-plugin-properties",
        ]);
        if (stormPluginConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in
            stormPluginConfig.properties
          ) {
            siteNames.push("ranger-storm-plugin-properties");
          }
        }
        var stormAuditConfig = find(data.items, ["type", "ranger-storm-audit"]);
        if (stormAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in stormAuditConfig.properties
          ) {
            siteNames.push("ranger-storm-audit");
          }
        }
        if (siteNames.length) {
          configs.push({
            Clusters: {
              desired_config: reconfigureSites(
                siteNames,
                data,
                t("admin.highAvailability.step9.save.configuration.note")
              ),
            },
          });
        }
      }
      if (selectedServices.includes("KAFKA")) {
        siteNames = [];
        const kafkaAuditConfig = find(data.items, [
          "type",
          "ranger-kafka-audit",
        ]);
        if (kafkaAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in kafkaAuditConfig.properties
          ) {
            siteNames.push("ranger-kafka-audit");
            configs.push({
              Clusters: {
                desired_config: reconfigureSites(
                  siteNames,
                  data,
                  t("admin.highAvailability.step9.save.configuration.note")
                ),
              },
            });
          }
        }
      }
      if (selectedServices.includes("KNOX")) {
        siteNames = [];
        let knoxPluginConfig = find(data.items, [
          "type",
          "ranger-knox-plugin-properties",
        ]);
        if (knoxPluginConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in knoxPluginConfig.properties
          ) {
            siteNames.push("ranger-knox-plugin-properties");
          }
        }
        var knoxAuditConfig = data.items.findProperty(
          "type",
          "ranger-knox-audit"
        );
        if (knoxAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in knoxAuditConfig.properties
          ) {
            siteNames.push("ranger-knox-audit");
          }
        }
        if (siteNames.length) {
          configs.push({
            Clusters: {
              desired_config: reconfigureSites(
                siteNames,
                data,
                t("admin.highAvailability.step9.save.configuration.note")
              ),
            },
          });
        }
      }
      if (selectedServices.includes("ATLAS")) {
        siteNames = [];
        const atlasAuditConfig = find(data.items, [
          "type",
          "ranger-atlas-audit",
        ]);
        if (atlasAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in atlasAuditConfig.properties
          ) {
            siteNames.push("ranger-atlas-audit");
            configs.push({
              Clusters: {
                desired_config: reconfigureSites(
                  siteNames,
                  data,
                  t("admin.highAvailability.step4.save.configuration.note")
                ),
              },
            });
          }
        }
      }
      if (selectedServices.includes("HIVE")) {
        siteNames = [];
        var hivePluginConfig = find(data.items, [
          "type",
          "ranger-hive-plugin-properties",
        ]);
        if (hivePluginConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in hivePluginConfig.properties
          ) {
            siteNames.push("ranger-hive-plugin-properties");
          }
        }
        var hiveAuditConfig = find(data.items, ["type", "ranger-hive-audit"]);
        if (hiveAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in hiveAuditConfig.properties
          ) {
            siteNames.push("ranger-hive-audit");
          }
        }
        if (siteNames.length) {
          configs.push({
            Clusters: {
              desired_config: reconfigureSites(
                siteNames,
                data,
                t("admin.highAvailability.step9.save.configuration.note")
              ),
            },
          });
        }
      }
      if (selectedServices.includes("RANGER_KMS")) {
        siteNames = [];
        let rangerKMSConfig = find(data.items, ["type", "ranger-kms-audit"]);
        if (rangerKMSConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in rangerKMSConfig.properties
          ) {
            siteNames.push("ranger-kms-audit");
            configs.push({
              Clusters: {
                desired_config: reconfigureSites(
                  siteNames,
                  data,
                  t("admin.highAvailability.step9.save.configuration.note")
                ),
              },
            });
          }
        }
      }

      allOps.push({
        id: id++,
        label: "Reconfigure Ranger",
        skippable: false,
        callback: async () => {

          const data = getStepData(
            state,
            enableNamenodeSteps.REVIEW,
            "overridenProperties",
            "enableHighAvailibilitySteps"
          );
          var siteNames = ["ranger-env"];
          var configs = [];
          configs.push({
            Clusters: {
              desired_config: reconfigureSites(
                siteNames,
                data,
                t("admin.highAvailability.step9.save.configuration.note")
              ),
            },
          });
          if (selectedServices.includes("YARN")) {
            siteNames = [];
            const yarnAuditConfig = find(data.items, [
              "type",
              "ranger-yarn-audit",
            ]);
            if (yarnAuditConfig) {
              if (
                "xasecure.audit.destination.hdfs.dir" in
                yarnAuditConfig.properties
              ) {
                siteNames.push("ranger-yarn-audit");
                configs.push({
                  Clusters: {
                    desired_config: reconfigureSites(
                      siteNames,
                      data,
                      t("admin.highAvailability.step9.save.configuration.note")
                    ),
                  },
                });
              }
            }
          }
          if (selectedServices.includes("HIVE")) {
            siteNames = [];
            var hivePluginConfig = find(data.items, [
              "type",
              "ranger-hive-plugin-properties",
            ]);
            if (hivePluginConfig) {
              if (
                "xasecure.audit.destination.hdfs.dir" in
                hivePluginConfig.properties
              ) {
                siteNames.push("ranger-hive-plugin-properties");
              }
            }
            var hiveAuditConfig = find(data.items, [
              "type",
              "ranger-hive-audit",
            ]);
            if (hiveAuditConfig) {
              if (
                "xasecure.audit.destination.hdfs.dir" in
                hiveAuditConfig.properties
              ) {
                siteNames.push("ranger-hive-audit");
              }
            }
            if (siteNames.length) {
              configs.push({
                Clusters: {
                  desired_config: reconfigureSites(
                    siteNames,
                    data,
                    t("admin.highAvailability.step9.save.configuration.note")
                  ),
                },
              });
            }
          }
          if (selectedServices.includes("RANGER_KMS")) {
            siteNames = [];
            let rangerKMSConfig = find(data.items, [
              "type",
              "ranger-kms-audit",
            ]);
            if (rangerKMSConfig) {
              if (
                "xasecure.audit.destination.hdfs.dir" in
                rangerKMSConfig.properties
              ) {
                siteNames.push("ranger-kms-audit");
                configs.push({
                  Clusters: {
                    desired_config: reconfigureSites(
                      siteNames,
                      data,
                      t("admin.highAvailability.step9.save.configuration.note")
                    ),
                  },
                });
              }
            }
          }
          return await ClusterApi.updateCluster(clusterName, configs);
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.reconfigureHBase)) {
      const data = getStepData(
        state,
        enableNamenodeSteps.REVIEW,
        "overridenProperties",
        "enableHighAvailibilitySteps"
      );
      const siteNames = ["hbase-site"];
      if (selectedServices.includes("RANGER")) {
        const hbasePluginConfig = find(data.items, [
          "type",
          "ranger-hbase-plugin-properties",
        ]);
        if (hbasePluginConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in
            hbasePluginConfig.properties
          ) {
            siteNames.push("ranger-hbase-plugin-properties");
          }
        }
        const hbaseAuditConfig = find(data.items, [
          "type",
          "ranger-hbase-audit",
        ]);
        if (hbaseAuditConfig) {
          if (
            "xasecure.audit.destination.hdfs.dir" in hbaseAuditConfig.properties
          ) {
            siteNames.push("ranger-hbase-audit");
          }
        }
      }
      const configs: any = [];
      configs.push({
        Clusters: {
          desired_config: reconfigureSites(
            siteNames,
            data,
            t("admin.highAvailability.step9.save.configuration.note")
          ),
        },
      });
      allOps.push({
        id: id++,
        label: "Reconfigure HBase",
        skippable: false,
        callback: async () => {
          return await saveReconfiguredConfigs(siteNames, data);
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.reconfigureAMS)) {
      const data = getStepData(
        state,
        enableNamenodeSteps.REVIEW,
        "overridenProperties",
        "enableHighAvailibilitySteps"
      );
      allOps.push({
        id: id++,
        label: "Reconfigure AMS",
        skippable: false,
        callback: async () => {
          return await saveReconfiguredConfigs(["ams-hbase-site"], data);
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.reconfigureAccumulo)) {
      const data = getStepData(
        state,
        enableNamenodeSteps.REVIEW,
        "overridenProperties",
        "enableHighAvailibilitySteps"
      );
      allOps.push({
        id: id++,
        label: "Reconfigure Accumulo",
        skippable: false,
        callback: async () => {
          return await saveReconfiguredConfigs(["accumulo-site"], data);
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.reconfigureHawq)) {
      const data = getStepData(
        state,
        enableNamenodeSteps.REVIEW,
        "overridenProperties",
        "enableHighAvailibilitySteps"
      );
      allOps.push({
        id: id++,
        label: "Reconfigure Hawq",
        skippable: false,
        callback: async () => {
          return await saveReconfiguredConfigs(["hawq-site"], data);
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.deleteSNameNode)) {
      const hostName = find(masterComponentHosts, (hostComponent: any) => {
        return (
          hostComponent.component === "SECONDARY_NAMENODE" &&
          hostComponent.isInstalled === true
        );
      })?.hostName;
      allOps.push({
        id: id++,
        label: "Delete Secondary NameNode",
        skippable: false,
        callback: async () => {
          return await HostsApi.deleteHostComponent(
            clusterName,
            hostName,
            "SECONDARY_NAMENODE"
          );
        },
      });
    }
    if (!tasksToRemove.includes(COMMANDS.stopHDFS)) {
      allOps.push({
        id: id++,
        label: "Stop HDFS",
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
    if (!tasksToRemove.includes(COMMANDS.startAllServices)) {
      allOps.push({
        id: id++,
        label: "Start All Services",
        skippable: false,
        callback: async () => {
          return await startServices(clusterName, false, [], false);
        },
      });
    }

    return allOps;
  }

  useEffect(() => {
    initializeTasks();
  }, []);



  const savedOperationsState = getStepData(
    state,
    enableNamenodeSteps.FINALIZE,
    "operationsState",
    "enableHighAvailibilitySteps"
  );

  useEffect(() => {
    const initialOperations = initializeTasks();
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
          window.location.href = "#/main/services/HDFS/summary";
          window.location.reload();
        }}
        onBack={() => {
          jumpToStep(8);
          flushStateToDb("back");
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step9;
