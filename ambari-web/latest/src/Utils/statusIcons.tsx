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
import {
  faCheck,
  faClock,
  faCog,
  faCogs,
  faExclamation,
  faMinus,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import classNames from "classnames";

type RequestStatus =
  | "INIT"
  | "PENDING"
  | "QUEUED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "FAILED"
  | "HOLDING_FAILED"
  | "SKIPPED_FAILED"
  | "HOLDING"
  | "SUSPENDED"
  | "ABORTED"
  | "TIMEDOUT"
  | "HOLDING_TIMEDOUT"
  | "SUBITEM_FAILED"
  | "WARNING";

type StatusIconConfig = {
  icon: any;
  color: string;
  shouldShowOpacity?: boolean;
};

const STATUS_ICON_MAP: Record<RequestStatus, StatusIconConfig> = {
  INIT: { icon: faCogs, color: "blue" },
  PENDING: { icon: faCog, color: "gray", shouldShowOpacity: true },
  QUEUED: { icon: faCog, color: "gray" },
  IN_PROGRESS: { icon: faCogs, color: "blue" },
  COMPLETED: { icon: faCheck, color: "green" },
  FAILED: { icon: faExclamation, color: "red" },
  HOLDING_FAILED: { icon: faExclamation, color: "red" },
  SKIPPED_FAILED: { icon: faTimes, color: "red" },
  HOLDING: { icon: faClock, color: "orange" },
  SUSPENDED: { icon: faClock, color: "orange" },
  ABORTED: { icon: faMinus, color: "orange" },
  TIMEDOUT: { icon: faClock, color: "orange" },
  HOLDING_TIMEDOUT: { icon: faClock, color: "orange" },
  SUBITEM_FAILED: { icon: faTimes, color: "red" },
  WARNING: { icon: faExclamation, color: "yellow" },
};

const DEFAULT_STATUS_CONFIG: StatusIconConfig = {
  icon: faCog,
  color: "blue",
  shouldShowOpacity: true,
};

export const getStatusIcon = (
  requestStatus: string | undefined
): React.ReactElement => {
  const config =
    requestStatus && requestStatus in STATUS_ICON_MAP
      ? STATUS_ICON_MAP[requestStatus as RequestStatus]
      : DEFAULT_STATUS_CONFIG;

  const { icon, color, shouldShowOpacity = false } = config;

  return (
    <FontAwesomeIcon
      icon={icon}
      color={color}
      className={classNames("me-2", { "opacity-50": shouldShowOpacity })}
    />
  );
};