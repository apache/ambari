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
require('controllers/main/admin/highAvailability/resourceManager/step3_controller');

describe('App.RMHighAvailabilityWizardStep3Controller', function () {

  describe('#isSubmitDisabled', function () {

    var controller = App.RMHighAvailabilityWizardStep3Controller.create(),
      cases = [
        {
          isLoaded: false,
          isSubmitDisabled: true,
          title: 'wizard step content not loaded'
        },
        {
          isLoaded: true,
          isSubmitDisabled: false,
          title: 'wizard step content loaded'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isLoaded', item.isLoaded);
        expect(controller.get('isSubmitDisabled')).to.equal(item.isSubmitDisabled);
      });
    });

  });

  describe('#loadConfigTagsSuccessCallback', function () {

    var controller = App.RMHighAvailabilityWizardStep3Controller.create();

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it('should send proper ajax request', function () {
      controller.loadConfigTagsSuccessCallback({
        'Clusters': {
          'desired_configs': {
            'zoo.cfg': {
              'tag': 1
            }
          }
        }
      }, {}, {
        'serviceConfig': {}
      });
      var data = App.ajax.send.args[0][0].data;
      expect(data.urlParams).to.equal('(type=zoo.cfg&tag=1)');
      expect(data.serviceConfig).to.eql({});
    });

  });

  describe('#loadConfigsSuccessCallback', function () {

    var controller = App.RMHighAvailabilityWizardStep3Controller.create(),
      cases = [
        {
          'items': [],
          'params': {
            'serviceConfig': {}
          },
          'port': '2181',
          'title': 'empty response'
        },
        {
          'items': [
            {
              'type': 'zoo.cfg'
            }
          ],
          'params': {
            'serviceConfig': {}
          },
          'port': '2181',
          'title': 'no zoo.cfg properties received'
        },
        {
          'items': [
            {
              'type': 'zoo.cfg',
              'properties': {
                'n': 'v'
              }
            }
          ],
          'params': {
            'serviceConfig': {}
          },
          'port': '2181',
          'title': 'no clientPort property received'
        },
        {
          'items': [
            {
              'type': 'zoo.cfg',
              'properties': {
                'clientPort': '2182'
              }
            }
          ],
          'params': {
            'serviceConfig': {}
          },
          'port': '2182',
          'title': 'clientPort property received'
        }
      ];

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.loadConfigsSuccessCallback({
          items: item.items
        }, {}, item.params);
        expect(controller.setDynamicConfigValues.args[0]).to.eql([{}, item.port]);
        expect(controller.get('selectedService')).to.eql({});
        expect(controller.get('isLoaded')).to.be.true;
      });
    });

  });

  describe('#loadConfigsErrorCallback', function () {

    var controller = App.RMHighAvailabilityWizardStep3Controller.create();

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    it('should proceed with default value', function () {
      controller.loadConfigsErrorCallback({}, {}, {}, {}, {
        serviceConfig: {}
      });
      expect(controller.setDynamicConfigValues.args[0]).to.eql([{}, '2181']);
      expect(controller.get('selectedService')).to.eql({});
      expect(controller.get('isLoaded')).to.be.true;
    });

  });

  describe('#setDynamicConfigValues', function () {

    var controller = App.RMHighAvailabilityWizardStep3Controller.create({
        content: {
          rmHosts: {
            currentRM: 'h0',
            additionalRM: 'h1'
          }
        }
      }),
      configs = {
        configs: [
          Em.Object.create({
            name: 'yarn.resourcemanager.hostname.rm1'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.hostname.rm2'
          }),
          Em.Object.create({
            name: 'yarn.resourcemanager.zk-address'
          })
        ]
      };

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find', function () {
        return [
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            host: {
              hostName: 'h2'
            }
          }),
          Em.Object.create({
            componentName: 'ZOOKEEPER_SERVER',
            host: {
              hostName: 'h3'
            }
          }),
          Em.Object.create({
            componentName: 'RESOURCEMANAGER',
            host: {
              hostName: 'h4'
            }
          })
        ];
      });
    });

    afterEach(function () {
      App.HostComponent.find.restore();
    });

    it('setting new RM properties values', function () {
      controller.setDynamicConfigValues(configs, '2181');
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm1').get('value')).to.equal('h0');
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm1').get('defaultValue')).to.equal('h0');
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm2').get('value')).to.equal('h1');
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.hostname.rm2').get('defaultValue')).to.equal('h1');
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.zk-address').get('value')).to.equal('h2:2181,h3:2181');
      expect(configs.configs.findProperty('name', 'yarn.resourcemanager.zk-address').get('defaultValue')).to.equal('h2:2181,h3:2181');
    });

  });

});
