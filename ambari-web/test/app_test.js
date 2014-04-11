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
require('views/common/quick_view_link_view');
require('models/host_component');
require('models/stack_service_component');
var modelSetup = require('test/init_model_test');

describe('#App', function() {

  describe('App.isHadoop21Stack', function() {
    var tests = [{
      v:'',
      e:false
    }, {
      v:'HDP',
      e: false
    }, {
      v:'HDP1',
      e: false
    }, {
      v:'HDP-1',
      e: false
    }, {
      v:'HDP-2.0',
      e: false
    }, {
      v:'HDP-2.0.1000',
      e: false
    }, {
      v:'HDP-2.1',
      e: true
    }, {
      v:'HDP-2.1.3434',
      e: true
    }, {
      v:'HDP-2.2',
      e: true
    }, {
      v:'HDP-2.2.1212',
      e: true
    }];
    tests.forEach(function(test){
      it(test.v, function() {
        App.QuickViewLinks.prototype.setQuickLinks = function(){};
        App.set('currentStackVersion', test.v);
        var calculated = App.get('isHadoop21Stack');
        var expected = test.e;
        expect(calculated).to.equal(expected);
      });
    });
  });

  describe('Disable/enable components', function() {

    var testableComponent =  Em.Object.create({
      componentName: 'APP_TIMELINE_SERVER',
      serviceName: 'YARN'
    });
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
    var reviewConfigs = require('data/review_configs');
    var disableResult;

    App.set('currentStackVersion', 'HDP-2.1');
    App.set('handleStackDependencyTest', true);

    describe('#disableComponent()', function() {
      disableResult = App.disableComponent(testableComponent);
      // copy
      var _globalProperties = $.extend({}, globalProperties);
      var _siteProperties = $.extend({}, siteProperties);
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


        it('should remove global properties of component', function() {
          expect(_globalProperties.configProperties.mapProperty('name')).to.not.include.members(expectedInfo.properties.global_properties);
        });

        it('should remove site properties of component', function() {
          expect(_siteProperties.configProperties.mapProperty('name')).to.not.include.members(expectedInfo.properties.site_properties);
        });

        it('should remove review config for component', function() {
          var reviewConfig = _reviewConfigs.findProperty('config_name', 'services')
            .config_value.findProperty('service_name', testableComponent.get('serviceName'))
            .service_components.mapProperty('component_name');
          expect(reviewConfig).to.not.include(expectedInfo.reviewConfigs.component_name);
        });
      });
    });

    describe('#enableComponent', function() {
      App.enableComponent(disableResult);

      it('should add global properties of component', function() {
        expect(globalProperties.configProperties.mapProperty('name')).to.include.members(expectedInfo.properties.global_properties);
      });

      it('should add site properties of component', function() {
        expect(siteProperties.configProperties.mapProperty('name')).to.include.members(expectedInfo.properties.site_properties);
      });

      it('should add review config for component', function() {
        var reviewConfig = reviewConfigs.findProperty('config_name', 'services')
          .config_value.findProperty('service_name', testableComponent.get('serviceName'))
          .get('service_components').mapProperty('component_name');
        expect(reviewConfig).to.include(expectedInfo.reviewConfigs.component_name);
      });
    });
  });

  describe('#stackVersionURL', function () {

    App.QuickViewLinks.reopen({
      loadTags: function () {}
    });

    var testCases = [
      {
        title: 'if currentStackVersion and defaultStackVersion are empty then stackVersionURL should contain prefix',
        currentStackVersion: '',
        defaultStackVersion: '',
        result: '/stacks/HDP/version/'
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then stackVersionURL should be "/stacks/HDP/version/1.3.1"',
        currentStackVersion: 'HDP-1.3.1',
        defaultStackVersion: '',
        result: '/stacks/HDP/version/1.3.1'
      },
      {
        title: 'if defaultStackVersion is "HDP-1.3.1" then stackVersionURL should be "/stacks/HDP/version/1.3.1"',
        currentStackVersion: '',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks/HDP/version/1.3.1'
      },
      {
        title: 'if defaultStackVersion and currentStackVersion are different then stackVersionURL should have currentStackVersion value',
        currentStackVersion: 'HDP-1.3.2',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks/HDP/version/1.3.2'
      },
      {
        title: 'if defaultStackVersion is "HDPLocal-1.3.1" then stackVersionURL should be "/stacks/HDPLocal/version/1.3.1"',
        currentStackVersion: '',
        defaultStackVersion: 'HDPLocal-1.3.1',
        result: '/stacks/HDPLocal/version/1.3.1'
      },
      {
        title: 'if currentStackVersion is "HDPLocal-1.3.1" then stackVersionURL should be "/stacks/HDPLocal/version/1.3.1"',
        currentStackVersion: 'HDPLocal-1.3.1',
        defaultStackVersion: '',
        result: '/stacks/HDPLocal/version/1.3.1'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('currentStackVersion', test.currentStackVersion);
        App.set('defaultStackVersion', test.defaultStackVersion);
        expect(App.get('stackVersionURL')).to.equal(test.result);
        App.set('currentStackVersion', "HDP-1.2.2");
        App.set('defaultStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#stack2VersionURL', function () {

    var testCases = [
      {
        title: 'if currentStackVersion and defaultStackVersion are empty then stack2VersionURL should contain prefix',
        currentStackVersion: '',
        defaultStackVersion: '',
        result: '/stacks2/HDP/versions/'
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then stack2VersionURL should be "/stacks2/HDP/versions/1.3.1"',
        currentStackVersion: 'HDP-1.3.1',
        defaultStackVersion: '',
        result: '/stacks2/HDP/versions/1.3.1'
      },
      {
        title: 'if defaultStackVersion is "HDP-1.3.1" then stack2VersionURL should be "/stacks/HDP/versions/1.3.1"',
        currentStackVersion: '',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks2/HDP/versions/1.3.1'
      },
      {
        title: 'if defaultStackVersion and currentStackVersion are different then stack2VersionURL should have currentStackVersion value',
        currentStackVersion: 'HDP-1.3.2',
        defaultStackVersion: 'HDP-1.3.1',
        result: '/stacks2/HDP/versions/1.3.2'
      },
      {
        title: 'if defaultStackVersion is "HDPLocal-1.3.1" then stack2VersionURL should be "/stacks2/HDPLocal/versions/1.3.1"',
        currentStackVersion: '',
        defaultStackVersion: 'HDPLocal-1.3.1',
        result: '/stacks2/HDPLocal/versions/1.3.1'
      },
      {
        title: 'if currentStackVersion is "HDPLocal-1.3.1" then stack2VersionURL should be "/stacks2/HDPLocal/versions/1.3.1"',
        currentStackVersion: 'HDPLocal-1.3.1',
        defaultStackVersion: '',
        result: '/stacks2/HDPLocal/versions/1.3.1'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('currentStackVersion', test.currentStackVersion);
        App.set('defaultStackVersion', test.defaultStackVersion);
        expect(App.get('stack2VersionURL')).to.equal(test.result);
        App.set('currentStackVersion', "HDP-1.2.2");
        App.set('defaultStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#currentStackVersionNumber', function () {

    var testCases = [
      {
        title: 'if currentStackVersion is empty then currentStackVersionNumber should be empty',
        currentStackVersion: '',
        result: ''
      },
      {
        title: 'if currentStackVersion is "HDP-1.3.1" then currentStackVersionNumber should be "1.3.1',
        currentStackVersion: 'HDP-1.3.1',
        result: '1.3.1'
      },
      {
        title: 'if currentStackVersion is "HDPLocal-1.3.1" then currentStackVersionNumber should be "1.3.1',
        currentStackVersion: 'HDPLocal-1.3.1',
        result: '1.3.1'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('currentStackVersion', test.currentStackVersion);
        expect(App.get('currentStackVersionNumber')).to.equal(test.result);
        App.set('currentStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#isHadoop2Stack', function () {

    var testCases = [
      {
        title: 'if currentStackVersion is empty then isHadoop2Stack should be false',
        currentStackVersion: '',
        result: false
      },
      {
        title: 'if currentStackVersion is "HDP-1.9.9" then isHadoop2Stack should be false',
        currentStackVersion: 'HDP-1.9.9',
        result: false
      },
      {
        title: 'if currentStackVersion is "HDP-2.0.0" then isHadoop2Stack should be true',
        currentStackVersion: 'HDP-2.0.0',
        result: true
      },
      {
        title: 'if currentStackVersion is "HDP-2.0.1" then isHadoop2Stack should be true',
        currentStackVersion: 'HDP-2.0.1',
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        App.set('currentStackVersion', test.currentStackVersion);
        expect(App.get('isHadoop2Stack')).to.equal(test.result);
        App.set('currentStackVersion', "HDP-1.2.2");
      });
    });
  });

  describe('#isHaEnabled', function () {

    it('if hadoop stack version less than 2 then isHaEnabled should be false', function () {
      App.set('currentStackVersion', 'HDP-1.3.1');
      expect(App.get('isHaEnabled')).to.equal(false);
      App.set('currentStackVersion', "HDP-1.2.2");
    });
    it('if hadoop stack version higher than 2 then isHaEnabled should be true', function () {
      App.set('currentStackVersion', 'HDP-2.0.1');
      expect(App.get('isHaEnabled')).to.equal(true);
      App.set('currentStackVersion', "HDP-1.2.2");
    });
    it('if cluster has SECONDARY_NAMENODE then isHaEnabled should be false', function () {
      App.store.load(App.HostComponent, {
        id: 'SECONDARY_NAMENODE',
        component_name: 'SECONDARY_NAMENODE'
      });
      App.set('currentStackVersion', 'HDP-2.0.1');
      expect(App.get('isHaEnabled')).to.equal(false);
      App.set('currentStackVersion', "HDP-1.2.2");
    });
  });

  describe('#handleStackDependedComponents()', function () {

    beforeEach(function(){
      modelSetup.setupStackServiceComponent();
    });

    afterEach(function(){
      modelSetup.cleanStackServiceComponent();
    });

    it('if handleStackDependencyTest is true then stackDependedComponents should be unmodified', function () {
      App.set('testMode', false);
      App.set('handleStackDependencyTest', true);
      App.handleStackDependedComponents();
      expect(App.get('stackDependedComponents')).to.be.empty;
    });

    it('if testMode is true then stackDependedComponents should be unmodified', function () {
      App.set('handleStackDependencyTest', false);
      App.set('testMode', true);
      App.handleStackDependedComponents();
      expect(App.get('stackDependedComponents')).to.be.empty;
    });

    it('if stack contains all components then stackDependedComponents should be empty', function () {
      App.set('testMode', false);
      App.set('handleStackDependencyTest', false);
      App.handleStackDependedComponents();
      expect(App.get('stackDependedComponents')).to.be.empty;
    });

    it('if stack is missing component then push it to stackDependedComponents', function () {
      App.set('testMode', false);
      App.set('handleStackDependencyTest', false);
      var dtRecord = App.StackServiceComponent.find('DATANODE');
      dtRecord.deleteRecord();
      dtRecord.get('stateManager').transitionTo('loading');
      App.handleStackDependedComponents();
      expect(App.get('stackDependedComponents').mapProperty('componentName')).to.eql(["DATANODE"]);
      App.store.load(App.StackServiceComponent, {
        id: 'DATANODE',
        component_name: 'DATANODE',
        service_name: 'HDFS',
        component_category: 'SLAVE',
        is_master: false,
        is_client: false,
        stack_name: 'HDP',
        stack_version: '2.1'
      });
    });

    it('remove stack components from stackDependedComponents', function () {
      App.set('testMode', false);
      App.set('handleStackDependencyTest', false);
      App.set('stackDependedComponents', [
        Em.Object.create({
          componentName: "DATANODE",
          serviceName: "HDFS",
          properties: {},
          reviewConfigs: {},
          configCategory: {}
        }),
        Em.Object.create({
          componentName: "categoryComponent",
          serviceName: "",
          properties: {},
          reviewConfigs: {},
          configCategory: {}
        })
      ]);
      App.handleStackDependedComponents();
      expect(App.get('stackDependedComponents').mapProperty('componentName')).to.eql(["categoryComponent"]);
    });
  });
});
