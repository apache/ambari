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
    'utils/Globals',
    'collections/VAuditLogList',
    'hbs!tmpl/dialog/GridGraphSettingView_tmpl',
    'bootstrap-daterangepicker'
], function(require, Backbone, Utils, Globals, VAuditLogList, GridGraphSettingViewTmpl, daterangepicker) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends GridGraphSettingView */
        {
            _viewName: 'GridGraphSettingView',

            template: GridGraphSettingViewTmpl,
            templateHelpers: function() {
                return {
                    title: this.options.title,
                    showLegend: this.options.showLegend,
                    showY: this.options.showY,
                    showX: this.options.showX,
                    unit: this.options.unit,
                    xTimeFormat: this.options.xTimeFormat,
                    xNormalFormat: this.options.xNormalFormat,
                    yAxisFormat: this.options.yAxisFormat,
                    stackOrGroup: this.options.stackOrGroup,
                    pieOrDonut: this.options.pieOrDonut,
                    viewTypePie: this.pieView,
                    firstTime: this.firstTime
                };
            },


            /** ui selector cache */
            ui: {
                select2XColumns: ".select2XColumns",
                select2XType: ".select2XType",
                select2Y: ".select2Y",
                select2StackColumns: ".select2StackColumns",
                startDate: "#startDate",
                endDate: "#endDate",
                dateRangeTitle: ".dateRangeTitle:not('.custome')",
                input: "input:not('.unbind')",
                select: "select[data-fetch='true']",
                selectXAxis: "[data-id='selectx']",
                column: "[data-id='column']",
                time: "[data-id='time']",
                showXBox: "[data-id='showX']",
                alert: "[data-id='alert']",
                stackOrGroupLov: "[data-id='stackOrGroupLov']"
            },


            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.applyFilter] = 'onApplyClick';
                events['click ' + this.ui.deleteFilter] = 'onDeleteClick';
                events['click ' + this.ui.dateRangeTitle] = 'onRelativeDateClick';
                events['change ' + this.ui.selectXAxis] = 'onSelectXAxis';
                events['change ' + this.ui.input] = 'onInputChanges';
                events['change ' + this.ui.select] = 'onInputChanges';
                return events;
            },

            /**
             * intialize a new GridGraphSettingView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'vent', 'model', 'viewType'));
                if (this.model) {
                    this.options = this.model.toJSON();
                    if (!this.options.params) {
                        this.firstTime = true;
                    }
                }
                this.dateUtil = Utils.dateUtil;

                this.params = {};
                if (!this.viewType || this.viewType == Globals.graphType.HISTOGRAM.value) {
                    this.histogramView = true;
                } else if (this.viewType == Globals.graphType.MULTILINE.value) {
                    this.lineView = true;
                } else if (this.viewType == Globals.graphType.PIE.value) {
                    this.pieView = true;
                }
                this.initializeCollection();
                this.bindEvents();
            },
            bindEvents: function() {


            },
            onRender: function() {
                this.initializePlugins(this.listOfselect2);
                //this.ui.column.hide();
                this.initializeDateRangePicker();
                this.initializePopover();
                if (this.options) {
                    if (!this.options.stackOrGroup && (this.options.stackOrGroup == "" || this.options)) {
                        this.$('.normalRadio').prop("checked", true);
                    }
                    if (this.options.xAxis == "evtTime") {
                        this.$('.xTimeFormat').show();
                        this.$('.xNormalFormat').hide();

                    } else {
                        this.$('.xNormalFormat').show();
                        this.$('.xTimeFormat').hide();
                    }

                }
                this.stackPrvValue = "Normal";
            },
            getDefaultDate: function() {
                var todayDate = this.dateUtil.getRelativeDateFromString('Today');
                return todayDate;
            },
            initializeDateRangePicker: function() {
                var that = this;
                this.ui.startDate.daterangepicker({
                    singleDatePicker: true,
                    showDropdowns: false,
                    timePicker: true,
                    autoApply: true,
                    autoUpdateInput: true,
                    timePicker: true,
                    timePickerIncrement: 1,
                    timePicker24Hour: true,
                    timePickerSeconds: true,
                    timeZone: 0,
                    locale: {
                        format: 'MM/DD/YYYY H:mm:ss,SSS'
                    }
                }, function(start, end, labe) {
                    if (that.relativeDateSet) {
                        that.relativeDateSet = false;
                    }
                    that.$el.find('.dateRangeTitle.custome').addClass('active').siblings().removeClass('active');
                    that.startDate = start;
                    that.onDateChanged();
                });
                this.ui.endDate.daterangepicker({
                    singleDatePicker: true,
                    showDropdowns: true,
                    timePicker: true,
                    autoApply: true,
                    autoUpdateInput: true,
                    timePicker: true,
                    timePickerIncrement: 1,
                    timePicker24Hour: true,
                    timePickerSeconds: true,
                    timeZone: 0,
                    locale: {
                        format: 'MM/DD/YYYY H:mm:ss,SSS'
                    }
                }, function(start, end, labe) {
                    if (that.relativeDateSet) {
                        that.relativeDateSet = false;
                    }
                    that.$el.find('.dateRangeTitle.custome').addClass('active').siblings().removeClass('active');
                    that.endDate = start;
                    that.onDateChanged();
                });
                if (this.options && this.options.params) {
                    this.setDateText(this.dateUtil.getMomentObject(this.options.params.from), this.dateUtil.getMomentObject(this.options.params.to));
                    this.params = this.options.params;
                    if (this.params.dateRangeTitle) {
                        that.$el.find('.dateRangeTitle:contains(' + this.params.dateRangeTitle + ')').addClass('active').siblings().removeClass('active');
                    }
                } else {
                    var getdateObject = this.getDefaultDate();
                    this.setDateText(getdateObject[0], getdateObject[1]);
                    this.params = this.setDateParams();
                }



            },
            onInputChanges: function(e) {
                var data = $(e.currentTarget).data(),
                    key = e.currentTarget.name,
                    dataObject = this.getData(),
                    value = dataObject[key];
                if (key == "stackOrGroup") {
                    this.ui.stackOrGroupLov.find('label').text(value);
                    if (value == "Normal") {
                        this.stackPrvValue = value;
                        this.vent.trigger('graph:data:update', {
                            "stackBy": null
                        }, dataObject);

                        this.ui.select2StackColumns.select2("disable");
                        return;
                    } else {
                        this.ui.select2StackColumns.select2("enable");
                        if (this.stackPrvValue == "Normal" && this.ui.select2StackColumns.val() != "") {
                            this.stackPrvValue = value;
                            this.vent.trigger('graph:data:update', {
                                "stackBy": this.ui.select2StackColumns.val()
                            }, dataObject);

                            return;
                        } else {
                            this.stackPrvValue = value;
                        }

                    }

                }
                if (key == "xAxis") {
                    if (value == "evtTime") {
                        this.$('.xTimeFormat').show();
                        this.$('.xNormalFormat').hide();
                    } else {
                        this.$('.xTimeFormat').hide();
                        this.$('.xNormalFormat').show();
                    }
                }
                var obj = {};
                // For Temp we set y axis explicitly
                if (dataObject.yAxis && dataObject.yAxis != "") {
                    obj['yAxis'] = dataObject['yAxis'];
                }
                if (data.fetch) {
                    obj[key] = value;

                    /*   if (key == "xAxis" && key == "yAxis" && dataObject.xAxis && dataObject.xAxis != "") {
                           if (this.ui.time.is(':hidden')) {
                               this.ui.alert.removeClass('text-danger').addClass(' text-success');
                           } else {
                               this.ui.alert.removeClass('text-success').addClass('text-danger');
                           }
                       } else {
                           this.ui.alert.removeClass('text-success').addClass('text-danger');
                       }*/
                    this.triggerFetch(obj, dataObject);
                } else if (data.grid) {
                    this.vent.trigger('graph:grid:update', dataObject);
                } else {
                    this.vent.trigger('graph:update', dataObject);

                }
            },
            onDateChanged: function(value) {
                var dataObject = this.getData();
                var params = this.setDateParams();
                if (dataObject.xAxis && dataObject.xAxis != "" && dataObject.yAxis && dataObject.yAxis != "") {
                    this.triggerFetch(params, dataObject);
                }

            },
            setDateParams: function() {
                if (this.startDate && this.endDate) {

                    var dateUnit = this.dateUtil.calculateUnit({
                        startDate: this.startDate,
                        endDate: this.endDate
                    });
                    $('input[name="unit"]').val(dateUnit.substr(1));

                    var params = {
                        from: this.startDate.toJSON(),
                        to: this.endDate.toJSON(),
                        unit: dateUnit
                    }
                    return params;
                }
            },
            triggerFetch: function(params, object) {
                this.vent.trigger('graph:data:update', $.extend(this.params, params), object);
            },
            getData: function() {
                return Utils.getFormData(this.$("#CreateLogicForm").serializeArray());
            },
            onRelativeDateClick: function(e) {
                this.relativeDateSet = true;
                this.params['dateRangeTitle'] = $(e.currentTarget).text()
                $(e.currentTarget).addClass('active').siblings().removeClass('active');
                var relativeDate = $(e.currentTarget).text()
                var date = this.dateUtil.getRelativeDateFromString(relativeDate);
                this.setDateText(date[0], date[1], true);
            },
            setDateText: function(start, end, fetch) {
                this.ui.startDate.data('daterangepicker').setStartDate(start);
                this.startDate = start;
                this.ui.startDate.val(this.dateUtil.getTimeZone(start, "MM/DD/YYYY H:mm:ss,SSS"));
                this.ui.endDate.data('daterangepicker').setStartDate(end);
                this.endDate = end;
                if (fetch) {
                    this.ui.endDate.val(this.dateUtil.getTimeZone(end, "MM/DD/YYYY H:mm:ss,SSS"));
                    this.onDateChanged();
                } else {
                    this.ui.endDate.val(this.dateUtil.getTimeZone(end, "MM/DD/YYYY H:mm:ss,SSS"));
                }


            },
            initializePopover: function() {
                var that = this;
                this.$('.timeInfo').popover({
                    html: true,
                    animation: true,
                    container: 'body',
                    placement: "auto",
                    trigger: 'click',
                    content: function() {
                        return ('<ul class="d3TimeDetails">' +
                            '<li>' +
                            '<code>%a</code> - abbreviated weekday name.</li>' +
                            '<li>' +
                            '<code>%A</code> - full weekday name.</li>' +
                            '<li>' +
                            '<code>%b</code> - abbreviated month name.</li>' +
                            '<li>' +
                            '<code>%B</code> - full month name.</li>' +
                            '<li>' +
                            '<code>%c</code> - date and time, as "%a %b %e %H:%M:%S %Y".</li>' +
                            '<li>' +
                            '<code>%d</code> - zero-padded day of the month as a decimal number [01,31].</li>' +
                            '<li>' +
                            '<code>%e</code> - space-padded day of the month as a decimal number [ 1,31]; equivalent to <code>%_d</code>.</li>' +
                            '<li>' +
                            '<code>%H</code> - hour (24-hour clock) as a decimal number [00,23].</li>' +
                            '<li>' +
                            '<code>%I</code> - hour (12-hour clock) as a decimal number [01,12].</li>' +
                            '<li>' +
                            '<code>%j</code> - day of the year as a decimal number [001,366].</li>' +
                            '<li>' +
                            '<code>%m</code> - month as a decimal number [01,12].</li>' +
                            '<li>' +
                            '<code>%M</code> - minute as a decimal number [00,59].</li>' +
                            '<li>' +
                            '<code>%L</code> - milliseconds as a decimal number [000, 999].</li>' +
                            '<li>' +
                            '<code>%p</code> - either AM or PM.</li>' +
                            '<li>' +
                            '<code>%S</code> - second as a decimal number [00,61].</li>' +
                            '<li>' +
                            '<code>%U</code> - week number of the year (Sunday as the first day of the week) as a decimal number [00,53].</li>' +
                            '<li>' +
                            '<code>%w</code> - weekday as a decimal number [0(Sunday),6].</li>' +
                            '<li>' +
                            '<code>%W</code> - week number of the year (Monday as the first day of the week) as a decimal number [00,53].</li>' +
                            '<li>' +
                            '<code>%x</code> - date, as "%m/%d/%Y".</li>' +
                            '<li>' +
                            '<code>%X</code> - time, as "%H:%M:%S".</li>' +
                            '<li>' +
                            '<code>%y</code> - year without century as a decimal number [00,99].</li>' +
                            '<li>' +
                            '<code>%Y</code> - year with century as a decimal number.</li>' +
                            '<li>' +
                            '<code>%Z</code> - time zone offset, such as "-0700".</li>' +
                            '<li>' +
                            '<code>%</code> - a literal "%" character.</li>' +
                            '</ul>');
                    }
                });
                this.$el.find('.Header span[data-id="gridSettingPopup"]').on('shown.bs.popover', function(e) {
                    //$(this).addClass("gridPopover")
                });

            },
            initializeCollection: function() {
                var that = this;
                this.auditLogList = new VAuditLogList([], {
                    state: {
                        firstPage: 0,
                        pageSize: 50
                    }
                });

                var getAuditSchemaFieldsName = function(el) {
                    that.auditLogList.getAuditSchemaFieldsName({}, {
                        beforeSend: function() {
                            that.$("#loaderAudit").show();
                        },
                        success: function(data) {
                            var myData = [];
                            _.each(data, function(a, b) {
                                myData.push({
                                    id: b,
                                    text: a
                                })
                            })
                            el.select2({
                                data: myData
                            });
                            if (el[0].name == "xAxis") {
                                el.select2("enable");
                            }
                            if (el[0].name == "stackBy" && (that.options.stackOrGroup == "Group" || that.options.stackOrGroup == "Stack")) {
                                el.select2("enable");
                            }
                            if (that.model) {
                                if (that.model.get('xAxis') && el[0].name == "xAxis") {
                                    that.ui.select2XColumns.select2('val', that.model.get('xAxis'))
                                }
                                if (that.model.get('stackBy') && el[0].name == "stackBy") {
                                    that.ui.select2StackColumns.select2('val', that.model.get('stackBy'))
                                }

                            }
                            //that.collection.reset(new Backbone.Model(myData));
                        },
                        error: function(error, data, status) {
                            var obj = JSON.parse(error.responseText);
                            if (obj)
                                Utils.notifyError({
                                    content: obj.msgDesc
                                });
                        },
                        complete: function() {
                            that.$("#loaderAudit").hide();
                        }
                    });

                }

                this.listOfselect2 = [{
                    id: "select2XType",
                    placeholder: "Select X axis",
                    mandatory: false,
                    attachSelect: true
                }, {
                    id: "select2Y",
                    placeholder: "select Y",
                    mandatory: false,
                    attachSelect: true
                }, {
                    id: "select2XColumns",
                    placeholder: "select Columns",
                    collection: this.auditLogList, //pass string or object reference
                    mandatory: false,
                    nonCrud: getAuditSchemaFieldsName, // pass function
                    fetch: true
                }]
                if (!this.pieView) {
                    this.listOfselect2.push({
                        id: "select2StackColumns",
                        placeholder: "select Columns",
                        collection: this.auditLogList, //pass string or object reference
                        mandatory: false,
                        nonCrud: getAuditSchemaFieldsName, // pass function
                        fetch: true
                    })
                }
            },
            initializePlugins: function(listOfselect2) {
                Utils.genrateSelect2(listOfselect2, this);
            }


        })
});
