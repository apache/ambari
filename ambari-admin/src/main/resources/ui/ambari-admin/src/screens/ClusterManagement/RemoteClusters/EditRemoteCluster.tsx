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
import { useState, useEffect, useMemo, useContext } from "react";
import { Link, useParams, useHistory } from "react-router-dom";
import { Button, Form, Modal } from "react-bootstrap";
import toast from "react-hot-toast";
import Spinner from "../../../components/Spinner";
import RemoteClusterApi from "../../../api/remoteCluster";
import DefaultButton from "../../../components/DefaultButton";
import AppContent from "../../../context/AppContext";
import DeregisterRemoteCluster from "./DeregisterRemoteCluster";

export default function EditRemoteCluster() {
  const { clusterName } = useParams() as any;
  const [cluster, setCluster] = useState({
    name: "",
    url: "",
    username: "",
    password: "",
  });
  const [clusterCopy, setClusterCopy] = useState({ ...cluster });
  const [showCredentialModal, setShowCredentialModal] = useState(false);
  const [errorMessageClusterName, setErrorMessageClusterName] = useState("");
  const [errorMessageUrl, setErrorMessageUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const history = useHistory();

  const { setSelectedOption } = useContext(AppContent);

  // Fetch cluster information when component mounts
  useEffect(() => {
    setSelectedOption("Remote Clusters");
    const fetchClusterInfo = async () => {
      setLoading(true)
      try {
        const response = await RemoteClusterApi.getRemoteClusterByName(
          clusterName
        );
        setCluster(response.ClusterInfo);
        setClusterCopy(response.ClusterInfo);
      } catch (error) {
        if (error instanceof Error)
          toast("Error while fetching cluster information: " + error.message);
        else toast("Error fetching cluster information");
      }
      setLoading(false);
    };
    fetchClusterInfo();
  }, []);

  const handleUpdate = async (event: any) => {
    event.preventDefault();
    validateInputs("name", clusterCopy.name);
    validateInputs("url", clusterCopy.url);

    if (
      !event.currentTarget.checkValidity() ||
      errorMessageClusterName ||
      errorMessageUrl
    ) {
      return;
    }

    try {
      await RemoteClusterApi.updateRemoteCluster(clusterName, clusterCopy);
      setCluster(clusterCopy);
      toast.success(
        <div className="toast-message">
          Cluster "{clusterName}" updated successfully!
        </div>
      );
      setShowCredentialModal(false);
      setErrorMessageClusterName("");
      setErrorMessageUrl("");
    } catch (error) {
      if (error instanceof Error)
        toast.error(
          <div className="toast-message">
            Error while updating cluster: {error.message}
          </div>
        );
      else toast.error("Error while updating cluster");
    }
  };

  const validateInputs = (name: string, value: string) => {
    let urlPattern = new RegExp(
      "^https?://[a-zA-Z0-9]+.example.com(:[0-9]{4})?(/.*)?$"
    );
    let clusterNamePattern = new RegExp("^[A-Za-z0-9]{1,80}$");

    if (name === "url") {
      if (value === "") {
        setErrorMessageUrl("This field is required.");
      } else if (!urlPattern.test(value)) {
        setErrorMessageUrl("Must be a valid URL.");
      } else {
        setErrorMessageUrl("");
      }
    }

    if (name === "name") {
      if (value === "") {
        setErrorMessageClusterName("This field is required.");
      } else if (!clusterNamePattern.test(value)) {
        setErrorMessageClusterName(
          "Must not contain any special characters or spaces."
        );
      } else {
        setErrorMessageClusterName("");
      }
    }
  };

  const handleInputChange = (e: any) => {
    const { name, value } = e.target;
    setClusterCopy({ ...clusterCopy, [name]: value });
    validateInputs(name, value);
  };

  const isModified = useMemo(() => {
    return JSON.stringify(cluster) !== JSON.stringify(clusterCopy);
  }, [cluster, clusterCopy]);

  if (loading) {
    return <Spinner />;
  }
  return (
    <>
      <div className="d-flex justify-content-between align-items-center border-bottom pb-3">
        <div className="d-flex ">
          <Link to={`/remoteClusters`} className="fs-lg text-decoration-none">
            <h4 className="custom-link">Remote Clusters </h4>
          </Link>
          <h4 className="mx-2">/</h4>
          <h4>{cluster.name}</h4>
        </div>
        <DeregisterRemoteCluster clusterName={cluster.name} />
      </div>
      <div>
        <Form onSubmit={handleUpdate} className="mt-4">
          <Form.Group
            className="row align-items-center"
            controlId="formClusterName"
          >
            <Form.Label className="col-sm-2 text-center">
              Cluster Name*
            </Form.Label>
            <div className="col-sm-10">
              <Form.Control
                className="rounded-1"
                type="text"
                isInvalid={!!errorMessageClusterName}
                name="name"
                pattern="^[A-Za-z0-9]{1,80}$"
                maxLength={80}
                value={clusterCopy.name}
                onChange={handleInputChange}
                required
              />
              <Form.Control.Feedback type="invalid">
                {errorMessageClusterName}
              </Form.Control.Feedback>
            </div>
          </Form.Group>

          <Form.Group
            controlId="formAmbariClusterUrl"
            className="mt-3 row align-items-center"
          >
            <Form.Label className="col-sm-2 text-center">
              Ambari Cluster URL*
            </Form.Label>
            <div className="col-sm-10">
              <Form.Control
                isInvalid={!!errorMessageUrl}
                className="col-sm-10 rounded-1"
                type="text"
                name="url"
                value={clusterCopy.url}
                onChange={handleInputChange}
                pattern="^https?:\/\/[a-zA-Z0-9]+\.example\.com(:[0-9]{4})?(\/.*)?$"
                maxLength={80}
                required
              />
              <Form.Control.Feedback type="invalid">
                {errorMessageUrl}
              </Form.Control.Feedback>
            </div>
          </Form.Group>

          <Form.Group
            controlId="updateCredentials"
            className="mt-3 row align-items-center"
          >
            <Form.Label className="col-sm-2"></Form.Label>
            <div className="col-sm-2" data-testid="updateCredentialButton">
              <DefaultButton
                onClick={() => setShowCredentialModal(true)}
                variant="outline-secondary"
              >
                Update Credentials
              </DefaultButton>
            </div>
          </Form.Group>

          <br />
          <div className="d-flex justify-content-end mt-1">
            <div>
              <DefaultButton
                variant="outline-secondary"
                className="text-uppercase"
                onClick={() => history.push("/remoteClusters")}
              >
                Cancel
              </DefaultButton>
            </div>
            <Button
              className="text-uppercase mx-2"
              type="submit"
              variant="outline-success"
              size="sm"
              disabled={!isModified}
            >
              Save
            </Button>
          </div>
        </Form>
      </div>

      <Modal
        show={showCredentialModal}
        onHide={() => setShowCredentialModal(false)}
      >
        <Form onSubmit={handleUpdate}>
          <Modal.Header closeButton>
            <Modal.Title>Update Credentials</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Group
              controlId="formClusterUser"
              className="row align-items-center"
            >
              <Form.Label className="col-sm-4 text-center">
                Cluster User*
              </Form.Label>
              <div className="col-sm-8">
                <Form.Control
                  type="text"
                  name="username"
                  value={clusterCopy.username}
                  maxLength={80}
                  onChange={handleInputChange}
                  required
                />
              </div>
            </Form.Group>
            <Form.Group
              controlId="formClusterPassword"
              className="mt-2 row align-items-center"
            >
              <Form.Label className="col-sm-4 text-center">
                Password*
              </Form.Label>
              <div className="col-sm-8">
                <Form.Control
                  type="password"
                  name="password"
                  value={clusterCopy.password}
                  maxLength={80}
                  onChange={handleInputChange}
                  data-testid="password-input"
                  required
                />
              </div>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer className="justify-content-end">
            <DefaultButton
              size="sm"
              onClick={() => {
                setShowCredentialModal(false);
                setClusterCopy((prevState) => ({
                  ...prevState,
                  username: cluster.username,
                  password: cluster.password,
                }));
              }}
              className="outline secondary"
            >
              Cancel
            </DefaultButton>
            <Button
              className="rounded-1 text-uppercase"
              type="submit"
              variant="success"
              size="sm"
              disabled={!isModified}
            >
              Update
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </>
  );
}
