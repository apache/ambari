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

export default function TextRenderer({ panel }: { panel: DashboardPanel }) {
  const custom = panel.custom && typeof panel.custom === "object" ? panel.custom as Record<string, unknown> : {};
  const content = String(custom.content || panel.description || "");
  const align = (value: unknown, fallback: "flex-start" | "stretch"): "flex-start" | "flex-end" | "center" | "stretch" => ({
    flexStart: "flex-start",
    flexEnd: "flex-end",
    center: "center",
    unset: fallback,
  }[String(value)] as "flex-start" | "flex-end" | "center" | "stretch" | undefined) || fallback;
  return <div className="dashboard-text-content" style={{
    color: typeof custom.textColor === "string" ? custom.textColor : undefined,
    backgroundColor: typeof custom.bgColor === "string" ? custom.bgColor : undefined,
    fontSize: typeof custom.textSize === "number" ? custom.textSize : undefined,
    justifyContent: align(custom.justifyContent, "flex-start"),
    alignItems: align(custom.alignItems, "stretch"),
  }}>{content}</div>;
}
