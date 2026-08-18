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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { MergedAlert } from "./types";
import { filterAlerts, isWithinTimeRange } from "./alertUtils";

const alert = {
  label: "NameNode Health",
  serviceDisplayName: "HDFS",
  component_name: "NAMENODE",
  state: "Enabled",
  groups: "HDFS Default, Operators",
  statuses: [],
} as unknown as MergedAlert;

describe("alert list filtering", () => {
  afterEach(() => vi.useRealTimers());

  it("matches one group when a definition belongs to multiple groups", () => {
    expect(filterAlerts([alert], [{ category: "Group", value: "Operators" }])).toEqual([alert]);
  });

  it("supports Ambari millisecond and second timestamps in time filters", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-08-17T12:00:00Z"));
    const withinHour = Date.now() - 30 * 60 * 1000;
    expect(isWithinTimeRange(String(withinHour), "Past 1 hour")).toBe(true);
    expect(isWithinTimeRange(String(Math.floor(withinHour / 1000)), "Past 1 hour")).toBe(true);
  });
});
