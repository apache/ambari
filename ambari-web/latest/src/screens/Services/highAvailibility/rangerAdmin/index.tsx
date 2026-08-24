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

import { Dropdown } from "react-bootstrap";
import { ServiceActionEnums } from "../../../../enums/ServiceActionEnums";
import { useContext, useEffect, useState } from "react";
import ValidateEnablement from "./ValidateEnablement";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faSitemap } from "@fortawesome/free-solid-svg-icons";
import useAuth from "../../../../hooks/useAuth";
import { AppContext } from "../../../../store/context";
import {
  evaluateRangerAdminEnablement,
  rangerAdminEnablementApi,
  type RangerAdminEnablementStatus,
} from "./rangerAdminHaApi";

type MenuState = {
  requestKey: string;
  status: "loading" | RangerAdminEnablementStatus;
};

function EnableHighAvailibilityRangerAdmin({
  isMappingOnly,
}: {
  isMappingOnly?: boolean;
}) {
  const { clusterName, allHostNames } = useContext(AppContext);
  const { hasAuthorization } = useAuth();
  const { componentName } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const shouldStartEnableFlow =
    location.pathname.includes("highAvailability") &&
    componentName === "RangerAdmin";
  const hasWorkflowPermissions =
    hasAuthorization("SERVICE.ENABLE_HA") &&
    hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const hostCount = allHostNames?.length ?? 0;
  const requestKey = [
    clusterName,
    hostCount,
    hasWorkflowPermissions,
    Boolean(isMappingOnly),
  ].join(":");
  const [menuState, setMenuState] = useState<MenuState>({
    requestKey: "",
    status: "loading",
  });
  const menuStatus =
    menuState.requestKey === requestKey ? menuState.status : "loading";

  useEffect(() => {
    if (isMappingOnly || !hasWorkflowPermissions || !clusterName) {
      setMenuState({ requestKey, status: "disabled" });
      return;
    }
    let cancelled = false;
    setMenuState({ requestKey, status: "loading" });
    void rangerAdminEnablementApi
      .loadRangerAdminComponent(clusterName)
      .then((component) => {
        if (cancelled) return;
        setMenuState({
          requestKey,
          status: evaluateRangerAdminEnablement(component, hostCount).status,
        });
      })
      .catch(() => {
        if (!cancelled) setMenuState({ requestKey, status: "disabled" });
      });
    return () => {
      cancelled = true;
    };
  }, [
    clusterName,
    hasWorkflowPermissions,
    hostCount,
    isMappingOnly,
    requestKey,
  ]);

  return (
    <>
      {shouldStartEnableFlow ? <ValidateEnablement /> : null}
      {!isMappingOnly && hasWorkflowPermissions && menuStatus !== "hidden" ? (
        <Dropdown.Item
          disabled={menuStatus !== "enabled"}
          onClick={() => {
            navigate(
              "/main/services/highAvailability/RangerAdmin/enable/step1",
            );
          }}
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />

          {ServiceActionEnums.enableRangerHighAvailibility}
        </Dropdown.Item>
      ) : null}
    </>
  );
}

export default EnableHighAvailibilityRangerAdmin;
