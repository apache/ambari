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
import { useContext, useEffect, useState } from "react";
import { Button, Image, Modal } from "react-bootstrap";
import ClusterApi from "./api/clusterApi";
import AppContent from "./context/AppContext";
import { get } from "lodash";
import Spinner from "./components/Spinner";
import AmbariLogo from "./assets/img/ambari-logo.png"
type AmbariAboutModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

export default function AmbariAboutModal({
  isOpen,
  onClose,
}: AmbariAboutModalProps) {
  const { ambariVersion, setAmbariVersion } = useContext(AppContent);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    async function getAmbariAboutInfo() {
      setLoading(true);
      const data: any = await ClusterApi.adminAboutInfo(
        "RootServiceComponents/component_version,RootServiceComponents/properties/server.os_family&minimal_response=true"
      );
      console.log("version", get(data, "RootServiceComponents.component_version"))
      setAmbariVersion(get(data, "RootServiceComponents.component_version"));
      setLoading(false);
    }
    if (!ambariVersion) {
      getAmbariAboutInfo();
    }
  }, []);

  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      size="lg"
      className="custom-modal-container"
    >
      <Modal.Header closeButton>
        <Modal.Title>
          <h3>About</h3>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {loading ? (
          <Spinner />
        ) : (
          <div className="d-flex">
            <Image
              src={AmbariLogo}
              height={75}
              width={75}
              className="me-5"
            />
            <div>
              <h2>Apache Ambari</h2>
              <div className="mb-3">Version {ambariVersion}</div>
              <div>
                <a href="http://ambari.apache.org/" className="custom-link">
                  Get involved!
                </a>
              </div>
              <div>
                <a
                  href="http://www.apache.org/licenses/LICENSE-2.0"
                  className="custom-link"
                >
                  Licensed under the Apache License, Version 2.0
                </a>
              </div>
            </div>
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button className="custom-btn" variant="success" onClick={onClose}>
          OK
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
