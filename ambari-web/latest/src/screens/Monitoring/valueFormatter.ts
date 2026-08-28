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

const numberFormatter = new Intl.NumberFormat("en-US", {
  maximumFractionDigits: 2,
});

const formatNumber = (value: number) => numberFormatter.format(value);

const formatBytes = (value: number) => {
  if (value === 0) return "0 B";
  const units = ["B", "KiB", "MiB", "GiB", "TiB", "PiB"];
  const unitIndex = Math.min(
    Math.floor(Math.log(Math.abs(value)) / Math.log(1024)),
    units.length - 1,
  );
  const safeUnitIndex = Math.max(unitIndex, 0);
  return `${formatNumber(value / 1024 ** safeUnitIndex)} ${units[safeUnitIndex]}`;
};

export const getPanelUnit = (options: unknown): string => {
  if (!options || typeof options !== "object" || Array.isArray(options)) return "";
  const standardOptions = (options as Record<string, unknown>).standardOptions;
  if (
    !standardOptions ||
    typeof standardOptions !== "object" ||
    Array.isArray(standardOptions)
  ) return "";
  const unit = (standardOptions as Record<string, unknown>).util;
  return typeof unit === "string" ? unit : "";
};

export const formatMetricValue = (
  value: string | number | null | undefined,
  unit = "",
): string => {
  if (value === null || value === undefined) return "-";
  const rawValue = String(value);
  if (!unit) return rawValue;
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) return rawValue;

  switch (unit) {
    case "bytesIEC":
      return formatBytes(numericValue);
    case "bytesSecIEC":
    case "Bps":
      return `${formatBytes(numericValue)}/s`;
    case "percentUnit":
      return `${formatNumber(numericValue * 100)}%`;
    case "percent":
      return `${formatNumber(numericValue)}%`;
    case "seconds":
      return `${formatNumber(numericValue)} s`;
    case "cps":
      return `${formatNumber(numericValue)} cps`;
    case "reqps":
      return `${formatNumber(numericValue)} req/s`;
    default:
      return rawValue;
  }
};
