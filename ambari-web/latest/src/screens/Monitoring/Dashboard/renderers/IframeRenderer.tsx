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

import type { DashboardPanel } from "../../types";

export default function IframeRenderer({ panel }: { panel: DashboardPanel }) {
  const custom = panel.custom && typeof panel.custom === "object" ? panel.custom as Record<string, unknown> : {};
  const url = String(custom.url || custom.iframe_url || "");
  if (!url) return <div className="monitoring-empty">No iframe URL is configured.</div>;
  try {
    const target = new URL(url, window.location.origin);
    if (target.origin !== window.location.origin || !["http:", "https:"].includes(target.protocol)) {
      return <div className="monitoring-empty">Embedded content must use the Ambari origin.</div>;
    }
    return (
      <iframe
        className="dashboard-embedded-frame"
        title={panel.name || "Embedded monitoring content"}
        src={target.toString()}
        sandbox="allow-forms allow-scripts"
        referrerPolicy="same-origin"
      />
    );
  } catch {
    return <div className="monitoring-empty">The configured iframe URL is invalid.</div>;
  }
}
