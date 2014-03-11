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

describe('#App', function() {

  describe('App.components', function() {

    it('slaves and masters should not intersect', function() {
      var intersected = App.get('components.slaves').filter(function(item){
        return App.get('components.masters').contains(item);
      });
      expect(intersected).to.eql([]);
    });

    it('decommissionAllowed', function() {
      expect(App.get('components.decommissionAllowed')).to.eql(["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER"]);
    });

    it('addableToHost', function() {
      expect(App.get('components.addableToHost')).to.eql(["DATANODE", "TASKTRACKER", "NODEMANAGER", "HBASE_REGIONSERVER", "HBASE_MASTER", "ZOOKEEPER_SERVER", "SUPERVISOR"]);
    });

  });

  describe('Disable/enable components', function() {
    var testableComponent =  {
      service_name: 'YARN',
      component_name: 'APP_TIMELINE_SERVER'
    };
    var expectedInfo = {
      componentName: 'APP_TIMELINE_SERVER',
      properties: {
        global_properties: ['ats_host', 'apptimelineserver_heapsize'],
        site_properties: ['yarn.timeline-service.generic-application-history.store-class', 'yarn.timeline-service.store-class', 'yarn.timeline-service.leveldb-timeline-store.path']
      },
      reviewConfigs: {
        component_name: 'APP_TIMELINE_SERVER'
      },
      configCategory: {
        name: 'AppTimelineServer'
      }
    };
    var globalProperties = require('data/HDP2/global_properties');
    var siteProperties = require('data/HDP2/site_properties');
    var serviceComponents = require('data/service_components');
    var reviewConfigs = require('data/review_configs');
    var disableResult;

    App.set('currentStackVersion', 'HDP-2.1');
    App.set('handleStackDependencyTest', true);

    describe('#disableComponent()', function() {
      disableResult = App.disableComponent(testableComponent);
      // copy
      var _globalProperties = $.extend({}, globalProperties);
      var _siteProperties = $.extend({}, siteProperties);
      var _serviceComponents = $.extend({}, serviceComponents);
      var _reviewConfigs = JSON.parse(JSON.stringify(reviewConfigs));

      describe('result validation', function() {

        it('component name should be "' + expectedInfo.componentName + '"', function() {
          expect(disableResult.get('componentName')).to.eql(expectedInfo.componentName);
        });

        it('config category name should be "' + expectedInfo.configCategory.name +'"', function() {
          expect(disableResult.get('configCategory.name')).to.eql(expectedInfo.configCategory.name);
        });

        for(var siteName in expectedInfo.properties) {
          (function(site) {
            expectedInfo.properties[site].forEach(function(property) {
              it(property + ' present in ' + site, function() {
                expect(disableResult.get('properties.' + site).mapProperty('name')).to.include(property);
              });
            }, this);
          })(siteName);
        }

        it('site and global properties should not be equal', function() {
          expect(disableResult.get('properties.global_properties')).to.not.include.members(disableResult.get('properties.site_properties'));
        });


      });

      describe('effect validation',function() {

        it('should remove component from service_components object', function() {
          expect(_serviceComponents.findProperty('component_name', testableComponent.component_name)).to.be.undefined;
        });

        it('should remove global properties of component', function() {
          expect(_globalProperties.configProperties.mapProperty('name')).to.not.include.members(expectedInfo.properties.global_properties);
        });

        it('should remove site properties of component', function() {
          expect(_siteProperties.configProperties.mapProperty('name')).to.not.include.members(expectedInfo.properties.site_properties);
        });

        it('should remove review config for component', function() {
          var reviewConfig = _reviewConfigs.findProperty('config_name', 'services')
            .config_value.findProperty('service_name', testableComponent.service_name)
            .service_components.mapProperty('component_name');
          expect(reviewConfig).to.not.include(expectedInfo.reviewConfigs.component_name);
        });
      });
    });

    describe('#enableComponent', function() {
      App.enableComponent(disableResult);

      it('should add component to service_components object', function() {
        expect(serviceComponents.findProperty('component_name', testableComponent.component_name)).to.exist;
      });

      it('should add global properties of component', function() {
        expect(globalProperties.configProperties.mapProperty('name')).to.include.members(expectedInfo.properties.global_properties);
      });

      it('should add site properties of component', function() {
        expect(siteProperties.configProperties.mapProperty('name')).to.include.members(expectedInfo.properties.site_properties);
      });

      it('should add review config for component', function() {
        var reviewConfig = reviewConfigs.findProperty('config_name', 'services')
          .config_value.findProperty('service_name', testableComponent.service_name)
          .get('service_components').mapProperty('component_name');
        expect(reviewConfig).to.include(expectedInfo.reviewConfigs.component_name);
      });
    });
  });

});
