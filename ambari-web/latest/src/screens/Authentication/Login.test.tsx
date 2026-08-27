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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ login: vi.fn() }));

vi.mock("../../store/UserContext", () => ({
  useUserContext: () => ({
    login: mocks.login,
    isAuthenticated: false,
    isLoading: false,
    loginError: null,
    loginMessage: null,
    acknowledgeLoginMessage: vi.fn(),
  }),
}));

import { Login } from "./Login";

describe("login form", () => {
  beforeEach(() => {
    mocks.login.mockReset();
    mocks.login.mockResolvedValue(true);
  });

  it("keeps username and password labels bound to distinct inputs", async () => {
    render(<MemoryRouter><Login isLocalLogin /></MemoryRouter>);

    const username = screen.getByLabelText("Username") as HTMLInputElement;
    const password = screen.getByLabelText("Password") as HTMLInputElement;

    expect(username).not.toBe(password);
    expect(username.type).toBe("text");
    expect(password.type).toBe("password");

    fireEvent.change(username, { target: { value: "operator" } });
    fireEvent.change(password, { target: { value: "secret" } });
    fireEvent.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => {
      expect(mocks.login).toHaveBeenCalledWith("operator", "secret");
    });
  });
});
