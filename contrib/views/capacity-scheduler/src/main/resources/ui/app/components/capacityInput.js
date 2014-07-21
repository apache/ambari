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


App.FocusInputComponent = Ember.TextField.extend({
  becomeFocused: function() {
    this.$().focus().val(this.value);
  }.on('didInsertElement'),
  cancel:function () {
    this.get('targetObject').send(this.get('revert'),'cancel');
  }
});

App.IntInputComponent = Ember.TextField.extend({
  classNames:['form-control'],
  maxVal:null,
  intVal:function () {
    var val = parseFloat(this.get('value'))||0;
    var maxVal = this.get('maxVal');
    val = (maxVal && maxVal < val)?maxVal:val;
    this.set('value', val);
  }.on('change'),
  checkNumber:function () {
    var val = this.get('value'),
        num = Number(val),
        str = String(val);
    if (typeof val !== "number" && !isNaN(num) && str == num.toString()) {
      this.set('value', Number(val));
    };
  }.observes('value')
});

App.CapacityInputComponent = App.IntInputComponent.extend({
  
  totalCapacity:null,
  queue:null,

  keyDown: function(evt) {
    var newChar, val = this.get('value')||0;
    val = val.toString();
    
    if ((evt.keyCode > 64 && evt.keyCode < 91) || 
      (evt.keyCode > 185 && evt.keyCode < 193) || 
      (evt.keyCode > 218 && evt.keyCode < 223)) {
      return false;
    };

    if (evt.keyCode > 95 && evt.keyCode < 106) {
      newChar = (evt.keyCode - 96).toString();
    } else {
      newChar = String.fromCharCode(evt.keyCode);
    }

    if (newChar.match(/[0-9]/)) {
      val = val.substring(0, evt.target.selectionStart) + newChar + val.substring(evt.target.selectionEnd);
    };

    return parseFloat(val)<=100;
  },
});

App.MaxCapacityInputComponent = App.CapacityInputComponent.extend({
  isInvalid:false,
  invalid:function (c,o) {
    var queue = this.get('queue'),
        max_capacity = Number(queue.get('maximum_capacity')),
        capacity = Number(queue.get('capacity'));
    if (o == 'queue.capacity' && max_capacity < capacity) {
      return queue.set('maximum_capacity',capacity);
    };
    if (max_capacity < capacity && queue.get('isDirty')) {
      queue.get('errors').add('maximum_capacity', 'Maximum capacity must be greater then capacity');
    } else {
      queue.get('errors').remove('maximum_capacity');
    }
  }.observes('queue.maximum_capacity','queue.capacity')
});
