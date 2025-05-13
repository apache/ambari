(function (factory) {
  "use strict";

  if (typeof define === "function" && define.amd) {

    // AMD. Register as an anonymous module.
    define(["jquery"], function (jQuery) {
      return factory(jQuery, window);
    });
  } else if (typeof module === "object" && module.exports) {

    // Node/CommonJS
    // eslint-disable-next-line no-undef
    module.exports = factory(require("jquery"), window);
  } else {

    // Browser globals
    factory(jQuery, window);
  }
})(function (jQuery, window) {
  "use strict";
  jQuery.uaMatch = function (ua) {
    ua = ua.toLowerCase();

    var match = /(chrome)[ \/]([\w.]+)/.exec(ua) ||
        /(webkit)[ \/]([\w.]+)/.exec(ua) ||
        /(opera)(?:.*version|)[ \/]([\w.]+)/.exec(ua) ||
        /(msie) ([\w.]+)/.exec(ua) ||
        ua.indexOf("compatible") < 0 && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec(ua)
        ||
        [];

    return {
      browser: match[1] || "",
      version: match[2] || "0"
    };
  };

  var matched = jQuery.uaMatch(navigator.userAgent);
  var browser = {};

  if (matched.browser) {
    browser[matched.browser] = true;
    browser.version = matched.version;
  }

// Chrome is Webkit, but Webkit is also Safari.
  if (browser.chrome) {
    browser.webkit = true;
  } else if (browser.webkit) {
    browser.safari = true;
  }

  jQuery.browser = browser;
  return jQuery;
});
