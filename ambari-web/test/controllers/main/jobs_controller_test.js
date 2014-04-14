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
require('mappers/jobs/hive_jobs_mapper');
require('models/service/yarn');
require('models/jobs/job');
require('models/jobs/hive_job');
require('controllers/main/jobs_controller');

describe('App.MainJobsController', function () {
  var mainJobsController = App.MainJobsController.create();
  var filterObject = mainJobsController.get('filterObject');
  describe('#totalOfJobs', function () {
    it('should be equal to content.length', function () {
      mainJobsController.set('content', [
        Ember.Object.create({
          id: 'id0',
          queryText: 'show tables',
          name: 'id0',
          user: ['user1'],
          hasTezDag: true,
          failed: false,
          startTime: 1393443850756,
          endTime: 1393443875265,
          tezDagId: 'id0:1'
        })
      ]);
      expect(mainJobsController.get('totalOfJobs')).to.equal(1);
    });
  });
  describe('#sortingDone', function () {
    it('should be true after automatically ran sorting', function () {
      expect(mainJobsController.get('sortingDone')).to.equal(true);
    });
  });
  describe('#sortProperty', function () {
    it('should change according to sortingColumn.name', function () {
      mainJobsController.set('sortingColumn').set('name', 'id');
      expect(mainJobsController.get('sortProperty')).to.equal('id');
    });
  });
  describe('#sortAscending', function () {
    it('sorting should be ascending as default', function () {
      mainJobsController.set('sortingColumn').set('status', '');
      expect(mainJobsController.get('sortAscending')).to.equal(true);
    });
  });
  describe('#filterObject.isIdFilterApplied', function () {
    it('should be true if id field is not empty', function () {
      filterObject.set('id', 'some_id');
      expect(filterObject.get('isIdFilterApplied')).to.equal(true);
    });
  });
  describe('#filterObject.isAnyFilterApplied', function () {
    it('should be true if id field is not empty', function () {
      filterObject.createJobsFiltersLink();
      expect(filterObject.get('isAnyFilterApplied')).to.equal(true);
    });
  });
  describe('#sortedContent', function () {
    it('sorting by different properties', function () {
      mainJobsController.get('content').push(Ember.Object.create({
        id: 'id1',
        queryText: 'show tables',
        name: 'id1',
        user: ['user0'],
        failed: false,
        startTime: 1393443850757,
        endTime: 1393443875264,
        tezDagId: null
      }));
      mainJobsController.contentAndSortUpdater();
      expect(mainJobsController.get('sortedContent')[0].get('user')).to.eql(['user1']);
      mainJobsController.toggleProperty('sortAscending');
      mainJobsController.contentAndSortUpdater();
      expect(mainJobsController.get('sortedContent')[0].get('user')).to.eql(['user0']);
      mainJobsController.set('sortProperty', 'user');
      mainJobsController.contentAndSortUpdater();
      expect(mainJobsController.get('sortedContent')[0].get('id')).to.equal('id0');
      mainJobsController.get('content').push(Ember.Object.create({
        id: 'id2',
        queryText: 'show tables',
        name: 'id2',
        user: ['user2'],
        failed: false,
        startTime: 1393443850758,
        endTime: 1393443875263,
        tezDagId: null
      }));
      mainJobsController.contentAndSortUpdater();
      mainJobsController.set('sortProperty', 'startTime');
      expect(mainJobsController.get('sortedContent')[0].get('id')).to.equal('id2');
      mainJobsController.set('sortProperty', 'endTime');
      mainJobsController.contentAndSortUpdater();
      expect(mainJobsController.get('sortedContent')[0].get('id')).to.equal('id0');
      mainJobsController.set('sortProperty', 'duration');
      mainJobsController.toggleProperty('sortAscending');
      mainJobsController.contentAndSortUpdater();
      expect(mainJobsController.get('sortedContent')[0].get('user')).to.eql(['user1']);
    });
  });
});
