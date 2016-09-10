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
    'utils/Globals',
    'utils/Utils',
    'moment',
    'utils/ViewUtils',
    'collections/VLogList',
    'hbs!tmpl/tabs/LogFileView_tmpl',
    'views/common/JBDialog',
    'views/tabs/ExportLogFileView',
    'select2'
], function(require, Backbone, Globals, Utils, moment, ViewUtils, VLogList, LogFileTmpl, JBDialog, ExportLogFileView) {

    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends LogFileView */
        {
            _viewName: 'LogFileView',

            template: LogFileTmpl,
            /** Layout sub regions */
            regions: {
                RLogFileTable: "#logFileTable",
                RLogLevel: "#r_LogLevel",
                //RVSSearch : "#r_VSSearch",
                RHistogram: "#r_Histogram",
                RTimer: "[data-id='timer']",
                RDatePicker: "#r_DatePicker",
                RLogSnapShot: "#r_LogSnapShot",
                RVisualSearchIncCol: "[data-id='r_vsSearchIncCol']",
                RVisualSearchExCol: "[data-id='r_vsSearchExCol']"
            },

            /** ui selector cache */
            ui: {
                searchBoxBtn: '[data-id="hierarchySearchBtn"]',
                searchBox: '[data-id="hierarchySearch"]',
                find: '[data-id="find"]',
                next: '[data-id="next"]',
                prev: '[data-id="prev"]',
                lock: '[data-id="lock"]',
                last: '[data-id="last"]',
                first: '[data-id="first"]',
                cancelFind: '[data-id="cancelFind"]',
                contextMenu: ".contextMenu",
                pageNotation: ".pageNotation",
                clearSearch: ".clearSearch"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.searchBoxBtn] = 'onSearchLogClick';
                events['click #searchLog'] = 'onSearchLogClick';
                events['click [data-id="refresh-tab"]'] = 'onTabRefresh';
                events['click ' + this.ui.first] = 'onFindFirst';
                events['click ' + this.ui.prev] = 'onFindNxt';
                events['click ' + this.ui.next] = 'onFindNxt';
                events['click ' + this.ui.last] = 'onFindLast';
                events["keyup " + this.ui.find] = 'onFindKeyPress';
                events['click .clearSearch'] = 'onClearSearchClick';
                events["click " + this.ui.lock] = 'onLockToggle';
                events["click " + this.ui.cancelFind] = 'onCancelFindClick';
                events["click .contextMenu li a"] = 'onDropDownMenuClick';
                events['click [data-id="export-logs-text"]'] = 'onExportLogClick';
                events['click .export-dropdown a'] = 'exportLogFile';
                events['change [data-id="toggleTable"]'] = 'onToggleTableView';
                events['click .quickMenu li'] = 'onQuickMenuClick';
                return events;
            },

            /**
             * intialize a new LogFileView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'globalVent', 'params'));
                this.logFileCollection = new VLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 25
                    }
                });
                this.logFileCollection.url = Globals.baseURL + "service/logs";
                this.vent = new Backbone.Wreqr.EventAggregator();
                this.bindEvents();
                this.commonTableOptions = {
                    collection: this.logFileCollection,
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
                        className: "table table-bordered table-striped table-hover table-condensed backgrid table-quickMenu",
                        //header : CustomBackgrid,
                        //row: IdRow,
                        emptyText: 'No records found!'
                    },
                    filterOpts: {},
                    paginatorOpts: {}
                };
            },
            onRender: function() {
                var that = this;
                this.fetchCollection((this.params) ? this.params : { q: "*:*" });
                this.renderHistogram();
                //this.renderTable();
                this.renderDatePicker();
                this.renderTableLikeLogFile();
                this.renderLogLevel();
                this.renderVisualSearch();
                this.renderTimer();
                this.setHostName();
                this.renderLogSnapShot();
                this.$('*:not(.export-dropdown)').on("click", function() {
                    that.$(".export-dropdown").hide();
                });
            },
            onShow: function() {
                this.findScrollEvent();
            },
            onTabRefresh: function() {
                ViewUtils.setLatestTimeParams(this.params);
                this.fetchTableCollection(this.params);
                this.vent.trigger("tab:refresh");
            },
            onExportLogClick: function(e) {
                //			console.log("clicked");
                //			this.$(".export-dropdown").show();
                var that = this;
                require(['views/common/JBDialog', ], function(JBDialog) {
                    var view = new ExportLogFileView();
                    var opts = _.extend({
                        title: "Export",
                        content: view,
                        viewType: 'Update',
                        appendTo: that.$el,
                        modal: true,
                        resizable: false,
                        width: 550,
                        beforeClose: function(event, ui) {
                            that.onDialogClosed();
                        },
                        buttons: [{
                            id: "okBtn",
                            text: "Export",
                            "class": "btn btn-primary",
                            click: function() {
                                that.onDialogSubmitted();
                            }
                        }, {
                            id: "cancelBtn",
                            text: "Cancel",
                            "class": "btn btn-default",
                            click: function() {
                                that.onDialogClosed();
                            }
                        }]
                    });
                    var dialog = that.dialog = new JBDialog(opts).render().open();
                })

            },
            onDialogSubmitted: function() {
                var obj = Utils.getFormData(this.dialog.$(".form-horizontal").serializeArray());
                this.downloadLogFile(obj);
            },
            /** closing the movable/resizable popup */
            onDialogClosed: function() {
                if (this.dialog) {
                    this.dialog.close && this.dialog.close();
                    this.dialog.remove && this.dialog.remove();
                    this.dialog = null;
                }
            },
            downloadLogFile: function(obj) {
                obj.utcOffset = moment().utcOffset();
                obj.startIndex = this.logFileCollection.state.currentPage * this.logFileCollection.state.pageSize;
                //			var params = $.param(_.pick(_.extend({},this.logFileCollection.queryParams,
                //				{startIndex : this.logFileCollection.state.currentPage * this.logFileCollection.state.pageSize},obj),
                //				'component','from','to','host','level','unit','startIndex','pageSize','format','utcOffset'));
                var params = $.param(_.extend({}, this.logFileCollection.queryParams, obj));
                var url = "api/v1/service/logs/export?" + params;
                window.open(url);
                this.onDialogClosed();
            },
            fetchCollection: function(params) {
                var that = this;
                this.$('#loaderToolbar').show();
                _.extend(this.params, params);
                $.extend(this.logFileCollection.queryParams, params);
                this.logFileCollection.getFirstPage({
                    reset: true,
                    complete: function() {
                        that.$('#loaderToolbar').hide();
                    }
                });
            },
            fetchTableCollection: function(queryParams, param) {
                var that = this;
                $.extend(this.logFileCollection.queryParams, queryParams);
                this.logFileCollection.fetch(_.extend({
                    reset: true,
                    beforeSend: function() {
                        that.$("#loaderToolbar").show();
                    }
                }, param));
            },
            onSearchLogClick: function() {
                var value = this.ui.searchBox.val();
                if (_.isEmpty(value)) {
                    this.ui.searchBox.val("*:*");
                    value = "*:*";
                }
                this.vent.trigger("main:search", { q: value });
            },
            bindEvents: function() {
                this.listenTo(this.logFileCollection, 'request', function() {
                    this.$("#loader").show();
                }, this);
                this.listenTo(this.logFileCollection, 'sync error', function() {
                    this.$("#loader").hide();
                    this.selectionText = "";
                }, this);
                this.listenTo(this.logFileCollection, "backgrid:refresh", function() {
                    this.setupFind();
                    var that = this;
                    var element = this.$("tr[data-id='" + this.params.sourceLogId + "']");
                    if (this.params && this.params.sourceLogId) {
                        this.params.sourceLogId = undefined;
                        (this.logFileCollection.queryParams.sourceLogId) ? this.logFileCollection.queryParams.sourceLogId = undefined: "";
                        if (element.offset()) {
                            var top = element.offset().top;
                            element.addClass('highlightLog');
                            $("html, body").animate({ scrollTop: (top - 200) }, 1);
                            /*setTimeout(function(){
                            	element.addClass('fadeOutColor')
                            	setTimeout(function(){element.removeClass('fadeOutColor highlightLog');},4000)
                            },6000);*/
                        }
                    }
                    this.$("#loaderToolbar").hide();
                }, this);
                this.listenTo(this.vent, "level:filter", function(value) {
                    this.fetchCollection(value);
                }, this);
                this.listenTo(this.vent, "search:include:exclude " + Globals.eventName.serviceLogsIncludeColumns + " " + Globals.eventName.serviceLogsExcludeColumns, function(value) {
                    this.fetchCollection(value);
                }, this);
                this.listenTo(this.vent, "main:search", function(value) {
                    this.fetchCollection(value);
                }, this);
                this.listenTo(this.vent, "logtime:filter", function(value) {
                    this.fetchCollection(value);
                }, this);
                this.listenTo(this.globalVent, "globalExclusion:component:message", function(value) {
                    this.fetchCollection(value);
                }, this);
                this.listenTo(this.vent, "tab:refresh", function(params) {
                	this.fetchTableCollection(params);
                },this);
                this.listenTo(this.vent, "timer:end", function(value) {
                    //timer should start only after log table fetch is complete.
                    ViewUtils.setLatestTimeParams(this.params);
                    this.vent.trigger("tab:refresh", this.params);
                    var that = this;
                    this.fetchTableCollection(this.params, {
                        complete: function() {
                            that.vent.trigger("start:timer");
                        }
                    });
                }, this);
            },
            renderLogLevel: function() {
                var that = this;
                require(['views/dashboard/LogLevelBoxView'], function(LogLevelBoxView) {
                    that.RLogLevel.show(new LogLevelBoxView({
                        vent: that.vent,
                        globalVent: that.globalVent,
                        params: that.params
                    }));
                });
            },
            renderVisualSearch: function() {
                var that = this;
                var data = _.values(Globals.serviceLogsColumns);
                var columns = _.without(data, _.findWhere(data, "logtime"));
                require(['views/tabs/VisualSearchView'], function(VisualSearchView) {
                    /*that.RVSSearch.show(new VisualSearchView({
                    	viewName : "includeExclude",
                    	vent : that.vent,
                    	globalVent:that.globalVent,
                    	params : that.params,
                    	eventName : "search:include:exclude"
                    }));*/
                    that.RVisualSearchIncCol.show(new VisualSearchView({
                        params: that.params,
                        viewName: "includeServiceColumns",
                        placeholder: "Include Search",
                        vent: that.vent,
                        globalVent: that.globalVent,
                        customOptions: columns,
                        eventName: Globals.eventName.serviceLogsIncludeColumns,
                        myFormatData: function(query, searchCollection) {
                            var obj = [];
                            searchCollection.each(function(m) {
                                var data = {};
                                data[m.get("category")] = m.get("value");
                                obj.push(data);
                            });
                            return {
                                includeQuery: JSON.stringify(obj),
                                query: query
                            }
                        }
                    }));
                    that.RVisualSearchExCol.show(new VisualSearchView({
                        params: that.params,
                        viewName: "excludeServiceColumns",
                        placeholder: "Exclude Search",
                        vent: that.vent,
                        globalVent: that.globalVent,
                        customOptions: columns,
                        eventName: Globals.eventName.serviceLogsExcludeColumns,
                        myFormatData: function(query, searchCollection) {
                            var obj = [];
                            searchCollection.each(function(m) {
                                var data = {};
                                data[m.get("category")] = m.get("value");
                                obj.push(data);
                            });
                            return {
                                excludeQuery: JSON.stringify(obj),
                                query: query
                            }
                        }
                    }));
                });
            },
            renderHistogram: function() {
                var that = this;
                require(['views/graphs/GraphLayoutView'], function(GraphLayoutView) {
                    that.RHistogram.show(new GraphLayoutView({
                        vent: that.vent,
                        globalVent: that.globalVent,
                        params: that.params,
                        showUnit : true
                    }));
                });
            },
            getIdRowForTableLayout : function(){
            	var IdRow =  Backgrid.Row.extend({
				    render: function() {
				        IdRow.__super__.render.apply(this, arguments);
				        if (this.model.has("id")) {
				            this.$el.attr("data-id", this.model.get('id'));
				        }
				        return this;
				    }
				});
            	return IdRow;
            },
            renderTableLikeLogFile: function() {
                var that = this;
                require(['views/common/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getTableLikeLogFileColumns());
                    that.RLogFileTable.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols,
                        includeColumnManager: false,
                        gridOpts: {
                        	row: that.getIdRowForTableLayout(),
                            className: "table table-bordered table-hover table-condensed backgrid logFileFont table-quickMenu",
                        },
                    })));
                });
            },
            renderTable: function() {
                var that = this;
                require(['views/common/TableLayout'], function(TableLayout) {
                    var cols = new Backgrid.Columns(that.getColumns());
                    that.RLogFileTable.show(new TableLayout(_.extend({}, that.commonTableOptions, {
                        columns: cols,
                        gridOpts: {
                        	row: that.getIdRowForTableLayout(),
                        }
                    })));
                });
            },
            renderTimer: function() {
                var that = this;
                require(['views/common/TimerView'], function(TimerView) {
                    that.RTimer.show(new TimerView({
                        vent: that.vent,
                        globalVent: that.globalVent
                    }));
                });
            },
            renderDatePicker: function() {
                var that = this;
                require(['views/common/DatePickerLayout'], function(DatePickerLayout) {
                    that.RDatePicker.show(new DatePickerLayout({
                        vent: that.vent,
                        globalVent: that.globalVent,
                        params: that.params,
                        datePickerPosition: 'left',
                        rangeLabel: true,
                        parentEl: that.$el.find('.topLevelFilter')
                    }));
                });
            },
            renderLogSnapShot: function() {
                var that = this;
                require(['views/common/LogSnapShotLayout'], function(LogSnalShopLayout) {
                    that.RLogSnapShot.show(new LogSnalShopLayout({
                        vent: that.vent,
                        globalVent: that.globalVent,
                        params: that.params,
                    }));
                });
            },
            setupFind: function() {
                var that = this;
                that.ui.contextMenu.hide();
                //that.ui.find.trigger("keyup");
                setTimeout(function() {
                    that.initializeContextMenu();
                    that.ui.find.trigger("keyup");
                }, 1000);
            },
            getTableLikeLogFileColumns: function() {
                var timeZone = moment().zoneAbbr(),
                    that = this;
                return this.logFileCollection.constructor.getTableCols({
                    logtime: {
                        label: "Log Time " + (!_.isEmpty(timeZone) ? "(" + timeZone + ")" : ""),
                        cell: "html",
                        editable: false,
                        sortType: 'toggle',
                        sortable: true,
                        //width : "50",
                        orderable: true,
                        displayOrder: 4,
                        //className : "logMessage",
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {

                                return (rawValue) ? "<div style='position:relative; padding-left:15px'>"+ViewUtils.foramtLogMessageAsLogFile(model) + that.getDropdownQuickMenuHtml() + "</div>" : "";
                            }
                        })
                    }
                }, this.logFileCollection);

            },
            getColumns: function() {
                var timeZone = moment().zoneAbbr(),
                    that = this,
                    cols = {};
                this.cols = {
                    logtime: {
                        label: "Log Time " + (!_.isEmpty(timeZone) ? "(" + timeZone + ")" : ""),
                        cell: "html",
                        editable: false,
                        sortType: 'toggle',
                        direction: "descending",
                        orderable: true,
                        displayOrder: 1,
                        width: "17",
                        className: "logTime",
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function(rawValue, model) {
                                var str = "";
                                if (rawValue)
                                    str += "<div style='position:relative'><p style='margin-left:20px'>" + moment(rawValue).format("YYYY-MM-DD HH:mm:ss,SSS") + "</p>";
                                if (model.get("level"))
                                    str += "<p style='margin-left:20px'><label class='label label-" + (""+model.get("level")).toUpperCase() + "'>" + (""+model.get("level")).toUpperCase() + "</label></p>";
                                str += that.getDropdownQuickMenuHtml()+"</div>";
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
                _.each(this.columns, function(value){
                  var name = Globals.invertedServiceLogMappings[value];
                  if (columns[name] === undefined) {
                    var columnObj = {
                      name: Globals.invertedServiceLogMappings[value],
                      label:value,
                      cell: "String",
                      sortType: 'toggle',
                      editable: false
                    };
                    columns[name] = columnObj;
                  } else {
                    if (columns[name] && columns[name].label) {
                      columns[name].label = value;
                    }
                  }
                });
                return this.logFileCollection.constructor.getTableCols(this.cols, this.logFileCollection);
            },
            initializeContextMenu: function() {
                var that = this;

                $('body').on("mouseup.contextMenuLogFile", function(e) {
                    var selection;
                    if (window.getSelection) {
                        selection = window.getSelection();
                    } else if (document.selection) {
                        selection = document.selection.createRange();
                    }
                    if (_.isEmpty(selection.toString())) {
                        that.selectionText = "";
                    }

                });

                this.$(".logMessage").on('mouseup contextmenu', function(e) {
                    var selection;
                    e.stopPropagation();

                    var range = window.getSelection().getRangeAt(0);
                    var selectionContents = range.cloneContents();
                    selection = selectionContents.textContent;

                    setTimeout(function() {
                        that.selectionCallBack(selection, e)
                    }, 1);
                });
            },
            selectionCallBack: function(selection, e) {
                this.RLogFileTable.currentView.$el.removeHighlight(true);
                if (this.selectionText != selection.toString()) {
                    this.selectionText = selection.toString();
                } else {
                    this.ui.contextMenu.hide();
                    return;
                }
                if (selection.toString() && selection && (!_.isEmpty(selection.toString().trim()))) {
                    this.RLogFileTable.currentView.$el.find(".logMessage").highlight(selection.toString().trim(), true, e.currentTarget);
                    this.ui.contextMenu.show();
                    this.ui.contextMenu.css({
                        'top': e.pageY - 140,
                        'left': e.pageX
                    });
                } else {
                    this.RLogFileTable.currentView.$el.removeHighlight(true);
                    this.ui.contextMenu.hide();
                }
            },
            onDropDownMenuClick: function(e) {
                var $el = $(e.currentTarget),
                    type = $el.data("id");
                if (!_.isEmpty(this.selectionText)) {
                    if (type == "F") {
                        this.ui.find.val(this.selectionText);
                        this.ui.find.trigger("keyup");
                        this.ui.find.focus();
                    }else if(type === "IA" || type === "EA"){
    					this.vent.trigger("toggle:facet",{viewName:((type === "IA") ? "include" : "exclude") +"ServiceColumns",
    						key:Globals.serviceLogsColumns["log_message"],value:"*"+this.selectionText+"*"});
    				}
                    else {
                        //this.vent.trigger("add:include:exclude",{type:type,value:this.selectionText});
                        this.vent.trigger("toggle:facet", { viewName: ((type === "I") ? "include" : "exclude") + "ServiceColumns", key: Globals.serviceLogsColumns["log_message"], value: this.selectionText });
                    }
                    this.ui.contextMenu.hide();
                }
            },
            setHostName: function() {
                this.$("[data-id='hostName']").text(this.params.host_name);
                this.$("[data-id='componentName']").text(this.params.component_name);
            },
            getFindValue: function() {
                return this.ui.find.val();
            },
            findScrollEvent: function() {
                var that = this;
                $(window).scroll(function() {
                    if ($(this).scrollTop() > 300 && that.isIconLock()) {
                        that.$('.advance-find').addClass('fixed');
                    } else {
                        that.$('.advance-find').removeClass('fixed');
                    }
                });
            },
            onClearSearchClick: function() {
                this.ui.find.val('');
                this.ui.find.trigger("keyup");
            },
            onFindKeyPress: function(e) {
                if (e.which == 13) {
                    this.ui.next.trigger("click");
                    return
                }
                if (this.RLogFileTable.currentView) {
                    this.RLogFileTable.currentView.$el.removeHighlight(true);
                    this.RLogFileTable.currentView.$el.removeHighlight();
                    var val = e.currentTarget.value.trim();
                    if (!_.isEmpty(val)) {
                        this.RLogFileTable.currentView.$el.find(".logMessage").highlight(val);
                        this.$highlights = this.$(".highlight");
                        this.counter = 0;
                        //this.scrollToFirstElement();
                    } else {
                        this.resetFindParams();
                    }
                }
            },
            onLockToggle: function(e) {
                var $el = $(e.currentTarget);
                if ($el.find('i').hasClass("fa-lock")) {
                    $el.find('i').removeClass("fa-lock").addClass("fa-unlock")
                } else
                    $el.find('i').removeClass("fa-unlock").addClass("fa-lock")
            },
            isIconLock: function() {
                if (this.ui.lock && this.ui.lock.find)
                    return (this.ui.lock.find("i").hasClass("fa-lock")) ? false : true;
            },
            scrollToFirstElement: function() {
                this.scroll(this.$highlights.first());
            },
            scroll: function($el) {
                $('html, body').animate({
                    scrollTop: $el.offset().top - 200
                }, 100);
            },
            onFindNxt: function(e) {

                var type = $(e.currentTarget).data("id");
                if (!this.searchFlag)
                    this.searchFlag = type;
                else if (this.searchFlag !== type) {
                    this.counter = (type === 'next' ? this.counter + 2 : this.counter - 2);
                    this.searchFlag = type;
                }
                this.counter = Utils.scrollToSearchString(this.$el.find('.highlight'), type, this.counter, 200);
            },
            onFindLast: function(e) {
                this.formDataToFind(1);
                this.ui.pageNotation.hide();
            },
            onFindFirst: function(e) {
                this.formDataToFind(0);
                this.ui.pageNotation.hide();
            },
            onCancelFindClick: function() {
                var that = this;
                that.ui.clearSearch.css({ 'right': 82 + 'px' });
                if (this.findRequest && this.findRequest.abort) {
                    this.findRequest.abort();
                    if (this.findToken) {
                        this.logFileCollection.cancelFindRequest({ token: this.findToken }, {
                            success: function() {},
                            complete: function() {
                                that.logFileCollection.trigger("sync")
                            }
                        });
                    }
                }

            },
            pageNotification: function() {
                var pageCount = this.logFileCollection.state.currentPage,
                    that = this;
                this.ui.clearSearch.css({ 'right': 198 + 'px' });
                this.ui.pageNotation.text("Found on Page: " + (++pageCount)).show();
                clearTimeout(this.findTimer);
                this.findTimer = setTimeout(function() {
                    that.ui.pageNotation.hide();
                    that.ui.clearSearch.css({ 'right': 82 + 'px' });

                }, 15000);
            },
            formDataToFind: function(keywordType) {
                var val = this.getFindValue(),
                    that = this;
                this.findToken = new Date().getTime() + Utils.randomNumber();
                if (!_.isEmpty(val)) {
                    $.extend(this.logFileCollection.queryParams, { find: val, keywordType: keywordType, token: this.findToken });
                    this.ui.first.attr("disabled", true);
                    this.ui.last.attr("disabled", true);
                    that.ui.clearSearch.css({ 'right': 129 + 'px' });
                    this.ui.cancelFind.show();
                    var $el;
                    if (keywordType == 1) {
                        $el = this.ui.last;
                    } else {
                        $el = this.ui.first;
                    }
                    $el.find("i").toggleClass("hidden");
                    this.findRequest = this.logFileCollection.fetch({
                        reset: true,
                        success: function() {
                            that.resetFindParams();
                            that.counter = 0;
                            //that.ui.find.trigger("keyup");
                            that.ui.next.trigger("click");
                            that.pageNotification();
                        },
                        error: function(col, xhr, errorThrown) {
                            that.resetFindParams();
                            if (!!errorThrown.xhr.getAllResponseHeaders()) {
                              //  Utils.notifyInfo({ content: "Keyword '" + val + "' not found in " + (keywordType == 1 ? "next" : "previous") + " page !" });
                                that.ui.clearSearch.css({ 'right': 82 + 'px' });
                            }
                        },
                        complete: function() {
                            if (!that.isClosed) {
                                that.ui.first.attr("disabled", false);
                                that.ui.last.attr("disabled", false);
                                that.ui.cancelFind.hide();
                                $el.find("i").toggleClass("hidden");
                            }

                        }
                    });
                }
            },
            resetFindParams: function() {
                $.extend(this.logFileCollection.queryParams, { find: null, keywordType: null });
            },
            onToggleTableView: function(e) {
                if (e.target.checked) {
                    this.renderTable();
                } else {
                    this.renderTableLikeLogFile();
                }
                this.setupFind();
            },
            reRenderView: function(params) {
                this.fetchCollection({});
            },
            setLatestTimeParams: function() {
                var arr = Utils.dateUtil.getRelativeDateFromString(this.params.dateRangeLabel);
                if (_.isArray(arr)) {
                    this.params.from = arr[0].toJSON();
                    this.params.to = arr[1].toJSON();
                }
            },
            getDropdownQuickMenuHtml: function() {
                return '<div class="dropdown quickMenu">' +
                    '<a class="btn btn-success btn-xs btn-quickMenu" data-toggle="dropdown">' +
                    '<i class="fa fa-ellipsis-v"></i></span></a>' +
                    '<ul class="dropdown-menu dropupright">' +
                    '<li data-id="A_B"><a href="javascript:void(0)">Preview</a></li>' +
                    //"<li data-id='N_T'><a title='Open logs in new tab' data-type='C' data-host='"+model.get("host")+"' data-node='"+model.get("type")+"' data-id='"+model.get("id")+"' href='javascript:void(0)' class=''>Open in New Tab</a></li>" +
                    '</ul>' +
                    '</div>';
            },
            onQuickMenuClick: function(e) {
                var that = this,
                    $el = $(e.currentTarget);
                if ($el.data("id") === "A_B") {
                    var model = this.logFileCollection.get($el.parents("tr").data("id"));
                    require(["views/dialog/DetailLogFileView"], function(view) {
                        that.renderDetailLogFileView(new view({
                            model: model,
                            collection: that.logFileCollection
                        }));
                    });
                }
            },
            renderDetailLogFileView: function(view) {
                var that = this;
                var opts = {
                    title: view.model.get("host") + " -> " + view.model.get("type"),
                    className: "ui-dialog-content ui-widget-content logFile",
                    content: view,
                    viewType: 'logfile',
                    resizable: false,
                    appendTo: this.$el,
                    modal: true,
                    width: 950,
                    height: 572,
                    buttons: [{
                        id: "cancelBtn",
                        text: "Close",
                        "class": "btn btn-default",
                        click: function() {
                            that.onDialogClosed();
                        }
                    }]
                };
                var dialog = that.dialog = new JBDialog(opts).render();
                dialog.open();
                dialog.on("dialog:closing", function() {
                    $('body').css('overflow', 'auto');
                })
                $('body').css('overflow', 'hidden');
            },
            onClose: function() {
                $('body').unbind("mouseup.contextMenuLogFile");
            }
        });
});
