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

import type { KeyboardEvent } from "react";
import { Form } from "react-bootstrap";
import {
  composeTimeIntervalParts,
  decomposeTimeInterval,
  getTimeIntervalStep,
  normalizeTimeIntervalUnits,
  timeIntervalPartMaximum,
  TimeIntervalUnit,
} from "../../Utils/unitConversionUtils";
import { PropertyType } from "./types";

type ThemeTimeIntervalControlProps = {
  property: PropertyType;
  onChange: (value: number) => void;
};

const labels: Record<TimeIntervalUnit, string> = {
  days: "Days",
  hours: "Hours",
  minutes: "Minutes",
  seconds: "Seconds",
  milliseconds: "Milliseconds",
};

export default function ThemeTimeIntervalControl({
  property,
  onChange,
}: ThemeTimeIntervalControlProps) {
  const configUnit =
    property.unit || property.propertyAttributes?.unit || "milliseconds";
  const configuredUnits = normalizeTimeIntervalUnits(
    property.widget?.units?.[0]?.["unit-name"] ||
      property.widget?.units?.[0]?.unit ||
      "days,hours,minutes,seconds",
  );
  const parts = decomposeTimeInterval(
    Number(property.value),
    configUnit,
    configuredUnits,
  );
  const lastUnit = configuredUnits[configuredUnits.length - 1];
  const finalStep = getTimeIntervalStep(
    property.propertyAttributes?.increment_step,
    configUnit,
    lastUnit,
  );
  const maximumParts = decomposeTimeInterval(
    Number(property.propertyAttributes?.maximum),
    configUnit,
    configuredUnits,
  );
  const configuredMaximum = Number(property.propertyAttributes?.maximum);
  const getPartMaximum = (unit: TimeIntervalUnit) => {
    if (unit !== configuredUnits[0]) return timeIntervalPartMaximum[unit];
    return Number.isFinite(configuredMaximum)
      ? Math.max(parts[unit], maximumParts[unit])
      : Math.max(parts[unit], timeIntervalPartMaximum[unit]);
  };
  const finalUnitDisabled = finalStep > timeIntervalPartMaximum[lastUnit];

  const updatePart = (unit: TimeIntervalUnit, requestedValue: number) => {
    const step = unit === lastUnit ? finalStep : 1;
    const maximum = getPartMaximum(unit);
    const maximumAlignedValue = Math.floor(maximum / step) * step;
    const nextValue = Math.min(
      maximumAlignedValue,
      Math.max(0, Math.round(requestedValue / step) * step),
    );
    onChange(
      composeTimeIntervalParts({ ...parts, [unit]: nextValue }, configUnit),
    );
  };

  const handleOverflowKey = (
    event: KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
    unit: TimeIntervalUnit,
  ) => {
    if (event.key !== "ArrowUp" && event.key !== "ArrowDown") return;
    const step = unit === lastUnit ? finalStep : 1;
    const maximum = getPartMaximum(unit);
    const maximumAlignedValue = Math.floor(maximum / step) * step;
    if (event.key === "ArrowUp" && parts[unit] + step > maximum) {
      event.preventDefault();
      updatePart(unit, 0);
    } else if (event.key === "ArrowDown" && parts[unit] - step < 0) {
      event.preventDefault();
      updatePart(unit, maximumAlignedValue);
    }
  };

  return (
    <div className="d-flex w-75">
      {configuredUnits.map((unit) => {
        const isFinalUnit = unit === lastUnit;
        return (
          <div className="d-flex flex-column me-2" key={unit}>
            <Form.Control
              type="number"
              aria-label={labels[unit]}
              min={0}
              max={getPartMaximum(unit)}
              step={isFinalUnit ? finalStep : 1}
              value={parts[unit]}
              onChange={(event) => {
                const nextValue = Number(event.target.value);
                if (Number.isFinite(nextValue)) updatePart(unit, nextValue);
              }}
              onKeyDown={(event) => handleOverflowKey(event, unit)}
              disabled={
                !property.isEditable || (isFinalUnit && finalUnitDisabled)
              }
            />
            <small>{labels[unit]}</small>
          </div>
        );
      })}
    </div>
  );
}
