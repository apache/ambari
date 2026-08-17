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
import { ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";
import modalManager from "../store/ModalManager";
import credentialsUtils from "../Utils/credentialsUtils";
import InvalidKdcPopup from "./InvalidKdcPopup";

const renderDialog = (getKdcSessionState = vi.fn(), onCancel = vi.fn()) =>
  render(
    <AppContext.Provider
      value={
        { clusterName: "c1" } as unknown as ComponentProps<
          typeof AppContext.Provider
        >["value"]
      }
    >
      <InvalidKdcPopup
        getKdcSessionState={getKdcSessionState}
        onCancel={onCancel}
      />
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
    vi.spyOn(credentialsUtils, "isStorePersisted").mockResolvedValue(true);
    vi.spyOn(modalManager, "hide").mockImplementation(() => undefined);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("does not close or replay the protected request when credential storage fails", async () => {
    vi.spyOn(credentialsUtils, "createOrUpdateCredentials").mockRejectedValue(
      new Error("Credential write failed"),
    );
    const retryProtectedRequest = vi.fn();
    renderDialog(retryProtectedRequest);
    fillCredentials();

    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    expect(
      await screen.findByText(
        "Ambari could not save the KDC administrator credentials.",
      ),
    ).toBeTruthy();
    expect(modalManager.hide).not.toHaveBeenCalled();
    expect(retryProtectedRequest).not.toHaveBeenCalled();
  });

  it("closes and revalidates only after credentials are saved", async () => {
    vi.spyOn(credentialsUtils, "createOrUpdateCredentials").mockResolvedValue(
      {} as any,
    );
    const retryProtectedRequest = vi.fn().mockResolvedValue(undefined);
    renderDialog(retryProtectedRequest);
    fillCredentials();

    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    await waitFor(() => expect(modalManager.hide).toHaveBeenCalledTimes(1));
    expect(retryProtectedRequest).toHaveBeenCalledTimes(1);
  });

  it("reports cancellation to the waiting protected operation", () => {
    const onCancel = vi.fn();
    renderDialog(vi.fn(), onCancel);

    fireEvent.click(screen.getAllByRole("button", { name: "CANCEL" }).at(-1)!);

    expect(modalManager.hide).toHaveBeenCalledTimes(1);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
