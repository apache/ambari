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

import { useContext, useEffect, useRef, useState } from "react";
import adminApi from "../api/adminApi";
import { AppContext } from "../store/context";
import { get } from "lodash";
import modalManager from "../store/ModalManager";
import InvalidKdcPopup from "../components/InvalidKdcPopup";

type KDCSessionCallback = () => void | Promise<void>;
type KDCSessionErrorCallback = (error: unknown) => void;
type KerberosConfiguration = {
  properties?: { kdc_type?: string };
  type?: string;
};

function useKDCSessionState(_cancelHandler: unknown) {
  const [securityEnabled, setSecurityEnabled] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const { clusterName, isKerberosEnabled } = useContext(AppContext);
  const kdc_type = useRef("");
  async function getSecurityType() {
    if (securityEnabled || isKerberosEnabled) {
      const data = await adminApi.getSecurityType(clusterName);
      kdc_type.current =
        data?.items?.[0]?.configurations?.find(
          (config: KerberosConfiguration) => config.type === "kerberos-env"
        )?.properties?.kdc_type ?? "none";
    }
  }
  useEffect(() => {
    async function getSecurityStatus() {
      try {
        const data = await adminApi.getSecurityStatus(clusterName);
        setIsLoaded(true);
        const securityType = data.Clusters.security_type;
        setSecurityEnabled(securityType === "KERBEROS");
      } catch {
        setIsLoaded(true);
        modalManager.show(<InvalidKdcPopup />);
      }
    }
    if (clusterName) {
      getSecurityStatus();
    }
  }, [clusterName]);

  const getKDCSessionState = async (
    callback: KDCSessionCallback,
    errorCallback?: KDCSessionErrorCallback,
  ): Promise<void> => {
    try {
      if (securityEnabled || isKerberosEnabled) {
        await getSecurityType();
        if (kdc_type.current !== "none") {
          const data = await adminApi.getKerberosSessionState(clusterName);
          const result = get(data, "Services.attributes.kdc_validation_result", "");
          if (result.toUpperCase() === "OK") {
            await callback();
          } else {
            const reportCredentialFailure = (error: unknown) => {
              errorCallback?.(error);
            };
            modalManager.show(
              <InvalidKdcPopup
                getKdcSessionState={() => {
                  void getKDCSessionState(callback, errorCallback);
                }}
                onCancel={() =>
                  reportCredentialFailure(
                    new Error("KDC credential entry was cancelled."),
                  )
                }
                onError={reportCredentialFailure}
              />
            );
          }
        } else {
          await callback();
        }
      } else {
        await callback();
      }
    } catch (error) {
      if (errorCallback) {
        errorCallback(error);
        return;
      }
      console.error("Could not validate the KDC session", error);
    }
  };

  return { isLoaded, getKDCSessionState };
}

export default useKDCSessionState;
