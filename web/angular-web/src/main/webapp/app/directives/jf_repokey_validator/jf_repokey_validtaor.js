import fieldsValuesDictionary from "../../constants/field_options.constats";

export function jfRepokeyValidator(RepositoriesDao, $q, $timeout) {
    return {
        restrict: 'A',
        require: 'ngModel',
        scope:{
            controller:'=jfRepokeyValidator'
        },
        link: function jfRepokeyValidatorLink(scope, element, attrs, ngModel) {


            ngModel.$asyncValidators.repoKeyValidator = validateRepoKey;

            function validateRepoKey(modelValue, viewValue) {
                let repoKey = modelValue || viewValue;
                let remote = scope.controller.repoType == fieldsValuesDictionary.REPO_TYPE.REMOTE;

                if (!repoKey) {
                    return $q.when();
                }

                return RepositoriesDao.repoKeyValidator({repoKey, remote}).$promise
                    .then(function (result) {
                        if (result.error) {
                            scope.controller.repoKeyValidatorMessage = result.error;
                            return $q.reject();
                        }
                        else if (scope.controller.repoInfo.isType('docker') && repoKey.toLowerCase() !== repoKey) {
                            scope.controller.repoKeyValidatorMessage = 'Docker repository key must be in lowercase';
                            return $q.reject();
                        }
                        else if (scope.controller.repoInfo.type === 'localRepoConfig' && repoKey.toLowerCase().endsWith('-cache')) {
                            scope.controller.repoKeyValidatorMessage = 'Cannot create local repository with "-cache" ending';
                            return $q.reject();
                        }
                        return true;
                    });
            }
        }
    }
}