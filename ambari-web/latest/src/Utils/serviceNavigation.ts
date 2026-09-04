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

type ServiceNavigationOptions = {
  availableTabs: Record<string, string[]>;
  canViewConfigs: boolean;
  canViewMetrics: boolean;
  installedServices: string[];
  requestedService?: string;
  requestedTab?: string;
};

export function resolveServiceNavigation({
  availableTabs,
  canViewConfigs,
  canViewMetrics,
  installedServices,
  requestedService,
  requestedTab,
}: ServiceNavigationOptions): {
  redirectPath?: string;
  selectedService?: string;
  selectedTab: string;
} {
  const selectedService =
    requestedService && installedServices.includes(requestedService)
      ? requestedService
      : installedServices[0];
  if (!selectedService) {
    return { selectedTab: "summary" };
  }

  if (selectedService !== requestedService) {
    return {
      redirectPath: `/main/services/${selectedService}/summary`,
      selectedService,
      selectedTab: "summary",
    };
  }

  const supportedTabs =
    availableTabs[selectedService.toUpperCase()] || ["summary", "configs"];
  const canSelectRequestedTab =
    Boolean(requestedTab) &&
    supportedTabs.includes(requestedTab as string) &&
    (requestedTab !== "configs" || canViewConfigs) &&
    (requestedTab !== "metrics" || canViewMetrics);
  const selectedTab = canSelectRequestedTab ? requestedTab as string : "summary";

  return {
    ...(selectedTab !== requestedTab && {
      redirectPath: `/main/services/${selectedService}/${selectedTab}`,
    }),
    selectedService,
    selectedTab,
  };
}
