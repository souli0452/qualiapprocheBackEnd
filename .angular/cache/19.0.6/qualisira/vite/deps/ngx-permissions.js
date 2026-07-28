import {
  Router,
  RouterStateSnapshot
} from "./chunk-M3JF7BPQ.js";
import "./chunk-HQ743VTI.js";
import "./chunk-ORXNKOXS.js";
import "./chunk-YQX3S2V2.js";
import {
  ChangeDetectorRef,
  Directive,
  EventEmitter,
  Inject,
  Injectable,
  InjectionToken,
  Input,
  NgModule,
  Output,
  TemplateRef,
  ViewContainerRef,
  inject,
  setClassMetadata,
  ɵɵNgOnChangesFeature,
  ɵɵdefineDirective,
  ɵɵdefineInjectable,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵinject
} from "./chunk-ECNLMTOX.js";
import {
  forkJoin,
  merge
} from "./chunk-4N4GOYJH.js";
import "./chunk-5OPE3T2R.js";
import {
  BehaviorSubject,
  catchError,
  every,
  first,
  from,
  map,
  mergeAll,
  mergeMap,
  of,
  skip,
  switchMap,
  take,
  tap
} from "./chunk-FHTVLBLO.js";
import {
  __spreadProps,
  __spreadValues
} from "./chunk-N6ESDQJH.js";

// node_modules/ngx-permissions/fesm2022/ngx-permissions.mjs
var NgxPermissionsPredefinedStrategies = {
  REMOVE: "remove",
  SHOW: "show"
};
var NgxPermissionsConfigurationStore = class _NgxPermissionsConfigurationStore {
  constructor() {
    this.strategiesSource = new BehaviorSubject({});
    this.strategies$ = this.strategiesSource.asObservable();
  }
  static {
    this.ɵfac = function NgxPermissionsConfigurationStore_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsConfigurationStore)();
    };
  }
  static {
    this.ɵprov = ɵɵdefineInjectable({
      token: _NgxPermissionsConfigurationStore,
      factory: _NgxPermissionsConfigurationStore.ɵfac
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsConfigurationStore, [{
    type: Injectable
  }], null, null);
})();
var USE_CONFIGURATION_STORE = new InjectionToken("USE_CONFIGURATION_STORE");
var NgxPermissionsConfigurationService = class _NgxPermissionsConfigurationService {
  constructor(isolate = false, configurationStore) {
    this.isolate = isolate;
    this.configurationStore = configurationStore;
    this.strategiesSource = this.isolate ? new BehaviorSubject({}) : this.configurationStore.strategiesSource;
    this.strategies$ = this.strategiesSource.asObservable();
    this.onAuthorisedDefaultStrategy = this.isolate ? void 0 : this.configurationStore.onAuthorisedDefaultStrategy;
    this.onUnAuthorisedDefaultStrategy = this.isolate ? void 0 : this.configurationStore.onUnAuthorisedDefaultStrategy;
  }
  setDefaultOnAuthorizedStrategy(name) {
    if (this.isolate) {
      this.onAuthorisedDefaultStrategy = this.getDefinedStrategy(name);
    } else {
      this.configurationStore.onAuthorisedDefaultStrategy = this.getDefinedStrategy(name);
      this.onAuthorisedDefaultStrategy = this.configurationStore.onAuthorisedDefaultStrategy;
    }
  }
  setDefaultOnUnauthorizedStrategy(name) {
    if (this.isolate) {
      this.onUnAuthorisedDefaultStrategy = this.getDefinedStrategy(name);
    } else {
      this.configurationStore.onUnAuthorisedDefaultStrategy = this.getDefinedStrategy(name);
      this.onUnAuthorisedDefaultStrategy = this.configurationStore.onUnAuthorisedDefaultStrategy;
    }
  }
  addPermissionStrategy(key, func) {
    this.strategiesSource.value[key] = func;
  }
  getStrategy(key) {
    return this.strategiesSource.value[key];
  }
  getAllStrategies() {
    return this.strategiesSource.value;
  }
  getDefinedStrategy(name) {
    if (this.strategiesSource.value[name] || this.isPredefinedStrategy(name)) {
      return name;
    } else {
      throw new Error(`No ' ${name} ' strategy is found please define one`);
    }
  }
  isPredefinedStrategy(strategy) {
    return strategy === NgxPermissionsPredefinedStrategies.SHOW || strategy === NgxPermissionsPredefinedStrategies.REMOVE;
  }
  static {
    this.ɵfac = function NgxPermissionsConfigurationService_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsConfigurationService)(ɵɵinject(USE_CONFIGURATION_STORE), ɵɵinject(NgxPermissionsConfigurationStore));
    };
  }
  static {
    this.ɵprov = ɵɵdefineInjectable({
      token: _NgxPermissionsConfigurationService,
      factory: _NgxPermissionsConfigurationService.ɵfac
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsConfigurationService, [{
    type: Injectable
  }], () => [{
    type: void 0,
    decorators: [{
      type: Inject,
      args: [USE_CONFIGURATION_STORE]
    }]
  }, {
    type: NgxPermissionsConfigurationStore
  }], null);
})();
function isFunction(value) {
  return typeof value === "function";
}
function isPlainObject(value) {
  if (Object.prototype.toString.call(value) !== "[object Object]") {
    return false;
  } else {
    const prototype = Object.getPrototypeOf(value);
    return prototype === null || prototype === Object.prototype;
  }
}
function isString(value) {
  return !!value && typeof value === "string";
}
function isBoolean(value) {
  return typeof value === "boolean";
}
function isPromise(promise) {
  return Object.prototype.toString.call(promise) === "[object Promise]";
}
function notEmptyValue(value) {
  if (Array.isArray(value)) {
    return value.length > 0;
  }
  return !!value;
}
function transformStringToArray(value) {
  if (isString(value)) {
    return [value];
  }
  return value;
}
var NgxPermissionsStore = class _NgxPermissionsStore {
  constructor() {
    this.permissionsSource = new BehaviorSubject({});
    this.permissions$ = this.permissionsSource.asObservable();
  }
  static {
    this.ɵfac = function NgxPermissionsStore_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsStore)();
    };
  }
  static {
    this.ɵprov = ɵɵdefineInjectable({
      token: _NgxPermissionsStore,
      factory: _NgxPermissionsStore.ɵfac
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsStore, [{
    type: Injectable
  }], null, null);
})();
var USE_PERMISSIONS_STORE = new InjectionToken("USE_PERMISSIONS_STORE");
var NgxPermissionsService = class _NgxPermissionsService {
  constructor(isolate = false, permissionsStore) {
    this.isolate = isolate;
    this.permissionsStore = permissionsStore;
    this.permissionsSource = this.isolate ? new BehaviorSubject({}) : this.permissionsStore.permissionsSource;
    this.permissions$ = this.permissionsSource.asObservable();
  }
  /**
   * Remove all permissions from permissions source
   */
  flushPermissions() {
    this.permissionsSource.next({});
  }
  hasPermission(permission) {
    if (!permission || Array.isArray(permission) && permission.length === 0) {
      return Promise.resolve(true);
    }
    permission = transformStringToArray(permission);
    return this.hasArrayPermission(permission);
  }
  loadPermissions(permissions, validationFunction) {
    const newPermissions = permissions.reduce((source, name) => this.reducePermission(source, name, validationFunction), {});
    this.permissionsSource.next(newPermissions);
  }
  addPermission(permission, validationFunction) {
    if (Array.isArray(permission)) {
      const permissions = permission.reduce((source, name) => this.reducePermission(source, name, validationFunction), this.permissionsSource.value);
      this.permissionsSource.next(permissions);
    } else {
      const permissions = this.reducePermission(this.permissionsSource.value, permission, validationFunction);
      this.permissionsSource.next(permissions);
    }
  }
  removePermission(permissionName) {
    const permissions = __spreadValues({}, this.permissionsSource.value);
    delete permissions[permissionName];
    this.permissionsSource.next(permissions);
  }
  getPermission(name) {
    return this.permissionsSource.value[name];
  }
  getPermissions() {
    return this.permissionsSource.value;
  }
  reducePermission(source, name, validationFunction) {
    if (!!validationFunction && isFunction(validationFunction)) {
      return __spreadProps(__spreadValues({}, source), {
        [name]: {
          name,
          validationFunction
        }
      });
    }
    return __spreadProps(__spreadValues({}, source), {
      [name]: {
        name
      }
    });
  }
  hasArrayPermission(permissions) {
    const promises = permissions.map((key) => {
      if (this.hasPermissionValidationFunction(key)) {
        const validationFunction = this.permissionsSource.value[key].validationFunction;
        const immutableValue = __spreadValues({}, this.permissionsSource.value);
        return of(null).pipe(map(() => validationFunction(key, immutableValue)), switchMap((promise) => isBoolean(promise) ? of(promise) : promise), catchError(() => of(false)));
      }
      return of(!!this.permissionsSource.value[key]);
    });
    return from(promises).pipe(mergeAll(), first((data) => data !== false, false), map((data) => data !== false)).toPromise().then((data) => data);
  }
  hasPermissionValidationFunction(key) {
    return !!this.permissionsSource.value[key] && !!this.permissionsSource.value[key].validationFunction && isFunction(this.permissionsSource.value[key].validationFunction);
  }
  static {
    this.ɵfac = function NgxPermissionsService_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsService)(ɵɵinject(USE_PERMISSIONS_STORE), ɵɵinject(NgxPermissionsStore));
    };
  }
  static {
    this.ɵprov = ɵɵdefineInjectable({
      token: _NgxPermissionsService,
      factory: _NgxPermissionsService.ɵfac
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsService, [{
    type: Injectable
  }], () => [{
    type: void 0,
    decorators: [{
      type: Inject,
      args: [USE_PERMISSIONS_STORE]
    }]
  }, {
    type: NgxPermissionsStore
  }], null);
})();
var NgxRolesStore = class {
  constructor() {
    this.rolesSource = new BehaviorSubject({});
    this.roles$ = this.rolesSource.asObservable();
  }
};
var USE_ROLES_STORE = new InjectionToken("USE_ROLES_STORE");
var NgxRolesService = class _NgxRolesService {
  constructor(isolate = false, rolesStore, permissionsService) {
    this.isolate = isolate;
    this.rolesStore = rolesStore;
    this.permissionsService = permissionsService;
    this.rolesSource = this.isolate ? new BehaviorSubject({}) : this.rolesStore.rolesSource;
    this.roles$ = this.rolesSource.asObservable();
  }
  addRole(name, validationFunction) {
    const roles = __spreadProps(__spreadValues({}, this.rolesSource.value), {
      [name]: {
        name,
        validationFunction
      }
    });
    this.rolesSource.next(roles);
  }
  addRoleWithPermissions(name, permissions) {
    this.permissionsService.addPermission(permissions);
    this.addRole(name, permissions);
  }
  addRoles(rolesObj) {
    Object.keys(rolesObj).forEach((key, index) => {
      this.addRole(key, rolesObj[key]);
    });
  }
  addRolesWithPermissions(rolesObj) {
    Object.keys(rolesObj).forEach((key, index) => {
      this.addRoleWithPermissions(key, rolesObj[key]);
    });
  }
  flushRoles() {
    this.rolesSource.next({});
  }
  flushRolesAndPermissions() {
    this.flushRoles();
    this.permissionsService.flushPermissions();
  }
  removeRole(roleName) {
    const roles = __spreadValues({}, this.rolesSource.value);
    delete roles[roleName];
    this.rolesSource.next(roles);
  }
  getRoles() {
    return this.rolesSource.value;
  }
  getRole(name) {
    return this.rolesSource.value[name];
  }
  hasOnlyRoles(names) {
    const isNamesEmpty = !names || Array.isArray(names) && names.length === 0;
    if (isNamesEmpty) {
      return Promise.resolve(true);
    }
    names = transformStringToArray(names);
    return Promise.all([this.hasRoleKey(names), this.hasRolePermission(this.rolesSource.value, names)]).then(([hasRoles, hasPermissions]) => {
      return hasRoles || hasPermissions;
    });
  }
  hasRoleKey(roleName) {
    const promises = roleName.map((key) => {
      const hasValidationFunction = !!this.rolesSource.value[key] && !!this.rolesSource.value[key].validationFunction && isFunction(this.rolesSource.value[key].validationFunction);
      if (hasValidationFunction && !isPromise(this.rolesSource.value[key].validationFunction)) {
        const validationFunction = this.rolesSource.value[key].validationFunction;
        const immutableValue = __spreadValues({}, this.rolesSource.value);
        return of(null).pipe(map(() => validationFunction(key, immutableValue)), switchMap((promise) => isBoolean(promise) ? of(promise) : promise), catchError(() => of(false)));
      }
      return of(false);
    });
    return from(promises).pipe(mergeAll(), first((data) => data !== false, false), map((data) => data !== false)).toPromise().then((data) => data);
  }
  hasRolePermission(roles, roleNames) {
    return from(roleNames).pipe(mergeMap((key) => {
      if (roles[key] && Array.isArray(roles[key].validationFunction)) {
        return from(roles[key].validationFunction).pipe(mergeMap((permission) => this.permissionsService.hasPermission(permission)), every((hasPermission) => hasPermission === true));
      }
      return of(false);
    }), first((hasPermission) => hasPermission === true, false)).toPromise();
  }
  static {
    this.ɵfac = function NgxRolesService_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxRolesService)(ɵɵinject(USE_ROLES_STORE), ɵɵinject(NgxRolesStore), ɵɵinject(NgxPermissionsService));
    };
  }
  static {
    this.ɵprov = ɵɵdefineInjectable({
      token: _NgxRolesService,
      factory: _NgxRolesService.ɵfac
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxRolesService, [{
    type: Injectable
  }], () => [{
    type: void 0,
    decorators: [{
      type: Inject,
      args: [USE_ROLES_STORE]
    }]
  }, {
    type: NgxRolesStore
  }, {
    type: NgxPermissionsService
  }], null);
})();
var NgxPermissionsDirective = class _NgxPermissionsDirective {
  constructor() {
    this.permissionsAuthorized = new EventEmitter();
    this.permissionsUnauthorized = new EventEmitter();
    this.firstMergeUnusedRun = 1;
    this.permissionsService = inject(NgxPermissionsService);
    this.configurationService = inject(NgxPermissionsConfigurationService);
    this.rolesService = inject(NgxRolesService);
    this.viewContainer = inject(ViewContainerRef);
    this.changeDetector = inject(ChangeDetectorRef);
    this.templateRef = inject(TemplateRef);
  }
  ngOnInit() {
    this.viewContainer.clear();
    this.initPermissionSubscription = this.validateExceptOnlyPermissions();
  }
  ngOnChanges(changes) {
    const onlyChanges = changes["ngxPermissionsOnly"];
    const exceptChanges = changes["ngxPermissionsExcept"];
    if (onlyChanges || exceptChanges) {
      if (onlyChanges && onlyChanges.firstChange) {
        return;
      }
      if (exceptChanges && exceptChanges.firstChange) {
        return;
      }
      merge(this.permissionsService.permissions$, this.rolesService.roles$).pipe(skip(this.firstMergeUnusedRun), take(1)).subscribe(() => {
        if (notEmptyValue(this.ngxPermissionsExcept)) {
          this.validateExceptAndOnlyPermissions();
          return;
        }
        if (notEmptyValue(this.ngxPermissionsOnly)) {
          this.validateOnlyPermissions();
          return;
        }
        this.handleAuthorisedPermission(this.getAuthorisedTemplates());
      });
    }
  }
  ngOnDestroy() {
    if (this.initPermissionSubscription) {
      this.initPermissionSubscription.unsubscribe();
    }
  }
  validateExceptOnlyPermissions() {
    return merge(this.permissionsService.permissions$, this.rolesService.roles$).pipe(skip(this.firstMergeUnusedRun)).subscribe(() => {
      if (notEmptyValue(this.ngxPermissionsExcept)) {
        this.validateExceptAndOnlyPermissions();
        return;
      }
      if (notEmptyValue(this.ngxPermissionsOnly)) {
        this.validateOnlyPermissions();
        return;
      }
      this.handleAuthorisedPermission(this.getAuthorisedTemplates());
    });
  }
  validateExceptAndOnlyPermissions() {
    Promise.all([this.permissionsService.hasPermission(this.ngxPermissionsExcept), this.rolesService.hasOnlyRoles(this.ngxPermissionsExcept)]).then(([hasPermission, hasRole]) => {
      if (hasPermission || hasRole) {
        this.handleUnauthorisedPermission(this.ngxPermissionsExceptElse || this.ngxPermissionsElse);
        return;
      }
      if (!!this.ngxPermissionsOnly) {
        throw false;
      }
      this.handleAuthorisedPermission(this.ngxPermissionsExceptThen || this.ngxPermissionsThen || this.templateRef);
    }).catch(() => {
      if (!!this.ngxPermissionsOnly) {
        this.validateOnlyPermissions();
      } else {
        this.handleAuthorisedPermission(this.ngxPermissionsExceptThen || this.ngxPermissionsThen || this.templateRef);
      }
    });
  }
  validateOnlyPermissions() {
    Promise.all([this.permissionsService.hasPermission(this.ngxPermissionsOnly), this.rolesService.hasOnlyRoles(this.ngxPermissionsOnly)]).then(([hasPermissions, hasRoles]) => {
      if (hasPermissions || hasRoles) {
        this.handleAuthorisedPermission(this.ngxPermissionsOnlyThen || this.ngxPermissionsThen || this.templateRef);
      } else {
        this.handleUnauthorisedPermission(this.ngxPermissionsOnlyElse || this.ngxPermissionsElse);
      }
    }).catch(() => {
      this.handleUnauthorisedPermission(this.ngxPermissionsOnlyElse || this.ngxPermissionsElse);
    });
  }
  handleUnauthorisedPermission(template) {
    if (isBoolean(this.currentAuthorizedState) && !this.currentAuthorizedState) {
      return;
    }
    this.currentAuthorizedState = false;
    this.permissionsUnauthorized.emit();
    if (this.getUnAuthorizedStrategyInput()) {
      this.applyStrategyAccordingToStrategyType(this.getUnAuthorizedStrategyInput());
      return;
    }
    if (this.configurationService.onUnAuthorisedDefaultStrategy && !this.elseBlockDefined()) {
      this.applyStrategy(this.configurationService.onUnAuthorisedDefaultStrategy);
    } else {
      this.showTemplateBlockInView(template);
    }
  }
  handleAuthorisedPermission(template) {
    if (isBoolean(this.currentAuthorizedState) && this.currentAuthorizedState) {
      return;
    }
    this.currentAuthorizedState = true;
    this.permissionsAuthorized.emit();
    if (this.getAuthorizedStrategyInput()) {
      this.applyStrategyAccordingToStrategyType(this.getAuthorizedStrategyInput());
      return;
    }
    if (this.configurationService.onAuthorisedDefaultStrategy && !this.thenBlockDefined()) {
      this.applyStrategy(this.configurationService.onAuthorisedDefaultStrategy);
    } else {
      this.showTemplateBlockInView(template);
    }
  }
  applyStrategyAccordingToStrategyType(strategy) {
    if (isString(strategy)) {
      this.applyStrategy(strategy);
      return;
    }
    if (isFunction(strategy)) {
      this.showTemplateBlockInView(this.templateRef);
      strategy(this.templateRef);
      return;
    }
  }
  showTemplateBlockInView(template) {
    this.viewContainer.clear();
    if (!template) {
      return;
    }
    this.viewContainer.createEmbeddedView(template);
    this.changeDetector.markForCheck();
  }
  getAuthorisedTemplates() {
    return this.ngxPermissionsOnlyThen || this.ngxPermissionsExceptThen || this.ngxPermissionsThen || this.templateRef;
  }
  elseBlockDefined() {
    return !!this.ngxPermissionsExceptElse || !!this.ngxPermissionsElse;
  }
  thenBlockDefined() {
    return !!this.ngxPermissionsExceptThen || !!this.ngxPermissionsThen;
  }
  getAuthorizedStrategyInput() {
    return this.ngxPermissionsOnlyAuthorisedStrategy || this.ngxPermissionsExceptAuthorisedStrategy || this.ngxPermissionsAuthorisedStrategy;
  }
  getUnAuthorizedStrategyInput() {
    return this.ngxPermissionsOnlyUnauthorisedStrategy || this.ngxPermissionsExceptUnauthorisedStrategy || this.ngxPermissionsUnauthorisedStrategy;
  }
  applyStrategy(name) {
    if (name === NgxPermissionsPredefinedStrategies.SHOW) {
      this.showTemplateBlockInView(this.templateRef);
      return;
    }
    if (name === NgxPermissionsPredefinedStrategies.REMOVE) {
      this.viewContainer.clear();
      return;
    }
    const strategy = this.configurationService.getStrategy(name);
    this.showTemplateBlockInView(this.templateRef);
    strategy(this.templateRef);
  }
  static {
    this.ɵfac = function NgxPermissionsDirective_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsDirective)();
    };
  }
  static {
    this.ɵdir = ɵɵdefineDirective({
      type: _NgxPermissionsDirective,
      selectors: [["", "ngxPermissionsOnly", ""], ["", "ngxPermissionsExcept", ""]],
      inputs: {
        ngxPermissionsOnly: "ngxPermissionsOnly",
        ngxPermissionsOnlyThen: "ngxPermissionsOnlyThen",
        ngxPermissionsOnlyElse: "ngxPermissionsOnlyElse",
        ngxPermissionsExcept: "ngxPermissionsExcept",
        ngxPermissionsExceptElse: "ngxPermissionsExceptElse",
        ngxPermissionsExceptThen: "ngxPermissionsExceptThen",
        ngxPermissionsThen: "ngxPermissionsThen",
        ngxPermissionsElse: "ngxPermissionsElse",
        ngxPermissionsOnlyAuthorisedStrategy: "ngxPermissionsOnlyAuthorisedStrategy",
        ngxPermissionsOnlyUnauthorisedStrategy: "ngxPermissionsOnlyUnauthorisedStrategy",
        ngxPermissionsExceptUnauthorisedStrategy: "ngxPermissionsExceptUnauthorisedStrategy",
        ngxPermissionsExceptAuthorisedStrategy: "ngxPermissionsExceptAuthorisedStrategy",
        ngxPermissionsUnauthorisedStrategy: "ngxPermissionsUnauthorisedStrategy",
        ngxPermissionsAuthorisedStrategy: "ngxPermissionsAuthorisedStrategy"
      },
      outputs: {
        permissionsAuthorized: "permissionsAuthorized",
        permissionsUnauthorized: "permissionsUnauthorized"
      },
      standalone: false,
      features: [ɵɵNgOnChangesFeature]
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsDirective, [{
    type: Directive,
    args: [{
      selector: "[ngxPermissionsOnly],[ngxPermissionsExcept]",
      standalone: false
    }]
  }], null, {
    ngxPermissionsOnly: [{
      type: Input
    }],
    ngxPermissionsOnlyThen: [{
      type: Input
    }],
    ngxPermissionsOnlyElse: [{
      type: Input
    }],
    ngxPermissionsExcept: [{
      type: Input
    }],
    ngxPermissionsExceptElse: [{
      type: Input
    }],
    ngxPermissionsExceptThen: [{
      type: Input
    }],
    ngxPermissionsThen: [{
      type: Input
    }],
    ngxPermissionsElse: [{
      type: Input
    }],
    ngxPermissionsOnlyAuthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsOnlyUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsExceptUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsExceptAuthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsAuthorisedStrategy: [{
      type: Input
    }],
    permissionsAuthorized: [{
      type: Output
    }],
    permissionsUnauthorized: [{
      type: Output
    }]
  });
})();
var DEFAULT_REDIRECT_KEY = "default";
var ngxPermissionsGuard = (route, state) => {
  const permissionsGuard = inject(NgxPermissionsGuard);
  if (state instanceof RouterStateSnapshot) {
    return permissionsGuard.hasPermissions(route, state);
  }
  return permissionsGuard.hasPermissions(route);
};
var NgxPermissionsGuard = class _NgxPermissionsGuard {
  constructor(permissionsService, rolesService, router) {
    this.permissionsService = permissionsService;
    this.rolesService = rolesService;
    this.router = router;
  }
  canActivate(route, state) {
    return this.hasPermissions(route, state);
  }
  canActivateChild(childRoute, state) {
    return this.hasPermissions(childRoute, state);
  }
  canLoad(route) {
    return this.hasPermissions(route);
  }
  canMatch(route) {
    return this.hasPermissions(route);
  }
  hasPermissions(route, state) {
    const routeDataPermissions = !!route && route.data ? route.data["permissions"] : {};
    const permissions = this.transformPermission(routeDataPermissions, route, state);
    if (this.isParameterAvailable(permissions.except)) {
      return this.passingExceptPermissionsValidation(permissions, route, state);
    }
    if (this.isParameterAvailable(permissions.only)) {
      return this.passingOnlyPermissionsValidation(permissions, route, state);
    }
    return true;
  }
  transformPermission(permissions, route, state) {
    const only = isFunction(permissions.only) ? permissions.only(route, state) : transformStringToArray(permissions.only);
    const except = isFunction(permissions.except) ? permissions.except(route, state) : transformStringToArray(permissions.except);
    const redirectTo = permissions.redirectTo;
    return {
      only,
      except,
      redirectTo
    };
  }
  isParameterAvailable(permission) {
    return !!permission && permission.length > 0;
  }
  passingExceptPermissionsValidation(permissions, route, state) {
    if (!!permissions.redirectTo && (isFunction(permissions.redirectTo) || isPlainObject(permissions.redirectTo) && !this.isRedirectionWithParameters(permissions.redirectTo))) {
      let failedPermission = "";
      return from(permissions.except).pipe(mergeMap((permissionsExcept) => {
        return forkJoin([this.permissionsService.hasPermission(permissionsExcept), this.rolesService.hasOnlyRoles(permissionsExcept)]).pipe(tap((hasPermissions) => {
          const dontHavePermissions = hasPermissions.every((hasPermission) => hasPermission === false);
          if (!dontHavePermissions) {
            failedPermission = permissionsExcept;
          }
        }));
      }), first((hasPermissions) => hasPermissions.some((hasPermission) => hasPermission === true), false), mergeMap((isAllFalse) => {
        if (!!failedPermission) {
          this.handleRedirectOfFailedPermission(permissions, failedPermission, route, state);
          return of(false);
        }
        if (!isAllFalse && permissions.only) {
          return this.onlyRedirectCheck(permissions, route, state);
        }
        return of(!isAllFalse);
      })).toPromise();
    }
    return Promise.all([this.permissionsService.hasPermission(permissions.except), this.rolesService.hasOnlyRoles(permissions.except)]).then(([hasPermission, hasRoles]) => {
      if (hasPermission || hasRoles) {
        if (permissions.redirectTo) {
          this.redirectToAnotherRoute(permissions.redirectTo, route, state);
        }
        return false;
      }
      if (permissions.only) {
        return this.checkOnlyPermissions(permissions, route, state);
      }
      return true;
    });
  }
  redirectToAnotherRoute(permissionRedirectTo, route, state, failedPermissionName) {
    const redirectTo = isFunction(permissionRedirectTo) ? permissionRedirectTo(failedPermissionName, route, state) : permissionRedirectTo;
    if (this.isRedirectionWithParameters(redirectTo)) {
      redirectTo.navigationCommands = this.transformNavigationCommands(redirectTo.navigationCommands, route, state);
      redirectTo.navigationExtras = this.transformNavigationExtras(redirectTo.navigationExtras, route, state);
      this.router.navigate(redirectTo.navigationCommands, redirectTo.navigationExtras);
      return;
    }
    if (Array.isArray(redirectTo)) {
      this.router.navigate(redirectTo);
    } else {
      this.router.navigate([redirectTo]);
    }
  }
  isRedirectionWithParameters(object) {
    return isPlainObject(object) && (!!object.navigationCommands || !!object.navigationExtras);
  }
  transformNavigationCommands(navigationCommands, route, state) {
    return isFunction(navigationCommands) ? navigationCommands(route, state) : navigationCommands;
  }
  transformNavigationExtras(navigationExtras, route, state) {
    return isFunction(navigationExtras) ? navigationExtras(route, state) : navigationExtras;
  }
  onlyRedirectCheck(permissions, route, state) {
    let failedPermission = "";
    return from(permissions.only).pipe(mergeMap((permissionsOnly) => {
      return forkJoin([this.permissionsService.hasPermission(permissionsOnly), this.rolesService.hasOnlyRoles(permissionsOnly)]).pipe(tap((hasPermissions) => {
        const failed = hasPermissions.every((hasPermission) => hasPermission === false);
        if (failed) {
          failedPermission = permissionsOnly;
        }
      }));
    }), first((hasPermissions) => {
      if (isFunction(permissions.redirectTo)) {
        return hasPermissions.some((hasPermission) => hasPermission === true);
      }
      return hasPermissions.every((hasPermission) => hasPermission === false);
    }, false), mergeMap((pass) => {
      if (isFunction(permissions.redirectTo)) {
        if (pass) {
          return of(true);
        } else {
          this.handleRedirectOfFailedPermission(permissions, failedPermission, route, state);
          return of(false);
        }
      } else {
        if (!!failedPermission) {
          this.handleRedirectOfFailedPermission(permissions, failedPermission, route, state);
        }
        return of(!pass);
      }
    })).toPromise();
  }
  handleRedirectOfFailedPermission(permissions, failedPermission, route, state) {
    if (this.isFailedPermissionPropertyOfRedirectTo(permissions, failedPermission)) {
      this.redirectToAnotherRoute(permissions.redirectTo[failedPermission], route, state, failedPermission);
    } else {
      if (isFunction(permissions.redirectTo)) {
        this.redirectToAnotherRoute(permissions.redirectTo, route, state, failedPermission);
      } else {
        this.redirectToAnotherRoute(permissions.redirectTo[DEFAULT_REDIRECT_KEY], route, state, failedPermission);
      }
    }
  }
  isFailedPermissionPropertyOfRedirectTo(permissions, failedPermission) {
    return !!permissions.redirectTo && permissions.redirectTo[failedPermission];
  }
  checkOnlyPermissions(purePermissions, route, state) {
    const permissions = __spreadValues({}, purePermissions);
    return Promise.all([this.permissionsService.hasPermission(permissions.only), this.rolesService.hasOnlyRoles(permissions.only)]).then(([hasPermission, hasRole]) => {
      if (hasPermission || hasRole) {
        return true;
      }
      if (permissions.redirectTo) {
        this.redirectToAnotherRoute(permissions.redirectTo, route, state);
      }
      return false;
    });
  }
  passingOnlyPermissionsValidation(permissions, route, state) {
    if (isFunction(permissions.redirectTo) || isPlainObject(permissions.redirectTo) && !this.isRedirectionWithParameters(permissions.redirectTo)) {
      return this.onlyRedirectCheck(permissions, route, state);
    }
    return this.checkOnlyPermissions(permissions, route, state);
  }
  static {
    this.ɵfac = function NgxPermissionsGuard_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsGuard)(ɵɵinject(NgxPermissionsService), ɵɵinject(NgxRolesService), ɵɵinject(Router));
    };
  }
  static {
    this.ɵprov = ɵɵdefineInjectable({
      token: _NgxPermissionsGuard,
      factory: _NgxPermissionsGuard.ɵfac
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsGuard, [{
    type: Injectable
  }], () => [{
    type: NgxPermissionsService
  }, {
    type: NgxRolesService
  }, {
    type: Router
  }], null);
})();
var NgxPermissionsAllowStubDirective = class _NgxPermissionsAllowStubDirective {
  constructor() {
    this.permissionsAuthorized = new EventEmitter();
    this.permissionsUnauthorized = new EventEmitter();
    this.viewContainer = inject(ViewContainerRef);
    this.templateRef = inject(TemplateRef);
  }
  ngOnInit() {
    this.viewContainer.clear();
    this.viewContainer.createEmbeddedView(this.getAuthorizedTemplate());
    this.permissionsAuthorized.emit();
  }
  getAuthorizedTemplate() {
    return this.ngxPermissionsOnlyThen || this.ngxPermissionsExceptThen || this.ngxPermissionsThen || this.templateRef;
  }
  static {
    this.ɵfac = function NgxPermissionsAllowStubDirective_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsAllowStubDirective)();
    };
  }
  static {
    this.ɵdir = ɵɵdefineDirective({
      type: _NgxPermissionsAllowStubDirective,
      selectors: [["", "ngxPermissionsOnly", ""], ["", "ngxPermissionsExcept", ""]],
      inputs: {
        ngxPermissionsOnly: "ngxPermissionsOnly",
        ngxPermissionsOnlyThen: "ngxPermissionsOnlyThen",
        ngxPermissionsOnlyElse: "ngxPermissionsOnlyElse",
        ngxPermissionsExcept: "ngxPermissionsExcept",
        ngxPermissionsExceptElse: "ngxPermissionsExceptElse",
        ngxPermissionsExceptThen: "ngxPermissionsExceptThen",
        ngxPermissionsThen: "ngxPermissionsThen",
        ngxPermissionsElse: "ngxPermissionsElse",
        ngxPermissionsOnlyAuthorisedStrategy: "ngxPermissionsOnlyAuthorisedStrategy",
        ngxPermissionsOnlyUnauthorisedStrategy: "ngxPermissionsOnlyUnauthorisedStrategy",
        ngxPermissionsExceptUnauthorisedStrategy: "ngxPermissionsExceptUnauthorisedStrategy",
        ngxPermissionsExceptAuthorisedStrategy: "ngxPermissionsExceptAuthorisedStrategy",
        ngxPermissionsUnauthorisedStrategy: "ngxPermissionsUnauthorisedStrategy",
        ngxPermissionsAuthorisedStrategy: "ngxPermissionsAuthorisedStrategy"
      },
      outputs: {
        permissionsAuthorized: "permissionsAuthorized",
        permissionsUnauthorized: "permissionsUnauthorized"
      }
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsAllowStubDirective, [{
    type: Directive,
    args: [{
      standalone: true,
      selector: "[ngxPermissionsOnly],[ngxPermissionsExcept]"
    }]
  }], null, {
    ngxPermissionsOnly: [{
      type: Input
    }],
    ngxPermissionsOnlyThen: [{
      type: Input
    }],
    ngxPermissionsOnlyElse: [{
      type: Input
    }],
    ngxPermissionsExcept: [{
      type: Input
    }],
    ngxPermissionsExceptElse: [{
      type: Input
    }],
    ngxPermissionsExceptThen: [{
      type: Input
    }],
    ngxPermissionsThen: [{
      type: Input
    }],
    ngxPermissionsElse: [{
      type: Input
    }],
    ngxPermissionsOnlyAuthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsOnlyUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsExceptUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsExceptAuthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsAuthorisedStrategy: [{
      type: Input
    }],
    permissionsAuthorized: [{
      type: Output
    }],
    permissionsUnauthorized: [{
      type: Output
    }]
  });
})();
var NgxPermissionsRestrictStubDirective = class _NgxPermissionsRestrictStubDirective {
  constructor() {
    this.permissionsAuthorized = new EventEmitter();
    this.permissionsUnauthorized = new EventEmitter();
    this.viewContainer = inject(ViewContainerRef);
  }
  ngOnInit() {
    this.viewContainer.clear();
    if (this.getUnAuthorizedTemplate()) {
      this.viewContainer.createEmbeddedView(this.getUnAuthorizedTemplate());
    }
    this.permissionsUnauthorized.emit();
  }
  getUnAuthorizedTemplate() {
    return this.ngxPermissionsOnlyElse || this.ngxPermissionsExceptElse || this.ngxPermissionsElse;
  }
  static {
    this.ɵfac = function NgxPermissionsRestrictStubDirective_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsRestrictStubDirective)();
    };
  }
  static {
    this.ɵdir = ɵɵdefineDirective({
      type: _NgxPermissionsRestrictStubDirective,
      selectors: [["", "ngxPermissionsOnly", ""], ["", "ngxPermissionsExcept", ""]],
      inputs: {
        ngxPermissionsOnly: "ngxPermissionsOnly",
        ngxPermissionsOnlyThen: "ngxPermissionsOnlyThen",
        ngxPermissionsOnlyElse: "ngxPermissionsOnlyElse",
        ngxPermissionsExcept: "ngxPermissionsExcept",
        ngxPermissionsExceptElse: "ngxPermissionsExceptElse",
        ngxPermissionsExceptThen: "ngxPermissionsExceptThen",
        ngxPermissionsThen: "ngxPermissionsThen",
        ngxPermissionsElse: "ngxPermissionsElse",
        ngxPermissionsOnlyAuthorisedStrategy: "ngxPermissionsOnlyAuthorisedStrategy",
        ngxPermissionsOnlyUnauthorisedStrategy: "ngxPermissionsOnlyUnauthorisedStrategy",
        ngxPermissionsExceptUnauthorisedStrategy: "ngxPermissionsExceptUnauthorisedStrategy",
        ngxPermissionsExceptAuthorisedStrategy: "ngxPermissionsExceptAuthorisedStrategy",
        ngxPermissionsUnauthorisedStrategy: "ngxPermissionsUnauthorisedStrategy",
        ngxPermissionsAuthorisedStrategy: "ngxPermissionsAuthorisedStrategy"
      },
      outputs: {
        permissionsAuthorized: "permissionsAuthorized",
        permissionsUnauthorized: "permissionsUnauthorized"
      }
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsRestrictStubDirective, [{
    type: Directive,
    args: [{
      standalone: true,
      selector: "[ngxPermissionsOnly],[ngxPermissionsExcept]"
    }]
  }], null, {
    ngxPermissionsOnly: [{
      type: Input
    }],
    ngxPermissionsOnlyThen: [{
      type: Input
    }],
    ngxPermissionsOnlyElse: [{
      type: Input
    }],
    ngxPermissionsExcept: [{
      type: Input
    }],
    ngxPermissionsExceptElse: [{
      type: Input
    }],
    ngxPermissionsExceptThen: [{
      type: Input
    }],
    ngxPermissionsThen: [{
      type: Input
    }],
    ngxPermissionsElse: [{
      type: Input
    }],
    ngxPermissionsOnlyAuthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsOnlyUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsExceptUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsExceptAuthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsUnauthorisedStrategy: [{
      type: Input
    }],
    ngxPermissionsAuthorisedStrategy: [{
      type: Input
    }],
    permissionsAuthorized: [{
      type: Output
    }],
    permissionsUnauthorized: [{
      type: Output
    }]
  });
})();
var NgxPermissionsModule = class _NgxPermissionsModule {
  static forRoot(config = {}) {
    return {
      ngModule: _NgxPermissionsModule,
      providers: [NgxPermissionsStore, NgxRolesStore, NgxPermissionsConfigurationStore, NgxPermissionsService, NgxPermissionsGuard, NgxRolesService, NgxPermissionsConfigurationService, {
        provide: USE_PERMISSIONS_STORE,
        useValue: config.permissionsIsolate
      }, {
        provide: USE_ROLES_STORE,
        useValue: config.rolesIsolate
      }, {
        provide: USE_CONFIGURATION_STORE,
        useValue: config.configurationIsolate
      }]
    };
  }
  static forChild(config = {}) {
    return {
      ngModule: _NgxPermissionsModule,
      providers: [{
        provide: USE_PERMISSIONS_STORE,
        useValue: config.permissionsIsolate
      }, {
        provide: USE_ROLES_STORE,
        useValue: config.rolesIsolate
      }, {
        provide: USE_CONFIGURATION_STORE,
        useValue: config.configurationIsolate
      }, NgxPermissionsConfigurationService, NgxPermissionsService, NgxRolesService, NgxPermissionsGuard]
    };
  }
  static {
    this.ɵfac = function NgxPermissionsModule_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsModule)();
    };
  }
  static {
    this.ɵmod = ɵɵdefineNgModule({
      type: _NgxPermissionsModule,
      declarations: [NgxPermissionsDirective],
      exports: [NgxPermissionsDirective]
    });
  }
  static {
    this.ɵinj = ɵɵdefineInjector({});
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsModule, [{
    type: NgModule,
    args: [{
      imports: [],
      declarations: [NgxPermissionsDirective],
      exports: [NgxPermissionsDirective]
    }]
  }], null, null);
})();
var NgxPermissionsAllowStubModule = class _NgxPermissionsAllowStubModule {
  static {
    this.ɵfac = function NgxPermissionsAllowStubModule_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsAllowStubModule)();
    };
  }
  static {
    this.ɵmod = ɵɵdefineNgModule({
      type: _NgxPermissionsAllowStubModule,
      imports: [NgxPermissionsAllowStubDirective],
      exports: [NgxPermissionsAllowStubDirective]
    });
  }
  static {
    this.ɵinj = ɵɵdefineInjector({});
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsAllowStubModule, [{
    type: NgModule,
    args: [{
      imports: [NgxPermissionsAllowStubDirective],
      declarations: [],
      exports: [NgxPermissionsAllowStubDirective]
    }]
  }], null, null);
})();
var NgxPermissionsRestrictStubModule = class _NgxPermissionsRestrictStubModule {
  static {
    this.ɵfac = function NgxPermissionsRestrictStubModule_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NgxPermissionsRestrictStubModule)();
    };
  }
  static {
    this.ɵmod = ɵɵdefineNgModule({
      type: _NgxPermissionsRestrictStubModule,
      imports: [NgxPermissionsRestrictStubDirective],
      exports: [NgxPermissionsRestrictStubDirective]
    });
  }
  static {
    this.ɵinj = ɵɵdefineInjector({});
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NgxPermissionsRestrictStubModule, [{
    type: NgModule,
    args: [{
      imports: [NgxPermissionsRestrictStubDirective],
      declarations: [],
      exports: [NgxPermissionsRestrictStubDirective]
    }]
  }], null, null);
})();
export {
  DEFAULT_REDIRECT_KEY,
  NgxPermissionsAllowStubDirective,
  NgxPermissionsAllowStubModule,
  NgxPermissionsConfigurationService,
  NgxPermissionsConfigurationStore,
  NgxPermissionsDirective,
  NgxPermissionsGuard,
  NgxPermissionsModule,
  NgxPermissionsPredefinedStrategies,
  NgxPermissionsRestrictStubDirective,
  NgxPermissionsRestrictStubModule,
  NgxPermissionsService,
  NgxPermissionsStore,
  NgxRolesService,
  NgxRolesStore,
  USE_CONFIGURATION_STORE,
  USE_PERMISSIONS_STORE,
  USE_ROLES_STORE,
  ngxPermissionsGuard
};
//# sourceMappingURL=ngx-permissions.js.map
