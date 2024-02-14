// internal storage for locale config files
import { Locale } from './locale.class';
import { baseConfig } from './locale.defaults';
import { hasOwnProp, isObject, isUndefined } from '../utils/type-checks';
var locales = {};
var localeFamilies = {};
var globalLocale;
function chooseLocale(name) {
    return locales[name];
}
// returns locale data
export function getLocale(key) {
    if (!key) {
        return globalLocale;
    }
    return chooseLocale(key);
}
export function listLocales() {
    return Object.keys(locales);
}
export function mergeConfigs(parentConfig, childConfig) {
    var res = Object.assign({}, parentConfig);
    for (var childProp in childConfig) {
        if (!hasOwnProp(childConfig, childProp)) {
            continue;
        }
        if (isObject(parentConfig[childProp]) && isObject(childConfig[childProp])) {
            (res[childProp]) = {};
            Object.assign(res[childProp], parentConfig[childProp]);
            Object.assign(res[childProp], childConfig[childProp]);
        }
        else if (childConfig[childProp] != null) {
            (res[childProp]) = childConfig[childProp];
        }
        else {
            delete res[childProp];
        }
    }
    for (var parentProp in parentConfig) {
        if (hasOwnProp(parentConfig, parentProp) &&
            !hasOwnProp(childConfig, parentProp) &&
            isObject(parentConfig[parentProp])) {
            // make sure changes to properties don't modify parent config
            (res[parentProp]) = Object.assign({}, res[parentProp]);
        }
    }
    return res;
}
// This function will load locale and then set the global locale.  If
// no arguments are passed in, it will simply return the current global
// locale key.
export function getSetGlobalLocale(key, values) {
    var data;
    if (key) {
        data = isUndefined(values) ? getLocale(key) : defineLocale(key, values);
        if (data) {
            globalLocale = data;
        }
    }
    return globalLocale._abbr;
}
export function defineLocale(name, config) {
    if (config === null) {
        // useful for testing
        delete locales[name];
        return null;
    }
    config.abbr = name;
    locales[name] = new Locale(mergeConfigs(baseConfig, config));
    if (localeFamilies[name]) {
        localeFamilies[name].forEach(function (x) {
            defineLocale(x.name, x.config);
        });
    }
    // backwards compat for now: also set the locale
    // make sure we set the locale AFTER all child locales have been
    // created, so we won't end up with the child locale set.
    getSetGlobalLocale(name);
    return locales[name];
}
//# sourceMappingURL=locales.service.js.map