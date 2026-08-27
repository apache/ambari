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
import CustomSlider from "./CustomSlider";

vi.mock("rc-slider", () => ({
  default: ({
    onChange,
    onChangeComplete,
  }: {
    onChange: (value: number) => void;
    onChangeComplete: (value: number) => void;
  }) => (
    <button
      type="button"
      onMouseDown={() => onChange(3.125)}
      onMouseUp={() => onChangeComplete(3.125)}
    >
      Slider
    </button>
  ),
}));

describe("CustomSlider", () => {
  afterEach(cleanup);

  it("keeps preview and slide-stop callbacks separate in widget units", () => {
    const onChange = vi.fn();
    const onChangeComplete = vi.fn();
    render(
      <CustomSlider
        min={0}
        max={10}
        step={0.125}
        value={2.5}
        unit="GB"
        onChange={onChange}
        onChangeComplete={onChangeComplete}
      />,
    );

    fireEvent.mouseDown(screen.getByRole("button", { name: "Slider" }));
    expect(onChange).toHaveBeenCalledWith(3.125);
    expect(onChangeComplete).not.toHaveBeenCalled();

    fireEvent.mouseUp(screen.getByRole("button", { name: "Slider" }));
    expect(onChangeComplete).toHaveBeenCalledWith(3.125);
  });

  it("renders and selects default and recommended markers at tick zero", () => {
    const useDefault = vi.fn();
    const useRecommended = vi.fn();
    render(
      <CustomSlider
        min={0}
        max={10}
        step={1}
        value={0}
        onChange={vi.fn()}
        markers={[
          { kind: "current", value: 0 },
          { kind: "default", value: 0, onSelect: useDefault },
          { kind: "recommended", value: 0, onSelect: useRecommended },
        ]}
      />,
    );

    expect(screen.getByLabelText("Current value: 0")).toBeTruthy();
    fireEvent.click(
      screen.getByRole("button", { name: "Use default value: 0" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Use recommended value: 0" }),
    );
    expect(useDefault).toHaveBeenCalledOnce();
    expect(useRecommended).toHaveBeenCalledOnce();
  });
});
