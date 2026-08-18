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

export type AutoStartComponent = {
  ServiceComponentInfo: {
    category?: string;
    component_name: string;
    recovery_enabled: string;
    service_name: string;
    total_count?: number;
    componentDisplayName?: string;
  };
};

export function filterAutoStartComponents(
  components: AutoStartComponent[],
): AutoStartComponent[] {
  return components.filter(({ ServiceComponentInfo: component }) =>
    component.category !== "CLIENT" && Number(component.total_count || 0) > 0
  );
}

export function changedRecoveryComponents(
  current: AutoStartComponent[],
  cached: AutoStartComponent[],
): { enabled: string[]; disabled: string[] } {
  const cachedValues = new Map(
    cached.map(({ ServiceComponentInfo: component }) => [
      component.component_name,
      component.recovery_enabled,
    ]),
  );
  const changed = { enabled: [] as string[], disabled: [] as string[] };

  current.forEach(({ ServiceComponentInfo: component }) => {
    if (cachedValues.get(component.component_name) === component.recovery_enabled) {
      return;
    }
    const destination = component.recovery_enabled === "true"
      ? changed.enabled
      : changed.disabled;
    destination.push(component.component_name);
  });

  return changed;
}
