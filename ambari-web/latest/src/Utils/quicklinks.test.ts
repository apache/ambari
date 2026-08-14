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
import {
  createPublicHostNameMap,
  resolveQuicklinkConfigPlaceholders,
  substituteQuicklinkTemplate,
} from "./quicklinks";

describe("quick link resolution", () => {
  it("substitutes protocol, public host, port, and the logged-in user", () => {
    expect(
      substituteQuicklinkTemplate(
        "%@://%@:%@/login?user=%@",
        "https",
        "public.example",
        "8443",
        "operator",
        true
      )
    ).toBe("https://public.example:8443/login?user=operator");
  });

  it("resolves config references and preserves unknown references", () => {
    expect(
      resolveQuicklinkConfigPlaceholders(
        "https://host:${core-site/http.port}/${missing/value}",
        [{ type: "core-site", properties: { "http.port": 9870 } }]
      )
    ).toBe("https://host:9870/${missing/value}");
  });

  it("maps internal hosts to public hosts without inventing missing values", () => {
    expect(
      createPublicHostNameMap([
        {
          Hosts: {
            host_name: "internal-1",
            public_host_name: "public-1",
          },
        },
        { Hosts: { host_name: "internal-2" } },
      ])
    ).toEqual(new Map([["internal-1", "public-1"]]));
  });
});
