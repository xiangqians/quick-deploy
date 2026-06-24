package org.xiangqian.quick.deploy.model;

import lombok.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组信息
 *
 * @author xiangqian
 * @date 2026/06/24 20:54
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    // 唯一标识
    private String id;
    // 名称
    private String name;
    // 项目列表
    @Getter(AccessLevel.NONE)
    private Map<String, Proj> projs;

    public void setProjs(List<Proj> projs) {
        this.projs = projs.stream()
                .collect(Collectors.toMap(Proj::getId, Function.identity(), (oldProj, newProj) -> {
                    throw new IllegalStateException(String.format("Duplicate project ids [{id=%s, name=%s}, {id=%s, name=%s}]",
                            oldProj.getId(), oldProj.getName(),
                            newProj.getId(), newProj.getName()));
                }, LinkedHashMap::new));
    }

    public Proj getProj(String id) {
        return projs.get(id);
    }

    public Collection<Proj> getProjs() {
        return projs.values();
    }

    public Group copy() {
        return Group.builder()
                .id(id)
                .name(name)
                .build();
    }
}
