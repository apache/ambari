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

import Modal from "../../components/Modal";
import { useState } from "react";
import { Form } from "react-bootstrap";
import { translate, translateWithVariables } from "../../Utils/Utility";

type ConfirmDeleteHostModalProps = {
  isOpen: boolean;
  onClose: () => void;
  successCallback: () => void;
  confirmKey: string;
};

export default function ConfirmDeleteHostModal({
  isOpen,
  onClose,
  successCallback,
  confirmKey,
}: ConfirmDeleteHostModalProps) {
  const [confirmInput, setConfirmInput] = useState("");

  const isConfirmDisabled = () => {
    return confirmInput !== confirmKey;
  };

  const getModalBody = () => {
    return (
      <div>
        <div>
          <span className="fw-bolder text-dark">
            {translate(
              "hosts.bulkOperation.deleteHosts.confirmation.body.note"
            )}
          </span>
          {translate("hosts.bulkOperation.deleteHosts.confirmation.body.msg1")}
        </div>
        <br />
        <div>
          {translate("hosts.bulkOperation.deleteHosts.confirmation.body.msg2")}
          <span className="ms-1 text-danger">
            {translate(
              "hosts.bulkOperation.deleteHosts.confirmation.body.msg3"
            )}
          </span>
        </div>
        <Form.Group className="d-flex justify-content-center p-4">
          <Form.Label className="pt-2 me-2 fw-bolder text-dark">
            {translateWithVariables(
              "services.service.confirmDelete.popup.body.type",
              {
                "0": confirmKey,
              }
            )}
          </Form.Label>
          <Form.Control
            value={confirmInput}
            onChange={(e) => setConfirmInput(e.target.value)}
            className="custom-form-control w-25"
          />
        </Form.Group>
      </div>
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => {
        onClose();
      }}
      successCallback={() => {
        successCallback();
      }}
      modalTitle={translate(
        "hosts.bulkOperation.deleteHosts.confirmation.header"
      )}
      modalBody={getModalBody()}
      options={{
        okButtonText: String(translate("common.confirm")).toUpperCase(),
        okButtonVariant: "warning",
        okButtonDisabled: isConfirmDisabled(),
        cancelableViaBtn: true,
        cancelableViaIcon: true,
        cancelButtonText: String(translate("common.cancel")).toUpperCase(),
      }}
    />
  );
}
