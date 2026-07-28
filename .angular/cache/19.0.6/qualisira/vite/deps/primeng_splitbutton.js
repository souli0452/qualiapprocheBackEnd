import {
  TieredMenu
} from "./chunk-L3JFDRBF.js";
import {
  Tooltip,
  TooltipModule
} from "./chunk-4EKXPA3K.js";
import {
  ButtonDirective
} from "./chunk-IYEO7CFM.js";
import "./chunk-KGQPIMGR.js";
import "./chunk-KHDC5QWA.js";
import {
  Ripple
} from "./chunk-HQJN727C.js";
import {
  ChevronDownIcon
} from "./chunk-G7GR35G3.js";
import {
  AutoFocus
} from "./chunk-K3LJODM5.js";
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
  uuid
} from "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import "./chunk-PBR2IEBG.js";
import "./chunk-M3JF7BPQ.js";
import "./chunk-HQ743VTI.js";
import "./chunk-ORXNKOXS.js";
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
  ViewChild,
  ViewEncapsulation,
  booleanAttribute,
  inject,
  numberAttribute,
  setClassMetadata,
  signal,
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
  ɵɵqueryRefresh,
  ɵɵreference,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵstyleMap,
  ɵɵtemplate,
  ɵɵtemplateRefExtractor,
  ɵɵviewQuery
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import {
  __spreadValues
} from "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-splitbutton.mjs
var _c0 = ["content"];
var _c1 = ["dropdownicon"];
var _c2 = ["container"];
var _c3 = ["defaultbtn"];
var _c4 = ["menu"];
function SplitButton_ng_container_2_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function SplitButton_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    const _r2 = ɵɵgetCurrentView();
    ɵɵelementContainerStart(0);
    ɵɵelementStart(1, "button", 10);
    ɵɵlistener("click", function SplitButton_ng_container_2_Template_button_click_1_listener($event) {
      ɵɵrestoreView(_r2);
      const ctx_r2 = ɵɵnextContext();
      return ɵɵresetView(ctx_r2.onDefaultButtonClick($event));
    });
    ɵɵtemplate(2, SplitButton_ng_container_2_ng_container_2_Template, 1, 0, "ng-container", 11);
    ɵɵelementEnd();
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext();
    ɵɵadvance();
    ɵɵproperty("severity", ctx_r2.severity)("text", ctx_r2.text)("outlined", ctx_r2.outlined)("size", ctx_r2.size)("icon", ctx_r2.icon)("iconPos", ctx_r2.iconPos)("disabled", ctx_r2.disabled)("pAutoFocus", ctx_r2.autofocus)("pTooltip", ctx_r2.tooltip)("tooltipOptions", ctx_r2.tooltipOptions);
    ɵɵattribute("tabindex", ctx_r2.tabindex)("aria-label", (ctx_r2.buttonProps == null ? null : ctx_r2.buttonProps["ariaLabel"]) || ctx_r2.label);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r2.contentTemplate || ctx_r2._contentTemplate);
  }
}
function SplitButton_ng_template_3_Template(rf, ctx) {
  if (rf & 1) {
    const _r4 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 12, 3);
    ɵɵlistener("click", function SplitButton_ng_template_3_Template_button_click_0_listener($event) {
      ɵɵrestoreView(_r4);
      const ctx_r2 = ɵɵnextContext();
      return ɵɵresetView(ctx_r2.onDefaultButtonClick($event));
    });
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext();
    ɵɵproperty("severity", ctx_r2.severity)("text", ctx_r2.text)("outlined", ctx_r2.outlined)("size", ctx_r2.size)("icon", ctx_r2.icon)("iconPos", ctx_r2.iconPos)("label", ctx_r2.label)("disabled", ctx_r2.buttonDisabled)("pAutoFocus", ctx_r2.autofocus)("pTooltip", ctx_r2.tooltip)("tooltipOptions", ctx_r2.tooltipOptions);
    ɵɵattribute("tabindex", ctx_r2.tabindex)("aria-label", ctx_r2.buttonProps == null ? null : ctx_r2.buttonProps["ariaLabel"]);
  }
}
function SplitButton_span_6_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span");
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext();
    ɵɵclassMap(ctx_r2.dropdownIcon);
  }
}
function SplitButton_ng_container_7_ChevronDownIcon_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "ChevronDownIcon");
  }
}
function SplitButton_ng_container_7_2_ng_template_0_Template(rf, ctx) {
}
function SplitButton_ng_container_7_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, SplitButton_ng_container_7_2_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function SplitButton_ng_container_7_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, SplitButton_ng_container_7_ChevronDownIcon_1_Template, 1, 0, "ChevronDownIcon", 8)(2, SplitButton_ng_container_7_2_Template, 1, 0, null, 11);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext();
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r2.dropdownIconTemplate && !ctx_r2._dropdownIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r2.dropdownIconTemplate || ctx_r2._dropdownIconTemplate);
  }
}
var theme = ({
  dt
}) => `
.p-splitbutton {
    display: inline-flex;
    position: relative;
    border-radius: ${dt("splitbutton.border.radius")};
}

.p-splitbutton-button.p-button {
    border-start-end-radius: 0;
    border-end-end-radius: 0;
    border-right: 0 none;
}

.p-splitbutton-button.p-button:focus-visible,
.p-splitbutton-dropdown.p-button:focus-visible {
    z-index: 1;
}

.p-splitbutton-button.p-button:not(:disabled):hover,
.p-splitbutton-button.p-button:not(:disabled):active {
    border-right: 0 none;
}

.p-splitbutton-dropdown.p-button {
    border-start-start-radius: 0;
    border-end-start-radius: 0;
}

.p-splitbutton .p-menu {
    min-width: 100%;
}

.p-splitbutton-fluid {
    display: flex;
}

.p-splitbutton-rounded .p-splitbutton-dropdown {
    border-start-end-radius: ${dt("splitbutton.rounded.border.radius")};
    border-end-end-radius: ${dt("splitbutton.rounded.border.radius")};
}

.p-splitbutton-rounded > .p-splitbutton-button {
    border-start-start-radius: ${dt("splitbutton.rounded.border.radius")};
    border-end-start-radius: ${dt("splitbutton.rounded.border.radius")};
}

.p-splitbutton-raised {
    box-shadow: ${dt("splitbutton.raised.shadow")};
}
`;
var classes = {
  root: ({
    props
  }) => ["p-splitbutton p-component", {
    "p-splitbutton-raised": props.raised,
    "p-splitbutton-rounded": props.rounded,
    "p-splitbutton-fluid": props.fluid
  }],
  pcButton: "p-splitbutton-button",
  pcDropdown: "p-splitbutton-dropdown"
};
var SplitButtonStyle = class _SplitButtonStyle extends BaseStyle {
  name = "splitbutton";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵSplitButtonStyle_BaseFactory;
    return function SplitButtonStyle_Factory(__ngFactoryType__) {
      return (ɵSplitButtonStyle_BaseFactory || (ɵSplitButtonStyle_BaseFactory = ɵɵgetInheritedFactory(_SplitButtonStyle)))(__ngFactoryType__ || _SplitButtonStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _SplitButtonStyle,
    factory: _SplitButtonStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(SplitButtonStyle, [{
    type: Injectable
  }], null, null);
})();
var SplitButtonClasses;
(function(SplitButtonClasses2) {
  SplitButtonClasses2["root"] = "p-splitbutton";
  SplitButtonClasses2["pcButton"] = "p-splitbutton-button";
  SplitButtonClasses2["pcDropdown"] = "p-splitbutton-dropdown";
})(SplitButtonClasses || (SplitButtonClasses = {}));
var SplitButton = class _SplitButton extends BaseComponent {
  /**
   * MenuModel instance to define the overlay items.
   * @group Props
   */
  model;
  /**
   * Defines the style of the button.
   * @group Props
   */
  severity;
  /**
   * Add a shadow to indicate elevation.
   * @group Props
   */
  raised = false;
  /**
   * Add a circular border radius to the button.
   * @group Props
   */
  rounded = false;
  /**
   * Add a textual class to the button without a background initially.
   * @group Props
   */
  text = false;
  /**
   * Add a border class without a background initially.
   * @group Props
   */
  outlined = false;
  /**
   * Defines the size of the button.
   * @group Props
   */
  size = null;
  /**
   * Add a plain textual class to the button without a background initially.
   * @group Props
   */
  plain = false;
  /**
   * Name of the icon.
   * @group Props
   */
  icon;
  /**
   * Position of the icon.
   * @group Props
   */
  iconPos = "left";
  /**
   * Text of the button.
   * @group Props
   */
  label;
  /**
   * Tooltip for the main button.
   * @group Props
   */
  tooltip;
  /**
   * Tooltip options for the main button.
   * @group Props
   */
  tooltipOptions;
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
   * Inline style of the overlay menu.
   * @group Props
   */
  menuStyle;
  /**
   * Style class of the overlay menu.
   * @group Props
   */
  menuStyleClass;
  /**
   * Name of the dropdown icon.
   * @group Props
   */
  dropdownIcon;
  /**
   *  Target element to attach the overlay, valid values are "body" or a local ng-template variable of another element (note: use binding with brackets for template variables, e.g. [appendTo]="mydiv" for a div element having #mydiv as variable name).
   * @group Props
   */
  appendTo = "body";
  /**
   * Indicates the direction of the element.
   * @group Props
   */
  dir;
  /**
   * Defines a string that labels the expand button for accessibility.
   * @group Props
   */
  expandAriaLabel;
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
   * Button Props
   */
  buttonProps;
  /**
   * Menu Button Props
   */
  menuButtonProps;
  /**
   * When present, it specifies that the component should automatically get focus on load.
   * @group Props
   */
  autofocus;
  /**
   * When present, it specifies that the element should be disabled.
   * @group Props
   */
  set disabled(v) {
    this._disabled = v;
    this.buttonDisabled = v;
    this.menuButtonDisabled = v;
  }
  get disabled() {
    return this._disabled;
  }
  /**
   * Index of the element in tabbing order.
   * @group Props
   */
  tabindex;
  /**
   * When present, it specifies that the menu button element should be disabled.
   * @group Props
   */
  menuButtonDisabled = false;
  /**
   * When present, it specifies that the button element should be disabled.
   * @group Props
   */
  buttonDisabled = false;
  /**
   * Callback to invoke when default command button is clicked.
   * @param {MouseEvent} event - Mouse event.
   * @group Emits
   */
  onClick = new EventEmitter();
  /**
   * Callback to invoke when overlay menu is hidden.
   * @group Emits
   */
  onMenuHide = new EventEmitter();
  /**
   * Callback to invoke when overlay menu is shown.
   * @group Emits
   */
  onMenuShow = new EventEmitter();
  /**
   * Callback to invoke when dropdown button is clicked.
   * @param {MouseEvent} event - Mouse event.
   * @group Emits
   */
  onDropdownClick = new EventEmitter();
  containerViewChild;
  buttonViewChild;
  menu;
  /**
   * Template of the content.
   * @group Templates
   */
  contentTemplate;
  /**
   * Template of the dropdownicon.
   * @group Templates
   **/
  dropdownIconTemplate;
  templates;
  ariaId;
  isExpanded = signal(false);
  _disabled;
  _componentStyle = inject(SplitButtonStyle);
  _contentTemplate;
  _dropdownIconTemplate;
  ngOnInit() {
    super.ngOnInit();
    this.ariaId = uuid("pn_id_");
  }
  ngAfterContentInit() {
    this.templates?.forEach((item) => {
      switch (item.getType()) {
        case "content":
          this._contentTemplate = item.template;
          break;
        case "dropdownicon":
          this._dropdownIconTemplate = item.template;
          break;
        default:
          this._contentTemplate = item.template;
          break;
      }
    });
  }
  get containerClass() {
    const cls = {
      "p-splitbutton p-component": true,
      "p-splitbutton-raised": this.raised,
      "p-splitbutton-rounded": this.rounded,
      "p-splitbutton-outlined": this.outlined,
      "p-splitbutton-text": this.text,
      [`p-splitbutton-${this.size === "small" ? "sm" : "lg"}`]: this.size
    };
    return __spreadValues({}, cls);
  }
  onDefaultButtonClick(event) {
    this.onClick.emit(event);
    this.menu.hide();
  }
  onDropdownButtonClick(event) {
    this.onDropdownClick.emit(event);
    this.menu?.toggle({
      currentTarget: this.containerViewChild?.nativeElement,
      relativeAlign: this.appendTo == null
    });
  }
  onDropdownButtonKeydown(event) {
    if (event.code === "ArrowDown" || event.code === "ArrowUp") {
      this.onDropdownButtonClick();
      event.preventDefault();
    }
  }
  onHide() {
    this.isExpanded.set(false);
    this.onMenuHide.emit();
  }
  onShow() {
    this.isExpanded.set(true);
    this.onMenuShow.emit();
  }
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵSplitButton_BaseFactory;
    return function SplitButton_Factory(__ngFactoryType__) {
      return (ɵSplitButton_BaseFactory || (ɵSplitButton_BaseFactory = ɵɵgetInheritedFactory(_SplitButton)))(__ngFactoryType__ || _SplitButton);
    };
  })();
  static ɵcmp = ɵɵdefineComponent({
    type: _SplitButton,
    selectors: [["p-splitbutton"], ["p-splitButton"], ["p-split-button"]],
    contentQueries: function SplitButton_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c0, 4);
        ɵɵcontentQuery(dirIndex, _c1, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.contentTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.dropdownIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    viewQuery: function SplitButton_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c2, 5);
        ɵɵviewQuery(_c3, 5);
        ɵɵviewQuery(_c4, 5);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.containerViewChild = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.buttonViewChild = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.menu = _t.first);
      }
    },
    inputs: {
      model: "model",
      severity: "severity",
      raised: [2, "raised", "raised", booleanAttribute],
      rounded: [2, "rounded", "rounded", booleanAttribute],
      text: [2, "text", "text", booleanAttribute],
      outlined: [2, "outlined", "outlined", booleanAttribute],
      size: "size",
      plain: [2, "plain", "plain", booleanAttribute],
      icon: "icon",
      iconPos: "iconPos",
      label: "label",
      tooltip: "tooltip",
      tooltipOptions: "tooltipOptions",
      style: "style",
      styleClass: "styleClass",
      menuStyle: "menuStyle",
      menuStyleClass: "menuStyleClass",
      dropdownIcon: "dropdownIcon",
      appendTo: "appendTo",
      dir: "dir",
      expandAriaLabel: "expandAriaLabel",
      showTransitionOptions: "showTransitionOptions",
      hideTransitionOptions: "hideTransitionOptions",
      buttonProps: "buttonProps",
      menuButtonProps: "menuButtonProps",
      autofocus: [2, "autofocus", "autofocus", booleanAttribute],
      disabled: [2, "disabled", "disabled", booleanAttribute],
      tabindex: [2, "tabindex", "tabindex", numberAttribute],
      menuButtonDisabled: [2, "menuButtonDisabled", "menuButtonDisabled", booleanAttribute],
      buttonDisabled: [2, "buttonDisabled", "buttonDisabled", booleanAttribute]
    },
    outputs: {
      onClick: "onClick",
      onMenuHide: "onMenuHide",
      onMenuShow: "onMenuShow",
      onDropdownClick: "onDropdownClick"
    },
    features: [ɵɵProvidersFeature([SplitButtonStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    decls: 10,
    vars: 26,
    consts: [["container", ""], ["defaultButton", ""], ["menu", ""], ["defaultbtn", ""], [3, "ngClass", "ngStyle"], [4, "ngIf", "ngIfElse"], ["type", "button", "pButton", "", "pRipple", "", 1, "p-splitbutton-dropdown", "p-button-icon-only", 3, "click", "keydown", "size", "severity", "text", "outlined", "disabled"], [3, "class", 4, "ngIf"], [4, "ngIf"], [3, "onHide", "onShow", "id", "popup", "model", "styleClass", "appendTo", "showTransitionOptions", "hideTransitionOptions"], ["type", "button", "pButton", "", "pRipple", "", 1, "p-splitbutton-button", 3, "click", "severity", "text", "outlined", "size", "icon", "iconPos", "disabled", "pAutoFocus", "pTooltip", "tooltipOptions"], [4, "ngTemplateOutlet"], ["type", "button", "pButton", "", "pRipple", "", 1, "p-splitbutton-button", 3, "click", "severity", "text", "outlined", "size", "icon", "iconPos", "label", "disabled", "pAutoFocus", "pTooltip", "tooltipOptions"]],
    template: function SplitButton_Template(rf, ctx) {
      if (rf & 1) {
        const _r1 = ɵɵgetCurrentView();
        ɵɵelementStart(0, "div", 4, 0);
        ɵɵtemplate(2, SplitButton_ng_container_2_Template, 3, 13, "ng-container", 5)(3, SplitButton_ng_template_3_Template, 2, 13, "ng-template", null, 1, ɵɵtemplateRefExtractor);
        ɵɵelementStart(5, "button", 6);
        ɵɵlistener("click", function SplitButton_Template_button_click_5_listener($event) {
          ɵɵrestoreView(_r1);
          return ɵɵresetView(ctx.onDropdownButtonClick($event));
        })("keydown", function SplitButton_Template_button_keydown_5_listener($event) {
          ɵɵrestoreView(_r1);
          return ɵɵresetView(ctx.onDropdownButtonKeydown($event));
        });
        ɵɵtemplate(6, SplitButton_span_6_Template, 1, 2, "span", 7)(7, SplitButton_ng_container_7_Template, 3, 2, "ng-container", 8);
        ɵɵelementEnd();
        ɵɵelementStart(8, "p-tieredMenu", 9, 2);
        ɵɵlistener("onHide", function SplitButton_Template_p_tieredMenu_onHide_8_listener() {
          ɵɵrestoreView(_r1);
          return ɵɵresetView(ctx.onHide());
        })("onShow", function SplitButton_Template_p_tieredMenu_onShow_8_listener() {
          ɵɵrestoreView(_r1);
          return ɵɵresetView(ctx.onShow());
        });
        ɵɵelementEnd()();
      }
      if (rf & 2) {
        const defaultButton_r5 = ɵɵreference(4);
        ɵɵclassMap(ctx.styleClass);
        ɵɵproperty("ngClass", ctx.containerClass)("ngStyle", ctx.style);
        ɵɵadvance(2);
        ɵɵproperty("ngIf", ctx.contentTemplate || ctx._contentTemplate)("ngIfElse", defaultButton_r5);
        ɵɵadvance(3);
        ɵɵproperty("size", ctx.size)("severity", ctx.severity)("text", ctx.text)("outlined", ctx.outlined)("disabled", ctx.menuButtonDisabled);
        ɵɵattribute("aria-label", (ctx.menuButtonProps == null ? null : ctx.menuButtonProps["ariaLabel"]) || ctx.expandAriaLabel)("aria-haspopup", (ctx.menuButtonProps == null ? null : ctx.menuButtonProps["ariaHasPopup"]) || true)("aria-expanded", (ctx.menuButtonProps == null ? null : ctx.menuButtonProps["ariaExpanded"]) || ctx.isExpanded())("aria-controls", (ctx.menuButtonProps == null ? null : ctx.menuButtonProps["ariaControls"]) || ctx.ariaId);
        ɵɵadvance();
        ɵɵproperty("ngIf", ctx.dropdownIcon);
        ɵɵadvance();
        ɵɵproperty("ngIf", !ctx.dropdownIcon);
        ɵɵadvance();
        ɵɵstyleMap(ctx.menuStyle);
        ɵɵproperty("id", ctx.ariaId)("popup", true)("model", ctx.model)("styleClass", ctx.menuStyleClass)("appendTo", ctx.appendTo)("showTransitionOptions", ctx.showTransitionOptions)("hideTransitionOptions", ctx.hideTransitionOptions);
      }
    },
    dependencies: [CommonModule, NgClass, NgIf, NgTemplateOutlet, NgStyle, ButtonDirective, TieredMenu, AutoFocus, ChevronDownIcon, Ripple, TooltipModule, Tooltip, SharedModule],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(SplitButton, [{
    type: Component,
    args: [{
      selector: "p-splitbutton, p-splitButton, p-split-button",
      standalone: true,
      imports: [CommonModule, ButtonDirective, TieredMenu, AutoFocus, ChevronDownIcon, Ripple, TooltipModule, SharedModule],
      template: `
        <div #container [ngClass]="containerClass" [class]="styleClass" [ngStyle]="style">
            <ng-container *ngIf="contentTemplate || _contentTemplate; else defaultButton">
                <button
                    class="p-splitbutton-button"
                    type="button"
                    pButton
                    pRipple
                    [severity]="severity"
                    [text]="text"
                    [outlined]="outlined"
                    [size]="size"
                    [icon]="icon"
                    [iconPos]="iconPos"
                    (click)="onDefaultButtonClick($event)"
                    [disabled]="disabled"
                    [attr.tabindex]="tabindex"
                    [attr.aria-label]="buttonProps?.['ariaLabel'] || label"
                    [pAutoFocus]="autofocus"
                    [pTooltip]="tooltip"
                    [tooltipOptions]="tooltipOptions"
                >
                    <ng-container *ngTemplateOutlet="contentTemplate || _contentTemplate"></ng-container>
                </button>
            </ng-container>
            <ng-template #defaultButton>
                <button
                    #defaultbtn
                    class="p-splitbutton-button"
                    type="button"
                    pButton
                    pRipple
                    [severity]="severity"
                    [text]="text"
                    [outlined]="outlined"
                    [size]="size"
                    [icon]="icon"
                    [iconPos]="iconPos"
                    [label]="label"
                    (click)="onDefaultButtonClick($event)"
                    [disabled]="buttonDisabled"
                    [attr.tabindex]="tabindex"
                    [attr.aria-label]="buttonProps?.['ariaLabel']"
                    [pAutoFocus]="autofocus"
                    [pTooltip]="tooltip"
                    [tooltipOptions]="tooltipOptions"
                ></button>
            </ng-template>
            <button
                type="button"
                pButton
                pRipple
                [size]="size"
                [severity]="severity"
                [text]="text"
                [outlined]="outlined"
                class="p-splitbutton-dropdown p-button-icon-only"
                (click)="onDropdownButtonClick($event)"
                (keydown)="onDropdownButtonKeydown($event)"
                [disabled]="menuButtonDisabled"
                [attr.aria-label]="menuButtonProps?.['ariaLabel'] || expandAriaLabel"
                [attr.aria-haspopup]="menuButtonProps?.['ariaHasPopup'] || true"
                [attr.aria-expanded]="menuButtonProps?.['ariaExpanded'] || isExpanded()"
                [attr.aria-controls]="menuButtonProps?.['ariaControls'] || ariaId"
            >
                <span *ngIf="dropdownIcon" [class]="dropdownIcon"></span>
                <ng-container *ngIf="!dropdownIcon">
                    <ChevronDownIcon *ngIf="!dropdownIconTemplate && !_dropdownIconTemplate" />
                    <ng-template *ngTemplateOutlet="dropdownIconTemplate || _dropdownIconTemplate"></ng-template>
                </ng-container>
            </button>
            <p-tieredMenu
                [id]="ariaId"
                #menu
                [popup]="true"
                [model]="model"
                [style]="menuStyle"
                [styleClass]="menuStyleClass"
                [appendTo]="appendTo"
                [showTransitionOptions]="showTransitionOptions"
                [hideTransitionOptions]="hideTransitionOptions"
                (onHide)="onHide()"
                (onShow)="onShow()"
            ></p-tieredMenu>
        </div>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      providers: [SplitButtonStyle],
      encapsulation: ViewEncapsulation.None
    }]
  }], null, {
    model: [{
      type: Input
    }],
    severity: [{
      type: Input
    }],
    raised: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    rounded: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    text: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    outlined: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    size: [{
      type: Input
    }],
    plain: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    icon: [{
      type: Input
    }],
    iconPos: [{
      type: Input
    }],
    label: [{
      type: Input
    }],
    tooltip: [{
      type: Input
    }],
    tooltipOptions: [{
      type: Input
    }],
    style: [{
      type: Input
    }],
    styleClass: [{
      type: Input
    }],
    menuStyle: [{
      type: Input
    }],
    menuStyleClass: [{
      type: Input
    }],
    dropdownIcon: [{
      type: Input
    }],
    appendTo: [{
      type: Input
    }],
    dir: [{
      type: Input
    }],
    expandAriaLabel: [{
      type: Input
    }],
    showTransitionOptions: [{
      type: Input
    }],
    hideTransitionOptions: [{
      type: Input
    }],
    buttonProps: [{
      type: Input
    }],
    menuButtonProps: [{
      type: Input
    }],
    autofocus: [{
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
    tabindex: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    menuButtonDisabled: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    buttonDisabled: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    onClick: [{
      type: Output
    }],
    onMenuHide: [{
      type: Output
    }],
    onMenuShow: [{
      type: Output
    }],
    onDropdownClick: [{
      type: Output
    }],
    containerViewChild: [{
      type: ViewChild,
      args: ["container"]
    }],
    buttonViewChild: [{
      type: ViewChild,
      args: ["defaultbtn"]
    }],
    menu: [{
      type: ViewChild,
      args: ["menu"]
    }],
    contentTemplate: [{
      type: ContentChild,
      args: ["content", {
        descendants: false
      }]
    }],
    dropdownIconTemplate: [{
      type: ContentChild,
      args: ["dropdownicon", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var SplitButtonModule = class _SplitButtonModule {
  static ɵfac = function SplitButtonModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _SplitButtonModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _SplitButtonModule,
    imports: [SplitButton, SharedModule],
    exports: [SplitButton, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [SplitButton, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(SplitButtonModule, [{
    type: NgModule,
    args: [{
      imports: [SplitButton, SharedModule],
      exports: [SplitButton, SharedModule]
    }]
  }], null, null);
})();
export {
  SplitButton,
  SplitButtonClasses,
  SplitButtonModule,
  SplitButtonStyle
};
//# sourceMappingURL=primeng_splitbutton.js.map
