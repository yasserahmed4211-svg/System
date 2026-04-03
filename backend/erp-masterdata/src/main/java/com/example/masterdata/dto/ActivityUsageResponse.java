package com.example.masterdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Activity Usage Response DTO
 * 
 * Shows where the activity is being used
 * 
 * @author ERP Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityUsageResponse {

    private Long activityId;
    private String activityCode;
    private String activityName;
    private long totalCategories;
    private long activeCategories;
    private boolean canDelete;
    private boolean canDeactivate;
    private String message;

    public static ActivityUsageResponse from(Long activityId, String code, String name, 
                                             long totalCategories, long activeCategories) {
        boolean canDelete = totalCategories == 0;
        boolean canDeactivate = activeCategories == 0;
        
        String message;
        if (!canDelete) {
            message = String.format("Activity is used by %d categories. Cannot delete.", totalCategories);
        } else if (!canDeactivate) {
            message = String.format("Activity has %d active categories. Cannot deactivate.", activeCategories);
        } else {
            message = "Activity can be deleted or deactivated.";
        }

        return ActivityUsageResponse.builder()
            .activityId(activityId)
            .activityCode(code)
            .activityName(name)
            .totalCategories(totalCategories)
            .activeCategories(activeCategories)
            .canDelete(canDelete)
            .canDeactivate(canDeactivate)
            .message(message)
            .build();
    }
}
