import { addFormatToken } from '../format-functions';
import { getDate } from '../utils/date-getters';
addFormatToken('D', ['DD', 2], 'Do', function (date) {
    return getDate(date).toString(10);
});
//# sourceMappingURL=day-of-month.js.map