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

App.MainAlertDefinitionConfigsController = Em.Controller.extend({

  name: 'mainAlertDefinitionConfigsController',

  /**
   * All configurable properties of alert definition
   * @type {Array}
   */
  configs: [],

  /**
   * Define whether configs are editable
   * binds to property populated in template
   * @type {Boolean}
   */
  canEdit: true,

  /**
   * Define configs view mode (Wizard or Definition Details page)
   * @type {Boolean}
   */
  isWizard: false,

  /**
   * Alert Definition type
   * binding is set in template
   * @type {String}
   */
  alertDefinitionType: '',

  /**
   * Array of displayNames of all services
   * is used for "Service" config options
   * @type {Array}
   */
  allServices: function () {
    return App.Service.find().mapProperty('displayName');
  }.property(),

  /**
   * All possible values for scope propery
   * @type {Array}
   */
  allScopes: ['Any', 'Host', 'Service'],

  /**
   * Array of all aggregate-alerts names
   * @type {Array}
   */
  aggregateAlertNames: function () {
    return App.AggregateAlertDefinition.find().mapProperty('name');
  }.property(),

  /**
   * Change options of "Component", after changing value of "Service" config
   * @method onServiceSelect
   */
  onServiceSelect: function () {
    var serviceProperty = this.get('configs').findProperty('name', 'service');
    if (serviceProperty && serviceProperty.get('value') !== 'Ambari') {
      var componentsProperty = this.get('configs').findProperty('name', 'component');
      componentsProperty.set('options', ['No component'].concat(App.HostComponent.find().filterProperty('service.displayName', serviceProperty.get('value')).mapProperty('displayName').uniq()));
    }
  }.observes('configs.@each.value'),

  /**
   * OnSelect handler for <code>select_type</code> property
   * disable fields related to definition type and set options to select lists
   */
  changeType: function (selectedType) {
    if (selectedType === 'alert_type_service') {
      this.get('configs').findProperty('name', 'service').setProperties({
        isDisabled: false,
        options: this.get('allServices'),
        value: this.get('allServices')[0]
      });
      this.get('configs').findProperty('name', 'component').setProperties({
        isDisabled: false,
        value: 'No component'
      });
      this.get('configs').findProperty('name', 'scope').setProperties({
        isDisabled: false,
        options: this.get('allScopes'),
        value: this.get('allScopes')[0]
      });
    } else {
      this.get('configs').findProperty('name', 'service').setProperties({
        isDisabled: true,
        options: ['Ambari'],
        value: 'Ambari'
      });
      this.get('configs').findProperty('name', 'component').setProperties({
        isDisabled: true,
        options: ['Ambari Agent'],
        value: 'Ambari Agent'
      });
      this.get('configs').findProperty('name', 'scope').setProperties({
        isDisabled: true,
        options: ['Host'],
        value: 'Host'
      });
    }
  },

  /**
   * @return {string|Null}
   * @method getThresholdsProperty
   */
  getThresholdsProperty: function (type, property) {
    var warning = this.get('content.reporting').findProperty('type', type);
    return warning && !Ember.isEmpty(warning.get(property)) ? warning.get(property) : null;
  },

  /**
   * Render array of configs for appropriate alert definition type
   * @method renderConfigs
   */
  renderConfigs: function () {
    var alertDefinitionType = this.get('alertDefinitionType');
    var configs = [];
    switch (alertDefinitionType) {
      case 'PORT':
        configs = this.renderPortConfigs();
        break;
      case 'METRIC':
        configs = this.renderMetricConfigs();
        break;
      case 'WEB':
        configs = this.renderWebConfigs();
        break;
      case 'SCRIPT':
        configs = this.renderScriptConfigs();
        break;
      case 'AGGREGATE':
        configs = this.renderAggregateConfigs();
        break;
      case 'SERVER':
      	configs = this.renderServerConfigs();
      	break;
      case 'RECOVERY':
      	configs = this.renderWebConfigs();
      	break;
      default:
        console.error('Incorrect Alert Definition Type: ', alertDefinitionType);
    }

    configs.setEach('isDisabled', !this.get('canEdit'));

    this.set('configs', configs);
  },

  /**
   * Render config properties for port-type alert definition
   * @method renderPortConfigs
   * @returns {App.AlertConfigProperty[]}
   */
  renderPortConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.OkThreshold.create({
        label: 'Thresholds',
        showInputForValue: false,
        text: isWizard ? '' : this.getThresholdsProperty('ok', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('ok', 'value')
      }),
      App.AlertConfigProperties.Thresholds.WarningThreshold.create(App.AlertConfigProperties.Thresholds.PositiveMixin, {
        valueMetric: 'Seconds',
        text: isWizard ? '' : this.getThresholdsProperty('warning', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('warning', 'value')
      }),
      App.AlertConfigProperties.Thresholds.CriticalThreshold.create(App.AlertConfigProperties.Thresholds.PositiveMixin, {
        valueMetric: 'Seconds',
        text: isWizard ? '' : this.getThresholdsProperty('critical', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('critical', 'value')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for metric-type alert definition
   * @method renderMetricConfigs
   * @returns {App.AlertConfigProperty[]}
   */
  renderMetricConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');
    var units = this.get('content.reporting').findProperty('type','units') ?
      this.get('content.reporting').findProperty('type','units').get('text'): null;

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.OkThreshold.create({
        label: 'Thresholds',
        showInputForValue: false,
        text: isWizard ? '' : this.getThresholdsProperty('ok', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('ok', 'value')
      }),
      App.AlertConfigProperties.Thresholds.WarningThreshold.create({
        valueMetric: units,
        text: isWizard ? '' : this.getThresholdsProperty('warning', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('warning', 'value')
      }),
      App.AlertConfigProperties.Thresholds.CriticalThreshold.create({
        valueMetric: units,
        text: isWizard ? '' : this.getThresholdsProperty('critical', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('critical', 'value')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for web-type alert definition
   * @method renderWebConfigs
   * @returns {App.AlertConfigProperty[]}
   */
  renderWebConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.OkThreshold.create({
        label: 'Thresholds',
        showInputForValue: false,
        text: isWizard ? '' : this.getThresholdsProperty('ok', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('ok', 'value')
      }),
      App.AlertConfigProperties.Thresholds.WarningThreshold.create({
        showInputForValue: false,
        text: isWizard ? '' : this.getThresholdsProperty('warning', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('warning', 'value')
      }),
      App.AlertConfigProperties.Thresholds.CriticalThreshold.create({
        showInputForValue: false,
        text: isWizard ? '' : this.getThresholdsProperty('critical', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('critical', 'value')
      }),
      App.AlertConfigProperties.ConnectionTimeout.create({
        value: alertDefinition.get('uri.connectionTimeout')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for script-type alert definition
   * @method renderScriptConfigs
   * @returns {App.AlertConfigProperty[]}
   */
  renderScriptConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for aggregate-type alert definition
   * @method renderAggregateConfigs
   * @returns {App.AlertConfigProperty[]}
   */
  renderAggregateConfigs: function () {
    var isWizard = this.get('isWizard');
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.OkThreshold.create({
        label: 'Thresholds',
        showInputForValue: false,
        text: isWizard ? '' : this.getThresholdsProperty('ok', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('ok', 'value')
      }),
      App.AlertConfigProperties.Thresholds.WarningThreshold.create(App.AlertConfigProperties.Thresholds.PercentageMixin, {
        text: isWizard ? '' : this.getThresholdsProperty('warning', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('warning', 'value'),
        valueMetric: '%'
      }),
      App.AlertConfigProperties.Thresholds.CriticalThreshold.create(App.AlertConfigProperties.Thresholds.PercentageMixin, {
        text: isWizard ? '' : this.getThresholdsProperty('critical', 'text'),
        value: isWizard ? '' : this.getThresholdsProperty('critical', 'value'),
        valueMetric: '%'
      })
    ];
  },
  
  /**
   * Render config properties for server-type alert definition
   * @method renderScriptConfigs
   * @returns {App.AlertConfigProperty[]}
   */
  renderServerConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      })
    ]);

    return result;
  },  

  /**
   * Render common list of configs used in almost all alert types in wizard
   * @returns {App.AlertConfigProperty[]}
   */
  renderCommonWizardConfigs: function () {
    return [
      App.AlertConfigProperties.AlertName.create({
        value: ''
      }),
      App.AlertConfigProperties.ServiceAlertType.create({
        value: true
      }),
      App.AlertConfigProperties.Service.create({
        options: this.get('allServices'),
        value: this.get('allServices')[0],
        isShifted: true
      }),
      App.AlertConfigProperties.Component.create({
        options: this.get('allComponents'),
        value: 'No component',
        isShifted: true
      }),
      App.AlertConfigProperties.Scope.create({
        options: this.get('allScopes'),
        isShifted: true
      }),
      App.AlertConfigProperties.HostAlertType.create({
        value: false
      })
    ];
  },

  /**
   * Edit configs button handler
   * @method editConfigs
   */
  editConfigs: function () {
    this.get('configs').forEach(function (property) {
      property.set('previousValue', property.get('value'));
      property.set('previousText', property.get('text'));
    });
    this.get('configs').setEach('isDisabled', false);
    this.set('canEdit', true);
  },

  /**
   * Cancel edit configs button handler
   * @method cancelEditConfigs
   */
  cancelEditConfigs: function () {
    this.get('configs').forEach(function (property) {
      property.set('value', property.get('previousValue'));
      property.set('text', property.get('previousText'));
    });
    this.get('configs').setEach('isDisabled', true);
    this.set('canEdit', false);
  },

  /**
   * Save edit configs button handler
   * @method saveConfigs
   * @return {$.ajax}
   */
  saveConfigs: function () {
    this.get('configs').setEach('isDisabled', true);
    this.set('canEdit', false);

    return App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: this.get('content.id'),
        data: this.getPropertiesToUpdate(true)
      },
      success: 'saveConfigsSuccessCallback'
    });
  },

  /**
   * Success-callback for saveConfigs-request
   * @method saveConfigsSuccessCallback
   */
  saveConfigsSuccessCallback: function () {
    App.router.get('updateController').updateAlertDefinitions(Em.K);
  },

  /**
   * Create object with new values to put it on server
   * @param {boolean} onlyChanged
   * @method getPropertiesToUpdate
   * @returns {Object}
   */
  getPropertiesToUpdate: function (onlyChanged) {
    var propertiesToUpdate = {};
    var configs = onlyChanged ? this.get('configs').filterProperty('wasChanged') : this.get('configs');
    configs.forEach(function (property) {
      var apiProperties = Em.makeArray(property.get('apiProperty'));
      var apiFormattedValues = Em.makeArray(property.get('apiFormattedValue'));
      apiProperties.forEach(function (apiProperty, i) {
        if (apiProperty.contains('source.')) {
          if (!propertiesToUpdate['AlertDefinition/source']) {
            if (this.get('content.rawSourceData')) {
              propertiesToUpdate['AlertDefinition/source'] = this.get('content.rawSourceData');
            }
          }

          if (this.get('content.rawSourceData')) {
            // use rawSourceData to populate propertiesToUpdate
            var sourcePath = propertiesToUpdate['AlertDefinition/source'];
            apiProperty.replace('source.', '').split('.').forEach(function (path, index, array) {
              // check if it is last path
              if (array.length - index === 1) {
                sourcePath[path] = apiFormattedValues[i];
              } else {
                sourcePath = sourcePath[path];
              }
            });
          }
          else {
            if (!propertiesToUpdate['AlertDefinition/source']) {
              propertiesToUpdate['AlertDefinition/source'] = {};
            }
            Ember.setFullPath(propertiesToUpdate['AlertDefinition/source'], apiProperty.replace('source.', ''), apiFormattedValues[i]);
          }
        }
        else {
          if (apiProperty) {
            propertiesToUpdate['AlertDefinition/' + apiProperty] = apiFormattedValues[i];
          }
        }
      }, this);
    }, this);

    if (Em.get(propertiesToUpdate, 'AlertDefinition/source.uri.id')) {
      delete propertiesToUpdate['AlertDefinition/source'].uri.id;
    }

    return propertiesToUpdate;
  },

  /**
   * Return array of all config values
   * used to save configs to local db in wizard
   * @method getConfigsValues
   * @returns {Array}
   */
  getConfigsValues: function () {
    return this.get('configs').map(function (property) {
      return {
        name: property.get('name'),
        value: property.get('value')
      }
    });
  },

  /**
   * Define whether critical threshold >= critical threshold
   * @type {Boolean}
   */
  hasThresholdsError: function () {
    var smallValue, smallValid, largeValue, largeValid;
    if (this.get('configs').findProperty('name', 'warning_threshold')) {
      smallValue = Em.get(this.get('configs').findProperty('name', 'warning_threshold'), 'value');
      smallValid = Em.get(this.get('configs').findProperty('name', 'warning_threshold'), 'isValid');
    }
    if (this.get('configs').findProperty('name', 'critical_threshold')) {
      largeValue = Em.get(this.get('configs').findProperty('name', 'critical_threshold'), 'value');
      largeValid = Em.get(this.get('configs').findProperty('name', 'critical_threshold'), 'isValid');
    }
    return smallValid && largeValid ? Number(smallValue) > Number(largeValue) : false;
  }.property('configs.@each.value'),

  /**
   * Define whether all configs are valid
   * @type {Boolean}
   */
  hasErrors: function () {
    return this.get('configs').someProperty('isValid', false) || this.get('hasThresholdsError');
  }.property('configs.@each.isValid', 'hasThresholdsError')

});
