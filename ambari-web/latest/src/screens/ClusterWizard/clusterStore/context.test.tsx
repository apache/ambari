/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { StrictMode, useContext } from "react";
import { MemoryRouter, useLocation, useNavigate } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ActionTypes } from "./types";

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
  jumpToStep: vi.fn(),
}));

vi.mock("../../../api/workflowStateApi", () => ({
  default: { get: mocks.get, put: mocks.put },
}));
vi.mock("../../../Utils/adminViewRedirect", () => ({
  redirectToAdminView: vi.fn(),
}));

import {
  ClusterCreationContext,
  ClusterCreationProvider,
} from "./context";

const DRAFT = "9d8e1f19-8fa5-4472-bb2a-c774b561f462";
const DRAFT_B = "c2d3dcf7-f9a4-46cc-9450-0e99b853650f";
const wizardSteps = {
  0: { name: "NAME" },
  2: { name: "HOSTS" },
  7: { name: "CONFIGURATION" },
  8: { name: "REVIEW" },
};

let currentContext: any;
function Probe() {
  currentContext = useContext(ClusterCreationContext);
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <div>
      <div>editable wizard</div>
      <div>{location.pathname}</div>
      <button onClick={() => navigate(`/installer/step1?draft=${DRAFT}`)}>Next URL</button>
      <button onClick={() => navigate(`/installer/step1?draft=${DRAFT_B}`)}>Open draft B</button>
    </div>
  );
}

const provider = (initialEntry: string) => (
  <MemoryRouter initialEntries={[initialEntry]}>
    <ClusterCreationProvider stepWizardUtilities={{
      currentStep: { name: "NAME" },
      jumpToStep: mocks.jumpToStep,
      wizardSteps,
    }}>
      <Probe />
    </ClusterCreationProvider>
  </MemoryRouter>
);
const renderProvider = (
  initialEntry = `/installer/step0?draft=${DRAFT}`,
  strict = false,
) => render(strict ? <StrictMode>{provider(initialEntry)}</StrictMode> : provider(initialEntry));

describe("ClusterCreationProvider scoped recovery", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    currentContext = undefined;
  });
  afterEach(() => cleanup());

  it("offers a fresh draft instead of retrying a malformed draft link", async () => {
    mocks.get.mockResolvedValue({
      revision: 0,
      workflow: "IDLE",
      phase: "IDLE",
      values: {},
    });
    mocks.put.mockResolvedValue({
      revision: 1,
      workflow: "CLUSTER_CREATE",
      phase: "START",
      values: {},
    });
    renderProvider("/installer/step0?draft=not-a-draft");

    expect(await screen.findByText(/draft ID in this link is invalid/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Start New" }));

    await waitFor(() => expect(mocks.get).toHaveBeenCalledWith({
      type: "drafts",
      id: expect.stringMatching(/^[0-9a-f-]{36}$/i),
    }));
  });

  it("does not render editable state until load and claim finish", async () => {
    let resolveLoad!: (value: any) => void;
    let resolveClaim!: (value: any) => void;
    mocks.get.mockReturnValue(new Promise((resolve) => { resolveLoad = resolve; }));
    mocks.put.mockReturnValue(new Promise((resolve) => { resolveClaim = resolve; }));

    renderProvider();
    expect(screen.queryByText("editable wizard")).toBeNull();

    await waitFor(() => expect(mocks.get).toHaveBeenCalledTimes(1));
    await act(async () => resolveLoad({
      revision: 0,
      owner: null,
      workflow: "IDLE",
      phase: "IDLE",
      values: {},
    }));
    expect(screen.queryByText("editable wizard")).toBeNull();

    await waitFor(() => expect(mocks.put).toHaveBeenCalledTimes(1));
    await act(async () => resolveClaim({
      revision: 1,
      owner: "alice",
      workflow: "CLUSTER_CREATE",
      phase: "START",
      values: {},
    }));
    expect(await screen.findByText("editable wizard")).toBeTruthy();
  });

  it("ignores a late recovery response after unmount", async () => {
    let resolveLoad!: (value: any) => void;
    mocks.get.mockReturnValue(new Promise((resolve) => { resolveLoad = resolve; }));
    const view = renderProvider();

    await waitFor(() => expect(mocks.get).toHaveBeenCalledTimes(1));
    view.unmount();
    await act(async () => resolveLoad({
      revision: 0,
      workflow: "IDLE",
      phase: "IDLE",
      values: {},
    }));

    expect(mocks.put).not.toHaveBeenCalled();
    expect(mocks.jumpToStep).not.toHaveBeenCalled();
  });

  it("survives the production Strict Mode effect cycle", async () => {
    mocks.get.mockResolvedValue({
      revision: 2,
      workflow: "CLUSTER_CREATE",
      phase: "NAME",
      values: { state: { clusterCreationSteps: {} }, step: { stepName: "NAME" } },
    });

    renderProvider(`/installer/step0?draft=${DRAFT}`, true);

    expect(await screen.findByText("editable wizard")).toBeTruthy();
  });

  it("does not reload the draft when only the installer step URL changes", async () => {
    mocks.get.mockResolvedValue({
      revision: 2,
      workflow: "CLUSTER_CREATE",
      phase: "NAME",
      values: { state: { clusterCreationSteps: {} }, step: { stepName: "NAME" } },
    });
    renderProvider();
    await screen.findByText("editable wizard");

    fireEvent.click(screen.getByRole("button", { name: "Next URL" }));
    expect(await screen.findByText("/installer/step1")).toBeTruthy();

    expect(mocks.get).toHaveBeenCalledTimes(1);
  });

  it("stores selector data once and returns the exact post-CAS draft revision", async () => {
    mocks.get.mockResolvedValue({
      revision: 4,
      workflow: "CLUSTER_CREATE",
      phase: "SERVICES",
      values: { state: { clusterCreationSteps: {} }, step: { stepName: "SERVICES" } },
    });
    mocks.put.mockResolvedValue({
      revision: 5,
      workflow: "CLUSTER_CREATE",
      phase: "SERVICES",
      values: {},
    });
    renderProvider();
    await screen.findByText("editable wizard");

    let revision = 0;
    await act(async () => {
      revision = await currentContext.storeStepDataAndFlush("SERVICES", {
        managedDependencies: { HDFS: { mode: "managed" } },
      });
    });

    expect(revision).toBe(5);
    expect(mocks.put).toHaveBeenCalledTimes(1);
    expect(mocks.put).toHaveBeenCalledWith({ type: "drafts", id: DRAFT }, expect.objectContaining({
      expected_revision: 4,
      workflow: "CLUSTER_CREATE",
      values: expect.objectContaining({ state: expect.objectContaining({
        clusterCreationSteps: expect.objectContaining({ SERVICES: expect.any(Object) }),
      }) }),
    }));
  });

  it("holds later master autosaves behind the advisor request using its saved revision", async () => {
    mocks.get.mockResolvedValue({
      revision: 4,
      workflow: "CLUSTER_CREATE",
      phase: "MASTERS",
      values: { state: { clusterCreationSteps: {} }, step: { stepName: "MASTERS" } },
    });
    mocks.put
      .mockResolvedValueOnce({ revision: 5, workflow: "CLUSTER_CREATE", phase: "MASTERS", values: {} })
      .mockResolvedValueOnce({ revision: 6, workflow: "CLUSTER_CREATE", phase: "MASTERS", values: {} });
    renderProvider();
    await screen.findByText("editable wizard");

    let resolveAdvice!: (value: string) => void;
    const advice = new Promise<string>((resolve) => { resolveAdvice = resolve; });
    let guardedAdvice!: Promise<string>;
    act(() => {
      currentContext.dispatch({
        type: ActionTypes.STORE_INFORMATION,
        payload: { step: "MASTERS", data: { selectedHost: "host-a" } },
      });
      guardedAdvice = currentContext.withStateCheckpoint(
        async (revision: number) => {
          expect(revision).toBe(5);
          return advice;
        },
      );
    });
    await waitFor(() => expect(mocks.put).toHaveBeenCalledTimes(1));
    expect(mocks.put.mock.calls[0][1]).toEqual(expect.objectContaining({
      expected_revision: 4,
      values: expect.objectContaining({
        state: expect.objectContaining({
          clusterCreationSteps: expect.objectContaining({
            MASTERS: expect.objectContaining({
              data: { selectedHost: "host-a" },
            }),
          }),
        }),
      }),
    }));

    act(() => currentContext.dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: { step: "MASTERS", data: { selectedHost: "host-b" } },
    }));
    await Promise.resolve();
    expect(mocks.put).toHaveBeenCalledTimes(1);

    await act(async () => resolveAdvice("validated"));
    await expect(guardedAdvice).resolves.toBe("validated");
    await waitFor(() => expect(mocks.put).toHaveBeenCalledTimes(2));
    expect(mocks.put.mock.calls[1][1]).toEqual(expect.objectContaining({
      expected_revision: 5,
      values: expect.objectContaining({
        state: expect.objectContaining({
          clusterCreationSteps: expect.objectContaining({
            MASTERS: expect.objectContaining({
              data: { selectedHost: "host-b" },
            }),
          }),
        }),
      }),
    }));
  });

  it("does not run advisor work after a same-draft checkpoint conflict", async () => {
    mocks.get.mockResolvedValue({
      revision: 4,
      workflow: "CLUSTER_CREATE",
      phase: "MASTERS",
      values: { state: { clusterCreationSteps: {} }, step: { stepName: "MASTERS" } },
    });
    mocks.put.mockRejectedValueOnce({
      response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } },
    });
    renderProvider();
    await screen.findByText("editable wizard");
    const advisorRequest = vi.fn().mockResolvedValue("unexpected");

    await expect(currentContext.withStateCheckpoint(advisorRequest)).rejects
      .toEqual(expect.objectContaining({
        response: expect.objectContaining({
          data: { code: "WORKFLOW_VERSION_CONFLICT" },
        }),
      }));
    expect(advisorRequest).not.toHaveBeenCalled();
    expect(await screen.findByText(/changed in another tab/)).toBeTruthy();

    await expect(currentContext.withStateCheckpoint(advisorRequest)).rejects
      .toThrow(/invalidated/i);
    expect(mocks.put).toHaveBeenCalledOnce();
    expect(advisorRequest).not.toHaveBeenCalled();
  });

  it("hides draft A while its pending save drains before draft B loads", async () => {
    mocks.get.mockResolvedValue({
      revision: 2,
      workflow: "CLUSTER_CREATE",
      phase: "NAME",
      values: { state: { clusterCreationSteps: {} }, step: { stepName: "NAME" } },
    });
    let resolveSave!: (value: any) => void;
    mocks.put.mockReturnValue(new Promise((resolve) => { resolveSave = resolve; }));
    renderProvider();
    await screen.findByText("editable wizard");
    const draftAContext = currentContext;

    act(() => {
      void draftAContext.flushStateToDb().catch(() => undefined);
    });
    await waitFor(() => expect(mocks.put).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole("button", { name: "Open draft B" }));

    expect(screen.queryByText("editable wizard")).toBeNull();
    await act(async () => resolveSave({
      revision: 3,
      workflow: "CLUSTER_CREATE",
      phase: "NAME",
      values: {},
    }));
    await waitFor(() => expect(mocks.get).toHaveBeenLastCalledWith({
      type: "drafts",
      id: DRAFT_B,
    }));
    expect(await screen.findByText("editable wizard")).toBeTruthy();
  });

  it("drops queued snapshots after a conflict before retrying at the new revision", async () => {
    mocks.get
      .mockResolvedValueOnce({
        revision: 3,
        workflow: "CLUSTER_CREATE",
        phase: "NAME",
        values: { state: { clusterCreationSteps: {} }, step: { stepName: "NAME" } },
      })
      .mockResolvedValueOnce({
        revision: 7,
        workflow: "CLUSTER_CREATE",
        phase: "NAME",
        values: { state: { clusterCreationSteps: {} }, step: { stepName: "NAME" } },
      });
    mocks.put.mockRejectedValueOnce({
      response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } },
    });
    renderProvider();
    await screen.findByText("editable wizard");

    await act(async () => {
      await currentContext.flushStateToDb().catch(() => undefined);
    });
    expect(await screen.findByText(/changed in another tab/)).toBeTruthy();

    act(() => currentContext.dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: { step: "NAME", data: { clusterName: "stale-name" } },
    }));
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.get).toHaveBeenCalledTimes(2));
    await screen.findByText("editable wizard");

    expect(mocks.put).toHaveBeenCalledTimes(1);
  });

  it("links restored credential markers back to the required steps", async () => {
    mocks.get.mockResolvedValue({
      revision: 2,
      workflow: "CLUSTER_CREATE",
      phase: "REVIEW",
      values: {
        state: {
          clusterCreationSteps: {
            HOSTS: { data: { requires_reentry: true } },
          },
        },
        step: { stepName: "REVIEW" },
      },
    });
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Review Install Options" }));

    expect(mocks.jumpToStep).toHaveBeenLastCalledWith(2);
  });
});
