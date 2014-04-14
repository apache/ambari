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
require('views/main/jobs_view');

describe('App.MainJobsView', function () {
  var mainJobsView = App.MainJobsView.create({
    controller: Ember.Object.create({
      filterObject: Ember.Object.create(),
      navIDs: Ember.Object.create({
        backIDs: ['id0', 'id1'],
        nextID: 'id2'
      }),
      content: [
        Ember.Object.create({
          id: 'id',
          query_text: 'show tables',
          name: 'id',
          user: ['user'],
          failed: false,
          startTime: 1393443850756,
          endTime: 1393443875265,
          tezDagId: 'id:1',
          hasTezDag: true
        })
      ]
    })
  });
  mainJobsView.get('controller').set('sortedContent', mainJobsView.controller.get('content').toArray());
  var jobNameView = mainJobsView.jobNameView.create();
  jobNameView.set('job', mainJobsView.get('controller.content').objectAt(0));
  describe('#noDataToShow', function () {
    it('should be false if content is not empty', function () {
      mainJobsView.noDataToShowObserver();
      expect(mainJobsView.get('noDataToShow')).to.equal(false);
    });
  });
  describe('#rowsPerPageSelectView.disabled', function() {
    it('should be true if controller.navIDs.backIDs.length > 1', function () {
      var rowsPerPageSelectView = mainJobsView.rowsPerPageSelectView.create({
        parentView: Ember.View.create({
          hasBackLinks: true
        })
      });
      rowsPerPageSelectView.disabledObserver();
      expect(rowsPerPageSelectView.get('disabled')).to.equal(true);
    });
  });
  describe('#hasNextJobs', function () {
    it('should be true if controller.navIDs.nextID is not empty', function () {
      expect(mainJobsView.get('hasNextJobs')).to.equal(true);
    });
  });
  describe('#hasBackLinks', function () {
    it('should be true if there are several controller.navIDs.backIDs', function () {
      expect(mainJobsView.get('hasBackLinks')).to.equal(true);
    });
  });
  describe('#jobsPaginationLeft.class', function () {
    it('should be paginate_next if there are new jobs and no filter is applied', function () {
      mainJobsView.get('controller.filterObject').set('isAnyFilterApplied', false);
      var jobsPaginationLeft = mainJobsView.jobsPaginationLeft.create({
        parentView: Ember.View.create({
          hasBackLinks: true
        })
      });
      expect(jobsPaginationLeft.get('class')).to.equal('paginate_previous');
    });
  });
  describe('#jobsPaginationRight.class', function () {
    it('should be paginate_next if there are new jobs and no filter is applied', function () {
      var jobsPaginationRight = mainJobsView.jobsPaginationRight.create({
        parentView: Ember.View.create({
          hasNextJobs: true
        })
      });
      expect(jobsPaginationRight.get('class')).to.equal('paginate_next');
    });
  });
  describe('#jobNameView.isLink', function () {
    it('should be empty if job has Tez DAG', function () {
      jobNameView.isLinkObserver();
      expect(jobNameView.get('isLink')).to.equal('');
    });
  });
});
