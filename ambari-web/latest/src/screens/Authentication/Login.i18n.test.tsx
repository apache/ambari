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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import i18n from "../../i18n";
import chinese from "../../locales/zh/translation.json";
import { Login } from "./Login";

const auth = vi.hoisted(() => ({
  login: vi.fn(),
  isAuthenticated: false,
  isLoading: false,
  loginError: "",
  loginMessage: null,
}));

vi.mock("../../store/UserContext", () => ({
  useUserContext: () => auth,
}));
vi.mock("./LoginMessageModal", () => ({ default: () => null }));

function loginPage(isLocalLogin = false, route = "/login") {
  return <MemoryRouter initialEntries={[route]}><Login isLocalLogin={isLocalLogin} /></MemoryRouter>;
}

describe("localized login", () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    auth.isLoading = false;
    auth.loginError = "";
    auth.login.mockResolvedValue(false);
    await i18n.changeLanguage("en");
  });

  afterEach(cleanup);

  it("switches language without discarding entered credentials or submitting the form", async () => {
    render(loginPage());
    fireEvent.change(screen.getByLabelText("Username"), { target: { value: "operator" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "test-password" } });
    fireEvent.change(screen.getByRole("combobox", { name: "Language" }), {
      target: { value: "zh" },
    });

    expect(await screen.findByRole("heading", { name: chinese["login.header"] })).toBeTruthy();
    expect((screen.getByLabelText(chinese["login.username"]) as HTMLInputElement).value).toBe("operator");
    expect((screen.getByLabelText(chinese["common.password"]) as HTMLInputElement).value).toBe("test-password");
    expect(localStorage.getItem("i18nextLng")).toBe("zh");
    expect(document.documentElement.lang).toBe("zh-CN");
    expect(auth.login).not.toHaveBeenCalled();

    fireEvent.change(screen.getByRole("combobox"), { target: { value: "en" } });
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeTruthy();
    expect((screen.getByLabelText("Username") as HTMLInputElement).value).toBe("operator");
  });

  it("localizes a failed submission and updates it when the language changes", async () => {
    await i18n.changeLanguage("zh");
    render(loginPage());
    fireEvent.click(screen.getByRole("button", { name: chinese["login.loginButton"] }));
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.getByRole("alert").textContent).toBe(chinese["login.error.failed"]);

    fireEvent.change(screen.getByRole("combobox"), { target: { value: "en" } });
    await waitFor(() => expect(screen.getByRole("alert").textContent)
      .toBe("Login failed. Please check your credentials."));
  });

  it("translates known authentication errors and retains server-provided messages", async () => {
    auth.loginError = "login.error.invalidCredentials";
    await i18n.changeLanguage("zh");
    const view = render(loginPage());
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.getByRole("alert").textContent).toBe(chinese["login.error.invalidCredentials"]);

    auth.loginError = "Server-specific diagnostic";
    view.rerender(loginPage());
    expect(screen.getByRole("alert").textContent).toBe("Server-specific diagnostic");
  });

  it("renders local recovery and the pending state in Chinese", async () => {
    await i18n.changeLanguage("zh");
    auth.isLoading = true;
    render(loginPage(true, "/login/local?redirectError=1"));
    expect(screen.getByRole("alert").textContent).toBe(chinese["login.externalAuthError"]);
    expect((screen.getByRole("button", { name: chinese["login.signingIn"] }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.queryByRole("link")).toBeNull();
  });

  it("translates unexpected login errors", async () => {
    await i18n.changeLanguage("zh");
    auth.login.mockRejectedValue(new Error("Network unavailable"));
    render(loginPage());
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: chinese["login.loginButton"] }));
    });
    expect(screen.getByRole("alert").textContent).toBe(chinese["login.error.unexpected"]);
  });
});
