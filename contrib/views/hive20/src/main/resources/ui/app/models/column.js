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
import datatypes from '../configs/datatypes';
import Helper from '../configs/helpers';
let Column = Ember.Object.extend(Ember.Copyable,{
  name: '',
  type: datatypes[0],
  precision: null,
  scale: null,
  isPartitioned: false,
  isClustered: false,
  comment: '',


  hasError: Ember.computed('errors.@each', function() { return this.get('errors.length') !== 0; }),
  errors: [],

  nameError: Ember.computed('errors.@each', function() {
    return this.get('errors').findBy('type', 'name');
  }),

  typeError: Ember.computed('errors.@each', function() {
    return this.get('errors').findBy('type', 'type');
  }),


  precisionError: Ember.computed('errors.@each', function() {
    return this.get('errors').findBy('type', 'precision');
  }),

  scaleError: Ember.computed('errors.@each', function() {
    return this.get('errors').findBy('type', 'scale');
  }),

  partitionObserver: Ember.observer('isPartitioned', function() {
    if(this.get('isPartitioned')) {
      this.set('isClustered', false);
    }
  }),

  clusteredObserver: Ember.observer('isClustered', function() {
    if(this.get('isClustered')) {
      this.set('isPartitioned', false);
    }
  }),


  // Control the UI
  editing: false,

  clearError() {
    this.set('errors', []);
  },


  validate() {
    this.clearError();
    if (Ember.isEmpty(this.get('name'))) {
      this.get('errors').pushObject({type: 'name', error: "name cannot be empty"});
    }

    if(Ember.isEmpty(this.get('type'))) {
      this.get('errors').pushObject({type: 'type', error: "Type cannot be empty"});
    }

    if(this.get('type.hasPrecision')) {
      if(Ember.isEmpty(this.get('precision'))) {
        this.get('errors').pushObject({type: 'precision', error: "Precision cannot be empty"});
      } else if(!Helper.isInteger(this.get('precision'))) {
        this.get('errors').pushObject({type: 'precision', error: "Precision can only be a number"});
      } else if(this.get('precision') <= 0) {
        this.get('errors').pushObject({type: 'precision', error: "Precision can only be greater than zero"});
      } else if(this.get('type.hasScale') && this.get('scale') && (this.get('precision') < this.get('scale'))) {
        this.get('errors').pushObject({type: 'precision', error: "Precision can only be greater than scale"});
      }
    }else{
      delete this.precision;
    }


    if(this.get('type.hasScale')) {
      if(Ember.isEmpty(this.get('scale'))) {
        this.get('errors').pushObject({type: 'scale', error: "Scale cannot be empty"});
      } else if(!Helper.isInteger(this.get('scale'))) {
        this.get('errors').pushObject({type: 'scale', error: "Scale can only be a number"});
      } else if(this.get('scale') <= 0) {
        this.get('errors').pushObject({type: 'scale', error: "Scale can only be greater than zero"});
      }
    }else{
      delete this.scale;
    }

    return this.get('errors.length') === 0;
  },

  copy: function(){
    let col = Column.create({
      name: this.get("name"),
      type: datatypes.findBy("label", this.get("type.label")),
      precision: this.get("precision"),
      scale: this.get("scale"),
      isPartitioned: this.get("isPartitioned"),
      isClustered: this.get("isClustered"),
      comment: this.get("comment"),

      errors: this.get("errors").copy(),
      editing: this.get("editing"),
    });
    return col;
  }

});

export default Column;
