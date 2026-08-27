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

import { get } from "lodash";
import { normalizeDefaultThemeResponse } from "./themeEngine";

export type ThemeLoadNotice = {
  kind: "empty" | "malformed" | "request";
  message: string;
};

export const classifyDefaultThemeResponse = (
  response: unknown,
  requestedServices: readonly string[],
): ThemeLoadNotice | null => {
  const normalized = normalizeDefaultThemeResponse(response, requestedServices);
  const missingServices = requestedServices.filter(
    (serviceName) => !normalized.themedServices.includes(serviceName),
  );
  if (!missingServices.length) return null;

  const relevantDiagnostics = normalized.diagnostics.filter(
    (diagnostic) =>
      !diagnostic.serviceName ||
      missingServices.includes(diagnostic.serviceName),
  );
  if (relevantDiagnostics.length) {
    return {
      kind: "malformed",
      message: relevantDiagnostics
        .map((diagnostic) => diagnostic.message)
        .join(" "),
    };
  }

  return {
    kind: "empty",
    message: `No default Theme is available for ${missingServices.join(", ")}.`,
  };
};

export const describeThemeRequestError = (
  error: unknown,
  fallback = "The Theme request failed.",
) => {
  const status = get(error as object, "response.status");
  const detail =
    get(error as object, "response.data.message") ||
    (error instanceof Error ? error.message : "") ||
    fallback;
  return status ? `HTTP ${status}: ${detail}` : detail;
};
