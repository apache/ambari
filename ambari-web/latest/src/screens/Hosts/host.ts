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

import { get, isArray, isEmpty } from "lodash";
import { SelectedFilters } from "./HostComboSearch";
import { hostFilterProperties } from "./constants";

export const getQueryParameters = (selectedFilters: SelectedFilters) => {
  let queryParams: any[] = [];
  selectedFilters.forEach((filter) => {
    if (filter.field && filter.value) {
      let existingFilterInstance = queryParams.find(
        (property) =>
          property.name === filter.field.value ||
          filter.field.name === property.name
      );
      if (!isEmpty(existingFilterInstance)) {
        if (filter.field.name === "componentState") {
          existingFilterInstance.value.push(
            filter.field.value + ":" + filter.value.value
          );
        } else {
          existingFilterInstance.value.push(filter.value.value);
        }
      } else {
        let filterProperty: any = {};
        if (get(filter, "value.name", "")) {
          filterProperty = hostFilterProperties.find(
            (property) => property.name === filter.value.name
          );
        } else {
          filterProperty = hostFilterProperties.find(
            (property) =>
              property.name === filter.field.value ||
              filter.field.name === property.name
          );
        }

        const result = {
          ...filterProperty,
          value:
            filter.field.name === "componentState"
              ? [filter.field.value + ":" + filter.value.value]
              : isArray(filter.value.value)
              ? filter.value.value
              : [filter.value.value],
        };
        const valueType = get(filterProperty, "valueType", "string");
        if (valueType === "number" || valueType === "ambari-bandwidth") {
          result.type = getComparisonType(result.value[0]);
          result.value = getProperValue(result.value[0]);
        }
        if (
          valueType === "ambari-bandwidth" &&
          result.type === "EQUAL" &&
          result.value
        ) {
          const valuePair = convertMemoryToRange(result.value);
          queryParams.push({
            ...result,
            value: valuePair[0],
            type: "MORE",
          });
          queryParams.push({
            ...result,
            value: valuePair[1],
            type: "LESS",
          });
        } else if (
          valueType === "ambari-bandwidth" &&
          result.type !== "EQUAL" &&
          result.value
        ) {
          result.value = convertMemory(result.value);
          queryParams.push(result);
        } else {
          queryParams.push(result);
        }
      }
    }
  });

  return queryParams;
};

const getComparisonType = (value: string): string => {
  const comparisonChar = value.charAt(0);
  let result = "EQUAL";

  if (isNaN(parseInt(comparisonChar))) {
    switch (comparisonChar) {
      case ">":
        result = "MORE";
        break;
      case "<":
        result = "LESS";
        break;
    }
  }

  return result;
};

const getProperValue = (value: string): string => {
  return [">", "<", "="].includes(value.charAt(0))
    ? value.substr(1, value.length)
    : value;
};

const convertMemoryToRange = (value: string): [number, number] => {
  const scale = value.charAt(value.length - 1);
  // first char may be predicate for comparison
  const properValue = getProperValue(value);
  const parsedValue = parseFloat(properValue);

  if (isNaN(parsedValue)) {
    return [0, 0];
  }

  const parsedValuePair = rangeConvertNumber(parsedValue, scale);
  let multiplyingFactor = 1;

  switch (scale) {
    case "g":
      multiplyingFactor = 1048576;
      break;
    case "m":
      multiplyingFactor = 1024;
      break;
    case "k":
      break;
    default:
      // default value in GB
      multiplyingFactor = 1048576;
  }

  return [
    Math.round(parsedValuePair[0] * multiplyingFactor),
    Math.round(parsedValuePair[1] * multiplyingFactor),
  ];
};

const rangeConvertNumber = (value: number, scale: string): [number, number] => {
  if (isNaN(value)) {
    return [0, 0];
  }

  let valuePair: [number, number];
  switch (scale) {
    case "g":
      valuePair = [value - 0.005, value + 0.004999999];
      break;
    case "m":
    case "k":
      valuePair = [value - 0.05, value + 0.04999];
      break;
    default:
      // default value in GB
      valuePair = [value - 0.005, value + 0.004999999];
  }

  return valuePair;
};

const convertMemory = (value: string): number => {
  const scale = value.charAt(value.length - 1);
  // first char may be predicate for comparison
  const properValue = getProperValue(value);
  const parsedValue = parseFloat(properValue);

  if (isNaN(parsedValue)) {
    return value as unknown as number;
  }

  let result = parsedValue;
  switch (scale) {
    case "g":
      result *= 1048576;
      break;
    case "m":
      result *= 1024;
      break;
    case "k":
      break;
    default:
      // default value in GB
      result *= 1048576;
  }
  return Math.round(result);
};
