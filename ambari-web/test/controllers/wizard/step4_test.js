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
var modelSetup = require('test/init_model_test');

require('controllers/wizard/step4_controller');
describe('App.WizardStep4Controller', function () {

  var services = [
    'HDFS', 'MAPREDUCE', 'NAGIOS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SCOOP', 'ZOOKEEPER',
    'YARN', 'MAPREDUCE2', 'FALCON', 'TEZ', 'STORM'
  ];

  var controller = App.WizardStep4Controller.create();

  var generateSelectedServicesContent = function(selectedServiceNames) {
    var allServices = services.slice(0);
    modelSetup.setupStackServiceComponent();
    if (selectedServiceNames.contains('GLUSTERFS')) allServices.push('GLUSTERFS');
    allServices = allServices.map(function(serviceName) {
      return [Ember.Object.create({
        'serviceName': serviceName,
        'isSelected': false,
        'canBeSelected': true,
        'isInstalled': false,
        isPrimaryDFS: serviceName == 'HDFS',
        isDFS: ['HDFS','GLUSTERFS'].contains(serviceName),
        isMonitoringService: ['NAGIOS','GANGLIA'].contains(serviceName),
        requiredServices: App.StackService.find(serviceName).get('requiredServices'),
        displayNameOnSelectServicePage: App.format.role(serviceName),
        coSelectedServices: function() {
          return App.StackService.coSelected[this.get('serviceName')] || [];
        }.property('serviceName')
      })];
    }).reduce(function(current, prev) { return current.concat(prev); });

    selectedServiceNames.forEach(function(serviceName) {
      allServices.findProperty('serviceName', serviceName).set('isSelected', true);
    });

    return allServices;
  };

  services.forEach(function(serviceName, index){
    controller.pushObject(Ember.Object.create({
      'serviceName':serviceName, 'isSelected': true, 'isHiddenOnSelectServicePage': false, 'isInstalled': false, 'isDisabled': 'HDFS' === serviceName, isDFS: 'HDFS' === serviceName
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
      controller.setEach('isInstalled', false);
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

  describe('#multipleDFSs()', function () {
    it('should return true if HDFS is selected and GLUSTERFS is selected', function () {
      controller.set('content', generateSelectedServicesContent(['HDFS', 'GLUSTERFS']));
      expect(controller.multipleDFSs()).to.equal(true);
    });
    it('should return false if HDFS is not selected and GLUSTERFS is selected', function () {
      controller.set('content', generateSelectedServicesContent(['GLUSTERFS']));
      expect(controller.multipleDFSs()).to.equal(false);
    });
    it('should return false if HDFS is selected and GLUSTERFS is not selected', function () {
      controller.set('content', generateSelectedServicesContent(['HDFS']));
      expect(controller.multipleDFSs()).to.equal(false);
    });
  });

  describe('#setGroupedServices()', function () {
    var testCases = [
      {
        title: 'should set MapReduce2 isSelected to true when YARN is selected',
        condition: {
          'YARN': true,
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': true,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': true
        }
      },
      {
        title: 'should set MapReduce2 isSelected to false when YARN is not selected',
        condition: {
          'YARN': false,
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': false,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': false
        }
      },
      {
        title: 'should set MAPREDUCE2 isSelected to true when YARN is selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': false,
          'YARN': true,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': true
        }
      },
      {
        title: 'should set MAPREDUCE2 isSelected to false when YARN is not selected',
        condition: {
          'HBASE': true,
          'ZOOKEEPER': true,
          'HIVE': true,
          'YARN': false,
          'MAPREDUCE2': true
        },
        result: {
          'MAPREDUCE2': false
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

  describe('#addValidationError()', function() {
    var tests = [
      {
        errorObjects: [
          {
            id: 'serviceCheck_ZOOKEEPER',
            shouldBeAdded: true
          },
          {
            id: 'serviceCheck_YARN',
            shouldBeAdded: true
          }
        ],
        expectedIds: ['serviceCheck_ZOOKEEPER', 'serviceCheck_YARN']
      },
      {
        errorObjects: [
          {
            id: 'fsCheck',
            shouldBeAdded: true
          },
          {
            id: 'fsCheck',
            shouldBeAdded: false
          }
        ],
        expectedIds: ['fsCheck']
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    tests.forEach(function(test) {
      var message = 'Erorrs {0} thrown. errorStack property should contains ids: {1}'
        .format(test.errorObjects.mapProperty('id').join(', '), test.expectedIds.join(', '));
      it(message, function() {
        test.errorObjects.forEach(function(errorObject) {
          expect(controller.addValidationError(errorObject)).to.equal(errorObject.shouldBeAdded);
        });
        expect(controller.get('errorStack').mapProperty('id')).to.eql(test.expectedIds);
      });
    })
  });

  describe('#validate()', function() {
    var tests = [
      {
        services: ['HDFS','ZOOKEEPER'],
        errorsExpected: ['monitoringCheck']
      },
      {
        services: ['ZOOKEEPER'],
        errorsExpected: ['monitoringCheck']
      },
      {
        services: ['HDFS'],
        errorsExpected: ['serviceCheck_ZOOKEEPER', 'monitoringCheck']
      },
      {
        services: ['HDFS', 'TEZ', 'ZOOKEEPER'],
        errorsExpected: ['serviceCheck_YARN', 'monitoringCheck']
      },
      {
        services: ['HDFS', 'ZOOKEEPER', 'FALCON', 'NAGIOS'],
        errorsExpected: ['serviceCheck_OOZIE', 'monitoringCheck']
      },
      {
        services: ['HDFS', 'ZOOKEEPER', 'GANGLIA', 'NAGIOS', 'HIVE'],
        errorsExpected: ['serviceCheck_YARN']
      },
      {
        services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE'],
        errorsExpected: ['serviceCheck_YARN', 'multipleDFS', 'monitoringCheck']
      },
      {
        services: ['HDFS','ZOOKEEPER', 'NAGIOS', 'GANGLIA'],
        errorsExpected: []
      }
    ];

    tests.forEach(function(test) {
      var message = '{0} selected validation should be {1}, errors with ids: {2} present'
        .format(test.services.join(','), !!test.validationPassed ? 'passed' : 'failed', test.errorsExpected.join(','));
      it(message, function() {
        controller.clear();
        controller.set('content', generateSelectedServicesContent(test.services));
        controller.validate();
        expect(controller.get('errorStack').mapProperty('id')).to.be.eql(test.errorsExpected);
      });
    })
  });

  describe('#onPrimaryPopupCallback()', function() {
    var c;
    var tests = [
      {
        services: ['HDFS','ZOOKEEPER'],
        confirmPopupCount: 1,
        errorsExpected: ['monitoringCheck']
      },
      {
        services: ['ZOOKEEPER'],
        confirmPopupCount: 1,
        errorsExpected: ['monitoringCheck']
      },
      {
        services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE'],
        confirmPopupCount: 3,
        errorsExpected: ['serviceCheck_YARN', 'serviceCheck_TEZ', 'multipleDFS', 'monitoringCheck']
      },
      {
        services: ['HDFS','ZOOKEEPER', 'NAGIOS', 'GANGLIA'],
        confirmPopupCount: 0,
        errorsExpected: []
      }
    ];

    beforeEach(function() {
      c = App.WizardStep4Controller.create({});
      sinon.stub(App.router, 'send', Em.K);
      sinon.stub(c, 'submit', Em.K);
      sinon.spy(c, 'onPrimaryPopupCallback');
    });

    afterEach(function() {
      App.router.send.restore();
      c.submit.restore();
      c.onPrimaryPopupCallback.restore();
    });


    tests.forEach(function(test) {
      var message = 'Selected services: {0}. {1} errors should be confirmed'
        .format(test.services.join(', '), test.confirmPopupCount);

      it(message, function() {
        var runValidations = function() {
          c.serviceDependencyValidation();
          c.fileSystemServiceValidation();
          c.serviceMonitoringValidation();
        }

        c.set('content', generateSelectedServicesContent(test.services));
        runValidations();
        // errors count validation
        expect(c.get('errorStack.length')).to.equal(test.confirmPopupCount);
        // if errors detected than it should be shown
        if (test.errorsExpected) {
          test.errorsExpected.forEach(function(error, index, errors) {
            // validate current error
            var currentErrorObject = c.get('errorStack').findProperty('isShown', false);
            if (currentErrorObject) {
              expect(error).to.be.equal(currentErrorObject.id);
              // show current error
              var popup = c.showError(currentErrorObject);
              // submit popup
              popup.onPrimary();
              // onPrimaryPopupCallback should be called
              expect(c.onPrimaryPopupCallback.called).to.equal(true);
              // submit called
              expect(c.submit.called).to.equal(true);
              if (c.get('errorStack').length) {
                // current error isShown flag changed to true
                expect(currentErrorObject.isShown).to.equal(true);
              }
              runValidations();
            }
          });
        }
      });
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
    var c;
    var tests = [
      {
        isSubmitDisabled: true,
        validate: false,
        userCanProceed: false
      },
      {
        isSubmitDisabled: false,
        validate: false,
        userCanProceed: false
      },
      {
        isSubmitDisabled: false,
        validate: true,
        userCanProceed: true
      }
    ];

    beforeEach(function() {
      c = App.WizardStep4Controller.create();
      sinon.stub(App.router, 'send', Em.K);
    });

    afterEach(function() {
      App.router.send.restore();
    });

    tests.forEach(function(test) {
      var messageFormat = [
        test.isSubmitDisabled ? 'disabled' : 'enabled',
        test.validate ? 'success' : 'failed',
        test.userCanProceed ? '' : 'not'
      ];
      var message = String.prototype.format.apply('Submit btn: {0}. Validation: {1}. Can{2} move to the next step.', messageFormat);

      it(message, function() {
        c.reopen({
          isSubmitDisabled: test.isSubmitDisabled,
          validate: function() { return test.validate; }
        });
        c.clear();
        c.submit();

        expect(App.router.send.calledOnce).to.equal(test.userCanProceed);
      });

    })
  });

  describe('#dependencies', function() {
    var tests = [
      {
        services: ['HDFS'],
        dependencies: ['ZOOKEEPER'] 
      },
      {
        services: ['STORM'],
        dependencies: ['ZOOKEEPER'] 
      }
    ];
    tests.forEach(function(test) {
      var message = '{0} dependency should be {1}'.format(test.services.join(','), test.dependencies.join(','));
      it(message, function() {
        
        controller.clear();
        controller.set('content', generateSelectedServicesContent(test.services));
        
        var dependentServicesTest = [];
        
        test.services.forEach(function(serviceName) {
          var service = controller.filterProperty('serviceName', serviceName);
          service.forEach(function(item) {
            var dependencies = item.get('requiredServices');
            if(!!dependencies) {
              dependentServicesTest = dependentServicesTest.concat(dependencies);
            }
          });
        });

        expect(dependentServicesTest).to.be.eql(test.dependencies);
      });
    })
  });

});
