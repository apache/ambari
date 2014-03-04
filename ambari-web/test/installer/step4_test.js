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
    'HDFS', 'MAPREDUCE', 'NAGIOS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SCOOP', 'ZOOKEEPER', 'HCATALOG',
    'WEBHCAT', 'YARN', 'MAPREDUCE2', 'FALCON', 'TEZ', 'STORM'
  ];

  var controller = App.WizardStep4Controller.create();
  services.forEach(function(serviceName, index){
    controller.pushObject(Ember.Object.create({
      'serviceName':serviceName, 'isSelected': true, 'canBeSelected': true, 'isInstalled': false, 'isDisabled': 'HDFS' === serviceName
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
    beforeEach(function() {
      ajax_send = App.ajax.send;
      App.ajax.send = function() {};
    });

    afterEach(function() {
      App.ajax.send = ajax_send;
    });
    var originalStackVersion = App.get('currentStackVersion');

    it('should return false if ZOOKEEPER is selected and Hadoop version above 2', function () {
      App.set('currentStackVersion', 'HDP-2.1.1');
      controller.findProperty('serviceName', 'ZOOKEEPER').set('isSelected', true);
      expect(controller.needToAddZooKeeper()).to.equal(false);
    });
    it('should return true if ZOOKEEPER is not selected and Hadoop version above 2', function () {
      controller.findProperty('serviceName', 'ZOOKEEPER').set('isSelected', false);
      expect(controller.needToAddZooKeeper()).to.equal(true);
    });
    it('should return false if none of the HBASE, HIVE, WEBHCAT, STORM is selected and Hadoop version below 2', function () {
      App.set('currentStackVersion', 'HDP-1.3.0');
      expect(controller.needToAddZooKeeper()).to.equal(false);
    });
    it('should return true if HBASE is not selected and Hadoop version below 2', function () {
      controller.findProperty('serviceName', 'HBASE').set('isSelected', true);
      expect(controller.needToAddZooKeeper()).to.equal(true);
    });
    it('should return true if HBASE, HIVE, WEBHCAT, STORM are selected and Hadoop version below 2', function () {
      controller.findProperty('serviceName', 'HIVE').set('isSelected', true);
      controller.findProperty('serviceName', 'WEBHCAT').set('isSelected', true);
      controller.findProperty('serviceName', 'STORM').set('isSelected', true);
      expect(controller.needToAddZooKeeper()).to.equal(true);
      App.set('currentStackVersion', originalStackVersion);
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

  describe('#needToAddTez()', function () {
    it('should return false if YARN is present, but not selected', function () {
      controller.findProperty('serviceName', 'YARN').set('isSelected', false);
      expect(controller.needToAddTez()).to.equal(false);
    });
    it('should return true if YARN is selected', function () {
      controller.findProperty('serviceName', 'YARN').set('isSelected', true);
      expect(controller.needToAddTez()).to.equal(true);
    });
  });

  describe('#needToAddOozie()', function () {
    it('should return false if FALCON is present, but not selected', function () {
      controller.findProperty('serviceName', 'FALCON').set('isSelected', false);
      expect(controller.needToAddOozie()).to.equal(false);
    });
    it('should return true if FALCON is selected', function () {
      controller.findProperty('serviceName', 'FALCON').set('isSelected', true);
      expect(controller.needToAddOozie()).to.equal(true);
    });
  });

  describe('#noDFSs()', function () {
    it('should return true if HDFS is not selected and GLUSTERFS is absent', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.noDFSs()).to.equal(true);
    });
    it('should return false if HDFS is selected and GLUSTERFS is absent', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', true);
      expect(controller.noDFSs()).to.equal(false);
    });
    it('should return true if HDFS is not selected and GLUSTERFS is not selected, but present', function () {
      controller.pushObject(Ember.Object.create({
        'serviceName':'GLUSTERFS', 'isSelected': false, 'canBeSelected': true, 'isInstalled': false, 'isDisabled': false
      }));
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.noDFSs()).to.equal(true);
    });
    it('should return false if HDFS is not selected and GLUSTERFS is selected', function () {
      controller.findProperty('serviceName', 'GLUSTERFS').set('isSelected', true);
      expect(controller.noDFSs()).to.equal(false);
    });
  });

  describe('#multipleDFSs()', function () {
    it('should return true if HDFS is selected and GLUSTERFS is selected', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', true);
      controller.findProperty('serviceName', 'GLUSTERFS').set('isSelected', true);
      expect(controller.multipleDFSs()).to.equal(true);
    });
    it('should return false if HDFS is not selected and GLUSTERFS is selected', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.multipleDFSs()).to.equal(false);
    });
    it('should return false if HDFS is selected and GLUSTERFS is not selected', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', true);
      controller.findProperty('serviceName', 'GLUSTERFS').set('isSelected', false);
      expect(controller.multipleDFSs()).to.equal(false);
    });
  });

  describe('#checkDependencies()', function () {
    var testCases = [
      {
        title: 'should set HCATALOG and WEBHCAT isSelected to true when HIVE is selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': true,
          'HCATALOG': true,
          'WEBHCAT': true
        },
        result: {
          'HCATALOG': true,
          'WEBHCAT': true
        }
      },
      {
        title: 'should set HCATALOG and WEBHCAT isSelected to false when HIVE is not selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': false,
          'HCATALOG': true,
          'WEBHCAT': true
        },
        result: {
          'HCATALOG': false,
          'WEBHCAT': false
        }
      },
      {
        title: 'should set MAPREDUCE2 isSelected to true when YARN is selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': false,
          'HCATALOG': true,
          'WEBHCAT': true,
          'YARN': true,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': true,
          'HCATALOG': false,
          'WEBHCAT': false
        }
      },
      {
        title: 'should set MAPREDUCE2 isSelected to false when YARN is not selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': true,
          'HCATALOG': true,
          'WEBHCAT': true,
          'YARN': false,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': false,
          'HCATALOG': true,
          'WEBHCAT': true
        }
      }
    ];

    testCases.forEach(function(testCase){
      it(testCase.title, function () {
        controller.clear();
        for(var id in testCase.condition) {
          controller.pushObject(Ember.Object.create({
            'serviceName':id, 'isSelected': testCase.condition[id], 'canBeSelected': true, 'isInstalled': false
          }));
        }
        controller.checkDependencies();
        for(var service in testCase.result) {
          expect(controller.findProperty('serviceName', service).get('isSelected')).to.equal(testCase.result[service]);
        }
      });
    }, this);
  });

});