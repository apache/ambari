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

import { Form } from "react-bootstrap";
import Tooltip from "./Tooltip";
import { ChangeEventHandler, FocusEventHandler } from "react";

type tooltipPropsType = {
  message: string;
  heading?: string;
  placement?: "top" | "right" | "bottom" | "left";
};

type formControlPropsType = {
  type: "text" | "password" | "email" | "number" | "checkbox";
  placeholder?: string;
  value?: string;
  className?: string;
  onChange?: ChangeEventHandler<HTMLInputElement>;
  onBlur?: FocusEventHandler<HTMLInputElement>;
  label?: string;
  checked?: boolean;
};

type TooltipInputProps = {
  tooltipProps: tooltipPropsType;
  formControlProps: formControlPropsType;
};

export default function TooltipInput({
  tooltipProps,
  formControlProps,
}: TooltipInputProps) {
  const { type, ...restProps } = formControlProps;

  return (
    <div>
      <Tooltip {...tooltipProps}>
        {type === "checkbox" ? (
          <Form.Check type={type} {...restProps} />
        ) : (
          <Form.Control type={type} {...restProps} />
        )}
      </Tooltip>
    </div>
  );
}
