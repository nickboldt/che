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
import static org.eclipse.che.ide.util.NameUtils.getFileExtension;

import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.resources.VirtualFile;

/**
 * Implementation of {@link org.eclipse.che.ide.api.filetypes.FileTypeRegistry}
 *
 * @author Artem Zatsarynnyi
 */
@Singleton
public class FileTypeRegistryImpl implements FileTypeRegistry {
  private final FileType unknownFileType;
  private final FileTypeCollisionChecker collisionChecker;
  private final Set<FileType> fileTypes = new HashSet<>();

  @Inject
  public FileTypeRegistryImpl(
      @Named("defaultFileType") FileType unknownFileType,
      FileTypeCollisionChecker collisionChecker) {
    this.unknownFileType = unknownFileType;
    this.collisionChecker = collisionChecker;
  }

  @Override
  public Registration register(FileType candidate) {
    Collision collision = collisionChecker.check(candidate, getFileTypes());
    if (collision != null) {
      return new FileTypeRegistration(collision);
    }

    fileTypes.add(candidate);
    return new FileTypeRegistration();
  }

  @Override
  public void registerFileType(FileType candidate) {
    Registration registration = register(candidate);
    if (registration.isSuccessful()) {
      return;
    }

    Collision collision = registration.getCollision();
    if (collision.canBeSafelyMerged()) {
      collision.merge();
      return;
    }

    throw new IllegalStateException(
        "Can not register file type with extension " + candidate.getExtension());
  }

  @Override
  public List<FileType> getRegisteredFileTypes() {
    return new ArrayList<>(fileTypes);
  }

  @Override
  public Set<FileType> getFileTypes() {
    return new HashSet<>(fileTypes);
  }

  @Override
  public FileType getFileTypeByFile(VirtualFile file) {
    String fileName = file.getName();
    String fileExtension = getFileExtension(fileName);

    FileType fileType = getFileTypeByExtension(fileExtension);
    if (fileType == unknownFileType) {
      fileType = getFileTypeByFileName(fileName);
    }
    return fileType != null ? fileType : unknownFileType;
  }

  @Override
  public FileType getFileTypeByExtension(String extension) {
    if (isNullOrEmpty(extension)) {
      return unknownFileType;
    }

    Optional<FileType> fileType =
        fileTypes.stream().filter(type -> extension.equals(type.getExtension())).findFirst();
    return fileType.orElse(unknownFileType);
  }

  @Override
  public FileType getFileTypeByFileName(String name) {
    if (isNullOrEmpty(name)) {
      return unknownFileType;
    }

    Optional<FileType> fileType =
        fileTypes.stream().filter(type -> doesFileNameMatchType(name, type)).findFirst();
    return fileType.orElse(unknownFileType);
  }

  private boolean doesFileNameMatchType(String nameToTest, FileType fileType) {
    return fileType
        .getNamePatterns()
        .stream()
        .anyMatch(
            namePattern -> {
              RegExp regExp = RegExp.compile(namePattern);
              return regExp.test(nameToTest);
            });
  }
}
