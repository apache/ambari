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

import { useContext } from "react";
import { Navigate, useLocation } from "react-router-dom";
import {
  isViewOnlyUser,
  shouldUseMinimalViewsShell,
} from "../Utils/authPolicy";
import { useAuth } from "../hooks/useAuth";
import DashboardLayout from "./Dashboard";
import ViewsLayout from "./Views";
import { ViewInstancesProvider } from "../screens/Views/ViewInstancesContext";
import { AppContext } from "../store/context";

export default function MainLayout() {
  const { authorizations } = useAuth();
  const { isClusterInstalled } = useContext(AppContext);
  const location = useLocation();
  const viewOnly = isViewOnlyUser(authorizations);
  const viewRoute = location.pathname.startsWith("/main/view");
  if (viewOnly && !viewRoute) {
    return <Navigate to="/main/view" replace />;
  }
  const Layout = shouldUseMinimalViewsShell({
    clusterInstalled: isClusterInstalled,
    viewOnly,
    viewRoute,
  }) ? ViewsLayout : DashboardLayout;
  return <ViewInstancesProvider><Layout /></ViewInstancesProvider>;
}
