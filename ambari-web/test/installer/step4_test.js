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

describe('App.WizardStep4Controller', function () {

  var services = [
    'HDFS', 'MAPREDUCE', 'NAGIOS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SCOOP', 'ZOOKEEPER', 'HCATALOG', 'WEBHCAT', 'YARN', 'MAPREDUCE2'
  ];

  var controller = App.WizardStep4Controller.create();
  services.forEach(function(serviceName, index){
    controller.pushObject(Ember.Object.create({
      'serviceName':serviceName, 'isSelected': true, 'canBeSelected': true, 'isInstalled': false, 'isDisabled': index == 0
    }));
  });

  describe('#isSubmitDisabled', function () {
    it('should return false if at least one selected service is not installed', function () {
      expect(controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should return true if all selected services are already installed', function () {
      controller.setEach('isInstalled', true);
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.get('isSubmitDisabled')).to.equal(true);
    });
  });

  describe('#isAll', function () {
    it('should return true if all services are selected', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', true);
      expect(controller.get('isAll')).to.equal(true);
    });

    it('should return false if at least one service is not selected', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.get('isAll')).to.equal(false);
    });
  });

  describe('#isMinimum', function () {
    it('should return true if there are no services selected, except disabled', function () {
      controller.setEach('isSelected', false);
      expect(controller.get('isMinimum')).to.equal(true);
    });

    it('should return false if at least one service is selected, except disabled', function () {
      controller.findProperty('serviceName', 'MAPREDUCE').set('isSelected', true);
      expect(controller.get('isMinimum')).to.equal(false);
    });
  });

  describe('#checkDependencies()', function () {
    /*it('should set ZooKeeper isSelected property like in HBase', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'HBASE').set('isSelected', true);
      controller.checkDependencies();
      expect(controller.findProperty('serviceName', 'ZOOKEEPER').get('isSelected')).to.equal(true);
    });*/
    it('should set ZooKeeper, HCatalog, WebHCatalog isSelected property like in Hive', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
      controller.checkDependencies();
      expect(controller.findProperty('serviceName', 'HCATALOG').get('isSelected')).to.equal(true);
      expect(controller.findProperty('serviceName', 'WEBHCAT').get('isSelected')).to.equal(true);
    });
    it('should set MapReduce2 isSelected property like in Yarn', function () {
      App.set('currentStackVersion', 'HDP-2.0.1');
      App.set('defaultStackVersion', 'HDP-2.0.1');
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'YARN').set('isSelected', true);
      controller.checkDependencies();
      expect(controller.findProperty('serviceName', 'MAPREDUCE2').get('isSelected')).to.equal(true);
      App.set('currentStackVersion', 'HDP-1.2.2');
      App.set('defaultStackVersion', 'HDP-1.2.2');
    });
  });

  describe('#selectAll()', function () {
    it('should select all services', function () {
      controller.setEach('isSelected', false);
      controller.selectAll();
      expect(controller.filterProperty('canBeSelected', true).everyProperty('isSelected', true)).to.equal(true);
    });
  });

  describe('#selectMinimum()', function () {
    it('should set isSelected false for all not disabled services', function () {
      controller.setEach('isSelected', true);
      controller.selectMinimum();
      expect(controller.findProperty('serviceName', 'HDFS').get('isSelected')).to.equal(true);
      expect(controller.filterProperty('isDisabled', false).everyProperty('isSelected', false)).to.equal(true);
    });
  });

  describe('#needToAddMapReduce()', function () {
    it('should return true if Pig is selected and MapReduce is not selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'PIG').set('isSelected', true);
      expect(controller.needToAddMapReduce()).to.equal(true);
    });

    it('should return true if Oozie is selected and MapReduce is not selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'OOZIE').set('isSelected', true);
      expect(controller.needToAddMapReduce()).to.equal(true);
    });

    it('should return true if Hive is selected and MapReduce is not selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
      expect(controller.needToAddMapReduce()).to.equal(true);
    });

    it('should return false if MapReduce is selected or Pig, Oozie and Hive are not selected', function () {
      controller.findProperty('serviceName', 'MAPREDUCE').set('isSelected', true);
      expect(controller.needToAddMapReduce()).to.equal(false);
      controller.setEach('isSelected', false);
      expect(controller.needToAddMapReduce()).to.equal(false);
    });
  });

  describe('#needToAddYarnMapReduce2()', function () {
    it('should return true if Pig is selected and YARN+MapReduce2 is not selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'PIG').set('isSelected', true);
      expect(controller.needToAddYarnMapReduce2()).to.equal(true);
    });

    it('should return true if Oozie is selected and YARN+MapReduce2 is not selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'OOZIE').set('isSelected', true);
      expect(controller.needToAddYarnMapReduce2()).to.equal(true);
    });

    it('should return true if Hive is selected and YARN+MapReduce2 is not selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
      expect(controller.needToAddYarnMapReduce2()).to.equal(true);
    });

    it('should return false if YARN+MapReduce2 is selected or Pig, Oozie and Hive are not selected', function () {
      controller.findProperty('serviceName', 'YARN').set('isSelected', true);
      expect(controller.needToAddYarnMapReduce2()).to.equal(false);
      controller.setEach('isSelected', false);
      expect(controller.needToAddYarnMapReduce2()).to.equal(false);
    });
  });

  describe('#needToAddZooKeeper()', function () {
    it('should return false if ZOOKEEPER is selected or HBASE is not selected', function () {
      controller.findProperty('serviceName', 'ZOOKEEPER').set('isSelected', true);
      expect(controller.needToAddZooKeeper()).to.equal(false);
      controller.setEach('isSelected', false);
      expect(controller.needToAddZooKeeper()).to.equal(false);
    });
  });

  describe('#gangliaOrNagiosNotSelected()', function () {
    it('should return true if Nagios or Ganglia is not selected', function () {
      controller.setEach('isSelected', true);
      controller.findProperty('serviceName', 'NAGIOS').set('isSelected', false);
      expect(controller.gangliaOrNagiosNotSelected()).to.equal(true);
      controller.setEach('isSelected', true);
      controller.findProperty('serviceName', 'GANGLIA').set('isSelected', false);
      expect(controller.gangliaOrNagiosNotSelected()).to.equal(true);
    });

    it('should return false if Nagios and Ganglia is selected', function () {
      controller.setEach('isSelected', false);
      controller.findProperty('serviceName', 'GANGLIA').set('isSelected', true);
      controller.findProperty('serviceName', 'NAGIOS').set('isSelected', true);
      expect(controller.gangliaOrNagiosNotSelected()).to.equal(false);
    });
  });

});