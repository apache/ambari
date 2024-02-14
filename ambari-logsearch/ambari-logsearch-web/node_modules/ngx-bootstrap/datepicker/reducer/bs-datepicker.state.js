import { defaultMonthOptions } from './_defaults';
import { BsDatepickerConfig } from '../bs-datepicker.config';
var BsDatepickerState = (function () {
    function BsDatepickerState() {
    }
    return BsDatepickerState;
}());
export { BsDatepickerState };
export var initialDatepickerState = Object.assign(new BsDatepickerConfig(), {
    view: { date: new Date(), mode: 'day' },
    selectedRange: [],
    monthViewOptions: defaultMonthOptions
});
//# sourceMappingURL=bs-datepicker.state.js.map