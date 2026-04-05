// Angular import
import { AfterViewInit, Component, ElementRef, OnInit, effect, viewChild, output, inject, ChangeDetectorRef, DestroyRef, untracked } from '@angular/core';
import { CommonModule, Location, LocationStrategy } from '@angular/common';
import { RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// project import
import { NavigationItem, NavigationItems } from '../navigation';
import { MantisConfig } from 'src/app/app-config';
import { environment } from 'src/environments/environment';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { NavCollapseComponent } from './nav-collapse/nav-collapse.component';
import { NavGroupComponent } from './nav-group/nav-group.component';
import { NavItemComponent } from './nav-item/nav-item.component';
import { AuthenticationService } from 'src/app/core/services/authentication.service';
import { MenuService } from 'src/app/theme/shared/service/menu.service';
import { LanguageService } from 'src/app/core/services/language.service';
import { Role } from 'src/app/theme/shared/components/_helpers/role';
import { ScrollbarComponent } from 'src/app/theme/shared/components/scrollbar/scrollbar.component';

// icon
import { IconService } from '@ant-design/icons-angular';
import { 
  ChromeOutline, 
  RightOutline,
  UserOutline,
  TeamOutline,
  MenuOutline,
  FileOutline,
  SafetyOutline,
  SettingOutline,
  LockOutline,
  KeyOutline,
  AppstoreOutline,
  DatabaseOutline,
  FolderOutline,
  HomeOutline,
  DashboardOutline,
  FormOutline,
  TableOutline,
  ProfileOutline,
  ToolOutline,
  AuditOutline,
  BankOutline,
  BookOutline,
  CalendarOutline,
  CloudOutline,
  CodeOutline,
  CreditCardOutline,
  CustomerServiceOutline,
  EnvironmentOutline,
  FileTextOutline,
  FundOutline,
  GlobalOutline,
  IdcardOutline,
  MailOutline,
  MobileOutline,
  MonitorOutline,
  PieChartOutline,
  PrinterOutline,
  ProjectOutline,
  ReadOutline,
  ReconciliationOutline,
  SafetyCertificateOutline,
  ScheduleOutline,
  ShopOutline,
  ShoppingCartOutline,
  SolutionOutline,
  TagsOutline,
  TransactionOutline,
  WalletOutline,
  // Additional icons for mapping support
  UnlockOutline,
  DollarOutline,
  BarChartOutline,
  LineChartOutline,
  SearchOutline,
  PlusOutline,
  MinusOutline,
  EditOutline,
  DeleteOutline,
  SaveOutline,
  CheckOutline,
  CloseOutline,
  InfoCircleOutline,
  WarningOutline,
  CloseCircleOutline,
  CheckCircleOutline,
  QuestionCircleOutline,
  PhoneOutline,
  MessageOutline,
  CommentOutline,
  BellOutline,
  ClockCircleOutline,
  HistoryOutline,
  ArrowRightOutline,
  ArrowLeftOutline,
  ArrowUpOutline,
  ArrowDownOutline,
  StarOutline,
  HeartOutline,
  TagOutline,
  UnorderedListOutline,
  LinkOutline,
  CopyOutline,
  SnippetsOutline,
  FilterOutline,
  SortAscendingOutline,
  ExportOutline,
  ImportOutline,
  DownloadOutline,
  UploadOutline,
  ReloadOutline,
  SyncOutline,
  EyeOutline,
  EyeInvisibleOutline,
  ApiOutline,
  LayoutOutline
} from '@ant-design/icons-angular/icons';

@Component({
  selector: 'app-nav-content',
  imports: [SharedModule, CommonModule, RouterModule, NavCollapseComponent, NavGroupComponent, NavItemComponent, ScrollbarComponent],
  templateUrl: './nav-content.component.html',
  styleUrls: ['./nav-content.component.scss']
})
export class NavContentComponent implements AfterViewInit, OnInit {
  authenticationService = inject(AuthenticationService);
  private menuService = inject(MenuService);
  languageService = inject(LanguageService);
  private location = inject(Location);
  private locationStrategy = inject(LocationStrategy);
  private iconService = inject(IconService);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);

  // After Media 1025 menu Open In Use
  readonly NavCollapsedMob = output();

  // for Compact Menu
  readonly SubmenuCollapse = output();

  // Theme version
  title = 'Application for version numbering';
  currentApplicationVersion = environment.appVersion;

  mantisConfig = MantisConfig;
  layout!: string;
  navigation!: NavigationItem[];
  prevDisabled: string;
  nextDisabled: string;
  contentWidth: number;
  wrapperWidth!: number;
  scrollWidth: number;
  windowWidth: number;
  collapseItem!: NavigationItem;

  readonly navbarContent = viewChild.required<ElementRef>('navbarContent');
  readonly navbarWrapper = viewChild.required<ElementRef>('navbarWrapper');

  // Constructor
  constructor() {
    this.windowWidth = window.innerWidth;
    // Register all icons that might be used in navigation
    this.iconService.addIcon(
      ChromeOutline, 
      RightOutline,
      UserOutline,
      TeamOutline,
      MenuOutline,
      FileOutline,
      SafetyOutline,
      SettingOutline,
      LockOutline,
      KeyOutline,
      AppstoreOutline,
      DatabaseOutline,
      FolderOutline,
      HomeOutline,
      DashboardOutline,
      FormOutline,
      TableOutline,
      ProfileOutline,
      ToolOutline,
      AuditOutline,
      BankOutline,
      BookOutline,
      CalendarOutline,
      CloudOutline,
      CodeOutline,
      CreditCardOutline,
      CustomerServiceOutline,
      EnvironmentOutline,
      FileTextOutline,
      FundOutline,
      GlobalOutline,
      IdcardOutline,
      MailOutline,
      MobileOutline,
      MonitorOutline,
      PieChartOutline,
      PrinterOutline,
      ProjectOutline,
      ReadOutline,
      ReconciliationOutline,
      SafetyCertificateOutline,
      ScheduleOutline,
      ShopOutline,
      ShoppingCartOutline,
      SolutionOutline,
      TagsOutline,
      TransactionOutline,
      WalletOutline,
      // Additional icons for mapping support
      UnlockOutline,
      DollarOutline,
      BarChartOutline,
      LineChartOutline,
      SearchOutline,
      PlusOutline,
      MinusOutline,
      EditOutline,
      DeleteOutline,
      SaveOutline,
      CheckOutline,
      CloseOutline,
      InfoCircleOutline,
      WarningOutline,
      CloseCircleOutline,
      CheckCircleOutline,
      QuestionCircleOutline,
      PhoneOutline,
      MessageOutline,
      CommentOutline,
      BellOutline,
      ClockCircleOutline,
      HistoryOutline,
      ArrowRightOutline,
      ArrowLeftOutline,
      ArrowUpOutline,
      ArrowDownOutline,
      StarOutline,
      HeartOutline,
      TagOutline,
      UnorderedListOutline,
      LinkOutline,
      CopyOutline,
      SnippetsOutline,
      FilterOutline,
      SortAscendingOutline,
      ExportOutline,
      ImportOutline,
      DownloadOutline,
      UploadOutline,
      ReloadOutline,
      SyncOutline,
      EyeOutline,
      EyeInvisibleOutline,
      ApiOutline,
      LayoutOutline
    );
    this.prevDisabled = 'disabled';
    this.nextDisabled = '';
    this.scrollWidth = 0;
    this.contentWidth = 0;
    
    // Reload menu when language changes
    effect(() => {
      const currentLang = this.languageService.currentLanguage();
      // Reload menu to update titles based on new language
      // Use untracked to avoid triggering effect from loadUserMenu side effects
      untracked(() => this.loadUserMenu());
    });
  }

  ngOnInit() {
    this.layout = MantisConfig.layout;
    this.loadUserMenu();
  }

  private loadUserMenu() {
    const token = this.authenticationService.getToken();
    
    if (!this.authenticationService.isLoggedIn() || !token) {
      const currentUser = this.authenticationService.currentUserValue;
      const userRoles = currentUser?.roles && currentUser.roles.length > 0 ? currentUser.roles : [Role.Admin];
      this.navigation = this.filterMenu(NavigationItems, userRoles);
      this.cdr.markForCheck();
      return;
    }
    
    this.menuService.getUserMenu().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (menuItems) => {
        if (menuItems && menuItems.length > 0) {
          this.navigation = this.menuService.groupMenuItems(menuItems);
        } else {
          const currentUser = this.authenticationService.currentUserValue;
          const userRoles = currentUser?.roles && currentUser.roles.length > 0 ? currentUser.roles : [Role.Admin];
          this.navigation = this.filterMenu(NavigationItems, userRoles);
        }
        this.cdr.markForCheck();
      },
      error: (error) => {
        const currentUser = this.authenticationService.currentUserValue;
        const userRoles = currentUser?.roles && currentUser.roles.length > 0 ? currentUser.roles : [Role.Admin];
        this.navigation = this.filterMenu(NavigationItems, userRoles);
        this.cdr.markForCheck();
      }
    });
  }

  filterMenu(NavigationItems: NavigationItem[], userRoles: string[], parentRoles: string[] = [Role.Admin]): NavigationItem[] {
    return NavigationItems.map((item) => {
      // If item doesn't have a specific role, inherit roles from parent
      const itemRoles = item.role ? item.role : parentRoles;

      // If item has children, recursively filter them, passing current item's roles as parentRoles
      if (item.children) {
        item.children = this.filterMenu(item.children, userRoles, itemRoles);
      }

      return item; // Return the item whether it is visible or disabled
    });
  }

  // public method
  ngAfterViewInit() {
    if (MantisConfig.layout === 'horizontal') {
      this.contentWidth = this.navbarContent().nativeElement.clientWidth;
      this.wrapperWidth = this.navbarWrapper().nativeElement.clientWidth;
    }
  }

  // Horizontal Menu
  scrollPlus() {
    this.scrollWidth = this.scrollWidth + (this.wrapperWidth - 200);
    if (this.scrollWidth > this.contentWidth - this.wrapperWidth) {
      this.scrollWidth = this.contentWidth - this.wrapperWidth + 200;
      this.nextDisabled = 'disabled';
    }
    this.prevDisabled = '';
    const el = document.querySelector('#side-nav-horizontal') as HTMLElement;
    if (this.languageService.isRTL()) {
      el.style.marginRight = '-' + this.scrollWidth + 'px';
      el.style.marginLeft = '';
    } else {
      el.style.marginLeft = '-' + this.scrollWidth + 'px';
      el.style.marginRight = '';
    }
  }

  scrollMinus() {
    this.scrollWidth = this.scrollWidth - this.wrapperWidth;
    if (this.scrollWidth < 0) {
      this.scrollWidth = 0;
      this.prevDisabled = 'disabled';
    }
    this.nextDisabled = '';
    const el = document.querySelector('#side-nav-horizontal') as HTMLElement;
    if (this.languageService.isRTL()) {
      el.style.marginRight = '-' + this.scrollWidth + 'px';
      el.style.marginLeft = '';
    } else {
      el.style.marginLeft = '-' + this.scrollWidth + 'px';
      el.style.marginRight = '';
    }
  }

  fireLeave() {
    const sections = document.querySelectorAll('.coded-hasmenu');
    for (let i = 0; i < sections.length; i++) {
      sections[i].classList.remove('active');
      sections[i].classList.remove('coded-trigger');
    }

    let current_url = this.location.path();
    // eslint-disable-next-line
    // @ts-ignore
    if (this.location['_baseHref']) {
      // eslint-disable-next-line
      // @ts-ignore
      current_url = this.location['_baseHref'] + this.location.path();
    }
    const link = "a.nav-link[ href='" + current_url + "' ]";
    const ele = document.querySelector(link);
    if (ele !== null && ele !== undefined) {
      const parent = ele.parentElement;
      const up_parent = parent?.parentElement?.parentElement;
      const last_parent = up_parent?.parentElement;
      if (parent?.classList.contains('coded-hasmenu')) {
        parent.classList.add('active');
      } else if (up_parent?.classList.contains('coded-hasmenu')) {
        up_parent.classList.add('active');
      } else if (last_parent?.classList.contains('coded-hasmenu')) {
        last_parent.classList.add('active');
      }
    }
  }

  fireOutClick() {
    let current_url = this.location.path();
    const baseHref = this.locationStrategy.getBaseHref();
    if (baseHref) {
      current_url = baseHref + this.location.path();
    }
    const link = "a.nav-link[ href='" + current_url + "' ]";
    const ele = document.querySelector(link);
    if (ele !== null && ele !== undefined) {
      const parent = ele.parentElement;
      const up_parent = parent?.parentElement?.parentElement;
      const last_parent = up_parent?.parentElement;
      if (parent?.classList.contains('coded-hasmenu')) {
        if (MantisConfig.layout === 'vertical') {
          parent.classList.add('coded-trigger');
        }
        parent.classList.add('active');
      } else if (up_parent?.classList.contains('coded-hasmenu')) {
        if (MantisConfig.layout === 'vertical') {
          up_parent.classList.add('coded-trigger');
        }
        up_parent.classList.add('active');
      } else if (last_parent?.classList.contains('coded-hasmenu')) {
        if (MantisConfig.layout === 'vertical') {
          last_parent.classList.add('coded-trigger');
        }
        last_parent.classList.add('active');
      }
    }
  }

  logout() {
    this.authenticationService.logout();
  }

  navMob() {
    if (this.windowWidth < 1025 && document.querySelector('app-navigation.pc-sidebar')?.classList.contains('mob-open')) {
      this.NavCollapsedMob.emit();
    }
  }

  subMenuCollapse(item: NavigationItem) {
    this.SubmenuCollapse.emit();
    this.collapseItem = item;
  }
}
