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

/**
 * @file Controlling all the routes in the application through router
 */
define(['require',
    'backbone',
    'models/VAppState',
    'App','utils/Globals',
    'backbone.marionette'
],function(require,Backbone,AppStateMod,App,XAGlobals) {
    'use strict';
    
    var AppRouting  = {};


    /**
     * Mention routes and its route handler function for overall application
     */
    AppRouting.Router = Backbone.Marionette.AppRouter.extend({

        appRoutes: {
            ""					: "dashboardAction",
            "!/dashboard"       : "dashboardAction",
            "!/logout"			: "logoutAction"
        },

       /**
        *  Override execute for Gloabal Loader it is an syn method 
        *  which is resolved in commonControllerRoutesAction
        *  But for now not required
        */
    /*    execute: function(callback, args) {
            this.appLoaderShow();
            try {
                if (callback) callback.apply(this, args);
            } catch (e) {
                FSUtils.hideAppLoader();
            }
        },
        appLoaderShow: function() {
            var that = this
            App.appDeff = $.Deferred(),
                FSUtils.showAppLoader();
            $.when(App.appDeff).done(function() {
                FSUtils.hideAppLoader();
            });

        }*/

    });

    /**
     * This is the controller object for every routes present in the application
     */
    AppRouting.Controller = Backbone.Marionette.Controller.extend({
        /** @lends AppRouting.Controller */

        /** intialize a new Controller */
        initialize: function(){
            this.showRegions();
            //this.FSAppTabs = new FSAppTabs();
            this.globalVent = new Backbone.Wreqr.EventAggregator();
        },

        /** setting up user profile values to be used overall in the application */
        setGlobalValues: function(){
            require(['models/VUser'], function(VUser){
                // get logged in user's profile
                var userMod = new VUser();
                userMod.getUserProfile({
                    success: function(data, textStatus, jqXHR){
                        userMod.trigger('sync');
                        AppStateMod.set('userProfile', data);
                    },
                    error: function(jqXHR, textStatus, errorThrown){
                        userMod.trigger('error', userMod, jqXHR);
                    }
                });
            });
        },

        /** show the Top bar and side bar */
        showRegions: function(){
            var that = this;
            require(["moment", "utils/Utils", "jstimezonedetect","moment-tz","utils/ViewUtils","WorldMapGenerator"],
                function(moment, Utils, jstz,momentTz,ViewUtils,WorldMapGenerator) {
                    var storeTimezone = Utils.localStorage.checkLocalStorage('timezone');
                    var systemZone = jstz.determine().name();
                    if (!storeTimezone.value || storeTimezone.value == "undefined") {
                       
                        Utils.localStorage.setLocalStorage('timezone', systemZone);
                        storeTimezone.value = systemZone;
                    }
                    if(storeTimezone.value.split(',').length>1){
                      var timezone =  storeTimezone.value.split(',')[0];
                    }else{
                      var timezone =  systemZone;
                    }
                    moment.tz.setDefault(timezone);
                    ViewUtils.setdefaultParams(); // setting default params after moment timezone is set
                    require(['views/common/Header'],
                        function(HeaderView) {
                            App.rHeader.show(new HeaderView({
                                globalVent: that.globalVent
                        }));
                    });
            });
            
        },

        /** function which trigger the TopBarView to show the page content in the respective tab */
        commonControllerRoutesAction: function(View, viewObj, viewOptions){
//            var ErrorLayoutView = require('views/common/ErrorLayoutView'),
//                error403tmpl = require('hbs!tmpl/site/error403');
//
//            if(!this.isAccessGranted(viewOptions)){
//                View = ErrorLayoutView;
//                viewObj = App.getView(FSGlobals.AppTabs.ERROR.value);
//                viewOptions = {errorTmpl: error403tmpl};
//            }
//
//            if(viewObj === null) {
//
//                viewObj = new View(_.extend({}, viewOptions));
//
//                App.saveView(viewObj);
//            }
//            //Resolving deff for global Loader
//           /* if(App.appDeff){
//                App.appDeff.resolve();
//            }*/
//            this.FSAppTabs.showView(viewObj);
        },

        /**
         * check whether logged-in user has access to that module
         * @param  {Object}  viewOptions - options for the view to be shown
         * @return {Boolean} has access or not
         */
        isAccessGranted: function(viewOptions){
            var str = Backbone.history.fragment.replace(/!\//g, "");
            if (viewOptions && viewOptions._multipleTabs) {
                str = str && str.split("/")[0];
            }
            var hasAccess = false;
            if (str === "") {
                hasAccess = true;
            } else {
                var isCheckAccess = this.checkAccess(str) || this.isSpecialMenuAccess(str);
                hasAccess = !!isCheckAccess;
            }
            return hasAccess;
        },

        /**
         * check whether logged-in user has access to that module from the list received from server
         * @param  {String} routeName - name of the menu item clicked
         * @return {Object} return object match found or else return undefined value
         */
        checkAccess: function(routeName){
            return App.menuListCol.findWhere({
                menuCode: routeName.toUpperCase()
            });
        },

        /**
         * whether route has been given special permission
         * @param  {String} routeName - name of the menu item clicked
         * @return {Boolean} - route has been given special permission or not
         */
        isSpecialMenuAccess: function(routeName){
            return FSLinks.isSpecialMenuAccess(routeName);
        },

        /*********************
         * Dashboard Actions *
         *********************/
        dashboardAction: function() {
            var that = this;
            AppStateMod.set({'currentTab': XAGlobals.AppTabs.MAINVIEW.value });
            require(['views/dashboard/MainLayoutView'],
                function(view){
            	App.rContent.show(new view({
                    globalVent:that.globalVent
                }));

            });
        },
        
        logoutAction: function() {
            if (sessionStorage) {
                sessionStorage.clear();
            }
            window.location.replace("logout.html");
        },

        databaseAction: function(){
            var that = this;
            AppStateMod.set({'currentTab': FSGlobals.AppTabs.DATABASES.value });

//            require(['views/infraConfig/DatabaseConfigLayoutView',
//                'collections/VDatabaseList'],
//                function(view, VDatabaseList){
//
//                var viewObj = App.getView(FSGlobals.AppTabs.DATABASES.value);
//                var databaseListCol = new VDatabaseList();
//                var viewOptions = {
//                    collection: databaseListCol
//                };
//
//                that.commonControllerRoutesAction(view, viewObj, viewOptions);
//
//                databaseListCol.fetch({reset: true });
//            });
        }

    });

    return {
		Controller: AppRouting.Controller,
		Router: AppRouting.Router
	};
});
