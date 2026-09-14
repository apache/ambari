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
import { describe, it, beforeEach, expect, vi} from "vitest";
import {fireEvent, render, screen, waitFor} from "@testing-library/react";
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

  it('downloads the loaded blueprint as blueprint.json', async () => {
    render(
        <Router history={createMemoryHistory()}>
          <AppContent.Provider value={mockData}>
            <ClusterInformation />
          </AppContent.Provider>
        </Router>
    );
    const downloadButton = await screen.findByRole('button', {name: /download/i});
    await waitFor(() => expect(downloadButton).not.toBeDisabled());

    const anchor = document.createElement('a');
    const click = vi.spyOn(anchor, 'click').mockImplementation(() => undefined);
    const remove = vi.spyOn(anchor, 'remove').mockImplementation(() => undefined);
    const originalCreateElement = document.createElement.bind(document);
    const createElement = vi.spyOn(document, 'createElement').mockImplementation((tagName, options) =>
      tagName.toLowerCase() === 'a' ? anchor : originalCreateElement(tagName, options));
    const appendChild = vi.spyOn(document.body, 'appendChild');

    fireEvent.click(downloadButton);

    expect(createElement).toHaveBeenCalledWith('a');
    expect(anchor.getAttribute('download')).toBe('blueprint.json');
    expect(anchor.getAttribute('href')).toContain('data:text/json;charset=utf-8,');
    expect(appendChild).toHaveBeenCalledWith(anchor);
    expect(click).toHaveBeenCalledOnce();
    expect(remove).toHaveBeenCalledOnce();
  });
});
