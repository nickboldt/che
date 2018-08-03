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
package org.eclipse.che.ide.filetypes;

import org.eclipse.che.ide.api.filetypes.FileTypeRegistry.Collision;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry.Registration;

/**
 * Implementation of {@link Registration}
 *
 * @author Roman Nikitenko
 */
public class FileTypeRegistration implements Registration {

  private boolean isSuccessful;
  private Collision collision;

  public FileTypeRegistration() {
    this(true, null);
  }

  public FileTypeRegistration(Collision collision) {
    this(false, collision);
  }

  public FileTypeRegistration(boolean isSuccessful, Collision collision) {
    this.isSuccessful = isSuccessful;
    setCollision(collision);
  }

  @Override
  public boolean isSuccessful() {
    return isSuccessful;
  }

  @Override
  public Collision getCollision() {
    return collision;
  }

  private void setCollision(Collision collision) {
    this.collision = collision != null ? collision : new FileTypeCollision(null, null, null);
  }
}
