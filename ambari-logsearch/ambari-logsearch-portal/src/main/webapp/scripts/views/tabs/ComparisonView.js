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
    'utils/Utils',
    'moment',
    'utils/Globals',
    'collections/VLogList',
    'hbs!tmpl/tabs/ComparisonView_tmpl',
], function(require, Backbone, Utils, moment, Globals, VLogList, ComparisonViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends ComparisonView */
        {
            _viewName: 'ComparisonView',

            template: ComparisonViewTmpl,

            regions: {
                RVSSearch: "#r_VSSearch",
                RLogTable: "#rLogTable",
                RDatePicker: "#r_DatePicker"
            },
            /** ui selector cache */
            ui: {
                dateRange: "#dateRange",
                selectDateRange: ".selectDateRange",
                tabTitle: "[data-id='tabTitle']"

            },

            /** ui events hash */
            events: function() {
                var events = {};
                return events;
            },
            /**
             * intialize a new ComparisonView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params', 'datePickerPosition'));
                this.vent = new Backbone.Wreqr.EventAggregator();
                this.collection = new VLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 25
                    }
                });
                this.dateUtil = Utils.dateUtil;
                this.collection.url = Globals.baseURL + "service/logs";
                this.bindEvents();
                this.dateRangeLabel = "Last 1 Hour"
            },
            bindEvents: function() {
                this.listenTo(this.vent, "search:include:exclude", function(value) {
                    this.fetchCollection(value);
                }, this);
                this.listenTo(this.vent, "logtime:filter", function(value) {
                    this.fetchCollection(value);
                }, this);
            },
            onRender: function() {
                if (this.params) {
                    this.fetchCollection(this.params);
                    if (this.params.component && this.params.host) {
                        this.ui.tabTitle.html(this.params.host + ' <i class="fa fa-angle-double-right"></i> ' + this.params.component)
                    }
                } else {
                    this.fetchCollection({
                        "q": "*:*"
                    });
                }
                this.renderVisualSearch();
                this.renderDatePicker();
                this.renderTable();
                /* if (this.params.from && this.params.to) {
                     this.setDateText(this.dateUtil.getMomentObject(this.params.from), this.dateUtil.getMomentObject(this.params.to));
                 }*/

            },
            renderVisualSearch: function() {
                var that = this;
                require(['views/tabs/VisualSearchView'], function(VisualSearchView) {
                    that.RVSSearch.show(new VisualSearchView({
                        vent: that.vent,
                        globalVent: that.globalVent,
                        params: that.params,
                        eventName: "search:include:exclude"
                    }));
                })

            },
            renderDatePicker: function() {
                var that = this;
                require(['views/common/DatePickerLayout'], function(DatePickerLayout) {
                    that.RDatePicker.show(new DatePickerLayout({
                        vent: that.vent,
                        globalVent: that.globalVent,
                        params: that.params,
                        datePickerPosition: that.datePickerPosition,
                        parentEl: that.$el,
                        rangeLabel: true
                    }));
                });
            },
            fetchCollection: function(params) {
                var that = this;
                this.$('#loaderToolbar').show();
                _.extend(this.params, params);
                $.extend(this.collection.queryParams, params);
                this.collection.getFirstPage({
                    reset: true,
                    complete: function() {
                        that.$('#loaderToolbar').hide();
                    }
                });
            },
            renderTable: function() {
                var that = this;
                require(['views/common/TableLayout', 'views/common/CustomBackgrid'], function(TableLayout, CustomBackgrid) {
                    var cols = new Backgrid.Columns(that.getColumns());
                    that.RLogTable.show(new TableLayout({
                        columns: cols,
                        collection: that.collection,
                        includeFilter: false,
                        includePagination: true,
                        includePageSize: true,
                        includeFooterRecords: true,
                        includeColumnManager: true,
                        columnOpts: {
                            initialColumnsVisible: 2,
                            saveState: false
                        },
                        gridOpts: {
                            header: CustomBackgrid,
                            //row: IdRow,
                            emptyText: 'No records found!'
                        },
                        filterOpts: {},
                        paginatorOpts: {}
                    }));
                });
            },
            getColumns: function() {
                var timeZone = moment().zoneAbbr();
                this.cols = {
                    logtime: {
                        label: "Log Time " + (!_.isEmpty(timeZone) ? "(" + timeZone + ")" : ""),
                        cell: "html",
                        editable: false,
                        sortType: 'toggle',
                        direction: "descending",
                        orderable: true,
                        displayOrder: 1,
                        width: 17,
                        className: "logTime",
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var str = "";
                                if (rawValue)
                                    str += "<p>" + moment(rawValue).format("YYYY-MM-DD HH:mm:ss,SSS") + "</p>";
                                if (model.get("type"))
                                    str += "<p style='float:left;width:90%'>" + (model.get("level") ? "<label class='label label-" + (""+model.get("level")).toUpperCase() + "'>" + (""+model.get("level")).toUpperCase() + "</label>" : "") +
                                    /* "<strong>" + model.get("type") + "</strong>" +*/
                                    "</p><a  style='width:9%' title='Open logs in new tab' data-type='C' data-host='" + model.get("host") + "' data-node='" + model.get("type") + "' href='javascript:void(0)' class='pull-right hidden'><i class='fa fa-share'></i></a>";
                                //                              if(model.get("level"))
                                //                                  str += "<p style='float:left;'><label class='label label-"+model.get("level")+"'>"+model.get("level")+"</label></p>";
                                return str;
                            }
                        })
                    },
                    log_message: {
                        label: "Message",
                        cell: "html",
                        editable: false,
                        sortType: 'toggle',
                        sortable: false,
                        //width : "50",
                        orderable: true,
                        displayOrder: 4,
                        className: "logMessage",
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                return (rawValue) ? "<pre>" + Utils.escapeHtmlChar(rawValue) + "</pre>" : "";
                            }
                        })
                    },
                    thread_name: {
                        label: "Thread",
                        cell: "String",
                        editable: false,
                        sortType: 'toggle',
                        sortable: false,
                        orderable: true,
                        displayOrder: 5,
                        width: 10
                    },
                    logger_name: {
                        label: "Logger",
                        cell: "String",
                        editable: false,
                        sortType: 'toggle',
                        sortable: false,
                        orderable: true,
                        displayOrder: 6,
                        width: 13
                    },
                    bundle_id: {
                        label: "Bundle Id",
                        cell: "String",
                        editable: false,
                        sortType: 'toggle',
                        sortable: false,
                        orderable: true,
                        displayOrder: 6,
                        width: 6
                    }

                };
                return this.collection.constructor.getTableCols(this.cols, this.collection);
            },
            setDateText: function(start, end) {

                this.ui.dateRange.val(this.dateUtil.getTimeZone(start, "MM/DD/YYYY H:mm:ss,SSS") + ' - ' + this.dateUtil.getTimeZone(end, "MM/DD/YYYY H:mm:ss,SSS"));
                this.ui.dateRange.data('daterangepicker').setStartDate(start);
                this.ui.dateRange.data('daterangepicker').setEndDate(end);

            },

        });


});
