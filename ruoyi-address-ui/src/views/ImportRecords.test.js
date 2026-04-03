import { describe, expect, it } from 'vitest';
import ImportRecords from './ImportRecords.vue';

describe('标准地址导入记录页面状态模型', () => {
  it('查询模型应围绕原型筛选项组织', () => {
    const state = ImportRecords.data();

    expect(state.query).toEqual({
      createBy: '',
      status: '',
    });
    expect(state.list).toEqual([]);
  });

  it('查询参数应输出 POST form 兼容字段', () => {
    const params = ImportRecords.methods.buildQueryParams.call({
      query: {
        status: '2',
        createBy: '200001'
      },
      pageNum: 2,
      pageSize: 10,
      timeRange: ['2026-04-01 00:00:00', '2026-04-02 00:00:00']
    });

    expect(params).toEqual({
      status: '2',
      createBy: '200001',
      pageNum: 2,
      pageSize: 10,
      params: {
        beginTime: '2026-04-01 00:00:00',
        endTime: '2026-04-02 00:00:00'
      }
    });
  });
});
