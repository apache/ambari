/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

export function ambariApplicationRoot(documentPath: string): string {
  const path = documentPath.startsWith("/") ? documentPath : `/${documentPath}`;
  const viewsIndex = path.lastIndexOf("/views/");
  if (viewsIndex >= 0) {
    return `${path.slice(0, viewsIndex)}/`.replace(/\/+/g, "/");
  }
  return "/";
}

export function latestAmbariUrl(
  route: string,
  documentPath = window.location.pathname,
): string {
  const normalizedRoute = route.startsWith("/") ? route : `/${route}`;
  return `${ambariApplicationRoot(documentPath)}latest/#${normalizedRoute}`;
}

export function latestViewInstanceUrl(
  viewName: string,
  version: string,
  instanceName: string,
  documentPath = window.location.pathname,
): string {
  const route = [viewName, version, instanceName]
    .map((segment) => encodeURIComponent(segment))
    .join("/");
  return latestAmbariUrl(`/main/views/${route}`, documentPath);
}

export function latestShortViewUrl(
  viewName: string,
  shortUrl: string,
  documentPath = window.location.pathname,
): string {
  const route = [viewName, shortUrl]
    .map((segment) => encodeURIComponent(segment))
    .join("/");
  return latestAmbariUrl(`/main/view/${route}`, documentPath);
}
