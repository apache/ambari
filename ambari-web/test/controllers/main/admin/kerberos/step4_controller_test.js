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
    var configCategories = Em.A([
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName: 'Advanced'})
    ]);
    var configs = Em.A([
      App.ServiceConfigProperty.create({ name: 'prop1', value: 'someVal1', category: 'Advanced'})
    ]);
    controller.set('stepConfigs', [controller.createServiceConfig(configCategories, configs)]);
    
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
      expect(controller.createServiceConfig([], [])).be.instanceof(App.ServiceConfig);
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
      Em.Object.create({ name: 'hdfs_keytab', value: '', serviceName: 'HDFS', observesValueFrom: 'spnego_keytab' }),
      Em.Object.create({ name: 'falcon_keytab', value: 'falcon_keytab_value', serviceName: 'FALCON' }),
      Em.Object.create({ name: 'mapreduce_keytab', value: 'mapreduce_keytab_value', serviceName: 'MAPREDUCE2' }),
      Em.Object.create({ name: 'hdfs_principal', value: 'hdfs_principal_value', serviceName: 'HDFS' })
    ]);
    
    var propertyValidationCases = [
      {
        property: 'spnego_keytab',
        e: [
          { key: 'category', value: 'General' },
          { key: 'observesValueFrom', absent: true },
        ]
      },
      {
        property: 'realm',
        e: [
          { key: 'category', value: 'General' },
          { key: 'value', value: 'realm_value' },
        ]
      },
      {
        property: 'hdfs_keytab',
        e: [
          { key: 'value', value: 'spnego_keytab_value' },
          { key: 'observesValueFrom', value: 'spnego_keytab' },
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
});

