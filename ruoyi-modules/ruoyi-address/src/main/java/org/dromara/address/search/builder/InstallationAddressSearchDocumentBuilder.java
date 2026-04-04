package org.dromara.address.search.builder;

import org.dromara.address.domain.vo.InstallationAddressVo;
import org.dromara.address.search.document.InstallationAddressSearchDocument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 安装地址搜索文档构建器。
 * <p>
 * 目的：将安装地址 VO 转换为 ES 检索文档，统一安装地址索引字段。
 * </p>
 */
public final class InstallationAddressSearchDocumentBuilder {

    private InstallationAddressSearchDocumentBuilder() {
    }

    /**
     * 将安装地址 VO 转换为安装地址搜索文档。
     * <p>
     * 目的：为安装地址检索写入提供标准化文档结构。
     * 入参：{@code source} 为安装地址展示对象，允许为空。
     * 出参：返回映射后的文档对象；当入参为空时返回 {@code null}。
     * 关键约束：需保留安装地址核心检索字段与 {@code associationStatus}。
     * 异常：本方法不主动抛出业务异常。
     * 副作用：无外部副作用，不访问数据库与远程服务。
     * </p>
     *
     * @param source 安装地址 VO
     * @return 安装地址搜索文档
     */
    public static InstallationAddressSearchDocument fromVo(InstallationAddressVo source) {
        if (source == null) {
            return null;
        }
        InstallationAddressSearchDocument target = new InstallationAddressSearchDocument();
        target.setSetAddrId(source.getSetAddrId());
        target.setSegmId(source.getSegmId());
        target.setSetAddrName(source.getSetAddrName());
        target.setStandName(source.getStandName());
        target.setSetAddrNo(source.getSetAddrNo());
        target.setSetType(source.getSetType());
        target.setOrgId(source.getOrgId());
        target.setAssociationStatus(source.getAssociationStatus());
        target.setSegmType(readOptionalString(source, "getSegmType"));
        target.setRegionId(readOptionalString(source, "getRegionId"));
        target.setCreateDate(source.getCreateDate());
        return target;
    }

    private static String readOptionalString(InstallationAddressVo source, String getterName) {
        try {
            Method getter = source.getClass().getDeclaredMethod(getterName);
            getter.setAccessible(true);
            Object value = getter.invoke(source);
            return value instanceof String str ? str : null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }
}
