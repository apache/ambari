import { addFormatToken } from '../format-functions';
import { startOf } from '../utils/start-end-of';
// FORMATTING
addFormatToken('DDD', ['DDDD', 3], 'DDDo', function (date) {
    return getDayOfYear(date).toString(10);
});
export function getDayOfYear(date) {
    var date1 = +startOf(date, 'day');
    var date2 = +startOf(date, 'year');
    var someDate = date1 - date2;
    var oneDay = 1000 * 60 * 60 * 24;
    return Math.round(someDate / oneDay) + 1;
}
export function _getDayOfYear(date) {
    var start = new Date(date.getFullYear(), 0, 0);
    var diff = date.getTime() - start.getTime();
    var oneDay = 1000 * 60 * 60 * 24;
    return Math.round(diff / oneDay) + 1;
}
//# sourceMappingURL=day-of-year.js.map