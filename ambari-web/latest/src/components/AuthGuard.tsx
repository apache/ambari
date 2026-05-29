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

import React from "react";
import { useAuth } from "../hooks/useAuth";
import { Alert } from "react-bootstrap";

interface AuthGuardProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  requireAdmin?: boolean;
  requireOperator?: boolean;
  requireAuthorization?: string;
  requirePrivilege?: string;
  clusterName?: string;
  fallback?: React.ReactNode;
  onUnauthorized?: () => void;
}

/**
 * AuthGuard component that conditionally renders children based on user permissions
 */
export const AuthGuard: React.FC<AuthGuardProps> = ({
  children,
  requireAuth = false,
  requireAdmin = false,
  requireOperator = false,
  requireAuthorization,
  requirePrivilege,
  clusterName,
  fallback = null,
  onUnauthorized,
}) => {
  const {
    isAuthenticated,
    isLoading,
    isAdmin,
    isOperator,
    hasAuthorization,
    hasPrivilege,
  } = useAuth();

  // Show loading state
  if (isLoading) {
    return <div>Loading...</div>;
  }

  // Check authentication requirement
  if (requireAuth && !isAuthenticated) {
    onUnauthorized?.();
    return <>{fallback}</>;
  }

  // Check admin requirement
  if (requireAdmin && !isAdmin()) {
    onUnauthorized?.();
    return <>{fallback}</>;
  }

  // Check operator requirement
  if (requireOperator && !isOperator()) {
    onUnauthorized?.();
    return <>{fallback}</>;
  }

  // Check specific authorization requirement
  if (requireAuthorization && !hasAuthorization(requireAuthorization)) {
    onUnauthorized?.();
    return <>{fallback}</>;
  }

  // Check specific privilege requirement
  if (requirePrivilege && !hasPrivilege(requirePrivilege, clusterName)) {
    onUnauthorized?.();
    return <>{fallback}</>;
  }

  // All checks passed, render children
  return <>{children}</>;
};

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  requireAdmin?: boolean;
  requireOperator?: boolean;
  requireAuthorization?: string;
  requirePrivilege?: string;
  clusterName?: string;
  redirectTo?: string;
}

/**
 * ProtectedRoute component for route-level authorization
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAuth = true,
  requireAdmin = false,
  requireOperator = false,
  requireAuthorization,
  requirePrivilege,
  clusterName,
  //@ts-ignore
  redirectTo = "/login",
}) => {
  const handleUnauthorized = () => {
    
  };

  return (
    <AuthGuard
      requireAuth={requireAuth}
      requireAdmin={requireAdmin}
      requireOperator={requireOperator}
      requireAuthorization={requireAuthorization}
      requirePrivilege={requirePrivilege}
      clusterName={clusterName}
      onUnauthorized={handleUnauthorized}
      fallback={
        <Alert variant="danger" className="m-3">
          You do not have permission to view this content.
        </Alert>
      }
    >
      {children}
    </AuthGuard>
  );
};

interface ConditionalRenderProps {
  children: React.ReactNode;
  when: boolean;
  fallback?: React.ReactNode;
}

/**
 * ConditionalRender component for simple conditional rendering
 */
export const ConditionalRender: React.FC<ConditionalRenderProps> = ({
  children,
  when,
  fallback = null,
}) => {
  return when ? <>{children}</> : <>{fallback}</>;
};

// Higher-order component for authorization
export const withAuth = <P extends object>(
  Component: React.ComponentType<P>,
  authConfig: Omit<AuthGuardProps, "children">
) => {
  return (props: P) => (
    <AuthGuard {...authConfig}>
      <Component {...props} />
    </AuthGuard>
  );
};

export default AuthGuard;
