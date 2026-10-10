/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useEffect, useState } from "react";
import { ArrowLeft, BoxSeam, Collection, Layers } from "react-bootstrap-icons";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import LicenseFooter from "../../components/LicenseFooter";
import NavBar from "../../components/Navbar";
import { useViewInstances } from "../Views/ViewInstancesContext";
import "./directories.scss";
import { useAuth } from "../../hooks/useAuth";
import { readWorkspace, rememberWorkspace, workspaceReturn } from "../../Utils/workspaceNavigation";

export default function GlobalDirectoryLayout() {
  const { t } = useTranslation();
  const { instances } = useViewInstances();
  const location = useLocation();
  const isServices = location.pathname === "/services";
  const isMpacks = location.pathname === "/mpacks";
  const { isAdmin, user } = useAuth();
  const username = user?.user_name || "";
  const [remembered, setRemembered] = useState(() => readWorkspace(username));
  useEffect(() => {
    const incoming = location.state?.workspaceReturn;
    const valid = incoming?.username === username ? workspaceReturn(username, incoming.path) : null;
    if (valid) rememberWorkspace(username, valid.path);
    setRemembered(current => valid || readWorkspace(username) || (current?.username === username ? current : null));
  }, [username, location.key, location.state]);
  const previous = remembered?.username === username ? remembered : null;
  const continuation = previous ? { workspaceReturn: previous } : undefined;

  return (
    <div className="console-shell d-flex flex-column h-100">
      <NavBar
        clusterControls={false}
        homePath="/clusters"
        subPath={isMpacks ? t("mpack.title") : isServices ? t("directory.services") : t("directory.clusters")}
        viewsList={instances}
      />
      <div className="global-directory-toolbar">
        <nav className="global-directory-nav" aria-label={t("directory.navigation")}>
          <NavLink to="/clusters" state={continuation}><Layers aria-hidden="true" />{t("directory.clusters")}</NavLink>
          <NavLink to="/services" state={continuation}><Collection aria-hidden="true" />{t("directory.services")}</NavLink>
          {isAdmin() && <NavLink to="/mpacks" state={continuation}><BoxSeam aria-hidden="true" />{t("mpack.title")}</NavLink>}
        </nav>
        {previous && <Link className="workspace-return" to={previous.path} title={t("workspaceNav.restorePage")}>
          <ArrowLeft aria-hidden="true" /><span><strong>{t("workspaceNav.return")}</strong><small>{previous.clusterName}</small></span>
        </Link>}
      </div>
      <div className="directory-scroll flex-grow-1">
        <Outlet />
      </div>
      <LicenseFooter hasSidebar={false} fixed={false} />
    </div>
  );
}
