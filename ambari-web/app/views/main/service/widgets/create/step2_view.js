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

App.WidgetWizardStep2View = Em.View.extend({

  templateName: require('templates/main/service/widgets/create/step2'),

  EXPRESSION_PREFIX: 'Expression',

  /**
   * content of template of Template widget
   * @type {string}
   */
  templateValue: '',

  /**
   * calculate template by widget type
   */
  templateType: function () {
    switch (this.get('controller.content.widgetType')) {
      case 'GAUGE':
      case 'NUMBER':
        return {
          isNumber: true
        };
      case 'TEMPLATE':
        return {
          isTemplate: true
        };
      case 'GRAPH':
        return {
          isGraph: true
        }
    }
  }.property('controller.content.widgetType'),

  /**
   * @type {Array}
   */
  expressions: [],

  /**
   * used only for GRAPH widget
   * @type {Array}
   */
  dataSets: [],

  /**
   * Add data set
   * @param {object|null} event
   * @param {boolean} isDefault
   */
  addDataSet: function(event, isDefault) {
    var id = (isDefault) ? 1 :(Math.max.apply(this, this.get('dataSets').mapProperty('id')) + 1);

    this.get('dataSets').pushObject(Em.Object.create({
      id: id,
      label: '',
      isRemovable: !isDefault,
      expression: Em.Object.create({
        data: []
      })
    }));
  },

  /**
   * Remove data set
   * @param {object} event
   */
  removeDataSet: function(event) {
    this.get('dataSets').removeObject(event.context);
  },

  /**
   * Add expression
   * @param {object|null} event
   * @param {boolean} isDefault
   */
  addExpression: function(event, isDefault) {
    var id = (isDefault) ? 1 :(Math.max.apply(this, this.get('expressions').mapProperty('id')) + 1);

    this.get('expressions').pushObject(Em.Object.create({
      id: id,
      isRemovable: !isDefault,
      data: [],
      alias: '{{' + this.get('EXPRESSION_PREFIX') + id + '}}'
    }));
  },

  /**
   * Remove expression
   * @param {object} event
   */
  removeExpression: function(event) {
    this.get('expressions').removeObject(event.context);
  },

  updatePreview: function() {
    this.get('controller').updateExpressions(this);
  }.observes('templateValue', 'dataSets.@each.label'),

  didInsertElement: function () {
    var controller = this.get('controller');
    controller.renderProperties();
    this.get('expressions').clear();
    this.get('dataSets').clear();
    this.addExpression(null, true);
    this.addDataSet(null, true);
    controller.updateExpressions(this);
  }
});


App.WidgetPropertyTextFieldView = Em.TextField.extend({
  valueBinding: 'property.value',
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});

App.WidgetPropertyThresholdView = Em.View.extend({
  templateName: require('templates/main/service/widgets/create/widget_property_threshold'),
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});

App.WidgetPropertySelectView = Em.Select.extend({
  selectionBinding: 'property.value',
  contentBinding: 'property.options',
  classNameBindings: ['property.classNames', 'parentView.basicClass']
});



