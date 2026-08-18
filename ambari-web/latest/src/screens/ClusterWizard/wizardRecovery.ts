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

export type InstallationWizard = "clusterCreation" | "addHost" | "addService";

const RECOVERY_STEPS: Record<InstallationWizard, Record<string, number>> = {
  clusterCreation: {
    CLUSTER_DEPLOY_PREP_2: 8,
    CLUSTER_INSTALLING_3: 9,
    SERVICE_STARTING_3: 9,
    CLUSTER_INSTALLED_4: 10,
  },
  addHost: {
    ADD_HOSTS_DEPLOY_PREP_2: 5,
    ADD_HOSTS_INSTALLING_3: 6,
    SERVICE_STARTING_3: 6,
    ADD_HOSTS_INSTALLED_4: 7,
  },
  addService: {
    ADD_SERVICES_DEPLOY_PREP_2: 5,
    ADD_SERVICES_INSTALLING_3: 6,
    SERVICE_STARTING_3: 6,
    ADD_SERVICES_INSTALLED_4: 7,
  },
};

export function resolveRecoveryStep(
  wizard: InstallationWizard,
  clusterState: unknown,
): number | undefined {
  return typeof clusterState === "string"
    ? RECOVERY_STEPS[wizard][clusterState]
    : undefined;
}
