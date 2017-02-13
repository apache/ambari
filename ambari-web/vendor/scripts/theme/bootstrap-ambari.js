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

      if (settings.fitHeight) {
        $(this).addClass('navigation-bar-fit-height');
      }

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
          $sideNavToggler.toggleClass(settings.collapseNavBarClass + ' ' + settings.expandNavBarClass);
        });
        return false;
      });
    });
  };

  $.fn.navigationBar.defaults = {
    fitHeight: false,
    content: '#main',
    footer: 'footer',
    moveLeftContent: true,
    moveLeftFooter: true,
    menuLeftClass: 'glyphicon-menu-right',
    menuDownClass: 'glyphicon-menu-down',
    collapseNavBarClass: 'fa-angle-double-left',
    expandNavBarClass: 'fa-angle-double-right',
    navBarToggleDataAttr: 'collapse-side-nav',
    subMenuNavToggleDataAttr: 'collapse-sub-menu'
  };
})(jQuery);