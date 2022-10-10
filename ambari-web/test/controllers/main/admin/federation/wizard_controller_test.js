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

function getController() {
  return App.NameNodeFederationWizardController.create({});
}

describe('App.NameNodeFederationWizardController', function() {
  var controller;

  beforeEach(function() {
    controller = getController();
  });

  describe("#finish()", function () {

    beforeEach(function() {
      sinon.stub(App.router, 'get').returns({updateAll: Em.K});
      sinon.stub(controller, 'resetDbNamespace', Em.K);
    });

    afterEach(function() {
      App.router.get.restore();
    });

    it("App.router.send should be called", function() {
      controller.finish();
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
      expect(App.router.get.calledOnce).to.be.true;
      expect(controller.get('isFinished')).to.be.true;
    });
  });

  describe("#clearAllSteps()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'clearInstallOptions', Em.K);
      sinon.stub(controller, 'getCluster').returns({name: 'c1'});
    });

    afterEach(function() {
      controller.getCluster.restore();
    });

    it("should clear all steps and return cluster", function() {
      controller.clearAllSteps();
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
      expect(controller.getCluster.calledOnce).to.be.true;
      expect(controller.get('content.cluster')).to.eql({name: 'c1'});
    });
  });

  describe("#loadServiceConfigProperties()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns({props: []});
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("should return serviceConfigProperties", function() {
      controller.loadServiceConfigProperties();
      expect(controller.getDBProperty.calledOnce).to.be.true;
      expect(controller.get('content.serviceConfigProperties')).to.eql({props: []});
    });
  });

  describe("#loadNameServiceId()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'getDBProperty').returns({id: '123'});
    });

    afterEach(function() {
      controller.getDBProperty.restore();
    });

    it("should load name service id", function() {
      controller.loadNameServiceId();
      expect(controller.getDBProperty.calledOnce).to.be.true;
      expect(controller.get('content.nameServiceId')).to.eql({id: '123'});
    });
  });

  describe("#saveNameServiceId()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty', Em.K);
    });

    it("should save name service id", function() {
      controller.saveNameServiceId({id: '123'});
      expect(controller.setDBProperty.calledOnce).to.be.true;
      expect(controller.get('content.nameServiceId')).to.eql({id: '123'});
    });
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("should set current step", function() {
      controller.setCurrentStep('step', true);
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#saveServiceConfigProperties()", function () {

    beforeEach(function() {
      sinon.stub(controller, 'setDBProperty', Em.K);
    });

    it("should set current set service config properties{1}", function() {
      var stepController = Em.Object.create(
        {
          serverConfigData: {
            items: [{
              type: 'file1',
              properties: {
                c1: 'val1'
              }
            }]
          },
          stepConfigs: [
            Em.Object.create({
              configs: [
                Em.Object.create({
                  name: 'c1',
                  value: 'val123',
                  filename: 'file1'
                })
              ]
            })
          ]
        }
      );
      controller.saveServiceConfigProperties(stepController);
      expect(controller.setDBProperty.calledOnce).to.be.true;
      expect(controller.get('content.serviceConfigProperties').items[0]).to.eql({
          type: 'file1',
          properties: {
            c1: 'val123'
          }
        }
      );
    });

    it("should set current set service config properties{2}", function() {
      var stepController = Em.Object.create(
        {
          serverConfigData: {
            items: [{
              type: 'file1',
              properties: {
                c1: 'val1'
              }
            }]
          },
          stepConfigs: [
            Em.Object.create({
              configs: [
                Em.Object.create({
                  name: 'c2',
                  value: 'val2',
                  filename: 'file2'
                })
              ]
            }),
          ]
        }
      );
      controller.saveServiceConfigProperties(stepController);
      expect(controller.setDBProperty.calledOnce).to.be.true;
      expect(controller.get('content.serviceConfigProperties').items[0]).to.eql({
          type: 'file1',
          properties: {
            c1: 'val1'
          }
        }
      );
    });
  });

  describe('#loadMap', function() {

    describe('should load cluster', function() {
      var loadCluster = false;
      var checker = {
        load: function() {
          loadCluster = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['1'][0].callback.call(checker);
      });

      it('cluster info is loaded', function () {
        expect(loadCluster).to.be.true;
      });
    });

    describe('should load service hosts', function() {
      var loadServicesFromServer = false;
      var loadMasterComponentHosts = false;
      var loadConfirmedHosts = false;

      var checker = {
        loadServicesFromServer: function () {
          loadServicesFromServer = true;
        },
        loadMasterComponentHosts: function () {
          loadMasterComponentHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadConfirmedHosts: function () {
          loadConfirmedHosts = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['2'][0].callback.call(checker);
      });

      it('services from server are loaded', function () {
        expect(loadServicesFromServer).to.be.true;
      });

      it('master component hosts are loaded', function () {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('confirmed hosts are loaded', function () {
        expect(loadConfirmedHosts).to.be.true;
      });
    });

    describe('should load name service id', function() {
      var loadNameServiceId = false;
      var checker = {
        loadNameServiceId: function() {
          loadNameServiceId = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['3'][0].callback.call(checker);
      });

      it('name service id is loaded', function () {
        expect(loadNameServiceId).to.be.true;
      });
    });

    describe('should load tasks', function() {
      var loadServiceConfigProperties = false;
      var loadTasksStatuses = false;
      var loadTasksRequestIds = false;
      var loadRequestIds = false;

      var checker = {
        loadServiceConfigProperties: function () {
          loadServiceConfigProperties = true;
        },
        loadTasksStatuses: function () {
          loadTasksStatuses = true;
          return $.Deferred().resolve().promise();
        },
        loadTasksRequestIds: function () {
          loadTasksRequestIds = true;
        },
        loadRequestIds: function () {
          loadRequestIds = true;
        }
      };

      beforeEach(function () {
        controller.loadMap['4'][0].callback.call(checker);
      });

      it('service config properties are loaded', function () {
        expect(loadServiceConfigProperties).to.be.true;
      });

      it('task statuses are loaded', function () {
        expect(loadTasksStatuses).to.be.true;
      });

      it('task request ids are loaded', function () {
        expect(loadTasksRequestIds).to.be.true;
      });
      
      it('request ids are loaded', function () {
        expect(loadRequestIds).to.be.true;
      });
    });
  });

});
