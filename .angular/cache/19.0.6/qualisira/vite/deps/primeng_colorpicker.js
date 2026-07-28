import {
  zindexutils
} from "./chunk-KHDC5QWA.js";
import {
  AutoFocus,
  AutoFocusModule
} from "./chunk-K3LJODM5.js";
import {
  ConnectedOverlayScrollHandler
} from "./chunk-5G7WYC4N.js";
import {
  BaseComponent
} from "./chunk-NKPC7AFX.js";
import "./chunk-3RXOYKMS.js";
import {
  BaseStyle
} from "./chunk-2GIPMIFT.js";
import {
  OverlayService,
  SharedModule,
  TranslationKeys
} from "./chunk-NG5OZP5M.js";
import {
  absolutePosition,
  appendChild,
  isTouchDevice,
  relativePosition
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
  NG_VALUE_ACCESSOR
} from "./chunk-Q4QQ2CLE.js";
import {
  CommonModule,
  NgClass,
  NgIf,
  NgStyle,
  isPlatformBrowser
} from "./chunk-YQX3S2V2.js";
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Injectable,
  Input,
  NgModule,
  Output,
  ViewChild,
  ViewEncapsulation,
  booleanAttribute,
  forwardRef,
  inject,
  numberAttribute,
  setClassMetadata,
  ɵɵInheritDefinitionFeature,
  ɵɵInputTransformsFeature,
  ɵɵProvidersFeature,
  ɵɵadvance,
  ɵɵattribute,
  ɵɵclassMap,
  ɵɵdefineComponent,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵelement,
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
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵstyleProp,
  ɵɵtemplate,
  ɵɵviewQuery
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-colorpicker.mjs
var _c0 = ["container"];
var _c1 = ["input"];
var _c2 = ["colorSelector"];
var _c3 = ["colorHandle"];
var _c4 = ["hue"];
var _c5 = ["hueHandle"];
var _c6 = (a0, a1) => ({
  "p-colorpicker p-component": true,
  "p-colorpicker-overlay": a0,
  "p-colorpicker-dragging": a1
});
var _c7 = (a0) => ({
  "p-disabled": a0
});
var _c8 = (a0, a1) => ({
  "p-colorpicker-panel": true,
  "p-colorpicker-panel-inline": a0,
  "p-disabled": a1
});
var _c9 = (a0, a1) => ({
  showTransitionParams: a0,
  hideTransitionParams: a1
});
var _c10 = (a0) => ({
  value: "visible",
  params: a0
});
function ColorPicker_input_2_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "input", 9, 1);
    ɵɵlistener("click", function ColorPicker_input_2_Template_input_click_0_listener() {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onInputClick());
    })("keydown", function ColorPicker_input_2_Template_input_keydown_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onInputKeydown($event));
    })("focus", function ColorPicker_input_2_Template_input_focus_0_listener() {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onInputFocus());
    });
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵstyleProp("background-color", ctx_r1.inputBgColor);
    ɵɵproperty("ngClass", ɵɵpureFunction1(9, _c7, ctx_r1.disabled))("disabled", ctx_r1.disabled)("pAutoFocus", ctx_r1.autofocus);
    ɵɵattribute("tabindex", ctx_r1.tabindex)("id", ctx_r1.inputId)("data-pc-section", "input")("aria-label", ctx_r1.ariaLabel);
  }
}
function ColorPicker_div_3_Template(rf, ctx) {
  if (rf & 1) {
    const _r3 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "div", 10);
    ɵɵlistener("click", function ColorPicker_div_3_Template_div_click_0_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onOverlayClick($event));
    })("@overlayAnimation.start", function ColorPicker_div_3_Template_div_animation_overlayAnimation_start_0_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onOverlayAnimationStart($event));
    })("@overlayAnimation.done", function ColorPicker_div_3_Template_div_animation_overlayAnimation_done_0_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onOverlayAnimationEnd($event));
    });
    ɵɵelementStart(1, "div", 11)(2, "div", 12, 2);
    ɵɵlistener("touchstart", function ColorPicker_div_3_Template_div_touchstart_2_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onColorDragStart($event));
    })("touchmove", function ColorPicker_div_3_Template_div_touchmove_2_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onDrag($event));
    })("touchend", function ColorPicker_div_3_Template_div_touchend_2_listener() {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onDragEnd());
    })("mousedown", function ColorPicker_div_3_Template_div_mousedown_2_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onColorMousedown($event));
    });
    ɵɵelementStart(4, "div", 13);
    ɵɵelement(5, "div", 14, 3);
    ɵɵelementEnd()();
    ɵɵelementStart(7, "div", 15, 4);
    ɵɵlistener("mousedown", function ColorPicker_div_3_Template_div_mousedown_7_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onHueMousedown($event));
    })("touchstart", function ColorPicker_div_3_Template_div_touchstart_7_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onHueDragStart($event));
    })("touchmove", function ColorPicker_div_3_Template_div_touchmove_7_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onDrag($event));
    })("touchend", function ColorPicker_div_3_Template_div_touchend_7_listener() {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onDragEnd());
    });
    ɵɵelement(9, "div", 16, 5);
    ɵɵelementEnd()()();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵproperty("ngClass", ɵɵpureFunction2(10, _c8, ctx_r1.inline, ctx_r1.disabled))("@overlayAnimation", ɵɵpureFunction1(16, _c10, ɵɵpureFunction2(13, _c9, ctx_r1.showTransitionOptions, ctx_r1.hideTransitionOptions)))("@.disabled", ctx_r1.inline === true);
    ɵɵattribute("data-pc-section", "panel");
    ɵɵadvance();
    ɵɵattribute("data-pc-section", "content");
    ɵɵadvance();
    ɵɵattribute("data-pc-section", "selector");
    ɵɵadvance(2);
    ɵɵattribute("data-pc-section", "color");
    ɵɵadvance();
    ɵɵattribute("data-pc-section", "colorHandle");
    ɵɵadvance(2);
    ɵɵattribute("data-pc-section", "hue");
    ɵɵadvance(2);
    ɵɵattribute("data-pc-section", "hueHandle");
  }
}
var theme = ({
  dt
}) => `
.p-colorpicker {
    display: inline-block;
    position: relative;
}

.p-colorpicker-dragging {
    cursor: pointer;
}

.p-colorpicker-preview {
    width: ${dt("colorpicker.preview.width")};
    height: ${dt("colorpicker.preview.height")};
    padding: 0;
    border: 0 none;
    border-radius: ${dt("colorpicker.preview.border.radius")};
    transition: background ${dt("colorpicker.transition.duration")}, color ${dt("colorpicker.transition.duration")}, border-color ${dt("colorpicker.transition.duration")}, outline-color ${dt("colorpicker.transition.duration")}, box-shadow ${dt("colorpicker.transition.duration")};
    outline-color: transparent;
    cursor: pointer;
}

.p-colorpicker-preview:enabled:focus-visible {
    border-color: ${dt("colorpicker.preview.focus.border.color")};
    box-shadow: ${dt("colorpicker.preview.focus.ring.shadow")};
    outline: ${dt("colorpicker.preview.focus.ring.width")} ${dt("colorpicker.preview.focus.ring.style")} ${dt("colorpicker.preview.focus.ring.color")};
    outline-offset: ${dt("colorpicker.preview.focus.ring.offset")};
}

.p-colorpicker-panel {
    background: ${dt("colorpicker.panel.background")};
    border: 1px solid ${dt("colorpicker.panel.border.color")};
    border-radius: ${dt("colorpicker.panel.border.radius")};
    box-shadow: ${dt("colorpicker.panel.shadow")};
    width: 193px;
    height: 166px;
    position: absolute;
    top: 0;
    left: 0;
}

.p-colorpicker-panel:dir(rtl) {
    left: auto;
    right: 0;
}

.p-colorpicker-panel-inline {
    box-shadow: none;
    position: static;
}

.p-colorpicker-content {
    position: relative;
}

.p-colorpicker-color-selector {
    width: 150px;
    height: 150px;
    top: 8px;
    left: 8px;
    position: absolute;
}

.p-colorpicker-color-selector:dir(rtl) {
    left: auto;
    right: 8px;
}

.p-colorpicker-color-background {
    width: 100%;
    height: 100%;
    background: linear-gradient(to top, #000 0%, rgba(0, 0, 0, 0) 100%), linear-gradient(to right, #fff 0%, rgba(255, 255, 255, 0) 100%);
}

.p-colorpicker-color-handle {
    position: absolute;
    top: 0px;
    left: 150px;
    border-radius: 100%;
    width: 10px;
    height: 10px;
    border-width: 1px;
    border-style: solid;
    margin: -5px 0 0 -5px;
    cursor: pointer;
    opacity: 0.85;
    border-color: ${dt("colorpicker.handle.color")};
}

.p-colorpicker-color-handle:dir(rtl) {
    left: auto;
    right: 150px;
    margin: -5px -5px 0 0;
}

.p-colorpicker-hue {
    width: 17px;
    height: 150px;
    top: 8px;
    left: 167px;
    position: absolute;
    opacity: 0.85;
    background: linear-gradient(0deg,
        red 0,
        #ff0 17%,
        #0f0 33%,
        #0ff 50%,
        #00f 67%,
        #f0f 83%,
        red);
}

.p-colorpicker-hue:dir(rtl) {
    left: auto;
    right: 167px;
}

.p-colorpicker-hue-handle {
    position: absolute;
    top: 150px;
    left: 0px;
    width: 21px;
    margin-left: -2px;
    margin-top: -5px;
    height: 10px;
    border-width: 2px;
    border-style: solid;
    opacity: 0.85;
    cursor: pointer;
    border-color: ${dt("colorpicker.handle.color")};
}

.p-colorpicker-hue-handle:dir(rtl) {
    left: auto;
    right: 0px;
    margin-left: 0;
    margin-right: -2px;
}
`;
var classes = {
  root: "p-colorpicker p-component",
  preview: ({
    props
  }) => ["p-colorpicker-preview", {
    "p-disabled": props.disabled
  }],
  panel: ({
    props
  }) => ["p-colorpicker-panel", {
    "p-colorpicker-panel-inline": props.inline,
    "p-disabled": props.disabled
  }],
  content: "p-colorpicker-content",
  colorSelector: "p-colorpicker-color-selector",
  colorBackground: "p-colorpicker-color-background",
  colorHandle: "p-colorpicker-color-handle",
  hue: "p-colorpicker-hue",
  hueHandle: "p-colorpicker-hue-handle"
};
var ColorPickerStyle = class _ColorPickerStyle extends BaseStyle {
  name = "colorpicker";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵColorPickerStyle_BaseFactory;
    return function ColorPickerStyle_Factory(__ngFactoryType__) {
      return (ɵColorPickerStyle_BaseFactory || (ɵColorPickerStyle_BaseFactory = ɵɵgetInheritedFactory(_ColorPickerStyle)))(__ngFactoryType__ || _ColorPickerStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _ColorPickerStyle,
    factory: _ColorPickerStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ColorPickerStyle, [{
    type: Injectable
  }], null, null);
})();
var ColorPickerClasses;
(function(ColorPickerClasses2) {
  ColorPickerClasses2["root"] = "p-colorpicker";
  ColorPickerClasses2["preview"] = "p-colorpicker-preview";
  ColorPickerClasses2["panel"] = "p-colorpicker-panel";
  ColorPickerClasses2["colorSelector"] = "p-colorpicker-color-selector";
  ColorPickerClasses2["colorBackground"] = "p-colorpicker-color-background";
  ColorPickerClasses2["colorHandle"] = "p-colorpicker-color-handle";
  ColorPickerClasses2["hue"] = "p-colorpicker-hue";
  ColorPickerClasses2["hueHandle"] = "p-colorpicker-hue-handle";
})(ColorPickerClasses || (ColorPickerClasses = {}));
var COLORPICKER_VALUE_ACCESSOR = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => ColorPicker),
  multi: true
};
var ColorPicker = class _ColorPicker extends BaseComponent {
  overlayService;
  /**
   * Inline style of the component.
   * @group Props
   */
  style;
  /**
   * Style class of the component.
   * @group Props
   */
  styleClass;
  /**
   * Whether to display as an overlay or not.
   * @group Props
   */
  inline;
  /**
   * Format to use in value binding.
   * @group Props
   */
  format = "hex";
  /**
   * Target element to attach the overlay, valid values are "body" or a local ng-template variable of another element (note: use binding with brackets for template variables, e.g. [appendTo]="mydiv" for a div element having #mydiv as variable name).
   * @group Props
   */
  appendTo;
  /**
   * When present, it specifies that the component should be disabled.
   * @group Props
   */
  disabled;
  /**
   * Index of the element in tabbing order.
   * @group Props
   */
  tabindex;
  /**
   * Identifier of the focus input to match a label defined for the dropdown.
   * @group Props
   */
  inputId;
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
   * Transition options of the show animation.
   * @group Props
   */
  showTransitionOptions = ".12s cubic-bezier(0, 0, 0.2, 1)";
  /**
   * Transition options of the hide animation.
   * @group Props
   */
  hideTransitionOptions = ".1s linear";
  /**
   * When present, it specifies that the component should automatically get focus on load.
   * @group Props
   */
  autofocus;
  /**
   * Callback to invoke on value change.
   * @param {ColorPickerChangeEvent} event - Custom value change event.
   * @group Emits
   */
  onChange = new EventEmitter();
  /**
   * Callback to invoke on panel is shown.
   * @group Emits
   */
  onShow = new EventEmitter();
  /**
   * Callback to invoke on panel is hidden.
   * @group Emits
   */
  onHide = new EventEmitter();
  containerViewChild;
  inputViewChild;
  value = {
    h: 0,
    s: 100,
    b: 100
  };
  inputBgColor;
  shown;
  overlayVisible;
  defaultColor = "ff0000";
  onModelChange = () => {
  };
  onModelTouched = () => {
  };
  documentClickListener;
  documentResizeListener;
  documentMousemoveListener;
  documentMouseupListener;
  documentHueMoveListener;
  scrollHandler;
  selfClick;
  colorDragging;
  hueDragging;
  overlay;
  colorSelectorViewChild;
  colorHandleViewChild;
  hueViewChild;
  hueHandleViewChild;
  _componentStyle = inject(ColorPickerStyle);
  constructor(overlayService) {
    super();
    this.overlayService = overlayService;
  }
  set colorSelector(element) {
    this.colorSelectorViewChild = element;
  }
  set colorHandle(element) {
    this.colorHandleViewChild = element;
  }
  set hue(element) {
    this.hueViewChild = element;
  }
  set hueHandle(element) {
    this.hueHandleViewChild = element;
  }
  get ariaLabel() {
    return this.config?.getTranslation(TranslationKeys.ARIA)[TranslationKeys.SELECT_COLOR];
  }
  onHueMousedown(event) {
    if (this.disabled) {
      return;
    }
    this.bindDocumentMousemoveListener();
    this.bindDocumentMouseupListener();
    this.hueDragging = true;
    this.pickHue(event);
  }
  onHueDragStart(event) {
    if (this.disabled) {
      return;
    }
    this.hueDragging = true;
    this.pickHue(event, event.changedTouches[0]);
  }
  onColorDragStart(event) {
    if (this.disabled) {
      return;
    }
    this.colorDragging = true;
    this.pickColor(event, event.changedTouches[0]);
  }
  pickHue(event, position) {
    let pageY = position ? position.pageY : event.pageY;
    let top = this.hueViewChild?.nativeElement.getBoundingClientRect().top + (this.document.defaultView.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0);
    this.value = this.validateHSB({
      h: Math.floor(360 * (150 - Math.max(0, Math.min(150, pageY - top))) / 150),
      s: this.value.s,
      b: this.value.b
    });
    this.updateColorSelector();
    this.updateUI();
    this.updateModel();
    this.onChange.emit({
      originalEvent: event,
      value: this.getValueToUpdate()
    });
  }
  onColorMousedown(event) {
    if (this.disabled) {
      return;
    }
    this.bindDocumentMousemoveListener();
    this.bindDocumentMouseupListener();
    this.colorDragging = true;
    this.pickColor(event);
  }
  onDrag(event) {
    if (this.colorDragging) {
      this.pickColor(event, event.changedTouches[0]);
      event.preventDefault();
    }
    if (this.hueDragging) {
      this.pickHue(event, event.changedTouches[0]);
      event.preventDefault();
    }
  }
  onDragEnd() {
    this.colorDragging = false;
    this.hueDragging = false;
    this.unbindDocumentMousemoveListener();
    this.unbindDocumentMouseupListener();
  }
  pickColor(event, position) {
    let pageX = position ? position.pageX : event.pageX;
    let pageY = position ? position.pageY : event.pageY;
    let rect = this.colorSelectorViewChild?.nativeElement.getBoundingClientRect();
    let top = rect.top + (this.document.defaultView.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0);
    let left = rect.left + this.document.body.scrollLeft;
    let saturation = Math.floor(100 * Math.max(0, Math.min(150, pageX - left)) / 150);
    let brightness = Math.floor(100 * (150 - Math.max(0, Math.min(150, pageY - top))) / 150);
    this.value = this.validateHSB({
      h: this.value.h,
      s: saturation,
      b: brightness
    });
    this.updateUI();
    this.updateModel();
    this.onChange.emit({
      originalEvent: event,
      value: this.getValueToUpdate()
    });
  }
  getValueToUpdate() {
    let val;
    switch (this.format) {
      case "hex":
        val = "#" + this.HSBtoHEX(this.value);
        break;
      case "rgb":
        val = this.HSBtoRGB(this.value);
        break;
      case "hsb":
        val = this.value;
        break;
    }
    return val;
  }
  updateModel() {
    this.onModelChange(this.getValueToUpdate());
    this.cd.markForCheck();
  }
  writeValue(value) {
    if (value) {
      switch (this.format) {
        case "hex":
          this.value = this.HEXtoHSB(value);
          break;
        case "rgb":
          this.value = this.RGBtoHSB(value);
          break;
        case "hsb":
          this.value = value;
          break;
      }
    } else {
      this.value = this.HEXtoHSB(this.defaultColor);
    }
    this.updateColorSelector();
    this.updateUI();
    this.cd.markForCheck();
  }
  updateColorSelector() {
    if (this.colorSelectorViewChild) {
      const hsb = {};
      hsb.s = 100;
      hsb.b = 100;
      hsb.h = this.value.h;
      this.colorSelectorViewChild.nativeElement.style.backgroundColor = "#" + this.HSBtoHEX(hsb);
    }
  }
  updateUI() {
    if (this.colorHandleViewChild && this.hueHandleViewChild?.nativeElement) {
      this.colorHandleViewChild.nativeElement.style.left = Math.floor(150 * this.value.s / 100) + "px";
      this.colorHandleViewChild.nativeElement.style.top = Math.floor(150 * (100 - this.value.b) / 100) + "px";
      this.hueHandleViewChild.nativeElement.style.top = Math.floor(150 - 150 * this.value.h / 360) + "px";
    }
    this.inputBgColor = "#" + this.HSBtoHEX(this.value);
  }
  onInputFocus() {
    this.onModelTouched();
  }
  show() {
    this.overlayVisible = true;
    this.cd.markForCheck();
  }
  onOverlayAnimationStart(event) {
    switch (event.toState) {
      case "visible":
        if (!this.inline) {
          this.overlay = event.element;
          this.appendOverlay();
          if (this.autoZIndex) {
            zindexutils.set("overlay", this.overlay, this.config.zIndex.overlay);
          }
          this.alignOverlay();
          this.bindDocumentClickListener();
          this.bindDocumentResizeListener();
          this.bindScrollListener();
          this.updateColorSelector();
          this.updateUI();
        }
        break;
      case "void":
        this.onOverlayHide();
        break;
    }
  }
  onOverlayAnimationEnd(event) {
    switch (event.toState) {
      case "visible":
        if (!this.inline) {
          this.onShow.emit({});
        }
        break;
      case "void":
        if (this.autoZIndex) {
          zindexutils.clear(event.element);
        }
        this.onHide.emit({});
        break;
    }
  }
  appendOverlay() {
    if (this.appendTo) {
      if (this.appendTo === "body") this.renderer.appendChild(this.document.body, this.overlay);
      else appendChild(this.appendTo, this.overlay);
    }
  }
  restoreOverlayAppend() {
    if (this.overlay && this.appendTo) {
      this.renderer.appendChild(this.el.nativeElement, this.overlay);
    }
  }
  alignOverlay() {
    if (this.appendTo) absolutePosition(this.overlay, this.inputViewChild?.nativeElement);
    else relativePosition(this.overlay, this.inputViewChild?.nativeElement);
  }
  hide() {
    this.overlayVisible = false;
    this.cd.markForCheck();
  }
  onInputClick() {
    this.selfClick = true;
    this.togglePanel();
  }
  togglePanel() {
    if (!this.overlayVisible) this.show();
    else this.hide();
  }
  onInputKeydown(event) {
    switch (event.code) {
      case "Space":
        this.togglePanel();
        event.preventDefault();
        break;
      case "Escape":
      case "Tab":
        this.hide();
        break;
      default:
        break;
    }
  }
  onOverlayClick(event) {
    this.overlayService.add({
      originalEvent: event,
      target: this.el.nativeElement
    });
    this.selfClick = true;
  }
  registerOnChange(fn) {
    this.onModelChange = fn;
  }
  registerOnTouched(fn) {
    this.onModelTouched = fn;
  }
  setDisabledState(val) {
    this.disabled = val;
    this.cd.markForCheck();
  }
  bindDocumentClickListener() {
    if (!this.documentClickListener) {
      const documentTarget = this.el ? this.el.nativeElement.ownerDocument : "document";
      this.documentClickListener = this.renderer.listen(documentTarget, "click", () => {
        if (!this.selfClick) {
          this.overlayVisible = false;
          this.unbindDocumentClickListener();
        }
        this.selfClick = false;
        this.cd.markForCheck();
      });
    }
  }
  unbindDocumentClickListener() {
    if (this.documentClickListener) {
      this.documentClickListener();
      this.documentClickListener = null;
    }
  }
  bindDocumentMousemoveListener() {
    if (!this.documentMousemoveListener) {
      const documentTarget = this.el ? this.el.nativeElement.ownerDocument : "document";
      this.documentMousemoveListener = this.renderer.listen(documentTarget, "mousemove", (event) => {
        if (this.colorDragging) {
          this.pickColor(event);
        }
        if (this.hueDragging) {
          this.pickHue(event);
        }
      });
    }
  }
  unbindDocumentMousemoveListener() {
    if (this.documentMousemoveListener) {
      this.documentMousemoveListener();
      this.documentMousemoveListener = null;
    }
  }
  bindDocumentMouseupListener() {
    if (!this.documentMouseupListener) {
      const documentTarget = this.el ? this.el.nativeElement.ownerDocument : "document";
      this.documentMouseupListener = this.renderer.listen(documentTarget, "mouseup", () => {
        this.colorDragging = false;
        this.hueDragging = false;
        this.unbindDocumentMousemoveListener();
        this.unbindDocumentMouseupListener();
      });
    }
  }
  unbindDocumentMouseupListener() {
    if (this.documentMouseupListener) {
      this.documentMouseupListener();
      this.documentMouseupListener = null;
    }
  }
  bindDocumentResizeListener() {
    if (isPlatformBrowser(this.platformId)) {
      this.documentResizeListener = this.renderer.listen(this.document.defaultView, "resize", this.onWindowResize.bind(this));
    }
  }
  unbindDocumentResizeListener() {
    if (this.documentResizeListener) {
      this.documentResizeListener();
      this.documentResizeListener = null;
    }
  }
  onWindowResize() {
    if (this.overlayVisible && !isTouchDevice()) {
      this.hide();
    }
  }
  bindScrollListener() {
    if (!this.scrollHandler) {
      this.scrollHandler = new ConnectedOverlayScrollHandler(this.containerViewChild?.nativeElement, () => {
        if (this.overlayVisible) {
          this.hide();
        }
      });
    }
    this.scrollHandler.bindScrollListener();
  }
  unbindScrollListener() {
    if (this.scrollHandler) {
      this.scrollHandler.unbindScrollListener();
    }
  }
  validateHSB(hsb) {
    return {
      h: Math.min(360, Math.max(0, hsb.h)),
      s: Math.min(100, Math.max(0, hsb.s)),
      b: Math.min(100, Math.max(0, hsb.b))
    };
  }
  validateRGB(rgb) {
    return {
      r: Math.min(255, Math.max(0, rgb.r)),
      g: Math.min(255, Math.max(0, rgb.g)),
      b: Math.min(255, Math.max(0, rgb.b))
    };
  }
  validateHEX(hex) {
    var len = 6 - hex.length;
    if (len > 0) {
      var o = [];
      for (var i = 0; i < len; i++) {
        o.push("0");
      }
      o.push(hex);
      hex = o.join("");
    }
    return hex;
  }
  HEXtoRGB(hex) {
    let hexValue = parseInt(hex.indexOf("#") > -1 ? hex.substring(1) : hex, 16);
    return {
      r: hexValue >> 16,
      g: (hexValue & 65280) >> 8,
      b: hexValue & 255
    };
  }
  HEXtoHSB(hex) {
    return this.RGBtoHSB(this.HEXtoRGB(hex));
  }
  RGBtoHSB(rgb) {
    var hsb = {
      h: 0,
      s: 0,
      b: 0
    };
    var min = Math.min(rgb.r, rgb.g, rgb.b);
    var max = Math.max(rgb.r, rgb.g, rgb.b);
    var delta = max - min;
    hsb.b = max;
    hsb.s = max != 0 ? 255 * delta / max : 0;
    if (hsb.s != 0) {
      if (rgb.r == max) {
        hsb.h = (rgb.g - rgb.b) / delta;
      } else if (rgb.g == max) {
        hsb.h = 2 + (rgb.b - rgb.r) / delta;
      } else {
        hsb.h = 4 + (rgb.r - rgb.g) / delta;
      }
    } else {
      hsb.h = -1;
    }
    hsb.h *= 60;
    if (hsb.h < 0) {
      hsb.h += 360;
    }
    hsb.s *= 100 / 255;
    hsb.b *= 100 / 255;
    return hsb;
  }
  HSBtoRGB(hsb) {
    var rgb = {
      r: 0,
      g: 0,
      b: 0
    };
    let h = hsb.h;
    let s = hsb.s * 255 / 100;
    let v = hsb.b * 255 / 100;
    if (s == 0) {
      rgb = {
        r: v,
        g: v,
        b: v
      };
    } else {
      let t1 = v;
      let t2 = (255 - s) * v / 255;
      let t3 = (t1 - t2) * (h % 60) / 60;
      if (h == 360) h = 0;
      if (h < 60) {
        rgb.r = t1;
        rgb.b = t2;
        rgb.g = t2 + t3;
      } else if (h < 120) {
        rgb.g = t1;
        rgb.b = t2;
        rgb.r = t1 - t3;
      } else if (h < 180) {
        rgb.g = t1;
        rgb.r = t2;
        rgb.b = t2 + t3;
      } else if (h < 240) {
        rgb.b = t1;
        rgb.r = t2;
        rgb.g = t1 - t3;
      } else if (h < 300) {
        rgb.b = t1;
        rgb.g = t2;
        rgb.r = t2 + t3;
      } else if (h < 360) {
        rgb.r = t1;
        rgb.g = t2;
        rgb.b = t1 - t3;
      } else {
        rgb.r = 0;
        rgb.g = 0;
        rgb.b = 0;
      }
    }
    return {
      r: Math.round(rgb.r),
      g: Math.round(rgb.g),
      b: Math.round(rgb.b)
    };
  }
  RGBtoHEX(rgb) {
    var hex = [rgb.r.toString(16), rgb.g.toString(16), rgb.b.toString(16)];
    for (var key in hex) {
      if (hex[key].length == 1) {
        hex[key] = "0" + hex[key];
      }
    }
    return hex.join("");
  }
  HSBtoHEX(hsb) {
    return this.RGBtoHEX(this.HSBtoRGB(hsb));
  }
  onOverlayHide() {
    this.unbindScrollListener();
    this.unbindDocumentResizeListener();
    this.unbindDocumentClickListener();
    this.overlay = null;
  }
  ngAfterViewInit() {
    if (this.inline) {
      this.updateColorSelector();
      this.updateUI();
    }
  }
  ngOnDestroy() {
    if (this.scrollHandler) {
      this.scrollHandler.destroy();
      this.scrollHandler = null;
    }
    if (this.overlay && this.autoZIndex) {
      zindexutils.clear(this.overlay);
    }
    this.restoreOverlayAppend();
    this.onOverlayHide();
  }
  static ɵfac = function ColorPicker_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ColorPicker)(ɵɵdirectiveInject(OverlayService));
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _ColorPicker,
    selectors: [["p-colorPicker"], ["p-colorpicker"], ["p-color-picker"]],
    viewQuery: function ColorPicker_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c0, 5);
        ɵɵviewQuery(_c1, 5);
        ɵɵviewQuery(_c2, 5);
        ɵɵviewQuery(_c3, 5);
        ɵɵviewQuery(_c4, 5);
        ɵɵviewQuery(_c5, 5);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.containerViewChild = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.inputViewChild = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.colorSelector = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.colorHandle = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.hue = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.hueHandle = _t.first);
      }
    },
    inputs: {
      style: "style",
      styleClass: "styleClass",
      inline: [2, "inline", "inline", booleanAttribute],
      format: "format",
      appendTo: "appendTo",
      disabled: [2, "disabled", "disabled", booleanAttribute],
      tabindex: "tabindex",
      inputId: "inputId",
      autoZIndex: [2, "autoZIndex", "autoZIndex", booleanAttribute],
      baseZIndex: [2, "baseZIndex", "baseZIndex", numberAttribute],
      showTransitionOptions: "showTransitionOptions",
      hideTransitionOptions: "hideTransitionOptions",
      autofocus: [2, "autofocus", "autofocus", booleanAttribute]
    },
    outputs: {
      onChange: "onChange",
      onShow: "onShow",
      onHide: "onHide"
    },
    features: [ɵɵProvidersFeature([COLORPICKER_VALUE_ACCESSOR, ColorPickerStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    decls: 4,
    vars: 11,
    consts: [["container", ""], ["input", ""], ["colorSelector", ""], ["colorHandle", ""], ["hue", ""], ["hueHandle", ""], [3, "ngStyle", "ngClass"], ["type", "text", "class", "p-colorpicker-preview", "readonly", "readonly", 3, "ngClass", "disabled", "backgroundColor", "pAutoFocus", "click", "keydown", "focus", 4, "ngIf"], [3, "ngClass", "click", 4, "ngIf"], ["type", "text", "readonly", "readonly", 1, "p-colorpicker-preview", 3, "click", "keydown", "focus", "ngClass", "disabled", "pAutoFocus"], [3, "click", "ngClass"], [1, "p-colorpicker-content"], [1, "p-colorpicker-color-selector", 3, "touchstart", "touchmove", "touchend", "mousedown"], [1, "p-colorpicker-color-background"], [1, "p-colorpicker-color-handle"], [1, "p-colorpicker-hue", 3, "mousedown", "touchstart", "touchmove", "touchend"], [1, "p-colorpicker-hue-handle"]],
    template: function ColorPicker_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵelementStart(0, "div", 6, 0);
        ɵɵtemplate(2, ColorPicker_input_2_Template, 2, 11, "input", 7)(3, ColorPicker_div_3_Template, 11, 18, "div", 8);
        ɵɵelementEnd();
      }
      if (rf & 2) {
        ɵɵclassMap(ctx.styleClass);
        ɵɵproperty("ngStyle", ctx.style)("ngClass", ɵɵpureFunction2(8, _c6, !ctx.inline, ctx.colorDragging || ctx.hueDragging));
        ɵɵattribute("data-pc-name", "colorpicker")("data-pc-section", "root");
        ɵɵadvance(2);
        ɵɵproperty("ngIf", !ctx.inline);
        ɵɵadvance();
        ɵɵproperty("ngIf", ctx.inline || ctx.overlayVisible);
      }
    },
    dependencies: [CommonModule, NgClass, NgIf, NgStyle, AutoFocusModule, AutoFocus, SharedModule],
    encapsulation: 2,
    data: {
      animation: [trigger("overlayAnimation", [transition(":enter", [style({
        opacity: 0,
        transform: "scaleY(0.8)"
      }), animate("{{showTransitionParams}}")]), transition(":leave", [animate("{{hideTransitionParams}}", style({
        opacity: 0
      }))])])]
    },
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ColorPicker, [{
    type: Component,
    args: [{
      selector: "p-colorPicker, p-colorpicker, p-color-picker",
      standalone: true,
      imports: [CommonModule, AutoFocusModule, SharedModule],
      template: `
        <div
            #container
            [ngStyle]="style"
            [class]="styleClass"
            [ngClass]="{
                'p-colorpicker p-component': true,
                'p-colorpicker-overlay': !inline,
                'p-colorpicker-dragging': colorDragging || hueDragging
            }"
            [attr.data-pc-name]="'colorpicker'"
            [attr.data-pc-section]="'root'"
        >
            <input
                *ngIf="!inline"
                #input
                type="text"
                class="p-colorpicker-preview"
                [ngClass]="{ 'p-disabled': disabled }"
                readonly="readonly"
                [attr.tabindex]="tabindex"
                [disabled]="disabled"
                (click)="onInputClick()"
                (keydown)="onInputKeydown($event)"
                (focus)="onInputFocus()"
                [attr.id]="inputId"
                [style.backgroundColor]="inputBgColor"
                [attr.data-pc-section]="'input'"
                [attr.aria-label]="ariaLabel"
                [pAutoFocus]="autofocus"
            />
            <div
                *ngIf="inline || overlayVisible"
                [ngClass]="{ 'p-colorpicker-panel': true, 'p-colorpicker-panel-inline': inline, 'p-disabled': disabled }"
                (click)="onOverlayClick($event)"
                [@overlayAnimation]="{
                    value: 'visible',
                    params: { showTransitionParams: showTransitionOptions, hideTransitionParams: hideTransitionOptions }
                }"
                [@.disabled]="inline === true"
                (@overlayAnimation.start)="onOverlayAnimationStart($event)"
                (@overlayAnimation.done)="onOverlayAnimationEnd($event)"
                [attr.data-pc-section]="'panel'"
            >
                <div class="p-colorpicker-content" [attr.data-pc-section]="'content'">
                    <div #colorSelector class="p-colorpicker-color-selector" (touchstart)="onColorDragStart($event)" (touchmove)="onDrag($event)" (touchend)="onDragEnd()" (mousedown)="onColorMousedown($event)" [attr.data-pc-section]="'selector'">
                        <div class="p-colorpicker-color-background" [attr.data-pc-section]="'color'">
                            <div #colorHandle class="p-colorpicker-color-handle" [attr.data-pc-section]="'colorHandle'"></div>
                        </div>
                    </div>
                    <div #hue class="p-colorpicker-hue" (mousedown)="onHueMousedown($event)" (touchstart)="onHueDragStart($event)" (touchmove)="onDrag($event)" (touchend)="onDragEnd()" [attr.data-pc-section]="'hue'">
                        <div #hueHandle class="p-colorpicker-hue-handle" [attr.data-pc-section]="'hueHandle'"></div>
                    </div>
                </div>
            </div>
        </div>
    `,
      animations: [trigger("overlayAnimation", [transition(":enter", [style({
        opacity: 0,
        transform: "scaleY(0.8)"
      }), animate("{{showTransitionParams}}")]), transition(":leave", [animate("{{hideTransitionParams}}", style({
        opacity: 0
      }))])])],
      providers: [COLORPICKER_VALUE_ACCESSOR, ColorPickerStyle],
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None
    }]
  }], () => [{
    type: OverlayService
  }], {
    style: [{
      type: Input
    }],
    styleClass: [{
      type: Input
    }],
    inline: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    format: [{
      type: Input
    }],
    appendTo: [{
      type: Input
    }],
    disabled: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    tabindex: [{
      type: Input
    }],
    inputId: [{
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
    showTransitionOptions: [{
      type: Input
    }],
    hideTransitionOptions: [{
      type: Input
    }],
    autofocus: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    onChange: [{
      type: Output
    }],
    onShow: [{
      type: Output
    }],
    onHide: [{
      type: Output
    }],
    containerViewChild: [{
      type: ViewChild,
      args: ["container"]
    }],
    inputViewChild: [{
      type: ViewChild,
      args: ["input"]
    }],
    colorSelector: [{
      type: ViewChild,
      args: ["colorSelector"]
    }],
    colorHandle: [{
      type: ViewChild,
      args: ["colorHandle"]
    }],
    hue: [{
      type: ViewChild,
      args: ["hue"]
    }],
    hueHandle: [{
      type: ViewChild,
      args: ["hueHandle"]
    }]
  });
})();
var ColorPickerModule = class _ColorPickerModule {
  static ɵfac = function ColorPickerModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ColorPickerModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _ColorPickerModule,
    imports: [ColorPicker, SharedModule],
    exports: [ColorPicker, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [ColorPicker, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ColorPickerModule, [{
    type: NgModule,
    args: [{
      imports: [ColorPicker, SharedModule],
      exports: [ColorPicker, SharedModule]
    }]
  }], null, null);
})();
export {
  COLORPICKER_VALUE_ACCESSOR,
  ColorPicker,
  ColorPickerClasses,
  ColorPickerModule,
  ColorPickerStyle
};
//# sourceMappingURL=primeng_colorpicker.js.map
