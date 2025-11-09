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

type PropTypes = {
  isOpen: boolean;
  onClose: () => void;
  onCancel: () => void;
};

const RedhatSatelliteUsageInfo = ({ isOpen, onClose, onCancel }: PropTypes) => {
  return (
    <Modal show={isOpen} onHide={onClose}>
      <Modal.Header closeButton>
        <Modal.Title>Use RedHat Satellite/Spacewalk</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        In order for Ambari to install packages from the right repositories, it
        is recommended that you edit the names of the repo's for each operating
        system so they match the channel names in your RedHat
        Satellite/Spacewalk instance.
      </Modal.Body>
      <Modal.Footer>
        <DefaultButton size="sm" onClick={onCancel}>
          Cancel
        </DefaultButton>
        <Button
          variant="success"
          size="sm"
          onClick={onClose}
          className="rounded-1"
        >
          OK
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default RedhatSatelliteUsageInfo;