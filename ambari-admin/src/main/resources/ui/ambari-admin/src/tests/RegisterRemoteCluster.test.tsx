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
import { describe, it, expect } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import RemoteClusterApi from "../api/remoteCluster";
import RegisterRemoteCluster from "../screens/ClusterManagement/RemoteClusters/RegisterRemoteCluster";
import AppContent from "../context/AppContext";
import toast from "react-hot-toast";

const mockClusterName = "TestCluster1";
const mockContext = {
  cluster: { cluster_name: mockClusterName },
  setSelectedOption: () => "RemoteCluster",
};

const renderRegisterRemoteCluster = () => {
  render(
    <AppContent.Provider value={mockContext}>
      <Router history={createMemoryHistory()}>
        <RegisterRemoteCluster />
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

describe("RegisterRemoteCluster component", () => {
  it("renders without creshing", () => {
    renderRegisterRemoteCluster();
  });

  it("render form for registering remote Cluster", () => {
    renderRegisterRemoteCluster();
    expect(getClusterName()).toBeInTheDocument();
    expect(getClusterUrl()).toBeInTheDocument();
    expect(getClusterUserName()).toBeInTheDocument();
    expect(getPassword()).toBeInTheDocument();
  });

  it("should redirect to /remoteCluster on clicking cancel button", async () => {
    const history = createMemoryHistory();
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={history}>
          <RegisterRemoteCluster />
        </Router>
      </AppContent.Provider>
    );
    expect(getClusterName()).toBeInTheDocument();
    expect(getClusterUrl()).toBeInTheDocument();
    expect(getClusterUserName()).toBeInTheDocument();
    expect(getPassword()).toBeInTheDocument();

    const cancelButton = screen.getByRole("button", { name: /cancel/i });
    expect(cancelButton).toBeInTheDocument();

    fireEvent.click(cancelButton);
    await waitFor(() => {});

    await waitFor(() => {
      expect(history.location.pathname).toBe("/remoteClusters");
    });
  });

  it("should display required errors when input fields are empty", async () => {
    renderRegisterRemoteCluster();
    fireEvent.click(getSaveButton());

    await waitFor(async () => {
      const errorMessages = await screen.findAllByText(/is required/i);
      expect(errorMessages).toHaveLength(4);
    });
  });

  it("should display error when cluster name includes special characters or spaces", async () => {
    renderRegisterRemoteCluster();
    const clusterNameInput = getClusterName();
    fireEvent.change(clusterNameInput, {target: {value: "invalid name@"}});
    fireEvent.keyDown(clusterNameInput, { key: "Tab", code: "Tab" });
    fireEvent.keyUp(clusterNameInput, { key: "Tab", code: "Tab" });

    await waitFor(() => {
      expect(
        screen.getByText("Must not contain any special characters or spaces.")
      ).toBeInTheDocument();
    });
  });

  it("should display error when url is invalid", async () => {
    renderRegisterRemoteCluster();
    const clusterUrlInput = getClusterUrl();
    fireEvent.change(clusterUrlInput, {target: {value: "invalid_url"}});
    fireEvent.keyDown(clusterUrlInput, { key: "Tab", code: "Tab" });
    fireEvent.keyUp(clusterUrlInput, { key: "Tab", code: "Tab" });

    await waitFor(() => {
      expect(screen.getByText("Must be a valid URL.")).toBeInTheDocument();
    });
  });

  it("should submit form when all values are valid", async () => {
    RemoteClusterApi.addRemoteCluster = async () => {
      toast.success(`Cluster "${mockClusterName}" registered successfully`);
      return { status: 200 };
    };

    renderRegisterRemoteCluster();
    fireEvent.change(getClusterName(), { target: { value: "TestCluster1" } });
    fireEvent.change(getClusterUrl(), {
      target: {
        value: "http://clusterHost.example.com:8080/api/v1/clusters/clusterName",
      },
    });
    fireEvent.change(getClusterUserName(), { target: { value: "admin" } });
    fireEvent.change(getPassword(), { target: { value: "admin" } });
    fireEvent.click(getSaveButton());

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe(
        `Cluster "TestCluster1" registered successfully`
      );
    });
  });

  it("should display error when API call fails", async () => {
    RemoteClusterApi.addRemoteCluster = async () => {
      toast.error("Error while adding remote cluster");
      return { status: 400 };
    };

    renderRegisterRemoteCluster();
    fireEvent.change(getClusterName(), { target: { value: "TestCluster1" } });
    fireEvent.change(getClusterUrl(), {
      target: {
        value: "http://clusterHost.example.com:8080/api/v1/clusters/clusterName",
      },
    });
    fireEvent.change(getClusterUserName(), { target: { value: "admin" } });
    fireEvent.change(getPassword(), { target: { value: "admin" } });
    fireEvent.click(getSaveButton());

    await waitFor(() => {
      expect(mockToastErrorMessage).not.toBeUndefined;
      expect(mockToastErrorMessage).toBe("Error while adding remote cluster");
    });
  });
});

function getClusterName() {
  return screen.getByRole("textbox", {
    name: /cluster name/i,
  });
}

function getClusterUrl() {
  return screen.getByRole("textbox", {
    name: /ambari cluster/i,
  });
}

function getClusterUserName() {
  return screen.getByRole("textbox", {
    name: /cluster user/i,
  });
}

function getPassword() {
  return screen.getByLabelText(/password/i);
}

function getSaveButton() {
  return screen.getByRole("button", {
    name: /save/i,
  });
}
