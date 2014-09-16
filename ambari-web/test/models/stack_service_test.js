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

require('models/stack_service');

describe('App.StackService', function () {

  App.store.load(App.StackService, {
    id: 'S1'
  });

  var ss = App.StackService.find('S1');
  ss.reopen({
    serviceComponents: []
  });

  describe('#isDFS', function () {
    it('service name is "SERVICE"', function () {
      ss.set('serviceName', 'SERVICE');
      ss.propertyDidChange('isDFS');
      expect(ss.get('isDFS')).to.be.false;
    });
    it('service name is "HDFS"', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('isDFS');
      expect(ss.get('isDFS')).to.be.true;
    });
    it('service name is "GLUSTERFS"', function () {
      ss.set('serviceName', 'GLUSTERFS');
      ss.propertyDidChange('isDFS');
      expect(ss.get('isDFS')).to.be.true;
    });
  });

  describe('#isPrimaryDFS', function () {
    it('service name is "SERVICE"', function () {
      ss.set('serviceName', 'SERVICE');
      ss.propertyDidChange('isPrimaryDFS');
      expect(ss.get('isPrimaryDFS')).to.be.false;
    });
    it('service name is "HDFS"', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('isPrimaryDFS');
      expect(ss.get('isPrimaryDFS')).to.be.true;
    });
  });

  describe('#configTypesRendered', function () {
    ss.set('configTypes', {
      'core-site': {},
      'hdfs-site': {},
      'oozie-site': {}
    });
    it('service name is "SERVICE"', function () {
      ss.set('serviceName', 'SERVICE');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {},'hdfs-site': {}, 'oozie-site': {}});
    });
    it('service name is "GLUSTERFS"', function () {
      ss.set('serviceName', 'GLUSTERFS');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {},'hdfs-site': {}, 'oozie-site': {}});
    });
    it('service name is "HDFS"', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {}, 'hdfs-site': {}, 'oozie-site': {}});
    });
    it('service name is "FALCON"', function () {
      ss.set('serviceName', 'FALCON');
      ss.propertyDidChange('configTypesRendered');
      expect(ss.get('configTypesRendered')).to.eql({'core-site': {}, 'hdfs-site': {}});
    });
  });

  describe('#displayNameOnSelectServicePage', function () {
    it('No coSelectedServices', function () {
      ss.set('serviceName', 'HDFS');
      ss.set('displayName', 'HDFS');
      ss.propertyDidChange('displayNameOnSelectServicePage');
      expect(ss.get('displayNameOnSelectServicePage')).to.equal('HDFS');
    });
    it('Present coSelectedServices', function () {
      ss.set('serviceName', 'YARN');
      ss.set('displayName', 'YARN');
      ss.propertyDidChange('displayNameOnSelectServicePage');
      expect(ss.get('displayNameOnSelectServicePage')).to.equal('YARN + MapReduce2');
    });
  });

  describe('#isHiddenOnSelectServicePage', function () {
    var testCases = [
      {
        serviceName: 'HDFS',
        result: false
      },
      {
        serviceName: 'MAPREDUCE2',
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it('service name - ' + test.serviceName, function () {
        ss.set('serviceName', test.serviceName);
        ss.propertyDidChange('isHiddenOnSelectServicePage');
        expect(ss.get('isHiddenOnSelectServicePage')).to.equal(test.result);
      });
    });
  });

  describe('#isMonitoringService', function () {
    var testCases = [
      {
        serviceName: 'HDFS',
        result: false
      },
      {
        serviceName: 'NAGIOS',
        result: true
      },
      {
        serviceName: 'GANGLIA',
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it('service name - ' + test.serviceName, function () {
        ss.set('serviceName', test.serviceName);
        ss.propertyDidChange('isMonitoringService');
        expect(ss.get('isMonitoringService')).to.equal(test.result);
      });
    });
  });

  describe('#hasClient', function () {
    it('No client serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasClient');
      expect(ss.get('hasClient')).to.be.false;
    });
    it('Has client serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isClient: true})]);
      ss.propertyDidChange('hasClient');
      expect(ss.get('hasClient')).to.be.true;
    });
  });

  describe('#hasMaster', function () {
    it('No master serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasMaster');
      expect(ss.get('hasMaster')).to.be.false;
    });
    it('Has master serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isMaster: true})]);
      ss.propertyDidChange('hasMaster');
      expect(ss.get('hasMaster')).to.be.true;
    });
  });

  describe('#hasSlave', function () {
    it('No slave serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.propertyDidChange('hasSlave');
      expect(ss.get('hasSlave')).to.be.false;
    });
    it('Has slave serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isSlave: true})]);
      ss.propertyDidChange('hasSlave');
      expect(ss.get('hasSlave')).to.be.true;
    });
  });

  describe('#isClientOnlyService', function () {
    it('Has not only client serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isSlave: true}), Em.Object.create({isClient: true})]);
      ss.propertyDidChange('isClientOnlyService');
      expect(ss.get('isClientOnlyService')).to.be.false;
    });
    it('Has only client serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({isClient: true})]);
      ss.propertyDidChange('isClientOnlyService');
      expect(ss.get('isClientOnlyService')).to.be.true;
    });
  });

  describe('#isNoConfigTypes', function () {
    it('configTypes is null', function () {
      ss.set('configTypes', null);
      ss.propertyDidChange('isNoConfigTypes');
      expect(ss.get('isNoConfigTypes')).to.be.true;
    });
    it('configTypes is empty', function () {
      ss.set('configTypes', {});
      ss.propertyDidChange('isNoConfigTypes');
      expect(ss.get('isNoConfigTypes')).to.be.true;
    });
    it('configTypes is correct', function () {
      ss.set('configTypes', {'key': {}});
      ss.propertyDidChange('isNoConfigTypes');
      expect(ss.get('isNoConfigTypes')).to.be.false;
    });
  });

  describe('#customReviewHandler', function () {
    it('service name is HDFS', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('customReviewHandler');
      expect(ss.get('customReviewHandler')).to.be.undefined;
    });
    it('service name is HIVE', function () {
      ss.set('serviceName', 'HIVE');
      ss.propertyDidChange('customReviewHandler');
      expect(ss.get('customReviewHandler')).to.eql({
        "Database": "loadHiveDbValue"
      });
    });
  });

  describe('#defaultsProviders', function () {
    it('service name is HDFS', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('defaultsProviders');
      expect(ss.get('defaultsProviders')).to.be.undefined;
    });
    it('service name is HIVE', function () {
      ss.set('serviceName', 'HIVE');
      ss.propertyDidChange('defaultsProviders');
      expect(ss.get('defaultsProviders')).to.not.be.empty;
    });
  });

  describe('#configsValidator', function () {
    it('service name is HDFS', function () {
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configsValidator');
      expect(ss.get('configsValidator')).to.be.undefined;
    });
    it('service name is HIVE', function () {
      ss.set('serviceName', 'HIVE');
      ss.propertyDidChange('configsValidator');
      expect(ss.get('configsValidator')).to.not.be.empty;
    });
  });

  describe('#configCategories', function () {
    it('HDFS service with no serviceComponents', function () {
      ss.set('serviceComponents', []);
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configCategories');
      expect(ss.get('configCategories').mapProperty('name')).to.eql([
        "General",
        "Advanced",
        "Advanced key",
        "Custom key"
      ]);
    });
    it('HDFS service with DATANODE serviceComponents', function () {
      ss.set('serviceComponents', [Em.Object.create({componentName: 'DATANODE'})]);
      ss.set('serviceName', 'HDFS');
      ss.propertyDidChange('configCategories');
      expect(ss.get('configCategories').mapProperty('name')).to.eql([
        "DATANODE",
        "General",
        "Advanced",
        "Advanced key",
        "Custom key"]);
    });
  });


});
