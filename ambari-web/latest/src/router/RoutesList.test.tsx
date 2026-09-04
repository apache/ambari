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
import { Navigate, RouteObject } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ProtectedRoute } from "../components/AuthGuard";
import FeatureRouteGuard from "../components/FeatureRouteGuard";
import ServiceOperationRouteGuard from "../components/ServiceOperationRouteGuard";
import { ServiceIndexRedirect } from "../screens/Services/ServiceDashboard";
import RoutesList, { HaPersistenceRouteGuard } from "./RoutesList";

function child(route: RouteObject, path?: string) {
  const result = route.children?.find((candidate) => candidate.path === path);
  if (!result) throw new Error(`Route ${path || "<pathless>"} was not found.`);
  return result;
}

type GuardProps = {
  children: ReactElement<GuardProps>;
  feature?: string;
  requireAuthorization?: string;
  redirectTo?: string;
  replace?: boolean;
  to?: string;
};

function element(route: RouteObject): ReactElement<GuardProps> {
  if (!isValidElement(route.element)) throw new Error("Route has no React element.");
  return route.element as ReactElement<GuardProps>;
}

function routePaths(routes: RouteObject[]): string[] {
  return routes.flatMap((route) => [
    ...(typeof route.path === "string" ? [route.path] : []),
    ...routePaths(route.children ?? []),
  ]);
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

describe("Kerberos routes", () => {
  it("registers management, Enable recovery, and Classic Disable paths", () => {
    const paths = routePaths(RoutesList);

    expect(paths).toContain("kerberos");
    expect(paths).toContain("kerberos/enable/:stepNumber");
    expect(paths).toContain("kerberos/disableSecurity");
  });
});

describe("Ambari View routes", () => {
  it("registers both regular and short View URLs", () => {
    const paths = routePaths(RoutesList);

    expect(paths).toContain("views");
    expect(paths).toContain("views/:viewName/:viewVersion/:instanceName/*");
    expect(paths).toContain("view");
    expect(paths).toContain("view/:viewName/:shortName/*");
  });
});

describe("operational authorization routes", () => {
  const root = RoutesList[0];
  const authenticated = child(root);
  const main = child(authenticated, "main");
  const admin = child(main, "admin");

  it("protects Experimental and Alert creation from runtime operation locks", () => {
    const experimental = element(child(authenticated, "experimental"));
    expect(experimental.type).toBe(ProtectedRoute);
    expect(experimental.props.children.type).toBe(ServiceOperationRouteGuard);

    const createAlert = element(child(main, "alerts/add/:stepNumber"));
    expect(createAlert.type).toBe(FeatureRouteGuard);
    expect(createAlert.props.feature).toBe("createAlerts");
    const alertPermission = createAlert.props.children;
    expect(alertPermission.type).toBe(ProtectedRoute);
    expect(alertPermission.props.requireAuthorization).toBe("SERVICE.TOGGLE_ALERTS");
    expect(alertPermission.props.children.type).toBe(ServiceOperationRouteGuard);
  });

  it.each([
    ["serviceAccounts", "SERVICE.SET_SERVICE_USERS_GROUPS"],
    ["serviceAutoStart", "SERVICE.MANAGE_AUTO_START, CLUSTER.MANAGE_AUTO_START"],
  ])("protects Admin mutation route %s", (path, permission) => {
    const route = element(child(admin, path));
    const permissionRoute = route.type === FeatureRouteGuard
      ? route.props.children
      : route;
    expect(permissionRoute.type).toBe(ProtectedRoute);
    expect(permissionRoute.props.requireAuthorization).toBe(permission);
    expect(permissionRoute.props.children.type).toBe(ServiceOperationRouteGuard);
  });
});

describe("service routes", () => {
  it("redirects the Services index to an installed service", () => {
    const root = RoutesList[0];
    const authenticated = child(root);
    const main = child(authenticated, "main");
    const services = child(main, "services");
    const indexRoute = services.children?.find((route) => route.index);

    expect(indexRoute).toBeDefined();
    expect(element(indexRoute as RouteObject).type).toBe(ServiceIndexRedirect);
  });
});

describe("monitoring route contracts", () => {
  const root = RoutesList[0];
  const authenticated = child(root);
  const main = child(authenticated, "main");
  const dashboard = child(main, "dashboard");
  const monitoring = child(main, "monitoring");

  it("protects dashboard metrics and migrates removed dashboard tabs", () => {
    const metrics = element(child(dashboard, "metrics"));
    expect(metrics.type).toBe(ProtectedRoute);
    expect(metrics.props.requireAuthorization).toBe("CLUSTER.VIEW_METRICS");
    expect(metrics.props.redirectTo).toBe("/main/dashboard/confighistory");

    for (const path of ["heatmaps", "*"]) {
      const redirect = element(child(dashboard, path));
      expect(redirect.type).toBe(Navigate);
      expect(redirect.props.to).toBe("/main/dashboard/metrics");
      expect(redirect.props.replace).toBe(true);
    }
  });

  it("uses separate cluster and host metric permissions", () => {
    const clusterGroup = monitoring.children?.find(
      (route) => !route.index && !route.path,
    );
    expect(clusterGroup).toBeDefined();
    const clusterGuard = element(clusterGroup as RouteObject);
    expect(clusterGuard.type).toBe(ProtectedRoute);
    expect(clusterGuard.props.requireAuthorization).toBe("CLUSTER.VIEW_METRICS");
    expect(clusterGuard.props.children.type).toBeDefined();
    expect(child(clusterGroup as RouteObject, "dashboards")).toBeDefined();
    expect(child(clusterGroup as RouteObject, "explorer")).toBeDefined();

    const targetsGuard = element(child(monitoring, "targets"));
    expect(targetsGuard.type).toBe(ProtectedRoute);
    expect(targetsGuard.props.requireAuthorization).toBe("HOST.VIEW_METRICS");
  });
});

describe("HA route contracts", () => {
  const root = RoutesList[0];
  const authenticated = child(root);
  const main = child(authenticated, "main");
  const services = child(main, "services");

  it.each([
    "highAvailability/:componentName/enable/:stepNumber",
    ":componentName/federation/:stepNumber",
    ":componentName/federation/routerBasedFederation/:stepNumber",
    "highAvailability/:componentName/add/:stepNumber",
  ])("requires persisted-data capability for %s", (path) => {
    const route = element(child(services, path));
    expect(route.type).toBe(HaPersistenceRouteGuard);
    const renderedGuard = HaPersistenceRouteGuard({ children: route.props.children });
    expect(renderedGuard.type).toBe(ProtectedRoute);
    expect(renderedGuard.props.requireAuthorization)
      .toBe("CLUSTER.MANAGE_USER_PERSISTED_DATA");

    const permissionRoute = route.props.children;
    expect(permissionRoute.type).toBe(ProtectedRoute);
    expect(permissionRoute.props.requireAuthorization).toBe("SERVICE.ENABLE_HA");
    expect(permissionRoute.props.children.type).toBe(ServiceOperationRouteGuard);
  });

  it.each([
    "highAvailability/:componentName/remove/:stepNumber",
    "highAvailability/:componentName/activate/:stepNumber",
  ])("accepts the HAWQ custom-command permission set for %s", (path) => {
    const route = element(child(services, path));
    expect(route.type).toBe(HaPersistenceRouteGuard);
    const renderedGuard = HaPersistenceRouteGuard({ children: route.props.children });
    expect(renderedGuard.type).toBe(ProtectedRoute);
    expect(renderedGuard.props.requireAuthorization)
      .toBe("CLUSTER.MANAGE_USER_PERSISTED_DATA");

    const permissionRoute = route.props.children;
    expect(permissionRoute.type).toBe(ProtectedRoute);
    expect(permissionRoute.props.requireAuthorization).toBe(
      "SERVICE.RUN_CUSTOM_COMMAND, SERVICE.RUN_SERVICE_CHECK, SERVICE.TOGGLE_MAINTENANCE, SERVICE.ENABLE_HA",
    );
    expect(permissionRoute.props.children.type).toBe(ServiceOperationRouteGuard);
  });
});
