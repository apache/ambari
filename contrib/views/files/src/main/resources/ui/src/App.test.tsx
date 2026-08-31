/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ViewApiError, type FilesApi } from "./api";
import App from "./App";

const file = {
  path: "/user/ambari/readme.txt",
  isDirectory: false,
  len: 2048,
  owner: "ambari",
  group: "hadoop",
  permission: "-rw-r-----",
  modificationTime: 1_700_000_000_000,
};

const directory = { ...file, path: "/user/ambari/input", isDirectory: true, len: 0, permission: "drwxr-x---" };

const mockApi = (): FilesApi => ({
  root: "/api/files",
  health: vi.fn().mockResolvedValue({ status: "200" }),
  home: vi.fn().mockResolvedValue({ ...directory, path: "/user/ambari" }),
  trash: vi.fn().mockRejectedValue(new Error("trash disabled")),
  list: vi.fn().mockResolvedValue({ files: [directory, file], meta: { originalSize: 2, truncated: false } }),
  mkdir: vi.fn().mockResolvedValue(directory),
  rename: vi.fn().mockResolvedValue(file),
  chmod: vi.fn().mockResolvedValue(file),
  copy: vi.fn().mockResolvedValue({ success: true }),
  move: vi.fn().mockResolvedValue({ success: true }),
  remove: vi.fn().mockResolvedValue({ success: true }),
  emptyTrash: vi.fn().mockResolvedValue({ success: true }),
  upload: vi.fn().mockResolvedValue(file),
  preview: vi.fn().mockResolvedValue({ data: "hello", readbytes: 5, isFileEnd: true }),
  checkDownload: vi.fn().mockResolvedValue({ allowed: true }),
  downloadUrl: vi.fn().mockReturnValue("/download"),
  generateArchive: vi.fn().mockResolvedValue({ requestId: "request" }),
  generatedDownloadUrl: vi.fn().mockReturnValue("/archive"),
});

describe("Files View", () => {
  it("checks HDFS, loads the home directory, and exposes all file columns", async () => {
    const api = mockApi();
    render(<App api={api} />);

    expect(screen.getByText("Checking HDFS access")).toBeInTheDocument();
    expect(await screen.findByText("readme.txt")).toBeInTheDocument();
    expect(screen.getByText("Erasure coding")).toBeInTheDocument();
    expect(api.list).toHaveBeenCalledWith("/user/ambari", "");
  });

  it("keeps single and multi-selection commands aligned with the Ember workflow", async () => {
    const user = userEvent.setup();
    render(<App api={mockApi()} />);
    await screen.findByText("readme.txt");

    await user.click(screen.getByRole("row", { name: /readme\.txt/ }));
    expect(screen.getByRole("button", { name: /Rename/ })).toBeEnabled();
    fireEvent.click(screen.getByRole("row", { name: /^input / }), { ctrlKey: true });
    expect(screen.getByRole("button", { name: /Rename/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Concatenate/ })).toBeDisabled();
  });

  it("creates a directory through the legacy fileops contract", async () => {
    const user = userEvent.setup();
    const api = mockApi();
    render(<App api={api} />);
    await screen.findByText("readme.txt");

    await user.click(screen.getByRole("button", { name: /New folder/ }));
    await user.type(screen.getByLabelText("Directory name"), "warehouse");
    await user.click(screen.getByRole("button", { name: "Create" }));
    await waitFor(() => expect(api.mkdir).toHaveBeenCalledWith("/user/ambari/warehouse"));
  });

  it("can retry failed and unprocessed paths after a partial operation", async () => {
    const user = userEvent.setup();
    const api = mockApi();
    vi.mocked(api.copy)
      .mockRejectedValueOnce(new ViewApiError("copy failed", 422, undefined, {
        success: false,
        failed: [file.path],
        unprocessed: [directory.path],
      }))
      .mockResolvedValueOnce({ success: true });
    render(<App api={api} />);
    await screen.findByText("readme.txt");

    await user.click(screen.getByRole("row", { name: /readme\.txt/ }));
    fireEvent.click(screen.getByRole("row", { name: /^input / }), { ctrlKey: true });
    await user.click(screen.getByRole("button", { name: /^Copy$/ }));
    const dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: /^Copy$/ }));

    expect(await within(dialog).findByText(/Remaining paths:/)).toHaveTextContent(file.path);
    await user.click(within(dialog).getByRole("button", { name: "Retry remaining" }));
    await waitFor(() => expect(api.copy).toHaveBeenLastCalledWith([file.path, directory.path], "/"));
  });
});
