package com.exam.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.system.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
    // 继承 BaseMapper 后，你直接免费拥有了 insert, deleteById, updateById, selectList 等几十个现成方法！
}