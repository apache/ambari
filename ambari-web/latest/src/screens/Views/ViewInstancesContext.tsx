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

import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import ViewApi from "../../api/viewApi";
import {
  flattenVisibleViewInstances,
  ViewInstance,
} from "../../Utils/viewUtils";
import axios from "axios";

type ViewInstancesContextValue = {
  error: string | null;
  instances: ViewInstance[];
  isLoading: boolean;
  reload: () => Promise<void>;
};

const ViewInstancesContext = createContext<ViewInstancesContextValue | undefined>(undefined);

export function ViewInstancesProvider({ children }: { children: ReactNode }) {
  const [instances, setInstances] = useState<ViewInstance[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await ViewApi.getInstances();
      setInstances(flattenVisibleViewInstances(data));
    } catch (reason: unknown) {
      setInstances([]);
      setError(
        (axios.isAxiosError<{ message?: string }>(reason)
          ? reason.response?.data?.message
          : undefined)
        || "Ambari could not load the available Views.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return (
    <ViewInstancesContext.Provider value={{ error, instances, isLoading, reload }}>
      {children}
    </ViewInstancesContext.Provider>
  );
}

export function useViewInstances(): ViewInstancesContextValue {
  const context = useContext(ViewInstancesContext);
  if (!context) {
    throw new Error("useViewInstances must be used within ViewInstancesProvider");
  }
  return context;
}

export default ViewInstancesContext;
