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

package org.artifactory.ui.utils;

import org.jfrog.build.api.Build;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Chen Keinan
 */
public class DateUtils {


    /**
     * return build duration in human
     * @param durationMill - build duration in mill
     * @return duration in human
     */
    public static String getDuration(long durationMill){
        int minutes = (int) ((durationMill / (1000*60)) % 60);
        int hours   = (int) ((durationMill / (1000*60*60)) % 24);

        if (hours > 0){
            BigDecimal duration = new BigDecimal(durationMill / (1000.0*60.0*60.0)).setScale(1, RoundingMode.UP);
            return duration.toString()+" hours";
        }
        else if (minutes > 0 ) {
            BigDecimal duration = new BigDecimal(durationMill / (1000.0*60.0)).setScale(1, RoundingMode.UP);
            return  duration.toString() + " minutes";
        }
        else {
            BigDecimal duration = new BigDecimal (durationMill/1000.0).setScale(1,RoundingMode.UP);
             return  duration.toString() + " seconds";
        }
    }

    /**
     * format Build date
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

    /**
     * build date in string format to long
     */
    public static long toBuildDate(String date) throws ParseException {
        SimpleDateFormat df2 = new SimpleDateFormat(Build.STARTED_FORMAT);
        Date parse = df2.parse(date);
        return parse.getTime();
    }
}
