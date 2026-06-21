package org.xiangqian.quick.deploy;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.ToString;
import org.xiangqian.quick.deploy.model.Record;
import org.xiangqian.quick.deploy.model.User;
import org.xiangqian.quick.deploy.util.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 修复 records.json
 *
 * @author xiangqian
 * @date 2026/05/07 14:25
 */
public class RepairRecordsData {

    public static void main(String[] args) throws Exception {
        Path file = Path.of("proj", "java_inc", "records.json");
        List<RepairRecord> records = JsonUtil.deser(Files.readAllBytes(file), new TypeReference<List<RepairRecord>>() {
        });
        for (RepairRecord record : records) {
            User operator = record.getOperator();
            if (operator == null) {
                continue;
            }
            record.setOperator(null);
            List<User> operators = record.getOperators();
            if (operators == null) {
                operators = new ArrayList<>(2);
                record.setOperators(operators);
            }
            if (!operators.contains(operator)) {
                operators.add(operator);
            }
            System.out.println(record + "\n");
            Files.writeString(file, JsonUtil.serToStr(records), StandardCharsets.UTF_8);
        }
    }

    @Data
    @ToString(callSuper = true)
    public static class RepairRecord extends Record {
        private User operator;
    }

}

