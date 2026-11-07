package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.ResumeSaveDTO;
import com.itlzy.aiinterview.vo.PageVO;
import com.itlzy.aiinterview.vo.ResumeDetailVO;
import com.itlzy.aiinterview.vo.ResumeListItemVO;

public interface ResumeService {

    void create(Long userId, ResumeSaveDTO dto);

    void update(Long userId,Long id,ResumeSaveDTO dto);

    void delete(Long userId,Long id);

    PageVO<ResumeListItemVO> page(Long userId, long page, long size);

    ResumeDetailVO detail(Long userId, Long id);
}
