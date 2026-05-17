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
import VersionsApi from "../api/versionsApi";
import usePolling from "./usePolling";
import toast from "react-hot-toast";
import { StackVersion, UpgradeData, UpgradeGroup, UpgradeItem, UpgradeParameters } from "../screens/ClusterAdmin/StackAndVersions/types";
import { get, merge, set } from "lodash";
import { failedStatuses, activeStatuses, getUpgradeRequestStatus } from "../Utils/Utility";
import { AppContext } from "../store/context";
import ClusterApi from "../api/clusterApi";

export function useUpgrade(upgradeId: number, onlyView: boolean) {
  const [data, setData] = useState<UpgradeData | null>(null);
  const [groups, setGroups] = useState<UpgradeGroup[]>([]);
  const [currUpgradeItem, setCurrUpgradeItem] = useState<UpgradeItem | null>(null);
  const [currentStack, setCurrentStack] = useState<StackVersion | null>(null);
  const [upgradeParameters, setUpgradeParameters] = useState<UpgradeParameters>({
    isDowngrade: false,
    downgradeAllowed: true,
    isDowngradeAvailable: false,
    overallProgress: 0,
    activeGroup: null,
    runningItem: null,
    failedItem: null,
    manualItem: null,
    plainManualItem: false,
    isSlaveComponentFailuresItem: false,
    isServiceCheckFailuresItem: false,
    isFinalizeItem: false,
    canSkipFailedItem: true,
    isHoldingState: true,
    requestInProgress: false,
    areSlaveComponentFailuresHostsLoaded: false,
    slaveComponentStructuredInfo: "",
    areServiceCheckFailuresServicenamesLoaded: false,
    serviceCheckFailuresServicenames: "",
    upgradeStatus: "NOT_REQUIRED",
    upgradeInit: false,
    upgradeInProgress: false,
    upgradeCompleted: false,
    upgradeSuspended: false,
    upgradeAborted: false,
    upgradeHolding: false,
    upgradeRunning: false,
    showPauseButton: false,
    upgradeStatusLabel: "",
    upgradeAssociatedversion: "",
    slaveComponentFailures: false,
    serviceCheckFailures: false,
    upgradeMethod: "",
  });
  const { clusterName, setUpgradeState, setCurrentStackVersion, setUpgradeIsFinalizeItem } = useContext(AppContext);

  const { pausePolling } = usePolling(fetchOperations, 6000);
  let slaveComponentStructuredInfo: any;
  let serviceCheckFailuresServicenames: any;

  async function fetchOperations() {
    try {
      const response = await VersionsApi.getUpgradeOperations(upgradeId, clusterName);
      setData(response);
    } catch (error) {
      toast.error("Failed to fetch data");
    }
  }

  useEffect(() => {
    fetchOperations();
    if(onlyView) pausePolling();
  }, [])

  const finalizeContext : string = 'Confirm Finalize';
  const slaveFailuresContext : string = "Check Component Versions";
  const serviceCheckFailuresContext: string = "Verifying Skipped Failures";

  useEffect(() => {
    if (data) {
      if(onlyView) {
        setGroups(data.upgrade_groups);
        return;
      }
      
      setUpgradeState(data.Upgrade.request_status);
      setGroups((prevGroups) => mergeGroups(prevGroups, data.upgrade_groups));

      if(data.Upgrade?.request_status === 'COMPLETED') {
        finish();
      }
    }
  }, [data]);

  useEffect(() => {
    const fetchData = async () => {
      const getUpgradeItem = async (item: UpgradeItem) => {
        const groupId = item?.UpgradeItem.group_id;
        const stageId = item?.UpgradeItem.stage_id;
        const response = await VersionsApi.getUpgradeItem(upgradeId, groupId, stageId, clusterName);
        const info = response.tasks[0];
        if(info && info.Tasks && info.Tasks?.structured_out) {
          slaveComponentStructuredInfo = info.Tasks.structured_out;
        }
        return true;
      }

      const getServiceCheckItem = async (item: UpgradeItem) => {
        const groupId = item?.UpgradeItem.group_id;
        const stageId = item?.UpgradeItem.stage_id;
        const response = await VersionsApi.getUpgradeItem(upgradeId, groupId, stageId, clusterName);
        const task = response.tasks[0];
        let info = {
          hosts: [] as string[],
          host_detail: {}
        }

        if (task && task.Tasks && task.Tasks?.structured_out && task.Tasks?.structured_out?.failures) {
          set(serviceCheckFailuresServicenames, task.Tasks?.structured_out.failures?.service_check, [])
          if (task.Tasks.structured_out.failures.host_component) {
            task.Tasks.structured_out.failures.host_component.forEach((hostName: string) => {
              info.hosts.push(hostName);
            })
            info.host_detail = task.Tasks.structured_out.failures.host_component;
          }
        slaveComponentStructuredInfo = info;
        }
        return true;
      }

      const currItem = groups
        .map((group) => group.upgrade_items)
        .flat()
        .find((item) =>
          [
            "IN_PROGRESS",
            "HOLDING_FAILED",
            "HOLDING_PENDING",
            "HOLDING_TIMEDOUT",
            "HOLDING",
          ].includes(item.UpgradeItem.status)
        );
      setCurrUpgradeItem(currItem || null);
      
      const upgradeAssociatedversion = get(data, "Upgrade.associated_version", "");
      const upgradeStatus = get(data, "Upgrade.request_status", "NOT_REQUIRED");
      const upgradeInit = (upgradeStatus === "NOT_REQUIRED");
      const upgradeInProgress = (upgradeStatus === "IN_PROGRESS");
      const upgradeCompleted = (upgradeStatus === "COMPLETED");
      const isSuspended = get(data, "Upgrade.suspended") || false;
      const upgradeSuspended = (upgradeStatus === "ABORTED") && isSuspended;
      const upgradeAborted = (upgradeStatus === "ABORTED") && !isSuspended;
      const upgradeHolding = (upgradeStatus.includes("HOLDING") || upgradeAborted);
      const upgradeRunning = (upgradeInProgress || upgradeHolding);
      const showPauseButton = (!upgradeSuspended && !upgradeCompleted && !upgradeInit);
      const isDowngrade = get(data, "Upgrade.direction", "") === "DOWNGRADE";
      const isDowngradeAvailable = get(data, "Upgrade.downgrade_allowed", false);

      const upgradeStatusLabel = getUpgradeRequestStatus(data?.Upgrade?.request_status || "", isDowngrade);
      const activeGroup = groups.find((group) => activeStatuses.includes(group.UpgradeGroup.status)) || null;
      const runningItem = activeGroup?.upgrade_items.find((item) => item.UpgradeItem.status === "IN_PROGRESS") || null;
      const failedItem = activeGroup?.upgrade_items.find((item) => failedStatuses.includes(item.UpgradeItem.status)) || null;
      const manualItem = activeGroup?.upgrade_items.find((item) => item.UpgradeItem.status === "HOLDING") || null;
      const plainManualItem = manualItem ? ![finalizeContext, slaveFailuresContext, serviceCheckFailuresContext].includes(manualItem.UpgradeItem.context) : false;
      const slaveItem = activeGroup?.upgrade_items.find((item) => item.UpgradeItem.context === slaveFailuresContext) || null;
      const isSlaveComponentFailuresItem = slaveItem ? ['HOLDING', 'HOLDING_FAILED', 'OUT_OF_SYNC'].includes(slaveItem.UpgradeItem.status) : false;
      const isServiceCheckFailuresItem = manualItem?.UpgradeItem.context === serviceCheckFailuresContext;
      const isFinalizeItem = manualItem?.UpgradeItem.context === finalizeContext;
      const canSkipFailedItem = failedItem ? failedItem.UpgradeItem.skippable : true;
      const isHoldingState = failedItem ? failedItem.UpgradeItem.status.includes("HOLDING") || failedItem.UpgradeItem.status === "ABORTED" : false;
      let areSlaveComponentFailuresHostsLoaded = false;

      if(isSlaveComponentFailuresItem && slaveItem) 
        areSlaveComponentFailuresHostsLoaded = await getUpgradeItem(slaveItem);

      let areServiceCheckFailuresServicenamesLoaded = false;
      if(isServiceCheckFailuresItem && manualItem)
        areServiceCheckFailuresServicenamesLoaded = await getServiceCheckItem(manualItem)

      const serviceCheckFailures = get(data, "Upgrade.skip_service_check_failures", false);
      const slaveComponentFailures = get(data, "Upgrade.skip_failures", false);
      const upgradeMethod = get(data, "Upgrade.upgrade_type", "");

      setUpgradeIsFinalizeItem(isFinalizeItem);
      await ClusterApi.postPersistData(
        JSON.stringify({
          upgradeIsFinalizeItem: JSON.stringify(isFinalizeItem)
        })
      )

      setUpgradeParameters({
        isDowngrade: isDowngrade,
        isDowngradeAvailable: isDowngradeAvailable,
        downgradeAllowed: data?.Upgrade.downgrade_allowed || false,
        overallProgress: Math.floor(data?.Upgrade.progress_percent || 0),
        activeGroup,
        runningItem,
        failedItem,
        manualItem,
        plainManualItem,
        isSlaveComponentFailuresItem,
        isServiceCheckFailuresItem,
        isFinalizeItem,
        canSkipFailedItem,
        isHoldingState,
        requestInProgress: false,
        areSlaveComponentFailuresHostsLoaded,
        slaveComponentStructuredInfo,
        serviceCheckFailuresServicenames,
        areServiceCheckFailuresServicenamesLoaded,
        upgradeStatus: upgradeStatus,
        upgradeInit: upgradeInit,
        upgradeInProgress: upgradeInProgress,
        upgradeCompleted: upgradeCompleted,
        upgradeSuspended: upgradeSuspended,
        upgradeAborted: upgradeAborted,
        upgradeHolding: upgradeHolding,
        upgradeRunning: upgradeRunning,
        showPauseButton: showPauseButton,
        upgradeStatusLabel: upgradeStatusLabel || "",
        upgradeAssociatedversion: upgradeAssociatedversion,
        slaveComponentFailures: slaveComponentFailures,
        serviceCheckFailures: serviceCheckFailures,
        upgradeMethod: upgradeMethod,
      });
    }
    fetchData();
  }, [groups]);

  const mergeGroups = (
    prevGroups: UpgradeGroup[],
    newGroups: UpgradeGroup[]
  ) => {
    const mergedGroups = [...prevGroups];

    newGroups.forEach((newGroup) => {
      const existingGroupIndex = mergedGroups.findIndex(
        (group) =>
          group.UpgradeGroup.group_id === newGroup.UpgradeGroup.group_id
      );

      if (existingGroupIndex !== -1) {
        // Preserve any custom properties (like collapse states) from the existing group
        const existingGroup = mergedGroups[existingGroupIndex];
        mergedGroups[existingGroupIndex] = merge(
          existingGroup,
          newGroup
        );
      } else {
        mergedGroups.push(newGroup);
      }
    });

    return mergedGroups;
  };

  async function updateCurrentStackVersion() {
    const response = await VersionsApi.getServices(clusterName);
    const stacks = response.items;

    const currentStack = stacks.find(
      (stack: StackVersion) => stack.ClusterStackVersions.state === "CURRENT"
    );

    if(currentStack) {
      setCurrentStack(currentStack);
    }
  }

  const fetchTasks = async (groupId: number, stageId: number) => {
    const response = await VersionsApi.getTasksList(upgradeId, groupId, stageId, clusterName);
    const tasks = response.tasks.map((task: any) => ({ ...task.Tasks, logs: null }));
    setGroups((prevGroups) =>
      prevGroups.map((group) => ({
        ...group,
        upgrade_items: group.upgrade_items.map((item) =>
          item.UpgradeItem.stage_id === stageId ? { ...item, tasks } : item
        ),
      }))
    );

    // Also update currUpgradeItem if it matches the current stage
    setCurrUpgradeItem((prevItem) => {
      if (prevItem && prevItem.UpgradeItem.stage_id === stageId) {
        return {
          ...prevItem,
          tasks
        };
      }
      return prevItem;
    });
  };

  const fetchLogs = async (groupId: number, stageId: number, taskId: number) => {
    const logs = await VersionsApi.getTasksLogs(upgradeId, groupId, stageId, taskId, clusterName);
    setGroups((prevGroups) =>
      prevGroups.map((group) => ({
        ...group,
        upgrade_items: group.upgrade_items.map((item) => ({
          ...item,
          tasks: item.tasks?.map((task) =>
            task.id === taskId ? { ...task, logs } : task
          ),
        })),
      }))
    );

    // Also update currUpgradeItem if it matches the current task
    setCurrUpgradeItem((prevItem) => {
      if (prevItem && prevItem.UpgradeItem.stage_id === stageId) {
        return {
          ...prevItem,
          tasks: prevItem.tasks?.map((task) =>
            task.id === taskId ? { ...task, logs } : task
          ),
        };
      }
      return prevItem;
    });
  };

  const handleCopy = async (text: string) => {
    try {
      // Check if clipboard API is available (HTTPS required)
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        alert("Copied to clipboard");
        return;
      }
      
      // Fallback for HTTP or older browsers
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      
      try {
        document.execCommand('copy');
        alert("Copied to clipboard");
      } catch (fallbackErr) {
        console.error("Fallback copy failed: ", fallbackErr);
        // Show user a message to manually copy
        alert(`Please copy this manually: ${text}`);
      }
      
      document.body.removeChild(textArea);
    } catch (err) {
      console.error('Failed to copy:', err);
      // Final fallback - show text for manual copy
      alert(`Please copy this manually: ${text}`);
    }
  };

  const handleOpenInNewTab = (content: string) => {
    const newWindow = window.open("about:blank", "_blank");
    if (newWindow) {
      newWindow.document.write(`<pre>${content}</pre>`);
      newWindow.document.close();
    }
  };

  const setUpgradeItemStatus = async (item: UpgradeItem, status: string) => {
    setUpgradeParameters((prev) => ({ ...prev, requestInProgress: true }));
    const reqData = {
        upgradeId: upgradeId,
        itemId: item.UpgradeItem.stage_id,
        groupId: item.UpgradeItem.group_id,
        status: status
    };

    try {
        await VersionsApi.setUpgradeItemState(clusterName, reqData);
        // set the currItem status to status
        setGroups((prevGroups) =>
          prevGroups.map((group) => ({
            ...group,
            upgrade_items: group.upgrade_items.map((upgradeItem) =>
              upgradeItem.UpgradeItem.stage_id === item.UpgradeItem.stage_id
                ? set(upgradeItem, "UpgradeItem.status", status)
                : upgradeItem
            ),
          }))
        );
    } catch (error) {
        toast.error("Failed to update status");
    } finally {
        setUpgradeParameters((prev) => ({ ...prev, requestInProgress: false }));
    }
  }

  async function finish() {
    const upgradeVersion = get(data, "Upgrade.associated_version", "");
    setCurrentStackVersion(upgradeVersion);
    setUpgradeState("NOT_REQUIRED");
    setUpgradeIsFinalizeItem(false);
    await ClusterApi.postPersistData(
      JSON.stringify({
        upgradeIsFinalizeItem: JSON.stringify(false)
      })
    )
    window.location.reload();
  }

  return {
    data,
    groups,
    currUpgradeItem,
    fetchTasks,
    fetchLogs,
    currentStack,
    updateCurrentStackVersion,
    handleCopy,
    handleOpenInNewTab,
    upgradeParameters,
    setUpgradeItemStatus
  };
}
