import { addFormatToken } from '../format-functions';
import { isLeapYear } from './year';
import { mod } from '../utils';
import { getMonth } from '../utils/date-getters';
export function daysInMonth(year, month) {
    if (isNaN(year) || isNaN(month)) {
        return NaN;
    }
    var modMonth = mod(month, 12);
    year += (month - modMonth) / 12;
    return modMonth === 1 ? (isLeapYear(year) ? 29 : 28) : (31 - modMonth % 7 % 2);
}
// FORMATTING
addFormatToken('M', ['MM', 2], 'Mo', function (date, format) {
    return (getMonth(date) + 1).toString();
});
addFormatToken('MMM', null, null, function (date, format, locale) {
    return locale.monthsShort(date, format);
});
addFormatToken('MMMM', null, null, function (date, format, locale) {
    return locale.months(date, format);
});
//# sourceMappingURL=month.js.map