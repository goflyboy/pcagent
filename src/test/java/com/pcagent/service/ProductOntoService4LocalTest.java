package com.pcagent.service;

import com.pcagent.model.*;
import com.pcagent.service.impl.ProductOntoService4Local;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductOntoService4Local测试类
 */
class ProductOntoService4LocalTest {
    private ProductOntoService productOntoService;

    @BeforeEach
    void setUp() {
        productOntoService = new ProductOntoService4Local();
        ((ProductOntoService4Local) productOntoService).init();
    }

    @Test
    void testQuerySalesCatalogNodes() {
        List<CatalogNode> nodes = productOntoService.querySalesCatalogNodes("001", "服务器");
        assertNotNull(nodes);
        assertFalse(nodes.isEmpty());
        assertTrue(nodes.stream().anyMatch(n -> n.getName().contains("服务器")));
    }

    @Test
    void testQueryProductByNode() {
        List<Product> products = productOntoService.queryProductByNode(
                Arrays.asList("data_center_server"));
        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertTrue(products.stream().anyMatch(p -> p.getCode().contains("poweredge")));
    }

    @Test
    void testQueryProductSpecification() {
        ProductSpecification spec = productOntoService.queryProductSpecification("poweredge_r760xa");
        assertNotNull(spec);
        assertEquals("poweredge_r760xa", spec.getProductCode());
        assertNotNull(spec.getSpecs());
        assertFalse(spec.getSpecs().isEmpty());
    }

    @Test
    void testQueryProductParameter() {
        ProductParameter param = productOntoService.queryProductParameter("poweredge_r760xa");
        assertNotNull(param);
        assertEquals("poweredge_r760xa", param.getProductCode());
        assertNotNull(param.getParas());
    }

    @Test
    void testParseProductSpecs() {
        List<String> originalSpecs = Arrays.asList(
                "最高工作环境温度=-40°C",
                "处理器核心数≥16核"
        );
        ProductSpecificationReq req = productOntoService.parseProductSpecs(
                "data_center_server", originalSpecs);
        assertNotNull(req);
        assertEquals("data_center_server", req.getCatalogNode());
        assertNotNull(req.getSpecReqs());
    }
}

