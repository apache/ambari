/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type {
  ManagedDependencyBindingPhase,
} from "../api/serviceDependenciesApi";

const dependencyPhaseTranslationKeys = {
  PREVIEWED: "serviceDependencies.phase.review",
  PROVIDER_PREPARING: "serviceDependencies.phase.prepareProvider",
  PROVIDER_PREPARED: "serviceDependencies.phase.installClients",
  ZOOKEEPER_HANDOFF_RECONCILING: "serviceDependencies.phase.confirmZookeeper",
  CONSUMER_VERIFYING: "serviceDependencies.phase.checkConnections",
  READY: "serviceDependencies.phase.ready",
  STALE: "serviceDependencies.phase.stale",
  FAILED: "serviceDependencies.phase.failed",
  DETACHING: "serviceDependencies.phase.detaching",
  FENCING_UNCERTAIN: "serviceDependencies.phase.checkExistingOperation",
  RETIRED: "serviceDependencies.phase.retired",
  DETACHED: "serviceDependencies.phase.detached",
  TOMBSTONED: "serviceDependencies.phase.removed",
} satisfies Record<ManagedDependencyBindingPhase, string>;

export function dependencyPhaseTranslationKey(phase?: string) {
  const normalized = String(phase || "").toUpperCase() as ManagedDependencyBindingPhase;
  return dependencyPhaseTranslationKeys[normalized]
    || "serviceDependencies.phase.unavailable";
}

const dependentStateTranslationKeys: Record<string, string> = {
  FAILED: "serviceDependencies.phase.failed",
  FENCING_UNCERTAIN: "serviceDependencies.phase.checkExistingOperation",
  PROVISIONING: "serviceDependents.state.inProgress",
  READY: "serviceDependencies.phase.ready",
  RETIRED: "serviceDependencies.phase.retired",
  STALE: "serviceDependencies.phase.stale",
};

export function dependentStateTranslationKey(state?: string) {
  return dependentStateTranslationKeys[String(state || "").toUpperCase()]
    || "serviceDependencies.phase.unavailable";
}
