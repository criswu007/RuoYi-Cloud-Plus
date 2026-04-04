package org.dromara.address.search.builder;

import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.search.document.StandardAddressSearchDocument;

/**
 * 标准地址搜索文档构建器。
 * <p>
 * 目的：将标准地址 VO 转换为 ES 检索文档，收敛检索所需核心字段。
 * </p>
 */
public final class StandardAddressSearchDocumentBuilder {

    private StandardAddressSearchDocumentBuilder() {
    }

    /**
     * 将标准地址 VO 转换为标准地址搜索文档。
     * <p>
     * 目的：为后续 ES 写入与检索提供统一字段映射。
     * 入参：{@code source} 为标准地址展示对象，允许为空。
     * 出参：返回映射后的文档对象；当入参为空时返回 {@code null}。
     * 关键约束：仅映射检索核心字段，不承担字段清洗与业务校验职责。
     * 异常：本方法不主动抛出业务异常。
     * 副作用：无外部副作用，不访问数据库与远程服务。
     * </p>
     *
     * @param source 标准地址 VO
     * @return 标准地址搜索文档
     */
    public static StandardAddressSearchDocument fromVo(StandardAddressVo source) {
        if (source == null) {
            return null;
        }
        StandardAddressSearchDocument target = new StandardAddressSearchDocument();
        target.setSegmId(source.getSegmId());
        target.setParentSegmId(source.getParentSegmId());
        target.setSegmName(source.getSegmName());
        target.setStandName(source.getStandName());
        target.setSegmNo(source.getSegmNo());
        target.setStandNo(source.getStandNo());
        target.setRegionId(source.getRegionId());
        target.setAddrLevel(source.getAddrLevel());
        target.setSegmType(source.getSegmType());
        target.setDistrictId(source.getDistrictId());
        target.setServiceRegionId(source.getServiceRegionId());
        target.setStatus(source.getStatus());
        target.setCreateDate(source.getCreateDate());
        return target;
    }
}
