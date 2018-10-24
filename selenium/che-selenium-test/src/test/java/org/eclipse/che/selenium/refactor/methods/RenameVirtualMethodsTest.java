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
package org.eclipse.che.selenium.refactor.methods;

import static org.testng.Assert.fail;

import com.google.inject.Inject;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.client.TestProjectServiceClient;
import org.eclipse.che.selenium.core.project.ProjectTemplates;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.core.workspace.TestWorkspace;
import org.eclipse.che.selenium.pageobject.AskDialog;
import org.eclipse.che.selenium.pageobject.CodenvyEditor;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.Ide;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.Refactor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** @author Musienko Maxim */
public class RenameVirtualMethodsTest {
  private static final Logger LOG = LoggerFactory.getLogger(RenameVirtualMethodsTest.class);
  private static final String nameOfProject =
      RenameVirtualMethodsTest.class.getSimpleName() + new Random().nextInt(9999);
  private static final String pathToPackageInChePrefix =
      nameOfProject + "/src" + "/main" + "/java" + "/renameVirtualMethods";

  private String pathToCurrentPackage;
  private String contentFromInA;
  private String contentFromOutA;

  @Inject private TestWorkspace workspace;
  @Inject private Ide ide;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Loader loader;
  @Inject private CodenvyEditor editor;
  @Inject private Refactor refactor;
  @Inject private AskDialog askDialog;
  @Inject private TestProjectServiceClient testProjectServiceClient;
  @Inject private SeleniumWebDriver seleniumWebDriver;
  @Inject private Consoles consoles;

  @BeforeClass
  public void setup() throws Exception {
    URL resource = getClass().getResource("/projects/RenameVirtualMethods");
    testProjectServiceClient.importProject(
        workspace.getId(),
        Paths.get(resource.toURI()),
        nameOfProject,
        ProjectTemplates.MAVEN_SIMPLE);
    ide.open(workspace);
    ide.waitOpenedWorkspaceIsReadyToUse();
    consoles.waitJDTLSProjectResolveFinishedMessage(nameOfProject);
    projectExplorer.waitItem(nameOfProject);
    projectExplorer.quickExpandWithJavaScript();
  }

  @BeforeMethod
  public void expandTreeOfProject(Method testName) {
    try {
      setFieldsForTest(testName.getName());
    } catch (Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  @AfterMethod
  public void closeForm() {
    try {
      if (refactor.isWidgetOpened()) {
        loader.waitOnClosed();
        try {
          refactor.clickCancelButtonRefactorForm();
        } catch (Exception ex) {
          LOG.warn(ex.getLocalizedMessage());
          seleniumWebDriver.navigate().refresh();
        }
      }
      editor.closeAllTabs();
    } catch (Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }

  @Test
  public void test1() {
    doRefactoringWithKeys(14, 19, "k");
  }

  @Test
  public void test11() {
    doRefactoringWithKeys(15, 10, "k");
  }

  @Test
  public void test25() {
    doRefactorByWizard(14, 10, "k");
  }

  @Test
  public void testEnum2() {
    doRefactoringWithKeys(30, 20, "get2ndPower");
  }

  @Test
  public void testFail35() {
    doRefactorByWizardWithClosingWarnMess(14, 10, "k");
  }

  @Test
  public void testGeneric2() {
    doRefactorByWizard(20, 20, "addIfPositive");
    editor.waitTextIntoEditor(contentFromOutA);
  }

  @Test
  public void testVarArgs1() {
    doRefactorByWizard(26, 74, "runThes");
    editor.waitTextIntoEditor(contentFromOutA);
  }

  private void doRefactoringWithKeys(
      int cursorPositionLine, int cursorPositionChar, String newName) {
    prepareProjectForRefactor(cursorPositionLine, cursorPositionChar);
    editor.launchLocalRefactor();
    editor.typeTextIntoEditor(newName);
    editor.typeTextIntoEditor(Keys.ENTER.toString());
    editor.waitTextIntoEditor(contentFromOutA);
  }

  private void doRefactorByWizard(int cursorPositionLine, int cursorPositionChar, String newName) {
    prepareProjectForRefactor(cursorPositionLine, cursorPositionChar);
    editor.launchRefactorForm();
    refactor.waitRenameMethodFormIsOpen();
    typeAndWaitNewName(newName);
    refactor.sendKeysIntoField(Keys.ARROW_LEFT.toString());
    refactor.sendKeysIntoField(Keys.ARROW_LEFT.toString());
    // need for validation on server side
    WaitUtils.sleepQuietly(2);
    refactor.clickOkButtonRefactorForm();

    try {
      refactor.waitRenameMethodFormIsClosed();
    } catch (TimeoutException ex) {
      // remove try-catch block after issue has been resolved
      fail("Known issue: https://github.com/eclipse/che/issues/10784", ex);
    }
  }

  private void doRefactorByWizardWithClosingWarnMess(
      int cursorPositionLine, int cursorPositionChar, String newName) {
    prepareProjectForRefactor(cursorPositionLine, cursorPositionChar);
    editor.launchRefactorForm();
    refactor.waitRenameMethodFormIsOpen();
    typeAndWaitNewName(newName);
    refactor.clickOkButtonRefactorForm();
    askDialog.waitFormToOpen();
    askDialog.clickOkBtn();
    askDialog.waitFormToClose();

    try {
      refactor.waitRenameMethodFormIsClosed();
    } catch (TimeoutException ex) {
      // remove try-catch block after issue has been resolved
      fail("Known issue: https://github.com/eclipse/che/issues/10784", ex);
    }
  }

  private void prepareProjectForRefactor(int cursorPositionLine, int cursorPositionChar) {
    projectExplorer.waitItem(pathToPackageInChePrefix);
    projectExplorer.openItemByPath(pathToCurrentPackage + "/A.java");
    editor.waitTextIntoEditor(contentFromInA);
    editor.goToCursorPositionVisible(cursorPositionLine, cursorPositionChar);
  }

  private void setFieldsForTest(String nameCurrentTest) throws Exception {
    pathToCurrentPackage = pathToPackageInChePrefix + "/" + nameCurrentTest;

    URL resourcesIn =
        getClass()
            .getResource(
                "/org/eclipse/che/selenium/refactor/methods/virtual/"
                    + nameCurrentTest
                    + "/in/A.java");
    URL resourcesOut =
        getClass()
            .getResource(
                "/org/eclipse/che/selenium/refactor/methods/virtual/"
                    + nameCurrentTest
                    + "/out/A.java");

    contentFromInA = getTextFromFile(resourcesIn);
    contentFromOutA = getTextFromFile(resourcesOut);
  }

  private String getTextFromFile(URL url) throws Exception {
    String result = "";
    List<String> listWithAllLines =
        Files.readAllLines(Paths.get(url.toURI()), Charset.forName("UTF-8"));
    for (String buffer : listWithAllLines) {
      result += buffer + '\n';
    }

    return result;
  }

  private void typeAndWaitNewName(String newName) {
    try {
      refactor.typeAndWaitNewName(newName);
    } catch (TimeoutException ex) {
      // remove try-catch block after issue has been resolved
      fail("Known issue https://github.com/eclipse/che/issues/7500");
    }
  }
}
