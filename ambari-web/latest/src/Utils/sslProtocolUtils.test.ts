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
import { getServiceProtocol, setProtocol } from "./sslProtocolUtils";

const configurations = [
  {
    type: "hdfs-site",
    properties: {
      "dfs.http.policy": "HTTPS_ONLY",
      enabled: "true",
      present: "value",
    },
  },
];

describe("quick link protocol policy", () => {
  it("honors explicit HTTP-only and HTTPS-only descriptor policies", () => {
    expect(setProtocol(configurations, { type: "HTTP_ONLY" })).toBe("http");
    expect(setProtocol([], { type: "HTTPS_ONLY" })).toBe("https");
  });

  it("uses Hadoop SSL when the descriptor has no protocol policy", () => {
    expect(getServiceProtocol("HBASE", configurations)).toBe("https");
    expect(getServiceProtocol("HBASE", [])).toBe("http");
  });

  it("supports exact, existence, and non-existence checks", () => {
    expect(
      setProtocol(configurations, {
        type: "HTTPS",
        checks: [
          { site: "hdfs-site", property: "enabled", desired: "true" },
          { site: "hdfs-site", property: "present", desired: "EXIST" },
          { site: "hdfs-site", property: "missing", desired: "NOT_EXIST" },
        ],
      })
    ).toBe("https");
  });

  it("reverses the preferred protocol when any check fails", () => {
    expect(
      setProtocol(configurations, {
        type: "HTTPS",
        checks: [
          { site: "hdfs-site", property: "enabled", desired: "true" },
          { site: "missing-site", property: "missing", desired: "EXIST" },
        ],
      })
    ).toBe("http");
    expect(
      setProtocol(configurations, {
        type: "HTTP",
        checks: [
          { site: "hdfs-site", property: "enabled", desired: "false" },
        ],
      })
    ).toBe("https");
  });
});
