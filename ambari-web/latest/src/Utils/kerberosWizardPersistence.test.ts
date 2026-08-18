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

import { afterEach, describe, expect, it, vi } from "vitest";
import ClusterApi from "../api/clusterApi";
import { postKerberosWizardPersistData } from "./kerberosWizardPersistence";

describe("Kerberos wizard persistence", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("serializes checkpoints before a later reset", async () => {
    let resolveCheckpoint: (() => void) | undefined;
    const checkpoint = new Promise<void>((resolve) => {
      resolveCheckpoint = resolve;
    });
    const post = vi.spyOn(ClusterApi, "postPersistData")
      .mockReturnValueOnce(checkpoint as ReturnType<typeof ClusterApi.postPersistData>)
      .mockResolvedValueOnce({} as Awaited<ReturnType<typeof ClusterApi.postPersistData>>);

    const checkpointRequest = postKerberosWizardPersistData("checkpoint");
    const resetRequest = postKerberosWizardPersistData("reset");
    await vi.waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post).toHaveBeenCalledWith("checkpoint");
    resolveCheckpoint?.();
    await checkpointRequest;
    await resetRequest;

    expect(post).toHaveBeenNthCalledWith(2, "reset");
  });
});
