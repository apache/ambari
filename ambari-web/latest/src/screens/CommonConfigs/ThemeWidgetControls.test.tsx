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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PropertyType } from "./types";
import {
  ThemeDirectoriesControl,
  ThemeDirectoryControl,
  ThemeLabelControl,
  ThemeListControl,
  ThemeRadioControl,
} from "./ThemeWidgetControls";
import {
  areThemeEntriesEditable,
  getUnsupportedThemeEntryValues,
  getThemeCheckboxState,
  getThemeWidgetEntries,
  isThemeCheckboxValueSupported,
  parseSelectionCardinality,
} from "./themeWidgetUtils";

const property = (
  value: unknown,
  propertyAttributes: Record<string, unknown> = {},
): PropertyType => ({
  propertyName: "choice",
  propertyDisplayname: "Choice",
  propertyValue: value,
  propertyAttributes,
  previousValue: String(value ?? ""),
  value,
  final: "false",
  isEditable: true,
});

describe("Ember-compatible Theme widget controls", () => {
  afterEach(cleanup);

  it("maps scalar and object entries with labels and descriptions", () => {
    const entries = getThemeWidgetEntries(
      property("one", {
        entries: ["one", { value: "two", label: "Second" }],
        entry_labels: ["First"],
        entry_descriptions: ["First description", "Second description"],
      }),
    );

    expect(entries).toEqual([
      {
        value: "one",
        label: "First",
        description: "First description",
      },
      {
        value: "two",
        label: "Second",
        description: "Second description",
      },
    ]);
  });

  it("honors both server and React spellings of entries_editable", () => {
    expect(
      areThemeEntriesEditable(property("", { entries_editable: false })),
    ).toBe(false);
    expect(
      areThemeEntriesEditable(property("", { entriesEditable: "false" })),
    ).toBe(false);
    expect(areThemeEntriesEditable(property(""))).toBe(true);
  });

  it("parses exact, range, unbounded, and ALL list cardinalities", () => {
    expect(parseSelectionCardinality("2")).toEqual({
      minimum: 2,
      maximum: 2,
    });
    expect(parseSelectionCardinality("1-3")).toEqual({
      minimum: 1,
      maximum: 3,
    });
    expect(parseSelectionCardinality("2+")).toEqual({
      minimum: 2,
      maximum: Number.POSITIVE_INFINITY,
    });
    expect(parseSelectionCardinality("ALL")).toEqual({
      minimum: Number.POSITIVE_INFINITY,
      maximum: Number.POSITIVE_INFINITY,
    });
  });

  it.each([
    ["true", undefined, true, "true", "false"],
    ["No", undefined, false, "Yes", "No"],
    ["YES", "boolean-inverted", false, "NO", "YES"],
    ["no", "boolean-inverted", true, "no", "yes"],
  ])(
    "maps Ember checkbox value %s with display type %s",
    (value, displayType, checked, checkedValue, uncheckedValue) => {
      expect(
        getThemeCheckboxState({
          ...property(value),
          displayType,
        }),
      ).toEqual({ checked, checkedValue, uncheckedValue });
    },
  );

  it("enforces list maximum selection and reports the minimum", () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <ThemeListControl
        property={property("one", {
          entries: ["one", "two", "three"],
          selection_cardinality: "2",
        })}
        onChange={onChange}
      />,
    );
    expect(screen.queryByText("Select at least 2 item(s).")).not.toBeNull();

    fireEvent.click(screen.getByRole("checkbox", { name: "two" }));
    expect(onChange).toHaveBeenLastCalledWith("one,two");

    rerender(
      <ThemeListControl
        property={property("one,two", {
          entries: ["one", "two", "three"],
          selection_cardinality: "2",
        })}
        onChange={onChange}
      />,
    );
    expect(
      (screen.getByRole("checkbox", { name: "three" }) as HTMLInputElement)
        .disabled,
    ).toBe(true);
  });

  it("preserves unsupported checkbox and entry values in raw controls", () => {
    expect(isThemeCheckboxValueSupported(property("custom"))).toBe(false);
    expect(isThemeCheckboxValueSupported(property("YES"))).toBe(true);
    expect(
      getUnsupportedThemeEntryValues(
        property("one,custom", { entries: ["one", "two"] }),
        true,
      ),
    ).toEqual(["custom"]);

    const onChange = vi.fn();
    const { rerender } = render(
      <ThemeListControl
        property={property("one,custom", { entries: ["one", "two"] })}
        onChange={onChange}
      />,
    );
    const listRawValue = screen.getByRole("textbox", { name: "Choice" });
    expect((listRawValue as HTMLInputElement).value).toBe("one,custom");
    fireEvent.change(listRawValue, { target: { value: "one,two" } });
    expect(onChange).toHaveBeenLastCalledWith("one,two");

    rerender(
      <ThemeRadioControl
        property={property("custom", { entries: ["one", "two"] })}
        onChange={onChange}
      />,
    );
    expect(
      (screen.getByRole("textbox", { name: "Choice" }) as HTMLInputElement)
        .value,
    ).toBe("custom");
  });

  it("renders radio, directory, and label widgets with their expected editability", () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <ThemeRadioControl
        property={property("one", {
          entries: ["one", "two"],
          entry_labels: ["First", "Second"],
        })}
        onChange={onChange}
      />,
    );
    fireEvent.click(screen.getByRole("radio", { name: "Second" }));
    expect(onChange).toHaveBeenCalledWith("two");

    rerender(
      <ThemeDirectoryControl property={property("/data")} onChange={onChange} />,
    );
    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "/srv/data" },
    });
    expect(onChange).toHaveBeenCalledWith("/srv/data");

    rerender(<ThemeLabelControl property={property("read only")} />);
    const label = screen.getByRole("textbox", {
      name: "Choice",
    }) as HTMLInputElement;
    expect(label.value).toBe("read only");
    expect(label.readOnly).toBe(true);
  });

  it("keeps Ember directory and directories controls structurally distinct", () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <ThemeDirectoryControl
        property={property("/srv/solr")}
        onChange={onChange}
      />,
    );
    const directory = screen.getByRole("textbox", {
      name: "Choice",
    }) as HTMLInputElement;
    expect(directory.tagName).toBe("INPUT");
    expect(directory.value).toBe("/srv/solr");

    rerender(
      <ThemeDirectoriesControl
        property={property("/data/one,/data/two")}
        onChange={onChange}
      />,
    );
    const directories = screen.getByRole("textbox", {
      name: "Choice",
    }) as HTMLTextAreaElement;
    expect(directories.tagName).toBe("TEXTAREA");
    expect(directories.rows).toBe(4);
    expect(directories.value).toBe("/data/one,/data/two");
    fireEvent.change(directories, {
      target: { value: "/data/one,/data/two,/data/three" },
    });
    expect(onChange).toHaveBeenLastCalledWith(
      "/data/one,/data/two,/data/three",
    );
  });
});
