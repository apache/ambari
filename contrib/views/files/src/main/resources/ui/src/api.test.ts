/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import { describe, expect, it, vi } from "vitest";
import { ambariApplicationRoot, createFilesApi, parseViewContext } from "./api";

describe("Files View API", () => {
  it("derives versioned and unversioned View identities", () => {
    expect(parseViewContext("/views/FILES/1.0.0/INSTANCE/")).toEqual({ view: "FILES", version: "1.0.0", instance: "INSTANCE" });
    expect(parseViewContext("/views/FILES/INSTANCE/")).toEqual({ view: "FILES", version: "", instance: "INSTANCE" });
  });

  it("preserves the Ambari reverse-proxy prefix for API and download URLs", () => {
    const path = "/gateway/default/ambari/views/FILES/1.0.0/INSTANCE/";
    expect(ambariApplicationRoot(path)).toBe("/gateway/default/ambari/");
    const api = createFilesApi(
      { view: "FILES", version: "1.0.0", instance: "INSTANCE" },
      vi.fn(),
      path,
    );
    expect(api.downloadUrl("/user/a.txt")).toBe(
      "/gateway/default/ambari/api/v1/views/FILES/versions/1.0.0/instances/INSTANCE/resources/files/download/browse?path=%2Fuser%2Fa.txt&download=true",
    );
  });

  it("encodes list parameters and sends the Ambari request header", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ files: [], meta: {} }), { status: 200 }));
    const api = createFilesApi({ view: "FILES", version: "1.0.0", instance: "A B" }, fetcher);
    await api.list("/user/a folder", "*.xml");

    const [url, options] = fetcher.mock.calls[0];
    expect(url).toBe("/api/v1/views/FILES/versions/1.0.0/instances/A%20B/resources/files/fileops/listdir?path=%2Fuser%2Fa+folder&nameFilter=*.xml");
    expect((options.headers as Headers).get("X-Requested-By")).toBe("ambari-files-view");
  });

  it("preserves partial-operation details from a 422 response", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      success: false,
      message: "permission denied",
      succeeded: ["/a"],
      failed: ["/b"],
      unprocessed: ["/c"],
    }), { status: 422 }));
    const api = createFilesApi({ view: "FILES", version: "1.0.0", instance: "INSTANCE" }, fetcher);

    await expect(api.remove(["/a", "/b", "/c"], true)).rejects.toMatchObject({
      status: 422,
      operation: { failed: ["/b"], unprocessed: ["/c"] },
    });
  });

  it("checks read permission before starting a single-file download", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ allowed: true }), { status: 200 }));
    const api = createFilesApi({ view: "FILES", version: "1.0.0", instance: "INSTANCE" }, fetcher);

    await expect(api.checkDownload("/user/ambari/a b.txt")).resolves.toEqual({ allowed: true });
    expect(fetcher.mock.calls[0][0]).toBe(
      "/api/v1/views/FILES/versions/1.0.0/instances/INSTANCE/resources/files/download/browse?path=%2Fuser%2Fambari%2Fa+b.txt&checkperm=true",
    );
  });
});
