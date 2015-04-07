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

var misc = require('utils/misc');

App.WidgetWizardExpressionView = Em.View.extend({
  templateName: require('templates/main/service/widgets/create/expression'),

  classNames: ['metric-container'],

  /**
   * list of operators that can be used in expression
   * @type {Array}
   * @constant
   */
  OPERATORS: ["+", "-", "*", "/", "(", ")"],

  /**
   * contains expression data before editing in order to restore previous state
   */
  dataBefore: [],

  /**
   * @type {Ember.Object}
   */
  expression: null,

  /**
   * @type {boolean}
   */
  editMode: false,

  /**
   * @type {boolean}
   */
  isValid: true,

  addOperator: function (event) {
    var data = this.get('expression.data');
    var lastId = (data.length > 0) ? Math.max.apply(this, data.mapProperty('id')) : 0;
    data.pushObject(Em.Object.create({
      id: ++lastId,
      name: event.context,
      isOperator: true
    }));
  },

  redrawField: function(){
    this.set('expression.data', misc.sortByOrder($(this.get('element')).find('.metric-instance').map(function () {
      return this.id;
    }), this.get('expression.data')));
  },

  startEdit: function () {
    var self = this;
    this.set('dataBefore', this.get('expression.data').slice(0));
    this.set('editMode', true);
    Em.run.next(function () {
      $(self.get('element')).find('.metric-field').sortable({
        items: "> div",
        tolerance: "pointer",
        scroll: false,
        update: function () {
          self.redrawField();
        }
      }).disableSelection();
    });
  },

  cancelEdit: function () {
    this.set('expression.data', this.get('dataBefore'));
    this.set('editMode', false);
  },

  saveMetrics: function () {
    this.set('editMode', false);
  },

  removeElement: function (event) {
    this.get('expression.data').removeObject(event.context);
  },

  validate: function() {
    //todo add validation
  }.observes('expression.data.length'),

  addMetric: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.wizard.step2.addMetric'),
      classNames: ['modal-690px-width'],
      disablePrimary: true,
      expression: this.get('expression'),

      /**
       * @type {string}
       * @default null
       */
      selectedMetric: null,

      /**
       * @type {Ember.View}
       * @class
       */
      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/widgets/create/step2_add_metric'),
        controller: this.get('controller'),
        elementId: 'add-metric-popup',
        didInsertElement: function () {
          //prevent dropdown closing on checkbox click
          $('html').on('click.dropdown', '.dropdown-menu li', function (e) {
            $(this).hasClass('keep-open') && e.stopPropagation();
          });
        },

        /**
         * @type {Array}
         */
        componentMetrics: [],

        /**
         * @type {boolean}
         */
        isComponentSelected: false,
        selectComponents: function () {
          var componentMetrics = [];

          this.get('componentMap').forEach(function (service) {
            service.get('components').filterProperty('selected').forEach(function (component) {
              componentMetrics.pushObjects(component.get('metrics'));
            }, this);
          }, this);
          this.set('componentMetrics', componentMetrics);
          this.set('isComponentSelected', true);
          this.set('parentView.disablePrimary', false);
        },
        cancelMetric: function () {
          this.get('componentMetrics').clear();
          this.set('parentView.disablePrimary', true);
          this.set('isComponentSelected', false);
        },
        componentMap: function () {
          var servicesMap = {};
          var result = [];
          var components = [];

          this.get('controller.filteredMetrics').forEach(function (metric) {
            var service = servicesMap[metric.service_name];
            if (service) {
              service.count++;
              if (service.components[metric.component_name]) {
                service.components[metric.component_name].count++;
                service.components[metric.component_name].metrics.push(metric.name);
              } else {
                service.components[metric.component_name] = {
                  count: 1,
                  metrics: [metric.name]
                };
              }
            } else {
              servicesMap[metric.service_name] = {
                count: 1,
                components: {}
              };
            }
          }, this);

          for (var serviceName in servicesMap) {
            for (var componentName in servicesMap[serviceName].components) {
              components.push(Em.Object.create({
                componentName: componentName,
                count: servicesMap[serviceName].components[componentName].count,
                metrics: servicesMap[serviceName].components[componentName].metrics,
                selected: false
              }));
            }
            result.push(Em.Object.create({
              serviceName: serviceName,
              count: servicesMap[serviceName].count,
              components: components
            }));
          }

          return result;
        }.property('controller.filteredMetrics')
      }),
      primary: Em.I18n.t('common.save'),
      onPrimary: function () {
        var data = this.get('expression.data');
        var lastId = (data.length > 0) ? Math.max.apply(this, data.mapProperty('id')) : 0;
        data.pushObject(Em.Object.create({
          id: ++lastId,
          name: this.get('selectedMetric'),
          isMetric: true
        }));
        this.hide();
      }
    })
  }
});
