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

/**
 * Filters applied when the Hosts List is opened from elsewhere in the app.
 *
 * Mirrors Ember's routes/main.js `filterHosts`, which sets the filter before
 * transitioning so the page mounts already filtered. A URL-param route (e.g.
 * hosts/component/:componentName) would remount HostsList and reset the
 * shared filter/selection state from HostsListStateContext, and the filter is
 * only derivable after cluster data loads - so an unfiltered request would go
 * out first anyway. Setting the filter via context and navigating to the
 * plain /main/hosts route avoids both problems.
 */

import { useNavigate } from "react-router-dom";
import { useHostsListState } from "../../store/HostsListStateContext";

export type HostsFilter = {
  field: { label: string; value: string; name?: string };
  value: { label: any; value: any };
};

export const HOSTS_LIST_PATH = "/main/hosts";

/**
 * Filter matching every host that runs the given component.
 */
export const buildComponentHostsFilter = (
  componentName: string,
  displayName?: string
): HostsFilter[] => [
  {
    field: {
      label: displayName || componentName,
      value: componentName,
      name: "componentState",
    },
    value: {
      label: "All",
      value: "ALL",
    },
  },
];

/**
 * Filter matching every host on the given stack version and state. NOT_INSTALLED
 * expands to what the versions page counts as "not installed".
 */
export const buildVersionHostsFilter = (
  versionName: string,
  versionStatus: string
): HostsFilter[] => {
  const status =
    versionStatus === "NOT_INSTALLED"
      ? ["INSTALLING", "INSTALL_FAILED", "OUT_OF_SYNC"]
      : versionStatus;
  return [
    {
      field: { label: "Stack Version", value: "version", name: "host" },
      value: { label: versionName, value: versionName },
    },
    {
      field: { label: "Version State", value: "versionState", name: "host" },
      value: { label: status, value: status },
    },
  ];
};

/**
 * Opens the Hosts List pre-filtered, applying the filter before navigating so
 * the page never issues an unfiltered request first.
 */
export const useHostsFilterNavigation = () => {
  const navigate = useNavigate();
  const { setSelectedFilters } = useHostsListState();

  const goToHostsFilteredByComponent = (
    componentName: string,
    displayName?: string
  ) => {
    setSelectedFilters(buildComponentHostsFilter(componentName, displayName));
    navigate(HOSTS_LIST_PATH);
  };

  const goToHostsFilteredByVersion = (
    versionName: string,
    versionStatus: string
  ) => {
    setSelectedFilters(buildVersionHostsFilter(versionName, versionStatus));
    navigate(HOSTS_LIST_PATH);
  };

  return { goToHostsFilteredByComponent, goToHostsFilteredByVersion };
};
