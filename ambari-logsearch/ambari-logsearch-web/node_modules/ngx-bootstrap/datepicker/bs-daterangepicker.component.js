import { Component, EventEmitter, Input, Output, ElementRef, Renderer, ViewContainerRef } from '@angular/core';
import { BsDaterangepickerContainerComponent } from './themes/bs/bs-daterangepicker-container.component';
import { ComponentLoaderFactory } from '../component-loader/component-loader.factory';
import { BsDatepickerConfig } from './bs-datepicker.config';
var BsDaterangepickerComponent = (function () {
    function BsDaterangepickerComponent(_config, _elementRef, _renderer, _viewContainerRef, cis) {
        this._config = _config;
        /**
         * Placement of a daterangepicker. Accepts: "top", "bottom", "left", "right"
         */
        this.placement = 'bottom';
        /**
         * Specifies events that should trigger. Supports a space separated list of
         * event names.
         */
        this.triggers = 'click';
        /**
         * Close daterangepicker on outside click
         */
        this.outsideClick = true;
        /**
         * A selector specifying the element the daterangepicker should be appended to.
         * Currently only supports "body".
         */
        this.container = 'body';
        /**
         * Emits when daterangepicker value has been changed
         */
        this.bsValueChange = new EventEmitter();
        this._subs = [];
        this._datepicker = cis
            .createLoader(_elementRef, _viewContainerRef, _renderer);
        Object.assign(this, _config);
        this.onShown = this._datepicker.onShown;
        this.onHidden = this._datepicker.onHidden;
    }
    Object.defineProperty(BsDaterangepickerComponent.prototype, "isOpen", {
        /**
         * Returns whether or not the daterangepicker is currently being shown
         */
        get: function () {
            return this._datepicker.isShown;
        },
        set: function (value) {
            if (value) {
                this.show();
            }
            else {
                this.hide();
            }
        },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(BsDaterangepickerComponent.prototype, "bsValue", {
        /**
         * Initial value of daterangepicker
         */
        set: function (value) {
            if (this._bsValue === value) {
                return;
            }
            this._bsValue = value;
            this.bsValueChange.emit(value);
        },
        enumerable: true,
        configurable: true
    });
    BsDaterangepickerComponent.prototype.ngOnInit = function () {
        var _this = this;
        this._datepicker.listen({
            outsideClick: this.outsideClick,
            triggers: this.triggers,
            show: function () { return _this.show(); }
        });
    };
    BsDaterangepickerComponent.prototype.ngOnChanges = function (changes) {
        if (!this._datepickerRef || !this._datepickerRef.instance) {
            return;
        }
        if (changes.minDate) {
            this._datepickerRef.instance.minDate = this.minDate;
        }
        if (changes.maxDate) {
            this._datepickerRef.instance.maxDate = this.maxDate;
        }
        if (changes.isDisabled) {
            this._datepickerRef.instance.isDisabled = this.isDisabled;
        }
    };
    /**
     * Opens an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDaterangepickerComponent.prototype.show = function () {
        var _this = this;
        if (this._datepicker.isShown) {
            return;
        }
        this._config = Object.assign({}, this._config, { displayMonths: 2 }, this.bsConfig, {
            value: this._bsValue,
            isDisabled: this.isDisabled,
            minDate: this.minDate || this._config.minDate,
            maxDate: this.maxDate || this._config.maxDate
        });
        this._datepickerRef = this._datepicker
            .provide({ provide: BsDatepickerConfig, useValue: this._config })
            .attach(BsDaterangepickerContainerComponent)
            .to(this.container)
            .position({ attachment: this.placement })
            .show({ placement: this.placement });
        // if date changes from external source (model -> view)
        this._subs.push(this.bsValueChange.subscribe(function (value) {
            _this._datepickerRef.instance.value = value;
        }));
        // if date changes from picker (view -> model)
        this._subs.push(this._datepickerRef.instance
            .valueChange
            .filter(function (range) { return range && range[0] && !!range[1]; })
            .subscribe(function (value) {
            _this.bsValue = value;
            _this.hide();
        }));
    };
    /**
     * Closes an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDaterangepickerComponent.prototype.hide = function () {
        if (this.isOpen) {
            this._datepicker.hide();
        }
        for (var _i = 0, _a = this._subs; _i < _a.length; _i++) {
            var sub = _a[_i];
            sub.unsubscribe();
        }
    };
    /**
     * Toggles an element’s datepicker. This is considered a “manual” triggering of
     * the datepicker.
     */
    BsDaterangepickerComponent.prototype.toggle = function () {
        if (this.isOpen) {
            return this.hide();
        }
        this.show();
    };
    BsDaterangepickerComponent.prototype.ngOnDestroy = function () {
        this._datepicker.dispose();
    };
    BsDaterangepickerComponent.decorators = [
        { type: Component, args: [{
                    selector: 'bs-daterangepicker,[bsDaterangepicker]',
                    exportAs: 'bsDaterangepicker',
                    template: ' '
                },] },
    ];
    /** @nocollapse */
    BsDaterangepickerComponent.ctorParameters = function () { return [
        { type: BsDatepickerConfig, },
        { type: ElementRef, },
        { type: Renderer, },
        { type: ViewContainerRef, },
        { type: ComponentLoaderFactory, },
    ]; };
    BsDaterangepickerComponent.propDecorators = {
        'placement': [{ type: Input },],
        'triggers': [{ type: Input },],
        'outsideClick': [{ type: Input },],
        'container': [{ type: Input },],
        'isOpen': [{ type: Input },],
        'onShown': [{ type: Output },],
        'onHidden': [{ type: Output },],
        'bsValue': [{ type: Input },],
        'bsConfig': [{ type: Input },],
        'isDisabled': [{ type: Input },],
        'minDate': [{ type: Input },],
        'maxDate': [{ type: Input },],
        'bsValueChange': [{ type: Output },],
    };
    return BsDaterangepickerComponent;
}());
export { BsDaterangepickerComponent };
//# sourceMappingURL=bs-daterangepicker.component.js.map