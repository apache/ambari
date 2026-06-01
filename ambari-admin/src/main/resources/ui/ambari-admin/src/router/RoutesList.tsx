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
import ClusterInformation from "../screens/ClusterManagement/ClusterInformation";
import RemoteClusters from "../screens/ClusterManagement/RemoteClusters";
import RegisterRemoteCluster from "../screens/ClusterManagement/RemoteClusters/RegisterRemoteCluster";
import EditRemoteCluster from "../screens/ClusterManagement/RemoteClusters/EditRemoteCluster";
import Users from "../screens/Users";
import EditGroup from "../screens/Users/EditGroup";
import EditUser from "../screens/Users/EditUser";
import Views from "../screens/Views";
import EditInstance from "../screens/Views/EditInstance";
import CreateShortUrl from "../screens/Views/CreateShortUrl";
import Dashboard from "../screens/ClusterManagement/Dasboard";
import { Redirect } from "react-router-dom";
import { Register, VersionsList } from "../screens/ClusterManagement/StackVersions";



export default [
  {
    path: "/main/dashboard",
    exact: true,
    Element: () => <Dashboard />,
    name: "Home",
  },
  {
    path: "/dashboard",
    exact: true,
    Element: () => <Dashboard />,
    name: "Dashboard",
  },
  {
    path: "/clusterInformation",
    exact: true,
    Element: () => <ClusterInformation />,
    name: "Cluster Information",
  },
  {
    path: "/stackVersions/create",
    exact: true,
    Element: () => <Register readOnly={false} />,
    name: "Stack Versions",
  },
  {
    path: "/stackVersions/:stack/:version/edit",
    exact: true,
    Element: () => <Register readOnly />,
    name: "Stack Versions",
  },
  {
    path: "/stackVersions",
    exact: true,
    Element: () => <VersionsList />,
    name: "Stack Versions",
  },
  {
    path: "/remoteClusters/:clusterName/edit",
    exact: true,
    Element: () => <EditRemoteCluster />,
    name: "Edit Remote Cluster",
  },
  {
    path: "/remoteClusters/create",
    exact: true,
    Element: () => <RegisterRemoteCluster />,
    name: "Register Remote Cluster",
  },
  {
    path: "/remoteClusters",
    exact: true,
    Element: () => <RemoteClusters />,
    name: "Remote Clusters",
  },
  {
    path: "/userManagement",
    exact: true,
    Element: () => <Users />,
    name: "UserManagement",
  },
  {
    path: "/users/:userName/edit",
    exact: true,
    Element: () => <EditUser />,
    name: "Users",
  },
  {
    path: "/groups/:groupName/edit",
    exact: true,
    Element: () => <EditGroup />,
    name: "Groups",
  },
  {
    path:`/views/:viewName/versions/:version/instances/:instanceName/edit`,
    exact:true,
    Element:()=><EditInstance/>,
    name:"Views",
  },
  {
    path: "/views",
    exact: true,
    Element: () => <Views />,
    name: "Views",
  },
  {
    path: '/urls/link/:view_name/:version/:instance_name',
    exact: true,
    Element: () => <CreateShortUrl/>,
    name: 'Views'
  },
  {
    path: '/',
    exact: true,
    Element: () => <Redirect to="/clusterInformation" />,
  },
];

