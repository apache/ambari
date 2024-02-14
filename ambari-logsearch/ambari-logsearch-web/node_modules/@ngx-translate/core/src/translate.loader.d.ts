import { Observable } from "rxjs/Observable";
export declare abstract class TranslateLoader {
    abstract getTranslation(lang: string): Observable<any>;
}
/**
 * This loader is just a placeholder that does nothing, in case you don't need a loader at all
 */
export declare class TranslateFakeLoader extends TranslateLoader {
    getTranslation(lang: string): Observable<any>;
}
