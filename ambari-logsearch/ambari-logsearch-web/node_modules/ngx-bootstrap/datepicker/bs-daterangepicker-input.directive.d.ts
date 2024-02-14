import { ElementRef, OnInit, Renderer } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { BsDatepickerConfig } from './bs-datepicker.config';
import { BsDaterangepickerComponent } from './bs-daterangepicker.component';
export declare class BsDaterangepickerInputDirective implements OnInit, ControlValueAccessor {
    private _picker;
    private _config;
    private _renderer;
    private _elRef;
    private _onChange;
    private _onTouched;
    constructor(_picker: BsDaterangepickerComponent, _config: BsDatepickerConfig, _renderer: Renderer, _elRef: ElementRef);
    ngOnInit(): void;
    onChange(event: any): void;
    writeValue(value: Date[] | string): void;
    setDisabledState(isDisabled: boolean): void;
    registerOnChange(fn: (value: any) => any): void;
    registerOnTouched(fn: () => any): void;
    onBlur(): void;
    hide(): void;
}
