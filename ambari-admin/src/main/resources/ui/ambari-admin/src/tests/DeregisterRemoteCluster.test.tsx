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
import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import RemoteClusterApi from "../api/remoteCluster";
import DeregisterRemoteCluster from "../screens/ClusterManagement/RemoteClusters/DeregisterRemoteCluster";
import AppContent from "../context/AppContext";
import toast from "react-hot-toast";
import { get } from "lodash";

const mockClusterName = "TestCluster1";
const mockContext = {
  cluster: { cluster_name: mockClusterName },
  setSelectedOption: () => "RemoteCluster",
};

const renderDeregisterRemoteCluster = () => {
  render(
    <AppContent.Provider value={mockContext}>
      <Router history={createMemoryHistory()}>
        <DeregisterRemoteCluster clusterName={mockClusterName} />
      </Router>
    </AppContent.Provider>
  );
};

let mockToastSuccessMessage: string;
let mockToastErrorMessage: string;

toast.success = (message) => {
  mockToastSuccessMessage = message as string;
  return "";
};

toast.error = (message) => {
  mockToastErrorMessage = message as string;
  return "";
};

describe("DeregisterRemoteCluster component", () => {
  it("renders without crashing", () => {
    renderDeregisterRemoteCluster();
  });

  it("should open the confirmation modal on clicking DEREGISTER CLUSTER button", async () => {
    renderDeregisterRemoteCluster();
    const deregisterButton = screen.getByText("DEREGISTER CLUSTER");
    fireEvent.click(deregisterButton);
    const modalTitle = await screen.findByText("Deregister cluster");
    expect(modalTitle).toBeInTheDocument();
  });

  it("should call API and show success toast on clicking Ok button in the modal", async () => {
    vi.spyOn(RemoteClusterApi, "deregisterRemoteCluster").mockResolvedValue({
      status: 200,
    });

    renderDeregisterRemoteCluster();
    const deregisterButton = screen.getByText("DEREGISTER CLUSTER");
    fireEvent.click(deregisterButton);
    const OkButton = await screen.findByText("OK");
    fireEvent.click(OkButton);

    await waitFor(() => {
      expect(get(mockToastSuccessMessage, "props.children", []).join("")).toBe(
        `De-registered cluster "${mockClusterName}" successfully!`
      );
    });
  });

  it("should show error toast when API call fails", async () => {
    vi.spyOn(RemoteClusterApi, 'deregisterRemoteCluster').mockRejectedValue(new Error("API call failed"));

    renderDeregisterRemoteCluster();
    const deregisterButton = screen.getByText("DEREGISTER CLUSTER");
    fireEvent.click(deregisterButton);
    const OkButton = await screen.findByText("OK");
    fireEvent.click(OkButton);

    await waitFor(() => {
      expect(get(mockToastErrorMessage, "props.children", []).join("")).toBe(
        "Error while deregistering cluster: API call failed"
      );
    });
  });
});