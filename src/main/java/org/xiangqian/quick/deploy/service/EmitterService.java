package org.xiangqian.quick.deploy.service;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xiangqian.quick.deploy.comp.ThreadPool;
import org.xiangqian.quick.deploy.util.DateTimeUtil;
import org.xiangqian.quick.deploy.util.UuidUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author xiangqian
 * @date 2026/01/22 17:54
 */
@Slf4j
@Service
public class EmitterService {

    @Autowired
    private ThreadPool threadPool;

    private Map<String, Set<Emitter>> map;

    public EmitterService() {
        map = new HashMap<>(16, 1f);
    }

    @SneakyThrows
    public SseEmitter create(String key, String groupId) {
        Emitter emitter = new Emitter(key, groupId);

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

    public void send(String key, Consumer<Emitter> consumer) {
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
                    consumer.accept(emitter);
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

    public void send(String key, String name, Object data) {
        send(key, emitter -> emitter.send(name, data));
    }

    public void sendAll(Consumer<Emitter> consumer) {
        Set<String> keys = null;
        synchronized (map) {
            if (MapUtils.isNotEmpty(map)) {
                keys = map.keySet().stream().collect(Collectors.toSet());
            }
        }
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String key : keys) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> send(key, consumer), threadPool.getExecutor());
            futures.add(future);
        }

        try {
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(keys.size() * 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            futures.forEach(future -> future.cancel(true));
        }
    }

    public void sendAll(String name, Object data) {
        sendAll(emitter -> emitter.send(name, data));
    }

    public boolean isEmpty() {
        synchronized (map) {
            return map.isEmpty() || map.values().stream().allMatch(CollectionUtils::isEmpty);
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
    public class Emitter extends SseEmitter {
        private final String uuid;
        private final String key;
        private final String groupId;

        public Emitter(String key, String groupId) {
            // 设置超时时间（0 表示永不超时）
            super(0L);

            this.uuid = UuidUtil.random();
            this.key = key;
            this.groupId = groupId;

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

        @SneakyThrows
        public void send(String name, Object data) {
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
