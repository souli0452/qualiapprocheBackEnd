import {
  Button
} from "./chunk-IYEO7CFM.js";
import "./chunk-KGQPIMGR.js";
import {
  zindexutils
} from "./chunk-KHDC5QWA.js";
import "./chunk-HQJN727C.js";
import {
  ChevronUpIcon
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
import {
  getWindowScrollTop
} from "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import {
  animate,
  state,
  style,
  transition,
  trigger
} from "./chunk-PBR2IEBG.js";
import {
  CommonModule,
  NgClass,
  NgIf,
  NgStyle,
  NgTemplateOutlet,
  isPlatformBrowser
} from "./chunk-YQX3S2V2.js";
import {
  ChangeDetectionStrategy,
  Component,
  ContentChild,
  ContentChildren,
  Injectable,
  Input,
  NgModule,
  ViewEncapsulation,
  inject,
  numberAttribute,
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
  ɵɵelementContainerEnd,
  ɵɵelementContainerStart,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵgetInheritedFactory,
  ɵɵlistener,
  ɵɵloadQuery,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵpureFunction0,
  ɵɵpureFunction1,
  ɵɵpureFunction2,
  ɵɵqueryRefresh,
  ɵɵreference,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtemplate,
  ɵɵtemplateRefExtractor
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-scrolltop.mjs
var _c0 = ["icon"];
var _c1 = (a0, a1) => ({
  showTransitionParams: a0,
  hideTransitionParams: a1
});
var _c2 = (a0) => ({
  value: "open",
  params: a0
});
var _c3 = () => ({
  styleClass: "p-scrolltop-icon"
});
var _c4 = () => ({
  "font-size": "1rem",
  scale: "1.5"
});
function ScrollTop_p_button_0_ng_template_1_ng_container_0_span_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 7);
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(4);
    ɵɵclassMap(ctx_r1._icon);
    ɵɵproperty("ngClass", "p-scrolltop-icon");
  }
}
function ScrollTop_p_button_0_ng_template_1_ng_container_0_ChevronUpIcon_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "ChevronUpIcon", 8);
  }
  if (rf & 2) {
    ɵɵproperty("styleClass", "p-scrolltop-icon")("ngStyle", ɵɵpureFunction0(2, _c4));
  }
}
function ScrollTop_p_button_0_ng_template_1_ng_container_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, ScrollTop_p_button_0_ng_template_1_ng_container_0_span_1_Template, 1, 3, "span", 5)(2, ScrollTop_p_button_0_ng_template_1_ng_container_0_ChevronUpIcon_2_Template, 1, 3, "ChevronUpIcon", 6);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1._icon);
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1._icon);
  }
}
function ScrollTop_p_button_0_ng_template_1_1_ng_template_0_Template(rf, ctx) {
}
function ScrollTop_p_button_0_ng_template_1_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ScrollTop_p_button_0_ng_template_1_1_ng_template_0_Template, 0, 0, "ng-template", 9);
  }
  if (rf & 2) {
    ɵɵnextContext(2);
    const icon_r3 = ɵɵreference(2);
    ɵɵproperty("ngIf", !icon_r3);
  }
}
function ScrollTop_p_button_0_ng_template_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ScrollTop_p_button_0_ng_template_1_ng_container_0_Template, 3, 2, "ng-container", 3)(1, ScrollTop_p_button_0_ng_template_1_1_Template, 1, 1, null, 4);
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("ngIf", !ctx_r1.iconTemplate && !ctx_r1._iconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.iconTemplate || ctx_r1._iconTemplate)("ngTemplateOutletContext", ɵɵpureFunction0(3, _c3));
  }
}
function ScrollTop_p_button_0_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "p-button", 2);
    ɵɵlistener("@animation.start", function ScrollTop_p_button_0_Template_p_button_animation_animation_start_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onEnter($event));
    })("@animation.done", function ScrollTop_p_button_0_Template_p_button_animation_animation_done_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onLeave($event));
    })("click", function ScrollTop_p_button_0_Template_p_button_click_0_listener() {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onClick());
    });
    ɵɵtemplate(1, ScrollTop_p_button_0_ng_template_1_Template, 2, 4, "ng-template", null, 0, ɵɵtemplateRefExtractor);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵproperty("@animation", ɵɵpureFunction1(8, _c2, ɵɵpureFunction2(5, _c1, ctx_r1.showTransitionOptions, ctx_r1.hideTransitionOptions)))("styleClass", ctx_r1.getStyleClass())("ngStyle", ctx_r1.style)("buttonProps", ctx_r1.buttonProps);
    ɵɵattribute("aria-label", ctx_r1.buttonAriaLabel);
  }
}
var theme = ({
  dt
}) => `
.p-scrolltop.p-button {
    position: fixed;
    bottom: 20px;
    inset-inline-end: 20px;
}

.p-scrolltop-sticky.p-button {
    position: sticky;
    display: flex;
    margin-left: auto;
}

.p-scrolltop-sticky.p-button:dir(rtl) {
    margin-left: 0;
    margin-right: auto;
}

.p-scrolltop-enter-from {
    opacity: 0;
}

.p-scrolltop-enter-active {
    transition: opacity 0.15s;
}

.p-scrolltop.p-scrolltop-leave-to {
    opacity: 0;
}

.p-scrolltop-leave-active {
    transition: opacity 0.15s;
}

/* For PrimeNG */
.p-scrolltop-sticky.p-link {
    margin-left: auto;
}
`;
var classes = {
  root: ({
    props
  }) => ["p-scrolltop", {
    "p-scrolltop-sticky": props.target !== "window"
  }],
  icon: "p-scrolltop-icon"
};
var ScrollTopStyle = class _ScrollTopStyle extends BaseStyle {
  name = "scrolltop";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵScrollTopStyle_BaseFactory;
    return function ScrollTopStyle_Factory(__ngFactoryType__) {
      return (ɵScrollTopStyle_BaseFactory || (ɵScrollTopStyle_BaseFactory = ɵɵgetInheritedFactory(_ScrollTopStyle)))(__ngFactoryType__ || _ScrollTopStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _ScrollTopStyle,
    factory: _ScrollTopStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ScrollTopStyle, [{
    type: Injectable
  }], null, null);
})();
var ScrollTopClasses;
(function(ScrollTopClasses2) {
  ScrollTopClasses2["root"] = "p-scrolltop";
  ScrollTopClasses2["icon"] = "p-scrolltop-icon";
})(ScrollTopClasses || (ScrollTopClasses = {}));
var ScrollTop = class _ScrollTop extends BaseComponent {
  /**
   * Class of the element.
   * @group Props
   */
  styleClass;
  /**
   * Inline style of the element.
   * @group Props
   */
  style;
  /**
   * Target of the ScrollTop.
   * @group Props
   */
  target = "window";
  /**
   * Defines the threshold value of the vertical scroll position of the target to toggle the visibility.
   * @group Props
   */
  threshold = 400;
  /**
   * Name of the icon or JSX.Element for icon.
   * @group Props
   */
  get icon() {
    return this._icon;
  }
  /**
   * Defines the scrolling behavior, "smooth" adds an animation and "auto" scrolls with a jump.
   * @group Props
   */
  behavior = "smooth";
  /**
   * A string value used to determine the display transition options.
   * @group Props
   */
  showTransitionOptions = ".15s";
  /**
   * A string value used to determine the hiding transition options.
   * @group Props
   */
  hideTransitionOptions = ".15s";
  /**
   * Establishes a string value that labels the scroll-top button.
   * @group Props
   */
  buttonAriaLabel;
  /**
   * Used to pass all properties of the ButtonProps to the Button component.
   * @group Props
   */
  buttonProps = {
    rounded: true
  };
  /**
   * Template of the icon.
   * @group Templates
   */
  iconTemplate;
  templates;
  _iconTemplate;
  _icon;
  set icon(value) {
    this._icon = value;
  }
  documentScrollListener;
  parentScrollListener;
  visible = false;
  overlay;
  _componentStyle = inject(ScrollTopStyle);
  ngOnInit() {
    super.ngOnInit();
    if (this.target === "window") this.bindDocumentScrollListener();
    else if (this.target === "parent") this.bindParentScrollListener();
  }
  ngAfterContentInit() {
    this.templates.forEach((item) => {
      switch (item.getType()) {
        case "icon":
          this._iconTemplate = item.template;
          break;
      }
    });
  }
  onClick() {
    let scrollElement = this.target === "window" ? this.document.defaultView : this.el.nativeElement.parentElement;
    scrollElement.scroll({
      top: 0,
      behavior: this.behavior
    });
  }
  onEnter(event) {
    switch (event.toState) {
      case "open":
        this.overlay = event.element;
        zindexutils.set("overlay", this.overlay, this.config.zIndex.overlay);
        break;
      case "void":
        this.overlay = null;
        break;
    }
  }
  onLeave(event) {
    switch (event.toState) {
      case "void":
        zindexutils.clear(event.element);
        break;
    }
  }
  checkVisibility(scrollY) {
    if (scrollY > this.threshold) this.visible = true;
    else this.visible = false;
    this.cd.markForCheck();
  }
  bindParentScrollListener() {
    if (isPlatformBrowser(this.platformId)) {
      this.parentScrollListener = this.renderer.listen(this.el.nativeElement.parentElement, "scroll", () => {
        this.checkVisibility(this.el.nativeElement.parentElement.scrollTop);
      });
    }
  }
  bindDocumentScrollListener() {
    if (isPlatformBrowser(this.platformId)) {
      this.documentScrollListener = this.renderer.listen(this.document.defaultView, "scroll", () => {
        this.checkVisibility(getWindowScrollTop());
      });
    }
  }
  unbindParentScrollListener() {
    if (this.parentScrollListener) {
      this.parentScrollListener();
      this.parentScrollListener = null;
    }
  }
  unbindDocumentScrollListener() {
    if (this.documentScrollListener) {
      this.documentScrollListener();
      this.documentScrollListener = null;
    }
  }
  getStyleClass() {
    return `p-scrolltop p-button${this.styleClass ? ` ${this.styleClass}` : ""}${this.target !== "window" ? " p-scrolltop-sticky" : ""}`;
  }
  ngOnDestroy() {
    if (this.target === "window") this.unbindDocumentScrollListener();
    else if (this.target === "parent") this.unbindParentScrollListener();
    if (this.overlay) {
      zindexutils.clear(this.overlay);
      this.overlay = null;
    }
    super.ngOnDestroy();
  }
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵScrollTop_BaseFactory;
    return function ScrollTop_Factory(__ngFactoryType__) {
      return (ɵScrollTop_BaseFactory || (ɵScrollTop_BaseFactory = ɵɵgetInheritedFactory(_ScrollTop)))(__ngFactoryType__ || _ScrollTop);
    };
  })();
  static ɵcmp = ɵɵdefineComponent({
    type: _ScrollTop,
    selectors: [["p-scrollTop"], ["p-scrolltop"], ["p-scroll-top"]],
    contentQueries: function ScrollTop_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c0, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.iconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    inputs: {
      styleClass: "styleClass",
      style: "style",
      target: "target",
      threshold: [2, "threshold", "threshold", numberAttribute],
      icon: "icon",
      behavior: "behavior",
      showTransitionOptions: "showTransitionOptions",
      hideTransitionOptions: "hideTransitionOptions",
      buttonAriaLabel: "buttonAriaLabel",
      buttonProps: "buttonProps"
    },
    features: [ɵɵProvidersFeature([ScrollTopStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    decls: 1,
    vars: 1,
    consts: [["icon", ""], ["type", "button", 3, "styleClass", "ngStyle", "buttonProps", "click", 4, "ngIf"], ["type", "button", 3, "click", "styleClass", "ngStyle", "buttonProps"], [4, "ngIf"], [4, "ngTemplateOutlet", "ngTemplateOutletContext"], [3, "class", "ngClass", 4, "ngIf"], [3, "styleClass", "ngStyle", 4, "ngIf"], [3, "ngClass"], [3, "styleClass", "ngStyle"], [3, "ngIf"]],
    template: function ScrollTop_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵtemplate(0, ScrollTop_p_button_0_Template, 3, 10, "p-button", 1);
      }
      if (rf & 2) {
        ɵɵproperty("ngIf", ctx.visible);
      }
    },
    dependencies: [CommonModule, NgClass, NgIf, NgTemplateOutlet, NgStyle, ChevronUpIcon, Button, SharedModule],
    encapsulation: 2,
    data: {
      animation: [trigger("animation", [state("void", style({
        opacity: 0
      })), state("open", style({
        opacity: 1
      })), transition("void => open", animate("{{showTransitionParams}}")), transition("open => void", animate("{{hideTransitionParams}}"))])]
    },
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ScrollTop, [{
    type: Component,
    args: [{
      selector: "p-scrollTop, p-scrolltop, p-scroll-top",
      standalone: true,
      imports: [CommonModule, ChevronUpIcon, Button, SharedModule],
      template: `
        <p-button
            *ngIf="visible"
            [@animation]="{
                value: 'open',
                params: { showTransitionParams: showTransitionOptions, hideTransitionParams: hideTransitionOptions }
            }"
            (@animation.start)="onEnter($event)"
            (@animation.done)="onLeave($event)"
            [attr.aria-label]="buttonAriaLabel"
            (click)="onClick()"
            [styleClass]="getStyleClass()"
            [ngStyle]="style"
            type="button"
            [buttonProps]="buttonProps"
        >
            <ng-template #icon>
                <ng-container *ngIf="!iconTemplate && !_iconTemplate">
                    <span *ngIf="_icon" [class]="_icon" [ngClass]="'p-scrolltop-icon'"></span>
                    <ChevronUpIcon *ngIf="!_icon" [styleClass]="'p-scrolltop-icon'" [ngStyle]="{ 'font-size': '1rem', scale: '1.5' }" />
                </ng-container>
                <ng-template [ngIf]="!icon" *ngTemplateOutlet="iconTemplate || _iconTemplate; context: { styleClass: 'p-scrolltop-icon' }"></ng-template>
            </ng-template>
        </p-button>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      animations: [trigger("animation", [state("void", style({
        opacity: 0
      })), state("open", style({
        opacity: 1
      })), transition("void => open", animate("{{showTransitionParams}}")), transition("open => void", animate("{{hideTransitionParams}}"))])],
      providers: [ScrollTopStyle]
    }]
  }], null, {
    styleClass: [{
      type: Input
    }],
    style: [{
      type: Input
    }],
    target: [{
      type: Input
    }],
    threshold: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    icon: [{
      type: Input
    }],
    behavior: [{
      type: Input
    }],
    showTransitionOptions: [{
      type: Input
    }],
    hideTransitionOptions: [{
      type: Input
    }],
    buttonAriaLabel: [{
      type: Input
    }],
    buttonProps: [{
      type: Input
    }],
    iconTemplate: [{
      type: ContentChild,
      args: ["icon", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var ScrollTopModule = class _ScrollTopModule {
  static ɵfac = function ScrollTopModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ScrollTopModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _ScrollTopModule,
    imports: [ScrollTop, SharedModule],
    exports: [ScrollTop, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [ScrollTop, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ScrollTopModule, [{
    type: NgModule,
    args: [{
      imports: [ScrollTop, SharedModule],
      exports: [ScrollTop, SharedModule]
    }]
  }], null, null);
})();
export {
  ScrollTop,
  ScrollTopClasses,
  ScrollTopModule,
  ScrollTopStyle
};
//# sourceMappingURL=primeng_scrolltop.js.map
