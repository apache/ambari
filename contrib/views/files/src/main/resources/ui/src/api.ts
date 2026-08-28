/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import type {
  DirectoryListing,
  HdfsFile,
  OperationResult,
  ViewContext,
} from "./types";

export class ViewApiError extends Error {
  status: number;
  detail?: string;
  operation?: OperationResult;

  constructor(message: string, status: number, detail?: string, operation?: OperationResult) {
    super(message);
    this.name = "ViewApiError";
    this.status = status;
    this.detail = detail;
    this.operation = operation;
  }
}

export const parseViewContext = (pathname: string): ViewContext => {
  const parts = pathname.split("/").filter(Boolean);
  const viewsIndex = parts.lastIndexOf("views");
  const tail = viewsIndex >= 0 ? parts.slice(viewsIndex + 1) : parts;
  const [view = "FILES", second = "", third = ""] = tail;
  const hasVersion = /^(?:\d+\.){2,3}\d+(?:[-.].*)?$/.test(second);
  return {
    view,
    version: hasVersion ? second : "",
    instance: hasVersion ? third : second,
  };
};

export const ambariApplicationRoot = (pathname: string): string => {
  const normalized = pathname.startsWith("/") ? pathname : `/${pathname}`;
  const viewsIndex = normalized.lastIndexOf("/views/");
  return viewsIndex >= 0 ? `${normalized.slice(0, viewsIndex)}/` : "/";
};

export const createFilesApi = (
  context = parseViewContext(window.location.pathname),
  fetcher: typeof fetch = fetch,
  documentPath = window.location.pathname,
) => {
  const versionSegment = context.version ? `/versions/${encodeURIComponent(context.version)}` : "";
  const root = `${ambariApplicationRoot(documentPath)}api/v1/views/${encodeURIComponent(context.view)}${versionSegment}/instances/${encodeURIComponent(context.instance)}/resources/files`;

  const request = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
    const headers = new Headers(init.headers);
    headers.set("X-Requested-By", "ambari-files-view");
    if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
    const response = await fetcher(`${root}${path}`, {
      ...init,
      headers,
      credentials: "same-origin",
    });
    const text = await response.text();
    let body: unknown = undefined;
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        body = text;
      }
    }
    if (!response.ok) {
      const object = body && typeof body === "object" ? (body as Record<string, unknown>) : {};
      const operation = typeof object.success === "boolean" ? (object as OperationResult) : undefined;
      throw new ViewApiError(
        String(object.message ?? response.statusText ?? "Request failed"),
        response.status,
        typeof object.trace === "string" ? object.trace : undefined,
        operation,
      );
    }
    return body as T;
  };

  const query = (values: Record<string, string | number | boolean>) => {
    const parameters = new URLSearchParams();
    Object.entries(values).forEach(([key, value]) => parameters.set(key, String(value)));
    return parameters.toString();
  };

  return {
    root,
    health: () => request<{ status: string | number; message?: string; trace?: string }>("/help/hdfsStatus"),
    home: () => request<HdfsFile>("/user/home"),
    trash: () => request<HdfsFile>("/user/trashDir"),
    list: (path: string, nameFilter = "") =>
      request<DirectoryListing>(`/fileops/listdir?${query({ path, nameFilter })}`),
    mkdir: (path: string) => request<HdfsFile>("/fileops/mkdir", { method: "PUT", body: JSON.stringify({ path }) }),
    rename: (src: string, dst: string) => request<HdfsFile>("/fileops/rename", { method: "POST", body: JSON.stringify({ src, dst }) }),
    chmod: (path: string, mode: string) => request<HdfsFile>("/fileops/chmod", { method: "POST", body: JSON.stringify({ path, mode }) }),
    copy: (sourcePaths: string[], destinationPath: string) => request<OperationResult>("/fileops/copy", { method: "POST", body: JSON.stringify({ sourcePaths, destinationPath }) }),
    move: (sourcePaths: string[], destinationPath: string) => request<OperationResult>("/fileops/move", { method: "POST", body: JSON.stringify({ sourcePaths, destinationPath }) }),
    remove: (paths: string[], permanent: boolean) => request<OperationResult>(permanent ? "/fileops/remove" : "/fileops/moveToTrash", {
      method: "POST",
      body: JSON.stringify({ paths: paths.map((path) => ({ path, recursive: true })) }),
    }),
    emptyTrash: () => request<OperationResult>("/fileops/trash/emptyTrash", { method: "DELETE" }),
    upload: (path: string, file: File, signal?: AbortSignal) => {
      const data = new FormData();
      data.append("path", path);
      data.append("file", file);
      return request<HdfsFile>("/upload", { method: "PUT", body: data, signal });
    },
    preview: (path: string, start: number, end: number) => request<{ data: string; readbytes: number; isFileEnd: boolean }>(`/preview/file?${query({ path, start, end })}`),
    checkDownload: (path: string) => request<{ allowed: boolean }>(`/download/browse?${query({ path, checkperm: true })}`),
    downloadUrl: (path: string) => `${root}/download/browse?${query({ path, download: true })}`,
    generateArchive: (paths: string[], concatenate = false) => request<{ requestId: string }>(`/download/${concatenate ? "concat" : "zip"}/generate-link`, {
      method: "POST",
      body: JSON.stringify({ download: true, entries: paths }),
    }),
    generatedDownloadUrl: (requestId: string, concatenate = false) => `${root}/download/${concatenate ? "concat" : "zip"}?${query({ requestId })}`,
  };
};

export type FilesApi = ReturnType<typeof createFilesApi>;
