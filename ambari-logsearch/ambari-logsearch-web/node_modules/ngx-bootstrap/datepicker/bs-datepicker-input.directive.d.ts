import { ElementRef, OnInit, Renderer } from '@angular/core';
import { ControlValueAccessor } from '@angular/forms';
import { BsDatepickerComponent } from './bs-datepicker.component';
import { BsDatepickerConfig } from './bs-datepicker.config';
export declare class BsDatepickerInputDirective implements OnInit, ControlValueAccessor {
    private _picker;
    private _config;
    private _renderer;
    private _elRef;
    private _onChange;
    private _onTouched;
    constructor(_picker: BsDatepickerComponent, _config: BsDatepickerConfig, _renderer: Renderer, _elRef: ElementRef);
    ngOnInit(): void;
    onChange(event: any): void;
    writeValue(value: Date | string): void;
    setDisabledState(isDisabled: boolean): void;
    registerOnChange(fn: (value: any) => any): void;
    registerOnTouched(fn: () => any): void;
    onBlur(): void;
    hide(): void;
}
