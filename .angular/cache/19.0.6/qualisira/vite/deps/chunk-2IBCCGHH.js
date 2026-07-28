import {
  Ripple
} from "./chunk-HQJN727C.js";
import {
  BaseComponent
} from "./chunk-NKPC7AFX.js";
import {
  BaseStyle
} from "./chunk-2GIPMIFT.js";
import {
  PrimeTemplate,
  SharedModule
} from "./chunk-NG5OZP5M.js";
import {
  NG_VALUE_ACCESSOR
} from "./chunk-Q4QQ2CLE.js";
import {
  CommonModule,
  NgClass,
  NgTemplateOutlet
} from "./chunk-YQX3S2V2.js";
import {
  ChangeDetectionStrategy,
  Component,
  ContentChild,
  ContentChildren,
  EventEmitter,
  HostBinding,
  HostListener,
  Injectable,
  Input,
  NgModule,
  Output,
  booleanAttribute,
  forwardRef,
  inject,
  numberAttribute,
  setClassMetadata,
  ɵɵHostDirectivesFeature,
  ɵɵInheritDefinitionFeature,
  ɵɵInputTransformsFeature,
  ɵɵProvidersFeature,
  ɵɵadvance,
  ɵɵattribute,
  ɵɵclassMap,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵcontentQuery,
  ɵɵdefineComponent,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵelement,
  ɵɵelementContainer,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetInheritedFactory,
  ɵɵhostProperty,
  ɵɵlistener,
  ɵɵloadQuery,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵpureFunction1,
  ɵɵpureFunction2,
  ɵɵqueryRefresh,
  ɵɵtemplate,
  ɵɵtext,
  ɵɵtextInterpolate
} from "./chunk-ECNLMTOX.js";

// node_modules/primeng/fesm2022/primeng-togglebutton.mjs
var _c0 = ["icon"];
var _c1 = ["content"];
var _c2 = (a0) => ({
  $implicit: a0
});
var _c3 = (a0, a1) => ({
  "p-togglebutton-icon": true,
  "p-togglebutton-icon-left": a0,
  "p-togglebutton-icon-right": a1
});
function ToggleButton_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function ToggleButton_Conditional_2_Conditional_0_Conditional_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 0);
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext(3);
    ɵɵclassMap(ctx_r0.checked ? ctx_r0.onIcon : ctx_r0.offIcon);
    ɵɵproperty("ngClass", ɵɵpureFunction2(4, _c3, ctx_r0.iconPos === "left", ctx_r0.iconPos === "right"));
    ɵɵattribute("data-pc-section", "icon");
  }
}
function ToggleButton_Conditional_2_Conditional_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ToggleButton_Conditional_2_Conditional_0_Conditional_0_Template, 1, 7, "span", 2);
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext(2);
    ɵɵconditional(ctx_r0.onIcon || ctx_r0.offIcon ? 0 : -1);
  }
}
function ToggleButton_Conditional_2_Conditional_1_ng_container_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function ToggleButton_Conditional_2_Conditional_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ToggleButton_Conditional_2_Conditional_1_ng_container_0_Template, 1, 0, "ng-container", 1);
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext(2);
    ɵɵproperty("ngTemplateOutlet", ctx_r0.iconTemplate || ctx_r0._iconTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(2, _c2, ctx_r0.checked));
  }
}
function ToggleButton_Conditional_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ToggleButton_Conditional_2_Conditional_0_Template, 1, 1)(1, ToggleButton_Conditional_2_Conditional_1_Template, 1, 4, "ng-container");
    ɵɵelementStart(2, "span", 0);
    ɵɵtext(3);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext();
    ɵɵconditional(!ctx_r0.iconTemplate ? 0 : 1);
    ɵɵadvance(2);
    ɵɵproperty("ngClass", ctx_r0.cx("label"));
    ɵɵattribute("data-pc-section", "label");
    ɵɵadvance();
    ɵɵtextInterpolate(ctx_r0.checked ? ctx_r0.hasOnLabel ? ctx_r0.onLabel : " " : ctx_r0.hasOffLabel ? ctx_r0.offLabel : " ");
  }
}
var theme = ({
  dt
}) => `
.p-togglebutton {
    display: inline-flex;
    cursor: pointer;
    user-select: none;
    overflow: hidden;
    position: relative;
    color: ${dt("togglebutton.color")};
    background: ${dt("togglebutton.background")};
    border: 1px solid ${dt("togglebutton.border.color")};
    padding: ${dt("togglebutton.padding")};
    font-size: 1rem;
    font-family: inherit;
    font-feature-settings: inherit;
    transition: background ${dt("togglebutton.transition.duration")}, color ${dt("togglebutton.transition.duration")}, border-color ${dt("togglebutton.transition.duration")},
        outline-color ${dt("togglebutton.transition.duration")}, box-shadow ${dt("togglebutton.transition.duration")};
    border-radius: ${dt("togglebutton.border.radius")};
    outline-color: transparent;
    font-weight: ${dt("togglebutton.font.weight")};
}

.p-togglebutton-content {
    display: inline-flex;
    flex: 1 1 auto;
    align-items: center;
    justify-content: center;
    gap: ${dt("togglebutton.gap")};
    padding: ${dt("togglebutton.content.padding")};
    background: transparent;
    border-radius: ${dt("togglebutton.content.border.radius")};
    transition: background ${dt("togglebutton.transition.duration")}, color ${dt("togglebutton.transition.duration")}, border-color ${dt("togglebutton.transition.duration")},
            outline-color ${dt("togglebutton.transition.duration")}, box-shadow ${dt("togglebutton.transition.duration")};
}

.p-togglebutton:not(:disabled):not(.p-togglebutton-checked):hover {
    background: ${dt("togglebutton.hover.background")};
    color: ${dt("togglebutton.hover.color")};
}

.p-togglebutton.p-togglebutton-checked {
    background: ${dt("togglebutton.checked.background")};
    border-color: ${dt("togglebutton.checked.border.color")};
    color: ${dt("togglebutton.checked.color")};
}

.p-togglebutton-checked .p-togglebutton-content {
    background: ${dt("togglebutton.content.checked.background")};
    box-shadow: ${dt("togglebutton.content.checked.shadow")};
}

.p-togglebutton:focus-visible {
    box-shadow: ${dt("togglebutton.focus.ring.shadow")};
    outline: ${dt("togglebutton.focus.ring.width")} ${dt("togglebutton.focus.ring.style")} ${dt("togglebutton.focus.ring.color")};
    outline-offset: ${dt("togglebutton.focus.ring.offset")};
}

.p-togglebutton.p-invalid {
    border-color: ${dt("togglebutton.invalid.border.color")};
}

.p-togglebutton:disabled:not(.p-togglebutton-checked) {
    opacity: 1;
    cursor: default;
    background: ${dt("togglebutton.disabled.background")};
    border-color: ${dt("togglebutton.disabled.border.color")};
    color: ${dt("togglebutton.disabled.color")};
}

.p-togglebutton-label,
.p-togglebutton-icon {
    position: relative;
    transition: none;
}

.p-togglebutton-icon {
    color: ${dt("togglebutton.icon.color")};
}

.p-togglebutton:not(:disabled):not(.p-togglebutton-checked):hover .p-togglebutton-icon {
    color: ${dt("togglebutton.icon.hover.color")};
}

.p-togglebutton.p-togglebutton-checked .p-togglebutton-icon {
    color: ${dt("togglebutton.icon.checked.color")};
}

.p-togglebutton:disabled .p-togglebutton-icon {
    color: ${dt("togglebutton.icon.disabled.color")};
}

.p-togglebutton-sm {
    padding: ${dt("togglebutton.sm.padding")};
    font-size: ${dt("togglebutton.sm.font.size")};
}

.p-togglebutton-sm .p-togglebutton-content {
    padding: ${dt("togglebutton.content.sm.padding")};
}

.p-togglebutton-lg {
    padding: ${dt("togglebutton.lg.padding")};
    font-size: ${dt("togglebutton.lg.font.size")};
}

.p-togglebutton-lg .p-togglebutton-content {
    padding: ${dt("togglebutton.content.lg.padding")};
}

/* For PrimeNG (iconPos) */
.p-togglebutton-icon-right {
    order: 1;
}

.p-togglebutton.ng-invalid.ng-dirty {
    border-color: ${dt("togglebutton.invalid.border.color")};
}
`;
var classes = {
  root: ({
    instance
  }) => ({
    "p-togglebutton p-component": true,
    "p-togglebutton-checked": instance.checked,
    "p-disabled": instance.disabled,
    "p-togglebutton-sm p-inputfield-sm": instance.size === "small",
    "p-togglebutton-lg p-inputfield-lg": instance.size === "large"
  }),
  content: "p-togglebutton-content",
  icon: "p-togglebutton-icon",
  label: "p-togglebutton-label"
};
var ToggleButtonStyle = class _ToggleButtonStyle extends BaseStyle {
  name = "togglebutton";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵToggleButtonStyle_BaseFactory;
    return function ToggleButtonStyle_Factory(__ngFactoryType__) {
      return (ɵToggleButtonStyle_BaseFactory || (ɵToggleButtonStyle_BaseFactory = ɵɵgetInheritedFactory(_ToggleButtonStyle)))(__ngFactoryType__ || _ToggleButtonStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _ToggleButtonStyle,
    factory: _ToggleButtonStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ToggleButtonStyle, [{
    type: Injectable
  }], null, null);
})();
var ToggleButtonClasses;
(function(ToggleButtonClasses2) {
  ToggleButtonClasses2["root"] = "p-togglebutton";
  ToggleButtonClasses2["icon"] = "p-togglebutton-icon";
  ToggleButtonClasses2["label"] = "p-togglebutton-label";
})(ToggleButtonClasses || (ToggleButtonClasses = {}));
var TOGGLEBUTTON_VALUE_ACCESSOR = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => ToggleButton),
  multi: true
};
var ToggleButton = class _ToggleButton extends BaseComponent {
  get hostClass() {
    return this.styleClass || "";
  }
  onKeyDown(event) {
    switch (event.code) {
      case "Enter":
        this.toggle(event);
        event.preventDefault();
        break;
      case "Space":
        this.toggle(event);
        event.preventDefault();
        break;
    }
  }
  toggle(event) {
    if (!this.disabled && !(this.allowEmpty === false && this.checked)) {
      this.checked = !this.checked;
      this.onModelChange(this.checked);
      this.onModelTouched();
      this.onChange.emit({
        originalEvent: event,
        checked: this.checked
      });
      this.cd.markForCheck();
    }
  }
  /**
   * Label for the on state.
   * @group Props
   */
  onLabel = "Yes";
  /**
   * Label for the off state.
   * @group Props
   */
  offLabel = "No";
  /**
   * Icon for the on state.
   * @group Props
   */
  onIcon;
  /**
   * Icon for the off state.
   * @group Props
   */
  offIcon;
  /**
   * Defines a string that labels the input for accessibility.
   * @group Props
   */
  ariaLabel;
  /**
   * Establishes relationships between the component and label(s) where its value should be one or more element IDs.
   * @group Props
   */
  ariaLabelledBy;
  /**
   * When present, it specifies that the element should be disabled.
   * @group Props
   */
  disabled;
  /**
   * Inline style of the element.
   * @group Props
   */
  style;
  /**
   * Style class of the element.
   * @group Props
   */
  styleClass;
  /**
   * Identifier of the focus input to match a label defined for the component.
   * @group Props
   */
  inputId;
  /**
   * Index of the element in tabbing order.
   * @group Props
   */
  tabindex = 0;
  /**
   * Defines the size of the component.
   * @group Props
   */
  size;
  /**
   * Position of the icon.
   * @group Props
   */
  iconPos = "left";
  /**
   * When present, it specifies that the component should automatically get focus on load.
   * @group Props
   */
  autofocus;
  /**
   * Whether selection can not be cleared.
   * @group Props
   */
  allowEmpty;
  /**
   * Callback to invoke on value change.
   * @param {ToggleButtonChangeEvent} event - Custom change event.
   * @group Emits
   */
  onChange = new EventEmitter();
  /**
   * Custom icon template.
   * @group Templates
   */
  iconTemplate;
  /**
   * Custom content template.
   * @group Templates
   */
  contentTemplate;
  templates;
  checked = false;
  onModelChange = () => {
  };
  onModelTouched = () => {
  };
  _componentStyle = inject(ToggleButtonStyle);
  onBlur() {
    this.onModelTouched();
  }
  writeValue(value) {
    this.checked = value;
    this.cd.markForCheck();
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
  get hasOnLabel() {
    return this.onLabel && this.onLabel.length > 0;
  }
  get hasOffLabel() {
    return this.onLabel && this.onLabel.length > 0;
  }
  get active() {
    return this.checked === true;
  }
  _iconTemplate;
  _contentTemplate;
  ngAfterContentInit() {
    this.templates.forEach((item) => {
      switch (item.getType()) {
        case "icon":
          this._iconTemplate = item.template;
          break;
        case "content":
          this._contentTemplate = item.template;
          break;
        default:
          this._contentTemplate = item.template;
          break;
      }
    });
  }
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵToggleButton_BaseFactory;
    return function ToggleButton_Factory(__ngFactoryType__) {
      return (ɵToggleButton_BaseFactory || (ɵToggleButton_BaseFactory = ɵɵgetInheritedFactory(_ToggleButton)))(__ngFactoryType__ || _ToggleButton);
    };
  })();
  static ɵcmp = ɵɵdefineComponent({
    type: _ToggleButton,
    selectors: [["p-toggleButton"], ["p-togglebutton"], ["p-toggle-button"]],
    contentQueries: function ToggleButton_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c0, 4);
        ɵɵcontentQuery(dirIndex, _c1, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.iconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.contentTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    hostVars: 23,
    hostBindings: function ToggleButton_HostBindings(rf, ctx) {
      if (rf & 1) {
        ɵɵlistener("keydown", function ToggleButton_keydown_HostBindingHandler($event) {
          return ctx.onKeyDown($event);
        })("click", function ToggleButton_click_HostBindingHandler($event) {
          return ctx.toggle($event);
        });
      }
      if (rf & 2) {
        ɵɵhostProperty("tabindex", ctx.tabindex);
        ɵɵattribute("disabled", ctx.disabled)("aria-labelledby", ctx.ariaLabelledBy)("aria-pressed", ctx.checked)("data-p-checked", ctx.active)("data-p-disabled", ctx.disabled)("type", "button");
        ɵɵclassMap(ctx.hostClass);
        ɵɵclassProp("p-togglebutton", true)("p-togglebutton-checked", ctx.checked)("p-disabled", ctx.disabled)("p-togglebutton-sm", ctx.size === "small")("p-inputfield-sm", ctx.size === "small")("p-togglebutton-lg", ctx.size === "large")("p-inputfield-lg", ctx.size === "large");
      }
    },
    inputs: {
      onLabel: "onLabel",
      offLabel: "offLabel",
      onIcon: "onIcon",
      offIcon: "offIcon",
      ariaLabel: "ariaLabel",
      ariaLabelledBy: "ariaLabelledBy",
      disabled: [2, "disabled", "disabled", booleanAttribute],
      style: "style",
      styleClass: "styleClass",
      inputId: "inputId",
      tabindex: [2, "tabindex", "tabindex", numberAttribute],
      size: "size",
      iconPos: "iconPos",
      autofocus: [2, "autofocus", "autofocus", booleanAttribute],
      allowEmpty: "allowEmpty"
    },
    outputs: {
      onChange: "onChange"
    },
    features: [ɵɵProvidersFeature([TOGGLEBUTTON_VALUE_ACCESSOR, ToggleButtonStyle]), ɵɵInputTransformsFeature, ɵɵHostDirectivesFeature([Ripple]), ɵɵInheritDefinitionFeature],
    decls: 3,
    vars: 6,
    consts: [[3, "ngClass"], [4, "ngTemplateOutlet", "ngTemplateOutletContext"], [3, "class", "ngClass"]],
    template: function ToggleButton_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵelementStart(0, "span", 0);
        ɵɵtemplate(1, ToggleButton_ng_container_1_Template, 1, 0, "ng-container", 1)(2, ToggleButton_Conditional_2_Template, 4, 4);
        ɵɵelementEnd();
      }
      if (rf & 2) {
        ɵɵproperty("ngClass", ctx.cx("content"));
        ɵɵadvance();
        ɵɵproperty("ngTemplateOutlet", ctx.contentTemplate || ctx._contentTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(4, _c2, ctx.checked));
        ɵɵadvance();
        ɵɵconditional(!ctx.contentTemplate ? 2 : -1);
      }
    },
    dependencies: [CommonModule, NgClass, NgTemplateOutlet, SharedModule],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ToggleButton, [{
    type: Component,
    args: [{
      selector: "p-toggleButton, p-togglebutton, p-toggle-button",
      standalone: true,
      imports: [CommonModule, SharedModule],
      hostDirectives: [{
        directive: Ripple
      }],
      host: {
        "[tabindex]": "tabindex",
        "[attr.disabled]": "disabled",
        "[attr.aria-labelledby]": "ariaLabelledBy",
        "[attr.aria-pressed]": "checked",
        "[attr.data-p-checked]": "active",
        "[attr.data-p-disabled]": "disabled",
        "[attr.type]": '"button"',
        "[class.p-togglebutton]": "true",
        "[class.p-togglebutton-checked]": "checked",
        "[class.p-disabled]": "disabled",
        "[class.p-togglebutton-sm]": 'size === "small"',
        "[class.p-inputfield-sm]": 'size === "small"',
        "[class.p-togglebutton-lg]": 'size === "large"',
        "[class.p-inputfield-lg]": 'size === "large"'
      },
      template: `<span [ngClass]="cx('content')">
        <ng-container *ngTemplateOutlet="contentTemplate || _contentTemplate; context: { $implicit: checked }"></ng-container>
        @if (!contentTemplate) {
            @if (!iconTemplate) {
                @if (onIcon || offIcon) {
                    <span
                        [class]="checked ? this.onIcon : this.offIcon"
                        [ngClass]="{
                            'p-togglebutton-icon': true,
                            'p-togglebutton-icon-left': iconPos === 'left',
                            'p-togglebutton-icon-right': iconPos === 'right'
                        }"
                        [attr.data-pc-section]="'icon'"
                    ></span>
                }
            } @else {
                <ng-container *ngTemplateOutlet="iconTemplate || _iconTemplate; context: { $implicit: checked }"></ng-container>
            }
            <span [ngClass]="cx('label')" [attr.data-pc-section]="'label'">{{ checked ? (hasOnLabel ? onLabel : ' ') : hasOffLabel ? offLabel : ' ' }}</span>
        }
    </span>`,
      providers: [TOGGLEBUTTON_VALUE_ACCESSOR, ToggleButtonStyle],
      changeDetection: ChangeDetectionStrategy.OnPush
    }]
  }], null, {
    hostClass: [{
      type: HostBinding,
      args: ["class"]
    }],
    onKeyDown: [{
      type: HostListener,
      args: ["keydown", ["$event"]]
    }],
    toggle: [{
      type: HostListener,
      args: ["click", ["$event"]]
    }],
    onLabel: [{
      type: Input
    }],
    offLabel: [{
      type: Input
    }],
    onIcon: [{
      type: Input
    }],
    offIcon: [{
      type: Input
    }],
    ariaLabel: [{
      type: Input
    }],
    ariaLabelledBy: [{
      type: Input
    }],
    disabled: [{
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
    inputId: [{
      type: Input
    }],
    tabindex: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    size: [{
      type: Input
    }],
    iconPos: [{
      type: Input
    }],
    autofocus: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    allowEmpty: [{
      type: Input
    }],
    onChange: [{
      type: Output
    }],
    iconTemplate: [{
      type: ContentChild,
      args: ["icon", {
        descendants: false
      }]
    }],
    contentTemplate: [{
      type: ContentChild,
      args: ["content", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var ToggleButtonModule = class _ToggleButtonModule {
  static ɵfac = function ToggleButtonModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ToggleButtonModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _ToggleButtonModule,
    imports: [ToggleButton, SharedModule],
    exports: [ToggleButton, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [ToggleButton, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ToggleButtonModule, [{
    type: NgModule,
    args: [{
      imports: [ToggleButton, SharedModule],
      exports: [ToggleButton, SharedModule]
    }]
  }], null, null);
})();

export {
  ToggleButtonStyle,
  ToggleButtonClasses,
  TOGGLEBUTTON_VALUE_ACCESSOR,
  ToggleButton,
  ToggleButtonModule
};
//# sourceMappingURL=chunk-2IBCCGHH.js.map
