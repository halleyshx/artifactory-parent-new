/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.addon;

/**
 * @author Gidi Shabat
 */
public class FooterMessage {
    private String message;
    private String type;
    private String visibility;

    public FooterMessage(String message, FooterMessageType type,FooterMessageVisibility visibility) {
        this.message = message;
        this.type = type.name();
        this.visibility=visibility.name();
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getVisibility() {
        return visibility;
    }

    public static FooterMessage createWarning(String message,FooterMessageVisibility visibility) {
        return new FooterMessage(message,FooterMessageType.warning,visibility);
    }

    public static FooterMessage createinfo(String message,FooterMessageVisibility visibility) {
        return new FooterMessage(message,FooterMessageType.info,visibility);
    }

    public static FooterMessage createError(String message,FooterMessageVisibility visibility) {
        return new FooterMessage(message,FooterMessageType.error,visibility);
    }

    enum FooterMessageType{
        info,warning,error
    }

    public enum FooterMessageVisibility{
        admin, user, all;

        public boolean isVisible(boolean adminPermission, boolean userPermission) {
            if (adminPermission) {
                return true;
            } else if (userPermission) {
                return this == user || this == all;
            } else {
                return this == all;
            }
        }

        public static boolean isVisible(String visibilityName, boolean admin, boolean notAnonymous) {
            try {
                FooterMessageVisibility visibility = FooterMessageVisibility.valueOf(visibilityName);
                return visibility.isVisible(admin , notAnonymous);
            }catch (Exception e){
                return false;
            }
        }
    }
}
