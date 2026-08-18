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
import { createKerberosPreconditionOptions } from "./constants";

describe("Kerberos Step 1 preconditions", () => {
  it("matches the Classic MIT, AD, IPA, and Manual prerequisite counts", () => {
    const options = createKerberosPreconditionOptions([]);

    expect(Object.keys(options["Existing MIT KDC"].Options)).toHaveLength(3);
    expect(Object.keys(options["Existing Active Directory"].Options)).toHaveLength(5);
    expect(Object.keys(options["Existing IPA"].Options)).toHaveLength(3);
    expect(Object.keys(
      options["Manage Kerberos principals and keytabs manually"].Options,
    )).toHaveLength(5);
  });

  it("adds the conditional OneFS prerequisite only when ONEFS is installed", () => {
    const withoutOneFs = createKerberosPreconditionOptions(["HDFS"]);
    const withOneFs = createKerberosPreconditionOptions(["HDFS", "ONEFS"]);
    const condition =
      "The Isilon administrator has setup all appropriate principals in OneFS";

    expect(withoutOneFs["Existing MIT KDC"].Options).not.toHaveProperty(condition);
    expect(withOneFs["Existing MIT KDC"].Options).toHaveProperty(condition, false);
  });
});
