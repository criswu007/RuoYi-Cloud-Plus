import { describe, expect, it, vi } from 'vitest';
import StandardList from './StandardList.vue';

describe('标准地址列表页面状态模型', () => {
  it('查询模型应使用标准地址 canonical 字段', () => {
    const state = StandardList.data();

    expect(state.query).toMatchObject({
      standName: '',
      segmType: ''
    });
    expect(state.levelOptions).toEqual([]);
    expect(state.query).not.toHaveProperty('name');
    expect(state.query).not.toHaveProperty('fullName');
    expect(state.query).not.toHaveProperty('code');
    expect(state.query).not.toHaveProperty('levelId');
  });

  it('编辑模型应以 segmId/parentSegmId/segmName/notes 为核心字段', () => {
    const editor = StandardList.methods.createEmptyEditor();

    expect(editor).toMatchObject({
      segmId: null,
      parentSegmId: '',
      parentAddrLevel: '',
      segmName: '',
      addrLevel: '',
      notes: '',
      stationId: '',
      installStationId: '',
      busStationId: ''
    });
  });

  it('页面状态应维护聚合字典与三类管理站候选缓存', () => {
    const state = StandardList.data();

    expect(state.formOptions).toMatchObject({
      statusOptions: [],
      addrInTypeFtthOptions: [],
      ftthPonTypeOptions: [],
      addrInTypeLanOptions: [],
      areaTypeOptions: [],
      placeTypeOptions: []
    });
    expect(state.formOptionLabelMaps).toMatchObject({
      status: {},
      addrInTypeFtth: {},
      ftthPonType: {},
      addrInTypeLan: {},
      areaType: {},
      placeType: {}
    });
    expect(state.stationOptions).toMatchObject({
      stationId: [],
      installStationId: [],
      busStationId: []
    });
    expect(state.stationOptionLoading).toMatchObject({
      stationId: false,
      installStationId: false,
      busStationId: false
    });
  });

  it('批量新增模型应围绕 parentSegmId 与预览列表组织', () => {
    const state = StandardList.data();

    expect(state.batchAdd).toMatchObject({
      parentSegmId: '',
      prefix: '',
      startNum: '',
      endNum: '',
      suffix: ''
    });
    expect(Array.isArray(state.batchPreviewList)).toBe(true);
  });

  it('导入弹窗状态应围绕模板下载、文件选择和失败导出组织', () => {
    const state = StandardList.data();

    expect(state.importDialogVisible).toBe(false);
    expect(state.importUploading).toBe(false);
    expect(state.importFile).toBe(null);
    expect(state.importResult).toBe(null);
    expect(state.importUpdateSupport).toBe(false);
  });

  it('页面状态应维护批量标签弹窗所需模型', () => {
    const state = StandardList.data();

    expect(state.tagDialogVisible).toBe(false);
    expect(state.tagDialogMode).toBe('bind');
    expect(state.tagDialogRows).toEqual([]);
    expect(state.tagOptionLoading).toBe(false);
    expect(state.tagSubmitLoading).toBe(false);
    expect(state.tagOptions).toEqual([]);
    expect(state.selectedTagIds).toEqual([]);
  });

  it('级别字典应由后端返回结果驱动映射关系', () => {
    const ctx = {
      levelOptions: [],
      levelNameMap: {},
      typeNameMap: {}
    };

    StandardList.methods.applyLevelOptions.call(ctx, [
      { addrTypeId: '180009', name: '建筑单元', addrLevel: 13, levelId: 130 },
      { addrTypeId: '180007', name: '房间号', addrLevel: 16, levelId: 160 },
      { addrTypeId: '180010', name: '门牌号', addrLevel: 15, levelId: 150 }
    ]);

    expect(ctx.levelOptions.map(item => item.addrLevel)).toEqual([13, 15, 16]);
    expect(ctx.levelNameMap[16]).toBe('房间号');
    expect(ctx.typeNameMap['180009']).toBe('建筑单元');
    expect(StandardList.methods.levelLabel.call(ctx, 16, '180007')).toBe('房间号');
    expect(StandardList.methods.levelLabel.call(ctx, 13, '180009')).toBe('建筑单元');
    expect(StandardList.methods.levelLabel.call(ctx, 15, '180010')).toBe('门牌号');
  });

  it('编辑模型应保留父级地址名称用于原型式禁用展示', () => {
    const editor = StandardList.methods.createEmptyEditor({ segmId: 'parent-1', standName: '江苏省南京市', addrLevel: 2 });

    expect(editor.parentSegmId).toBe('parent-1');
    expect(editor.parentAddrLevel).toBe(2);
    expect(editor.parentStandName).toBe('江苏省南京市');
  });

  it('编辑模型应保留 regionId 供管理站候选按区域过滤', () => {
    const editor = StandardList.methods.createEmptyEditor({ segmId: 'parent-1', standName: '江苏省南京市', regionId: '320100' });

    expect(editor.regionId).toBe('320100');
  });

  it('聚合字典应构建字段值到中文名的映射关系', () => {
    const ctx = {
      formOptions: {},
      formOptionLabelMaps: {}
    };

    StandardList.methods.applyFormOptions.call(ctx, {
      statusOptions: [{ value: '2140900', label: '有效' }],
      addrInTypeFtthOptions: [{ value: '2140760', label: 'FTTH_双纤' }],
      ftthPonTypeOptions: [{ value: '2141301', label: '1G-PON' }],
      addrInTypeLanOptions: [{ value: '2140770', label: 'LAN' }],
      areaTypeOptions: [{ value: '2140511', label: '城区' }],
      placeTypeOptions: [{ value: '2140800', label: '普通住宅' }]
    });

    expect(ctx.formOptions.statusOptions[0]).toEqual({ value: '2140900', label: '有效' });
    expect(ctx.formOptionLabelMaps.status['2140900']).toBe('有效');
    expect(ctx.formOptionLabelMaps.addrInTypeFtth['2140760']).toBe('FTTH_双纤');
    expect(StandardList.methods.formOptionLabel.call(ctx, 'areaType', 2140511)).toBe('城区');
    expect(StandardList.methods.formOptionLabel.call(ctx, 'placeType', '')).toBe('-');
  });

  it('管理站候选应按字段分别缓存并输出原型展示名称', () => {
    const ctx = {
      stationOptions: {
        stationId: [],
        installStationId: [],
        busStationId: []
      }
    };

    StandardList.methods.applyStationOptions.call(ctx, 'stationId', [
      { stationId: 'ST320100WX001', stationName: '洪武路维修站', regionId: '320100', manageType: '2017101' }
    ]);

    expect(ctx.stationOptions.stationId).toEqual([
      { stationId: 'ST320100WX001', stationName: '洪武路维修站', regionId: '320100', manageType: '2017101' }
    ]);
    expect(StandardList.methods.stationOptionLabel.call(ctx, ctx.stationOptions.stationId[0])).toBe('洪武路维修站');
  });

  it('编辑回填时应优先使用详情返回的管理站名称，而不是直接显示 stationId', () => {
    const ctx = {
      stationOptions: {
        stationId: [],
        installStationId: [],
        busStationId: []
      }
    };

    StandardList.methods.seedEditorStationOptions.call(ctx, {
      stationId: 'ST320100WX001',
      stationName: '洪武路维修站',
      installStationId: 'ST320100AZ001',
      installStationName: '鼓楼安装站',
      busStationId: 'ST320100BY001',
      busStationName: '中央门营业站'
    });

    expect(ctx.stationOptions.stationId).toEqual([
      { stationId: 'ST320100WX001', stationName: '洪武路维修站', regionId: '', manageType: '' }
    ]);
    expect(ctx.stationOptions.installStationId).toEqual([
      { stationId: 'ST320100AZ001', stationName: '鼓楼安装站', regionId: '', manageType: '' }
    ]);
    expect(ctx.stationOptions.busStationId).toEqual([
      { stationId: 'ST320100BY001', stationName: '中央门营业站', regionId: '', manageType: '' }
    ]);
  });

  it('新增场景应只展示父级之后的所有可选级别', () => {
    const ctx = {
      levelOptions: [
        { addrTypeId: '180004', name: '路、里、弄、巷', addrLevel: 7, levelId: 70 },
        { addrTypeId: '180011', name: '庄、组、队', addrLevel: 8, levelId: 80 },
        { addrTypeId: '180005', name: '建筑群、小区', addrLevel: 9, levelId: 90 },
        { addrTypeId: '180100', name: '尾级地址（选址生成）', addrLevel: 19, levelId: 190 }
      ],
      editor: {
        segmId: null,
        parentAddrLevel: 7
      }
    };

    expect(StandardList.methods.editorLevelOptions.call(ctx).map(item => item.addrLevel)).toEqual([8, 9, 19]);
  });

  it('新增保存时应提交用户显式选择的当前地址级别', () => {
    const ctx = {
      editor: {
        segmId: null,
        parentSegmId: 'parent-1',
        parentAddrLevel: 7,
        regionId: '320100',
        segmName: '紫峰大厦',
        addrLevel: '9',
        segmType: '',
        status: '2140900',
        notes: '',
        stationId: '',
        installStationId: '',
        busStationId: '',
        addrInTypeFtth: '',
        ftthPonType: '',
        addrInTypeLan: '',
        areaType: '',
        placeType: '',
        coverNum: '',
        singleProjectCode: '',
        supportingFeeCommunityFlag: '',
        isCity: ''
      },
      normalizeOptionalNumber: StandardList.methods.normalizeOptionalNumber
    };

    const payload = StandardList.methods.buildEditorPayload.call(ctx);

    expect(payload.addrLevel).toBe(9);
    expect(payload).not.toHaveProperty('segmType');
  });

  it('一二级标准地址查询参数不应携带排序字段', () => {
    const ctx = {
      pageNum: 1,
      pageSize: 10,
      query: {
        standName: '南京',
        segmType: '180001'
      },
      levelOptions: [
        { addrTypeId: '180000', name: '省', addrLevel: 1, levelId: 10 },
        { addrTypeId: '180001', name: '市', addrLevel: 2, levelId: 20 },
        { addrTypeId: '180004', name: '路、里、弄、巷', addrLevel: 7, levelId: 70 }
      ]
    };

    const params = StandardList.methods.buildListQueryParams.call(ctx);

    expect(params).toMatchObject({
      pageNum: 1,
      pageSize: 10,
      standName: '南京',
      segmType: '180001'
    });
    expect(params).not.toHaveProperty('orderByColumn');
    expect(params).not.toHaveProperty('isAsc');
  });

  it('三级及以下标准地址查询参数应保留统一排序字段', () => {
    const ctx = {
      pageNum: 2,
      pageSize: 20,
      query: {
        standName: '鼓楼',
        segmType: '180004'
      },
      levelOptions: [
        { addrTypeId: '180000', name: '省', addrLevel: 1, levelId: 10 },
        { addrTypeId: '180001', name: '市', addrLevel: 2, levelId: 20 },
        { addrTypeId: '180004', name: '路、里、弄、巷', addrLevel: 7, levelId: 70 }
      ]
    };

    const params = StandardList.methods.buildListQueryParams.call(ctx);

    expect(params).toMatchObject({
      pageNum: 2,
      pageSize: 20,
      standName: '鼓楼',
      segmType: '180004',
      orderByColumn: 'createDate',
      isAsc: 'desc'
    });
  });

  it('拼装名称应优先使用父级地址名称与当级名称做前端预览', () => {
    expect(StandardList.methods.buildAssembledStandName({
      parentStandName: '江苏省南京市',
      segmName: '鼓楼区'
    })).toBe('江苏省南京市鼓楼区');

    expect(StandardList.methods.buildAssembledStandName({
      parentStandName: '江苏省南京市',
      segmName: ''
    })).toBe('江苏省南京市');
  });

  it('新增入口在未勾选记录时应提示原型文案', () => {
    const warning = vi.fn();

    StandardList.methods.openCreateDialog.call({
      selectedRows: [],
      $message: { warning }
    });

    expect(warning).toHaveBeenCalledWith('请先勾选一条或多条标准地址记录');
  });

  it('更多操作中的标签命令应打开对应批量标签弹窗', () => {
    const openBindTagDialog = vi.fn();
    const openUnbindTagDialog = vi.fn();

    StandardList.methods.handleMoreAction.call({
      openBindTagDialog,
      openUnbindTagDialog
    }, 'bindTag');
    StandardList.methods.handleMoreAction.call({
      openBindTagDialog,
      openUnbindTagDialog
    }, 'unbindTag');

    expect(openBindTagDialog).toHaveBeenCalledTimes(1);
    expect(openUnbindTagDialog).toHaveBeenCalledTimes(1);
  });

  it('列表时间展示应转换为原型更接近的日期时间格式', () => {
    expect(StandardList.methods.formatDate('2026-03-29T18:47:03.000+08:00')).toBe('2026-03-29 18:47:03');
  });
});
