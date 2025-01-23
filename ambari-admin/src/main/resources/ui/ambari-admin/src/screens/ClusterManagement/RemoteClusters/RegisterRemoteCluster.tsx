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
import { Form, Button } from "react-bootstrap";
import { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useHistory } from "react-router-dom";
import toast from "react-hot-toast";
import DefaultButton from "../../../components/DefaultButton";
import RemoteClusterApi from "../../../api/remoteCluster";
import AppContent from "../../../context/AppContext";

export default function RegisterRemoteCluster() {
  const [cluster, setCluster] = useState({
    name: "",
    url: "",
    username: "",
    password: "",
  });
  const [validated, setValidated] = useState(false);
  const [errorMessageClusterName, setErrorMessageClusterName] = useState("");
  const [errorMessageUrl, setErrorMessageUrl] = useState("");
  const history = useHistory();

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
  const { setSelectedOption } = useContext(AppContent);

  useEffect(() => {setSelectedOption("Remote Clusters")}, []);

  const handleInputChange = (e: any) => {
    const { name, value } = e.target;
    setCluster({ ...cluster, [name]: value });
    validateInputs(name, value);
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    setValidated(true);

    validateInputs("name", cluster.name);
    validateInputs("url", cluster.url);

    if (
      !e.currentTarget.checkValidity() ||
      errorMessageUrl ||
      errorMessageClusterName
    ) {
      return;
    }

    if (e.currentTarget.checkValidity()) {
      try {
        await RemoteClusterApi.addRemoteCluster(cluster);
        toast.success(
          <div className="toast-message">
            Cluster "{cluster.name}" registered successfully!
          </div>
        );
        history.push(`/remoteClusters/${cluster.name}/edit`);
      } catch (error) {
        if (error instanceof Error)
          toast.error(
            <div className="toast-message">
              Error while adding remote Cluster: {error.message}
            </div>
          );
        else toast.error("Error while adding remote cluster");
      }
    }
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-center border-bottom pb-3">
        <div className="d-flex ">
          <Link to={`/remoteClusters`} className="fs-lg text-decoration-none">
            <h4 className="custom-link">Remote Clusters </h4>
          </Link>
          <h4 className="mx-2">/</h4>
          <h4>Register</h4>
        </div>
      </div>

      <Form
        onSubmit={handleSubmit}
        className="mt-4"
        noValidate
        validated={validated}
      >
        <Form.Group
          controlId="formClusterName"
          className="row align-items-center"
        >
          <Form.Label className="col-sm-2 text-center">
            Cluster Name*
          </Form.Label>
          <div className="col-sm-10">
            <Form.Control
              className="rounded-1"
              isInvalid={!!errorMessageClusterName}
              type="text"
              placeholder="Ambari Cluster Name"
              name="name"
              value={cluster.name}
              pattern="^[A-Za-z0-9]{1,80}$"
              maxLength={80}
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
              type="text"
              placeholder="http://ambari.server:8080/api/v1/clusters/clusterName"
              name="url"
              value={cluster.url}
              pattern="^https?:\/\/[a-zA-Z0-9]+\.example\.com(:[0-9]{4})?(\/.*)?$"
              maxLength={80}
              onChange={handleInputChange}
              required
            />
            <Form.Control.Feedback type="invalid">
              {errorMessageUrl}
            </Form.Control.Feedback>
          </div>
        </Form.Group>

        <Form.Group
          controlId="formClusterUser"
          className="mt-3 row align-items-center"
        >
          <Form.Label className="col-sm-2 text-center">
            Cluster User*
          </Form.Label>
          <div className="col-sm-10">
            <Form.Control
              type="text"
              placeholder="Cluster User"
              name="username"
              value={cluster.username}
              onChange={handleInputChange}
              required
            />
            <Form.Control.Feedback type="invalid">
              This field is required.
            </Form.Control.Feedback>
          </div>
        </Form.Group>

        <Form.Group
          controlId="formPassword"
          className="mt-3 row align-items-center"
        >
          <Form.Label className="col-sm-2 text-center">Password*</Form.Label>
          <div className="col-sm-10">
            <Form.Control
              type="password"
              placeholder="Password"
              name="password"
              value={cluster.password}
              onChange={handleInputChange}
              required
            />
            <Form.Control.Feedback type="invalid">
              This field is required.
            </Form.Control.Feedback>
          </div>
        </Form.Group>

        <div className="d-flex justify-content-end mt-4">
          <Link className="text-decoration-none" to={`/remoteClusters`}>
            <DefaultButton
              variant="primary"
              className="rounded-1 text-uppercase"
              size="sm"
            >
              Cancel
            </DefaultButton>
          </Link>
          <Button
            variant="success"
            className="ms-2 rounded-1 text-uppercase"
            size="sm"
            type="submit"
          >
            Save
          </Button>
        </div>
      </Form>
    </>
  );
}
