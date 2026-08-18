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

import { describe, expect, it, vi } from "vitest";
import {
  buildAlertDefinitionUpdate,
  buildAlertDefinitionDetails,
  countAlertHistoryByHost,
  filterAndSortAlertInstances,
  openAlertResponseInNewWindow,
  validateAlertDefinitionConfiguration,
  validateRepeatTolerance,
} from "./alertDefinitions";

describe("alert definition contracts", () => {
  it("builds details directly even when a definition has no group membership", () => {
    const details = buildAlertDefinitionDetails({
      id: 42,
      name: "service_check",
      label: "Service Check",
      description: "Checks the service",
      enabled: true,
      service_name: "HDFS",
      component_name: "NAMENODE",
      source: { type: "PORT" },
    }, [], [{
      definition_id: 42,
      summary: {
        WARNING: { count: 1, original_timestamp: "123", latest_text: "warning" },
      },
    }]);

    expect(details).toMatchObject({
      alert_definition_id: 42,
      label: "Service Check",
      groups: "",
      source_type: "PORT",
      state: "Enabled",
    });
    expect(details.statuses).toEqual([{
      status: "warning",
      count: 1,
      maintenance_count: 0,
      last_status_changed: "123",
      latest_text: "warning",
    }]);
  });

  it("counts 24-hour history records by host", () => {
    expect(countAlertHistoryByHost([
      { AlertHistory: { host_name: "host-1" } },
      { AlertHistory: { host_name: "host-2" } },
      { AlertHistory: { host_name: "host-1" } },
      { AlertHistory: {} },
    ])).toEqual({ "host-1": 2, "host-2": 1 });
  });

  it("reapplies instance filters and full-result sorting after refreshed data", () => {
    const instances = [
      { host_name: "host-1", service_name: "HDFS", state: "OK", last_updated_time: 30 },
      { host_name: "host-2", service_name: "HDFS", state: "WARNING", last_updated_time: 20 },
      { host_name: "host-3", service_name: "YARN", state: "OK", last_updated_time: 10 },
    ];

    expect(filterAndSortAlertInstances(
      instances,
      { service: "HDFS", hostName: "host", state: "" },
      { id: "history_count", desc: true },
      { "host-1": 1, "host-2": 4, "host-3": 10 },
    ).map((instance) => instance.host_name)).toEqual(["host-2", "host-1"]);
  });

  it("accepts 1 through 99 and DEBUG repeat tolerance", () => {
    expect(validateRepeatTolerance(1)).toBeNull();
    expect(validateRepeatTolerance("99")).toBeNull();
    expect(validateRepeatTolerance("DEBUG")).toBeNull();
    expect(validateRepeatTolerance(0)).not.toBeNull();
    expect(validateRepeatTolerance("1.5")).not.toBeNull();
  });

  it("validates interval, parameters, and threshold order", () => {
    const errors = validateAlertDefinitionConfiguration({
      interval: 0,
      source: {
        parameters: [{ name: "timeout", type: "NUMERIC", value: 0 }],
        reporting: { warning: { value: 10 }, critical: { value: 5 } },
      },
    });
    expect(errors).toEqual([
      "Check Interval must be a positive integer.",
      "timeout must be a positive number.",
      "Warning threshold cannot be greater than Critical threshold.",
    ]);
  });

  it("rejects non-positive and non-numeric reporting thresholds", () => {
    expect(validateAlertDefinitionConfiguration({
      interval: 1,
      source: {
        reporting: { warning: { value: 0 }, critical: { value: "invalid" } },
      },
    })).toEqual([
      "Warning threshold must be positive.",
      "Critical threshold must be a number.",
    ]);
  });

  it("preserves description, interval, and the complete changed source", () => {
    expect(buildAlertDefinitionUpdate({
      description: "new",
      interval: 5,
      source: { type: "PORT", uri: "host", reporting: { critical: { value: 5 } } },
    }, {
      description: "old",
      interval: 1,
      source: { type: "PORT", uri: "host", reporting: { critical: { value: 2 } } },
    })).toEqual({
      "AlertDefinition/description": "new",
      "AlertDefinition/interval": "5",
      "AlertDefinition/source": { type: "PORT", uri: "host", reporting: { critical: { value: 5 } } },
    });
  });

  it("opens response data through textContent", () => {
    const pre = { textContent: "" };
    const target = {
      opener: {} as unknown,
      document: {
        title: "",
        createElement: vi.fn(() => pre),
        body: { replaceChildren: vi.fn() },
      },
    };
    const openWindow = vi.fn(() => target);

    expect(openAlertResponseInNewWindow("<script>unsafe()</script>", openWindow as any)).toBe(true);
    expect(pre.textContent).toBe("<script>unsafe()</script>");
    expect(target.document.body.replaceChildren).toHaveBeenCalledWith(pre);
  });
});
