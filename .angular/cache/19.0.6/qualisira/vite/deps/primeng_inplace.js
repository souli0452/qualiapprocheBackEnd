import {
  ButtonDirective,
  ButtonModule
} from "./chunk-IYEO7CFM.js";
import "./chunk-KGQPIMGR.js";
import {
  Ripple
} from "./chunk-HQJN727C.js";
import {
  TimesIcon
} from "./chunk-G7GR35G3.js";
import "./chunk-K3LJODM5.js";
import "./chunk-5G7WYC4N.js";
import {
  BaseComponent
} from "./chunk-NKPC7AFX.js";
import "./chunk-3RXOYKMS.js";
import {
  BaseStyle
} from "./chunk-2GIPMIFT.js";
import {
  PrimeTemplate,
  SharedModule
} from "./chunk-NG5OZP5M.js";
import "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import {
  CommonModule,
  NgClass,
  NgIf,
  NgStyle,
  NgTemplateOutlet
} from "./chunk-YQX3S2V2.js";
import {
  ChangeDetectionStrategy,
  Component,
  ContentChild,
  ContentChildren,
  EventEmitter,
  Injectable,
  Input,
  NgModule,
  Output,
  ViewEncapsulation,
  booleanAttribute,
  inject,
  setClassMetadata,
  ɵɵInheritDefinitionFeature,
  ɵɵInputTransformsFeature,
  ɵɵProvidersFeature,
  ɵɵadvance,
  ɵɵattribute,
  ɵɵclassMap,
  ɵɵcontentQuery,
  ɵɵdefineComponent,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵelement,
  ɵɵelementContainer,
  ɵɵelementContainerEnd,
  ɵɵelementContainerStart,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵgetInheritedFactory,
  ɵɵlistener,
  ɵɵloadQuery,
  ɵɵnextContext,
  ɵɵprojection,
  ɵɵprojectionDef,
  ɵɵproperty,
  ɵɵpureFunction1,
  ɵɵqueryRefresh,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtemplate
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-inplace.mjs
var _c0 = ["*"];
var _c1 = ["display"];
var _c2 = ["content"];
var _c3 = ["closeicon"];
var _c4 = [[["", "pInplaceDisplay", ""]], [["", "pInplaceContent", ""]]];
var _c5 = ["[pInplaceDisplay]", "[pInplaceContent]"];
var _c6 = (a0) => ({
  "p-inplace p-component": true,
  "p-inplace-closable": a0
});
var _c7 = (a0) => ({
  "p-disabled": a0
});
var _c8 = (a0) => ({
  closeCallback: a0
});
function Inplace_div_1_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Inplace_div_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "div", 3);
    ɵɵlistener("click", function Inplace_div_1_Template_div_click_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onActivateClick($event));
    })("keydown", function Inplace_div_1_Template_div_keydown_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onKeydown($event));
    });
    ɵɵprojection(1);
    ɵɵtemplate(2, Inplace_div_1_ng_container_2_Template, 1, 0, "ng-container", 4);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵproperty("ngClass", ɵɵpureFunction1(2, _c7, ctx_r1.disabled));
    ɵɵadvance(2);
    ɵɵproperty("ngTemplateOutlet", ctx_r1.displayTemplate || ctx_r1._displayTemplate);
  }
}
function Inplace_div_2_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Inplace_div_2_ng_container_3_button_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r3 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 10);
    ɵɵlistener("click", function Inplace_div_2_ng_container_3_button_1_Template_button_click_0_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext(3);
      return ɵɵresetView(ctx_r1.onDeactivateClick($event));
    });
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵproperty("icon", ctx_r1.closeIcon);
    ɵɵattribute("aria-label", ctx_r1.closeAriaLabel);
  }
}
function Inplace_div_2_ng_container_3_button_2_TimesIcon_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "TimesIcon");
  }
}
function Inplace_div_2_ng_container_3_button_2_2_ng_template_0_Template(rf, ctx) {
}
function Inplace_div_2_ng_container_3_button_2_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Inplace_div_2_ng_container_3_button_2_2_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Inplace_div_2_ng_container_3_button_2_Template(rf, ctx) {
  if (rf & 1) {
    const _r4 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 11);
    ɵɵlistener("click", function Inplace_div_2_ng_container_3_button_2_Template_button_click_0_listener($event) {
      ɵɵrestoreView(_r4);
      const ctx_r1 = ɵɵnextContext(3);
      return ɵɵresetView(ctx_r1.onDeactivateClick($event));
    });
    ɵɵtemplate(1, Inplace_div_2_ng_container_3_button_2_TimesIcon_1_Template, 1, 0, "TimesIcon", 7)(2, Inplace_div_2_ng_container_3_button_2_2_Template, 1, 0, null, 4);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵproperty("ngClass", "p-button-icon-only");
    ɵɵattribute("aria-label", ctx_r1.closeAriaLabel);
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.closeIconTemplate && !ctx_r1._closeIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.closeIconTemplate || ctx_r1._closeIconTemplate);
  }
}
function Inplace_div_2_ng_container_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, Inplace_div_2_ng_container_3_button_1_Template, 1, 2, "button", 8)(2, Inplace_div_2_ng_container_3_button_2_Template, 3, 4, "button", 9);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.closeIcon);
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.closeIcon);
  }
}
function Inplace_div_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "div", 5);
    ɵɵprojection(1, 1);
    ɵɵtemplate(2, Inplace_div_2_ng_container_2_Template, 1, 0, "ng-container", 6)(3, Inplace_div_2_ng_container_3_Template, 3, 2, "ng-container", 7);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵadvance(2);
    ɵɵproperty("ngTemplateOutlet", ctx_r1.contentTemplate || ctx_r1._contentTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(3, _c8, ctx_r1.onDeactivateClick.bind(ctx_r1)));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.closable);
  }
}
var theme = ({
  dt
}) => `
.p-inplace-display {
    display: inline-block;
    cursor: pointer;
    border: 1px solid transparent;
    padding: ${dt("inplace.padding")};
    border-radius: ${dt("inplace.border.radius")};
    transition: background ${dt("inplace.transition.duration")}, color ${dt("inplace.transition.duration")}, outline-color ${dt("inplace.transition.duration")}, box-shadow ${dt("inplace.transition.duration")};
    outline-color: transparent;
}

.p-inplace-display:not(.p-disabled):hover {
    background: ${dt("inplace.display.hover.background")};
    color: ${dt("inplace.display.hover.color")};
}

.p-inplace-display:focus-visible {
    box-shadow: ${dt("inplace.focus.ring.shadow")};
    outline: ${dt("inplace.focus.ring.width")} ${dt("inplace.focus.ring.style")} ${dt("inplace.focus.ring.color")};
    outline-offset: ${dt("inplace.focus.ring.offset")};
}

.p-inplace-content {
    display: block;
}
`;
var classes = {
  root: "p-inplace p-component",
  display: ({
    props
  }) => ["p-inplace-display", {
    "p-disabled": props.disabled
  }],
  content: "p-inplace-content"
};
var InplaceStyle = class _InplaceStyle extends BaseStyle {
  name = "inplace";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵInplaceStyle_BaseFactory;
    return function InplaceStyle_Factory(__ngFactoryType__) {
      return (ɵInplaceStyle_BaseFactory || (ɵInplaceStyle_BaseFactory = ɵɵgetInheritedFactory(_InplaceStyle)))(__ngFactoryType__ || _InplaceStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _InplaceStyle,
    factory: _InplaceStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(InplaceStyle, [{
    type: Injectable
  }], null, null);
})();
var InplaceClasses;
(function(InplaceClasses2) {
  InplaceClasses2["root"] = "p-inplace";
  InplaceClasses2["display"] = "p-inplace-display";
  InplaceClasses2["content"] = "p-inplace-content";
})(InplaceClasses || (InplaceClasses = {}));
var InplaceDisplay = class _InplaceDisplay {
  static ɵfac = function InplaceDisplay_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _InplaceDisplay)();
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _InplaceDisplay,
    selectors: [["p-inplacedisplay"], ["p-inplaceDisplay"]],
    ngContentSelectors: _c0,
    decls: 1,
    vars: 0,
    template: function InplaceDisplay_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵprojectionDef();
        ɵɵprojection(0);
      }
    },
    dependencies: [CommonModule],
    encapsulation: 2
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(InplaceDisplay, [{
    type: Component,
    args: [{
      selector: "p-inplacedisplay, p-inplaceDisplay",
      standalone: true,
      imports: [CommonModule],
      template: "<ng-content></ng-content>"
    }]
  }], null, null);
})();
var InplaceContent = class _InplaceContent {
  static ɵfac = function InplaceContent_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _InplaceContent)();
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _InplaceContent,
    selectors: [["p-inplacecontent"], ["p-inplaceContent"]],
    ngContentSelectors: _c0,
    decls: 1,
    vars: 0,
    template: function InplaceContent_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵprojectionDef();
        ɵɵprojection(0);
      }
    },
    dependencies: [CommonModule],
    encapsulation: 2
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(InplaceContent, [{
    type: Component,
    args: [{
      selector: "p-inplacecontent, p-inplaceContent",
      standalone: true,
      imports: [CommonModule],
      template: "<ng-content></ng-content>"
    }]
  }], null, null);
})();
var Inplace = class _Inplace extends BaseComponent {
  /**
   * Whether the content is displayed or not.
   * @group Props
   */
  active = false;
  /**
   * Displays a button to switch back to display mode.
   * @group Props
   */
  closable = false;
  /**
   * When present, it specifies that the element should be disabled.
   * @group Props
   */
  disabled = false;
  /**
   * Allows to prevent clicking.
   * @group Props
   */
  preventClick;
  /**
   * Inline style of the element.
   * @group Props
   */
  style;
  /**
   * Class of the element.
   * @group Props
   */
  styleClass;
  /**
   * Icon to display in the close button.
   * @group Props
   */
  closeIcon;
  /**
   * Establishes a string value that labels the close button.
   * @group Props
   */
  closeAriaLabel;
  /**
   * Callback to invoke when inplace is opened.
   * @param {Event} event - Browser event.
   * @group Emits
   */
  onActivate = new EventEmitter();
  /**
   * Callback to invoke when inplace is closed.
   * @param {Event} event - Browser event.
   * @group Emits
   */
  onDeactivate = new EventEmitter();
  hover;
  /**
   * Display template of the element.
   * @group Templates
   */
  displayTemplate;
  /**
   * Content template of the element.
   * @group Templates
   */
  contentTemplate;
  /**
   * Close icon template of the element.
   * @group Templates
   */
  closeIconTemplate;
  _componentStyle = inject(InplaceStyle);
  onActivateClick(event) {
    if (!this.preventClick) this.activate(event);
  }
  onDeactivateClick(event) {
    if (!this.preventClick) this.deactivate(event);
  }
  /**
   * Activates the content.
   * @param {Event} event - Browser event.
   * @group Method
   */
  activate(event) {
    if (!this.disabled) {
      this.active = true;
      this.onActivate.emit(event);
      this.cd.markForCheck();
    }
  }
  /**
   * Deactivates the content.
   * @param {Event} event - Browser event.
   * @group Method
   */
  deactivate(event) {
    if (!this.disabled) {
      this.active = false;
      this.hover = false;
      this.onDeactivate.emit(event);
      this.cd.markForCheck();
    }
  }
  onKeydown(event) {
    if (event.code === "Enter") {
      this.activate(event);
      event.preventDefault();
    }
  }
  templates;
  _displayTemplate;
  _closeIconTemplate;
  _contentTemplate;
  ngAfterContentInit() {
    this.templates?.forEach((item) => {
      switch (item.getType()) {
        case "display":
          this._displayTemplate = item.template;
          break;
        case "closeicon":
          this._closeIconTemplate = item.template;
          break;
        case "content":
          this._contentTemplate = item.template;
          break;
      }
    });
  }
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵInplace_BaseFactory;
    return function Inplace_Factory(__ngFactoryType__) {
      return (ɵInplace_BaseFactory || (ɵInplace_BaseFactory = ɵɵgetInheritedFactory(_Inplace)))(__ngFactoryType__ || _Inplace);
    };
  })();
  static ɵcmp = ɵɵdefineComponent({
    type: _Inplace,
    selectors: [["p-inplace"]],
    contentQueries: function Inplace_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c1, 4);
        ɵɵcontentQuery(dirIndex, _c2, 4);
        ɵɵcontentQuery(dirIndex, _c3, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.displayTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.contentTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.closeIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    inputs: {
      active: [2, "active", "active", booleanAttribute],
      closable: [2, "closable", "closable", booleanAttribute],
      disabled: [2, "disabled", "disabled", booleanAttribute],
      preventClick: [2, "preventClick", "preventClick", booleanAttribute],
      style: "style",
      styleClass: "styleClass",
      closeIcon: "closeIcon",
      closeAriaLabel: "closeAriaLabel"
    },
    outputs: {
      onActivate: "onActivate",
      onDeactivate: "onDeactivate"
    },
    features: [ɵɵProvidersFeature([InplaceStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    ngContentSelectors: _c5,
    decls: 3,
    vars: 9,
    consts: [[3, "ngClass", "ngStyle"], ["class", "p-inplace-display", "tabindex", "0", "role", "button", 3, "ngClass", "click", "keydown", 4, "ngIf"], ["class", "p-inplace-content", 4, "ngIf"], ["tabindex", "0", "role", "button", 1, "p-inplace-display", 3, "click", "keydown", "ngClass"], [4, "ngTemplateOutlet"], [1, "p-inplace-content"], [4, "ngTemplateOutlet", "ngTemplateOutletContext"], [4, "ngIf"], ["type", "button", "pButton", "", "pRipple", "", 3, "icon", "click", 4, "ngIf"], ["type", "button", "pButton", "", "pRipple", "", 3, "ngClass", "click", 4, "ngIf"], ["type", "button", "pButton", "", "pRipple", "", 3, "click", "icon"], ["type", "button", "pButton", "", "pRipple", "", 3, "click", "ngClass"]],
    template: function Inplace_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵprojectionDef(_c4);
        ɵɵelementStart(0, "div", 0);
        ɵɵtemplate(1, Inplace_div_1_Template, 3, 4, "div", 1)(2, Inplace_div_2_Template, 4, 5, "div", 2);
        ɵɵelementEnd();
      }
      if (rf & 2) {
        ɵɵclassMap(ctx.styleClass);
        ɵɵproperty("ngClass", ɵɵpureFunction1(7, _c6, ctx.closable))("ngStyle", ctx.style);
        ɵɵattribute("aria-live", "polite");
        ɵɵadvance();
        ɵɵproperty("ngIf", !ctx.active);
        ɵɵadvance();
        ɵɵproperty("ngIf", ctx.active);
      }
    },
    dependencies: [CommonModule, NgClass, NgIf, NgTemplateOutlet, NgStyle, ButtonModule, ButtonDirective, TimesIcon, SharedModule, Ripple],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(Inplace, [{
    type: Component,
    args: [{
      selector: "p-inplace",
      standalone: true,
      imports: [CommonModule, ButtonModule, TimesIcon, SharedModule, Ripple],
      template: `
        <div [ngClass]="{ 'p-inplace p-component': true, 'p-inplace-closable': closable }" [ngStyle]="style" [class]="styleClass" [attr.aria-live]="'polite'">
            <div class="p-inplace-display" (click)="onActivateClick($event)" tabindex="0" role="button" (keydown)="onKeydown($event)" [ngClass]="{ 'p-disabled': disabled }" *ngIf="!active">
                <ng-content select="[pInplaceDisplay]"></ng-content>
                <ng-container *ngTemplateOutlet="displayTemplate || _displayTemplate"></ng-container>
            </div>
            <div class="p-inplace-content" *ngIf="active">
                <ng-content select="[pInplaceContent]"></ng-content>
                <ng-container *ngTemplateOutlet="contentTemplate || _contentTemplate; context: { closeCallback: onDeactivateClick.bind(this) }"></ng-container>

                <ng-container *ngIf="closable">
                    <button *ngIf="closeIcon" type="button" [icon]="closeIcon" pButton pRipple (click)="onDeactivateClick($event)" [attr.aria-label]="closeAriaLabel"></button>
                    <button *ngIf="!closeIcon" type="button" pButton pRipple [ngClass]="'p-button-icon-only'" (click)="onDeactivateClick($event)" [attr.aria-label]="closeAriaLabel">
                        <TimesIcon *ngIf="!closeIconTemplate && !_closeIconTemplate" />
                        <ng-template *ngTemplateOutlet="closeIconTemplate || _closeIconTemplate"></ng-template>
                    </button>
                </ng-container>
            </div>
        </div>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [InplaceStyle]
    }]
  }], null, {
    active: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    closable: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    disabled: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    preventClick: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    style: [{
      type: Input
    }],
    styleClass: [{
      type: Input
    }],
    closeIcon: [{
      type: Input
    }],
    closeAriaLabel: [{
      type: Input
    }],
    onActivate: [{
      type: Output
    }],
    onDeactivate: [{
      type: Output
    }],
    displayTemplate: [{
      type: ContentChild,
      args: ["display", {
        descendants: false
      }]
    }],
    contentTemplate: [{
      type: ContentChild,
      args: ["content", {
        descendants: false
      }]
    }],
    closeIconTemplate: [{
      type: ContentChild,
      args: ["closeicon", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var InplaceModule = class _InplaceModule {
  static ɵfac = function InplaceModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _InplaceModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _InplaceModule,
    imports: [Inplace, InplaceContent, InplaceDisplay, SharedModule],
    exports: [Inplace, InplaceContent, InplaceDisplay, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [Inplace, InplaceContent, InplaceDisplay, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(InplaceModule, [{
    type: NgModule,
    args: [{
      imports: [Inplace, InplaceContent, InplaceDisplay, SharedModule],
      exports: [Inplace, InplaceContent, InplaceDisplay, SharedModule]
    }]
  }], null, null);
})();
export {
  Inplace,
  InplaceClasses,
  InplaceContent,
  InplaceDisplay,
  InplaceModule,
  InplaceStyle
};
//# sourceMappingURL=primeng_inplace.js.map
