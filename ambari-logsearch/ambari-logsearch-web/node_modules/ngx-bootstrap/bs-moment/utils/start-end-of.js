import { setDate, shiftDate } from './date-setters';
export function startOf(date, units) {
    var unit = getDateShift(units);
    return setDate(date, unit);
}
export function endOf(date, units) {
    var start = startOf(date, units);
    var shift = (_a = {}, _a[units] = 1, _a);
    var change = shiftDate(start, shift);
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
//# sourceMappingURL=start-end-of.js.map