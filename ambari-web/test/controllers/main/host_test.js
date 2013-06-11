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

/*
var App = require('app');
require('models/cluster');
require('models/service');
require('models/pagination');
require('controllers/main/host');

describe('MainHostController', function () {
    describe('#sortByName()', function () {
        it('should change isSort value to true', function () {
            var mainHostController = App.MainHostController.create();
            mainHostController.set('isSort', false);
            mainHostController.sortByName();
            expect(mainHostController.get('isSort')).to.equal(true);
        });


        it('should inverse sortingAsc ', function () {
            var mainHostController = App.MainHostController.create();
            mainHostController.set('sortingAsc', false);
            mainHostController.sortByName();
            expect(mainHostController.get('sortingAsc')).to.equal(true);
            mainHostController.sortByName();
            expect(mainHostController.get('sortingAsc')).to.equal(false);
        })
    });


    describe('#showNextPage, #showPreviousPage()', function () {
        it('should change rangeStart according to page', function () {
            var mainHostController = App.MainHostController.create();
            mainHostController.set('pageSize', 3);
            mainHostController.showNextPage();
            expect(mainHostController.get('rangeStart')).to.equal(3);
            mainHostController.showPreviousPage();
            expect(mainHostController.get('rangeStart')).to.equal(0);
        })
    });


    describe('#sortClass()', function () {
        it('should return \'icon-arrow-down\' if sortingAsc is true', function () {
            var mainHostController = App.MainHostController.create({});
            mainHostController.set('sortingAsc', true);
            expect(mainHostController.get('sortClass')).to.equal('icon-arrow-down');
        });
        it('should return \'icon-arrow-up\' if sortingAsc is false', function () {
            var mainHostController = App.MainHostController.create({});
            mainHostController.set('sortingAsc', false);
            expect(mainHostController.get('sortClass')).to.equal('icon-arrow-up');
        })
    });


    describe('#allChecked', function () {
        it('should fill selectedhostsids array', function () {
            var mainHostController = App.MainHostController.create();
            mainHostController.set('allChecked', false);
            expect(mainHostController.get('selectedHostsIds').length).to.equal(0);
            mainHostController.set('allChecked', true);
            expect(!!(mainHostController.get('selectedHostsIds').length)).to.equal(true);
        })
    });


});
*/
