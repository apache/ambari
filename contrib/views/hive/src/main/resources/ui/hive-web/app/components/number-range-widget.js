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
  didInsertElement: function () {
    var self = this;

    var slider;

    this.$(function() {
      if (!self.get('numberRange.from') && !self.get('numberRange.to')) {
        self.get('numberRange').set('from', self.get('numberRange.min'));
        self.get('numberRange').set('to', self.get('numberRange.max'));
      }

      slider = self.$( ".slider" ).slider({
        range: true,
        min: self.get('numberRange.min'),
        max: self.get('numberRange.max'),
        units: self.get('numberRange.units'),
        values: [ self.get('numberRange.from'), self.get('numberRange.to') ],
        slide: function (event, ui) {
          self.set('numberRange.from', ui.values[0]);
          self.set('numberRange.to', ui.values[1]);
        },

        change: function () {
          self.sendAction('rangeChanged', self.get('numberRange'));
        }
      });
    });

    this.set('slider', slider);
  }
});
