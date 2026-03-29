package org.dromara.address.service.impl;

import com.github.promeg.pinyinhelper.Pinyin;
import org.springframework.stereotype.Service;

/**
 * 标准地址名称服务。
 * 目的：统一封装标准地址全称、当级名称及其简拼生成规则，避免写链路重复实现字符串拼装逻辑。
 * 入参/出参：输入标准地址名称或全称，输出清洗后的名称简拼。
 * 关键约束：括号统一转英文括号，简拼统一输出大写首字母，数字保持原样。
 * 异常与副作用：纯计算服务，无数据库写入副作用。
 */
@Service
public class StandardAddressNameService {

    /**
     * 目的：根据标准地址全称生成完整简拼。
     * 入参：标准地址全称。
     * 出参：大写首字母简拼；入参为空时返回空字符串。
     * 关键约束：中文括号统一转换为英文括号，再执行拼音首字母转换。
     * 异常与副作用：无写入副作用。
     */
    public String buildStandNo(String standName) {
        return buildInitials(standName);
    }

    /**
     * 目的：根据当级标准地址名称生成当级简拼。
     * 入参：当级标准地址名称。
     * 出参：大写首字母简拼；入参为空时返回空字符串。
     * 关键约束：与 `standNo` 生成规则保持一致。
     * 异常与副作用：无写入副作用。
     */
    public String buildSegmNo(String segmName) {
        return buildInitials(segmName);
    }

    private String buildInitials(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        String normalized = source.replace('（', '(').replace('）', ')');
        StringBuilder builder = new StringBuilder(normalized.length());
        for (char ch : normalized.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (Pinyin.isChinese(ch)) {
                builder.append(Pinyin.toPinyin(ch).charAt(0));
                continue;
            }
            if (Character.isLetter(ch)) {
                builder.append(Character.toUpperCase(ch));
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }
}
