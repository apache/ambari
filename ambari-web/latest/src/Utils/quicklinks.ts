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

export type QuicklinkConfiguration = {
  type: string;
  properties?: Record<string, unknown>;
};

export function substituteQuicklinkTemplate(
  template: string,
  protocol: string,
  host: string,
  port: string,
  username: string,
  requiresUsername: boolean
): string {
  const replacements = requiresUsername
    ? [protocol, host, port, username]
    : [protocol, host, port];
  let index = 0;
  return template.replace(/%@/g, () => replacements[index++] ?? "");
}

export function resolveQuicklinkConfigPlaceholders(
  url: string,
  configurations: QuicklinkConfiguration[]
): string {
  return url.replace(
    /\$\{([^/{}\s]+)\/([^{}\s]+)\}/g,
    (placeholder, configType: string, propertyName: string) => {
      const configuration = configurations.find(
        (candidate) => candidate.type === configType
      );
      const value = configuration?.properties?.[propertyName];
      return value === undefined || value === null ? placeholder : String(value);
    }
  );
}

export function createPublicHostNameMap(
  items: Array<{ Hosts?: { host_name?: string; public_host_name?: string } }>
): Map<string, string> {
  const result = new Map<string, string>();
  items.forEach((item) => {
    const hostName = item.Hosts?.host_name;
    const publicHostName = item.Hosts?.public_host_name;
    if (hostName && publicHostName) {
      result.set(hostName, publicHostName);
    }
  });
  return result;
}
