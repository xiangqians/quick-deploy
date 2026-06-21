package org.xiangqian.quick.deploy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiangqian
 * @date 2026/05/07 17:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    private String code;
    private String label;
    private String detail;
}
