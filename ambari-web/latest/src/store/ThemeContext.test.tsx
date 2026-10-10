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

import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider, useTheme, THEME_STORAGE_KEY } from "./ThemeContext";

function Controls() {
  const { mode, resolved, setMode } = useTheme();
  return <><output>{mode}:{resolved}</output>{(["light", "dark", "system"] as const).map(value =>
    <button key={value} onClick={() => setMode(value)}>{value}</button>)}</>;
}
afterEach(() => {
  vi.restoreAllMocks();
  delete document.documentElement.dataset.bsTheme;
  document.documentElement.style.colorScheme = "";
});
describe("console appearance", () => {
  it("applies a stored theme and persists explicit changes across remounts", () => {
    localStorage.setItem(THEME_STORAGE_KEY, "dark");
    const view = render(<ThemeProvider><Controls /></ThemeProvider>);
    expect(screen.getByText("dark:dark")).toBeTruthy();
    expect(document.documentElement.dataset.bsTheme).toBe("dark");
    fireEvent.click(screen.getByRole("button", { name: "light" }));
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe("light");
    view.unmount();
    render(<ThemeProvider><Controls /></ThemeProvider>);
    expect(document.documentElement.style.colorScheme).toBe("light");
  });
  it("follows OS changes only in system mode and removes its listener", () => {
    let dark = false;
    const listeners = new Set<() => void>();
    const remove = vi.fn((_: string, listener: () => void) => listeners.delete(listener));
    vi.spyOn(window, "matchMedia").mockReturnValue({
      get matches() { return dark; },
      addEventListener: (_: string, listener: () => void) => listeners.add(listener),
      removeEventListener: remove,
    } as any);
    const view = render(<ThemeProvider><Controls /></ThemeProvider>);
    fireEvent.click(screen.getByRole("button", { name: "system" }));
    act(() => { dark = true; listeners.forEach(listener => listener()); });
    expect(screen.getByText("system:dark")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "light" }));
    act(() => { dark = false; listeners.forEach(listener => listener()); dark = true; listeners.forEach(listener => listener()); });
    expect(screen.getByText("light:light")).toBeTruthy();
    view.unmount();
    expect(remove).toHaveBeenCalledOnce();
    expect(listeners.size).toBe(0);
  });
  it("synchronizes valid preferences across tabs and tolerates blocked storage", () => {
    vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new Error("blocked"); });
    render(<ThemeProvider><Controls /></ThemeProvider>);
    fireEvent.click(screen.getByRole("button", { name: "dark" }));
    expect(document.documentElement.dataset.bsTheme).toBe("dark");
    act(() => window.dispatchEvent(new StorageEvent("storage", { key: THEME_STORAGE_KEY, newValue: "light" })));
    expect(document.documentElement.dataset.bsTheme).toBe("light");
    act(() => window.dispatchEvent(new StorageEvent("storage", { key: THEME_STORAGE_KEY, newValue: "unexpected" })));
    expect(screen.getByText("light:light")).toBeTruthy();
  });
});
