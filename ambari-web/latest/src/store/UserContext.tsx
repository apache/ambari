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

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { isString } from "lodash";
import LoginApi, { LoginMessage } from "../api/loginApi";
import { db } from "../Utils/db";
import {
  resetExternalRedirectCount,
  SESSION_EXPIRED_EVENT,
} from "../Utils/authNavigation";
import { Authorization, Privilege, User, UserContextType } from "../types/auth";

const UserContext = createContext<UserContextType | undefined>(undefined);

export const useUserContext = () => {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error("useUserContext must be used within a UserProvider");
  }
  return context;
};

function storedLoginName(): string | null {
  const value = db.getItem("ambari");
  if (!value) {
    return null;
  }

  try {
    let parsed: any = JSON.parse(value);
    if (isString(parsed)) {
      parsed = JSON.parse(parsed);
    }
    return parsed?.app?.loginName ? decodeURIComponent(parsed.app.loginName) : null;
  } catch {
    return null;
  }
}

export const UserProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [authorizations, setAuthorizations] = useState<Authorization[]>([]);
  const [privileges, setPrivileges] = useState<Privilege[]>([]);
  const [clusterPrivileges, setClusterPrivileges] = useState<Record<string, string[]>>({});
  const [viewPrivileges, setViewPrivileges] = useState<Record<string, {
    privileges: string[];
    version: string;
    view_name: string;
  }>>({});
  const [loginError, setLoginError] = useState<string | null>(null);
  const [loginMessage, setLoginMessage] = useState<LoginMessage | null>(null);
  const [sessionError, setSessionError] = useState<string | null>(null);

  const parsePrivileges = useCallback((items: Privilege[]) => {
    const clusters: Record<string, string[]> = {};
    const views: Record<string, { privileges: string[]; version: string; view_name: string }> = {};

    items.forEach((privilege) => {
      if (privilege.type === "CLUSTER" && privilege.cluster_name) {
        clusters[privilege.cluster_name] ??= [];
        clusters[privilege.cluster_name].push(privilege.permission_label);
      } else if (privilege.type === "VIEW" && privilege.instance_name) {
        views[privilege.instance_name] ??= {
          privileges: [],
          version: privilege.version || "",
          view_name: privilege.view_name || "",
        };
        views[privilege.instance_name].privileges.push(privilege.permission_label);
      }
    });

    setClusterPrivileges(clusters);
    setViewPrivileges(views);
  }, []);

  const clearSession = useCallback(() => {
    setUser(null);
    setIsAuthenticated(false);
    setAuthorizations([]);
    setPrivileges([]);
    setClusterPrivileges({});
    setViewPrivileges({});
    setLoginMessage(null);
    db.clearSession();
  }, []);

  const loadSession = useCallback(async (loginName: string): Promise<User> => {
    const [userResponse, authorizationResponse, message] = await Promise.all([
      LoginApi.handleSuccessfulLogin({ usr: "", loginName }),
      LoginApi.loadAuthorizationsCallback({ usr: "", loginName }),
      LoginApi.loadLoginMessage(),
    ]);
    const userData = userResponse.data.Users as User | undefined;
    if (!userData) {
      throw new Error("Ambari did not return the authenticated user");
    }

    const mappedPrivileges = (userResponse.data.privileges || []).map(
      (item: any) => item.PrivilegeInfo,
    );
    const mappedAuthorizations = (authorizationResponse.data.items || []).map(
      (item: any) => item.AuthorizationInfo,
    );

    setUser(userData);
    setPrivileges(mappedPrivileges);
    setAuthorizations(mappedAuthorizations);
    parsePrivileges(mappedPrivileges);
    const authorizationIds = mappedAuthorizations.map(
      (authorization: Authorization) => authorization.authorization_id,
    );
    db.setSession(userData.user_name, userData, authorizationIds);
    setLoginMessage(message);
    setIsAuthenticated(true);
    resetExternalRedirectCount();
    return userData;
  }, [parsePrivileges]);

  const initializeUser = useCallback(async () => {
    setIsLoading(true);
    setSessionError(null);
    try {
      const response = await LoginApi.probeSession();
      const responseUser = response.headers?.user as string | undefined;
      const loginName = responseUser || storedLoginName();
      if (!loginName) {
        clearSession();
        return;
      }

      await loadSession(loginName);
    } catch (error: any) {
      clearSession();
      const status = error?.response?.status;
      if (status !== 401 && status !== 403) {
        setSessionError(
          error?.response?.data?.message || "Ambari Server could not validate the current session.",
        );
      }
    } finally {
      setIsLoading(false);
    }
  }, [clearSession, loadSession]);

  const login = useCallback(async (username: string, password: string): Promise<boolean> => {
    setIsLoading(true);
    setLoginError(null);
    try {
      await LoginApi.authenticate(username, password);
      await loadSession(username);
      return true;
    } catch (error: any) {
      clearSession();
      const status = error?.response?.status;
      if (status === 403) {
        setLoginError(error?.response?.data?.message || "Invalid username or password.");
      } else if (status === 500) {
        setLoginError(error?.response?.data?.message || "Ambari Server could not complete the login request.");
      } else {
        setLoginError(error?.response?.data?.message || "Unable to sign in to Ambari.");
      }
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [clearSession, loadSession]);

  const logout = useCallback(async () => {
    clearSession();
    db.cleanUp();
    sessionStorage.clear();
    localStorage.removeItem("lastVisitedURL");
    void LoginApi.logout().catch(() => {
      // Client cleanup and navigation must not depend on server logoff success.
    });
  }, [clearSession]);

  const refreshUserData = useCallback(async () => {
    if (user?.user_name) {
      await loadSession(user.user_name);
    }
  }, [loadSession, user]);

  const havePermissions = useCallback((authorizationIds: string): boolean => {
    const requested = authorizationIds.split(",").map((value) => value.trim());
    return requested.some((id) => authorizations.some(
      (authorization) => authorization.authorization_id === id,
    ));
  }, [authorizations]);

  const hasAuthorization = useCallback((authorizationId: string): boolean => {
    if (authorizationId === "HOST.ADD_DELETE_COMPONENTS") {
      return havePermissions(authorizationId) || havePermissions("CLUSTER.ADMINISTRATOR");
    }
    return havePermissions(authorizationId);
  }, [havePermissions]);

  const hasPrivilege = useCallback((permissionName: string, clusterName?: string): boolean => {
    if (clusterName) {
      return clusterPrivileges[clusterName]?.includes(permissionName) || false;
    }
    return Object.values(clusterPrivileges).some((items) => items.includes(permissionName));
  }, [clusterPrivileges]);

  const isAdmin = useCallback(() => privileges.some(
    (privilege) => privilege.permission_name === "AMBARI.ADMINISTRATOR",
  ) || hasAuthorization("CLUSTER.ADMINISTRATOR"), [hasAuthorization, privileges]);
  const isOperator = useCallback(
    () => hasAuthorization("CLUSTER.ADMINISTRATOR"),
    [hasAuthorization],
  );
  const isClusterUser = useCallback(
    () => privileges.length === 1 && privileges[0].permission_name === "CLUSTER.USER",
    [privileges],
  );
  const isClusterOperator = useCallback(() => isOperator() && !privileges.some(
    (privilege) => privilege.permission_name === "AMBARI.ADMINISTRATOR",
  ), [isOperator, privileges]);

  useEffect(() => {
    const expireSession = () => {
      clearSession();
      setSessionError(null);
      setIsLoading(false);
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, expireSession);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expireSession);
  }, [clearSession]);

  useEffect(() => {
    void initializeUser();
  }, [initializeUser]);

  return (
    <UserContext.Provider value={{
      user,
      isAuthenticated,
      isLoading,
      sessionError,
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
      retrySession: initializeUser,
      loginError,
      loginMessage,
      acknowledgeLoginMessage: () => setLoginMessage(null),
    }}>
      {children}
    </UserContext.Provider>
  );
};

export default UserContext;
