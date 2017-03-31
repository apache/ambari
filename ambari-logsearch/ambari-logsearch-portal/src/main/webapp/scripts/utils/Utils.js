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
    'utils/Enums',
    'utils/LangSupport',
    'moment',
    'utils/Globals',
    'bootbox'
],function(require,XAEnums,localization,moment,Globals,bootbox) {
    'use strict';


    var prevNetworkErrorTime = 0;
    var Utils = {};

    require(['noty'],function(){
        $.extend($.noty.defaults,{
            timeout : 5000,
            layout : "topRight",
            theme : "relax",
            closeWith: ['click','button'],
             animation   : {
                 open  : 'animated flipInX',
                 close : 'animated flipOutX',
                 easing: 'swing',
                 speed : 500
             }
            
        });
    });

    // ///////////////////////////////////////////////////////
    // Enum utility methods
    // //////////////////////////////////////////////////////
    /**Utils
     * Get enum for the enumId
     * 
     * @param {integer}
     *            enumId - The enumId
     */
    Utils.getEnum = function(enumId) {
        if (!enumId || enumId.length < 1) {
            return "";
        }
        // check if the enums are loaded
        if (!XAEnums[enumId]) {
            return "";
        }
        return XAEnums[enumId];
    };

    /**
     * Get enum by Enum and value
     * 
     * @param {Object}
     *            myEnum - The enum
     * @param {integer}
     *            value - The value
     */
    Utils.enumElementByValue = function(myEnum, value) {
        var element = _.detect(myEnum, function(element) {
            return element.value == value;
        });
        return element;
    };

    /**
     * Get enum by Enum and name, value
     * 
     * @param {Object}
     *            myEnum - The enum
     * @param {string}
     *            propertyName - The name of key
     * @param {integer}
     *            propertyValue - The value
     */
    Utils.enumElementByPropertyNameValue = function(myEnum, propertyName,
            propertyValue) {
        for ( var element in myEnum) {
            if (myEnum[element][propertyName] == propertyValue) {
                return myEnum[element];
            }
        }
        return null;
    };

    /**
     * Get enum value for given enum label
     * 
     * @param {Object}
     *            myEnum - The enum
     * @param {string}
     *            label - The label to search for in the Enum
     */
    Utils.enumLabelToValue = function(myEnum, label) {
        var element = _.detect(myEnum, function(element) {
            return element.label == label;
        });
        return (typeof element === "undefined") ? "--" : element.value;
    };

    /**
     * Get enum label for given enum value
     * 
     * @param {Object}
     *            myEnum - The enum
     * @param {integer}
     *            value - The value
     */
    Utils.enumValueToLabel = function(myEnum, value) {
        var element = _.detect(myEnum, function(element) {
            return element.value == value;
        });
        return (typeof element === "undefined") ? "--" : element.label;
    };

    /**
     * Get enum label tt string for given Enum value
     * 
     * @param {Object}
     *            myEnum - The enum
     * @param {integer}
     *            value - The value
     */
    Utils.enumValueToLabeltt = function(myEnum, value) {
        var element = _.detect(myEnum, function(element) {
            return element.value == value;
        });
        return (typeof element === "undefined") ? "--" : element.tt;
    };

    /**
     * Get NVpairs for given Enum to be used in Select
     * 
     * @param {Object}
     *            myEnum - The enum
     */
    Utils.enumToSelectPairs = function(myEnum) {
        return _.map(myEnum, function(o) {
            return {
                val : o.value,
                label : o.label
            };
        });
    };

    /**
     * Get NVpairs for given Enum
     * 
     * @param {Object}
     *            myEnum - The enum
     */
    Utils.enumNVPairs = function(myEnum) {
        var nvPairs = {
            ' ' : '--Select--'
        };

        for ( var name in myEnum) {
            nvPairs[myEnum[name].value] = myEnum[name].label;
        }

        return nvPairs;
    };

    /**
     * Get array NV pairs for given Array
     * 
     * @param {Array}
     *            myArray - The eArraynum
     */
    Utils.arrayNVPairs = function(myArray) {
        var nvPairs = {
            ' ' : '--Select--'
        };
        _.each(myArray, function(val) {
            nvPairs[val] = val;
        });
        return nvPairs;
    };

    Utils.notifyInfo = function(options) {
        noty({
            type:"information",
            text : "<i class='fa fa-exclamation-circle'></i> "+(options.content || "Info message.")
        });
    };
    Utils.notifyWarn = function(options) {
        noty({
            type:"warning",
            text : "<i class='fa fa-times-circle'></i> "+(options.content || "Info message.")
        });
    };

    Utils.notifyError = function(options) {
        noty({
            type:"error",
            text : "<i class='fa fa-times-circle'></i> "+(options.content || "Error occurred.")
        });
    };

    Utils.notifySuccess = function(options) {
        noty({
            type:"success",
            text : "<i class='fa fa-check-circle-o'></i> "+(options.content || "Error occurred.")
        });
    };

    /**
     * Convert new line to <br />
     * 
     * @param {string}
     *            str - the string to convert
     */
    Utils.nl2br = function(str) {
        if (!str)
            return '';
        return str.replace(/\n/g, '<br/>').replace(/[\r\t]/g, " ");
    };

    /**
     * Convert <br />
     * to new line
     * 
     * @param {string}
     *            str - the string to convert
     */
    Utils.br2nl = function(str) {
        if (!str)
            return '';
        return str.replace(/\<br(\s*\/|)\>/gi, '\n');
    };

    /**
     * Escape html chars
     * 
     * @param {string}
     *            str - the html string to escape
     */
    Utils.escapeHtmlChar = function(str) {
        if (!str)
            return '';
        str = str.replace(/&/g, "&amp;");
        str = str.replace(/>/g, "&gt;");
        str = str.replace(/</g, "&lt;");
        str = str.replace(/\"/g, "&quot;");
        str = str.replace(/'/g, "&#039;");
        return str;
    };

    /**
     * nl2br and Escape html chars
     * 
     * @param {string}
     *            str - the html string
     */
    Utils.nl2brAndEscapeHtmlChar = function(str) {

        if (!str)
            return '';
        var escapedStr = escapeHtmlChar(str);
        var finalStr = nl2br(str);
        return finalStr;
    };

    /**
     * prevent navigation with msg and call callback
     * 
     * @param {String}
     *            msg - The msg to show
     * @param {function}
     *            callback - The callback to call
     */
    Utils.preventNavigation = function(msg, $form) {
        window._preventNavigation = true;
        window._preventNavigationMsg = msg;
        $("body a, i[class^='icon-']").on("click.blockNavigation", function(e) {
            Utils.preventNavigationHandler.call(this, e, msg, $form);
        });
    };

    /**
     * remove the block of preventNavigation
     */
    Utils.allowNavigation = function() {
        window._preventNavigation = false;
        window._preventNavigationMsg = undefined;
        $("body a, i[class^='icon-']").off('click.blockNavigation');
    };

    Utils.preventNavigationHandler = function(e, msg, $form) {
        var formChanged = false;
        var target = this;
        if (!_.isUndefined($form))
            formChanged = $form.find('.dirtyField').length > 0 ? true : false;
        if (!$(e.currentTarget).hasClass("_allowNav") && formChanged) {

            e.preventDefault();
            e.stopImmediatePropagation();
            bootbox.dialog(msg, [ {
                "label" : localization.tt('btn.stayOnPage'),
                "class" : "btn-success btn-small",
                "callback" : function() {
                }
            }, {
                "label" : localization.tt('btn.leavePage'),
                "class" : "btn-danger btn-small",
                "callback" : function() {
                    Utils.allowNavigation();
                    target.click();
                }
            } ]);
            return false;
        }
    };

    /**
     * Bootbox wrapper for alert
     * 
     * @param {Object}
     *            params - The params
     */
    Utils.alertPopup = function(params) {
        var obj = _.extend({
                message : params.msg
        },params);
        bootbox.alert(obj);
    };

    /**
     * Bootbox wrapper for confirm
     * 
     * @param {Object}
     *            params - The params
     */
    Utils.confirmPopup = function(params) {
        bootbox.confirm(params.msg, function(result) {
            if (result) {
                params.callback();
            }
        });
    };

    Utils.bootboxCustomDialogs = function(params) {
        bootbox.dialog({
            title: params.title,
            message: params.msg,
            buttons: {
                cancel: {
                    label: "Cancel",
                    className: "btn-secondary"
                },
                Ok: {
                    className: "btn-primary",
                    callback: params.callback
                }
            }
        });
    };

    Utils.filterResultByIds = function(results, selectedVals) {
        return _.filter(results, function(obj) {
            if ($.inArray(obj.id, selectedVals) < 0)
                return obj;

        });
    };
    Utils.filterResultByText = function(results, selectedVals) {
        return _.filter(results, function(obj) {
            if ($.inArray(obj.text, selectedVals) < 0)
                return obj;

        });
    };
    Utils.scrollToField = function(field) {
        $("html, body").animate({
            scrollTop : field.position().top - 80
        }, 1100, function() {
            field.focus();
        });
    };
    Utils.blockUI = function(options) {
        var Opt = {
            autoUnblock : false,
            clickUnblock : false,
            bgPath : 'images/',
            content : '<img src="images/blockLoading.gif" > Please wait..',
            css : {}
        };
        options = _.isUndefined(options) ? Opt : options;
        $.msg(options);
    };
    var errorShown = false;
    Utils.defaultErrorHandler = function(model, error) {
        if (error.status == 500) {
            try {
                if (!errorShown) {
                    errorShown = true;
                    var errorMessage = "Some issues on server, Please try again later."
                    if (error != null && error.responseText != null) {
                      var errorObj = JSON.parse(error.responseText);
                      if (errorObj.hasOwnProperty('msgDesc')) {
                        errorMessage = errorObj.msgDesc;
                      }
                    }
                    Utils.notifyError({
                      content: errorMessage
                    });
                    setTimeout(function() {
                        errorShown = false;
                    }, 3000);
                }
            } catch (e) {}
        }
        else if (error.status == 400) {
            try {
                if (!errorShown) {
                    errorShown = true;
                    Utils.notifyError({
                        content: JSON.parse(error.responseText).msgDesc
                    });
                    setTimeout(function() {
                        errorShown = false;
                    }, 3000);
                }
            } catch (e) {}
        } else if (error.status == 401) {
            window.location = 'login.html' + window.location.search;
            // App.rContent.show(new vError({
            //     status : error.status
            // }));

        } else if (error.status == 419) {
            window.location = 'login.html' + window.location.search;

        } else if (error.status == "0") {
            var diffTime = (new Date().getTime() - prevNetworkErrorTime);
            if (diffTime > 3000) {
                prevNetworkErrorTime = new Date().getTime();
                if(error.statusText === "abort"){
                    Utils.notifyInfo({ content: "You have canceled the request"});
                }else{
                     Utils.notifyError({
                        content: "Network Connection Failure : " +
                            "It seems you are not connected to the internet. Please check your internet connection and try again"
                    });
                }
            }
        }
        // require(['views/common/ErrorView','App'],function(vError,App){

        // });
    };
    Utils.select2Focus = function(event) {
        if (/^select2-focus/.test(event.type)) {
            $(this).select2('open');
        }
    };

    Utils.checkDirtyField = function(arg1, arg2, $elem) {
        if (_.isEqual(arg1, arg2)) {
            $elem.removeClass('dirtyField');
        } else {
            $elem.addClass('dirtyField');
        }
    };
    Utils.checkDirtyFieldForToggle = function($el) {
        if ($el.hasClass('dirtyField')) {
            $el.removeClass('dirtyField');
        } else {
            $el.addClass('dirtyField');
        }
    };
    Utils.checkDirtyFieldForSelect2 = function($el, dirtyFieldValue, that) {
        if ($el.hasClass('dirtyField')
                && _.isEqual($el.val(), dirtyFieldValue.toString())) {
            $el.removeClass('dirtyField');
        } else if (!$el.hasClass('dirtyField')) {
            $el.addClass('dirtyField');
            dirtyFieldValue = !_.isUndefined(that.value.values) ? that.value.values
                    : '';
        }
        return dirtyFieldValue;
    };
    Utils.enumToSelectLabelValuePairs = function(myEnum) {
        return _.map(myEnum, function(o) {
            return {
                label : o.label,
                value : o.value + ''
            // category :'DHSS',
            };
        });
    };
    Utils.hackForVSLabelValuePairs = function(myEnum) {
        return _.map(myEnum, function(o) {
            return {
                label : o.label,
                value : o.label + ''
            // category :'DHSS',
            };
        });
    };
    Utils.addVisualSearch = function(searchOpt, serverAttrName, collection,
            pluginAttr) {
        var visualSearch;
        var search = function(searchCollection, serverAttrName, searchOpt,
                collection) {
            var params = {};
            searchCollection.each(function(m) {
                var serverParamName = _.findWhere(serverAttrName, {
                    text : m.attributes.category
                });
                var extraParam = {};
                if (_.has(serverParamName, 'multiple')
                        && serverParamName.multiple) {
                    extraParam[serverParamName.label] = Utils
                            .enumLabelToValue(serverParamName.optionsArr, m
                                    .get('value'));
                    ;
                    $.extend(params, extraParam);
                } else {
                    if (!_.isUndefined(serverParamName)) {
                        extraParam[serverParamName.label] = m.get('value');
                        $.extend(params, extraParam);
                    }
                }
            });
            collection.queryParams = $.extend(collection.queryParams, params);
            collection.state.currentPage = collection.state.firstPage;
            collection.fetch({
                reset : true,
                cache : false
            // data : params,
            });
        };
        // var searchOpt = ['Event Time','User','Resource Name','Resource
        // ID','Resource Type','Repository Name','Repository
        // Type','Result','Client IP','Client Type','Access Type','Access
        // Enforcer','Audit Type','Session ID'];

        var callbackCommon = {
            search : function(query, searchCollection) {
                collection.VSQuery = query;
                search(searchCollection, serverAttrName, searchOpt, collection);
            },
            clearSearch : function(callback) {
                _.each(serverAttrName, function(attr) {
                    delete collection.queryParams[attr.label];
                });
                callback();
            },
            facetMatches : function(callback) {
                // console.log(visualSearch);
                var searchOptTemp = $.extend(true, [], searchOpt);
                visualSearch.searchQuery.each(function(m) {
                    if ($.inArray(m.get('category'), searchOptTemp) >= 0) {
                        searchOptTemp.splice($.inArray(m.get('category'),
                                searchOptTemp), 1);
                    }
                });
                // visualSearch.options.readOnly = searchOptTemp.length <= 0 ?
                // true : false;
                callback(searchOptTemp, {
                    preserveOrder : false
                });
            },
            removedFacet : function(removedFacet, searchCollection, indexObj) {
                // console.log(removedFacet);

                var removedFacetSeverName = _.findWhere(serverAttrName, {
                    text : removedFacet.get('category')
                });
                if (!_.isUndefined(removedFacetSeverName)) {
                    delete collection.queryParams[removedFacetSeverName.label];
                    collection.state.currentPage = collection.state.firstPage;
                    collection.fetch({
                        reset : true,
                        cache : false
                    });
                }
                // TODO Added for Demo to remove datapicker popups
                if (!_.isUndefined(visualSearch.searchBox.$el))
                    visualSearch.searchBox.$el.parents('body').find(
                            '.datepicker').remove();
            }
        // we can also add focus, blur events callback here..
        };
        pluginAttr.callbacks = $.extend(callbackCommon, pluginAttr.callbacks);
        // Initializing VisualSearch Plugin....
        visualSearch = VS.init($.extend(pluginAttr, {
            remainder : false
        }));

        if (visualSearch.searchQuery.length > 0) // For On Load Visual Search
            search(visualSearch.searchQuery, serverAttrName, searchOpt,
                    collection);

        return visualSearch;
    };

    Utils.displayDatepicker = function($el, facet, $date, callback) {
        var input = $el
                .find('.search_facet.is_editing input.search_facet_input');
        $el.parents('body').find('.datepicker').hide();
        input.datepicker({
            autoclose : true,
            dateFormat : 'yy-mm-dd'
        }).on('changeDate', function(ev) {
            callback(ev.date);
            input.datepicker("hide");
            var e = jQuery.Event("keydown");
            e.which = 13; // Enter
            $(this).trigger(e);
        });
        if (!_.isUndefined($date)) {
            if (facet == 'Start Date') {
                input.datepicker('setEndDate', $date);
            } else {
                input.datepicker('setStartDate', $date);
            }
        }
        input.datepicker('show');
        input.on('blur', function(e) {
            input.datepicker("hide");
            // $('.datepicker').remove();

        });
        // input.attr("readonly", "readonly");
        input.on('keydown', function(e) {
            if (e.which == 9 && e.shiftKey) {
                input.datepicker('setValue', new Date());
                input.trigger('change');
                input.datepicker("hide");
            }
            if (e.which == 13) {
                var e1 = jQuery.Event("keypress");
                e1.which = 13; // Enter
                $(this).trigger(e1);

            }
        });
        return input;
    };
    
    Utils.capitaliseFirstLetter = function(string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    };
    Utils.lowerCaseFirstLetter = function(string) {
        return string.charAt(0).toLowerCase() + string.slice(1);
    };
    Utils.toUpperCase = function(string) {
        return (""+string).toUpperCase();
    };
    
    Utils.bindDraggableEvent = function($el){
        //
        //  Function maked all .box selector is draggable, to disable for concrete element add class .no-drop
        //
        $el
        .draggable({
            revert: true,
            zIndex: 2000,
            cursor: "crosshair",
            handle: '.box-name',
            opacity: 0.8
        })
        .droppable({
            tolerance: 'pointer',
            drop: function( event, ui ) {
                var draggable = ui.draggable;
                var droppable = $(this);
                var dragPos = draggable.position();
                var dropPos = droppable.position();
                draggable.swap(droppable);
                setTimeout(function() {
                    var dropmap = droppable.find('[id^=map-]');
                    var dragmap = draggable.find('[id^=map-]');
                    if (dragmap.length > 0 || dropmap.length > 0){
                        dragmap.resize();
                        dropmap.resize();
                    }
                    else {
                        draggable.resize();
                        droppable.resize();
                    }
                }, 50);
                setTimeout(function() {
                    draggable.find('[id^=map-]').resize();
                    droppable.find('[id^=map-]').resize();
                }, 250);
            }
        });
    };
    
    Utils.scrollToSearchString = function(results, type, counter, adjPx,$el){
        if(results.length > 0){
            if(type === 'next'){
                if(counter > 0 && results.length == counter){
                    counter = 0;
                }
            } else if (type === 'prev') {
                if(counter < 0){
                    counter = results.length - 1;
                } else if(counter === results.length){
                    counter = counter - 2;
                }
            }
            results.removeClass("active");
            $(results[counter]).addClass("active");
            if(_.isUndefined($el)){
                $('html,body').animate({
                    scrollTop: $(results[counter]).offset().top  - adjPx
                }, 100);
            }else{
                $el.animate({
                    scrollTop: $(results[counter]).offset().top  - adjPx
                }, 100);
            }
            
            counter = (type === 'prev') ? counter - 1 : counter + 1;
        }
        return counter;
    };
    //date should be javascript date
    Utils.convertDateToUTC  = function (date) {
        return new Date(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate(), date.getUTCHours(), date.getUTCMinutes(), date.getUTCSeconds());
    }
    Utils.randomNumber = function(){
        var date = new Date();
        var id = new Number();
        return ((Math.random()*date.getTime())/2).toFixed(0)
    }
    /**
     * GET data from form
     * @param {object} title -serializeArray
     */
    Utils.getFormData = function(serializeArray) {
        var formJson = {};
        _.each(serializeArray, function(fValues) {
            formJson[fValues.name] = fValues.value;

        });
        return formJson;
    };
    Utils.getQueryParams = function(qs) {
        qs = qs.split('+').join(' ');

        var params = {},
            tokens,
            re = /[?&]?([^=]+)=([^&]*)/g;
        try {
            while (tokens = re.exec(qs)) {
                params[decodeURIComponent(tokens[1])] = decodeURIComponent(tokens[2]);
            }
        } catch (exception) {
            console.error(exception);
            Utils.notifyError({
                content: exception
            });
        }
        return params;
    };
    /**
     * [converDate description]
     * @param  {[type]} option [pass String or momnet object]
     * @return {[type]}        [it is string]
     */
    Utils.dateUtil = new function() {
        var that = this;

        this.getTimeZone = function(string, format) {
            return moment(string).format((format || Globals.dateFormat));
        };
        this.getJSON = function(string) {
            return moment(string).toJSON();
        };
        this.getMomentUTC = function(string) {
            return moment.utc(string)
        };
        this.getMomentObject = function(string) {
            return moment(string)
        }
        this.getLocalTimeZoneDateObject = function(date,offset) {
            return new Date(date.setMinutes(-(date.getTimezoneOffset() + (offset))))
        }
        this.getTimeZoneDateObject = function(string) {
            return new Date(string)
        }
        /**
         * [getTimeZoneMomentDateObject it will return ]
         * @param  {[type]} option [require moment tz object]
         * @return {[type]}        [description]
         */
        this.getTimeZoneFromMomentObject = function(momentO) {
            if(momentO.isValid()){
                var date = momentO.format('MM/DD/YYYY,HH:mm:ss.SSS').split(',');
                var dateObjectWithMilisecond ="";
                if(date[0] && date[1]){
                    var milliseconds = date[1].split('.');
                    if(milliseconds[0] && milliseconds[1] ){
                        dateObjectWithMilisecond =  new Date( date[0] +" " +milliseconds[0]);
                        dateObjectWithMilisecond.setMilliseconds(milliseconds[1]);
                    }else{
                        dateObjectWithMilisecond =  new Date(date[0]);
                    }
                    return dateObjectWithMilisecond;
                }
            }else{
                this.getLocalTimeZoneDateObject( ((momentO.toDate())?(momentO.toDate()):(new Date(momentO))));
            }
        }
        this.getTimeDiff = function(option) {
            // If You have time more then 24 hours so moment returns 0 for HH:MM:SS so using this 3 line we get perfect time gap
            var self = this;
            var ms = moment(option[1], "DD/MM/YYYY HH:mm:ss").diff(moment(option[0], "DD/MM/YYYY HH:mm:ss"));
            var d = moment.duration(ms);
            var s = Math.floor(d.asHours()) + that.getMomentUTC(ms).format(":mm:ss");
            this.splitedValue = s.split(':');

            this.getHourDiff = function() {
                return parseInt(self.splitedValue[0]);
            };
            this.getMinuteDiff = function() {
                return parseInt(self.splitedValue[1]);
            };
            this.getSecondDiff = function() {
                return parseInt(self.splitedValue[2]);
            };
        }
        this.setTimeZone =function(zone){
            moment.tz.setDefault(zone)
        }
        this.getRelativeDateString =function(){}
        this.getLast1HourRange = function() {
            var m = moment()
            return [moment().hour(m.hours() - 1).minute(m.minutes()).seconds(m.seconds()).milliseconds(m.milliseconds() + 1), moment().hour(m.hours()).minute(m.minutes()).seconds(m.seconds()).milliseconds(m.milliseconds())];
        }
        this.getLast24HourRange = function() {
          var m = moment()
          return [moment().hour(m.hours() - 24).minute(m.minutes()).seconds(m.seconds()).milliseconds(m.milliseconds() + 1), moment().hour(m.hours()).minute(m.minutes()).seconds(m.seconds()).milliseconds(m.milliseconds())];
      }
        this.getTodayRange = function() {
            return [moment().hour('0').minute('0').seconds('0').milliseconds("000"), moment().hour('23').minute('59').seconds('59').milliseconds("999")];
        }
        this.getYesterdayRange = function() {
            return [moment().subtract(1, 'days').hour('0').minute('0').seconds('0').milliseconds("000"), moment().subtract(1, 'days').hour('23').minute('59').seconds('59').milliseconds("999")];
        }
        this.getLast7DaysRange = function() {
            return [moment().subtract(6, 'days').hour('0').minute('0').seconds('0').milliseconds("000"), moment().hour('23').minute('59').seconds('59').milliseconds("999")];
        }
        this.getLast30DaysRange = function() {
            return [moment().subtract(29, 'days').hour('0').minute('0').seconds('0').milliseconds("000"), moment().hour('23').minute('59').seconds('59').milliseconds("999")];
        }
        this.getThisMonthRange = function() {
            return [moment().startOf('month').hour('0').minute('0').seconds('0').milliseconds("000"), moment().endOf('month').hour('23').minute('59').seconds('59').milliseconds("999")];
        }
        this.getLastMonthRange = function() {
            return [moment().subtract(1, 'month').startOf('month').hour('0').minute('0').seconds('0').milliseconds("000"), moment().subtract(1, 'month').endOf('month').hour('23').minute('59').seconds('59').milliseconds("999")];
        }
        this.getOneDayTimeDiff = function(checkTime) {
            var hourDiff = checkTime.getHourDiff();
            var seconDiff = checkTime.getSecondDiff();
            var minuteDiff = checkTime.getMinuteDiff();
            if (hourDiff <= 2) {

                if (hourDiff == 0) {
                    if (minuteDiff == 0) {
                        if (seconDiff == 0) {
                            return "+100MILLISECOND";
                        } else {
                            if (seconDiff > 30) {
                                return "+2SECOND";
                            } else if (seconDiff < 30 && seconDiff > 1) {
                                return "+500MILLISECOND";
                            } else {
                                return "+100MILLISECOND";
                            }
                        }

                    } else {
                        if (minuteDiff > 30) {
                            return "+2MINUTE";
                        } else if (minuteDiff < 30 || minuteDiff > 1) {
                            return "+1MINUTE";
                        }
                    }

                } else {
                    if (hourDiff == 1) {
                        return "+2MINUTE";
                    } else if (hourDiff == 2) {
                        return "+5MINUTE";
                    }
                }
            } else if (hourDiff <= 6) {
                return "+5MINUTE";
            } else if (hourDiff <= 10) {
                return "+10MINUTE";
            } else {
                return "+1HOUR";
            }
        }
        this.getMonthDiff = function(startDate, endDate, dayGap, checkTime) {
            var dayDiff = (moment(endDate).diff(startDate, 'days'));
            if (dayDiff <= dayGap) {
                if (dayDiff == 0) {
                    return this.getOneDayTimeDiff(checkTime)
                } else {
                    return "+" + (moment(endDate).diff(startDate, 'days')) + "HOUR"
                }
            } else {
                return "+1DAY"
            }
        }
        this.calculateUnit =function(picker){
                var dayGap = 10,
                startDate = new Date(picker.startDate.format('MM/DD/YYYY')),
                endDate = new Date(picker.endDate.format('MM/DD/YYYY')),
                now = new Date(moment().format('MM/DD/YYYY'));

            var checkTime = new that.getTimeDiff([picker.startDate.format('MM/DD/YYYY HH:mm:ss'), picker.endDate.format('MM/DD/YYYY HH:mm:ss')]);

            if ((moment(startDate).isSame(endDate)) && (moment(startDate).isSame(now))) {
                //console.log("today")
                return that.getOneDayTimeDiff(checkTime);
            } else if ((moment(startDate).isSame(endDate)) && (moment(startDate).isBefore(now))) {
                //console.log("yesterday")
                return that.getOneDayTimeDiff(checkTime);

            } else if ((moment(startDate).isBefore(now)) || (moment(now).diff(startDate, 'days'))) {
                if ((moment(now).diff(startDate, 'days')) === 6) {
                    //console.log("last 7 days");
                    return "+8HOUR";
                } else if ((moment(now).diff(startDate, 'days') === 29) || (moment(now).diff(startDate, 'days') === 28) || (moment(now).diff(startDate, 'days') === 30)) {
                    //console.log("Last 30 days");
                    return that.getMonthDiff(startDate, endDate, dayGap, checkTime);
                } else if ((moment(now).diff(startDate, 'month') === 1) && (moment(now).diff(startDate, 'days') > 30) && (moment(startDate).isSame(endDate, 'month'))) {
                    //console.log("Last Month");
                    return that.getMonthDiff(startDate, endDate, dayGap, checkTime);
                } else if ((moment(startDate).isSame(endDate, 'month')) && ((moment(now).diff(startDate, 'days') === 29) || (moment(now).diff(startDate, 'days') === 30) || (moment(now).diff(startDate, 'days') === 28))) {
                    //console.log("this Month");
                    return that.getMonthDiff(startDate, endDate, dayGap, checkTime);
                } else if ((moment(endDate).diff(startDate, 'days') >= 28) && (moment(endDate).diff(startDate, 'days') <= 30)) {
                    //console.log("Last 30 days");
                    return that.getMonthDiff(startDate, endDate, dayGap, checkTime);
                } else if ((moment(endDate).diff(startDate, 'month') > 3)) {
                    return "+1MONTH";
                } else if ((moment(endDate).diff(startDate, 'month') < 3)) {
                    if ((moment(endDate).diff(startDate, 'month')) === 0) {
                        return that.getMonthDiff(startDate, endDate, dayGap, checkTime);
                    } else {
                        return "+1MONTH"
                    }

                } else {
                    return "+1MONTH";
                }
            } else {
                if ((moment(endDate).diff(startDate, 'days') < 10)) {
                    return "+2HOUR";
                } else if ((moment(endDate).diff(startDate, 'days') >15)) {
                    return "+8HOUR";
                } else if ((moment(endDate).diff(startDate, 'days') <= 30)) {
                    return "+1DAY";
                } else {
                    return "+1MONTH";
                }
            }
            
        }
        this.getRelativeDateFromString = function(string){
            var obj =  _.findWhere(Utils.relativeDates, { text : string})
            if(obj)
                return obj.fn && obj.fn();
        }

    };
    Utils.relativeDates = {
            last1Hour : {text : "Last 1 Hour",fn:Utils.dateUtil.getLast1HourRange},
            last24Hour: {text : "Last 24 Hour",fn:Utils.dateUtil.getLast24HourRange},
            today     : {text : "Today",fn:Utils.dateUtil.getTodayRange},
            yesterday : {text : "Yesterday",fn:Utils.dateUtil.getYesterdayRange},
            last7Days : {text : "Last 7 Days",fn:Utils.dateUtil.getLast7DaysRange},
            last30Days: {text : "Last 30 Days",fn:Utils.dateUtil.getLast30DaysRange},
            thisMonth : {text : "This Month",fn:Utils.dateUtil.getThisMonthRange},
            lastMonth : {text : "Last Month",fn:Utils.dateUtil.getLastMonthRange}
    };

    /*
     * Converting Include Exclude string
     */
    Utils.encodeIncludeExcludeStr = function(arrOrStr,doEncode,token){
        var token = token  || Globals.splitToken;
        if(doEncode && _.isArray(arrOrStr)){
            return arrOrStr.join(token);
        }else if(_.isString(arrOrStr)){
            return arrOrStr.split(token);
        }
    };

    Utils.localStorage = {
        checkLocalStorage:function(key,value){
            if (typeof(Storage) !== "undefined") {
                return this.getLocalStorage(key,value);
            } else {
                console.log('Sorry! No Web Storage support');
                Utils.cookie.checkCookie(key,value);
            }
        },
        setLocalStorage:function(key,value){
            localStorage.setItem(key,value);
            return {found:false,'value':value};
        },
        getLocalStorage:function(key,value){
            var keyValue = localStorage.getItem(key)
            if(!keyValue || keyValue == "undefined"){
                return this.setLocalStorage(key,value);
            }else{
                return {found:true,'value':keyValue};
            }
        }
        
    }
    Utils.cookie ={
        setCookie:function(cname,cvalue) {
            //var d = new Date();
            //d.setTime(d.getTime() + (exdays*24*60*60*1000));
            //var expires = "expires=" + d.toGMTString();
            document.cookie = cname+"="+cvalue+"; "
            return {found:false,'value':cvalue};
        },
        getCookie:function(findString) {
            var search = findString + "=";
            var ca = document.cookie.split(';');
            for(var i=0; i<ca.length; i++) {
                var c = ca[i];
                while (c.charAt(0)==' ') c = c.substring(1);
                if (c.indexOf(name) == 0) {
                    return c.substring(name.length, c.length);
                }
            }
            return "";
        },
        checkCookie:function(key,value) {
            var findString = getCookie(key);
            if (findString != "" || keyValue != "undefined") {
                return {found:true,'value':((findString == "undefined")?(undefined):(findString))};
            } else {
                return setCookie(key,value);
            }
        }
    }
    
    Utils.getRandomColor = function getRandomColor(str) {
        if(!str)
            return "#000";
        var hashCode = function(str) {
            var hash = 0;
            for (var i = 0; i < str.length; i++) {
               hash = str.charCodeAt(i) + ((hash << 5) - hash);
            }
            return hash;
        };

        var intToRGB = function(i){
            var c = (i & 0x00FFFFFF)
                .toString(16)
                .toUpperCase();

            return "00000".substring(0, 6 - c.length) + c;
        };
        return "#" +intToRGB(hashCode(str));
    };
    /**
     * [genrateSelect2 description]
     * @param  {[array of object]}  listOfselect2  []
     * listOfselect2 = [
                {
                    id: "select2 id",
                    placeholder: "placeholder",
                    collection: "collection",
                    dataText: 'display text', //in binding for appling name
                    collectionHasCode: 'collection dosnt have id then pass what you want to show', // if collection dont have id ,
                    modelAttr: 'collection attribute name',
                    mandatory: false,
                    nonCrud: getAuditSchemaFieldsName // pass function Name with params (suucess,error,etc)
                    fetch: true,
                    data:[] // it will not fetch from collection
                },
                {...}
                ]
                collectionFetchLov // listenTo in your view
     * @param  {[type]} that          [scope of function or view]
     * @return {[type]}               [description]
     */
    Utils.genrateSelect2 =function(listOfselect2,that){

        for (var i = 0; i < listOfselect2.length; i++) {

            if (listOfselect2[i]['data'] || listOfselect2[i]['attachSelect']) {
                if(listOfselect2[i]['attachSelect']){
                    that.ui[listOfselect2[i]['id']].select2({
                        placeholder: listOfselect2[i]['placeholder'],
                        width: '100%'
                    });
                    continue;
                }

                if(that.ui[listOfselect2[i]['id']]){
                    that.ui[listOfselect2[i]['id']].select2({
                        placeholder: listOfselect2[i]['placeholder'],
                        width: '100%',
                        data: listOfselect2[i]['data']
                    });
                }
               
                continue;
            } else {
                if(that.ui[listOfselect2[i]['id']]){
                    that.ui[listOfselect2[i]['id']].select2({
                        placeholder: listOfselect2[i]['placeholder'],
                        width: '100%',
                        data: []
                    });
                    that.ui[listOfselect2[i]['id']].select2("disable");
                }else{
                    continue;
                }
              
            }


            if (listOfselect2[i]['fetch']) {
                if (listOfselect2[i].collection && typeof that.listOfselect2[i].collection === "object") {
                    that.listOfselect2[i]['collectionFetchLov'] = that.listOfselect2[i].collection
                } else if (listOfselect2[i].collection && typeof that.listOfselect2[i].collection === "string") {
                    that.listOfselect2[i]['collectionFetchLov'] = that[listOfselect2[i]['collection']]
                }

            }
            
         }

        _.each(that.listOfselect2, function(obj, i) {
            if(obj['collectionFetchLov']){
                   that.listenTo(obj['collectionFetchLov'], "reset", function(collection, response, options) {
                    if (obj['collectionHasCode']) {
                        for (var i = 0; i < collection.models.length; i++) {
                            $.extend(collection.models[i].attributes, {
                                id: collection.models[i].get(obj['collectionHasCode'])
                            })
                        }
                    }
                var allowClearFlag = false;
                if(!obj['mandatory'])allowClearFlag = true
                var data = _.pluck(collection.models, 'attributes');
                that.ui[obj['id']].select2({
                    placeholder: obj['placeholder'],
                    width: '100%',
                    data: {
                        results: data,
                        text: obj['dataText']
                    },
                    allowClear: allowClearFlag,
                    formatSelection: function(item) {
                        return item[obj['dataText']];
                    },
                    formatResult: function(item) {
                        return item[obj['dataText']];
                    }
                });
                if(!obj['disabled']){
                    that.ui[obj['id']].select2("enable");
                }
                
                if (that.model && !that.model.isNew()) {
                    that.ui[obj['id']].select2('val', that.model.get(obj['modelAttr'])).trigger('change');
                }
            }, that);
                 if(obj['nonCrud']){
                    obj['nonCrud'](that.ui[obj['id']]);
                }else{
                    obj['collectionFetchLov'].fetch({reset:true});
                }
                
            }
        });
    },

    /* This Method for handling graph unit.
        which seperate number from the string and again append to
        the string by formatting it
    
    */
    Utils.graphUnitParse = function(unitVal){
        if(! unitVal){
            return "";
        }
        var pattern = /(\d)\s+(?=\d)/g;
        var number = unitVal.match(/\d+/g).map(Number);
        var numString = number.toString().replace(pattern , '$1');
        var str = unitVal.replace(/\d+/g, '').replace(/\+/g,'');
        return numString +" " + Utils.getCamelCase(str) + "(s) gap";
    },

    Utils.getCamelCase = function(str){
        if(!str){
            return "";
        }
        var str = str.toLowerCase();
        return str.replace(/(?:^|\s)\w/g, function(match) {
            return match.toUpperCase()
        });
    },
    Utils.manipulateValueForAddingAstrik = function(str){
        if(!str){
            return "";
        }
        var string = ((str.lastIndexOf('*',0) === 0)) ? str : '*'+str;
        string = ((str.lastIndexOf('*', str.length - 1) === str.length - 1)) ?  string : string+'*';
        
        return string;
    };
    
    return Utils;
});
