package org.xiangqian.quick.deploy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiangqian
 * @date 2026/05/07 17:43
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDetail {
    // 概述
    private String summary;
    // 详情
    private String detail;
}
