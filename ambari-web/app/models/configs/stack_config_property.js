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

App.StackConfigProperty = DS.Model.extend({
  /**
   * id is consist of property <code>name<code>+<code>fileName<code>
   */
  id: DS.attr('string'),
  /**
   * name of property that is taken from stack
   * @property {string}
   */
  name: DS.attr('string'),

  /**
   * display name of property that is taken from ui or
   * if UI has no info about property this can be formatted or plain name of property
   * @property {string}
   */
  displayName: DS.attr('string'),

  /**
   * filename of property that is taken from stack
   * ex: "hdfs-site.xml"
   * @property {string}
   */
  fileName: DS.attr('string'),

  /**
   * same as fileName
   * @property {string}
   */
  filename: function() {
    return this.get('fileName');
  }.property('fileName'),

  /**
   * description of config property meaning
   * @property {string}
   */
  description: DS.attr('string'),

  /**
   * defaultValue value of property is taken from stack before cluster is created
   * after cluster created is taken from cluster properties value
   * @property {string}
   */
  recommendedValue: DS.attr('string'),

  /**
   * defines if property support usage <code>isFinal<code> flag
   * @property {boolean}
   */
  supportsFinal: DS.attr('boolean', {defaultValue: true}),

  /**
   * defines the recommended value of <code>isFinal<code> value
   * @property {boolean}
   */
  recommendedIsFinal: DS.attr('boolean', {defaultValue: false}),

  /**
   * type of property
   * @property {string[]}
   */
  type: DS.attr('array', {defaultValue: []}),

  /**
   * service name
   * @property {string}
   */
  serviceName:  DS.attr('string', {defaultValue: 'MISC'}),

  /**
   * stack name
   * @property {string}
   */
  stackName:  DS.attr('string'),

  /**
   * stack version
   * @property {string}
   */
  stackVersion:  DS.attr('string'),

  /**
   * describe widget details
   * @property {object}
   */
  widget: DS.attr('object', {defaultValue: null}),

  /**
   * widget type
   * @property {string}
   */
  widgetType: DS.attr('string', {defaultValue: null}),

  /**
   * this property contains array of properties which value
   * is dependent from current property
   * @property {array}
   */
  propertyDependedBy: DS.attr('array', {defaultValue: []}),

  /**
   * this property contains list of file type and name of properties
   * which values depends on current property
   * @property {object[]}
   */
  propertyDependsOn: DS.attr('array', {defaultValue: []}),

  /**
   * info for displaying property
   * example:
   * {
   *    "type": "value-list",
   *    "entries": ["true", "false"],
   *    "entry_labels": ["Active", "Inactive"],
   *    "entries_editable": "false",
   *    "selection_cardinality": "1"
   * },
   * OR
   * {
   *    "type": "int",
   *    "minimum": "512",
   *    "maximum": "10240",
   *    "unit": "MB"
   * },
   * OR
   * {
   *    "type": "value-list",
   *    "entries": ["New_MySQL_Database", "Existing_MySQL_Database", "Existing_PostgreSQL_Database", "Existing_Oracle_Database"],
   *    "entry_labels": ["New MySQL Database", "Existing MySQL Database", "Existing PostgreSQL Database", "Existing Oracle Database"],
   *    "entry_descriptions": ["d1", "d2", "d3", "d4"],
   *    "entries_editable": "false",
   *    "selection_cardinality": "1" // 0+, 1+, etc.
   * }
   * @property {object}
   */
  valueAttributes: DS.attr('object', {defaultValue: null}),

  /**
   * sub section to which belongs this property
   * @property {App.SubSection}
   */
  subSection: DS.belongsTo('App.SubSection'),

  /**
   * sub section tab to which belongs this property
   * @property {App.SubSectionTab}
   */
  subSectionTab: DS.belongsTo('App.SubSectionTab'),

  /******************************* UI properties ****************************************/

  /**
   * defines what kind of value this property contains
   * ex: string, digits, number, directories, custom
   * @property {string}
   */
  displayType: DS.attr('string', {defaultValue: 'string'}),

  /**
   * defines category name of property
   * used for advanced tab
   * @property {string}
   */
  category: DS.attr('string'),

  /**
   * config property value same as default
   * @property {string}
   */
  value: DS.attr('string'),

  /**
   * config property isFinal value same as recommendedIsFinal
   * @property {boolean}
   */
  isFinal: DS.attr('boolean', {defaultValue: false}),

  /**
   * @type {boolean}
   */
  index: DS.attr('number', {defaultValue: null}),

  /**
   * defines if the property can be overriden in the host config group
   * @type {boolean}
   */
  isOverridable: function() {
    var result = true;
    if (this.get('valueAttributes') && !Em.none(this.get('valueAttributes.overridable'))) {
      result =  !!this.get('valueAttributes.overridable');
    }
    return result;
  }.property('valueAttributes.overridable'),

  /**
   * defines if the property is visible or hidden
   * @type {boolean}
   */
  isVisible: function() {
    var result = true;
    if (this.get('valueAttributes') && !Em.none(this.get('valueAttributes.visible'))) {
      result =  !!this.get('valueAttributes.visible');
    }
    return result;
  }.property('valueAttributes.visible'),

  /**
   * defines if the value of property can be left empty or not
   * @type {boolean}
   */
  isRequired: function() {
    var result = true;
    if (this.get('valueAttributes') && !Em.none(this.get('valueAttributes.empty_value_valid'))) {
      result =  !this.get('valueAttributes.empty_value_valid');
    }
    return result;
  }.property('valueAttributes.empty_value_valid'),

  /**
   * defines if the value of property can be reconfigured post-install
   * @type {boolean}
   */
  isReconfigurable: function() {
    var result = true;
    if (this.get('valueAttributes') && !Em.none(this.get('valueAttributes.editable_only_at_install'))) {
      result =  !this.get('valueAttributes.editable_only_at_install');
    }
    return result;
  }.property('valueAttributes.editable_only_at_install'),

  /**
   * defines if the name of the property is visible or hidden
   * @type {boolean}
   */
  showLabel: function() {
    var result = true;
    if (this.get('valueAttributes') && !Em.none(this.get('valueAttributes.show_property_name'))) {
      result =  !!this.get('valueAttributes.show_property_name');
    }
    return result;
  }.property('valueAttributes.show_property_name'),

  /**
   * defines if the property is editable or not
   * @type {boolean}
   */
  isEditable: function() {
    var result = true;
    if (this.get('valueAttributes') && !Em.none(this.get('valueAttributes.read_only'))) {
      result =  !!this.get('valueAttributes.read_only');
    }
    return result;
  }.property('valueAttributes.read_only'),

  /**
   * defines if the property is editable or not
   * @type {boolean}
   */
  unit: function() {
    var result = '';
    if (this.get('valueAttributes') && !Em.empty(this.get('valueAttributes.unit'))) {
      result =  this.get('valueAttributes.unit');
    }
    return result;
  }.property('valueAttributes.unit'),

  /**
   * Does config property has a valid value defined in the stack
   * @type {boolean}
   */
  isValueDefined: function() {
    return !Em.none(this.get('value'));
  }.property('id')
});


App.StackConfigProperty.FIXTURES = [];

App.StackConfigValAttributesMap = {
  'overridable': 'isOverridable' ,
  'visible': 'isVisible' ,
  'empty_value_valid':'isRequired' ,
  'editable_only_at_install': 'isReconfigurable' ,
  'show_property_name': 'showLabel',
  'read_only': 'isEditable',
  'ui_only_property': 'isRequiredByAgent'
};
