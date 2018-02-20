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

App.Timeline = Em.View.extend({
  templateName: require('templates/common/timeline'),

  //bound from template
  events: [],

  //custom sort function
  sort: null,

  //path to property to sort by  
  datePath: null,

  //default sort function
  byDate: function (datePath) {
    //use datePath to find dates to compare if provided as a string
    if (datePath && typeof datePath === "string") {
      return (a, b) => {
        let valueA = a, valueB = b;
        const path = datePath.split(".");
        
        for (let x = 0, length = path.length, prop = path[x]; x < length; x++, prop = path[x]) {
          valueA = valueA[prop];
          valueB = valueB[prop];
        }

        return valueA - valueB;
      }
    }
    
    //otherwise, assume a first level property named "date"
    return (a, b) => a.date - b.date;
  },

  sortFunction: function () {
    return this.get('sort') || this.byDate(this.get('datePath'))
  }.observes('sort', 'datePath'),

  sortedEvents: function () {
    const sortFunction = this.sortFunction();
    const events = this.get('events');
    return events.sort(sortFunction);
  }.property('events')
});