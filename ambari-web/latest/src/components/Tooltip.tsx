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

import React from "react";
import { OverlayTrigger, Popover } from "react-bootstrap";

interface TooltipComponentProps {
  message: string;
  children: any;
  heading?: string;
  placement?: "top" | "right" | "bottom" | "left";
}

const Tooltip: React.FC<TooltipComponentProps> = ({
  message,
  children,
  heading = "",
  placement = "top",
}) => {
  const renderPopover = (props: any) => (
    <Popover id="popover-basic" {...props}>
      {heading && <Popover.Header as="h3">{heading}</Popover.Header>}
      <Popover.Body>{message}</Popover.Body>
    </Popover>
  );

  return (
    <OverlayTrigger placement={placement} overlay={renderPopover}>
      {children}
    </OverlayTrigger>
  );
};

export default Tooltip;
