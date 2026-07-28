import {
  BaseComponent
} from "./chunk-NKPC7AFX.js";
import "./chunk-3RXOYKMS.js";
import {
  BaseStyle
} from "./chunk-2GIPMIFT.js";
import {
  SharedModule
} from "./chunk-NG5OZP5M.js";
import {
  find
} from "./chunk-LGT36IR7.js";
import "./chunk-US7LRVFB.js";
import "./chunk-PXYLXCRT.js";
import {
  DefaultValueAccessor,
  FormsModule,
  NgControlStatus,
  NgModel
} from "./chunk-Q4QQ2CLE.js";
import {
  CommonModule,
  NgClass,
  NgForOf,
  NgIf,
  NgStyle
} from "./chunk-YQX3S2V2.js";
import {
  ChangeDetectionStrategy,
  Component,
  Injectable,
  Input,
  NgModule,
  ViewEncapsulation,
  inject,
  setClassMetadata,
  ɵɵInheritDefinitionFeature,
  ɵɵProvidersFeature,
  ɵɵadvance,
  ɵɵattribute,
  ɵɵclassMap,
  ɵɵdefineComponent,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵdirectiveInject,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵgetCurrentView,
  ɵɵgetInheritedFactory,
  ɵɵlistener,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵreference,
  ɵɵresetView,
  ɵɵrestoreView,
  ɵɵtemplate,
  ɵɵtext,
  ɵɵtextInterpolate,
  ɵɵtwoWayBindingSet,
  ɵɵtwoWayListener,
  ɵɵtwoWayProperty
} from "./chunk-ECNLMTOX.js";
import "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import {
  Subject
} from "./chunk-FHTVLBLO.js";
import "./chunk-N6ESDQJH.js";

// node_modules/primeng/fesm2022/primeng-terminal.mjs
function Terminal_div_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "div", 8);
    ɵɵtext(1);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r2 = ɵɵnextContext();
    ɵɵadvance();
    ɵɵtextInterpolate(ctx_r2.welcomeMessage);
  }
}
function Terminal_div_3_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "div", 9)(1, "span", 6);
    ɵɵtext(2);
    ɵɵelementEnd();
    ɵɵelementStart(3, "span", 10);
    ɵɵtext(4);
    ɵɵelementEnd();
    ɵɵelementStart(5, "div", 11);
    ɵɵtext(6);
    ɵɵelementEnd()();
  }
  if (rf & 2) {
    const command_r4 = ctx.$implicit;
    const ctx_r2 = ɵɵnextContext();
    ɵɵadvance(2);
    ɵɵtextInterpolate(ctx_r2.prompt);
    ɵɵadvance(2);
    ɵɵtextInterpolate(command_r4.text);
    ɵɵadvance();
    ɵɵattribute("aria-live", "polite");
    ɵɵadvance();
    ɵɵtextInterpolate(command_r4.response);
  }
}
var theme = ({
  dt
}) => `
.p-terminal {
    height: ${dt("terminal.height")};
    overflow: auto;
    background: ${dt("terminal.background")};
    color: ${dt("terminal.color")};
    border: 1px solid ${dt("terminal.border.color")};
    padding: ${dt("terminal.padding")};
    border-radius: ${dt("terminal.border.radius")};
}

.p-terminal-prompt {
    display: flex;
    align-items: center;
}

.p-terminal-prompt-value {
    flex: 1 1 auto;
    border: 0 none;
    background: transparent;
    color: inherit;
    padding: 0;
    outline: 0 none;
    font-family: inherit;
    font-feature-settings: inherit;
    font-size: 1rem;
}

.p-terminal-prompt-label {
    margin-inline-end: ${dt("terminal.prompt.gap")};
}

.p-terminal-input::-ms-clear {
    display: none;
}

.p-terminal-command-response {
    margin: ${dt("terminal.command.response.margin")};
}
`;
var classes = {
  root: "p-terminal p-component",
  welcomeMessage: "p-terminal-welcome-message",
  commandList: "p-terminal-command-list",
  command: "p-terminal-command",
  commandValue: "p-terminal-command-value",
  commandResponse: "p-terminal-command-response",
  prompt: "p-terminal-prompt",
  promptLabel: "p-terminal-prompt-label",
  promptValue: "p-terminal-prompt-value"
};
var TerminalStyle = class _TerminalStyle extends BaseStyle {
  name = "terminal";
  theme = theme;
  classes = classes;
  static ɵfac = /* @__PURE__ */ (() => {
    let ɵTerminalStyle_BaseFactory;
    return function TerminalStyle_Factory(__ngFactoryType__) {
      return (ɵTerminalStyle_BaseFactory || (ɵTerminalStyle_BaseFactory = ɵɵgetInheritedFactory(_TerminalStyle)))(__ngFactoryType__ || _TerminalStyle);
    };
  })();
  static ɵprov = ɵɵdefineInjectable({
    token: _TerminalStyle,
    factory: _TerminalStyle.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TerminalStyle, [{
    type: Injectable
  }], null, null);
})();
var TerminalClasses;
(function(TerminalClasses2) {
  TerminalClasses2["root"] = "p-terminal";
  TerminalClasses2["welcomeMessage"] = "p-terminal-welcome-message";
  TerminalClasses2["commandList"] = "p-terminal-command-list";
  TerminalClasses2["command"] = "p-terminal-command";
  TerminalClasses2["commandValue"] = "p-terminal-command-value";
  TerminalClasses2["commandResponse"] = "p-terminal-command-response";
  TerminalClasses2["prompt"] = "p-terminal-prompt";
  TerminalClasses2["promptLabel"] = "p-terminal-prompt-label";
  TerminalClasses2["promptValue"] = "p-terminal-prompt-value";
})(TerminalClasses || (TerminalClasses = {}));
var TerminalService = class _TerminalService {
  commandSource = new Subject();
  responseSource = new Subject();
  commandHandler = this.commandSource.asObservable();
  responseHandler = this.responseSource.asObservable();
  sendCommand(command) {
    if (command) {
      this.commandSource.next(command);
    }
  }
  sendResponse(response) {
    if (response) {
      this.responseSource.next(response);
    }
  }
  static ɵfac = function TerminalService_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _TerminalService)();
  };
  static ɵprov = ɵɵdefineInjectable({
    token: _TerminalService,
    factory: _TerminalService.ɵfac
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TerminalService, [{
    type: Injectable
  }], null, null);
})();
var Terminal = class _Terminal extends BaseComponent {
  terminalService;
  /**
   * Initial text to display on terminal.
   * @group Props
   */
  welcomeMessage;
  /**
   * Prompt text for each command.
   * @group Props
   */
  prompt;
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
  commands = [];
  command;
  container;
  commandProcessed;
  subscription;
  _componentStyle = inject(TerminalStyle);
  constructor(terminalService) {
    super();
    this.terminalService = terminalService;
    this.subscription = terminalService.responseHandler.subscribe((response) => {
      this.commands[this.commands.length - 1].response = response;
      this.commandProcessed = true;
    });
  }
  ngAfterViewInit() {
    super.ngAfterViewInit();
    this.container = find(this.el.nativeElement, ".p-terminal")[0];
  }
  ngAfterViewChecked() {
    if (this.commandProcessed) {
      this.container.scrollTop = this.container.scrollHeight;
      this.commandProcessed = false;
    }
  }
  set response(value) {
    if (value) {
      this.commands[this.commands.length - 1].response = value;
      this.commandProcessed = true;
    }
  }
  handleCommand(event) {
    if (event.keyCode == 13) {
      this.commands.push({
        text: this.command
      });
      this.terminalService.sendCommand(this.command);
      this.command = "";
    }
  }
  focus(element) {
    element.focus();
  }
  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    super.ngOnDestroy();
  }
  static ɵfac = function Terminal_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _Terminal)(ɵɵdirectiveInject(TerminalService));
  };
  static ɵcmp = ɵɵdefineComponent({
    type: _Terminal,
    selectors: [["p-terminal"]],
    inputs: {
      welcomeMessage: "welcomeMessage",
      prompt: "prompt",
      style: "style",
      styleClass: "styleClass",
      response: "response"
    },
    features: [ɵɵProvidersFeature([TerminalStyle]), ɵɵInheritDefinitionFeature],
    decls: 9,
    vars: 8,
    consts: [["in", ""], [3, "click", "ngClass", "ngStyle"], ["class", "p-terminal-welcome-message", 4, "ngIf"], [1, "p-terminal-command-list"], ["class", "p-terminal-command", 4, "ngFor", "ngForOf"], [1, "p-terminal-prompt"], [1, "p-terminal-prompt-label"], ["type", "text", "autocomplete", "off", "autofocus", "", 1, "p-terminal-prompt-value", 3, "ngModelChange", "keydown", "ngModel"], [1, "p-terminal-welcome-message"], [1, "p-terminal-command"], [1, "p-terminal-command-value"], [1, "p-terminal-command-response"]],
    template: function Terminal_Template(rf, ctx) {
      if (rf & 1) {
        const _r1 = ɵɵgetCurrentView();
        ɵɵelementStart(0, "div", 1);
        ɵɵlistener("click", function Terminal_Template_div_click_0_listener() {
          ɵɵrestoreView(_r1);
          const in_r2 = ɵɵreference(8);
          return ɵɵresetView(ctx.focus(in_r2));
        });
        ɵɵtemplate(1, Terminal_div_1_Template, 2, 1, "div", 2);
        ɵɵelementStart(2, "div", 3);
        ɵɵtemplate(3, Terminal_div_3_Template, 7, 4, "div", 4);
        ɵɵelementEnd();
        ɵɵelementStart(4, "div", 5)(5, "span", 6);
        ɵɵtext(6);
        ɵɵelementEnd();
        ɵɵelementStart(7, "input", 7, 0);
        ɵɵtwoWayListener("ngModelChange", function Terminal_Template_input_ngModelChange_7_listener($event) {
          ɵɵrestoreView(_r1);
          ɵɵtwoWayBindingSet(ctx.command, $event) || (ctx.command = $event);
          return ɵɵresetView($event);
        });
        ɵɵlistener("keydown", function Terminal_Template_input_keydown_7_listener($event) {
          ɵɵrestoreView(_r1);
          return ɵɵresetView(ctx.handleCommand($event));
        });
        ɵɵelementEnd()()();
      }
      if (rf & 2) {
        ɵɵclassMap(ctx.styleClass);
        ɵɵproperty("ngClass", "p-terminal p-component")("ngStyle", ctx.style);
        ɵɵadvance();
        ɵɵproperty("ngIf", ctx.welcomeMessage);
        ɵɵadvance(2);
        ɵɵproperty("ngForOf", ctx.commands);
        ɵɵadvance(3);
        ɵɵtextInterpolate(ctx.prompt);
        ɵɵadvance();
        ɵɵtwoWayProperty("ngModel", ctx.command);
      }
    },
    dependencies: [CommonModule, NgClass, NgForOf, NgIf, NgStyle, FormsModule, DefaultValueAccessor, NgControlStatus, NgModel, SharedModule],
    encapsulation: 2,
    changeDetection: 0
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(Terminal, [{
    type: Component,
    args: [{
      selector: "p-terminal",
      standalone: true,
      imports: [CommonModule, FormsModule, SharedModule],
      template: `
        <div [ngClass]="'p-terminal p-component'" [ngStyle]="style" [class]="styleClass" (click)="focus(in)">
            <div class="p-terminal-welcome-message" *ngIf="welcomeMessage">{{ welcomeMessage }}</div>
            <div class="p-terminal-command-list">
                <div class="p-terminal-command" *ngFor="let command of commands">
                    <span class="p-terminal-prompt-label">{{ prompt }}</span>
                    <span class="p-terminal-command-value">{{ command.text }}</span>
                    <div class="p-terminal-command-response" [attr.aria-live]="'polite'">{{ command.response }}</div>
                </div>
            </div>
            <div class="p-terminal-prompt">
                <span class="p-terminal-prompt-label">{{ prompt }}</span>
                <input #in type="text" [(ngModel)]="command" class="p-terminal-prompt-value" autocomplete="off" (keydown)="handleCommand($event)" autofocus />
            </div>
        </div>
    `,
      changeDetection: ChangeDetectionStrategy.OnPush,
      encapsulation: ViewEncapsulation.None,
      providers: [TerminalStyle]
    }]
  }], () => [{
    type: TerminalService
  }], {
    welcomeMessage: [{
      type: Input
    }],
    prompt: [{
      type: Input
    }],
    style: [{
      type: Input
    }],
    styleClass: [{
      type: Input
    }],
    response: [{
      type: Input
    }]
  });
})();
var TerminalModule = class _TerminalModule {
  static ɵfac = function TerminalModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _TerminalModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _TerminalModule,
    imports: [Terminal, SharedModule],
    exports: [Terminal, SharedModule]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [Terminal, SharedModule, SharedModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(TerminalModule, [{
    type: NgModule,
    args: [{
      exports: [Terminal, SharedModule],
      imports: [Terminal, SharedModule]
    }]
  }], null, null);
})();
export {
  Terminal,
  TerminalClasses,
  TerminalModule,
  TerminalService,
  TerminalStyle
};
//# sourceMappingURL=primeng_terminal.js.map
