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

describe('App.EnhancedConfigsMixin', function() {

  var mixinObject = Em.Controller.extend(App.EnhancedConfigsMixin, {});
  var instanceObject = mixinObject.create({});
  describe('#removeCurrentFromDependentList()', function() {
    it('update some fields', function() {
      instanceObject.get('_dependentConfigValues').pushObject({
          saveRecommended: true,
          saveRecommendedDefault: true,
          propertyName: 'p1',
          fileName: 'f1',
          value: 'v1'
        });
      instanceObject.removeCurrentFromDependentList(Em.Object.create({name: 'p1', filename: 'f1.xml', value: 'v2'}));
      expect(instanceObject.get('_dependentConfigValues')[0]).to.eql({
        saveRecommended: false,
        saveRecommendedDefault: false,
        propertyName: 'p1',
        fileName: 'f1',
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
});

