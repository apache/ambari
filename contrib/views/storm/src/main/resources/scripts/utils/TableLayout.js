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

/**************************************************************************
-- Purpose: @file This is the common View file for displaying Table/Grid to be used overall in the application.
**************************************************************************/

define([
  'require',
  'utils/LangSupport'
], function (require, Localize) {
  'use strict';


  var TablelayoutTmpl = '<div>'+
                        '<div class="position-relative col-md-12">'+
                          '<div data-id="r_tableList" class="table-responsive tableBorder"> </div>'+
                          '<div data-id="r_tableSpinner"></div>'+
                        '</div>'+
                        '</div>';

  var TableLayout = Marionette.LayoutView.extend(
    /** @lends TableLayout */
    {
      _viewName: 'TableLayout',

      template: TablelayoutTmpl,

      /** Layout sub regions */
      regions: {
        'rTableList': 'div[data-id="r_tableList"]',
        'rTableSpinner': 'div[data-id="r_tableSpinner"]'
      },

      /** ui selector cache */
      ui: {},

      defaultGrid: {
        className: 'table table-bordered table-hover table-condensed backgrid',
        emptyText: 'No Records found!'
      },

      /** ui events hash */
      events: function () {},

      /**
       * intialize a new HDTableLayout Layout
       * @constructs
       */
      initialize: function (options) {
        _.extend(this, _.pick(options, 'collection', 'columns'));
        this.gridOpts = _.clone(this.defaultGrid,{});
        _.extend(this.gridOpts, options.gridOpts, {
          collection: this.collection,
          columns: this.columns
        });

        this.bindEvents();
      },

      /** all events binding here */
      bindEvents: function () {
        this.listenTo(this.collection, 'request', function () {
          this.$('div[data-id="r_tableSpinner"]').addClass('loading');
        }, this);
        this.listenTo(this.collection, 'sync', function () {
          this.$('div[data-id="r_tableSpinner"]').removeClass('loading');
        }, this);
        this.listenTo(this.collection, 'error', function () {
          this.$('div[data-id="r_tableSpinner"]').removeClass('loading');
        }, this);
      },

      /** on render callback */
      onRender: function () {
        this.renderTable();
      },

      /**
       * show table
       */
      renderTable: function () {
        this.rTableList.show(new Backgrid.Grid(this.gridOpts));
      },

      /** on close */
      onClose: function () {},
    });

  return TableLayout;
});