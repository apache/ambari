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
import { ConfigPropertiesType } from "../screens/CommonConfigs/types";
import {
  addRecommendedConfigProperty,
  hasConfigProperty,
} from "./recommendedConfigProperty";

const hdfsConfigs = (): ConfigPropertiesType => ({
  HDFS: {
    "core-site": {
      errors: 0,
      properties: {
        "fs.defaultFS": {
          propertyName: "fs.defaultFS",
          propertyDisplayname: "fs.defaultFS",
          propertyValue: "hdfs://nn:8020",
          propertyAttributes: {},
          previousValue: "hdfs://nn:8020",
          value: "hdfs://nn:8020",
          final: "false",
          type: "core-site",
          isEditable: true,
        },
      },
    },
    "Custom core-site": { errors: 0, properties: {}, displayName: "Custom core-site" },
  },
});

describe("addRecommendedConfigProperty", () => {
  it("adds a recommended property the stack does not define to the custom section", () => {
    const configs = hdfsConfigs();

    const added = addRecommendedConfigProperty(
      configs, "HDFS", "core-site", "hadoop.proxyuser.hive.hosts", "hs2.example",
    );

    expect(added).toBe(true);
    expect(configs.HDFS["Custom core-site"].properties["hadoop.proxyuser.hive.hosts"]).toMatchObject({
      propertyName: "hadoop.proxyuser.hive.hosts",
      value: "hs2.example",
      recommendedValue: "hs2.example",
      initialValue: null,
      type: "core-site",
      fileName: "core-site.xml",
      serviceName: "HDFS",
      isUserProperty: true,
      isNotSaved: true,
    });
  });

  it("creates the custom section when the configuration does not have one", () => {
    const configs = hdfsConfigs();
    delete configs.HDFS["Custom core-site"];

    addRecommendedConfigProperty(configs, "HDFS", "core-site", "hadoop.proxyuser.hdfs.groups", "*");

    expect(configs.HDFS["Custom core-site"]).toMatchObject({ errors: 0, displayName: "Custom core-site" });
    expect(configs.HDFS["Custom core-site"].properties["hadoop.proxyuser.hdfs.groups"].value).toBe("*");
  });

  it("does not duplicate a property that already exists in any section", () => {
    const configs = hdfsConfigs();

    expect(addRecommendedConfigProperty(configs, "HDFS", "core-site", "fs.defaultFS", "hdfs://other:8020")).toBe(false);
    expect(configs.HDFS["Custom core-site"].properties).toEqual({});
    expect(configs.HDFS["core-site"].properties["fs.defaultFS"].value).toBe("hdfs://nn:8020");
  });

  it("matches existing properties by config type, not only by name", () => {
    const configs = hdfsConfigs();

    expect(hasConfigProperty(configs, "fs.defaultFS", "core-site")).toBe(true);
    expect(hasConfigProperty(configs, "fs.defaultFS", "hdfs-site")).toBe(false);
  });

  it("ignores services that are not part of the configuration and absent values", () => {
    const configs = hdfsConfigs();

    expect(addRecommendedConfigProperty(configs, "OOZIE", "oozie-site", "oozie.x", "1")).toBe(false);
    expect(addRecommendedConfigProperty(configs, "HDFS", "core-site", "hadoop.proxyuser.x.hosts", null)).toBe(false);
    expect(addRecommendedConfigProperty(configs, "HDFS", "core-site", "hadoop.proxyuser.x.hosts", undefined)).toBe(false);
    expect(configs.OOZIE).toBeUndefined();
    expect(configs.HDFS["Custom core-site"].properties).toEqual({});
  });
});
