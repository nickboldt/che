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
package org.eclipse.che.plugin.languageserver.ide;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;
import org.eclipse.che.api.languageserver.shared.model.LanguageRegex;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry.Collision;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry.Registration;
import org.eclipse.che.plugin.languageserver.ide.editor.LanguageServerEditorProvider;
import org.eclipse.che.plugin.languageserver.ide.registry.LanguageServerRegistry;
import org.eclipse.che.plugin.languageserver.ide.service.LanguageServerServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LanguageRegexesInitializer {
  private static Logger LOGGER = LoggerFactory.getLogger(LanguageRegexesInitializer.class);

  private final LanguageServerRegistry lsRegistry;
  private final LanguageServerResources resources;
  private final EditorRegistry editorRegistry;
  private final LanguageServerEditorProvider editorProvider;
  private final LanguageServerServiceClient languageServerServiceClient;
  private final FileTypeRegistry fileTypeRegistry;

  @Inject
  public LanguageRegexesInitializer(
      LanguageServerRegistry lsRegistry,
      LanguageServerResources resources,
      EditorRegistry editorRegistry,
      LanguageServerEditorProvider editorProvider,
      LanguageServerServiceClient languageServerServiceClient,
      FileTypeRegistry fileTypeRegistry) {
    this.lsRegistry = lsRegistry;
    this.resources = resources;
    this.editorRegistry = editorRegistry;
    this.editorProvider = editorProvider;
    this.languageServerServiceClient = languageServerServiceClient;
    this.fileTypeRegistry = fileTypeRegistry;
  }

  void initialize() {
    languageServerServiceClient
        .getLanguageRegexes()
        .then(
            languageRegexes -> {
              languageRegexes.forEach(
                  languageRegex -> {
                    FileType fileTypeCandidate =
                        new FileType(resources.file(), null, languageRegex.getNamePattern());
                    registerFileType(fileTypeCandidate, languageRegex);
                  });
            })
        .catchError(
            promiseError -> {
              LOGGER.error("Error", promiseError.getCause());
            });
  }

  private void registerFileType(FileType fileTypeCandidate, LanguageRegex languageRegex) {
    Registration registration = fileTypeRegistry.register(fileTypeCandidate);
    if (registration.isSuccessfully()) {
      lsRegistry.registerFileType(fileTypeCandidate, languageRegex);
      editorRegistry.registerDefaultEditor(fileTypeCandidate, editorProvider);
      return;
    }

    Collision collision = registration.getCollision();
    if (collision.hasConflicts()) {
      LOGGER.error("Can not register file type with extension " + fileTypeCandidate.getExtension());
      return;
    }

    if (collision.hasMerges()) {
      Set<FileType> mergedTypes = collision.merge();
      mergedTypes.forEach(
          fileType -> {
            lsRegistry.registerFileType(fileTypeCandidate, languageRegex);
            editorRegistry.registerDefaultEditor(fileTypeCandidate, editorProvider);
          });
    }
  }
}
