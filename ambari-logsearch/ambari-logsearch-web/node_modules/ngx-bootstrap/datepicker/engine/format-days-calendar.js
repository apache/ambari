import { formatDate } from '../../bs-moment/format';
import { getLocale } from '../../bs-moment/locale/locales.service';
export function formatDaysCalendar(daysCalendar, formatOptions, monthIndex) {
    return {
        month: daysCalendar.month,
        monthTitle: formatDate(daysCalendar.month, formatOptions.monthTitle, formatOptions.locale),
        yearTitle: formatDate(daysCalendar.month, formatOptions.yearTitle, formatOptions.locale),
        weekNumbers: getWeekNumbers(daysCalendar.daysMatrix, formatOptions.weekNumbers, formatOptions.locale),
        weekdays: getLocale(formatOptions.locale).weekdaysShort(),
        weeks: daysCalendar.daysMatrix
            .map(function (week, weekIndex) { return ({
            days: week.map(function (date, dayIndex) { return ({
                date: date,
                label: formatDate(date, formatOptions.dayLabel, formatOptions.locale),
                monthIndex: monthIndex, weekIndex: weekIndex, dayIndex: dayIndex
            }); })
        }); })
    };
}
export function getWeekNumbers(daysMatrix, format, locale) {
    return daysMatrix.map(function (days) { return days[0]
        ? formatDate(days[0], format, locale)
        : ''; });
}
//# sourceMappingURL=format-days-calendar.js.map