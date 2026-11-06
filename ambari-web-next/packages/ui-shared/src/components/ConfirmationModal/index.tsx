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
import { Button, Modal } from "react-bootstrap";
import DefaultButton from "../DefaultButton";

type ConfirmationModalProps = {
  isOpen: boolean;
  onClose: () => void;
  modalTitle: string;
  modalBody: string;
  successCallback: () => void;
  buttonVariant?: string;
};

export default function ConfirmationModal({
  isOpen,
  onClose,
  modalTitle,
  modalBody,
  successCallback,
  buttonVariant = "success",
}: ConfirmationModalProps) {
  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      size="lg"
      className="custom-modal-container modal-width"
      data-testid="confirmation-modal"
    >
      <Modal.Header>
        <Modal.Title>
          <h3>{modalTitle}</h3>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>{modalBody}</Modal.Body>
      <Modal.Footer className="d-flex justify-content-end">
        <DefaultButton
          size="sm"
          onClick={onClose}
          data-testid="confirm-cancel-btn"
        >
          CANCEL
        </DefaultButton>
        <Button
          className="custom-btn"
          variant={buttonVariant}
          onClick={successCallback}
          size="sm"
          data-testid="confirm-ok-btn"
        >
          OK
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
