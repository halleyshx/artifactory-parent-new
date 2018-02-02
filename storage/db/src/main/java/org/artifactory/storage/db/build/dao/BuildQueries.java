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

package org.artifactory.storage.db.build.dao;

/**
 * @author Chen Keinan
 */
public class BuildQueries {

    private BuildQueries() {
    }

    public static final String MODULE_DEPENDENCY_DIFF_QUERY = "select * from (\n" +
                // get module dependencies which are in build b and not in a - removed from last build
            "SELECT * from (\n" +
                "                SELECT  build_dependencies.dependency_name_id ,build_dependencies.dependency_type,build_dependencies.sha1  sh,build_dependencies.dependency_scopes,'Removed'  status FROM build_dependencies\n" +
                "                                left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                                left join builds on  build_modules.build_id = builds.build_id\n" +
                "                              where build_dependencies.dependency_name_id not in (SELECT distinct build_dependencies.dependency_name_id   FROM build_dependencies\n" +
                "                                left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                                left join builds on  build_modules.build_id = builds.build_id\n" +
                "                              where builds.build_number = ? and builds.build_date = ?  and\n" +
                "                                    build_modules.module_name_id = ?) and builds.build_number = ? and builds.build_date = ? and\n" +
                "                                    build_modules.module_name_id = ?\n" +
            "\n" +
                // get module dependencies which are in build a and not in b - added to last build
                "union\n" +
                "        SELECT  build_dependencies.dependency_name_id ,build_dependencies.dependency_type,build_dependencies.sha1  sh,build_dependencies.dependency_scopes,'New' status FROM build_dependencies\n" +
                "                        left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                        left join builds on  build_modules.build_id = builds.build_id\n" +
                "                      where build_dependencies.dependency_name_id not in (SELECT distinct build_dependencies.dependency_name_id   FROM build_dependencies\n" +
                "                        left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                        left join builds on  build_modules.build_id = builds.build_id\n" +
                "                      where builds.build_number = ? and builds.build_date =?  and\n" +
                "                            build_modules.module_name_id = ?) and builds.build_number = ? and builds.build_date = ?  and\n" +
                "                            build_modules.module_name_id = ?)  x\n" +
            "\n" +
            "union\n" +
                // get module dependencies which has the same sha1 on both builds - no changes
                "        SELECT o.c,o.dependency_type,o.sh,o.dependency_scopes,o.status from (\n" +
                "                 SELECT * from (\n" +
                "                                 SELECT  build_dependencies.dependency_name_id c  ,build_dependencies.dependency_type ,build_dependencies.sha1 sh,build_dependencies.dependency_scopes,'Unchanged' status1 FROM build_dependencies\n" +
                "                                   left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                                   left join builds on  build_modules.build_id = builds.build_id\n" +
                "                                 where builds.build_number = ? and builds.build_date = ? and build_modules.module_name_id = ? )  x\n" +
                "                   inner join (\n" +
            "\n" +
                "                 SELECT * from (\n" +
                "                                SELECT  build_dependencies.dependency_name_id  b,build_dependencies.dependency_type type_x,build_dependencies.sha1 sh2,build_dependencies.dependency_scopes scope,'Unchanged' status FROM build_dependencies\n" +
                "                                  left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                                  left join builds on  build_modules.build_id = builds.build_id\n" +
                "                                where builds.build_number = ? and builds.build_date = ? and\n" +
                "                                      build_modules.module_name_id = ?)  t)  v on v.sh2 = x.sh) o\n" +
            "\n" +
                // get module dependencies which has the diff sha1 on both builds - has changes
                "union\n" +
                "        SELECT o.c,o.dependency_type,o.sh,o.dependency_scopes,o.status from (\n" +
                "             SELECT * from (\n" +
                "                             SELECT  build_dependencies.dependency_name_id  c  ,build_dependencies.dependency_type ,build_dependencies.sha1 sh,build_dependencies.dependency_scopes,'Updated' status1 FROM build_dependencies\n" +
                "                               left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                               left join builds on  build_modules.build_id = builds.build_id\n" +
                "                             where builds.build_number = ? and builds.build_date = ? and build_modules.module_name_id = ? )  x\n" +
            "\n" +
                "               inner join (\n" +
            "\n" +
                "             SELECT * from (\n" +
                "                            SELECT  build_dependencies.dependency_name_id  b,build_dependencies.dependency_type type_x,build_dependencies.sha1 sh2,build_dependencies.dependency_scopes  scop,'Updated' status FROM build_dependencies\n" +
                "                              left join build_modules on build_modules.module_id=build_dependencies.module_id\n" +
                "                              left join builds on  build_modules.build_id = builds.build_id\n" +
                "                            where builds.build_number = ? and builds.build_date = ? and\n" +
                "                                  build_modules.module_name_id = ?)  t) v on v.b = x.c where v.sh2 != x.sh) o\n" +
                "                                  )  z  ";
    public static final String MODULE_ARTIFACT_DIFF_QUERY = "\n" +
            "select * from (\n" +
                // get module artifacts which are in build b and not in a - removed from last build
                "\n" +
            "                SELECT * from (\n" +
                "                                SELECT  build_artifacts.artifact_name ,build_artifacts.artifact_type,build_artifacts.sha1  sh,'Removed' status FROM build_artifacts\n" +
            "                                  left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                  left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                where build_artifacts.artifact_name not in (SELECT distinct build_artifacts.artifact_name   FROM build_artifacts\n" +
            "                                  left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                  left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                where builds.build_number = ? and builds.build_date = ?  and\n" +
            "                                      build_modules.module_name_id = ?) and builds.build_number = ? and builds.build_date = ? and\n" +
            "                                      build_modules.module_name_id = ?\n" +
                // get module artifacts which are in build a and not in b - added to last build
                "\n" +
            "                                union\n" +
                "                                SELECT  build_artifacts.artifact_name ,build_artifacts.artifact_type,build_artifacts.sha1  sh,'New'  status FROM build_artifacts\n" +
            "                                  left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                  left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                where build_artifacts.artifact_name not in (SELECT distinct build_artifacts.artifact_name   FROM build_artifacts\n" +
            "                                  left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                  left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                where builds.build_number = ? and builds.build_date =?  and\n" +
            "                                      build_modules.module_name_id = ?) and builds.build_number = ? and builds.build_date = ?  and\n" +
                "                                      build_modules.module_name_id = ?)  x\n" +
            "\n" +
                // get module artifacts which has the same sha1 on both builds - no changes
                "                union\n" +
                "                SELECT o.c,o.artifact_type,o.sh,o.status from (\n" +
            "                                                       SELECT * from (\n" +
                "                                                                       SELECT  build_artifacts.artifact_name  c  ,build_artifacts.artifact_type ,build_artifacts.sha1  sh,'Unchanged'  status1 FROM build_artifacts\n" +
            "                                                                         left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                                                         left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                                                       where builds.build_number = ? and builds.build_date = ? and build_modules.module_name_id = ? )  x\n" +
            "                                                         inner join (\n" +
            "\n" +
            "                                                                      SELECT * from (\n" +
                "                                                                                      SELECT  build_artifacts.artifact_name  b,build_artifacts.artifact_type  type_x,build_artifacts.sha1  sh2,'Unchanged'  status FROM build_artifacts\n" +
            "                                                                                        left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                                                                        left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                                                                      where builds.build_number = ? and builds.build_date = ? and\n" +
                "                                                                                            build_modules.module_name_id = ?)  t)  v on v.sh2 = x.sh) o\n" +
            "\n" +
                // get module artifacts which has the diff sha1 on both builds - has changes
                "                union\n" +
                "                SELECT o.c,o.artifact_type,o.sh,o.status from (\n" +
            "                                                       SELECT * from (\n" +
                "                                                                       SELECT  build_artifacts.artifact_name  c  ,build_artifacts.artifact_type ,build_artifacts.sha1  sh,'Updated'  status1 FROM build_artifacts\n" +
            "                                                                         left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                                                         left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                                                       where builds.build_number = ? and builds.build_date = ? and build_modules.module_name_id = ? )  x\n" +
            "\n" +
            "                                                         inner join (\n" +
            "\n" +
            "                                                                      SELECT * from (\n" +
                "                                                                                      SELECT  build_artifacts.artifact_name  b,build_artifacts.artifact_type  type_x,build_artifacts.sha1  sh2,'Updated'  status FROM build_artifacts\n" +
            "                                                                                        left join build_modules on build_modules.module_id=build_artifacts.module_id\n" +
            "                                                                                        left join builds on  build_modules.build_id = builds.build_id\n" +
            "                                                                                      where builds.build_number = ? and builds.build_date = ? and\n" +
                "                                                                                            build_modules.module_name_id = ?)  t)  v on v.b = x.c where v.sh2 != x.sh) o )  z  \n" +
            "\n";


    public static final String BUILD_PROPS_DIFF = "select * from (\tSELECT distinct build_props.prop_key,build_props.prop_value,'New' status,'new' new from  build_props inner join\n" +
            "  builds on builds.build_id = build_props.build_id where build_props.prop_key not in\n" +
            "                                                         (SELECT distinct build_props.prop_key from  build_props inner join\n" +
            "                                                           builds on builds.build_id = build_props.build_id where builds.build_number = ? and builds.build_date = ?)  and\n" +
            "                                                         builds.build_number = ? and builds.build_date = ?\n" +
            "                 union\n" +
            "                 SELECT distinct build_props.prop_key,build_props.prop_value,'Removed' status,'removed' removed from  build_props inner join\n" +
            "                   builds on builds.build_id = build_props.build_id where build_props.prop_key not in\n" +
            "                                                                          (SELECT distinct build_props.prop_key from  build_props inner join\n" +
            "                                                                            builds on builds.build_id = build_props.build_id where builds.build_number = ? and builds.build_date = ?)  and\n" +
            "                                                                          builds.build_number = ? and builds.build_date = ?\n" +
            "                 union\n" +
            "                 select r.c,r.v,r.st1,r.vb from (\n" +
            "                                             select * from (\n" +
            "                                                             SELECT distinct  build_props.prop_key c,build_props.prop_value  v,'Updated' st1 from  build_props inner join\n" +
            "                                                               builds on builds.build_id = build_props.build_id where\n" +
            "                                                               builds.build_number = ? and builds.build_date = ?) k\n" +
            "                                               left  join\n" +
            "                                               (SELECT distinct  build_props.prop_key b,build_props.prop_value vb,'Updated' status from  build_props inner join\n" +
            "                                                 builds on builds.build_id = build_props.build_id where\n" +
            "                                                 builds.build_number =?   and builds.build_date =? )t  on t.b = k.c where t.vb != k.v) r\n" +
            "                 union\n" +
            "                 select r.c,r.v,r.st1,r.v from (\n" +
            "                                             select * from (\n" +
            "                                                             SELECT distinct  build_props.prop_key c,build_props.prop_value v,'Unchanged' st1 from  build_props inner join\n" +
            "                                                               builds on builds.build_id = build_props.build_id where\n" +
            "                                                               builds.build_number = ? and builds.build_date = ?) k\n" +
            "                                               left  join\n" +
            "                                               (SELECT distinct  build_props.prop_key b,build_props.prop_value vb,'Unchanged' status from  build_props inner join\n" +
            "                                                 builds on builds.build_id = build_props.build_id where\n" +
            "                                                 builds.build_number =? and builds.build_date = ?)t  on t.b = k.c where t.vb = k.v)  r) z order by 1";

    public static final String BUILD_BY_NUMBER_DATE = "\n" +
            "SELECT build_id FROM builds WHERE build_number = ?\n" +
                                " AND builds.build_date = ?\n";

    public static final String BUILD_ENV_PROPS_BY_BUILD_ID = "\n" +
            "SELECT prop_key, prop_value FROM  build_props\n" +
            "  WHERE build_id  = ?\n " +
            " AND prop_key LIKE 'buildInfo.env.%'";

    public static final String BUILD_SYSTEM_PROPS_BY_BUILD_ID = "\n" +
            "SELECT prop_key, prop_value FROM  build_props\n" +
            " WHERE build_id  = ? \n AND prop_key NOT LIKE 'buildInfo.env.%'";

    /**
     * all prep query below has been done as part of trying to UI support diff in db query along with db pagination
     * currently it not removed from here and from thr code consuming it as it not finally been confirmed
     * that we are not going to use it ,maybe should be removed later as part of code clean up when its confirm
     * that is no longer needed
     */
    public static final String MODULE_DEPENDENCY_DIFF_COUNT = "module dependency diff count prep";
    public static final String MODULE_ARTIFACT_DIFF_COUNT = "module artifact diff count prep";
    public static final String BUILD_ARTIFACT_DIFF_QUERY = "build artifact query prep";
    public static final String BUILD_DEPENDENCY_DIFF_QUERY = "build dependency query prep";
    public static final String BUILD_ARTIFACT_DIFF_COUNT = "build artifact count query prep";
    public static final String BUILD_DEPENDENCY_DIFF_COUNT = "build dependency count query prep";
    public static final String BUILD_ENV_PROPS_COUNT = "build env count query prep";
    public static final String BUILD_SYSTEM_PROPS_COUNT = "build system count query prep";
    public static final String BUILD_PROPS_COUNT = "build props query prep";
}
