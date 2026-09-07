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

const formatNumber = (value: number, decimals?: number) => new Intl.NumberFormat("en-US", {
  minimumFractionDigits: decimals,
  maximumFractionDigits: decimals ?? 2,
}).format(value);

const formatBytes = (value: number, decimals?: number) => {
  if (value === 0) return "0 B";
  const units = ["B", "KiB", "MiB", "GiB", "TiB", "PiB"];
  const unitIndex = Math.min(
    Math.floor(Math.log(Math.abs(value)) / Math.log(1024)),
    units.length - 1,
  );
  const safeUnitIndex = Math.max(unitIndex, 0);
  return `${formatNumber(value / 1024 ** safeUnitIndex, decimals)} ${units[safeUnitIndex]}`;
};

const formatSiRate = (value: number, decimals: number | undefined, suffix: string) => {
  const units = ["", "k", "M", "G", "T", "P"];
  const magnitude = value === 0
    ? 0
    : Math.min(Math.max(Math.floor(Math.log10(Math.abs(value)) / 3), 0), units.length - 1);
  return `${formatNumber(value / 1000 ** magnitude, decimals)} ${units[magnitude]}${suffix}`;
};

const formatSeconds = (value: number, decimals?: number) => {
  const absolute = Math.abs(value);
  if (absolute === 0) return "0 s";
  if (absolute !== 0 && absolute < 0.000001) return `${formatNumber(value * 1e9, decimals)} ns`;
  if (absolute < 0.001) return `${formatNumber(value * 1e6, decimals)} us`;
  if (absolute < 1) return `${formatNumber(value * 1e3, decimals)} ms`;
  if (absolute < 60) return `${formatNumber(value, decimals)} s`;
  if (absolute < 3600) return `${formatNumber(value / 60, decimals)} min`;
  if (absolute < 86400) return `${formatNumber(value / 3600, decimals)} hour`;
  if (absolute < 604800) return `${formatNumber(value / 86400, decimals)} day`;
  if (absolute < 31536000) return `${formatNumber(value / 604800, decimals)} week`;
  return `${formatNumber(value / 31556900, decimals)} year`;
};

const formatMilliseconds = (value: number, decimals?: number) => {
  const absolute = Math.abs(value);
  if (absolute < 1000) return `${formatNumber(value, decimals)} ms`;
  if (absolute < 60000) return `${formatNumber(value / 1000, decimals)} s`;
  if (absolute < 3600000) return `${formatNumber(value / 60000, decimals)} min`;
  if (absolute < 86400000) return `${formatNumber(value / 3600000, decimals)} hour`;
  if (absolute < 31536000000) return `${formatNumber(value / 86400000, decimals)} day`;
  return `${formatNumber(value / 31536000000, decimals)} year`;
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

export const getPanelDecimals = (options: unknown): number | undefined => {
  if (!options || typeof options !== "object" || Array.isArray(options)) return undefined;
  const standardOptions = (options as Record<string, unknown>).standardOptions;
  if (!standardOptions || typeof standardOptions !== "object" || Array.isArray(standardOptions)) return undefined;
  const decimals = (standardOptions as Record<string, unknown>).decimals;
  return typeof decimals === "number" && Number.isInteger(decimals) && decimals >= 0 && decimals <= 8
    ? decimals
    : undefined;
};

export const formatMetricValue = (
  value: string | number | null | undefined,
  unit = "",
  decimals?: number,
): string => {
  if (value === null || value === undefined) return "-";
  const rawValue = String(value);
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) return rawValue;
  if (!unit || unit === "none") return decimals === undefined ? rawValue : formatNumber(numericValue, decimals);

  switch (unit) {
    case "bytesIEC":
      return formatBytes(numericValue, decimals);
    case "bytesSecIEC":
    case "Bps":
      return `${formatBytes(numericValue, decimals)}/s`;
    case "bitsSecSI":
      return formatSiRate(numericValue, decimals, "b/s");
    case "packetsSec":
      return formatSiRate(numericValue, decimals, "p/s");
    case "iops":
      return formatSiRate(numericValue, decimals, "io/s");
    case "percentUnit":
      return `${formatNumber(numericValue * 100, decimals)}%`;
    case "percent":
      return `${formatNumber(numericValue, decimals)}%`;
    case "seconds":
      return formatSeconds(numericValue, decimals);
    case "milliseconds":
      return formatMilliseconds(numericValue, decimals);
    case "cps":
      return `${formatNumber(numericValue, decimals)} cps`;
    case "reqps":
      return `${formatNumber(numericValue, decimals)} req/s`;
    default:
      return decimals === undefined ? rawValue : formatNumber(numericValue, decimals);
  }
};
