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

define(['require',
    'backbone',
    'moment',
    'd3.tip',
    'utils/Globals',
    'utils/Utils',
    'collections/VLogList',
    'hbs!tmpl/dashboard/GridTableLayoutView_tmpl',
    'bootstrap-daterangepicker',
    'nv'
], function(require, Backbone, moment, tip, Globals, Utils, VLogList, GridTableLayoutViewTmpl, daterangepicker) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends GridGraphLayoutView */
        {
            _viewName: 'GridGraphLayoutView',

            template: GridTableLayoutViewTmpl,


            /** ui selector cache */
            ui: {
                dateRange: "#dateRange",
                selectDateRange: ".selectDateRange",
                dateRangeTitle: "span[data-id='dateRangeTitle']",
                gridSettingPopup: "[data-id='gridSettingPopup']",
                gridHeader: ".gridHeader"

            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.viewType] = 'onViewTypeChange';
                events['click [data-id="refresh-tab-graph"]'] = 'onTabRefresh';
                events['click ' + this.ui.gridSettingPopup] = 'onGridSettingPopupClick';
                return events;
            },

            /**
             * intialize a new GridGraphLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params', 'viewType', 'dashboard', 'model', 'gridHelp'));
                this.vent = new Backbone.Wreqr.EventAggregator();
                this.collection = new VLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 999999999,

                    }
                });

                this.dateUtil = Utils.dateUtil;
                this.dateRangeLabel = new String();

                this.bindEvents();
                this.graphParams = {};
                this.unit = (this.model.params && this.model.params.unit) ? this.model.params.unit : "+1HOUR";
                this.firstRender = true;
                this.collection.url = Globals.baseURL + "audit/logs/anygraph";
                this.collection.modelAttrName = "graphData";

            },
            bindEvents: function() {
                this.listenTo(this.collection, "reset", function(collection) {
                    this.$(".loader").hide();
                    this.$("#loaderGraph").hide();
                }, this);
                this.listenTo(this.collection, 'request', function() {
                    this.$(".loader").show();
                    this.$("#loaderGraph").show();
                }, this);
                this.listenTo(this.collection, 'sync error', function() {
                    this.$(".loader").hide();
                    this.$("#loaderGraph").hide();
                }, this);
                this.listenTo(this.vent, "graph:update", function(options) {

                    options['params'] = this.params
                    if (this.model) {
                        this.model.clear().set(options)
                    }
                    this.configureChart(options);
                }, this);
                this.listenTo(this.vent, "graph:data:update", function(params, options) {
                    this.params = params
                    options['params'] = this.params
                    if (this.model) {
                        this.model.clear().set(options)
                    }
                    this.fetchGraphData(params, options);
                }, this);
                this.listenTo(this.vent, "table:grid:update", function(options) {
                    options['params'] = this.params
                    if (this.model) {
                        this.model.clear().set(options)
                    }
                    this.updateGrid(options)
                }, this);

            },
            onRender: function() {
                var that = this;
                if (this.model) {
                    var mObject = this.model.toJSON();
                    if (mObject.params) {
                        this.params = mObject.params;
                        this.fetchGraphData(mObject.params, mObject);
                    }
                    this.updateGrid(mObject);
                }
                if (this.gridHelp) {
                    setTimeout(function() {
                        this.$('.gridSettinghand').hide();
                    }, 3000);
                } else {
                    this.$('.gridSettinghand').hide();
                }

            },

            fetchGraphData: function(params, options) {
                var that = this;
                that.$("#loaderGraph").show();
                that.$(".loader").show();
                _.extend(this.collection.queryParams, params);
                this.collection.fetch({
                    reset: true,
                    success: function() {
                        that.createDataForGraph(options)
                    }
                });
            },
            updateGrid: function(options) {
                if (options.title) {
                    this.ui.gridHeader.find('.gridTitle').text(options.title);
                }
            },
            setupDialog: function(options) {
                var that = this;
                require(['views/common/JBDialog'], function(JBDialog) {
                    var opts = _.extend({
                        appendTo: that.$el,
                        modal: true,
                        resizable: false,
                        beforeClose: function(event, ui) {
                            that.onDialogClosed();
                        }
                    }, options);
                    var dialog = that.dialog = new JBDialog(opts).render().open();
                })

            },
            onGridSettingPopupClick: function() {
                this.$('.gridSettinghand').hide();
                this.dashboard.disable();
                var that = this;
                var overlay = document.createElement('div');
                overlay.setAttribute('class', 'overlayDashboard');
                this.$el.append(overlay);
                require(['views/dialog/GridGraphSettingView'], function(GridGraphSettingView) {
                    var view = new GridGraphSettingView({
                        vent: that.vent,
                        params: (that.model) ? (that.params) : ({}),
                        model: that.model,
                        viewType: that.viewType
                    });

                    that.setupDialog({
                        title: "Setting",
                        content: view,
                        viewType: 'Save',
                        width: 560,
                        height: 500,
                        buttons: [{
                            id: "cancelBtn",
                            text: "Close",
                            "class": "btn btn-default",
                            click: function() {
                                that.onDialogClosed();
                            }
                        }]
                    });
                });
            },
            onDialogClosed: function() {
                this.$el.find('.overlayDashboard').remove();
                this.dashboard.enable();
                if (this.dialog) {
                    this.dialog.close && this.dialog.close();
                    this.dialog.remove && this.dialog.remove();
                    this.dialog = null;
                }
            }
        });


});
