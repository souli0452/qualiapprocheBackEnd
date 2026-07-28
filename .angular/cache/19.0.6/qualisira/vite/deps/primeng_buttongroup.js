import {
  BaseComponent
} from "./chunk-NKPC7AFX.js";
import "./chunk-3RXOYKMS.js";
import {
  BaseStyle
} from "./chunk-2GIPMIFT.js";
import "./chunk-NG5OZP5M.js";
import "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import {
  CommonModule
} from "./chunk-YQX3S2V2.js";
import {
  ChangeDetectionStrategy,
  Component,
  Injectable,
  NgModule,
  ViewEncapsulation,
  inject,
  setClassMetadata,
  ɵɵInheritDefinitionFeature,
  ɵɵProvidersFeature,
  ɵɵdefineComponent,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetInheritedFactory,
  ɵɵprojection,
  ɵɵprojectionDef
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-buttongroup.mjs
var _c0 = ["*"];
var theme = ({
  dt
}) => `
.p-buttongroup .p-button {
    margin: 0;
}

.p-buttongroup .p-button:not(:last-child),
.p-buttongroup .p-button:not(:last-child):hover {
    border-right: 0 none;
}

.p-buttongroup .p-button:not(:first-of-type):not(:last-of-type) {
    border-radius: 0;
}

.p-buttongroup .p-button:first-of-type:not(:only-of-type) {
    border-start-end-radius: 0;
    border-end-end-radius: 0;
}

.p-buttongroup .p-button:last-of-type:not(:only-of-type) {
    border-start-start-radius: 0;
    border-end-start-radius: 0;
}

.p-buttongroup .p-button:focus {
    position: relative;
    z-index: 1;
}

.p-buttongroup {
    display:flex;
}

.p-buttongroup > .p-button {
    flex: 1;
}

/* For PrimeNG */

.p-buttongroup .p-button:focus,
.p-buttongroup p-button:focus .p-button,
.p-buttonset .p-button:focus,
.p-buttonset .p-button:focus,
.p-buttonset p-button:focus .p-button,
.p-buttonset .p-button:focus {
    position: relative;
    z-index: 1;
}

.p-buttongroup .p-button:not(:last-child),
.p-buttongroup .p-button:not(:last-child):hover,
.p-buttongroup p-button:not(:last-child) .p-button,
.p-buttongroup p-button:not(:last-child) .p-button:hover,
.p-buttonset .p-button:not(:last-child),
.p-buttonset .p-button:not(:last-child):hover,
.p-buttonset p-button:not(:last-child) .p-button,
.p-buttonset p-button:not(:last-child) .p-button:hover {
    border-right: 0 none;
}

.p-buttongroup .p-button:not(:first-of-type):not(:last-of-type),
.p-buttongroup p-button:not(:first-of-type):not(:last-of-type) .p-button,
.p-buttonset .p-button:not(:first-of-type):not(:last-of-type),
.p-buttonset p-button:not(:first-of-type):not(:last-of-type) .p-button {
    border-radius: 0;
}

.p-buttongroup .p-button:first-of-type:not(:only-of-type),
.p-buttongroup p-button:first-of-type:not(:only-of-type) .p-button,
.p-buttonset .p-button:first-of-type:not(:only-of-type),
.p-buttonset p-button:first-of-type:not(:only-of-type) .p-button {
    border-start-end-radius: 0;
    border-end-end-radius: 0;
}

.p-buttongroup .p-button:last-of-type:not(:only-of-type),
.p-buttongroup p-button:last-of-type:not(:only-of-type) .p-button,
.p-buttonset .p-button:last-of-type:not(:only-of-type),
.p-buttonset p-button:last-of-type:not(:only-of-type) .p-button {
    border-start-start-radius: 0;
    border-end-start-radius: 0;
}

p-button[iconpos='right'] spinnericon {
    order: 1;
}
`;
var classes = {
  root: "p-buttongroup p-component"
};
var ButtonGroupStyle = class _ButtonGroupStyle extends BaseStyle {
  name = "buttongroup";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵButtonGroupStyle_BaseFactory;
    return function ButtonGroupStyle_Factory(__ngFactoryType__) {
      return (ɵButtonGroupStyle_BaseFactory || (ɵButtonGroupStyle_BaseFactory = ɵɵgetInheritedFactory(_ButtonGroupStyle)))(__ngFactoryType__ || _ButtonGroupStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _ButtonGroupStyle,
    factory: _ButtonGroupStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ButtonGroupStyle, [{
    type: Injectable
  }], null, null);
})();
var ButtonGroupClasses;
(function(ButtonGroupClasses2) {
  ButtonGroupClasses2["root"] = "p-buttongroup";
})(ButtonGroupClasses || (ButtonGroupClasses = {}));
var ButtonGroup = class _ButtonGroup extends BaseComponent {
  _componentStyle = inject(ButtonGroupStyle);
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵButtonGroup_BaseFactory;
    return function ButtonGroup_Factory(__ngFactoryType__) {
      return (ɵButtonGroup_BaseFactory || (ɵButtonGroup_BaseFactory = ɵɵgetInheritedFactory(_ButtonGroup)))(__ngFactoryType__ || _ButtonGroup);
    };
  })();
  static ɵcmp = ɵɵdefineComponent({
    type: _ButtonGroup,
    selectors: [["p-buttonGroup"], ["p-buttongroup"], ["p-button-group"]],
    features: [ɵɵProvidersFeature([ButtonGroupStyle]), ɵɵInheritDefinitionFeature],
    ngContentSelectors: _c0,
    decls: 2,
    vars: 0,
    consts: [["role", "group", 1, "p-buttongroup", "p-component"]],
    template: function ButtonGroup_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵprojectionDef();
        ɵɵelementStart(0, "span", 0);
        ɵɵprojection(1);
        ɵɵelementEnd();
      }
    },
    dependencies: [CommonModule],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ButtonGroup, [{
    type: Component,
    args: [{
      selector: "p-buttonGroup, p-buttongroup, p-button-group",
      standalone: true,
      imports: [CommonModule],
      template: `
        <span class="p-buttongroup p-component" role="group">
            <ng-content></ng-content>
        </span>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [ButtonGroupStyle]
    }]
  }], null, null);
})();
var ButtonGroupModule = class _ButtonGroupModule {
  static ɵfac = function ButtonGroupModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ButtonGroupModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _ButtonGroupModule,
    imports: [ButtonGroup],
    exports: [ButtonGroup]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [ButtonGroup]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ButtonGroupModule, [{
    type: NgModule,
    args: [{
      imports: [ButtonGroup],
      exports: [ButtonGroup]
    }]
  }], null, null);
})();
export {
  ButtonGroup,
  ButtonGroupClasses,
  ButtonGroupModule,
  ButtonGroupStyle
};
//# sourceMappingURL=primeng_buttongroup.js.map
