package org.example.data;

import lombok.Data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class PersonEntity {
    private String 中文名;
    private String 附加名称;
    private String 国籍;
    private String 民族;
    private String 出生地;
    private String 出生日期;
    private String 死亡日期;
    private String 工作职责;
    private List<String> 毕业于 = new LinkedList<>();
    private List<String> 作品 = new LinkedList<>();
    private Map<String, List<String>> 相关人物 = new HashMap<>();
}
