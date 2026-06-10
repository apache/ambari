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
import { describe, it, beforeEach, expect} from "vitest";
import {render, screen, waitFor} from "@testing-library/react";
import {Router} from "react-router-dom";
import AppContent from "../../../src/context/AppContext";
import "@testing-library/jest-dom/vitest";
import { createMemoryHistory } from "history";
import ClusterApi from "../../../src/api/clusterApi";
import ClusterInformation from "../../../src/screens/ClusterManagement/ClusterInformation";
import mockClusterBluePrintInfo from "../../__mocks__/mockClusterBluePrintInfo.ts";
import mockUpdateClusterName from "../../__mocks__/mockUpdateClusterName.ts";

describe('Cluster is Installed', () => {
  const mockData = {
    isInstallWizardLaunched: false,
    clusterExists: true,
    selectedOption: '',
    cluster: {
      cluster_name: "abc"
    },
    setClusterInfo: () => {},
    rbacData: {},
    setRbacData: () => {},
    permissionLabelList: [],
    setPermissionLabelList: () => {},
    setSelectedOption: () => "Versions",
  };


  beforeEach(() => {
    //Mock window object
    const { location } = window;
    //delete global.window.location;
    // @ts-ignore
    global.window.location = { ...location,
      replace: ():any => {},
      hash: '/clusterInformation' // this line is for setting the hash
    };
    const url = "http://localhost";
    global.window.location.href = url;
    ClusterApi.blueprintInfo = async () => mockClusterBluePrintInfo;
    ClusterApi.updateClusterName = mockUpdateClusterName;
  });

  it('should render without crashing', () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );
  });

  it('should update the cluster name', async () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );
    const oldClusterName = 'oldName';
    const newClusterName = 'newName';
    const expectedParams = { clusterName: oldClusterName, updatedClusterName: newClusterName };
    const result = await ClusterApi.updateClusterName(oldClusterName, newClusterName);
    // Assert
    expect(result).toEqual(expectedParams);
  });

  it('Cluster Blueprint should be present', async () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );
    await waitFor(() => {
      const textElement = screen.getByText(/Cluster Blueprint/i);
      console.log("text element is ", textElement.textContent);
      expect(textElement).toBeInTheDocument();
    });
  });

  it('Download button should be present', async () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );

    await waitFor(() => {
      const downloadButton = screen.getByText(/Download/i);
      console.log("text element is ", downloadButton.textContent);
      expect(downloadButton).toBeTruthy();
    });
  });

  it('Compulsory field Cluster Name should be present', async () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );

    await waitFor(() => {
      const downloadButton = screen.getByText(/Cluster Name*/i);
      console.log("text element is ", downloadButton.textContent);
      expect(downloadButton).toBeTruthy();
    });
  });

  it('should call the download methods the correct number of times', () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );
    // Save original methods
    const originalCreateElement = document.createElement;
    const originalAppendChild = document.body.appendChild;

    // Setup our mock methods with counters
    let createElementCounter = 0;
    let setAttributeCounter = 0;
    let appendChildCounter = 0;
    let clickCounter = 0;
    let removeCounter = 0;

    // Mock the relevant methods to increment counters
    document.createElement = (() => {
      createElementCounter++;
      return {
        setAttribute: () => setAttributeCounter++,
        click: () => clickCounter++,
        remove: () => removeCounter++
      };
    }) as any;

    document.body.appendChild = (() => appendChildCounter++) as any;

    // Simulate click event on the download button
    const downloadButton = screen.getByText(/Download/i);
    console.log("download button is ", downloadButton);
    downloadButton.click();

    // Check that the methods were called
    expect(createElementCounter).toBe(1);
    expect(setAttributeCounter).toBe(2);
    expect(appendChildCounter).toBe(1);
    expect(clickCounter).toBe(1);
    expect(removeCounter).toBe(1);

    // Restore original methods
    document.createElement = originalCreateElement;
    document.body.appendChild = originalAppendChild;
  });
});