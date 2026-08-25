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
import {
  composeTimeInterval,
  configValueByWidget,
  convertValue,
  formatTickLabel,
  parseTimeInterval,
  widgetValueByConfigAttributes,
} from "./unitConversionUtils";

describe("Theme unit conversion", () => {
  it.each([
    [1024, "b", "kb", 1],
    [1024 ** 2, "b", "mb", 1],
    [1024 ** 3, "b", "gb", 1],
    [1024 ** 4, "b", "tb", 1],
    [1024, "kb", "mb", 1],
    [1024, "mb", "gb", 1],
    [1024, "gb", "tb", 1],
  ])("converts %s %s to %s", (value, fromUnit, toUnit, expected) => {
    expect(convertValue(value, fromUnit, toUnit)).toBe(expected);
    expect(convertValue(expected, toUnit, fromUnit)).toBe(value);
  });

  it("round trips integer-backed percentages without scaling", () => {
    expect(
      widgetValueByConfigAttributes(
        75,
        "int",
        "percent",
        "percent.percent_int",
      ),
    ).toBe(75);
    expect(
      configValueByWidget(
        75,
        "percent",
        "int",
        "int",
        "percent.percent_int",
      ),
    ).toBe(75);
  });

  it("round trips float-backed percentages", () => {
    expect(
      widgetValueByConfigAttributes(
        0.999,
        "float",
        "percent",
        "percent.percent_float",
      ),
    ).toBe(99.9);
    expect(
      configValueByWidget(
        99.9,
        "percent",
        "float",
        "float",
        "percent.percent_float",
      ),
    ).toBe(0.999);
  });

  it("preserves millisecond remainders while parsing and composing", () => {
    expect(parseTimeInterval(90_001, "milliseconds")).toEqual({
      days: 0,
      hours: 0,
      minutes: 1,
      seconds: 30,
      milliseconds: 1,
    });
    expect(composeTimeInterval(0, 0, 1, 30, "milliseconds", 1)).toBe(
      90_001,
    );
  });

  it("converts a base-unit increment into the displayed spinner unit", () => {
    expect(convertValue(10_000, "milliseconds", "seconds")).toBe(10);
  });

  it("preserves zero and three-decimal precision in both directions", () => {
    expect(
      widgetValueByConfigAttributes(
        0,
        "float",
        "percent",
        "percent.percent_float",
      ),
    ).toBe(0);
    expect(
      configValueByWidget(
        0,
        "percent",
        "float",
        "float",
        "percent.percent_float",
      ),
    ).toBe(0);
    expect(convertValue(1537, "b", "kb")).toBe(1.501);
    expect(formatTickLabel(0, "percent", " ")).toBe("0 %");
    expect(formatTickLabel(0, "b", " ")).toBe("0 B");
  });
});
