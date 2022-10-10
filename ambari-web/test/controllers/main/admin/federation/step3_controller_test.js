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
var testHelpers = require('test/helpers');

function getController() {
  return App.NameNodeFederationWizardStep3Controller.create();
}

describe('App.NameNodeFederationWizardStep3Controller', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  describe("#clearStep()", function () {

    it("should clear step", function() {
      controller.reopen({
        stepConfigs: [1,2,3],
        serverConfigData: {data: 'data'},
        isConfigsLoaded: true,
        isLoaded: true
      });
      controller.clearStep();
      expect(controller.get('stepConfigs')).to.be.empty;
      expect(controller.get('serverConfigData')).to.be.empty;
      expect(controller.get('isConfigsLoaded')).to.be.false;
      expect(controller.get('isLoaded')).to.be.false;
    });
  });

  describe("#loadStep()", function () {

    beforeEach(function() {
      sinon.spy(controller, 'clearStep');
      sinon.spy(controller, 'loadConfigsTags')
    });

    afterEach(function() {
      controller.clearStep.restore();
      controller.loadConfigsTags.restore();
    });

    it("should execute updateComponent function", function() {
      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
      expect(controller.loadConfigsTags.calledOnce).to.be.true;
    });
  });

  describe("#loadConfigsTags()", function () {

    it("should return ajax send request", function() {
      controller.loadConfigsTags();
      var args = testHelpers.findAjaxRequest('name', 'config.tags');
      expect(args[0]).to.be.not.empty;
    });
  });

  describe("#onLoadConfigs()", function () {

    it("should set proper values", function() {
      var data = {data: 'data'};
      controller.reopen({
        serverConfigData: data,
        isConfigsLoaded: true,
      });
      controller.onLoadConfigs(data);
      expect(controller.get('serverConfigData')).to.eql(data);
      expect(controller.get('isConfigsLoaded')).to.be.true;
    });
  });

  describe("#onLoadConfigsTags()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it("should return ajax send request without RANGER and ACCUMULO services", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'hdfs-site': {
              tag: 'tag1'
            }
          }
        }
      };
      this.mock.returns([
        {serviceName: 'YARN'},
        {serviceName: 'HIVE'}
      ]);
      controller.onLoadConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0].data.urlParams).to.equal('(type=hdfs-site&tag=tag1)');
    });

    it("should return ajax send request without RANGER and ACCUMULO services", function() {
      var data = {
        Clusters: {
          desired_configs: {
            'hdfs-site': {
              tag: 'tag1'
            },
            'core-site': {
              tag: 'tag2'
            },
            'ranger-tagsync-site': {
              tag: 'tag3'
            },
            'ranger-hdfs-security': {
              tag: 'tag4'
            },
            'accumulo-site': {
              tag: 'tag5'
            }
          }
        }
      };
      this.mock.returns([
        {serviceName: 'RANGER'},
        {serviceName: 'ACCUMULO'}
      ]);
      controller.onLoadConfigsTags(data);
      var args = testHelpers.findAjaxRequest('name', 'admin.get.all_configurations');
      expect(args[0].data.urlParams).to.equal('(type=hdfs-site&tag=tag1)|(type=core-site&tag=tag2)|(type=ranger-tagsync-site&tag=tag3)|(type=ranger-hdfs-security&tag=tag4)|(type=accumulo-site&tag=tag5)');
    });
  });

  describe("#createRangerServiceProperty()", function () {

    it("should return object with proper param values", function() {
      expect(controller.createRangerServiceProperty('service1', 'pref1', 'prop1')).to.eql({
        "name": 'prop1',
        "displayName": 'prop1',
        "isReconfigurable": false,
        "recommendedValue": 'pref1service1',
        "value": 'pref1service1',
        "category": "RANGER",
        "filename": "ranger-tagsync-site",
        "serviceName": 'MISC'
      });
    });
  });

  describe('#removeConfigs', function() {

    it('should remove config properties', function() {
      var configs = {
        items: [
          {
            type: 'k1',
            properties: {
              p1: {}
            }
          }
        ]
      };
      expect(JSON.stringify(controller.removeConfigs({k1: ['p1']}, configs))).to.be.equal(JSON.stringify({
        items: [
          {
            type: 'k1',
            properties: {}
          }
        ]
      }));
    });
  });

  describe('#renderServiceConfigs', function() {
    var _serviceConfig = {
      configCategories: [
        {
          name: 'S1'
        }
      ]
    };

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        {
          serviceName: 'S1'
        }
      ]);
      sinon.stub(controller, 'loadComponentConfigs');
      controller.set('stepConfigs', []);
      controller.renderServiceConfigs(_serviceConfig);
    });

    afterEach(function() {
      controller.loadComponentConfigs.restore();
      App.Service.find.restore();
    });

    it('stepConfigs should not be empty', function() {
      expect(controller.get('stepConfigs')).to.not.be.empty;
    });

    it('selectedService should be object', function() {
      expect(controller.get('selectedService')).to.be.an.object;
    });

    it('once should be true', function() {
      expect(controller.get('once')).to.be.true;
    });

    it('loadComponentConfigs should be called', function() {
      expect(controller.loadComponentConfigs.calledOnce).to.be.true;
    });
  });

  describe('#loadComponentConfigs', function() {
    var componentConfig = {
      configs: []
    };

    it('configs should not be empty', function() {
      controller.loadComponentConfigs({configs: [{}]}, componentConfig);
      expect(componentConfig.configs).to.not.be.empty;
    });

    it('isEditable should be true', function() {
      controller.loadComponentConfigs({configs: [{isReconfigurable: true}]}, componentConfig);
      expect(componentConfig.configs[0].get('isEditable')).to.be.true;
    });
  });

  describe("#isNextDisabled()", function () {

    var cases = [
      {
        isLoaded: false,
        isNextDisabled: true,
        configs: {isValid: true},
        title: 'wizard step content not loaded{1}'
      },
      {
        isLoaded: true,
        isNextDisabled: true,
        configs: {isValid: false},
        title: 'wizard step content not loaded{2}'
      },
      {
        isLoaded: true,
        isNextDisabled: false,
        configs: {isValid: true},
        title: 'wizard step content loaded'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.reopen({
          selectedService: {
            configs: [item.configs]
          },
          isLoaded: item.isLoaded
        });
        expect(controller.get('isNextDisabled')).to.equal(item.isNextDisabled);
      });
    });
  });

  describe('#onLoad', function() {

    beforeEach(function() {
      sinon.stub(controller, 'tweakServiceConfigs');
      sinon.stub(controller, 'renderServiceConfigs');
      sinon.stub(controller, 'removeConfigs');
      this.routerMock = sinon.stub(App.router, 'get');
      this.appMock = sinon.stub(App, 'get');
    });

    afterEach(function() {
      controller.tweakServiceConfigs.restore();
      controller.renderServiceConfigs.restore();
      controller.removeConfigs.restore();
      this.routerMock.restore();
      this.appMock.restore();
    });

    it('renderServiceConfigs should be called{1}', function() {
      controller.reopen({
        isConfigsLoaded: true,
      });
      this.appMock.returns(true);
      this.routerMock.returns(true);
      controller.onLoad();
      expect(controller.tweakServiceConfigs.calledOnce).to.be.true;
      expect(controller.removeConfigs.calledOnce).to.be.true;
      expect(controller.renderServiceConfigs.calledOnce).to.be.true;
      expect(controller.get('isLoaded')).to.be.true;
    });

    it('renderServiceConfigs should be called{2}', function() {
      controller.reopen({
        isConfigsLoaded: true,
      });
      this.appMock.returns(false);
      this.routerMock.returns(true);
      controller.onLoad();
      expect(controller.tweakServiceConfigs.calledOnce).to.be.true;
      expect(controller.removeConfigs.calledOnce).to.be.true;
      expect(controller.renderServiceConfigs.calledOnce).to.be.true;
      expect(controller.get('isLoaded')).to.be.true;
    });

    it('renderServiceConfigs should not be called', function() {
      controller.onLoad();
      expect(controller.tweakServiceConfigs.calledOnce).to.be.false;
      expect(controller.removeConfigs.calledOnce).to.be.false;
      expect(controller.renderServiceConfigs.calledOnce).to.be.false;
      expect(controller.get('isLoaded')).to.be.false;
    });
  });

  describe('#prepareDependencies', function() {

    beforeEach(function() {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'JOURNALNODE',
          service: {
            serviceName: 'S1'
          },
          hostName: 'host1'
        })
      ]);
      sinon.stub(App.HDFSService, 'find').returns([
        Em.Object.create({
          masterComponentGroups: [
            {name: 'group1'},
            {name: 'group2'}
          ]
        })
      ]);
      sinon.stub(App, 'get').returns('c1');
    });

    afterEach(function() {
      App.HostComponent.find.restore();
      App.HDFSService.find.restore();
      App.get.restore();
    });

    it('should return object with dependencies{1}', function () {
      controller.reopen({
        content: {
          nameServiceId: 'group3',
          masterComponentHosts: [
            {
              component: 'NAMENODE',
              isInstalled: false,
              hostName: 'host1'
            },
            {
              component: 'NAMENODE',
              isInstalled: false,
              hostName: 'host2'
            }
          ]
        },
        serverConfigData: {
          items: [{
            type: 'hdfs-site',
            properties: {
              'dfs.namenode.http-address': 'h1:1234',
              'dfs.namenode.https-address': 'h1:4321',
              'dfs.namenode.rpc-address': 'h:1111',
              'dfs.namenode.rpc-address.group1.nn1': 'h1:1111',
              'dfs.namenode.rpc-address.group1.nn2': 'h2:1111',
              'dfs.journalnode.edits.dir': '/hadoop/hdfs/journalnode123'
            }
          },]
        }
      });
      expect(controller.prepareDependencies()).to.eql({
        nameServicesList: 'group1,group2',
        nameservice1: 'group1',
        newNameservice: 'group3',
        namenode1: 'h1',
        namenode2: 'h2',
        newNameNode1Index: 'nn1',
        newNameNode2Index: 'nn2',
        newNameNode1: 'host1',
        newNameNode2: 'host2',
        journalnodes: 'host1:8485',
        clustername: 'c1',
        nnHttpPort: '1234',
        nnHttpsPort: '4321',
        nnRpcPort: '1111',
        journalnode_edits_dir: '/hadoop/hdfs/journalnode123'
      });
    });

    it('should return object with dependencies{2}', function () {
      controller.reopen({
        content: {
          nameServiceId: 'group3',
          masterComponentHosts: [
            {
              component: 'NAMENODE',
              isInstalled: false,
              hostName: 'host1'
            },
            {
              component: 'NAMENODE',
              isInstalled: false,
              hostName: 'host2'
            }
          ]
        },
        serverConfigData: {
          items: [{
            type: 'hdfs-site',
            properties: {
              'dfs.namenode.rpc-address.group1.nn1': 'h1:1111',
              'dfs.namenode.rpc-address.group1.nn2': 'h2:1111',
              'dfs.journalnode.edits.dir': '/hadoop/hdfs/journalnode123'
            }
          },]
        }
      });
      expect(controller.prepareDependencies()).to.eql({
        nameServicesList: 'group1,group2',
        nameservice1: 'group1',
        newNameservice: 'group3',
        namenode1: 'h1',
        namenode2: 'h2',
        newNameNode1Index: 'nn1',
        newNameNode2Index: 'nn2',
        newNameNode1: 'host1',
        newNameNode2: 'host2',
        journalnodes: 'host1:8485',
        clustername: 'c1',
        nnHttpPort: 50070,
        nnHttpsPort: 50470,
        nnRpcPort: 8020,
        journalnode_edits_dir: '/hadoop/hdfs/journalnode123'
      });
    });
  });

});
