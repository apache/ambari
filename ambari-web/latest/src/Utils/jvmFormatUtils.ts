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

const splitQuotedParameters = (value: string): string[] => {
  const parameters: string[] = [];
  let current = "";
  let quote = "";
  let escaped = false;

  for (const character of value) {
    if (escaped) {
      current += character;
      escaped = false;
      continue;
    }
    if (character === "\\") {
      current += character;
      escaped = true;
      continue;
    }
    if (quote) {
      current += character;
      if (character === quote) quote = "";
      continue;
    }
    if (character === '"' || character === "'") {
      current += character;
      quote = character;
      continue;
    }
    if (/\s/.test(character)) {
      if (current) parameters.push(current);
      current = "";
      continue;
    }
    current += character;
  }
  if (current) parameters.push(current);
  return parameters;
};

const startsLikeJvmOption = (value: string) =>
  /^(?:-X|-D|-XX:|-server$|-client$|--add-(?:opens|exports)=)/.test(value);

export const formatParamsForDisplay = (
  value: string,
  _displayType?: string,
): string => splitQuotedParameters(String(value ?? "")).join("\n");

export const formatParamsForSave = (value: string): string =>
  splitQuotedParameters(String(value ?? "")).join(" ");

export const shouldUseMultilineFormatting = (
  value: string,
  displayType?: string,
): boolean => {
  if (
    ["content", "directories", "directory", "multiLine"].includes(
      String(displayType),
    )
  ) {
    return false;
  }
  const parameters = splitQuotedParameters(String(value ?? ""));
  return parameters.length > 1 && parameters.every(startsLikeJvmOption);
};
