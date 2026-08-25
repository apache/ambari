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
import { getSideItemList, SideItemLabels } from "./SideItemList";

const havePermissions = (authorization: string) =>
  authorization.split(",").map((value) => value.trim())
    .includes("CLUSTER.TOGGLE_KERBEROS");

const hasKerberosItem = (isNonWizardUser: boolean, stackName = "HDP") => {
  const isAuthorized = (authorization: string) => (
    !isNonWizardUser && havePermissions(authorization)
  );
  const items = getSideItemList(
    havePermissions,
    isAuthorized,
    { enableToggleKerberos: true },
    stackName,
  );
  return Boolean(items
    .find((item) => item.id === SideItemLabels.CLUSTER_ADMIN)
    ?.children.some((item) => item.id === SideItemLabels.KERBEROS));
};

describe("Kerberos sidebar ownership", () => {
  it("shows Kerberos to the wizard owner", () => {
    expect(hasKerberosItem(false)).toBe(true);
  });

  it("hides Kerberos while another user owns a wizard", () => {
    expect(hasKerberosItem(true)).toBe(false);
  });

  it("hides Kerberos for the Classic HDPWIN stack", () => {
    expect(hasKerberosItem(false, "HDPWIN")).toBe(false);
  });

  it("keeps read-only Stack and Versions visible when mutations are blocked", () => {
    const items = getSideItemList(
      (authorization) => authorization.includes("CLUSTER.VIEW_STACK_DETAILS"),
      () => false,
      { enableToggleKerberos: true },
    );

    expect(items
      .find((item) => item.id === SideItemLabels.CLUSTER_ADMIN)
      ?.children.some((item) => item.id === SideItemLabels.STACK_AND_VERSIONS))
      .toBe(true);
  });
});
