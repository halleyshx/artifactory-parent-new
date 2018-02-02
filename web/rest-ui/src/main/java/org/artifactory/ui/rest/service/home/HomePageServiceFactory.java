package org.artifactory.ui.rest.service.home;

import org.artifactory.ui.rest.service.home.widget.*;
import org.springframework.beans.factory.annotation.Lookup;

/**
 * @author Dan Feldman
 */
public abstract class HomePageServiceFactory {

    @Lookup
    public abstract GeneralInfoWidgetService getGeneralInfoWidget();

    @Lookup
    public abstract ArtifactCountWidgetService artifactCountWidget();

    @Lookup
    public abstract AddonInfoWidgetService getAddonsWidget();

    @Lookup
    public abstract LatestBuildsWidgetService getLatestBuildsWidget();

    @Lookup
    public abstract MostDownloadedWidgetService getMostDownloadedWidget();

}
