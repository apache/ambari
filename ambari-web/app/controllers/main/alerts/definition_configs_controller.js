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
   * Compute thresholds from value using <code>content.reporting</code>
   * @type {Number|Null}
   */
  thresholdsFrom: function () {
    var warning = this.get('content.reporting').findProperty('type', 'warning');
    if (warning && warning.get('value')) {
      return warning.get('value');
    } else {
      return null;
    }
  }.property('content.reporting.@each.value'),

  /**
   * Compute thresholds to value using <code>content.reporting</code>
   * @type {Number|Null}
   */
  thresholdsTo: function () {
    var critical = this.get('content.reporting').findProperty('type', 'critical');
    if (critical && critical.get('value')) {
      return critical.get('value');
    } else {
      return null;
    }
  }.property('content.reporting.@each.value'),

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
      this.get('configs').findProperty('name', 'service').set('isDisabled', false).set('options', this.get('allServices')).set('value', this.get('allServices')[0]);
      this.get('configs').findProperty('name', 'component').set('isDisabled', false).set('value', 'No component');
      this.get('configs').findProperty('name', 'scope').set('isDisabled', false).set('options', this.get('allScopes')).set('value', this.get('allScopes')[0]);
    } else {
      this.get('configs').findProperty('name', 'service').set('isDisabled', true).set('options', ['Ambari']).set('value', 'Ambari');
      this.get('configs').findProperty('name', 'component').set('isDisabled', true).set('options', ['Ambari Agent']).set('value', 'Ambari Agent');
      this.get('configs').findProperty('name', 'scope').set('isDisabled', true).set('options', ['Host']).set('value', 'Host');
    }
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
      default:
        console.error('Incorrect Alert Definition Type: ', alertDefinitionType);
    }

    configs.setEach('isDisabled', !this.get('canEdit'));

    this.set('configs', configs);
  },

  /**
   * Render config properties for port-type alert definition
   * @method renderPortConfigs
   * @returns {Array}
   */
  renderPortConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: isWizard ? '' : this.get('thresholdsFrom') + '-' + this.get('thresholdsTo'),
        from: isWizard ? '' : this.get('thresholdsFrom'),
        to: isWizard ? '' : this.get('thresholdsTo')
      }),
      App.AlertConfigProperties.URI.create({
        value: isWizard ? '' : alertDefinition.get('uri')
      }),
      App.AlertConfigProperties.DefaultPort.create({
        value: isWizard ? '' : alertDefinition.get('defaultPort')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for metric-type alert definition
   * @method renderMetricConfigs
   * @returns {Array}
   */
  renderMetricConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: isWizard ? '' : this.get('thresholdsFrom') + '-' + this.get('thresholdsTo'),
        from: isWizard ? '' : this.get('thresholdsFrom'),
        to: isWizard ? '' : this.get('thresholdsTo')
      }),
      App.AlertConfigProperties.URIExtended.create({
        value: isWizard ? '' : JSON.stringify({
          http: alertDefinition.get('uri.http'),
          https: alertDefinition.get('uri.https'),
          https_property: alertDefinition.get('uri.httpsProperty'),
          https_property_value: alertDefinition.get('uri.httpsPropertyValue')
        })
      }),
      App.AlertConfigProperties.Metrics.create({
        value: isWizard ? '' : alertDefinition.get('jmx.propertyList') ? alertDefinition.get('jmx.propertyList').join(',\n') : alertDefinition.get('ganglia.propertyList').join(',\n'),
        isJMXMetric: isWizard ? false : !!alertDefinition.get('jmx.propertyList')
      }),
      App.AlertConfigProperties.FormatString.create({
        value: isWizard ? '' : alertDefinition.get('jmx.value') ? alertDefinition.get('jmx.value') : alertDefinition.get('ganglia.value'),
        isJMXMetric: isWizard ? false : !!alertDefinition.get('jmx.value')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for web-type alert definition
   * @method renderWebConfigs
   * @returns {Array}
   */
  renderWebConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: isWizard ? '' : this.get('thresholdsFrom') + '-' + this.get('thresholdsTo'),
        from: isWizard ? '' : this.get('thresholdsFrom'),
        to: isWizard ? '' : this.get('thresholdsTo')
      }),
      App.AlertConfigProperties.URIExtended.create({
        value: isWizard ? '' : JSON.stringify({
          http: alertDefinition.get('uri.http'),
          https: alertDefinition.get('uri.https'),
          https_property: alertDefinition.get('uri.httpsProperty'),
          https_property_value: alertDefinition.get('uri.httpsPropertyValue')
        })
      })
    ]);

    return result;
  },

  /**
   * Render config properties for script-type alert definition
   * @method renderScriptConfigs
   * @returns {Array}
   */
  renderScriptConfigs: function () {
    var result = [];
    var alertDefinition = this.get('content');
    var isWizard = this.get('isWizard');

    if (this.get('isWizard')) {
      result = result.concat(this.renderCommonWizardConfigs());
    }

    result = result.concat([
      App.AlertConfigProperties.Interval.create({
        value: isWizard ? '' : alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: isWizard ? '' : this.get('thresholdsFrom') + '-' + this.get('thresholdsTo'),
        from: isWizard ? '' : this.get('thresholdsFrom'),
        to: isWizard ? '' : this.get('thresholdsTo')
      }),
      App.AlertConfigProperties.Path.create({
        value: isWizard ? '' : alertDefinition.get('location')
      })
    ]);

    return result;
  },

  /**
   * Render config properties for aggregate-type alert definition
   * @method renderAggregateConfigs
   * @returns {Array}
   */
  renderAggregateConfigs: function () {
    var isWizard = this.get('isWizard');
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.AlertNameSelected.create({
        value: isWizard ? this.get('aggregateAlertNames')[0] : alertDefinition.get('name'),
        options: this.get('aggregateAlertNames')
      }),
      App.AlertConfigProperties.Description.create({
        value: isWizard ? '' : alertDefinition.get('description')
      })
    ];
  },

  /**
   * Render common list of configs used in almost all alert types in wizard
   * @returns {Array}
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
      }),
      App.AlertConfigProperties.Description.create({
        value: ''
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
        data: this.getPropertiesToUpdate()
      }
    });
  },

  /**
   * Create object with new values to put it on server
   * @method getPropertiesToUpdate
   * @returns {Object}
   */
  getPropertiesToUpdate: function () {
    var propertiesToUpdate = {};
    this.get('configs').filterProperty('wasChanged').forEach(function (property) {
      var apiProperties = property.get('apiProperty');
      var apiFormattedValues = property.get('apiFormattedValue');
      if (!Em.isArray(property.get('apiProperty'))) {
        apiProperties = [property.get('apiProperty')];
        apiFormattedValues = [property.get('apiFormattedValue')];
      }
      apiProperties.forEach(function (apiProperty, i) {
        if (apiProperty.contains('source.')) {
          if (!propertiesToUpdate['AlertDefinition/source']) {
            propertiesToUpdate['AlertDefinition/source'] = this.get('content.rawSourceData');
          }

          var sourcePath = propertiesToUpdate['AlertDefinition/source'];
          apiProperty.replace('source.', '').split('.').forEach(function (path, index, array) {
            // check if it is last path
            if (array.length - index === 1) {
              sourcePath[path] = apiFormattedValues[i];
            } else {
              sourcePath = sourcePath[path];
            }
          });

        } else {
          propertiesToUpdate['AlertDefinition/' + apiProperty] = apiFormattedValues[i];
        }
      }, this);
    }, this);

    return propertiesToUpdate;
  },

  /**
   * Return array of all config values
   * used to save configs to local db in wizard
   * @returns {Array}
   */
  getConfigsValues: function () {
    return this.get('configs').map(function (property) {
      return {
        name: property.get('name'),
        value: property.get('value')
      }
    });
  }

});
