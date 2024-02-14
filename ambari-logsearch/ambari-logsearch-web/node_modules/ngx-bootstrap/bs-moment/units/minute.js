import { addFormatToken } from '../format-functions';
import { getMinutes } from '../utils/date-getters';
addFormatToken('m', ['mm', 2], null, function (date) {
    return getMinutes(date).toString(10);
});
//# sourceMappingURL=minute.js.map