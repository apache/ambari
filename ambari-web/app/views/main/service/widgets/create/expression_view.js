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

  /**
   * @type {Array}
   */
  classNames: ['metric-container'],

  /**
   * @type {Array}
   */
  classNameBindings: ['isInvalid'],

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
  isInvalid: false,

  /**
   * add operator to expression data
   * @param event
   */
  addOperator: function (event) {
    var data = this.get('expression.data');
    var lastId = (data.length > 0) ? Math.max.apply(this, data.mapProperty('id')) : 0;

    data.pushObject(Em.Object.create({
      id: ++lastId,
      name: event.context,
      isOperator: true
    }));
  },

  /**
   * redraw expression
   * NOTE: needed in order to avoid collision between scrollable lib and metric action event
   */
  redrawField: function () {
    this.set('expression.data', misc.sortByOrder($(this.get('element')).find('.metric-instance').map(function () {
      return this.id;
    }), this.get('expression.data')));
  },

  /**
   * enable metric edit area
   */
  startEdit: function () {
    var self = this;
    this.set('dataBefore', this.get('expression.data').slice(0));
    this.set('expression.editMode', true);
    this.propertyDidChange('expression');
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

  /**
   * discard changes and disable metric edit area
   */
  cancelEdit: function () {
    this.set('expression.data', this.get('dataBefore'));
    this.set('expression.editMode', false);
    this.propertyDidChange('expression');
  },

  /**
   * save changes and disable metric edit area
   */
  saveMetrics: function () {
    this.set('expression.editMode', false);
    this.propertyDidChange('expression');
  },

  /**
   * remove metric or operator from expression
   * @param {object} event
   */
  removeElement: function (event) {
    this.get('expression.data').removeObject(event.context);
  },

  validate: function () {
    //number 1 used as substitute to test expression to be mathematically correct
    var testNumber = 1;
    var isInvalid = true;
    var expression = this.get('expression.data').map(function (element) {
      if (element.isMetric) {
        return testNumber;
      } else {
        return element.name;
      }
    }, this).join(" ");

    if (expression.length > 0) {
      if (/^((\(\s)*[\d]+)[\(\)\+\-\*\/\d\s]*[\d\)]*$/.test(expression)) {
        try {
          isInvalid = !isFinite(window.eval(expression));
        } catch (e) {
          isInvalid = true;
        }
      }
    } else {
      isInvalid = false;
    }

    this.set('isInvalid', isInvalid);
    if (!isInvalid) this.get('controller').updateExpressions();
  }.observes('expression.data.length'),

  /**
   * show popup that provide ability to add metric
   */
  addMetric: function () {
    return App.ModalPopup.show({
      header: Em.I18n.t('dashboard.widgets.wizard.step2.addMetric'),
      classNames: ['modal-690px-width'],
      disablePrimary: function () {
        return Em.isNone(this.get('selectedMetric'));
      }.property('selectedMetric'),
      expression: this.get('expression'),

      /**
       * @type {object|null}
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
          var self = this;

          //prevent dropdown closing on checkbox click
          $('html').on('click.dropdown', '.dropdown-menu li', function (e) {
            $(this).hasClass('keep-open') && e.stopPropagation();
          });

          $(".chosen-select").chosen({
            placeholder_text: Em.I18n.t('widget.create.wizard.step2.noMetricFound'),
            no_results_text: Em.I18n.t('widget.create.wizard.step2.noMetricFound')
          }).change(function (event, obj) {
            self.set('parentView.selectedMetric', Em.Object.create({
              name: obj.selected,
              componentName: self.get('selectedComponent.componentName'),
              serviceName: self.get('selectedComponent.serviceName'),
              metricPath: self.get('controller.filteredMetrics').findProperty('name', obj.selected).widget_id,
              isMetric: true
            }));
          });
        },

        /**
         * @type {Ember.Object}
         * @default null
         */
        selectedComponent: null,

        showMore: Em.K,

        selectComponents: function (event) {
          var component = this.get('componentMap').findProperty('serviceName', event.context.get('serviceName'))
            .get('components').findProperty('id', event.context.get('id'));
          $('#add-metric-popup .component-select').removeClass('open');

          this.set('selectedComponent', component);
          this.set('parentView.selectedMetric', null);
          Em.run.next(function () {
            $('.chosen-select').trigger('chosen:updated');
          });
        },

        /**
         * map of components
         * has following hierarchy: service -> component -> metrics
         */
        componentMap: function () {
          var servicesMap = {};
          var result = [];
          var components = [];
          var masterNames = App.StackServiceComponent.find().filterProperty('isMaster').mapProperty('componentName');

          this.get('controller.filteredMetrics').forEach(function (metric) {
            var service = servicesMap[metric.service_name];
            var componentId = masterNames.contains(metric.component_name) ? metric.component_name + '_' + metric.level : metric.component_name;
            if (service) {
              service.count++;
              if (service.components[componentId]) {
                service.components[componentId].count++;
                service.components[componentId].metrics.push(metric.name);
              } else {
                service.components[componentId] = {
                  component_name: metric.component_name,
                  level: metric.level,
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
            components = [];
            for (var componentId in servicesMap[serviceName].components) {
              components.push(Em.Object.create({
                componentName: servicesMap[serviceName].components[componentId].component_name,
                level: servicesMap[serviceName].components[componentId].level,
                displayName: function() {
                  var stackComponent = App.StackServiceComponent.find(this.get('componentName'));
                  if (stackComponent.get('isMaster')) {
                    if (this.get('level') === 'COMPONENT') {
                      return Em.I18n.t('widget.create.wizard.step2.allComponents').format(stackComponent.get('displayName'));
                    } else {
                      return Em.I18n.t('widget.create.wizard.step2.activeComponents').format(stackComponent.get('displayName'));
                    }
                  }
                  return stackComponent.get('displayName');
                }.property('componentName', 'level'),
                count: servicesMap[serviceName].components[componentId].count,
                metrics: servicesMap[serviceName].components[componentId].metrics.uniq().sort(),
                selected: false,
                id: componentId,
                serviceName: serviceName
              }));
            }
            result.push(Em.Object.create({
              serviceName: serviceName,
              //in order to support accordion lists
              href: '#' + serviceName,
              displayName: App.StackService.find(serviceName).get('displayName'),
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
        var id = (data.length > 0) ? Math.max.apply(this, data.mapProperty('id')) + 1 : 1;
        var selectedMetric = this.get('selectedMetric');
        selectedMetric.set('id', id);
        data.pushObject(selectedMetric);
        this.hide();
      }
    })
  }
});
