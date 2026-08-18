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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
}));

vi.mock("../api/credentialsApi", () => ({
  default: {
    getCredentials: mocks.get,
    createCredentials: mocks.create,
    updateCredentials: mocks.update,
  },
}));

import credentialsUtils from "./credentialsUtils";

describe("credentialsUtils.createOrUpdateCredentials", () => {
  const resource = { principal: "admin", key: "secret", type: "temporary" };

  beforeEach(() => {
    vi.clearAllMocks();
    mocks.create.mockResolvedValue({});
    mocks.update.mockResolvedValue({});
  });

  it("waits for an existing credential update", async () => {
    mocks.get.mockResolvedValue({ Credential: { alias: "kdc" } });

    await credentialsUtils.createOrUpdateCredentials("c1", "kdc", resource);

    expect(mocks.update).toHaveBeenCalledOnce();
    expect(mocks.create).not.toHaveBeenCalled();
    expect(mocks.get.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.update.mock.invocationCallOrder[0],
    );
  });

  it("creates only after an explicit missing-credential response", async () => {
    mocks.get.mockRejectedValue({
      response: { status: 404, data: { message: "NoSuchResourceException" } },
    });

    await credentialsUtils.createOrUpdateCredentials("c1", "kdc", resource);

    expect(mocks.create).toHaveBeenCalledOnce();
    expect(mocks.update).not.toHaveBeenCalled();
  });

  it("does not create or replay after a credential lookup failure", async () => {
    const lookupError = new Error("credential API unavailable");
    mocks.get.mockRejectedValue(lookupError);

    await expect(
      credentialsUtils.createOrUpdateCredentials("c1", "kdc", resource),
    ).rejects.toBe(lookupError);
    expect(mocks.create).not.toHaveBeenCalled();
    expect(mocks.update).not.toHaveBeenCalled();
  });
});
