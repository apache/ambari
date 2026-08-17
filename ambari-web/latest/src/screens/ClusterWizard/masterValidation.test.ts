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
  getMasterValidationMessages,
  masterValidationKey,
} from "./masterValidation";
import { Masters } from "./types/AssignMastersTypes";

const masters: Masters[] = [
  {
    component: "NAMENODE",
    display_name: "NameNode",
    hostName: "host1.example.com",
    host_id: 1,
    serviceId: "HDFS",
  },
  {
    component: "ZOOKEEPER_SERVER",
    display_name: "ZooKeeper Server",
    hostName: "host2.example.com",
    host_id: 2,
    serviceId: "ZOOKEEPER",
    isInstalled: true,
  },
];

describe("master validation issue mapping", () => {
  it("keeps only ERROR and WARN issues matching non-installed assignments", () => {
    const response = {
      resources: [
        {
          items: [
            {
              type: "host-component",
              level: "ERROR",
              host: "host1.example.com",
              "component-name": "NAMENODE",
              message: "Move NameNode",
            },
            {
              type: "host-component",
              level: "WARN",
              host: "host1.example.com",
              "component-name": "NAMENODE",
              message: "Review NameNode placement",
            },
            {
              type: "host-component",
              level: "ERROR",
              host: "host2.example.com",
              "component-name": "ZOOKEEPER_SERVER",
              message: "Already installed",
            },
            {
              type: "service",
              level: "ERROR",
              message: "General issue",
            },
            {
              type: "host-component",
              level: "WARN",
              host: "host3.example.com",
              "component-name": "JOURNALNODE",
              message: "Unmatched issue",
            },
          ],
        },
      ],
    };

    expect(getMasterValidationMessages(response, masters)).toEqual({
      [masterValidationKey("host1.example.com", "NAMENODE")]: {
        errorMessage: "Move NameNode",
        warnMessage: "Review NameNode placement",
      },
    });
  });

  it("returns no issues for an incomplete validation response", () => {
    expect(getMasterValidationMessages({}, masters)).toEqual({});
  });
});

