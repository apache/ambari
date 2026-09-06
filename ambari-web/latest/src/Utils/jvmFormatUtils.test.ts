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
  it("does not treat single-line JVM/command-line argument strings as multiline", () => {
    // e.g. mapreduce.admin.map.child.java.opts — must stay single-line, or the
    // injected display newlines get persisted and corrupt the config.
    const value =
      "-server -XX:NewRatio=8 -Djava.net.preferIPv4Stack=true -Dhdp.version=${hdp.version}";

    expect(shouldUseMultilineFormatting(value)).toBe(false);
    expect(formatParamsForDisplay(value)).toBe(value);
    expect(formatParamsForSave(value)).toBe(value);
  });

  it("honors an explicit multiLine displayType regardless of content", () => {
    expect(shouldUseMultilineFormatting("-Xmx1g -Xms1g", "multiLine")).toBe(true);
  });

  it("does not treat content as multiline when displayType says otherwise", () => {
    expect(shouldUseMultilineFormatting("first\nsecond", "string")).toBe(false);
  });

  it("falls back to content-based detection when no displayType is given", () => {
    expect(shouldUseMultilineFormatting("first\nsecond")).toBe(true);
    expect(shouldUseMultilineFormatting("first\\nsecond")).toBe(true);
    expect(shouldUseMultilineFormatting("plain words")).toBe(false);
  });

  it("converts escaped newlines for display and leaves real newlines for save", () => {
    expect(formatParamsForDisplay("first\\nsecond")).toBe("first\nsecond");
    expect(formatParamsForSave("first\nsecond")).toBe("first\nsecond");
  });
});
