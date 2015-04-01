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

/**
 * Provide methods for config-versions loading from server and saving them into models
 *
 * @type {Em.Mixin}
 */
App.VersionsMappingMixin = Em.Mixin.create({

  /**
   * load config groups
   * @param {string} [serviceName=null]
   * @param {number} [configGroupId=null]
   * @param {number} [configVersion=null]
   * @param {boolean} [isForCompare=false]
   * @returns {$.ajax}
   * @method loadConfigVersions
   */
  loadConfigVersions: function (serviceName, configGroupId, configVersion, isForCompare) {
    var info = this._generateAjaxDataForVersions(serviceName, configGroupId, configVersion, isForCompare);
    return App.ajax.send($.extend({sender: this, success: 'saveConfigVersionsToModel'}, info));
  },

  /**
   * Generate ajax info
   * @param {string} [serviceName=null]
   * @param {number} [configGroupId=null]
   * @param {number} [configVersion=null]
   * @param {boolean} [isForCompare=false]
   * @returns {{name: string, data: {}}}
   * @private
   * @method generateAjaxDataForVersions
   */
  _generateAjaxDataForVersions: function (serviceName, configGroupId, configVersion, isForCompare) {
    var result = {
      name: 'configs.config_versions.load.all.min',
      data: {}
    };
    if (serviceName) {
      result.data.serviceName = serviceName;
      if (configVersion) {
        result.name = 'configs.config_versions.load';
        result.data.configVersion = configVersion;
        result.data.isForCompare = isForCompare;
      }
      else {
        if (configGroupId) {
          result.name = 'configs.config_versions.load.group';
          result.data.configGroupId = configGroupId;
        }
        else {
          result.name = 'configs.config_versions.load.service.min';
        }
      }
    }
    return result;
  },

  /**
   *
   * @param {string[]} serviceNames
   * @returns {$.ajax}
   * @method loadConfigCurrentVersions
   */
  loadConfigCurrentVersions: function (serviceNames) {
    Em.assert('`serviceNames` should not be empty array', Em.isArray(serviceNames) && serviceNames.length > 0);
    return App.ajax.send({
      name: 'configs.config_versions.load.current_versions',
      sender: this,
      data: {
        serviceNames: serviceNames.join(",")
      },
      success: '_saveConfigVersionsToModel'
    });
  },

  /**
   * Runs <code>configGroupsMapper<code>
   * @param {object} data
   * @param {object} opt
   * @param {object} params
   * @method _saveConfigVersionsToModel
   * @private
   */
  _saveConfigVersionsToModel: function (data, opt, params) {
    App.configVersionsMapper.map(data, params.isForCompare);
  }

});