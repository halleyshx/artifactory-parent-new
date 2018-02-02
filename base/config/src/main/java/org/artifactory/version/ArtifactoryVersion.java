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

package org.artifactory.version;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: freds Date: May 29, 2008 Time: 10:15:59 AM
 */
public enum ArtifactoryVersion {
    v122rc0("1.2.2-rc0", 804),
    v122rc1("1.2.2-rc1", 819),
    v122rc2("1.2.2-rc2", 826),
    v122("1.2.2", 836),
    v125rc0("1.2.5-rc0", 970),
    v125rc1("1.2.5-rc1", 1015),
    v125rc2("1.2.5-rc2", 1082),
    v125rc3("1.2.5-rc3", 1087),
    v125rc4("1.2.5-rc4", 1104),
    v125rc5("1.2.5-rc5", 1115),
    v125rc6("1.2.5-rc6", 1136),
    v125("1.2.5", 1154),
    v125u1("1.2.5u1", 1174),
    v130beta1("1.3.0-beta-1", 1501),
    v130beta2("1.3.0-beta-2", 1509),
    v130beta3("1.3.0-beta-3", 1992),
    v130beta4("1.3.0-beta-4", 2065),
    v130beta5("1.3.0-beta-5", 2282),
    v130beta6("1.3.0-beta-6", 2862),
    v130beta61("1.3.0-beta-6.1", 2897),
    v130rc1("1.3.0-rc-1", 3148),
    v130rc2("1.3.0-rc-2", 3392),
    v200("2.0.0", 3498),
    v201("2.0.1", 3768),
    v202("2.0.2", 3947),
    v203("2.0.3", 4468),
    v204("2.0.4", 4781),
    v205("2.0.5", 4903),
    v206("2.0.6", 5625),
    v207("2.0.7", 7453),
    v208("2.0.8", 7829),
    v210("2.1.0", 8350),
    v211("2.1.1", 8514),
    v212("2.1.2", 8706),
    v213("2.1.3", 9204),
    v220("2.2.0", 9932),
    v221("2.2.1", 10024),
    v222("2.2.2", 10427),
    v223("2.2.3", 10588),
    v224("2.2.4", 11256),
    v225("2.2.5", 11524),
    v230("2.3.0", 12450),
    v231("2.3.1", 12714),
    v232("2.3.2", 13005),
    v233("2.3.3", 13011),
    v2331("2.3.3.1", 13012),
    v234("2.3.4", 13017),
    v2341("2.3.4.1", 13021),
    v240("2.4.0", 13048),
    v241("2.4.1", 13050),
    v242("2.4.2", 13059),
    v250("2.5.0", 13086),
    v251("2.5.1", 13089),
    v2511("2.5.1.1", 13098),
    v252("2.5.2", 13110),
    v260("2.6.0", 13119),
    v261("2.6.1", 13124),
    v262("2.6.2", 13147),
    v263("2.6.3", 13148),
    v264("2.6.4", 13153),
    v265("2.6.5", 13174),
    v266("2.6.6", 13183),
    v267("2.6.7", 13201),
    v2671("2.6.7.1", 13243),
    v300("3.0.0", 30001),
    v301("3.0.1", 30008),
    v302("3.0.2", 30017),
    v3021("3.0.2.1", 30034),
    v303("3.0.3", 30044),
    v304("3.0.4", 30058),
    v310("3.1.0", 30062),
    v311("3.1.1", 30072),
    v3111("3.1.1.1", 30080),
    v320("3.2.0", 30088),
    v321("3.2.1", 30093),
    v3211("3.2.1.1", 30094),
    v322("3.2.2", 30097),
    v330("3.3.0", 30104),
    v3301("3.3.0.1", 30106),
    v331("3.3.1", 30120),
    v340("3.4.0", 30125),
    v3401("3.4.0.1", 30126),
    v341("3.4.1", 30130),
    v342("3.4.2", 30140),
    v350("3.5.0", 30150),
    v351("3.5.1", 30152),
    v352("3.5.2", 30159),
    v3521("3.5.2.1", 30160),
    v353("3.5.3", 30172),
    v360("3.6.0", 30178),
    v370("3.7.0", 30185),
    v380("3.8.0", 30190),
    v390("3.9.0", 30199),
    v391("3.9.1", 30200),
    v392("3.9.2", 30204),
    v393("3.9.3", 30224),
    v394("3.9.4", 30226),
    v395("3.9.5", 30242),
    v400("4.0.0", 40005),
    v401("4.0.1", 40008),
    v402("4.0.2", 40009),
    v410("4.1.0", 40011),
    v411("4.1.1", 40016),
    v412("4.1.2", 40017),
    v413("4.1.3", 40020),
    v420("4.2.0", 40030),
    v421("4.2.1", 40045),
    v422("4.2.2", 40049),
    v430("4.3.0", 40057),
    v431("4.3.1", 40062),
    v432("4.3.2", 40063),
    v433("4.3.3", 40071),
    v440("4.4.0", 40080),
    v441("4.4.1", 40089),
    v442("4.4.2", 40093),
    v443("4.4.3", 40110),
    v450("4.5.0", 40115),
    v451("4.5.1", 40117),
    v452("4.5.2", 40121),
    v460("4.6.0", 40135),
    v461("4.6.1", 40143),
    v470("4.7.0", 40155),
    v471("4.7.1", 40159),
    v472("4.7.2", 40162),
    v473("4.7.3", 40167),
    v474("4.7.4", 40169),
    v475("4.7.5", 40176),
    v476("4.7.6", 40195),
    v477("4.7.7", 40199),
    v478("4.7.8", 40200),
    v480("4.8.0", 40210),
    v481("4.8.1", 40220),
    v482("4.8.2", 40222),
    v483("4.8.3", 40223),
    v484("4.8.4", 40224),
    v490("4.9.0", 40226),
    v491("4.9.1", 40231),
    v492("4.9.2", 40232),
    v493("4.9.3", 40233),
    v4100("4.10.0", 40236),
    v4110("4.11.0", 40239),
    v4111("4.11.1", 40241),
    v4112("4.11.2", 40242),
    v4120("4.12.0", 40247),
    v41201("4.12.0.1", 40248),
    v4121("4.12.1", 40259),
    v4122("4.12.2", 40261),
    v4130("4.13.0", 40269),
    v4131("4.13.1", 40284),
    v4132("4.13.2", 40285),
    v4133("4.13.3", 40289),
    v4140("4.14.0", 40293),
    v4141("4.14.1", 40307),
    v4142("4.14.2", 40320),
    v4143("4.14.3", 40328),
    v4150("4.15.0", 40330),
    v4160("4.16.0", 40364),
    v4161("4.16.1", 40367),
    v500beta1("5.0.0-beta-1", 50001),
    v500rc2("5.0.0-rc2", 50002),
    v500rc3("5.0.0-rc3", 50003),
    v500rc4("5.0.0-rc4", 50004),
    v500rc5("5.0.0-rc5", 50005),
    v500rc6("5.0.0-rc6", 50006),
    v500("5.0.0", 50007),
    v501("5.0.1", 50008),
    v5011("5.0.1.1", 50009),
    v510("5.1.0", 50013),
    v511("5.1.1", 50014),
    v512("5.1.2", 50017),
    v513("5.1.3", 50019),
    v514("5.1.4", 50021),
    v520("5.2.0", 50024),
    v521m003("5.2.1-m003", 50030),
    v521m004("5.2.1-m004", 50031),
    v521m006("5.2.1-m006", 50034),
    v521("5.2.1", 50040),
    v522m001("5.2.2-m001", 50041),
    v530("5.3.0", 50045),
    v531("5.3.1", 50046),
    v532("5.3.2", 50047),
    v533("5.3.3", 50048),
    v534("5.3.4", 50049),
    v540m001("5.4.0-m001", 50050),
    v540("5.4.0", 50053),
    v541("5.4.1", 50054),
    v542("5.4.2", 50055),
    v543("5.4.3", 50403900),
    v544m001("5.4.4-m001", 50404001),
    v544("5.4.4", 50404900),
    v545("5.4.5", 50405900),
    v545p001("5.4.5-p001", 50405901),
    v546("5.4.6", 50406900),
    v547("5.4.7", 50407900),
    v548("5.4.8", 50408900),
    v550m001("5.5.0-m001", 50500001),
    v550m003("5.5.0-m003", 50500003),
    v550m004("5.5.0-m004", 50500004),
    v550m005("5.5.0-m005", 50500005),
    v550m006("5.5.0-m006", 50500006),
    v550("5.5.0", 50500900),
    v551("5.5.1", 50501900),
    v551p001("5.5.1-p001", 50501901),
    v552m001("5.5.2-m001", 50502001),
    v552m002("5.5.2-m002", 50502002),
    v552("5.5.2", 50502900),
    v560m001("5.6.0-m001", 50600001),
    v560m002("5.6.0-m002", 50600002),
    v560m003("5.6.0-m003", 50600003),
    v560m004("5.6.0-m004", 50600004),
    v560m005("5.6.0-m005", 50600005),
    v560("5.6.0", 50600900),
    v561("5.6.1", 50601900),
    v562("5.6.2", 50602900),
    v563("5.6.3", 50603900),
    v564("5.6.4", 50604900),
    v565("5.6.5", 50605900),
    v566("5.6.6", 50606900),
    v570m001("5.7.0-m001", 50700001),
    v570m002("5.7.0-m002", 50700002),
    v570m003("5.7.0-m003", 50700003),
    v570m004("5.7.0-m004", 50700004),
    v570m005("5.7.0-m005", 50700005),
    v570("5.7.0", 50700900),
    v571("5.7.1", 50701900),
    v572("5.7.2", 50702900),
    v580m008("5.8.0-m008", 50800008),
    v580("5.8.0", 50800900),
    v581("5.8.1", 50801900),
    v582("5.8.2", 50802900),
    v583("5.8.3", 50803900);

    public static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-[mp]\\d{3})?");

    public static ArtifactoryVersion getCurrent() {
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        return versions[versions.length - 1];
    }

    private final String value;
    private final int revision;
    private final Map<String, SubConfigElementVersion> subConfigElementVersionsByClass = new HashMap<>();

    ArtifactoryVersion(String value, int revision) {
        this.value = value;
        this.revision = revision;
    }

    public static <T extends SubConfigElementVersion> void addSubConfigElementVersion(T scev,
            VersionComparator versionComparator) {
        ArtifactoryVersion[] versions = values();
        for (ArtifactoryVersion version : versions) {
            if (versionComparator.supports(version)) {
                version.subConfigElementVersionsByClass.put(scev.getClass().getName(), scev);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T extends SubConfigElementVersion> T getSubConfigElementVersion(Class<T> subConfigElementVersion) {
        return (T) subConfigElementVersionsByClass.get(subConfigElementVersion.getName());
    }

    public String getValue() {
        return value;
    }

    public long getRevision() {
        return revision;
    }

    public boolean isCurrent() {
        return this == getCurrent();
    }

    @Nullable
    public static ArtifactoryVersion fromVersionString(String version) {
        if (StringUtils.isBlank(version)) {
            return null;
        }
        String mavenVersion = convertToMavenVersion(version);
        return Arrays.stream(values())
                .filter(artVersion -> artVersion.getValue().equals(mavenVersion))
                .findAny()
                .orElse(isCurrentVersion(version, "") ? getCurrent() : null);
    }

    public static String convertToMavenVersion(String version) {
        if (version.contains("_m") || version.contains("_p")) {
            version = version.replace("_m", "-m");
            version = version.replace("_p", "-p");
        }
        return version;
    }

    public static boolean isCurrentVersion(String versionString, String revisionString) {
        // If current version or development version=${artifactory.version.prop} or revision=${artifactory.revision.prop}
        if (StringUtils.isBlank(versionString)) {
            throw new IllegalArgumentException("Version value is empty!");
        }
        // Dev version
        if (getCurrent().getValue().equals(convertToMavenVersion(versionString))
                || versionString.endsWith("-SNAPSHOT")
                || versionString.contains(".x-DOWN-")
                || versionString.contains(".x-DOWN.")
                || versionString.startsWith("${") // TODO: [by fsi] why we keep the old issue
                ) {
            return true;
        }
        // No revision, from version it is not latest => Probably an error here?
        if (revisionString == null) {
            return false;
        }
        // Dev revision
        if (revisionString.startsWith("dev") || revisionString.startsWith("${")) {
            return true;
        }
        // Stork dev branch version
        if ((versionString.contains(".x.") || versionString.contains(".x_")) && StringUtils.isNumeric(revisionString)) {
            return true;
        }

        return false;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is before the other version
     */
    public boolean before(ArtifactoryVersion otherVersion) {
        return this.compareTo(otherVersion) < 0;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is after the other version
     */
    public boolean after(ArtifactoryVersion otherVersion) {
        return this.compareTo(otherVersion) > 0;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is before or equal to the other version
     */
    public boolean beforeOrEqual(ArtifactoryVersion otherVersion) {
        return this == otherVersion || this.compareTo(otherVersion) < 0;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is after or equal to the other version
     */
    public boolean afterOrEqual(ArtifactoryVersion otherVersion) {
        return this == otherVersion || this.compareTo(otherVersion) > 0;
    }
}
