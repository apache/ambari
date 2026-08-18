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

import { useContext, useEffect, useState } from "react";
import ClusterApi from "../api/clusterApi";
import { ChooseServicesApi } from "../api/chooseServicesApi";
import { AppContext } from "../store/context";

function useStackServices() {
  const [services, setServices] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retryAttempt, setRetryAttempt] = useState(0);
  const { isClusterInstalled } = useContext(AppContext);

  useEffect(() => {
    let active = true;

    const fetchServices = async () => {
      if (!isClusterInstalled) {
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const clusterName = await ClusterApi.getClusterName();
        const clusterDetails = await ClusterApi.getDesiredClusterConfigs(
          clusterName,
          "Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id",
        );
        const [stack, version] = String(clusterDetails?.Clusters?.version || "").split("-");
        if (!stack || !version) {
          throw new Error("The current stack version could not be determined");
        }
        const response = await ChooseServicesApi.getServices(stack, version);
        if (active) {
          setServices(response.items || []);
        }
      } catch (requestError: any) {
        if (active) {
          setError(
            requestError?.response?.data?.message
              || requestError?.message
              || "Stack services could not be loaded"
          );
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void fetchServices();
    return () => {
      active = false;
    };
  }, [isClusterInstalled, retryAttempt]);

  return {
    services,
    loading,
    error,
    retry: () => {
      setRetryAttempt((attempt) => attempt + 1);
    },
  };
}

export default useStackServices;
