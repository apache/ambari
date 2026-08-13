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

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
}));

import LoginApi from "./loginApi";

describe("login API", () => {
  beforeEach(() => mocks.request.mockReset());

  it("sends UTF-8 Basic credentials without exposing them in the body", async () => {
    mocks.request.mockResolvedValue({});
    await LoginApi.authenticate("用户", "päss");

    expect(mocks.request).toHaveBeenCalledWith(expect.objectContaining({
      url: "/auth",
      method: "POST",
      headers: expect.objectContaining({
        Authorization: `Basic ${Buffer.from("用户:päss", "utf8").toString("base64")}`,
      }),
      skipAuthRedirect: true,
    }));
  });

  it("URL-encodes the username exactly once at the API boundary", async () => {
    mocks.request.mockResolvedValue({});
    await LoginApi.handleSuccessfulLogin({ usr: "", loginName: "user/name %" });
    expect(mocks.request).toHaveBeenCalledWith(expect.objectContaining({
      url: "/users/user%2Fname%20%25?fields=*,privileges/PrivilegeInfo/*",
    }));
  });
});
