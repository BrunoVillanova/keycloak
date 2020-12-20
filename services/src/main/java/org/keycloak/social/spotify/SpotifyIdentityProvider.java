/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.social.spotify;

import com.fasterxml.jackson.databind.JsonNode;

import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.util.StringUtil;

/**
 * @author <a href="mailto:bruno@digimundi.com.br">Bruno Villanova</a>
 */
public class SpotifyIdentityProvider extends AbstractOAuth2IdentityProvider<SpotifyIdentityProviderConfig> implements SocialIdentityProvider<SpotifyIdentityProviderConfig> {

	public static final String AUTH_URL = "https://accounts.spotify.com/authorize";
	public static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
	public static final String PROFILE_URL = "https://api.spotify.com/v1/me?fields=id,display_name,email";
	public static final String DEFAULT_SCOPE = "user-read-email";
	protected static final String PROFILE_URL_FIELDS_SEPARATOR = ",";

	public SpotifyIdentityProvider(KeycloakSession session, SpotifyIdentityProviderConfig config) {
		super(session, config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
		try {
			final String fetchedFields = getConfig().getFetchedFields();
			final String url = StringUtil.isNotNull(fetchedFields)
					? String.join(PROFILE_URL_FIELDS_SEPARATOR, PROFILE_URL, fetchedFields)
					: PROFILE_URL;
			JsonNode profile = SimpleHttp.doGet(url, session).header("Authorization", "Bearer " + accessToken).asJson();
			return extractIdentityFromProfile(null, profile);
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from Spotify.", e);
		}
	}

	@Override
	protected boolean supportsExternalExchange() {
		return true;
	}

	@Override
	protected String getProfileEndpointForValidation(EventBuilder event) {
		return PROFILE_URL;
	}

	@Override
	protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
		String id = getJsonProperty(profile, "id");

		BrokeredIdentityContext user = new BrokeredIdentityContext(id);

		String email = getJsonProperty(profile, "email");

		user.setEmail(email);

		String displayName = getJsonProperty(profile, "display_name");
		String username = email;

		if (username == null) {
			username = id;
		}

		user.setUsername(username);
		user.setName(displayName);
		user.setIdpConfig(getConfig());
		user.setIdp(this);

		AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

		return user;
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}
}
