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

import { useId } from "react";
import { Form, Stack } from "react-bootstrap";
import { PropertyType } from "./types";
import {
  getUnsupportedThemeEntryValues,
  getThemeWidgetEntries,
  parseSelectionCardinality,
} from "./themeWidgetUtils";

type ThemeControlProps = {
  property: PropertyType;
  onChange: (value: string) => void;
};

export function ThemeDirectoryControl({
  property,
  onChange,
}: ThemeControlProps) {
  return (
    <Form.Control
      type="text"
      value={String(property.value ?? "")}
      aria-label={property.propertyDisplayname || property.propertyName}
      onChange={(event) => onChange(event.target.value)}
      disabled={!property.isEditable}
    />
  );
}

export function ThemeDirectoriesControl({
  property,
  onChange,
}: ThemeControlProps) {
  return (
    <Form.Control
      as="textarea"
      rows={4}
      value={String(property.value ?? "")}
      aria-label={property.propertyDisplayname || property.propertyName}
      onChange={(event) => onChange(event.target.value)}
      disabled={!property.isEditable}
    />
  );
}

export function ThemeListControl({ property, onChange }: ThemeControlProps) {
  const controlGroupId = useId();
  const entries = getThemeWidgetEntries(property);
  const selected = String(property.value ?? "")
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
  const cardinality = parseSelectionCardinality(
    property.propertyAttributes?.selection_cardinality,
  );
  const minimum = Number.isFinite(cardinality.minimum)
    ? cardinality.minimum
    : entries.length;
  const maximum = Number.isFinite(cardinality.maximum)
    ? cardinality.maximum
    : entries.length;
  const unsupportedValues = getUnsupportedThemeEntryValues(property, true);

  if (unsupportedValues.length) {
    return (
      <Form.Control
        type="text"
        value={String(property.value ?? "")}
        aria-label={property.propertyDisplayname || property.propertyName}
        onChange={(event) => onChange(event.target.value)}
        disabled={!property.isEditable}
      />
    );
  }

  const toggle = (value: string) => {
    const next = selected.includes(value)
      ? selected.filter((item) => item !== value)
      : [...selected, value];
    if (next.length <= maximum) onChange(next.join(","));
  };

  return (
    <fieldset>
      <Stack gap={2}>
        {entries.map((entry) => {
          const checked = selected.includes(entry.value);
          return (
            <Form.Check
              key={entry.value}
              id={`${controlGroupId}-${entry.value}`}
              type="checkbox"
              label={entry.label}
              title={entry.description}
              checked={checked}
              disabled={
                !property.isEditable || (!checked && selected.length >= maximum)
              }
              onChange={() => toggle(entry.value)}
            />
          );
        })}
      </Stack>
      {selected.length < minimum && (
        <Form.Text className="text-danger">
          Select at least {minimum} item(s).
        </Form.Text>
      )}
    </fieldset>
  );
}

export function ThemeRadioControl({ property, onChange }: ThemeControlProps) {
  const controlGroupId = useId();
  if (getUnsupportedThemeEntryValues(property).length) {
    return (
      <Form.Control
        type="text"
        value={String(property.value ?? "")}
        aria-label={property.propertyDisplayname || property.propertyName}
        onChange={(event) => onChange(event.target.value)}
        disabled={!property.isEditable}
      />
    );
  }
  return (
    <fieldset>
      {getThemeWidgetEntries(property).map((entry) => (
        <Form.Check
          key={entry.value}
          id={`${controlGroupId}-${entry.value}`}
          type="radio"
          name={`theme-radio-${controlGroupId}`}
          label={entry.label}
          title={entry.description}
          value={entry.value}
          checked={String(property.value) === entry.value}
          disabled={!property.isEditable}
          onChange={(event) => onChange(event.target.value)}
        />
      ))}
    </fieldset>
  );
}

export function ThemeLabelControl({ property }: Pick<ThemeControlProps, "property">) {
  return (
    <Form.Control
      plaintext
      readOnly
      value={String(property.value ?? "")}
      aria-label={property.propertyDisplayname || property.propertyName}
    />
  );
}
