/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import PrometheusChart from "./PrometheusChart";

vi.mock("react-chartjs-2", () => ({
  Line: ({ data }: any) => <output data-testid="plot">{JSON.stringify(data.datasets)}</output>,
  Bar: ({ data }: any) => <output data-testid="plot">{JSON.stringify(data.datasets)}</output>,
}));

const a = { metric: { host: "worker-a", device: "sda" }, targetRefId: "read", displayName: "worker-a sda read", values: [[10, "4"]] as [number, string][] };
const b = { metric: { host: "worker-b", device: "sdb" }, targetRefId: "read", displayName: "worker-b sdb read", values: [[10, "8"]] as [number, string][] };
const c = { metric: { host: "worker-c", device: "sdc" }, targetRefId: "read", displayName: "worker-c sdc read", values: [[10, "9"]] as [number, string][] };
const datasets = (): Array<{ label: string; hidden: boolean; borderColor: string }> =>
  JSON.parse(screen.getByTestId("plot").textContent || "[]");

describe("monitoring series legend", () => {
  it("toggles just one curve and keeps visibility and colors attached to its labels across reordering", () => {
    const view = render(<PrometheusChart results={[a, b]} />);
    const color = datasets().find(item => item.label === b.displayName)!.borderColor;
    fireEvent.click(screen.getByRole("button", { name: "Hide series: " + b.displayName }));
    expect(screen.getByRole("button", { name: "Show series: " + b.displayName }).getAttribute("aria-pressed")).toBe("false");
    view.rerender(<PrometheusChart results={[{ ...b, values: [[20, "12"]] }, a, c]} />);
    expect(datasets().find(item => item.label === b.displayName)).toMatchObject({ hidden: true, borderColor: color });
    expect(datasets().find(item => item.label === a.displayName)?.hidden).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: "Show all" }));
    expect(datasets().every(item => !item.hidden)).toBe(true);
  });

  it("isolates a selected curve even when refreshed queries introduce more series", () => {
    const view = render(<PrometheusChart results={[a, b]} />);
    fireEvent.click(screen.getByRole("button", { name: "Show only: " + a.displayName }));
    view.rerender(<PrometheusChart results={[c, b, a]} />);
    expect(datasets().filter(item => !item.hidden).map(item => item.label)).toEqual([a.displayName]);
    view.rerender(<PrometheusChart results={[c, b]} />);
    expect(screen.getByText("All series are hidden")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Show all" }));
    expect(datasets().every(item => !item.hidden)).toBe(true);
  });

  it("keeps separate query targets independent even when their labels and display names match", () => {
    render(<PrometheusChart results={[a, { ...a, targetRefId: "write" }]} />);
    fireEvent.click(screen.getAllByRole("button", { name: "Hide series: " + a.displayName })[0]);
    expect(datasets().map(item => item.hidden)).toEqual([true, false]);
  });

  it("supports the same explicit controls for table legends and bar presentation", () => {
    render(<PrometheusChart results={[a, b]} drawStyle="bars" legendMode="table" legendColumns={["last"]} />);
    expect(screen.getByRole("columnheader", { name: "last" })).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Hide series: " + a.displayName }));
    expect(datasets().map(item => item.hidden)).toEqual([true, false]);
  });
});
