package org.artifactory.addon.conan.info;

/**
 * @author Yinon Avraham
 * Created on 07/09/2016.
 */
public class ConanRecipeInfo {

    private String name;
    private String version;
    private String user;
    private String channel;
    private String reference;
    private String author;
    private String url;
    private String license;

    private ConanRecipeInfo() { }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getUser() {
        return user;
    }

    public String getChannel() {
        return channel;
    }

    public String getReference() {
        return reference;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    public String getLicense() {
        return license;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ConanRecipeInfo recipeInfo = new ConanRecipeInfo();

        private Builder() {}

        public Builder name(String name) {
            recipeInfo.name = name;
            return this;
        }

        public Builder version(String version) {
            recipeInfo.version = version;
            return this;
        }

        public Builder user(String user) {
            recipeInfo.user = user;
            return this;
        }

        public Builder channel(String channel) {
            recipeInfo.channel = channel;
            return this;
        }

        public Builder reference(String reference) {
            recipeInfo.reference = reference;
            return this;
        }

        public Builder author(String author) {
            recipeInfo.author = author;
            return this;
        }

        public Builder url(String url) {
            recipeInfo.url = url;
            return this;
        }

        public Builder license(String license) {
            recipeInfo.license = license;
            return this;
        }

        public ConanRecipeInfo create() {
            ConanRecipeInfo result = recipeInfo;
            recipeInfo = null; // ensure this builder can no longer modify the instance
            return result;
        }
    }
}
