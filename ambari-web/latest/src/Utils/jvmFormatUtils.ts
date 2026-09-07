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

// Checks if a string contains only a single line. Also treats an escaped
// newline sequence from the backend as a newline.
const isSingleLine = (value: string): boolean => {
  const stringValue = String(value).trim();
  return stringValue.indexOf("\n") === -1 && stringValue.indexOf("\\n") === -1;
};

// Ember's logic: an explicit displayType from the backend takes priority;
// otherwise fall back to content-based detection. Values that merely look
// like command-line/JVM arguments (e.g. mapreduce.admin.map.child.java.opts:
// "-server -XX:NewRatio=8 -Djava.net.preferIPv4Stack=true -Dhdp.version=${hdp.version}")
// must NOT be treated as multiline just because they contain multiple "-X"-style
// tokens — only genuine (real or escaped) newlines should.
export const shouldUseMultilineFormatting = (
  value: string,
  displayType?: string,
): boolean => {
  if (!value || typeof value !== "string") {
    return false;
  }
  if (displayType) {
    return displayType === "multiLine";
  }
  return !isSingleLine(value);
};

export const formatParamsForDisplay = (
  value: string,
  displayType?: string,
): string => {
  if (!value || typeof value !== "string") {
    return value;
  }
  if (!shouldUseMultilineFormatting(value, displayType)) {
    return value;
  }
  // Already has real newlines; let the textarea render it naturally.
  if (value.includes("\n")) {
    return value;
  }
  // Convert escaped newline sequences to actual newlines for display.
  if (value.includes("\\n")) {
    return value.replace(/\\n/g, "\n");
  }
  return value;
};

// Leaves real newlines as real newlines — JSON.stringify() will escape them
// when the save payload is serialized. Escaping here too would double-escape
// (\n -> \\n -> \\\\n).
export const formatParamsForSave = (value: string): string => {
  if (!value || typeof value !== "string") {
    return value;
  }
  if (value.includes("\\n")) {
    return value;
  }
  return value;
};
