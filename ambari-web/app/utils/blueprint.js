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
    console.time('mergeBlueprints');
    var self = this;

    // Check edge cases
    if (!slaveBlueprint && !masterBlueprint) {
      throw 'slaveBlueprint or masterBlueprint should not be empty';
    }
    else
      if (slaveBlueprint && !masterBlueprint) {
        return slaveBlueprint;
      }
      else
        if (!slaveBlueprint && masterBlueprint) {
          return masterBlueprint;
        }

    // Work with main case (both blueprint are presented)
    var matches = self.matchGroups(masterBlueprint, slaveBlueprint);

    var res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] }
    };

    var tmpObj = {hosts: [], components: []};
    var masterBluePrintHostGroupsCluster = this.blueprintToObject(masterBlueprint, 'blueprint_cluster_binding.host_groups');
    var slaveBluePrintHostGroupsCluster = this.blueprintToObject(slaveBlueprint, 'blueprint_cluster_binding.host_groups');
    var masterBluePrintHostGroupsBlueprint = this.blueprintToObject(masterBlueprint, 'blueprint.host_groups');
    var slaveBluePrintHostGroupsBlueprint = this.blueprintToObject(slaveBlueprint, 'blueprint.host_groups');

    matches.forEach(function (match, i) {
      var group_name = 'host-group-' + (i + 1);

      var masterComponents = match.g1 ? Em.getWithDefault(masterBluePrintHostGroupsBlueprint, match.g1, tmpObj).components : [];
      var slaveComponents = match.g2 ? Em.getWithDefault(slaveBluePrintHostGroupsBlueprint, match.g2, tmpObj).components : [];

      res.blueprint.host_groups.push({
        name: group_name,
        components: masterComponents.concat(slaveComponents)
      });

      var hosts = match.g1 ? Em.getWithDefault(masterBluePrintHostGroupsCluster, match.g1, tmpObj).hosts :
        Em.getWithDefault(slaveBluePrintHostGroupsCluster, match.g2, tmpObj).hosts;

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: hosts
      });
    });
    console.timeEnd('mergeBlueprints');
    return res;
  },

  /**
   * Convert <code>blueprint</code>-object to the array with keys equal to the host-groups names
   * Used to improve performance when user try to search value in the blueprint using host-group name as search-field
   * Example:
   *  Before:
   *  <pre>
   *    // blueprint
   *    {
   *      blueprint: {
   *        host_groups: [
   *          {
   *            components: [{}, {}, ...],
   *            name: 'n1'
   *          },
   *          {
   *            components: [{}, {}, ...],
   *            name: 'n2'
   *          }
   *        ]
   *      },
   *      blueprint_cluster_binding: {
   *        host_groups: [
   *          {
   *            hosts: [{}, {}, ...],
   *            name: 'n1'
   *          },
   *          {
   *            hosts: [{}, {}, ...],
   *            name: 'n2'
   *          }
   *        ]
   *      }
   *    }
   *  </pre>
   *  Return:
   *  <pre>
   *    // field = 'blueprint_cluster_binding.host_groups'
   *    {
   *      n1: {
   *        hosts: [{}, {}, ...],
   *        name: 'n1'
   *      },
   *      n2: {
   *        hosts: [{}, {}, ...],
   *        name: 'n2'
   *      }
   *    }
   *
   *    // field = 'blueprint.host_groups'
   *    {
   *      n1: {
   *        components: [{}, {}, ...],
   *        name: 'n1'
   *      },
   *      n2: {
   *        components: [{}, {}, ...],
   *        name: 'n2'
   *      }
   *    }
   *  </pre>
   * @param {object} blueprint
   * @param {string} field
   * @returns {object}
   */
  blueprintToObject: function(blueprint, field) {
    var ret = {};
    var valueToMap = Em.get(blueprint, field);
    if (!Array.isArray(valueToMap)) {
      return ret;
    }
    valueToMap.forEach(function(n) {
      ret[Em.get(n, 'name')] = n;
    });
    return ret;
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
    var gs2 = this.groupsToObject(groups2);
    for (var i = 0; i < groups1.length; i++) {
      if (groups1_used[i]) {
        continue;
      }

      var group1 = groups1[i];
      groups1_used[i] = true;

      var group2 = gs2[group1.hosts.mapProperty('fqdn').join(',')];
      if (group2) {
        groups2_used[group2.index] = true;
      }

      var item = {};

      if (inverse) {
        item.g2 = group1.name;
        if (group2) {
          item.g1 = group2.name;
        }
      }
      else {
        item.g1 = group1.name;
        if (group2) {
          item.g2 = group2.name;
        }
      }
      res.push(item);
    }

    // remove unneeded property
    groups2.forEach(function(group) {
      delete group.index;
    });
  },

  /**
   * Convert array of objects to the one object to improve performance with searching objects in the provided array
   * Example:
   *  Before:
   *  <pre>
   *    // groups
   *    [
   *      {
   *        hosts: [
   *          {fqdn: 'h1'}, {fqdn: 'h2'}
   *        ],
   *        name: 'n1'
   *      },
   *      {
   *        hosts: [
   *          {fqdn: 'h3'}, {fqdn: 'h4'}
   *        ]
   *      }
   *    ]
   *  </pre>
   *  Return:
   *  <pre>
   *    {
   *      'h1,h2': {
   *        hosts: [
   *          {fqdn: 'h1'}, {fqdn: 'h2'}
   *        ],
   *        name: 'n1',
   *        index: 0
   *      },
   *      'h3,h4': {
   *        hosts: [
   *          {fqdn: 'h3'}, {fqdn: 'h4'}
   *        ],
   *        name: 'n2',
   *        index: 1
   *      }
   *    }
   *  </pre>
   * @param {{hosts: object[], name: string}[]} groups
   * @returns {object}
   */
  groupsToObject: function (groups) {
    var ret = {};
    groups.forEach(function (group, index) {
      var key = group.hosts.mapProperty('fqdn').join(',');
      ret[key] = group;
      ret[key].index = index;
    });
    return ret;
  },

  /**
   * Remove from blueprint all components expect given components
   * @param {object} blueprint
   * @param {string[]} components
   */
  filterByComponents: function(blueprint, components) {
    if (!blueprint) {
      return null;
    }

    var res = JSON.parse(JSON.stringify(blueprint));
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
    var res = JSON.parse(JSON.stringify(blueprint));

    res.blueprint.host_groups.forEach(function(group) {
      components.forEach(function(component) {
        group.components.push({ name: component });
      });
    });

    return res;
  },

  /**
   * @method buildConfigsJSON - generates JSON according to blueprint format
   * @param {Em.Array} stepConfigs - array of Ember Objects
   * @param {Array} services
   * @returns {Object}
   * Example:
   * {
   *   "yarn-env": {
   *     "properties": {
   *       "content": "some value",
   *       "yarn_heapsize": "1024",
   *       "resourcemanager_heapsize": "1024",
   *     }
   *   },
   *   "yarn-log4j": {
   *     "properties": {
   *       "content": "some other value"
   *     }
   *   }
   * }
   */
  buildConfigsJSON: function(services, stepConfigs) {
    var configurations = {};
    services.forEach(function(service) {
      var config = stepConfigs.findProperty('serviceName', service.get('serviceName'));
      if (config && service.get('configTypes')) {
        Object.keys(service.get('configTypes')).forEach(function(type) {
          if(!configurations[type]){
            configurations[type] = {
              properties: {}
            }
          }
        });
        config.get('configs').forEach(function(property){
          if (configurations[property.get('filename').replace('.xml','')]){
            configurations[property.get('filename').replace('.xml','')]['properties'][property.get('name')] = property.get('value');
          } else {
            console.warn(property.get('name') + " from " + property.get('filename') + " can't be validate");
          }
        });
      }
    });
    return configurations;
  },

  /**
   * @method generateHostGroups
   * @param {Array} hostNames - list of all hostNames
   * @returns {{blueprint: {host_groups: Array}, blueprint_cluster_binding: {host_groups: Array}}}
   */
  generateHostGroups: function(hostNames) {
    var recommendations = {
      blueprint: {
        host_groups: []
      },
      blueprint_cluster_binding: {
        host_groups: []
      }
    };
    var hostsMap = this.getComponentForHosts();

    for (var i = 0; i <= hostNames.length; i++) {
      var host_group = {
        name: "host-group-" + (i + 1),
        components: []
      };
      var hcFiltered = hostsMap[hostNames[i]];
      if (Em.isNone(hcFiltered)) continue;
      for (var j = 0; j < hcFiltered.length; j++) {
        host_group.components.push({
          name: hcFiltered[j]
        });
      }
      recommendations.blueprint.host_groups.push(host_group);
      recommendations.blueprint_cluster_binding.host_groups.push({
        name: "host-group-" + (i + 1),
        hosts: [{
          fqdn: hostNames[i]
        }]
      });
    }
    return recommendations;
  },

  /**
   * Small helper method to update hostMap
   * it perform update of object only
   * if unique component per host is added
   *
   * @param {Object} hostMapObject
   * @param {string[]} hostNames
   * @param {string} componentName
   * @returns {Object}
   * @private
   */
  _generateHostMap: function(hostMapObject, hostNames, componentName) {
    Em.assert('hostMapObject, hostNames, componentName should be defined', !!hostMapObject && !!hostNames && !!componentName);
    if (!hostNames.length) return hostMapObject;
    hostNames.forEach(function(hostName) {
      if (!hostMapObject[hostName])
        hostMapObject[hostName] = [];

      if (!hostMapObject[hostName].contains(componentName))
        hostMapObject[hostName].push(componentName);
    });
    return hostMapObject;
  },

  /**
   * collect all component names that are present on hosts
   * @returns {object}
   */
  getComponentForHosts: function() {
    var hostsMap = {};
    App.ClientComponent.find().forEach(function(c) {
      hostsMap = this._generateHostMap(hostsMap, c.get('hostNames'), c.get('componentName'));
    }, this);
    App.SlaveComponent.find().forEach(function (c) {
      hostsMap = this._generateHostMap(hostsMap, c.get('hostNames'), c.get('componentName'));
    }, this);
    App.MasterComponent.find().forEach(function (c) {
      hostsMap = this._generateHostMap(hostsMap, c.get('hostNames'), c.get('componentName'));
    }, this);
    return hostsMap;
  }
};
