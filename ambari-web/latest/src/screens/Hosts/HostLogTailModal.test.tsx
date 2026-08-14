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

import { act, cleanup, render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({ fetchLogTail: vi.fn() }));
vi.mock("../../api/hostLogsApi", () => ({
  default: { fetchLogTail: mocks.fetchLogTail },
}));
vi.mock("../../components/Spinner", () => ({
  default: () => <div>Loading log</div>,
}));

import HostLogTailModal from "./HostLogTailModal";

describe("HostLogTailModal polling", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    mocks.fetchLogTail.mockResolvedValue({ logList: [] });
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("polls serially and stops after unmount", async () => {
    const view = render(
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <HostLogTailModal
          componentName="hdfs_namenode"
          filePath="/var/log/hdfs/namenode.log"
          hostName="nn1"
          onClose={() => {}}
        />
      </AppContext.Provider>,
    );

    await act(async () => {});
    expect(mocks.fetchLogTail).toHaveBeenCalledTimes(1);
    expect(mocks.fetchLogTail).toHaveBeenLastCalledWith(
      "c1",
      "hdfs_namenode",
      "nn1",
      50,
      0,
    );

    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(mocks.fetchLogTail).toHaveBeenCalledTimes(2);

    view.unmount();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(4000);
    });
    expect(mocks.fetchLogTail).toHaveBeenCalledTimes(2);
  });
});
