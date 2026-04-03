<template>
  <div class="standard-page">
    <el-card class="search-card" shadow="never">
      <el-form class="search-form" :inline="true" :model="query" @submit.native.prevent>
        <el-form-item label="标准地址">
          <el-input
            v-model="query.standName"
            clearable
            placeholder="请输入标准地址"
            @keyup.enter.native="fetchList"
          />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.segmType" clearable placeholder="请选择级别">
            <el-option
              v-for="item in levelOptions"
              :key="item.addrTypeId"
              :label="levelOptionLabel(item)"
              :value="item.addrTypeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" @click="openCreateDialog">新增</el-button>
          <el-button plain type="danger" :disabled="!selectedStandardAddressIds.length" @click="batchRemove">
            删除
          </el-button>
          <el-button plain @click="exportCurrentPage">导出</el-button>
          <el-dropdown @command="handleMoreAction">
            <el-button plain>
              更多操作
              <i class="el-icon-arrow-down el-icon--right" />
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="batchAdd">批量新增地址</el-dropdown-item>
              <el-dropdown-item command="import">导入地址</el-dropdown-item>
              <el-dropdown-item command="bindTag">批量打标签</el-dropdown-item>
              <el-dropdown-item command="unbindTag">批量删标签</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div class="toolbar-summary">
          <span>当前页 {{ list.length }} 条</span>
          <span>已勾选 {{ selectedStandardAddressIds.length }} 条</span>
        </div>
      </div>
      <div class="toolbar-tip">
        新增标准地址需先勾选一条父级地址记录；批量新增地址可从左侧候选列表继续选择父级地址。
      </div>

      <el-table
        ref="standardTable"
        v-loading="loading"
        :data="list"
        border
        stripe
        size="small"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="序号" width="68">
          <template #default="{ $index }">
            {{ (pageNum - 1) * pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="standName" label="标准地址" min-width="280" show-overflow-tooltip />
        <el-table-column prop="standNo" label="地址简拼" min-width="180" show-overflow-tooltip />
        <el-table-column label="父级地址" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ parentDisplayName(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="segmName" label="当级名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="级别" width="110">
          <template #default="{ row }">
            <span class="level-chip">{{ levelLabel(row.addrLevel, row.segmType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createDate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="mini" @click="openDetail(row)">详情</el-button>
            <el-button
              size="mini"
              type="primary"
              plain
              :disabled="row.readOnlyFlag === true || row.canEdit === false"
              @click="openEditDialog(row)"
            >
              修改
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :current-page="pageNum"
          :page-size="pageSize"
          @current-change="changePage"
        />
      </div>
    </el-card>

    <el-dialog
      :visible.sync="editorVisible"
      :title="editor.segmId ? '编辑标准地址' : '新增标准地址'"
      width="860px"
    >
      <el-form ref="editorForm" :model="editor" :rules="editorRules" label-width="130px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="父级地址" prop="parentSegmId">
              <el-input :value="editor.parentStandName || '-'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前地址级别" prop="addrLevel">
              <el-select
                v-if="!editor.segmId"
                v-model="editor.addrLevel"
                clearable
                placeholder="请选择当前地址级别"
                style="width: 100%"
              >
                <el-option
                  v-for="item in editorLevelOptions()"
                  :key="item.addrTypeId"
                  :label="levelOptionLabel(item)"
                  :value="item.addrLevel"
                />
              </el-select>
              <el-input
                v-else
                :value="levelLabel(editor.addrLevel, editor.segmType)"
                disabled
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="当级名称" prop="segmName">
              <el-input v-model="editor.segmName" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拼装名称">
              <el-input :value="buildAssembledStandName(editor)" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="editor.status" clearable placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="item in formOptions.statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属维修管理站">
              <el-select
                v-model="editor.stationId"
                clearable
                filterable
                remote
                reserve-keyword
                placeholder="请输入维修管理站名称搜索"
                style="width: 100%"
                :loading="stationOptionLoading.stationId"
                :remote-method="keyword => searchStationOptions('stationId', keyword)"
                @visible-change="visible => handleStationDropdownVisible('stationId', visible)"
              >
                <el-option
                  v-for="item in stationOptions.stationId"
                  :key="item.stationId"
                  :label="stationOptionLabel(item)"
                  :value="item.stationId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属安装管理站">
              <el-select
                v-model="editor.installStationId"
                clearable
                filterable
                remote
                reserve-keyword
                placeholder="请输入安装管理站名称搜索"
                style="width: 100%"
                :loading="stationOptionLoading.installStationId"
                :remote-method="keyword => searchStationOptions('installStationId', keyword)"
                @visible-change="visible => handleStationDropdownVisible('installStationId', visible)"
              >
                <el-option
                  v-for="item in stationOptions.installStationId"
                  :key="item.stationId"
                  :label="stationOptionLabel(item)"
                  :value="item.stationId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属营业管理站">
              <el-select
                v-model="editor.busStationId"
                clearable
                filterable
                remote
                reserve-keyword
                placeholder="请输入营业管理站名称搜索"
                style="width: 100%"
                :loading="stationOptionLoading.busStationId"
                :remote-method="keyword => searchStationOptions('busStationId', keyword)"
                @visible-change="visible => handleStationDropdownVisible('busStationId', visible)"
              >
                <el-option
                  v-for="item in stationOptions.busStationId"
                  :key="item.stationId"
                  :label="stationOptionLabel(item)"
                  :value="item.stationId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="光纤接入方式">
              <el-select v-model="editor.addrInTypeFtth" clearable placeholder="请选择光纤接入方式" style="width: 100%">
                <el-option
                  v-for="item in formOptions.addrInTypeFtthOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="光纤接入能力">
              <el-select v-model="editor.ftthPonType" clearable placeholder="请选择光纤接入能力" style="width: 100%">
                <el-option
                  v-for="item in formOptions.ftthPonTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电缆接入方式">
              <el-select v-model="editor.addrInTypeLan" clearable placeholder="请选择电缆接入方式" style="width: 100%">
                <el-option
                  v-for="item in formOptions.addrInTypeLanOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="城乡属性">
              <el-select v-model="editor.areaType" clearable placeholder="请选择城乡属性" style="width: 100%">
                <el-option
                  v-for="item in formOptions.areaTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房屋属性">
              <el-select v-model="editor.placeType" clearable placeholder="请选择房屋属性" style="width: 100%">
                <el-option
                  v-for="item in formOptions.placeTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否配套费小区">
              <el-select v-model="editor.supportingFeeCommunityFlag" clearable style="width: 100%">
                <el-option label="是" value="Y" />
                <el-option label="否" value="N" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="覆盖户数">
              <el-input v-model="editor.coverNum" placeholder="请输入覆盖户数" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工程编号">
              <el-input v-model="editor.singleProjectCode" placeholder="请输入工程编号" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditor">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="batchAddVisible"
      title="批量新增下级地址"
      width="1080px"
      @close="resetBatchAddState"
    >
      <div class="batch-layout">
        <div class="batch-panel">
          <div class="panel-title">父级地址候选</div>
          <div class="parent-search">
            <el-input
              v-model="parentSearchKeyword"
              clearable
              placeholder="请输入地址名称进行搜索"
              @keyup.enter.native="searchBatchParents"
            />
            <el-button :loading="parentSearchLoading" type="primary" @click="searchBatchParents">
              搜索
            </el-button>
          </div>
          <div class="result-tip">未输入关键字时默认展示当前页候选地址。</div>
          <el-table
            v-loading="parentSearchLoading"
            :data="parentSearchList"
            border
            height="420"
            highlight-current-row
            size="small"
            @row-click="chooseBatchParent"
          >
            <el-table-column prop="standName" label="标准地址" min-width="240" show-overflow-tooltip />
            <el-table-column prop="segmName" label="当级名称" min-width="120" show-overflow-tooltip />
            <el-table-column label="级别" width="110">
              <template #default="{ row }">
                {{ levelLabel(row.addrLevel, row.segmType) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button size="mini" type="text" @click.stop="chooseBatchParent(row)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="batch-panel">
          <div class="panel-title">批量生成配置</div>
          <el-form :model="batchAdd" label-width="110px">
            <el-form-item label="父级地址">
              <el-input :value="batchAdd.parentStandName || '-'" disabled />
            </el-form-item>
            <el-form-item label="前缀">
              <el-input v-model="batchAdd.prefix" placeholder="例如 A、B 或 第" />
            </el-form-item>
            <el-form-item label="起始号">
              <el-input v-model="batchAdd.startNum" placeholder="例如 1" />
            </el-form-item>
            <el-form-item label="结束号">
              <el-input v-model="batchAdd.endNum" placeholder="例如 20" />
            </el-form-item>
            <el-form-item label="后缀">
              <el-input v-model="batchAdd.suffix" placeholder="例如 单元、室" />
            </el-form-item>
          </el-form>

          <div class="preview-actions">
            <el-button :loading="previewLoading" @click="previewBatchAdd">预览</el-button>
            <el-button
              type="primary"
              :disabled="!batchPreviewList.length"
              :loading="submitBatchLoading"
              @click="submitBatchAdd"
            >
              执行批量生成
            </el-button>
          </div>

          <div class="panel-title preview-title">预览结果</div>
          <el-table
            v-loading="previewLoading"
            :data="batchPreviewList"
            border
            height="280"
            size="small"
          >
            <el-table-column label="序号" width="68">
              <template #default="{ $index }">
                {{ $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="standName" label="完整地址" min-width="320" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="tagDialogVisible"
      :title="tagDialogTitle()"
      width="760px"
      @close="resetTagDialog"
    >
      <div v-loading="tagOptionLoading" class="tag-dialog-body">
        <div class="tag-section">
          <div class="panel-title">已选地址（共{{ tagDialogRows.length }}条）</div>
          <el-table :data="tagDialogRows" border size="small" max-height="220">
            <el-table-column label="标准地址名称" min-width="320" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.standName || row.segmName || row.segmId }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button size="mini" type="text" @click="removeTagDialogRow(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="tag-selected-actions">
            <el-button size="mini" @click="clearTagDialogRows">一键清空</el-button>
          </div>
        </div>

        <div class="tag-section">
          <div class="panel-title">{{ tagDialogOptionTitle() }}</div>
          <el-checkbox-group v-if="tagOptions.length" v-model="selectedTagIds" class="tag-checkbox-group">
            <el-checkbox
              v-for="item in tagOptions"
              :key="item.id"
              :label="item.id"
            >
              {{ item.name }}
            </el-checkbox>
          </el-checkbox-group>
          <div v-else class="tag-empty-tip">{{ tagDialogEmptyText() }}</div>
        </div>
      </div>
      <div slot="footer" class="dialog-actions">
        <el-button @click="tagDialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          :disabled="!tagDialogRows.length || !selectedTagIds.length || tagOptionLoading"
          :loading="tagSubmitLoading"
          @click="submitTagDialog"
        >
          {{ tagDialogConfirmText() }}
        </el-button>
      </div>
    </el-dialog>

    <el-dialog
      :visible.sync="importDialogVisible"
      title="批量导入"
      width="620px"
      @close="resetImportDialog"
    >
      <div class="import-panel">
        <el-button type="primary" plain @click="downloadImportTemplate">下载导入模板</el-button>
        <p class="import-tip">模板内置地址等级下拉框、必填项校验等限制</p>
        <el-upload
          drag
          action=""
          :auto-upload="false"
          :limit="1"
          :file-list="importFileList"
          :before-upload="() => false"
          :on-change="handleImportFileChange"
          :on-remove="handleImportFileRemove"
        >
          <i class="el-icon-upload" />
          <div class="el-upload__text">将文件拖到此处，或点击上传</div>
          <div class="el-upload__tip">仅支持.xlsx格式，需按模板填写</div>
        </el-upload>
        <div v-if="importResult" class="import-result">
          <div>本次导入共 {{ importResult.totalCount || 0 }} 条，成功 {{ importResult.successCount || 0 }} 条，失败 {{ importResult.failCount || 0 }} 条</div>
          <el-button
            v-if="importResult.failureExportable && importResult.batchId"
            type="warning"
            plain
            size="mini"
            @click="downloadImportFailures(importResult.batchId)"
          >
            导出失败数据
          </el-button>
        </div>
      </div>
      <div slot="footer" class="dialog-actions">
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!importFile" :loading="importUploading" @click="submitImport">
          上传
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  bindTagsToStandardAddresses,
  batchAddStandardAddressChildren,
  createStandardAddress,
  deleteStandardAddresses,
  downloadStandardAddressImportTemplate,
  exportImportFailDetails,
  getStandardAddressDetail,
  getStandardAddressFormOptions,
  getStandardAddressLevelOptions,
  getStandardAddressList,
  getStandardAddressTags,
  getStandardAddressStationOptions,
  getTagList,
  importStandardAddressData,
  previewStandardAddressChildren,
  searchSelectionStandardAddresses,
  unbindTagsFromStandardAddresses,
  updateStandardAddress
} from '../api/address';
import {
  buildFormOptionState,
  createEmptyFormOptionLabelMaps,
  createEmptyFormOptions,
  createEmptyStationOptionLoading,
  createEmptyStationOptions,
  getFormOptionLabel,
  getStationOptionLabel,
  mergeStationOptions,
  STATION_MANAGE_TYPE_MAP
} from '../utils/standard-form-options';
import { buildLevelMaps, normalizeLevelOptions, resolveLevelLabel } from '../utils/standard-level';

const ACTIVE_STATUS = '2140900';
const READONLY_REGION_ADDR_TYPES = ['180000', '180001'];

function createEmptyEditor(parent = null) {
  return {
    segmId: null,
    parentSegmId: parent?.segmId || '',
    parentStandName: parent?.standName || parent?.parentStandName || '',
    parentAddrLevel: parent?.addrLevel ?? parent?.level ?? '',
    regionId: parent?.regionId || '',
    segmName: '',
    addrLevel: '',
    segmType: '',
    status: ACTIVE_STATUS,
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
  };
}

function createEmptyBatchAdd() {
  return {
    parentSegmId: '',
    parentStandName: '',
    prefix: '',
    startNum: '',
    endNum: '',
    suffix: ''
  };
}

function isReadonlyRegionSegmType(segmType, levelOptions = []) {
  const normalizedSegmType = segmType === null || segmType === undefined ? '' : String(segmType).trim();
  if (!normalizedSegmType) {
    return false;
  }
  if (READONLY_REGION_ADDR_TYPES.includes(normalizedSegmType)) {
    return true;
  }
  const matchedOption = (levelOptions || []).find(item => String(item?.addrTypeId || '').trim() === normalizedSegmType);
  const addrLevel = Number(matchedOption?.addrLevel);
  return addrLevel === 1 || addrLevel === 2;
}

function dedupeBySegmId(rows = []) {
  const rowMap = new Map();
  rows.forEach(item => {
    if (item?.segmId && !rowMap.has(item.segmId)) {
      rowMap.set(item.segmId, item);
    }
  });
  return [...rowMap.values()];
}

function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hour = `${date.getHours()}`.padStart(2, '0');
  const minute = `${date.getMinutes()}`.padStart(2, '0');
  const second = `${date.getSeconds()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

export default {
  data() {
    return {
      loading: false,
      query: {
        standName: '',
        segmType: ''
      },
      levelOptions: [],
      levelNameMap: {},
      typeNameMap: {},
      formOptions: createEmptyFormOptions(),
      formOptionLabelMaps: createEmptyFormOptionLabelMaps(),
      stationOptions: createEmptyStationOptions(),
      stationOptionLoading: createEmptyStationOptionLoading(),
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      selectedRows: [],
      selectedStandardAddressIds: [],
      parentNameMap: {},
      editorVisible: false,
      editor: createEmptyEditor(),
      editorRules: {
        parentSegmId: [{ required: true, message: '请先选择父级地址', trigger: 'blur' }],
        addrLevel: [{ required: true, message: '请选择当前地址级别', trigger: 'change' }],
        segmName: [{ required: true, message: '请输入当级名称', trigger: 'blur' }]
      },
      batchAddVisible: false,
      batchAdd: createEmptyBatchAdd(),
      batchPreviewList: [],
      batchSelectedParent: null,
      parentSearchKeyword: '',
      parentSearchList: [],
      parentSearchLoading: false,
      previewLoading: false,
      submitBatchLoading: false,
      importDialogVisible: false,
      importUploading: false,
      importFile: null,
      importFileList: [],
      importResult: null,
      importUpdateSupport: false,
      tagDialogVisible: false,
      tagDialogMode: 'bind',
      tagDialogRows: [],
      tagOptionLoading: false,
      tagSubmitLoading: false,
      tagOptions: [],
      selectedTagIds: []
    };
  },
  mounted() {
    this.initializePage();
  },
  methods: {
    async initializePage() {
      await Promise.all([
        this.loadLevelOptions(),
        this.loadFormOptions()
      ]);
      await this.fetchList();
    },
    createEmptyEditor(parent = null) {
      return createEmptyEditor(parent);
    },
    createEmptyBatchAdd() {
      return createEmptyBatchAdd();
    },
    applyLevelOptions(options) {
      const normalizedOptions = normalizeLevelOptions(options);
      const { levelNameMap, typeNameMap } = buildLevelMaps(normalizedOptions);
      this.levelOptions = normalizedOptions;
      this.levelNameMap = levelNameMap;
      this.typeNameMap = typeNameMap;
    },
    applyFormOptions(payload) {
      const { formOptions, formOptionLabelMaps } = buildFormOptionState(payload);
      this.formOptions = formOptions;
      this.formOptionLabelMaps = formOptionLabelMaps;
    },
    applyStationOptions(field, options, currentValue = '') {
      this.stationOptions = {
        ...this.stationOptions,
        [field]: mergeStationOptions(this.stationOptions[field], options, currentValue)
      };
    },
    seedEditorStationOptions(detail = {}) {
      const stationFieldMap = {
        stationId: 'stationName',
        installStationId: 'installStationName',
        busStationId: 'busStationName'
      };
      Object.entries(stationFieldMap).forEach(([field, nameField]) => {
        const stationId = detail?.[field];
        if (!stationId) {
          return;
        }
        this.stationOptions = {
          ...this.stationOptions,
          [field]: mergeStationOptions(this.stationOptions[field], [{
            stationId,
            stationName: detail?.[nameField] || stationId,
            regionId: '',
            manageType: ''
          }], stationId)
        };
      });
    },
    async loadLevelOptions() {
      try {
        const res = await getStandardAddressLevelOptions();
        this.applyLevelOptions(res.data || []);
      } catch (err) {
        this.applyLevelOptions([]);
        this.$message.error(err?.friendlyMessage || err?.message || '级别字典加载失败');
      }
    },
    async loadFormOptions() {
      try {
        const res = await getStandardAddressFormOptions();
        this.applyFormOptions(res.data || {});
      } catch (err) {
        this.applyFormOptions({});
        this.$message.error(err?.friendlyMessage || err?.message || '编辑页字典加载失败');
      }
    },
    levelOptionLabel(option) {
      if (!option) {
        return '-';
      }
      return option.name || (option.addrLevel ? `第 ${option.addrLevel} 级` : '-');
    },
    levelLabel(addrLevel, segmType) {
      return resolveLevelLabel({
        levelNameMap: this.levelNameMap,
        typeNameMap: this.typeNameMap
      }, addrLevel, segmType);
    },
    editorLevelOptions() {
      const parentAddrLevel = Number(this.editor?.parentAddrLevel);
      if (!Number.isFinite(parentAddrLevel)) {
        return this.levelOptions;
      }
      return this.levelOptions.filter(item => Number(item.addrLevel) > parentAddrLevel);
    },
    buildAssembledStandName(editor) {
      const parentStandName = editor?.parentStandName?.trim?.() || '';
      const segmName = editor?.segmName?.trim?.() || '';
      return `${parentStandName}${segmName}` || '系统自动生成';
    },
    formOptionLabel(field, value) {
      return getFormOptionLabel(this.formOptionLabelMaps, field, value);
    },
    stationOptionLabel(option) {
      return getStationOptionLabel(option);
    },
    formatDate(value) {
      return formatDateTime(value);
    },
    parentDisplayName(row) {
      return row.parentStandName || this.parentNameMap[row.parentSegmId] || row.parentSegmId || '-';
    },
    async resolveParentStandName(parentSegmId, row = null) {
      if (!parentSegmId) {
        return '';
      }
      if (row?.parentStandName) {
        return row.parentStandName;
      }
      if (this.parentNameMap[parentSegmId]) {
        return this.parentNameMap[parentSegmId];
      }
      try {
        const res = await getStandardAddressDetail(parentSegmId);
        const detail = res.data || {};
        const parentStandName = detail.standName || detail.segmName || parentSegmId;
        this.parentNameMap = {
          ...this.parentNameMap,
          [parentSegmId]: parentStandName
        };
        return parentStandName;
      } catch (err) {
        return row?.parentSegmId || parentSegmId;
      }
    },
    buildDefaultBatchParentCandidates() {
      return dedupeBySegmId([
        ...this.selectedRows,
        ...this.list
      ]).slice(0, 50);
    },
    buildListQueryParams() {
      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize
      };
      if (this.query.standName) {
        params.standName = this.query.standName.trim();
      }
      if (this.query.segmType !== '' && this.query.segmType !== null) {
        params.segmType = String(this.query.segmType).trim();
      }
      if (!isReadonlyRegionSegmType(params.segmType, this.levelOptions)) {
        params.orderByColumn = 'createDate';
        params.isAsc = 'desc';
      }
      return params;
    },
    async fetchList() {
      this.loading = true;
      try {
        const params = this.buildListQueryParams();
        const res = await getStandardAddressList(params);
        this.list = res.rows || [];
        this.total = res.total || 0;
        await this.loadParentNames();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '查询失败');
      } finally {
        this.loading = false;
      }
    },
    async loadParentNames() {
      const parentNameMapFromRows = Object.fromEntries(
        (this.list || [])
          .filter(item => item?.parentSegmId && item?.parentStandName)
          .map(item => [item.parentSegmId, item.parentStandName])
      );
      if (Object.keys(parentNameMapFromRows).length) {
        this.parentNameMap = {
          ...this.parentNameMap,
          ...parentNameMapFromRows
        };
      }
      const parentIds = [...new Set(
        (this.list || [])
          .map(item => item.parentSegmId)
          .filter(item => item && !parentNameMapFromRows[item] && !this.parentNameMap[item])
      )];
      if (!parentIds.length) {
        return;
      }
      const results = await Promise.all(parentIds.map(async parentSegmId => {
        try {
          const res = await getStandardAddressDetail(parentSegmId);
          const detail = res.data || {};
          return [parentSegmId, detail.standName || detail.segmName || parentSegmId];
        } catch (err) {
          return [parentSegmId, parentSegmId];
        }
      }));
      this.parentNameMap = {
        ...this.parentNameMap,
        ...Object.fromEntries(results)
      };
    },
    reset() {
      this.query = {
        standName: '',
        segmType: ''
      };
      this.pageNum = 1;
      this.fetchList();
    },
    resolveEditorRegionId() {
      return this.editor?.regionId || '';
    },
    async loadStationOptions(field, keyword = '') {
      const manageType = STATION_MANAGE_TYPE_MAP[field];
      if (!manageType) {
        return;
      }
      this.stationOptionLoading = {
        ...this.stationOptionLoading,
        [field]: true
      };
      try {
        const payload = {
          manageType,
          limit: 20
        };
        const regionId = this.resolveEditorRegionId();
        const trimmedKeyword = keyword?.trim?.() || '';
        if (regionId) {
          payload.regionId = regionId;
        }
        if (trimmedKeyword) {
          payload.keyword = trimmedKeyword;
        }
        const res = await getStandardAddressStationOptions(payload);
        this.applyStationOptions(field, res.data || [], this.editor?.[field]);
      } catch (err) {
        this.applyStationOptions(field, [], this.editor?.[field]);
        this.$message.error(err?.friendlyMessage || err?.message || '管理站候选加载失败');
      } finally {
        this.stationOptionLoading = {
          ...this.stationOptionLoading,
          [field]: false
        };
      }
    },
    async loadEditorStationOptions() {
      await Promise.all([
        this.loadStationOptions('stationId'),
        this.loadStationOptions('installStationId'),
        this.loadStationOptions('busStationId')
      ]);
    },
    searchStationOptions(field, keyword) {
      this.loadStationOptions(field, keyword);
    },
    handleStationDropdownVisible(field, visible) {
      if (visible) {
        this.loadStationOptions(field);
      }
    },
    changePage(page) {
      this.pageNum = page;
      this.fetchList();
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows;
      this.selectedStandardAddressIds = rows.map(item => item.segmId);
    },
    openDetail(row) {
      this.$router.push(`/standard/detail/${row.segmId}`);
    },
    async openCreateDialog() {
      if (!this.selectedRows.length) {
        this.$message.warning('请先勾选一条或多条标准地址记录');
        return;
      }
      if (this.selectedRows.length > 1) {
        this.$message.warning('新增标准地址时只能勾选一条父级地址记录');
        return;
      }
      this.editor = this.createEmptyEditor(this.selectedRows[0]);
      if (!this.editorLevelOptions().length) {
        this.$message.warning('当前父级地址下已无可选的更低级别');
        return;
      }
      await this.loadFormOptions();
      await this.loadEditorStationOptions();
      this.editorVisible = true;
      this.$nextTick(() => this.$refs.editorForm?.clearValidate());
    },
    async openEditDialog(row) {
      try {
        const res = await getStandardAddressDetail(row.segmId);
        const detail = res.data || row;
        const parentStandName = await this.resolveParentStandName(detail.parentSegmId || row.parentSegmId, row);
        this.editor = {
          ...this.createEmptyEditor(),
          segmId: detail.segmId || row.segmId,
          parentSegmId: detail.parentSegmId || row.parentSegmId || '',
          parentStandName,
          regionId: detail.regionId || row.regionId || '',
          segmName: detail.segmName || row.segmName || '',
          addrLevel: detail.addrLevel ?? row.addrLevel ?? '',
          segmType: detail.segmType ?? row.segmType ?? '',
          status: String(detail.status || row.status || ACTIVE_STATUS),
          notes: detail.notes || '',
          stationId: detail.stationId || '',
          installStationId: detail.installStationId || '',
          busStationId: detail.busStationId || '',
          addrInTypeFtth: detail.addrInTypeFtth === null || detail.addrInTypeFtth === undefined ? '' : String(detail.addrInTypeFtth),
          ftthPonType: detail.ftthPonType === null || detail.ftthPonType === undefined ? '' : String(detail.ftthPonType),
          addrInTypeLan: detail.addrInTypeLan === null || detail.addrInTypeLan === undefined ? '' : String(detail.addrInTypeLan),
          areaType: detail.areaType === null || detail.areaType === undefined ? '' : String(detail.areaType),
          placeType: detail.placeType === null || detail.placeType === undefined ? '' : String(detail.placeType),
          coverNum: detail.coverNum ?? '',
          singleProjectCode: detail.singleProjectCode || '',
          supportingFeeCommunityFlag: detail.supportingFeeCommunityFlag || '',
          isCity: detail.isCity || ''
        };
        this.seedEditorStationOptions(detail);
        await this.loadFormOptions();
        await this.loadEditorStationOptions();
        this.editorVisible = true;
        this.$nextTick(() => this.$refs.editorForm?.clearValidate());
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '加载详情失败');
      }
    },
    normalizeOptionalNumber(value) {
      if (value === '' || value === null || value === undefined) {
        return undefined;
      }
      const parsed = Number(value);
      return Number.isNaN(parsed) ? undefined : parsed;
    },
    buildEditorPayload() {
      const payload = {
        segmId: this.editor.segmId || undefined,
        parentSegmId: this.editor.parentSegmId || undefined,
        regionId: this.editor.regionId?.trim?.() || undefined,
        segmName: this.editor.segmName?.trim() || undefined,
        addrLevel: this.normalizeOptionalNumber(this.editor.addrLevel),
        segmType: this.editor.segmType?.toString?.().trim?.() || undefined,
        status: this.editor.status || ACTIVE_STATUS,
        notes: this.editor.notes?.trim() || undefined,
        stationId: this.editor.stationId?.trim() || undefined,
        installStationId: this.editor.installStationId?.trim() || undefined,
        busStationId: this.editor.busStationId?.trim() || undefined,
        addrInTypeFtth: this.normalizeOptionalNumber(this.editor.addrInTypeFtth),
        ftthPonType: this.normalizeOptionalNumber(this.editor.ftthPonType),
        addrInTypeLan: this.normalizeOptionalNumber(this.editor.addrInTypeLan),
        areaType: this.normalizeOptionalNumber(this.editor.areaType),
        placeType: this.normalizeOptionalNumber(this.editor.placeType),
        coverNum: this.normalizeOptionalNumber(this.editor.coverNum),
        singleProjectCode: this.editor.singleProjectCode?.trim() || undefined,
        supportingFeeCommunityFlag: this.editor.supportingFeeCommunityFlag || undefined,
        isCity: this.editor.isCity || undefined
      };
      return Object.fromEntries(
        Object.entries(payload).filter(([, value]) => value !== undefined)
      );
    },
    submitEditor() {
      this.$refs.editorForm.validate(async valid => {
        if (!valid) {
          return;
        }
        try {
          const payload = this.buildEditorPayload();
          if (this.editor.segmId) {
            await updateStandardAddress(payload);
            this.$message.success('标准地址已更新');
          } else {
            await createStandardAddress(payload);
            this.$message.success('标准地址已创建');
          }
          this.editorVisible = false;
          await this.fetchList();
        } catch (err) {
          this.$message.error(err?.friendlyMessage || err?.message || '保存失败');
        }
      });
    },
    async executeDelete(segmIds) {
      try {
        await deleteStandardAddresses(segmIds, false);
        this.$message.success('标准地址已删除');
      } catch (err) {
        const message = err?.friendlyMessage || err?.message || '';
        if (!message.includes('确认')) {
          throw err;
        }
        await this.$confirm('勾选的标准地址中存在关联安装地址，是否继续删除？', '二次确认');
        await deleteStandardAddresses(segmIds, true);
        this.$message.success('标准地址已删除');
      }
    },
    async batchRemove() {
      if (!this.selectedStandardAddressIds.length) {
        this.$message.warning('请先勾选一条或多条标准地址记录');
        return;
      }
      try {
        await this.$confirm(`确认删除已勾选的 ${this.selectedStandardAddressIds.length} 条标准地址记录吗？`, '提示');
        await this.executeDelete(this.selectedStandardAddressIds.join(','));
        this.$refs.standardTable?.clearSelection();
        await this.fetchList();
      } catch (err) {
        if (err !== 'cancel' && err !== 'close') {
          this.$message.warning(err?.friendlyMessage || err?.message || '删除失败');
        }
      }
    },
    exportCurrentPage() {
      if (!this.list.length) {
        this.$message.warning('当前没有可导出的标准地址记录');
        return;
      }
      const header = ['序号', '标准地址', '地址简拼', '父级地址', '当级名称', '级别', '创建时间'];
      const rows = this.list.map((item, index) => [
        (this.pageNum - 1) * this.pageSize + index + 1,
        item.standName || '',
        item.standNo || '',
        this.parentDisplayName(item),
        item.segmName || '',
        this.levelLabel(item.addrLevel, item.segmType),
        this.formatDate(item.createDate)
      ]);
      const csvContent = [header, ...rows]
        .map(columns => columns.map(value => `"${String(value).replace(/"/g, '""')}"`).join(','))
        .join('\n');
      const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      const date = new Date();
      const timestamp = [
        date.getFullYear(),
        `${date.getMonth() + 1}`.padStart(2, '0'),
        `${date.getDate()}`.padStart(2, '0'),
        `${date.getHours()}`.padStart(2, '0'),
        `${date.getMinutes()}`.padStart(2, '0'),
        `${date.getSeconds()}`.padStart(2, '0')
      ].join('');
      link.href = window.URL.createObjectURL(blob);
      link.download = `standard-address-list-${timestamp}.csv`;
      link.click();
      window.URL.revokeObjectURL(link.href);
      this.$message.success('标准地址列表已导出');
    },
    handleMoreAction(command) {
      if (command === 'batchAdd') {
        this.openBatchAddDialog();
        return;
      }
      if (command === 'import') {
        this.openImportDialog();
        return;
      }
      if (command === 'bindTag') {
        this.openBindTagDialog();
        return;
      }
      if (command === 'unbindTag') {
        this.openUnbindTagDialog();
      }
    },
    tagDialogTitle() {
      return this.tagDialogMode === 'unbind' ? '批量删标签' : '批量打标签';
    },
    tagDialogOptionTitle() {
      if (this.tagDialogMode === 'unbind') {
        return '选择要删除的标签（仅显示选中地址已绑定的标签）';
      }
      return '选择标签';
    },
    tagDialogEmptyText() {
      if (this.tagDialogMode === 'unbind') {
        return '选中地址暂无已绑定标签';
      }
      return '暂无可选标签';
    },
    tagDialogConfirmText() {
      return this.tagDialogMode === 'unbind' ? '确认删标签' : '确认打标签';
    },
    buildTagDialogRows(rows = []) {
      return dedupeBySegmId(rows)
        .filter(item => item?.segmId)
        .map(item => ({ ...item }));
    },
    async openTagDialog(mode) {
      if (!this.selectedRows.length) {
        this.$message.warning('请先勾选一条或多条标准地址记录');
        return;
      }
      this.tagDialogMode = mode;
      this.tagDialogRows = this.buildTagDialogRows(this.selectedRows);
      this.tagDialogVisible = true;
      this.selectedTagIds = [];
      this.tagOptions = [];
      if (mode === 'unbind') {
        await this.loadBoundTagOptions();
        return;
      }
      await this.loadTagOptions();
    },
    openBindTagDialog() {
      return this.openTagDialog('bind');
    },
    openUnbindTagDialog() {
      return this.openTagDialog('unbind');
    },
    async loadTagOptions() {
      this.tagOptionLoading = true;
      try {
        const res = await getTagList({
          pageNum: 1,
          pageSize: 1000
        });
        this.tagOptions = res.rows || [];
      } catch (err) {
        this.tagOptions = [];
        this.$message.error(err?.friendlyMessage || err?.message || '标签加载失败');
      } finally {
        this.tagOptionLoading = false;
      }
    },
    async loadBoundTagOptions() {
      this.tagOptionLoading = true;
      try {
        const results = await Promise.all(
          this.tagDialogRows.map(row => getStandardAddressTags(row.segmId))
        );
        const optionMap = new Map();
        results.forEach(res => {
          const rows = res?.data || [];
          rows.forEach(item => {
            if (item?.id && !optionMap.has(item.id)) {
              optionMap.set(item.id, item);
            }
          });
        });
        this.tagOptions = [...optionMap.values()];
      } catch (err) {
        this.tagOptions = [];
        this.$message.error(err?.friendlyMessage || err?.message || '已绑标签加载失败');
      } finally {
        this.tagOptionLoading = false;
      }
    },
    async removeTagDialogRow(row) {
      this.tagDialogRows = this.tagDialogRows.filter(item => item.segmId !== row.segmId);
      if (this.tagDialogMode === 'unbind') {
        this.selectedTagIds = [];
        await this.loadBoundTagOptions();
      }
    },
    async clearTagDialogRows() {
      this.tagDialogRows = [];
      this.selectedTagIds = [];
      this.tagOptions = [];
    },
    resetTagDialog() {
      this.tagDialogVisible = false;
      this.tagDialogMode = 'bind';
      this.tagDialogRows = [];
      this.tagOptionLoading = false;
      this.tagSubmitLoading = false;
      this.tagOptions = [];
      this.selectedTagIds = [];
    },
    async submitTagDialog() {
      if (!this.tagDialogRows.length) {
        this.$message.warning('请先勾选一条或多条标准地址记录');
        return;
      }
      if (!this.selectedTagIds.length) {
        this.$message.warning('请选择标签');
        return;
      }
      this.tagSubmitLoading = true;
      try {
        const payload = {
          standardAddressIds: this.tagDialogRows.map(item => item.segmId),
          tagIds: this.selectedTagIds
        };
        if (this.tagDialogMode === 'unbind') {
          await unbindTagsFromStandardAddresses(payload);
          this.$message.success('批量删标签成功');
        } else {
          await bindTagsToStandardAddresses(payload);
          this.$message.success('批量打标签成功');
        }
        this.tagDialogVisible = false;
        this.resetTagDialog();
        await this.fetchList();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '标签操作失败');
      } finally {
        this.tagSubmitLoading = false;
      }
    },
    openImportDialog() {
      this.importDialogVisible = true;
      this.importResult = null;
    },
    resetImportDialog() {
      this.importUploading = false;
      this.importFile = null;
      this.importFileList = [];
      this.importUpdateSupport = false;
      this.importResult = null;
    },
    handleImportFileChange(file, fileList) {
      const latestFile = fileList.slice(-1)[0];
      const rawFile = latestFile?.raw || file?.raw || null;
      const fileName = latestFile?.name || rawFile?.name || '';
      if (!fileName.toLowerCase().endsWith('.xlsx')) {
        this.$message.warning('仅支持上传.xlsx文件');
        this.importFile = null;
        this.importFileList = [];
        return;
      }
      this.importFile = rawFile;
      this.importFileList = latestFile ? [latestFile] : [];
    },
    handleImportFileRemove() {
      this.importFile = null;
      this.importFileList = [];
    },
    async downloadImportTemplate() {
      try {
        const blob = await downloadStandardAddressImportTemplate();
        this.downloadBlob(blob, '标准地址导入模板.xlsx');
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '导入模板下载失败');
      }
    },
    async downloadImportFailures(batchId) {
      try {
        const blob = await exportImportFailDetails(batchId);
        this.downloadBlob(blob, `standard-address-import-failures-${batchId}.xlsx`);
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '失败数据导出失败');
      }
    },
    downloadBlob(blob, filename) {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.click();
      window.URL.revokeObjectURL(url);
    },
    async submitImport() {
      if (!this.importFile) {
        this.$message.warning('请先选择导入文件');
        return;
      }
      this.importUploading = true;
      try {
        const res = await importStandardAddressData(this.importFile, this.importUpdateSupport);
        this.importResult = res.data || res;
        await this.fetchList();
        this.$message.success('导入完成');
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '导入失败');
      } finally {
        this.importUploading = false;
      }
    },
    openBatchAddDialog() {
      this.batchAddVisible = true;
      this.parentSearchList = this.buildDefaultBatchParentCandidates();
      if (this.selectedRows.length === 1) {
        this.chooseBatchParent(this.selectedRows[0]);
      }
      if (!this.parentSearchList.length) {
        this.searchBatchParents({ allowEmpty: true, silent: true });
      }
    },
    chooseBatchParent(row) {
      if (!row?.segmId) {
        return;
      }
      this.batchSelectedParent = row;
      this.batchAdd.parentSegmId = row.segmId;
      this.batchAdd.parentStandName = row.standName || row.segmName || row.segmId;
      this.parentSearchKeyword = row.standName || row.segmName || '';
      this.batchPreviewList = [];
    },
    async searchBatchParents(options = {}) {
      const keyword = this.parentSearchKeyword.trim();
      if (!keyword && !options.allowEmpty) {
        this.$message.warning('请输入地址名称进行搜索');
        return;
      }
      this.parentSearchLoading = true;
      try {
        const res = await searchSelectionStandardAddresses({
          keyword,
          limit: 200
        });
        const result = res.data || [];
        this.parentSearchList = result.length ? result : this.buildDefaultBatchParentCandidates();
      } catch (err) {
        if (!options.silent) {
          this.$message.error(err?.friendlyMessage || err?.message || '父级地址搜索失败');
        }
        this.parentSearchList = this.buildDefaultBatchParentCandidates();
      } finally {
        this.parentSearchLoading = false;
      }
    },
    buildBatchPayload() {
      return {
        parentSegmId: this.batchAdd.parentSegmId,
        prefix: this.batchAdd.prefix?.trim() || '',
        startNum: Number(this.batchAdd.startNum),
        endNum: Number(this.batchAdd.endNum),
        suffix: this.batchAdd.suffix?.trim() || ''
      };
    },
    validateBatchAdd() {
      if (!this.batchAdd.parentSegmId) {
        this.$message.warning('请先选择父级地址');
        return false;
      }
      if (!this.batchAdd.prefix?.trim()) {
        this.$message.warning('请填写地址前缀');
        return false;
      }
      if (this.batchAdd.startNum === '' || this.batchAdd.endNum === '') {
        this.$message.warning('请填写起始号和结束号');
        return false;
      }
      return true;
    },
    async previewBatchAdd() {
      if (!this.validateBatchAdd()) {
        return;
      }
      this.previewLoading = true;
      try {
        const res = await previewStandardAddressChildren(this.buildBatchPayload());
        this.batchPreviewList = res.data || [];
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '预览失败');
      } finally {
        this.previewLoading = false;
      }
    },
    async submitBatchAdd() {
      if (!this.batchPreviewList.length) {
        this.$message.warning('请先生成预览结果');
        return;
      }
      this.submitBatchLoading = true;
      try {
        await batchAddStandardAddressChildren(this.buildBatchPayload());
        this.$message.success('批量新增成功');
        this.batchAddVisible = false;
        this.resetBatchAddState();
        await this.fetchList();
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '批量新增失败');
      } finally {
        this.submitBatchLoading = false;
      }
    },
    resetBatchAddState() {
      this.batchAdd = this.createEmptyBatchAdd();
      this.batchPreviewList = [];
      this.batchSelectedParent = null;
      this.parentSearchKeyword = '';
      this.parentSearchList = [];
      this.parentSearchLoading = false;
      this.previewLoading = false;
      this.submitBatchLoading = false;
    }
  }
};
</script>

<style scoped>
.standard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card,
.table-card {
  border-radius: 16px;
  border: 1px solid #ebeef5;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 8px;
}

.search-actions {
  margin-left: auto;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.toolbar-actions,
.toolbar-summary {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-summary {
  color: #606266;
  font-size: 13px;
}

.toolbar-tip {
  margin-bottom: 16px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.pager {
  margin-top: 16px;
  text-align: right;
}

.level-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 0 10px;
  height: 28px;
  border-radius: 999px;
  color: #1f5da8;
  background: #eef5ff;
  font-weight: 600;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.batch-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.batch-panel {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  background: #fafbfd;
}

.panel-title {
  margin-bottom: 12px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.parent-search {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.result-tip {
  margin-bottom: 12px;
  color: #909399;
  font-size: 12px;
}

.preview-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 16px;
}

.preview-title {
  margin-top: 16px;
}

.tag-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tag-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tag-selected-actions {
  display: flex;
  justify-content: flex-end;
}

.tag-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
}

.tag-empty-tip {
  color: #909399;
  font-size: 13px;
}

.import-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.import-tip {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.import-result {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff7e6;
  color: #8a5a00;
}

:deep(.search-form .el-form-item) {
  margin-bottom: 0;
}

:deep(.search-form .el-input),
:deep(.search-form .el-select) {
  width: 220px;
}

@media (max-width: 1100px) {
  .batch-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .table-toolbar {
    align-items: flex-start;
  }

  .search-actions {
    margin-left: 0;
  }

  :deep(.search-form .el-input),
  :deep(.search-form .el-select) {
    width: 100%;
  }
}
</style>
