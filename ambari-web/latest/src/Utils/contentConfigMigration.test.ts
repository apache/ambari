/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import { migrateConfigContent, serializeConfigContent } from "./contentConfigMigration";
import type { ConfigPropertiesType } from "../screens/CommonConfigs/types";

function fixture(format = "properties") {
  return {
    SVC: {
      site: { errors: 0, properties: { content: {
        propertyName: "content", propertyDisplayname: "Configuration",
        propertyValue: "default=value", propertyAttributes: { type: "content", "property-file-type": format },
        value: null, previousValue: null, final: "false", type: "site",
        foundInPropertyValues: false, isVisible: false, isHidden: true, isEditable: true,
      } } },
      "Custom site": { errors: 0, properties: { existing: {
        propertyName: "existing", propertyDisplayname: "existing",
        propertyValue: "user value", propertyAttributes: {}, value: "user value",
        previousValue: "user value", final: "false", type: "site", foundInPropertyValues: true, isEditable: true,
      } } },
    },
  } as ConfigPropertiesType;
}
const versions = () => ({ items: [{ service_name: "SVC", group_name: "Default",
  service_config_version: 7, configurations: [{ type: "site", properties: { existing: "user value" } }] }] });

describe("content configuration migration", () => {
  it("uses current values and removes their obsolete scalar counterparts only on save", () => {
    const source = fixture();
    const result = migrateConfigContent(source, versions(), true);
    expect(result.SVC.site.properties.content.value).toContain("existing=user\\ value");
    expect(result.SVC.site.properties.content.isVisible).toBe(true);
    expect(result.SVC.site.properties.content.foundInPropertyValues).toBe(false);
    expect(result.SVC["Custom site"].properties.existing.value).toBeNull();
    expect(source.SVC["Custom site"].properties.existing.value).toBe("user value");
  });
  it("does not rewrite historical versions or already saved content", () => {
    const source = fixture();
    expect(migrateConfigContent(source, versions(), false)).toBe(source);
    const saved = versions();
    saved.items[0].configurations[0].properties = { content: "# user comment\nx=value\n" } as any;
    expect(migrateConfigContent(source, saved, true)).toEqual(source);
  });
  it("does not modify another service loaded for dependency recommendations", () => {
    const source = fixture();
    source.OTHER = structuredClone(source.SVC);
    const values = versions();
    values.items.push({ ...structuredClone(values.items[0]), service_name: "OTHER" });
    const result = migrateConfigContent(source, values, true, "SVC");
    expect(result.OTHER).toEqual(source.OTHER);
    expect(result.SVC.site.properties.content.value).toContain("existing=");
  });
  it("requires one matching version and declared format and protects masked credentials", () => {
    expect(migrateConfigContent(fixture(), { items: [] }, true)).toEqual(fixture());
    expect(migrateConfigContent(fixture(), { items: [...versions().items, ...versions().items] }, true)).toEqual(fixture());
    expect(migrateConfigContent(fixture("unknown"), versions(), true)).toEqual(fixture("unknown"));
    const empty = versions();
    empty.items[0].configurations[0].properties = {} as any;
    expect(migrateConfigContent(fixture(), empty, true)).toEqual(fixture());
    const secret = versions();
    Object.assign(secret.items[0].configurations[0], { properties_attributes: { password: { existing: "true" } } });
    expect(migrateConfigContent(fixture(), secret, true)).toEqual(fixture());
  });
  it("serializes properties escapes without changing application expressions", () => {
    const text = serializeConfigContent("properties", { path: "a\\b c\nx", label: "\u4e2d", expr: "${ENV:VALUE}" });
    expect(text).toBe("path=a\\\\b\\ c\\nx\nlabel=\\u4e2d\nexpr=${ENV:VALUE}\n");
  });
  it("retains scalar host overrides until the group and its default can be migrated together", () => {
    const source = fixture();
    source.SVC["Custom site"].properties.existing.overrideValues = [{ groupName: "Blue", value: "group value", previousValue: "group value" }] as any;
    expect(migrateConfigContent(source, versions(), true)).toEqual(source);
  });
  it("serializes native YAML, INI and literal environment assignments", () => {
    expect(serializeConfigContent("yaml", { "node.attr.rack": "rack: 1" })).toBe('"node.attr.rack": "rack: 1"\n');
    expect(serializeConfigContent("ini", { "core.load_examples": "False" })).toBe("[core]\nload_examples = False\n");
    expect(serializeConfigContent("environment", { JAVA_OPTS: "-Xmx1g -Dname=$NAME" })).toBe("JAVA_OPTS='-Xmx1g -Dname=$NAME'\n");
    expect(serializeConfigContent("ini", { invalid: "value" })).toBeNull();
  });
});
