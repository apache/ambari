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
import { useManagement } from "../../../context/ManagementContext";
import { useEffect, useState } from "react";
import {Button, Form, OverlayTrigger, Tooltip} from "react-bootstrap";
import DefaultButton from "../../../components/DefaultButton";
import AppContent from "../../../context/AppContext";
import ClusterApi from "../../../api/clusterApi";
import Spinner from "../../../components/Spinner";
import toast from "react-hot-toast";
import ConfirmationModal from "../../../components/ConfirmationModal";
import { useContext } from "react";

export default function ClusterInformation() {
  const { can } = useManagement();
  const [loadError, setLoadError] = useState(false);
  const [saving, setSaving] = useState(false);
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
  const { setSelectedOption, availableClusters, selectCluster } = useContext(AppContent);
  const [showTooltip, setShowTooltip] = useState(false);
  const handleFocus = () => setShowTooltip(true);
  const handleBlur = () => setShowTooltip(false);

  useEffect(() => {
    setSelectedOption("Cluster Details");
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

  useEffect(() => {
    if (!clusterName) return;
    let active = true;
    setInfoData({});
    setLoadError(false);
    setLoading(true);
    ClusterApi.blueprintInfo(clusterName).then((data) => {
      if (active) setInfoData(data);
    }).catch(() => {
      if (active) { setLoadError(true); toast.error("Could not load the cluster blueprint"); }
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
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
    setSaving(true);
    try {
      try { await ClusterApi.updateClusterName(clusterName, clusterNameInput); }
      catch { /* Reconcile a possibly lost response by stable cluster identity. */ }
      const current = await ClusterApi.hostClustersInfo();
      const renamed = current.items?.find((item: { Clusters: { cluster_id: number; cluster_name: string } }) =>
        item.Clusters.cluster_id === cluster.cluster_id && item.Clusters.cluster_name === clusterNameInput);
      if (!renamed) throw new Error("Cluster rename could not be confirmed");
      setClusterInfo({ ...cluster, ...renamed.Clusters });
    } catch {
      toast.error("Could not confirm the cluster name change. Refresh cluster overview before retrying.");
    } finally {
      setSaving(false);
      setShowConfirmationModal(false);
    }
  };

  if (!clusterName && availableClusters?.length > 1) {
    return <div>
      <h4>Choose a cluster</h4>
      <p>Select a cluster to view its details or return to its Dashboard.</p>
      <div className="d-flex flex-wrap gap-2">
        {availableClusters.map((item: { cluster_id: number; cluster_name: string }) => (
          <Button key={item.cluster_id} variant="outline-primary" onClick={() => selectCluster(item.cluster_name)}>
            {item.cluster_name}
          </Button>
        ))}
      </div>
    </div>;
  }

  return clusterName ? (
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
        onSubmit={(event) => {
          event.preventDefault();
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
                disabled={!can("AMBARI.RENAME_CLUSTER") || saving}
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
            {can("AMBARI.RENAME_CLUSTER") && clusterNameInput !== clusterName && (
              <DefaultButton
                type="submit"
                variant="primary"
                disabled={saving || clusterNameError || clusterNameInput.length > 80}
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
            disabled={loading || loadError || !Object.keys(infoData).length}
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
