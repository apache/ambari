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

import Ember from 'ember';

export default Ember.Component.extend({
  tagName: '',
  dataTypes: null,
  column: null,
  precisionChanged: Ember.observer('column.precision', function () {
    var col = this.get('column');
    if( typeof col.precision !== 'number') {
        Ember.set(col, 'precision', Number(col.precision));
      }
  }),

  scaleChanged: Ember.observer('column.scale', function () {
    var col = this.get('column');
    if( typeof col.scale !== 'number'){
      Ember.set(col,'scale',Number(col.scale));
    }
  }),

  typeChanged: Ember.observer('column.type', function () {
    var col = this.get('column');

    var type = col.type;
    if( type != "DECIMAL" ){
      Ember.set(col,'scale');
    }

    if(type != "VARCHAR" && type != "CHAR" && type != "DECIMAL" ){
      Ember.set(col,'precision');
    }
  }),

  noPrecision: Ember.computed('column.type', function () {
    var type = this.get('column').type;
    return (type == "VARCHAR" || type == "CHAR" || type == "DECIMAL" ) ? false : true;
  }),

  noScale: Ember.computed('column.type', function () {
    return this.get('column').type == "DECIMAL" ? false : true;
  })

});
