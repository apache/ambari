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
import {
  getExternalLoginUrl,
  handleAuthenticationError,
  normalizeInternalPath,
  SESSION_EXPIRED_EVENT,
} from "./authNavigation";

describe("authentication navigation", () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    window.location.hash = "#/login";
  });

  it.each([
    ["/main/hosts", "/main/hosts"],
    ["#/main/hosts?tab=summary", "/main/hosts?tab=summary"],
    ["/#/installer/step0", "/installer/step0"],
    ["//attacker.example/path", null],
    ["https://attacker.example/path", null],
    ["/login", null],
    ["/login/local", null],
    ["/", null],
  ])("normalizes %s", (input, expected) => {
    expect(normalizeInternalPath(input)).toBe(expected);
  });

  it("supports external login while preserving the return URL", () => {
    expect(getExternalLoginUrl(
      "https://knox.example/sso?originalUrl=",
      "https://ambari.example/#/main/hosts",
      false,
    )).toBe(
      "https://knox.example/sso?originalUrl="
      + encodeURIComponent("https://ambari.example/#/main/hosts"),
    );
  });

  it("does not redirect local login and caps external redirect loops", () => {
    expect(getExternalLoginUrl("https://knox.example/", "return", true)).toBeNull();
    expect(getExternalLoginUrl("https://knox.example/", "return", false)).not.toBeNull();
    expect(getExternalLoginUrl("https://knox.example/", "return", false)).not.toBeNull();
    expect(getExternalLoginUrl("https://knox.example/", "return", false)).not.toBeNull();
    expect(getExternalLoginUrl("https://knox.example/", "return", false)).toBeNull();
  });

  it("expires the client session for 401 but not a plain authorization 403", () => {
    const listener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, listener);

    expect(handleAuthenticationError({ status: 403, data: {} })).toBe(false);
    expect(listener).not.toHaveBeenCalled();

    expect(handleAuthenticationError({ status: 401, data: {} })).toBe(false);
    expect(listener).toHaveBeenCalledOnce();
    window.removeEventListener(SESSION_EXPIRED_EVENT, listener);
  });
});
