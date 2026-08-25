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
import { isAxiosError } from "axios";
import { isEmpty } from "lodash";
import { ChooseServicesApi } from "../../../api/chooseServicesApi";
import { HostsApi } from "../../../api/hostsApi";
import { AppContext } from "../../../store/context";
import { maxToInstall } from "../../Hosts/utils";

type StackComponentMetadata = Parameters<typeof maxToInstall>[0] & {
  component_name?: string;
  is_master?: boolean;
  isMasterWithMultipleInstances?: boolean;
};

interface StackServiceRecord {
  StackServices: { service_name: string };
  components: Array<{ StackServiceComponents: StackComponentMetadata }>;
}

interface StackServicesResponse {
  items: StackServiceRecord[];
}

interface ClusterComponentRecord {
  ServiceComponentInfo: { component_name: string };
  host_components: Array<{
    HostRoles: {
      component_name: string;
      ha_state: string;
      host_name: string;
    };
  }>;
}

const errorMessage = (error: unknown) => {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error instanceof Error
    ? error.message
    : "Ambari could not load stack services and host components.";
};

function useHostComponents(services?: string[], showJournalNode = false) {
  const [isLoading, setIsLoading] = useState(false);
  const { cluster, clusterName } = useContext(AppContext);
  const [serviceComponents, setServiceComponents] = useState<StackServiceRecord[]>([]);
  const [hostComponents, setHostComponents] = useState<ClusterComponentRecord[]>([]);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const servicesKey = (services || []).join(",");
  const servicesProvided = services !== undefined;

  useEffect(() => {
    if (isEmpty(cluster)) return;

    let cancelled = false;
    async function fetchServicesAndComponents() {
      setIsLoading(true);
      setError("");
      setServiceComponents([]);
      setHostComponents([]);
      try {
        const stack = cluster?.stack || cluster?.version?.split("-")[0];
        const version = cluster?.versionNum || cluster?.version?.split("-")[1];
        const requestedServices = servicesProvided
          ? servicesKey.split(",").filter(Boolean)
          : undefined;
        const servicesAndComponents =
          (await ChooseServicesApi.getServices(
            stack,
            version,
            requestedServices,
          )) as StackServicesResponse;
        const fields = `ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,,host_components/metrics/jvm/memHeapUsedM,host_components/metrics/jvm/HeapMemoryMax,host_components/metrics/jvm/HeapMemoryUsed,host_components/metrics/jvm/memHeapCommittedM,host_components/metrics/mapred/jobtracker/trackers_decommissioned,host_components/metrics/cpu/cpu_wio,host_components/metrics/rpc/client/RpcQueueTime_avg_time,host_components/metrics/dfs/FSNamesystem/*,host_components/metrics/dfs/namenode/Version,host_components/metrics/dfs/namenode/LiveNodes,host_components/metrics/dfs/namenode/DeadNodes,host_components/metrics/dfs/namenode/DecomNodes,host_components/metrics/dfs/namenode/TotalFiles,host_components/metrics/dfs/namenode/UpgradeFinalized,host_components/metrics/dfs/namenode/Safemode,host_components/metrics/runtime/StartTime,host_components/metrics/hbase/master/IsActiveMaster,host_components/metrics/hbase/master/MasterStartTime,host_components/metrics/hbase/master/MasterActiveTime,host_components/metrics/hbase/master/AverageLoad,host_components/metrics/master/AssignmentManager/ritCount,host_components/metrics/dfs/namenode/ClusterId,host_components/metrics/yarn/Queue,host_components/metrics/yarn/ClusterMetrics/NumActiveNMs,host_components/metrics/yarn/ClusterMetrics/NumLostNMs,host_components/metrics/yarn/ClusterMetrics/NumUnhealthyNMs,host_components/metrics/yarn/ClusterMetrics/NumRebootedNMs,host_components/metrics/yarn/ClusterMetrics/NumDecommissionedNMs&ServiceComponentInfo/service_name.in(${servicesKey})&minimal_response=true`;
        const hostComponentsData =
          await HostsApi.getMasterSlaveClusterComponentsByComponentName(
            clusterName,
            showJournalNode
              ? ["APP_TIMELINE_SERVER", "JOURNALNODE"]
              : ["APP_TIMELINE_SERVER"],
            fields,
          );

        (servicesAndComponents?.items || []).forEach((serviceComponentObj) => {
            const allComponents = serviceComponentObj.components || [];
            allComponents.forEach((comp) => {
              const serviceComponent = comp.StackServiceComponents;
              const isMultipleAllowed =
                maxToInstall(serviceComponent) > 1;
              serviceComponent.isMasterWithMultipleInstances =
                (serviceComponent.is_master && isMultipleAllowed) ||
                serviceComponent.component_name === "JOURNALNODE";
            });
          });
        if (!cancelled) {
          setHostComponents(
            (hostComponentsData?.items || []) as ClusterComponentRecord[],
          );
          setServiceComponents(servicesAndComponents?.items || []);
        }
      } catch (caught) {
        if (!cancelled) setError(errorMessage(caught));
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    void fetchServicesAndComponents();
    return () => {
      cancelled = true;
    };
  }, [
    cluster,
    clusterName,
    retryCount,
    servicesKey,
    servicesProvided,
    showJournalNode,
  ]);

  return {
    hostComponents,
    serviceComponents,
    isLoading,
    error,
    retry: () => setRetryCount((value) => value + 1),
  };
}

export default useHostComponents;
