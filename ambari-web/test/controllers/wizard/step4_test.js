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
      'serviceName':serviceName, 'isSelected': true, 'canBeSelected': true, 'isInstalled': false, 'isDisabled': 'HDFS' === serviceName, isDFS: 'HDFS' === serviceName
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
    it('should set isSelected false for all services', function () {
      controller.setEach('isSelected', true);
      controller.selectMinimum();
      expect(controller.findProperty('serviceName', 'HDFS').get('isSelected')).to.equal(false);
      expect(controller.filterProperty('isDisabled', false).everyProperty('isSelected', false)).to.equal(true);
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
        'serviceName':'GLUSTERFS', 'isSelected': false, 'canBeSelected': true, 'isInstalled': false, 'isDisabled': false, 'isDFS': true
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

  describe('#setGroupedServices()', function () {
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
            'serviceName':id, 'isSelected': testCase.condition[id], 'canBeSelected': true, 'isInstalled': false,
            coSelectedServices: function() {
              return App.StackService.coSelected[this.get('serviceName')] || [];
            }.property('serviceName')
          }));
        }
        controller.setGroupedServices();
        for(var service in testCase.result) {
          expect(controller.findProperty('serviceName', service).get('isSelected')).to.equal(testCase.result[service]);
        }
      });
    }, this);
  });

  describe('#monitoringCheckPopup', function() {
    it('should show App.ModalPopup', function() {
      sinon.spy(App.ModalPopup, 'show');
      controller.monitoringCheckPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
    it('onPrimary should proceed to next step', function() {
      sinon.stub(App.router, 'send', Em.K);
      controller.monitoringCheckPopup().onPrimary();
      expect(App.router.send.calledWith('next')).to.equal(true);
      App.router.send.restore();
    });
  });

  describe('#needToAddServicePopup', function() {
    Em.A([
        {
          m: 'one service',
          services: {selected: true, serviceName: 's1'},
          content: [Em.Object.create({serviceName: 's1', isSelected: false})],
          e: [true]
        },
        {
          m: 'many services',
          services: [{selected: true, serviceName: 's1'}, {selected: false, serviceName: 's2'}],
          content: [Em.Object.create({serviceName: 's1', isSelected: false}),
            Em.Object.create({serviceName: 's2', isSelected: true})],
          e: [true, false]
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          sinon.stub(controller, 'submit', Em.K);
          controller.set('content', test.content);
          controller.needToAddServicePopup(test.services, '').onPrimary();
          expect(controller.submit.calledOnce).to.equal(true);
          expect(controller.mapProperty('isSelected')).to.eql(test.e);
          controller.submit.restore();
        });
      });
  });

 describe('#submit', function() {
    beforeEach(function() {
      sinon.stub(controller, 'validateMonitoring', Em.K);
      sinon.stub(controller, 'setGroupedServices', Em.K);
    });
    afterEach(function() {
      controller.validateMonitoring.restore();
      controller.setGroupedServices.restore();
    });
    it('if not isSubmitDisabled shound\'t do nothing', function() {
      controller.reopen({isSubmitDisabled: true});
      controller.submit();
      expect(controller.validateMonitoring.called).to.equal(false);
    });
    it('if isSubmitDisabled and not submitChecks should call validateMonitoring', function() {
      sinon.stub(controller, 'isSubmitChecksFailed', function() { return false; });
      controller.reopen({
        isSubmitDisabled: false,
        submitChecks: []
      });
      controller.submit();
      expect(controller.validateMonitoring.calledOnce).to.equal(true);
      controller.isSubmitChecksFailed.restore();
    });
    it('if isSubmitDisabled and some submitChecks true shouldn\'t call validateMonitoring', function() {
      controller.reopen({
        isSubmitDisabled: false,
        submitChecks: [
          {
            popupParams: [
              {serviceName: 'MAPREDUCE', selected: true},
              'mapreduceCheck'
            ]
          }
        ]
      });
      sinon.stub(controller, 'isSubmitChecksFailed', function() { return true; });
      controller.submit();
      controller.isSubmitChecksFailed.restore();
      expect(controller.validateMonitoring.called).to.equal(false);
    });
    it('if isSubmitDisabled and some submitChecks false should call validateMonitoring', function() {
      controller.reopen({
        isSubmitDisabled: false,
        submitChecks: [
          {
            checkCallback: 'needToAddMapReduce',
            popupParams: [
              {serviceName: 'MAPREDUCE', selected: true},
              'mapreduceCheck'
            ]
          }
        ]
      });
      sinon.stub(controller, 'isSubmitChecksFailed', function() { return false; });
      controller.submit();
      controller.isSubmitChecksFailed.restore();
      expect(controller.validateMonitoring.calledOnce).to.equal(true);
    });
  });

});