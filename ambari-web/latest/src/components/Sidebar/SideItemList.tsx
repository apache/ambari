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
import { Image } from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faTachometerAlt,
  faBriefcase,
  faTasksAlt,
  faBell,
  faWrench 
} from "@fortawesome/free-solid-svg-icons";
import AmbariLogo from "../../assets/img/ambari-logo.png"
import { isWindowsStack } from "../../Utils/stackMetadata";

enum SideItemLabels {
  LOGO = "logo",
  DASHBOARD = "dashboard",
  SERVICES = "services",
  HOSTS = "hosts",
  ALERTS = "alerts",
  CLUSTER_ADMIN = "cluster_admin",
  STACK_AND_VERSIONS = "Stack and Versions",
  SERVICE_ACCOUNTS = "Service Accounts",
  KERBEROS = "kerberos",
  SERVICE_AUTO_START = "Service Auto Start",
}
/**
 * Generate sidebar items with authorization checks
 * Based on Ember.js ui/app/views/main/admin.js authorization patterns
 */
const getSideItemList = (
  havePermissions: (auth: string) => boolean,
  isAuthorized: (auth: string) => boolean,
  supports: Record<string, boolean>,
  stackName = "",
): SideItem[] => {
  const adminChildren: SideItem[] = [];
  
  // Stack and Versions - Requires CLUSTER.VIEW_STACK_DETAILS OR CLUSTER.UPGRADE_DOWNGRADE_STACK (matches Ember.js)
  if (havePermissions('CLUSTER.VIEW_STACK_DETAILS, CLUSTER.UPGRADE_DOWNGRADE_STACK')) {
    adminChildren.push({
      id: SideItemLabels.STACK_AND_VERSIONS,
      icon: <></>,
      name: "Stack And Versions",
      path: "/main/admin/stack/services",
      children: [],
      style: {}
    });
  }
  
  // Service Accounts - Requires SERVICE.SET_SERVICE_USERS_GROUPS
  if (isAuthorized('SERVICE.SET_SERVICE_USERS_GROUPS')) {
    adminChildren.push({
      id: SideItemLabels.SERVICE_ACCOUNTS,
      icon: <></>,
      name: "Service Accounts",
      path: "/main/admin/serviceAccounts",
      children: [],
      style: {}
    });
  }
  
  // Kerberos - Requires CLUSTER.TOGGLE_KERBEROS
  if (
    !isWindowsStack(stackName)
    && supports.enableToggleKerberos
    && isAuthorized('CLUSTER.TOGGLE_KERBEROS')
  ) {
    adminChildren.push({
      id: SideItemLabels.KERBEROS,
      icon: <></>,
      name: "Kerberos",
      path: "/main/admin/kerberos",
      children: [],
      style: {}
    });
  }
  
  // Service Auto Start - Requires SERVICE.START_STOP authorization (matches ServiceAutoStart component)
  const canSeeAutoStart = isAuthorized('SERVICE.START_STOP, CLUSTER.MODIFY_CONFIGS')
    && isAuthorized('SERVICE.MANAGE_AUTO_START, CLUSTER.MANAGE_AUTO_START');
  if (supports.serviceAutoStart && canSeeAutoStart) {
    adminChildren.push({
      id: SideItemLabels.SERVICE_AUTO_START,
      icon: <></>,
      name: "Service Auto Start",
      path: "/main/admin/serviceAutoStart",
      children: [],
      style: {}
    });
  }

  const baseItems: SideItem[] = [
    {
      id: SideItemLabels.LOGO,
      icon: (
        <Image src={AmbariLogo} height={25} width={25} />
      ),
      name: <div className="fs-4">Ambari</div>,
      path: "/main/dashboard",
      children: [],
      style: { background: "#313d54", height: "60px" },
    },
    {
      id: SideItemLabels.DASHBOARD,
      icon: <FontAwesomeIcon icon={faTachometerAlt} height={15} width={15} />,
      name: "Dashboard",
      path: "/main/dashboard",
      children: [],
      style: {},
    },
    {
      id: SideItemLabels.SERVICES,
      icon: <FontAwesomeIcon icon={faBriefcase} height={15} width={15} />,
      name: "Services",
      path: "/main/dashboard",
      style: { position: "relative" },
      sideItems: true,
      children: [],
    },
    {
      id: SideItemLabels.HOSTS,
      icon: <FontAwesomeIcon icon={faTasksAlt} height={15} width={15} />,
      name: "Hosts",
      path: "/main/hosts",
      children: [],
      style: {}
    },
    {
      id: SideItemLabels.ALERTS,
      icon: <FontAwesomeIcon icon={faBell} height={15} width={15} />,
      name: "Alerts",
      path: "/main/alerts",
      children: [],
      style: {}
    }
  ];

  // Only add Cluster Admin if user has any admin permissions
  // This matches Ember.js ui/app/views/main/menu.js logic
  const hasAnyAdminPermissions = havePermissions(
    'CLUSTER.TOGGLE_KERBEROS, CLUSTER.MODIFY_CONFIGS, SERVICE.START_STOP, '
      + 'SERVICE.SET_SERVICE_USERS_GROUPS, CLUSTER.UPGRADE_DOWNGRADE_STACK, '
      + 'CLUSTER.VIEW_STACK_DETAILS',
  );

  if (hasAnyAdminPermissions && adminChildren.length > 0) {
    baseItems.push({
      id: SideItemLabels.CLUSTER_ADMIN,
      icon: <FontAwesomeIcon icon={faWrench} height={15} width={15} />,
      path: "/main/admin",
      name: "Cluster Admin",
      children: adminChildren,
      sideItems: true,
      style: {}
    });
  }

  return baseItems;
};

// Default export for backward compatibility
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
    path: "/main/dashboard",
    children: [],
    style: {},
  },
  {
    id: SideItemLabels.SERVICES,
    icon: <FontAwesomeIcon icon={faBriefcase} height={15} width={15} />,
    name: "Services",
    path: "/main/dashboard",
    style: { position: "relative" },
    sideItems: true,
    children: [],
  },
  {
    id: SideItemLabels.HOSTS,
    icon: <FontAwesomeIcon icon={faTasksAlt} height={15} width={15} />,
    name: "Hosts",
    path: "/main/hosts",
    children: [],
    style: {}
  },
  {
    id: SideItemLabels.ALERTS,
    icon: <FontAwesomeIcon icon={faBell} height={15} width={15} />,
    name: "Alerts",
    path: "/main/alerts",
    children: [],
    style: {}
  },
  {
    id: SideItemLabels.CLUSTER_ADMIN,
    icon: <FontAwesomeIcon icon={faWrench} height={15} width={15} />,
    path: "/main/admin",
    name: "Cluster Admin",
    children: [
      {
        id: SideItemLabels.STACK_AND_VERSIONS,
        icon: <></>,
        name: "Stack And Versions",
        path: "/main/admin/stack/services",
        children: [],
        style: {}
      },
      {
        id: SideItemLabels.SERVICE_ACCOUNTS,
        icon: <></>,
        name: "Service Accounts",
        path: "/main/admin/serviceAccounts",
        children: [],
        style: {}
      },
      {
        id: SideItemLabels.KERBEROS,
        icon: <></>,
        name: "Kerberos",
        path: "/main/admin/kerberos",
        children: [],
        style: {}
      },
      {
        id: SideItemLabels.SERVICE_AUTO_START,
        icon: <></>,
        name: "Service Auto Start",
        path: "/main/admin/serviceAutoStart",
        children: [],
        style: {}
      }
    ],
    sideItems: true,
    style: {}
  },
];

// const SideItemListComponent = () => {
//   return (
//     <nav className="sidebar-nav">
//       <ul>
//         {SideItemList.map((item, index) => (
//           <li key={index}>
//             <Link to="" className="sidebar-item">
//               {item.icon}
//               {item.name}
//                 <span className="dropdown-icon">{item.dropDownIcon}</span>
//             </Link>
//           </li>
//         ))}
//       </ul>
//     </nav>
//   );
// };

export { SideItemList, SideItemLabels, getSideItemList };
