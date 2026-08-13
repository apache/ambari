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

import { useContext } from "react";
import { Image } from "react-bootstrap";
import Modal from "./components/Modal";
import AmbariLogo from "./assets/img/ambari-logo.png";
import { AppContext } from "./store/context";

export default function AmbariAboutModal({
  isOpen,
  onClose,
}: {
  isOpen: boolean;
  onClose: () => void;
}) {
  const { ambariServerVersion } = useContext(AppContext);
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      modalTitle="About"
      modalBody={(
        <div className="d-flex">
          <Image src={AmbariLogo} height={75} width={75} className="me-5" />
          <div>
            <h2>Apache Ambari</h2>
            <div className="mb-3">Version {ambariServerVersion || "N/A"}</div>
            <div><a href="https://ambari.apache.org/" className="custom-link">Get involved!</a></div>
            <div>
              <a href="https://www.apache.org/licenses/LICENSE-2.0" className="custom-link">
                Licensed under the Apache License, Version 2.0
              </a>
            </div>
          </div>
        </div>
      )}
      successCallback={onClose}
      options={{
        modalSize: "modal-lg",
        okButtonText: "OK",
        cancelableViaBtn: false,
        okButtonVariant: "success",
      }}
    />
  );
}
