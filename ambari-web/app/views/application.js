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

App.ApplicationView = Em.View.extend({
  templateName: require('templates/application'),

  views: function () { 
    if (App.router.get('loggedIn')) { 
      return App.router.get('mainViewsController.ambariViews').filterProperty('visible'); 
    } else { 
      return []; 
    } }.property('App.router.mainViewsController.ambariViews.length', 'App.router.loggedIn'),

  didInsertElement: function () {
    // on 'Enter' pressed, trigger modal window primary button if primary button is enabled(green)
    // on 'Esc' pressed, close the modal
    $(document).keydown(function (event) {
      if (event.which === 13 || event.keyCode === 13) {
        $('.modal:last').trigger('enter-key-pressed');
      }
      return true;
    });
    $(document).keyup(function (event) {
      if (event.which === 27 || event.keyCode === 27) {
        $('.modal:last').trigger('escape-key-pressed');
      }
      return true;
    });

    $('[data-toggle=collapseSideNav]').click(function() {
      $('.navigation-bar-container').toggleClass('collapsed').promise().done(function(){

        if ($('.navigation-bar-container').hasClass('collapsed')) {
          // set submenu invisible when collapsed
          $('.navigation-bar-container.collapsed ul.sub-menu').hide();
          // set the hover effect when collapsed, should show sub-menu on hovering
          $(".navigation-bar-container.collapsed .side-nav-menu>li").hover(function() {
            $(this).children("ul.sub-menu").show();
          }, function() {
            $(this).children("ul.sub-menu").hide();
          });
        } else {
          // keep showing all submenu
          $('.navigation-bar-container ul.sub-menu').show();
          $(".navigation-bar-container .side-nav-menu>li").unbind('mouseenter mouseleave');
          $('[data-toggle=collapseSubMenu]').removeClass('glyphicon-menu-right');
          $('[data-toggle=collapseSubMenu]').addClass('glyphicon-menu-down');
        }

        //set main content left margin based on the width of side-nav
        $('#main').css('margin-left', $('.navigation-bar-container').width());
        $('footer').css('margin-left', $('.navigation-bar-container').width());
      });
      $('[data-toggle=collapseSideNav]').toggleClass('icon-double-angle-right');//, 100, "easeOutSine");
    });
  }
});
