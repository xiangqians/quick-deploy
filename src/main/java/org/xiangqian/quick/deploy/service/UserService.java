package org.xiangqian.quick.deploy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.xiangqian.quick.deploy.model.User;
import org.xiangqian.quick.deploy.util.SecurityUser;
import org.xiangqian.quick.deploy.util.YamlUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xiangqian
 * @date 2026/03/04 11:18
 */
@Service
public class UserService implements UserDetailsService {

    @Value("${dir}")
    private String dir;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Map<String, User> users;

    @SneakyThrows
    @PostConstruct
    public void init() {
        users = YamlUtil.deser(Path.of(dir, "user.yml").toFile(), new TypeReference<List<User>>() {
                }).stream()
                .map(user -> {
                    user.setPasswd(passwordEncoder.encode(user.getPasswd()));
                    return user;
                })
                .collect(Collectors.toMap(User::getName, Function.identity()));

        User webhook = User.WEBHOOK;
        webhook.setPasswd(passwordEncoder.encode(webhook.getPasswd()));
        users.put(webhook.getName(), webhook);
    }

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        User user = null;
        name = StringUtils.trim(name);
        if (User.WEBHOOK.getName().equals(name) || (user = users.get(name)) == null) {
            throw new UsernameNotFoundException("user '" + name + "' not found");
        }
        return new SecurityUser(user);
    }
}
