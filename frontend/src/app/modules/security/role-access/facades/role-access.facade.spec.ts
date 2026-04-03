import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RoleAccessFacade, RoleSearchFilter } from './role-access.facade';
import { RoleAccessApiService } from '../services/role-access-api.service';
import { ErpErrorMapperService } from 'src/app/shared/services/erp-error-mapper.service';

class MockRoleAccessApiService {
  getRoles = jasmine.createSpy('getRoles').and.returnValue(of({ content: [], totalElements: 0, totalPages: 0 }));
  createRole = jasmine.createSpy('createRole');
  toggleRoleActive = jasmine.createSpy('toggleRoleActive');
  deleteRole = jasmine.createSpy('deleteRole');
  getRoleById = jasmine.createSpy('getRoleById');
  getActivePages = jasmine.createSpy('getActivePages');
  getRolePages = jasmine.createSpy('getRolePages');
  addPageToRole = jasmine.createSpy('addPageToRole');
  syncRolePages = jasmine.createSpy('syncRolePages');
  removeRolePage = jasmine.createSpy('removeRolePage');
  copyFromRole = jasmine.createSpy('copyFromRole');
}

class MockErpErrorMapperService {
  hasMapping(): boolean {
    return false;
  }
  mapError(): any {
    return { translationKey: 'ERRORS.OPERATION_FAILED' };
  }
}

describe('RoleAccessFacade', () => {
  let facade: RoleAccessFacade;
  let api: MockRoleAccessApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RoleAccessFacade,
        { provide: RoleAccessApiService, useClass: MockRoleAccessApiService },
        { provide: ErpErrorMapperService, useClass: MockErpErrorMapperService }
      ]
    });

    facade = TestBed.inject(RoleAccessFacade);
    api = TestBed.inject(RoleAccessApiService) as unknown as MockRoleAccessApiService;
  });

  it('passes search when operator is LIKE', () => {
    const filters: RoleSearchFilter[] = [{ field: 'search', op: 'LIKE', value: 'adm' }];
    facade.setFilters(filters);

    facade.loadRoles();

    expect(api.getRoles).toHaveBeenCalled();
    const args = api.getRoles.calls.mostRecent().args[0];
    expect(args.search).toBe('adm');
  });

  it('passes search when operator is EQ (was previously ignored)', () => {
    const filters: RoleSearchFilter[] = [{ field: 'search', op: 'EQ', value: 'ROLE_ADMIN' }];
    facade.setFilters(filters);

    facade.loadRoles();

    expect(api.getRoles).toHaveBeenCalled();
    const args = api.getRoles.calls.mostRecent().args[0];
    expect(args.search).toBe('ROLE_ADMIN');
  });

  it('prefers EQ over LIKE when both are present', () => {
    const filters: RoleSearchFilter[] = [
      { field: 'search', op: 'LIKE', value: 'adm' },
      { field: 'search', op: 'EQ', value: 'ROLE_ADMIN' }
    ];
    facade.setFilters(filters);

    facade.loadRoles();

    const args = api.getRoles.calls.mostRecent().args[0];
    expect(args.search).toBe('ROLE_ADMIN');
  });
});
