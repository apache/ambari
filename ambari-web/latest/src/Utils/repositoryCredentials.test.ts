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

import { describe, expect, it } from "vitest";
import { copyRepositoryCredentials } from "./repositoryCredentials";

describe("repository credential propagation", () => {
  it("copies encoded credentials without changing the target repository", () => {
    expect(copyRepositoryCredentials(
      "https://repo.example/rhel/8",
      "https://user:p%40ss@source.example/base",
    )).toBe("https://user:p%40ss@repo.example/rhel/8");
  });

  it("clears target credentials when the source has none", () => {
    expect(copyRepositoryCredentials(
      "https://old:secret@repo.example/rhel/8",
      "https://source.example/base",
    )).toBe("https://repo.example/rhel/8");
  });

  it("leaves the target unchanged when either URL is invalid", () => {
    expect(copyRepositoryCredentials(
      "not a target URL",
      "https://user:secret@source.example/base",
    )).toBe("not a target URL");
    expect(copyRepositoryCredentials(
      "https://repo.example/base",
      "not a source URL",
    )).toBe("https://repo.example/base");
  });
});
