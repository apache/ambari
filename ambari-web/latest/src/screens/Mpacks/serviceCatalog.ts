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

import type { MpackServiceEntry } from "../../api/mpackApi";

export interface ServiceGroup {
  key: string;
  entries: MpackServiceEntry[];
}

const versionParts = (id: string): bigint[] | null => {
  const version = id.slice(id.lastIndexOf("/") + 1);
  return /^(0|[1-9][0-9]*)(\.(0|[1-9][0-9]*))*$/.test(version) ? version.split(".").map(BigInt) : null;
};

function compareEntries(left: MpackServiceEntry, right: MpackServiceEntry) {
  if (left.enabled !== right.enabled) return left.enabled ? -1 : 1;
  const a = versionParts(left.release_id), b = versionParts(right.release_id);
  if (a && b) {
    for (let index = 0; index < Math.max(a.length, b.length); index++) {
      if ((a[index] ?? 0n) !== (b[index] ?? 0n)) return (a[index] ?? 0n) > (b[index] ?? 0n) ? -1 : 1;
    }
  }
  return left.release_id.localeCompare(right.release_id) || left.id.localeCompare(right.id);
}

export function groupServiceCatalog(items: MpackServiceEntry[]): ServiceGroup[] {
  const groups = new Map<string, MpackServiceEntry[]>();
  for (const item of items) {
    const key = JSON.stringify([item.service_name, item.stack_name, item.stack_version]);
    const entries = groups.get(key) || [];
    entries.push(item);
    groups.set(key, entries);
  }
  return [...groups].map(([key, entries]) => ({ key, entries: entries.slice().sort(compareEntries) }))
    .sort((a, b) => a.entries[0].display_name.localeCompare(b.entries[0].display_name) || a.key.localeCompare(b.key));
}

export function chooseServiceVersion(selection: string[], group: ServiceGroup, id: string | null): string[] {
  if (id !== null && !group.entries.some(item => item.id === id)) throw new Error("Unknown service version");
  const remaining = selection.filter(value => !group.entries.some(item => item.id === value));
  return id === null ? remaining : [...remaining, id];
}
