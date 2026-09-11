/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
const getImpact = vi.hoisted(() => vi.fn());
vi.mock("../../api/serviceDependenciesApi", () => ({ default: { getImpact } }));
vi.mock("react-i18next", () => ({ useTranslation: () => ({ t: (key: string) => key }) }));
import useDependencyImpact from "./useDependencyImpact";
afterEach(() => { cleanup(); getImpact.mockReset(); });
const impact = (cluster: number, action = "STOP") => ({ provider_cluster_id: cluster, service_name: "HDFS", action,
  requires_confirmation: true, dependent_count: 2, hidden_dependent_count: 1, impact_revision: `revision-${cluster}`, items: [] });
it("discards a delayed old-cluster result and confirms the current action and revision", async () => {
  let resolveOld: (value: unknown) => void = () => {};
  getImpact.mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve; }));
  getImpact.mockResolvedValue(impact(2, "RESTART"));
  const { result, rerender } = renderHook(({ cluster, action }) => useDependencyImpact(cluster, "HDFS", action, true),
    { initialProps: { cluster: "A", action: "STOP" as "STOP" | "RESTART" } });
  expect(result.current.blocked).toBe(true);
  rerender({ cluster: "B", action: "RESTART" });
  await waitFor(() => expect(result.current.blocked).toBe(false));
  await act(async () => { resolveOld(impact(1)); });
  expect(JSON.parse(result.current.parameters["parameters/managed_dependency_impact_confirmations"])).toEqual([
    { provider_cluster_id: 2, service_name: "HDFS", action: "RESTART", revision: "revision-2" },
  ]);
});
it("requires a fresh impact after a rejected revision", async () => {
  getImpact.mockResolvedValue(impact(1));
  const { result } = renderHook(() => useDependencyImpact("A", "HDFS", "STOP", true));
  await waitFor(() => expect(result.current.blocked).toBe(false));
  getImpact.mockImplementation(() => new Promise(() => {}));
  act(() => result.current.handleFailure({ response: { data: { code: "DEPENDENCY_IMPACT_STALE" } } }));
  expect(result.current.blocked).toBe(true); expect(result.current.parameters).toEqual({});
});
