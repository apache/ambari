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
require('controllers/main/host/configs_service');


describe('App.MainHostServiceConfigsController', function () {

  var controller = App.MainHostServiceConfigsController.create({
    host: Em.Object.create()
  });


  describe('#filterServiceConfigs()', function () {
    var testCases = [
      {
        title: 'configCategories is empty',
        content: {
          configCategories: [],
          hostComponents: []
        },
        result: []
      },
      {
        title: 'Category hostComponentNames is null',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: null})
          ],
          hostComponents: []
        },
        result: [
          Em.Object.create({hostComponentNames: null})
        ]
      },
      {
        title: 'Components of host are empty',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: ['comp1']})
          ],
          hostComponents: []
        },
        result: []
      },
      {
        title: 'Host components do not match component of categories',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: ['comp1']})
          ],
          hostComponents: [
            {
              componentName: 'comp2'
            }
          ]
        },
        result: []
      },
      {
        title: 'Host components match component of categories',
        content: {
          configCategories: [
            Em.Object.create({hostComponentNames: ['comp1']})
          ],
          hostComponents: [
            {
              componentName: 'comp1'
            }
          ]
        },
        result: [
          Em.Object.create({hostComponentNames: ['comp1']})
        ]
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('host.hostComponents', test.content.hostComponents);
        expect(controller.filterServiceConfigs(test.content.configCategories)).to.eql(test.result);
      });
    });
  });

  describe('#constructUrlParams()', function () {
    it('loadedGroupToOverrideSiteToTagMap is empty', function () {
      var loadedGroupToOverrideSiteToTagMap = {};
      var configGroups = [];
      expect(controller.constructUrlParams(loadedGroupToOverrideSiteToTagMap, configGroups)).to.eql([]);
      expect(controller.get('typeTagToGroupMap')).to.eql({});
    });
    it('Group does not have hosts', function () {
      var loadedGroupToOverrideSiteToTagMap = {'group1': {}};
      var configGroups = [
        Em.Object.create({
          name: 'group1',
          hosts: []
        })
      ];
      expect(controller.constructUrlParams(loadedGroupToOverrideSiteToTagMap, configGroups)).to.eql([]);
      expect(controller.get('typeTagToGroupMap')).to.eql({});
    });
    it('Group does not contain current host', function () {
      controller.set('host.hostName', 'host2');
      var loadedGroupToOverrideSiteToTagMap = {'group1': {}};
      var configGroups = [
        Em.Object.create({
          name: 'group1',
          hosts: ['host1']
        })
      ];
      expect(controller.constructUrlParams(loadedGroupToOverrideSiteToTagMap, configGroups)).to.eql([]);
      expect(controller.get('typeTagToGroupMap')).to.eql({});
    });
    it('No type to tags relations in group', function () {
      var loadedGroupToOverrideSiteToTagMap = {'group1': {}};
      var configGroups = [
        Em.Object.create({
          name: 'group1',
          hosts: ['host2']
        })
      ];
      expect(controller.constructUrlParams(loadedGroupToOverrideSiteToTagMap, configGroups)).to.eql([]);
      expect(controller.get('typeTagToGroupMap')).to.eql({});
    });
    it('Input params are correct', function () {
      var loadedGroupToOverrideSiteToTagMap = {
        'group1': {
          'type1': 'tag1'
        }
      };
      var configGroups = [
        Em.Object.create({
          name: 'group1',
          hosts: ['host2']
        })
      ];
      expect(controller.constructUrlParams(loadedGroupToOverrideSiteToTagMap, configGroups)).to.eql(['(type=type1&tag=tag1)']);
      expect(controller.get('typeTagToGroupMap')['type1///tag1'].get('name')).to.equal('group1');
    });
  });

  describe('#loadServiceConfigHostsOverrides()', function () {

    beforeEach(function () {
      sinon.stub(controller, "constructUrlParams", function(){
        return controller.get('testUrlParams');
      });
      sinon.spy(App.ajax, "send");
    });
    afterEach(function () {
      controller.constructUrlParams.restore();
      App.ajax.send.restore();
    });

    it('configKeyToConfigMap and urlParams are empty', function () {
      var serviceConfigs = [];
      controller.set('testUrlParams', []);
      controller.loadServiceConfigHostsOverrides(serviceConfigs);
      expect(controller.get('configKeyToConfigMap')).to.eql({});
      expect(App.ajax.send.called).to.be.false;
    });

    it('configKeyToConfigMap and urlParams are correct', function () {
      var serviceConfigs = [{
        name: 'config1'
      }];
      controller.set('testUrlParams', ['params']);
      controller.loadServiceConfigHostsOverrides(serviceConfigs);
      expect(controller.get('configKeyToConfigMap')).to.eql({'config1': {
        name: 'config1'
      }});
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('#loadServiceConfigHostsOverridesSuccessCallback()', function () {

    beforeEach(function () {
      sinon.spy(App.config, "handleSpecialProperties");
    });
    afterEach(function () {
      App.config.handleSpecialProperties.restore();
    });
    it('data.items is empty', function () {
      var data = {
        items: [
          {
            type: 'type1',
            tag: 'tag1',
            properties: {'prop1': 'value1'}
          }
        ]
      };
      controller.set('typeTagToGroupMap', {'type1///tag1': {}});
      controller.set('configKeyToConfigMap', {'prop1': {}});
      controller.loadServiceConfigHostsOverridesSuccessCallback(data);
      expect(App.config.handleSpecialProperties.calledWith({
        value: 'value1',
        isOriginalSCP: false,
        group: {}
      })).to.be.true;
    });
  });
});
