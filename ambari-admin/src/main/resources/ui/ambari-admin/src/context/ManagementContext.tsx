/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { adminApi } from "../api/configs/axiosConfig";
import { Authorization, hasPermission } from "../api/clusterManagement";

const ManagementContext = createContext({ loading: true, error: "", can: (_permission: string, _cluster?: string) => false });
export const useManagement = () => useContext(ManagementContext);
export function ManagementProvider({ children }: { children: ReactNode }) {
  const [grants, setGrants] = useState<Authorization[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try {
        const session = await adminApi.get("/clusters?fields=Clusters/cluster_id", { signal: controller.signal });
        const user = session.headers.user;
        if (typeof user !== "string" || !user) throw new Error("Missing authenticated user");
        const response = await adminApi.get(`/users/${encodeURIComponent(user)}/authorizations?fields=*`, { signal: controller.signal });
        if (!Array.isArray(response.data?.items)) throw new Error("Invalid authorization response");
        if (!controller.signal.aborted) setGrants(response.data.items.map((item: { AuthorizationInfo: Authorization }) => item.AuthorizationInfo));
      } catch {
        if (!controller.signal.aborted) setError("Unable to load your permissions. Management actions are disabled; reload this page to retry.");
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void load();
    return () => controller.abort();
  }, []);
  return <ManagementContext.Provider value={{ loading, error, can: (permission, cluster) => hasPermission(grants, permission, cluster) }}>{children}</ManagementContext.Provider>;
}
