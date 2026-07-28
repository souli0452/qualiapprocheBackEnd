import {
  Select
} from "./chunk-2S5KUDD5.js";
import {
  InputNumber
} from "./chunk-5LJWEKOG.js";
import {
  Ripple
} from "./chunk-HQJN727C.js";
import {
  AngleDoubleLeftIcon,
  AngleDoubleRightIcon,
  AngleLeftIcon,
  AngleRightIcon
} from "./chunk-G7GR35G3.js";
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
  FormsModule,
  NgControlStatus,
  NgModel
} from "./chunk-Q4QQ2CLE.js";
import {
  CommonModule,
  NgClass,
  NgForOf,
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
  numberAttribute,
  setClassMetadata,
  ɵɵInheritDefinitionFeature,
  ɵɵInputTransformsFeature,
  ɵɵNgOnChangesFeature,
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
  ɵɵqueryRefresh,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtemplate,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtextInterpolate1,
  ɵɵtwoWayBindingSet,
  ɵɵtwoWayListener,
  ɵɵtwoWayProperty
} from "./chunk-ECNLMTOX.js";

// node_modules/primeng/fesm2022/primeng-paginator.mjs
var _c0 = ["dropdownicon"];
var _c1 = ["firstpagelinkicon"];
var _c2 = ["previouspagelinkicon"];
var _c3 = ["lastpagelinkicon"];
var _c4 = ["nextpagelinkicon"];
var _c5 = (a0) => ({
  "p-disabled": a0
});
var _c6 = (a0) => ({
  $implicit: a0
});
var _c7 = (a0) => ({
  "p-paginator-page-selected": a0
});
function Paginator_div_0_div_1_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Paginator_div_0_div_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "div", 16);
    ɵɵtemplate(1, Paginator_div_0_div_1_ng_container_1_Template, 1, 0, "ng-container", 17);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵattribute("data-pc-section", "start");
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.templateLeft)("ngTemplateOutletContext", ɵɵpureFunction1(3, _c6, ctx_r1.paginatorState));
  }
}
function Paginator_div_0_span_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 18);
    ɵɵtext(1);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵtextInterpolate(ctx_r1.currentPageReport);
  }
}
function Paginator_div_0_button_3_AngleDoubleLeftIcon_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "AngleDoubleLeftIcon", 21);
  }
  if (rf & 2) {
    ɵɵproperty("styleClass", "p-paginator-first-icon");
  }
}
function Paginator_div_0_button_3_span_2_1_ng_template_0_Template(rf, ctx) {
}
function Paginator_div_0_button_3_span_2_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_button_3_span_2_1_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Paginator_div_0_button_3_span_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 22);
    ɵɵtemplate(1, Paginator_div_0_button_3_span_2_1_Template, 1, 0, null, 23);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.firstPageLinkIconTemplate || ctx_r1._firstPageLinkIconTemplate);
  }
}
function Paginator_div_0_button_3_Template(rf, ctx) {
  if (rf & 1) {
    const _r3 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 19);
    ɵɵlistener("click", function Paginator_div_0_button_3_Template_button_click_0_listener($event) {
      ɵɵrestoreView(_r3);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.changePageToFirst($event));
    });
    ɵɵtemplate(1, Paginator_div_0_button_3_AngleDoubleLeftIcon_1_Template, 1, 1, "AngleDoubleLeftIcon", 6)(2, Paginator_div_0_button_3_span_2_Template, 2, 1, "span", 20);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("disabled", ctx_r1.isFirstPage() || ctx_r1.empty())("ngClass", ɵɵpureFunction1(5, _c5, ctx_r1.isFirstPage() || ctx_r1.empty()));
    ɵɵattribute("aria-label", ctx_r1.getAriaLabel("firstPageLabel"));
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.firstPageLinkIconTemplate && !ctx_r1._firstPageLinkIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.firstPageLinkIconTemplate || ctx_r1._firstPageLinkIconTemplate);
  }
}
function Paginator_div_0_AngleLeftIcon_5_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "AngleLeftIcon", 21);
  }
  if (rf & 2) {
    ɵɵproperty("styleClass", "p-paginator-prev-icon");
  }
}
function Paginator_div_0_span_6_1_ng_template_0_Template(rf, ctx) {
}
function Paginator_div_0_span_6_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_span_6_1_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Paginator_div_0_span_6_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 24);
    ɵɵtemplate(1, Paginator_div_0_span_6_1_Template, 1, 0, null, 23);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.previousPageLinkIconTemplate || ctx_r1._previousPageLinkIconTemplate);
  }
}
function Paginator_div_0_span_7_button_1_Template(rf, ctx) {
  if (rf & 1) {
    const _r4 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 27);
    ɵɵlistener("click", function Paginator_div_0_span_7_button_1_Template_button_click_0_listener($event) {
      const pageLink_r5 = ɵɵrestoreView(_r4).$implicit;
      const ctx_r1 = ɵɵnextContext(3);
      return ɵɵresetView(ctx_r1.onPageLinkClick($event, pageLink_r5 - 1));
    });
    ɵɵtext(1);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const pageLink_r5 = ctx.$implicit;
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵproperty("ngClass", ɵɵpureFunction1(4, _c7, pageLink_r5 - 1 == ctx_r1.getPage()));
    ɵɵattribute("aria-label", ctx_r1.getPageAriaLabel(pageLink_r5))("aria-current", pageLink_r5 - 1 == ctx_r1.getPage() ? "page" : void 0);
    ɵɵadvance();
    ɵɵtextInterpolate1(" ", ctx_r1.getLocalization(pageLink_r5), " ");
  }
}
function Paginator_div_0_span_7_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 25);
    ɵɵtemplate(1, Paginator_div_0_span_7_button_1_Template, 2, 6, "button", 26);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngForOf", ctx_r1.pageLinks);
  }
}
function Paginator_div_0_p_select_8_ng_template_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtext(0);
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵtextInterpolate(ctx_r1.currentPageReport);
  }
}
function Paginator_div_0_p_select_8_ng_container_2_ng_template_1_ng_container_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Paginator_div_0_p_select_8_ng_container_2_ng_template_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_p_select_8_ng_container_2_ng_template_1_ng_container_0_Template, 1, 0, "ng-container", 17);
  }
  if (rf & 2) {
    const item_r7 = ctx.$implicit;
    const ctx_r1 = ɵɵnextContext(4);
    ɵɵproperty("ngTemplateOutlet", ctx_r1.jumpToPageItemTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(2, _c6, item_r7));
  }
}
function Paginator_div_0_p_select_8_ng_container_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, Paginator_div_0_p_select_8_ng_container_2_ng_template_1_Template, 1, 4, "ng-template", 31);
    ɵɵelementContainerEnd();
  }
}
function Paginator_div_0_p_select_8_3_ng_template_0_ng_container_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Paginator_div_0_p_select_8_3_ng_template_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_p_select_8_3_ng_template_0_ng_container_0_Template, 1, 0, "ng-container", 23);
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(4);
    ɵɵproperty("ngTemplateOutlet", ctx_r1.dropdownIconTemplate || ctx_r1._dropdownIconTemplate);
  }
}
function Paginator_div_0_p_select_8_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_p_select_8_3_ng_template_0_Template, 1, 1, "ng-template", 32);
  }
}
function Paginator_div_0_p_select_8_Template(rf, ctx) {
  if (rf & 1) {
    const _r6 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "p-select", 28);
    ɵɵlistener("onChange", function Paginator_div_0_p_select_8_Template_p_select_onChange_0_listener($event) {
      ɵɵrestoreView(_r6);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.onPageDropdownChange($event));
    });
    ɵɵtemplate(1, Paginator_div_0_p_select_8_ng_template_1_Template, 1, 1, "ng-template", 29)(2, Paginator_div_0_p_select_8_ng_container_2_Template, 2, 0, "ng-container", 30)(3, Paginator_div_0_p_select_8_3_Template, 1, 0, null, 30);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("options", ctx_r1.pageItems)("ngModel", ctx_r1.getPage())("disabled", ctx_r1.empty())("appendTo", ctx_r1.dropdownAppendTo)("scrollHeight", ctx_r1.dropdownScrollHeight);
    ɵɵattribute("aria-label", ctx_r1.getAriaLabel("jumpToPageDropdownLabel"));
    ɵɵadvance(2);
    ɵɵproperty("ngIf", ctx_r1.jumpToPageItemTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.dropdownIconTemplate || ctx_r1._dropdownIconTemplate);
  }
}
function Paginator_div_0_AngleRightIcon_10_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "AngleRightIcon", 21);
  }
  if (rf & 2) {
    ɵɵproperty("styleClass", "p-paginator-next-icon");
  }
}
function Paginator_div_0_span_11_1_ng_template_0_Template(rf, ctx) {
}
function Paginator_div_0_span_11_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_span_11_1_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Paginator_div_0_span_11_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 33);
    ɵɵtemplate(1, Paginator_div_0_span_11_1_Template, 1, 0, null, 23);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.nextPageLinkIconTemplate || ctx_r1._nextPageLinkIconTemplate);
  }
}
function Paginator_div_0_button_12_AngleDoubleRightIcon_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelement(0, "AngleDoubleRightIcon", 21);
  }
  if (rf & 2) {
    ɵɵproperty("styleClass", "p-paginator-last-icon");
  }
}
function Paginator_div_0_button_12_span_2_1_ng_template_0_Template(rf, ctx) {
}
function Paginator_div_0_button_12_span_2_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_button_12_span_2_1_ng_template_0_Template, 0, 0, "ng-template");
  }
}
function Paginator_div_0_button_12_span_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 36);
    ɵɵtemplate(1, Paginator_div_0_button_12_span_2_1_Template, 1, 0, null, 23);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(3);
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.lastPageLinkIconTemplate || ctx_r1._lastPageLinkIconTemplate);
  }
}
function Paginator_div_0_button_12_Template(rf, ctx) {
  if (rf & 1) {
    const _r8 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "button", 34);
    ɵɵlistener("click", function Paginator_div_0_button_12_Template_button_click_0_listener($event) {
      ɵɵrestoreView(_r8);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.changePageToLast($event));
    });
    ɵɵtemplate(1, Paginator_div_0_button_12_AngleDoubleRightIcon_1_Template, 1, 1, "AngleDoubleRightIcon", 6)(2, Paginator_div_0_button_12_span_2_Template, 2, 1, "span", 35);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("disabled", ctx_r1.isLastPage() || ctx_r1.empty())("ngClass", ɵɵpureFunction1(5, _c5, ctx_r1.isLastPage() || ctx_r1.empty()));
    ɵɵattribute("aria-label", ctx_r1.getAriaLabel("lastPageLabel"));
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.lastPageLinkIconTemplate && !ctx_r1._lastPageLinkIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.lastPageLinkIconTemplate || ctx_r1._lastPageLinkIconTemplate);
  }
}
function Paginator_div_0_p_inputnumber_13_Template(rf, ctx) {
  if (rf & 1) {
    const _r9 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "p-inputnumber", 37);
    ɵɵlistener("ngModelChange", function Paginator_div_0_p_inputnumber_13_Template_p_inputnumber_ngModelChange_0_listener($event) {
      ɵɵrestoreView(_r9);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.changePage($event - 1));
    });
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("ngModel", ctx_r1.currentPage())("disabled", ctx_r1.empty());
  }
}
function Paginator_div_0_p_select_14_ng_container_1_ng_template_1_ng_container_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Paginator_div_0_p_select_14_ng_container_1_ng_template_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_p_select_14_ng_container_1_ng_template_1_ng_container_0_Template, 1, 0, "ng-container", 17);
  }
  if (rf & 2) {
    const item_r11 = ctx.$implicit;
    const ctx_r1 = ɵɵnextContext(4);
    ɵɵproperty("ngTemplateOutlet", ctx_r1.dropdownItemTemplate)("ngTemplateOutletContext", ɵɵpureFunction1(2, _c6, item_r11));
  }
}
function Paginator_div_0_p_select_14_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtemplate(1, Paginator_div_0_p_select_14_ng_container_1_ng_template_1_Template, 1, 4, "ng-template", 31);
    ɵɵelementContainerEnd();
  }
}
function Paginator_div_0_p_select_14_2_ng_template_0_ng_container_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Paginator_div_0_p_select_14_2_ng_template_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_p_select_14_2_ng_template_0_ng_container_0_Template, 1, 0, "ng-container", 23);
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(4);
    ɵɵproperty("ngTemplateOutlet", ctx_r1.dropdownIconTemplate || ctx_r1._dropdownIconTemplate);
  }
}
function Paginator_div_0_p_select_14_2_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵtemplate(0, Paginator_div_0_p_select_14_2_ng_template_0_Template, 1, 1, "ng-template", 32);
  }
}
function Paginator_div_0_p_select_14_Template(rf, ctx) {
  if (rf & 1) {
    const _r10 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "p-select", 38);
    ɵɵtwoWayListener("ngModelChange", function Paginator_div_0_p_select_14_Template_p_select_ngModelChange_0_listener($event) {
      ɵɵrestoreView(_r10);
      const ctx_r1 = ɵɵnextContext(2);
      ɵɵtwoWayBindingSet(ctx_r1.rows, $event) || (ctx_r1.rows = $event);
      return ɵɵresetView($event);
    });
    ɵɵlistener("onChange", function Paginator_div_0_p_select_14_Template_p_select_onChange_0_listener($event) {
      ɵɵrestoreView(_r10);
      const ctx_r1 = ɵɵnextContext(2);
      return ɵɵresetView(ctx_r1.onRppChange($event));
    });
    ɵɵtemplate(1, Paginator_div_0_p_select_14_ng_container_1_Template, 2, 0, "ng-container", 30)(2, Paginator_div_0_p_select_14_2_Template, 1, 0, null, 30);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵproperty("options", ctx_r1.rowsPerPageItems);
    ɵɵtwoWayProperty("ngModel", ctx_r1.rows);
    ɵɵproperty("disabled", ctx_r1.empty())("appendTo", ctx_r1.dropdownAppendTo)("scrollHeight", ctx_r1.dropdownScrollHeight)("ariaLabel", ctx_r1.getAriaLabel("rowsPerPageLabel"));
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.dropdownItemTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.dropdownIconTemplate || ctx_r1._dropdownIconTemplate);
  }
}
function Paginator_div_0_div_15_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainer(0);
  }
}
function Paginator_div_0_div_15_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "div", 39);
    ɵɵtemplate(1, Paginator_div_0_div_15_ng_container_1_Template, 1, 0, "ng-container", 17);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext(2);
    ɵɵattribute("data-pc-section", "end");
    ɵɵadvance();
    ɵɵproperty("ngTemplateOutlet", ctx_r1.templateRight)("ngTemplateOutletContext", ɵɵpureFunction1(3, _c6, ctx_r1.paginatorState));
  }
}
function Paginator_div_0_Template(rf, ctx) {
  if (rf & 1) {
    const _r1 = ɵɵgetCurrentView();
    ɵɵelementStart(0, "div", 1);
    ɵɵtemplate(1, Paginator_div_0_div_1_Template, 2, 5, "div", 2)(2, Paginator_div_0_span_2_Template, 2, 1, "span", 3)(3, Paginator_div_0_button_3_Template, 3, 7, "button", 4);
    ɵɵelementStart(4, "button", 5);
    ɵɵlistener("click", function Paginator_div_0_Template_button_click_4_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.changePageToPrev($event));
    });
    ɵɵtemplate(5, Paginator_div_0_AngleLeftIcon_5_Template, 1, 1, "AngleLeftIcon", 6)(6, Paginator_div_0_span_6_Template, 2, 1, "span", 7);
    ɵɵelementEnd();
    ɵɵtemplate(7, Paginator_div_0_span_7_Template, 2, 1, "span", 8)(8, Paginator_div_0_p_select_8_Template, 4, 8, "p-select", 9);
    ɵɵelementStart(9, "button", 10);
    ɵɵlistener("click", function Paginator_div_0_Template_button_click_9_listener($event) {
      ɵɵrestoreView(_r1);
      const ctx_r1 = ɵɵnextContext();
      return ɵɵresetView(ctx_r1.changePageToNext($event));
    });
    ɵɵtemplate(10, Paginator_div_0_AngleRightIcon_10_Template, 1, 1, "AngleRightIcon", 6)(11, Paginator_div_0_span_11_Template, 2, 1, "span", 11);
    ɵɵelementEnd();
    ɵɵtemplate(12, Paginator_div_0_button_12_Template, 3, 7, "button", 12)(13, Paginator_div_0_p_inputnumber_13_Template, 1, 2, "p-inputnumber", 13)(14, Paginator_div_0_p_select_14_Template, 3, 8, "p-select", 14)(15, Paginator_div_0_div_15_Template, 2, 5, "div", 15);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r1 = ɵɵnextContext();
    ɵɵclassMap(ctx_r1.styleClass);
    ɵɵproperty("ngStyle", ctx_r1.style)("ngClass", "p-paginator p-component");
    ɵɵattribute("data-pc-section", "paginator")("data-pc-section", "root");
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.templateLeft);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.showCurrentPageReport);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.showFirstLastIcon);
    ɵɵadvance();
    ɵɵproperty("disabled", ctx_r1.isFirstPage() || ctx_r1.empty())("ngClass", ɵɵpureFunction1(25, _c5, ctx_r1.isFirstPage() || ctx_r1.empty()));
    ɵɵattribute("aria-label", ctx_r1.getAriaLabel("prevPageLabel"));
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.previousPageLinkIconTemplate && !ctx_r1._previousPageLinkIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.previousPageLinkIconTemplate || ctx_r1._previousPageLinkIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.showPageLinks);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.showJumpToPageDropdown);
    ɵɵadvance();
    ɵɵproperty("disabled", ctx_r1.isLastPage() || ctx_r1.empty())("ngClass", ɵɵpureFunction1(27, _c5, ctx_r1.isLastPage() || ctx_r1.empty()));
    ɵɵattribute("aria-label", ctx_r1.getAriaLabel("nextPageLabel"));
    ɵɵadvance();
    ɵɵproperty("ngIf", !ctx_r1.nextPageLinkIconTemplate && !ctx_r1._nextPageLinkIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.nextPageLinkIconTemplate || ctx_r1._nextPageLinkIconTemplate);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.showFirstLastIcon);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.showJumpToPageInput);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.rowsPerPageOptions);
    ɵɵadvance();
    ɵɵproperty("ngIf", ctx_r1.templateRight);
  }
}
var theme = ({
  dt
}) => `
.p-paginator {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    background: ${dt("paginator.background")};
    color: ${dt("paginator.color")};
    padding: ${dt("paginator.padding")};
    border-radius: ${dt("paginator.border.radius")};
    gap: ${dt("paginator.gap")};
}

.p-paginator-content {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: ${dt("paginator.gap")};
}

.p-paginator-content-start {
    margin-inline-end: auto;
}

.p-paginator-content-end {
    margin-inline-start: auto;
}

.p-paginator-page,
.p-paginator-next,
.p-paginator-last,
.p-paginator-first,
.p-paginator-prev {
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    user-select: none;
    overflow: hidden;
    position: relative;
    background: ${dt("paginator.nav.button.background")};
    border: 0 none;
    color: ${dt("paginator.nav.button.color")};
    min-width: ${dt("paginator.nav.button.width")};
    height: ${dt("paginator.nav.button.height")};
    transition: background ${dt("paginator.transition.duration")}, color ${dt("paginator.transition.duration")}, outline-color ${dt("paginator.transition.duration")}, box-shadow ${dt("paginator.transition.duration")};
    border-radius: ${dt("paginator.nav.button.border.radius")};
    padding: 0;
    margin: 0;
}

.p-paginator-page:focus-visible,
.p-paginator-next:focus-visible,
.p-paginator-last:focus-visible,
.p-paginator-first:focus-visible,
.p-paginator-prev:focus-visible {
    box-shadow: ${dt("paginator.nav.button.focus.ring.shadow")};
    outline: ${dt("paginator.nav.button.focus.ring.width")} ${dt("paginator.nav.button.focus.ring.style")} ${dt("paginator.nav.button.focus.ring.color")};
    outline-offset: ${dt("paginator.nav.button.focus.ring.offset")};
}

.p-paginator-page:not(.p-disabled):not(.p-paginator-page-selected):hover,
.p-paginator-first:not(.p-disabled):hover,
.p-paginator-prev:not(.p-disabled):hover,
.p-paginator-next:not(.p-disabled):hover,
.p-paginator-last:not(.p-disabled):hover {
    background: ${dt("paginator.nav.button.hover.background")};
    color: ${dt("paginator.nav.button.hover.color")};
}

.p-paginator-first:dir(rtl),
.p-paginator-prev:dir(rtl),
.p-paginator-next:dir(rtl),
.p-paginator-last:dir(rtl) {
    transform: rotate(180deg);
}

.p-paginator-page.p-paginator-page-selected {
    background: ${dt("paginator.nav.button.selected.background")};
    color: ${dt("paginator.nav.button.selected.color")};
}

.p-paginator-current {
    color: ${dt("paginator.current.page.report.color")};
}

.p-paginator-pages {
    display: flex;
    align-items: center;
    gap: ${dt("paginator.gap")};
}

.p-paginator-jtp-input .p-inputtext {
    max-width: ${dt("paginator.jump.to.page.input.max.width")};
}
`;
var classes = {
  paginator: ({
    instance,
    key
  }) => ["p-paginator p-component", {
    "p-paginator-default": !instance.hasBreakpoints(),
    [`p-paginator-${key}`]: instance.hasBreakpoints()
  }],
  content: "p-paginator-content",
  contentStart: "p-paginator-content-start",
  contentEnd: "p-paginator-content-end",
  first: ({
    instance
  }) => ["p-paginator-first", {
    "p-disabled": instance.$attrs.disabled
  }],
  firstIcon: "p-paginator-first-icon",
  prev: ({
    instance
  }) => ["p-paginator-prev", {
    "p-disabled": instance.$attrs.disabled
  }],
  prevIcon: "p-paginator-prev-icon",
  next: ({
    instance
  }) => ["p-paginator-next", {
    "p-disabled": instance.$attrs.disabled
  }],
  nextIcon: "p-paginator-next-icon",
  last: ({
    instance
  }) => ["p-paginator-last", {
    "p-disabled": instance.$attrs.disabled
  }],
  lastIcon: "p-paginator-last-icon",
  pages: "p-paginator-pages",
  page: ({
    props,
    pageLink
  }) => ["p-paginator-page", {
    "p-paginator-page-selected": pageLink - 1 === props.page
  }],
  current: "p-paginator-current",
  pcRowPerPageDropdown: "p-paginator-rpp-dropdown",
  pcJumpToPageDropdown: "p-paginator-jtp-dropdown",
  pcJumpToPageInput: "p-paginator-jtp-input"
};
var PaginatorStyle = class _PaginatorStyle extends BaseStyle {
  name = "paginator";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵPaginatorStyle_BaseFactory;
    return function PaginatorStyle_Factory(__ngFactoryType__) {
      return (ɵPaginatorStyle_BaseFactory || (ɵPaginatorStyle_BaseFactory = ɵɵgetInheritedFactory(_PaginatorStyle)))(__ngFactoryType__ || _PaginatorStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _PaginatorStyle,
    factory: _PaginatorStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(PaginatorStyle, [{
    type: Injectable
  }], null, null);
})();
var PaginatorClasses;
(function(PaginatorClasses2) {
  PaginatorClasses2["paginator"] = "p-paginator";
  PaginatorClasses2["contentStart"] = "p-paginator-content-start";
  PaginatorClasses2["contentEnd"] = "p-paginator-content-end";
  PaginatorClasses2["first"] = "p-paginator-first";
  PaginatorClasses2["firstIcon"] = "p-paginator-first-icon";
  PaginatorClasses2["prev"] = "p-paginator-prev";
  PaginatorClasses2["prevIcon"] = "p-paginator-prev-icon";
  PaginatorClasses2["next"] = "p-paginator-next";
  PaginatorClasses2["nextIcon"] = "p-paginator-next-icon";
  PaginatorClasses2["last"] = "p-paginator-last";
  PaginatorClasses2["lastIcon"] = "p-paginator-last-icon";
  PaginatorClasses2["pages"] = "p-paginator-pages";
  PaginatorClasses2["page"] = "p-paginator-page";
  PaginatorClasses2["current"] = "p-paginator-current";
  PaginatorClasses2["pcRowPerPageDropdown"] = "p-paginator-rpp-dropdown";
  PaginatorClasses2["pcJumpToPageDropdown"] = "p-paginator-jtp-dropdown";
  PaginatorClasses2["pcJumpToPageInput"] = "p-paginator-jtp-input";
})(PaginatorClasses || (PaginatorClasses = {}));
var Paginator = class _Paginator extends BaseComponent {
  /**
   * Number of page links to display.
   * @group Props
   */
  pageLinkSize = 5;
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
   * Whether to show it even there is only one page.
   * @group Props
   */
  alwaysShow = true;
  /**
   * Target element to attach the dropdown overlay, valid values are "body" or a local ng-template variable of another element (note: use binding with brackets for template variables, e.g. [appendTo]="mydiv" for a div element having #mydiv as variable name).
   * @group Props
   */
  dropdownAppendTo;
  /**
   * Template instance to inject into the left side of the paginator.
   * @param {PaginatorState} context - Paginator state.
   * @group Props
   */
  templateLeft;
  /**
   * Template instance to inject into the right side of the paginator.
   * @param {PaginatorState} context - Paginator state.
   * @group Props
   */
  templateRight;
  /**
   * Target element to attach the dropdown overlay, valid values are "body" or a local ng-template variable of another element (note: use binding with brackets for template variables, e.g. [appendTo]="mydiv" for a div element having #mydiv as variable name).
   * @group Props
   */
  appendTo;
  /**
   * Dropdown height of the viewport in pixels, a scrollbar is defined if height of list exceeds this value.
   * @group Props
   */
  dropdownScrollHeight = "200px";
  /**
   * Template of the current page report element. Available placeholders are {currentPage},{totalPages},{rows},{first},{last} and {totalRecords}
   * @group Props
   */
  currentPageReportTemplate = "{currentPage} of {totalPages}";
  /**
   * Whether to display current page report.
   * @group Props
   */
  showCurrentPageReport;
  /**
   * When enabled, icons are displayed on paginator to go first and last page.
   * @group Props
   */
  showFirstLastIcon = true;
  /**
   * Number of total records.
   * @group Props
   */
  totalRecords = 0;
  /**
   * Data count to display per page.
   * @group Props
   */
  rows = 0;
  /**
   * Array of integer/object values to display inside rows per page dropdown. A object that have 'showAll' key can be added to it to show all data. Exp; [10,20,30,{showAll:'All'}]
   * @group Props
   */
  rowsPerPageOptions;
  /**
   * Whether to display a dropdown to navigate to any page.
   * @group Props
   */
  showJumpToPageDropdown;
  /**
   * Whether to display a input to navigate to any page.
   * @group Props
   */
  showJumpToPageInput;
  /**
   * Template instance to inject into the jump to page dropdown item inside in the paginator.
   * @param {Object} context - item instance.
   * @group Props
   */
  jumpToPageItemTemplate;
  /**
   * Whether to show page links.
   * @group Props
   */
  showPageLinks = true;
  /**
   * Locale to be used in formatting.
   * @group Props
   */
  locale;
  /**
   * Template instance to inject into the rows per page dropdown item inside in the paginator.
   * @param {Object} context - item instance.
   * @group Props
   */
  dropdownItemTemplate;
  /**
   * Zero-relative number of the first row to be displayed.
   * @group Props
   */
  get first() {
    return this._first;
  }
  set first(val) {
    this._first = val;
  }
  /**
   * Callback to invoke when page changes, the event object contains information about the new state.
   * @param {PaginatorState} event - Paginator state.
   * @group Emits
   */
  onPageChange = new EventEmitter();
  /**
   * Template for the dropdown icon.
   * @group Templates
   */
  dropdownIconTemplate;
  /**
   * Template for the first page link icon.
   * @group Templates
   */
  firstPageLinkIconTemplate;
  /**
   * Template for the previous page link icon.
   * @group Templates
   */
  previousPageLinkIconTemplate;
  /**
   * Template for the last page link icon.
   * @group Templates
   */
  lastPageLinkIconTemplate;
  /**
   * Template for the next page link icon.
   * @group Templates
   */
  nextPageLinkIconTemplate;
  templates;
  _dropdownIconTemplate;
  _firstPageLinkIconTemplate;
  _previousPageLinkIconTemplate;
  _lastPageLinkIconTemplate;
  _nextPageLinkIconTemplate;
  pageLinks;
  pageItems;
  rowsPerPageItems;
  paginatorState;
  _first = 0;
  _page = 0;
  _componentStyle = inject(PaginatorStyle);
  constructor() {
    super();
  }
  ngOnInit() {
    super.ngOnInit();
    this.updatePaginatorState();
  }
  ngAfterContentInit() {
    this.templates.forEach((item) => {
      switch (item.getType()) {
        case "dropdownicon":
          this._dropdownIconTemplate = item.template;
          break;
        case "firstpagelinkicon":
          this._firstPageLinkIconTemplate = item.template;
          break;
        case "previouspagelinkicon":
          this._previousPageLinkIconTemplate = item.template;
          break;
        case "lastpagelinkicon":
          this._lastPageLinkIconTemplate = item.template;
          break;
        case "nextpagelinkicon":
          this._nextPageLinkIconTemplate = item.template;
          break;
      }
    });
  }
  getAriaLabel(labelType) {
    return this.config.translation.aria ? this.config.translation.aria[labelType] : void 0;
  }
  getPageAriaLabel(value) {
    return this.config.translation.aria ? this.config.translation.aria.pageLabel.replace(/{page}/g, `${value}`) : void 0;
  }
  getLocalization(digit) {
    const numerals = [...new Intl.NumberFormat(this.locale, {
      useGrouping: false
    }).format(9876543210)].reverse();
    const index = new Map(numerals.map((d, i) => [i, d]));
    if (digit > 9) {
      const numbers = String(digit).split("");
      return numbers.map((number) => index.get(Number(number))).join("");
    } else {
      return index.get(digit);
    }
  }
  ngOnChanges(simpleChange) {
    super.ngOnChanges(simpleChange);
    if (simpleChange.totalRecords) {
      this.updatePageLinks();
      this.updatePaginatorState();
      this.updateFirst();
      this.updateRowsPerPageOptions();
    }
    if (simpleChange.first) {
      this._first = simpleChange.first.currentValue;
      this.updatePageLinks();
      this.updatePaginatorState();
    }
    if (simpleChange.rows) {
      this.updatePageLinks();
      this.updatePaginatorState();
    }
    if (simpleChange.rowsPerPageOptions) {
      this.updateRowsPerPageOptions();
    }
    if (simpleChange.pageLinkSize) {
      this.updatePageLinks();
    }
  }
  updateRowsPerPageOptions() {
    if (this.rowsPerPageOptions) {
      this.rowsPerPageItems = [];
      let showAllItem = null;
      for (let opt of this.rowsPerPageOptions) {
        if (typeof opt == "object" && opt["showAll"]) {
          showAllItem = {
            label: opt["showAll"],
            value: this.totalRecords
          };
        } else {
          this.rowsPerPageItems.push({
            label: String(this.getLocalization(opt)),
            value: opt
          });
        }
      }
      if (showAllItem) {
        this.rowsPerPageItems.push(showAllItem);
      }
    }
  }
  isFirstPage() {
    return this.getPage() === 0;
  }
  isLastPage() {
    return this.getPage() === this.getPageCount() - 1;
  }
  getPageCount() {
    return Math.ceil(this.totalRecords / this.rows);
  }
  calculatePageLinkBoundaries() {
    let numberOfPages = this.getPageCount(), visiblePages = Math.min(this.pageLinkSize, numberOfPages);
    let start = Math.max(0, Math.ceil(this.getPage() - visiblePages / 2)), end = Math.min(numberOfPages - 1, start + visiblePages - 1);
    var delta = this.pageLinkSize - (end - start + 1);
    start = Math.max(0, start - delta);
    return [start, end];
  }
  updatePageLinks() {
    this.pageLinks = [];
    let boundaries = this.calculatePageLinkBoundaries(), start = boundaries[0], end = boundaries[1];
    for (let i = start; i <= end; i++) {
      this.pageLinks.push(i + 1);
    }
    if (this.showJumpToPageDropdown) {
      this.pageItems = [];
      for (let i = 0; i < this.getPageCount(); i++) {
        this.pageItems.push({
          label: String(i + 1),
          value: i
        });
      }
    }
  }
  changePage(p) {
    var pc = this.getPageCount();
    if (p >= 0 && p < pc) {
      this._first = this.rows * p;
      var state = {
        page: p,
        first: this.first,
        rows: this.rows,
        pageCount: pc
      };
      this.updatePageLinks();
      this.onPageChange.emit(state);
      this.updatePaginatorState();
    }
  }
  updateFirst() {
    const page = this.getPage();
    if (page > 0 && this.totalRecords && this.first >= this.totalRecords) {
      Promise.resolve(null).then(() => this.changePage(page - 1));
    }
  }
  getPage() {
    return Math.floor(this.first / this.rows);
  }
  changePageToFirst(event) {
    if (!this.isFirstPage()) {
      this.changePage(0);
    }
    event.preventDefault();
  }
  changePageToPrev(event) {
    this.changePage(this.getPage() - 1);
    event.preventDefault();
  }
  changePageToNext(event) {
    this.changePage(this.getPage() + 1);
    event.preventDefault();
  }
  changePageToLast(event) {
    if (!this.isLastPage()) {
      this.changePage(this.getPageCount() - 1);
    }
    event.preventDefault();
  }
  onPageLinkClick(event, page) {
    this.changePage(page);
    event.preventDefault();
  }
  onRppChange(event) {
    this.changePage(this.getPage());
  }
  onPageDropdownChange(event) {
    this.changePage(event.value);
  }
  updatePaginatorState() {
    this.paginatorState = {
      page: this.getPage(),
      pageCount: this.getPageCount(),
      rows: this.rows,
      first: this.first,
      totalRecords: this.totalRecords
    };
  }
  empty() {
    return this.getPageCount() === 0;
  }
  currentPage() {
    return this.getPageCount() > 0 ? this.getPage() + 1 : 0;
  }
  get currentPageReport() {
    return this.currentPageReportTemplate.replace("{currentPage}", String(this.currentPage())).replace("{totalPages}", String(this.getPageCount())).replace("{first}", String(this.totalRecords > 0 ? this._first + 1 : 0)).replace("{last}", String(Math.min(this._first + this.rows, this.totalRecords))).replace("{rows}", String(this.rows)).replace("{totalRecords}", String(this.totalRecords));
  }
  static ɵfac = function Paginator_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _Paginator)();
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _Paginator,
    selectors: [["p-paginator"]],
    contentQueries: function Paginator_ContentQueries(rf, ctx, dirIndex) {
      if (rf & 1) {
        ɵɵcontentQuery(dirIndex, _c0, 4);
        ɵɵcontentQuery(dirIndex, _c1, 4);
        ɵɵcontentQuery(dirIndex, _c2, 4);
        ɵɵcontentQuery(dirIndex, _c3, 4);
        ɵɵcontentQuery(dirIndex, _c4, 4);
        ɵɵcontentQuery(dirIndex, PrimeTemplate, 4);
      }
      if (rf & 2) {
        let _t;
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.dropdownIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.firstPageLinkIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.previousPageLinkIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.lastPageLinkIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.nextPageLinkIconTemplate = _t.first);
        ɵɵqueryRefresh(_t = ɵɵloadQuery()) && (ctx.templates = _t);
      }
    },
    inputs: {
      pageLinkSize: [2, "pageLinkSize", "pageLinkSize", numberAttribute],
      style: "style",
      styleClass: "styleClass",
      alwaysShow: [2, "alwaysShow", "alwaysShow", booleanAttribute],
      dropdownAppendTo: "dropdownAppendTo",
      templateLeft: "templateLeft",
      templateRight: "templateRight",
      appendTo: "appendTo",
      dropdownScrollHeight: "dropdownScrollHeight",
      currentPageReportTemplate: "currentPageReportTemplate",
      showCurrentPageReport: [2, "showCurrentPageReport", "showCurrentPageReport", booleanAttribute],
      showFirstLastIcon: [2, "showFirstLastIcon", "showFirstLastIcon", booleanAttribute],
      totalRecords: [2, "totalRecords", "totalRecords", numberAttribute],
      rows: [2, "rows", "rows", numberAttribute],
      rowsPerPageOptions: "rowsPerPageOptions",
      showJumpToPageDropdown: [2, "showJumpToPageDropdown", "showJumpToPageDropdown", booleanAttribute],
      showJumpToPageInput: [2, "showJumpToPageInput", "showJumpToPageInput", booleanAttribute],
      jumpToPageItemTemplate: "jumpToPageItemTemplate",
      showPageLinks: [2, "showPageLinks", "showPageLinks", booleanAttribute],
      locale: "locale",
      dropdownItemTemplate: "dropdownItemTemplate",
      first: "first"
    },
    outputs: {
      onPageChange: "onPageChange"
    },
    features: [ɵɵProvidersFeature([PaginatorStyle]), ɵɵInputTransformsFeature, ɵɵInheritDefinitionFeature, ɵɵNgOnChangesFeature],
    decls: 1,
    vars: 1,
    consts: [[3, "class", "ngStyle", "ngClass", 4, "ngIf"], [3, "ngStyle", "ngClass"], ["class", "p-paginator-content-start", 4, "ngIf"], ["class", "p-paginator-current", 4, "ngIf"], ["type", "button", "pRipple", "", "class", "p-paginator-first", 3, "disabled", "ngClass", "click", 4, "ngIf"], ["type", "button", "pRipple", "", 1, "p-paginator-prev", 3, "click", "disabled", "ngClass"], [3, "styleClass", 4, "ngIf"], ["class", "p-paginator-prev-icon", 4, "ngIf"], ["class", "p-paginator-pages", 4, "ngIf"], ["styleClass", "p-paginator-jtp-dropdown", 3, "options", "ngModel", "disabled", "appendTo", "scrollHeight", "onChange", 4, "ngIf"], ["type", "button", "pRipple", "", 1, "p-paginator-next", 3, "click", "disabled", "ngClass"], ["class", "p-paginator-next-icon", 4, "ngIf"], ["type", "button", "pRipple", "", "class", "p-paginator-last", 3, "disabled", "ngClass", "click", 4, "ngIf"], ["class", "p-paginator-jtp-input", 3, "ngModel", "disabled", "ngModelChange", 4, "ngIf"], ["styleClass", "p-paginator-rpp-dropdown", 3, "options", "ngModel", "disabled", "appendTo", "scrollHeight", "ariaLabel", "ngModelChange", "onChange", 4, "ngIf"], ["class", "p-paginator-content-end", 4, "ngIf"], [1, "p-paginator-content-start"], [4, "ngTemplateOutlet", "ngTemplateOutletContext"], [1, "p-paginator-current"], ["type", "button", "pRipple", "", 1, "p-paginator-first", 3, "click", "disabled", "ngClass"], ["class", "p-paginator-first-icon", 4, "ngIf"], [3, "styleClass"], [1, "p-paginator-first-icon"], [4, "ngTemplateOutlet"], [1, "p-paginator-prev-icon"], [1, "p-paginator-pages"], ["type", "button", "class", "p-paginator-page", "pRipple", "", 3, "ngClass", "click", 4, "ngFor", "ngForOf"], ["type", "button", "pRipple", "", 1, "p-paginator-page", 3, "click", "ngClass"], ["styleClass", "p-paginator-jtp-dropdown", 3, "onChange", "options", "ngModel", "disabled", "appendTo", "scrollHeight"], ["pTemplate", "selectedItem"], [4, "ngIf"], ["pTemplate", "item"], ["pTemplate", "dropdownicon"], [1, "p-paginator-next-icon"], ["type", "button", "pRipple", "", 1, "p-paginator-last", 3, "click", "disabled", "ngClass"], ["class", "p-paginator-last-icon", 4, "ngIf"], [1, "p-paginator-last-icon"], [1, "p-paginator-jtp-input", 3, "ngModelChange", "ngModel", "disabled"], ["styleClass", "p-paginator-rpp-dropdown", 3, "ngModelChange", "onChange", "options", "ngModel", "disabled", "appendTo", "scrollHeight", "ariaLabel"], [1, "p-paginator-content-end"]],
    template: function Paginator_Template(rf, ctx) {
      if (rf & 1) {
        ɵɵtemplate(0, Paginator_div_0_Template, 16, 29, "div", 0);
      }
      if (rf & 2) {
        ɵɵproperty("ngIf", ctx.alwaysShow ? true : ctx.pageLinks && ctx.pageLinks.length > 1);
      }
    },
    dependencies: [CommonModule, NgClass, NgForOf, NgIf, NgTemplateOutlet, NgStyle, Select, InputNumber, FormsModule, NgControlStatus, NgModel, Ripple, AngleDoubleLeftIcon, AngleDoubleRightIcon, AngleLeftIcon, AngleRightIcon, SharedModule, PrimeTemplate],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(Paginator, [{
    type: Component,
    args: [{
      selector: "p-paginator",
      standalone: true,
      imports: [CommonModule, Select, InputNumber, FormsModule, Ripple, AngleDoubleLeftIcon, AngleDoubleRightIcon, AngleLeftIcon, AngleRightIcon, SharedModule],
      template: `
        <div [class]="styleClass" [ngStyle]="style" [ngClass]="'p-paginator p-component'" *ngIf="alwaysShow ? true : pageLinks && pageLinks.length > 1" [attr.data-pc-section]="'paginator'" [attr.data-pc-section]="'root'">
            <div class="p-paginator-content-start" *ngIf="templateLeft" [attr.data-pc-section]="'start'">
                <ng-container *ngTemplateOutlet="templateLeft; context: { $implicit: paginatorState }"></ng-container>
            </div>
            <span class="p-paginator-current" *ngIf="showCurrentPageReport">{{ currentPageReport }}</span>
            <button
                *ngIf="showFirstLastIcon"
                type="button"
                [disabled]="isFirstPage() || empty()"
                (click)="changePageToFirst($event)"
                pRipple
                class="p-paginator-first"
                [ngClass]="{ 'p-disabled': isFirstPage() || empty() }"
                [attr.aria-label]="getAriaLabel('firstPageLabel')"
            >
                <AngleDoubleLeftIcon *ngIf="!firstPageLinkIconTemplate && !_firstPageLinkIconTemplate" [styleClass]="'p-paginator-first-icon'" />
                <span class="p-paginator-first-icon" *ngIf="firstPageLinkIconTemplate || _firstPageLinkIconTemplate">
                    <ng-template *ngTemplateOutlet="firstPageLinkIconTemplate || _firstPageLinkIconTemplate"></ng-template>
                </span>
            </button>
            <button type="button" [disabled]="isFirstPage() || empty()" (click)="changePageToPrev($event)" pRipple class="p-paginator-prev" [ngClass]="{ 'p-disabled': isFirstPage() || empty() }" [attr.aria-label]="getAriaLabel('prevPageLabel')">
                <AngleLeftIcon *ngIf="!previousPageLinkIconTemplate && !_previousPageLinkIconTemplate" [styleClass]="'p-paginator-prev-icon'" />
                <span class="p-paginator-prev-icon" *ngIf="previousPageLinkIconTemplate || _previousPageLinkIconTemplate">
                    <ng-template *ngTemplateOutlet="previousPageLinkIconTemplate || _previousPageLinkIconTemplate"></ng-template>
                </span>
            </button>
            <span class="p-paginator-pages" *ngIf="showPageLinks">
                <button
                    type="button"
                    *ngFor="let pageLink of pageLinks"
                    class="p-paginator-page"
                    [ngClass]="{ 'p-paginator-page-selected': pageLink - 1 == getPage() }"
                    [attr.aria-label]="getPageAriaLabel(pageLink)"
                    [attr.aria-current]="pageLink - 1 == getPage() ? 'page' : undefined"
                    (click)="onPageLinkClick($event, pageLink - 1)"
                    pRipple
                >
                    {{ getLocalization(pageLink) }}
                </button>
            </span>
            <p-select
                [options]="pageItems"
                [ngModel]="getPage()"
                *ngIf="showJumpToPageDropdown"
                [disabled]="empty()"
                [attr.aria-label]="getAriaLabel('jumpToPageDropdownLabel')"
                styleClass="p-paginator-jtp-dropdown"
                (onChange)="onPageDropdownChange($event)"
                [appendTo]="dropdownAppendTo"
                [scrollHeight]="dropdownScrollHeight"
            >
                <ng-template pTemplate="selectedItem">{{ currentPageReport }}</ng-template>
                <ng-container *ngIf="jumpToPageItemTemplate">
                    <ng-template let-item pTemplate="item">
                        <ng-container *ngTemplateOutlet="jumpToPageItemTemplate; context: { $implicit: item }"></ng-container>
                    </ng-template>
                </ng-container>
                <ng-template pTemplate="dropdownicon" *ngIf="dropdownIconTemplate || _dropdownIconTemplate">
                    <ng-container *ngTemplateOutlet="dropdownIconTemplate || _dropdownIconTemplate"></ng-container>
                </ng-template>
            </p-select>
            <button type="button" [disabled]="isLastPage() || empty()" (click)="changePageToNext($event)" pRipple class="p-paginator-next" [ngClass]="{ 'p-disabled': isLastPage() || empty() }" [attr.aria-label]="getAriaLabel('nextPageLabel')">
                <AngleRightIcon *ngIf="!nextPageLinkIconTemplate && !_nextPageLinkIconTemplate" [styleClass]="'p-paginator-next-icon'" />
                <span class="p-paginator-next-icon" *ngIf="nextPageLinkIconTemplate || _nextPageLinkIconTemplate">
                    <ng-template *ngTemplateOutlet="nextPageLinkIconTemplate || _nextPageLinkIconTemplate"></ng-template>
                </span>
            </button>
            <button
                *ngIf="showFirstLastIcon"
                type="button"
                [disabled]="isLastPage() || empty()"
                (click)="changePageToLast($event)"
                pRipple
                class="p-paginator-last"
                [ngClass]="{ 'p-disabled': isLastPage() || empty() }"
                [attr.aria-label]="getAriaLabel('lastPageLabel')"
            >
                <AngleDoubleRightIcon *ngIf="!lastPageLinkIconTemplate && !_lastPageLinkIconTemplate" [styleClass]="'p-paginator-last-icon'" />
                <span class="p-paginator-last-icon" *ngIf="lastPageLinkIconTemplate || _lastPageLinkIconTemplate">
                    <ng-template *ngTemplateOutlet="lastPageLinkIconTemplate || _lastPageLinkIconTemplate"></ng-template>
                </span>
            </button>
            <p-inputnumber *ngIf="showJumpToPageInput" [ngModel]="currentPage()" class="p-paginator-jtp-input" [disabled]="empty()" (ngModelChange)="changePage($event - 1)"></p-inputnumber>
            <p-select
                [options]="rowsPerPageItems"
                [(ngModel)]="rows"
                *ngIf="rowsPerPageOptions"
                styleClass="p-paginator-rpp-dropdown"
                [disabled]="empty()"
                (onChange)="onRppChange($event)"
                [appendTo]="dropdownAppendTo"
                [scrollHeight]="dropdownScrollHeight"
                [ariaLabel]="getAriaLabel('rowsPerPageLabel')"
            >
                <ng-container *ngIf="dropdownItemTemplate">
                    <ng-template let-item pTemplate="item">
                        <ng-container *ngTemplateOutlet="dropdownItemTemplate; context: { $implicit: item }"></ng-container>
                    </ng-template>
                </ng-container>
                <ng-template pTemplate="dropdownicon" *ngIf="dropdownIconTemplate || _dropdownIconTemplate">
                    <ng-container *ngTemplateOutlet="dropdownIconTemplate || _dropdownIconTemplate"></ng-container>
                </ng-template>
            </p-select>
            <div class="p-paginator-content-end" *ngIf="templateRight" [attr.data-pc-section]="'end'">
                <ng-container *ngTemplateOutlet="templateRight; context: { $implicit: paginatorState }"></ng-container>
            </div>
        </div>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [PaginatorStyle]
    }]
  }], () => [], {
    pageLinkSize: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    style: [{
      type: Input
    }],
    styleClass: [{
      type: Input
    }],
    alwaysShow: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    dropdownAppendTo: [{
      type: Input
    }],
    templateLeft: [{
      type: Input
    }],
    templateRight: [{
      type: Input
    }],
    appendTo: [{
      type: Input
    }],
    dropdownScrollHeight: [{
      type: Input
    }],
    currentPageReportTemplate: [{
      type: Input
    }],
    showCurrentPageReport: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    showFirstLastIcon: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    totalRecords: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    rows: [{
      type: Input,
      args: [{
        transform: numberAttribute
      }]
    }],
    rowsPerPageOptions: [{
      type: Input
    }],
    showJumpToPageDropdown: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    showJumpToPageInput: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    jumpToPageItemTemplate: [{
      type: Input
    }],
    showPageLinks: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    locale: [{
      type: Input
    }],
    dropdownItemTemplate: [{
      type: Input
    }],
    first: [{
      type: Input
    }],
    onPageChange: [{
      type: Output
    }],
    dropdownIconTemplate: [{
      type: ContentChild,
      args: ["dropdownicon", {
        descendants: false
      }]
    }],
    firstPageLinkIconTemplate: [{
      type: ContentChild,
      args: ["firstpagelinkicon", {
        descendants: false
      }]
    }],
    previousPageLinkIconTemplate: [{
      type: ContentChild,
      args: ["previouspagelinkicon", {
        descendants: false
      }]
    }],
    lastPageLinkIconTemplate: [{
      type: ContentChild,
      args: ["lastpagelinkicon", {
        descendants: false
      }]
    }],
    nextPageLinkIconTemplate: [{
      type: ContentChild,
      args: ["nextpagelinkicon", {
        descendants: false
      }]
    }],
    templates: [{
      type: ContentChildren,
      args: [PrimeTemplate]
    }]
  });
})();
var PaginatorModule = class _PaginatorModule {
  static ɵfac = function PaginatorModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _PaginatorModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _PaginatorModule,
    imports: [Paginator, SharedModule],
    exports: [Paginator, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [Paginator, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(PaginatorModule, [{
    type: NgModule,
    args: [{
      imports: [Paginator, SharedModule],
      exports: [Paginator, SharedModule]
    }]
  }], null, null);
})();

export {
  PaginatorStyle,
  PaginatorClasses,
  Paginator,
  PaginatorModule
};
//# sourceMappingURL=chunk-T47ENXVA.js.map
