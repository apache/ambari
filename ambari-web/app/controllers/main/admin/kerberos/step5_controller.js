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
var stringUtils = require('utils/string_utils');
var fileUtils = require('utils/file_utils');

App.KerberosWizardStep5Controller = App.KerberosProgressPageController.extend({
  name: 'kerberosWizardStep5Controller',
  csvData: [],

  submit: function() {
    App.router.send('next');
  },

  /**
   * get CSV data from the server
   */
  getCSVData: function (skipDownload) {
    App.ajax.send({
      name: 'admin.kerberos.cluster.csv',
      sender: this,
      data: {
        'skipDownload': skipDownload
      },
      success: 'getCSVDataSuccessCallback',
      error: 'getCSVDataSuccessCallback'
    })
  },

  /**
   * get CSV data from server success callback
   */
  getCSVDataSuccessCallback: function (data, opt, params) {
    this.set('csvData', this.prepareCSVData(data.split('\n')));
    if(!Em.get(params, 'skipDownload')){
      fileUtils.downloadTextFile(stringUtils.arrayToCSV(this.get('csvData')), 'csv', 'kerberos.csv');
    }
  },

  prepareCSVData: function (array) {
    for (var i = 0; i < array.length; i += 1) {
      array[i] = array[i].split(',');
    }

    return array;
  },

  /**
   * Send request to post kerberos descriptor
   * @param kerberosDescriptor
   * @returns {$.ajax|*}
   */
  postKerberosDescriptor: function (kerberosDescriptor) {
    return App.ajax.send({
      name: 'admin.kerberos.cluster.artifact.create',
      sender: this,
      data: {
        artifactName: 'kerberos_descriptor',
        data: {
          artifact_data: kerberosDescriptor
        }
      }
    });
  },

  /**
   * Send request to update kerberos descriptor
   * @param kerberosDescriptor
   * @returns {$.ajax|*}
   */
  putKerberosDescriptor: function (kerberosDescriptor) {
    return App.ajax.send({
      name: 'admin.kerberos.cluster.artifact.update',
      sender: this,
      data: {
        artifactName: 'kerberos_descriptor',
        data: {
          artifact_data: kerberosDescriptor
        }
      },
      success: 'unkerberizeCluster',
      error: 'unkerberizeCluster'
    });
  },

  /**
   * Send request to unkerberisze cluster
   * @returns {$.ajax}
   */
  unkerberizeCluster: function () {
    return App.ajax.send({
      name: 'admin.unkerberize.cluster',
      sender: this,
      success: 'goToNextStep',
      error: 'goToNextStep'
    });
  },


  goToNextStep: function() {
    this.clearStage();
    App.router.transitionTo('step5');
  },

  isSubmitDisabled: function () {
    return !["COMPLETED", "FAILED"].contains(this.get('status'));
  }.property('status'),

  confirmProperties: function () {
    var kdc_type = App.router.get('kerberosWizardController.content.kerberosOption'),
        filterObject = [
          {
            key: Em.I18n.t('admin.kerberos.wizard.step1.option.kdc'),
            properties: ['kdc_type', 'kdc_host', 'realm', 'executable_search_paths']
          },
          {
            key: Em.I18n.t('admin.kerberos.wizard.step1.option.ad'),
            properties: ['kdc_type', 'kdc_host', 'realm', 'ldap_url', 'container_dn', 'executable_search_paths']
          },
          {
            key: Em.I18n.t('admin.kerberos.wizard.step1.option.manual'),
            properties: ['kdc_type', 'realm', 'executable_search_paths']
          }
        ],
        kdcTypeProperties = filterObject.filter(function (item) {
          return item.key === kdc_type;
        }),
        filterBy = kdcTypeProperties.length ? kdcTypeProperties[0].properties : [];
    return App.router.kerberosWizardController.content.serviceConfigProperties.filter(function (item) {
      return filterBy.contains(item.name);
    }).map(function (item) {
      item['label'] = Em.I18n.t('admin.kerberos.wizard.step5.' + item['name'] + '.label');
      return item;
    });
  }.property('App.router.kerberosWizardController.content.@each.serviceConfigProperties')
});
