package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.tabs.conan;

/**
 * @author Yinon Avraham
 * Created on 06/09/2016.
 */
public class ConanInfoModel {

    private ConanRecipeInfoModel recipeInfo;
    private int packageCount;

    public ConanRecipeInfoModel getRecipeInfo() {
        return recipeInfo;
    }

    public void setRecipeInfo(ConanRecipeInfoModel recipeInfo) {
        this.recipeInfo = recipeInfo;
    }

    public int getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(int packageCount) {
        this.packageCount = packageCount;
    }
}
