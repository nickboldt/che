/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.keycloak.server.deploy;

import com.google.inject.servlet.ServletModule;
import javax.inject.Singleton;
import org.eclipse.che.commons.logback.filter.IdentityIdLoggerFilter;
import org.eclipse.che.multiuser.keycloak.server.KeycloakAuthenticationFilter;
import org.eclipse.che.multiuser.keycloak.server.KeycloakEnvironmentInitalizationFilter;
import org.eclipse.che.multiuser.keycloak.server.UnavailableResourceInMultiUserFilter;

public class KeycloakServletModule extends ServletModule {

  private static final String KEYCLOAK_FILTER_PATHS =
      "^"
          // not equals to /keycloak/OIDCKeycloak.js
          + "(?!/keycloak/(OIDC|oidc)[^\\/]+$)"
          // not contains /docs/ (for swagger)
          + "(?!.*(/docs/))"
          // not ends with '/oauth/callback/' or '/keycloak/settings/' or '/system/state'
          + "(?!.*(/keycloak/settings/?|/oauth/callback/?|/system/state/?)$)"
          // all other
          + ".*";

  @Override
  protected void configureServlets() {
    bind(KeycloakAuthenticationFilter.class).in(Singleton.class);

    filterRegex(KEYCLOAK_FILTER_PATHS).through(KeycloakAuthenticationFilter.class);
    filterRegex(KEYCLOAK_FILTER_PATHS).through(KeycloakEnvironmentInitalizationFilter.class);
    filterRegex(KEYCLOAK_FILTER_PATHS).through(IdentityIdLoggerFilter.class);

    // Ban change password (POST /user/password) and create a user (POST /user/) methods
    // but not remove user (DELETE /user/{USER_ID}
    filterRegex("^/user(/password/?|/)?$").through(UnavailableResourceInMultiUserFilter.class);

    filterRegex("^/profile/(.*/)?attributes$").through(UnavailableResourceInMultiUserFilter.class);
  }
}
