"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const semver = require("semver");
const config_1 = require("../config");
const config_source_1 = require("./config_source");
class ChromeXml extends config_source_1.XmlConfigSource {
    constructor() {
        super('chrome', config_1.Config.cdnUrls()['chrome']);
    }
    getUrl(version) {
        if (version === 'latest') {
            return this.getLatestChromeDriverVersion();
        }
        else {
            return this.getSpecificChromeDriverVersion(version);
        }
    }
    /**
     * Get a list of chrome drivers paths available for the configuration OS type and architecture.
     */
    getVersionList() {
        return this.getXml().then(xml => {
            let versionPaths = [];
            let osType = this.getOsTypeName();
            for (let content of xml.ListBucketResult.Contents) {
                let contentKey = content.Key[0];
                // Filter for 32-bit devices, make sure x64 is not an option
                if (this.osarch === 'x64' || !contentKey.includes('64')) {
                    // Filter for only the osType
                    if (contentKey.includes(osType)) {
                        versionPaths.push(contentKey);
                    }
                }
            }
            return versionPaths;
        });
    }
    /**
     * Helper method, gets the ostype and gets the name used by the XML
     */
    getOsTypeName() {
        // Get the os type name.
        if (this.ostype === 'Darwin') {
            return 'mac';
        }
        else if (this.ostype === 'Windows_NT') {
            return 'win';
        }
        else {
            return 'linux';
        }
    }
    /**
     * Gets the latest item from the XML.
     */
    getLatestChromeDriverVersion() {
        return this.getVersionList().then(list => {
            let chromedriverVersion = null;
            let latest = '';
            let latestVersion = '';
            for (let item of list) {
                // Get a semantic version
                let version = item.split('/')[0];
                if (semver.valid(version) == null) {
                    version += '.0';
                    if (semver.valid(version)) {
                        // First time: use the version found.
                        if (chromedriverVersion == null) {
                            chromedriverVersion = version;
                            latest = item;
                            latestVersion = item.split('/')[0];
                        }
                        else if (semver.gt(version, chromedriverVersion)) {
                            // After the first time, make sure the semantic version is greater.
                            chromedriverVersion = version;
                            latest = item;
                            latestVersion = item.split('/')[0];
                        }
                        else if (version === chromedriverVersion) {
                            // If the semantic version is the same, check os arch.
                            // For 64-bit systems, prefer the 64-bit version.
                            if (this.osarch === 'x64') {
                                if (item.includes(this.getOsTypeName() + '64')) {
                                    latest = item;
                                }
                            }
                        }
                    }
                }
            }
            return { url: config_1.Config.cdnUrls().chrome + latest, version: latestVersion };
        });
    }
    /**
     * Gets a specific item from the XML.
     */
    getSpecificChromeDriverVersion(inputVersion) {
        return this.getVersionList().then(list => {
            let itemFound = '';
            let specificVersion = semver.valid(inputVersion) ? inputVersion : inputVersion + '.0';
            for (let item of list) {
                // Get a semantic version.
                let version = item.split('/')[0];
                if (semver.valid(version) == null) {
                    version += '.0';
                    if (semver.valid(version)) {
                        // Check to see if the specified version matches.
                        if (version === specificVersion) {
                            // When item found is null, check the os arch
                            // 64-bit version works OR not 64-bit version and the path does not have '64'
                            if (itemFound == '') {
                                if (this.osarch === 'x64' ||
                                    (this.osarch !== 'x64' && !item.includes(this.getOsTypeName() + '64'))) {
                                    itemFound = item;
                                }
                            }
                            else if (this.osarch === 'x64') {
                                if (item.includes(this.getOsTypeName() + '64')) {
                                    itemFound = item;
                                }
                            }
                        }
                    }
                }
            }
            if (itemFound == '') {
                return { url: '', version: inputVersion };
            }
            else {
                return { url: config_1.Config.cdnUrls().chrome + itemFound, version: inputVersion };
            }
        });
    }
}
exports.ChromeXml = ChromeXml;
//# sourceMappingURL=chrome_xml.js.map