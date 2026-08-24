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
import Service, { ServiceData } from "./service";

function service(name: string) {
  return new Service({ serviceName: name } as ServiceData);
}

describe("classic service type tags", () => {
  it("retains the hard-coded HDFS workflow tags", () => {
    expect(service("HDFS").serviceTypes).toEqual([
      "HA_MODE",
      "FEDERATION",
      "DFSRouter",
    ]);
  });

  it("assigns HA_MODE only to the other classic HA services", () => {
    ["YARN", "RANGER", "HAWQ"].forEach((name) => {
      expect(service(name).serviceTypes).toEqual(["HA_MODE"]);
    });
    expect(service("HIVE").serviceTypes).toEqual([]);
  });
});
