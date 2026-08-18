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

export type DeploymentOperation = {
  id: string;
  label: string;
  run: () => Promise<unknown>;
};

export type DeploymentStage = {
  operations: DeploymentOperation[];
  parallel?: boolean;
};

type DeploymentProgress = {
  completed: number;
  operation: DeploymentOperation;
  total: number;
};

export async function runDeploymentPlan(
  stages: DeploymentStage[],
  completedOperationIds: Set<string> = new Set(),
  onProgress: (progress: DeploymentProgress) => void | Promise<void> = () => undefined,
): Promise<Set<string>> {
  const operations = stages.flatMap((stage) => stage.operations);
  let completed = operations.filter((operation) =>
    completedOperationIds.has(operation.id),
  ).length;

  const runOperation = async (operation: DeploymentOperation) => {
    if (completedOperationIds.has(operation.id)) {
      return;
    }
    await operation.run();
    completedOperationIds.add(operation.id);
    completed += 1;
    await onProgress({ completed, operation, total: operations.length });
  };

  for (const stage of stages) {
    if (stage.parallel) {
      await Promise.all(stage.operations.map(runOperation));
    } else {
      for (const operation of stage.operations) {
        await runOperation(operation);
      }
    }
  }

  return completedOperationIds;
}
