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

module.exports = {
  mergeBlueprints: function(masterBlueprint, slaveBlueprint) {
    var self = this;

    // Check edge cases
    if (!slaveBlueprint && !masterBlueprint) {
      throw 'slaveBlueprint or masterBlueprint should not be empty';
    } else if (slaveBlueprint && !masterBlueprint) {
      return slaveBlueprint;
    } else if (!slaveBlueprint && masterBlueprint) {
      return masterBlueprint;
    }

    // Work with main case (both blueprint are presented)
    var matches = self.matchGroups(masterBlueprint, slaveBlueprint);

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    var i = 0;
    matches.forEach(function(match){
      i += 1;
      var group_name = 'host-group-' + i;

      var masterComponents = self.getComponentsFromBlueprintByGroupName(masterBlueprint, match.g1);
      var slaveComponents = self.getComponentsFromBlueprintByGroupName(slaveBlueprint, match.g2);

      res.blueprint.host_groups.push({
        name: group_name,
        components: masterComponents.concat(slaveComponents)
      });

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: self.getHostsFromBlueprintByGroupName(match.g1 ? masterBlueprint : slaveBlueprint, match.g1 ? match.g1 : match.g2)
      });
    });
    return res;
  },

  getHostsFromBlueprint: function(blueprint) {
    return blueprint.blueprint_cluster_binding.host_groups.mapProperty("hosts").reduce(function(prev, curr){ return prev.concat(curr); }, []).mapProperty("fqdn");
  },

  getHostsFromBlueprintByGroupName: function(blueprint, groupName) {
    if (groupName) {
      var group = blueprint.blueprint_cluster_binding.host_groups.find(function(g) {
        return g.name === groupName;
      });

      if (group) {
        return group.hosts;
      }
    }
    return [];
  },

  getComponentsFromBlueprintByGroupName: function(blueprint, groupName) {
    if (groupName) {
      var group = blueprint.blueprint.host_groups.find(function(g) {
        return g.name === groupName;
      });

      if (group) {
        return group.components;
      }
    }
    return [];
  },

  matchGroups: function(masterBlueprint, slaveBlueprint) {
    var self = this;
    var res = [];

    var groups1 = masterBlueprint.blueprint_cluster_binding.host_groups;
    var groups2 = slaveBlueprint.blueprint_cluster_binding.host_groups;

    var groups1_used = groups1.map(function() { return false; });
    var groups2_used = groups2.map(function() { return false; });

    self.matchGroupsWithLeft(groups1, groups2, groups1_used, groups2_used, res, false);
    self.matchGroupsWithLeft(groups2, groups1, groups2_used, groups1_used, res, true);

    return res;
  },

  matchGroupsWithLeft: function(groups1, groups2, groups1_used, groups2_used, res, inverse) {
    for (var i = 0; i < groups1.length; i++) {
      if (groups1_used[i]) {
        continue;
      }

      var group1 = groups1[i];
      groups1_used[i] = true;

      var group2 = groups2.find(function(g2, index) {
        if (group1.hosts.length != g2.hosts.length) {
          return false;
        }

        for (var gi = 0; gi < group1.hosts.length; gi++) {
          if (group1.hosts[gi].fqdn != g2.hosts[gi].fqdn) {
            return false;
          }
        }

        groups2_used[index] = true;
        return true;
      });

      var item = {};

      if (inverse) {
        item.g2 = group1.name;
        if (group2) {
          item.g1 = group2.name;
        }
      } else {
        item.g1 = group1.name;
        if (group2) {
          item.g2 = group2.name;
        }
      }
      res.push(item);
    }
  },

  /**
   * Remove from blueprint all components expect given components
   * @param blueprint
   * @param [string] components
   */
  filterByComponents: function(blueprint, components) {
    var res = JSON.parse(JSON.stringify(blueprint))
    var emptyGroups = [];

    for (var i = 0; i < res.blueprint.host_groups.length; i++) {
      res.blueprint.host_groups[i].components = res.blueprint.host_groups[i].components.filter(function(c) {
        return components.contains(c.name);
      });

      if (res.blueprint.host_groups[i].components.length == 0) {
        emptyGroups.push(res.blueprint.host_groups[i].name);
      }
    }

    res.blueprint.host_groups = res.blueprint.host_groups.filter(function(g) {
      return !emptyGroups.contains(g.name);
    });

    res.blueprint_cluster_binding.host_groups = res.blueprint_cluster_binding.host_groups.filter(function(g) {
      return !emptyGroups.contains(g.name);
    });

    return res;
  },

  addComponentsToBlueprint: function(blueprint, components) {
    var res = JSON.parse(JSON.stringify(blueprint))

    res.blueprint.host_groups.forEach(function(group) {
      components.forEach(function(component) {
        group.components.push({ name: component });
      });
    });

    return res;
  }
};
