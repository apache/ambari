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
var credentialsUtils = require('utils/credentials');

App.KDCCredentialsControllerMixin = Em.Mixin.create({

  /**
   * Alias name used to store KDC credentials
   *
   * @type {string}
   */
  credentialAlias: credentialsUtils.ALIAS.KDC_CREDENTIALS,

  /**
   * Returns <code>true</code> if persisted secure storage available.
   *
   * @type {boolean}
   */
  isStorePersisted: function() {
    return App.get('isCredentialStorePersistent');
  }.property('App.isCredentialStorePersistent'),

  /**
   * List of required UI-only properties needed for storing KDC credentials
   *
   * @type {object[]}
   */
  credentialsStoreConfigs: [
    {
      name: 'persist_credentials',
      displayType: 'checkbox',
      value: 'false',
      recommendedValue: 'false',
      supportsFinal: false,
      recommendedIsFinal: false,
      displayName: Em.I18n.t('admin.kerberos.credentials.store.label'),
      category: 'Kadmin',
      isRequired: false,
      isRequiredByAgent: false,
      hintMessage: false,
      rightSideLabel: true,
      isEditable: true,
      index: 3
    }
  ],

  /**
   * @param {object} resource resource info to set e.g.
   * <code>
   * {
   *   principal: "USERNAME",
   *   key: "SecretKey",
   *   type: "persisted"
   * }
   * </code>
   *
   * Where:
   * <ul>
   *   <li>principal: the principal (or username) part of the credential to store</li>
   *   <li>key: the secret key part of the credential to store</li>
   *   <li>type: declares the storage facility type: "persisted" or "temporary"</li>
   * </ul>
   * @returns {$.Deferred} promise object
   */
  createKDCCredentials: function(configs) {
    var resource = credentialsUtils.createCredentialResource(
      Em.getWithDefault(configs.findProperty('name', 'admin_principal') || {}, 'value', ''),
      Em.getWithDefault(configs.findProperty('name', 'admin_password') || {}, 'value', ''),
      this._getStorageTypeValue(configs));
    return credentialsUtils.createOrUpdateCredentials(App.get('clusterName'), this.get('credentialAlias'), resource);
  },

  /**
   * Remove KDC credentials
   *
   * @returns {$.Deferred} promise object
   */
  removeKDCCredentials: function() {
    return credentialsUtils.removeCredentials(App.get('clusterName'), this.get('credentialAlias'));
  },

  /**
   * @see createKDCCredentials
   * @param {object} resource
   * @returns {$.Deferred} promise object
   */
  updateKDCCredentials: function(resource) {
    return credentialsUtils.updateCredentials(App.get('clusterName'), this.get('credentialAlias'), resource);
  },

  /**
   * Generate additional properties regarding KDC credential storage
   *
   * @param {App.ServiceConfigProperty[]} configs list of configs
   */
  initilizeKDCStoreProperties: function(configs) {
    var self = this;
    this.get('credentialsStoreConfigs').forEach(function(item) {
      var configObject = App.config.createDefaultConfig(item.name, 'KERBEROS', 'krb5-conf.xml', false, false);
      $.extend(configObject, item);
      if (item.name === 'persist_credentials') {
        if (self.get('isStorePersisted')) {
          configObject.hintMessage = Em.I18n.t('admin.kerberos.credentials.store.hint.supported');
        } else {
          configObject.hintMessage = Em.I18n.t('admin.kerberos.credentials.store.hint.not.supported');
          configObject.isEditable = false;
        }
      }
      configs.pushObject(configObject);
    });
  },

  /**
   * Return storage type e.g. <b>temporary</b>, <b>persisted</b>
   *
   * @param {App.ServiceConfigProperty[]} configs configs array from step configs
   * @returns {string} storage type value
   */
  _getStorageTypeValue: function(configs) {
    if (this.get('isStorePersisted')) {
      return Em.getWithDefault(configs.findProperty('name', 'persist_credentials') || {}, 'value', '') === "true" ?
        credentialsUtils.STORE_TYPES.PERSISTENT :
        credentialsUtils.STORE_TYPES.TEMPORARY;
    }
    return credentialsUtils.STORE_TYPES.TEMPORARY;
  }

});
