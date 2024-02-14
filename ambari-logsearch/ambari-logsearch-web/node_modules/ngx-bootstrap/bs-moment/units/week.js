import { addFormatToken } from '../format-functions';
import { weekOfYear } from './week-calendar-utils';
addFormatToken('w', ['ww', 2], 'wo', function (date, format, locale) {
    return getWeek(date, locale).toString(10);
});
addFormatToken('W', ['WW', 2], 'Wo', function (date) {
    return getISOWeek(date).toString(10);
});
export function getWeek(date, locale) {
    return locale.week(date);
}
export function getISOWeek(date) {
    return weekOfYear(date, 1, 4).week;
}
//# sourceMappingURL=week.js.map