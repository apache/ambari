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
import AdminApi from "../api/adminApi";
import { AppContext } from "../store/context";
import { get } from "lodash";
import modalManager from "../store/ModalManager";
import InvalidKdcPopup from "../components/InvalidKdcPopup";
//@ts-ignore
function useKDCSessionState(cancelHandler: unknown) {
  const [securityEnabled, setSecurityEnabled] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const { clusterName, isKerberosEnabled } = useContext(AppContext);
  const kdc_type = useRef("");
  async function getSecurityType(additionalCallback: any) {
    if (securityEnabled || isKerberosEnabled) {
      try {
        const data = await AdminApi.getSecurityType(clusterName);
        const kdcType =
          data?.items?.[0]?.configurations?.find(
            (config: any) => config.type === "kerberos-env"
          )?.properties?.kdc_type ?? "none";
        kdc_type.current = kdcType;
        additionalCallback();
      } catch (err) {
        console.error("Could not get security type", err);
      }
    } else {
      additionalCallback();
    }
  }
  useEffect(() => {
    async function getSecurityStatus() {
      try {
        const data = await AdminApi.getSecurityStatus(clusterName);
        setIsLoaded(true);
        const securityType = data.Clusters.security_type;
        setSecurityEnabled(securityType === "KERBEROS");
      } catch (error) {
        setIsLoaded(true);
        modalManager.show(<InvalidKdcPopup />);
      }
    }
    if (clusterName) {
      getSecurityStatus();
    }
  }, [clusterName]);

  const getKDCSessionState = async (callback: Function) => {
    if (securityEnabled || isKerberosEnabled) {
      getSecurityType(async function () {
        if (kdc_type.current !== "none") {
          const data = await AdminApi.getKerberosSessionState(clusterName);
          const res = get(data, "Services.attributes.kdc_validation_result");
          get(data, "Services.attributes.kdc_validation_failure_details");
          if (res.toUpperCase() === "OK") {
            callback();
          } else {
            modalManager.show(
              <InvalidKdcPopup
                getKdcSessionState={() => {
                  getKDCSessionState(callback);
                }}
              />
            );
          }
        } else {
          callback();
        }
      });
    } else {
      callback();
    }
  };

  return { isLoaded, getKDCSessionState };
}

export default useKDCSessionState;
