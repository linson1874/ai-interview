package com.itlzy.aiinterview.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;
import java.util.function.Function;


@Data
public class PageVO<T> {

    private List<T> list;
    private long total;
    private long page;
    private long size;

    /** 工厂：把 MP 的 Page<Entity> 转换装进我们的信封
     *  @param converter 单条 Entity→VO 的转换函数（Service 传 lambda 进来） */
    public static <E, T> PageVO<T> of(Page<E> source, Function<E, T> converter) {
        PageVO<T> vo = new PageVO<>();
        vo.setList(source.getRecords().stream().map(converter).toList());
        vo.setTotal(source.getTotal());
        vo.setPage(source.getCurrent());
        vo.setSize(source.getSize());
        return vo;
    }
}