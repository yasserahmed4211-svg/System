package com.example.erp.finance.gl.service;

import com.example.erp.common.domain.status.Status;
import com.example.erp.common.exception.LocalizedException;
import com.example.erp.finance.gl.exception.GlErrorCodes;
import com.example.erp.finance.gl.repository.AccountsChartRepository;
import com.example.masterdata.api.LookupValidationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Generates hierarchical ACCOUNT_CHART_NO values based on ERP accounting standards.
 *
 * <h3>Root Level Numbering</h3>
 * Root accounts use the {@code SORT_ORDER} from the {@code GL_ACCOUNT_TYPE} lookup
 * as a numeric prefix followed by a sequential digit.
 * For example, with ASSET (sortOrder=1):
 * <ul>
 *   <li>1x = Asset   → first root "10", next "11", "12", ...</li>
 *   <li>2x = Liability → first root "20", next "21", "22", ...</li>
 * </ul>
 * This prevents cross-type code collisions under the UNIQUE(code, org) constraint.
 *
 * <h3>Child Level Numbering</h3>
 * Child accounts append a two-digit sequential segment to the parent code:
 * <pre>
 *   Parent "10" → children "1001", "1002", "1003", ...
 *   Parent "1003" → children "100301", "100302", ...
 * </pre>
 *
 * @author ERP Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountChartNumberGenerator {

    private final AccountsChartRepository accountsChartRepository;
    private final LookupValidationApi lookupValidationApi;

    private static final String LK_GL_ACCOUNT_TYPE = "GL_ACCOUNT_TYPE";

    /** Segment width for child numbering (2 digits → 01..99) */
    private static final int CHILD_SEGMENT_WIDTH = 2;

    /**
     * Generate the next ACCOUNT_CHART_NO for a root-level account.
     * Uses the SORT_ORDER from the GL_ACCOUNT_TYPE lookup as prefix + sequential digit.
     * For example: ASSET (sortOrder=1) → "10", "11", "12"...
     *
     * @param organizationFk organization to scope the numbering
     * @param accountType    account type code from GL_ACCOUNT_TYPE lookup (e.g., "ASSET", "LIABILITY", ...)
     * @return generated account number
     */
    public String generateRootAccountNo(Long organizationFk, String accountType) {
        validateAccountType(accountType);

        // Resolve the numeric prefix from SORT_ORDER in the lookup
        String prefix = resolveAccountTypePrefix(accountType);

        // Search ALL accounts in the org matching the root code pattern (prefix + 1 digit)
        int expectedLen = prefix.length() + 1;
        String likePattern = prefix + "%";
        Optional<String> maxCode = accountsChartRepository.findMaxAccountNoByPrefixAndLength(
                organizationFk, likePattern, expectedLen);

        if (maxCode.isEmpty() || maxCode.get() == null) {
            // First root of this type → prefix + "0" (e.g., "10" for Assets)
            String code = prefix + "0";
            log.debug("First root account for type={}, org={} → code={}", accountType, organizationFk, code);
            return code;
        }

        String currentMax = maxCode.get();

        // Extract the sequence part after the type prefix
        if (currentMax.length() < prefix.length() + 1) {
            // Legacy single-digit code (e.g., "1") — next code is prefix + "0"
            String code = prefix + "0";
            log.debug("Root account generation (legacy upgrade): type={}, org={}, maxExisting={}, next={}",
                    accountType, organizationFk, currentMax, code);
            return code;
        }

        try {
            // Parse the sequence digit(s) after the prefix
            String seqPart = currentMax.substring(prefix.length());
            int currentSeq = Integer.parseInt(seqPart);
            int nextSeq = currentSeq + 1;
            if (nextSeq > 9) {
                throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                        GlErrorCodes.GL_ACCOUNT_NO_GENERATION_FAILED,
                        "Maximum root accounts (10) reached for type " + accountType);
            }
            String nextCode = prefix + nextSeq;
            log.debug("Root account generation: type={}, org={}, maxExisting={}, next={}",
                    accountType, organizationFk, currentMax, nextCode);
            return nextCode;
        } catch (NumberFormatException e) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                    GlErrorCodes.GL_ACCOUNT_NO_GENERATION_FAILED,
                    "Cannot parse root code sequence for type " + accountType + ": " + currentMax);
        }
    }

    /**
     * Generate the next ACCOUNT_CHART_NO for a child account under the given parent.
     * Searches ALL accounts in the organization matching the prefix pattern and expected length
     * to avoid cross-hierarchy collisions.
     *
     * @param parentAccountNo the parent's ACCOUNT_CHART_NO
     * @param parentPk        the parent's primary key
     * @param organizationFk  the organization to scope the numbering
     * @return generated account number
     */
    public String generateChildAccountNo(String parentAccountNo, Long parentPk, Long organizationFk) {
        // Search ALL accounts in the org matching parentCode + CHILD_SEGMENT_WIDTH pattern
        int expectedLen = parentAccountNo.length() + CHILD_SEGMENT_WIDTH;
        String likePattern = parentAccountNo + "%";
        Optional<String> maxChild = accountsChartRepository.findMaxAccountNoByPrefixAndLength(
                organizationFk, likePattern, expectedLen);

        String nextCode;
        if (maxChild.isEmpty() || maxChild.get() == null) {
            // No children yet → start from parentCode + "01"
            nextCode = parentAccountNo + padSegment(1);
        } else {
            String currentMax = maxChild.get();
            // Extract the child segment (last CHILD_SEGMENT_WIDTH digits)
            if (currentMax.length() <= parentAccountNo.length()) {
                // Edge case: max child code equals parent code length, start from 01
                nextCode = parentAccountNo + padSegment(1);
            } else {
                String childSegment = currentMax.substring(parentAccountNo.length());
                try {
                    int currentSeq = Integer.parseInt(childSegment);
                    int nextSeq = currentSeq + 1;
                    // Validate we haven't exceeded segment capacity
                    int maxValue = (int) Math.pow(10, CHILD_SEGMENT_WIDTH) - 1;
                    if (nextSeq > maxValue) {
                        throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                                GlErrorCodes.GL_ACCOUNT_NO_GENERATION_FAILED,
                                "Maximum child accounts reached for parent " + parentAccountNo);
                    }
                    nextCode = parentAccountNo + padSegment(nextSeq);
                } catch (NumberFormatException e) {
                    throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                            GlErrorCodes.GL_ACCOUNT_NO_GENERATION_FAILED,
                            "Cannot parse child segment for parent " + parentAccountNo);
                }
            }
        }

        log.debug("Child account generation: parent={}, parentPk={}, next={}", parentAccountNo, parentPk, nextCode);
        return nextCode;
    }

    /**
     * Resolve the numeric prefix for an account type by fetching its SORT_ORDER from the lookup.
     * For example: "ASSET" → sortOrder 1 → prefix "1", "LIABILITY" → sortOrder 2 → prefix "2".
     *
     * @param accountType account type code (e.g., "ASSET")
     * @return numeric prefix string derived from SORT_ORDER
     */
    private String resolveAccountTypePrefix(String accountType) {
        return lookupValidationApi.getSortOrder(LK_GL_ACCOUNT_TYPE, accountType)
                .map(String::valueOf)
                .orElseThrow(() -> new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                        GlErrorCodes.GL_INVALID_ROOT_ACCOUNT_TYPE,
                        "Cannot resolve sort order for account type: " + accountType));
    }

    /**
     * Validate that the account type is a valid code in the GL_ACCOUNT_TYPE lookup.
     */
    public void validateAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            throw new LocalizedException(Status.BAD_REQUEST,
                    GlErrorCodes.GL_INVALID_ROOT_ACCOUNT_TYPE, accountType);
        }
        lookupValidationApi.validateOrThrow(LK_GL_ACCOUNT_TYPE, accountType);
    }

    // ==================== Private Helpers ====================

    /**
     * Pad a sequence number to the standard child segment width.
     * Example: padSegment(1) → "01", padSegment(12) → "12"
     */
    private String padSegment(int seq) {
        return String.format("%0" + CHILD_SEGMENT_WIDTH + "d", seq);
    }
}
