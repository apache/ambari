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
require('config');
require('utils/config');
require('models/service/hdfs');
var setups = require('test/init_model_test');
var modelSetup = setups.configs;

describe('App.config', function () {

  describe('#trimProperty',function() {
    var testMessage = 'displayType `{0}`, value `{1}`{3} should return `{2}`';
    var tests = [
      {
        config: {
          displayType: 'directory',
          value: ' /a /b /c'
        },
        e: '/a,/b,/c'
      },
      {
        config: {
          displayType: 'directories',
          value: ' /a /b '
        },
        e: '/a,/b'
      },
      {
        config: {
          displayType: 'directories',
          name: 'dfs.datanode.data.dir',
          value: ' [DISK]/a [SSD]/b '
        },
        e: '[DISK]/a,[SSD]/b'
      },
      {
        config: {
          displayType: 'directories',
          name: 'dfs.datanode.data.dir',
          value: '/a,/b, /c\n/d,\n/e  /f'
        },
        e: '/a,/b,/c,/d,/e,/f'
      },
      {
        config: {
          displayType: 'host',
          value: ' localhost '
        },
        e: 'localhost'
      },
      {
        config: {
          displayType: 'password',
          value: ' passw ord '
        },
        e: ' passw ord '
      },
      {
        config: {
          displayType: 'string',
          value: ' value'
        },
        e: ' value'
      },
      {
        config: {
          displayType: 'string',
          value: ' value'
        },
        e: ' value'
      },
      {
        config: {
          displayType: 'string',
          value: 'http://localhost ',
          name: 'javax.jdo.option.ConnectionURL'
        },
        e: 'http://localhost'
      },
      {
        config: {
          displayType: 'string',
          value: 'http://localhost    ',
          name: 'oozie.service.JPAService.jdbc.url'
        },
        e: 'http://localhost'
      },
      {
        config: {
          displayType: 'custom',
          value: ' custom value '
        },
        e: ' custom value'
      },
      {
        config: {
          displayType: 'componentHosts',
          value: ['host1.com', 'host2.com']
        },
        e: ['host1.com', 'host2.com']
      }
    ];

    tests.forEach(function(test) {
      it(testMessage.format(test.config.displayType, test.config.value, test.e, !!test.config.name ? ', name `' + test.config.name + '`' : ''), function() {
        expect(App.config.trimProperty(test.config)).to.eql(test.e);
        expect(App.config.trimProperty(Em.Object.create(test.config), true)).to.eql(test.e);
      });
    });
  });

  describe('#preDefinedConfigFile', function() {
    before(function() {
      setups.setupStackVersion(this, 'BIGTOP-0.8');
    });

    it('bigtop site properties should be ok.', function() {
      var bigtopSiteProperties = App.config.preDefinedConfigFile('BIGTOP', 'site_properties');
      expect(bigtopSiteProperties).to.be.ok;
    });

    it('a non-existing file should not be ok.', function () {
      var notExistingSiteProperty = App.config.preDefinedConfigFile('notExisting');
      expect(notExistingSiteProperty).to.not.be.ok;
    });

    after(function() {
      setups.restoreStackVersion(this);
    });
  });

  describe('#generateConfigPropertiesByName', function() {
    var tests = [
      {
        names: ['property_1', 'property_2'],
        properties: undefined,
        e: {
          keys: ['name']
        },
        m: 'Should generate base property object without additional fields'
      },
      {
        names: ['property_1', 'property_2'],
        properties: { category: 'SomeCat', serviceName: 'SERVICE_NAME' },
        e: {
          keys: ['name', 'category', 'serviceName']
        },
        m: 'Should generate base property object without additional fields'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).length).to.eql(test.names.length);
        expect(App.config.generateConfigPropertiesByName(test.names, test.properties).map(function(property) {
          return Em.keys(property);
        }).reduce(function(p, c) {
          return p.concat(c);
        }).uniq()).to.eql(test.e.keys);
      });
    });

  });

  describe('#setPreDefinedServiceConfigs', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find', function() {
        return [
          Em.Object.create({
            id: 'HDFS',
            serviceName: 'HDFS',
            configTypes: {
              'hadoop-env': {},
              'hdfs-site': {}
            }
          }),
          Em.Object.create({
            id: 'OOZIE',
            serviceName: 'OOZIE',
            configTypes: {
              'oozie-env': {},
              'oozie-site': {}
            }
          })
        ];
      });
      App.config.setPreDefinedServiceConfigs(true);
    });
    afterEach(function() {
      App.StackService.find.restore();
    });

    it('should include service MISC', function() {
      expect(App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'MISC')).to.be.ok;
    });

    it('should include -env config types according to stack services', function() {
      var miscCategory = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'MISC');
      expect(Em.keys(miscCategory.get('configTypes'))).to.eql(['cluster-env', 'hadoop-env', 'oozie-env']);
    });

    it('should not load configs for missed config types', function() {
      var hdfsService = App.config.get('preDefinedServiceConfigs').findProperty('serviceName', 'HDFS');
      var rangerRelatedConfigs = hdfsService.get('configs').filterProperty('filename', 'ranger-hdfs-plugin-properties.xml');
      expect(rangerRelatedConfigs.length).to.be.eql(0);
      expect(hdfsService.get('configs.length') > 0).to.be.true;
    });
  });
  
  describe('#isManagedMySQLForHiveAllowed', function () {

    var cases = [
      {
        osFamily: 'redhat5',
        expected: false
      },
      {
        osFamily: 'redhat6',
        expected: true
      },
      {
        osFamily: 'suse11',
        expected: false
      }
    ],
      title = 'should be {0} for {1}';

    cases.forEach(function (item) {
      it(title.format(item.expected, item.osFamily), function () {
        expect(App.config.isManagedMySQLForHiveAllowed(item.osFamily)).to.equal(item.expected);
      });
    });

  });


  describe('#advancedConfigIdentityData', function () {

    var configs = [
      {
        input: {
          property_type: ['USER'],
          property_name: 'hdfs_user'
        },
        output: {
          category: 'Users and Groups',
          isVisible: true,
          serviceName: 'MISC',
          displayType: 'user',
          index: 30
        },
        title: 'user, no service name specified, default display name behaviour'
      },
      {
        input: {
          property_type: ['GROUP'],
          property_name: 'knox_group',
          service_name: 'KNOX'
        },
        output: {
          category: 'Users and Groups',
          isVisible: true,
          serviceName: 'MISC',
          displayType: 'user',
          index: 0
        },
        title: 'group, service_name = KNOX, default display name behaviour'
      },
      {
        input: {
          property_type: ['USER']
        },
        output: {
          isVisible: false
        },
        isHDPWIN: true,
        title: 'HDPWIN stack'
      },
      {
        input: {
          property_type: ['USER'],
          property_name: 'smokeuser',
          service_name: 'MISC'
        },
        output: {
          serviceName: 'MISC',
          belongsToService: ['MISC'],
          index: 30
        },
        title: 'smokeuser, service_name = MISC'
      },
      {
        input: {
          property_type: ['USER'],
          property_name: 'ignore_groupsusers_create'
        },
        output: {
          displayType: 'boolean'
        },
        title: 'ignore_groupsusers_create'
      },
      {
        input: {
          property_type: ['GROUP'],
          property_name: 'proxyuser_group'
        },
        output: {
          belongsToService: ['HIVE', 'OOZIE', 'FALCON']
        },
        title: 'proxyuser_group'
      },
      {
        input: {
          property_type: ['PASSWORD'],
          property_name: 'javax.jdo.option.ConnectionPassword'
        },
        output: {
          displayType: 'password'
        },
        title: 'password'
      }
    ];

    before(function () {
      sinon.stub(App.StackService, 'find').returns([
        {
          serviceName: 'KNOX'
        }
      ]);
    });

    afterEach(function () {
      App.get.restore();
    });

    after(function () {
      App.StackService.find.restore();
    });

    configs.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(App, 'get').withArgs('isHadoopWindowsStack').returns(Boolean(item.isHDPWIN));
        var propertyData = App.config.advancedConfigIdentityData(item.input);
        Em.keys(item.output).forEach(function (key) {
          expect(propertyData[key]).to.eql(item.output[key]);
        });
      });
    });

  });

  describe('#shouldSupportFinal', function () {

    var cases = [
      {
        shouldSupportFinal: false,
        title: 'no service name specified'
      },
      {
        serviceName: 's0',
        shouldSupportFinal: false,
        title: 'no filename specified'
      },
      {
        serviceName: 'MISC',
        shouldSupportFinal: false,
        title: 'MISC'
      },
      {
        serviceName: 's0',
        filename: 's0-site',
        shouldSupportFinal: true,
        title: 'final attribute supported'
      },
      {
        serviceName: 's0',
        filename: 's0-env',
        shouldSupportFinal: false,
        title: 'final attribute not supported'
      },
      {
        serviceName: 'Cluster',
        filename: 'krb5-conf.xml',
        shouldSupportFinal: false,
        title: 'kerberos descriptor identities don\'t support final'
      }
    ];

    beforeEach(function () {
      sinon.stub(App.StackService, 'find').returns([
        {
          serviceName: 's0'
        }
      ]);
      sinon.stub(App.config, 'getConfigTypesInfoFromService').returns({
        supportsFinal: ['s0-site']
      });
    });

    afterEach(function () {
      App.StackService.find.restore();
      App.config.getConfigTypesInfoFromService.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(App.config.shouldSupportFinal(item.serviceName, item.filename)).to.equal(item.shouldSupportFinal);
      });
    });

  });

  describe('#removeRangerConfigs', function () {

    it('should remove ranger configs and categories', function () {
      var configs = [
        Em.Object.create({
          configs: [
            Em.Object.create({filename: 'filename'}),
            Em.Object.create({filename: 'ranger-filename'})
          ],
          configCategories: [
            Em.Object.create({name: 'ranger-name'}),
            Em.Object.create({name: 'name'}),
            Em.Object.create({name: 'also-ranger-name'})
          ]
        })
      ];
      App.config.removeRangerConfigs(configs);
      expect(configs).eql(
          [
            Em.Object.create({
              configs: [
                Em.Object.create({filename: 'filename'})
              ],
              configCategories: [
                Em.Object.create({name: 'name'})
              ]
            })
          ]
      );
    });

  });

  describe('#updateHostsListValue', function() {
    var tests = [
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1,host2'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        hostsList: 'host1',
        e: 'host1'
      },
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1:10,host2:10'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        hostsList: 'host2:10,host1:10',
        e: 'host1:10,host2:10'
      },
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1:10,host2:10,host3:10'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        hostsList: 'host2:10,host1:10',
        e: 'host2:10,host1:10'
      },
      {
        siteConfigs: {
          'hadoop.registry.zk.quorum': 'host1:10,host2:10,host3:10'
        },
        propertyName: 'hadoop.registry.zk.quorum',
        hostsList: 'host2:10,host1:10,host3:10,host4:11',
        e: 'host2:10,host1:10,host3:10,host4:11'
      },
      {
        siteConfigs: {
          'hive.zookeeper.quorum': 'host1'
        },
        propertyName: 'some.new.property',
        hostsList: 'host2,host1:10',
        e: 'host2,host1:10'
      }
    ];
    tests.forEach(function(test) {
      it('ZK located on {0}, current prop value is "{1}" "{2}" value should be "{3}"'.format(test.hostsList, ''+test.siteConfigs[test.propertyName], test.propertyName, test.e), function() {
        var result = App.config.updateHostsListValue(test.siteConfigs, test.propertyName, test.hostsList);
        expect(result).to.be.eql(test.e);
        expect(test.siteConfigs[test.propertyName]).to.be.eql(test.e);
      });
    });
  });

  describe("#createOverride", function() {
    var template = {
      name: "p1",
      filename: "f1",
      value: "v1",
      recommendedValue: "rv1",
      savedValue: "sv1",
      isFinal: true,
      recommendedIsFinal: false,
      savedIsFinal: true
    };

    var configProperty = App.ServiceConfigProperty.create(template);

    var group = App.ConfigGroup.create({name: "group1"});

    it('creates override with save properties as original config', function() {
      var override = App.config.createOverride(configProperty, {}, group);
      for (var key in template) {
        expect(override.get(key)).to.eql(template[key]);
      }
    });

    it('overrides some values that should be different for override', function() {
      var override = App.config.createOverride(configProperty, {}, group);
      expect(override.get('isOriginalSCP')).to.be.false;
      expect(override.get('overrides')).to.be.null;
      expect(override.get('group')).to.eql(group);
      expect(override.get('parentSCP')).to.eql(configProperty);
    });

    it('overrides some specific values', function() {
      var overridenTemplate = {
        value: "v2",
        recommendedValue: "rv2",
        savedValue: "sv2",
        isFinal: true,
        recommendedIsFinal: false,
        savedIsFinal: true
      };

      var override = App.config.createOverride(configProperty, overridenTemplate, group);
      for (var key in overridenTemplate) {
        expect(override.get(key)).to.eql(overridenTemplate[key]);
      }
    });

    it('throws error due to undefined configGroup', function() {
      expect(App.config.createOverride.bind(App.config, configProperty, {}, null)).to.throw(Error, 'configGroup can\' be null');
    });

    it('throws error due to undefined originalSCP', function() {
      expect(App.config.createOverride.bind(App.config, null, {}, group)).to.throw(Error, 'serviceConfigProperty can\' be null');
    });

    it('updates originalSCP object ', function() {
      configProperty.set('overrides', null);
      configProperty.set('overrideValues', []);
      configProperty.set('overrideIsFinalValues', []);

      var overridenTemplate2 = {
        value: "v12",
        recommendedValue: "rv12",
        savedValue: "sv12",
        isFinal: true,
        recommendedIsFinal: false,
        savedIsFinal: false
      };

      var override = App.config.createOverride(configProperty, overridenTemplate2, group);

      expect(configProperty.get('overrides')[0]).to.be.eql(override);
      expect(configProperty.get('overrideValues')).to.be.eql([overridenTemplate2.value]);
      expect(configProperty.get('overrideIsFinalValues')).to.be.eql([overridenTemplate2.isFinal]);
    });
  });

  describe('#getIsEditable', function() {
    [{
        isDefaultGroup: true,
        isReconfigurable: true,
        canEdit: true,
        res: true,
        m: "isEditable is true"
      },
      {
        isDefaultGroup: false,
        isReconfigurable: true,
        canEdit: true,
        res: false,
        m: "isEditable is false; config group is not default"
      },
      {
        isDefaultGroup: true,
        isReconfigurable: false,
        canEdit: true,
        res: false,
        m: "isEditable is true; config is not reconfigurable"
      },
      {
        isDefaultGroup: true,
        isReconfigurable: true,
        canEdit: false,
        res: false,
        m: "isEditable is true; edition restricted by controller state"
    }].forEach(function(t) {
        it(t.m, function() {
          var configProperty = Ember.Object.create({isReconfigurable: t.isReconfigurable});
          var configGroup = Ember.Object.create({isDefault: t.isDefaultGroup});
          var isEditable = App.config.getIsEditable(configProperty, configGroup, t.canEdit);
          expect(isEditable).to.equal(t.res);
        })
      });
  });

  describe('#getIsSecure', function() {
    var secureConfigs = App.config.get('secureConfigs');
    before(function() {
      App.config.set('secureConfigs', [{name: 'secureConfig'}]);
    });
    after(function() {
      App.config.set('secureConfigs', secureConfigs);
    });

    it('config is secure', function() {
      expect(App.config.getIsSecure('secureConfig')).to.equal(true);
    });
    it('config is not secure', function() {
      expect(App.config.getIsSecure('NotSecureConfig')).to.equal(false);
    });
  });

  describe('#getDefaultCategory', function() {
    it('returns custom category', function() {
      expect(App.config.getDefaultCategory(null, 'filename.xml')).to.equal('Custom filename');
    });
    it('returns advanced category', function() {
      expect(App.config.getDefaultCategory(Em.Object.create, 'filename.xml')).to.equal('Advanced filename');
    });
  });

  describe('#getDefaultDisplayType', function() {
    it('returns content displayType', function() {
      sinon.stub(App.config, 'isContentProperty', function () {return true});
      expect(App.config.getDefaultDisplayType('content','f1','anything')).to.equal('content');
      App.config.isContentProperty.restore();
    });
    it('returns singleLine displayType', function() {
      sinon.stub(App.config, 'isContentProperty', function () {return false});
      expect(App.config.getDefaultDisplayType('n1','f1','v1')).to.equal('string');
      App.config.isContentProperty.restore();
    });
    it('returns multiLine displayType', function() {
      sinon.stub(App.config, 'isContentProperty', function () {return false});
      expect(App.config.getDefaultDisplayType('n2', 'f2', 'v1\nv2')).to.equal('multiLine');
      App.config.isContentProperty.restore();
    });
    it('returns custom displayType for FALCON oozie-site properties', function() {
      sinon.stub(App.config, 'isContentProperty', function () {return false});
      expect(App.config.getDefaultDisplayType('n2', 'oozie-site.xml', 'v1\nv2', 'FALCON')).to.equal('custom');
      App.config.isContentProperty.restore();
    });
  });

  describe('#getDefaultDisplayName', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'getConfigTagFromFileName', function(fName) {return fName} );
    });
    afterEach(function() {
      App.config.getConfigTagFromFileName.restore();
    });

    it('returns name', function() {
      sinon.stub(App.config, 'isContentProperty', function() {return false} );
      expect(App.config.getDefaultDisplayName('name')).to.equal('name');
      App.config.isContentProperty.restore();
    });
    it('returns name for env content', function() {
      sinon.stub(App.config, 'isContentProperty', function() {return true} );
      expect(App.config.getDefaultDisplayName('name', 'fileName')).to.equal('fileName template');
      App.config.isContentProperty.restore();
    });
  });

  describe('#isContentProperty', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'getConfigTagFromFileName', function(fName) {return fName} );
    });
    afterEach(function() {
      App.config.getConfigTagFromFileName.restore();
    });
    var tests = [
      {
        name: 'content',
        fileName: 'something-env',
        tagEnds: null,
        res: true,
        m: 'returns true as it\'s content property'
      },
      {
        name: 'content',
        fileName: 'something-any-end',
        tagEnds: ['-any-end'],
        res: true,
        m: 'returns true as it\'s content property with specific fileName ending'
      },
      {
        name: 'notContent',
        fileName: 'something-env',
        tagEnds: ['-env'],
        res: false,
        m: 'returns false as name is not content'
      },
      {
        name: 'content',
        fileName: 'something-env1',
        tagEnds: ['-env'],
        res: false,
        m: 'returns false as fileName is not correct'
      }
    ].forEach(function(t) {
        it(t.m, function() {
          expect(App.config.isContentProperty(t.name, t.fileName, t.tagEnds)).to.equal(t.res);
        });
      });
  });

  describe('#formatValue', function() {
    it('formatValue for componentHosts', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'componentHosts', value: "['h1','h2']"});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.eql(['h1','h2']);
    });

    it('formatValue for int', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'int', value: '4.0'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('4');
    });

    it('formatValue for int with m', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'int', value: '4m'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('4');
    });

    it('formatValue for float', function () {
      var serviceConfigProperty = Em.Object.create({'displayType': 'float', value: '0.40'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('0.4');
    });

    it('formatValue for kdc_type', function () {
      var serviceConfigProperty = Em.Object.create({'name': 'kdc_type', value: 'mit-kdc'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal(Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'));
    });

    it('don\'t format value', function () {
      var serviceConfigProperty = Em.Object.create({'name': 'any', displayType: 'any', value: 'any'});
      expect(App.config.formatPropertyValue(serviceConfigProperty)).to.equal('any');
    });
  });

  describe('#getPropertyIfExists', function() {
    [
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someProperty: '1' },
        secondObject: { someProperty: '2' },
        res: '1',
        m: 'use value from first object'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someOtherProperty: '1' },
        secondObject: { someProperty: '2' },
        res: '2',
        m: 'use value from second object'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        firstObject: { someOtherProperty: '1' },
        secondObject: { someOtherProperty: '2' },
        res: 'default',
        m: 'use default value'
      },
      {
        propertyName: 'someProperty',
        defaultValue: 'default',
        res: 'default',
        m: 'use default value'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: false },
        secondObject: { someProperty: true },
        res: false,
        m: 'use value from first object, check booleans'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: 0 },
        secondObject: { someProperty: 1 },
        res: 0,
        m: 'use value from first object, check 0'
      },
      {
        propertyName: 'someProperty',
        defaultValue: true,
        firstObject: { someProperty: '' },
        secondObject: { someProperty: '1' },
        res: '',
        m: 'use value from first object, check empty string'
      }
    ].forEach(function (t) {
        it(t.m, function () {
          expect(App.config.getPropertyIfExists(t.propertyName, t.defaultValue, t.firstObject, t.secondObject)).to.equal(t.res);
        })
      });
  });

  describe('#createDefaultConfig', function() {
    before(function () {
      sinon.stub(App.config, 'getDefaultDisplayName', function () {
        return 'pDisplayName';
      });
      sinon.stub(App.config, 'getDefaultDisplayType', function () {
        return 'pDisplayType';
      });
      sinon.stub(App.config, 'getDefaultCategory', function () {
        return 'pCategory';
      });
      sinon.stub(App.config, 'getIsSecure', function () {
        return false;
      });
      sinon.stub(App.config, 'getDefaultIsShowLabel', function () {
        return true;
      });
      sinon.stub(App.config, 'shouldSupportFinal', function () {
        return true;
      });
    });

    after(function () {
      App.config.getDefaultDisplayName.restore();
      App.config.getDefaultDisplayType.restore();
      App.config.getDefaultCategory.restore();
      App.config.getIsSecure.restore();
      App.config.getDefaultIsShowLabel.restore();
      App.config.shouldSupportFinal.restore();
    });

    var res = {
      /** core properties **/
      name: 'pName',
      filename: 'pFileName',
      value: '',
      savedValue: null,
      isFinal: false,
      savedIsFinal: null,
      /** UI and Stack properties **/
      recommendedValue: null,
      recommendedIsFinal: null,
      supportsFinal: true,
      serviceName: 'pServiceName',
      displayName: 'pDisplayName',
      displayType: 'pDisplayType',
      description: null,
      category: 'pCategory',
      isSecureConfig: false,
      showLabel: true,
      isVisible: true,
      isUserProperty: false,
      isRequired: true,
      group: null,
      isRequiredByAgent:  true,
      isReconfigurable: true,
      unit: null,
      hasInitialValue: false,
      isOverridable: true,
      index: Infinity,
      dependentConfigPattern: null,
      options: null,
      radioName: null,
      belongsToService: [],
      widgetType: null
    };
    it('create default config object', function () {
      expect(App.config.createDefaultConfig('pName', 'pServiceName', 'pFileName', true)).to.eql(res);
    });
    it('runs proper methods', function () {
      expect(App.config.getDefaultDisplayName.calledWith('pName', 'pFileName')).to.be.true;
      expect(App.config.getDefaultDisplayType.calledWith('pName', 'pFileName', '')).to.be.true;
      expect(App.config.getDefaultCategory.calledWith(true, 'pFileName')).to.be.true;
      expect(App.config.getIsSecure.calledWith('pName')).to.be.true;
      expect(App.config.getDefaultIsShowLabel.calledWith('pName', 'pFileName')).to.be.true;
      expect(App.config.shouldSupportFinal.calledWith('pServiceName', 'pFileName')).to.be.true;
    });
  });

  describe('#mergeStackConfigsWithUI', function() {
    beforeEach(function() {
      sinon.stub(App.config, 'getPropertyIfExists', function(key, value) {return 'res_' + value});
    });

    afterEach(function() {
      App.config.getPropertyIfExists.restore();
    });

    var template = {
      name: 'pName',
      filename: 'pFileName',
      value: 'pValue',
      savedValue: 'pValue',
      isFinal: true,
      savedIsFinal: true,

      serviceName: 'pServiceName',
      displayName: 'pDisplayName',
      displayType: 'pDisplayType',
      category: 'pCategory'
    };

    var result = {
      name: 'pName',
      filename: 'pFileName',
      value: 'pValue',
      savedValue: 'pValue',
      isFinal: true,
      savedIsFinal: true,

      serviceName: 'res_pServiceName',
      displayName: 'res_pDisplayName',
      displayType: 'res_pDisplayType',
      category: 'res_pCategory'
    };

    it('called generate property object', function () {
      expect(App.config.mergeStaticProperties(template, {}, {})).to.eql(result);
    });
  });

  describe('#createHostNameProperty', function() {
    it('create host property', function() {
      expect(App.config.createHostNameProperty('service1', 'component1', ['host1'], Em.Object.create({
        isMultipleAllowed: false,
        displayName: 'display name'
      }))).to.eql({
          "name": 'component1_host',
          "displayName": 'display name host',
          "value": ['host1'],
          "recommendedValue": ['host1'],
          "description": "The host that has been assigned to run display name",
          "displayType": "componentHost",
          "isOverridable": false,
          "isRequiredByAgent": false,
          "serviceName": 'service1',
          "filename": "service1-site.xml",
          "category": 'component1',
          "index": 0
        })
    });

    it('create hosts property', function() {
      expect(App.config.createHostNameProperty('service1', 'component1', ['host1'], Em.Object.create({
        isMultipleAllowed: true,
        displayName: 'display name'
      }))).to.eql({
          "name": 'component1_hosts',
          "displayName": 'display name host',
          "value": ['host1'],
          "recommendedValue": ['host1'],
          "description": "The hosts that has been assigned to run display name",
          "displayType": "componentHosts",
          "isOverridable": false,
          "isRequiredByAgent": false,
          "serviceName": 'service1',
          "filename": "service1-site.xml",
          "category": 'component1',
          "index": 0
        })
    });
  })
});
