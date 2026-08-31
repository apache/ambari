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
import { basename, humanSize, joinPath, parentPath, permissionBits, permissionFromBits } from "./format";

describe("Files View formatting", () => {
  it("keeps HDFS paths normalized at root and nested levels", () => {
    expect(basename("/user/ambari/data")).toBe("data");
    expect(parentPath("/user/ambari/data")).toBe("/user/ambari");
    expect(parentPath("/user")).toBe("/");
    expect(parentPath("/")).toBe("");
    expect(joinPath("/", "/new")).toBe("/new");
  });

  it("round trips symbolic permissions without losing the file type prefix", () => {
    const source = "drwxr-x---";
    const bits = permissionBits(source);
    expect(bits).toEqual([true, true, true, true, false, true, false, false, false]);
    bits[7] = true;
    expect(permissionFromBits(source, bits)).toBe("drwxr-x-w-");
  });

  it("formats file sizes at stable binary boundaries", () => {
    expect(humanSize(0)).toBe("0 B");
    expect(humanSize(1024)).toBe("1.0 KB");
    expect(humanSize(10 * 1024)).toBe("10 KB");
  });
});
