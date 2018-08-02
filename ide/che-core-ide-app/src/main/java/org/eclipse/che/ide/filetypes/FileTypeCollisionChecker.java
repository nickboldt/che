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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry.Collision;

/**
 * Allows to detect File Type incompatibility
 *
 * @author Roman Nikitenko
 */
@Singleton
public class FileTypeCollisionChecker {

  /**
   * Returns {@link Collision} when File Type incompatibility is detected and {@code null} otherwise
   */
  @Nullable
  public Collision check(FileType candidate, Set<FileType> fileTypes) {
    if (candidate == null || fileTypes == null) {
      return null;
    }

    Set<FileType> mergeTypes = new HashSet<>();
    Set<FileType> conflictTypes = new HashSet<>();

    for (FileType fileType : fileTypes) {
      if (hasExtensionConflict(fileType, candidate)) {
        conflictTypes.add(fileType);
        continue;
      }

      if (canBeMerged(fileType, candidate)) {
        mergeTypes.add(fileType);
      }
    }

    return mergeTypes.isEmpty() && conflictTypes.isEmpty()
        ? null
        : new FileTypeCollision(candidate, conflictTypes, mergeTypes);
  }

  private boolean hasExtensionConflict(FileType fileType, FileType candidate) {
    String extensionCandidate = candidate.getExtension();

    return !isNullOrEmpty(extensionCandidate)
        && extensionCandidate.equals(fileType.getExtension())
        && !candidate.equals(fileType);
  }

  private boolean canBeMerged(FileType fileType, FileType candidate) {
    String extension = fileType.getExtension();
    Set<String> namePatterns = fileType.getNamePatterns();

    return candidate
        .getNamePatterns()
        .stream()
        .anyMatch(
            patternCandidate -> {
              if (!isNullOrEmpty(extension)
                  && RegExp.compile(patternCandidate).test('.' + extension)) {
                return true;
              }
              return namePatterns.contains(patternCandidate)
                  || namePatterns.contains(RegExp.quote(patternCandidate));
            });
  }
}
