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
import { Switch, Route, Redirect, RouteProps } from "react-router-dom";
import routes from "./RoutesList.tsx";
import { useEffect, useState, ReactNode } from "react";
import LoadingSpinner from "../components/LoadingSpinner";

interface ProtectedRouteProps extends Omit<RouteProps, 'render'> {
  children: ReactNode;
}

// Protected route component that checks for authentication
const ProtectedRoute = ({ children, ...rest }: ProtectedRouteProps) => {
  const isAuthenticated = localStorage.getItem('ambari-user') !== null;
  
  return (
    <Route
      {...rest}
      render={({ location }) =>
        isAuthenticated ? (
          children
        ) : (
          <Redirect
            to={{
              pathname: "/",
              state: { from: location }
            }}
          />
        )
      }
    />
  );
};

export default function Routes() {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Simulating initial auth check
    setTimeout(() => {
      setIsLoading(false);
    }, 500);
  }, []);

  if (isLoading) {
    return <LoadingSpinner message="Initializing application..." />;
  }

  return (
    <Switch>
      {routes.map(({ path, Element }, key) => (
        <ProtectedRoute path={path} key={key}>
          <Element />
        </ProtectedRoute>
      ))}
    </Switch>
  );
}
