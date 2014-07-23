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

App.ConfigHistoryFlowView = Em.View.extend({
  templateName: require('templates/common/configs/config_history_flow'),

  /**
   * index of the first element(service version box) in viewport
   */
  startIndex: 0,
  showLeftArrow: true,
  showRightArrow: false,

  currentServiceVersion: function () {
    return this.get('serviceVersions').findProperty('current');
  }.property('serviceVersions.@each.current'),
  /**
   * identify whether to show link that open whole content of notes
   */
  showMoreLink: function () {
    //100 is number of symbols that fit into label
    return (this.get('currentServiceVersion.notes.length') > 100);
  }.property('currentServiceVersion.notes.length'),
  /**
   * formatted notes ready to display
   */
  shortNotes: function () {
    //100 is number of symbols that fit into label
    if (this.get('showMoreLink')) {
      return this.get('currentServiceVersion.notes').slice(0, 100) + '...';
    }
    return this.get('currentServiceVersion.notes');
  }.property('currentServiceVersion'),
  /**
   * service versions which in viewport and visible to user
   */
  visibleServiceVersion: function () {
    return this.get('serviceVersions').slice(this.get('startIndex'), (this.get('startIndex') + 5));
  }.property('startIndex'),

  willInsertElement: function () {
    var serviceVersions = this.get('serviceVersions');
    var startIndex = 0;
    var numberOfDisplayed = 5;

    if (serviceVersions.length > numberOfDisplayed) {
      startIndex = serviceVersions.length - numberOfDisplayed;
      this.set('startIndex', startIndex);
      this.adjustFlowView();
    }
  },
  /**
   *  define the first element in viewport
   *  change visibility of arrows
   */
  adjustFlowView: function () {
    var startIndex = this.get('startIndex');

    this.get('serviceVersions').forEach(function (serviceVersion, index) {
      serviceVersion.set('first', (index === startIndex));
    });
    this.set('showLeftArrow', (startIndex !== 0));
    this.set('showRightArrow', ((startIndex + 5) !== this.get('serviceVersions.length')));
  },
  serviceVersions: [
    Em.Object.create({
      serviceName: 'HDFS',
      version: '1',
      date: 'Apr 4, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '2',
      date: 'Apr 4, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '3',
      date: 'Apr 4, 2014',
      author: 'user',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '4',
      date: 'Apr 4, 2014',
      author: 'user',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '5',
      date: 'Apr 4, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '22',
      date: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '33',
      date: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '44',
      date: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '55',
      date: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes'
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      version: '666',
      date: 'Apr 9, 2014',
      author: 'admin',
      notes: 'notes',
      current: true
    })
  ],
  /**
   * move back to the previous service version
   */
  shiftBack: function () {
    this.decrementProperty('startIndex');
    this.adjustFlowView();
  },
  /**
   * move forward to the next service version
   */
  shiftForward: function () {
    this.incrementProperty('startIndex');
    this.adjustFlowView();
  }
});
