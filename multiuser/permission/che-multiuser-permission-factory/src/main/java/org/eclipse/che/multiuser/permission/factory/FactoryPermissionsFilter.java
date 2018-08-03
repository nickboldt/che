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
package org.eclipse.che.multiuser.permission.factory;

import javax.ws.rs.Path;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.eclipse.che.multiuser.permission.workspace.server.WorkspaceDomain;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

/**
 * Restricts access to methods of FactoryService by user's permissions
 *
 * @author Anton Korneta
 */
@Filter
@Path("/factory/{path:.*}")
public class FactoryPermissionsFilter extends CheMethodInvokerFilter {

  @Override
  protected void filter(GenericResourceMethod genericResourceMethod, Object[] arguments)
      throws ApiException {
    final String methodName = genericResourceMethod.getMethod().getName();

    final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
    String action;
    String workspaceId;

    switch (methodName) {
      case "getFactoryJson":
        {
          workspaceId = ((String) arguments[0]);
          action = WorkspaceDomain.READ;
          break;
        }
      default:
        // public methods
        return;
    }
    currentSubject.checkPermission(WorkspaceDomain.DOMAIN_ID, workspaceId, action);
  }
}
