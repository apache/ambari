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

require('mixins/common/kdc_credentials_controller_mixin');

var App = require('app');
var credentialsUtils = require('utils/credentials');

var mixedObject;

describe('App.KDCCredentialsControllerMixin', function() {

  beforeEach(function() {
    mixedObject = Em.Object.create(App.KDCCredentialsControllerMixin);
  });

  afterEach(function() {
    mixedObject.destroy();
  });

  describe('#initilizeKDCStoreProperties', function() {
    [
      {
        isStorePersisted: true,
        e: {
          isEditable: true,
          hintMessage: Em.I18n.t('admin.kerberos.credentials.store.hint.supported')
        },
        message: 'Persistent store available, config should be editable, and appropriate hint shown'
      },
      {
        isStorePersisted: false,
        e: {
          isEditable: false,
          hintMessage: Em.I18n.t('admin.kerberos.credentials.store.hint.not.supported')
        },
        message: 'Only temporary store available, config should be disabled, and appropriate hint shown'
      }
    ].forEach(function(test) {
      it(test.message, function() {
        var configs = [],
            config;
        mixedObject.reopen({
          isStorePersisted: function() {
            return test.isStorePersisted;
          }.property()
        });
        mixedObject.initilizeKDCStoreProperties(configs);
        config = configs.findProperty('name', 'persist_credentials');
        Em.keys(test.e).forEach(function(key) {
          assert.equal(Em.get(config, key), test.e[key], 'validate attribute: ' + key);
        });
      });
    });
  });

  describe('#createKDCCredentials', function() {
    var createConfig = function(name, value) {
      return App.ServiceConfigProperty.create({
        name: name,
        value: value
      });
    };
    [
      {
        configs: [
          createConfig('admin_password', 'admin'),
          createConfig('admin_principal', 'admin/admin'),
          createConfig('persist_credentials', 'true')
        ],
        e: [
          'testName',
          'kdc.admin.credential',
          {
            type: 'persisted',
            key: 'admin',
            principal: 'admin/admin'
          }
        ],
        message: 'Save Admin credentials checkbox checked, credentials should be saved as `persisted`'
      },
      {
        configs: [
          createConfig('admin_password', 'admin'),
          createConfig('admin_principal', 'admin/admin'),
          createConfig('persist_credentials', 'false')
        ],
        e: [
          'testName',
          'kdc.admin.credential',
          {
            type: 'temporary',
            key: 'admin',
            principal: 'admin/admin'
          }
        ],
        message: 'Save Admin credentials checkbox un-checked, credentials should be saved as `temporary`'
      },
      {
        configs: [
          createConfig('admin_password', 'admin'),
          createConfig('admin_principal', 'admin/admin'),
          createConfig('persist_credentials', 'false')
        ],
        e: [
          'testName',
          'kdc.admin.credential',
          {
            type: 'temporary',
            key: 'admin',
            principal: 'admin/admin'
          }
        ],
        credentialWasSaved: true,
        message: 'Save Admin credentials checkbox checked, credential was saved, credentials should be saved as `temporary`, #updateKDCCredentials should be called'
      }
    ].forEach(function(test) {
      it(test.message, function() {
        sinon.stub(App, 'get').withArgs('clusterName').returns('testName');
        sinon.stub(credentialsUtils, 'createCredentials', function() {
          if (test.credentialWasSaved) {
            return $.Deferred().reject().promise();
          } else {
            return $.Deferred().resolve().promise();
          }
        });
        if (test.credentialWasSaved) {
          sinon.stub(credentialsUtils, 'updateCredentials', function() {
            return $.Deferred().resolve().promise();
          });
        }

        mixedObject.reopen({
          isStorePersisted: function() {
            return true;
          }.property()
        });
        mixedObject.createKDCCredentials(test.configs);
        assert.isTrue(credentialsUtils.createCredentials.calledOnce, 'credentialsUtils#createCredentials called');
        assert.deepEqual(credentialsUtils.createCredentials.args[0], test.e, 'credentialsUtils#createCredentials called with correct arguments');
        credentialsUtils.createCredentials.restore();
        if (test.credentialWasSaved) {
          assert.isTrue(credentialsUtils.updateCredentials.calledOnce, 'credentialUtils#updateCredentials called');
          assert.deepEqual(credentialsUtils.updateCredentials.args[0], test.e, 'credentialUtils#updateCredentials called with correct arguments');
          credentialsUtils.updateCredentials.restore();
        }
        App.get.restore();
      });
    });
  });

});
