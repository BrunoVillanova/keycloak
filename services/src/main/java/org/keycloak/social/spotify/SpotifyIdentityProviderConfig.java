package org.keycloak.social.spotify;

import java.util.Optional;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class SpotifyIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public SpotifyIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public SpotifyIdentityProviderConfig() {
    }

    public String getFetchedFields() {
        return Optional.ofNullable(getConfig().get("fetchedFields"))
                .map(fieldsConfig -> fieldsConfig.replaceAll("\\s+",""))
                .orElse("");
    }

    public void setFetchedFields(final String fetchedFields) {
        getConfig().put("fetchedFields", fetchedFields);
    }
}
