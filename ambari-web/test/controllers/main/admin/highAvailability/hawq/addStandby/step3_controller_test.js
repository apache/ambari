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
require('controllers/main/admin/highAvailability/hawq/addStandby/step3_controller');
var testHelpers = require('test/helpers');

function getController() {
  return App.AddHawqStandbyWizardStep3Controller.create({
    content: Em.Object.create({})
  });
}
var controller;

describe('App.AddHawqStandbyWizardStep3Controller', function () {

  beforeEach(function () {
    controller = getController();
  });

  describe('#isSubmitDisabled', function () {

    var cases = [
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

    it('should send proper ajax request', function () {
      controller.loadConfigTagsSuccessCallback({
        'Clusters': {
          'desired_configs': {
            'hawq-site': {
              'tag': 1
            }
          }
        }
      }, {}, {
        'serviceConfig': {}
      });
      var data = testHelpers.findAjaxRequest('name', 'reassign.load_configs')[0].data;
      expect(data.urlParams).to.equal('(type=hawq-site&tag=1)');
      expect(data.serviceConfig).to.eql({});
    });

  });

  describe('#loadConfigsSuccessCallback', function () {

    var cases = [
        {
          'title': 'should set properties from load config success callback',
          'items': [
            {
              'type': 'hawq-site',
              'properties': {
                'hawq_master_address_host' : 'h0'
              }
            }
          ],
          'params': {
            'serviceConfig': {}
          }
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
        expect(controller.get('selectedService')).to.eql({});
        expect(controller.get('isLoaded')).to.be.true;
      });
    });

  });

  describe('#loadConfigsSuccessCallback=loadConfigsErrorCallback(we have one callback for both cases)', function () {

    beforeEach(function () {
      sinon.stub(controller, 'setDynamicConfigValues', Em.K);
    });

    afterEach(function () {
      controller.setDynamicConfigValues.restore();
    });

    it('should proceed with default value', function () {
      controller.loadConfigsSuccessCallback({}, {}, {});
      expect(controller.get('selectedService')).to.eql({});
      expect(controller.get('isLoaded')).to.be.true;
    });

  });

  describe('#setDynamicConfigValues', function () {

    var data = {
      items: [
        {
          type: 'hawq-site',
          properties: {
           hawq_master_address_host : 'h0'
          }
        }
      ]
    };

    var configs = {
      configs: [
        Em.Object.create({
          name: 'hawq_standby_address_host'
        })
      ]
    };


    beforeEach(function () {
      controller = App.AddHawqStandbyWizardStep3Controller.create({
        content: Em.Object.create({
          masterComponentHosts: [
            {component: 'HAWQMASTER', hostName: 'h0', isInstalled: true},
            {component: 'HAWQSTANDBY', hostName: 'h1', isInstalled: false}
          ],
          slaveComponentHosts: [],
          hosts: {},
          hawqHost: {
            hawqMaster: 'h0',
            newHawqStandby: 'h1'
          }
        })
      });
      controller.setDynamicConfigValues(configs, data);
    });

    it('hawq_standby_address_host value', function () {
      expect(configs.configs.findProperty('name', 'hawq_standby_address_host').get('value')).to.equal('h1');
    });
    it('hawq_standby_address_host recommendedValue', function () {
      expect(configs.configs.findProperty('name', 'hawq_standby_address_host').get('recommendedValue')).to.equal('h1');
    });
  });

});
