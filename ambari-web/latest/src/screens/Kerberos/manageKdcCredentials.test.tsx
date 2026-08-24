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

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import credentialsUtils from "../../Utils/credentialsUtils";
import ManageKdcCredentials from "./manageKdcCredentials";

const modalState = vi.hoisted(() => ({ current: null as any }));

vi.mock("../../components/Modal", () => ({
  default: ({
    isOpen,
    modalBody,
    successCallback,
    options,
  }: any) => {
    modalState.current = { successCallback, options };
    return isOpen ? (
      <div role="dialog">
        {modalBody}
        {(options.extraButtons ?? []).map((button: any) => (
          <button
            key={button.text}
            onClick={button.onClick}
            disabled={button.disabled}
          >
            {button.text}
          </button>
        ))}
        <button
          onClick={successCallback}
          disabled={options.okButtonDisabled}
        >
          {options.okButtonText}
        </button>
      </div>
    ) : null;
  },
}));

vi.mock("../../components/ConfirmationModal", () => ({
  default: ({
    isOpen,
    modalBody,
    successCallback,
    okButtonText,
    isOkDisabled,
  }: any) =>
    isOpen ? (
      <div role="alertdialog">
        {modalBody}
        <button onClick={successCallback} disabled={isOkDisabled}>
          {okButtonText}
        </button>
      </div>
    ) : null,
}));

const renderDialog = (onClose = vi.fn()) =>
  render(
    <AppContext.Provider
      value={
        { clusterName: "c1" } as unknown as ComponentProps<
          typeof AppContext.Provider
        >["value"]
      }
    >
      <ManageKdcCredentials isOpen onClose={onClose} />
    </AppContext.Provider>,
  );

describe("ManageKdcCredentials", () => {
  beforeEach(() => {
    vi.spyOn(credentialsUtils, "credentials").mockImplementation(
      async (
        _clusterName: string,
        callback: Parameters<typeof credentialsUtils.credentials>[1],
      ) => {
        callback([]);
      },
    );
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("requires valid fields and stores management credentials as persisted", async () => {
    const onClose = vi.fn();
    const save = vi
      .spyOn(credentialsUtils, "createOrUpdateCredentials")
      .mockResolvedValue({} as any);
    renderDialog(onClose);

    const saveButton = screen.getByRole("button", { name: "SAVE" });
    expect(saveButton.hasAttribute("disabled")).toBe(true);
    fireEvent.change(screen.getByLabelText("Admin Principal"), {
      target: { value: "admin user" },
    });
    fireEvent.change(screen.getByLabelText("Admin Password"), {
      target: { value: "secret" },
    });
    expect(saveButton.hasAttribute("disabled")).toBe(true);

    fireEvent.change(screen.getByLabelText("Admin Principal"), {
      target: { value: "admin/admin.example.com@EXAMPLE.COM" },
    });
    await act(async () => {
      await modalState.current.successCallback();
    });

    await waitFor(() => {
      expect(save).toHaveBeenCalledWith(
        "c1",
        "kdc.admin.credential",
        {
          principal: "admin/admin.example.com@EXAMPLE.COM",
          key: "secret",
          type: "persisted",
        },
      );
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("keeps the dialog open and displays save failures", async () => {
    const onClose = vi.fn();
    vi.spyOn(credentialsUtils, "createOrUpdateCredentials").mockRejectedValue(
      new Error("Credential write failed"),
    );
    renderDialog(onClose);

    fireEvent.change(screen.getByLabelText("Admin Principal"), {
      target: { value: "admin@EXAMPLE.COM" },
    });
    fireEvent.change(screen.getByLabelText("Admin Password"), {
      target: { value: "secret" },
    });
    await act(async () => {
      await modalState.current.successCallback();
    });

    expect(
      await screen.findByText("Credential write failed"),
    ).toBeTruthy();
    expect(onClose).not.toHaveBeenCalled();
  });

  it("requires confirmation before removing stored credentials", async () => {
    vi.mocked(credentialsUtils.credentials).mockImplementation(
      async (
        _clusterName: string,
        callback: Parameters<typeof credentialsUtils.credentials>[1],
      ) => {
        callback([{ alias: "kdc.admin.credential", type: "persisted" }]);
      },
    );
    const onClose = vi.fn();
    const remove = vi
      .spyOn(credentialsUtils, "removeCredentials")
      .mockResolvedValue({} as any);
    renderDialog(onClose);

    const removeButton = await screen.findByRole("button", { name: "REMOVE" });
    fireEvent.click(removeButton);
    expect(remove).not.toHaveBeenCalled();

    const confirmationButtons = screen.getAllByRole("button", {
      name: "REMOVE",
    });
    fireEvent.click(confirmationButtons.at(-1)!);

    await waitFor(() => {
      expect(remove).toHaveBeenCalledWith("c1", "kdc.admin.credential");
    });
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
