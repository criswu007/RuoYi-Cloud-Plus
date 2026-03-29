package org.dromara.address.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.address.domain.bo.StandardAddressSelectionRoomBo;
import org.dromara.address.domain.vo.StandardAddressSelectionResultVo;
import org.dromara.address.domain.vo.StandardAddressVo;
import org.dromara.address.service.IStandardAddressSelectionService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 选址平台对外接口。
 * 目的：提供楼栋级地址模糊查询与房间地址创建能力，服务选址平台场景。
 * 关键约束：默认查询限制到楼栋级；创建房间地址必须基于楼栋级标准地址。
 * 副作用：创建房间地址时会同时新增安装地址。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/address/selection")
public class StandardAddressSelectionController extends BaseController {

    private final IStandardAddressSelectionService addressSelectionService;

    /**
     * 模糊查询标准地址（默认限制到楼栋级）。
     *
     * @param keyword 关键字（匹配名称或完整名称，可为空）
     * @param levelMax 最大层级（为空默认 10）
     * @param limit 返回条数上限（为空默认 50）
     * @return 匹配的标准地址列表
     *
     * 关键约束：仅返回状态正常的地址。
     */
    @SaCheckPermission("address:selection:query")
    @GetMapping("/search")
    public R<List<StandardAddressVo>> searchStandardAddresses(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer levelMax,
                                             @RequestParam(required = false) Integer limit) {
        return R.ok(addressSelectionService.searchStandardAddresses(keyword, levelMax, limit));
    }

    /**
     * 楼栋级地址下新增房间标准地址并生成安装地址。
     *
     * @param bo 房间创建参数（楼栋标准地址ID、房间号/名称、安装描述）
     * @return 新创建的标准地址与安装地址ID
     *
     * 关键约束：同一楼栋下房间号唯一；仅允许在楼栋级标准地址下创建。
     * 异常：楼栋标准地址不存在、层级不满足或房间号重复时抛出业务异常。
     * 副作用：新增标准地址与安装地址。
     */
    @SaCheckPermission("address:selection:create")
    @Log(title = "选址新增房间地址", businessType = BusinessType.INSERT)
    @PostMapping("/room")
    public R<StandardAddressSelectionResultVo> createRoomStandardAddress(@Validated @RequestBody StandardAddressSelectionRoomBo bo) {
        return R.ok(addressSelectionService.createRoomStandardAddress(bo));
    }
}
