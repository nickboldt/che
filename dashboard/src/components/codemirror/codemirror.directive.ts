/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';


/**
 * Binds a CodeMirror widget to a textarea element.
 *
 * @author Oleksii Orel
 */
export class CheUiCodemirrorDirective implements ng.IDirective {

  static $inject = ['$timeout', 'udCodemirrorConfig'];

  restrict = 'EA';
  require = 'ngModel';

  private $timeout: ng.ITimeoutService;
  private udCodemirrorConfig: any;

  /**
   * Default constructor that is using resource injection
   */
  constructor($timeout: ng.ITimeoutService, udCodemirrorConfig: any) {
    this.$timeout = $timeout;
    this.udCodemirrorConfig = udCodemirrorConfig;
  }

  link($scope: ng.IScope, $element: ng.IAugmentedJQuery, $attrs: ng.IAttributes, $ctrl: ng.INgModelController): void {

    let codemirrorOptions = angular.extend(
      {value: $element.text()},
      this.udCodemirrorConfig.codemirror || {},
      $scope.$eval(($attrs as any).uiCodemirror),
      $scope.$eval(($attrs as any).uiCodemirrorOpts)
    );

    if (angular.isUndefined(codemirrorOptions.onLoad)) {
      codemirrorOptions.onLoad = (editor: any) => {
        this.$timeout(() => {
          editor.refresh();
        });
      };
    }

    let codemirror = this.newCodemirrorEditor($element, codemirrorOptions);

    this.configOptionsWatcher(
      codemirror,
      ($attrs as any).uiCodemirror || ($attrs as any).uiCodemirrorOpts,
      $scope
    );

    this.configNgModelLink(codemirror, $ctrl, $scope);

    this.configUiRefreshAttribute(codemirror, ($attrs as any).uiRefresh, $scope);

    // allow access to the CodeMirror instance through a broadcasted event
    // eg: $broadcast('CodeMirror', function(cm){...});
    $scope.$on('CodeMirror', (event, callback) => {
      if (angular.isFunction(callback)) {
        callback(codemirror);
      } else {
        throw new Error('the CodeMirror event requires a callback function');
      }
    });

    // onLoad callback
    if (angular.isFunction(codemirrorOptions.onLoad)) {
      codemirrorOptions.onLoad(codemirror);
    }
  }

  newCodemirrorEditor(iElement, codemirrorOptions) {
    if (iElement[0].tagName === 'TEXTAREA') {
      // might bug but still ...
      return this.CodeMirror.fromTextArea(iElement[0], codemirrorOptions);
    }
    iElement.html('');
    return new this.CodeMirror((cm_el) => {
      iElement.append(cm_el);
    }, codemirrorOptions);
  }

  configOptionsWatcher(codemirrot, uiCodemirrorAttr, scope) {
    if (!uiCodemirrorAttr) {
      return;
    }

    const updateOptions = (newValues, oldValue) => {
      if (!angular.isObject(newValues)) {
        return;
      }
      Object.keys(this.CodeMirror.defaults).forEach((key) => {
        if (newValues.hasOwnProperty(key)) {

          if (oldValue && newValues[key] === oldValue[key]) {
            return;
          }

          codemirrot.setOption(key, newValues[key]);
        }
      });
    };
    scope.$watch(uiCodemirrorAttr, updateOptions, true);

  }

  configNgModelLink(codemirror, ngModel, scope) {
    if (!ngModel) {
      return;
    }
    // codeMirror expects a string, so make sure it gets one.
    // this does not change the model.
    ngModel.$formatters.push((value) => {
      if (angular.isUndefined(value) || value === null) {
        return '';
      } else if (angular.isObject(value) || angular.isArray(value)) {
        throw new Error('ui-codemirror cannot use an object or an array as a model');
      }
      return value;
    });


    // override the ngModelController $render method, which is what gets called when the model is updated.
    // this takes care of the synchronizing the codeMirror element with the underlying model, in the case that it is changed by something else.
    ngModel.$render = () => {
      // code mirror expects a string so make sure it gets one
      // although the formatter have already done this, it can be possible that another formatter returns undefined (for example the required directive)
      let safeViewValue = ngModel.$viewValue || '';
      codemirror.setValue(safeViewValue);
    };


    // keep the ngModel in sync with changes from CodeMirror
    codemirror.on('change', (instance) => {
      let newValue = instance.getValue();
      if (newValue !== ngModel.$viewValue) {
        scope.$evalAsync(() => {
          ngModel.$setViewValue(newValue);
        });
      }
    });
  }

  configUiRefreshAttribute(codeMirror, uiRefreshAttr, scope) {
    if (!uiRefreshAttr) {
      return;
    }

    scope.$watch(uiRefreshAttr, (newVal, oldVal) => {
      // skip the initial watch firing
      if (newVal !== oldVal) {
        this.$timeout(() => {
          codeMirror.refresh();
        });
      }
    });
  }

  private get CodeMirror(): any {
    return (window as any).CodeMirror;
  }

}
