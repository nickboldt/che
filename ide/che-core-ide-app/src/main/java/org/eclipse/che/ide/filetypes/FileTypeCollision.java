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

import static java.util.Collections.emptySet;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry.Collision;

/**
 * Implementation of {@link Collision}
 *
 * @author Roman Nikitenko
 */
public class FileTypeCollision implements Collision {

  private FileType candidate;
  private Set<FileType> conflictTypes;
  private Set<FileType> mergeTypes;

  public FileTypeCollision(
      FileType candidate, Set<FileType> conflictTypes, Set<FileType> mergeTypes) {
    this.candidate = candidate;
    this.conflictTypes = conflictTypes;
    this.mergeTypes = mergeTypes;
  }

  @Override
  public FileType getCandidate() {
    return candidate;
  }

  @Override
  public boolean hasMerges() {
    return !getMergeTypes().isEmpty();
  }

  @Override
  public Set<FileType> merge() {
    Set<FileType> typesToMerge = getMergeTypes();
    if (candidate == null || typesToMerge.isEmpty()) {
      return emptySet();
    }

    typesToMerge.forEach(fileType -> fileType.addNamePatterns(candidate.getNamePatterns()));
    return typesToMerge;
  }

  @Override
  public Set<FileType> getMergeTypes() {
    return mergeTypes == null ? emptySet() : new HashSet<>(mergeTypes);
  }

  @Override
  public boolean hasConflicts() {
    return !getConflictTypes().isEmpty();
  }

  @Override
  public Set<FileType> getConflictTypes() {
    return conflictTypes == null ? emptySet() : new HashSet<>(conflictTypes);
  }
}
