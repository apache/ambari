import { EventEmitter } from "@angular/core";
import { DefaultLangChangeEvent, LangChangeEvent, TranslationChangeEvent } from "./translate.service";
export declare class TranslateStore {
    /**
     * The default lang to fallback when translations are missing on the current lang
     */
    defaultLang: string;
    /**
     * The lang currently used
     * @type {string}
     */
    currentLang: string;
    /**
     * a list of translations per lang
     * @type {{}}
     */
    translations: any;
    /**
     * an array of langs
     * @type {Array}
     */
    langs: Array<string>;
    /**
     * An EventEmitter to listen to translation change events
     * onTranslationChange.subscribe((params: TranslationChangeEvent) => {
     *     // do something
     * });
     * @type {EventEmitter<TranslationChangeEvent>}
     */
    onTranslationChange: EventEmitter<TranslationChangeEvent>;
    /**
     * An EventEmitter to listen to lang change events
     * onLangChange.subscribe((params: LangChangeEvent) => {
     *     // do something
     * });
     * @type {EventEmitter<LangChangeEvent>}
     */
    onLangChange: EventEmitter<LangChangeEvent>;
    /**
     * An EventEmitter to listen to default lang change events
     * onDefaultLangChange.subscribe((params: DefaultLangChangeEvent) => {
     *     // do something
     * });
     * @type {EventEmitter<DefaultLangChangeEvent>}
     */
    onDefaultLangChange: EventEmitter<DefaultLangChangeEvent>;
}
