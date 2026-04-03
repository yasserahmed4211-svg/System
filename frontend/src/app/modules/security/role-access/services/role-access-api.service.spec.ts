import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RoleAccessApiService } from './role-access-api.service';

describe('RoleAccessApiService', () => {
  let service: RoleAccessApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(RoleAccessApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('maps roles list to RoleDto shape', () => {
    service.getRoles({ search: 'adm', page: 0, size: 20, sort: 'roleName,asc' }).subscribe((resp) => {
      expect(resp.totalElements).toBe(1);
      expect(resp.content.length).toBe(1);
      expect(resp.content[0]).toEqual(
        jasmine.objectContaining({
          id: 10,
          roleCode: 'ROLE_ADMIN',
          roleName: 'Admin',
          active: true
        })
      );
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/roles') && r.method === 'GET');
    const url = new URL(req.request.urlWithParams, 'http://localhost');
    expect(url.searchParams.get('search')).toBe('adm');
    expect(url.searchParams.get('page')).toBe('0');
    expect(url.searchParams.get('size')).toBe('20');
    expect(url.searchParams.get('sort')).toBe('roleName,asc');

    req.flush({
      content: [{ id: 10, roleCode: 'ROLE_ADMIN', roleName: 'Admin', active: true }],
      totalElements: 1,
      totalPages: 1
    });
  });

  it('maps role pages permissions[] into boolean flags', () => {
    service.getRolePages(5).subscribe((resp) => {
      expect(resp.roleId).toBe(5);
      expect(resp.assignments).toEqual([
        { pageCode: 'USER', create: true, update: false, delete: true }
      ]);
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/roles/5/pages') && r.method === 'GET');
    req.flush({
      roleId: 5,
      assignments: [{ pageCode: 'USER', permissions: ['CREATE', 'DELETE'] }]
    });
  });

  // ============================================================
  // ERROR HANDLING TESTS (HTTP 422, 400, 404)
  // ============================================================

  it('handles 422 unprocessable entity with clear error', (done) => {
    service.getRoles({ page: 0, size: 20, sort: 'roleName,asc' }).subscribe({
      next: () => fail('should have errored'),
      error: (err) => {
        expect(err.status).toBe(422);
        expect(err.error).toBeTruthy();
        expect(err.error.error.code).toBe('INVALID_SORT_FIELD');
        done();
      }
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/roles'));
    req.flush(
      {
        success: false,
        message: 'Invalid sort field',
        error: {
          code: 'INVALID_SORT_FIELD',
          details: 'Invalid sort fields: invalidField. Allowed fields: id, roleCode, roleName, createdAt, updatedAt, active'
        }
      },
      { status: 422, statusText: 'Unprocessable Entity' }
    );
  });

  it('handles 400 bad request with validation errors', (done) => {
    service.createRole({ roleCode: '', roleName: '', active: true }).subscribe({
      next: () => fail('should have errored'),
      error: (err) => {
        expect(err.status).toBe(400);
        expect(err.error.error.fieldErrors).toBeDefined();
        done();
      }
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/roles') && r.method === 'POST');
    req.flush(
      {
        success: false,
        message: 'Validation failed',
        error: {
          code: 'VALIDATION_ERROR',
          fieldErrors: [
            { field: 'roleCode', message: 'Role code is required' },
            { field: 'roleName', message: 'Role name is required' }
          ]
        }
      },
      { status: 400, statusText: 'Bad Request' }
    );
  });

  it('handles 404 not found error', (done) => {
    service.getRoleById(999).subscribe({
      next: () => fail('should have errored'),
      error: (err) => {
        expect(err.status).toBe(404);
        expect(err.error.error.code).toBe('NOT_FOUND');
        done();
      }
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/roles/999'));
    req.flush(
      {
        success: false,
        message: 'Role not found',
        error: { code: 'NOT_FOUND', details: 'Role with id 999 not found' }
      },
      { status: 404, statusText: 'Not Found' }
    );
  });

  it('handles network error gracefully', (done) => {
    service.getRoles({ page: 0, size: 20 }).subscribe({
      next: () => fail('should have errored'),
      error: (err) => {
        expect(err.status).toBe(0);
        done();
      }
    });

    const req = httpMock.expectOne((r) => r.url.includes('/api/roles'));
    req.error(new ProgressEvent('error'));
  });
});