'use strict';

(function ($) {

  /**
   * jQuery plugin for navigation bars
   * Usage:
   * <pre>
   *   $('.navigation-bar').navigationBar();
   * </pre>
   *
   * @param {object} options see <code>$.fn.navigationBar.defaults</code>
   * @returns {$}
   */

  $.fn.navigationBar = function (options) {

    var settings = $.extend({}, $.fn.navigationBar.defaults, options);

    return this.each(function () {

      var containerSelector = '.navigation-bar-container';
      var $navigationContainer = $(this).find(containerSelector);
      var $sideNavToggler = $(this).find('[data-toggle=' + settings.navBarToggleDataAttr + ']');
      var $subMenuToggler = $(this).find('[data-toggle=' + settings.subMenuNavToggleDataAttr + ']');
      var firstLvlMenuItemsSelector = '.side-nav-menu>li';
      var secondLvlMenuItemsSelector = '.side-nav-menu>li>ul>li';
      var $servicesMenuItem = $(this).find('.side-nav-services-menu-item');
      var $allServicesAction = $(this).find('.all-services-actions');

      $subMenuToggler.each(function (index, toggler) {
        return $(toggler).parent().addClass('has-sub-menu');
      });

      if (settings.fitHeight) {
        $(this).addClass('navigation-bar-fit-height');
        $allServicesAction.children('.dropdown-menu').css('position', 'fixed');
        $allServicesAction.children('.dropdown-menu').css('top', '140px');
        $allServicesAction.children('.dropdown-menu').css('left', '130px');
      }

      function popStateHandler() {
        var path = window.location.pathname + window.location.hash;
        $navigationContainer.find('li a').each(function (index, link) {
          var $link = $(link);
          var href = $link.attr('data-href') || $link.attr('href');
          if (path.indexOf(href) !== -1 && !['', '#'].includes(href)) {
            $link.parent().addClass('active');
          } else {
            $link.parent().removeClass('active');
          }
        });
      }

      if (settings.handlePopState) {
        popStateHandler();
        $(window).bind('popstate', popStateHandler);
      }

      function clickHandler(el) {
        var $li = $(el).parent();
        var activeClass = settings.activeClass;

        var activeMenuItems = firstLvlMenuItemsSelector + '.' + activeClass;
        var activeSubMenuItems = secondLvlMenuItemsSelector + '.' + activeClass;
        $navigationContainer.find(activeMenuItems).removeClass(activeClass);
        $navigationContainer.find(activeSubMenuItems).removeClass(activeClass);
        $li.addClass(activeClass);
      }

      /**
       * Click on menu item
       */
      $(firstLvlMenuItemsSelector + '>a').on('click', function () {
        clickHandler(this);
      });

      /**
       * Click on sub menu item
       */
      $(secondLvlMenuItemsSelector + '>a').on('click', function () {
        clickHandler(this);
        $(this).parent().parent().parent().addClass(settings.activeClass);
      });

      /**
       * Slider for sub menu
       */
      $subMenuToggler.off('click').on('click', function (event) {
        // ignore click if navigation-bar is collapsed
        if ($navigationContainer.hasClass('collapsed')) {
          return false;
        }
        var $this = $(this);
        $this.siblings('.sub-menu').slideToggle(600);
        $this.children('.toggle-icon').toggleClass(settings.menuLeftClass + ' ' + settings.menuDownClass);
        event.stopPropagation();
        return false;
      });

      /**
       * hover effects for all services actions icon
       */
      $servicesMenuItem.hover(function () {
        if (!$navigationContainer.hasClass('collapsed')) {
          $allServicesAction.css('display', 'inline-block');
        }
      }, function () {
        if (!$navigationContainer.hasClass('collapsed')) {
          $allServicesAction.css('display', 'none');
        }
      });
      $allServicesAction.hover(function () {
        $allServicesAction.css('display', 'inline-block');
      });

      /**
       * Expand/collapse navigation bar
       */
      $sideNavToggler.click(function () {

        $navigationContainer.toggleClass('collapsed').promise().done(function () {
          var subMenuSelector = 'ul.sub-menu';
          var $subMenus = $navigationContainer.find(subMenuSelector);
          var $subMenuItems = $navigationContainer.find('.side-nav-menu>li');
          if ($navigationContainer.hasClass('collapsed')) {
            // set sub menu invisible when collapsed
            $subMenus.hide();
            // set the hover effect when collapsed, should show sub-menu on hovering
            $subMenuItems.hover(function () {
              $(this).find(subMenuSelector).show();
            }, function () {
              $(this).find(subMenuSelector).hide();
            });
          } else {
            // keep showing all sub menu
            $subMenus.show();
            $subMenuItems.unbind('mouseenter mouseleave');
            $navigationContainer.find('.toggle-icon').removeClass(settings.menuLeftClass).addClass(settings.menuDownClass);
          }

          //set main content left margin based on the width of side-nav
          var containerWidth = $navigationContainer.width();
          if (settings.moveLeftContent) {
            $(settings.content).css('margin-left', containerWidth);
          }
          if (settings.moveLeftFooter) {
            $(settings.footer).css('margin-left', containerWidth);
          }
          $sideNavToggler.find('span').toggleClass(settings.collapseNavBarClass + ' ' + settings.expandNavBarClass);
        });
        return false;
      });
    });
  };

  $.fn.navigationBar.defaults = {
    handlePopState: true,
    fitHeight: false,
    content: '#main',
    footer: 'footer',
    moveLeftContent: true,
    moveLeftFooter: true,
    menuLeftClass: 'glyphicon-menu-right',
    menuDownClass: 'glyphicon-menu-down',
    collapseNavBarClass: 'fa-angle-double-left',
    expandNavBarClass: 'fa-angle-double-right',
    activeClass: 'active',
    navBarToggleDataAttr: 'collapse-side-nav',
    subMenuNavToggleDataAttr: 'collapse-sub-menu'
  };
})(jQuery);