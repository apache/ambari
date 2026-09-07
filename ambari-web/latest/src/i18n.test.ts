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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import english from "./locales/en/translation.json";
import chinese from "./locales/zh/translation.json";

function leaves(value: object, path: string[] = []): Record<string, string> {
  return Object.fromEntries(Object.entries(value).flatMap(([key, item]) => {
    const childPath = [...path, key];
    return typeof item === "string"
      ? [[JSON.stringify(childPath), item]]
      : Object.entries(leaves(item, childPath));
  }));
}

function placeholders(value: string) {
  return (value.match(/\{\{[^{}]+\}\}|\{[^{}]+\}|%(?:\d+\$)?[sdif]/g) || []).sort();
}

function markup(value: string) {
  const template = document.createElement("template");
  template.innerHTML = value;
  return Array.from(template.content.querySelectorAll("*"), (element) => {
    const parents = [];
    for (let parent = element.parentElement; parent; parent = parent.parentElement) {
      parents.push(parent.tagName);
    }
    return {
      tag: element.tagName,
      parents,
      attributes: Array.from(element.attributes, ({ name, value }) => [name, value]).sort(),
      code: element.matches(".code-snippet, pre, code") ? element.textContent?.trim() : undefined,
    };
  }).sort((left, right) => JSON.stringify(left).localeCompare(JSON.stringify(right)));
}

describe("Chinese translation integrity", () => {
  it("matches English keys, interpolation tokens and HTML structure", () => {
    const source = leaves(english);
    const translated = leaves(chinese);
    expect(Object.keys(translated).sort()).toEqual(Object.keys(source).sort());
    expect(chinese._License).toBe(english._License);

    for (const [key, value] of Object.entries(source)) {
      expect(typeof translated[key], key).toBe("string");
      if (value.trim()) expect(translated[key].trim(), key).not.toBe("");
      expect(placeholders(translated[key]), key).toEqual(placeholders(value));
      expect(markup(translated[key]), key).toEqual(markup(value));
    }
  });
});

describe("language selection", () => {
  beforeEach(() => {
    vi.resetModules();
    localStorage.clear();
    document.documentElement.lang = "en";
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function browserLanguage(language: string) {
    vi.spyOn(navigator, "languages", "get").mockReturnValue([language]);
    vi.spyOn(navigator, "language", "get").mockReturnValue(language);
  }

  it.each(["zh", "zh-CN", "zh-SG", "zh-Hans", "zh-TW", "zh-Hant"])(
    "loads Simplified Chinese for %s",
    async (language) => {
      browserLanguage(language);
      const { default: i18n } = await import("./i18n");
      expect(i18n.resolvedLanguage).toBe("zh");
      expect(i18n.t("login.header")).toBe(chinese["login.header"]);
      expect(document.documentElement.lang).toBe("zh-CN");
    },
  );

  it.each(["en-US", "fr-FR"])("uses English for %s", async (language) => {
    browserLanguage(language);
    const { default: i18n } = await import("./i18n");
    expect(i18n.resolvedLanguage).toBe("en");
    expect(i18n.t("login.header")).toBe(english["login.header"]);
    expect(document.documentElement.lang).toBe("en");
  });

  it("preserves an explicit selection across initialization", async () => {
    browserLanguage("zh-CN");
    const { default: i18n } = await import("./i18n");
    await i18n.changeLanguage("en");
    expect(localStorage.getItem("i18nextLng")).toBe("en");
    expect(document.documentElement.lang).toBe("en");

    vi.resetModules();
    const { default: restored } = await import("./i18n");
    expect(restored.resolvedLanguage).toBe("en");
  });

  it("ignores an unsupported saved language and uses the browser preference", async () => {
    localStorage.setItem("i18nextLng", "unsupported");
    browserLanguage("zh-CN");
    const { default: i18n } = await import("./i18n");
    expect(i18n.resolvedLanguage).toBe("zh");
  });

  it("works when browser storage cannot be accessed", async () => {
    browserLanguage("zh-CN");
    vi.spyOn(localStorage, "getItem").mockImplementation(() => {
      throw new DOMException("Storage unavailable", "SecurityError");
    });
    vi.spyOn(localStorage, "setItem").mockImplementation(() => {
      throw new DOMException("Storage unavailable", "SecurityError");
    });
    const { default: i18n } = await import("./i18n");
    expect(i18n.resolvedLanguage).toBe("zh");
    await i18n.changeLanguage("en");
    expect(i18n.resolvedLanguage).toBe("en");
  });

  it("falls back to English for missing Chinese keys and preserves interpolation", async () => {
    browserLanguage("zh-CN");
    const { default: i18n } = await import("./i18n");
    i18n.addResource("en", "translation", "test.englishOnly", "English fallback");
    expect(i18n.t("test.englishOnly")).toBe("English fallback");
    expect(i18n.t("app.hostMaintenance", { hostname: "host-1" }))
      .toBe(chinese["app.hostMaintenance"].replace("{{hostname}}", "host-1"));
  });
});
