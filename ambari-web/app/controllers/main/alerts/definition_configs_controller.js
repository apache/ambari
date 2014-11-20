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
   * Array of displayNames of all services
   * is used for "Service" config options
   * @type {Array}
   */
  allServices: function () {
    return App.Service.find().mapProperty('displayName');
  }.property(),

  /**
   * Array of all aggregate-alerts names
   * @type {Array}
   */
  aggregateAlertNames: function () {
    return App.AggregateAlertDefinition.find().mapProperty('name');
  }.property(),

  /**
   * Change options of "Component", after changing value of "Service" config
   */
  onServiceSelect: function () {
    var serviceProperty = this.get('configs').findProperty('label', 'Service');
    if (serviceProperty) {
      var componentsProperty = this.get('configs').findProperty('label', 'Component');
      componentsProperty.set('options', ['No component'].concat(App.HostComponent.find().filterProperty('service.displayName', serviceProperty.get('value')).mapProperty('displayName').uniq()));
    }
  }.observes('configs.@each.value'),

  /**
   * Render array of configs for appropriate alert definition type
   */
  renderConfigs: function () {
    var alertDefinition = this.get('content');
    var configs = [];
    switch (alertDefinition.get('type')) {
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
        console.error('Incorrect Alert Definition Type: ', alertDefinition.get('type'));
    }

    configs.setEach('isDisabled', !this.get('canEdit'));

    this.set('configs', configs);
  },

  /**
   * Render config properties for port-type alert definition
   * @returns {Array}
   */
  renderPortConfigs: function () {
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.AlertName.create({
        value: alertDefinition.get('name')
      }),
      App.AlertConfigProperties.Service.create({
        options: this.get('allServices'),
        value: alertDefinition.get('service.displayName')
      }),
      App.AlertConfigProperties.Component.create({
        options: this.get('allComponents'),
        value: alertDefinition.get('componentName') ? App.format.role(alertDefinition.get('componentName')) : 'No component'
      }),
      App.AlertConfigProperties.Scope.create({
        value: alertDefinition.get('scope').toLowerCase().capitalize()
      }),
      App.AlertConfigProperties.Description.create({
        value: alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: alertDefinition.get('thresholds'),
        from: alertDefinition.get('thresholds').split('-')[0],
        to: alertDefinition.get('thresholds').split('-')[1]
      }),
      App.AlertConfigProperties.URI.create({
        value: alertDefinition.get('uri')
      }),
      App.AlertConfigProperties.DefaultPort.create({
        value: alertDefinition.get('defaultPort')
      })
    ];
  },

  /**
   * Render config properties for metric-type alert definition
   * @returns {Array}
   */
  renderMetricConfigs: function () {
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.AlertName.create({
        value: alertDefinition.get('name')
      }),
      App.AlertConfigProperties.Service.create({
        options: this.get('allServices'),
        value: alertDefinition.get('service.displayName')
      }),
      App.AlertConfigProperties.Component.create({
        options: this.get('allComponents'),
        value: alertDefinition.get('componentName') ? App.format.role(alertDefinition.get('componentName')) : 'No component'
      }),
      App.AlertConfigProperties.Scope.create({
        value: alertDefinition.get('scope').toLowerCase().capitalize()
      }),
      App.AlertConfigProperties.Description.create({
        value: alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: alertDefinition.get('thresholds'),
        from: alertDefinition.get('thresholds').split('-')[0],
        to: alertDefinition.get('thresholds').split('-')[1]
      }),
      App.AlertConfigProperties.URIExtended.create({
        value: JSON.stringify({
          http: alertDefinition.get('uri.http'),
          https: alertDefinition.get('uri.https'),
          https_property: alertDefinition.get('uri.httpsProperty'),
          https_property_value: alertDefinition.get('uri.httpsPropertyValue')
        })
      }),
      App.AlertConfigProperties.Metrics.create({
        value: alertDefinition.get('jmx.propertyList') ? alertDefinition.get('jmx.propertyList').join(',\n') : alertDefinition.get('ganglia.propertyList').join(',\n'),
        isJMXMetric: !!alertDefinition.get('jmx.propertyList')
      }),
      App.AlertConfigProperties.FormatString.create({
        value: alertDefinition.get('jmx.value') ? alertDefinition.get('jmx.value') : alertDefinition.get('ganglia.value'),
        isJMXMetric: !!alertDefinition.get('jmx.value')
      })
    ];
  },

  /**
   * Render config properties for web-type alert definition
   * @returns {Array}
   */
  renderWebConfigs: function () {
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.AlertName.create({
        value: alertDefinition.get('name')
      }),
      App.AlertConfigProperties.Service.create({
        options: this.get('allServices'),
        value: alertDefinition.get('service.displayName')
      }),
      App.AlertConfigProperties.Component.create({
        options: this.get('allComponents'),
        value: alertDefinition.get('componentName') ? App.format.role(alertDefinition.get('componentName')) : 'No component'
      }),
      App.AlertConfigProperties.Scope.create({
        value: alertDefinition.get('scope').toLowerCase().capitalize()
      }),
      App.AlertConfigProperties.Description.create({
        value: alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: alertDefinition.get('thresholds'),
        from: alertDefinition.get('thresholds').split('-')[0],
        to: alertDefinition.get('thresholds').split('-')[1]
      }),
      App.AlertConfigProperties.URIExtended.create({
        value: JSON.stringify({
          http: alertDefinition.get('uri.http'),
          https: alertDefinition.get('uri.https'),
          https_property: alertDefinition.get('uri.httpsProperty'),
          https_property_value: alertDefinition.get('uri.httpsPropertyValue')
        })
      })
    ];
  },

  /**
   * Render config properties for script-type alert definition
   * @returns {Array}
   */
  renderScriptConfigs: function () {
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.AlertName.create({
        value: alertDefinition.get('name')
      }),
      App.AlertConfigProperties.Service.create({
        options: this.get('allServices'),
        value: alertDefinition.get('service.displayName')
      }),
      App.AlertConfigProperties.Component.create({
        options: this.get('allComponents'),
        value: alertDefinition.get('componentName') ? App.format.role(alertDefinition.get('componentName')) : 'No component'
      }),
      App.AlertConfigProperties.Scope.create({
        value: alertDefinition.get('scope').toLowerCase().capitalize()
      }),
      App.AlertConfigProperties.Description.create({
        value: alertDefinition.get('description')
      }),
      App.AlertConfigProperties.Interval.create({
        value: alertDefinition.get('interval')
      }),
      App.AlertConfigProperties.Thresholds.create({
        value: alertDefinition.get('thresholds'),
        from: alertDefinition.get('thresholds').split('-')[0],
        to: alertDefinition.get('thresholds').split('-')[1]
      }),
      App.AlertConfigProperties.Path.create({
        value: alertDefinition.get('location')
      })
    ];
  },

  /**
   * Render config properties for aggregate-type alert definition
   * @returns {Array}
   */
  renderAggregateConfigs: function () {
    var alertDefinition = this.get('content');
    return [
      App.AlertConfigProperties.AlertNameSelected.create({
        value: alertDefinition.get('name'),
        options: this.get('aggregateAlertNames')
      }),
      App.AlertConfigProperties.Description.create({
        value: alertDefinition.get('description')
      })
    ];
  },

  /**
   * Edit configs button handler
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
   */
  saveConfigs: function () {
    this.get('configs').setEach('isDisabled', true);
    this.set('canEdit', false);

    var propertiesToUpdate = this.getPropertiesToUpdate();

    App.ajax.send({
      name: 'alerts.update_alert_definition',
      sender: this,
      data: {
        id: this.get('content.id'),
        data: propertiesToUpdate
      }
    });
  },

  /**
   * Create object with new values to put it on server
   * @returns {Object}
   */
  getPropertiesToUpdate: function () {
    var propertiesToUpdate = {};
    this.get('configs').filterProperty('wasChanged').forEach(function (property) {
      if (property.get('apiProperty').contains('source.')) {
        if (!propertiesToUpdate['AlertDefinition/source']) {
          propertiesToUpdate['AlertDefinition/source'] = this.get('content.rawSourceData');
        }

        var sourcePath = propertiesToUpdate['AlertDefinition/source'];
        property.get('apiProperty').replace('source.', '').split('.').forEach(function (path, index, array) {
          // check if it is last path
          if (array.length - index === 1) {
            sourcePath[path] = property.get('apiFormattedValue');
          } else {
            sourcePath = sourcePath[path];
          }
        });

      } else {
        propertiesToUpdate['AlertDefinition/' + property.get('apiProperty')] = property.get('apiFormattedValue');
      }
    }, this);

    return propertiesToUpdate;
  }

});
