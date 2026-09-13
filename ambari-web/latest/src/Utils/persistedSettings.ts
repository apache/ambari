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

export function parsePersistedValue<T>(value: unknown, fallback: T): T {
  if (value === undefined || value === null || value === "") {
    return fallback;
  }
  if (typeof value !== "string") {
    return value as T;
  }
  try {
    const parsed = JSON.parse(value);
    return parsed === null ? fallback : parsed as T;
  } catch {
    return fallback;
  }
}

/**
 * GET /persist serializes Map<String, String>, so every value comes back as the
 * raw string it was stored as - anything written with JSON.stringify stays
 * encoded. (GET /persist/<key> returns that lone value as the whole body, which
 * axios parses for us, which is why the per-key reads never needed this.) Decode
 * a single value, leaving anything that was never JSON - USER_REDIRECTION_URL,
 * for instance - as the plain string it is.
 */
export function decodePersistedValue(value: unknown): unknown {
  if (typeof value !== "string") {
    return value;
  }
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

/** Decode every value of the aggregate GET /persist response. */
export function decodePersistedMap(data: unknown): unknown {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return data;
  }
  return Object.fromEntries(
    Object.entries(data as Record<string, unknown>)
      .map(([key, value]) => [key, decodePersistedValue(value)]),
  );
}

export function persistedPayload(values: Record<string, unknown>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(values).map(([key, value]) => [key, JSON.stringify(value)]),
  );
}
