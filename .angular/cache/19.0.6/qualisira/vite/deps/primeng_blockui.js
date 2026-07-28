import {
  zindexutils
} from "./chunk-KHDC5QWA.js";
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
  blockBodyScroll,
  unblockBodyScroll
} from "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import {
  CommonModule,
  NgClass,
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
  ViewChild,
  ViewEncapsulation,
  booleanAttribute,
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
  ɵɵelementContainer,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetInheritedFactory,
  ɵɵloadQuery,
  ɵɵprojection,
  ɵɵprojectionDef,
  ɵɵproperty,
  ɵɵpureFunction0,
  ɵɵpureFunction1,
  ɵɵqueryRefresh,
  ɵɵtemplate,
  ɵɵviewQuery
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-blockui.mjs
var _c0 = ["content"];
var _c1 = ["mask"];
var _c2 = ["*"];
var _c3 = (a0) => ({
  "p-blockui-mask-document": a0,
  "p-blockui p-blockui-mask p-overlay-mask": true
});
var _c4 = () => ({
  display: "none"
});
function BlockUI_ng_container_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
var theme = ({
  dt
}) => `
.p-blockui {
    position: relative;
}

.p-blockui-mask {
    border-radius: ${dt("blockui.border.radius")};
}

.p-blockui-mask.p-overlay-mask {
    position: absolute;
}

.p-blockui-mask-document.p-overlay-mask {
    position: fixed;
}
`;
var classes = {
  root: "p-blockui"
};
var BlockUiStyle = class _BlockUiStyle extends BaseStyle {
  name = "blockui";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵBlockUiStyle_BaseFactory;
    return function BlockUiStyle_Factory(__ngFactoryType__) {
      return (ɵBlockUiStyle_BaseFactory || (ɵBlockUiStyle_BaseFactory = ɵɵgetInheritedFactory(_BlockUiStyle)))(__ngFactoryType__ || _BlockUiStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _BlockUiStyle,
    factory: _BlockUiStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(BlockUiStyle, [{
    type: Injectable
  }], null, null);
})();
var BlockUIClasses;
(function(BlockUIClasses2) {
  BlockUIClasses2["root"] = "p-blockui";
})(BlockUIClasses || (BlockUIClasses = {}));
var BlockUI = class _BlockUI extends BaseComponent {
  /**
   * Name of the local ng-template variable referring to another component.
   * @group Props
   */
  target;
  /**
   * Whether to automatically manage layering.
   * @group Props
   */
  autoZIndex = true;
  /**
   * Base zIndex value to use in layering.
   * @group Props
   */
  baseZIndex = 0;
  /**
   * Class of the element.
   * @group Props
   */
  styleClass;
  /**
   * Current blocked state as a boolean.
   * @group Props
   */
  get blocked() {
    return this._blocked;
  }
  set blocked(val) {
    if (this.mask && this.mask.nativeElement) {
      if (val) this.block();
      else this.unblock();
    } else {
      this._blocked = val;
    }
  }
  /**
   * template of the content
   * @group Templates
   */
  contentTemplate;
  mask;
  _blocked = false;
  animationEndListener;
  _componentStyle = inject(BlockUiStyle);
  constructor() {
    super();
  }
  ngAfterViewInit() {
    super.ngAfterViewInit();
    if (this._blocked) this.block();
    if (this.target && !this.target.getBlockableElement) {
      throw "Target of BlockUI must implement BlockableUI interface";
    }
  }
  _contentTemplate;
  templates;
  ngAfterContentInit() {
    this.templates.forEach((item) => {
      switch (item.getType()) {
        case "content":
          this.contentTemplate = item.template;
          break;
        default:
          this.contentTemplate = item.template;
          break;
      }
    });
  }
  block() {
    if (isPlatformBrowser(this.platformId)) {
      this._blocked = true;
      this.mask.nativeElement.style.display = "flex";
      if (this.target) {
        this.target.getBlockableElement().appendChild(this.mask.nativeElement);
        this.target.getBlockableElement().style.position = "relative";
      } else {
        this.renderer.appendChild(this.document.body, this.mask.nativeElement);
        blockBodyScroll();
      }
      if (this.autoZIndex) {
        zindexutils.set("modal", this.mask.nativeElement, this.baseZIndex + this.config.zIndex.modal);
      }
    }
  }
  unblock() {
    if (isPlatformBrowser(this.platformId) && this.mask && !this.animationEndListener) {
      this.destroyModal();
    }
  }
  destroyModal() {
    this._blocked = false;
    if (this.mask && isPlatformBrowser(this.platformId)) {
      zindexutils.clear(this.mask.nativeElement);
      this.renderer.removeChild(this.el.nativeElement, this.mask.nativeElement);
      unblockBodyScroll();
    }
    this.unbindAnimationEndListener();
    this.cd.markForCheck();
  }
  unbindAnimationEndListener() {
    if (this.animationEndListener && this.mask) {
      this.animationEndListener();
      this.animationEndListener = null;
    }
  }
  ngOnDestroy() {
    this.unblock();
    this.destroyModal();
    super.ngOnDestroy();
  }
  static ɵfac = function BlockUI_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _BlockUI)();
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _BlockUI,
    selectors: [["p-blockUI"], ["p-blockui"], ["p-block-ui"]],
    contentQueries: function BlockUI_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c0, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.contentTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    viewQuery: function BlockUI_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c1, 5);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.mask = _t.first);
      }
    },
    inputs: {
      target: "target",
      autoZIndex: [2, "autoZIndex", "autoZIndex", booleanAttribute],
      baseZIndex: [2, "baseZIndex", "baseZIndex", numberAttribute],
      styleClass: "styleClass",
      blocked: [2, "blocked", "blocked", booleanAttribute]
    },
    features: [ɵɵProvidersFeature([BlockUiStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    ngContentSelectors: _c2,
    decls: 4,
    vars: 11,
    consts: [["mask", ""], [3, "ngClass", "ngStyle"], [4, "ngTemplateOutlet"]],
    template: function BlockUI_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵprojectionDef();
        ɵɵelementStart(0, "div", 1, 0);
        ɵɵprojection(2);
        ɵɵtemplate(3, BlockUI_ng_container_3_Template, 1, 0, "ng-container", 2);
        ɵɵelementEnd();
      }
      if (rf & 2) {
        ɵɵclassMap(ctx.styleClass);
        ɵɵproperty("ngClass", ɵɵpureFunction1(8, _c3, !ctx.target))("ngStyle", ɵɵpureFunction0(10, _c4));
        ɵɵattribute("aria-busy", ctx.blocked)("data-pc-name", "blockui")("data-pc-section", "root");
        ɵɵadvance(3);
        ɵɵproperty("ngTemplateOutlet", ctx.contentTemplate || ctx._contentTemplate);
      }
    },
    dependencies: [CommonModule, NgClass, NgTemplateOutlet, NgStyle, SharedModule],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(BlockUI, [{
    type: Component,
    args: [{
      selector: "p-blockUI, p-blockui, p-block-ui",
      standalone: true,
      imports: [CommonModule, SharedModule],
      template: `
        <div
            #mask
            [class]="styleClass"
            [attr.aria-busy]="blocked"
            [ngClass]="{ 'p-blockui-mask-document': !target, 'p-blockui p-blockui-mask p-overlay-mask': true }"
            [ngStyle]="{ display: 'none' }"
            [attr.data-pc-name]="'blockui'"
            [attr.data-pc-section]="'root'"
        >
            <ng-content></ng-content>
            <ng-container *ngTemplateOutlet="contentTemplate || _contentTemplate"></ng-container>
        </div>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [BlockUiStyle]
    }]
  }], () => [], {
    target: [{
      type: Input
    }],
    autoZIndex: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    baseZIndex: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    styleClass: [{
      type: Input
    }],
    blocked: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    contentTemplate: [{
      type: ContentChild,
      args: ["content", {
        descendants: false
      }]
    }],
    mask: [{
      type: ViewChild,
      args: ["mask"]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var BlockUIModule = class _BlockUIModule {
  static ɵfac = function BlockUIModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _BlockUIModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _BlockUIModule,
    imports: [BlockUI, SharedModule],
    exports: [BlockUI, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [BlockUI, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(BlockUIModule, [{
    type: NgModule,
    args: [{
      imports: [BlockUI, SharedModule],
      exports: [BlockUI, SharedModule]
    }]
  }], null, null);
})();
export {
  BlockUI,
  BlockUIClasses,
  BlockUIModule,
  BlockUiStyle
};
//# sourceMappingURL=primeng_blockui.js.map
