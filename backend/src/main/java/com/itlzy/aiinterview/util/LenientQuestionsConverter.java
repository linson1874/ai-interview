package com.itlzy.aiinterview.util;

import com.itlzy.aiinterview.vo.QuestionsSchema;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LenientQuestionsConverter implements StructuredOutputConverter<QuestionsSchema> {

    private static final Pattern FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final BeanOutputConverter<QuestionsSchema> delegate = new BeanOutputConverter<>(QuestionsSchema.class);

    @Override
    public String getFormat(){
        return delegate.getFormat();
    }

    @Override
    public QuestionsSchema convert(String source) {
        if(source == null || source.isBlank()){
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
