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
import Modal from "./Modal";
import { formatDate, getTimeInNumber, getUserTimezone } from "../Utils/Utility";
import { useState } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleXmark } from "@fortawesome/free-solid-svg-icons";
import { get } from "lodash";
import { validateDateTime } from "../Utils/validators";
import { durationMap, durationOptions } from "./constants";

type SelectTimeRangeModalProps = {
  isOpen: boolean;
  onClose: () => void;
  successCallback: (data: any) => void;
};

export default function SelectTimeRangeModal({
  isOpen,
  onClose,
  successCallback,
}: SelectTimeRangeModalProps) {
  const [formData, setFormData] = useState({
    startTime: {
      value: formatDate(new Date()),
      error: "",
    },
    endTime: {
      value: formatDate(new Date()),
      error: "",
    },
  });
  const [selectedDuration, setSelectedDuration] =
    useState<string>("15 minutes");

  const hasError = () => {
    return Object.values(formData).some((data) => data.error);
  };

  const handleSubmit = () => {
    if (hasError()) {
      return;
    }

    const startTimeNumber = getTimeInNumber(formData.startTime.value);
    let endTimeNumber = getTimeInNumber(formData.endTime.value);

    if (selectedDuration !== "Custom") {
      endTimeNumber = startTimeNumber + durationMap[selectedDuration];
    }

    successCallback({
      startTime: startTimeNumber,
      endTime: endTimeNumber,
    });
  };

  const validateTimeRange = (key: string, time: string) => {
    if (!validateDateTime(time)) {
      setFormData({
        ...formData,
        [key]: {
          value: time,
          error: "Date is incorrect",
        },
      });
      return;
    }
    const errors = { startTime: "", endTime: "" };
    const currentTimeInNumber = getTimeInNumber(formatDate(new Date()));

    const newTime: { [key: string]: string } = {
      startTime: formData.startTime.value,
      endTime: formData.endTime.value,
    };

    newTime[key] = time;

    if (
      newTime.startTime &&
      getTimeInNumber(newTime.startTime) > currentTimeInNumber
    ) {
      errors.startTime = "The specified time is in the future";
    }

    if (
      selectedDuration === "Custom" &&
      newTime.startTime &&
      newTime.endTime &&
      getTimeInNumber(newTime.endTime) < getTimeInNumber(newTime.startTime)
    ) {
      errors.endTime = "End Date must be after Start Date";
    }

    setFormData({
      startTime: {
        value: newTime.startTime,
        error: errors.startTime,
      },
      endTime: {
        value: newTime.endTime,
        error: errors.endTime,
      },
    });
  };

  const getModalBody = () => {
    return (
      <Form>
        <Form.Group className="mb-3">
          <Form.Label>Start Time</Form.Label>
          <Form.Control
            value={get(formData, "startTime.value", "")}
            type="datetime-local"
            className="custom-form-control"
            onChange={(e) => validateTimeRange("startTime", e.target.value)}
          />
          {get(formData, "startTime.error", "") && (
            <div className="text-danger mt-3">
              <FontAwesomeIcon icon={faCircleXmark} />{" "}
              {get(formData, "startTime.error", "")}
            </div>
          )}
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>Duration</Form.Label>
          <Form.Select
            className="custom-form-control"
            value={selectedDuration}
            onChange={(e) => setSelectedDuration(e.target.value)}
          >
            {durationOptions.map((option) => (
              <option key={option.value} value={option.label}>
                {option.label}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
        {selectedDuration === "Custom" ? (
          <Form.Group className="mb-3">
            <Form.Label>End Time</Form.Label>
            <Form.Control
              value={get(formData, "endTime.value", "")}
              type="datetime-local"
              className="custom-form-control"
              onChange={(e) => validateTimeRange("endTime", e.target.value)}
            />
            {get(formData, "endTime.error", "") && (
              <div className="text-danger mt-3">
                <FontAwesomeIcon icon={faCircleXmark} />{" "}
                {get(formData, "endTime.error", "")}
              </div>
            )}
          </Form.Group>
        ) : null}
        <div>
          <span>Timezone: </span>
          {getUserTimezone()}
        </div>
      </Form>
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      modalTitle="Select Time Range"
      modalBody={getModalBody()}
      successCallback={handleSubmit}
      options={{
        modalSize: "modal-sm",
        cancelableViaIcon: true,
        cancelableViaBtn: true,
        okButtonVariant: "primary",
      }}
    />
  );
}
