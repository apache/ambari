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

import { RouteObject, Navigate, Outlet } from "react-router-dom";
import ClusterCreationWizard from "../screens/ClusterWizard";
import { Login } from "../screens/Authentication/Login.tsx";
import InstallerLayout from "../layout/Installer";
import HostsList from "../screens/Hosts/HostsList.tsx";
import Alerts from "../screens/Alerts/Alerts.tsx";
import DashboardLayout from "../layout/Dashboard";
import ServiceAutoStart from "../screens/ServiceAutoStart/index.tsx";
import ServiceAccounts from "../screens/ServiceAccounts/index.tsx";
import EnableKerberos from "../screens/KerberosWizard/EnableKerberos.tsx";
import { Actions } from "../screens/Services/Actions.tsx";
import AlertDefinitionDetails from "../screens/Alerts/AlertDefinitionDetails.tsx";
import ServiceDashboard from "../screens/Services/ServiceDashboard.tsx";
import { redirectToAdminView } from "../Utils/adminViewRedirect";
import { Hosts } from "../screens/Hosts/index.tsx";
import Spinner from "../components/Spinner";
import ViewDetails from "../screens/Views/ViewDetails.tsx";
import ViewsListPage from "../screens/Views/ViewsListPage";
import { RouteTracker } from "../AppLoader.tsx";
import StackAndVersions from "../screens/ClusterAdmin/StackAndVersions/StackAndVersions.tsx";
import {
  ClusterCreationContext,
  ClusterCreationProvider,
} from "../screens/ClusterWizard/clusterStore/context.tsx";
import wizardSteps from "../screens/ClusterWizard/wizardSteps.tsx";
import ServiceLoader from "../screens/Services/ServiceLoader.tsx";
import Dashboard from "../screens/Dashboard/Index.tsx";
import AddWizardUrlMapping from "../screens/Services/AddWizardUrlMapping.tsx";
import { ProtectedRoute } from "../components/AuthGuard";
import AdminRouteGuard from "../components/AdminRouteGuard";

const RoutesList: RouteObject[] = [
  {
    path: "/",
    element: (
      <>
        <RouteTracker />
        <Outlet />
      </>
    ),
    children: [
      {
        path: "installer",
        element: <InstallerLayout />,
        children: [
          {
            path: ":stepNumber",
            element: (
              <ClusterCreationWizard
                Context={ClusterCreationContext}
                Provider={ClusterCreationProvider}
                wizardSteps={wizardSteps as any}
              />
            ),
          },
        ],
      },
      {
        path: "/login",
        element: <Login />,
      },

      {
        path: "/main",
        element: (
          <>
            <DashboardLayout />
          </>
        ),
        children: [
          {
            path: "service/add/:stepNumber",
            element: <AddWizardUrlMapping />,
          },
          {
            path: "dashboard",
            element: <Outlet />,
            children: [
              {
                index: true,
                element: <Navigate to="metrics" replace />,
              },
              {
                path: ":tabName",
                element: <Dashboard />,
              },
            ],
          },
          {
            path: "actions",
            element: <Actions serviceName="HDFS" />,
          },

          {
            path: "services",
            element: <Outlet />,
            children: [
              {
                path: ":serviceName",
                element: <Outlet />,
                children: [
                  {
                    path: ":tabName",
                    element: <ServiceDashboard />,
                  },
                ],
              },
              {
                path: "highAvailability/:componentName/enable/:stepNumber",
                element: <ServiceLoader />,
              },
              {
                path:":componentName/federation/:stepNumber",
                element: <ServiceLoader />,
              },
              {
                path: "highAvailability/:componentName/manage/:stepNumber",
                element: <ServiceLoader />,
              },
              
            ],
          },
          {
            path:"service",
            element:<Outlet/>,
            children:[{
              path:"reassign/:componentName/:stepNumber",
              element:<ServiceLoader />
            }]
          },
          {
            path: "hosts",
            element: <HostsList />,
          },
          {
            path: "hosts/component/:componentName",
            element: <HostsList />,
          },
          {
            path: "hosts/version/:versionName/:versionStatus",
            element: <HostsList />,
          },
          {
            path: "hosts/:hostname/:tab",
            element: <Hosts />,
          },
          {
            path: "host/add/:stepNumber",
            element: (
              <AddWizardUrlMapping />
            ),
          },
          {
            path: "alerts",
            element: <Alerts />,
          },
          {
            path: "alerts/:alertId",
            element: <AlertDefinitionDetails />,
          },
          {
            path: "admin",
            element: (
              <AdminRouteGuard>
                <Outlet />
              </AdminRouteGuard>
            ),
            children: [
              {
                index: true,
                element: <Navigate to="stack/services" replace />,
              },
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
                  <ProtectedRoute 
                    requireAuthorization="SERVICE.START_STOP"
                    redirectTo="/main/dashboard/metrics"
                  >
                    <ServiceAutoStart />
                  </ProtectedRoute>
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
                  <ProtectedRoute 
                    requireAuthorization="CLUSTER.TOGGLE_KERBEROS"
                    redirectTo="/main/dashboard/metrics"
                  >
                    <EnableKerberos />
                  </ProtectedRoute>
                ),
              },
              {
                path: "kerberos/enable/:stepNumber",
                element: (
                  <ProtectedRoute 
                    requireAuthorization="CLUSTER.TOGGLE_KERBEROS"
                    redirectTo="/main/dashboard/metrics"
                  >
                    <EnableKerberos />
                  </ProtectedRoute>
                ),
              }
            ],
          },
          {
            path: "views/:viewName/:viewVersion/:instanceName/*",
            element: <ViewDetails />,
          },
          {
            path: "view",
            element: <ViewsListPage />,
          },
          {
            path: "view/:viewName",
            element: <Navigate to="/main/view" replace />,
          },
        ],
      },
      {
        path: "main",
        element: (
          <>
            {/* <DashboardWrapper /> */}
            <Outlet />
          </>
        ),
        children: [
          {
            path: "admin",
            element: <Outlet />,
            children: [
              {
                path: "serviceAccounts",
                element: <ServiceAccounts />,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    path: "adminView",
    loader: () => {
      redirectToAdminView();
      return null;
    },
    element: <Spinner />, // This won't actually render as we'll redirect immediately
  },
  {
    path: "/login",
    element: <Login />,
  },
];

export default RoutesList;
