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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";

const mocks = vi.hoisted(() => ({
  hide: vi.fn(),
  isStorePersisted: vi.fn(),
  saveCredentials: vi.fn(),
}));

vi.mock("../store/ModalManager", () => ({
  default: { show: vi.fn(), hide: mocks.hide },
}));
vi.mock("../Utils/credentialsUtils", () => ({
  default: {
    STORE_TYPES: { PERSISTENT: "persisted", TEMPORARY: "temporary" },
    ALIAS: { KDC_CREDENTIALS: "kdc.admin.credential" },
    createCredentialResource: vi.fn((principal, key, type) => ({
      principal,
      key,
      type,
    })),
    createOrUpdateCredentials: mocks.saveCredentials,
    isStorePersisted: mocks.isStorePersisted,
  },
}));

import InvalidKdcPopup from "./InvalidKdcPopup";

const renderDialog = (props = {}) =>
  render(
    <AppContext.Provider value={{ clusterName: "c1" } as never}>
      <InvalidKdcPopup {...props} />
    </AppContext.Provider>,
  );

const fillCredentials = () => {
  fireEvent.change(screen.getByLabelText("Admin Principal"), {
    target: { value: "admin@EXAMPLE.COM" },
  });
  fireEvent.change(screen.getByLabelText("Admin password"), {
    target: { value: "secret" },
  });
};

describe("InvalidKdcPopup", () => {
  beforeEach(() => {
    mocks.isStorePersisted.mockResolvedValue(true);
    mocks.saveCredentials.mockResolvedValue({});
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("does not close or replay the protected request when credential storage fails", async () => {
    mocks.saveCredentials.mockRejectedValue(new Error("Credential write failed"));
    const retryProtectedRequest = vi.fn();
    renderDialog({ getKdcSessionState: retryProtectedRequest });
    fillCredentials();

    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    expect(
      await screen.findByText(
        "Ambari could not save the KDC administrator credentials.",
      ),
    ).toBeTruthy();
    expect(mocks.hide).not.toHaveBeenCalled();
    expect(retryProtectedRequest).not.toHaveBeenCalled();
  });

  it("closes and revalidates only after credentials are saved", async () => {
    const retryProtectedRequest = vi.fn().mockResolvedValue(undefined);
    renderDialog({ getKdcSessionState: retryProtectedRequest });
    fillCredentials();

    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    await waitFor(() => expect(mocks.hide).toHaveBeenCalledOnce());
    expect(retryProtectedRequest).toHaveBeenCalledOnce();
  });

  it("reports cancellation to the waiting protected operation", () => {
    const onCancel = vi.fn();
    renderDialog({ onCancel });

    fireEvent.click(screen.getAllByRole("button", { name: "CANCEL" }).at(-1)!);

    expect(mocks.hide).toHaveBeenCalledOnce();
    expect(onCancel).toHaveBeenCalledWith(expect.any(Error));
  });

  it("fails the protected operation through its error callback", async () => {
    const saveError = new Error("credential store unavailable");
    mocks.saveCredentials.mockRejectedValue(saveError);
    const onError = vi.fn();
    renderDialog({ getKdcSessionState: vi.fn(), onError });
    fillCredentials();

    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    await waitFor(() => expect(onError).toHaveBeenCalledWith(saveError));
    expect(mocks.hide).toHaveBeenCalledOnce();
  });
});
