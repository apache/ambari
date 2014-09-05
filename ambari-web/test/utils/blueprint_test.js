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

var blueprintUtils = require('utils/blueprint');

describe('utils/blueprint', function() {
  var masterBlueprint = {
    blueprint: {
      host_groups: [
        {
          name: "host-group-1",
          components: [
            { name: "ZOOKEEPER_SERVER" },
            { name: "NAMENODE" },
            { name: "HBASE_MASTER" }
          ]
        },
        {
          name: "host-group-2",
          components: [
            { name: "SECONDARY_NAMENODE" }
          ]
        }
      ]
    },
    blueprint_cluster_binding: {
      host_groups: [
        {
          name: "host-group-1",
          hosts: [
            { fqdn: "host1" },
            { fqdn: "host2" }
          ]
        },
        {
          name: "host-group-2",
          hosts: [
            { fqdn: "host3" }
          ]
        }
      ]
    }
  };

  var slaveBlueprint = {
    blueprint: {
      host_groups: [
        {
          name: "host-group-1",
          components: [
            { name: "DATANODE" }
          ]
        },
        {
          name: "host-group-2",
          components: [
            { name: "DATANODE" },
            { name: "HDFS_CLIENT" },
            { name: "ZOOKEEPER_CLIENT" }
          ]
        }
      ]
    },
    blueprint_cluster_binding: {
      host_groups: [
        {
          name: "host-group-1",
          hosts: [
            { fqdn: "host3" }
          ]
        },
        {
          name: "host-group-2",
          hosts: [
            { fqdn: "host4" },
            { fqdn: "host5" }
          ]
        }
      ]
    }
  };

  describe('#getHostsFromBlueprint', function() {
    it('should extract all hosts from blueprint', function() {
      expect(blueprintUtils.getHostsFromBlueprint(masterBlueprint)).to.deep.equal(["host1", "host2", "host3"]);
    });
  });

  describe('#getHostsFromBlueprintByGroupName', function() {
    it('should extract hosts from blueprint by given group name', function() {
      expect(blueprintUtils.getHostsFromBlueprintByGroupName(masterBlueprint, "host-group-1")).to.deep.equal([
        { fqdn: "host1" },
        { fqdn: "host2" }
      ]);
    });

    it('should return empty array if group with given name doesn\'t exist', function() {
      expect(blueprintUtils.getHostsFromBlueprintByGroupName(masterBlueprint, "not an existing group")).to.deep.equal([]);
    });
  });

  describe('#getComponentsFromBlueprintByGroupName', function() {
    it('should extract all components from blueprint for given host', function() {
      expect(blueprintUtils.getComponentsFromBlueprintByGroupName(masterBlueprint, "host-group-1")).to.deep.equal([
        { name: "ZOOKEEPER_SERVER" },
        { name: "NAMENODE" },
        { name: "HBASE_MASTER" }
      ]);
    });

    it('should return empty array if group doesn\'t exists', function() {
      expect(blueprintUtils.getComponentsFromBlueprintByGroupName(masterBlueprint, "not an existing group")).to.deep.equal([]);
    });

    it('should return empty array if group name isn\'t valid', function() {
      expect(blueprintUtils.getComponentsFromBlueprintByGroupName(masterBlueprint, undefined)).to.deep.equal([]);
    });
  });

  describe('#matchGroups', function() {
    it('should compose same host group into pairs', function() {
      expect(blueprintUtils.matchGroups(masterBlueprint, slaveBlueprint)).to.deep.equal([
        { g1: "host-group-1" },
        { g1: "host-group-2", g2: "host-group-1" },
        { g2: "host-group-2" }
      ]);
    });
  });

  describe('#filterByComponents', function() {
    it('should remove all components except', function() {
      expect(blueprintUtils.filterByComponents(masterBlueprint, ["NAMENODE"])).to.deep.equal({
        blueprint: {
          host_groups: [
            {
              name: "host-group-1",
              components: [
                { name: "NAMENODE" }
              ]
            }
          ]
        },
        blueprint_cluster_binding: {
          host_groups: [
            {
              name: "host-group-1",
              hosts: [
                { fqdn: "host1" },
                { fqdn: "host2" }
              ]
            }
          ]
        }
      });
    });
  });

  describe('#addComponentsToBlueprint', function() {
    it('should add components to blueprint', function() {
      var components = ["FLUME_HANDLER", "HCAT"];
      expect(blueprintUtils.addComponentsToBlueprint(masterBlueprint, components)).to.deep.equal({
        blueprint: {
          host_groups: [
            {
              name: "host-group-1",
              components: [
                { name: "ZOOKEEPER_SERVER" },
                { name: "NAMENODE" },
                { name: "HBASE_MASTER" },
                { name: "FLUME_HANDLER" },
                { name: "HCAT" }
              ]
            },
            {
              name: "host-group-2",
              components: [
                { name: "SECONDARY_NAMENODE" },
                { name: "FLUME_HANDLER" },
                { name: "HCAT" }
              ]
            }
          ]
        },
        blueprint_cluster_binding: {
          host_groups: [
            {
              name: "host-group-1",
              hosts: [
                { fqdn: "host1" },
                { fqdn: "host2" }
              ]
            },
            {
              name: "host-group-2",
              hosts: [
                { fqdn: "host3" }
              ]
            }
          ]
        }
      });
    });
  });

  describe('#mergeBlueprints', function() {
    it('should merge components', function() {
      expect(blueprintUtils.mergeBlueprints(masterBlueprint, slaveBlueprint)).to.deep.equal(
        {
          blueprint: {
            host_groups: [
              {
                name: "host-group-1",
                components: [
                  { name: "ZOOKEEPER_SERVER" },
                  { name: "NAMENODE" },
                  { name: "HBASE_MASTER" }
                ]
              },
              {
                name: "host-group-2",
                components: [
                  { name: "SECONDARY_NAMENODE" },
                  { name: "DATANODE" }
                ]
              },
              {
                name: "host-group-3",
                components: [
                  { name: "DATANODE" },
                  { name: "HDFS_CLIENT" },
                  { name: "ZOOKEEPER_CLIENT" }
                ]
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                name: "host-group-1",
                hosts: [
                  { fqdn: "host1" },
                  { fqdn: "host2" }
                ]
              },
              {
                name: "host-group-2",
                hosts: [
                  { fqdn: "host3" }
                ]
              },
              {
                name: "host-group-3",
                hosts: [
                  { fqdn: "host4" },
                  { fqdn: "host5" }
                ]
              }
            ]
          }
        }
      );
    });
  });

  describe('#buildConfisJSON', function () {
    var tests = [
      {
        "services": [
          Em.Object.create({
            serviceName: "YARN",
            configTypes: {
              "yarn-site": {},
              "yarn-env": {}
            },
            isInstalled: true
          })
        ],
        "stepConfigs": [
          Em.Object.create({
            serviceName: "YARN",
            configs: [
              Em.Object.create({
                name: "p1",
                value: "v1",
                filename: "yarn-site.xml"
              }),
              Em.Object.create({
                name: "p2",
                value: "v2",
                filename: "yarn-site.xml"
              }),
              Em.Object.create({
                name: "p3",
                value: "v3",
                filename: "yarn-env.xml"
              })
            ]
          })
        ],
        "configurations": {
          "yarn-site": {
            "properties": {
              "p1": "v1",
              "p2": "v2"
            }
          },
          "yarn-env": {
            "properties": {
              "p3": "v3"
            }
          }
        }
      }
    ];
    tests.forEach(function (test) {
      it("generate configs for request (use in validation)", function () {
        expect(blueprintUtils.buildConfisJSON(test.services, test.stepConfigs)).to.eql(test.configurations);
      });
    });
  });

  describe('#generateHostGroups', function () {
    var tests = [
      {
        "hostNames": ["host1", "host2"],
        "hostComponents": [
          Em.Object.create({
            componentName: "C1",
            hostName: "host1"
          }),
          Em.Object.create({
            componentName: "C2",
            hostName: "host1"
          }),
          Em.Object.create({
            componentName: "C1",
            hostName: "host2"
          }),
          Em.Object.create({
            componentName: "C3",
            hostName: "host2"
          })
        ],
        result: {
          blueprint: {
            host_groups: [
              {
                name: "host-group-1",
                "components": [
                  {
                    "name": "C1"
                  },
                  {
                    "name": "C2"
                  }
                ]
              },
              {
                name: "host-group-2",
                "components": [
                  {
                    "name": "C1"
                  },
                  {
                    "name": "C3"
                  }
                ]
              }
            ]
          },
          blueprint_cluster_binding: {
            host_groups: [
              {
                "name": "host-group-1",
                "hosts": [
                  {
                    "fqdn": "host1"
                  }
                ]
              },
              {
                "name": "host-group-2",
                "hosts": [
                  {
                    "fqdn": "host2"
                  }
                ]
              }
            ]
          }
        }
      }
    ];
    tests.forEach(function (test) {
      it("generate host groups", function () {
        expect(blueprintUtils.generateHostGroups(test.hostNames, test.hostComponents)).to.eql(test.result);
      });
    });
  });
});