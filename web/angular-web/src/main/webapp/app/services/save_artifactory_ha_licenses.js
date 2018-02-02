/**
 * Created by tomere on 12/23/2016.
 */

export class SaveArtifactoryHaLicenses {
    constructor(HaLicensesDao) {
        this.haLicensesDao = HaLicensesDao;
    }

    splitText(text) {
        // let splitted = this.splitLicensesTextByWrappingStrings(text);
        // return splitted.length !== 0 ? splitted : this.splitLicensesTextByDelimiters(text);
        let cleanText = this.removeComments(text);
        return this.splitLicensesTextByDelimiters(cleanText);
    }

    removeComments(text){
        return text.replace(/#+((?:.)+?)*/g,'');
    }

    splitLicensesTextByDelimiters(text) {
        let splittedText = text.split(/[,;]+|\n{2,}|(?:\r\n){2,}/g);
        if (splittedText[splittedText.length - 1] == "") {
            splittedText.pop();
        }
        return splittedText;
    }

    toLicensesObjArray(splittedText, key) {
        let res = [];
        for (let i in splittedText) {
            let textBlock = {};
            textBlock[key] = splittedText[i];
            res.push(textBlock);
        }
        return res;
    }

    toLicensesJson(rawText) {
        let splittedText = this.splitText(rawText),
            licensesObjArray = this.toLicensesObjArray(splittedText, "licenseKey"),
            licensesJson = {
                'licenses': licensesObjArray
            };

        return licensesJson;
    }

    saveLicenses(options,rawText) {
        let licensesJson = this.toLicensesJson(rawText);
        return this.haLicensesDao.add(options,licensesJson).$promise;
    }

/*
    splitLicensesTextByWrappingStrings(text) {
        let splittedText = [];

        if (text.indexOf('#Start License Key #') === 0) {
            return splittedText;
        }

        let pattern = /#Start License Key #\d+((?:.|\n|\r\n)+?)#End License Key #\d+/g;
        let match;
        while ((match = pattern.exec(text)) !== null) {
            splittedText.push(match[1]);
        }

        return splittedText;
    }*/

}