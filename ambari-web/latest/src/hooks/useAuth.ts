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
import { useUserContext } from '../store/UserContext';

/**
 * Custom hook for accessing user authentication and authorization data
 */
export const useAuth = () => {
  const context = useUserContext();
  
  return {
    // User data
    user: context.user,
    isAuthenticated: context.isAuthenticated,
    isLoading: context.isLoading,
    
    // Authorization data
    authorizations: context.authorizations,
    privileges: context.privileges,
    clusterPrivileges: context.clusterPrivileges,
    viewPrivileges: context.viewPrivileges,
    
    // Helper methods
    havePermissions: context.havePermissions,
    hasAuthorization: context.hasAuthorization,
    hasPrivilege: context.hasPrivilege,
    isAdmin: context.isAdmin,
    isOperator: context.isOperator,
    isClusterUser: context.isClusterUser,
    
    // Actions
    login: context.login,
    logout: context.logout,
    refreshUserData: context.refreshUserData
  };
};

/**
 * Hook for checking specific authorizations
 */
export const useAuthorization = (authId: string) => {
  const { hasAuthorization } = useAuth();
  return hasAuthorization(authId);
};

/**
 * Hook for checking specific privileges
 */
export const usePrivilege = (permissionName: string, clusterName?: string) => {
  const { hasPrivilege } = useAuth();
  return hasPrivilege(permissionName, clusterName);
};

/**
 * Hook for checking admin status
 */
export const useIsAdmin = () => {
  const { isAdmin } = useAuth();
  return isAdmin();
};

/**
 * Hook for checking operator status
 */
export const useIsOperator = () => {
  const { isOperator } = useAuth();
  return isOperator();
};

/**
 * Hook for checking cluster user status
 */
export const useIsClusterUser = () => {
  const { isClusterUser } = useAuth();
  return isClusterUser();
};

export default useAuth;
