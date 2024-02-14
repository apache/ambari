import { ElementRef, AfterViewChecked, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Subscription } from 'rxjs/Subscription';
import { TranslateService } from './translate.service';
export declare class TranslateDirective implements AfterViewChecked, OnDestroy {
    private translateService;
    private element;
    private _ref;
    key: string;
    lastParams: any;
    currentParams: any;
    onLangChangeSub: Subscription;
    onDefaultLangChangeSub: Subscription;
    onTranslationChangeSub: Subscription;
    translate: string;
    translateParams: any;
    constructor(translateService: TranslateService, element: ElementRef, _ref: ChangeDetectorRef);
    ngAfterViewChecked(): void;
    checkNodes(forceUpdate?: boolean, translations?: any): void;
    updateValue(key: string, node: any, translations: any): void;
    getContent(node: any): string;
    setContent(node: any, content: string): void;
    ngOnDestroy(): void;
}
