package com.pcagent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品偏离度
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeviationDegree {
    private String productCode;
    private List<SpecItemDeviationDegree> specItemDeviationDegrees = new ArrayList<>();
    private Integer totalDeviationDegrees; // 总体满足度，如80%

    @JsonIgnore
    private Map<String, SpecItemDeviationDegree> specItemDeviationDegreesMap = new HashMap<>();

    public void addSpecItemDeviationDegree(SpecItemDeviationDegree item) {
        this.specItemDeviationDegrees.add(item);
        this.specItemDeviationDegreesMap.put(item.getSpecName(), item);
    }

    public SpecItemDeviationDegree querySpecItemDeviationDegree(String specName) {
        return specItemDeviationDegreesMap.get(specName);
    }
}

