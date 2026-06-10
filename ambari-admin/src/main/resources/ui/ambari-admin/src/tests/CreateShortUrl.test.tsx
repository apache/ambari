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
import { describe, it, beforeEach, expect } from "vitest";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import CreateShortUrl from "../screens/Views/CreateShortUrl";
import ViewsInformationApi from "../api/viewsApiInfo";
import AppContent from "../context/AppContext";
import toast from "react-hot-toast";
import { mockViewsList } from "../__mocks__/mockViewsList";
import "@testing-library/jest-dom/vitest";

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

describe("CreateShortUrl", () => {
  const mockContext = {
    cluster: { cluster_name: "testCluster" },
    setSelectedOption: () => "Views",
  };

  const renderCreateShortUrlComponent = () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory()}>
          <CreateShortUrl />
        </Router>
      </AppContent.Provider>
    );
  };

  beforeEach(async () => {
    ViewsInformationApi.viewsListAPI = async () => mockViewsList;
  });

  it("renders CreateShortUrl component without crashing", () => {
    renderCreateShortUrlComponent();
  });

  it("renders loading spinner when data is being fetched", async () => {
    renderCreateShortUrlComponent();
    const spinner = screen.getByTestId("admin-spinner");
    expect(spinner).toBeInTheDocument();
  });

  it("renders form fields correctly", async () => {
    renderCreateShortUrlComponent();
    await waitFor(() => screen.getByText(/Create New URL/i));

    const nameInput = screen.getByText(/Name/i);
    const shortUrlInput = screen.getByText(/Short URL/i);

    expect(nameInput).toBeInTheDocument();
    expect(shortUrlInput).toBeInTheDocument();
  });

  it("handles form inputs correctly", async () => {
    renderCreateShortUrlComponent();
    await waitFor(() => screen.getByText(/Create New URL/i));

    const nameInput = screen.getByTestId(/name-input/i);
    const shortUrlInput = screen.getByTestId(/shorturl-input/i);

    fireEvent.change(nameInput, { target: { value: "TestName" } });
    fireEvent.change(shortUrlInput, { target: { value: "testurl" } });

    expect((nameInput as HTMLInputElement).value).toBe("TestName");
    expect((shortUrlInput as HTMLInputElement).value).toBe("testurl");
  });

  it("should render views, instance select", async () => {
    renderCreateShortUrlComponent();
    await waitFor(() => screen.getByText(/Create New URL/i));

    const viewSelect = screen.getByLabelText(/view-select/i);
    const instanceSelect = screen.getByLabelText(/instance-select/i);

    fireEvent.mouseDown(viewSelect);
    await waitFor(() => {
      expect(screen.getByText(/Item 1 view/i)).toBeInTheDocument();
    });

    const selectedView = screen.getByText(/Item 1 view/i);
    fireEvent.click(selectedView);

    fireEvent.mouseDown(instanceSelect);
    await waitFor(() => {
      expect(screen.getByText(/Instance 1/i)).toBeInTheDocument();
    });

    const selectedInstance = screen.getByText(/Instance 1/i);
    fireEvent.click(selectedInstance);

    expect(screen.getByText(/Instance 1/i)).toBeInTheDocument();
    expect(screen.getByText(/Item 1 view/i)).toBeInTheDocument();
  });

  it("submits the form and shows success toast on successful API call", async () => {
    ViewsInformationApi.createShortUrl = async () => {
      toast.success("URL created successfully");
      return { status: 200 };
    };
    renderCreateShortUrlComponent();
    await waitFor(() => screen.getByText(/Name/i));

    const nameInput = screen.getByTestId(/name-input/i);
    const shortUrlInput = screen.getByTestId(/shorturl-input/i);
    const viewSelect = screen.getByLabelText(/view-select/i);
    const instanceSelect = screen.getByLabelText(/instance-select/i);
    fireEvent.change(nameInput, { target: { value: "TestName" } });
    fireEvent.change(shortUrlInput, { target: { value: "testurl" } });
    fireEvent.mouseDown(viewSelect);
    const selectedView = screen.getByText(/Item 1 view/i);
    fireEvent.click(selectedView);

    fireEvent.mouseDown(instanceSelect);
    const selectedInstance = screen.getByText(/Instance 1/i);
    fireEvent.click(selectedInstance);

    const saveButton = screen.getByText(/SAVE/i);

    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastSuccessMessage).not.toBeUndefined();
      expect(mockToastSuccessMessage).toBe("URL created successfully");
    });
  });

  it("shows warnings on submitting empty inputs", async () => {
    renderCreateShortUrlComponent();

    await waitFor(() => screen.getByText(/Name/i));
    const saveButton = screen.getByText(/SAVE/i);
    fireEvent.click(saveButton);
    const warnings = screen.getAllByText(/Required/i);

    expect(warnings.length).toBe(3);
  });

  it("shows error toast on API call failure", async () => {
    ViewsInformationApi.createShortUrl = async () => {
      toast.error("Error creating URL");
      return { status: 404 };
    };
    renderCreateShortUrlComponent();
    await waitFor(() => screen.getByText(/Name/i));

    const nameInput = screen.getByTestId(/name-input/i);
    const shortUrlInput = screen.getByTestId(/shorturl-input/i);
    const viewSelect = screen.getByLabelText(/view-select/i);
    const instanceSelect = screen.getByLabelText(/instance-select/i);
    fireEvent.change(nameInput, { target: { value: "TestName" } });
    fireEvent.change(shortUrlInput, { target: { value: "testurl" } });
    fireEvent.mouseDown(viewSelect);
    const selectedView = screen.getByText(/Item 1 view/i);
    fireEvent.click(selectedView);

    fireEvent.mouseDown(instanceSelect);
    const selectedInstance = screen.getByText(/Instance 1/i);
    fireEvent.click(selectedInstance);

    const saveButton = screen.getByText(/SAVE/i);

    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockToastErrorMessage).not.toBeUndefined();
      expect(mockToastErrorMessage).toBe("Error creating URL");
    });
  });
});
