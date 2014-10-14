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

describe('App.ManageConfigGroupsController', function() {

	var manageConfigGroupsController = App.ManageConfigGroupsController.create({});

	describe('#addConfigGroup', function() {
		beforeEach(function() {
			manageConfigGroupsController.addConfigGroup();
		});

		describe("#validate", function() {
			it("should display no warning if user inputs valid characters into group name", function() {

				manageConfigGroupsController.addGroupPopup.set('configGroupName', 'hello');

				expect(manageConfigGroupsController.addGroupPopup.warningMessage).to.be.empty;
			});

			it("should display warning if user inputs invalid characters into group name", function() {
				manageConfigGroupsController.addGroupPopup.set('configGroupName', '/{"!@#$%');

				expect(manageConfigGroupsController.addGroupPopup.warningMessage).to.equal('Invalid Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
			});
		});
	});

	describe('#renameConfigGroup', function() {
		beforeEach(function() {
			var configGroup = Ember.Object.create ({
				name: 'name',
				description: 'description'
			});

			manageConfigGroupsController.set('selectedConfigGroup', configGroup);
			manageConfigGroupsController.renameConfigGroup();
		});

		describe("#validate", function() {
			it("should display no warning if user inputs valid characters into group name", function() {
				manageConfigGroupsController.renameGroupPopup.set('configGroupName', 'hello');

				expect(manageConfigGroupsController.renameGroupPopup.warningMessage).to.be.empty;
			});

			it("should display warning if user inputs invalid characters into group name", function() {
				manageConfigGroupsController.renameGroupPopup.set('configGroupName', '/{"!@#$%');

				expect(manageConfigGroupsController.renameGroupPopup.warningMessage).to.equal('Invalid Group Name. Only alphanumerics, hyphens, spaces and underscores are allowed.');
			});
		});
	});
});
