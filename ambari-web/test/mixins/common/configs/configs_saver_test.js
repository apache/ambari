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

describe('App.ConfigsSaverMixin', function() {
  var mixinObject =  Em.Controller.extend(App.ConfigsSaverMixin, {});
  var instanceObject = mixinObject.create({});

  describe('#allowSaveSite()', function() {
    var stackServices = [
      Em.Object.create({
        serviceName: 'HDFS',
        serviceType: 'NONHCFS'
      }),
      Em.Object.create({
        serviceName: 'S1',
        serviceType: 'HCFS'
      }),
      Em.Object.create({
        serviceName: 'S2',
        serviceType: 'NONHCFS'
      }),
      Em.Object.create({
        serviceName: 'S3'
      })
    ];
    beforeEach(function() {
      instanceObject.set('content', {});
      sinon.stub(App.StackService, 'find', function () {
        return stackServices;
      });
    });

    afterEach(function() {
      App.StackService.find.restore();
    });

    it('returns true by default', function() {
      expect(instanceObject.allowSaveSite('some-site')).to.be.true
    });

    it('returns true for core-site and proper service', function() {
      instanceObject.set('content.serviceName', 'HDFS');
      expect(instanceObject.allowSaveSite('core-site.xml')).to.be.true
    });

    it('returns true for core-site and serviceType HCFS', function() {
      instanceObject.set('content.serviceName', 'S1');
      expect(instanceObject.allowSaveSite('core-site.xml')).to.be.true
    });

    it('returns false for core-site and serviceType not HCFS', function() {
      instanceObject.set('content.serviceName', 'S2');
      expect(instanceObject.allowSaveSite('core-site.xml')).to.be.false
    });

    it('returns false for core-site but serviceType undefined', function() {
      instanceObject.set('content.serviceName', 'S3');
      expect(instanceObject.allowSaveSite('core-site.xml')).to.be.false
    });

    it('returns false for mapred-queue-acls.xml', function() {
      expect(instanceObject.allowSaveSite('mapred-queue-acls.xml')).to.be.false
    });
  });

  describe('#createDesiredConfig()', function() {
    beforeEach(function() {
      sinon.stub(instanceObject, 'formatValueBeforeSave', function(property) {
        return property.get('value');
      })
    });
    afterEach(function() {
      instanceObject.formatValueBeforeSave.restore();
    });

    it('generates config wil throw error', function() {
      expect(instanceObject.createDesiredConfig.bind(instanceObject)).to.throw(Error, 'assertion failed');
    });

    it('generates config without properties', function() {
      expect(instanceObject.createDesiredConfig('type1', 'version1')).to.eql({
        "type": 'type1',
        "tag": 'version1',
        "properties": {},
        "service_config_version_note": ""
      })
    });

    it('generates config with properties', function() {
      expect(instanceObject.createDesiredConfig('type1', 'version1', [Em.Object.create({name: 'p1', value: 'v1', isRequiredByAgent: true}), Em.Object.create({name: 'p2', value: 'v2', isRequiredByAgent: true})], "note")).to.eql({
        "type": 'type1',
        "tag": 'version1',
        "properties": {
          "p1": 'v1',
          "p2": 'v2'
        },
        "service_config_version_note": 'note'
      })
    });

    it('generates config with properties and skip isRequiredByAgent', function() {
      expect(instanceObject.createDesiredConfig('type1', 'version1', [Em.Object.create({name: 'p1', value: 'v1', isRequiredByAgent: true}), Em.Object.create({name: 'p2', value: 'v2', isRequiredByAgent: false})], "note")).to.eql({
        "type": 'type1',
        "tag": 'version1',
        "properties": {
          p1: 'v1'
        },
        "service_config_version_note": 'note'
      })
    });

    it('generates config with properties and skip service_config_version_note', function() {
      expect(instanceObject.createDesiredConfig('type1', 'version1', [Em.Object.create({name: 'p1', value: 'v1', isRequiredByAgent: true})], "note", true)).to.eql({
        "type": 'type1',
        "tag": 'version1',
        "properties": {
          p1: 'v1'
        }
      })
    });

    it('generates config with final', function() {
      expect(instanceObject.createDesiredConfig('type1', 'version1', [Em.Object.create({name: 'p1', value: 'v1', isFinal: true, isRequiredByAgent: true}), Em.Object.create({name: 'p2', value: 'v2', isRequiredByAgent: true})], "note")).to.eql({
        "type": 'type1',
        "tag": 'version1',
        "properties": {
          p1: 'v1',
          p2: 'v2'
        },
        "properties_attributes": {
          final: {
            'p1': "true"
          }
        },
        "service_config_version_note": 'note'
      })
    })
  });

  describe('#generateDesiredConfigsJSON()', function() {
    beforeEach(function() {
      sinon.stub(instanceObject, 'createDesiredConfig', function(type) {
        return 'desiredConfig_' + type;
      });
      sinon.stub(instanceObject, 'allowSaveSite', function() {
        return true;
      });

    });
    afterEach(function() {
      instanceObject.createDesiredConfig.restore();
      instanceObject.allowSaveSite.restore();
    });

    it('generates empty array as data is missing', function() {
      expect(instanceObject.generateDesiredConfigsJSON()).to.eql([]);
      expect(instanceObject.generateDesiredConfigsJSON(1,1)).to.eql([]);
      expect(instanceObject.generateDesiredConfigsJSON([],[])).to.eql([]);
    });

    it('generates array with desired configs', function() {
      expect(instanceObject.generateDesiredConfigsJSON([Em.Object.create({'name': 'p1', 'fileName': 'f1.xml'})], ['f1'])).to.eql(['desiredConfig_f1']);
      expect(instanceObject.createDesiredConfig).to.be.calledOnce
    })
  });

  describe('#getModifiedConfigs', function () {
    var configs = [
      Em.Object.create({
        name: 'p1',
        filename: 'f1',
        isNotDefaultValue: true,
        value: 'v1'
      }),
      Em.Object.create({
        name: 'p2',
        filename: 'f1',
        isNotDefaultValue: false,
        value: 'v2'
      }),
      Em.Object.create({
        name: 'p3',
        filename: 'f2',
        isNotSaved: true,
        value: 'v4'
      }),
      Em.Object.create({
        name: 'p4',
        filename: 'f3',
        isNotDefaultValue: false,
        isNotSaved: false,
        value: 'v4'
      })
    ];
    it('filter out changed configs', function () {
      expect(instanceObject.getModifiedConfigs(configs).mapProperty('name')).to.eql(['p1','p2','p3']);
      expect(instanceObject.getModifiedConfigs(configs).mapProperty('filename').uniq()).to.eql(['f1','f2']);
    });

    it('filter out changed configs and modifiedFileNames', function () {
      instanceObject.set('modifiedFileNames', ['f3']);
      expect(instanceObject.getModifiedConfigs(configs).mapProperty('name')).to.eql(['p1','p2','p3','p4']);
      expect(instanceObject.getModifiedConfigs(configs).mapProperty('filename').uniq()).to.eql(['f1','f2','f3']);
    });
  });
});

