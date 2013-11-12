/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/validators/service_configs_validator');

App.YARNConfigsValidator = App.ServiceConfigsValidator.create({
  /**
   * List of the configs that should be validated
   */
  configValidators: {
    'yarn.nodemanager.resource.memory-mb': 'yarnNodemanagerResourceMemoryMb',
    'yarn.scheduler.minimum-allocation-mb': 'yarnSchedulerMinimumAllocationMb',
    'yarn.scheduler.maximum-allocation-mb': 'yarnSchedulerMaximumAllocationMb'
  },

  yarnNodemanagerResourceMemoryMb: function(config) {
    return this.validatorLessThenDefaultValue(config);
  },

  yarnSchedulerMinimumAllocationMb: function(config) {
    return this.validatorLessThenDefaultValue(config);
  },

  yarnSchedulerMaximumAllocationMb: function(config) {
    return this.validatorLessThenDefaultValue(config);
  }

});
