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

describe('App.KerberosWizardStep2Controller', function() {

  describe('#createKerberosSiteObj', function() {
    var controller;

    beforeEach(function() {
      sinon.stub(App, 'get').withArgs('stackVersion').returns('HDP-2.3');
      controller = App.KerberosWizardStep2Controller.create({});
      sinon.stub(controller, 'tweakKdcTypeValue', Em.K);
      sinon.stub(controller, 'tweakManualKdcProperties', Em.K);
    });

    after(function() {
      App.get.restore();
      controller.tweakKdcTypeValue.restore();
      controller.tweakManualKdcProperties.restore();
    });

    var _createProperty = function(name, value) {
      var preDefProp = App.config.get('preDefinedSiteProperties').findProperty('name', name);
      if (preDefProp) {
        return App.ServiceConfigProperty.create(
          $.extend(true, {}, preDefProp, {
            value: value, filename: 'some-site.xml',
            isRequiredByAgent: preDefProp.isRequiredByAgent == undefined ? true : preDefProp.isRequiredByAgent
          }));
      } else {
        return App.ServiceConfigProperty.create({name: name, value: value, isRequiredByAgent: true});
      }
    };

    var tests = [
      {
        stepConfigs: [
          ['realm', ' SPACES '],
          ['admin_server_host', ' space_left'],
          ['kdc_host', ' space_left_and_right '],
          ['ldap_url', 'space_right ']
        ],
        e: {
          realm: 'SPACES',
          admin_server_host: 'space_left',
          kdc_host: 'space_left_and_right',
          ldap_url: 'space_right'
        }
      }
    ];

    tests.forEach(function(test) {
      it('Should trim values for properties ' + Em.keys(test.e).join(','), function() {
        controller.set('stepConfigs', [
          App.ServiceConfig.create({
            configs: test.stepConfigs.map(function(item) { return _createProperty(item[0], item[1]); })
          })
        ]);
        var result = controller.createKerberosSiteObj('some-site', 'random-tag');
        Em.keys(test.e).forEach(function(propertyName) {
          expect(result.properties[propertyName]).to.be.eql(test.e[propertyName]);
        });
      });
    });
  });
});
