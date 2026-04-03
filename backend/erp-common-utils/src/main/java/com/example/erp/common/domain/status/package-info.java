/**
 * Domain layer package for business status codes.
 * 
 * <h2>Architecture Rules</h2>
 * <ul>
 *   <li>No HTTP dependencies (no Spring Web imports)</li>
 *   <li>Status enum is the single source of truth for business codes</li>
 *   <li>Service layer uses StatusCode, not HTTP status codes</li>
 *   <li>HTTP mapping exists ONLY in web layer (OperationCodeImpl)</li>
 * </ul>
 * 
 * <h2>Package Contents</h2>
 * <ul>
 *   <li>{@link com.example.erp.common.domain.status.StatusCategory} - Categorization enum</li>
 *   <li>{@link com.example.erp.common.domain.status.StatusCode} - Contract interface</li>
 *   <li>{@link com.example.erp.common.domain.status.Status} - Single source of truth enum</li>
 *   <li>{@link com.example.erp.common.domain.status.ServiceResult} - Service layer result wrapper</li>
 * </ul>
 * 
 * <h2>Usage in Service Layer</h2>
 * <pre>
 * public ServiceResult&lt;CustomerDto&gt; findCustomer(Long id) {
 *     return customerRepository.findById(id)
 *         .map(c -&gt; ServiceResult.success(mapper.toDto(c)))
 *         .orElse(ServiceResult.notFound("Customer not found"));
 * }
 * </pre>
 * 
 * @author ERP Team
 */
package com.example.erp.common.domain.status;
