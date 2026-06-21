package org.xiangqian.quick.deploy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 用户配置
 *
 * @author xiangqian
 * @date 2026/03/04 11:19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // 昵称
    private String nick;
    // 用户名
    private String name;
    // 密码
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwd;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return String.format("%s（%s）", nick, name);
    }
}
