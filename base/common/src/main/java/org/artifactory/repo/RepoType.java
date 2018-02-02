package org.artifactory.repo;

/**
 * @author nadavy
 */
public enum RepoType{
    DISTRIBUTION("distribution"),
    LOCAL("local"),
    REMOTE("remote"),
    VIRTUAL("virtual");

    private String nativeName;

    RepoType(String nativeName) {
        this.nativeName = nativeName;
    }

    public static RepoType byNativeName(String nativeName) {
        for (RepoType repoType : values()) {
            if(repoType.nativeName.equals(nativeName.trim())){
                return repoType;
            }
        }
        return null;
    }
}
