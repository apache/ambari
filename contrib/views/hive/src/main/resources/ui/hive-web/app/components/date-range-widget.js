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

 /* globals moment */

import Ember from 'ember';

export default Ember.Component.extend({
  displayFromDate: function () {
    return moment(this.get('dateRange.from')).format('MM/DD/YYYY');
  }.property('dateRange.from'),

  displayToDate: function () {
    return moment(this.get('dateRange.to')).format('MM/DD/YYYY');
  }.property('dateRange.to'),

  didInsertElement: function () {
    var self = this;

    if (!this.get('dateRange.min') && !this.get('dateRange.max')) {
      this.set('dateRange.max', new Date());
    }

    if (!this.get('dateRange.from') && !this.get('dateRange.to')) {
      this.set('dateRange.from', this.get('dateRange.min'));
      this.set('dateRange.to', this.get('dateRange.max'));
    }

    this.$(".fromDate").datepicker({
      defaultDate: this.get("dateRange.from"),
      onSelect: function (selectedDate) {
        self.$(".toDate").datepicker("option", "minDate", selectedDate);
        self.set('dateRange.from', new Date(selectedDate));
        self.sendAction('rangeChanged', self.get('dateRange'));
      }
    });

    this.$(".toDate").datepicker({
      defaultDate: this.get('dateRange.to'),
      onSelect: function (selectedDate) {
        self.$(".fromDate").datepicker("option", "maxDate", selectedDate);
        self.set('dateRange.to', new Date(selectedDate + " 23:59"));
        self.sendAction('rangeChanged', self.get('dateRange'));
      }
    });
  }
});
