import { Http } from "@angular/http";
import { TranslateLoader } from "@ngx-translate/core";
import "rxjs/add/operator/map";
export declare class TranslateHttpLoader implements TranslateLoader {
    private http;
    private prefix;
    private suffix;
    constructor(http: Http, prefix?: string, suffix?: string);
    /**
     * Gets the translations from the server
     * @param lang
     * @returns {any}
     */
    getTranslation(lang: string): any;
}
