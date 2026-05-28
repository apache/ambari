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

import { Button, Form, Modal } from "react-bootstrap";
import DefaultButton from "../../components/DefaultButton";
import { useState } from "react";

type FormDataType = {
  name: string;
  description: string;
};

type CreateNewConfigGroupProps = {
  isOpen: boolean;
  onClose: () => void;
  successCallback: (formData: any) => void;
  existingConfigGroups: string[];
  config?: FormDataType;
  isRename?: boolean;
};

export default function CreateNewConfigGroup({
  isOpen,
  onClose,
  successCallback,
  existingConfigGroups,
  config = {
    name: "",
    description: "",
  },
  isRename = false,
}: CreateNewConfigGroupProps) {
  const [formData, setFormData] = useState(config);

  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      className="custom-modal-container modal-width make-scrollable custom-scrollbar"
    >
      <Modal.Header closeButton>
        <h2>{isRename ? "Rename " : "Create New "} Configuration Group</h2>
      </Modal.Header>
      <Form
        onSubmit={(e: any) => {
          e.preventDefault();
          successCallback(formData);
          onClose();
        }}
      >
        <Modal.Body>
          {existingConfigGroups.includes(formData.name) ? (
            <div className="text-warning fs-12 mb-2">
              Configuration Group with given name already exists
            </div>
          ) : null}
          <Form.Group className="d-flex mb-3">
            <Form.Label className="w-25 d-flex justify-content-end me-4 pt-2">
              Name:
            </Form.Label>
            <Form.Control
              type="text"
              value={formData.name}
              className="custom-form-control"
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
            />
          </Form.Group>
          <Form.Group className="d-flex mb-3">
            <Form.Label className="w-25 d-flex justify-content-end me-4 pt-2">
              Description:
            </Form.Label>
            <Form.Control
              type="text"
              as="textarea"
              rows={4}
              value={formData.description}
              className="custom-form-control"
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <DefaultButton onClick={onClose}>CANCEL</DefaultButton>
          <Button
            type="submit"
            className="custom-btn text-white"
            disabled={
              formData.name === "" ||
              existingConfigGroups.includes(formData.name)
            }
          >
            OK
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
