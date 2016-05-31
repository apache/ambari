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

  updateMinDate: function () {
    if (this.get('rendered')) {
      this.$('.toDate').datepicker("option", "minDate", new Date(this.get('dateRange.from')));
    }
  }.observes('dateRange.from'),

  updateMaxDate: function () {
    if (this.get('rendered')) {
      this.$('.fromDate').datepicker("option", "maxDate", new Date(this.get('dateRange.to')));
    }
  }.observes('dateRange.to'),

  didInsertElement: function () {
    var self = this;
    var dateRange = this.get('dateRange');

    if (!dateRange.get('min') && !dateRange.get('max')) {
      dateRange.set('max', new Date());
    }

    if (!dateRange.get('from') && !dateRange.get('to')) {
      dateRange.set('from', dateRange.get('min'));
      dateRange.set('to', dateRange.get('max'));
    }

    this.$(".fromDate").datepicker({
      defaultDate: new Date(dateRange.get("from")),
      maxDate: new Date(dateRange.get('to')),

      onSelect: function (selectedDate) {
        self.$(".toDate").datepicker("option", "minDate", selectedDate);

        dateRange.set('from', new Date(selectedDate).getTime());
        self.sendAction('rangeChanged', dateRange);
      }
    });

    this.$(".toDate").datepicker({
      defaultDate: new Date(dateRange.get('to')),
      minDate: new Date(dateRange.get('from')),

      onSelect: function (selectedDate) {
        selectedDate += ' 23:59';

        self.$(".fromDate").datepicker("option", "maxDate", selectedDate);

        dateRange.set('to', new Date(selectedDate).getTime());
        self.sendAction('rangeChanged', dateRange);
      }
    });

    this.set('rendered', true);
  }
});
