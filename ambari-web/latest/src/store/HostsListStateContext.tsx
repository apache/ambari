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

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { useLocation } from "react-router-dom";
import { db } from "../Utils/db";

interface HostsListStateContextType {
  selectedFilters: any;
  setSelectedFilters: (filters: any | ((prev: any) => any)) => void;
  selectedHosts: string[];
  setSelectedHosts: (
    hosts: string[] | ((prev: string[]) => string[])
  ) => void;
}

const HostsListStateContext = createContext<
  HostsListStateContextType | undefined
>(undefined);

/**
 * Hosts List page filters and host selection are held here rather than inside
 * HostsList so they survive the unmount that happens when navigating to a Host
 * page. Filters are in memory and reset once the user leaves the Hosts pages;
 * the host selection is mirrored into the local db (the same
 * app.tables.selectedItems.mainHostController slot Ember uses) so it also
 * survives a page refresh.
 */
export const HostsListStateProvider: React.FC<{
  children: React.ReactNode;
}> = ({ children }) => {
  const location = useLocation();
  const [selectedFilters, setSelectedFilters] = useState<any>([]);
  const [selectedHosts, setSelectedHostsState] = useState<string[]>(() =>
    db.getSelectedHosts()
  );
  const wasOnHostsPages = useRef(false);

  const isOnHostsPages = /^\/main\/hosts(\/|$)/.test(location.pathname);

  useEffect(() => {
    if (wasOnHostsPages.current && !isOnHostsPages) {
      setSelectedFilters([]);
    }
    wasOnHostsPages.current = isOnHostsPages;
  }, [isOnHostsPages]);

  // Persist as part of the setter rather than in an effect: an effect also runs
  // on mount/remount, which could write an empty selection over the stored one
  // before the restore had been applied.
  const setSelectedHosts = useCallback(
    (hosts: string[] | ((prev: string[]) => string[])) => {
      const next =
        typeof hosts === "function" ? hosts(db.getSelectedHosts()) : hosts;
      db.setSelectedHosts(next);
      setSelectedHostsState(next);
    },
    []
  );

  return (
    <HostsListStateContext.Provider
      value={{
        selectedFilters,
        setSelectedFilters,
        selectedHosts,
        setSelectedHosts,
      }}
    >
      {children}
    </HostsListStateContext.Provider>
  );
};

export const useHostsListState = (): HostsListStateContextType => {
  const context = useContext(HostsListStateContext);
  if (!context) {
    throw new Error(
      "useHostsListState must be used within a HostsListStateProvider"
    );
  }
  return context;
};
