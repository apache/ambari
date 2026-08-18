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

import { isValidElement, ReactElement } from "react";
import { RouteObject } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProtectedRoute } from "../components/AuthGuard";
import FeatureRouteGuard from "../components/FeatureRouteGuard";
import ServiceOperationRouteGuard from "../components/ServiceOperationRouteGuard";
import RoutesList from "./RoutesList";

function child(route: RouteObject, path?: string) {
  const result = route.children?.find((candidate) => candidate.path === path);
  if (!result) throw new Error(`Route ${path || "<pathless>"} was not found.`);
  return result;
}

type GuardProps = {
  children: ReactElement<GuardProps>;
  requireAuthorization?: string;
};

function element(route: RouteObject): ReactElement<GuardProps> {
  if (!isValidElement(route.element)) throw new Error("Route has no React element.");
  return route.element as ReactElement<GuardProps>;
}

describe("installation route contracts", () => {
  const root = RoutesList[0];
  const authenticated = child(root);
  const installer = child(authenticated, "installer");
  const main = child(authenticated, "main");

  it("protects Installer and Add Host with their mutation permissions and operation guard", () => {
    const installerRoute = element(child(installer, ":stepNumber"));
    expect(installerRoute.type).toBe(ProtectedRoute);
    expect(installerRoute.props.requireAuthorization).toBe("AMBARI.ADD_DELETE_CLUSTERS");
    expect(installerRoute.props.children.type).toBe(ServiceOperationRouteGuard);

    const addHostRoute = element(child(main, "host/add/:stepNumber"));
    expect(addHostRoute.type).toBe(ProtectedRoute);
    expect(addHostRoute.props.requireAuthorization).toBe("HOST.ADD_DELETE_HOSTS");
    expect(addHostRoute.props.children.type).toBe(ServiceOperationRouteGuard);
  });

  it("retains the Add Service feature, permission, and operation gates", () => {
    const addServiceRoute = element(child(main, "service/add/:stepNumber"));
    expect(addServiceRoute.type).toBe(FeatureRouteGuard);
    const permissionRoute = addServiceRoute.props.children;
    expect(permissionRoute.type).toBe(ProtectedRoute);
    expect(permissionRoute.props.requireAuthorization).toBe("SERVICE.ADD_DELETE_SERVICES");
    expect(permissionRoute.props.children.type).toBe(ServiceOperationRouteGuard);
  });
});
