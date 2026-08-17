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
import CredentialsApi from "../api/credentialsApi";
import credentialsUtils from "./credentialsUtils";

vi.mock("../api/credentialsApi", () => ({
  default: {
    createCredentials: vi.fn(),
    getCredentials: vi.fn(),
    updateCredentials: vi.fn(),
    deleteCredentials: vi.fn(),
    listCredentials: vi.fn(),
    credentialsStoreInfo: vi.fn(),
  },
}));

describe("credentials utilities", () => {
  beforeEach(() => vi.clearAllMocks());

  it("waits for an existing credential update", async () => {
    vi.mocked(CredentialsApi.getCredentials).mockResolvedValue({} as any);
    vi.mocked(CredentialsApi.updateCredentials).mockResolvedValue({ saved: true } as any);

    await expect(credentialsUtils.createOrUpdateCredentials("c1", "alias", "resource"))
      .resolves.toEqual({ saved: true });
    expect(CredentialsApi.updateCredentials).toHaveBeenCalledOnce();
    expect(CredentialsApi.createCredentials).not.toHaveBeenCalled();
  });

  it("creates only after a 404 credential lookup", async () => {
    vi.mocked(CredentialsApi.getCredentials).mockRejectedValue({ response: { status: 404 } });
    vi.mocked(CredentialsApi.createCredentials).mockResolvedValue({ created: true } as any);

    await expect(credentialsUtils.createOrUpdateCredentials("c1", "alias", "resource"))
      .resolves.toEqual({ created: true });
    expect(CredentialsApi.createCredentials).toHaveBeenCalledOnce();
  });

  it.each([401, 403, 500])("propagates lookup status %s without writing", async (status) => {
    const error = { response: { status } };
    vi.mocked(CredentialsApi.getCredentials).mockRejectedValue(error);

    await expect(credentialsUtils.createOrUpdateCredentials("c1", "alias", "resource"))
      .rejects.toBe(error);
    expect(CredentialsApi.updateCredentials).not.toHaveBeenCalled();
    expect(CredentialsApi.createCredentials).not.toHaveBeenCalled();
  });

  it("returns credential-store support instead of losing the async result", async () => {
    vi.mocked(CredentialsApi.credentialsStoreInfo).mockResolvedValue({
      Clusters: {
        credential_store_properties: {
          "storage.persistent": "true",
          "storage.temporary": "false",
        },
      },
    } as any);

    await expect(credentialsUtils.isStorePersisted("c1")).resolves.toBe(true);
    await expect(credentialsUtils.isStoreTemporary("c1")).resolves.toBe(false);
  });
});
