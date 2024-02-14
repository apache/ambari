/**
 * Wait until Angular has finished rendering and has
 * no outstanding $http calls before continuing. The specific Angular app
 * is determined by the rootSelector. Copied from Protractor 5.
 *
 * Asynchronous.
 *
 * @param {string} rootSelector The selector housing an ng-app
 * @param {function(string)} callback callback. If a failure occurs, it will
 *     be passed as a parameter.
 */
function waitForAngular(rootSelector, callback) {
  try {
    if (window.angular && !(window.angular.version &&
        window.angular.version.major > 1)) {
      /* ng1 */
      var hooks = getNg1Hooks(rootSelector);
      if (hooks.$$testability) {
        hooks.$$testability.whenStable(callback);
      } else if (hooks.$injector) {
        hooks.$injector.get('$browser').
        notifyWhenNoOutstandingRequests(callback);
      } else if (!!rootSelector) {
        throw new Error('Could not automatically find injector on page: "' +
            window.location.toString() + '".  Consider using config.rootEl');
      } else {
        throw new Error('root element (' + rootSelector + ') has no injector.' +
            ' this may mean it is not inside ng-app.');
      }
    } else if (rootSelector && window.getAngularTestability) {
      var el = document.querySelector(rootSelector);
      window.getAngularTestability(el).whenStable(callback);
    } else if (window.getAllAngularTestabilities) {
      var testabilities = window.getAllAngularTestabilities();
      var count = testabilities.length;
      var decrement = function() {
        count--;
        if (count === 0) {
          callback();
        }
      };
      testabilities.forEach(function(testability) {
        testability.whenStable(decrement);
      });
    } else if (!window.angular) {
      throw new Error('window.angular is undefined.  This could be either ' +
          'because this is a non-angular page or because your test involves ' +
          'client-side navigation, which can interfere with Protractor\'s ' +
          'bootstrapping.  See http://git.io/v4gXM for details');
    } else if (window.angular.version >= 2) {
      throw new Error('You appear to be using angular, but window.' +
          'getAngularTestability was never set.  This may be due to bad ' +
          'obfuscation.');
    } else {
      throw new Error('Cannot get testability API for unknown angular ' +
          'version "' + window.angular.version + '"');
    }
  } catch (err) {
    callback(err.message);
  }
};

/* Tries to find $$testability and possibly $injector for an ng1 app
 *
 * By default, doesn't care about $injector if it finds $$testability.  However,
 * these priorities can be reversed.
 *
 * @param {string=} selector The selector for the element with the injector.  If
 *   falsy, tries a variety of methods to find an injector
 * @param {boolean=} injectorPlease Prioritize finding an injector
 * @return {$$testability?: Testability, $injector?: Injector} Returns whatever
 *   ng1 app hooks it finds
 */
function getNg1Hooks(selector, injectorPlease) {
  function tryEl(el) {
    try {
      if (!injectorPlease && angular.getTestability) {
        var $$testability = angular.getTestability(el);
        if ($$testability) {
          return {$$testability: $$testability};
        }
      } else {
        var $injector = angular.element(el).injector();
        if ($injector) {
          return {$injector: $injector};
        }
      }
    } catch(err) {}
  }
  function trySelector(selector) {
    var els = document.querySelectorAll(selector);
    for (var i = 0; i < els.length; i++) {
      var elHooks = tryEl(els[i]);
      if (elHooks) {
        return elHooks;
      }
    }
  }

  if (selector) {
    return trySelector(selector);
  } else if (window.__TESTABILITY__NG1_APP_ROOT_INJECTOR__) {
    var $injector = window.__TESTABILITY__NG1_APP_ROOT_INJECTOR__;
    var $$testability = null;
    try {
      $$testability = $injector.get('$$testability');
    } catch (e) {}
    return {$injector: $injector, $$testability: $$testability};
  } else {
    return tryEl(document.body) ||
        trySelector('[ng-app]') || trySelector('[ng:app]') ||
        trySelector('[ng-controller]') || trySelector('[ng:controller]');
  }
}

/* Wraps a function up into a string with its helper functions so that it can
 * call those helper functions client side
 *
 * @param {function} fun The function to wrap up with its helpers
 * @param {...function} The helper functions.  Each function must be named
 *
 * @return {string} The string which, when executed, will invoke fun in such a
 *   way that it has access to its helper functions
 */
function wrapWithHelpers(fun) {
  var helpers = Array.prototype.slice.call(arguments, 1);
  if (!helpers.length) {
    return fun;
  }
  var FunClass = Function; // Get the linter to allow this eval
  return new FunClass(
      helpers.join(';') + String.fromCharCode(59) +
      '  return (' + fun.toString() + ').apply(this, arguments);');
}

exports.NG_WAIT_FN = wrapWithHelpers(waitForAngular, getNg1Hooks);
