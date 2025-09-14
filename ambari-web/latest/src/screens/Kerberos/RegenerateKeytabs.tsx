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
import { ProgressStatus } from "../../constants";
import usePolling from "../../hooks/usePolling";
import { isFinished } from "../../Utils/Utility";
import { AppContext } from "../../store/context";
import { RequestApi } from "../../api/requestApi";
import KerberosApi from "../../api/kerberosApi";
import InvalidKDCPopup from "./InvalidKdcPopup";

type RegenerateKeytabsProps = {
  missingHostCheck: boolean;
  restartComponentsCheck: boolean;
};

export default function RegenerateKeytabs({
  missingHostCheck,
  restartComponentsCheck,
}: RegenerateKeytabsProps) {
  // @ts-ignore
  const [showBgOperation, setShowBgOperation] = useState(false);
  const [showInvalidKDCPopup, setShowInvalidKDCPopup] = useState(false);
  const requestId = useRef<string | number>("");
  const restartCheckRef = useRef(restartComponentsCheck);
  const { clusterName } = useContext(AppContext);

  const { stopPolling, pausePolling, resumePolling } = usePolling(
    getRequestStatus,
    3000
  );

  useEffect(() => {
    regenerate();
  }, []);

  useEffect(() => {
    if (restartComponentsCheck) {
      resumePolling();
    } else {
      pausePolling();
    }
  }, [restartComponentsCheck]);

  async function getRequestStatus() {
    if (!restartCheckRef.current) pausePolling();

    const requestStatus: any = await RequestApi.getRequestStatus(
      clusterName,
      requestId.current as string
    );
    const { Requests } = requestStatus;
    if (isFinished(Requests.request_status)) {
      if (Requests.request_status === ProgressStatus.COMPLETED) {
        stopPolling();
        if (restartCheckRef.current) restartComponents();
      }
    }
  }

  async function restartComponents() {
    const restartPayload = {
      RequestInfo: {
        command: "RESTART",
        context: "Restart all services",
        operation_level: "host_component",
      },
      "Requests/resource_filters": [
        {
          hosts_predicate: `HostRoles/cluster_name=${clusterName}`,
        },
      ],
    };
    const requestData = await RequestApi.postRequest(
      clusterName,
      restartPayload
    );
    requestId.current = requestData.Requests.id;
  }

  async function regenerate() {
    const payload = {
      Clusters: {
        security_type: "KERBEROS",
      },
    };
    try {
      const params = missingHostCheck
        ? "regenerate_keytabs=missing"
        : "regenerate_keytabs=all";
      const requestData = await RequestApi.regenerateKeytabs(
        clusterName,
        payload,
        params
      );
      requestId.current = requestData.Requests.id;
      if (requestId.current !== "") setShowBgOperation(true);
    } catch (error) {
      console.log("Error regenerating keytabs: ", error);
      setShowInvalidKDCPopup(true);
    }
  }

  const handleSaveInvalidKDC = async (
    adminPrincipal: string,
    adminPassword: string,
    saveCredentials: boolean
  ) => {
    setShowInvalidKDCPopup(false);
    const payload = {
      Credential: {
        key: adminPassword,
        principal: adminPrincipal,
        type: saveCredentials ? "persisted" : "temporary",
      },
    };

    try {
      await KerberosApi.postKDCAdminCredentials(
        clusterName,
        payload
      );

      // Retry the regenerateKeytabs API call with the provided credentials
      await regenerate();
    } catch (error) {
      console.error("Error posting KDC Admin Credentials:", error);
    }
  };

  return (
    <>
      {/* {showBgOperation ? (
        <BackgroundOperations
          isOpen={showBgOperation}
          onClose={() => setShowBgOperation(false)}
          rootLevel={ViewLevel.REQUESTS}
          requestId={requestId.current}
        />
      ) : null} */}

      <InvalidKDCPopup
        isOpen={showInvalidKDCPopup}
        onClose={() => setShowInvalidKDCPopup(false)}
        handleSave={handleSaveInvalidKDC}
      />
    </>
  );
}
