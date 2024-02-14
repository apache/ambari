import { addFormatToken } from '../format-functions';
import { getDayOfWeek } from '../utils/date-getters';
// FORMATTING
addFormatToken('d', null, 'do', function (date) {
    return getDayOfWeek(date).toString(10);
});
addFormatToken('dd', null, null, function (date, format, locale) {
    return locale.weekdaysShort(date);
});
addFormatToken('ddd', null, null, function (date, format, locale) {
    return locale.weekdaysMin(date);
});
addFormatToken('dddd', null, null, function (date, format, locale) {
    return locale.weekdays(date, format);
});
addFormatToken('e', null, null, function (date) {
    return getDayOfWeek(date).toString(10);
});
addFormatToken('E', null, null, function (date) {
    return getISODayOfWeek(date).toString(10);
});
export function getLocaleDayOfWeek(date, locale) {
    return (getDayOfWeek(date) + 7 - locale.firstDayOfWeek()) % 7;
}
export function getISODayOfWeek(date) {
    return getDayOfWeek(date) || 7;
}
//# sourceMappingURL=day-of-week.js.map