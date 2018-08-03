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
  private Set<FileType> unmergeableTypes;
  private Set<FileType> mergeableTypes;

  public FileTypeCollision(
      FileType candidate, Set<FileType> unmergeableTypes, Set<FileType> mergeableTypes) {
    this.candidate = candidate;
    this.unmergeableTypes = unmergeableTypes;
    this.mergeableTypes = mergeableTypes;
  }

  @Override
  public FileType getCandidate() {
    return candidate;
  }

  @Override
  public boolean canBeSafelyMerged() {
    return getUnmergeableTypes().isEmpty() && !getMergeableTypes().isEmpty();
  }

  @Override
  public Set<FileType> merge() {
    Set<FileType> typesToMerge = getMergeableTypes();
    if (candidate == null || typesToMerge.isEmpty()) {
      return emptySet();
    }

    typesToMerge.forEach(fileType -> fileType.addNamePatterns(candidate.getNamePatterns()));
    return typesToMerge;
  }

  @Override
  public Set<FileType> getMergeableTypes() {
    return mergeableTypes == null ? emptySet() : new HashSet<>(mergeableTypes);
  }

  @Override
  public Set<FileType> getUnmergeableTypes() {
    return unmergeableTypes == null ? emptySet() : new HashSet<>(unmergeableTypes);
  }
}
