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

import { useCallback, useContext, useEffect, useRef, useState } from "react";
import VersionsApi from "../api/versionsApi";
import usePolling from "./usePolling";
import toast from "react-hot-toast";
import { StackVersion, UpgradeData, UpgradeGroup, UpgradeItem, UpgradeParameters } from "../screens/ClusterAdmin/StackAndVersions/types";
import { get, merge, set } from "lodash";
import { failedStatuses, activeStatuses, getUpgradeRequestStatus } from "../Utils/Utility";
import { AppContext } from "../store/context";
import ClusterApi from "../api/clusterApi";
import {
  isTerminalUpgradeStatus,
  serviceCheckFailureSummary,
  skippedServiceCheckNames,
  slaveComponentFailureDetails,
} from "../screens/ClusterAdmin/StackAndVersions/upgradeUtils";
import { persistedPayload } from "../Utils/persistedSettings";

export function useUpgrade(upgradeId: number, onlyView: boolean) {
  const [data, setData] = useState<UpgradeData | null>(null);
  const [groups, setGroups] = useState<UpgradeGroup[]>([]);
  const [currUpgradeItem, setCurrUpgradeItem] = useState<UpgradeItem | null>(null);
  const [currentStack, setCurrentStack] = useState<StackVersion | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoadError, setDetailLoadError] = useState<string | null>(null);
  const [statusUpdateError, setStatusUpdateError] = useState<string | null>(null);
  const [detailLoadAttempt, setDetailLoadAttempt] = useState(0);
  const failureDetailsCache = useRef(new Map<string, any>());
  const completedUpgradeHandled = useRef(false);
  const hasLoadedData = useRef(false);
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
    serviceCheckFailuresServicenames: [],
    skippedServiceChecks: [],
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

  const fetchOperations = useCallback(async () => {
    if (!hasLoadedData.current) {
      setLoading(true);
    }
    try {
      const response = await VersionsApi.getUpgradeOperations(upgradeId, clusterName);
      hasLoadedData.current = true;
      setLoadError(null);
      setData(response);
      return response;
    } catch (error: any) {
      setLoadError(error?.response?.data?.message || error?.message || "Upgrade data could not be loaded");
      throw error;
    } finally {
      setLoading(false);
    }
  }, [clusterName, upgradeId]);

  const { resumePolling, stopPolling } = usePolling(fetchOperations, onlyView ? null : 6000);

  useEffect(() => {
    hasLoadedData.current = false;
    completedUpgradeHandled.current = false;
    failureDetailsCache.current.clear();
  }, [clusterName, upgradeId]);

  useEffect(() => {
    if (onlyView) {
      void fetchOperations().catch(() => undefined);
    }
  }, [fetchOperations, onlyView]);

  const finalizeContext : string = 'Confirm Finalize';
  const slaveFailuresContext : string = "Check Component Versions";
  const serviceCheckFailuresContext: string = "Verifying Skipped Failures";

  useEffect(() => {
    if (!data) return;
    if (onlyView) {
      setGroups(data.upgrade_groups || []);
      return;
    }

    setUpgradeState(data.Upgrade.request_status);
    setGroups((prevGroups) => mergeGroups(prevGroups, data.upgrade_groups || []));

    if (data.Upgrade?.request_status === "COMPLETED") {
      stopPolling();
      if (!completedUpgradeHandled.current) {
        completedUpgradeHandled.current = true;
        setCurrentStackVersion(get(data, "Upgrade.associated_version", ""));
        setUpgradeState("NOT_REQUIRED");
        setUpgradeIsFinalizeItem(false);
        void ClusterApi.postPersistData(persistedPayload({
          upgradeIsFinalizeItem: false,
          "wizard-data": {},
        })).catch(() => {
          toast.error("The completed upgrade state could not be persisted in this browser");
        }).finally(() => window.location.reload());
      }
    } else if (isTerminalUpgradeStatus(data.Upgrade?.request_status)) {
      stopPolling();
    }
  }, [data, onlyView, setCurrentStackVersion, setUpgradeIsFinalizeItem, setUpgradeState, stopPolling]);

  useEffect(() => {
    let active = true;
    const fetchData = async () => {
      const getUpgradeItem = async (item: UpgradeItem) => {
        const groupId = item?.UpgradeItem.group_id;
        const stageId = item?.UpgradeItem.stage_id;
        const key = `item-${groupId}-${stageId}`;
        if (!failureDetailsCache.current.has(key)) {
          failureDetailsCache.current.set(
            key,
            await VersionsApi.getUpgradeItem(upgradeId, groupId, stageId, clusterName),
          );
        }
        return failureDetailsCache.current.get(key);
      }

      const getSkippedServiceChecks = async () => {
        const key = `service-checks-${upgradeId}`;
        if (!failureDetailsCache.current.has(key)) {
          failureDetailsCache.current.set(
            key,
            await VersionsApi.getFailedServiceChecks(clusterName, upgradeId),
          );
        }
        return failureDetailsCache.current.get(key);
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
            ...failedStatuses,
          ].includes(item.UpgradeItem.status)
        );
      if (!active) return;
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
      const showPauseButton = !upgradeSuspended && upgradeRunning;
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
      let slaveComponentStructuredInfo = { hosts: [] as string[], host_detail: {} };
      let serviceCheckFailuresServicenames: string[] = [];
      let skippedServiceChecks: string[] = [];

      setDetailLoadError(null);
      if(isSlaveComponentFailuresItem && slaveItem) {
        try {
          slaveComponentStructuredInfo = slaveComponentFailureDetails(await getUpgradeItem(slaveItem));
          areSlaveComponentFailuresHostsLoaded = true;
        } catch (error: any) {
          const message = error?.response?.data?.message || error?.message || "Failed component details could not be loaded";
          if (active) {
            setDetailLoadError(message);
            toast.error(message);
          }
        }
      }

      let areServiceCheckFailuresServicenamesLoaded = false;
      if(isServiceCheckFailuresItem && manualItem) {
        try {
          const summary = serviceCheckFailureSummary(await getUpgradeItem(manualItem));
          serviceCheckFailuresServicenames = summary.serviceNames;
          slaveComponentStructuredInfo = summary.hostDetails;
          areServiceCheckFailuresServicenamesLoaded = true;
        } catch (error: any) {
          const message = error?.response?.data?.message || error?.message || "Service check failure details could not be loaded";
          if (active) {
            setDetailLoadError(message);
            toast.error(message);
          }
        }
      }
      if (isFinalizeItem) {
        try {
          skippedServiceChecks = skippedServiceCheckNames(await getSkippedServiceChecks());
        } catch (error: any) {
          const message = error?.response?.data?.message || error?.message || "Skipped service checks could not be loaded";
          if (active) {
            setDetailLoadError(message);
            toast.error(message);
          }
        }
      }

      const serviceCheckFailures = get(data, "Upgrade.skip_service_check_failures", false);
      const slaveComponentFailures = get(data, "Upgrade.skip_failures", false);
      const upgradeMethod = get(data, "Upgrade.upgrade_type", "");

      if (!active) return;
      if (!onlyView) {
        setUpgradeIsFinalizeItem(isFinalizeItem);
        await ClusterApi.postPersistData(
          persistedPayload({ upgradeIsFinalizeItem: isFinalizeItem }),
        ).catch(() => {
          toast.error("The current upgrade step could not be persisted in this browser");
        });
      }

      setUpgradeParameters((previous) => ({
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
        requestInProgress: previous.requestInProgress,
        areSlaveComponentFailuresHostsLoaded,
        slaveComponentStructuredInfo,
        serviceCheckFailuresServicenames,
        skippedServiceChecks,
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
      }));
    }
    void fetchData();
    return () => {
      active = false;
    };
  }, [clusterName, data, detailLoadAttempt, groups, onlyView, setUpgradeIsFinalizeItem, upgradeId]);

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
    return tasks;
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
    return logs;
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
    if (upgradeParameters.requestInProgress) {
      return;
    }
    setUpgradeParameters((prev) => ({ ...prev, requestInProgress: true }));
    setStatusUpdateError(null);
    const reqData = {
        upgradeId: upgradeId,
        itemId: item.UpgradeItem.stage_id,
        groupId: item.UpgradeItem.group_id,
        status: status
    };

    try {
        await VersionsApi.setUpgradeItemState(clusterName, reqData);
        resumePolling();
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
    } catch (error: any) {
        const message = error?.response?.data?.message || error?.message || "Failed to update upgrade item status";
        setStatusUpdateError(message);
        toast.error(message);
    } finally {
        setUpgradeParameters((prev) => ({ ...prev, requestInProgress: false }));
    }
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
    setUpgradeItemStatus,
    loadError,
    loading,
    detailLoadError,
    statusUpdateError,
    resumePolling,
    retryFetch: fetchOperations,
    retryFailureDetails: () => {
      failureDetailsCache.current.clear();
      setDetailLoadAttempt((attempt) => attempt + 1);
    },
  };
}
