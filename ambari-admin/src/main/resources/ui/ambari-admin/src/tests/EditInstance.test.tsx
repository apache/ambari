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
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { Router } from "react-router-dom";
import EditInstance from "../screens/Views/EditInstance";
import { createMemoryHistory } from "history";
import AppContent from "../context/AppContext";
import toast from "react-hot-toast";
import ViewsInformationApi from "../api/viewsApiInfo";
import "@testing-library/jest-dom/vitest";
import {
  mockGroupData,
  mockInstanceDetails,
  mockPrivileges,
  mockUsersdata,
  mockViewsData,
} from "../__mocks__/mockEditInstance";
import { beforeEach, describe, expect, it } from "vitest";
import UserGroupApi from "../api/userGroupApi";

const mockClusterName = "TestCluster";
const mockContext = {
  cluster: { cluster_name: mockClusterName },
  setSelectedOption: () => "Views",
};

let mockToastSuccessMessage = "";
let mockToastErrorMessage = "";

toast.success = (message) => {
  mockToastSuccessMessage = message as string;
  return "";
};

toast.error = (message) => {
  mockToastErrorMessage = message as string;
  return "";
};

describe("EditInstance Component Tests", () => {
  beforeEach(async () => {
    ViewsInformationApi.getInstanceLabel = async () => mockInstanceDetails;
    ViewsInformationApi.getViewDetails = async () => mockViewsData;
    UserGroupApi.usersList = async () => mockUsersdata;
    UserGroupApi.groupsList = async () => mockGroupData;
    ViewsInformationApi.getPrivileges = async () => mockPrivileges;
    ViewsInformationApi.getViewDetails = async () => mockViewsData;
  });

  const renderEditInstanceComponent = () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <EditInstance />
        </Router>
      </AppContent.Provider>
    );
  };

  it("Should render the Edit Instance component without crashing", () => {
    renderEditInstanceComponent();
  });

  it("renders loading spinner when data is being fetched", async () => {
    renderEditInstanceComponent();
    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("should render all sections", async () => {
    renderEditInstanceComponent();

    await waitFor(() => screen.getByText(/Details/i));

    const details = await screen.getByText(/Details/i);
    expect(details).toBeInTheDocument();
    const settings = await screen.getByText(/Settings/i);
    expect(settings).toBeInTheDocument();
    const permissions = await screen.getByText(/Permissions/i);
    expect(permissions).toBeInTheDocument();
  });

  it("renders form fields correctly", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByTestId(/instanceName/i));

    const instanceNameInput = screen.getByTestId(/instanceName/i);
    const displayNameInput = screen.getByTestId(/displayName/i);
    const descriptionInput = screen.getByTestId(/description/i);

    expect(instanceNameInput).toBeInTheDocument();
    expect(displayNameInput).toBeInTheDocument();
    expect(descriptionInput).toBeInTheDocument();
  });

  it("handles form inputs correctly", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByTestId(/instanceName/i));

    const displayNameInput = screen.getByTestId(/displayName/i);
    const descriptionInput = screen.getByTestId(/description/i);

    fireEvent.change(displayNameInput, {
      target: { value: "Updated Display" },
    });
    fireEvent.change(descriptionInput, {
      target: { value: "Updated Description" },
    });

    expect((displayNameInput as HTMLInputElement).value).toBe(
      "Updated Display"
    );
    expect((descriptionInput as HTMLInputElement).value).toBe(
      "Updated Description"
    );
  });

  it("submits the form and shows success toast on successful API call", async () => {
    ViewsInformationApi.updateDetails = async () => {
      toast.success("Updated instance details successfully");
      return { status: 200 };
    };

    renderEditInstanceComponent();
    await waitFor(() => screen.getByTestId(/instanceName/i));

    const displayNameInput = screen.getByTestId(/displayName/i);
    const descriptionInput = screen.getByTestId(/description/i);

    const editButton = screen.getByTestId("details");
    fireEvent.click(editButton);

    const saveButton = screen.getByTestId("details");

    fireEvent.change(displayNameInput, {
      target: { value: "Updated Display" },
    });
    fireEvent.change(descriptionInput, {
      target: { value: "Updated Description" },
    });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe(
        "Updated instance details successfully"
      );
    });
  });

  it("should display warnings for incorrect form inputs", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByTestId(/instanceName/i));

    const displayNameInput = screen.getByTestId(/displayName/i);

    const editButton = screen.getByTestId("details");
    fireEvent.click(editButton);

    const saveButton = screen.getByTestId("details");
    fireEvent.change(displayNameInput, { target: { value: "" } });

    fireEvent.click(saveButton);

    const warning1 = screen.getByText(/Field is required/i);
    const warning2 = screen.getByText(
      /Must not contain any special characters/i
    );
    expect(warning1).toBeInTheDocument();
    expect(warning2).toBeInTheDocument();
  });

  it("should display Create Short URL", async () => {
    const mockInstanceDateCreateURL = {
      href: "http://example.com/root",
      ViewInstanceInfo: {
        cluster_handle: 123,
        cluster_type: "HDFS",
        context_path: "/context/path",
        description: "A description of the view instance",
        icon64_path: null,
        icon_path: null,
        instance_name: "Instance1",
        label: "Instance Label",
        static: false,
        validation_result: {
          valid: true,
          detail: "Validation successful",
        },
        version: "1.0",
        view_name: "View1",
        visible: true,
        instance_data: {},
        properties: {
          "hdfs.auth_to_local": {
            viewInfo: {
              name: "hdfs.auth_to_local",
              description: "Description for hdfs.auth_to_local",
              label: "HDFS Auth To Local",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "hdfs.umask-mode": {
            viewInfo: {
              name: "hdfs.umask-mode",
              description: "Description for hdfs.umask-mode",
              label: "HDFS Umask Mode",
              placeholder: null,
              defaultValue: "022",
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: "022",
              isSetting: false,
            },
          },
          "tmp.dir": {
            viewInfo: {
              name: "tmp.dir",
              description: "Description for tmp.dir",
              label: "Temporary Directory",
              placeholder: "/tmp",
              defaultValue: "/tmp",
              clusterConfig: null,
              required: true,
              masked: false,
              value: "/tmp",
              isSetting: false,
            },
          },
          "view.conf.keyvalues": {
            viewInfo: {
              name: "view.conf.keyvalues",
              description: "Description for view.conf.keyvalues",
              label: "View Config Key Values",
              placeholder: null,
              defaultValue: null,
              clusterConfig: null,
              required: false,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.auth": {
            viewInfo: {
              name: "webhdfs.auth",
              description: "Description for webhdfs.auth",
              label: "WebHDFS Auth",
              placeholder: "auth-placeholder",
              defaultValue: null,
              clusterConfig: null,
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.client.failover.proxy.provider": {
            viewInfo: {
              name: "webhdfs.client.failover.proxy.provider",
              description:
                "Description for webhdfs.client.failover.proxy.provider",
              label: "WebHDFS Client Failover Proxy Provider",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.ha.namenode.http-address.list": {
            viewInfo: {
              name: "webhdfs.ha.namenode.http-address.list",
              description:
                "Description for webhdfs.ha.namenode.http-address.list",
              label: "WebHDFS HA Namenode HTTP Address List",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.ha.namenode.https-address.list": {
            viewInfo: {
              name: "webhdfs.ha.namenode.https-address.list",
              description:
                "Description for webhdfs.ha.namenode.https-address.list",
              label: "WebHDFS HA Namenode HTTPS Address List",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.ha.namenode.rpc-address.list": {
            viewInfo: {
              name: "webhdfs.ha.namenode.rpc-address.list",
              description:
                "Description for webhdfs.ha.namenode.rpc-address.list",
              label: "WebHDFS HA Namenode RPC Address List",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.ha.namenodes.list": {
            viewInfo: {
              name: "webhdfs.ha.namenodes.list",
              description: "Description for webhdfs.ha.namenodes.list",
              label: "WebHDFS HA Namenodes List",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.nameservices": {
            viewInfo: {
              name: "webhdfs.nameservices",
              description: "Description for webhdfs.nameservices",
              label: "WebHDFS Nameservices",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.url": {
            viewInfo: {
              name: "webhdfs.url",
              description: "Description for webhdfs.url",
              label: "WebHDFS URL",
              placeholder: null,
              defaultValue: null,
              clusterConfig: "cluster-config",
              required: true,
              masked: false,
              value: null,
              isSetting: false,
            },
          },
          "webhdfs.username": {
            viewInfo: {
              name: "webhdfs.username",
              description: "Description for webhdfs.username",
              label: "WebHDFS Username",
              placeholder: "username-placeholder",
              defaultValue: "default-username",
              clusterConfig: null,
              required: true,
              masked: false,
              value: "default-username",
              isSetting: false,
            },
          },
        },
        property_validation_results: {
          "hdfs.auth_to_local": {
            valid: true,
            detail: "Validation successful for hdfs.auth_to_local",
          },
          "hdfs.umask-mode": {
            valid: true,
            detail: "Validation successful for hdfs.umask-mode",
          },
          "tmp.dir": {
            valid: true,
            detail: "Validation successful for tmp.dir",
          },
          "view.conf.keyvalues": {
            valid: true,
            detail: "Validation successful for view.conf.keyvalues",
          },
          "webhdfs.auth": {
            valid: true,
            detail: "Validation successful for webhdfs.auth",
          },
          "webhdfs.client.failover.proxy.provider": {
            valid: true,
            detail:
              "Validation successful for webhdfs.client.failover.proxy.provider",
          },
          "webhdfs.ha.namenode.http-address.list": {
            valid: true,
            detail:
              "Validation successful for webhdfs.ha.namenode.http-address.list",
          },
          "webhdfs.ha.namenode.https-address.list": {
            valid: true,
            detail:
              "Validation successful for webhdfs.ha.namenode.https-address.list",
          },
          "webhdfs.ha.namenode.rpc-address.list": {
            valid: true,
            detail:
              "Validation successful for webhdfs.ha.namenode.rpc-address.list",
          },
          "webhdfs.ha.namenodes.list": {
            valid: true,
            detail: "Validation successful for webhdfs.ha.namenodes.list",
          },
          "webhdfs.nameservices": {
            valid: true,
            detail: "Validation successful for webhdfs.nameservices",
          },
          "webhdfs.url": {
            valid: true,
            detail: "Validation successful for webhdfs.url",
          },
          "webhdfs.username": {
            valid: true,
            detail: "Validation successful for webhdfs.username",
          },
        },
      },
      privileges: [
        {
          href: "http://example.com/privilege/1",
          PrivilegeInfo: {
            instance_name: "Instance1",
            permission_label: "Read",
            permission_name: "READ_PRIVILEGE",
            principal_name: "User1",
            principal_type: "USER",
            privilege_id: 1,
            version: "1.0",
            view_name: "View1",
          },
        },
        {
          href: "http://example.com/privilege/2",
          PrivilegeInfo: {
            instance_name: "Instance1",
            permission_label: "Write",
            permission_name: "WRITE_PRIVILEGE",
            principal_name: "User2",
            principal_type: "USER",
            privilege_id: 2,
            version: "1.0",
            view_name: "View1",
          },
        },
      ],
      resources: [
        {
          href: "http://example.com/resource/1",
          instance_name: "Instance1",
          name: "Resource1",
          version: "1.0",
          view_name: "View1",
        },
        {
          href: "http://example.com/resource/2",
          instance_name: "Instance1",
          name: "Resource2",
          version: "1.0",
          view_name: "View1",
        },
      ],
    };

    ViewsInformationApi.getInstanceLabel = async () =>
      mockInstanceDateCreateURL;

    renderEditInstanceComponent();

    await waitFor(() => screen.getByText(/Instance Name/i));

    const permissions = await screen.getByText(/Create new URL/i);
    expect(permissions).toBeInTheDocument();
  });

  it("should display options in the select users according to the data obtained from the API", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByText(/Grant permission to these users/i));

    const userSelect = screen.getByLabelText("select-users");
    const groupSelect = screen.getByLabelText("select-groups");

    expect(userSelect).toBeInTheDocument();
    expect(groupSelect).toBeInTheDocument();
  });

  it("should update selected users and groups correctly", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByText(/Grant permission to these users/i));

    const userSelect = screen.getByLabelText("select-users");
    const groupSelect = screen.getByLabelText("select-groups");

    if (userSelect.firstChild) {
      fireEvent.keyDown(userSelect.firstChild, { key: "ArrowDown" });
    }
    await waitFor(() => screen.getByText("User1"));
    fireEvent.click(screen.getByText("User1"));

    if (groupSelect.firstChild) {
      fireEvent.keyDown(groupSelect.firstChild, { key: "ArrowDown" });
    }
    await waitFor(() => screen.getByText("Group1"));
    fireEvent.click(screen.getByText("Group1"));

    // Verify the selected values
    expect(screen.getByText("User1")).toBeInTheDocument();
    expect(screen.getByText("Group1")).toBeInTheDocument();
  });

  it("calls delete short URL API and shows success toast on confirmation, if URL exists ", async () => {
    ViewsInformationApi.deleteShortUrl = async () => {
      toast.success("short Url deleted");
      return { status: 200 };
    };
    renderEditInstanceComponent();
    await waitFor(() => screen.getByText(/Details/i));
    const editButton = screen.getByTestId("details");
    fireEvent.click(editButton);

    const deleteButton = screen.getByText("Delete");
    fireEvent.click(deleteButton);

    const confirmButton = await screen.findByText(/OK/i);
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe("Short Url deleted");
    });
  });

  it("Error toast on Delete short URL API failure ", async () => {
    ViewsInformationApi.deleteShortUrl = async () => {
      toast.error("Failed to delete URL");
      return { status: 400 };
    };
    renderEditInstanceComponent();
    await waitFor(() => screen.getByText(/Details/i));
    const editButton = screen.getByTestId("details");
    fireEvent.click(editButton);

    const deleteButton = screen.getByText("Delete");
    fireEvent.click(deleteButton);

    const confirmButton = await screen.findByText(/OK/i);
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockToastErrorMessage).not.toBeUndefined;
      expect(mockToastErrorMessage).toBe("Failed to delete URL");
    });
  });

  it("should delete the instance", async () => {
    ViewsInformationApi.deleteInstance = async () => {
      toast.success(`Instance deleted successfully`);
      return { status: 200 };
    };

    renderEditInstanceComponent();
    await waitFor(() => screen.getByText(/DELETE INSTANCE/i));

    const deleteIcon = screen.getByText(/DELETE INSTANCE/i);
    fireEvent.click(deleteIcon);
    const yesButton = await screen.findByText("OK");
    fireEvent.click(yesButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe(`Instance deleted successfully`);
    });
  });

  it("updates checkbox state correctly when clicked", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByText("Local Cluster Permissons"), {
      timeout: 5000,
    });

    const clusterAdministratorBox = screen.getByLabelText(
      "Cluster Administrator"
    );
    const clusterOperatorBox = screen.getByLabelText("Cluster Operator");
    const serviceAdministratorBox = screen.getByLabelText(
      "Service Administrator"
    );
    const clusterUserCheckbox = screen.getByLabelText("Cluster User");
    const serviceOperator = screen.getByLabelText("Service Operator");

    expect(clusterAdministratorBox).toBeInTheDocument();
    expect(clusterOperatorBox).toBeInTheDocument();
    expect(clusterAdministratorBox).not.toBeChecked();
    expect(clusterOperatorBox).not.toBeChecked();
    expect(serviceAdministratorBox).not.toBeChecked();
    expect(serviceAdministratorBox).not.toBeChecked();
    expect(clusterUserCheckbox).not.toBeChecked();
    expect(serviceOperator).not.toBeChecked();

    fireEvent.click(clusterAdministratorBox);
    fireEvent.click(clusterOperatorBox);
    fireEvent.click(serviceAdministratorBox);
    fireEvent.click(clusterUserCheckbox);
    fireEvent.click(serviceOperator);

    await waitFor(() => {
      expect(clusterAdministratorBox).toBeChecked();
      expect(clusterOperatorBox).toBeChecked();
      expect(serviceAdministratorBox).toBeChecked();
      expect(clusterUserCheckbox).toBeChecked();
      expect(serviceOperator).toBeChecked();
    });
  });

  it("should select all and clear all", async () => {
    renderEditInstanceComponent();
    await waitFor(() => screen.getByText("Local Cluster Permissons"), {
      timeout: 5000,
    });

    const checkAll = screen.getByTestId("check-all");
    expect(checkAll).toBeInTheDocument();

    fireEvent.click(checkAll);
    const clusterAdministratorBox = screen.getByLabelText(
      "Cluster Administrator"
    );
    const clusterOperatorBox = screen.getByLabelText("Cluster Operator");
    const serviceAdministratorBox = screen.getByLabelText(
      "Service Administrator"
    );

    expect(clusterAdministratorBox).toBeChecked();
    expect(clusterOperatorBox).toBeChecked();
    expect(serviceAdministratorBox).toBeChecked();
  });

  it("Calls the update privileges API, and displays a success toast", async () => {
    ViewsInformationApi.updatePrivileges = async () => {
      toast.success("Updated permissions");
      return { status: 200 };
    };

    renderEditInstanceComponent();
    await waitFor(() => screen.getByText("Local Cluster Permissons"), {
      timeout: 5000,
    });

    const clusterUserCheckbox = screen.getByLabelText("Cluster Administrator");
    expect(clusterUserCheckbox).toBeInTheDocument();
    expect(clusterUserCheckbox).not.toBeChecked();

    fireEvent.click(clusterUserCheckbox);

    await waitFor(() => {
      expect(clusterUserCheckbox).toBeChecked();
      expect(mockToastSuccessMessage).not.toBeUndefined;
      expect(mockToastSuccessMessage).toBe("Updated permissions");
    });
  });

  it("should not render cluster role permissions when clusster type is not local ambari", async () => {
    mockInstanceDetails.ViewInstanceInfo.cluster_type = "REOTE_AMBARI";
    ViewsInformationApi.getInstanceLabel = async () => mockInstanceDetails;

    renderEditInstanceComponent();

    await waitFor(() => screen.getByText(/Permissions/i));

    const clusterPermissions = screen.queryByText(
      "Cluster Roles is only available"
    );

    expect(clusterPermissions).not.toBeInTheDocument();
  });
});
