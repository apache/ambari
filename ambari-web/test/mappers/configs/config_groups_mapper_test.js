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

var App = require('app');
require('mappers/configs/config_groups_mapper');

/**
 * not using this mapper
 */
describe.skip('App.configGroupsMapper', function () {

  var allHosts = App.get('allHostNames');
  var defaultAllHosts = ['host1', 'host2', 'host3'];
  beforeEach(function () {
    App.set('allHostNames', defaultAllHosts);
  });
  afterEach(function(){
    App.set('allHostNames', allHosts);
  });

  describe("#map", function() {

    var json = {
      "items" : [
        {
          "ConfigGroup" : {
            "cluster_name" : "1",
            "description" : "1",
            "desired_configs" : [
              {
                "tag" : "version1426088081862",
                "type" : "hadoop-env"
              }
            ],
            "group_name" : "1",
            "hosts" : [
              {
                "host_name" : "host1"
              }
            ],
            "id" : 2,
            "tag" : "Service1"
          }
        },
        {
          "ConfigGroup" : {
            "cluster_name" : "1",
            "description" : "hdfs2",
            "desired_configs" : [ ],
            "group_name" : "hdfs2",
            "hosts" : [
              {
                "host_name" : "host2"
              }
            ],
            "id" : 3,
            "tag" : "Service1"
          }
        },
        {
          "ConfigGroup" : {
            "cluster_name" : "1",
            "description" : "yarn1",
            "desired_configs" : [ ],
            "group_name" : "yarn1",
            "hosts" : [
              {
                "host_name" : "host1"
              },
              {
                "host_name" : "host2"
              }
            ],
            "id" : 4,
            "tag" : "Service2"
          }
        }
      ]
    };

    beforeEach(function () {
      App.resetDsStoreTypeMap(App.ServiceConfigGroup);
      App.resetDsStoreTypeMap(App.Service);
      sinon.stub(App.store, 'commit', Em.K);
    });
    afterEach(function(){
      App.store.commit.restore();
    });

    it('should not do anything as there is no serviceName', function() {
      App.configGroupsMapper.map(json);
      expect(App.ServiceConfigGroup.find().get('length')).to.equal(0);
    });

    it('should generate default groups for services', function() {
      App.Service.createRecord({'id': 'Service1'});
      App.configGroupsMapper.map(null, ["Service1"]);
      expect(App.ServiceConfigGroup.find().get('length')).to.equal(1);
      expect(App.ServiceConfigGroup.find('Service10').get('id')).to.eql('Service10');
      expect(App.ServiceConfigGroup.find('Service10').get('configGroupId')).to.eql(-1);
      expect(App.ServiceConfigGroup.find('Service10').get('name')).to.eql('Service1 Default');
      expect(App.ServiceConfigGroup.find('Service10').get('description')).to.eql('Default cluster level Service1 configuration');
      expect(App.ServiceConfigGroup.find('Service10').get('hostNames')).to.eql(defaultAllHosts);
      expect(App.ServiceConfigGroup.find('Service10').get('serviceName')).to.eql('Service1');
      expect(App.ServiceConfigGroup.find('Service10').get('service.id')).to.eql('Service1');
    });

    it('should generate groups form json and default config groups', function() {
      App.Service.createRecord({'id': 'Service1'});
      App.Service.createRecord({'id': 'Service2'});
      App.configGroupsMapper.map(json, ["Service1", "Service2"]);
      expect(App.ServiceConfigGroup.find().get('length')).to.equal(5);
      expect(App.ServiceConfigGroup.find().mapProperty('id')).to.eql(["Service12", "Service13", "Service24", "Service10", "Service20"]);
    });

    it('should generate groups form json and default config groups and check data', function() {
      App.Service.createRecord({'id': 'Service1'});
      App.Service.createRecord({'id': 'Service2'});
      App.configGroupsMapper.map(json, ["Service1", "Service2"]);

      expect(App.ServiceConfigGroup.find('Service12').get('id')).to.eql('Service12');
      expect(App.ServiceConfigGroup.find('Service12').get('configGroupId')).to.eql(2);
      expect(App.ServiceConfigGroup.find('Service12').get('name')).to.eql('1');
      expect(App.ServiceConfigGroup.find('Service12').get('description')).to.eql('1');
      expect(App.ServiceConfigGroup.find('Service12').get('hostNames')).to.eql(["host1"]);
      expect(App.ServiceConfigGroup.find('Service12').get('serviceName')).to.eql('Service1');
      expect(App.ServiceConfigGroup.find('Service12').get('service.id')).to.eql('Service1');
    });
  });

  describe("generateDefaultGroup", function() {
    var tests = [
      {
        service: 's1',
        hosts: ['h1'],
        res: {
          id: 's10',
          config_group_id: '-1',
          name: 's1 Default',
          service_name: 's1',
          description: 'Default cluster level s1 configuration',
          host_names: ['h1'],
          service_id: 's1'
        },
        m: 'with hosts'
      },
      {
        service: 's1',
        res: {
          id: 's10',
          config_group_id: '-1',
          name: 's1 Default',
          service_name: 's1',
          description: 'Default cluster level s1 configuration',
          host_names: defaultAllHosts,
          service_id: 's1'
        },
        m: 'without hosts'
      }
    ];


    tests.forEach(function(t) {
      it('generates default config group mock object ' + t.m, function() {
        expect(App.configGroupsMapper.generateDefaultGroup(t.service, t.hosts)).to.be.eql(t.res);
      });
    });

  });
});
