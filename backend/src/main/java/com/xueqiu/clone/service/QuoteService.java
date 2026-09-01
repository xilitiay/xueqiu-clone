package com.xueqiu.clone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xueqiu.clone.dto.IndexDTO;
import com.xueqiu.clone.dto.QuoteDTO;
import com.xueqiu.clone.model.IndexDef;
import com.xueqiu.clone.repository.IndexDefRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 实时行情服务（数据源可配置）。
 *
 * - provider=tencent（默认）：代理「腾讯 gtimg 公开行情接口」，无需授权 key，仅在交易时段实时；
 *   后端代理可规避浏览器跨域限制。字段解析见 parse()。
 * - provider=json：接入你自己的行情源（如付费 JSON API）。后端会把 app.quote.api-key
 *   作为 Bearer Token 注入请求头，并把返回的 JSON 映射为 QuoteDTO / IndexDTO。
 *   约定 JSON 形如：{ "SH600519": { "name":"...", "price":123, "changePercent":1.2 }, ... }
 *
 * 想换成其它数据源时，只需调整 app.quote 配置与 provider 分支，对外返回的 DTO 结构不变。
 */
@Service
public class QuoteService {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.quote.provider:tencent}")
    private String provider;

    @Value("${app.quote.base-url:https://qt.gtimg.cn/q=}")
    private String baseUrl;

    @Value("${app.quote.api-key:}")
    private String apiKey;

    @Value("${app.quote.indices:SH000001,SZ399001,SZ399006,SH000300,HKHSI,USSPX}")
    private String indices;

    private final IndexDefRepository indexDefRepository;

    private static final String GTIMG = "https://qt.gtimg.cn/q=";

    public QuoteService(IndexDefRepository indexDefRepository) {
        this.indexDefRepository = indexDefRepository;
    }

    /** 内部 symbol（SH600519 / SZ300750 / HK00700 / AAPL）→ gtimg 格式（sh600519 / usAAPL） */
    public static String toGtimgSymbol(String symbol) {
        if (symbol == null) return "";
        String s = symbol.trim().toUpperCase();
        if (s.startsWith("SH")) return "sh" + s.substring(2);
        if (s.startsWith("SZ")) return "sz" + s.substring(2);
        if (s.startsWith("HK")) return "hk" + s.substring(2);
        return "us" + s; // 其余按美股处理
    }

    /** 单只股票实时行情 */
    public QuoteDTO getQuote(String symbol) {
        if ("json".equalsIgnoreCase(provider)) {
            return fetchJsonQuotes(List.of(symbol)).get(symbol);
        }
        String body = fetch(GTIMG + enc(toGtimgSymbol(symbol)));
        return parse(symbol, extractSegment(body, toGtimgSymbol(symbol)));
    }

    /** 批量行情，返回 symbol -> QuoteDTO */
    public Map<String, QuoteDTO> getQuotes(List<String> symbols) {
        Map<String, QuoteDTO> map = new LinkedHashMap<>();
        if (symbols == null || symbols.isEmpty()) return map;
        if ("json".equalsIgnoreCase(provider)) {
            return fetchJsonQuotes(symbols);
        }
        String q = symbols.stream().map(QuoteService::toGtimgSymbol).collect(Collectors.joining(","));
        String body = fetch(GTIMG + enc(q));
        for (String symbol : symbols) {
            try {
                map.put(symbol, parse(symbol, extractSegment(body, toGtimgSymbol(symbol))));
            } catch (Exception ignored) {
                // 单只解析失败不影响其余
            }
        }
        return map;
    }

    /**
     * 市场指数实时行情。指数清单由数据库 t_index_def 驱动（启用且按 sortOrder 排序），
     * 数据库为空时回退到 application.yml 的 app.quote.indices。
     * 单只取不到实时行情时，用同名内置 Mock 兜底；整体不可达则返回全部 Mock 兜底，
     * 保证前端侧栏指数模块始终有数据。
     */
    public List<IndexDTO> getIndexQuotes() {
        List<IndexDef> defs = indexDefRepository.findByEnabledTrueOrderBySortOrderAsc();
        if (defs.isEmpty()) {
            defs = Arrays.stream(indices.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(c -> new IndexDef(c, defaultName(c), null, 0))
                    .toList();
        }
        List<String> codes = defs.stream().map(IndexDef::getCode).toList();
        try {
            Map<String, QuoteDTO> map = getQuotes(codes);
            List<IndexDTO> out = new ArrayList<>();
            for (IndexDef d : defs) {
                QuoteDTO q = map.get(d.getCode());
                if (q != null && q.price() != null) {
                    out.add(new IndexDTO(d.getCode(), d.getName(), q.price(), q.changePercent()));
                } else {
                    IndexDTO fb = mockIndexMap().get(d.getCode());
                    out.add(fb != null ? fb : new IndexDTO(d.getCode(), d.getName(), null, null));
                }
            }
            return out;
        } catch (Exception e) {
            return defs.stream().map(d -> {
                IndexDTO fb = mockIndexMap().get(d.getCode());
                return fb != null ? fb : new IndexDTO(d.getCode(), d.getName(), null, null);
            }).toList();
        }
    }

    /** 请求行情源（GBK 文本或 JSON 均按 byte[] 取回后自行解析） */
    private String fetch(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0");
            headers.add("Referer", "https://stockapp.finance.qq.com/");
            if (apiKey != null && !apiKey.isEmpty()) headers.setBearerAuth(apiKey);
            ResponseEntity<byte[]> resp = rest.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), byte[].class);
            return new String(resp.getBody(), Charset.forName("GBK"));
        } catch (Exception e) {
            throw new IllegalStateException("行情接口暂不可用，请检查网络或后端出网权限: " + e.getMessage(), e);
        }
    }

    /** 从多段响应中截取属于某 symbol 的引号内容 */
    private String extractSegment(String body, String g) {
        String prefix = "v_" + g + "=\"";
        int idx = body.indexOf(prefix);
        if (idx < 0) return "";
        int start = idx + prefix.length();
        int end = body.indexOf("\"", start);
        if (end < 0) end = body.length();
        return body.substring(start, end);
    }

    /** 解析 gtimg 返回字段（~ 分隔）。字段定义见腾讯行情接口文档。 */
    private QuoteDTO parse(String symbol, String text) {
        if (text == null || text.isEmpty()) throw new IllegalStateException("空行情数据");
        String[] f = text.split("~");
        if (f.length < 40) throw new IllegalStateException("行情字段不足");

        String name = at(f, 1);
        BigDecimal price = bd(f, 3);
        BigDecimal prevClose = bd(f, 4);
        BigDecimal open = bd(f, 5);
        BigDecimal high = bd(f, 34);
        BigDecimal low = bd(f, 35);
        BigDecimal change = bd(f, 32);
        BigDecimal changePercent = bd(f, 33);
        Long volume = lng(f, 6);
        BigDecimal turnoverRate = bd(f, 37);
        BigDecimal pe = bd(f, 38);
        BigDecimal pb = bd(f, 39);
        BigDecimal marketCap = bd(f, 45);              // 单位：万元
        if (marketCap != null) marketCap = marketCap.divide(BigDecimal.valueOf(10000)); // 转亿元
        String time = (at(f, 30) + " " + at(f, 31)).trim();

        return new QuoteDTO(symbol, name, price, change, changePercent,
                open, high, low, prevClose, volume, turnoverRate, pe, pb, marketCap, time);
    }

    /** 接入自带 JSON 行情源：响应为 { symbol: { name, price, changePercent, ... } } */
    private Map<String, QuoteDTO> fetchJsonQuotes(List<String> symbols) {
        Map<String, QuoteDTO> map = new LinkedHashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0");
            if (apiKey != null && !apiKey.isEmpty()) headers.setBearerAuth(apiKey);
            ResponseEntity<String> resp = rest.exchange(baseUrl, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            for (String symbol : symbols) {
                JsonNode node = root.has(symbol) ? root.get(symbol)
                        : root.has(toGtimgSymbol(symbol)) ? root.get(toGtimgSymbol(symbol)) : null;
                if (node == null) continue;
                BigDecimal price = node.has("price") ? new BigDecimal(node.get("price").asText()) : null;
                BigDecimal cp = node.has("changePercent") ? new BigDecimal(node.get("changePercent").asText()) : null;
                String name = node.has("name") ? node.get("name").asText() : symbol;
                BigDecimal change = node.has("change") ? new BigDecimal(node.get("change").asText()) : null;
                BigDecimal prevClose = node.has("prevClose") ? new BigDecimal(node.get("prevClose").asText()) : null;
                BigDecimal open = node.has("open") ? new BigDecimal(node.get("open").asText()) : null;
                BigDecimal high = node.has("high") ? new BigDecimal(node.get("high").asText()) : null;
                BigDecimal low = node.has("low") ? new BigDecimal(node.get("low").asText()) : null;
                Long volume = node.has("volume") ? node.get("volume").asLong() : null;
                map.put(symbol, new QuoteDTO(symbol, name, price, change, cp,
                        open, high, low, prevClose, volume, null, null, null, null, ""));
            }
        } catch (Exception e) {
            // 单只或全部失败：返回已成功解析的部分
        }
        return map;
    }

    /** 行情源不可达时的内置指数 Mock（按 code 索引，便于按 DB 配置兜底） */
    private Map<String, IndexDTO> mockIndexMap() {
        Map<String, IndexDTO> m = new LinkedHashMap<>();
        List<IndexDTO> list = List.of(
                new IndexDTO("SH000001", "上证指数", new BigDecimal("3210.50"), new BigDecimal("0.42")),
                new IndexDTO("SZ399001", "深证成指", new BigDecimal("10180.30"), new BigDecimal("-0.18")),
                new IndexDTO("SZ399006", "创业板指", new BigDecimal("2030.75"), new BigDecimal("0.91")),
                new IndexDTO("SH000300", "沪深300", new BigDecimal("3780.20"), new BigDecimal("0.25")),
                new IndexDTO("HKHSI", "恒生指数", new BigDecimal("17650.00"), new BigDecimal("-0.55")),
                new IndexDTO("USSPX", "标普500", new BigDecimal("5230.10"), new BigDecimal("0.33"))
        );
        list.forEach(d -> m.put(d.code(), d));
        return m;
    }

    private String defaultName(String code) {
        IndexDTO fb = mockIndexMap().get(code);
        return fb != null ? fb.name() : code;
    }

    private String at(String[] f, int i) {
        return i < f.length ? f[i] : "";
    }

    private BigDecimal bd(String[] f, int i) {
        if (i >= f.length || f[i].isEmpty()) return null;
        try { return new BigDecimal(f[i]); } catch (Exception e) { return null; }
    }

    private Long lng(String[] f, int i) {
        if (i >= f.length || f[i].isEmpty()) return null;
        try { return Long.parseLong(f[i]); } catch (Exception e) { return null; }
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
