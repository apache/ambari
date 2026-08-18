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

import { Navigate, Outlet, RouteObject } from "react-router-dom";
import { AuthenticatedApplication, LandingRoute } from "../AppLoader";
import { ProtectedRoute } from "../components/AuthGuard";
import AdminRouteGuard from "../components/AdminRouteGuard";
import InstallerLayout from "../layout/Installer";
import MainLayout from "../layout/Main";
import InstallerVersionGuard from "../layout/InstallerVersionGuard";
import ClusterCreationWizard from "../screens/ClusterWizard";
import {
  ClusterCreationContext,
  ClusterCreationProvider,
} from "../screens/ClusterWizard/clusterStore/context";
import wizardSteps from "../screens/ClusterWizard/wizardSteps";
import { Login } from "../screens/Authentication/Login";
import HostsList from "../screens/Hosts/HostsList";
import { Hosts } from "../screens/Hosts";
import Alerts from "../screens/Alerts/Alerts";
import AlertDefinitionDetails from "../screens/Alerts/AlertDefinitionDetails";
import ServiceAutoStart from "../screens/ServiceAutoStart";
import ServiceAccounts from "../screens/ServiceAccounts";
import EnableKerberos from "../screens/KerberosWizard/EnableKerberos";
import { Actions } from "../screens/Services/Actions";
import ServiceDashboard from "../screens/Services/ServiceDashboard";
import ServiceLoader from "../screens/Services/ServiceLoader";
import AddWizardUrlMapping from "../screens/Services/AddWizardUrlMapping";
import ViewDetails from "../screens/Views/ViewDetails";
import ViewsListPage from "../screens/Views/ViewsListPage";
import StackAndVersions from "../screens/ClusterAdmin/StackAndVersions/StackAndVersions";
import Dashboard from "../screens/Dashboard/Index";
import AdminViewRedirect from "../screens/Authentication/AdminViewRedirect";
import Experimental from "../screens/Experimental";
import FeatureRouteGuard from "../components/FeatureRouteGuard";
import AdminViewRouteGuard from "../components/AdminViewRouteGuard";
import ServiceOperationRouteGuard from "../components/ServiceOperationRouteGuard";

const RoutesList: RouteObject[] = [
  {
    path: "/",
    element: <Outlet />,
    children: [
      { path: "login", element: <Login /> },
      { path: "login/local", element: <Login isLocalLogin /> },
      {
        element: <AuthenticatedApplication />,
        children: [
          { index: true, element: <LandingRoute /> },
          {
            path: "adminView",
            element: (
              <AdminViewRouteGuard>
                <AdminViewRedirect />
              </AdminViewRouteGuard>
            ),
          },
          {
            path: "experimental",
            element: (
              <ProtectedRoute
                requireAuthorization="AMBARI.MANAGE_SETTINGS"
                redirectTo="/"
              >
                <Experimental />
              </ProtectedRoute>
            ),
          },
          {
            path: "installer",
            element: <InstallerVersionGuard><InstallerLayout /></InstallerVersionGuard>,
            children: [
              {
                path: ":stepNumber",
                element: (
                  <ProtectedRoute
                    requireAuthorization="AMBARI.ADD_DELETE_CLUSTERS"
                    redirectTo="/main/view"
                  >
                    <ServiceOperationRouteGuard>
                      <ClusterCreationWizard
                        Context={ClusterCreationContext}
                        Provider={ClusterCreationProvider}
                        wizardSteps={wizardSteps as any}
                      />
                    </ServiceOperationRouteGuard>
                  </ProtectedRoute>
                ),
              },
            ],
          },
          {
            path: "main",
            element: <MainLayout />,
            children: [
              {
                path: "service/add/:stepNumber",
                element: (
                  <FeatureRouteGuard feature="enableAddDeleteServices">
                    <ProtectedRoute
                      requireAuthorization="SERVICE.ADD_DELETE_SERVICES"
                      redirectTo="/main/dashboard/metrics"
                    >
                      <ServiceOperationRouteGuard>
                        <AddWizardUrlMapping />
                      </ServiceOperationRouteGuard>
                    </ProtectedRoute>
                  </FeatureRouteGuard>
                ),
              },
              {
                path: "dashboard",
                element: <Outlet />,
                children: [
                  { index: true, element: <Navigate to="metrics" replace /> },
                  { path: ":tabName", element: <Dashboard /> },
                ],
              },
              { path: "actions", element: <Actions serviceName="HDFS" /> },
              {
                path: "services",
                element: <Outlet />,
                children: [
                  {
                    path: ":serviceName",
                    element: <Outlet />,
                    children: [
                      { path: ":tabName", element: <ServiceDashboard /> },
                    ],
                  },
                  {
                    path: "highAvailability/:componentName/enable/:stepNumber",
                    element: (
                      <ProtectedRoute
                        requireAuthorization="SERVICE.ENABLE_HA"
                        redirectTo="/main/dashboard/metrics"
                      >
                        <ServiceOperationRouteGuard>
                          <ServiceLoader />
                        </ServiceOperationRouteGuard>
                      </ProtectedRoute>
                    ),
                  },
                  {
                    path: ":componentName/federation/:stepNumber",
                    element: (
                      <ProtectedRoute
                        requireAuthorization="SERVICE.ENABLE_HA"
                        redirectTo="/main/dashboard/metrics"
                      >
                        <ServiceOperationRouteGuard>
                          <ServiceLoader />
                        </ServiceOperationRouteGuard>
                      </ProtectedRoute>
                    ),
                  },
                  {
                    path: "highAvailability/:componentName/manage/:stepNumber",
                    element: (
                      <ProtectedRoute
                        requireAuthorization="SERVICE.ENABLE_HA"
                        redirectTo="/main/dashboard/metrics"
                      >
                        <ServiceOperationRouteGuard>
                          <ServiceLoader />
                        </ServiceOperationRouteGuard>
                      </ProtectedRoute>
                    ),
                  },
                ],
              },
              {
                path: "service/reassign/:componentName/:stepNumber",
                element: (
                  <ProtectedRoute
                    requireAuthorization="SERVICE.MOVE"
                    redirectTo="/main/dashboard/metrics"
                  >
                    <ServiceOperationRouteGuard>
                      <ServiceLoader />
                    </ServiceOperationRouteGuard>
                  </ProtectedRoute>
                ),
              },
              { path: "hosts", element: <HostsList /> },
              { path: "hosts/component/:componentName", element: <HostsList /> },
              { path: "hosts/version/:versionName/:versionStatus", element: <HostsList /> },
              { path: "hosts/:hostname/:tab", element: <Hosts /> },
              {
                path: "host/add/:stepNumber",
                element: (
                  <ProtectedRoute
                    requireAuthorization="HOST.ADD_DELETE_HOSTS"
                    redirectTo="/main/hosts"
                  >
                    <ServiceOperationRouteGuard>
                      <AddWizardUrlMapping />
                    </ServiceOperationRouteGuard>
                  </ProtectedRoute>
                ),
              },
              { path: "alerts", element: <Alerts /> },
              { path: "alerts/:alertId", element: <AlertDefinitionDetails /> },
              {
                path: "admin",
                element: (
                  <AdminRouteGuard>
                    <Outlet />
                  </AdminRouteGuard>
                ),
                children: [
                  { index: true, element: <Navigate to="stack/services" replace /> },
                  {
                    path: "stack/:tabName",
                    element: (
                      <ProtectedRoute
                        requireAuthorization="CLUSTER.VIEW_STACK_DETAILS, CLUSTER.UPGRADE_DOWNGRADE_STACK"
                        redirectTo="/main/dashboard/metrics"
                      >
                        <StackAndVersions />
                      </ProtectedRoute>
                    ),
                  },
                  {
                    path: "serviceAutoStart",
                    element: (
                      <FeatureRouteGuard feature="serviceAutoStart">
                        <ProtectedRoute
                          requireAuthorization="SERVICE.MANAGE_AUTO_START, CLUSTER.MANAGE_AUTO_START"
                          redirectTo="/main/dashboard/metrics"
                        >
                          <ServiceAutoStart />
                        </ProtectedRoute>
                      </FeatureRouteGuard>
                    ),
                  },
                  {
                    path: "serviceAccounts",
                    element: (
                      <ProtectedRoute
                        requireAuthorization="SERVICE.SET_SERVICE_USERS_GROUPS"
                        redirectTo="/main/dashboard/metrics"
                      >
                        <ServiceAccounts />
                      </ProtectedRoute>
                    ),
                  },
                  {
                    path: "kerberos",
                    element: (
                      <FeatureRouteGuard feature="enableToggleKerberos">
                        <ProtectedRoute
                          requireAuthorization="CLUSTER.TOGGLE_KERBEROS"
                          redirectTo="/main/dashboard/metrics"
                        >
                          <EnableKerberos />
                        </ProtectedRoute>
                      </FeatureRouteGuard>
                    ),
                  },
                  {
                    path: "kerberos/enable/:stepNumber",
                    element: (
                      <FeatureRouteGuard feature="enableToggleKerberos">
                        <ProtectedRoute
                          requireAuthorization="CLUSTER.TOGGLE_KERBEROS"
                          redirectTo="/main/dashboard/metrics"
                        >
                          <EnableKerberos />
                        </ProtectedRoute>
                      </FeatureRouteGuard>
                    ),
                  },
                ],
              },
              { path: "views/:viewName/:viewVersion/:instanceName/*", element: <ViewDetails /> },
              { path: "view", element: <ViewsListPage /> },
              { path: "view/:viewName", element: <Navigate to="/main/view" replace /> },
            ],
          },
        ],
      },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
];

export default RoutesList;
