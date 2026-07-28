import {
  Tooltip,
  TooltipModule
} from "./chunk-4EKXPA3K.js";
import {
  Badge,
  BadgeModule
} from "./chunk-KGQPIMGR.js";
import {
  zindexutils
} from "./chunk-KHDC5QWA.js";
import {
  Ripple
} from "./chunk-HQJN727C.js";
import {
  AngleRightIcon
} from "./chunk-G7GR35G3.js";
import "./chunk-5G7WYC4N.js";
import {
  BaseComponent
} from "./chunk-NKPC7AFX.js";
import "./chunk-3RXOYKMS.js";
import {
  BaseStyle
} from "./chunk-2GIPMIFT.js";
import {
  OverlayService,
  PrimeTemplate,
  SharedModule
} from "./chunk-NG5OZP5M.js";
import {
  appendChild,
  calculateScrollbarWidth,
  findSingle,
  focus,
  getHiddenElementOuterHeight,
  getHiddenElementOuterWidth,
  getOffset,
  getOuterWidth,
  getViewport,
  isAndroid,
  isIOS,
  removeChild,
  uuid
} from "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import {
  findLastIndex,
  isEmpty,
  isNotEmpty,
  isPrintableCharacter,
  resolve
} from "./chunk-PXYLXCRT.js";
import {
  animate,
  style,
  transition,
  trigger
} from "./chunk-PBR2IEBG.js";
import {
  RouterLink,
  RouterModule
} from "./chunk-M3JF7BPQ.js";
import "./chunk-HQ743VTI.js";
import "./chunk-ORXNKOXS.js";
import {
  CommonModule,
  NgClass,
  NgForOf,
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
  EventEmitter,
  Inject,
  Injectable,
  Input,
  NgModule,
  Output,
  ViewChild,
  ViewEncapsulation,
  booleanAttribute,
  effect,
  forwardRef,
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
  ɵɵdirectiveInject,
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
  ɵɵsanitizeHtml,
  ɵɵsanitizeUrl,
  ɵɵstyleMap,
  ɵɵtemplate,
  ɵɵtemplateRefExtractor,
  ɵɵtext,
  ɵɵtextInterpolate1,
  ɵɵviewQuery
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import "./chunk-FHTVLBLO.js";
import {
  __spreadProps,
  __spreadValues
} from "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-contextmenu.mjs
var _c0 = ["sublist"];
var _c1 = (a0, a1) => ({
  "p-contextmenu-submenu": a0,
  "p-contextmenu-root-list": a1
});
var _c2 = () => ({
  "p-contextmenu-item-link": true
});
var _c3 = () => ({
  class: "p-contextmenu-submenu-icon"
});
var _c4 = () => ({
  exact: false
});
var _c5 = (a0) => ({
  "p-contextmenu-item-link": true,
  "p-disabled": a0
});
var _c6 = (a0) => ({
  $implicit: a0
});
function ContextMenuSub_ul_0_ng_template_2_li_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "li", 8);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext().$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵstyleMap(ctx_r2.getItemProp(processedItem_r4, "style"));
    ɵɵproperty("ngClass", ctx_r2.getSeparatorItemClass(processedItem_r4));
    ɵɵattribute("id", ctx_r2.getItemId(processedItem_r4))("data-pc-section", "separator");
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_span_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 19);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("ngClass", ctx_r2.getItemProp(processedItem_r4, "icon"))("ngStyle", ctx_r2.getItemProp(processedItem_r4, "iconStyle"));
    ɵɵattribute("data-pc-section", "icon")("aria-hidden", true)("tabindex", -1);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_span_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 20);
    ɵɵtext(1);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵattribute("data-pc-section", "label");
    ɵɵadvance();
    ɵɵtextInterpolate1(" ", ctx_r2.getItemLabel(processedItem_r4), " ");
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_template_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 21);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("innerHTML", ctx_r2.getItemLabel(processedItem_r4), ɵɵsanitizeHtml);
    ɵɵattribute("data-pc-section", "label");
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_p_badge_5_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "p-badge", 22);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("styleClass", ctx_r2.getItemProp(processedItem_r4, "badgeStyleClass"))("value", ctx_r2.getItemProp(processedItem_r4, "badge"));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_AngleRightIcon_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "AngleRightIcon");
  }
  if (rf & 2) {
    ɵɵclassMap("p-contextmenu-submenu-icon");
    ɵɵattribute("data-pc-section", "submenuicon")("aria-hidden", true);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_2_ng_template_0_Template(rf, ctx) {
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_2_ng_template_0_Template, 0, 0, "ng-template", 25);
  }
  if (rf & 2) {
    ɵɵproperty("data-pc-section", "submenuicon")("aria-hidden", true);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_AngleRightIcon_1_Template, 1, 4, "AngleRightIcon", 23)(2, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_2_Template, 1, 2, null, 24);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext(6);
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r2.contextMenu.submenuIconTemplate && !ctx_r2.contextMenu._submenuIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r2.contextMenu.submenuIconTemplate || ctx_r2.contextMenu._submenuIconTemplate)("ngTemplateOutletContext", ɵɵpureFunction0(3, _c3));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "a", 15);
    ɵɵtemplate(1, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_span_1_Template, 1, 5, "span", 16)(2, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_span_2_Template, 2, 2, "span", 17)(3, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_template_3_Template, 1, 2, "ng-template", null, 2, ɵɵtemplateRefExtractor)(5, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_p_badge_5_Template, 1, 2, "p-badge", 18)(6, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_ng_container_6_Template, 3, 4, "ng-container", 11);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const htmlLabel_r6 = ɵɵreference(4);
    const processedItem_r4 = ɵɵnextContext(3).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("target", ctx_r2.getItemProp(processedItem_r4, "target"))("ngClass", ɵɵpureFunction0(12, _c2));
    ɵɵattribute("href", ctx_r2.getItemProp(processedItem_r4, "url"), ɵɵsanitizeUrl)("aria-hidden", true)("data-automationid", ctx_r2.getItemProp(processedItem_r4, "automationId"))("data-pc-section", "action")("tabindex", -1);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "icon"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "escape"))("ngIfElse", htmlLabel_r6);
    ɵɵadvance(3);
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "badge"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.isItemGroup(processedItem_r4));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_span_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 19);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("ngClass", ctx_r2.getItemProp(processedItem_r4, "icon"))("ngStyle", ctx_r2.getItemProp(processedItem_r4, "iconStyle"));
    ɵɵattribute("data-pc-section", "icon")("aria-hidden", true)("tabindex", -1);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_span_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 20);
    ɵɵtext(1);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵattribute("data-pc-section", "label");
    ɵɵadvance();
    ɵɵtextInterpolate1(" ", ctx_r2.getItemLabel(processedItem_r4), " ");
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_template_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "span", 21);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("innerHTML", ctx_r2.getItemLabel(processedItem_r4), ɵɵsanitizeHtml);
    ɵɵattribute("data-pc-section", "label");
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_p_badge_5_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "p-badge", 22);
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(4).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("styleClass", ctx_r2.getItemProp(processedItem_r4, "badgeStyleClass"))("value", ctx_r2.getItemProp(processedItem_r4, "badge"));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_AngleRightIcon_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "AngleRightIcon");
  }
  if (rf & 2) {
    ɵɵclassMap("p-contextmenu-submenu-icon");
    ɵɵattribute("data-pc-section", "submenuicon")("aria-hidden", true);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_2_ng_template_0_Template(rf, ctx) {
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_2_ng_template_0_Template, 0, 0, "ng-template", 25);
  }
  if (rf & 2) {
    ɵɵproperty("data-pc-section", "submenuicon")("aria-hidden", true);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_AngleRightIcon_1_Template, 1, 4, "AngleRightIcon", 23)(2, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_2_Template, 1, 2, null, 24);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext(6);
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r2.contextMenu.submenuIconTemplate && !ctx_r2.contextMenu._submenuIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", !ctx_r2.contextMenu.submenuIconTemplate || !ctx_r2.contextMenu._submenuIconTemplate)("ngTemplateOutletContext", ɵɵpureFunction0(3, _c3));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "a", 26);
    ɵɵtemplate(1, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_span_1_Template, 1, 5, "span", 16)(2, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_span_2_Template, 2, 2, "span", 17)(3, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_template_3_Template, 1, 2, "ng-template", null, 2, ɵɵtemplateRefExtractor)(5, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_p_badge_5_Template, 1, 2, "p-badge", 18)(6, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_ng_container_6_Template, 3, 4, "ng-container", 11);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const htmlLabel_r7 = ɵɵreference(4);
    const processedItem_r4 = ɵɵnextContext(3).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("routerLink", ctx_r2.getItemProp(processedItem_r4, "routerLink"))("queryParams", ctx_r2.getItemProp(processedItem_r4, "queryParams"))("routerLinkActiveOptions", ctx_r2.getItemProp(processedItem_r4, "routerLinkActiveOptions") || ɵɵpureFunction0(20, _c4))("target", ctx_r2.getItemProp(processedItem_r4, "target"))("ngClass", ɵɵpureFunction1(21, _c5, ctx_r2.getItemProp(processedItem_r4, "disabled")))("fragment", ctx_r2.getItemProp(processedItem_r4, "fragment"))("queryParamsHandling", ctx_r2.getItemProp(processedItem_r4, "queryParamsHandling"))("preserveFragment", ctx_r2.getItemProp(processedItem_r4, "preserveFragment"))("skipLocationChange", ctx_r2.getItemProp(processedItem_r4, "skipLocationChange"))("replaceUrl", ctx_r2.getItemProp(processedItem_r4, "replaceUrl"))("state", ctx_r2.getItemProp(processedItem_r4, "state"));
    ɵɵattribute("data-automationid", ctx_r2.getItemProp(processedItem_r4, "automationId"))("tabindex", -1)("aria-hidden", true)("data-pc-section", "action");
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "icon"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "escape"))("ngIfElse", htmlLabel_r7);
    ɵɵadvance(3);
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "badge"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.isItemGroup(processedItem_r4));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_1_Template, 7, 13, "a", 13)(2, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_a_2_Template, 7, 23, "a", 14);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(2).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r2.getItemProp(processedItem_r4, "routerLink"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.getItemProp(processedItem_r4, "routerLink"));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_4_1_ng_template_0_Template(rf, ctx) {
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_4_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_4_1_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_4_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_4_1_Template, 1, 0, null, 24);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(2).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r2.itemTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(2, _c6, processedItem_r4.item));
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_p_contextmenu_sub_5_Template(rf, ctx) {
  if (rf & 1) {
    const _r8 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "p-contextmenu-sub", 27);
    ɵɵlistener("itemClick", function ContextMenuSub_ul_0_ng_template_2_li_1_p_contextmenu_sub_5_Template_p_contextmenu_sub_itemClick_0_listener($event) {
      ɵɵrestoreView(_r8);
      const ctx_r2 = ɵɵnextContext(4);
      return ɵɵresetView(ctx_r2.itemClick.emit($event));
    })("itemMouseEnter", function ContextMenuSub_ul_0_ng_template_2_li_1_p_contextmenu_sub_5_Template_p_contextmenu_sub_itemMouseEnter_0_listener($event) {
      ɵɵrestoreView(_r8);
      const ctx_r2 = ɵɵnextContext(4);
      return ɵɵresetView(ctx_r2.onItemMouseEnter($event));
    });
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const processedItem_r4 = ɵɵnextContext(2).$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("items", processedItem_r4.items)("itemTemplate", ctx_r2.itemTemplate)("menuId", ctx_r2.menuId)("visible", ctx_r2.isItemActive(processedItem_r4) && ctx_r2.isItemGroup(processedItem_r4))("activeItemPath", ctx_r2.activeItemPath)("focusedItemId", ctx_r2.focusedItemId)("level", ctx_r2.level + 1);
  }
}
function ContextMenuSub_ul_0_ng_template_2_li_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r5 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "li", 9, 1)(2, "div", 10);
    ɵɵlistener("click", function ContextMenuSub_ul_0_ng_template_2_li_1_Template_div_click_2_listener($event) {
      ɵɵrestoreView(_r5);
      const processedItem_r4 = ɵɵnextContext().$implicit;
      const ctx_r2 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r2.onItemClick($event, processedItem_r4));
    })("mouseenter", function ContextMenuSub_ul_0_ng_template_2_li_1_Template_div_mouseenter_2_listener($event) {
      ɵɵrestoreView(_r5);
      const processedItem_r4 = ɵɵnextContext().$implicit;
      const ctx_r2 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r2.onItemMouseEnter({
        $event,
        processedItem: processedItem_r4
      }));
    });
    ɵɵtemplate(3, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_3_Template, 3, 2, "ng-container", 11)(4, ContextMenuSub_ul_0_ng_template_2_li_1_ng_container_4_Template, 2, 4, "ng-container", 11);
    ɵɵelementEnd();
    ɵɵtemplate(5, ContextMenuSub_ul_0_ng_template_2_li_1_p_contextmenu_sub_5_Template, 1, 7, "p-contextmenu-sub", 12);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r8 = ɵɵnextContext();
    const processedItem_r4 = ctx_r8.$implicit;
    const index_r10 = ctx_r8.index;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵclassMap(ctx_r2.getItemProp(processedItem_r4, "styleClass"));
    ɵɵproperty("ngStyle", ctx_r2.getItemProp(processedItem_r4, "style"))("ngClass", ctx_r2.getItemClass(processedItem_r4))("tooltipOptions", ctx_r2.getItemProp(processedItem_r4, "tooltipOptions"));
    ɵɵattribute("id", ctx_r2.getItemId(processedItem_r4))("data-pc-section", "menuitem")("data-p-highlight", ctx_r2.isItemActive(processedItem_r4))("data-p-focused", ctx_r2.isItemFocused(processedItem_r4))("data-p-disabled", ctx_r2.isItemDisabled(processedItem_r4))("aria-label", ctx_r2.getItemLabel(processedItem_r4))("aria-disabled", ctx_r2.isItemDisabled(processedItem_r4) || void 0)("aria-haspopup", ctx_r2.isItemGroup(processedItem_r4) && !ctx_r2.getItemProp(processedItem_r4, "to") ? "menu" : void 0)("aria-expanded", ctx_r2.isItemGroup(processedItem_r4) ? ctx_r2.isItemActive(processedItem_r4) : void 0)("aria-level", ctx_r2.level + 1)("aria-setsize", ctx_r2.getAriaSetSize())("aria-posinset", ctx_r2.getAriaPosInset(index_r10));
    ɵɵadvance(2);
    ɵɵattribute("data-pc-section", "content");
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r2.itemTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.itemTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.isItemVisible(processedItem_r4) && ctx_r2.isItemGroup(processedItem_r4));
  }
}
function ContextMenuSub_ul_0_ng_template_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, ContextMenuSub_ul_0_ng_template_2_li_0_Template, 1, 5, "li", 6)(1, ContextMenuSub_ul_0_ng_template_2_li_1_Template, 6, 21, "li", 7);
  }
  if (rf & 2) {
    const processedItem_r4 = ctx.$implicit;
    const ctx_r2 = ɵɵnextContext(2);
    ɵɵproperty("ngIf", ctx_r2.isItemVisible(processedItem_r4) && ctx_r2.getItemProp(processedItem_r4, "separator"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r2.isItemVisible(processedItem_r4) && !ctx_r2.getItemProp(processedItem_r4, "separator"));
  }
}
function ContextMenuSub_ul_0_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "ul", 4, 0);
    ɵɵlistener("@overlayAnimation.start", function ContextMenuSub_ul_0_Template_ul_animation_overlayAnimation_start_0_listener($event) {
      ɵɵrestoreView(_r1);
      const sublist_r2 = ɵɵreference(1);
      const ctx_r2 = ɵɵnextContext();
      return ɵɵresetView(ctx_r2.onEnter($event, sublist_r2));
    })("keydown", function ContextMenuSub_ul_0_Template_ul_keydown_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r2 = ɵɵnextContext();
      return ɵɵresetView(ctx_r2.menuKeydown.emit($event));
    })("focus", function ContextMenuSub_ul_0_Template_ul_focus_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r2 = ɵɵnextContext();
      return ɵɵresetView(ctx_r2.menuFocus.emit($event));
    })("blur", function ContextMenuSub_ul_0_Template_ul_blur_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r2 = ɵɵnextContext();
      return ɵɵresetView(ctx_r2.menuBlur.emit($event));
    });
    ɵɵtemplate(2, ContextMenuSub_ul_0_ng_template_2_Template, 2, 2, "ng-template", 5);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext();
    ɵɵproperty("ngClass", ɵɵpureFunction2(10, _c1, !ctx_r2.root, ctx_r2.root))("@overlayAnimation", ctx_r2.visible)("tabindex", ctx_r2.tabindex);
    ɵɵattribute("id", ctx_r2.menuId + "_list")("aria-label", ctx_r2.ariaLabel)("aria-labelledBy", ctx_r2.ariaLabelledBy)("aria-activedescendant", ctx_r2.focusedItemId)("aria-orientation", "vertical")("data-pc-section", "menu");
    ɵɵadvance(2);
    ɵɵproperty("ngForOf", ctx_r2.items);
  }
}
var _c7 = ["item"];
var _c8 = ["submenuicon"];
var _c9 = ["rootmenu"];
var _c10 = ["container"];
var _c11 = (a0) => ({
  "p-contextmenu p-component": true,
  "p-contextmenu-mobile": a0
});
var _c12 = () => ({
  value: "visible"
});
function ContextMenu_div_0_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "div", 3, 0);
    ɵɵlistener("@overlayAnimation.start", function ContextMenu_div_0_Template_div_animation_overlayAnimation_start_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onOverlayAnimationStart($event));
    })("@overlayAnimation.done", function ContextMenu_div_0_Template_div_animation_overlayAnimation_done_0_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onOverlayAnimationEnd($event));
    });
    ɵɵelementStart(2, "p-contextmenu-sub", 4, 1);
    ɵɵlistener("itemClick", function ContextMenu_div_0_Template_p_contextmenu_sub_itemClick_2_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onItemClick($event));
    })("menuFocus", function ContextMenu_div_0_Template_p_contextmenu_sub_menuFocus_2_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onMenuFocus($event));
    })("menuBlur", function ContextMenu_div_0_Template_p_contextmenu_sub_menuBlur_2_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onMenuBlur($event));
    })("menuKeydown", function ContextMenu_div_0_Template_p_contextmenu_sub_menuKeydown_2_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onKeyDown($event));
    })("itemMouseEnter", function ContextMenu_div_0_Template_p_contextmenu_sub_itemMouseEnter_2_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.onItemMouseEnter($event));
    });
    ɵɵelementEnd()();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵclassMap(ctx_r1.styleClass);
    ɵɵproperty("ngClass", ɵɵpureFunction1(20, _c11, ctx_r1.queryMatches))("ngStyle", ctx_r1.style)("@overlayAnimation", ɵɵpureFunction0(22, _c12));
    ɵɵattribute("data-pc-section", "root")("data-pc-name", "contextmenu")("id", ctx_r1.id);
    ɵɵadvance(2);
    ɵɵproperty("root", true)("items", ctx_r1.processedItems)("itemTemplate", ctx_r1.itemTemplate || ctx_r1._itemTemplate)("menuId", ctx_r1.id)("tabindex", !ctx_r1.disabled ? ctx_r1.tabindex : -1)("ariaLabel", ctx_r1.ariaLabel)("ariaLabelledBy", ctx_r1.ariaLabelledBy)("baseZIndex", ctx_r1.baseZIndex)("autoZIndex", ctx_r1.autoZIndex)("visible", ctx_r1.submenuVisible())("focusedItemId", ctx_r1.focused ? ctx_r1.focusedItemId : void 0)("activeItemPath", ctx_r1.activeItemPath());
  }
}
var theme = ({
  dt
}) => `
.p-contextmenu {
    position: absolute;
    background: ${dt("contextmenu.background")};
    color: ${dt("contextmenu.color")};
    border: 1px solid ${dt("contextmenu.border.color")};
    border-radius: ${dt("contextmenu.border.radius")};
    box-shadow: ${dt("contextmenu.shadow")};
    min-width: 12.5rem;
}

.p-contextmenu-root-list,
.p-contextmenu-submenu {
    margin: 0;
    padding: ${dt("contextmenu.list.padding")};
    list-style: none;
    outline: 0 none;
    display: flex;
    flex-direction: column;
    gap: ${dt("contextmenu.list.gap")};
}

.p-contextmenu-submenu {
    position: absolute;
    display: flex;
    flex-direction: column;
    min-width: 100%;
    z-index: 1;
    background: ${dt("contextmenu.background")};
    color: ${dt("contextmenu.color")};
    border: 1px solid ${dt("contextmenu.border.color")};
    border-radius: ${dt("contextmenu.border.radius")};
    box-shadow: ${dt("contextmenu.shadow")};
}

.p-contextmenu-item {
    position: relative;
}

.p-contextmenu-item-content {
    transition: background ${dt("contextmenu.transition.duration")}, color ${dt("contextmenu.transition.duration")};
    border-radius: ${dt("contextmenu.item.border.radius")};
    color: ${dt("contextmenu.item.color")};
}

.p-contextmenu-item-link {
    cursor: pointer;
    display: flex;
    align-items: center;
    text-decoration: none;
    overflow: hidden;
    position: relative;
    color: inherit;
    padding: ${dt("contextmenu.item.padding")};
    gap: ${dt("contextmenu.item.gap")};
    user-select: none;
}

.p-contextmenu-item-label {
    line-height: 1;
}

.p-contextmenu-item-icon {
    color: ${dt("contextmenu.item.icon.color")};
}

.p-contextmenu-submenu-icon {
    color: ${dt("contextmenu.submenu.icon.color")};
    margin-left: auto;
    font-size: ${dt("contextmenu.submenu.icon.size")};
    width: ${dt("contextmenu.submenu.icon.size")};
    height: ${dt("contextmenu.submenu.icon.size")};
}

.p-contextmenu-submenu-icon:dir(rtl) {
    margin-left: 0;
    margin-right: auto;
}

.p-contextmenu-item.p-focus > .p-contextmenu-item-content {
    color: ${dt("contextmenu.item.focus.color")};
    background: ${dt("contextmenu.item.focus.background")};
}

.p-contextmenu-item.p-focus > .p-contextmenu-item-content .p-contextmenu-item-icon {
    color: ${dt("contextmenu.item.icon.focus.color")};
}

.p-contextmenu-item.p-focus > .p-contextmenu-item-content .p-contextmenu-submenu-icon {
    color: ${dt("contextmenu.submenu.icon.focus.color")};
}

.p-contextmenu-item:not(.p-disabled) > .p-contextmenu-item-content:hover {
    color: ${dt("contextmenu.item.focus.color")};
    background: ${dt("contextmenu.item.focus.background")};
}

.p-contextmenu-item:not(.p-disabled) > .p-contextmenu-item-content:hover .p-contextmenu-item-icon {
    color: ${dt("contextmenu.item.icon.focus.color")};
}

.p-contextmenu-item:not(.p-disabled) > .p-contextmenu-item-content:hover .p-contextmenu-submenu-icon {
    color: ${dt("contextmenu.submenu.icon.focus.color")};
}

.p-contextmenu-item-active > .p-contextmenu-item-content {
    color: ${dt("contextmenu.item.active.color")};
    background: ${dt("contextmenu.item.active.background")};
}

.p-contextmenu-item-active > .p-contextmenu-item-content .p-contextmenu-item-icon {
    color: ${dt("contextmenu.item.icon.active.color")};
}

.p-contextmenu-item-active > .p-contextmenu-item-content .p-contextmenu-submenu-icon {
    color: ${dt("contextmenu.submenu.icon.active.color")};
}

.p-contextmenu-separator {
    border-top: 1px solid  ${dt("contextmenu.separator.border.color")};
}

.p-contextmenu-enter-from,
.p-contextmenu-leave-active {
    opacity: 0;
}

.p-contextmenu-enter-active {
    transition: opacity 250ms;
}

.p-contextmenu-mobile .p-contextmenu-submenu {
    position: static;
    box-shadow: none;
    border: 0 none;
    padding-left: ${dt("tieredmenu.submenu.mobile.indent")};
    padding-right: 0;
}

.p-contextmenu-mobile .p-contextmenu-submenu-icon {
    transition: transform 0.2s;
    transform: rotate(90deg);
}

.p-contextmenu-mobile .p-contextmenu-item-active > .p-contextmenu-item-content .p-contextmenu-submenu-icon {
    transform: rotate(-90deg);
}

/* For PrimeNG */
.p-contextmenu-submenu-icon.p-iconwrapper {
    margin-left: auto;
}

.p-contextmenu-submenu-icon.p-iconwrapper:dir(rtl) {
    margin-left: 0;
    margin-right: auto;
}
`;
var classes = {
  root: "p-contextmenu p-component",
  rootList: "p-contextmenu-root-list",
  item: ({
    instance,
    processedItem
  }) => ["p-contextmenu-item", {
    "p-contextmenu-item-active": instance.isItemActive(processedItem),
    "p-focus": instance.isItemFocused(processedItem),
    "p-disabled": instance.isItemDisabled(processedItem)
  }],
  itemContent: "p-contextmenu-item-content",
  itemLink: "p-contextmenu-item-link",
  itemIcon: "p-contextmenu-item-icon",
  itemLabel: "p-contextmenu-item-label",
  submenuIcon: "p-contextmenu-submenu-icon",
  submenu: "p-contextmenu-submenu",
  separator: "p-contextmenu-separator"
};
var ContextMenuStyle = class _ContextMenuStyle extends BaseStyle {
  name = "contextmenu";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵContextMenuStyle_BaseFactory;
    return function ContextMenuStyle_Factory(__ngFactoryType__) {
      return (ɵContextMenuStyle_BaseFactory || (ɵContextMenuStyle_BaseFactory = ɵɵgetInheritedFactory(_ContextMenuStyle)))(__ngFactoryType__ || _ContextMenuStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _ContextMenuStyle,
    factory: _ContextMenuStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ContextMenuStyle, [{
    type: Injectable
  }], null, null);
})();
var ContextMenuClasses;
(function(ContextMenuClasses2) {
  ContextMenuClasses2["root"] = "p-contextmenu";
  ContextMenuClasses2["rootList"] = "p-contextmenu-root-list";
  ContextMenuClasses2["item"] = "p-contextmenu-item";
  ContextMenuClasses2["itemContent"] = "p-contextmenu-item-content";
  ContextMenuClasses2["itemLink"] = "p-contextmenu-item-link";
  ContextMenuClasses2["itemIcon"] = "p-contextmenu-item-icon";
  ContextMenuClasses2["itemLabel"] = "p-contextmenu-item-label";
  ContextMenuClasses2["submenuIcon"] = "p-contextmenu-submenu-icon";
  ContextMenuClasses2["submenu"] = "p-contextmenu-submenu";
  ContextMenuClasses2["separator"] = "p-contextmenu-separator";
})(ContextMenuClasses || (ContextMenuClasses = {}));
var ContextMenuSub = class _ContextMenuSub extends BaseComponent {
  contextMenu;
  visible = false;
  items;
  itemTemplate;
  root = false;
  autoZIndex = true;
  baseZIndex = 0;
  popup;
  menuId;
  ariaLabel;
  ariaLabelledBy;
  level = 0;
  focusedItemId;
  activeItemPath;
  tabindex = 0;
  itemClick = new EventEmitter();
  itemMouseEnter = new EventEmitter();
  menuFocus = new EventEmitter();
  menuBlur = new EventEmitter();
  menuKeydown = new EventEmitter();
  sublistViewChild;
  constructor(contextMenu) {
    super();
    this.contextMenu = contextMenu;
  }
  getItemProp(processedItem, name, params = null) {
    return processedItem && processedItem.item ? resolve(processedItem.item[name], params) : void 0;
  }
  getItemId(processedItem) {
    return processedItem.item && processedItem.item?.id ? processedItem.item.id : `${this.menuId}_${processedItem.key}`;
  }
  getItemKey(processedItem) {
    return this.getItemId(processedItem);
  }
  getItemClass(processedItem) {
    return __spreadProps(__spreadValues({}, this.getItemProp(processedItem, "class")), {
      "p-contextmenu-item": true,
      "p-contextmenu-item-active": this.isItemActive(processedItem),
      "p-focus": this.isItemFocused(processedItem),
      "p-disabled": this.isItemDisabled(processedItem)
    });
  }
  getItemLabel(processedItem) {
    return this.getItemProp(processedItem, "label");
  }
  getSeparatorItemClass(processedItem) {
    return __spreadProps(__spreadValues({}, this.getItemProp(processedItem, "class")), {
      "p-contextmenu-separator": true
    });
  }
  getAriaSetSize() {
    return this.items.filter((processedItem) => this.isItemVisible(processedItem) && !this.getItemProp(processedItem, "separator")).length;
  }
  getAriaPosInset(index) {
    return index - this.items.slice(0, index).filter((processedItem) => this.isItemVisible(processedItem) && this.getItemProp(processedItem, "separator")).length + 1;
  }
  isItemVisible(processedItem) {
    return this.getItemProp(processedItem, "visible") !== false;
  }
  isItemActive(processedItem) {
    if (this.activeItemPath) {
      return this.activeItemPath.some((path) => path.key === processedItem.key);
    }
  }
  isItemDisabled(processedItem) {
    return this.getItemProp(processedItem, "disabled");
  }
  isItemFocused(processedItem) {
    return this.focusedItemId === this.getItemId(processedItem);
  }
  isItemGroup(processedItem) {
    return isNotEmpty(processedItem.items);
  }
  onItemMouseEnter(param) {
    const {
      event,
      processedItem
    } = param;
    this.itemMouseEnter.emit({
      originalEvent: event,
      processedItem
    });
  }
  onItemClick(event, processedItem) {
    this.getItemProp(processedItem, "command", {
      originalEvent: event,
      item: processedItem.item
    });
    this.itemClick.emit({
      originalEvent: event,
      processedItem,
      isFocus: true
    });
  }
  onEnter(event, sublist) {
    if (event.fromState === "void" && event.toState) {
      const sublist2 = event.element;
      this.position(sublist2);
    }
  }
  position(sublist) {
    const parentItem = sublist.parentElement.parentElement;
    const containerOffset = getOffset(sublist.parentElement.parentElement);
    const viewport = getViewport();
    const sublistWidth = sublist.offsetParent ? sublist.offsetWidth : getHiddenElementOuterWidth(sublist);
    const itemOuterWidth = getOuterWidth(parentItem.children[0]);
    sublist.style.top = "0px";
    if (parseInt(containerOffset.left, 10) + itemOuterWidth + sublistWidth > viewport.width - calculateScrollbarWidth()) {
      sublist.style.left = -1 * sublistWidth + "px";
    } else {
      sublist.style.left = itemOuterWidth + "px";
    }
  }
  static ɵfac = function ContextMenuSub_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ContextMenuSub)(ɵɵdirectiveInject(forwardRef(() => ContextMenu)));
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _ContextMenuSub,
    selectors: [["p-contextMenuSub"], ["p-contextmenu-sub"]],
    viewQuery: function ContextMenuSub_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c0, 5);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.sublistViewChild = _t.first);
      }
    },
    inputs: {
      visible: [2, "visible", "visible", booleanAttribute],
      items: "items",
      itemTemplate: "itemTemplate",
      root: [2, "root", "root", booleanAttribute],
      autoZIndex: [2, "autoZIndex", "autoZIndex", booleanAttribute],
      baseZIndex: [2, "baseZIndex", "baseZIndex", numberAttribute],
      popup: [2, "popup", "popup", booleanAttribute],
      menuId: "menuId",
      ariaLabel: "ariaLabel",
      ariaLabelledBy: "ariaLabelledBy",
      level: [2, "level", "level", numberAttribute],
      focusedItemId: "focusedItemId",
      activeItemPath: "activeItemPath",
      tabindex: [2, "tabindex", "tabindex", numberAttribute]
    },
    outputs: {
      itemClick: "itemClick",
      itemMouseEnter: "itemMouseEnter",
      menuFocus: "menuFocus",
      menuBlur: "menuBlur",
      menuKeydown: "menuKeydown"
    },
    features: [ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    decls: 1,
    vars: 1,
    consts: [["sublist", ""], ["listItem", ""], ["htmlLabel", ""], ["role", "menu", 3, "ngClass", "tabindex", "keydown", "focus", "blur", 4, "ngIf"], ["role", "menu", 3, "keydown", "focus", "blur", "ngClass", "tabindex"], ["ngFor", "", 3, "ngForOf"], ["role", "separator", 3, "style", "ngClass", 4, "ngIf"], ["role", "menuitem", "pTooltip", "", 3, "ngStyle", "ngClass", "class", "tooltipOptions", 4, "ngIf"], ["role", "separator", 3, "ngClass"], ["role", "menuitem", "pTooltip", "", 3, "ngStyle", "ngClass", "tooltipOptions"], [1, "p-contextmenu-item-content", 3, "click", "mouseenter"], [4, "ngIf"], [3, "items", "itemTemplate", "menuId", "visible", "activeItemPath", "focusedItemId", "level", "itemClick", "itemMouseEnter", 4, "ngIf"], ["pRipple", "", 3, "target", "ngClass", 4, "ngIf"], ["pRipple", "", 3, "routerLink", "queryParams", "routerLinkActiveOptions", "target", "ngClass", "fragment", "queryParamsHandling", "preserveFragment", "skipLocationChange", "replaceUrl", "state", 4, "ngIf"], ["pRipple", "", 3, "target", "ngClass"], ["class", "p-contextmenu-item-icon", 3, "ngClass", "ngStyle", 4, "ngIf"], ["class", "p-contextmenu-item-label", 4, "ngIf", "ngIfElse"], [3, "styleClass", "value", 4, "ngIf"], [1, "p-contextmenu-item-icon", 3, "ngClass", "ngStyle"], [1, "p-contextmenu-item-label"], [1, "p-contextmenu-item-label", 3, "innerHTML"], [3, "styleClass", "value"], [3, "class", 4, "ngIf"], [4, "ngTemplateOutlet", "ngTemplateOutletContext"], [3, "data-pc-section", "aria-hidden"], ["pRipple", "", 3, "routerLink", "queryParams", "routerLinkActiveOptions", "target", "ngClass", "fragment", "queryParamsHandling", "preserveFragment", "skipLocationChange", "replaceUrl", "state"], [3, "itemClick", "itemMouseEnter", "items", "itemTemplate", "menuId", "visible", "activeItemPath", "focusedItemId", "level"]],
    template: function ContextMenuSub_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵtemplate(0, ContextMenuSub_ul_0_Template, 3, 13, "ul", 3);
      }
      if (rf & 2) {
        ɵɵproperty("ngIf", ctx.root ? true : ctx.visible);
      }
    },
    dependencies: [_ContextMenuSub, CommonModule, NgClass, NgForOf, NgIf, NgTemplateOutlet, NgStyle, RouterModule, RouterLink, Ripple, TooltipModule, Tooltip, AngleRightIcon, BadgeModule, Badge, SharedModule],
    encapsulation: 2,
    data: {
      animation: [trigger("overlayAnimation", [transition(":enter", [style({
        opacity: 0
      })]), transition(":leave", [style({
        opacity: 0
      })])])]
    }
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ContextMenuSub, [{
    type: Component,
    args: [{
      selector: "p-contextMenuSub, p-contextmenu-sub",
      standalone: true,
      imports: [CommonModule, RouterModule, Ripple, TooltipModule, AngleRightIcon, BadgeModule, SharedModule],
      template: `
        <ul
            *ngIf="root ? true : visible"
            #sublist
            role="menu"
            [ngClass]="{ 'p-contextmenu-submenu': !root, 'p-contextmenu-root-list': root }"
            [@overlayAnimation]="visible"
            (@overlayAnimation.start)="onEnter($event, sublist)"
            [attr.id]="menuId + '_list'"
            [tabindex]="tabindex"
            [attr.aria-label]="ariaLabel"
            [attr.aria-labelledBy]="ariaLabelledBy"
            [attr.aria-activedescendant]="focusedItemId"
            [attr.aria-orientation]="'vertical'"
            [attr.data-pc-section]="'menu'"
            (keydown)="menuKeydown.emit($event)"
            (focus)="menuFocus.emit($event)"
            (blur)="menuBlur.emit($event)"
        >
            <ng-template ngFor let-processedItem [ngForOf]="items" let-index="index">
                <li
                    *ngIf="isItemVisible(processedItem) && getItemProp(processedItem, 'separator')"
                    [attr.id]="getItemId(processedItem)"
                    [style]="getItemProp(processedItem, 'style')"
                    [ngClass]="getSeparatorItemClass(processedItem)"
                    role="separator"
                    [attr.data-pc-section]="'separator'"
                ></li>
                <li
                    #listItem
                    *ngIf="isItemVisible(processedItem) && !getItemProp(processedItem, 'separator')"
                    role="menuitem"
                    [attr.id]="getItemId(processedItem)"
                    [attr.data-pc-section]="'menuitem'"
                    [attr.data-p-highlight]="isItemActive(processedItem)"
                    [attr.data-p-focused]="isItemFocused(processedItem)"
                    [attr.data-p-disabled]="isItemDisabled(processedItem)"
                    [attr.aria-label]="getItemLabel(processedItem)"
                    [attr.aria-disabled]="isItemDisabled(processedItem) || undefined"
                    [attr.aria-haspopup]="isItemGroup(processedItem) && !getItemProp(processedItem, 'to') ? 'menu' : undefined"
                    [attr.aria-expanded]="isItemGroup(processedItem) ? isItemActive(processedItem) : undefined"
                    [attr.aria-level]="level + 1"
                    [attr.aria-setsize]="getAriaSetSize()"
                    [attr.aria-posinset]="getAriaPosInset(index)"
                    [ngStyle]="getItemProp(processedItem, 'style')"
                    [ngClass]="getItemClass(processedItem)"
                    [class]="getItemProp(processedItem, 'styleClass')"
                    pTooltip
                    [tooltipOptions]="getItemProp(processedItem, 'tooltipOptions')"
                >
                    <div [attr.data-pc-section]="'content'" class="p-contextmenu-item-content" (click)="onItemClick($event, processedItem)" (mouseenter)="onItemMouseEnter({ $event, processedItem })">
                        <ng-container *ngIf="!itemTemplate">
                            <a
                                *ngIf="!getItemProp(processedItem, 'routerLink')"
                                [attr.href]="getItemProp(processedItem, 'url')"
                                [attr.aria-hidden]="true"
                                [attr.data-automationid]="getItemProp(processedItem, 'automationId')"
                                [attr.data-pc-section]="'action'"
                                [target]="getItemProp(processedItem, 'target')"
                                [ngClass]="{ 'p-contextmenu-item-link': true }"
                                [attr.tabindex]="-1"
                                pRipple
                            >
                                <span
                                    *ngIf="getItemProp(processedItem, 'icon')"
                                    class="p-contextmenu-item-icon"
                                    [ngClass]="getItemProp(processedItem, 'icon')"
                                    [ngStyle]="getItemProp(processedItem, 'iconStyle')"
                                    [attr.data-pc-section]="'icon'"
                                    [attr.aria-hidden]="true"
                                    [attr.tabindex]="-1"
                                >
                                </span>
                                <span *ngIf="getItemProp(processedItem, 'escape'); else htmlLabel" class="p-contextmenu-item-label" [attr.data-pc-section]="'label'">
                                    {{ getItemLabel(processedItem) }}
                                </span>
                                <ng-template #htmlLabel>
                                    <span class="p-contextmenu-item-label" [innerHTML]="getItemLabel(processedItem)" [attr.data-pc-section]="'label'"></span>
                                </ng-template>
                                <p-badge *ngIf="getItemProp(processedItem, 'badge')" [styleClass]="getItemProp(processedItem, 'badgeStyleClass')" [value]="getItemProp(processedItem, 'badge')" />
                                <ng-container *ngIf="isItemGroup(processedItem)">
                                    <AngleRightIcon *ngIf="!contextMenu.submenuIconTemplate && !contextMenu._submenuIconTemplate" [class]="'p-contextmenu-submenu-icon'" [attr.data-pc-section]="'submenuicon'" [attr.aria-hidden]="true" />
                                    <ng-template
                                        *ngTemplateOutlet="contextMenu.submenuIconTemplate || contextMenu._submenuIconTemplate; context: { class: 'p-contextmenu-submenu-icon' }"
                                        [attr.data-pc-section]="'submenuicon'"
                                        [attr.aria-hidden]="true"
                                    ></ng-template>
                                </ng-container>
                            </a>
                            <a
                                *ngIf="getItemProp(processedItem, 'routerLink')"
                                [routerLink]="getItemProp(processedItem, 'routerLink')"
                                [attr.data-automationid]="getItemProp(processedItem, 'automationId')"
                                [attr.tabindex]="-1"
                                [attr.aria-hidden]="true"
                                [attr.data-pc-section]="'action'"
                                [queryParams]="getItemProp(processedItem, 'queryParams')"
                                [routerLinkActiveOptions]="getItemProp(processedItem, 'routerLinkActiveOptions') || { exact: false }"
                                [target]="getItemProp(processedItem, 'target')"
                                [ngClass]="{ 'p-contextmenu-item-link': true, 'p-disabled': getItemProp(processedItem, 'disabled') }"
                                [fragment]="getItemProp(processedItem, 'fragment')"
                                [queryParamsHandling]="getItemProp(processedItem, 'queryParamsHandling')"
                                [preserveFragment]="getItemProp(processedItem, 'preserveFragment')"
                                [skipLocationChange]="getItemProp(processedItem, 'skipLocationChange')"
                                [replaceUrl]="getItemProp(processedItem, 'replaceUrl')"
                                [state]="getItemProp(processedItem, 'state')"
                                pRipple
                            >
                                <span
                                    *ngIf="getItemProp(processedItem, 'icon')"
                                    class="p-contextmenu-item-icon"
                                    [ngClass]="getItemProp(processedItem, 'icon')"
                                    [ngStyle]="getItemProp(processedItem, 'iconStyle')"
                                    [attr.data-pc-section]="'icon'"
                                    [attr.aria-hidden]="true"
                                    [attr.tabindex]="-1"
                                >
                                </span>
                                <span *ngIf="getItemProp(processedItem, 'escape'); else htmlLabel" class="p-contextmenu-item-label" [attr.data-pc-section]="'label'">
                                    {{ getItemLabel(processedItem) }}
                                </span>
                                <ng-template #htmlLabel>
                                    <span class="p-contextmenu-item-label" [innerHTML]="getItemLabel(processedItem)" [attr.data-pc-section]="'label'"></span>
                                </ng-template>
                                <p-badge *ngIf="getItemProp(processedItem, 'badge')" [styleClass]="getItemProp(processedItem, 'badgeStyleClass')" [value]="getItemProp(processedItem, 'badge')" />
                                <ng-container *ngIf="isItemGroup(processedItem)">
                                    <AngleRightIcon *ngIf="!contextMenu.submenuIconTemplate && !contextMenu._submenuIconTemplate" [class]="'p-contextmenu-submenu-icon'" [attr.data-pc-section]="'submenuicon'" [attr.aria-hidden]="true" />
                                    <ng-template
                                        *ngTemplateOutlet="!contextMenu.submenuIconTemplate || !contextMenu._submenuIconTemplate; context: { class: 'p-contextmenu-submenu-icon' }"
                                        [attr.data-pc-section]="'submenuicon'"
                                        [attr.aria-hidden]="true"
                                    ></ng-template>
                                </ng-container>
                            </a>
                        </ng-container>
                        <ng-container *ngIf="itemTemplate">
                            <ng-template *ngTemplateOutlet="itemTemplate; context: { $implicit: processedItem.item }"></ng-template>
                        </ng-container>
                    </div>

                    <p-contextmenu-sub
                        *ngIf="isItemVisible(processedItem) && isItemGroup(processedItem)"
                        [items]="processedItem.items"
                        [itemTemplate]="itemTemplate"
                        [menuId]="menuId"
                        [visible]="isItemActive(processedItem) && isItemGroup(processedItem)"
                        [activeItemPath]="activeItemPath"
                        [focusedItemId]="focusedItemId"
                        [level]="level + 1"
                        (itemClick)="itemClick.emit($event)"
                        (itemMouseEnter)="onItemMouseEnter($event)"
                    />
                </li>
            </ng-template>
        </ul>
    `,
      animations: [trigger("overlayAnimation", [transition(":enter", [style({
        opacity: 0
      })]), transition(":leave", [style({
        opacity: 0
      })])])],
      encapsulation: ViewEncapsulation.None
    }]
  }], () => [{
    type: ContextMenu,
    decorators: [{
      type: Inject,
      args: [forwardRef(() => ContextMenu)]
    }]
  }], {
    visible: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    items: [{
      type: Input
    }],
    itemTemplate: [{
      type: Input
    }],
    root: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
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
    popup: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    menuId: [{
      type: Input
    }],
    ariaLabel: [{
      type: Input
    }],
    ariaLabelledBy: [{
      type: Input
    }],
    level: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    focusedItemId: [{
      type: Input
    }],
    activeItemPath: [{
      type: Input
    }],
    tabindex: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    itemClick: [{
      type: Output
    }],
    itemMouseEnter: [{
      type: Output
    }],
    menuFocus: [{
      type: Output
    }],
    menuBlur: [{
      type: Output
    }],
    menuKeydown: [{
      type: Output
    }],
    sublistViewChild: [{
      type: ViewChild,
      args: ["sublist"]
    }]
  });
})();
var ContextMenu = class _ContextMenu extends BaseComponent {
  overlayService;
  /**
   * An array of menuitems.
   * @group Props
   */
  set model(value) {
    this._model = value;
    this._processedItems = this.createProcessedItems(this._model || []);
  }
  get model() {
    return this._model;
  }
  /**
   * Event for which the menu must be displayed.
   * @group Props
   */
  triggerEvent = "contextmenu";
  /**
   * Local template variable name of the element to attach the context menu.
   * @group Props
   */
  target;
  /**
   * Attaches the menu to document instead of a particular item.
   * @group Props
   */
  global;
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
   * Target element to attach the overlay, valid values are "body" or a local ng-template variable of another element.
   * @group Props
   */
  appendTo;
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
   * Current id state as a string.
   * @group Props
   */
  id;
  /**
   * The breakpoint to define the maximum width boundary.
   * @group Props
   */
  breakpoint = "960px";
  /**
   * Defines a string value that labels an interactive element.
   * @group Props
   */
  ariaLabel;
  /**
   * Identifier of the underlying input element.
   * @group Props
   */
  ariaLabelledBy;
  /**
   * Press delay in touch devices as miliseconds.
   * @group Props
   */
  pressDelay = 500;
  /**
   * Callback to invoke when overlay menu is shown.
   * @group Emits
   */
  onShow = new EventEmitter();
  /**
   * Callback to invoke when overlay menu is hidden.
   * @group Emits
   */
  onHide = new EventEmitter();
  rootmenu;
  containerViewChild;
  container;
  outsideClickListener;
  resizeListener;
  triggerEventListener;
  documentClickListener;
  documentTriggerListener;
  touchEndListener;
  pageX;
  pageY;
  visible = signal(false);
  relativeAlign;
  window;
  focused = false;
  activeItemPath = signal([]);
  focusedItemInfo = signal({
    index: -1,
    level: 0,
    parentKey: "",
    item: null
  });
  submenuVisible = signal(false);
  searchValue = "";
  searchTimeout;
  _processedItems;
  _model;
  pressTimer;
  matchMediaListener;
  query;
  queryMatches;
  _componentStyle = inject(ContextMenuStyle);
  get visibleItems() {
    const processedItem = this.activeItemPath().find((p) => p.key === this.focusedItemInfo().parentKey);
    return processedItem ? processedItem.items : this.processedItems;
  }
  get processedItems() {
    if (!this._processedItems || !this._processedItems.length) {
      this._processedItems = this.createProcessedItems(this.model || []);
    }
    return this._processedItems;
  }
  get focusedItemId() {
    const focusedItem = this.focusedItemInfo();
    return focusedItem.item && focusedItem.item?.id ? focusedItem.item.id : focusedItem.index !== -1 ? `${this.id}${isNotEmpty(focusedItem.parentKey) ? "_" + focusedItem.parentKey : ""}_${focusedItem.index}` : null;
  }
  constructor(overlayService) {
    super();
    this.overlayService = overlayService;
    effect(() => {
      const path = this.activeItemPath();
      if (isNotEmpty(path)) {
        this.bindGlobalListeners();
      } else if (!this.visible()) {
        this.unbindGlobalListeners();
      }
    });
  }
  ngOnInit() {
    super.ngOnInit();
    this.id = this.id || uuid("pn_id_");
    this.bindMatchMediaListener();
    this.bindTriggerEventListener();
  }
  isMobile() {
    return isIOS() || isAndroid();
  }
  bindTriggerEventListener() {
    if (isPlatformBrowser(this.platformId)) {
      if (!this.triggerEventListener) {
        if (!this.isMobile()) {
          if (this.global) {
            this.triggerEventListener = this.renderer.listen(this.document, this.triggerEvent, (event) => {
              this.show(event);
            });
          } else if (this.target) {
            this.triggerEventListener = this.renderer.listen(this.target, this.triggerEvent, (event) => {
              this.show(event);
            });
          }
        } else {
          if (this.global) {
            this.triggerEventListener = this.renderer.listen(this.document, "touchstart", this.onTouchStart.bind(this));
            this.touchEndListener = this.renderer.listen(this.document, "touchend", this.onTouchEnd.bind(this));
          } else if (this.target) {
            this.triggerEventListener = this.renderer.listen(this.target, "touchstart", this.onTouchStart.bind(this));
            this.touchEndListener = this.renderer.listen(this.target, "touchend", this.onTouchEnd.bind(this));
          }
        }
      }
    }
  }
  bindGlobalListeners() {
    if (isPlatformBrowser(this.platformId)) {
      if (!this.documentClickListener) {
        const documentTarget = this.el ? this.el.nativeElement.ownerDocument : "document";
        this.documentClickListener = this.renderer.listen(documentTarget, "click", (event) => {
          if (this.containerViewChild.nativeElement.offsetParent && this.isOutsideClicked(event) && !event.ctrlKey && event.button !== 2 && this.triggerEvent !== "click") {
            this.hide();
          }
        });
        this.documentTriggerListener = this.renderer.listen(documentTarget, this.triggerEvent, (event) => {
          if (this.containerViewChild.nativeElement.offsetParent && this.isOutsideClicked(event)) {
            this.hide();
          }
        });
      }
      if (!this.resizeListener) {
        this.resizeListener = this.renderer.listen(this.document.defaultView, "resize", (event) => {
          this.hide();
        });
      }
    }
  }
  /**
   * Defines template option for item.
   * @group Templates
   */
  itemTemplate;
  /**
   * Defines template option for submenuIcon.
   * @group Templates
   */
  submenuIconTemplate;
  templates;
  _submenuIconTemplate;
  _itemTemplate;
  ngAfterContentInit() {
    this.templates?.forEach((item) => {
      switch (item.getType()) {
        case "submenuicon":
          this._submenuIconTemplate = item.template;
          break;
        case "item":
          this._itemTemplate = item.template;
          break;
        default:
          this._itemTemplate = item.template;
          break;
      }
    });
  }
  createProcessedItems(items, level = 0, parent = {}, parentKey = "") {
    const processedItems = [];
    items && items.forEach((item, index) => {
      const key = (parentKey !== "" ? parentKey + "_" : "") + index;
      const newItem = {
        item,
        index,
        level,
        key,
        parent,
        parentKey
      };
      newItem["items"] = this.createProcessedItems(item.items, level + 1, newItem, key);
      processedItems.push(newItem);
    });
    return processedItems;
  }
  bindMatchMediaListener() {
    if (isPlatformBrowser(this.platformId)) {
      if (!this.matchMediaListener) {
        const query = window.matchMedia(`(max-width: ${this.breakpoint})`);
        this.query = query;
        this.queryMatches = query.matches;
        this.matchMediaListener = () => {
          this.queryMatches = query.matches;
        };
        query.addEventListener("change", this.matchMediaListener);
      }
    }
  }
  unbindMatchMediaListener() {
    if (this.matchMediaListener) {
      this.query.removeEventListener("change", this.matchMediaListener);
      this.matchMediaListener = null;
    }
  }
  getItemProp(item, name) {
    return item ? resolve(item[name]) : void 0;
  }
  getProccessedItemLabel(processedItem) {
    return processedItem ? this.getItemLabel(processedItem.item) : void 0;
  }
  getItemLabel(item) {
    return this.getItemProp(item, "label");
  }
  isProcessedItemGroup(processedItem) {
    return processedItem && isNotEmpty(processedItem.items);
  }
  isSelected(processedItem) {
    return this.activeItemPath().some((p) => p.key === processedItem.key);
  }
  isValidSelectedItem(processedItem) {
    return this.isValidItem(processedItem) && this.isSelected(processedItem);
  }
  isValidItem(processedItem) {
    return !!processedItem && !this.isItemDisabled(processedItem.item) && !this.isItemSeparator(processedItem.item);
  }
  isItemDisabled(item) {
    return this.getItemProp(item, "disabled");
  }
  isItemSeparator(item) {
    return this.getItemProp(item, "separator");
  }
  isItemMatched(processedItem) {
    return this.isValidItem(processedItem) && this.getProccessedItemLabel(processedItem).toLocaleLowerCase().startsWith(this.searchValue.toLocaleLowerCase());
  }
  isProccessedItemGroup(processedItem) {
    return processedItem && isNotEmpty(processedItem.items);
  }
  onItemClick(event) {
    const {
      processedItem
    } = event;
    const grouped = this.isProcessedItemGroup(processedItem);
    const selected = this.isSelected(processedItem);
    if (selected) {
      const {
        index,
        key,
        level,
        parentKey,
        item
      } = processedItem;
      this.activeItemPath.set(this.activeItemPath().filter((p) => key !== p.key && key.startsWith(p.key)));
      this.focusedItemInfo.set({
        index,
        level,
        parentKey,
        item
      });
      focus(this.rootmenu.sublistViewChild.nativeElement);
    } else {
      grouped ? this.onItemChange(event) : this.hide();
    }
  }
  onItemMouseEnter(event) {
    this.onItemChange(event, "hover");
  }
  onKeyDown(event) {
    const metaKey = event.metaKey || event.ctrlKey;
    switch (event.code) {
      case "ArrowDown":
        this.onArrowDownKey(event);
        break;
      case "ArrowUp":
        this.onArrowUpKey(event);
        break;
      case "ArrowLeft":
        this.onArrowLeftKey(event);
        break;
      case "ArrowRight":
        this.onArrowRightKey(event);
        break;
      case "Home":
        this.onHomeKey(event);
        break;
      case "End":
        this.onEndKey(event);
        break;
      case "Space":
        this.onSpaceKey(event);
        break;
      case "Enter":
        this.onEnterKey(event);
        break;
      case "Escape":
        this.onEscapeKey(event);
        break;
      case "Tab":
        this.onTabKey(event);
        break;
      case "PageDown":
      case "PageUp":
      case "Backspace":
      case "ShiftLeft":
      case "ShiftRight":
        break;
      default:
        if (!metaKey && isPrintableCharacter(event.key)) {
          this.searchItems(event, event.key);
        }
        break;
    }
  }
  onArrowDownKey(event) {
    const itemIndex = this.focusedItemInfo().index !== -1 ? this.findNextItemIndex(this.focusedItemInfo().index) : this.findFirstFocusedItemIndex();
    this.changeFocusedItemIndex(event, itemIndex);
    event.preventDefault();
  }
  onArrowRightKey(event) {
    const processedItem = this.visibleItems[this.focusedItemInfo().index];
    const grouped = this.isProccessedItemGroup(processedItem);
    if (grouped) {
      this.onItemChange({
        originalEvent: event,
        processedItem
      });
      this.focusedItemInfo.set({
        index: -1,
        parentKey: processedItem.key,
        item: processedItem.item
      });
      this.searchValue = "";
      this.onArrowDownKey(event);
    }
    event.preventDefault();
  }
  onArrowUpKey(event) {
    if (event.altKey) {
      if (this.focusedItemInfo().index !== -1) {
        const processedItem = this.visibleItems[this.focusedItemInfo().index];
        const grouped = this.isProccessedItemGroup(processedItem);
        !grouped && this.onItemChange({
          originalEvent: event,
          processedItem
        });
      }
      this.hide();
      event.preventDefault();
    } else {
      const itemIndex = this.focusedItemInfo().index !== -1 ? this.findPrevItemIndex(this.focusedItemInfo().index) : this.findLastFocusedItemIndex();
      this.changeFocusedItemIndex(event, itemIndex);
      event.preventDefault();
    }
  }
  onArrowLeftKey(event) {
    const processedItem = this.visibleItems[this.focusedItemInfo().index];
    const parentItem = this.activeItemPath().find((p) => p.key === processedItem.parentKey);
    const root = isEmpty(processedItem.parent);
    if (!root) {
      this.focusedItemInfo.set({
        index: -1,
        parentKey: parentItem ? parentItem.parentKey : "",
        item: processedItem.item
      });
      this.searchValue = "";
      this.onArrowDownKey(event);
    }
    const activeItemPath = this.activeItemPath().filter((p) => p.parentKey !== this.focusedItemInfo().parentKey);
    this.activeItemPath.set(activeItemPath);
    event.preventDefault();
  }
  onHomeKey(event) {
    this.changeFocusedItemIndex(event, this.findFirstItemIndex());
    event.preventDefault();
  }
  onEndKey(event) {
    this.changeFocusedItemIndex(event, this.findLastItemIndex());
    event.preventDefault();
  }
  onSpaceKey(event) {
    this.onEnterKey(event);
  }
  onEscapeKey(event) {
    this.hide();
    const processedItem = this.findVisibleItem(this.findFirstFocusedItemIndex());
    const focusedItemInfo = this.focusedItemInfo();
    this.focusedItemInfo.set(__spreadProps(__spreadValues({}, focusedItemInfo), {
      index: this.findFirstFocusedItemIndex(),
      item: processedItem.item
    }));
    event.preventDefault();
  }
  onTabKey(event) {
    if (this.focusedItemInfo().index !== -1) {
      const processedItem = this.visibleItems[this.focusedItemInfo().index];
      const grouped = this.isProccessedItemGroup(processedItem);
      !grouped && this.onItemChange({
        originalEvent: event,
        processedItem
      });
    }
    this.hide();
  }
  onEnterKey(event) {
    if (this.focusedItemInfo().index !== -1) {
      const element = findSingle(this.rootmenu.el.nativeElement, `li[id="${`${this.focusedItemId}`}"]`);
      const anchorElement = element && findSingle(element, 'a[data-pc-section="action"]');
      anchorElement ? anchorElement.click() : element && element.click();
      const processedItem = this.visibleItems[this.focusedItemInfo().index];
      const grouped = this.isProccessedItemGroup(processedItem);
      if (!grouped) {
        const focusedItemInfo = this.focusedItemInfo();
        this.focusedItemInfo.set(__spreadProps(__spreadValues({}, focusedItemInfo), {
          index: this.findFirstFocusedItemIndex()
        }));
      }
    }
    event.preventDefault();
  }
  onItemChange(event, type) {
    const {
      processedItem,
      isFocus
    } = event;
    if (isEmpty(processedItem)) return;
    const {
      index,
      key,
      level,
      parentKey,
      items
    } = processedItem;
    const grouped = isNotEmpty(items);
    const activeItemPath = this.activeItemPath().filter((p) => p.parentKey !== parentKey && p.parentKey !== key);
    if (grouped) {
      activeItemPath.push(processedItem);
      this.submenuVisible.set(true);
    }
    this.focusedItemInfo.set({
      index,
      level,
      parentKey,
      item: processedItem.item
    });
    isFocus && focus(this.rootmenu.sublistViewChild.nativeElement);
    if (type === "hover" && this.queryMatches) {
      return;
    }
    this.activeItemPath.set(activeItemPath);
  }
  onMenuFocus(event) {
    this.focused = true;
    const focusedItemInfo = this.focusedItemInfo().index !== -1 ? this.focusedItemInfo() : {
      index: -1,
      level: 0,
      parentKey: "",
      item: null
    };
    this.focusedItemInfo.set(focusedItemInfo);
  }
  onMenuBlur(event) {
    this.focused = false;
    this.focusedItemInfo.set({
      index: -1,
      level: 0,
      parentKey: "",
      item: null
    });
    this.searchValue = "";
  }
  onOverlayAnimationStart(event) {
    switch (event.toState) {
      case "visible":
        this.container = event.element;
        this.position();
        this.moveOnTop();
        this.appendOverlay();
        this.bindGlobalListeners();
        focus(this.rootmenu.sublistViewChild.nativeElement);
        break;
    }
  }
  onOverlayAnimationEnd(event) {
    switch (event.toState) {
      case "void":
        this.onOverlayHide();
        break;
    }
  }
  appendOverlay() {
    if (this.appendTo) {
      if (this.appendTo === "body") this.renderer.appendChild(this.document.body, this.containerViewChild.nativeElement);
      else appendChild(this.appendTo, this.containerViewChild.nativeElement);
    }
  }
  moveOnTop() {
    if (this.autoZIndex && this.containerViewChild) {
      zindexutils.set("menu", this.containerViewChild.nativeElement, this.baseZIndex + this.config.zIndex.menu);
    }
  }
  onOverlayHide() {
    this.unbindGlobalListeners();
    if (!this.cd.destroyed) {
      this.target = null;
    }
    if (this.container && this.autoZIndex) {
      zindexutils.clear(this.container);
    }
    this.container = null;
  }
  onTouchStart(event) {
    this.pressTimer = setTimeout(() => {
      this.show(event);
    }, this.pressDelay);
  }
  onTouchEnd() {
    clearTimeout(this.pressTimer);
  }
  hide() {
    this.visible.set(false);
    this.onHide.emit();
    this.activeItemPath.set([]);
    this.focusedItemInfo.set({
      index: -1,
      level: 0,
      parentKey: "",
      item: null
    });
  }
  toggle(event) {
    this.visible() ? this.hide() : this.show(event);
  }
  show(event) {
    this.activeItemPath.set([]);
    this.focusedItemInfo.set({
      index: -1,
      level: 0,
      parentKey: "",
      item: null
    });
    this.pageX = event.pageX;
    this.pageY = event.pageY;
    this.onShow.emit();
    this.visible() ? this.position() : this.visible.set(true);
    event.stopPropagation();
    event.preventDefault();
  }
  position() {
    let left = this.pageX + 1;
    let top = this.pageY + 1;
    let width = this.containerViewChild.nativeElement.offsetParent ? this.containerViewChild.nativeElement.offsetWidth : getHiddenElementOuterWidth(this.containerViewChild.nativeElement);
    let height = this.containerViewChild.nativeElement.offsetParent ? this.containerViewChild.nativeElement.offsetHeight : getHiddenElementOuterHeight(this.containerViewChild.nativeElement);
    let viewport = getViewport();
    if (left + width - this.document.scrollingElement.scrollLeft > viewport.width) {
      left -= width;
    }
    if (top + height - this.document.scrollingElement.scrollTop > viewport.height) {
      top -= height;
    }
    if (left < this.document.scrollingElement.scrollLeft) {
      left = this.document.scrollingElement.scrollLeft;
    }
    if (top < this.document.scrollingElement.scrollTop) {
      top = this.document.scrollingElement.scrollTop;
    }
    this.containerViewChild.nativeElement.style.left = left + "px";
    this.containerViewChild.nativeElement.style.top = top + "px";
  }
  searchItems(event, char) {
    this.searchValue = (this.searchValue || "") + char;
    let itemIndex = -1;
    let matched = false;
    if (this.focusedItemInfo().index !== -1) {
      itemIndex = this.visibleItems.slice(this.focusedItemInfo().index).findIndex((processedItem) => this.isItemMatched(processedItem));
      itemIndex = itemIndex === -1 ? this.visibleItems.slice(0, this.focusedItemInfo().index).findIndex((processedItem) => this.isItemMatched(processedItem)) : itemIndex + this.focusedItemInfo().index;
    } else {
      itemIndex = this.visibleItems.findIndex((processedItem) => this.isItemMatched(processedItem));
    }
    if (itemIndex !== -1) {
      matched = true;
    }
    if (itemIndex === -1 && this.focusedItemInfo().index === -1) {
      itemIndex = this.findFirstFocusedItemIndex();
    }
    if (itemIndex !== -1) {
      this.changeFocusedItemIndex(event, itemIndex);
    }
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.searchValue = "";
      this.searchTimeout = null;
    }, 500);
    return matched;
  }
  findVisibleItem(index) {
    return isNotEmpty(this.visibleItems) ? this.visibleItems[index] : null;
  }
  findLastFocusedItemIndex() {
    const selectedIndex = this.findSelectedItemIndex();
    return selectedIndex < 0 ? this.findLastItemIndex() : selectedIndex;
  }
  findLastItemIndex() {
    return findLastIndex(this.visibleItems, (processedItem) => this.isValidItem(processedItem));
  }
  findPrevItemIndex(index) {
    const matchedItemIndex = index > 0 ? findLastIndex(this.visibleItems.slice(0, index), (processedItem) => this.isValidItem(processedItem)) : -1;
    return matchedItemIndex > -1 ? matchedItemIndex : index;
  }
  findNextItemIndex(index) {
    const matchedItemIndex = index < this.visibleItems.length - 1 ? this.visibleItems.slice(index + 1).findIndex((processedItem) => this.isValidItem(processedItem)) : -1;
    return matchedItemIndex > -1 ? matchedItemIndex + index + 1 : index;
  }
  findFirstFocusedItemIndex() {
    const selectedIndex = this.findSelectedItemIndex();
    return selectedIndex < 0 ? this.findFirstItemIndex() : selectedIndex;
  }
  findFirstItemIndex() {
    return this.visibleItems.findIndex((processedItem) => this.isValidItem(processedItem));
  }
  findSelectedItemIndex() {
    return this.visibleItems.findIndex((processedItem) => this.isValidSelectedItem(processedItem));
  }
  changeFocusedItemIndex(event, index) {
    const processedItem = this.findVisibleItem(index);
    const focusedItemInfo = this.focusedItemInfo();
    if (focusedItemInfo.index !== index) {
      this.focusedItemInfo.set(__spreadProps(__spreadValues({}, focusedItemInfo), {
        index,
        item: processedItem.item
      }));
      this.scrollInView();
    }
  }
  scrollInView(index = -1) {
    const id = index !== -1 ? `${this.id}_${index}` : this.focusedItemId;
    const element = findSingle(this.rootmenu.el.nativeElement, `li[id="${id}"]`);
    if (element) {
      element.scrollIntoView && element.scrollIntoView({
        block: "nearest",
        inline: "nearest"
      });
    }
  }
  bindResizeListener() {
    if (isPlatformBrowser(this.platformId)) {
      if (!this.resizeListener) {
        this.resizeListener = this.renderer.listen(this.document.defaultView, "resize", (event) => {
          this.hide();
        });
      }
    }
  }
  isOutsideClicked(event) {
    return !(this.containerViewChild.nativeElement.isSameNode(event.target) || this.containerViewChild.nativeElement.contains(event.target));
  }
  unbindResizeListener() {
    if (this.resizeListener) {
      this.resizeListener();
      this.resizeListener = null;
    }
  }
  unbindGlobalListeners() {
    if (this.documentClickListener) {
      this.documentClickListener();
      this.documentClickListener = null;
    }
    if (this.documentTriggerListener) {
      this.documentTriggerListener();
      this.documentTriggerListener = null;
    }
    if (this.resizeListener) {
      this.resizeListener();
      this.resizeListener = null;
    }
    if (this.touchEndListener) {
      this.touchEndListener();
      this.touchEndListener = null;
    }
  }
  unbindTriggerEventListener() {
    if (this.triggerEventListener) {
      this.triggerEventListener();
      this.triggerEventListener = null;
    }
  }
  removeAppendedElements() {
    if (this.appendTo && this.containerViewChild) {
      if (this.appendTo === "body") {
        this.renderer.removeChild(this.document.body, this.containerViewChild.nativeElement);
      } else {
        removeChild(this.containerViewChild.nativeElement, this.appendTo);
      }
    }
  }
  ngOnDestroy() {
    this.unbindGlobalListeners();
    this.unbindTriggerEventListener();
    this.unbindMatchMediaListener();
    this.removeAppendedElements();
    super.ngOnDestroy();
  }
  static ɵfac = function ContextMenu_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ContextMenu)(ɵɵdirectiveInject(OverlayService));
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _ContextMenu,
    selectors: [["p-contextMenu"], ["p-contextmenu"], ["p-context-menu"]],
    contentQueries: function ContextMenu_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c7, 4);
        ɵɵcontentQuery(dirIndex, _c8, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.itemTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.submenuIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    viewQuery: function ContextMenu_Query(rf, ctx) {
      if (rf & 1) {
        ɵɵviewQuery(_c9, 5);
        ɵɵviewQuery(_c10, 5);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.rootmenu = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.containerViewChild = _t.first);
      }
    },
    inputs: {
      model: "model",
      triggerEvent: "triggerEvent",
      target: "target",
      global: [2, "global", "global", booleanAttribute],
      style: "style",
      styleClass: "styleClass",
      appendTo: "appendTo",
      autoZIndex: [2, "autoZIndex", "autoZIndex", booleanAttribute],
      baseZIndex: [2, "baseZIndex", "baseZIndex", numberAttribute],
      id: "id",
      breakpoint: "breakpoint",
      ariaLabel: "ariaLabel",
      ariaLabelledBy: "ariaLabelledBy",
      pressDelay: [2, "pressDelay", "pressDelay", numberAttribute]
    },
    outputs: {
      onShow: "onShow",
      onHide: "onHide"
    },
    features: [ɵɵProvidersFeature([ContextMenuStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature],
    decls: 1,
    vars: 1,
    consts: [["container", ""], ["rootmenu", ""], [3, "ngClass", "class", "ngStyle", 4, "ngIf"], [3, "ngClass", "ngStyle"], [3, "itemClick", "menuFocus", "menuBlur", "menuKeydown", "itemMouseEnter", "root", "items", "itemTemplate", "menuId", "tabindex", "ariaLabel", "ariaLabelledBy", "baseZIndex", "autoZIndex", "visible", "focusedItemId", "activeItemPath"]],
    template: function ContextMenu_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵtemplate(0, ContextMenu_div_0_Template, 4, 23, "div", 2);
      }
      if (rf & 2) {
        ɵɵproperty("ngIf", ctx.visible());
      }
    },
    dependencies: [CommonModule, NgClass, NgIf, NgStyle, ContextMenuSub, RouterModule, TooltipModule, BadgeModule, SharedModule],
    encapsulation: 2,
    data: {
      animation: [trigger("overlayAnimation", [transition(":enter", [style({
        opacity: 0
      }), animate("250ms")]), transition(":leave", [animate(".1s linear", style({
        opacity: 0
      }))])])]
    },
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ContextMenu, [{
    type: Component,
    args: [{
      selector: "p-contextMenu, p-contextmenu, p-context-menu",
      standalone: true,
      imports: [CommonModule, ContextMenuSub, RouterModule, TooltipModule, BadgeModule, SharedModule],
      template: `
        <div
            #container
            [attr.data-pc-section]="'root'"
            [attr.data-pc-name]="'contextmenu'"
            [attr.id]="id"
            [ngClass]="{ 'p-contextmenu p-component': true, 'p-contextmenu-mobile': queryMatches }"
            [class]="styleClass"
            [ngStyle]="style"
            [@overlayAnimation]="{ value: 'visible' }"
            (@overlayAnimation.start)="onOverlayAnimationStart($event)"
            (@overlayAnimation.done)="onOverlayAnimationEnd($event)"
            *ngIf="visible()"
        >
            <p-contextmenu-sub
                #rootmenu
                [root]="true"
                [items]="processedItems"
                [itemTemplate]="itemTemplate || _itemTemplate"
                [menuId]="id"
                [tabindex]="!disabled ? tabindex : -1"
                [ariaLabel]="ariaLabel"
                [ariaLabelledBy]="ariaLabelledBy"
                [baseZIndex]="baseZIndex"
                [autoZIndex]="autoZIndex"
                [visible]="submenuVisible()"
                [focusedItemId]="focused ? focusedItemId : undefined"
                [activeItemPath]="activeItemPath()"
                (itemClick)="onItemClick($event)"
                (menuFocus)="onMenuFocus($event)"
                (menuBlur)="onMenuBlur($event)"
                (menuKeydown)="onKeyDown($event)"
                (itemMouseEnter)="onItemMouseEnter($event)"
            />
        </div>
    `,
      animations: [trigger("overlayAnimation", [transition(":enter", [style({
        opacity: 0
      }), animate("250ms")]), transition(":leave", [animate(".1s linear", style({
        opacity: 0
      }))])])],
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [ContextMenuStyle]
    }]
  }], () => [{
    type: OverlayService
  }], {
    model: [{
      type: Input
    }],
    triggerEvent: [{
      type: Input
    }],
    target: [{
      type: Input
    }],
    global: [{
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
    appendTo: [{
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
    id: [{
      type: Input
    }],
    breakpoint: [{
      type: Input
    }],
    ariaLabel: [{
      type: Input
    }],
    ariaLabelledBy: [{
      type: Input
    }],
    pressDelay: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    onShow: [{
      type: Output
    }],
    onHide: [{
      type: Output
    }],
    rootmenu: [{
      type: ViewChild,
      args: ["rootmenu"]
    }],
    containerViewChild: [{
      type: ViewChild,
      args: ["container"]
    }],
    itemTemplate: [{
      type: ContentChild,
      args: ["item", {
        descendants: false
      }]
    }],
    submenuIconTemplate: [{
      type: ContentChild,
      args: ["submenuicon", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var ContextMenuModule = class _ContextMenuModule {
  static ɵfac = function ContextMenuModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _ContextMenuModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _ContextMenuModule,
    imports: [ContextMenu, SharedModule],
    exports: [ContextMenu, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [ContextMenu, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(ContextMenuModule, [{
    type: NgModule,
    args: [{
      imports: [ContextMenu, SharedModule],
      exports: [ContextMenu, SharedModule]
    }]
  }], null, null);
})();
export {
  ContextMenu,
  ContextMenuClasses,
  ContextMenuModule,
  ContextMenuStyle,
  ContextMenuSub
};
//# sourceMappingURL=primeng_contextmenu.js.map
