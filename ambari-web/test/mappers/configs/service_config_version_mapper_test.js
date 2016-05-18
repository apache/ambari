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
require('mappers/configs/service_config_version_mapper');

describe.skip('App.serviceConfigVersionsMapper', function () {

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
      App.resetDsStoreTypeMap(App.ServiceConfigVersion);
      sinon.stub(App.store, 'commit', Em.K);
    });
    afterEach(function(){
      App.store.commit.restore();
    });

    it('should not do anything as there is no json', function() {
      App.serviceConfigVersionsMapper.map(null);
      expect(App.ServiceConfigVersion.find().get('length')).to.equal(0);
    });

    describe('should load data to model', function() {

      beforeEach(function () {
        App.serviceConfigVersionsMapper.map(json);
      });

      it('two versions are mapped', function () {
        expect(App.ServiceConfigVersion.find().get('length')).to.equal(2);
      });

      it('services have correct ids', function () {
        expect(App.ServiceConfigVersion.find().mapProperty('id')).to.eql(['SERVICE1_1','SERVICE2_4']);
      });

      it('SERVICE1_1 createTime', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('createTime')).to.equal(1425979244738);
      });
      it('SERVICE1_1 groupId', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('groupId')).to.equal(-1);
      });
      it('SERVICE1_1 hosts', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('hosts')).to.eql(defaultAllHosts);
      });
      it('SERVICE1_1 isCurrent', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('isCurrent')).to.be.true;
      });
      it('SERVICE1_1 version', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('version')).to.equal(1);
      });
      it('SERVICE1_1 notes', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('notes')).to.equal("Initial configurations for SERVICE1");
      });
      it('SERVICE1_1 serviceName', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('serviceName')).to.equal("SERVICE1");
      });
      it('SERVICE1_1 author', function () {
        expect(App.ServiceConfigVersion.find('SERVICE1_1').get('author')).to.equal("admin");
      });

      it('SERVICE2_4 createTime', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('createTime')).to.equal(1426088137115);
      });
      it('SERVICE2_4 groupId', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('groupId')).to.equal(2);
      });
      it('SERVICE2_4 hosts', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('hosts')).to.eql(["host1"]);
      });
      it('SERVICE2_4 isCurrent', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('isCurrent')).to.be.false;
      });
      it('SERVICE2_4 version', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('version')).to.equal(4);
      });
      it('SERVICE2_4 notes', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('notes')).to.equal("");
      });
      it('SERVICE2_4 serviceName', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('serviceName')).to.equal("SERVICE2");
      });
      it('SERVICE2_4 author', function () {
        expect(App.ServiceConfigVersion.find('SERVICE2_4').get('author')).to.equal("admin");
      });
    });
  });

});

