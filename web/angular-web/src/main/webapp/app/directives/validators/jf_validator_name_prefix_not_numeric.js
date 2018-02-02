/**
 * Validates an input to not be a numeric value
 */
export function jfValidatorNamePrefixNotNumeric() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: (scope, element, attrs, ngModel) => {
            ngModel.$validators.notPrefixedWithNumeric = (modelValue, viewValue) => {
                let value = modelValue || viewValue;
                let firstChar = value.charAt(0);
                let firstCharIsNotNumber = !(firstChar >='0' && firstChar <='9');
                let notNumericValue = isNaN(value) && value !='';
                return notNumericValue && firstCharIsNotNumber;
            };
        }
    }
}