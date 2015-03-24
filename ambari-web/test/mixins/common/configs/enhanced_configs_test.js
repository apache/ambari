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

describe('App.EnhancedConfigsMixin', function() {

  var mixinObject =  Em.Controller.extend(App.EnhancedConfigsMixin, {});
  var instanceObject = mixinObject.create({});
  describe('#getFileNamesToSave()', function() {

    beforeEach(function() {
      App.resetDsStoreTypeMap(App.ConfigProperty);
    });

    it('returns file names that was changed', function() {
      App.ConfigProperty.createRecord({id: 'p1_c1', value:'1', defaultValue: '2', fileName: 'file1'});
      App.ConfigProperty.createRecord({id: 'p2_c1', value:'1', defaultValue: '1', fileName: 'file2'});
      expect(instanceObject.getFileNamesToSave(['file3'])).to.eql(['file1','file3'])
    });

    it('returns file names that was changed by adding property', function() {
      App.ConfigProperty.createRecord({id: 'p1_c1', value:'1', defaultValue: '1', fileName: 'file1', isNotSaved: false});
      App.ConfigProperty.createRecord({id: 'p2_c1', value:'1', defaultValue: '1', fileName: 'file2', isNotSaved: true});
      expect(instanceObject.getFileNamesToSave(['file3'])).to.eql(['file2','file3'])
    });
  });

  describe('#allowSaveSite()', function() {

    beforeEach(function() {
      instanceObject.set('content', {});
    });

    it('returns true by default', function() {
      expect(instanceObject.allowSaveSite('some-site')).to.be.true
    });

    it('returns false for mapred-queue-acls.xml', function() {
      expect(instanceObject.allowSaveSite('mapred-queue-acls.xml')).to.be.false
    });

    it('returns false for core-site but not proper service', function() {
      instanceObject.set('content.serviceName', 'ANY');
      expect(instanceObject.allowSaveSite('core-site.xml')).to.be.false
    });

    it('returns true for core-site and proper service', function() {
      instanceObject.set('content.serviceName', 'HDFS');
      expect(instanceObject.allowSaveSite('core-site.xml')).to.be.true
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
});

