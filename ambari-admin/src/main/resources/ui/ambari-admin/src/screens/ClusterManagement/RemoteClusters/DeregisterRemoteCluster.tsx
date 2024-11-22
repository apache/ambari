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
import { useHistory } from "react-router-dom";
import { Button } from "react-bootstrap";
import toast from "react-hot-toast";
import RemoteClusterApi from "../../../api/remoteCluster";
import ConfirmationModal from "../../../components/ConfirmationModal";

type PropTypes = {
  clusterName: string;
};

export default function DeregisterRemoteCluster({ clusterName }: PropTypes) {
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);
  const history = useHistory();

  const handleDeleteCluster = (event: any) => {
    event.preventDefault();
    setShowConfirmationModal(true);
  };

  const handleDeleteClusterConfirmed = async () => {
    try {
      await RemoteClusterApi.deregisterRemoteCluster(clusterName);
      toast.success(
        <div className="toast-message">
          De-registered cluster "{clusterName}" successfully!
        </div>
      );
      history.push("/remoteClusters");
    } catch (error: any) {
      toast.error(
        <div className="toast-message">
          Error while deregistering cluster: {error.message}
        </div>
      );
    } finally {
      setShowConfirmationModal(false);
    }
  };

  return (
    <>
      <Button
        onClick={handleDeleteCluster}
        variant="danger"
        className="px-3 rounded-1"
        size="sm"
      >
        DEREGISTER CLUSTER
      </Button>
      <ConfirmationModal
        isOpen={showConfirmationModal}
        onClose={() => setShowConfirmationModal(false)}
        modalTitle="Deregister cluster"
        modalBody={`Are you sure you want to delete cluster "${clusterName}"? This operation cannot be undone.`}
        successCallback={handleDeleteClusterConfirmed}
      />
    </>
  );
}
