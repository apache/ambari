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

import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import ViewApi from "../api/viewApi";
import LicenseFooter from "../components/LicenseFooter";
import NavBar from "../components/Navbar";

type ViewLink = {
  instance_name: string;
  label?: string;
  version: string;
  view_name: string;
};

type ViewInstanceResponse = { ViewInstanceInfo?: Omit<ViewLink, "version" | "view_name"> };
type ViewVersionResponse = {
  ViewVersionInfo?: { version?: string };
  instances?: ViewInstanceResponse[];
};
type ViewResponse = {
  ViewInfo?: { view_name?: string };
  versions?: ViewVersionResponse[];
};

function viewLinks(data: { items?: ViewResponse[] }): ViewLink[] {
  return (data?.items || []).flatMap((item) => {
    const viewName = item?.ViewInfo?.view_name;
    return (item?.versions || []).flatMap((version) => (
      (version?.instances || []).flatMap((instance) => {
        const instanceInfo = instance.ViewInstanceInfo;
        const versionNumber = version.ViewVersionInfo?.version;
        return instanceInfo?.instance_name && versionNumber && viewName
          ? [{ ...instanceInfo, version: versionNumber, view_name: viewName }]
          : [];
      })
    ));
  });
}

export default function ViewsLayout() {
  const [views, setViews] = useState<ViewLink[]>([]);

  useEffect(() => {
    ViewApi.getInstances()
      .then((data) => setViews(viewLinks(data)))
      .catch(() => setViews([]));
  }, []);

  return (
    <div className="d-flex flex-column h-100">
      <NavBar
        clusterControls={false}
        homePath="/main/view"
        viewsList={views}
        subPath="Views"
      />
      <div className="h-100" style={{ paddingBottom: "80px", overflowY: "auto" }}>
        <Outlet />
      </div>
      <LicenseFooter hasSidebar={false} />
    </div>
  );
}
