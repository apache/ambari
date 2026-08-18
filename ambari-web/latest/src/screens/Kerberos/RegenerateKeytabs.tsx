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
import { RequestApi } from "../../api/requestApi";
import BackgroundOperations from "../BackgroundOperations";
import { ProgressStatus, ViewLevel } from "../../constants";
import usePolling from "../../hooks/usePolling";
import { isFinished, showAlertModal } from "../../Utils/Utility";
import { AppContext } from "../../store/context";
import useKDCSessionState from "../../hooks/useKDCSessionState";
import { responseErrorMessage } from "../../Utils/httpError";

type RegenerateKeytabsProps = {
  missingHostCheck: boolean;
  restartComponentsCheck: boolean;
  onFinished: () => void;
};

export default function RegenerateKeytabs({
  missingHostCheck,
  restartComponentsCheck,
  onFinished,
}: RegenerateKeytabsProps) {
  const [showBgOperation, setShowBgOperation] = useState(false);
  const [backgroundRequestId, setBackgroundRequestId] = useState<
    string | number
  >("");
  const requestId = useRef<string | number>("");
  const restartCheckRef = useRef(restartComponentsCheck);
  const restartStarted = useRef(false);
  const regenerateStarted = useRef(false);
  const regenerateFinished = useRef(false);
  const closeRequested = useRef(false);
  const onFinishedRef = useRef(onFinished);
  onFinishedRef.current = onFinished;
  const { clusterName } = useContext(AppContext);
  const { getKDCSessionState } = useKDCSessionState(() => {});

  const { pausePolling, resumePolling } = usePolling(
    getRequestStatus,
    3000
  );

  useEffect(() => {
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
        setBackgroundRequestId(requestData.Requests.id);
        if (requestId.current !== "") setShowBgOperation(true);
      } catch (error) {
        showAlertModal(
          "Error",
          responseErrorMessage(error, "Ambari could not regenerate keytabs."),
        );
        onFinishedRef.current();
      }
    }

    if (regenerateStarted.current) {
      return;
    }
    regenerateStarted.current = true;
    void getKDCSessionState(
      regenerate,
      (error) => {
        showAlertModal(
          "Error",
          responseErrorMessage(
            error,
            "Ambari could not validate the KDC administrator session.",
          ),
        );
        onFinishedRef.current();
      },
    );
  }, [clusterName, getKDCSessionState, missingHostCheck]);

  useEffect(() => {
    restartCheckRef.current = restartComponentsCheck;
    if (restartComponentsCheck) {
      resumePolling();
    } else {
      pausePolling();
    }
  }, [pausePolling, restartComponentsCheck, resumePolling]);

  async function getRequestStatus() {
    if (!requestId.current) {
      return;
    }
    if (!restartCheckRef.current) pausePolling();

    const requestStatus = await RequestApi.getRequestStatus(
      clusterName,
      requestId.current as string
    );
    const { Requests } = requestStatus;
    if (isFinished(Requests.request_status)) {
      pausePolling();
      regenerateFinished.current = true;
      if (
        Requests.request_status === ProgressStatus.COMPLETED
        && restartCheckRef.current
        && !restartStarted.current
      ) {
        restartStarted.current = true;
        try {
          await restartComponents();
        } catch (error) {
          showAlertModal(
            "Error",
            responseErrorMessage(
              error,
              "Ambari could not restart components after regenerating keytabs.",
            ),
          );
          if (closeRequested.current) {
            onFinishedRef.current();
          }
        }
      } else if (closeRequested.current) {
        onFinishedRef.current();
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
    setBackgroundRequestId(requestData.Requests.id);
    setShowBgOperation(true);
  }

  return (
    <>
      {showBgOperation ? (
        <BackgroundOperations
          isOpen={showBgOperation}
          onClose={() => {
            setShowBgOperation(false);
            if (
              !restartCheckRef.current
              || restartStarted.current
              || regenerateFinished.current
            ) {
              onFinishedRef.current();
            } else {
              closeRequested.current = true;
            }
          }}
          rootLevel={ViewLevel.REQUESTS}
          requestId={backgroundRequestId}
        />
      ) : null}
    </>
  );
}
