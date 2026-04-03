import { describe, expect, it } from 'vitest';
import StandardDetail from './StandardDetail.vue';

describe('标准地址详情页展示规则', () => {
  it('级别展示应沿用后端返回的标准地址类型中文名称', () => {
    const ctx = {
      levelOptions: [],
      levelNameMap: {},
      typeNameMap: {}
    };

    StandardDetail.methods.applyLevelOptions.call(ctx, [
      { addrTypeId: '180010', name: '门牌号', addrLevel: 15, levelId: 150 },
      { addrTypeId: '180007', name: '房间号', addrLevel: 16, levelId: 160 }
    ]);

    expect(StandardDetail.methods.levelLabel.call(ctx, 15, '180010')).toBe('门牌号');
    expect(StandardDetail.methods.levelLabel.call(ctx, 16, '180007')).toBe('房间号');
  });

  it('详情页应缓存聚合字典并优先回显中文名称', () => {
    const ctx = {
      formOptions: {},
      formOptionLabelMaps: {}
    };

    StandardDetail.methods.applyFormOptions.call(ctx, {
      statusOptions: [{ value: '2140900', label: '有效' }],
      addrInTypeFtthOptions: [{ value: '2140760', label: 'FTTH_双纤' }],
      ftthPonTypeOptions: [{ value: '2141301', label: '1G-PON' }],
      addrInTypeLanOptions: [{ value: '2140770', label: 'LAN' }],
      areaTypeOptions: [{ value: '2140511', label: '城区' }],
      placeTypeOptions: [{ value: '2140800', label: '普通住宅' }]
    });

    expect(StandardDetail.methods.formOptionLabel.call(ctx, 'status', 2140900)).toBe('有效');
    expect(StandardDetail.methods.formOptionLabel.call(ctx, 'ftthPonType', '2141301')).toBe('1G-PON');
    expect(StandardDetail.methods.formOptionLabel.call(ctx, 'placeType', '')).toBe('-');
  });

  it('布尔型标识应转换为中文是/否', () => {
    expect(StandardDetail.methods.yesNoText('Y')).toBe('是');
    expect(StandardDetail.methods.yesNoText('N')).toBe('否');
    expect(StandardDetail.methods.yesNoText('')).toBe('-');
  });

  it('详情时间展示应输出本地化日期时间', () => {
    expect(StandardDetail.methods.formatDate('2026-03-29T18:47:03.000+08:00')).toBe('2026-03-29 18:47:03');
  });
});
