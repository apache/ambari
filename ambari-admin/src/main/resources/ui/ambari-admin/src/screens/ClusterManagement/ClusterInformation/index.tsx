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
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useEffect, useState } from "react";
import {Form, OverlayTrigger, Tooltip} from "react-bootstrap";
import DefaultButton from "../../../components/DefaultButton";
import AppContent from "../../../context/AppContext";
import ClusterApi from "../../../api/clusterApi";
import Spinner from "../../../components/Spinner";
import toast from "react-hot-toast";
import { cloneDeep } from "lodash";
import ConfirmationModal from "../../../components/ConfirmationModal";
import { useContext } from "react";

export default function ClusterInformation() {
  const [infoData, setInfoData] = useState({});
  const [loading, setLoading] = useState(false);
  const {
    setClusterInfo,
    cluster,
    cluster: { cluster_name: clusterName },
  } = useContext(AppContent);
  const [clusterNameInput, setClusterNameInput] = useState(clusterName);
  const [clusterNameError, setClusterNameError] = useState("");
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  console.log("Clutser is", cluster);
  const { clusterExists, setSelectedOption } = useContext(AppContent);
  const [showTooltip, setShowTooltip] = useState(false);
  const handleFocus = () => setShowTooltip(true);
  const handleBlur = () => setShowTooltip(false);

  useEffect(() => {
    setSelectedOption("Cluster Information");
  }, []);

  useEffect(() => {
    setClusterNameInput(clusterName);
  }, [clusterName]);

  useEffect(() => {
    if (!clusterNameInput) {
      setClusterNameError("Cluster Name is required");
    } else if (clusterNameInput.length > 80) {
      setClusterNameError("Cluster Name should be less than 80 characters");
    }
    //Should contain only alphanumeric characters
    else if (!/^[a-zA-Z0-9_]*$/.test(clusterNameInput)) {
      setClusterNameError(
        "Cluster Name should contain \n only alphanumeric characters"
      );
    } else {
      setClusterNameError("");
    }
  }, [clusterNameInput]);

  async function getClusterInfoData(requiredClusterName: string = clusterName) {
    setLoading(true);
    const data = await ClusterApi.blueprintInfo(requiredClusterName);
    setInfoData(data as any);
    setLoading(false);
  }

  useEffect(() => {
    if (clusterName) getClusterInfoData();
  }, [clusterName]);

  function downloadBlueprint() {
    const dataStr =
      "data:text/json;charset=utf-8," +
      encodeURIComponent(JSON.stringify(infoData, null, 4));
    const downloadAnchorNode = document.createElement("a");
    downloadAnchorNode.setAttribute("href", dataStr);
    downloadAnchorNode.setAttribute("download", "blueprint.json");
    document.body.appendChild(downloadAnchorNode);
    downloadAnchorNode.click();
    downloadAnchorNode.remove();
  }

  const handleInputChange = (event: any) => {
    setClusterNameInput(event.target.value);
  };

  const saveNewClusterName = async () => {
    try {
      await ClusterApi.updateClusterName(clusterName, clusterNameInput);
      const clusterInfoCopy = cloneDeep(cluster);
      clusterInfoCopy.cluster_name = clusterNameInput;
      setClusterInfo(clusterInfoCopy);
    } catch (err) {
      console.log("Error is", err);
      toast.error("Could not update cluster name");
    } finally {
      setShowConfirmationModal(false);
    }
  };

  return clusterExists ? (
    <div>
      <ConfirmationModal
        successCallback={saveNewClusterName}
        isOpen={showConfirmationModal}
        onClose={() => {
          setShowConfirmationModal(false);
        }}
        modalTitle="Confirm Cluster Name Change"
        modalBody={`Are you sure you want to change the cluster name to ${clusterNameInput}?`}
      />
      <Form
        className="p-2 m-2 d-flex flex-column"
        onSubmit={() => {
          if (!clusterNameError && clusterNameInput !== clusterName)
            setShowConfirmationModal(true);
        }}
      >
        <Form.Group className="d-flex flex-column mb-5">
          <Form.Label>Cluster Name*</Form.Label>
          <div className="d-flex flex-start">
            <div className="d-flex flex-column">
              <OverlayTrigger
                  show={showTooltip}
                  placement="bottom"
                  overlay={
                    <Tooltip id="clusterNameTooltip">
                      <div className="small">
                        Only alpha-numeric characters, up to 80 characters
                      </div>
                    </Tooltip>
                  }
              >
                <Form.Control
                    type="input"
                value={clusterNameInput}
                placeholder="ClusterName"
                className="me-2"
                onChange={handleInputChange}
                onFocus={handleFocus}
                onBlur={handleBlur}
              ></Form.Control>
              </OverlayTrigger>
              {!clusterNameError ? null : (
                <div className="text-danger">{clusterNameError}</div>
              )}
            </div>
            {clusterNameInput !== clusterName && (
              <DefaultButton
                type="submit"
                variant="primary"
                disabled={clusterNameError || clusterNameInput.length > 80}
              >
                Save
              </DefaultButton>
            )}
          </div>
        </Form.Group>
        <Form.Group>
          <Form.Label className="me-auto">Cluster Blueprint</Form.Label>
          <DefaultButton
            variant="primary"
            className="pull-right"
            onClick={downloadBlueprint}
          >
            Download
          </DefaultButton>
          {loading ? (
            <Spinner />
          ) : (
            <Form.Control
              as="textarea"
              value={JSON.stringify(infoData, null, 4)}
              style={{ height: "60vh" }}
              className="mt-3 text-primary"
              disabled
              readOnly
            />
          )}
        </Form.Group>
      </Form>
    </div>
  ) : null;
}
