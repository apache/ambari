import { defaultLocaleMonths, defaultLocaleMonthsShort, defaultLocaleWeekdays, defaultLocaleWeekdaysMin, defaultLocaleWeekdaysShort } from './locale.class';
export var defaultInvalidDate = 'Invalid date';
export var defaultLocaleWeek = {
    dow: 0,
    doy: 6 // The week that contains Jan 1st is the first week of the year.
};
export var defaultLocaleMeridiemParse = /[ap]\.?m?\.?/i;
export var baseConfig = {
    // calendar: defaultCalendar,
    // longDateFormat: defaultLongDateFormat,
    invalidDate: defaultInvalidDate,
    // ordinal: defaultOrdinal,
    // dayOfMonthOrdinalParse: defaultDayOfMonthOrdinalParse,
    // relativeTime: defaultRelativeTime,
    months: defaultLocaleMonths,
    monthsShort: defaultLocaleMonthsShort,
    week: defaultLocaleWeek,
    weekdays: defaultLocaleWeekdays,
    weekdaysMin: defaultLocaleWeekdaysMin,
    weekdaysShort: defaultLocaleWeekdaysShort,
    meridiemParse: defaultLocaleMeridiemParse
};
//# sourceMappingURL=locale.defaults.js.map