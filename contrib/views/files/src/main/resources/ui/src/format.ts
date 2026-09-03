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
export const basename = (path: string) => path === "/" ? "/" : path.replace(/\/$/, "").split("/").pop() ?? path;

export const parentPath = (path: string) => {
  if (path === "/") return "";
  const parent = path.replace(/\/$/, "").replace(/\/[^/]+$/, "");
  return parent || "/";
};

export const joinPath = (parent: string, name: string) => `${parent === "/" ? "" : parent}/${name.replace(/^\/+/, "")}` || "/";

export const humanSize = (bytes: number) => {
  if (!Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB", "PB"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** index;
  return `${value >= 10 || index === 0 ? value.toFixed(0) : value.toFixed(1)} ${units[index]}`;
};

export const formatDate = (value?: string | number) => {
  if (value === undefined || value === null || value === "") return "-";
  const numeric = typeof value === "string" && /^\d+$/.test(value) ? Number(value) : value;
  const date = new Date(numeric);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
};

export const permissionBits = (permission: string) => {
  const symbolic = permission.padEnd(10, "-").slice(-9);
  return [...symbolic].map((character) => character !== "-");
};

export const permissionFromBits = (permission: string, bits: boolean[]) => {
  const prefix = permission.length >= 10 ? permission.slice(0, permission.length - 9) : "-";
  const symbols = ["r", "w", "x", "r", "w", "x", "r", "w", "x"];
  return prefix + symbols.map((symbol, index) => bits[index] ? symbol : "-").join("");
};
