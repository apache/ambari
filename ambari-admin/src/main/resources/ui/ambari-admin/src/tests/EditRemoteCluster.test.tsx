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
import { describe, it, expect, beforeEach } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import { mockClusterDataForEdit } from "../__mocks__/mockRemoteCluster";
import "@testing-library/jest-dom/vitest";
import RemoteClusterApi from "../api/remoteCluster";
import EditRemoteCluster from "../screens/ClusterManagement/RemoteClusters/EditRemoteCluster";
import AppContent from "../context/AppContext";
import toast from "react-hot-toast";

const mockClusterName = "TestCluster1";
const mockContext = {
  cluster: { cluster_name: mockClusterName },
  setSelectedOption: () => "RemoteCluster",
};

const renderEditRemoteCluster = () => {
  render(
    <AppContent.Provider value={mockContext}>
      <Router history={createMemoryHistory()}>
        <EditRemoteCluster />
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

describe("EditRemoteCluster component", () => {
  beforeEach(() => {
    mockToastErrorMessage = "";
    mockToastSuccessMessage = "";
  });

  it("renders without crashing", () => {
    renderEditRemoteCluster();
  });

  it("shows loading spinner when data is being fetched.", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () => [];
    renderEditRemoteCluster();

    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("should display current details of remote cluster", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    renderEditRemoteCluster();

    await waitFor(() => {});
    expect(getClusterNameInput()).toHaveValue(
      mockClusterDataForEdit.ClusterInfo.name
    );
    expect(getclusterUrlInput()).toHaveValue(
      mockClusterDataForEdit.ClusterInfo.url
    );
  });

  it("should redirect to /remoteCluster on clicking cancel button", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    const history = createMemoryHistory();
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={history}>
          <EditRemoteCluster />
        </Router>
      </AppContent.Provider>
    );

    await waitFor(() => {});
    const cancelButton = screen.getByRole("button", { name: /cancel/i });
    expect(cancelButton).toBeInTheDocument();

    fireEvent.click(cancelButton);
    await waitFor(() => {});

    await waitFor(() => {
      expect(history.location.pathname).toBe("/remoteClusters");
    });
  });

  it("loads data and displays update credential modal with username, password input fields, and buttons", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    renderEditRemoteCluster();

    await waitFor(() => {
      expect(getClusterNameInput()).toHaveValue(
        mockClusterDataForEdit.ClusterInfo.name
      );
      expect(getclusterUrlInput()).toHaveValue(
        mockClusterDataForEdit.ClusterInfo.url
      );
    });

    const updateCredentialButton = screen
      .getByTestId("updateCredentialButton")
      .querySelector("button");
    if (updateCredentialButton) {
      fireEvent.click(updateCredentialButton);
    } else {
      throw new Error("Update credential button not found");
    }

    // Wait for the modal to be rendered
    await waitFor(() => {
      const dialogs = screen.getAllByRole("dialog");
      expect(dialogs.length).toBe(1);

      const updateCredentialsDialog = dialogs[0];
      expect(
        within(updateCredentialsDialog).getByText(/Update Credentials/i)
      ).toBeInTheDocument();
    });

    // Check if the user and password fields are rendered
    const updateCredentialsDialog = screen.getAllByRole("dialog")[0];
    expect(
      within(updateCredentialsDialog).getByLabelText(/Cluster user/i)
    ).toBeInTheDocument();
    expect(
      within(updateCredentialsDialog).getByTestId("password-input")
    ).toBeInTheDocument();

    // Check if the cancel and update buttons are rendered
    expect(
      within(updateCredentialsDialog).getByRole("button", { name: /Cancel/i })
    ).toBeInTheDocument();
    expect(
      within(updateCredentialsDialog).getByRole("button", { name: /Update/i })
    ).toBeInTheDocument();
  });

  it("should update cluster name after submitting", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    RemoteClusterApi.updateRemoteCluster = async () => {
      toast.success(`Cluster "UpdatedClusterName" updated successfully`);
      return { status: 200 };
    };
    renderEditRemoteCluster();

    await waitFor(() => {
      expect(
        screen.getByDisplayValue(mockClusterDataForEdit.ClusterInfo.name)
      ).toBeInTheDocument();
    });
    fireEvent.change(
      screen.getByDisplayValue(mockClusterDataForEdit.ClusterInfo.name),
      { target: { value: "UpdatedClusterName" } }
    );
    fireEvent.click(getSaveButton());
    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        'Cluster "UpdatedClusterName" updated successfully'
      );
    });
  });

  it("should display error when API call fails for clustername", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    RemoteClusterApi.updateRemoteCluster = async () => {
      toast.error("Error while updating remote cluster");
      return { status: 400 };
    };
    renderEditRemoteCluster();

    await waitFor(() => {
      expect(
        screen.getByDisplayValue(mockClusterDataForEdit.ClusterInfo.name)
      ).toBeInTheDocument();
    });
    fireEvent.change(
      screen.getByDisplayValue(mockClusterDataForEdit.ClusterInfo.name),
      { target: { value: "UpdatedClusterName" } }
    );
    fireEvent.click(getSaveButton());

    await waitFor(() => {
      expect(mockToastErrorMessage).toBe("Error while updating remote cluster");
    });
  });

  it("should show modal and populate username when clicking update credentials button", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    renderEditRemoteCluster();

    await waitFor(() => {
      expect(getClusterNameInput()).toHaveValue(
        mockClusterDataForEdit.ClusterInfo.name
      );
    });
    const updateCredentialButton = screen
      .getByTestId("updateCredentialButton")
      .querySelector("button");
    if (updateCredentialButton) {
      fireEvent.click(updateCredentialButton);
    } else {
      throw new Error("Update credential button not found");
    }

    // Wait for the modal to be rendered
    await waitFor(() => {
      const dialogs = screen.getAllByRole("dialog");
      expect(dialogs.length).toBe(1);

      const updateCredentialsDialog = dialogs[0];
      expect(
        within(updateCredentialsDialog).getByText(/Update Credentials/i)
      ).toBeInTheDocument();
    });

    // Check if the username field is populated
    const updateCredentialsDialog = screen.getAllByRole("dialog")[0];
    expect(
      within(updateCredentialsDialog).getByLabelText(/Cluster user/i)
    ).toHaveValue(mockClusterDataForEdit.ClusterInfo.username);
  });

  it("should display success message when updating credentials successfully", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    RemoteClusterApi.updateRemoteCluster = async () => {
      toast.success(
        `Credentials for Cluster "${mockClusterName}" updated successfully.`
      );
      return { status: 200 };
    };
    renderEditRemoteCluster();

    await waitFor(() => {
      expect(getClusterNameInput()).toHaveValue(
        mockClusterDataForEdit.ClusterInfo.name
      );
    });

    const updateCredentialButton = screen
      .getByTestId("updateCredentialButton")
      .querySelector("button");
    if (updateCredentialButton) {
      fireEvent.click(updateCredentialButton);
    } else {
      throw new Error("Update credential button not found");
    }

    // Wait for the modal to be rendered
    await waitFor(() => {
      const dialogs = screen.getAllByRole("dialog");
      expect(dialogs.length).toBe(1);

      const updateCredentialsDialog = dialogs[0];
      expect(
        within(updateCredentialsDialog).getByText(/Update Credentials/i)
      ).toBeInTheDocument();
    });

    // Update username and password
    const updateCredentialsDialog = screen.getAllByRole("dialog")[0];
    fireEvent.change(
      within(updateCredentialsDialog).getByLabelText(/Cluster user/i),
      { target: { value: "newuser" } }
    );
    fireEvent.change(
      within(updateCredentialsDialog).getByTestId("password-input"),
      { target: { value: "newPassword" } }
    );
    fireEvent.click(
      within(updateCredentialsDialog).getByRole("button", { name: /Update/i })
    );

    await waitFor(() => {
      expect(mockToastSuccessMessage).toBe(
        `Credentials for Cluster "${mockClusterName}" updated successfully.`
      );
    });
  });

  it("should display error message when updating credentials fails", async () => {
    RemoteClusterApi.getRemoteClusterByName = async () =>
      mockClusterDataForEdit;
    RemoteClusterApi.updateRemoteCluster = async () => {
      toast.error("Error while updating credentials");
      return { status: 400 };
    };

    renderEditRemoteCluster();

    await waitFor(() => {
      expect(getClusterNameInput()).toHaveValue(
        mockClusterDataForEdit.ClusterInfo.name
      );
    });

    const updateCredentialButton = screen
      .getByTestId("updateCredentialButton")
      .querySelector("button");
    if (updateCredentialButton) {
      fireEvent.click(updateCredentialButton);
    } else {
      throw new Error("Update credential button not found");
    }

    // Wait for the modal to be rendered
    await waitFor(() => {
      const dialogs = screen.getAllByRole("dialog");
      expect(dialogs.length).toBe(1);

      const updateCredentialsDialog = dialogs[0];
      expect(
        within(updateCredentialsDialog).getByText(/Update Credentials/i)
      ).toBeInTheDocument();
    });

    // Update username and password
    const updateCredentialsDialog = screen.getAllByRole("dialog")[0];
    fireEvent.change(
      within(updateCredentialsDialog).getByLabelText(/Cluster User/i),
      { target: { value: "newUser" } }
    );
    fireEvent.change(
      within(updateCredentialsDialog).getByTestId("password-input"),
      { target: { value: "newPassword" } }
    );
    fireEvent.click(
      within(updateCredentialsDialog).getByRole("button", { name: /Update/i })
    );

    await waitFor(() => {
      expect(mockToastErrorMessage).toBe("Error while updating credentials");
    });
  });
});

function getClusterNameInput() {
  return screen.getByLabelText(/Cluster Name/i);
}

function getclusterUrlInput() {
  return screen.getByLabelText(/Ambari Cluster URL/i);
}

function getSaveButton() {
  return screen.getByRole("button", {
    name: /save/i,
  });
}
