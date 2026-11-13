package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.vo.ReportSchema;
import com.itlzy.aiinterview.vo.ScoringSchema;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 评分输出清洗器（合同07 工单②）——整版分叉自 LenientQuestionsConverter，只换 schema 类型三件套。
 *  为什么复制不抽父类：两个 schema 将随阶段四/五各自演化（出题带 category/difficulty，
 *  评分后续可能带 evidence 引用），此刻抽公共父类=为未来的分叉提前打石膏。
 *  37 行的重复是教学版的价格，不是架构的债。 */
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

    /** 无围栏的寒暄脏：截第一个 { 到最后一个 }；压根没花括号就原样交给 delegate 抛——L2 不吞错 */
    private String extractBraces(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return (start >= 0 && end > start) ? s.substring(start, end + 1) : s;
    }
}
