import { PipeTransform, EventEmitter, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { TranslateService, LangChangeEvent, TranslationChangeEvent, DefaultLangChangeEvent } from './translate.service';
export declare class TranslatePipe implements PipeTransform, OnDestroy {
    private translate;
    private _ref;
    value: string;
    lastKey: string;
    lastParams: any[];
    onTranslationChange: EventEmitter<TranslationChangeEvent>;
    onLangChange: EventEmitter<LangChangeEvent>;
    onDefaultLangChange: EventEmitter<DefaultLangChangeEvent>;
    constructor(translate: TranslateService, _ref: ChangeDetectorRef);
    updateValue(key: string, interpolateParams?: Object, translations?: any): void;
    transform(query: string, ...args: any[]): any;
    /**
     * Clean any existing subscription to change events
     * @private
     */
    _dispose(): void;
    ngOnDestroy(): void;
}
