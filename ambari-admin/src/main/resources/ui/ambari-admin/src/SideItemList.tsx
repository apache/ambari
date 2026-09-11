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
import SideItem from "./SideItem";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faTachometerAlt,
  faCloud,
  faUsers,
  faTh,
} from "@fortawesome/free-solid-svg-icons";
import { Image } from "react-bootstrap";
import AmbariLogo from "./assets/img/ambari-logo.png";

export enum SideItemLabels {
  LOGO = "logo",
  DASHBOARD = "dashboard",
  CLUSTERMANAGEMENT = "Cluster Management",
  CLUSTERINFORMATION = "Cluster Details",
  CLUSTEROVERVIEW = "Cluster Overview",
  CREATECLUSTER = "Create Cluster",
  HOSTRESOURCES = "Host Resources",
  CLUSTERPERMISSIONS = "Cluster Permissions",
  VERSIONS = "Versions",
  REMOTECLUSTERS = "Remote Clusters",
  USERS = "Users",
  VIEWS = "Views",
}
const getSideItemList = (_clusterExists: boolean): SideItem[] => SideItemList;

const SideItemList: SideItem[] = [
  {
    id: SideItemLabels.LOGO,
    icon: (
      <Image src={AmbariLogo} height={25} width={25} />
    ),
    name: <div className="fs-4">Ambari</div>,
    path: "/dashboard",
    children: [],
    style: { background: "#313d54", height: "60px" },
  },
  {
    id: SideItemLabels.DASHBOARD,
    icon: <FontAwesomeIcon icon={faTachometerAlt} height={15} width={15} />,
    name: "Dashboard",
    path: "/dashboard",
    children: [],
  },
  {
    id: SideItemLabels.CLUSTERMANAGEMENT,
    icon: <FontAwesomeIcon icon={faCloud} height={15} width={15} />,
    name: "Cluster Management",
    children: [
      {
        id: SideItemLabels.CLUSTEROVERVIEW,
        icon: null,
        name: "Cluster Overview",
        path: "/clusters",
        children: [],
      },
      { id: SideItemLabels.CREATECLUSTER, icon: null, name: "Create Cluster", path: "/clusters/create", children: [] },
      { id: SideItemLabels.HOSTRESOURCES, icon: null, name: "Host Resources", path: "/hostResources", children: [] },
      { id: SideItemLabels.CLUSTERPERMISSIONS, icon: null, name: "Cluster Permissions", path: "/clusterPermissions", children: [] },
      {
        id: SideItemLabels.VERSIONS,
        icon: null,
        name: "Versions & Repositories",
        path: "/stackVersions",
        children: [],
      },
      {
        id: SideItemLabels.REMOTECLUSTERS,
        icon: null,
        name: "Remote Clusters",
        path: "/remoteClusters",
        children: [],
      },
    ],
  },
  {
    id: SideItemLabels.USERS,
    icon: <FontAwesomeIcon icon={faUsers} height={15} width={15} />,
    name: "Users",
    path: "/userManagement?tab=users",
    children: [],
  },
  {
    id: SideItemLabels.VIEWS,
    icon: <FontAwesomeIcon icon={faTh} height={15} width={15} />,
    name: "Views",
    path: "/views",
    children: [],
  },
];

export default getSideItemList;