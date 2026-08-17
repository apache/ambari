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
  parseCustomPropertyInput,
  validateCustomPropertyKey,
} from "./customConfigProperties";

describe("custom config properties", () => {
  it("parses multiple properties and preserves equals signs in values", () => {
    expect(
      parseCustomPropertyInput("first=value\nsecond=a=b=c")
    ).toEqual({
      properties: [
        { key: "first", value: "value" },
        { key: "second", value: "a=b=c" },
      ],
      errors: [],
    });
  });

  it("reports format, key, duplicate, and existing-property errors", () => {
    const result = parseCustomPropertyInput(
      "missing separator\n\n=empty\nbad key=value\nduplicate=1\nduplicate=2\nsaved=3",
      { saved: { value: "current", isVisible: true } }
    );

    expect(result.errors).toEqual([
      "Line 1: Invalid format. Expected key=value",
      "Line 3: Key cannot be empty",
      'Line 4: Invalid key "bad key". Only alphanumerics, hyphens, underscores, asterisks and periods are allowed.',
      'Line 6: Duplicate key "duplicate"',
      'Line 7: Property "saved" already exists',
    ]);
    expect(result.properties).toEqual([{ key: "duplicate", value: "1" }]);
  });

  it("allows a removed property to be added again", () => {
    const removed = { saved: { value: null, isVisible: false } };

    expect(validateCustomPropertyKey(" saved ", removed)).toBe("");
    expect(parseCustomPropertyInput("saved=new", removed).errors).toEqual([]);
    expect(validateCustomPropertyKey("saved", { saved: { value: "old" } }))
      .toBe('Property "saved" already exists');
  });
});
