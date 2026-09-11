/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file to You under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import { describe, expect, it } from "vitest";
import {
  clusterNameFromPath,
  clusterHashPath,
  clusterPath,
  legacyMainContinuation,
  normalizeLegacyMainPath,
  safeLegacyMainPath,
  scopeClusterPath,
} from "./clusterRoute";

describe("cluster routes", () => {
  it("encodes names and retains an exact main suffix", () => {
    expect(clusterPath("east / prod", "/main/hosts?sort=name"))
      .toBe("/clusters/east%20%2F%20prod/main/hosts?sort=name");
    expect(clusterNameFromPath("/clusters/east%20%2F%20prod/main/hosts"))
      .toBe("east / prod");
  });

  it("scopes only legacy main paths", () => {
    expect(scopeClusterPath("/main/alerts", "alpha"))
      .toBe("/clusters/alpha/main/alerts");
    expect(scopeClusterPath("/main/view", null)).toBe("/main/view");
    expect(scopeClusterPath("/adminView", "alpha")).toBe("/adminView");
    expect(scopeClusterPath("/mainish", "alpha")).toBe("/mainish");
  });

  it("preserves a legacy suffix and query", () => {
    expect(legacyMainContinuation("/main/hosts", "?page=2"))
      .toBe("/main/hosts?page=2");
  });

  it("normalizes only safe internal continuations", () => {
    expect(normalizeLegacyMainPath("/main/hosts?page=2&sort=name"))
      .toBe("/main/hosts?page=2&sort=name");
    expect(normalizeLegacyMainPath("/mainish/hosts")).toBeNull();
    expect(normalizeLegacyMainPath("https://example.test/main/hosts")).toBeNull();
    expect(normalizeLegacyMainPath("/main/%2e%2e/admin")).toBeNull();
    expect(normalizeLegacyMainPath("/main/../admin")).toBeNull();
    expect(safeLegacyMainPath("/main/%2e%2e/admin"))
      .toBe("/main/dashboard/metrics");
    expect(scopeClusterPath("/main/%2e%2e/admin", "alpha"))
      .toBe("/clusters/alpha/main/dashboard/metrics");
  });

  it("builds a cluster-owned hash redirect from an explicit target", () => {
    expect(clusterHashPath("east prod", "/main/hosts"))
      .toBe("/#/clusters/east%20prod/main/hosts");
    expect(() => clusterHashPath("", "/main/hosts"))
      .toThrow("explicit cluster");
  });

});
