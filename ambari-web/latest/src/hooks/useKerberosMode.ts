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
import adminApi from "../api/adminApi";
import { AppContext } from "../store/context";
import { responseErrorMessage } from "../Utils/httpError";

export function kerberosTypeFromConfig(response: any): string {
  return response?.items?.[0]?.configurations?.find(
    (configuration: any) => configuration.type === "kerberos-env",
  )?.properties?.kdc_type || "none";
}

export default function useKerberosMode() {
  const { clusterName, isKerberosEnabled } = useContext(AppContext);
  const [kdcType, setKdcType] = useState("");
  const [isLoaded, setIsLoaded] = useState(!isKerberosEnabled);
  const [loadError, setLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    if (!isKerberosEnabled || !clusterName) {
      setKdcType("");
      setLoadError("");
      setIsLoaded(true);
      return () => {
        active = false;
      };
    }
    setIsLoaded(false);
    setLoadError("");
    void adminApi.getSecurityType(clusterName)
      .then((response) => {
        if (active) {
          setKdcType(kerberosTypeFromConfig(response));
        }
      })
      .catch((error) => {
        if (active) {
          setKdcType("");
          setLoadError(
            responseErrorMessage(
              error,
              "Ambari could not determine the current Kerberos mode.",
            ),
          );
        }
      })
      .finally(() => {
        if (active) {
          setIsLoaded(true);
        }
      });
    return () => {
      active = false;
    };
  }, [clusterName, isKerberosEnabled, reloadKey]);

  return {
    isLoaded,
    isManualKerberos: isKerberosEnabled && kdcType === "none",
    kdcType,
    loadError,
    retry: () => setReloadKey((value) => value + 1),
  };
}
