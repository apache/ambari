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

import { useTranslation } from "react-i18next";
import english from "../../locales/en/translation.json";

export function useWorkspaceText() {
  const { t } = useTranslation();
  return (key: keyof typeof english.monitoringWorkspace) =>
    String(t(`monitoringWorkspace.${key}`, { defaultValue: english.monitoringWorkspace[key] }));
}

export function localDateTime(date: Date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);
}

export const REFRESH_INTERVALS = [0, 10, 30, 60, 300];
export function refreshPreferenceKey(cluster: string, user: string) {
  return `ambari:monitoring:refresh:${JSON.stringify([user, cluster])}`;
}
export function readRefreshPreference(key: string) {
  try {
    const saved = localStorage.getItem(key);
    return saved !== null && REFRESH_INTERVALS.includes(Number(saved)) ? Number(saved) : 30;
  } catch {
    return 30;
  }
}
export function saveRefreshPreference(key: string, value: number) {
  if (!REFRESH_INTERVALS.includes(value)) return;
  try { localStorage.setItem(key, String(value)); } catch { /* Storage can be disabled by the browser. */ }
}

export function explorerParameters() {
  const hash = window.location.hash;
  return new URLSearchParams(hash.includes("?") ? hash.slice(hash.indexOf("?") + 1) : "");
}
