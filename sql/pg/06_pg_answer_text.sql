-- kb_question 加标准答案知识段列：ask 链检索材料的主体
ALTER TABLE kb_question ADD COLUMN IF NOT EXISTS answer_text TEXT;

COMMENT ON COLUMN kb_question.answer_text IS
    '标准答案知识段(八股正文);库内存全文(实测最长457字),ask拼材料时截断300字;NULL=待补(老行兼容)';