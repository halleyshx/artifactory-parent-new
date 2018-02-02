package org.artifactory.rest.resource.token;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Yinon Avraham.
 */
public class TokensResponseModel {

    @JsonProperty("tokens")
    private final List<TokenInfoModel> tokens = Lists.newArrayList();

    @Nonnull
    public List<TokenInfoModel> getTokens() {
        return tokens;
    }

    public void setTokens(@Nullable List<TokenInfoModel> tokens) {
        this.tokens.clear();
        if (tokens != null) {
            this.tokens.addAll(tokens);
        }
    }

}
