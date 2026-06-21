package org.xiangqian.quick.deploy.service;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xiangqian.quick.deploy.util.DateTimeUtil;
import org.xiangqian.quick.deploy.util.UuidUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xiangqian
 * @date 2026/01/22 17:54
 */
@Slf4j
@Service
public class EmitterService {

    private Map<String, Set<Emitter>> map;

    public EmitterService() {
        map = new HashMap<>(16, 1f);
    }

    @SneakyThrows
    public SseEmitter create(String key) {
        Emitter emitter = new Emitter(key);

        Set<Emitter> value = null;
        synchronized (map) {
            value = map.get(key);
            if (value == null) {
                value = new HashSet<>();
                map.put(key, value);
            }
        }

        synchronized (value) {
            value.add(emitter);
        }

        emitter.send("test", Map.of("time", DateTimeUtil.format(LocalDateTime.now())));
        return emitter;
    }

    public void send(String key, String name, Object data) {
        Set<Emitter> value = null;
        synchronized (map) {
            value = map.get(key);
        }
        if (value == null) {
            return;
        }

        synchronized (value) {
            Iterator<Emitter> iterator = value.iterator();
            while (iterator.hasNext()) {
                Emitter emitter = iterator.next();
                try {
                    emitter.send(name, data);
                } catch (Exception e) {
//                    log.error("{} 发送异常", emitter, e);
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception ex) {
                    } finally {
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void sendAll(String name, Object data) {
//        log.debug("{}", this);
        Set<String> keys = null;
        synchronized (map) {
            if (MapUtils.isNotEmpty(map)) {
                keys = map.keySet().stream().collect(Collectors.toSet());
            }
        }
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }

        for (String key : keys) {
            send(key, name, data);
        }
    }

    public boolean isEmpty() {
        synchronized (map) {
            return map.isEmpty();
        }
    }

    @Override
    public String toString() {
        synchronized (map) {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            for (Map.Entry<String, Set<Emitter>> entry : map.entrySet()) {
                int size = CollectionUtils.size(entry.getValue());
                if (size > 0) {
                    if (builder.length() > 1) {
                        builder.append(",");
                    }
                    builder.append(entry.getKey()).append("=").append(size);
                }
            }
            builder.append("}");
            return builder.toString();
        }
    }

    @Getter
    private class Emitter extends SseEmitter {
        private final String uuid;
        private final String key;

        public Emitter(String key) {
            // 设置超时时间（0 表示永不超时）
            super(0L);

            this.uuid = UuidUtil.random();
            this.key = key;

            // 连接关闭时清理
            Runnable callback = () -> {
                Set<Emitter> value = null;
                synchronized (map) {
                    value = map.get(Emitter.this.getKey());
                }
                if (value != null) {
                    synchronized (value) {
                        value.remove(Emitter.this);
                    }
                }
            };
            onCompletion(callback);
            onTimeout(callback);
        }

        public void send(String name, Object data) throws Exception {
            send(SseEmitter.event().name(name).data(data));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Emitter that = (Emitter) o;
            return Objects.equals(uuid, that.uuid) && Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, key);
        }

        @Override
        public String toString() {
            return "Emitter(uuid=" + uuid + ", key=" + key + ")";
        }
    }

}
