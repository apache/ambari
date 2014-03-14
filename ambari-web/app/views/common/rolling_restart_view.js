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
var numberUtils = require('utils/number_utils');

/**
 * View content of the rolling restart dialog.
 *
 * Callers provide the context in which this dialog is invoked.
 */
App.RollingRestartView = Em.View.extend({
  templateName : require('templates/common/rolling_restart_view'),

  /**
   * Component name for components that should be restarted
   * @type {String}
   */
  hostComponentName : null,

  /**
   * Restart only components with <code>staleConfigs</code>
   * @type {bool}
   */
  staleConfigsOnly : false,

  /**
   * We should do rolling restart for components if we run
   * restart for service and service is in Maintenance mode
   * @type {bool}
   */
  skipMaintenance: false,

  /**
   * Count of host components in one batch
   * @type {Number}
   */
  batchSize : -1,

  /**
   * Delay between batches
   * @type {Number}
   */
  interBatchWaitTimeSeconds : -1,

  /**
   * @type {Number}
   */
  tolerateSize : -1,

  /**
   * List of error in batch-request properties
   * @type {Array}
   */
  errors : [],
  /**
   * Set initial values for batch-request properties
   */
  initialize : function() {
    if (this.get('batchSize') == -1 && this.get('interBatchWaitTimeSeconds') == -1 && this.get('tolerateSize') == -1) {
      var restartCount = this.get('restartHostComponents');
      var batchSize = 1;
      if (restartCount > 10) {
        batchSize = Math.ceil(restartCount / 10);
      }
      var tolerateCount = batchSize;
      this.set('batchSize', batchSize);
      this.set('tolerateSize', tolerateCount);
      this.set('interBatchWaitTimeSeconds', 120);
    }
  },

  /**
   * Validate batch-request properties
   * List of errors is saved to <code>errors</code>
   */
  validate : function() {
    var displayName = this.get('hostComponentDisplayName');
    var totalCount = this.get('restartHostComponents.length');
    var bs = this.get('batchSize');
    var ts = this.get('tolerateSize');
    var wait = this.get('interBatchWaitTimeSeconds');
    var errors = [];
    if (totalCount < 1) {
      errors.push(Em.I18n.t('rollingrestart.dialog.msg.noRestartHosts').format(displayName));
    } else {
      var bsError = numberUtils.validateInteger(bs, 1, totalCount);
      var tsError = numberUtils.validateInteger(ts, 0, totalCount);
      if (bsError != null) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.batchsize').format(bsError));
      }
      if (tsError != null) {
        errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.toleratesize').format(tsError));
      }
    }
    var waitError = numberUtils.validateInteger(wait, 0, NaN);
    if (waitError != null) {
      errors.push(Em.I18n.t('rollingrestart.dialog.err.invalid.waitTime').format(waitError));
    }
    this.set('errors', errors);
  }.observes('batchSize', 'interBatchWaitTimeSeconds', 'tolerateSize', 'restartHostComponents', 'hostComponentDisplayName'),

  /**
   * Formatted <code>hostComponentName</code>
   * @type {String}
   */
  hostComponentDisplayName : function() {
    return App.format.role(this.get('hostComponentName'));
  }.property('hostComponentName'),

  /**
   * List of all host components
   * @type {Array}
   */
  allHostComponents : function() {
    return App.HostComponent.find().filterProperty('componentName', this.get('hostComponentName'));
  }.property('hostComponentName'),

  /**
   * List of host components without components in out-of-service state
   * @type {Array}
   */
  nonMaintainanceHostComponents : function() {
    return this.get('allHostComponents').filter(function(item) {
      return item.get('passiveState') == 'OFF';
    });
  }.property('allHostComponents', 'allHostComponents.@each.passiveState'),

  /**
   * List of host components without components in out-of-service state
   * If <code>staleConfigsOnly</code> is true, components with <code>staleConfigs</code> = false are also filtered
   * @type {Array}
   */
  restartHostComponents : function() {
    var hostComponents = null;
    if (this.get('skipMaintenance')) {
      hostComponents = this.get('allHostComponents').filterProperty('host.passiveState','OFF');
    } else {
      hostComponents = this.get('nonMaintainanceHostComponents');
    }
    if (this.get('staleConfigsOnly')) {
      hostComponents = hostComponents.filterProperty('staleConfigs', true);
    }
    return hostComponents;
  }.property('nonMaintainanceHostComponents', 'staleConfigsOnly'),

  /**
   * @type {String}
   */
  restartMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.restart').format(this.get('hostComponentDisplayName'))
  }.property('hostComponentDisplayName'),

  /**
   * @type {String}
   */
  maintainanceMessage : function() {
    var allCount = this.get('allHostComponents.length');
    var nonMaintainCount = this.get('nonMaintainanceHostComponents.length');
    var count = allCount - nonMaintainCount;
    if (count > 0) {
      var name = this.get('hostComponentDisplayName');
      if (count > 1) {
        return Em.I18n.t('rollingrestart.dialog.msg.maintainance.plural').format(count, name)
      }
      return Em.I18n.t('rollingrestart.dialog.msg.maintainance').format(count, name)
    }
    return null;
  }.property('allHostComponents', 'nonMaintainanceHostComponents', 'hostComponentDisplayName'),

  /**
   * @type {String}
   */
  batchSizeMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.componentsAtATime').format(this.get('hostComponentDisplayName'));
  }.property('hostComponentDisplayName'),

  /**
   * @type {String}
   */
  staleConfigsOnlyMessage : function() {
    return Em.I18n.t('rollingrestart.dialog.msg.staleConfigsOnly').format(this.get('hostComponentDisplayName'));
  }.property('hostComponentDisplayName')
});
