/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import fixture from "./testData/legacy-scheduler-configuration.json";
import {
  addQueue,
  deleteQueue,
  parseCapacityModel,
  propertiesToXml,
  propertyDiff,
  renameQueue,
  serializeCapacityModel,
  validateCapacityModel,
} from "./capacityModel";
import type { ConfigurationPayload } from "./types";

const payload = fixture as ConfigurationPayload;

describe("capacity scheduler model", () => {
  it("parses the legacy fixture into a complete queue hierarchy", () => {
    const model = parseCapacityModel(payload);
    expect(model.tag).toBe("version1400218672484");
    expect(model.clusterName).toBe("MyCluster");
    expect(model.scheduler.maximumAmResourcePercent).toBe(35);
    expect(model.queues.map((queue) => queue.path)).toContain("root.Engineering.Development");
    expect(model.queues.find((queue) => queue.path === "root.Marketing.Advertising")?.state).toBe("STOPPED");
  });

  it("round trips managed values while preserving custom properties", () => {
    const model = parseCapacityModel(payload);
    const properties = serializeCapacityModel(model);
    expect(properties["yarn.scheduler.capacity.maximum-am-resource-percent"]).toBe("0.35");
    expect(properties["yarn.scheduler.capacity.root.Engineering.Development.capacity"]).toBe("20");
    expect(properties["yarn.scheduler.capacity.root.unfunded.capacity"]).toBe("50");
  });

  it("preserves enabled node-label access with an empty label set", () => {
    const model = parseCapacityModel(payload, ["gpu"]);
    model.queues = model.queues.map((queue) => queue.path === "root.Engineering"
      ? { ...queue, labelsEnabled: true, accessAllLabels: false, accessibleLabels: [] }
      : queue);
    expect(serializeCapacityModel(model)["yarn.scheduler.capacity.root.Engineering.accessible-node-labels"]).toBe("");
  });

  it("moves all descendant and custom properties when a queue is renamed", () => {
    const model = renameQueue(parseCapacityModel(payload), "root.Engineering", "Product");
    const properties = serializeCapacityModel(model);
    expect(model.queues.map((queue) => queue.path)).toContain("root.Product.Development");
    expect(properties["yarn.scheduler.capacity.root.Product.Development.capacity"]).toBe("20");
    expect(properties).not.toHaveProperty("yarn.scheduler.capacity.root.Engineering.Development.capacity");
  });

  it("removes every property owned by a deleted subtree", () => {
    const model = deleteQueue(parseCapacityModel(payload), "root.Marketing");
    const properties = serializeCapacityModel(model);
    expect(Object.keys(properties).some((key) => key.startsWith("yarn.scheduler.capacity.root.Marketing."))).toBe(false);
    expect(properties["yarn.scheduler.capacity.root.queues"]).not.toContain("Marketing");
  });

  it("adds a queue and reports invalid sibling capacity totals", () => {
    const model = addQueue(parseCapacityModel(payload), "root.Engineering", "Docs");
    expect(model.queues.find((queue) => queue.path === "root.Engineering.Docs")?.capacity).toBe(0);
    expect(validateCapacityModel(model).filter((issue) => issue.field === "childrenCapacity")).toHaveLength(0);
    const changed = {
      ...model,
      queues: model.queues.map((queue) => queue.path === "root.Engineering.Docs" ? { ...queue, capacity: 10 } : queue),
    };
    expect(validateCapacityModel(changed).some((issue) => issue.path === "root.Engineering" && issue.field === "childrenCapacity")).toBe(true);
  });

  it("validates queue mappings against leaf queues", () => {
    const model = parseCapacityModel(payload);
    model.scheduler.queueMappings = "u:alice:Development,g:analysts:Missing";
    expect(validateCapacityModel(model).filter((issue) => issue.field === "queueMappings")).toHaveLength(1);
    model.scheduler.queueMappings = "u:%user:%primary_group";
    expect(validateCapacityModel(model).filter((issue) => issue.field === "queueMappings")).toHaveLength(0);
  });

  it("creates escaped XML and a property-level diff", () => {
    expect(propertiesToXml({ "a&b": "<value>" })).toContain("<name>a&amp;b</name>");
    expect(propertiesToXml({ "a&b": "<value>" })).toContain("<value>&lt;value&gt;</value>");
    expect(propertyDiff({ a: "1", b: "2" }, { a: "3", c: "4" })).toEqual([
      { key: "a", before: "1", after: "3" },
      { key: "b", before: "2", after: undefined },
      { key: "c", before: undefined, after: "4" },
    ]);
  });
});
