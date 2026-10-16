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

import { createContext, useContext, useEffect, useLayoutEffect, useState, type ReactNode } from "react";

export type ThemeMode = "light" | "dark" | "system";
export const THEME_STORAGE_KEY = "ambari.appearance";
export const parseThemeMode = (value: unknown): ThemeMode =>
  value === "dark" || value === "system" ? value : "light";

export function readThemeMode(): ThemeMode {
  try { return parseThemeMode(localStorage.getItem(THEME_STORAGE_KEY)); } catch { return "light"; }
}

const ThemeContext = createContext<{ mode: ThemeMode; resolved: "light" | "dark"; setMode: (mode: ThemeMode) => void }>({
  mode: "light", resolved: "light", setMode: () => {},
});

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setPreference] = useState<ThemeMode>(readThemeMode);
  const [systemDark, setSystemDark] = useState(() => window.matchMedia?.("(prefers-color-scheme: dark)").matches ?? false);
  const resolved = mode === "system" ? systemDark ? "dark" : "light" : mode;

  useEffect(() => {
    const media = window.matchMedia?.("(prefers-color-scheme: dark)");
    const change = () => setSystemDark(media?.matches ?? false);
    const storage = (event: StorageEvent) => {
      if (event.key === THEME_STORAGE_KEY || event.key === null) setPreference(parseThemeMode(event.newValue));
    };
    media?.addEventListener("change", change);
    window.addEventListener("storage", storage);
    return () => {
      media?.removeEventListener("change", change);
      window.removeEventListener("storage", storage);
    };
  }, []);

  useLayoutEffect(() => {
    document.documentElement.dataset.bsTheme = resolved;
    document.documentElement.style.colorScheme = resolved;
  }, [resolved]);

  const setMode = (value: ThemeMode) => {
    setPreference(value);
    try { localStorage.setItem(THEME_STORAGE_KEY, value); } catch { /* Keep the current tab usable without storage. */ }
  };
  return <ThemeContext.Provider value={{ mode, resolved, setMode }}>{children}</ThemeContext.Provider>;
}

export const useTheme = () => useContext(ThemeContext);
