import { Directive, ElementRef, forwardRef, Host, Renderer } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { BsDatepickerComponent } from './bs-datepicker.component';
import { formatDate } from '../bs-moment/format';
import { BsDatepickerConfig } from './bs-datepicker.config';
import { getLocale } from '../bs-moment/locale/locales.service';
var BS_DATEPICKER_VALUE_ACCESSOR = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(function () { return BsDatepickerInputDirective; }),
    multi: true
};
var BsDatepickerInputDirective = (function () {
    function BsDatepickerInputDirective(_picker, _config, _renderer, _elRef) {
        this._picker = _picker;
        this._config = _config;
        this._renderer = _renderer;
        this._elRef = _elRef;
        this._onChange = Function.prototype;
        this._onTouched = Function.prototype;
    }
    BsDatepickerInputDirective.prototype.ngOnInit = function () {
        var _this = this;
        this._picker.bsValueChange.subscribe(function (v) {
            _this._renderer.setElementProperty(_this._elRef.nativeElement, 'value', formatDate(v, _this._picker._config.dateInputFormat, _this._picker._config.locale) || '');
            _this._onChange(v);
        });
    };
    BsDatepickerInputDirective.prototype.onChange = function (event) {
        this.writeValue(event.target.value);
        this._onTouched();
    };
    BsDatepickerInputDirective.prototype.writeValue = function (value) {
        if (!value) {
            this._picker.bsValue = null;
        }
        var _locale = getLocale(this._picker._config.locale);
        if (!_locale) {
            throw new Error("Locale \"" + this._picker._config.locale + "\" is not defined, please add it with \"defineLocale(...)\"");
        }
        if (typeof value === 'string') {
            var date = new Date(_locale.preparse(value));
            this._picker.bsValue = isNaN(date.valueOf()) ? null : date;
        }
        if (value instanceof Date) {
            this._picker.bsValue = value;
        }
    };
    BsDatepickerInputDirective.prototype.setDisabledState = function (isDisabled) {
        this._picker.isDisabled = isDisabled;
        this._renderer.setElementAttribute(this._elRef.nativeElement, 'disabled', 'disabled');
    };
    BsDatepickerInputDirective.prototype.registerOnChange = function (fn) { this._onChange = fn; };
    BsDatepickerInputDirective.prototype.registerOnTouched = function (fn) { this._onTouched = fn; };
    BsDatepickerInputDirective.prototype.onBlur = function () { this._onTouched(); };
    BsDatepickerInputDirective.prototype.hide = function () {
        this._picker.hide();
    };
    BsDatepickerInputDirective.decorators = [
        { type: Directive, args: [{
                    selector: "input[bsDatepicker]",
                    host: {
                        '(change)': 'onChange($event)',
                        '(keyup.esc)': 'hide()',
                        '(blur)': 'onBlur()'
                    },
                    providers: [BS_DATEPICKER_VALUE_ACCESSOR]
                },] },
    ];
    /** @nocollapse */
    BsDatepickerInputDirective.ctorParameters = function () { return [
        { type: BsDatepickerComponent, decorators: [{ type: Host },] },
        { type: BsDatepickerConfig, },
        { type: Renderer, },
        { type: ElementRef, },
    ]; };
    return BsDatepickerInputDirective;
}());
export { BsDatepickerInputDirective };
//# sourceMappingURL=bs-datepicker-input.directive.js.map