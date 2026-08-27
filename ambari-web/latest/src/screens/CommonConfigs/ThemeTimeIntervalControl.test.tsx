/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ThemeTimeIntervalControl from "./ThemeTimeIntervalControl";
import { PropertyType } from "./types";

const property = (
  value: number,
  incrementStep: number,
  units = "minutes,seconds",
): PropertyType => ({
  propertyName: "interval",
  propertyDisplayname: "Interval",
  propertyValue: value,
  propertyAttributes: {
    type: "int",
    unit: "milliseconds",
    minimum: 0,
    maximum: 240_000,
    increment_step: incrementStep,
  },
  previousValue: String(value),
  value,
  final: "false",
  isEditable: true,
  widget: { units: [{ "unit-name": units }] },
});

describe("ThemeTimeIntervalControl", () => {
  afterEach(cleanup);

  it("composes fields in config units and aligns direct input to the step", () => {
    const onChange = vi.fn();
    render(
      <ThemeTimeIntervalControl
        property={property(120_000, 10_000)}
        onChange={onChange}
      />,
    );
    expect(
      (screen.getByRole("spinbutton", { name: "Minutes" }) as HTMLInputElement)
        .value,
    ).toBe("2");
    const seconds = screen.getByRole("spinbutton", {
      name: "Seconds",
    }) as HTMLInputElement;
    expect(seconds.value).toBe("0");
    expect(seconds.step).toBe("10");

    fireEvent.change(seconds, { target: { value: "17" } });
    expect(onChange).toHaveBeenLastCalledWith(140_000);
  });

  it("wraps keyboard overflow and disables an unusable final unit", () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <ThemeTimeIntervalControl
        property={property(170_000, 10_000)}
        onChange={onChange}
      />,
    );
    const seconds = screen.getByRole("spinbutton", { name: "Seconds" });
    fireEvent.keyDown(seconds, { key: "ArrowUp" });
    expect(onChange).toHaveBeenLastCalledWith(120_000);

    rerender(
      <ThemeTimeIntervalControl
        property={property(120_000, 60_000)}
        onChange={onChange}
      />,
    );
    expect(
      (screen.getByRole("spinbutton", { name: "Seconds" }) as HTMLInputElement)
        .disabled,
    ).toBe(true);
  });

  it("supports the complete unit set including milliseconds", () => {
    render(
      <ThemeTimeIntervalControl
        property={property(
          90_061_001,
          1,
          "days,hours,minutes,seconds,milliseconds",
        )}
        onChange={vi.fn()}
      />,
    );
    expect(
      screen
        .getAllByRole("spinbutton")
        .map((input) => input.getAttribute("aria-label")),
    ).toEqual(["Days", "Hours", "Minutes", "Seconds", "Milliseconds"]);
    expect(
      (
        screen.getByRole("spinbutton", {
          name: "Milliseconds",
        }) as HTMLInputElement
      ).value,
    ).toBe("1");
  });
});
