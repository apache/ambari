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

describe('App.EnhancedConfigsMixin', function() {

  var mixinObject = Em.Controller.extend(App.EnhancedConfigsMixin, {});
  var instanceObject = mixinObject.create({});
  describe('#removeCurrentFromDependentList()', function() {
    it('update some fields', function() {
      instanceObject.get('recommendations').pushObject({
          saveRecommended: true,
          saveRecommendedDefault: true,
          configGroup: "Default",
          propertyName: 'p1',
          propertyFileName: 'f1',
          value: 'v1'
        });
      instanceObject.removeCurrentFromDependentList(Em.Object.create({name: 'p1', filename: 'f1.xml', value: 'v2'}));
      expect(instanceObject.get('recommendations')[0]).to.eql({
        saveRecommended: false,
        saveRecommendedDefault: false,
        configGroup: "Default",
        propertyName: 'p1',
        propertyFileName: 'f1',
        value: 'v1'
      });
    });
  });

  describe('#buildConfigGroupJSON()', function() {
    it('generates JSON based on config group info', function() {
      var configGroup = Em.Object.create({
        name: 'group1',
        isDefault: false,
        hosts: ['host1', 'host2']
      });
      var configs = [
        App.ServiceConfigProperty.create({
          name: 'p1',
          filename: 'f1',
          overrides: [
            App.ServiceConfigProperty.create({
              group: configGroup,
              value: 'v1'
            })
          ]
        }),
        App.ServiceConfigProperty.create({
          name: 'p2',
          filename: 'f1',
          overrides: [
            App.ServiceConfigProperty.create({
              group: configGroup,
              value: 'v2'
            })
          ]
        }),
        App.ServiceConfigProperty.create({
          name: 'p3',
          filename: 'f2'
        })
      ];
      expect(instanceObject.buildConfigGroupJSON(configs, configGroup)).to.eql({
        "configurations": [
          {
            "f1": {
              "properties": {
                "p1": "v1",
                "p2": "v2"
              }
            }
          }
        ],
        "hosts": ['host1', 'host2']
      })
    });

    it('throws error as group is null', function() {
      expect(instanceObject.buildConfigGroupJSON.bind(instanceObject)).to.throw(Error, 'configGroup can\'t be null');
    });
  });

  describe("#dependenciesMessage", function () {
    var mixinInstance = mixinObject.create({
      changedProperties: []
    });
    it("no properties changed", function() {
      mixinInstance.set('changedProperties', []);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.plural').format(0) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.plural').format(0)
      )
    });
    it("single property changed", function() {
      mixinInstance.set('changedProperties', [
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        })
      ]);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.singular').format(1) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.singular').format(1)
      )
    });
    it("two properties changed", function() {
      mixinInstance.set('changedProperties', [
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        }),
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        })
      ]);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.plural').format(2) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.singular').format(1)
      )
    });
    it("two properties changed, from different services", function() {
      mixinInstance.set('changedProperties', [
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S1'
        }),
        Em.Object.create({
          saveRecommended: true,
          serviceName: 'S2'
        })
      ]);
      mixinInstance.propertyDidChange('dependenciesMessage');
      expect(mixinInstance.get('dependenciesMessage')).to.equal(
        Em.I18n.t('popup.dependent.configs.dependencies.config.plural').format(2) +
        Em.I18n.t('popup.dependent.configs.dependencies.service.plural').format(2)
      )
    });
  });

  describe("#loadConfigRecommendations", function () {
    var mixinInstance;

    beforeEach(function(){
      mixinInstance = mixinObject.create({
        recommendationsConfigs: {},
        stepConfigs: [],
        hostGroups: {
          blueprint: {
            configurations: {}
          }
        }
      });
      this.mockedCallback = sinon.stub();
      sinon.stub(App.config, 'getClusterEnvConfigs').returns({
        done: function (callback) {
          callback([]);
        }
      });
    });

    afterEach(function(){
      App.config.getClusterEnvConfigs.restore();
    });

    it("should call callback if changedConfigs is empty array", function() {
      mixinInstance.loadConfigRecommendations([], this.mockedCallback);
      expect(testHelpers.findAjaxRequest('name', 'config.recommendations')).to.not.exist;
      expect(this.mockedCallback.calledOnce).to.be.true;
    });

    it("should call callback from ajax callback if changedConfigs is not empty", function() {
      mixinInstance.loadConfigRecommendations([{}], this.mockedCallback);
      var args = testHelpers.findAjaxRequest('name', 'config.recommendations');
      expect(args[0]).exists;
      args[0].callback();
      expect(this.mockedCallback.calledOnce).to.be.true;
    });

    it("should call getClusterEnvConfigs if there is no cluster-env configs in stepConfigs", function() {
      mixinInstance.loadConfigRecommendations([{}]);
      expect(App.config.getClusterEnvConfigs.calledOnce).to.be.true;
    });

    it("should not call getClusterEnvConfigs if there is cluster-env configs in stepConfigs", function() {
      mixinInstance.set('stepConfigs', [Em.Object.create({
        serviceName: 'MISC',
        configs: []
      })]);
      mixinInstance.loadConfigRecommendations([{}]);
      expect(App.config.getClusterEnvConfigs.calledOnce).to.be.false;
    });
  });

  describe("#changedDependentGroup", function () {
    var mixinInstance;

    beforeEach(function () {
      mixinInstance = mixinObject.create({
        selectedService: {
          serviceName: 'test',
          dependentServiceNames: ['test1', 'test2', 'test3'],
          configGroups: [
            {name: 'testCG'},
            {name: 'notTestCG'}
          ]
        },
        stepConfigs: [
          Em.Object.create({serviceName: 'test1'}),
          Em.Object.create({serviceName: 'test2'}),
          Em.Object.create({serviceName: 'test3'}),
          Em.Object.create({serviceName: 'test4'}),
          Em.Object.create({serviceName: 'test5'})
        ],
        selectedConfigGroup: {name: 'testCG'},
        recommendations: [1, 2, 3]
      });

      sinon.stub(App, 'showSelectGroupsPopup', Em.K);
      sinon.stub(App.Service, 'find').returns([
        {serviceName: 'test2'},
        {serviceName: 'test3'},
        {serviceName: 'test4'}
      ]);
    });

    afterEach(function () {
      App.showSelectGroupsPopup.restore();
      App.Service.find.restore();
    });

    it("should call showSelectGroupsPopup with appropriate arguments", function () {
      mixinInstance.changedDependentGroup();
      expect(App.showSelectGroupsPopup.calledWith(
          'test',
          {name: 'testCG'},
          [
            Em.Object.create({serviceName: 'test2'}),
            Em.Object.create({serviceName: 'test3'})
          ],
          [1, 2, 3]
      )).to.be.true;
    });
  });
});

