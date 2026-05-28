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
  message: any;
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
  // Clean up the message by trimming whitespace and normalizing line breaks
  const cleanMessage = typeof message === 'string' 
    ? message.trim().replace(/\s+/g, ' ').replace(/\n\s*/g, ' ')
    : message;

  // Clean up the heading
  const cleanHeading = typeof heading === 'string' 
    ? heading.trim().replace(/\s+/g, ' ').replace(/\n\s*/g, ' ')
    : heading;

  // Don't render tooltip if:
  // 1. Message is empty, null, undefined, or just whitespace
  // 2. Message is the same as the heading (redundant)
  if (!cleanMessage || 
      (typeof cleanMessage === 'string' && cleanMessage.trim() === '') ||
      (cleanMessage === cleanHeading)) {
    return <>{children}</>;
  }

  const renderPopover = (props: any) => (
    <Popover id="popover-basic" {...props}>
      {cleanHeading && <Popover.Header as="h3">{cleanHeading}</Popover.Header>}
      <Popover.Body style={{ maxWidth: '300px', wordWrap: 'break-word' }}>
        {cleanMessage}
      </Popover.Body>
    </Popover>
  );

  return (
    <OverlayTrigger 
      placement={placement} 
      overlay={renderPopover}
      trigger={['hover', 'focus']}
    >
      {children}
    </OverlayTrigger>
  );
};

export default Tooltip;
