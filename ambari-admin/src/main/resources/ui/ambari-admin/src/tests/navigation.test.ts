/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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
import {
  ambariApplicationRoot,
  latestAmbariUrl,
  latestShortViewUrl,
  latestViewInstanceUrl,
} from "../utils/navigation";

describe("Ambari experience navigation", () => {
  it("returns to the React application from a root Admin View", () => {
    const path = "/views/ADMIN_VIEW/3.1.0.0/INSTANCE/latest/";
    expect(ambariApplicationRoot(path)).toBe("/");
    expect(latestAmbariUrl("/main/views/FILES/1.0.0/INSTANCE", path))
      .toBe("/latest/#/main/views/FILES/1.0.0/INSTANCE");
  });

  it("preserves a reverse-proxy application prefix", () => {
    const path = "/gateway/default/ambari/views/ADMIN_VIEW/3.1.0.0/INSTANCE/latest/index.html";
    expect(ambariApplicationRoot(path)).toBe("/gateway/default/ambari/");
    expect(latestAmbariUrl("main/dashboard", path))
      .toBe("/gateway/default/ambari/latest/#/main/dashboard");
  });

  it("encodes regular and short View route parameters", () => {
    const path = "/views/ADMIN_VIEW/3.1.0.0/INSTANCE/latest/";
    expect(latestViewInstanceUrl("FILES", "1.0.0", "User Files", path))
      .toBe("/latest/#/main/views/FILES/1.0.0/User%20Files");
    expect(latestShortViewUrl("CAPACITY-SCHEDULER", "queue editor", path))
      .toBe("/latest/#/main/view/CAPACITY-SCHEDULER/queue%20editor");
  });
});
