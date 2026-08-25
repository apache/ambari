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
import rmHaApi from "./rmHaApi";
import { flattenClusterTopology } from "./rmHaUtils";

type MenuState = {
  requestKey: string;
  status: "loading" | "enabled" | "disabled" | "hidden";
};

function EnableHighAvailibilityResourceManger({
  isMappingOnly,
}: {
  isMappingOnly?: boolean;
}) {
  const { clusterName, allHostNames } = useContext(AppContext);
  const { componentName } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasAuthorization } = useAuth();
  const hasWorkflowPermissions =
    hasAuthorization("SERVICE.ENABLE_HA") &&
    hasAuthorization("CLUSTER.MANAGE_USER_PERSISTED_DATA");
  const hostCount = allHostNames.length;
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
  const shouldStartEnableFlow =
    location.pathname.includes("highAvailability") &&
    componentName === "ResourceManager";

  useEffect(() => {
    if (isMappingOnly || !hasWorkflowPermissions) {
      setMenuState({ requestKey, status: "disabled" });
      return;
    }
    if (!clusterName) {
      setMenuState({ requestKey, status: "disabled" });
      return;
    }

    let cancelled = false;
    setMenuState({ requestKey, status: "loading" });
    void rmHaApi
      .getClusterComponents(clusterName)
      .then((response) => {
        if (cancelled) return;
        const resourceManagers = flattenClusterTopology(response).filter(
          ({ component }) => component === "RESOURCEMANAGER",
        );
        if (resourceManagers.length > 1) {
          setMenuState({ requestKey, status: "hidden" });
          return;
        }
        const resourceManager = resourceManagers[0];
        const isNotInstalled =
          resourceManager?.state === "INIT" ||
          resourceManager?.state === "INSTALL_FAILED";
        setMenuState({
          requestKey,
          status:
            hostCount <= 1 || !resourceManager || isNotInstalled
              ? "disabled"
              : "enabled",
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
      {!isMappingOnly &&
      hasWorkflowPermissions &&
      menuStatus !== "hidden" ? (
        <Dropdown.Item
          disabled={menuStatus !== "enabled"}
          onClick={() => {
            navigate(
              `/main/services/highAvailability/ResourceManager/enable/step1`
            );
          }}
        >
          <FontAwesomeIcon className="text-secondary me-2" icon={faSitemap} />

          {ServiceActionEnums.enableRmHighAvailability}
        </Dropdown.Item>
      ) : null}
    </>
  );
}

export default EnableHighAvailibilityResourceManger;
