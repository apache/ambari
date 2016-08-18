/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


define(['require',
    'backbone',
    'handlebars',
    'hbs!tmpl/common/Header_tmpl',
    'utils/Utils',
    'moment',
    'utils/Globals',
], function(require, Backbone, Handlebars, Header_tmpl, Utils, moment, Globals) {
    'use strict';

    var Header = Backbone.Marionette.Layout.extend(
        /** @lends Header */
        {
            _viewName: 'Header',

            template: Header_tmpl,


            /** ui selector cache */
            ui: {
                'takeATour': "[data-id='takeATour']",
                'globalFilter': "li[data-id='exclusionList']",
                'globalNotification': '.dropdown .excludeStatus',
                'timeZoneChange': "li[data-id='timeZoneChange']",
                'createFilters' : "[data-id='createFilters']",
                'editParams'  : "a[data-id='editParams']"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events['click ' + this.ui.takeATour] = 'takeATour';
                events['click ' + this.ui.globalFilter] = 'exclusionListClick';
                events['click ' + this.ui.timeZoneChange] = 'timeZoneChangeClick';
                events['click ' + this.ui.createFilters] = 'createFiltersClick';
                events['click ' + this.ui.editParams] = 'editParamsClick';
                return events;
            },

            /**
             * intialize a new Header Layout 
             * @constructs
             */
            initialize: function(options) {

                _.extend(this, _.pick(options, 'collection', 'globalVent'));
                this.collection = new Backbone.Collection();
                this.bottomToTop();
                this.topToBottom();
                this.exclusionObj = {
                    logMessageCollection: this.collection,
                    components: []
                }

                this.bindEvents();
            },

            /** all events binding here */
            bindEvents: function() {
                this.listenTo(this.globalVent,"currentMap:load",function(obj){
                    this.currentTimezone = obj;
                },this);
            },
            /** on render callback */
            onRender: function() {
                this.loadTimeZone();
                this.setNotificationCount(this.exclusionObj.components, this.collection.length);
                var storeTimezone = Utils.localStorage.checkLocalStorage('timezone');
                var zoneName = moment.tz(storeTimezone.value.split(',')[0]).zoneName();

                if (storeTimezone.value.split(',').length) {
                    if (storeTimezone.value.split(',')[1]) {
                        if (storeTimezone.value.split(',')[1] != zoneName) {
                            Utils.localStorage.setLocalStorage('timezone', storeTimezone.value.split(',')[0] + "," + zoneName);
                        }
                    }
                    this.ui.timeZoneChange.find('span').text(moment.tz(storeTimezone.value.split(',')[0]).zoneName());
                }
                this.currentTimezone = storeTimezone;
                this.checkParams();
            },
            onShow : function(){
                this.triggerAutoTourCheck();
            },
            loadTimeZone: function() {


            },
            checkParams : function(){
                if(window.location.search){
                    var url = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
                    if(url.length === 1){
                        var bundleIdCheck = url[0].split('=');
                        (bundleIdCheck[0] ==='bundle_id') ? this.ui.editParams.hide() : this.ui.editParams.show();
                    }else{
                      this.ui.editParams.show();  
                    }
                }
            },
            editParamsClick: function() {
                 var that = this;
                 var newUrl = '',
                     hash,
                     str = '<ul>';
                 var oldUrl = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
                 for (var i = 0; i < oldUrl.length; i++) {

                     hash = oldUrl[i].split('=');
                     if (hash[0] === "bundle_id") {
                            if(_.isEmpty(hash[1])){
                                hash[1] = '';
                            }
                         newUrl = hash[0] + "=" + hash[1];
                     }else{
                       str += '<li>' + hash[0] + "  : " + hash[1] + '</li>'; 
                     }     
                 }
                 str += '</ul>';

                 Utils.bootboxCustomDialogs({
                     'title': ' Are you sure you want to remove these params ?',
                     'msg': str,
                     'callback': function() {
                         var editUrl = window.location.href.substring(0, window.location.href.indexOf('?'));
                         var params = (newUrl.length > 0) ? window.location.search = '?' + newUrl : window.location.search = '';
                         window.location.href = editUrl + params;
                         that.ui.editParams.hide();
                     }
                 });
            },
            takeATour: function() {
            	require(['utils/Tour'],function(Tour){
            		Tour.Start();
            	});

                /*localStorage.clear();
                if (typeof(Storage) !== "undefined") {
                    if (!localStorage.getItem("startIntro")) {
                        localStorage.setItem("startIntro", true);
                        //Intro.Start();
                    }
                } else {
                    // Sorry! No Web Storage support..
                }*/
            },
            createFiltersClick: function(){
                var that = this;
                require(['views/filter/CreateLogfeederFilterView'],function(CreateLogfeederFilter){
                    var view = new CreateLogfeederFilter({});
                    var options = {
                        title: "Log Feeder Log Levels",
                        content: view,
                        viewType: 'Filter',
                        resizable: false,
                        width: 950,
                        height: 550,
                        autoFocus1stElement : false,
                        buttons: [{
                            id: "okBtn",
                            text: "Save",
                            "class": "btn btn-primary defaultBtn",
                            click: function() {
                                that.onCreateFilterSubmit();
                            }
                        }, {
                            id: "cancelBtn",
                            text: "Close",
                            "class": "btn btn-default defaultCancelBtn",
                            click: function() {
                                that.onDialogClosed();
                            }
                        }]
                    };
                    that.createFilterDialog(options);
                    that.onDialogClosed();
                });
            },// Filter Dialogs
            createFilterDialog : function(options){
                 var that = this,
                    opts = _.extend({
                        appendTo: this.$el,
                        modal: true,
                        resizable: false,
                        width: 650,
                        height: 350,
                        beforeClose: function(event, ui) {
                            //that.onDialogClosed();
                        }
                    },options);

                  require(['views/common/JBDialog'], function(JBDialog) {
                    var dialog = that.dialog = new JBDialog(opts).render();
                    if(options.viewType == "Filter"){
                        dialog.on("dialog:open", function(){
                            options.content.trigger("toggle:okBtn",false);
                            // dialog.trigger("toggle:okBtn",false);
                        });
                    }
                    options.content.on("closeDialog",function(){
                    	that.onDialogClosed();
                    });
                    dialog.open();
                });
            },
            onCreateFilterSubmit : function(){
                var content = this.dialog.options.content;
                var that = this;
                content.setValues();
                    content.trigger("toggle:okBtn",false);
                    
                    // this.componentArray  = content.ui.componentSelect2.val().split(',');
                    //this.hostArray = content.ui.hostSelect2.val().split(',');
                    // this.levelArray = content.ui.levelSelect2.val().split(',');

                    //this.filterList = { /*components : this.componentArray,*/hosts : this.hostArray/*, levels : this.levelArray */};

                    content.model.set(content.setValues());

                    content.model.save(content.model.attributes,{
                        success : function(model,response){
                            Utils.notifySuccess({
                                content: "Filter has been saved."
                            });
                        },
                        error : function(model,response){
                            Utils.notifyError({
                                content: "There is some issues on server, Please try again later."
                            });
                        },
                        complete : function(){
                            that.onDialogClosed();
                        }
                    });
            },
            setupDialog: function(options) {
                var that = this,
                    opts = _.extend({
                        appendTo: this.$el,
                        modal: true,
                        resizable: false,
                        width: 650,
                        height: 450,
                        beforeClose: function(event, ui) {
                            that.onDialogClosed();
                        }
                    }, options);

                require(['views/common/JBDialog'], function(JBDialog) {
                    var dialog = that.dialog = new JBDialog(opts).render();
                    if (options.viewType == "timezone") {
                        dialog.on("dialog:open", function() {
                            that.dialog.options.content.$('#timezone-picker').WorldMapGenerator({
                                quickLink: [{
                                    "PST": "PST",
                                    "MST": "MST",
                                    "CST": "CST",
                                    "EST": "EST",
                                    "GMT": "GMT",
                                    "LONDON": "Europe/London",
                                    "IST": "IST"
                                }]
                            });
                        });
                    }
                    dialog.open();
                });
            },
            exclusionListClick: function() {
                var that = this;
                require(['views/dialog/GlobalExclusionCompositeView'], function(GlobalExclusionView) {
                    var view = new GlobalExclusionView({
                        exclusionObj: that.exclusionObj
                    });
                    var opts = {
                        title: "Global Exclusion",
                        content: view,
                        viewType: 'exclusion',
                        resizable: false,
                        width: 650,
                        height: 450,
                        buttons: [{
                            id: "okBtn",
                            text: "Apply",
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
                    }
                    that.setupDialog(opts);
                });

            },
            onDialogSubmitted: function() {
                var content = this.dialog.options.content;
                var componentList = [];
                if (content.ui.select2Input.select2("data") != null) {
                    componentList = content.ui.select2Input.select2("data").map(function(d) {
                        return d.type
                    });
                }

                this.exclusionObj.components = componentList;
                var logMessagesList = [];
                content.$('div[data-id="L"] textarea').map(function(i, element) {
                    if (element.value != "") logMessagesList.push({
                        "message": element.value
                    })

                })
                this.collection.reset(logMessagesList);

                var gMessage = logMessagesList.map(function(e) {
                    return e.message
                })
                this.setNotificationCount(componentList.length, gMessage.length);
                this.globalVent.trigger("globalExclusion:component:message", {
                    gMustNot: (componentList.length != 0) ? (componentList.join()) : (""),
                    gEMessage: (gMessage.length != 0) ? (gMessage.join(Globals.splitToken)) : ("")
                });
            },
            /** closing the movable/resizable popup */
            onDialogClosed: function() {
                if (this.dialog) {
                    this.dialog.close && this.dialog.close();
                    this.dialog.remove && this.dialog.remove();
                    this.dialog = null;
                }
            },
            timeZoneChangeClick: function() {
                var that = this;
                require(['views/dialog/TimeZoneChangeView'], function(TimeZoneChangeView) {
                    var view = new TimeZoneChangeView({currentTime : that.currentTimezone});
                    var opts = {
                        title: "Time Zone",
                        content: view,
                        viewType: 'timezone',
                        resizable: false,
                        width: 650,
                        height: 530,
                        buttons: [{
                            id: "reloadBtn",
                            text: "Reload",
                            "class": "btn btn-primary defaultBtn",
                            click: function() {
                                that.onTimeZoneReload();
                            }
                        }, {
                            id: "reloadNewBtn",
                            text: "Reload in new tab",
                            "class": "btn btn-primary defaultBtn",
                            click: function() {
                                that.onTimeZoneReloadinNewTab();
                            }
                        }, {
                            id: "cancelBtn",
                            text: "Close",
                            "class": "btn btn-default defaultCancelBtn",
                            click: function() {
                                that.onDialogClosed();
                            }
                        }]
                    }
                    that.setupDialog(opts);
                });
            },
            onTimeZoneReloadinNewTab: function() {
                var content = this.dialog.options.content;
                this.onDialogClosed();
                if (content.changedTimeZone) {
                    var obj = Utils.localStorage.checkLocalStorage('timezone');
                    Utils.localStorage.setLocalStorage('timezone', content.selectedtimeZone);
                    //this.ui.timeZoneChange.find('span').text(moment.tz(content.selectedtimeZone).zoneName());
                    this.globalVent.trigger("currentMap:load",obj);
                    window.open(window.location.href);

                }
            },
            onTimeZoneReload: function() {
                var content = this.dialog.options.content;
                if (content.changedTimeZone) {
                    Utils.localStorage.setLocalStorage('timezone', content.selectedtimeZone);
                    //this.ui.timeZoneChange.find('span').text(moment.tz(content.selectedtimeZone).zoneName());
                    window.location.reload();

                } else {
                    this.onDialogClosed();
                }
            },
            setNotificationCount: function(componentList, gMessage) {
                if (componentList > 0 || gMessage > 0) {
                    this.ui.globalNotification.addClass('full')
                } else {
                    this.ui.globalNotification.removeClass('full')
                }

            },
            /** on close */
            onClose: function() {},
            bottomToTop: function() {
                $(window).scroll(function() {
                    var tabSelected = $('[role="tablist"]').find('.active').data()
                        /*if (tabSelected.id == "hierarchy") {*/
                    if ($(this).scrollTop() >= 53) {
                        $('.topLevelFilter').addClass('fixed');
                        if ($('.topLevelFilter').find('.fixedSearchBox .select2-container.select2-dropdown-open').length || $('.topLevelFilter').find('.VS-focus').length || $('.topLevelFilter').find('.advanceSearchActive').length) {
                            $('.topLevelFilter').find('.fixedSearchBox').removeClass('hiddeBox')
                        } else {
                            $('.topLevelFilter').find('.fixedSearchBox').addClass('hiddeBox')
                        }
                        $('.setHeight').css('height', '120px');
                        $('.setHeight_LogFile').css('height', '90px');
                    } else {
                        $('.topLevelFilter').removeClass('fixed');
                        $('.setHeight').css('height', '0px');
                        $('.setHeight_LogFile').css('height', '0px');
                    }
                    /* }*/

                    if ($(this).scrollTop() > 600) {
                        $('.arrowDiv').fadeIn();
                    } else {
                        $('.arrowDiv').fadeOut();
                    }
                });
                $('.bottomToTop').click(function() {
                    $('html, body').animate({
                        scrollTop: 0
                    }, 400);
                });

            },
            topToBottom: function() {
                $('.topToBottom').click(function() {
                    $('html, body').animate({
                        scrollTop: $(document).height()
                    }, 400);
                });
            },
            triggerAutoTourCheck : function(){
                var that = this;
                var storageVal = Utils.localStorage.checkLocalStorage('autoTour');
                if(! storageVal.found){
                    Utils.localStorage.setLocalStorage("autoTour",true);
                    setTimeout(function(){
                        Utils.confirmPopup({
                            'msg':'Do you want to take a Tour with LogSearch App ?',
                            'callback':that.takeATour
                        });
                    },3000);
                }
            }


        });

    return Header;
});
