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
import { formatMetricValue, getPanelDecimals, getPanelUnit } from "./valueFormatter";

describe("monitoring value formatter", () => {
  it("formats Nightingale byte and rate units", () => {
    expect(formatMetricValue(1536, "bytesIEC")).toBe("1.5 KiB");
    expect(formatMetricValue(2048, "bytesSecIEC")).toBe("2 KiB/s");
    expect(formatMetricValue(2048, "Bps")).toBe("2 KiB/s");
  });

  it("distinguishes ratio and already-percent values", () => {
    expect(formatMetricValue(0.425, "percentUnit")).toBe("42.5%");
    expect(formatMetricValue(42.5, "percent")).toBe("42.5%");
  });

  it("formats duration and rate suffixes", () => {
    expect(formatMetricValue(12.25, "seconds")).toBe("12.25 s");
    expect(formatMetricValue(7, "cps")).toBe("7 cps");
    expect(formatMetricValue(3.5, "reqps")).toBe("3.5 req/s");
    expect(formatMetricValue(1250000, "bitsSecSI")).toBe("1.25 Mb/s");
    expect(formatMetricValue(1800, "packetsSec")).toBe("1.8 kp/s");
    expect(formatMetricValue(42000, "iops")).toBe("42 kio/s");
    expect(formatMetricValue(7200, "seconds")).toBe("2 hour");
  });

  it("preserves raw values for empty, unknown, and non-numeric units", () => {
    expect(formatMetricValue("001.50", "")).toBe("001.50");
    expect(formatMetricValue("12", "mixed")).toBe("12");
    expect(formatMetricValue("NaN", "bytesIEC")).toBe("NaN");
  });

  it("reads only the panel-level Nightingale unit", () => {
    expect(getPanelUnit({ standardOptions: { util: "bytesIEC" } })).toBe("bytesIEC");
    expect(getPanelUnit({ standardOptions: {}, overrides: [{ util: "percent" }] })).toBe("");
    expect(getPanelUnit(null)).toBe("");
  });

  it("applies configured decimals without changing legacy raw values", () => {
    expect(formatMetricValue(12.3456, "none", 3)).toBe("12.346");
    expect(formatMetricValue(0.4251, "percentUnit", 1)).toBe("42.5%");
    expect(getPanelDecimals({ standardOptions: { decimals: 4 } })).toBe(4);
    expect(getPanelDecimals({ standardOptions: { decimals: 12 } })).toBeUndefined();
  });
});
