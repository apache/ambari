(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory(require("@angular/core"), require("@angular/common"), require("@angular/forms"), require("rxjs/add/operator/filter"), require("rxjs/BehaviorSubject"), require("rxjs/Observable"), require("rxjs/add/operator/map"), require("rxjs/Subject"), require("rxjs/operator/distinctUntilChanged"), require("rxjs/operator/map"), require("rxjs/operator/observeOn"), require("rxjs/scheduler/queue"), require("rxjs/operator/scan"), require("rxjs/add/observable/from"), require("rxjs/add/operator/debounceTime"), require("rxjs/add/operator/mergeMap"), require("rxjs/add/operator/toArray"));
	else if(typeof define === 'function' && define.amd)
		define(["@angular/core", "@angular/common", "@angular/forms", "rxjs/add/operator/filter", "rxjs/BehaviorSubject", "rxjs/Observable", "rxjs/add/operator/map", "rxjs/Subject", "rxjs/operator/distinctUntilChanged", "rxjs/operator/map", "rxjs/operator/observeOn", "rxjs/scheduler/queue", "rxjs/operator/scan", "rxjs/add/observable/from", "rxjs/add/operator/debounceTime", "rxjs/add/operator/mergeMap", "rxjs/add/operator/toArray"], factory);
	else if(typeof exports === 'object')
		exports["ngx-bootstrap.umd"] = factory(require("@angular/core"), require("@angular/common"), require("@angular/forms"), require("rxjs/add/operator/filter"), require("rxjs/BehaviorSubject"), require("rxjs/Observable"), require("rxjs/add/operator/map"), require("rxjs/Subject"), require("rxjs/operator/distinctUntilChanged"), require("rxjs/operator/map"), require("rxjs/operator/observeOn"), require("rxjs/scheduler/queue"), require("rxjs/operator/scan"), require("rxjs/add/observable/from"), require("rxjs/add/operator/debounceTime"), require("rxjs/add/operator/mergeMap"), require("rxjs/add/operator/toArray"));
	else
		root["ngx-bootstrap.umd"] = factory(root["@angular/core"], root["@angular/common"], root["@angular/forms"], root["rxjs/add/operator/filter"], root["rxjs/BehaviorSubject"], root["rxjs/Observable"], root["rxjs/add/operator/map"], root["rxjs/Subject"], root["rxjs/operator/distinctUntilChanged"], root["rxjs/operator/map"], root["rxjs/operator/observeOn"], root["rxjs/scheduler/queue"], root["rxjs/operator/scan"], root["rxjs/add/observable/from"], root["rxjs/add/operator/debounceTime"], root["rxjs/add/operator/mergeMap"], root["rxjs/add/operator/toArray"]);
})(this, function(__WEBPACK_EXTERNAL_MODULE_0__, __WEBPACK_EXTERNAL_MODULE_2__, __WEBPACK_EXTERNAL_MODULE_4__, __WEBPACK_EXTERNAL_MODULE_21__, __WEBPACK_EXTERNAL_MODULE_54__, __WEBPACK_EXTERNAL_MODULE_110__, __WEBPACK_EXTERNAL_MODULE_118__, __WEBPACK_EXTERNAL_MODULE_143__, __WEBPACK_EXTERNAL_MODULE_145__, __WEBPACK_EXTERNAL_MODULE_146__, __WEBPACK_EXTERNAL_MODULE_147__, __WEBPACK_EXTERNAL_MODULE_148__, __WEBPACK_EXTERNAL_MODULE_149__, __WEBPACK_EXTERNAL_MODULE_150__, __WEBPACK_EXTERNAL_MODULE_151__, __WEBPACK_EXTERNAL_MODULE_152__, __WEBPACK_EXTERNAL_MODULE_153__) {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "/";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 128);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_0__;

/***/ }),
/* 1 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["e"] = getHours;
/* harmony export (immutable) */ __webpack_exports__["f"] = getMinutes;
/* harmony export (immutable) */ __webpack_exports__["h"] = getSeconds;
/* harmony export (immutable) */ __webpack_exports__["b"] = getDayOfWeek;
/* harmony export (immutable) */ __webpack_exports__["a"] = getDate;
/* harmony export (immutable) */ __webpack_exports__["g"] = getMonth;
/* harmony export (immutable) */ __webpack_exports__["d"] = getFullYear;
/* harmony export (immutable) */ __webpack_exports__["c"] = getFirstDayOfMonth;
/* unused harmony export daysInMonth */
/* unused harmony export _daysInMonth */
/* harmony export (immutable) */ __webpack_exports__["i"] = isFirstDayOfWeek;
/* harmony export (immutable) */ __webpack_exports__["k"] = isSameMonth;
/* harmony export (immutable) */ __webpack_exports__["l"] = isSameYear;
/* harmony export (immutable) */ __webpack_exports__["j"] = isSameDay;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__date_setters__ = __webpack_require__(7);

function getHours(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCHours() : date.getHours();
}
function getMinutes(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCMinutes() : date.getMinutes();
}
function getSeconds(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCSeconds() : date.getSeconds();
}
function getDayOfWeek(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCDay() : date.getDay();
}
function getDate(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCDate() : date.getDate();
}
function getMonth(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCMonth() : date.getMonth();
}
function getFullYear(date, isUTC) {
    if (isUTC === void 0) { isUTC = false; }
    return isUTC ? date.getUTCFullYear() : date.getFullYear();
}
function getFirstDayOfMonth(date) {
    return Object(__WEBPACK_IMPORTED_MODULE_0__date_setters__["a" /* createDate */])(date.getFullYear(), date.getMonth(), 1, date.getHours(), date.getMinutes(), date.getSeconds());
}
function daysInMonth(date) {
    return _daysInMonth(date.getFullYear(), date.getMonth());
}
function _daysInMonth(year, month) {
    return new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
}
function isFirstDayOfWeek(date, firstDayOfWeek) {
    return date.getDay() === firstDayOfWeek;
}
function isSameMonth(date1, date2) {
    if (!date1 || !date2) {
        return false;
    }
    return isSameYear(date1, date2) && getMonth(date1) === getMonth(date2);
}
function isSameYear(date1, date2) {
    if (!date1 || !date2) {
        return false;
    }
    return getFullYear(date1) === getFullYear(date2);
}
function isSameDay(date1, date2) {
    if (!date1 || !date2) {
        return false;
    }
    return isSameYear(date1, date2) && isSameMonth(date1, date2)
        && getDate(date1) === getDate(date2);
}


/***/ }),
/* 2 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_2__;

/***/ }),
/* 3 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = isBs3;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__facade_browser__ = __webpack_require__(20);

var guessedVersion;
function _guessBsVersion() {
    if (typeof document === 'undefined') {
        return null;
    }
    var spanEl = document.createElement('span');
    spanEl.innerText = 'test bs version';
    document.body.appendChild(spanEl);
    spanEl.classList.add('d-none');
    var rect = spanEl.getBoundingClientRect();
    document.body.removeChild(spanEl);
    if (!rect) {
        return 'bs3';
    }
    return rect.top === 0 ? 'bs4' : 'bs3';
}
// todo: in ngx-bootstrap, bs4 will became a default one
function isBs3() {
    if (typeof __WEBPACK_IMPORTED_MODULE_0__facade_browser__["b" /* window */] === 'undefined') {
        return true;
    }
    if (typeof __WEBPACK_IMPORTED_MODULE_0__facade_browser__["b" /* window */].__theme === 'undefined') {
        if (guessedVersion) {
            return guessedVersion === 'bs3';
        }
        guessedVersion = _guessBsVersion();
        return guessedVersion === 'bs3';
    }
    return __WEBPACK_IMPORTED_MODULE_0__facade_browser__["b" /* window */].__theme !== 'bs4';
}


/***/ }),
/* 4 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_4__;

/***/ }),
/* 5 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return formatFunctions; });
/* unused harmony export formatTokenFunctions */
/* unused harmony export formattingTokens */
/* harmony export (immutable) */ __webpack_exports__["a"] = addFormatToken;
/* harmony export (immutable) */ __webpack_exports__["c"] = makeFormatFunction;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__utils__ = __webpack_require__(16);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_type_checks__ = __webpack_require__(11);


var formatFunctions = {};
var formatTokenFunctions = {};
var formattingTokens = /(\[[^\[]*\])|(\\)?([Hh]mm(ss)?|Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Qo?|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|kk?|mm?|ss?|S{1,9}|x|X|zz?|ZZ?|.)/g;
// token:    'M'
// padded:   ['MM', 2]
// ordinal:  'Mo'
// callback: function () { this.month() + 1 }
function addFormatToken(token, padded, ordinal, callback) {
    var func = callback;
    if (token) {
        formatTokenFunctions[token] = func;
    }
    if (padded) {
        var key = padded[0];
        formatTokenFunctions[key] = function (date, format, locale) {
            return Object(__WEBPACK_IMPORTED_MODULE_0__utils__["d" /* zeroFill */])(func.apply(null, arguments), padded[1], padded[2]);
        };
    }
    if (ordinal) {
        formatTokenFunctions[ordinal] = function (date, format, locale) {
            // todo: fix this
            return locale.ordinal(func.apply(null, arguments), token);
        };
    }
}
function makeFormatFunction(format) {
    var array = format.match(formattingTokens);
    var length = array.length;
    var formatArr = new Array(length);
    for (var i = 0; i < length; i++) {
        formatArr[i] = formatTokenFunctions[array[i]]
            ? formatTokenFunctions[array[i]]
            : removeFormattingTokens(array[i]);
    }
    return function (date, locale) {
        var output = '';
        for (var j = 0; j < length; j++) {
            output += Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["d" /* isFunction */])(formatArr[j])
                ? formatArr[j].call(null, date, format, locale)
                : formatArr[j];
        }
        return output;
    };
}
function removeFormattingTokens(input) {
    if (input.match(/\[[\s\S]/)) {
        return input.replace(/^\[|\]$/g, '');
    }
    return input.replace(/\\/g, '');
}


/***/ }),
/* 6 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__component_loader_class__ = __webpack_require__(84);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__component_loader_class__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__component_loader_factory__ = __webpack_require__(13);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__component_loader_factory__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__content_ref_class__ = __webpack_require__(85);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_2__content_ref_class__["a"]; });





/***/ }),
/* 7 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = createDate;
/* harmony export (immutable) */ __webpack_exports__["c"] = shiftDate;
/* harmony export (immutable) */ __webpack_exports__["b"] = setDate;
var defaultTimeUnit = {
    year: 0, month: 0, day: 0, hour: 0, minute: 0, seconds: 0
};
function createDate(year, month, day, hour, minute, seconds) {
    if (month === void 0) { month = 0; }
    if (day === void 0) { day = 1; }
    if (hour === void 0) { hour = 0; }
    if (minute === void 0) { minute = 0; }
    if (seconds === void 0) { seconds = 0; }
    var _date = new Date();
    return new Date(year || _date.getFullYear(), month, day, hour, minute, seconds);
}
function shiftDate(date, unit) {
    var _unit = Object.assign({}, defaultTimeUnit, unit);
    return createDate(date.getFullYear() + _unit.year, date.getMonth() + _unit.month, date.getDate() + _unit.day, date.getHours() + _unit.hour, date.getMinutes() + _unit.minute, date.getSeconds() + _unit.seconds);
}
function setDate(date, unit) {
    return createDate(getNum(date.getFullYear(), unit.year), getNum(date.getMonth(), unit.month), getNum(date.getDate(), unit.day), getNum(date.getHours(), unit.hour), getNum(date.getMinutes(), unit.minute), getNum(date.getSeconds(), unit.seconds));
}
function getNum(def, num) {
    return typeof num === 'number' ? num : def;
}


/***/ }),
/* 8 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var BsDatepickerConfig = (function () {
    function BsDatepickerConfig() {
        /** CSS class which will be applied to datepicker container,
         * usually used to set color theme
         */
        this.containerClass = 'theme-green';
        // DatepickerRenderOptions
        this.displayMonths = 1;
        /**
         * Allows to hide week numbers in datepicker
         */
        this.showWeekNumbers = true;
        this.dateInputFormat = 'L';
        // range picker
        this.rangeSeparator = ' - ';
        this.rangeInputFormat = 'L';
        // DatepickerFormatOptions
        /**
         * Allows to globally set default locale of datepicker,
         * see documentation on how to enable custom locales
         */
        this.locale = 'en';
        this.monthTitle = 'MMMM';
        this.yearTitle = 'YYYY';
        this.dayLabel = 'D';
        this.monthLabel = 'MMMM';
        this.yearLabel = 'YYYY';
        this.weekNumbers = 'w';
    }
    BsDatepickerConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], BsDatepickerConfig);
    return BsDatepickerConfig;
}());



/***/ }),
/* 9 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__ng_positioning__ = __webpack_require__(86);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_0__ng_positioning__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__ng_positioning__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__positioning_service__ = __webpack_require__(87);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__positioning_service__["a"]; });




/***/ }),
/* 10 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = formatDate;
/* unused harmony export formatMoment */
/* unused harmony export expandFormat */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__locale__ = __webpack_require__(130);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__units__ = __webpack_require__(133);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__locale_locales_service__ = __webpack_require__(12);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__utils_type_checks__ = __webpack_require__(11);
// moment.js
// version : 2.18.1
// authors : Tim Wood, Iskren Chernev, Moment.js contributors
// license : MIT
// momentjs.com





function formatDate(date, format, locale) {
    if (locale === void 0) { locale = 'en'; }
    var _locale = Object(__WEBPACK_IMPORTED_MODULE_3__locale_locales_service__["b" /* getLocale */])(locale);
    if (!_locale) {
        throw new Error("Locale \"" + locale + "\" is not defined, please add it with \"defineLocale(...)\"");
    }
    var output = formatMoment(date, format, _locale);
    return _locale.postformat(output);
}
// format date using native date object
function formatMoment(date, format, locale) {
    if (!Object(__WEBPACK_IMPORTED_MODULE_4__utils_type_checks__["c" /* isDateValid */])(date)) {
        return locale.invalidDate;
    }
    format = expandFormat(format, locale);
    __WEBPACK_IMPORTED_MODULE_0__format_functions__["b" /* formatFunctions */][format] = __WEBPACK_IMPORTED_MODULE_0__format_functions__["b" /* formatFunctions */][format] || Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["c" /* makeFormatFunction */])(format);
    return __WEBPACK_IMPORTED_MODULE_0__format_functions__["b" /* formatFunctions */][format](date, locale);
}
function expandFormat(format, locale) {
    var i = 5;
    var localFormattingTokens = /(\[[^\[]*\])|(\\)?(LTS|LT|LL?L?L?|l{1,4})/g;
    var replaceLongDateFormatTokens = function (input) {
        return locale.longDateFormat(input) || input;
    };
    localFormattingTokens.lastIndex = 0;
    while (i >= 0 && localFormattingTokens.test(format)) {
        format = format.replace(localFormattingTokens, replaceLongDateFormatTokens);
        localFormattingTokens.lastIndex = 0;
        i -= 1;
    }
    return format;
}


/***/ }),
/* 11 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["c"] = isDateValid;
/* harmony export (immutable) */ __webpack_exports__["d"] = isFunction;
/* harmony export (immutable) */ __webpack_exports__["b"] = isArray;
/* harmony export (immutable) */ __webpack_exports__["a"] = hasOwnProp;
/* harmony export (immutable) */ __webpack_exports__["e"] = isObject;
/* harmony export (immutable) */ __webpack_exports__["f"] = isUndefined;
/* harmony export (immutable) */ __webpack_exports__["g"] = toInt;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__utils__ = __webpack_require__(16);

function isDateValid(date) {
    return date && !isNaN(date.getTime());
}
function isFunction(fn) {
    return fn instanceof Function || Object.prototype.toString.call(fn) === '[object Function]';
}
function isArray(input) {
    return input instanceof Array || Object.prototype.toString.call(input) === '[object Array]';
}
function hasOwnProp(a /*object*/, b) {
    return Object.prototype.hasOwnProperty.call(a, b);
}
function isObject(input /*object*/) {
    // IE8 will treat undefined and null as object if it wasn't for
    // input != null
    return input != null && Object.prototype.toString.call(input) === '[object Object]';
}
function isUndefined(input) {
    return input === void 0;
}
function toInt(argumentForCoercion) {
    var coercedNumber = +argumentForCoercion;
    var value = 0;
    if (coercedNumber !== 0 && isFinite(coercedNumber)) {
        value = Object(__WEBPACK_IMPORTED_MODULE_0__utils__["a" /* absFloor */])(coercedNumber);
    }
    return value;
}


/***/ }),
/* 12 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["b"] = getLocale;
/* unused harmony export listLocales */
/* unused harmony export mergeConfigs */
/* harmony export (immutable) */ __webpack_exports__["c"] = getSetGlobalLocale;
/* harmony export (immutable) */ __webpack_exports__["a"] = defineLocale;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__locale_class__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__locale_defaults__ = __webpack_require__(132);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_type_checks__ = __webpack_require__(11);
// internal storage for locale config files



var locales = {};
var localeFamilies = {};
var globalLocale;
function chooseLocale(name) {
    return locales[name];
}
// returns locale data
function getLocale(key) {
    if (!key) {
        return globalLocale;
    }
    return chooseLocale(key);
}
function listLocales() {
    return Object.keys(locales);
}
function mergeConfigs(parentConfig, childConfig) {
    var res = Object.assign({}, parentConfig);
    for (var childProp in childConfig) {
        if (!Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["a" /* hasOwnProp */])(childConfig, childProp)) {
            continue;
        }
        if (Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["e" /* isObject */])(parentConfig[childProp]) && Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["e" /* isObject */])(childConfig[childProp])) {
            (res[childProp]) = {};
            Object.assign(res[childProp], parentConfig[childProp]);
            Object.assign(res[childProp], childConfig[childProp]);
        }
        else if (childConfig[childProp] != null) {
            (res[childProp]) = childConfig[childProp];
        }
        else {
            delete res[childProp];
        }
    }
    for (var parentProp in parentConfig) {
        if (Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["a" /* hasOwnProp */])(parentConfig, parentProp) &&
            !Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["a" /* hasOwnProp */])(childConfig, parentProp) &&
            Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["e" /* isObject */])(parentConfig[parentProp])) {
            // make sure changes to properties don't modify parent config
            (res[parentProp]) = Object.assign({}, res[parentProp]);
        }
    }
    return res;
}
// This function will load locale and then set the global locale.  If
// no arguments are passed in, it will simply return the current global
// locale key.
function getSetGlobalLocale(key, values) {
    var data;
    if (key) {
        data = Object(__WEBPACK_IMPORTED_MODULE_2__utils_type_checks__["f" /* isUndefined */])(values) ? getLocale(key) : defineLocale(key, values);
        if (data) {
            globalLocale = data;
        }
    }
    return globalLocale._abbr;
}
function defineLocale(name, config) {
    if (config === null) {
        // useful for testing
        delete locales[name];
        return null;
    }
    config.abbr = name;
    locales[name] = new __WEBPACK_IMPORTED_MODULE_0__locale_class__["a" /* Locale */](mergeConfigs(__WEBPACK_IMPORTED_MODULE_1__locale_defaults__["a" /* baseConfig */], config));
    if (localeFamilies[name]) {
        localeFamilies[name].forEach(function (x) {
            defineLocale(x.name, x.config);
        });
    }
    // backwards compat for now: also set the locale
    // make sure we set the locale AFTER all child locales have been
    // created, so we won't end up with the child locale set.
    getSetGlobalLocale(name);
    return locales[name];
}


/***/ }),
/* 13 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ComponentLoaderFactory; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__component_loader_class__ = __webpack_require__(84);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__positioning__ = __webpack_require__(9);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var ComponentLoaderFactory = (function () {
    function ComponentLoaderFactory(_componentFactoryResolver, _ngZone, _injector, _posService, _applicationRef) {
        this._componentFactoryResolver = _componentFactoryResolver;
        this._ngZone = _ngZone;
        this._injector = _injector;
        this._posService = _posService;
        this._applicationRef = _applicationRef;
    }
    /**
     *
     * @param _elementRef
     * @param _viewContainerRef
     * @param _renderer
     * @returns {ComponentLoader}
     */
    ComponentLoaderFactory.prototype.createLoader = function (_elementRef, _viewContainerRef, _renderer) {
        return new __WEBPACK_IMPORTED_MODULE_1__component_loader_class__["a" /* ComponentLoader */](_viewContainerRef, _renderer, _elementRef, this._injector, this._componentFactoryResolver, this._ngZone, this._applicationRef, this._posService);
    };
    ComponentLoaderFactory = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ComponentFactoryResolver"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["NgZone"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Injector"],
            __WEBPACK_IMPORTED_MODULE_2__positioning__["b" /* PositioningService */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ApplicationRef"]])
    ], ComponentLoaderFactory);
    return ComponentLoaderFactory;
}());



/***/ }),
/* 14 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownState; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var BsDropdownState = (function () {
    function BsDropdownState() {
        var _this = this;
        this.direction = 'down';
        this.isOpenChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.isDisabledChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.toggleClick = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.dropdownMenu = new Promise(function (resolve) {
            _this.resolveDropdownMenu = resolve;
        });
    }
    BsDropdownState = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [])
    ], BsDropdownState);
    return BsDropdownState;
}());



/***/ }),
/* 15 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DatePickerInnerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__date_formatter__ = __webpack_require__(76);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/* tslint:disable:max-file-line-count */


// const MIN_DATE:Date = void 0;
// const MAX_DATE:Date = void 0;
// const DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
/*
 const KEYS = {
 13: 'enter',
 32: 'space',
 33: 'pageup',
 34: 'pagedown',
 35: 'end',
 36: 'home',
 37: 'left',
 38: 'up',
 39: 'right',
 40: 'down'
 };
 */
var DatePickerInnerComponent = (function () {
    function DatePickerInnerComponent() {
        this.selectionDone = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"](undefined);
        this.update = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"](false);
        this.activeDateChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"](undefined);
        this.stepDay = {};
        this.stepMonth = {};
        this.stepYear = {};
        this.modes = ['day', 'month', 'year'];
        this.dateFormatter = new __WEBPACK_IMPORTED_MODULE_1__date_formatter__["a" /* DateFormatter */]();
    }
    Object.defineProperty(DatePickerInnerComponent.prototype, "activeDate", {
        get: function () {
            return this._activeDate;
        },
        set: function (value) {
            this._activeDate = value;
        },
        enumerable: true,
        configurable: true
    });
    // todo: add formatter value to Date object
    DatePickerInnerComponent.prototype.ngOnInit = function () {
        // todo: use date for unique value
        this.uniqueId = 'datepicker-' + '-' + Math.floor(Math.random() * 10000);
        if (this.initDate) {
            this.activeDate = this.initDate;
            this.selectedDate = new Date(this.activeDate.valueOf());
            this.update.emit(this.activeDate);
        }
        else if (this.activeDate === undefined) {
            this.activeDate = new Date();
        }
    };
    // this.refreshView should be called here to reflect the changes on the fly
    // tslint:disable-next-line:no-unused-variable
    DatePickerInnerComponent.prototype.ngOnChanges = function (changes) {
        this.refreshView();
        this.checkIfActiveDateGotUpdated(changes['activeDate']);
    };
    // Check if activeDate has been update and then emit the activeDateChange with the new date
    DatePickerInnerComponent.prototype.checkIfActiveDateGotUpdated = function (activeDate) {
        if (activeDate && !activeDate.firstChange) {
            var previousValue = activeDate.previousValue;
            if (previousValue && previousValue instanceof Date && previousValue.getTime() !== activeDate.currentValue.getTime()) {
                this.activeDateChange.emit(this.activeDate);
            }
        }
    };
    DatePickerInnerComponent.prototype.setCompareHandler = function (handler, type) {
        if (type === 'day') {
            this.compareHandlerDay = handler;
        }
        if (type === 'month') {
            this.compareHandlerMonth = handler;
        }
        if (type === 'year') {
            this.compareHandlerYear = handler;
        }
    };
    DatePickerInnerComponent.prototype.compare = function (date1, date2) {
        if (date1 === undefined || date2 === undefined) {
            return undefined;
        }
        if (this.datepickerMode === 'day' && this.compareHandlerDay) {
            return this.compareHandlerDay(date1, date2);
        }
        if (this.datepickerMode === 'month' && this.compareHandlerMonth) {
            return this.compareHandlerMonth(date1, date2);
        }
        if (this.datepickerMode === 'year' && this.compareHandlerYear) {
            return this.compareHandlerYear(date1, date2);
        }
        return void 0;
    };
    DatePickerInnerComponent.prototype.setRefreshViewHandler = function (handler, type) {
        if (type === 'day') {
            this.refreshViewHandlerDay = handler;
        }
        if (type === 'month') {
            this.refreshViewHandlerMonth = handler;
        }
        if (type === 'year') {
            this.refreshViewHandlerYear = handler;
        }
    };
    DatePickerInnerComponent.prototype.refreshView = function () {
        if (this.datepickerMode === 'day' && this.refreshViewHandlerDay) {
            this.refreshViewHandlerDay();
        }
        if (this.datepickerMode === 'month' && this.refreshViewHandlerMonth) {
            this.refreshViewHandlerMonth();
        }
        if (this.datepickerMode === 'year' && this.refreshViewHandlerYear) {
            this.refreshViewHandlerYear();
        }
    };
    DatePickerInnerComponent.prototype.dateFilter = function (date, format) {
        return this.dateFormatter.format(date, format, this.locale);
    };
    DatePickerInnerComponent.prototype.isActive = function (dateObject) {
        if (this.compare(dateObject.date, this.activeDate) === 0) {
            this.activeDateId = dateObject.uid;
            return true;
        }
        return false;
    };
    DatePickerInnerComponent.prototype.createDateObject = function (date, format) {
        var dateObject = {};
        dateObject.date = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        dateObject.label = this.dateFilter(date, format);
        dateObject.selected = this.compare(date, this.selectedDate) === 0;
        dateObject.disabled = this.isDisabled(date);
        dateObject.current = this.compare(date, new Date()) === 0;
        dateObject.customClass = this.getCustomClassForDate(dateObject.date);
        return dateObject;
    };
    DatePickerInnerComponent.prototype.split = function (arr, size) {
        var arrays = [];
        while (arr.length > 0) {
            arrays.push(arr.splice(0, size));
        }
        return arrays;
    };
    // Fix a hard-reproducible bug with timezones
    // The bug depends on OS, browser, current timezone and current date
    // i.e.
    // var date = new Date(2014, 0, 1);
    // console.log(date.getFullYear(), date.getMonth(), date.getDate(),
    // date.getHours()); can result in "2013 11 31 23" because of the bug.
    DatePickerInnerComponent.prototype.fixTimeZone = function (date) {
        var hours = date.getHours();
        return new Date(date.getFullYear(), date.getMonth(), date.getDate(), hours === 23 ? hours + 2 : 0);
    };
    DatePickerInnerComponent.prototype.select = function (date, isManual) {
        if (isManual === void 0) { isManual = true; }
        if (this.datepickerMode === this.minMode) {
            if (!this.activeDate) {
                this.activeDate = new Date(0, 0, 0, 0, 0, 0, 0);
            }
            this.activeDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            if (isManual) {
                this.selectionDone.emit(this.activeDate);
            }
        }
        else {
            this.activeDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            if (isManual) {
                this.datepickerMode = this.modes[this.modes.indexOf(this.datepickerMode) - 1];
            }
        }
        this.selectedDate = new Date(this.activeDate.valueOf());
        this.update.emit(this.activeDate);
        this.refreshView();
    };
    DatePickerInnerComponent.prototype.move = function (direction) {
        var expectedStep;
        if (this.datepickerMode === 'day') {
            expectedStep = this.stepDay;
        }
        if (this.datepickerMode === 'month') {
            expectedStep = this.stepMonth;
        }
        if (this.datepickerMode === 'year') {
            expectedStep = this.stepYear;
        }
        if (expectedStep) {
            var year = this.activeDate.getFullYear() + direction * (expectedStep.years || 0);
            var month = this.activeDate.getMonth() + direction * (expectedStep.months || 0);
            this.activeDate = new Date(year, month, 1);
            this.refreshView();
            this.activeDateChange.emit(this.activeDate);
        }
    };
    DatePickerInnerComponent.prototype.toggleMode = function (direction) {
        direction = direction || 1;
        if ((this.datepickerMode === this.maxMode && direction === 1) ||
            (this.datepickerMode === this.minMode && direction === -1)) {
            return;
        }
        this.datepickerMode = this.modes[this.modes.indexOf(this.datepickerMode) + direction];
        this.refreshView();
    };
    DatePickerInnerComponent.prototype.getCustomClassForDate = function (date) {
        var _this = this;
        if (!this.customClass) {
            return '';
        }
        // todo: build a hash of custom classes, it will work faster
        var customClassObject = this.customClass
            .find(function (customClass) {
            return customClass.date.valueOf() === date.valueOf() &&
                customClass.mode === _this.datepickerMode;
        }, this);
        return customClassObject === undefined ? '' : customClassObject.clazz;
    };
    DatePickerInnerComponent.prototype.compareDateDisabled = function (date1Disabled, date2) {
        if (date1Disabled === undefined || date2 === undefined) {
            return undefined;
        }
        if (date1Disabled.mode === 'day' && this.compareHandlerDay) {
            return this.compareHandlerDay(date1Disabled.date, date2);
        }
        if (date1Disabled.mode === 'month' && this.compareHandlerMonth) {
            return this.compareHandlerMonth(date1Disabled.date, date2);
        }
        if (date1Disabled.mode === 'year' && this.compareHandlerYear) {
            return this.compareHandlerYear(date1Disabled.date, date2);
        }
        return undefined;
    };
    DatePickerInnerComponent.prototype.isDisabled = function (date) {
        var _this = this;
        var isDateDisabled = false;
        if (this.dateDisabled) {
            this.dateDisabled.forEach(function (disabledDate) {
                if (_this.compareDateDisabled(disabledDate, date) === 0) {
                    isDateDisabled = true;
                }
            });
        }
        return (isDateDisabled || (this.minDate && this.compare(date, this.minDate) < 0) ||
            (this.maxDate && this.compare(date, this.maxDate) > 0));
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "locale", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "datepickerMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerInnerComponent.prototype, "startingDay", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerInnerComponent.prototype, "yearRange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], DatePickerInnerComponent.prototype, "minDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], DatePickerInnerComponent.prototype, "maxDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "minMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "maxMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], DatePickerInnerComponent.prototype, "showWeeks", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "formatDay", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "formatMonth", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "formatYear", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "formatDayHeader", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "formatDayTitle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerInnerComponent.prototype, "formatMonthTitle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], DatePickerInnerComponent.prototype, "onlyCurrentMonth", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], DatePickerInnerComponent.prototype, "shortcutPropagation", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], DatePickerInnerComponent.prototype, "customClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerInnerComponent.prototype, "monthColLimit", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerInnerComponent.prototype, "yearColLimit", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], DatePickerInnerComponent.prototype, "dateDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], DatePickerInnerComponent.prototype, "initDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], DatePickerInnerComponent.prototype, "selectionDone", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], DatePickerInnerComponent.prototype, "update", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], DatePickerInnerComponent.prototype, "activeDateChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date),
        __metadata("design:paramtypes", [Date])
    ], DatePickerInnerComponent.prototype, "activeDate", null);
    DatePickerInnerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'datepicker-inner',
            template: "\n    <div *ngIf=\"datepickerMode\" class=\"well well-sm bg-faded p-a card\" role=\"application\" ><!--&lt;!&ndash;ng-keydown=\"keydown($event)\"&ndash;&gt;-->\n      <ng-content></ng-content>\n    </div>\n  "
        })
    ], DatePickerInnerComponent);
    return DatePickerInnerComponent;
}());



/***/ }),
/* 16 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["d"] = zeroFill;
/* harmony export (immutable) */ __webpack_exports__["c"] = mod;
/* harmony export (immutable) */ __webpack_exports__["a"] = absFloor;
/* harmony export (immutable) */ __webpack_exports__["b"] = createUTCDate;
function zeroFill(num, targetLength, forceSign) {
    var absNumber = "" + Math.abs(num);
    var zerosToFill = targetLength - absNumber.length;
    var sign = num >= 0;
    return (sign ? (forceSign ? '+' : '') : '-') +
        Math.pow(10, Math.max(0, zerosToFill)).toString().substr(1) + absNumber;
}
function mod(n, x) {
    return ((n % x) + x) % x;
}
function absFloor(number) {
    return number < 0
        ? Math.ceil(number) || 0
        : Math.floor(number);
}
function createUTCDate(y, m, d, h, M, s, ms) {
    var date = new Date(Date.UTC.apply(null, arguments));
    // the Date.UTC function remaps years 0-99 to 1900-1999
    if (y < 100 && y >= 0 && isFinite(date.getUTCFullYear())) {
        date.setUTCFullYear(y);
    }
    return date;
}


/***/ }),
/* 17 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["b"] = startOf;
/* harmony export (immutable) */ __webpack_exports__["a"] = endOf;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__date_setters__ = __webpack_require__(7);

function startOf(date, units) {
    var unit = getDateShift(units);
    return Object(__WEBPACK_IMPORTED_MODULE_0__date_setters__["b" /* setDate */])(date, unit);
}
function endOf(date, units) {
    var start = startOf(date, units);
    var shift = (_a = {}, _a[units] = 1, _a);
    var change = Object(__WEBPACK_IMPORTED_MODULE_0__date_setters__["c" /* shiftDate */])(start, shift);
    change.setMilliseconds(-1);
    return change;
    var _a;
}
function getDateShift(units) {
    var unit = {};
    switch (units) {
        case 'year':
            unit.month = 0;
        /* falls through */
        case 'month':
            unit.day = 1;
        /* falls through */
        case 'week':
        case 'day':
            unit.hour = 0;
        /* falls through */
        case 'hour':
            unit.minute = 0;
        /* falls through */
        case 'minute':
            unit.seconds = 0;
    }
    return unit;
}


/***/ }),
/* 18 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return ModalOptions; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsModalRef; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return modalConfigDefaults; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return ClassName; });
/* unused harmony export Selector */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return TransitionDurations; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return DISMISS_REASONS; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var ModalOptions = (function () {
    function ModalOptions() {
    }
    ModalOptions = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], ModalOptions);
    return ModalOptions;
}());

var BsModalRef = (function () {
    function BsModalRef() {
    }
    /**
     * Hides the modal
     */
    BsModalRef.prototype.hide = function () { };
    BsModalRef = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], BsModalRef);
    return BsModalRef;
}());

var modalConfigDefaults = {
    backdrop: true,
    keyboard: true,
    focus: true,
    show: false,
    ignoreBackdropClick: false,
    class: '',
    animated: true
};
var ClassName = {
    SCROLLBAR_MEASURER: 'modal-scrollbar-measure',
    BACKDROP: 'modal-backdrop',
    OPEN: 'modal-open',
    FADE: 'fade',
    IN: 'in',
    SHOW: 'show' // bs4
};
var Selector = {
    DIALOG: '.modal-dialog',
    DATA_TOGGLE: '[data-toggle="modal"]',
    DATA_DISMISS: '[data-dismiss="modal"]',
    FIXED_CONTENT: '.navbar-fixed-top, .navbar-fixed-bottom, .is-fixed'
};
var TransitionDurations = {
    MODAL: 300,
    BACKDROP: 150
};
var DISMISS_REASONS = {
    BACKRDOP: 'backdrop-click',
    ESC: 'esc'
};


/***/ }),
/* 19 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerActions; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var BsDatepickerActions = (function () {
    function BsDatepickerActions() {
    }
    BsDatepickerActions_1 = BsDatepickerActions;
    BsDatepickerActions.prototype.calculate = function () { return { type: BsDatepickerActions_1.CALCULATE }; };
    BsDatepickerActions.prototype.format = function () { return { type: BsDatepickerActions_1.FORMAT }; };
    BsDatepickerActions.prototype.flag = function () { return { type: BsDatepickerActions_1.FLAG }; };
    BsDatepickerActions.prototype.select = function (date) {
        return {
            type: BsDatepickerActions_1.SELECT,
            payload: date
        };
    };
    BsDatepickerActions.prototype.changeViewMode = function (event) {
        return {
            type: BsDatepickerActions_1.CHANGE_VIEWMODE,
            payload: event
        };
    };
    BsDatepickerActions.prototype.navigateTo = function (event) {
        return {
            type: BsDatepickerActions_1.NAVIGATE_TO,
            payload: event
        };
    };
    BsDatepickerActions.prototype.navigateStep = function (step) {
        return {
            type: BsDatepickerActions_1.NAVIGATE_OFFSET,
            payload: step
        };
    };
    BsDatepickerActions.prototype.setOptions = function (options) {
        return {
            type: BsDatepickerActions_1.SET_OPTIONS,
            payload: options
        };
    };
    // date range picker
    BsDatepickerActions.prototype.selectRange = function (value) {
        return {
            type: BsDatepickerActions_1.SELECT_RANGE,
            payload: value
        };
    };
    BsDatepickerActions.prototype.hoverDay = function (event) {
        return {
            type: BsDatepickerActions_1.HOVER,
            payload: event.isHovered ? event.cell.date : null
        };
    };
    BsDatepickerActions.prototype.minDate = function (date) {
        return {
            type: BsDatepickerActions_1.SET_MIN_DATE,
            payload: date
        };
    };
    BsDatepickerActions.prototype.maxDate = function (date) {
        return {
            type: BsDatepickerActions_1.SET_MAX_DATE,
            payload: date
        };
    };
    BsDatepickerActions.prototype.isDisabled = function (value) {
        return {
            type: BsDatepickerActions_1.SET_IS_DISABLED,
            payload: value
        };
    };
    BsDatepickerActions.CALCULATE = '[datepicker] calculate dates matrix';
    BsDatepickerActions.FORMAT = '[datepicker] format datepicker values';
    BsDatepickerActions.FLAG = '[datepicker] set flags';
    BsDatepickerActions.SELECT = '[datepicker] select date';
    BsDatepickerActions.NAVIGATE_OFFSET = '[datepicker] shift view date';
    BsDatepickerActions.NAVIGATE_TO = '[datepicker] change view date';
    BsDatepickerActions.SET_OPTIONS = '[datepicker] update render options';
    BsDatepickerActions.HOVER = '[datepicker] hover date';
    BsDatepickerActions.CHANGE_VIEWMODE = '[datepicker] switch view mode';
    BsDatepickerActions.SET_MIN_DATE = '[datepicker] set min date';
    BsDatepickerActions.SET_MAX_DATE = '[datepicker] set max date';
    BsDatepickerActions.SET_IS_DISABLED = '[datepicker] set is disabled';
    BsDatepickerActions.SELECT_RANGE = '[daterangepicker] select dates range';
    BsDatepickerActions = BsDatepickerActions_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], BsDatepickerActions);
    return BsDatepickerActions;
    var BsDatepickerActions_1;
}());



/***/ }),
/* 20 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return win; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return document; });
/* unused harmony export location */
/* unused harmony export gc */
/* unused harmony export performance */
/* unused harmony export Event */
/* unused harmony export MouseEvent */
/* unused harmony export KeyboardEvent */
/* unused harmony export EventTarget */
/* unused harmony export History */
/* unused harmony export Location */
/* unused harmony export EventListener */
/*tslint:disable */
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
/**
 * JS version of browser APIs. This library can only run in the browser.
 */
var win = typeof window !== 'undefined' && window || {};

var document = win.document;
var location = win.location;
var gc = win['gc'] ? function () { return win['gc'](); } : function () { return null; };
var performance = win['performance'] ? win['performance'] : null;
var Event = win['Event'];
var MouseEvent = win['MouseEvent'];
var KeyboardEvent = win['KeyboardEvent'];
var EventTarget = win['EventTarget'];
var History = win['History'];
var Location = win['Location'];
var EventListener = win['EventListener'];


/***/ }),
/* 21 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_21__;

/***/ }),
/* 22 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return ModalBackdropOptions; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ModalBackdropComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__modal_options_class__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__utils_utils_class__ = __webpack_require__(37);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




var ModalBackdropOptions = (function () {
    function ModalBackdropOptions(options) {
        this.animate = true;
        Object.assign(this, options);
    }
    return ModalBackdropOptions;
}());

/** This component will be added as background layout for modals if enabled */
var ModalBackdropComponent = (function () {
    function ModalBackdropComponent(element, renderer) {
        this._isShown = false;
        this.element = element;
        this.renderer = renderer;
    }
    Object.defineProperty(ModalBackdropComponent.prototype, "isAnimated", {
        get: function () {
            return this._isAnimated;
        },
        set: function (value) {
            this._isAnimated = value;
            // this.renderer.setElementClass(this.element.nativeElement, `${ClassName.FADE}`, value);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(ModalBackdropComponent.prototype, "isShown", {
        get: function () {
            return this._isShown;
        },
        set: function (value) {
            this._isShown = value;
            this.renderer.setElementClass(this.element.nativeElement, "" + __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].IN, value);
            if (!Object(__WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__["a" /* isBs3 */])()) {
                this.renderer.setElementClass(this.element.nativeElement, "" + __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].SHOW, value);
            }
        },
        enumerable: true,
        configurable: true
    });
    ModalBackdropComponent.prototype.ngOnInit = function () {
        if (this.isAnimated) {
            this.renderer.setElementClass(this.element.nativeElement, "" + __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].FADE, this.isAnimated);
            __WEBPACK_IMPORTED_MODULE_3__utils_utils_class__["a" /* Utils */].reflow(this.element.nativeElement);
        }
        this.isShown = true;
    };
    ModalBackdropComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-modal-backdrop',
            template: '',
            // tslint:disable-next-line
            host: { 'class': __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].BACKDROP }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"]])
    ], ModalBackdropComponent);
    return ModalBackdropComponent;
}());



/***/ }),
/* 23 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PaginationConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
// todo: split

/** Provides default values for Pagination and pager components */
var PaginationConfig = (function () {
    function PaginationConfig() {
        this.main = {
            maxSize: void 0,
            itemsPerPage: 10,
            boundaryLinks: false,
            directionLinks: true,
            firstText: 'First',
            previousText: 'Previous',
            nextText: 'Next',
            lastText: 'Last',
            pageBtnClass: '',
            rotate: true
        };
        this.pager = {
            itemsPerPage: 15,
            previousText: '« Previous',
            nextText: 'Next »',
            pageBtnClass: '',
            align: true
        };
    }
    PaginationConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], PaginationConfig);
    return PaginationConfig;
}());



/***/ }),
/* 24 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimepickerActions; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var TimepickerActions = (function () {
    function TimepickerActions() {
    }
    TimepickerActions_1 = TimepickerActions;
    TimepickerActions.prototype.writeValue = function (value) {
        return {
            type: TimepickerActions_1.WRITE_VALUE,
            payload: value
        };
    };
    TimepickerActions.prototype.changeHours = function (event) {
        return {
            type: TimepickerActions_1.CHANGE_HOURS,
            payload: event
        };
    };
    TimepickerActions.prototype.changeMinutes = function (event) {
        return {
            type: TimepickerActions_1.CHANGE_MINUTES,
            payload: event
        };
    };
    TimepickerActions.prototype.changeSeconds = function (event) {
        return {
            type: TimepickerActions_1.CHANGE_SECONDS,
            payload: event
        };
    };
    TimepickerActions.prototype.setTime = function (value) {
        return {
            type: TimepickerActions_1.SET_TIME_UNIT,
            payload: value
        };
    };
    TimepickerActions.prototype.updateControls = function (value) {
        return {
            type: TimepickerActions_1.UPDATE_CONTROLS,
            payload: value
        };
    };
    TimepickerActions.WRITE_VALUE = '[timepicker] write value from ng model';
    TimepickerActions.CHANGE_HOURS = '[timepicker] change hours';
    TimepickerActions.CHANGE_MINUTES = '[timepicker] change minutes';
    TimepickerActions.CHANGE_SECONDS = '[timepicker] change seconds';
    TimepickerActions.SET_TIME_UNIT = '[timepicker] set time unit';
    TimepickerActions.UPDATE_CONTROLS = '[timepicker] update controls';
    TimepickerActions = TimepickerActions_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], TimepickerActions);
    return TimepickerActions;
    var TimepickerActions_1;
}());



/***/ }),
/* 25 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimepickerConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

/** Provides default configuration values for timepicker */
var TimepickerConfig = (function () {
    function TimepickerConfig() {
        /** hours change step */
        this.hourStep = 1;
        /** hours change step */
        this.minuteStep = 5;
        /** seconds changes step */
        this.secondsStep = 10;
        /** if true works in 12H mode and displays AM/PM. If false works in 24H mode and hides AM/PM */
        this.showMeridian = true;
        /** meridian labels based on locale */
        this.meridians = ['AM', 'PM'];
        /** if true hours and minutes fields will be readonly */
        this.readonlyInput = false;
        /** if true scroll inside hours and minutes inputs will change time */
        this.mousewheel = true;
        /** if true up/down arrowkeys inside hours and minutes inputs will change time */
        this.arrowkeys = true;
        /** if true spinner arrows above and below the inputs will be shown */
        this.showSpinners = true;
        /** show seconds in timepicker */
        this.showSeconds = false;
    }
    TimepickerConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], TimepickerConfig);
    return TimepickerConfig;
}());



/***/ }),
/* 26 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TooltipConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

/** Default values provider for tooltip */
var TooltipConfig = (function () {
    function TooltipConfig() {
        /** tooltip placement, supported positions: 'top', 'bottom', 'left', 'right' */
        this.placement = 'top';
        /** array of event names which triggers tooltip opening */
        this.triggers = 'hover focus';
    }
    TooltipConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], TooltipConfig);
    return TooltipConfig;
}());



/***/ }),
/* 27 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PopoverConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

/**
 * Configuration service for the Popover directive.
 * You can inject this service, typically in your root component, and customize
 * the values of its properties in order to provide default values for all the
 * popovers used in the application.
 */
var PopoverConfig = (function () {
    function PopoverConfig() {
        /**
         * Placement of a popover. Accepts: "top", "bottom", "left", "right", "auto"
         */
        this.placement = 'top';
        /**
         * Specifies events that should trigger. Supports a space separated list of
         * event names.
         */
        this.triggers = 'click';
        this.outsideClick = false;
    }
    PopoverConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], PopoverConfig);
    return PopoverConfig;
}());



/***/ }),
/* 28 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = getStartingDayOfCalendar;
/* harmony export (immutable) */ __webpack_exports__["b"] = isMonthDisabled;
/* harmony export (immutable) */ __webpack_exports__["c"] = isYearDisabled;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_moment_utils_date_setters__ = __webpack_require__(7);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_compare__ = __webpack_require__(123);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_start_end_of__ = __webpack_require__(17);




function getStartingDayOfCalendar(date, options) {
    if (Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["i" /* isFirstDayOfWeek */])(date, options.firstDayOfWeek)) {
        return date;
    }
    var weekDay = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["b" /* getDayOfWeek */])(date);
    return Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_utils_date_setters__["c" /* shiftDate */])(date, { day: -weekDay });
}
function isMonthDisabled(date, min, max) {
    var minBound = min && Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_compare__["d" /* isSameOrBefore */])(Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_start_end_of__["a" /* endOf */])(date, 'month'), min, 'day');
    var maxBound = max && Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_compare__["c" /* isSameOrAfter */])(Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_start_end_of__["b" /* startOf */])(date, 'month'), max, 'day');
    return minBound || maxBound;
}
function isYearDisabled(date, min, max) {
    var minBound = min && Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_compare__["d" /* isSameOrBefore */])(Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_start_end_of__["a" /* endOf */])(date, 'year'), min, 'day');
    var maxBound = max && Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_compare__["c" /* isSameOrAfter */])(Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_start_end_of__["b" /* startOf */])(date, 'year'), max, 'day');
    return minBound || maxBound;
}


/***/ }),
/* 29 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsNavigationDirection; });
/** *************** */
// events
/** *************** */
var BsNavigationDirection;
(function (BsNavigationDirection) {
    BsNavigationDirection[BsNavigationDirection["UP"] = 0] = "UP";
    BsNavigationDirection[BsNavigationDirection["DOWN"] = 1] = "DOWN";
})(BsNavigationDirection || (BsNavigationDirection = {}));


/***/ }),
/* 30 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return CollapseModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__collapse_directive__ = __webpack_require__(65);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};


var CollapseModule = (function () {
    function CollapseModule() {
    }
    CollapseModule_1 = CollapseModule;
    CollapseModule.forRoot = function () {
        return { ngModule: CollapseModule_1, providers: [] };
    };
    CollapseModule = CollapseModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            declarations: [__WEBPACK_IMPORTED_MODULE_1__collapse_directive__["a" /* CollapseDirective */]],
            exports: [__WEBPACK_IMPORTED_MODULE_1__collapse_directive__["a" /* CollapseDirective */]]
        })
    ], CollapseModule);
    return CollapseModule;
    var CollapseModule_1;
}());



/***/ }),
/* 31 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AccordionComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__accordion_config__ = __webpack_require__(32);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


/** Displays collapsible content panels for presenting information in a limited amount of space. */
var AccordionComponent = (function () {
    function AccordionComponent(config) {
        this.groups = [];
        Object.assign(this, config);
    }
    AccordionComponent.prototype.closeOtherPanels = function (openGroup) {
        if (!this.closeOthers) {
            return;
        }
        this.groups.forEach(function (group) {
            if (group !== openGroup) {
                group.isOpen = false;
            }
        });
    };
    AccordionComponent.prototype.addGroup = function (group) {
        this.groups.push(group);
    };
    AccordionComponent.prototype.removeGroup = function (group) {
        var index = this.groups.indexOf(group);
        if (index !== -1) {
            this.groups.splice(index, 1);
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], AccordionComponent.prototype, "closeOthers", void 0);
    AccordionComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'accordion',
            template: "<ng-content></ng-content>",
            // tslint:disable-next-line
            host: {
                //'[.class.panel-group]': 'true',
                '[attr.aria-multiselectable]': 'closeOthers',
                role: 'tablist',
                class: 'panel-group',
                style: 'display: block'
            }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__accordion_config__["a" /* AccordionConfig */]])
    ], AccordionComponent);
    return AccordionComponent;
}());



/***/ }),
/* 32 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AccordionConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

/**
 * Configuration service, provides default values for the AccordionComponent.
 */
var AccordionConfig = (function () {
    function AccordionConfig() {
        /** Whether the other panels should be closed when a panel is opened */
        this.closeOthers = false;
    }
    AccordionConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], AccordionConfig);
    return AccordionConfig;
}());



/***/ }),
/* 33 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AlertConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var AlertConfig = (function () {
    function AlertConfig() {
        /** default alert type */
        this.type = 'warning';
        /** is alerts are dismissible by default */
        this.dismissible = false;
        /** default time before alert will dismiss */
        this.dismissOnTimeout = undefined;
    }
    AlertConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], AlertConfig);
    return AlertConfig;
}());



/***/ }),
/* 34 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = OnChange;
/*tslint:disable:no-invalid-this */
function OnChange(defaultValue) {
    var sufix = 'Change';
    return function OnChangeHandler(target, propertyKey) {
        var _key = " __" + propertyKey + "Value";
        Object.defineProperty(target, propertyKey, {
            get: function () { return this[_key]; },
            set: function (value) {
                var prevValue = this[_key];
                this[_key] = value;
                if (prevValue !== value && this[propertyKey + sufix]) {
                    this[propertyKey + sufix].emit(value);
                }
            }
        });
    };
}
/* tslint:enable */


/***/ }),
/* 35 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export Direction */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return CarouselComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils__ = __webpack_require__(36);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__carousel_config__ = __webpack_require__(38);
// todo: add animation
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/***
 * pause (not yet supported) (?string='hover') - event group name which pauses the cycling of the carousel, if hover pauses on mouseenter and resumes on mouseleave
 keyboard (not yet supported) (?boolean=true) - if false carousel will not react to keyboard events
 note: swiping not yet supported
 */
/****
 * Problems:
 * 1) if we set an active slide via model changes, .active class remains on a current slide.
 * 2) if we have only one slide, we shouldn't show prev/next nav buttons
 * 3) if first or last slide is active and noWrap is true, there should be "disabled" class on the nav buttons.
 * 4) default interval should be equal 5000
 */



var Direction;
(function (Direction) {
    Direction[Direction["UNKNOWN"] = 0] = "UNKNOWN";
    Direction[Direction["NEXT"] = 1] = "NEXT";
    Direction[Direction["PREV"] = 2] = "PREV";
})(Direction || (Direction = {}));
/**
 * Base element to create carousel
 */
var CarouselComponent = (function () {
    function CarouselComponent(config, ngZone) {
        this.ngZone = ngZone;
        /** Will be emitted when active slide has been changed. Part of two-way-bindable [(activeSlide)] property */
        this.activeSlideChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"](false);
        this._slides = new __WEBPACK_IMPORTED_MODULE_1__utils__["a" /* LinkedList */]();
        this.destroyed = false;
        Object.assign(this, config);
    }
    Object.defineProperty(CarouselComponent.prototype, "activeSlide", {
        get: function () {
            return this._currentActiveSlide;
        },
        /** Index of currently displayed slide(started for 0) */
        set: function (index) {
            if (this._slides.length && index !== this._currentActiveSlide) {
                this._select(index);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CarouselComponent.prototype, "interval", {
        /**
         * Delay of item cycling in milliseconds. If false, carousel won't cycle automatically.
         */
        get: function () {
            return this._interval;
        },
        set: function (value) {
            this._interval = value;
            this.restartTimer();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CarouselComponent.prototype, "slides", {
        get: function () {
            return this._slides.toArray();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(CarouselComponent.prototype, "isBs4", {
        get: function () {
            return !Object(__WEBPACK_IMPORTED_MODULE_1__utils__["e" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    CarouselComponent.prototype.ngOnDestroy = function () {
        this.destroyed = true;
    };
    /**
     * Adds new slide. If this slide is first in collection - set it as active and starts auto changing
     * @param slide
     */
    CarouselComponent.prototype.addSlide = function (slide) {
        this._slides.add(slide);
        if (this._slides.length === 1) {
            this._currentActiveSlide = void 0;
            this.activeSlide = 0;
            this.play();
        }
    };
    /**
     * Removes specified slide. If this slide is active - will roll to another slide
     * @param slide
     */
    CarouselComponent.prototype.removeSlide = function (slide) {
        var _this = this;
        var remIndex = this._slides.indexOf(slide);
        if (this._currentActiveSlide === remIndex) {
            // removing of active slide
            var nextSlideIndex_1 = void 0;
            if (this._slides.length > 1) {
                // if this slide last - will roll to first slide, if noWrap flag is FALSE or to previous, if noWrap is TRUE
                // in case, if this slide in middle of collection, index of next slide is same to removed
                nextSlideIndex_1 = !this.isLast(remIndex) ? remIndex :
                    this.noWrap ? remIndex - 1 : 0;
            }
            this._slides.remove(remIndex);
            // prevents exception with changing some value after checking
            setTimeout(function () {
                _this._select(nextSlideIndex_1);
            }, 0);
        }
        else {
            this._slides.remove(remIndex);
            var currentSlideIndex_1 = this.getCurrentSlideIndex();
            setTimeout(function () {
                // after removing, need to actualize index of current active slide
                _this._currentActiveSlide = currentSlideIndex_1;
                _this.activeSlideChange.emit(_this._currentActiveSlide);
            }, 0);
        }
    };
    /**
     * Rolling to next slide
     * @param force: {boolean} if true - will ignore noWrap flag
     */
    CarouselComponent.prototype.nextSlide = function (force) {
        if (force === void 0) { force = false; }
        this.activeSlide = this.findNextSlideIndex(Direction.NEXT, force);
    };
    /**
     * Rolling to previous slide
     * @param force: {boolean} if true - will ignore noWrap flag
     */
    CarouselComponent.prototype.previousSlide = function (force) {
        if (force === void 0) { force = false; }
        this.activeSlide = this.findNextSlideIndex(Direction.PREV, force);
    };
    /**
     * Rolling to specified slide
     * @param index: {number} index of slide, which must be shown
     */
    CarouselComponent.prototype.selectSlide = function (index) {
        this.activeSlide = index;
    };
    /**
     * Starts a auto changing of slides
     */
    CarouselComponent.prototype.play = function () {
        if (!this.isPlaying) {
            this.isPlaying = true;
            this.restartTimer();
        }
    };
    /**
     * Stops a auto changing of slides
     */
    CarouselComponent.prototype.pause = function () {
        if (!this.noPause) {
            this.isPlaying = false;
            this.resetTimer();
        }
    };
    /**
     * Finds and returns index of currently displayed slide
     * @returns {number}
     */
    CarouselComponent.prototype.getCurrentSlideIndex = function () {
        return this._slides.findIndex(function (slide) { return slide.active; });
    };
    /**
     * Defines, whether the specified index is last in collection
     * @param index
     * @returns {boolean}
     */
    CarouselComponent.prototype.isLast = function (index) {
        return index + 1 >= this._slides.length;
    };
    /**
     * Defines next slide index, depending of direction
     * @param direction: Direction(UNKNOWN|PREV|NEXT)
     * @param force: {boolean} if TRUE - will ignore noWrap flag, else will return undefined if next slide require wrapping
     * @returns {any}
     */
    CarouselComponent.prototype.findNextSlideIndex = function (direction, force) {
        var nextSlideIndex = 0;
        if (!force && (this.isLast(this.activeSlide) && direction !== Direction.PREV && this.noWrap)) {
            return void 0;
        }
        switch (direction) {
            case Direction.NEXT:
                // if this is last slide, not force, looping is disabled and need to going forward - select current slide, as a next
                nextSlideIndex = (!this.isLast(this._currentActiveSlide)) ? this._currentActiveSlide + 1 :
                    (!force && this.noWrap) ? this._currentActiveSlide : 0;
                break;
            case Direction.PREV:
                // if this is first slide, not force, looping is disabled and need to going backward - select current slide, as a next
                nextSlideIndex = (this._currentActiveSlide > 0) ? this._currentActiveSlide - 1 :
                    (!force && this.noWrap) ? this._currentActiveSlide : this._slides.length - 1;
                break;
            default:
                throw new Error('Unknown direction');
        }
        return nextSlideIndex;
    };
    /**
     * Sets a slide, which specified through index, as active
     * @param index
     * @private
     */
    CarouselComponent.prototype._select = function (index) {
        if (isNaN(index)) {
            this.pause();
            return;
        }
        var currentSlide = this._slides.get(this._currentActiveSlide);
        if (currentSlide) {
            currentSlide.active = false;
        }
        var nextSlide = this._slides.get(index);
        if (nextSlide) {
            this._currentActiveSlide = index;
            nextSlide.active = true;
            this.activeSlide = index;
            this.activeSlideChange.emit(index);
        }
    };
    /**
     * Starts loop of auto changing of slides
     */
    CarouselComponent.prototype.restartTimer = function () {
        var _this = this;
        this.resetTimer();
        var interval = +this.interval;
        if (!isNaN(interval) && interval > 0) {
            this.currentInterval = this.ngZone.runOutsideAngular(function () {
                return setInterval(function () {
                    var nInterval = +_this.interval;
                    _this.ngZone.run(function () {
                        if (_this.isPlaying && !isNaN(_this.interval) && nInterval > 0 && _this.slides.length) {
                            _this.nextSlide();
                        }
                        else {
                            _this.pause();
                        }
                    });
                }, interval);
            });
        }
    };
    /**
     * Stops loop of auto changing of slides
     */
    CarouselComponent.prototype.resetTimer = function () {
        if (this.currentInterval) {
            clearInterval(this.currentInterval);
            this.currentInterval = void 0;
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], CarouselComponent.prototype, "noWrap", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], CarouselComponent.prototype, "noPause", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], CarouselComponent.prototype, "activeSlideChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], CarouselComponent.prototype, "activeSlide", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], CarouselComponent.prototype, "interval", null);
    CarouselComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'carousel',
            template: "\n    <div (mouseenter)=\"pause()\" (mouseleave)=\"play()\" (mouseup)=\"play()\" class=\"carousel slide\">\n      <ol class=\"carousel-indicators\" *ngIf=\"slides.length > 1\">\n         <li *ngFor=\"let slidez of slides; let i = index;\" [class.active]=\"slidez.active === true\" (click)=\"selectSlide(i)\"></li>\n      </ol>\n      <div class=\"carousel-inner\"><ng-content></ng-content></div>\n      <a class=\"left carousel-control carousel-control-prev\" [class.disabled]=\"activeSlide === 0 && noWrap\" (click)=\"previousSlide()\" *ngIf=\"slides.length > 1\">\n        <span class=\"icon-prev carousel-control-prev-icon\" aria-hidden=\"true\"></span>\n        <span *ngIf=\"isBs4\" class=\"sr-only\">Previous</span>\n      </a>\n      <a class=\"right carousel-control carousel-control-next\" (click)=\"nextSlide()\"  [class.disabled]=\"isLast(activeSlide) && noWrap\" *ngIf=\"slides.length > 1\">\n        <span class=\"icon-next carousel-control-next-icon\" aria-hidden=\"true\"></span>\n        <span class=\"sr-only\">Next</span>\n      </a>\n    </div>\n  "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__carousel_config__["a" /* CarouselConfig */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["NgZone"]])
    ], CarouselComponent);
    return CarouselComponent;
}());



/***/ }),
/* 36 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__decorators__ = __webpack_require__(34);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_0__decorators__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__linked_list_class__ = __webpack_require__(129);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_1__linked_list_class__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_2__ng2_bootstrap_config__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__trigger_class__ = __webpack_require__(73);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_3__trigger_class__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__utils_class__ = __webpack_require__(37);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_4__utils_class__["a"]; });







/***/ }),
/* 37 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Utils; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__facade_browser__ = __webpack_require__(20);

var Utils = (function () {
    function Utils() {
    }
    Utils.reflow = function (element) {
        (function (bs) { return bs; })(element.offsetHeight);
    };
    // source: https://github.com/jquery/jquery/blob/master/src/css/var/getStyles.js
    Utils.getStyles = function (elem) {
        // Support: IE <=11 only, Firefox <=30 (#15098, #14150)
        // IE throws on elements created in popups
        // FF meanwhile throws on frame elements through "defaultView.getComputedStyle"
        var view = elem.ownerDocument.defaultView;
        if (!view || !view.opener) {
            view = __WEBPACK_IMPORTED_MODULE_0__facade_browser__["b" /* window */];
        }
        return view.getComputedStyle(elem);
    };
    return Utils;
}());



/***/ }),
/* 38 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return CarouselConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var CarouselConfig = (function () {
    function CarouselConfig() {
        /** Default interval of auto changing of slides */
        this.interval = 5000;
        /** Is loop of auto changing of slides can be paused */
        this.noPause = false;
        /** Is slides can wrap from the last to the first slide */
        this.noWrap = false;
    }
    CarouselConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], CarouselConfig);
    return CarouselConfig;
}());



/***/ }),
/* 39 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export dayOfYearFromWeeks */
/* harmony export (immutable) */ __webpack_exports__["a"] = weekOfYear;
/* unused harmony export weeksInYear */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__utils__ = __webpack_require__(16);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__year__ = __webpack_require__(40);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__day_of_year__ = __webpack_require__(78);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__utils_date_getters__ = __webpack_require__(1);
/**
 *
 * @param {number} year
 * @param {number} dow - start-of-first-week
 * @param {number} doy - start-of-year
 * @returns {number}
 */




function firstWeekOffset(year, dow, doy) {
    // first-week day -- which january is always in the first week (4 for iso, 1 for other)
    var fwd = 7 + dow - doy;
    // first-week day local weekday -- which local weekday is fwd
    var fwdlw = (7 + Object(__WEBPACK_IMPORTED_MODULE_0__utils__["b" /* createUTCDate */])(year, 0, fwd).getUTCDay() - dow) % 7;
    return -fwdlw + fwd - 1;
}
// https://en.wikipedia.org/wiki/ISO_week_date#Calculating_a_date_given_the_year.2C_week_number_and_weekday
function dayOfYearFromWeeks(year, week, weekday, dow, doy) {
    var localWeekday = (7 + weekday - dow) % 7;
    var weekOffset = firstWeekOffset(year, dow, doy);
    var dayOfYear = 1 + 7 * (week - 1) + localWeekday + weekOffset;
    var resYear;
    var resDayOfYear;
    if (dayOfYear <= 0) {
        resYear = year - 1;
        resDayOfYear = Object(__WEBPACK_IMPORTED_MODULE_1__year__["a" /* daysInYear */])(resYear) + dayOfYear;
    }
    else if (dayOfYear > Object(__WEBPACK_IMPORTED_MODULE_1__year__["a" /* daysInYear */])(year)) {
        resYear = year + 1;
        resDayOfYear = dayOfYear - Object(__WEBPACK_IMPORTED_MODULE_1__year__["a" /* daysInYear */])(year);
    }
    else {
        resYear = year;
        resDayOfYear = dayOfYear;
    }
    return {
        year: resYear,
        dayOfYear: resDayOfYear
    };
}
function weekOfYear(date, dow, doy) {
    var weekOffset = firstWeekOffset(Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["d" /* getFullYear */])(date), dow, doy);
    var week = Math.floor((Object(__WEBPACK_IMPORTED_MODULE_2__day_of_year__["a" /* getDayOfYear */])(date) - weekOffset - 1) / 7) + 1;
    var resWeek;
    var resYear;
    if (week < 1) {
        resYear = Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["d" /* getFullYear */])(date) - 1;
        resWeek = week + weeksInYear(resYear, dow, doy);
    }
    else if (week > weeksInYear(Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["d" /* getFullYear */])(date), dow, doy)) {
        resWeek = week - weeksInYear(Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["d" /* getFullYear */])(date), dow, doy);
        resYear = Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["d" /* getFullYear */])(date) + 1;
    }
    else {
        resYear = Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["d" /* getFullYear */])(date);
        resWeek = week;
    }
    return {
        week: resWeek,
        year: resYear
    };
}
function weeksInYear(year, dow, doy) {
    var weekOffset = firstWeekOffset(year, dow, doy);
    var weekOffsetNext = firstWeekOffset(year + 1, dow, doy);
    return (Object(__WEBPACK_IMPORTED_MODULE_1__year__["a" /* daysInYear */])(year) - weekOffset + weekOffsetNext) / 7;
}


/***/ }),
/* 40 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = daysInYear;
/* harmony export (immutable) */ __webpack_exports__["b"] = isLeapYear;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_date_getters__ = __webpack_require__(1);


// FORMATTING
function getYear(date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["d" /* getFullYear */])(date).toString();
}
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('Y', null, null, function (date) {
    var y = Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["d" /* getFullYear */])(date);
    return y <= 9999 ? '' + y : '+' + y;
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])(null, ['YY', 2], null, function (date) {
    return (Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["d" /* getFullYear */])(date) % 100).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])(null, ['YYYY', 4], null, getYear);
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])(null, ['YYYYY', 5], null, getYear);
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])(null, ['YYYYYY', 6, true], null, getYear);
function daysInYear(year) {
    return isLeapYear(year) ? 366 : 365;
}
function isLeapYear(year) {
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}


/***/ }),
/* 41 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DatepickerConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var DatepickerConfig = (function () {
    function DatepickerConfig() {
        this.locale = 'en';
        this.datepickerMode = 'day';
        this.startingDay = 0;
        this.yearRange = 20;
        this.minMode = 'day';
        this.maxMode = 'year';
        this.showWeeks = true;
        this.formatDay = 'DD';
        this.formatMonth = 'MMMM';
        this.formatYear = 'YYYY';
        this.formatDayHeader = 'dd';
        this.formatDayTitle = 'MMMM YYYY';
        this.formatMonthTitle = 'YYYY';
        this.onlyCurrentMonth = false;
        this.monthColLimit = 3;
        this.yearColLimit = 5;
        this.shortcutPropagation = false;
    }
    DatepickerConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], DatepickerConfig);
    return DatepickerConfig;
}());



/***/ }),
/* 42 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_dropdown_state__ = __webpack_require__(14);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var BsDropdownContainerComponent = (function () {
    function BsDropdownContainerComponent(_state, cd, _renderer, _element) {
        var _this = this;
        this._state = _state;
        this.cd = cd;
        this._renderer = _renderer;
        this.isOpen = false;
        this._subscription = _state.isOpenChange.subscribe(function (value) {
            _this.isOpen = value;
            var dropdown = _element.nativeElement.querySelector('.dropdown-menu');
            if (dropdown) {
                _this._renderer.setElementClass(dropdown, 'show', true);
                if (dropdown.classList.contains('dropdown-menu-right')) {
                    _this._renderer.setElementStyle(dropdown, 'left', 'auto');
                    _this._renderer.setElementStyle(dropdown, 'right', '0');
                }
                if (_this.direction === 'up') {
                    _this._renderer.setElementStyle(dropdown, 'top', 'auto');
                    _this._renderer.setElementStyle(dropdown, 'transform', 'translateY(-101%)');
                }
            }
            _this.cd.markForCheck();
            _this.cd.detectChanges();
        });
    }
    Object.defineProperty(BsDropdownContainerComponent.prototype, "direction", {
        get: function () {
            return this._state.direction;
        },
        enumerable: true,
        configurable: true
    });
    BsDropdownContainerComponent.prototype.ngOnDestroy = function () {
        this._subscription.unsubscribe();
    };
    BsDropdownContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-dropdown-container',
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush,
            host: {
                style: 'display:block;position: absolute;'
            },
            template: "\n    <div [class.dropup]=\"direction === 'up'\"\n         [class.dropdown]=\"direction === 'down'\"\n         [class.show]=\"isOpen\"\n         [class.open]=\"isOpen\"><ng-content></ng-content></div>\n  "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__bs_dropdown_state__["a" /* BsDropdownState */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectorRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]])
    ], BsDropdownContainerComponent);
    return BsDropdownContainerComponent;
}());



/***/ }),
/* 43 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

/** Default dropdown configuration */
var BsDropdownConfig = (function () {
    function BsDropdownConfig() {
        /** default dropdown auto closing behavior */
        this.autoClose = true;
    }
    BsDropdownConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], BsDropdownConfig);
    return BsDropdownConfig;
}());



/***/ }),
/* 44 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ModalContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__modal_options_class__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_modal_service__ = __webpack_require__(45);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__utils_ng2_bootstrap_config__ = __webpack_require__(3);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




var ModalContainerComponent = (function () {
    // @HostListener('window:focusin', ['$event'])
    // public enforceFocus($event:any): void {
    //   if (!(this._element.nativeElement === $event.target || this._element.nativeElement.contains($event.target))) {
    //     this._element.nativeElement.focus();
    //   }
    // }
    // @HostListener('focusout', ['$event'])
    // public preventFocusOut($event:any): void {
    //   if (!$event.relatedTarget) {
    //     this._element.nativeElement.focus();
    //   }
    // }
    function ModalContainerComponent(options, _element, bsModalService, _renderer) {
        this.bsModalService = bsModalService;
        this._renderer = _renderer;
        this.isShown = false;
        this.isModalHiding = false;
        this._element = _element;
        this.config = Object.assign({}, options);
    }
    ModalContainerComponent.prototype.onClick = function (event) {
        if (this.config.ignoreBackdropClick || this.config.backdrop === 'static' || event.target !== this._element.nativeElement) {
            return;
        }
        this.bsModalService.setDismissReason(__WEBPACK_IMPORTED_MODULE_1__modal_options_class__["c" /* DISMISS_REASONS */].BACKRDOP);
        this.hide();
    };
    ModalContainerComponent.prototype.onEsc = function () {
        if (this.config.keyboard && this.level === this.bsModalService.getModalsCount()) {
            this.bsModalService.setDismissReason(__WEBPACK_IMPORTED_MODULE_1__modal_options_class__["c" /* DISMISS_REASONS */].ESC);
            this.hide();
        }
    };
    ModalContainerComponent.prototype.ngOnInit = function () {
        var _this = this;
        if (this.isAnimated) {
            this._renderer.setElementClass(this._element.nativeElement, __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].FADE, true);
        }
        this._renderer.setElementStyle(this._element.nativeElement, 'display', 'block');
        setTimeout(function () {
            _this.isShown = true;
            _this._renderer.setElementClass(_this._element.nativeElement, Object(__WEBPACK_IMPORTED_MODULE_3__utils_ng2_bootstrap_config__["a" /* isBs3 */])() ? __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].IN : __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].SHOW, true);
        }, this.isAnimated ? __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["e" /* TransitionDurations */].BACKDROP : 0);
        if (document && document.body) {
            if (this.bsModalService.getModalsCount() === 1) {
                this.bsModalService.checkScrollbar();
                this.bsModalService.setScrollbar();
            }
            this._renderer.setElementClass(document.body, __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].OPEN, true);
        }
    };
    ModalContainerComponent.prototype.ngOnDestroy = function () {
        if (this.isShown) {
            this.hide();
        }
    };
    ModalContainerComponent.prototype.hide = function () {
        var _this = this;
        if (this.isModalHiding || !this.isShown) {
            return;
        }
        this.isModalHiding = true;
        this._renderer.setElementClass(this._element.nativeElement, Object(__WEBPACK_IMPORTED_MODULE_3__utils_ng2_bootstrap_config__["a" /* isBs3 */])() ? __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].IN : __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].SHOW, false);
        setTimeout(function () {
            _this.isShown = false;
            if (document && document.body && _this.bsModalService.getModalsCount() === 1) {
                _this._renderer.setElementClass(document.body, __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["b" /* ClassName */].OPEN, false);
            }
            _this.bsModalService.hide(_this.level);
            _this.isModalHiding = false;
        }, this.isAnimated ? __WEBPACK_IMPORTED_MODULE_1__modal_options_class__["e" /* TransitionDurations */].MODAL : 0);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('click', ['$event']),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [Object]),
        __metadata("design:returntype", void 0)
    ], ModalContainerComponent.prototype, "onClick", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('window:keydown.esc'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], ModalContainerComponent.prototype, "onEsc", null);
    ModalContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'modal-container',
            template: "\n    <div [class]=\"'modal-dialog' + (config.class ? ' ' + config.class : '')\" role=\"document\">\n      <div class=\"modal-content\"><ng-content></ng-content></div>\n    </div>\n  ",
            // tslint:disable-next-line
            host: {
                class: 'modal',
                role: 'dialog',
                tabindex: '-1'
            }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__modal_options_class__["d" /* ModalOptions */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_2__bs_modal_service__["a" /* BsModalService */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"]])
    ], ModalContainerComponent);
    return ModalContainerComponent;
}());



/***/ }),
/* 45 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsModalService; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__component_loader_component_loader_factory__ = __webpack_require__(13);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__modal_backdrop_component__ = __webpack_require__(22);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__modal_container_component__ = __webpack_require__(44);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__modal_options_class__ = __webpack_require__(18);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var BsModalService = (function () {
    function BsModalService(clf) {
        this.clf = clf;
        // constructor props
        this.config = __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["f" /* modalConfigDefaults */];
        this.onShow = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onShown = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onHide = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onHidden = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.isBodyOverflowing = false;
        this.originalBodyPadding = 0;
        this.scrollbarWidth = 0;
        this.modalsCount = 0;
        this.lastDismissReason = '';
        this.loaders = [];
        this._backdropLoader = this.clf.createLoader(null, null, null);
    }
    /** Shows a modal */
    BsModalService.prototype.show = function (content, config) {
        this.modalsCount++;
        this._createLoaders();
        this.config = Object.assign({}, __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["f" /* modalConfigDefaults */], config);
        this._showBackdrop();
        this.lastDismissReason = null;
        return this._showModal(content);
    };
    BsModalService.prototype.hide = function (level) {
        var _this = this;
        if (this.modalsCount === 1) {
            this._hideBackdrop();
            this.resetScrollbar();
        }
        this.modalsCount = this.modalsCount >= 1 ? this.modalsCount - 1 : 0;
        setTimeout(function () {
            _this._hideModal(level);
            _this.removeLoaders(level);
        }, this.config.animated ? __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["e" /* TransitionDurations */].BACKDROP : 0);
    };
    BsModalService.prototype._showBackdrop = function () {
        var isBackdropEnabled = this.config.backdrop || this.config.backdrop === 'static';
        var isBackdropInDOM = !this.backdropRef || !this.backdropRef.instance.isShown;
        if (this.modalsCount === 1) {
            this.removeBackdrop();
            if (isBackdropEnabled && isBackdropInDOM) {
                this._backdropLoader
                    .attach(__WEBPACK_IMPORTED_MODULE_2__modal_backdrop_component__["a" /* ModalBackdropComponent */])
                    .to('body')
                    .show({ isAnimated: this.config.animated });
                this.backdropRef = this._backdropLoader._componentRef;
            }
        }
    };
    BsModalService.prototype._hideBackdrop = function () {
        var _this = this;
        if (!this.backdropRef) {
            return;
        }
        this.backdropRef.instance.isShown = false;
        var duration = this.config.animated ? __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["e" /* TransitionDurations */].BACKDROP : 0;
        setTimeout(function () { return _this.removeBackdrop(); }, duration);
    };
    BsModalService.prototype._showModal = function (content) {
        var modalLoader = this.loaders[this.loaders.length - 1];
        var bsModalRef = new __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["a" /* BsModalRef */]();
        var modalContainerRef = modalLoader
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["d" /* ModalOptions */], useValue: this.config })
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["a" /* BsModalRef */], useValue: bsModalRef })
            .attach(__WEBPACK_IMPORTED_MODULE_3__modal_container_component__["a" /* ModalContainerComponent */])
            .to('body')
            .show({ content: content, isAnimated: this.config.animated });
        modalContainerRef.instance.level = this.getModalsCount();
        bsModalRef.hide = function () {
            modalContainerRef.instance.hide();
        };
        bsModalRef.content = modalLoader.getInnerComponent() || null;
        return bsModalRef;
    };
    BsModalService.prototype._hideModal = function (level) {
        var modalLoader = this.loaders[level - 1];
        if (modalLoader) {
            modalLoader.hide();
        }
    };
    BsModalService.prototype.getModalsCount = function () {
        return this.modalsCount;
    };
    BsModalService.prototype.setDismissReason = function (reason) {
        this.lastDismissReason = reason;
    };
    BsModalService.prototype.removeBackdrop = function () {
        this._backdropLoader.hide();
        this.backdropRef = null;
    };
    /** AFTER PR MERGE MODAL.COMPONENT WILL BE USING THIS CODE*/
    /** Scroll bar tricks */
    /** @internal */
    BsModalService.prototype.checkScrollbar = function () {
        this.isBodyOverflowing = document.body.clientWidth < window.innerWidth;
        this.scrollbarWidth = this.getScrollbarWidth();
    };
    BsModalService.prototype.setScrollbar = function () {
        if (!document) {
            return;
        }
        this.originalBodyPadding = parseInt(window.getComputedStyle(document.body).getPropertyValue('padding-right') || '0', 10);
        if (this.isBodyOverflowing) {
            document.body.style.paddingRight = this.originalBodyPadding + this.scrollbarWidth + "px";
        }
    };
    BsModalService.prototype.resetScrollbar = function () {
        document.body.style.paddingRight = this.originalBodyPadding + 'px';
    };
    // thx d.walsh
    BsModalService.prototype.getScrollbarWidth = function () {
        var scrollDiv = document.createElement('div');
        scrollDiv.className = __WEBPACK_IMPORTED_MODULE_4__modal_options_class__["b" /* ClassName */].SCROLLBAR_MEASURER;
        document.body.appendChild(scrollDiv);
        var scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth;
        document.body.removeChild(scrollDiv);
        return scrollbarWidth;
    };
    BsModalService.prototype._createLoaders = function () {
        var loader = this.clf.createLoader(null, null, null);
        this.copyEvent(loader.onBeforeShow, this.onShow);
        this.copyEvent(loader.onShown, this.onShown);
        this.copyEvent(loader.onBeforeHide, this.onHide);
        this.copyEvent(loader.onHidden, this.onHidden);
        this.loaders.push(loader);
    };
    BsModalService.prototype.removeLoaders = function (level) {
        this.loaders.splice(level - 1, 1);
        this.loaders.forEach(function (loader, i) {
            loader.instance.level = i + 1;
        });
    };
    BsModalService.prototype.copyEvent = function (from, to) {
        var _this = this;
        from.subscribe(function () {
            to.emit(_this.lastDismissReason);
        });
    };
    BsModalService = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__component_loader_component_loader_factory__["a" /* ComponentLoaderFactory */]])
    ], BsModalService);
    return BsModalService;
}());



/***/ }),
/* 46 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ProgressDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

// todo: progress element conflict with bootstrap.css
// todo: need hack: replace host element with div
var ProgressDirective = (function () {
    function ProgressDirective() {
        this.addClass = true;
        this.bars = [];
        this._max = 100;
    }
    Object.defineProperty(ProgressDirective.prototype, "max", {
        /** maximum total value of progress element */
        get: function () {
            return this._max;
        },
        set: function (v) {
            this._max = v;
            this.bars.forEach(function (bar) {
                bar.recalculatePercentage();
            });
        },
        enumerable: true,
        configurable: true
    });
    ProgressDirective.prototype.addBar = function (bar) {
        if (!this.animate) {
            bar.transition = 'none';
        }
        this.bars.push(bar);
    };
    ProgressDirective.prototype.removeBar = function (bar) {
        this.bars.splice(this.bars.indexOf(bar), 1);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], ProgressDirective.prototype, "animate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('attr.max'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], ProgressDirective.prototype, "max", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.progress'),
        __metadata("design:type", Boolean)
    ], ProgressDirective.prototype, "addClass", void 0);
    ProgressDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: 'bs-progress, [progress]' })
    ], ProgressDirective);
    return ProgressDirective;
}());



/***/ }),
/* 47 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ProgressbarConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var ProgressbarConfig = (function () {
    function ProgressbarConfig() {
        /** if `true` changing value of progress bar will be animated (note: not supported by Bootstrap 4) */
        this.animate = true;
        /** maximum total value of progress element */
        this.max = 100;
    }
    ProgressbarConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], ProgressbarConfig);
    return ProgressbarConfig;
}());



/***/ }),
/* 48 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DraggableItemService; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_Subject__ = __webpack_require__(143);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_Subject___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_rxjs_Subject__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};


var DraggableItemService = (function () {
    function DraggableItemService() {
        this.onCapture = new __WEBPACK_IMPORTED_MODULE_1_rxjs_Subject__["Subject"]();
    }
    DraggableItemService.prototype.dragStart = function (item) {
        this.draggableItem = item;
    };
    DraggableItemService.prototype.getItem = function () {
        return this.draggableItem;
    };
    DraggableItemService.prototype.captureItem = function (overZoneIndex, newIndex) {
        if (this.draggableItem.overZoneIndex !== overZoneIndex) {
            this.draggableItem.lastZoneIndex = this.draggableItem.overZoneIndex;
            this.draggableItem.overZoneIndex = overZoneIndex;
            this.onCapture.next(this.draggableItem);
            this.draggableItem = Object.assign({}, this.draggableItem, { overZoneIndex: overZoneIndex, i: newIndex });
        }
        return this.draggableItem;
    };
    DraggableItemService.prototype.onCaptureItem = function () {
        return this.onCapture;
    };
    DraggableItemService = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], DraggableItemService);
    return DraggableItemService;
}());



/***/ }),
/* 49 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TabDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tabset_component__ = __webpack_require__(50);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var TabDirective = (function () {
    function TabDirective(tabset, elementRef, renderer) {
        this.elementRef = elementRef;
        this.renderer = renderer;
        /** fired when tab became active, $event:Tab equals to selected instance of Tab component */
        this.select = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired when tab became inactive, $event:Tab equals to deselected instance of Tab component */
        this.deselect = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired before tab will be removed, $event:Tab equals to instance of removed tab */
        this.removed = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.addClass = true;
        this.tabset = tabset;
        this.tabset.addTab(this);
    }
    Object.defineProperty(TabDirective.prototype, "customClass", {
        /** if set, will be added to the tab's class atribute */
        get: function () {
            return this._customClass;
        },
        set: function (customClass) {
            if (this._customClass && this._customClass !== customClass) {
                this.renderer.setElementClass(this.elementRef.nativeElement, this._customClass, false);
            }
            this._customClass = customClass;
            if (this._customClass) {
                this.renderer.setElementClass(this.elementRef.nativeElement, this._customClass, true);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TabDirective.prototype, "active", {
        /** tab active state toggle */
        get: function () {
            return this._active;
        },
        set: function (active) {
            var _this = this;
            if (this._active === active) {
                return;
            }
            if (this.disabled && active || !active) {
                if (this._active && !active) {
                    this.deselect.emit(this);
                    this._active = active;
                }
                return;
            }
            this._active = active;
            this.select.emit(this);
            this.tabset.tabs.forEach(function (tab) {
                if (tab !== _this) {
                    tab.active = false;
                }
            });
        },
        enumerable: true,
        configurable: true
    });
    TabDirective.prototype.ngOnInit = function () {
        this.removable = this.removable;
    };
    TabDirective.prototype.ngOnDestroy = function () {
        this.tabset.removeTab(this, { reselect: false, emit: false });
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TabDirective.prototype, "heading", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TabDirective.prototype, "id", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TabDirective.prototype, "disabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TabDirective.prototype, "removable", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String),
        __metadata("design:paramtypes", [String])
    ], TabDirective.prototype, "customClass", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.active'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TabDirective.prototype, "active", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TabDirective.prototype, "select", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TabDirective.prototype, "deselect", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TabDirective.prototype, "removed", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.tab-pane'),
        __metadata("design:type", Boolean)
    ], TabDirective.prototype, "addClass", void 0);
    TabDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: 'tab, [tab]' }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__tabset_component__["a" /* TabsetComponent */], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"]])
    ], TabDirective);
    return TabDirective;
}());



/***/ }),
/* 50 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TabsetComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tabset_config__ = __webpack_require__(51);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


// todo: add active event to tab
// todo: fix? mixing static and dynamic tabs position tabs in order of creation
var TabsetComponent = (function () {
    function TabsetComponent(config) {
        this.clazz = true;
        this.tabs = [];
        this.classMap = {};
        Object.assign(this, config);
    }
    Object.defineProperty(TabsetComponent.prototype, "vertical", {
        /** if true tabs will be placed vertically */
        get: function () {
            return this._vertical;
        },
        set: function (value) {
            this._vertical = value;
            this.setClassMap();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TabsetComponent.prototype, "justified", {
        /** if true tabs fill the container and have a consistent width */
        get: function () {
            return this._justified;
        },
        set: function (value) {
            this._justified = value;
            this.setClassMap();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TabsetComponent.prototype, "type", {
        /** navigation context class: 'tabs' or 'pills' */
        get: function () {
            return this._type;
        },
        set: function (value) {
            this._type = value;
            this.setClassMap();
        },
        enumerable: true,
        configurable: true
    });
    TabsetComponent.prototype.ngOnDestroy = function () {
        this.isDestroyed = true;
    };
    TabsetComponent.prototype.addTab = function (tab) {
        this.tabs.push(tab);
        tab.active = this.tabs.length === 1 && tab.active !== false;
    };
    TabsetComponent.prototype.removeTab = function (tab, options) {
        if (options === void 0) { options = { reselect: true, emit: true }; }
        var index = this.tabs.indexOf(tab);
        if (index === -1 || this.isDestroyed) {
            return;
        }
        // Select a new tab if the tab to be removed is selected and not destroyed
        if (options.reselect && tab.active && this.hasAvailableTabs(index)) {
            var newActiveIndex = this.getClosestTabIndex(index);
            this.tabs[newActiveIndex].active = true;
        }
        if (options.emit) {
            tab.removed.emit(tab);
        }
        this.tabs.splice(index, 1);
        if (tab.elementRef.nativeElement.parentNode) {
            tab.elementRef.nativeElement.parentNode.removeChild(tab.elementRef.nativeElement);
        }
    };
    TabsetComponent.prototype.getClosestTabIndex = function (index) {
        var tabsLength = this.tabs.length;
        if (!tabsLength) {
            return -1;
        }
        for (var step = 1; step <= tabsLength; step += 1) {
            var prevIndex = index - step;
            var nextIndex = index + step;
            if (this.tabs[prevIndex] && !this.tabs[prevIndex].disabled) {
                return prevIndex;
            }
            if (this.tabs[nextIndex] && !this.tabs[nextIndex].disabled) {
                return nextIndex;
            }
        }
        return -1;
    };
    TabsetComponent.prototype.hasAvailableTabs = function (index) {
        var tabsLength = this.tabs.length;
        if (!tabsLength) {
            return false;
        }
        for (var i = 0; i < tabsLength; i += 1) {
            if (!this.tabs[i].disabled && i !== index) {
                return true;
            }
        }
        return false;
    };
    TabsetComponent.prototype.setClassMap = function () {
        this.classMap = (_a = {
                'nav-stacked': this.vertical,
                'flex-column': this.vertical,
                'nav-justified': this.justified
            },
            _a["nav-" + this.type] = true,
            _a);
        var _a;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TabsetComponent.prototype, "vertical", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TabsetComponent.prototype, "justified", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String),
        __metadata("design:paramtypes", [String])
    ], TabsetComponent.prototype, "type", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.tab-container'),
        __metadata("design:type", Boolean)
    ], TabsetComponent.prototype, "clazz", void 0);
    TabsetComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'tabset',
            template: "\n    <ul class=\"nav\" [ngClass]=\"classMap\" (click)=\"$event.preventDefault()\">\n        <li *ngFor=\"let tabz of tabs\" [ngClass]=\"['nav-item', tabz.customClass || '']\"\n          [class.active]=\"tabz.active\" [class.disabled]=\"tabz.disabled\">\n          <a href=\"javascript:void(0);\" class=\"nav-link\"\n            [class.active]=\"tabz.active\" [class.disabled]=\"tabz.disabled\"\n            (click)=\"tabz.active = true\">\n            <span [ngTransclude]=\"tabz.headingRef\">{{tabz.heading}}</span>\n            <span *ngIf=\"tabz.removable\">\n              <span (click)=\"$event.preventDefault(); removeTab(tabz);\" class=\"glyphicon glyphicon-remove-circle\"></span>\n            </span>\n          </a>\n        </li>\n    </ul>\n    <div class=\"tab-content\">\n      <ng-content></ng-content>\n    </div>\n  "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__tabset_config__["a" /* TabsetConfig */]])
    ], TabsetComponent);
    return TabsetComponent;
}());



/***/ }),
/* 51 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TabsetConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var TabsetConfig = (function () {
    function TabsetConfig() {
        /** provides default navigation context class: 'tabs' or 'pills' */
        this.type = 'tabs';
    }
    TabsetConfig = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], TabsetConfig);
    return TabsetConfig;
}());



/***/ }),
/* 52 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimepickerStore; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__timepicker_reducer__ = __webpack_require__(144);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_BehaviorSubject__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_BehaviorSubject___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_BehaviorSubject__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__mini_ngrx_store_class__ = __webpack_require__(109);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__mini_ngrx_state_class__ = __webpack_require__(111);
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var TimepickerStore = (function (_super) {
    __extends(TimepickerStore, _super);
    function TimepickerStore() {
        var _this = this;
        var _dispatcher = new __WEBPACK_IMPORTED_MODULE_2_rxjs_BehaviorSubject__["BehaviorSubject"]({ type: '[mini-ngrx] dispatcher init' });
        var state = new __WEBPACK_IMPORTED_MODULE_4__mini_ngrx_state_class__["a" /* MiniState */](__WEBPACK_IMPORTED_MODULE_1__timepicker_reducer__["a" /* initialState */], _dispatcher, __WEBPACK_IMPORTED_MODULE_1__timepicker_reducer__["b" /* timepickerReducer */]);
        _this = _super.call(this, _dispatcher, __WEBPACK_IMPORTED_MODULE_1__timepicker_reducer__["b" /* timepickerReducer */], state) || this;
        return _this;
    }
    TimepickerStore = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [])
    ], TimepickerStore);
    return TimepickerStore;
}(__WEBPACK_IMPORTED_MODULE_3__mini_ngrx_store_class__["a" /* MiniStore */]));



/***/ }),
/* 53 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["c"] = isValidDate;
/* unused harmony export toNumber */
/* unused harmony export isNumber */
/* unused harmony export parseHours */
/* unused harmony export parseMinutes */
/* unused harmony export parseSeconds */
/* harmony export (immutable) */ __webpack_exports__["e"] = parseTime;
/* harmony export (immutable) */ __webpack_exports__["a"] = changeTime;
/* harmony export (immutable) */ __webpack_exports__["f"] = setTime;
/* unused harmony export createDate */
/* harmony export (immutable) */ __webpack_exports__["d"] = padNumber;
/* harmony export (immutable) */ __webpack_exports__["b"] = isInputValid;
var dex = 10;
var hoursPerDay = 24;
var hoursPerDayHalf = 12;
var minutesPerHour = 60;
var secondsPerMinute = 60;
function isValidDate(value) {
    if (!value) {
        return false;
    }
    if (value instanceof Date && isNaN(value.getHours())) {
        return false;
    }
    if (typeof value === 'string') {
        return isValidDate(new Date(value));
    }
    return true;
}
function toNumber(value) {
    if (typeof value === 'number') {
        return value;
    }
    return parseInt(value, dex);
}
function isNumber(value) {
    return !isNaN(toNumber(value));
}
function parseHours(value, isPM) {
    if (isPM === void 0) { isPM = false; }
    var hour = toNumber(value);
    if (isNaN(hour) || hour < 0 || hour > (isPM ? hoursPerDayHalf : hoursPerDay)) {
        return NaN;
    }
    return hour;
}
function parseMinutes(value) {
    var minute = toNumber(value);
    if (isNaN(minute) || minute < 0 || minute > minutesPerHour) {
        return NaN;
    }
    return minute;
}
function parseSeconds(value) {
    var seconds = toNumber(value);
    if (isNaN(seconds) || seconds < 0 || seconds > secondsPerMinute) {
        return NaN;
    }
    return seconds;
}
function parseTime(value) {
    if (typeof value === 'string') {
        return new Date(value);
    }
    return value;
}
function changeTime(value, diff) {
    if (!value) {
        return changeTime(createDate(new Date(), 0, 0, 0), diff);
    }
    var hour = value.getHours();
    var minutes = value.getMinutes();
    var seconds = value.getSeconds();
    if (diff.hour) {
        hour = (hour + toNumber(diff.hour)) % hoursPerDay;
        if (hour < 0) {
            hour += hoursPerDay;
        }
    }
    if (diff.minute) {
        minutes = (minutes + toNumber(diff.minute));
    }
    if (diff.seconds) {
        seconds = (seconds + toNumber(diff.seconds));
    }
    return createDate(value, hour, minutes, seconds);
}
function setTime(value, opts) {
    var hour = parseHours(opts.hour);
    var minute = parseMinutes(opts.minute);
    var seconds = parseSeconds(opts.seconds) || 0;
    if (opts.isPM) {
        hour += hoursPerDayHalf;
    }
    // fixme: unreachable code, value is mandatory
    if (!value) {
        if (!isNaN(hour) && !isNaN(minute)) {
            return createDate(new Date(), hour, minute, seconds);
        }
        return value;
    }
    if (isNaN(hour) || isNaN(minute)) {
        return value;
    }
    return createDate(value, hour, minute, seconds);
}
function createDate(value, hours, minutes, seconds) {
    // fixme: unreachable code, value is mandatory
    var _value = value || new Date();
    return new Date(_value.getFullYear(), _value.getMonth(), _value.getDate(), hours, minutes, seconds, _value.getMilliseconds());
}
function padNumber(value) {
    var _value = value.toString();
    if (_value.length > 1) {
        return _value;
    }
    return "0" + _value;
}
function isInputValid(hours, minutes, seconds, isPM) {
    if (seconds === void 0) { seconds = '0'; }
    if (isNaN(parseHours(hours, isPM)) || isNaN(parseMinutes(minutes)) || isNaN(parseSeconds(seconds))) {
        return false;
    }
    return true;
}


/***/ }),
/* 54 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_54__;

/***/ }),
/* 55 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TooltipContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tooltip_config__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__ = __webpack_require__(3);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var TooltipContainerComponent = (function () {
    function TooltipContainerComponent(config) {
        Object.assign(this, config);
    }
    Object.defineProperty(TooltipContainerComponent.prototype, "isBs3", {
        get: function () {
            return Object(__WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    TooltipContainerComponent.prototype.ngAfterViewInit = function () {
        this.classMap = { in: false, fade: false };
        this.classMap[this.placement] = true;
        this.classMap['tooltip-' + this.placement] = true;
        this.classMap.in = true;
        if (this.animation) {
            this.classMap.fade = true;
        }
        if (this.containerClass) {
            this.classMap[this.containerClass] = true;
        }
    };
    TooltipContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-tooltip-container',
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush,
            // tslint:disable-next-line
            host: {
                '[class]': '"tooltip in tooltip-" + placement + " " + "bs-tooltip-" + placement + " " + placement + " " + containerClass',
                '[class.show]': '!isBs3',
                role: 'tooltip'
            },
            styles: ["    \n    :host.tooltip {\n      display: block;\n    }\n    :host.bs-tooltip-top .arrow, :host.bs-tooltip-bottom .arrow {\n      left: calc(50% - 2.5px);\n    }\n    :host.bs-tooltip-left .arrow, :host.bs-tooltip-right .arrow {\n      top: calc(50% - 2.5px);\n    }\n  "],
            template: "\n    <div class=\"tooltip-arrow arrow\"></div>\n    <div class=\"tooltip-inner\"><ng-content></ng-content></div>\n    "
            // template: `<div class="tooltip" role="tooltip"
            //    [ngStyle]="{top: top, left: left, display: display}"
            //    [ngClass]="classMap">
            //     <div class="tooltip-arrow"></div>
            //     <div class="tooltip-inner"
            //          *ngIf="htmlContent && !isTemplate"
            //          innerHtml="{{htmlContent}}">
            //     </div>
            //     <div class="tooltip-inner"
            //          *ngIf="htmlContent && isTemplate">
            //       <template [ngTemplateOutlet]="htmlContent"
            //                 [ngOutletContext]="{model: context}">
            //       </template>
            //     </div>
            //     <div class="tooltip-inner"
            //          *ngIf="content">
            //       {{content}}
            //     </div>
            //   </div>`
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__tooltip_config__["a" /* TooltipConfig */]])
    ], TooltipContainerComponent);
    return TooltipContainerComponent;
}());



/***/ }),
/* 56 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TypeaheadContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__typeahead_utils__ = __webpack_require__(57);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var TypeaheadContainerComponent = (function () {
    function TypeaheadContainerComponent(element) {
        this.isFocused = false;
        this._matches = [];
        this.element = element;
    }
    Object.defineProperty(TypeaheadContainerComponent.prototype, "isBs4", {
        get: function () {
            return !Object(__WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeaheadContainerComponent.prototype, "active", {
        get: function () {
            return this._active;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeaheadContainerComponent.prototype, "matches", {
        get: function () {
            return this._matches;
        },
        set: function (value) {
            this._matches = value;
            if (this._matches.length > 0) {
                this._active = this._matches[0];
                if (this._active.isHeader()) {
                    this.nextActiveMatch();
                }
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeaheadContainerComponent.prototype, "optionsListTemplate", {
        get: function () {
            return this.parent ? this.parent.optionsListTemplate : undefined;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TypeaheadContainerComponent.prototype, "itemTemplate", {
        get: function () {
            return this.parent ? this.parent.typeaheadItemTemplate : undefined;
        },
        enumerable: true,
        configurable: true
    });
    TypeaheadContainerComponent.prototype.selectActiveMatch = function () {
        this.selectMatch(this._active);
    };
    TypeaheadContainerComponent.prototype.prevActiveMatch = function () {
        var index = this.matches.indexOf(this._active);
        this._active = this.matches[index - 1 < 0
            ? this.matches.length - 1
            : index - 1];
        if (this._active.isHeader()) {
            this.prevActiveMatch();
        }
    };
    TypeaheadContainerComponent.prototype.nextActiveMatch = function () {
        var index = this.matches.indexOf(this._active);
        this._active = this.matches[index + 1 > this.matches.length - 1
            ? 0
            : index + 1];
        if (this._active.isHeader()) {
            this.nextActiveMatch();
        }
    };
    TypeaheadContainerComponent.prototype.selectActive = function (value) {
        this.isFocused = true;
        this._active = value;
    };
    TypeaheadContainerComponent.prototype.hightlight = function (match, query) {
        var itemStr = match.value;
        var itemStrHelper = (this.parent && this.parent.typeaheadLatinize
            ? Object(__WEBPACK_IMPORTED_MODULE_2__typeahead_utils__["b" /* latinize */])(itemStr)
            : itemStr).toLowerCase();
        var startIdx;
        var tokenLen;
        // Replaces the capture string with the same string inside of a "strong" tag
        if (typeof query === 'object') {
            var queryLen = query.length;
            for (var i = 0; i < queryLen; i += 1) {
                // query[i] is already latinized and lower case
                startIdx = itemStrHelper.indexOf(query[i]);
                tokenLen = query[i].length;
                if (startIdx >= 0 && tokenLen > 0) {
                    itemStr = itemStr.substring(0, startIdx) + '<strong>' + itemStr.substring(startIdx, startIdx + tokenLen) + '</strong>' + itemStr.substring(startIdx + tokenLen);
                    itemStrHelper = itemStrHelper.substring(0, startIdx) + '        ' + ' '.repeat(tokenLen) + '         ' + itemStrHelper.substring(startIdx + tokenLen);
                }
            }
        }
        else if (query) {
            // query is already latinized and lower case
            startIdx = itemStrHelper.indexOf(query);
            tokenLen = query.length;
            if (startIdx >= 0 && tokenLen > 0) {
                itemStr = itemStr.substring(0, startIdx) + '<strong>' + itemStr.substring(startIdx, startIdx + tokenLen) + '</strong>' + itemStr.substring(startIdx + tokenLen);
            }
        }
        return itemStr;
    };
    TypeaheadContainerComponent.prototype.focusLost = function () {
        this.isFocused = false;
    };
    TypeaheadContainerComponent.prototype.isActive = function (value) {
        return this._active === value;
    };
    TypeaheadContainerComponent.prototype.selectMatch = function (value, e) {
        var _this = this;
        if (e === void 0) { e = void 0; }
        if (e) {
            e.stopPropagation();
            e.preventDefault();
        }
        this.parent.changeModel(value);
        setTimeout(function () {
            return _this.parent.typeaheadOnSelect.emit(value);
        }, 0);
        return false;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('mouseleave'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('blur'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], TypeaheadContainerComponent.prototype, "focusLost", null);
    TypeaheadContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'typeahead-container',
            // tslint:disable-next-line
            template: "\n<!-- inject options list template -->\n<template [ngTemplateOutlet]=\"optionsListTemplate || (isBs4 ? bs4Template : bs3Template)\"\n  [ngOutletContext]=\"{matches:matches, itemTemplate:itemTemplate, query:query}\"></template>\n\n<!-- default options item template -->\n<template #bsItemTemplate let-match=\"match\" let-query=\"query\"><span [innerHtml]=\"hightlight(match, query)\"></span></template>\n\n<!-- Bootstrap 3 options list template -->\n<template #bs3Template>\n<ul class=\"dropdown-menu\">\n  <template ngFor let-match let-i=\"index\" [ngForOf]=\"matches\">\n    <li *ngIf=\"match.isHeader()\" class=\"dropdown-header\">{{match}}</li>\n    <li *ngIf=\"!match.isHeader()\" [class.active]=\"isActive(match)\" (mouseenter)=\"selectActive(match)\">\n      <a href=\"#\" (click)=\"selectMatch(match, $event)\" tabindex=\"-1\">\n        <template [ngTemplateOutlet]=\"itemTemplate || bsItemTemplate\" \n          [ngOutletContext]=\"{item:match.item, index:i, match:match, query:query}\"></template>\n      </a>\n    </li>\n  </template>\n</ul>\n</template>\n\n<!-- Bootstrap 4 options list template -->\n<template #bs4Template >\n<template ngFor let-match let-i=\"index\" [ngForOf]=\"matches\">\n   <h6 *ngIf=\"match.isHeader()\" class=\"dropdown-header\">{{match}}</h6>\n   <template [ngIf]=\"!match.isHeader()\">\n      <button\n        class=\"dropdown-item\"\n        (click)=\"selectMatch(match, $event)\"\n        (mouseenter)=\"selectActive(match)\"\n        [class.active]=\"isActive(match)\">\n          <template [ngTemplateOutlet]=\"itemTemplate || bsItemTemplate\" \n            [ngOutletContext]=\"{item:match.item, index:i, match:match, query:query}\"></template>\n      </button>\n  </template>\n</template>\n</template>\n",
            // tslint:disable
            host: {
                'class': 'dropdown open',
                '[class.dropdown-menu]': 'isBs4',
                '[class.dropup]': 'dropup',
                style: 'position: absolute;display: block;'
            },
            // tslint: enable
            encapsulation: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewEncapsulation"].None
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]])
    ], TypeaheadContainerComponent);
    return TypeaheadContainerComponent;
}());



/***/ }),
/* 57 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["b"] = latinize;
/* unused harmony export escapeRegexp */
/* harmony export (immutable) */ __webpack_exports__["c"] = tokenize;
/* harmony export (immutable) */ __webpack_exports__["a"] = getValueFromObject;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__latin_map__ = __webpack_require__(116);

function latinize(str) {
    if (!str) {
        return '';
    }
    return str.replace(/[^A-Za-z0-9\[\] ]/g, function (a) {
        return __WEBPACK_IMPORTED_MODULE_0__latin_map__["a" /* latinMap */][a] || a;
    });
}
function escapeRegexp(queryToEscape) {
    // Regex: capture the whole query string and replace it with the string
    // that will be used to match the results, for example if the capture is
    // 'a' the result will be \a
    return queryToEscape.replace(/([.?*+^$[\]\\(){}|-])/g, '\\$1');
}
/* tslint:disable */
function tokenize(str, wordRegexDelimiters, phraseRegexDelimiters) {
    if (wordRegexDelimiters === void 0) { wordRegexDelimiters = ' '; }
    if (phraseRegexDelimiters === void 0) { phraseRegexDelimiters = ''; }
    /* tslint:enable */
    var regexStr = '(?:[' + phraseRegexDelimiters + '])([^' + phraseRegexDelimiters + ']+)(?:[' + phraseRegexDelimiters + '])|([^' + wordRegexDelimiters + ']+)';
    var preTokenized = str.split(new RegExp(regexStr, 'g'));
    var result = [];
    var preTokenizedLength = preTokenized.length;
    var token;
    var replacePhraseDelimiters = new RegExp('[' + phraseRegexDelimiters + ']+', 'g');
    for (var i = 0; i < preTokenizedLength; i += 1) {
        token = preTokenized[i];
        if (token && token.length && token !== wordRegexDelimiters) {
            result.push(token.replace(replacePhraseDelimiters, ''));
        }
    }
    return result;
}
function getValueFromObject(object, option) {
    if (!option || typeof object !== 'object') {
        return object.toString();
    }
    if (option.endsWith('()')) {
        var functionName = option.slice(0, option.length - 2);
        return object[functionName]().toString();
    }
    var properties = option.replace(/\[(\w+)\]/g, '.$1')
        .replace(/^\./, '');
    var propertiesArray = properties.split('.');
    for (var _i = 0, propertiesArray_1 = propertiesArray; _i < propertiesArray_1.length; _i++) {
        var property = propertiesArray_1[_i];
        if (property in object) {
            object = object[property];
        }
    }
    if (!object)
        return "";
    return object.toString();
}


/***/ }),
/* 58 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PopoverContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__popover_config__ = __webpack_require__(27);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__ = __webpack_require__(3);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var PopoverContainerComponent = (function () {
    function PopoverContainerComponent(config) {
        Object.assign(this, config);
    }
    Object.defineProperty(PopoverContainerComponent.prototype, "isBs3", {
        get: function () {
            return Object(__WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverContainerComponent.prototype, "placement", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverContainerComponent.prototype, "title", void 0);
    PopoverContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'popover-container',
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush,
            // tslint:disable-next-line
            host: {
                '[class]': '"popover in popover-" + placement + " " + "bs-popover-" + placement + " " + placement + " " + containerClass',
                '[class.show]': '!isBs3',
                role: 'tooltip',
                style: 'display:block;'
            },
            styles: ["\n    :host.bs-popover-top .arrow, :host.bs-popover-bottom .arrow {\n      left: calc(50% - 5px);\n    }\n    :host.bs-popover-left .arrow, :host.bs-popover-right .arrow {\n      top: calc(50% - 2.5px);\n    }\n  "],
            template: "\n<div class=\"popover-arrow arrow\"></div>\n<h3 class=\"popover-title popover-header\" *ngIf=\"title\">{{title}}</h3><div class=\"popover-content popover-body\"><ng-content></ng-content></div>\n    "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__popover_config__["a" /* PopoverConfig */]])
    ], PopoverContainerComponent);
    return PopoverContainerComponent;
}());



/***/ }),
/* 59 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerStore; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__mini_ngrx_store_class__ = __webpack_require__(109);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_datepicker_state__ = __webpack_require__(122);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_BehaviorSubject__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_BehaviorSubject___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_BehaviorSubject__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__mini_ngrx_state_class__ = __webpack_require__(111);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_datepicker_reducer__ = __webpack_require__(162);
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};






var BsDatepickerStore = (function (_super) {
    __extends(BsDatepickerStore, _super);
    function BsDatepickerStore() {
        var _this = this;
        var _dispatcher = new __WEBPACK_IMPORTED_MODULE_3_rxjs_BehaviorSubject__["BehaviorSubject"]({ type: '[datepicker] dispatcher init' });
        var state = new __WEBPACK_IMPORTED_MODULE_4__mini_ngrx_state_class__["a" /* MiniState */](__WEBPACK_IMPORTED_MODULE_2__bs_datepicker_state__["a" /* initialDatepickerState */], _dispatcher, __WEBPACK_IMPORTED_MODULE_5__bs_datepicker_reducer__["a" /* bsDatepickerReducer */]);
        _this = _super.call(this, _dispatcher, __WEBPACK_IMPORTED_MODULE_5__bs_datepicker_reducer__["a" /* bsDatepickerReducer */], state) || this;
        return _this;
    }
    BsDatepickerStore = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [])
    ], BsDatepickerStore);
    return BsDatepickerStore;
}(__WEBPACK_IMPORTED_MODULE_1__mini_ngrx_store_class__["a" /* MiniStore */]));



/***/ }),
/* 60 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = createMatrix;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_setters__ = __webpack_require__(7);

function createMatrix(options, fn) {
    var prevValue = options.initialDate;
    var matrix = new Array(options.height);
    for (var i = 0; i < options.height; i++) {
        matrix[i] = new Array(options.width);
        for (var j = 0; j < options.width; j++) {
            matrix[i][j] = fn(prevValue);
            prevValue = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_setters__["c" /* shiftDate */])(prevValue, options.shift);
        }
    }
    return matrix;
}


/***/ }),
/* 61 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerEffects; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_add_operator_filter__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_add_operator_filter___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_rxjs_add_operator_filter__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_map__ = __webpack_require__(118);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_map___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_map__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_datepicker_actions__ = __webpack_require__(19);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var BsDatepickerEffects = (function () {
    function BsDatepickerEffects(_actions) {
        this._actions = _actions;
        this._subs = [];
    }
    BsDatepickerEffects.prototype.init = function (_bsDatepickerStore) {
        this._store = _bsDatepickerStore;
        return this;
    };
    /** setters */
    BsDatepickerEffects.prototype.setValue = function (value) {
        this._store.dispatch(this._actions.select(value));
    };
    BsDatepickerEffects.prototype.setRangeValue = function (value) {
        this._store.dispatch(this._actions.selectRange(value));
    };
    BsDatepickerEffects.prototype.setMinDate = function (value) {
        this._store.dispatch(this._actions.minDate(value));
        return this;
    };
    BsDatepickerEffects.prototype.setMaxDate = function (value) {
        this._store.dispatch(this._actions.maxDate(value));
        return this;
    };
    BsDatepickerEffects.prototype.setDisabled = function (value) {
        this._store.dispatch(this._actions.isDisabled(value));
        return this;
    };
    /* Set rendering options */
    BsDatepickerEffects.prototype.setOptions = function (_config) {
        this._store.dispatch(this._actions.setOptions(_config));
        return this;
    };
    /** view to mode bindings */
    BsDatepickerEffects.prototype.setBindings = function (container) {
        container.daysCalendar = this._store
            .select(function (state) { return state.flaggedMonths; })
            .filter(function (months) { return !!months; });
        // month calendar
        container.monthsCalendar = this._store
            .select(function (state) { return state.flaggedMonthsCalendar; })
            .filter(function (months) { return !!months; });
        // year calendar
        container.yearsCalendar = this._store
            .select(function (state) { return state.yearsCalendarFlagged; })
            .filter(function (years) { return !!years; });
        container.viewMode = this._store
            .select(function (state) { return state.view.mode; });
        container.options = this._store.select(function (state) { return state.showWeekNumbers; })
            .map(function (showWeekNumbers) { return ({ showWeekNumbers: showWeekNumbers }); });
        return this;
    };
    /** event handlers*/
    BsDatepickerEffects.prototype.setEventHandlers = function (container) {
        var _this = this;
        container.setViewMode = function (event) {
            _this._store.dispatch(_this._actions.changeViewMode(event));
        };
        container.navigateTo = function (event) {
            _this._store.dispatch(_this._actions.navigateStep(event.step));
        };
        container.dayHoverHandler = function (event) {
            var _cell = event.cell;
            if (_cell.isOtherMonth || _cell.isDisabled) {
                return;
            }
            _this._store.dispatch(_this._actions.hoverDay(event));
            _cell.isHovered = event.isHovered;
        };
        container.monthHoverHandler = function (event) {
            event.cell.isHovered = event.isHovered;
        };
        container.yearHoverHandler = function (event) {
            event.cell.isHovered = event.isHovered;
        };
        /** select handlers */
        // container.daySelectHandler = (day: DayViewModel): void => {
        //   if (day.isOtherMonth || day.isDisabled) {
        //     return;
        //   }
        //   this._store.dispatch(this._actions.select(day.date));
        // };
        container.monthSelectHandler = function (event) {
            if (event.isDisabled) {
                return;
            }
            _this._store.dispatch(_this._actions.navigateTo({
                unit: { month: Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_date_getters__["g" /* getMonth */])(event.date) },
                viewMode: 'day'
            }));
        };
        container.yearSelectHandler = function (event) {
            if (event.isDisabled) {
                return;
            }
            _this._store.dispatch(_this._actions.navigateTo({
                unit: { year: Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_date_getters__["d" /* getFullYear */])(event.date) },
                viewMode: 'month'
            }));
        };
        return this;
    };
    BsDatepickerEffects.prototype.registerDatepickerSideEffects = function () {
        var _this = this;
        this._subs.push(this._store.select(function (state) { return state.view; })
            .subscribe(function (view) {
            _this._store.dispatch(_this._actions.calculate());
        }));
        // format calendar values on month model change
        this._subs.push(this._store
            .select(function (state) { return state.monthsModel; })
            .filter(function (monthModel) { return !!monthModel; })
            .subscribe(function (month) {
            return _this._store.dispatch(_this._actions.format());
        }));
        // flag day values
        this._subs.push(this._store
            .select(function (state) { return state.formattedMonths; })
            .filter(function (month) { return !!month; })
            .subscribe(function (month) {
            return _this._store.dispatch(_this._actions.flag());
        }));
        // flag day values
        this._subs.push(this._store.select(function (state) { return state.selectedDate; })
            .filter(function (selectedDate) { return !!selectedDate; })
            .subscribe(function (selectedDate) {
            return _this._store.dispatch(_this._actions.flag());
        }));
        // flag for date range picker
        this._subs.push(this._store.select(function (state) { return state.selectedRange; })
            .filter(function (selectedRange) { return !!selectedRange; })
            .subscribe(function (selectedRange) {
            return _this._store.dispatch(_this._actions.flag());
        }));
        // monthsCalendar
        this._subs.push(this._store
            .select(function (state) { return state.monthsCalendar; })
            .subscribe(function () { return _this._store.dispatch(_this._actions.flag()); }));
        // years calendar
        this._subs.push(this._store
            .select(function (state) { return state.yearsCalendarModel; })
            .filter(function (state) { return !!state; })
            .subscribe(function () { return _this._store.dispatch(_this._actions.flag()); }));
        // on hover
        this._subs.push(this._store.select(function (state) { return state.hoveredDate; })
            .filter(function (hoveredDate) { return !!hoveredDate; })
            .subscribe(function (hoveredDate) {
            return _this._store.dispatch(_this._actions.flag());
        }));
        return this;
    };
    BsDatepickerEffects.prototype.destroy = function () {
        for (var _i = 0, _a = this._subs; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
    };
    BsDatepickerEffects = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_4__bs_datepicker_actions__["a" /* BsDatepickerActions */]])
    ], BsDatepickerEffects);
    return BsDatepickerEffects;
}());



/***/ }),
/* 62 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDaterangepickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__themes_bs_bs_daterangepicker_container_component__ = __webpack_require__(127);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__component_loader_component_loader_factory__ = __webpack_require__(13);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_datepicker_config__ = __webpack_require__(8);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




var BsDaterangepickerComponent = (function () {
    function BsDaterangepickerComponent(_config, _elementRef, _renderer, _viewContainerRef, cis) {
        this._config = _config;
        /**
         * Placement of a daterangepicker. Accepts: "top", "bottom", "left", "right"
         */
        this.placement = 'bottom';
        /**
         * Specifies events that should trigger. Supports a space separated list of
         * event names.
         */
        this.triggers = 'click';
        /**
         * Close daterangepicker on outside click
         */
        this.outsideClick = true;
        /**
         * A selector specifying the element the daterangepicker should be appended to.
         * Currently only supports "body".
         */
        this.container = 'body';
        /**
         * Emits when daterangepicker value has been changed
         */
        this.bsValueChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this._subs = [];
        this._datepicker = cis
            .createLoader(_elementRef, _viewContainerRef, _renderer);
        Object.assign(this, _config);
        this.onShown = this._datepicker.onShown;
        this.onHidden = this._datepicker.onHidden;
    }
    Object.defineProperty(BsDaterangepickerComponent.prototype, "isOpen", {
        /**
         * Returns whether or not the daterangepicker is currently being shown
         */
        get: function () {
            return this._datepicker.isShown;
        },
        set: function (value) {
            if (value) {
                this.show();
            }
            else {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDaterangepickerComponent.prototype, "bsValue", {
        /**
         * Initial value of daterangepicker
         */
        set: function (value) {
            if (this._bsValue === value) {
                return;
            }
            this._bsValue = value;
            this.bsValueChange.emit(value);
        },
        enumerable: true,
        configurable: true
    });
    BsDaterangepickerComponent.prototype.ngOnInit = function () {
        var _this = this;
        this._datepicker.listen({
            outsideClick: this.outsideClick,
            triggers: this.triggers,
            show: function () { return _this.show(); }
        });
    };
    BsDaterangepickerComponent.prototype.ngOnChanges = function (changes) {
        if (!this._datepickerRef || !this._datepickerRef.instance) {
            return;
        }
        if (changes.minDate) {
            this._datepickerRef.instance.minDate = this.minDate;
        }
        if (changes.maxDate) {
            this._datepickerRef.instance.maxDate = this.maxDate;
        }
        if (changes.isDisabled) {
            this._datepickerRef.instance.isDisabled = this.isDisabled;
        }
    };
    /**
     * Opens an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDaterangepickerComponent.prototype.show = function () {
        var _this = this;
        if (this._datepicker.isShown) {
            return;
        }
        this._config = Object.assign({}, this._config, { displayMonths: 2 }, this.bsConfig, {
            value: this._bsValue,
            isDisabled: this.isDisabled,
            minDate: this.minDate || this._config.minDate,
            maxDate: this.maxDate || this._config.maxDate
        });
        this._datepickerRef = this._datepicker
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_3__bs_datepicker_config__["a" /* BsDatepickerConfig */], useValue: this._config })
            .attach(__WEBPACK_IMPORTED_MODULE_1__themes_bs_bs_daterangepicker_container_component__["a" /* BsDaterangepickerContainerComponent */])
            .to(this.container)
            .position({ attachment: this.placement })
            .show({ placement: this.placement });
        // if date changes from external source (model -> view)
        this._subs.push(this.bsValueChange.subscribe(function (value) {
            _this._datepickerRef.instance.value = value;
        }));
        // if date changes from picker (view -> model)
        this._subs.push(this._datepickerRef.instance
            .valueChange
            .filter(function (range) { return range && range[0] && !!range[1]; })
            .subscribe(function (value) {
            _this.bsValue = value;
            _this.hide();
        }));
    };
    /**
     * Closes an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDaterangepickerComponent.prototype.hide = function () {
        if (this.isOpen) {
            this._datepicker.hide();
        }
        for (var _i = 0, _a = this._subs; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
    };
    /**
     * Toggles an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDaterangepickerComponent.prototype.toggle = function () {
        if (this.isOpen) {
            return this.hide();
        }
        this.show();
    };
    BsDaterangepickerComponent.prototype.ngOnDestroy = function () {
        this._datepicker.dispose();
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BsDaterangepickerComponent.prototype, "placement", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDaterangepickerComponent.prototype, "triggers", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDaterangepickerComponent.prototype, "outsideClick", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDaterangepickerComponent.prototype, "container", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], BsDaterangepickerComponent.prototype, "isOpen", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDaterangepickerComponent.prototype, "onShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDaterangepickerComponent.prototype, "onHidden", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array),
        __metadata("design:paramtypes", [Array])
    ], BsDaterangepickerComponent.prototype, "bsValue", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDaterangepickerComponent.prototype, "bsConfig", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], BsDaterangepickerComponent.prototype, "isDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], BsDaterangepickerComponent.prototype, "minDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], BsDaterangepickerComponent.prototype, "maxDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDaterangepickerComponent.prototype, "bsValueChange", void 0);
    BsDaterangepickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-daterangepicker,[bsDaterangepicker]',
            exportAs: 'bsDaterangepicker',
            template: ' '
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_3__bs_datepicker_config__["a" /* BsDatepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"],
            __WEBPACK_IMPORTED_MODULE_2__component_loader_component_loader_factory__["a" /* ComponentLoaderFactory */]])
    ], BsDaterangepickerComponent);
    return BsDaterangepickerComponent;
}());



/***/ }),
/* 63 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__component_loader_component_loader_factory__ = __webpack_require__(13);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__themes_bs_bs_datepicker_container_component__ = __webpack_require__(125);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_filter__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_filter___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_filter__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_datepicker_config__ = __webpack_require__(8);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var BsDatepickerComponent = (function () {
    function BsDatepickerComponent(_config, _elementRef, _renderer, _viewContainerRef, cis) {
        this._config = _config;
        /**
         * Placement of a datepicker. Accepts: "top", "bottom", "left", "right"
         */
        this.placement = 'bottom';
        /**
         * Specifies events that should trigger. Supports a space separated list of
         * event names.
         */
        this.triggers = 'click';
        /**
         * Close datepicker on outside click
         */
        this.outsideClick = true;
        /**
         * A selector specifying the element the datepicker should be appended to.
         * Currently only supports "body".
         */
        this.container = 'body';
        /**
         * Emits when datepicker value has been changed
         */
        this.bsValueChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this._subs = [];
        // todo: assign only subset of fields
        Object.assign(this, this._config);
        this._datepicker = cis
            .createLoader(_elementRef, _viewContainerRef, _renderer);
        this.onShown = this._datepicker.onShown;
        this.onHidden = this._datepicker.onHidden;
    }
    Object.defineProperty(BsDatepickerComponent.prototype, "isOpen", {
        /**
         * Returns whether or not the datepicker is currently being shown
         */
        get: function () {
            return this._datepicker.isShown;
        },
        set: function (value) {
            if (value) {
                this.show();
            }
            else {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDatepickerComponent.prototype, "bsValue", {
        /**
         * Initial value of datepicker
         */
        set: function (value) {
            if (this._bsValue === value) {
                return;
            }
            this._bsValue = value;
            this.bsValueChange.emit(value);
        },
        enumerable: true,
        configurable: true
    });
    BsDatepickerComponent.prototype.ngOnInit = function () {
        var _this = this;
        this._datepicker.listen({
            outsideClick: this.outsideClick,
            triggers: this.triggers,
            show: function () { return _this.show(); }
        });
    };
    BsDatepickerComponent.prototype.ngOnChanges = function (changes) {
        if (!this._datepickerRef || !this._datepickerRef.instance) {
            return;
        }
        if (changes.minDate) {
            this._datepickerRef.instance.minDate = this.minDate;
        }
        if (changes.maxDate) {
            this._datepickerRef.instance.maxDate = this.maxDate;
        }
        if (changes.isDisabled) {
            this._datepickerRef.instance.isDisabled = this.isDisabled;
        }
    };
    /**
     * Opens an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDatepickerComponent.prototype.show = function () {
        var _this = this;
        if (this._datepicker.isShown) {
            return;
        }
        this._config = Object.assign({}, this._config, this.bsConfig, {
            value: this._bsValue,
            isDisabled: this.isDisabled,
            minDate: this.minDate || this._config.minDate,
            maxDate: this.maxDate || this._config.maxDate
        });
        this._datepickerRef = this._datepicker
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_4__bs_datepicker_config__["a" /* BsDatepickerConfig */], useValue: this._config })
            .attach(__WEBPACK_IMPORTED_MODULE_2__themes_bs_bs_datepicker_container_component__["a" /* BsDatepickerContainerComponent */])
            .to(this.container)
            .position({ attachment: this.placement })
            .show({ placement: this.placement });
        // if date changes from external source (model -> view)
        this._subs.push(this.bsValueChange.subscribe(function (value) {
            _this._datepickerRef.instance.value = value;
        }));
        // if date changes from picker (view -> model)
        this._subs.push(this._datepickerRef.instance
            .valueChange.subscribe(function (value) {
            _this.bsValue = value;
            _this.hide();
        }));
    };
    /**
     * Closes an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDatepickerComponent.prototype.hide = function () {
        if (this.isOpen) {
            this._datepicker.hide();
        }
        for (var _i = 0, _a = this._subs; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
    };
    /**
     * Toggles an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDatepickerComponent.prototype.toggle = function () {
        if (this.isOpen) {
            return this.hide();
        }
        this.show();
    };
    BsDatepickerComponent.prototype.ngOnDestroy = function () {
        this._datepicker.dispose();
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BsDatepickerComponent.prototype, "placement", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDatepickerComponent.prototype, "triggers", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDatepickerComponent.prototype, "outsideClick", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDatepickerComponent.prototype, "container", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], BsDatepickerComponent.prototype, "isOpen", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDatepickerComponent.prototype, "onShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDatepickerComponent.prototype, "onHidden", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date),
        __metadata("design:paramtypes", [Date])
    ], BsDatepickerComponent.prototype, "bsValue", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDatepickerComponent.prototype, "bsConfig", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], BsDatepickerComponent.prototype, "isDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], BsDatepickerComponent.prototype, "minDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], BsDatepickerComponent.prototype, "maxDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDatepickerComponent.prototype, "bsValueChange", void 0);
    BsDatepickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-datepicker,[bsDatepicker]',
            exportAs: 'bsDatepicker',
            template: '<ng-content></ng-content>'
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_4__bs_datepicker_config__["a" /* BsDatepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"],
            __WEBPACK_IMPORTED_MODULE_1__component_loader_component_loader_factory__["a" /* ComponentLoaderFactory */]])
    ], BsDatepickerComponent);
    return BsDatepickerComponent;
}());



/***/ }),
/* 64 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AccordionModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__collapse_collapse_module__ = __webpack_require__(30);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__accordion_group_component__ = __webpack_require__(66);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__accordion_component__ = __webpack_require__(31);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__accordion_config__ = __webpack_require__(32);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};






var AccordionModule = (function () {
    function AccordionModule() {
    }
    AccordionModule_1 = AccordionModule;
    AccordionModule.forRoot = function () { return { ngModule: AccordionModule_1, providers: [__WEBPACK_IMPORTED_MODULE_5__accordion_config__["a" /* AccordionConfig */]] }; };
    AccordionModule = AccordionModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"], __WEBPACK_IMPORTED_MODULE_2__collapse_collapse_module__["a" /* CollapseModule */]],
            declarations: [__WEBPACK_IMPORTED_MODULE_4__accordion_component__["a" /* AccordionComponent */], __WEBPACK_IMPORTED_MODULE_3__accordion_group_component__["a" /* AccordionPanelComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_4__accordion_component__["a" /* AccordionComponent */], __WEBPACK_IMPORTED_MODULE_3__accordion_group_component__["a" /* AccordionPanelComponent */]]
        })
    ], AccordionModule);
    return AccordionModule;
    var AccordionModule_1;
}());



/***/ }),
/* 65 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return CollapseDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
// todo: add animations when https://github.com/angular/angular/issues/9947 solved

var CollapseDirective = (function () {
    function CollapseDirective(_el, _renderer) {
        /** This event fires as soon as content collapses */
        this.collapsed = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** This event fires as soon as content becomes visible */
        this.expanded = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        // shown
        this.isExpanded = true;
        // hidden
        this.isCollapsed = false;
        // stale state
        this.isCollapse = true;
        // animation state
        this.isCollapsing = false;
        this._el = _el;
        this._renderer = _renderer;
    }
    Object.defineProperty(CollapseDirective.prototype, "collapse", {
        get: function () {
            return this.isExpanded;
        },
        /** A flag indicating visibility of content (shown or hidden) */
        set: function (value) {
            this.isExpanded = value;
            this.toggle();
        },
        enumerable: true,
        configurable: true
    });
    /** allows to manually toggle content visibility */
    CollapseDirective.prototype.toggle = function () {
        if (this.isExpanded) {
            this.hide();
        }
        else {
            this.show();
        }
    };
    /** allows to manually hide content */
    CollapseDirective.prototype.hide = function () {
        this.isCollapse = false;
        this.isCollapsing = true;
        this.isExpanded = false;
        this.isCollapsed = true;
        this.isCollapse = true;
        this.isCollapsing = false;
        this.display = 'none';
        this.collapsed.emit(this);
    };
    /** allows to manually show collapsed content */
    CollapseDirective.prototype.show = function () {
        this.isCollapse = false;
        this.isCollapsing = true;
        this.isExpanded = true;
        this.isCollapsed = false;
        this.display = 'block';
        // this.height = 'auto';
        this.isCollapse = true;
        this.isCollapsing = false;
        this._renderer.setElementStyle(this._el.nativeElement, 'overflow', 'visible');
        this._renderer.setElementStyle(this._el.nativeElement, 'height', 'auto');
        this.expanded.emit(this);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], CollapseDirective.prototype, "collapsed", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], CollapseDirective.prototype, "expanded", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('style.display'),
        __metadata("design:type", String)
    ], CollapseDirective.prototype, "display", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.in'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.show'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('attr.aria-expanded'),
        __metadata("design:type", Boolean)
    ], CollapseDirective.prototype, "isExpanded", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('attr.aria-hidden'),
        __metadata("design:type", Boolean)
    ], CollapseDirective.prototype, "isCollapsed", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.collapse'),
        __metadata("design:type", Boolean)
    ], CollapseDirective.prototype, "isCollapse", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.collapsing'),
        __metadata("design:type", Boolean)
    ], CollapseDirective.prototype, "isCollapsing", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], CollapseDirective.prototype, "collapse", null);
    CollapseDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[collapse]',
            exportAs: 'bs-collapse',
            /* tslint:disable-next-line */
            host: { '[class.collapse]': 'true' } /*,
            animations: [
              trigger('active', [
                state('void', style({height: 0})),
                state('closed', style({height: 0})),
                state('open', style({height: '*'})),
                transition('void => closed', [animate(0)]),
                transition('closed => open', [animate('350ms ease-out')]),
                transition('open => closed', [animate('350ms ease-out')])
              ])
            ]*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"]])
    ], CollapseDirective);
    return CollapseDirective;
}());



/***/ }),
/* 66 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AccordionPanelComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__accordion_component__ = __webpack_require__(31);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};



/*
 * ### Accordion heading

 Instead of using `heading` attribute on the `accordion-group`, you can use an `accordion-heading` attribute on `any` element inside of a group that will be used as group's header template.

 * */
var AccordionPanelComponent = (function () {
    function AccordionPanelComponent(accordion) {
        this.accordion = accordion;
    }
    Object.defineProperty(AccordionPanelComponent.prototype, "isOpen", {
        // Questionable, maybe .panel-open should be on child div.panel element?
        /** Is accordion group open or closed */
        get: function () {
            return this._isOpen;
        },
        set: function (value) {
            this._isOpen = value;
            if (value) {
                this.accordion.closeOtherPanels(this);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(AccordionPanelComponent.prototype, "isBs3", {
        get: function () {
            return Object(__WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    AccordionPanelComponent.prototype.ngOnInit = function () {
        this.panelClass = this.panelClass || 'panel-default';
        this.accordion.addGroup(this);
    };
    AccordionPanelComponent.prototype.ngOnDestroy = function () {
        this.accordion.removeGroup(this);
    };
    AccordionPanelComponent.prototype.toggleOpen = function (event) {
        if (!this.isDisabled) {
            this.isOpen = !this.isOpen;
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], AccordionPanelComponent.prototype, "heading", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], AccordionPanelComponent.prototype, "panelClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], AccordionPanelComponent.prototype, "isDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.panel-open'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], AccordionPanelComponent.prototype, "isOpen", null);
    AccordionPanelComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'accordion-group, accordion-panel',
            template: "\n    <div class=\"panel card\" [ngClass]=\"panelClass\">\n      <div class=\"panel-heading card-header\" role=\"tab\" (click)=\"toggleOpen($event)\">\n        <div class=\"panel-title\">\n          <div role=\"button\" class=\"accordion-toggle\" [attr.aria-expanded]=\"isOpen\">\n            <div *ngIf=\"heading\"[ngClass]=\"{'text-muted': isDisabled}\">{{heading}}</div>\n            <ng-content select=\"[accordion-heading]\"></ng-content>\n          </div>\n        </div>\n      </div>\n      <div class=\"panel-collapse collapse\" role=\"tabpanel\" [collapse]=\"!isOpen\">\n        <div class=\"panel-body card-block card-body\">\n          <ng-content></ng-content>\n        </div>\n      </div>\n    </div>\n  ",
            host: {
                class: 'panel',
                style: 'display: block'
            }
        }),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Inject"])(__WEBPACK_IMPORTED_MODULE_2__accordion_component__["a" /* AccordionComponent */])),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__accordion_component__["a" /* AccordionComponent */]])
    ], AccordionPanelComponent);
    return AccordionPanelComponent;
}());



/***/ }),
/* 67 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AlertModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__alert_component__ = __webpack_require__(68);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__alert_config__ = __webpack_require__(33);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};




var AlertModule = (function () {
    function AlertModule() {
    }
    AlertModule_1 = AlertModule;
    AlertModule.forRoot = function () {
        return { ngModule: AlertModule_1, providers: [__WEBPACK_IMPORTED_MODULE_3__alert_config__["a" /* AlertConfig */]] };
    };
    AlertModule = AlertModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_2__alert_component__["a" /* AlertComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_2__alert_component__["a" /* AlertComponent */]],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_2__alert_component__["a" /* AlertComponent */]]
        })
    ], AlertModule);
    return AlertModule;
    var AlertModule_1;
}());



/***/ }),
/* 68 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AlertComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__alert_config__ = __webpack_require__(33);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_decorators__ = __webpack_require__(34);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var AlertComponent = (function () {
    function AlertComponent(_config) {
        var _this = this;
        /** Alert type. Provides one of four bootstrap supported contextual classes: `success`, `info`, `warning` and `danger` */
        this.type = 'warning';
        /** If set, displays an inline "Close" button */
        this.dismissible = false;
        /** This event fires immediately after close instance method is called, $event is an instance of Alert component. */
        this.onClose = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** This event fires when alert closed, $event is an instance of Alert component */
        this.onClosed = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.isClosed = false;
        this.classes = '';
        this.dismissibleChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        Object.assign(this, _config);
        this.dismissibleChange.subscribe(function (dismissible) {
            _this.classes = _this.dismissible ? 'alert-dismissible' : '';
        });
    }
    AlertComponent.prototype.ngOnInit = function () {
        var _this = this;
        if (this.dismissOnTimeout) {
            // if dismissOnTimeout used as attr without binding, it will be a string
            setTimeout(function () { return _this.close(); }, parseInt(this.dismissOnTimeout, 10));
        }
    };
    // todo: animation ` If the .fade and .in classes are present on the element,
    // the alert will fade out before it is removed`
    /**
     * Closes an alert by removing it from the DOM.
     */
    AlertComponent.prototype.close = function () {
        if (this.isClosed) {
            return;
        }
        this.onClose.emit(this);
        this.isClosed = true;
        this.onClosed.emit(this);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], AlertComponent.prototype, "type", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_2__utils_decorators__["a" /* OnChange */])(),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], AlertComponent.prototype, "dismissible", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], AlertComponent.prototype, "dismissOnTimeout", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], AlertComponent.prototype, "onClose", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], AlertComponent.prototype, "onClosed", void 0);
    AlertComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'alert,ngx-alert',
            template: "\n<template [ngIf]=\"!isClosed\">\n  <div [class]=\"'alert alert-' + type\" role=\"alert\" [ngClass]=\"classes\">\n    <template [ngIf]=\"dismissible\">\n      <button type=\"button\" class=\"close\" aria-label=\"Close\" (click)=\"close()\">\n        <span aria-hidden=\"true\">&times;</span>\n        <span class=\"sr-only\">Close</span>\n      </button>\n    </template>\n    <ng-content></ng-content>\n  </div>\n</template>\n  "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__alert_config__["a" /* AlertConfig */]])
    ], AlertComponent);
    return AlertComponent;
}());



/***/ }),
/* 69 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ButtonsModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__button_checkbox_directive__ = __webpack_require__(70);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__button_radio_directive__ = __webpack_require__(71);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};



var ButtonsModule = (function () {
    function ButtonsModule() {
    }
    ButtonsModule_1 = ButtonsModule;
    ButtonsModule.forRoot = function () {
        return { ngModule: ButtonsModule_1, providers: [] };
    };
    ButtonsModule = ButtonsModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            declarations: [__WEBPACK_IMPORTED_MODULE_1__button_checkbox_directive__["a" /* ButtonCheckboxDirective */], __WEBPACK_IMPORTED_MODULE_2__button_radio_directive__["a" /* ButtonRadioDirective */]],
            exports: [__WEBPACK_IMPORTED_MODULE_1__button_checkbox_directive__["a" /* ButtonCheckboxDirective */], __WEBPACK_IMPORTED_MODULE_2__button_radio_directive__["a" /* ButtonRadioDirective */]]
        })
    ], ButtonsModule);
    return ButtonsModule;
    var ButtonsModule_1;
}());



/***/ }),
/* 70 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export CHECKBOX_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ButtonCheckboxDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


// TODO: config: activeClass - Class to apply to the checked buttons
var CHECKBOX_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return ButtonCheckboxDirective; }),
    multi: true
};
/**
 * Add checkbox functionality to any element
 */
var ButtonCheckboxDirective = (function () {
    function ButtonCheckboxDirective() {
        /** Truthy value, will be set to ngModel */
        this.btnCheckboxTrue = true;
        /** Falsy value, will be set to ngModel */
        this.btnCheckboxFalse = false;
        this.state = false;
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
    }
    // view -> model
    ButtonCheckboxDirective.prototype.onClick = function () {
        if (this.isDisabled) {
            return;
        }
        this.toggle(!this.state);
        this.onChange(this.value);
    };
    ButtonCheckboxDirective.prototype.ngOnInit = function () {
        this.toggle(this.trueValue === this.value);
    };
    Object.defineProperty(ButtonCheckboxDirective.prototype, "trueValue", {
        get: function () {
            return typeof this.btnCheckboxTrue !== 'undefined'
                ? this.btnCheckboxTrue
                : true;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(ButtonCheckboxDirective.prototype, "falseValue", {
        get: function () {
            return typeof this.btnCheckboxFalse !== 'undefined'
                ? this.btnCheckboxFalse
                : false;
        },
        enumerable: true,
        configurable: true
    });
    ButtonCheckboxDirective.prototype.toggle = function (state) {
        this.state = state;
        this.value = this.state ? this.trueValue : this.falseValue;
    };
    // ControlValueAccessor
    // model -> view
    ButtonCheckboxDirective.prototype.writeValue = function (value) {
        this.state = this.trueValue === value;
        this.value = value ? this.trueValue : this.falseValue;
    };
    ButtonCheckboxDirective.prototype.setDisabledState = function (isDisabled) {
        this.isDisabled = isDisabled;
    };
    ButtonCheckboxDirective.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    ButtonCheckboxDirective.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], ButtonCheckboxDirective.prototype, "btnCheckboxTrue", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], ButtonCheckboxDirective.prototype, "btnCheckboxFalse", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.active'),
        __metadata("design:type", Boolean)
    ], ButtonCheckboxDirective.prototype, "state", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('click'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], ButtonCheckboxDirective.prototype, "onClick", null);
    ButtonCheckboxDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: '[btnCheckbox]', providers: [CHECKBOX_CONTROL_VALUE_ACCESSOR] })
    ], ButtonCheckboxDirective);
    return ButtonCheckboxDirective;
}());



/***/ }),
/* 71 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export RADIO_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ButtonRadioDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var RADIO_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return ButtonRadioDirective; }),
    multi: true
};
/**
 * Create radio buttons or groups of buttons.
 * A value of a selected button is bound to a variable specified via ngModel.
 */
var ButtonRadioDirective = (function () {
    function ButtonRadioDirective(el, cdr) {
        this.cdr = cdr;
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
        this.el = el;
    }
    Object.defineProperty(ButtonRadioDirective.prototype, "isActive", {
        get: function () {
            return this.btnRadio === this.value;
        },
        enumerable: true,
        configurable: true
    });
    ButtonRadioDirective.prototype.onClick = function () {
        if (this.el.nativeElement.attributes.disabled) {
            return;
        }
        if (this.uncheckable && this.btnRadio === this.value) {
            this.value = undefined;
            this.onTouched();
            this.onChange(this.value);
            return;
        }
        if (this.btnRadio !== this.value) {
            this.value = this.btnRadio;
            this.onTouched();
            this.onChange(this.value);
            return;
        }
    };
    ButtonRadioDirective.prototype.ngOnInit = function () {
        this.uncheckable = typeof this.uncheckable !== 'undefined';
    };
    ButtonRadioDirective.prototype.onBlur = function () {
        this.onTouched();
    };
    // ControlValueAccessor
    // model -> view
    ButtonRadioDirective.prototype.writeValue = function (value) {
        this.value = value;
        this.cdr.markForCheck();
    };
    ButtonRadioDirective.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    ButtonRadioDirective.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], ButtonRadioDirective.prototype, "btnRadio", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], ButtonRadioDirective.prototype, "uncheckable", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], ButtonRadioDirective.prototype, "value", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.active'),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [])
    ], ButtonRadioDirective.prototype, "isActive", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('click'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], ButtonRadioDirective.prototype, "onClick", null);
    ButtonRadioDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: '[btnRadio]', providers: [RADIO_CONTROL_VALUE_ACCESSOR] }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectorRef"]])
    ], ButtonRadioDirective);
    return ButtonRadioDirective;
}());



/***/ }),
/* 72 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return CarouselModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__carousel_component__ = __webpack_require__(35);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__slide_component__ = __webpack_require__(74);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__carousel_config__ = __webpack_require__(38);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};





var CarouselModule = (function () {
    function CarouselModule() {
    }
    CarouselModule_1 = CarouselModule;
    CarouselModule.forRoot = function () {
        return { ngModule: CarouselModule_1, providers: [] };
    };
    CarouselModule = CarouselModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__slide_component__["a" /* SlideComponent */], __WEBPACK_IMPORTED_MODULE_2__carousel_component__["a" /* CarouselComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__slide_component__["a" /* SlideComponent */], __WEBPACK_IMPORTED_MODULE_2__carousel_component__["a" /* CarouselComponent */]],
            providers: [__WEBPACK_IMPORTED_MODULE_4__carousel_config__["a" /* CarouselConfig */]]
        })
    ], CarouselModule);
    return CarouselModule;
    var CarouselModule_1;
}());



/***/ }),
/* 73 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Trigger; });
/**
 * @copyright Valor Software
 * @copyright Angular ng-bootstrap team
 */
var Trigger = (function () {
    function Trigger(open, close) {
        this.open = open;
        this.close = close || open;
    }
    Trigger.prototype.isManual = function () { return this.open === 'manual' || this.close === 'manual'; };
    return Trigger;
}());



/***/ }),
/* 74 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return SlideComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__carousel_component__ = __webpack_require__(35);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var SlideComponent = (function () {
    function SlideComponent(carousel) {
        /** Wraps element by appropriate CSS classes */
        this.addClass = true;
        this.carousel = carousel;
    }
    /** Fires changes in container collection after adding a new slide instance */
    SlideComponent.prototype.ngOnInit = function () {
        this.carousel.addSlide(this);
    };
    /** Fires changes in container collection after removing of this slide instance */
    SlideComponent.prototype.ngOnDestroy = function () {
        this.carousel.removeSlide(this);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.active'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], SlideComponent.prototype, "active", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.item'),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('class.carousel-item'),
        __metadata("design:type", Boolean)
    ], SlideComponent.prototype, "addClass", void 0);
    SlideComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'slide',
            template: "\n    <div [class.active]=\"active\" class=\"item\">\n      <ng-content></ng-content>\n    </div>\n  "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__carousel_component__["a" /* CarouselComponent */]])
    ], SlideComponent);
    return SlideComponent;
}());



/***/ }),
/* 75 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DatepickerModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__datepicker_inner_component__ = __webpack_require__(15);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__datepicker_component__ = __webpack_require__(79);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__daypicker_component__ = __webpack_require__(80);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__monthpicker_component__ = __webpack_require__(81);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__yearpicker_component__ = __webpack_require__(82);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__datepicker_config__ = __webpack_require__(41);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};









var DatepickerModule = (function () {
    function DatepickerModule() {
    }
    DatepickerModule_1 = DatepickerModule;
    DatepickerModule.forRoot = function () {
        return { ngModule: DatepickerModule_1, providers: [__WEBPACK_IMPORTED_MODULE_8__datepicker_config__["a" /* DatepickerConfig */]] };
    };
    DatepickerModule = DatepickerModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"], __WEBPACK_IMPORTED_MODULE_2__angular_forms__["FormsModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_4__datepicker_component__["a" /* DatePickerComponent */], __WEBPACK_IMPORTED_MODULE_3__datepicker_inner_component__["a" /* DatePickerInnerComponent */], __WEBPACK_IMPORTED_MODULE_5__daypicker_component__["a" /* DayPickerComponent */],
                __WEBPACK_IMPORTED_MODULE_6__monthpicker_component__["a" /* MonthPickerComponent */], __WEBPACK_IMPORTED_MODULE_7__yearpicker_component__["a" /* YearPickerComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_4__datepicker_component__["a" /* DatePickerComponent */], __WEBPACK_IMPORTED_MODULE_3__datepicker_inner_component__["a" /* DatePickerInnerComponent */], __WEBPACK_IMPORTED_MODULE_5__daypicker_component__["a" /* DayPickerComponent */],
                __WEBPACK_IMPORTED_MODULE_6__monthpicker_component__["a" /* MonthPickerComponent */], __WEBPACK_IMPORTED_MODULE_7__yearpicker_component__["a" /* YearPickerComponent */]],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_4__datepicker_component__["a" /* DatePickerComponent */]]
        })
    ], DatepickerModule);
    return DatepickerModule;
    var DatepickerModule_1;
}());



/***/ }),
/* 76 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DateFormatter; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_format__ = __webpack_require__(10);

var DateFormatter = (function () {
    function DateFormatter() {
    }
    DateFormatter.prototype.format = function (date, format, locale) {
        return Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_format__["a" /* formatDate */])(date, format, locale);
    };
    return DateFormatter;
}());



/***/ }),
/* 77 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return defaultLocaleMonths; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return defaultLocaleMonthsShort; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return defaultLocaleWeekdays; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return defaultLocaleWeekdaysShort; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return defaultLocaleWeekdaysMin; });
/* unused harmony export defaultLongDateFormat */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Locale; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__units_week_calendar_utils__ = __webpack_require__(39);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_type_checks__ = __webpack_require__(11);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_date_getters__ = __webpack_require__(1);



var MONTHS_IN_FORMAT = /D[oD]?(\[[^\[\]]*\]|\s)+MMMM?/;
var defaultLocaleMonths = 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_');
var defaultLocaleMonthsShort = 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_');
var defaultLocaleWeekdays = 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_');
var defaultLocaleWeekdaysShort = 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_');
var defaultLocaleWeekdaysMin = 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_');
var defaultLongDateFormat = {
    LTS: 'h:mm:ss A',
    LT: 'h:mm A',
    L: 'MM/DD/YYYY',
    LL: 'MMMM D, YYYY',
    LLL: 'MMMM D, YYYY h:mm A',
    LLLL: 'dddd, MMMM D, YYYY h:mm A'
};
var Locale = (function () {
    function Locale(config) {
        if (!!config) {
            this.set(config);
        }
    }
    Locale.prototype.set = function (config) {
        for (var i in config) {
            if (!config.hasOwnProperty(i)) {
                continue;
            }
            var prop = config[i];
            var key = Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["d" /* isFunction */])(prop) ? i : "_" + i;
            this[key] = prop;
        }
        this._config = config;
    };
    // Months
    // LOCALES
    Locale.prototype.months = function (date, format) {
        if (!date) {
            return Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["b" /* isArray */])(this._months)
                ? this._months
                : this._months.standalone;
        }
        if (Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["b" /* isArray */])(this._months)) {
            return this._months[Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["g" /* getMonth */])(date)];
        }
        var key = (this._months.isFormat || MONTHS_IN_FORMAT)
            .test(format) ? 'format' : 'standalone';
        return this._months[key][Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["g" /* getMonth */])(date)];
    };
    Locale.prototype.monthsShort = function (date, format) {
        if (!date) {
            return Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["b" /* isArray */])(this._monthsShort)
                ? this._monthsShort
                : this._monthsShort.standalone;
        }
        if (Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["b" /* isArray */])(this._monthsShort)) {
            return this._monthsShort[Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["g" /* getMonth */])(date)];
        }
        var key = MONTHS_IN_FORMAT.test(format) ? 'format' : 'standalone';
        return this._monthsShort[key][Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["g" /* getMonth */])(date)];
    };
    // Days of week
    // LOCALES
    Locale.prototype.weekdays = function (date, format) {
        var _isArray = Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["b" /* isArray */])(this._weekdays);
        if (!date) {
            return _isArray
                ? this._weekdays
                : this._weekdays.standalone;
        }
        if (_isArray) {
            return this._weekdays[Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["b" /* getDayOfWeek */])(date)];
        }
        var _key = this._weekdays.isFormat.test(format) ? 'format' : 'standalone';
        return this._weekdays[_key][Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["b" /* getDayOfWeek */])(date)];
    };
    Locale.prototype.weekdaysMin = function (date) {
        return (date) ? this._weekdaysShort[Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["b" /* getDayOfWeek */])(date)] : this._weekdaysShort;
    };
    Locale.prototype.weekdaysShort = function (date) {
        return (date) ? this._weekdaysMin[Object(__WEBPACK_IMPORTED_MODULE_2__utils_date_getters__["b" /* getDayOfWeek */])(date)] : this._weekdaysMin;
    };
    Locale.prototype.week = function (date) {
        return Object(__WEBPACK_IMPORTED_MODULE_0__units_week_calendar_utils__["a" /* weekOfYear */])(date, this._week.dow, this._week.doy).week;
    };
    Locale.prototype.firstDayOfWeek = function () {
        return this._week.dow;
    };
    Locale.prototype.firstDayOfYear = function () {
        return this._week.doy;
    };
    Locale.prototype.meridiem = function (hours, minutes, isLower) {
        if (hours > 11) {
            return isLower ? 'pm' : 'PM';
        }
        return isLower ? 'am' : 'AM';
    };
    Locale.prototype.ordinal = function (num, token) {
        return this._ordinal.replace('%d', num.toString(10));
    };
    Locale.prototype.preparse = function (str) { return str; };
    Locale.prototype.postformat = function (str) { return str; };
    Locale.prototype.longDateFormat = function (key) {
        var format = defaultLongDateFormat[key];
        var formatUpper = defaultLongDateFormat[key.toUpperCase()];
        if (format || !formatUpper) {
            return format;
        }
        defaultLongDateFormat[key] = formatUpper.replace(/MMMM|MM|DD|dddd/g, function (val) {
            return val.slice(1);
        });
        return defaultLongDateFormat[key];
    };
    return Locale;
}());



/***/ }),
/* 78 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = getDayOfYear;
/* unused harmony export _getDayOfYear */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_start_end_of__ = __webpack_require__(17);


// FORMATTING
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('DDD', ['DDDD', 3], 'DDDo', function (date) {
    return getDayOfYear(date).toString(10);
});
function getDayOfYear(date) {
    var date1 = +Object(__WEBPACK_IMPORTED_MODULE_1__utils_start_end_of__["b" /* startOf */])(date, 'day');
    var date2 = +Object(__WEBPACK_IMPORTED_MODULE_1__utils_start_end_of__["b" /* startOf */])(date, 'year');
    var someDate = date1 - date2;
    var oneDay = 1000 * 60 * 60 * 24;
    return Math.round(someDate / oneDay) + 1;
}
function _getDayOfYear(date) {
    var start = new Date(date.getFullYear(), 0, 0);
    var diff = date.getTime() - start.getTime();
    var oneDay = 1000 * 60 * 60 * 24;
    return Math.round(diff / oneDay) + 1;
}


/***/ }),
/* 79 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export DATEPICKER_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DatePickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__ = __webpack_require__(15);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__datepicker_config__ = __webpack_require__(41);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




var DATEPICKER_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return DatePickerComponent; }),
    multi: true
};
/* tslint:disable:component-selector-name component-selector-type */
var DatePickerComponent = (function () {
    function DatePickerComponent(config) {
        /** sets datepicker mode, supports: `day`, `month`, `year` */
        this.datepickerMode = 'day';
        /** if false week numbers will be hidden */
        this.showWeeks = true;
        this.selectionDone = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"](undefined);
        /** callback to invoke when the activeDate is changed. */
        this.activeDateChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"](undefined);
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
        this._now = new Date();
        this.config = config;
        this.configureOptions();
    }
    Object.defineProperty(DatePickerComponent.prototype, "activeDate", {
        /** currently active date */
        get: function () {
            return this._activeDate || this._now;
        },
        set: function (value) {
            this._activeDate = value;
        },
        enumerable: true,
        configurable: true
    });
    DatePickerComponent.prototype.configureOptions = function () {
        Object.assign(this, this.config);
    };
    DatePickerComponent.prototype.onUpdate = function (event) {
        this.activeDate = event;
        this.onChange(event);
    };
    DatePickerComponent.prototype.onSelectionDone = function (event) {
        this.selectionDone.emit(event);
    };
    DatePickerComponent.prototype.onActiveDateChange = function (event) {
        this.activeDateChange.emit(event);
    };
    // todo: support null value
    DatePickerComponent.prototype.writeValue = function (value) {
        if (this._datePicker.compare(value, this._activeDate) === 0) {
            return;
        }
        if (value && value instanceof Date) {
            this.activeDate = value;
            this._datePicker.select(value, false);
            return;
        }
        this.activeDate = value ? new Date(value) : void 0;
    };
    DatePickerComponent.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    DatePickerComponent.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "datepickerMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], DatePickerComponent.prototype, "initDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], DatePickerComponent.prototype, "minDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], DatePickerComponent.prototype, "maxDate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "minMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "maxMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], DatePickerComponent.prototype, "showWeeks", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "formatDay", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "formatMonth", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "formatYear", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "formatDayHeader", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "formatDayTitle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], DatePickerComponent.prototype, "formatMonthTitle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerComponent.prototype, "startingDay", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerComponent.prototype, "yearRange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], DatePickerComponent.prototype, "onlyCurrentMonth", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], DatePickerComponent.prototype, "shortcutPropagation", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerComponent.prototype, "monthColLimit", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], DatePickerComponent.prototype, "yearColLimit", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], DatePickerComponent.prototype, "customClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], DatePickerComponent.prototype, "dateDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date),
        __metadata("design:paramtypes", [Date])
    ], DatePickerComponent.prototype, "activeDate", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], DatePickerComponent.prototype, "selectionDone", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], DatePickerComponent.prototype, "activeDateChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewChild"])(__WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__["a" /* DatePickerInnerComponent */]),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__["a" /* DatePickerInnerComponent */])
    ], DatePickerComponent.prototype, "_datePicker", void 0);
    DatePickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'datepicker',
            template: "\n    <datepicker-inner [activeDate]=\"activeDate\"\n                      (update)=\"onUpdate($event)\"\n                      [locale]=\"config.locale\"\n                      [datepickerMode]=\"datepickerMode\"\n                      [initDate]=\"initDate\"\n                      [minDate]=\"minDate\"\n                      [maxDate]=\"maxDate\"\n                      [minMode]=\"minMode\"\n                      [maxMode]=\"maxMode\"\n                      [showWeeks]=\"showWeeks\"\n                      [formatDay]=\"formatDay\"\n                      [formatMonth]=\"formatMonth\"\n                      [formatYear]=\"formatYear\"\n                      [formatDayHeader]=\"formatDayHeader\"\n                      [formatDayTitle]=\"formatDayTitle\"\n                      [formatMonthTitle]=\"formatMonthTitle\"\n                      [startingDay]=\"startingDay\"\n                      [yearRange]=\"yearRange\"\n                      [customClass]=\"customClass\"\n                      [dateDisabled]=\"dateDisabled\"\n                      [onlyCurrentMonth]=\"onlyCurrentMonth\"\n                      [shortcutPropagation]=\"shortcutPropagation\"\n                      [monthColLimit]=\"monthColLimit\"\n                      [yearColLimit]=\"yearColLimit\"\n                      (selectionDone)=\"onSelectionDone($event)\"\n                      (activeDateChange)=\"onActiveDateChange($event)\">\n      <daypicker tabindex=\"0\"></daypicker>\n      <monthpicker tabindex=\"0\"></monthpicker>\n      <yearpicker tabindex=\"0\"></yearpicker>\n    </datepicker-inner>\n    ",
            providers: [DATEPICKER_CONTROL_VALUE_ACCESSOR]
        })
        /* tslint:enable:component-selector-name component-selector-type */
        ,
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_3__datepicker_config__["a" /* DatepickerConfig */]])
    ], DatePickerComponent);
    return DatePickerComponent;
}());



/***/ }),
/* 80 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DayPickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__ = __webpack_require__(15);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var DayPickerComponent = (function () {
    function DayPickerComponent(datePicker) {
        this.labels = [];
        this.rows = [];
        this.weekNumbers = [];
        this.datePicker = datePicker;
    }
    Object.defineProperty(DayPickerComponent.prototype, "isBs4", {
        get: function () {
            return !Object(__WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    /*protected getDaysInMonth(year:number, month:number) {
     return ((month === 1) && (year % 4 === 0) &&
     ((year % 100 !== 0) || (year % 400 === 0))) ? 29 : DAYS_IN_MONTH[month];
     }*/
    DayPickerComponent.prototype.ngOnInit = function () {
        var self = this;
        this.datePicker.stepDay = { months: 1 };
        this.datePicker.setRefreshViewHandler(function () {
            var year = this.activeDate.getFullYear();
            var month = this.activeDate.getMonth();
            var firstDayOfMonth = new Date(year, month, 1);
            var difference = this.startingDay - firstDayOfMonth.getDay();
            var numDisplayedFromPreviousMonth = (difference > 0)
                ? 7 - difference
                : -difference;
            var firstDate = new Date(firstDayOfMonth.getTime());
            if (numDisplayedFromPreviousMonth > 0) {
                firstDate.setDate(-numDisplayedFromPreviousMonth + 1);
            }
            // 42 is the number of days on a six-week calendar
            var _days = self.getDates(firstDate, 42);
            var days = [];
            for (var i = 0; i < 42; i++) {
                var _dateObject = this.createDateObject(_days[i], this.formatDay);
                _dateObject.secondary = _days[i].getMonth() !== month;
                _dateObject.uid = this.uniqueId + '-' + i;
                days[i] = _dateObject;
            }
            self.labels = [];
            for (var j = 0; j < 7; j++) {
                self.labels[j] = {};
                self.labels[j].abbr = this.dateFilter(days[j].date, this.formatDayHeader);
                self.labels[j].full = this.dateFilter(days[j].date, 'EEEE');
            }
            self.title = this.dateFilter(this.activeDate, this.formatDayTitle);
            self.rows = this.split(days, 7);
            if (this.showWeeks) {
                self.weekNumbers = [];
                var thursdayIndex = (4 + 7 - this.startingDay) % 7;
                var numWeeks = self.rows.length;
                for (var curWeek = 0; curWeek < numWeeks; curWeek++) {
                    self.weekNumbers.push(self.getISO8601WeekNumber(self.rows[curWeek][thursdayIndex].date));
                }
            }
        }, 'day');
        this.datePicker.setCompareHandler(function (date1, date2) {
            var d1 = new Date(date1.getFullYear(), date1.getMonth(), date1.getDate());
            var d2 = new Date(date2.getFullYear(), date2.getMonth(), date2.getDate());
            return d1.getTime() - d2.getTime();
        }, 'day');
        this.datePicker.refreshView();
    };
    DayPickerComponent.prototype.getDates = function (startDate, n) {
        var dates = new Array(n);
        var current = new Date(startDate.getTime());
        var i = 0;
        var date;
        while (i < n) {
            date = new Date(current.getTime());
            date = this.datePicker.fixTimeZone(date);
            dates[i++] = date;
            current = new Date(current.getFullYear(), current.getMonth(), current.getDate() + 1);
        }
        return dates;
    };
    DayPickerComponent.prototype.getISO8601WeekNumber = function (date) {
        var checkDate = new Date(date.getTime());
        // Thursday
        checkDate.setDate(checkDate.getDate() + 4 - (checkDate.getDay() || 7));
        var time = checkDate.getTime();
        // Compare with Jan 1
        checkDate.setMonth(0);
        checkDate.setDate(1);
        return Math.floor(Math.round((time - checkDate.getTime()) / 86400000) / 7) + 1;
    };
    DayPickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'daypicker',
            template: "\n<table *ngIf=\"datePicker.datepickerMode==='day'\" role=\"grid\" [attr.aria-labelledby]=\"datePicker.uniqueId+'-title'\" aria-activedescendant=\"activeDateId\">\n  <thead>\n    <tr>\n      <th>\n        <button *ngIf=\"!isBs4\"\n                type=\"button\"\n                class=\"btn btn-default btn-secondary btn-sm pull-left float-left\"\n                (click)=\"datePicker.move(-1)\"\n                tabindex=\"-1\">\n          <i class=\"glyphicon glyphicon-chevron-left\"></i>\n        </button>\n        <button *ngIf=\"isBs4\"\n                type=\"button\"\n                class=\"btn btn-default btn-secondary btn-sm pull-left float-left\"\n                (click)=\"datePicker.move(-1)\"\n                tabindex=\"-1\">&lt;\n        </button>\n      </th>\n      <th [attr.colspan]=\"5 + (datePicker.showWeeks ? 1 : 0)\">\n        <button [id]=\"datePicker.uniqueId + '-title'\"\n                type=\"button\" class=\"btn btn-default btn-secondary btn-sm\"\n                (click)=\"datePicker.toggleMode()\"\n                [disabled]=\"datePicker.datepickerMode === datePicker.maxMode\"\n                [ngClass]=\"{disabled: datePicker.datepickerMode === datePicker.maxMode}\" tabindex=\"-1\" style=\"width:100%;\">\n          <strong>{{title}}</strong>\n        </button>\n      </th>\n      <th>\n        <button *ngIf=\"!isBs4\"\n                type=\"button\"\n                class=\"btn btn-default btn-secondary btn-sm pull-right float-right\"\n                (click)=\"datePicker.move(1)\"\n                tabindex=\"-1\">\n          <i  class=\"glyphicon glyphicon-chevron-right\"></i>\n        </button>\n        <button *ngIf=\"isBs4\"\n                type=\"button\"\n                class=\"btn btn-default btn-secondary btn-sm pull-right float-right\"\n                (click)=\"datePicker.move(1)\"\n                tabindex=\"-1\">&gt;\n        </button>\n      </th>\n    </tr>\n    <tr>\n      <th *ngIf=\"datePicker.showWeeks\"></th>\n      <th *ngFor=\"let labelz of labels\" class=\"text-center\">\n        <small aria-label=\"labelz.full\"><b>{{labelz.abbr}}</b></small>\n      </th>\n    </tr>\n  </thead>\n  <tbody>\n    <template ngFor [ngForOf]=\"rows\" let-rowz=\"$implicit\" let-index=\"index\">\n      <tr *ngIf=\"!(datePicker.onlyCurrentMonth && rowz[0].secondary && rowz[6].secondary)\">\n        <td *ngIf=\"datePicker.showWeeks\" class=\"h6\" class=\"text-center\">\n          <em>{{ weekNumbers[index] }}</em>\n        </td>\n        <td *ngFor=\"let dtz of rowz\" class=\"text-center\" role=\"gridcell\" [id]=\"dtz.uid\">\n          <button type=\"button\" style=\"min-width:100%;\" class=\"btn btn-sm {{dtz.customClass}}\"\n                  *ngIf=\"!(datePicker.onlyCurrentMonth && dtz.secondary)\"\n                  [ngClass]=\"{'btn-secondary': isBs4 && !dtz.selected && !datePicker.isActive(dtz), 'btn-info': dtz.selected, disabled: dtz.disabled, active: !isBs4 && datePicker.isActive(dtz), 'btn-default': !isBs4}\"\n                  [disabled]=\"dtz.disabled\"\n                  (click)=\"datePicker.select(dtz.date)\" tabindex=\"-1\">\n            <span [ngClass]=\"{'text-muted': dtz.secondary || dtz.current, 'text-info': !isBs4 && dtz.current}\">{{dtz.label}}</span>\n          </button>\n        </td>\n      </tr>\n    </template>\n  </tbody>\n</table>\n  ",
            styles: ["\n    :host .btn-secondary {\n      color: #292b2c;\n      background-color: #fff;\n      border-color: #ccc;\n    }\n    :host .btn-info .text-muted {\n      color: #292b2c !important;\n    }\n  "]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__["a" /* DatePickerInnerComponent */]])
    ], DayPickerComponent);
    return DayPickerComponent;
}());



/***/ }),
/* 81 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return MonthPickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__ = __webpack_require__(15);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var MonthPickerComponent = (function () {
    function MonthPickerComponent(datePicker) {
        this.rows = [];
        this.datePicker = datePicker;
    }
    Object.defineProperty(MonthPickerComponent.prototype, "isBs4", {
        get: function () {
            return !Object(__WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    MonthPickerComponent.prototype.ngOnInit = function () {
        var self = this;
        this.datePicker.stepMonth = { years: 1 };
        this.datePicker.setRefreshViewHandler(function () {
            var months = new Array(12);
            var year = this.activeDate.getFullYear();
            var date;
            for (var i = 0; i < 12; i++) {
                date = new Date(year, i, 1);
                date = this.fixTimeZone(date);
                months[i] = this.createDateObject(date, this.formatMonth);
                months[i].uid = this.uniqueId + '-' + i;
            }
            self.title = this.dateFilter(this.activeDate, this.formatMonthTitle);
            self.rows = this.split(months, self.datePicker.monthColLimit);
        }, 'month');
        this.datePicker.setCompareHandler(function (date1, date2) {
            var d1 = new Date(date1.getFullYear(), date1.getMonth());
            var d2 = new Date(date2.getFullYear(), date2.getMonth());
            return d1.getTime() - d2.getTime();
        }, 'month');
        this.datePicker.refreshView();
    };
    MonthPickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'monthpicker',
            template: "\n<table *ngIf=\"datePicker.datepickerMode==='month'\" role=\"grid\">\n  <thead>\n    <tr>\n      <th>\n        <button type=\"button\" class=\"btn btn-default btn-sm pull-left float-left\"\n                (click)=\"datePicker.move(-1)\" tabindex=\"-1\">\n          <i class=\"glyphicon glyphicon-chevron-left\"></i>\n        </button></th>\n      <th [attr.colspan]=\"((datePicker.monthColLimit - 2) <= 0) ? 1 : datePicker.monthColLimit - 2\">\n        <button [id]=\"datePicker.uniqueId + '-title'\"\n                type=\"button\" class=\"btn btn-default btn-sm\"\n                (click)=\"datePicker.toggleMode()\"\n                [disabled]=\"datePicker.datepickerMode === maxMode\"\n                [ngClass]=\"{disabled: datePicker.datepickerMode === maxMode}\" tabindex=\"-1\" style=\"width:100%;\">\n          <strong>{{title}}</strong>\n        </button>\n      </th>\n      <th>\n        <button type=\"button\" class=\"btn btn-default btn-sm pull-right float-right\"\n                (click)=\"datePicker.move(1)\" tabindex=\"-1\">\n          <i class=\"glyphicon glyphicon-chevron-right\"></i>\n        </button>\n      </th>\n    </tr>\n  </thead>\n  <tbody>\n    <tr *ngFor=\"let rowz of rows\">\n      <td *ngFor=\"let dtz of rowz\" class=\"text-center\" role=\"gridcell\" id=\"{{dtz.uid}}\" [ngClass]=\"dtz.customClass\">\n        <button type=\"button\" style=\"min-width:100%;\" class=\"btn btn-default\"\n                [ngClass]=\"{'btn-link': isBs4 && !dtz.selected && !datePicker.isActive(dtz), 'btn-info': dtz.selected || (isBs4 && !dtz.selected && datePicker.isActive(dtz)), disabled: dtz.disabled, active: !isBs4 && datePicker.isActive(dtz)}\"\n                [disabled]=\"dtz.disabled\"\n                (click)=\"datePicker.select(dtz.date)\" tabindex=\"-1\">\n          <span [ngClass]=\"{'text-success': isBs4 && dtz.current, 'text-info': !isBs4 && dtz.current}\">{{dtz.label}}</span>\n        </button>\n      </td>\n    </tr>\n  </tbody>\n</table>\n  ",
            styles: ["\n    :host .btn-info .text-success {\n      color: #fff !important;\n    }\n  "]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__["a" /* DatePickerInnerComponent */]])
    ], MonthPickerComponent);
    return MonthPickerComponent;
}());



/***/ }),
/* 82 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return YearPickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__ = __webpack_require__(15);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var YearPickerComponent = (function () {
    function YearPickerComponent(datePicker) {
        this.rows = [];
        this.datePicker = datePicker;
    }
    Object.defineProperty(YearPickerComponent.prototype, "isBs4", {
        get: function () {
            return !Object(__WEBPACK_IMPORTED_MODULE_1__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    YearPickerComponent.prototype.ngOnInit = function () {
        var self = this;
        this.datePicker.stepYear = { years: this.datePicker.yearRange };
        this.datePicker.setRefreshViewHandler(function () {
            var years = new Array(this.yearRange);
            var date;
            var start = self.getStartingYear(this.activeDate.getFullYear());
            for (var i = 0; i < this.yearRange; i++) {
                date = new Date(start + i, 0, 1);
                date = this.fixTimeZone(date);
                years[i] = this.createDateObject(date, this.formatYear);
                years[i].uid = this.uniqueId + '-' + i;
            }
            self.title = [years[0].label,
                years[this.yearRange - 1].label].join(' - ');
            self.rows = this.split(years, self.datePicker.yearColLimit);
        }, 'year');
        this.datePicker.setCompareHandler(function (date1, date2) {
            return date1.getFullYear() - date2.getFullYear();
        }, 'year');
        this.datePicker.refreshView();
    };
    YearPickerComponent.prototype.getStartingYear = function (year) {
        // todo: parseInt
        return ((year - 1) / this.datePicker.yearRange) * this.datePicker.yearRange + 1;
    };
    YearPickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'yearpicker',
            template: "\n<table *ngIf=\"datePicker.datepickerMode==='year'\" role=\"grid\">\n  <thead>\n    <tr>\n      <th>\n        <button type=\"button\" class=\"btn btn-default btn-sm pull-left float-left\"\n                (click)=\"datePicker.move(-1)\" tabindex=\"-1\">\n          <i class=\"glyphicon glyphicon-chevron-left\"></i>\n        </button>\n      </th>\n      <th [attr.colspan]=\"((datePicker.yearColLimit - 2) <= 0) ? 1 : datePicker.yearColLimit - 2\">\n        <button [id]=\"datePicker.uniqueId + '-title'\" role=\"heading\"\n                type=\"button\" class=\"btn btn-default btn-sm\"\n                (click)=\"datePicker.toggleMode()\"\n                [disabled]=\"datePicker.datepickerMode === datePicker.maxMode\"\n                [ngClass]=\"{disabled: datePicker.datepickerMode === datePicker.maxMode}\" tabindex=\"-1\" style=\"width:100%;\">\n          <strong>{{title}}</strong>\n        </button>\n      </th>\n      <th>\n        <button type=\"button\" class=\"btn btn-default btn-sm pull-right float-right\"\n                (click)=\"datePicker.move(1)\" tabindex=\"-1\">\n          <i class=\"glyphicon glyphicon-chevron-right\"></i>\n        </button>\n      </th>\n    </tr>\n  </thead>\n  <tbody>\n    <tr *ngFor=\"let rowz of rows\">\n      <td *ngFor=\"let dtz of rowz\" class=\"text-center\" role=\"gridcell\">\n        <button type=\"button\" style=\"min-width:100%;\" class=\"btn btn-default\"\n                [ngClass]=\"{'btn-link': isBs4 && !dtz.selected && !datePicker.isActive(dtz), 'btn-info': dtz.selected || (isBs4 && !dtz.selected && datePicker.isActive(dtz)), disabled: dtz.disabled, active: !isBs4 && datePicker.isActive(dtz)}\"\n                [disabled]=\"dtz.disabled\"\n                (click)=\"datePicker.select(dtz.date)\" tabindex=\"-1\">\n          <span [ngClass]=\"{'text-success': isBs4 && dtz.current, 'text-info': !isBs4 && dtz.current}\">{{dtz.label}}</span>\n        </button>\n      </td>\n    </tr>\n  </tbody>\n</table>\n  ",
            styles: ["\n    :host .btn-info .text-success {\n      color: #fff !important;\n    }\n  "]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__datepicker_inner_component__["a" /* DatePickerInnerComponent */]])
    ], YearPickerComponent);
    return YearPickerComponent;
}());



/***/ }),
/* 83 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__positioning__ = __webpack_require__(9);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_dropdown_container_component__ = __webpack_require__(42);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_dropdown_menu_directive__ = __webpack_require__(88);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_toggle_directive__ = __webpack_require__(89);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__bs_dropdown_config__ = __webpack_require__(43);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__bs_dropdown_directive__ = __webpack_require__(90);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__bs_dropdown_state__ = __webpack_require__(14);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};









var BsDropdownModule = (function () {
    function BsDropdownModule() {
    }
    BsDropdownModule_1 = BsDropdownModule;
    BsDropdownModule.forRoot = function (config) {
        return {
            ngModule: BsDropdownModule_1, providers: [
                __WEBPACK_IMPORTED_MODULE_1__component_loader__["b" /* ComponentLoaderFactory */],
                __WEBPACK_IMPORTED_MODULE_2__positioning__["b" /* PositioningService */],
                __WEBPACK_IMPORTED_MODULE_8__bs_dropdown_state__["a" /* BsDropdownState */],
                { provide: __WEBPACK_IMPORTED_MODULE_6__bs_dropdown_config__["a" /* BsDropdownConfig */], useValue: config ? config : { autoClose: true } }
            ]
        };
    };
    ;
    BsDropdownModule = BsDropdownModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            declarations: [
                __WEBPACK_IMPORTED_MODULE_4__bs_dropdown_menu_directive__["a" /* BsDropdownMenuDirective */],
                __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_toggle_directive__["a" /* BsDropdownToggleDirective */],
                __WEBPACK_IMPORTED_MODULE_3__bs_dropdown_container_component__["a" /* BsDropdownContainerComponent */],
                __WEBPACK_IMPORTED_MODULE_7__bs_dropdown_directive__["a" /* BsDropdownDirective */]
            ],
            exports: [
                __WEBPACK_IMPORTED_MODULE_4__bs_dropdown_menu_directive__["a" /* BsDropdownMenuDirective */],
                __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_toggle_directive__["a" /* BsDropdownToggleDirective */],
                __WEBPACK_IMPORTED_MODULE_7__bs_dropdown_directive__["a" /* BsDropdownDirective */]
            ],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_3__bs_dropdown_container_component__["a" /* BsDropdownContainerComponent */]]
        })
    ], BsDropdownModule);
    return BsDropdownModule;
    var BsDropdownModule_1;
}());



/***/ }),
/* 84 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ComponentLoader; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_triggers__ = __webpack_require__(141);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__content_ref_class__ = __webpack_require__(85);
// todo: add delay support
// todo: merge events onShow, onShown, etc...
// todo: add global positioning configuration?



var ComponentLoader = (function () {
    /**
     * Do not use this directly, it should be instanced via
     * `ComponentLoadFactory.attach`
     * @internal
     */
    // tslint:disable-next-line
    function ComponentLoader(_viewContainerRef, _renderer, _elementRef, _injector, _componentFactoryResolver, _ngZone, _applicationRef, _posService) {
        this._viewContainerRef = _viewContainerRef;
        this._renderer = _renderer;
        this._elementRef = _elementRef;
        this._injector = _injector;
        this._componentFactoryResolver = _componentFactoryResolver;
        this._ngZone = _ngZone;
        this._applicationRef = _applicationRef;
        this._posService = _posService;
        this.onBeforeShow = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onShown = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onBeforeHide = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onHidden = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this._providers = [];
        this._listenOpts = {};
        this._globalListener = Function.prototype;
    }
    Object.defineProperty(ComponentLoader.prototype, "isShown", {
        get: function () {
            return !!this._componentRef;
        },
        enumerable: true,
        configurable: true
    });
    ;
    ComponentLoader.prototype.attach = function (compType) {
        this._componentFactory = this._componentFactoryResolver
            .resolveComponentFactory(compType);
        return this;
    };
    // todo: add behaviour: to target element, `body`, custom element
    ComponentLoader.prototype.to = function (container) {
        this.container = container || this.container;
        return this;
    };
    ComponentLoader.prototype.position = function (opts) {
        this.attachment = opts.attachment || this.attachment;
        this._elementRef = opts.target || this._elementRef;
        return this;
    };
    ComponentLoader.prototype.provide = function (provider) {
        this._providers.push(provider);
        return this;
    };
    // todo: appendChild to element or document.querySelector(this.container)
    ComponentLoader.prototype.show = function (opts) {
        if (opts === void 0) { opts = {}; }
        this._subscribePositioning();
        this._innerComponent = null;
        if (!this._componentRef) {
            this.onBeforeShow.emit();
            this._contentRef = this._getContentRef(opts.content, opts.context);
            var injector = __WEBPACK_IMPORTED_MODULE_0__angular_core__["ReflectiveInjector"].resolveAndCreate(this._providers, this._injector);
            this._componentRef = this._componentFactory.create(injector, this._contentRef.nodes);
            this._applicationRef.attachView(this._componentRef.hostView);
            // this._componentRef = this._viewContainerRef
            //   .createComponent(this._componentFactory, 0, injector, this._contentRef.nodes);
            this.instance = this._componentRef.instance;
            Object.assign(this._componentRef.instance, opts);
            if (this.container instanceof __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]) {
                this.container.nativeElement
                    .appendChild(this._componentRef.location.nativeElement);
            }
            if (this.container === 'body' && typeof document !== 'undefined') {
                document.querySelector(this.container)
                    .appendChild(this._componentRef.location.nativeElement);
            }
            if (!this.container && this._elementRef && this._elementRef.nativeElement.parentElement) {
                this._elementRef.nativeElement.parentElement
                    .appendChild(this._componentRef.location.nativeElement);
            }
            // we need to manually invoke change detection since events registered
            // via
            // Renderer::listen() are not picked up by change detection with the
            // OnPush strategy
            if (this._contentRef.componentRef) {
                this._innerComponent = this._contentRef.componentRef.instance;
                this._contentRef.componentRef.changeDetectorRef.markForCheck();
                this._contentRef.componentRef.changeDetectorRef.detectChanges();
            }
            this._componentRef.changeDetectorRef.markForCheck();
            this._componentRef.changeDetectorRef.detectChanges();
            this.onShown.emit(this._componentRef.instance);
        }
        this._registerOutsideClick();
        return this._componentRef;
    };
    ComponentLoader.prototype.hide = function () {
        if (!this._componentRef) {
            return this;
        }
        this.onBeforeHide.emit(this._componentRef.instance);
        var componentEl = this._componentRef.location.nativeElement;
        componentEl.parentNode.removeChild(componentEl);
        if (this._contentRef.componentRef) {
            this._contentRef.componentRef.destroy();
        }
        this._componentRef.destroy();
        if (this._viewContainerRef && this._contentRef.viewRef) {
            this._viewContainerRef.remove(this._viewContainerRef.indexOf(this._contentRef.viewRef));
        }
        // this._viewContainerRef.remove(this._viewContainerRef.indexOf(this._componentRef.hostView));
        //
        // if (this._contentRef.viewRef && this._viewContainerRef.indexOf(this._contentRef.viewRef) !== -1) {
        //   this._viewContainerRef.remove(this._viewContainerRef.indexOf(this._contentRef.viewRef));
        // }
        this._contentRef = null;
        this._componentRef = null;
        this._removeGlobalListener();
        this.onHidden.emit();
        return this;
    };
    ComponentLoader.prototype.toggle = function () {
        if (this.isShown) {
            this.hide();
            return;
        }
        this.show();
    };
    ComponentLoader.prototype.dispose = function () {
        if (this.isShown) {
            this.hide();
        }
        this._unsubscribePositioning();
        if (this._unregisterListenersFn) {
            this._unregisterListenersFn();
        }
    };
    ComponentLoader.prototype.listen = function (listenOpts) {
        var _this = this;
        this.triggers = listenOpts.triggers || this.triggers;
        this._listenOpts.outsideClick = listenOpts.outsideClick;
        listenOpts.target = listenOpts.target || this._elementRef.nativeElement;
        var hide = this._listenOpts.hide = function () { return listenOpts.hide ? listenOpts.hide() : _this.hide(); };
        var show = this._listenOpts.show = function (registerHide) {
            listenOpts.show ? listenOpts.show(registerHide) : _this.show(registerHide);
            registerHide();
        };
        var toggle = function (registerHide) {
            _this.isShown ? hide() : show(registerHide);
        };
        this._unregisterListenersFn = Object(__WEBPACK_IMPORTED_MODULE_1__utils_triggers__["a" /* listenToTriggersV2 */])(this._renderer, {
            target: listenOpts.target,
            triggers: listenOpts.triggers,
            show: show, hide: hide, toggle: toggle
        });
        return this;
    };
    ComponentLoader.prototype._removeGlobalListener = function () {
        if (this._globalListener) {
            this._globalListener();
            this._globalListener = null;
        }
    };
    ComponentLoader.prototype.attachInline = function (vRef, template) {
        this._inlineViewRef = vRef.createEmbeddedView(template);
        return this;
    };
    ComponentLoader.prototype._registerOutsideClick = function () {
        var _this = this;
        if (!this._componentRef || !this._componentRef.location) {
            return;
        }
        // why: should run after first event bubble
        if (this._listenOpts.outsideClick) {
            var target_1 = this._componentRef.location.nativeElement;
            setTimeout(function () {
                _this._globalListener = Object(__WEBPACK_IMPORTED_MODULE_1__utils_triggers__["b" /* registerOutsideClick */])(_this._renderer, {
                    targets: [target_1, _this._elementRef.nativeElement],
                    outsideClick: _this._listenOpts.outsideClick,
                    hide: function () { return _this._listenOpts.hide(); }
                });
            });
        }
    };
    ComponentLoader.prototype.getInnerComponent = function () {
        return this._innerComponent;
    };
    ComponentLoader.prototype._subscribePositioning = function () {
        var _this = this;
        if (this._zoneSubscription || !this.attachment) {
            return;
        }
        this._zoneSubscription = this._ngZone
            .onStable.subscribe(function () {
            if (!_this._componentRef) {
                return;
            }
            _this._posService.position({
                element: _this._componentRef.location,
                target: _this._elementRef,
                attachment: _this.attachment,
                appendToBody: _this.container === 'body'
            });
        });
    };
    ComponentLoader.prototype._unsubscribePositioning = function () {
        if (!this._zoneSubscription) {
            return;
        }
        this._zoneSubscription.unsubscribe();
        this._zoneSubscription = null;
    };
    ComponentLoader.prototype._getContentRef = function (content, context) {
        if (!content) {
            return new __WEBPACK_IMPORTED_MODULE_2__content_ref_class__["a" /* ContentRef */]([]);
        }
        if (content instanceof __WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"]) {
            if (this._viewContainerRef) {
                var viewRef_1 = this._viewContainerRef.createEmbeddedView(content, context);
                viewRef_1.markForCheck();
                return new __WEBPACK_IMPORTED_MODULE_2__content_ref_class__["a" /* ContentRef */]([viewRef_1.rootNodes], viewRef_1);
            }
            var viewRef = content.createEmbeddedView({});
            this._applicationRef.attachView(viewRef);
            return new __WEBPACK_IMPORTED_MODULE_2__content_ref_class__["a" /* ContentRef */]([viewRef.rootNodes], viewRef);
        }
        if (typeof content === 'function') {
            var contentCmptFactory = this._componentFactoryResolver.resolveComponentFactory(content);
            var modalContentInjector = __WEBPACK_IMPORTED_MODULE_0__angular_core__["ReflectiveInjector"].resolveAndCreate(this._providers.concat([content]), this._injector);
            var componentRef = contentCmptFactory.create(modalContentInjector);
            this._applicationRef.attachView(componentRef.hostView);
            return new __WEBPACK_IMPORTED_MODULE_2__content_ref_class__["a" /* ContentRef */]([[componentRef.location.nativeElement]], componentRef.hostView, componentRef);
        }
        return new __WEBPACK_IMPORTED_MODULE_2__content_ref_class__["a" /* ContentRef */]([[this._renderer.createText(null, "" + content)]]);
    };
    return ComponentLoader;
}());



/***/ }),
/* 85 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ContentRef; });
/**
 * @copyright Valor Software
 * @copyright Angular ng-bootstrap team
 */
var ContentRef = (function () {
    function ContentRef(nodes, viewRef, componentRef) {
        this.nodes = nodes;
        this.viewRef = viewRef;
        this.componentRef = componentRef;
    }
    return ContentRef;
}());



/***/ }),
/* 86 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Positioning; });
/* harmony export (immutable) */ __webpack_exports__["b"] = positionElements;
/**
 * @copyright Valor Software
 * @copyright Angular ng-bootstrap team
 */
// previous version:
// https://github.com/angular-ui/bootstrap/blob/07c31d0731f7cb068a1932b8e01d2312b796b4ec/src/position/position.js
// tslint:disable
var Positioning = (function () {
    function Positioning() {
    }
    Positioning.prototype.position = function (element, round) {
        if (round === void 0) { round = true; }
        var elPosition;
        var parentOffset = { width: 0, height: 0, top: 0, bottom: 0, left: 0, right: 0 };
        if (this.getStyle(element, 'position') === 'fixed') {
            var bcRect = element.getBoundingClientRect();
            elPosition = {
                width: bcRect.width,
                height: bcRect.height,
                top: bcRect.top,
                bottom: bcRect.bottom,
                left: bcRect.left,
                right: bcRect.right
            };
        }
        else {
            var offsetParentEl = this.offsetParent(element);
            elPosition = this.offset(element, false);
            if (offsetParentEl !== document.documentElement) {
                parentOffset = this.offset(offsetParentEl, false);
            }
            parentOffset.top += offsetParentEl.clientTop;
            parentOffset.left += offsetParentEl.clientLeft;
        }
        elPosition.top -= parentOffset.top;
        elPosition.bottom -= parentOffset.top;
        elPosition.left -= parentOffset.left;
        elPosition.right -= parentOffset.left;
        if (round) {
            elPosition.top = Math.round(elPosition.top);
            elPosition.bottom = Math.round(elPosition.bottom);
            elPosition.left = Math.round(elPosition.left);
            elPosition.right = Math.round(elPosition.right);
        }
        return elPosition;
    };
    Positioning.prototype.offset = function (element, round) {
        if (round === void 0) { round = true; }
        var elBcr = element.getBoundingClientRect();
        var viewportOffset = {
            top: window.pageYOffset - document.documentElement.clientTop,
            left: window.pageXOffset - document.documentElement.clientLeft
        };
        var elOffset = {
            height: elBcr.height || element.offsetHeight,
            width: elBcr.width || element.offsetWidth,
            top: elBcr.top + viewportOffset.top,
            bottom: elBcr.bottom + viewportOffset.top,
            left: elBcr.left + viewportOffset.left,
            right: elBcr.right + viewportOffset.left
        };
        if (round) {
            elOffset.height = Math.round(elOffset.height);
            elOffset.width = Math.round(elOffset.width);
            elOffset.top = Math.round(elOffset.top);
            elOffset.bottom = Math.round(elOffset.bottom);
            elOffset.left = Math.round(elOffset.left);
            elOffset.right = Math.round(elOffset.right);
        }
        return elOffset;
    };
    Positioning.prototype.positionElements = function (hostElement, targetElement, placement, appendToBody) {
        var hostElPosition = appendToBody ? this.offset(hostElement, false) : this.position(hostElement, false);
        var targetElStyles = this.getAllStyles(targetElement);
        var shiftWidth = {
            left: hostElPosition.left,
            center: hostElPosition.left + hostElPosition.width / 2 - targetElement.offsetWidth / 2,
            right: hostElPosition.left + hostElPosition.width
        };
        var shiftHeight = {
            top: hostElPosition.top,
            center: hostElPosition.top + hostElPosition.height / 2 - targetElement.offsetHeight / 2,
            bottom: hostElPosition.top + hostElPosition.height
        };
        var targetElBCR = targetElement.getBoundingClientRect();
        var placementPrimary = placement.split(' ')[0] || 'top';
        var placementSecondary = placement.split(' ')[1] || 'center';
        var targetElPosition = {
            height: targetElBCR.height || targetElement.offsetHeight,
            width: targetElBCR.width || targetElement.offsetWidth,
            top: 0,
            bottom: targetElBCR.height || targetElement.offsetHeight,
            left: 0,
            right: targetElBCR.width || targetElement.offsetWidth
        };
        if (placementPrimary === "auto") {
            var newPlacementPrimary = this.autoPosition(targetElPosition, hostElPosition, targetElement, placementSecondary);
            if (!newPlacementPrimary)
                newPlacementPrimary = this.autoPosition(targetElPosition, hostElPosition, targetElement);
            if (newPlacementPrimary)
                placementPrimary = newPlacementPrimary;
            targetElement.classList.add(placementPrimary);
        }
        switch (placementPrimary) {
            case 'top':
                targetElPosition.top = hostElPosition.top - (targetElement.offsetHeight + parseFloat(targetElStyles.marginBottom));
                targetElPosition.bottom += hostElPosition.top - targetElement.offsetHeight;
                targetElPosition.left = shiftWidth[placementSecondary];
                targetElPosition.right += shiftWidth[placementSecondary];
                break;
            case 'bottom':
                targetElPosition.top = shiftHeight[placementPrimary];
                targetElPosition.bottom += shiftHeight[placementPrimary];
                targetElPosition.left = shiftWidth[placementSecondary];
                targetElPosition.right += shiftWidth[placementSecondary];
                break;
            case 'left':
                targetElPosition.top = shiftHeight[placementSecondary];
                targetElPosition.bottom += shiftHeight[placementSecondary];
                targetElPosition.left = hostElPosition.left - (targetElement.offsetWidth + parseFloat(targetElStyles.marginRight));
                targetElPosition.right += hostElPosition.left - targetElement.offsetWidth;
                break;
            case 'right':
                targetElPosition.top = shiftHeight[placementSecondary];
                targetElPosition.bottom += shiftHeight[placementSecondary];
                targetElPosition.left = shiftWidth[placementPrimary];
                targetElPosition.right += shiftWidth[placementPrimary];
                break;
        }
        targetElPosition.top = Math.round(targetElPosition.top);
        targetElPosition.bottom = Math.round(targetElPosition.bottom);
        targetElPosition.left = Math.round(targetElPosition.left);
        targetElPosition.right = Math.round(targetElPosition.right);
        return targetElPosition;
    };
    Positioning.prototype.autoPosition = function (targetElPosition, hostElPosition, targetElement, preferredPosition) {
        if ((!preferredPosition || preferredPosition === "right") && targetElPosition.left + hostElPosition.left - targetElement.offsetWidth < 0) {
            return "right";
        }
        else if ((!preferredPosition || preferredPosition === "top") && targetElPosition.bottom + hostElPosition.bottom + targetElement.offsetHeight > window.innerHeight) {
            return "top";
        }
        else if ((!preferredPosition || preferredPosition === "bottom") && targetElPosition.top + hostElPosition.top - targetElement.offsetHeight < 0) {
            return "bottom";
        }
        else if ((!preferredPosition || preferredPosition === "left") && targetElPosition.right + hostElPosition.right + targetElement.offsetWidth > window.innerWidth) {
            return "left";
        }
        return null;
    };
    Positioning.prototype.getAllStyles = function (element) { return window.getComputedStyle(element); };
    Positioning.prototype.getStyle = function (element, prop) { return this.getAllStyles(element)[prop]; };
    Positioning.prototype.isStaticPositioned = function (element) {
        return (this.getStyle(element, 'position') || 'static') === 'static';
    };
    Positioning.prototype.offsetParent = function (element) {
        var offsetParentEl = element.offsetParent || document.documentElement;
        while (offsetParentEl && offsetParentEl !== document.documentElement && this.isStaticPositioned(offsetParentEl)) {
            offsetParentEl = offsetParentEl.offsetParent;
        }
        return offsetParentEl || document.documentElement;
    };
    return Positioning;
}());

var positionService = new Positioning();
function positionElements(hostElement, targetElement, placement, appendToBody) {
    var pos = positionService.positionElements(hostElement, targetElement, placement, appendToBody);
    targetElement.style.top = pos.top + "px";
    targetElement.style.left = pos.left + "px";
}


/***/ }),
/* 87 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PositioningService; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__ng_positioning__ = __webpack_require__(86);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};


var PositioningService = (function () {
    function PositioningService() {
    }
    PositioningService.prototype.position = function (options) {
        var element = options.element, target = options.target, attachment = options.attachment, appendToBody = options.appendToBody;
        Object(__WEBPACK_IMPORTED_MODULE_1__ng_positioning__["b" /* positionElements */])(this._getHtmlElement(target), this._getHtmlElement(element), attachment, appendToBody);
    };
    PositioningService.prototype._getHtmlElement = function (element) {
        // it means that we got a selector
        if (typeof element === 'string') {
            return document.querySelector(element);
        }
        if (element instanceof __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]) {
            return element.nativeElement;
        }
        return element;
    };
    PositioningService = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])()
    ], PositioningService);
    return PositioningService;
}());



/***/ }),
/* 88 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownMenuDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_dropdown_state__ = __webpack_require__(14);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var BsDropdownMenuDirective = (function () {
    function BsDropdownMenuDirective(_state, _viewContainer, _templateRef) {
        _state.resolveDropdownMenu({
            templateRef: _templateRef,
            viewContainer: _viewContainer
        });
    }
    BsDropdownMenuDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[bsDropdownMenu],[dropdownMenu]',
            exportAs: 'bs-dropdown-menu'
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__bs_dropdown_state__["a" /* BsDropdownState */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"]])
    ], BsDropdownMenuDirective);
    return BsDropdownMenuDirective;
}());



/***/ }),
/* 89 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownToggleDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_dropdown_state__ = __webpack_require__(14);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var BsDropdownToggleDirective = (function () {
    function BsDropdownToggleDirective(_state, _element) {
        var _this = this;
        this._state = _state;
        this._element = _element;
        this.isDisabled = null;
        this._subscriptions = [];
        // sync is open value with state
        this._subscriptions.push(this._state
            .isOpenChange.subscribe(function (value) { return _this.isOpen = value; }));
        // populate disabled state
        this._subscriptions.push(this._state
            .isDisabledChange
            .subscribe(function (value) { return _this.isDisabled = value || null; }));
    }
    BsDropdownToggleDirective.prototype.onClick = function () {
        if (this.isDisabled) {
            return;
        }
        this._state.toggleClick.emit();
    };
    BsDropdownToggleDirective.prototype.onDocumentClick = function (event) {
        if (this._state.autoClose && event.button !== 2 &&
            !this._element.nativeElement.contains(event.target)) {
            this._state.toggleClick.emit(false);
        }
    };
    BsDropdownToggleDirective.prototype.onEsc = function () {
        if (this._state.autoClose) {
            this._state.toggleClick.emit(false);
        }
    };
    BsDropdownToggleDirective.prototype.ngOnDestroy = function () {
        for (var _i = 0, _a = this._subscriptions; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('attr.disabled'),
        __metadata("design:type", Boolean)
    ], BsDropdownToggleDirective.prototype, "isDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('attr.aria-expanded'),
        __metadata("design:type", Boolean)
    ], BsDropdownToggleDirective.prototype, "isOpen", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('click'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], BsDropdownToggleDirective.prototype, "onClick", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('document:click', ['$event']),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [Object]),
        __metadata("design:returntype", void 0)
    ], BsDropdownToggleDirective.prototype, "onDocumentClick", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('keyup.esc'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], BsDropdownToggleDirective.prototype, "onEsc", null);
    BsDropdownToggleDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[bsDropdownToggle],[dropdownToggle]',
            exportAs: 'bs-dropdown-toggle',
            host: {
                '[attr.aria-haspopup]': 'true'
            }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__bs_dropdown_state__["a" /* BsDropdownState */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]])
    ], BsDropdownToggleDirective);
    return BsDropdownToggleDirective;
}());



/***/ }),
/* 90 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDropdownDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_add_operator_filter__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_add_operator_filter___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_rxjs_add_operator_filter__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_dropdown_config__ = __webpack_require__(43);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_dropdown_container_component__ = __webpack_require__(42);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_state__ = __webpack_require__(14);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__utils_ng2_bootstrap_config__ = __webpack_require__(3);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};







var BsDropdownDirective = (function () {
    function BsDropdownDirective(_elementRef, _renderer, _viewContainerRef, _cis, _config, _state) {
        this._elementRef = _elementRef;
        this._renderer = _renderer;
        this._viewContainerRef = _viewContainerRef;
        this._cis = _cis;
        this._config = _config;
        this._state = _state;
        // todo: move to component loader
        this._isInlineOpen = false;
        this._subscriptions = [];
        this._isInited = false;
        // create dropdown component loader
        this._dropdown = this._cis
            .createLoader(this._elementRef, this._viewContainerRef, this._renderer)
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_state__["a" /* BsDropdownState */], useValue: this._state });
        this.onShown = this._dropdown.onShown;
        this.onHidden = this._dropdown.onHidden;
        this.isOpenChange = this._state.isOpenChange;
        // set initial dropdown state from config
        this._state.autoClose = this._config.autoClose;
    }
    Object.defineProperty(BsDropdownDirective.prototype, "autoClose", {
        get: function () {
            return this._state.autoClose;
        },
        /**
         * Indicates that dropdown will be closed on item or document click,
         * and after pressing ESC
         */
        set: function (value) {
            if (typeof value === 'boolean') {
                this._state.autoClose = value;
            }
        },
        enumerable: true,
        configurable: true
    });
    ;
    Object.defineProperty(BsDropdownDirective.prototype, "isDisabled", {
        get: function () { return this._isDisabled; },
        /**
         * Disables dropdown toggle and hides dropdown menu if opened
         */
        set: function (value) {
            this._isDisabled = value;
            this._state.isDisabledChange.emit(value);
            if (value) {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDropdownDirective.prototype, "isOpen", {
        /**
         * Returns whether or not the popover is currently being shown
         */
        get: function () {
            if (this._showInline) {
                return this._isInlineOpen;
            }
            return this._dropdown.isShown;
        },
        set: function (value) {
            if (value) {
                this.show();
            }
            else {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDropdownDirective.prototype, "isBs4", {
        get: function () {
            return !Object(__WEBPACK_IMPORTED_MODULE_6__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDropdownDirective.prototype, "_showInline", {
        get: function () {
            return !this.container;
        },
        enumerable: true,
        configurable: true
    });
    ;
    BsDropdownDirective.prototype.ngOnInit = function () {
        var _this = this;
        // fix: seems there are an issue with `routerLinkActive`
        // which result in duplicated call ngOnInit without call to ngOnDestroy
        // read more: https://github.com/valor-software/ngx-bootstrap/issues/1885
        if (this._isInited) {
            return;
        }
        this._isInited = true;
        // attach DOM listeners
        this._dropdown.listen({
            triggers: this.triggers,
            show: function () { return _this.show(); }
        });
        // toggle visibility on toggle element click
        this._subscriptions.push(this._state
            .toggleClick.subscribe(function (value) { return _this.toggle(value); }));
        // hide dropdown if set disabled while opened
        this._subscriptions.push(this._state
            .isDisabledChange
            .filter(function (value) { return value === true; })
            .subscribe(function (value) { return _this.hide(); }));
    };
    /**
     * Opens an element’s popover. This is considered a “manual” triggering of
     * the popover.
     */
    BsDropdownDirective.prototype.show = function () {
        var _this = this;
        if (this.isOpen || this.isDisabled) {
            return;
        }
        if (this._showInline) {
            if (!this._inlinedMenu) {
                this._state.dropdownMenu
                    .then(function (dropdownMenu) {
                    _this._dropdown.attachInline(dropdownMenu.viewContainer, dropdownMenu.templateRef);
                    _this._inlinedMenu = _this._dropdown._inlineViewRef;
                    _this.addBs4Polyfills();
                });
            }
            this.addBs4Polyfills();
            this._isInlineOpen = true;
            this.onShown.emit(true);
            this._state.isOpenChange.emit(true);
            return;
        }
        this._state.dropdownMenu
            .then(function (dropdownMenu) {
            // check direction in which dropdown should be opened
            var _dropup = _this.dropup === true ||
                (typeof _this.dropup !== 'undefined' && _this.dropup !== false);
            _this._state.direction = _dropup ? 'up' : 'down';
            var _placement = _this.placement ||
                (_dropup ? 'top left' : 'bottom left');
            // show dropdown
            _this._dropdown
                .attach(__WEBPACK_IMPORTED_MODULE_4__bs_dropdown_container_component__["a" /* BsDropdownContainerComponent */])
                .to(_this.container)
                .position({ attachment: _placement })
                .show({
                content: dropdownMenu.templateRef,
                placement: _placement
            });
            _this._state.isOpenChange.emit(true);
        });
    };
    /**
     * Closes an element’s popover. This is considered a “manual” triggering of
     * the popover.
     */
    BsDropdownDirective.prototype.hide = function () {
        if (!this.isOpen) {
            return;
        }
        if (this._showInline) {
            this.removeShowClass();
            this._isInlineOpen = false;
            this.onHidden.emit(true);
        }
        else {
            this._dropdown.hide();
        }
        this._state.isOpenChange.emit(false);
    };
    /**
     * Toggles an element’s popover. This is considered a “manual” triggering of
     * the popover.
     */
    BsDropdownDirective.prototype.toggle = function (value) {
        if (this.isOpen || value === false) {
            return this.hide();
        }
        return this.show();
    };
    BsDropdownDirective.prototype.ngOnDestroy = function () {
        // clean up subscriptions and destroy dropdown
        for (var _i = 0, _a = this._subscriptions; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
        this._dropdown.dispose();
    };
    BsDropdownDirective.prototype.addBs4Polyfills = function () {
        if (!Object(__WEBPACK_IMPORTED_MODULE_6__utils_ng2_bootstrap_config__["a" /* isBs3 */])()) {
            this.addShowClass();
            this.checkRightAlignment();
            this.checkDropup();
        }
    };
    BsDropdownDirective.prototype.addShowClass = function () {
        if (this._inlinedMenu && this._inlinedMenu.rootNodes[0]) {
            this._renderer.setElementClass(this._inlinedMenu.rootNodes[0], 'show', true);
        }
    };
    BsDropdownDirective.prototype.removeShowClass = function () {
        if (this._inlinedMenu && this._inlinedMenu.rootNodes[0]) {
            this._renderer.setElementClass(this._inlinedMenu.rootNodes[0], 'show', false);
        }
    };
    BsDropdownDirective.prototype.checkRightAlignment = function () {
        if (this._inlinedMenu && this._inlinedMenu.rootNodes[0]) {
            var isRightAligned = this._inlinedMenu.rootNodes[0].classList.contains('dropdown-menu-right');
            this._renderer.setElementStyle(this._inlinedMenu.rootNodes[0], 'left', isRightAligned ? 'auto' : '0');
            this._renderer.setElementStyle(this._inlinedMenu.rootNodes[0], 'right', isRightAligned ? '0' : 'auto');
        }
    };
    BsDropdownDirective.prototype.checkDropup = function () {
        if (this._inlinedMenu && this._inlinedMenu.rootNodes[0]) {
            // a little hack to not break support of bootstrap 4 beta
            var top_1 = getComputedStyle(this._inlinedMenu.rootNodes[0])['top'];
            var topAuto = top_1 === 'auto' || top_1 === '100%';
            this._renderer.setElementStyle(this._inlinedMenu.rootNodes[0], 'top', this.dropup ? 'auto' : '100%');
            this._renderer.setElementStyle(this._inlinedMenu.rootNodes[0], 'transform', this.dropup && !topAuto ? 'translateY(-101%)' : 'translateY(0)');
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BsDropdownDirective.prototype, "placement", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BsDropdownDirective.prototype, "triggers", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BsDropdownDirective.prototype, "container", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], BsDropdownDirective.prototype, "dropup", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], BsDropdownDirective.prototype, "autoClose", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], BsDropdownDirective.prototype, "isDisabled", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], BsDropdownDirective.prototype, "isOpen", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDropdownDirective.prototype, "isOpenChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDropdownDirective.prototype, "onShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], BsDropdownDirective.prototype, "onHidden", void 0);
    BsDropdownDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[bsDropdown],[dropdown]',
            exportAs: 'bs-dropdown',
            providers: [__WEBPACK_IMPORTED_MODULE_5__bs_dropdown_state__["a" /* BsDropdownState */]],
            host: {
                '[class.dropup]': 'dropup',
                '[class.open]': 'isOpen',
                '[class.show]': 'isOpen && isBs4'
            }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"],
            __WEBPACK_IMPORTED_MODULE_2__component_loader__["b" /* ComponentLoaderFactory */],
            __WEBPACK_IMPORTED_MODULE_3__bs_dropdown_config__["a" /* BsDropdownConfig */],
            __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_state__["a" /* BsDropdownState */]])
    ], BsDropdownDirective);
    return BsDropdownDirective;
}());



/***/ }),
/* 91 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ModalModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__ = __webpack_require__(22);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__modal_component__ = __webpack_require__(92);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__positioning__ = __webpack_require__(9);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__modal_container_component__ = __webpack_require__(44);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__bs_modal_service__ = __webpack_require__(45);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var ModalModule = (function () {
    function ModalModule() {
    }
    ModalModule_1 = ModalModule;
    ModalModule.forRoot = function () {
        return { ngModule: ModalModule_1, providers: [__WEBPACK_IMPORTED_MODULE_6__bs_modal_service__["a" /* BsModalService */], __WEBPACK_IMPORTED_MODULE_4__component_loader__["b" /* ComponentLoaderFactory */], __WEBPACK_IMPORTED_MODULE_3__positioning__["b" /* PositioningService */]] };
    };
    ModalModule = ModalModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            declarations: [__WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__["a" /* ModalBackdropComponent */], __WEBPACK_IMPORTED_MODULE_2__modal_component__["a" /* ModalDirective */], __WEBPACK_IMPORTED_MODULE_5__modal_container_component__["a" /* ModalContainerComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__["a" /* ModalBackdropComponent */], __WEBPACK_IMPORTED_MODULE_2__modal_component__["a" /* ModalDirective */]],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__["a" /* ModalBackdropComponent */], __WEBPACK_IMPORTED_MODULE_5__modal_container_component__["a" /* ModalContainerComponent */]]
        })
    ], ModalModule);
    return ModalModule;
    var ModalModule_1;
}());



/***/ }),
/* 92 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ModalDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__utils_utils_class__ = __webpack_require__(37);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__modal_backdrop_component__ = __webpack_require__(22);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__modal_options_class__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__component_loader_component_loader_factory__ = __webpack_require__(13);
/* tslint:disable:max-file-line-count */
// todo: should we support enforce focus in?
// todo: in original bs there are was a way to prevent modal from showing
// todo: original modal had resize events
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};








var TRANSITION_DURATION = 300;
var BACKDROP_TRANSITION_DURATION = 150;
/** Mark any code with directive to show it's content in modal */
var ModalDirective = (function () {
    function ModalDirective(_element, _viewContainerRef, _renderer, clf) {
        /** This event fires immediately when the `show` instance method is called. */
        this.onShow = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** This event is fired when the modal has been made visible to the user (will wait for CSS transitions to complete) */
        this.onShown = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** This event is fired immediately when the hide instance method has been called. */
        this.onHide = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** This event is fired when the modal has finished being hidden from the user (will wait for CSS transitions to complete). */
        this.onHidden = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        // seems like an Options
        this.isAnimated = true;
        this._isShown = false;
        this.isBodyOverflowing = false;
        this.originalBodyPadding = 0;
        this.scrollbarWidth = 0;
        this.timerHideModal = 0;
        this.timerRmBackDrop = 0;
        this.isNested = false;
        this._element = _element;
        this._renderer = _renderer;
        this._backdrop = clf.createLoader(_element, _viewContainerRef, _renderer);
    }
    Object.defineProperty(ModalDirective.prototype, "config", {
        get: function () {
            return this._config;
        },
        /** allows to set modal configuration via element property */
        set: function (conf) {
            this._config = this.getConfig(conf);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(ModalDirective.prototype, "isShown", {
        get: function () {
            return this._isShown;
        },
        enumerable: true,
        configurable: true
    });
    ModalDirective.prototype.onClick = function (event) {
        if (this.config.ignoreBackdropClick || this.config.backdrop === 'static' || event.target !== this._element.nativeElement) {
            return;
        }
        this.dismissReason = __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["c" /* DISMISS_REASONS */].BACKRDOP;
        this.hide(event);
    };
    // todo: consider preventing default and stopping propagation
    ModalDirective.prototype.onEsc = function () {
        if (this.config.keyboard) {
            this.dismissReason = __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["c" /* DISMISS_REASONS */].ESC;
            this.hide();
        }
    };
    ModalDirective.prototype.ngOnDestroy = function () {
        this.config = void 0;
        if (this._isShown) {
            this._isShown = false;
            this.hideModal();
            this._backdrop.dispose();
        }
    };
    ModalDirective.prototype.ngAfterViewInit = function () {
        var _this = this;
        this._config = this._config || this.getConfig();
        setTimeout(function () {
            if (_this._config.show) {
                _this.show();
            }
        }, 0);
    };
    /* Public methods */
    /** Allows to manually toggle modal visibility */
    ModalDirective.prototype.toggle = function () {
        return this._isShown ? this.hide() : this.show();
    };
    /** Allows to manually open modal */
    ModalDirective.prototype.show = function () {
        var _this = this;
        this.dismissReason = null;
        this.onShow.emit(this);
        if (this._isShown) {
            return;
        }
        clearTimeout(this.timerHideModal);
        clearTimeout(this.timerRmBackDrop);
        this._isShown = true;
        this.checkScrollbar();
        this.setScrollbar();
        if (__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */] && __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body) {
            if (__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body.classList.contains(__WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].OPEN)) {
                this.isNested = true;
            }
            else {
                this._renderer.setElementClass(__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].OPEN, true);
            }
        }
        this.showBackdrop(function () {
            _this.showElement();
        });
    };
    /** Allows to manually close modal */
    ModalDirective.prototype.hide = function (event) {
        var _this = this;
        if (event) {
            event.preventDefault();
        }
        this.onHide.emit(this);
        // todo: add an option to prevent hiding
        if (!this._isShown) {
            return;
        }
        clearTimeout(this.timerHideModal);
        clearTimeout(this.timerRmBackDrop);
        this._isShown = false;
        this._renderer.setElementClass(this._element.nativeElement, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].IN, false);
        if (!Object(__WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__["a" /* isBs3 */])()) {
            this._renderer.setElementClass(this._element.nativeElement, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].SHOW, false);
        }
        // this._addClassIn = false;
        if (this.isAnimated) {
            this.timerHideModal = setTimeout(function () { return _this.hideModal(); }, TRANSITION_DURATION);
        }
        else {
            this.hideModal();
        }
    };
    /** Private methods @internal */
    ModalDirective.prototype.getConfig = function (config) {
        return Object.assign({}, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["f" /* modalConfigDefaults */], config);
    };
    /**
     *  Show dialog
     *  @internal
     */
    ModalDirective.prototype.showElement = function () {
        var _this = this;
        // todo: replace this with component loader usage
        if (!this._element.nativeElement.parentNode ||
            (this._element.nativeElement.parentNode.nodeType !== Node.ELEMENT_NODE)) {
            // don't move modals dom position
            if (__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */] && __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body) {
                __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body.appendChild(this._element.nativeElement);
            }
        }
        this._renderer.setElementAttribute(this._element.nativeElement, 'aria-hidden', 'false');
        this._renderer.setElementStyle(this._element.nativeElement, 'display', 'block');
        this._renderer.setElementProperty(this._element.nativeElement, 'scrollTop', 0);
        if (this.isAnimated) {
            __WEBPACK_IMPORTED_MODULE_3__utils_utils_class__["a" /* Utils */].reflow(this._element.nativeElement);
        }
        // this._addClassIn = true;
        this._renderer.setElementClass(this._element.nativeElement, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].IN, true);
        if (!Object(__WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__["a" /* isBs3 */])()) {
            this._renderer.setElementClass(this._element.nativeElement, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].SHOW, true);
        }
        var transitionComplete = function () {
            if (_this._config.focus) {
                _this._element.nativeElement.focus();
            }
            _this.onShown.emit(_this);
        };
        if (this.isAnimated) {
            setTimeout(transitionComplete, TRANSITION_DURATION);
        }
        else {
            transitionComplete();
        }
    };
    /** @internal */
    ModalDirective.prototype.hideModal = function () {
        var _this = this;
        this._renderer.setElementAttribute(this._element.nativeElement, 'aria-hidden', 'true');
        this._renderer.setElementStyle(this._element.nativeElement, 'display', 'none');
        this.showBackdrop(function () {
            if (!_this.isNested) {
                if (__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */] && __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body) {
                    _this._renderer.setElementClass(__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body, __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].OPEN, false);
                }
                _this.resetScrollbar();
            }
            _this.resetAdjustments();
            _this.focusOtherModal();
            _this.onHidden.emit(_this);
        });
    };
    // todo: original show was calling a callback when done, but we can use promise
    /** @internal */
    ModalDirective.prototype.showBackdrop = function (callback) {
        var _this = this;
        if (this._isShown && this.config.backdrop && (!this.backdrop || !this.backdrop.instance.isShown)) {
            this.removeBackdrop();
            this._backdrop
                .attach(__WEBPACK_IMPORTED_MODULE_4__modal_backdrop_component__["a" /* ModalBackdropComponent */])
                .to('body')
                .show({ isAnimated: this.isAnimated });
            this.backdrop = this._backdrop._componentRef;
            if (!callback) {
                return;
            }
            if (!this.isAnimated) {
                callback();
                return;
            }
            setTimeout(callback, BACKDROP_TRANSITION_DURATION);
        }
        else if (!this._isShown && this.backdrop) {
            this.backdrop.instance.isShown = false;
            var callbackRemove = function () {
                _this.removeBackdrop();
                if (callback) {
                    callback();
                }
            };
            if (this.backdrop.instance.isAnimated) {
                this.timerRmBackDrop = setTimeout(callbackRemove, BACKDROP_TRANSITION_DURATION);
            }
            else {
                callbackRemove();
            }
        }
        else if (callback) {
            callback();
        }
    };
    /** @internal */
    ModalDirective.prototype.removeBackdrop = function () {
        this._backdrop.hide();
    };
    /** Events tricks */
    // no need for it
    // protected setEscapeEvent():void {
    //   if (this._isShown && this._config.keyboard) {
    //     $(this._element).on(Event.KEYDOWN_DISMISS, (event) => {
    //       if (event.which === 27) {
    //         this.hide()
    //       }
    //     })
    //
    //   } else if (!this._isShown) {
    //     $(this._element).off(Event.KEYDOWN_DISMISS)
    //   }
    // }
    // protected setResizeEvent():void {
    // console.log(this.renderer.listenGlobal('', Event.RESIZE));
    // if (this._isShown) {
    //   $(window).on(Event.RESIZE, $.proxy(this._handleUpdate, this))
    // } else {
    //   $(window).off(Event.RESIZE)
    // }
    // }
    ModalDirective.prototype.focusOtherModal = function () {
        var otherOpenedModals = this._element.nativeElement.parentElement.querySelectorAll('.in[bsModal]');
        if (!otherOpenedModals.length) {
            return;
        }
        this._renderer.invokeElementMethod(otherOpenedModals[otherOpenedModals.length - 1], 'focus');
    };
    /** @internal */
    ModalDirective.prototype.resetAdjustments = function () {
        this._renderer.setElementStyle(this._element.nativeElement, 'paddingLeft', '');
        this._renderer.setElementStyle(this._element.nativeElement, 'paddingRight', '');
    };
    /** Scroll bar tricks */
    /** @internal */
    ModalDirective.prototype.checkScrollbar = function () {
        this.isBodyOverflowing = __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body.clientWidth < __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["b" /* window */].innerWidth;
        this.scrollbarWidth = this.getScrollbarWidth();
    };
    ModalDirective.prototype.setScrollbar = function () {
        if (!__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */]) {
            return;
        }
        this.originalBodyPadding = parseInt(__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["b" /* window */].getComputedStyle(__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body).getPropertyValue('padding-right') || 0, 10);
        if (this.isBodyOverflowing) {
            __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body.style.paddingRight = this.originalBodyPadding + this.scrollbarWidth + "px";
        }
    };
    ModalDirective.prototype.resetScrollbar = function () {
        __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body.style.paddingRight = this.originalBodyPadding;
    };
    // thx d.walsh
    ModalDirective.prototype.getScrollbarWidth = function () {
        var scrollDiv = this._renderer.createElement(__WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body, 'div', void 0);
        scrollDiv.className = __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["b" /* ClassName */].SCROLLBAR_MEASURER;
        var scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth;
        __WEBPACK_IMPORTED_MODULE_1__utils_facade_browser__["a" /* document */].body.removeChild(scrollDiv);
        return scrollbarWidth;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_5__modal_options_class__["d" /* ModalOptions */]),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_5__modal_options_class__["d" /* ModalOptions */]])
    ], ModalDirective.prototype, "config", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], ModalDirective.prototype, "onShow", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], ModalDirective.prototype, "onShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], ModalDirective.prototype, "onHide", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], ModalDirective.prototype, "onHidden", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('click', ['$event']),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [Object]),
        __metadata("design:returntype", void 0)
    ], ModalDirective.prototype, "onClick", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('keydown.esc'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], ModalDirective.prototype, "onEsc", null);
    ModalDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[bsModal]',
            exportAs: 'bs-modal'
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"], __WEBPACK_IMPORTED_MODULE_6__component_loader_component_loader_factory__["a" /* ComponentLoaderFactory */]])
    ], ModalDirective);
    return ModalDirective;
}());



/***/ }),
/* 93 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PaginationModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__pagination_config__ = __webpack_require__(23);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__pager_component__ = __webpack_require__(94);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__pagination_component__ = __webpack_require__(95);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};





var PaginationModule = (function () {
    function PaginationModule() {
    }
    PaginationModule_1 = PaginationModule;
    PaginationModule.forRoot = function () {
        return { ngModule: PaginationModule_1, providers: [__WEBPACK_IMPORTED_MODULE_2__pagination_config__["a" /* PaginationConfig */]] };
    };
    PaginationModule = PaginationModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__pager_component__["a" /* PagerComponent */], __WEBPACK_IMPORTED_MODULE_4__pagination_component__["a" /* PaginationComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__pager_component__["a" /* PagerComponent */], __WEBPACK_IMPORTED_MODULE_4__pagination_component__["a" /* PaginationComponent */]]
        })
    ], PaginationModule);
    return PaginationModule;
    var PaginationModule_1;
}());



/***/ }),
/* 94 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export PAGER_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PagerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__pagination_config__ = __webpack_require__(23);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var PAGER_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return PagerComponent; }),
    multi: true
};
var PAGER_TEMPLATE = "\n    <ul class=\"pager\">\n      <li [class.disabled]=\"noPrevious()\" [class.previous]=\"align\" [ngClass]=\"{'pull-right': align, 'float-right': align}\" class=\"{{ pageBtnClass }}\">\n        <a href (click)=\"selectPage(page - 1, $event)\">{{getText('previous')}}</a>\n      </li>\n      <li [class.disabled]=\"noNext()\" [class.next]=\"align\" [ngClass]=\"{'pull-right': align, 'float-right': align}\" class=\"{{ pageBtnClass }}\">\n        <a href (click)=\"selectPage(page + 1, $event)\">{{getText('next')}}</a>\n      </li>\n  </ul>\n";
var PagerComponent = (function () {
    function PagerComponent(renderer, elementRef, paginationConfig) {
        /** fired when total pages count changes, $event:number equals to total pages count */
        this.numPages = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired when page was changed, $event:{page, itemsPerPage} equals to object with current page index and number of items per page */
        this.pageChanged = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
        this.inited = false;
        this._page = 1;
        this.renderer = renderer;
        this.elementRef = elementRef;
        if (!this.config) {
            this.configureOptions(Object.assign({}, paginationConfig.main, paginationConfig.pager));
        }
    }
    Object.defineProperty(PagerComponent.prototype, "itemsPerPage", {
        /** maximum number of items per page. If value less than 1 will display all items on one page */
        get: function () {
            return this._itemsPerPage;
        },
        set: function (v) {
            this._itemsPerPage = v;
            this.totalPages = this.calculateTotalPages();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PagerComponent.prototype, "totalItems", {
        /** total number of items in all pages */
        get: function () {
            return this._totalItems;
        },
        set: function (v) {
            this._totalItems = v;
            this.totalPages = this.calculateTotalPages();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PagerComponent.prototype, "totalPages", {
        get: function () {
            return this._totalPages;
        },
        set: function (v) {
            this._totalPages = v;
            this.numPages.emit(v);
            if (this.inited) {
                this.selectPage(this.page);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PagerComponent.prototype, "page", {
        get: function () {
            return this._page;
        },
        set: function (value) {
            var _previous = this._page;
            this._page = (value > this.totalPages) ? this.totalPages : (value || 1);
            if (_previous === this._page || typeof _previous === 'undefined') {
                return;
            }
            this.pageChanged.emit({
                page: this._page,
                itemsPerPage: this.itemsPerPage
            });
        },
        enumerable: true,
        configurable: true
    });
    PagerComponent.prototype.configureOptions = function (config) {
        this.config = Object.assign({}, config);
    };
    PagerComponent.prototype.ngOnInit = function () {
        this.classMap = this.elementRef.nativeElement.getAttribute('class') || '';
        // watch for maxSize
        this.maxSize = typeof this.maxSize !== 'undefined'
            ? this.maxSize
            : this.config.maxSize;
        this.rotate = typeof this.rotate !== 'undefined'
            ? this.rotate
            : this.config.rotate;
        this.boundaryLinks = typeof this.boundaryLinks !== 'undefined'
            ? this.boundaryLinks
            : this.config.boundaryLinks;
        this.directionLinks = typeof this.directionLinks !== 'undefined'
            ? this.directionLinks
            : this.config.directionLinks;
        this.pageBtnClass = typeof this.pageBtnClass !== 'undefined'
            ? this.pageBtnClass
            : this.config.pageBtnClass;
        // base class
        this.itemsPerPage = typeof this.itemsPerPage !== 'undefined'
            ? this.itemsPerPage
            : this.config.itemsPerPage;
        this.totalPages = this.calculateTotalPages();
        // this class
        this.pages = this.getPages(this.page, this.totalPages);
        this.inited = true;
    };
    PagerComponent.prototype.writeValue = function (value) {
        this.page = value;
        this.pages = this.getPages(this.page, this.totalPages);
    };
    PagerComponent.prototype.getText = function (key) {
        return this[key + 'Text'] || this.config[key + 'Text'];
    };
    PagerComponent.prototype.noPrevious = function () {
        return this.page === 1;
    };
    PagerComponent.prototype.noNext = function () {
        return this.page === this.totalPages;
    };
    PagerComponent.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    PagerComponent.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    PagerComponent.prototype.selectPage = function (page, event) {
        if (event) {
            event.preventDefault();
        }
        if (!this.disabled) {
            if (event && event.target) {
                var target = event.target;
                target.blur();
            }
            this.writeValue(page);
            this.onChange(this.page);
        }
    };
    // Create page object used in template
    PagerComponent.prototype.makePage = function (num, text, active) {
        return { text: text, number: num, active: active };
    };
    PagerComponent.prototype.getPages = function (currentPage, totalPages) {
        var pages = [];
        // Default page limits
        var startPage = 1;
        var endPage = totalPages;
        var isMaxSized = typeof this.maxSize !== 'undefined' && this.maxSize < totalPages;
        // recompute if maxSize
        if (isMaxSized) {
            if (this.rotate) {
                // Current page is displayed in the middle of the visible ones
                startPage = Math.max(currentPage - Math.floor(this.maxSize / 2), 1);
                endPage = startPage + this.maxSize - 1;
                // Adjust if limit is exceeded
                if (endPage > totalPages) {
                    endPage = totalPages;
                    startPage = endPage - this.maxSize + 1;
                }
            }
            else {
                // Visible pages are paginated with maxSize
                startPage = ((Math.ceil(currentPage / this.maxSize) - 1) * this.maxSize) + 1;
                // Adjust last page if limit is exceeded
                endPage = Math.min(startPage + this.maxSize - 1, totalPages);
            }
        }
        // Add page number links
        for (var num = startPage; num <= endPage; num++) {
            var page = this.makePage(num, num.toString(), num === currentPage);
            pages.push(page);
        }
        // Add links to move between page sets
        if (isMaxSized && !this.rotate) {
            if (startPage > 1) {
                var previousPageSet = this.makePage(startPage - 1, '...', false);
                pages.unshift(previousPageSet);
            }
            if (endPage < totalPages) {
                var nextPageSet = this.makePage(endPage + 1, '...', false);
                pages.push(nextPageSet);
            }
        }
        return pages;
    };
    // base class
    PagerComponent.prototype.calculateTotalPages = function () {
        var totalPages = this.itemsPerPage < 1
            ? 1
            : Math.ceil(this.totalItems / this.itemsPerPage);
        return Math.max(totalPages || 0, 1);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PagerComponent.prototype, "align", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], PagerComponent.prototype, "maxSize", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PagerComponent.prototype, "boundaryLinks", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PagerComponent.prototype, "directionLinks", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PagerComponent.prototype, "firstText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PagerComponent.prototype, "previousText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PagerComponent.prototype, "nextText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PagerComponent.prototype, "lastText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PagerComponent.prototype, "rotate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PagerComponent.prototype, "pageBtnClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PagerComponent.prototype, "disabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], PagerComponent.prototype, "numPages", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], PagerComponent.prototype, "pageChanged", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], PagerComponent.prototype, "itemsPerPage", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], PagerComponent.prototype, "totalItems", null);
    PagerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'pager',
            template: PAGER_TEMPLATE,
            providers: [PAGER_CONTROL_VALUE_ACCESSOR]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_2__pagination_config__["a" /* PaginationConfig */]])
    ], PagerComponent);
    return PagerComponent;
}());



/***/ }),
/* 95 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export PAGINATION_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PaginationComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__pagination_config__ = __webpack_require__(23);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var PAGINATION_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return PaginationComponent; }),
    multi: true
};
var PAGINATION_TEMPLATE = "\n  <ul class=\"pagination\" [ngClass]=\"classMap\">\n    <li class=\"pagination-first page-item\"\n        *ngIf=\"boundaryLinks\"\n        [class.disabled]=\"noPrevious()||disabled\">\n      <a class=\"page-link\" href (click)=\"selectPage(1, $event)\" [innerHTML]=\"getText('first')\"></a>\n    </li>\n\n    <li class=\"pagination-prev page-item\"\n        *ngIf=\"directionLinks\"\n        [class.disabled]=\"noPrevious()||disabled\">\n      <a class=\"page-link\" href (click)=\"selectPage(page - 1, $event)\" [innerHTML]=\"getText('previous')\"></a>\n      </li>\n\n    <li *ngFor=\"let pg of pages\"\n        [class.active]=\"pg.active\"\n        [class.disabled]=\"disabled&&!pg.active\"\n        class=\"pagination-page page-item\">\n      <a class=\"page-link\" href (click)=\"selectPage(pg.number, $event)\" [innerHTML]=\"pg.text\"></a>\n    </li>\n\n    <li class=\"pagination-next page-item\"\n        *ngIf=\"directionLinks\"\n        [class.disabled]=\"noNext()||disabled\">\n      <a class=\"page-link\" href (click)=\"selectPage(page + 1, $event)\" [innerHTML]=\"getText('next')\"></a></li>\n\n    <li class=\"pagination-last page-item\"\n        *ngIf=\"boundaryLinks\"\n        [class.disabled]=\"noNext()||disabled\">\n      <a class=\"page-link\" href (click)=\"selectPage(totalPages, $event)\" [innerHTML]=\"getText('last')\"></a></li>\n  </ul>\n  ";
var PaginationComponent = (function () {
    function PaginationComponent(renderer, elementRef, paginationConfig) {
        /** fired when total pages count changes, $event:number equals to total pages count */
        this.numPages = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired when page was changed, $event:{page, itemsPerPage} equals to object with current page index and number of items per page */
        this.pageChanged = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
        this.inited = false;
        this._page = 1;
        this.renderer = renderer;
        this.elementRef = elementRef;
        if (!this.config) {
            this.configureOptions(paginationConfig.main);
        }
    }
    Object.defineProperty(PaginationComponent.prototype, "itemsPerPage", {
        /** maximum number of items per page. If value less than 1 will display all items on one page */
        get: function () {
            return this._itemsPerPage;
        },
        set: function (v) {
            this._itemsPerPage = v;
            this.totalPages = this.calculateTotalPages();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PaginationComponent.prototype, "totalItems", {
        /** total number of items in all pages */
        get: function () {
            return this._totalItems;
        },
        set: function (v) {
            this._totalItems = v;
            this.totalPages = this.calculateTotalPages();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PaginationComponent.prototype, "totalPages", {
        get: function () {
            return this._totalPages;
        },
        set: function (v) {
            this._totalPages = v;
            this.numPages.emit(v);
            if (this.inited) {
                this.selectPage(this.page);
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(PaginationComponent.prototype, "page", {
        get: function () {
            return this._page;
        },
        set: function (value) {
            var _previous = this._page;
            this._page = (value > this.totalPages) ? this.totalPages : (value || 1);
            if (_previous === this._page || typeof _previous === 'undefined') {
                return;
            }
            this.pageChanged.emit({
                page: this._page,
                itemsPerPage: this.itemsPerPage
            });
        },
        enumerable: true,
        configurable: true
    });
    PaginationComponent.prototype.configureOptions = function (config) {
        this.config = Object.assign({}, config);
    };
    PaginationComponent.prototype.ngOnInit = function () {
        this.classMap = this.elementRef.nativeElement.getAttribute('class') || '';
        // watch for maxSize
        this.maxSize = typeof this.maxSize !== 'undefined'
            ? this.maxSize
            : this.config.maxSize;
        this.rotate = typeof this.rotate !== 'undefined'
            ? this.rotate
            : this.config.rotate;
        this.boundaryLinks = typeof this.boundaryLinks !== 'undefined'
            ? this.boundaryLinks
            : this.config.boundaryLinks;
        this.directionLinks = typeof this.directionLinks !== 'undefined'
            ? this.directionLinks
            : this.config.directionLinks;
        this.pageBtnClass = typeof this.pageBtnClass !== 'undefined'
            ? this.pageBtnClass
            : this.config.pageBtnClass;
        // base class
        this.itemsPerPage = typeof this.itemsPerPage !== 'undefined'
            ? this.itemsPerPage
            : this.config.itemsPerPage;
        this.totalPages = this.calculateTotalPages();
        // this class
        this.pages = this.getPages(this.page, this.totalPages);
        this.inited = true;
    };
    PaginationComponent.prototype.writeValue = function (value) {
        this.page = value;
        this.pages = this.getPages(this.page, this.totalPages);
    };
    PaginationComponent.prototype.getText = function (key) {
        return this[key + 'Text'] || this.config[key + 'Text'];
    };
    PaginationComponent.prototype.noPrevious = function () {
        return this.page === 1;
    };
    PaginationComponent.prototype.noNext = function () {
        return this.page === this.totalPages;
    };
    PaginationComponent.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    PaginationComponent.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    PaginationComponent.prototype.selectPage = function (page, event) {
        if (event) {
            event.preventDefault();
        }
        if (!this.disabled) {
            if (event && event.target) {
                var target = event.target;
                target.blur();
            }
            this.writeValue(page);
            this.onChange(this.page);
        }
    };
    // Create page object used in template
    PaginationComponent.prototype.makePage = function (num, text, active) {
        return { text: text, number: num, active: active };
    };
    PaginationComponent.prototype.getPages = function (currentPage, totalPages) {
        var pages = [];
        // Default page limits
        var startPage = 1;
        var endPage = totalPages;
        var isMaxSized = typeof this.maxSize !== 'undefined' && this.maxSize < totalPages;
        // recompute if maxSize
        if (isMaxSized) {
            if (this.rotate) {
                // Current page is displayed in the middle of the visible ones
                startPage = Math.max(currentPage - Math.floor(this.maxSize / 2), 1);
                endPage = startPage + this.maxSize - 1;
                // Adjust if limit is exceeded
                if (endPage > totalPages) {
                    endPage = totalPages;
                    startPage = endPage - this.maxSize + 1;
                }
            }
            else {
                // Visible pages are paginated with maxSize
                startPage = ((Math.ceil(currentPage / this.maxSize) - 1) * this.maxSize) + 1;
                // Adjust last page if limit is exceeded
                endPage = Math.min(startPage + this.maxSize - 1, totalPages);
            }
        }
        // Add page number links
        for (var num = startPage; num <= endPage; num++) {
            var page = this.makePage(num, num.toString(), num === currentPage);
            pages.push(page);
        }
        // Add links to move between page sets
        if (isMaxSized && !this.rotate) {
            if (startPage > 1) {
                var previousPageSet = this.makePage(startPage - 1, '...', false);
                pages.unshift(previousPageSet);
            }
            if (endPage < totalPages) {
                var nextPageSet = this.makePage(endPage + 1, '...', false);
                pages.push(nextPageSet);
            }
        }
        return pages;
    };
    // base class
    PaginationComponent.prototype.calculateTotalPages = function () {
        var totalPages = this.itemsPerPage < 1
            ? 1
            : Math.ceil(this.totalItems / this.itemsPerPage);
        return Math.max(totalPages || 0, 1);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PaginationComponent.prototype, "align", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], PaginationComponent.prototype, "maxSize", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PaginationComponent.prototype, "boundaryLinks", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PaginationComponent.prototype, "directionLinks", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PaginationComponent.prototype, "firstText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PaginationComponent.prototype, "previousText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PaginationComponent.prototype, "nextText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PaginationComponent.prototype, "lastText", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PaginationComponent.prototype, "rotate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PaginationComponent.prototype, "pageBtnClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], PaginationComponent.prototype, "disabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], PaginationComponent.prototype, "numPages", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], PaginationComponent.prototype, "pageChanged", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], PaginationComponent.prototype, "itemsPerPage", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], PaginationComponent.prototype, "totalItems", null);
    PaginationComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'pagination',
            template: PAGINATION_TEMPLATE,
            providers: [PAGINATION_CONTROL_VALUE_ACCESSOR]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_2__pagination_config__["a" /* PaginationConfig */]])
    ], PaginationComponent);
    return PaginationComponent;
}());



/***/ }),
/* 96 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ProgressbarModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bar_component__ = __webpack_require__(97);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__progress_directive__ = __webpack_require__(46);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__progressbar_component__ = __webpack_require__(98);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__progressbar_config__ = __webpack_require__(47);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};






var ProgressbarModule = (function () {
    function ProgressbarModule() {
    }
    ProgressbarModule_1 = ProgressbarModule;
    ProgressbarModule.forRoot = function () {
        return { ngModule: ProgressbarModule_1, providers: [__WEBPACK_IMPORTED_MODULE_5__progressbar_config__["a" /* ProgressbarConfig */]] };
    };
    ProgressbarModule = ProgressbarModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__progress_directive__["a" /* ProgressDirective */], __WEBPACK_IMPORTED_MODULE_2__bar_component__["a" /* BarComponent */], __WEBPACK_IMPORTED_MODULE_4__progressbar_component__["a" /* ProgressbarComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__progress_directive__["a" /* ProgressDirective */], __WEBPACK_IMPORTED_MODULE_2__bar_component__["a" /* BarComponent */], __WEBPACK_IMPORTED_MODULE_4__progressbar_component__["a" /* ProgressbarComponent */]]
        })
    ], ProgressbarModule);
    return ProgressbarModule;
    var ProgressbarModule_1;
}());



/***/ }),
/* 97 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BarComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__progress_directive__ = __webpack_require__(46);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__ = __webpack_require__(3);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};



// todo: number pipe
// todo: use query from progress?
var BarComponent = (function () {
    function BarComponent(progress) {
        this.percent = 0;
        this.progress = progress;
    }
    Object.defineProperty(BarComponent.prototype, "value", {
        /** current value of progress bar */
        get: function () {
            return this._value;
        },
        set: function (v) {
            if (!v && v !== 0) {
                return;
            }
            this._value = v;
            this.recalculatePercentage();
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BarComponent.prototype, "setBarWidth", {
        get: function () {
            this.recalculatePercentage();
            return this.isBs3 ? '' : this.percent;
        },
        enumerable: true,
        configurable: true
    });
    ;
    Object.defineProperty(BarComponent.prototype, "isBs3", {
        get: function () {
            return Object(__WEBPACK_IMPORTED_MODULE_2__utils_ng2_bootstrap_config__["a" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    BarComponent.prototype.ngOnInit = function () {
        this.progress.addBar(this);
    };
    BarComponent.prototype.ngOnDestroy = function () {
        this.progress.removeBar(this);
    };
    BarComponent.prototype.recalculatePercentage = function () {
        this.percent = +(100 * this.value / this.progress.max).toFixed(2);
        var totalPercentage = this.progress.bars.reduce(function (total, bar) {
            return total + bar.percent;
        }, 0);
        if (totalPercentage > 100) {
            this.percent -= totalPercentage - 100;
        }
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BarComponent.prototype, "type", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number),
        __metadata("design:paramtypes", [Number])
    ], BarComponent.prototype, "value", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostBinding"])('style.width.%'),
        __metadata("design:type", Object),
        __metadata("design:paramtypes", [])
    ], BarComponent.prototype, "setBarWidth", null);
    BarComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bar',
            template: "\n  <div class=\"progress-bar\"\n    style=\"min-width: 0;\"\n    role=\"progressbar\"\n    [ngClass]=\"type && 'progress-bar-' + type + ' bg-' + type\"\n    [ngStyle]=\"{width: (isBs3 ? (percent < 100 ? percent : 100) + '%' : '100%'), transition: transition}\"\n    aria-valuemin=\"0\"\n    [attr.aria-valuenow]=\"value\"\n    [attr.aria-valuetext]=\"percent.toFixed(0) + '%'\"\n    [attr.aria-valuemax]=\"max\"><ng-content></ng-content></div>\n"
        }),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Host"])()),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__progress_directive__["a" /* ProgressDirective */]])
    ], BarComponent);
    return BarComponent;
}());



/***/ }),
/* 98 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ProgressbarComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__progressbar_config__ = __webpack_require__(47);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils__ = __webpack_require__(36);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var ProgressbarComponent = (function () {
    function ProgressbarComponent(config) {
        this.isStacked = false;
        Object.assign(this, config);
    }
    Object.defineProperty(ProgressbarComponent.prototype, "value", {
        /** current value of progress bar. Could be a number or array of objects like {"value":15,"type":"info","label":"15 %"} */
        set: function (value) {
            this.isStacked = Array.isArray(value);
            this._value = value;
        },
        enumerable: true,
        configurable: true
    });
    ;
    Object.defineProperty(ProgressbarComponent.prototype, "isBs3", {
        get: function () {
            return Object(__WEBPACK_IMPORTED_MODULE_2__utils__["e" /* isBs3 */])();
        },
        enumerable: true,
        configurable: true
    });
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], ProgressbarComponent.prototype, "animate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], ProgressbarComponent.prototype, "max", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], ProgressbarComponent.prototype, "type", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object),
        __metadata("design:paramtypes", [Object])
    ], ProgressbarComponent.prototype, "value", null);
    ProgressbarComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'progressbar',
            template: "\n    <div progress [animate]=\"animate\" [max]=\"max\" [style.width]=\"!isBs3 ? '100%' : 'auto'\">\n      <bar [type]=\"type\" [value]=\"_value\" *ngIf=\"!isStacked\">\n          <ng-content></ng-content>\n      </bar>\n      <template [ngIf]=\"isStacked\">\n        <bar *ngFor=\"let item of _value\" [type]=\"item.type\" [value]=\"item.value\">{{item.label}}</bar>\n      </template>\n    </div>\n  ",
            styles: ["\n    :host {\n      width: 100%;\n    }\n  "]
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__progressbar_config__["a" /* ProgressbarConfig */]])
    ], ProgressbarComponent);
    return ProgressbarComponent;
}());



/***/ }),
/* 99 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return RatingModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__rating_component__ = __webpack_require__(100);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};



var RatingModule = (function () {
    function RatingModule() {
    }
    RatingModule_1 = RatingModule;
    RatingModule.forRoot = function () {
        return {
            ngModule: RatingModule_1,
            providers: []
        };
    };
    RatingModule = RatingModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_2__rating_component__["a" /* RatingComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_2__rating_component__["a" /* RatingComponent */]]
        })
    ], RatingModule);
    return RatingModule;
    var RatingModule_1;
}());



/***/ }),
/* 100 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export RATING_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return RatingComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var RATING_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return RatingComponent; }),
    multi: true
};
var RatingComponent = (function () {
    function RatingComponent() {
        /** number of icons */
        this.max = 5;
        /** fired when icon selected, $event:number equals to selected rating */
        this.onHover = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired when icon selected, $event:number equals to previous rating value */
        this.onLeave = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
    }
    RatingComponent.prototype.onKeydown = function (event) {
        if ([37, 38, 39, 40].indexOf(event.which) === -1) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        var sign = event.which === 38 || event.which === 39 ? 1 : -1;
        this.rate(this.value + sign);
    };
    RatingComponent.prototype.ngOnInit = function () {
        this.max = typeof this.max !== 'undefined' ? this.max : 5;
        this.readonly = this.readonly === true;
        this.stateOn = typeof this.stateOn !== 'undefined'
            ? this.stateOn
            : 'glyphicon-star';
        this.stateOff = typeof this.stateOff !== 'undefined'
            ? this.stateOff
            : 'glyphicon-star-empty';
        this.titles = typeof this.titles !== 'undefined' && this.titles.length > 0
            ? this.titles
            : ['one', 'two', 'three', 'four', 'five'];
        this.range = this.buildTemplateObjects(this.ratingStates, this.max);
    };
    // model -> view
    RatingComponent.prototype.writeValue = function (value) {
        if (value % 1 !== value) {
            this.value = Math.round(value);
            this.preValue = value;
            return;
        }
        this.preValue = value;
        this.value = value;
    };
    RatingComponent.prototype.enter = function (value) {
        if (!this.readonly) {
            this.value = value;
            this.onHover.emit(value);
        }
    };
    RatingComponent.prototype.reset = function () {
        this.value = this.preValue;
        this.onLeave.emit(this.value);
    };
    RatingComponent.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    RatingComponent.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    RatingComponent.prototype.rate = function (value) {
        if (!this.readonly && value >= 0 && value <= this.range.length) {
            this.writeValue(value);
            this.onChange(value);
        }
    };
    RatingComponent.prototype.buildTemplateObjects = function (ratingStates, max) {
        ratingStates = ratingStates || [];
        var count = ratingStates.length || max;
        var result = [];
        for (var i = 0; i < count; i++) {
            result.push(Object.assign({
                index: i,
                stateOn: this.stateOn,
                stateOff: this.stateOff,
                title: this.titles[i] || i + 1
            }, ratingStates[i] || {}));
        }
        return result;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], RatingComponent.prototype, "max", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], RatingComponent.prototype, "stateOn", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], RatingComponent.prototype, "stateOff", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], RatingComponent.prototype, "readonly", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], RatingComponent.prototype, "titles", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], RatingComponent.prototype, "ratingStates", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], RatingComponent.prototype, "onHover", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], RatingComponent.prototype, "onLeave", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('keydown', ['$event']),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [Object]),
        __metadata("design:returntype", void 0)
    ], RatingComponent.prototype, "onKeydown", null);
    RatingComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'rating',
            template: "\n    <span (mouseleave)=\"reset()\" (keydown)=\"onKeydown($event)\" tabindex=\"0\" role=\"slider\" aria-valuemin=\"0\" [attr.aria-valuemax]=\"range.length\" [attr.aria-valuenow]=\"value\">\n      <template ngFor let-r [ngForOf]=\"range\" let-index=\"index\">\n        <span class=\"sr-only\">({{ index < value ? '*' : ' ' }})</span>\n        <i (mouseenter)=\"enter(index + 1)\" (click)=\"rate(index + 1)\" class=\"glyphicon\" [ngClass]=\"index < value ? r.stateOn : r.stateOff\" [title]=\"r.title\" ></i>\n      </template>\n    </span>\n  ",
            providers: [RATING_CONTROL_VALUE_ACCESSOR]
        })
    ], RatingComponent);
    return RatingComponent;
}());



/***/ }),
/* 101 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__sortable_module__ = __webpack_require__(142);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_0__sortable_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__sortable_component__ = __webpack_require__(102);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__sortable_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__draggable_item_service__ = __webpack_require__(48);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_2__draggable_item_service__["a"]; });





/***/ }),
/* 102 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return SortableComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__draggable_item_service__ = __webpack_require__(48);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



/* tslint:disable */
var SortableComponent = (function () {
    function SortableComponent(transfer) {
        var _this = this;
        /** class name for items wrapper */
        this.wrapperClass = '';
        /** style object for items wrapper */
        this.wrapperStyle = {};
        /** class name for item */
        this.itemClass = '';
        /** style object for item */
        this.itemStyle = {};
        /** class name for active item */
        this.itemActiveClass = '';
        /** style object for active item */
        this.itemActiveStyle = {};
        /** class name for placeholder */
        this.placeholderClass = '';
        /** style object for placeholder */
        this.placeholderStyle = {};
        /** placeholder item which will be shown if collection is empty */
        this.placeholderItem = '';
        /** fired on array change (reordering, insert, remove), same as <code>ngModelChange</code>.
         *  Returns new items collection as a payload.
         */
        this.onChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.showPlaceholder = false;
        this.activeItem = -1;
        this.onTouched = Function.prototype;
        this.onChanged = Function.prototype;
        this.transfer = transfer;
        this.currentZoneIndex = SortableComponent_1.globalZoneIndex++;
        this.transfer.onCaptureItem()
            .subscribe(function (item) { return _this.onDrop(item); });
    }
    SortableComponent_1 = SortableComponent;
    Object.defineProperty(SortableComponent.prototype, "items", {
        get: function () {
            return this._items;
        },
        set: function (value) {
            this._items = value;
            var out = this.items.map(function (x) { return x.initData; });
            this.onChanged(out);
            this.onChange.emit(out);
        },
        enumerable: true,
        configurable: true
    });
    SortableComponent.prototype.onItemDragstart = function (event, item, i) {
        this.initDragstartEvent(event);
        this.onTouched();
        this.transfer.dragStart({
            event: event,
            item: item,
            i: i,
            initialIndex: i,
            lastZoneIndex: this.currentZoneIndex,
            overZoneIndex: this.currentZoneIndex
        });
    };
    SortableComponent.prototype.onItemDragover = function (event, i) {
        if (!this.transfer.getItem()) {
            return;
        }
        event.preventDefault();
        var dragItem = this.transfer.captureItem(this.currentZoneIndex, this.items.length);
        var newArray = [];
        if (!this.items.length) {
            newArray = [dragItem.item];
        }
        else if (dragItem.i > i) {
            newArray = this.items.slice(0, i).concat([
                dragItem.item
            ], this.items.slice(i, dragItem.i), this.items.slice(dragItem.i + 1));
        }
        else {
            newArray = this.items.slice(0, dragItem.i).concat(this.items.slice(dragItem.i + 1, i + 1), [
                dragItem.item
            ], this.items.slice(i + 1));
        }
        this.items = newArray;
        dragItem.i = i;
        this.activeItem = i;
        this.updatePlaceholderState();
    };
    SortableComponent.prototype.cancelEvent = function (event) {
        if (!this.transfer.getItem() || !event) {
            return;
        }
        event.preventDefault();
    };
    SortableComponent.prototype.onDrop = function (item) {
        if (item &&
            item.overZoneIndex !== this.currentZoneIndex &&
            item.lastZoneIndex === this.currentZoneIndex) {
            this.items = this.items.filter(function (x, i) { return i !== item.i; });
            this.updatePlaceholderState();
        }
        this.resetActiveItem(undefined);
    };
    SortableComponent.prototype.resetActiveItem = function (event) {
        this.cancelEvent(event);
        this.activeItem = -1;
    };
    SortableComponent.prototype.registerOnChange = function (callback) {
        this.onChanged = callback;
    };
    SortableComponent.prototype.registerOnTouched = function (callback) {
        this.onTouched = callback;
    };
    SortableComponent.prototype.writeValue = function (value) {
        var _this = this;
        if (value) {
            this.items = value.map(function (x, i) { return ({
                id: i,
                initData: x,
                value: _this.fieldName ? x[_this.fieldName] : x
            }); });
        }
        else {
            this.items = [];
        }
        this.updatePlaceholderState();
    };
    SortableComponent.prototype.updatePlaceholderState = function () {
        this.showPlaceholder = !this._items.length;
    };
    SortableComponent.prototype.getItemStyle = function (isActive) {
        return isActive
            ? Object.assign({}, this.itemStyle, this.itemActiveStyle)
            : this.itemStyle;
    };
    SortableComponent.prototype.initDragstartEvent = function (event) {
        // it is necessary for mozilla
        // data type should be 'Text' instead of 'text/plain' to keep compatibility
        // with IE
        event.dataTransfer.setData('Text', 'placeholder');
    };
    SortableComponent.globalZoneIndex = 0;
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], SortableComponent.prototype, "fieldName", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], SortableComponent.prototype, "wrapperClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], SortableComponent.prototype, "wrapperStyle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], SortableComponent.prototype, "itemClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], SortableComponent.prototype, "itemStyle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], SortableComponent.prototype, "itemActiveClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], SortableComponent.prototype, "itemActiveStyle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], SortableComponent.prototype, "placeholderClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], SortableComponent.prototype, "placeholderStyle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], SortableComponent.prototype, "placeholderItem", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"])
    ], SortableComponent.prototype, "itemTemplate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], SortableComponent.prototype, "onChange", void 0);
    SortableComponent = SortableComponent_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-sortable',
            exportAs: 'bs-sortable',
            template: "\n<div\n    [ngClass]=\"wrapperClass\"\n    [ngStyle]=\"wrapperStyle\"\n    [ngStyle]=\"wrapperStyle\"\n    (dragover)=\"cancelEvent($event)\"\n    (dragenter)=\"cancelEvent($event)\"\n    (drop)=\"resetActiveItem($event)\"\n    (mouseleave)=\"resetActiveItem($event)\">\n  <div\n        *ngIf=\"showPlaceholder\"\n        [ngClass]=\"placeholderClass\"\n        [ngStyle]=\"placeholderStyle\"\n        (dragover)=\"onItemDragover($event, 0)\"\n        (dragenter)=\"cancelEvent($event)\"\n    >{{placeholderItem}}</div>\n    <div\n        *ngFor=\"let item of items; let i=index;\"\n        [ngClass]=\"[ itemClass, i === activeItem ? itemActiveClass : '' ]\"\n        [ngStyle]=\"getItemStyle(i === activeItem)\"\n        draggable=\"true\"\n        (dragstart)=\"onItemDragstart($event, item, i)\"\n        (dragend)=\"resetActiveItem($event)\"\n        (dragover)=\"onItemDragover($event, i)\"\n        (dragenter)=\"cancelEvent($event)\"\n    ><template [ngTemplateOutlet]=\"itemTemplate || defItemTemplate\"\n  [ngOutletContext]=\"{item:item, index: i}\"></template></div>\n</div>\n\n<template #defItemTemplate let-item=\"item\">{{item.value}}</template>  \n",
            providers: [{
                    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
                    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return SortableComponent_1; }),
                    multi: true
                }],
        })
        /* tslint:enable */
        ,
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__draggable_item_service__["a" /* DraggableItemService */]])
    ], SortableComponent);
    return SortableComponent;
    var SortableComponent_1;
}());



/***/ }),
/* 103 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TabsModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ng_transclude_directive__ = __webpack_require__(104);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__tab_heading_directive__ = __webpack_require__(105);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__tab_directive__ = __webpack_require__(49);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__tabset_component__ = __webpack_require__(50);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__tabset_config__ = __webpack_require__(51);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var TabsModule = (function () {
    function TabsModule() {
    }
    TabsModule_1 = TabsModule;
    TabsModule.forRoot = function () {
        return {
            ngModule: TabsModule_1,
            providers: [__WEBPACK_IMPORTED_MODULE_6__tabset_config__["a" /* TabsetConfig */]]
        };
    };
    TabsModule = TabsModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_2__ng_transclude_directive__["a" /* NgTranscludeDirective */], __WEBPACK_IMPORTED_MODULE_4__tab_directive__["a" /* TabDirective */], __WEBPACK_IMPORTED_MODULE_5__tabset_component__["a" /* TabsetComponent */], __WEBPACK_IMPORTED_MODULE_3__tab_heading_directive__["a" /* TabHeadingDirective */]],
            exports: [__WEBPACK_IMPORTED_MODULE_4__tab_directive__["a" /* TabDirective */], __WEBPACK_IMPORTED_MODULE_5__tabset_component__["a" /* TabsetComponent */], __WEBPACK_IMPORTED_MODULE_3__tab_heading_directive__["a" /* TabHeadingDirective */], __WEBPACK_IMPORTED_MODULE_2__ng_transclude_directive__["a" /* NgTranscludeDirective */]]
        })
    ], TabsModule);
    return TabsModule;
    var TabsModule_1;
}());



/***/ }),
/* 104 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return NgTranscludeDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var NgTranscludeDirective = (function () {
    function NgTranscludeDirective(viewRef) {
        this.viewRef = viewRef;
    }
    Object.defineProperty(NgTranscludeDirective.prototype, "ngTransclude", {
        get: function () {
            return this._ngTransclude;
        },
        set: function (templateRef) {
            this._ngTransclude = templateRef;
            if (templateRef) {
                this.viewRef.createEmbeddedView(templateRef);
            }
        },
        enumerable: true,
        configurable: true
    });
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"]),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"]])
    ], NgTranscludeDirective.prototype, "ngTransclude", null);
    NgTranscludeDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[ngTransclude]'
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"]])
    ], NgTranscludeDirective);
    return NgTranscludeDirective;
}());



/***/ }),
/* 105 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TabHeadingDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tab_directive__ = __webpack_require__(49);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


/** Should be used to mark <template> element as a template for tab heading */
var TabHeadingDirective = (function () {
    function TabHeadingDirective(templateRef, tab) {
        tab.headingRef = templateRef;
    }
    TabHeadingDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: '[tabHeading]' }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"], __WEBPACK_IMPORTED_MODULE_1__tab_directive__["a" /* TabDirective */]])
    ], TabHeadingDirective);
    return TabHeadingDirective;
}());



/***/ }),
/* 106 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimepickerModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__timepicker_component__ = __webpack_require__(107);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__reducer_timepicker_actions__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__timepicker_config__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__reducer_timepicker_store__ = __webpack_require__(52);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};






var TimepickerModule = (function () {
    function TimepickerModule() {
    }
    TimepickerModule_1 = TimepickerModule;
    TimepickerModule.forRoot = function () {
        return {
            ngModule: TimepickerModule_1,
            providers: [__WEBPACK_IMPORTED_MODULE_4__timepicker_config__["a" /* TimepickerConfig */], __WEBPACK_IMPORTED_MODULE_3__reducer_timepicker_actions__["a" /* TimepickerActions */], __WEBPACK_IMPORTED_MODULE_5__reducer_timepicker_store__["a" /* TimepickerStore */]]
        };
    };
    TimepickerModule = TimepickerModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_1__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_2__timepicker_component__["a" /* TimepickerComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_2__timepicker_component__["a" /* TimepickerComponent */]]
        })
    ], TimepickerModule);
    return TimepickerModule;
    var TimepickerModule_1;
}());



/***/ }),
/* 107 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export TIMEPICKER_CONTROL_VALUE_ACCESSOR */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TimepickerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__reducer_timepicker_actions__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__reducer_timepicker_store__ = __webpack_require__(52);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__timepicker_controls_util__ = __webpack_require__(108);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__timepicker_config__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__timepicker_utils__ = __webpack_require__(53);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/* tslint:disable:no-forward-ref max-file-line-count */







var TIMEPICKER_CONTROL_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    // tslint:disable-next-line
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return TimepickerComponent; }),
    multi: true
};
var TimepickerComponent = (function () {
    function TimepickerComponent(_config, _cd, _store, _timepickerActions) {
        var _this = this;
        this._cd = _cd;
        this._store = _store;
        this._timepickerActions = _timepickerActions;
        /** emits true if value is a valid date */
        this.isValid = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        // min\max validation for input fields
        this.invalidHours = false;
        this.invalidMinutes = false;
        this.invalidSeconds = false;
        // control value accessor methods
        this.onChange = Function.prototype;
        this.onTouched = Function.prototype;
        Object.assign(this, _config);
        // todo: add unsubscribe
        _store
            .select(function (state) { return state.value; })
            .subscribe(function (value) {
            // update UI values if date changed
            _this._renderTime(value);
            _this.onChange(value);
            _this._store.dispatch(_this._timepickerActions.updateControls(Object(__WEBPACK_IMPORTED_MODULE_4__timepicker_controls_util__["e" /* getControlsValue */])(_this)));
        });
        _store
            .select(function (state) { return state.controls; })
            .subscribe(function (controlsState) {
            _this.isValid.emit(Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["b" /* isInputValid */])(_this.hours, _this.minutes, _this.seconds, _this.isPM()));
            Object.assign(_this, controlsState);
            _cd.markForCheck();
        });
    }
    Object.defineProperty(TimepickerComponent.prototype, "isSpinnersVisible", {
        get: function () {
            return this.showSpinners && !this.readonlyInput;
        },
        enumerable: true,
        configurable: true
    });
    TimepickerComponent.prototype.isPM = function () {
        return this.showMeridian && this.meridian === this.meridians[1];
    };
    TimepickerComponent.prototype.prevDef = function ($event) {
        $event.preventDefault();
    };
    TimepickerComponent.prototype.wheelSign = function ($event) {
        return Math.sign($event.deltaY) * -1;
    };
    TimepickerComponent.prototype.ngOnChanges = function (changes) {
        this._store.dispatch(this._timepickerActions.updateControls(Object(__WEBPACK_IMPORTED_MODULE_4__timepicker_controls_util__["e" /* getControlsValue */])(this)));
    };
    TimepickerComponent.prototype.changeHours = function (step, source) {
        if (source === void 0) { source = ''; }
        this._store.dispatch(this._timepickerActions.changeHours({ step: step, source: source }));
    };
    TimepickerComponent.prototype.changeMinutes = function (step, source) {
        if (source === void 0) { source = ''; }
        this._store.dispatch(this._timepickerActions.changeMinutes({ step: step, source: source }));
    };
    TimepickerComponent.prototype.changeSeconds = function (step, source) {
        if (source === void 0) { source = ''; }
        this._store.dispatch(this._timepickerActions.changeSeconds({ step: step, source: source }));
    };
    TimepickerComponent.prototype.updateHours = function (hours) {
        this.hours = hours;
        this._updateTime();
    };
    TimepickerComponent.prototype.updateMinutes = function (minutes) {
        this.minutes = minutes;
        this._updateTime();
    };
    TimepickerComponent.prototype.updateSeconds = function (seconds) {
        this.seconds = seconds;
        this._updateTime();
    };
    TimepickerComponent.prototype._updateTime = function () {
        if (!Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["b" /* isInputValid */])(this.hours, this.minutes, this.seconds, this.isPM())) {
            this.onChange(null);
            return;
        }
        this._store.dispatch(this._timepickerActions
            .setTime({
            hour: this.hours,
            minute: this.minutes,
            seconds: this.seconds,
            isPM: this.isPM()
        }));
    };
    TimepickerComponent.prototype.toggleMeridian = function () {
        if (!this.showMeridian || this.readonlyInput) {
            return;
        }
        var _hoursPerDayHalf = 12;
        this._store.dispatch(this._timepickerActions.changeHours({ step: _hoursPerDayHalf, source: '' }));
    };
    /**
     * Write a new value to the element.
     */
    TimepickerComponent.prototype.writeValue = function (obj) {
        if (Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["c" /* isValidDate */])(obj)) {
            this._store.dispatch(this._timepickerActions.writeValue(Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["e" /* parseTime */])(obj)));
        }
    };
    /**
     * Set the function to be called when the control receives a change event.
     */
    TimepickerComponent.prototype.registerOnChange = function (fn) {
        this.onChange = fn;
    };
    /**
     * Set the function to be called when the control receives a touch event.
     */
    TimepickerComponent.prototype.registerOnTouched = function (fn) {
        this.onTouched = fn;
    };
    /**
     * This function is called when the control status changes to or from "DISABLED".
     * Depending on the value, it will enable or disable the appropriate DOM element.
     *
     * @param isDisabled
     */
    TimepickerComponent.prototype.setDisabledState = function (isDisabled) {
        this.readonlyInput = isDisabled;
    };
    TimepickerComponent.prototype._renderTime = function (value) {
        if (!Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["c" /* isValidDate */])(value)) {
            this.hours = '';
            this.minutes = '';
            this.seconds = '';
            this.meridian = this.meridians[0];
            return;
        }
        var _value = Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["e" /* parseTime */])(value);
        var _hoursPerDayHalf = 12;
        var _hours = _value.getHours();
        if (this.showMeridian) {
            this.meridian = this.meridians[_hours >= _hoursPerDayHalf ? 1 : 0];
            _hours = _hours % _hoursPerDayHalf;
            // should be 12 PM, not 00 PM
            if (_hours === 0) {
                _hours = _hoursPerDayHalf;
            }
        }
        this.hours = Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["d" /* padNumber */])(_hours);
        this.minutes = Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["d" /* padNumber */])(_value.getMinutes());
        this.seconds = Object(__WEBPACK_IMPORTED_MODULE_6__timepicker_utils__["d" /* padNumber */])(_value.getUTCSeconds());
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], TimepickerComponent.prototype, "hourStep", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], TimepickerComponent.prototype, "minuteStep", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], TimepickerComponent.prototype, "secondsStep", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TimepickerComponent.prototype, "readonlyInput", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TimepickerComponent.prototype, "mousewheel", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TimepickerComponent.prototype, "arrowkeys", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TimepickerComponent.prototype, "showSpinners", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TimepickerComponent.prototype, "showMeridian", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TimepickerComponent.prototype, "showSeconds", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], TimepickerComponent.prototype, "meridians", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], TimepickerComponent.prototype, "min", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Date)
    ], TimepickerComponent.prototype, "max", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TimepickerComponent.prototype, "isValid", void 0);
    TimepickerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'timepicker',
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush,
            providers: [TIMEPICKER_CONTROL_VALUE_ACCESSOR, __WEBPACK_IMPORTED_MODULE_3__reducer_timepicker_store__["a" /* TimepickerStore */]],
            template: "\n    <table>\n      <tbody>\n      <tr class=\"text-center\" [class.hidden]=\"!isSpinnersVisible\">\n        <!-- increment hours button-->\n        <td>\n          <a class=\"btn btn-link\" [class.disabled]=\"!canIncrementHours\"\n             (click)=\"changeHours(hourStep)\"\n          ><span class=\"glyphicon glyphicon-chevron-up\"></span></a>\n        </td>\n        <!-- divider -->\n        <td>&nbsp;&nbsp;&nbsp;</td>\n        <!-- increment minutes button -->\n        <td>\n          <a class=\"btn btn-link\" [class.disabled]=\"!canIncrementMinutes\"\n             (click)=\"changeMinutes(minuteStep)\"\n          ><span class=\"glyphicon glyphicon-chevron-up\"></span></a>\n        </td>\n        <!-- divider -->\n        <td *ngIf=\"showSeconds\">&nbsp;</td>\n        <!-- increment seconds button -->\n        <td *ngIf=\"showSeconds\">\n          <a class=\"btn btn-link\" [class.disabled]=\"!canIncrementSeconds\"\n             (click)=\"changeSeconds(secondsStep)\">\n            <span class=\"glyphicon glyphicon-chevron-up\"></span>\n          </a>\n        </td>\n        <!-- space between -->\n        <td>&nbsp;&nbsp;&nbsp;</td>\n        <!-- meridian placeholder-->\n        <td *ngIf=\"showMeridian\"></td>\n      </tr>\n      <tr>\n        <!-- hours -->\n        <td class=\"form-group\" [class.has-error]=\"invalidHours\">\n          <input type=\"text\" style=\"width:50px;\"\n                 class=\"form-control text-center\"\n                 placeholder=\"HH\"\n                 maxlength=\"2\"\n                 [readonly]=\"readonlyInput\"\n                 [value]=\"hours\"\n                 (wheel)=\"prevDef($event);changeHours(hourStep * wheelSign($event), 'wheel')\"\n                 (keydown.ArrowUp)=\"changeHours(hourStep, 'key')\"\n                 (keydown.ArrowDown)=\"changeHours(-hourStep, 'key')\"\n                 (change)=\"updateHours($event.target.value)\"></td>\n        <!-- divider -->\n        <td>&nbsp;:&nbsp;</td>\n        <!-- minutes -->\n        <td class=\"form-group\" [class.has-error]=\"invalidMinutes\">\n          <input style=\"width:50px;\" type=\"text\"\n                 class=\"form-control text-center\"\n                 placeholder=\"MM\"\n                 maxlength=\"2\"\n                 [readonly]=\"readonlyInput\"\n                 [value]=\"minutes\"\n                 (wheel)=\"prevDef($event);changeMinutes(minuteStep * wheelSign($event), 'wheel')\"\n                 (keydown.ArrowUp)=\"changeMinutes(minuteStep, 'key')\"\n                 (keydown.ArrowDown)=\"changeMinutes(-minuteStep, 'key')\"\n                 (change)=\"updateMinutes($event.target.value)\">\n        </td>\n        <!-- divider -->\n        <td *ngIf=\"showSeconds\">&nbsp;:&nbsp;</td>\n        <!-- seconds -->\n        <td class=\"form-group\" *ngIf=\"showSeconds\" [class.has-error]=\"invalidSeconds\">\n          <input style=\"width:50px;\" type=\"text\"\n                 class=\"form-control text-center\"\n                 placeholder=\"SS\"\n                 maxlength=\"2\"\n                 [readonly]=\"readonlyInput\"\n                 [value]=\"seconds\"\n                 (wheel)=\"prevDef($event);changeSeconds(secondsStep * wheelSign($event), 'wheel')\"\n                 (keydown.ArrowUp)=\"changeSeconds(secondsStep, 'key')\"\n                 (keydown.ArrowDown)=\"changeSeconds(-secondsStep, 'key')\"\n                 (change)=\"updateSeconds($event.target.value)\">\n        </td>\n        <!-- space between -->\n        <td>&nbsp;&nbsp;&nbsp;</td>\n        <!-- meridian -->\n        <td *ngIf=\"showMeridian\">\n          <button type=\"button\" class=\"btn btn-default text-center\"\n                  [disabled]=\"readonlyInput\"\n                  [class.disabled]=\"readonlyInput\"\n                  (click)=\"toggleMeridian()\"\n          >{{ meridian }}\n          </button>\n        </td>\n      </tr>\n      <tr class=\"text-center\" [class.hidden]=\"!isSpinnersVisible\">\n        <!-- decrement hours button-->\n        <td>\n          <a class=\"btn btn-link\" [class.disabled]=\"!canDecrementHours\" (click)=\"changeHours(-hourStep)\">\n            <span class=\"glyphicon glyphicon-chevron-down\"></span>\n          </a>\n        </td>\n        <!-- divider -->\n        <td>&nbsp;&nbsp;&nbsp;</td>\n        <!-- decrement minutes button-->\n        <td>\n          <a class=\"btn btn-link\" [class.disabled]=\"!canDecrementMinutes\" (click)=\"changeMinutes(-minuteStep)\">\n            <span class=\"glyphicon glyphicon-chevron-down\"></span>\n          </a>\n        </td>\n        <!-- divider -->\n        <td *ngIf=\"showSeconds\">&nbsp;</td>\n        <!-- decrement seconds button-->\n        <td *ngIf=\"showSeconds\">\n          <a class=\"btn btn-link\" [class.disabled]=\"!canDecrementSeconds\" (click)=\"changeSeconds(-secondsStep)\">\n            <span class=\"glyphicon glyphicon-chevron-down\"></span>\n          </a>\n        </td>\n        <!-- space between -->\n        <td>&nbsp;&nbsp;&nbsp;</td>\n        <!-- meridian placeholder-->\n        <td *ngIf=\"showMeridian\"></td>\n      </tr>\n      </tbody>\n    </table>\n  "
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_5__timepicker_config__["a" /* TimepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectorRef"],
            __WEBPACK_IMPORTED_MODULE_3__reducer_timepicker_store__["a" /* TimepickerStore */],
            __WEBPACK_IMPORTED_MODULE_2__reducer_timepicker_actions__["a" /* TimepickerActions */]])
    ], TimepickerComponent);
    return TimepickerComponent;
}());



/***/ }),
/* 108 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["d"] = canChangeValue;
/* harmony export (immutable) */ __webpack_exports__["a"] = canChangeHours;
/* harmony export (immutable) */ __webpack_exports__["b"] = canChangeMinutes;
/* harmony export (immutable) */ __webpack_exports__["c"] = canChangeSeconds;
/* harmony export (immutable) */ __webpack_exports__["e"] = getControlsValue;
/* harmony export (immutable) */ __webpack_exports__["f"] = timepickerControls;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__timepicker_utils__ = __webpack_require__(53);

function canChangeValue(state, event) {
    if (state.readonlyInput) {
        return false;
    }
    if (event) {
        if (event.source === 'wheel' && !state.mousewheel) {
            return false;
        }
        if (event.source === 'key' && !state.arrowkeys) {
            return false;
        }
    }
    return true;
}
function canChangeHours(event, controls) {
    if (!event.step) {
        return false;
    }
    if (event.step > 0 && !controls.canIncrementHours) {
        return false;
    }
    if (event.step < 0 && !controls.canDecrementHours) {
        return false;
    }
    return true;
}
function canChangeMinutes(event, controls) {
    if (!event.step) {
        return false;
    }
    if (event.step > 0 && !controls.canIncrementMinutes) {
        return false;
    }
    if (event.step < 0 && !controls.canDecrementMinutes) {
        return false;
    }
    return true;
}
function canChangeSeconds(event, controls) {
    if (!event.step) {
        return false;
    }
    if (event.step > 0 && !controls.canIncrementSeconds) {
        return false;
    }
    if (event.step < 0 && !controls.canDecrementSeconds) {
        return false;
    }
    return true;
}
function getControlsValue(state) {
    var hourStep = state.hourStep, minuteStep = state.minuteStep, secondsStep = state.secondsStep, readonlyInput = state.readonlyInput, mousewheel = state.mousewheel, arrowkeys = state.arrowkeys, showSpinners = state.showSpinners, showMeridian = state.showMeridian, showSeconds = state.showSeconds, meridians = state.meridians, min = state.min, max = state.max;
    return {
        hourStep: hourStep, minuteStep: minuteStep, secondsStep: secondsStep,
        readonlyInput: readonlyInput, mousewheel: mousewheel, arrowkeys: arrowkeys,
        showSpinners: showSpinners, showMeridian: showMeridian, showSeconds: showSeconds,
        meridians: meridians, min: min, max: max
    };
}
function timepickerControls(value, state) {
    var min = state.min, max = state.max, hourStep = state.hourStep, minuteStep = state.minuteStep, secondsStep = state.secondsStep, showSeconds = state.showSeconds;
    var res = {
        canIncrementHours: true,
        canIncrementMinutes: true,
        canIncrementSeconds: true,
        canDecrementHours: true,
        canDecrementMinutes: true,
        canDecrementSeconds: true
    };
    if (!value) {
        return res;
    }
    // compare dates
    if (max) {
        var _newHour = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_utils__["a" /* changeTime */])(value, { hour: hourStep });
        res.canIncrementHours = max > _newHour;
        if (!res.canIncrementHours) {
            var _newMinutes = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_utils__["a" /* changeTime */])(value, { minute: minuteStep });
            res.canIncrementMinutes = showSeconds ? max > _newMinutes : max >= _newMinutes;
        }
        if (!res.canIncrementMinutes) {
            var _newSeconds = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_utils__["a" /* changeTime */])(value, { seconds: secondsStep });
            res.canIncrementSeconds = max >= _newSeconds;
        }
    }
    if (min) {
        var _newHour = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_utils__["a" /* changeTime */])(value, { hour: -hourStep });
        res.canDecrementHours = min < _newHour;
        if (!res.canDecrementHours) {
            var _newMinutes = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_utils__["a" /* changeTime */])(value, { minute: -minuteStep });
            res.canDecrementMinutes = showSeconds ? min < _newMinutes : min <= _newMinutes;
        }
        if (!res.canDecrementMinutes) {
            var _newSeconds = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_utils__["a" /* changeTime */])(value, { seconds: -secondsStep });
            res.canDecrementSeconds = min <= _newSeconds;
        }
    }
    return res;
}


/***/ }),
/* 109 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return MiniStore; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0_rxjs_Observable__ = __webpack_require__(110);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0_rxjs_Observable___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0_rxjs_Observable__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_operator_distinctUntilChanged__ = __webpack_require__(145);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_operator_distinctUntilChanged___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_rxjs_operator_distinctUntilChanged__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_operator_map__ = __webpack_require__(146);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_operator_map___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_operator_map__);
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
/**
 * @copyright ngrx
 */



var MiniStore = (function (_super) {
    __extends(MiniStore, _super);
    function MiniStore(_dispatcher, _reducer, state$) {
        var _this = _super.call(this) || this;
        _this._dispatcher = _dispatcher;
        _this._reducer = _reducer;
        _this.source = state$;
        return _this;
    }
    MiniStore.prototype.select = function (pathOrMapFn) {
        var mapped$ = __WEBPACK_IMPORTED_MODULE_2_rxjs_operator_map__["map"].call(this, pathOrMapFn);
        return __WEBPACK_IMPORTED_MODULE_1_rxjs_operator_distinctUntilChanged__["distinctUntilChanged"].call(mapped$);
    };
    MiniStore.prototype.lift = function (operator) {
        var store = new MiniStore(this._dispatcher, this._reducer, this);
        store.operator = operator;
        return store;
    };
    MiniStore.prototype.dispatch = function (action) { this._dispatcher.next(action); };
    MiniStore.prototype.next = function (action) { this._dispatcher.next(action); };
    MiniStore.prototype.error = function (err) { this._dispatcher.error(err); };
    MiniStore.prototype.complete = function () { };
    return MiniStore;
}(__WEBPACK_IMPORTED_MODULE_0_rxjs_Observable__["Observable"]));



/***/ }),
/* 110 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_110__;

/***/ }),
/* 111 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return MiniState; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0_rxjs_BehaviorSubject__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0_rxjs_BehaviorSubject___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0_rxjs_BehaviorSubject__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_operator_observeOn__ = __webpack_require__(147);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_operator_observeOn___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_rxjs_operator_observeOn__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_scheduler_queue__ = __webpack_require__(148);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_scheduler_queue___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_scheduler_queue__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_operator_scan__ = __webpack_require__(149);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_operator_scan___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_operator_scan__);
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
/**
 * @copyright ngrx
 */




var MiniState = (function (_super) {
    __extends(MiniState, _super);
    function MiniState(_initialState, actionsDispatcher$, reducer) {
        var _this = _super.call(this, _initialState) || this;
        var actionInQueue$ = __WEBPACK_IMPORTED_MODULE_1_rxjs_operator_observeOn__["observeOn"].call(actionsDispatcher$, __WEBPACK_IMPORTED_MODULE_2_rxjs_scheduler_queue__["queue"]);
        var state$ = __WEBPACK_IMPORTED_MODULE_3_rxjs_operator_scan__["scan"].call(actionInQueue$, function (state, action) {
            if (!action) {
                return state;
            }
            return reducer(state, action);
        }, _initialState);
        state$.subscribe(function (value) { return _this.next(value); });
        return _this;
    }
    return MiniState;
}(__WEBPACK_IMPORTED_MODULE_0_rxjs_BehaviorSubject__["BehaviorSubject"]));



/***/ }),
/* 112 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TooltipModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__tooltip_container_component__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__tooltip_directive__ = __webpack_require__(113);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__tooltip_config__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__positioning__ = __webpack_require__(9);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var TooltipModule = (function () {
    function TooltipModule() {
    }
    TooltipModule_1 = TooltipModule;
    TooltipModule.forRoot = function () {
        return {
            ngModule: TooltipModule_1,
            providers: [__WEBPACK_IMPORTED_MODULE_4__tooltip_config__["a" /* TooltipConfig */], __WEBPACK_IMPORTED_MODULE_5__component_loader__["b" /* ComponentLoaderFactory */], __WEBPACK_IMPORTED_MODULE_6__positioning__["b" /* PositioningService */]]
        };
    };
    ;
    TooltipModule = TooltipModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_3__tooltip_directive__["a" /* TooltipDirective */], __WEBPACK_IMPORTED_MODULE_2__tooltip_container_component__["a" /* TooltipContainerComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_3__tooltip_directive__["a" /* TooltipDirective */]],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_2__tooltip_container_component__["a" /* TooltipContainerComponent */]]
        })
    ], TooltipModule);
    return TooltipModule;
    var TooltipModule_1;
}());



/***/ }),
/* 113 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TooltipDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tooltip_container_component__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__tooltip_config__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__utils_decorators__ = __webpack_require__(34);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__utils_warn_once__ = __webpack_require__(114);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};






var TooltipDirective = (function () {
    // tslint:disable-next-line
    function TooltipDirective(_viewContainerRef, _renderer, _elementRef, cis, config) {
        /** Fired when tooltip content changes */
        this.tooltipChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /**
         * Css class for tooltip container
         */
        this.containerClass = '';
        /** @deprecated - removed, will be added to configuration */
        this._animation = true;
        /** @deprecated */
        this._delay = 0;
        /** @deprecated */
        this._fadeDuration = 150;
        /** @deprecated */
        this.tooltipStateChanged = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this._tooltip = cis
            .createLoader(_elementRef, _viewContainerRef, _renderer)
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_2__tooltip_config__["a" /* TooltipConfig */], useValue: config });
        Object.assign(this, config);
        this.onShown = this._tooltip.onShown;
        this.onHidden = this._tooltip.onHidden;
    }
    Object.defineProperty(TooltipDirective.prototype, "isOpen", {
        /**
         * Returns whether or not the tooltip is currently being shown
         */
        get: function () { return this._tooltip.isShown; },
        set: function (value) {
            if (value) {
                this.show();
            }
            else {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "htmlContent", {
        /* tslint:disable */
        /** @deprecated - please use `tooltip` instead */
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipHtml was deprecated, please use `tooltip` instead');
            this.tooltip = value;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_placement", {
        /** @deprecated - please use `placement` instead */
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipPlacement was deprecated, please use `placement` instead');
            this.placement = value;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_isOpen", {
        get: function () {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipIsOpen was deprecated, please use `isOpen` instead');
            return this.isOpen;
        },
        /** @deprecated - please use `isOpen` instead*/
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipIsOpen was deprecated, please use `isOpen` instead');
            this.isOpen = value;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_enable", {
        get: function () {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipEnable was deprecated, please use `isDisabled` instead');
            return this.isDisabled === true;
        },
        /** @deprecated - please use `isDisabled` instead */
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipEnable was deprecated, please use `isDisabled` instead');
            this.isDisabled = value === true;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_appendToBody", {
        get: function () {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipAppendToBody was deprecated, please use `container="body"` instead');
            return this.container === 'body';
        },
        /** @deprecated - please use `container="body"` instead */
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipAppendToBody was deprecated, please use `container="body"` instead');
            this.container = value ? 'body' : this.container;
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_popupClass", {
        /** @deprecated - will replaced with customClass */
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipClass deprecated');
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_tooltipContext", {
        /** @deprecated - removed */
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipContext deprecated');
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(TooltipDirective.prototype, "_tooltipTrigger", {
        /** @deprecated -  please use `triggers` instead */
        get: function () {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipTrigger was deprecated, please use `triggers` instead');
            return this.triggers;
        },
        set: function (value) {
            Object(__WEBPACK_IMPORTED_MODULE_5__utils_warn_once__["a" /* warnOnce */])('tooltipTrigger was deprecated, please use `triggers` instead');
            this.triggers = (value || '').toString();
        },
        enumerable: true,
        configurable: true
    });
    ;
    ;
    TooltipDirective.prototype.ngOnInit = function () {
        var _this = this;
        this._tooltip.listen({
            triggers: this.triggers,
            show: function () { return _this.show(); }
        });
        this.tooltipChange.subscribe(function (value) {
            if (!value) {
                _this._tooltip.hide();
            }
        });
    };
    /**
     * Toggles an element’s tooltip. This is considered a “manual” triggering of
     * the tooltip.
     */
    TooltipDirective.prototype.toggle = function () {
        if (this.isOpen) {
            return this.hide();
        }
        this.show();
    };
    /**
     * Opens an element’s tooltip. This is considered a “manual” triggering of
     * the tooltip.
     */
    TooltipDirective.prototype.show = function () {
        var _this = this;
        if (this.isOpen || this.isDisabled || this._delayTimeoutId || !this.tooltip) {
            return;
        }
        var showTooltip = function () { return _this._tooltip
            .attach(__WEBPACK_IMPORTED_MODULE_1__tooltip_container_component__["a" /* TooltipContainerComponent */])
            .to(_this.container)
            .position({ attachment: _this.placement })
            .show({
            content: _this.tooltip,
            placement: _this.placement,
            containerClass: _this.containerClass
        }); };
        if (this._delay) {
            this._delayTimeoutId = setTimeout(function () { showTooltip(); }, this._delay);
        }
        else {
            showTooltip();
        }
    };
    /**
     * Closes an element’s tooltip. This is considered a “manual” triggering of
     * the tooltip.
     */
    TooltipDirective.prototype.hide = function () {
        var _this = this;
        if (this._delayTimeoutId) {
            clearTimeout(this._delayTimeoutId);
            this._delayTimeoutId = undefined;
        }
        if (!this._tooltip.isShown) {
            return;
        }
        this._tooltip.instance.classMap.in = false;
        setTimeout(function () {
            _this._tooltip.hide();
        }, this._fadeDuration);
    };
    TooltipDirective.prototype.ngOnDestroy = function () {
        this._tooltip.dispose();
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_4__utils_decorators__["a" /* OnChange */])(),
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], TooltipDirective.prototype, "tooltip", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TooltipDirective.prototype, "tooltipChange", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TooltipDirective.prototype, "placement", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TooltipDirective.prototype, "triggers", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TooltipDirective.prototype, "container", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TooltipDirective.prototype, "isOpen", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TooltipDirective.prototype, "isDisabled", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TooltipDirective.prototype, "containerClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TooltipDirective.prototype, "onShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TooltipDirective.prototype, "onHidden", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipHtml'),
        __metadata("design:type", Object),
        __metadata("design:paramtypes", [Object])
    ], TooltipDirective.prototype, "htmlContent", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipPlacement'),
        __metadata("design:type", String),
        __metadata("design:paramtypes", [String])
    ], TooltipDirective.prototype, "_placement", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipIsOpen'),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TooltipDirective.prototype, "_isOpen", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipEnable'),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TooltipDirective.prototype, "_enable", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipAppendToBody'),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], TooltipDirective.prototype, "_appendToBody", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipAnimation'),
        __metadata("design:type", Boolean)
    ], TooltipDirective.prototype, "_animation", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipClass'),
        __metadata("design:type", String),
        __metadata("design:paramtypes", [String])
    ], TooltipDirective.prototype, "_popupClass", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipContext'),
        __metadata("design:type", Object),
        __metadata("design:paramtypes", [Object])
    ], TooltipDirective.prototype, "_tooltipContext", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipPopupDelay'),
        __metadata("design:type", Number)
    ], TooltipDirective.prototype, "_delay", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipFadeDuration'),
        __metadata("design:type", Number)
    ], TooltipDirective.prototype, "_fadeDuration", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])('tooltipTrigger'),
        __metadata("design:type", Object),
        __metadata("design:paramtypes", [Object])
    ], TooltipDirective.prototype, "_tooltipTrigger", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TooltipDirective.prototype, "tooltipStateChanged", void 0);
    TooltipDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: '[tooltip], [tooltipHtml]',
            exportAs: 'bs-tooltip'
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"],
            __WEBPACK_IMPORTED_MODULE_3__component_loader__["b" /* ComponentLoaderFactory */],
            __WEBPACK_IMPORTED_MODULE_2__tooltip_config__["a" /* TooltipConfig */]])
    ], TooltipDirective);
    return TooltipDirective;
}());



/***/ }),
/* 114 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = warnOnce;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);

var _messagesHash = {};
var _hideMsg = typeof console === 'undefined' || !console.warn;
function warnOnce(msg) {
    if (!Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["isDevMode"])() || _hideMsg || msg in _messagesHash) {
        return;
    }
    _messagesHash[msg] = true;
    /*tslint:disable-next-line*/
    console.warn(msg);
}


/***/ }),
/* 115 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TypeaheadModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__typeahead_container_component__ = __webpack_require__(56);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__typeahead_directive__ = __webpack_require__(117);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__positioning__ = __webpack_require__(9);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};






var TypeaheadModule = (function () {
    function TypeaheadModule() {
    }
    TypeaheadModule_1 = TypeaheadModule;
    TypeaheadModule.forRoot = function () {
        return {
            ngModule: TypeaheadModule_1,
            providers: [__WEBPACK_IMPORTED_MODULE_4__component_loader__["b" /* ComponentLoaderFactory */], __WEBPACK_IMPORTED_MODULE_5__positioning__["b" /* PositioningService */]]
        };
    };
    ;
    TypeaheadModule = TypeaheadModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_2__typeahead_container_component__["a" /* TypeaheadContainerComponent */], __WEBPACK_IMPORTED_MODULE_3__typeahead_directive__["a" /* TypeaheadDirective */]],
            exports: [__WEBPACK_IMPORTED_MODULE_2__typeahead_container_component__["a" /* TypeaheadContainerComponent */], __WEBPACK_IMPORTED_MODULE_3__typeahead_directive__["a" /* TypeaheadDirective */]],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_2__typeahead_container_component__["a" /* TypeaheadContainerComponent */]]
        })
    ], TypeaheadModule);
    return TypeaheadModule;
    var TypeaheadModule_1;
}());



/***/ }),
/* 116 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return latinMap; });
/* tslint:disable:max-file-line-count */
var latinMap = {
    'Á': 'A',
    'Ă': 'A',
    'Ắ': 'A',
    'Ặ': 'A',
    'Ằ': 'A',
    'Ẳ': 'A',
    'Ẵ': 'A',
    'Ǎ': 'A',
    'Â': 'A',
    'Ấ': 'A',
    'Ậ': 'A',
    'Ầ': 'A',
    'Ẩ': 'A',
    'Ẫ': 'A',
    'Ä': 'A',
    'Ǟ': 'A',
    'Ȧ': 'A',
    'Ǡ': 'A',
    'Ạ': 'A',
    'Ȁ': 'A',
    'À': 'A',
    'Ả': 'A',
    'Ȃ': 'A',
    'Ā': 'A',
    'Ą': 'A',
    'Å': 'A',
    'Ǻ': 'A',
    'Ḁ': 'A',
    'Ⱥ': 'A',
    'Ã': 'A',
    'Ꜳ': 'AA',
    'Æ': 'AE',
    'Ǽ': 'AE',
    'Ǣ': 'AE',
    'Ꜵ': 'AO',
    'Ꜷ': 'AU',
    'Ꜹ': 'AV',
    'Ꜻ': 'AV',
    'Ꜽ': 'AY',
    'Ḃ': 'B',
    'Ḅ': 'B',
    'Ɓ': 'B',
    'Ḇ': 'B',
    'Ƀ': 'B',
    'Ƃ': 'B',
    'Ć': 'C',
    'Č': 'C',
    'Ç': 'C',
    'Ḉ': 'C',
    'Ĉ': 'C',
    'Ċ': 'C',
    'Ƈ': 'C',
    'Ȼ': 'C',
    'Ď': 'D',
    'Ḑ': 'D',
    'Ḓ': 'D',
    'Ḋ': 'D',
    'Ḍ': 'D',
    'Ɗ': 'D',
    'Ḏ': 'D',
    'ǲ': 'D',
    'ǅ': 'D',
    'Đ': 'D',
    'Ƌ': 'D',
    'Ǳ': 'DZ',
    'Ǆ': 'DZ',
    'É': 'E',
    'Ĕ': 'E',
    'Ě': 'E',
    'Ȩ': 'E',
    'Ḝ': 'E',
    'Ê': 'E',
    'Ế': 'E',
    'Ệ': 'E',
    'Ề': 'E',
    'Ể': 'E',
    'Ễ': 'E',
    'Ḙ': 'E',
    'Ë': 'E',
    'Ė': 'E',
    'Ẹ': 'E',
    'Ȅ': 'E',
    'È': 'E',
    'Ẻ': 'E',
    'Ȇ': 'E',
    'Ē': 'E',
    'Ḗ': 'E',
    'Ḕ': 'E',
    'Ę': 'E',
    'Ɇ': 'E',
    'Ẽ': 'E',
    'Ḛ': 'E',
    'Ꝫ': 'ET',
    'Ḟ': 'F',
    'Ƒ': 'F',
    'Ǵ': 'G',
    'Ğ': 'G',
    'Ǧ': 'G',
    'Ģ': 'G',
    'Ĝ': 'G',
    'Ġ': 'G',
    'Ɠ': 'G',
    'Ḡ': 'G',
    'Ǥ': 'G',
    'Ḫ': 'H',
    'Ȟ': 'H',
    'Ḩ': 'H',
    'Ĥ': 'H',
    'Ⱨ': 'H',
    'Ḧ': 'H',
    'Ḣ': 'H',
    'Ḥ': 'H',
    'Ħ': 'H',
    'Í': 'I',
    'Ĭ': 'I',
    'Ǐ': 'I',
    'Î': 'I',
    'Ï': 'I',
    'Ḯ': 'I',
    'İ': 'I',
    'Ị': 'I',
    'Ȉ': 'I',
    'Ì': 'I',
    'Ỉ': 'I',
    'Ȋ': 'I',
    'Ī': 'I',
    'Į': 'I',
    'Ɨ': 'I',
    'Ĩ': 'I',
    'Ḭ': 'I',
    'Ꝺ': 'D',
    'Ꝼ': 'F',
    'Ᵹ': 'G',
    'Ꞃ': 'R',
    'Ꞅ': 'S',
    'Ꞇ': 'T',
    'Ꝭ': 'IS',
    'Ĵ': 'J',
    'Ɉ': 'J',
    'Ḱ': 'K',
    'Ǩ': 'K',
    'Ķ': 'K',
    'Ⱪ': 'K',
    'Ꝃ': 'K',
    'Ḳ': 'K',
    'Ƙ': 'K',
    'Ḵ': 'K',
    'Ꝁ': 'K',
    'Ꝅ': 'K',
    'Ĺ': 'L',
    'Ƚ': 'L',
    'Ľ': 'L',
    'Ļ': 'L',
    'Ḽ': 'L',
    'Ḷ': 'L',
    'Ḹ': 'L',
    'Ⱡ': 'L',
    'Ꝉ': 'L',
    'Ḻ': 'L',
    'Ŀ': 'L',
    'Ɫ': 'L',
    'ǈ': 'L',
    'Ł': 'L',
    'Ǉ': 'LJ',
    'Ḿ': 'M',
    'Ṁ': 'M',
    'Ṃ': 'M',
    'Ɱ': 'M',
    'Ń': 'N',
    'Ň': 'N',
    'Ņ': 'N',
    'Ṋ': 'N',
    'Ṅ': 'N',
    'Ṇ': 'N',
    'Ǹ': 'N',
    'Ɲ': 'N',
    'Ṉ': 'N',
    'Ƞ': 'N',
    'ǋ': 'N',
    'Ñ': 'N',
    'Ǌ': 'NJ',
    'Ó': 'O',
    'Ŏ': 'O',
    'Ǒ': 'O',
    'Ô': 'O',
    'Ố': 'O',
    'Ộ': 'O',
    'Ồ': 'O',
    'Ổ': 'O',
    'Ỗ': 'O',
    'Ö': 'O',
    'Ȫ': 'O',
    'Ȯ': 'O',
    'Ȱ': 'O',
    'Ọ': 'O',
    'Ő': 'O',
    'Ȍ': 'O',
    'Ò': 'O',
    'Ỏ': 'O',
    'Ơ': 'O',
    'Ớ': 'O',
    'Ợ': 'O',
    'Ờ': 'O',
    'Ở': 'O',
    'Ỡ': 'O',
    'Ȏ': 'O',
    'Ꝋ': 'O',
    'Ꝍ': 'O',
    'Ō': 'O',
    'Ṓ': 'O',
    'Ṑ': 'O',
    'Ɵ': 'O',
    'Ǫ': 'O',
    'Ǭ': 'O',
    'Ø': 'O',
    'Ǿ': 'O',
    'Õ': 'O',
    'Ṍ': 'O',
    'Ṏ': 'O',
    'Ȭ': 'O',
    'Ƣ': 'OI',
    'Ꝏ': 'OO',
    'Ɛ': 'E',
    'Ɔ': 'O',
    'Ȣ': 'OU',
    'Ṕ': 'P',
    'Ṗ': 'P',
    'Ꝓ': 'P',
    'Ƥ': 'P',
    'Ꝕ': 'P',
    'Ᵽ': 'P',
    'Ꝑ': 'P',
    'Ꝙ': 'Q',
    'Ꝗ': 'Q',
    'Ŕ': 'R',
    'Ř': 'R',
    'Ŗ': 'R',
    'Ṙ': 'R',
    'Ṛ': 'R',
    'Ṝ': 'R',
    'Ȑ': 'R',
    'Ȓ': 'R',
    'Ṟ': 'R',
    'Ɍ': 'R',
    'Ɽ': 'R',
    'Ꜿ': 'C',
    'Ǝ': 'E',
    'Ś': 'S',
    'Ṥ': 'S',
    'Š': 'S',
    'Ṧ': 'S',
    'Ş': 'S',
    'Ŝ': 'S',
    'Ș': 'S',
    'Ṡ': 'S',
    'Ṣ': 'S',
    'Ṩ': 'S',
    'Ť': 'T',
    'Ţ': 'T',
    'Ṱ': 'T',
    'Ț': 'T',
    'Ⱦ': 'T',
    'Ṫ': 'T',
    'Ṭ': 'T',
    'Ƭ': 'T',
    'Ṯ': 'T',
    'Ʈ': 'T',
    'Ŧ': 'T',
    'Ɐ': 'A',
    'Ꞁ': 'L',
    'Ɯ': 'M',
    'Ʌ': 'V',
    'Ꜩ': 'TZ',
    'Ú': 'U',
    'Ŭ': 'U',
    'Ǔ': 'U',
    'Û': 'U',
    'Ṷ': 'U',
    'Ü': 'U',
    'Ǘ': 'U',
    'Ǚ': 'U',
    'Ǜ': 'U',
    'Ǖ': 'U',
    'Ṳ': 'U',
    'Ụ': 'U',
    'Ű': 'U',
    'Ȕ': 'U',
    'Ù': 'U',
    'Ủ': 'U',
    'Ư': 'U',
    'Ứ': 'U',
    'Ự': 'U',
    'Ừ': 'U',
    'Ử': 'U',
    'Ữ': 'U',
    'Ȗ': 'U',
    'Ū': 'U',
    'Ṻ': 'U',
    'Ų': 'U',
    'Ů': 'U',
    'Ũ': 'U',
    'Ṹ': 'U',
    'Ṵ': 'U',
    'Ꝟ': 'V',
    'Ṿ': 'V',
    'Ʋ': 'V',
    'Ṽ': 'V',
    'Ꝡ': 'VY',
    'Ẃ': 'W',
    'Ŵ': 'W',
    'Ẅ': 'W',
    'Ẇ': 'W',
    'Ẉ': 'W',
    'Ẁ': 'W',
    'Ⱳ': 'W',
    'Ẍ': 'X',
    'Ẋ': 'X',
    'Ý': 'Y',
    'Ŷ': 'Y',
    'Ÿ': 'Y',
    'Ẏ': 'Y',
    'Ỵ': 'Y',
    'Ỳ': 'Y',
    'Ƴ': 'Y',
    'Ỷ': 'Y',
    'Ỿ': 'Y',
    'Ȳ': 'Y',
    'Ɏ': 'Y',
    'Ỹ': 'Y',
    'Ź': 'Z',
    'Ž': 'Z',
    'Ẑ': 'Z',
    'Ⱬ': 'Z',
    'Ż': 'Z',
    'Ẓ': 'Z',
    'Ȥ': 'Z',
    'Ẕ': 'Z',
    'Ƶ': 'Z',
    'Ĳ': 'IJ',
    'Œ': 'OE',
    'ᴀ': 'A',
    'ᴁ': 'AE',
    'ʙ': 'B',
    'ᴃ': 'B',
    'ᴄ': 'C',
    'ᴅ': 'D',
    'ᴇ': 'E',
    'ꜰ': 'F',
    'ɢ': 'G',
    'ʛ': 'G',
    'ʜ': 'H',
    'ɪ': 'I',
    'ʁ': 'R',
    'ᴊ': 'J',
    'ᴋ': 'K',
    'ʟ': 'L',
    'ᴌ': 'L',
    'ᴍ': 'M',
    'ɴ': 'N',
    'ᴏ': 'O',
    'ɶ': 'OE',
    'ᴐ': 'O',
    'ᴕ': 'OU',
    'ᴘ': 'P',
    'ʀ': 'R',
    'ᴎ': 'N',
    'ᴙ': 'R',
    'ꜱ': 'S',
    'ᴛ': 'T',
    'ⱻ': 'E',
    'ᴚ': 'R',
    'ᴜ': 'U',
    'ᴠ': 'V',
    'ᴡ': 'W',
    'ʏ': 'Y',
    'ᴢ': 'Z',
    'á': 'a',
    'ă': 'a',
    'ắ': 'a',
    'ặ': 'a',
    'ằ': 'a',
    'ẳ': 'a',
    'ẵ': 'a',
    'ǎ': 'a',
    'â': 'a',
    'ấ': 'a',
    'ậ': 'a',
    'ầ': 'a',
    'ẩ': 'a',
    'ẫ': 'a',
    'ä': 'a',
    'ǟ': 'a',
    'ȧ': 'a',
    'ǡ': 'a',
    'ạ': 'a',
    'ȁ': 'a',
    'à': 'a',
    'ả': 'a',
    'ȃ': 'a',
    'ā': 'a',
    'ą': 'a',
    'ᶏ': 'a',
    'ẚ': 'a',
    'å': 'a',
    'ǻ': 'a',
    'ḁ': 'a',
    'ⱥ': 'a',
    'ã': 'a',
    'ꜳ': 'aa',
    'æ': 'ae',
    'ǽ': 'ae',
    'ǣ': 'ae',
    'ꜵ': 'ao',
    'ꜷ': 'au',
    'ꜹ': 'av',
    'ꜻ': 'av',
    'ꜽ': 'ay',
    'ḃ': 'b',
    'ḅ': 'b',
    'ɓ': 'b',
    'ḇ': 'b',
    'ᵬ': 'b',
    'ᶀ': 'b',
    'ƀ': 'b',
    'ƃ': 'b',
    'ɵ': 'o',
    'ć': 'c',
    'č': 'c',
    'ç': 'c',
    'ḉ': 'c',
    'ĉ': 'c',
    'ɕ': 'c',
    'ċ': 'c',
    'ƈ': 'c',
    'ȼ': 'c',
    'ď': 'd',
    'ḑ': 'd',
    'ḓ': 'd',
    'ȡ': 'd',
    'ḋ': 'd',
    'ḍ': 'd',
    'ɗ': 'd',
    'ᶑ': 'd',
    'ḏ': 'd',
    'ᵭ': 'd',
    'ᶁ': 'd',
    'đ': 'd',
    'ɖ': 'd',
    'ƌ': 'd',
    'ı': 'i',
    'ȷ': 'j',
    'ɟ': 'j',
    'ʄ': 'j',
    'ǳ': 'dz',
    'ǆ': 'dz',
    'é': 'e',
    'ĕ': 'e',
    'ě': 'e',
    'ȩ': 'e',
    'ḝ': 'e',
    'ê': 'e',
    'ế': 'e',
    'ệ': 'e',
    'ề': 'e',
    'ể': 'e',
    'ễ': 'e',
    'ḙ': 'e',
    'ë': 'e',
    'ė': 'e',
    'ẹ': 'e',
    'ȅ': 'e',
    'è': 'e',
    'ẻ': 'e',
    'ȇ': 'e',
    'ē': 'e',
    'ḗ': 'e',
    'ḕ': 'e',
    'ⱸ': 'e',
    'ę': 'e',
    'ᶒ': 'e',
    'ɇ': 'e',
    'ẽ': 'e',
    'ḛ': 'e',
    'ꝫ': 'et',
    'ḟ': 'f',
    'ƒ': 'f',
    'ᵮ': 'f',
    'ᶂ': 'f',
    'ǵ': 'g',
    'ğ': 'g',
    'ǧ': 'g',
    'ģ': 'g',
    'ĝ': 'g',
    'ġ': 'g',
    'ɠ': 'g',
    'ḡ': 'g',
    'ᶃ': 'g',
    'ǥ': 'g',
    'ḫ': 'h',
    'ȟ': 'h',
    'ḩ': 'h',
    'ĥ': 'h',
    'ⱨ': 'h',
    'ḧ': 'h',
    'ḣ': 'h',
    'ḥ': 'h',
    'ɦ': 'h',
    'ẖ': 'h',
    'ħ': 'h',
    'ƕ': 'hv',
    'í': 'i',
    'ĭ': 'i',
    'ǐ': 'i',
    'î': 'i',
    'ï': 'i',
    'ḯ': 'i',
    'ị': 'i',
    'ȉ': 'i',
    'ì': 'i',
    'ỉ': 'i',
    'ȋ': 'i',
    'ī': 'i',
    'į': 'i',
    'ᶖ': 'i',
    'ɨ': 'i',
    'ĩ': 'i',
    'ḭ': 'i',
    'ꝺ': 'd',
    'ꝼ': 'f',
    'ᵹ': 'g',
    'ꞃ': 'r',
    'ꞅ': 's',
    'ꞇ': 't',
    'ꝭ': 'is',
    'ǰ': 'j',
    'ĵ': 'j',
    'ʝ': 'j',
    'ɉ': 'j',
    'ḱ': 'k',
    'ǩ': 'k',
    'ķ': 'k',
    'ⱪ': 'k',
    'ꝃ': 'k',
    'ḳ': 'k',
    'ƙ': 'k',
    'ḵ': 'k',
    'ᶄ': 'k',
    'ꝁ': 'k',
    'ꝅ': 'k',
    'ĺ': 'l',
    'ƚ': 'l',
    'ɬ': 'l',
    'ľ': 'l',
    'ļ': 'l',
    'ḽ': 'l',
    'ȴ': 'l',
    'ḷ': 'l',
    'ḹ': 'l',
    'ⱡ': 'l',
    'ꝉ': 'l',
    'ḻ': 'l',
    'ŀ': 'l',
    'ɫ': 'l',
    'ᶅ': 'l',
    'ɭ': 'l',
    'ł': 'l',
    'ǉ': 'lj',
    'ſ': 's',
    'ẜ': 's',
    'ẛ': 's',
    'ẝ': 's',
    'ḿ': 'm',
    'ṁ': 'm',
    'ṃ': 'm',
    'ɱ': 'm',
    'ᵯ': 'm',
    'ᶆ': 'm',
    'ń': 'n',
    'ň': 'n',
    'ņ': 'n',
    'ṋ': 'n',
    'ȵ': 'n',
    'ṅ': 'n',
    'ṇ': 'n',
    'ǹ': 'n',
    'ɲ': 'n',
    'ṉ': 'n',
    'ƞ': 'n',
    'ᵰ': 'n',
    'ᶇ': 'n',
    'ɳ': 'n',
    'ñ': 'n',
    'ǌ': 'nj',
    'ó': 'o',
    'ŏ': 'o',
    'ǒ': 'o',
    'ô': 'o',
    'ố': 'o',
    'ộ': 'o',
    'ồ': 'o',
    'ổ': 'o',
    'ỗ': 'o',
    'ö': 'o',
    'ȫ': 'o',
    'ȯ': 'o',
    'ȱ': 'o',
    'ọ': 'o',
    'ő': 'o',
    'ȍ': 'o',
    'ò': 'o',
    'ỏ': 'o',
    'ơ': 'o',
    'ớ': 'o',
    'ợ': 'o',
    'ờ': 'o',
    'ở': 'o',
    'ỡ': 'o',
    'ȏ': 'o',
    'ꝋ': 'o',
    'ꝍ': 'o',
    'ⱺ': 'o',
    'ō': 'o',
    'ṓ': 'o',
    'ṑ': 'o',
    'ǫ': 'o',
    'ǭ': 'o',
    'ø': 'o',
    'ǿ': 'o',
    'õ': 'o',
    'ṍ': 'o',
    'ṏ': 'o',
    'ȭ': 'o',
    'ƣ': 'oi',
    'ꝏ': 'oo',
    'ɛ': 'e',
    'ᶓ': 'e',
    'ɔ': 'o',
    'ᶗ': 'o',
    'ȣ': 'ou',
    'ṕ': 'p',
    'ṗ': 'p',
    'ꝓ': 'p',
    'ƥ': 'p',
    'ᵱ': 'p',
    'ᶈ': 'p',
    'ꝕ': 'p',
    'ᵽ': 'p',
    'ꝑ': 'p',
    'ꝙ': 'q',
    'ʠ': 'q',
    'ɋ': 'q',
    'ꝗ': 'q',
    'ŕ': 'r',
    'ř': 'r',
    'ŗ': 'r',
    'ṙ': 'r',
    'ṛ': 'r',
    'ṝ': 'r',
    'ȑ': 'r',
    'ɾ': 'r',
    'ᵳ': 'r',
    'ȓ': 'r',
    'ṟ': 'r',
    'ɼ': 'r',
    'ᵲ': 'r',
    'ᶉ': 'r',
    'ɍ': 'r',
    'ɽ': 'r',
    'ↄ': 'c',
    'ꜿ': 'c',
    'ɘ': 'e',
    'ɿ': 'r',
    'ś': 's',
    'ṥ': 's',
    'š': 's',
    'ṧ': 's',
    'ş': 's',
    'ŝ': 's',
    'ș': 's',
    'ṡ': 's',
    'ṣ': 's',
    'ṩ': 's',
    'ʂ': 's',
    'ᵴ': 's',
    'ᶊ': 's',
    'ȿ': 's',
    'ɡ': 'g',
    'ᴑ': 'o',
    'ᴓ': 'o',
    'ᴝ': 'u',
    'ť': 't',
    'ţ': 't',
    'ṱ': 't',
    'ț': 't',
    'ȶ': 't',
    'ẗ': 't',
    'ⱦ': 't',
    'ṫ': 't',
    'ṭ': 't',
    'ƭ': 't',
    'ṯ': 't',
    'ᵵ': 't',
    'ƫ': 't',
    'ʈ': 't',
    'ŧ': 't',
    'ᵺ': 'th',
    'ɐ': 'a',
    'ᴂ': 'ae',
    'ǝ': 'e',
    'ᵷ': 'g',
    'ɥ': 'h',
    'ʮ': 'h',
    'ʯ': 'h',
    'ᴉ': 'i',
    'ʞ': 'k',
    'ꞁ': 'l',
    'ɯ': 'm',
    'ɰ': 'm',
    'ᴔ': 'oe',
    'ɹ': 'r',
    'ɻ': 'r',
    'ɺ': 'r',
    'ⱹ': 'r',
    'ʇ': 't',
    'ʌ': 'v',
    'ʍ': 'w',
    'ʎ': 'y',
    'ꜩ': 'tz',
    'ú': 'u',
    'ŭ': 'u',
    'ǔ': 'u',
    'û': 'u',
    'ṷ': 'u',
    'ü': 'u',
    'ǘ': 'u',
    'ǚ': 'u',
    'ǜ': 'u',
    'ǖ': 'u',
    'ṳ': 'u',
    'ụ': 'u',
    'ű': 'u',
    'ȕ': 'u',
    'ù': 'u',
    'ủ': 'u',
    'ư': 'u',
    'ứ': 'u',
    'ự': 'u',
    'ừ': 'u',
    'ử': 'u',
    'ữ': 'u',
    'ȗ': 'u',
    'ū': 'u',
    'ṻ': 'u',
    'ų': 'u',
    'ᶙ': 'u',
    'ů': 'u',
    'ũ': 'u',
    'ṹ': 'u',
    'ṵ': 'u',
    'ᵫ': 'ue',
    'ꝸ': 'um',
    'ⱴ': 'v',
    'ꝟ': 'v',
    'ṿ': 'v',
    'ʋ': 'v',
    'ᶌ': 'v',
    'ⱱ': 'v',
    'ṽ': 'v',
    'ꝡ': 'vy',
    'ẃ': 'w',
    'ŵ': 'w',
    'ẅ': 'w',
    'ẇ': 'w',
    'ẉ': 'w',
    'ẁ': 'w',
    'ⱳ': 'w',
    'ẘ': 'w',
    'ẍ': 'x',
    'ẋ': 'x',
    'ᶍ': 'x',
    'ý': 'y',
    'ŷ': 'y',
    'ÿ': 'y',
    'ẏ': 'y',
    'ỵ': 'y',
    'ỳ': 'y',
    'ƴ': 'y',
    'ỷ': 'y',
    'ỿ': 'y',
    'ȳ': 'y',
    'ẙ': 'y',
    'ɏ': 'y',
    'ỹ': 'y',
    'ź': 'z',
    'ž': 'z',
    'ẑ': 'z',
    'ʑ': 'z',
    'ⱬ': 'z',
    'ż': 'z',
    'ẓ': 'z',
    'ȥ': 'z',
    'ẕ': 'z',
    'ᵶ': 'z',
    'ᶎ': 'z',
    'ʐ': 'z',
    'ƶ': 'z',
    'ɀ': 'z',
    'ﬀ': 'ff',
    'ﬃ': 'ffi',
    'ﬄ': 'ffl',
    'ﬁ': 'fi',
    'ﬂ': 'fl',
    'ĳ': 'ij',
    'œ': 'oe',
    'ﬆ': 'st',
    'ₐ': 'a',
    'ₑ': 'e',
    'ᵢ': 'i',
    'ⱼ': 'j',
    'ₒ': 'o',
    'ᵣ': 'r',
    'ᵤ': 'u',
    'ᵥ': 'v',
    'ₓ': 'x'
};


/***/ }),
/* 117 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TypeaheadDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__typeahead_container_component__ = __webpack_require__(56);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__typeahead_utils__ = __webpack_require__(57);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_Observable__ = __webpack_require__(110);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_Observable___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_rxjs_Observable__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_add_observable_from__ = __webpack_require__(150);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_add_observable_from___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_rxjs_add_observable_from__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_debounceTime__ = __webpack_require__(151);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_debounceTime___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_rxjs_add_operator_debounceTime__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7_rxjs_add_operator_filter__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7_rxjs_add_operator_filter___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_7_rxjs_add_operator_filter__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_rxjs_add_operator_map__ = __webpack_require__(118);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_rxjs_add_operator_map___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_8_rxjs_add_operator_map__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_rxjs_add_operator_mergeMap__ = __webpack_require__(152);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_rxjs_add_operator_mergeMap___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_9_rxjs_add_operator_mergeMap__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10_rxjs_add_operator_toArray__ = __webpack_require__(153);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10_rxjs_add_operator_toArray___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_10_rxjs_add_operator_toArray__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__typeahead_match_class__ = __webpack_require__(119);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__component_loader__ = __webpack_require__(6);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};













var TypeaheadDirective = (function () {
    function TypeaheadDirective(control, viewContainerRef, element, renderer, cis) {
        /** minimal no of characters that needs to be entered before typeahead kicks-in. When set to 0, typeahead shows on focus with full list of options (limited as normal by typeaheadOptionsLimit) */
        this.typeaheadMinLength = void 0;
        /** should be used only in case of typeahead attribute is array. If true - loading of options will be async, otherwise - sync. true make sense if options array is large. */
        this.typeaheadAsync = void 0;
        /** match latin symbols. If true the word súper would match super and vice versa. */
        this.typeaheadLatinize = true;
        /** break words with spaces. If true the text "exact phrase" here match would match with match exact phrase here but not with phrase here exact match (kind of "google style"). */
        this.typeaheadSingleWords = true;
        /** should be used only in case typeaheadSingleWords attribute is true. Sets the word delimiter to break words. Defaults to space. */
        this.typeaheadWordDelimiters = ' ';
        /** should be used only in case typeaheadSingleWords attribute is true. Sets the word delimiter to match exact phrase. Defaults to simple and double quotes. */
        this.typeaheadPhraseDelimiters = '\'"';
        /** fired when 'busy' state of this component was changed, fired on async mode only, returns boolean */
        this.typeaheadLoading = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired on every key event and returns true in case of matches are not detected */
        this.typeaheadNoResults = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired when option was selected, return object with data of this option */
        this.typeaheadOnSelect = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** fired when blur event occurres. returns the active item */
        this.typeaheadOnBlur = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        /** This attribute indicates that the dropdown should be opened upwards */
        this.dropup = false;
        this.isTypeaheadOptionsListActive = false;
        this.keyUpEventEmitter = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.placement = 'bottom-left';
        this._subscriptions = [];
        this.element = element;
        this.ngControl = control;
        this.viewContainerRef = viewContainerRef;
        this.renderer = renderer;
        this._typeahead = cis
            .createLoader(element, viewContainerRef, renderer);
    }
    TypeaheadDirective.prototype.onChange = function (e) {
        if (this._container) {
            // esc
            if (e.keyCode === 27) {
                this.hide();
                return;
            }
            // up
            if (e.keyCode === 38) {
                this._container.prevActiveMatch();
                return;
            }
            // down
            if (e.keyCode === 40) {
                this._container.nextActiveMatch();
                return;
            }
            // enter
            if (e.keyCode === 13) {
                this._container.selectActiveMatch();
                return;
            }
        }
        // For `<input>`s, use the `value` property. For others that don't have a
        // `value` (such as `<span contenteditable="true">`), use either
        // `textContent` or `innerText` (depending on which one is supported, i.e.
        // Firefox or IE).
        var value = e.target.value !== undefined
            ? e.target.value
            : e.target.textContent !== undefined
                ? e.target.textContent
                : e.target.innerText;
        if (value != null && value.trim().length >= this.typeaheadMinLength) {
            this.typeaheadLoading.emit(true);
            this.keyUpEventEmitter.emit(e.target.value);
        }
        else {
            this.typeaheadLoading.emit(false);
            this.typeaheadNoResults.emit(false);
            this.hide();
        }
    };
    TypeaheadDirective.prototype.onFocus = function () {
        if (this.typeaheadMinLength === 0) {
            this.typeaheadLoading.emit(true);
            this.keyUpEventEmitter.emit('');
        }
    };
    TypeaheadDirective.prototype.onBlur = function () {
        if (this._container && !this._container.isFocused) {
            this.typeaheadOnBlur.emit(this._container.active);
            this.hide();
        }
    };
    TypeaheadDirective.prototype.onKeydown = function (e) {
        // no container - no problems
        if (!this._container) {
            return;
        }
        // if items is visible - prevent form submition
        if (e.keyCode === 13) {
            e.preventDefault();
            return;
        }
    };
    TypeaheadDirective.prototype.ngOnInit = function () {
        this.typeaheadOptionsLimit = this.typeaheadOptionsLimit || 20;
        this.typeaheadMinLength = this.typeaheadMinLength === void 0
            ? 1
            : this.typeaheadMinLength;
        this.typeaheadWaitMs = this.typeaheadWaitMs || 0;
        // async should be false in case of array
        if (this.typeaheadAsync === undefined && !(this.typeahead instanceof __WEBPACK_IMPORTED_MODULE_4_rxjs_Observable__["Observable"])) {
            this.typeaheadAsync = false;
        }
        if (this.typeahead instanceof __WEBPACK_IMPORTED_MODULE_4_rxjs_Observable__["Observable"]) {
            this.typeaheadAsync = true;
        }
        if (this.typeaheadAsync) {
            this.asyncActions();
        }
        else {
            this.syncActions();
        }
    };
    TypeaheadDirective.prototype.changeModel = function (match) {
        var valueStr = match.value;
        this.ngControl.viewToModelUpdate(valueStr);
        this.ngControl.control.setValue(valueStr);
        this.hide();
    };
    Object.defineProperty(TypeaheadDirective.prototype, "matches", {
        get: function () {
            return this._matches;
        },
        enumerable: true,
        configurable: true
    });
    TypeaheadDirective.prototype.show = function () {
        this._typeahead
            .attach(__WEBPACK_IMPORTED_MODULE_2__typeahead_container_component__["a" /* TypeaheadContainerComponent */])
            .to(this.container)
            .position({ attachment: (this.dropup ? 'top' : 'bottom') + " left" })
            .show({
            typeaheadRef: this,
            placement: this.placement,
            animation: false,
            dropup: this.dropup
        });
        this._container = this._typeahead.instance;
        this._container.parent = this;
        // This improves the speed as it won't have to be done for each list item
        var normalizedQuery = (this.typeaheadLatinize
            ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["b" /* latinize */])(this.ngControl.control.value)
            : this.ngControl.control.value).toString()
            .toLowerCase();
        this._container.query = this.typeaheadSingleWords
            ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["c" /* tokenize */])(normalizedQuery, this.typeaheadWordDelimiters, this.typeaheadPhraseDelimiters)
            : normalizedQuery;
        this._container.matches = this._matches;
        this.element.nativeElement.focus();
    };
    TypeaheadDirective.prototype.hide = function () {
        if (this._typeahead.isShown) {
            this._typeahead.hide();
            this._container = null;
        }
    };
    TypeaheadDirective.prototype.ngOnDestroy = function () {
        // clean up subscriptions
        for (var _i = 0, _a = this._subscriptions; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
        this._typeahead.dispose();
    };
    TypeaheadDirective.prototype.asyncActions = function () {
        var _this = this;
        this._subscriptions.push(this.keyUpEventEmitter
            .debounceTime(this.typeaheadWaitMs)
            .mergeMap(function () { return _this.typeahead; })
            .subscribe(function (matches) {
            _this.finalizeAsyncCall(matches);
        }, function (err) {
            console.error(err);
        }));
    };
    TypeaheadDirective.prototype.syncActions = function () {
        var _this = this;
        this._subscriptions.push(this.keyUpEventEmitter
            .debounceTime(this.typeaheadWaitMs)
            .mergeMap(function (value) {
            var normalizedQuery = _this.normalizeQuery(value);
            return __WEBPACK_IMPORTED_MODULE_4_rxjs_Observable__["Observable"].from(_this.typeahead)
                .filter(function (option) {
                return option && _this.testMatch(_this.normalizeOption(option), normalizedQuery);
            })
                .toArray();
        })
            .subscribe(function (matches) {
            _this.finalizeAsyncCall(matches);
        }, function (err) {
            console.error(err);
        }));
    };
    TypeaheadDirective.prototype.normalizeOption = function (option) {
        var optionValue = Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["a" /* getValueFromObject */])(option, this.typeaheadOptionField);
        var normalizedOption = this.typeaheadLatinize
            ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["b" /* latinize */])(optionValue)
            : optionValue;
        return normalizedOption.toLowerCase();
    };
    TypeaheadDirective.prototype.normalizeQuery = function (value) {
        // If singleWords, break model here to not be doing extra work on each
        // iteration
        var normalizedQuery = (this.typeaheadLatinize ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["b" /* latinize */])(value) : value)
            .toString()
            .toLowerCase();
        normalizedQuery = this.typeaheadSingleWords
            ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["c" /* tokenize */])(normalizedQuery, this.typeaheadWordDelimiters, this.typeaheadPhraseDelimiters)
            : normalizedQuery;
        return normalizedQuery;
    };
    TypeaheadDirective.prototype.testMatch = function (match, test) {
        var spaceLength;
        if (typeof test === 'object') {
            spaceLength = test.length;
            for (var i = 0; i < spaceLength; i += 1) {
                if (test[i].length > 0 && match.indexOf(test[i]) < 0) {
                    return false;
                }
            }
            return true;
        }
        else {
            return match.indexOf(test) >= 0;
        }
    };
    TypeaheadDirective.prototype.finalizeAsyncCall = function (matches) {
        this.prepareMatches(matches);
        this.typeaheadLoading.emit(false);
        this.typeaheadNoResults.emit(!this.hasMatches());
        if (!this.hasMatches()) {
            this.hide();
            return;
        }
        if (this._container) {
            // This improves the speed as it won't have to be done for each list item
            var normalizedQuery = (this.typeaheadLatinize
                ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["b" /* latinize */])(this.ngControl.control.value)
                : this.ngControl.control.value).toString()
                .toLowerCase();
            this._container.query = this.typeaheadSingleWords
                ? Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["c" /* tokenize */])(normalizedQuery, this.typeaheadWordDelimiters, this.typeaheadPhraseDelimiters)
                : normalizedQuery;
            this._container.matches = this._matches;
        }
        else {
            this.show();
        }
    };
    TypeaheadDirective.prototype.prepareMatches = function (options) {
        var _this = this;
        var limited = options.slice(0, this.typeaheadOptionsLimit);
        if (this.typeaheadGroupField) {
            var matches_1 = [];
            // extract all group names
            var groups = limited
                .map(function (option) { return Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["a" /* getValueFromObject */])(option, _this.typeaheadGroupField); })
                .filter(function (v, i, a) { return a.indexOf(v) === i; });
            groups.forEach(function (group) {
                // add group header to array of matches
                matches_1.push(new __WEBPACK_IMPORTED_MODULE_11__typeahead_match_class__["a" /* TypeaheadMatch */](group, group, true));
                // add each item of group to array of matches
                matches_1 = matches_1.concat(limited
                    .filter(function (option) { return Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["a" /* getValueFromObject */])(option, _this.typeaheadGroupField) === group; })
                    .map(function (option) { return new __WEBPACK_IMPORTED_MODULE_11__typeahead_match_class__["a" /* TypeaheadMatch */](option, Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["a" /* getValueFromObject */])(option, _this.typeaheadOptionField)); }));
            });
            this._matches = matches_1;
        }
        else {
            this._matches = limited.map(function (option) { return new __WEBPACK_IMPORTED_MODULE_11__typeahead_match_class__["a" /* TypeaheadMatch */](option, Object(__WEBPACK_IMPORTED_MODULE_3__typeahead_utils__["a" /* getValueFromObject */])(option, _this.typeaheadOptionField)); });
        }
    };
    TypeaheadDirective.prototype.hasMatches = function () {
        return this._matches.length > 0;
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], TypeaheadDirective.prototype, "typeahead", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], TypeaheadDirective.prototype, "typeaheadMinLength", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], TypeaheadDirective.prototype, "typeaheadWaitMs", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Number)
    ], TypeaheadDirective.prototype, "typeaheadOptionsLimit", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TypeaheadDirective.prototype, "typeaheadOptionField", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TypeaheadDirective.prototype, "typeaheadGroupField", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TypeaheadDirective.prototype, "typeaheadAsync", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TypeaheadDirective.prototype, "typeaheadLatinize", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TypeaheadDirective.prototype, "typeaheadSingleWords", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TypeaheadDirective.prototype, "typeaheadWordDelimiters", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TypeaheadDirective.prototype, "typeaheadPhraseDelimiters", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"])
    ], TypeaheadDirective.prototype, "typeaheadItemTemplate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["TemplateRef"])
    ], TypeaheadDirective.prototype, "optionsListTemplate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TypeaheadDirective.prototype, "typeaheadLoading", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TypeaheadDirective.prototype, "typeaheadNoResults", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TypeaheadDirective.prototype, "typeaheadOnSelect", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], TypeaheadDirective.prototype, "typeaheadOnBlur", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], TypeaheadDirective.prototype, "container", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], TypeaheadDirective.prototype, "dropup", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('keyup', ['$event']),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [Object]),
        __metadata("design:returntype", void 0)
    ], TypeaheadDirective.prototype, "onChange", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('focus'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], TypeaheadDirective.prototype, "onFocus", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('blur'),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", void 0)
    ], TypeaheadDirective.prototype, "onBlur", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["HostListener"])('keydown', ['$event']),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [Object]),
        __metadata("design:returntype", void 0)
    ], TypeaheadDirective.prototype, "onKeydown", null);
    TypeaheadDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: '[typeahead]', exportAs: 'bs-typeahead' }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1__angular_forms__["NgControl"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"], __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"], __WEBPACK_IMPORTED_MODULE_12__component_loader__["b" /* ComponentLoaderFactory */]])
    ], TypeaheadDirective);
    return TypeaheadDirective;
}());



/***/ }),
/* 118 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_118__;

/***/ }),
/* 119 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TypeaheadMatch; });
var TypeaheadMatch = (function () {
    function TypeaheadMatch(item, value, header) {
        if (value === void 0) { value = item; }
        if (header === void 0) { header = false; }
        this.item = item;
        this.value = value;
        this.header = header;
    }
    TypeaheadMatch.prototype.isHeader = function () {
        return this.header;
    };
    TypeaheadMatch.prototype.toString = function () {
        return this.value;
    };
    return TypeaheadMatch;
}());



/***/ }),
/* 120 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PopoverModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__positioning__ = __webpack_require__(9);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__popover_config__ = __webpack_require__(27);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__popover_directive__ = __webpack_require__(121);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__popover_container_component__ = __webpack_require__(58);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var PopoverModule = (function () {
    function PopoverModule() {
    }
    PopoverModule_1 = PopoverModule;
    PopoverModule.forRoot = function () {
        return {
            ngModule: PopoverModule_1,
            providers: [__WEBPACK_IMPORTED_MODULE_4__popover_config__["a" /* PopoverConfig */], __WEBPACK_IMPORTED_MODULE_2__component_loader__["b" /* ComponentLoaderFactory */], __WEBPACK_IMPORTED_MODULE_3__positioning__["b" /* PositioningService */]]
        };
    };
    PopoverModule = PopoverModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_1__angular_common__["CommonModule"]],
            declarations: [__WEBPACK_IMPORTED_MODULE_5__popover_directive__["a" /* PopoverDirective */], __WEBPACK_IMPORTED_MODULE_6__popover_container_component__["a" /* PopoverContainerComponent */]],
            exports: [__WEBPACK_IMPORTED_MODULE_5__popover_directive__["a" /* PopoverDirective */]],
            entryComponents: [__WEBPACK_IMPORTED_MODULE_6__popover_container_component__["a" /* PopoverContainerComponent */]]
        })
    ], PopoverModule);
    return PopoverModule;
    var PopoverModule_1;
}());



/***/ }),
/* 121 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PopoverDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__popover_config__ = __webpack_require__(27);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__component_loader__ = __webpack_require__(6);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__popover_container_component__ = __webpack_require__(58);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};




/**
 * A lightweight, extensible directive for fancy popover creation.
 */
var PopoverDirective = (function () {
    function PopoverDirective(_elementRef, _renderer, _viewContainerRef, _config, cis) {
        /**
         * Close popover on outside click
         */
        this.outsideClick = false;
        /**
         * Css class for popover container
         */
        this.containerClass = '';
        this._isInited = false;
        this._popover = cis
            .createLoader(_elementRef, _viewContainerRef, _renderer)
            .provide({ provide: __WEBPACK_IMPORTED_MODULE_1__popover_config__["a" /* PopoverConfig */], useValue: _config });
        Object.assign(this, _config);
        this.onShown = this._popover.onShown;
        this.onHidden = this._popover.onHidden;
        // fix: no focus on button on Mac OS #1795
        _elementRef.nativeElement.addEventListener('click', function () {
            try {
                _elementRef.nativeElement.focus();
            }
            catch (err) {
                return;
            }
        });
    }
    Object.defineProperty(PopoverDirective.prototype, "isOpen", {
        /**
         * Returns whether or not the popover is currently being shown
         */
        get: function () { return this._popover.isShown; },
        set: function (value) {
            if (value) {
                this.show();
            }
            else {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    /**
     * Opens an element’s popover. This is considered a “manual” triggering of
     * the popover.
     */
    PopoverDirective.prototype.show = function () {
        if (this._popover.isShown) {
            return;
        }
        this._popover
            .attach(__WEBPACK_IMPORTED_MODULE_3__popover_container_component__["a" /* PopoverContainerComponent */])
            .to(this.container)
            .position({ attachment: this.placement })
            .show({
            content: this.popover,
            context: this.popoverContext,
            placement: this.placement,
            title: this.popoverTitle,
            containerClass: this.containerClass
        });
        this.isOpen = true;
    };
    /**
     * Closes an element’s popover. This is considered a “manual” triggering of
     * the popover.
     */
    PopoverDirective.prototype.hide = function () {
        if (this.isOpen) {
            this._popover.hide();
            this.isOpen = false;
        }
    };
    /**
     * Toggles an element’s popover. This is considered a “manual” triggering of
     * the popover.
     */
    PopoverDirective.prototype.toggle = function () {
        if (this.isOpen) {
            return this.hide();
        }
        this.show();
    };
    PopoverDirective.prototype.ngOnInit = function () {
        var _this = this;
        // fix: seems there are an issue with `routerLinkActive`
        // which result in duplicated call ngOnInit without call to ngOnDestroy
        // read more: https://github.com/valor-software/ngx-bootstrap/issues/1885
        if (this._isInited) {
            return;
        }
        this._isInited = true;
        this._popover.listen({
            triggers: this.triggers,
            outsideClick: this.outsideClick,
            show: function () { return _this.show(); }
        });
    };
    PopoverDirective.prototype.ngOnDestroy = function () {
        this._popover.dispose();
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], PopoverDirective.prototype, "popover", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], PopoverDirective.prototype, "popoverContext", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverDirective.prototype, "popoverTitle", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverDirective.prototype, "placement", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], PopoverDirective.prototype, "outsideClick", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverDirective.prototype, "triggers", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverDirective.prototype, "container", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], PopoverDirective.prototype, "containerClass", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean),
        __metadata("design:paramtypes", [Boolean])
    ], PopoverDirective.prototype, "isOpen", null);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], PopoverDirective.prototype, "onShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"])
    ], PopoverDirective.prototype, "onHidden", void 0);
    PopoverDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({ selector: '[popover]', exportAs: 'bs-popover' }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewContainerRef"],
            __WEBPACK_IMPORTED_MODULE_1__popover_config__["a" /* PopoverConfig */],
            __WEBPACK_IMPORTED_MODULE_2__component_loader__["b" /* ComponentLoaderFactory */]])
    ], PopoverDirective);
    return PopoverDirective;
}());



/***/ }),
/* 122 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export BsDatepickerState */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return initialDatepickerState; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__defaults__ = __webpack_require__(161);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_datepicker_config__ = __webpack_require__(8);


var BsDatepickerState = (function () {
    function BsDatepickerState() {
    }
    return BsDatepickerState;
}());

var initialDatepickerState = Object.assign(new __WEBPACK_IMPORTED_MODULE_1__bs_datepicker_config__["a" /* BsDatepickerConfig */](), {
    view: { date: new Date(), mode: 'day' },
    selectedRange: [],
    monthViewOptions: __WEBPACK_IMPORTED_MODULE_0__defaults__["a" /* defaultMonthOptions */]
});


/***/ }),
/* 123 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = isAfter;
/* harmony export (immutable) */ __webpack_exports__["b"] = isBefore;
/* unused harmony export isBetween */
/* unused harmony export isSame */
/* harmony export (immutable) */ __webpack_exports__["c"] = isSameOrAfter;
/* harmony export (immutable) */ __webpack_exports__["d"] = isSameOrBefore;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__start_end_of__ = __webpack_require__(17);

function isAfter(date1, date2, units) {
    if (units === void 0) { units = 'milliseconds'; }
    if (!date1 || !date2) {
        return false;
    }
    if (units === 'milliseconds') {
        return date1.valueOf() > date2.valueOf();
    }
    return date2.valueOf() < Object(__WEBPACK_IMPORTED_MODULE_0__start_end_of__["b" /* startOf */])(date1, units).valueOf();
}
function isBefore(date1, date2, units) {
    if (units === void 0) { units = 'milliseconds'; }
    if (!date1 || !date2) {
        return false;
    }
    if (units === 'milliseconds') {
        return date1.valueOf() < date2.valueOf();
    }
    return Object(__WEBPACK_IMPORTED_MODULE_0__start_end_of__["a" /* endOf */])(date1, units).valueOf() < date2.valueOf();
}
function isBetween(date, from, to, units, inclusivity) {
    if (inclusivity === void 0) { inclusivity = '()'; }
    var leftBound = inclusivity[0] === '('
        ? isAfter(date, from, units) :
        !isBefore(date, from, units);
    var rightBound = inclusivity[1] === ')'
        ? isBefore(date, to, units)
        : !isAfter(date, to, units);
    return leftBound && rightBound;
}
function isSame(date1, date2, units) {
    if (units === void 0) { units = 'milliseconds'; }
    if (!date1 || !date2) {
        return false;
    }
    if (units === 'milliseconds') {
        return date1.valueOf() === date2.valueOf();
    }
    var inputMs = date2.valueOf();
    return Object(__WEBPACK_IMPORTED_MODULE_0__start_end_of__["b" /* startOf */])(date1, units).valueOf() <= inputMs
        && inputMs <= Object(__WEBPACK_IMPORTED_MODULE_0__start_end_of__["a" /* endOf */])(date1, units).valueOf();
}
function isSameOrAfter(date1, date2, units) {
    return isSame(date1, date2, units) || isAfter(date1, date2, units);
}
function isSameOrBefore(date1, date2, units) {
    return isSame(date1, date2, units) || isBefore(date1, date2, units);
}


/***/ }),
/* 124 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return yearsPerCalendar; });
/* harmony export (immutable) */ __webpack_exports__["a"] = formatYearsCalendar;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_setters__ = __webpack_require__(7);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_moment_format__ = __webpack_require__(10);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_matrix_utils__ = __webpack_require__(60);



var height = 4;
var width = 4;
var yearsPerCalendar = height * width;
var initialShift = (Math.floor(yearsPerCalendar / 2) - 1) * -1;
var shift = { year: 1 };
function formatYearsCalendar(viewDate, formatOptions) {
    var initialDate = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_setters__["c" /* shiftDate */])(viewDate, { year: initialShift });
    var matrixOptions = { width: width, height: height, initialDate: initialDate, shift: shift };
    var yearsMatrix = Object(__WEBPACK_IMPORTED_MODULE_2__utils_matrix_utils__["a" /* createMatrix */])(matrixOptions, function (date) { return ({
        date: date,
        label: Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_format__["a" /* formatDate */])(date, formatOptions.yearLabel, formatOptions.locale)
    }); });
    var yearTitle = formatYearRangeTitle(yearsMatrix, formatOptions);
    return {
        years: yearsMatrix,
        monthTitle: '',
        yearTitle: yearTitle
    };
}
function formatYearRangeTitle(yearsMatrix, formatOptions) {
    var from = Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_format__["a" /* formatDate */])(yearsMatrix[0][0].date, formatOptions.yearTitle, formatOptions.locale);
    var to = Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_format__["a" /* formatDate */])(yearsMatrix[height - 1][width - 1].date, formatOptions.yearTitle, formatOptions.locale);
    return from + " - " + to;
}


/***/ }),
/* 125 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__base_bs_datepicker_container__ = __webpack_require__(126);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_datepicker_config__ = __webpack_require__(8);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__reducer_bs_datepicker_actions__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__reducer_bs_datepicker_effects__ = __webpack_require__(61);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__reducer_bs_datepicker_store__ = __webpack_require__(59);
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};






var BsDatepickerContainerComponent = (function (_super) {
    __extends(BsDatepickerContainerComponent, _super);
    function BsDatepickerContainerComponent(_config, _store, _actions, _effects) {
        var _this = _super.call(this) || this;
        _this._config = _config;
        _this._store = _store;
        _this._actions = _actions;
        _this.valueChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        _this._subs = [];
        _this._effects = _effects;
        return _this;
    }
    Object.defineProperty(BsDatepickerContainerComponent.prototype, "value", {
        set: function (value) {
            this._effects.setValue(value);
        },
        enumerable: true,
        configurable: true
    });
    BsDatepickerContainerComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.containerClass = this._config.containerClass;
        this._effects
            .init(this._store)
            .setOptions(this._config)
            .setBindings(this)
            .setEventHandlers(this)
            .registerDatepickerSideEffects();
        // todo: move it somewhere else
        // on selected date change
        this._subs.push(this._store
            .select(function (state) { return state.selectedDate; })
            .subscribe(function (date) { return _this.valueChange.emit(date); }));
    };
    BsDatepickerContainerComponent.prototype.daySelectHandler = function (day) {
        if (day.isOtherMonth || day.isDisabled) {
            return;
        }
        this._store.dispatch(this._actions.select(day.date));
    };
    BsDatepickerContainerComponent.prototype.ngOnDestroy = function () {
        for (var _i = 0, _a = this._subs; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
        this._effects.destroy();
    };
    BsDatepickerContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-datepicker-container',
            providers: [__WEBPACK_IMPORTED_MODULE_5__reducer_bs_datepicker_store__["a" /* BsDatepickerStore */], __WEBPACK_IMPORTED_MODULE_4__reducer_bs_datepicker_effects__["a" /* BsDatepickerEffects */]],
            templateUrl: './bs-datepicker-view.html',
            host: {
                '(click)': '_stopPropagation($event)',
                style: 'position: absolute; display: block;'
            }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__bs_datepicker_config__["a" /* BsDatepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_5__reducer_bs_datepicker_store__["a" /* BsDatepickerStore */],
            __WEBPACK_IMPORTED_MODULE_3__reducer_bs_datepicker_actions__["a" /* BsDatepickerActions */],
            __WEBPACK_IMPORTED_MODULE_4__reducer_bs_datepicker_effects__["a" /* BsDatepickerEffects */]])
    ], BsDatepickerContainerComponent);
    return BsDatepickerContainerComponent;
}(__WEBPACK_IMPORTED_MODULE_1__base_bs_datepicker_container__["a" /* BsDatepickerAbstractComponent */]));



/***/ }),
/* 126 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerAbstractComponent; });
var BsDatepickerAbstractComponent = (function () {
    function BsDatepickerAbstractComponent() {
        this._customRangesFish = [];
    }
    Object.defineProperty(BsDatepickerAbstractComponent.prototype, "minDate", {
        set: function (value) {
            this._effects.setMinDate(value);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDatepickerAbstractComponent.prototype, "maxDate", {
        set: function (value) {
            this._effects.setMaxDate(value);
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDatepickerAbstractComponent.prototype, "isDisabled", {
        set: function (value) {
            this._effects.setDisabled(value);
        },
        enumerable: true,
        configurable: true
    });
    BsDatepickerAbstractComponent.prototype.setViewMode = function (event) { };
    BsDatepickerAbstractComponent.prototype.navigateTo = function (event) { };
    BsDatepickerAbstractComponent.prototype.dayHoverHandler = function (event) { };
    BsDatepickerAbstractComponent.prototype.monthHoverHandler = function (event) { };
    BsDatepickerAbstractComponent.prototype.yearHoverHandler = function (event) { };
    BsDatepickerAbstractComponent.prototype.daySelectHandler = function (day) { };
    ;
    BsDatepickerAbstractComponent.prototype.monthSelectHandler = function (event) { };
    BsDatepickerAbstractComponent.prototype.yearSelectHandler = function (event) { };
    BsDatepickerAbstractComponent.prototype._stopPropagation = function (event) {
        event.stopPropagation();
    };
    return BsDatepickerAbstractComponent;
}());



/***/ }),
/* 127 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDaterangepickerContainerComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__base_bs_datepicker_container__ = __webpack_require__(126);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_datepicker_config__ = __webpack_require__(8);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__reducer_bs_datepicker_actions__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__reducer_bs_datepicker_effects__ = __webpack_require__(61);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__reducer_bs_datepicker_store__ = __webpack_require__(59);
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};






var BsDaterangepickerContainerComponent = (function (_super) {
    __extends(BsDaterangepickerContainerComponent, _super);
    function BsDaterangepickerContainerComponent(_config, _store, _actions, _effects) {
        var _this = _super.call(this) || this;
        _this._config = _config;
        _this._store = _store;
        _this._actions = _actions;
        _this.valueChange = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        _this._rangeStack = [];
        _this._subs = [];
        _this._effects = _effects;
        return _this;
    }
    Object.defineProperty(BsDaterangepickerContainerComponent.prototype, "value", {
        set: function (value) {
            this._effects.setRangeValue(value);
        },
        enumerable: true,
        configurable: true
    });
    BsDaterangepickerContainerComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.containerClass = this._config.containerClass;
        this._effects
            .init(this._store)
            .setOptions(this._config)
            .setBindings(this)
            .setEventHandlers(this)
            .registerDatepickerSideEffects();
        // todo: move it somewhere else
        // on selected date change
        this._subs.push(this._store
            .select(function (state) { return state.selectedRange; })
            .subscribe(function (date) { return _this.valueChange.emit(date); }));
    };
    BsDaterangepickerContainerComponent.prototype.daySelectHandler = function (day) {
        if (day.isOtherMonth || day.isDisabled) {
            return;
        }
        // if only one date is already selected
        // and user clicks on previous date
        // start selection from new date
        // but if new date is after initial one
        // than finish selection
        if (this._rangeStack.length === 1) {
            this._rangeStack = day.date >= this._rangeStack[0]
                ? [this._rangeStack[0], day.date]
                : [day.date];
        }
        if (this._rangeStack.length === 0) {
            this._rangeStack = [day.date];
        }
        this._store.dispatch(this._actions.selectRange(this._rangeStack));
        if (this._rangeStack.length === 2) {
            this._rangeStack = [];
        }
    };
    BsDaterangepickerContainerComponent.prototype.ngOnDestroy = function () {
        for (var _i = 0, _a = this._subs; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
        this._effects.destroy();
    };
    BsDaterangepickerContainerComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-daterangepicker-container',
            providers: [__WEBPACK_IMPORTED_MODULE_5__reducer_bs_datepicker_store__["a" /* BsDatepickerStore */], __WEBPACK_IMPORTED_MODULE_4__reducer_bs_datepicker_effects__["a" /* BsDatepickerEffects */]],
            templateUrl: './bs-datepicker-view.html',
            host: {
                '(click)': '_stopPropagation($event)',
                style: 'position: absolute; display: block;'
            }
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__bs_datepicker_config__["a" /* BsDatepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_5__reducer_bs_datepicker_store__["a" /* BsDatepickerStore */],
            __WEBPACK_IMPORTED_MODULE_3__reducer_bs_datepicker_actions__["a" /* BsDatepickerActions */],
            __WEBPACK_IMPORTED_MODULE_4__reducer_bs_datepicker_effects__["a" /* BsDatepickerEffects */]])
    ], BsDaterangepickerContainerComponent);
    return BsDaterangepickerContainerComponent;
}(__WEBPACK_IMPORTED_MODULE_1__base_bs_datepicker_container__["a" /* BsDatepickerAbstractComponent */]));



/***/ }),
/* 128 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "BsRootModule", function() { return BsRootModule; });
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "Ng2BootstrapModule", function() { return Ng2BootstrapModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__accordion_accordion_module__ = __webpack_require__(64);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__alert_alert_module__ = __webpack_require__(67);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__buttons_buttons_module__ = __webpack_require__(69);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__carousel_carousel_module__ = __webpack_require__(72);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__collapse_collapse_module__ = __webpack_require__(30);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__datepicker_datepicker_module__ = __webpack_require__(75);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__dropdown_bs_dropdown_module__ = __webpack_require__(83);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__modal_modal_module__ = __webpack_require__(91);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__pagination_pagination_module__ = __webpack_require__(93);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__progressbar_progressbar_module__ = __webpack_require__(96);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__rating_rating_module__ = __webpack_require__(99);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__sortable__ = __webpack_require__(101);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__tabs_tabs_module__ = __webpack_require__(103);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__timepicker_timepicker_module__ = __webpack_require__(106);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__tooltip_tooltip_module__ = __webpack_require__(112);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16__typeahead_typeahead_module__ = __webpack_require__(115);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__popover_popover_module__ = __webpack_require__(120);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_18__accordion__ = __webpack_require__(154);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AccordionComponent", function() { return __WEBPACK_IMPORTED_MODULE_18__accordion__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AccordionConfig", function() { return __WEBPACK_IMPORTED_MODULE_18__accordion__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AccordionModule", function() { return __WEBPACK_IMPORTED_MODULE_18__accordion__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AccordionPanelComponent", function() { return __WEBPACK_IMPORTED_MODULE_18__accordion__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_19__alert__ = __webpack_require__(155);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AlertComponent", function() { return __WEBPACK_IMPORTED_MODULE_19__alert__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AlertConfig", function() { return __WEBPACK_IMPORTED_MODULE_19__alert__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "AlertModule", function() { return __WEBPACK_IMPORTED_MODULE_19__alert__["c"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_20__buttons__ = __webpack_require__(156);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ButtonCheckboxDirective", function() { return __WEBPACK_IMPORTED_MODULE_20__buttons__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ButtonRadioDirective", function() { return __WEBPACK_IMPORTED_MODULE_20__buttons__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ButtonsModule", function() { return __WEBPACK_IMPORTED_MODULE_20__buttons__["c"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_21__carousel__ = __webpack_require__(157);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "CarouselComponent", function() { return __WEBPACK_IMPORTED_MODULE_21__carousel__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "CarouselConfig", function() { return __WEBPACK_IMPORTED_MODULE_21__carousel__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "CarouselModule", function() { return __WEBPACK_IMPORTED_MODULE_21__carousel__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "SlideComponent", function() { return __WEBPACK_IMPORTED_MODULE_21__carousel__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_22__collapse__ = __webpack_require__(158);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "CollapseDirective", function() { return __WEBPACK_IMPORTED_MODULE_22__collapse__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "CollapseModule", function() { return __WEBPACK_IMPORTED_MODULE_22__collapse__["b"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_23__datepicker__ = __webpack_require__(159);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "DateFormatter", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "DatePickerComponent", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "DatepickerConfig", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["d"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "DatepickerModule", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "DayPickerComponent", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["f"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "MonthPickerComponent", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["g"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "YearPickerComponent", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["h"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDatepickerModule", function() { return __WEBPACK_IMPORTED_MODULE_23__datepicker__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_24__modal__ = __webpack_require__(181);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ModalDirective", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ModalOptions", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["g"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ModalBackdropOptions", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["d"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ModalBackdropComponent", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ModalModule", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["f"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsModalRef", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsModalService", function() { return __WEBPACK_IMPORTED_MODULE_24__modal__["b"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_25__dropdown__ = __webpack_require__(182);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownModule", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownConfig", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownState", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["f"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownContainerComponent", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownDirective", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownMenuDirective", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["d"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BsDropdownToggleDirective", function() { return __WEBPACK_IMPORTED_MODULE_25__dropdown__["g"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_26__pagination__ = __webpack_require__(183);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PagerComponent", function() { return __WEBPACK_IMPORTED_MODULE_26__pagination__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PaginationComponent", function() { return __WEBPACK_IMPORTED_MODULE_26__pagination__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PaginationConfig", function() { return __WEBPACK_IMPORTED_MODULE_26__pagination__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PaginationModule", function() { return __WEBPACK_IMPORTED_MODULE_26__pagination__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_27__progressbar__ = __webpack_require__(184);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "BarComponent", function() { return __WEBPACK_IMPORTED_MODULE_27__progressbar__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ProgressbarComponent", function() { return __WEBPACK_IMPORTED_MODULE_27__progressbar__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ProgressbarConfig", function() { return __WEBPACK_IMPORTED_MODULE_27__progressbar__["d"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ProgressbarModule", function() { return __WEBPACK_IMPORTED_MODULE_27__progressbar__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ProgressDirective", function() { return __WEBPACK_IMPORTED_MODULE_27__progressbar__["b"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_28__rating__ = __webpack_require__(185);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "RatingComponent", function() { return __WEBPACK_IMPORTED_MODULE_28__rating__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "RatingModule", function() { return __WEBPACK_IMPORTED_MODULE_28__rating__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "DraggableItemService", function() { return __WEBPACK_IMPORTED_MODULE_12__sortable__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "SortableComponent", function() { return __WEBPACK_IMPORTED_MODULE_12__sortable__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "SortableModule", function() { return __WEBPACK_IMPORTED_MODULE_12__sortable__["c"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_29__tabs__ = __webpack_require__(186);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "NgTranscludeDirective", function() { return __WEBPACK_IMPORTED_MODULE_29__tabs__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TabDirective", function() { return __WEBPACK_IMPORTED_MODULE_29__tabs__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TabHeadingDirective", function() { return __WEBPACK_IMPORTED_MODULE_29__tabs__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TabsetComponent", function() { return __WEBPACK_IMPORTED_MODULE_29__tabs__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TabsetConfig", function() { return __WEBPACK_IMPORTED_MODULE_29__tabs__["f"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TabsModule", function() { return __WEBPACK_IMPORTED_MODULE_29__tabs__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_30__timepicker__ = __webpack_require__(187);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TimepickerComponent", function() { return __WEBPACK_IMPORTED_MODULE_30__timepicker__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TimepickerConfig", function() { return __WEBPACK_IMPORTED_MODULE_30__timepicker__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TimepickerModule", function() { return __WEBPACK_IMPORTED_MODULE_30__timepicker__["c"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_31__tooltip__ = __webpack_require__(188);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TooltipConfig", function() { return __WEBPACK_IMPORTED_MODULE_31__tooltip__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TooltipContainerComponent", function() { return __WEBPACK_IMPORTED_MODULE_31__tooltip__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TooltipDirective", function() { return __WEBPACK_IMPORTED_MODULE_31__tooltip__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TooltipModule", function() { return __WEBPACK_IMPORTED_MODULE_31__tooltip__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_32__typeahead__ = __webpack_require__(189);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TypeaheadOptions", function() { return __WEBPACK_IMPORTED_MODULE_32__typeahead__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TypeaheadContainerComponent", function() { return __WEBPACK_IMPORTED_MODULE_32__typeahead__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TypeaheadDirective", function() { return __WEBPACK_IMPORTED_MODULE_32__typeahead__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TypeaheadMatch", function() { return __WEBPACK_IMPORTED_MODULE_32__typeahead__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "TypeaheadModule", function() { return __WEBPACK_IMPORTED_MODULE_32__typeahead__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_33__popover__ = __webpack_require__(191);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PopoverConfig", function() { return __WEBPACK_IMPORTED_MODULE_33__popover__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PopoverContainerComponent", function() { return __WEBPACK_IMPORTED_MODULE_33__popover__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PopoverDirective", function() { return __WEBPACK_IMPORTED_MODULE_33__popover__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PopoverModule", function() { return __WEBPACK_IMPORTED_MODULE_33__popover__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_34__utils__ = __webpack_require__(36);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "OnChange", function() { return __WEBPACK_IMPORTED_MODULE_34__utils__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "LinkedList", function() { return __WEBPACK_IMPORTED_MODULE_34__utils__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "isBs3", function() { return __WEBPACK_IMPORTED_MODULE_34__utils__["e"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "Trigger", function() { return __WEBPACK_IMPORTED_MODULE_34__utils__["c"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "Utils", function() { return __WEBPACK_IMPORTED_MODULE_34__utils__["d"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_35__component_loader__ = __webpack_require__(6);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ComponentLoader", function() { return __WEBPACK_IMPORTED_MODULE_35__component_loader__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ComponentLoaderFactory", function() { return __WEBPACK_IMPORTED_MODULE_35__component_loader__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "ContentRef", function() { return __WEBPACK_IMPORTED_MODULE_35__component_loader__["c"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_36__positioning__ = __webpack_require__(9);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "Positioning", function() { return __WEBPACK_IMPORTED_MODULE_36__positioning__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "PositioningService", function() { return __WEBPACK_IMPORTED_MODULE_36__positioning__["b"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "positionElements", function() { return __WEBPACK_IMPORTED_MODULE_36__positioning__["c"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_37__bs_moment__ = __webpack_require__(192);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "defineLocale", function() { return __WEBPACK_IMPORTED_MODULE_37__bs_moment__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "getSetGlobalLocale", function() { return __WEBPACK_IMPORTED_MODULE_37__bs_moment__["b"]; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
/* tslint:disable: max-classes-per-file */







































var MODULES = [
    __WEBPACK_IMPORTED_MODULE_1__accordion_accordion_module__["a" /* AccordionModule */], __WEBPACK_IMPORTED_MODULE_2__alert_alert_module__["a" /* AlertModule */], __WEBPACK_IMPORTED_MODULE_3__buttons_buttons_module__["a" /* ButtonsModule */],
    __WEBPACK_IMPORTED_MODULE_4__carousel_carousel_module__["a" /* CarouselModule */], __WEBPACK_IMPORTED_MODULE_5__collapse_collapse_module__["a" /* CollapseModule */], __WEBPACK_IMPORTED_MODULE_6__datepicker_datepicker_module__["a" /* DatepickerModule */],
    __WEBPACK_IMPORTED_MODULE_7__dropdown_bs_dropdown_module__["a" /* BsDropdownModule */], __WEBPACK_IMPORTED_MODULE_8__modal_modal_module__["a" /* ModalModule */], __WEBPACK_IMPORTED_MODULE_9__pagination_pagination_module__["a" /* PaginationModule */],
    __WEBPACK_IMPORTED_MODULE_10__progressbar_progressbar_module__["a" /* ProgressbarModule */], __WEBPACK_IMPORTED_MODULE_17__popover_popover_module__["a" /* PopoverModule */], __WEBPACK_IMPORTED_MODULE_11__rating_rating_module__["a" /* RatingModule */],
    __WEBPACK_IMPORTED_MODULE_13__tabs_tabs_module__["a" /* TabsModule */], __WEBPACK_IMPORTED_MODULE_14__timepicker_timepicker_module__["a" /* TimepickerModule */], __WEBPACK_IMPORTED_MODULE_15__tooltip_tooltip_module__["a" /* TooltipModule */],
    __WEBPACK_IMPORTED_MODULE_16__typeahead_typeahead_module__["a" /* TypeaheadModule */]
];
var BsRootModule = (function () {
    function BsRootModule() {
    }
    BsRootModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            imports: [
                __WEBPACK_IMPORTED_MODULE_1__accordion_accordion_module__["a" /* AccordionModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_2__alert_alert_module__["a" /* AlertModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_3__buttons_buttons_module__["a" /* ButtonsModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_4__carousel_carousel_module__["a" /* CarouselModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_5__collapse_collapse_module__["a" /* CollapseModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_6__datepicker_datepicker_module__["a" /* DatepickerModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_7__dropdown_bs_dropdown_module__["a" /* BsDropdownModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_8__modal_modal_module__["a" /* ModalModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_9__pagination_pagination_module__["a" /* PaginationModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_10__progressbar_progressbar_module__["a" /* ProgressbarModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_17__popover_popover_module__["a" /* PopoverModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_11__rating_rating_module__["a" /* RatingModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_13__tabs_tabs_module__["a" /* TabsModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_14__timepicker_timepicker_module__["a" /* TimepickerModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_15__tooltip_tooltip_module__["a" /* TooltipModule */].forRoot(),
                __WEBPACK_IMPORTED_MODULE_16__typeahead_typeahead_module__["a" /* TypeaheadModule */].forRoot(), __WEBPACK_IMPORTED_MODULE_12__sortable__["c" /* SortableModule */].forRoot()
            ],
            exports: MODULES
        })
    ], BsRootModule);
    return BsRootModule;
}());

var Ng2BootstrapModule = (function () {
    function Ng2BootstrapModule() {
    }
    Ng2BootstrapModule.forRoot = function () {
        return { ngModule: BsRootModule };
    };
    Ng2BootstrapModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({ exports: MODULES })
    ], Ng2BootstrapModule);
    return Ng2BootstrapModule;
}());



/***/ }),
/* 129 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return LinkedList; });
var LinkedList = (function () {
    function LinkedList() {
        this.length = 0;
        this.asArray = [];
        // Array methods overriding END
    }
    LinkedList.prototype.getNode = function (position) {
        if (this.length === 0 || position < 0 || position >= this.length) {
            throw new Error('Position is out of the list');
        }
        var current = this.head;
        for (var index = 0; index < position; index++) {
            current = current.next;
        }
        return current;
    };
    LinkedList.prototype.createInternalArrayRepresentation = function () {
        var outArray = [];
        var current = this.head;
        while (current) {
            outArray.push(current.value);
            current = current.next;
        }
        this.asArray = outArray;
    };
    LinkedList.prototype.get = function (position) {
        if (this.length === 0 || position < 0 || position >= this.length) {
            return void 0;
        }
        var current = this.head;
        for (var index = 0; index < position; index++) {
            current = current.next;
        }
        return current.value;
    };
    LinkedList.prototype.add = function (value, position) {
        if (position === void 0) { position = this.length; }
        if (position < 0 || position > this.length) {
            throw new Error('Position is out of the list');
        }
        var node = {
            value: value,
            next: undefined,
            previous: undefined
        };
        if (this.length === 0) {
            this.head = node;
            this.tail = node;
            this.current = node;
        }
        else {
            if (position === 0) {
                // first node
                node.next = this.head;
                this.head.previous = node;
                this.head = node;
            }
            else if (position === this.length) {
                // last node
                this.tail.next = node;
                node.previous = this.tail;
                this.tail = node;
            }
            else {
                // node in middle
                var currentPreviousNode = this.getNode(position - 1);
                var currentNextNode = currentPreviousNode.next;
                currentPreviousNode.next = node;
                currentNextNode.previous = node;
                node.previous = currentPreviousNode;
                node.next = currentNextNode;
            }
        }
        this.length++;
        this.createInternalArrayRepresentation();
    };
    LinkedList.prototype.remove = function (position) {
        if (position === void 0) { position = 0; }
        if (this.length === 0 || position < 0 || position >= this.length) {
            throw new Error('Position is out of the list');
        }
        if (position === 0) {
            // first node
            this.head = this.head.next;
            if (this.head) {
                // there is no second node
                this.head.previous = undefined;
            }
            else {
                // there is no second node
                this.tail = undefined;
            }
        }
        else if (position === this.length - 1) {
            // last node
            this.tail = this.tail.previous;
            this.tail.next = undefined;
        }
        else {
            // middle node
            var removedNode = this.getNode(position);
            removedNode.next.previous = removedNode.previous;
            removedNode.previous.next = removedNode.next;
        }
        this.length--;
        this.createInternalArrayRepresentation();
    };
    LinkedList.prototype.set = function (position, value) {
        if (this.length === 0 || position < 0 || position >= this.length) {
            throw new Error('Position is out of the list');
        }
        var node = this.getNode(position);
        node.value = value;
        this.createInternalArrayRepresentation();
    };
    LinkedList.prototype.toArray = function () {
        return this.asArray;
    };
    LinkedList.prototype.findAll = function (fn) {
        var current = this.head;
        var result = [];
        for (var index = 0; index < this.length; index++) {
            if (fn(current.value, index)) {
                result.push({ index: index, value: current.value });
            }
            current = current.next;
        }
        return result;
    };
    // Array methods overriding start
    LinkedList.prototype.push = function () {
        var _this = this;
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        args.forEach(function (arg) {
            _this.add(arg);
        });
        return this.length;
    };
    LinkedList.prototype.pop = function () {
        if (this.length === 0) {
            return undefined;
        }
        var last = this.tail;
        this.remove(this.length - 1);
        return last.value;
    };
    LinkedList.prototype.unshift = function () {
        var _this = this;
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        args.reverse();
        args.forEach(function (arg) {
            _this.add(arg, 0);
        });
        return this.length;
    };
    LinkedList.prototype.shift = function () {
        if (this.length === 0) {
            return undefined;
        }
        var lastItem = this.head.value;
        this.remove();
        return lastItem;
    };
    LinkedList.prototype.forEach = function (fn) {
        var current = this.head;
        for (var index = 0; index < this.length; index++) {
            fn(current.value, index);
            current = current.next;
        }
    };
    LinkedList.prototype.indexOf = function (value) {
        var current = this.head;
        var position = 0;
        for (var index = 0; index < this.length; index++) {
            if (current.value === value) {
                position = index;
                break;
            }
            current = current.next;
        }
        return position;
    };
    LinkedList.prototype.some = function (fn) {
        var current = this.head;
        var result = false;
        while (current && !result) {
            if (fn(current.value)) {
                result = true;
                break;
            }
            current = current.next;
        }
        return result;
    };
    LinkedList.prototype.every = function (fn) {
        var current = this.head;
        var result = true;
        while (current && result) {
            if (!fn(current.value)) {
                result = false;
            }
            current = current.next;
        }
        return result;
    };
    LinkedList.prototype.toString = function () {
        return '[Linked List]';
    };
    LinkedList.prototype.find = function (fn) {
        var current = this.head;
        var result;
        for (var index = 0; index < this.length; index++) {
            if (fn(current.value, index)) {
                result = current.value;
                break;
            }
            current = current.next;
        }
        return result;
    };
    LinkedList.prototype.findIndex = function (fn) {
        var current = this.head;
        var result;
        for (var index = 0; index < this.length; index++) {
            if (fn(current.value, index)) {
                result = index;
                break;
            }
            current = current.next;
        }
        return result;
    };
    return LinkedList;
}());



/***/ }),
/* 130 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__en__ = __webpack_require__(131);



/***/ }),
/* 131 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__locales_service__ = __webpack_require__(12);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_type_checks__ = __webpack_require__(11);


Object(__WEBPACK_IMPORTED_MODULE_0__locales_service__["c" /* getSetGlobalLocale */])('en', {
    dayOfMonthOrdinalParse: /\d{1,2}(th|st|nd|rd)/,
    ordinal: function (num) {
        var b = num % 10;
        var output = (Object(__WEBPACK_IMPORTED_MODULE_1__utils_type_checks__["g" /* toInt */])(num % 100 / 10) === 1) ? 'th' :
            (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                    (b === 3) ? 'rd' : 'th';
        return num + output;
    }
});


/***/ }),
/* 132 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export defaultInvalidDate */
/* unused harmony export defaultLocaleWeek */
/* unused harmony export defaultLocaleMeridiemParse */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return baseConfig; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__locale_class__ = __webpack_require__(77);

var defaultInvalidDate = 'Invalid date';
var defaultLocaleWeek = {
    dow: 0,
    doy: 6 // The week that contains Jan 1st is the first week of the year.
};
var defaultLocaleMeridiemParse = /[ap]\.?m?\.?/i;
var baseConfig = {
    // calendar: defaultCalendar,
    // longDateFormat: defaultLongDateFormat,
    invalidDate: defaultInvalidDate,
    // ordinal: defaultOrdinal,
    // dayOfMonthOrdinalParse: defaultDayOfMonthOrdinalParse,
    // relativeTime: defaultRelativeTime,
    months: __WEBPACK_IMPORTED_MODULE_0__locale_class__["b" /* defaultLocaleMonths */],
    monthsShort: __WEBPACK_IMPORTED_MODULE_0__locale_class__["c" /* defaultLocaleMonthsShort */],
    week: defaultLocaleWeek,
    weekdays: __WEBPACK_IMPORTED_MODULE_0__locale_class__["d" /* defaultLocaleWeekdays */],
    weekdaysMin: __WEBPACK_IMPORTED_MODULE_0__locale_class__["e" /* defaultLocaleWeekdaysMin */],
    weekdaysShort: __WEBPACK_IMPORTED_MODULE_0__locale_class__["f" /* defaultLocaleWeekdaysShort */],
    meridiemParse: defaultLocaleMeridiemParse
};


/***/ }),
/* 133 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__day_of_month__ = __webpack_require__(134);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__day_of_week__ = __webpack_require__(135);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__day_of_year__ = __webpack_require__(78);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__hour__ = __webpack_require__(136);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__minute__ = __webpack_require__(137);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__month__ = __webpack_require__(138);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__second__ = __webpack_require__(139);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__week__ = __webpack_require__(140);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__week_calendar_utils__ = __webpack_require__(39);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__year__ = __webpack_require__(40);












/***/ }),
/* 134 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_date_getters__ = __webpack_require__(1);


Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('D', ['DD', 2], 'Do', function (date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["a" /* getDate */])(date).toString(10);
});


/***/ }),
/* 135 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export getLocaleDayOfWeek */
/* unused harmony export getISODayOfWeek */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_date_getters__ = __webpack_require__(1);


// FORMATTING
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('d', null, 'do', function (date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["b" /* getDayOfWeek */])(date).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('dd', null, null, function (date, format, locale) {
    return locale.weekdaysShort(date);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('ddd', null, null, function (date, format, locale) {
    return locale.weekdaysMin(date);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('dddd', null, null, function (date, format, locale) {
    return locale.weekdays(date, format);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('e', null, null, function (date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["b" /* getDayOfWeek */])(date).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('E', null, null, function (date) {
    return getISODayOfWeek(date).toString(10);
});
function getLocaleDayOfWeek(date, locale) {
    return (Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["b" /* getDayOfWeek */])(date) + 7 - locale.firstDayOfWeek()) % 7;
}
function getISODayOfWeek(date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["b" /* getDayOfWeek */])(date) || 7;
}


/***/ }),
/* 136 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils__ = __webpack_require__(16);
// import { makeGetSet } from '../moment/get-set';
// import { addFormatToken } from '../format/format';
// import { addUnitAlias } from './aliases';
// import { addUnitPriority } from './priorities';
// import { addRegexToken, match1to2, match2, match3to4, match5to6 } from '../parse/regex';
// import { addParseToken } from '../parse/token';
// import { HOUR, MINUTE, SECOND } from './constants';
// import toInt from '../utils/to-int';
// import zeroFill from '../utils/zero-fill';
// import getParsingFlags from '../create/parsing-flags';
// FORMATTING



function hFormat(date) {
    return Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["e" /* getHours */])(date) % 12 || 12;
}
function kFormat(date) {
    return Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["e" /* getHours */])(date) || 24;
}
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('H', ['HH', 2], null, function (date, format, locale) {
    return Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["e" /* getHours */])(date).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('h', ['hh', 2], null, function (date, format, locale) {
    return hFormat(date).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('k', ['kk', 2], null, function (date, format, locale) {
    return kFormat(date).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('hmm', null, null, function (date, format, locale) {
    return "" + hFormat(date) + Object(__WEBPACK_IMPORTED_MODULE_2__utils__["d" /* zeroFill */])(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["f" /* getMinutes */])(date), 2);
});
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('hmmss', null, null, function (date, format, locale) {
    return "" + hFormat(date) + Object(__WEBPACK_IMPORTED_MODULE_2__utils__["d" /* zeroFill */])(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["f" /* getMinutes */])(date), 2) + Object(__WEBPACK_IMPORTED_MODULE_2__utils__["d" /* zeroFill */])(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["h" /* getSeconds */])(date), 2);
});
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('Hmm', null, null, function (date, format, locale) {
    return "" + Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["e" /* getHours */])(date) + Object(__WEBPACK_IMPORTED_MODULE_2__utils__["d" /* zeroFill */])(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["f" /* getMinutes */])(date), 2);
});
Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])('Hmmss', null, null, function (date, format, locale) {
    return "" + Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["e" /* getHours */])(date) + Object(__WEBPACK_IMPORTED_MODULE_2__utils__["d" /* zeroFill */])(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["f" /* getMinutes */])(date), 2) + Object(__WEBPACK_IMPORTED_MODULE_2__utils__["d" /* zeroFill */])(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["h" /* getSeconds */])(date), 2);
});
function meridiem(token, lowercase) {
    Object(__WEBPACK_IMPORTED_MODULE_1__format_functions__["a" /* addFormatToken */])(token, null, null, function (date, format, locale) {
        return locale.meridiem(Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["e" /* getHours */])(date), Object(__WEBPACK_IMPORTED_MODULE_0__utils_date_getters__["f" /* getMinutes */])(date), lowercase);
    });
}
meridiem('a', true);
meridiem('A', false);


/***/ }),
/* 137 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_date_getters__ = __webpack_require__(1);


Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('m', ['mm', 2], null, function (date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["f" /* getMinutes */])(date).toString(10);
});


/***/ }),
/* 138 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export daysInMonth */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__year__ = __webpack_require__(40);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils__ = __webpack_require__(16);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__utils_date_getters__ = __webpack_require__(1);




function daysInMonth(year, month) {
    if (isNaN(year) || isNaN(month)) {
        return NaN;
    }
    var modMonth = Object(__WEBPACK_IMPORTED_MODULE_2__utils__["c" /* mod */])(month, 12);
    year += (month - modMonth) / 12;
    return modMonth === 1 ? (Object(__WEBPACK_IMPORTED_MODULE_1__year__["b" /* isLeapYear */])(year) ? 29 : 28) : (31 - modMonth % 7 % 2);
}
// FORMATTING
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('M', ['MM', 2], 'Mo', function (date, format) {
    return (Object(__WEBPACK_IMPORTED_MODULE_3__utils_date_getters__["g" /* getMonth */])(date) + 1).toString();
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('MMM', null, null, function (date, format, locale) {
    return locale.monthsShort(date, format);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('MMMM', null, null, function (date, format, locale) {
    return locale.months(date, format);
});


/***/ }),
/* 139 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_date_getters__ = __webpack_require__(1);


Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('s', ['ss', 2], null, function (date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__utils_date_getters__["h" /* getSeconds */])(date).toString(10);
});


/***/ }),
/* 140 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export getWeek */
/* unused harmony export getISOWeek */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__format_functions__ = __webpack_require__(5);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__week_calendar_utils__ = __webpack_require__(39);


Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('w', ['ww', 2], 'wo', function (date, format, locale) {
    return getWeek(date, locale).toString(10);
});
Object(__WEBPACK_IMPORTED_MODULE_0__format_functions__["a" /* addFormatToken */])('W', ['WW', 2], 'Wo', function (date) {
    return getISOWeek(date).toString(10);
});
function getWeek(date, locale) {
    return locale.week(date);
}
function getISOWeek(date) {
    return Object(__WEBPACK_IMPORTED_MODULE_1__week_calendar_utils__["a" /* weekOfYear */])(date, 1, 4).week;
}


/***/ }),
/* 141 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export parseTriggers */
/* unused harmony export listenToTriggers */
/* harmony export (immutable) */ __webpack_exports__["a"] = listenToTriggersV2;
/* harmony export (immutable) */ __webpack_exports__["b"] = registerOutsideClick;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__trigger_class__ = __webpack_require__(73);

var DEFAULT_ALIASES = {
    hover: ['mouseover', 'mouseout'],
    focus: ['focusin', 'focusout']
};
function parseTriggers(triggers, aliases) {
    if (aliases === void 0) { aliases = DEFAULT_ALIASES; }
    var trimmedTriggers = (triggers || '').trim();
    if (trimmedTriggers.length === 0) {
        return [];
    }
    var parsedTriggers = trimmedTriggers.split(/\s+/)
        .map(function (trigger) { return trigger.split(':'); })
        .map(function (triggerPair) {
        var alias = aliases[triggerPair[0]] || triggerPair;
        return new __WEBPACK_IMPORTED_MODULE_0__trigger_class__["a" /* Trigger */](alias[0], alias[1]);
    });
    var manualTriggers = parsedTriggers
        .filter(function (triggerPair) { return triggerPair.isManual(); });
    if (manualTriggers.length > 1) {
        throw 'Triggers parse error: only one manual trigger is allowed';
    }
    if (manualTriggers.length === 1 && parsedTriggers.length > 1) {
        throw 'Triggers parse error: manual trigger can\'t be mixed with other triggers';
    }
    return parsedTriggers;
}
function listenToTriggers(renderer, target, triggers, showFn, hideFn, toggleFn) {
    var parsedTriggers = parseTriggers(triggers);
    var listeners = [];
    if (parsedTriggers.length === 1 && parsedTriggers[0].isManual()) {
        return Function.prototype;
    }
    parsedTriggers.forEach(function (trigger) {
        if (trigger.open === trigger.close) {
            listeners.push(renderer.listen(target, trigger.open, toggleFn));
            return;
        }
        listeners.push(renderer.listen(target, trigger.open, showFn), renderer.listen(target, trigger.close, hideFn));
    });
    return function () { listeners.forEach(function (unsubscribeFn) { return unsubscribeFn(); }); };
}
function listenToTriggersV2(renderer, options) {
    var parsedTriggers = parseTriggers(options.triggers);
    var target = options.target;
    // do nothing
    if (parsedTriggers.length === 1 && parsedTriggers[0].isManual()) {
        return Function.prototype;
    }
    // all listeners
    var listeners = [];
    // lazy listeners registration
    var _registerHide = [];
    var registerHide = function () {
        // add hide listeners to unregister array
        _registerHide.forEach(function (fn) { return listeners.push(fn()); });
        // register hide events only once
        _registerHide.length = 0;
    };
    // register open\close\toggle listeners
    parsedTriggers.forEach(function (trigger) {
        var useToggle = trigger.open === trigger.close;
        var showFn = useToggle ? options.toggle : options.show;
        if (!useToggle) {
            _registerHide.push(function () { return renderer.listen(target, trigger.close, options.hide); });
        }
        listeners.push(renderer.listen(target, trigger.open, function () { return showFn(registerHide); }));
    });
    return function () {
        listeners.forEach(function (unsubscribeFn) { return unsubscribeFn(); });
    };
}
function registerOutsideClick(renderer, options) {
    if (!options.outsideClick) {
        return Function.prototype;
    }
    return renderer.listenGlobal('document', 'click', function (event) {
        if (options.target && options.target.contains(event.target)) {
            return;
        }
        if (options.targets && options.targets.some(function (target) { return target.contains(event.target); })) {
            return;
        }
        options.hide();
    });
}


/***/ }),
/* 142 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return SortableModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__sortable_component__ = __webpack_require__(102);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__draggable_item_service__ = __webpack_require__(48);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};




var SortableModule = (function () {
    function SortableModule() {
    }
    SortableModule_1 = SortableModule;
    SortableModule.forRoot = function () {
        return { ngModule: SortableModule_1, providers: [__WEBPACK_IMPORTED_MODULE_3__draggable_item_service__["a" /* DraggableItemService */]] };
    };
    SortableModule = SortableModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["NgModule"])({
            declarations: [__WEBPACK_IMPORTED_MODULE_2__sortable_component__["a" /* SortableComponent */]],
            imports: [__WEBPACK_IMPORTED_MODULE_1__angular_common__["CommonModule"]],
            exports: [__WEBPACK_IMPORTED_MODULE_2__sortable_component__["a" /* SortableComponent */]]
        })
    ], SortableModule);
    return SortableModule;
    var SortableModule_1;
}());



/***/ }),
/* 143 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_143__;

/***/ }),
/* 144 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* unused harmony export TimepickerState */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return initialState; });
/* harmony export (immutable) */ __webpack_exports__["b"] = timepickerReducer;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__ = __webpack_require__(108);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__timepicker_config__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__timepicker_utils__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__timepicker_actions__ = __webpack_require__(24);




var TimepickerState = (function () {
    function TimepickerState() {
    }
    return TimepickerState;
}());

var initialState = {
    value: null,
    config: new __WEBPACK_IMPORTED_MODULE_1__timepicker_config__["a" /* TimepickerConfig */](),
    controls: {
        canIncrementHours: true,
        canIncrementMinutes: true,
        canIncrementSeconds: true,
        canDecrementHours: true,
        canDecrementMinutes: true,
        canDecrementSeconds: true
    }
};
function timepickerReducer(state, action) {
    if (state === void 0) { state = initialState; }
    switch (action.type) {
        case (__WEBPACK_IMPORTED_MODULE_3__timepicker_actions__["a" /* TimepickerActions */].WRITE_VALUE): {
            return Object.assign({}, state, { value: action.payload });
        }
        case (__WEBPACK_IMPORTED_MODULE_3__timepicker_actions__["a" /* TimepickerActions */].CHANGE_HOURS): {
            if (!Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["d" /* canChangeValue */])(state.config, action.payload) ||
                !Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["a" /* canChangeHours */])(action.payload, state.controls)) {
                return state;
            }
            var _newTime = Object(__WEBPACK_IMPORTED_MODULE_2__timepicker_utils__["a" /* changeTime */])(state.value, { hour: action.payload.step });
            return Object.assign({}, state, { value: _newTime });
        }
        case (__WEBPACK_IMPORTED_MODULE_3__timepicker_actions__["a" /* TimepickerActions */].CHANGE_MINUTES): {
            if (!Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["d" /* canChangeValue */])(state.config, action.payload) ||
                !Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["b" /* canChangeMinutes */])(action.payload, state.controls)) {
                return state;
            }
            var _newTime = Object(__WEBPACK_IMPORTED_MODULE_2__timepicker_utils__["a" /* changeTime */])(state.value, { minute: action.payload.step });
            return Object.assign({}, state, { value: _newTime });
        }
        case (__WEBPACK_IMPORTED_MODULE_3__timepicker_actions__["a" /* TimepickerActions */].CHANGE_SECONDS): {
            if (!Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["d" /* canChangeValue */])(state.config, action.payload) ||
                !Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["c" /* canChangeSeconds */])(action.payload, state.controls)) {
                return state;
            }
            var _newTime = Object(__WEBPACK_IMPORTED_MODULE_2__timepicker_utils__["a" /* changeTime */])(state.value, { seconds: action.payload.step });
            return Object.assign({}, state, { value: _newTime });
        }
        case (__WEBPACK_IMPORTED_MODULE_3__timepicker_actions__["a" /* TimepickerActions */].SET_TIME_UNIT): {
            if (!Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["d" /* canChangeValue */])(state.config)) {
                return state;
            }
            var _newTime = Object(__WEBPACK_IMPORTED_MODULE_2__timepicker_utils__["f" /* setTime */])(state.value, action.payload);
            return Object.assign({}, state, { value: _newTime });
        }
        case (__WEBPACK_IMPORTED_MODULE_3__timepicker_actions__["a" /* TimepickerActions */].UPDATE_CONTROLS): {
            var _newControlsState = Object(__WEBPACK_IMPORTED_MODULE_0__timepicker_controls_util__["f" /* timepickerControls */])(state.value, action.payload);
            var _newState = {
                value: state.value,
                config: action.payload,
                controls: _newControlsState
            };
            if (state.config.showMeridian !== _newState.config.showMeridian) {
                _newState.value = new Date(state.value);
            }
            return Object.assign({}, state, _newState);
        }
        default:
            return state;
    }
}


/***/ }),
/* 145 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_145__;

/***/ }),
/* 146 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_146__;

/***/ }),
/* 147 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_147__;

/***/ }),
/* 148 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_148__;

/***/ }),
/* 149 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_149__;

/***/ }),
/* 150 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_150__;

/***/ }),
/* 151 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_151__;

/***/ }),
/* 152 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_152__;

/***/ }),
/* 153 */
/***/ (function(module, exports) {

module.exports = __WEBPACK_EXTERNAL_MODULE_153__;

/***/ }),
/* 154 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__accordion_group_component__ = __webpack_require__(66);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_0__accordion_group_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__accordion_component__ = __webpack_require__(31);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_1__accordion_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__accordion_module__ = __webpack_require__(64);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_2__accordion_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__accordion_config__ = __webpack_require__(32);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_3__accordion_config__["a"]; });






/***/ }),
/* 155 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__alert_component__ = __webpack_require__(68);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__alert_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__alert_module__ = __webpack_require__(67);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_1__alert_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__alert_config__ = __webpack_require__(33);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_2__alert_config__["a"]; });





/***/ }),
/* 156 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__button_checkbox_directive__ = __webpack_require__(70);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__button_checkbox_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__button_radio_directive__ = __webpack_require__(71);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__button_radio_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__buttons_module__ = __webpack_require__(69);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_2__buttons_module__["a"]; });





/***/ }),
/* 157 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__carousel_component__ = __webpack_require__(35);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__carousel_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__carousel_module__ = __webpack_require__(72);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_1__carousel_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__slide_component__ = __webpack_require__(74);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_2__slide_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__carousel_config__ = __webpack_require__(38);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_3__carousel_config__["a"]; });






/***/ }),
/* 158 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__collapse_directive__ = __webpack_require__(65);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__collapse_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__collapse_module__ = __webpack_require__(30);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__collapse_module__["a"]; });




/***/ }),
/* 159 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__datepicker_component__ = __webpack_require__(79);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_0__datepicker_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__datepicker_module__ = __webpack_require__(75);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_1__datepicker_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__daypicker_component__ = __webpack_require__(80);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return __WEBPACK_IMPORTED_MODULE_2__daypicker_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__monthpicker_component__ = __webpack_require__(81);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "g", function() { return __WEBPACK_IMPORTED_MODULE_3__monthpicker_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__yearpicker_component__ = __webpack_require__(82);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "h", function() { return __WEBPACK_IMPORTED_MODULE_4__yearpicker_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__date_formatter__ = __webpack_require__(76);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_5__date_formatter__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__datepicker_config__ = __webpack_require__(41);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_6__datepicker_config__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__bs_datepicker_module__ = __webpack_require__(160);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_7__bs_datepicker_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__bs_datepicker_component__ = __webpack_require__(63);
/* unused harmony reexport BsDatepickerComponent */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__bs_daterangepicker_component__ = __webpack_require__(62);
/* unused harmony reexport BsDaterangepickerComponent */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__bs_datepicker_config__ = __webpack_require__(8);
/* unused harmony reexport BsDatepickerConfig */













/***/ }),
/* 160 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_common___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_common__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__reducer_bs_datepicker_actions__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__reducer_bs_datepicker_store__ = __webpack_require__(59);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__themes_bs_bs_datepicker_container_component__ = __webpack_require__(125);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__themes_bs_bs_datepicker_navigation_view_component__ = __webpack_require__(170);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__themes_bs_bs_days_calendar_view_component__ = __webpack_require__(171);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__reducer_bs_datepicker_effects__ = __webpack_require__(61);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__themes_bs_bs_daterangepicker_container_component__ = __webpack_require__(127);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__bs_daterangepicker_component__ = __webpack_require__(62);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__bs_datepicker_component__ = __webpack_require__(63);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__component_loader_component_loader_factory__ = __webpack_require__(13);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__positioning_positioning_service__ = __webpack_require__(87);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__themes_bs_bs_datepicker_day_decorator_directive__ = __webpack_require__(172);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__themes_bs_bs_months_calendar_view_component__ = __webpack_require__(173);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__themes_bs_bs_years_calendar_view_component__ = __webpack_require__(174);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16__themes_bs_bs_custom_dates_view_component__ = __webpack_require__(175);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__themes_bs_bs_current_date_view_component__ = __webpack_require__(176);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_18__themes_bs_bs_timepicker_view_component__ = __webpack_require__(177);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_19__bs_datepicker_config__ = __webpack_require__(8);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_20__themes_bs_bs_calendar_layout_component__ = __webpack_require__(178);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_21__bs_datepicker_input_directive__ = __webpack_require__(179);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_22__bs_daterangepicker_input_directive__ = __webpack_require__(180);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_23__utils_warn_once__ = __webpack_require__(114);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
























var _exports = [
    __WEBPACK_IMPORTED_MODULE_4__themes_bs_bs_datepicker_container_component__["a" /* BsDatepickerContainerComponent */],
    __WEBPACK_IMPORTED_MODULE_8__themes_bs_bs_daterangepicker_container_component__["a" /* BsDaterangepickerContainerComponent */],
    __WEBPACK_IMPORTED_MODULE_10__bs_datepicker_component__["a" /* BsDatepickerComponent */],
    __WEBPACK_IMPORTED_MODULE_21__bs_datepicker_input_directive__["a" /* BsDatepickerInputDirective */],
    __WEBPACK_IMPORTED_MODULE_22__bs_daterangepicker_input_directive__["a" /* BsDaterangepickerInputDirective */],
    __WEBPACK_IMPORTED_MODULE_9__bs_daterangepicker_component__["a" /* BsDaterangepickerComponent */]
];
var BsDatepickerModule = (function () {
    function BsDatepickerModule() {
        Object(__WEBPACK_IMPORTED_MODULE_23__utils_warn_once__["a" /* warnOnce */])("BsDatepickerModule is under development,\n      BREAKING CHANGES are possible,\n      PLEASE, read changelog");
    }
    BsDatepickerModule_1 = BsDatepickerModule;
    BsDatepickerModule.forRoot = function () {
        return {
            ngModule: BsDatepickerModule_1,
            providers: [
                __WEBPACK_IMPORTED_MODULE_11__component_loader_component_loader_factory__["a" /* ComponentLoaderFactory */], __WEBPACK_IMPORTED_MODULE_12__positioning_positioning_service__["a" /* PositioningService */],
                __WEBPACK_IMPORTED_MODULE_3__reducer_bs_datepicker_store__["a" /* BsDatepickerStore */], __WEBPACK_IMPORTED_MODULE_2__reducer_bs_datepicker_actions__["a" /* BsDatepickerActions */], __WEBPACK_IMPORTED_MODULE_19__bs_datepicker_config__["a" /* BsDatepickerConfig */], __WEBPACK_IMPORTED_MODULE_7__reducer_bs_datepicker_effects__["a" /* BsDatepickerEffects */]
            ]
        };
    };
    BsDatepickerModule = BsDatepickerModule_1 = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
            imports: [__WEBPACK_IMPORTED_MODULE_0__angular_common__["CommonModule"]],
            declarations: [
                __WEBPACK_IMPORTED_MODULE_13__themes_bs_bs_datepicker_day_decorator_directive__["a" /* BsDatepickerDayDecoratorComponent */],
                __WEBPACK_IMPORTED_MODULE_17__themes_bs_bs_current_date_view_component__["a" /* BsCurrentDateViewComponent */],
                __WEBPACK_IMPORTED_MODULE_5__themes_bs_bs_datepicker_navigation_view_component__["a" /* BsDatepickerNavigationViewComponent */],
                __WEBPACK_IMPORTED_MODULE_18__themes_bs_bs_timepicker_view_component__["a" /* BsTimepickerViewComponent */],
                __WEBPACK_IMPORTED_MODULE_20__themes_bs_bs_calendar_layout_component__["a" /* BsCalendarLayoutComponent */],
                __WEBPACK_IMPORTED_MODULE_6__themes_bs_bs_days_calendar_view_component__["a" /* BsDaysCalendarViewComponent */],
                __WEBPACK_IMPORTED_MODULE_14__themes_bs_bs_months_calendar_view_component__["a" /* BsMonthCalendarViewComponent */],
                __WEBPACK_IMPORTED_MODULE_15__themes_bs_bs_years_calendar_view_component__["a" /* BsYearsCalendarViewComponent */],
                __WEBPACK_IMPORTED_MODULE_16__themes_bs_bs_custom_dates_view_component__["a" /* BsCustomDatesViewComponent */]
            ].concat(_exports),
            entryComponents: [__WEBPACK_IMPORTED_MODULE_4__themes_bs_bs_datepicker_container_component__["a" /* BsDatepickerContainerComponent */], __WEBPACK_IMPORTED_MODULE_8__themes_bs_bs_daterangepicker_container_component__["a" /* BsDaterangepickerContainerComponent */]],
            exports: _exports
        }),
        __metadata("design:paramtypes", [])
    ], BsDatepickerModule);
    return BsDatepickerModule;
    var BsDatepickerModule_1;
}());



/***/ }),
/* 161 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return defaultMonthOptions; });
var defaultMonthOptions = {
    width: 7,
    height: 6
};


/***/ }),
/* 162 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = bsDatepickerReducer;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_datepicker_state__ = __webpack_require__(122);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__engine_calc_days_calendar__ = __webpack_require__(163);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__engine_format_days_calendar__ = __webpack_require__(164);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__engine_flag_days_calendar__ = __webpack_require__(165);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__ = __webpack_require__(7);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__engine_view_mode__ = __webpack_require__(166);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__engine_format_months_calendar__ = __webpack_require__(167);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__engine_flag_months_calendar__ = __webpack_require__(168);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__engine_format_years_calendar__ = __webpack_require__(124);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__engine_flag_years_calendar__ = __webpack_require__(169);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__bs_moment_utils_type_checks__ = __webpack_require__(11);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__bs_moment_utils_start_end_of__ = __webpack_require__(17);













function bsDatepickerReducer(state, action) {
    if (state === void 0) { state = __WEBPACK_IMPORTED_MODULE_0__bs_datepicker_state__["a" /* initialDatepickerState */]; }
    switch (action.type) {
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].CALCULATE): {
            return calculateReducer(state);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].FORMAT): {
            return formatReducer(state, action);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].FLAG): {
            return flagReducer(state, action);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].NAVIGATE_OFFSET): {
            var date = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["c" /* shiftDate */])(Object(__WEBPACK_IMPORTED_MODULE_12__bs_moment_utils_start_end_of__["b" /* startOf */])(state.view.date, 'month'), action.payload);
            var newState = {
                view: {
                    mode: state.view.mode,
                    date: date
                }
            };
            return Object.assign({}, state, newState);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].NAVIGATE_TO): {
            var payload = action.payload;
            var date = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["b" /* setDate */])(state.view.date, payload.unit);
            var mode = payload.viewMode;
            var newState = { view: { date: date, mode: mode } };
            return Object.assign({}, state, newState);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].CHANGE_VIEWMODE): {
            if (!Object(__WEBPACK_IMPORTED_MODULE_6__engine_view_mode__["a" /* canSwitchMode */])(action.payload)) {
                return state;
            }
            var date = state.view.date;
            var mode = action.payload;
            var newState = { view: { date: date, mode: mode } };
            return Object.assign({}, state, newState);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].HOVER): {
            return Object.assign({}, state, { hoveredDate: action.payload });
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].SELECT): {
            var newState = {
                selectedDate: action.payload,
                view: state.view
            };
            if (action.payload) {
                newState.view = {
                    date: action.payload,
                    mode: state.view.mode
                };
            }
            return Object.assign({}, state, newState);
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].SET_OPTIONS): {
            var newState = action.payload;
            // looks not really good
            if (newState.value) {
                newState.view = state.view;
                if (Object(__WEBPACK_IMPORTED_MODULE_11__bs_moment_utils_type_checks__["b" /* isArray */])(newState.value)) {
                    newState.view = {
                        mode: state.view.mode,
                        date: newState.value[0]
                    };
                    newState.selectedRange = newState.value;
                }
                else {
                    newState.view = {
                        mode: state.view.mode,
                        date: newState.value
                    };
                    newState.selectedDate = newState.value;
                }
            }
            return Object.assign({}, state, newState);
        }
        // date range picker
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].SELECT_RANGE): {
            return Object.assign({}, state, { selectedRange: action.payload });
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].SET_MIN_DATE): {
            return Object.assign({}, state, {
                minDate: action.payload
            });
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].SET_MAX_DATE): {
            return Object.assign({}, state, {
                maxDate: action.payload
            });
        }
        case (__WEBPACK_IMPORTED_MODULE_1__bs_datepicker_actions__["a" /* BsDatepickerActions */].SET_IS_DISABLED): {
            return Object.assign({}, state, {
                isDisabled: action.payload
            });
        }
        default:
            return state;
    }
}
function calculateReducer(state) {
    // how many calendars
    var displayMonths = state.displayMonths;
    // use selected date on initial rendering if set
    var viewDate = state.view.date;
    if (state.view.mode === 'day') {
        var monthsModel = new Array(displayMonths);
        for (var monthIndex = 0; monthIndex < displayMonths; monthIndex++) {
            // todo: for unlinked calendars it will be harder
            monthsModel[monthIndex] = Object(__WEBPACK_IMPORTED_MODULE_2__engine_calc_days_calendar__["a" /* calcDaysCalendar */])(viewDate, state.monthViewOptions);
            viewDate = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["c" /* shiftDate */])(viewDate, { month: 1 });
        }
        return Object.assign({}, state, { monthsModel: monthsModel });
    }
    if (state.view.mode === 'month') {
        var monthsCalendar = new Array(displayMonths);
        for (var calendarIndex = 0; calendarIndex < displayMonths; calendarIndex++) {
            // todo: for unlinked calendars it will be harder
            monthsCalendar[calendarIndex] = Object(__WEBPACK_IMPORTED_MODULE_7__engine_format_months_calendar__["a" /* formatMonthsCalendar */])(viewDate, getFormatOptions(state));
            viewDate = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["c" /* shiftDate */])(viewDate, { year: 1 });
        }
        return Object.assign({}, state, { monthsCalendar: monthsCalendar });
    }
    if (state.view.mode === 'year') {
        var yearsCalendarModel = new Array(displayMonths);
        for (var calendarIndex = 0; calendarIndex < displayMonths; calendarIndex++) {
            // todo: for unlinked calendars it will be harder
            yearsCalendarModel[calendarIndex] = Object(__WEBPACK_IMPORTED_MODULE_9__engine_format_years_calendar__["a" /* formatYearsCalendar */])(viewDate, getFormatOptions(state));
            viewDate = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["c" /* shiftDate */])(viewDate, { year: __WEBPACK_IMPORTED_MODULE_9__engine_format_years_calendar__["b" /* yearsPerCalendar */] });
        }
        return Object.assign({}, state, { yearsCalendarModel: yearsCalendarModel });
    }
    return state;
}
function formatReducer(state, action) {
    if (state.view.mode === 'day') {
        var formattedMonths = state.monthsModel
            .map(function (month, monthIndex) { return Object(__WEBPACK_IMPORTED_MODULE_3__engine_format_days_calendar__["a" /* formatDaysCalendar */])(month, getFormatOptions(state), monthIndex); });
        return Object.assign({}, state, { formattedMonths: formattedMonths });
    }
    // how many calendars
    var displayMonths = state.displayMonths;
    // check initial rendering
    // use selected date on initial rendering if set
    var viewDate = state.view.date;
    if (state.view.mode === 'month') {
        var monthsCalendar = new Array(displayMonths);
        for (var calendarIndex = 0; calendarIndex < displayMonths; calendarIndex++) {
            // todo: for unlinked calendars it will be harder
            monthsCalendar[calendarIndex] = Object(__WEBPACK_IMPORTED_MODULE_7__engine_format_months_calendar__["a" /* formatMonthsCalendar */])(viewDate, getFormatOptions(state));
            viewDate = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["c" /* shiftDate */])(viewDate, { year: 1 });
        }
        return Object.assign({}, state, { monthsCalendar: monthsCalendar });
    }
    if (state.view.mode === 'year') {
        var yearsCalendarModel = new Array(displayMonths);
        for (var calendarIndex = 0; calendarIndex < displayMonths; calendarIndex++) {
            // todo: for unlinked calendars it will be harder
            yearsCalendarModel[calendarIndex] = Object(__WEBPACK_IMPORTED_MODULE_9__engine_format_years_calendar__["a" /* formatYearsCalendar */])(viewDate, getFormatOptions(state));
            viewDate = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_utils_date_setters__["c" /* shiftDate */])(viewDate, { year: 16 });
        }
        return Object.assign({}, state, { yearsCalendarModel: yearsCalendarModel });
    }
    return state;
}
function flagReducer(state, action) {
    if (state.view.mode === 'day') {
        var flaggedMonths = state.formattedMonths
            .map(function (formattedMonth, monthIndex) { return Object(__WEBPACK_IMPORTED_MODULE_4__engine_flag_days_calendar__["a" /* flagDaysCalendar */])(formattedMonth, {
            isDisabled: state.isDisabled,
            minDate: state.minDate,
            maxDate: state.maxDate,
            hoveredDate: state.hoveredDate,
            selectedDate: state.selectedDate,
            selectedRange: state.selectedRange,
            displayMonths: state.displayMonths,
            monthIndex: monthIndex
        }); });
        return Object.assign({}, state, { flaggedMonths: flaggedMonths });
    }
    if (state.view.mode === 'month') {
        var flaggedMonthsCalendar = state.monthsCalendar
            .map(function (formattedMonth, monthIndex) { return Object(__WEBPACK_IMPORTED_MODULE_8__engine_flag_months_calendar__["a" /* flagMonthsCalendar */])(formattedMonth, {
            isDisabled: state.isDisabled,
            minDate: state.minDate,
            maxDate: state.maxDate,
            hoveredMonth: state.hoveredMonth,
            displayMonths: state.displayMonths,
            monthIndex: monthIndex
        }); });
        return Object.assign({}, state, { flaggedMonthsCalendar: flaggedMonthsCalendar });
    }
    if (state.view.mode === 'year') {
        var yearsCalendarFlagged = state.yearsCalendarModel
            .map(function (formattedMonth, yearIndex) { return Object(__WEBPACK_IMPORTED_MODULE_10__engine_flag_years_calendar__["a" /* flagYearsCalendar */])(formattedMonth, {
            isDisabled: state.isDisabled,
            minDate: state.minDate,
            maxDate: state.maxDate,
            hoveredYear: state.hoveredYear,
            displayMonths: state.displayMonths,
            yearIndex: yearIndex
        }); });
        return Object.assign({}, state, { yearsCalendarFlagged: yearsCalendarFlagged });
    }
    return state;
}
function getFormatOptions(state) {
    return {
        locale: state.locale,
        monthTitle: state.monthTitle,
        yearTitle: state.yearTitle,
        dayLabel: state.dayLabel,
        monthLabel: state.monthLabel,
        yearLabel: state.yearLabel,
        weekNumbers: state.weekNumbers
    };
}


/***/ }),
/* 163 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = calcDaysCalendar;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_matrix_utils__ = __webpack_require__(60);



function calcDaysCalendar(startingDate, options) {
    var firstDay = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["c" /* getFirstDayOfMonth */])(startingDate);
    var initialDate = Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["a" /* getStartingDayOfCalendar */])(firstDay, options);
    var matrixOptions = {
        width: options.width,
        height: options.height,
        initialDate: initialDate, shift: { day: 1 }
    };
    var daysMatrix = Object(__WEBPACK_IMPORTED_MODULE_2__utils_matrix_utils__["a" /* createMatrix */])(matrixOptions, function (date) { return date; });
    return {
        daysMatrix: daysMatrix,
        month: firstDay
    };
}


/***/ }),
/* 164 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = formatDaysCalendar;
/* unused harmony export getWeekNumbers */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_format__ = __webpack_require__(10);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_moment_locale_locales_service__ = __webpack_require__(12);


function formatDaysCalendar(daysCalendar, formatOptions, monthIndex) {
    return {
        month: daysCalendar.month,
        monthTitle: Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_format__["a" /* formatDate */])(daysCalendar.month, formatOptions.monthTitle, formatOptions.locale),
        yearTitle: Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_format__["a" /* formatDate */])(daysCalendar.month, formatOptions.yearTitle, formatOptions.locale),
        weekNumbers: getWeekNumbers(daysCalendar.daysMatrix, formatOptions.weekNumbers, formatOptions.locale),
        weekdays: Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_locale_locales_service__["b" /* getLocale */])(formatOptions.locale).weekdaysShort(),
        weeks: daysCalendar.daysMatrix
            .map(function (week, weekIndex) { return ({
            days: week.map(function (date, dayIndex) { return ({
                date: date,
                label: Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_format__["a" /* formatDate */])(date, formatOptions.dayLabel, formatOptions.locale),
                monthIndex: monthIndex, weekIndex: weekIndex, dayIndex: dayIndex
            }); })
        }); })
    };
}
function getWeekNumbers(daysMatrix, format, locale) {
    return daysMatrix.map(function (days) { return days[0]
        ? Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_format__["a" /* formatDate */])(days[0], format, locale)
        : ''; });
}


/***/ }),
/* 165 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = flagDaysCalendar;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_moment_utils_date_compare__ = __webpack_require__(123);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_bs_calendar_utils__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_date_setters__ = __webpack_require__(7);




function flagDaysCalendar(formattedMonth, options) {
    formattedMonth.weeks
        .forEach(function (week, weekIndex) {
        week.days.forEach(function (day, dayIndex) {
            // datepicker
            var isOtherMonth = !Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["k" /* isSameMonth */])(day.date, formattedMonth.month);
            var isHovered = !isOtherMonth && Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["j" /* isSameDay */])(day.date, options.hoveredDate);
            // date range picker
            var isSelectionStart = !isOtherMonth
                && options.selectedRange
                && Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["j" /* isSameDay */])(day.date, options.selectedRange[0]);
            var isSelectionEnd = !isOtherMonth
                && options.selectedRange
                && Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["j" /* isSameDay */])(day.date, options.selectedRange[1]);
            var isSelected = !isOtherMonth && Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["j" /* isSameDay */])(day.date, options.selectedDate) ||
                isSelectionStart || isSelectionEnd;
            var isInRange = !isOtherMonth
                && options.selectedRange
                && isDateInRange(day.date, options.selectedRange, options.hoveredDate);
            var isDisabled = options.isDisabled
                || Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_utils_date_compare__["b" /* isBefore */])(day.date, options.minDate, 'day')
                || Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_utils_date_compare__["a" /* isAfter */])(day.date, options.maxDate, 'day');
            // decide update or not
            var newDay = Object.assign({}, day, {
                isOtherMonth: isOtherMonth,
                isHovered: isHovered,
                isSelected: isSelected,
                isSelectionStart: isSelectionStart,
                isSelectionEnd: isSelectionEnd,
                isInRange: isInRange,
                isDisabled: isDisabled
            });
            if (day.isOtherMonth !== newDay.isOtherMonth ||
                day.isHovered !== newDay.isHovered ||
                day.isSelected !== newDay.isSelected ||
                day.isSelectionStart !== newDay.isSelectionStart ||
                day.isSelectionEnd !== newDay.isSelectionEnd ||
                day.isDisabled !== newDay.isDisabled ||
                day.isInRange !== newDay.isInRange) {
                week.days[dayIndex] = newDay;
            }
        });
    });
    // todo: add check for linked calendars
    formattedMonth.hideLeftArrow = options.isDisabled
        || (options.monthIndex > 0
            && options.monthIndex !== options.displayMonths);
    formattedMonth.hideRightArrow = options.isDisabled
        || (options.monthIndex < options.displayMonths
            && (options.monthIndex + 1) !== options.displayMonths);
    formattedMonth.disableLeftArrow = Object(__WEBPACK_IMPORTED_MODULE_2__utils_bs_calendar_utils__["b" /* isMonthDisabled */])(Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_date_setters__["c" /* shiftDate */])(formattedMonth.month, { month: -1 }), options.minDate, options.maxDate);
    formattedMonth.disableRightArrow = Object(__WEBPACK_IMPORTED_MODULE_2__utils_bs_calendar_utils__["b" /* isMonthDisabled */])(Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_utils_date_setters__["c" /* shiftDate */])(formattedMonth.month, { month: 1 }), options.minDate, options.maxDate);
    return formattedMonth;
}
function isDateInRange(date, selectedRange, hoveredDate) {
    if (!date || !selectedRange[0]) {
        return false;
    }
    if (selectedRange[1]) {
        return date > selectedRange[0] && date <= selectedRange[1];
    }
    if (hoveredDate) {
        return date > selectedRange[0] && date <= hoveredDate;
    }
    return false;
}


/***/ }),
/* 166 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = canSwitchMode;
function canSwitchMode(mode) {
    return true;
}


/***/ }),
/* 167 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = formatMonthsCalendar;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_start_end_of__ = __webpack_require__(17);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_moment_format__ = __webpack_require__(10);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__utils_matrix_utils__ = __webpack_require__(60);



var height = 4;
var width = 3;
var shift = { month: 1 };
function formatMonthsCalendar(viewDate, formatOptions) {
    var initialDate = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_start_end_of__["b" /* startOf */])(viewDate, 'year');
    var matrixOptions = { width: width, height: height, initialDate: initialDate, shift: shift };
    var monthMatrix = Object(__WEBPACK_IMPORTED_MODULE_2__utils_matrix_utils__["a" /* createMatrix */])(matrixOptions, function (date) { return ({
        date: date,
        label: Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_format__["a" /* formatDate */])(date, formatOptions.monthLabel, formatOptions.locale)
    }); });
    return {
        months: monthMatrix,
        monthTitle: '',
        yearTitle: Object(__WEBPACK_IMPORTED_MODULE_1__bs_moment_format__["a" /* formatDate */])(viewDate, formatOptions.yearTitle, formatOptions.locale)
    };
}


/***/ }),
/* 168 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = flagMonthsCalendar;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_setters__ = __webpack_require__(7);



function flagMonthsCalendar(monthCalendar, options) {
    monthCalendar.months
        .forEach(function (months, rowIndex) {
        months.forEach(function (month, monthIndex) {
            var isHovered = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["k" /* isSameMonth */])(month.date, options.hoveredMonth);
            var isDisabled = options.isDisabled
                || Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["b" /* isMonthDisabled */])(month.date, options.minDate, options.maxDate);
            var newMonth = Object.assign(/*{},*/ month, { isHovered: isHovered, isDisabled: isDisabled });
            if (month.isHovered !== newMonth.isHovered
                || month.isDisabled !== newMonth.isDisabled) {
                monthCalendar.months[rowIndex][monthIndex] = newMonth;
            }
        });
    });
    // todo: add check for linked calendars
    monthCalendar.hideLeftArrow = options.monthIndex > 0
        && options.monthIndex !== options.displayMonths;
    monthCalendar.hideRightArrow = options.monthIndex < options.displayMonths
        && (options.monthIndex + 1) !== options.displayMonths;
    monthCalendar.disableLeftArrow = Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["c" /* isYearDisabled */])(Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_setters__["c" /* shiftDate */])(monthCalendar.months[0][0].date, { year: -1 }), options.minDate, options.maxDate);
    monthCalendar.disableRightArrow = Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["c" /* isYearDisabled */])(Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_setters__["c" /* shiftDate */])(monthCalendar.months[0][0].date, { year: 1 }), options.minDate, options.maxDate);
    return monthCalendar;
}


/***/ }),
/* 169 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = flagYearsCalendar;
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_setters__ = __webpack_require__(7);



function flagYearsCalendar(yearsCalendar, options) {
    yearsCalendar.years
        .forEach(function (years, rowIndex) {
        years.forEach(function (year, yearIndex) {
            var isHovered = Object(__WEBPACK_IMPORTED_MODULE_0__bs_moment_utils_date_getters__["l" /* isSameYear */])(year.date, options.hoveredYear);
            var isDisabled = options.isDisabled
                || Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["c" /* isYearDisabled */])(year.date, options.minDate, options.maxDate);
            var newMonth = Object.assign(/*{},*/ year, { isHovered: isHovered, isDisabled: isDisabled });
            if (year.isHovered !== newMonth.isHovered
                || year.isDisabled !== newMonth.isDisabled) {
                yearsCalendar.years[rowIndex][yearIndex] = newMonth;
            }
        });
    });
    // todo: add check for linked calendars
    yearsCalendar.hideLeftArrow = options.yearIndex > 0
        && options.yearIndex !== options.displayMonths;
    yearsCalendar.hideRightArrow = options.yearIndex < options.displayMonths
        && (options.yearIndex + 1) !== options.displayMonths;
    yearsCalendar.disableLeftArrow = Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["c" /* isYearDisabled */])(Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_setters__["c" /* shiftDate */])(yearsCalendar.years[0][0].date, { year: -1 }), options.minDate, options.maxDate);
    var i = yearsCalendar.years.length - 1;
    var j = yearsCalendar.years[i].length - 1;
    yearsCalendar.disableRightArrow = Object(__WEBPACK_IMPORTED_MODULE_1__utils_bs_calendar_utils__["c" /* isYearDisabled */])(Object(__WEBPACK_IMPORTED_MODULE_2__bs_moment_utils_date_setters__["c" /* shiftDate */])(yearsCalendar.years[i][j].date, { year: 1 }), options.minDate, options.maxDate);
    return yearsCalendar;
}


/***/ }),
/* 170 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerNavigationViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__models_index__ = __webpack_require__(29);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var BsDatepickerNavigationViewComponent = (function () {
    function BsDatepickerNavigationViewComponent() {
        this.onNavigate = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onViewMode = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
    }
    BsDatepickerNavigationViewComponent.prototype.navTo = function (down) {
        this.onNavigate.emit(down
            ? __WEBPACK_IMPORTED_MODULE_1__models_index__["a" /* BsNavigationDirection */].DOWN
            : __WEBPACK_IMPORTED_MODULE_1__models_index__["a" /* BsNavigationDirection */].UP);
    };
    BsDatepickerNavigationViewComponent.prototype.view = function (viewMode) {
        this.onViewMode.emit(viewMode);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDatepickerNavigationViewComponent.prototype, "calendar", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsDatepickerNavigationViewComponent.prototype, "onNavigate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsDatepickerNavigationViewComponent.prototype, "onViewMode", void 0);
    BsDatepickerNavigationViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-datepicker-navigation-view',
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush,
            template: "\n    <button class=\"previous\"\n            [disabled]=\"calendar.disableLeftArrow\"\n            [style.visibility]=\"calendar.hideLeftArrow ? 'hidden' : 'visible'\"\n            (click)=\"navTo(true)\"><span>&lsaquo;</span>\n    </button>\n\n    <button class=\"current\"\n            *ngIf=\"calendar.monthTitle\"\n            (click)=\"view('month')\"\n    ><span>{{ calendar.monthTitle }}</span>\n    </button>\n\n    <button class=\"current\" (click)=\"view('year')\"\n    ><span>{{ calendar.yearTitle }}</span></button>\n\n    <button class=\"next\"\n            [disabled]=\"calendar.disableRightArrow\"\n            [style.visibility]=\"calendar.hideRightArrow ? 'hidden' : 'visible'\"\n            (click)=\"navTo(false)\"><span>&rsaquo;</span>\n    </button>\n  "
        })
    ], BsDatepickerNavigationViewComponent);
    return BsDatepickerNavigationViewComponent;
}());



/***/ }),
/* 171 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDaysCalendarViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__models_index__ = __webpack_require__(29);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var BsDaysCalendarViewComponent = (function () {
    function BsDaysCalendarViewComponent() {
        this.onNavigate = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onViewMode = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onSelect = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onHover = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
    }
    BsDaysCalendarViewComponent.prototype.navigateTo = function (event) {
        var step = __WEBPACK_IMPORTED_MODULE_1__models_index__["a" /* BsNavigationDirection */].DOWN === event ? -1 : 1;
        this.onNavigate.emit({ step: { month: step } });
    };
    BsDaysCalendarViewComponent.prototype.changeViewMode = function (event) {
        this.onViewMode.emit(event);
    };
    BsDaysCalendarViewComponent.prototype.selectDay = function (event) {
        this.onSelect.emit(event);
    };
    BsDaysCalendarViewComponent.prototype.hoverDay = function (cell, isHovered) {
        this.onHover.emit({ cell: cell, isHovered: isHovered });
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDaysCalendarViewComponent.prototype, "calendar", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDaysCalendarViewComponent.prototype, "options", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsDaysCalendarViewComponent.prototype, "onNavigate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsDaysCalendarViewComponent.prototype, "onViewMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsDaysCalendarViewComponent.prototype, "onSelect", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsDaysCalendarViewComponent.prototype, "onHover", void 0);
    BsDaysCalendarViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-days-calendar-view',
            // changeDetection: ChangeDetectionStrategy.OnPush,
            template: "\n    <bs-calendar-layout>\n      <bs-datepicker-navigation-view\n        [calendar]=\"calendar\"\n        (onNavigate)=\"navigateTo($event)\"\n        (onViewMode)=\"changeViewMode($event)\"\n      ></bs-datepicker-navigation-view>\n\n      <!--days matrix-->\n      <table role=\"grid\" class=\"days weeks\">\n        <thead>\n        <tr>\n          <!--if show weeks-->\n          <th *ngIf=\"options.showWeekNumbers\"></th>\n          <th *ngFor=\"let weekday of calendar.weekdays; let i = index\"\n              aria-label=\"weekday\">{{ calendar.weekdays[i] }}\n          </th>\n        </tr>\n        </thead>\n        <tbody>\n        <tr *ngFor=\"let week of calendar.weeks; let i = index\">\n          <td class=\"week\" *ngIf=\"options.showWeekNumbers\">\n            <span>{{ calendar.weekNumbers[i] }}</span>\n          </td>\n          <td *ngFor=\"let day of week.days\" role=\"gridcell\">\n          <span bsDatepickerDayDecorator\n                [day]=\"day\"\n                (click)=\"selectDay(day)\"\n                (mouseenter)=\"hoverDay(day, true)\"\n                (mouseleave)=\"hoverDay(day, false)\">{{ day.label }}</span>\n          </td>\n        </tr>\n        </tbody>\n      </table>\n\n    </bs-calendar-layout>\n  "
        })
    ], BsDaysCalendarViewComponent);
    return BsDaysCalendarViewComponent;
}());



/***/ }),
/* 172 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerDayDecoratorComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var BsDatepickerDayDecoratorComponent = (function () {
    function BsDatepickerDayDecoratorComponent() {
    }
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsDatepickerDayDecoratorComponent.prototype, "day", void 0);
    BsDatepickerDayDecoratorComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: '[bsDatepickerDayDecorator]',
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush,
            host: {
                '[class.disabled]': 'day.isDisabled',
                '[class.is-highlighted]': 'day.isHovered',
                '[class.is-other-month]': 'day.isOtherMonth',
                '[class.in-range]': 'day.isInRange',
                '[class.select-start]': 'day.isSelectionStart',
                '[class.select-end]': 'day.isSelectionEnd',
                '[class.selected]': 'day.isSelected'
            },
            template: "{{ day.label }}"
        })
    ], BsDatepickerDayDecoratorComponent);
    return BsDatepickerDayDecoratorComponent;
}());



/***/ }),
/* 173 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsMonthCalendarViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__models_index__ = __webpack_require__(29);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var BsMonthCalendarViewComponent = (function () {
    function BsMonthCalendarViewComponent() {
        this.onNavigate = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onViewMode = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onSelect = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onHover = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
    }
    BsMonthCalendarViewComponent.prototype.navigateTo = function (event) {
        var step = __WEBPACK_IMPORTED_MODULE_1__models_index__["a" /* BsNavigationDirection */].DOWN === event ? -1 : 1;
        this.onNavigate.emit({ step: { year: step } });
    };
    BsMonthCalendarViewComponent.prototype.viewMonth = function (month) {
        this.onSelect.emit(month);
    };
    BsMonthCalendarViewComponent.prototype.hoverMonth = function (cell, isHovered) {
        this.onHover.emit({ cell: cell, isHovered: isHovered });
    };
    BsMonthCalendarViewComponent.prototype.changeViewMode = function (event) {
        this.onViewMode.emit(event);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsMonthCalendarViewComponent.prototype, "calendar", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsMonthCalendarViewComponent.prototype, "onNavigate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsMonthCalendarViewComponent.prototype, "onViewMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsMonthCalendarViewComponent.prototype, "onSelect", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsMonthCalendarViewComponent.prototype, "onHover", void 0);
    BsMonthCalendarViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-month-calendar-view',
            template: "\n    <bs-calendar-layout>\n      <bs-datepicker-navigation-view\n        [calendar]=\"calendar\"\n        (onNavigate)=\"navigateTo($event)\"\n        (onViewMode)=\"changeViewMode($event)\"\n      ></bs-datepicker-navigation-view>\n\n      <table role=\"grid\" class=\"months\">\n        <tbody>\n        <tr *ngFor=\"let row of calendar.months\">\n          <td *ngFor=\"let month of row\" role=\"gridcell\"\n              (click)=\"viewMonth(month)\"\n              (mouseenter)=\"hoverMonth(month, true)\"\n              (mouseleave)=\"hoverMonth(month, false)\"\n              [class.disabled]=\"month.isDisabled\"\n              [class.is-highlighted]=\"month.isHovered\">\n            <span>{{ month.label }}</span>\n          </td>\n        </tr>\n        </tbody>\n      </table>\n    </bs-calendar-layout>\n  "
        })
    ], BsMonthCalendarViewComponent);
    return BsMonthCalendarViewComponent;
}());



/***/ }),
/* 174 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsYearsCalendarViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__engine_format_years_calendar__ = __webpack_require__(124);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__models_index__ = __webpack_require__(29);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};



var BsYearsCalendarViewComponent = (function () {
    function BsYearsCalendarViewComponent() {
        this.onNavigate = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onViewMode = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onSelect = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.onHover = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
    }
    BsYearsCalendarViewComponent.prototype.navigateTo = function (event) {
        var step = __WEBPACK_IMPORTED_MODULE_2__models_index__["a" /* BsNavigationDirection */].DOWN === event ? -1 : 1;
        this.onNavigate.emit({ step: { year: step * __WEBPACK_IMPORTED_MODULE_1__engine_format_years_calendar__["b" /* yearsPerCalendar */] } });
    };
    BsYearsCalendarViewComponent.prototype.viewYear = function (year) {
        this.onSelect.emit(year);
    };
    BsYearsCalendarViewComponent.prototype.hoverYear = function (cell, isHovered) {
        this.onHover.emit({ cell: cell, isHovered: isHovered });
    };
    BsYearsCalendarViewComponent.prototype.changeViewMode = function (event) {
        this.onViewMode.emit(event);
    };
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Object)
    ], BsYearsCalendarViewComponent.prototype, "calendar", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsYearsCalendarViewComponent.prototype, "onNavigate", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsYearsCalendarViewComponent.prototype, "onViewMode", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsYearsCalendarViewComponent.prototype, "onSelect", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
        __metadata("design:type", Object)
    ], BsYearsCalendarViewComponent.prototype, "onHover", void 0);
    BsYearsCalendarViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-years-calendar-view',
            template: "\n    <bs-calendar-layout>\n      <bs-datepicker-navigation-view\n        [calendar]=\"calendar\"\n        (onNavigate)=\"navigateTo($event)\"\n        (onViewMode)=\"changeViewMode($event)\"\n      ></bs-datepicker-navigation-view>\n\n      <table role=\"grid\" class=\"years\">\n        <tbody>\n        <tr *ngFor=\"let row of calendar.years\">\n          <td *ngFor=\"let year of row\" role=\"gridcell\"\n              (click)=\"viewYear(year)\"\n              (mouseenter)=\"hoverYear(year, true)\"\n              (mouseleave)=\"hoverYear(year, false)\"\n              [class.disabled]=\"year.isDisabled\"\n              [class.is-highlighted]=\"year.isHovered\">\n            <span>{{ year.label }}</span>\n          </td>\n        </tr>\n        </tbody>\n      </table>\n    </bs-calendar-layout>\n  "
        })
    ], BsYearsCalendarViewComponent);
    return BsYearsCalendarViewComponent;
}());



/***/ }),
/* 175 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsCustomDatesViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var BsCustomDatesViewComponent = (function () {
    function BsCustomDatesViewComponent() {
    }
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Boolean)
    ], BsCustomDatesViewComponent.prototype, "isCustomRangeShown", void 0);
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", Array)
    ], BsCustomDatesViewComponent.prototype, "ranges", void 0);
    BsCustomDatesViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-custom-date-view',
            template: "\n    <div class=\"bs-datepicker-predefined-btns\">\n      <button *ngFor=\"let range of ranges\">{{ range.label }}</button>\n      <button *ngIf=\"isCustomRangeShown\">Custom Range</button>\n    </div>\n  ",
            changeDetection: __WEBPACK_IMPORTED_MODULE_0__angular_core__["ChangeDetectionStrategy"].OnPush
        })
    ], BsCustomDatesViewComponent);
    return BsCustomDatesViewComponent;
}());



/***/ }),
/* 176 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsCurrentDateViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};

var BsCurrentDateViewComponent = (function () {
    function BsCurrentDateViewComponent() {
    }
    __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
        __metadata("design:type", String)
    ], BsCurrentDateViewComponent.prototype, "title", void 0);
    BsCurrentDateViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-current-date',
            template: "<div class=\"current-timedate\"><span>{{ title }}</span></div>"
        })
    ], BsCurrentDateViewComponent);
    return BsCurrentDateViewComponent;
}());



/***/ }),
/* 177 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsTimepickerViewComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var BsTimepickerViewComponent = (function () {
    function BsTimepickerViewComponent() {
        this.ampm = 'ok';
        this.hours = 0;
        this.minutes = 0;
    }
    BsTimepickerViewComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-timepicker',
            template: "\n<div class=\"bs-timepicker-container\">\n  <div class=\"bs-timepicker-controls\">\n    <button class=\"bs-decrease\">-</button>\n    <input type=\"text\" [value]=\"hours\" placeholder=\"00\">\n    <button class=\"bs-increase\">+</button>\n  </div>\n  <div class=\"bs-timepicker-controls\">\n    <button class=\"bs-decrease\">-</button>\n    <input type=\"text\" [value]=\"minutes\" placeholder=\"00\">\n    <button class=\"bs-increase\">+</button>\n  </div>\n  <button class=\"switch-time-format\">{{ ampm }}\n    <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAKCAYAAABi8KSDAAABSElEQVQYV3XQPUvDUBQG4HNuagtVqc6KgouCv6GIuIntYBLB9hcIQpLStCAIV7DYmpTcRWcXqZio3Vwc/UCc/QEqfgyKGbr0I7nS1EiHeqYzPO/h5SD0jaxUZjmSLCB+OFb+UFINFwASAEAdpu9gaGXVyAHHFQBkHpKHc6a9dzECvADyY9sqlAMsK9W0jzxDXqeytr3mhQckxSji27TJJ5/rPmIpwJJq3HrtduriYOurv1a4i1p5HnhkG9OFymi0ReoO05cGwb+ayv4dysVygjeFmsP05f8wpZQ8fsdvfmuY9zjWSNqUtgYFVnOVReILYoBFzdQI5/GGFzNHhGbeZnopDGU29sZbscgldmC99w35VOATTycIMMcBXIfpSVGzZhA6C8hh00conln6VQ9TGgV32OEAKQC4DrBq7CJwd0ggR7Vq/rPrfgB+C3sGypY5DAAAAABJRU5ErkJggg==\" alt=\"\">\n  </button>\n</div>\n"
        })
    ], BsTimepickerViewComponent);
    return BsTimepickerViewComponent;
}());



/***/ }),
/* 178 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsCalendarLayoutComponent; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};

var BsCalendarLayoutComponent = (function () {
    function BsCalendarLayoutComponent() {
    }
    BsCalendarLayoutComponent = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
            selector: 'bs-calendar-layout',
            template: "\n    <!-- current date, will be added in nearest releases -->\n    <bs-current-date title=\"hey there\" *ngIf=\"false\"></bs-current-date>\n\n    <!--navigation-->\n    <div class=\"bs-datepicker-head\">\n      <ng-content select=\"bs-datepicker-navigation-view\"></ng-content>\n    </div>\n\n    <div class=\"bs-datepicker-body\">\n      <ng-content></ng-content>\n    </div>\n\n    <!--timepicker-->\n    <bs-timepicker *ngIf=\"false\"></bs-timepicker>\n  "
        })
    ], BsCalendarLayoutComponent);
    return BsCalendarLayoutComponent;
}());



/***/ }),
/* 179 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDatepickerInputDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_datepicker_component__ = __webpack_require__(63);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_moment_format__ = __webpack_require__(10);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_datepicker_config__ = __webpack_require__(8);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_moment_locale_locales_service__ = __webpack_require__(12);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};






var BS_DATEPICKER_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return BsDatepickerInputDirective; }),
    multi: true
};
var BsDatepickerInputDirective = (function () {
    function BsDatepickerInputDirective(_picker, _config, _renderer, _elRef) {
        this._picker = _picker;
        this._config = _config;
        this._renderer = _renderer;
        this._elRef = _elRef;
        this._onChange = Function.prototype;
        this._onTouched = Function.prototype;
    }
    BsDatepickerInputDirective.prototype.ngOnInit = function () {
        var _this = this;
        this._picker.bsValueChange.subscribe(function (v) {
            _this._renderer.setElementProperty(_this._elRef.nativeElement, 'value', Object(__WEBPACK_IMPORTED_MODULE_3__bs_moment_format__["a" /* formatDate */])(v, _this._picker._config.dateInputFormat, _this._picker._config.locale) || '');
            _this._onChange(v);
        });
    };
    BsDatepickerInputDirective.prototype.onChange = function (event) {
        this.writeValue(event.target.value);
        this._onTouched();
    };
    BsDatepickerInputDirective.prototype.writeValue = function (value) {
        if (!value) {
            this._picker.bsValue = null;
        }
        var _locale = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_locale_locales_service__["b" /* getLocale */])(this._picker._config.locale);
        if (!_locale) {
            throw new Error("Locale \"" + this._picker._config.locale + "\" is not defined, please add it with \"defineLocale(...)\"");
        }
        if (typeof value === 'string') {
            var date = new Date(_locale.preparse(value));
            this._picker.bsValue = isNaN(date.valueOf()) ? null : date;
        }
        if (value instanceof Date) {
            this._picker.bsValue = value;
        }
    };
    BsDatepickerInputDirective.prototype.setDisabledState = function (isDisabled) {
        this._picker.isDisabled = isDisabled;
        this._renderer.setElementAttribute(this._elRef.nativeElement, 'disabled', 'disabled');
    };
    BsDatepickerInputDirective.prototype.registerOnChange = function (fn) { this._onChange = fn; };
    BsDatepickerInputDirective.prototype.registerOnTouched = function (fn) { this._onTouched = fn; };
    BsDatepickerInputDirective.prototype.onBlur = function () { this._onTouched(); };
    BsDatepickerInputDirective.prototype.hide = function () {
        this._picker.hide();
    };
    BsDatepickerInputDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: "input[bsDatepicker]",
            host: {
                '(change)': 'onChange($event)',
                '(keyup.esc)': 'hide()',
                '(blur)': 'onBlur()'
            },
            providers: [BS_DATEPICKER_VALUE_ACCESSOR]
        }),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Host"])()),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_2__bs_datepicker_component__["a" /* BsDatepickerComponent */],
            __WEBPACK_IMPORTED_MODULE_4__bs_datepicker_config__["a" /* BsDatepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]])
    ], BsDatepickerInputDirective);
    return BsDatepickerInputDirective;
}());



/***/ }),
/* 180 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return BsDaterangepickerInputDirective; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_0__angular_core__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms__ = __webpack_require__(4);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_forms___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1__angular_forms__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_datepicker_config__ = __webpack_require__(8);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_daterangepicker_component__ = __webpack_require__(62);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_moment_format__ = __webpack_require__(10);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_moment_locale_locales_service__ = __webpack_require__(12);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};






var BS_DATERANGEPICKER_VALUE_ACCESSOR = {
    provide: __WEBPACK_IMPORTED_MODULE_1__angular_forms__["NG_VALUE_ACCESSOR"],
    useExisting: Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["forwardRef"])(function () { return BsDaterangepickerInputDirective; }),
    multi: true
};
var BsDaterangepickerInputDirective = (function () {
    function BsDaterangepickerInputDirective(_picker, _config, _renderer, _elRef) {
        this._picker = _picker;
        this._config = _config;
        this._renderer = _renderer;
        this._elRef = _elRef;
        this._onChange = Function.prototype;
        this._onTouched = Function.prototype;
    }
    BsDaterangepickerInputDirective.prototype.ngOnInit = function () {
        var _this = this;
        this._picker.bsValueChange.subscribe(function (v) {
            var range = '';
            if (v) {
                var start = Object(__WEBPACK_IMPORTED_MODULE_4__bs_moment_format__["a" /* formatDate */])(v[0], _this._picker._config.rangeInputFormat, _this._picker._config.locale);
                var end = Object(__WEBPACK_IMPORTED_MODULE_4__bs_moment_format__["a" /* formatDate */])(v[1], _this._picker._config.rangeInputFormat, _this._picker._config.locale);
                range = start + _this._picker._config.rangeSeparator + end;
            }
            _this._renderer.setElementProperty(_this._elRef.nativeElement, 'value', range);
            _this._onChange(v);
        });
    };
    BsDaterangepickerInputDirective.prototype.onChange = function (event) {
        this.writeValue(event.target.value);
        this._onTouched();
    };
    BsDaterangepickerInputDirective.prototype.writeValue = function (value) {
        if (!value) {
            this._picker.bsValue = null;
        }
        var _locale = Object(__WEBPACK_IMPORTED_MODULE_5__bs_moment_locale_locales_service__["b" /* getLocale */])(this._picker._config.locale);
        if (!_locale) {
            throw new Error("Locale \"" + this._picker._config.locale + "\" is not defined, please add it with \"defineLocale(...)\"");
        }
        if (typeof value === 'string') {
            this._picker.bsValue = value
                .split(this._picker._config.rangeSeparator)
                .map(function (date) { return new Date(_locale.preparse(date)); })
                .map(function (date) { return isNaN(date.valueOf()) ? null : date; });
        }
        if (Array.isArray(value)) {
            this._picker.bsValue = value;
        }
    };
    BsDaterangepickerInputDirective.prototype.setDisabledState = function (isDisabled) {
        this._picker.isDisabled = isDisabled;
        this._renderer.setElementAttribute(this._elRef.nativeElement, 'disabled', 'disabled');
    };
    BsDaterangepickerInputDirective.prototype.registerOnChange = function (fn) { this._onChange = fn; };
    BsDaterangepickerInputDirective.prototype.registerOnTouched = function (fn) { this._onTouched = fn; };
    BsDaterangepickerInputDirective.prototype.onBlur = function () { this._onTouched(); };
    BsDaterangepickerInputDirective.prototype.hide = function () {
        this._picker.hide();
    };
    BsDaterangepickerInputDirective = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Directive"])({
            selector: "input[bsDaterangepicker]",
            host: {
                '(change)': 'onChange($event)',
                '(keyup.esc)': 'hide()',
                '(blur)': 'onBlur()'
            },
            providers: [BS_DATERANGEPICKER_VALUE_ACCESSOR]
        }),
        __param(0, Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Host"])()),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_3__bs_daterangepicker_component__["a" /* BsDaterangepickerComponent */],
            __WEBPACK_IMPORTED_MODULE_2__bs_datepicker_config__["a" /* BsDatepickerConfig */],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["Renderer"],
            __WEBPACK_IMPORTED_MODULE_0__angular_core__["ElementRef"]])
    ], BsDaterangepickerInputDirective);
    return BsDaterangepickerInputDirective;
}());



/***/ }),
/* 181 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__modal_container_component__ = __webpack_require__(44);
/* unused harmony reexport ModalContainerComponent */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__ = __webpack_require__(22);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_1__modal_backdrop_component__["b"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__modal_options_class__ = __webpack_require__(18);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "g", function() { return __WEBPACK_IMPORTED_MODULE_2__modal_options_class__["d"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_2__modal_options_class__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__modal_component__ = __webpack_require__(92);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_3__modal_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__modal_module__ = __webpack_require__(91);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return __WEBPACK_IMPORTED_MODULE_4__modal_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_modal_service__ = __webpack_require__(45);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_5__bs_modal_service__["a"]; });








/***/ }),
/* 182 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bs_dropdown_directive__ = __webpack_require__(90);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_0__bs_dropdown_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__bs_dropdown_menu_directive__ = __webpack_require__(88);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_1__bs_dropdown_menu_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__bs_dropdown_toggle_directive__ = __webpack_require__(89);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "g", function() { return __WEBPACK_IMPORTED_MODULE_2__bs_dropdown_toggle_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__bs_dropdown_container_component__ = __webpack_require__(42);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_3__bs_dropdown_container_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__bs_dropdown_state__ = __webpack_require__(14);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return __WEBPACK_IMPORTED_MODULE_4__bs_dropdown_state__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_config__ = __webpack_require__(43);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_5__bs_dropdown_config__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__bs_dropdown_module__ = __webpack_require__(83);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_6__bs_dropdown_module__["a"]; });









/***/ }),
/* 183 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__pager_component__ = __webpack_require__(94);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__pager_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__pagination_component__ = __webpack_require__(95);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__pagination_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__pagination_module__ = __webpack_require__(93);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_2__pagination_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__pagination_config__ = __webpack_require__(23);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_3__pagination_config__["a"]; });






/***/ }),
/* 184 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__bar_component__ = __webpack_require__(97);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__bar_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__progress_directive__ = __webpack_require__(46);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__progress_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__progressbar_component__ = __webpack_require__(98);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_2__progressbar_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__progressbar_module__ = __webpack_require__(96);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_3__progressbar_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__progressbar_config__ = __webpack_require__(47);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_4__progressbar_config__["a"]; });







/***/ }),
/* 185 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__rating_component__ = __webpack_require__(100);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__rating_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__rating_module__ = __webpack_require__(99);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__rating_module__["a"]; });




/***/ }),
/* 186 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__ng_transclude_directive__ = __webpack_require__(104);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__ng_transclude_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tab_directive__ = __webpack_require__(49);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_1__tab_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__tab_heading_directive__ = __webpack_require__(105);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_2__tab_heading_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__tabset_component__ = __webpack_require__(50);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_3__tabset_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__tabset_config__ = __webpack_require__(51);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "f", function() { return __WEBPACK_IMPORTED_MODULE_4__tabset_config__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__tabs_module__ = __webpack_require__(103);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_5__tabs_module__["a"]; });








/***/ }),
/* 187 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__timepicker_component__ = __webpack_require__(107);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__timepicker_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__reducer_timepicker_actions__ = __webpack_require__(24);
/* unused harmony reexport TimepickerActions */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__reducer_timepicker_store__ = __webpack_require__(52);
/* unused harmony reexport TimepickerStore */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__timepicker_config__ = __webpack_require__(25);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_3__timepicker_config__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__timepicker_module__ = __webpack_require__(106);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_4__timepicker_module__["a"]; });







/***/ }),
/* 188 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__tooltip_container_component__ = __webpack_require__(55);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_0__tooltip_container_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__tooltip_directive__ = __webpack_require__(113);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_1__tooltip_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__tooltip_module__ = __webpack_require__(112);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_2__tooltip_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__tooltip_config__ = __webpack_require__(26);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_3__tooltip_config__["a"]; });






/***/ }),
/* 189 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__latin_map__ = __webpack_require__(116);
/* unused harmony reexport latinMap */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__typeahead_options_class__ = __webpack_require__(190);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "e", function() { return __WEBPACK_IMPORTED_MODULE_1__typeahead_options_class__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__typeahead_match_class__ = __webpack_require__(119);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_2__typeahead_match_class__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__typeahead_utils__ = __webpack_require__(57);
/* unused harmony reexport escapeRegexp */
/* unused harmony reexport getValueFromObject */
/* unused harmony reexport tokenize */
/* unused harmony reexport latinize */
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__typeahead_container_component__ = __webpack_require__(56);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_4__typeahead_container_component__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__typeahead_directive__ = __webpack_require__(117);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_5__typeahead_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__typeahead_module__ = __webpack_require__(115);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_6__typeahead_module__["a"]; });









/***/ }),
/* 190 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TypeaheadOptions; });
var TypeaheadOptions = (function () {
    function TypeaheadOptions(options) {
        Object.assign(this, options);
    }
    return TypeaheadOptions;
}());



/***/ }),
/* 191 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__popover_directive__ = __webpack_require__(121);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "c", function() { return __WEBPACK_IMPORTED_MODULE_0__popover_directive__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__popover_module__ = __webpack_require__(120);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "d", function() { return __WEBPACK_IMPORTED_MODULE_1__popover_module__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__popover_config__ = __webpack_require__(27);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_2__popover_config__["a"]; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__popover_container_component__ = __webpack_require__(58);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_3__popover_container_component__["a"]; });






/***/ }),
/* 192 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__locale_locales_service__ = __webpack_require__(12);
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return __WEBPACK_IMPORTED_MODULE_0__locale_locales_service__["a"]; });
/* harmony reexport (binding) */ __webpack_require__.d(__webpack_exports__, "b", function() { return __WEBPACK_IMPORTED_MODULE_0__locale_locales_service__["c"]; });
/* unused harmony reexport listLocales */



/***/ })
/******/ ]);
});
//# sourceMappingURL=ngx-bootstrap.umd.js.map