package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.vo.ReportSchema;
import com.itlzy.aiinterview.vo.ScoringSchema;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 报告输出清洗器：剥离代码围栏与杂质后交给 JSON 解析。结构相似但与出题清洗器各自演化，不抽父类。 */
public class LenientReportConverter implements StructuredOutputConverter<ReportSchema> {

    private static final Pattern FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final BeanOutputConverter<ReportSchema> delegate = new BeanOutputConverter<>(ReportSchema.class);

    @Override
    public String getFormat() {
        return delegate.getFormat();
    }

    @Override
    public ReportSchema convert(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("模型输出为空");
        }
        Matcher fence = FENCE.matcher(source);
        String json = fence.find() ? fence.group(1).trim() : extractBraces(source.trim());
        return delegate.convert(json);
    }

    /** 无代码围栏时截取第一个 { 到最后一个 }；无花括号则原样交由解析器报错 */
    private String extractBraces(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : s;
    }
}
