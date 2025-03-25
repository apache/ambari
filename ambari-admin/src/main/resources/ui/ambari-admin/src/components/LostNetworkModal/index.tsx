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

type PropTypes = {
  onClose: () => void;
  isOpen: boolean;
};

const LostNetworkModal = ({ onClose, isOpen }: PropTypes) => {
  const options = [
    "Configure your hosts for access to the Internet.",
    " If you are using an Internet Proxy, refer to the Ambari Documentation on how to configure Ambari to use the Internet Proxy.",
    "Use the Local Repository option.",
  ];
  return (
    <Modal show={isOpen} onHide={onClose} size="lg">
      <Modal.Header closeButton>
        <Modal.Title>Public Repository Option Disabled</Modal.Title>
      </Modal.Header>
      <Modal.Body className="fs-12">
        Ambari does not have access to the Internet and cannot use the Public
        Repository for installing the software. Your Options:
        <ul>
          {options.map((opt:string)=>{
            return <li key="opt" className="mt-2" style={{fontSize:12}}>
              {opt}
            </li>
          })}
        </ul>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="success" onClick={onClose}>
          OK
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default LostNetworkModal;