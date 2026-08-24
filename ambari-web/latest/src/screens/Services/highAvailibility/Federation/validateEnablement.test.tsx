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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  hostState: {
    hostComponents: [],
    serviceComponents: [],
    isLoading: false,
    error: "",
    retry: vi.fn(),
  },
}));

vi.mock("../../../ClusterWizard/hooks/useHostComponents", () => ({
  default: () => mocks.hostState,
}));
vi.mock("../../../../components/Modal", () => ({
  default: ({ modalBody }: { modalBody: React.ReactNode }) => <div>{modalBody}</div>,
}));
vi.mock("../../../../hooks/useStepWizard", () => ({ default: () => ({}) }));

import ValidateEnablement from "./validateEnablement";

describe("NameNode Federation prerequisite reads", () => {
  beforeEach(() => {
    mocks.hostState.retry.mockReset();
  });
  afterEach(cleanup);

  it.each(["stack services failed", "host components failed"])(
    "shows a Retry action when %s",
    (message) => {
      mocks.hostState.error = message;
      render(<ValidateEnablement />);
      expect(screen.getByText(message)).toBeTruthy();
      fireEvent.click(screen.getByRole("button", { name: "Retry" }));
      expect(mocks.hostState.retry).toHaveBeenCalledOnce();
    },
  );
});
