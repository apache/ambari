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
    'HDFS', 'GANGLIA', 'OOZIE', 'HIVE', 'HBASE', 'PIG', 'SCOOP', 'ZOOKEEPER',
    'YARN', 'MAPREDUCE2', 'FALCON', 'TEZ', 'STORM', 'AMBARI_METRICS', 'RANGER', 'SPARK'
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
        isMonitoringService: ['GANGLIA'].contains(serviceName),
        requiredServices: App.StackService.find(serviceName).get('requiredServices'),
        displayNameOnSelectServicePage: App.format.role(serviceName, true),
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

  describe('#isAllChecked', function () {
    it('should return true if all services are selected', function () {
      controller.setEach('isInstalled', false);
      controller.findProperty('serviceName', 'HDFS').set('isSelected', true);
      expect(controller.get('isAllChecked')).to.equal(true);
    });

    it('should return false if at least one service is not selected', function () {
      controller.findProperty('serviceName', 'HDFS').set('isSelected', false);
      expect(controller.get('isAllChecked')).to.equal(false);
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
          errorsExpected: ['ambariMetricsCheck']
        },
        {
          services: ['ZOOKEEPER'],
          errorsExpected: ['ambariMetricsCheck']
        },
        {
          services: ['HDFS'],
          errorsExpected: ['serviceCheck_ZOOKEEPER', 'ambariMetricsCheck']
        },
        {
          services: ['HDFS', 'TEZ', 'ZOOKEEPER'],
          errorsExpected: ['serviceCheck_YARN', 'ambariMetricsCheck']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'FALCON'],
          errorsExpected: ['serviceCheck_OOZIE', 'ambariMetricsCheck']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'GANGLIA', 'HIVE'],
          errorsExpected: ['serviceCheck_YARN', 'ambariMetricsCheck']
        },
        {
          services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE'],
          errorsExpected: ['serviceCheck_YARN', 'multipleDFS', 'ambariMetricsCheck']
        },
        {
          services: ['HDFS','ZOOKEEPER', 'GANGLIA'],
          errorsExpected: ['ambariMetricsCheck']
        },
        {
          services: ['HDFS','ZOOKEEPER', 'AMBARI_METRICS'],
          errorsExpected: []
        },
        {
          services: ['ZOOKEEPER', 'AMBARI_METRICS'],
          errorsExpected: []
        },
        {
          services: ['HDFS', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_ZOOKEEPER']
        },
        {
          services: ['HDFS', 'TEZ', 'ZOOKEEPER', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_YARN']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'FALCON', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_OOZIE']
        },
        {
          services: ['HDFS', 'ZOOKEEPER', 'GANGLIA', 'HIVE', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_YARN']
        },
        {
          services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE', 'AMBARI_METRICS'],
          errorsExpected: ['serviceCheck_YARN', 'multipleDFS']
        },
        {
          services: ['HDFS','ZOOKEEPER', 'GANGLIA', 'AMBARI_METRICS'],
          errorsExpected: []
        },
        {
          services: ['RANGER'],
          errorsExpected: ['ambariMetricsCheck', 'rangerRequirements']
        }
      ],
      controllerNames = ['installerController', 'addServiceController'],
      wizardNames = {
        installerController: 'Install Wizard',
        addServiceController: 'Add Service Wizard'
      },
      sparkCases = [
        {
          currentStackName: 'HDP',
          currentStackVersionNumber: '2.2',
          sparkWarningExpected: true,
          title: 'HDP 2.2'
        },
        {
          currentStackName: 'HDP',
          currentStackVersionNumber: '2.3',
          sparkWarningExpected: false,
          title: 'HDP 2.3'
        },
        {
          currentStackName: 'BIGTOP',
          currentStackVersionNumber: '0.8',
          sparkWarningExpected: false,
          title: 'Non-HDP stack'
        }
      ];

    beforeEach(function () {
      controller.clear();
    });

    controllerNames.forEach(function (name) {
      tests.forEach(function(test) {
        var errorsExpected = test.errorsExpected;
        if (name != 'installerController') {
          errorsExpected = test.errorsExpected.without('ambariMetricsCheck');
        }
        var message = '{0}, {1} selected validation should be {2}, errors: {3}'
          .format(wizardNames[name], test.services.join(','), errorsExpected.length ? 'passed' : 'failed',
            errorsExpected.length ? errorsExpected.join(',') : 'absent');
        it(message, function() {
          controller.setProperties({
            content: generateSelectedServicesContent(test.services),
            errorStack: [],
            wizardController: Em.Object.create({
              name: name
            })
          });
          controller.validate();
          expect(controller.get('errorStack').mapProperty('id')).to.eql(errorsExpected.toArray());
        });
      })
    });

    sparkCases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('currentStackName').returns(item.currentStackName).
          withArgs('currentStackVersionNumber').returns(item.currentStackVersionNumber);
        controller.set('errorStack', []);
        controller.set('content', generateSelectedServicesContent(['SPARK']));
        controller.validate();
        expect(controller.get('errorStack').someProperty('id', 'sparkWarning')).to.equal(item.sparkWarningExpected);
        App.get.restore();
      });
    });

  });

  describe('#onPrimaryPopupCallback()', function() {
    var c;
    var tests = [
      {
        services: ['HDFS','ZOOKEEPER'],
        confirmPopupCount: 0,
        errorsExpected: []
      },
      {
        services: ['ZOOKEEPER'],
        confirmPopupCount: 0,
        errorsExpected: []
      },
      {
        services: ['HDFS', 'GLUSTERFS', 'ZOOKEEPER', 'HIVE'],
        confirmPopupCount: 2,
        errorsExpected: ['serviceCheck_YARN', 'serviceCheck_TEZ', 'multipleDFS']
      },
      {
        services: ['HDFS','ZOOKEEPER', 'GANGLIA'],
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
        };

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

  describe('#ambariMetricsValidation', function () {

    var cases = [
      {
        services: ['HDFS'],
        isAmbariMetricsWarning: false,
        title: 'Ambari Metrics not available'
      },
      {
        services: ['AMBARI_METRICS'],
        isAmbariMetricsSelected: false,
        isAmbariMetricsWarning: true,
        title: 'Ambari Metrics not selected'
      },
      {
        services: ['AMBARI_METRICS'],
        isAmbariMetricsSelected: true,
        isAmbariMetricsWarning: false,
        title: 'Ambari Metrics selected'
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('content', generateSelectedServicesContent(item.services));
        var ams = controller.findProperty('serviceName', 'AMBARI_METRICS');
        if (item.services.contains('AMBARI_METRICS')) {
          ams.set('isSelected', item.isAmbariMetricsSelected);
        } else {
          controller.removeObject(ams);
        }
        controller.ambariMetricsValidation();
        expect(controller.get('errorStack').mapProperty('id').contains('ambariMetricsCheck')).to.equal(item.isAmbariMetricsWarning);
      });
    });

  });

  describe('#rangerValidation', function () {

    var cases = [
      {
        services: ['HDFS'],
        isRangerWarning: false,
        title: 'Ranger not available'
      },
      {
        services: ['RANGER'],
        isRangerSelected: false,
        isRangerInstalled: false,
        isRangerWarning: false,
        title: 'Ranger not selected'
      },
      {
        services: ['RANGER'],
        isRangerSelected: true,
        isRangerInstalled: false,
        isRangerWarning: true,
        title: 'Ranger selected'
      },
      {
        services: ['RANGER'],
        isRangerSelected: true,
        isRangerInstalled: true,
        isRangerWarning: false,
        title: 'Ranger installed'
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('content', generateSelectedServicesContent(item.services));
        var ranger = controller.findProperty('serviceName', 'RANGER');
        if (item.services.contains('RANGER')) {
          ranger.setProperties({
            isSelected: item.isRangerSelected,
            isInstalled: item.isRangerInstalled
          });
        } else {
          controller.removeObject(ranger);
        }
        controller.rangerValidation();
        expect(controller.get('errorStack').mapProperty('id').contains('rangerRequirements')).to.equal(item.isRangerWarning);
      });
    });

  });

  describe('#sparkValidation', function () {

    var cases = [
      {
        services: ['HDFS'],
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark not available'
      },
      {
        services: ['HDFS'],
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark not available'
      },
      {
        services: ['HDFS'],
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark not available'
      },
      {
        services: ['SPARK'],
        isSparkSelected: false,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark not selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: false,
        isSparkWarning: true,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: true,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.2',
        title: 'HDP 2.2, Spark installed'
      },
      {
        services: ['SPARK'],
        isSparkSelected: false,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark not selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: true,
        isSparkWarning: false,
        currentStackName: 'HDP',
        currentStackVersionNumber: '2.3',
        title: 'HDP 2.3, Spark installed'
      },
      {
        services: ['SPARK'],
        isSparkSelected: false,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark not selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: false,
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark selected'
      },
      {
        services: ['SPARK'],
        isSparkSelected: true,
        isSparkInstalled: true,
        isSparkWarning: false,
        currentStackName: 'BIGTOP',
        currentStackVersionNumber: '0.8',
        title: 'Non-HDP stack, Spark installed'
      }
    ];

    beforeEach(function() {
      controller.clear();
      controller.set('errorStack', []);
    });

    afterEach(function () {
      App.get.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('currentStackName').returns(item.currentStackName).
          withArgs('currentStackVersionNumber').returns(item.currentStackVersionNumber);
        controller.set('content', generateSelectedServicesContent(item.services));
        var spark = controller.findProperty('serviceName', 'SPARK');
        if (item.services.contains('SPARK')) {
          spark.setProperties({
            isSelected: item.isSparkSelected,
            isInstalled: item.isSparkInstalled
          });
        } else {
          controller.removeObject(spark);
        }
        controller.sparkValidation();
        expect(controller.get('errorStack').mapProperty('id').contains('sparkWarning')).to.equal(item.isSparkWarning);
      });
    });

  });

  describe('#clearErrors', function () {

    var cases = [
      {
        isValidating: true,
        errorStack: [{}],
        title: 'error stack shouldn\'t be cleared during validation'
      },
      {
        isValidating: false,
        errorStack: [],
        title: 'error stack should be cleared'
      }
    ];

    beforeEach(function () {
      controller.set('errorStack', [{}]);
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.set('isValidating', item.isValidating);
        controller.propertyDidChange('@each.isSelected');
        expect(controller.get('errorStack')).to.eql(item.errorStack);
      });
    });

  });

});
