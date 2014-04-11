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
var component = require('utils/component');
require('models/host_component');
require('models/stack_service_component');

describe('utils/component', function(){
  describe('#getInstalledComponents()', function(){
    beforeEach(function(){
      App.HostComponent.find().set('content',[]);
      App.store.loadMany(App.HostComponent, [
        {
          "component_name" : "HISTORYSERVER",
          "is_client" : false,
          "is_master" : true,
          "is_slave" : false
        },
        {
          "component_name" : "TASKTRACKER",
          "is_client" : false,
          "is_master" : false,
          "is_slave" : true
        }
      ]);
    });
    afterEach(function(){
      App.HostComponent.find().set('content',[]);
    });
    it('names of components should be equal for input and output arrays', function(){
      expect(component.getInstalledComponents().mapProperty('id')).to.have.members(App.HostComponent.find().mapProperty('componentName'));
    });
  });
  describe('#loadStackServiceComponentModel()', function(){
    var data = {
      "items": [
        {
          "serviceComponents": [
            {
              "StackServiceComponents": {
                "component_category": "CLIENT",
                "component_name": "FALCON_CLIENT",
                "is_client": true,
                "is_master": false,
                "service_name": "FALCON",
                "stack_name": "HDP",
                "stack_version": "2.1"
              }
            },
            {
              "StackServiceComponents": {
                "component_category": "MASTER",
                "component_name": "FALCON_SERVER",
                "is_client": false,
                "is_master": true,
                "service_name": "FALCON",
                "stack_name": "HDP",
                "stack_version": "2.1"
              }
            }
          ]
        },
        {
          "serviceComponents": [
            {
              "StackServiceComponents": {
                "component_category": "SLAVE",
                "component_name": "GANGLIA_MONITOR",
                "is_client": false,
                "is_master": false,
                "service_name": "GANGLIA",
                "stack_name": "HDP",
                "stack_version": "2.1"
              }
            },
            {
              "StackServiceComponents": {
                "component_category": "MASTER",
                "component_name": "GANGLIA_SERVER",
                "is_client": false,
                "is_master": true,
                "service_name": "GANGLIA",
                "stack_name": "HDP",
                "stack_version": "2.1"
              }
            }
          ]
        }
      ]
    };

    afterEach(function(){
      App.StackServiceComponent.find().set('content', []);
    });

    it('should return 4 components', function(){
      expect(component.loadStackServiceComponentModel(data).items.length).to.eql(4);
    });

    it('should load data to StackServiceComponent model', function(){
      component.loadStackServiceComponentModel(data);
      expect(App.StackServiceComponent.find().get('content')).have.length(4);
    });
  });
});
