import {
  FocusTrap
} from "./chunk-CPGOCM5I.js";
import {
  zindexutils
} from "./chunk-KHDC5QWA.js";
import {
  EyeIcon,
  RefreshIcon,
  SearchMinusIcon,
  SearchPlusIcon,
  TimesIcon,
  UndoIcon
} from "./chunk-G7GR35G3.js";
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
  addClass,
  appendChild,
  blockBodyScroll,
  focus,
  unblockBodyScroll
} from "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import {
  animate,
  style,
  transition,
  trigger
} from "./chunk-PBR2IEBG.js";
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
  HostListener,
  Injectable,
  Input,
  NgModule,
  Output,
  ViewChild,
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
  ɵɵproperty,
  ɵɵpureFunction1,
  ɵɵpureFunction2,
  ɵɵqueryRefresh,
  ɵɵreference,
  ɵɵresetView,
  ɵɵresolveDocument,
  ɵɵrestoreView,
  ɵɵsanitizeUrl,
  ɵɵtemplate,
  ɵɵtemplateRefExtractor,
  ɵɵviewQuery
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-image.mjs
var _c0 = ["indicator"];
var _c1 = ["rotaterighticon"];
var _c2 = ["rotatelefticon"];
var _c3 = ["zoomouticon"];
var _c4 = ["zoominicon"];
var _c5 = ["closeicon"];
var _c6 = ["preview"];
var _c7 = ["image"];
var _c8 = ["mask"];
var _c9 = ["previewButton"];
var _c10 = ["closeButton"];
var _c11 = (a0) => ({
  errorCallback: a0
});
var _c12 = (a0, a1) => ({
  height: a0,
  width: a1
});
var _c13 = (a0) => ({
  "p-image-action p-image-zoom-out-button": true,
  "p-disabled": a0
});
var _c14 = (a0) => ({
  "p-image-action p-image-zoom-in-button": true,
  "p-disabled": a0
});
var _c15 = (a0, a1) => ({
  showTransitionParams: a0,
  hideTransitionParams: a1
});
var _c16 = (a0) => ({
  value: "visible",
  params: a0
});
var _c17 = (a0, a1) => ({
  class: "p-image-original",
  style: a0,
  previewCallback: a1
});
function Image_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementContainerStart(0);
    ɵɵelementStart(1, "img", 9);
    ɵɵlistener("error", function Image_ng_container_1_Template_img_error_1_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.imageError($event));
    });
    ɵɵelementEnd();
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵadvance();
    ɵɵclassMap(ctx_r1.imageClass);
    ɵɵproperty("ngStyle", ctx_r1.imageStyle);
    ɵɵattribute("src", ctx_r1.src, ɵɵsanitizeUrl)("srcset", ctx_r1.srcSet)("sizes", ctx_r1.sizes)("alt", ctx_r1.alt)("width", ctx_r1.width)("height", ctx_r1.height)("loading", ctx_r1.loading);
  }
}
function Image_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Image_button_3_ng_container_2_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Image_button_3_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, Image_button_3_ng_container_2_ng_container_1_Template, 1, 0, "ng-container", 12);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.indicatorTemplate || ctx_r1._indicatorTemplate);
  }
}
function Image_button_3_ng_template_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "EyeIcon", 13);
  }
  if (rf & 2) {
    ɵɵproperty("styleClass", "p-image-preview-icon");
  }
}
function Image_button_3_Template(rf, ctx) {
  if (rf & 1) {
    const _r3 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 10, 0);
    ɵɵlistener("click", function Image_button_3_Template_button_click_0_listener() {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onImageClick());
    });
    ɵɵtemplate(2, Image_button_3_ng_container_2_Template, 2, 1, "ng-container", 11)(3, Image_button_3_ng_template_3_Template, 1, 1, "ng-template", null, 1, ɵɵtemplateRefExtractor);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const defaultTemplate_r4 = ɵɵreference(4);
    const ctx_r1 = ɵɵnextContext();
    ɵɵproperty("ngStyle", ɵɵpureFunction2(4, _c12, ctx_r1.height + "px", ctx_r1.width + "px"));
    ɵɵattribute("aria-label", ctx_r1.zoomImageAriaLabel);
    ɵɵadvance(2);
    ɵɵproperty("ngIf", ctx_r1.indicatorTemplate || !ctx_r1._indicatorTemplate)("ngIfElse", defaultTemplate_r4);
  }
}
function Image_div_4_RefreshIcon_4_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "RefreshIcon");
  }
}
function Image_div_4_5_ng_template_0_Template(rf, ctx) {
}
function Image_div_4_5_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Image_div_4_5_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Image_div_4_UndoIcon_7_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "UndoIcon");
  }
}
function Image_div_4_8_ng_template_0_Template(rf, ctx) {
}
function Image_div_4_8_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Image_div_4_8_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Image_div_4_SearchMinusIcon_10_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "SearchMinusIcon");
  }
}
function Image_div_4_11_ng_template_0_Template(rf, ctx) {
}
function Image_div_4_11_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Image_div_4_11_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Image_div_4_SearchPlusIcon_13_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "SearchPlusIcon");
  }
}
function Image_div_4_14_ng_template_0_Template(rf, ctx) {
}
function Image_div_4_14_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Image_div_4_14_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Image_div_4_TimesIcon_17_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "TimesIcon");
  }
}
function Image_div_4_18_ng_template_0_Template(rf, ctx) {
}
function Image_div_4_18_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Image_div_4_18_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Image_div_4_div_19_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r7 = ɵɵgetCurrentView();
    ɵɵelementContainerStart(0);
    ɵɵelementStart(1, "img", 20);
    ɵɵlistener("click", function Image_div_4_div_19_ng_container_1_Template_img_click_1_listener() {
      ɵɵrestoreView(_r7);
      const ctx_r1 = ɵɵnextContext(3);
      return ɵɵresetView(ctx_r1.onPreviewImageClick());
    });
    ɵɵelementEnd();
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵadvance();
    ɵɵproperty("ngStyle", ctx_r1.imagePreviewStyle());
    ɵɵattribute("src", ctx_r1.previewImageSrc ? ctx_r1.previewImageSrc : ctx_r1.src, ɵɵsanitizeUrl)("srcset", ctx_r1.previewImageSrcSet)("sizes", ctx_r1.previewImageSizes);
  }
}
function Image_div_4_div_19_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Image_div_4_div_19_Template(rf, ctx) {
  if (rf & 1) {
    const _r6 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "div");
    ɵɵlistener("@animation.start", function Image_div_4_div_19_Template_div_animation_animation_start_0_listener($event) {
      ɵɵrestoreView(_r6);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.onAnimationStart($event));
    })("@animation.done", function Image_div_4_div_19_Template_div_animation_animation_done_0_listener($event) {
      ɵɵrestoreView(_r6);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.onAnimationEnd($event));
    });
    ɵɵtemplate(1, Image_div_4_div_19_ng_container_1_Template, 2, 4, "ng-container", 5)(2, Image_div_4_div_19_ng_container_2_Template, 1, 0, "ng-container", 6);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("@animation", ɵɵpureFunction1(7, _c16, ɵɵpureFunction2(4, _c15, ctx_r1.showTransitionOptions, ctx_r1.hideTransitionOptions)));
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.previewTemplate && !ctx_r1._previewTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.previewTemplate || ctx_r1._previewTemplate)("ngTemplateOutletContext", ɵɵpureFunction2(9, _c17, ctx_r1.imagePreviewStyle(), ctx_r1.onPreviewImageClick.bind(ctx_r1)));
  }
}
function Image_div_4_Template(rf, ctx) {
  if (rf & 1) {
    const _r5 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "div", 14, 2);
    ɵɵlistener("click", function Image_div_4_Template_div_click_0_listener() {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onMaskClick());
    })("keydown", function Image_div_4_Template_div_keydown_0_listener($event) {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onMaskKeydown($event));
    });
    ɵɵelementStart(2, "div", 15);
    ɵɵlistener("click", function Image_div_4_Template_div_click_2_listener($event) {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.handleToolbarClick($event));
    });
    ɵɵelementStart(3, "button", 16);
    ɵɵlistener("click", function Image_div_4_Template_button_click_3_listener() {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.rotateRight());
    });
    ɵɵtemplate(4, Image_div_4_RefreshIcon_4_Template, 1, 0, "RefreshIcon", 5)(5, Image_div_4_5_Template, 1, 0, null, 12);
    ɵɵelementEnd();
    ɵɵelementStart(6, "button", 17);
    ɵɵlistener("click", function Image_div_4_Template_button_click_6_listener() {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.rotateLeft());
    });
    ɵɵtemplate(7, Image_div_4_UndoIcon_7_Template, 1, 0, "UndoIcon", 5)(8, Image_div_4_8_Template, 1, 0, null, 12);
    ɵɵelementEnd();
    ɵɵelementStart(9, "button", 18);
    ɵɵlistener("click", function Image_div_4_Template_button_click_9_listener() {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.zoomOut());
    });
    ɵɵtemplate(10, Image_div_4_SearchMinusIcon_10_Template, 1, 0, "SearchMinusIcon", 5)(11, Image_div_4_11_Template, 1, 0, null, 12);
    ɵɵelementEnd();
    ɵɵelementStart(12, "button", 18);
    ɵɵlistener("click", function Image_div_4_Template_button_click_12_listener() {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.zoomIn());
    });
    ɵɵtemplate(13, Image_div_4_SearchPlusIcon_13_Template, 1, 0, "SearchPlusIcon", 5)(14, Image_div_4_14_Template, 1, 0, null, 12);
    ɵɵelementEnd();
    ɵɵelementStart(15, "button", 19, 3);
    ɵɵlistener("click", function Image_div_4_Template_button_click_15_listener() {
      ɵɵrestoreView(_r5);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.closePreview());
    });
    ɵɵtemplate(17, Image_div_4_TimesIcon_17_Template, 1, 0, "TimesIcon", 5)(18, Image_div_4_18_Template, 1, 0, null, 12);
    ɵɵelementEnd()();
    ɵɵtemplate(19, Image_div_4_div_19_Template, 3, 12, "div", 5);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵattribute("aria-modal", ctx_r1.maskVisible);
    ɵɵadvance(3);
    ɵɵattribute("aria-label", ctx_r1.rightAriaLabel());
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.rotateRightIconTemplate && !ctx_r1._rotateRightIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.rotateRightIconTemplate || ctx_r1._rotateRightIconTemplate);
    ɵɵadvance();
    ɵɵattribute("aria-label", ctx_r1.leftAriaLabel());
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.rotateLeftIconTemplate && !ctx_r1._rotateLeftIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.rotateLeftIconTemplate || ctx_r1._rotateLeftIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngClass", ɵɵpureFunction1(21, _c13, ctx_r1.isZoomOutDisabled))("disabled", ctx_r1.isZoomOutDisabled);
    ɵɵattribute("aria-label", ctx_r1.zoomOutAriaLabel());
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.zoomOutIconTemplate && !ctx_r1._zoomOutIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.zoomOutIconTemplate || ctx_r1._zoomOutIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngClass", ɵɵpureFunction1(23, _c14, ctx_r1.isZoomOutDisabled))("disabled", ctx_r1.isZoomInDisabled);
    ɵɵattribute("aria-label", ctx_r1.zoomInAriaLabel());
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.zoomInIconTemplate && !ctx_r1._zoomInIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.zoomInIconTemplate || ctx_r1._zoomInIconTemplate);
    ɵɵadvance();
    ɵɵattribute("aria-label", ctx_r1.closeAriaLabel());
    ɵɵadvance(2);
    ɵɵproperty("ngIf", !ctx_r1.closeIconTemplate && !ctx_r1._closeIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.closeIconTemplate || ctx_r1._closeIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.previewVisible);
  }
}
var theme = ({
  dt
}) => `
.p-image-mask {
    display: flex;
    align-items: center;
    justify-content: center;
}

.p-image-preview {
    position: relative;
    display: inline-flex;
    line-height: 0;
}

.p-image-preview-mask {
    position: absolute;
    inset-inline-start: 0;
    inset-block-start: 0;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.3s;
    border: 0 none;
    padding: 0;
    cursor: pointer;
    background: transparent;
    color: ${dt("image.preview.mask.color")};
    transition: background ${dt("image.transition.duration")};
}

.p-image-preview:hover > .p-image-preview-mask {
    opacity: 1;
    cursor: pointer;
    background: ${dt("image.preview.mask.background")};
}

.p-image-preview-icon {
    font-size: ${dt("image.preview.icon.size")};
    width: ${dt("image.preview.icon.size")};
    height: ${dt("image.preview.icon.size")};
}

.p-image-toolbar {
    position: absolute;
    inset-block-start: ${dt("image.toolbar.position.top")};
    inset-inline-end: ${dt("image.toolbar.position.right")};
    inset-inline-start: ${dt("image.toolbar.position.left")};
    inset-block-end: ${dt("image.toolbar.position.bottom")};
    display: flex;
    z-index: 1;
    padding: ${dt("image.toolbar.padding")};
    background: ${dt("image.toolbar.background")};
    backdrop-filter: blur(${dt("image.toolbar.blur")});
    border-color: ${dt("image.toolbar.border.color")};
    border-style: solid;
    border-width: ${dt("image.toolbar.border.width")};
    border-radius: ${dt("image.toolbar.border.radius")};
    gap: ${dt("image.toolbar.gap")};
}

.p-image-action {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    color: ${dt("image.action.color")};
    background: transparent;
    width: ${dt("image.action.size")};
    height: ${dt("image.action.size")};
    margin: 0;
    padding: 0;
    border: 0 none;
    cursor: pointer;
    user-select: none;
    border-radius: ${dt("image.action.border.radius")};
    outline-color: transparent;
    transition: background ${dt("image.transition.duration")}, color ${dt("image.transition.duration")}, outline-color ${dt("image.transition.duration")}, box-shadow ${dt("image.transition.duration")};
}

.p-image-action:hover {
    color: ${dt("image.action.hover.color")};
    background: ${dt("image.action.hover.background")};
}

.p-image-action:focus-visible {
    box-shadow: ${dt("toolbar.action.focus.ring.shadow")};
    outline: ${dt("toolbar.action.focus.ring.width")} ${dt("toolbar.action.focus.ring.style")} ${dt("toolbar.action.focus.ring.color")};
    outline-offset: ${dt("toolbar.action.focus.ring.offset")};
}

.p-image-action .p-icon {
    font-size: ${dt("image.action.icon.size")};
    width: ${dt("image.action.icon.size")};
    height: ${dt("image.action.icon.size")};
}

.p-image-action.p-disabled {
    pointer-events: auto;
}

.p-image-original {
    transition: transform 0.15s;
    max-width: 100vw;
    max-height: 100vh;
}

.p-image-original-enter-active {
    transition: all 150ms cubic-bezier(0, 0, 0.2, 1);
}

.p-image-original-leave-active {
    transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);
}

.p-image-original-enter-from,
.p-image-original-leave-to {
    opacity: 0;
    transform: scale(0.7);
}
`;
var classes = {
  root: ({
    props
  }) => ["p-image p-component", {
    "p-image-preview": props.preview
  }],
  previewMask: "p-image-preview-mask",
  previewIcon: "p-image-preview-icon",
  mask: "p-image-mask p-overlay-mask p-overlay-mask-enter",
  toolbar: "p-image-toolbar",
  rotateRightButton: "p-image-action p-image-rotate-right-button",
  rotateLeftButton: "p-image-action p-image-rotate-left-button",
  zoomOutButton: ({
    instance
  }) => ["p-image-action p-image-zoom-out-button", {
    "p-disabled": instance.isZoomOutDisabled
  }],
  zoomInButton: ({
    instance
  }) => ["p-image-action p-image-zoom-in-button", {
    "p-disabled": instance.isZoomInDisabled
  }],
  closeButton: "p-image-action p-image-close-button",
  original: "p-image-original"
};
var ImageStyle = class _ImageStyle extends BaseStyle {
  name = "image";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵImageStyle_BaseFactory;
    return function ImageStyle_Factory(__ngFactoryType__) {
      return (ɵImageStyle_BaseFactory || (ɵImageStyle_BaseFactory = ɵɵgetInheritedFactory(_ImageStyle)))(__ngFactoryType__ || _ImageStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _ImageStyle,
    factory: _ImageStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ImageStyle, [{
    type: Injectable
  }], null, null);
})();
var ImageClasses;
(function(ImageClasses2) {
  ImageClasses2["root"] = "p-image";
  ImageClasses2["previewMask"] = "p-image-preview-mask";
  ImageClasses2["previewIcon"] = "p-image-preview-icon";
  ImageClasses2["mask"] = "p-image-mask";
  ImageClasses2["toolbar"] = "p-image-toolbar";
  ImageClasses2["rotateRightButton"] = "p-image-rotate-right-button";
  ImageClasses2["rotateLeftButton"] = "p-image-rotate-left-button";
  ImageClasses2["zoomOutButton"] = "p-image-zoom-out-button";
  ImageClasses2["zoomInButton"] = "p-image-zoom-in-button";
  ImageClasses2["closeButton"] = "p-image-close-button";
  ImageClasses2["original"] = "p-image-original";
})(ImageClasses || (ImageClasses = {}));
var Image = class _Image extends BaseComponent {
  /**
   * Style class of the image element.
   * @group Props
   */
  imageClass;
  /**
   * Inline style of the image element.
   * @group Props
   */
  imageStyle;
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
   * The source path for the main image.
   * @group Props
   */
  src;
  /**
   * The srcset definition for the main image.
   * @group Props
   */
  srcSet;
  /**
   * The sizes definition for the main image.
   * @group Props
   */
  sizes;
  /**
   * The source path for the preview image.
   * @group Props
   */
  previewImageSrc;
  /**
   * The srcset definition for the preview image.
   * @group Props
   */
  previewImageSrcSet;
  /**
   * The sizes definition for the preview image.
   * @group Props
   */
  previewImageSizes;
  /**
   * Attribute of the preview image element.
   * @group Props
   */
  alt;
  /**
   * Attribute of the image element.
   * @group Props
   */
  width;
  /**
   * Attribute of the image element.
   * @group Props
   */
  height;
  /**
   * Attribute of the image element.
   * @group Props
   */
  loading;
  /**
   * Target element to attach the dialog, valid values are "body" or a local ng-template variable of another element (note: use binding with brackets for template variables, e.g. [appendTo]="mydiv" for a div element having #mydiv as variable name).
   * @group Props
   */
  appendTo;
  /**
   * Controls the preview functionality.
   * @group Props
   */
  preview = false;
  /**
   * Transition options of the show animation
   * @group Props
   */
  showTransitionOptions = "150ms cubic-bezier(0, 0, 0.2, 1)";
  /**
   * Transition options of the hide animation
   * @group Props
   */
  hideTransitionOptions = "150ms cubic-bezier(0, 0, 0.2, 1)";
  /**
   * Triggered when the preview overlay is shown.
   * @group Emits
   */
  onShow = new EventEmitter();
  /**
   * Triggered when the preview overlay is hidden.
   * @group Emits
   */
  onHide = new EventEmitter();
  /**
   * This event is triggered if an error occurs while loading an image file.
   * @param {Event} event - Browser event.
   * @group Emits
   */
  onImageError = new EventEmitter();
  mask;
  previewButton;
  closeButton;
  /**
   * Custom template of indicator.
   * @group Templates
   */
  indicatorTemplate;
  /**
   * Custom template of rotaterighticon.
   * @group Templates
   */
  rotateRightIconTemplate;
  /**
   * Custom template of rotatelefticon.
   * @group Templates
   */
  rotateLeftIconTemplate;
  /**
   * Custom template of zoomouticon.
   * @group Templates
   */
  zoomOutIconTemplate;
  /**
   * Custom template of zoominicon.
   * @group Templates
   */
  zoomInIconTemplate;
  /**
   * Custom template of closeicon.
   * @group Templates
   */
  closeIconTemplate;
  /**
   * Custom template of preview.
   * @group Templates
   */
  previewTemplate;
  /**
   * Custom template of image.
   * @group Templates
   */
  imageTemplate;
  maskVisible = false;
  previewVisible = false;
  rotate = 0;
  scale = 1;
  previewClick = false;
  container;
  wrapper;
  _componentStyle = inject(ImageStyle);
  get isZoomOutDisabled() {
    return this.scale - this.zoomSettings.step <= this.zoomSettings.min;
  }
  get isZoomInDisabled() {
    return this.scale + this.zoomSettings.step >= this.zoomSettings.max;
  }
  zoomSettings = {
    default: 1,
    step: 0.1,
    max: 1.5,
    min: 0.5
  };
  constructor() {
    super();
  }
  templates;
  _indicatorTemplate;
  _rotateRightIconTemplate;
  _rotateLeftIconTemplate;
  _zoomOutIconTemplate;
  _zoomInIconTemplate;
  _closeIconTemplate;
  _imageTemplate;
  _previewTemplate;
  ngAfterContentInit() {
    this.templates?.forEach((item) => {
      switch (item.getType()) {
        case "indicator":
          this._indicatorTemplate = item.template;
          break;
        case "rotaterighticon":
          this._rotateRightIconTemplate = item.template;
          break;
        case "rotatelefticon":
          this._rotateLeftIconTemplate = item.template;
          break;
        case "zoomouticon":
          this._zoomOutIconTemplate = item.template;
          break;
        case "zoominicon":
          this._zoomInIconTemplate = item.template;
          break;
        case "closeicon":
          this._closeIconTemplate = item.template;
          break;
        case "image":
          this._imageTemplate = item.template;
          break;
        case "preview":
          this._previewTemplate = item.template;
          break;
        default:
          this._indicatorTemplate = item.template;
          break;
      }
    });
  }
  onImageClick() {
    if (this.preview) {
      this.maskVisible = true;
      this.previewVisible = true;
      blockBodyScroll();
    }
  }
  onMaskClick() {
    if (!this.previewClick) {
      this.closePreview();
    }
    this.previewClick = false;
  }
  onMaskKeydown(event) {
    switch (event.code) {
      case "Escape":
        this.onMaskClick();
        setTimeout(() => {
          focus(this.previewButton.nativeElement);
        }, 25);
        event.preventDefault();
        break;
      default:
        break;
    }
  }
  onPreviewImageClick() {
    this.previewClick = true;
  }
  rotateRight() {
    this.rotate += 90;
    this.previewClick = true;
  }
  rotateLeft() {
    this.rotate -= 90;
    this.previewClick = true;
  }
  zoomIn() {
    this.scale = this.scale + this.zoomSettings.step;
    this.previewClick = true;
  }
  zoomOut() {
    this.scale = this.scale - this.zoomSettings.step;
    this.previewClick = true;
  }
  onAnimationStart(event) {
    switch (event.toState) {
      case "visible":
        this.container = event.element;
        this.wrapper = this.container?.parentElement;
        this.appendContainer();
        this.moveOnTop();
        setTimeout(() => {
          focus(this.closeButton.nativeElement);
        }, 25);
        break;
      case "void":
        addClass(this.wrapper, "p-overlay-mask-leave");
        break;
    }
  }
  onAnimationEnd(event) {
    switch (event.toState) {
      case "void":
        zindexutils.clear(this.wrapper);
        this.maskVisible = false;
        this.container = null;
        this.wrapper = null;
        this.cd.markForCheck();
        this.onHide.emit({});
        break;
      case "visible":
        this.onShow.emit({});
        break;
    }
  }
  moveOnTop() {
    zindexutils.set("modal", this.wrapper, this.config.zIndex.modal);
  }
  appendContainer() {
    if (this.appendTo) {
      if (this.appendTo === "body") this.document.body.appendChild(this.wrapper);
      else appendChild(this.appendTo, this.wrapper);
    }
  }
  imagePreviewStyle() {
    return {
      transform: "rotate(" + this.rotate + "deg) scale(" + this.scale + ")"
    };
  }
  get zoomImageAriaLabel() {
    return this.config.translation.aria ? this.config.translation.aria.zoomImage : void 0;
  }
  containerClass() {
    return {
      "p-image p-component": true,
      "p-image-preview": this.preview
    };
  }
  handleToolbarClick(event) {
    event.stopPropagation();
  }
  closePreview() {
    this.previewVisible = false;
    this.rotate = 0;
    this.scale = this.zoomSettings.default;
    unblockBodyScroll();
  }
  imageError(event) {
    this.onImageError.emit(event);
  }
  rightAriaLabel() {
    return this.config.translation.aria ? this.config.translation.aria.rotateRight : void 0;
  }
  leftAriaLabel() {
    return this.config.translation.aria ? this.config.translation.aria.rotateLeft : void 0;
  }
  zoomInAriaLabel() {
    return this.config.translation.aria ? this.config.translation.aria.zoomIn : void 0;
  }
  zoomOutAriaLabel() {
    return this.config.translation.aria ? this.config.translation.aria.zoomOut : void 0;
  }
  closeAriaLabel() {
    return this.config.translation.aria ? this.config.translation.aria.close : void 0;
  }
  onKeydownHandler(event) {
    if (this.previewVisible) {
      this.closePreview();
    }
  }
  static ɵfac = function Image_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _Image)();
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _Image,
    selectors: [["p-image"]],
    contentQueries: function Image_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c0, 4);
        ɵɵcontentQuery(dirIndex, _c1, 4);
        ɵɵcontentQuery(dirIndex, _c2, 4);
        ɵɵcontentQuery(dirIndex, _c3, 4);
        ɵɵcontentQuery(dirIndex, _c4, 4);
        ɵɵcontentQuery(dirIndex, _c5, 4);
        ɵɵcontentQuery(dirIndex, _c6, 4);
        ɵɵcontentQuery(dirIndex, _c7, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.indicatorTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.rotateRightIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.rotateLeftIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.zoomOutIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.zoomInIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.closeIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.previewTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.imageTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    viewQuery: function Image_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c8, 5);
        ɵɵviewQuery(_c9, 5);
        ɵɵviewQuery(_c10, 5);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.mask = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.previewButton = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.closeButton = _t.first);
      }
    },
    hostBindings: function Image_HostBindings(rf, ctx) {
      if (rf & 1) {
        ɵɵlistener("keydown.escape", function Image_keydown_escape_HostBindingHandler($event) {
          return ctx.onKeydownHandler($event);
        }, false, ɵɵresolveDocument);
      }
    },
    inputs: {
      imageClass: "imageClass",
      imageStyle: "imageStyle",
      styleClass: "styleClass",
      style: "style",
      src: "src",
      srcSet: "srcSet",
      sizes: "sizes",
      previewImageSrc: "previewImageSrc",
      previewImageSrcSet: "previewImageSrcSet",
      previewImageSizes: "previewImageSizes",
      alt: "alt",
      width: "width",
      height: "height",
      loading: "loading",
      appendTo: "appendTo",
      preview: [2, "preview", "preview", booleanAttribute],
      showTransitionOptions: "showTransitionOptions",
      hideTransitionOptions: "hideTransitionOptions"
    },
    outputs: {
      onShow: "onShow",
      onHide: "onHide",
      onImageError: "onImageError"
    },
    features: [ɵɵProvidersFeature([ImageStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    decls: 5,
    vars: 11,
    consts: [["previewButton", ""], ["defaultTemplate", ""], ["mask", ""], ["closeButton", ""], [3, "ngClass", "ngStyle"], [4, "ngIf"], [4, "ngTemplateOutlet", "ngTemplateOutletContext"], ["type", "button", "class", "p-image-preview-mask", 3, "ngStyle", "click", 4, "ngIf"], ["class", "p-image-mask p-overlay-mask p-overlay-mask-enter", "role", "dialog", "pFocusTrap", "", 3, "click", "keydown", 4, "ngIf"], [3, "error", "ngStyle"], ["type", "button", 1, "p-image-preview-mask", 3, "click", "ngStyle"], [4, "ngIf", "ngIfElse"], [4, "ngTemplateOutlet"], [3, "styleClass"], ["role", "dialog", "pFocusTrap", "", 1, "p-image-mask", "p-overlay-mask", "p-overlay-mask-enter", 3, "click", "keydown"], [1, "p-image-toolbar", 3, "click"], ["type", "button", 1, "p-image-action", "p-image-rotate-right-button", 3, "click"], ["type", "button", 1, "p-image-action", "p-image-rotate-left-button", 3, "click"], ["type", "button", 3, "click", "ngClass", "disabled"], ["type", "button", 1, "p-image-action", "p-image-close-button", 3, "click"], [1, "p-image-original", 3, "click", "ngStyle"]],
    template: function Image_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵelementStart(0, "span", 4);
        ɵɵtemplate(1, Image_ng_container_1_Template, 2, 10, "ng-container", 5)(2, Image_ng_container_2_Template, 1, 0, "ng-container", 6)(3, Image_button_3_Template, 5, 7, "button", 7)(4, Image_div_4_Template, 20, 25, "div", 8);
        ɵɵelementEnd();
      }
      if (rf & 2) {
        ɵɵclassMap(ctx.styleClass);
        ɵɵproperty("ngClass", ctx.containerClass())("ngStyle", ctx.style);
        ɵɵadvance();
        ɵɵproperty("ngIf", !ctx.imageTemplate && !ctx._imageTemplate);
        ɵɵadvance();
        ɵɵproperty("ngTemplateOutlet", ctx.imageTemplate || ctx._imageTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(9, _c11, ctx.imageError.bind(ctx)));
        ɵɵadvance();
        ɵɵproperty("ngIf", ctx.preview);
        ɵɵadvance();
        ɵɵproperty("ngIf", ctx.maskVisible);
      }
    },
    dependencies: [CommonModule, NgClass, NgIf, NgTemplateOutlet, NgStyle, RefreshIcon, EyeIcon, UndoIcon, SearchMinusIcon, SearchPlusIcon, TimesIcon, FocusTrap, SharedModule],
    encapsulation: 2,
    data: {
      animation: [trigger("animation", [transition("void => visible", [style({
        transform: "scale(0.7)",
        opacity: 0
      }), animate("{{showTransitionParams}}")]), transition("visible => void", [animate("{{hideTransitionParams}}", style({
        transform: "scale(0.7)",
        opacity: 0
      }))])])]
    },
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(Image, [{
    type: Component,
    args: [{
      selector: "p-image",
      standalone: true,
      imports: [CommonModule, RefreshIcon, EyeIcon, UndoIcon, SearchMinusIcon, SearchPlusIcon, TimesIcon, FocusTrap, SharedModule],
      template: `
        <span [ngClass]="containerClass()" [class]="styleClass" [ngStyle]="style">
            <ng-container *ngIf="!imageTemplate && !_imageTemplate">
                <img [attr.src]="src" [attr.srcset]="srcSet" [attr.sizes]="sizes" [attr.alt]="alt" [attr.width]="width" [attr.height]="height" [attr.loading]="loading" [ngStyle]="imageStyle" [class]="imageClass" (error)="imageError($event)" />
            </ng-container>

            <ng-container *ngTemplateOutlet="imageTemplate || _imageTemplate; context: { errorCallback: imageError.bind(this) }"></ng-container>

            <button *ngIf="preview" [attr.aria-label]="zoomImageAriaLabel" type="button" class="p-image-preview-mask" (click)="onImageClick()" #previewButton [ngStyle]="{ height: height + 'px', width: width + 'px' }">
                <ng-container *ngIf="indicatorTemplate || !_indicatorTemplate; else defaultTemplate">
                    <ng-container *ngTemplateOutlet="indicatorTemplate || _indicatorTemplate"></ng-container>
                </ng-container>
                <ng-template #defaultTemplate>
                    <EyeIcon [styleClass]="'p-image-preview-icon'" />
                </ng-template>
            </button>
            <div #mask class="p-image-mask p-overlay-mask p-overlay-mask-enter" *ngIf="maskVisible" [attr.aria-modal]="maskVisible" role="dialog" (click)="onMaskClick()" (keydown)="onMaskKeydown($event)" pFocusTrap>
                <div class="p-image-toolbar" (click)="handleToolbarClick($event)">
                    <button class="p-image-action p-image-rotate-right-button" (click)="rotateRight()" type="button" [attr.aria-label]="rightAriaLabel()">
                        <RefreshIcon *ngIf="!rotateRightIconTemplate && !_rotateRightIconTemplate" />
                        <ng-template *ngTemplateOutlet="rotateRightIconTemplate || _rotateRightIconTemplate"></ng-template>
                    </button>
                    <button class="p-image-action p-image-rotate-left-button" (click)="rotateLeft()" type="button" [attr.aria-label]="leftAriaLabel()">
                        <UndoIcon *ngIf="!rotateLeftIconTemplate && !_rotateLeftIconTemplate" />
                        <ng-template *ngTemplateOutlet="rotateLeftIconTemplate || _rotateLeftIconTemplate"></ng-template>
                    </button>
                    <button [ngClass]="{ 'p-image-action p-image-zoom-out-button': true, 'p-disabled': isZoomOutDisabled }" (click)="zoomOut()" type="button" [disabled]="isZoomOutDisabled" [attr.aria-label]="zoomOutAriaLabel()">
                        <SearchMinusIcon *ngIf="!zoomOutIconTemplate && !_zoomOutIconTemplate" />
                        <ng-template *ngTemplateOutlet="zoomOutIconTemplate || _zoomOutIconTemplate"></ng-template>
                    </button>
                    <button [ngClass]="{ 'p-image-action p-image-zoom-in-button': true, 'p-disabled': isZoomOutDisabled }" (click)="zoomIn()" type="button" [disabled]="isZoomInDisabled" [attr.aria-label]="zoomInAriaLabel()">
                        <SearchPlusIcon *ngIf="!zoomInIconTemplate && !_zoomInIconTemplate" />
                        <ng-template *ngTemplateOutlet="zoomInIconTemplate || _zoomInIconTemplate"></ng-template>
                    </button>
                    <button class="p-image-action p-image-close-button" type="button" (click)="closePreview()" [attr.aria-label]="closeAriaLabel()" #closeButton>
                        <TimesIcon *ngIf="!closeIconTemplate && !_closeIconTemplate" />
                        <ng-template *ngTemplateOutlet="closeIconTemplate || _closeIconTemplate"></ng-template>
                    </button>
                </div>
                <div
                    *ngIf="previewVisible"
                    [@animation]="{
                        value: 'visible',
                        params: { showTransitionParams: showTransitionOptions, hideTransitionParams: hideTransitionOptions }
                    }"
                    (@animation.start)="onAnimationStart($event)"
                    (@animation.done)="onAnimationEnd($event)"
                >
                    <ng-container *ngIf="!previewTemplate && !_previewTemplate">
                        <img [attr.src]="previewImageSrc ? previewImageSrc : src" [attr.srcset]="previewImageSrcSet" [attr.sizes]="previewImageSizes" class="p-image-original" [ngStyle]="imagePreviewStyle()" (click)="onPreviewImageClick()" />
                    </ng-container>
                    <ng-container
                        *ngTemplateOutlet="
                            previewTemplate || _previewTemplate;
                            context: {
                                class: 'p-image-original',
                                style: imagePreviewStyle(),
                                previewCallback: onPreviewImageClick.bind(this)
                            }
                        "
                    >
                    </ng-container>
                </div>
            </div>
        </span>
    `,
      animations: [trigger("animation", [transition("void => visible", [style({
        transform: "scale(0.7)",
        opacity: 0
      }), animate("{{showTransitionParams}}")]), transition("visible => void", [animate("{{hideTransitionParams}}", style({
        transform: "scale(0.7)",
        opacity: 0
      }))])])],
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [ImageStyle]
    }]
  }], () => [], {
    imageClass: [{
      type: Input
    }],
    imageStyle: [{
      type: Input
    }],
    styleClass: [{
      type: Input
    }],
    style: [{
      type: Input
    }],
    src: [{
      type: Input
    }],
    srcSet: [{
      type: Input
    }],
    sizes: [{
      type: Input
    }],
    previewImageSrc: [{
      type: Input
    }],
    previewImageSrcSet: [{
      type: Input
    }],
    previewImageSizes: [{
      type: Input
    }],
    alt: [{
      type: Input
    }],
    width: [{
      type: Input
    }],
    height: [{
      type: Input
    }],
    loading: [{
      type: Input
    }],
    appendTo: [{
      type: Input
    }],
    preview: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    showTransitionOptions: [{
      type: Input
    }],
    hideTransitionOptions: [{
      type: Input
    }],
    onShow: [{
      type: Output
    }],
    onHide: [{
      type: Output
    }],
    onImageError: [{
      type: Output
    }],
    mask: [{
      type: ViewChild,
      args: ["mask"]
    }],
    previewButton: [{
      type: ViewChild,
      args: ["previewButton"]
    }],
    closeButton: [{
      type: ViewChild,
      args: ["closeButton"]
    }],
    indicatorTemplate: [{
      type: ContentChild,
      args: ["indicator", {
        descendants: false
      }]
    }],
    rotateRightIconTemplate: [{
      type: ContentChild,
      args: ["rotaterighticon", {
        descendants: false
      }]
    }],
    rotateLeftIconTemplate: [{
      type: ContentChild,
      args: ["rotatelefticon", {
        descendants: false
      }]
    }],
    zoomOutIconTemplate: [{
      type: ContentChild,
      args: ["zoomouticon", {
        descendants: false
      }]
    }],
    zoomInIconTemplate: [{
      type: ContentChild,
      args: ["zoominicon", {
        descendants: false
      }]
    }],
    closeIconTemplate: [{
      type: ContentChild,
      args: ["closeicon", {
        descendants: false
      }]
    }],
    previewTemplate: [{
      type: ContentChild,
      args: ["preview", {
        descendants: false
      }]
    }],
    imageTemplate: [{
      type: ContentChild,
      args: ["image", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }],
    onKeydownHandler: [{
      type: HostListener,
      args: ["document:keydown.escape", ["$event"]]
    }]
  });
})();
var ImageModule = class _ImageModule {
  static ɵfac = function ImageModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ImageModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _ImageModule,
    imports: [Image, SharedModule],
    exports: [Image, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [Image, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ImageModule, [{
    type: NgModule,
    args: [{
      imports: [Image, SharedModule],
      exports: [Image, SharedModule]
    }]
  }], null, null);
})();
export {
  Image,
  ImageClasses,
  ImageModule,
  ImageStyle
};
//# sourceMappingURL=primeng_image.js.map
