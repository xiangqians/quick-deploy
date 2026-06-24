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
import org.xiangqian.quick.deploy.util.YamlUtil;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        users = Stream.concat(YamlUtil.deser(Path.of(dir, "user.yml").toFile(), new TypeReference<List<User>>() {
                        }).stream(),
                        Stream.of(User.builder().nick("Webhook").name("webhook").passwd("webhook").build()))
                .map(user -> {
                    user.setPasswd(passwordEncoder.encode(user.getPasswd()));
                    return user;
                })
                .collect(Collectors.toMap(User::getName, Function.identity()));
    }

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        User user = null;
        name = StringUtils.trim(name);
        if ("webhook".equals(name) || (user = users.get(name)) == null) {
            throw new UsernameNotFoundException("user '" + name + "' not found");
        }
        return new org.springframework.security.core.userdetails.User(user.getName(), user.getPasswd(), Collections.emptyList());
    }

    public User getByName(String name) {
        return users.get(name);
    }

}
