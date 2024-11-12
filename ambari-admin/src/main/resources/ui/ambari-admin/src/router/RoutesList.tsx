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
import { Redirect } from "react-router-dom";
import Users from "../screens/Users";
import WIP from "../components/WIP";



export default [
  {
    path: "/main/dashboard",
    exact: true,
    Element: () => <WIP />,
    name: "Home",
  },
  {
    path: "/dashboard",
    exact: true,
    Element: () => <WIP />,
    name: "Dashboard",
  },
  {
    path: "/clusterInformation",
    exact: true,
    Element: () => <WIP />,
    name: "Cluster Information",
  },
  {
    path: "/stackVersions/create",
    exact: true,
    Element: () => <WIP />,
    name: "Stack Versions",
  },
  {
    path: "/stackVersions/:stack/:version/edit",
    exact: true,
    Element: () => <WIP />,
    name: "Stack Versions",
  },
  {
    path: "/stackVersions",
    exact: true,
    Element: () => <WIP />,
    name: "Stack Versions",
  },
  {
    path: "/remoteClusters/:clusterName/edit",
    exact: true,
    Element: () => <WIP />,
    name: "Edit Remote Cluster",
  },
  {
    path: "/remoteClusters/create",
    exact: true,
    Element: () => <WIP />,
    name: "Register Remote Cluster",
  },
  {
    path: "/remoteClusters",
    exact: true,
    Element: () => <WIP />,
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
    Element: () => <WIP />,
    name: "Users",
  },
  {
    path: "/groups/:groupName/edit",
    exact: true,
    Element: () => <WIP />,
    name: "Groups",
  },
  {
    path:`/views/:viewName/versions/:version/instances/:instanceName/edit`,
    exact:true,
    Element:()=><WIP/>,
    name:"Views",
  },
  {
    path: "/views",
    exact: true,
    Element: () => <WIP />,
    name: "Views",
  },
  {
    path: '/urls/link/:view_name/:version/:instance_name',
    exact: true,
    Element: () => <WIP/>,
    name: 'Views'
  },
  {
    path: '/',
    exact: true,
    Element: () => <Redirect to="/clusterInformation" />,
  },
];

