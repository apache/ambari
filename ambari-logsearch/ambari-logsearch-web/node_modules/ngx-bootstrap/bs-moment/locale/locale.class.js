import { weekOfYear } from '../units/week-calendar-utils';
import { isArray, isFunction } from '../utils/type-checks';
import { getDayOfWeek, getMonth } from '../utils/date-getters';
var MONTHS_IN_FORMAT = /D[oD]?(\[[^\[\]]*\]|\s)+MMMM?/;
export var defaultLocaleMonths = 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_');
export var defaultLocaleMonthsShort = 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_');
export var defaultLocaleWeekdays = 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_');
export var defaultLocaleWeekdaysShort = 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_');
export var defaultLocaleWeekdaysMin = 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_');
export var defaultLongDateFormat = {
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
            var key = isFunction(prop) ? i : "_" + i;
            this[key] = prop;
        }
        this._config = config;
    };
    // Months
    // LOCALES
    Locale.prototype.months = function (date, format) {
        if (!date) {
            return isArray(this._months)
                ? this._months
                : this._months.standalone;
        }
        if (isArray(this._months)) {
            return this._months[getMonth(date)];
        }
        var key = (this._months.isFormat || MONTHS_IN_FORMAT)
            .test(format) ? 'format' : 'standalone';
        return this._months[key][getMonth(date)];
    };
    Locale.prototype.monthsShort = function (date, format) {
        if (!date) {
            return isArray(this._monthsShort)
                ? this._monthsShort
                : this._monthsShort.standalone;
        }
        if (isArray(this._monthsShort)) {
            return this._monthsShort[getMonth(date)];
        }
        var key = MONTHS_IN_FORMAT.test(format) ? 'format' : 'standalone';
        return this._monthsShort[key][getMonth(date)];
    };
    // Days of week
    // LOCALES
    Locale.prototype.weekdays = function (date, format) {
        var _isArray = isArray(this._weekdays);
        if (!date) {
            return _isArray
                ? this._weekdays
                : this._weekdays.standalone;
        }
        if (_isArray) {
            return this._weekdays[getDayOfWeek(date)];
        }
        var _key = this._weekdays.isFormat.test(format) ? 'format' : 'standalone';
        return this._weekdays[_key][getDayOfWeek(date)];
    };
    Locale.prototype.weekdaysMin = function (date) {
        return (date) ? this._weekdaysShort[getDayOfWeek(date)] : this._weekdaysShort;
    };
    Locale.prototype.weekdaysShort = function (date) {
        return (date) ? this._weekdaysMin[getDayOfWeek(date)] : this._weekdaysMin;
    };
    Locale.prototype.week = function (date) {
        return weekOfYear(date, this._week.dow, this._week.doy).week;
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
export { Locale };
//# sourceMappingURL=locale.class.js.map