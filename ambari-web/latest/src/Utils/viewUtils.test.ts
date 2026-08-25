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

import { describe, expect, it, vi } from "vitest";
import {
  buildViewIframeSrc,
  findRegularViewInstance,
  findShortViewInstance,
  flattenVisibleViewInstances,
  generateViewUrl,
  openViewInstance,
  parseViewPath,
  viewRouteBreadcrumb,
  ViewInstance,
} from "./viewUtils";

const shortInstance: ViewInstance = {
  contextPath: "/proxy/views/TEZ/1.0/INSTANCE",
  description: "Tez jobs",
  iconPath: "/icons/tez.png",
  instanceName: "INSTANCE",
  label: "Tez",
  shortUrl: "tez",
  version: "1.0",
  viewName: "TEZ",
  visible: true,
};

describe("Ambari View utilities", () => {
  it("flattens every visible version and preserves server context paths", () => {
    const instances = flattenVisibleViewInstances({
      items: [{
        ViewInfo: { view_name: "TEZ" },
        versions: [{
          ViewVersionInfo: { label: "Tez version", version: "1.0" },
          instances: [
            { ViewInstanceInfo: {
              context_path: "/proxy/views/TEZ/1.0/A",
              instance_name: "A",
              short_url: "tez-a",
              visible: true,
            } },
            { ViewInstanceInfo: {
              context_path: "/views/TEZ/1.0/HIDDEN",
              instance_name: "HIDDEN",
              visible: false,
            } },
          ],
        }, {
          ViewVersionInfo: { version: "2.0" },
          instances: [{ ViewInstanceInfo: {
            context_path: "/views/TEZ/2.0/B",
            description: "Second",
            instance_name: "B",
            label: "Tez B",
            visible: true,
          } }],
        }],
      }],
    });

    expect(instances).toHaveLength(2);
    expect(instances[0]).toMatchObject({
      contextPath: "/proxy/views/TEZ/1.0/A",
      description: "No description",
      label: "Tez version",
      shortUrl: "tez-a",
    });
    expect(instances[1]).toMatchObject({ instanceName: "B", version: "2.0" });
  });

  it("prefers a short URL and encodes every route segment", () => {
    expect(generateViewUrl({ ...shortInstance, viewName: "TEZ Jobs", shortUrl: "my view" }))
      .toBe("#/main/view/TEZ%20Jobs/my%20view");
    expect(generateViewUrl({ ...shortInstance, shortUrl: "", instanceName: "A/B" }))
      .toBe("#/main/views/TEZ/1.0/A%2FB");
  });

  it("opens the selected View route in a separate browsing context", () => {
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    openViewInstance(shortInstance);
    expect(open).toHaveBeenCalledWith(
      "#/main/view/TEZ/tez",
      "_blank",
      "noopener,noreferrer",
    );
    open.mockRestore();
  });

  it("matches regular identities, proxy-prefixed context paths, and short names", () => {
    const regular = { ...shortInstance, shortUrl: "" };
    expect(findRegularViewInstance([regular], "TEZ", "1.0", "INSTANCE")).toBe(regular);
    expect(findRegularViewInstance(
      [{ ...regular, viewName: "legacy", version: "legacy", instanceName: "legacy" }],
      "TEZ",
      "1.0",
      "INSTANCE",
    )).toBeDefined();
    expect(findShortViewInstance([shortInstance], "TEZ", "tez")).toBe(shortInstance);
    expect(findShortViewInstance([shortInstance], "TEZ", "missing")).toBeUndefined();
  });

  it("uses the regular instance label and no short-route breadcrumb", () => {
    expect(viewRouteBreadcrumb(
      "/main/views/TEZ/1.0/INSTANCE",
      [shortInstance],
    )).toBe("Tez");
    expect(viewRouteBreadcrumb(
      "/main/views/TEZ/1.0/DELETED",
      [shortInstance],
    )).toBe("Views");
    expect(viewRouteBreadcrumb(
      "/main/view/TEZ/tez",
      [shortInstance],
    )).toBe("");
    expect(viewRouteBreadcrumb("/main/view", [shortInstance])).toBeUndefined();
  });

  it.each([
    ["", "", ""],
    ["", "nested/path", "nested/path"],
    ["?foo=bar&count=1", "", "?foo=bar&count=1"],
    ["?viewPath=%2Fuser%2Fadmin%2Faddress", "", "user/admin/address"],
    [
      "?viewPath=%2Fuser%2Fadmin%2Faddress&foo=bar&count=1",
      "",
      "user/admin/address?foo=bar&count=1",
    ],
    ["?viewPath=%2F%23%2Ftez-app%2Fapplication_1", "", "#/tez-app/application_1"],
    ["?foo=bar&viewPath=%2Fnested%2Fpage&count=1", "", "nested/page?foo=bar&count=1"],
    ["?notviewPath=%2Fwrong", "", "?notviewPath=%2Fwrong"],
  ])("parses an internal View path", (search, wildcard, expected) => {
    expect(parseViewPath(search, wildcard)).toBe(expected);
  });

  it("does not throw on invalid percent encoding", () => {
    expect(() => parseViewPath("?viewPath=%E0%A4%A")).not.toThrow();
  });

  it("uses the server context path on the current origin", () => {
    expect(buildViewIframeSrc(
      "https://ambari.example",
      "/gateway/default/views/TEZ/1.0/INSTANCE",
      "#/tez-app/application_1",
    )).toBe(
      "https://ambari.example/gateway/default/views/TEZ/1.0/INSTANCE/#/tez-app/application_1",
    );
  });
});
