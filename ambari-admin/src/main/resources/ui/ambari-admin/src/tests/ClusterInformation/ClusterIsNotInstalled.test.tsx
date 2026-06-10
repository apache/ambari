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
import App from "../../../src/App";
import AppContent from "../../../src/context/AppContext";
import "@testing-library/jest-dom/vitest";
import userEvent from '@testing-library/user-event';
import { createMemoryHistory, MemoryHistory } from "history";
import ClusterApi from "../../../src/api/clusterApi";
import mockClusterInfo from "../../__mocks__/mockClusterInfo";
import mockHostClusterInfo from "../../__mocks__/mockHostClusterInfo";
import ClusterInformation from "../../screens/ClusterManagement/ClusterInformation";
describe('Cluster is not installed', () => {
    //The mockData consists of variables and values only with initial state
    //once the apis update the necessary variables the state changes accordingly
    const mockData = {
        clusterInfoLoading: false,
        isInstallWizardLaunched: false,
        clusterExists: false,
        selectedOption: '',
        cluster: {},
        setClusterInfo: () => {},
        rbacData: {},
        setRbacData: () => {},
        permissionLabelList: [],
        setPermissionLabelList: () => {},
        setSelectedOption: () => "Versions",
    };
    const history: MemoryHistory = createMemoryHistory();
    // let originalLocationHref: string;
    // let originalLocationHash: string;

    // beforeAll(() => {
    //     // Save the original location
    //     originalLocationHref = global.window.location.href;
    //     originalLocationHash = global.window.location.hash;
    //     Object.defineProperty(window, 'matchMedia', {
    //         writable: true,
    //         value: (query) => ({
    //             matches: false,
    //             media: query,
    //             onchange: null,
    //             addListener: () => {}, // Deprecated
    //             removeListener: () => {}, // Deprecated
    //             addEventListener: () => {},
    //             removeEventListener: () => {},
    //             dispatchEvent: () => {},
    //         }),
    //     });
    //  });
    //
    // afterAll(() => {
    //     // Restore the original location properties
    //     global.window.location.href = originalLocationHref;
    //     global.window.location.hash = originalLocationHash;
    // });
    
    beforeAll(() => {
        window.matchMedia = window.matchMedia || function() {
            return {
                matches: false,
                addListener: function() {},
                removeListener: function() {}
            };
        };
    });
    beforeEach(() => {
        // history = createMemoryHistory();
        //Mock window object
        const { location } = window;
        //delete global.window.location;
        global.window.location = { ...location,
            //@ts-ignore
            replace: () => {},
            hash: '/clusterInformation' // add this line to set the hash
        };
        const url = "http://localhost";
        global.window.location.href = url;
        ClusterApi.clusterInfo = async () => mockClusterInfo;
        ClusterApi.hostClustersInfo = async () => mockHostClusterInfo;
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

    const texts = [
        'Welcome to Apache Ambari',
        'Provision a cluster, manage who can access the cluster, and customize views for Ambari users.',
        'Create a Cluster',
        'Use the Install Wizard to select services and configure your cluster'
    ];

    it.each(texts)('displays the text "%s"', async (text) => {
        render(
            <Router history={history}>
                <AppContent.Provider value={mockData}>
                    <App />
                </AppContent.Provider>
            </Router>
        );
        await waitFor(async () => {
            const textElement = await screen.findByText(new RegExp(text, 'i'));
            console.log("text element is ", textElement.textContent);
            expect(textElement).toBeInTheDocument();
        });
    });

    it('renders the launch install wizard button and navigates to the installer route on click', async () => {
        render(
            <Router history={history}>
                <AppContent.Provider value={mockData}>
                    <App />
                </AppContent.Provider>
            </Router>
        );
        await waitFor(async () => {
            const launchInstallWizardButton = await screen.findByRole('button', {name: /launch install wizard/i});
            expect(launchInstallWizardButton).toBeInTheDocument();

            await userEvent.click(launchInstallWizardButton);

            // Wait for the URL to change
            expect(window.location.href).toBe('/#/installer/step0');
        });
    });

    it('renders installBox image when conditions are met', async () => {
        render(
            <Router history={history}>
                <AppContent.Provider value={mockData}>
                    <App />
                </AppContent.Provider>
            </Router>
        );
        await waitFor(() => {
            const installBoxImgElement = screen.getByAltText('Install Box') as HTMLImageElement;
            expect(installBoxImgElement).toBeInTheDocument();
            expect(installBoxImgElement.src).toContain("install-box.svg");
        })
    });
});