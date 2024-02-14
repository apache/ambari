import { addFormatToken } from '../format-functions';
import { getFullYear } from '../utils/date-getters';
// FORMATTING
function getYear(date) {
    return getFullYear(date).toString();
}
addFormatToken('Y', null, null, function (date) {
    var y = getFullYear(date);
    return y <= 9999 ? '' + y : '+' + y;
});
addFormatToken(null, ['YY', 2], null, function (date) {
    return (getFullYear(date) % 100).toString(10);
});
addFormatToken(null, ['YYYY', 4], null, getYear);
addFormatToken(null, ['YYYYY', 5], null, getYear);
addFormatToken(null, ['YYYYYY', 6, true], null, getYear);
export function daysInYear(year) {
    return isLeapYear(year) ? 366 : 365;
}
export function isLeapYear(year) {
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}
//# sourceMappingURL=year.js.map