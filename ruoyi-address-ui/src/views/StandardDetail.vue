<template>
  <div class="detail-page">
    <el-page-header content="标准地址详情" @back="goBack" />

    <el-card class="detail-card" shadow="never">
      <div slot="header" class="card-header">
        <div>
          <div class="card-title">{{ detail?.standName || '标准地址详情' }}</div>
          <div class="card-subtitle">
            重点展示标准地址清单、管理站归属与接入属性，页面字段与原型保持一致。
          </div>
        </div>
        <el-tag size="small" effect="plain">
          {{ levelLabel(detail?.addrLevel, detail?.segmType) }}
        </el-tag>
      </div>

      <el-descriptions :column="2" border class="detail-descriptions">
        <el-descriptions-item label="标准地址">{{ detail?.standName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址简拼">{{ detail?.standNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="父级地址">{{ parentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当级名称">{{ detail?.segmName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          {{ levelLabel(detail?.addrLevel, detail?.segmType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ formOptionLabel('status', detail?.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDate(detail?.createDate || detail?.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="标签">{{ formatTags(detail?.tagNames) }}</el-descriptions-item>
        <el-descriptions-item label="所属维修管理站">
          {{ displayStation(detail?.stationName, detail?.stationId) }}
        </el-descriptions-item>
        <el-descriptions-item label="所属安装管理站">
          {{ displayStation(detail?.installStationName, detail?.installStationId) }}
        </el-descriptions-item>
        <el-descriptions-item label="所属营业管理站">
          {{ displayStation(detail?.busStationName, detail?.busStationId) }}
        </el-descriptions-item>
        <el-descriptions-item label="光纤接入方式">{{ formOptionLabel('addrInTypeFtth', detail?.addrInTypeFtth) }}</el-descriptions-item>
        <el-descriptions-item label="光纤接入能力">{{ formOptionLabel('ftthPonType', detail?.ftthPonType) }}</el-descriptions-item>
        <el-descriptions-item label="电缆接入方式">{{ formOptionLabel('addrInTypeLan', detail?.addrInTypeLan) }}</el-descriptions-item>
        <el-descriptions-item label="城乡属性">{{ formOptionLabel('areaType', detail?.areaType) }}</el-descriptions-item>
        <el-descriptions-item label="房屋属性">{{ formOptionLabel('placeType', detail?.placeType) }}</el-descriptions-item>
        <el-descriptions-item label="是否配套费小区">
          {{ yesNoText(detail?.supportingFeeCommunityFlag) }}
        </el-descriptions-item>
        <el-descriptions-item label="覆盖户数">{{ detail?.coverNum ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="工程编号" :span="2">
          {{ detail?.singleProjectCode || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script>
import { getStandardAddressDetail, getStandardAddressFormOptions, getStandardAddressLevelOptions } from '../api/address';
import {
  buildFormOptionState,
  createEmptyFormOptionLabelMaps,
  createEmptyFormOptions,
  getFormOptionLabel
} from '../utils/standard-form-options';
import { buildLevelMaps, normalizeLevelOptions, resolveLevelLabel } from '../utils/standard-level';

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
      detail: null,
      parentName: '',
      levelOptions: [],
      levelNameMap: {},
      typeNameMap: {},
      formOptions: createEmptyFormOptions(),
      formOptionLabelMaps: createEmptyFormOptionLabelMaps()
    };
  },
  mounted() {
    this.initializePage();
  },
  watch: {
    '$route.params.segmId': 'fetchDetail'
  },
  methods: {
    async initializePage() {
      await Promise.all([
        this.loadLevelOptions(),
        this.loadFormOptions()
      ]);
      await this.fetchDetail();
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
        this.$message.error(err?.friendlyMessage || err?.message || '详情字典加载失败');
      }
    },
    async fetchDetail() {
      const segmId = this.$route.params.segmId;
      if (!segmId) {
        return;
      }
      try {
        const res = await getStandardAddressDetail(segmId);
        this.detail = res.data || null;
        this.parentName = this.detail?.parentStandName || '';
        if (!this.parentName && this.detail?.parentSegmId) {
          try {
            const parentRes = await getStandardAddressDetail(this.detail.parentSegmId);
            const parent = parentRes.data || {};
            this.parentName = parent.standName || parent.segmName || this.detail.parentSegmId;
          } catch (err) {
            this.parentName = this.detail.parentSegmId;
          }
        }
      } catch (err) {
        this.$message.error(err?.friendlyMessage || err?.message || '详情加载失败');
      }
    },
    goBack() {
      if (window.history.length > 1) {
        this.$router.back();
        return;
      }
      this.$router.push('/standard/list');
    },
    levelLabel(addrLevel, segmType) {
      return resolveLevelLabel({
        levelNameMap: this.levelNameMap,
        typeNameMap: this.typeNameMap
      }, addrLevel, segmType);
    },
    formOptionLabel(field, value) {
      return getFormOptionLabel(this.formOptionLabelMaps, field, value);
    },
    yesNoText(value) {
      if (value === 'Y' || value === '1' || value === true) {
        return '是';
      }
      if (value === 'N' || value === '0' || value === false) {
        return '否';
      }
      return '-';
    },
    formatDate(value) {
      return formatDateTime(value);
    },
    displayStation(name, id) {
      return name || id || '-';
    },
    formatTags(value) {
      if (Array.isArray(value)) {
        return value.length ? value.join('、') : '-';
      }
      return value || '-';
    }
  }
};
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card {
  border-radius: 16px;
  border: 1px solid #ebeef5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.card-title {
  color: #303133;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.6;
}

.card-subtitle {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.detail-descriptions {
  margin-top: 8px;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
  }
}
</style>
