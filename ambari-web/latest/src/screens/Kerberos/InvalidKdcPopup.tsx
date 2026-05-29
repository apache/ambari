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

import { useState } from "react";
import Modal from "../../components/Modal";
import { messages } from "../messages";
import { get } from "lodash";
import { Alert, Form } from "react-bootstrap";

type InvalidKDCPopupProps = {
    isOpen: boolean;
    onClose: () => void;
    handleSave: (adminPrincipal: string, adminPassword: string, saveCredentials: boolean) => void;
};

export default function invalidKDCPopup({isOpen, onClose, handleSave}: InvalidKDCPopupProps): React.ReactElement {
    const [adminPrincipal, setAdminPrincipal] = useState("");
    const [adminPassword, setAdminPassword] = useState("");
    const [saveCredentials, setSaveCredentials] = useState(false);
    
    function getModalBody() {
        return (
            <>
                <Alert variant="warning">
                Warning: Missing KDC administrator credentials. Please enter admin principal and password.
                </Alert>
                <Form>
                    <Form.Group controlId="adminPrincipal">
                        <Form.Label>Admin Principal</Form.Label>
                        <Form.Control
                        type="text"
                        value={adminPrincipal}
                        onChange={(e) => setAdminPrincipal(e.target.value)}
                        />
                    </Form.Group>
                    <Form.Group controlId="adminPassword">
                        <Form.Label>Admin Password</Form.Label>
                        <Form.Control
                        type="password"
                        value={adminPassword}
                        onChange={(e) => setAdminPassword(e.target.value)}
                        />
                    </Form.Group>
                    <Form.Group controlId="saveCredentials">
                        <Form.Check
                        type="checkbox"
                        id="save-admin-credentials-checkbox"
                        label="Save Admin Credentials"
                        checked={saveCredentials}
                        onChange={(e) => setSaveCredentials(e.target.checked)}
                        />
                    </Form.Group>
                </Form>
            </>
        )
    }

    async function handleSaveDetails() {
        handleSave(adminPrincipal, adminPassword, saveCredentials);
    }

    return (
        <>
            <Modal
                isOpen={isOpen}
                onClose={() => onClose()}
                modalTitle={get(messages, "popup.invalid.KDC.header", "")}
                modalBody={getModalBody()}
                options={{
                    okButtonText: "SAVE",
                    cancelableViaIcon: true,
                    cancelableViaBtn: true,
                    modalSize: "modal-md",
                }}
                successCallback={() => {
                    handleSaveDetails();
                }}
            />
        </>
    )
}
