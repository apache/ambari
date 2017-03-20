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
    'timeline',
    'collections/VEventHistoryList',
    'hbs!tmpl/tabs/EventHistoryLayoutView_tmpl'
], function(require, Backbone, Globals, Utils, moment, Timeline, VEventHistory, EventHistoryLayoutViewTmpl) {
    'use strict';

    return Backbone.Marionette.Layout.extend(
        /** @lends EventHistoryLayoutViewTmpl */
        {
            _viewName: 'EventHistoryLayoutView',

            template: EventHistoryLayoutViewTmpl,


            /** ui selector cache */
            ui: {
                eventHistory: "#timeline",
                saveLink: ".save-link",
                applyLink: ".apply-link"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['change ' + this.ui.viewType] = 'onViewTypeChange';
                events['click ' + this.ui.saveLink] = 'onSaveClick';
                events['click ' + this.ui.applyLink] = 'onEventHistoryLoadClick';

                return events;
            },

            /**
             * intialize a new LogLevelView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'vent', 'globalVent', 'params'));
                this.searchParams = (this.params) ? this.params : {};
                this.collection = new VEventHistory();
                this.collection.totalCount = Globals.EventHistory.totalCount;
                this.dateUtil = Utils.dateUtil;
                this.data = {
                    "timeline": {
                        "headline": "Events",
                        "type": "default",
                        "text": "People say stuff"

                    }
                }
            },
            bindEvents: function() {
                var that = this
                this.listenTo(this.vent, "level:filter type:mustNot type:mustBe search:include:exclude " +
                    "logtime:filter " + Globals.eventName.serviceLogsIncludeColumns + " " + Globals.eventName.serviceLogsExcludeColumns,
                    function(value) {
                        if (this.collection.length >= this.collection.totalCount)
                            return;
                        _.extend(this.searchParams, value);
                        that.eventClicked = true;
                        if(that.time){
                           that.addEventInTimeline(this.searchParams, Object.keys(value)[0]);  
                       }else{
                            that.timeundefined =[this.searchParams, Object.keys(value)[0]]
                       }
                       

                    });
                this.ui.eventHistory.delegate(".vco-timeline", "LOADED", function() {
                    if (that.timeLineLoadedOnce) {
                        /*if (typeof(Storage) !== "undefined") {
                            if (!localStorage.getItem("startIntro")) {
                                localStorage.setItem("startIntro", true);
                                //Intro.Start();
                            }
                        } else {
                            // Sorry! No Web Storage support..
                        }*/
                        that.time = new VMM.Timeline()
                        that.timeLineLoadedOnce = false;
                    }
                    if(that.timeundefined){

                    }

                    if (that.eventClicked) {
                        that.ui.eventHistory.find('.marker#' + (that.markerId) + " h3").click();
                        that.eventClicked = false;
                    }
                    that.$("#loaderEvent").hide();
                    that.$(".loader").hide();

                });
                this.ui.eventHistory.delegate(".marker", "click", function(event) {
                    if (event.target.nodeName === "I") return;
                    var typeToTrigger = $(this).find('h3').text().split(":")

                    if (event.isTrigger === undefined) {
                        that.fireReinitialize($(this).attr('id'))
                    }
                });
                this.ui.eventHistory.delegate(".close", "click", function(event) {
                    var elementId = $(event.currentTarget).attr('id');
                    var p = $(event.currentTarget).parents(".marker")
                    var prevClick = p.prev();
                    //prevClick.find("h3").click();
                    VMM.fireEvent(prevClick.find(".flag"), 'click', {
                        number: 1
                    });
                    that.collection.remove(elementId);
                    var lastStartDate, count = 1;
                    for (var i = 0; i < that.data.timeline.date.length; i++) {
                        if (that.data.timeline.date[i].customOption.id == elementId) {
                            that.data.timeline.date.splice(i, 1);
                            i = i - 1;
                            that.markerId = that.data.timeline.date[that.data.timeline.date.length - 1].customOption.id;
                        } else {
                            if (i == 1) lastStartDate = that.data.timeline.date[i].startDate;
                            if (i > 1) {
                                that.data.timeline.date[i].startDate = moment(lastStartDate.split(',').join("-")).add((count++), 'd').format('YYYY,MM,DD');
                            }
                        }

                    }
                    /* that.markerId = that.data.timeline.date.length+1;*/
                    that.fireReinitialize(prevClick.attr('id'));
                    that.addEventInTimeline();
                    return false;
                })

                this.listenTo(this.collection, "add remove reset", function() {
                    this.$("[data-id='count']").text(this.collection.length);
                    this.$("[data-id='totalCount']").text(this.collection.totalCount);
                }, this);
                /*that.ui.saveLink.on('click', function() {
                    that.saveEventHistory();
                });*/
            },
            fireReinitialize: function(id) {
                this.vent.trigger("reinitialize:filter:tree reinitialize:filter:include:exclude reinitialize:filter:bubbleTable reinitialize:filter:mustNot reinitialize:filter:mustBe reinitialize:filter:level reinitialize:filter:logtime", _.extend({
                    mustNot: null,
                    mustBe: null,
                    iMessage: null,
                    eMessage: null,
                    query: null,
                    includeQuery: null,
                    excludeQuery: null
                }, this.collection.get(id).get('params')));
            },
            initializeData: function() {
                var that = this;
                this.markerId = 1;
                that.eventClicked = true;
                this.data.timeline.date = [{
                    "startDate": moment('2015-01-02').format('YYYY,MM,DD'),
                    "headline": this.getHeadline("level", this.searchParams),
                    "params": _.extend({
                        type: "level"
                    }, this.searchParams),
                    "customOption": {
                        id: this.markerId,
                        close: false
                    }
                }, {
                    "startDate": moment('2015-01-02').add((this.markerId++), 'd').format('YYYY,MM,DD'),
                    "headline": this.getHeadline("from", this.searchParams),
                    /*"headline": "Logtime: " + moment(this.searchParams.from).format(Globals.dateFormat) + " TO " + moment(this.searchParams.to).format(Globals.dateFormat),*/
                    "params": _.extend({
                        type: "from"
                    }, this.searchParams),
                    "customOption": {
                        id: this.markerId,
                        close: false
                    }
                }];
                _.each(this.data.timeline.date, function(values) {
                    that.collection.add(new Backbone.Model({
                        id: values.customOption.id,
                        /* headline: values.headline,*/
                        params: values.params
                    }))
                })


                this.generateTimeline(this.data);
            },

            generateTimeline: function(data) {
                var that = this;
                createStoryJS({
                    type: 'timeline',
                    width: '100%',
                    height: '180',
                    source: data,
                    embed_id: that.ui.eventHistory,
                    debug: true
                });
                this.timeLineLoadedOnce = true
            },
            onRender: function() {
                this.bindEvents()
                this.initializeData();
                //this.fetchEventHistory();
                this.$("#loaderEvent").show();
                this.$(".loader").show();
            },
            addEventInTimeline: function(params, type) {
                if (params) {

                    try {
                        var lastDate = this.data.timeline.date[this.data.timeline.date.length - 1].startDate;
                    } catch (e) {
                        throw "StartDate undefined";
                    }

                    this.data.timeline.date.push({
                        "startDate": moment(lastDate.split(',').join("-")).add(1, 'd').format('YYYY,MM,DD'),
                        "headline": this.getHeadline(type, params),
                        "customOption": {
                            id: ++this.markerId,
                            close: true
                        }
                    })

                    params.type = type;
                    this.collection.add({
                        id: this.markerId,
                        /* headline: getHeadline(),*/
                        params: _.extend({}, params)
                    })

                }
                if (this.time) {
                    this.time.reload(this.data)
                }

                return;


            },
            getHeadline: function(type, params) {
                var excludeInclude = function() {
                    var str = "";
                    if (params.iMessage) {
                        str += "IS:" + Utils.encodeIncludeExcludeStr(params.iMessage);
                    } else
                        str += "IS:";
                    if (params.eMessage) {
                        str += " ES:" + Utils.encodeIncludeExcludeStr(params.eMessage);
                    } else
                        str += " ES:";
                    return str;

                };
                var includeExcludeColumnSearch = function() {
                    var str = "";
                    if (params.includeQuery || params.excludeQuery) {
                        var obj;
                        if (type === "includeQuery") {
                            str += "IColumn:";
                            obj = JSON.parse(params.includeQuery)
                        } else {
                            str += "EColumn:";
                            obj = JSON.parse(params.excludeQuery)
                        }

                        if (_.isArray(obj)) {
                            for (var i = 0; i < obj.length; i++) {
                                var key = _.keys(obj[i])[0];
                                str += "" + key + "=" + obj[i][key];
                                if (!(i == obj.length - 1))
                                    str += ",";
                            }

                        }
                    }
                    return str;
                };
                return (type === "level") ?
                    ("Level: " + params.level) : (type === "from") ?
                    ("Logtime: " + this.dateUtil.getTimeZone(params.from) + " TO " + this.dateUtil.getTimeZone(params.to)) : (type === "mustBe") ?
                    ("IC:" + params.mustBe) : (type === "mustNot") ?
                    ("EC:" + params.mustNot) : ((type === "includeQuery" || type === "excludeQuery") ? (includeExcludeColumnSearch()) : (excludeInclude()))


            },
            onEventHistoryLoadClick: function() {
                var that = this;
                require(['views/dialog/ApplySearchFilterView'], function(ApplySearchFilterView) {
                    if (that.collection.length >= 50) {
                        Utils.alertPopup({
                            msg: "Event History limit has reached, Please clear your history"
                        });
                        return;
                    }
                    var view = new ApplySearchFilterView({
                        collection: new VEventHistory([], {
                            state: {
                                firstPage: 0,
                                pageSize: 10 // have to pass max pageSize value or
                                    // else it will take default pageSize
                            }
                        })
                    })
                    that.setupDialog({
                        title: "Apply Filter",
                        content: view,
                        viewType: 'Save',
                        width: 850,
                        height: 500,
                        buttons: [{
                            id: "cancelBtn",
                            text: "Close",
                            "class": "btn btn-default",
                            click: function() {
                                that.onApplyDialogClosed();
                            }
                        }]
                    });
                });
            },
            onSaveClick: function() {
                var that = this;
                require(['views/dialog/SaveSearchFilterView'], function(SaveSearchFilterView) {
                    var activeFlag = that.$('.timenav').find('.marker:gt(0).active');
                    if (activeFlag.length == 0) {
                        Utils.notifyInfo({
                            content: "Item not selected in event history."
                        });
                        return;
                    } else {
                        var selectedCollectionObject = that.collection.findWhere({
                            "id": parseInt(activeFlag.attr('id'))
                        });
                    }

                    var view = new SaveSearchFilterView({
                        selectedCollectionObject: selectedCollectionObject
                    });

                    that.setupDialog({
                        title: "Save Search Filter",
                        content: view,
                        viewType: 'Save',
                        width: 850,
                        height: 500,
                        buttons: [{
                            id: "okBtn",
                            text: "Save",
                            "class": "btn btn-primary",
                            click: function() {
                                that.onDialogSubmitted();
                            }
                        }, {
                            id: "cancelBtn",
                            text: "Close",
                            "class": "btn btn-default",
                            click: function() {
                                that.onDialogClosed();
                            }
                        }]
                    });
                })
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
            onDialogSubmitted: function() {
                var content = this.dialog.options.content;
                /* if (!content.$('form')[0].reportValidity()) {
                     return;
                 }*/


                    
                if (content.$('form')[0].checkValidity && !content.$('form')[0].checkValidity()) {
                    content.$('form').addClass('has-error');
                    if (content.$('form')[0].reportValidity) {
                        if (!content.$('form')[0].reportValidity()) {
                            return;
                        }
                    }
                    return;
                } else {
                    if(_.isEmpty(content.ui.filterName.val().trim())){
                        if(content.$('form')[0].reportValidity){
                            content.ui.filterName.val('')
                            content.$('form')[0].reportValidity();
                            return;
                        }
                        return;
                    }else{
                        content.$('form').removeClass('has-error');  
                    }
                    
                }

                var timeType = content.$("input[name='radio']:checked").parents("[data-id]").data('id');

                if(timeType === "absolute"){
                    content.selectedCollectionObject.get("params")["dateRangeLabel"] = "Custom Range";
                }

                content.selectedCollectionObject.get("params")["time"] = timeType;
                var postObject = {
                    filtername: content.ui.filterName.val(),
                    rowType: "history",
                    values: JSON.stringify(content.selectedCollectionObject.get('params'))
                }
                this.onDialogClosed();
                this.saveEventHistory(postObject);
            },
            /** closing the movable/resizable popup */
            onApplyDialogClosed: function() {
                var content = this.dialog.options.content;
                if (content.apllyedModel) {
                    var params = JSON.parse(content.apllyedModel.toJSON().values);
                    if (params.time === "relative") {
                        var rangeNew = Utils.dateUtil.getRelativeDateFromString(params.dateRangeLabel);
                        if (_.isArray(rangeNew)) {
                            params.from = rangeNew[0].toJSON();
                            params.to = rangeNew[1].toJSON();
                        }
                    }
                    this.eventClicked = true;
                    this.addEventInTimeline(params, params.type);
                    if (this.collection.last()) {
                        this.fireReinitialize(this.collection.last().get('id'));
                    } else {
                        console.log('Not going to last marker as collection is empty');
                    }

                }

                this.onDialogClosed();


            },
            onDialogClosed: function() {
                if (this.dialog) {
                    this.dialog.close && this.dialog.close();
                    this.dialog.remove && this.dialog.remove();
                    this.dialog = null;
                }
            },
            saveEventHistory: function(postObject) {
                var that = this
                this.$("#loaderEvent").show();
                that.$(".loader").show();
                this.collection.saveEventHistory(postObject, {
                    success: function(data, textStatus, jqXHR) {
                        Utils.notifySuccess({
                            content: "Event History saved successfully."
                        });
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        Utils.notifyError({
                            content: JSON.parse(jqXHR.responseText).msgDesc || "There is some problem in Event History, Please try again later."
                        });
                    },
                    complete: function() {
                        that.$("#loaderEvent").hide();
                        that.$(".loader").hide();
                    }
                });
            }

        });


});
