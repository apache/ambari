/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
  candidateCanBePlanned,
  choiceCanContinue,
  localDependencyDefaults,
} from "./managedDependencySelection";

describe("managed dependency selection", () => {
  it("defaults both HBase dependencies to their local services", () => {
    expect(localDependencyDefaults({})).toEqual({
      HDFS: { mode: "local" },
      ZOOKEEPER: { mode: "local" },
    });
  });

  it("allows an incomplete draft to reach its editable version or security step", () => {
    const issue = {
      code: "DEPENDENCY_SECURITY_PLAN_INCOMPLETE",
      message: "Realm required",
    };
    expect(candidateCanBePlanned({
      cluster_id: 4,
      cluster_name: "provider",
      compatible: false,
      errors: [issue],
      service_name: "HDFS",
    })).toBe(true);
    expect(choiceCanContinue({
      mode: "managed",
      planningIssue: issue,
      provider: {
        cluster_id: 4,
        cluster_name: "provider",
        compatible: false,
        errors: [issue],
        service_name: "HDFS",
      },
    }, false)).toBe(true);
  });

  it("does not treat an unavailable or unreviewed managed provider as ready", () => {
    expect(choiceCanContinue({ mode: "managed" }, true)).toBe(false);
    expect(candidateCanBePlanned({
      cluster_id: 4,
      cluster_name: "provider",
      compatible: false,
      errors: [{ code: "DEPENDENCY_VERSION_UNSUPPORTED", message: "Unsupported" }],
      service_name: "HDFS",
    })).toBe(false);
  });
});
