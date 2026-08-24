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

export type ViewInstance = {
  contextPath: string;
  description: string;
  iconPath: string;
  instanceName: string;
  label: string;
  shortUrl: string;
  version: string;
  viewName: string;
  visible: boolean;
};

type ViewInstanceInfo = {
  context_path?: string;
  description?: string;
  icon_path?: string;
  instance_name?: string;
  label?: string;
  short_url?: string;
  version?: string;
  view_name?: string;
  visible?: boolean;
};

type ViewVersion = {
  ViewVersionInfo?: {
    label?: string;
    version?: string;
  };
  instances?: Array<{ ViewInstanceInfo?: ViewInstanceInfo }>;
};

type ViewDefinition = {
  ViewInfo?: { view_name?: string };
  versions?: ViewVersion[];
};

export type ViewsResponse = { items?: ViewDefinition[] };

function removeLeadingSlash(value: string): string {
  return value.startsWith("/") ? value.slice(1) : value;
}

function decodeRouteSegment(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

export function flattenVisibleViewInstances(data: ViewsResponse): ViewInstance[] {
  return (data.items || []).flatMap((definition) => {
    const definitionName = definition.ViewInfo?.view_name || "";
    return (definition.versions || []).flatMap((version) => {
      const versionInfo = version.ViewVersionInfo || {};
      return (version.instances || []).flatMap((entry) => {
        const info = entry.ViewInstanceInfo;
        if (!info?.visible || !info.instance_name || !info.context_path) {
          return [];
        }

        const viewName = info.view_name || definitionName;
        const viewVersion = info.version || versionInfo.version || "";
        if (!viewName || !viewVersion) {
          return [];
        }

        return [{
          contextPath: info.context_path,
          description: info.description || "No description",
          iconPath: info.icon_path || "",
          instanceName: info.instance_name,
          label: info.label || versionInfo.label || viewName,
          shortUrl: info.short_url || "",
          version: viewVersion,
          viewName,
          visible: true,
        }];
      });
    });
  });
}

export function generateViewUrl(instance: ViewInstance): string {
  if (instance.shortUrl) {
    return `#/main/view/${encodeURIComponent(instance.viewName)}/${encodeURIComponent(instance.shortUrl)}`;
  }
  return `#${generateRegularViewUrl(
    instance.viewName,
    instance.version,
    instance.instanceName,
  )}`;
}

export function openViewInstance(instance: ViewInstance): void {
  window.open(generateViewUrl(instance), "_blank", "noopener,noreferrer");
}

export function generateRegularViewUrl(
  viewName: string,
  version: string,
  instanceName: string,
  viewPath = "",
): string {
  const route = `/main/views/${encodeURIComponent(viewName)}/${encodeURIComponent(version)}/${encodeURIComponent(instanceName)}`;
  return viewPath ? `${route}?viewPath=${encodeURIComponent(viewPath)}` : route;
}

export function findRegularViewInstance(
  instances: ViewInstance[],
  viewName: string,
  version: string,
  instanceName: string,
): ViewInstance | undefined {
  const expectedContext = `/views/${viewName}/${version}/${instanceName}/`;
  return instances.find((instance) => (
    instance.viewName === viewName
    && instance.version === version
    && instance.instanceName === instanceName
  )) || instances.find((instance) => `${instance.contextPath}/`.endsWith(expectedContext));
}

export function findShortViewInstance(
  instances: ViewInstance[],
  viewName: string,
  shortUrl: string,
): ViewInstance | undefined {
  return instances.find((instance) => (
    instance.viewName === viewName && instance.shortUrl === shortUrl
  ));
}

export function viewRouteBreadcrumb(
  pathname: string,
  instances: ViewInstance[],
): string | undefined {
  const segments = pathname.split("/").filter(Boolean).map(decodeRouteSegment);
  if (segments[0] !== "main") {
    return undefined;
  }
  if (segments[1] === "views" && segments.length >= 5) {
    return findRegularViewInstance(
      instances,
      segments[2],
      segments[3],
      segments[4],
    )?.label || "Views";
  }
  if (segments[1] === "view" && segments.length >= 4) {
    return "";
  }
  return undefined;
}

export function parseViewPath(search: string, wildcardPath = ""): string {
  const wildcard = removeLeadingSlash(wildcardPath);
  const rawSearch = search.startsWith("?") ? search.slice(1) : search;
  if (!rawSearch) {
    return wildcard;
  }

  const parameters = new URLSearchParams(rawSearch);
  if (!parameters.has("viewPath")) {
    const query = parameters.toString();
    return `${wildcard}${query ? `?${query}` : ""}`;
  }

  const path = removeLeadingSlash(parameters.get("viewPath") || "");
  parameters.delete("viewPath");
  const remainingQuery = parameters.toString();
  const combinedPath = wildcard || path;
  return `${combinedPath}${remainingQuery ? `?${remainingQuery}` : ""}`;
}

export function buildViewIframeSrc(
  origin: string,
  contextPath: string,
  viewPath = "",
): string {
  const normalizedContext = contextPath.startsWith("/") ? contextPath : `/${contextPath}`;
  const base = `${origin}${normalizedContext.replace(/\/$/, "")}/`;
  return `${base}${removeLeadingSlash(viewPath)}`;
}

export default {
  buildViewIframeSrc,
  findRegularViewInstance,
  findShortViewInstance,
  flattenVisibleViewInstances,
  generateRegularViewUrl,
  generateViewUrl,
  openViewInstance,
  parseViewPath,
  viewRouteBreadcrumb,
};
