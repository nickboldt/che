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
package org.eclipse.che.multiuser.keycloak.server;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.multiuser.keycloak.shared.KeycloakConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches user profile from Keycloack server.
 *
 * @author David Festal <dfestal@redhat.com>
 */
@Singleton
public class KeycloakProfileRetriever {
  private static final Logger LOG = LoggerFactory.getLogger(KeycloakProfileRetriever.class);

  private final String keyclockCurrentUserInfoUrl;
  private final HttpJsonRequestFactory requestFactory;

  @Inject
  public KeycloakProfileRetriever(
      KeycloakSettings keycloakSettings, HttpJsonRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    this.keyclockCurrentUserInfoUrl =
        keycloakSettings.get().get(KeycloakConstants.USERINFO_ENDPOINT_SETTING);
  }

  public Map<String, String> retrieveKeycloakAttributes() throws ServerException {
    try {
      return requestFactory.fromUrl(keyclockCurrentUserInfoUrl).request().asProperties();
    } catch (IOException | ApiException e) {
      LOG.warn("Exception during retrieval of the Keycloak user profile", e);
      throw new ServerException("Exception during retrieval of the Keycloak user profile", e);
    }
  }
}
