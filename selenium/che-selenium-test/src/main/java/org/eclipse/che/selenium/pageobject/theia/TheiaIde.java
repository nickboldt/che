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
package org.eclipse.che.selenium.pageobject.theia;

import static java.lang.String.format;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.PREPARING_WS_TIMEOUT_SEC;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.PreDestroy;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

@Singleton
public class TheiaIde {

  private final SeleniumWebDriverHelper seleniumWebDriverHelper;
  private final SeleniumWebDriver seleniumWebDriver;

  @Inject
  TheiaIde(SeleniumWebDriver seleniumWebDriver, SeleniumWebDriverHelper seleniumWebDriverHelper) {
    this.seleniumWebDriverHelper = seleniumWebDriverHelper;
    this.seleniumWebDriver = seleniumWebDriver;
    PageFactory.initElements(seleniumWebDriver, this);
  }

  public interface Locators {
    String THEIA_IDE_ID = "theia-app-shell";
    String THEIA_IDE_TOP_PANEL_ID = "theia-top-panel";
    String LOADER_XPATH = "//div[@class='theia-preload theia-hidden']";
    String MAIN_MENU_ITEM_XPATH_PATTERN = "//div[@id='theia:menubar']//li/div[text()='%s']";
    String SUBMENU_ITEM_XPATH_PATTERN = "//li[@class='p-Menu-item']/div[text()='%s']";
    String ABOUT_DIALOG_XPATH = "//div[@class='dialogBlock']";
    String ABOUT_DIALOG_TITLE_XPATH = ABOUT_DIALOG_XPATH + "//div[@class='dialogTitle']";
    String ABOUT_DIALOG_CONTENT_XPATH = ABOUT_DIALOG_XPATH + "//div[@class='dialogContent']";
    String ABOUT_DIALOG_OK_BUTTON_XPATH = ABOUT_DIALOG_XPATH + "//button";
  }

  @FindBy(id = Locators.THEIA_IDE_ID)
  WebElement theiaIde;

  @FindBy(id = Locators.THEIA_IDE_TOP_PANEL_ID)
  WebElement theiaIdeTopPanel;

  @FindBy(xpath = Locators.LOADER_XPATH)
  WebElement loader;

  @FindBy(xpath = Locators.ABOUT_DIALOG_XPATH)
  WebElement aboutDialog;

  @FindBy(xpath = Locators.ABOUT_DIALOG_TITLE_XPATH)
  WebElement aboutDialogTitle;

  @FindBy(xpath = Locators.ABOUT_DIALOG_CONTENT_XPATH)
  WebElement aboutDialogContent;

  @FindBy(xpath = Locators.ABOUT_DIALOG_OK_BUTTON_XPATH)
  WebElement aboutDialogOkButton;

  public void waitTheiaIde() {
    seleniumWebDriverHelper.waitVisibility(theiaIde, PREPARING_WS_TIMEOUT_SEC);
  }

  public void waitTheiaIdeTopPanel() {
    seleniumWebDriverHelper.waitVisibility(theiaIdeTopPanel);
  }

  public void waitLoaderInvisibility() {
    seleniumWebDriverHelper.waitInvisibility(loader);
  }

  public void clickOnMenuItemInMainMenu(String itemName) {
    seleniumWebDriverHelper.waitAndClick(
        By.xpath(format(Locators.MAIN_MENU_ITEM_XPATH_PATTERN, itemName)));
  }

  public void clickOnSubmenuItem(String itemName) {
    seleniumWebDriverHelper.waitAndClick(
        By.xpath(format(Locators.SUBMENU_ITEM_XPATH_PATTERN, itemName)));
  }

  /**
   * Run command from sub menu.
   *
   * @param topMenuCommand
   * @param commandName
   */
  public void runMenuCommand(String topMenuCommand, String commandName) {
    clickOnMenuItemInMainMenu(topMenuCommand);
    clickOnSubmenuItem(commandName);
  }

  public void waitAboutDialogIsOpen() {
    seleniumWebDriverHelper.waitVisibility(aboutDialog, PREPARING_WS_TIMEOUT_SEC);
    seleniumWebDriverHelper.waitTextContains(aboutDialogTitle, "Theia");
  }

  public void waitAboutDialogIsClosed() {
    seleniumWebDriverHelper.waitInvisibility(aboutDialog);
  }

  public void closeAboutDialog() {
    seleniumWebDriverHelper.waitAndClick(aboutDialogOkButton);
  }

  public void waitAboutDialogContains(String expectedText) {
    seleniumWebDriverHelper.waitTextContains(aboutDialogContent, expectedText);
  }

  public void switchToIdeFrame() {
    seleniumWebDriverHelper.waitAndSwitchToFrame(
        By.id("ide-application-iframe"), PREPARING_WS_TIMEOUT_SEC);
  }

  @PreDestroy
  public void close() {
    seleniumWebDriver.quit();
  }
}
