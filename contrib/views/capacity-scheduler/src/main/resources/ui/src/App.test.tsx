/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import fixture from "./testData/legacy-scheduler-configuration.json";
import type { CapacityApi } from "./api";
import App from "./App";
import type { ConfigurationPayload } from "./types";

const makeApi = (operator = true): CapacityApi => ({
  root: "/api/test",
  cluster: vi.fn().mockResolvedValue({ Clusters: { cluster_name: "MyCluster", version: "HDP-2.6" } }),
  privilege: vi.fn().mockResolvedValue(operator),
  latest: vi.fn().mockResolvedValue(fixture as ConfigurationPayload),
  byTag: vi.fn().mockResolvedValue(fixture as ConfigurationPayload),
  versions: vi.fn().mockResolvedValue([{ tag: "version1400218672484", version: 7 }]),
  configValue: vi.fn().mockImplementation((_site: string, config: string) => Promise.resolve(config === "yarn.resourcemanager.scheduler.monitor.enable" ? "true" : "false")),
  nodeLabels: vi.fn().mockResolvedValue(["gpu"]),
  rmQueues: vi.fn().mockResolvedValue([
    { path: "root", state: "RUNNING" },
    { path: "root.engineering", state: "RUNNING" },
  ]),
  save: vi.fn().mockResolvedValue({}),
  refresh: vi.fn().mockResolvedValue(fixture as ConfigurationPayload),
  restart: vi.fn().mockResolvedValue(fixture as ConfigurationPayload),
});

describe("Capacity Scheduler application", () => {
  it("loads queue, scheduler, mappings, advanced, and version surfaces", async () => {
    const user = userEvent.setup();
    render(<App api={makeApi()} />);
    expect(await screen.findByRole("heading", { name: "root" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Scheduler" }));
    expect(screen.getByLabelText("Maximum applications")).toHaveValue(10000);
    await user.click(screen.getByRole("button", { name: "Queue mappings" }));
    expect(screen.getByPlaceholderText("u:%user:%primary_group")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Advanced" }));
    expect(screen.getByText("Generated capacity-scheduler.xml")).toBeInTheDocument();
    expect(screen.getByText("v7")).toBeInTheDocument();
  });

  it("adds a child queue and marks the configuration dirty", async () => {
    const user = userEvent.setup();
    render(<App api={makeApi()} />);
    await screen.findByRole("heading", { name: "root" });
    await user.click(screen.getAllByTitle("Add child queue")[0]);
    await user.type(screen.getByLabelText("Queue name"), "NewQueue");
    await user.click(screen.getByRole("button", { name: "Add queue" }));
    expect(screen.getAllByText("NewQueue").length).toBeGreaterThan(0);
    expect(screen.getByText(/pending property change/)).toBeInTheDocument();
  });

  it("enforces read-only mode for users without operator privileges", async () => {
    render(<App api={makeApi(false)} />);
    expect(await screen.findByText(/only Ambari or cluster administrators and operators can edit/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Save and refresh/ })).toBeDisabled();
  });
});
