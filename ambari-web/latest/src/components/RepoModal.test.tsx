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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import VersionsApi from "../api/versionsApi";
import RepoModal from "./RepoModal";

vi.mock("../api/versionsApi", () => ({
  default: {
    saveRepoVersions: vi.fn(),
    validateRepos: vi.fn(),
  },
}));
vi.mock("react-hot-toast", () => ({
  default: { error: vi.fn(), success: vi.fn() },
}));
vi.mock("./Tooltip", () => ({
  default: ({ children }: { children: ReactNode }) => children,
}));
type TestModalButton = {
  text: string;
  disabled?: boolean;
  onClick: () => void;
};

type TestModalProps = {
  isOpen: boolean;
  modalBody: ReactNode;
  successCallback: () => void;
  options: {
    extraButtons?: TestModalButton[];
    okButtonDisabled?: boolean;
    okButtonText?: string;
  };
};

vi.mock("./Modal", () => ({
  default: ({ isOpen, modalBody, successCallback, options }: TestModalProps) => isOpen ? (
    <div>
      {modalBody}
      {(options.extraButtons || []).map((button) => (
        <button key={button.text} disabled={button.disabled} onClick={button.onClick}>
          {button.text}
        </button>
      ))}
      <button disabled={options.okButtonDisabled} onClick={successCallback}>
        {options.okButtonText}
      </button>
    </div>
  ) : null,
}));

const selectedStack = {
  ClusterStackVersions: { stack: "HDP", version: "3.1" },
  repository_versions: [{
    RepositoryVersions: { id: 7 },
    operating_systems: [
      {
        OperatingSystems: { os_type: "redhat8", ambari_managed_repositories: true },
        repositories: [{ Repositories: { repo_id: "HDP", repo_name: "HDP", base_url: "https://r8/original" } }],
      },
      {
        OperatingSystems: { os_type: "ubuntu22", ambari_managed_repositories: true },
        repositories: [{ Repositories: { repo_id: "HDP", repo_name: "HDP", base_url: "https://u22/original" } }],
      },
    ],
  }],
};

describe("RepoModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(VersionsApi.saveRepoVersions).mockResolvedValue({} as never);
  });

  it("keeps duplicate repository IDs separate by OS and saves after explicit validation override", async () => {
    vi.mocked(VersionsApi.validateRepos)
      .mockRejectedValueOnce(new Error("redhat repository rejected"))
      .mockResolvedValueOnce({} as never);
    const onClose = vi.fn();
    render(<RepoModal isOpen onClose={onClose} selectedStack={selectedStack} />);

    const inputs = screen.getAllByRole("textbox");
    fireEvent.change(inputs[0], { target: { value: "https://r8/updated" } });
    fireEvent.change(inputs[1], { target: { value: "https://u22/updated" } });
    fireEvent.click(screen.getByRole("button", { name: "SAVE" }));

    expect(await screen.findByText("redhat repository rejected")).toBeTruthy();
    expect(VersionsApi.validateRepos).toHaveBeenNthCalledWith(
      1,
      "HDP",
      "3.1",
      "redhat8",
      "HDP",
      { base_url: "https://r8/updated", repo_name: "HDP" },
    );
    expect(VersionsApi.validateRepos).toHaveBeenNthCalledWith(
      2,
      "HDP",
      "3.1",
      "ubuntu22",
      "HDP",
      { base_url: "https://u22/updated", repo_name: "HDP" },
    );

    fireEvent.click(screen.getByRole("button", { name: "SAVE ANYWAY" }));
    await waitFor(() => expect(VersionsApi.saveRepoVersions).toHaveBeenCalledWith(
      "HDP",
      "3.1",
      "7",
      {
        operating_systems: [
          {
            OperatingSystems: { os_type: "redhat8", ambari_managed_repositories: true },
            repositories: [{ Repositories: { base_url: "https://r8/updated", repo_id: "HDP", repo_name: "HDP" } }],
          },
          {
            OperatingSystems: { os_type: "ubuntu22", ambari_managed_repositories: true },
            repositories: [{ Repositories: { base_url: "https://u22/updated", repo_id: "HDP", repo_name: "HDP" } }],
          },
        ],
      },
    ));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
