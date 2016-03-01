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

describe('App.KerberosWizardStep4Controller', function() {

  describe('#isSubmitDisabled', function() {
    var controller = App.KerberosWizardStep4Controller.create({});
    var configs = Em.A([
      App.ServiceConfigProperty.create({ name: 'prop1', value: 'someVal1', identityType: 'user', category: 'Ambari Principals', serviceName: 'Cluster'})
    ]);
    controller.set('stepConfigs', controller.createServiceConfig(configs));

    it('configuration errors are absent, submit should be not disabled', function() {
      expect(controller.get('stepConfigs')[0].get('errorCount')).to.be.eql(0);
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });

    it('config has invalid value, submit should be disabled', function() {
      var serviceConfig = controller.get('stepConfigs')[0];
      serviceConfig.get('configs').findProperty('name', 'prop1').set('value', '');
      expect(serviceConfig.get('errorCount')).to.be.eql(1);
      expect(controller.get('isSubmitDisabled')).to.be.true;
    });
  });

  describe('#createServiceConfig', function() {
    var controller = App.KerberosWizardStep4Controller.create({});
    it('should create instance of App.ServiceConfig', function() {
      controller.createServiceConfig([], []).forEach(function(item){
        expect(item).be.instanceof(App.ServiceConfig);
      });
    });
  });

  describe('#prepareConfigProperties', function() {

    before(function() {
      var controller = App.KerberosWizardStep4Controller.create({
        wizardController: {
          getDBProperty: function() {
            return Em.A([
              Em.Object.create({ name: 'realm', value: 'realm_value' })
            ]);
          },
          loadCachedStepConfigValues: function() {
            return null;
          }
        }
      });
      sinon.stub(App.Service, 'find').returns(Em.A([
        { serviceName: 'HDFS' }
      ]));
      sinon.stub(App.config, 'get').withArgs('preDefinedSiteProperties').returns([
        {
          name: 'hadoop.security.auth_to_local',
          displayType: 'multiLine'
        }
      ]);
      sinon.stub(App.router, 'get').withArgs('mainAdminKerberosController.isManualKerberos').returns(false);
      this.result = controller.prepareConfigProperties(properties);
    });

    after(function() {
      App.Service.find.restore();
      App.config.get.restore();
      App.router.get.restore();
    });

    var properties = Em.A([
      Em.Object.create({ name: 'realm', value: '', serviceName: 'Cluster' }),
      Em.Object.create({ name: 'spnego_keytab', value: 'spnego_keytab_value', serviceName: 'Cluster' }),
      Em.Object.create({ name: 'hdfs_keytab', value: '', serviceName: 'HDFS', identityType: 'user', observesValueFrom: 'spnego_keytab' }),
      Em.Object.create({ name: 'falcon_keytab', value: 'falcon_keytab_value', serviceName: 'FALCON' }),
      Em.Object.create({ name: 'mapreduce_keytab', value: 'mapreduce_keytab_value', serviceName: 'MAPREDUCE2' }),
      Em.Object.create({ name: 'hdfs_principal', value: 'hdfs_principal_value', identityType: 'user', serviceName: 'HDFS' }),
      Em.Object.create({ name: 'hadoop.security.auth_to_local', serviceName: 'HDFS' })
    ]);

    var propertyValidationCases = [
      {
        property: 'spnego_keytab',
        e: [
          { key: 'category', value: 'Global' },
          { key: 'observesValueFrom', absent: true }
        ]
      },
      {
        property: 'realm',
        e: [
          { key: 'category', value: 'Global' },
          { key: 'value', value: 'realm_value' }
        ]
      },
      {
        property: 'hdfs_keytab',
        e: [
          { key: 'category', value: 'Ambari Principals' },
          { key: 'value', value: 'spnego_keytab_value' },
          { key: 'observesValueFrom', value: 'spnego_keytab' }
        ]
      },
      {
        property: 'hadoop.security.auth_to_local',
        e: [
          { key: 'displayType', value: 'multiLine' }
        ]
      }
    ];

    var absentPropertiesTest = ['falcon_keytab', 'mapreduce_keytab'];

    it('should contains properties only for installed services', function() {
      expect(this.result.mapProperty('serviceName').uniq()).to.be.eql(['Cluster', 'HDFS']);
    });

    absentPropertiesTest.forEach(function(item) {
      it('property `{0}` should be absent'.format(item), function() {
        expect(this.result.findProperty('name', item)).to.be.undefined;
      });
    }, this);

    propertyValidationCases.forEach(function(test) {
      it('property {0} should be created'.format(test.property), function() {
        expect(this.result.findProperty('name', test.property)).to.be.ok;
      });
      test.e.forEach(function(expected) {
        it('property `{0}` should have `{1}` with value `{2}`'.format(test.property, expected.key, expected.value), function() {
          if (!!expected.absent) {
            expect(this.result.findProperty('name', test.property)).to.not.have.deep.property(expected.key);
          } else {
            expect(this.result.findProperty('name', test.property)).to.have.deep.property(expected.key, expected.value);
          }
        }, this);
      }, this);
    });
  });

  describe('#setStepConfigs', function() {
    describe('Add Service Wizard', function() {
      var res;
      var controller;
      before(function() {
        sinon.stub(App.StackService, 'find').returns([
          Em.Object.create({
            serviceName: 'KERBEROS',
            configCategories: []
          }),
          Em.Object.create({
            serviceName: 'HDFS',
            configCategories: []
          }),
          Em.Object.create({
            serviceName: 'MAPREDUCE2'
          })
        ]);
        sinon.stub(App.Service, 'find').returns([
          Em.Object.create({
            serviceName: 'HDFS'
          }),
          Em.Object.create({
            serviceName: 'KERBEROS'
          })
        ]);
        controller = App.KerberosWizardStep4Controller.create({
          selectedServiceNames: ['FALCON', 'MAPREDUCE2'],
          installedServiceNames: ['HDFS', 'KERBEROS'],
          wizardController: Em.Object.create({
            name: 'addServiceController',
            getDBProperty: function() {
              return Em.A([
                Em.Object.create({ name: 'realm', value: 'realm_value' }),
              ]);
            },
            loadCachedStepConfigValues : function() {
              return null;
            }
          })
        });
        sinon.stub(App.router, 'get').withArgs('mainAdminKerberosController.isManualKerberos').returns(false);
        controller.setStepConfigs(properties);
        res = controller.get('stepConfigs')[0].get('configs').concat(controller.get('stepConfigs')[1].get('configs'));
      });

      var properties = Em.A([
        Em.Object.create({ name: 'realm', value: '', serviceName: 'Cluster' }),
        Em.Object.create({ name: 'spnego_keytab', value: 'spnego_keytab_value', serviceName: 'Cluster', isEditable: true }),
        Em.Object.create({ name: 'hdfs_keytab', value: '', serviceName: 'HDFS', observesValueFrom: 'spnego_keytab', isEditable: true }),
        Em.Object.create({ name: 'falcon_keytab', value: 'falcon_keytab_value', serviceName: 'FALCON', isEditable: true }),
        Em.Object.create({ name: 'mapreduce_keytab', value: 'mapreduce_keytab_value', serviceName: 'MAPREDUCE2', isEditable: true })
      ]);

      var propertiesEditableTests = [
        { name: 'spnego_keytab', e: false },
        { name: 'falcon_keytab', e: true },
        { name: 'hdfs_keytab', e: false },
        { name: 'mapreduce_keytab', e: true }
      ];

      propertiesEditableTests.forEach(function(test) {
        it('Add Service: property `{0}` should be {1} editable'.format(test.name, !!test.e ? '' : 'not '), function() {
          expect(res.findProperty('name', test.name).get('isEditable')).to.eql(test.e);
        });
      });

      after(function() {
        controller.destroy();
        controller = null;
        App.StackService.find.restore();
        App.Service.find.restore();
        App.router.get.restore();
      });
    });
  });

  describe("#createCategoryForServices()", function() {
    var controller = App.KerberosWizardStep4Controller.create({
      wizardController: {
        name: 'addServiceController'
      }
    });
    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayName: 'HDFS'
        })
      ]);
      sinon.stub(App.StackService, 'find').returns([
        Em.Object.create({
          serviceName: 'HDFS',
          displayName: 'HDFS',
          isInstalled: true
        }),
        Em.Object.create({
          serviceName: 'MAPREDUCE2',
          displayName: 'MapReduce 2',
          isInstalled: false,
          isSelected: true
        })
      ]);
    });

    afterEach(function() {
      App.Service.find.restore();
      App.StackService.find.restore();
    });

    it('for add service', function() {
      expect(controller.createCategoryForServices()).to.eql([App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS', collapsedByDefault: true}),
        App.ServiceConfigCategory.create({ name: 'MAPREDUCE2', displayName: 'MapReduce 2', collapsedByDefault: true})]);
    });

    it('for kerberos wizard', function() {
      controller.set('wizardController.name', 'KerberosWizard');
      expect(controller.createCategoryForServices()).to.eql([App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS', collapsedByDefault: true})]);
    });
  });

  describe('#loadStep', function() {
    var controller;
    describe('skip "Configure Identities" step. ', function() {
      beforeEach(function() {
        controller = App.KerberosWizardStep4Controller.create({});
        this.wizardController = App.AddServiceController.create({});
        controller.set('wizardController', this.wizardController);
        sinon.stub(controller, 'clearStep').returns(true);
        sinon.stub(controller, 'getDescriptorConfigs').returns((new $.Deferred()).resolve(true).promise());
        sinon.stub(controller, 'setStepConfigs').returns(true);
        sinon.stub(App.router, 'send').withArgs('next');
      });

      afterEach(function() {
        controller.clearStep.restore();
        controller.getDescriptorConfigs.restore();
        controller.setStepConfigs.restore();
        App.router.send.restore();
      });

      var tests = [
        {
          securityEnabled: true,
          stepSkipped: false
        },
        {
          securityEnabled: false,
          stepSkipped: true
        }
      ];

      tests.forEach(function(test) {
        it('Security {0} configure identities step should be {1}'.format(!!test.securityEnabled ? 'enabled' : 'disabled', !!test.stepSkipped ? 'skipped' : 'not skipped'), function() {
          sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(test.securityEnabled);
          this.wizardController.checkSecurityStatus();
          App.get.restore();
          controller.loadStep();
          expect(App.router.send.calledWith('next')).to.be.eql(test.stepSkipped);
        });
      }, this);

      it('step should not be disabled for Add Kerberos wizard', function() {
        controller.set('wizardController', App.KerberosWizardController.create({}));
        controller.loadStep();
        expect(App.router.send.calledWith('next')).to.be.false;
      });
    });
  });

  describe('#getDescriptorConfigs', function() {
    describe('Within Add Service', function () {
      var controller;
      beforeEach(function () {
        controller = App.KerberosWizardStep4Controller.create({
          wizardController: Em.Object.create({
            name: 'addServiceController',
            setDBProperty: sinon.spy()
          })
        });
        this.loadStackDescriptorStub = sinon.stub(controller, 'loadStackDescriptorConfigs').returns($.Deferred().resolve().promise());
        this.loadClusterDescriptorStub = sinon.stub(controller, 'loadClusterDescriptorConfigs');
        sinon.stub(controller, 'createServicesStackDescriptorConfigs', Em.K);
      });

      afterEach(function() {
        this.loadStackDescriptorStub.restore();
        this.loadClusterDescriptorStub.restore();
        controller.createServicesStackDescriptorConfigs.restore();
        controller.destroy();
        controller = null;
      });

      var cases = [
        {
          wizardController: 'addServiceController',
          clusterDescriptorExists: false,
          m: 'Within Add Service, Cluster Descriptor not exists. Should be reflected in wizard controller',
          e: {
            setDBPropertyCalled: true,
            setDBPropertyCalledWith: ['isClusterDescriptorExists', false]
          }
        },
        {
          wizardController: 'addServiceController',
          clusterDescriptorExists: true,
          m: 'Within Add Service, Cluster Descriptor is present. Should be reflected in wizard controller',
          e: {
            setDBPropertyCalled: true,
            setDBPropertyCalledWith: ['isClusterDescriptorExists', true]
          }
        },
        {
          wizardController: 'notAddService',
          clusterDescriptorExists: true,
          m: 'Within another controller, nothing to store',
          e: {
            setDBPropertyCalled: false,
          }
        }
      ];

      cases.forEach(function(test) {
        it(test.m, function () {
          controller.get('wizardController').set('name', test.wizardController);
          this.loadClusterDescriptorStub.returns(test.clusterDescriptorExists ?
             $.Deferred().resolve().promise() :
             $.Deferred().reject().promise());
          controller.getDescriptorConfigs();
          expect(controller.get('wizardController').setDBProperty.called).to.be.eql(test.e.setDBPropertyCalled);
          if (test.e.setDBPropertyCalled) {
            expect(controller.get('wizardController').setDBProperty.args[0]).to.be.eql(test.e.setDBPropertyCalledWith);
          }
        })
      })
    });
  });
});
