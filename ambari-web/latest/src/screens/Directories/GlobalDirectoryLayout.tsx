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

import { Nav } from "react-bootstrap";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import LicenseFooter from "../../components/LicenseFooter";
import NavBar from "../../components/Navbar";
import { useViewInstances } from "../Views/ViewInstancesContext";
import "./directories.scss";

export default function GlobalDirectoryLayout() {
  const { t } = useTranslation();
  const { instances } = useViewInstances();
  const location = useLocation();
  const isServices = location.pathname === "/services";

  return (
    <div className="d-flex flex-column h-100">
      <NavBar
        clusterControls={false}
        homePath="/clusters"
        subPath={isServices ? t("directory.services") : t("directory.clusters")}
        viewsList={instances}
      />
      <Nav className="directory-nav px-3 px-md-4" variant="tabs" aria-label={t("directory.navigation")}>
        <Nav.Item>
          <Nav.Link as={NavLink} to="/clusters">{t("directory.clusters")}</Nav.Link>
        </Nav.Item>
        <Nav.Item>
          <Nav.Link as={NavLink} to="/services">{t("directory.services")}</Nav.Link>
        </Nav.Item>
      </Nav>
      <div className="directory-scroll flex-grow-1">
        <Outlet />
      </div>
      <LicenseFooter hasSidebar={false} />
    </div>
  );
}
