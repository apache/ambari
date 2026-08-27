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
import {
  formatParamsForDisplay,
  formatParamsForSave,
  shouldUseMultilineFormatting,
} from "./jvmFormatUtils";

describe("JVM parameter formatting", () => {
  it("formats JVM options without splitting quoted whitespace", () => {
    const value = '-Xmx1g -Dname="value with spaces" -XX:+UseG1GC';
    const displayed = [
      "-Xmx1g",
      '-Dname="value with spaces"',
      "-XX:+UseG1GC",
    ].join("\n");

    expect(shouldUseMultilineFormatting(value, "string")).toBe(true);
    expect(formatParamsForDisplay(value, "string")).toBe(displayed);
    expect(formatParamsForSave(displayed)).toBe(value);
  });

  it("does not rewrite ordinary or explicitly multiline content", () => {
    expect(shouldUseMultilineFormatting("first\nsecond", "string")).toBe(false);
    expect(shouldUseMultilineFormatting("-Xmx1g -Xms1g", "multiLine")).toBe(
      false,
    );
    expect(shouldUseMultilineFormatting("plain words", "string")).toBe(false);
  });
});
