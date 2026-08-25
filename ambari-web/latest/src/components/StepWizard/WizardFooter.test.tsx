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
import WizardFooter from "./WizardFooter";

const step = {
  label: "Review",
  completed: false,
  Component: null,
  canGoBack: true,
  isNextEnabled: false,
};

describe("WizardFooter cancellation", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("shows an asynchronous cancellation failure and allows retry", async () => {
    const onCancel = vi
      .fn()
      .mockRejectedValueOnce(new Error("checkpoint cleanup failed"))
      .mockResolvedValueOnce(undefined);
    render(
      <WizardFooter
        step={step}
        onBack={() => undefined}
        onNext={() => undefined}
        onCancel={onCancel}
        isNextEnabled={false}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "CANCEL" }));
    fireEvent.click(screen.getByTestId("confirm-ok-btn"));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "checkpoint cleanup failed",
    );
    expect(screen.getByTestId("confirmation-modal")).toBeTruthy();

    fireEvent.click(screen.getByTestId("confirm-ok-btn"));
    await waitFor(() =>
      expect(screen.queryByTestId("confirmation-modal")).toBeNull(),
    );
    expect(onCancel).toHaveBeenCalledTimes(2);
  });

  it("omits cancellation while a critical workflow phase is locked", () => {
    render(
      <WizardFooter
        step={step}
        onBack={() => undefined}
        onNext={() => undefined}
        isNextEnabled={false}
        showCancel={false}
      />,
    );

    expect(screen.queryByRole("button", { name: "CANCEL" })).toBeNull();
  });
});
