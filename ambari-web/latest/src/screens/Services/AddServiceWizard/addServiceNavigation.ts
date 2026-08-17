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

export type AddServiceFlow = {
  skipConfigStep: boolean;
  skipMasterStep: boolean;
  skipSlavesStep: boolean;
};

type ServiceFlowMetadata = {
  hasClient?: boolean;
  hasConfigs?: boolean;
  hasMaster?: boolean;
  hasNonMastersWithCustomAssignment?: boolean;
  hasSlave?: boolean;
  installed?: boolean;
  selected?: boolean;
};

export function deriveAddServiceFlow(
  services: Record<string, ServiceFlowMetadata>,
): AddServiceFlow {
  const selected = Object.values(services).filter(
    (service) => service.selected && !service.installed,
  );
  return {
    skipMasterStep: selected.every((service) => !service.hasMaster),
    skipSlavesStep: selected.every((service) =>
      !service.hasSlave
      && !service.hasClient
      && !service.hasNonMastersWithCustomAssignment,
    ),
    skipConfigStep: selected.every((service) => service.hasConfigs === false),
  };
}

export function nextAddServiceStep(
  currentStep: number,
  flow: AddServiceFlow,
): number {
  const candidates = [2, 3, 4, 5];
  return candidates.find((step) => step > currentStep && !(
    (step === 2 && flow.skipMasterStep)
    || (step === 3 && flow.skipSlavesStep)
    || (step === 4 && flow.skipConfigStep)
  )) ?? 5;
}

export function previousAddServiceStep(
  currentStep: number,
  flow: AddServiceFlow,
): number {
  const candidates = [4, 3, 2, 1];
  return candidates.find((step) => step < currentStep && !(
    (step === 2 && flow.skipMasterStep)
    || (step === 3 && flow.skipSlavesStep)
    || (step === 4 && flow.skipConfigStep)
  )) ?? 1;
}
