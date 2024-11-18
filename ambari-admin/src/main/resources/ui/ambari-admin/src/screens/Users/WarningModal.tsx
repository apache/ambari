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
import DefaultButton from "../../components/DefaultButton";

type WarningModalProps = {
    isOpen: boolean;
    onClose: () => void;
    handleWarningDiscard: () => void;
    handleWarningSave: (event: any) => void;
};

export default function WarningModal({
    isOpen,
    onClose,
    handleWarningDiscard,
    handleWarningSave,
}: WarningModalProps) {
  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      size="lg"
      className="custom-modal-container modal-width"
      data-testid="warning-modal"
    >
      <Modal.Header>
        <Modal.Title><h3>Warning</h3></Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div>You have unsaved changes. Save changes or discard?</div>
      </Modal.Body>
      <Modal.Footer className="d-flex justify-content-end">
        <DefaultButton onClick={onClose}>
          Cancel
        </DefaultButton>
        <Button
          className="custom-btn text-light"
          variant="warning"
          onClick={handleWarningDiscard}
        >
          Discard
        </Button>
        <Button
          className="custom-btn"
          variant="success"
          onClick={handleWarningSave}
        >
          Save
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
