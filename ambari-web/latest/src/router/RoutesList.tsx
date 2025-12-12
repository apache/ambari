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
import StackAndVersions from "../screens/ClusterAdmin/StackAndVersions/StackAndVersions";
import { ComponentInProgress } from "../Utils/ComponentInProgress";
import HostsList from "../screens/Hosts/HostsList";
import { Hosts } from "../screens/Hosts";

const RoutesList: RouteObject[] = [
  {
    path: "/",
    element: (
      <>
        <ComponentInProgress />
        <Outlet />
      </>
    ),
    children: [
      {
        path: "installer",
        element: <ComponentInProgress />,
        children: [
          {
            path: ":stepNumber",
            element: <ComponentInProgress />,
          },
        ],
      },
      {
        path: "/login",
        element: <ComponentInProgress />,
      },
      {
        path: "/main",
        element: (
          <>
            <ComponentInProgress />
          </>
        ),
        children: [
          {
            path: "dashboard",
            element: <Outlet />,
            children: [
              {
                index: true,
                element: <Navigate to="metrics" replace />,
              },
              {
                path: "metrics",
                element: <Outlet />,
              },
            ],
          },
          //TODO: after services page is implemented
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
                    element: <ComponentInProgress />,
                  },
                ],
              },
            ],
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
            element: <ComponentInProgress />,
          },
          {
            path: "alerts",
            element: <ComponentInProgress />,
          },
          {
            path: "alerts/:alertId",
            element: <ComponentInProgress />,
          },
          {
            path: "admin",
            element: <Outlet />,
            children: [
              {
                path: "stack/:tabName",
                element: <StackAndVersions />,
              },
              {
                path: "serviceAutoStart",
                element: <ComponentInProgress />,
              },
              {
                path: "serviceAccounts",
                element: <ComponentInProgress />,
              },
              {
                path: "kerberos",
                element: <ComponentInProgress />,
              },
            ],
          },
          {
            path: "views/:viewName/:viewVersion/:instanceName/*",
            element: <ComponentInProgress />,
          },
          {
            path: "views",
            element: <ComponentInProgress />,
          },
          {
            path: "service/add/:stepNumber",
            element: <ComponentInProgress />,
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
                element: <ComponentInProgress />,
              },
            ],
          },
        ],
      },
    ],
  },
];

export default RoutesList;
