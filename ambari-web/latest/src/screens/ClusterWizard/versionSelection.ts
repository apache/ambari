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

function versionParts(value: string): number[] {
  return value.split(/[^0-9]+/).filter(Boolean).map(Number);
}

export function compareVersions(left: string, right: string): number {
  const leftParts = versionParts(left);
  const rightParts = versionParts(right);
  const length = Math.max(leftParts.length, rightParts.length);
  for (let index = 0; index < length; index += 1) {
    const difference = (leftParts[index] || 0) - (rightParts[index] || 0);
    if (difference !== 0) {
      return difference < 0 ? -1 : 1;
    }
  }
  return 0;
}

export function isJdkCompatible(
  currentVersion: string | undefined,
  minimumVersion: string | undefined,
  maximumVersion: string | undefined,
): boolean {
  if (!currentVersion || (!minimumVersion && !maximumVersion)) {
    return true;
  }
  const minimum = minimumVersion || maximumVersion || "";
  const maximum = maximumVersion || minimumVersion || "";
  return compareVersions(currentVersion, minimum) >= 0
    && compareVersions(currentVersion, maximum) <= 0;
}
