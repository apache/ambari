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
require('mappers/configs/config_versions_mapper');

describe('App.configVersionsMapper', function () {

  var allHosts = App.get('allHostNames');
  var defaultAllHosts = ['host1', 'host2', 'host3'];
  beforeEach(function () {
    App.set('allHostNames', defaultAllHosts);
  });
  afterEach(function(){
    App.set('allHostNames', allHosts);
  });

  describe("#map", function() {

    var json = { items: [
      {
        "cluster_name" : "1",
        "createtime" : 1425979244738,
        "group_id" : -1,
        "group_name" : "default",
        "hosts" : [ ],
        "is_current" : true,
        "service_config_version" : 1,
        "service_config_version_note" : "Initial configurations for SERVICE1",
        "service_name" : "SERVICE1",
        "user" : "admin"
      },
      {
        "cluster_name" : "1",
        "configurations" : [
          {
            "Config" : {
              "cluster_name" : "1"
            },
            "type" : "hadoop-env",
            "tag" : "version1426088081862",
            "version" : 2,
            "properties" : {
              "dtnode_heapsize" : "1026m"
            },
            "properties_attributes" : { }
          }
        ],
        "createtime" : 1426088137115,
        "group_id" : 2,
        "group_name" : "1",
        "hosts" : [
          "host1"
        ],
        "is_current" : false,
        "service_config_version" : 4,
        "service_config_version_note" : "",
        "service_name" : "SERVICE2",
        "user" : "admin"
      },
    ]};

    beforeEach(function () {
      App.resetDsStoreTypeMap(App.ConfigVersion);
      App.resetDsStoreTypeMap(App.ConfigProperty);
      sinon.stub(App.store, 'commit', Em.K);
    });
    afterEach(function(){
      App.store.commit.restore();
    });

    it('should not do anything as there is no json', function() {
      App.configVersionsMapper.map(null);
      expect(App.ConfigVersion.find().get('length')).to.equal(0);
    });

    it('should load data to model', function() {
      App.configVersionsMapper.map(json);
      expect(App.ConfigVersion.find().get('length')).to.equal(2);
      expect(App.ConfigVersion.find().mapProperty('id')).to.eql(['SERVICE1_1','SERVICE2_4']);

      //SERVICE1_1
      expect(App.ConfigVersion.find('SERVICE1_1').get('configProperties.length')).to.eql(0);
      expect(App.ConfigVersion.find('SERVICE1_1').get('createTime')).to.eql(1425979244738);
      expect(App.ConfigVersion.find('SERVICE1_1').get('groupId')).to.eql(-1);
      expect(App.ConfigVersion.find('SERVICE1_1').get('hosts')).to.eql(defaultAllHosts);
      expect(App.ConfigVersion.find('SERVICE1_1').get('isCurrent')).to.be.true;
      expect(App.ConfigVersion.find('SERVICE1_1').get('version')).to.eql(1);
      expect(App.ConfigVersion.find('SERVICE1_1').get('notes')).to.eql("Initial configurations for SERVICE1");
      expect(App.ConfigVersion.find('SERVICE1_1').get('serviceName')).to.eql("SERVICE1");
      expect(App.ConfigVersion.find('SERVICE1_1').get('author')).to.eql("admin");

      //SERVICE1_2
      expect(App.ConfigVersion.find('SERVICE2_4').get('configProperties.length')).to.eql(1);
      expect(App.ConfigVersion.find('SERVICE2_4').get('createTime')).to.eql(1426088137115);
      expect(App.ConfigVersion.find('SERVICE2_4').get('groupId')).to.eql(2);
      expect(App.ConfigVersion.find('SERVICE2_4').get('hosts')).to.eql(["host1"]);
      expect(App.ConfigVersion.find('SERVICE2_4').get('isCurrent')).to.be.false;
      expect(App.ConfigVersion.find('SERVICE2_4').get('version')).to.eql(4);
      expect(App.ConfigVersion.find('SERVICE2_4').get('notes')).to.eql("");
      expect(App.ConfigVersion.find('SERVICE2_4').get('serviceName')).to.eql("SERVICE2");
      expect(App.ConfigVersion.find('SERVICE2_4').get('author')).to.eql("admin");

      //CONFIG_PROPERTY
      expect(App.ConfigProperty.find('dtnode_heapsize_hadoop-env_4').get('value')).to.eql('1026m');
      expect(App.ConfigProperty.find('dtnode_heapsize_hadoop-env_4').get('recommendedValue')).to.equal('1026m');
      expect(App.ConfigProperty.find('dtnode_heapsize_hadoop-env_4').get('isFinal')).to.be.false;
      expect(App.ConfigProperty.find('dtnode_heapsize_hadoop-env_4').get('recommendedIsFinal')).to.be.false;
    });
  });

});

