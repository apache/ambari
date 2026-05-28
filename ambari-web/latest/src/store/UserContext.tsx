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

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { User, UserContextType, Authorization, Privilege} from '../types/auth';
import LoginApi from '../api/loginApi';
import { db } from '../Utils/db';
import { isString } from 'lodash';
import { AppContext } from './context';

const UserContext = createContext<UserContextType | undefined>(undefined);

export const useUserContext = () => {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUserContext must be used within a UserProvider');
  }
  return context;
};

interface UserProviderProps {
  children: React.ReactNode;
}

export const UserProvider: React.FC<UserProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [authorizations, setAuthorizations] = useState<Authorization[]>([]);
  const [privileges, setPrivileges] = useState<Privilege[]>([]);
  const [clusterPrivileges, setClusterPrivileges] = useState<Record<string, string[]>>({});
  const [viewPrivileges, setViewPrivileges] = useState<Record<string, { privileges: string[]; version: string; view_name: string }>>({});
  const [loginError, setLoginError] = useState<string | null>(null);
  
  // Access AppContext for upgrade state - this will be available after AppProvider wraps UserProvider
  const appContext = useContext(AppContext);
  // Parse privileges into cluster and view privileges
  const parsePrivileges = useCallback((privilegesList: Privilege[]) => {
    const clusters: Record<string, string[]> = {};
    const views: Record<string, { privileges: string[]; version: string; view_name: string }> = {};

    privilegesList.forEach((privilege) => {
      if (privilege.type === 'CLUSTER' && privilege.cluster_name) {
        if (!clusters[privilege.cluster_name]) {
          clusters[privilege.cluster_name] = [];
        }
        clusters[privilege.cluster_name].push(privilege.permission_label);
      } else if (privilege.type === 'VIEW' && privilege.instance_name) {
        if (!views[privilege.instance_name]) {
          views[privilege.instance_name] = {
            privileges: [],
            version: privilege.version || '',
            view_name: privilege.view_name || ''
          };
        }
        views[privilege.instance_name].privileges.push(privilege.permission_label);
      }
    });

    setClusterPrivileges(clusters);
    setViewPrivileges(views);
  }, []);

  // Helper methods - implementing Ember.js App.havePermissions and App.isAuthorized logic
  const havePermissions = useCallback((authRoles: string): boolean => {
    if (!authorizations.length) {
      return false;
    }

    const authRolesList = authRoles.split(',').map(role => role.trim());
    
    // When Upgrade running(not suspended) only operations related to upgrade should be allowed
    // This matches the Ember.js logic in ui/app/app.js
    const upgradeState = appContext?.upgradeState || 'NOT_REQUIRED';
    const upgradeSuspended = appContext?.upgradeSuspend || false;
    
    // Check if upgrade is blocking operations
    if ((!upgradeSuspended &&
         !authRolesList.includes('CLUSTER.UPGRADE_DOWNGRADE_STACK') &&
         !authRolesList.includes('CLUSTER.MANAGE_USER_PERSISTED_DATA')) &&
        // TODO: Add supports.opsDuringRollingUpgrade check when available
        !['NOT_REQUIRED', 'COMPLETED'].includes(upgradeState)) {
      return false;
    }
    
    return authRolesList.some(auth => 
      authorizations.some(authorization => authorization.authorization_id === auth)
    );
  }, [authorizations, appContext?.upgradeState, appContext?.upgradeSuspend]);

  const hasAuthorization = useCallback((authId: string): boolean => {
    // This implements App.isAuthorized logic: havePermissions + wizard check
    // For now, we'll use havePermissions. Wizard check can be added later when needed
    
    // Special case: Cluster operators should have HOST.ADD_DELETE_COMPONENTS permission
    // This matches Ember.js behavior where cluster operators can manage components
    if (authId === 'HOST.ADD_DELETE_COMPONENTS') {
      // Check if user has explicit authorization OR is a cluster operator
      return havePermissions(authId) || havePermissions('CLUSTER.ADMINISTRATOR');
    }
    
    return havePermissions(authId);
  }, [havePermissions]);

  const hasPrivilege = useCallback((permissionName: string, clusterName?: string): boolean => {
    if (clusterName) {
      return clusterPrivileges[clusterName]?.includes(permissionName) || false;
    }
    // Check across all clusters if no specific cluster is provided
    return Object.values(clusterPrivileges).some(privs => privs.includes(permissionName));
  }, [clusterPrivileges]);

  const isAdmin = useCallback((): boolean => {
    const hasAmbariAdmin = privileges.some(privilege => 
      privilege.permission_name === 'AMBARI.ADMINISTRATOR'
    );
    
    if (hasAmbariAdmin) {
      return true;
    }
    
    // Check if user has CLUSTER.ADMINISTRATOR permission for any cluster
    return hasAuthorization('CLUSTER.ADMINISTRATOR');
  }, [privileges, hasAuthorization]);

  const isOperator = useCallback((): boolean => {
    // Based on Ember.js router.js loginGetClustersSuccessCallback logic:
    // isOperator is set to true when user has CLUSTER.ADMINISTRATOR permission
    // This matches the exact logic in router.js lines 670-675
    return hasAuthorization('CLUSTER.ADMINISTRATOR');
  }, [hasAuthorization]);

  const isClusterUser = useCallback((): boolean => {
    // Based on Ember.js router.js loginGetClustersSuccessCallback logic:
    // isClusterUser is set to false when user has CLUSTER.ADMINISTRATOR permission
    // This matches the exact logic in router.js lines 670-675
    return !hasAuthorization('CLUSTER.ADMINISTRATOR');
  }, [hasAuthorization]);

  // Additional role helper - isClusterOperator (same as isOperator but more explicit naming)
  const isClusterOperator = useCallback((): boolean => {
    // This follows the users_mapper.js logic: user has CLUSTER.ADMINISTRATOR but not AMBARI.ADMINISTRATOR
    const hasAmbariAdmin = privileges.some(privilege => 
      privilege.permission_name === 'AMBARI.ADMINISTRATOR'
    );
    return hasAuthorization('CLUSTER.ADMINISTRATOR') && !hasAmbariAdmin;
  }, [hasAuthorization, privileges]);

  // Load user authorizations
  const loadAuthorizations = useCallback(async (loginName: string): Promise<Authorization[]> => {
    try {
      const response = await LoginApi.loadAuthorizationsCallback({
        usr: '',
        loginName: encodeURIComponent(loginName)
      });
      
      const authList = response.data.items.map((item: any) => item.AuthorizationInfo);
      setAuthorizations(authList);
      
      // Store in database similar to Ember implementation
      const authIds = authList.map((auth: Authorization) => auth.authorization_id);
      db.set('app', 'auth', authIds);
      
      return authList;
    } catch (error) {
      console.error('Failed to load authorizations:', error);
      return [];
    }
  }, []);

  // Load user data and privileges
  const loadUserData = useCallback(async (loginName: string): Promise<User | null> => {
    try {
      const response = await LoginApi.handleSuccessfulLogin({
        usr: '',
        loginName: encodeURIComponent(loginName)
      });

      const userData = response.data.Users;
      setUser(userData);

      // Load privileges if they exist in the response
      if (response.data.privileges) {
        // Map the privileges from the API response structure
        const mappedPrivileges = response.data.privileges.map((item: any) => item.PrivilegeInfo);
        setPrivileges(mappedPrivileges);
        parsePrivileges(mappedPrivileges);
      }

      return userData;
    } catch (error) {
      console.error('Failed to load user data:', error);
      return null;
    }
  }, [parsePrivileges]);

  // Initialize user from stored data
  const initializeUser = useCallback(async (): Promise<boolean> => {
    try {
      setIsLoading(true);
      
      const ambariLocalData = db.getItem('ambari');
      if (!ambariLocalData) {
        setIsLoading(false);
        return false;
      }

      let parsedData: any = {};
      try {
        parsedData = JSON.parse(ambariLocalData);
        if (isString(parsedData)) {
          parsedData = JSON.parse(parsedData);
        }
      } catch (err) {
        console.error('Error parsing ambari data:', err);
        setIsLoading(false);
        return false;
      }

      const loginName = parsedData?.app?.loginName;
      if (!loginName) {
        setIsLoading(false);
        return false;
      }

      // Load user data and authorizations
      const [userData] = await Promise.all([
        loadUserData(decodeURIComponent(loginName)),
        loadAuthorizations(decodeURIComponent(loginName))
      ]);

      if (userData) {
        setIsAuthenticated(true);
        setIsLoading(false);
        return true;
      }

      setIsLoading(false);
      return false;
    } catch (error) {
      console.error('Failed to initialize user:', error);
      setIsLoading(false);
      return false;
    }
  }, [loadUserData, loadAuthorizations]);

  // Login method
  const login = useCallback(async (username: string, password: string): Promise<boolean> => {
    try {
      setIsLoading(true);
      
      // Authenticate user
      await LoginApi.authenticate(username, password);
      
      // Store initial data
      const initialAmbariLsData = {
        app: {
          loginName: encodeURIComponent(username),
          authenticated: true
        }
      };
      
      db.setItem('ambari', JSON.stringify(initialAmbariLsData));
      
      // Load user data and authorizations
      const [userData] = await Promise.all([
        loadUserData(username),
        loadAuthorizations(username)
      ]);

      if (userData) {
        // Update stored data with user info
        const updatedData = {
          app: {
            loginName: encodeURIComponent(username),
            authenticated: true,
            user: userData
          }
        };
        db.setItem('ambari', JSON.stringify(updatedData));
        
        setIsAuthenticated(true);
        setIsLoading(false);
        setLoginError(null);
        return true;
      }
      setIsLoading(false);
      return false;
    } catch (error:any) {
      setLoginError(error?.response?.data?.message || 'Login failed');
      setIsLoading(false);
      return false;
    }
  }, [loadUserData, loadAuthorizations]);

  // Logout method
  const logout = useCallback(async (): Promise<void> => {
    try {
      await LoginApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      // Clear all user data
      setUser(null);
      setIsAuthenticated(false);
      setAuthorizations([]);
      setPrivileges([]);
      setClusterPrivileges({});
      setViewPrivileges({});
      db.cleanUp();
    }
  }, []);

  // Refresh user data
  const refreshUserData = useCallback(async (): Promise<void> => {
    if (user?.user_name) {
      await Promise.all([
        loadUserData(user.user_name),
        loadAuthorizations(user.user_name)
      ]);
    }
  }, [user, loadUserData, loadAuthorizations]);

  // Initialize on mount
  useEffect(() => {
    initializeUser();
  }, [initializeUser]);

  // Update parsed privileges when privileges change
  useEffect(() => {
    parsePrivileges(privileges);
  }, [privileges, parsePrivileges]);

  const contextValue: UserContextType = {
    user,
    isAuthenticated,
    isLoading,
    authorizations,
    privileges,
    clusterPrivileges,
    viewPrivileges,
    havePermissions,
    hasAuthorization,
    hasPrivilege,
    isAdmin,
    isOperator,
    isClusterUser,
    isClusterOperator,
    login,
    logout,
    refreshUserData,
    loginError
  };

  return (
    <UserContext.Provider value={contextValue}>
      {children}
    </UserContext.Provider>
  );
};

export default UserContext;
