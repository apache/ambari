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

import { describe, expect, it } from "vitest";
import { RouteObject } from "react-router-dom";
import RoutesList from "./RoutesList";

function routePaths(routes: RouteObject[]): string[] {
  return routes.flatMap((route) => [
    ...(typeof route.path === "string" ? [route.path] : []),
    ...routePaths(route.children ?? []),
  ]);
}

describe("Kerberos routes", () => {
  it("registers management, Enable recovery, and Classic Disable paths", () => {
    const paths = routePaths(RoutesList);

    expect(paths).toContain("kerberos");
    expect(paths).toContain("kerberos/enable/:stepNumber");
    expect(paths).toContain("kerberos/disableSecurity");
  });
});
