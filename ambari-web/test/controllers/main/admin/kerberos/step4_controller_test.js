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
          }
        }
      });
      sinon.stub(App.Service, 'find').returns(Em.A([
        { serviceName: 'HDFS' }
      ]));
      this.result = controller.prepareConfigProperties(properties);
    });

    after(function() {
      App.Service.find.restore();
    });

    var properties = Em.A([
      Em.Object.create({ name: 'realm', value: '', serviceName: 'Cluster' }),
      Em.Object.create({ name: 'spnego_keytab', value: 'spnego_keytab_value', serviceName: 'Cluster' }),
      Em.Object.create({ name: 'hdfs_keytab', value: '', serviceName: 'HDFS', identityType: 'user', observesValueFrom: 'spnego_keytab' }),
      Em.Object.create({ name: 'falcon_keytab', value: 'falcon_keytab_value', serviceName: 'FALCON' }),
      Em.Object.create({ name: 'mapreduce_keytab', value: 'mapreduce_keytab_value', serviceName: 'MAPREDUCE2' }),
      Em.Object.create({ name: 'hdfs_principal', value: 'hdfs_principal_value', identityType: 'user', serviceName: 'HDFS' })
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
          {key: 'category', value: 'Ambari Principals'},
          { key: 'value', value: 'spnego_keytab_value' },
          { key: 'observesValueFrom', value: 'spnego_keytab' }
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
        var controller = App.KerberosWizardStep4Controller.create({
          selectedServiceNames: ['FALCON', 'MAPREDUCE2'],
          installedServiceNames: ['HDFS', 'KERBEROS'],
          wizardController: Em.Object.create({
            name: 'addServiceController',
            getDBProperty: function() {
              return Em.A([
                Em.Object.create({ name: 'realm', value: 'realm_value' }),
                Em.Object.create({ name: 'admin_principal', value: 'some_val1', defaultValue: 'some_val1', filename: 'krb5-conf.xml' }),
                Em.Object.create({ name: 'admin_password', value: 'some_password', defaultValue: 'some_password', filename: 'krb5-conf.xml' })
              ]);
            }
          })
        });
        controller.setStepConfigs(properties);
        this.result = controller.get('stepConfigs')[0].get('configs').concat(controller.get('stepConfigs')[1].get('configs'));
      });

      after(function() {
        App.StackService.find.restore();
        App.Service.find.restore();
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
        { name: 'mapreduce_keytab', e: true },
        { name: 'admin_principal', e: true },
        { name: 'admin_password', e: true }
      ];
      
      propertiesEditableTests.forEach(function(test) {
        it('Add Service: property `{0}` should be {1} editable'.format(test.name, !!test.e ? '' : 'not '), function() {
          expect(this.result.findProperty('name', test.name).get('isEditable')).to.eql(test.e);
        });
      });

      ['admin_principal', 'admin_password'].forEach(function(item) {
        it('property `{0}` should have empty value'.format(item), function() {
          expect(this.result.findProperty('name', item).get('value')).to.be.empty;
        });
      });
    });
  });

});
