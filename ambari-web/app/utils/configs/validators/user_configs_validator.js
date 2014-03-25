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
var validator = require('utils/validator');
require('utils/configs/validators/service_configs_validator');

App.userConfigsValidator = App.ServiceConfigsValidator.create({
  /**
   * List of the configs that should be validated
   */
  configValidators: {
    'hdfs_user': 'validateUserValue',
    'mapred_user': 'validateUserValue',
    'yarn_user': 'validateUserValue',
    'hbase_user': 'validateUserValue',
    'hive_user': 'validateUserValue',
    'hcat_user': 'validateUserValue',
    'webhcat_user': 'validateUserValue',
    'oozie_user': 'validateUserValue',
    'falcon_user': 'validateUserValue',
    'storm_user': 'validateUserValue',
    'zk_user': 'validateUserValue',
    'gmetad_user': 'validateUserValue',
    'gmond_user': 'validateUserValue',
    'nagios_user': 'validateUserValue',
    'smokeuser': 'validateUserValue'
  },

  validateUserValue: function(config) {
    var recomendedDefault = this.get('recommendedDefaults')[config.get('name')];
    var defaultValue = Em.isNone(recomendedDefault) ? config.get('defaultValue') : recomendedDefault;
    Em.assert('validateUserValue: User\'s default value can\'t be null or undefined', !Em.isNone(defaultValue));
    var currentValue = config.get('value');
    if (!validator.isValidUserName(currentValue)) {
      return Em.I18n.t('users.userName.validationFail');
    }
    return null;
  }
});
