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

export default function bytesToSize(
  bytes: number | undefined,
  precision = 0,
  parseType = "parseInt",
  multiplyBy = 1
) {
  if (bytes === undefined) {
    return "n/a";
  } else {
    let value = bytes * multiplyBy;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB", "PB"];
    let posttxt = 0;
    while (value >= 1024) {
      posttxt++;
      value = value / 1024;
    }
    if (value === 0) {
      precision = 0;
    }
    //@ts-expect-error
    const parsedValue = window[parseType](value);
    return parsedValue.toFixed(precision) + " " + sizes[posttxt];
  }
}

export function getCardinalityValue(cardinality: string, isMax: boolean) {
  if (cardinality) {
    const isOptional = cardinality.toString().split("-").length > 1;
    if (isOptional) {
      return parseInt(cardinality.split("-")[isMax ? 1 : 0]);
    } else {
      if (isMax)
        return /^\d+\+/.test(cardinality as string) || cardinality == "ALL"
          ? Infinity
          : parseInt(cardinality);
      return cardinality == "ALL"
        ? Infinity
        : parseInt(cardinality.toString().replace("+", ""));
    }
  } else {
    return 0;
  }
}

export function isHAComponentOnly(componentName: string) {
  return ["ZKFC", "JOURNALNODE"].includes(componentName);
}

export function to2DecimalPlaces(
  value: number | string,
  decimalPlaces = 2
): string {
  return Number(
    Math.floor(parseFloat(value + "e" + decimalPlaces)) + "e-" + decimalPlaces
  ).toFixed(decimalPlaces);
}
