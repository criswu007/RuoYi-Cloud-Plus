import { beforeEach, describe, expect, it, vi } from 'vitest';

const { getStandardAddressList, getStandardAddressLevelOptions, splitStandardAddress } = vi.hoisted(() => ({
  getStandardAddressList: vi.fn(),
  getStandardAddressLevelOptions: vi.fn(),
  splitStandardAddress: vi.fn()
}));

vi.mock('../api/address', () => ({
  getStandardAddressList,
  getStandardAddressLevelOptions,
  splitStandardAddress
}));

import StandardSplit from './StandardSplit.vue';

describe('标准地址拆分页面状态模型', () => {
  beforeEach(() => {
    getStandardAddressList.mockReset();
    getStandardAddressLevelOptions.mockReset();
    splitStandardAddress.mockReset();
  });

  it('查询模型应使用标准地址关键词与级别', () => {
    const state = StandardSplit.data();

    expect(state.query).toEqual({
      standName: '',
      segmType: ''
    });
    expect(state.selectedSource).toBe(null);
    expect(state.splitItems).toEqual([]);
  });

  it('新增拆分项后只应维护 segmName 字段', () => {
    const ctx = {
      splitItems: []
    };

    StandardSplit.methods.addSplitItem.call(ctx);

    expect(ctx.splitItems).toEqual([{ segmName: '' }]);
  });

  it('提交载荷应只输出 sourceSegmId 与 splitItems', () => {
    const payload = StandardSplit.methods.buildSubmitPayload.call({
      selectedSource: { segmId: '0001' },
      splitItems: [{ segmName: '1单元' }, { segmName: '2单元' }]
    });

    expect(payload).toEqual({
      sourceSegmId: '0001',
      splitItems: [{ segmName: '1单元' }, { segmName: '2单元' }]
    });
  });

  it('初始化页面时不应自动触发大列表查询', async () => {
    const ctx = {
      loadLevelOptions: vi.fn().mockResolvedValue()
    };

    await StandardSplit.methods.initializePage.call(ctx);

    expect(ctx.loadLevelOptions).toHaveBeenCalledTimes(1);
  });

  it('空条件查询时应直接提示而不是请求后端列表接口', async () => {
    const ctx = {
      query: {
        standName: '',
        segmType: ''
      },
      hasQueryCondition: StandardSplit.methods.hasQueryCondition,
      clearListResult: StandardSplit.methods.clearListResult,
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

    await StandardSplit.methods.fetchList.call(ctx);

    expect(getStandardAddressList).not.toHaveBeenCalled();
    expect(ctx.$message.warning).toHaveBeenCalledWith('请输入标准地址关键词或选择级别后再查询');
    expect(ctx.list).toEqual([]);
    expect(ctx.total).toBe(0);
  });

  it('拆分模块级别下拉不应展示一二级只读地址', async () => {
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
      isReadonlyRegionLevelOption: StandardSplit.methods.isReadonlyRegionLevelOption,
      $message: {
        error: vi.fn()
      }
    };

    await StandardSplit.methods.loadLevelOptions.call(ctx);

    expect(ctx.levelOptions).toEqual([
      { addrTypeId: '180015', name: '市区', addrLevel: 3 },
      { addrTypeId: '180002', name: '区县', addrLevel: 3 }
    ]);
    expect(ctx.levelMap).toEqual({
      '180015': '市区',
      '180002': '区县'
    });
  });

  it('拆分模块传入一二级标准地址类型时应直接拦截', async () => {
    const ctx = {
      query: {
        standName: '南京',
        segmType: '180001'
      },
      hasQueryCondition: StandardSplit.methods.hasQueryCondition,
      clearListResult: StandardSplit.methods.clearListResult,
      isReadonlyRegionSegmType: StandardSplit.methods.isReadonlyRegionSegmType,
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

    await StandardSplit.methods.fetchList.call(ctx);

    expect(getStandardAddressList).not.toHaveBeenCalled();
    expect(ctx.$message.warning).toHaveBeenCalledWith('一二级标准地址不支持拆分，请选择三级及以下标准地址');
    expect(ctx.list).toEqual([]);
    expect(ctx.total).toBe(0);
  });

  it('拆分模块不应把市区级别 180015 误判为只读区域', () => {
    expect(StandardSplit.methods.isReadonlyRegionSegmType.call({}, '180015')).toBe(false);
  });
});
