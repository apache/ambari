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

export type ConfigGroupLike = {
  ConfigGroup: {
    id?: string | number;
    group_name?: string;
    hosts?: Array<{ host_name?: string }>;
  };
};

const getHostNames = (configGroup: ConfigGroupLike) =>
  new Set(
    (configGroup.ConfigGroup.hosts || [])
      .map((host) => host.host_name)
      .filter((hostName): hostName is string => Boolean(hostName))
  );

const hasSameHosts = (first: Set<string>, second: Set<string>) =>
  first.size === second.size && [...first].every((host) => second.has(host));

export const buildConfigGroupUpdatePlan = <T extends ConfigGroupLike>(
  previousGroups: T[],
  updatedGroups: T[]
) => {
  const previousById = new Map(
    previousGroups.map((group) => [group.ConfigGroup.id, group])
  );
  const toClear = updatedGroups.filter((group) => {
    const previous = previousById.get(group.ConfigGroup.id);
    return (
      previous && !hasSameHosts(getHostNames(previous), getHostNames(group))
    );
  });
  const clearSet = new Set(toClear);
  const toSet = updatedGroups.filter(
    (group) => !clearSet.has(group) || getHostNames(group).size > 0
  );

  return { toClear, toSet };
};

export const moveHostsToConfigGroup = <T extends ConfigGroupLike>(
  groups: T[],
  targetGroupName: string,
  hostsToMove: string[]
): T[] => {
  const movedHosts = new Set(hostsToMove);
  return groups.map((group) => {
    const remainingHosts = (group.ConfigGroup.hosts || []).filter(
      (host) => !host.host_name || !movedHosts.has(host.host_name)
    );
    const targetHosts =
      group.ConfigGroup.group_name === targetGroupName
        ? [
            ...remainingHosts,
            ...hostsToMove.map((host_name) => ({ host_name })),
          ]
        : remainingHosts;
    return {
      ...group,
      ConfigGroup: {
        ...group.ConfigGroup,
        hosts: targetHosts,
      },
    };
  });
};

export const removeConfigGroupAndReturnHosts = <T extends ConfigGroupLike>(
  groups: T[],
  groupName: string,
  defaultGroupName = "Default"
): T[] => {
  const removedGroup = groups.find(
    (group) => group.ConfigGroup.group_name === groupName
  );
  const hostNames = [...getHostNames(removedGroup || { ConfigGroup: {} })];
  return moveHostsToConfigGroup(groups, defaultGroupName, hostNames).filter(
    (group) => group.ConfigGroup.group_name !== groupName
  );
};
