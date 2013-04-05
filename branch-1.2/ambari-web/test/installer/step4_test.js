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

var Ember = require('ember');
var App = require('app');
require('controllers/wizard/step4_controller');

/*
describe('App.InstallerStep4Controller', function () {

  var DEFAULT_SERVICES = ['HDFS'];
  var OPTIONAL_SERVICES = ['MAPREDUCE', 'NAGIOS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SQOOP', 'ZOOKEEPER', 'HCATALOG'];

  var controller = App.InstallerStep4Controller.create();
  controller.rawContent.forEach(function(item){
    item.isSelected = true;
    controller.pushObject(Ember.Object.create(item));
    });

  describe('#selectMinimum()', function () {
    it('should set isSelected is false on all non-default services and isSelected is true on all default services', function() {
      controller.selectMinimum();
      DEFAULT_SERVICES.forEach(function (serviceName) {
        expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(true);
      });
      OPTIONAL_SERVICES.forEach(function (serviceName) {
        expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(false);
      });
    })
  })

  describe('#selectAll()', function () {
    it('should set isSelected is true on all non-default services and isSelected is true on all default services', function() {
      controller.selectAll();
      DEFAULT_SERVICES.forEach(function (serviceName) {
        expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(true);
      });
      OPTIONAL_SERVICES.forEach(function (serviceName) {
        expect(controller.findProperty('serviceName', serviceName).get('isSelected')).to.equal(true);
      });
    })
  })

  describe('#isAll()', function () {

    beforeEach(function() {
      DEFAULT_SERVICES.forEach(function(serviceName) {
        controller.findProperty('serviceName', serviceName).set('isSelected', true);
      });
      OPTIONAL_SERVICES.forEach(function(serviceName) {
        controller.findProperty('serviceName', serviceName).set('isSelected', true);
      });
    });

    it('should return true if isSelected is true for all services', function() {
      expect(controller.get('isAll')).to.equal(true);
    })

    it('should return false if isSelected is false for one of the services', function() {
      controller.findProperty('serviceName', 'HBASE').set('isSelected', false);
      expect(controller.get('isAll')).to.equal(false);
    })
  })

  describe('#isMinimum()', function () {

    beforeEach(function() {
      DEFAULT_SERVICES.forEach(function(serviceName) {
        controller.findProperty('serviceName', serviceName).set('isSelected', true);
      });
      OPTIONAL_SERVICES.forEach(function(serviceName) {
        controller.findProperty('serviceName', serviceName).set('isSelected', false);
      });
    });

    it('should return true if isSelected is true for all default services and isSelected is false for all optional services', function() {
      expect(controller.get('isMinimum')).to.equal(true);
    })

    it('should return false if isSelected is true for all default serices and isSelected is true for one of optional services', function() {
      controller.findProperty('serviceName', 'HBASE').set('isSelected', true);
      expect(controller.get('isMinimum')).to.equal(false);
    })

  })

  describe('#needToAddMapReduce', function() {

    describe('mapreduce not selected', function() {
      beforeEach(function() {
        controller.findProperty('serviceName', 'MAPREDUCE').set('isSelected', false);
      })

      it('should return true if Hive is selected and MapReduce is not selected', function() {
        controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
        expect(controller.needToAddMapReduce()).to.equal(true);
      })
      it('should return true if Pig is selected and MapReduce is not selected', function() {
        controller.findProperty('serviceName', 'PIG').set('isSelected', true);
        expect(controller.needToAddMapReduce()).to.equal(true);
      })
      it('should return true if Oozie is selected and MapReduce is not selected', function() {
        controller.findProperty('serviceName', 'OOZIE').set('isSelected', true);
        expect(controller.needToAddMapReduce()).to.equal(true);
      })
    })

    describe('mapreduce not selected', function() {
      beforeEach(function() {
        controller.findProperty('serviceName', 'MAPREDUCE').set('isSelected', true);
      })

      it('should return false if Hive is selected and MapReduce is selected', function() {
        controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
        expect(controller.needToAddMapReduce()).to.equal(false);
      })
      it('should return false if Pig is selected and MapReduce is not selected', function() {
        controller.findProperty('serviceName', 'PIG').set('isSelected', true);
        expect(controller.needToAddMapReduce()).to.equal(false);
      })
      it('should return false if Oozie is selected and MapReduce is not selected', function() {
        controller.findProperty('serviceName', 'OOZIE').set('isSelected', true);
        expect(controller.needToAddMapReduce()).to.equal(false);
      })
    })

  })

  describe('#saveSelectedServiceNamesToDB', function() {

    beforeEach(function() {
      DEFAULT_SERVICES.forEach(function(serviceName) {
        controller.findProperty('serviceName', serviceName).set('isSelected', true);
      });
      OPTIONAL_SERVICES.forEach(function(serviceName) {
        controller.findProperty('serviceName', serviceName).set('isSelected', true);
      });
    });

    it('should store the selected service names in App.db.selectedServiceNames', function() {
      App.db.setLoginName('tester');
      App.db.setClusterName('test');
      controller.saveSelectedServiceNamesToDB();
      // console.log('controller length=' + controller.get('length'));
      var selectedServiceNames = App.db.getSelectedServiceNames();
      // console.log('service length=' + selectedServiceNames.get('length'));
      expect(selectedServiceNames.length === DEFAULT_SERVICES.length + OPTIONAL_SERVICES.length).to.equal(true);
    })

  })

})*/
