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
  ManagedDependencyCandidate,
  ManagedDependencyIssue,
  ManagedDependencyPreview,
  ManagedDependencyType,
} from "../../api/serviceDependenciesApi";

export type ManagedDependencyChoice = {
  mode: "local" | "managed";
  planningIssue?: ManagedDependencyIssue;
  preview?: ManagedDependencyPreview;
  provider?: ManagedDependencyCandidate;
};

export type ManagedDependencySelections = Partial<
  Record<ManagedDependencyType, ManagedDependencyChoice>
>;

export const dependencyTypes: ManagedDependencyType[] = ["HDFS", "ZOOKEEPER"];

const incompletePlanningCodes = new Set([
  "DEPENDENCY_DRAFT_VERSION_UNMATERIALIZED",
  "DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
]);

export function isIncompleteDependencyPlan(issue?: ManagedDependencyIssue) {
  return Boolean(issue && incompletePlanningCodes.has(issue.code));
}
export function candidateCanBePlanned(candidate: ManagedDependencyCandidate) {
  return candidate.compatible
    || (candidate.errors.length > 0 && candidate.errors.every(isIncompleteDependencyPlan));
}

export function choiceCanContinue(
  choice: ManagedDependencyChoice | undefined,
  localSelected: boolean,
) {
  if (!choice || choice.mode === "local") return localSelected;
  return Boolean(choice.provider && (
    choice.preview?.compatible || isIncompleteDependencyPlan(choice.planningIssue)
  ));
}

export function localDependencyDefaults(
  selections: ManagedDependencySelections,
): ManagedDependencySelections {
  return dependencyTypes.reduce((result, type) => ({
    ...result,
    [type]: result[type] || { mode: "local" },
  }), { ...selections });
}

export function providerIdentity(candidate: ManagedDependencyCandidate) {
  return `${candidate.cluster_id}:${candidate.service_name}`;
}
