import { getDayOfWeek, isFirstDayOfWeek } from '../../bs-moment/utils/date-getters';
import { shiftDate } from '../../bs-moment/utils/date-setters';
import { isSameOrAfter, isSameOrBefore } from '../../bs-moment/utils/date-compare';
import { endOf, startOf } from '../../bs-moment/utils/start-end-of';
export function getStartingDayOfCalendar(date, options) {
    if (isFirstDayOfWeek(date, options.firstDayOfWeek)) {
        return date;
    }
    var weekDay = getDayOfWeek(date);
    return shiftDate(date, { day: -weekDay });
}
export function isMonthDisabled(date, min, max) {
    var minBound = min && isSameOrBefore(endOf(date, 'month'), min, 'day');
    var maxBound = max && isSameOrAfter(startOf(date, 'month'), max, 'day');
    return minBound || maxBound;
}
export function isYearDisabled(date, min, max) {
    var minBound = min && isSameOrBefore(endOf(date, 'year'), min, 'day');
    var maxBound = max && isSameOrAfter(startOf(date, 'year'), max, 'day');
    return minBound || maxBound;
}
//# sourceMappingURL=bs-calendar-utils.js.map