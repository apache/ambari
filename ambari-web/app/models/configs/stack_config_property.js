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
   * description of config property meaning
   * @property {string}
   */
  description: DS.attr('string'),

  /**
   * defaultValue value of property is taken from stack before cluster is created
   * after cluster created is taken from cluster properties value
   * @property {string}
   */
  defaultValue: DS.attr('string'),

  /**
   * defines if property support usage <code>isFinal<code> flag
   * @property {boolean}
   */
  supportsFinal: DS.attr('boolean', {defaultValue: true}),

  /**
   * defines the defaultValue value of <code>isFinal<code> value
   * @property {boolean}
   */
  defaultIsFinal: DS.attr('boolean', {defaultValue: false}),

  /**
   * type of property
   * @property {string[]}
   */
  type: DS.attr('array', {defaultValue: []}),

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
  categoryName: DS.attr('string'),

  /**
   * service name
   * @property {string}
   */
  serviceName:  DS.attr('string'),

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
   * this property contains array of properties which value
   * is dependent from current property
   * @property {array}
   */
  propertyDependedBy: DS.attr('array', {defaultValue: []}),

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
  valueAttributes: DS.attr('object', {defaultValue: null})

});


App.StackConfigProperty.FIXTURES = [];

