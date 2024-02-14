"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var path = require("path");
var fs = require("fs");
var FileUtils_1 = require("./FileUtils");
var LicenseWebpackPluginError_1 = require("./LicenseWebpackPluginError");
var ErrorMessage_1 = require("./ErrorMessage");
var LicenseExtractor = (function () {
    function LicenseExtractor(context, options, errors) {
        this.context = context;
        this.options = options;
        this.errors = errors;
        this.moduleCache = {};
        this.modulePrefix = path.join(this.context, FileUtils_1.FileUtils.MODULE_DIR);
    }
    // returns true if the package is included as part of license report
    LicenseExtractor.prototype.parsePackage = function (packageName) {
        if (this.moduleCache[packageName]) {
            return true;
        }
        var packageJson = this.readPackageJson(packageName);
        var licenseName = this.getLicenseName(packageJson);
        if (licenseName === LicenseExtractor.UNKNOWN_LICENSE &&
            !this.options.includePackagesWithoutLicense) {
            return false;
        }
        if (licenseName !== LicenseExtractor.UNKNOWN_LICENSE &&
            !this.options.pattern.test(licenseName)) {
            return false;
        }
        if (licenseName !== LicenseExtractor.UNKNOWN_LICENSE &&
            this.options.unacceptablePattern &&
            this.options.unacceptablePattern.test(licenseName)) {
            var error = new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.UNNACEPTABLE_LICENSE, packageName, licenseName);
            if (this.options.abortOnUnacceptableLicense) {
                throw error;
            }
            else {
                this.errors.push(error);
            }
        }
        var licenseText = this.getLicenseText(packageJson, licenseName);
        var moduleCacheEntry = {
            packageJson: packageJson,
            license: {
                name: licenseName,
                text: licenseText
            }
        };
        this.moduleCache[packageName] = moduleCacheEntry;
        return true;
    };
    LicenseExtractor.prototype.getCachedPackage = function (packageName) {
        return this.moduleCache[packageName];
    };
    LicenseExtractor.prototype.getLicenseName = function (packageJson) {
        var overriddenLicense = this.options.licenseTypeOverrides &&
            this.options.licenseTypeOverrides[packageJson.name];
        if (overriddenLicense) {
            return overriddenLicense;
        }
        var license = packageJson.license;
        // add support license like `{type: '...', url: '...'}`
        if (license && license.type) {
            license = license.type;
        }
        // add support licenses like `[{type: '...', url: '...'}]`
        if (!license && packageJson.licenses) {
            var licenses = packageJson.licenses;
            if (Array.isArray(licenses) && licenses[0].type) {
                license = licenses[0].type;
                if (licenses.length > 1) {
                    this.errors.push(new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.MULTIPLE_LICENSE_AMBIGUITY, packageJson.name, license));
                }
            }
        }
        if (!license) {
            license = LicenseExtractor.UNKNOWN_LICENSE;
        }
        return license;
    };
    LicenseExtractor.prototype.getLicenseFilename = function (packageJson, licenseName) {
        var filename;
        var packageName = packageJson.name;
        var overrideFile = this.options.licenseFileOverrides &&
            this.options.licenseFileOverrides[packageName];
        if (overrideFile) {
            if (!FileUtils_1.FileUtils.isThere(overrideFile)) {
                this.errors.push(new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.NO_LICENSE_OVERRIDE_FILE_FOUND, packageName, overrideFile));
            }
            return overrideFile;
        }
        for (var i = 0; i < this.options.licenseFilenames.length; i = i + 1) {
            var licenseFile = path.join(this.modulePrefix, packageName, this.options.licenseFilenames[i]);
            if (FileUtils_1.FileUtils.isThere(licenseFile)) {
                filename = licenseFile;
                break;
            }
        }
        if (!filename && this.options.licenseTemplateDir) {
            var templateFilename = path.join(this.options.licenseTemplateDir, licenseName + '.txt');
            if (FileUtils_1.FileUtils.isThere(templateFilename)) {
                filename = templateFilename;
            }
        }
        return filename;
    };
    LicenseExtractor.prototype.getLicenseText = function (packageJson, licenseName) {
        if (licenseName === LicenseExtractor.UNKNOWN_LICENSE) {
            return '';
        }
        var licenseFilename = this.getLicenseFilename(packageJson, licenseName);
        if (!licenseFilename) {
            this.errors.push(new LicenseWebpackPluginError_1.LicenseWebpackPluginError(ErrorMessage_1.ErrorMessage.NO_LICENSE_FILE, packageJson.name, licenseName));
            return licenseName;
        }
        return fs.readFileSync(licenseFilename, 'utf8').trim();
    };
    LicenseExtractor.prototype.readPackageJson = function (packageName) {
        var pathName = path.join(this.modulePrefix, packageName, 'package.json');
        var file = fs.readFileSync(pathName, 'utf8');
        return JSON.parse(file);
    };
    LicenseExtractor.UNKNOWN_LICENSE = 'Unknown license';
    return LicenseExtractor;
}());
exports.LicenseExtractor = LicenseExtractor;
