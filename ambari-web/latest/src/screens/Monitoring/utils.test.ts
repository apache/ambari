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

import { describe, expect, it } from "vitest";
import { ChartShare, Dashboard, DashboardPanel, Datasource } from "./types";
import {
  dashboardAppearsAt,
  escapePrometheusLabelValue,
  normalizeDashboardPayload,
  normalizePrometheusResults,
  panelFromShare,
  replaceDashboardVariables,
  resolvePanelDatasourceId,
  withDashboardBuiltIns,
} from "./utils";

const datasource = (id: number, overrides: Partial<Datasource> = {}): Datasource => ({
  id,
  name: `source-${id}`,
  description: "",
  category: "prometheus",
  plugin_id: 0,
  plugin_type: "prometheus",
  plugin_type_name: "Prometheus",
  cluster_name: "west",
  settings: {},
  http: {},
  auth_configured: false,
  status: "enabled",
  is_default: false,
  created_at: 0,
  created_by: "",
  updated_at: 0,
  updated_by: "",
  ...overrides,
});

describe("monitoring dashboard compatibility helpers", () => {
  it("replaces braced and bare variables without replacing longer names", () => {
    expect(replaceDashboardVariables(
      'up{service="$service",instance=~"${hosts}",other="$service_name"}',
      { service: "HDFS", hosts: ["nn1", "nn2"] },
    )).toBe('up{service="HDFS",instance=~"nn1|nn2",other="$service_name"}');
  });

  it("overrides payload values with cluster-scoped dashboard built-ins", () => {
    const west = withDashboardBuiltIns({
      cluster: "payload-cluster",
      __rate_interval: "1s",
      service: "HDFS",
    }, "west", 30);
    const east = withDashboardBuiltIns(west, "east", 5);

    expect(replaceDashboardVariables(
      'rate(requests_total{cluster="${cluster}",service="$service"}[$__rate_interval])',
      west,
    )).toBe('rate(requests_total{cluster="west",service="HDFS"}[120s])');
    expect(east.cluster).toBe("east");
    expect(east.__rate_interval).toBe("120s");
  });

  it("escapes cluster names as PromQL label string content", () => {
    const clusterName = 'east\\zone"one\nline\rreturn';
    const variables = withDashboardBuiltIns({}, clusterName, 15);

    expect(escapePrometheusLabelValue(clusterName)).toBe(
      'east\\\\zone\\"one\\nline\\rreturn',
    );
    expect(replaceDashboardVariables(
      'up{cluster="${cluster}"}',
      variables,
    )).toBe('up{cluster="east\\\\zone\\"one\\nline\\rreturn"}');
  });

  it("falls back from a historical datasource ID to the enabled default in the same category", () => {
    const panel: DashboardPanel = { datasourceCate: "prometheus", datasourceValue: 1 };
    expect(resolvePanelDatasourceId(panel, {}, [
      datasource(8),
      datasource(9, { is_default: true }),
      datasource(10, { category: "loki", plugin_type: "loki", is_default: true }),
    ])).toBe(9);
  });

  it("matches service display locations case-insensitively", () => {
    const dashboard = { display_locations: "HDFS, YARN" } as Dashboard;
    expect(dashboardAppearsAt(dashboard, "hdfs")).toBe(true);
    expect(dashboardAppearsAt(dashboard, "HBASE")).toBe(false);
  });

  it("preserves unknown shared panel fields", () => {
    const share: ChartShare = {
      id: 4,
      cluster: "west",
      datasource_id: 9,
      configs: JSON.stringify({
        future_share_option: true,
        dataProps: {
          type: "timeseries",
          datasourceValue: 1,
          customPluginState: { mode: "future" },
        },
      }),
      create_at: 0,
      create_by: "operator",
    };

    const panel = panelFromShare(share);

    expect(panel?.datasourceValue).toBe(9);
    expect(panel?.customPluginState).toEqual({ mode: "future" });
  });

  it("normalizes malformed dashboard collections without dropping unknown fields", () => {
    const payload = normalizeDashboardPayload({
      future_option: { mode: "future" },
      var: { name: "invalid" },
      panels: [
        null,
        "invalid",
        { name: "Valid panel", targets: { expr: "up" }, future_panel_option: true },
      ],
    });

    expect(payload.var).toEqual([]);
    expect(payload.panels).toEqual([{
      name: "Valid panel",
      targets: [],
      future_panel_option: true,
    }]);
    expect(payload.future_option).toEqual({ mode: "future" });
    expect(normalizeDashboardPayload(null)).toEqual({ var: [], panels: [] });
  });

  it("normalizes malformed Prometheus result collections", () => {
    expect(normalizePrometheusResults([
      null,
      { metric: null, value: [1, "ignored"] },
      {
        metric: { __name__: "up", invalid: 1 },
        value: [1, "1"],
        values: [[1, "1"], [2, 0]],
      },
    ])).toEqual([{
      metric: { __name__: "up" },
      value: [1, "1"],
      values: [[1, "1"]],
    }]);
  });
});
