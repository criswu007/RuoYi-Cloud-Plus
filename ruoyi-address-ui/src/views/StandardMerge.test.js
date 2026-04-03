import { beforeEach, describe, expect, it, vi } from 'vitest';

const { getStandardAddressList, getStandardAddressLevelOptions, mergeStandardAddresses } = vi.hoisted(() => ({
  getStandardAddressList: vi.fn(),
  getStandardAddressLevelOptions: vi.fn(),
  mergeStandardAddresses: vi.fn()
}));

vi.mock('../api/address', () => ({
  getStandardAddressList,
  getStandardAddressLevelOptions,
  mergeStandardAddresses
}));

import StandardMerge from './StandardMerge.vue';

describe('标准地址合并页面状态模型', () => {
  beforeEach(() => {
    getStandardAddressList.mockReset();
    getStandardAddressLevelOptions.mockReset();
    mergeStandardAddresses.mockReset();
  });

  it('查询模型应使用标准地址关键词与级别', () => {
    const state = StandardMerge.data();

    expect(state.query).toEqual({
      standName: '',
      segmType: ''
    });
    expect(state.pendingMergeList).toEqual([]);
    expect(state.targetAddress).toBe(null);
  });

  it('添加待合并地址时应按 segmId 去重', () => {
    const ctx = {
      pendingMergeList: [{ segmId: '0001', standName: '江苏省南京市鼓楼区中央路' }]
    };

    StandardMerge.methods.appendPendingMerge.call(ctx, [
      { segmId: '0001', standName: '江苏省南京市鼓楼区中央路' },
      { segmId: '0002', standName: '江苏省南京市鼓楼区中央路1号' }
    ]);

    expect(ctx.pendingMergeList.map(item => item.segmId)).toEqual(['0001', '0002']);
  });

  it('提交载荷应只输出 sourceSegmIds 与 targetSegmId', () => {
    const payload = StandardMerge.methods.buildSubmitPayload.call({
      pendingMergeList: [{ segmId: '0001' }, { segmId: '0002' }],
      targetAddress: { segmId: '0003' }
    });

    expect(payload).toEqual({
      sourceSegmIds: ['0001', '0002'],
      targetSegmId: '0003'
    });
  });

  it('初始化页面时不应自动触发大列表查询', async () => {
    const ctx = {
      loadLevelOptions: vi.fn().mockResolvedValue()
    };

    await StandardMerge.methods.initializePage.call(ctx);

    expect(ctx.loadLevelOptions).toHaveBeenCalledTimes(1);
  });

  it('空条件查询时应直接提示而不是请求后端列表接口', async () => {
    const ctx = {
      query: {
        standName: '',
        segmType: ''
      },
      hasQueryCondition: StandardMerge.methods.hasQueryCondition,
      clearListResult: StandardMerge.methods.clearListResult,
      pageNum: 1,
      pageSize: 10,
      list: [{ segmId: '0001' }],
      total: 1,
      loading: false,
      $message: {
        warning: vi.fn(),
        error: vi.fn()
      }
    };

    await StandardMerge.methods.fetchList.call(ctx);

    expect(getStandardAddressList).not.toHaveBeenCalled();
    expect(ctx.$message.warning).toHaveBeenCalledWith('请输入标准地址关键词或选择级别后再查询');
    expect(ctx.list).toEqual([]);
    expect(ctx.total).toBe(0);
  });

  it('合并模块级别下拉不应展示一二级只读地址', async () => {
    getStandardAddressLevelOptions.mockResolvedValue({
      data: [
        { addrTypeId: '180000', name: '省', addrLevel: 1 },
        { addrTypeId: '180001', name: '市', addrLevel: 2 },
        { addrTypeId: '180015', name: '市区', addrLevel: 3 },
        { addrTypeId: '180002', name: '区县', addrLevel: 3 }
      ]
    });
    const ctx = {
      levelOptions: [],
      levelMap: {},
      isReadonlyRegionLevelOption: StandardMerge.methods.isReadonlyRegionLevelOption,
      $message: {
        error: vi.fn()
      }
    };

    await StandardMerge.methods.loadLevelOptions.call(ctx);

    expect(ctx.levelOptions).toEqual([
      { addrTypeId: '180015', name: '市区', addrLevel: 3 },
      { addrTypeId: '180002', name: '区县', addrLevel: 3 }
    ]);
    expect(ctx.levelMap).toEqual({
      '180015': '市区',
      '180002': '区县'
    });
  });

  it('合并模块传入一二级标准地址类型时应直接拦截', async () => {
    const ctx = {
      query: {
        standName: '南京',
        segmType: '180001'
      },
      hasQueryCondition: StandardMerge.methods.hasQueryCondition,
      clearListResult: StandardMerge.methods.clearListResult,
      isReadonlyRegionSegmType: StandardMerge.methods.isReadonlyRegionSegmType,
      pageNum: 1,
      pageSize: 10,
      list: [{ segmId: '0001' }],
      total: 1,
      loading: false,
      $message: {
        warning: vi.fn(),
        error: vi.fn()
      }
    };

    await StandardMerge.methods.fetchList.call(ctx);

    expect(getStandardAddressList).not.toHaveBeenCalled();
    expect(ctx.$message.warning).toHaveBeenCalledWith('一二级标准地址不支持合并，请选择三级及以下标准地址');
    expect(ctx.list).toEqual([]);
    expect(ctx.total).toBe(0);
  });

  it('合并模块不应把市区级别 180015 误判为只读区域', () => {
    expect(StandardMerge.methods.isReadonlyRegionSegmType.call({}, '180015')).toBe(false);
  });
});
