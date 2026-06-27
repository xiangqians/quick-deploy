package org.xiangqian.quick.deploy.util;

import lombok.Getter;
import lombok.Setter;
import org.xiangqian.quick.deploy.model.Group;
import org.xiangqian.quick.deploy.model.User;

import java.util.Collections;

/**
 * @author xiangqian
 * @date 2026/06/27 16:08
 */
@Getter
public class SecurityUser extends org.springframework.security.core.userdetails.User {
    @Setter
    private Group group;

    private User user;

    public SecurityUser(User user) {
        super(user.getName(), user.getPasswd(), Collections.emptyList());
        this.user = user;
    }

    public String getNick() {
        return user.getNick();
    }

    public String getName() {
        return user.getName();
    }
}
