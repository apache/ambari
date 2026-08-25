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

export const ServiceActionEnums = {
  startedServiceState: "STARTED",
  startAction: "Start",
  stopAction: "Stop",
  stoppedServiceState: "INSTALLED",
  clusterLevel: "CLUSTER",
  restartAllAction: "Restart All",
  turnOnMaintenanceMode: "Turn on Maintenance Mode",
  turnOffMaintenanceMode: "Turn off Maintenance Mode",
  restartDataNodeAction: "Restart DataNodes",
  restartRangerTagSyncsAction: "Restart Ranger TagSyncs",
  restartJournalNodeAction: "Restart JournalNodes",
  enableHighAvailibility: "Enable Namenode HA",
  enableRmHighAvailability:"Enable ResourceManager HA",
  enableNamenodeFederation: "Add New HDFS Namespace",
  addDfsRouter: "Add DFSRouter",
  addHawqStandby: "Add HAWQ Standby",
  removeHawqStandby: "Remove HAWQ Standby",
  activateHawqStandby: "Activate HAWQ Standby",
  rebalanceHDFS: "Rebalance HDFS",
  enableRangerHighAvailibility: "Enable Ranger Admin HA",
  runServiceCheck: "Run Service Check",
  serviceLevel: "SERVICE",
  manageJournalNodes:"Manage JournalNodes",
  restartYarnCapacityScheduler: "Restart YC",
  executeRefreshNodes: "Execute REFRESH_NODES",
  refreshYarnCapacityScheduler: "Refresh YARN Capacity Scheduler",
  refreshNodes: "Refresh Nodes",
  deleteServiceAction: "Delete Service",
  restartNodeManagerAction: "Restart NodeManagers",
  restartZooKeeperServerAction: "Restart ZooKeeper Servers",
  refreshConfigs: "Refresh Configs",
  restartZKFC: "Restart ZKFailoverControllers",
};

/**
 * Gets or creates a service action enum value for restarting a component
 * 
 * @param componentDisplayName - The display name of the component
 * @returns The action enum value
 */
export const getRestartActionEnum = (componentDisplayName: string): string => {
  const actionKey = `restart${componentDisplayName.replace(/\s+/g, '')}Action`;
  
  // If the action already exists in ServiceActionEnums, return it
  if (ServiceActionEnums[actionKey as keyof typeof ServiceActionEnums]) {
    return ServiceActionEnums[actionKey as keyof typeof ServiceActionEnums];
  }
  
  // Otherwise, create a new action name
  return `Restart ${componentDisplayName}`;
};
