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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";

const mocks = vi.hoisted(() => ({
  hide: vi.fn(),
  saveCredentials: vi.fn(),
}));

vi.mock("../store/ModalManager", () => ({
  default: { show: vi.fn(), hide: mocks.hide },
}));
vi.mock("../Utils/credentialsUtils", () => ({
  default: {
    STORE_TYPES: { PERSISTENT: "persisted", TEMPORARY: "temporary" },
    ALIAS: { KDC_CREDENTIALS: "kdc.admin.credential" },
    createCredentialResource: vi.fn(() => ({})),
    createOrUpdateCredentials: mocks.saveCredentials,
  },
}));

import InvalidKdcPopup from "./InvalidKdcPopup";

describe("InvalidKdcPopup", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("fails the protected operation when credential persistence fails", async () => {
    const saveError = new Error("credential store unavailable");
    mocks.saveCredentials.mockRejectedValue(saveError);
    const onError = vi.fn();

    render(
      <AppContext.Provider value={{ clusterName: "c1" } as never}>
        <InvalidKdcPopup getKdcSessionState={vi.fn()} onError={onError} />
      </AppContext.Provider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    await waitFor(() => expect(onError).toHaveBeenCalledWith(saveError));
    expect(mocks.hide).toHaveBeenCalledOnce();
  });
});
