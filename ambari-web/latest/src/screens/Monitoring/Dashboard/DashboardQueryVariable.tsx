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
import { Dropdown, Form } from "react-bootstrap";
import type { DashboardVariable } from "../types";

interface DashboardQueryVariableProps {
  variable: DashboardVariable;
  options: string[];
  value: string | number | string[] | undefined;
  onChange: (value: string | string[]) => void;
}

export default function DashboardQueryVariable({
  variable,
  options,
  value,
  onChange,
}: DashboardQueryVariableProps) {
  const label = variable.label || variable.name;
  const optionIdPrefix = useId();
  if (!variable.multi) {
    return <Form.Group><Form.Label>{label}</Form.Label><Form.Select aria-label={label} size="sm" value={String(value ?? "")} onChange={(event) => onChange(event.target.value)}>
      {variable.includeAll && <option value=".*">All</option>}
      {options.map((option) => <option key={option} value={option}>{option}</option>)}
    </Form.Select></Form.Group>;
  }

  const selected = Array.isArray(value) ? value : value ? [String(value)] : [];
  const allSelected = selected.includes(".*");
  const selectionLabel = allSelected
    ? "All"
    : selected.length === 0
      ? "Select"
      : selected.length === 1
        ? selected[0]
        : `${selected.length} selected`;
  const toggle = (option: string, checked: boolean) => {
    if (option === ".*") {
      onChange(checked ? [".*"] : []);
      return;
    }
    const concrete = selected.filter((item) => item !== ".*");
    onChange(checked
      ? Array.from(new Set([...concrete, option]))
      : concrete.filter((item) => item !== option));
  };

  return <Form.Group className="dashboard-query-variable"><Form.Label>{label}</Form.Label><Dropdown autoClose="outside">
    <Dropdown.Toggle size="sm" variant="light" title={selected.filter((item) => item !== ".*").join(", ")}>{selectionLabel}</Dropdown.Toggle>
    <Dropdown.Menu>
      {variable.includeAll && <Form.Check id={`${optionIdPrefix}-all`} label="All" checked={allSelected} onChange={(event) => toggle(".*", event.target.checked)} />}
      {options.map((option, index) => <Form.Check id={`${optionIdPrefix}-${index}`} key={option} label={option} checked={!allSelected && selected.includes(option)} onChange={(event) => toggle(option, event.target.checked)} />)}
      {!variable.includeAll && options.length === 0 && <Dropdown.ItemText>No values</Dropdown.ItemText>}
    </Dropdown.Menu>
  </Dropdown></Form.Group>;
}
