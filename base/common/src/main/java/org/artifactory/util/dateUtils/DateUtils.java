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

package org.artifactory.util.dateUtils;

import org.jfrog.build.api.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Chen Keinan
 */
public class DateUtils {

    /**
     * format Build date
     *
     * @param time - long date
     * @return
     * @throws ParseException
     */
    public static String formatBuildDate(long time) throws ParseException {
        Date date = new Date(time);
        SimpleDateFormat df2 = new SimpleDateFormat(Build.STARTED_FORMAT);
        String dateText = df2.format(date);
        return dateText;
    }
}
