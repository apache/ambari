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

import React, { useContext, ReactNode } from 'react';
import { Alert } from 'react-bootstrap';
import './PermissionGuard.scss';

// Define permission types
export enum Permission {
  VIEW_CLUSTERS = 'VIEW_CLUSTERS',
  MANAGE_CLUSTERS = 'MANAGE_CLUSTERS',
  VIEW_USERS = 'VIEW_USERS',
  MANAGE_USERS = 'MANAGE_USERS',
  VIEW_STACK_VERSIONS = 'VIEW_STACK_VERSIONS',
  MANAGE_STACK_VERSIONS = 'MANAGE_STACK_VERSIONS',
  VIEW_VIEWS = 'VIEW_VIEWS',
  MANAGE_VIEWS = 'MANAGE_VIEWS',
  ADMINISTRATOR = 'ADMINISTRATOR',
}

// Define user roles with associated permissions
export const RolePermissions = {
  Administrator: Object.values(Permission),
  Operator: [
    Permission.VIEW_CLUSTERS,
    Permission.VIEW_USERS,
    Permission.VIEW_STACK_VERSIONS,
    Permission.VIEW_VIEWS,
  ],
  User: [
    Permission.VIEW_CLUSTERS,
    Permission.VIEW_VIEWS,
  ],
};

// Create a context for user permissions
export const PermissionContext = React.createContext<{
  userPermissions: Permission[];
  hasPermission: (permission: Permission) => boolean;
}>({
  userPermissions: [],
  hasPermission: () => false,
});

// Create a provider component for the permission context
export const PermissionProvider: React.FC<{
  children: ReactNode;
  userRoles?: string[];
}> = ({ children, userRoles = [] }) => {
  // Determine user permissions based on roles
  const userPermissions = React.useMemo(() => {
    const permissions = new Set<Permission>();
    
    userRoles.forEach(role => {
      const rolePerms = (RolePermissions as Record<string, Permission[]>)[role];
      if (rolePerms) {
        rolePerms.forEach(perm => permissions.add(perm));
      }
    });
    
    // If user has ADMINISTRATOR permission, they have all permissions
    if (permissions.has(Permission.ADMINISTRATOR)) {
      Object.values(Permission).forEach(perm => permissions.add(perm));
    }
    
    return Array.from(permissions);
  }, [userRoles]);
  
  // Function to check if user has a specific permission
  const hasPermission = (permission: Permission) => {
    return userPermissions.includes(permission);
  };
  
  return (
    <PermissionContext.Provider value={{ userPermissions, hasPermission }}>
      {children}
    </PermissionContext.Provider>
  );
};

// Hook to use permissions
export const usePermissions = () => {
  return useContext(PermissionContext);
};

interface PermissionGuardProps {
  requiredPermission: Permission;
  children: ReactNode;
  fallback?: ReactNode;
  showAlert?: boolean;
}

const PermissionGuard: React.FC<PermissionGuardProps> = ({
  requiredPermission,
  children,
  fallback,
  showAlert = true,
}) => {
  const { hasPermission } = usePermissions();
  
  if (hasPermission(requiredPermission)) {
    return <>{children}</>;
  }
  
  if (fallback) {
    return <>{fallback}</>;
  }
  
  if (showAlert) {
    return (
      <Alert variant="warning" className="permission-alert">
        You do not have permission to access this feature.
        Required permission: {requiredPermission}
      </Alert>
    );
  }
  
  return null;
};

export default PermissionGuard;
