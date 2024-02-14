import { addFormatToken } from '../format-functions';
import { getSeconds } from '../utils/date-getters';
addFormatToken('s', ['ss', 2], null, function (date) {
    return getSeconds(date).toString(10);
});
//# sourceMappingURL=second.js.map