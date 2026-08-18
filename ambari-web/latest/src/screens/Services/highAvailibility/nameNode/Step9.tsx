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
import { find, get, map } from "lodash";
import { t } from "i18next";
import { EnableHighAvailibilityContext } from "./store/context";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { getStepData } from "../../../../Utils/Utility";
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
import ClusterApi from "../../../../api/clusterApi";
import { HostsApi } from "../../../../api/hostsApi";
import { ActionTypes } from "./store/types";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";
import {
  getRangerReconfigureSiteGroups,
  mergeSavedOperations,
} from "../haWorkflowUtils";
import modalManager from "../../../../store/ModalManager";
import { Alert } from "react-bootstrap";

function Step9() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services } = useContext(AppContext);
  const { serviceModels, masterSlaveClientsData }: any =
    useContext(ServiceContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const [completionStatus, setCompletionStatus] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [completionError, setCompletionError] = useState("");
  const masterComponentHosts =
    getStepData(
      state,
      enableNamenodeSteps.SELECT_HOSTS,
      "masterComponentHosts",
      "enableHighAvailibilitySteps",
    ) || [];
  const configData = getStepData(
    state,
    enableNamenodeSteps.REVIEW,
    "overridenProperties",
    "enableHighAvailibilitySteps",
  );
  const selectedServices = map(services, "ServiceInfo.service_name");
  const note = t("admin.highAvailability.step9.save.configuration.note");

  const additionalNameNodeHost = find(
    masterComponentHosts,
    (hostComponent: any) =>
      hostComponent.component === "NAMENODE" && !hostComponent.isInstalled,
  )?.hostName;

  const saveReconfiguredConfigs = async (siteNames: string[]) => {
    if (!configData?.items) {
      throw new Error("The reviewed HA configuration snapshot is missing.");
    }
    const missingSites = siteNames.filter(
      (siteName) => !find(configData.items, ["type", siteName]),
    );
    if (missingSites.length) {
      throw new Error(
        `The reviewed HA configuration is missing: ${missingSites.join(", ")}.`,
      );
    }
    return await ClusterApi.updateCluster(clusterName, {
      Clusters: {
        desired_config: reconfigureSites(siteNames, configData, note),
      },
    });
  };

  const initializeTasks = () => {
    const operations: any[] = [];
    const nameNodeHosts = masterComponentHosts
      .filter((hostComponent: any) => hostComponent.component === "NAMENODE")
      .map((hostComponent: any) => hostComponent.hostName);

    operations.push({
      id: 0,
      label: "Start Additional NameNode",
      skippable: false,
      callback: async () =>
        await updateComponent(
          clusterName,
          "NAMENODE",
          additionalNameNodeHost,
          "HDFS",
          "Start",
          1,
        ),
    });
    operations.push({
      id: 1,
      label: "Install ZKFC",
      skippable: false,
      callback: async () =>
        await createInstallComponentTask(
          "ZKFC",
          nameNodeHosts,
          "HDFS",
          clusterName,
          ["HDFS"],
          serviceModels.hdfs,
          getKDCSessionState,
        ),
    });
    operations.push({
      id: 2,
      label: "Start ZKFC",
      skippable: false,
      callback: async () =>
        await updateComponent(
          clusterName,
          "ZKFC",
          nameNodeHosts,
          "HDFS",
          "Start",
          1,
        ),
    });

    const pxfComponent = find(
      Object.values(masterSlaveClientsData || {}),
      ["ServiceComponentInfo.component_name", "PXF"],
    );
    const pxfHosts: string[] = map(
      get(pxfComponent, "host_components", []),
      (hostComponent: any) => get(hostComponent, "HostRoles.host_name"),
    ).filter(Boolean);
    if (
      selectedServices.includes("PXF") &&
      additionalNameNodeHost &&
      !pxfHosts.includes(additionalNameNodeHost)
    ) {
      operations.push({
        id: 3,
        label: "Install PXF",
        skippable: false,
        callback: async () =>
          await createInstallComponentTask(
            "PXF",
            additionalNameNodeHost,
            "PXF",
            clusterName,
            selectedServices,
            serviceModels.pxf,
            getKDCSessionState,
          ),
      });
    }

    if (selectedServices.includes("RANGER")) {
      operations.push({
        id: 4,
        label: "Reconfigure Ranger",
        skippable: false,
        callback: async () => {
          const groups = getRangerReconfigureSiteGroups(
            selectedServices,
            configData?.items || [],
          );
          const configs = groups.map((siteNames) => ({
            Clusters: {
              desired_config: reconfigureSites(siteNames, configData, note),
            },
          }));
          return await ClusterApi.updateCluster(clusterName, configs);
        },
      });
    }

    if (selectedServices.includes("HBASE")) {
      operations.push({
        id: 5,
        label: "Reconfigure HBase",
        skippable: false,
        callback: async () => {
          const hbaseSites = ["hbase-site"];
          if (selectedServices.includes("RANGER")) {
            ["ranger-hbase-plugin-properties", "ranger-hbase-audit"].forEach(
              (siteName) => {
                const site = find(configData?.items, ["type", siteName]);
                if (
                  site?.properties &&
                  Object.prototype.hasOwnProperty.call(
                    site.properties,
                    "xasecure.audit.destination.hdfs.dir",
                  )
                ) {
                  hbaseSites.push(siteName);
                }
              },
            );
          }
          return await saveReconfiguredConfigs(hbaseSites);
        },
      });
    }
    if (selectedServices.includes("AMBARI_METRICS")) {
      operations.push({
        id: 6,
        label: "Reconfigure AMS for NameNode HA",
        skippable: false,
        callback: async () =>
          await saveReconfiguredConfigs(["ams-hbase-site"]),
      });
    }
    if (selectedServices.includes("ACCUMULO")) {
      operations.push({
        id: 7,
        label: "Reconfigure Accumulo",
        skippable: false,
        callback: async () => await saveReconfiguredConfigs(["accumulo-site"]),
      });
    }
    if (selectedServices.includes("HAWQ")) {
      operations.push({
        id: 8,
        label: "Reconfigure HAWQ",
        skippable: false,
        callback: async () =>
          await saveReconfiguredConfigs(["hawq-site", "hdfs-client"]),
      });
    }

    const secondaryNameNodeHost = find(
      masterComponentHosts,
      (hostComponent: any) =>
        hostComponent.component === "SECONDARY_NAMENODE" &&
        hostComponent.isInstalled,
    )?.hostName;
    operations.push({
      id: 9,
      label: "Delete Secondary NameNode",
      skippable: false,
      callback: async () =>
        await HostsApi.deleteHostComponent(
          clusterName,
          secondaryNameNodeHost,
          "SECONDARY_NAMENODE",
        ),
    });
    operations.push({
      id: 10,
      label: "Stop HDFS",
      skippable: false,
      callback: async () =>
        await stopServices(
          clusterName,
          ["HDFS"],
          true,
          false,
          selectedServices,
        ),
    });
    operations.push({
      id: 11,
      label: "Start All Services",
      skippable: false,
      callback: async () => await startServices(clusterName, false, [], false),
    });
    return operations;
  };

  const savedOperationsState = getStepData(
    state,
    enableNamenodeSteps.FINALIZE,
    "operationsState",
    "enableHighAvailibilitySteps",
  );
  const [stepOperations] = useState(() =>
    mergeSavedOperations(initializeTasks(), savedOperationsState),
  );

  const completeWizard = async () => {
    setIsCompleting(true);
    setCompletionError("");
    try {
      await flushStateToDb("complete");
      window.location.href = "#/main/services/HDFS/summary";
      window.location.reload();
    } catch (error: any) {
      setCompletionError(
        error?.response?.data?.message ||
          error?.message ||
          "Ambari could not clear the NameNode HA workflow checkpoint.",
      );
      setIsCompleting(false);
    }
  };

  const handleComplete = () => {
    if (!selectedServices.includes("HAWQ")) {
      void completeWizard();
      return;
    }
    modalManager.show({
      modalTitle: t(
        "admin.highAvailability.wizard.step9.hawq.confirmPopup.header",
      ),
      modalBody: t(
        "admin.highAvailability.wizard.step9.hawq.confirmPopup.body",
      ),
      onClose: () => {},
      successCallback: () => {
        modalManager.hide();
        void completeWizard();
      },
      options: {
        buttonSize: "sm",
        cancelableViaIcon: true,
        cancelableViaBtn: false,
        okButtonVariant: "primary",
      },
    });
  };

  return (
    <>
      {completionError ? <Alert variant="danger">{completionError}</Alert> : null}
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
              data: { operationsState },
            },
          });
          await flushStateToDb();
        }}
      />
      <WizardFooter
        step={currentStep}
        isNextEnabled={completionStatus && !isCompleting}
        onNext={handleComplete}
        onBack={() => {}}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
        cancelConfirmationBody="NameNode HA changes have started. Exiting preserves the workflow checkpoint so the operation can be resumed. Complete the documented manual recovery before making further HDFS topology changes."
      />
    </>
  );
}

export default Step9;
