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
package org.eclipse.che.ide.util;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.Selection;
import elemental.html.TextAreaElement;
import elemental.ranges.Range;

/** Utility methods to work with browser clipboard component. */
public class ClipboardUtils {

  /**
   * Copies element content into buffer.
   *
   * @param elementId DOM element id
   * @return {@code true} if copy successful, otherwise {@code false}
   */
  public static boolean copyElementContents(String elementId) {
    Selection selection = Browser.getWindow().getSelection();
    Range range = Browser.getDocument().createRange();
    Element element = Browser.getDocument().getElementById(elementId);
    if (element instanceof TextAreaElement) {
      ((TextAreaElement) element).select();
      element.focus();
    } else {
      range.selectNodeContents(element);
      selection.removeAllRanges();
      selection.addRange(range);
    }
    boolean success = Browser.getWindow().getDocument().execCommand("copy", false, "");
    selection.removeAllRanges();
    return success;
  }
}
