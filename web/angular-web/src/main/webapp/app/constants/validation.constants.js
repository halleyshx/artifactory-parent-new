export default {
    "adminGeneral": {
        "min": "Value must be between 0 and 2,147,483,647",
        "max": "Value must be between 0 and 2,147,483,647",
        "dateFormatExpression": "Invalid date format"
    },
    "folderDownload": {
        "min": "Number of downloads must be bigger than 0"
    },
    "adminBackup": {
        "name": "Invalid backup name",
        "xmlName": "Invalid backup name"
    },
    "adminMail": {
        "min": "Port must be between 1 and 65535",
        "max": "Port must be between 1 and 65535"
    },
    "proxies": {
        "min": "Port must be between 1 and 65535",
        "max": "Port must be between 1 and 65535",
    },
    "users": {
        "validator": "Passwords do not match",
        "minlength": "Password must contain at least 4 characters",
        "maxlength": "Username cannot be longer than 64 characters",
        "invalidUsername": "Username cannot contain uppercase letters"
    },
    "groups": {
        "maxlength": "Group name cannot be longer than 64 characters"
    },
    "maintenance": {
        "min": "Value must be between 0 and 99",
        "max": "Value must be between 0 and 99"
    },
    "crowd": {
        "min": "Value must be between 0 and 9999999999999",
        "max": "Value must be between 0 and 9999999999999",
        "url": "Invalid URL"
    },
    "ldapSettings": {
        "ldapUrl": "Invalid LDAP URL"
    },
    "gridFilter": {
        "maxlength": "Filter field exceed max length"
    },
    "properties": {
        "validCharacters": "Name cannot include the following characters * < > ~ ! @ # $ % ^ & ( ) + = - { } [ ] ; , ` / \\",
        "predefinedValues": "Predefined values for the selected type cannot be empty",
        "name": "Name must start with a letter and cannot contain spaces or special characters",
        "xmlName": "Name must start with a letter and cannot contain spaces or special characters",
        "notPrefixedWithNumeric": "Name must start with a letter and cannot contain spaces or special characters"
    },
    "repoLayouts": {
        "pathPattern": "Pattern must contain at-least the following tokens 'module', 'baseRev' and 'org' or 'orgPath'"
    },
    "bintray": {
        "required": "API Key / Username cannot be empty"
    },
    "licenses": {
        "validateLicense": "License name contains illegal characters"
    },
    "propertySet": {
        "name": "Property set name must start with a letter and contain only letters, digits, dashes or underscores",
        "xmlName": "Property set name must start with a letter and contain only letters, digits, dashes or underscores"
    },
    "reverseProxy": {
        "port": "Port is not available"
    },
    "distRepo": {
        "existRuleName": "Rule name already in use",
        "length": "Rule name must contain between 1-50 characters",
        "reservedRepoName": "Repository name is reserved and cannot be used",   // TODO: message here [Changed to repository name, was rule name]
        "illegalCharacters": "Illegal character. Use only lowercase letter, numbers or the following characters - _ .", // TODO: message here
        "illegalProductName": "Product name can only contain lowercase letter, numbers and the following characters . - _ :",   // TODO: message here
        "firstLetterValidation": "Repository name must start with letter or number",     // TODO: message here
        "dockerVagrantValidate" : "Repository name cannot contain uppercase."
    }
};