export class HaLicensesDao {
    constructor(RESOURCE, ArtifactoryDaoFactory) {
        return ArtifactoryDaoFactory()
                .setPath(RESOURCE.MANAGE_HA_LICENSES + "/:action")
                .setCustomActions({
                    'add': {
                        method: 'POST',
                        notifications: true, // This is for the 'toaster' to catch the error and display the message
                        params: {action: 'add'},
                    },
                    'getLicenses': {
                        method: 'GET',
                        notifications: true,
                        params: {action: 'details'},
                    },
                    'replace': {
                        method: 'POST',
                        notifications: true,
                        params: {action: 'replace'},
                    },
                    'delete': {
                        method: 'POST',
                        notifications: true,
                        params: {action: 'remove'}
                    }
                })
                .getInstance();
    }
}