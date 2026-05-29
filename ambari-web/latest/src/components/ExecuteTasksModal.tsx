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

import { Button, Modal, ProgressBar } from "react-bootstrap";
import { TaskExecutionStatus } from "../constants";
import { useEffect } from "react";

type PropTypes = {
  tasks: Task[];
  isOpen: boolean;
  onClose: Function;
  totalCount: number;
  successCount: number;
  failedCount: number;
  successCallback?: any;
};
type Task = {
  status: TaskExecutionStatus;
};
function ExecuteTasksModal({
  isOpen,
  onClose,
  totalCount,
  successCount,
  failedCount,
  successCallback,
}: PropTypes) {
  function getCurrentProgress() {
    return ((successCount + failedCount) / totalCount) * 100;
  }
  function getVariant() {
    if (failedCount > 0) {
      return "danger";
    } else if (successCount === totalCount) {
      return "success";
    } else {
      return "info";
    }
  }
  useEffect(() => {
    if (totalCount && successCount && successCount === totalCount) {
      successCallback();
      onClose();
    }
  }, [totalCount, successCount]);
  return (
    <Modal
      show={isOpen}
      onHide={onClose as any}
      size="lg"
      className="custom-modal-container modal-width"
      data-testid="confirmation-modal"
    >
      <Modal.Header>
        <Modal.Title>
          <h2>
            Initialising {successCount} of {totalCount} tasks
          </h2>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <ProgressBar now={getCurrentProgress()} variant={getVariant()} />
      </Modal.Body>
      <Modal.Footer>
        <Button
          className="text-white"
          variant="primary"
          disabled={successCount + failedCount !== totalCount}
          onClick={() => {
            onClose();
          }}
        >
          OK
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export default ExecuteTasksModal;
