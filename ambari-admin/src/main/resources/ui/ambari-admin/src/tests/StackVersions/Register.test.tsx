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
import { describe, it, beforeEach, expect } from "vitest";
import { render, waitFor, screen, fireEvent } from "@testing-library/react";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import VersionsApi from "../../api/versions";
import mockVersionDefinitions from "../../__mocks__/mockVersionDefinitions";
import mockRepoDetails from "../../__mocks__/mockRepoDetails";
import AppContent from "../../context/AppContext";
import Register from "../../screens/StackVersions/Register";
import mockOperatingSystems from "../../__mocks__/mockOperatingSystems";

describe("Register Component", () => {
  const mockClusterName = "testCluster";
  const mockContext = {
    cluster: { cluster_name: mockClusterName },
    setSelectedOption: () => "Versions",
  };

  beforeEach(async () => {
    VersionsApi.getVersionOperatingSystems = async () => mockOperatingSystems;
    VersionsApi.getVersionDefinitions = async () => mockVersionDefinitions;
    VersionsApi.getRepoDetails = async () => mockRepoDetails;
  });


  it("renders without crashing", () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory() as any}>
          <Register readOnly={false} />
        </Router>
      </AppContent.Provider>
    );
  });

  it("Check for all Version Definitions to be present on screen", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory() as any}>
          <Register readOnly={false} />
        </Router>
      </AppContent.Provider>
    );
    await waitFor(() => screen.getAllByTestId("version-definition"));
    const versionDefinitions = screen.getAllByTestId("version-definition");
    expect(versionDefinitions.length).toBe(mockVersionDefinitions.items.length);
  });

  it("Check for Stack Services Table to be present on the screen", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory() as any}>
          <Register readOnly={false} />
        </Router>
      </AppContent.Provider>
    );
    await waitFor(() => screen.getByTestId("stack-services"));
    const stackVersionTable = screen.getByTestId("stack-services");
    expect(stackVersionTable).toBeInTheDocument();
  });

  it("Check for Operating Systems to be present on the screen", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory() as any}>
          <Register readOnly={false} />
        </Router>
      </AppContent.Provider>
    );
    await waitFor(() => screen.getAllByTestId("operating-systems"));
    const allOs = screen.getAllByTestId("operating-systems");
    expect(allOs.length).toBe(mockOperatingSystems.operating_systems.length);
  });


  it("renders operating systems with repositories", async () => {
    render(
      <AppContent.Provider value={mockContext}>
        <Router history={createMemoryHistory() as any}>
          <Register readOnly={false} />
        </Router>
      </AppContent.Provider>
    );
    await waitFor(() => screen.getAllByTestId("operating-systems"));
    const operatingSystems = screen.getAllByTestId("operating-systems");
    expect(operatingSystems.length).toBe(mockOperatingSystems.operating_systems.length);

    mockOperatingSystems.operating_systems.forEach((os, index) => {
      const osElement = operatingSystems[index];
      const expectedOsName = `${os.OperatingSystems.os_type} ${os.repositories.map(repo=>{
        return `${repo.Repositories.repo_id}`
      }).join(" ")}Remove`;

      expect(osElement).toHaveTextContent(expectedOsName);
    });
  });

  it('should add an operating system when the dropdown option is clicked', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );
      await waitFor(() => expect(screen.getByRole('button', { name: /Add/i })).toBeInTheDocument());
      const dropdownButton = screen.getByRole('button', { name: /Add/i });
      fireEvent.click(dropdownButton);

      const addableOs = screen.getByText('amazonlinux2');
      fireEvent.click(addableOs);

      expect(screen.getByText('amazonlinux2')).toBeInTheDocument();
  })

  it("should only show the operating systems to be added when remove was clicked", async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );
    await waitFor(() => expect(screen.getByRole('button', { name: /Add/i })).toBeInTheDocument());
    //const osRow = screen.getByTestId('operating-systems').querySelector('div:contains("amazonlinux2")');
    const osRow = screen.getByText('amazonlinux2').closest('.border-bottom');
    if (!osRow) {
      throw new Error("osRow should not be null");
    }
    const removeButton = osRow.querySelector('div.text-danger.cursor-pointer');
    if (!removeButton) {
      throw new Error("removeButton should not be null");
    }
    fireEvent.click(removeButton);
    await waitFor(() => expect(screen.queryByText('amazonlinux2')).not.toBeInTheDocument());

    const addButton = screen.getByRole('button', { name: /Add/i });
    fireEvent.click(addButton);

    const addableOs = screen.getByText('amazonlinux2');
    fireEvent.click(addableOs);

    // Verify the operating system is added back
    await waitFor(() => expect(screen.getByText('amazonlinux2')).toBeInTheDocument());
  })

  it('should  render modal on clicking why is this disabled', async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );
    await waitFor(() => screen.getAllByTestId("operating-systems"));
    const info =  screen.getByText(/Why is this/i);
    fireEvent.click(info);
    expect(screen.getByText('Public Repository Option Disabled')).toBeInTheDocument();
  });

  //it should render Use Local repository
  it('should render Use Local Repository when the checkbox is clicked', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );

      await waitFor( () => expect(screen.getByText(/Use Local Repository/i)).toBeInTheDocument());
  });
  //initially skip validation should be unchecked
  it('should render Skip Validation as unchecked by default', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );
    await waitFor(() => screen.getByTestId('skipValidationCheckbox'));
    await waitFor(() => expect(screen.getByTestId('skipValidationCheckbox')).not.toBeChecked());
  });

  it('should render Use RedHat Satellite/Spacewalk as unchecked by default', async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );
    await waitFor(() => screen.getByTestId('redhatSatelliteCheckbox'));
    await waitFor(() => expect(screen.getByTestId('redhatSatelliteCheckbox')).not.toBeChecked());
  });

  it('should check and uncheck the Skip Validation checkbox', async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );
    const skipValidationCheckbox = await waitFor(() => screen.getByTestId('skipValidationCheckbox'));
    // Initial state should be unchecked
    expect(skipValidationCheckbox).not.toBeChecked();

    // Click the checkbox to toggle its state
    fireEvent.click(skipValidationCheckbox);

    // Verify the checkbox is now checked
    expect(skipValidationCheckbox).toBeChecked();
  })

  it('should check and uncheck the RedHat Satellite/Spacewalk checkbox', async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );
    const redhatSatteliteCheckBox = await waitFor(() => screen.getByTestId('redhatSatelliteCheckbox'));
    // Initial state should be unchecked
    expect(redhatSatteliteCheckBox).not.toBeChecked();

    // Click the checkbox to toggle its state
    fireEvent.click(redhatSatteliteCheckBox);

    // Verify the checkbox is now checked
    expect(redhatSatteliteCheckBox).toBeChecked();
  })

  it('should render the modal when the checkbox is clicked', async () => {
      render(
          <AppContent.Provider value={mockContext}>
            <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
            </Router>
          </AppContent.Provider>
      );

      const redhatSatteliteCheckBox = await waitFor(() => screen.getByTestId('redhatSatelliteCheckbox'));
      fireEvent.click(redhatSatteliteCheckBox);
      await waitFor(() => expect(screen.getByText(/In order for Ambari/i)).toBeInTheDocument());
  })

  //TODO: Add test for the Add Version Modal
  //the test should render the Add Version Modal when Add Version option is clicked
  // it('should render the AddVersionModal when Add Version option is clicked', async () => {
  //   render(
  //       <AppContent.Provider value={mockContext}>
  //         <Router history={createMemoryHistory()}>
  //           <Register readOnly={false} />
  //         </Router>
  //       </AppContent.Provider>
  //   );
  //   await waitFor(() => screen.getByTestId('version-dropdown'));
  //   fireEvent.click(screen.getByTestId('version-dropdown'));
  //
  //
  //   await waitFor(() => screen.getByTestId('add-version-option'));
  //   // Click the "Add Version" option
  //   fireEvent.click(screen.getByTestId('add-version-option'));
  //
  //
  //   // await waitFor(() => {
  //   //   expect(screen.getByText('Upload Version Definition')).toBeInTheDocument();
  //   // });
  // });

  it('should show pencil icon when redhat satellite is checked', async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );
    const redhatSatteliteCheckBox = await waitFor(() => screen.getByTestId('redhatSatelliteCheckbox'));
    expect(redhatSatteliteCheckBox).toBeInTheDocument();
    fireEvent.click(redhatSatteliteCheckBox);

    const pencilIcons = await waitFor(() => screen.getAllByTestId('pencil-icon'));

    expect(pencilIcons.length).toBeGreaterThan(0); // Check that there is at least one pencil icon
    pencilIcons.forEach(icon => {
      expect(icon).toBeInTheDocument(); // Verify each pencil icon is in the document
    });

  });

  it('should render the cancel button', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );

      await waitFor(() => expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument());
  });

  it('should render repo-base-url-input when there is no base url', async () => {
    render(
        <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
            <Register readOnly={false} />
          </Router>
        </AppContent.Provider>
    );

    const baseUrlInputs = await waitFor(() => screen.getAllByTestId('repo-base-url-input'));
    expect(baseUrlInputs.length).toBeGreaterThan(0); // Check that there is at least one pencil icon
    baseUrlInputs.forEach(icon => {
      expect(icon).toBeInTheDocument(); // Verify each pencil icon is in the document
    });
  });

  it('should click on save button when Version number is valid and also ' +
      'display invalid when Version Number is invalid', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );

      const versionInput = await waitFor(() => screen.getByTestId('version-input'));
      fireEvent.change(versionInput, { target: { value: '1.1.0' } });

      // Check for the invalid message
      const invalidMessage = screen.getByText(/invalid/i);
      expect(invalidMessage).toBeInTheDocument();

      // Change the input to a valid version number
      fireEvent.change(versionInput, { target: { value: '1.0' } });

      const saveButton = await waitFor(() => screen.getByRole('button', { name: /Save/i }));
      fireEvent.click(saveButton);
  });

  it('should click on checkbox skip repository url validation and click on save', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );

      const versionInput = await waitFor(() => screen.getByTestId('version-input'));
      fireEvent.change(versionInput, { target: { value: '1.0' } });

      const skipValidationCheckbox = await waitFor(() => screen.getByTestId('skipValidationCheckbox'));
      fireEvent.click(skipValidationCheckbox);

      const saveButton = await waitFor(() => screen.getByRole('button', { name: /Save/i }));
      fireEvent.click(saveButton);
  });

  it('should click on version-dropdown', async () => {
      render(
          <AppContent.Provider value={mockContext}>
          <Router history={createMemoryHistory() as any}>
              <Register readOnly={false} />
          </Router>
          </AppContent.Provider>
      );
      const versionInput = await waitFor(() => screen.getByTestId('version-input'));
      fireEvent.change(versionInput, { target: { value: '1.1' } });

      const versionDropdown = await waitFor(() => screen.getByTestId('version-dropdown'));
      fireEvent.click(versionDropdown);
  });
});
